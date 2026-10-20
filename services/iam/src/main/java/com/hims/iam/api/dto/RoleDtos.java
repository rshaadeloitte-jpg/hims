package com.hims.iam.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Role management request/response contracts for /api/v1/roles. */
public final class RoleDtos {

    private RoleDtos() {
    }

    public record CreateRoleRequest(@NotBlank @Size(max = 64) String code,
                                    @Size(max = 255) String description,
                                    Long unitId) {
    }

    public record RoleView(Long id, String code, String description, Long unitId, boolean status) {
    }
}
