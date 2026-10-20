package com.hims.item.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Indent line (legacy T_INDENTDETAIL -> clean t_indent_detail). */
@Entity
@Table(name = "t_indent_detail")
public class IndentDetail extends ItemBase {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "indent_id", nullable = false) private Long indentId;
    @Column(name = "item_id", nullable = false) private Long itemId;
    @Column(name = "quantity", nullable = false) private BigDecimal quantity;
    @Column(name = "issued_qty", nullable = false) private BigDecimal issuedQty = BigDecimal.ZERO;
    @Column(name = "remarks") private String remarks;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getIndentId() { return indentId; }
    public void setIndentId(Long indentId) { this.indentId = indentId; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getIssuedQty() { return issuedQty; }
    public void setIssuedQty(BigDecimal issuedQty) { this.issuedQty = issuedQty; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
