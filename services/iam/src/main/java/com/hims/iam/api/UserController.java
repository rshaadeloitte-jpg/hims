package com.hims.iam.api;

import com.hims.iam.api.dto.UserDtos.AssignRolesRequest;
import com.hims.iam.api.dto.UserDtos.CreateUserRequest;
import com.hims.iam.api.dto.UserDtos.UpdateUserRequest;
import com.hims.iam.api.dto.UserDtos.UserView;
import com.hims.iam.application.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

/** User CRUD API (Catalog #22 — usermanagement slice). Passwords are BCrypt-hashed server-side and never returned. */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User master CRUD and per-user role assignment")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "List active users (optional unitId / role scope)")
    public List<UserView> list(@RequestParam(required = false) Long unitId,
                               @RequestParam(required = false) Long roleId) {
        return userService.list(unitId, roleId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a user by id")
    public UserView get(@PathVariable long id) {
        return userService.get(id);
    }

    @PostMapping
    @Operation(summary = "Create a user (password stored as BCrypt hash; optional initial roleIds)")
    public ResponseEntity<UserView> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user profile fields / status (password changes are out of scope here)")
    public UserView update(@PathVariable long id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @PutMapping("/{id}/roles")
    @Operation(summary = "Replace the user's role grants with the given roleIds")
    public UserView assignRoles(@PathVariable long id, @RequestBody AssignRolesRequest request) {
        return userService.assignRoles(id, request);
    }
}
