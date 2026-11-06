package com.hims.bloodbank.domain.model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "t_bloodbag_return")
public class BloodBagReturn extends BloodBankBase {
    public static final short STATUS_PENDING = 1;
    public static final short STATUS_RETURNED = 2;
    public static final short STATUS_REJECTED = 3;

    @Id
    @Column(name = "id") private Long id;
    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    @Column(name = "return_no", nullable = false, length = 64, unique = true) private String returnNo;
    @Column(name = "bag_id", nullable = false) private Long bagId;
    @Column(name = "reason", length = 500) private String reason;
    @Column(name = "returned_to", length = 200) private String returnedTo;
    @Column(name = "doc_status", nullable = false) private Short docStatus = STATUS_PENDING;
    @Column(name = "approved_by") private Long approvedBy;
    @Column(name = "approved_datetime") private Timestamp approvedDatetime;
    @Column(name = "completed_at") private Timestamp completedAt;
    public String getReturnNo() { return returnNo; }
    public void setReturnNo(String v) { this.returnNo = v; }
    public Long getBagId() { return bagId; }
    public void setBagId(Long v) { this.bagId = v; }
    public String getReason() { return reason; }
    public void setReason(String v) { this.reason = v; }
    public String getReturnedTo() { return returnedTo; }
    public void setReturnedTo(String v) { this.returnedTo = v; }
    public Short getDocStatus() { return docStatus; }
    public void setDocStatus(Short v) { this.docStatus = v; }
    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long v) { this.approvedBy = v; }
    public Timestamp getApprovedDatetime() { return approvedDatetime; }
    public void setApprovedDatetime(Timestamp v) { this.approvedDatetime = v; }
    public Timestamp getCompletedAt() { return completedAt; }
    public void setCompletedAt(Timestamp v) { this.completedAt = v; }
}
