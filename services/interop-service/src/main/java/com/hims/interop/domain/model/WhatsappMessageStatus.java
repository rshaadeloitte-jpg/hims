package com.hims.interop.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * WhatsApp status-history row: one per accepted transition of a queue message
 * (replaces legacy T_WHATSAPPMESSAGESTATUS).
 */
@Entity
@Table(name = "t_whatsapp_message_status")
public class WhatsappMessageStatus extends InteropBase {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "status_code", nullable = false)
    private Short statusCode;

    @Column(name = "ts", nullable = false)
    private Timestamp ts;

    @Column(name = "detail", length = 200)
    private String detail;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }
    public Short getStatusCode() { return statusCode; }
    public void setStatusCode(Short statusCode) { this.statusCode = statusCode; }
    public Timestamp getTs() { return ts; }
    public void setTs(Timestamp ts) { this.ts = ts; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
