package com.hims.rcm.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Table(name = "t_chiranjeevi_case")
public class ChiranjeeviCase extends RcmBase {
    public static final short STATUS_NEW = 1;
    public static final short STATUS_APPROVED = 2;
    public static final short STATUS_REJECTED = 3;
    public static final short STATUS_CLOSED = 4;

    @Id
    @Column(name = "id") private Long id;
    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    @Column(name = "case_no", nullable = false, length = 64, unique = true) private String caseNo;
    public String getCaseNo() { return caseNo; } public void setCaseNo(String v) { this.caseNo = v; }
    @Column(name = "patient_id", nullable = false) private Long patientId;
    public Long getPatientId() { return patientId; } public void setPatientId(Long v) { this.patientId = v; }
    @Column(name = "chiranjeevi_id", nullable = false, length = 64) private String chiranjeeviId;
    public String getChiranjeeviId() { return chiranjeeviId; } public void setChiranjeeviId(String v) { this.chiranjeeviId = v; }
    @Column(name = "package_code", nullable = false, length = 64) private String packageCode;
    public String getPackageCode() { return packageCode; } public void setPackageCode(String v) { this.packageCode = v; }
    @Column(name = "amount", nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal v) { this.amount = v; }
    @Column(name = "doc_status", nullable = false) private Short docStatus = STATUS_NEW;
    public Short getDocStatus() { return docStatus; } public void setDocStatus(Short v) { this.docStatus = v; }
    @Column(name = "requested_at", nullable = false) private Timestamp requestedAt;
    public Timestamp getRequestedAt() { return requestedAt; } public void setRequestedAt(Timestamp v) { this.requestedAt = v; }
    @Column(name = "decided_at") private Timestamp decidedAt;
    public Timestamp getDecidedAt() { return decidedAt; } public void setDecidedAt(Timestamp v) { this.decidedAt = v; }
    @Column(name = "closed_at") private Timestamp closedAt;
    public Timestamp getClosedAt() { return closedAt; } public void setClosedAt(Timestamp v) { this.closedAt = v; }
    @Column(name = "remarks", length = 500) private String remarks;
    public String getRemarks() { return remarks; } public void setRemarks(String v) { this.remarks = v; }
}
