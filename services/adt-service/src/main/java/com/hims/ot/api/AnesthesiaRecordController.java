package com.hims.ot.api;

import com.hims.ot.application.AnesthesiaRecordService;
import com.hims.ot.domain.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

@RestController
@RequestMapping("/ot/anesthesia")
public class AnesthesiaRecordController {

    private final AnesthesiaRecordService service;

    public AnesthesiaRecordController(AnesthesiaRecordService service) {
        this.service = service;
    }

    @PostMapping("/pre-op")
    public ResponseEntity<AnesthesiaRecord> createPreOp(@RequestParam Long scheduleId, @RequestParam Long patientId,
                                                        @RequestParam String asaGrade, @RequestParam String airway,
                                                        @RequestParam String comorbidities) {
        return ResponseEntity.ok(service.createPreOp(scheduleId, patientId, asaGrade, airway, comorbidities));
    }

    @PostMapping("/{recordId}/vitals")
    public ResponseEntity<VitalSignRecord> addVital(@PathVariable Long recordId, @RequestParam Timestamp recordedAt,
                                                    @RequestParam Integer hr, @RequestParam Integer bpSys,
                                                    @RequestParam Integer bpDia, @RequestParam Integer spo2,
                                                    @RequestParam Integer rr, @RequestParam BigDecimal temp) {
        return ResponseEntity.ok(service.addVital(recordId, recordedAt, hr, bpSys, bpDia, spo2, rr, temp));
    }

    @PostMapping("/{recordId}/drugs")
    public ResponseEntity<AnesthesiaDrug> addDrug(@PathVariable Long recordId, @RequestParam String drugName,
                                                  @RequestParam String dose, @RequestParam String route,
                                                  @RequestParam Timestamp administeredAt) {
        return ResponseEntity.ok(service.addDrug(recordId, drugName, dose, route, administeredAt));
    }

    @PostMapping("/{recordId}/post-op")
    public ResponseEntity<AnesthesiaRecord> completePostOp(@PathVariable Long recordId,
                                                           @RequestParam Integer pacuMinutes,
                                                           @RequestParam Integer recoveryScore,
                                                           @RequestParam String complications) {
        return ResponseEntity.ok(service.completePostOp(recordId, pacuMinutes, recoveryScore, complications));
    }

    @GetMapping("/{recordId}/vitals")
    public ResponseEntity<List<VitalSignRecord>> findVitals(@PathVariable Long recordId) {
        return ResponseEntity.ok(service.findVitals(recordId));
    }
}
