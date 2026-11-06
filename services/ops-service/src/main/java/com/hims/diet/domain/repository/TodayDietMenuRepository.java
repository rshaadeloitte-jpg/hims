package com.hims.diet.domain.repository;

import com.hims.diet.domain.model.TodayDietMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TodayDietMenuRepository extends JpaRepository<TodayDietMenu, Long> {

    /**
     * Legacy addDietMenuItem / removeDietMenuItem uniqueness key:
     * (unitId, dietTypeId, dietSessionId, itemId). Returns the row regardless
     * of status so a removed item can be re-activated.
     */
    @Query("""
            SELECT t FROM TodayDietMenu t
            WHERE t.unitId = :unitId
              AND t.dietTypeId = :dietTypeId
              AND t.dietSessionId = :dietSessionId
              AND t.itemId = :itemId
            """)
    Optional<TodayDietMenu> findByKey(@Param("unitId") Long unitId,
                                      @Param("dietTypeId") Long dietTypeId,
                                      @Param("dietSessionId") Long dietSessionId,
                                      @Param("itemId") Long itemId);

    /** Legacy getItemsByDietTypeId: active menu rows for a diet type in a unit. */
    @Query("""
            SELECT t FROM TodayDietMenu t
            WHERE t.status = true
              AND t.unitId = :unitId
              AND t.dietTypeId = :dietTypeId
            ORDER BY t.id ASC
            """)
    List<TodayDietMenu> findByDietType(@Param("unitId") Long unitId,
                                       @Param("dietTypeId") Long dietTypeId);

    /** Active menu rows for a unit (legacy getTodayDietItem menu half). */
    @Query("""
            SELECT t FROM TodayDietMenu t
            WHERE t.status = true
              AND t.unitId = :unitId
            ORDER BY t.id ASC
            """)
    List<TodayDietMenu> findByUnit(@Param("unitId") Long unitId);
}
