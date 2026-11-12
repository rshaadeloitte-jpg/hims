package com.hims.item.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.pharmacy.infrastructure.eventbus.PgEventBus;
import com.hims.item.domain.model.OutboxEvent;
import com.hims.item.domain.repository.OutboxRepository;
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
 * Transactional outbox publisher (same contract as reference-service): CAS claim,
 * KAFKA-EVENT-CATALOG envelope, PUBLISHED; retries to maxAttempts then DLQ topic.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository repository;
    private final PgEventBus eventBus;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final String stockTopic;
    private final String dlqTopic;
    private final int maxAttempts;

    public OutboxPublisher(OutboxRepository repository,
                           PgEventBus eventBus,
                           ObjectMapper objectMapper,
                           @Value("${app.eventbus.topic.item-data-changed}") String topic,
                           @Value("${app.eventbus.topic.stock-movement}") String stockTopic,
                           @Value("${app.eventbus.topic.dlq}") String dlqTopic,
                           @Value("${app.outbox.max-attempts:3}") int maxAttempts) {
        this.repository = repository;
        this.eventBus = eventBus;
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
            eventBus.send(topicFor(event), event.getPartitionKey(), envelope);
            repository.markPublished(event.getId(), Timestamp.from(Instant.now()));
        } catch (Exception e) {
            log.error("Outbox publish failed for event id={} type={}", event.getId(), event.getEventType(), e);
            OutboxEvent.OutboxStatus next =
                    nextAttempts >= maxAttempts ? OutboxEvent.OutboxStatus.DLQ : OutboxEvent.OutboxStatus.FAILED;
            if (next == OutboxEvent.OutboxStatus.DLQ) {
                publishToDlq(event, envelope, e);
            }
            repository.markFailed(event.getId(), nextAttempts, next, nextAttemptAt(nextAttempts));
        }
    }

    private void publishToDlq(OutboxEvent event, Map<String, Object> envelope, Exception cause) {
        Map<String, Object> dlqEnvelope = new LinkedHashMap<>(envelope);
        dlqEnvelope.put("dlqReason", cause.getClass().getSimpleName() + ": " + String.valueOf(cause.getMessage()));
        dlqEnvelope.put("dlqAfterAttempts", event.getAttempts() + 1);
        try {
            eventBus.send(dlqTopic, event.getPartitionKey(), dlqEnvelope);
            log.warn("Outbox event id={} type={} routed to DLQ topic {}", event.getId(), event.getEventType(), dlqTopic);
        } catch (Exception dlqError) {
            log.error("DLQ topic publish failed for event id={}", event.getId(), dlqError);
        }
    }
}
