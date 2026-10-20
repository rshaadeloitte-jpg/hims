package com.hims.iam.domain.repository;

import com.hims.iam.domain.model.UserRoleMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRoleMasterRepository extends JpaRepository<UserRoleMaster, Long> {

    Optional<UserRoleMaster> findByCodeIgnoreCase(String code);

    List<UserRoleMaster> findByStatusTrueOrderByIdAsc();
}
