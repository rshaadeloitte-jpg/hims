package com.hims.bcma.domain.model;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "t_verification_log")
public class VerificationLog extends BcmaBase {

    @Id
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "bcma_id", nullable = false)
    private Long bcmaId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "verification_event", nullable = false, length = 50)
    private String verificationEvent;

    @Column(name = "result", nullable = false)
    private Boolean result = false;

    @Column(name = "scanned_by")
    private Long scannedBy;

    @Column(name = "scan_timestamp")
    private Timestamp scanTimestamp;

    @Override
    public Long getId() { return logId; }
    @Override
    public void setId(Long id) { this.logId = id; }

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public Long getBcmaId() { return bcmaId; }
    public void setBcmaId(Long bcmaId) { this.bcmaId = bcmaId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getVerificationEvent() { return verificationEvent; }
    public void setVerificationEvent(String verificationEvent) { this.verificationEvent = verificationEvent; }
    public Boolean getResult() { return result; }
    public void setResult(Boolean result) { this.result = result; }
    public Long getScannedBy() { return scannedBy; }
    public void setScannedBy(Long scannedBy) { this.scannedBy = scannedBy; }
    public Timestamp getScanTimestamp() { return scanTimestamp; }
    public void setScanTimestamp(Timestamp scanTimestamp) { this.scanTimestamp = scanTimestamp; }
}
