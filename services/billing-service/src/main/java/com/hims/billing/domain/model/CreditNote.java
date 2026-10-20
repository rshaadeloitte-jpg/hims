package com.hims.billing.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Credit note issued against a finalized bill (M04 gap).
 * Ported from old IHMS: GetCreditNoteDetailsByBillId, CreditNoteBillListForSearch.
 * A credit note reduces the amount owed on a bill.
 */
@Entity
@Table(name = "t_credit_note", schema = "ihms_billing")
public class CreditNote extends BillingBase {

    @Id
    @Column(name = "credit_note_id")
    private Long creditNoteId;

    @Column(name = "bill_id", nullable = false)
    private Long billId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "credit_note_no", nullable = false, unique = true)
    private String creditNoteNo;

    @Column(name = "reason")
    private String reason;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "is_applied", nullable = false)
    private Boolean isApplied = false;

    public Long getCreditNoteId() { return creditNoteId; }
    public void setCreditNoteId(Long creditNoteId) { this.creditNoteId = creditNoteId; }
    public Long getId() { return creditNoteId; }
    public void setId(Long id) { this.creditNoteId = id; }
    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getCreditNoteNo() { return creditNoteNo; }
    public void setCreditNoteNo(String creditNoteNo) { this.creditNoteNo = creditNoteNo; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Boolean getIsApplied() { return isApplied; }
    public void setIsApplied(Boolean isApplied) { this.isApplied = isApplied; }
}
