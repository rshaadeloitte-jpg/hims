package com.hims.ordering.application;

import com.hims.ordering.application.exceptions.InvalidOrderException;
import com.hims.ordering.application.exceptions.OrderNotFoundException;
import com.hims.ordering.domain.model.ClinicalOrder;
import com.hims.ordering.domain.model.OrderItem;
import com.hims.ordering.domain.model.SagaState;
import com.hims.ordering.domain.repository.ClinicalOrderRepository;
import com.hims.ordering.domain.repository.OrderItemRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Clinical-order placement service (MIG-033, Catalog #4). Replaces the legacy
 * AddServiceOrderServiceImpl mega-transaction (@Transactional(rollbackOn=
 * Exception.class) :2037 — order -> bill -> charges -> lab -> rad -> EMR ->
 * doctor share) with saga choreography:
 *
 * CREATE: clinical_order + order_item rows are validated against BR-ORD-001..004,
 * totals are computed SERVER-SIDE, the saga_state row is created and ONE
 * OrderInitiated event is appended to the transactional outbox — all in a single
 * Postgres transaction. Downstream services (Billing charge, Lab booking, Rad
 * scan booking, Nursing scheduling, Pharmacy prescription receipt) confirm or
 * fail their step via {@link SagaService}.
 *
 * COMPENSATION: see {@link SagaService#failStep} — failures emit OrderFailed in
 * the same tx as the FAILED flip so reservations roll back downstream.
 *
 * BR-ORD rules enforced here:
 *   BR-ORD-001 identity/context: patientId + prescriberId required; encounterId
 *            XOR admissionId.
 *   BR-ORD-002 line integrity: >= 1 item; every item needs itemType in
 *            {LAB,RADIOLOGY,NURSING,CONSULTATION}, itemId, serviceName, rate>=0,
 *            qty>0; gross = SUM(rate*qty) computed server-side (client totals
 *            never trusted).
 *   BR-ORD-003 concession pre-validation: concessionPercent in [0,100]; amount
 *            = gross*pct/100 HALF_UP 2dp (documented choice for UNKNOWN-020
 *            rounding); amount <= gross; net = gross - amount >= 0.
 *   BR-ORD-004 doctor-share tags: per-item doctorSharePercent in [0,100]
 *            (default 0), tagged into the OrderInitiated payload for Billing's
 *            doctor-share charges (legacy T_DOCTORSHARECHARGESDETAIL).
 * (BR-ORD-005 lives in PrescriptionService.)
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final ClinicalOrderRepository orderRepository;
    private final OrderItemRepository itemRepository;
    private final SagaService sagaService;

    @PersistenceContext
    private EntityManager em;

    public OrderService(ClinicalOrderRepository orderRepository,
                        OrderItemRepository itemRepository,
                        SagaService sagaService) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.sagaService = sagaService;
    }

    // ------------------------------------------------------------------ create

    /**
     * Place an order: validate BR-ORD-001..004, persist header + items, start the
     * saga, emit OrderInitiated — one atomic transaction. Returns the order with
     * items and its fresh IN_PROGRESS saga attached.
     */
    @Transactional
    public ClinicalOrder create(Long patientId, Long encounterId, Long admissionId,
                                Long prescriberId, Long wardId, BigDecimal concessionPercent,
                                Long pharmacyPrescriptionId, List<ItemInput> itemsIn) {
        // BR-ORD-001
        if (patientId == null) {
            throw new InvalidOrderException("BEXA003", "patientId is required");
        }
        if (prescriberId == null) {
            throw new InvalidOrderException("BEXA003", "prescriberId is required");
        }
        if (encounterId != null && admissionId != null) {
            throw new InvalidOrderException("BEXA003",
                    "encounterId and admissionId are mutually exclusive — supply at most one");
        }
        // BR-ORD-002
        if (itemsIn == null || itemsIn.isEmpty()) {
            throw new InvalidOrderException("BEXA003",
                    "At least one order item {itemType,itemId,serviceName,rate,qty} is required");
        }
        for (ItemInput i : itemsIn) {
            if (i == null || i.itemType() == null) {
                throw new InvalidOrderException("BEXA003",
                        "Each item needs an itemType (LAB|RADIOLOGY|NURSING|CONSULTATION)");
            }
            try {
                com.hims.ordering.domain.model.OrderItemType.of(i.itemType());
            } catch (IllegalArgumentException e) {
                throw new InvalidOrderException("BEXA003", e.getMessage());
            }
            if (i.itemId() == null) {
                throw new InvalidOrderException("BEXA003",
                        "Each item needs a non-null itemId (" + i.itemType() + ")");
            }
            if (i.serviceName() == null || i.serviceName().isBlank()) {
                throw new InvalidOrderException("BEXA003",
                        "Each item needs a serviceName (itemId " + i.itemId() + ")");
            }
            if (i.rate() == null || i.rate().signum() < 0) {
                throw new InvalidOrderException("BEXA003",
                        "Item rate must be >= 0 for itemId " + i.itemId());
            }
            if (i.qty() == null || i.qty() <= 0) {
                throw new InvalidOrderException("BEXA003",
                        "Item qty must be > 0 for itemId " + i.itemId());
            }
        }
        BigDecimal pct = normalizeConcession(concessionPercent);
        // BR-ORD-004 bound validation up front (DB CHECK mirrors it; nothing persists on violation)
        for (ItemInput i : itemsIn) {
            BigDecimal sh = i.doctorSharePercent();
            if (sh != null && (sh.signum() < 0 || sh.compareTo(BigDecimal.valueOf(100)) > 0)) {
                throw new InvalidOrderException("BR-ORD-004",
                        "doctorSharePercent must be within [0,100] for itemId " + i.itemId());
            }
        }

        Timestamp nowTs = now();
        ClinicalOrder order = new ClinicalOrder();
        order.setOrderId(nextId("clinical_order_seq"));
        order.setPatientId(patientId);
        order.setEncounterId(encounterId);
        order.setAdmissionId(admissionId);
        order.setPrescriberId(prescriberId);
        order.setWardId(wardId);
        order.setStatusCode(ClinicalOrder.STATUS_INITIATED);
        order.setStatus(true);
        order.setSynchronizedd(false);
        order.setAddedBy(AUDIT_ACTOR_ID);
        order.setAddedDatetime(nowTs);
        order.setPharmacyPrescriptionId(pharmacyPrescriptionId);

        // BR-ORD-002/003 server-computed totals BEFORE persist (DB CHECKs mirror bounds)
        BigDecimal gross = BigDecimal.ZERO;
        for (ItemInput i : itemsIn) {
            gross = gross.add(i.rate().multiply(BigDecimal.valueOf(i.qty())));
        }
        gross = gross.setScale(2, RoundingMode.HALF_UP);
        BigDecimal concessionAmount = gross.multiply(pct)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        if (concessionAmount.compareTo(gross) > 0) {
            throw new InvalidOrderException("BR-ORD-003",
                    "Concession amount " + concessionAmount + " exceeds gross total " + gross);
        }
        order.setGrossTotal(gross);
        order.setConcessionPercent(pct);
        order.setConcessionAmount(concessionAmount);
        order.setNetTotal(gross.subtract(concessionAmount).setScale(2, RoundingMode.HALF_UP));
        orderRepository.save(order);

        List<OrderItem> savedItems = new ArrayList<>();
        long lineNo = 1L;
        for (ItemInput in : itemsIn) {
            OrderItem item = new OrderItem();
            item.setOrderId(order.getOrderId());
            item.setLineNo(lineNo++);
            item.setItemType(in.itemType().trim().toUpperCase());
            item.setItemId(in.itemId());
            item.setServiceName(in.serviceName().trim());
            item.setRate(in.rate());
            item.setQty(in.qty());
            BigDecimal share = in.doctorSharePercent() == null ? BigDecimal.ZERO : in.doctorSharePercent();
            item.setDoctorSharePercent(share.setScale(2, RoundingMode.HALF_UP));
            item.setStatus(true);
            item.setSynchronizedd(false);
            item.setAddedBy(AUDIT_ACTOR_ID);
            item.setAddedDatetime(nowTs);
            itemRepository.save(item);
            savedItems.add(item);
        }

        sagaService.start(order, savedItems);   // saga_state row + OrderInitiated, same tx
        log.info("Placed order {} for patient {} with {} item(s), saga started",
                order.getOrderId(), patientId, savedItems.size());
        return get(order.getOrderId());
    }

    /** BR-ORD-003 pre-validation: percent must be within [0,100] when supplied. */
    private BigDecimal normalizeConcession(BigDecimal concessionPercent) {
        if (concessionPercent == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        if (concessionPercent.signum() < 0 || concessionPercent.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new InvalidOrderException("BR-ORD-003",
                    "concessionPercent must be within [0,100]");
        }
        return concessionPercent.setScale(2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------ lifecycle

    /** Guard INITIATED only: 1 -> 3; compensates succeeded steps; emits OrderFailed. */
    @Transactional
    public ClinicalOrder cancel(long orderId) {
        ClinicalOrder order = load(orderId);
        if (!order.isInitiated()) {
            throw new InvalidOrderException("ORD_ILLEGAL_TRANSITION",
                    "Cannot cancel order " + orderId + " from status "
                            + statusName(order.getStatusCode())
                            + " — only INITIATED orders can be cancelled");
        }
        order.setStatusCode(ClinicalOrder.STATUS_CANCELLED);
        order.setUpdatedBy(AUDIT_ACTOR_ID);
        order.setUpdatedDatetime(now());
        orderRepository.save(order);
        sagaService.cancel(orderId);   // compensation + OrderFailed, same tx
        log.info("Cancelled order {}", orderId);
        return get(orderId);
    }

    /** Downstream success callback (see {@link SagaService#confirmStep}). */
    @Transactional
    public ClinicalOrder confirmStep(long orderId, String step) {
        sagaService.confirmStep(orderId, step);
        return get(orderId);
    }

    /** Downstream failure callback (see {@link SagaService#failStep}). */
    @Transactional
    public ClinicalOrder failStep(long orderId, String step, String reason) {
        sagaService.failStep(orderId, step, reason);
        return get(orderId);
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public ClinicalOrder get(long orderId) {
        ClinicalOrder order = load(orderId);
        order.setItems(itemRepository.findByOrderIdOrderByLineNoAsc(orderId));
        order.setSaga(sagaService.byOrder(orderId));
        return order;
    }

    @Transactional(readOnly = true)
    public List<ClinicalOrder> list(Short status, Long patientId) {
        List<ClinicalOrder> orders = orderRepository.search(status, patientId);
        for (ClinicalOrder o : orders) {
            o.setItems(itemRepository.findByOrderIdOrderByLineNoAsc(o.getOrderId()));
            o.setSaga(sagaService.byOrder(o.getOrderId()));
        }
        return orders;
    }

    // ------------------------------------------------------------------ helpers

    private ClinicalOrder load(long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("NOT_FOUND",
                        "Order not found: " + orderId));
    }

    static String statusName(short s) {
        return switch (s) {
            case 1 -> "INITIATED";
            case 2 -> "CONFIRMED";
            case 3 -> "CANCELLED";
            case 4 -> "FAILED";
            default -> String.valueOf(s);
        };
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_ordering." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }

    /** One requested order item at creation. */
    public record ItemInput(String itemType, Long itemId, String serviceName,
                            BigDecimal rate, Integer qty, BigDecimal doctorSharePercent) {}
}
