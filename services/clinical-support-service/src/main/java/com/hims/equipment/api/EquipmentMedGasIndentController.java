package com.hims.equipment.api;

import com.hims.equipment.application.EquipmentMedGasIndentService;
import com.hims.equipment.domain.model.EquipmentMedGasIndent;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/equipment/med-gas-indent")
public class EquipmentMedGasIndentController {
    private final EquipmentMedGasIndentService service;
    public EquipmentMedGasIndentController(EquipmentMedGasIndentService service) { this.service = service; }

    @GetMapping public List<EquipmentMedGasIndent> list() { return service.list(); }
    @GetMapping("/{id}") public EquipmentMedGasIndent get(@PathVariable Long id) { return service.get(id); }
    @PostMapping public ResponseEntity<EquipmentMedGasIndent> create(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(body));
    }
    @PutMapping("/{id}/status") public EquipmentMedGasIndent updateStatus(@PathVariable Long id, @RequestParam String status) {
        return service.updateStatus(id, status);
    }
}