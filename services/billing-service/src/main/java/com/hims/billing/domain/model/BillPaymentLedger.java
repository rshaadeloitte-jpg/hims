package com.hims.billing.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Running payment-state ledger for bills (M04 partial-payment).
 * One row per (re)computation event. Authoritative payment state lives in
 * payments-service; this is the billing-UI mirror for partial-payment
 * visualization (running balance without sync-roundtrip).
 */
@Entity
@Table(name = "t_bill_payment_ledger", schema = "ihms_billing")
public class BillPaymentLedger extends BillingBase {

    public static final String EVENT_PAYMENT = "PAYMENT";
    public static final String EVENT_REFUND = "REFUND";
    public static final String EVENT_ADVANCE_USE = "ADVANCE_USE";
    public static final String EVENT_CANCEL = "CANCEL";

    @Id
    @Column(name = "ledger_id")
    private Long ledgerId;

    @Column(name = "bill_id", nullable = false)
    private Long billId;

    @Column(name = "event_type", nullable = false, length = 32)
    private String eventType;

    @Column(name = "paid_self", nullable = false, precision = 19, scale = 2)
    private BigDecimal paidSelf = BigDecimal.ZERO;

    @Column(name = "paid_non_self", nullable = false, precision = 19, scale = 2)
    private BigDecimal paidNonSelf = BigDecimal.ZERO;

    @Column(name = "balance_self", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceSelf = BigDecimal.ZERO;

    @Column(name = "balance_non_self", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceNonSelf = BigDecimal.ZERO;

    @Column(name = "is_settled", nullable = false)
    private Boolean isSettled = false;

    @Column(name = "source_ref", length = 64)
    private String sourceRef;

    @Column(name = "source_service", length = 64)
    private String sourceService;

    @Column(name = "notes", length = 500)
    private String notes;

    public Long getLedgerId() { return ledgerId; }
    public void setLedgerId(Long ledgerId) { this.ledgerId = ledgerId; }
    public Long getId() { return ledgerId; }
    public void setId(Long id) { this.ledgerId = id; }
    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public BigDecimal getPaidSelf() { return paidSelf; }
    public void setPaidSelf(BigDecimal paidSelf) { this.paidSelf = paidSelf; }
    public BigDecimal getPaidNonSelf() { return paidNonSelf; }
    public void setPaidNonSelf(BigDecimal paidNonSelf) { this.paidNonSelf = paidNonSelf; }
    public BigDecimal getBalanceSelf() { return balanceSelf; }
    public void setBalanceSelf(BigDecimal balanceSelf) { this.balanceSelf = balanceSelf; }
    public BigDecimal getBalanceNonSelf() { return balanceNonSelf; }
    public void setBalanceNonSelf(BigDecimal balanceNonSelf) { this.balanceNonSelf = balanceNonSelf; }
    public Boolean getIsSettled() { return isSettled; }
    public void setIsSettled(Boolean isSettled) { this.isSettled = isSettled; }
    public String getSourceRef() { return sourceRef; }
    public void setSourceRef(String sourceRef) { this.sourceRef = sourceRef; }
    public String getSourceService() { return sourceService; }
    public void setSourceService(String sourceService) { this.sourceService = sourceService; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
