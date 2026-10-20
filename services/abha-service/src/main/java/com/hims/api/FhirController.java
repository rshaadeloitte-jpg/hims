package com.hims.api;

import com.hims.application.FhirR4Service;
import com.hims.domain.model.FhirResource;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api/fhir")
@RequiredArgsConstructor
public class FhirController {
    private final FhirR4Service service;

    @GetMapping("/{id}")
    public ResponseEntity<FhirResource> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<FhirResource>> findAll() {
        return ResponseEntity.ok(StreamSupport.stream(service.findAll().spliterator(), false).toList());
    }

    @PostMapping
    public ResponseEntity<FhirResource> createResource(@RequestBody FhirResource resource) {
        return ResponseEntity.ok(service.createResource(resource));
    }
}