package com.hims.appointment.domain.repository;

import com.hims.appointment.domain.model.DoctorSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface DoctorSlotRepository extends JpaRepository<DoctorSlot, Long> {

    Optional<DoctorSlot> findById(long id);

    List<DoctorSlot> findByDoctorIdAndSlotDateAndStatusTrueOrderBySlotStartAsc(Long doctorId, LocalDate slotDate);

    Optional<DoctorSlot> findByScheduleIdAndSlotDateAndSlotStart(Long scheduleId, LocalDate slotDate, LocalTime slotStart);

    /**
     * Idempotent materialization of a slot row from its schedule window
     * (uq_doctor_slot makes the conflict path a no-op).
     */
    @Modifying
    @Query(value = """
            INSERT INTO ihms_appointment.doctor_slot
                (id, unit_id, status, synchronized, added_by, added_datetime,
                 schedule_id, doctor_id, department_id, clinic_id,
                 slot_date, slot_start, slot_end, capacity, booked_count, cabin)
            VALUES (:id, :unitId, TRUE, FALSE, :addedBy, now(),
                    :scheduleId, :doctorId, :departmentId, :clinicId,
                    :slotDate, :slotStart, :slotEnd, :capacity, 0, :cabin)
            ON CONFLICT (schedule_id, slot_date, slot_start) DO NOTHING
            """, nativeQuery = true)
    int materialize(@Param("id") long id,
                    @Param("unitId") Long unitId,
                    @Param("addedBy") Long addedBy,
                    @Param("scheduleId") Long scheduleId,
                    @Param("doctorId") Long doctorId,
                    @Param("departmentId") Long departmentId,
                    @Param("clinicId") Long clinicId,
                    @Param("slotDate") LocalDate slotDate,
                    @Param("slotStart") LocalTime slotStart,
                    @Param("slotEnd") LocalTime slotEnd,
                    @Param("capacity") int capacity,
                    @Param("cabin") String cabin);

    /** Atomic double-booking guard: claim one unit of capacity (BR-OPD-002). */
    @Modifying
    @Query("""
            UPDATE DoctorSlot s SET s.bookedCount = s.bookedCount + 1
            WHERE s.id = :id AND s.status = TRUE AND s.bookedCount < s.capacity
            """)
    int claimCapacity(@Param("id") long id);

    /** Release one claimed unit (cancel / reschedule of an active booking). */
    @Modifying
    @Query("""
            UPDATE DoctorSlot s SET s.bookedCount = s.bookedCount - 1
            WHERE s.id = :id AND s.bookedCount > 0
            """)
    int releaseCapacity(@Param("id") long id);
}
