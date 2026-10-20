package com.hims.reporting.api;

import com.hims.reporting.application.EventIngressService;
import com.hims.reporting.application.ReportEngineService;
import com.hims.reporting.domain.model.ReportExecution;
import com.hims.reporting.domain.model.ReportMaster;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Report master/execute engine + dashboard counts over the rm_* read models
 * (register WP-T2). The dispatcher is EMULATED inline as JSON results —
 * Jasper/Velocity rendering and regulatory exports (GSTR/NHM/RGHS), NMC/PCTS
 * push jobs are OUT OF SCOPE (comment-only per register). Emits NOTHING.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Reports & Dashboard (WP-T2)", description = "Report master (kind 1=TABULAR 2=SUMMARY) over the rm_* read models; inline dispatcher emulation stores JSONB results (Jasper/regulatory OUT OF SCOPE); dashboard counts {patients, encounters, revenueTotal}. Emits nothing.")
public class ReportController {

    private final ReportEngineService engine;
    private final EventIngressService ingressService;

    public ReportController(ReportEngineService engine, EventIngressService ingressService) {
        this.engine = engine;
        this.ingressService = ingressService;
    }

    // ------------------------------------------------------------------ reports

    @PostMapping("/reports")
    @Operation(summary = "Create a report definition",
            description = "{reportCode, title, kind(1=TABULAR|2=SUMMARY), definition}. "
                    + "409 on duplicate reportCode (REPORTING_DUPLICATE_REPORT_CODE); definition is "
                    + "whitelist-validated against the rm_* tables at create time.")
    public ResponseEntity<ReportMaster> create(@RequestBody CreateReportRequest body) {
        ReportMaster created = engine.create(body.reportCode(), body.title(),
                body.kind() == null ? null : body.kind().shortValue(), body.definition());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/reports")
    @Operation(summary = "List report definitions", description = "?active=true|false filter.")
    public List<ReportMaster> list(@RequestParam(required = false) Boolean active) {
        return engine.list(active);
    }

    @PostMapping("/reports/{code}/execute")
    @Operation(summary = "Execute a report by code",
            description = "{params:{dateFrom?, dateTo?}} -> runs the definition against the read models, "
                    + "stores execution DONE with result JSONB + rowCount (inline dispatcher emulation). "
                    + "Unknown code -> 404.")
    public ReportExecution execute(@PathVariable String code,
                                   @RequestBody ExecuteReportRequest body) {
        return engine.execute(code, body.params());
    }

    @GetMapping("/reports/executions/{id}")
    @Operation(summary = "Fetch one execution")
    public ReportExecution execution(@PathVariable long id) {
        return engine.getExecution(id);
    }

    @GetMapping("/reports/executions")
    @Operation(summary = "List executions",
            description = "?reportCode= and/or ?status= (0..2 or QUEUED/DONE/FAILED).")
    public List<ReportExecution> executions(@RequestParam(required = false) String reportCode,
                                            @RequestParam(required = false) String status) {
        return engine.listExecutions(reportCode, status);
    }

    // ------------------------------------------------------------------ dashboard

    @GetMapping("/dashboard/counts")
    @Operation(summary = "Dashboard counts from the read models",
            description = "{patients, encounters, revenueTotal}; optional ?asOf=yyyy-MM-dd bounds "
                    + "stat_date <= asOf for encounters/revenue.")
    public Map<String, Object> dashboardCounts(
            @RequestParam(name = "asOf", required = false) String asOfRaw) {
        LocalDate asOf = EventIngressService.parseDateStrict(asOfRaw, "asOf");
        return engine.dashboardCounts(asOf);
    }

    public record CreateReportRequest(String reportCode, String title, Integer kind,
                                      Map<String, Object> definition) {}

    public record ExecuteReportRequest(Map<String, Object> params) {}
}
