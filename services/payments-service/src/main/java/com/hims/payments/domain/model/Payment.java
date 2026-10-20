package com.hims.payments.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.util.List;

/**
 * Clean target of T_PAYMENT. transaction_type per 06 §A6 CHECK IN (2,3,4):
 * 2=payment (this service inserts type 2 here), 3=advance refund, 4=bill refund
 * (refund rows live on t_refund).
 */
@Entity
@Table(name = "t_payment")
public class Payment extends PaymentsBase {

    public static final short TYPE_PAYMENT = 2;

    @Id
    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "bill_id", nullable = false)
    private Long billId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "transaction_type", nullable = false)
    private Short transactionType = TYPE_PAYMENT;

    @Column(name = "change_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal changeAmount = BigDecimal.ZERO;

    /** Mode details of this capture (not a DB relationship — populated on read). */
    @Transient
    private List<PaymentModeDetail> modes;

    /** Advance consumption ledger rows (not a DB relationship — populated on read). */
    @Transient
    private List<AdvanceConsumption> advanceConsumptions;

    @Override
    public Long getId() { return paymentId; }
    @Override
    public void setId(Long id) { this.paymentId = id; }

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Short getTransactionType() { return transactionType; }
    public void setTransactionType(Short transactionType) { this.transactionType = transactionType; }
    public BigDecimal getChangeAmount() { return changeAmount; }
    public void setChangeAmount(BigDecimal changeAmount) { this.changeAmount = changeAmount; }
    public List<PaymentModeDetail> getModes() { return modes; }
    public void setModes(List<PaymentModeDetail> modes) { this.modes = modes; }
    public List<AdvanceConsumption> getAdvanceConsumptions() { return advanceConsumptions; }
    public void setAdvanceConsumptions(List<AdvanceConsumption> advanceConsumptions) {
        this.advanceConsumptions = advanceConsumptions;
    }
}
