package com.hims.item.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.item.domain.model.OutboxEvent;
import com.hims.item.domain.repository.OutboxRepository;
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
 * Transactional outbox publisher (same contract as reference-service): CAS claim,
 * KAFKA-EVENT-CATALOG envelope, PUBLISHED; retries to maxAttempts then DLQ topic.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository repository;
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final String stockTopic;
    private final String dlqTopic;
    private final int maxAttempts;

    public OutboxPublisher(OutboxRepository repository,
                           KafkaTemplate<Object, Object> kafkaTemplate,
                           ObjectMapper objectMapper,
                           @Value("${app.kafka.topic.item-data-changed}") String topic,
                           @Value("${app.kafka.topic.stock-movement}") String stockTopic,
                           @Value("${app.kafka.topic.dlq}") String dlqTopic,
                           @Value("${app.outbox.max-attempts:3}") int maxAttempts) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
        this.stockTopic = stockTopic;
        this.dlqTopic = dlqTopic;
        this.maxAttempts = maxAttempts;
    }

    /**
     * KAFKA-EVENT-CATALOG routing: E-09 InventoryUpdated is a distinct event class
     * (ordering required per (item, store, batch)) and goes to the stock-movement
     * topic; master-data events go to the master-data-changed topic.
     */
    private String topicFor(OutboxEvent event) {
        return "InventoryUpdated".equals(event.getEventType()) ? stockTopic : topic;
    }

    @Transactional
    public void publish(OutboxEvent event) {
        int nextAttempts = event.getAttempts() + 1;
        if (repository.claim(event.getId(), nextAttempts) == 0) {
            return;
        }
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", event.getEventId().toString());
        envelope.put("eventType", event.getEventType());
        envelope.put("version", event.getVersion());
        envelope.put("occurredAt", event.getOccurredAt().toString());
        envelope.put("producerService", event.getProducerService());
        envelope.put("entityId", event.getAggregateId());
        envelope.put("partitionKey", event.getPartitionKey());
        envelope.put("idempotencyKey", event.getIdempotencyKey());
        try {
            envelope.put("payload", objectMapper.readTree(event.getPayload()));
            kafkaTemplate.send(topicFor(event), event.getPartitionKey(), envelope)
                    .get(10, TimeUnit.SECONDS);
            repository.markPublished(event.getId(), Timestamp.from(Instant.now()));
        } catch (Exception e) {
            log.error("Outbox publish failed for event id={} type={}", event.getId(), event.getEventType(), e);
            OutboxEvent.OutboxStatus next =
                    nextAttempts >= maxAttempts ? OutboxEvent.OutboxStatus.DLQ : OutboxEvent.OutboxStatus.FAILED;
            if (next == OutboxEvent.OutboxStatus.DLQ) {
                publishToDlq(event, envelope, e);
            }
            repository.markFailed(event.getId(), nextAttempts, next);
        }
    }

    private void publishToDlq(OutboxEvent event, Map<String, Object> envelope, Exception cause) {
        Map<String, Object> dlqEnvelope = new LinkedHashMap<>(envelope);
        dlqEnvelope.put("dlqReason", cause.getClass().getSimpleName() + ": " + String.valueOf(cause.getMessage()));
        dlqEnvelope.put("dlqAfterAttempts", event.getAttempts() + 1);
        try {
            kafkaTemplate.send(dlqTopic, event.getPartitionKey(), dlqEnvelope).get(10, TimeUnit.SECONDS);
            log.warn("Outbox event id={} type={} routed to DLQ topic {}", event.getId(), event.getEventType(), dlqTopic);
        } catch (Exception dlqError) {
            log.error("DLQ topic publish failed for event id={}", event.getId(), dlqError);
        }
    }
}
