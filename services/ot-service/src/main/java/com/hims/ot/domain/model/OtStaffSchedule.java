package com.hims.ot.domain.model;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "t_ot_staff_schedule")
public class OtStaffSchedule extends OtBase {

    @Id
    @Column(name = "staff_schedule_id")
    private Long staffScheduleId;

    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "staff_role", nullable = false, length = 50)
    private String staffRole;

    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    @Column(name = "assigned_start", nullable = false)
    private Timestamp assignedStart;

    @Column(name = "assigned_end", nullable = false)
    private Timestamp assignedEnd;

    public Long getId() { return staffScheduleId; }
    public void setId(Long id) { this.staffScheduleId = id; }
    public Long getStaffScheduleId() { return staffScheduleId; }
    public void setStaffScheduleId(Long staffScheduleId) { this.staffScheduleId = staffScheduleId; }
    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }
    public String getStaffRole() { return staffRole; }
    public void setStaffRole(String staffRole) { this.staffRole = staffRole; }
    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }
    public Timestamp getAssignedStart() { return assignedStart; }
    public void setAssignedStart(Timestamp assignedStart) { this.assignedStart = assignedStart; }
    public Timestamp getAssignedEnd() { return assignedEnd; }
    public void setAssignedEnd(Timestamp assignedEnd) { this.assignedEnd = assignedEnd; }
}
