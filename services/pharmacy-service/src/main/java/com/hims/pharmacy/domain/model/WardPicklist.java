package com.hims.pharmacy.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/** Ward pick list row (register WP-R2): ward_id + sale_id + picked_at. */
@Entity
@Table(name = "t_ward_picklist")
public class WardPicklist extends PharmacyBase {

    @Id
    @Column(name = "picklist_id")
    private Long picklistId;

    @Column(name = "ward_id", nullable = false)
    private Long wardId;

    @Column(name = "sale_id", nullable = false)
    private Long saleId;

    @Column(name = "picked_at", nullable = false)
    private Timestamp pickedAt;

    @Override public Long getId() { return picklistId; }
    @Override public void setId(Long id) { this.picklistId = id; }
    public Long getPicklistId() { return picklistId; }
    public void setPicklistId(Long picklistId) { this.picklistId = picklistId; }
    public Long getWardId() { return wardId; }
    public void setWardId(Long wardId) { this.wardId = wardId; }
    public Long getSaleId() { return saleId; }
    public void setSaleId(Long saleId) { this.saleId = saleId; }
    public Timestamp getPickedAt() { return pickedAt; }
    public void setPickedAt(Timestamp pickedAt) { this.pickedAt = pickedAt; }
}
