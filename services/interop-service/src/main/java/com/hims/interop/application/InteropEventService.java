package com.hims.interop.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.interop.application.exceptions.InvalidRequestException;
import com.hims.interop.domain.model.OutboundHis;
import com.hims.interop.domain.repository.OutboundHisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Internal event-relay ingress (register WP-T1) — models the FUTURE KAFKA
 * CONSUMER that will feed the interop queues; production cutover replaces HTTP
 * ingress with a Kafka listener (in-flight continuity across the cutover is a
 * deployment concern, NOT modeled here).
 *
 * Routing (eventType -&gt; t_outbound_his target_system):
 *   InvoiceGenerated / PaymentReceived          -&gt; HL7  (billing revenue events)
 *   LabResultFinalized                          -&gt; EHR
 *   PatientRegistered / EncounterCreated /
 *   PatientAdmitted / PatientDischarged         -&gt; HL7  (patient/encounter/ADT feed)
 *   anything else                               -&gt; 400 INTEROP_UNKNOWN_EVENT_TYPE
 * (event names are the KAFKA-EVENT-CATALOG E-01..E-08 identifiers.)
 *
 * IDEMPOTENCY: unique per (eventType, aggregateId) — duplicate ingress returns
 * the EXISTING row and writes nothing new (pinned by tests at L0 and L2,
 * including the concurrent-insert race via the DB UNIQUE constraint).
 *
 * EVENT POLICY: this service EMITS NOTHING — no outbox write on any path here;
 * asserted by tests.
 */
@Service
public class InteropEventService {

    private static final Logger log = LoggerFactory.getLogger(InteropEventService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    /** Revenue events -> HL7 target. */
    public static final Set<String> HL7_REVENUE_EVENTS = Set.of("InvoiceGenerated", "PaymentReceived");
    /** Lab finalize -> EHR target. */
    public static final Set<String> EHR_EVENTS = Set.of("LabResultFinalized");
    /** Patient/encounter/admission/discharge feed -> HL7 target. */
    public static final Set<String> HL7_PATIENT_EVENTS = Set.of(
            "PatientRegistered", "EncounterCreated", "PatientAdmitted", "PatientDischarged");

    public static final Set<String> KNOWN_EVENTS = Set.copyOf(
            java.util.stream.Stream.concat(
                    java.util.stream.Stream.concat(HL7_REVENUE_EVENTS.stream(), EHR_EVENTS.stream()),
                    HL7_PATIENT_EVENTS.stream()).toList());

    private final OutboundHisRepository outboundRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    public InteropEventService(OutboundHisRepository outboundRepository, ObjectMapper objectMapper) {
        this.outboundRepository = outboundRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Ingest one sibling-service event into the outbound queue.
     *
     * @return {@link IngestResult} with created=true when a NEW row was written,
     *         false when an existing row was returned (duplicate ingress).
     */
    @Transactional
    public IngestResult ingest(String eventType, String aggregateId, Map<String, Object> payload) {
        if (eventType == null || eventType.isBlank()) {
            throw new InvalidRequestException("BEXA003", "eventType is required");
        }
        if (aggregateId == null || aggregateId.isBlank()) {
            throw new InvalidRequestException("BEXA003",
                    "aggregateId is required (idempotency key per eventType+aggregateId)");
        }
        if (aggregateId.length() > 40) {
            throw new InvalidRequestException("BEXA003",
                    "aggregateId must be at most 40 characters");
        }
        String target = route(eventType);

        // Idempotent fast path: same (eventType, aggregateId) already ingested.
        var existing = outboundRepository.findByEventTypeAndAggregateId(eventType, aggregateId);
        if (existing.isPresent()) {
            log.info("Duplicate ingress for {}:{} — returning existing row {}", eventType, aggregateId,
                    existing.get().getId());
            return new IngestResult(existing.get(), false);
        }

        OutboundHis row = new OutboundHis();
        row.setId(nextId("t_outbound_his_seq"));
        row.setEventType(eventType);
        row.setAggregateId(aggregateId);
        row.setPayload(toJson(payload));
        row.setTargetSystem(target);
        row.setStatus(OutboundHis.STATUS_PENDING);
        row.setAddedBy(AUDIT_ACTOR_ID);
        row.setAddedDatetime(now());
        try {
            outboundRepository.save(row);
        } catch (DataIntegrityViolationException e) {
            // race with a concurrent ingest of the same event — uq_t_outbound_event fired;
            // translate to the documented idempotent outcome (no duplicate row).
            OutboundHis winner = outboundRepository.findByEventTypeAndAggregateId(eventType, aggregateId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Unique constraint hit but no row found for " + eventType + ":" + aggregateId));
            return new IngestResult(winner, false);
        }
        log.info("Ingress {}:{} -> t_outbound_his[{}] ({})", eventType, aggregateId, target, row.getId());
        return new IngestResult(row, true);
    }

    /** Route an event type to its outbound target system (400 for unknown types). */
    public static String route(String eventType) {
        if (HL7_REVENUE_EVENTS.contains(eventType) || HL7_PATIENT_EVENTS.contains(eventType)) {
            return OutboundHis.TARGET_HL7;
        }
        if (EHR_EVENTS.contains(eventType)) {
            return OutboundHis.TARGET_EHR;
        }
        throw new InvalidRequestException("INTEROP_UNKNOWN_EVENT_TYPE",
                "Unknown eventType: " + eventType + " — known events: "
                        + KNOWN_EVENTS.stream().sorted());
    }

    @Transactional(readOnly = true)
    public java.util.List<OutboundHis> list(Short status, String target) {
        if (status != null && target != null) {
            return outboundRepository.findByStatusAndTargetSystemIgnoreCaseOrderByIdDesc(status, target);
        }
        if (status != null) {
            return outboundRepository.findByStatusOrderByIdDesc(status);
        }
        if (target != null) {
            return outboundRepository.findByTargetSystemIgnoreCaseOrderByIdDesc(target);
        }
        return outboundRepository.findAllByOrderByIdDesc();
    }

    // ------------------------------------------------------------------ helpers

    private String toJson(Map<String, Object> payload) {
        if (payload == null) {
            throw new InvalidRequestException("BEXA003", "payload is required");
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new InvalidRequestException("BEXA003", "payload must be a JSON object");
        }
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_interop." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }

    /** Ingress outcome: the queue row plus whether THIS call created it. */
    public record IngestResult(OutboundHis row, boolean created) {}
}
