package com.hims.pharmacy.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Patient drug allergy record (legacy t_patientallergy parity).
 * Severity gates pharmacist override on dispense (BR-PHAR-004).
 */
@Entity
@Table(name = "t_drug_allergy")
public class DrugAllergy extends PharmacyBase {

    @Id
    @Column(name = "allergy_id")
    private Long allergyId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "allergen", nullable = false, length = 200)
    private String allergen;

    @Column(name = "allergy_type", length = 50)
    private String allergyType;

    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @Column(name = "reaction_description", length = 500)
    private String reactionDescription;

    @Column(name = "recorded_by")
    private Long recordedBy;

    @Column(name = "recorded_at", nullable = false)
    private Timestamp recordedAt;

    public Long getAllergyId() { return allergyId; }
    public void setAllergyId(Long allergyId) { this.allergyId = allergyId; }
    @Override public Long getId() { return allergyId; }
    @Override public void setId(Long id) { this.allergyId = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getAllergen() { return allergen; }
    public void setAllergen(String allergen) { this.allergen = allergen; }
    public String getAllergyType() { return allergyType; }
    public void setAllergyType(String allergyType) { this.allergyType = allergyType; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getReactionDescription() { return reactionDescription; }
    public void setReactionDescription(String reactionDescription) { this.reactionDescription = reactionDescription; }
    public Long getRecordedBy() { return recordedBy; }
    public void setRecordedBy(Long recordedBy) { this.recordedBy = recordedBy; }
    public Timestamp getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Timestamp recordedAt) { this.recordedAt = recordedAt; }
}
