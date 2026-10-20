package com.hims.rcm.infrastructure.persistence;

import com.hims.rcm.RcmServiceApplication;
import com.hims.rcm.application.NhcxService;
import com.hims.rcm.application.TidService;
import com.hims.rcm.application.exceptions.InvalidRcmRequestException;
import com.hims.rcm.application.exceptions.RcmNotFoundException;
import com.hims.rcm.application.exceptions.TidConflictException;
import com.hims.rcm.domain.model.AdmissionTid;
import com.hims.rcm.domain.model.EligibilityCheck;
import com.hims.rcm.domain.model.NhcxRequest;
import com.hims.rcm.domain.model.NhcxStatus;
import com.hims.rcm.domain.repository.AdmissionTidRepository;
import com.hims.rcm.domain.repository.EligibilityCheckRepository;
import com.hims.rcm.domain.repository.NhcxRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * L2 — WP-R4 RCM &amp; Claims on real PG16: V1 schema applies (ihms_rcm: 4 domain tables
 * + outbox + 4 sequences), the full eligibility / pre-auth / claim / callback / TID
 * flows behave per register WP-R4, and — the explicit NEGATIVE event assertion —
 * the transactional outbox stays EMPTY after every flow because ClaimSubmitted is
 * DEFERRED per KAFKA-EVENT-CATALOG §3 (nothing in this service emits events).
 * NOTE: requires Docker; skipped automatically when Docker is absent
 * (@Testcontainers(disabledWithoutDocker = true)) — do not remove.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = RcmServiceApplication.class)
@DirtiesContext
@Transactional
class RcmServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private NhcxService nhcxService;

    @Autowired
    private TidService tidService;

    @Autowired
    private EligibilityCheckRepository eligibilityRepository;

    @Autowired
    private NhcxRequestRepository requestRepository;

    @Autowired
    private AdmissionTidRepository tidRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private com.hims.rcm.infrastructure.outbox.OutboxRepository outboxRepository;

    @Test
    void flywayAppliesIhmsRcmSchema_withAllRegisterTables() {
        List<String> tables = jdbc.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'ihms_rcm' ORDER BY table_name
                """, String.class);
        assertThat(tables).contains("t_eligibility_check", "t_admission_tid",
                "t_nhcx_request", "t_nhcx_request_detail", "outbox");

        List<String> sequences = jdbc.queryForList("""
                SELECT sequence_name FROM information_schema.sequences
                WHERE sequence_schema = 'ihms_rcm' ORDER BY sequence_name
                """, String.class);
        assertThat(sequences).contains("t_eligibility_check_seq", "t_admission_tid_seq",
                "t_nhcx_request_seq", "t_nhcx_request_detail_seq");
    }

    @Test
    void fullRcmFlow_withOutboxStayingEmpty_negativeEventAssertion() {
        // ---- NEGATIVE precondition: DEFERRED-event policy -> outbox starts empty
        assertThat(outboxRepository.count()).isZero();

        // ---- guards before anything is written --------------------------------
        assertThatThrownBy(() -> nhcxService.createCoverageEligibility(null, "RGHS"))
                .isInstanceOf(InvalidRcmRequestException.class)
                .extracting(e -> ((InvalidRcmRequestException) e).getErrorCode())
                .isEqualTo("BEXA003");
        assertThatThrownBy(() -> nhcxService.createClaim(11L, null, Map.of("a", 1)))
                .isInstanceOf(InvalidRcmRequestException.class)
                .hasMessageContaining("billId");
        assertThat(outboxRepository.count()).isZero();   // rejected creates emit nothing

        // ---- coverage eligibility: check status 0 + companion kind=1 request --
        EligibilityCheck check = nhcxService.createCoverageEligibility(505L, "RGHS");
        long checkId = check.getEligibilityId();

        assertThat(check.getStatusCode()).isEqualTo(NhcxStatus.INITIATED);
        assertThat(check.getCorrelationId()).isNotBlank();
        assertThat(requestRepository.findByCorrelationId(check.getCorrelationId()))
                .hasValueSatisfying(r -> {
                    assertThat(r.getKind()).isEqualTo((short) NhcxRequest.KIND_COVERAGE_ELIGIBILITY);
                    assertThat(r.getStatusCode()).isEqualTo(NhcxStatus.INITIATED);
                });

        // response pins status 2=ACKNOWLEDGED (status machine 0 -> 2)
        EligibilityCheck acked = nhcxService.applyEligibilityResponse(checkId,
                Map.of("eligible", true, "plan", "RGHS-2026"));
        assertThat(acked.getStatusCode()).isEqualTo(NhcxStatus.ACKNOWLEDGED);
        assertThat(acked.getResponsePayload()).contains("RGHS-2026");

        // second response rejected (RCM_ILLEGAL_TRANSITION)
        assertThatThrownBy(() -> nhcxService.applyEligibilityResponse(checkId, Map.of("again", true)))
                .isInstanceOf(InvalidRcmRequestException.class)
                .extracting(e -> ((InvalidRcmRequestException) e).getErrorCode())
                .isEqualTo("RCM_ILLEGAL_TRANSITION");

        assertThat(outboxRepository.count()).isZero();   // NEGATIVE assertion mid-flow

        // ---- pre-auth + claim submit records ----------------------------------
        NhcxRequest preAuth = nhcxService.createPreAuth(505L, 8L,
                Map.of("lines", List.of(Map.of("item", 1), Map.of("item", 2))));
        assertThat(preAuth.getKind()).isEqualTo((short) NhcxRequest.KIND_PRE_AUTH);
        assertThat(preAuth.getStatusCode()).isEqualTo(NhcxStatus.INITIATED);
        assertThat(preAuth.getDetails())
                .extracting(com.hims.rcm.domain.model.NhcxRequestDetail::getLineNo)
                .containsExactly(1L, 2L);   // payload.lines split into detail rows

        NhcxRequest claim = nhcxService.createClaim(505L, 9001L, Map.of("claimAmount", 12500));
        assertThat(claim.getKind()).isEqualTo((short) NhcxRequest.KIND_CLAIM_SUBMIT);
        assertThat(claim.getBillId()).isEqualTo(9001L);

        assertThat(requestRepository.search((short) NhcxRequest.KIND_CLAIM_SUBMIT, null))
                .extracting(NhcxRequest::getRequestId)
                .containsExactly(claim.getRequestId());
        // status=INITIATED matches the companion kind=1 request too: the eligibility
        // RESPONSE endpoint pins the CHECK (t_eligibility_check.check_status -> 2), the
        // companion t_nhcx_request row keeps its own status 0 until a callback arrives.
        assertThat(nhcxService.list(null, (short) NhcxStatus.INITIATED))
                .extracting(NhcxRequest::getRequestId)
                .containsExactlyInAnyOrder(check.getRequestId(), preAuth.getRequestId(), claim.getRequestId());

        // ---- callback machine on the claim correlation id ---------------------
        String corr = claim.getCorrelationId();
        NhcxRequest sent = nhcxService.callback(corr, Map.of("sentToPayor", true), (short) NhcxStatus.SENT);
        assertThat(sent.getStatusCode()).isEqualTo(NhcxStatus.SENT);

        NhcxRequest done = nhcxService.callback(corr, Map.of("claimOutcome", "APPROVED"), (short) NhcxStatus.COMPLETED);
        assertThat(done.getStatusCode()).isEqualTo(NhcxStatus.COMPLETED);   // 0 -> 1 -> 3 forward-only

        // idempotent repeat by correlation_id: same status is a no-op success
        NhcxRequest repeat = nhcxService.callback(corr, Map.of("duplicate", true), (short) NhcxStatus.COMPLETED);
        assertThat(repeat.getStatusCode()).isEqualTo(NhcxStatus.COMPLETED);
        assertThat(repeat.getResponsePayload()).contains("APPROVED");   // NOT overwritten by duplicate
        assertThat(requestRepository.count()).isEqualTo(3);   // no duplicate rows created

        // backward transition rejected
        assertThatThrownBy(() -> nhcxService.callback(corr, Map.of(), (short) NhcxStatus.INITIATED))
                .isInstanceOf(InvalidRcmRequestException.class)
                .extracting(e -> ((InvalidRcmRequestException) e).getErrorCode())
                .isEqualTo("RCM_ILLEGAL_TRANSITION");

        // invalid status values rejected
        assertThatThrownBy(() -> nhcxService.callback(corr, Map.of(), (short) 4))
                .isInstanceOf(InvalidRcmRequestException.class)
                .hasMessageContaining("status must be");

        // unknown correlation id maps to a 404 envelope
        assertThatThrownBy(() -> nhcxService.callback("ghost-correlation", Map.of(), (short) NhcxStatus.ACKNOWLEDGED))
                .isInstanceOf(RcmNotFoundException.class)
                .extracting(e -> ((RcmNotFoundException) e).getErrorCode())
                .isEqualTo("NOT_FOUND");

        assertThat(outboxRepository.count()).isZero();   // NEGATIVE assertion mid-flow

        // ---- admission TID registry: assign once, duplicate -> 409 ------------
        AdmissionTid registered = tidService.assign(7777L, "TID-2026-000777");
        assertThat(registered.getTid()).isEqualTo("TID-2026-000777");
        assertThat(tidService.get(7777L).getTid()).isEqualTo("TID-2026-000777");

        assertThatThrownBy(() -> tidService.assign(7777L, "TID-DIFFERENT"))
                .isInstanceOf(TidConflictException.class)
                .extracting(e -> ((TidConflictException) e).getErrorCode())
                .isEqualTo("TID_EXISTS");
        assertThat(tidRepository.count()).isEqualTo(1);

        // DB-level unique backstop: direct duplicate insert violates uq_t_admission_tid_admission
        var dup = catchThrowableOfType(
                () -> jdbc.update(
                        "INSERT INTO ihms_rcm.t_admission_tid "
                                + "(tid_id, admission_id, tid, added_by, added_datetime) "
                                + "VALUES (nextval('ihms_rcm.t_admission_tid_seq'), 7777, 'TID-DIFFERENT', 1, now())"),
                org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(dup).as("duplicate admission_id insert must violate uq_t_admission_tid_admission")
                .isNotNull();
    }
}
