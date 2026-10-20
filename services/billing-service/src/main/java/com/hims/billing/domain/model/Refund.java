package com.hims.billing.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Refund / cancellation ledger (BR-BILL-008). One row per refund event.
 * <p>
 * Legacy transaction types: {@code transactionType=3} for advance refunds
 * (refunding a previously-received advance back to the patient) and
 * {@code transactionType=4} for bill-cancel refunds (cancel a settled bill;
 * reverses the balance state and unblocks the doctor share / item-sale rows).
 * <p>
 * Cross-service ownership: payments-service WRITES to this table directly
 * (MIG-041 catalog #6). The settled-bill cancel path on the billing service
 * restores the *BeforeFinalization snapshots and unbills doctor shares; the
 * refund row records the audit / receipt reversal.
 */
@Entity
@Table(name = "t_refund")
public class Refund extends BillingBase {

    public static final String MODE_CASH = "CASH";
    public static final String MODE_CARD = "CARD";
    public static final String MODE_ADVANCE = "ADVANCE";

    public static final short TX_TYPE_ADVANCE_REFUND = 3;   // legacy convention
    public static final short TX_TYPE_BILL_CANCEL = 4;       // legacy convention

    @Id
    @Column(name = "refund_id")
    private Long refundId;

    @Column(name = "bill_id", nullable = false)
    private Long billId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    /** source receipt (if this refund is against a previously-recorded receipt). */
    @Column(name = "receipt_id")
    private Long receiptId;

    @Column(name = "transaction_type", nullable = false)
    private Short transactionType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "mode_code", nullable = false, length = 20)
    private String modeCode;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "refund_datetime", nullable = false)
    private Timestamp refundDatetime;

    public Long getRefundId() { return refundId; }
    public void setRefundId(Long v) { this.refundId = v; }
    public Long getId() { return refundId; }
    public void setId(Long v) { this.refundId = v; }
    public Long getBillId() { return billId; }
    public void setBillId(Long v) { this.billId = v; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long v) { this.patientId = v; }
    public Long getReceiptId() { return receiptId; }
    public void setReceiptId(Long v) { this.receiptId = v; }
    public Short getTransactionType() { return transactionType; }
    public void setTransactionType(Short v) { this.transactionType = v; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal v) { this.amount = v; }
    public String getModeCode() { return modeCode; }
    public void setModeCode(String v) { this.modeCode = v; }
    public String getReason() { return reason; }
    public void setReason(String v) { this.reason = v; }
    public Timestamp getRefundDatetime() { return refundDatetime; }
    public void setRefundDatetime(Timestamp v) { this.refundDatetime = v; }
}
