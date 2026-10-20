package com.hims.billing.api;

import com.hims.billing.application.DebitNoteService;
import com.hims.billing.domain.model.DebitNote;
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

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Debit Notes (M04 gap)", description = "Positive adjustments against finalized bills. Opposite of credit note: amount ADDS to bill total. BR-BILL-007 cap: amount <= 1.5x original bill total. Requires approval (approved_by/at) before apply; emits DebitNoteApplied via outbox.")
public class DebitNoteController {

    private final DebitNoteService service;

    public DebitNoteController(DebitNoteService service) {
        this.service = service;
    }

    @PostMapping("/bills/{id}/debit-notes")
    @Operation(summary = "Issue a debit note (M04 gap; opposite of credit note)")
    public ResponseEntity<DebitNote> issue(@PathVariable long id, @RequestBody DebitNoteRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.issue(id, body.amount(), body.reason()));
    }

    @PostMapping("/debit-notes/{id}/approve")
    @Operation(summary = "Approve + apply a debit note",
            description = "Marks the note applied, ADDs amount to bill total, increments balance_self, emits DebitNoteApplied via outbox.")
    public DebitNote approve(@PathVariable long id, @RequestParam(required = false) Long approvedBy) {
        return service.approve(id, approvedBy);
    }

    @GetMapping("/debit-notes/{id}")
    public DebitNote get(@PathVariable long id) {
        return service.get(id);
    }

    @GetMapping("/bills/{id}/debit-notes")
    public List<DebitNote> listForBill(@PathVariable long id) {
        return service.listForBill(id);
    }

    @GetMapping("/patients/{id}/debit-notes")
    public List<DebitNote> listForPatient(@PathVariable long id) {
        return service.listForPatient(id);
    }

    public record DebitNoteRequest(BigDecimal amount, String reason) {}
}
