package com.hims.pharmacy.api;

import com.hims.pharmacy.application.HomeMedicationService;
import com.hims.pharmacy.domain.model.HomeMedication;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Discharge home medication API.
 * Emits NOTHING to the outbox.
 */
@RestController
@RequestMapping("/api/v1/home-medications")
@Tag(name = "Home Medications", description = "Discharge home medication prescription lines. No events emitted.")
public class HomeMedicationController {

    private final HomeMedicationService service;

    public HomeMedicationController(HomeMedicationService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Prescribe a home medication line",
            description = "patientId, encounterId, itemId, itemName required")
    public ResponseEntity<HomeMedication> prescribe(@RequestBody Map<String, Object> body) {
        HomeMedication result = service.prescribe(
                lng(body.get("patientId")),
                lng(body.get("encounterId")),
                lng(body.get("admissionId")),
                lng(body.get("itemId")),
                str(body.get("itemName")),
                str(body.get("dosage")),
                str(body.get("frequency")),
                intVal(body.get("durationDays")),
                bigDecimal(body.get("quantity")),
                str(body.get("uom")),
                str(body.get("instructions")),
                lng(body.get("prescribedBy")));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/{id}/dispense")
    @Operation(summary = "Mark home medication as dispensed (patient collected from pharmacy)")
    public HomeMedication markDispensed(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        return service.markDispensed(id, lng(body.get("userId")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a home medication record")
    public HomeMedication get(@PathVariable("id") Long id) {
        return service.get(id);
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "List home medications for a patient")
    public List<HomeMedication> listForPatient(@PathVariable Long patientId) {
        return service.listForPatient(patientId);
    }

    @GetMapping("/encounter/{encounterId}")
    @Operation(summary = "List home medications for an encounter")
    public List<HomeMedication> listForEncounter(@PathVariable Long encounterId) {
        return service.listForEncounter(encounterId);
    }

    @GetMapping("/pending-dispense")
    @Operation(summary = "List home medications not yet dispensed")
    public List<HomeMedication> listPendingDispense() {
        return service.listPendingDispense();
    }

    private static String str(Object v) { return v == null ? null : String.valueOf(v); }
    private static Long lng(Object v) { return v == null ? null : Long.valueOf(String.valueOf(v)); }
    private static Integer intVal(Object v) { return v == null ? null : Integer.valueOf(String.valueOf(v)); }
    private static BigDecimal bigDecimal(Object v) { return v == null ? null : new BigDecimal(String.valueOf(v)); }
}
