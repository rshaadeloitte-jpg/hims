package com.hims.lab.api;

import com.hims.lab.application.CriticalAlertDeliveryService;
import com.hims.lab.domain.model.CriticalAlertDelivery;
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
 * Critical alert delivery API (GAP-REGISTER WP-7 HIGH).
 * Enhanced fan-out: creates delivery records per channel and tracks status.
 */
@RestController
@RequestMapping("/api/v1/critical-alerts")
@Tag(name = "Critical Alert Delivery (WP-7 HIGH)", description = "Enhanced fan-out delivery tracking for critical lab results (WhatsApp/SMS/email)")
public class CriticalAlertDeliveryController {

    private final CriticalAlertDeliveryService service;

    public CriticalAlertDeliveryController(CriticalAlertDeliveryService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create delivery records for a critical detail across channels")
    public ResponseEntity<List<CriticalAlertDelivery>> createDeliveries(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> channels = (List<String>) body.get("channels");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createDeliveries(
                        longVal(body, "bookingId"),
                        longVal(body, "detailId"),
                        str(body, "informPassedTo"),
                        str(body, "informPassedBy"),
                        longVal(body, "docId"),
                        str(body, "ward"),
                        str(body, "bedNo"),
                        str(body, "emailId"),
                        channels));
    }

    @PostMapping("/{id}/send")
    @Operation(summary = "Mark a delivery as sent")
    public CriticalAlertDelivery markSent(@PathVariable long id) {
        return service.markSent(id);
    }

    @PostMapping("/{id}/deliver")
    @Operation(summary = "Mark a delivery as delivered (confirmed receipt)")
    public CriticalAlertDelivery markDelivered(@PathVariable long id) {
        return service.markDelivered(id);
    }

    @PostMapping("/{id}/fail")
    @Operation(summary = "Mark a delivery as failed")
    public CriticalAlertDelivery markFailed(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return service.markFailed(id, str(body, "failureReason"));
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "Retry a failed delivery")
    public CriticalAlertDelivery retry(@PathVariable long id) {
        return service.retry(id);
    }

    @GetMapping("/pending")
    @Operation(summary = "List pending/retry deliveries (dispatch queue)")
    public List<CriticalAlertDelivery> listPending() {
        return service.listPending();
    }

    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "List deliveries for a booking")
    public List<CriticalAlertDelivery> listByBooking(@PathVariable long bookingId) {
        return service.listByBooking(bookingId);
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "List deliveries for a patient")
    public List<CriticalAlertDelivery> listByPatient(@PathVariable long patientId) {
        return service.listByPatient(patientId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a delivery record by id")
    public CriticalAlertDelivery get(@PathVariable long id) {
        return service.get(id);
    }

    private static String str(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private static Long longVal(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        return Long.valueOf(String.valueOf(v));
    }
}
