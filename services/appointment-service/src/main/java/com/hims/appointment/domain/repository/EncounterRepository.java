package com.hims.appointment.domain.repository;

import com.hims.appointment.domain.model.Encounter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EncounterRepository extends JpaRepository<Encounter, Long> {

    List<Encounter> findByAppointmentId(Long appointmentId);

    List<Encounter> findByClosedFalseAndEncounterDateLessThan(LocalDate date);

    List<Encounter> findByPatientIdAndEncounterDateOrderByIdAsc(Long patientId, LocalDate date);
}
