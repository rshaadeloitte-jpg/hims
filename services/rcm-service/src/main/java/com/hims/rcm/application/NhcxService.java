package com.hims.rcm.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.rcm.application.exceptions.InvalidRcmRequestException;
import com.hims.rcm.application.exceptions.RcmNotFoundException;
import com.hims.rcm.domain.model.EligibilityCheck;
import com.hims.rcm.domain.model.NhcxRequest;
import com.hims.rcm.domain.model.NhcxRequestDetail;
import com.hims.rcm.domain.model.NhcxStatus;
import com.hims.rcm.domain.repository.EligibilityCheckRepository;
import com.hims.rcm.domain.repository.NhcxRequestDetailRepository;
import com.hims.rcm.domain.repository.NhcxRequestRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * RCM &amp; Claims slice (GAP-REGISTER-5 WP-R4, Catalog #23). NEW service replacing the
 * NHCX fragment of the legacy abdm/xcg-hcx cluster (NHCXServiceImpl + xcg/hcx helpers).
 *
 * SCOPE (register WP-R4): HCX/NHCX EXTERNAL gateway calls are OUT OF SCOPE this
 * tranche — requests are persisted with correlation ids and callbacks are simulated
 * locally (contract-level; disclosed). ClaimSubmitted is DEFERRED per
 * KAFKA-EVENT-CATALOG §3: NOTHING is emitted to the outbox (negative-tested at L0+L2).
 * InvoiceGenerated/PaymentReceived consumption lands post-saga — comment-only.
 *
 * Status machine (SMALLINT 0..3 per register mapping of 06 §A9):
 *   0=INITIATED -> (callback) -> 1=SENT -> 2=ACKNOWLEDGED -> 3=COMPLETED/ERROR.
 *   Forward-only + monotonic; re-applying the CURRENT status via callback is an
 *   idempotent no-op success (correlation_id anchor); backward/different-after-terminal
 *   transitions are 400 RCM_ILLEGAL_TRANSITION. Unknown correlation_id -> 404 envelope.
 *   The eligibility response endpoint pins status directly to 2 (guard: still INITIATED).
 */
@Service
public class NhcxService {

    private static final Logger log = LoggerFactory.getLogger(NhcxService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    public static final int MAX_PAYER_SCHEME_LENGTH = 60;
    public static final int MAX_CORRELATION_LENGTH = 64;
    public static final int MAX_TID_LENGTH = 60;

    private final EligibilityCheckRepository eligibilityRepository;
    private final NhcxRequestRepository requestRepository;
    private final NhcxRequestDetailRepository detailRepository;

    @PersistenceContext
    private EntityManager em;

    private final ObjectMapper mapper = new ObjectMapper();

    public NhcxService(EligibilityCheckRepository eligibilityRepository,
                       NhcxRequestRepository requestRepository,
                       NhcxRequestDetailRepository detailRepository) {
        this.eligibilityRepository = eligibilityRepository;
        this.requestRepository = requestRepository;
        this.detailRepository = detailRepository;
    }

    // ------------------------------------------------------- coverage eligibility

    /**
     * POST /api/v1/coverage-eligibility {patientId, payerScheme} — creates the check
     * with status 0 AND the companion t_nhcx_request row (kind=1) carrying a fresh
     * correlation id. Guards (400 BEXA003): patientId required; payerScheme required,
     * max 60 chars. Emits NOTHING (ClaimSubmitted deferred — no event exists here).
     */
    @Transactional
    public EligibilityCheck createCoverageEligibility(Long patientId, String payerScheme) {
        if (patientId == null) {
            throw new InvalidRcmRequestException("BEXA003", "patientId is required");
        }
        if (payerScheme == null || payerScheme.isBlank()) {
            throw new InvalidRcmRequestException("BEXA003", "payerScheme is required");
        }
        if (payerScheme.length() > MAX_PAYER_SCHEME_LENGTH) {
            throw new InvalidRcmRequestException("BEXA003",
                    "payerScheme must be at most " + MAX_PAYER_SCHEME_LENGTH + " characters");
        }

        Timestamp nowTs = now();

        EligibilityCheck check = new EligibilityCheck();
        check.setEligibilityId(nextId("t_eligibility_check_seq"));
        check.setPatientId(patientId);
        check.setPayerScheme(payerScheme);
        check.setStatusCode(NhcxStatus.INITIATED);

        // Companion t_nhcx_request row (kind=1) carries the correlation id; its
        // request_payload echoes the check for traceability (eligibilityId key).
        Map<String, Object> requestMap = new LinkedHashMap<>();
        requestMap.put("patientId", patientId);
        requestMap.put("payerScheme", payerScheme);
        requestMap.put("eligibilityId", check.getEligibilityId());
        String requestJson = toJson(requestMap);

        check.setRequestPayload(toJson(Map.of("patientId", patientId, "payerScheme", payerScheme)));
        stampCreated(check, nowTs);
        eligibilityRepository.save(check);

        NhcxRequest request = newRequest(NhcxRequest.KIND_COVERAGE_ELIGIBILITY, patientId,
                null, null, requestJson, nowTs);
        check.setRequestId(request.getRequestId());
        check.setCorrelationId(request.getCorrelationId());

        log.info("Created coverage-eligibility check {} for patient {} (correlation {})",
                check.getEligibilityId(), patientId, request.getCorrelationId());
        return check;
    }

    /**
     * PUT /api/v1/coverage-eligibility/{id}/response {payload} — records the simulated
     * payor response and pins status 2=ACKNOWLEDGED. Guard: only from status 0
     * (INITIATED); a second response is rejected 400 RCM_ILLEGAL_TRANSITION.
     */
    @Transactional
    public EligibilityCheck applyEligibilityResponse(long eligibilityId, Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            throw new InvalidRcmRequestException("BEXA003", "payload is required");
        }
        EligibilityCheck check = eligibilityRepository.findById(eligibilityId)
                .orElseThrow(() -> new RcmNotFoundException("NOT_FOUND",
                        "Eligibility check not found: " + eligibilityId));
        if (check.getStatusCode() != NhcxStatus.INITIATED) {
            throw new InvalidRcmRequestException("RCM_ILLEGAL_TRANSITION",
                    "Eligibility check " + eligibilityId + " already has a response (status "
                            + NhcxStatus.name(check.getStatusCode()) + ") — only INITIATED checks accept one");
        }
        check.setResponsePayload(toJson(payload));
        check.setStatusCode(NhcxStatus.ACKNOWLEDGED);
        stampUpdated(check, now());
        eligibilityRepository.save(check);
        log.info("Recorded eligibility response for check {} -> ACKNOWLEDGED", eligibilityId);
        return check;
    }

    // ------------------------------------------------------------ pre-auth / claim

    /**
     * POST /api/v1/pre-auths {patientId, admissionId?, payload} — persists a PRE_AUTH
     * request (kind=2, status 0) with a fresh correlation id. Guards (400 BEXA003):
     * patientId required; payload required.
     */
    @Transactional
    public NhcxRequest createPreAuth(Long patientId, Long admissionId, Map<String, Object> payload) {
        return createRequest(NhcxRequest.KIND_PRE_AUTH, patientId, null, admissionId, payload, "pre-auth");
    }

    /**
     * POST /api/v1/claims {patientId, billId, payload} — persists a CLAIM_SUBMIT record
     * (kind=3, status 0) with a fresh correlation id. Guards (400 BEXA003):
     * patientId required; billId required; payload required. Emits NOTHING:
     * ClaimSubmitted is DEFERRED per KAFKA-EVENT-CATALOG §3.
     */
    @Transactional
    public NhcxRequest createClaim(Long patientId, Long billId, Map<String, Object> payload) {
        return createRequest(NhcxRequest.KIND_CLAIM_SUBMIT, patientId, billId, null, payload, "claim");
    }

    private NhcxRequest createRequest(short kind, Long patientId, Long billId, Long admissionId,
                                      Map<String, Object> payload, String label) {
        if (patientId == null) {
            throw new InvalidRcmRequestException("BEXA003", "patientId is required");
        }
        if (billId == null && NhcxRequest.KIND_CLAIM_SUBMIT == kind) {
            throw new InvalidRcmRequestException("BEXA003", "billId is required for claim submission");
        }
        if (payload == null || payload.isEmpty()) {
            throw new InvalidRcmRequestException("BEXA003", "payload is required");
        }
        Timestamp nowTs = now();
        NhcxRequest request = newRequest(kind, patientId, billId, admissionId, toJson(payload), nowTs);

        saveDetails(request, payload, nowTs);
        log.info("Persisted {} NHCX request {} for patient {} (correlation {})",
                label, request.getRequestId(), patientId, request.getCorrelationId());
        return get(request.getRequestId());
    }

    /** Details: one row per payload.lines entry (line_no 1..n), else single full-payload row. */
    private void saveDetails(NhcxRequest request, Map<String, Object> payload, Timestamp nowTs) {
        Object lines = payload.get("lines");
        long lineNo = 1L;
        if (lines instanceof List<?> list && !list.isEmpty()) {
            for (Object line : list) {
                saveDetail(request, lineNo++, toJson(line), nowTs);
            }
        } else {
            saveDetail(request, lineNo, request.getRequestPayload(), nowTs);
        }
    }

    private void saveDetail(NhcxRequest request, long lineNo, String json, Timestamp nowTs) {
        NhcxRequestDetail d = new NhcxRequestDetail();
        d.setRequestId(request.getRequestId());
        d.setLineNo(lineNo);
        d.setDetail(json);
        stampCreated(d, nowTs);
        detailRepository.save(d);
    }

    // ------------------------------------------------------------------- callback

    /**
     * PUT /api/v1/nhcx/{correlationId}/callback {payload,status} — simulated payor/gateway
     * callback, IDEMPOTENT by correlation_id (unique column):
     *   unknown correlation_id      -> 404 NOT_FOUND envelope
     *   status null/outside 0..3    -> 400 BEXA003
     *   status == current           -> idempotent no-op success (state unchanged)
     *   forward transition          -> response_payload updated, status advanced
     *   backward/illegal transition -> 400 RCM_ILLEGAL_TRANSITION
     */
    @Transactional
    public NhcxRequest callback(String correlationId, Map<String, Object> payload, Short status) {
        if (correlationId == null || correlationId.isBlank()
                || correlationId.length() > MAX_CORRELATION_LENGTH) {
            throw new InvalidRcmRequestException("BEXA003",
                    "correlationId must be a non-blank id of at most " + MAX_CORRELATION_LENGTH + " characters");
        }
        if (status == null || !NhcxStatus.isValid(status)) {
            throw new InvalidRcmRequestException("BEXA003",
                    "status must be one of 0=INITIATED 1=SENT 2=ACKNOWLEDGED 3=COMPLETED");
        }
        NhcxRequest request = requestRepository.findByCorrelationId(correlationId)
                .orElseThrow(() -> new RcmNotFoundException("NOT_FOUND",
                        "Unknown correlation id: " + correlationId));

        short current = request.getStatusCode();
        if (status == current) {
            log.info("Idempotent callback for correlation {} — status already {}", correlationId, NhcxStatus.name(current));
            return get(request.getRequestId());   // no state change
        }
        if (!NhcxStatus.canTransition(current, status)) {
            throw new InvalidRcmRequestException("RCM_ILLEGAL_TRANSITION",
                    "Cannot move request " + request.getRequestId() + " from "
                            + NhcxStatus.name(current) + " back to " + NhcxStatus.name(status));
        }
        if (payload != null && !payload.isEmpty()) {
            request.setResponsePayload(toJson(payload));
        }
        request.setStatusCode(status);
        stampUpdated(request, now());
        requestRepository.save(request);
        log.info("Callback advanced request {} {} -> {}", request.getRequestId(),
                NhcxStatus.name(current), NhcxStatus.name(status));
        return get(request.getRequestId());
    }

    // -------------------------------------------------------------------- queries

    @Transactional(readOnly = true)
    public NhcxRequest get(long requestId) {
        NhcxRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RcmNotFoundException("NOT_FOUND",
                        "NHCX request not found: " + requestId));
        request.setDetails(detailsOf(requestId));
        return request;
    }

    @Transactional(readOnly = true)
    public List<NhcxRequest> list(Short kind, Short status) {
        List<NhcxRequest> requests = requestRepository.search(kind, status);
        for (NhcxRequest r : requests) {
            r.setDetails(detailsOf(r.getRequestId()));
        }
        return requests;
    }

    // -------------------------------------------------------------------- helpers

    private NhcxRequest newRequest(short kind, Long patientId, Long billId, Long admissionId,
                                   String requestJson, Timestamp nowTs) {
        NhcxRequest request = new NhcxRequest();
        request.setRequestId(nextId("t_nhcx_request_seq"));
        request.setKind(kind);
        request.setPatientId(patientId);
        request.setBillId(billId);
        request.setAdmissionId(admissionId);
        request.setStatusCode(NhcxStatus.INITIATED);
        request.setRequestPayload(requestJson);
        request.setCorrelationId(UUID.randomUUID().toString());   // fits VARCHAR(64)
        stampCreated(request, nowTs);
        return requestRepository.save(request);
    }

    private List<NhcxRequestDetail> detailsOf(long requestId) {
        return detailRepository.findByRequestIdOrderByLineNoAsc(requestId);
    }

    /** Status filter parser: numeric (0..3) or name (INITIATED, SENT, ACKNOWLEDGED, COMPLETED). */
    public static Short parseStatus(String raw) {
        try {
            short s = Short.parseShort(raw.trim());
            if (!NhcxStatus.isValid(s)) {
                throw new InvalidRcmRequestException("BEXA003",
                        "Unknown status: " + raw + " (valid 0..3)");
            }
            return s;
        } catch (NumberFormatException ignored) {
            // fall through to name mapping
        }
        return switch (raw.trim().toUpperCase()) {
            case "INITIATED" -> NhcxStatus.INITIATED;
            case "SENT" -> NhcxStatus.SENT;
            case "ACKNOWLEDGED" -> NhcxStatus.ACKNOWLEDGED;
            case "COMPLETED" -> NhcxStatus.COMPLETED;
            default -> throw new InvalidRcmRequestException("BEXA003", "Unknown status: " + raw);
        };
    }

    /** Kind filter parser: numeric (1..3) or name (COVERAGE_ELIGIBILITY, PRE_AUTH, CLAIM_SUBMIT). */
    public static Short parseKind(String raw) {
        try {
            short k = Short.parseShort(raw.trim());
            if (k < NhcxRequest.KIND_COVERAGE_ELIGIBILITY || k > NhcxRequest.KIND_CLAIM_SUBMIT) {
                throw new InvalidRcmRequestException("BEXA003",
                        "Unknown kind: " + raw + " (valid 1..3)");
            }
            return k;
        } catch (NumberFormatException ignored) {
            // fall through to name mapping
        }
        return switch (raw.trim().toUpperCase()) {
            case "COVERAGE_ELIGIBILITY" -> NhcxRequest.KIND_COVERAGE_ELIGIBILITY;
            case "PRE_AUTH" -> NhcxRequest.KIND_PRE_AUTH;
            case "CLAIM_SUBMIT" -> NhcxRequest.KIND_CLAIM_SUBMIT;
            default -> throw new InvalidRcmRequestException("BEXA003", "Unknown kind: " + raw);
        };
    }

    private void stampCreated(com.hims.rcm.domain.model.RcmBase entity, Timestamp nowTs) {
        entity.setStatus(true);
        entity.setSynchronizedd(false);
        entity.setAddedBy(AUDIT_ACTOR_ID);
        entity.setAddedDatetime(nowTs);
    }

    private void stampUpdated(com.hims.rcm.domain.model.RcmBase entity, Timestamp nowTs) {
        entity.setUpdatedBy(AUDIT_ACTOR_ID);
        entity.setUpdatedDatetime(nowTs);
    }

    private String toJson(Object payload) {
        try {
            return mapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize JSON payload", e);
        }
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_rcm." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }
}
