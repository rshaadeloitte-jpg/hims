package com.hims.payments.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Clean target of T_ADVANCE. {@code balance} is a DB GENERATED ALWAYS AS
 * (amount - used) STORED column — read-only for the application: refunds
 * decrement {@code amount}, BR-BILL-007 consumption increments {@code used}.
 */
@Entity
@Table(name = "t_advance")
public class Advance extends PaymentsBase {

    @Id
    @Column(name = "advance_id")
    private Long advanceId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "used", nullable = false, precision = 19, scale = 2)
    private BigDecimal used = BigDecimal.ZERO;

    /** Generated column — never written by the application. */
    @Column(name = "balance", insertable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Override
    public Long getId() { return advanceId; }
    @Override
    public void setId(Long id) { this.advanceId = id; }

    public Long getAdvanceId() { return advanceId; }
    public void setAdvanceId(Long advanceId) { this.advanceId = advanceId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getUsed() { return used; }
    public void setUsed(BigDecimal used) { this.used = used; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    /** Remaining refundable/consumable value; falls back to amount-used pre-flush in tests. */
    public BigDecimal remaining() {
        return balance != null ? balance : amount.subtract(used == null ? BigDecimal.ZERO : used);
    }
}
