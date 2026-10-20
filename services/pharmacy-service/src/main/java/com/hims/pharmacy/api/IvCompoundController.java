package com.hims.pharmacy.api;

import com.hims.pharmacy.application.IvCompoundService;
import com.hims.pharmacy.domain.model.IvCompound;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

/**
 * IV compounding API.
 * Emits NOTHING to the outbox.
 */
@RestController
@RequestMapping("/api/v1/iv-compounds")
@Tag(name = "IV Compounding", description = "Sterile IV compounding orders: create -> compound -> verify -> administer. Status 1=PENDING 2=COMPOUNDED 3=VERIFIED 4=ADMINISTERED 5=CANCELLED. No events emitted.")
public class IvCompoundController {

    private final IvCompoundService service;

    public IvCompoundController(IvCompoundService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create an IV compound with >= 1 line",
            description = "lines[{itemId,itemName,quantity,uom,batchId?,expiryDate?}]")
    public ResponseEntity<IvCompound> create(@RequestBody Map<String, Object> body) {
        List<Map<String, Object>> rawLines = (List<Map<String, Object>>) body.get("lines");
        List<IvCompoundService.CompoundLineInput> lines = rawLines.stream()
                .map(l -> new IvCompoundService.CompoundLineInput(
                        lng(l.get("itemId")),
                        str(l.get("itemName")),
                        bigDecimal(l.get("quantity")),
                        str(l.get("uom")),
                        str(l.get("batchId")),
                        l.get("expiryDate") == null ? null : java.sql.Date.valueOf(str(l.get("expiryDate")))))
                .toList();
        IvCompound result = service.create(
                lng(body.get("patientId")),
                lng(body.get("encounterId")),
                lng(body.get("prescriptionId")),
                lines,
                bigDecimal(body.get("totalVolumeMl")),
                body.get("beyondUseDate") == null ? null : Timestamp.valueOf(str(body.get("beyondUseDate"))),
                str(body.get("remarks")),
                lng(body.get("userId")));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/{id}/compound")
    @Operation(summary = "Mark compound as COMPOUNDED (preparation complete)")
    public IvCompound compound(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        return service.compound(id, lng(body.get("userId")));
    }

    @PostMapping("/{id}/verify")
    @Operation(summary = "Verify the compounded preparation (pharmacist sign-off)")
    public IvCompound verify(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        return service.verify(id, lng(body.get("userId")));
    }

    @PostMapping("/{id}/administer")
    @Operation(summary = "Mark as ADMINISTERED (nurse administers to patient)")
    public IvCompound administer(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        return service.administer(id, lng(body.get("userId")));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel (only from PENDING or COMPOUNDED)")
    public IvCompound cancel(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        return service.cancel(id, lng(body.get("userId")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an IV compound with lines")
    public IvCompound get(@PathVariable("id") Long id) {
        return service.get(id);
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "List IV compounds for a patient")
    public List<IvCompound> listForPatient(@PathVariable Long patientId) {
        return service.listForPatient(patientId);
    }

    @GetMapping
    @Operation(summary = "List IV compounds by status", description = "?status=1..5 filter")
    public List<IvCompound> listByStatus(@RequestParam(required = false) Short status) {
        return service.listByStatus(status);
    }

    private static String str(Object v) { return v == null ? null : String.valueOf(v); }
    private static Long lng(Object v) { return v == null ? null : Long.valueOf(String.valueOf(v)); }
    private static BigDecimal bigDecimal(Object v) { return v == null ? null : new BigDecimal(String.valueOf(v)); }
}
