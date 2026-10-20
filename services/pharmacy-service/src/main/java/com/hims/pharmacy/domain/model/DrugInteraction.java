package com.hims.pharmacy.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Drug-drug / drug-food / drug-disease interaction warning (M07 gap).
 * Acknowledged flag stores pharmacist override with timestamp.
 */
@Entity
@Table(name = "t_drug_interaction")
public class DrugInteraction extends PharmacyBase {

    @Id
    @Column(name = "interaction_id")
    private Long interactionId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "drug1_id", nullable = false)
    private Long drug1Id;

    @Column(name = "drug2_id", nullable = false)
    private Long drug2Id;

    @Column(name = "interaction_type", nullable = false, length = 50)
    private String interactionType;

    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "acknowledged", nullable = false)
    private Boolean acknowledged = false;

    @Column(name = "acknowledged_by")
    private Long acknowledgedBy;

    @Column(name = "acknowledged_at")
    private Timestamp acknowledgedAt;

    @Column(name = "recorded_at", nullable = false)
    private Timestamp recordedAt;

    public Long getInteractionId() { return interactionId; }
    public void setInteractionId(Long interactionId) { this.interactionId = interactionId; }
    @Override public Long getId() { return interactionId; }
    @Override public void setId(Long id) { this.interactionId = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getDrug1Id() { return drug1Id; }
    public void setDrug1Id(Long drug1Id) { this.drug1Id = drug1Id; }
    public Long getDrug2Id() { return drug2Id; }
    public void setDrug2Id(Long drug2Id) { this.drug2Id = drug2Id; }
    public String getInteractionType() { return interactionType; }
    public void setInteractionType(String interactionType) { this.interactionType = interactionType; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getAcknowledged() { return acknowledged; }
    public void setAcknowledged(Boolean acknowledged) { this.acknowledged = acknowledged; }
    public Long getAcknowledgedBy() { return acknowledgedBy; }
    public void setAcknowledgedBy(Long acknowledgedBy) { this.acknowledgedBy = acknowledgedBy; }
    public Timestamp getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(Timestamp acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
    public Timestamp getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Timestamp recordedAt) { this.recordedAt = recordedAt; }
}
