package com.hims.linen.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Clean target of T_LINENREQUESTDETAILS — one row per requested linen item
 * line. item_id / batch_id reference ihms_item (item-service) — cross-domain
 * FK deferred until the item-service integration slice (dependency-neutral).
 * Quantity columns track the lifecycle: quantity (raised) -> receivedQuantity
 * (main-store receive) -> dispatchedQuantity (dispatch) -> finalQuantity
 * (complete); damagedQuantity + reason captured at receive/dispatch.
 * Server-owned id from linen_request_details_seq (IMP-DEC-003).
 */
@Entity
@Table(name = "linen_request_details")
public class LinenRequestDetails extends LinenBase {

    @Id
    private Long id;

    @Column(name = "linen_request_id", nullable = false)
    private Long linenRequestId;

    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "batch_id")
    private Long batchId;

    @Column(name = "quantity", nullable = false)
    private Long quantity = 0L;

    @Column(name = "remark")
    private String remark;

    @Column(name = "received_quantity")
    private Long receivedQuantity;

    @Column(name = "damaged_quantity")
    private Long damagedQuantity;

    @Column(name = "dispatched_quantity")
    private Long dispatchedQuantity;

    @Column(name = "final_quantity")
    private Long finalQuantity;

    @Column(name = "reason")
    private String reason = "";

    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }
    public Long getLinenRequestId() { return linenRequestId; }
    public void setLinenRequestId(Long linenRequestId) { this.linenRequestId = linenRequestId; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public Long getQuantity() { return quantity; }
    public void setQuantity(Long quantity) { this.quantity = quantity; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Long getReceivedQuantity() { return receivedQuantity; }
    public void setReceivedQuantity(Long receivedQuantity) { this.receivedQuantity = receivedQuantity; }
    public Long getDamagedQuantity() { return damagedQuantity; }
    public void setDamagedQuantity(Long damagedQuantity) { this.damagedQuantity = damagedQuantity; }
    public Long getDispatchedQuantity() { return dispatchedQuantity; }
    public void setDispatchedQuantity(Long dispatchedQuantity) { this.dispatchedQuantity = dispatchedQuantity; }
    public Long getFinalQuantity() { return finalQuantity; }
    public void setFinalQuantity(Long finalQuantity) { this.finalQuantity = finalQuantity; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
