package com.hims.nursing.infrastructure.web;

import com.hims.nursing.application.NursingCarePlanService;
import com.hims.nursing.domain.model.CarePlan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/nursing/care-plans")
public class NursingCarePlanController {

    private final NursingCarePlanService service;

    public NursingCarePlanController(NursingCarePlanService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CarePlan> create(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(
                longOf(body, "admissionId"), strOf(body, "nursingDiagnosis"), strOf(body, "goal"), longOf(body, "createdBy")));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<CarePlan> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(service.updateStatus(id, shortOf(body, "statusCode")));
    }

    @GetMapping
    public List<CarePlan> getByAdmission(@RequestParam Long admissionId) {
        return service.getByAdmission(admissionId);
    }

    @PostMapping("/{id}/interventions")
    public ResponseEntity<CarePlan> addIntervention(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(service.addIntervention(id, strOf(body, "intervention")));
    }

    @PostMapping("/{id}/outcomes")
    public ResponseEntity<CarePlan> addOutcome(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(service.addOutcome(id, strOf(body, "outcome")));
    }

    private static Long longOf(Map<String, Object> body, String key) {
        Object v = body.get(key); return v == null ? null : ((Number) v).longValue();
    }
    private static Short shortOf(Map<String, Object> body, String key) {
        Object v = body.get(key); return v == null ? null : ((Number) v).shortValue();
    }
    private static String strOf(Map<String, Object> body, String key) {
        Object v = body.get(key); return v == null ? null : v.toString();
    }
}
