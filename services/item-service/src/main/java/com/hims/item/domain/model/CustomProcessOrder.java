package com.hims.item.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * Custom process order (M13). One-off request to an external or central supplier
 * for an item, with partial-receive semantics.
 * docStatus: 1=PENDING 2=APPROVED 3=PARTIALLY_RECEIVED 4=RECEIVED 5=CANCELLED.
 */
@Entity
@Table(name = "t_custom_process_order")
public class CustomProcessOrder extends ItemBase {
    public static final short STATUS_PENDING = 1;
    public static final short STATUS_APPROVED = 2;
    public static final short STATUS_PARTIALLY_RECEIVED = 3;
    public static final short STATUS_RECEIVED = 4;
    public static final short STATUS_CANCELLED = 5;

    @Id
    @Column(name = "id") private Long id;
    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }

    @Column(name = "cpo_no", nullable = false, length = 64, unique = true) private String cpoNo;
    @Column(name = "store_id", nullable = false) private Long storeId;
    @Column(name = "supplier_id") private Long supplierId;
    @Column(name = "dept_id") private Long deptId;
    @Column(name = "request_date") private Date requestDate;
    @Column(name = "required_date") private Date requiredDate;
    @Column(name = "remarks", length = 500) private String remarks;
    @Column(name = "doc_status", nullable = false) private Short docStatus = STATUS_PENDING;
    @Column(name = "approved_by") private Long approvedBy;
    @Column(name = "approved_datetime") private Timestamp approvedDatetime;

    public String getCpoNo() { return cpoNo; }
    public void setCpoNo(String v) { this.cpoNo = v; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long v) { this.storeId = v; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long v) { this.supplierId = v; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long v) { this.deptId = v; }
    public Date getRequestDate() { return requestDate; }
    public void setRequestDate(Date v) { this.requestDate = v; }
    public Date getRequiredDate() { return requiredDate; }
    public void setRequiredDate(Date v) { this.requiredDate = v; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String v) { this.remarks = v; }
    public Short getDocStatus() { return docStatus; }
    public void setDocStatus(Short v) { this.docStatus = v; }
    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long v) { this.approvedBy = v; }
    public Timestamp getApprovedDatetime() { return approvedDatetime; }
    public void setApprovedDatetime(Timestamp v) { this.approvedDatetime = v; }
}
