package com.hims.bcma.domain.repository;

import com.hims.bcma.domain.model.MedicationScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicationScanRepository extends JpaRepository<MedicationScan, Long> {
    Optional<MedicationScan> findByScanId(Long scanId);
    List<MedicationScan> findByPatientIdAndDrugIen(Long patientId, String drugIen);
    List<MedicationScan> findByPatientId(Long patientId);
}
