package com.hims.iam.api;

import com.hims.iam.api.dto.AuthDtos.LoginRequest;
import com.hims.iam.api.dto.AuthDtos.LoginResponse;
import com.hims.iam.api.dto.AuthDtos.LogoutRequest;
import com.hims.iam.api.dto.AuthDtos.ValidateRequest;
import com.hims.iam.api.dto.AuthDtos.ValidateResponse;
import com.hims.iam.application.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Authentication API (Catalog #22): POST /api/v1/auth/login issues an HS256
 * JWT access token signed with the server-side secret; logout adds the token
 * to the revoke list; validate introspects signature/expiry/revocation.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Login (JWT issue), logout (token revoke), validate (token introspection)")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate a user and issue a JWT access token (HS256)")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the presented bearer token (server-side revoke list)")
    public ResponseEntity<Map<String, Object>> logout(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody(required = false) LogoutRequest body) {
        String token = null;
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            token = authorizationHeader.substring(7).trim();
        } else if (body != null && body.token() != null && !body.token().isBlank()) {
            token = body.token().trim();
        }
        authService.logout(token);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Token revoked"));
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate a JWT access token (signature, expiry, revocation)")
    public ResponseEntity<ValidateResponse> validate(@RequestBody(required = false) ValidateRequest body,
                                                     @RequestHeader(value = "Authorization", required = false)
                                                     String authorizationHeader) {
        String token = null;
        if (body != null && body.token() != null && !body.token().isBlank()) {
            token = body.token().trim();
        } else if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            token = authorizationHeader.substring(7).trim();
        }
        return ResponseEntity.ok(authService.validate(token));
    }
}
