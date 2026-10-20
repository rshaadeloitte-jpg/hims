package com.hims.radiology.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.util.ArrayList;
import java.util.List;

/**
 * Clean target of the legacy radiology order booking header (Catalog #8, register WP-R1).
 *
 * Status machine (SMALLINT per 06 §A9):
 *   1=SCHEDULED -> start -> 2=IN_PROGRESS -> report -> 3=COMPLETED
 *   cancel allowed from any non-COMPLETED status -> 4=CANCELLED.
 * Image capture is a flag on the header (is_image_captured), rejected only when COMPLETED.
 */
@Entity
@Table(name = "t_radorderbooking")
public class RadOrderBooking extends RadOrderBase {

    public static final short STATUS_SCHEDULED = 1;
    public static final short STATUS_IN_PROGRESS = 2;
    public static final short STATUS_COMPLETED = 3;
    public static final short STATUS_CANCELLED = 4;

    public static final short PRIORITY_ROUTINE = 1;
    public static final short PRIORITY_URGENT = 2;

    @Id
    @Column(name = "rad_order_id")
    private Long radOrderId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "encounter_id")
    private Long encounterId;

    @Column(name = "admission_id")
    private Long admissionId;

    /** XRAY / CT / MRI / US (DB CHECK pins the set; API validates identically, BEXA003). */
    @Column(name = "modality", nullable = false, length = 50)
    private String modality;

    @Column(name = "priority", nullable = false)
    private Short priority = PRIORITY_ROUTINE;

    @Column(name = "status", nullable = false)
    private Short status = STATUS_SCHEDULED;

    @Column(name = "is_image_captured", nullable = false)
    private Boolean imageCaptured = false;

    @Column(name = "report_text", columnDefinition = "text")
    private String reportText;

    @Column(name = "reported_by", length = 100)
    private String reportedBy;

    /** Not persisted: lines are loaded via repository in the service layer. */
    @Transient
    private List<RadOrderDetail> details = new ArrayList<>();

    public Long getRadOrderId() { return radOrderId; }
    public void setRadOrderId(Long radOrderId) { this.radOrderId = radOrderId; }
    public Long getId() { return radOrderId; }
    public void setId(Long id) { this.radOrderId = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getEncounterId() { return encounterId; }
    public void setEncounterId(Long encounterId) { this.encounterId = encounterId; }
    public Long getAdmissionId() { return admissionId; }
    public void setAdmissionId(Long admissionId) { this.admissionId = admissionId; }
    public String getModality() { return modality; }
    public void setModality(String modality) { this.modality = modality; }
    public Short getPriority() { return priority; }
    public void setPriority(Short priority) { this.priority = priority; }
    public Short getStatus() { return status; }
    public void setStatus(Short status) { this.status = status; }
    public Boolean getImageCaptured() { return imageCaptured; }
    public void setImageCaptured(Boolean imageCaptured) { this.imageCaptured = imageCaptured; }
    public String getReportText() { return reportText; }
    public void setReportText(String reportText) { this.reportText = reportText; }
    public String getReportedBy() { return reportedBy; }
    public void setReportedBy(String reportedBy) { this.reportedBy = reportedBy; }
    public List<RadOrderDetail> getDetails() { return details; }
    public void setDetails(List<RadOrderDetail> details) { this.details = details; }

    public boolean isScheduled() { return status == STATUS_SCHEDULED; }
    public boolean isInProgress() { return status == STATUS_IN_PROGRESS; }
    public boolean isCompleted() { return status == STATUS_COMPLETED; }
    public boolean isCancelled() { return status == STATUS_CANCELLED; }
}
