package com.hims.radiology.domain.model;

import java.io.Serializable;
import java.util.Objects;

/** Composite key of t_radorder_detail: UNIQUE(rad_order_id, line_no). */
public class RadOrderDetailKey implements Serializable {

    private Long radOrderId;
    private Long lineNo;

    public RadOrderDetailKey() {}
    public RadOrderDetailKey(Long radOrderId, Long lineNo) {
        this.radOrderId = radOrderId;
        this.lineNo = lineNo;
    }

    public Long getRadOrderId() { return radOrderId; }
    public Long getLineNo() { return lineNo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RadOrderDetailKey that)) return false;
        return Objects.equals(radOrderId, that.radOrderId)
                && Objects.equals(lineNo, that.lineNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(radOrderId, lineNo);
    }
}
