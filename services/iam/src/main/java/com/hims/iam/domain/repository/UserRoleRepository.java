package com.hims.iam.domain.repository;

import com.hims.iam.domain.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    List<UserRole> findByUserIdAndStatusTrue(Long userId);
}
