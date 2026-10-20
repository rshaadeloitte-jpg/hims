package com.hims.govt.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Rate table for incentive compute (register WP-T4): amount = SUM(entry.value x rate).
 * Seeded with 3 illustrative rows in V1__init_ihms_govt.sql (seed codes/rates not
 * named by the register — disclosed in WP-T4-govt.md). Entries whose indicator_code
 * has no rate row contribute 0 to the sum (documented behavior).
 */
@Entity
@Table(name = "t_incentive_rate")
public class IncentiveRate {

    @Id
    @Column(name = "indicator_code", length = 20)
    private String indicatorCode;

    @Column(name = "rate", nullable = false, precision = 19, scale = 2)
    private BigDecimal rate;

    public String getIndicatorCode() { return indicatorCode; }
    public void setIndicatorCode(String indicatorCode) { this.indicatorCode = indicatorCode; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
}
