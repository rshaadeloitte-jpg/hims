package com.hims.rmrs.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Treasury head master (register WP-R5 t_head_master). head_code UNIQUE. */
@Entity
@Table(name = "t_head_master")
public class HeadMaster extends RmrsAuditableBase {

    @Id
    @Column(name = "head_id")
    private Long headId;

    @Column(name = "head_code", nullable = false, length = 20)
    private String headCode;

    @Column(name = "head_name", length = 100)
    private String headName;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public Long getHeadId() { return headId; }
    public void setHeadId(Long headId) { this.headId = headId; }
    public Long getId() { return headId; }
    public void setId(Long id) { this.headId = id; }
    public String getHeadCode() { return headCode; }
    public void setHeadCode(String headCode) { this.headCode = headCode; }
    public String getHeadName() { return headName; }
    public void setHeadName(String headName) { this.headName = headName; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
