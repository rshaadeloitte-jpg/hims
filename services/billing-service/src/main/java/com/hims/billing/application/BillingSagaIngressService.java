package com.hims.billing.application;

import com.hims.billing.application.exceptions.InvalidBillingRequestException;
import com.hims.billing.domain.model.Bill;
import com.hims.billing.domain.model.Charge;
import com.hims.billing.domain.model.ConsumedEvent;
import com.hims.billing.domain.repository.BillRepository;
import com.hims.billing.domain.repository.ChargeRepository;
import com.hims.billing.domain.repository.ConsumedEventRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Order-saga CONSUMER side (WP-S1, Catalog #5): consumes E-13 OrderCreated
 * (type string 'OrderCreated', schema token order-created.v1, topic
 * ihms.ordering.events) and creates the bill DRAFT + one t_charge per line,
 * idempotent per envelope eventId.
 *
 * TRANSPORT STUB (disclosed at every site): production is a Kafka listener on
 * ihms.ordering.events; the contract-stubbed stand-in is POST
 * /api/v1/internal/events fed with the EXACT outbox envelope in tests. The
 * ingress logic itself is transport-agnostic.
 *
 * IDEMPOTENCY (three layers):
 *   1. fast path: t_consumed_event lookup by eventId -> DUPLICATE / rejected-replay;
 *   2. DB race backstop: the ledger claim is INSERT ... ON CONFLICT (event_id)
 *      DO NOTHING — a concurrent duplicate delivery loses the race atomically;
 *   3. second backstop: uq_t_bill_saga_event partial UNIQUE on t_bill.
 * A replayed eventId returns the EXISTING outcome (200 DUPLICATE / the stored
 * rejection) and writes NOTHING new.
 *
 * VALIDATION FAILURE -> structured REJECTED result (HTTP 422): the rejection is
 * recorded on the ledger row (status REJECTED + reason) and NO bill/charge rows
 * are written; the relay maps it to POST /api/v1/internal/orders/{id}/billing-failed
 * on ordering-service (saga compensation PLACED -> BILLING_FAILED).
 *
 * EVENT POLICY: this ingress EMITS NOTHING to the outbox — E-07 InvoiceGenerated
 * stays exclusively on finalize (negative outbox assertion pinned by tests).
 */
@Service
public class BillingSagaIngressService {

    private static final Logger log = LoggerFactory.getLogger(BillingSagaIngressService.class);

    public static final String EV_ORDER_CREATED = "OrderCreated";            // E-13 type string
    public static final String SCHEMA_ORDER_CREATED = "order-created.v1";    // E-13 schema token

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final ConsumedEventRepository consumedRepository;
    private final BillRepository billRepository;
    private final ChargeRepository chargeRepository;

    @PersistenceContext
    private EntityManager em;

    public BillingSagaIngressService(ConsumedEventRepository consumedRepository,
                                     BillRepository billRepository,
                                     ChargeRepository chargeRepository) {
        this.consumedRepository = consumedRepository;
        this.billRepository = billRepository;
        this.chargeRepository = chargeRepository;
    }

    /** One ingress outcome; {@code status} is APPLIED | DUPLICATE | REJECTED. */
    public record IngestResult(String status, String eventId, String eventType,
                               Long billId, Long sourceOrderId, Integer chargeCount,
                               String errorCode, String message) {

        public static IngestResult applied(String eventId, Long billId, Long orderId, int charges) {
            return new IngestResult("APPLIED", eventId, EV_ORDER_CREATED, billId, orderId, charges, null, null);
        }

        public static IngestResult duplicate(String eventId, Long billId, Long orderId) {
            return new IngestResult("DUPLICATE", eventId, EV_ORDER_CREATED, billId, orderId, null, null,
                    "Event already ingested — existing draft returned, nothing written");
        }

        public static IngestResult rejected(String eventId, Long orderId, String errorCode, String message) {
            return new IngestResult("REJECTED", eventId, EV_ORDER_CREATED, null, orderId, null, errorCode, message);
        }
    }

    /**
     * Ingest one E-13 OrderCreated envelope. Envelope guards (eventId/eventType
     * required, eventType must be E-13) reject with 400 BEFORE any ledger write.
     * Payload validation failures record a REJECTED ledger row and return the
     * structured failure (422 at the API boundary). Emits NOTHING.
     */
    @Transactional
    public IngestResult ingest(String eventId, String eventType, Map<String, Object> payload) {
        if (eventId == null || eventId.isBlank()) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID",
                    "eventId is required (outbox envelope idempotency key)");
        }
        if (eventId.length() > 64) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID",
                    "eventId must be at most 64 characters");
        }
        if (eventType == null || eventType.isBlank()) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID", "eventType is required");
        }
        if (!EV_ORDER_CREATED.equals(eventType)) {
            throw new InvalidBillingRequestException("BILL_UNKNOWN_EVENT_TYPE",
                    "Unknown eventType: " + eventType + " — this consumer handles only "
                            + EV_ORDER_CREATED + " (" + SCHEMA_ORDER_CREATED + ")");
        }
        if (payload == null) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID", "payload is required");
        }

        // ---- fast path: exact replay of an already-ingested event -------------
        var existing = consumedRepository.findByEventId(eventId);
        if (existing.isPresent()) {
            return outcomeOf(existing.get());
        }

        // ---- atomic claim: INSERT ... ON CONFLICT DO NOTHING (race backstop #1)
        Timestamp nowTs = now();
        int claimed = em.createNativeQuery("""
                INSERT INTO ihms_billing.t_consumed_event
                    (consumed_id, event_id, event_type, status, ingested_at, added_by, added_datetime)
                VALUES (nextval('ihms_billing.t_consumed_event_seq'), ?, ?, '%s', ?, ?, ?)
                ON CONFLICT (event_id) DO NOTHING
                """.formatted(ConsumedEvent.STATUS_APPLIED))
                .setParameter(1, eventId)
                .setParameter(2, eventType)
                .setParameter(3, Timestamp.from(Instant.now()))
                .setParameter(4, AUDIT_ACTOR_ID)
                .setParameter(5, nowTs)
                .executeUpdate();
        if (claimed == 0) {
            // lost a concurrent race — the winning tx owns the outcome
            ConsumedEvent winner = consumedRepository.findByEventId(eventId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Unique constraint hit but no ledger row found for " + eventId));
            log.info("Lost ingest race for {} — returning winner outcome {}", eventId, winner.getStatus());
            return outcomeOf(winner);
        }

        // ---- payload validation (bill-side semantics; producer mirrors these) -
        Long sourceOrderId = requireLong(payload, "orderId");
        try {
            validateDraftPayload(payload);
        } catch (InvalidBillingRequestException e) {
            markRejected(eventId, sourceOrderId, e.getErrorCode(), e.getMessage());
            log.info("Rejected {} for order {}: {}", eventId, sourceOrderId, e.getMessage());
            return IngestResult.rejected(eventId, sourceOrderId, e.getErrorCode(), e.getMessage());
        }

        // ---- create the DRAFT bill + one charge per line ----------------------
        Bill bill = new Bill();
        bill.setBillId(nextId("t_bill_seq"));
        bill.setPatientId(requireLong(payload, "patientId"));
        bill.setEncounterId(optLong(payload.get("encounterId")));
        bill.setAdmissionId(optLong(payload.get("admissionId")));
        bill.setCashCounterId(1L);
        bill.setPrescriberDoctorId(optLong(payload.get("prescriberId")));
        bill.setIsSelfCompany(true);
        bill.setBillNo(Bill.PLACEHOLDER_BILL_NO);          // BR-BILL-001 placeholder
        bill.setStatus(true);
        bill.setIsSettled(false);
        bill.setIsCancelled(false);
        bill.setIsInterim(true);                           // DRAFT == interim until finalize
        bill.setSagaSourceOrderId(sourceOrderId);
        bill.setSagaEventId(eventId);                      // race backstop #2 (partial UNIQUE)
        bill.setSynchronizedd(false);
        bill.setAddedBy(AUDIT_ACTOR_ID);
        bill.setAddedDatetime(nowTs);
        // persist the header FIRST so t_charge.bill_id FK holds when lines flush
        billRepository.save(bill);

        List<Map<String, Object>> lines = linesOf(payload);
        List<Charge> created = new ArrayList<>();
        for (Map<String, Object> line : lines) {
            Charge c = new Charge();
            c.setChargeId(nextId("t_charge_seq"));
            c.setBillId(bill.getBillId());                 // attached directly to the draft
            c.setServiceId(requireLong(line, "itemId"));
            c.setServiceName(String.valueOf(line.get("serviceName")).trim());
            c.setRate(new BigDecimal(String.valueOf(line.get("rate")).trim()));
            c.setQty(Integer.valueOf(String.valueOf(line.get("qty")).trim()));
            c.setStatus(true);
            c.setSynchronizedd(false);
            c.setAddedBy(AUDIT_ACTOR_ID);
            c.setAddedDatetime(nowTs);
            created.add(chargeRepository.save(c));
        }

        // explicit flush: relay consumers (and tests reading via JdbcTemplate / other
        // connections) must observe the committed draft atomically with the ledger claim
        em.flush();
        log.info("Ingested {} for order {} -> bill DRAFT {} with {} charge(s)",
                eventId, sourceOrderId, bill.getBillId(), created.size());
        return IngestResult.applied(eventId, bill.getBillId(), sourceOrderId, created.size());
    }

    // ------------------------------------------------------------------ helpers

    /** Map a persisted ledger row to its replay-stable outcome. */
    private IngestResult outcomeOf(ConsumedEvent row) {
        if (ConsumedEvent.STATUS_REJECTED.equals(row.getStatus())) {
            return IngestResult.rejected(row.getEventId(), row.getSourceOrderId(),
                    "BILL_REQ_INVALID", row.getRejectionReason());
        }
        Bill draft = billRepository.findBySagaEventId(row.getEventId()).orElse(null);
        return IngestResult.duplicate(row.getEventId(),
                draft == null ? null : draft.getBillId(), row.getSourceOrderId());
    }

    private void markRejected(String eventId, Long sourceOrderId, String errorCode, String message) {
        ConsumedEvent row = consumedRepository.findByEventId(eventId)
                .orElseThrow(() -> new IllegalStateException("Ledger row vanished for " + eventId));
        row.setSourceOrderId(sourceOrderId);
        row.setStatus(ConsumedEvent.STATUS_REJECTED);
        String reason = errorCode + ": " + message;
        row.setRejectionReason(reason.length() > 500 ? reason.substring(0, 500) : reason);
        consumedRepository.save(row);
        em.flush();   // the structured rejection must be durable before the response returns
    }

    /** Exact bill-side semantics the ordering producer validates before emitting E-13. */
    private void validateDraftPayload(Map<String, Object> payload) {
        requireLong(payload, "patientId");
        List<Map<String, Object>> lines = linesOf(payload);
        if (lines.isEmpty()) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID",
                    "At least one charge is required (zero lines)");
        }
        boolean hasEncounter = optLong(payload.get("encounterId")) != null;
        boolean hasAdmission = optLong(payload.get("admissionId")) != null;
        if (hasEncounter && hasAdmission) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID",
                    "encounterId and admissionId are mutually exclusive");
        }
        for (Map<String, Object> line : lines) {
            if (line.get("itemId") == null) {
                throw new InvalidBillingRequestException("BILL_REQ_INVALID",
                        "Each line needs a non-null itemId");
            }
            requireLong(line, "itemId");
            Object name = line.get("serviceName");
            if (name == null || String.valueOf(name).isBlank()) {
                throw new InvalidBillingRequestException("BILL_REQ_INVALID",
                        "serviceName is required for itemId " + line.get("itemId"));
            }
            BigDecimal rate = requireAmount(line);
            if (rate.signum() < 0) {
                throw new InvalidBillingRequestException("BILL_REQ_INVALID",
                        "Line rate must be >= 0 for itemId " + line.get("itemId"));
            }
            int qty = requireQty(line);
            if (qty <= 0) {
                throw new InvalidBillingRequestException("BILL_REQ_INVALID",
                        "Line qty must be > 0 for itemId " + line.get("itemId"));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> linesOf(Map<String, Object> payload) {
        Object raw = payload.get("lines");
        if (raw == null) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID", "lines is required");
        }
        if (!(raw instanceof List<?> list)) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID", "lines must be a JSON array");
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object o : list) {
            if (!(o instanceof Map)) {
                throw new InvalidBillingRequestException("BILL_REQ_INVALID",
                        "Each line must be a JSON object");
            }
            out.add((Map<String, Object>) o);
        }
        return out;
    }

    private long requireLong(Map<String, Object> map, String field) {
        Object v = map.get(field);
        if (v == null) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID",
                    field + " is required in the event payload");
        }
        try {
            return Long.parseLong(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID",
                    field + " must be numeric: " + v);
        }
    }

    private BigDecimal requireAmount(Map<String, Object> map) {
        Object v = map.get("rate");
        if (v == null) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID",
                    "rate is required in every line");
        }
        try {
            return new BigDecimal(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID",
                    "rate must be numeric: " + v);
        }
    }

    private int requireQty(Map<String, Object> map) {
        Object v = map.get("qty");
        if (v == null) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID",
                    "qty is required in every line");
        }
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID",
                    "qty must be an integer: " + v);
        }
    }

    private Long optLong(Object v) {
        if (v == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID",
                    "id field must be numeric or null: " + v);
        }
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_billing." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }
}
