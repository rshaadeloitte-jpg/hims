package com.hims.payments.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Clean target of T_REFUND. transactionType 3 = advance refund (guard BEXAR002),
 * 4 = bill refund. Restore-of-charges compensation stays billing-side later
 * (BR-BILL-008) — documented deferral, E-12 restoredCharges stays [].
 */
@Entity
@Table(name = "t_refund")
public class Refund extends PaymentsBase {

    public static final short TYPE_ADVANCE_REFUND = 3;
    public static final short TYPE_BILL_REFUND = 4;

    @Id
    @Column(name = "refund_id")
    private Long refundId;

    @Column(name = "transaction_type", nullable = false)
    private Short transactionType;

    @Column(name = "bill_id")
    private Long billId;

    @Column(name = "advance_id")
    private Long advanceId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Override
    public Long getId() { return refundId; }
    @Override
    public void setId(Long id) { this.refundId = id; }

    public Long getRefundId() { return refundId; }
    public void setRefundId(Long refundId) { this.refundId = refundId; }
    public Short getTransactionType() { return transactionType; }
    public void setTransactionType(Short transactionType) { this.transactionType = transactionType; }
    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }
    public Long getAdvanceId() { return advanceId; }
    public void setAdvanceId(Long advanceId) { this.advanceId = advanceId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
