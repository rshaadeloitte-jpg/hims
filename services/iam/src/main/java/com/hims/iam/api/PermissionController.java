package com.hims.iam.api;

import com.hims.iam.application.PermissionService;
import com.hims.iam.domain.model.ModulePermission;
import com.hims.iam.domain.model.RolePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import java.util.Set;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Permissions (M35)", description = "Module/menu permissions: catalogue + role/user grants (ported from RoleBasedPermission)")
public class PermissionController {

    private final PermissionService service;

    public PermissionController(PermissionService service) {
        this.service = service;
    }

    // ------------------------------------------------------------------ catalogue

    @GetMapping("/permissions")
    @Operation(summary = "List permissions (?module= filter)")
    public List<ModulePermission> list(@RequestParam(required = false) String module) {
        return service.listPermissions(module);
    }

    @PostMapping("/permissions")
    @Operation(summary = "Create a permission (unique code)")
    public ResponseEntity<ModulePermission> create(@RequestBody Map<String, String> body) {
        ModulePermission p = service.createPermission(body.get("code"), body.get("module"), body.get("description"));
        return ResponseEntity.status(HttpStatus.CREATED).body(p);
    }

    @PutMapping("/permissions/{id}")
    @Operation(summary = "Update a permission")
    public ModulePermission update(@PathVariable long id, @RequestBody Map<String, String> body) {
        return service.updatePermission(id, body.get("code"), body.get("module"), body.get("description"));
    }

    // ------------------------------------------------------------------ grants

    @PutMapping("/roles/{roleId}/permissions")
    @Operation(summary = "Set permissions for a role (replaces existing set)")
    public RolePermission setRolePermissions(@PathVariable long roleId, @RequestBody Map<String, Object> body) {
        Set<Long> ids = toIds(body.get("permissionIds"));
        return service.setRolePermissions(roleId, ids);
    }

    @GetMapping("/roles/{roleId}/permissions")
    @Operation(summary = "Get permissions for a role")
    public Set<Long> getRolePermissions(@PathVariable long roleId) {
        return service.getRolePermissions(roleId);
    }

    @PutMapping("/users/{userId}/permissions")
    @Operation(summary = "Set permissions for a user (override, replaces; null/empty clears override)")
    public RolePermission setUserPermissions(@PathVariable long userId, @RequestBody Map<String, Object> body) {
        Set<Long> ids = toIds(body.get("permissionIds"));
        return service.setUserPermissions(userId, ids);
    }

    @GetMapping("/users/{userId}/permissions")
    @Operation(summary = "Get effective permissions for a user (direct override or union of role grants)")
    public Set<Long> getUserPermissions(@PathVariable long userId) {
        return service.getUserEffectivePermissions(userId);
    }

    @SuppressWarnings("unchecked")
    private static Set<Long> toIds(Object raw) {
        if (raw == null) return Set.of();
        if (raw instanceof List<?> list) {
            return new java.util.LinkedHashSet<>(list.stream()
                    .map(v -> Long.valueOf(String.valueOf(v))).toList());
        }
        return Set.of();
    }
}
