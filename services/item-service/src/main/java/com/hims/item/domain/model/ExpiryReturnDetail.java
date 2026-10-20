package com.hims.item.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Expiry/damage return line (legacy CIMS_ADDITEMEXPIRYRETURNDETAILS).
 */
@Entity
@Table(name = "t_expiry_return_detail")
public class ExpiryReturnDetail extends ItemBase {

    @Id
    @Column(name = "id")
    private Long id;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }

    @Column(name = "expiry_return_id", nullable = false)
    private Long expiryReturnId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "batch_id")
    private Long batchId;

    @Column(name = "grn_id")
    private Long grnId;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "purchase_rate", precision = 19, scale = 2)
    private BigDecimal purchaseRate;

    @Column(name = "return_reason", length = 255)
    private String returnReason;

    public Long getExpiryReturnId() { return expiryReturnId; }
    public void setExpiryReturnId(Long v) { this.expiryReturnId = v; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long v) { this.itemId = v; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long v) { this.batchId = v; }
    public Long getGrnId() { return grnId; }
    public void setGrnId(Long v) { this.grnId = v; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal v) { this.quantity = v; }
    public BigDecimal getPurchaseRate() { return purchaseRate; }
    public void setPurchaseRate(BigDecimal v) { this.purchaseRate = v; }
    public String getReturnReason() { return returnReason; }
    public void setReturnReason(String v) { this.returnReason = v; }
}
