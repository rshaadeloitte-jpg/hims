package com.hims.pharmacy.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * One line of the pharmacy-local prescription dispense projection (legacy
 * T_PRESCRIPTIONDISPENSEDETAILS semantics, GAP-REGISTER-5 WP-R2).
 *
 * BR-PHAR-002 detail rules (pinned by tests):
 *   full-qty dispense of the linked sale line -&gt; isClosed=true, pendingQty=0;
 *   partial dispense                          -&gt; pendingQty -= dispensed qty, stays open;
 *   master closes when ALL details are closed.
 */
@Entity
@IdClass(PrescriptionDispenseDetailKey.class)
@Table(name = "t_prescription_dispense_detail")
public class PrescriptionDispenseDetail extends PharmacyBase {

    @Id
    @Column(name = "dispense_id")
    private Long dispenseId;

    @Id
    @Column(name = "line_no")
    private Long lineNo;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "qty", nullable = false)
    private Integer qty;

    @Column(name = "pending_qty", nullable = false)
    private Integer pendingQty;

    /** Legacy IsClosed='1' — true once pendingQty reaches 0 via dispensing. */
    @Column(name = "is_closed", nullable = false)
    private Boolean closed = false;

    @Override public Long getId() { return dispenseId; }
    @Override public void setId(Long id) { this.dispenseId = id; }
    public Long getDispenseId() { return dispenseId; }
    public void setDispenseId(Long dispenseId) { this.dispenseId = dispenseId; }
    public Long getLineNo() { return lineNo; }
    public void setLineNo(Long lineNo) { this.lineNo = lineNo; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Integer getQty() { return qty; }
    public void setQty(Integer qty) { this.qty = qty; }
    public Integer getPendingQty() { return pendingQty; }
    public void setPendingQty(Integer pendingQty) { this.pendingQty = pendingQty; }
    public Boolean getClosed() { return closed; }
    public void setClosed(Boolean closed) { this.closed = closed; }
}
