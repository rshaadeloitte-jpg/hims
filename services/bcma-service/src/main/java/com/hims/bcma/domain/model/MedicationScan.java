package com.hims.bcma.domain.model;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "t_bcma_medication_scan")
public class MedicationScan extends BcmaBase {
    @Id
    @Column(name = "scan_id")
    private Long scanId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "drug_ien")
    private String drugIen;

    @Column(name = "drug_name")
    private String drugName;

    @Column(name = "med_order_no")
    private String medOrderNo;

    @Column(name = "dosage")
    private String dosage;

    @Column(name = "admin_dt")
    private Timestamp adminDt;

    @Column(name = "schedule")
    private String schedule;

    @Column(name = "verified", nullable = false)
    private Boolean verified = false;

    @Column(name = "verification_status", length = 50)
    private String verificationStatus; // MATCH / MISMATCH / PENDING

    @Column(name = "scan_time", nullable = false)
    private Timestamp scanTime;

    @Column(name = "scanned_by")
    private Long scannedBy;

    @Column(name = "ward_id")
    private Long wardId;

    @Override public Long getId() { return scanId; }
    @Override public void setId(Long id) { this.scanId = id; }

    public Long getScanId() { return scanId; }
    public void setScanId(Long scanId) { this.scanId = scanId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getDrugIen() { return drugIen; }
    public void setDrugIen(String drugIen) { this.drugIen = drugIen; }
    public String getDrugName() { return drugName; }
    public void setDrugName(String drugName) { this.drugName = drugName; }
    public String getMedOrderNo() { return medOrderNo; }
    public void setMedOrderNo(String medOrderNo) { this.medOrderNo = medOrderNo; }
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public Timestamp getAdminDt() { return adminDt; }
    public void setAdminDt(Timestamp adminDt) { this.adminDt = adminDt; }
    public String getSchedule() { return schedule; }
    public void setSchedule(String schedule) { this.schedule = schedule; }
    public Boolean getVerified() { return verified; }
    public void setVerified(Boolean verified) { this.verified = verified; }
    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }
    public Timestamp getScanTime() { return scanTime; }
    public void setScanTime(Timestamp scanTime) { this.scanTime = scanTime; }
    public Long getScannedBy() { return scannedBy; }
    public void setScannedBy(Long scannedBy) { this.scannedBy = scannedBy; }
    public Long getWardId() { return wardId; }
    public void setWardId(Long wardId) { this.wardId = wardId; }

    public boolean isPatientDrugMatch() {
        return "MATCH".equalsIgnoreCase(verificationStatus) || Boolean.TRUE.equals(verified);
    }
}
