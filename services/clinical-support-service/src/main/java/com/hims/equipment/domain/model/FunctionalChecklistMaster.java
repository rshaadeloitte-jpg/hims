package com.hims.equipment.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Functional checklist master (V1 functional_checklist_master) — clean target
 * of legacy M_EquipmentFunctionalCheckList. Dictionary of functional checks;
 * equipment_checklist lines reference it via functional_list_id. Server-owned
 * id from ihms_equipment.functional_checklist_master_seq.
 */
@Entity
@Table(name = "functional_checklist_master")
public class FunctionalChecklistMaster extends EquipmentBase {

    @Id
    private Long id;

    /** Check code (NOT NULL). */
    @Column(name = "code", nullable = false)
    private String code;

    /** Check description (NOT NULL). */
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
