package com.hims.nursing.infrastructure.web;

import com.hims.nursing.application.ShiftHandoverService;
import com.hims.nursing.domain.model.ShiftHandover;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/nursing/shift-handovers")
public class ShiftHandoverController {

    private final ShiftHandoverService service;

    public ShiftHandoverController(ShiftHandoverService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ShiftHandover> create(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(
                longOf(body, "admissionId"), longOf(body, "wardId"), strOf(body, "shift"),
                strOf(body, "situation"), strOf(body, "background"), strOf(body, "assessment"),
                strOf(body, "recommendation"), strOf(body, "pendingTasks"), longOf(body, "handedOverBy")));
    }

    @PutMapping("/{id}/sign-off")
    public ResponseEntity<ShiftHandover> signOff(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(service.completeSignOff(id, longOf(body, "receivedBy")));
    }

    @GetMapping
    public List<ShiftHandover> getByAdmission(@RequestParam Long admissionId) {
        return service.getByAdmission(admissionId);
    }

    private static Long longOf(Map<String, Object> body, String key) {
        Object v = body.get(key); return v == null ? null : ((Number) v).longValue();
    }
    private static String strOf(Map<String, Object> body, String key) {
        Object v = body.get(key); return v == null ? null : v.toString();
    }
}
