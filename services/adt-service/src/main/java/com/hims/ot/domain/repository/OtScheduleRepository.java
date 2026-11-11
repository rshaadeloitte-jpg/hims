package com.hims.ot.domain.repository;

import com.hims.ot.domain.model.OtSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;

public interface OtScheduleRepository extends JpaRepository<OtSchedule, Long> {

    /**
     * Worklist search backing GET /api/v1/ot/schedules?date=&amp;status=.
     * date matches the calendar day of scheduled_start; both filters optional.
     * CASTs pin the JDBC parameter types — without them PostgreSQL cannot infer
     * the type of a null-bound "? is null" placeholder ("could not determine
     * data type of parameter").
     */
    @Query("""
            SELECT s FROM OtSchedule s
            WHERE (cast(:status AS integer) IS NULL OR s.status = :status)
              AND (cast(:dayStart AS timestamp) IS NULL
                   OR (s.scheduledStart >= :dayStart AND s.scheduledStart < :dayEnd))
            ORDER BY s.scheduledStart ASC, s.scheduleId ASC
            """)
    List<OtSchedule> search(@Param("status") Integer status,
                            @Param("dayStart") Timestamp dayStart,
                            @Param("dayEnd") Timestamp dayEnd);
}
