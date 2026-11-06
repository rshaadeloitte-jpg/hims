package com.hims.diet.domain.repository;

import com.hims.diet.domain.model.DietSessionMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DietSessionMasterRepository extends JpaRepository<DietSessionMaster, Long> {

    /** Legacy allowedFilters=status; unit-scoped active sessions ordered by sequence. */
    @Query("""
            SELECT d FROM DietSessionMaster d
            WHERE d.status = true
              AND (:hasUnit = false OR d.unitId = :unitId)
            ORDER BY d.dietSequenceNumber ASC, d.id ASC
            """)
    List<DietSessionMaster> findScoped(@Param("unitId") Long unitId,
                                       @Param("hasUnit") boolean hasUnit);
}
