package com.hims.interop.api;

import com.hims.interop.application.EhrJobQueryService;
import com.hims.interop.domain.model.EhrJobDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * EHR job read-model query API (register WP-T1). PHASE-1 NOTE: no write path —
 * job creation rides the OUT-OF-SCOPE dispatcher slice (comment-only).
 */
@RestController
@RequestMapping("/api/v1/health/jobs")
@Tag(name = "EHR jobs (WP-T1)", description = "t_ehr_job_details read model: status 0=QUEUED 1=RUNNING 2=DONE 3=FAILED. No phase-1 write path (dispatcher slice OUT OF SCOPE, disclosed).")
public class HealthJobController {

    private final EhrJobQueryService service;

    public HealthJobController(EhrJobQueryService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List EHR jobs", description = "Optional ?status= (0..3) filter; newest first.")
    public List<EhrJobDetails> list(@RequestParam(required = false) Short status) {
        if (status != null && (status < EhrJobDetails.STATUS_QUEUED || status > EhrJobDetails.STATUS_FAILED)) {
            throw new com.hims.interop.application.exceptions.InvalidRequestException("BEXA003",
                    "status must be within 0..3");
        }
        return service.list(status);
    }
}
