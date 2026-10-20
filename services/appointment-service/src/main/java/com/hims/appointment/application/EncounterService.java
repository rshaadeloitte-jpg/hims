package com.hims.appointment.application;

import com.hims.appointment.application.exceptions.EncounterNotFoundException;
import com.hims.appointment.application.exceptions.InvalidAppointmentRequestException;
import com.hims.appointment.domain.model.Appointment;
import com.hims.appointment.domain.model.Encounter;
import com.hims.appointment.domain.model.LegacyVisitStatus;
import com.hims.appointment.domain.repository.AppointmentRepository;
import com.hims.appointment.domain.repository.EncounterRepository;
import com.hims.appointment.infrastructure.outbox.OutboxService;
import com.hims.appointment.infrastructure.persistence.DailyCounterDao;
import com.hims.appointment.infrastructure.persistence.SequenceGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Encounter lifecycle (MIG-031): creation at check-in, split-brain VisitStatus
 * resolution, closure by completion or by the daily closing job.
 *
 * Split-brain resolution (BR-OPD-007 / BR-PHAR-001): legacy wrote VisitStatus
 * 11 (bill paid) / 12 (pharmacy dispensed) to T_Appointment while the clinical
 * flow wrote T_Encounter, and kiosk/queue read one side. Here both live on one
 * aggregate: billing/pharmacy advances advance encounter.legacy_visit_status
 * with the exact legacy guards ({@link LegacyVisitStatus}), and every consumer-
 * facing payload resolves effective = max(appointment side, encounter side).
 *
 * Token & OPD queue linkage: encounter_no comes from the atomic daily counter
 * (per date+unit) and the E-02 payload carries opdNo/token exactly as the
 * KAFKA-EVENT-CATALOG defines for queue-service compatibility.
 */
@Service
public class EncounterService {

    private static final Logger log = LoggerFactory.getLogger(EncounterService.class);

    private final EncounterRepository encounterRepository;
    private final AppointmentRepository appointmentRepository;
    private final OutboxService outboxService;
    private final SequenceGateway sequences;
    private final DailyCounterDao dailyCounter;

    public EncounterService(EncounterRepository encounterRepository,
                            AppointmentRepository appointmentRepository,
                            OutboxService outboxService,
                            SequenceGateway sequences,
                            DailyCounterDao dailyCounter) {
        this.encounterRepository = encounterRepository;
        this.appointmentRepository = appointmentRepository;
        this.outboxService = outboxService;
        this.sequences = sequences;
        this.dailyCounter = dailyCounter;
    }

    /**
     * Check-in: creates the encounter (daily-counter OPD number), moves the
     * appointment BOOKED -&gt; CHECKED_IN, appends E-02 EncounterCreated to the
     * outbox in the SAME transaction.
     */
    @Transactional
    public Encounter checkIn(long appointmentId) {
        Appointment a = appointmentRepository.findById(appointmentId)
                .filter(x -> Boolean.TRUE.equals(x.getStatus()))
                .orElseThrow(() -> new com.hims.appointment.application.exceptions.AppointmentNotFoundException(
                        "NOT_FOUND", "Appointment not found: " + appointmentId));
        if (!a.isOpenForCheckIn()) {
            throw new InvalidAppointmentRequestException("APPT_ILLEGAL_TRANSITION",
                    "checkin requires status BOOKED, current=" + a.getStatusEnum());
        }
        LocalDate today = LocalDate.now();
        Long unitKey = a.getUnitId() == null ? 0L : a.getUnitId();
        int encounterNo = dailyCounter.nextEncounterNo(today, unitKey);

        Encounter e = new Encounter();
        e.setId(sequences.next("ihms_appointment.encounter_seq"));
        e.setAppointmentId(a.getId());
        e.setPatientId(a.getPatientId());
        e.setDoctorId(a.getDoctorId());
        e.setDepartmentId(a.getDepartmentId());
        e.setClinicId(a.getClinicId());
        e.setUnitId(a.getUnitId());
        e.setEncounterDate(today);
        e.setEncounterNo(encounterNo);
        e.setTokenNo(a.getTokenNo());
        e.setCabin(a.getCabin());
        e.setVisitstatus2("2");   // legacy default on visit creation (STATUS-CODE-DICTIONARY §1)
        e.setAddedBy(AppointmentService.AUDIT_ACTOR_ID);
        e.setAddedDatetime(Timestamp.from(Instant.now()));
        e.setStatus(true);
        e.setSynchronizedd(false);
        Encounter saved = encounterRepository.save(e);

        // Forward-only state machine move (BOOKED -> CHECKED_IN).
        a.transitionTo(com.hims.appointment.domain.model.AppointmentStatus.CHECKED_IN);
        a.setEncounterId(saved.getId());
        a.setUpdatedBy(AppointmentService.AUDIT_ACTOR_ID);
        a.setUpdatedDatetime(Timestamp.from(Instant.now()));
        appointmentRepository.save(a);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("encounterId", String.valueOf(saved.getId()));
        payload.put("patientId", String.valueOf(saved.getPatientId()));
        payload.put("unitId", saved.getUnitId() == null ? null : String.valueOf(saved.getUnitId()));
        payload.put("doctorId", String.valueOf(saved.getDoctorId()));
        payload.put("deptId", saved.getDepartmentId() == null ? null : String.valueOf(saved.getDepartmentId()));
        payload.put("visitMark", null);
        payload.put("appointmentType", "OPD_APPOINTMENT");
        // Token & OPD queue linkage: legacy-named key kept for queue/kiosk consumers.
        payload.put("opdNo/token", String.valueOf(saved.getTokenNo()));
        payload.put("tokenNo", saved.getTokenNo());
        payload.put("encounterNo", saved.getEncounterNo());
        payload.put("visitstatus2", saved.getVisitstatus2());
        payload.put("clinicId", saved.getClinicId() == null ? null : String.valueOf(saved.getClinicId()));
        outboxService.append(AppointmentService.EVENT_ENCOUNTER_CREATED, "encounter", saved.getId(),
                String.valueOf(saved.getPatientId()), AppointmentService.toJson(payload),
                "encounter-created:" + saved.getId());
        log.info("Checked in appointment {} -> encounter {} (no={} token={})",
                appointmentId, saved.getId(), saved.getEncounterNo(), saved.getTokenNo());
        return saved;
    }

    /** Complete consultation: closes the encounter and emits EncounterClosed. */
    @Transactional
    public Encounter completeEncounter(long appointmentId) {
        Appointment a = loadAppointment(appointmentId);
        Encounter e = encounterByAppointment(a.getId());
        a.transitionTo(com.hims.appointment.domain.model.AppointmentStatus.COMPLETED);
        a.setUpdatedBy(AppointmentService.AUDIT_ACTOR_ID);
        a.setUpdatedDatetime(Timestamp.from(Instant.now()));
        appointmentRepository.save(a);
        return close(e, AppointmentService.AUDIT_ACTOR_ID, "COMPLETED");
    }

    // ------------------------------------------------------------- split-brain ops

    /**
     * Billing paid (legacy addBillPayment parity, AddPaymentServiceHelper:477,820):
     * advances legacy_visit_status to 11 ONLY IF current &lt; 11.
     */
    @Transactional
    public Encounter markBilled(long encounterId) {
        Encounter e = load(encounterId);
        requireOpen(e, "billing update");
        e.setLegacyVisitStatus(LegacyVisitStatus.advanceBillPayment(e.getLegacyVisitStatus()));
        stamp(e);
        return encounterRepository.save(e);
    }

    /**
     * Pharmacy fully dispensed (legacy MedicationServiceImpl:812 parity):
     * 11 -&gt; 12 only when no undispensed item sales remain; blocked otherwise
     * (legacy left the visit at 11 until the last item was dispensed).
     */
    @Transactional
    public Encounter markDispensed(long encounterId, boolean undispensedItemsRemain) {
        Encounter e = load(encounterId);
        requireOpen(e, "dispense update");
        Short advanced = LegacyVisitStatus.advanceDispense(e.getLegacyVisitStatus(), undispensedItemsRemain);
        if (advanced == null) {
            throw new InvalidAppointmentRequestException("ENC_DISPENSE_BLOCKED",
                    "Dispense advance requires VisitStatus 11 (billed) with no undispensed items, current="
                            + e.getLegacyVisitStatus());
        }
        e.setLegacyVisitStatus(advanced);
        stamp(e);
        return encounterRepository.save(e);
    }

    /** Effective VisitStatus across both sides of the visit (split-brain read parity). */
    @Transactional(readOnly = true)
    public short effectiveVisitStatus(long encounterId) {
        Encounter e = load(encounterId);
        Short apptSide = e.getAppointmentId() == null ? null
                : appointmentRepository.findById(e.getAppointmentId()).map(Appointment::getLegacyVisitStatus).orElse(null);
        Short effective = e.effectiveLegacyVisitStatus(apptSide);
        return effective == null ? 0 : effective;
    }

    // ------------------------------------------------------------------ closing

    /**
     * Daily encounter closing job body (ApplicationJobs:542 parity): close every
     * open encounter dated BEFORE the cutoff and emit EncounterClosed per row.
     * Returns the number of closed encounters.
     */
    @Transactional
    public int closeStaleEncounters(LocalDate exclusiveCutoff, String reason) {
        List<Encounter> stale = encounterRepository.findByClosedFalseAndEncounterDateLessThan(exclusiveCutoff);
        for (Encounter e : stale) {
            close(e, AppointmentService.AUDIT_ACTOR_ID, reason);
        }
        if (!stale.isEmpty()) {
            log.info("Daily encounter close: closed {} stale encounters (cutoff={}, reason={})",
                    stale.size(), exclusiveCutoff, reason);
        }
        return stale.size();
    }

    /** Close one encounter and append EncounterClosed (partition key = encounterId). */
    @Transactional
    public Encounter close(Encounter e, long closedBy, String reason) {
        if (e.isClosed()) {
            return e;
        }
        e.setClosed(true);
        e.setClosedAt(Timestamp.from(Instant.now()));
        e.setClosedBy(closedBy);
        e.setCloseReason(reason);
        stamp(e);
        Encounter saved = encounterRepository.save(e);

        Short effective = resolveWithAppointmentSide(saved);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("encounterId", String.valueOf(saved.getId()));
        payload.put("patientId", String.valueOf(saved.getPatientId()));
        payload.put("appointmentId", saved.getAppointmentId() == null ? null : String.valueOf(saved.getAppointmentId()));
        payload.put("encounterDate", saved.getEncounterDate().toString());
        payload.put("tokenNo", saved.getTokenNo());
        payload.put("opdNo/token", saved.getTokenNo() == null ? null : String.valueOf(saved.getTokenNo()));
        payload.put("encounterNo", saved.getEncounterNo());
        payload.put("effectiveVisitStatus", effective == null ? null : (int) effective);
        payload.put("closedAt", saved.getClosedAt().toString());
        payload.put("closeReason", reason);
        outboxService.append(AppointmentService.EVENT_ENCOUNTER_CLOSED, "encounter", saved.getId(),
                String.valueOf(saved.getId()),   // partition key encounterId (per-patient ordering preserved via patientId-keyed events upstream)
                AppointmentService.toJson(payload),
                "encounter-closed:" + saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Encounter get(long id) {
        return load(id);
    }

    // ------------------------------------------------------------------ helpers

    private Short resolveWithAppointmentSide(Encounter e) {
        Short apptSide = e.getAppointmentId() == null ? null
                : appointmentRepository.findById(e.getAppointmentId()).map(Appointment::getLegacyVisitStatus).orElse(null);
        return e.effectiveLegacyVisitStatus(apptSide);
    }

    private Encounter load(long id) {
        return encounterRepository.findById(id)
                .filter(x -> Boolean.TRUE.equals(x.getStatus()))
                .orElseThrow(() -> new EncounterNotFoundException("NOT_FOUND", "Encounter not found: " + id));
    }

    private Appointment loadAppointment(long id) {
        return appointmentRepository.findById(id)
                .filter(x -> Boolean.TRUE.equals(x.getStatus()))
                .orElseThrow(() -> new com.hims.appointment.application.exceptions.AppointmentNotFoundException(
                        "NOT_FOUND", "Appointment not found: " + id));
    }

    private Encounter encounterByAppointment(long appointmentId) {
        List<Encounter> list = encounterRepository.findByAppointmentId(appointmentId);
        if (list.isEmpty()) {
            throw new EncounterNotFoundException("NOT_FOUND", "No encounter for appointment: " + appointmentId);
        }
        return list.getFirst();
    }

    private void requireOpen(Encounter e, String action) {
        if (e.isClosed()) {
            throw new InvalidAppointmentRequestException("ENC_CLOSED",
                    action + " not allowed on closed encounter " + e.getId());
        }
    }

    private void stamp(Encounter e) {
        e.setUpdatedBy(AppointmentService.AUDIT_ACTOR_ID);
        e.setUpdatedDatetime(Timestamp.from(Instant.now()));
    }
}
