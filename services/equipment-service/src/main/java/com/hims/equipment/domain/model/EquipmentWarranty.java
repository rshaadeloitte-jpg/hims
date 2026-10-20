package com.hims.equipment.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Equipment warranty / AMC / insurance cover record (equipment_warranty, V1) —
 * one row per cover period on an asset.
 *
 * <p>{@code warrantyType} is a free dictionary value (legacy shapes used
 * WARRANTY/AMC/CMC style codes); the column is NOT NULL DEFAULT 'WARRANTY' and
 * the service applies that default when the payload omits it — deliberately NO
 * Java-side initializer here so null-based defaulting can detect absence
 * (see PmService.complete for the initializer gotcha). {@code terms} maps the
 * TEXT column (contract conditions, coverage notes). Server-owned id from
 * ihms_equipment.equipment_warranty_seq (IMP-DEC-003).
 */
@Entity
@Table(name = "equipment_warranty")
public class EquipmentWarranty extends EquipmentBase {

    @Id
    private Long id;

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    /** WARRANTY | AMC | CMC ... — NOT NULL, service defaults to 'WARRANTY'. */
    @Column(name = "warranty_type", nullable = false)
    private String warrantyType;

    @Column(name = "start_date")
    private Timestamp startDate;

    @Column(name = "end_date")
    private Timestamp endDate;

    @Column(name = "vendor")
    private String vendor;

    /** TEXT column — contract terms / coverage notes (free text). */
    @Column(name = "terms")
    private String terms;

    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }
    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
    public String getWarrantyType() { return warrantyType; }
    public void setWarrantyType(String warrantyType) { this.warrantyType = warrantyType; }
    public Timestamp getStartDate() { return startDate; }
    public void setStartDate(Timestamp startDate) { this.startDate = startDate; }
    public Timestamp getEndDate() { return endDate; }
    public void setEndDate(Timestamp endDate) { this.endDate = endDate; }
    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }
    public String getTerms() { return terms; }
    public void setTerms(String terms) { this.terms = terms; }
}
