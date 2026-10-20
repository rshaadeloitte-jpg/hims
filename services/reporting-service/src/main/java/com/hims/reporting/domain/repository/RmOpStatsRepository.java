package com.hims.reporting.domain.repository;

import com.hims.reporting.domain.model.RmOpStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface RmOpStatsRepository extends JpaRepository<RmOpStats, RmOpStats.Key> {
    @Query("SELECT COALESCE(SUM(c.cnt), 0) FROM RmOpStats c WHERE c.id.statDate <= :asOf")
    long totalCnt(@Param("asOf") LocalDate asOf);
    @Query(value = "SELECT stat_date, op_type, cnt FROM ihms_reporting.rm_op_stats WHERE stat_date BETWEEN ?1 AND ?2 ORDER BY stat_date, op_type", nativeQuery = true)
    List<Object[]> rangeRows(LocalDate from, LocalDate to);
}
