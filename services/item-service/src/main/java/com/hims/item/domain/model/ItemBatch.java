package com.hims.item.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Clean target of M_ITEMBATCHMASTER (AGT-02 §5.5 / V1 ihms_item). MIG-012. */
@Entity
@Table(name = "item_batch")
public class ItemBatch extends ItemBase {

    @Id
    @Column(name = "id")
    private Long id;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }

    @Column(name = "item_id", nullable = false) private Long itemId;
    @Column(name = "batch_code", nullable = false) private String batchCode;
    @Column(name = "expiry_date") private java.sql.Date expiryDate;
    @Column(name = "manufacturing_date") private java.sql.Date manufacturingDate;
    @Column(name = "quantity") private java.math.BigDecimal quantity;
    @Column(name = "conversion_factor") private java.math.BigDecimal conversionFactor;
    @Column(name = "purchase_rate") private java.math.BigDecimal purchaseRate;
    @Column(name = "mrp") private java.math.BigDecimal mrp;
    @Column(name = "vat_percentage") private java.math.BigDecimal vatPercentage;
    @Column(name = "vat_amount") private java.math.BigDecimal vatAmount;
    @Column(name = "discount_on_sale") private java.math.BigDecimal discountOnSale;
    @Column(name = "landed_rate") private java.math.BigDecimal landedRate;
    @Column(name = "is_freeze_recall") private Boolean isFreezeRecall = false;
    @Column(name = "freeze_recall_remark") private String freezeRecallRemark;
    @Column(name = "is_expiry_date_updated") private Boolean isExpiryDateUpdated = false;
    @Column(name = "previous_expiry_date") private java.sql.Date previousExpiryDate;
    @Column(name = "remarks") private String remarks;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String batchCode) { this.batchCode = batchCode; }
    public java.math.BigDecimal getQuantity() { return quantity; }
    public void setQuantity(java.math.BigDecimal quantity) { this.quantity = quantity; }
    public java.math.BigDecimal getPurchaseRate() { return purchaseRate; }
    public void setPurchaseRate(java.math.BigDecimal purchaseRate) { this.purchaseRate = purchaseRate; }
    public java.math.BigDecimal getMrp() { return mrp; }
    public void setMrp(java.math.BigDecimal mrp) { this.mrp = mrp; }
    public java.sql.Date getExpiryDate() { return expiryDate; }
    public void setExpiryDate(java.sql.Date expiryDate) { this.expiryDate = expiryDate; }
}
