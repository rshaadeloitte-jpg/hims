package com.hims.reporting.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Read model: revenue accumulation per (stat_date, head_code), fed by
 * InvoiceGenerated AND PaymentReceived via the internal event ingress
 * (headCode defaults to OPD when absent). Amounts ACCUMULATE (+=) per key;
 * duplicate ingestion adds nothing (t_ingested_event ledger — pinned).
 */
@Entity
@Table(name = "rm_revenue_daily")
@IdClass(RmRevenueDaily.Key.class)
public class RmRevenueDaily extends ReportingBase {

    @Id
    @Column(name = "stat_date")
    private LocalDate statDate;

    @Id
    @Column(name = "head_code", length = 20)
    private String headCode;

    @Column(name = "total", nullable = false, precision = 19, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    public LocalDate getStatDate() { return statDate; }
    public void setStatDate(LocalDate statDate) { this.statDate = statDate; }
    public String getHeadCode() { return headCode; }
    public void setHeadCode(String headCode) { this.headCode = headCode; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    /** Composite natural key (stat_date, head_code). */
    public static class Key implements Serializable {
        private LocalDate statDate;
        private String headCode;

        public Key() { }
        public Key(LocalDate statDate, String headCode) {
            this.statDate = statDate;
            this.headCode = headCode;
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return Objects.equals(statDate, key.statDate)
                    && Objects.equals(headCode, key.headCode);
        }
        @Override public int hashCode() { return Objects.hash(statDate, headCode); }
    }
}
