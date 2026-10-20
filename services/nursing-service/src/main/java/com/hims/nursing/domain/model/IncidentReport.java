package com.hims.nursing.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

@Entity
@Table(name = "t_incident_report")
public class IncidentReport extends NursingBase {

    public static final short REVIEW_PENDING = 1;
    public static final short REVIEW_REVIEWED = 2;
    public static final short REVIEW_CLOSED = 3;

    @Id
    @Column(name = "incident_id")
    private Long incidentId;

    @Column(name = "admission_id")
    private Long admissionId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "incident_type", nullable = false, length = 50)
    private String incidentType;

    @Column(name = "severity", nullable = false, length = 20)
    private String severity = "LOW";

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "occurred_at", nullable = false)
    private Timestamp occurredAt;

    @Column(name = "reported_by", nullable = false)
    private Long reportedBy;

    @Column(name = "review_status", nullable = false)
    private Short reviewStatus = REVIEW_PENDING;

    @Column(name = "reviewer_id")
    private Long reviewerId;

    @Column(name = "reviewed_at")
    private Timestamp reviewedAt;

    @Column(name = "corrective_action", columnDefinition = "TEXT")
    private String correctiveAction;

    @Override public Long getId() { return incidentId; }
    @Override public void setId(Long id) { this.incidentId = id; }
    public Long getIncidentId() { return incidentId; }
    public void setIncidentId(Long v) { this.incidentId = v; }
    public Long getAdmissionId() { return admissionId; }
    public void setAdmissionId(Long v) { this.admissionId = v; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long v) { this.patientId = v; }
    public String getIncidentType() { return incidentType; }
    public void setIncidentType(String v) { this.incidentType = v; }
    public String getSeverity() { return severity; }
    public void setSeverity(String v) { this.severity = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getLocation() { return location; }
    public void setLocation(String v) { this.location = v; }
    public Timestamp getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Timestamp v) { this.occurredAt = v; }
    public Long getReportedBy() { return reportedBy; }
    public void setReportedBy(Long v) { this.reportedBy = v; }
    public Short getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(Short v) { this.reviewStatus = v; }
    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long v) { this.reviewerId = v; }
    public Timestamp getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Timestamp v) { this.reviewedAt = v; }
    public String getCorrectiveAction() { return correctiveAction; }
    public void setCorrectiveAction(String v) { this.correctiveAction = v; }
}
