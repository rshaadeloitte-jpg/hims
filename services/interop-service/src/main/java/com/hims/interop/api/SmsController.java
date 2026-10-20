package com.hims.interop.api;

import com.hims.interop.application.SmsService;
import com.hims.interop.application.exceptions.InvalidRequestException;
import com.hims.interop.domain.model.SmsNotificationQueue;
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
 * SMS notification queue APIs (MIG-055, eSanchar gateway). Same forward-only
 * status machine as the WhatsApp queue with an extra terminal EXPIRED (4):
 *   0=QUEUED 1=SENT 2=DELIVERED                (strictly one step forward)
 *   3=DELIVERY_FAILED legal ONLY from 0/1
 *   4=EXPIRED legal from 0/1/2
 *   anything else -&gt; 400 INTEROP_ILLEGAL_TRANSITION.
 */
@RestController
@RequestMapping("/api/v1/sms")
@Tag(name = "SMS notifications (MIG-055)", description = "eSanchar outbound SMS queue (07 §3.1 #15). Forward-only machine with EXPIRED terminal; provider send OUT OF SCOPE.")
public class SmsController {

    private final SmsService service;

    public SmsController(SmsService service) {
        this.service = service;
    }

    @PostMapping("/messages")
    @Operation(summary = "Enqueue an SMS notification",
            description = "{template, recipient, gateway?, payloadVars}; recipient required (<= 20 chars). New rows QUEUED (0).")
    public ResponseEntity<SmsNotificationQueue> create(@RequestBody CreateSmsRequest body) {
        SmsNotificationQueue created = service.create(body.template(), body.recipient(), body.gateway(), body.payloadVars());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/messages/{id}/status")
    @Operation(summary = "Apply a provider status update (forward-only machine)",
            description = "{statusCode, providerMessageId?, detail?}; statusCode in 1..4; only forward "
                    + "0->1->2 and DELIVERY_FAILED (3) from 0/1; EXPIRED (4) from 0/1/2. Unknown id -> 404.")
    public SmsNotificationQueue applyStatus(@PathVariable long id,
                                            @RequestBody StatusUpdateRequest body) {
        return service.applyStatus(id, body.statusCode(), body.providerMessageId(), body.detail());
    }

    @GetMapping("/messages")
    @Operation(summary = "List SMS messages", description = "Optional ?status= (0..4) filter; newest first.")
    public List<SmsNotificationQueue> list(@RequestParam(required = false) Short status) {
        if (status != null && (status < SmsNotificationQueue.STATUS_QUEUED
                || status > SmsNotificationQueue.STATUS_EXPIRED)) {
            throw new InvalidRequestException("BEXA003", "status must be within 0..4");
        }
        return service.list(status);
    }

    @GetMapping("/messages/{id}")
    @Operation(summary = "One SMS message with its status history")
    public SmsNotificationQueue get(@PathVariable long id) {
        return service.get(id);
    }

    public record CreateSmsRequest(String template, String recipient, String gateway, Map<String, Object> payloadVars) {}
    public record StatusUpdateRequest(Short statusCode, String providerMessageId, String detail) {}
}
