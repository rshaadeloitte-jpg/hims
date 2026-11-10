package com.hims.ot.application;

import com.hims.ot.application.exceptions.InvalidOtRequestException;
import com.hims.ot.application.exceptions.OtEntityNotFoundException;
import com.hims.ot.domain.model.OtSchedule;
import com.hims.ot.domain.model.OtScheduleDetail;
import com.hims.ot.domain.repository.OtScheduleDetailRepository;
import com.hims.ot.domain.repository.OtScheduleRepository;
import com.hims.ot.infrastructure.outbox.OutboxService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OT scheduling core (GAP-REGISTER-3 WP-8, Catalog #11).
 *
 * Business rules:
 *  - BR-OT-001 prerequisites: the caller must pass admissionId (active admission)
 *    OR encounterId (latest visit); neither -> HTTP 400 code BEOT004. Cross-service
 *    validation of the referenced ids is DEFERRED — ids are treated as opaque
 *    here (same boundary as nursing service); ADT/IPD owns admission truth.
 *    Dependent-surgery detail rows (dependent_of set) get discount_pct applied
 *    from the request; procedure rate defaults to 0 when not supplied (tariff
 *    lookup deferred to Reference integration).
 *  - BR-OT-002 pre-billing: the pre_billing flag is persisted on t_otschedule and
 *    echoed in every response. The actual procedure-specialization direct-charge
 *    creation (legacy consumed=true path) is DEFERRED to Billing (Wave 4) —
 *    this service intentionally does NOT create charges.
 *
 * Lifecycle guards (status 1=PLANNED 2=IN_PROGRESS 3=DONE 4=CANCELLED):
 *  - start:    PLANNED -> IN_PROGRESS only
 *  - complete: IN_PROGRESS -> DONE only
 *  - cancel:   PLANNED|IN_PROGRESS -> CANCELLED only (never from DONE)
 * Illegal transitions -> HTTP 400 (OT_ILLEGAL_TRANSITION).
 *
 * Events via transactional outbox (topic ihms.ot.events, partition key patientId):
 *  - ProcedureScheduled on create, SurgeryCompleted on complete. BOTH ARE
 *    PROPOSED per KAFKA-EVENT-CATALOG §3 ("weak consumer evidence" — CSSD/
 *    Inventory consumption unconfirmed); they are emitted behind the outbox and
 *    flagged PROPOSED in code + report, never silently promoted.
 */
@Service
public class OtScheduleService {

    private static final Logger log = LoggerFactory.getLogger(OtScheduleService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    public static final String ERR_PREREQUISITE = "BEOT004";
    public static final String ERR_ILLEGAL_TRANSITION = "OT_ILLEGAL_TRANSITION";

    private final OtScheduleRepository scheduleRepository;
    private final OtScheduleDetailRepository detailRepository;
    private final OutboxService outboxService;

    @PersistenceContext
    private EntityManager em;

    public OtScheduleService(OtScheduleRepository scheduleRepository,
                             OtScheduleDetailRepository detailRepository,
                             OutboxService outboxService) {
        this.scheduleRepository = scheduleRepository;
        this.detailRepository = detailRepository;
        this.outboxService = outboxService;
    }

    // ------------------------------------------------------------------ create

    /**
     * Schedule an OT: BR-OT-001 prerequisite (admissionId OR encounterId, else
     * BEOT004), persists schedule + procedure details with dependent-surgery
     * discounts, emits PROPOSED ProcedureScheduled via the outbox.
     */
    @Transactional
    public OtSchedule create(Long patientId,
                             Long encounterId,
                             Long admissionId,
                             String otTable,
                             Timestamp scheduledStart,
                             Timestamp scheduledEnd,
                             Long anesthetistId,
                             Long surgeonId,
                             Boolean preBilling,
                             List<ProcedureRequest> procedures) {
        if (patientId == null) {
            throw new InvalidOtRequestException("OT_REQ_INVALID", "patientId is required");
        }
        // BR-OT-001: patient needs an active admission (admissionId) or at least a
        // visit (encounterId); none supplied -> BEOT004. Referenced ids are opaque:
        // existence validation across services is deferred (documented boundary).
        if (encounterId == null && admissionId == null) {
            throw new InvalidOtRequestException(ERR_PREREQUISITE,
                    "BR-OT-001: either admissionId or encounterId must be supplied");
        }
        if (otTable == null || otTable.isBlank()) {
            throw new InvalidOtRequestException("OT_REQ_INVALID", "otTable is required");
        }
        if (scheduledStart == null) {
            throw new InvalidOtRequestException("OT_REQ_INVALID", "scheduledStart is required");
        }
        if (procedures == null || procedures.isEmpty()) {
            throw new InvalidOtRequestException("OT_REQ_INVALID", "at least one procedure is required");
        }

        OtSchedule schedule = new OtSchedule();
        schedule.setScheduleId(nextId("t_otschedule_seq"));
        schedule.setPatientId(patientId);
        schedule.setEncounterId(encounterId);
        schedule.setAdmissionId(admissionId);
        schedule.setOtTable(otTable);
        schedule.setScheduledStart(scheduledStart);
        schedule.setScheduledEnd(scheduledEnd);
        schedule.setAnesthetistId(anesthetistId);
        schedule.setSurgeonId(surgeonId);
        schedule.setStatus(OtSchedule.STATUS_PLANNED);
        schedule.setPreBilling(Boolean.TRUE.equals(preBilling));   // BR-OT-002 flag persisted; charge creation deferred to Billing Wave 4
        schedule.setUnitId(null);
        schedule.setAddedBy(AUDIT_ACTOR_ID);
        schedule.setAddedDatetime(Timestamp.from(Instant.now()));
        scheduleRepository.save(schedule);

        Map<Integer, Long> indexToDetailId = new HashMap<>();
        List<OtScheduleDetail> savedDetails = new ArrayList<>();
        for (int i = 0; i < procedures.size(); i++) {
            ProcedureRequest pr = procedures.get(i);
            if (pr.procedureCode() == null || pr.procedureCode().isBlank()) {
                throw new InvalidOtRequestException("OT_REQ_INVALID",
                        "procedures[" + i + "].procedureCode is required");
            }
            Long dependentOf = null;
            BigDecimal discountPct = BigDecimal.ZERO;
            if (pr.dependentOfIndex() != null) {
                // BR-OT-001 dependent surgery: must reference an earlier row of THIS request
                Integer idx = pr.dependentOfIndex();
                if (idx < 0 || idx >= i) {
                    throw new InvalidOtRequestException("OT_REQ_INVALID",
                            "procedures[" + i + "].dependentOfIndex must reference an earlier procedure in the same request");
                }
                dependentOf = indexToDetailId.get(idx);
                discountPct = pr.discountPct() == null ? BigDecimal.ZERO : pr.discountPct();
                if (discountPct.compareTo(BigDecimal.ZERO) < 0 || discountPct.compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new InvalidOtRequestException("OT_REQ_INVALID",
                            "procedures[" + i + "].discountPct must be within 0..100");
                }
            } else if (pr.discountPct() != null && pr.discountPct().compareTo(BigDecimal.ZERO) != 0) {
                throw new InvalidOtRequestException("OT_REQ_INVALID",
                        "procedures[" + i + "].discountPct applies only to dependent surgeries (dependentOfIndex required)");
            }

            OtScheduleDetail d = new OtScheduleDetail();
            d.setDetailId(nextId("t_otscheduledetail_seq"));
            d.setScheduleId(schedule.getScheduleId());
            d.setProcedureCode(pr.procedureCode());
            d.setProcedureName(pr.procedureName() == null ? pr.procedureCode() : pr.procedureName());
            // rate defaults 0 when not supplied — tariff lookup deferred to Reference integration
            d.setRate(pr.rate() == null ? BigDecimal.ZERO : pr.rate());
            d.setDependentOf(dependentOf);
            d.setDiscountPct(discountPct);
            d.setUnitId(null);
            d.setStatus(true);
            d.setAddedBy(AUDIT_ACTOR_ID);
            d.setAddedDatetime(Timestamp.from(Instant.now()));
            detailRepository.save(d);
            indexToDetailId.put(i, d.getDetailId());
            savedDetails.add(d);
        }
        schedule.setDetails(savedDetails);

        emitProcedureScheduled(schedule, savedDetails);
        log.info("Scheduled OT id={} patient={} table={} procedures={} preBilling={}",
                schedule.getScheduleId(), patientId, otTable, savedDetails.size(), schedule.getPreBilling());
        return schedule;
    }

    // ------------------------------------------------------------------ lifecycle

    /** PLANNED -> IN_PROGRESS. Guard: any other source state is rejected (400). */
    @Transactional
    public OtSchedule start(long scheduleId) {
        OtSchedule s = load(scheduleId);
        requireStatus(s, OtSchedule.STATUS_PLANNED, "start");
        s.setStatus(OtSchedule.STATUS_IN_PROGRESS);
        touch(s);
        OtSchedule saved = scheduleRepository.save(s);
        log.info("OT schedule {} started", scheduleId);
        return attachDetails(saved);
    }

    /**
     * IN_PROGRESS -> DONE (complete sets status=3). Emits PROPOSED SurgeryCompleted
     * via the outbox, partition key patientId.
     */
    @Transactional
    public OtSchedule complete(long scheduleId) {
        OtSchedule s = load(scheduleId);
        requireStatus(s, OtSchedule.STATUS_IN_PROGRESS, "complete");

        s.setStatus(OtSchedule.STATUS_DONE);
        touch(s);
        OtSchedule saved = scheduleRepository.save(s);

        List<OtScheduleDetail> details = detailRepository.findByScheduleIdOrderByDetailIdAsc(scheduleId);
        emitSurgeryCompleted(saved, details);
        log.info("OT schedule {} completed", scheduleId);
        return attachDetails(saved);
    }

    /** PLANNED|IN_PROGRESS -> CANCELLED. Never from DONE. No event emitted. */
    @Transactional
    public OtSchedule cancel(long scheduleId) {
        OtSchedule s = load(scheduleId);
        if (s.getStatus() == OtSchedule.STATUS_DONE) {
            throw new InvalidOtRequestException(ERR_ILLEGAL_TRANSITION,
                    "cancel not allowed on a completed schedule (id=" + scheduleId + ")");
        }
        if (s.getStatus() == OtSchedule.STATUS_CANCELLED) {
            throw new InvalidOtRequestException(ERR_ILLEGAL_TRANSITION,
                    "schedule already cancelled (id=" + scheduleId + ")");
        }
        s.setStatus(OtSchedule.STATUS_CANCELLED);
        touch(s);
        OtSchedule saved = scheduleRepository.save(s);
        log.info("OT schedule {} cancelled", scheduleId);
        return attachDetails(saved);
    }

    // ------------------------------------------------------------------ queries

    /** GET /api/v1/ot/schedules?date=&status= worklist (both filters optional). */
    @Transactional(readOnly = true)
    public List<OtSchedule> search(LocalDate date, Integer status) {
        Timestamp dayStart = date == null ? null : Timestamp.valueOf(date.atStartOfDay());
        Timestamp dayEnd = date == null ? null : Timestamp.valueOf(date.atTime(LocalTime.MAX));
        List<OtSchedule> schedules =
                scheduleRepository.search(status, dayStart, dayEnd);
        schedules.forEach(this::attachDetails);
        return schedules;
    }

    /** GET /api/v1/ot/schedules/{id} — full schedule incl. details. */
    @Transactional(readOnly = true)
    public OtSchedule get(long id) {
        return attachDetails(load(id));
    }

    // ------------------------------------------------------------------ events

    /**
     * PROPOSED ProcedureScheduled (KAFKA-EVENT-CATALOG §3 — weak consumer evidence;
     * CSSD/Inventory consumption unconfirmed). Topic ihms.ot.events,
     * schema token procedure-scheduled.v1, partition key patientId.
     */
    private void emitProcedureScheduled(OtSchedule s, List<OtScheduleDetail> details) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("scheduleId", String.valueOf(s.getScheduleId()));
        payload.put("patientId", String.valueOf(s.getPatientId()));
        payload.put("encounterId", s.getEncounterId() == null ? null : String.valueOf(s.getEncounterId()));
        payload.put("admissionId", s.getAdmissionId() == null ? null : String.valueOf(s.getAdmissionId()));
        payload.put("otTable", s.getOtTable());
        payload.put("scheduledStart", s.getScheduledStart().toInstant().toString());
        payload.put("scheduledEnd", s.getScheduledEnd() == null ? null : s.getScheduledEnd().toInstant().toString());
        payload.put("surgeonId", s.getSurgeonId() == null ? null : String.valueOf(s.getSurgeonId()));
        payload.put("anesthetistId", s.getAnesthetistId() == null ? null : String.valueOf(s.getAnesthetistId()));
        payload.put("preBilling", s.getPreBilling());   // BR-OT-002 flag echoed; charge creation deferred to Billing Wave 4
        payload.put("procedures", details.stream().map(d -> {
            Map<String, Object> pd = new LinkedHashMap<>();
            pd.put("detailId", String.valueOf(d.getDetailId()));
            pd.put("procedureCode", d.getProcedureCode());
            pd.put("procedureName", d.getProcedureName());
            pd.put("rate", d.getRate());
            pd.put("dependentOf", d.getDependentOf() == null ? null : String.valueOf(d.getDependentOf()));
            pd.put("discountPct", d.getDiscountPct());
            return pd;
        }).toList());
        payload.put("scheduledAt", Instant.now().toString());
        outboxService.append(OutboxEvents.PROCEDURE_SCHEDULED, "schedule", s.getScheduleId(),
                String.valueOf(s.getPatientId()), toJson(payload),
                "procedure-scheduled:" + s.getScheduleId());
    }

    /**
     * PROPOSED SurgeryCompleted (KAFKA-EVENT-CATALOG §3). Topic ihms.ot.events,
     * schema token surgery-completed.v1, partition key patientId.
     */
    private void emitSurgeryCompleted(OtSchedule s, List<OtScheduleDetail> details) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("scheduleId", String.valueOf(s.getScheduleId()));
        payload.put("patientId", String.valueOf(s.getPatientId()));
        payload.put("encounterId", s.getEncounterId() == null ? null : String.valueOf(s.getEncounterId()));
        payload.put("admissionId", s.getAdmissionId() == null ? null : String.valueOf(s.getAdmissionId()));
        payload.put("completedAt", Instant.now().toString());
        payload.put("preBilling", s.getPreBilling());
        payload.put("procedureCodes", details.stream().map(OtScheduleDetail::getProcedureCode).toList());
        outboxService.append(OutboxEvents.SURGERY_COMPLETED, "schedule", s.getScheduleId(),
                String.valueOf(s.getPatientId()), toJson(payload),
                "surgery-completed:" + s.getScheduleId());
    }

    // ------------------------------------------------------------------ helpers

    private OtSchedule load(long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new OtEntityNotFoundException("NOT_FOUND", "OT schedule not found: " + id));
    }

    private void requireStatus(OtSchedule s, short expected, String action) {
        if (s.getStatus() == null || s.getStatus() != expected) {
            throw new InvalidOtRequestException(ERR_ILLEGAL_TRANSITION,
                    action + " allowed only from status "
                            + statusName(expected) + " but schedule " + s.getScheduleId()
                            + " is " + statusName(s.getStatus()));
        }
    }

    private static String statusName(short status) {
        return switch (status) {
            case OtSchedule.STATUS_PLANNED -> "PLANNED";
            case OtSchedule.STATUS_IN_PROGRESS -> "IN_PROGRESS";
            case OtSchedule.STATUS_DONE -> "DONE";
            case OtSchedule.STATUS_CANCELLED -> "CANCELLED";
            default -> String.valueOf(status);
        };
    }

    private void touch(OtSchedule s) {
        s.setUpdatedBy(AUDIT_ACTOR_ID);
        s.setUpdatedDatetime(Timestamp.from(Instant.now()));
    }

    private OtSchedule attachDetails(OtSchedule s) {
        s.setDetails(detailRepository.findByScheduleIdOrderByDetailIdAsc(s.getScheduleId()));
        return s;
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_ot." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }

    /** Request shape for one procedure row (controller maps JSON into these records). */
    public record ProcedureRequest(String procedureCode,
                                   String procedureName,
                                   BigDecimal rate,
                                   Integer dependentOfIndex,
                                   BigDecimal discountPct) {}

    /** Exact event TYPE strings — both PROPOSED per KAFKA-EVENT-CATALOG §3. */
    public static final class OutboxEvents {
        public static final String PROCEDURE_SCHEDULED = "ProcedureScheduled";  // PROPOSED (§3)
        public static final String SURGERY_COMPLETED = "SurgeryCompleted";      // PROPOSED (§3)

        private OutboxEvents() {}
    }
}
