package com.hims.bloodbank.domain.model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Date;
import java.sql.Timestamp;

@Entity
@Table(name = "t_bloodbank_indent")
public class BloodBankIndent extends BloodBankBase {
    public static final short STATUS_NEW = 1;
    public static final short STATUS_APPROVED = 2;
    public static final short STATUS_ISSUED = 3;
    public static final short STATUS_CANCELLED = 4;

    @Id
    @Column(name = "id") private Long id;
    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    @Column(name = "indent_no", nullable = false, length = 64, unique = true) private String indentNo;
    @Column(name = "from_store_id", nullable = false) private Long fromStoreId;
    @Column(name = "to_store_id", nullable = false) private Long toStoreId;
    @Column(name = "blood_group", nullable = false, length = 5) private String bloodGroup;
    @Column(name = "component", nullable = false, length = 100) private String component;
    @Column(name = "quantity", nullable = false) private Integer quantity;
    @Column(name = "requested_date", nullable = false) private Date requestedDate;
    @Column(name = "remarks", length = 500) private String remarks;
    @Column(name = "doc_status", nullable = false) private Short docStatus = STATUS_NEW;
    @Column(name = "approved_by") private Long approvedBy;
    @Column(name = "approved_datetime") private Timestamp approvedDatetime;
    @Column(name = "issued_at") private Timestamp issuedAt;
    public String getIndentNo() { return indentNo; }
    public void setIndentNo(String v) { this.indentNo = v; }
    public Long getFromStoreId() { return fromStoreId; }
    public void setFromStoreId(Long v) { this.fromStoreId = v; }
    public Long getToStoreId() { return toStoreId; }
    public void setToStoreId(Long v) { this.toStoreId = v; }
    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String v) { this.bloodGroup = v; }
    public String getComponent() { return component; }
    public void setComponent(String v) { this.component = v; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer v) { this.quantity = v; }
    public Date getRequestedDate() { return requestedDate; }
    public void setRequestedDate(Date v) { this.requestedDate = v; }
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
}
