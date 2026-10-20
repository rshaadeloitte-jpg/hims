package com.hims.appointment.domain.repository;

import com.hims.appointment.domain.model.Questionnaire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionnaireRepository extends JpaRepository<Questionnaire, Long> {

    Optional<Questionnaire> findByAppointmentIdAndModule(Long appointmentId, String module);

    List<Questionnaire> findByAppointmentId(Long appointmentId);

    Optional<Questionnaire> findByEncounterIdAndModule(Long encounterId, String module);

    List<Questionnaire> findByPatientIdAndModuleOrderByAddedDatetimeDesc(Long patientId, String module);

    List<Questionnaire> findByEncounterId(Long encounterId);

    List<Questionnaire> findByClinicianReviewFlagTrueAndStatusTrue();
}
