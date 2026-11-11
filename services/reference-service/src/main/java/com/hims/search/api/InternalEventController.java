package com.hims.search.api;

import com.hims.search.application.SearchIngestService;
import com.hims.search.domain.model.IndexRequest;
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
 * Internal event-routing ingress (register WP-T3). Models the FUTURE Kafka
 * consumer of sibling topics — production replaces this endpoint with a
 * consumer group; documented at every site, not invented silently.
 */
@RestController
@RequestMapping("/api/v1/internal/events")
@Tag(name = "Internal event ingress (WP-T3)", description = "Models the future Kafka consumer of sibling events: routes eventType to the t_index_request journal + dispatches to the indexer backend. Dedupe via latest-row: duplicate ingress returns the existing row (200), new rows return 201.")
public class InternalEventController {

    private final SearchIngestService service;

    public InternalEventController(SearchIngestService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Route one internal event into the index journal",
            description = "{eventType, payload{docId, ...}}. Routing: PatientRegistered -> patient; "
                    + "EncounterCreated/PatientAdmitted/PatientDischarged -> encounter; InventoryUpdated -> item; "
                    + "MasterDataChanged -> icd10cm|healthfactor (payload.collection, default healthfactor); "
                    + "unknown eventType -> 400. Journal 0=PENDING then synchronous dispatch (1=INDEXED | 2=FAILED). "
                    + "201 when a new row was created, 200 when dedupe returned the existing row.")
    public ResponseEntity<IndexRequest> ingest(@RequestBody IngestEventRequest body) {
        SearchIngestService.IngestResult result = service.ingest(body.eventType(), body.payload());
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(result.request());
    }

    public record IngestEventRequest(String eventType, Map<String, Object> payload) {}
}
