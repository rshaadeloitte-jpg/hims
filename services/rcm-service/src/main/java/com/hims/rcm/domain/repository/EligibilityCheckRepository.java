package com.hims.rcm.domain.repository;

import com.hims.rcm.domain.model.EligibilityCheck;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EligibilityCheckRepository extends JpaRepository<EligibilityCheck, Long> {
}
