package com.hims.pharmacy.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Per-line return quantity and refund. Composite key (return_id, line_no).
 */
@Entity
@Table(name = "t_sale_return_line")
@IdClass(SaleReturnLine.Key.class)
public class SaleReturnLine extends PharmacyBase {

    @Id
    @Column(name = "return_id")
    private Long returnId;

    @Id
    @Column(name = "line_no")
    private Long lineNo;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "batch_id", length = 50)
    private String batchId;

    @Column(name = "return_qty", nullable = false)
    private Integer returnQty;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "refund_amount", nullable = false)
    private BigDecimal refundAmount;

    @Column(name = "line_reason", length = 200)
    private String lineReason;

    public Long getReturnId() { return returnId; }
    public void setReturnId(Long returnId) { this.returnId = returnId; }
    @Override public Long getId() { return returnId; }
    @Override public void setId(Long id) { this.returnId = id; }
    public Long getLineNo() { return lineNo; }
    public void setLineNo(Long lineNo) { this.lineNo = lineNo; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public Integer getReturnQty() { return returnQty; }
    public void setReturnQty(Integer returnQty) { this.returnQty = returnQty; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
    public String getLineReason() { return lineReason; }
    public void setLineReason(String lineReason) { this.lineReason = lineReason; }

    public static class Key implements Serializable {
        private Long returnId;
        private Long lineNo;
        public Key() {}
        public Key(Long returnId, Long lineNo) { this.returnId = returnId; this.lineNo = lineNo; }
        public Long getReturnId() { return returnId; }
        public void setReturnId(Long returnId) { this.returnId = returnId; }
        public Long getLineNo() { return lineNo; }
        public void setLineNo(Long lineNo) { this.lineNo = lineNo; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key k)) return false;
            return Objects.equals(returnId, k.returnId) && Objects.equals(lineNo, k.lineNo);
        }
        @Override public int hashCode() { return Objects.hash(returnId, lineNo); }
    }
}
