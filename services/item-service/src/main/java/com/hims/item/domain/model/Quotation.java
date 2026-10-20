package com.hims.item.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * Quotation header (M14). docStatus:
 * 1=DRAFT 2=SUBMITTED 3=AWARDED 4=REJECTED 5=EXPIRED.
 */
@Entity
@Table(name = "t_quotation")
public class Quotation extends ItemBase {

    public static final short STATUS_DRAFT = 1;
    public static final short STATUS_SUBMITTED = 2;
    public static final short STATUS_AWARDED = 3;
    public static final short STATUS_REJECTED = 4;
    public static final short STATUS_EXPIRED = 5;

    @Id
    @Column(name = "id") private Long id;
    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }

    @Column(name = "quotation_no", nullable = false, length = 64, unique = true) private String quotationNo;
    @Column(name = "supplier_id", nullable = false) private Long supplierId;
    @Column(name = "enquiry_ref", length = 64) private String enquiryRef;
    @Column(name = "valid_until") private Date validUntil;
    @Column(name = "submitted_at") private Timestamp submittedAt;
    @Column(name = "awarded_at") private Timestamp awardedAt;
    @Column(name = "awarded_po_id") private Long awardedPoId;
    @Column(name = "remarks", length = 500) private String remarks;
    @Column(name = "doc_status", nullable = false) private Short docStatus = STATUS_DRAFT;

    public String getQuotationNo() { return quotationNo; }
    public void setQuotationNo(String v) { this.quotationNo = v; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long v) { this.supplierId = v; }
    public String getEnquiryRef() { return enquiryRef; }
    public void setEnquiryRef(String v) { this.enquiryRef = v; }
    public Date getValidUntil() { return validUntil; }
    public void setValidUntil(Date v) { this.validUntil = v; }
    public Timestamp getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Timestamp v) { this.submittedAt = v; }
    public Timestamp getAwardedAt() { return awardedAt; }
    public void setAwardedAt(Timestamp v) { this.awardedAt = v; }
    public Long getAwardedPoId() { return awardedPoId; }
    public void setAwardedPoId(Long v) { this.awardedPoId = v; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String v) { this.remarks = v; }
    public Short getDocStatus() { return docStatus; }
    public void setDocStatus(Short v) { this.docStatus = v; }
}
