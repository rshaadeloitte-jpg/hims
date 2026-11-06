package com.hims.diet.domain.repository;

import com.hims.diet.domain.model.DietPlanMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DietPlanMasterRepository extends JpaRepository<DietPlanMaster, Long> {

    /** Unit-scoped active plans, newest first. */
    @Query("""
            SELECT d FROM DietPlanMaster d
            WHERE d.status = true
              AND (:hasUnit = false OR d.unitId = :unitId)
            ORDER BY d.id DESC
            """)
    List<DietPlanMaster> findScoped(@Param("unitId") Long unitId,
                                    @Param("hasUnit") boolean hasUnit);
}
