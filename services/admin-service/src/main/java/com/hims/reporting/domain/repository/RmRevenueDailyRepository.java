package com.hims.reporting.domain.repository;

import com.hims.reporting.domain.model.RmRevenueDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Read model access for daily revenue. Accumulation uses a native PG upsert
 * (INSERT .. ON CONFLICT DO UPDATE total = total + EXCLUDED.total); duplicate
 * events never reach this method (ledger guard upstream).
 */
public interface RmRevenueDailyRepository extends JpaRepository<RmRevenueDaily, RmRevenueDaily.Key> {

    @Modifying
    @Query(value = """
            INSERT INTO ihms_reporting.rm_revenue_daily (stat_date, head_code, total)
            VALUES (:statDate, :headCode, :amount)
            ON CONFLICT (stat_date, head_code)
            DO UPDATE SET total = ihms_reporting.rm_revenue_daily.total + EXCLUDED.total,
                          updated_datetime = CURRENT_TIMESTAMP
            """, nativeQuery = true)
    void accumulate(@Param("statDate") LocalDate statDate,
                    @Param("headCode") String headCode,
                    @Param("amount") BigDecimal amount);

    @Query(value = """
            SELECT COALESCE(SUM(total), 0) FROM ihms_reporting.rm_revenue_daily
            WHERE CAST(:asOf AS date) IS NULL OR stat_date <= :asOf
            """, nativeQuery = true)
    BigDecimal totalRevenue(@Param("asOf") LocalDate asOf);
}
