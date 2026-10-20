package com.hims.pharmacy.api;

import com.hims.pharmacy.application.DrugMappingService;
import com.hims.pharmacy.domain.model.DrugMapping;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/drug-mappings")
@Tag(name = "Drug Mapping", description = "Drug-to-item master mapping with MRP/tax, generic/brand lookup")
public class DrugMappingController {
    private final DrugMappingService service;
    public DrugMappingController(DrugMappingService service) { this.service = service; }

    @PostMapping
    @Operation(summary = "Create drug mapping")
    public ResponseEntity<DrugMapping> create(@RequestBody Map<String,Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(
                lng(body.get("itemId")), str(body.get("itemName")), str(body.get("genericName")),
                str(body.get("brandName")), str(body.get("category")), str(body.get("strength")),
                str(body.get("uom")), bigDecimal(body.get("mrp")), bigDecimal(body.get("taxPercent")),
                lng(body.get("userId"))));
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<DrugMapping> getByItem(@PathVariable Long itemId) {
        return service.getByItem(itemId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/active")
    public List<DrugMapping> active() { return service.listActive(); }

    @GetMapping("/search")
    public List<DrugMapping> search(@RequestParam String q) { return service.searchGeneric(q); }

    private Long lng(Object o) { return o == null ? null : Long.valueOf(o.toString()); }
    private String str(Object o) { return o == null ? null : o.toString(); }
    private BigDecimal bigDecimal(Object o) { return o == null ? null : new BigDecimal(o.toString()); }
}
