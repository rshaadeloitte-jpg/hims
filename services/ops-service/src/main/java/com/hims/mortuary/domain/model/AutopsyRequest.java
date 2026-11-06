package com.hims.mortuary.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Autopsy request tracking (clean target of T_MORTUARYVERIFICATIONS / autopsy flow).
 *
 * BR-M28-002 request status (SMALLINT):
 *   1 = PENDING, 2 = APPROVED, 3 = COMPLETED, 4 = REJECTED
 */
@Entity
@Table(name = "t_autopsy_request")
public class AutopsyRequest extends MortuaryBase {

    public static final short STATUS_PENDING = 1;
    public static final short STATUS_APPROVED = 2;
    public static final short STATUS_COMPLETED = 3;
    public static final short STATUS_REJECTED = 4;

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "dead_body_id", nullable = false)
    private Long deadBodyId;

    @Column(name = "request_date", nullable = false)
    private Timestamp requestDate;

    @Column(name = "requested_by", nullable = false)
    private Long requestedBy;

    @Column(name = "request_status", nullable = false)
    private Short requestStatus = STATUS_PENDING;

    @Column(name = "reason")
    private String reason;

    @Column(name = "doctor_id")
    private Long doctorId;

    @Column(name = "autopsy_date")
    private Timestamp autopsyDate;

    @Column(name = "findings")
    private String findings;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDeadBodyId() { return deadBodyId; }
    public void setDeadBodyId(Long deadBodyId) { this.deadBodyId = deadBodyId; }
    public Timestamp getRequestDate() { return requestDate; }
    public void setRequestDate(Timestamp requestDate) { this.requestDate = requestDate; }
    public Long getRequestedBy() { return requestedBy; }
    public void setRequestedBy(Long requestedBy) { this.requestedBy = requestedBy; }
    public Short getRequestStatus() { return requestStatus; }
    public void setRequestStatus(Short requestStatus) { this.requestStatus = requestStatus; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
    public Timestamp getAutopsyDate() { return autopsyDate; }
    public void setAutopsyDate(Timestamp autopsyDate) { this.autopsyDate = autopsyDate; }
    public String getFindings() { return findings; }
    public void setFindings(String findings) { this.findings = findings; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
