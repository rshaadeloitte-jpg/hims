package com.hims.billing.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Appends domain events to the outbox within the business transaction (IMP-DEC-004). */
@Service
public class OutboxService {

    public static final String PRODUCER_SERVICE = "billing-service";

    private final OutboxRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OutboxEvent append(String eventType,
                              String aggregateType,
                              Long aggregateId,
                              String partitionKey,
                              String payloadJson,
                              String idempotencyKey) {
        OutboxEvent event = new OutboxEvent();
        event.setEventId(UUID.randomUUID());
        event.setEventType(eventType);
        event.setVersion("v1");
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setPartitionKey(partitionKey);
        event.setPayload(payloadJson);
        event.setIdempotencyKey(idempotencyKey);
        event.setProducerService(PRODUCER_SERVICE);
        event.setOccurredAt(Timestamp.from(Instant.now()));
        event.setCreatedAt(Timestamp.from(Instant.now()));
        return repository.save(event);
    }

    /**
     * Convenience overload that serializes a Map payload to JSON.
     * Used by gap services (debit notes, etc.) that build event payloads as Maps.
     */
    @Transactional
    public OutboxEvent appendOutbox(String eventType, String version, String aggregateType, Long aggregateId,
                                     String partitionKey, Map<String, Object> payload,
                                     String idempotencyKeyPrefix, String idempotencyVersion) {
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox payload", e);
        }
        return append(eventType, aggregateType, aggregateId, partitionKey, payloadJson,
                idempotencyKeyPrefix + ":" + idempotencyVersion);
    }
}
