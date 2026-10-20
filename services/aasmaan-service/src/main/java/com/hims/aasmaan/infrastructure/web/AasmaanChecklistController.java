package com.hims.aasmaan.infrastructure.web;

import com.hims.aasmaan.application.AasmaanChecklistService;
import com.hims.aasmaan.domain.model.AasmaanChecklist;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/checklists")
public class AasmaanChecklistController {
    private final AasmaanChecklistService service;
    public AasmaanChecklistController(AasmaanChecklistService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<AasmaanChecklist> create(@RequestBody AasmaanChecklist c) { return ResponseEntity.ok(service.save(c)); }
    @GetMapping
    public ResponseEntity<List<AasmaanChecklist>> list() { return ResponseEntity.ok(service.byCase(null)); }
    @GetMapping("/case/{caseId}")
    public ResponseEntity<List<AasmaanChecklist>> byCase(@PathVariable Long caseId) { return ResponseEntity.ok(service.byCase(caseId)); }
    @GetMapping("/{id}")
    public ResponseEntity<AasmaanChecklist> get(@PathVariable Long id) { return ResponseEntity.ok(service.byId(id)); }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }
}
