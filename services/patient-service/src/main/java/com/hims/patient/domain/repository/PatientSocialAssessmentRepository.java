package com.hims.patient.domain.repository;

import com.hims.patient.domain.model.PatientSocialAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatientSocialAssessmentRepository extends JpaRepository<PatientSocialAssessment, Long> {

    List<PatientSocialAssessment> findByPatientIdOrderByIdDesc(Long patientId);
}
