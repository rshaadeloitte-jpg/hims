package com.hims.interop.api;

import com.hims.interop.application.WhatsappService;
import com.hims.interop.domain.model.WhatsappMessageQueue;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * WhatsApp queue APIs (register WP-T1): enqueue + forward-only status machine
 * (webhook emulation surface). Provider send/receive jobs are OUT OF SCOPE.
 */
@RestController
@RequestMapping("/api/v1/whatsapp")
@Tag(name = "WhatsApp messages (WP-T1)", description = "WhatsApp notification queue with FORWARD-ONLY machine: 0=QUEUED -> 1=SENT -> 2=DELIVERED -> 3=READ (one step at a time); FAILED (4) only from 0/1; anything else 400 INTEROP_ILLEGAL_TRANSITION. Every accepted transition appends a t_whatsapp_message_status history row.")
public class WhatsappController {

    private final WhatsappService service;

    public WhatsappController(WhatsappService service) {
        this.service = service;
    }

    @PostMapping("/messages")
    @Operation(summary = "Enqueue a WhatsApp notification",
            description = "{template, recipient, payloadVars}; recipient required (<= 20 chars). New rows QUEUED (0).")
    public ResponseEntity<WhatsappMessageQueue> create(@RequestBody CreateMessageRequest body) {
        WhatsappMessageQueue created =
                service.create(body.template(), body.recipient(), body.payloadVars());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/messages/{id}/status")
    @Operation(summary = "Apply a provider status update (forward-only machine)",
            description = "{statusCode, providerMessageId?, detail?}; statusCode in 1..4; only forward "
                    + "0->1->2->3 and FAILED (4) from 0/1 — else 400 INTEROP_ILLEGAL_TRANSITION. Unknown "
                    + "id -> 404.")
    public WhatsappMessageQueue applyStatus(@PathVariable long id,
                                            @RequestBody StatusUpdateRequest body) {
        return service.applyStatus(id, body.statusCode(), body.providerMessageId(), body.detail());
    }

    @GetMapping("/messages")
    @Operation(summary = "List messages", description = "Optional ?status= (0..4) filter; newest first.")
    public List<WhatsappMessageQueue> list(@RequestParam(required = false) Short status) {
        if (status != null && (status < WhatsappMessageQueue.STATUS_QUEUED
                || status > WhatsappMessageQueue.STATUS_FAILED)) {
            throw new com.hims.interop.application.exceptions.InvalidRequestException("BEXA003",
                    "status must be within 0..4");
        }
        return service.list(status);
    }

    @GetMapping("/messages/{id}")
    @Operation(summary = "One message with its status history")
    public WhatsappMessageQueue get(@PathVariable long id) {
        return service.get(id);
    }

    public record CreateMessageRequest(String template, String recipient, Map<String, Object> payloadVars) {}
    public record StatusUpdateRequest(Short statusCode, String providerMessageId, String detail) {}
}
