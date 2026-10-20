package com.hims.iam.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;

/** Auth request/response contracts for /api/v1/auth/*. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds,
                                long userId, String username, List<String> roles) {
    }

    public record LogoutRequest(String token) {
    }

    public record ValidateRequest(String token) {
    }

    public record ValidateResponse(boolean valid, String username, Long userId,
                                   List<String> roles, Instant expiresAt, String reason) {
    }
}
