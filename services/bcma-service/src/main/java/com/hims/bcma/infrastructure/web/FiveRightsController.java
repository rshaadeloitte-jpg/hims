package com.hims.bcma.infrastructure.web;

import com.hims.bcma.application.FiveRightsVerificationService;
import com.hims.bcma.domain.model.BcmaVerification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bcma/five-rights")
public class FiveRightsController {

    private final FiveRightsVerificationService service;

    public FiveRightsController(FiveRightsVerificationService service) { this.service = service; }

    @PostMapping("/verify-patient")
    public ResponseEntity<BcmaVerification> verifyPatient(@RequestParam Long bcmaId, @RequestParam Long patientId,
                                                          @RequestParam String wristbandBarcode, @RequestParam Boolean match) {
        return ResponseEntity.ok(service.verifyRightPatient(bcmaId, patientId, wristbandBarcode, match));
    }

    @PostMapping("/verify-drug")
    public ResponseEntity<BcmaVerification> verifyDrug(@RequestParam Long bcmaId, @RequestParam String drugNdc,
                                                       @RequestParam Boolean match) {
        return ResponseEntity.ok(service.verifyRightDrug(bcmaId, drugNdc, match));
    }

    @PostMapping("/verify-dose")
    public ResponseEntity<BcmaVerification> verifyDose(@RequestParam Long bcmaId, @RequestParam Boolean match) {
        return ResponseEntity.ok(service.verifyRightDose(bcmaId, match));
    }

    @PostMapping("/verify-route")
    public ResponseEntity<BcmaVerification> verifyRoute(@RequestParam Long bcmaId, @RequestParam Boolean match) {
        return ResponseEntity.ok(service.verifyRightRoute(bcmaId, match));
    }

    @PostMapping("/verify-time")
    public ResponseEntity<BcmaVerification> verifyTime(@RequestParam Long bcmaId, @RequestParam Boolean match) {
        return ResponseEntity.ok(service.verifyRightTime(bcmaId, match));
    }

    @PostMapping("/finalize/{bcmaId}")
    public ResponseEntity<BcmaVerification> finalize(@PathVariable Long bcmaId) {
        return ResponseEntity.ok(service.finalizeVerification(bcmaId));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<BcmaVerification>> byPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(service.findByPatient(patientId));
    }
}
