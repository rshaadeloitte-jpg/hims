package com.hims.reference.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.reference.application.ConcessionTemplateService;
import com.hims.reference.domain.model.ConcessionTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/masters/concession-templates")
public class ConcessionTemplateController {
    private final ConcessionTemplateService service;
    public ConcessionTemplateController(ConcessionTemplateService service) { this.service = service; }
    @PostMapping
    public ResponseEntity<ConcessionTemplate> create(@RequestBody JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(
                textOrThrow(body, "code"), textOrThrow(body, "name"),
                textOrThrow(body, "scheme"), textOrNull(body, "description"),
                decimalOrThrow(body, "maxPercentOff"),
                body == null ? null : body.path("requiresApproval").asBoolean(false)));
    }
    @GetMapping public List<ConcessionTemplate> list(@RequestParam(required = false) String scheme,
                                                   @RequestParam(required = false) Short status) {
        return service.list(scheme, status);
    }
    @GetMapping("/{id}") public ConcessionTemplate get(@PathVariable long id) { return service.get(id); }
    @PostMapping("/{id}/deactivate") public ConcessionTemplate deactivate(@PathVariable long id) { return service.deactivate(id); }

    private static String textOrThrow(JsonNode n, String f) { JsonNode v = n.path(f); if (v.isMissingNode()||v.isNull()) throw new com.hims.reference.application.exceptions.InvalidMasterRequestException("BEXA003", f + " is required"); return v.asText(); }
    private static String textOrNull(JsonNode n, String f) { JsonNode v = n.path(f); return v.isMissingNode()||v.isNull()?null:v.asText(); }
    private static BigDecimal decimalOrThrow(JsonNode n, String f) { JsonNode v = n.path(f); if (v.isMissingNode()||v.isNull()) throw new com.hims.reference.application.exceptions.InvalidMasterRequestException("BEXA003", f + " is required"); return v.decimalValue(); }
}
