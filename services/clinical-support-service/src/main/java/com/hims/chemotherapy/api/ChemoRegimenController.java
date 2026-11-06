package com.hims.chemotherapy.api;

import com.hims.chemotherapy.application.ChemoRegimenService;
import com.hims.chemotherapy.domain.model.ChemoRegimen;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping({"/api/v1/regimens", "/v1/regimens"})
@Tag(name = "Chemotherapy Regimens (M29)", description = "APIs for chemotherapy regimen lifecycle, cycles, and scheduling")
public class ChemoRegimenController {

    private final ChemoRegimenService service;

    public ChemoRegimenController(ChemoRegimenService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List chemotherapy regimens", description = "Query regimens by optional patientId and status filters.")
    public List<ChemoRegimen> listRegimens(@RequestParam(required = false) Long patientId,
                                           @RequestParam(required = false) String status) {
        return service.getAllRegimens(patientId, status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get chemotherapy regimen by ID")
    public ResponseEntity<ChemoRegimen> getRegimen(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create a new chemotherapy regimen")
    public ResponseEntity<ChemoRegimen> createRegimen(@RequestBody ChemoRegimen regimen) {
        ChemoRegimen created = service.createRegimen(regimen);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing chemotherapy regimen")
    public ResponseEntity<ChemoRegimen> updateRegimen(@PathVariable Long id, @RequestBody ChemoRegimen regimen) {
        ChemoRegimen updated = service.updateRegimen(id, regimen);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/advance-cycle")
    @Operation(summary = "Advance regimen to next cycle")
    public ResponseEntity<ChemoRegimen> advanceCycle(@PathVariable Long id) {
        ChemoRegimen advanced = service.advanceCycle(id);
        return ResponseEntity.ok(advanced);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update regimen status")
    public ResponseEntity<ChemoRegimen> updateStatus(@PathVariable Long id, @RequestParam String status) {
        ChemoRegimen updated = service.updateStatus(id, status);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete chemotherapy regimen by ID")
    public ResponseEntity<Void> deleteRegimen(@PathVariable Long id) {
        service.deleteRegimen(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
    }
}
