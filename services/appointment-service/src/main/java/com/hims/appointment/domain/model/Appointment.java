package com.hims.appointment.domain.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Clean target of T_APPOINTMENT — the appointment aggregate root.
 *
 * Status lifecycle is the MIG-031 LOCKED dictionary (see {@link AppointmentStatus});
 * transitions are validated through {@link AppointmentStatus#canTransitionTo},
 * never by comparing magic numbers at call sites.
 *
 * token_no is auto-assigned per (schedule_id, appt_date) — legacy T_PATIENTTOKEN
 * semantics; uq_appointment_token makes issuance race-safe.
 *
 * legacy_visit_status mirrors the split-brain VisitStatus writes (11/12) for
 * kiosk/queue compatibility; resolution rules in {@link LegacyVisitStatus}.
 */
@Entity
@Table(name = "appointment")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Appointment extends AppointmentBase {

    @Id
    private Long id;

    @Column(name = "token_no", nullable = false)
    private Integer tokenNo;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "clinic_id")
    private Long clinicId;

    @Column(name = "appt_date", nullable = false)
    private LocalDate apptDate;

    @Column(name = "slot_start", nullable = false)
    private LocalTime slotStart;

    @Column(name = "slot_end", nullable = false)
    private LocalTime slotEnd;

    @Column(name = "cabin")
    private String cabin;

    /** Locked dictionary: see {@link AppointmentStatus}. */
    @Column(name = "appt_status", nullable = false)
    private short apptStatus = AppointmentStatus.BOOKED.code();

    @Column(name = "rescheduled_from_id")
    private Long rescheduledFromId;

    @Column(name = "encounter_id")
    private Long encounterId;

    /** Split-brain mirror (VisitStatus 11/12); resolution in {@link LegacyVisitStatus}. */
    @Column(name = "legacy_visit_status")
    private Short legacyVisitStatus;

    /** Visit type: APPOINTMENT, WALK_IN, EMERGENCY (M02 gap). */
    @Column(name = "visit_type", nullable = false)
    private String visitType = "APPOINTMENT";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getTokenNo() { return tokenNo; }
    public void setTokenNo(Integer tokenNo) { this.tokenNo = tokenNo; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public Long getClinicId() { return clinicId; }
    public void setClinicId(Long clinicId) { this.clinicId = clinicId; }
    public LocalDate getApptDate() { return apptDate; }
    public void setApptDate(LocalDate apptDate) { this.apptDate = apptDate; }
    public LocalTime getSlotStart() { return slotStart; }
    public void setSlotStart(LocalTime slotStart) { this.slotStart = slotStart; }
    public LocalTime getSlotEnd() { return slotEnd; }
    public void setSlotEnd(LocalTime slotEnd) { this.slotEnd = slotEnd; }
    public String getCabin() { return cabin; }
    public void setCabin(String cabin) { this.cabin = cabin; }
    public short getApptStatus() { return apptStatus; }
    public void setApptStatus(short apptStatus) { this.apptStatus = apptStatus; }
    public AppointmentStatus getStatusEnum() { return AppointmentStatus.fromCode(apptStatus); }
    public Long getRescheduledFromId() { return rescheduledFromId; }
    public void setRescheduledFromId(Long rescheduledFromId) { this.rescheduledFromId = rescheduledFromId; }
    public Long getEncounterId() { return encounterId; }
    public void setEncounterId(Long encounterId) { this.encounterId = encounterId; }
    public Short getLegacyVisitStatus() { return legacyVisitStatus; }
    public void setLegacyVisitStatus(Short legacyVisitStatus) { this.legacyVisitStatus = legacyVisitStatus; }
    public String getVisitType() { return visitType; }
    public void setVisitType(String visitType) { this.visitType = visitType; }

    /** Forward-only transition guard (state machine, MIG-031 gate). */
    public void transitionTo(AppointmentStatus target) {
        AppointmentStatus current = getStatusEnum();
        if (!current.canTransitionTo(target)) {
            throw new com.hims.appointment.application.exceptions.InvalidAppointmentRequestException(
                    "APPT_ILLEGAL_TRANSITION",
                    "Illegal transition " + current + "->" + target + " (appointment " + id + ")");
        }
        this.apptStatus = target.code();
    }

    /** Check-in is legal only from BOOKED (state machine gate). */
    public boolean isOpenForCheckIn() {
        return Boolean.TRUE.equals(getStatus()) && getStatusEnum() == AppointmentStatus.BOOKED;
    }

    public boolean slotActive() {
        return Boolean.TRUE.equals(getStatus()) && AppointmentStatus.SLOT_CONSUMING.contains(getStatusEnum());
    }
}
