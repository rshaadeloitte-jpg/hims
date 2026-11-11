package com.hims.aasmaan.infrastructure.web;

import com.hims.aasmaan.application.AasmaanETicketService;
import com.hims.aasmaan.domain.model.AasmaanETicket;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/e-tickets")
public class AasmaanETicketController {
    private final AasmaanETicketService service;
    public AasmaanETicketController(AasmaanETicketService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<AasmaanETicket> create(@RequestBody AasmaanETicket t) { return ResponseEntity.ok(service.create(t)); }
    @GetMapping
    public ResponseEntity<List<AasmaanETicket>> list() { return ResponseEntity.ok(service.all()); }
    @GetMapping("/{id}")
    public ResponseEntity<AasmaanETicket> get(@PathVariable Long id) { return ResponseEntity.ok(service.byId(id)); }
}
