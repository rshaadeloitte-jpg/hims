package com.hims.iam.api;

import com.hims.iam.api.dto.RoleDtos.CreateRoleRequest;
import com.hims.iam.api.dto.RoleDtos.RoleView;
import com.hims.iam.application.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Role management API (Catalog #22 — T_USERROLEMASTER slice). */
@RestController
@RequestMapping("/api/v1/roles")
@Tag(name = "Roles", description = "Role catalogue: list and create roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @Operation(summary = "List active roles")
    public List<RoleView> list() {
        return roleService.list();
    }

    @PostMapping
    @Operation(summary = "Create a role (unique code)")
    public ResponseEntity<RoleView> create(@Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.create(request));
    }
}
