package com.hims.bcma.domain.repository;

import com.hims.bcma.domain.model.BcmaVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BcmaVerificationRepository extends JpaRepository<BcmaVerification, Long> {
    Optional<BcmaVerification> findByBcmaId(Long bcmaId);
    List<BcmaVerification> findByPatientId(Long patientId);
    List<BcmaVerification> findByDrugNdc(String drugNdc);
}
