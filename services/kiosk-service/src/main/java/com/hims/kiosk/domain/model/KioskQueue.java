package com.hims.kiosk.domain.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.sql.Timestamp;

@Entity
@Table(name = "kiosk_queue", schema = "ihms_kiosk")
public class KioskQueue extends KioskBase {
    @Id
    private Long id;
    @Column(name = "patient_id", nullable = false)
    private Long patientId;
    @Column(name = "department_id")
    private Long departmentId;
    @Column(name = "queue_date", nullable = false)
    private LocalDate queueDate;
    @Column(name = "queue_status", nullable = false)
    private Short queueStatus = 1;
    @Column(name = "priority", nullable = false)
    private Short priority = 1;
    @Column(name = "token_no", nullable = false)
    private Long tokenNo;
    @Column(name = "called_at")
    private Timestamp calledAt;
    @Column(name = "completed_at")
    private Timestamp completedAt;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public LocalDate getQueueDate() { return queueDate; }
    public void setQueueDate(LocalDate queueDate) { this.queueDate = queueDate; }
    public Short getQueueStatus() { return queueStatus; }
    public void setQueueStatus(Short queueStatus) { this.queueStatus = queueStatus; }
    public Short getPriority() { return priority; }
    public void setPriority(Short priority) { this.priority = priority; }
    public Long getTokenNo() { return tokenNo; }
    public void setTokenNo(Long tokenNo) { this.tokenNo = tokenNo; }
    public Timestamp getCalledAt() { return calledAt; }
    public void setCalledAt(Timestamp calledAt) { this.calledAt = calledAt; }
    public Timestamp getCompletedAt() { return completedAt; }
    public void setCompletedAt(Timestamp completedAt) { this.completedAt = completedAt; }
}
