package com.hims.reporting.infrastructure.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Transactional outbox row — SAME envelope DDL as sibling services (IMP-DEC-004).
 *
 * REPORTING NOTE (register WP-T2): the table exists per schema convention, but this
 * service has ZERO event emitters — it is a pure consumer/aggregator (register EVENT
 * POLICY: "emits NOTHING"). Nothing writes here; tests assert it stays EMPTY through
 * every flow (negative outbox assertion). ReportExecuted-style emission was never
 * proposed in KAFKA-EVENT-CATALOG and is not invented here.
 */
@Entity
@Table(name = "outbox")
public class OutboxEvent {

    public enum OutboxStatus {
        PENDING, PUBLISHED, FAILED, DLQ
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "version", nullable = false, length = 16)
    private String version = "v1";

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "partition_key", nullable = false, length = 100)
    private String partitionKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;

    @Column(name = "producer_service", nullable = false, length = 100)
    private String producerService;

    @Column(name = "occurred_at", nullable = false)
    private Timestamp occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "published_at")
    private Timestamp publishedAt;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    public Long getId() { return id; }
    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getAggregateType() { return aggregateType; }
    public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }
    public Long getAggregateId() { return aggregateId; }
    public void setAggregateId(Long aggregateId) { this.aggregateId = aggregateId; }
    public String getPartitionKey() { return partitionKey; }
    public void setPartitionKey(String partitionKey) { this.partitionKey = partitionKey; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getProducerService() { return producerService; }
    public void setProducerService(String producerService) { this.producerService = producerService; }
    public Timestamp getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Timestamp occurredAt) { this.occurredAt = occurredAt; }
    public OutboxStatus getStatus() { return status; }
    public void setStatus(OutboxEvent.OutboxStatus status) { this.status = status; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public Timestamp getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Timestamp publishedAt) { this.publishedAt = publishedAt; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
