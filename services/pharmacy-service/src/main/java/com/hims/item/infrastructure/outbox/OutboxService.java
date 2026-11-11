package com.hims.item.infrastructure.outbox;

import com.hims.item.domain.model.OutboxEvent;
import com.hims.item.domain.repository.OutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

/** Appends domain events to the transactional outbox within the business tx (same contract as reference-service). */
@Service
public class OutboxService {

    public static final String PRODUCER_SERVICE = "item-service";

    private final OutboxRepository repository;

    public OutboxService(OutboxRepository repository) {
        this.repository = repository;
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
}
