package com.hims.patient.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

/**
 * Social Worker assessment (M01 gap: isSocialAssessment → "Social Assesment" module).
 * Ported from QuetionnaireServiceImpl / PatientPortalServiceImpl social assessment path.
 * JSONB payload so schema evolves without DDL (ponytail: one table, not 368 forms).
 */
@Entity
@Table(name = "patient_social_assessment", schema = "ihms_patient")
public class PatientSocialAssessment extends PatientBase {

    @Id
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "assessment_data", nullable = false)
    private Map<String, Object> assessmentData;

    @Column(name = "social_worker_id")
    private Long socialWorkerId;

    @Column(name = "remarks")
    private String remarks;

    @PrePersist
    void onCreate() {
        if (addedDatetime == null) addedDatetime = Timestamp.from(Instant.now());
    }

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Map<String, Object> getAssessmentData() { return assessmentData; }
    public void setAssessmentData(Map<String, Object> assessmentData) { this.assessmentData = assessmentData; }
    public Long getSocialWorkerId() { return socialWorkerId; }
    public void setSocialWorkerId(Long socialWorkerId) { this.socialWorkerId = socialWorkerId; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
