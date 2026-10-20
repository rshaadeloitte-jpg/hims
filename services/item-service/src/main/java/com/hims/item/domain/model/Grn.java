package com.hims.item.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * GRN header (legacy T_GRN -> clean t_grn). doc_status: 1=PENDING 2=APPROVED
 * 3=REJECTED 4=CANCELLED. Id server-owned from ihms_item.t_grn_seq (IMP-DEC-003).
 */
@Entity
@Table(name = "t_grn")
public class Grn extends ItemBase {

    public static final short STATUS_PENDING = 1;
    public static final short STATUS_APPROVED = 2;

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "grn_no", nullable = false) private String grnNo;
    @Column(name = "store_id", nullable = false) private Long storeId;
    @Column(name = "supplier_id") private Long supplierId;
    @Column(name = "invoice_no") private String invoiceNo;
    @Column(name = "invoice_date") private java.sql.Date invoiceDate;
    @Column(name = "received_date") private java.sql.Date receivedDate;
    @Column(name = "remarks") private String remarks;
    @Column(name = "doc_status", nullable = false) private Short docStatus = STATUS_PENDING;
    @Column(name = "approved_by") private Long approvedBy;
    @Column(name = "approved_datetime") private Timestamp approvedDatetime;
    @Column(name = "provisional", nullable = false) private Boolean provisional = false;
    @Column(name = "consignment", nullable = false) private Boolean consignment = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getGrnNo() { return grnNo; }
    public void setGrnNo(String grnNo) { this.grnNo = grnNo; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }
    public java.sql.Date getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(java.sql.Date invoiceDate) { this.invoiceDate = invoiceDate; }
    public java.sql.Date getReceivedDate() { return receivedDate; }
    public void setReceivedDate(java.sql.Date receivedDate) { this.receivedDate = receivedDate; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public Short getDocStatus() { return docStatus; }
    public void setDocStatus(Short docStatus) { this.docStatus = docStatus; }
    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }
    public Timestamp getApprovedDatetime() { return approvedDatetime; }
    public void setApprovedDatetime(Timestamp approvedDatetime) { this.approvedDatetime = approvedDatetime; }
    public Boolean getProvisional() { return provisional; }
    public void setProvisional(Boolean v) { this.provisional = v; }
    public Boolean getConsignment() { return consignment; }
    public void setConsignment(Boolean v) { this.consignment = v; }
}
