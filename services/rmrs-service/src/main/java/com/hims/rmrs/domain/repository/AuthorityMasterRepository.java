package com.hims.rmrs.domain.repository;

import com.hims.rmrs.domain.model.AuthorityMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthorityMasterRepository extends JpaRepository<AuthorityMaster, Long> {

    Optional<AuthorityMaster> findByAuthorityCode(String authorityCode);

    boolean existsByAuthorityCode(String authorityCode);
}
