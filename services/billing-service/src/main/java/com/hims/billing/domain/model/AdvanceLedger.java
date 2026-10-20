package com.hims.billing.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Patient advance balance + FIFO consumption (BR-BILL-007).
 * <p>
 * One row per advance RECEIPT (amount &gt; 0, credits the patient running balance)
 * or per advance CONSUMPTION (amount &lt; 0, debits it against a bill).
 * {@code balance_after} is the running total AFTER the entry — for the patient's
 * most recent entry this IS the current advance balance. FIFO consumption reads
 * all unconsumed RECEIPT rows ordered by advance_id ASC and debits them in order
 * (per BR-BILL-007: "advances consumed against self balance in id order (FIFO)").
 * <p>
 * Cross-service ownership: this ledger is the source of truth for advance balances
 * (migration target of T_ADVANCE + T_ADVANCEDETAILS in the monolith). The payments
 * service in MIG-041 WRITES to this table directly (same schema, same transaction
 * context) and the BR-BILL-008 settled-bill cancellation path produces a paired
 * RECEIPT entry to restore the balance.
 */
@Entity
@Table(name = "t_advance_ledger")
public class AdvanceLedger extends BillingBase {

    public static final String TYPE_RECEIPT = "RECEIPT";
    public static final String TYPE_CONSUMPTION = "CONSUMPTION";

    @Id
    @Column(name = "advance_id")
    private Long advanceId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    /** populated on CONSUMPTION; NULL on RECEIPT. */
    @Column(name = "bill_id")
    private Long billId;

    @Column(name = "entry_type", nullable = false, length = 20)
    private String entryType;

    /** signed: + credit (RECEIPT), - debit (CONSUMPTION). */
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** running balance after this entry. */
    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "receipt_no", length = 64)
    private String receiptNo;

    /** on CONSUMPTION: the source advance that this entry consumed. */
    @Column(name = "consumed_advance_id")
    private Long consumedAdvanceId;

    @Column(name = "mode_code", length = 20)
    private String modeCode;

    @Column(name = "remarks", length = 500)
    private String remarks;

    public Long getAdvanceId() { return advanceId; }
    public void setAdvanceId(Long v) { this.advanceId = v; }
    public Long getId() { return advanceId; }
    public void setId(Long v) { this.advanceId = v; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long v) { this.patientId = v; }
    public Long getBillId() { return billId; }
    public void setBillId(Long v) { this.billId = v; }
    public String getEntryType() { return entryType; }
    public void setEntryType(String v) { this.entryType = v; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal v) { this.amount = v; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal v) { this.balanceAfter = v; }
    public String getReceiptNo() { return receiptNo; }
    public void setReceiptNo(String v) { this.receiptNo = v; }
    public Long getConsumedAdvanceId() { return consumedAdvanceId; }
    public void setConsumedAdvanceId(Long v) { this.consumedAdvanceId = v; }
    public String getModeCode() { return modeCode; }
    public void setModeCode(String v) { this.modeCode = v; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String v) { this.remarks = v; }
}
