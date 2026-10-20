package com.hims.interop.domain.model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.sql.Timestamp;

@Entity
@Table(name = "t_phr_push")
public class PhrPush extends InteropBase {
    public static final short STATUS_PENDING = 1;
    public static final short STATUS_PUSHED = 2;
    public static final short STATUS_FAILED = 3;
    @Id @Column(name = "id") private Long id;
    @Override public Long getId() { return id; }
    @Override public void setId(Long v) { this.id = v; }
    @Column(name = "abha_link_id", nullable = false) private Long abhaLinkId;
    public Long getAbhaLinkId() { return abhaLinkId; } public void setAbhaLinkId(Long v) { this.abhaLinkId = v; }
    @Column(name = "payload_type", nullable = false, length = 32) private String payloadType;
    public String getPayloadType() { return payloadType; } public void setPayloadType(String v) { this.payloadType = v; }
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb") private String payload;
    public String getPayload() { return payload; } public void setPayload(String v) { this.payload = v; }
    @Column(name = "doc_status", nullable = false) private Short docStatus = STATUS_PENDING;
    public Short getDocStatus() { return docStatus; } public void setDocStatus(Short v) { this.docStatus = v; }
    @Column(name = "pushed_at") private Timestamp pushedAt;
    public Timestamp getPushedAt() { return pushedAt; } public void setPushedAt(Timestamp v) { this.pushedAt = v; }
    @Column(name = "failure_reason", length = 500) private String failureReason;
    public String getFailureReason() { return failureReason; } public void setFailureReason(String v) { this.failureReason = v; }
}
