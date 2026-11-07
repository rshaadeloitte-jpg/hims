package com.hims.bloodbank.api;

import com.hims.bloodbank.application.RequisitionService;
import com.hims.bloodbank.domain.model.BloodCrossmatchResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Direct inquiry endpoint for individual cross-match results.
 */
@RestController
@RequestMapping("/api/v1/crossmatch-results")
@Tag(name = "Cross-match Results (WP-9)", description = "Inquiry endpoints for blood compatibility test records")
public class CrossmatchResultController {

    private final RequisitionService service;

    public CrossmatchResultController(RequisitionService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a cross-match result by id",
            description = "Returns the cross-match result record (including patient, bag, test outcome, tester, and timestamps). 404 if not found.")
    public BloodCrossmatchResult get(@PathVariable long id) {
        return service.getCrossmatchResult(id);
    }
}
