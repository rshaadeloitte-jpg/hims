package com.hims.chemotherapy.api;

import com.hims.chemotherapy.application.ChemotherapyService;
import com.hims.chemotherapy.domain.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/chemo")
public class ChemotherapyController {
    private final ChemotherapyService service;
    public ChemotherapyController(ChemotherapyService service) { this.service = service; }

    @GetMapping("/plan/{patientId}")
    public ResponseEntity<List<ChemoPlan>> getPlan(@PathVariable Long patientId) { return ResponseEntity.ok(service.getPlanByPatient(patientId)); }

    @PostMapping("/plan")
    public ResponseEntity<ChemoPlan> postPlan(@RequestBody ChemoPlan plan) { return ResponseEntity.ok(service.savePlan(plan)); }

    @GetMapping("/regimen/{planId}")
    public ResponseEntity<List<ChemoRegimenDetail>> getRegimen(@PathVariable Long planId) { return ResponseEntity.ok(service.getRegimenLines(planId)); }

    @PostMapping("/regimen-line")
    public ResponseEntity<ChemoRegimenDetail> postRegimenLine(@RequestBody ChemoRegimenDetail line) { return ResponseEntity.ok(service.saveRegimenLine(line)); }

    @GetMapping("/regimens/patient/{patientId}")
    public ResponseEntity<List<ChemoRegimen>> getRegimensByPatient(@PathVariable Long patientId) { return ResponseEntity.ok(service.getRegimenByPatient(patientId)); }

    @GetMapping("/cycle/{planId}")
    public ResponseEntity<List<ChemoCycle>> getCycle(@PathVariable Long planId) { return ResponseEntity.ok(service.getCycle(planId)); }

    @PostMapping("/cycle/{cycleId}/complete")
    public ResponseEntity<ChemoCycle> completeCycle(@PathVariable Long cycleId) { return ResponseEntity.ok(service.completeCycle(cycleId)); }

    @GetMapping("/coversheet/{planId}")
    public ResponseEntity<ChemoCoversheet> getCoversheet(@PathVariable Long planId) { return ResponseEntity.ok(service.getCoversheet(planId)); }
}
