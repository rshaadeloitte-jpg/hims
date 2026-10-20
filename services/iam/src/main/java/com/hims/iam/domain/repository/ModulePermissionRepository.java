package com.hims.iam.domain.repository;

import com.hims.iam.domain.model.ModulePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ModulePermissionRepository extends JpaRepository<ModulePermission, Long> {
    Optional<ModulePermission> findByCode(String code);
    boolean existsByCode(String code);
}
