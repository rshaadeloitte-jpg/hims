package com.hims.bloodbank.domain.model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "t_bloodbag_trail")
public class BloodBagTrail extends BloodBankBase {
    public static final short EVENT_COLLECTED = 1;
    public static final short EVENT_CROSSMATCHED = 2;
    public static final short EVENT_ISSUED = 3;
    public static final short EVENT_RETURNED = 4;
    public static final short EVENT_DISCARDED = 5;
    public static final short EVENT_SEPARATED = 6;
    public static final short EVENT_INDENT_REQUESTED = 7;
    public static final short EVENT_INDENT_APPROVED = 8;
    public static final short EVENT_INDENT_ISSUED = 9;
    public static final short EVENT_INDENT_CANCELLED = 10;
    public static final short EVENT_CROSS_UNIT_REQUESTED = 11;
    public static final short EVENT_CROSS_UNIT_RECEIVED = 12;

    @Id
    @Column(name = "id") private Long id;
    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    @Column(name = "bag_id", nullable = false) private Long bagId;
    @Column(name = "event_type", nullable = false) private Short eventType;
    @Column(name = "event_at", nullable = false) private Timestamp eventAt;
    @Column(name = "actor_id") private Long actorId;
    @Column(name = "ref_doc_type", length = 32) private String refDocType;
    @Column(name = "ref_doc_id") private Long refDocId;
    @Column(name = "remarks", length = 500) private String remarks;
    public Long getBagId() { return bagId; }
    public void setBagId(Long v) { this.bagId = v; }
    public Short getEventType() { return eventType; }
    public void setEventType(Short v) { this.eventType = v; }
    public Timestamp getEventAt() { return eventAt; }
    public void setEventAt(Timestamp v) { this.eventAt = v; }
    public Long getActorId() { return actorId; }
    public void setActorId(Long v) { this.actorId = v; }
    public String getRefDocType() { return refDocType; }
    public void setRefDocType(String v) { this.refDocType = v; }
    public Long getRefDocId() { return refDocId; }
    public void setRefDocId(Long v) { this.refDocId = v; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String v) { this.remarks = v; }
}
