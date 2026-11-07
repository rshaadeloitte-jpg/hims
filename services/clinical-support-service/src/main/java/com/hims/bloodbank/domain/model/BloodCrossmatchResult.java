package com.hims.bloodbank.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Cross-match result entity ported from legacy BloodCrossmatchingResult / BloodCrossMatchResultDetails.
 *
 * Tracks the serological testing lifecycle for a bag against a requisition:
 *   PENDING -> COMPATIBLE | INCOMPATIBLE.
 * Only COMPATIBLE promotes the bag to crossMatched=true and counts volume toward requisition status 2.
 * INCOMPATIBLE releases the bag reservation.
 */
@Entity
@Table(name = "t_bloodcrossmatchingresult")
public class BloodCrossmatchResult extends BloodBankBase {

    public static final String RESULT_PENDING = "PENDING";
    public static final String RESULT_COMPATIBLE = "COMPATIBLE";
    public static final String RESULT_INCOMPATIBLE = "INCOMPATIBLE";

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "requisition_id", nullable = false)
    private Long requisitionId;

    @Column(name = "requisition_detail_id")
    private Long requisitionDetailId;

    @Column(name = "bag_id", nullable = false)
    private Long bagId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "method")
    private String method;

    @Column(name = "result", nullable = false)
    private String result = RESULT_PENDING;

    @Column(name = "test_date")
    private Timestamp testDate;

    @Column(name = "tested_by")
    private String testedBy;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "isdecrossed", nullable = false)
    private Boolean isDecrossed = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRequisitionId() { return requisitionId; }
    public void setRequisitionId(Long requisitionId) { this.requisitionId = requisitionId; }

    public Long getRequisitionDetailId() { return requisitionDetailId; }
    public void setRequisitionDetailId(Long requisitionDetailId) { this.requisitionDetailId = requisitionDetailId; }

    public Long getBagId() { return bagId; }
    public void setBagId(Long bagId) { this.bagId = bagId; }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public Timestamp getTestDate() { return testDate; }
    public void setTestDate(Timestamp testDate) { this.testDate = testDate; }

    public String getTestedBy() { return testedBy; }
    public void setTestedBy(String testedBy) { this.testedBy = testedBy; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public Boolean getIsDecrossed() { return isDecrossed; }
    public void setIsDecrossed(Boolean isDecrossed) { this.isDecrossed = isDecrossed; }

    // Legacy field alias accessors
    public String getNotes() { return remarks; }
    public void setNotes(String notes) { this.remarks = notes; }

    public Long getReqDetailsId() { return requisitionDetailId; }
    public void setReqDetailsId(Long reqDetailsId) { this.requisitionDetailId = reqDetailsId; }

    public Timestamp getCrossMatchedTestDate() { return testDate; }
    public void setCrossMatchedTestDate(Timestamp crossMatchedTestDate) { this.testDate = crossMatchedTestDate; }
}
