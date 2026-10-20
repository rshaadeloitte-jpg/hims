package com.hims.interop.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Dead-letter landing for terminal dispatch failures (MIG-055). Captures
 * the FAILED/DEAD events from BOTH the outbound HIS queue (status=DEAD),
 * the WhatsApp notification queue (terminal EXPIRED), and the SMS/eSanchar
 * queue (terminal DELIVERY_FAILED / EXPIRED). retry/backoff/DLQ policy
 * lives in the OUT-OF-SCOPE dispatcher (comment-only) — this table IS the
 * durable landing so no message is silently lost.
 */
@Entity
@Table(name = "t_dispatch_dlq")
public class DispatchDlq extends InteropBase {

    public static final String SOURCE_OUTBOUND_HIS = "t_outbound_his";
    public static final String SOURCE_WHATSAPP = "t_whatsapp_message_queue";
    public static final String SOURCE_SMS = "t_sms_notification_queue";

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "source_table", nullable = false, length = 40)
    private String sourceTable;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "failed_at", nullable = false)
    private Timestamp failedAt;

    @Column(name = "attempts", nullable = false)
    private Integer attempts = 0;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public String getSourceTable() { return sourceTable; }
    public void setSourceTable(String sourceTable) { this.sourceTable = sourceTable; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public Timestamp getFailedAt() { return failedAt; }
    public void setFailedAt(Timestamp failedAt) { this.failedAt = failedAt; }
    public Integer getAttempts() { return attempts; }
    public void setAttempts(Integer attempts) { this.attempts = attempts; }
}
