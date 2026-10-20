package com.hims.reporting.domain.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import java.sql.Timestamp;

/**
 * Base for ihms_reporting entities: audit columns per GAP-REGISTER-6 conventions
 * (added_datetime / updated_datetime + acting-user ids). Tables with surrogate
 * keys use server-owned explicit sequences (IMP-DEC-003, assigned manually before
 * persist); rm_* read models use source/composite natural keys.
 */
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@MappedSuperclass
public abstract class ReportingBase {

    @Column(name = "added_by")
    protected Long addedBy;

    @Column(name = "added_datetime")
    protected Timestamp addedDatetime;

    @Column(name = "updated_by")
    protected Long updatedBy;

    @Column(name = "updated_datetime")
    protected Timestamp updatedDatetime;

    /**
     * Surrogate-id access for tables with explicit sequences (t_report_master,
     * t_report_execution, t_ingested_event). The rm_* read models use source /
     * composite natural keys and do NOT override these.
     */
    public Long getId() { return null; }
    public void setId(Long id) { throw new UnsupportedOperationException(
            getClass().getSimpleName() + " uses a natural key"); }
    public Long getAddedBy() { return addedBy; }
    public void setAddedBy(Long addedBy) { this.addedBy = addedBy; }
    public Timestamp getAddedDatetime() { return addedDatetime; }
    public void setAddedDatetime(Timestamp addedDatetime) { this.addedDatetime = addedDatetime; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public Timestamp getUpdatedDatetime() { return updatedDatetime; }
    public void setUpdatedDatetime(Timestamp updatedDatetime) { this.updatedDatetime = updatedDatetime; }
}
