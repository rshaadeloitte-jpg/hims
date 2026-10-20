package com.hims.ordering.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * Saga coordination row for one placed order (MIG-033). One row per order
 * (UNIQUE order_id). Step columns are NULL when the step is not applicable:
 * BILLING always applies (the legacy mega-txn billed every order); LAB /
 * RADIOLOGY / NURSING apply when matching order_item rows exist; PHARMACY
 * applies when clinical_order.pharmacy_prescription_id was supplied.
 *
 * saga status: IN_PROGRESS -> COMPLETED (all applicable steps SUCCEEDED)
 *                                |-> CANCELLED (requester cancelled; compensated)
 *                                |-> FAILED    (a step failed; compensated)
 * step values: PENDING -> SUCCEEDED | FAILED; SUCCEEDED/FAILED -> COMPENSATED
 */
@Entity
@Table(name = "saga_state")
public class SagaState extends PrescriptionBase {

    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";
    public static final String FAILED = "FAILED";

    public static final String PENDING = "PENDING";
    public static final String SUCCEEDED = "SUCCEEDED";
    public static final String STEP_FAILED = "FAILED";
    public static final String COMPENSATED = "COMPENSATED";

    /** Applicable saga steps, in confirmation order. */
    public static final String BILLING = "BILLING";
    public static final String LAB = "LAB";
    public static final String RADIOLOGY = "RADIOLOGY";
    public static final String NURSING = "NURSING";
    public static final String PHARMACY = "PHARMACY";

    @Id
    @Column(name = "saga_id")
    private Long sagaId;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "saga_status", nullable = false, length = 20)
    private String sagaStatus = IN_PROGRESS;

    @Column(name = "billing_step", length = 20)
    private String billingStep;

    @Column(name = "lab_step", length = 20)
    private String labStep;

    @Column(name = "radiology_step", length = 20)
    private String radiologyStep;

    @Column(name = "nursing_step", length = 20)
    private String nursingStep;

    @Column(name = "pharmacy_step", length = 20)
    private String pharmacyStep;

    @Column(name = "failed_step", length = 20)
    private String failedStep;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    public Long getSagaId() { return sagaId; }
    public void setSagaId(Long sagaId) { this.sagaId = sagaId; }
    public Long getId() { return sagaId; }
    public void setId(Long id) { this.sagaId = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getSagaStatus() { return sagaStatus; }
    public void setSagaStatus(String sagaStatus) { this.sagaStatus = sagaStatus; }
    public String getBillingStep() { return billingStep; }
    public void setBillingStep(String billingStep) { this.billingStep = billingStep; }
    public String getLabStep() { return labStep; }
    public void setLabStep(String labStep) { this.labStep = labStep; }
    public String getRadiologyStep() { return radiologyStep; }
    public void setRadiologyStep(String radiologyStep) { this.radiologyStep = radiologyStep; }
    public String getNursingStep() { return nursingStep; }
    public void setNursingStep(String nursingStep) { this.nursingStep = nursingStep; }
    public String getPharmacyStep() { return pharmacyStep; }
    public void setPharmacyStep(String pharmacyStep) { this.pharmacyStep = pharmacyStep; }
    public String getFailedStep() { return failedStep; }
    public void setFailedStep(String failedStep) { this.failedStep = failedStep; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    /** Value of a named step column (null when not applicable). */
    public String step(String step) {
        return switch (step) {
            case BILLING -> billingStep;
            case LAB -> labStep;
            case RADIOLOGY -> radiologyStep;
            case NURSING -> nursingStep;
            case PHARMACY -> pharmacyStep;
            default -> throw new IllegalArgumentException("Unknown saga step: " + step);
        };
    }

    /** Sets a named step column. */
    public void setStep(String step, String value) {
        switch (step) {
            case BILLING -> billingStep = value;
            case LAB -> labStep = value;
            case RADIOLOGY -> radiologyStep = value;
            case NURSING -> nursingStep = value;
            case PHARMACY -> pharmacyStep = value;
            default -> throw new IllegalArgumentException("Unknown saga step: " + step);
        }
    }

    /** All applicable steps in canonical order (non-null columns only). */
    public List<String> applicableSteps() {
        List<String> steps = new ArrayList<>();
        for (String s : new String[]{BILLING, LAB, RADIOLOGY, NURSING, PHARMACY}) {
            if (step(s) != null) {
                steps.add(s);
            }
        }
        return steps;
    }

    /** True when every applicable step is SUCCEEDED. */
    public boolean allStepsSucceeded() {
        List<String> applicable = applicableSteps();
        return !applicable.isEmpty()
                && applicable.stream().allMatch(s -> SUCCEEDED.equals(step(s)));
    }

    /** Steps that were SUCCEEDED before compensation (OrderFailed payload). */
    public List<String> succeededSteps() {
        return applicableSteps().stream().filter(s -> SUCCEEDED.equals(step(s))).toList();
    }

    public boolean isInProgress() { return IN_PROGRESS.equals(sagaStatus); }
}
