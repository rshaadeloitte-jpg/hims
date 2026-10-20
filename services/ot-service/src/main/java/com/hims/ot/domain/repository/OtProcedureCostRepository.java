package com.hims.ot.domain.repository;

import com.hims.ot.domain.model.OtProcedureCost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OtProcedureCostRepository extends JpaRepository<OtProcedureCost, Long> {
    List<OtProcedureCost> findByEstimateId(Long estimateId);
}
