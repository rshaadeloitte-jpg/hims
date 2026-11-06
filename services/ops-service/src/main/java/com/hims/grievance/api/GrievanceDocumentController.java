package com.hims.grievance.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.grievance.application.GrievanceDocumentService;
import com.hims.grievance.domain.model.GrievanceDocument;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/grievances")
public class GrievanceDocumentController {
    private final GrievanceDocumentService service;
    public GrievanceDocumentController(GrievanceDocumentService service) { this.service = service; }

    @PostMapping("/{id}/documents")
    public ResponseEntity<GrievanceDocument> upload(@PathVariable long id, @RequestBody JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.upload(
                id,
                body == null ? null : (body.path("userId").isMissingNode() || body.path("userId").isNull() || body.path("userId").asLong(0) == 0 ? null : body.path("userId").asLong()),
                body == null ? null : body.path("fileName").asText()));
    }
    @GetMapping("/{id}/documents")
    public List<GrievanceDocument> list(@PathVariable long id) { return service.listForTicket(id); }
}
