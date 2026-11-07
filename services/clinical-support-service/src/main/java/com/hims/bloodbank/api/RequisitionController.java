package com.hims.bloodbank.api;

import static com.hims.bloodbank.api.DonorController.*;

import com.hims.bloodbank.application.RequisitionService;
import com.hims.bloodbank.domain.model.BloodBag;
import com.hims.bloodbank.domain.model.BloodCrossmatchResult;
import com.hims.bloodbank.domain.model.BloodRequisition;
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

import java.security.Principal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Requisition API (GAP-REGISTER WP-9): create/list/detail plus crossmatch, issue and
 * cancel transitions implementing the BR-BLD-001 status machine (1/2/3/10) with
 * patient-safety ABO/Rh cross-match gating.
 *
 * NOTE: creating a requisition does NOT emit BloodRequisitionCreated — DEFERRED per
 * MICROSERVICE-CATALOG #13 / KAFKA-EVENT-CATALOG §3.
 */
@RestController
@RequestMapping("/api/v1/requisitions")
@Tag(name = "Requisitions (WP-9)", description = "BR-BLD-001 status machine: 1=new, 2=cross-matched (cumulative >= ordered), 3=issued (cumulative issued >= ordered), 10=cancelled/expired")
public class RequisitionController {

    private final RequisitionService service;

    public RequisitionController(RequisitionService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create a blood requisition",
            description = "patientId required; optional components[] {component, volumeMl}; NO event emitted (BloodRequisitionCreated deferred).")
    public ResponseEntity<BloodRequisition> create(@RequestBody Map<String, Object> body) {
        Timestamp requested = body.get("requesteddate") == null ? null
                : Timestamp.from(Instant.parse(str(body, "requesteddate")));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> components = (List<Map<String, Object>>) body.get("components");
        BloodRequisition r = service.create(longOf(body, "patientId"), str(body, "bloodGroup"),
                intOf(body, "orderedVolumeMl"), requested,
                intOf(body, "orderValidationHours"), components);
        return ResponseEntity.status(HttpStatus.CREATED).body(r);
    }

    @GetMapping
    @Operation(summary = "List requisitions", description = "?status= or ?patientId= filters.")
    public List<BloodRequisition> list(@RequestParam(required = false) Integer status,
                                       @RequestParam(required = false) Long patientId) {
        return service.list(status, patientId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Full requisition view (header + details + linked bags + crossmatch results + cumulative volumes)",
            description = "Read-only — BR-BLD-004 expiry runs in POST /api/v1/jobs/expire-requisitions, never on read.")
    public Map<String, Object> get(@PathVariable long id) {
        return service.detail(id);
    }

    @PostMapping("/{id}/crossmatch")
    @Operation(summary = "Initiate cross-match for a bag onto this requisition",
            description = "Records a PENDING crossmatch result and reserves the bag. 409 if bag already crossmatched/issued/expired/reserved.")
    public ResponseEntity<BloodCrossmatchResult> crossmatch(@PathVariable long id, @RequestBody Map<String, Object> body) {
        String method = str(body, "method");
        String remarks = str(body, "remarks");
        BloodCrossmatchResult result = service.crossmatch(id, longOf(body, "bagId"), method, remarks);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/{id}/crossmatch/{resultId}/result")
    @Operation(summary = "Record compatibility test result for a crossmatched bag",
            description = "COMPATIBLE requires ABO/Rh gate to pass (409 otherwise); INCOMPATIBLE releases the bag reservation. Tester is extracted from authenticated principal.")
    public ResponseEntity<BloodCrossmatchResult> recordResult(
            @PathVariable long id,
            @PathVariable long resultId,
            @RequestBody Map<String, Object> body,
            Principal principal) {
        // Tester is extracted strictly from the authenticated principal, never from the request body
        String tester = principal != null && principal.getName() != null && !principal.getName().isBlank()
                ? principal.getName()
                : "system";
        String result = str(body, "result");
        String remarks = str(body, "remarks");
        String method = str(body, "method");
        BloodCrossmatchResult record = service.recordCrossmatchResult(id, resultId, result, tester, remarks, method);
        return ResponseEntity.ok(record);
    }

    @GetMapping("/{id}/crossmatch-results")
    @Operation(summary = "List crossmatch results for a requisition")
    public List<BloodCrossmatchResult> getCrossmatchResults(@PathVariable long id) {
        return service.getCrossmatchResults(id);
    }

    @PostMapping("/{id}/issue")
    @Operation(summary = "Issue a bag for this requisition",
            description = "Requires an active, unexpired COMPATIBLE result for that bag, patient, and requisition (400 otherwise); cumulative issued volume >= ordered -> status 3.")
    public BloodBag issue(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return service.issue(id, longOf(body, "bagId"));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel the requisition", description = "Status 1|2 -> 10; releases unissued crossmatched bags and marks crossmatch results decrossed.")
    public BloodRequisition cancel(@PathVariable long id) {
        return service.cancel(id);
    }
}
