package com.hims.billing.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Ingest-idempotency ledger row for the order-saga consumer side (WP-S1).
 * Mirrors the reporting-service t_ingested_event pattern: event_id (the outbox
 * envelope UUID, VARCHAR + UNIQUE) is the durable dedupe key AND the DB race
 * backstop when two deliveries of the same event run concurrently.
 * status APPLIED -> a real bill DRAFT exists; REJECTED -> payload validation
 * failed and rejection_reason holds the bill-side reason the relay maps to
 * ordering-service BILLING_FAILED compensation.
 */
@Entity
@Table(name = "t_consumed_event")
public class ConsumedEvent {

    public static final String STATUS_APPLIED = "APPLIED";
    public static final String STATUS_REJECTED = "REJECTED";

    @Id
    @Column(name = "consumed_id")
    private Long consumedId;

    @Column(name = "event_id", nullable = false, length = 64, unique = true)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "source_order_id")
    private Long sourceOrderId;

    @Column(name = "status", nullable = false, length = 20)
    private String status = STATUS_APPLIED;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "ingested_at", nullable = false)
    private Timestamp ingestedAt;

    @Column(name = "added_by")
    private Long addedBy;

    @Column(name = "added_datetime")
    private Timestamp addedDatetime;

    public Long getConsumedId() { return consumedId; }
    public void setConsumedId(Long v) { this.consumedId = v; }
    public Long getId() { return consumedId; }
    public void setId(Long id) { this.consumedId = id; }
    public String getEventId() { return eventId; }
    public void setEventId(String v) { this.eventId = v; }
    public String getEventType() { return eventType; }
    public void setEventType(String v) { this.eventType = v; }
    public Long getSourceOrderId() { return sourceOrderId; }
    public void setSourceOrderId(Long v) { this.sourceOrderId = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String v) { this.rejectionReason = v; }
    public Timestamp getIngestedAt() { return ingestedAt; }
    public void setIngestedAt(Timestamp v) { this.ingestedAt = v; }
    public Long getAddedBy() { return addedBy; }
    public void setAddedBy(Long v) { this.addedBy = v; }
    public Timestamp getAddedDatetime() { return addedDatetime; }
    public void setAddedDatetime(Timestamp v) { this.addedDatetime = v; }
}
