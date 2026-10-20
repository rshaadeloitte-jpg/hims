package com.hims.rcm.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Line-level detail of an NHCX request (register WP-R4): composite PK
 * UNIQUE(request_id, line_no), detail JSONB. One row per payload.lines entry
 * when the payload carries a lines array, else a single line_no=1 row holding
 * the whole payload (documented interpretation of the register's bare table spec).
 */
@Entity
@IdClass(NhcxRequestDetailKey.class)
@Table(name = "t_nhcx_request_detail")
public class NhcxRequestDetail extends RcmBase {

    @Id
    @Column(name = "request_id")
    private Long requestId;

    @Id
    @Column(name = "line_no")
    private Long lineNo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detail", nullable = false, columnDefinition = "jsonb")
    private String detail;

    /** Composite key has no single surrogate id (base contract satisfied structurally). */
    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }
    public Long getLineNo() { return lineNo; }
    public void setLineNo(Long lineNo) { this.lineNo = lineNo; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
