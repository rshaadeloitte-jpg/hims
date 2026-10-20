package com.hims.pharmacy.api;

import com.hims.pharmacy.application.WardPicklistService;
import com.hims.pharmacy.domain.model.WardPicklist;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Ward pick lists (register WP-R2): {wardId, saleId} -> picked_at=now. */
@RestController
@RequestMapping("/api/v1/picklists")
@Tag(name = "Ward Picklists (WP-R2)", description = "Pick list rows tying a ward to an item sale; picked_at stamped server-side")
public class PicklistController {

    private final WardPicklistService service;

    public PicklistController(WardPicklistService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create a ward pick list row",
            description = "wardId and saleId required (400 BEXA003); unknown sale -> 404 envelope.")
    public ResponseEntity<WardPicklist> create(@RequestBody Map<String, Object> body) {
        Object wardId = body.get("wardId");
        Object saleId = body.get("saleId");
        WardPicklist created = service.create(
                wardId == null ? null : Long.valueOf(String.valueOf(wardId)),
                saleId == null ? null : Long.valueOf(String.valueOf(saleId)));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
