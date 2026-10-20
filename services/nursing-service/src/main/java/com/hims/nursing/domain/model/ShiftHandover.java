package com.hims.nursing.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

@Entity
@Table(name = "t_shift_handover")
public class ShiftHandover extends NursingBase {

    @Id
    @Column(name = "handover_id")
    private Long handoverId;

    @Column(name = "admission_id", nullable = false)
    private Long admissionId;

    @Column(name = "ward_id")
    private Long wardId;

    @Column(name = "shift", nullable = false, length = 20)
    private String shift;

    @Column(name = "situation", columnDefinition = "TEXT")
    private String situation;

    @Column(name = "background", columnDefinition = "TEXT")
    private String background;

    @Column(name = "assessment", columnDefinition = "TEXT")
    private String assessment;

    @Column(name = "recommendation", columnDefinition = "TEXT")
    private String recommendation;

    @Column(name = "pending_tasks", columnDefinition = "TEXT")
    private String pendingTasks;

    @Column(name = "handed_over_by", nullable = false)
    private Long handedOverBy;

    @Column(name = "received_by")
    private Long receivedBy;

    @Column(name = "signed_at")
    private Timestamp signedAt;

    @Override public Long getId() { return handoverId; }
    @Override public void setId(Long id) { this.handoverId = id; }
    public Long getHandoverId() { return handoverId; }
    public void setHandoverId(Long v) { this.handoverId = v; }
    public Long getAdmissionId() { return admissionId; }
    public void setAdmissionId(Long v) { this.admissionId = v; }
    public Long getWardId() { return wardId; }
    public void setWardId(Long v) { this.wardId = v; }
    public String getShift() { return shift; }
    public void setShift(String v) { this.shift = v; }
    public String getSituation() { return situation; }
    public void setSituation(String v) { this.situation = v; }
    public String getBackground() { return background; }
    public void setBackground(String v) { this.background = v; }
    public String getAssessment() { return assessment; }
    public void setAssessment(String v) { this.assessment = v; }
    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String v) { this.recommendation = v; }
    public String getPendingTasks() { return pendingTasks; }
    public void setPendingTasks(String v) { this.pendingTasks = v; }
    public Long getHandedOverBy() { return handedOverBy; }
    public void setHandedOverBy(Long v) { this.handedOverBy = v; }
    public Long getReceivedBy() { return receivedBy; }
    public void setReceivedBy(Long v) { this.receivedBy = v; }
    public Timestamp getSignedAt() { return signedAt; }
    public void setSignedAt(Timestamp v) { this.signedAt = v; }
}
