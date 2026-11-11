package com.hims.ot.domain.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import java.sql.Timestamp;

/**
 * Base for ihms_ot entities: the shared MBase-style audit block
 * (snake_case clean columns, same as sibling services).
 * NOTE: the boolean audit {@code status} flag is deliberately NOT part of this
 * base — on t_otschedule the register pins {@code status} SMALLINT (1..4) as
 * the lifecycle column, so that entity owns its own column set.
 * The {@code id} field + {@code @Id} live on each concrete entity: all tables
 * use server-owned explicit sequences (IMP-DEC-003, assigned manually before persist).
 */
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@MappedSuperclass
public abstract class OtBase {

    @Column(name = "unit_id")
    protected Long unitId;

    @Column(name = "created_unit_id")
    protected Long createdUnitId;

    @Column(name = "updated_unit_id")
    protected Long updatedUnitId;

    @Column(name = "added_by")
    protected Long addedBy;

    @Column(name = "added_datetime")
    protected Timestamp addedDatetime;

    @Column(name = "updated_by")
    protected Long updatedBy;

    @Column(name = "updated_datetime")
    protected Timestamp updatedDatetime;

    @Column(name = "synchronized")
    protected Boolean synchronizedd = false;

    public abstract Long getId();
    public abstract void setId(Long id);
    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }
    public Long getAddedBy() { return addedBy; }
    public void setAddedBy(Long addedBy) { this.addedBy = addedBy; }
    public Timestamp getAddedDatetime() { return addedDatetime; }
    public void setAddedDatetime(Timestamp addedDatetime) { this.addedDatetime = addedDatetime; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public Timestamp getUpdatedDatetime() { return updatedDatetime; }
    public void setUpdatedDatetime(Timestamp updatedDatetime) { this.updatedDatetime = updatedDatetime; }
    public Boolean getSynchronizedd() { return synchronizedd; }
    public void setSynchronizedd(Boolean synchronizedd) { this.synchronizedd = synchronizedd; }
}
