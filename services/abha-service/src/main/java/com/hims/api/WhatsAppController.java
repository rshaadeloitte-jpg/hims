package com.hims.api;

import com.hims.application.WhatsAppNotificationService;
import com.hims.domain.model.WhatsAppNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api/whatsapp")
@RequiredArgsConstructor
public class WhatsAppController {
    private final WhatsAppNotificationService service;

    @GetMapping("/{id}")
    public ResponseEntity<WhatsAppNotification> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<WhatsAppNotification>> findAll() {
        return ResponseEntity.ok(StreamSupport.stream(service.findAll().spliterator(), false).toList());
    }

    @PostMapping
    public ResponseEntity<WhatsAppNotification> send(@RequestBody WhatsAppNotification notification) {
        return ResponseEntity.ok(service.sendNotification(notification));
    }
}