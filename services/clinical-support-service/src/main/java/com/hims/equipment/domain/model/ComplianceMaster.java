package com.hims.equipment.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Compliance requirements master (V1 compliance_master) — clean target of
 * legacy M_COMPLIANCEMASTER. A code+description dictionary with an optional
 * compliance-type grouping reference; server-owned id from
 * ihms_equipment.compliance_master_seq.
 */
@Entity
@Table(name = "compliance_master")
public class ComplianceMaster extends EquipmentBase {

    @Id
    private Long id;

    /** Compliance requirement code (NOT NULL). */
    @Column(name = "code", nullable = false)
    private String code;

    /** Requirement description (NOT NULL). */
    @Column(name = "description", nullable = false)
    private String description;

    /** Grouping reference into the compliance-type dictionary (deferred FK). */
    @Column(name = "compliance_type_id")
    private Long complianceTypeId;

    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getComplianceTypeId() { return complianceTypeId; }
    public void setComplianceTypeId(Long complianceTypeId) { this.complianceTypeId = complianceTypeId; }
}
