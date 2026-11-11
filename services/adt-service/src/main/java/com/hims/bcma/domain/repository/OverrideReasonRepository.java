package com.hims.bcma.domain.repository;

import com.hims.bcma.domain.model.OverrideReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OverrideReasonRepository extends JpaRepository<OverrideReason, Long> {
    Optional<OverrideReason> findByReasonId(Long reasonId);
    Optional<OverrideReason> findByCode(String code);
}
