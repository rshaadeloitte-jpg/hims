package com.hims.payments.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * BR-BILL-007 advance-adjustment ledger (legacy CIMS_ADDPAYMENTADVANCEDETAILS):
 * one row per (advance, payment) chunk consumed against the self balance.
 */
@Entity
@Table(name = "t_advance_consumption")
public class AdvanceConsumption extends PaymentsBase {

    @Id
    @Column(name = "consumption_id")
    private Long consumptionId;

    @Column(name = "advance_id", nullable = false)
    private Long advanceId;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "consumed_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal consumedAmount;

    @Override
    public Long getId() { return consumptionId; }
    @Override
    public void setId(Long id) { this.consumptionId = id; }

    public Long getConsumptionId() { return consumptionId; }
    public void setConsumptionId(Long consumptionId) { this.consumptionId = consumptionId; }
    public Long getAdvanceId() { return advanceId; }
    public void setAdvanceId(Long advanceId) { this.advanceId = advanceId; }
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public BigDecimal getConsumedAmount() { return consumedAmount; }
    public void setConsumedAmount(BigDecimal consumedAmount) { this.consumedAmount = consumedAmount; }
}
