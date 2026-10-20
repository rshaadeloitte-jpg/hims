package com.hims.aasmaan.domain.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "t_aasmaan_case")
public class AasmaanCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "case_id")
    private Long caseId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "case_type", nullable = false, length = 40)
    private String caseType;

    @Column(name = "status", nullable = false)
    private Short status = 1; // 1=OPEN, 2=IN_PROGRESS, 3=CLOSED

    @Column(name = "opened_on")
    private LocalDate openedOn;

    @Column(name = "closed_on")
    private LocalDate closedOn;

    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getCaseType() { return caseType; }
    public void setCaseType(String caseType) { this.caseType = caseType; }
    public Short getStatus() { return status; }
    public void setStatus(Short status) { this.status = status; }
    public LocalDate getOpenedOn() { return openedOn; }
    public void setOpenedOn(LocalDate openedOn) { this.openedOn = openedOn; }
    public LocalDate getClosedOn() { return closedOn; }
    public void setClosedOn(LocalDate closedOn) { this.closedOn = closedOn; }

    public boolean isOpen() { return status != null && status == 1; }
    public boolean isInProgress() { return status != null && status == 2; }
    public boolean isClosed() { return status != null && status == 3; }

    public boolean canTransitionTo(short target) {
        return target == status + 1 && target <= 3;
    }
}
