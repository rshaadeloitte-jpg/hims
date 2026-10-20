package com.hims.bloodbank.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;
import java.time.LocalDate;

/**
 * Blood bag (clean target of T_BLOODBAG); components hang off t_bloodcomponent.
 *
 * Lifecycle: collected -> crossMatched=true (+crossMatchedTestDate, linked to a
 * requisition via requisition_id) -> issued=true. BR-BLD-002 decrosses after 48h
 * crossmatched & unissued; BR-BLD-003 flags expirydate=today as isbagexpired
 * unless issued (and decrosses if it was crossmatched).
 */
@Entity
@Table(name = "t_bloodbag")
public class BloodBag extends BloodBankBase {

    @Id
    @Column(name = "bag_id")
    private Long bagId;

    @Column(name = "donor_id", nullable = false)
    private Long donorId;

    @Column(name = "blood_group", nullable = false)
    private String bloodGroup;

    @Column(name = "volume_ml", nullable = false)
    private Integer volumeMl;

    @Column(name = "collected_at", nullable = false)
    private Timestamp collectedAt;

    @Column(name = "expirydate", nullable = false)
    private LocalDate expirydate;

    /** Legacy column spelling kept verbatim (crosSMatched). */
    @Column(name = "crosSMatched", nullable = false)
    private Boolean crossMatched = false;

    /** Set when crossmatched — BR-BLD-002 48h timer measures from here. */
    @Column(name = "cross_matched_test_date")
    private Timestamp crossMatchedTestDate;

    /** Requisition this bag is cross-matched against (null while unlinked). */
    @Column(name = "requisition_id")
    private Long requisitionId;

    @Column(name = "isbagexpired", nullable = false)
    private Boolean isbagexpired = false;

    @Column(name = "issued", nullable = false)
    private Boolean issued = false;

    @Column(name = "issued_requisition_id")
    private Long issuedRequisitionId;

    public Long getBagId() { return bagId; }
    public void setBagId(Long bagId) { this.bagId = bagId; }
    public Long getId() { return bagId; }
    public void setId(Long id) { this.bagId = id; }
    public Long getDonorId() { return donorId; }
    public void setDonorId(Long donorId) { this.donorId = donorId; }
    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }
    public Integer getVolumeMl() { return volumeMl; }
    public void setVolumeMl(Integer volumeMl) { this.volumeMl = volumeMl; }
    public Timestamp getCollectedAt() { return collectedAt; }
    public void setCollectedAt(Timestamp collectedAt) { this.collectedAt = collectedAt; }
    public LocalDate getExpirydate() { return expirydate; }
    public void setExpirydate(LocalDate expirydate) { this.expirydate = expirydate; }
    public Boolean getCrossMatched() { return crossMatched; }
    public void setCrossMatched(Boolean crossMatched) { this.crossMatched = crossMatched; }
    public Timestamp getCrossMatchedTestDate() { return crossMatchedTestDate; }
    public void setCrossMatchedTestDate(Timestamp crossMatchedTestDate) { this.crossMatchedTestDate = crossMatchedTestDate; }
    public Long getRequisitionId() { return requisitionId; }
    public void setRequisitionId(Long requisitionId) { this.requisitionId = requisitionId; }
    public Boolean getIsbagexpired() { return isbagexpired; }
    public void setIsbagexpired(Boolean isbagexpired) { this.isbagexpired = isbagexpired; }
    public Boolean getIssued() { return issued; }
    public void setIssued(Boolean issued) { this.issued = issued; }
    public Long getIssuedRequisitionId() { return issuedRequisitionId; }
    public void setIssuedRequisitionId(Long issuedRequisitionId) { this.issuedRequisitionId = issuedRequisitionId; }
}
