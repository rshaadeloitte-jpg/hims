package com.hims.ot.api;

import com.hims.ot.application.SurgicalChecklistService;
import com.hims.ot.domain.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ot/checklists")
public class SurgicalChecklistController {

    private final SurgicalChecklistService service;

    public SurgicalChecklistController(SurgicalChecklistService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<SurgicalChecklist> create(@RequestParam Long scheduleId, @RequestParam Long patientId, @RequestParam String phase) {
        return ResponseEntity.ok(service.create(scheduleId, patientId, phase));
    }

    @PostMapping("/{checklistId}/items")
    public ResponseEntity<ChecklistItem> addItem(@PathVariable Long checklistId, @RequestParam String itemName, @RequestParam String category) {
        return ResponseEntity.ok(service.addItem(checklistId, itemName, category));
    }

    @PostMapping("/items/{itemId}/complete")
    public ResponseEntity<ChecklistItem> completeItem(@PathVariable Long itemId) {
        return ResponseEntity.ok(service.completeItem(itemId));
    }

    @PostMapping("/{checklistId}/completion")
    public ResponseEntity<ChecklistCompletion> recordCompletion(@PathVariable Long checklistId, @RequestParam String overallStatus) {
        return ResponseEntity.ok(service.recordCompletion(checklistId, overallStatus));
    }

    @GetMapping
    public ResponseEntity<List<SurgicalChecklist>> findBySchedule(@RequestParam Long scheduleId) {
        return ResponseEntity.ok(service.findBySchedule(scheduleId));
    }
}
