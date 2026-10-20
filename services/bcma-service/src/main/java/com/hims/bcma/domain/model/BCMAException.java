package com.hims.bcma.domain.model;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "t_bcma_exception")
public class BCMAException extends BcmaBase {
    @Id
    @Column(name = "exception_id")
    private Long exceptionId;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "drug_ien")
    private String drugIen;

    @Column(name = "exception_type", length = 20, nullable = false)
    private String exceptionType;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "override_nurse")
    private Long overrideNurse;

    @Column(name = "override_reason", length = 500)
    private String overrideReason;

    @Column(name = "exception_time", nullable = false)
    private Timestamp exceptionTime;

    @Column(name = "ward_id")
    private Long wardId;

    @Column(name = "scanned_by")
    private Long scannedBy;

    @Column(name = "overridden")
    private Boolean overridden = false;

    public enum ExceptionType { REFUSED, MISSED, DELAYED, MISMATCH, PATIENT_MISMATCH, OVERRIDE, WRONG_DRUG, WRONG_PATIENT, WRONG_DOSE, WRONG_TIME, WRONG_ROUTE, OTHER }

    @Override public Long getId() { return exceptionId; }
    @Override public void setId(Long id) { this.exceptionId = id; }

    public Long getExceptionId() { return exceptionId; }
    public void setExceptionId(Long exceptionId) { this.exceptionId = exceptionId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getDrugIen() { return drugIen; }
    public void setDrugIen(String drugIen) { this.drugIen = drugIen; }
    public String getExceptionType() { return exceptionType; }
    public void setExceptionType(String exceptionType) { this.exceptionType = exceptionType; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Long getOverrideNurse() { return overrideNurse; }
    public void setOverrideNurse(Long overrideNurse) { this.overrideNurse = overrideNurse; }
    public String getOverrideReason() { return overrideReason; }
    public void setOverrideReason(String overrideReason) { this.overrideReason = overrideReason; }
    public Timestamp getExceptionTime() { return exceptionTime; }
    public void setExceptionTime(Timestamp exceptionTime) { this.exceptionTime = exceptionTime; }
    public Long getWardId() { return wardId; }
    public void setWardId(Long wardId) { this.wardId = wardId; }
    public Long getScannedBy() { return scannedBy; }
    public void setScannedBy(Long scannedBy) { this.scannedBy = scannedBy; }
    public Boolean getOverridden() { return overridden; }
    public void setOverridden(Boolean overridden) { this.overridden = overridden; }
}
