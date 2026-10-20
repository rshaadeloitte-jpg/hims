package com.hims.item.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.item.application.RateContractService;
import com.hims.item.domain.model.RateContract;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/inventory/rate-contracts")
public class RateContractController {
    private final RateContractService service;
    public RateContractController(RateContractService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<RateContract> create(@RequestBody JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(
                longOrNull(body, "supplierId"), longOrNull(body, "itemId"),
                decimalOrNull(body, "contractPrice"), decimalOrNull(body, "taxPercent"),
                decimalOrNull(body, "minOrderQty"),
                textOrNull(body, "validFrom"), textOrNull(body, "validTo"),
                textOrNull(body, "remarks")));
    }
    @GetMapping public List<RateContract> list(@RequestParam(required = false) Long supplierId,
                                               @RequestParam(required = false) Long itemId) {
        return service.list(supplierId, itemId);
    }
    @GetMapping("/{id}") public RateContract get(@PathVariable long id) { return service.get(id); }
    @PostMapping("/{id}/cancel") public RateContract cancel(@PathVariable long id) { return service.cancel(id); }
    @GetMapping("/lookup") public Optional<RateContract> lookup(@RequestParam Long supplierId,
                                                              @RequestParam Long itemId,
                                                              @RequestParam(required = false) String onDate) {
        return service.lookupActive(supplierId, itemId, onDate);
    }

    private static String textOrNull(JsonNode n, String f) { JsonNode v = n.path(f); return v.isMissingNode()||v.isNull()?null:v.asText(); }
    private static Long longOrNull(JsonNode n, String f) { JsonNode v = n.path(f); return v.isMissingNode()||v.isNull()||v.asLong(0)==0?null:v.asLong(); }
    private static BigDecimal decimalOrNull(JsonNode n, String f) { JsonNode v = n.path(f); return v.isMissingNode()||v.isNull()?null:v.decimalValue(); }
}
