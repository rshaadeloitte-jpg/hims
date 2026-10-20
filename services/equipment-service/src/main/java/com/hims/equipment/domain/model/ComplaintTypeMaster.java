package com.hims.equipment.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Complaint types master (V1 complaint_type_master) — clean target of legacy
 * M_EqupComplaintType. Adds the SLA hints expected_resolution_hours and
 * period; server-owned id from ihms_equipment.complaint_type_master_seq.
 */
@Entity
@Table(name = "complaint_type_master")
public class ComplaintTypeMaster extends EquipmentBase {

    @Id
    private Long id;

    /** Type code (legacy CODE, NOT NULL). */
    @Column(name = "code", nullable = false)
    private String code;

    /** Type description (legacy DESCRIPTION, NOT NULL). */
    @Column(name = "description", nullable = false)
    private String description;

    /** SLA hint: expected resolution window in hours (optional). */
    @Column(name = "expected_resolution_hours")
    private Long expectedResolutionHours;

    /** Free period qualifier from legacy screens (e.g. DAILY, WEEKLY). */
    @Column(name = "period")
    private String period;

    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getExpectedResolutionHours() { return expectedResolutionHours; }
    public void setExpectedResolutionHours(Long expectedResolutionHours) { this.expectedResolutionHours = expectedResolutionHours; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
}
