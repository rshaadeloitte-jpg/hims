package com.hims.grievance.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.grievance.application.GrievanceMasterService;
import com.hims.grievance.application.exceptions.GrievanceNotFoundException;
import com.hims.grievance.domain.model.GrievanceCategory;
import com.hims.grievance.domain.model.GrievanceSeverity;
import com.hims.grievance.domain.model.GrievanceSubCategory;
import com.hims.grievance.domain.repository.GrievanceCategoryRepository;
import com.hims.grievance.domain.repository.GrievanceSeverityRepository;
import com.hims.grievance.domain.repository.GrievanceSubCategoryRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Grievance classification masters (MIG-020): category / sub-category / severity.
 * Server-owned ids (per-table sequences) + audit fields.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Grievance Masters (MIG-020)", description = "Category / Sub-category / Severity master CRUD")
public class GrievanceMasterController {

    private final GrievanceMasterService service;
    private final GrievanceCategoryRepository categoryRepository;
    private final GrievanceSubCategoryRepository subCategoryRepository;
    private final GrievanceSeverityRepository severityRepository;

    public GrievanceMasterController(GrievanceMasterService service,
                                     GrievanceCategoryRepository categoryRepository,
                                     GrievanceSubCategoryRepository subCategoryRepository,
                                     GrievanceSeverityRepository severityRepository) {
        this.service = service;
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.severityRepository = severityRepository;
    }

    // ---- categories ----
    @PostMapping("/grievance-categories")
    public ResponseEntity<GrievanceCategory> createCategory(@RequestBody(required = false) JsonNode body,
                                                            @RequestParam(required = false) Long unitId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createCategory(body, unitId));
    }

    @PutMapping("/grievance-categories/{id}")
    public GrievanceCategory updateCategory(@PathVariable long id, @RequestBody(required = false) JsonNode body) {
        return service.updateCategory(id, body);
    }

    @DeleteMapping("/grievance-categories/{id}")
    public GrievanceCategory removeCategory(@PathVariable long id) {
        return service.removeCategory(id);
    }

    @GetMapping("/grievance-categories")
    public List<GrievanceCategory> listCategories() {
        return categoryRepository.findByStatusTrueOrderByIdAsc();
    }

    @GetMapping("/grievance-categories/{id}")
    public GrievanceCategory getCategory(@PathVariable long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new GrievanceNotFoundException("NOT_FOUND", "Record not found"));
    }

    // ---- sub-categories ----
    @PostMapping("/grievance-sub-categories")
    public ResponseEntity<GrievanceSubCategory> createSubCategory(@RequestBody(required = false) JsonNode body,
                                                                  @RequestParam(required = false) Long unitId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createSubCategory(body, unitId));
    }

    @PutMapping("/grievance-sub-categories/{id}")
    public GrievanceSubCategory updateSubCategory(@PathVariable long id, @RequestBody(required = false) JsonNode body) {
        return service.updateSubCategory(id, body);
    }

    @DeleteMapping("/grievance-sub-categories/{id}")
    public GrievanceSubCategory removeSubCategory(@PathVariable long id) {
        return service.removeSubCategory(id);
    }

    @GetMapping("/grievance-sub-categories")
    public List<GrievanceSubCategory> listSubCategories(@RequestParam(required = false) Long categoryId) {
        if (categoryId != null) {
            return subCategoryRepository.findByCategoryIdOrderByIdAsc(categoryId);
        }
        return subCategoryRepository.findAll();
    }

    @GetMapping("/grievance-sub-categories/{id}")
    public GrievanceSubCategory getSubCategory(@PathVariable long id) {
        return subCategoryRepository.findById(id)
                .orElseThrow(() -> new GrievanceNotFoundException("NOT_FOUND", "Record not found"));
    }

    // ---- severities ----
    @PostMapping("/grievance-severities")
    public ResponseEntity<GrievanceSeverity> createSeverity(@RequestBody(required = false) JsonNode body,
                                                            @RequestParam(required = false) Long unitId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createSeverity(body, unitId));
    }

    @PutMapping("/grievance-severities/{id}")
    public GrievanceSeverity updateSeverity(@PathVariable long id, @RequestBody(required = false) JsonNode body) {
        return service.updateSeverity(id, body);
    }

    @DeleteMapping("/grievance-severities/{id}")
    public GrievanceSeverity removeSeverity(@PathVariable long id) {
        return service.removeSeverity(id);
    }

    @GetMapping("/grievance-severities")
    public List<GrievanceSeverity> listSeverities() {
        return severityRepository.findByStatusTrueOrderByIdAsc();
    }

    @GetMapping("/grievance-severities/{id}")
    public GrievanceSeverity getSeverity(@PathVariable long id) {
        return severityRepository.findById(id)
                .orElseThrow(() -> new GrievanceNotFoundException("NOT_FOUND", "Record not found"));
    }
}
