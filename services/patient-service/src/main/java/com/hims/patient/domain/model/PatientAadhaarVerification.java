package com.hims.patient.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * Aadhaar OTP/biometric verification audit (M01 gap).
 * Ported from ihms IntegrationServiceImpl aadhaar flow (AES/CBC) + FamilyMemberDetails.biometricDetailsJson.
 */
@Entity
@Table(name = "patient_aadhaar_verification", schema = "ihms_patient")
public class PatientAadhaarVerification extends PatientBase {

    @Id
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "aadhaar_no", nullable = false)
    private String aadhaarNo;

    @Column(name = "verification_type", nullable = false)
    private String verificationType; // OTP or BIOMETRIC

    @Column(name = "otp")
    private String otp;

    @Column(name = "verified", nullable = false)
    private Boolean verified = false;

    @Column(name = "verified_at")
    private Timestamp verifiedAt;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "attempts", nullable = false)
    private Integer attempts = 0;

    @Column(name = "expires_at")
    private Timestamp expiresAt;

    @PrePersist
    void onCreate() {
        if (addedDatetime == null) addedDatetime = Timestamp.from(Instant.now());
    }

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getAadhaarNo() { return aadhaarNo; }
    public void setAadhaarNo(String aadhaarNo) { this.aadhaarNo = aadhaarNo; }
    public String getVerificationType() { return verificationType; }
    public void setVerificationType(String verificationType) { this.verificationType = verificationType; }
    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
    public Boolean getVerified() { return verified; }
    public void setVerified(Boolean verified) { this.verified = verified; }
    public Timestamp getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Timestamp verifiedAt) { this.verifiedAt = verifiedAt; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public Integer getAttempts() { return attempts; }
    public void setAttempts(Integer attempts) { this.attempts = attempts; }
    public Timestamp getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Timestamp expiresAt) { this.expiresAt = expiresAt; }
}
