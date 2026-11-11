package com.hims.radiology.api;

import com.hims.radiology.application.RadOrderService;
import com.hims.radiology.application.exceptions.InvalidRadOrderException;
import com.hims.radiology.domain.model.RadOrderBooking;
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
 * Radiology API (GAP-REGISTER-5 WP-R1, Catalog #8): rad order booking header+lines
 * creation with validation guards, get, list (?patientId=&amp;status=&amp;modality=),
 * start / capture-image / report / cancel lifecycle guards. RadOrderCreated is
 * DEFERRED per KAFKA-EVENT-CATALOG §3 — NO event is emitted anywhere.
 */
@RestController
@RequestMapping("/api/v1/rad-orders")
@Tag(name = "Rad Orders (WP-R1)", description = "Radiology (Catalog #8): booking header+lines, modality XRAY/CT/MRI/US, priority 1=routine 2=urgent, status 1=SCHEDULED 2=IN_PROGRESS 3=COMPLETED 4=CANCELLED; start/capture-image/report/cancel guards; RadOrderCreated DEFERRED — no events emitted")
public class RadOrderController {

    private final RadOrderService service;

    public RadOrderController(RadOrderService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create a rad order booking with >=1 line",
            description = "patientId + modality required (XRAY/CT/MRI/US); encounterId XOR admissionId "
                    + "(400 BEXA003 when both); priority 1=routine 2=urgent (default routine); "
                    + "lines[{itemId,qty}] >= 1, qty > 0. Emits nothing (RadOrderCreated DEFERRED).")
    public ResponseEntity<RadOrderBooking> create(@RequestBody CreateRadOrderRequest body) {
        List<RadOrderService.LineInput> lines = body.lines() == null ? null : body.lines().stream()
                .map(l -> new RadOrderService.LineInput(
                        lng(l.get("itemId")), qty(l.get("qty"))))
                .toList();
        RadOrderBooking created = service.create(body.patientId(), body.encounterId(),
                body.admissionId(), body.modality(), body.priority(), lines);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "List rad orders", description = "?status= (1..4 or name), ?patientId= and/or ?modality= filter.")
    public List<RadOrderBooking> list(@RequestParam(required = false) String status,
                                      @RequestParam(required = false) Long patientId,
                                      @RequestParam(required = false) String modality) {
        return service.list(status == null ? null : parseStatus(status), patientId, modality);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Full rad order with lines")
    public RadOrderBooking get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Start a scheduled rad order",
            description = "Guard: SCHEDULED (1) -> IN_PROGRESS (2); 400 RAD_ILLEGAL_TRANSITION otherwise. Emits nothing.")
    public RadOrderBooking start(@PathVariable long id) {
        return service.start(id);
    }

    @PostMapping("/{id}/capture-image")
    @Operation(summary = "Mark the image as captured",
            description = "Sets is_image_captured=true; 400 when the order is already COMPLETED. Emits nothing.")
    public RadOrderBooking captureImage(@PathVariable long id) {
        return service.captureImage(id);
    }

    @PostMapping("/{id}/report")
    @Operation(summary = "Report the study",
            description = "Requires IN_PROGRESS; sets reportText/reportedBy and status COMPLETED (3). Emits nothing.")
    public RadOrderBooking report(@PathVariable long id, @RequestBody ReportRequest body) {
        return service.report(id, body.reportText(), body.reportedBy());
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a rad order",
            description = "Guard: not COMPLETED -> CANCELLED (4); 400 RAD_ILLEGAL_TRANSITION when COMPLETED. Emits nothing.")
    public RadOrderBooking cancel(@PathVariable long id) {
        return service.cancel(id);
    }

    // ------------------------------------------------------------------ helpers

    /** Status filter: numeric (1..4) or name (SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED). */
    static Short parseStatus(String raw) {
        try {
            return Short.valueOf(raw.trim());
        } catch (NumberFormatException ignored) {
            // fall through to name mapping
        }
        return switch (raw.trim().toUpperCase()) {
            case "SCHEDULED" -> 1;
            case "IN_PROGRESS" -> 2;
            case "COMPLETED" -> 3;
            case "CANCELLED" -> 4;
            default -> throw new InvalidRadOrderException("BEXA003",
                    "Unknown status: " + raw);
        };
    }

    private static Long lng(Object v) {
        return v == null ? null : Long.valueOf(String.valueOf(v));
    }

    private static java.math.BigDecimal qty(Object v) {
        return v == null ? null : new java.math.BigDecimal(String.valueOf(v));
    }

    public record CreateRadOrderRequest(Long patientId, Long encounterId, Long admissionId,
                                        String modality, Short priority,
                                        List<Map<String, Object>> lines) {}

    public record ReportRequest(String reportText, String reportedBy) {}
}
