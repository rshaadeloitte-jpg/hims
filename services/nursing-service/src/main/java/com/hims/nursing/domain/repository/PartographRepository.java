package com.hims.nursing.domain.repository;

import com.hims.nursing.domain.model.Partograph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartographRepository extends JpaRepository<Partograph, Long> {
    List<Partograph> findByPatientIdOrderByAddedDatetimeDesc(Long patientId);
    List<Partograph> findByAdmissionIdOrderByAddedDatetimeDesc(Long admissionId);
}
