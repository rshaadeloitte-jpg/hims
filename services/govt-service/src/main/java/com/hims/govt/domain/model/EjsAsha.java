package com.hims.govt.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** EJS ASHA worker under an optional supervisor (register WP-T4); asha_code UNIQUE. */
@Entity
@Table(name = "t_ejs_asha")
public class EjsAsha extends GovtBase {

    @Id
    @Column(name = "asha_id")
    private Long ashaId;

    @Column(name = "asha_code", nullable = false, length = 20)
    private String ashaCode;

    @Column(name = "name", length = 120)
    private String name;

    @Column(name = "supervisor_id")
    private Long supervisorId;

    public Long getAshaId() { return ashaId; }
    public void setAshaId(Long ashaId) { this.ashaId = ashaId; }
    @Override public Long getId() { return ashaId; }
    @Override public void setId(Long id) { this.ashaId = id; }
    public String getAshaCode() { return ashaCode; }
    public void setAshaCode(String ashaCode) { this.ashaCode = ashaCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getSupervisorId() { return supervisorId; }
    public void setSupervisorId(Long supervisorId) { this.supervisorId = supervisorId; }
}
