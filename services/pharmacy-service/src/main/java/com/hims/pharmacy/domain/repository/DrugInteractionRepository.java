package com.hims.pharmacy.domain.repository;

import com.hims.pharmacy.domain.model.DrugInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DrugInteractionRepository extends JpaRepository<DrugInteraction, Long> {
    List<DrugInteraction> findByPatientIdOrderByRecordedAtDesc(Long patientId);
    List<DrugInteraction> findByPatientIdAndAcknowledgedFalse(Long patientId);

    @Query("SELECT di FROM DrugInteraction di WHERE di.patientId = :patientId "
         + "AND ((:drug1 = di.drug1Id AND :drug2 = di.drug2Id) "
         + "OR (:drug2 = di.drug1Id AND :drug1 = di.drug2Id))")
    List<DrugInteraction> findByPatientIdAndDrug1IdAndDrug2Id(
        @Param("patientId") Long patientId,
        @Param("drug1") Long drug1,
        @Param("drug2") Long drug2);
}
