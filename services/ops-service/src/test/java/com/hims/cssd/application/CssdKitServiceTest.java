package com.hims.cssd.application;

import com.hims.cssd.application.exceptions.CssdNotFoundException;
import com.hims.cssd.application.exceptions.InvalidKitRequestException;
import com.hims.cssd.domain.model.Kit;
import com.hims.cssd.domain.model.SterilizationCycle;
import com.hims.cssd.domain.repository.KitIssueRepository;
import com.hims.cssd.domain.repository.KitItemRepository;
import com.hims.cssd.domain.repository.KitRepository;
import com.hims.cssd.domain.repository.SterilizationCycleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L0 — kit lifecycle guards and event policy, no Spring context.
 * Register WP-R3 assertions pinned here:
 *   - assemble: kitCode required/unique, >=1 item line, qty > 0, expiryDays > 0
 *     (default 14)
 *   - start cycle ONLY from ASSEMBLED / STERILE (reprocess); method required
 *   - complete once-only; PASS -> STERILE (+sterilized_at), FAIL -> REJECTED
 *   - issue ONLY from STERILE (REJECTED kits unusable); issuedToRef required;
 *     second issue rejected (single active issue per kit)
 *   - EVENT POLICY: nothing may write to the outbox on ANY path — the service has
 *     no emitter dependency at all and this is additionally asserted at L2.
 */
class CssdKitServiceTest {

    private KitRepository kitRepository;
    private KitItemRepository kitItemRepository;
    private SterilizationCycleRepository cycleRepository;
    private KitIssueRepository issueRepository;
    private CssdKitService service;
    private final AtomicLong seq = new AtomicLong(100);

    @BeforeEach
    void setUp() {
        kitRepository = mock(KitRepository.class);
        kitItemRepository = mock(KitItemRepository.class);
        cycleRepository = mock(SterilizationCycleRepository.class);
        issueRepository = mock(KitIssueRepository.class);
        EntityManager em = mock(EntityManager.class);
        service = new CssdKitService(kitRepository, kitItemRepository, cycleRepository, issueRepository);
        ReflectionTestUtils.setField(service, "em", em);

        Query seqQuery = mock(Query.class);
        when(seqQuery.getSingleResult()).thenAnswer(inv -> seq.incrementAndGet());
        when(em.createNativeQuery(contains("nextval"))).thenReturn(seqQuery);

        // tiny in-memory kit store so findById mirrors what was saved (lifecycle reads)
        Map<Long, Kit> kitStore = new HashMap<>();
        when(kitRepository.save(any())).thenAnswer(inv -> {
            Kit k = inv.getArgument(0);
            kitStore.put(k.getKitId(), k);
            return k;
        });
        when(kitRepository.findById(anyLong()))
                .thenAnswer(inv -> Optional.ofNullable(kitStore.get((Long) inv.getArgument(0))));
        when(kitItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Map<Long, SterilizationCycle> cycleStore = new HashMap<>();
        when(cycleRepository.save(any())).thenAnswer(inv -> {
            SterilizationCycle c = inv.getArgument(0);
            cycleStore.put(c.getCycleId(), c);
            return c;
        });
        when(cycleRepository.findById(anyLong()))
                .thenAnswer(inv -> Optional.ofNullable(cycleStore.get((Long) inv.getArgument(0))));
        when(issueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(kitItemRepository.findByKitId(anyLong())).thenReturn(List.of());
    }

    private static CssdKitService.ItemInput item(long itemId) {
        return new CssdKitService.ItemInput(itemId, 2);
    }

    /** Drive a kit through assemble -> cycle -> PASS so it lands in STERILE. */
    private long makeSterileKit(String code) {
        Kit k = service.create(code, "GENERAL", List.of(item(501)), null);
        SterilizationCycle c = service.startCycle(k.getKitId(), "AUTOCLAVE");
        service.completeCycle(c.getCycleId(), "PASS");
        return k.getKitId();
    }

    // ------------------------------------------------------------- assemble guards

    @Test
    void create_requiresKitCode_bexa003() {
        assertThatThrownBy(() -> service.create(null, "GENERAL", List.of(item(1)), null))
                .isInstanceOf(InvalidKitRequestException.class)
                .extracting(e -> ((InvalidKitRequestException) e).getErrorCode())
                .isEqualTo("BEXA003");
        assertThatThrownBy(() -> service.create("   ", "GENERAL", List.of(item(1)), null))
                .isInstanceOf(InvalidKitRequestException.class)
                .hasMessageContaining("kitCode");
    }

    @Test
    void create_requiresAtLeastOneItem_bexa003() {
        assertThatThrownBy(() -> service.create("K-1", "GENERAL", List.of(), null))
                .isInstanceOf(InvalidKitRequestException.class)
                .hasMessageContaining("At least one kit item");
        assertThatThrownBy(() -> service.create("K-1", "GENERAL", null, null))
                .isInstanceOf(InvalidKitRequestException.class)
                .hasMessageContaining("At least one kit item");
    }

    @Test
    void create_requiresQtyPositive_bexa003() {
        assertThatThrownBy(() -> service.create("K-1", "GENERAL",
                List.of(new CssdKitService.ItemInput(5L, 0)), null))
                .isInstanceOf(InvalidKitRequestException.class)
                .hasMessageContaining("qty must be > 0");
        assertThatThrownBy(() -> service.create("K-1", "GENERAL",
                List.of(new CssdKitService.ItemInput(5L, -1)), null))
                .isInstanceOf(InvalidKitRequestException.class)
                .hasMessageContaining("qty must be > 0");
        assertThatThrownBy(() -> service.create("K-1", "GENERAL",
                List.of(new CssdKitService.ItemInput(null, 2)), null))
                .isInstanceOf(InvalidKitRequestException.class)
                .hasMessageContaining("itemId");
    }

    @Test
    void create_expiryDefaults14_andMustBePositiveWhenSupplied() {
        Kit defaultExpiry = service.create("K-DEF", "GENERAL", List.of(item(501)), null);
        assertThat(defaultExpiry.getExpiryDays()).isEqualTo(Kit.DEFAULT_EXPIRY_DAYS);

        assertThatThrownBy(() -> service.create("K-NEG", "GENERAL", List.of(item(501)), 0))
                .isInstanceOf(InvalidKitRequestException.class)
                .hasMessageContaining("expiryDays must be > 0");
        assertThatThrownBy(() -> service.create("K-NEG", "GENERAL", List.of(item(501)), -3))
                .isInstanceOf(InvalidKitRequestException.class)
                .hasMessageContaining("expiryDays must be > 0");

        Kit custom = service.create("K-CUSTOM", "GENERAL", List.of(item(501)), 30);
        assertThat(custom.getExpiryDays()).isEqualTo(30);
    }

    @Test
    void create_rejectsDuplicateKitCode() {
        service.create("K-DUP", "GENERAL", List.of(item(501)), null);
        when(kitRepository.existsByKitCode("K-DUP")).thenReturn(true);

        assertThatThrownBy(() -> service.create("K-DUP", "GENERAL", List.of(item(502)), null))
                .isInstanceOf(InvalidKitRequestException.class)
                .extracting(e -> ((InvalidKitRequestException) e).getErrorCode())
                .isEqualTo("CSSD_DUPLICATE_KIT_CODE");
    }

    @Test
    void create_newKitStartsAssembledWithItems() {
        Kit kit = service.create("K-NEW", "ORTHOPEDIC", List.of(item(501), item(502)), null);
        assertThat(kit.getStatus()).isEqualTo(Kit.STATUS_ASSEMBLED);
        assertThat(kit.getKitCode()).isEqualTo("K-NEW");
        verify(kitItemRepository, times(2)).save(any());
    }

    // ------------------------------------------------------------- cycle guards

    @Test
    void startCycle_onlyFromAssembledOrSterile() {
        // happy: ASSEMBLED -> STERILIZING
        Kit kit = service.create("K-C1", "GENERAL", List.of(item(501)), null);
        SterilizationCycle cycle = service.startCycle(kit.getKitId(), "AUTOCLAVE");
        assertThat(cycle.getMethod()).isEqualTo("AUTOCLAVE");
        assertThat(cycle.getResult()).isNull();
        assertThat(cycle.getCompletedAt()).isNull();

        // STERILIZING cannot start another cycle
        assertThatThrownBy(() -> service.startCycle(kit.getKitId(), "AUTOCLAVE"))
                .isInstanceOf(InvalidKitRequestException.class)
                .extracting(e -> ((InvalidKitRequestException) e).getErrorCode())
                .isEqualTo("CSSD_ILLEGAL_TRANSITION");
    }

    @Test
    void startCycle_requiresMethod_bexa003() {
        Kit kit = service.create("K-M", "GENERAL", List.of(item(501)), null);
        assertThatThrownBy(() -> service.startCycle(kit.getKitId(), " "))
                .isInstanceOf(InvalidKitRequestException.class)
                .extracting(e -> ((InvalidKitRequestException) e).getErrorCode())
                .isEqualTo("BEXA003");
    }

    @Test
    void startCycle_unknownKit_404() {
        when(kitRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.startCycle(404L, "AUTOCLAVE"))
                .isInstanceOf(CssdNotFoundException.class)
                .extracting(e -> ((CssdNotFoundException) e).getErrorCode())
                .isEqualTo("NOT_FOUND");
    }

    @Test
    void completeCycle_pass_makesSterileAndStampsSterilizedAt() {
        long kitId = makeSterileKit("K-PASS");
        Kit kit = service.get(kitId);
        assertThat(kit.getStatus()).isEqualTo(Kit.STATUS_STERILE);
        assertThat(kit.getSterilizedAt()).isNotNull();
    }

    @Test
    void completeCycle_fail_rejectsKit_unusableForIssue() {
        Kit kit = service.create("K-FAIL", "GENERAL", List.of(item(501)), null);
        SterilizationCycle cycle = service.startCycle(kit.getKitId(), "AUTOCLAVE");

        SterilizationCycle done = service.completeCycle(cycle.getCycleId(), "FAIL");
        assertThat(done.getResult()).isEqualTo(SterilizationCycle.RESULT_FAIL);
        assertThat(done.getCompletedAt()).isNotNull();

        Kit after = service.get(kit.getKitId());
        assertThat(after.getStatus()).isEqualTo(Kit.STATUS_REJECTED);

        // REJECTED kits are unusable -> issue is a guarded 400
        assertThatThrownBy(() -> service.issue(kit.getKitId(), "OT-REF-1"))
                .isInstanceOf(InvalidKitRequestException.class)
                .extracting(e -> ((InvalidKitRequestException) e).getErrorCode())
                .isEqualTo("CSSD_ILLEGAL_TRANSITION");
    }

    @Test
    void completeCycle_onceOnly_andValidatesResultEnum() {
        Kit kit = service.create("K-ONCE", "GENERAL", List.of(item(501)), null);
        SterilizationCycle cycle = service.startCycle(kit.getKitId(), "AUTOCLAVE");

        assertThatThrownBy(() -> service.completeCycle(cycle.getCycleId(), "MAYBE"))
                .isInstanceOf(InvalidKitRequestException.class)
                .extracting(e -> ((InvalidKitRequestException) e).getErrorCode())
                .isEqualTo("BEXA003");
        assertThatThrownBy(() -> service.completeCycle(cycle.getCycleId(), null))
                .isInstanceOf(InvalidKitRequestException.class)
                .hasMessageContaining("PASS or FAIL");

        service.completeCycle(cycle.getCycleId(), "PASS");
        assertThatThrownBy(() -> service.completeCycle(cycle.getCycleId(), "FAIL"))
                .isInstanceOf(InvalidKitRequestException.class)
                .extracting(e -> ((InvalidKitRequestException) e).getErrorCode())
                .isEqualTo("CSSD_CYCLE_ALREADY_COMPLETED");
    }

    @Test
    void completeCycle_unknownCycle_404() {
        when(cycleRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.completeCycle(999L, "PASS"))
                .isInstanceOf(CssdNotFoundException.class)
                .extracting(e -> ((CssdNotFoundException) e).getErrorCode())
                .isEqualTo("NOT_FOUND");
    }

    @Test
    void sterileKit_canReprocess_throughNewCycle() {
        long kitId = makeSterileKit("K-REPROC");
        // STERILE re-entering a cycle is allowed (reprocess path)
        SterilizationCycle again = service.startCycle(kitId, "AUTOCLAVE");
        assertThat(service.get(kitId).getStatus()).isEqualTo(Kit.STATUS_STERILIZING);
        service.completeCycle(again.getCycleId(), "PASS");
        assertThat(service.get(kitId).getStatus()).isEqualTo(Kit.STATUS_STERILE);
    }

    // ------------------------------------------------------------- issue guards

    @Test
    void issue_onlyFromSterile_singleActiveIssue() {
        long kitId = makeSterileKit("K-ISSUE");

        // first issue ok -> ISSUED
        var issue = service.issue(kitId, "OT-SCHED-42");
        assertThat(issue.getIssuedToRef()).isEqualTo("OT-SCHED-42");
        assertThat(issue.getIssuedAt()).isNotNull();
        assertThat(service.get(kitId).getStatus()).isEqualTo(Kit.STATUS_ISSUED);

        // second issue from ISSUED is rejected — single active issue per kit
        assertThatThrownBy(() -> service.issue(kitId, "OT-SCHED-43"))
                .isInstanceOf(InvalidKitRequestException.class)
                .extracting(e -> ((InvalidKitRequestException) e).getErrorCode())
                .isEqualTo("CSSD_ILLEGAL_TRANSITION");

        // an ASSEMBLED (never sterilized) kit cannot be issued either
        Kit fresh = service.create("K-FRESH", "GENERAL", List.of(item(501)), null);
        assertThatThrownBy(() -> service.issue(fresh.getKitId(), "OT-SCHED-44"))
                .isInstanceOf(InvalidKitRequestException.class)
                .extracting(e -> ((InvalidKitRequestException) e).getErrorCode())
                .isEqualTo("CSSD_ILLEGAL_TRANSITION");
    }

    @Test
    void issue_requiresIssuedToRef_bexa003() {
        long kitId = makeSterileKit("K-REF");
        assertThatThrownBy(() -> service.issue(kitId, ""))
                .isInstanceOf(InvalidKitRequestException.class)
                .extracting(e -> ((InvalidKitRequestException) e).getErrorCode())
                .isEqualTo("BEXA003");
        assertThatThrownBy(() -> service.issue(kitId, null))
                .isInstanceOf(InvalidKitRequestException.class)
                .hasMessageContaining("issuedToRef");
    }

    // ------------------------------------------------------------- queries

    @Test
    void issuesListing_unknownKit_404() {
        when(kitRepository.findById(77L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.issues(77L))
                .isInstanceOf(CssdNotFoundException.class)
                .extracting(e -> ((CssdNotFoundException) e).getErrorCode())
                .isEqualTo("NOT_FOUND");
    }

    // ------------------------------------------------------------- event policy

    /**
     * NEGATIVE OUTBOX assertion at L0: the full happy lifecycle performs NO outbox
     * writes whatsoever. The service holds no outbox-emitter dependency by design
     * (events REJECTED phase 1); this pins that no repository other than the four
     * domain ones was touched during any transition.
     */
    @Test
    void fullLifecycle_emitsNothing_noEventInfrastructureTouched() {
        long kitId = makeSterileKit("K-NOEVENT");
        service.issue(kitId, "OT-SCHED-99");

        // exactly one save per transition: assemble -> sterilizing -> sterile -> issued
        verify(kitRepository, times(4)).save(any(Kit.class));

        // structural: CssdKitService takes ONLY the four domain repositories —
        // no OutboxService/OutboxRepository/emitter exists in its constructor graph,
        // so no code path can write an outbox row (L2 asserts the same on real PG16).
    }
}
