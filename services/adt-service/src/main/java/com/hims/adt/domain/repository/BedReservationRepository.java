package com.hims.adt.domain.repository;

import com.hims.adt.domain.model.BedReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BedReservationRepository extends JpaRepository<BedReservation, Long> {

    Optional<BedReservation> findFirstByBedIdAndResStatus(Long bedId, BedReservation.ResStatus status);

    Optional<BedReservation> findFirstByBedIdAndResStatusOrderByReservationIdDesc(
            Long bedId, BedReservation.ResStatus status);

    List<BedReservation> findByResStatus(BedReservation.ResStatus status);

    /** Consume the reservation of the given patient for the claimed bed (CAS-style). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE BedReservation r SET r.resStatus = 'CONSUMED', r.releasedAt = :now
            WHERE r.bedId = :bedId AND r.patientId = :patientId AND r.resStatus = 'ACTIVE'
            """)
    int consume(@Param("bedId") Long bedId, @Param("patientId") Long patientId, @Param("now") java.sql.Timestamp now);

    /** Expiry sweep: stale ACTIVE rows -> EXPIRED. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE BedReservation r SET r.resStatus = 'EXPIRED', r.releasedAt = :now
            WHERE r.resStatus = 'ACTIVE' AND r.expiresAt < :now
            """)
    int expireStale(@Param("now") java.sql.Timestamp now);
}
