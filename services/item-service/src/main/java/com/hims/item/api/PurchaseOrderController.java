package com.hims.item.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.item.application.PurchaseOrderService;
import com.hims.item.application.PurchaseOrderService.Line;
import com.hims.item.domain.model.PurchaseOrder;
import com.hims.item.domain.model.PurchaseOrderDetail;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/purchase-orders")
public class PurchaseOrderController {
    private final PurchaseOrderService service;
    public PurchaseOrderController(PurchaseOrderService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<PurchaseOrder> create(@RequestBody JsonNode body) {
        List<Line> lines = new ArrayList<>();
        for (JsonNode l : body.path("lines")) lines.add(PurchaseOrderService.toLine(l));
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(
                longOrNull(body, "storeId"), longOrNull(body, "supplierId"), longOrNull(body, "quotationId"),
                textOrNull(body, "orderDate"), textOrNull(body, "expectedDate"),
                textOrNull(body, "remarks"), lines));
    }
    @GetMapping public List<PurchaseOrder> list(@RequestParam(required = false) Long storeId,
                                               @RequestParam(required = false) Long supplierId,
                                               @RequestParam(required = false) Short status) {
        return service.list(storeId, supplierId, status);
    }
    @GetMapping("/{id}") public PurchaseOrder get(@PathVariable long id) { return service.get(id); }
    @GetMapping("/{id}/lines") public List<PurchaseOrderDetail> lines(@PathVariable long id) { return service.details(id); }
    @PostMapping("/{id}/submit") public PurchaseOrder submit(@PathVariable long id) { return service.submit(id); }
    @PostMapping("/{id}/approve") public PurchaseOrder approve(@PathVariable long id) { return service.approve(id, null); }
    @PostMapping("/{id}/reject") public PurchaseOrder reject(@PathVariable long id) { return service.reject(id); }
    @PostMapping("/{id}/issue") public PurchaseOrder issue(@PathVariable long id) { return service.issue(id); }
    @PostMapping("/{id}/cancel") public PurchaseOrder cancel(@PathVariable long id) { return service.cancel(id); }

    private static String textOrNull(JsonNode n, String f) { JsonNode v = n.path(f); return v.isMissingNode()||v.isNull()?null:v.asText(); }
    private static Long longOrNull(JsonNode n, String f) { JsonNode v = n.path(f); return v.isMissingNode()||v.isNull()||v.asLong(0)==0?null:v.asLong(); }
}
