package com.hims.ordering.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * One drug/item line of a prescription (BR-ORD-005): qty NUMERIC(12,2) &gt; 0,
 * dose + frequency REQUIRED (clinical safety gate over the legacy nullable
 * columns). UNIQUE(prescription_id, line_no) as the composite PK.
 */
@Entity
@IdClass(PrescriptionItemKey.class)
@Table(name = "prescription_item")
public class PrescriptionItem extends PrescriptionBase {

    @Id
    @Column(name = "prescription_id")
    private Long prescriptionId;

    @Id
    @Column(name = "line_no")
    private Long lineNo;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "qty", nullable = false, precision = 12, scale = 2)
    private BigDecimal qty;

    @Column(name = "dose", nullable = false, length = 50)
    private String dose;

    @Column(name = "frequency", nullable = false, length = 50)
    private String frequency;

    /** Composite key has no single surrogate id (base contract satisfied structurally). */
    @Override public Long getId() { return prescriptionId; }
    @Override public void setId(Long id) { this.prescriptionId = id; }
    public Long getPrescriptionId() { return prescriptionId; }
    public void setPrescriptionId(Long prescriptionId) { this.prescriptionId = prescriptionId; }
    public Long getLineNo() { return lineNo; }
    public void setLineNo(Long lineNo) { this.lineNo = lineNo; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public BigDecimal getQty() { return qty; }
    public void setQty(BigDecimal qty) { this.qty = qty; }
    public String getDose() { return dose; }
    public void setDose(String dose) { this.dose = dose; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PrescriptionItem that)) return false;
        return Objects.equals(prescriptionId, that.prescriptionId)
                && Objects.equals(lineNo, that.lineNo);
    }
    @Override public int hashCode() { return Objects.hash(prescriptionId, lineNo); }
}
