package com.hims.pharmacy.api;

import com.hims.pharmacy.application.HomeMedicationService;
import com.hims.pharmacy.application.IvCompoundEnhancementService;
import com.hims.pharmacy.domain.model.HomeMedication;
import com.hims.pharmacy.domain.model.IvCompound;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/pharmacy-discharge")
@Tag(name = "Pharmacy Discharge / Billing", description = "Home meds discharge + IV compound billing integration for M07")
public class PharmacyDischargeController {
    private final HomeMedicationService homeService;
    private final IvCompoundEnhancementService ivEnhance;

    public PharmacyDischargeController(HomeMedicationService homeService, IvCompoundEnhancementService ivEnhance) {
        this.homeService = homeService; this.ivEnhance = ivEnhance;
    }

    @PostMapping("/home-medications")
    @Operation(summary = "Prescribe discharge home medication")
    public ResponseEntity<HomeMedication> prescribe(@RequestBody Map<String,Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(homeService.prescribe(
                lng(body.get("patientId")), lng(body.get("encounterId")), lng(body.get("admissionId")),
                lng(body.get("itemId")), str(body.get("itemName")), str(body.get("dosage")),
                str(body.get("frequency")), intVal(body.get("durationDays")), bigDecimal(body.get("quantity")),
                str(body.get("uom")), str(body.get("instructions")), lng(body.get("prescribedBy"))));
    }

    @PostMapping("/home-medications/{id}/dispense")
    @Operation(summary = "Mark home medication dispensed")
    public ResponseEntity<HomeMedication> dispense(@PathVariable Long id, @RequestBody Map<String,Object> body) {
        return ResponseEntity.ok(homeService.markDispensed(id, lng(body.get("userId"))));
    }

    @GetMapping("/home-medications/patient/{patientId}")
    public List<HomeMedication> byPatient(@PathVariable Long patientId) { return homeService.listForPatient(patientId); }

    @PostMapping("/iv-compounds/bill")
    @Operation(summary = "Finalize IV compound for billing (integration)")
    public ResponseEntity<IvCompound> billIv(@RequestBody Map<String,Object> body) {
        return ResponseEntity.ok(ivEnhance.finalizeForBilling(
                lng(body.get("compoundId")), bigDecimal(body.get("billedAmount")), lng(body.get("userId"))));
    }

    private Long lng(Object o) { return o == null ? null : Long.valueOf(o.toString()); }
    private String str(Object o) { return o == null ? null : o.toString(); }
    private BigDecimal bigDecimal(Object o) { return o == null ? null : new BigDecimal(o.toString()); }
    private Integer intVal(Object o) { return o == null ? null : Integer.valueOf(o.toString()); }
}
