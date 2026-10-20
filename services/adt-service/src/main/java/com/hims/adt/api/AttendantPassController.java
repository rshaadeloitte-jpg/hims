package com.hims.adt.api;

import com.hims.adt.application.AdmissionService;
import com.hims.adt.domain.model.AttendantPass;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/attendant-passes")
@Tag(name = "Attendant Pass (M03)", description = "Attendant pass issue/return/reprint (T_ATTENDANTPASSDETAILS parity)")
public class AttendantPassController {

    private final AdmissionService service;

    public AttendantPassController(AdmissionService service) {
        this.service = service;
    }

    @PostMapping("/{passId}/return")
    @Operation(summary = "Return an issued pass (ISSUED -> RETURNED)")
    public AttendantPass returnPass(@PathVariable long passId) {
        return service.returnAttendantPass(passId);
    }

    @PostMapping("/{passId}/reprint")
    @Operation(summary = "Reprint an attendant pass (bump printCount)")
    public AttendantPass reprint(@PathVariable long passId) {
        return service.reprintAttendantPass(passId);
    }
}
