package com.hims.bcma.infrastructure.web;

import com.hims.bcma.application.BcmaModuleService;
import com.hims.bcma.domain.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.List;

@RestController
@RequestMapping("/api/bcma")
public class BcmaController {

    private final BcmaModuleService service;

    public BcmaController(BcmaModuleService service) {
        this.service = service;
    }

    @PostMapping("/scan")
    public ResponseEntity<PatientDrugMatch> scanMedication(@RequestParam Long patientId,
                                                           @RequestParam String drugIen,
                                                           @RequestParam(required = false) String drugName,
                                                           @RequestParam(required = false) Long scannedBy,
                                                           @RequestParam(required = false) Long wardId) {
        return ResponseEntity.ok(service.scanMedication(patientId, drugIen, drugName, scannedBy, wardId));
    }

    @PostMapping("/ivp")
    public ResponseEntity<IVPEvent> recordIVP(@RequestParam Long patientId,
                                              @RequestParam String drugName,
                                              @RequestParam(required = false) String dose,
                                              @RequestParam(required = false) String rate,
                                              @RequestParam(required = false) Timestamp startTime,
                                              @RequestParam(required = false) Timestamp endTime,
                                              @RequestParam Long nurseId,
                                              @RequestParam(required = false) Long wardId) {
        return ResponseEntity.ok(service.recordIVP(patientId, drugName, dose, rate, startTime, endTime, nurseId, wardId));
    }

    @PostMapping("/exception")
    public ResponseEntity<BCMAException> logException(@RequestParam Long patientId,
                                                      @RequestParam String drugIen,
                                                      @RequestParam BCMAException.ExceptionType exceptionType,
                                                      @RequestParam String reason,
                                                      @RequestParam(required = false) Long overrideNurse,
                                                      @RequestParam(required = false) Timestamp exceptionTime,
                                                      @RequestParam(required = false) Long wardId,
                                                      @RequestParam(required = false) Long scannedBy) {
        return ResponseEntity.ok(service.logException(patientId, drugIen, exceptionType, reason, overrideNurse, exceptionTime, wardId, scannedBy));
    }

    @GetMapping("/stock")
    public ResponseEntity<List<MedicationStock>> getStock(@RequestParam(required = false) String drugIen,
                                                          @RequestParam(required = false) Long wardId) {
        return ResponseEntity.ok(service.getStockByMedication(drugIen, wardId));
    }

    @GetMapping("/scan/{scanId}")
    public ResponseEntity<PatientDrugMatch> getScan(@PathVariable Long scanId) {
        PatientDrugMatch m = service.getScanResult(scanId);
        return m != null ? ResponseEntity.ok(m) : ResponseEntity.notFound().build();
    }
}
