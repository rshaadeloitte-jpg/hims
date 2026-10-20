package com.hims.item.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Purchase order line. received_qty accumulates on each GRN receive. */
@Entity
@Table(name = "t_purchase_order_detail")
public class PurchaseOrderDetail extends ItemBase {
    @Id
    @Column(name = "id") private Long id;
    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }

    @Column(name = "po_id", nullable = false) private Long poId;
    @Column(name = "item_id", nullable = false) private Long itemId;
    @Column(name = "ordered_qty", nullable = false, precision = 19, scale = 4) private BigDecimal orderedQty;
    @Column(name = "received_qty", nullable = false, precision = 19, scale = 4) private BigDecimal receivedQty = BigDecimal.ZERO;
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2) private BigDecimal unitPrice;
    @Column(name = "tax_percent", nullable = false, precision = 6, scale = 2) private BigDecimal taxPercent = BigDecimal.ZERO;
    @Column(name = "lead_time_days") private Integer leadTimeDays;
    @Column(name = "remarks", length = 500) private String remarks;

    public Long getPoId() { return poId; }
    public void setPoId(Long v) { this.poId = v; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long v) { this.itemId = v; }
    public BigDecimal getOrderedQty() { return orderedQty; }
    public void setOrderedQty(BigDecimal v) { this.orderedQty = v; }
    public BigDecimal getReceivedQty() { return receivedQty; }
    public void setReceivedQty(BigDecimal v) { this.receivedQty = v; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal v) { this.unitPrice = v; }
    public BigDecimal getTaxPercent() { return taxPercent; }
    public void setTaxPercent(BigDecimal v) { this.taxPercent = v; }
    public Integer getLeadTimeDays() { return leadTimeDays; }
    public void setLeadTimeDays(Integer v) { this.leadTimeDays = v; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String v) { this.remarks = v; }
}
