package com.hims.interop.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Outbound HIS queue row (register WP-T1). Fed by the internal event-relay
 * ingress, idempotent per UNIQUE(event_type, aggregate_id). The dispatcher that
 * would drain PENDING rows over HL7 MLLP / print spool is OUT OF SCOPE — the
 * status machine 0=PENDING 1=SENT 2=FAILED 3=DEAD is modeled only.
 */
@Entity
@Table(name = "t_outbound_his")
public class OutboundHis extends InteropBase {

    public static final short STATUS_PENDING = 0;
    public static final short STATUS_SENT = 1;
    public static final short STATUS_FAILED = 2;
    public static final short STATUS_DEAD = 3;

    public static final String TARGET_HL7 = "HL7";
    public static final String TARGET_EHR = "EHR";
    public static final String TARGET_PRINT = "PRINT";

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "aggregate_id", length = 40)
    private String aggregateId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "target_system", nullable = false, length = 30)
    private String targetSystem;

    @Column(name = "status", nullable = false)
    private Short status = STATUS_PENDING;

    @Column(name = "attempts", nullable = false)
    private Integer attempts = 0;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getAggregateId() { return aggregateId; }
    public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getTargetSystem() { return targetSystem; }
    public void setTargetSystem(String targetSystem) { this.targetSystem = targetSystem; }
    public Short getStatus() { return status; }
    public void setStatus(Short status) { this.status = status; }
    public Integer getAttempts() { return attempts; }
    public void setAttempts(Integer attempts) { this.attempts = attempts; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
}
