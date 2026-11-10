package com.hims.nursing.api;

import com.hims.nursing.application.NursingCareService;
import com.hims.nursing.domain.model.CarePlan;
import com.hims.nursing.domain.model.IncidentReport;
import com.hims.nursing.domain.model.NurseAllocation;
import com.hims.nursing.domain.model.ShiftHandover;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

/**
 * M09 gap controller: care plans, SBAR shift handover, nurse allocation,
 * incident reporting (nursing-service V2).
 */
@RestController
@Tag(name = "Nursing Care Plans & Incidents (M09)",
        description = "Care plans, SBAR shift handover, nurse-to-patient allocation, clinical incident reporting")
public class NursingCareController {

    private final NursingCareService service;

    public NursingCareController(NursingCareService service) {
        this.service = service;
    }

    // ------------------------------------------------------- care plans

    @PostMapping("/api/v1/care-plans")
    @Operation(summary = "Create a care plan for an admission (M09 gap)")
    public ResponseEntity<CarePlan> createCarePlan(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createCarePlan(
                lng(body, "admissionId"), str(body, "nursingDiagnosis"),
                str(body, "goal"), str(body, "interventions"), str(body, "outcomes")));
    }

    @GetMapping("/api/v1/care-plans")
    @Operation(summary = "List care plans for an admission")
    public List<CarePlan> listCarePlans(@RequestParam Long admissionId) {
        return service.listCarePlans(admissionId);
    }

    @PutMapping("/api/v1/care-plans/{id}/resolve")
    @Operation(summary = "Resolve an active care plan")
    public CarePlan resolveCarePlan(@PathVariable long id) {
        return service.resolveCarePlan(id);
    }

    // ------------------------------------------------------- shift handover

    @PostMapping("/api/v1/shift-handovers")
    @Operation(summary = "Create SBAR shift handover (M09 gap)")
    public ResponseEntity<ShiftHandover> createHandover(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createHandover(
                lng(body, "admissionId"), lng(body, "wardId"), str(body, "shift"),
                str(body, "situation"), str(body, "background"),
                str(body, "assessment"), str(body, "recommendation"),
                str(body, "pendingTasks"), lng(body, "handedOverBy")));
    }

    @PutMapping("/api/v1/shift-handovers/{id}/sign")
    @Operation(summary = "Sign (receive) a handover")
    public ShiftHandover signHandover(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return service.signHandover(id, lng(body, "receivedBy"));
    }

    @GetMapping("/api/v1/shift-handovers")
    @Operation(summary = "List handovers for an admission")
    public List<ShiftHandover> listHandovers(@RequestParam Long admissionId) {
        return service.listHandoversByAdmission(admissionId);
    }

    // ------------------------------------------------------- nurse allocation

    @PostMapping("/api/v1/nurse-allocations")
    @Operation(summary = "Allocate a nurse to a patient (M09 gap)")
    public ResponseEntity<NurseAllocation> allocate(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.allocate(
                lng(body, "wardId"), lng(body, "nurseId"),
                lng(body, "admissionId"), str(body, "shift")));
    }

    @PutMapping("/api/v1/nurse-allocations/{id}/release")
    @Operation(summary = "Release a nurse allocation")
    public NurseAllocation release(@PathVariable long id) {
        return service.release(id);
    }

    @GetMapping("/api/v1/nurse-allocations")
    @Operation(summary = "Active allocations for a ward")
    public List<NurseAllocation> activeAllocations(@RequestParam Long wardId) {
        return service.activeAllocationsForWard(wardId);
    }

    // ------------------------------------------------------- incident reporting

    @PostMapping("/api/v1/incidents")
    @Operation(summary = "Report a clinical incident (M09 gap)")
    public ResponseEntity<IncidentReport> reportIncident(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.reportIncident(
                lng(body, "patientId"), lng(body, "admissionId"),
                str(body, "incidentType"), str(body, "severity"),
                str(body, "description"), str(body, "location"),
                body.get("occurredAt") != null ? Timestamp.valueOf(str(body, "occurredAt")) : null,
                lng(body, "reportedBy")));
    }

    @PutMapping("/api/v1/incidents/{id}/review")
    @Operation(summary = "Review an incident")
    public IncidentReport reviewIncident(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return service.reviewIncident(id, lng(body, "reviewerId"), str(body, "correctiveAction"));
    }

    @GetMapping("/api/v1/incidents")
    @Operation(summary = "List incidents for a patient")
    public List<IncidentReport> incidentsByPatient(@RequestParam Long patientId) {
        return service.incidentsByPatient(patientId);
    }

    // ------------------------------------------------------- helpers

    private static Long lng(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? null : Long.valueOf(String.valueOf(v));
    }

    private static String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? null : String.valueOf(v);
    }
}
