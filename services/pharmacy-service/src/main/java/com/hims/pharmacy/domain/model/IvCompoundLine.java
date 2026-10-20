package com.hims.pharmacy.domain.model;

import jakarta.persistence.*;
import java.sql.Date;

@Entity
@Table(name = "t_iv_compound_line")
public class IvCompoundLine extends PharmacyBase {

    @Id
    @Column(name = "line_id")
    private Long lineId;

    @Column(name = "compound_id", nullable = false)
    private Long compoundId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(name = "quantity", nullable = false, precision = 10, scale = 3)
    private java.math.BigDecimal quantity;

    @Column(name = "uom", nullable = false, length = 20)
    private String uom;

    @Column(name = "batch_id", length = 50)
    private String batchId;

    @Column(name = "expiry_date")
    private Date expiryDate;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    public Long getLineId() { return lineId; }
    public void setLineId(Long lineId) { this.lineId = lineId; }
    @Override public Long getId() { return lineId; }
    @Override public void setId(Long id) { this.lineId = id; }
    public Long getCompoundId() { return compoundId; }
    public void setCompoundId(Long compoundId) { this.compoundId = compoundId; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public java.math.BigDecimal getQuantity() { return quantity; }
    public void setQuantity(java.math.BigDecimal quantity) { this.quantity = quantity; }
    public String getUom() { return uom; }
    public void setUom(String uom) { this.uom = uom; }
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public Date getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Date expiryDate) { this.expiryDate = expiryDate; }
    public Integer getLineNo() { return lineNo; }
    public void setLineNo(Integer lineNo) { this.lineNo = lineNo; }
}
