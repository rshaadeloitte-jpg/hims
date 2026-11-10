package com.hims.rcm.domain.model;

import java.io.Serializable;
import java.util.Objects;

/** Composite key of t_nhcx_request_detail: UNIQUE(request_id, line_no). */
public class NhcxRequestDetailKey implements Serializable {

    private Long requestId;
    private Long lineNo;

    public NhcxRequestDetailKey() {}
    public NhcxRequestDetailKey(Long requestId, Long lineNo) {
        this.requestId = requestId;
        this.lineNo = lineNo;
    }

    public Long getRequestId() { return requestId; }
    public Long getLineNo() { return lineNo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NhcxRequestDetailKey that)) return false;
        return Objects.equals(requestId, that.requestId)
                && Objects.equals(lineNo, that.lineNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId, lineNo);
    }
}
