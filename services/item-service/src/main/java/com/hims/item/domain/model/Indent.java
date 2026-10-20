package com.hims.item.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Indent header (legacy T_INDENT -> clean t_indent). doc_status: 1=PENDING
 * 2=APPROVED 3=REJECTED 4=CANCELLED. Id server-owned from ihms_item.t_indent_seq.
 */
@Entity
@Table(name = "t_indent")
public class Indent extends ItemBase {

    public static final short STATUS_PENDING = 1;
    public static final short STATUS_APPROVED = 2;

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "indent_no", nullable = false) private String indentNo;
    @Column(name = "store_id", nullable = false) private Long storeId;
    @Column(name = "department_id") private Long departmentId;
    @Column(name = "remarks") private String remarks;
    @Column(name = "doc_status", nullable = false) private Short docStatus = STATUS_PENDING;
    @Column(name = "approved_by") private Long approvedBy;
    @Column(name = "approved_datetime") private Timestamp approvedDatetime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIndentNo() { return indentNo; }
    public void setIndentNo(String indentNo) { this.indentNo = indentNo; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public Short getDocStatus() { return docStatus; }
    public void setDocStatus(Short docStatus) { this.docStatus = docStatus; }
    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }
    public Timestamp getApprovedDatetime() { return approvedDatetime; }
    public void setApprovedDatetime(Timestamp approvedDatetime) { this.approvedDatetime = approvedDatetime; }
}
