package com.hims.equipment.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * PM checklist item definition (V1 master equipment_checklist) — clean target
 * of legacy M_EQUIPCHECKLIST. A spare-part / check line attached to an item or
 * a concrete asset; {@code quantity} carries the column DEFAULT 1 when the
 * payload omits it (applied by MasterDataService — no Java initializer, per the
 * note #17 gotcha, so key-absence stays observable). Server-owned id from
 * ihms_equipment.equipment_checklist_seq.
 */
@Entity
@Table(name = "equipment_checklist")
public class EquipmentChecklist extends EquipmentBase {

    @Id
    private Long id;

    /** Item the checklist line belongs to (cross-domain reference, deferred FK). */
    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "serial_no")
    private String serialNo;

    @Column(name = "description")
    private String description;

    @Column(name = "part_no")
    private String partNo;

    @Column(name = "part_sr_no")
    private String partSrNo;

    /**
     * Line quantity — column is NOT NULL DEFAULT 1; the service applies the
     * default on key-absence (deliberately no initializer here).
     */
    @Column(name = "quantity")
    private Long quantity;

    /** Functional checklist master this line belongs to. */
    @Column(name = "functional_list_id")
    private Long functionalListId;

    /** Concrete asset this checklist line is attached to (optional). */
    @Column(name = "equipment_id")
    private Long equipmentId;

    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public String getSerialNo() { return serialNo; }
    public void setSerialNo(String serialNo) { this.serialNo = serialNo; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPartNo() { return partNo; }
    public void setPartNo(String partNo) { this.partNo = partNo; }
    public String getPartSrNo() { return partSrNo; }
    public void setPartSrNo(String partSrNo) { this.partSrNo = partSrNo; }
    public Long getQuantity() { return quantity; }
    public void setQuantity(Long quantity) { this.quantity = quantity; }
    public Long getFunctionalListId() { return functionalListId; }
    public void setFunctionalListId(Long functionalListId) { this.functionalListId = functionalListId; }
    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
}
