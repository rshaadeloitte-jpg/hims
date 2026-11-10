package com.hims.payments.domain.repository;

import com.hims.payments.domain.model.DbtClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DbtClaimRepository extends JpaRepository<DbtClaim, Long> {
}
