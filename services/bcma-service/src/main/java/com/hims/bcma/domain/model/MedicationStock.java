package com.hims.bcma.domain.model;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.time.LocalDate;

@Entity
@Table(name = "t_medication_stock")
public class MedicationStock extends BcmaBase {

    @Id
    @Column(name = "stock_id")
    private Long stockId;

    @Column(name = "drug_ndc", nullable = false, length = 50)
    private String drugNdc;

    @Column(name = "drug_ien", length = 50)
    private String drugIen;

    @Column(name = "ward_id")
    private Long wardId;

    @Column(name = "batch_id", length = 50)
    private String batchId;

    @Column(name = "quantity_on_hand", precision = 10, scale = 3)
    private java.math.BigDecimal quantityOnHand;

    @Column(name = "available_qty", precision = 10, scale = 3)
    private java.math.BigDecimal availableQty;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "storage_location", length = 100)
    private String storageLocation;

    @Column(name = "last_updated")
    private Timestamp lastUpdated;

    @Override
    public Long getId() { return stockId; }
    @Override
    public void setId(Long id) { this.stockId = id; }

    public Long getStockId() { return stockId; }
    public void setStockId(Long stockId) { this.stockId = stockId; }
    public String getDrugNdc() { return drugNdc; }
    public void setDrugNdc(String drugNdc) { this.drugNdc = drugNdc; }
    public String getDrugIen() { return drugIen; }
    public void setDrugIen(String drugIen) { this.drugIen = drugIen; }
    public Long getWardId() { return wardId; }
    public void setWardId(Long wardId) { this.wardId = wardId; }
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public java.math.BigDecimal getQuantityOnHand() { return quantityOnHand; }
    public void setQuantityOnHand(java.math.BigDecimal quantityOnHand) { this.quantityOnHand = quantityOnHand; }
    public java.math.BigDecimal getAvailableQty() { return availableQty; }
    public void setAvailableQty(java.math.BigDecimal availableQty) { this.availableQty = availableQty; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public String getStorageLocation() { return storageLocation; }
    public void setStorageLocation(String storageLocation) { this.storageLocation = storageLocation; }
    public Timestamp getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Timestamp lastUpdated) { this.lastUpdated = lastUpdated; }
}
