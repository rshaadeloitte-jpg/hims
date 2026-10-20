package com.hims.ordering.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.util.ArrayList;
import java.util.List;

/**
 * Clean target of the legacy prescription header (Catalog #4, register WP-10).
 *
 * Status machine (SMALLINT per 06 §A9):
 *   1=ACTIVE -> cancel -> 3=CANCELLED (guard: ACTIVE only)
 *   2=COMPLETED is terminal (completion flow lands with a later slice — not exposed here).
 */
@Entity
@Table(name = "prescription")
public class Prescription extends PrescriptionBase {

    public static final short STATUS_ACTIVE = 1;
    public static final short STATUS_COMPLETED = 2;
    public static final short STATUS_CANCELLED = 3;

    @Id
    @Column(name = "prescription_id")
    private Long prescriptionId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "encounter_id")
    private Long encounterId;

    @Column(name = "admission_id")
    private Long admissionId;

    @Column(name = "prescriber_id", nullable = false)
    private Long prescriberId;

    @Column(name = "ward_id")
    private Long wardId;

    @Column(name = "status_code", nullable = false)
    private Short statusCode = STATUS_ACTIVE;

    /** Not persisted: lines are loaded via repository in the service layer. */
    @Transient
    private List<PrescriptionItem> details = new ArrayList<>();

    public Long getPrescriptionId() { return prescriptionId; }
    public void setPrescriptionId(Long prescriptionId) { this.prescriptionId = prescriptionId; }
    public Long getId() { return prescriptionId; }
    public void setId(Long id) { this.prescriptionId = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getEncounterId() { return encounterId; }
    public void setEncounterId(Long encounterId) { this.encounterId = encounterId; }
    public Long getAdmissionId() { return admissionId; }
    public void setAdmissionId(Long admissionId) { this.admissionId = admissionId; }
    public Long getPrescriberId() { return prescriberId; }
    public void setPrescriberId(Long prescriberId) { this.prescriberId = prescriberId; }
    public Long getWardId() { return wardId; }
    public void setWardId(Long wardId) { this.wardId = wardId; }
    public Short getStatusCode() { return statusCode; }
    public void setStatusCode(Short statusCode) { this.statusCode = statusCode; }
    public List<PrescriptionItem> getDetails() { return details; }
    public void setDetails(List<PrescriptionItem> details) { this.details = details; }

    public boolean isActive() { return statusCode == STATUS_ACTIVE; }
    public boolean isCancelled() { return statusCode == STATUS_CANCELLED; }
}
