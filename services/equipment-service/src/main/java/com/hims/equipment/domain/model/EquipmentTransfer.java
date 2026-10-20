package com.hims.equipment.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Department-transfer log row for an asset (equipment_transfer). One row per
 * transfer; the equipment row's department_id is moved forward in the same
 * transaction. {@code reason} maps the TEXT column. Server-owned id from
 * ihms_equipment.equipment_transfer_seq.
 */
@Entity
@Table(name = "equipment_transfer")
public class EquipmentTransfer extends EquipmentBase {

    @Id
    private Long id;

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    @Column(name = "from_department_id")
    private Long fromDepartmentId;

    @Column(name = "to_department_id")
    private Long toDepartmentId;

    @Column(name = "transfer_date")
    private Timestamp transferDate;

    /** TEXT column. */
    @Column(name = "reason")
    private String reason;

    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }
    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
    public Long getFromDepartmentId() { return fromDepartmentId; }
    public void setFromDepartmentId(Long fromDepartmentId) { this.fromDepartmentId = fromDepartmentId; }
    public Long getToDepartmentId() { return toDepartmentId; }
    public void setToDepartmentId(Long toDepartmentId) { this.toDepartmentId = toDepartmentId; }
    public Timestamp getTransferDate() { return transferDate; }
    public void setTransferDate(Timestamp transferDate) { this.transferDate = transferDate; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
