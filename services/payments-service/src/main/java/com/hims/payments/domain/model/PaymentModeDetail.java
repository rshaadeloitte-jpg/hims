package com.hims.payments.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Clean target of T_PAYMENTMODEDETAILS. mode 7 = ADVANCE reserved:
 * BR-BILL-007 partial splits are recorded as extra mode-7 rows.
 */
@Entity
@Table(name = "t_payment_mode_detail")
public class PaymentModeDetail extends PaymentsBase {

    /** Legacy reserved mode id for advance adjustment (BR-BILL-007). */
    public static final short MODE_ADVANCE = 7;

    @Id
    @Column(name = "mode_detail_id")
    private Long modeDetailId;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "mode", nullable = false)
    private Short mode;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Override
    public Long getId() { return modeDetailId; }
    @Override
    public void setId(Long id) { this.modeDetailId = id; }

    public Long getModeDetailId() { return modeDetailId; }
    public void setModeDetailId(Long modeDetailId) { this.modeDetailId = modeDetailId; }
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public Short getMode() { return mode; }
    public void setMode(Short mode) { this.mode = mode; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
