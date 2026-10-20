package com.hims.adt.api;

import com.hims.adt.application.AdmissionService;
import com.hims.adt.application.exceptions.InvalidAdtRequestException;
import com.hims.adt.domain.model.BedReservation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Planned-admission bed holds. One ACTIVE reservation per bed (partial unique
 * index); reserved beds are invisible to auto-allocation for other patients.
 */
@RestController
@RequestMapping("/api/v1/bed-reservations")
@Tag(name = "Beds (WP-5)", description = "Bed reservations: ACTIVE -> CONSUMED on the "
        + "reserved patient's admission, CANCELLED manually, EXPIRED by the sweep")
public class BedReservationController {

    private final AdmissionService service;

    public BedReservationController(AdmissionService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Reserve a bed for a planned admission",
            description = "409 ADT_BED_RESERVED when the bed already has an ACTIVE reservation.")
    public ResponseEntity<BedReservation> reserve(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.reserve(
                        WardController.longOf(body, "bedId"),
                        WardController.longOf(body, "patientId"),
                        timestampOf(WardController.strOf(body, "expiresAt"))));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel a reservation", description = "Idempotent: non-ACTIVE rows are left untouched.")
    public ResponseEntity<Void> cancel(@PathVariable long id) {
        service.cancelReservation(id);
        return ResponseEntity.noContent().build();
    }

    private static Timestamp timestampOf(String iso) {
        if (iso == null || iso.isBlank()) {
            throw new InvalidAdtRequestException("ADT_REQ_INVALID", "expiresAt is required");
        }
        try {
            return Timestamp.from(Instant.parse(iso));
        } catch (DateTimeParseException e) {
            throw new InvalidAdtRequestException("ADT_REQ_INVALID",
                    "'" + iso + "' is not an ISO-8601 instant");
        }
    }
}
