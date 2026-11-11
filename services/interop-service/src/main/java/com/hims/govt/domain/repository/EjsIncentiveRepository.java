package com.hims.govt.domain.repository;

import com.hims.govt.domain.model.EjsIncentive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EjsIncentiveRepository extends JpaRepository<EjsIncentive, Long> {

    /** All incentive rows of one ASHA + period (COMPUTED replacement scan). */
    List<EjsIncentive> findByAshaIdAndPeriodYearAndPeriodMonth(Long ashaId, Integer periodYear, Integer periodMonth);

    /** List: optional status (1..3 or name) and/or ashaId filter, newest first. */
    @Query("""
            SELECT i FROM EjsIncentive i
            WHERE (:status IS NULL OR i.status = :status)
              AND (:ashaId IS NULL OR i.ashaId = :ashaId)
            ORDER BY i.incentiveId DESC
            """)
    List<EjsIncentive> search(@Param("status") Short status, @Param("ashaId") Long ashaId);
}
