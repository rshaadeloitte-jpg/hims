package com.hims.forms.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.forms.application.FormService;
import com.hims.forms.application.exceptions.FormNotFoundException;
import com.hims.forms.domain.model.FormDefinition;
import com.hims.forms.domain.model.FormSubmission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/forms")
@Tag(name = "Clinical Forms (M40)", description = "TIU Clinical Forms engine — versioned JSON-schema store + submit (ported from M_NOTETITLEMASTER + TIU XML engine + T_PATIENTNOTE/T_NOTEDETAILS)")
public class FormController {

    private final FormService service;
    private final ObjectMapper objectMapper;

    public FormController(FormService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    @Operation(summary = "List all active forms",
            description = "Returns active form definitions ordered by specialty/code. Each includes code, specialty, version, and schema_json. Pass ?specialty=NURSING to filter.")
    public List<FormDefinition> list(@RequestParam(required = false) String specialty) {
        return service.listFormsBySpecialty(specialty);
    }

    @GetMapping("/specialties")
    @Operation(summary = "List distinct specialties with active forms",
            description = "Returns sorted list of specialties (NURSING, OBGYN, etc.) for UI filter dropdown.")
    public List<String> specialties() {
        return service.listSpecialties();
    }

    @GetMapping("/{id}/schema")
    @Operation(summary = "Get form schema by id (versioned JSON Schema)",
            description = "Returns the versioned JSON Schema for rendering. 404 if form not found or inactive.")
    public ResponseEntity<Map<String, Object>> getSchema(@PathVariable long id) {
        FormDefinition f = service.getSchema(id);
        return ResponseEntity.ok(toMap(f));
    }

    @GetMapping("/code/{code}/schema")
    @Operation(summary = "Get form schema by code (alias for id)")
    public ResponseEntity<Map<String, Object>> getSchemaByCode(@PathVariable String code) {
        FormDefinition f = service.getSchemaByCode(code);
        return ResponseEntity.ok(toMap(f));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit a filled form (creates t_form_submission)",
            description = "Body is the JSON document matching the form's schema. Required fields per schema validated (400 if missing). patientId/encounterId optional opaque IDs in phase 1.")
    public ResponseEntity<FormSubmission> submit(
            @PathVariable long id,
            @RequestBody Map<String, Object> body,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long encounterId,
            @RequestParam(required = false) Long submittedBy) {
        // body may contain wrapper {submission: {...}, patientId: ...} or flat document — handle both
        Object payload = body;
        if (body.containsKey("submission") && body.get("submission") instanceof Map) {
            payload = body.get("submission");
            if (body.containsKey("patientId") && patientId == null) {
                try { patientId = Long.valueOf(String.valueOf(body.get("patientId"))); } catch (Exception ignored) {}
            }
            if (body.containsKey("encounterId") && encounterId == null) {
                try { encounterId = Long.valueOf(String.valueOf(body.get("encounterId"))); } catch (Exception ignored) {}
            }
        }
        FormSubmission sub = service.submit(id, patientId, encounterId, payload, submittedBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(sub);
    }

    @GetMapping("/{id}/submissions")
    @Operation(summary = "List submissions for a form")
    public List<FormSubmission> listSubmissions(@PathVariable long id) {
        return service.listSubmissions(id);
    }

    @GetMapping("/submissions/patient/{patientId}")
    @Operation(summary = "List all submissions for a patient (cross-form)",
            description = "Returns all submissions across all forms for the patient, newest first. Used by clinical tabs / longitudinal view.")
    public List<FormSubmission> listByPatient(@PathVariable long patientId) {
        return service.listSubmissionsByPatient(patientId);
    }

    @GetMapping("/submissions/encounter/{encounterId}")
    @Operation(summary = "List all submissions for an encounter")
    public List<FormSubmission> listByEncounter(@PathVariable long encounterId) {
        return service.listSubmissionsByEncounter(encounterId);
    }

    @GetMapping("/{id}/submissions/{submissionId}")
    @Operation(summary = "Get a single submission by id")
    public FormSubmission getSubmission(@PathVariable long id, @PathVariable long submissionId) {
        return service.getSubmission(id, submissionId);
    }

    @DeleteMapping("/{id}/submissions/{submissionId}")
    @Operation(summary = "Soft-delete a submission (sets status=false; legacy parity T_PATIENTNOTE.deleteNote)",
            description = "Per legacy T_PATIENTNOTE.deleteNote semantics: the row remains for audit, status is flipped to false. Submission is no longer returned by default lists but is retrievable by id for audit replay.")
    public ResponseEntity<FormSubmission> deleteSubmission(@PathVariable long id, @PathVariable long submissionId) {
        FormSubmission sub = service.softDeleteSubmission(id, submissionId);
        return ResponseEntity.status(HttpStatus.OK).body(sub);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(FormDefinition f) {
        try {
            Map<String, Object> schema = objectMapper.readValue(f.getSchemaJson(), Map.class);
            return Map.of(
                    "id", f.getId(),
                    "code", f.getCode(),
                    "name", f.getName(),
                    "specialty", f.getSpecialty(),
                    "description", f.getDescription() != null ? f.getDescription() : "",
                    "version", f.getVersion(),
                    "schema", schema,
                    "legacyTiuxmlRef", f.getLegacyTiuxmlRef() != null ? f.getLegacyTiuxmlRef() : ""
            );
        } catch (Exception e) {
            return Map.of(
                    "id", f.getId(),
                    "code", f.getCode(),
                    "name", f.getName(),
                    "specialty", f.getSpecialty(),
                    "version", f.getVersion(),
                    "schema", f.getSchemaJson(),
                    "error", "schema_json parse failed: " + e.getMessage()
            );
        }
    }
}
