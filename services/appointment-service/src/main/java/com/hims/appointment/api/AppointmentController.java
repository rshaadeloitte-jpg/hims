package com.hims.appointment.api;

import com.hims.appointment.application.AppointmentService;
import com.hims.appointment.application.EncounterService;
import com.hims.appointment.application.exceptions.InvalidAppointmentRequestException;
import com.hims.appointment.domain.model.Appointment;
import com.hims.appointment.domain.model.AppointmentStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Appointment lifecycle API (MIG-031): book / list / get / checkin / complete /
 * cancel / noshow / reschedule. Booking enforces BR-OPD-002 unique-slot via the
 * doctor_slot capacity CAS; check-in creates the encounter and emits E-02
 * EncounterCreated through the transactional outbox; cancel emits
 * AppointmentCancelled. All events go to topic ihms.appointment.encounter.
 */
@RestController
@RequestMapping("/api/v1/appointments")
@Tag(name = "Appointments (WP-4)",
        description = "Appointment booking with unique-slot constraint and MIG-031 locked status lifecycle "
                + "(1=BOOKED 2=CHECKED_IN 3=COMPLETED 4=CANCELLED 5=RESCHEDULED 6=NO_SHOW)")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final EncounterService encounterService;

    public AppointmentController(AppointmentService appointmentService, EncounterService encounterService) {
        this.appointmentService = appointmentService;
        this.encounterService = encounterService;
    }

    @PostMapping
    @Operation(summary = "Book an appointment",
            description = "Validates the slot against the doctor's schedule (doctorId, departmentId, day-of-week, "
                    + "window, slotMinutes grid, cabin) and claims doctor_slot capacity atomically "
                    + "(BR-OPD-002 double-booking guard); assigns token per schedule/day. "
                    + "Writes AppointmentBooked to the outbox in the same transaction. Returns 409 if the slot is taken.")
    public ResponseEntity<Appointment> book(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.book(bookingSpec(body)));
    }

    @GetMapping
    @Operation(summary = "List appointments for a date", description = "?date=yyyy-MM-dd&doctorId= (doctorId optional).")
    public List<Appointment> list(@RequestParam LocalDate date,
                                  @RequestParam(required = false) Long doctorId) {
        return appointmentService.listByDateAndDoctor(date, doctorId);
    }

    @GetMapping("/{id}")
    public Appointment get(@PathVariable long id) {
        return appointmentService.get(id);
    }

    @PostMapping("/{id}/checkin")
    @Operation(summary = "Check in the patient (BOOKED -> CHECKED_IN)",
            description = "Creates the encounter (daily-counter OPD number) and emits E-02 EncounterCreated via the "
                    + "outbox in the same transaction (payload per KAFKA-EVENT-CATALOG: encounterId, patientId, unitId, "
                    + "doctorId, deptId, visitMark, appointmentType, opdNo/token, visitstatus2, clinicId; "
                    + "schema encounter-created.v1; partition key patientId).")
    public Appointment checkin(@PathVariable long id) {
        encounterService.checkIn(id);
        return appointmentService.get(id);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete the consultation (CHECKED_IN -> COMPLETED)",
            description = "Closes the encounter and emits EncounterClosed (partition key encounterId).")
    public Appointment complete(@PathVariable long id) {
        encounterService.completeEncounter(id);
        return appointmentService.get(id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel the appointment (BOOKED|CHECKED_IN -> CANCELLED)",
            description = "Releases the claimed slot capacity for rebooking; emits AppointmentCancelled.")
    public Appointment cancel(@PathVariable long id) {
        return appointmentService.cancel(id);
    }

    @PostMapping("/{id}/noshow")
    @Operation(summary = "Mark no-show (BOOKED -> NO_SHOW)")
    public Appointment noshow(@PathVariable long id) {
        return appointmentService.noshow(id);
    }

    @PostMapping("/{id}/reschedule")
    @Operation(summary = "Reschedule to a new slot (BOOKED|CHECKED_IN -> RESCHEDULED + fresh BOOKED row)",
            description = "Forward-only: the original row becomes RESCHEDULED (slot released) and a new linked "
                    + "appointment is booked (rescheduledFromId). Emits AppointmentBooked for the new row.")
    public ResponseEntity<Appointment> reschedule(@PathVariable long id, @RequestBody Map<String, Object> body) {
        Appointment moved = appointmentService.reschedule(id, bookingSpec(body));
        return ResponseEntity.status(HttpStatus.CREATED).body(moved);
    }

    // --------------------------------------------------------------- walk-in

    @PostMapping("/walkin")
    @Operation(summary = "Register a walk-in or emergency visit (M02 gap)",
            description = "Creates an appointment without slot validation. Patient is immediately CHECKED_IN. "
                    + "visitType: WALK_IN (default) or EMERGENCY. Requires patientId and doctorId.")
    public ResponseEntity<Appointment> walkIn(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.walkIn(
                        longOf(body, "patientId"),
                        longOf(body, "doctorId"),
                        longOf(body, "departmentId"),
                        longOf(body, "unitId"),
                        strOf(body, "visitType")));
    }

    // ------------------------------------------------------------------ helpers

    private AppointmentService.BookingSpec bookingSpec(Map<String, Object> body) {
        String dateStr = strOf(body, "apptDate");
        String slotStr = strOf(body, "slotStart");
        if (dateStr == null || slotStr == null) {
            throw new InvalidAppointmentRequestException("BEXA003",
                    "apptDate (yyyy-MM-dd) and slotStart (HH:mm[:ss]) are required");
        }
        try {
            return new AppointmentService.BookingSpec(
                    longOf(body, "patientId"), longOf(body, "doctorId"), longOf(body, "scheduleId"),
                    longOf(body, "unitId"), longOf(body, "departmentId"),
                    LocalDate.parse(dateStr), LocalTime.parse(slotStr),
                    strOf(body, "cabin"));
        } catch (java.time.format.DateTimeParseException e) {
            throw new InvalidAppointmentRequestException("BEXA003",
                    "Invalid date/time format: " + e.getParsedString());
        }
    }

    static Long longOf(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.valueOf(String.valueOf(v));
        } catch (NumberFormatException e) {
            throw new InvalidAppointmentRequestException("BEXA003", key + " must be numeric");
        }
    }

    static String strOf(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? null : String.valueOf(v);
    }
}
