package com.hims.nursing.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Date;

@Entity
@Table(name = "t_nurse_roster")
public class NurseRoster extends NursingBase {

    @Id
    @Column(name = "roster_id")
    private Long rosterId;

    @Column(name = "roster_date", nullable = false)
    private Date rosterDate;

    @Column(name = "ward_id", nullable = false)
    private Long wardId;

    @Column(name = "nurse_id", nullable = false)
    private Long nurseId;

    @Column(name = "shift", nullable = false, length = 20)
    private String shift;

    @Column(name = "bed_id")
    private Long bedId;

    @Column(name = "is_allied", nullable = false)
    private Boolean isAllied = false;

    @Override public Long getId() { return rosterId; }
    @Override public void setId(Long id) { this.rosterId = id; }
    public Long getRosterId() { return rosterId; }
    public void setRosterId(Long v) { this.rosterId = v; }
    public Date getRosterDate() { return rosterDate; }
    public void setRosterDate(Date v) { this.rosterDate = v; }
    public Long getWardId() { return wardId; }
    public void setWardId(Long v) { this.wardId = v; }
    public Long getNurseId() { return nurseId; }
    public void setNurseId(Long v) { this.nurseId = v; }
    public String getShift() { return shift; }
    public void setShift(String v) { this.shift = v; }
    public Long getBedId() { return bedId; }
    public void setBedId(Long v) { this.bedId = v; }
    public Boolean getIsAllied() { return isAllied; }
    public void setIsAllied(Boolean v) { this.isAllied = v; }
}
