package com.hims.govt.api;

import com.hims.govt.application.EjsService;
import com.hims.govt.domain.model.EjsAsha;
import com.hims.govt.domain.model.EjsIndicatorEntry;
import com.hims.govt.domain.model.EjsIncentive;
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
 * EJS API (GAP-REGISTER-6 WP-T4, Catalog #28): ASHA registration, monthly
 * indicator upsert, incentive compute (recompute replaces COMPUTED only),
 * approve/pay machine and the filtered incentive list.
 * Emits NOTHING to the outbox — IncentiveComputed is DEFERRED per
 * KAFKA-EVENT-CATALOG. Full module-125 endpoint parity OUT OF SCOPE.
 */
@RestController
@RequestMapping("/api/v1/ejs")
@Tag(name = "EJS (WP-T4)", description = "EJS incentives (Catalog #28): indicator upsert on UNIQUE(ashaId,indicatorCode,periodYear,periodMonth); compute = SUM(value x rate-table); recompute REPLACES prior COMPUTED rows only (APPROVED/PAID immutable); machine 1=COMPUTED -> approve -> 2=APPROVED -> pay -> 3=PAID. IncentiveComputed DEFERRED - no events.")
public class EjsController {

    private final EjsService service;

    public EjsController(EjsService service) {
        this.service = service;
    }

    @PostMapping("/asha")
    @Operation(summary = "Register an ASHA worker",
            description = "{code, name, supervisorId?}; supervisorId optional but must exist when supplied "
                    + "(400 GOVT_UNKNOWN_SUPERVISOR); dup code -> 409 GOVT_DUPLICATE_ASHA_CODE. Emits nothing.")
    public ResponseEntity<EjsAsha> createAsha(@RequestBody CreateAshaRequest body) {
        EjsAsha created = service.createAsha(body.code(), body.name(), body.supervisorId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/indicators")
    @Operation(summary = "Upsert a monthly indicator entry",
            description = "{ashaId, indicatorCode, periodYear, periodMonth, value}; upsert on "
                    + "UNIQUE(ashaId, indicatorCode, periodYear, periodMonth); periodMonth 1..12; value >= 0. Emits nothing.")
    public EjsIndicatorEntry upsertIndicator(@RequestBody UpsertIndicatorRequest body) {
        return service.upsertIndicator(body.ashaId(), body.indicatorCode(),
                body.periodYear(), body.periodMonth(), body.value());
    }

    @PostMapping("/incentives/compute")
    @Operation(summary = "Compute an incentive for one ASHA + period",
            description = "{ashaId, periodYear, periodMonth} -> SUM(entry.value x rate-table) as a fresh COMPUTED "
                    + "incentive; recompute REPLACES prior COMPUTED rows only (APPROVED/PAID untouched). Emits nothing.")
    public ResponseEntity<EjsIncentive> compute(@RequestBody ComputeRequest body) {
        EjsIncentive computed = service.compute(body.ashaId(), body.periodYear(), body.periodMonth());
        return ResponseEntity.status(HttpStatus.CREATED).body(computed);
    }

    @PutMapping("/incentives/{id}/approve")
    @Operation(summary = "Approve a COMPUTED incentive (1 -> 2)",
            description = "Guard: COMPUTED only; anything else 400 GOVT_ILLEGAL_TRANSITION. Emits nothing.")
    public EjsIncentive approve(@PathVariable long id) {
        return service.approve(id);
    }

    @PutMapping("/incentives/{id}/pay")
    @Operation(summary = "Pay an APPROVED incentive (2 -> 3)",
            description = "Guard: APPROVED only; anything else 400 GOVT_ILLEGAL_TRANSITION. Emits nothing.")
    public EjsIncentive pay(@PathVariable long id) {
        return service.pay(id);
    }

    @GetMapping("/incentives")
    @Operation(summary = "List incentives", description = "?status= (1..3 or name) and/or ?ashaId= filter.")
    public List<EjsIncentive> listIncentives(@RequestParam(required = false) String status,
                                             @RequestParam(required = false) Long ashaId) {
        return service.listIncentives(status == null ? null : EjsService.parseStatus(status), ashaId);
    }

    public record CreateAshaRequest(String code, String name, Long supervisorId) {}

    public record UpsertIndicatorRequest(Long ashaId, String indicatorCode,
                                         Integer periodYear, Integer periodMonth, Integer value) {}

    public record ComputeRequest(Long ashaId, Integer periodYear, Integer periodMonth) {}
}
