package com.hims.patient.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * Medico-Legal Case record linked to a patient (M01 gap: MLC capture).
 * Ported from old IHMS PatDetails.mlcCase / mlcType fields.
 */
@Entity
@Table(name = "patient_mlc", schema = "ihms_patient")
public class PatientMlc extends PatientBase {

    @Id
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "mlc_case", nullable = false)
    private String mlcCase;

    @Column(name = "mlc_type")
    private String mlcType;

    @Column(name = "mlc_number")
    private String mlcNumber;

    @Column(name = "police_station")
    private String policeStation;

    @Column(name = "fir_number")
    private String firNumber;

    @Column(name = "reported_date_time")
    private Timestamp reportedDateTime;

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
    public String getMlcCase() { return mlcCase; }
    public void setMlcCase(String mlcCase) { this.mlcCase = mlcCase; }
    public String getMlcType() { return mlcType; }
    public void setMlcType(String mlcType) { this.mlcType = mlcType; }
    public String getMlcNumber() { return mlcNumber; }
    public void setMlcNumber(String mlcNumber) { this.mlcNumber = mlcNumber; }
    public String getPoliceStation() { return policeStation; }
    public void setPoliceStation(String policeStation) { this.policeStation = policeStation; }
    public String getFirNumber() { return firNumber; }
    public void setFirNumber(String firNumber) { this.firNumber = firNumber; }
    public Timestamp getReportedDateTime() { return reportedDateTime; }
    public void setReportedDateTime(Timestamp reportedDateTime) { this.reportedDateTime = reportedDateTime; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
