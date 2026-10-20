package com.hims.govt.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * EJS incentive for one ASHA + period (register WP-T4).
 *
 * Status machine (SMALLINT):
 *   1=COMPUTED -&gt; approve -&gt; 2=APPROVED -&gt; pay -&gt; 3=PAID (guarded; anything else
 *   is 400 GOVT_ILLEGAL_TRANSITION). Recompute REPLACES prior COMPUTED rows only —
 *   APPROVED/PAID rows are immutable history, so no UNIQUE key spans the period.
 *   IncentiveComputed event DEFERRED — nothing emitted.
 */
@Entity
@Table(name = "t_ejs_incentive")
public class EjsIncentive extends GovtBase {

    public static final short STATUS_COMPUTED = 1;
    public static final short STATUS_APPROVED = 2;
    public static final short STATUS_PAID = 3;

    @Id
    @Column(name = "incentive_id")
    private Long incentiveId;

    @Column(name = "asha_id", nullable = false)
    private Long ashaId;

    @Column(name = "period_year")
    private Integer periodYear;

    @Column(name = "period_month")
    private Integer periodMonth;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "status", nullable = false)
    private Short status = STATUS_COMPUTED;

    @Column(name = "computed_at")
    private Timestamp computedAt;

    public Long getIncentiveId() { return incentiveId; }
    public void setIncentiveId(Long incentiveId) { this.incentiveId = incentiveId; }
    @Override public Long getId() { return incentiveId; }
    @Override public void setId(Long id) { this.incentiveId = id; }
    public Long getAshaId() { return ashaId; }
    public void setAshaId(Long ashaId) { this.ashaId = ashaId; }
    public Integer getPeriodYear() { return periodYear; }
    public void setPeriodYear(Integer periodYear) { this.periodYear = periodYear; }
    public Integer getPeriodMonth() { return periodMonth; }
    public void setPeriodMonth(Integer periodMonth) { this.periodMonth = periodMonth; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Short getStatus() { return status; }
    public void setStatus(Short status) { this.status = status; }
    public Timestamp getComputedAt() { return computedAt; }
    public void setComputedAt(Timestamp computedAt) { this.computedAt = computedAt; }

    public boolean isComputed() { return status == STATUS_COMPUTED; }
    public boolean isApproved() { return status == STATUS_APPROVED; }
    public boolean isPaid() { return status == STATUS_PAID; }
}
