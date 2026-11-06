package com.hims.bloodbank.api;

import static com.hims.bloodbank.api.DonorController.*;

import com.hims.bloodbank.application.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * One-shot job endpoints for the legacy daily crons. IDEMPOTENT — safe to re-run.
 * CRON WIRING DEFERRED: legacy schedules these internally (ApplicationJobs daily);
 * here an orchestrator/cron invokes the endpoints. No @Scheduled wiring in Wave 3.
 */
@RestController
@RequestMapping("/api/v1/jobs")
@Tag(name = "Jobs (WP-9)", description = "BR-BLD-002 decross-expired / BR-BLD-003 bag expiry sweep / BR-BLD-004 expire-requisitions / BR-BLD-005 cancel-stale-donors — all idempotent one-shot scans")
public class JobController {

    private final JobService service;

    public JobController(JobService service) {
        this.service = service;
    }

    @PostMapping("/decross-expired")
    @Operation(summary = "Run BR-BLD-002 + BR-BLD-003 sweeps",
            description = "Decrosses crossmatched unissued bags with crossMatchedTestDate >= 48h; flags expirydate=today bags expired unless issued (decrossing those crossmatched); recalculates affected requisition statuses toward 1.")
    public Map<String, Object> decrossExpired() {
        return service.decrossExpired();
    }

    @PostMapping("/expire-requisitions")
    @Operation(summary = "Run BR-BLD-004 scan",
            description = "requesteddate + validation period <= now AND NOT sample_collected AND status=1 -> status=10, order_expired=true, reason 'Order is expired'. ?validationHours= overrides the per-requisition snapshot.")
    public Map<String, Object> expireRequisitions(
            @RequestParam(required = false) Integer validationHours,
            @RequestBody(required = false) Map<String, Object> body) {
        Integer hours = validationHours != null ? validationHours : intOf(body, "validationHours");
        return service.expireRequisitions(hours);
    }

    @PostMapping("/cancel-stale-donors")
    @Operation(summary = "Run BR-BLD-005 scan",
            description = "DNR_NEW/DNR_FIT donors with no touch within 72h (inclusive) -> donationStatus=7 DNR_CANCELLATION.")
    public Map<String, Object> cancelStaleDonors() {
        return service.cancelStaleDonors();
    }
}
