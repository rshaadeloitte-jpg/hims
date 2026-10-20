package com.hims.govt.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * EJS supervisor reference data (register WP-T4). NO creation API in this slice —
 * full module-125 endpoint parity is OUT OF SCOPE (comment-only); rows exist as
 * reference data and may be referenced by {@link EjsAsha#getSupervisorId()}.
 */
@Entity
@Table(name = "t_ejs_supervisor")
public class EjsSupervisor extends GovtBase {

    @Id
    @Column(name = "supervisor_id")
    private Long supervisorId;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "name", length = 120)
    private String name;

    public Long getSupervisorId() { return supervisorId; }
    public void setSupervisorId(Long supervisorId) { this.supervisorId = supervisorId; }
    @Override public Long getId() { return supervisorId; }
    @Override public void setId(Long id) { this.supervisorId = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
