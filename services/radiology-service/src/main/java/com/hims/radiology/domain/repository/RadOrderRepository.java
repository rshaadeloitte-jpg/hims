package com.hims.radiology.domain.repository;

import com.hims.radiology.domain.model.RadOrderBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RadOrderRepository extends JpaRepository<RadOrderBooking, Long> {

    /** List: optional status, patient and modality filters, newest first. */
    @Query("""
            SELECT r FROM RadOrderBooking r
            WHERE (:status IS NULL OR r.status = :status)
              AND (:patientId IS NULL OR r.patientId = :patientId)
              AND (:modality IS NULL OR r.modality = :modality)
            ORDER BY r.radOrderId DESC
            """)
    List<RadOrderBooking> search(@Param("status") Short status,
                                 @Param("patientId") Long patientId,
                                 @Param("modality") String modality);
}
