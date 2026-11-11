package com.hims.kiosk.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.kiosk.application.KioskService;
import com.hims.kiosk.domain.model.KioskAppointment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/kiosk/appointments")
@Tag(name = "Kiosk Self-Appointment (M31)")
public class KioskAppointmentController {
    private final KioskService service;
    public KioskAppointmentController(KioskService service) { this.service = service; }

    @PostMapping
    @Operation(summary = "Book self-appointment")
    public ResponseEntity<KioskAppointment> book(@RequestBody JsonNode body,
                                                  @RequestParam(required = false) Long unitId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.bookAppointment(body, unitId));
    }

    @GetMapping
    @Operation(summary = "List appointments by date")
    public List<KioskAppointment> list(@RequestParam(required = false) LocalDate date) {
        return service.listAppointments(date);
    }
}
