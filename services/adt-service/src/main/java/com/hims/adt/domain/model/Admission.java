package com.hims.adt.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Clean target of T_ADMISSION (legacy CIMS_ADDADMISSIONINFORMATION).
 *
 * Lifecycle: admitted_at set on create (ipd_no = 'IPD-' + admissionId); discharge fills
 * discharge_id/discharge_type/is_expired/discharge_date (BR-DIS-001/002 slice — billing and
 * advised-discharge preconditions land with Billing). is_cancel=true marks a cancelled admission
 * (BR-IPD-005 auto-abscond job deferred). An admission is ACTIVE when
 * is_cancel=false AND discharge_date IS NULL.
 */
@Entity
@Table(name = "t_admission")
public class Admission extends AdtBase {

    @Id
    @Column(name = "admission_id")
    private Long admissionId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "encounter_id")
    private Long encounterId;

    @Column(name = "bed_id")
    private Long bedId;

    @Column(name = "ward_id")
    private Long wardId;

    @Column(name = "admission_type")
    private String admissionType;

    @Column(name = "ipd_no", nullable = false, length = 50)
    private String ipdNo;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "admitted_at", nullable = false)
    private Timestamp admittedAt;

    @Column(name = "is_cancel", nullable = false)
    private Boolean isCancel = false;

    // ---- discharge block (BR-DIS-002/003), null until discharge ----
    @Column(name = "discharge_id")
    private Long dischargeId;

    @Column(name = "discharge_type")
    private String dischargeType;

    @Column(name = "is_expired")
    private Boolean isExpired;

    @Column(name = "discharge_date")
    private Timestamp dischargeDate;

    @Column(name = "abscond_datetime")
    private Timestamp abscondDatetime;

    @Column(name = "death_date")
    private Timestamp deathDate;

    @Column(name = "handover_date")
    private Timestamp handoverDate;

    @Column(name = "is_interim", nullable = false)
    private Boolean isInterim = false;

    @Column(name = "tid", length = 50)
    private String tid;

    @Column(name = "death_cause")
    private String deathCause;

    @Column(name = "death_type", length = 50)
    private String deathType;

    public Long getAdmissionId() { return admissionId; }
    public void setAdmissionId(Long admissionId) { this.admissionId = admissionId; }
    public Long getId() { return admissionId; }
    public void setId(Long id) { this.admissionId = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getEncounterId() { return encounterId; }
    public void setEncounterId(Long encounterId) { this.encounterId = encounterId; }
    public Long getBedId() { return bedId; }
    public void setBedId(Long bedId) { this.bedId = bedId; }
    public Long getWardId() { return wardId; }
    public void setWardId(Long wardId) { this.wardId = wardId; }
    public String getAdmissionType() { return admissionType; }
    public void setAdmissionType(String admissionType) { this.admissionType = admissionType; }
    public String getIpdNo() { return ipdNo; }
    public void setIpdNo(String ipdNo) { this.ipdNo = ipdNo; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
    public Timestamp getAdmittedAt() { return admittedAt; }
    public void setAdmittedAt(Timestamp admittedAt) { this.admittedAt = admittedAt; }
    public Boolean getIsCancel() { return isCancel; }
    public void setIsCancel(Boolean isCancel) { this.isCancel = isCancel; }
    public Long getDischargeId() { return dischargeId; }
    public void setDischargeId(Long dischargeId) { this.dischargeId = dischargeId; }
    public String getDischargeType() { return dischargeType; }
    public void setDischargeType(String dischargeType) { this.dischargeType = dischargeType; }
    public Boolean getIsExpired() { return isExpired; }
    public void setIsExpired(Boolean isExpired) { this.isExpired = isExpired; }
    public Timestamp getDischargeDate() { return dischargeDate; }
    public void setDischargeDate(Timestamp dischargeDate) { this.dischargeDate = dischargeDate; }
    public Timestamp getAbscondDatetime() { return abscondDatetime; }
    public void setAbscondDatetime(Timestamp abscondDatetime) { this.abscondDatetime = abscondDatetime; }
    public Timestamp getDeathDate() { return deathDate; }
    public void setDeathDate(Timestamp deathDate) { this.deathDate = deathDate; }
    public Timestamp getHandoverDate() { return handoverDate; }
    public void setHandoverDate(Timestamp handoverDate) { this.handoverDate = handoverDate; }
    public Boolean getIsInterim() { return isInterim; }
    public void setIsInterim(Boolean isInterim) { this.isInterim = isInterim; }
    public String getTid() { return tid; }
    public void setTid(String tid) { this.tid = tid; }
    public String getDeathCause() { return deathCause; }
    public void setDeathCause(String deathCause) { this.deathCause = deathCause; }
    public String getDeathType() { return deathType; }
    public void setDeathType(String deathType) { this.deathType = deathType; }

    /** True when the patient left without settling (BR-IPD-005 auto-abscond or manual). */
    public boolean isAbsconded() {
        return abscondDatetime != null;
    }

    /** True when the admission ended in a recorded death (BR-DIS-002). */
    public boolean isDeath() {
        return Boolean.TRUE.equals(isExpired);
    }

    /** Active = not cancelled and not discharged (BR-IPD-001 duplicate guard). */
    public boolean isActive() {
        return !Boolean.TRUE.equals(isCancel) && dischargeDate == null;
    }
}
