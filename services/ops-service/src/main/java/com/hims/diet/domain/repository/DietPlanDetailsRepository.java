package com.hims.diet.domain.repository;

import com.hims.diet.domain.model.DietPlanDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DietPlanDetailsRepository extends JpaRepository<DietPlanDetails, Long> {

    /** Active detail lines of one plan, ordered by timing (session). */
    @Query("""
            SELECT d FROM DietPlanDetails d
            WHERE d.status = true
              AND d.planId = :planId
            ORDER BY d.timing ASC, d.id ASC
            """)
    List<DietPlanDetails> findByPlan(@Param("planId") Long planId);
}
