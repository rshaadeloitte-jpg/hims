package com.hims.reporting.domain.model;

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
 * Read model: encounter counts per (stat_date, encounter_type), fed by
 * EncounterCreated via the internal event ingress. Increment is guarded by the
 * t_ingested_event ledger — a re-ingested encounter adds NOTHING (pinned).
 */
@Entity
@Table(name = "rm_encounter_stats")
@IdClass(RmEncounterStats.Key.class)
public class RmEncounterStats extends ReportingBase {

    @Id
    @Column(name = "stat_date")
    private LocalDate statDate;

    @Id
    @Column(name = "encounter_type", length = 20)
    private String encounterType;

    @Column(name = "cnt", nullable = false)
    private Integer cnt = 0;

    public LocalDate getStatDate() { return statDate; }
    public void setStatDate(LocalDate statDate) { this.statDate = statDate; }
    public String getEncounterType() { return encounterType; }
    public void setEncounterType(String encounterType) { this.encounterType = encounterType; }
    public Integer getCnt() { return cnt; }
    public void setCnt(Integer cnt) { this.cnt = cnt; }

    /** Composite natural key (stat_date, encounter_type). */
    public static class Key implements Serializable {
        private LocalDate statDate;
        private String encounterType;

        public Key() { }
        public Key(LocalDate statDate, String encounterType) {
            this.statDate = statDate;
            this.encounterType = encounterType;
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return Objects.equals(statDate, key.statDate)
                    && Objects.equals(encounterType, key.encounterType);
        }
        @Override public int hashCode() { return Objects.hash(statDate, encounterType); }
    }
}
