package com.hims.govt.infrastructure.persistence;

import com.hims.govt.GovtServiceApplication;
import com.hims.govt.application.AasmaanService;
import com.hims.govt.application.BeneficiaryService;
import com.hims.govt.application.EjsService;
import com.hims.govt.application.exceptions.InvalidGovtRequestException;
import com.hims.govt.domain.model.AasmaanCase;
import com.hims.govt.domain.model.BeneficiaryEnrolment;
import com.hims.govt.domain.model.EjsIndicatorEntry;
import com.hims.govt.domain.model.EjsIncentive;
import com.hims.govt.domain.model.IncentiveRate;
import com.hims.govt.infrastructure.outbox.OutboxRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L2 — WP-T4 Govt Programs on real PG16: V1 schema applies (ihms_govt:
 * t_beneficiary_enrolment, t_reference_tid, t_aasmaan_case, t_aasmaan_case_note,
 * t_ejs_supervisor, t_ejs_asha, t_ejs_indicator_entry, t_ejs_incentive,
 * t_incentive_rate + 3 seed rows, outbox), every flow runs against the DB
 * (Chiranjeevi enrolment + TID linkage, Aasmaan forward-only status +
 * closed-case note guard, EJS indicator upsert, compute math, recompute rule,
 * approve/pay machine) and — per register WP-T4 EVENT POLICY — the outbox stays
 * EMPTY through EVERY flow (IncentiveComputed / CaseUpdated DEFERRED; explicit
 * NEGATIVE OUTBOX assertion, cssd-service convention).
 * NOTE: requires Docker; skipped automatically when Docker is absent
 * (@Testcontainers(disabledWithoutDocker = true)) — do not remove.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = GovtServiceApplication.class)
@DirtiesContext
@Transactional
class GovtServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private BeneficiaryService beneficiaryService;

    @Autowired
    private AasmaanService aasmaanService;

    @Autowired
    private EjsService ejsService;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private EntityManager em;

    @SuppressWarnings("unchecked")
    private List<String> tables() {
        return em.createNativeQuery("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'ihms_govt' ORDER BY table_name
                """).getResultList();
    }

    @Test
    void flywaySchema_applies_allTablesSequencesAndSeedRatesPresent() {
        assertThat(tables()).contains("outbox", "t_beneficiary_enrolment", "t_reference_tid",
                "t_aasmaan_case", "t_aasmaan_case_note", "t_ejs_supervisor", "t_ejs_asha",
                "t_ejs_indicator_entry", "t_ejs_incentive", "t_incentive_rate");
        Number seqCount = (Number) em.createNativeQuery("""
                SELECT count(*) FROM information_schema.sequences
                WHERE sequence_schema = 'ihms_govt'
                  AND sequence_name IN ('t_beneficiary_enrolment_seq', 't_reference_tid_seq',
                      't_aasmaan_case_seq', 't_aasmaan_case_note_seq', 't_ejs_supervisor_seq',
                      't_ejs_asha_seq', 't_ejs_indicator_entry_seq', 't_ejs_incentive_seq')
                """).getSingleResult();
        assertThat(seqCount.intValue()).isEqualTo(8);

        // rate-table seeds: exactly 3 rows per register WP-T4
        Number rateCount = (Number) em.createNativeQuery(
                "SELECT count(*) FROM ihms_govt.t_incentive_rate").getSingleResult();
        assertThat(rateCount.intValue()).isEqualTo(3);

        // EVENT POLICY: nothing has touched the outbox during migration/boot
        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void beneficiaryFlow_enrolAndTidLinkage_withNegativeOutboxAssertion() {
        assertThat(outboxRepository.count()).isZero();

        // guards before anything is written
        assertThatThrownBy(() -> beneficiaryService.enrol(11L, "OTHER", "CH-X", LocalDate.now()))
                .isInstanceOf(InvalidGovtRequestException.class)
                .extracting(e -> ((InvalidGovtRequestException) e).getErrorCode())
                .isEqualTo("BEXA003");   // scheme pinned to CHIRANJEEVI
        assertThat(outboxRepository.count()).isZero();   // rejected enrol emits nothing

        // enrol + duplicate enrolmentNo -> 409 code
        BeneficiaryEnrolment e = beneficiaryService.enrol(11L, "CHIRANJEEVI", "CH-001",
                LocalDate.of(2025, 1, 15));
        assertThat(e.getScheme()).isEqualTo("CHIRANJEEVI");
        assertThat(e.getIsActive()).isTrue();

        assertThatThrownBy(() -> beneficiaryService.enrol(12L, "CHIRANJEEVI", "CH-001", LocalDate.now()))
                .isInstanceOf(com.hims.govt.application.exceptions.GovtConflictException.class)
                .hasMessageContaining("enrolmentNo already exists: CH-001");
        // DB-level uniqueness backstop too (uq_enrolment_no)

        // TID linkage: unknown enrolmentNo -> 404; dup tid -> conflict; happy path links
        assertThatThrownBy(() -> beneficiaryService.linkTid("GHOST", "TID-1"))
                .isInstanceOf(com.hims.govt.application.exceptions.GovtNotFoundException.class);
        var linked = beneficiaryService.linkTid("CH-001", "TID-ABC-123");
        assertThat(linked.getEnrolmentId()).isEqualTo(e.getEnrolmentId());
        assertThatThrownBy(() -> beneficiaryService.linkTid("CH-001", "TID-ABC-123"))
                .isInstanceOf(com.hims.govt.application.exceptions.GovtConflictException.class)
                .extracting(x -> ((com.hims.govt.application.exceptions.GovtConflictException) x).getErrorCode())
                .isEqualTo("GOVT_DUPLICATE_TID");

        // NEGATIVE OUTBOX: enrolment/TID flow emitted nothing
        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void aasmaanFlow_forwardOnlyStatusAndClosedCaseNoteGuard_withNegativeOutboxAssertion() {
        assertThat(outboxRepository.count()).isZero();

        AasmaanCase c = aasmaanService.create(505L, "ANC");
        long caseId = c.getCaseId();
        assertThat(c.getStatus()).isEqualTo(AasmaanCase.STATUS_OPEN);
        assertThat(c.getOpenedOn()).isNotNull();

        // notes allowed while open/in-progress
        aasmaanService.addNote(caseId, "first contact", "asha-7");
        aasmaanService.updateStatus(caseId, "IN_PROGRESS");
        aasmaanService.addNote(caseId, "second contact", "asha-7");

        // forward-only machine pinned against real data
        assertThatThrownBy(() -> aasmaanService.updateStatus(caseId, "OPEN"))
                .isInstanceOf(InvalidGovtRequestException.class)
                .extracting(e -> ((InvalidGovtRequestException) e).getErrorCode())
                .isEqualTo("GOVT_ILLEGAL_TRANSITION");   // backward

        AasmaanCase closed = aasmaanService.updateStatus(caseId, "CLOSED");
        assertThat(closed.getStatus()).isEqualTo(AasmaanCase.STATUS_CLOSED);
        assertThat(closed.getClosedOn()).isNotNull();   // closing stamps closed_on

        // closed-case note guard
        assertThatThrownBy(() -> aasmaanService.addNote(caseId, "too late", "asha-7"))
                .isInstanceOf(InvalidGovtRequestException.class)
                .extracting(e -> ((InvalidGovtRequestException) e).getErrorCode())
                .isEqualTo("GOVT_CASE_CLOSED");

        // terminal: no moves out of CLOSED
        assertThatThrownBy(() -> aasmaanService.updateStatus(caseId, "IN_PROGRESS"))
                .isInstanceOf(InvalidGovtRequestException.class)
                .extracting(x -> ((InvalidGovtRequestException) x).getErrorCode())
                .isEqualTo("GOVT_ILLEGAL_TRANSITION");

        // list filters hit the DB query
        assertThat(aasmaanService.list((short) AasmaanCase.STATUS_CLOSED, 505L))
                .extracting(AasmaanCase::getCaseId)
                .containsExactly(caseId);
        assertThat(aasmaanService.list(null, 999_999L)).isEmpty();

        // NEGATIVE OUTBOX: CaseUpdated is DEFERRED — nothing emitted through the whole flow
        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void ejsFlow_indicatorUpsertComputeMathRecomputeApprovePay_withNegativeOutboxAssertion() {
        assertThat(outboxRepository.count()).isZero();

        // ASHA registration (supervisor optional; none exists here)
        var asha = ejsService.createAsha("ASHA-001", "Test Asha", null);
        long ashaId = asha.getAshaId();
        assertThatThrownBy(() -> ejsService.createAsha("ASHA-001", "Dup", null))
                .isInstanceOf(com.hims.govt.application.exceptions.GovtConflictException.class);

        // indicator upsert on the unique key — insert then update in place
        EjsIndicatorEntry first = ejsService.upsertIndicator(ashaId, "ANC_CHECKUP", 2025, 4, 2);
        EjsIndicatorEntry updated = ejsService.upsertIndicator(ashaId, "ANC_CHECKUP", 2025, 4, 4);
        assertThat(updated.getEntryId()).isEqualTo(first.getEntryId());   // same row upserted
        assertThat(updated.getValue()).isEqualTo(4);
        ejsService.upsertIndicator(ashaId, "DELIVERY", 2025, 4, 1);
        ejsService.upsertIndicator(ashaId, "CHILD_IMMUNISATION", 2025, 4, 4);

        // compute math vs the SEEDED rate table: 4x50 + 1x600 + 4x75 = 1100.00
        EjsIncentive computed = ejsService.compute(ashaId, 2025, 4);
        assertThat(computed.getStatus()).isEqualTo(EjsIncentive.STATUS_COMPUTED);
        assertThat(computed.getAmount()).isEqualByComparingTo("1100.00");
        assertThat(computed.getComputedAt()).isNotNull();

        // RECOMPUTE RULE: prior COMPUTED replaced; APPROVED/PAID untouched.
        // Move the current COMPUTED forward so history exists, then recompute.
        EjsIncentive approved = ejsService.approve(computed.getIncentiveId());
        assertThat(approved.getStatus()).isEqualTo(EjsIncentive.STATUS_APPROVED);
        ejsService.upsertIndicator(ashaId, "DELIVERY", 2025, 4, 2);
        EjsIncentive recomputed = ejsService.compute(ashaId, 2025, 4);
        assertThat(recomputed.getIncentiveId()).isNotEqualTo(computed.getIncentiveId());
        assertThat(recomputed.getAmount()).isEqualByComparingTo("1700.00");   // 4x50 + 2x600 + 4x75
        // APPROVED history survived the recompute
        List<EjsIncentive> all = ejsService.listIncentives(null, ashaId);
        assertThat(all).extracting(EjsIncentive::getStatus)
                .contains(EjsIncentive.STATUS_APPROVED, EjsIncentive.STATUS_COMPUTED);
        // exactly ONE COMPUTED row remains for the period
        assertThat(all.stream().filter(i -> i.getStatus() == EjsIncentive.STATUS_COMPUTED)).hasSize(1);

        // pay machine: APPROVED -> PAID ok; COMPUTED cannot be paid directly
        EjsIncentive paid = ejsService.pay(approved.getIncentiveId());
        assertThat(paid.getStatus()).isEqualTo(EjsIncentive.STATUS_PAID);
        assertThatThrownBy(() -> ejsService.pay(recomputed.getIncentiveId()))
                .isInstanceOf(InvalidGovtRequestException.class)
                .extracting(e -> ((InvalidGovtRequestException) e).getErrorCode())
                .isEqualTo("GOVT_ILLEGAL_TRANSITION");   // COMPUTED -> pay is illegal
        EjsIncentive reApproved = ejsService.approve(recomputed.getIncentiveId());   // COMPUTED -> APPROVED ok
        assertThat(reApproved.getStatus()).isEqualTo(EjsIncentive.STATUS_APPROVED);
        assertThatThrownBy(() -> ejsService.approve(reApproved.getIncentiveId()))
                .isInstanceOf(InvalidGovtRequestException.class)
                .extracting(e -> ((InvalidGovtRequestException) e).getErrorCode())
                .isEqualTo("GOVT_ILLEGAL_TRANSITION");   // APPROVED is not re-approvable
        EjsIncentive rePaid = ejsService.pay(reApproved.getIncentiveId());           // APPROVED -> PAID ok
        assertThat(rePaid.getStatus()).isEqualTo(EjsIncentive.STATUS_PAID);

        // status filter round-trip
        assertThat(ejsService.listIncentives((short) EjsIncentive.STATUS_PAID, ashaId))
                .allMatch(x -> x.getStatus() == EjsIncentive.STATUS_PAID);

        // NEGATIVE OUTBOX: IncentiveComputed is DEFERRED — ZERO rows through the whole flow
        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void seededRateTable_roundTripsThroughCompute_withNegativeOutboxAssertion() {
        assertThat(outboxRepository.count()).isZero();
        for (String code : new String[]{"ANC_CHECKUP", "DELIVERY", "CHILD_IMMUNISATION"}) {
            IncentiveRate rate = em.find(IncentiveRate.class, code);
            assertThat(rate).as("seeded rate %s", code).isNotNull();
            assertThat(rate.getRate().signum()).isPositive();
        }
        // an entry whose code has no rate row contributes 0 (documented behavior)
        var asha = ejsService.createAsha("ASHA-RATE", "Rate Probe", null);
        ejsService.upsertIndicator(asha.getAshaId(), "UNRATED_CODE", 2030, 12, 100);
        EjsIncentive result = ejsService.compute(asha.getAshaId(), 2030, 12);
        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.ZERO.setScale(2));
        assertThat(outboxRepository.count()).isZero();
    }
}
