package com.hims.equipment.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Complaint categories master (V1 complaint_category_master) — clean target
 * of legacy M_COMPLAINTCATEGORYMASTER. Dictionary row (code + description);
 * server-owned id from ihms_equipment.complaint_category_master_seq.
 */
@Entity
@Table(name = "complaint_category_master")
public class ComplaintCategoryMaster extends EquipmentBase {

    @Id
    private Long id;

    /** Category code (legacy CODE, NOT NULL). */
    @Column(name = "code", nullable = false)
    private String code;

    /** Category description (legacy DESCRIPTION, NOT NULL). */
    @Column(name = "description", nullable = false)
    private String description;

    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
