package com.hims.ordering.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Order header (MIG-033 saga owner). Clean target of the legacy AddServiceOrder
 * header fragment of the mega-transaction.
 *
 * Saga status machine (SMALLINT per 06 §A9):
 *   1=INITIATED  — created; OrderInitiated emitted in the SAME tx via the outbox
 *   2=CONFIRMED  — every applicable saga step SUCCEEDED
 *   3=CANCELLED  — cancel guard INITIATED-only; succeeded steps are compensated
 *                  and an OrderFailed event is emitted in the same tx
 *   4=FAILED     — a downstream step failed; rejectionReason persisted and an
 *                  OrderFailed compensation event emitted in the same tx
 */
@Entity
@Table(name = "clinical_order")
public class ClinicalOrder extends PrescriptionBase {

    public static final short STATUS_INITIATED = 1;
    public static final short STATUS_CONFIRMED = 2;
    public static final short STATUS_CANCELLED = 3;
    public static final short STATUS_FAILED = 4;

    @Id
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "encounter_id")
    private Long encounterId;

    @Column(name = "admission_id")
    private Long admissionId;

    @Column(name = "prescriber_id", nullable = false)
    private Long prescriberId;

    @Column(name = "ward_id")
    private Long wardId;

    @Column(name = "status_code", nullable = false)
    private Short statusCode = STATUS_INITIATED;

    /** Server-computed SUM(rate*qty) over order_item lines (never client-supplied). */
    @Column(name = "gross_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal grossTotal = BigDecimal.ZERO;

    /** BR-ORD-003: concession percent 0..100; amount derived HALF_UP 2dp server-side. */
    @Column(name = "concession_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal concessionPercent = BigDecimal.ZERO;

    @Column(name = "concession_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal concessionAmount = BigDecimal.ZERO;

    @Column(name = "net_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal netTotal = BigDecimal.ZERO;

    /** Downstream rejection reason persisted on compensation (FAILED/CANCELLED only). */
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    /** When supplied at creation, links the order to a prescription -> PHARMACY saga step applies. */
    @Column(name = "pharmacy_prescription_id")
    private Long pharmacyPrescriptionId;

    /** Not persisted: items loaded via repository in the service layer. */
    @Transient
    private List<OrderItem> items = new ArrayList<>();

    /** Not persisted: saga coordination row (loaded via repository in the service layer). */
    @Transient
    private SagaState saga;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getId() { return orderId; }
    public void setId(Long id) { this.orderId = orderId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getEncounterId() { return encounterId; }
    public void setEncounterId(Long encounterId) { this.encounterId = encounterId; }
    public Long getAdmissionId() { return admissionId; }
    public void setAdmissionId(Long admissionId) { this.admissionId = admissionId; }
    public Long getPrescriberId() { return prescriberId; }
    public void setPrescriberId(Long prescriberId) { this.prescriberId = prescriberId; }
    public Long getWardId() { return wardId; }
    public void setWardId(Long wardId) { this.wardId = wardId; }
    public Short getStatusCode() { return statusCode; }
    public void setStatusCode(Short statusCode) { this.statusCode = statusCode; }
    public BigDecimal getGrossTotal() { return grossTotal; }
    public void setGrossTotal(BigDecimal grossTotal) { this.grossTotal = grossTotal; }
    public BigDecimal getConcessionPercent() { return concessionPercent; }
    public void setConcessionPercent(BigDecimal concessionPercent) { this.concessionPercent = concessionPercent; }
    public BigDecimal getConcessionAmount() { return concessionAmount; }
    public void setConcessionAmount(BigDecimal concessionAmount) { this.concessionAmount = concessionAmount; }
    public BigDecimal getNetTotal() { return netTotal; }
    public void setNetTotal(BigDecimal netTotal) { this.netTotal = netTotal; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public Long getPharmacyPrescriptionId() { return pharmacyPrescriptionId; }
    public void setPharmacyPrescriptionId(Long pharmacyPrescriptionId) { this.pharmacyPrescriptionId = pharmacyPrescriptionId; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    public SagaState getSaga() { return saga; }
    public void setSaga(SagaState saga) { this.saga = saga; }

    public boolean isInitiated() { return statusCode == STATUS_INITIATED; }
    public boolean isConfirmed() { return statusCode == STATUS_CONFIRMED; }
    public boolean isCancelled() { return statusCode == STATUS_CANCELLED; }
    public boolean isFailed() { return statusCode == STATUS_FAILED; }

    public boolean isActive() { return statusCode == STATUS_INITIATED || statusCode == STATUS_CONFIRMED; }
}
