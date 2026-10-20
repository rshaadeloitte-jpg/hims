package com.hims.ot.domain.repository;

import com.hims.ot.domain.model.OtBillingEstimate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OtBillingEstimateRepository extends JpaRepository<OtBillingEstimate, Long> {
}
