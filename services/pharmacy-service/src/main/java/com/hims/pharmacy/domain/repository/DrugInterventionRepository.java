package com.hims.pharmacy.domain.repository;

import com.hims.pharmacy.domain.model.DrugIntervention;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DrugInterventionRepository extends JpaRepository<DrugIntervention, Long> {

    List<DrugIntervention> findByPatientIdOrderByInterventionIdDesc(Long patientId);
}
