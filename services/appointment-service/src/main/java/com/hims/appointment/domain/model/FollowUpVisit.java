package com.hims.appointment.domain.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalTime;
import java.sql.Timestamp;

/**
 * Follow-up visit scheduling (legacy T_Followupvisitinfo parity).
 * Status: 1=SCHEDULED, 2=COMPLETED, 3=CANCELLED
 * Recurrence via recurrenceRule (RRULE subset, e.g., FREQ=WEEKLY;COUNT=4;BYDAY=MO)
 */
@Entity
@Table(name = "t_follow_up_visit")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FollowUpVisit extends AppointmentBase {

    @Id
    private Long id;

    @Column(name = "parent_appointment_id")
    private Long parentAppointmentId;

    @Column(name = "parent_encounter_id")
    private Long parentEncounterId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "doctor_id")
    private Long doctorId;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "clinic_id")
    private Long clinicId;

    @Column(name = "follow_up_date", nullable = false)
    private LocalDate followUpDate;

    @Column(name = "slot_start")
    private LocalTime slotStart;

    @Column(name = "slot_end")
    private LocalTime slotEnd;

    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "token_no")
    private Integer tokenNo;

    @Column(name = "cabin")
    private String cabin;

    @Column(name = "recurrence_rule")
    private String recurrenceRule;

    @Column(name = "recurrence_end_date")
    private LocalDate recurrenceEndDate;

    @Column(name = "parent_follow_up_id")
    private Long parentFollowUpId;

    /** Status: 1=SCHEDULED, 2=COMPLETED, 3=CANCELLED */
    @Column(name = "follow_up_status", nullable = false)
    private Short followUpStatus = 1;

    @Column(name = "is_from_patient_portal", nullable = false)
    private Boolean isFromPatientPortal = false;

    @Column(name = "patient_portal_id")
    private Long patientPortalId;

    @Column(name = "patient_portal_unit_id")
    private Long patientPortalUnitId;

    @Column(name = "portal_patient_id")
    private Long portalPatientId;

    @Column(name = "is_cancelled", nullable = false)
    private Boolean isCancelled = false;

    @Column(name = "cancelled_by")
    private Long cancelledBy;

    @Column(name = "cancelled_date")
    private Timestamp cancelledDate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getParentAppointmentId() { return parentAppointmentId; }
    public void setParentAppointmentId(Long parentAppointmentId) { this.parentAppointmentId = parentAppointmentId; }
    public Long getParentEncounterId() { return parentEncounterId; }
    public void setParentEncounterId(Long parentEncounterId) { this.parentEncounterId = parentEncounterId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public Long getClinicId() { return clinicId; }
    public void setClinicId(Long clinicId) { this.clinicId = clinicId; }
    public LocalDate getFollowUpDate() { return followUpDate; }
    public void setFollowUpDate(LocalDate followUpDate) { this.followUpDate = followUpDate; }
    public LocalTime getSlotStart() { return slotStart; }
    public void setSlotStart(LocalTime slotStart) { this.slotStart = slotStart; }
    public LocalTime getSlotEnd() { return slotEnd; }
    public void setSlotEnd(LocalTime slotEnd) { this.slotEnd = slotEnd; }
    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }
    public Integer getTokenNo() { return tokenNo; }
    public void setTokenNo(Integer tokenNo) { this.tokenNo = tokenNo; }
    public String getCabin() { return cabin; }
    public void setCabin(String cabin) { this.cabin = cabin; }
    public String getRecurrenceRule() { return recurrenceRule; }
    public void setRecurrenceRule(String recurrenceRule) { this.recurrenceRule = recurrenceRule; }
    public LocalDate getRecurrenceEndDate() { return recurrenceEndDate; }
    public void setRecurrenceEndDate(LocalDate recurrenceEndDate) { this.recurrenceEndDate = recurrenceEndDate; }
    public Long getParentFollowUpId() { return parentFollowUpId; }
    public void setParentFollowUpId(Long parentFollowUpId) { this.parentFollowUpId = parentFollowUpId; }
    public Short getFollowUpStatus() { return followUpStatus; }
    public void setFollowUpStatus(Short followUpStatus) { this.followUpStatus = followUpStatus; }
    public Boolean getIsFromPatientPortal() { return isFromPatientPortal; }
    public void setIsFromPatientPortal(Boolean isFromPatientPortal) { this.isFromPatientPortal = isFromPatientPortal; }
    public Long getPatientPortalId() { return patientPortalId; }
    public void setPatientPortalId(Long patientPortalId) { this.patientPortalId = patientPortalId; }
    public Long getPatientPortalUnitId() { return patientPortalUnitId; }
    public void setPatientPortalUnitId(Long patientPortalUnitId) { this.patientPortalUnitId = patientPortalUnitId; }
    public Long getPortalPatientId() { return portalPatientId; }
    public void setPortalPatientId(Long portalPatientId) { this.portalPatientId = portalPatientId; }
    public Boolean getIsCancelled() { return isCancelled; }
    public void setIsCancelled(Boolean isCancelled) { this.isCancelled = isCancelled; }
    public Long getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(Long cancelledBy) { this.cancelledBy = cancelledBy; }
    public Timestamp getCancelledDate() { return cancelledDate; }
    public void setCancelledDate(Timestamp cancelledDate) { this.cancelledDate = cancelledDate; }
}
