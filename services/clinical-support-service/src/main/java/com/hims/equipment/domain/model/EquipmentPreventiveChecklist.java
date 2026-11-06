package com.hims.equipment.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Preventive-maintenance checklist template (equipment_preventive_checklist,
 * V1 master) — clean target of legacy M_EQUIPMENTPREVENTIVECHECKLIST. A
 * reusable code+description line engineers tick off during a PM visit; the
 * per-execution results are the TEXT checklist block on
 * {@link EqpPreventiveMaintenance}. Server-owned id from
 * ihms_equipment.equipment_preventive_checklist_seq.
 */
@Entity
@Table(name = "equipment_preventive_checklist")
public class EquipmentPreventiveChecklist extends EquipmentBase {

    @Id
    private Long id;

    /** Template reference code (legacy CODE, NOT NULL). */
    @Column(name = "code", nullable = false)
    private String code;

    /** Checklist instruction shown to the engineer (legacy DESCRIPTION, NOT NULL). */
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
