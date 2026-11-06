package com.hims.bloodbank.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Blood requisition header (clean target of T_BLOODREQUISITION).
 *
 * BR-BLD-001 status machine (requisition_status SMALLINT):
 *   1 = new; 2 = cross-matched (cumulative cross-matched volume >= ordered volume);
 *   3 = issued (cumulative issued volume >= ordered volume); 10 = cancelled/expired.
 * Transitions are computed from the linked bag states on every crossmatch/issue/decross
 * (see RequisitionService.recomputeStatus); cancel -> 10 directly.
 *
 * NOTE: NO BloodRequisitionCreated event is emitted on create — DEFERRED per
 * MICROSERVICE-CATALOG #13 / KAFKA-EVENT-CATALOG §3 (weak consumer evidence).
 */
@Entity
@Table(name = "t_bloodrequisition")
public class BloodRequisition extends BloodBankBase {

    public static final int STATUS_NEW = 1;
    public static final int STATUS_CROSS_MATCHED = 2;
    public static final int STATUS_ISSUED = 3;
    public static final int STATUS_CANCELLED_EXPIRED = 10;

    @Id
    @Column(name = "requisition_id")
    private Long requisitionId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "blood_group", nullable = false)
    private String bloodGroup;

    @Column(name = "ordered_volume_ml", nullable = false)
    private Integer orderedVolumeMl;

    @Column(name = "requesteddate", nullable = false)
    private Timestamp requesteddate;

    @Column(name = "sample_collected", nullable = false)
    private Boolean sampleCollected = false;

    /** BR-BLD-001 machine state; column requisition_status (audit-block "status" is the generic boolean). */
    @Column(name = "requisition_status", nullable = false)
    private Integer status = STATUS_NEW;

    /** Set by BR-BLD-004 expiry scan (never by reads). */
    @Column(name = "order_expired", nullable = false)
    private Boolean orderExpired = false;

    /** Legacy orderValidationPeriod(h) snapshot at creation — drives BR-BLD-004. */
    @Column(name = "order_validation_hours", nullable = false)
    private Integer orderValidationHours = 72;

    public Long getRequisitionId() { return requisitionId; }
    public void setRequisitionId(Long requisitionId) { this.requisitionId = requisitionId; }
    public Long getId() { return requisitionId; }
    public void setId(Long id) { this.requisitionId = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }
    public Integer getOrderedVolumeMl() { return orderedVolumeMl; }
    public void setOrderedVolumeMl(Integer orderedVolumeMl) { this.orderedVolumeMl = orderedVolumeMl; }
    public Timestamp getRequesteddate() { return requesteddate; }
    public void setRequesteddate(Timestamp requesteddate) { this.requesteddate = requesteddate; }
    public Boolean getSampleCollected() { return sampleCollected; }
    public void setSampleCollected(Boolean sampleCollected) { this.sampleCollected = sampleCollected; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Boolean getOrderExpired() { return orderExpired; }
    public void setOrderExpired(Boolean orderExpired) { this.orderExpired = orderExpired; }
    public Integer getOrderValidationHours() { return orderValidationHours; }
    public void setOrderValidationHours(Integer orderValidationHours) { this.orderValidationHours = orderValidationHours; }
}
