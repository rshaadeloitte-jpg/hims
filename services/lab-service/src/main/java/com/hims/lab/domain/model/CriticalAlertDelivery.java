package com.hims.lab.domain.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Enhanced fan-out delivery tracking for critical alerts.
 * Channels: WHATSAPP, SMS, EMAIL, CALL.
 * Status: PENDING, SENT, DELIVERED, FAILED, RETRY.
 * Ported from legacy NotifyPatientCriticalDetails.
 */
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@Table(name = "t_critical_alert_delivery")
public class CriticalAlertDelivery extends LabBase {

    @Id
    @Column(name = "delivery_id")
    private Long deliveryId;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "detail_id", nullable = false)
    private Long detailId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "encounter_id")
    private Long encounterId;

    @Column(name = "mrn_no", length = 50)
    private String mrnNo;

    @Column(name = "patient_name", length = 200)
    private String patientName;

    @Column(name = "test_id")
    private Long testId;

    @Column(name = "test_name", length = 200)
    private String testName;

    @Column(name = "result_value", length = 500)
    private String resultValue;

    @Column(name = "reference_range", length = 100)
    private String referenceRange;

    @Column(name = "inform_passed_to", length = 200)
    private String informPassedTo;

    @Column(name = "inform_passed_by", length = 200)
    private String informPassedBy;

    @Column(name = "doc_id")
    private Long docId;

    @Column(name = "ward", length = 100)
    private String ward;

    @Column(name = "bed_no", length = 50)
    private String bedNo;

    @Column(name = "email_id", length = 200)
    private String emailId;

    @Column(name = "channel", nullable = false, length = 20)
    private String channel = "WHATSAPP";

    @Column(name = "delivery_status", nullable = false, length = 20)
    private String deliveryStatus = "PENDING";

    @Column(name = "delivered_at")
    private Timestamp deliveredAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    public Long getDeliveryId() { return deliveryId; }
    public void setDeliveryId(Long deliveryId) { this.deliveryId = deliveryId; }
    public Long getId() { return deliveryId; }
    public void setId(Long id) { this.deliveryId = id; }
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public Long getDetailId() { return detailId; }
    public void setDetailId(Long detailId) { this.detailId = detailId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getEncounterId() { return encounterId; }
    public void setEncounterId(Long encounterId) { this.encounterId = encounterId; }
    public String getMrnNo() { return mrnNo; }
    public void setMrnNo(String mrnNo) { this.mrnNo = mrnNo; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public Long getTestId() { return testId; }
    public void setTestId(Long testId) { this.testId = testId; }
    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }
    public String getResultValue() { return resultValue; }
    public void setResultValue(String resultValue) { this.resultValue = resultValue; }
    public String getReferenceRange() { return referenceRange; }
    public void setReferenceRange(String referenceRange) { this.referenceRange = referenceRange; }
    public String getInformPassedTo() { return informPassedTo; }
    public void setInformPassedTo(String informPassedTo) { this.informPassedTo = informPassedTo; }
    public String getInformPassedBy() { return informPassedBy; }
    public void setInformPassedBy(String informPassedBy) { this.informPassedBy = informPassedBy; }
    public Long getDocId() { return docId; }
    public void setDocId(Long docId) { this.docId = docId; }
    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }
    public String getBedNo() { return bedNo; }
    public void setBedNo(String bedNo) { this.bedNo = bedNo; }
    public String getEmailId() { return emailId; }
    public void setEmailId(String emailId) { this.emailId = emailId; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }
    public Timestamp getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Timestamp deliveredAt) { this.deliveredAt = deliveredAt; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
}
