package com.hims.ordering.api;

import com.hims.ordering.application.OrderService;
import com.hims.ordering.application.exceptions.InvalidOrderException;
import com.hims.ordering.domain.model.ClinicalOrder;
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

import java.math.BigDecimal;
import java.util.List;

/**
 * Ordering saga API (MIG-033, Catalog #4): clinical-order placement whose
 * ordering-&gt;downstream coupling travels as OrderInitiated via the transactional
 * outbox. Downstream services confirm/fail steps via the internal endpoints
 * (/api/v1/internal/orders/{id}/steps/{step}/confirm|fail); failures trigger the
 * OrderFailed compensation event in the same tx as the FAILED flip.
 */
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders (MIG-033)", description = "Clinical-order saga (Catalog #4): place order with typed items LAB|RADIOLOGY|NURSING|CONSULTATION "
        + "(status 1=INITIATED 2=CONFIRMED 3=CANCELLED 4=FAILED), cancel guard INITIATED-only; OrderInitiated (order-initiated.v1) on create; "
        + "OrderFailed (order-failed.v1) on step failure or cancel — same tx as the status flip")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Place a clinical order with >= 1 item",
            description = "BR-ORD-001 patientId+prescriberId required, encounterId XOR admissionId; BR-ORD-002 items[{itemType,"
                    + "itemId,serviceName,rate,qty}] with rate>=0 qty>0 and server-computed totals; BR-ORD-003 concessionPercent "
                    + "[0,100] HALF_UP rounding; BR-ORD-004 per-item doctorSharePercent [0,100]. Emits OrderInitiated once in the "
                    + "same tx (order lands INITIATED with an IN_PROGRESS saga).")
    public ResponseEntity<ClinicalOrder> create(@RequestBody CreateOrderRequest body) {
        List<OrderService.ItemInput> items = body.items() == null ? null : body.items().stream()
                .map(m -> new OrderService.ItemInput(
                        str(m.get("itemType")), lng(m.get("itemId")), str(m.get("serviceName")),
                        dec(m.get("rate")), intQty(m.get("qty")), dec(m.get("doctorSharePercent"))))
                .toList();
        ClinicalOrder created = service.create(body.patientId(), body.encounterId(),
                body.admissionId(), body.prescriberId(), body.wardId(),
                dec(body.concessionPercent()), body.pharmacyPrescriptionId(), items);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "List orders", description = "?status= (1..4 or name incl. FAILED) and/or ?patientId= filter.")
    public List<ClinicalOrder> list(@RequestParam(required = false) String status,
                                    @RequestParam(required = false) Long patientId) {
        return service.list(status == null ? null : parseStatus(status), patientId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Full order with items and saga state (statusCode incl. 4=FAILED, rejectionReason)")
    public ClinicalOrder get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order",
            description = "Guard: INITIATED (1) only -> CANCELLED (3). Succeeded saga steps are COMPENSATED and OrderFailed is "
                    + "emitted so downstream reservations roll back; 400 ORD_ILLEGAL_TRANSITION otherwise.")
    public ClinicalOrder cancel(@PathVariable long id) {
        return service.cancel(id);
    }

    // ------------------------------------------------------------------ helpers

    /** Status filter: numeric (1..4) or name (INITIATED, CONFIRMED, CANCELLED, FAILED). */
    static Short parseStatus(String raw) {
        try {
            return Short.valueOf(raw.trim());
        } catch (NumberFormatException ignored) {
            // fall through to name mapping
        }
        return switch (raw.trim().toUpperCase()) {
            case "INITIATED", "PLACED" -> 1;
            case "CONFIRMED", "BILLED" -> 2;
            case "CANCELLED" -> 3;
            case "FAILED", "BILLING_FAILED" -> 4;
            default -> throw new InvalidOrderException("BEXA003", "Unknown status: " + raw);
        };
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static Long lng(Object v) {
        return v == null ? null : Long.valueOf(String.valueOf(v));
    }

    private static BigDecimal dec(Object v) {
        return v == null ? null : new BigDecimal(String.valueOf(v));
    }

    private static Integer intQty(Object v) {
        return v == null ? null : Integer.valueOf(String.valueOf(v));
    }

    public record CreateOrderRequest(Long patientId, Long encounterId, Long admissionId,
                                     Long prescriberId, Long wardId,
                                     BigDecimal concessionPercent, Long pharmacyPrescriptionId,
                                     List<java.util.Map<String, Object>> items) {}
}
