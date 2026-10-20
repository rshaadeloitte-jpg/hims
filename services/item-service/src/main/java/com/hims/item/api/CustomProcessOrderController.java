package com.hims.item.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.item.application.CustomProcessOrderService;
import com.hims.item.application.CustomProcessOrderService.CpoLine;
import com.hims.item.application.CustomProcessOrderService.ReceiveLine;
import com.hims.item.domain.model.CustomProcessOrder;
import com.hims.item.domain.model.CustomProcessOrderDetail;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/custom-process-orders")
public class CustomProcessOrderController {
    private final CustomProcessOrderService service;
    public CustomProcessOrderController(CustomProcessOrderService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<CustomProcessOrder> create(@RequestBody JsonNode body) {
        List<CpoLine> lines = new ArrayList<>();
        for (JsonNode l : body.path("lines")) lines.add(CustomProcessOrderService.toLine(l));
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(
                longOrNull(body, "storeId"), longOrNull(body, "supplierId"), longOrNull(body, "deptId"),
                textOrNull(body, "requestDate"), textOrNull(body, "requiredDate"),
                textOrNull(body, "remarks"), lines));
    }

    @GetMapping
    public List<CustomProcessOrder> list(@RequestParam(required = false) Long storeId,
                                         @RequestParam(required = false) Short status) {
        return service.list(storeId, status);
    }

    @GetMapping("/{id}")
    public CustomProcessOrder get(@PathVariable long id) { return service.get(id); }

    @GetMapping("/{id}/lines")
    public List<CustomProcessOrderDetail> lines(@PathVariable long id) { return service.details(id); }

    @PostMapping("/{id}/approve")
    public CustomProcessOrder approve(@PathVariable long id) { return service.approve(id, null); }

    @PostMapping("/{id}/cancel")
    public CustomProcessOrder cancel(@PathVariable long id) { return service.cancel(id); }

    @PostMapping("/{id}/receive")
    public CustomProcessOrder receive(@PathVariable long id, @RequestBody JsonNode body) {
        List<ReceiveLine> lines = new ArrayList<>();
        for (JsonNode l : body.path("lines")) lines.add(CustomProcessOrderService.toReceive(l));
        return service.receive(id, lines);
    }

    private static String textOrNull(JsonNode n, String f) {
        JsonNode v = n.path(f);
        return v.isMissingNode() || v.isNull() ? null : v.asText();
    }
    private static Long longOrNull(JsonNode n, String f) {
        JsonNode v = n.path(f);
        return v.isMissingNode() || v.isNull() || v.asLong(0) == 0 ? null : v.asLong();
    }
}
