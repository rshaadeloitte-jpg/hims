package com.hims.govt.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Monthly EJS indicator entry (register WP-T4).
 * UNIQUE(asha_id, indicator_code, period_year, period_month) — the API upserts
 * by that key; period_month pinned to 1..12 at DB level.
 */
@Entity
@Table(name = "t_ejs_indicator_entry")
public class EjsIndicatorEntry extends GovtBase {

    @Id
    @Column(name = "entry_id")
    private Long entryId;

    @Column(name = "asha_id", nullable = false)
    private Long ashaId;

    @Column(name = "indicator_code", nullable = false, length = 20)
    private String indicatorCode;

    @Column(name = "period_year")
    private Integer periodYear;

    @Column(name = "period_month")
    private Integer periodMonth;

    @Column(name = "value", nullable = false)
    private Integer value = 0;

    public Long getEntryId() { return entryId; }
    public void setEntryId(Long entryId) { this.entryId = entryId; }
    @Override public Long getId() { return entryId; }
    @Override public void setId(Long id) { this.entryId = id; }
    public Long getAshaId() { return ashaId; }
    public void setAshaId(Long ashaId) { this.ashaId = ashaId; }
    public String getIndicatorCode() { return indicatorCode; }
    public void setIndicatorCode(String indicatorCode) { this.indicatorCode = indicatorCode; }
    public Integer getPeriodYear() { return periodYear; }
    public void setPeriodYear(Integer periodYear) { this.periodYear = periodYear; }
    public Integer getPeriodMonth() { return periodMonth; }
    public void setPeriodMonth(Integer periodMonth) { this.periodMonth = periodMonth; }
    public Integer getValue() { return value; }
    public void setValue(Integer value) { this.value = value; }
}
