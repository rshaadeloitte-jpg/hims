package com.hims.adt.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * One department's clearance row for an admission (BR-DIS-001).
 * PENDING -> CLEARED only (never reverts; a re-clearance is idempotent).
 */
@Entity
@Table(name = "r_clearance_checklist")
public class ClearanceChecklistItem {

    public enum ClrStatus { PENDING, CLEARED }

    @Id
    @Column(name = "clearance_id")
    private Long clearanceId;

    @Column(name = "admission_id", nullable = false)
    private Long admissionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "department", nullable = false)
    private ClearanceDepartment department;

    @Enumerated(EnumType.STRING)
    @Column(name = "clr_status", nullable = false)
    private ClrStatus clrStatus = ClrStatus.PENDING;

    @Column(name = "cleared_by")
    private Long clearedBy;

    @Column(name = "cleared_at")
    private Timestamp clearedAt;

    @Column(name = "remarks")
    private String remarks;

    public boolean pending() { return clrStatus == ClrStatus.PENDING; }

    public Long getClearanceId() { return clearanceId; }
    public void setClearanceId(Long clearanceId) { this.clearanceId = clearanceId; }
    public Long getAdmissionId() { return admissionId; }
    public void setAdmissionId(Long admissionId) { this.admissionId = admissionId; }
    public ClearanceDepartment getDepartment() { return department; }
    public void setDepartment(ClearanceDepartment department) { this.department = department; }
    public ClrStatus getClrStatus() { return clrStatus; }
    public void setClrStatus(ClrStatus clrStatus) { this.clrStatus = clrStatus; }
    public Long getClearedBy() { return clearedBy; }
    public void setClearedBy(Long clearedBy) { this.clearedBy = clearedBy; }
    public Timestamp getClearedAt() { return clearedAt; }
    public void setClearedAt(Timestamp clearedAt) { this.clearedAt = clearedAt; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
