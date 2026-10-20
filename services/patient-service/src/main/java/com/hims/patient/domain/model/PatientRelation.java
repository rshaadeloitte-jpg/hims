package com.hims.patient.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * Patient-to-patient relationship (M01 gap: mother-child linking).
 * Supports MOTHER, CHILD, SPOUSE, FATHER, GUARDIAN relation types.
 */
@Entity
@Table(name = "patient_relation", schema = "ihms_patient")
public class PatientRelation extends PatientBase {

    @Id
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "related_patient_id", nullable = false)
    private Long relatedPatientId;

    @Column(name = "relation_type", nullable = false)
    private String relationType;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;

    @PrePersist
    void onCreate() {
        if (addedDatetime == null) addedDatetime = Timestamp.from(Instant.now());
    }

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getRelatedPatientId() { return relatedPatientId; }
    public void setRelatedPatientId(Long relatedPatientId) { this.relatedPatientId = relatedPatientId; }
    public String getRelationType() { return relationType; }
    public void setRelationType(String relationType) { this.relationType = relationType; }
    public Boolean getIsPrimary() { return isPrimary; }
    public void setIsPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; }
}
