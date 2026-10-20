package com.hims.patient.domain.repository;

import com.hims.patient.domain.model.PatientMlc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatientMlcRepository extends JpaRepository<PatientMlc, Long> {

    List<PatientMlc> findByPatientIdAndStatusTrueOrderByIdDesc(Long patientId);
}
