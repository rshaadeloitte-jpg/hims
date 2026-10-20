package com.hims.equipment.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Maintenance dictionary (V1 maintenance_type_master) — new clean dictionary
 * replacing the free-text legacy M_EQUIPMENTDETAILSAMC.MAINTAINCETYPE column.
 * Seeded ids ARE the codes carried by {@code EqpPmSchedule.pmType}
 * (1=PREVENTIVE 2=CORRECTIVE 3=CALIBRATION). {@code code} is UNIQUE;
 * server-owned id from ihms_equipment.maintenance_type_master_seq — never
 * hard-delete a seeded row.
 */
@Entity
@Table(name = "maintenance_type_master")
public class MaintenanceTypeMaster extends EquipmentBase {

    @Id
    private Long id;

    /** Type code (NOT NULL UNIQUE) — e.g. PREVENTIVE, CORRECTIVE, CALIBRATION. */
    @Column(name = "code", nullable = false)
    private String code;

    /** Type description (NOT NULL). */
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
