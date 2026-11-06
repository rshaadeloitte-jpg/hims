package com.hims.mortuary.api;

import com.hims.mortuary.application.MortuaryService;
import com.hims.mortuary.domain.model.AutopsyRequest;
import com.hims.mortuary.domain.model.DeadBody;
import com.hims.mortuary.domain.model.ReleaseCertificate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Mortuary &amp; Death API (M28): dead body registry, autopsy requests, release certificates.
 * Integrates with adt-service for death linkage (admission_id references t_admission).
 */
@RestController
@RequestMapping("/api/v1/mortuary")
@Tag(name = "Mortuary (M28)", description = "Dead body registry, autopsy requests, release certificates. admission_id links to adt-service t_admission.")
public class MortuaryController {

    private final MortuaryService service;

    public MortuaryController(MortuaryService service) {
        this.service = service;
    }

    // --------------------------------------------------------------- dead body CRUD

    @PostMapping("/cases")
    @Operation(summary = "Register a dead body",
            description = "admissionId (adt-service link), patientId, patientName, deathDate required. admission_id links to adt-service t_admission.")
    public ResponseEntity<DeadBody> createDeadBody(@RequestBody Map<String, Object> body) {
        Timestamp deathDate = body.get("deathDate") == null ? null
                : Timestamp.from(Instant.parse(str(body, "deathDate")));
        DeadBody deadBody = service.createDeadBody(
                longOf(body, "admissionId"),
                longOf(body, "patientId"),
                str(body, "patientName"),
                deathDate,
                str(body, "causeOfDeath"),
                str(body, "deathLocation"),
                str(body, "nextOfKin"),
                str(body, "nextOfKinRelation"),
                str(body, "nextOfKinPhone"),
                str(body, "chamberNo"),
                boolOf(body, "policeCase"),
                str(body, "mlcNo"),
                str(body, "remark"),
                longOf(body, "unitId"));
        return ResponseEntity.status(HttpStatus.CREATED).body(deadBody);
    }

    @GetMapping("/cases")
    @Operation(summary = "List dead bodies", description = "?bodyStatus= or ?patientId= filters.")
    public List<DeadBody> listDeadBodies(@RequestParam(required = false) String bodyStatus,
                                         @RequestParam(required = false) Long patientId) {
        return service.listDeadBodies(bodyStatus, patientId);
    }

    @GetMapping("/cases/{id}")
    public DeadBody getDeadBody(@PathVariable long id) {
        return service.getDeadBody(id);
    }

    @PutMapping("/cases/{id}")
    @Operation(summary = "Update dead body details",
            description = "Cannot update released bodies. admission_id links to adt-service t_admission.")
    public DeadBody updateDeadBody(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return service.updateDeadBody(
                id,
                str(body, "causeOfDeath"),
                str(body, "deathLocation"),
                str(body, "nextOfKin"),
                str(body, "nextOfKinRelation"),
                str(body, "nextOfKinPhone"),
                str(body, "chamberNo"),
                str(body, "deathCertNo"),
                str(body, "remark"));
    }

    // --------------------------------------------------------------- release

    @PostMapping("/cases/{id}/release")
    @Operation(summary = "Release body to next of kin",
            description = "handoverTo required. Creates release certificate and marks body as RELEASED.")
    public ReleaseCertificate releaseBody(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return service.releaseBody(
                id,
                str(body, "handoverTo"),
                str(body, "handoverRelation"),
                str(body, "handoverPhone"),
                str(body, "remark"));
    }

    // --------------------------------------------------------------- autopsy

    @PostMapping("/cases/{id}/autopsy-request")
    @Operation(summary = "Request autopsy for a dead body",
            description = "requestedBy required. Sets body status to AUTOPSY_PENDING.")
    public AutopsyRequest requestAutopsy(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return service.requestAutopsy(id, longOf(body, "requestedBy"), str(body, "reason"));
    }

    @PostMapping("/autopsy/{id}/approve")
    @Operation(summary = "Approve autopsy request", description = "Sets request status to APPROVED.")
    public AutopsyRequest approveAutopsy(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return service.approveAutopsy(id, longOf(body, "doctorId"));
    }

    @PostMapping("/autopsy/{id}/complete")
    @Operation(summary = "Complete autopsy", description = "Sets request status to COMPLETED, body to AUTOPSY_COMPLETE.")
    public AutopsyRequest completeAutopsy(@PathVariable long id, @RequestBody Map<String, Object> body) {
        Timestamp autopsyDate = body.get("autopsyDate") == null ? null
                : Timestamp.from(Instant.parse(str(body, "autopsyDate")));
        return service.completeAutopsy(id, autopsyDate, str(body, "findings"));
    }

    @PostMapping("/autopsy/{id}/reject")
    @Operation(summary = "Reject autopsy request", description = "Sets request status to REJECTED, body reverts to ADMITTED.")
    public AutopsyRequest rejectAutopsy(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return service.rejectAutopsy(id, str(body, "rejectionReason"));
    }

    // --------------------------------------------------------------- report

    @GetMapping("/report")
    @Operation(summary = "Mortuary report", description = "?bodyStatus= filters. Returns counts by status.")
    public Map<String, Object> getReport(@RequestParam(required = false) String bodyStatus) {
        return service.getReport(bodyStatus);
    }

    // --------------------------------------------------------------- helpers

    static Long longOf(Map<String, Object> body, String key) {
        if (body == null) return null;
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.valueOf(String.valueOf(v));
        } catch (NumberFormatException e) {
            throw new com.hims.mortuary.application.exceptions.InvalidMortuaryRequestException(
                    "MOR_REQ_INVALID", key + " must be numeric");
        }
    }

    static Boolean boolOf(Map<String, Object> body, String key) {
        if (body == null) return null;
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Boolean b) return b;
        return Boolean.valueOf(String.valueOf(v));
    }

    static String str(Map<String, Object> body, String key) {
        if (body == null) return null;
        Object v = body.get(key);
        return v == null ? null : String.valueOf(v);
    }
}
