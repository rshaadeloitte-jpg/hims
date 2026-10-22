package com.hims.equipment.api;

import com.hims.equipment.application.EquipmentScrapService;
import com.hims.equipment.domain.model.EquipmentScrap;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/equipment/scrap")
public class EquipmentScrapController {
    private final EquipmentScrapService service;
    public EquipmentScrapController(EquipmentScrapService service) { this.service = service; }

    @GetMapping public List<EquipmentScrap> list() { return service.list(); }
    @GetMapping("/{id}") public EquipmentScrap get(@PathVariable Long id) { return service.get(id); }
    @PostMapping public ResponseEntity<EquipmentScrap> create(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(body));
    }
    @PutMapping("/{id}/status") public EquipmentScrap updateStatus(@PathVariable Long id, @RequestParam String status) {
        return service.updateStatus(id, status);
    }
}