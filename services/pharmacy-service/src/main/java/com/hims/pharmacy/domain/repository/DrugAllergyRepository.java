package com.hims.pharmacy.domain.repository;

import com.hims.pharmacy.domain.model.DrugAllergy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DrugAllergyRepository extends JpaRepository<DrugAllergy, Long> {
    List<DrugAllergy> findByPatientIdOrderByRecordedAtDesc(Long patientId);
    List<DrugAllergy> findByPatientIdAndAllergenIgnoreCase(Long patientId, String allergen);
    List<DrugAllergy> findByPatientIdAndAllergenIn(Long patientId, java.util.Set<Long> allergens);
}
