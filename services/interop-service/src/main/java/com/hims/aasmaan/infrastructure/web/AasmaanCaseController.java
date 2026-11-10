package com.hims.aasmaan.infrastructure.web;

import com.hims.aasmaan.application.AasmaanCaseService;
import com.hims.aasmaan.domain.model.AasmaanCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/cases")
public class AasmaanCaseController {
    private final AasmaanCaseService service;
    public AasmaanCaseController(AasmaanCaseService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<AasmaanCase> create(@RequestBody AasmaanCase c) { return ResponseEntity.ok(service.create(c)); }
    @GetMapping
    public ResponseEntity<List<AasmaanCase>> list() { return ResponseEntity.ok(service.findAll()); }
    @GetMapping("/{id}")
    public ResponseEntity<AasmaanCase> get(@PathVariable Long id) { return ResponseEntity.ok(service.findById(id)); }
    @PutMapping("/{id}")
    public ResponseEntity<AasmaanCase> update(@PathVariable Long id, @RequestBody AasmaanCase c) { return ResponseEntity.ok(service.update(id, c)); }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }
}
