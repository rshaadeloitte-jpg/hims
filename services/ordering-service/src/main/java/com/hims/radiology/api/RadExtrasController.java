package com.hims.radiology.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.radiology.application.RadOrderService;
import com.hims.radiology.application.RadTemplateService;
import com.hims.radiology.domain.model.RadOrderDetail;
import com.hims.radiology.domain.model.RadTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/radiology")
public class RadExtrasController {
    private final RadOrderService orderService;
    private final RadTemplateService templateService;
    public RadExtrasController(RadOrderService orderService, RadTemplateService templateService) {
        this.orderService = orderService; this.templateService = templateService;
    }

    @PostMapping("/orders/{orderId}/lines/{lineNo}/retest")
    public RadOrderDetail requestRetest(@PathVariable long orderId, @PathVariable long lineNo, @RequestBody(required = false) JsonNode body) {
        return orderService.requestRetest(orderId, lineNo, body == null ? null : body.path("reason").asText());
    }

    @PostMapping("/orders/{orderId}/lines/{lineNo}/reschedule")
public RadOrderDetail reschedule(@PathVariable long orderId, @PathVariable long lineNo, @RequestBody JsonNode body) {
        return orderService.reschedule(orderId, lineNo, java.sql.Timestamp.valueOf(body.path("rescheduledTo").asText().replace("T", " ").replace("Z", "")));
    }

    @PostMapping("/templates")
    public ResponseEntity<RadTemplate> createTemplate(@RequestBody JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.create(
                body.path("code").asText(),
                body.path("modality").asText(),
                body.path("body").asText(),
                body.path("description").isMissingNode() || body.path("description").isNull() ? null : body.path("description").asText()));
    }
    @GetMapping("/templates")
    public List<RadTemplate> listTemplates(@RequestParam(required = false) String modality) {
        return templateService.list(modality);
    }
    @GetMapping("/templates/{id}")
    public RadTemplate getTemplate(@PathVariable long id) { return templateService.get(id); }
}
