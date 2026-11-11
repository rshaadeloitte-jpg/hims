package com.hims.kiosk.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.kiosk.application.KioskService;
import com.hims.kiosk.domain.model.KioskLabResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/kiosk/lab-results")
@Tag(name = "Kiosk Lab Result Upload (M31)")
public class KioskLabResultController {
    private final KioskService service;
    public KioskLabResultController(KioskService service) { this.service = service; }

    @PostMapping
    @Operation(summary = "Upload lab result")
    public ResponseEntity<KioskLabResult> upload(@RequestBody JsonNode body,
                                                  @RequestParam(required = false) Long unitId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.uploadResult(body, unitId));
    }

    @GetMapping
    @Operation(summary = "List lab results by patient")
    public List<KioskLabResult> list(@RequestParam Long patientId) {
        return service.listLabResults(patientId);
    }
}
