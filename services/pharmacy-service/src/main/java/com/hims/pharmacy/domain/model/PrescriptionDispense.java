package com.hims.pharmacy.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * LOCAL projection of the ordering-service prescription master for dispensing
 * tracking (legacy T_PRESCRIPTIONDISPENSE semantics, GAP-REGISTER-5 WP-R2).
 *
 * One row per prescription_id (UNIQUE). Ordering-service remains the OWNER of
 * prescription truth via the E-06 PrescriptionCreated payload; the cross-service
 * write-back of IsClosed/PendingQty to ihms_clinical is DEFERRED post-saga
 * (register: "comment + report") — this table is the pharmacy-local projection
 * BR-PHAR-002 operates on.
 *
 * BR-PHAR-002 master rule: {@code isClosed=true} EXACTLY when ALL projection
 * detail rows have isClosed=true.
 */
@Entity
@Table(name = "t_prescription_dispense")
public class PrescriptionDispense extends PharmacyBase {

    @Id
    @Column(name = "dispense_id")
    private Long dispenseId;

    @Column(name = "prescription_id", nullable = false, unique = true)
    private Long prescriptionId;

    /** EMR number of the linked prescription (PUT /api/v1/prescriptions/{id}/emr-no). */
    @Column(name = "emr_no", length = 50)
    private String emrNo;

    /** BR-PHAR-002 master flag: legacy IsClosed='1' when all details are closed. */
    @Column(name = "is_closed", nullable = false)
    private Boolean closed = false;

    @Override public Long getId() { return dispenseId; }
    @Override public void setId(Long id) { this.dispenseId = id; }
    public Long getDispenseId() { return dispenseId; }
    public void setDispenseId(Long dispenseId) { this.dispenseId = dispenseId; }
    public Long getPrescriptionId() { return prescriptionId; }
    public void setPrescriptionId(Long prescriptionId) { this.prescriptionId = prescriptionId; }
    public String getEmrNo() { return emrNo; }
    public void setEmrNo(String emrNo) { this.emrNo = emrNo; }
    public Boolean getClosed() { return closed; }
    public void setClosed(Boolean closed) { this.closed = closed; }
}
