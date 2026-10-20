package com.hims.bcma.domain.repository;

import com.hims.bcma.domain.model.BCMAException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BCMAExceptionRepository extends JpaRepository<BCMAException, Long> {
    Optional<BCMAException> findByExceptionId(Long exceptionId);
    List<BCMAException> findByPatientId(Long patientId);
    List<BCMAException> findByExceptionType(String exceptionType);
    List<BCMAException> findByWardId(Long wardId);
}
