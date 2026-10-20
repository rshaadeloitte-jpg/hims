package com.hims.ot.domain.model;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "t_checklist_completion")
public class ChecklistCompletion extends OtBase {

    @Id
    @Column(name = "completion_id")
    private Long completionId;

    @Column(name = "checklist_id", nullable = false)
    private Long checklistId;

    @Column(name = "overall_status", nullable = false, length = 20)
    private String overallStatus = "PENDING";

    @Column(name = "completed_by")
    private Long completedBy;

    @Column(name = "completed_at")
    private Timestamp completedAt;

    public Long getId() { return completionId; }
    public void setId(Long id) { this.completionId = id; }
    public Long getCompletionId() { return completionId; }
    public void setCompletionId(Long completionId) { this.completionId = completionId; }
    public Long getChecklistId() { return checklistId; }
    public void setChecklistId(Long checklistId) { this.checklistId = checklistId; }
    public String getOverallStatus() { return overallStatus; }
    public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }
    public Long getCompletedBy() { return completedBy; }
    public void setCompletedBy(Long completedBy) { this.completedBy = completedBy; }
    public Timestamp getCompletedAt() { return completedAt; }
    public void setCompletedAt(Timestamp completedAt) { this.completedAt = completedAt; }
}
