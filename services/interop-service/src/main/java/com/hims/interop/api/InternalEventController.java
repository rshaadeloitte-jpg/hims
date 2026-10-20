package com.hims.interop.api;

import com.hims.interop.application.InteropEventService;
import com.hims.interop.domain.model.OutboundHis;
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
 * Internal event-relay ingress (register WP-T1) — models the FUTURE KAFKA
 * CONSUMER. Routes sibling-service events into t_outbound_his, idempotent per
 * (eventType, aggregateId). Tagged internal.
 */
@RestController
@RequestMapping("/api/v1/internal")
@Tag(name = "Internal event relay (WP-T1)", description = "INTERNAL ingress modeling the future Kafka consumer: routes events to t_outbound_his (InvoiceGenerated/PaymentReceived -> HL7; LabResultFinalized -> EHR; patient/encounter/admission/discharge -> HL7); idempotent per (eventType, aggregateId). Emits NOTHING.")
public class InternalEventController {

    private final InteropEventService service;

    public InternalEventController(InteropEventService service) {
        this.service = service;
    }

    @PostMapping("/events")
    @Operation(summary = "Ingest one sibling-service event",
            description = "{eventType, aggregateId, payload}. Routing: InvoiceGenerated/PaymentReceived "
                    + "-> t_outbound_his(HL7); LabResultFinalized -> EHR; PatientRegistered/EncounterCreated/"
                    + "PatientAdmitted/PatientDischarged -> HL7; unknown eventType -> 400. Idempotent per "
                    + "(eventType, aggregateId): first call 201 with the new row; duplicate 200 with the "
                    + "EXISTING row (no duplicate written).")
    public ResponseEntity<OutboundHis> ingest(@RequestBody IngestRequest body) {
        InteropEventService.IngestResult result = service.ingest(body.eventType(), body.aggregateId(), body.payload());
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.row());
    }

    public record IngestRequest(String eventType, String aggregateId, Map<String, Object> payload) {}
}
