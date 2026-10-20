package com.hims.appointment.application;

import com.hims.appointment.application.exceptions.AppointmentNotFoundException;
import com.hims.appointment.application.exceptions.InvalidAppointmentRequestException;
import com.hims.appointment.application.exceptions.SlotAlreadyBookedException;
import com.hims.appointment.domain.model.Appointment;
import com.hims.appointment.domain.model.AppointmentStatus;
import com.hims.appointment.domain.model.DoctorSchedule;
import com.hims.appointment.domain.model.DoctorSlot;
import com.hims.appointment.domain.repository.AppointmentRepository;
import com.hims.appointment.domain.repository.DoctorScheduleRepository;
import com.hims.appointment.domain.repository.DoctorSlotRepository;
import com.hims.appointment.infrastructure.outbox.OutboxService;
import com.hims.appointment.infrastructure.persistence.SequenceGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Appointment booking + lifecycle (MIG-031, Catalog #2 Appointment &amp; Encounter OPD).
 *
 * Business rules:
 *  - BR-OPD-002 slot rules: schedule window split into slotMinutes intervals;
 *    booking claims capacity on the materialized doctor_slot row with a
 *    conditional UPDATE (booked_count &lt; capacity) — the atomic double-booking
 *    guard — after validating doctorId / departmentId / date / cabin.
 *  - Token per (schedule, day): legacy T_PATIENTTOKEN semantics,
 *    race-safe via uq_appointment_token.
 *  - MIG-031 locked status dictionary: all transitions go through
 *    {@link AppointmentStatus#canTransitionTo} (forward-only state machine).
 *  - Transactional outbox: AppointmentBooked / AppointmentCancelled are
 *    appended in the SAME transaction as the write.
 *
 * Events topic: ihms.appointment.encounter (partition key patientId).
 */
@Service
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    public static final String EVENT_APPOINTMENT_BOOKED = "AppointmentBooked";
    public static final String EVENT_APPOINTMENT_CANCELLED = "AppointmentCancelled";
    public static final String EVENT_ENCOUNTER_CREATED = "EncounterCreated";   // KAFKA-EVENT-CATALOG E-02
    public static final String EVENT_ENCOUNTER_CLOSED = "EncounterClosed";

    public record BookingSpec(Long patientId, Long doctorId, Long scheduleId, Long unitId,
                              Long departmentId, LocalDate apptDate, LocalTime slotStart,
                              String cabin) { }

    private final DoctorScheduleRepository scheduleRepository;
    private final DoctorSlotRepository slotRepository;
    private final AppointmentRepository appointmentRepository;
    private final OutboxService outboxService;
    private final SequenceGateway sequences;

    public AppointmentService(DoctorScheduleRepository scheduleRepository,
                              DoctorSlotRepository slotRepository,
                              AppointmentRepository appointmentRepository,
                              OutboxService outboxService,
                              SequenceGateway sequences) {
        this.scheduleRepository = scheduleRepository;
        this.slotRepository = slotRepository;
        this.appointmentRepository = appointmentRepository;
        this.outboxService = outboxService;
        this.sequences = sequences;
    }

    // ------------------------------------------------------------------ schedules

    @Transactional
    public DoctorSchedule createSchedule(Long unitId, Long doctorId, Short dayOfWeek,
                                         LocalTime startTime, LocalTime endTime,
                                         Integer slotMinutes, Long deptId, Long clinicId,
                                         String cabin) {
        if (doctorId == null || dayOfWeek == null || startTime == null || endTime == null) {
            throw new InvalidAppointmentRequestException("BEXA003",
                    "doctorId, dayOfWeek, startTime, endTime are required");
        }
        if (dayOfWeek < 1 || dayOfWeek > 7) {
            throw new InvalidAppointmentRequestException("APPT_DOW_INVALID", "dayOfWeek must be 1..7 (ISO MON..SUN)");
        }
        if (!endTime.isAfter(startTime)) {
            throw new InvalidAppointmentRequestException("APPT_WINDOW_INVALID", "endTime must be after startTime");
        }
        int slotMins = slotMinutes == null ? 15 : slotMinutes;
        if (slotMins <= 0) {
            throw new InvalidAppointmentRequestException("APPT_SLOT_INVALID", "slotMinutes must be positive");
        }
        DoctorSchedule s = new DoctorSchedule();
        s.setId(sequences.next("ihms_appointment.doctor_schedule_seq"));
        s.setDoctorId(doctorId);
        s.setDayOfWeek(dayOfWeek);
        s.setStartTime(startTime);
        s.setEndTime(endTime);
        s.setSlotMinutes(slotMins);
        s.setDepartmentId(deptId);
        s.setClinicId(clinicId);
        s.setCabin(cabin);
        s.setUnitId(unitId);
        s.setStatus(true);
        s.setSynchronizedd(false);
        s.setAddedBy(AUDIT_ACTOR_ID);
        s.setAddedDatetime(now());
        return scheduleRepository.save(s);
    }

    @Transactional(readOnly = true)
    public List<DoctorSchedule> listSchedules(Long doctorId) {
        return doctorId == null
                ? scheduleRepository.findAll()
                : scheduleRepository.findByDoctorIdAndStatusTrue(doctorId);
    }

    // ------------------------------------------------------------------ slot search

    /**
     * Slot search for a doctor on a date: lazily materializes each matching
     * weekly schedule window into doctor_slot rows (idempotent via
     * uq_doctor_slot), then reports availability from booked_count/capacity.
     */
    @Transactional
    public List<Map<String, Object>> findSlots(Long doctorId, LocalDate date) {
        if (doctorId == null || date == null) {
            throw new InvalidAppointmentRequestException("BEXA003", "date is required (?date=yyyy-MM-dd)");
        }
        short dow = (short) date.getDayOfWeek().getValue();
        List<Map<String, Object>> result = new ArrayList<>();
        for (DoctorSchedule s : scheduleRepository.findByDoctorIdAndStatusTrue(doctorId)) {
            if (s.getDayOfWeek() != null && s.getDayOfWeek().shortValue() != dow) {
                continue;
            }
            int slotMins = s.getSlotMinutes() == null || s.getSlotMinutes() <= 0 ? 15 : s.getSlotMinutes();
            LocalTime t = s.getStartTime();
            while (t.plusMinutes(slotMins).compareTo(s.getEndTime()) <= 0) {
                slotRepository.materialize(
                        sequences.next("ihms_appointment.doctor_slot_seq"),
                        s.getUnitId(), AUDIT_ACTOR_ID, s.getId(), s.getDoctorId(),
                        s.getDepartmentId(), s.getClinicId(), date, t, t.plusMinutes(slotMins),
                        1, s.getCabin());
                t = t.plusMinutes(slotMins);
            }
        }
        for (DoctorSlot slot : slotRepository.findByDoctorIdAndSlotDateAndStatusTrueOrderBySlotStartAsc(doctorId, date)) {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("scheduleId", slot.getScheduleId());
            view.put("doctorId", slot.getDoctorId());
            view.put("departmentId", slot.getDepartmentId());
            view.put("date", slot.getSlotDate().toString());
            view.put("slotStart", slot.getSlotStart().toString());
            view.put("slotEnd", slot.getSlotEnd().toString());
            view.put("cabin", slot.getCabin());
            view.put("capacity", (int) slot.getCapacity());
            view.put("bookedCount", (int) slot.getBookedCount());
            view.put("booked", slot.getBookedCount() >= slot.getCapacity());
            result.add(view);
        }
        return result;
    }

    // ------------------------------------------------------------------ booking

    /** Book an appointment: validates the slot and claims its capacity atomically. */
    @Transactional
    public Appointment book(BookingSpec spec) {
        validateBookingSpec(spec);
        DoctorSlot slot = validateAgainstSchedule(spec, null);

        Appointment a = new Appointment();
        a.setId(sequences.next("ihms_appointment.appointment_seq"));
        applySlotToAppointment(a, spec, slot);

        Integer maxToken = appointmentRepository.maxToken(spec.scheduleId(), spec.apptDate());
        a.setTokenNo((maxToken == null ? 0 : maxToken) + 1);
        stampInsert(a);
        Appointment saved = appointmentRepository.save(a);

        outboxService.append(EVENT_APPOINTMENT_BOOKED, "appointment", saved.getId(),
                partitionKeyPatient(saved), toJson(bookedPayload(saved)),
                "appointment-booked:" + saved.getId());
        log.info("Booked appointment id={} token={} slot={} {}", saved.getId(), saved.getTokenNo(),
                saved.getApptDate(), saved.getSlotStart());
        return saved;
    }

    /**
     * Reschedule: forward-only BOOKED|CHECKED_IN -&gt; RESCHEDULED on the original
     * row (releasing its slot claim), then a fresh booking linked via
     * rescheduled_from_id. Emits AppointmentBooked for the new row.
     */
    @Transactional
    public Appointment reschedule(long id, BookingSpec newSpec) {
        validateBookingSpec(newSpec);
        Appointment original = load(id);
        boolean wasActive = original.slotActive();
        original.transitionTo(AppointmentStatus.RESCHEDULED);
        stampUpdate(original);

        if (wasActive) {
            // Release the ORIGINAL slot claim, never the target slot.
            releaseSlotClaim(original.getScheduleId(), original.getApptDate(), original.getSlotStart());
        }

        DoctorSlot slot = validateAgainstSchedule(newSpec, original);
        Appointment next = new Appointment();
        next.setId(sequences.next("ihms_appointment.appointment_seq"));
        applySlotToAppointment(next, newSpec, slot);
        next.setRescheduledFromId(original.getId());
        Integer maxToken = appointmentRepository.maxToken(newSpec.scheduleId(), newSpec.apptDate());
        next.setTokenNo((maxToken == null ? 0 : maxToken) + 1);
        stampInsert(next);
        Appointment saved = appointmentRepository.save(next);
        appointmentRepository.save(original);

        Map<String, Object> payload = bookedPayload(saved);
        payload.put("rescheduledFromId", original.getId());
        outboxService.append(EVENT_APPOINTMENT_BOOKED, "appointment", saved.getId(),
                partitionKeyPatient(saved), toJson(payload),
                "appointment-booked:" + saved.getId());
        log.info("Rescheduled appointment {} -> {} (new id={})", original.getId(), saved.getId(), saved.getId());
        return saved;
    }

    // ------------------------------------------------------------------ lifecycle

    /** complete CHECKED_IN -&gt; COMPLETED (encounter closure handled by {@link EncounterService}). */
    @Transactional
    public Appointment complete(long id) {
        Appointment a = load(id);
        a.transitionTo(AppointmentStatus.COMPLETED);
        return stampAndSave(a);
    }

    /** cancel BOOKED|CHECKED_IN -&gt; CANCELLED — releases the slot claim; emits AppointmentCancelled. */
    @Transactional
    public Appointment cancel(long id) {
        Appointment a = load(id);
        AppointmentStatus before = a.getStatusEnum();
        a.transitionTo(AppointmentStatus.CANCELLED);
        if (before == AppointmentStatus.BOOKED || before == AppointmentStatus.CHECKED_IN) {
            releaseSlotClaim(a.getScheduleId(), a.getApptDate(), a.getSlotStart());
        }
        Appointment saved = stampAndSave(a);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("appointmentId", String.valueOf(saved.getId()));
        payload.put("patientId", String.valueOf(saved.getPatientId()));
        payload.put("doctorId", String.valueOf(saved.getDoctorId()));
        payload.put("tokenNo", saved.getTokenNo());
        payload.put("apptDate", saved.getApptDate().toString());
        payload.put("slotStart", saved.getSlotStart().toString());
        payload.put("previousStatus", before.code());
        payload.put("cancelledAt", Timestamp.from(Instant.now()).toString());
        outboxService.append(EVENT_APPOINTMENT_CANCELLED, "appointment", saved.getId(),
                partitionKeyPatient(saved), toJson(payload),
                "appointment-cancelled:" + saved.getId() + ":" + before.code());
        return saved;
    }

    /** noshow BOOKED -&gt; NO_SHOW (slot stays consumed for the historical day roster). */
    @Transactional
    public Appointment noshow(long id) {
        Appointment a = load(id);
        a.transitionTo(AppointmentStatus.NO_SHOW);
        return stampAndSave(a);
    }

    // --------------------------------------------------------------- walk-in

    /**
     * Walk-in / emergency visit (M02 gap): creates an appointment without
     * slot validation. The patient arrives without a pre-booked slot.
     * visitType distinguishes WALK_IN from EMERGENCY.
     * Status starts at CHECKED_IN since the patient is already present.
     */
    @Transactional
    public Appointment walkIn(Long patientId, Long doctorId, Long departmentId,
                              Long unitId, String visitType) {
        if (patientId == null || doctorId == null) {
            throw new InvalidAppointmentRequestException("BEXA003",
                    "patientId and doctorId are required for walk-in");
        }
        String vType = visitType != null ? visitType.toUpperCase() : "WALK_IN";
        if (!vType.equals("WALK_IN") && !vType.equals("EMERGENCY")) {
            throw new InvalidAppointmentRequestException("BEXA003",
                    "visitType must be WALK_IN or EMERGENCY");
        }

        LocalDate today = LocalDate.now();
        Appointment a = new Appointment();
        a.setId(sequences.next("ihms_appointment.appointment_seq"));
        a.setPatientId(patientId);
        a.setDoctorId(doctorId);
        a.setDepartmentId(departmentId);
        a.setUnitId(unitId);
        a.setApptDate(today);
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        a.setSlotStart(now);
        a.setSlotEnd(now.plusMinutes(15));
        a.setTokenNo(0);
        a.setApptStatus(AppointmentStatus.CHECKED_IN.code());
        a.setVisitType(vType);
        a.setStatus(true);
        a.setSynchronizedd(false);
        stampInsert(a);
        Appointment saved = appointmentRepository.save(a);

        Map<String, Object> payload = bookedPayload(saved);
        payload.put("visitType", vType);
        outboxService.append(EVENT_APPOINTMENT_BOOKED, "appointment", saved.getId(),
                partitionKeyPatient(saved), toJson(payload),
                "appointment-walkin:" + saved.getId());
        log.info("{} visit created id={} patient={} doctor={}",
                vType, saved.getId(), patientId, doctorId);
        return saved;
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public List<Appointment> listByDateAndDoctor(LocalDate date, Long doctorId) {
        if (date == null) {
            throw new InvalidAppointmentRequestException("BEXA003", "date is required (?date=yyyy-MM-dd)");
        }
        return doctorId == null
                ? appointmentRepository.findByApptDateAndStatusTrueOrderByDoctorIdAscSlotStartAscTokenNoAsc(date)
                : appointmentRepository.findByApptDateAndDoctorIdAndStatusTrueOrderBySlotStartAscTokenNoAsc(date, doctorId);
    }

    @Transactional(readOnly = true)
    public Appointment get(long id) {
        return load(id);
    }

    // ------------------------------------------------------------------ shared internals

    private void validateBookingSpec(BookingSpec spec) {
        if (spec.patientId() == null || spec.doctorId() == null || spec.scheduleId() == null
                || spec.apptDate() == null || spec.slotStart() == null) {
            throw new InvalidAppointmentRequestException("BEXA003",
                    "patientId, doctorId, scheduleId, apptDate, slotStart are required");
        }
    }

    /**
     * Slot validation (BR-OPD-002): schedule active + owned by doctorId,
     * day-of-week match, department match when supplied, slot on the grid
     * inside the window, cabin compatibility; returns/creates the slot row.
     */
    private DoctorSlot validateAgainstSchedule(BookingSpec spec, Appointment existing) {
        long scheduleId = spec.scheduleId();
        DoctorSchedule schedule = scheduleRepository.findById(scheduleId)
                .filter(s -> Boolean.TRUE.equals(s.getStatus()))
                .orElseThrow(() -> new AppointmentNotFoundException("NOT_FOUND",
                        "Doctor schedule not found: " + scheduleId));
        if (!schedule.getDoctorId().equals(spec.doctorId())) {
            throw new InvalidAppointmentRequestException("APPT_DOCTOR_MISMATCH",
                    "scheduleId does not belong to doctorId");
        }
        short dow = (short) spec.apptDate().getDayOfWeek().getValue();
        if (schedule.getDayOfWeek() != null && schedule.getDayOfWeek().shortValue() != dow) {
            throw new InvalidAppointmentRequestException("APPT_DAY_MISMATCH",
                    "Doctor has no clinic on " + spec.apptDate() + " (" + dow + ")");
        }
        if (spec.departmentId() != null && schedule.getDepartmentId() != null
                && !spec.departmentId().equals(schedule.getDepartmentId())) {
            throw new InvalidAppointmentRequestException("APPT_DEPT_MISMATCH",
                    "departmentId does not match the schedule's department");
        }
        int slotMins = schedule.getSlotMinutes() == null ? 15 : schedule.getSlotMinutes();
        if (spec.slotStart().isBefore(schedule.getStartTime())
                || spec.slotStart().plusMinutes(slotMins).isAfter(schedule.getEndTime())
                || spec.slotStart().getMinute() % slotMins != 0 || spec.slotStart().getSecond() != 0) {
            throw new InvalidAppointmentRequestException("APPT_SLOT_NOT_IN_SCHEDULE",
                    "Slot " + spec.slotStart() + " is not a valid " + slotMins
                            + "-minute slot of schedule " + scheduleId);
        }
        String cabin = spec.cabin() != null ? spec.cabin()
                : (existing != null ? existing.getCabin() : null);
        if (schedule.getCabin() != null && cabin != null && !schedule.getCabin().equalsIgnoreCase(cabin)) {
            throw new InvalidAppointmentRequestException("APPT_CABIN_MISMATCH",
                    "cabin '" + cabin + "' does not match schedule cabin '" + schedule.getCabin() + "'");
        }
        if (cabin == null && schedule.getCabin() != null) {
            cabin = schedule.getCabin();   // default to the schedule-bound cabin
        }
        return ensureSlotRow(schedule, spec.apptDate(), spec.slotStart(), cabin);
    }

    private DoctorSlot ensureSlotRow(DoctorSchedule schedule, LocalDate date, LocalTime slotStart, String cabin) {
        int slotMins = schedule.getSlotMinutes() == null ? 15 : schedule.getSlotMinutes();
        slotRepository.materialize(
                sequences.next("ihms_appointment.doctor_slot_seq"),
                schedule.getUnitId(), AUDIT_ACTOR_ID, schedule.getId(), schedule.getDoctorId(),
                schedule.getDepartmentId(), schedule.getClinicId(), date, slotStart,
                slotStart.plusMinutes(slotMins), 1, cabin);
        return slotRepository.findByScheduleIdAndSlotDateAndSlotStart(schedule.getId(), date, slotStart)
                .orElseThrow(() -> new IllegalStateException(
                        "doctor_slot row missing after materialization: " + schedule.getId() + "/" + date + "/" + slotStart));
    }

    private void applySlotToAppointment(Appointment a, BookingSpec spec, DoctorSlot slot) {
        if (!slot.hasCapacity()) {
            throw new SlotAlreadyBookedException("APPT_SLOT_TAKEN",
                    "Slot " + slot.getSlotStart() + " on " + slot.getSlotDate()
                            + " is fully booked (" + slot.getBookedCount() + "/" + slot.getCapacity() + ")");
        }
        if (appointmentRepository.countByScheduleIdAndApptDateAndSlotStartAndApptStatusInAndStatusTrue(
                slot.getScheduleId(), slot.getSlotDate(), slot.getSlotStart(),
                List.of(AppointmentStatus.BOOKED.code(), AppointmentStatus.CHECKED_IN.code())) > 0
                && slot.getCapacity() == 1) {
            throw new SlotAlreadyBookedException("APPT_SLOT_TAKEN",
                    "Slot already booked: schedule " + slot.getScheduleId() + " "
                            + slot.getSlotDate() + " " + slot.getSlotStart());
        }
        // Atomic double-booking guard: conditional UPDATE claims one capacity unit.
        if (slotRepository.claimCapacity(slot.getId()) == 0) {
            throw new SlotAlreadyBookedException("APPT_SLOT_TAKEN",
                    "Slot was just taken by a concurrent booking (schedule " + slot.getScheduleId()
                            + " " + slot.getSlotDate() + " " + slot.getSlotStart() + ")");
        }
        a.setPatientId(spec.patientId());
        a.setDoctorId(spec.doctorId());
        a.setScheduleId(slot.getScheduleId());
        a.setDepartmentId(slot.getDepartmentId() != null ? slot.getDepartmentId() : spec.departmentId());
        a.setClinicId(slot.getClinicId());
        a.setApptDate(slot.getSlotDate());
        a.setSlotStart(slot.getSlotStart());
        a.setSlotEnd(slot.getSlotEnd());
        a.setCabin(spec.cabin() != null ? spec.cabin() : slot.getCabin());
        a.setUnitId(spec.unitId() != null ? spec.unitId() : slot.getUnitId());
        a.setApptStatus(AppointmentStatus.BOOKED.code());
        a.setStatus(true);
        a.setSynchronizedd(false);
    }

    private void releaseSlotClaim(Long scheduleId, LocalDate date, LocalTime slotStart) {
        slotRepository.findByScheduleIdAndSlotDateAndSlotStart(scheduleId, date, slotStart)
                .ifPresent(slot -> slotRepository.releaseCapacity(slot.getId()));
    }

    private Appointment load(long id) {
        return appointmentRepository.findById(id)
                .filter(a -> Boolean.TRUE.equals(a.getStatus()))
                .orElseThrow(() -> new AppointmentNotFoundException("NOT_FOUND", "Appointment not found: " + id));
    }

    void stampInsert(Appointment a) {
        a.setAddedBy(AUDIT_ACTOR_ID);
        a.setAddedDatetime(now());
        a.setUpdatedBy(AUDIT_ACTOR_ID);
        a.setUpdatedDatetime(now());
    }

    void stampUpdate(Appointment a) {
        a.setUpdatedBy(AUDIT_ACTOR_ID);
        a.setUpdatedDatetime(now());
    }

    private Appointment stampAndSave(Appointment a) {
        stampUpdate(a);
        return appointmentRepository.save(a);
    }

    static String partitionKeyPatient(Appointment a) {
        return String.valueOf(a.getPatientId());
    }

    static Timestamp now() {
        return Timestamp.from(Instant.now());
    }

    Map<String, Object> bookedPayload(Appointment a) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("appointmentId", String.valueOf(a.getId()));
        payload.put("patientId", String.valueOf(a.getPatientId()));
        payload.put("doctorId", String.valueOf(a.getDoctorId()));
        payload.put("scheduleId", String.valueOf(a.getScheduleId()));
        payload.put("unitId", a.getUnitId() == null ? null : String.valueOf(a.getUnitId()));
        payload.put("departmentId", a.getDepartmentId() == null ? null : String.valueOf(a.getDepartmentId()));
        payload.put("clinicId", a.getClinicId() == null ? null : String.valueOf(a.getClinicId()));
        payload.put("apptDate", a.getApptDate().toString());
        payload.put("slotStart", a.getSlotStart().toString());
        payload.put("slotEnd", a.getSlotEnd() == null ? null : a.getSlotEnd().toString());
        payload.put("cabin", a.getCabin());
        payload.put("tokenNo", a.getTokenNo());
        payload.put("apptStatus", (int) a.getApptStatus());
        return payload;
    }

    static String toJson(Map<String, Object> payload) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }
}
