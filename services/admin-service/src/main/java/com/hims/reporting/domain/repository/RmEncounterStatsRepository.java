package com.hims.reporting.domain.repository;

import com.hims.reporting.domain.model.RmEncounterStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

/**
 * Read model access for encounter stats. The increment uses a native PG upsert
 * (INSERT .. ON CONFLICT DO UPDATE cnt = cnt + 1) — read-model accumulation
 * semantics; duplicate events never reach this method (ledger guard upstream).
 */
public interface RmEncounterStatsRepository extends JpaRepository<RmEncounterStats, RmEncounterStats.Key> {

    @Modifying
    @Query(value = """
            INSERT INTO ihms_reporting.rm_encounter_stats (stat_date, encounter_type, cnt)
            VALUES (:statDate, :encounterType, 1)
            ON CONFLICT (stat_date, encounter_type)
            DO UPDATE SET cnt = ihms_reporting.rm_encounter_stats.cnt + 1,
                          updated_datetime = CURRENT_TIMESTAMP
            """, nativeQuery = true)
    void increment(@Param("statDate") LocalDate statDate, @Param("encounterType") String encounterType);

    @Query(value = """
            SELECT COALESCE(SUM(cnt), 0) FROM ihms_reporting.rm_encounter_stats
            WHERE CAST(:asOf AS date) IS NULL OR stat_date <= :asOf
            """, nativeQuery = true)
    long totalCnt(@Param("asOf") LocalDate asOf);
}
