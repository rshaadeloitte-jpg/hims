package com.hims.item.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.item.application.QuotationService;
import com.hims.item.application.QuotationService.Line;
import com.hims.item.domain.model.Quotation;
import com.hims.item.domain.model.QuotationDetail;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/quotations")
public class QuotationController {
    private final QuotationService service;
    public QuotationController(QuotationService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<Quotation> create(@RequestBody JsonNode body) {
        List<Line> lines = new ArrayList<>();
        for (JsonNode l : body.path("lines")) lines.add(QuotationService.toLine(l));
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(
                longOrNull(body, "supplierId"), textOrNull(body, "enquiryRef"),
                textOrNull(body, "validUntil"), textOrNull(body, "remarks"), lines));
    }
    @GetMapping public List<Quotation> list(@RequestParam(required = false) Long supplierId,
                                            @RequestParam(required = false) Short status) {
        return service.list(supplierId, status);
    }
    @GetMapping("/{id}") public Quotation get(@PathVariable long id) { return service.get(id); }
    @GetMapping("/{id}/lines") public List<QuotationDetail> lines(@PathVariable long id) { return service.details(id); }
    @PostMapping("/{id}/submit") public Quotation submit(@PathVariable long id) { return service.submit(id); }
    @PostMapping("/{id}/reject") public Quotation reject(@PathVariable long id) { return service.reject(id); }
    @PostMapping("/{id}/expire") public Quotation expire(@PathVariable long id) { return service.expire(id); }
    @GetMapping("/compare") public List<QuotationService.CompareRow> compare(@RequestParam Long itemId) {
        return service.compare(itemId);
    }

    private static String textOrNull(JsonNode n, String f) { JsonNode v = n.path(f); return v.isMissingNode()||v.isNull()?null:v.asText(); }
    private static Long longOrNull(JsonNode n, String f) { JsonNode v = n.path(f); return v.isMissingNode()||v.isNull()||v.asLong(0)==0?null:v.asLong(); }
}
