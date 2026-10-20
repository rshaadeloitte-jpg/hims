package com.hims.nursing.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Clean target of T_NURSETASKDETAILS — a planned nursing task for an admission.
 *
 * Task status lifecycle (SMALLINT): 1=PENDING 2=COMPLETED 3=CANCELLED.
 * Transitions: create(->1 PENDING); complete 1->2 ONLY (illegal transition from any
 * other state -> 400), emitting the PROPOSED event NurseTaskCompleted via the outbox.
 * cancel 1->3.
 *
 * BOUNDARY NOTE (phase 1): {@code admission_id} is accepted as an OPAQUE ID.
 * Cross-service validation against adt-service is deferred to the inter-service phase.
 */
@Entity
@Table(name = "t_nursetaskdetails")
public class NurseTask extends NursingBase {

    public static final short STATUS_PENDING = 1;
    public static final short STATUS_COMPLETED = 2;
    public static final short STATUS_CANCELLED = 3;

    @Id
    private Long id;

    /** Opaque id in phase 1 (adt-service owns admissions). */
    @Column(name = "admission_id", nullable = false)
    private Long admissionId;

    @Column(name = "task_desc", nullable = false, length = 500)
    private String taskDesc;

    /** Frequency semantics per 06-business-rules-catalog §A (e.g. OD/BD/TID/Q4H). */
    @Column(name = "frequency", length = 32)
    private String frequency;

    @Column(name = "due_at")
    private Timestamp dueAt;

    // NOTE: named taskStatus in Java because NursingBase.status is the row-active
    // BOOLEAN audit flag; this maps to the DB column "status" (1/2/3 lifecycle).
    @Column(name = "task_status", nullable = false)
    private Short taskStatus = STATUS_PENDING;

    @Column(name = "completed_at")
    private Timestamp completedAt;

    @Column(name = "completed_by")
    private Long completedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAdmissionId() { return admissionId; }
    public void setAdmissionId(Long admissionId) { this.admissionId = admissionId; }
    public String getTaskDesc() { return taskDesc; }
    public void setTaskDesc(String taskDesc) { this.taskDesc = taskDesc; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public Timestamp getDueAt() { return dueAt; }
    public void setDueAt(Timestamp dueAt) { this.dueAt = dueAt; }
    public Short getTaskStatus() { return taskStatus; }
    public void setTaskStatus(Short taskStatus) { this.taskStatus = taskStatus; }
    public Timestamp getCompletedAt() { return completedAt; }
    public void setCompletedAt(Timestamp completedAt) { this.completedAt = completedAt; }
    public Long getCompletedBy() { return completedBy; }
    public void setCompletedBy(Long completedBy) { this.completedBy = completedBy; }
}
