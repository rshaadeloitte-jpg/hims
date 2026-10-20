package com.hims.equipment.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Electrical checklist master (V1 electrical_checklist_master) — clean target
 * of legacy M_EquipmentElecticalCheckList (legacy class name typo not carried
 * over). Dictionary of electrical safety checks (code + description);
 * server-owned id from ihms_equipment.electrical_checklist_master_seq.
 */
@Entity
@Table(name = "electrical_checklist_master")
public class ElectricalChecklistMaster extends EquipmentBase {

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
