package com.hims.lab.api;

import com.hims.lab.application.KdlService;
import com.hims.lab.domain.model.KdlResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * KDL/machine interface API (GAP-REGISTER WP-7 HIGH).
 * Parses raw instrument payloads and maps results to patho details.
 */
@RestController
@RequestMapping("/api/v1/kdl")
@Tag(name = "KDL Interface (WP-7 HIGH)", description = "Machine/analyser protocol parsing (HL7/ASTM/file-drop) and result mapping to patho details")
public class KdlController {

    private final KdlService service;

    public KdlController(KdlService service) {
        this.service = service;
    }

    @PostMapping("/results")
    @Operation(summary = "Parse and store a KDL/machine result payload")
    public ResponseEntity<KdlResult> parseAndStore(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.parseAndStore(
                        longVal(body, "machineId"),
                        str(body, "protocol"),
                        str(body, "rawPayload"),
                        str(body, "labNumber"),
                        str(body, "mrnNo"),
                        str(body, "patientName"),
                        str(body, "testName"),
                        str(body, "sampleStatus")));
    }

    @PostMapping("/results/{id}/map")
    @Operation(summary = "Map an unmapped KDL result to a patho detail")
    public KdlResult mapToDetail(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return service.mapToDetail(id, longVal(body, "detailId"));
    }

    @GetMapping("/results/unmapped")
    @Operation(summary = "List unmapped KDL results (pending mapping)")
    public List<KdlResult> listUnmapped() {
        return service.listUnmapped();
    }

    @GetMapping("/results")
    @Operation(summary = "List KDL results by machine and/or MRN",
               description = "?machineId= and/or ?mrnNo= filter; returns all if no filter")
    public List<KdlResult> list(@RequestParam(required = false) Long machineId,
                                 @RequestParam(required = false) String mrnNo) {
        if (machineId != null) return service.listByMachine(machineId);
        if (mrnNo != null) return service.listByMrn(mrnNo);
        return service.listUnmapped();
    }

    @GetMapping("/results/{id}")
    @Operation(summary = "Get a KDL result by id")
    public KdlResult get(@PathVariable long id) {
        return service.get(id);
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
}
