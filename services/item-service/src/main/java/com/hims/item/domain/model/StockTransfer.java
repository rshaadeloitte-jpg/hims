package com.hims.item.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Inter-store stock transfer (M13 gap). docStatus:
 * 1=REQUESTED 2=DISPATCHED 3=RECEIVED 4=CANCELLED.
 * Stock mutations reuse the D-1 single-writer movement API with
 * movementType 7=TRANSFER_OUT (dispatch) and 6=TRANSFER_IN (receive).
 */
@Entity
@Table(name = "t_stock_transfer")
public class StockTransfer extends ItemBase {

    public static final short STATUS_REQUESTED = 1;
    public static final short STATUS_DISPATCHED = 2;
    public static final short STATUS_RECEIVED = 3;
    public static final short STATUS_CANCELLED = 4;

    @Id
    @Column(name = "id")
    private Long id;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }

    @Column(name = "transfer_no", nullable = false, length = 64, unique = true)
    private String transferNo;

    @Column(name = "from_store_id", nullable = false)
    private Long fromStoreId;

    @Column(name = "to_store_id", nullable = false)
    private Long toStoreId;

    @Column(name = "doc_status", nullable = false)
    private Short docStatus = STATUS_REQUESTED;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "dispatched_by")
    private Long dispatchedBy;

    @Column(name = "dispatched_at")
    private Timestamp dispatchedAt;

    @Column(name = "received_by")
    private Long receivedBy;

    @Column(name = "received_at")
    private Timestamp receivedAt;

    public String getTransferNo() { return transferNo; }
    public void setTransferNo(String v) { this.transferNo = v; }
    public Long getFromStoreId() { return fromStoreId; }
    public void setFromStoreId(Long v) { this.fromStoreId = v; }
    public Long getToStoreId() { return toStoreId; }
    public void setToStoreId(Long v) { this.toStoreId = v; }
    public Short getDocStatus() { return docStatus; }
    public void setDocStatus(Short v) { this.docStatus = v; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String v) { this.remarks = v; }
    public Long getDispatchedBy() { return dispatchedBy; }
    public void setDispatchedBy(Long v) { this.dispatchedBy = v; }
    public Timestamp getDispatchedAt() { return dispatchedAt; }
    public void setDispatchedAt(Timestamp v) { this.dispatchedAt = v; }
    public Long getReceivedBy() { return receivedBy; }
    public void setReceivedBy(Long v) { this.receivedBy = v; }
    public Timestamp getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Timestamp v) { this.receivedAt = v; }
}
