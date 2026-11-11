package com.hims.kiosk.domain.repository;

import com.hims.kiosk.domain.model.KioskAppointment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface KioskAppointmentRepository extends JpaRepository<KioskAppointment, Long> {
    List<KioskAppointment> findByAppointmentDateAndStatusTrue(LocalDate date);
    List<KioskAppointment> findByPatientIdAndStatusTrue(Long patientId);
}
