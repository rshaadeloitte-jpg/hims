package com.hims.kiosk.api;

import com.hims.kiosk.application.KioskService;
import com.hims.kiosk.domain.model.KioskQueue;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/kiosk/queue")
@Tag(name = "Kiosk Queue (M31)")
public class KioskQueueController {
    private final KioskService service;
    public KioskQueueController(KioskService service) { this.service = service; }

    @PostMapping("/token")
    @Operation(summary = "Issue kiosk queue token")
    public ResponseEntity<KioskQueue> issue(@RequestParam Long patientId,
                                            @RequestParam Long departmentId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.issueToken(patientId, departmentId));
    }

    @GetMapping
    @Operation(summary = "List queue by date/status")
    public List<KioskQueue> list(@RequestParam(required = false) LocalDate date,
                                  @RequestParam(required = false) Short status) {
        return service.listQueue(date, status);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Kiosk dashboard counts")
    public Map<String, Object> dashboard() {
        return service.kioskDashboard();
    }
}
