package com.hims.govt.api;

import com.hims.govt.application.AasmaanService;
import com.hims.govt.domain.model.AasmaanCase;
import com.hims.govt.domain.model.AasmaanCaseNote;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
 * Aasmaan case API (GAP-REGISTER-6 WP-T4, Catalog #28): open cases, add notes
 * (closed-case guard), forward-only status moves, filtered list.
 * Emits NOTHING to the outbox — CaseUpdated is DEFERRED per KAFKA-EVENT-CATALOG.
 */
@RestController
@RequestMapping("/api/v1/aasmaan/cases")
@Tag(name = "Aasmaan Cases (WP-T4)", description = "Aasmaan case management (Catalog #28): forward-only status 1=OPEN -> 2=IN_PROGRESS -> 3=CLOSED (adjacent steps only); notes rejected on CLOSED cases (GOVT_CASE_CLOSED); closing stamps closed_on. CaseUpdated DEFERRED - no events.")
public class AasmaanController {

    private final AasmaanService service;

    public AasmaanController(AasmaanService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Open an Aasmaan case",
            description = "{patientId, caseType}; new case is OPEN with opened_on = today. Emits nothing.")
    public ResponseEntity<AasmaanCase> create(@RequestBody CreateCaseRequest body) {
        AasmaanCase created = service.create(body.patientId(), body.caseType());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "List cases", description = "?status= (1..3 or name) and/or ?patientId= filter.")
    public List<AasmaanCase> list(@RequestParam(required = false) String status,
                                  @RequestParam(required = false) Long patientId) {
        return service.list(status == null ? null : AasmaanService.parseStatus(status), patientId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "One case by id")
    public AasmaanCase get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/notes")
    @Operation(summary = "Append a note to a case",
            description = "{note, author}; 400 GOVT_CASE_CLOSED when the case is CLOSED. Emits nothing.")
    public ResponseEntity<AasmaanCaseNote> addNote(@PathVariable long id,
                                                   @RequestBody AddNoteRequest body) {
        AasmaanCaseNote note = service.addNote(id, body.note(), body.author());
        return ResponseEntity.status(HttpStatus.CREATED).body(note);
    }

    @GetMapping("/{id}/notes")
    @Operation(summary = "Notes of a case")
    public List<AasmaanCaseNote> notes(@PathVariable long id) {
        return service.notes(id);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Forward-only status move",
            description = "{status}: 1=OPEN -> 2=IN_PROGRESS -> 3=CLOSED (adjacent steps only; "
                    + "repeat/backward/skip -> 400 GOVT_ILLEGAL_TRANSITION); closing stamps closed_on. Emits nothing.")
    public AasmaanCase updateStatus(@PathVariable long id, @RequestBody UpdateStatusRequest body) {
        return service.updateStatus(id, body.status());
    }

    public record CreateCaseRequest(Long patientId, String caseType) {}

    public record AddNoteRequest(String note, String author) {}

    public record UpdateStatusRequest(String status) {}
}
