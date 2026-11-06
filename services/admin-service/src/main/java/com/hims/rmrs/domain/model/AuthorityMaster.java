package com.hims.rmrs.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Collecting authority master (register WP-R5 t_authority_master); head link NULLABLE per register. */
@Entity
@Table(name = "t_authority_master")
public class AuthorityMaster extends RmrsAuditableBase {

    @Id
    @Column(name = "authority_id")
    private Long authorityId;

    @Column(name = "authority_code", nullable = false, length = 20)
    private String authorityCode;

    @Column(name = "authority_name", length = 120)
    private String authorityName;

    /** FK to t_head_master — NULL allowed per register ("head_id FK NULL"). */
    @Column(name = "head_id")
    private Long headId;

    public Long getAuthorityId() { return authorityId; }
    public void setAuthorityId(Long authorityId) { this.authorityId = authorityId; }
    public Long getId() { return authorityId; }
    public void setId(Long id) { this.authorityId = id; }
    public String getAuthorityCode() { return authorityCode; }
    public void setAuthorityCode(String authorityCode) { this.authorityCode = authorityCode; }
    public String getAuthorityName() { return authorityName; }
    public void setAuthorityName(String authorityName) { this.authorityName = authorityName; }
    public Long getHeadId() { return headId; }
    public void setHeadId(Long headId) { this.headId = headId; }
}
