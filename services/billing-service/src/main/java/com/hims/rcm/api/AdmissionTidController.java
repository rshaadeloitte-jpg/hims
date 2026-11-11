package com.hims.rcm.api;

import com.hims.rcm.application.TidService;
import com.hims.rcm.domain.model.AdmissionTid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admission TID registry API (register WP-R4): GET returns the registered TID
 * (404 envelope when absent); PUT assigns it exactly once (duplicate -> 409).
 */
@RestController
@RequestMapping("/api/v1/admissions/{admissionId}/tid")
@Tag(name = "Admission TID (WP-R4)", description = "RCM & Claims (Catalog #23): admission -> NHCX transaction id registry; unique per admission (409 TID_EXISTS on reassignment)")
public class AdmissionTidController {

    private final TidService service;

    public AdmissionTidController(TidService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Get the NHCX transaction id registered for an admission",
            description = "404 envelope when the admission carries no TID yet.")
    public AdmissionTid get(@PathVariable long admissionId) {
        return service.get(admissionId);
    }

    @PutMapping
    @Operation(summary = "Register the NHCX transaction id for an admission",
            description = "tid required, max 60 chars (400 BEXA003 otherwise). Duplicate assignment -> 409 TID_EXISTS.")
    public AdmissionTid assign(@PathVariable long admissionId,
                               @RequestBody AssignTidRequest body) {
        return service.assign(admissionId, body.tid());
    }

    public record AssignTidRequest(String tid) {}
}
