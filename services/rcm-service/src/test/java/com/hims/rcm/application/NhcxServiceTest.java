package com.hims.rcm.application;

import com.hims.rcm.application.exceptions.InvalidRcmRequestException;
import com.hims.rcm.application.exceptions.RcmNotFoundException;
import com.hims.rcm.domain.model.AdmissionTid;
import com.hims.rcm.domain.model.EligibilityCheck;
import com.hims.rcm.domain.model.NhcxRequestDetail;
import com.hims.rcm.domain.model.NhcxRequest;
import com.hims.rcm.domain.model.NhcxStatus;
import com.hims.rcm.domain.repository.AdmissionTidRepository;
import com.hims.rcm.domain.repository.EligibilityCheckRepository;
import com.hims.rcm.domain.repository.NhcxRequestDetailRepository;
import com.hims.rcm.domain.repository.NhcxRequestRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L0 — WP-R4 validation guards, NHCX status machine (0..3 forward-only, monotonic),
 * callback idempotency by correlation_id, and the DEFERRED-event policy.
 *
 * Event policy note (register WP-R4): ClaimSubmitted is DEFERRED per
 * KAFKA-EVENT-CATALOG §3 — NOTHING may reach the outbox. Pinned here structurally
 * ({@link #serviceLayerHasNoOutboxDependency_noCodePathCanEmit}) because neither
 * application service takes an {@code OutboxService}; pinned behaviorally at L2
 * (RcmServiceIntegrationTest asserts outbox count == 0 after every flow).
 */
class NhcxServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private EligibilityCheckRepository eligibilityRepository;
    private NhcxRequestRepository requestRepository;
    private NhcxRequestDetailRepository detailRepository;
    private NhcxService service;
    private final AtomicLong seq = new AtomicLong(100);

    @BeforeEach
    void setUp() {
        eligibilityRepository = mock(EligibilityCheckRepository.class);
        requestRepository = mock(NhcxRequestRepository.class);
        detailRepository = mock(NhcxRequestDetailRepository.class);
        EntityManager em = mock(EntityManager.class);
        service = new NhcxService(eligibilityRepository, requestRepository, detailRepository);
        ReflectionTestUtils.setField(service, "em", em);

        Query seqQuery = mock(Query.class);
        when(seqQuery.getSingleResult()).thenAnswer(inv -> seq.incrementAndGet());
        when(em.createNativeQuery(contains("nextval"))).thenReturn(seqQuery);

        when(eligibilityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Stateful request-repo mock: save remembers the row, findById serves what was saved,
        // mirroring real JPA semantics that the service's read-back (NhcxService.get) relies on.
        java.util.concurrent.atomic.AtomicReference<NhcxRequest> lastSaved =
                new java.util.concurrent.atomic.AtomicReference<>();
        when(requestRepository.save(any())).thenAnswer(inv -> {
            lastSaved.set(inv.getArgument(0));
            return inv.getArgument(0);
        });
        when(requestRepository.findById(anyLong())).thenAnswer(inv ->
                Optional.ofNullable(lastSaved.get())
                        .filter(r -> r.getRequestId().equals(((Number) inv.getArgument(0)).longValue())));

        // Stateful detail-repo mock: details accumulate per requestId, ordered by line_no.
        java.util.Map<Long, List<com.hims.rcm.domain.model.NhcxRequestDetail>> detailsById =
                new java.util.HashMap<>();
        when(detailRepository.save(any())).thenAnswer(inv -> {
            com.hims.rcm.domain.model.NhcxRequestDetail d = inv.getArgument(0);
            detailsById.computeIfAbsent(d.getRequestId(), k -> new java.util.ArrayList<>()).add(d);
            return d;
        });
        when(detailRepository.findByRequestIdOrderByLineNoAsc(anyLong())).thenAnswer(inv ->
                detailsById.getOrDefault(((Number) inv.getArgument(0)).longValue(), List.of()).stream()
                        .sorted(java.util.Comparator.comparing(
                                com.hims.rcm.domain.model.NhcxRequestDetail::getLineNo))
                        .toList());
    }

    // ------------------------------------------------------- creation guards

    @Test
    void eligibility_requiresPatientId_bexa003() {
        assertThatThrownBy(() -> service.createCoverageEligibility(null, "RGHS"))
                .isInstanceOf(InvalidRcmRequestException.class)
                .extracting(e -> ((InvalidRcmRequestException) e).getErrorCode())
                .isEqualTo("BEXA003");
    }

    @Test
    void eligibility_rejectsBlankOrOverlongPayerScheme_bexa003() {
        assertThatThrownBy(() -> service.createCoverageEligibility(11L, null))
                .isInstanceOf(InvalidRcmRequestException.class)
                .hasMessageContaining("payerScheme");
        assertThatThrownBy(() -> service.createCoverageEligibility(11L, "   "))
                .isInstanceOf(InvalidRcmRequestException.class)
                .hasMessageContaining("payerScheme");
        assertThatThrownBy(() -> service.createCoverageEligibility(11L, "x".repeat(NhcxService.MAX_PAYER_SCHEME_LENGTH + 1)))
                .isInstanceOf(InvalidRcmRequestException.class)
                .hasMessageContaining("60");
    }

    @Test
    void eligibility_create_pinsStatusInitiated_withCompanionCorrelatedRequest() throws Exception {
        ArgumentCaptor<NhcxRequest> requestCaptor = ArgumentCaptor.forClass(NhcxRequest.class);

        EligibilityCheck check = service.createCoverageEligibility(505L, "RGHS");

        assertThat(check.getStatusCode()).isEqualTo(NhcxStatus.INITIATED);          // status 0 per register
        assertThat(check.getRequestId()).isNotNull();
        assertThat(check.getCorrelationId()).isNotBlank();
        JsonNode payload = mapper.readTree(check.getRequestPayload());
        assertThat(payload.get("patientId").asText()).isEqualTo("505");
        assertThat(payload.get("payerScheme").asText()).isEqualTo("RGHS");

        verify(requestRepository).save(requestCaptor.capture());
        NhcxRequest companion = requestCaptor.getValue();
        assertThat(companion.getKind()).isEqualTo((short) NhcxRequest.KIND_COVERAGE_ELIGIBILITY);   // kind=1
        assertThat(companion.getStatusCode()).isEqualTo(NhcxStatus.INITIATED);
        assertThat(companion.getCorrelationId()).isEqualTo(check.getCorrelationId());
        assertThat(companion.getCorrelationId().length()).isLessThanOrEqualTo(64);
        JsonNode companionPayload = mapper.readTree(companion.getRequestPayload());
        assertThat(companionPayload.get("eligibilityId").asText())
                .isEqualTo(String.valueOf(check.getEligibilityId()));
    }

    // ------------------------------------------------------- eligibility response

    @Test
    void eligibilityResponse_unknownId_mapsTo404Code() {
        when(eligibilityRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.applyEligibilityResponse(404L, Map.of("ok", true)))
                .isInstanceOf(RcmNotFoundException.class)
                .extracting(e -> ((RcmNotFoundException) e).getErrorCode())
                .isEqualTo("NOT_FOUND");
    }

    @Test
    void eligibilityResponse_requiresPayload_bexa003() {
        assertThatThrownBy(() -> service.applyEligibilityResponse(1L, null))
                .isInstanceOf(InvalidRcmRequestException.class)
                .hasMessageContaining("payload");
        assertThatThrownBy(() -> service.applyEligibilityResponse(1L, Map.of()))
                .isInstanceOf(InvalidRcmRequestException.class)
                .hasMessageContaining("payload");
    }

    @Test
    void eligibilityResponse_fromInitiated_pinsAcknowledged() {
        EligibilityCheck stored = initiatedCheck(7L);
        when(eligibilityRepository.findById(7L)).thenReturn(Optional.of(stored));

        EligibilityCheck updated = service.applyEligibilityResponse(7L, Map.of("eligible", true));

        assertThat(updated.getStatusCode()).isEqualTo(NhcxStatus.ACKNOWLEDGED);      // status machine 0 -> 2
        assertThat(updated.getResponsePayload()).contains("eligible");
    }

    @Test
    void eligibilityResponse_secondResponse_rejected_rcmIllegalTransition() {
        EligibilityCheck stored = initiatedCheck(7L);
        stored.setStatusCode(NhcxStatus.ACKNOWLEDGED);
        when(eligibilityRepository.findById(7L)).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.applyEligibilityResponse(7L, Map.of("eligible", true)))
                .isInstanceOf(InvalidRcmRequestException.class)
                .extracting(e -> ((InvalidRcmRequestException) e).getErrorCode())
                .isEqualTo("RCM_ILLEGAL_TRANSITION");
    }

    // ------------------------------------------------------- pre-auth / claim intake

    @Test
    void preAuth_requiresPatientIdAndPayload_bexa003() {
        assertThatThrownBy(() -> service.createPreAuth(null, 8L, Map.of("a", 1)))
                .isInstanceOf(InvalidRcmRequestException.class)
                .hasMessageContaining("patientId");
        assertThatThrownBy(() -> service.createPreAuth(11L, 8L, Map.of()))
                .isInstanceOf(InvalidRcmRequestException.class)
                .hasMessageContaining("payload");
    }

    @Test
    void claim_requiresPatientIdBillIdAndPayload_bexa003() {
        assertThatThrownBy(() -> service.createClaim(null, 99L, Map.of("a", 1)))
                .isInstanceOf(InvalidRcmRequestException.class)
                .hasMessageContaining("patientId");
        assertThatThrownBy(() -> service.createClaim(11L, null, Map.of("a", 1)))
                .isInstanceOf(InvalidRcmRequestException.class)
                .hasMessageContaining("billId");
        assertThatThrownBy(() -> service.createClaim(11L, 99L, null))
                .isInstanceOf(InvalidRcmRequestException.class)
                .hasMessageContaining("payload");
    }

    @Test
    void claim_create_pinsKind3StatusInitiated_uniqueCorrelation_perRequest() {
        NhcxRequest first = service.createClaim(505L, 9001L, Map.of("claim", Map.of("amount", 100)));
        NhcxRequest second = service.createClaim(506L, 9002L, Map.of("claim", Map.of("amount", 200)));

        assertThat(first.getKind()).isEqualTo((short) NhcxRequest.KIND_CLAIM_SUBMIT);
        assertThat(first.getStatusCode()).isEqualTo(NhcxStatus.INITIATED);
        assertThat(first.getBillId()).isEqualTo(9001L);
        assertThat(first.getCorrelationId()).isNotEqualTo(second.getCorrelationId());   // UNIQUE per register
        assertThat(first.getDetails()).hasSize(1);                                      // no lines array -> single detail row
    }

    @Test
    void preAuth_create_splitsLinesIntoDetailRows_whenPayloadCarriesLines() {
        NhcxRequest preAuth = service.createPreAuth(505L, 8L,
                Map.of("lines", List.of(Map.of("item", 1), Map.of("item", 2))));

        assertThat(preAuth.getKind()).isEqualTo((short) NhcxRequest.KIND_PRE_AUTH);
        assertThat(preAuth.getDetails())
                .extracting(NhcxRequestDetail::getLineNo)
                .containsExactly(1L, 2L);
    }

    // ------------------------------------------------------------------- callback

    @Test
    void callback_unknownCorrelation_mapsTo404Envelope() {
        when(requestRepository.findByCorrelationId("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.callback("nope", Map.of("ok", true), (short) 2))
                .isInstanceOf(RcmNotFoundException.class)
                .hasMessageContaining("Unknown correlation")
                .extracting(e -> ((RcmNotFoundException) e).getErrorCode())
                .isEqualTo("NOT_FOUND");
    }

    @Test
    void callback_invalidStatus_bexa003() {
        assertThatThrownBy(() -> service.callback("c1", Map.of(), null))
                .isInstanceOf(InvalidRcmRequestException.class)
                .hasMessageContaining("status must be");
        assertThatThrownBy(() -> service.callback("c1", Map.of(), (short) -1))
                .isInstanceOf(InvalidRcmRequestException.class)
                .hasMessageContaining("status must be");
        assertThatThrownBy(() -> service.callback("c1", Map.of(), (short) 4))
                .isInstanceOf(InvalidRcmRequestException.class)
                .hasMessageContaining("status must be");
    }

    @Test
    void callback_blankOrOverlongCorrelation_bexa003() {
        assertThatThrownBy(() -> service.callback("", Map.of(), (short) 1))
                .isInstanceOf(InvalidRcmRequestException.class)
                .hasMessageContaining("correlationId");
        assertThatThrownBy(() -> service.callback("x".repeat(65), Map.of(), (short) 1))
                .isInstanceOf(InvalidRcmRequestException.class)
                .hasMessageContaining("correlationId");
    }

    @Test
    void callback_forwardTransitions_advanceStatus_machine0to2to3() {
        NhcxRequest stored = correlatedRequest("corr-1", NhcxStatus.INITIATED);
        when(requestRepository.findByCorrelationId("corr-1")).thenReturn(Optional.of(stored));
        when(requestRepository.findById(stored.getRequestId())).thenReturn(Optional.of(stored));

        NhcxRequest ack = service.callback("corr-1", Map.of("ack", true), (short) NhcxStatus.ACKNOWLEDGED);
        assertThat(ack.getStatusCode()).isEqualTo(NhcxStatus.ACKNOWLEDGED);          // 0 -> 2

        NhcxRequest done = service.callback("corr-1", Map.of("done", true), (short) NhcxStatus.COMPLETED);
        assertThat(done.getStatusCode()).isEqualTo(NhcxStatus.COMPLETED);      // 2 -> 3
        assertThat(done.getResponsePayload()).contains("done");
    }

    @Test
    void callback_sameStatus_isIdempotentNoOpSuccess() {
        NhcxRequest stored = correlatedRequest("corr-2", NhcxStatus.SENT);
        when(requestRepository.findByCorrelationId("corr-2")).thenReturn(Optional.of(stored));
        when(requestRepository.findById(stored.getRequestId())).thenReturn(Optional.of(stored));

        NhcxRequest result = service.callback("corr-2", Map.of("late", "duplicate"), (short) NhcxStatus.SENT);

        assertThat(result.getStatusCode()).isEqualTo(NhcxStatus.SENT);
        assertThat(result.getResponsePayload()).isNull();     // duplicate did NOT overwrite state
    }

    @Test
    void callback_terminalCompleted_acceptsOnlyIdenticalRepeats() {
        NhcxRequest stored = correlatedRequest("corr-3", NhcxStatus.COMPLETED);
        when(requestRepository.findByCorrelationId("corr-3")).thenReturn(Optional.of(stored));
        when(requestRepository.findById(stored.getRequestId())).thenReturn(Optional.of(stored));

        assertThat(service.callback("corr-3", Map.of(), (short) NhcxStatus.COMPLETED).getStatusCode())
                .isEqualTo(NhcxStatus.COMPLETED);   // idempotent repeat OK
        assertThatThrownBy(() -> service.callback("corr-3", Map.of(), (short) NhcxStatus.ACKNOWLEDGED))
                .isInstanceOf(InvalidRcmRequestException.class)
                .extracting(e -> ((InvalidRcmRequestException) e).getErrorCode())
                .isEqualTo("RCM_ILLEGAL_TRANSITION");   // any different move from terminal is illegal
    }

    @Test
    void callback_backwardTransition_rejected_rcmIllegalTransition() {
        NhcxRequest stored = correlatedRequest("corr-4", NhcxStatus.ACKNOWLEDGED);
        when(requestRepository.findByCorrelationId("corr-4")).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.callback("corr-4", Map.of(), (short) NhcxStatus.INITIATED))
                .isInstanceOf(InvalidRcmRequestException.class)
                .extracting(e -> ((InvalidRcmRequestException) e).getErrorCode())
                .isEqualTo("RCM_ILLEGAL_TRANSITION");

        NhcxRequest sent = correlatedRequest("corr-5", NhcxStatus.SENT);
        when(requestRepository.findByCorrelationId("corr-5")).thenReturn(Optional.of(sent));
        assertThatThrownBy(() -> service.callback("corr-5", Map.of(), (short) NhcxStatus.INITIATED))
                .isInstanceOf(InvalidRcmRequestException.class)
                .hasMessageContaining("back to INITIATED");
    }

    // ------------------------------------------------------------- filter parsers

    @Test
    void statusFilter_parsesNumericAndNames_rejectsUnknown() {
        assertThat(NhcxService.parseStatus("0")).isEqualTo((short) 0);
        assertThat(NhcxService.parseStatus("acknowledged")).isEqualTo((short) NhcxStatus.ACKNOWLEDGED);
        assertThat(NhcxService.parseStatus("COMPLETED")).isEqualTo((short) NhcxStatus.COMPLETED);
        assertThatThrownBy(() -> NhcxService.parseStatus("9"))
                .isInstanceOf(InvalidRcmRequestException.class);
        assertThatThrownBy(() -> NhcxService.parseStatus("WEIRD"))
                .isInstanceOf(InvalidRcmRequestException.class);
    }

    @Test
    void kindFilter_parsesNumericAndNames_rejectsUnknown() {
        assertThat(NhcxService.parseKind("2")).isEqualTo((short) NhcxRequest.KIND_PRE_AUTH);
        assertThat(NhcxService.parseKind("claim_submit")).isEqualTo((short) NhcxRequest.KIND_CLAIM_SUBMIT);
        assertThat(NhcxService.parseKind("COVERAGE_ELIGIBILITY"))
                .isEqualTo((short) NhcxRequest.KIND_COVERAGE_ELIGIBILITY);
        assertThatThrownBy(() -> NhcxService.parseKind("0"))
                .isInstanceOf(InvalidRcmRequestException.class);
    }

    // ------------------------------------------------------------- event policy

    /**
     * Register WP-R4 negative event assertion (structural half): ClaimSubmitted is
     * DEFERRED per KAFKA-EVENT-CATALOG §3 — the application services must not even be
     * WIRABLE to the outbox. The behavioral half lives in RcmServiceIntegrationTest.
     */
    @Test
    void serviceLayerHasNoOutboxDependency_noCodePathCanEmit() {
        for (Class<?> serviceClass : List.of(NhcxService.class, TidService.class)) {
            boolean wiredToOutbox = java.util.Arrays.stream(serviceClass.getConstructors())
                    .flatMap(c -> java.util.Arrays.stream(c.getParameterTypes()))
                    .anyMatch(t -> t.getSimpleName().startsWith("Outbox"));
            assertThat(wiredToOutbox)
                    .as("%s must have no outbox dependency — events are DEFERRED this tranche", serviceClass.getSimpleName())
                    .isFalse();
        }
    }

    // -------------------------------------------------------------------- helpers

    private EligibilityCheck initiatedCheck(long id) {
        EligibilityCheck check = new EligibilityCheck();
        check.setEligibilityId(id);
        check.setPatientId(505L);
        check.setPayerScheme("RGHS");
        check.setStatusCode(NhcxStatus.INITIATED);
        check.setRequestPayload("{\"patientId\":505}");
        return check;
    }

    private NhcxRequest correlatedRequest(String correlationId, short status) {
        NhcxRequest request = new NhcxRequest();
        request.setRequestId(seq.incrementAndGet());
        request.setKind(NhcxRequest.KIND_PRE_AUTH);
        request.setPatientId(505L);
        request.setStatusCode(status);
        request.setCorrelationId(correlationId);
        request.setRequestPayload("{\"kind\":\"PRE_AUTH\"}");
        return request;
    }
}
