package com.hims.patient.domain.repository;

import com.hims.patient.domain.model.PatientRelation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatientRelationRepository extends JpaRepository<PatientRelation, Long> {

    List<PatientRelation> findByPatientIdAndStatusTrueOrderByIdAsc(Long patientId);

    List<PatientRelation> findByRelatedPatientIdAndStatusTrueOrderByIdAsc(Long relatedPatientId);
}
