package com.hims.bloodbank.api;

import com.hims.bloodbank.application.DonorService;
import com.hims.bloodbank.domain.model.BloodDonor;
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

import java.util.List;
import java.util.Map;

/** Donor registry (GAP-REGISTER WP-9): POST/GET /api/v1/donors, POST /{id}/touch (BR-BLD-005 semantics). */
@RestController
@RequestMapping("/api/v1/donors")
@Tag(name = "Donors (WP-9)", description = "Donor registry (t_blooddonor); touch refreshes last_action_at for the BR-BLD-005 72h staleness gate")
public class DonorController {

    private final DonorService service;

    public DonorController(DonorService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Register a donor", description = "donationStatus defaults to DNR_NEW(1).")
    public ResponseEntity<BloodDonor> create(@RequestBody Map<String, Object> body) {
        BloodDonor donor = service.create(str(body, "name"), str(body, "bloodGroup"),
                intOf(body, "donationStatus"), longOf(body, "unitId"));
        return ResponseEntity.status(HttpStatus.CREATED).body(donor);
    }

    @GetMapping
    @Operation(summary = "List donors", description = "?donationStatus= filters (1=new,2=fit,...,7=cancelled).")
    public List<BloodDonor> list(@RequestParam(required = false) Integer donationStatus) {
        return service.list(donationStatus);
    }

    @GetMapping("/{id}")
    public BloodDonor get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/touch")
    @Operation(summary = "Record a donor workflow action",
            description = "Refreshes last_action_at — keeps the donor out of the BR-BLD-005 72h auto-cancellation scan.")
    public BloodDonor touch(@PathVariable long id) {
        return service.touch(id);
    }

    static Long longOf(Map<String, Object> body, String key) {
        if (body == null) return null;
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.valueOf(String.valueOf(v));
        } catch (NumberFormatException e) {
            throw new com.hims.bloodbank.application.exceptions.InvalidBloodBankRequestException(
                    "BLD_REQ_INVALID", key + " must be numeric");
        }
    }

    static Integer intOf(Map<String, Object> body, String key) {
        if (body == null) return null;
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        try {
            return Integer.valueOf(String.valueOf(v));
        } catch (NumberFormatException e) {
            throw new com.hims.bloodbank.application.exceptions.InvalidBloodBankRequestException(
                    "BLD_REQ_INVALID", key + " must be an integer");
        }
    }

    static String str(Map<String, Object> body, String key) {
        if (body == null) return null;
        Object v = body.get(key);
        return v == null ? null : String.valueOf(v);
    }
}
