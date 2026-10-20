package com.hims.patient.domain.repository;

import com.hims.patient.domain.model.PatientAadhaarVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientAadhaarVerificationRepository extends JpaRepository<PatientAadhaarVerification, Long> {

    Optional<PatientAadhaarVerification> findTopByPatientIdOrderByIdDesc(Long patientId);

    List<PatientAadhaarVerification> findByPatientIdOrderByIdDesc(Long patientId);
}
