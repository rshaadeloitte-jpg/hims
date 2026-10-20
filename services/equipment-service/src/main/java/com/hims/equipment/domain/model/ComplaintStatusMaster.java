package com.hims.equipment.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Complaint lifecycle dictionary (V1 complaint_status_master) — clean target
 * of legacy M_MAINTENANCECOMPLAINTSTATUS. The seeded ids ARE the codes used by
 * the {@link ComplaintStatus} constants (1=OPEN 2=IN_PROGRESS 3=RESOLVED
 * 4=CLOSED); the resolved/closed/reopened flags consolidate legacy
 * M_EquipmentStatus.ISRESOLVED/isClosed/isReOpened. {@code code} is UNIQUE.
 * Server-owned id from ihms_equipment.complaint_status_master_seq — never
 * hard-delete a seeded row (complaint_status values reference it); deactivate
 * instead.
 */
@Entity
@Table(name = "complaint_status_master")
public class ComplaintStatusMaster extends EquipmentBase {

    @Id
    private Long id;

    /** Status code (legacy CODE, NOT NULL UNIQUE) — e.g. OPEN, IN_PROGRESS. */
    @Column(name = "code", nullable = false)
    private String code;

    /** Status description (legacy DESCRIPTION, NOT NULL). */
    @Column(name = "description", nullable = false)
    private String description;

    /** Legacy ISRESOLVED: does this status mean the complaint is fixed? */
    @Column(name = "is_resolved", nullable = false)
    private Boolean isResolved;

    /** Legacy isClosed: does this status terminate the complaint? */
    @Column(name = "is_closed", nullable = false)
    private Boolean isClosed;

    /** Legacy isReOpened: can a complaint in this status be reopened? */
    @Column(name = "is_reopened", nullable = false)
    private Boolean isReopened;

    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getIsResolved() { return isResolved; }
    public void setIsResolved(Boolean resolved) { isResolved = resolved; }
    public Boolean getIsClosed() { return isClosed; }
    public void setIsClosed(Boolean closed) { isClosed = closed; }
    public Boolean getIsReopened() { return isReopened; }
    public void setIsReopened(Boolean reopened) { isReopened = reopened; }
}
