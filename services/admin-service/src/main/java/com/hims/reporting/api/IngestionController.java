package com.hims.reporting.api;

import com.hims.reporting.application.EventIngressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * INTERNAL event-relay ingress (register WP-T2). Models the future Kafka consumer
 * of PatientRegistered / EncounterCreated / InvoiceGenerated / PaymentReceived —
 * NOT real sibling connectivity (disclosed at every site). Strict idempotency:
 * re-ingesting the same event returns DUPLICATE and touches no read model.
 * This service EMITS NOTHING to the outbox.
 */
@RestController
@RequestMapping("/api/v1/internal")
@Tag(name = "Internal Event Ingress (WP-T2)", description = "Internal event-relay ingress feeding the rm_* read models (production: Kafka consumer — contract stub, disclosed). Strict idempotency: replays return DUPLICATE without double counting.")
public class IngestionController {

    private final EventIngressService service;

    public IngestionController(EventIngressService service) {
        this.service = service;
    }

    @PostMapping("/events")
    @Operation(summary = "Relay one domain event onto the read models",
            description = "{eventType, payload}. Supported: PatientRegistered {patientId, mrn?, name?, "
                    + "registeredAt?} -> upsert rm_patient_registry; EncounterCreated {encounterId, statDate?, "
                    + "encounterType?} -> increment rm_encounter_stats; InvoiceGenerated {invoiceId, statDate?, "
                    + "headCode?, amount} and PaymentReceived {paymentId, statDate?, headCode?, amount} -> "
                    + "accumulate rm_revenue_daily (headCode default OPD). Unknown eventType -> 400 "
                    + "REPORTING_UNKNOWN_EVENT_TYPE. Re-ingest of the same source document id -> DUPLICATE, "
                    + "no double count.")
    public EventIngressService.IngestResult ingest(@RequestBody IngestEventRequest body) {
        return service.ingest(body.eventType(), body.payload());
    }

    public record IngestEventRequest(String eventType, Map<String, Object> payload) {}
}
