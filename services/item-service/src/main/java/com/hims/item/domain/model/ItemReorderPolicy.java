package com.hims.item.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Per-store reorder policy (V1 table item_reorder_policy).
 * Legacy parity: ReorderAndSafetyMaster + M_ITEMMASTER.reorderQty/safetyStock.
 * reorder_level triggers ROL alert when available_qty <= reorder_level.
 */
@Entity
@Table(name = "item_reorder_policy")
public class ItemReorderPolicy extends ItemBase {

    @Id
    @Column(name = "id")
    private Long id;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "reorder_level", precision = 19, scale = 4)
    private BigDecimal reorderLevel;

    @Column(name = "safety_stock", precision = 19, scale = 4)
    private BigDecimal safetyStock;

    @Column(name = "reorder_value", precision = 19, scale = 2)
    private BigDecimal reorderValue;

    @Column(name = "is_automatic", nullable = false)
    private Boolean isAutomatic = false;

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long v) { this.storeId = v; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long v) { this.itemId = v; }
    public BigDecimal getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(BigDecimal v) { this.reorderLevel = v; }
    public BigDecimal getSafetyStock() { return safetyStock; }
    public void setSafetyStock(BigDecimal v) { this.safetyStock = v; }
    public BigDecimal getReorderValue() { return reorderValue; }
    public void setReorderValue(BigDecimal v) { this.reorderValue = v; }
    public Boolean getIsAutomatic() { return isAutomatic; }
    public void setIsAutomatic(Boolean v) { this.isAutomatic = v; }
}
