package com.hims.pharmacy.api;

import com.hims.pharmacy.application.DrugInteractionService;
import com.hims.pharmacy.domain.model.DrugInteraction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/drug-interactions")
@Tag(name = "Drug Interactions (M07 gap)", description = "Drug-drug / drug-food / drug-disease interaction warnings (BR-PHAR-004). Acknowledged flag stores pharmacist override.")
public class DrugInteractionController {

    private final DrugInteractionService service;

    public DrugInteractionController(DrugInteractionService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Record an interaction warning")
    public ResponseEntity<DrugInteraction> record(@RequestBody RecordRequest body) {
        DrugInteraction d = service.record(body.patientId(), body.drug1Id(), body.drug2Id(),
                body.interactionType(), body.severity(), body.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(d);
    }

    @PostMapping("/{id}/acknowledge")
    @Operation(summary = "Pharmacist override — acknowledge the interaction warning")
    public DrugInteraction acknowledge(@PathVariable long id, @RequestParam(required = false) Long acknowledgedBy) {
        return service.acknowledge(id, acknowledgedBy);
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "List all interaction warnings for a patient (newest first)")
    public List<DrugInteraction> list(@PathVariable long patientId) {
        return service.listByPatient(patientId);
    }

    @GetMapping("/patient/{patientId}/unacknowledged")
    @Operation(summary = "List unacknowledged warnings for a patient")
    public List<DrugInteraction> unacknowledged(@PathVariable long patientId) {
        return service.unacknowledged(patientId);
    }

    public record RecordRequest(Long patientId, Long drug1Id, Long drug2Id,
                                 String interactionType, String severity, String description) {}
}
