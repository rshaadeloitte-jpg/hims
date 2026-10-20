package com.hims.lab.domain.repository;

import com.hims.lab.domain.model.PathOrderBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PathOrderBookingRepository extends JpaRepository<PathOrderBooking, Long> {

    /** Worklist: optional status and patient filters, newest first. */
    @Query("""
            SELECT b FROM PathOrderBooking b
            WHERE (:status IS NULL OR b.bookingStatus = :status)
              AND (:patientId IS NULL OR b.patientId = :patientId)
            ORDER BY b.bookingId DESC
            """)
    List<PathOrderBooking> search(@Param("status") Short status, @Param("patientId") Long patientId);

    long countByBookingStatus(Short bookingStatus);
}
