package com.hims.bcma.domain.model;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "t_bcma_ivp_event")
public class IVPEvent extends BcmaBase {
    @Id
    @Column(name = "ivp_id")
    private Long ivpId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "drug_name", nullable = false, length = 255)
    private String drugName;

    @Column(name = "dose", length = 100)
    private String dose;

    @Column(name = "rate", length = 100)
    private String rate;

    @Column(name = "start_time", nullable = false)
    private Timestamp startTime;

    @Column(name = "end_time")
    private Timestamp endTime;

    @Column(name = "nurse_id", nullable = false)
    private Long nurseId;

    @Column(name = "ward_id")
    private Long wardId;

    @Column(name = "event_status", length = 20)
    private String eventStatus; // ACTIVE / COMPLETED / CANCELLED

    @Override public Long getId() { return ivpId; }
    @Override public void setId(Long id) { this.ivpId = id; }

    public Long getIvpId() { return ivpId; }
    public void setIvpId(Long ivpId) { this.ivpId = ivpId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getDrugName() { return drugName; }
    public void setDrugName(String drugName) { this.drugName = drugName; }
    public String getDose() { return dose; }
    public void setDose(String dose) { this.dose = dose; }
    public String getRate() { return rate; }
    public void setRate(String rate) { this.rate = rate; }
    public Timestamp getStartTime() { return startTime; }
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }
    public Timestamp getEndTime() { return endTime; }
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }
    public Long getNurseId() { return nurseId; }
    public void setNurseId(Long nurseId) { this.nurseId = nurseId; }
    public Long getWardId() { return wardId; }
    public void setWardId(Long wardId) { this.wardId = wardId; }
    public String getEventStatus() { return eventStatus; }
    public void setEventStatus(String eventStatus) { this.eventStatus = eventStatus; }
}
