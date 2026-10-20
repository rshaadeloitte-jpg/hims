package com.hims.item.domain.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import java.sql.Timestamp;

/**
 * Base for ihms_item entities: id + the shared MBase-style audit block
 * (snake_case clean columns per AGT-02 design). Jackson field-visibility with
 * NON_NULL (same contract shape as reference-service).
 */
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@MappedSuperclass
public abstract class ItemBase {

    @Column(name = "unit_id")
    protected Long unitId;

    @Column(name = "status")
    protected Boolean status = true;

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

    /**
     * Primary key accessors. The {@code id} field + {@code @Id} live on each
     * concrete entity because the generation strategy differs by table family:
     * migrated masters (item, store, item_batch, ...) use server-owned explicit
     * sequences (IMP-DEC-003, assigned manually before persist), while the NEW
     * stock hotspot tables (item_stock_balance, item_stock_ledger) use
     * GENERATED ALWAYS AS IDENTITY.
     */
    public abstract Long getId();
    public abstract void setId(Long id);
    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }
    public Boolean getStatus() { return status; }
    public void setStatus(Boolean status) { this.status = status; }
    public void setAddedBy(Long addedBy) { this.addedBy = addedBy; }
    public void setAddedDatetime(Timestamp addedDatetime) { this.addedDatetime = addedDatetime; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public void setUpdatedDatetime(Timestamp updatedDatetime) { this.updatedDatetime = updatedDatetime; }
    public void setSynchronizedd(Boolean synchronizedd) { this.synchronizedd = synchronizedd; }
    public Timestamp getAddedDatetime() { return addedDatetime; }
    public Timestamp getUpdatedDatetime() { return updatedDatetime; }
}
