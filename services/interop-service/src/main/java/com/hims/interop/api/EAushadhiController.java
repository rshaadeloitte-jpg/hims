package com.hims.interop.api;

import com.hims.interop.application.EAushadhiService;
import com.hims.interop.domain.model.EAushadhiSyncQueue;
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

/**
 * e-Aushadhi sync queue APIs (register WP-T1 extension): enqueue + forward-only
 * status machine (sync emulation surface). The actual e-Aushadhi API call is
 * OUT OF SCOPE (comment-only per register); the queue row + status machine
 * ARE the deliverable.
 */
@RestController
@RequestMapping("/api/v1/interop/eaushadhi")
@Tag(name = "e-Aushadhi sync (WP-T1 extension)", description = "e-Aushadhi drug catalogue sync queue with FORWARD-ONLY machine: 0=PENDING -> 1=SYNCED -> 2=FAILED; FAILED (2) only from 0; anything else 400 INTEROP_ILLEGAL_TRANSITION. Every accepted transition appends a t_eaushadhi_sync_status history row.")
public class EAushadhiController {

    private final EAushadhiService service;

    public EAushadhiController(EAushadhiService service) {
        this.service = service;
    }

    @PostMapping("/sync")
    @Operation(summary = "Enqueue a drug for e-Aushadhi sync",
            description = "{drugCode, drugName, manufacturer?, category?}; drugCode and drugName required. New rows PENDING (0).")
    public ResponseEntity<EAushadhiSyncQueue> enqueue(@RequestBody EnqueueRequest body) {
        EAushadhiSyncQueue created =
                service.enqueue(body.drugCode(), body.drugName(), body.manufacturer(), body.category());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/sync/{id}/status")
    @Operation(summary = "Apply a sync status update (forward-only machine)",
            description = "{statusCode, detail?}; statusCode in 1..2; only forward 0->1 and FAILED (2) from 0 — else 400 INTEROP_ILLEGAL_TRANSITION. Unknown id -> 404.")
    public EAushadhiSyncQueue applyStatus(@PathVariable long id,
                                          @RequestBody StatusUpdateRequest body) {
        return service.applyStatus(id, body.statusCode(), body.detail());
    }

    @GetMapping("/sync")
    @Operation(summary = "List sync queue rows", description = "Optional ?status= (0..2) filter; newest first.")
    public List<EAushadhiSyncQueue> list(@RequestParam(required = false) Short status) {
        if (status != null && (status < EAushadhiSyncQueue.STATUS_PENDING
                || status > EAushadhiSyncQueue.STATUS_FAILED)) {
            throw new com.hims.interop.application.exceptions.InvalidRequestException("BEXA003",
                    "status must be within 0..2");
        }
        return service.list(status);
    }

    @GetMapping("/sync/{id}")
    @Operation(summary = "One sync queue row with its status history")
    public EAushadhiSyncQueue get(@PathVariable long id) {
        return service.get(id);
    }

    public record EnqueueRequest(String drugCode, String drugName, String manufacturer, String category) {}
    public record StatusUpdateRequest(Short statusCode, String detail) {}
}
