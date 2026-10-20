package com.hims.pharmacy.domain.model;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * BCMA five-rights verification log (BR-PHAR-BCMA).
 * Captures right-patient / right-drug / right-dose / right-route / right-time
 * scan event with override audit trail.
 */
@Entity
@Table(name = "t_bcma_verification")
public class BcmaVerification extends PharmacyBase {

    @Id
    @Column(name = "bcma_id")
    private Long bcmaId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "sale_id", nullable = false)
    private Long saleId;

    @Column(name = "detail_id", nullable = false)
    private Long detailId;

    @Column(name = "encounter_id")
    private Long encounterId;

    @Column(name = "scan_time", nullable = false)
    private Timestamp scanTime;

    @Column(name = "right_patient", nullable = false)
    private Boolean rightPatient = false;

    @Column(name = "right_drug", nullable = false)
    private Boolean rightDrug = false;

    @Column(name = "right_dose", nullable = false)
    private Boolean rightDose = false;

    @Column(name = "right_route", nullable = false)
    private Boolean rightRoute = false;

    @Column(name = "right_time", nullable = false)
    private Boolean rightTime = false;

    @Column(name = "all_rights_passed", nullable = false)
    private Boolean allRightsPassed = false;

    @Column(name = "overridden", nullable = false)
    private Boolean overridden = false;

    @Column(name = "override_reason", length = 500)
    private String overrideReason;

    @Column(name = "override_by")
    private Long overrideBy;

    @Column(name = "administered", nullable = false)
    private Boolean administered = false;

    @Column(name = "administered_at")
    private Timestamp administeredAt;

    @Column(name = "administered_by")
    private Long administeredBy;

    public Long getBcmaId() { return bcmaId; }
    public void setBcmaId(Long bcmaId) { this.bcmaId = bcmaId; }
    @Override public Long getId() { return bcmaId; }
    @Override public void setId(Long id) { this.bcmaId = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getSaleId() { return saleId; }
    public void setSaleId(Long saleId) { this.saleId = saleId; }
    public Long getDetailId() { return detailId; }
    public void setDetailId(Long detailId) { this.detailId = detailId; }
    public Long getEncounterId() { return encounterId; }
    public void setEncounterId(Long encounterId) { this.encounterId = encounterId; }
    public Timestamp getScanTime() { return scanTime; }
    public void setScanTime(Timestamp scanTime) { this.scanTime = scanTime; }
    public Boolean getRightPatient() { return rightPatient; }
    public void setRightPatient(Boolean rightPatient) { this.rightPatient = rightPatient; }
    public Boolean getRightDrug() { return rightDrug; }
    public void setRightDrug(Boolean rightDrug) { this.rightDrug = rightDrug; }
    public Boolean getRightDose() { return rightDose; }
    public void setRightDose(Boolean rightDose) { this.rightDose = rightDose; }
    public Boolean getRightRoute() { return rightRoute; }
    public void setRightRoute(Boolean rightRoute) { this.rightRoute = rightRoute; }
    public Boolean getRightTime() { return rightTime; }
    public void setRightTime(Boolean rightTime) { this.rightTime = rightTime; }
    public Boolean getAllRightsPassed() { return allRightsPassed; }
    public void setAllRightsPassed(Boolean allRightsPassed) { this.allRightsPassed = allRightsPassed; }
    public Boolean getOverridden() { return overridden; }
    public void setOverridden(Boolean overridden) { this.overridden = overridden; }
    public String getOverrideReason() { return overrideReason; }
    public void setOverrideReason(String overrideReason) { this.overrideReason = overrideReason; }
    public Long getOverrideBy() { return overrideBy; }
    public void setOverrideBy(Long overrideBy) { this.overrideBy = overrideBy; }
    public Boolean getAdministered() { return administered; }
    public void setAdministered(Boolean administered) { this.administered = administered; }
    public Timestamp getAdministeredAt() { return administeredAt; }
    public void setAdministeredAt(Timestamp administeredAt) { this.administeredAt = administeredAt; }
    public Long getAdministeredBy() { return administeredBy; }
    public void setAdministeredBy(Long administeredBy) { this.administeredBy = administeredBy; }

    public boolean isAllRightsPassed() {
        return Boolean.TRUE.equals(rightPatient) && Boolean.TRUE.equals(rightDrug)
                && Boolean.TRUE.equals(rightDose) && Boolean.TRUE.equals(rightRoute)
                && Boolean.TRUE.equals(rightTime);
    }
}
