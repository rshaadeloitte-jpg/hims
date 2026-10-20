package com.hims.item.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * NEW clean table (no 1:1 legacy table): current stock state per (store,item,batch).
 * Single-writer hotspot (D-1) — ONLY the item service stock API mutates it
 * (SELECT ... FOR UPDATE + ledger append in the same tx).
 */
@Entity
@Table(name = "item_stock_balance")
public class ItemStockBalance extends ItemBase {

    /** NEW clean table: GENERATED ALWAYS AS IDENTITY (no legacy sequence). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }

    @Column(name = "store_id", nullable = false) private Long storeId;
    @Column(name = "item_id", nullable = false) private Long itemId;
    @Column(name = "batch_id") private Long batchId;
    @Column(name = "department_id") private Long departmentId;
    @Column(name = "qty_in_hand", nullable = false) private java.math.BigDecimal qtyInHand = java.math.BigDecimal.ZERO;
    @Column(name = "blocked_qty", nullable = false) private java.math.BigDecimal blockedQty = java.math.BigDecimal.ZERO;
    @Column(name = "available_qty", nullable = false) private java.math.BigDecimal availableQty = java.math.BigDecimal.ZERO;
    @Column(name = "damaged_qty", nullable = false) private java.math.BigDecimal damagedQty = java.math.BigDecimal.ZERO;
    @Column(name = "expired_qty", nullable = false) private java.math.BigDecimal expiredQty = java.math.BigDecimal.ZERO;
    @Column(name = "avg_daily_demand") private java.math.BigDecimal avgDailyDemand;
    @Column(name = "safety_stock") private java.math.BigDecimal safetyStock;
    @Column(name = "is_reorder") private Boolean isReorder = false;
    @Column(name = "is_reorder_indent") private Boolean isReorderIndent = false;
    @Column(name = "last_ledger_id") private Long lastLedgerId;
    @Column(name = "version", nullable = false) private Long version = 0L;

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public java.math.BigDecimal getAvgDailyDemand() { return avgDailyDemand; }
    public void setAvgDailyDemand(java.math.BigDecimal avgDailyDemand) { this.avgDailyDemand = avgDailyDemand; }
    public java.math.BigDecimal getSafetyStock() { return safetyStock; }
    public void setSafetyStock(java.math.BigDecimal safetyStock) { this.safetyStock = safetyStock; }
    public java.math.BigDecimal getQtyInHand() { return qtyInHand; }
    public void setQtyInHand(java.math.BigDecimal qtyInHand) { this.qtyInHand = qtyInHand; }
    public java.math.BigDecimal getBlockedQty() { return blockedQty; }
    public void setBlockedQty(java.math.BigDecimal blockedQty) { this.blockedQty = blockedQty; }
    public java.math.BigDecimal getAvailableQty() { return availableQty; }
    public void setAvailableQty(java.math.BigDecimal availableQty) { this.availableQty = availableQty; }
    public java.math.BigDecimal getDamagedQty() { return damagedQty; }
    public void setDamagedQty(java.math.BigDecimal damagedQty) { this.damagedQty = damagedQty; }
    public java.math.BigDecimal getExpiredQty() { return expiredQty; }
    public void setExpiredQty(java.math.BigDecimal expiredQty) { this.expiredQty = expiredQty; }
    public Long getLastLedgerId() { return lastLedgerId; }
    public void setLastLedgerId(Long lastLedgerId) { this.lastLedgerId = lastLedgerId; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
