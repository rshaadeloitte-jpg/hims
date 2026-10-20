package com.hims.adt.api;

import com.hims.adt.application.AdmissionService;
import com.hims.adt.domain.model.Admission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/adt")
@Tag(name = "ADT Ops (M03)", description = "Census, TID list, death list (M03 nursing station census, TID, death handling)")
public class AdtOpsController {

    private final AdmissionService service;

    public AdtOpsController(AdmissionService service) {
        this.service = service;
    }

    @GetMapping("/census")
    @Operation(summary = "Nursing station census (M03)",
            description = "Per-ward bed counts (total/occupied/free/cleaning) + active admissions; iscensus flag respected. Interim unbedded count included.")
    public Map<String, Object> census(@RequestParam(required = false) Long wardId) {
        return service.census(wardId);
    }

    @GetMapping("/tids")
    @Operation(summary = "TID list (M03)",
            description = "Admissions with external TID (RGHS/TPA) set.")
    public List<Admission> tids() {
        return service.tidList();
    }

    @GetMapping("/deaths")
    @Operation(summary = "Death list (M03 death handling)",
            description = "Discharged admissions where is_expired=true, ordered by dischargeDate desc.")
    public List<Admission> deaths() {
        return service.deathList();
    }
}
