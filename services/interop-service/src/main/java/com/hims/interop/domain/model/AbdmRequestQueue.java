package com.hims.interop.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ABDM gateway request row (register WP-T1). correlation_id is UNIQUE — the
 * durable replacement for the legacy Redis correlation store (ABDM_REQ_&lt;reqId&gt;,
 * 07 §ABDM); duplicate correlationId -> 409 at the API and DB level. Callbacks
 * are IDEMPOTENT: first callback wins, repeats are no-ops. Gateway dispatch
 * itself is OUT OF SCOPE (comment-only).
 *
 * Status SMALLINT 0..3 per 06 §A9 NHCX request-status enums (0=ACCEPTED ..
 * 3=FAILED); intermediate names are service labels for the documented numeric
 * range — endpoints 0 and 3 are catalog-evidenced.
 */
@Entity
@Table(name = "t_abdm_request_queue")
public class AbdmRequestQueue extends InteropBase {

    /** ABDM Milestone 1 (ABHA create/verify/link — 07 §ABDM). */
    public static final String KIND_ABHA = "ABHA";
    /** ABDM Milestone 2 (Health Information Provider — push). */
    public static final String KIND_HIP = "HIP";
    /** ABDM Milestone 3 (Health Information User — pull). */
    public static final String KIND_HIU = "HIU";
    /** Health Facility Registry (M0 — facility onboarding). */
    public static final String KIND_HFR = "HFR";
    /** Consent Management (subset of M2/M3). */
    public static final String KIND_CM = "CM";

    /** ABDM Milestone tags — see 07 §ABDM. */
    public static final String MILESTONE_M0 = "M0";   // facility registration
    public static final String MILESTONE_M1 = "M1";   // ABHA
    public static final String MILESTONE_M2 = "M2";   // HIP
    public static final String MILESTONE_M3 = "M3";   // HIU

    /** Map a request_kind to its ABDM milestone tag (single source of truth). */
    public static String milestoneOf(String kind) {
        return switch (kind) {
            case KIND_ABHA -> MILESTONE_M1;
            case KIND_HIP, KIND_CM -> MILESTONE_M2;
            case KIND_HIU -> MILESTONE_M3;
            case KIND_HFR -> MILESTONE_M0;
            default -> throw new IllegalArgumentException("Unknown ABDM request_kind: " + kind);
        };
    }

    /** NHCX status range per 06 §A9: 0=ACCEPTED .. 3=FAILED. */
    public static final short STATUS_ACCEPTED = 0;
    public static final short STATUS_IN_PROGRESS = 1;
    public static final short STATUS_COMPLETED = 2;
    public static final short STATUS_FAILED = 3;

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "request_kind", nullable = false, length = 30)
    private String requestKind;

    @Column(name = "correlation_id", nullable = false, unique = true, length = 64)
    private String correlationId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private String payload;

    @Column(name = "status", nullable = false)
    private Short status = STATUS_ACCEPTED;

    /** First-callback-wins: null until the first accepted callback lands. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "callback_payload", columnDefinition = "jsonb")
    private String callbackPayload;

    /** ABDM milestone tag (M0/M1/M2/M3) — see {@link #milestoneOf}. */
    @Column(name = "milestone", nullable = false, length = 4)
    private String milestone = MILESTONE_M2;

    /** Outbox envelope eventId for producer-side dedup (nullable; UNIQUE in DB). */
    @Column(name = "envelope_event_id", length = 64)
    private String envelopeEventId;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public String getRequestKind() { return requestKind; }
    public void setRequestKind(String requestKind) { this.requestKind = requestKind; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public Short getStatus() { return status; }
    public void setStatus(Short status) { this.status = status; }
    public String getCallbackPayload() { return callbackPayload; }
    public void setCallbackPayload(String callbackPayload) { this.callbackPayload = callbackPayload; }

    /** True once ANY callback has been applied (idempotency marker). */
    public boolean hasCallback() { return callbackPayload != null; }

    public String getMilestone() { return milestone; }
    public void setMilestone(String milestone) { this.milestone = milestone; }
    public String getEnvelopeEventId() { return envelopeEventId; }
    public void setEnvelopeEventId(String envelopeEventId) { this.envelopeEventId = envelopeEventId; }
}
