package com.hims.pharmacy.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Pharmacy sale return header (M07 gap; legacy PharmacySaleReturnBillDto parity).
 * Status code: 1=INITIATED 2=APPROVED 3=COMPLETED 4=REJECTED.
 */
@Entity
@Table(name = "t_sale_return")
public class SaleReturn extends PharmacyBase {

    public static final short STATUS_INITIATED = 1;
    public static final short STATUS_APPROVED = 2;
    public static final short STATUS_COMPLETED = 3;
    public static final short STATUS_REJECTED = 4;

    @Id
    @Column(name = "return_id")
    private Long returnId;

    @Column(name = "original_sale_id", nullable = false)
    private Long originalSaleId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "return_reason", nullable = false, length = 100)
    private String returnReason;

    @Column(name = "status_code", nullable = false)
    private Short statusCode = STATUS_INITIATED;

    @Column(name = "total_refund_amount", nullable = false)
    private BigDecimal totalRefundAmount = BigDecimal.ZERO;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private Timestamp approvedAt;

    @Column(name = "completed_at")
    private Timestamp completedAt;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Transient
    private List<SaleReturnLine> lines = new ArrayList<>();

    public Long getReturnId() { return returnId; }
    public void setReturnId(Long returnId) { this.returnId = returnId; }
    @Override public Long getId() { return returnId; }
    @Override public void setId(Long id) { this.returnId = id; }
    public Long getOriginalSaleId() { return originalSaleId; }
    public void setOriginalSaleId(Long originalSaleId) { this.originalSaleId = originalSaleId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getReturnReason() { return returnReason; }
    public void setReturnReason(String returnReason) { this.returnReason = returnReason; }
    public Short getStatusCode() { return statusCode; }
    public void setStatusCode(Short statusCode) { this.statusCode = statusCode; }
    public BigDecimal getTotalRefundAmount() { return totalRefundAmount; }
    public void setTotalRefundAmount(BigDecimal totalRefundAmount) { this.totalRefundAmount = totalRefundAmount; }
    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }
    public Timestamp getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Timestamp approvedAt) { this.approvedAt = approvedAt; }
    public Timestamp getCompletedAt() { return completedAt; }
    public void setCompletedAt(Timestamp completedAt) { this.completedAt = completedAt; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public List<SaleReturnLine> getLines() { return lines; }
    public void setLines(List<SaleReturnLine> lines) { this.lines = lines; }
}
