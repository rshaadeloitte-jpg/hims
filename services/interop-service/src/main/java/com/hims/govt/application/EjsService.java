package com.hims.govt.application;

import com.hims.govt.application.exceptions.GovtConflictException;
import com.hims.govt.application.exceptions.GovtNotFoundException;
import com.hims.govt.application.exceptions.InvalidGovtRequestException;
import com.hims.govt.domain.model.EjsAsha;
import com.hims.govt.domain.model.EjsIndicatorEntry;
import com.hims.govt.domain.model.EjsIncentive;
import com.hims.govt.domain.model.EjsSupervisor;
import com.hims.govt.domain.model.IncentiveRate;
import com.hims.govt.domain.repository.EjsAshaRepository;
import com.hims.govt.domain.repository.EjsIndicatorEntryRepository;
import com.hims.govt.domain.repository.EjsIncentiveRepository;
import com.hims.govt.domain.repository.EjsSupervisorRepository;
import com.hims.govt.domain.repository.IncentiveRateRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * EJS ASHA / supervisor / indicator entries + incentive compute (GAP-REGISTER-6 WP-T4).
 *
 * Guards & rules pinned by tests:
 *   - createAsha: code + name required; duplicate ashaCode -> 409
 *     GOVT_DUPLICATE_ASHA_CODE; supervisorId, when supplied, must reference an
 *     existing t_ejs_supervisor row (400 GOVT_UNKNOWN_SUPERVISOR — supervisors
 *     are reference data with NO creation API in this slice)
 *   - upsertIndicator: asha must exist (404); indicatorCode + periodYear required
 *     and periodMonth 1..12 (BEXA003); value required, >= 0 (BEXA003); upsert on
 *     UNIQUE(asha_id, indicator_code, period_year, period_month)
 *   - compute: amount = SUM(entry.value x rate) over the period's entries
 *     (entries whose indicator_code has no rate row contribute 0 — documented);
 *     recompute REPLACES prior COMPUTED rows only — APPROVED/PAID rows are
 *     immutable history (register-pinned rule)
 *   - approve: only from COMPUTED (1 -> 2); pay: only from APPROVED (2 -> 3);
 *     anything else is 400 GOVT_ILLEGAL_TRANSITION
 *
 * EVENT POLICY (register WP-T4): IncentiveComputed is DEFERRED — this service
 * emits NOTHING to the outbox on ANY path (asserted by the integration test).
 * Full EJS module-125 endpoint parity is OUT OF SCOPE (comment-only).
 */
@Service
public class EjsService {

    private static final Logger log = LoggerFactory.getLogger(EjsService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final EjsAshaRepository ashaRepository;
    private final EjsSupervisorRepository supervisorRepository;
    private final EjsIndicatorEntryRepository entryRepository;
    private final EjsIncentiveRepository incentiveRepository;
    private final IncentiveRateRepository rateRepository;

    @PersistenceContext
    private EntityManager em;

    public EjsService(EjsAshaRepository ashaRepository,
                      EjsSupervisorRepository supervisorRepository,
                      EjsIndicatorEntryRepository entryRepository,
                      EjsIncentiveRepository incentiveRepository,
                      IncentiveRateRepository rateRepository) {
        this.ashaRepository = ashaRepository;
        this.supervisorRepository = supervisorRepository;
        this.entryRepository = entryRepository;
        this.incentiveRepository = incentiveRepository;
        this.rateRepository = rateRepository;
    }

    // ------------------------------------------------------------------ asha

    /**
     * Register an ASHA worker {code, name, supervisorId?}. Guards: code + name
     * required; duplicate ashaCode -> 409 GOVT_DUPLICATE_ASHA_CODE; a supplied
     * supervisorId must exist (400 GOVT_UNKNOWN_SUPERVISOR). Emits NOTHING.
     */
    @Transactional
    public EjsAsha createAsha(String code, String name, Long supervisorId) {
        if (code == null || code.isBlank()) {
            throw new InvalidGovtRequestException("BEXA003", "code is required");
        }
        if (code.length() > 20) {
            throw new InvalidGovtRequestException("BEXA003", "code must be at most 20 characters");
        }
        if (name == null || name.isBlank()) {
            throw new InvalidGovtRequestException("BEXA003", "name is required");
        }
        if (supervisorId != null && !supervisorRepository.existsById(supervisorId)) {
            throw new InvalidGovtRequestException("GOVT_UNKNOWN_SUPERVISOR",
                    "supervisor not found: " + supervisorId);
        }
        if (ashaRepository.existsByAshaCode(code)) {
            throw new GovtConflictException("GOVT_DUPLICATE_ASHA_CODE",
                    "asha code already exists: " + code);
        }

        Timestamp nowTs = now();
        EjsAsha asha = new EjsAsha();
        asha.setAshaId(nextId("t_ejs_asha_seq"));
        asha.setAshaCode(code);
        asha.setName(name);
        asha.setSupervisorId(supervisorId);
        asha.setAddedBy(AUDIT_ACTOR_ID);
        asha.setAddedDatetime(nowTs);
        try {
            ashaRepository.save(asha);
        } catch (DataIntegrityViolationException ex) {
            // race with a concurrent register of the same ashaCode — same 409 contract
            throw new GovtConflictException("GOVT_DUPLICATE_ASHA_CODE",
                    "asha code already exists: " + code);
        }
        log.info("Registered ASHA {} ({})", code, asha.getAshaId());
        return asha;
    }

    /** Supervisor reference lookup (no creation API in this slice). */
    @Transactional(readOnly = true)
    public EjsSupervisor getSupervisor(long supervisorId) {
        return supervisorRepository.findById(supervisorId)
                .orElseThrow(() -> new GovtNotFoundException("NOT_FOUND",
                        "Supervisor not found: " + supervisorId));
    }

    // ------------------------------------------------------------------ indicators

    /**
     * Upsert one monthly indicator value {ashaId, indicatorCode, periodYear,
     * periodMonth, value} on UNIQUE(asha_id, indicator_code, period_year,
     * period_month): an existing key is updated in place, otherwise inserted.
     * Emits NOTHING to the outbox.
     */
    @Transactional
    public EjsIndicatorEntry upsertIndicator(Long ashaId, String indicatorCode,
                                             Integer periodYear, Integer periodMonth, Integer value) {
        loadAsha(ashaId);   // 404 for unknown ASHA
        validatePeriod(indicatorCode, periodYear, periodMonth);
        if (value == null) {
            throw new InvalidGovtRequestException("BEXA003", "value is required");
        }
        if (value < 0) {
            throw new InvalidGovtRequestException("BEXA003", "value must be >= 0");
        }

        Timestamp nowTs = now();
        EjsIndicatorEntry entry = entryRepository
                .findByAshaIdAndIndicatorCodeAndPeriodYearAndPeriodMonth(
                        ashaId, indicatorCode, periodYear, periodMonth)
                .orElseGet(() -> {
                    EjsIndicatorEntry e = new EjsIndicatorEntry();
                    e.setEntryId(nextId("t_ejs_indicator_entry_seq"));
                    e.setAshaId(ashaId);
                    e.setIndicatorCode(indicatorCode);
                    e.setPeriodYear(periodYear);
                    e.setPeriodMonth(periodMonth);
                    e.setAddedBy(AUDIT_ACTOR_ID);
                    e.setAddedDatetime(nowTs);
                    return e;
                });
        boolean isNew = entry.getId() == null;
        entry.setValue(value);
        entry.setUpdatedBy(AUDIT_ACTOR_ID);
        entry.setUpdatedDatetime(nowTs);
        entryRepository.save(entry);
        log.info("{} indicator {} for ASHA {} {}/{} = {}", isNew ? "Inserted" : "Updated",
                indicatorCode, ashaId, periodYear, periodMonth, value);
        return entry;
    }

    // ------------------------------------------------------------------ incentives

    /**
     * Compute the incentive for one ASHA + period: amount = SUM(entry.value x rate)
     * across the period's indicator entries (an entry whose indicator_code has no
     * t_incentive_rate row contributes 0 — documented behavior). RECOMPUTE RULE
     * (register-pinned): prior COMPUTED rows of the same period are REPLACED
     * (deleted), while APPROVED/PAID rows are left untouched as immutable history;
     * a fresh COMPUTED row is always produced. Emits NOTHING to the outbox.
     */
    @Transactional
    public EjsIncentive compute(Long ashaId, Integer periodYear, Integer periodMonth) {
        loadAsha(ashaId);   // 404 for unknown ASHA
        validatePeriod(null, periodYear, periodMonth);

        List<EjsIndicatorEntry> entries =
                entryRepository.findByAshaIdAndPeriodYearAndPeriodMonth(ashaId, periodYear, periodMonth);
        BigDecimal amount = BigDecimal.ZERO.setScale(2);
        for (EjsIndicatorEntry entry : entries) {
            BigDecimal rate = rateRepository.findById(entry.getIndicatorCode())
                    .map(IncentiveRate::getRate)
                    .orElse(BigDecimal.ZERO);   // documented: no rate row -> contributes 0
            amount = amount.add(rate.multiply(BigDecimal.valueOf(entry.getValue())));
        }
        BigDecimal finalAmount = maxZero(amount);

        // recompute replaces prior COMPUTED rows ONLY — APPROVED/PAID are history
        List<EjsIncentive> existing =
                incentiveRepository.findByAshaIdAndPeriodYearAndPeriodMonth(ashaId, periodYear, periodMonth);
        List<Long> replaced = existing.stream()
                .filter(EjsIncentive::isComputed)
                .map(EjsIncentive::getIncentiveId)
                .toList();
        if (!replaced.isEmpty()) {
            incentiveRepository.deleteAllById(replaced);
        }

        Timestamp nowTs = now();
        EjsIncentive incentive = new EjsIncentive();
        incentive.setIncentiveId(nextId("t_ejs_incentive_seq"));
        incentive.setAshaId(ashaId);
        incentive.setPeriodYear(periodYear);
        incentive.setPeriodMonth(periodMonth);
        incentive.setAmount(finalAmount);
        incentive.setStatus(EjsIncentive.STATUS_COMPUTED);
        incentive.setComputedAt(nowTs);
        incentive.setAddedBy(AUDIT_ACTOR_ID);
        incentive.setAddedDatetime(nowTs);
        incentiveRepository.save(incentive);
        log.info("Computed incentive {} for ASHA {} {}/{}: amount={} (replaced COMPUTED rows: {})",
                incentive.getIncentiveId(), ashaId, periodYear, periodMonth, finalAmount, replaced.size());
        return incentive;
    }

    /**
     * Approve a COMPUTED incentive (1 -&gt; 2). Guards: unknown id -&gt; 404;
     * anything but COMPUTED -&gt; 400 GOVT_ILLEGAL_TRANSITION. Emits NOTHING.
     */
    @Transactional
    public EjsIncentive approve(long incentiveId) {
        EjsIncentive incentive = loadIncentive(incentiveId);
        if (!incentive.isComputed()) {
            throw new InvalidGovtRequestException("GOVT_ILLEGAL_TRANSITION",
                    "Cannot approve incentive " + incentiveId + " from status "
                            + statusName(incentive.getStatus()) + " — only COMPUTED can be approved");
        }
        incentive.setStatus(EjsIncentive.STATUS_APPROVED);
        incentive.setUpdatedBy(AUDIT_ACTOR_ID);
        incentive.setUpdatedDatetime(now());
        incentiveRepository.save(incentive);
        log.info("Approved incentive {}", incentiveId);
        return incentive;
    }

    /**
     * Pay an APPROVED incentive (2 -&gt; 3). Guards: unknown id -&gt; 404;
     * anything but APPROVED -&gt; 400 GOVT_ILLEGAL_TRANSITION. Emits NOTHING.
     */
    @Transactional
    public EjsIncentive pay(long incentiveId) {
        EjsIncentive incentive = loadIncentive(incentiveId);
        if (!incentive.isApproved()) {
            throw new InvalidGovtRequestException("GOVT_ILLEGAL_TRANSITION",
                    "Cannot pay incentive " + incentiveId + " from status "
                            + statusName(incentive.getStatus()) + " — only APPROVED can be paid");
        }
        incentive.setStatus(EjsIncentive.STATUS_PAID);
        incentive.setUpdatedBy(AUDIT_ACTOR_ID);
        incentive.setUpdatedDatetime(now());
        incentiveRepository.save(incentive);
        log.info("Paid incentive {}", incentiveId);
        return incentive;
    }

    @Transactional(readOnly = true)
    public List<EjsIncentive> listIncentives(Short status, Long ashaId) {
        return incentiveRepository.search(status, ashaId);
    }

    // ------------------------------------------------------------------ helpers

    private EjsAsha loadAsha(Long ashaId) {
        if (ashaId == null) {
            throw new InvalidGovtRequestException("BEXA003", "ashaId is required");
        }
        return ashaRepository.findById(ashaId)
                .orElseThrow(() -> new GovtNotFoundException("NOT_FOUND", "ASHA not found: " + ashaId));
    }

    private EjsIncentive loadIncentive(long incentiveId) {
        return incentiveRepository.findById(incentiveId)
                .orElseThrow(() -> new GovtNotFoundException("NOT_FOUND",
                        "Incentive not found: " + incentiveId));
    }

    /** Shared validation for the period inputs (compute + upsert). */
    private void validatePeriod(String indicatorCode, Integer periodYear, Integer periodMonth) {
        if (indicatorCode != null && indicatorCode.isBlank()) {
            throw new InvalidGovtRequestException("BEXA003", "indicatorCode is required");
        }
        if (periodYear == null) {
            throw new InvalidGovtRequestException("BEXA003", "periodYear is required");
        }
        if (periodMonth == null || periodMonth < 1 || periodMonth > 12) {
            throw new InvalidGovtRequestException("BEXA003",
                    "periodMonth must be between 1 and 12 — got: " + periodMonth);
        }
    }

    /** NUMERIC(19,2) CHECK >= 0 backstop: never persist a negative sum. */
    private static BigDecimal maxZero(BigDecimal amount) {
        return amount.signum() < 0 ? BigDecimal.ZERO.setScale(2) : amount;
    }

    public static Short parseStatus(String raw) {
        try {
            return Short.valueOf(raw.trim());
        } catch (NumberFormatException ignored) {
            // fall through to name mapping
        }
        Short named = switch (raw == null ? "" : raw.trim().toUpperCase()) {
            case "COMPUTED" -> EjsIncentive.STATUS_COMPUTED;
            case "APPROVED" -> EjsIncentive.STATUS_APPROVED;
            case "PAID" -> EjsIncentive.STATUS_PAID;
            default -> null;
        };
        if (named == null) {
            throw new InvalidGovtRequestException("BEXA003", "Unknown status: " + raw);
        }
        return named;
    }

    public static String statusName(short s) {
        return switch (s) {
            case EjsIncentive.STATUS_COMPUTED -> "COMPUTED";
            case EjsIncentive.STATUS_APPROVED -> "APPROVED";
            case EjsIncentive.STATUS_PAID -> "PAID";
            default -> String.valueOf(s);
        };
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_govt." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }
}
