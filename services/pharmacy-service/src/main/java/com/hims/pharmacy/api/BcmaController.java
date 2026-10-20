package com.hims.pharmacy.api;

import com.hims.pharmacy.application.BcmaService;
import com.hims.pharmacy.domain.model.BcmaVerification;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * BCMA five-rights verification API (BR-PHAR-BCMA).
 * Emits NOTHING to the outbox.
 */
@RestController
@RequestMapping("/api/v1/bcma")
@Tag(name = "BCMA Verification (BR-PHAR-BCMA)", description = "Five-rights medication administration check: right-patient/drug/dose/route/time scan, override audit trail, administration marking. No events emitted.")
public class BcmaController {

    private final BcmaService service;

    public BcmaController(BcmaService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Record a five-rights scan",
            description = "patientId, saleId, detailId required. rightPatient/rightDrug/rightDose/rightRoute/rightTime booleans.")
    public ResponseEntity<BcmaVerification> recordScan(@RequestBody Map<String, Object> body) {
        BcmaVerification result = service.recordScan(
                lng(body.get("patientId")),
                lng(body.get("saleId")),
                lng(body.get("detailId")),
                lng(body.get("encounterId")),
                bool(body.get("rightPatient")),
                bool(body.get("rightDrug")),
                bool(body.get("rightDose")),
                bool(body.get("rightRoute")),
                bool(body.get("rightTime")),
                lng(body.get("userId")));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/{id}/override")
    @Operation(summary = "Override a failed-rights scan (requires reason)")
    public BcmaVerification override(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        return service.override(id, str(body.get("reason")), lng(body.get("userId")));
    }

    @PostMapping("/{id}/administer")
    @Operation(summary = "Mark administration complete (allowed when all rights passed OR overridden)")
    public BcmaVerification administer(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        return service.markAdministered(id, lng(body.get("userId")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a BCMA verification record")
    public BcmaVerification get(@PathVariable("id") Long id) {
        return service.get(id);
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "List BCMA records for a patient")
    public List<BcmaVerification> listForPatient(@PathVariable Long patientId) {
        return service.listForPatient(patientId);
    }

    @GetMapping("/sale/{saleId}")
    @Operation(summary = "List BCMA records for a sale")
    public List<BcmaVerification> listForSale(@PathVariable Long saleId) {
        return service.listForSale(saleId);
    }

    @GetMapping("/pending-overrides")
    @Operation(summary = "List records where rights failed and not yet administered")
    public List<BcmaVerification> listPendingOverrides() {
        return service.listPendingOverrides();
    }

    private static String str(Object v) { return v == null ? null : String.valueOf(v); }
    private static Long lng(Object v) { return v == null ? null : Long.valueOf(String.valueOf(v)); }
    private static boolean bool(Object v) { return v != null && (Boolean.parseBoolean(String.valueOf(v))); }
}
