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
 * Clean target of T_PATHORDERDETAIL — one test line per booking.
 * Status mirrors the header status machine while the booking flows through
 * collect/start/cancel; result finalization is tracked by is_final/finalized_at
 * (documented register extension so per-detail finality survives without a
 * separate marker table).
 */
@Entity
@Table(name = "t_pathorderdetail")
public class PathOrderDetail extends LabBase {

    @Id
    @Column(name = "detail_id")
    private Long detailId;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "test_code", nullable = false, length = 50)
    private String testCode;

    @Column(name = "test_name", nullable = false, length = 200)
    private String testName;

    /** Mirrors header status machine (1..5). */
    @Column(name = "detail_status", nullable = false)
    private Short detailStatus = PathOrderBooking.STATUS_ORDERED;

    @Column(name = "is_final", nullable = false)
    private Boolean isFinal = false;

    @Column(name = "finalized_at")
    private Timestamp finalizedAt;

    // M15 workflow — ported from T_PathOrderBookingDetails (PathoOrderServiceImpl)
    @Column(name = "sample_no", length = 50)
    private String sampleNo;

    @Column(name = "barcode", length = 100)
    private String barcode;

    @Column(name = "is_accepted", nullable = false)
    private Boolean isAccepted = false;

    @Column(name = "is_rejected", nullable = false)
    private Boolean isRejected = false;

    @Column(name = "acceptance_date")
    private Timestamp acceptanceDate;

    @Column(name = "rejection_date")
    private Timestamp rejectionDate;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "acceptance_reason", length = 500)
    private String acceptanceReason;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    @Column(name = "verified_at")
    private Timestamp verifiedAt;

    @Column(name = "verified_by")
    private Long verifiedBy;

    /** Not persisted; populated by the service layer for API responses. */
    @Transient
    private List<PathoPatientParameterDetails> parameters = new ArrayList<>();

    public Long getDetailId() { return detailId; }
    public void setDetailId(Long detailId) { this.detailId = detailId; }
    public Long getId() { return detailId; }
    public void setId(Long id) { this.detailId = id; }
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public String getTestCode() { return testCode; }
    public void setTestCode(String testCode) { this.testCode = testCode; }
    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }
    public Short getDetailStatus() { return detailStatus; }
    public void setDetailStatus(Short detailStatus) { this.detailStatus = detailStatus; }
    public Boolean getIsFinal() { return isFinal; }
    public void setIsFinal(Boolean isFinal) { this.isFinal = isFinal; }
    public Timestamp getFinalizedAt() { return finalizedAt; }
    public void setFinalizedAt(Timestamp finalizedAt) { this.finalizedAt = finalizedAt; }
    public String getSampleNo() { return sampleNo; }
    public void setSampleNo(String sampleNo) { this.sampleNo = sampleNo; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public Boolean getIsAccepted() { return isAccepted; }
    public void setIsAccepted(Boolean isAccepted) { this.isAccepted = isAccepted; }
    public Boolean getIsRejected() { return isRejected; }
    public void setIsRejected(Boolean isRejected) { this.isRejected = isRejected; }
    public Timestamp getAcceptanceDate() { return acceptanceDate; }
    public void setAcceptanceDate(Timestamp acceptanceDate) { this.acceptanceDate = acceptanceDate; }
    public Timestamp getRejectionDate() { return rejectionDate; }
    public void setRejectionDate(Timestamp rejectionDate) { this.rejectionDate = rejectionDate; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public String getAcceptanceReason() { return acceptanceReason; }
    public void setAcceptanceReason(String acceptanceReason) { this.acceptanceReason = acceptanceReason; }
    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }
    public Timestamp getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Timestamp verifiedAt) { this.verifiedAt = verifiedAt; }
    public Long getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(Long verifiedBy) { this.verifiedBy = verifiedBy; }
    public List<PathoPatientParameterDetails> getParameters() { return parameters; }
    public void setParameters(List<PathoPatientParameterDetails> parameters) { this.parameters = parameters; }
}
