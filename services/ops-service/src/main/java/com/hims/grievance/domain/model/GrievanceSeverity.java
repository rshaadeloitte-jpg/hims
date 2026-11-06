package com.hims.grievance.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Clean target of M_GRIEVANCESEVERITYMASTER. */
@Entity
@Table(name = "grievance_severity")
public class GrievanceSeverity extends GrievanceBase {

    @Id
    private Long id;

    @Column(name = "code")
    private String code;

    @Column(name = "description", nullable = false)
    private String description;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
