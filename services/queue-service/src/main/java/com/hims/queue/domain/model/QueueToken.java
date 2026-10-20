package com.hims.queue.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;

/** Clean target of PatientQueue/RegistrationQueue (M27). One row = one token. */
@Entity
@Table(name = "queue_token")
public class QueueToken extends QueueBase {

    public static final short PRIORITY_NORMAL = 1;
    public static final short PRIORITY_EMERGENCY = 2;

    public static final short WAITING = 1;
    public static final short CALLED = 2;
    public static final short COMPLETED = 3;
    public static final short NO_SHOW = 4;

    @Id
    private Long id;

    @Column(name = "queue_date", nullable = false)
    private LocalDate queueDate;

    @Column(name = "token_no", nullable = false)
    private Long tokenNo;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "visit_ref")
    private String visitRef;

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @Column(name = "counter_id")
    private Long counterId;

    @Column(name = "priority", nullable = false)
    private Short priority = PRIORITY_NORMAL;

    @Column(name = "queue_status", nullable = false)
    private Short queueStatus = WAITING;

    @Column(name = "called_at")
    private Timestamp calledAt;

    @Column(name = "completed_at")
    private Timestamp completedAt;

    @PrePersist
    void onCreate() {
        if (addedDatetime == null) addedDatetime = Timestamp.from(Instant.now());
    }

    @PreUpdate
    void onUpdate() {
        updatedDatetime = Timestamp.from(Instant.now());
    }

    @JsonIgnore
    public boolean isTerminal() {
        return queueStatus == COMPLETED || queueStatus == NO_SHOW;
    }

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public LocalDate getQueueDate() { return queueDate; }
    public void setQueueDate(LocalDate queueDate) { this.queueDate = queueDate; }
    public Long getTokenNo() { return tokenNo; }
    public void setTokenNo(Long tokenNo) { this.tokenNo = tokenNo; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getVisitRef() { return visitRef; }
    public void setVisitRef(String visitRef) { this.visitRef = visitRef; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public Long getCounterId() { return counterId; }
    public void setCounterId(Long counterId) { this.counterId = counterId; }
    public Short getPriority() { return priority; }
    public void setPriority(Short priority) { this.priority = priority; }
    public Short getQueueStatus() { return queueStatus; }
    public void setQueueStatus(Short queueStatus) { this.queueStatus = queueStatus; }
    public Timestamp getCalledAt() { return calledAt; }
    public void setCalledAt(Timestamp calledAt) { this.calledAt = calledAt; }
    public Timestamp getCompletedAt() { return completedAt; }
    public void setCompletedAt(Timestamp completedAt) { this.completedAt = completedAt; }
}
