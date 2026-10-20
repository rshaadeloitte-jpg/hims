package com.hims.interop.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.interop.application.exceptions.ConflictException;
import com.hims.interop.application.exceptions.InteropNotFoundException;
import com.hims.interop.application.exceptions.InvalidRequestException;
import com.hims.interop.domain.model.AbdmRequestQueue;
import com.hims.interop.domain.repository.AbdmRequestQueueRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * ABDM request queue (register WP-T1). correlation_id UNIQUE — duplicates are a
 * 409 INTEROP_DUPLICATE_CORRELATION_ID (API check + DB constraint backstop).
 * Callbacks via PUT .../{correlationId}/callback are IDEMPOTENT: first callback
 * wins; repeats (even with a different status/payload) are no-ops returning the
 * stored state — mirrors the legacy Redis ABDM_REQ_/ABDM_RES_ consume-once
 * semantics (07 §ABDM).
 *
 * OUT OF SCOPE (comment-only): the actual ABDM gateway call (HIP/HIU/HFR/CM),
 * JWE/JWS payload encryption, session-token handling (ABDM_SES_TKN) and the
 * eSanchar pushes. The queue row IS the deliverable.
 */
@Service
public class AbdmService {

    private static final Logger log = LoggerFactory.getLogger(AbdmService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    public static final Set<String> REQUEST_KINDS =
            Set.of(AbdmRequestQueue.KIND_ABHA, AbdmRequestQueue.KIND_HIP, AbdmRequestQueue.KIND_HIU,
                    AbdmRequestQueue.KIND_HFR, AbdmRequestQueue.KIND_CM);

    private final AbdmRequestQueueRepository repository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    public AbdmService(AbdmRequestQueueRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Enqueue an ABDM gateway request {requestKind, correlationId, payload}.
     * Guards: kind in HIP|HIU|HFR|CM else 400 BEXA003; correlationId required
     * (&lt;= 64); duplicate correlationId -&gt; 409 INTEROP_DUPLICATE_CORRELATION_ID.
     * New rows start at NHCX status 0=ACCEPTED. Emits NOTHING to the outbox.
     */
    /**
     * Enqueue an ABDM gateway request {requestKind, correlationId, payload,
     * envelopeEventId?}. Guards: kind in ABHA|HIP|HIU|HFR|CM else 400 BEXA003;
     * correlationId required (&lt;= 64); duplicate correlationId -&gt; 409
     * INTEROP_DUPLICATE_CORRELATION_ID; duplicate envelopeEventId -&gt; 409 too
     * (mirrors the saga-consumer dedup contract — a Kafka replay of an
     * already-ingested event is a no-op at the durability boundary).
     * New rows start at NHCX status 0=ACCEPTED with the milestone auto-derived
     * from request_kind (ABHA=M1, HIP/CM=M2, HIU=M3, HFR=M0). Emits NOTHING.
     */
    @Transactional
    public AbdmRequestQueue create(String requestKind, String correlationId,
                                   Map<String, Object> payload, String envelopeEventId) {
        if (requestKind == null || !REQUEST_KINDS.contains(requestKind)) {
            throw new InvalidRequestException("BEXA003",
                    "requestKind must be one of ABHA (M1), HIP (M2), HIU (M3), HFR (M0), CM");
        }
        if (correlationId == null || correlationId.isBlank()) {
            throw new InvalidRequestException("BEXA003", "correlationId is required");
        }
        if (correlationId.length() > 64) {
            throw new InvalidRequestException("BEXA003",
                    "correlationId must be at most 64 characters");
        }
        if (envelopeEventId != null && envelopeEventId.length() > 64) {
            throw new InvalidRequestException("BEXA003",
                    "envelopeEventId must be at most 64 characters");
        }
        if (repository.existsByCorrelationId(correlationId)) {
            throw conflict(correlationId);
        }
        if (envelopeEventId != null && repository.findByEnvelopeEventId(envelopeEventId).isPresent()) {
            throw new ConflictException("INTEROP_DUPLICATE_ENVELOPE_EVENT_ID",
                    "ABDM envelopeEventId already exists: " + envelopeEventId);
        }

        AbdmRequestQueue row = new AbdmRequestQueue();
        row.setId(nextId("t_abdm_request_queue_seq"));
        row.setRequestKind(requestKind);
        row.setCorrelationId(correlationId);
        row.setPayload(toJson(payload));
        row.setMilestone(AbdmRequestQueue.milestoneOf(requestKind));
        row.setEnvelopeEventId(envelopeEventId);
        row.setStatus(AbdmRequestQueue.STATUS_ACCEPTED);
        row.setAddedBy(AUDIT_ACTOR_ID);
        row.setAddedDatetime(now());
        try {
            repository.save(row);
        } catch (DataIntegrityViolationException e) {
            // race with a concurrent create of the same correlationId OR envelopeEventId
            // — both uq_t_abdm_correlation and uq_t_abdm_envelope_event can fire here.
            // The conflict envelope is the same on either path (409 with the relevant code).
            if (envelopeEventId != null && repository.findByEnvelopeEventId(envelopeEventId).isPresent()) {
                throw new ConflictException("INTEROP_DUPLICATE_ENVELOPE_EVENT_ID",
                        "ABDM envelopeEventId already exists: " + envelopeEventId);
            }
            throw conflict(correlationId);
        }
        log.info("Enqueued ABDM {} ({}) request {}", requestKind, row.getMilestone(), correlationId);
        return row;
    }

    /**
     * Backward-compatible overload for callers that don't supply an envelopeEventId
     * (kept so the existing controller / unit tests don't need to change).
     */
    public AbdmRequestQueue create(String requestKind, String correlationId, Map<String, Object> payload) {
        return create(requestKind, correlationId, payload, null);
    }

    /**
     * Apply an ABDM gateway callback {payload, status} for correlationId.
     * Guards: unknown correlationId -&gt; 404; status must parse to 0..3 (NHCX range
     * per 06 §A9) else 400 BEXA003. IDEMPOTENT: once ANY callback has been applied,
     * further callbacks are no-ops returning the unchanged stored row (first
     * callback wins). Emits NOTHING to the outbox.
     */
    @Transactional
    public AbdmRequestQueue callback(String correlationId, Map<String, Object> payload, Short status) {
        AbdmRequestQueue row = repository.findByCorrelationId(correlationId)
                .orElseThrow(() -> new InteropNotFoundException("NOT_FOUND",
                        "ABDM request not found for correlationId: " + correlationId));

        short s = parseStatus(status);
        if (row.hasCallback()) {
            log.info("Idempotent repeat callback for {} — keeping first-callback state (status {})",
                    correlationId, row.getStatus());
            return row;
        }

        row.setCallbackPayload(toJson(payload));
        row.setStatus(s);
        row.setUpdatedBy(AUDIT_ACTOR_ID);
        row.setUpdatedDatetime(now());
        repository.save(row);
        log.info("Applied callback for {}: status {}", correlationId, s);
        return row;
    }

    // ------------------------------------------------------------------ queries

    /** Read by primary key — 404 if not present. */
    @Transactional(readOnly = true)
    public AbdmRequestQueue get(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new InteropNotFoundException("NOT_FOUND",
                        "ABDM request not found: " + id));
    }

    /**
     * Lookup by envelopeEventId for the Kafka consumer stub (in-flight continuity).
     * Returns null when not present (the stub's job is to verify the durability
     * boundary, not to fail on miss — that is the producer's job at the API).
     */
    @Transactional(readOnly = true)
    public AbdmRequestQueue findByEnvelopeEventId(String envelopeEventId) {
        if (envelopeEventId == null || envelopeEventId.isBlank()) {
            return null;
        }
        return repository.findByEnvelopeEventId(envelopeEventId).orElse(null);
    }

    // ------------------------------------------------------------------ helpers

    private static ConflictException conflict(String correlationId) {
        return new ConflictException("INTEROP_DUPLICATE_CORRELATION_ID",
                "ABDM correlationId already exists: " + correlationId);
    }

    /** NHCX callback status range per 06 §A9: 0..3. */
    public static short parseStatus(Short raw) {
        if (raw == null || raw < AbdmRequestQueue.STATUS_ACCEPTED || raw > AbdmRequestQueue.STATUS_FAILED) {
            throw new InvalidRequestException("BEXA003",
                    "callback status must be within the NHCX range 0..3 (per 06 §A9)");
        }
        return raw;
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_interop." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }

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

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }
}
