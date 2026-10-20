package com.hims.billing.api;

import com.hims.billing.application.BillingSagaIngressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Internal event-relay ingress (WP-S1) — models the FUTURE KAFKA CONSUMER of
 * ihms.ordering.events. Consumes ONLY E-13 OrderCreated and creates the bill
 * DRAFT idempotently keyed on eventId. Tagged internal; default-deny still
 * applies (no SecurityConfig exemption until MIG-004 service-to-service auth).
 */
@RestController
@RequestMapping("/api/v1/internal")
@Tag(name = "Internal event relay (WP-S1)", description = "INTERNAL ingress modeling the future Kafka consumer of ihms.ordering.events: consumes E-13 OrderCreated (order-created.v1) and creates the bill DRAFT idempotently per eventId. Unknown eventType -> 400 BILL_UNKNOWN_EVENT_TYPE; invalid payload -> 422 structured rejection (relay maps it to ordering-service BILLING_FAILED compensation). Emits NOTHING.")
public class InternalEventController {

    private final BillingSagaIngressService service;

    public InternalEventController(BillingSagaIngressService service) {
        this.service = service;
    }

    @PostMapping("/events")
    @Operation(summary = "Consume one sibling-service event (E-13 OrderCreated)",
            description = "{eventId, eventType, payload}. Only 'OrderCreated' (order-created.v1) is consumed: "
                    + "creates the interim bill DRAFT + one charge per payload line. First delivery 201 APPLIED; "
                    + "duplicate eventId replay 200 DUPLICATE (no-op, asserted at DB level); unknown eventType 400; "
                    + "payload validation failure 422 REJECTED {status,errorCode,message} which the relay maps to "
                    + "POST /api/v1/internal/orders/{id}/billing-failed on ordering-service.")
    public ResponseEntity<BillingSagaIngressService.IngestResult> consume(@RequestBody ConsumeEventRequest body) {
        BillingSagaIngressService.IngestResult result =
                service.ingest(body.eventId(), body.eventType(), body.payload());
        return switch (result.status()) {
            case "APPLIED" -> ResponseEntity.status(HttpStatus.CREATED).body(result);
            case "DUPLICATE" -> ResponseEntity.ok(result);
            default -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(result);
        };
    }

    public record ConsumeEventRequest(String eventId, String eventType, Map<String, Object> payload) {}
}
