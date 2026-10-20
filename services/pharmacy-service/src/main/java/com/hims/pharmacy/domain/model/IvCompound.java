package com.hims.pharmacy.domain.model;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * IV compounding order (sterile preparation).
 * Status: 1=PENDING 2=COMPOUNDED 3=VERIFIED 4=ADMINISTERED 5=CANCELLED
 */
@Entity
@Table(name = "t_iv_compound")
public class IvCompound extends PharmacyBase {

    public static final short STATUS_PENDING = 1;
    public static final short STATUS_COMPOUNDED = 2;
    public static final short STATUS_VERIFIED = 3;
    public static final short STATUS_ADMINISTERED = 4;
    public static final short STATUS_CANCELLED = 5;

    @Id
    @Column(name = "compound_id")
    private Long compoundId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "encounter_id")
    private Long encounterId;

    @Column(name = "prescription_id")
    private Long prescriptionId;

    @Column(name = "order_date", nullable = false)
    private Timestamp orderDate;

    @Column(name = "status_code", nullable = false)
    private Short statusCode = STATUS_PENDING;

    @Column(name = "total_volume_ml", precision = 10, scale = 2)
    private java.math.BigDecimal totalVolumeMl;

    @Column(name = "beyond_use_date")
    private Timestamp beyondUseDate;

    @Column(name = "verified_by")
    private Long verifiedBy;

    @Column(name = "verified_at")
    private Timestamp verifiedAt;

    @Column(name = "administered_by")
    private Long administeredBy;

    @Column(name = "administered_at")
    private Timestamp administeredAt;

    @Column(name = "unit_cost", precision = 10, scale = 2)
    private java.math.BigDecimal unitCost;

    @Column(name = "billed_amount", precision = 10, scale = 2)
    private java.math.BigDecimal billedAmount;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Transient
    private java.util.List<IvCompoundLine> lines = new java.util.ArrayList<>();

    public Long getCompoundId() { return compoundId; }
    public void setCompoundId(Long compoundId) { this.compoundId = compoundId; }
    @Override public Long getId() { return compoundId; }
    @Override public void setId(Long id) { this.compoundId = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getEncounterId() { return encounterId; }
    public void setEncounterId(Long encounterId) { this.encounterId = encounterId; }
    public Long getPrescriptionId() { return prescriptionId; }
    public void setPrescriptionId(Long prescriptionId) { this.prescriptionId = prescriptionId; }
    public Timestamp getOrderDate() { return orderDate; }
    public void setOrderDate(Timestamp orderDate) { this.orderDate = orderDate; }
    public Short getStatusCode() { return statusCode; }
    public void setStatusCode(Short statusCode) { this.statusCode = statusCode; }
    public java.math.BigDecimal getTotalVolumeMl() { return totalVolumeMl; }
    public void setTotalVolumeMl(java.math.BigDecimal totalVolumeMl) { this.totalVolumeMl = totalVolumeMl; }
    public Timestamp getBeyondUseDate() { return beyondUseDate; }
    public void setBeyondUseDate(Timestamp beyondUseDate) { this.beyondUseDate = beyondUseDate; }
    public Long getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(Long verifiedBy) { this.verifiedBy = verifiedBy; }
    public Timestamp getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Timestamp verifiedAt) { this.verifiedAt = verifiedAt; }
    public Long getAdministeredBy() { return administeredBy; }
    public void setAdministeredBy(Long administeredBy) { this.administeredBy = administeredBy; }
    public Timestamp getAdministeredAt() { return administeredAt; }
    public void setAdministeredAt(Timestamp administeredAt) { this.administeredAt = administeredAt; }
    public java.math.BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(java.math.BigDecimal unitCost) { this.unitCost = unitCost; }
    public java.math.BigDecimal getBilledAmount() { return billedAmount; }
    public void setBilledAmount(java.math.BigDecimal billedAmount) { this.billedAmount = billedAmount; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public java.util.List<IvCompoundLine> getLines() { return lines; }
    public void setLines(java.util.List<IvCompoundLine> lines) { this.lines = lines; }

    public boolean isPending() { return statusCode == STATUS_PENDING; }
    public boolean isCompounded() { return statusCode == STATUS_COMPOUNDED; }
    public boolean isTerminal() {
        return statusCode == STATUS_ADMINISTERED || statusCode == STATUS_CANCELLED;
    }
}
