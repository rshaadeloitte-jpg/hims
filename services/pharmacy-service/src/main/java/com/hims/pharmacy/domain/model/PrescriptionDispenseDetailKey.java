package com.hims.pharmacy.domain.model;

import java.io.Serializable;
import java.util.Objects;

/** Composite key of t_prescription_dispense_detail: UNIQUE(dispense_id, line_no). */
public class PrescriptionDispenseDetailKey implements Serializable {

    private Long dispenseId;
    private Long lineNo;

    public PrescriptionDispenseDetailKey() {}
    public PrescriptionDispenseDetailKey(Long dispenseId, Long lineNo) {
        this.dispenseId = dispenseId;
        this.lineNo = lineNo;
    }

    public Long getDispenseId() { return dispenseId; }
    public Long getLineNo() { return lineNo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PrescriptionDispenseDetailKey that)) return false;
        return Objects.equals(dispenseId, that.dispenseId) && Objects.equals(lineNo, that.lineNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dispenseId, lineNo);
    }
}
