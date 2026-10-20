package com.hims.nursing.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Clean target of T_PATIENTVITALS — one recorded vital observation for a patient.
 *
 * BOUNDARY NOTE (phase 1): {@code patient_id} and {@code admission_id} are accepted as
 * OPAQUE IDS. Cross-service validation against adt-service (does the admission exist /
 * belong to the patient?) is deferred to the inter-service phase — see GAP-REGISTER-2 WP-6.
 */
@Entity
@Table(name = "t_patientvitals")
public class PatientVitals extends NursingBase {

    @Id
    private Long id;

    /** Opaque id in phase 1 (adt-service owns admissions). */
    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    /** Opaque id in phase 1 (adt-service owns admissions); null for non-admitted patients. */
    @Column(name = "admission_id")
    private Long admissionId;

    @Column(name = "vitals_type", nullable = false, length = 100)
    private String vitalsType;

    @Column(name = "value", precision = 10, scale = 2)
    private BigDecimal value;

    @Column(name = "unit", length = 32)
    private String unit;

    @Column(name = "recorded_at", nullable = false)
    private Timestamp recordedAt;

    @Column(name = "recorded_by")
    private Long recordedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getAdmissionId() { return admissionId; }
    public void setAdmissionId(Long admissionId) { this.admissionId = admissionId; }
    public String getVitalsType() { return vitalsType; }
    public void setVitalsType(String vitalsType) { this.vitalsType = vitalsType; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public Timestamp getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Timestamp recordedAt) { this.recordedAt = recordedAt; }
    public Long getRecordedBy() { return recordedBy; }
    public void setRecordedBy(Long recordedBy) { this.recordedBy = recordedBy; }
}
