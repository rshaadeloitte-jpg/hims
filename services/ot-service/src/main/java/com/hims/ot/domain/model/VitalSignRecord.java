package com.hims.ot.domain.model;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "t_vital_sign_record")
public class VitalSignRecord extends OtBase {

    @Id
    @Column(name = "vital_id")
    private Long vitalId;

    @Column(name = "record_id", nullable = false)
    private Long recordId;

    @Column(name = "recorded_at", nullable = false)
    private Timestamp recordedAt;

    @Column(name = "heart_rate")
    private Integer heartRate;

    @Column(name = "bp_systolic")
    private Integer bpSystolic;

    @Column(name = "bp_diastolic")
    private Integer bpDiastolic;

    @Column(name = "spo2")
    private Integer spo2;

    @Column(name = "respiratory_rate")
    private Integer respiratoryRate;

    @Column(name = "temperature", precision = 5, scale = 2)
    private java.math.BigDecimal temperature;

    public Long getId() { return vitalId; }
    public void setId(Long id) { this.vitalId = id; }
    public Long getVitalId() { return vitalId; }
    public void setVitalId(Long vitalId) { this.vitalId = vitalId; }
    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public Timestamp getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Timestamp recordedAt) { this.recordedAt = recordedAt; }
    public Integer getHeartRate() { return heartRate; }
    public void setHeartRate(Integer heartRate) { this.heartRate = heartRate; }
    public Integer getBpSystolic() { return bpSystolic; }
    public void setBpSystolic(Integer bpSystolic) { this.bpSystolic = bpSystolic; }
    public Integer getBpDiastolic() { return bpDiastolic; }
    public void setBpDiastolic(Integer bpDiastolic) { this.bpDiastolic = bpDiastolic; }
    public Integer getSpo2() { return spo2; }
    public void setSpo2(Integer spo2) { this.spo2 = spo2; }
    public Integer getRespiratoryRate() { return respiratoryRate; }
    public void setRespiratoryRate(Integer respiratoryRate) { this.respiratoryRate = respiratoryRate; }
    public java.math.BigDecimal getTemperature() { return temperature; }
    public void setTemperature(java.math.BigDecimal temperature) { this.temperature = temperature; }
}
