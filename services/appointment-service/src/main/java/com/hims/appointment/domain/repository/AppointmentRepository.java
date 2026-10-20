package com.hims.appointment.domain.repository;

import com.hims.appointment.domain.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByApptDateAndStatusTrueOrderByDoctorIdAscSlotStartAscTokenNoAsc(LocalDate date);

    List<Appointment> findByApptDateAndDoctorIdAndStatusTrueOrderBySlotStartAscTokenNoAsc(
            LocalDate date, Long doctorId);

    /** Active (slot-consuming) bookings on a slot — service-level pre-check in front of the CAS guard. */
    long countByScheduleIdAndApptDateAndSlotStartAndApptStatusInAndStatusTrue(
            Long scheduleId, LocalDate apptDate, LocalTime slotStart, Collection<Short> statuses);

    /** Highest token issued for schedule/day (T_PATIENTTOKEN parity); null when day is fresh. */
    @Query("""
            SELECT MAX(a.tokenNo) FROM Appointment a
            WHERE a.scheduleId = :scheduleId AND a.apptDate = :apptDate AND a.status = TRUE
            """)
    Integer maxToken(@Param("scheduleId") Long scheduleId, @Param("apptDate") LocalDate apptDate);
}
