package com.hims.iam.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** User management request/response contracts for /api/v1/users/*. */
public final class UserDtos {

    private UserDtos() {
    }

    public record CreateUserRequest(@NotBlank @Size(max = 100) String username,
                                    @NotBlank @Size(min = 8, max = 72) String password,
                                    @Size(max = 255) String email,
                                    @Size(max = 255) String fullName,
                                    Long employeeId,
                                    Long unitId,
                                    List<Long> roleIds) {
    }

    public record UpdateUserRequest(@Size(max = 255) String email,
                                    @Size(max = 255) String fullName,
                                    Long employeeId,
                                    Long unitId,
                                    Long storeId,
                                    Long wardId,
                                    Long departmentId,
                                    Boolean status) {
    }

    public record AssignRolesRequest(List<Long> roleIds) {
    }

    /** Never exposes password_hash. */
    public record UserView(Long id, String username, String email, String fullName,
                           Long employeeId, Long unitId, Long storeId, Long wardId, Long departmentId,
                           boolean status,
                           List<Long> roleIds, List<String> roles) {
    }
}
