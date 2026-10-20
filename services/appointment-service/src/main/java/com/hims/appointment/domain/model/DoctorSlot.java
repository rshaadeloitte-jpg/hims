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
 * Materialized bookable slot per (schedule, date). booked_count counts ACTIVE
 * appointments (BOOKED/CHECKED_IN); the booking transaction claims capacity via
 * a conditional UPDATE (booked_count &lt; capacity) — the atomic double-booking
 * guard of BR-OPD-002. uq_doctor_slot (schedule_id, slot_date, slot_start)
 * makes materialization idempotent.
 */
@Entity
@Table(name = "doctor_slot")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DoctorSlot extends AppointmentBase {

    @Id
    private Long id;

    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "clinic_id")
    private Long clinicId;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @Column(name = "slot_start", nullable = false)
    private LocalTime slotStart;

    @Column(name = "slot_end", nullable = false)
    private LocalTime slotEnd;

    @Column(name = "capacity", nullable = false)
    private short capacity = 1;

    @Column(name = "booked_count", nullable = false)
    private short bookedCount = 0;

    @Column(name = "cabin")
    private String cabin;

    public boolean hasCapacity() {
        return status != null && status && bookedCount < capacity;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public Long getClinicId() { return clinicId; }
    public void setClinicId(Long clinicId) { this.clinicId = clinicId; }
    public LocalDate getSlotDate() { return slotDate; }
    public void setSlotDate(LocalDate slotDate) { this.slotDate = slotDate; }
    public LocalTime getSlotStart() { return slotStart; }
    public void setSlotStart(LocalTime slotStart) { this.slotStart = slotStart; }
    public LocalTime getSlotEnd() { return slotEnd; }
    public void setSlotEnd(LocalTime slotEnd) { this.slotEnd = slotEnd; }
    public short getCapacity() { return capacity; }
    public void setCapacity(short capacity) { this.capacity = capacity; }
    public short getBookedCount() { return bookedCount; }
    public void setBookedCount(short bookedCount) { this.bookedCount = bookedCount; }
    public String getCabin() { return cabin; }
    public void setCabin(String cabin) { this.cabin = cabin; }
}
