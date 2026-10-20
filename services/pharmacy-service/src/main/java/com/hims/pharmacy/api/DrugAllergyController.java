package com.hims.pharmacy.api;

import com.hims.pharmacy.application.DrugAllergyService;
import com.hims.pharmacy.domain.model.DrugAllergy;
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
@RequestMapping("/api/v1/drug-allergies")
@Tag(name = "Drug Allergies (M07 gap)", description = "Patient drug allergy list (legacy t_patientallergy parity). Severity gates dispense (BR-PHAR-004).")
public class DrugAllergyController {

    private final DrugAllergyService service;

    public DrugAllergyController(DrugAllergyService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Add a drug allergy record for a patient",
            description = "Body: patientId, allergen, allergyType (DRUG|CLASS|FOOD)?, severity (MILD|MODERATE|SEVERE|ANAPHYLAXIS), reactionDescription?, recordedBy?")
    public ResponseEntity<DrugAllergy> add(@RequestBody AddRequest body) {
        DrugAllergy a = service.add(body.patientId(), body.allergen(), body.allergyType(),
                body.severity(), body.reactionDescription(), body.recordedBy());
        return ResponseEntity.status(HttpStatus.CREATED).body(a);
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "List drug allergies for a patient (newest first)")
    public List<DrugAllergy> list(@PathVariable long patientId) {
        return service.listByPatient(patientId);
    }

    @GetMapping("/patient/{patientId}/match")
    @Operation(summary = "Check if a patient has a recorded allergy for a given allergen (case-insensitive)")
    public List<DrugAllergy> match(@PathVariable long patientId, @RequestParam String allergen) {
        return service.findMatching(patientId, allergen);
    }

    public record AddRequest(Long patientId, String allergen, String allergyType, String severity,
                              String reactionDescription, Long recordedBy) {}
}
