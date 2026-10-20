package com.hims.rcm.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Table(name = "t_dm_approval")
public class DmApproval extends RcmBase {
    public static final short STATUS_PENDING = 1;
    public static final short STATUS_APPROVED = 2;
    public static final short STATUS_REJECTED = 3;

    @Id
    @Column(name = "id") private Long id;
    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    @Column(name = "approval_no", nullable = false, length = 64, unique = true) private String approvalNo;
    public String getApprovalNo() { return approvalNo; } public void setApprovalNo(String v) { this.approvalNo = v; }
    @Column(name = "patient_id", nullable = false) private Long patientId;
    public Long getPatientId() { return patientId; } public void setPatientId(Long v) { this.patientId = v; }
    @Column(name = "admission_id") private Long admissionId;
    public Long getAdmissionId() { return admissionId; } public void setAdmissionId(Long v) { this.admissionId = v; }
    @Column(name = "scheme", nullable = false, length = 64) private String scheme;
    public String getScheme() { return scheme; } public void setScheme(String v) { this.scheme = v; }
    @Column(name = "requested_amount", nullable = false, precision = 19, scale = 2) private BigDecimal requestedAmount;
    public BigDecimal getRequestedAmount() { return requestedAmount; } public void setRequestedAmount(BigDecimal v) { this.requestedAmount = v; }
    @Column(name = "approved_amount", precision = 19, scale = 2) private BigDecimal approvedAmount;
    public BigDecimal getApprovedAmount() { return approvedAmount; } public void setApprovedAmount(BigDecimal v) { this.approvedAmount = v; }
    @Column(name = "dm_user_id") private Long dmUserId;
    public Long getDmUserId() { return dmUserId; } public void setDmUserId(Long v) { this.dmUserId = v; }
    @Column(name = "dm_remarks", length = 500) private String dmRemarks;
    public String getDmRemarks() { return dmRemarks; } public void setDmRemarks(String v) { this.dmRemarks = v; }
    @Column(name = "requested_at", nullable = false) private Timestamp requestedAt;
    public Timestamp getRequestedAt() { return requestedAt; } public void setRequestedAt(Timestamp v) { this.requestedAt = v; }
    @Column(name = "decided_at") private Timestamp decidedAt;
    public Timestamp getDecidedAt() { return decidedAt; } public void setDecidedAt(Timestamp v) { this.decidedAt = v; }
    @Column(name = "doc_status", nullable = false) private Short docStatus = STATUS_PENDING;
    public Short getDocStatus() { return docStatus; } public void setDocStatus(Short v) { this.docStatus = v; }
}
