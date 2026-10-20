package com.hims.ordering.domain.repository;

import com.hims.ordering.domain.model.ClinicalOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClinicalOrderRepository extends JpaRepository<ClinicalOrder, Long> {

    /** List: optional status and patient filters, newest first. */
    @Query("""
            SELECT o FROM ClinicalOrder o
            WHERE (:status IS NULL OR o.statusCode = :status)
              AND (:patientId IS NULL OR o.patientId = :patientId)
            ORDER BY o.orderId DESC
            """)
    List<ClinicalOrder> search(@Param("status") Short status, @Param("patientId") Long patientId);
}
