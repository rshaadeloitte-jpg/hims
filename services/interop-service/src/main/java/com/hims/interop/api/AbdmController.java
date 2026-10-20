package com.hims.interop.api;

import com.hims.interop.application.AbdmService;
import com.hims.interop.application.InteropKafkaConsumer;
import com.hims.interop.domain.model.AbdmRequestQueue;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * ABDM request queue APIs (MIG-055, M0/M1/M2/M3): correlation-id unique (409),
 * idempotent callbacks, ABHA + HIP + HIU + HFR + CM all on the same queue.
 * POST /api/v1/abdm/requests/kafka is the Kafka consumer stub (in-flight
 * continuity; production uses spring-kafka with consumer groups).
 */
@RestController
@RequestMapping("/api/v1/abdm")
@Tag(name = "ABDM requests (MIG-055)", description = "ABDM gateway request queue M0..M3 (ABHA, HIP, HIU, HFR, CM). correlationId UNIQUE (409); envelopeEventId UNIQUE for Kafka replay dedup; callbacks idempotent (first wins).")
public class AbdmController {

    private final AbdmService service;
    private final InteropKafkaConsumer kafkaConsumer;

    public AbdmController(AbdmService service, InteropKafkaConsumer kafkaConsumer) {
        this.service = service;
        this.kafkaConsumer = kafkaConsumer;
    }

    @PostMapping("/requests")
    @Operation(summary = "Enqueue an ABDM gateway request",
            description = "{requestKind, correlationId, payload, envelopeEventId?}; kind in ABHA|M1|HIP|M2|HIU|M3|HFR|M0|CM else 400 BEXA003; "
                    + "duplicate correlationId or envelopeEventId -> 409 INTEROP_DUPLICATE_*; new rows: status 0=ACCEPTED, milestone auto-derived from kind.")
    public ResponseEntity<AbdmRequestQueue> create(@RequestBody CreateRequest body) {
        AbdmRequestQueue created = service.create(body.requestKind(), body.correlationId(), body.payload(), body.envelopeEventId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/requests/{correlationId}/callback")
    @Operation(summary = "Apply an ABDM gateway callback (idempotent)",
            description = "{payload, status}; unknown correlationId -> 404; status within NHCX range 0..3 "
                    + "(per 06 §A9) else 400 BEXA003; repeat callbacks are no-ops returning the stored "
                    + "first-callback state.")
    public AbdmRequestQueue callback(@PathVariable String correlationId,
                                     @RequestBody CallbackRequest body) {
        return service.callback(correlationId, body.payload(), body.status());
    }

    @GetMapping("/requests/{id}")
    @Operation(summary = "Read one ABDM request (by id)")
    public AbdmRequestQueue get(@PathVariable long id) {
        return service.get(id);
    }

    /**
     * Kafka consumer stub — models the production Kafka consumer (group
     * {@code ihms-interop-abdm}). Idempotent per (envelopeEventId, correlationId).
     * OUT OF SCOPE: actual spring-kafka wiring + offset commits.
     */
    @PostMapping("/requests/kafka")
    @Operation(summary = "[kafka stub] Ingest one ABDM envelope",
            description = "{envelopeEventId, requestKind, correlationId, payload}. Idempotent per "
                    + "envelopeEventId (UNIQUE) and correlationId (UNIQUE). Replays of the same envelope "
                    + "return the existing row.")
    public AbdmRequestQueue consumeKafka(@RequestBody KafkaRequest body) {
        return kafkaConsumer.consumeAbdmRequest(body.envelopeEventId(),
                body.requestKind(), body.correlationId(), body.payload());
    }

    public record CreateRequest(String requestKind, String correlationId, Map<String, Object> payload, String envelopeEventId) {}
    public record CallbackRequest(Map<String, Object> payload, Short status) {}
    public record KafkaRequest(String envelopeEventId, String requestKind, String correlationId, Map<String, Object> payload) {}
}
