package com.hims.pharmacy.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * One drug/item line of an item sale (GAP-REGISTER-5 WP-R2): item_id NOT NULL,
 * qty INT CHECK qty &gt; 0, pending_qty tracks what is still to be dispensed,
 * batch_id VARCHAR(50) as the register specifies (the item-service stock call maps
 * it to the numeric batch reference when it parses as a number — see ItemStockClient).
 */
@Entity
@IdClass(ItemSaleDetailKey.class)
@Table(name = "t_item_sale_detail")
public class ItemSaleDetail extends PharmacyBase {

    @Id
    @Column(name = "sale_id")
    private Long saleId;

    @Id
    @Column(name = "line_no")
    private Long lineNo;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "batch_id", length = 50)
    private String batchId;

    @Column(name = "qty", nullable = false)
    private Integer qty;

    @Column(name = "pending_qty", nullable = false)
    private Integer pendingQty;

    /** BR-PHAR-001 line flag: true once pending_qty reaches 0 via dispensing. */
    @Column(name = "dispensed", nullable = false)
    private Boolean dispensed = false;

    /** Provisioned per register; no WP-R2 API sets it yet — stays FALSE (verification flow is a later slice). */
    @Column(name = "is_verified", nullable = false)
    private Boolean verified = false;

    @Column(name = "is_partial_dispense", nullable = false)
    private Boolean partialDispense = false;

    /** Composite key has no single surrogate id (base contract satisfied structurally). */
    @Override public Long getId() { return saleId; }
    @Override public void setId(Long id) { this.saleId = id; }
    public Long getSaleId() { return saleId; }
    public void setSaleId(Long saleId) { this.saleId = saleId; }
    public Long getLineNo() { return lineNo; }
    public void setLineNo(Long lineNo) { this.lineNo = lineNo; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public Integer getQty() { return qty; }
    public void setQty(Integer qty) { this.qty = qty; }
    public Integer getPendingQty() { return pendingQty; }
    public void setPendingQty(Integer pendingQty) { this.pendingQty = pendingQty; }
    public Boolean getDispensed() { return dispensed; }
    public void setDispensed(Boolean dispensed) { this.dispensed = dispensed; }
    public Boolean getVerified() { return verified; }
    public void setVerified(Boolean verified) { this.verified = verified; }
    public Boolean getPartialDispense() { return partialDispense; }
    public void setPartialDispense(Boolean partialDispense) { this.partialDispense = partialDispense; }
}
