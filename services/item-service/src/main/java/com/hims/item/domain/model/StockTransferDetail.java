package com.hims.item.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Inter-store transfer line. received_qty accumulates on receive (partial
 * receipt allowed per line, mirroring indent issue semantics).
 */
@Entity
@Table(name = "t_stock_transfer_detail")
public class StockTransferDetail extends ItemBase {

    @Id
    @Column(name = "id")
    private Long id;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }

    @Column(name = "transfer_id", nullable = false)
    private Long transferId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "batch_id")
    private Long batchId;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "received_qty", nullable = false, precision = 19, scale = 4)
    private BigDecimal receivedQty = BigDecimal.ZERO;

    @Column(name = "remarks", length = 500)
    private String remarks;

    public Long getTransferId() { return transferId; }
    public void setTransferId(Long v) { this.transferId = v; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long v) { this.itemId = v; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long v) { this.batchId = v; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal v) { this.quantity = v; }
    public BigDecimal getReceivedQty() { return receivedQty; }
    public void setReceivedQty(BigDecimal v) { this.receivedQty = v; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String v) { this.remarks = v; }
}
