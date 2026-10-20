package com.hims.appointment.api;

import com.hims.appointment.application.AppointmentService;
import com.hims.appointment.domain.model.DoctorSchedule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Doctor schedule + slot search API (MIG-031). Slots are materialized into
 * doctor_slot rows (capacity/booked_count — BR-OPD-002 double-booking guard).
 */
@RestController
@Tag(name = "Doctor Schedules & Slots (WP-4)",
        description = "Weekly doctor schedule master and slot search for OPD appointment booking")
public class DoctorScheduleController {

    private final AppointmentService service;

    public DoctorScheduleController(AppointmentService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/doctors/{doctorId}/schedules")
    @Operation(summary = "Create a weekly doctor schedule",
            description = "dayOfWeek 1=MON..7=SUN; window [startTime,endTime) split into slotMinutes slots "
                    + "(default 15); optional deptId/clinicId/cabin. BR-OPD-002 slot rules.")
    public ResponseEntity<DoctorSchedule> createSchedule(@PathVariable long doctorId,
                                                         @RequestParam Long unitId,
                                                         @RequestParam short dayOfWeek,
                                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) java.time.LocalTime startTime,
                                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) java.time.LocalTime endTime,
                                                         @RequestParam(required = false, defaultValue = "15") int slotMinutes,
                                                         @RequestParam(required = false) Long deptId,
                                                         @RequestParam(required = false) Long clinicId,
                                                         @RequestParam(required = false) String cabin) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createSchedule(unitId, doctorId, dayOfWeek, startTime, endTime,
                        slotMinutes, deptId, clinicId, cabin));
    }

    @GetMapping("/api/v1/doctors/schedules")
    @Operation(summary = "List doctor schedules", description = "Optional doctorId filter; active schedules only when filtered.")
    public List<DoctorSchedule> listSchedules(@RequestParam(required = false) Long doctorId) {
        return service.listSchedules(doctorId);
    }

    @GetMapping("/api/v1/doctors/{doctorId}/slots")
    @Operation(summary = "Search bookable slots for a doctor on a date",
            description = "Materializes the doctor's weekly schedule windows for the requested day into doctor_slot "
                    + "rows and reports availability (booked=true when bookedCount>=capacity). Query: ?date=yyyy-MM-dd.")
    public List<Map<String, Object>> slots(@PathVariable long doctorId,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.findSlots(doctorId, date);
    }
}
