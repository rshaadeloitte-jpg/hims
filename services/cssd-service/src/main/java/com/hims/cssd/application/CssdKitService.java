package com.hims.cssd.application;

import com.hims.cssd.application.exceptions.CssdNotFoundException;
import com.hims.cssd.application.exceptions.CssdRuleException;
import com.hims.cssd.application.exceptions.InvalidKitRequestException;
import com.hims.cssd.domain.model.Kit;
import com.hims.cssd.domain.model.KitIssue;
import com.hims.cssd.domain.model.KitItem;
import com.hims.cssd.domain.model.SterilizationCycle;
import com.hims.cssd.domain.repository.KitIssueRepository;
import com.hims.cssd.domain.repository.KitItemRepository;
import com.hims.cssd.domain.repository.KitRepository;
import com.hims.cssd.domain.repository.SterilizationCycleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import com.hims.cssd.domain.model.CssdBase;
import java.util.ArrayList;
import java.util.List;

/**
 * CSSD kit lifecycle (GAP-REGISTER-5 WP-R3, Catalog #15).
 *
 * Status machine (t_kit.status SMALLINT):
 *   1=ASSEMBLED -&gt; 2=STERILIZING -&gt; 3=STERILE -&gt; 4=ISSUED
 *                              -&gt; 5=REJECTED (FAIL; terminal &amp; unusable — issue is 400)
 *   3=STERILE may re-enter 2=STERILIZING (reprocess cycle).
 *
 * Guards pinned by tests:
 *   - assemble: kitCode required + unique; &gt;=1 item line; qty &gt; 0; expiryDays &gt; 0
 *     (defaults to 14 when not supplied)
 *   - start cycle: ONLY from ASSEMBLED or STERILE (reprocess); method required
 *   - complete cycle: PASS -&gt; STERILE (+ sterilized_at), FAIL -&gt; REJECTED;
 *     a cycle can be completed exactly once; result must be PASS|FAIL
 *   - issue: ONLY from STERILE (REJECTED kits are unusable -&gt; 400); issuedToRef
 *     required; single active issue per kit is structural (issue flips kit to ISSUED,
 *     and only a fresh PASS cycle returns it to STERILE)
 *
 * EVENT POLICY (register WP-R3): KitIssued / SterilizationCompleted are REJECTED
 * phase 1 per KAFKA-EVENT-CATALOG (no cross-service consumer evidence) — this service
 * emits NOTHING to the outbox on ANY transition; the integration test asserts the
 * outbox stays empty through every path below. Consuming ProcedureScheduled /
 * InventoryUpdated post-saga is comment-only (never silently invented here).
 */
@Service
public class CssdKitService {

    private static final Logger log = LoggerFactory.getLogger(CssdKitService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final KitRepository kitRepository;
    private final KitItemRepository kitItemRepository;
    private final SterilizationCycleRepository cycleRepository;
    private final KitIssueRepository issueRepository;

    @PersistenceContext
    private EntityManager em;

    public CssdKitService(KitRepository kitRepository,
                          KitItemRepository kitItemRepository,
                          SterilizationCycleRepository cycleRepository,
                          KitIssueRepository issueRepository) {
        this.kitRepository = kitRepository;
        this.kitItemRepository = kitItemRepository;
        this.cycleRepository = cycleRepository;
        this.issueRepository = issueRepository;
    }

    // ------------------------------------------------------------------ assemble

    /**
     * Assemble a new kit (status ASSEMBLED). Guards (400 BEXA003-style):
     * kitCode required (&lt;= 30 chars) and UNIQUE; at least one item line;
     * each line needs itemId and qty &gt; 0; expiryDays, if supplied, must be &gt; 0
     * (defaults to 14). Emits NOTHING to the outbox.
     */
    @Transactional
    public Kit create(String kitCode, String procedureType, List<ItemInput> items, Integer expiryDays) {
        if (kitCode == null || kitCode.isBlank()) {
            throw new InvalidKitRequestException("BEXA003", "kitCode is required");
        }
        if (kitCode.length() > 30) {
            throw new InvalidKitRequestException("BEXA003",
                    "kitCode must be at most 30 characters");
        }
        if (items == null || items.isEmpty()) {
            throw new InvalidKitRequestException("BEXA003",
                    "At least one kit item {itemId,qty} is required");
        }
        for (ItemInput item : items) {
            if (item == null || item.itemId() == null) {
                throw new InvalidKitRequestException("BEXA003",
                        "Each kit item needs a non-null itemId");
            }
            if (item.qty() == null || item.qty() <= 0) {
                throw new InvalidKitRequestException("BEXA003",
                        "Kit item qty must be > 0 for itemId " + item.itemId());
            }
        }
        int effectiveExpiry = expiryDays == null ? Kit.DEFAULT_EXPIRY_DAYS : expiryDays;
        if (effectiveExpiry <= 0) {
            throw new InvalidKitRequestException("BEXA003", "expiryDays must be > 0");
        }
        if (kitRepository.existsByKitCode(kitCode)) {
            throw new InvalidKitRequestException("CSSD_DUPLICATE_KIT_CODE",
                    "kitCode already exists: " + kitCode);
        }

        Timestamp nowTs = now();
        Kit kit = new Kit();
        kit.setKitId(nextId("t_kit_seq"));
        kit.setKitCode(kitCode);
        kit.setProcedureType(procedureType);
        kit.setStatus(Kit.STATUS_ASSEMBLED);
        kit.setExpiryDays(effectiveExpiry);
        kit.setAddedBy(AUDIT_ACTOR_ID);
        kit.setAddedDatetime(nowTs);
        try {
            kitRepository.save(kit);
        } catch (DataIntegrityViolationException e) {
            // race with a concurrent assemble of the same kitCode — translate to the same 400
            throw new InvalidKitRequestException("CSSD_DUPLICATE_KIT_CODE",
                    "kitCode already exists: " + kitCode);
        }

        List<KitItem> savedItems = new ArrayList<>();
        for (ItemInput item : items) {
            KitItem ki = new KitItem();
            ki.setKitId(kit.getKitId());
            ki.setItemId(item.itemId());
            ki.setQty(item.qty());
            ki.setAddedBy(AUDIT_ACTOR_ID);
            ki.setAddedDatetime(nowTs);
            kitItemRepository.save(ki);
            savedItems.add(ki);
        }
        log.info("Assembled kit {} ({}) with {} item(s)", kit.getKitId(), kitCode, savedItems.size());
        return get(kit.getKitId());
    }

    // ------------------------------------------------------------------ cycles

    /**
     * Start a sterilization cycle {method}. Guard: kit must be ASSEMBLED or STERILE
     * (reprocess) — anything else is 400 CSSD_ILLEGAL_TRANSITION. Moves the kit to
     * STERILIZING in the same tx. Emits NOTHING to the outbox.
     */
    @Transactional
    public SterilizationCycle startCycle(long kitId, String method) {
        Kit kit = loadKit(kitId);
        if (method == null || method.isBlank()) {
            throw new InvalidKitRequestException("BEXA003", "method is required");
        }
        if (!kit.canStartCycle()) {
            throw new InvalidKitRequestException("CSSD_ILLEGAL_TRANSITION",
                    "Cannot start a sterilization cycle for kit " + kitId + " from status "
                            + statusName(kit.getStatus())
                            + " — only ASSEMBLED or STERILE kits can enter a cycle");
        }
        Timestamp nowTs = now();
        SterilizationCycle cycle = new SterilizationCycle();
        cycle.setCycleId(nextId("t_sterilization_cycle_seq"));
        cycle.setKitId(kitId);
        cycle.setStartedAt(nowTs);
        cycle.setMethod(method);
        cycle.setAddedBy(AUDIT_ACTOR_ID);
        cycle.setAddedDatetime(nowTs);
        cycleRepository.save(cycle);

        kit.setStatus(Kit.STATUS_STERILIZING);
        kit.setUpdatedBy(AUDIT_ACTOR_ID);
        kit.setUpdatedDatetime(nowTs);
        kitRepository.save(kit);
        log.info("Started {} cycle {} for kit {}", method, cycle.getCycleId(), kitId);
        return cycle;
    }

    /**
     * Complete an open cycle with {result: PASS|FAIL}. Guards: unknown cycle 404;
     * already-completed cycle 400 CSSD_CYCLE_ALREADY_COMPLETED; result must parse to
     * PASS|FAIL (400 BEXA003). PASS -&gt; kit STERILE + sterilized_at stamped;
     * FAIL -&gt; kit REJECTED (unusable). Emits NOTHING to the outbox.
     */
    @Transactional
    public SterilizationCycle completeCycle(long cycleId, String rawResult) {
        SterilizationCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new CssdNotFoundException("NOT_FOUND",
                        "Sterilization cycle not found: " + cycleId));
        if (cycle.isComplete()) {
            throw new InvalidKitRequestException("CSSD_CYCLE_ALREADY_COMPLETED",
                    "Cycle " + cycleId + " was already completed");
        }
        short result = parseResult(rawResult);

        Timestamp nowTs = now();
        cycle.setResult(result);
        cycle.setCompletedAt(nowTs);
        cycle.setUpdatedBy(AUDIT_ACTOR_ID);
        cycle.setUpdatedDatetime(nowTs);
        cycleRepository.save(cycle);

        Kit kit = loadKit(cycle.getKitId());
        kit.setStatus(result == SterilizationCycle.RESULT_PASS
                ? Kit.STATUS_STERILE : Kit.STATUS_REJECTED);
        if (result == SterilizationCycle.RESULT_PASS) {
            kit.setSterilizedAt(nowTs);
        }
        kit.setUpdatedBy(AUDIT_ACTOR_ID);
        kit.setUpdatedDatetime(nowTs);
        kitRepository.save(kit);
        log.info("Completed cycle {} for kit {}: {} -> {}", cycleId, kit.getKitId(),
                rawResult, statusName(kit.getStatus()));
        return cycle;
    }

    // ------------------------------------------------------------------ issues

    /**
     * Issue a kit to an OT schedule / procedure ref {issuedToRef}. Guards:
     * issuedToRef required (BEXA003); kit must be STERILE (400 CSSD_ILLEGAL_TRANSITION
     * otherwise — REJECTED kits are explicitly unusable); issuing moves the kit to
     * ISSUED so a second concurrent/sequential issue cannot pass the guard again
     * (single active issue per kit). Emits NOTHING to the outbox.
     */
    @Transactional
    public KitIssue issue(long kitId, String issuedToRef) {
        Kit kit = loadKit(kitId);
        if (issuedToRef == null || issuedToRef.isBlank()) {
            throw new InvalidKitRequestException("BEXA003", "issuedToRef is required");
        }
        if (!kit.isSterile()) {
            String why = kit.isRejected()
                    ? " — REJECTED kits are unusable"
                    : " — only STERILE kits can be issued";
            throw new InvalidKitRequestException("CSSD_ILLEGAL_TRANSITION",
                    "Cannot issue kit " + kitId + " from status "
                            + statusName(kit.getStatus()) + why);
        }
        Timestamp nowTs = now();
        KitIssue issue = new KitIssue();
        issue.setIssueId(nextId("t_kit_issue_seq"));
        issue.setKitId(kitId);
        issue.setIssuedToRef(issuedToRef);
        issue.setIssuedAt(nowTs);
        issue.setAddedBy(AUDIT_ACTOR_ID);
        issue.setAddedDatetime(nowTs);
        issueRepository.save(issue);

        kit.setStatus(Kit.STATUS_ISSUED);
        kit.setUpdatedBy(AUDIT_ACTOR_ID);
        kit.setUpdatedDatetime(nowTs);
        kitRepository.save(kit);
        log.info("Issued kit {} to {}", kitId, issuedToRef);
        return issue;
    }

    // ------------------------------------------------------------------ queries

    /**
     * M11 CSSD store-dispatch: a kit issue is handed off to a store. The
     * legacy flow dispatched straight from the issue screen; here we keep
     * the two steps explicit so we can record dispatchedAt + receiver.
     */
    @Transactional
    public KitIssue dispatchIssue(long issueId, Long toStoreId) {
        KitIssue i = issueRepository.findById(issueId)
                .orElseThrow(() -> new CssdNotFoundException("NOT_FOUND", "Issue not found: " + issueId));
        i.setDispatchedToStoreId(toStoreId);
        i.setDispatchedAt(Timestamp.from(Instant.now()));
        stampUpdate(i);
        return issueRepository.save(i);
    }

    @Transactional
    public KitIssue receiveIssue(long issueId, String receivedBy) {
        KitIssue i = issueRepository.findById(issueId)
                .orElseThrow(() -> new CssdNotFoundException("NOT_FOUND", "Issue not found: " + issueId));
        if (i.getDispatchedAt() == null) {
            throw new CssdRuleException("CSSD_ILLEGAL_TRANSITION", "Issue has not been dispatched yet");
        }
        if (i.getReceivedAt() != null) {
            return i;
        }
        i.setReceivedAt(Timestamp.from(Instant.now()));
        i.setReceivedBy(receivedBy);
        stampUpdate(i);
        return issueRepository.save(i);
    }

    /**
     * M11 CSSD complete-cycle with quality test: BI + CI indicators + PASS/FAIL.
     * Updates the existing cycle and the linked kit status.
     */
    @Transactional
    public SterilizationCycle completeCycleWithQualityTest(long cycleId, short result,
                                                            String biologicalIndicator,
                                                            String chemicalIndicator) {
        SterilizationCycle c = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new CssdNotFoundException("NOT_FOUND", "Cycle not found: " + cycleId));
        c.setBiologicalIndicator(biologicalIndicator);
        c.setChemicalIndicator(chemicalIndicator);
        String raw = result == com.hims.cssd.domain.model.SterilizationCycle.RESULT_FAIL ? "FAIL" : "PASS";
        return completeCycle(cycleId, raw);
    }

    public Kit get(long kitId) {
        Kit kit = loadKit(kitId);
        kit.setItems(kitItemRepository.findByKitId(kitId));
        return kit;
    }

    @Transactional(readOnly = true)
    public List<Kit> list(Short status, String procedureType) {
        List<Kit> kits = kitRepository.search(status, procedureType);
        for (Kit k : kits) {
            k.setItems(kitItemRepository.findByKitId(k.getKitId()));
        }
        return kits;
    }

    @Transactional(readOnly = true)
    public List<KitIssue> issues(long kitId) {
        loadKit(kitId);   // 404 for unknown kit
        return issueRepository.findByKitIdOrderByIssuedAtDesc(kitId);
    }

    // ------------------------------------------------------------------ helpers

    private Kit loadKit(long kitId) {
        return kitRepository.findById(kitId)
                .orElseThrow(() -> new CssdNotFoundException("NOT_FOUND",
                        "Kit not found: " + kitId));
    }

    public static Short parseStatus(String raw) {
        try {
            return Short.valueOf(raw.trim());
        } catch (NumberFormatException ignored) {
            // fall through to name mapping
        }
        Short named = statusOf(raw.trim().toUpperCase());
        if (named == null) {
            throw new InvalidKitRequestException("BEXA003", "Unknown status: " + raw);
        }
        return named;
    }

    static Short statusOf(String name) {
        return switch (name) {
            case "ASSEMBLED" -> Kit.STATUS_ASSEMBLED;
            case "STERILIZING" -> Kit.STATUS_STERILIZING;
            case "STERILE" -> Kit.STATUS_STERILE;
            case "ISSUED" -> Kit.STATUS_ISSUED;
            case "REJECTED" -> Kit.STATUS_REJECTED;
            default -> null;
        };
    }

    public static String statusName(short s) {
        return switch (s) {
            case Kit.STATUS_ASSEMBLED -> "ASSEMBLED";
            case Kit.STATUS_STERILIZING -> "STERILIZING";
            case Kit.STATUS_STERILE -> "STERILE";
            case Kit.STATUS_ISSUED -> "ISSUED";
            case Kit.STATUS_REJECTED -> "REJECTED";
            default -> String.valueOf(s);
        };
    }

    /** Result filter/parse: PASS=1 / FAIL=2, case-insensitive. */
    static short parseResult(String raw) {
        if (raw != null) {
            switch (raw.trim().toUpperCase()) {
                case "PASS" -> { return SterilizationCycle.RESULT_PASS; }
                case "FAIL" -> { return SterilizationCycle.RESULT_FAIL; }
                default -> { }
            }
        }
        throw new InvalidKitRequestException("BEXA003",
                "result must be PASS or FAIL");
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_cssd." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }

    /** One requested kit content line at assembly. */
    public record ItemInput(Long itemId, Integer qty) {}

    private void stampUpdate(CssdBase entity) {
        entity.setUpdatedBy(AUDIT_ACTOR_ID);
        entity.setUpdatedDatetime(Timestamp.from(Instant.now()));
    }
}
