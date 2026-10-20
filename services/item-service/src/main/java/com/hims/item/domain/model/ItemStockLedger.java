package com.hims.item.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * NEW clean table: append-only movement ledger (AGT-02 §5.5 / V1 ihms_item).
 * Written in the same transaction as the balance update (single-writer D-1).
 * movement_type map (1..30) finalized at MIG-001.
 */
@Entity
@Table(name = "item_stock_ledger")
public class ItemStockLedger extends ItemBase {

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
    @Column(name = "movement_type", nullable = false) private Short movementType;
    @Column(name = "operation", nullable = false) private Short operation;   // 1=ADD 2=SUBTRACT
    @Column(name = "quantity", nullable = false) private java.math.BigDecimal quantity;
    @Column(name = "prev_balance", nullable = false) private java.math.BigDecimal prevBalance;
    @Column(name = "new_balance", nullable = false) private java.math.BigDecimal newBalance;
    @Column(name = "ref_doc_type") private String refDocType;
    @Column(name = "ref_doc_id") private Long refDocId;
    @Column(name = "grn_id") private Long grnId;
    @Column(name = "item_supplier_id") private Long itemSupplierId;
    @Column(name = "remarks") private String remarks;
    @Column(name = "is_cancel") private Boolean isCancel = false;

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public Short getMovementType() { return movementType; }
    public void setMovementType(Short movementType) { this.movementType = movementType; }
    public Short getOperation() { return operation; }
    public void setOperation(Short operation) { this.operation = operation; }
    public java.math.BigDecimal getQuantity() { return quantity; }
    public void setQuantity(java.math.BigDecimal quantity) { this.quantity = quantity; }
    public java.math.BigDecimal getPrevBalance() { return prevBalance; }
    public void setPrevBalance(java.math.BigDecimal prevBalance) { this.prevBalance = prevBalance; }
    public java.math.BigDecimal getNewBalance() { return newBalance; }
    public void setNewBalance(java.math.BigDecimal newBalance) { this.newBalance = newBalance; }
    public String getRefDocType() { return refDocType; }
    public void setRefDocType(String refDocType) { this.refDocType = refDocType; }
    public Long getRefDocId() { return refDocId; }
    public void setRefDocId(Long refDocId) { this.refDocId = refDocId; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
