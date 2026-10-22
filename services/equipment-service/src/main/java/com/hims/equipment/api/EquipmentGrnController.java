package com.hims.equipment.api;

import com.hims.equipment.application.EquipmentGrnService;
import com.hims.equipment.domain.model.EquipmentGrn;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/equipment/grn")
public class EquipmentGrnController {

    private final EquipmentGrnService service;

    public EquipmentGrnController(EquipmentGrnService service) {
        this.service = service;
    }

    @GetMapping
    public List<EquipmentGrn> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public EquipmentGrn get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<EquipmentGrn> create(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(body));
    }

    @PutMapping("/{id}/status")
    public EquipmentGrn updateStatus(@PathVariable Long id, @RequestParam String status) {
        return service.updateStatus(id, status);
    }
}