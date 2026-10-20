package com.hims.ordering.application;

import com.hims.ordering.application.exceptions.InvalidOrderException;
import com.hims.ordering.domain.model.ClinicalOrder;
import com.hims.ordering.domain.model.OrderItem;
import com.hims.ordering.domain.model.SagaState;
import com.hims.ordering.domain.repository.ClinicalOrderRepository;
import com.hims.ordering.domain.repository.OrderItemRepository;
import com.hims.ordering.domain.repository.SagaStateRepository;
import com.hims.ordering.infrastructure.outbox.OutboxPublisher;
import com.hims.ordering.infrastructure.outbox.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Saga orchestrator state machine (MIG-033). Owns saga_state transitions and
 * the compensation semantics that replace the legacy single mega-transaction
 * (AddServiceOrderServiceImpl:2037):
 *
 * START (called from OrderService.create, same tx):
 *   clinical_order + order_item + saga_state + OrderInitiated outbox row —
 *   one atomic Postgres transaction (IMP-DEC-004).
 *
 * CONFIRM step: PENDING -> SUCCEEDED for an applicable step. When every
 *   applicable step is SUCCEEDED the order flips INITIATED -> CONFIRMED(2)
 *   and the saga COMPLETES. Idempotent on consumer redelivery.
 *
 * FAIL step: the failed step -> FAILED; every SUCCEEDED step -> COMPENSATED
 *   ("roll back pending reservations" — downstream services release what they
 *   reserved when they consume OrderFailed); order -> FAILED(4) with reason;
 *   saga -> FAILED; OrderFailed emitted in the SAME tx so it can never be lost.
 *
 * CANCEL: INITIATED-only guard; succeeded steps compensated; saga CANCELLED;
 *   OrderFailed emitted with failedStep=CANCELLED_BY_USER.
 */
@Service
public class SagaService {

    private static final Logger log = LoggerFactory.getLogger(SagaService.class);

    public static final long AUDIT_ACTOR_ID = 1L;

    /** Canonical saga steps (SagaState constants mirrored here for readability). */
    public static final String BILLING = SagaState.BILLING;
    public static final String LAB = SagaState.LAB;
    public static final String RADIOLOGY = SagaState.RADIOLOGY;
    public static final String NURSING = SagaState.NURSING;
    public static final String PHARMACY = SagaState.PHARMACY;

    private final SagaStateRepository sagaRepository;
    private final ClinicalOrderRepository orderRepository;
    private final OrderItemRepository itemRepository;
    private final OutboxService outboxService;

    /** Server-owned saga ids via ihms_ordering.saga_state_seq (IMP-DEC-003). */
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;

    public SagaService(SagaStateRepository sagaRepository,
                       ClinicalOrderRepository orderRepository,
                       OrderItemRepository itemRepository,
                       OutboxService outboxService) {
        this.sagaRepository = sagaRepository;
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.outboxService = outboxService;
    }

    // ------------------------------------------------------------------ start

    /**
     * Create the saga row for a freshly placed order and emit OrderInitiated in
     * the SAME transaction. Applicable steps: BILLING always; LAB/RADIOLOGY/
     * NURSING when matching item types exist; PHARMACY when pharmacyPrescriptionId
     * was linked at creation.
     */
    @Transactional
    public void start(ClinicalOrder order, List<OrderItem> items) {
        SagaState saga = new SagaState();
        saga.setOrderId(order.getOrderId());
        saga.setSagaStatus(SagaState.IN_PROGRESS);
        saga.setBillingStep(SagaState.PENDING);
        boolean hasLab = false, hasRad = false, hasNursing = false;
        for (OrderItem i : items) {
            switch (OrderItemTypeValue.of(i.getItemType())) {
                case "LAB" -> hasLab = true;
                case "RADIOLOGY" -> hasRad = true;
                case "NURSING" -> hasNursing = true;
                default -> { /* CONSULTATION needs billing only */ }
            }
        }
        if (hasLab) {
            saga.setLabStep(SagaState.PENDING);
        }
        if (hasRad) {
            saga.setRadiologyStep(SagaState.PENDING);
        }
        if (hasNursing) {
            saga.setNursingStep(SagaState.PENDING);
        }
        if (order.getPharmacyPrescriptionId() != null) {
            saga.setPharmacyStep(SagaState.PENDING);
        }
        saga.setSagaId(nextId("saga_state_seq"));
        saga.setStatus(true);
        saga.setSynchronizedd(false);
        saga.setAddedBy(AUDIT_ACTOR_ID);
        saga.setAddedDatetime(now());
        sagaRepository.save(saga);

        emitOrderInitiated(order, items, saga);
        log.info("Started saga {} for order {} (steps: {})",
                saga.getSagaId(), order.getOrderId(), saga.applicableSteps());
    }

    /**
     * OrderInitiated — type string EXACTLY 'OrderInitiated', schema token
     * order-initiated.v1, partition key encounterId with documented fallbacks
     * ('&lt;admissionId&gt;' then 'no-context:&lt;id&gt;', column NOT NULL).
     * Payload keys EXACTLY {orderId, patientId, encounterId, admissionId,
     * items[{lineNo,itemType,itemId,serviceName,rate,qty,doctorSharePercent}],
     * grossTotal, concessionPercent, concessionAmount, netTotal,
     * pharmacyPrescriptionId, prescriberId, wardId}.
     */
    void emitOrderInitiated(ClinicalOrder order, List<OrderItem> items, SagaState saga) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", String.valueOf(order.getOrderId()));
        payload.put("patientId", String.valueOf(order.getPatientId()));
        payload.put("encounterId", order.getEncounterId() == null ? null : String.valueOf(order.getEncounterId()));
        payload.put("admissionId", order.getAdmissionId() == null ? null : String.valueOf(order.getAdmissionId()));
        java.util.List<Map<String, Object>> itemMaps = new java.util.ArrayList<>();
        for (OrderItem i : items) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("lineNo", i.getLineNo());
            m.put("itemType", i.getItemType());
            m.put("itemId", String.valueOf(i.getItemId()));
            m.put("serviceName", i.getServiceName());
            m.put("rate", i.getRate());
            m.put("qty", i.getQty());
            m.put("doctorSharePercent", i.getDoctorSharePercent());  // BR-ORD-004 tag
            itemMaps.add(m);
        }
        payload.put("items", itemMaps);
        payload.put("grossTotal", order.getGrossTotal());
        payload.put("concessionPercent", order.getConcessionPercent());
        payload.put("concessionAmount", order.getConcessionAmount());
        payload.put("netTotal", order.getNetTotal());
        payload.put("pharmacyPrescriptionId", order.getPharmacyPrescriptionId() == null
                ? null : String.valueOf(order.getPharmacyPrescriptionId()));
        payload.put("prescriberId", String.valueOf(order.getPrescriberId()));
        payload.put("wardId", order.getWardId() == null ? null : String.valueOf(order.getWardId()));

        outboxService.append(OutboxPublisher.OrderingEvents.ORDER_INITIATED, "order",
                order.getOrderId(), partitionKeyOf(order), toJson(payload),
                "order-initiated:" + order.getOrderId());
    }

    // ------------------------------------------------------------------ confirm

    /**
     * Downstream SUCCESS callback: mark {@code step} SUCCEEDED for the order's
     * saga. Idempotent: an already-SUCCEEDED step returns quietly (consumer
     * redelivery). Non-applicable or terminal-saga invocations reject with
     * ORD_SAGA_STEP_INVALID / ORD_ILLEGAL_TRANSITION. When all applicable steps
     * are SUCCEEDED the order flips to CONFIRMED(2) and the saga COMPLETES.
     */
    @Transactional
    public SagaState confirmStep(long orderId, String step) {
        validateStepName(step);
        SagaState saga = loadByOrderId(orderId);
        String current = saga.step(step);
        if (current == null) {
            throw new InvalidOrderException("ORD_SAGA_STEP_INVALID",
                    "Step " + step + " does not apply to order " + orderId);
        }
        if (!saga.isInProgress()) {
            // consumer redelivery of an already-succeeded step after COMPLETION stays quiet;
            // anything else against a terminal saga rejects (no silent state drift)
            if (SagaState.SUCCEEDED.equals(current)) {
                return saga;
            }
            throw new InvalidOrderException("ORD_ILLEGAL_TRANSITION",
                    "Cannot confirm step " + step + " — saga of order " + orderId
                            + " is already " + saga.getSagaStatus());
        }
        if (SagaState.SUCCEEDED.equals(current)) {
            return saga;   // idempotent redelivery mid-flight
        }
        saga.setStep(step, SagaState.SUCCEEDED);
        saga.setUpdatedBy(AUDIT_ACTOR_ID);
        saga.setUpdatedDatetime(now());
        sagaRepository.save(saga);

        if (saga.allStepsSucceeded()) {
            ClinicalOrder order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new InvalidOrderException("NOT_FOUND",
                            "Order not found: " + orderId));
            order.setStatusCode(ClinicalOrder.STATUS_CONFIRMED);
            order.setUpdatedBy(AUDIT_ACTOR_ID);
            order.setUpdatedDatetime(now());
            orderRepository.save(order);
            saga.setSagaStatus(SagaState.COMPLETED);
            sagaRepository.save(saga);
            log.info("Saga for order {} COMPLETED — order CONFIRMED", orderId);
        } else {
            log.info("Saga step {} SUCCEEDED for order {} (remaining pending)",
                    step, orderId);
        }
        return saga;
    }

    // ------------------------------------------------------------------ fail / compensate

    /**
     * Downstream FAILURE callback (the saga compensation trigger). The failing
     * step is marked FAILED; every SUCCEEDED step is flipped COMPENSATED; the
     * order goes FAILED(4) with {@code reason} persisted as rejection_reason;
     * the saga goes FAILED — ALL in the same tx as the OrderFailed event, so
     * downstream consumers can roll back their reservations without any lost-
     * update window. Idempotent: re-delivered failures against an already-FAILED
     * saga refresh the reason only.
     */
    @Transactional
    public SagaState failStep(long orderId, String step, String reason) {
        validateStepName(step);
        if (reason == null || reason.isBlank()) {
            throw new InvalidOrderException("BEXA003",
                    "reason is required to fail a saga step");
        }
        SagaState saga = loadByOrderId(orderId);
        if (SagaState.FAILED.equals(saga.getSagaStatus())) {
            saga.setFailureReason(reason.trim());   // idempotent redelivery
            saga.setUpdatedDatetime(now());
            sagaRepository.save(saga);
            return saga;
        }
        if (!saga.isInProgress()) {
            throw new InvalidOrderException("ORD_ILLEGAL_TRANSITION",
                    "Cannot fail step " + step + " — saga of order " + orderId
                            + " is " + saga.getSagaStatus());
        }
        String current = saga.step(step);
        if (current == null) {
            throw new InvalidOrderException("ORD_SAGA_STEP_INVALID",
                    "Step " + step + " does not apply to order " + orderId);
        }

        saga.setStep(step, SagaState.STEP_FAILED);
        saga.setFailedStep(step);
        saga.setFailureReason(reason.trim());
        List<String> compensated = compensateSucceededSteps(saga);
        saga.setSagaStatus(SagaState.FAILED);
        saga.setUpdatedBy(AUDIT_ACTOR_ID);
        saga.setUpdatedDatetime(now());
        sagaRepository.save(saga);

        ClinicalOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new InvalidOrderException("NOT_FOUND", "Order not found: " + orderId));
        order.setStatusCode(ClinicalOrder.STATUS_FAILED);
        order.setRejectionReason(reason.trim());
        order.setUpdatedBy(AUDIT_ACTOR_ID);
        order.setUpdatedDatetime(now());
        orderRepository.save(order);

        emitOrderFailed(order, step, reason.trim(), compensated);   // SAME tx (IMP-DEC-004)
        log.warn("Saga FAILED at step {} for order {} — compensated: {}", step, orderId, compensated);
        return saga;
    }

    /**
     * Cancel an INITIATED order's saga: succeeded steps compensated, saga
     * CANCELLED, OrderFailed emitted (failedStep=CANCELLED_BY_USER) so any
     * reservation made before the cancel lands is released downstream too.
     * Guard lives in OrderService (INITIATED-only).
     */
    @Transactional
    public SagaState cancel(long orderId) {
        SagaState saga = loadByOrderId(orderId);
        if (!saga.isInProgress()) {
            throw new InvalidOrderException("ORD_ILLEGAL_TRANSITION",
                    "Cannot cancel saga of order " + orderId + " — it is " + saga.getSagaStatus());
        }
        List<String> compensated = compensateSucceededSteps(saga);
        saga.setSagaStatus(SagaState.CANCELLED);
        saga.setFailedStep("CANCELLED_BY_USER");
        saga.setFailureReason("cancelled by requester");
        saga.setUpdatedBy(AUDIT_ACTOR_ID);
        saga.setUpdatedDatetime(now());
        sagaRepository.save(saga);

        ClinicalOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new InvalidOrderException("NOT_FOUND", "Order not found: " + orderId));
        order.setRejectionReason("cancelled by requester");
        emitOrderFailed(order, "CANCELLED_BY_USER", "cancelled by requester", compensated);
        return saga;
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public SagaState byOrder(long orderId) {
        return loadByOrderId(orderId);
    }

    // ------------------------------------------------------------------ helpers

    private List<String> compensateSucceededSteps(SagaState saga) {
        List<String> compensated = new java.util.ArrayList<>(saga.succeededSteps());
        for (String s : saga.applicableSteps()) {
            String v = saga.step(s);
            if (SagaState.SUCCEEDED.equals(v) || SagaState.STEP_FAILED.equals(v)) {
                saga.setStep(s, SagaState.COMPENSATED);
            }
        }
        return compensated;
    }

    private void validateStepName(String step) {
        if (!(BILLING.equals(step) || LAB.equals(step) || RADIOLOGY.equals(step)
                || NURSING.equals(step) || PHARMACY.equals(step))) {
            throw new InvalidOrderException("ORD_SAGA_STEP_INVALID", "Unknown saga step: " + step);
        }
    }

    private SagaState loadByOrderId(long orderId) {
        return sagaRepository.findByOrderId(orderId)
                .orElseThrow(() -> new InvalidOrderException("NOT_FOUND",
                        "No saga found for order: " + orderId));
    }

    static String partitionKeyOf(ClinicalOrder order) {
        if (order.getEncounterId() != null) {
            return String.valueOf(order.getEncounterId());
        }
        if (order.getAdmissionId() != null) {
            return String.valueOf(order.getAdmissionId());
        }
        return "no-context:" + order.getOrderId();
    }

    private void emitOrderFailed(ClinicalOrder order, String failedStep, String reason,
                                 List<String> compensated) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", String.valueOf(order.getOrderId()));
        payload.put("patientId", String.valueOf(order.getPatientId()));
        payload.put("failedStep", failedStep);
        payload.put("reason", reason);
        payload.put("compensatedSteps", compensated);
        payload.put("netTotal", order.getNetTotal());
        outboxService.append(OutboxPublisher.OrderingEvents.ORDER_FAILED, "order",
                order.getOrderId(), partitionKeyOf(order), toJson(payload),
                "order-failed:" + order.getOrderId() + ":" + failedStep);
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_ordering." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }

    /** Internal string-switch helper over item types. */
    private static final class OrderItemTypeValue {
        static String of(String raw) {
            return raw == null ? "" : raw.trim().toUpperCase();
        }
    }
}
