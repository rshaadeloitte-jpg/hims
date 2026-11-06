package com.hims.bloodbank.domain.model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "t_bloodbank_cross_unit")
public class BloodBankCrossUnit extends BloodBankBase {
    public static final short STATUS_REQUESTED = 1;
    public static final short STATUS_ACKNOWLEDGED = 2;
    public static final short STATUS_RECEIVED = 3;
    public static final short STATUS_CANCELLED = 4;

    @Id
    @Column(name = "id") private Long id;
    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    @Column(name = "request_no", nullable = false, length = 64, unique = true) private String requestNo;
    @Column(name = "from_bloodbank_id", nullable = false) private Long fromBloodbankId;
    @Column(name = "to_bloodbank_id", nullable = false) private Long toBloodbankId;
    @Column(name = "blood_group", nullable = false, length = 5) private String bloodGroup;
    @Column(name = "component", nullable = false, length = 100) private String component;
    @Column(name = "quantity", nullable = false) private Integer quantity;
    @Column(name = "remarks", length = 500) private String remarks;
    @Column(name = "doc_status", nullable = false) private Short docStatus = STATUS_REQUESTED;
    @Column(name = "acknowledged_by") private Long acknowledgedBy;
    @Column(name = "acknowledged_datetime") private Timestamp acknowledgedDatetime;
    @Column(name = "received_by") private Long receivedBy;
    @Column(name = "received_datetime") private Timestamp receivedDatetime;
    public String getRequestNo() { return requestNo; }
    public void setRequestNo(String v) { this.requestNo = v; }
    public Long getFromBloodbankId() { return fromBloodbankId; }
    public void setFromBloodbankId(Long v) { this.fromBloodbankId = v; }
    public Long getToBloodbankId() { return toBloodbankId; }
    public void setToBloodbankId(Long v) { this.toBloodbankId = v; }
    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String v) { this.bloodGroup = v; }
    public String getComponent() { return component; }
    public void setComponent(String v) { this.component = v; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer v) { this.quantity = v; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String v) { this.remarks = v; }
    public Short getDocStatus() { return docStatus; }
    public void setDocStatus(Short v) { this.docStatus = v; }
    public Long getAcknowledgedBy() { return acknowledgedBy; }
    public void setAcknowledgedBy(Long v) { this.acknowledgedBy = v; }
    public Timestamp getAcknowledgedDatetime() { return acknowledgedDatetime; }
    public void setAcknowledgedDatetime(Timestamp v) { this.acknowledgedDatetime = v; }
    public Long getReceivedBy() { return receivedBy; }
    public void setReceivedBy(Long v) { this.receivedBy = v; }
    public Timestamp getReceivedDatetime() { return receivedDatetime; }
    public void setReceivedDatetime(Timestamp v) { this.receivedDatetime = v; }
}
