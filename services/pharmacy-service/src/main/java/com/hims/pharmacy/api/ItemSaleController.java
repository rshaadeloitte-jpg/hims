package com.hims.pharmacy.api;

import com.hims.pharmacy.application.ItemSaleService;
import com.hims.pharmacy.application.exceptions.InvalidSaleRequestException;
import com.hims.pharmacy.domain.model.ItemSale;
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
import java.util.Map;

/**
 * Pharmacy item-sale API (GAP-REGISTER-5 WP-R2, Catalog #9): sale creation with
 * validation guards, get, list (?patientId=&amp;status=), dispense (partial allowed).
 * Dispensing synchronously borrows stock from item-service (single-writer D-1);
 * BR-PHAR-001/002 run in the same transaction; NOTHING is emitted to the outbox
 * (DispensingCompleted is PROPOSED, not a catalog event).
 */
@RestController
@RequestMapping("/api/v1/item-sales")
@Tag(name = "Item Sales (WP-R2)", description = "Pharmacy & Dispensing (Catalog #9): sales status 1=PENDING 2=PARTIALLY_DISPENSED 3=FULLY_DISPENSED; BR-PHAR-001 dispense flags + BR-PHAR-002 prescription projection close/decrement; stock borrowed from item-service per dispense (D-1); no events emitted — DispensingCompleted is PROPOSED")
public class ItemSaleController {

    private final ItemSaleService service;

    public ItemSaleController(ItemSaleService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create an item sale with >=1 line",
            description = "patientId required; lines[{itemId,batchId?,qty}] >= 1, qty > 0 integer. "
                    + "Sale starts PENDING; stock is NOT touched until dispense.")
    public ResponseEntity<ItemSale> create(@RequestBody CreateSaleRequest body) {
        List<ItemSaleService.LineInput> lines = body.lines() == null ? null : body.lines().stream()
                .map(l -> new ItemSaleService.LineInput(
                        lng(l.get("itemId")), str(l.get("batchId")), intVal(l.get("qty"))))
                .toList();
        ItemSale created = service.create(body.patientId(), body.prescriptionId(), lines);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Full item sale with lines")
    public ItemSale get(@PathVariable long id) {
        return service.get(id);
    }

    @GetMapping
    @Operation(summary = "List item sales", description = "?status= (1..3 or name PENDING / PARTIALLY_DISPENSED / FULLY_DISPENSED) and/or ?patientId= filter.")
    public List<ItemSale> list(@RequestParam(required = false) String status,
                               @RequestParam(required = false) Long patientId) {
        return service.list(status == null ? null : parseStatus(status), patientId);
    }

    @PostMapping("/{id}/dispense")
    @Operation(summary = "Dispense lines of an item sale (partial allowed)",
            description = "lines[{lineNo,qty}]; qty <= remaining pending_qty per line "
                    + "(qty == pending closes the line). Each qty POSTs a SUBTRACT movement to "
                    + "item-service (single writer); on failure the whole tx rolls back and the "
                    + "sale stays PENDING. All lines closed -> sale.dispensed=true + "
                    + "FULLY_DISPENSED (BR-PHAR-001); linked prescription projection closes/"
                    + "decrements per BR-PHAR-002. Emits NOTHING to the outbox.")
    public ItemSale dispense(@PathVariable long id, @RequestBody DispenseRequest body) {
        List<ItemSaleService.DispenseLine> lines = body.lines() == null ? null : body.lines().stream()
                .map(l -> new ItemSaleService.DispenseLine(
                        lng(l.get("lineNo")), intVal(l.get("qty"))))
                .toList();
        return service.dispense(id, lines);
    }

    // ------------------------------------------------------------------ helpers

    /** Status filter: numeric (1..3) or name (PENDING, PARTIALLY_DISPENSED, FULLY_DISPENSED). */
    static Short parseStatus(String raw) {
        try {
            return Short.valueOf(raw.trim());
        } catch (NumberFormatException ignored) {
            // fall through to name mapping
        }
        return switch (raw.trim().toUpperCase()) {
            case "PENDING" -> 1;
            case "PARTIALLY_DISPENSED" -> 2;
            case "FULLY_DISPENSED" -> 3;
            default -> throw new InvalidSaleRequestException("BEXA003",
                    "Unknown status: " + raw);
        };
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static Long lng(Object v) {
        return v == null ? null : Long.valueOf(String.valueOf(v));
    }

    private static Integer intVal(Object v) {
        return v == null ? null : Integer.valueOf(String.valueOf(v));
    }

    public record CreateSaleRequest(Long patientId, Long prescriptionId,
                                    List<Map<String, Object>> lines) {}

    public record DispenseRequest(List<Map<String, Object>> lines) {}
}
