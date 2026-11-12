package com.hims.ot.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.adt.infrastructure.eventbus.PgEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Transactional outbox publisher (separate bean so @Transactional applies from
 * the scheduled relay). CAS claim -> send catalog envelope -> mark PUBLISHED;
 * on failure attempts++ until maxAttempts then DLQ topic + DLQ row.
 * Envelope shape per KAFKA-EVENT-CATALOG §4 item 3.
 *
 * NOTE — PROPOSED events: ProcedureScheduled / SurgeryCompleted are NOT in the
 * ratified catalog; they are PROPOSED per KAFKA-EVENT-CATALOG §3 (CSSD/Inventory
 * consumption has weak evidence). Emission is implemented here behind the outbox
 * but flagged PROPOSED everywhere; promote or drop when consumers are confirmed.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository repository;
    private final PgEventBus eventBus;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final String dlqTopic;
    private final int maxAttempts;

    public OutboxPublisher(OutboxRepository repository,
                           PgEventBus eventBus,
                           ObjectMapper objectMapper,
                           @Value("${app.eventbus.topic.ot-events}") String topic,
                           @Value("${app.eventbus.topic.ot-dlq:${app.eventbus.topic.dlq}}") String dlqTopic,
                           @Value("${app.outbox.max-attempts:5}") int maxAttempts) {
        this.repository = repository;
        this.eventBus = eventBus;
        this.objectMapper = objectMapper;
        this.topic = topic;
        this.dlqTopic = dlqTopic;
        this.maxAttempts = maxAttempts;
    }

    /**
     * Claims up to {@code batchSize} due rows with FOR UPDATE SKIP LOCKED and
     * dispatches them in this one transaction, so concurrent relay instances never
     * pick the same row and the bus insert commits together with the PUBLISHED mark.
     */
    @Transactional
    public int publishDue(int batchSize) {
        List<OutboxEvent> due = repository.findDue(maxAttempts, Timestamp.from(Instant.now()),
                PageRequest.of(0, batchSize));
        due.forEach(this::publish);
        return due.size();
    }

    /** Exponential retry backoff: 10s, 20s, 40s ... capped at 5 minutes. */
    static Timestamp nextAttemptAt(int attempts) {
        long delaySeconds = Math.min(300L, 5L << Math.min(attempts, 6));
        return Timestamp.from(Instant.now().plusSeconds(delaySeconds));
    }

    @Transactional
    public void publish(OutboxEvent event) {
        int nextAttempts = event.getAttempts() + 1;
        if (repository.claim(event.getId(), nextAttempts) == 0) {
            return;
        }
        Map<String, Object> envelope = new LinkedHashMap<>();
        try {
            envelope.put("eventId", event.getEventId().toString());
            envelope.put("eventType", event.getEventType());
            envelope.put("version", event.getVersion());
            envelope.put("occurredAt", event.getOccurredAt().toString());
            envelope.put("producerService", event.getProducerService());
            envelope.put("entityId", event.getAggregateId());
            envelope.put("partitionKey", event.getPartitionKey());
            envelope.put("idempotencyKey", event.getIdempotencyKey());
            envelope.put("schemaName", schemaOf(event.getEventType()));
            envelope.put("payload", objectMapper.readTree(event.getPayload()));
            eventBus.send(topic, event.getPartitionKey(), envelope);
            repository.markPublished(event.getId(), Timestamp.from(Instant.now()));
        } catch (Exception e) {
            log.error("Outbox publish failed for event id={} type={}", event.getId(), event.getEventType(), e);
            if (nextAttempts >= maxAttempts) {
                publishToDlq(event, envelope, e);
                repository.markDlq(event.getId(), nextAttempts);
            } else {
                repository.markFailed(event.getId(), nextAttempts, nextAttemptAt(nextAttempts));
            }
        }
    }

    /** Catalog schema tokens (PROPOSED): procedure-scheduled.v1 / surgery-completed.v1. */
    static String schemaOf(String eventType) {
        return switch (eventType) {
            case OtEvents.PROCEDURE_SCHEDULED -> "procedure-scheduled.v1";
            case OtEvents.SURGERY_COMPLETED -> "surgery-completed.v1";
            default -> "unknown.v1";
        };
    }

    /** Exhausted events go to the DLQ topic so no event is silently lost. */
    private void publishToDlq(OutboxEvent event, Map<String, Object> envelope, Exception cause) {
        Map<String, Object> dlqEnvelope = new LinkedHashMap<>(envelope);
        dlqEnvelope.put("dlqReason", cause.getClass().getSimpleName() + ": " + String.valueOf(cause.getMessage()));
        dlqEnvelope.put("dlqAfterAttempts", event.getAttempts() + 1);
        try {
            eventBus.send(dlqTopic, event.getPartitionKey(), dlqEnvelope);
            log.warn("Outbox event id={} type={} routed to DLQ topic {}", event.getId(), event.getEventType(), dlqTopic);
        } catch (Exception dlqError) {
            log.error("DLQ topic publish failed for event id={} (row is DLQ-status, replay job will retry)", event.getId(), dlqError);
        }
    }

    /**
     * Event TYPE strings. Both events are PROPOSED per KAFKA-EVENT-CATALOG §3
     * ("ProcedureScheduled/SurgeryCompleted ... proposed — CSSD/Inventory consume;
     * weak evidence → not in catalog yet"). Strings kept verbatim with the
     * register/GAP-REGISTER-3 WP-8 wording.
     */
    public static final class OtEvents {
        public static final String PROCEDURE_SCHEDULED = "ProcedureScheduled";  // PROPOSED (§3)
        public static final String SURGERY_COMPLETED = "SurgeryCompleted";     // PROPOSED (§3)

        private OtEvents() {}
    }
}
