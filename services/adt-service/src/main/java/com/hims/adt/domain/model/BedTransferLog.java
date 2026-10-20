package com.hims.adt.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/** One row per patient bed movement (transfer audit). */
@Entity
@Table(name = "t_bed_transfer_log")
public class BedTransferLog {

    @Id
    @Column(name = "transfer_id")
    private Long transferId;

    @Column(name = "admission_id", nullable = false)
    private Long admissionId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "from_bed_id")
    private Long fromBedId;

    @Column(name = "to_bed_id", nullable = false)
    private Long toBedId;

    @Column(name = "from_ward_id")
    private Long fromWardId;

    @Column(name = "to_ward_id")
    private Long toWardId;

    @Column(name = "reason")
    private String reason;

    @Column(name = "transferred_by")
    private Long transferredBy;

    @Column(name = "transferred_at", nullable = false)
    private Timestamp transferredAt;

    public Long getTransferId() { return transferId; }
    public void setTransferId(Long transferId) { this.transferId = transferId; }
    public Long getAdmissionId() { return admissionId; }
    public void setAdmissionId(Long admissionId) { this.admissionId = admissionId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getFromBedId() { return fromBedId; }
    public void setFromBedId(Long fromBedId) { this.fromBedId = fromBedId; }
    public Long getToBedId() { return toBedId; }
    public void setToBedId(Long toBedId) { this.toBedId = toBedId; }
    public Long getFromWardId() { return fromWardId; }
    public void setFromWardId(Long fromWardId) { this.fromWardId = fromWardId; }
    public Long getToWardId() { return toWardId; }
    public void setToWardId(Long toWardId) { this.toWardId = toWardId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Long getTransferredBy() { return transferredBy; }
    public void setTransferredBy(Long transferredBy) { this.transferredBy = transferredBy; }
    public Timestamp getTransferredAt() { return transferredAt; }
    public void setTransferredAt(Timestamp transferredAt) { this.transferredAt = transferredAt; }
}
