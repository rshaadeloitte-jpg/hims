package com.hims.ot.domain.model;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "t_anesthesia_record")
public class AnesthesiaRecord extends OtBase {

    @Id
    @Column(name = "record_id")
    private Long recordId;

    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "asa_grade", length = 10)
    private String asaGrade;

    @Column(name = "airway_assessment", length = 200)
    private String airwayAssessment;

    @Column(name = "comorbidities", length = 500)
    private String comorbidities;

    @Column(name = "pre_anesthesia_notes", length = 500)
    private String preAnesthesiaNotes;

    @Column(name = "intra_op_notes", length = 500)
    private String intraOpNotes;

    @Column(name = "post_anesthesia_notes", length = 500)
    private String postAnesthesiaNotes;

    @Column(name = "pacu_stay_minutes")
    private Integer pacuStayMinutes;

    @Column(name = "recovery_score")
    private Integer recoveryScore;

    @Column(name = "complications", length = 500)
    private String complications;

    public Long getId() { return recordId; }
    public void setId(Long id) { this.recordId = id; }
    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getAsaGrade() { return asaGrade; }
    public void setAsaGrade(String asaGrade) { this.asaGrade = asaGrade; }
    public String getAirwayAssessment() { return airwayAssessment; }
    public void setAirwayAssessment(String airwayAssessment) { this.airwayAssessment = airwayAssessment; }
    public String getComorbidities() { return comorbidities; }
    public void setComorbidities(String comorbidities) { this.comorbidities = comorbidities; }
    public String getPreAnesthesiaNotes() { return preAnesthesiaNotes; }
    public void setPreAnesthesiaNotes(String preAnesthesiaNotes) { this.preAnesthesiaNotes = preAnesthesiaNotes; }
    public String getIntraOpNotes() { return intraOpNotes; }
    public void setIntraOpNotes(String intraOpNotes) { this.intraOpNotes = intraOpNotes; }
    public String getPostAnesthesiaNotes() { return postAnesthesiaNotes; }
    public void setPostAnesthesiaNotes(String postAnesthesiaNotes) { this.postAnesthesiaNotes = postAnesthesiaNotes; }
    public Integer getPacuStayMinutes() { return pacuStayMinutes; }
    public void setPacuStayMinutes(Integer pacuStayMinutes) { this.pacuStayMinutes = pacuStayMinutes; }
    public Integer getRecoveryScore() { return recoveryScore; }
    public void setRecoveryScore(Integer recoveryScore) { this.recoveryScore = recoveryScore; }
    public String getComplications() { return complications; }
    public void setComplications(String complications) { this.complications = complications; }
}
