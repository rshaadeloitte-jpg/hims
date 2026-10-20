package com.hims.rcm.api;

import com.hims.rcm.application.NhcxService;
import com.hims.rcm.domain.model.EligibilityCheck;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Coverage-eligibility API (register WP-R4): create check (status 0) and record the
 * simulated payor response (pins status 2=ACKNOWLEDGED). External gateway OUT OF SCOPE.
 */
@RestController
@RequestMapping("/api/v1/coverage-eligibility")
@Tag(name = "Coverage Eligibility (WP-R4)", description = "RCM & Claims (Catalog #23): coverage-eligibility checks, status 0=INITIATED 1=SENT 2=ACKNOWLEDGED 3=COMPLETED/ERROR; callbacks simulated locally (external HCX/NHCX gateway OUT OF SCOPE)")
public class CoverageEligibilityController {

    private final NhcxService service;

    public CoverageEligibilityController(NhcxService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create a coverage-eligibility check",
            description = "patientId + payerScheme required (400 BEXA003 otherwise). Creates the check with "
                    + "status 0=INITIATED plus the companion t_nhcx_request row (kind=1) with a fresh correlation id. Emits nothing.")
    public ResponseEntity<EligibilityCheck> create(@RequestBody CreateCoverageEligibilityRequest body) {
        EligibilityCheck created = service.createCoverageEligibility(body.patientId(), body.payerScheme());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}/response")
    @Operation(summary = "Record the payor response for a check",
            description = "Pins status 2=ACKNOWLEDGED and stores response_payload. Guard: only from INITIATED (0); "
                    + "second response -> 400 RCM_ILLEGAL_TRANSITION. Unknown id -> 404.")
    public EligibilityCheck respond(@PathVariable long id,
                                    @RequestBody EligibilityResponseRequest body) {
        return service.applyEligibilityResponse(id, body.payload());
    }

    public record CreateCoverageEligibilityRequest(Long patientId, String payerScheme) {}

    public record EligibilityResponseRequest(Map<String, Object> payload) {}
}
