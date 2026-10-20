package com.hims.aasmaan.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_aasmaan_checklist")
public class AasmaanChecklist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "checklist_id")
    private Long checklistId;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "category", length = 40)
    private String category;

    @Column(name = "completed", nullable = false)
    private Boolean completed = false;

    @Column(name = "updated_on")
    private LocalDateTime updatedOn;

    public Long getChecklistId() { return checklistId; }
    public void setChecklistId(Long checklistId) { this.checklistId = checklistId; }
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }
    public LocalDateTime getUpdatedOn() { return updatedOn; }
    public void setUpdatedOn(LocalDateTime updatedOn) { this.updatedOn = updatedOn; }
}
