package com.hims.equipment.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Equipment operational-status dictionary (V1
 * equipment_working_status_master) — clean target of legacy
 * M_eqpworkingstatus. The seeded ids ARE the codes used by the
 * {@link EquipmentWorkingStatus} constants (1=WORKING 2=NOT_WORKING
 * 3=UNDER_REPAIR 4=CONDEMNED) stored on {@code equipment.working_status}.
 * {@code code} is UNIQUE; server-owned id from
 * ihms_equipment.equipment_working_status_master_seq — never hard-delete a
 * seeded row.
 */
@Entity
@Table(name = "equipment_working_status_master")
public class EquipmentWorkingStatusMaster extends EquipmentBase {

    @Id
    private Long id;

    /** Status code (NOT NULL UNIQUE) — e.g. WORKING, CONDEMNED. */
    @Column(name = "code", nullable = false)
    private String code;

    /** Status description (NOT NULL). */
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
