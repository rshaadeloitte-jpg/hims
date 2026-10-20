package com.hims.pharmacy.domain.repository;

import com.hims.pharmacy.domain.model.PrescriptionDispense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrescriptionDispenseRepository extends JpaRepository<PrescriptionDispense, Long> {

    Optional<PrescriptionDispense> findByPrescriptionId(Long prescriptionId);
}
