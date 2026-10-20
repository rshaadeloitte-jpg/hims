package com.hims.rcm.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Persisted NHCX exchange request (register WP-R4): kind SMALLINT
 * 1=COVERAGE_ELIGIBILITY 2=PRE_AUTH 3=CLAIM_SUBMIT, status 0..3 DEFAULT 0,
 * correlation_id VARCHAR(64) UNIQUE — the callback idempotency anchor.
 * External HCX/NHCX gateway calls are OUT OF SCOPE this tranche: requests are
 * persisted and callbacks simulated locally against this row.
 */
@Entity
@Table(name = "t_nhcx_request")
public class NhcxRequest extends RcmBase {

    public static final short KIND_COVERAGE_ELIGIBILITY = 1;
    public static final short KIND_PRE_AUTH = 2;
    public static final short KIND_CLAIM_SUBMIT = 3;

    @Id
    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "kind", nullable = false)
    private Short kind;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "bill_id")
    private Long billId;

    @Column(name = "admission_id")
    private Long admissionId;

    @Column(name = "request_status", nullable = false)
    private Short statusCode = NhcxStatus.INITIATED;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_payload", nullable = false, columnDefinition = "jsonb")
    private String requestPayload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_payload", columnDefinition = "jsonb")
    private String responsePayload;

    @Column(name = "correlation_id", nullable = false, unique = true, length = 64)
    private String correlationId;

    /** Not persisted: details loaded via repository in the service layer. */
    @Transient
    private List<NhcxRequestDetail> details = new ArrayList<>();

    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }
    public Short getKind() { return kind; }
    public void setKind(Short kind) { this.kind = kind; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }
    public Long getAdmissionId() { return admissionId; }
    public void setAdmissionId(Long admissionId) { this.admissionId = admissionId; }
    public Short getStatusCode() { return statusCode; }
    public void setStatusCode(Short statusCode) { this.statusCode = statusCode; }
    public String getRequestPayload() { return requestPayload; }
    public void setRequestPayload(String requestPayload) { this.requestPayload = requestPayload; }
    public String getResponsePayload() { return responsePayload; }
    public void setResponsePayload(String responsePayload) { this.responsePayload = responsePayload; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public List<NhcxRequestDetail> getDetails() { return details; }
    public void setDetails(List<NhcxRequestDetail> details) { this.details = details; }
}
