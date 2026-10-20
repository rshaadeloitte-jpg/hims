package com.hims.lab.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Clean target of T_PATHORDERBOOKING (result-side slice of Catalog #7).
 *
 * Status machine (register WP-7):
 *   1=ORDERED -> collect -> 2=SAMPLE_COLLECTED -> start -> 3=IN_PROGRESS
 *   -> all details final -> 4=FINALIZED
 *   cancel from 1|2|3 -> 5=CANCELLED (never from FINALIZED)
 * bookingNo = 'LAB-' + bookingId.
 */
@Entity
@Table(name = "t_pathorderbooking")
public class PathOrderBooking extends LabBase {

    public static final short STATUS_ORDERED = 1;
    public static final short STATUS_SAMPLE_COLLECTED = 2;
    public static final short STATUS_IN_PROGRESS = 3;
    public static final short STATUS_FINALIZED = 4;
    public static final short STATUS_CANCELLED = 5;

    @Id
    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "encounter_id")
    private Long encounterId;

    @Column(name = "booking_no", nullable = false, length = 50, unique = true)
    private String bookingNo;

    @Column(name = "status_code", nullable = false)
    private Short bookingStatus = STATUS_ORDERED;

    @Column(name = "is_out_sourced", nullable = false)
    private Boolean isOutSourced = false;

    @Column(name = "finalized_at")
    private Timestamp finalizedAt;

    @Column(name = "is_critical", nullable = false)
    private Boolean isCritical = false;

    /** Not persisted: details are loaded via repository in the service layer. */
    @Transient
    private List<PathOrderDetail> details = new ArrayList<>();

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public Long getId() { return bookingId; }
    public void setId(Long id) { this.bookingId = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getEncounterId() { return encounterId; }
    public void setEncounterId(Long encounterId) { this.encounterId = encounterId; }
    public String getBookingNo() { return bookingNo; }
    public void setBookingNo(String bookingNo) { this.bookingNo = bookingNo; }
    public Short getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(Short bookingStatus) { this.bookingStatus = bookingStatus; }
    public Boolean getIsOutSourced() { return isOutSourced; }
    public void setIsOutSourced(Boolean isOutSourced) { this.isOutSourced = isOutSourced; }
    public Timestamp getFinalizedAt() { return finalizedAt; }
    public void setFinalizedAt(Timestamp finalizedAt) { this.finalizedAt = finalizedAt; }
    public Boolean getIsCritical() { return isCritical; }
    public void setIsCritical(Boolean isCritical) { this.isCritical = isCritical; }
    public List<PathOrderDetail> getDetails() { return details; }
    public void setDetails(List<PathOrderDetail> details) { this.details = details; }

    public boolean isCancelled() { return bookingStatus == STATUS_CANCELLED; }
    public boolean isFinalized() { return bookingStatus == STATUS_FINALIZED; }
}
