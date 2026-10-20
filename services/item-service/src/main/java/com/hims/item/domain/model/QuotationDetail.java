package com.hims.item.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Quotation line. */
@Entity
@Table(name = "t_quotation_detail")
public class QuotationDetail extends ItemBase {
    @Id
    @Column(name = "id") private Long id;
    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }

    @Column(name = "quotation_id", nullable = false) private Long quotationId;
    @Column(name = "item_id", nullable = false) private Long itemId;
    @Column(name = "quantity", nullable = false, precision = 19, scale = 4) private BigDecimal quantity;
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2) private BigDecimal unitPrice;
    @Column(name = "tax_percent", nullable = false, precision = 6, scale = 2) private BigDecimal taxPercent = BigDecimal.ZERO;
    @Column(name = "lead_time_days") private Integer leadTimeDays;
    @Column(name = "remarks", length = 500) private String remarks;

    public Long getQuotationId() { return quotationId; }
    public void setQuotationId(Long v) { this.quotationId = v; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long v) { this.itemId = v; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal v) { this.quantity = v; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal v) { this.unitPrice = v; }
    public BigDecimal getTaxPercent() { return taxPercent; }
    public void setTaxPercent(BigDecimal v) { this.taxPercent = v; }
    public Integer getLeadTimeDays() { return leadTimeDays; }
    public void setLeadTimeDays(Integer v) { this.leadTimeDays = v; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String v) { this.remarks = v; }
}
