package com.hims.interop.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Inbound HL7 v2 queue row (register WP-T1). Ingress validates messageType
 * against the allowed ADT/ORM set; rows land 0=RECEIVED with processed_at NULL.
 * Parsing/routing to PROCESSED/REJECTED belongs to the OUT-OF-SCOPE inbound
 * dispatcher (comment-only).
 */
@Entity
@Table(name = "t_inbound_his")
public class InboundHis extends InteropBase {

    public static final short STATUS_RECEIVED = 0;
    public static final short STATUS_PROCESSED = 1;
    public static final short STATUS_REJECTED = 2;

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "message_type", nullable = false, length = 20)
    private String messageType;

    @Column(name = "sender", length = 40)
    private String sender;

    @Column(name = "raw_payload", nullable = false, columnDefinition = "text")
    private String rawPayload;

    @Column(name = "status", nullable = false)
    private Short status = STATUS_RECEIVED;

    @Column(name = "processed_at")
    private Timestamp processedAt;

    /**
     * Kafka consumer group that ingested this message (NULL = HTTP ingress path).
     * Lets ops observe the cutover from HTTP ingress to Kafka consumer at DB
     * level (in-flight continuity, zero message loss across the cutover).
     */
    @Column(name = "consumer_group", length = 60)
    private String consumerGroup;

    /**
     * Outbox envelope eventId (UUID-as-string) for cross-service dedup.
     * UNIQUE in the schema (uq_t_inbound_envelope_event); a Kafka replay of
     * the same envelope is a no-op at the durability boundary.
     */
    @Column(name = "envelope_event_id", length = 64)
    private String envelopeEventId;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public String getRawPayload() { return rawPayload; }
    public void setRawPayload(String rawPayload) { this.rawPayload = rawPayload; }
    public Short getStatus() { return status; }
    public void setStatus(Short status) { this.status = status; }
    public Timestamp getProcessedAt() { return processedAt; }
    public void setProcessedAt(Timestamp processedAt) { this.processedAt = processedAt; }
    public String getConsumerGroup() { return consumerGroup; }
    public void setConsumerGroup(String consumerGroup) { this.consumerGroup = consumerGroup; }
    public String getEnvelopeEventId() { return envelopeEventId; }
    public void setEnvelopeEventId(String envelopeEventId) { this.envelopeEventId = envelopeEventId; }
}
