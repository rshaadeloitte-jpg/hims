package com.hims.rmrs.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

@Entity
@Table(name = "t_po_request")
public class PoRequest extends RmrsAuditableBase {
    public static final short STATUS_NEW = 1;
    public static final short STATUS_APPROVED = 2;
    public static final short STATUS_ISSUED = 3;
    public static final short STATUS_REJECTED = 4;

    @Id
    @Column(name = "id") private Long id;
    @Override public Long getId() { return id; }
    @Override public void setId(Long v) { this.id = v; }
    @Column(name = "request_id", nullable = false) private Long requestId;
    public Long getRequestId() { return requestId; } public void setRequestId(Long v) { this.requestId = v; }
    @Column(name = "request_no", nullable = false, length = 64, unique = true) private String requestNo;
    public String getRequestNo() { return requestNo; } public void setRequestNo(String v) { this.requestNo = v; }
    @Column(name = "requesting_dept_id") private Long requestingDeptId;
    public Long getRequestingDeptId() { return requestingDeptId; } public void setRequestingDeptId(Long v) { this.requestingDeptId = v; }
    @Column(name = "head_id", nullable = false) private Long headId;
    public Long getHeadId() { return headId; } public void setHeadId(Long v) { this.headId = v; }
    @Column(name = "ledger_id") private Long ledgerId;
    public Long getLedgerId() { return ledgerId; } public void setLedgerId(Long v) { this.ledgerId = v; }
    @Column(name = "amount", nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal v) { this.amount = v; }
    @Column(name = "remarks", length = 500) private String remarks;
    public String getRemarks() { return remarks; } public void setRemarks(String v) { this.remarks = v; }
    @Column(name = "doc_status", nullable = false) private Short docStatus = STATUS_NEW;
    public Short getDocStatus() { return docStatus; } public void setDocStatus(Short v) { this.docStatus = v; }
    @Column(name = "approved_by") private Long approvedBy;
    public Long getApprovedBy() { return approvedBy; } public void setApprovedBy(Long v) { this.approvedBy = v; }
    @Column(name = "approved_datetime") private Timestamp approvedDatetime;
    public Timestamp getApprovedDatetime() { return approvedDatetime; } public void setApprovedDatetime(Timestamp v) { this.approvedDatetime = v; }
    @Column(name = "issued_po_id") private Long issuedPoId;
    public Long getIssuedPoId() { return issuedPoId; } public void setIssuedPoId(Long v) { this.issuedPoId = v; }
}
