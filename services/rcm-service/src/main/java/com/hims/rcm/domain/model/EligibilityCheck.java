package com.hims.rcm.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Coverage-eligibility check (register WP-R4): patient + payer scheme, status
 * SMALLINT 0..3 (0=INITIATED 1=SENT 2=ACKNOWLEDGED 3=COMPLETED/ERROR — the
 * register's binding mapping of 06 §A9). Created with status 0; the response
 * callback pins status 2. The companion t_nhcx_request row (kind=1) carries the
 * correlation id used by simulated callbacks; both ids surface here transiently.
 */
@Entity
@Table(name = "t_eligibility_check")
public class EligibilityCheck extends RcmBase {

    @Id
    @Column(name = "eligibility_id")
    private Long eligibilityId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "payer_scheme", length = 60)
    private String payerScheme;

    @Column(name = "check_status", nullable = false)
    private Short statusCode = NhcxStatus.INITIATED;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_payload", nullable = false, columnDefinition = "jsonb")
    private String requestPayload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_payload", columnDefinition = "jsonb")
    private String responsePayload;

    /** Not persisted: surfaced from the companion t_nhcx_request row for API convenience. */
    @Transient
    private Long requestId;

    /** Not persisted: correlation id of the companion request (callback anchor). */
    @Transient
    private String correlationId;

    public Long getEligibilityId() { return eligibilityId; }
    public void setEligibilityId(Long eligibilityId) { this.eligibilityId = eligibilityId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getPayerScheme() { return payerScheme; }
    public void setPayerScheme(String payerScheme) { this.payerScheme = payerScheme; }
    public Short getStatusCode() { return statusCode; }
    public void setStatusCode(Short statusCode) { this.statusCode = statusCode; }
    public String getRequestPayload() { return requestPayload; }
    public void setRequestPayload(String requestPayload) { this.requestPayload = requestPayload; }
    public String getResponsePayload() { return responsePayload; }
    public void setResponsePayload(String responsePayload) { this.responsePayload = responsePayload; }
    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
}
