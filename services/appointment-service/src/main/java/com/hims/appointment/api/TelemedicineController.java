package com.hims.appointment.api;

import com.hims.appointment.application.TelemedicineService;
import com.hims.appointment.domain.model.TelemedicineRoom;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Telemedicine room API (M02 gap, stub behind app.telemedicine.enabled=false).
 * Creates roomId + videoLink placeholder; real provider (Jitsi/Twilio) deferred.
 */
@RestController
@RequestMapping("/api/v1/appointments")
@Tag(name = "Telemedicine (M02)",
        description = "Telemedicine consultation room stub. Real video provider behind app.telemedicine.enabled=false. "
                + "Room status: 1=CREATED, 2=ACTIVE, 3=ENDED, 4=EXPIRED")
public class TelemedicineController {

    private final TelemedicineService telemedicineService;

    public TelemedicineController(TelemedicineService telemedicineService) {
        this.telemedicineService = telemedicineService;
    }

    @PostMapping("/{id}/telemedicine")
    @Operation(summary = "Create a telemedicine room for an appointment",
            description = "Creates a telemedicine room with stub video link when app.telemedicine.enabled=false. "
                    + "Real provider (Jitsi/Twilio) integration deferred.")
    public ResponseEntity<TelemedicineRoom> createRoom(@PathVariable("id") long appointmentId) {
        TelemedicineRoom room = telemedicineService.createRoom(appointmentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(room);
    }

    @GetMapping("/{id}/telemedicine")
    @Operation(summary = "Get telemedicine room for an appointment")
    public TelemedicineRoom getRoom(@PathVariable("id") long appointmentId) {
        return telemedicineService.getByAppointment(appointmentId);
    }

    @PostMapping("/telemedicine/{id}/start")
    @Operation(summary = "Start telemedicine session (CREATED -> ACTIVE)")
    public TelemedicineRoom startRoom(@PathVariable long id) {
        return telemedicineService.startRoom(id);
    }

    @PostMapping("/telemedicine/{id}/end")
    @Operation(summary = "End telemedicine session (ACTIVE -> ENDED)")
    public TelemedicineRoom endRoom(@PathVariable long id) {
        return telemedicineService.endRoom(id);
    }
}
