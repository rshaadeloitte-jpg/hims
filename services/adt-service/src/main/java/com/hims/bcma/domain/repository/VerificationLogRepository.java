package com.hims.bcma.domain.repository;

import com.hims.bcma.domain.model.VerificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VerificationLogRepository extends JpaRepository<VerificationLog, Long> {
    List<VerificationLog> findByBcmaId(Long bcmaId);
    List<VerificationLog> findByPatientId(Long patientId);
    List<VerificationLog> findByVerificationEvent(String event);
}
