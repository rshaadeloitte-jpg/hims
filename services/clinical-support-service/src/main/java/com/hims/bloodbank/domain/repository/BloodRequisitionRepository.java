package com.hims.bloodbank.domain.repository;

import com.hims.bloodbank.domain.model.BloodRequisition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BloodRequisitionRepository extends JpaRepository<BloodRequisition, Long> {

    List<BloodRequisition> findByStatus(Integer status);

    List<BloodRequisition> findByPatientId(Long patientId);
}
