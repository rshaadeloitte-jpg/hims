package com.hims.aasmaan.infrastructure.web;

import com.hims.aasmaan.application.AasmaanRscCallLogService;
import com.hims.aasmaan.domain.model.AasmaanRscCallLog;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/rsc-logs")
public class AasmaanRscController {
    private final AasmaanRscCallLogService service;
    public AasmaanRscController(AasmaanRscCallLogService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<AasmaanRscCallLog> log(@RequestBody AasmaanRscCallLog r) { return ResponseEntity.ok(service.log(r)); }
    @GetMapping
    public ResponseEntity<List<AasmaanRscCallLog>> list() { return ResponseEntity.ok(service.all()); }
}
