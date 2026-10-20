package com.hims.aasmaan.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_aasmaan_rsc_call_log")
public class AasmaanRscCallLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "case_id")
    private Long caseId;

    @Column(name = "call_ref", length = 40)
    private String callRef;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "logged_at")
    private LocalDateTime loggedAt;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }
    public String getCallRef() { return callRef; }
    public void setCallRef(String callRef) { this.callRef = callRef; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
}
