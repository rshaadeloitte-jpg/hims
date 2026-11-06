package com.hims.diet.domain.repository;

import com.hims.diet.domain.model.DietItemMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DietItemMasterRepository extends JpaRepository<DietItemMaster, Long> {

    /** Legacy allowedFilters=status; unit-scoped active list, newest first. */
    @Query("""
            SELECT d FROM DietItemMaster d
            WHERE d.status = true
              AND (:hasUnit = false OR d.unitId = :unitId)
            ORDER BY d.id DESC
            """)
    List<DietItemMaster> findScoped(@Param("unitId") Long unitId,
                                    @Param("hasUnit") boolean hasUnit);
}
