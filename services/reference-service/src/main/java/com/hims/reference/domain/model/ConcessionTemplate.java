package com.hims.reference.domain.model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Table(name = "concession_template", schema = "ihms_master")
public class ConcessionTemplate {
    public static final short STATUS_ACTIVE = 1;
    public static final short STATUS_INACTIVE = 2;
    @Id @Column(name = "id") private Long id;
    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }

    @Column(name = "unit_id") protected Long unitId;
    @Column(name = "status") protected Boolean status;
    @Column(name = "created_unit_id") protected Long createdunitid;
    @Column(name = "updated_unit_id") protected Long updatedunitid;
    @Column(name = "added_by") protected Long addedby;
    @Column(name = "added_datetime") protected Timestamp addeddatetime;
    @Column(name = "updated_by") protected Long updatedby;
    @Column(name = "updated_datetime") protected Timestamp updateddatetime;
    @Column(name = "synchronized") protected Boolean synchronizedd = false;

    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }
    public Boolean getStatus() { return status; }
    public void setStatus(Boolean status) { this.status = status; }
    public Long getCreatedunitid() { return createdunitid; }
    public void setCreatedunitid(Long createdunitid) { this.createdunitid = createdunitid; }
    public Long getUpdatedunitid() { return updatedunitid; }
    public void setUpdatedunitid(Long updatedunitid) { this.updatedunitid = updatedunitid; }
    public Long getAddedby() { return addedby; }
    public void setAddedby(Long addedby) { this.addedby = addedby; }
    public Timestamp getAddeddatetime() { return addeddatetime; }
    public void setAddeddatetime(Timestamp addeddatetime) { this.addeddatetime = addeddatetime; }
    public Long getUpdatedby() { return updatedby; }
    public void setUpdatedby(Long updatedby) { this.updatedby = updatedby; }
    public Timestamp getUpdateddatetime() { return updateddatetime; }
    public void setUpdateddatetime(Timestamp updateddatetime) { this.updateddatetime = updateddatetime; }
    public Boolean getSynchronizedd() { return synchronizedd; }
    public void setSynchronizedd(Boolean synchronizedd) { this.synchronizedd = synchronizedd; }

    @Column(name = "code", nullable = false, length = 64) private String code;
    public String getCode() { return code; } public void setCode(String v) { this.code = v; }
    @Column(name = "name", nullable = false, length = 200) private String name;
    public String getName() { return name; } public void setName(String v) { this.name = v; }
    @Column(name = "scheme", nullable = false, length = 64) private String scheme;
    public String getScheme() { return scheme; } public void setScheme(String v) { this.scheme = v; }
    @Column(name = "description", length = 500) private String description;
    public String getDescription() { return description; } public void setDescription(String v) { this.description = v; }
    @Column(name = "max_percent_off", nullable = false, precision = 5, scale = 2) private BigDecimal maxPercentOff;
    public BigDecimal getMaxPercentOff() { return maxPercentOff; } public void setMaxPercentOff(BigDecimal v) { this.maxPercentOff = v; }
    @Column(name = "requires_approval", nullable = false) private Boolean requiresApproval = false;
    public Boolean getRequiresApproval() { return requiresApproval; } public void setRequiresApproval(Boolean v) { this.requiresApproval = v; }
    @Column(name = "doc_status", nullable = false) private Short docStatus = STATUS_ACTIVE;
    public Short getDocStatus() { return docStatus; } public void setDocStatus(Short v) { this.docStatus = v; }
}
