package com.hims.diet.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Clean target of legacy M_DIETSESSIONMASTER (Dietsessionmaster.java). A diet
 * feeding session (e.g. breakfast / lunch / dinner): description, from/to
 * time, and a sequence number ordering sessions within a day. Legacy
 * allowedFilters: status.
 */
@Entity
@Table(name = "diet_session_master")
public class DietSessionMaster extends DietBase {

    @Id
    private Long id;

    @Column(name = "description")
    private String description;

    @Column(name = "from_time")
    private Timestamp fromTime;

    @Column(name = "to_time")
    private Timestamp toTime;

    @Column(name = "diet_sequence_number")
    private Long dietSequenceNumber;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Timestamp getFromTime() { return fromTime; }
    public void setFromTime(Timestamp fromTime) { this.fromTime = fromTime; }
    public Timestamp getToTime() { return toTime; }
    public void setToTime(Timestamp toTime) { this.toTime = toTime; }
    public Long getDietSequenceNumber() { return dietSequenceNumber; }
    public void setDietSequenceNumber(Long dietSequenceNumber) { this.dietSequenceNumber = dietSequenceNumber; }
}
