package com.hims.item.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.sql.Date;

/**
 * Rate contract (M14). statusFlag: 1=ACTIVE 2=EXPIRED 3=CANCELLED.
 * Only one ACTIVE contract per (supplier, item) is allowed at a time.
 */
@Entity
@Table(name = "t_rate_contract")
public class RateContract extends ItemBase {

    public static final short STATUS_ACTIVE = 1;
    public static final short STATUS_EXPIRED = 2;
    public static final short STATUS_CANCELLED = 3;

    @Id
    @Column(name = "id") private Long id;
    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }

    @Column(name = "contract_no", nullable = false, length = 64, unique = true) private String contractNo;
    @Column(name = "supplier_id", nullable = false) private Long supplierId;
    @Column(name = "item_id", nullable = false) private Long itemId;
    @Column(name = "contract_price", nullable = false, precision = 19, scale = 2) private BigDecimal contractPrice;
    @Column(name = "tax_percent", nullable = false, precision = 6, scale = 2) private BigDecimal taxPercent = BigDecimal.ZERO;
    @Column(name = "min_order_qty", precision = 19, scale = 4) private BigDecimal minOrderQty;
    @Column(name = "valid_from", nullable = false) private Date validFrom;
    @Column(name = "valid_to", nullable = false) private Date validTo;
    @Column(name = "status_flag", nullable = false) private Short statusFlag = STATUS_ACTIVE;
    @Column(name = "remarks", length = 500) private String remarks;

    public String getContractNo() { return contractNo; }
    public void setContractNo(String v) { this.contractNo = v; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long v) { this.supplierId = v; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long v) { this.itemId = v; }
    public BigDecimal getContractPrice() { return contractPrice; }
    public void setContractPrice(BigDecimal v) { this.contractPrice = v; }
    public BigDecimal getTaxPercent() { return taxPercent; }
    public void setTaxPercent(BigDecimal v) { this.taxPercent = v; }
    public BigDecimal getMinOrderQty() { return minOrderQty; }
    public void setMinOrderQty(BigDecimal v) { this.minOrderQty = v; }
    public Date getValidFrom() { return validFrom; }
    public void setValidFrom(Date v) { this.validFrom = v; }
    public Date getValidTo() { return validTo; }
    public void setValidTo(Date v) { this.validTo = v; }
    public Short getStatusFlag() { return statusFlag; }
    public void setStatusFlag(Short v) { this.statusFlag = v; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String v) { this.remarks = v; }
}
