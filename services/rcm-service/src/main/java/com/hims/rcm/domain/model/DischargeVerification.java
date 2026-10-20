package com.hims.rcm.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Table(name = "t_discharge_verification")
public class DischargeVerification extends RcmBase {
    public static final short STATUS_PENDING = 1;
    public static final short STATUS_VERIFIED = 2;
    public static final short STATUS_REJECTED = 3;

    @Id
    @Column(name = "id") private Long id;
    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    @Column(name = "admission_id", nullable = false) private Long admissionId;
    public Long getAdmissionId() { return admissionId; } public void setAdmissionId(Long v) { this.admissionId = v; }
    @Column(name = "patient_id", nullable = false) private Long patientId;
    public Long getPatientId() { return patientId; } public void setPatientId(Long v) { this.patientId = v; }
    @Column(name = "verifier_user_id") private Long verifierUserId;
    public Long getVerifierUserId() { return verifierUserId; } public void setVerifierUserId(Long v) { this.verifierUserId = v; }
    @Column(name = "verified_at") private Timestamp verifiedAt;
    public Timestamp getVerifiedAt() { return verifiedAt; } public void setVerifiedAt(Timestamp v) { this.verifiedAt = v; }
    @Column(name = "bill_settled", nullable = false) private Boolean billSettled = false;
    public Boolean getBillSettled() { return billSettled; } public void setBillSettled(Boolean v) { this.billSettled = v; }
    @Column(name = "clearance_done", nullable = false) private Boolean clearanceDone = false;
    public Boolean getClearanceDone() { return clearanceDone; } public void setClearanceDone(Boolean v) { this.clearanceDone = v; }
    @Column(name = "remarks", length = 500) private String remarks;
    public String getRemarks() { return remarks; } public void setRemarks(String v) { this.remarks = v; }
    @Column(name = "doc_status", nullable = false) private Short docStatus = STATUS_PENDING;
    public Short getDocStatus() { return docStatus; } public void setDocStatus(Short v) { this.docStatus = v; }
}
