package com.hims.bloodbank.domain.repository;

import com.hims.bloodbank.domain.model.BloodCrossmatchResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BloodCrossmatchResultRepository extends JpaRepository<BloodCrossmatchResult, Long> {

    List<BloodCrossmatchResult> findByRequisitionId(Long requisitionId);

    List<BloodCrossmatchResult> findByBagId(Long bagId);

    Optional<BloodCrossmatchResult> findFirstByBagIdAndRequisitionIdAndPatientIdAndResultAndIsDecrossedFalse(
            Long bagId, Long requisitionId, Long patientId, String result);

    List<BloodCrossmatchResult> findByBagIdAndIsDecrossedFalse(Long bagId);
}
