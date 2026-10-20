package com.hims.item.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** GRN line (legacy T_GRNDETAIL -> clean t_grn_detail). */
@Entity
@Table(name = "t_grn_detail")
public class GrnDetail extends ItemBase {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "grn_id", nullable = false) private Long grnId;
    @Column(name = "item_id", nullable = false) private Long itemId;
    @Column(name = "batch_id") private Long batchId;
    @Column(name = "quantity", nullable = false) private BigDecimal quantity;
    @Column(name = "purchase_rate") private BigDecimal purchaseRate;
    @Column(name = "mrp") private BigDecimal mrp;
    @Column(name = "remarks") private String remarks;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getGrnId() { return grnId; }
    public void setGrnId(Long grnId) { this.grnId = grnId; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getPurchaseRate() { return purchaseRate; }
    public void setPurchaseRate(BigDecimal purchaseRate) { this.purchaseRate = purchaseRate; }
    public BigDecimal getMrp() { return mrp; }
    public void setMrp(BigDecimal mrp) { this.mrp = mrp; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
