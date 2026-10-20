package com.hims.api;

import com.hims.application.HprRegistrationService;
import com.hims.domain.model.HprRegistration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api/hpr")
@RequiredArgsConstructor
public class HprController {
    private final HprRegistrationService service;

    @GetMapping("/{id}")
    public ResponseEntity<HprRegistration> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<HprRegistration>> findAll() {
        return ResponseEntity.ok(StreamSupport.stream(service.findAll().spliterator(), false).toList());
    }

    @PostMapping
    public ResponseEntity<HprRegistration> register(@RequestBody HprRegistration registration) {
        return ResponseEntity.ok(service.createRegistration(registration));
    }
}