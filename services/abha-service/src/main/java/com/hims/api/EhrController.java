package com.hims.api;

import com.hims.application.EhrFileSyncService;
import com.hims.domain.model.EhrFile;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api/ehr")
@RequiredArgsConstructor
public class EhrController {
    private final EhrFileSyncService service;

    @GetMapping("/{id}")
    public ResponseEntity<EhrFile> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<EhrFile>> findAll() {
        return ResponseEntity.ok(StreamSupport.stream(service.findAll().spliterator(), false).toList());
    }

    @PostMapping
    public ResponseEntity<EhrFile> sync(@RequestBody EhrFile file) {
        return ResponseEntity.ok(service.syncFile(file));
    }
}