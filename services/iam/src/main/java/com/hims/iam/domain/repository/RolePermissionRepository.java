package com.hims.iam.domain.repository;

import com.hims.iam.domain.model.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    Optional<RolePermission> findByRoleId(Long roleId);
    Optional<RolePermission> findByUserId(Long userId);
}
