package com.hims.kiosk.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.kiosk.application.KioskService;
import com.hims.kiosk.domain.model.KioskRegistration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/kiosk/registration")
@Tag(name = "Kiosk Self-Registration (M31)", description = "Self-registration with token issuance")
public class KioskRegistrationController {

    private final KioskService service;
    public KioskRegistrationController(KioskService service) { this.service = service; }

    @PostMapping
    @Operation(summary = "Self-register at kiosk")
    public ResponseEntity<KioskRegistration> register(@RequestBody JsonNode body,
                                                       @RequestParam(required = false) Long unitId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(body, unitId));
    }

    @GetMapping("/token/{token}")
    @Operation(summary = "Find registration by token")
    public ResponseEntity<KioskRegistration> byToken(@PathVariable String token) {
        return service.findByToken(token)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
