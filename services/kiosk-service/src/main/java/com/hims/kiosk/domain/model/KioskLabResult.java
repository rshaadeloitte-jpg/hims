package com.hims.kiosk.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "kiosk_lab_result", schema = "ihms_kiosk")
public class KioskLabResult extends KioskBase {
    @Id
    private Long id;
    @Column(name = "patient_id", nullable = false)
    private Long patientId;
    @Column(name = "test_order_id")
    private Long testOrderId;
    @Column(name = "result_data")
    private String resultData;
    @Column(name = "file_token")
    private String fileToken;
    @Column(name = "verified", nullable = false)
    private Boolean verified = false;
    @Column(name = "uploaded_at")
    private java.sql.Timestamp uploadedAt;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getTestOrderId() { return testOrderId; }
    public void setTestOrderId(Long testOrderId) { this.testOrderId = testOrderId; }
    public String getResultData() { return resultData; }
    public void setResultData(String resultData) { this.resultData = resultData; }
    public String getFileToken() { return fileToken; }
    public void setFileToken(String fileToken) { this.fileToken = fileToken; }
    public Boolean getVerified() { return verified; }
    public void setVerified(Boolean verified) { this.verified = verified; }
    public java.sql.Timestamp getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(java.sql.Timestamp uploadedAt) { this.uploadedAt = uploadedAt; }
}
