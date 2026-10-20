package com.hims.govt.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Chiranjeevi beneficiary enrolment (register WP-T4).
 * scheme CHECK-pinned to CHIRANJEEVI at DB level; enrolment_no UNIQUE.
 *
 * OUT OF SCOPE (comment-only): external Chiranjeevi-Yojana scheme-side push
 * sync — no inbound/outbound integration is modeled in this slice.
 */
@Entity
@Table(name = "t_beneficiary_enrolment")
public class BeneficiaryEnrolment extends GovtBase {

    public static final String SCHEME_CHIRANJEEVI = "CHIRANJEEVI";

    @Id
    @Column(name = "enrolment_id")
    private Long enrolmentId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "scheme", nullable = false, length = 30)
    private String scheme = SCHEME_CHIRANJEEVI;

    @Column(name = "enrolment_no", nullable = false, length = 40)
    private String enrolmentNo;

    @Column(name = "enrolled_on", nullable = false)
    private LocalDate enrolledOn;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public Long getEnrolmentId() { return enrolmentId; }
    public void setEnrolmentId(Long enrolmentId) { this.enrolmentId = enrolmentId; }
    @Override public Long getId() { return enrolmentId; }
    @Override public void setId(Long id) { this.enrolmentId = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getScheme() { return scheme; }
    public void setScheme(String scheme) { this.scheme = scheme; }
    public String getEnrolmentNo() { return enrolmentNo; }
    public void setEnrolmentNo(String enrolmentNo) { this.enrolmentNo = enrolmentNo; }
    public LocalDate getEnrolledOn() { return enrolledOn; }
    public void setEnrolledOn(LocalDate enrolledOn) { this.enrolledOn = enrolledOn; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
