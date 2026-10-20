package com.hims.cssd.infrastructure.persistence;

import com.hims.cssd.CssdServiceApplication;
import com.hims.cssd.application.CssdKitService;
import com.hims.cssd.application.exceptions.CssdNotFoundException;
import com.hims.cssd.application.exceptions.InvalidKitRequestException;
import com.hims.cssd.domain.model.Kit;
import com.hims.cssd.domain.model.KitIssue;
import com.hims.cssd.domain.model.SterilizationCycle;
import com.hims.cssd.infrastructure.outbox.OutboxRepository;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L2 — WP-R3 CSSD service on real PG16: V1 schema applies (ihms_cssd: t_kit,
 * t_kit_item, t_sterilization_cycle, t_kit_issue, outbox), the full kit lifecycle
 * runs against the DB (assemble -> cycle -> PASS -> STERILE -> issue -> ISSUED),
 * the FAIL -&gt; REJECTED path makes kits unusable, and — per register WP-R3 EVENT
 * POLICY — the outbox stays EMPTY through EVERY path (KitIssued /
 * SterilizationCompleted are REJECTED phase 1; explicit NEGATIVE outbox assertion).
 * NOTE: requires Docker; skipped automatically when Docker is absent
 * (@Testcontainers(disabledWithoutDocker = true)) — do not remove.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = CssdServiceApplication.class)
@DirtiesContext
@Transactional
class CssdServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private CssdKitService cssdKitService;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private EntityManager em;

    @SuppressWarnings("unchecked")
    private List<String> tables() {
        return em.createNativeQuery("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'ihms_cssd' ORDER BY table_name
                """).getResultList();
    }

    @Test
    void flywaySchema_applies_allTablesAndSequencesPresent() {
        assertThat(tables()).contains("outbox", "t_kit", "t_kit_item",
                "t_sterilization_cycle", "t_kit_issue");
        Number seqCount = (Number) em.createNativeQuery("""
                SELECT count(*) FROM information_schema.sequences
                WHERE sequence_schema = 'ihms_cssd'
                  AND sequence_name IN ('t_kit_seq', 't_sterilization_cycle_seq', 't_kit_issue_seq')
                """).getSingleResult();
        assertThat(seqCount.intValue()).isEqualTo(3);
        // EVENT POLICY: nothing has touched the outbox during migration/boot
        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void fullLifecycle_happyPath_withNegativeOutboxAssertion() throws Exception {
        assertThat(outboxRepository.count()).isZero();

        // ---- assemble guards before anything is written -----------------------
        assertThatThrownBy(() -> cssdKitService.create(null, "GENERAL", List.of(), null))
                .isInstanceOf(InvalidKitRequestException.class)
                .extracting(e -> ((InvalidKitRequestException) e).getErrorCode())
                .isEqualTo("BEXA003");   // kitCode required / >=1 item required
        assertThatThrownBy(() -> cssdKitService.create("K-G", "GENERAL",
                List.of(new CssdKitService.ItemInput(5L, 0)), null))
                .isInstanceOf(InvalidKitRequestException.class)
                .hasMessageContaining("qty must be > 0");
        assertThat(outboxRepository.count()).isZero();   // rejected creates emit nothing

        // ---- assemble ----------------------------------------------------------
        Kit kit = cssdKitService.create("KIT-0001", "ORTHOPEDIC", List.of(
                new CssdKitService.ItemInput(501L, 4),
                new CssdKitService.ItemInput(502L, 1)), null);
        long kitId = kit.getKitId();
        assertThat(kit.getStatus()).isEqualTo(Kit.STATUS_ASSEMBLED);
        assertThat(kit.getExpiryDays()).isEqualTo(14);   // documented default

        // duplicate kit_code pinned at service AND DB level (uq_t_kit_code)
        assertThatThrownBy(() -> cssdKitService.create("KIT-0001", "GENERAL",
                List.of(new CssdKitService.ItemInput(501L, 1)), null))
                .isInstanceOf(InvalidKitRequestException.class)
                .extracting(e -> ((InvalidKitRequestException) e).getErrorCode())
                .isEqualTo("CSSD_DUPLICATE_KIT_CODE");

        // ---- cycle guards + start ----------------------------------------------
        assertThatThrownBy(() -> cssdKitService.startCycle(kitId, ""))
                .isInstanceOf(InvalidKitRequestException.class)
                .extracting(e -> ((InvalidKitRequestException) e).getErrorCode())
                .isEqualTo("BEXA003");

        SterilizationCycle cycle = cssdKitService.startCycle(kitId, "AUTOCLAVE");
        assertThat(cssdKitService.get(kitId).getStatus()).isEqualTo(Kit.STATUS_STERILIZING);
        assertThatThrownBy(() -> cssdKitService.startCycle(kitId, "AUTOCLAVE"))
                .isInstanceOf(InvalidKitRequestException.class)
                .extracting(e -> ((InvalidKitRequestException) e).getErrorCode())
                .isEqualTo("CSSD_ILLEGAL_TRANSITION");   // cannot nest cycles

        // ---- complete PASS -> STERILE (+sterilized_at) --------------------------
        SterilizationCycle done = cssdKitService.completeCycle(cycle.getCycleId(), "PASS");
        assertThat(done.getResult()).isEqualTo(SterilizationCycle.RESULT_PASS);
        assertThat(done.getCompletedAt()).isNotNull();
        Kit sterile = cssdKitService.get(kitId);
        assertThat(sterile.getStatus()).isEqualTo(Kit.STATUS_STERILE);
        assertThat(sterile.getSterilizedAt()).isNotNull();

        assertThatThrownBy(() -> cssdKitService.completeCycle(cycle.getCycleId(), "FAIL"))
                .isInstanceOf(InvalidKitRequestException.class)
                .extracting(e -> ((InvalidKitRequestException) e).getErrorCode())
                .isEqualTo("CSSD_CYCLE_ALREADY_COMPLETED");

        assertThat(outboxRepository.count()).isZero();   // SterilizationCompleted REJECTED — nothing emitted

        // ---- issue STERILE-only, single active issue ----------------------------
        assertThatThrownBy(() -> cssdKitService.issue(kitId, " "))
                .isInstanceOf(InvalidKitRequestException.class)
                .extracting(e -> ((InvalidKitRequestException) e).getErrorCode())
                .isEqualTo("BEXA003");

        KitIssue issue = cssdKitService.issue(kitId, "OT-SCHED-9001");
        assertThat(issue.getIssuedToRef()).isEqualTo("OT-SCHED-9001");
        assertThat(cssdKitService.get(kitId).getStatus()).isEqualTo(Kit.STATUS_ISSUED);

        assertThatThrownBy(() -> cssdKitService.issue(kitId, "OT-SCHED-9002"))
                .isInstanceOf(InvalidKitRequestException.class)
                .extracting(e -> ((InvalidKitRequestException) e).getErrorCode())
                .isEqualTo("CSSD_ILLEGAL_TRANSITION");   // single active issue per kit

        List<KitIssue> issues = cssdKitService.issues(kitId);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0).getIssueId()).isEqualTo(issue.getIssueId());

        assertThat(outboxRepository.count()).isZero();   // KitIssued REJECTED — nothing emitted

        // ---- list filters --------------------------------------------------------
        assertThat(cssdKitService.list((short) Kit.STATUS_ISSUED, "ORTHOPEDIC"))
                .extracting(Kit::getKitId).containsExactly(kitId);
        assertThat(cssdKitService.list((short) Kit.STATUS_ASSEMBLED, null)).isEmpty();
        assertThat(cssdKitService.list(null, "cardiac")).isEmpty();
        assertThatThrownBy(() -> cssdKitService.get(999_999L))
                .isInstanceOf(CssdNotFoundException.class)
                .extracting(e -> ((CssdNotFoundException) e).getErrorCode())
                .isEqualTo("NOT_FOUND");

        // FINAL NEGATIVE OUTBOX ASSERTION: full lifecycle produced ZERO outbox rows
        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void failPath_rejectsKit_unusable_withNegativeOutboxAssertion() {
        Kit kit = cssdKitService.create("KIT-FAIL", "GENERAL",
                List.of(new CssdKitService.ItemInput(601L, 2)), null);

        SterilizationCycle cycle = cssdKitService.startCycle(kit.getKitId(), "AUTOCLAVE");
        cssdKitService.completeCycle(cycle.getCycleId(), "FAIL");

        Kit rejected = cssdKitService.get(kit.getKitId());
        assertThat(rejected.getStatus()).isEqualTo(Kit.STATUS_REJECTED);
        assertThat(rejected.getSterilizedAt()).isNull();   // FAIL never stamps sterilization

        // REJECTED kits are unusable: issue is a guarded 400
        assertThatThrownBy(() -> cssdKitService.issue(kit.getKitId(), "OT-X"))
                .isInstanceOf(InvalidKitRequestException.class)
                .extracting(e -> ((InvalidKitRequestException) e).getErrorCode())
                .isEqualTo("CSSD_ILLEGAL_TRANSITION");

        // a REJECTED kit cannot re-enter a cycle either (only ASSEMBLED/STERILE can)
        assertThatThrownBy(() -> cssdKitService.startCycle(kit.getKitId(), "AUTOCLAVE"))
                .isInstanceOf(InvalidKitRequestException.class)
                .extracting(e -> ((InvalidKitRequestException) e).getErrorCode())
                .isEqualTo("CSSD_ILLEGAL_TRANSITION");

        // NEGATIVE OUTBOX assertion on the failure path too
        assertThat(outboxRepository.count()).isZero();
    }
}
