package com.hims.appointment.domain.repository;

import com.hims.appointment.domain.model.TelemedicineRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TelemedicineRoomRepository extends JpaRepository<TelemedicineRoom, Long> {

    Optional<TelemedicineRoom> findByAppointmentId(Long appointmentId);

    Optional<TelemedicineRoom> findByEncounterId(Long encounterId);

    Optional<TelemedicineRoom> findByRoomId(String roomId);
}
