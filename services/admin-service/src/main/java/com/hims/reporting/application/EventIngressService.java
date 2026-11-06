package com.hims.reporting.application;

import com.hims.reporting.application.exceptions.InvalidReportingRequestException;
import com.hims.reporting.domain.model.IngestedEvent;
import com.hims.reporting.domain.model.RmPatientRegistry;
import com.hims.reporting.domain.repository.IngestedEventRepository;
import com.hims.reporting.domain.repository.RmEncounterStatsRepository;
import com.hims.reporting.domain.repository.RmPatientRegistryRepository;
import com.hims.reporting.domain.repository.RmOpStatsRepository;
import com.hims.reporting.domain.repository.RmRevenueDailyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * INTERNAL event-relay ingress feeding the rm_* read models (register WP-T2).
 * Production would be a Kafka consumer over PatientRegistered /
 * EncounterCreated / InvoiceGenerated / PaymentReceived (KAFKA-EVENT-CATALOG);
 * the POST /api/v1/internal/events API is the contract-stubbed stand-in —
 * disclosed at every site, never presented as real connectivity.
 *
 * STRICT IDEMPOTENCY (register: "re-ingest same event => no double count"):
 *   - PatientRegistered: upsert rm_patient_registry by patient_id (the SOURCE id
 *     IS the PK -> re-ingest updates in place, never duplicates).
 *   - EncounterCreated / InvoiceGenerated / PaymentReceived: accumulating read
 *     models dedupe via the t_ingested_event ledger keyed by the source document
 *     id (encounterId / invoiceId / paymentId) — a replay short-circuits BEFORE
 *     any increment/accumulation happens (pinned by tests).
 *
 * OUT OF SCOPE (comment-only per register WP-T2): Jasper/Velocity rendering,
 * regulatory exports (GSTR/NHM/RGHS), NMC/PCTS push jobs. This service EMITS
 * NOTHING to the outbox (negative outbox assertion at L0 + L2).
 */
@Service
public class EventIngressService {

    private static final Logger log = LoggerFactory.getLogger(EventIngressService.class);

    public static final String EV_PATIENT_REGISTERED = "PatientRegistered";
    public static final String EV_ENCOUNTER_CREATED = "EncounterCreated";
    public static final String EV_INVOICE_GENERATED = "InvoiceGenerated";
    public static final String EV_PAYMENT_RECEIVED = "PaymentReceived";
    public static final String EV_OT_COMPLETED = "OtCompleted";
    public static final String EV_LAB_VERIFIED = "LabVerified";
    public static final String EV_BLOOD_ISSUED = "BloodBagIssued";

    public static final String DEFAULT_HEAD_CODE = "OPD";
    public static final String DEFAULT_ENCOUNTER_TYPE = "OPD";

    public static final String STATUS_APPLIED = "APPLIED";
    public static final String STATUS_DUPLICATE = "DUPLICATE";

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    @PersistenceContext
    private EntityManager em;

    private final RmPatientRegistryRepository patientRepository;
    private final RmEncounterStatsRepository encounterRepository;
    private final RmRevenueDailyRepository revenueRepository;
    private final RmOpStatsRepository opStatsRepository;
    private final IngestedEventRepository ledgerRepository;

    public EventIngressService(RmPatientRegistryRepository patientRepository,
                               RmEncounterStatsRepository encounterRepository,
                               RmRevenueDailyRepository revenueRepository,
                               RmOpStatsRepository opStatsRepository,
                               IngestedEventRepository ledgerRepository) {
        this.patientRepository = patientRepository;
        this.encounterRepository = encounterRepository;
        this.revenueRepository = revenueRepository;
        this.opStatsRepository = opStatsRepository;
        this.ledgerRepository = ledgerRepository;
    }

    /** One ingress outcome: APPLIED (first delivery) or DUPLICATE (replay, no-op). */
    public record IngestResult(String status, String eventType, String eventKey) {}

    /**
     * Route one relayed event onto the read models. Guards: eventType required
     * and must be one of the four supported types (else 400
     * REPORTING_UNKNOWN_EVENT_TYPE); the source document id required by the type
     * must be present (else 400 BEXA003); amounts must be >= 0. Idempotent:
     * replays return DUPLICATE and touch NO read model. Emits NOTHING.
     */
    @Transactional
    public IngestResult ingest(String eventType, Map<String, Object> payload) {
        if (eventType == null || eventType.isBlank()) {
            throw new InvalidReportingRequestException("BEXA003", "eventType is required");
        }
        if (payload == null) {
            throw new InvalidReportingRequestException("BEXA003", "payload is required");
        }
        return switch (eventType) {
            case EV_PATIENT_REGISTERED -> ingestPatient(payload);
            case EV_ENCOUNTER_CREATED -> ingestEncounter(payload);
            case EV_INVOICE_GENERATED -> ingestRevenue(EV_INVOICE_GENERATED, "invoice", payload);
            case EV_PAYMENT_RECEIVED -> ingestRevenue(EV_PAYMENT_RECEIVED, "payment", payload);
            case EV_OT_COMPLETED, EV_LAB_VERIFIED, EV_BLOOD_ISSUED -> ingestOpStats(eventType, payload);
            default -> throw new InvalidReportingRequestException("REPORTING_UNKNOWN_EVENT_TYPE",
                    "Unknown eventType: " + eventType + " — supported: "
                            + EV_PATIENT_REGISTERED + ", " + EV_ENCOUNTER_CREATED + ", "
                            + EV_INVOICE_GENERATED + ", " + EV_PAYMENT_RECEIVED + ", "
                            + EV_OT_COMPLETED + ", " + EV_LAB_VERIFIED + ", " + EV_BLOOD_ISSUED);
        };
    }

    // ------------------------------------------------------------------ handlers

    /**
     * PatientRegistered -> UPSERT rm_patient_registry by patient_id. Re-ingest of
     * the same patient updates mrn/name/registered_at in place (never a second
     * row) — naturally idempotent WITHOUT the ledger; the ledger row still records
     * the delivery for uniform observability. Emits NOTHING.
     */
    private IngestResult ingestPatient(Map<String, Object> payload) {
        long patientId = requireLong(payload, "patientId");
        String eventKey = "patient:" + patientId;
        if (ledgerRepository.existsByEventTypeAndEventKey(EV_PATIENT_REGISTERED, eventKey)) {
            return new IngestResult(STATUS_DUPLICATE, EV_PATIENT_REGISTERED, eventKey);
        }

        Timestamp nowTs = now();
        RmPatientRegistry row = patientRepository.findById(patientId).orElseGet(() -> {
            RmPatientRegistry fresh = new RmPatientRegistry();
            fresh.setPatientId(patientId);
            fresh.setAddedBy(AUDIT_ACTOR_ID);
            fresh.setAddedDatetime(nowTs);
            return fresh;
        });
        row.setMrn(optString(payload.get("mrn")));
        row.setName(optString(payload.get("name")));
        row.setRegisteredAt(parseTimestamp(payload.get("registeredAt"), nowTs));
        row.setUpdatedBy(AUDIT_ACTOR_ID);
        row.setUpdatedDatetime(nowTs);
        patientRepository.save(row);

        recordLedger(EV_PATIENT_REGISTERED, eventKey, nowTs);
        log.info("Ingress {}: {} (upsert)", EV_PATIENT_REGISTERED, eventKey);
        return new IngestResult(STATUS_APPLIED, EV_PATIENT_REGISTERED, eventKey);
    }

    /**
     * EncounterCreated -> INCREMENT rm_encounter_stats cnt by 1 for
     * (statDate, encounterType). Defaults: statDate=today, encounterType=OPD.
     * Ledger-guarded: a replayed encounterId adds NOTHING (strict idempotency).
     * Emits NOTHING.
     */
    private IngestResult ingestEncounter(Map<String, Object> payload) {
        long encounterId = requireLong(payload, "encounterId");
        String eventKey = "encounter:" + encounterId;
        if (ledgerRepository.existsByEventTypeAndEventKey(EV_ENCOUNTER_CREATED, eventKey)) {
            return new IngestResult(STATUS_DUPLICATE, EV_ENCOUNTER_CREATED, eventKey);
        }

        LocalDate statDate = parseDate(payload.get("statDate"), LocalDate.now());
        String encounterType = optString(payload.get("encounterType"));
        if (encounterType == null || encounterType.isBlank()) {
            encounterType = DEFAULT_ENCOUNTER_TYPE;
        } else if (encounterType.length() > 20) {
            throw new InvalidReportingRequestException("BEXA003",
                    "encounterType must be at most 20 characters");
        }

        Timestamp nowTs = now();
        encounterRepository.increment(statDate, encounterType);
        recordLedger(EV_ENCOUNTER_CREATED, eventKey, nowTs);
        log.info("Ingress {}: {} (+1 {} on {})", EV_ENCOUNTER_CREATED, eventKey, encounterType, statDate);
        return new IngestResult(STATUS_APPLIED, EV_ENCOUNTER_CREATED, eventKey);
    }

    /**
     * InvoiceGenerated | PaymentReceived -> ACCUMULATE rm_revenue_daily total +=
     * amount for (statDate, headCode). headCode defaults to OPD when absent.
     * Ledger-guarded by source document id (invoiceId / paymentId): a replayed
     * event adds NOTHING — double-ingest can never double count (pinned).
     * Emits NOTHING.
     */
    private IngestResult ingestRevenue(String eventType, String prefix, Map<String, Object> payload) {
        String idField = prefix + "Id";
        long documentId = requireLong(payload, idField);
        BigDecimal amount = requireAmount(payload);
        String eventKey = prefix + ":" + documentId;
        if (ledgerRepository.existsByEventTypeAndEventKey(eventType, eventKey)) {
            return new IngestResult(STATUS_DUPLICATE, eventType, eventKey);
        }

        LocalDate statDate = parseDate(payload.get("statDate"), LocalDate.now());
        String headCode = optString(payload.get("headCode"));
        if (headCode == null || headCode.isBlank()) {
            headCode = DEFAULT_HEAD_CODE;
        } else if (headCode.length() > 20) {
            throw new InvalidReportingRequestException("BEXA003",
                    "headCode must be at most 20 characters");
        }

        Timestamp nowTs = now();
        revenueRepository.accumulate(statDate, headCode, amount);
        recordLedger(eventType, eventKey, nowTs);
        log.info("Ingress {}: {} (+={} to {} on {})", eventType, eventKey, amount, headCode, statDate);
        return new IngestResult(STATUS_APPLIED, eventType, eventKey);
    }

    // ------------------------------------------------------------------ helpers

    private void recordLedger(String eventType, String eventKey, Timestamp nowTs) {
        IngestedEvent ledgerRow = new IngestedEvent();
        ledgerRow.setIngestedId(nextId("t_ingested_event_seq"));
        ledgerRow.setEventType(eventType);
        ledgerRow.setEventKey(eventKey);
        ledgerRow.setIngestedAt(nowTs);
        ledgerRow.setAddedBy(AUDIT_ACTOR_ID);
        ledgerRow.setAddedDatetime(nowTs);
        try {
            ledgerRepository.save(ledgerRow);
        } catch (DataIntegrityViolationException e) {
            // concurrent duplicate delivery raced past the existence check —
            // uq_ingested_event wins; treat as DUPLICATE (no read-model change:
            // the racing tx rolled back with our save)
            throw new DuplicateIngestRaceException(eventType, eventKey);
        }
    }

    /** Internal marker so a raced duplicate surfaces as DUPLICATE semantics, not a 500. */
    public static class DuplicateIngestRaceException extends RuntimeException {
        public DuplicateIngestRaceException(String eventType, String eventKey) {
            super("Duplicate ingest raced for " + eventType + " " + eventKey);
        }
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_reporting." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }

    long requireLong(Map<String, Object> payload, String field) {
        Object v = payload.get(field);
        if (v == null) {
            throw new InvalidReportingRequestException("BEXA003",
                    field + " is required in the event payload");
        }
        try {
            return Long.parseLong(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            throw new InvalidReportingRequestException("BEXA003",
                    field + " must be numeric: " + v);
        }
    }

    BigDecimal requireAmount(Map<String, Object> payload) {
        Object v = payload.get("amount");
        if (v == null) {
            throw new InvalidReportingRequestException("BEXA003",
                    "amount is required in the event payload");
        }
        try {
            BigDecimal amount = new BigDecimal(String.valueOf(v).trim());
            if (amount.signum() < 0) {
                throw new InvalidReportingRequestException("BEXA003",
                        "amount must be >= 0");
            }
            return amount;
        } catch (NumberFormatException e) {
            throw new InvalidReportingRequestException("BEXA003",
                    "amount must be numeric: " + v);
        }
    }

    public static LocalDate parseDateStrict(Object raw, String fieldName) {
        if (raw == null || String.valueOf(raw).isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(String.valueOf(raw).trim().substring(0, 10));
        } catch (DateTimeParseException e) {
            throw new InvalidReportingRequestException("BEXA003",
                    "Invalid " + fieldName + " (expected yyyy-MM-dd): " + raw);
        }
    }

    LocalDate parseDate(Object raw, LocalDate fallback) {
        if (raw == null || String.valueOf(raw).isBlank()) {
            return fallback;
        }
        try {
            return LocalDate.parse(String.valueOf(raw).trim().substring(0, 10));
        } catch (DateTimeParseException e) {
            throw new InvalidReportingRequestException("BEXA003",
                    "Invalid date (expected yyyy-MM-dd): " + raw);
        }
    }

    Timestamp parseTimestamp(Object raw, Timestamp fallback) {
        if (raw == null || String.valueOf(raw).isBlank()) {
            return fallback;
        }
        String s = String.valueOf(raw).trim();
        try {
            return Timestamp.from(Instant.parse(s));
        } catch (DateTimeParseException ignored) {
            // fall through to space-separated legacy form
        }
        try {
            return Timestamp.valueOf(s.length() == 10 ? s + " 00:00:00" : s.replace('T', ' '));
        } catch (IllegalArgumentException e) {
            throw new InvalidReportingRequestException("BEXA003",
                    "Invalid timestamp: " + raw);
        }
    }

    static String optString(Object raw) {
        return raw == null ? null : String.valueOf(raw);
    }

    static Timestamp now() {
        return Timestamp.from(Instant.now());
    }
    /**
     * OtCompleted | LabVerified | BloodBagIssued -> INCREMENT rm_op_stats cnt by 1
     * for (statDate, opType). Defaults: statDate=today, opType derived from eventType.
     * Ledger-guarded: a replayed opId adds NOTHING (strict idempotency).
     */
    private IngestResult ingestOpStats(String eventType, Map<String, Object> payload) {
        String opId = optString(payload.get("opId"));
        if (opId == null || opId.isBlank()) {
            throw new InvalidReportingRequestException("BEXA003", "opId is required for " + eventType);
        }
        String eventKey = eventType + ":" + opId;
        if (ledgerRepository.existsByEventTypeAndEventKey(eventType, eventKey)) {
            return new IngestResult(STATUS_DUPLICATE, eventType, eventKey);
        }
        LocalDate statDate = parseDate(payload.get("statDate"), LocalDate.now());
        String opType = optString(payload.get("opType"));
        if (opType == null || opType.isBlank()) {
            opType = switch (eventType) {
                case EV_OT_COMPLETED -> "OT";
                case EV_LAB_VERIFIED -> "LAB";
                case EV_BLOOD_ISSUED -> "BLOOD";
                default -> "GENERIC";
            };
        }
        Timestamp nowTs = now();
        var key = new com.hims.reporting.domain.model.RmOpStats.Key(java.sql.Date.valueOf(statDate), opType);
        var row = opStatsRepository.findById(key).orElseGet(() -> {
            com.hims.reporting.domain.model.RmOpStats fresh = new com.hims.reporting.domain.model.RmOpStats();
            fresh.setId(key);
            fresh.setCnt(0);
            return fresh;
        });
        row.setCnt((row.getCnt() == null ? 0 : row.getCnt()) + 1);
        opStatsRepository.save(row);
        recordLedger(eventType, eventKey, nowTs);
        log.info("Ingress {}: {} (+1 {} on {})", eventType, eventKey, opType, statDate);
        return new IngestResult(STATUS_APPLIED, eventType, eventKey);
    }
}
