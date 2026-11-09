package com.hims.kiosk.api;

import com.hims.kiosk.application.KioskService;
import com.hims.kiosk.domain.model.KioskDepartment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/kiosk/departments")
@Tag(name = "Kiosk Departments (M31)")
public class KioskDepartmentController {

    private final KioskService service;

    public KioskDepartmentController(KioskService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List active departments ordered by name")
    public List<KioskDepartment> listDepartments() {
        return service.listActiveDepartments();
    }
}
