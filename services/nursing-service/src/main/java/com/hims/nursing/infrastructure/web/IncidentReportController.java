package com.hims.nursing.infrastructure.web;

import com.hims.nursing.application.IncidentReportService;
import com.hims.nursing.domain.model.IncidentReport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/nursing/incidents")
public class IncidentReportController {

    private final IncidentReportService service;

    public IncidentReportController(IncidentReportService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<IncidentReport> report(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.report(
                longOf(body, "patientId"), strOf(body, "incidentType"), strOf(body, "severity"),
                strOf(body, "description"), strOf(body, "location"),
                strOf(body, "occurredAt") != null ? Timestamp.valueOf(strOf(body, "occurredAt")) : null,
                longOf(body, "reportedBy")));
    }

    @PutMapping("/{id}/review")
    public ResponseEntity<IncidentReport> review(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(service.reviewStatus(id, shortOf(body, "reviewStatus"), longOf(body, "reviewerId"), strOf(body, "correctiveAction")));
    }

    @GetMapping
    public List<IncidentReport> getByDateRange(@RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        return service.getByDateRange(from != null ? Timestamp.valueOf(from) : null, to != null ? Timestamp.valueOf(to) : null);
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
