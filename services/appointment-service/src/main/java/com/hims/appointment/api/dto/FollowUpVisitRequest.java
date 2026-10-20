package com.hims.appointment.api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Request DTO for creating a follow-up visit.
 * Maps to legacy AddFollowUpVisitInfoDto parity with new domain model.
 */
public class FollowUpVisitRequest {

    @NotNull(message = "Follow-up date is required")
    @Future(message = "Follow-up date must be in the future")
    private LocalDate followUpDate;

    @NotNull(message = "Doctor is required")
    private Long doctorId;

    @NotNull(message = "Department is required")
    private Long departmentId;

    private Long clinicId;
    private Integer tokenNo;
    private String cabin;
    private String recurrenceRule;
    private LocalDate recurrenceEndDate;
    private Boolean isFromPatientPortal = false;

    // Time slots
    private LocalTime slotStart;
    private LocalTime slotEnd;

    // Nested DTO for follow-up details (maps to legacy AddFollowUpVisitDetailsDto)
    public static class FollowUpDetail {
        @NotNull(message = "Visit type is required")
        private Long visitType;

        // This might be redundant with top-level departmentId
        private Long departmentId;

        private Long clinicId;

        // Getters and setters
        public Long getVisitType() { return visitType; }
        public void setVisitType(Long visitType) { this.visitType = visitType; }
        public Long getDepartmentId() { return departmentId; }
        public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
        public Long getClinicId() { return clinicId; }
        public void setClinicId(Long clinicId) { this.clinicId = clinicId; }
    }

    private List<FollowUpDetail> followUpDetails;

    // Getters and setters
    public LocalDate getFollowUpDate() { return followUpDate; }
    public void setFollowUpDate(LocalDate followUpDate) { this.followUpDate = followUpDate; }

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public Long getClinicId() { return clinicId; }
    public void setClinicId(Long clinicId) { this.clinicId = clinicId; }

    public Integer getTokenNo() { return tokenNo; }
    public void setTokenNo(Integer tokenNo) { this.tokenNo = tokenNo; }

    public String getCabin() { return cabin; }
    public void setCabin(String cabin) { this.cabin = cabin; }

    public String getRecurrenceRule() { return recurrenceRule; }
    public void setRecurrenceRule(String recurrenceRule) { this.recurrenceRule = recurrenceRule; }

    public LocalDate getRecurrenceEndDate() { return recurrenceEndDate; }
    public void setRecurrenceEndDate(LocalDate recurrenceEndDate) { this.recurrenceEndDate = recurrenceEndDate; }

    public Boolean getIsFromPatientPortal() { return isFromPatientPortal; }
    public void setIsFromPatientPortal(Boolean isFromPatientPortal) { this.isFromPatientPortal = isFromPatientPortal; }

    public LocalTime getSlotStart() { return slotStart; }
    public void setSlotStart(LocalTime slotStart) { this.slotStart = slotStart; }

    public LocalTime getSlotEnd() { return slotEnd; }
    public void setSlotEnd(LocalTime slotEnd) { this.slotEnd = slotEnd; }

    public List<FollowUpDetail> getFollowUpDetails() { return followUpDetails; }
    public void setFollowUpDetails(List<FollowUpDetail> followUpDetails) { this.followUpDetails = followUpDetails; }
}
