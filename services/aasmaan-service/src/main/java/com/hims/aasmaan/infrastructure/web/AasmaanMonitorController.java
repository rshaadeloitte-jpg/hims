package com.hims.aasmaan.infrastructure.web;

import com.hims.aasmaan.application.AasmaanMonitorService;
import com.hims.aasmaan.domain.model.AasmaanMonitor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/monitor")
public class AasmaanMonitorController {
    private final AasmaanMonitorService service;
    public AasmaanMonitorController(AasmaanMonitorService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<AasmaanMonitor> record(@RequestBody AasmaanMonitor m) { return ResponseEntity.ok(service.record(m)); }
    @GetMapping
    public ResponseEntity<List<AasmaanMonitor>> list() { return ResponseEntity.ok(service.all()); }
}
