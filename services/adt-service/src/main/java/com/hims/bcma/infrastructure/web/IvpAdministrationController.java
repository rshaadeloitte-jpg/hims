package com.hims.bcma.infrastructure.web;

import com.hims.bcma.application.IvpAdministrationService;
import com.hims.bcma.domain.model.IvpAdministration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/bcma/ivp")
public class IvpAdministrationController {

    private final IvpAdministrationService service;

    public IvpAdministrationController(IvpAdministrationService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<IvpAdministration> record(@RequestParam Long patientId, @RequestParam String drugName,
                                                     @RequestParam BigDecimal doseAmount, @RequestParam String doseUnit,
                                                     @RequestParam BigDecimal rateMlPerMin, @RequestParam Boolean dilutionRequired,
                                                     @RequestParam BigDecimal dilutionVolumeMl, @RequestParam Long administeredBy,
                                                     @RequestParam Long wardId) {
        return ResponseEntity.ok(service.recordIVP(patientId, drugName, doseAmount, doseUnit, rateMlPerMin,
                dilutionRequired, dilutionVolumeMl, administeredBy, wardId));
    }

    @PostMapping("/verify-dilution/{ivpId}")
    public ResponseEntity<IvpAdministration> verifyDilution(@PathVariable Long ivpId, @RequestParam Boolean verified) {
        return ResponseEntity.ok(service.verifyDilution(ivpId, verified));
    }

    @PostMapping("/complete/{ivpId}")
    public ResponseEntity<IvpAdministration> complete(@PathVariable Long ivpId, @RequestParam Long administeredBy) {
        return ResponseEntity.ok(service.completeIVP(ivpId, administeredBy));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<IvpAdministration>> byPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(service.findByPatient(patientId));
    }
}
