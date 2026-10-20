package com.hims.pharmacy.domain.repository;

import com.hims.pharmacy.domain.model.HomeMedication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface HomeMedicationRepository extends JpaRepository<HomeMedication, Long> {

    @Query("SELECT h FROM HomeMedication h WHERE h.patientId = :patientId ORDER BY h.prescribedDate DESC")
    List<HomeMedication> findByPatientId(@Param("patientId") Long patientId);

    @Query("SELECT h FROM HomeMedication h WHERE h.encounterId = :encounterId ORDER BY h.prescribedDate DESC")
    List<HomeMedication> findByEncounterId(@Param("encounterId") Long encounterId);

    @Query("SELECT h FROM HomeMedication h WHERE h.dispensed = false ORDER BY h.prescribedDate DESC")
    List<HomeMedication> findPendingDispense();
}
