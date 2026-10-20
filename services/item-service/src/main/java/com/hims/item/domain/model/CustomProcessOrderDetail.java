package com.hims.item.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Custom process order line. received_qty accumulates on each receive call. */
@Entity
@Table(name = "t_custom_process_order_detail")
public class CustomProcessOrderDetail extends ItemBase {
    @Id
    @Column(name = "id") private Long id;
    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }

    @Column(name = "cpo_id", nullable = false) private Long cpoId;
    @Column(name = "item_id", nullable = false) private Long itemId;
    @Column(name = "batch_id") private Long batchId;
    @Column(name = "ordered_qty", nullable = false, precision = 19, scale = 4) private BigDecimal orderedQty;
    @Column(name = "received_qty", nullable = false, precision = 19, scale = 4) private BigDecimal receivedQty = BigDecimal.ZERO;
    @Column(name = "unit_price", precision = 19, scale = 2) private BigDecimal unitPrice;
    @Column(name = "remarks", length = 500) private String remarks;

    public Long getCpoId() { return cpoId; }
    public void setCpoId(Long v) { this.cpoId = v; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long v) { this.itemId = v; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long v) { this.batchId = v; }
    public BigDecimal getOrderedQty() { return orderedQty; }
    public void setOrderedQty(BigDecimal v) { this.orderedQty = v; }
    public BigDecimal getReceivedQty() { return receivedQty; }
    public void setReceivedQty(BigDecimal v) { this.receivedQty = v; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal v) { this.unitPrice = v; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String v) { this.remarks = v; }
}
