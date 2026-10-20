package com.hims.item.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * Purchase order header (M14). docStatus:
 * 1=DRAFT 2=PENDING_APPROVAL 3=APPROVED 4=ISSUED 5=CLOSED 6=CANCELLED.
 */
@Entity
@Table(name = "t_purchase_order")
public class PurchaseOrder extends ItemBase {

    public static final short STATUS_DRAFT = 1;
    public static final short STATUS_PENDING_APPROVAL = 2;
    public static final short STATUS_APPROVED = 3;
    public static final short STATUS_ISSUED = 4;
    public static final short STATUS_CLOSED = 5;
    public static final short STATUS_CANCELLED = 6;

    @Id
    @Column(name = "id") private Long id;
    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }

    @Column(name = "po_no", nullable = false, length = 64, unique = true) private String poNo;
    @Column(name = "quotation_id") private Long quotationId;
    @Column(name = "store_id", nullable = false) private Long storeId;
    @Column(name = "supplier_id", nullable = false) private Long supplierId;
    @Column(name = "order_date") private Date orderDate;
    @Column(name = "expected_date") private Date expectedDate;
    @Column(name = "remarks", length = 500) private String remarks;
    @Column(name = "doc_status", nullable = false) private Short docStatus = STATUS_DRAFT;
    @Column(name = "approved_by") private Long approvedBy;
    @Column(name = "approved_datetime") private Timestamp approvedDatetime;
    @Column(name = "issued_at") private Timestamp issuedAt;
    @Column(name = "closed_at") private Timestamp closedAt;

    public String getPoNo() { return poNo; }
    public void setPoNo(String v) { this.poNo = v; }
    public Long getQuotationId() { return quotationId; }
    public void setQuotationId(Long v) { this.quotationId = v; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long v) { this.storeId = v; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long v) { this.supplierId = v; }
    public Date getOrderDate() { return orderDate; }
    public void setOrderDate(Date v) { this.orderDate = v; }
    public Date getExpectedDate() { return expectedDate; }
    public void setExpectedDate(Date v) { this.expectedDate = v; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String v) { this.remarks = v; }
    public Short getDocStatus() { return docStatus; }
    public void setDocStatus(Short v) { this.docStatus = v; }
    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long v) { this.approvedBy = v; }
    public Timestamp getApprovedDatetime() { return approvedDatetime; }
    public void setApprovedDatetime(Timestamp v) { this.approvedDatetime = v; }
    public Timestamp getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Timestamp v) { this.issuedAt = v; }
    public Timestamp getClosedAt() { return closedAt; }
    public void setClosedAt(Timestamp v) { this.closedAt = v; }
}
