package com.hims.linen.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.linen.application.LinenRequestService;
import com.hims.linen.domain.model.LinenRequest;
import com.hims.linen.domain.model.LinenRequestDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Linen &amp; laundry request lifecycle API (MIG-023 sub-slice, S16) — clean
 * target of legacy LinenLaundryServiceImpl (raise/approve/handover/
 * receive-main-store/process/dispatch/complete, counts, detail updates).
 * REST-only: no linen events in KAFKA-EVENT-CATALOG.
 */
@RestController
@RequestMapping("/api/v1/linen-requests")
@Tag(name = "Linen Requests (MIG-023)",
        description = "Linen & laundry request lifecycle: raise, approve, handover, receive at main store, "
                + "process (wash), dispatch, complete, counts, detail updates")
public class LinenRequestController {
    private final LinenRequestService service;

    public LinenRequestController(LinenRequestService service) {
        this.service = service;
    }

    @GetMapping("/report")
    @Operation(summary = "Laundry status counts", description = "Count of linen requests grouped by lifecycle status.")
    public java.util.Map<String, Long> report() { return service.laundryReport(); }

    @PostMapping
    @Operation(summary = "Raise a linen request",
            description = "Payload {\"request\": {storeId, toStoreId, ...}, \"details\": [{itemId, batchId, quantity, remark}]}. "
                    + "Server stamps requisitionNo ({unitPrefix}/LR/{yy}/{%05d}), requestStatus=1(PENDING), requestDate. "
                    + "unitPrefix optional (default IHMS until M_UNITMASTER integration).")
    public ResponseEntity<LinenRequest> raise(@RequestBody(required = false) JsonNode body,
                                              @RequestParam(required = false) Long unitId,
                                              @RequestParam(required = false) String unitPrefix) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.raiseRequest(body, unitId, unitPrefix));
    }

    @GetMapping
    @Operation(summary = "List linen requests",
            description = "A store sees requests where it is source OR destination (storeId = store_id OR to_store_id). "
                    + "Optional filters: unitId, storeId, requestStatus (1..7), search (requisitionNo LIKE), "
                    + "fromDate/toDate (request date range, inclusive). Newest first.")
    public List<LinenRequest> list(@RequestParam(required = false) Long unitId,
                                   @RequestParam(required = false) Long storeId,
                                   @RequestParam(required = false) Long requestStatus,
                                   @RequestParam(required = false) String search,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return service.listRequests(unitId, storeId, requestStatus, search, fromDate, toDate);
    }

    @GetMapping("/counts")
    @Operation(summary = "Dashboard request counts",
            description = "TOTAL / PENDING / ACCEPTED / HANDOVERED / RECEIVED / WASHED / DISPATCHED / COMPLETED, "
                    + "scoped by unitId + storeId (source or destination) + optional date range (legacy getLinenRequestCount).")
    public Map<String, Long> counts(@RequestParam(required = false) Long unitId,
                                    @RequestParam(required = false) Long storeId,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return service.getCounts(unitId, storeId, fromDate, toDate);
    }

    @GetMapping("/{id}")
    public LinenRequest get(@PathVariable long id) {
        return service.getRequest(id);
    }

    @GetMapping("/{id}/details")
    @Operation(summary = "List detail lines of a request",
            description = "One row per requested linen item line (quantity/receivedQuantity/damagedQuantity/"
                    + "dispatchedQuantity/finalQuantity/reason).")
    public List<LinenRequestDetails> details(@PathVariable long id) {
        return service.getDetails(id);
    }

    @PutMapping("/{id}/details")
    @Operation(summary = "Update detail quantity/remark before approval",
            description = "Body [{id, quantity?, remark?}]; request must be PENDING (legacy updateItemsForLinenRequest).")
    public List<LinenRequestDetails> updateDetails(@PathVariable long id,
                                                   @RequestBody(required = false) JsonNode body) {
        return service.updateDetails(id, body);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a request",
            description = "PENDING(1) -> ACCEPTED(2) (legacy approveLinenRequest).")
    public LinenRequest approve(@PathVariable long id) {
        return service.approve(id);
    }

    @PostMapping("/{id}/handover")
    @Operation(summary = "Hand over linen to the laundry",
            description = "ACCEPTED(2) -> HANDOVERED(3); handedOverTo stamped (legacy handOverLinenItems).")
    public LinenRequest handover(@PathVariable long id, @RequestParam String handedOverTo) {
        return service.handover(id, handedOverTo);
    }

    @PostMapping("/{id}/receive-main-store")
    @Operation(summary = "Receive at main store",
            description = "HANDOVERED(3) -> RECEIVED(4). Optional body [{id, receivedQuantity, damagedQuantity?, reason?}]: "
                    + "lines with damagedQuantity stamp receivedQuantity + damagedQuantity + reason "
                    + "(legacy receiveLinenRequestMainStore).")
    public LinenRequest receiveMainStore(@PathVariable long id,
                                         @RequestBody(required = false) JsonNode body) {
        return service.receiveMainStore(id, body);
    }

    @PostMapping("/{id}/process")
    @Operation(summary = "Process/wash the request",
            description = "RECEIVED(4) -> WASHED(5) (legacy processLinenRequest).")
    public LinenRequest process(@PathVariable long id) {
        return service.process(id);
    }

    @PostMapping("/{id}/dispatch")
    @Operation(summary = "Dispatch washed linen back",
            description = "WASHED(5) -> DISPATCHED(6). Optional body [{id, dispatchedQuantity, damagedQuantity?, reason?}]: "
                    + "dispatchedQuantity stamped; damagedQuantity ACCUMULATED (new + previous — legacy dispatchLinenRequest).")
    public LinenRequest dispatch(@PathVariable long id,
                                 @RequestBody(required = false) JsonNode body) {
        return service.dispatch(id, body);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete the request (final receive)",
            description = "DISPATCHED(6) -> COMPLETED(7); receivedBy stamped. Optional body [{id, finalQuantity}] stamps "
                    + "per-line finalQuantity (legacy receiveLinenRequest). Damaged-quantity stock subtraction is deferred "
                    + "until the item-service integration slice.")
    public LinenRequest complete(@PathVariable long id,
                                 @RequestParam(required = false) String receivedBy,
                                 @RequestBody(required = false) JsonNode body) {
        return service.complete(id, receivedBy, body);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a request",
            description = "status=false + requestStatus=0 (legacy deleteLinenRequest).")
    public LinenRequest delete(@PathVariable long id) {
        return service.deleteRequest(id);
    }
}
