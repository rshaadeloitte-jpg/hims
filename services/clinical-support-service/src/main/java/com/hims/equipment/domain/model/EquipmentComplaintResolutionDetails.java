package com.hims.equipment.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Resolution line item (equipment_complaint_resolution_details) — clean target
 * of legacy t_eqpcomplaintresolutionagentdetail. Replaced-part lines with
 * batch/expiry traceability; {@code quantity}/{@code cost}/{@code totalAmount}
 * map NUMERIC(19,2) columns. Server-owned id from
 * ihms_equipment.equipment_complaint_resolution_details_seq (IMP-DEC-003).
 */
@Entity
@Table(name = "equipment_complaint_resolution_details")
public class EquipmentComplaintResolutionDetails extends EquipmentBase {

    @Id
    private Long id;

    @Column(name = "resolution_id", nullable = false)
    private Long resolutionId;

    @Column(name = "engineer_id")
    private Long engineerId;

    /** Replaced-part item id. */
    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "batch_id")
    private Long batchId;

    @Column(name = "batch_code")
    private String batchCode;

    @Column(name = "expiry_date")
    private Timestamp expiryDate;

    /** NUMERIC(19,2) in PostgreSQL. */
    @Column(name = "quantity", precision = 19, scale = 2)
    private BigDecimal quantity;

    /** NUMERIC(19,2) in PostgreSQL. */
    @Column(name = "cost", precision = 19, scale = 2)
    private BigDecimal cost;

    /** NUMERIC(19,2) in PostgreSQL. */
    @Column(name = "total_amount", precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "store_id")
    private Long storeId;

    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }
    public Long getResolutionId() { return resolutionId; }
    public void setResolutionId(Long resolutionId) { this.resolutionId = resolutionId; }
    public Long getEngineerId() { return engineerId; }
    public void setEngineerId(Long engineerId) { this.engineerId = engineerId; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String batchCode) { this.batchCode = batchCode; }
    public Timestamp getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Timestamp expiryDate) { this.expiryDate = expiryDate; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
}
