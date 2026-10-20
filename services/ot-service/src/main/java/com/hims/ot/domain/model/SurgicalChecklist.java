package com.hims.ot.domain.model;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "t_surgical_checklist")
public class SurgicalChecklist extends OtBase {

    @Id
    @Column(name = "checklist_id")
    private Long checklistId;

    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "phase", nullable = false, length = 50)
    private String phase; // SIGN_IN, TIME_OUT, SIGN_OUT

    @Column(name = "completed", nullable = false)
    private Boolean completed = false;

    @Column(name = "completed_by")
    private Long completedBy;

    @Column(name = "completed_at")
    private Timestamp completedAt;

    public Long getId() { return checklistId; }
    public void setId(Long id) { this.checklistId = id; }
    public Long getChecklistId() { return checklistId; }
    public void setChecklistId(Long checklistId) { this.checklistId = checklistId; }
    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }
    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }
    public Long getCompletedBy() { return completedBy; }
    public void setCompletedBy(Long completedBy) { this.completedBy = completedBy; }
    public Timestamp getCompletedAt() { return completedAt; }
    public void setCompletedAt(Timestamp completedAt) { this.completedAt = completedAt; }
}
