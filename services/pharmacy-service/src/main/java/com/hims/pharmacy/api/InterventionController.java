package com.hims.pharmacy.api;

import com.hims.pharmacy.application.DrugInterventionService;
import com.hims.pharmacy.domain.model.DrugIntervention;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Pharmacist drug interventions (register WP-R2): record + list by patient. */
@RestController
@RequestMapping("/api/v1/interventions")
@Tag(name = "Drug Interventions (WP-R2)", description = "Pharmacist clinical interventions: interventionType, notes, pharmacistId; listed per patient")
public class InterventionController {

    private final DrugInterventionService service;

    public InterventionController(DrugInterventionService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Record a drug intervention",
            description = "patientId, interventionType and pharmacistId required (400 BEXA003 otherwise); saleId optional. Emits nothing.")
    public ResponseEntity<DrugIntervention> create(@RequestBody CreateInterventionRequest body) {
        DrugIntervention created = service.create(body.patientId(), body.saleId(),
                body.interventionType(), body.notes(), body.pharmacistId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "List interventions for one patient", description = "?patientId= required.")
    public List<DrugIntervention> list(@RequestParam Long patientId) {
        return service.listByPatient(patientId);
    }

    public record CreateInterventionRequest(Long patientId, Long saleId,
                                            String interventionType, String notes,
                                            Long pharmacistId) {}
}
