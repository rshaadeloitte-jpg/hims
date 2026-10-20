package com.hims.appointment.api;

import com.hims.appointment.application.FollowUpVisitService;
import com.hims.appointment.domain.model.FollowUpVisit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Follow-up visit scheduling API (M02 gap, legacy PatientRegApptServiceImpl:3362 parity).
 * Validates parent appointment exists, reserves slot, supports recurrence.
 */
@RestController
@RequestMapping("/api/v1/appointments")
@Tag(name = "Follow-up Visits (M02)",
        description = "Follow-up visit scheduling linked to parent appointment/encounter. "
                + "Status: 1=SCHEDULED, 2=COMPLETED, 3=CANCELLED")
public class FollowUpVisitController {

    private final FollowUpVisitService followUpService;

    public FollowUpVisitController(FollowUpVisitService followUpService) {
        this.followUpService = followUpService;
    }

    @PostMapping("/{id}/follow-up")
    @Operation(summary = "Schedule a follow-up visit",
            description = "Creates a follow-up visit linked to the parent appointment. "
                    + "Validates parent appointment exists. Supports recurrence rule (RRULE subset).")
    public ResponseEntity<FollowUpVisit> scheduleFollowUp(
            @PathVariable("id") long parentAppointmentId,
            @RequestBody Map<String, Object> body) {
        Long patientId = AppointmentController.longOf(body, "patientId");
        Long doctorId = AppointmentController.longOf(body, "doctorId");
        Long departmentId = AppointmentController.longOf(body, "departmentId");
        Long unitId = AppointmentController.longOf(body, "unitId");
        String dateStr = AppointmentController.strOf(body, "followUpDate");
        String slotStartStr = AppointmentController.strOf(body, "slotStart");
        String slotEndStr = AppointmentController.strOf(body, "slotEnd");
        Long scheduleId = AppointmentController.longOf(body, "scheduleId");
        String cabin = AppointmentController.strOf(body, "cabin");
        String recurrenceRule = AppointmentController.strOf(body, "recurrenceRule");
        String recurrenceEndStr = AppointmentController.strOf(body, "recurrenceEndDate");

        LocalDate followUpDate = dateStr != null ? LocalDate.parse(dateStr) : null;
        LocalTime slotStart = slotStartStr != null ? LocalTime.parse(slotStartStr) : null;
        LocalTime slotEnd = slotEndStr != null ? LocalTime.parse(slotEndStr) : null;
        LocalDate recurrenceEndDate = recurrenceEndStr != null ? LocalDate.parse(recurrenceEndStr) : null;

        FollowUpVisit saved = followUpService.scheduleFollowUp(
                parentAppointmentId, patientId, doctorId, departmentId, unitId,
                followUpDate, slotStart, slotEnd, scheduleId, cabin,
                recurrenceRule, recurrenceEndDate);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/{id}/follow-ups")
    @Operation(summary = "List follow-up visits for a parent appointment")
    public List<FollowUpVisit> listByParent(@PathVariable("id") long parentAppointmentId) {
        return followUpService.listByParentAppointment(parentAppointmentId);
    }

    @GetMapping("/follow-ups/patient/{patientId}")
    @Operation(summary = "List follow-up visits for a patient on a date")
    public List<FollowUpVisit> listByPatientAndDate(
            @PathVariable long patientId,
            @RequestParam String date) {
        return followUpService.listByPatientAndDate(patientId, LocalDate.parse(date));
    }

    @GetMapping("/follow-ups/doctor/{doctorId}")
    @Operation(summary = "List follow-up visits for a doctor on a date")
    public List<FollowUpVisit> listByDoctorAndDate(
            @PathVariable long doctorId,
            @RequestParam String date) {
        return followUpService.listByDoctorAndDate(doctorId, LocalDate.parse(date));
    }

    @PostMapping("/follow-ups/{id}/cancel")
    @Operation(summary = "Cancel a follow-up visit (SCHEDULED -> CANCELLED)")
    public FollowUpVisit cancel(@PathVariable long id) {
        return followUpService.cancel(id);
    }

    @PostMapping("/follow-ups/{id}/complete")
    @Operation(summary = "Mark follow-up as completed (SCHEDULED -> COMPLETED)")
    public FollowUpVisit complete(@PathVariable long id) {
        return followUpService.complete(id);
    }
}
