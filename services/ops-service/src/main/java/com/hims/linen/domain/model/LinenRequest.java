package com.hims.linen.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Clean target of T_LINENREQUEST — a linen/laundry request (store -> laundry
 * store -> back). request_status follows the 7-state lifecycle in
 * {@link LinenStatus}. requisition_no rule (legacy linen.no.template=%s/LR/%d/%s):
 * {unitPrefix}/LR/{year-2000}/{%05d seq}. Server-owned id from
 * linen_request_seq (IMP-DEC-003).
 */
@Entity
@Table(name = "linen_request")
public class LinenRequest extends LinenBase {

    @Id
    private Long id;

    @Column(name = "requisition_no", nullable = false)
    private String requisitionNo;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "to_store_id")
    private Long toStoreId;

    @Column(name = "request_date")
    private Timestamp requestDate;

    @Column(name = "request_status", nullable = false)
    private Long requestStatus = LinenStatus.PENDING;

    @Column(name = "handed_over_to")
    private String handedOverTo;

    @Column(name = "received_by")
    private String receivedBy;

    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }
    public String getRequisitionNo() { return requisitionNo; }
    public void setRequisitionNo(String requisitionNo) { this.requisitionNo = requisitionNo; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public Long getToStoreId() { return toStoreId; }
    public void setToStoreId(Long toStoreId) { this.toStoreId = toStoreId; }
    public Timestamp getRequestDate() { return requestDate; }
    public void setRequestDate(Timestamp requestDate) { this.requestDate = requestDate; }
    public Long getRequestStatus() { return requestStatus; }
    public void setRequestStatus(Long requestStatus) { this.requestStatus = requestStatus; }
    public String getHandedOverTo() { return handedOverTo; }
    public void setHandedOverTo(String handedOverTo) { this.handedOverTo = handedOverTo; }
    public String getReceivedBy() { return receivedBy; }
    public void setReceivedBy(String receivedBy) { this.receivedBy = receivedBy; }
}
