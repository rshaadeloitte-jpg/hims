package com.hims.billing.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Payment receipt (BR-BILL-006). One row per payment attempt.
 * <p>
 * Legacy convention: {@code transactionType=2} for receipts. receiptNo is the
 * human-facing number from the sequence {@code t_receipt_seq} (drawn via
 * {@code nextval} in the service layer; the actual rendered format follows the
 * unit's receipt-number template, defaulting to "RCPT-{seq}" this tranche).
 * balance_self / balance_non_self are NOT stored here — they are RECOMPUTED on
 * t_bill at apply time which makes settlement idempotent (BR-BILL-006
 * settled &lt;=&gt; both balances &lt;= 0).
 * <p>
 * Cross-service ownership: payments-service WRITES to this table directly
 * (MIG-041 catalog #6). The settle endpoint on t_bill is the
 * synchronous payments-&gt;billing contract that the receipt may invoke
 * (or the receipt may be written after the bill settle call as the audit row).
 */
@Entity
@Table(name = "t_receipt")
public class Receipt extends BillingBase {

    public static final String MODE_CASH = "CASH";
    public static final String MODE_CARD = "CARD";
    public static final String MODE_CHEQUE = "CHEQUE";
    public static final String MODE_ONLINE = "ONLINE";
    public static final String MODE_COMPANY = "COMPANY";
    public static final String MODE_ADVANCE = "ADVANCE";

    public static final short TX_TYPE_RECEIPT = 2;     // legacy BR-BILL-006 convention

    @Id
    @Column(name = "receipt_id")
    private Long receiptId;

    @Column(name = "bill_id", nullable = false)
    private Long billId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "receipt_no", nullable = false, length = 64, unique = true)
    private String receiptNo;

    /** self portion of the payment (legacy T_Payment.con_pay). */
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** non-self portion of the payment (legacy T_Payment.nonself_pay). */
    @Column(name = "non_self_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal nonSelfAmount = BigDecimal.ZERO;

    @Column(name = "mode_code", nullable = false, length = 20)
    private String modeCode;

    @Column(name = "bank_id")
    private Long bankId;

    @Column(name = "bank_ref_no", length = 64)
    private String bankRefNo;

    @Column(name = "transaction_type", nullable = false)
    private Short transactionType = TX_TYPE_RECEIPT;

    @Column(name = "cash_counter_id", nullable = false)
    private Long cashCounterId = 1L;

    @Column(name = "change_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal changeAmount = BigDecimal.ZERO;

    @Column(name = "payment_datetime", nullable = false)
    private Timestamp paymentDatetime;

    public Long getReceiptId() { return receiptId; }
    public void setReceiptId(Long v) { this.receiptId = v; }
    public Long getId() { return receiptId; }
    public void setId(Long v) { this.receiptId = v; }
    public Long getBillId() { return billId; }
    public void setBillId(Long v) { this.billId = v; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long v) { this.patientId = v; }
    public String getReceiptNo() { return receiptNo; }
    public void setReceiptNo(String v) { this.receiptNo = v; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal v) { this.amount = v; }
    public BigDecimal getNonSelfAmount() { return nonSelfAmount; }
    public void setNonSelfAmount(BigDecimal v) { this.nonSelfAmount = v; }
    public String getModeCode() { return modeCode; }
    public void setModeCode(String v) { this.modeCode = v; }
    public Long getBankId() { return bankId; }
    public void setBankId(Long v) { this.bankId = v; }
    public String getBankRefNo() { return bankRefNo; }
    public void setBankRefNo(String v) { this.bankRefNo = v; }
    public Short getTransactionType() { return transactionType; }
    public void setTransactionType(Short v) { this.transactionType = v; }
    public Long getCashCounterId() { return cashCounterId; }
    public void setCashCounterId(Long v) { this.cashCounterId = v; }
    public BigDecimal getChangeAmount() { return changeAmount; }
    public void setChangeAmount(BigDecimal v) { this.changeAmount = v; }
    public Timestamp getPaymentDatetime() { return paymentDatetime; }
    public void setPaymentDatetime(Timestamp v) { this.paymentDatetime = v; }
}
