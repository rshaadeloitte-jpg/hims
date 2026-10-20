package com.hims.rmrs.api;

import com.hims.rmrs.application.MasterService;
import com.hims.rmrs.domain.model.AuthorityMaster;
import com.hims.rmrs.domain.model.HeadMaster;
import com.hims.rmrs.domain.model.LedgerMaster;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Treasury masters CRUD-minimal API (register WP-R5): POST/GET heads,
 * authorities, ledgers. Emits NOTHING to the outbox (no events defined for masters).
 */
@RestController
@RequestMapping("/api/v1/masters")
@Tag(name = "Treasury Masters (WP-R5)", description = "RMRS & PMRD (Catalog #24): treasury heads, authorities, ledgers — CRUD-minimal POST/GET")
public class MasterController {

    private final MasterService service;

    public MasterController(MasterService service) {
        this.service = service;
    }

    // ------------------------------------------------------------------ heads

    @PostMapping("/heads")
    @Operation(summary = "Create a treasury head",
            description = "headCode + headName required; headCode unique (409 RMRS_DUPLICATE_CODE). Emits nothing.")
    public ResponseEntity<HeadMaster> createHead(@RequestBody CreateHeadRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createHead(body.headCode(), body.headName()));
    }

    @GetMapping("/heads")
    @Operation(summary = "List treasury heads")
    public List<HeadMaster> listHeads() {
        return service.listHeads();
    }

    // ------------------------------------------------------------------ authorities

    @PostMapping("/authorities")
    @Operation(summary = "Create a collecting authority",
            description = "authorityCode + authorityName required; authorityCode unique; headId optional but must exist when supplied.")
    public ResponseEntity<AuthorityMaster> createAuthority(@RequestBody CreateAuthorityRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createAuthority(body.authorityCode(), body.authorityName(), body.headId()));
    }

    @GetMapping("/authorities")
    @Operation(summary = "List collecting authorities")
    public List<AuthorityMaster> listAuthorities() {
        return service.listAuthorities();
    }

    // ------------------------------------------------------------------ ledgers

    @PostMapping("/ledgers")
    @Operation(summary = "Create a ledger",
            description = "ledgerCode + ledgerName + headId required (head_id FK NOT NULL); ledgerCode unique; head must exist (400 BEXA003).")
    public ResponseEntity<LedgerMaster> createLedger(@RequestBody CreateLedgerRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createLedger(body.ledgerCode(), body.ledgerName(), body.headId()));
    }

    @GetMapping("/ledgers")
    @Operation(summary = "List ledgers")
    public List<LedgerMaster> listLedgers() {
        return service.listLedgers();
    }

    // ------------------------------------------------------------------ requests

    public record CreateHeadRequest(String headCode, String headName) {}

    public record CreateAuthorityRequest(String authorityCode, String authorityName, Long headId) {}

    public record CreateLedgerRequest(String ledgerCode, String ledgerName, Long headId) {}
}
