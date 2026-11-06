package com.hims.equipment.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.equipment.application.exceptions.EquipmentNotFoundException;
import com.hims.equipment.application.exceptions.InvalidEquipmentRequestException;
import com.hims.equipment.domain.model.Equipment;
import com.hims.equipment.domain.model.EquipmentBase;
import com.hims.equipment.domain.model.EquipmentPreventiveChecklist;
import com.hims.equipment.domain.model.EquipmentWorkingStatus;
import com.hims.equipment.domain.model.EqpPmSchedule;
import com.hims.equipment.domain.model.EqpPmScheduleDetails;
import com.hims.equipment.domain.model.EqpPreventiveMaintenance;
import com.hims.equipment.domain.model.PmStatus;
import com.hims.equipment.domain.repository.EquipmentPreventiveChecklistRepository;
import com.hims.equipment.domain.repository.EquipmentRepository;
import com.hims.equipment.domain.repository.EqpPmScheduleDetailsRepository;
import com.hims.equipment.domain.repository.EqpPmScheduleRepository;
import com.hims.equipment.domain.repository.EqpPreventiveMaintenanceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Preventive-maintenance lifecycle (MIG-023 Phase 3, S18) — clean target of
 * the legacy T_EqpPMSchedule / T_EqpPreventiveMaintenance flows.
 *
 * <p>Preserved conventions:
 * <ul>
 *   <li>server-owned ids from explicit sequences (IMP-DEC-003) via native
 *       nextval — no auto-increment;</li>
 *   <li>{@code pmType} holds a maintenance_type_master id (V1 seed, ids ARE
 *       the codes: 1=PREVENTIVE 2=CORRECTIVE 3=CALIBRATION), default
 *       PREVENTIVE; {@code pmStatus} is the execution lifecycle
 *       ({@link PmStatus}, default COMPLETED(3) when recording work);</li>
 *   <li>legacy month-of-year flags (jan..dec) pick which months a service
 *       falls in — a schedule created with no flag set gets the plan-start
 *       month flagged so the legacy view of the plan stays meaningful;</li>
 *   <li>{@code scheduleCode} / {@code maintenanceNo} are human-facing
 *       references ({@code IHMS/EQ/PM/{yyyy}/{%05d}} and
 *       {@code PM-{yyyy}-{%05d}}) driven by the table sequences, unique per
 *       partial index uq_eqp_pm_schedule_code for codes.</li>
 * </ul>
 *
 * <p>Documented improvement: completing a schedule advances its bookkeeping in
 * the same transaction as the execution row — {@code lastCompletedDate} is
 * stamped and {@code nextDueDate} rolls forward by {@code frequency} months.
 * Schedules can only be created on an existing, active, non-condemned asset
 * (clean BEXA003/404 instead of raw constraint violations). Recording a
 * completion deliberately does NOT re-check the asset state: it records work
 * performed against an existing plan, so history stays writable even if the
 * asset was deactivated after scheduling.
 */
@Service
public class PmService {

    private static final Logger log = LoggerFactory.getLogger(PmService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id
    public static final String SCHEDULE_CODE_PREFIX = "IHMS/EQ/PM/";
    public static final String MAINTENANCE_NO_PREFIX = "PM-";

    private final EquipmentRepository equipmentRepository;
    private final EqpPmScheduleRepository scheduleRepository;
    private final EqpPmScheduleDetailsRepository scheduleDetailsRepository;
    private final EqpPreventiveMaintenanceRepository maintenanceRepository;
    private final EquipmentPreventiveChecklistRepository checklistRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    public PmService(EquipmentRepository equipmentRepository,
                     EqpPmScheduleRepository scheduleRepository,
                     EqpPmScheduleDetailsRepository scheduleDetailsRepository,
                     EqpPreventiveMaintenanceRepository maintenanceRepository,
                     EquipmentPreventiveChecklistRepository checklistRepository,
                     ObjectMapper objectMapper) {
        this.equipmentRepository = equipmentRepository;
        this.scheduleRepository = scheduleRepository;
        this.scheduleDetailsRepository = scheduleDetailsRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.checklistRepository = checklistRepository;
        this.objectMapper = objectMapper;
    }

    // -------------------------------------------------------------- schedules

    /**
     * Create a PM schedule for an asset. Server stamps id (eqp_pm_schedule_seq),
     * scheduleCode ({@code IHMS/EQ/PM/{yyyy}/{%05d}} unless a unique code is
     * supplied), pmDate (now unless given) and nextDueDate (= pmDate unless
     * given, so a new plan is due immediately from its start). pmType defaults
     * to PREVENTIVE(1) and must be a valid dictionary id 1..3; frequency must
     * be positive when supplied. Optional nested {@code details[]} array saves
     * planned occurrence lines after the header (no JPA relationship mapping,
     * so parent-first insert order is mandatory).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EqpPmSchedule createSchedule(JsonNode payload, Long unitId) {
        if (payload == null || payload.isNull() || payload.isMissingNode()) {
            throw new InvalidEquipmentRequestException("BEXA003", "Invalid input data !");
        }
        EqpPmSchedule schedule = bind(payload, EqpPmSchedule.class);
        if (schedule.getEquipmentId() == null) {
            throw new InvalidEquipmentRequestException("BEXA003", "equipmentId is required");
        }
        Equipment asset = requireAsset(schedule.getEquipmentId());
        if (schedule.getPmType() == null) {
            schedule.setPmType(1L);
        } else if (!isValidPmType(schedule.getPmType())) {
            throw new InvalidEquipmentRequestException("BEXA003",
                    "Invalid pmType: " + schedule.getPmType()
                            + " (expected 1 PREVENTIVE, 2 CORRECTIVE, 3 CALIBRATION)");
        }
        if (schedule.getFrequency() != null && schedule.getFrequency() <= 0) {
            throw new InvalidEquipmentRequestException("BEXA003",
                    "frequency must be a positive number of months");
        }
        Timestamp now = Timestamp.from(Instant.now());
        if (schedule.getPmDate() == null) {
            schedule.setPmDate(now);
        }
        if (schedule.getNextDueDate() == null) {
            schedule.setNextDueDate(schedule.getPmDate());
        }
        if (schedule.getStoreId() == null) {
            schedule.setStoreId(asset.getStoreId());
        }
        applyDefaultMonthFlag(schedule);
        String requestedCode = schedule.getScheduleCode();
        if (requestedCode != null && !requestedCode.isBlank()) {
            schedule.setScheduleCode(requestedCode.trim());
            scheduleRepository.findByScheduleCodeIgnoreCase(schedule.getScheduleCode())
                    .ifPresent(existing -> {
                        throw new InvalidEquipmentRequestException("BEXA003",
                                "scheduleCode '" + schedule.getScheduleCode() + "' already exists");
                    });
        } else {
            schedule.setScheduleCode(generateScheduleCode());
        }
        schedule.setId(nextId("eqp_pm_schedule_seq"));
        schedule.setStatus(schedule.getStatus() == null ? true : schedule.getStatus());
        stampCreate(schedule, unitId);
        // Parent row MUST be inserted before its detail lines: there is no JPA
        // relationship mapping between the entities, so Hibernate has no
        // dependency information and would otherwise flush the child INSERTs
        // first — failing the pm_schedule_id FK at commit time.
        EqpPmSchedule saved = scheduleRepository.save(schedule);
        List<EqpPmScheduleDetails> lines = bindDetailLines(payload);
        for (EqpPmScheduleDetails line : lines) {
            line.setId(nextId("eqp_pm_schedule_details_seq"));
            line.setPmScheduleId(saved.getId());
            line.setStatus(line.getStatus() == null ? true : line.getStatus());
            stampCreate(line, unitId);
            scheduleDetailsRepository.save(line);
        }
        log.info("PM schedule {} created for equipment {} code={} nextDue={}",
                saved.getId(), saved.getEquipmentId(), saved.getScheduleCode(), saved.getNextDueDate());
        return saved;
    }

    /** PM schedules of one asset — newest first (asset PM plans). */
    @Transactional(readOnly = true)
    public List<EqpPmSchedule> schedulesOf(Long equipmentId) {
        requireAsset(equipmentId);
        return scheduleRepository.findByEquipmentIdAndStatusTrueOrderByIdDesc(equipmentId);
    }

    // ---------------------------------------------------------------- search

    /**
     * Schedule listing/search (unit, asset, pm type dictionary id, next-due
     * date range, free text over schedule_code). The due-date range on
     * {@code next_due_date} doubles as the due list: everything already fallen
     * due is {@code dueTo <= today}.
     */
    @Transactional(readOnly = true)
    public List<EqpPmSchedule> search(Long unitId, Long equipmentId, Long pmType,
                                      Timestamp dueFrom, Timestamp dueTo, String searchText) {
        validatePmType(pmType);
        String trimmed = (searchText == null || searchText.isBlank()) ? null : searchText.trim();
        // Sentinel values (0L / "") are bound instead of null: PostgreSQL cannot
        // type an untyped null parameter; the boolean gates ignore them when absent.
        return scheduleRepository.search(
                unitId == null ? 0L : unitId, unitId != null,
                equipmentId == null ? 0L : equipmentId, equipmentId != null,
                pmType == null ? 0L : pmType, pmType != null,
                dueFrom, dueFrom != null,
                dueTo, dueTo != null,
                trimmed == null ? "" : trimmed, trimmed != null);
    }

    // ------------------------------------------------------------- completion

    /**
     * Record a completed maintenance execution against a schedule. Server owns
     * equipmentId/pmScheduleId (taken from the schedule), assigns the id
     * (eqp_preventive_maintenance_seq), generates maintenanceNo
     * ({@code PM-{yyyy}-{%05d}} unless supplied), defaults pmStatus to
     * COMPLETED(3) and maintenanceDate to now. A supplied
     * {@code pmScheduleDetailId} must belong to the schedule. Atomically
     * advances the schedule: lastCompletedDate = maintenanceDate and
     * nextDueDate += frequency months (nextDueDate untouched when no
     * frequency is set on the plan).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EqpPreventiveMaintenance complete(Long scheduleId, JsonNode payload) {
        if (payload == null || payload.isNull() || payload.isMissingNode()) {
            throw new InvalidEquipmentRequestException("BEXA003", "Invalid input data !");
        }
        EqpPmSchedule schedule = getSchedule(scheduleId);
        EqpPreventiveMaintenance maintenance = bind(payload, EqpPreventiveMaintenance.class);
        // pmStatus defaults to COMPLETED when the key is absent: the entity field
        // carries the column default 1=SCHEDULED as a Java initializer (NOT NULL
        // insert safety), so null-checking the bound value cannot detect absence.
        if (!payload.hasNonNull("pmStatus")) {
            maintenance.setPmStatus(PmStatus.COMPLETED);
        } else if (!PmStatus.isValid(maintenance.getPmStatus())) {
            throw new InvalidEquipmentRequestException("BEXA003",
                    "Invalid pmStatus: " + maintenance.getPmStatus());
        }
        Timestamp now = Timestamp.from(Instant.now());
        Timestamp maintenanceDate = maintenance.getMaintenanceDate() == null ? now : maintenance.getMaintenanceDate();
        maintenance.setMaintenanceDate(maintenanceDate);

        if (maintenance.getPmScheduleDetailId() != null) {
            // Ownership check BEFORE any id is burned from the sequence.
            boolean belongs = scheduleDetailsRepository
                    .findByPmScheduleIdOrderByIdAsc(scheduleId).stream()
                    .anyMatch(d -> d.getId().equals(maintenance.getPmScheduleDetailId()));
            if (!belongs) {
                throw new InvalidEquipmentRequestException("BEXA003",
                        "pmScheduleDetailId " + maintenance.getPmScheduleDetailId()
                                + " does not belong to schedule " + scheduleId);
            }
        }
        maintenance.setId(nextId("eqp_preventive_maintenance_seq"));
        maintenance.setPmScheduleId(schedule.getId());          // server-owned pairing
        maintenance.setEquipmentId(schedule.getEquipmentId());  // server-owned pairing
        if (maintenance.getItemId() == null) {
            equipmentRepository.findById(schedule.getEquipmentId())
                    .ifPresent(asset -> maintenance.setItemId(asset.getItemId()));
        }
        if (maintenance.getMaintenanceNo() == null || maintenance.getMaintenanceNo().isBlank()) {
            maintenance.setMaintenanceNo(generateMaintenanceNo());
        }
        maintenance.setStatus(maintenance.getStatus() == null ? true : maintenance.getStatus());
        stampCreate(maintenance, schedule.getUnitId());
        EqpPreventiveMaintenance saved = maintenanceRepository.save(maintenance);

        schedule.setLastCompletedDate(maintenanceDate);
        if (schedule.getFrequency() != null && schedule.getFrequency() > 0) {
            LocalDateTime nextDue = maintenanceDate.toInstant().atZone(ZoneId.systemDefault())
                    .toLocalDateTime().plusMonths(schedule.getFrequency());
            schedule.setNextDueDate(Timestamp.valueOf(nextDue));
        }
        stampUpdate(schedule);
        scheduleRepository.save(schedule);
        log.info("PM {} recorded on schedule {} -> lastCompleted={}, nextDue={}",
                saved.getId(), schedule.getId(), schedule.getLastCompletedDate(), schedule.getNextDueDate());
        return saved;
    }

    /** Execution history of one schedule — chronological (oldest first). */
    @Transactional(readOnly = true)
    public List<EqpPreventiveMaintenance> history(Long scheduleId) {
        getSchedule(scheduleId); // 404 for unknown schedule
        return maintenanceRepository.findByPmScheduleIdOrderByIdAsc(scheduleId);
    }

    // ------------------------------------------------------------- checklists

    /** All active PM checklist templates — template order. */
    @Transactional(readOnly = true)
    public List<EquipmentPreventiveChecklist> listChecklists() {
        return checklistRepository.findByStatusTrueOrderByIdAsc();
    }

    /**
     * Create a checklist template. code/description are NOT NULL columns —
     * both required (BEXA003 otherwise); codes are unique case-insensitively.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EquipmentPreventiveChecklist createChecklist(JsonNode payload, Long unitId) {
        if (payload == null || payload.isNull() || payload.isMissingNode()) {
            throw new InvalidEquipmentRequestException("BEXA003", "Invalid input data !");
        }
        EquipmentPreventiveChecklist checklist = bind(payload, EquipmentPreventiveChecklist.class);
        if (checklist.getCode() == null || checklist.getCode().isBlank()) {
            throw new InvalidEquipmentRequestException("BEXA003", "code is required");
        }
        if (checklist.getDescription() == null || checklist.getDescription().isBlank()) {
            throw new InvalidEquipmentRequestException("BEXA003", "description is required");
        }
        checklist.setCode(checklist.getCode().trim());
        checklist.setDescription(checklist.getDescription().trim());
        checklistRepository.findByCodeIgnoreCase(checklist.getCode())
                .ifPresent(existing -> {
                    throw new InvalidEquipmentRequestException("BEXA003",
                            "Checklist code '" + checklist.getCode() + "' already exists");
                });
        checklist.setId(nextId("equipment_preventive_checklist_seq"));
        checklist.setStatus(checklist.getStatus() == null ? true : checklist.getStatus());
        stampCreate(checklist, unitId);
        EquipmentPreventiveChecklist saved = checklistRepository.save(checklist);
        log.info("PM checklist template {} created ({})", saved.getId(), saved.getCode());
        return saved;
    }

    // --------------------------------------------------------------- helpers

    private Equipment requireAsset(Long equipmentId) {
        Equipment asset = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new EquipmentNotFoundException("NOT_FOUND",
                        "Equipment not found: " + equipmentId));
        if (!Boolean.TRUE.equals(asset.getStatus())) {
            throw new InvalidEquipmentRequestException("BEXA003",
                    "Cannot schedule preventive maintenance on an inactive asset");
        }
        if (asset.getWorkingStatus() != null
                && asset.getWorkingStatus() == EquipmentWorkingStatus.CONDEMNED) {
            throw new InvalidEquipmentRequestException("BEXA003",
                    "Cannot schedule preventive maintenance on a condemned asset");
        }
        return asset;
    }

    private void validatePmType(Long pmType) {
        if (pmType != null && !isValidPmType(pmType)) {
            throw new InvalidEquipmentRequestException("BEXA003",
                    "Invalid pmType: " + pmType
                            + " (expected 1 PREVENTIVE, 2 CORRECTIVE, 3 CALIBRATION)");
        }
    }

    /** maintenance_type_master V1 seed: ids ARE the codes 1..3. */
    private boolean isValidPmType(Long pmType) {
        return pmType != null && pmType >= 1L && pmType <= 3L;
    }

    /**
     * Legacy month flags: when the payload sets none of jan..dec, flag the
     * plan-start month so the recurring-month view stays meaningful.
     */
    private void applyDefaultMonthFlag(EqpPmSchedule schedule) {
        boolean anyFlagSet = schedule.getJan() || schedule.getFeb() || schedule.getMar()
                || schedule.getApr() || schedule.getMay() || schedule.getJun()
                || schedule.getJul() || schedule.getAug() || schedule.getSep()
                || schedule.getOct() || schedule.getNov() || schedule.getDec();
        if (anyFlagSet || schedule.getPmDate() == null) {
            return;
        }
        int month = schedule.getPmDate().toInstant().atZone(ZoneId.systemDefault())
                .toLocalDateTime().getMonthValue();
        switch (month) {
            case 1 -> schedule.setJan(true);
            case 2 -> schedule.setFeb(true);
            case 3 -> schedule.setMar(true);
            case 4 -> schedule.setApr(true);
            case 5 -> schedule.setMay(true);
            case 6 -> schedule.setJun(true);
            case 7 -> schedule.setJul(true);
            case 8 -> schedule.setAug(true);
            case 9 -> schedule.setSep(true);
            case 10 -> schedule.setOct(true);
            case 11 -> schedule.setNov(true);
            default -> schedule.setDec(true);
        }
    }

    /** Optional {@code details[]} planned-occurrence lines on a create payload. */
    private List<EqpPmScheduleDetails> bindDetailLines(JsonNode payload) {
        List<EqpPmScheduleDetails> lines = new java.util.ArrayList<>();
        JsonNode details = payload.path("details");
        if (details.isArray()) {
            for (JsonNode node : details) {
                try {
                    lines.add(objectMapper.treeToValue(node, EqpPmScheduleDetails.class));
                } catch (Exception e) {
                    throw new InvalidEquipmentRequestException("BEXA003", "Invalid input data !");
                }
            }
        }
        return lines;
    }

    private EqpPmSchedule getSchedule(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new EquipmentNotFoundException("NOT_FOUND", "PM schedule not found"));
    }

    /** Schedule reference rule: IHMS/EQ/PM/{yyyy}/{%05d seq}. */
    private String generateScheduleCode() {
        long seq = nextId("eqp_pm_schedule_seq");
        int year = LocalDate.now(ZoneId.systemDefault()).getYear();
        return SCHEDULE_CODE_PREFIX + year + "/" + String.format("%05d", seq);
    }

    /** Execution reference rule: PM-{yyyy}-{%05d seq}. */
    private String generateMaintenanceNo() {
        long seq = nextId("eqp_preventive_maintenance_seq");
        int year = LocalDate.now(ZoneId.systemDefault()).getYear();
        return MAINTENANCE_NO_PREFIX + year + "-" + String.format("%05d", seq);
    }

    private <T> T bind(JsonNode payload, Class<T> type) {
        try {
            return objectMapper.treeToValue(payload, type);
        } catch (Exception e) {
            throw new InvalidEquipmentRequestException("BEXA003", "Invalid input data !");
        }
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

    private void stampUpdate(EquipmentBase entity) {
        entity.setUpdatedBy(AUDIT_ACTOR_ID);
        entity.setUpdatedDatetime(Timestamp.from(Instant.now()));
    }

    private long nextId(String sequenceName) {
        return ((Number) em.createNativeQuery("SELECT nextval('ihms_equipment.\"" + sequenceName + "\"')")
                .getSingleResult()).longValue();
    }
}
