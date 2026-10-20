package com.hims.billing.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Debit note (positive adjustment) on a finalized bill (M04 gap).
 * Ported from legacy T_DEBITNOTE. Opposite of credit note: amount ADDS to
 * the bill total. Requires approval (approved_by/at) before being applied.
 */
@Entity
@Table(name = "t_debit_note", schema = "ihms_billing")
public class DebitNote extends BillingBase {

    @Id
    @Column(name = "debit_note_id")
    private Long debitNoteId;

    @Column(name = "bill_id", nullable = false)
    private Long billId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "debit_note_no", nullable = false, unique = true)
    private String debitNoteNo;

    @Column(name = "reason")
    private String reason;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "is_applied", nullable = false)
    private Boolean isApplied = false;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private Timestamp approvedAt;

    public Long getDebitNoteId() { return debitNoteId; }
    public void setDebitNoteId(Long debitNoteId) { this.debitNoteId = debitNoteId; }
    public Long getId() { return debitNoteId; }
    public void setId(Long id) { this.debitNoteId = id; }
    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getDebitNoteNo() { return debitNoteNo; }
    public void setDebitNoteNo(String debitNoteNo) { this.debitNoteNo = debitNoteNo; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Boolean getIsApplied() { return isApplied; }
    public void setIsApplied(Boolean isApplied) { this.isApplied = isApplied; }
    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }
    public Timestamp getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Timestamp approvedAt) { this.approvedAt = approvedAt; }
}
