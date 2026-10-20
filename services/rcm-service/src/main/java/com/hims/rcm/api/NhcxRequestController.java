package com.hims.rcm.api;

import com.hims.rcm.application.NhcxService;
import com.hims.rcm.domain.model.NhcxRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * NHCX request intake + callback API (register WP-R4): pre-auth / claim-submit
 * creation, idempotent correlation-id callback (unknown -> 404 envelope), filtered list.
 * ClaimSubmitted is DEFERRED per KAFKA-EVENT-CATALOG §3 — claim creation emits NOTHING.
 */
@RestController
@Tag(name = "NHCX Requests (WP-R4)", description = "RCM & Claims (Catalog #23): persisted NHCX requests kind 1=COVERAGE_ELIGIBILITY 2=PRE_AUTH 3=CLAIM_SUBMIT, status 0=INITIATED 1=SENT 2=ACKNOWLEDGED 3=COMPLETED/ERROR; callback idempotent by correlation_id")
public class NhcxRequestController {

    private final NhcxService service;

    public NhcxRequestController(NhcxService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/pre-auths")
    @Operation(summary = "Create a pre-auth request",
            description = "patientId + payload required (admissionId optional). Persists kind=2 PRE_AUTH with status 0 and a fresh correlation id. Emits nothing.")
    public NhcxRequest createPreAuth(@RequestBody CreatePreAuthRequest body) {
        return service.createPreAuth(body.patientId(), body.admissionId(), body.payload());
    }

    @PostMapping("/api/v1/claims")
    @Operation(summary = "Submit a claim record",
            description = "patientId + billId + payload required. Persists kind=3 CLAIM_SUBMIT with status 0 and a fresh "
                    + "correlation id. ClaimSubmitted event DEFERRED per KAFKA-EVENT-CATALOG §3 — no outbox write.")
    public NhcxRequest createClaim(@RequestBody CreateClaimRequest body) {
        return service.createClaim(body.patientId(), body.billId(), body.payload());
    }

    @PutMapping("/api/v1/nhcx/{correlationId}/callback")
    @Operation(summary = "Simulated NHCX/gateway callback (idempotent by correlation id)",
            description = "Unknown correlation id -> 404 envelope. status must be 0..3 (400 BEXA003 otherwise). "
                    + "Same-status repeat is an idempotent no-op success; backward transition -> 400 RCM_ILLEGAL_TRANSITION.")
    public NhcxRequest callback(@PathVariable String correlationId,
                                @RequestBody CallbackRequest body) {
        return service.callback(correlationId, body.payload(), body.status());
    }

    @GetMapping("/api/v1/nhcx")
    @Operation(summary = "List NHCX requests", description = "?kind= (1..3 or name) and/or ?status= (0..3 or name) filter.")
    public List<NhcxRequest> list(@RequestParam(required = false) String kind,
                                  @RequestParam(required = false) String status) {
        return service.list(kind == null ? null : NhcxService.parseKind(kind),
                status == null ? null : NhcxService.parseStatus(status));
    }

    public record CreatePreAuthRequest(Long patientId, Long admissionId, Map<String, Object> payload) {}

    public record CreateClaimRequest(Long patientId, Long billId, Map<String, Object> payload) {}

    public record CallbackRequest(Map<String, Object> payload, Short status) {}
}
