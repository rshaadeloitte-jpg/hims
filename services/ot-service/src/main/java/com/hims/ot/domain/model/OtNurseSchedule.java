package com.hims.ot.domain.model;

import jakarta.persistence.*;
import java.sql.Date;

@Entity
@Table(name = "t_ot_nurse_schedule")
public class OtNurseSchedule extends OtBase {

    @Id
    @Column(name = "nurse_schedule_id")
    private Long nurseScheduleId;

    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    @Column(name = "duty_date", nullable = false)
    private Date dutyDate;

    @Column(name = "shift_type", nullable = false, length = 50)
    private String shiftType;

    @Column(name = "duty_roster", length = 50)
    private String dutyRoster;

    @Column(name = "emergency_call", nullable = false)
    private Boolean emergencyCall = false;

    public Long getId() { return nurseScheduleId; }
    public void setId(Long id) { this.nurseScheduleId = id; }
    public Long getNurseScheduleId() { return nurseScheduleId; }
    public void setNurseScheduleId(Long nurseScheduleId) { this.nurseScheduleId = nurseScheduleId; }
    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }
    public Date getDutyDate() { return dutyDate; }
    public void setDutyDate(Date dutyDate) { this.dutyDate = dutyDate; }
    public String getShiftType() { return shiftType; }
    public void setShiftType(String shiftType) { this.shiftType = shiftType; }
    public String getDutyRoster() { return dutyRoster; }
    public void setDutyRoster(String dutyRoster) { this.dutyRoster = dutyRoster; }
    public Boolean getEmergencyCall() { return emergencyCall; }
    public void setEmergencyCall(Boolean emergencyCall) { this.emergencyCall = emergencyCall; }
}
