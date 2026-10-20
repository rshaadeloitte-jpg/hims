package com.hims.api;

import com.hims.application.EAushadhiIntegrationService;
import com.hims.domain.model.EAushadhiDrug;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api/eaushadhi")
@RequiredArgsConstructor
public class EAushadhiController {
    private final EAushadhiIntegrationService service;

    @GetMapping("/{id}")
    public ResponseEntity<EAushadhiDrug> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<EAushadhiDrug>> findAll() {
        return ResponseEntity.ok(StreamSupport.stream(service.findAll().spliterator(), false).toList());
    }

    @PostMapping
    public ResponseEntity<EAushadhiDrug> addDrug(@RequestBody EAushadhiDrug drug) {
        return ResponseEntity.ok(service.addDrug(drug));
    }
}