package com.hims.bcma.domain.model;

import jakarta.persistence.*;
import java.sql.Timestamp;

@MappedSuperclass
public abstract class BcmaBase {
    @Column(name = "unit_id") protected Long unitId;
    @Column(name = "status") protected Boolean status = true;
    @Column(name = "created_unit_id") protected Long createdUnitId;
    @Column(name = "updated_unit_id") protected Long updatedUnitId;
    @Column(name = "added_by") protected Long addedBy;
    @Column(name = "added_datetime") protected Timestamp addedDatetime;
    @Column(name = "updated_by") protected Long updatedBy;
    @Column(name = "updated_datetime") protected Timestamp updatedDatetime;
    @Column(name = "synchronized") protected Boolean synchronizedd = false;

    public abstract Long getId();
    public abstract void setId(Long id);

    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }
    public Boolean getStatus() { return status; }
    public void setStatus(Boolean status) { this.status = status; }
    public Long getCreatedUnitId() { return createdUnitId; }
    public void setCreatedUnitId(Long createdUnitId) { this.createdUnitId = createdUnitId; }
    public Long getUpdatedUnitId() { return updatedUnitId; }
    public void setUpdatedUnitId(Long updatedUnitId) { this.updatedUnitId = updatedUnitId; }
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
