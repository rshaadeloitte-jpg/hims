package com.hims.iam.domain.repository;

import com.hims.iam.domain.model.UserDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserDetailRepository extends JpaRepository<UserDetail, Long> {

    Optional<UserDetail> findByUsernameIgnoreCase(String username);

    List<UserDetail> findByStatusTrueOrderByIdAsc();

    List<UserDetail> findByStatusTrueAndUnitIdOrderByIdAsc(Long unitId);
}
