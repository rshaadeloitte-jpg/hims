package com.hims.ordering.domain.model;

import java.io.Serializable;
import java.util.Objects;

/** Composite PK of prescription_item: (prescription_id, line_no). */
public class PrescriptionItemKey implements Serializable {
    private Long prescriptionId;
    private Long lineNo;

    public PrescriptionItemKey() {}
    public PrescriptionItemKey(Long prescriptionId, Long lineNo) {
        this.prescriptionId = prescriptionId;
        this.lineNo = lineNo;
    }
    public Long getPrescriptionId() { return prescriptionId; }
    public void setPrescriptionId(Long prescriptionId) { this.prescriptionId = prescriptionId; }
    public Long getLineNo() { return lineNo; }
    public void setLineNo(Long lineNo) { this.lineNo = lineNo; }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PrescriptionItemKey that)) return false;
        return Objects.equals(prescriptionId, that.prescriptionId) && Objects.equals(lineNo, that.lineNo);
    }
    @Override public int hashCode() { return Objects.hash(prescriptionId, lineNo); }
}
