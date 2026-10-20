package com.hims.ordering.api;

import com.hims.ordering.application.PrescriptionService;
import com.hims.ordering.application.exceptions.InvalidPrescriptionException;
import com.hims.ordering.domain.model.Prescription;
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

import java.util.List;
import java.util.Map;

/**
 * Ordering &amp; Prescription API (GAP-REGISTER-4 WP-10, Catalog #4): prescription
 * header+lines creation with validation guards, get, list (?patientId=&amp;status=),
 * cancel (guard ACTIVE only). E-06 PrescriptionCreated is emitted via the
 * transactional outbox on CREATE ONLY.
 */
@RestController
@RequestMapping("/api/v1/prescriptions")
@Tag(name = "Prescriptions (WP-10)", description = "Ordering & Prescription (Catalog #4): header+lines, status 1=ACTIVE 2=COMPLETED 3=CANCELLED, cancel guard ACTIVE-only; E-06 PrescriptionCreated (prescription-created.v1, partition key encounterId) on create ONLY")
public class PrescriptionController {

    private final PrescriptionService service;

    public PrescriptionController(PrescriptionService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create a prescription with >=1 line",
            description = "patientId + prescriberId required; encounterId XOR admissionId (400 BEXA003 when both); "
                    + "lines[{itemId,qty,dose,frequency}] >= 1, qty > 0. Emits E-06 PrescriptionCreated once.")
    public ResponseEntity<Prescription> create(@RequestBody CreatePrescriptionRequest body) {
        List<PrescriptionService.LineInput> lines = body.lines() == null ? null : body.lines().stream()
                .map(l -> new PrescriptionService.LineInput(
                        lng(l.get("itemId")), qty(l.get("qty")),
                        str(l.get("dose")), str(l.get("frequency"))))
                .toList();
        Prescription created = service.create(body.patientId(), body.encounterId(),
                body.admissionId(), body.prescriberId(), body.wardId(), lines);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "List prescriptions", description = "?status= (1..3 or name) and/or ?patientId= filter.")
    public List<Prescription> list(@RequestParam(required = false) String status,
                                   @RequestParam(required = false) Long patientId) {
        return service.list(status == null ? null : parseStatus(status), patientId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Full prescription with lines")
    public Prescription get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a prescription", description = "Guard: ACTIVE (1) only -> CANCELLED (3); 400 ORD_ILLEGAL_TRANSITION otherwise. Emits nothing.")
    public Prescription cancel(@PathVariable long id) {
        return service.cancel(id);
    }

    // ------------------------------------------------------------------ helpers

    /** Status filter: numeric (1..3) or name (ACTIVE, COMPLETED, CANCELLED). */
    static Short parseStatus(String raw) {
        try {
            return Short.valueOf(raw.trim());
        } catch (NumberFormatException ignored) {
            // fall through to name mapping
        }
        return switch (raw.trim().toUpperCase()) {
            case "ACTIVE" -> 1;
            case "COMPLETED" -> 2;
            case "CANCELLED" -> 3;
            default -> throw new InvalidPrescriptionException("BEXA003",
                    "Unknown status: " + raw);
        };
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static Long lng(Object v) {
        return v == null ? null : Long.valueOf(String.valueOf(v));
    }

    private static java.math.BigDecimal qty(Object v) {
        return v == null ? null : new java.math.BigDecimal(String.valueOf(v));
    }

    public record CreatePrescriptionRequest(Long patientId, Long encounterId, Long admissionId,
                                            Long prescriberId, Long wardId,
                                            List<Map<String, Object>> lines) {}
}
