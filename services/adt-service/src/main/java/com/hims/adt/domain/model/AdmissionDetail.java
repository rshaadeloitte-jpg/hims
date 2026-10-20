package com.hims.adt.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * 1:1 admission detail. Attending doctor defaults to the admitting doctor (BR-IPD-001).
 * MLC handling (legacy CIMS_ADDADMMLCDETAILS evidence): is_mlc=true requires an MLC number.
 */
@Entity
@Table(name = "t_admission_detail")
public class AdmissionDetail {

    @Id
    @Column(name = "detail_id")
    private Long detailId;

    @Column(name = "admission_id", nullable = false, unique = true)
    private Long admissionId;

    @Column(name = "attending_doctor_id")
    private Long attendingDoctorId;

    @Column(name = "is_mlc", nullable = false)
    private Boolean isMlc = false;

    @Column(name = "mlc_number")
    private String mlcNumber;

    @Column(name = "mlc_remarks")
    private String mlcRemarks;

    @Column(name = "remarks")
    private String remarks;

    public Long getDetailId() { return detailId; }
    public void setDetailId(Long detailId) { this.detailId = detailId; }
    public Long getId() { return detailId; }
    public void setId(Long id) { this.detailId = id; }
    public Long getAdmissionId() { return admissionId; }
    public void setAdmissionId(Long admissionId) { this.admissionId = admissionId; }
    public Long getAttendingDoctorId() { return attendingDoctorId; }
    public void setAttendingDoctorId(Long attendingDoctorId) { this.attendingDoctorId = attendingDoctorId; }
    public Boolean getIsMlc() { return isMlc; }
    public void setIsMlc(Boolean isMlc) { this.isMlc = isMlc; }
    public String getMlcNumber() { return mlcNumber; }
    public void setMlcNumber(String mlcNumber) { this.mlcNumber = mlcNumber; }
    public String getMlcRemarks() { return mlcRemarks; }
    public void setMlcRemarks(String mlcRemarks) { this.mlcRemarks = mlcRemarks; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    @Column(name = "added_by")
    private Long addedBy;

    @Column(name = "added_datetime")
    private Timestamp addedDatetime;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_datetime")
    private Timestamp updatedDatetime;

    public Long getAddedBy() { return addedBy; }
    public void setAddedBy(Long addedBy) { this.addedBy = addedBy; }
    public Timestamp getAddedDatetime() { return addedDatetime; }
    public void setAddedDatetime(Timestamp addedDatetime) { this.addedDatetime = addedDatetime; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public Timestamp getUpdatedDatetime() { return updatedDatetime; }
    public void setUpdatedDatetime(Timestamp updatedDatetime) { this.updatedDatetime = updatedDatetime; }
}
