package com.hims.ordering.domain.repository;

import com.hims.ordering.domain.model.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    /** List: optional status and patient filters, newest first. */
    @Query("""
            SELECT p FROM Prescription p
            WHERE (:status IS NULL OR p.statusCode = :status)
              AND (:patientId IS NULL OR p.patientId = :patientId)
            ORDER BY p.prescriptionId DESC
            """)
    List<Prescription> search(@Param("status") Short status, @Param("patientId") Long patientId);
}
