package com.hims.pharmacy.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.util.ArrayList;
import java.util.List;

/**
 * Clean target of the legacy item-sale header (Catalog #9, GAP-REGISTER-5 WP-R2).
 *
 * Status machine (SMALLINT per 06 §A9):
 *   1=PENDING -> partial dispense -> 2=PARTIALLY_DISPENSED
 *            -> last line dispensed -> 3=FULLY_DISPENSED (terminal; further dispense rejects 400).
 *
 * BR-PHAR-001 header flag: {@code dispensed=true} EXACTLY when ALL detail lines
 * are dispensed=true.
 *
 * NOTE: {@code visit_ref} is provisioned (legacy parity column) but the WP-R2 API
 * contract for POST /api/v1/item-sales does not carry it — it stays NULL unless a
 * later slice adds it to the request (register-faithful, nothing invented).
 */
@Entity
@Table(name = "t_itemsale")
public class ItemSale extends PharmacyBase {

    public static final short STATUS_PENDING = 1;
    public static final short STATUS_PARTIALLY_DISPENSED = 2;
    public static final short STATUS_FULLY_DISPENSED = 3;

    @Id
    @Column(name = "sale_id")
    private Long saleId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "visit_ref", length = 50)
    private String visitRef;

    /** Prescription id in the ordering-service domain (E-06 payload); optional. */
    @Column(name = "prescription_id")
    private Long prescriptionId;

    @Column(name = "status_code", nullable = false)
    private Short statusCode = STATUS_PENDING;

    /** BR-PHAR-001 header flag: true iff every detail row has dispensed=true. */
    @Column(name = "dispensed", nullable = false)
    private Boolean dispensed = false;

    /** Not persisted: lines are loaded via repository in the service layer. */
    @Transient
    private List<ItemSaleDetail> details = new ArrayList<>();

    public Long getSaleId() { return saleId; }
    public void setSaleId(Long saleId) { this.saleId = saleId; }
    @Override public Long getId() { return saleId; }
    @Override public void setId(Long id) { this.saleId = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getVisitRef() { return visitRef; }
    public void setVisitRef(String visitRef) { this.visitRef = visitRef; }
    public Long getPrescriptionId() { return prescriptionId; }
    public void setPrescriptionId(Long prescriptionId) { this.prescriptionId = prescriptionId; }
    public Short getStatusCode() { return statusCode; }
    public void setStatusCode(Short statusCode) { this.statusCode = statusCode; }
    public Boolean getDispensed() { return dispensed; }
    public void setDispensed(Boolean dispensed) { this.dispensed = dispensed; }
    public List<ItemSaleDetail> getDetails() { return details; }
    public void setDetails(List<ItemSaleDetail> details) { this.details = details; }

    public boolean isPending() { return statusCode == STATUS_PENDING; }
    public boolean isFullyDispensed() { return statusCode == STATUS_FULLY_DISPENSED; }
}
