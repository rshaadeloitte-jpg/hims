package com.hims.govt.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Aasmaan case header (register WP-T4).
 * FLAG: table name UNVERIFIED against the legacy Oracle schema — catalog name
 * used as-is (flagged in the migration comments and WP-T4-govt.md too).
 *
 * Forward-only status machine (SMALLINT): 1=OPEN -> 2=IN_PROGRESS -> 3=CLOSED.
 * Adjacent steps only; closing stamps closed_on. CaseUpdated event DEFERRED —
 * nothing emitted.
 */
@Entity
@Table(name = "t_aasmaan_case")
public class AasmaanCase extends GovtBase {

    public static final short STATUS_OPEN = 1;
    public static final short STATUS_IN_PROGRESS = 2;
    public static final short STATUS_CLOSED = 3;

    @Id
    @Column(name = "case_id")
    private Long caseId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "case_type", nullable = false, length = 40)
    private String caseType;

    @Column(name = "status", nullable = false)
    private Short status = STATUS_OPEN;

    @Column(name = "opened_on")
    private LocalDate openedOn;

    @Column(name = "closed_on")
    private LocalDate closedOn;

    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }
    @Override public Long getId() { return caseId; }
    @Override public void setId(Long id) { this.caseId = id; }
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

    public boolean isOpen() { return status == STATUS_OPEN; }
    public boolean isInProgress() { return status == STATUS_IN_PROGRESS; }
    public boolean isClosed() { return status == STATUS_CLOSED; }

    /** Forward-only adjacent step: OPEN->IN_PROGRESS or IN_PROGRESS->CLOSED. */
    public boolean canTransitionTo(short target) {
        return target == status + 1 && target <= STATUS_CLOSED;
    }
}
