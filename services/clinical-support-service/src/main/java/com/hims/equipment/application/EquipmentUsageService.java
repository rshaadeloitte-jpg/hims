package com.hims.equipment.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.equipment.api.UsageReport;
import com.hims.equipment.application.exceptions.EquipmentNotFoundException;
import com.hims.equipment.application.exceptions.InvalidEquipmentRequestException;
import com.hims.equipment.domain.model.Equipment;
import com.hims.equipment.domain.model.EquipmentBase;
import com.hims.equipment.domain.model.EquipmentUsageDetails;
import com.hims.equipment.domain.model.EquipmentWorkingStatus;
import com.hims.equipment.domain.repository.EquipmentRepository;
import com.hims.equipment.domain.repository.EquipmentUsageDetailsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

/**
 * Equipment usage tracking (MIG-023 Phase 5, S18) — clean target of the legacy
 * T_EQUIPMENTUSAGEDETAILS flows on the V1 equipment_usage_details table:
 * log a usage session, read an asset's usage history and build a date-range
 * utilization report.
 *
 * <p>Preserved conventions:
 * <ul>
 *   <li>server-owned ids from explicit sequences (IMP-DEC-003) via native
 *       nextval — no auto-increment;</li>
 *   <li>{@code equipmentId} is server-owned from the path on log;</li>
 *   <li>gated-sentinel list/report queries (EquipmentRepository.search
 *       convention).</li>
 * </ul>
 *
 * <p>Documented improvements:
 * <ul>
 *   <li>{@code hoursUsed} is required and must be positive (a usage entry
 *       without machine-hours is meaningless) and is normalized to the
 *       NUMERIC(8,2) column scale with an explicit range guard so oversized
 *       values fail as BEXA003 instead of an SQL error;</li>
 *   <li>{@code usageDate} defaults to now when the key is absent (the entity
 *       has deliberately NO Java initializer so the null-check fires);</li>
 *   <li>logging is rejected for inactive AND CONDEMNED assets — unlike
 *       documents (condemnation paperwork) or complaints (faults found at
 *       condemnation), a condemned machine no longer operates, so new usage
 *       rows would be data errors;</li>
 *   <li>the report defaults to the trailing 30 days when no range is given,
 *       mirrors warranty's default alerting window, and computes totalHours
 *       from exactly the returned rows (one snapshot, no aggregate/query
 *       drift).</li>
 * </ul>
 */
@Service
public class EquipmentUsageService {

    private static final Logger log = LoggerFactory.getLogger(EquipmentUsageService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    /** NUMERIC(8,2) column ceiling — friendlier guard than an SQL overflow error. */
    private static final BigDecimal MAX_HOURS = new BigDecimal("999999.99");

    /** Report window applied when from/to are omitted (inclusive bounds). */
    private static final int DEFAULT_REPORT_DAYS = 30;

    private final EquipmentRepository equipmentRepository;
    private final EquipmentUsageDetailsRepository usageRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    public EquipmentUsageService(EquipmentRepository equipmentRepository,
                                 EquipmentUsageDetailsRepository usageRepository,
                                 ObjectMapper objectMapper) {
        this.equipmentRepository = equipmentRepository;
        this.usageRepository = usageRepository;
        this.objectMapper = objectMapper;
    }

    // ------------------------------------------------------------------- log

    /**
     * Log one usage session against an asset. Server stamps id
     * (equipment_usage_details_seq), pairs {@code equipmentId} from the path,
     * defaults {@code usageDate} to now and validates {@code hoursUsed}
     * (required, positive, fits NUMERIC(8,2)). Asset must exist, be active and
     * not be CONDEMNED.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EquipmentUsageDetails log(Long equipmentId, JsonNode payload, Long unitId) {
        if (payload == null || payload.isNull() || payload.isMissingNode()) {
            throw new InvalidEquipmentRequestException("BEXA003", "Invalid input data !");
        }
        Equipment asset = requireOperatingAsset(equipmentId);
        EquipmentUsageDetails usage = bind(payload);
        if (usage.getHoursUsed() == null
                || usage.getHoursUsed().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidEquipmentRequestException("BEXA003",
                    "hoursUsed is required and must be positive");
        }
        usage.setHoursUsed(usage.getHoursUsed().setScale(2, RoundingMode.HALF_UP));
        if (usage.getHoursUsed().compareTo(MAX_HOURS) > 0) {
            throw new InvalidEquipmentRequestException("BEXA003",
                    "hoursUsed exceeds the maximum supported value 999999.99");
        }
        usage.setOperator(trimToNull(usage.getOperator()));
        usage.setRemarks(trimToNull(usage.getRemarks()));
        if (usage.getUsageDate() == null) {            // key absent -> session happened now
            usage.setUsageDate(Timestamp.from(Instant.now()));
        }
        usage.setId(nextId("equipment_usage_details_seq"));
        usage.setEquipmentId(asset.getId());           // server-owned pairing
        usage.setStatus(usage.getStatus() == null ? true : usage.getStatus());
        stampCreate(usage, unitId);
        EquipmentUsageDetails saved = usageRepository.save(usage);
        log.info("Usage {} logged on equipment {} date={} hours={} operator={}",
                saved.getId(), saved.getEquipmentId(), saved.getUsageDate(),
                saved.getHoursUsed(), saved.getOperator());
        return saved;
    }

    // ------------------------------------------------------------------ reads

    /** Active usage entries of one asset — newest first. */
    @Transactional(readOnly = true)
    public List<EquipmentUsageDetails> history(Long equipmentId) {
        requireAsset(equipmentId); // 404 for unknown asset even when it has no entries
        return usageRepository.findByEquipmentIdAndStatusTrueOrderByIdDesc(equipmentId);
    }

    /**
     * Utilization report over a usage_date window. {@code from}/{@code to} are
     * inclusive ISO dates; either may be omitted (missing lower bound falls
     * back to today-29 so the default report covers the trailing 30 days,
     * missing upper bound falls back to today). {@code unitId}/
     * {@code equipmentId} scope the report when present. totalHours sums the
     * returned rows' hours_used — one query, one consistent snapshot.
     */
    @Transactional(readOnly = true)
    public UsageReport report(LocalDate from, LocalDate to, Long unitId, Long equipmentId) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidEquipmentRequestException("BEXA003",
                    "report 'from' must not be after 'to'");
        }
        LocalDate today = LocalDate.now();
        LocalDate effectiveFrom = from != null ? from : today.minusDays(DEFAULT_REPORT_DAYS - 1L);
        LocalDate effectiveTo = to != null ? to : today;

        List<EquipmentUsageDetails> rows = usageRepository.findReportRows(
                startOfDay(effectiveFrom),
                startOfDay(effectiveTo.plusDays(1)),          // inclusive 'to'
                unitId == null ? 0L : unitId, unitId != null,
                equipmentId == null ? 0L : equipmentId, equipmentId != null);

        BigDecimal totalHours = rows.stream()
                .map(EquipmentUsageDetails::getHoursUsed)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        log.info("Usage report [{} .. {}] unit={} equipment={}: {} entries, {} h",
                effectiveFrom, effectiveTo, unitId, equipmentId, rows.size(), totalHours);
        return new UsageReport(unitId, equipmentId, effectiveFrom, effectiveTo,
                rows.size(), totalHours, rows);
    }

    // --------------------------------------------------------------- helpers

    /** Existence only (history endpoint works for any real asset row). */
    private Equipment requireAsset(Long equipmentId) {
        return equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new EquipmentNotFoundException("NOT_FOUND",
                        "Equipment not found: " + equipmentId));
    }

    /**
     * Existence + activity + not-condemned guard for writes: a condemned
     * machine no longer operates, so logging fresh usage against it is a data
     * error (complaints/documents still accept condemned assets).
     */
    private Equipment requireOperatingAsset(Long equipmentId) {
        Equipment asset = requireAsset(equipmentId);
        if (!Boolean.TRUE.equals(asset.getStatus())) {
            throw new InvalidEquipmentRequestException("BEXA003",
                    "Cannot log usage against an inactive asset");
        }
        if (asset.getWorkingStatus() != null
                && asset.getWorkingStatus() == EquipmentWorkingStatus.CONDEMNED) {
            throw new InvalidEquipmentRequestException("BEXA003",
                    "Cannot log usage against a condemned asset");
        }
        return asset;
    }

    private EquipmentUsageDetails bind(JsonNode payload) {
        try {
            return objectMapper.treeToValue(payload, EquipmentUsageDetails.class);
        } catch (Exception e) {
            throw new InvalidEquipmentRequestException("BEXA003", "Invalid input data !");
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Timestamp startOfDay(LocalDate date) {
        return Timestamp.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private void stampCreate(EquipmentBase entity, Long unitId) {
        entity.setAddedBy(AUDIT_ACTOR_ID);
        entity.setAddedDatetime(Timestamp.from(Instant.now()));
        entity.setUpdatedBy(null);
        entity.setUpdatedDatetime(null);
        entity.setSynchronizedd(false);
        entity.setCreatedUnitId(unitId);
        entity.setUpdatedUnitId(unitId);
        if (unitId != null) {
            entity.setUnitId(unitId);
        }
    }

    private long nextId(String sequenceName) {
        return ((Number) em.createNativeQuery("SELECT nextval('ihms_equipment.\"" + sequenceName + "\"')")
                .getSingleResult()).longValue();
    }
}
