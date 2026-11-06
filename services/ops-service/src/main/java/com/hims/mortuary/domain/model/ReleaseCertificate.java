package com.hims.mortuary.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Body release certificate tracking (clean target of release flow).
 *
 * BR-M28-003 release status (SMALLINT):
 *   1 = REQUESTED, 2 = APPROVED, 3 = RELEASED, 4 = REJECTED
 */
@Entity
@Table(name = "t_release_certificate")
public class ReleaseCertificate extends MortuaryBase {

    public static final short STATUS_REQUESTED = 1;
    public static final short STATUS_APPROVED = 2;
    public static final short STATUS_RELEASED = 3;
    public static final short STATUS_REJECTED = 4;

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "dead_body_id", nullable = false)
    private Long deadBodyId;

    @Column(name = "release_no", nullable = false, unique = true)
    private String releaseNo;

    @Column(name = "request_date", nullable = false)
    private Timestamp requestDate;

    @Column(name = "requested_by", nullable = false)
    private Long requestedBy;

    @Column(name = "release_status", nullable = false)
    private Short releaseStatus = STATUS_REQUESTED;

    @Column(name = "handover_to")
    private String handoverTo;

    @Column(name = "handover_relation")
    private String handoverRelation;

    @Column(name = "handover_phone")
    private String handoverPhone;

    @Column(name = "handover_date")
    private Timestamp handoverDate;

    @Column(name = "remark")
    private String remark;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDeadBodyId() { return deadBodyId; }
    public void setDeadBodyId(Long deadBodyId) { this.deadBodyId = deadBodyId; }
    public String getReleaseNo() { return releaseNo; }
    public void setReleaseNo(String releaseNo) { this.releaseNo = releaseNo; }
    public Timestamp getRequestDate() { return requestDate; }
    public void setRequestDate(Timestamp requestDate) { this.requestDate = requestDate; }
    public Long getRequestedBy() { return requestedBy; }
    public void setRequestedBy(Long requestedBy) { this.requestedBy = requestedBy; }
    public Short getReleaseStatus() { return releaseStatus; }
    public void setReleaseStatus(Short releaseStatus) { this.releaseStatus = releaseStatus; }
    public String getHandoverTo() { return handoverTo; }
    public void setHandoverTo(String handoverTo) { this.handoverTo = handoverTo; }
    public String getHandoverRelation() { return handoverRelation; }
    public void setHandoverRelation(String handoverRelation) { this.handoverRelation = handoverRelation; }
    public String getHandoverPhone() { return handoverPhone; }
    public void setHandoverPhone(String handoverPhone) { this.handoverPhone = handoverPhone; }
    public Timestamp getHandoverDate() { return handoverDate; }
    public void setHandoverDate(Timestamp handoverDate) { this.handoverDate = handoverDate; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
