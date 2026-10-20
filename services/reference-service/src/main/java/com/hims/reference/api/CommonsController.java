package com.hims.reference.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.reference.application.CommonsService;
import com.hims.reference.domain.model.AppConfig;
import com.hims.reference.domain.model.AuditLog;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/commons")
public class CommonsController {
    private final CommonsService service;
    public CommonsController(CommonsService service) { this.service = service; }

    @PutMapping("/config")
    public AppConfig setConfig(@RequestParam(required = false) Long unitId, @RequestBody JsonNode body) {
        return service.setConfig(unitId,
                body.path("key").asText(),
                body.path("value").isMissingNode() || body.path("value").isNull() ? null : body.path("value").asText(),
                body.path("description").isMissingNode() || body.path("description").isNull() ? null : body.path("description").asText());
    }
    @GetMapping("/config/{key}")
    public Map<String, String> getConfig(@PathVariable String key, @RequestParam(required = false) Long unitId) {
        return Map.of("key", key, "value", service.getConfig(unitId, key) == null ? "" : service.getConfig(unitId, key));
    }
    @GetMapping("/config")
    public List<AppConfig> listConfig(@RequestParam(required = false) Long unitId) {
        return service.listConfig(unitId);
    }

    @PostMapping("/audit")
    public ResponseEntity<AuditLog> recordAudit(@RequestBody JsonNode body) {
        final java.util.Map<String, Object>[] diff = new java.util.Map[]{ null };
        if (body.path("diff").isObject()) {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            body.path("diff").fields().forEachRemaining(e -> m.put(e.getKey(), e.getValue().asText()));
            diff[0] = m;
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(service.record(
                body.path("entityType").asText(),
                body.path("entityId").asLong(),
                body.path("action").asText(),
                body.path("actorId").isMissingNode() || body.path("actorId").isNull() || body.path("actorId").asLong(0) == 0 ? null : body.path("actorId").asLong(),
                body.path("actorUsername").isMissingNode() || body.path("actorUsername").isNull() ? null : body.path("actorUsername").asText(),
                diff[0]));
    }
    @GetMapping("/audit/by-entity")
    public List<AuditLog> byEntity(@RequestParam String entityType, @RequestParam Long entityId) {
        return service.findByEntity(entityType, entityId);
    }
    @GetMapping("/audit/by-actor/{actorId}")
    public List<AuditLog> byActor(@PathVariable Long actorId) {
        return service.findByActor(actorId);
    }
}
