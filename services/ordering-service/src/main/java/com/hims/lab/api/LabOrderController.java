package com.hims.lab.api;

import com.hims.lab.application.LabOrderService;
import com.hims.lab.application.exceptions.InvalidLabRequestException;
import com.hims.lab.domain.model.PathOrderBooking;
import com.hims.lab.domain.model.PathOrderDetail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Lab order result-side API (GAP-REGISTER WP-7): booking+details creation,
 * status machine (collect/start/cancel), parameter result upsert,
 * finalize-per-detail (E-05 only when the WHOLE booking finalizes), worklist.
 */
@RestController
@RequestMapping("/api/v1/lab/orders")
@Tag(name = "Lab Orders (WP-7)", description = "Patho order booking result-side slice (Catalog #7): status machine 1=ORDERED..5=CANCELLED, parameter results, finalize-per-detail; E-05 LabResultFinalized on whole-booking finalization")
public class LabOrderController {

    private final LabOrderService service;

    public LabOrderController(LabOrderService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create a lab booking with test lines",
            description = "patientId required; encounterId nullable; tests[{testCode,testName}] >= 1.")
    public ResponseEntity<PathOrderBooking> create(@RequestBody CreateOrderRequest body) {
        List<LabOrderService.TestLine> lines = body.tests() == null ? null : body.tests().stream()
                .map(t -> new LabOrderService.TestLine(str(t.get("testCode")), str(t.get("testName"))))
                .toList();
        PathOrderBooking created = service.create(body.patientId(), body.encounterId(),
                body.isOutSourced(), lines);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "Worklist", description = "?status= (1..5 or name) and/or ?patientId= filter.")
    public List<PathOrderBooking> worklist(@RequestParam(required = false) String status,
                                           @RequestParam(required = false) Long patientId) {
        return service.worklist(status == null ? null : parseStatus(status), patientId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Full booking with details + parameters")
    public PathOrderBooking get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/collect")
    @Operation(summary = "ORDERED -> SAMPLE_COLLECTED")
    public PathOrderBooking collect(@PathVariable long id) {
        return service.collect(id);
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "SAMPLE_COLLECTED -> IN_PROGRESS")
    public PathOrderBooking start(@PathVariable long id) {
        return service.start(id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "ORDERED/SAMPLE_COLLECTED/IN_PROGRESS -> CANCELLED")
    public PathOrderBooking cancel(@PathVariable long id) {
        return service.cancel(id);
    }

    @PostMapping("/{id}/details/{detailId}/results")
    @Operation(summary = "Upsert result parameter rows for one detail",
            description = "Keyed by parameterName (case-insensitive); rows removed when absent from payload; locked after finalize.")
    public PathOrderDetail upsertResults(@PathVariable long id, @PathVariable long detailId,
                                         @RequestBody ResultsRequest body) {
        List<LabOrderService.ResultRow> rows = body.results() == null ? null : body.results().stream()
                .map(r -> new LabOrderService.ResultRow(
                        str(r.get("parameterName")), str(r.get("value")),
                        str(r.get("unit")), str(r.get("referenceRange"))))
                .toList();
        return service.upsertResults(id, detailId, rows);
    }

    @PostMapping("/{id}/details/{detailId}/finalize")
    @Operation(summary = "Finalize one detail",
            description = "Marks the detail's parameter rows final; when ALL details are final the booking becomes FINALIZED and E-05 LabResultFinalized is emitted once (partition key encounterId).")
    public PathOrderBooking finalizeDetail(@PathVariable long id, @PathVariable long detailId) {
        return service.finalizeDetail(id, detailId);
    }

    // ------------------------------------------------------------------ M15 workflow

    @PostMapping("/{id}/details/{detailId}/barcode")
    @Operation(summary = "Generate sample barcode (M15)",
            description = "Idempotent barcode generation for a detail: yyyymm + 7-digit seq via lab_barcode_seq. Also stamps sample_no. Fails on cancelled/finalized booking.")
    public PathOrderDetail generateBarcode(@PathVariable long id, @PathVariable long detailId) {
        return service.generateBarcode(id, detailId);
    }

    @PostMapping("/{id}/details/{detailId}/accept")
    @Operation(summary = "Accept sample (M15)",
            description = "Ported from CIMS_SampleAccept / SampleAcceptDto: requires sample_no (barcode first), flips is_accepted + acceptance_date. Guards: already rejected/accepted, cancelled booking.")
    public PathOrderDetail acceptSample(@PathVariable long id, @PathVariable long detailId,
                                        @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("acceptanceReason");
        return service.acceptSample(id, detailId, reason);
    }

    @PostMapping("/{id}/details/{detailId}/reject")
    @Operation(summary = "Reject sample (M15)",
            description = "Ported from sampleRejected / RejectResultDto: requires rejectionReason, flips is_rejected + rejection_date and clears finalize/verify so results must be re-entered.")
    public PathOrderDetail rejectSample(@PathVariable long id, @PathVariable long detailId,
                                        @RequestBody Map<String, String> body) {
        String reason = body == null ? null : body.get("rejectionReason");
        return service.rejectSample(id, detailId, reason);
    }

    @PostMapping("/{id}/details/{detailId}/verify")
    @Operation(summary = "Double-verification release (M15)",
            description = "Second checker confirms finalized detail: requires is_final, not rejected/verified. When ALL details verified emits LabResultVerified once.")
    public PathOrderDetail verifyDetail(@PathVariable long id, @PathVariable long detailId) {
        return service.verifyDetail(id, detailId);
    }

    @PostMapping("/{id}/details/{detailId}/notify-critical")
    @Operation(summary = "Critical alert notify (M15)",
            description = "Emits LabCriticalAlert for critical params (value outside reference_range). Marks critical_notified_at and booking is_critical. WhatsApp/SMS via interop outbox downstream (feature-flagged).")
    public PathOrderDetail notifyCritical(@PathVariable long id, @PathVariable long detailId) {
        return service.notifyCritical(id, detailId);
    }

    @GetMapping("/workload")
    @Operation(summary = "Lab workload counts (M15)",
            description = "Counts per status (ORDERED..CANCELLED) + TOTAL; used for lab workload board.")
    public Map<String, Long> workload() {
        return service.workload();
    }

    @GetMapping("/covid")
    @Operation(summary = "COVID lab list (M15)",
            description = "Bookings where any testName contains COVID (case-insensitive). In-memory filter; move to DB query when volume grows.")
    public List<PathOrderBooking> covidList() {
        return service.covidList();
    }

    // ------------------------------------------------------------------ helpers

    /** Status filter: numeric (1..5) or name (ORDERED, SAMPLE_COLLECTED, ...). */
    static Short parseStatus(String raw) {
        try {
            return Short.valueOf(raw.trim());
        } catch (NumberFormatException ignored) {
            // fall through to name mapping
        }
        return switch (raw.trim().toUpperCase()) {
            case "ORDERED" -> 1;
            case "SAMPLE_COLLECTED" -> 2;
            case "IN_PROGRESS" -> 3;
            case "FINALIZED" -> 4;
            case "CANCELLED" -> 5;
            default -> throw new InvalidLabRequestException("LAB_REQ_INVALID",
                    "Unknown status: " + raw);
        };
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    public record CreateOrderRequest(Long patientId, Long encounterId, Boolean isOutSourced,
                                     List<Map<String, Object>> tests) {}

    public record ResultsRequest(List<Map<String, Object>> results) {}
}
