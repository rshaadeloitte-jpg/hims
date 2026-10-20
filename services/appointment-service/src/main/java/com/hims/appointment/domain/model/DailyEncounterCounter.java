package com.hims.appointment.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Per-(date, unit) daily OPD encounter number allocator — same daily-reset
 * pattern as queue-service daily_counter (IMP-DEC-038): bumped atomically
 * inside the check-in transaction via INSERT .. ON CONFLICT .. UPDATE RETURNING,
 * so a rollback never burns a number. Keeps appointment-service OPD numbering
 * compatible with queue-service counters.
 */
@Entity
@Table(name = "daily_encounter_counter")
@IdClass(DailyEncounterCounter.CounterId.class)
public class DailyEncounterCounter {

    public static class CounterId implements Serializable {
        private LocalDate counterDate;
        private Long unitId;

        public CounterId() { }
        public CounterId(LocalDate counterDate, Long unitId) {
            this.counterDate = counterDate;
            this.unitId = unitId;
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CounterId that)) return false;
            return Objects.equals(counterDate, that.counterDate) && Objects.equals(unitId, that.unitId);
        }
        @Override public int hashCode() { return Objects.hash(counterDate, unitId); }
    }

    @Id
    @Column(name = "counter_date", nullable = false)
    private LocalDate counterDate;

    @Id
    @Column(name = "unit_id", nullable = false)
    private Long unitId;

    @Column(name = "last_no", nullable = false)
    private Integer lastNo = 0;

    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt;

    public LocalDate getCounterDate() { return counterDate; }
    public void setCounterDate(LocalDate counterDate) { this.counterDate = counterDate; }
    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }
    public Integer getLastNo() { return lastNo; }
    public void setLastNo(Integer lastNo) { this.lastNo = lastNo; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
