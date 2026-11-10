package com.hims.bcma.infrastructure.web;

import com.hims.bcma.application.ExceptionHandlingService;
import com.hims.bcma.domain.model.BCMAException;
import com.hims.bcma.domain.model.OverrideReason;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bcma/exception")
public class ExceptionHandlingController {

    private final ExceptionHandlingService service;

    public ExceptionHandlingController(ExceptionHandlingService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<BCMAException> create(@RequestParam Long patientId, @RequestParam String drugNdc,
                                                 @RequestParam String exceptionType, @RequestParam String reason,
                                                 @RequestParam Long overrideNurse, @RequestParam Long wardId,
                                                 @RequestParam Long scannedBy) {
        return ResponseEntity.ok(service.createException(patientId, drugNdc, exceptionType, reason, overrideNurse, wardId, scannedBy));
    }

    @PostMapping("/override/{exceptionId}")
    public ResponseEntity<BCMAException> override(@PathVariable Long exceptionId, @RequestParam String justification,
                                                   @RequestParam Long overrideNurse) {
        return ResponseEntity.ok(service.applyOverride(exceptionId, justification, overrideNurse));
    }

    @PostMapping("/reason")
    public ResponseEntity<OverrideReason> addReason(@RequestParam String code, @RequestParam String description,
                                                     @RequestParam String category) {
        return ResponseEntity.ok(service.addReason(code, description, category));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<BCMAException>> byPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(service.findByPatient(patientId));
    }
}
