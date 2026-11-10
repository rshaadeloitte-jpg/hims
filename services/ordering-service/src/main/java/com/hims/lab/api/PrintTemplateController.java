package com.hims.lab.api;

import com.hims.lab.application.PrintTemplateService;
import com.hims.lab.domain.model.ReportPrintTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Report print template API (GAP-REGISTER WP-7 HIGH).
 * Manages 201 legacy print templates (REPORT/LABEL/RECEIPT/BARCODE).
 */
@RestController
@RequestMapping("/api/v1/print-templates")
@Tag(name = "Print Templates (WP-7 HIGH)", description = "Report print template storage and retrieval (201 legacy templates)")
public class PrintTemplateController {

    private final PrintTemplateService service;

    public PrintTemplateController(PrintTemplateService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create a print template")
    public ResponseEntity<ReportPrintTemplate> create(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(
                        str(body, "code"),
                        str(body, "description"),
                        str(body, "templateContent"),
                        null, // blob via multipart in real impl; JSON base64 for now
                        longVal(body, "genderId"),
                        longVal(body, "serviceId"),
                        longVal(body, "organId"),
                        bool(body, "isClinical"),
                        str(body, "templateType")));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a print template")
    public ReportPrintTemplate update(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return service.update(id,
                str(body, "description"),
                str(body, "templateContent"),
                null,
                longVal(body, "genderId"),
                longVal(body, "serviceId"),
                longVal(body, "organId"),
                bool(body, "isClinical"),
                str(body, "templateType"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a template by id")
    public ReportPrintTemplate get(@PathVariable long id) {
        return service.get(id);
    }

    @GetMapping("/by-code/{code}")
    @Operation(summary = "Get a template by code")
    public ReportPrintTemplate getByCode(@PathVariable String code) {
        return service.getByCode(code);
    }

    @GetMapping
    @Operation(summary = "List templates",
               description = "?type=REPORT|LABEL|RECEIPT|BARCODE&genderId=&serviceId= filter")
    public List<ReportPrintTemplate> list(@RequestParam(required = false) String type,
                                           @RequestParam(required = false) Long genderId,
                                           @RequestParam(required = false) Long serviceId) {
        if (type != null) return service.listByType(type);
        if (genderId != null) return service.listByGender(genderId);
        if (serviceId != null) return service.listByService(serviceId);
        return service.listAll();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a template")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private static String str(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private static Long longVal(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        return Long.valueOf(String.valueOf(v));
    }

    private static Boolean bool(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Boolean b) return b;
        return Boolean.valueOf(String.valueOf(v));
    }
}
