package com.hims.interop.api;

import com.hims.interop.application.FhirR4Converter;
import com.hims.interop.application.Hl7V2Parser;
import com.hims.interop.application.InboundService;
import com.hims.interop.application.InteropKafkaConsumer;
import com.hims.interop.domain.model.InboundHis;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Inbound HL7 v2 queue APIs (MIG-055). Adds a pure-function HL7 v2 parser
 * + a HAPI-free FHIR R4 resource converter (Patient/Encounter Bundle) and
 * a Kafka consumer stub (in-flight continuity via envelope eventId UNIQUE).
 */
@RestController
@RequestMapping("/api/v1/inbound")
@Tag(name = "Inbound HIS queue (MIG-055)", description = "Raw inbound HL7 v2 messages; parser + FHIR R4 converter; Kafka consumer stub (in-flight continuity).")
public class InboundController {

    private final InboundService service;
    private final InteropKafkaConsumer kafkaConsumer;

    public InboundController(InboundService service, InteropKafkaConsumer kafkaConsumer) {
        this.service = service;
        this.kafkaConsumer = kafkaConsumer;
    }

    @GetMapping
    @Operation(summary = "List inbound messages (newest first)")
    public List<InboundHis> list() {
        return service.list();
    }

    @PostMapping("/hl7")
    @Operation(summary = "Receive an inbound HL7 v2 message",
            description = "{messageType, sender, rawPayload, envelopeEventId?}; messageType must be one of "
                    + "ADT^A01, ADT^A02, ADT^A03, ADT^A04, ADT^A08, ADT^A45, ORM^O01, ORU^R01 — else 400 "
                    + "INTEROP_INVALID_MESSAGE_TYPE. New rows are 0=RECEIVED.")
    public ResponseEntity<InboundHis> receive(@RequestBody ReceiveHl7Request body) {
        InboundHis created = service.receive(body.messageType(), body.sender(), body.rawPayload());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/hl7/kafka")
    @Operation(summary = "[kafka stub] Ingest one inbound HL7 envelope",
            description = "{envelopeEventId, messageType, sender, rawPayload}. Idempotent per envelopeEventId "
                    + "(UNIQUE). Replays of the same envelope return the existing row (consumerGroup stamped).")
    public InboundHis consumeKafka(@RequestBody KafkaRequest body) {
        return kafkaConsumer.consumeInboundHl7(body.envelopeEventId(),
                body.messageType(), body.sender(), body.rawPayload());
    }

    @PostMapping("/hl7/parse")
    @Operation(summary = "Parse a raw HL7 v2 message (no persistence)",
            description = "Validates the message and returns the parsed segments as JSON. Pure function — no DB write. "
                    + "Useful for testing the parser; production code should persist via /hl7 (HTTP) or /hl7/kafka "
                    + "(consumer stub).")
    public Map<String, Object> parse(@RequestBody ParseRequest body) {
        Hl7V2Parser.Hl7Message m = Hl7V2Parser.parse(body.rawPayload());
        Hl7V2Parser.validateMessageType(body.rawPayload());
        return Map.of(
                "messageType", m.messageType(),
                "triggerEvent", m.triggerEvent(),
                "controlId", m.controlId() == null ? "" : m.controlId(),
                "sendingApp", m.sendingApp() == null ? "" : m.sendingApp(),
                "sendingFacility", m.sendingFacility() == null ? "" : m.sendingFacility(),
                "segments", m.rawSegments().size());
    }

    @PostMapping("/hl7/parse/fhir")
    @Operation(summary = "Parse an HL7 v2 ADT and convert to a FHIR R4 Bundle",
            description = "Returns a Bundle with Patient (from PID) and Encounter (from PV1) as JSON-ready Maps. "
                    + "Pure function — no DB write.")
    public Map<String, Object> parseAndConvert(@RequestBody ParseRequest body) {
        Hl7V2Parser.Hl7Message m = Hl7V2Parser.parse(body.rawPayload());
        Hl7V2Parser.validateMessageType(body.rawPayload());
        return FhirR4Converter.adtToFhirBundle(m);
    }

    public record ReceiveHl7Request(String messageType, String sender, String rawPayload, String envelopeEventId) {}
    public record KafkaRequest(String envelopeEventId, String messageType, String sender, String rawPayload) {}
    public record ParseRequest(String rawPayload) {}
}
