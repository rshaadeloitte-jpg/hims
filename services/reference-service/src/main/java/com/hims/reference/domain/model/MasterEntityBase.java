package com.hims.reference.domain.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

import java.sql.Timestamp;

/**
 * Base for master entities. Mirrors legacy MBase/LBase semantics:
 * <ul>
 *   <li>Jackson: field-level visibility, NON_NULL — identical JSON keys to the legacy
 *       FAUG REST contract (field names preserved verbatim, e.g. {@code addeddatetime},
 *       {@code createdunitid}, {@code synchronizedd}).</li>
 *   <li>Audit columns ADDEDBY/ADDEDDATETIME/UPDATEDBY/UPDATEDDATETIME + UnitID/STATUS
 *       (system prompt §69).</li>
 * </ul>
 * JSON field keys are the Java field names — do not rename fields without a legacy parity fixture.
 */
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@MappedSuperclass
public abstract class MasterEntityBase {

    @Id
    @Column(name = "ID")
    protected Long id;

    @Column(name = "UnitID")
    protected Long unitId;

    @Column(name = "STATUS")
    protected Boolean status;

    @Column(name = "CREATEDUNITID")
    protected Long createdunitid;

    @Column(name = "UPDATEDUNITID")
    protected Long updatedunitid;

    @Column(name = "ADDEDBY")
    protected Long addedby;

    @Column(name = "ADDEDDATETIME")
    protected Timestamp addeddatetime;

    @Column(name = "UPDATEDBY")
    protected Long updatedby;

    @Column(name = "UPDATEDDATETIME")
    protected Timestamp updateddatetime;

    @Column(name = "SYNCHRONIZED")
    protected Boolean synchronizedd = false;

    @Transient
    protected Boolean complete = false;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUnitId() {
        return unitId;
    }

    public void setUnitId(Long unitId) {
        this.unitId = unitId;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public Timestamp getAddeddatetime() {
        return addeddatetime;
    }

    public void setAddeddatetime(Timestamp addeddatetime) {
        this.addeddatetime = addeddatetime;
    }

    public void setAddedby(Long addedby) {
        this.addedby = addedby;
    }

    public void setUpdatedby(Long updatedby) {
        this.updatedby = updatedby;
    }

    public void setUpdateddatetime(Timestamp updateddatetime) {
        this.updateddatetime = updateddatetime;
    }

    public Timestamp getUpdateddatetime() {
        return updateddatetime;
    }

    public void markComplete(boolean complete) {
        this.complete = complete;
    }

    public void setSynchronizedd(Boolean synchronizedd) {
        this.synchronizedd = synchronizedd;
    }

    public Boolean getSynchronizedd() {
        return synchronizedd;
    }

    public Long getAddedby() {
        return addedby;
    }

    public void setCreatedunitid(Long createdunitid) {
        this.createdunitid = createdunitid;
    }

    public Long getCreatedunitid() {
        return createdunitid;
    }

    public void setUpdatedunitid(Long updatedunitid) {
        this.updatedunitid = updatedunitid;
    }

    public Long getUpdatedby() {
        return updatedby;
    }
}
