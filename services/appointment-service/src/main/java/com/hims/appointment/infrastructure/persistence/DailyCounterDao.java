package com.hims.appointment.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Atomic daily OPD encounter number allocation:
 * INSERT .. ON CONFLICT (counter_date, unit_id) DO UPDATE .. RETURNING — runs in
 * the check-in transaction; a rollback never burns a number and concurrent
 * check-ins get distinct consecutive numbers (queue-service counter compatible).
 */
@Component
public class DailyCounterDao {

    @PersistenceContext
    private EntityManager em;

    public int nextEncounterNo(LocalDate date, Long unitId) {
        Object result = em.createNativeQuery("""
                INSERT INTO ihms_appointment.daily_encounter_counter (counter_date, unit_id, last_no, updated_at)
                VALUES (:d, :u, 1, now())
                ON CONFLICT (counter_date, unit_id)
                    DO UPDATE SET last_no = ihms_appointment.daily_encounter_counter.last_no + 1,
                                  updated_at = now()
                RETURNING last_no
                """)
                .setParameter("d", date)
                .setParameter("u", unitId == null ? 0L : unitId)
                .getSingleResult();
        return ((Number) result).intValue();
    }
}
