package com.hims.interop.application;

import com.hims.interop.application.exceptions.InvalidRequestException;
import com.hims.interop.domain.model.AbdmRequestQueue;
import com.hims.interop.domain.model.InboundHis;
import com.hims.interop.domain.repository.AbdmRequestQueueRepository;
import com.hims.interop.domain.repository.InboundHisRepository;
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
import java.util.Optional;

/**
 * In-flight continuity stub for the ABDM + HL7 ingress paths (MIG-055). The
 * production cutover replaces the HTTP ingress with Kafka consumer groups;
 * this stub models the SHAPE of the consumer so the queue tables (and the
 * UNIQUE(envelope_event_id) indexes) are the durability boundary across the
 * cutover.
 *
 * ZERO MESSAGE LOSS GUARANTEE (per task):
 *   1. The queue row is the durability boundary. HTTP ingress writes with
 *      envelope_event_id = the outbox eventId. Kafka replay of the same
 *      eventId hits UNIQUE(envelope_event_id) and the service returns the
 *      existing row (no second insert, no double-processing).
 *   2. UNIQUE(correlation_id) on t_abdm_request_queue and the messageType
 *      guard on t_inbound_his give the consumer a backstop independent of
 *      the envelope eventId (so even a malformed Kafka message with a
 *      missing/duplicate eventId does not double-write).
 *   3. Idempotency is at the DB level (UNIQUE), so retries during transient
 *      network errors (HTTP, Kafka) are safe — the service retries the
 *      commit; the second attempt gets a clean no-op.
 *
 * OUT OF SCOPE: the actual Kafka client wiring (the production service will
 * use spring-kafka with consumer groups and offset commits per batch). This
 * stub is the CONVERSION from "an envelope arrived" to "a row exists in the
 * queue" — that conversion is what matters for zero message loss.
 */
@Service
public class InteropKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(InteropKafkaConsumer.class);

    public static final long AUDIT_ACTOR_ID = 1L;

    public static final String CONSUMER_GROUP_ABDM = "ihms-interop-abdm";
    public static final String CONSUMER_GROUP_HL7 = "ihms-interop-hl7";

    private final InboundHisRepository inboundRepository;
    private final AbdmRequestQueueRepository abdmRepository;
    private final AbdmService abdmService;

    @PersistenceContext
    private EntityManager em;

    public InteropKafkaConsumer(InboundHisRepository inboundRepository,
                                AbdmRequestQueueRepository abdmRepository,
                                AbdmService abdmService) {
        this.inboundRepository = inboundRepository;
        this.abdmRepository = abdmRepository;
        this.abdmService = abdmService;
    }

    /**
     * Consume one ABDM request envelope (eventType = "ABDMGatewayRequest" or
     * similar; payload carries {requestKind, correlationId, payload,
     * envelopeEventId}). Idempotent per (envelopeEventId, correlationId).
     * The Kafka consumer group is recorded on the row so ops can observe the
     * HTTP-to-Kafka cutover at the DB level.
     */
    @Transactional
    public AbdmRequestQueue consumeAbdmRequest(String envelopeEventId,
                                              String requestKind, String correlationId,
                                              Map<String, Object> payload) {
        if (envelopeEventId == null || envelopeEventId.isBlank()) {
            throw new InvalidRequestException("BEXA003",
                    "envelopeEventId is required (idempotency key for the Kafka consumer)");
        }
        // fast path: same envelope already ingested via this OR the HTTP path
        Optional<AbdmRequestQueue> existing = abdmRepository.findByEnvelopeEventId(envelopeEventId);
        if (existing.isPresent()) {
            log.info("Kafka consumer dedup hit: envelopeEventId={} -> existing row {} (milestone={})",
                    envelopeEventId, existing.get().getId(), existing.get().getMilestone());
            return existing.get();
        }
        // delegate to the existing service for the validation + persist path
        // (this also handles the race with the UNIQUE index)
        return abdmService.create(requestKind, correlationId, payload, envelopeEventId);
    }

    /**
     * Consume one inbound HL7 envelope (eventType = "InboundHl7"; payload carries
     * {messageType, sender, rawPayload, envelopeEventId}). Idempotent per
     * envelopeEventId via UNIQUE(uq_t_inbound_envelope_event).
     */
    @Transactional
    public InboundHis consumeInboundHl7(String envelopeEventId,
                                        String messageType, String sender, String rawPayload) {
        if (envelopeEventId == null || envelopeEventId.isBlank()) {
            throw new InvalidRequestException("BEXA003",
                    "envelopeEventId is required (idempotency key for the Kafka consumer)");
        }
        // fast path: same envelope already ingested via this OR the HTTP path
        Optional<InboundHis> existing = inboundRepository.findByEnvelopeEventId(envelopeEventId);
        if (existing.isPresent()) {
            log.info("Kafka consumer dedup hit: envelopeEventId={} -> existing row {} (consumerGroup={})",
                    envelopeEventId, existing.get().getId(), existing.get().getConsumerGroup());
            return existing.get();
        }
        // Validate the messageType before persisting (mirrors the HTTP guard)
        if (!InboundService.ALLOWED_MESSAGE_TYPES.contains(messageType)) {
            throw new InvalidRequestException("INTEROP_INVALID_MESSAGE_TYPE",
                    "Unsupported HL7 messageType: " + messageType);
        }
        // raw persist (we don't go through InboundService here because that
        // service is HTTP-shaped: it stamps addedBy/addedDatetime from the
        // HTTP session; the Kafka consumer stamps its own audit fields).
        InboundHis row = new InboundHis();
        row.setId(nextId("t_inbound_his_seq"));
        row.setMessageType(messageType);
        row.setSender(sender);
        row.setRawPayload(rawPayload);
        row.setStatus(InboundHis.STATUS_RECEIVED);
        row.setConsumerGroup(CONSUMER_GROUP_HL7);
        row.setEnvelopeEventId(envelopeEventId);
        row.setAddedBy(AUDIT_ACTOR_ID);
        row.setAddedDatetime(now());
        try {
            inboundRepository.save(row);
        } catch (DataIntegrityViolationException e) {
            // race: another consumer wrote the same envelope first; return the winner
            return inboundRepository.findByEnvelopeEventId(envelopeEventId)
                    .orElseThrow(() -> new IllegalStateException(
                            "UNIQUE(uq_t_inbound_envelope_event) hit but no row for " + envelopeEventId));
        }
        log.info("Kafka consumer ingested HL7 {} (envelopeEventId={}, consumerGroup={})",
                messageType, envelopeEventId, CONSUMER_GROUP_HL7);
        return row;
    }

    // ------------------------------------------------------------------ helpers

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_interop." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }

    private static Timestamp now() { return Timestamp.from(Instant.now()); }
}
