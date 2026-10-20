package com.hims.item.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Expiry/damage return header (M13 gap). Ported from legacy
 * ExpiryItemServiceImpl (CIMS_ADDITEMEXPIRYRETURN).
 * returnType: 1=EXPIRED 2=DAMAGED 3=BOTH.
 * docStatus: 1=PENDING 2=APPROVED 3=REJECTED 4=CANCELLED 5=RETURNED_TO_SUPPLIER.
 */
@Entity
@Table(name = "t_expiry_return")
public class ExpiryReturn extends ItemBase {

    public static final short TYPE_EXPIRED = 1;
    public static final short TYPE_DAMAGED = 2;
    public static final short TYPE_BOTH = 3;

    public static final short STATUS_PENDING = 1;
    public static final short STATUS_APPROVED = 2;
    public static final short STATUS_REJECTED = 3;
    public static final short STATUS_CANCELLED = 4;
    public static final short STATUS_RETURNED = 5;

    @Id
    @Column(name = "id")
    private Long id;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }

    @Column(name = "return_no", nullable = false, length = 64, unique = true)
    private String returnNo;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "return_type", nullable = false)
    private Short returnType = TYPE_EXPIRED;

    @Column(name = "doc_status", nullable = false)
    private Short docStatus = STATUS_PENDING;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_datetime")
    private Timestamp approvedDatetime;

    @Column(name = "returned_at")
    private Timestamp returnedAt;

    @Column(name = "credit_note_no", length = 64)
    private String creditNoteNo;

    @Column(name = "credit_note_amount", precision = 19, scale = 2)
    private BigDecimal creditNoteAmount;

    public String getReturnNo() { return returnNo; }
    public void setReturnNo(String v) { this.returnNo = v; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long v) { this.storeId = v; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long v) { this.supplierId = v; }
    public Short getReturnType() { return returnType; }
    public void setReturnType(Short v) { this.returnType = v; }
    public Short getDocStatus() { return docStatus; }
    public void setDocStatus(Short v) { this.docStatus = v; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String v) { this.remarks = v; }
    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long v) { this.approvedBy = v; }
    public Timestamp getApprovedDatetime() { return approvedDatetime; }
    public void setApprovedDatetime(Timestamp v) { this.approvedDatetime = v; }
    public Timestamp getReturnedAt() { return returnedAt; }
    public void setReturnedAt(Timestamp v) { this.returnedAt = v; }
    public String getCreditNoteNo() { return creditNoteNo; }
    public void setCreditNoteNo(String v) { this.creditNoteNo = v; }
    public BigDecimal getCreditNoteAmount() { return creditNoteAmount; }
    public void setCreditNoteAmount(BigDecimal v) { this.creditNoteAmount = v; }
}
