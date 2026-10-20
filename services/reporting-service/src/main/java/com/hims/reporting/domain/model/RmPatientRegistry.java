package com.hims.reporting.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Read model: one row per registered patient, fed by PatientRegistered via the
 * internal event ingress. Upsert by patient_id — the SOURCE patient id IS the
 * primary key, so re-ingesting the same event updates the row in place and can
 * never duplicate it (strict idempotency, pinned by tests).
 */
@Entity
@Table(name = "rm_patient_registry")
public class RmPatientRegistry extends ReportingBase {

    @Id
    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "mrn", length = 30)
    private String mrn;

    @Column(name = "name", length = 120)
    private String name;

    @Column(name = "registered_at")
    private Timestamp registeredAt;

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getMrn() { return mrn; }
    public void setMrn(String mrn) { this.mrn = mrn; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Timestamp getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(Timestamp registeredAt) { this.registeredAt = registeredAt; }
}
