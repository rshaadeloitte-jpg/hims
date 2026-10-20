package com.hims.govt.domain.repository;

import com.hims.govt.domain.model.AasmaanCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AasmaanCaseRepository extends JpaRepository<AasmaanCase, Long> {

    /** List: optional status and patient filters, newest first. */
    @Query("""
            SELECT c FROM AasmaanCase c
            WHERE (:status IS NULL OR c.status = :status)
              AND (:patientId IS NULL OR c.patientId = :patientId)
            ORDER BY c.caseId DESC
            """)
    List<AasmaanCase> search(@Param("status") Short status, @Param("patientId") Long patientId);
}
