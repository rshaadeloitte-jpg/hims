package com.hims.patient.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** Clean target of T_PATIENTSPONSOR (sponsor/scheme linkage — RGHS/Chiranjeevi etc.). */
@Entity
@Table(name = "patient_sponsor")
public class PatientSponsor extends PatientBase {

    @Id
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    /** e.g. RGHS / CHIRANJEEVI / CGHS / PRIVATE-INSURER */
    @Column(name = "sponsor_type", nullable = false)
    private String sponsorType;

    @Column(name = "scheme_code")
    private String schemeCode;

    @Column(name = "policy_no")
    private String policyNo;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getSponsorType() { return sponsorType; }
    public void setSponsorType(String sponsorType) { this.sponsorType = sponsorType; }
    public String getSchemeCode() { return schemeCode; }
    public void setSchemeCode(String schemeCode) { this.schemeCode = schemeCode; }
    public String getPolicyNo() { return policyNo; }
    public void setPolicyNo(String policyNo) { this.policyNo = policyNo; }
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }
}
