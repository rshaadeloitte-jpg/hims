package com.hims.rcm.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Transactional outbox publisher (separate bean so {@code @Transactional} applies from
 * the scheduled relay). CAS claim -> send catalog envelope -> mark PUBLISHED;
 * on failure attempts++ until maxAttempts then DLQ topic + DLQ row.
 * Envelope shape per KAFKA-EVENT-CATALOG §4 item 3.
 * DORMANT this tranche: the relay always finds an empty outbox because ClaimSubmitted
 * is DEFERRED (KAFKA-EVENT-CATALOG §3, register WP-R4) — kept for post-saga parity.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository repository;
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final String dlqTopic;
    private final int maxAttempts;

    public OutboxPublisher(OutboxRepository repository,
                           KafkaTemplate<Object, Object> kafkaTemplate,
                           ObjectMapper objectMapper,
                           @Value("${app.kafka.topic.rcm-events}") String topic,
                           @Value("${app.kafka.topic.dlq}") String dlqTopic,
                           @Value("${app.outbox.max-attempts:5}") int maxAttempts) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
        this.dlqTopic = dlqTopic;
        this.maxAttempts = maxAttempts;
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
            kafkaTemplate.send(topic, event.getPartitionKey(), envelope)
                    .get(10, TimeUnit.SECONDS);
            repository.markPublished(event.getId(), Timestamp.from(Instant.now()));
        } catch (Exception e) {
            log.error("Outbox publish failed for event id={} type={}", event.getId(), event.getEventType(), e);
            if (nextAttempts >= maxAttempts) {
                publishToDlq(event, envelope, e);
                repository.markDlq(event.getId(), nextAttempts);
            } else {
                repository.markFailed(event.getId(), nextAttempts);
            }
        }
    }

    /**
     * Catalog schema name per event type. ClaimSubmitted (the only future RCM event)
     * is DEFERRED — no mapping exists yet; anything reaching this switch is unknown.v1.
     */
    static String schemaOf(String eventType) {
        return "unknown.v1";
    }

    /** Exhausted events go to the DLQ topic so no event is silently lost. */
    private void publishToDlq(OutboxEvent event, Map<String, Object> envelope, Exception cause) {
        Map<String, Object> dlqEnvelope = new LinkedHashMap<>(envelope);
        dlqEnvelope.put("dlqReason", cause.getClass().getSimpleName() + ": " + String.valueOf(cause.getMessage()));
        dlqEnvelope.put("dlqAfterAttempts", event.getAttempts() + 1);
        try {
            kafkaTemplate.send(dlqTopic, event.getPartitionKey(), dlqEnvelope).get(10, TimeUnit.SECONDS);
            log.warn("Outbox event id={} type={} routed to DLQ topic {}", event.getId(), event.getEventType(), dlqTopic);
        } catch (Exception dlqError) {
            log.error("DLQ topic publish failed for event id={} (row is DLQ-status, replay job will retry)", event.getId(), dlqError);
        }
    }
}
