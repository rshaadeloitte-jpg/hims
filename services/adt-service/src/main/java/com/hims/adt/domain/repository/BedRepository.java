package com.hims.adt.domain.repository;

import com.hims.adt.domain.model.Bed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BedRepository extends JpaRepository<Bed, Long> {

    List<Bed> findByWardIdAndStatusTrue(Long wardId);

    /**
     * BR-IPD-002 free-bed predicate: occupied=false AND status=true AND
     * isnoncensus/isot/ispreop/ispostop=false AND master isrelease=true (BR-IPD-003).
     */
    @Query("""
            SELECT b FROM Bed b
              JOIN BedStatusMaster s ON s.bedStatusId = b.bedStatusId
            WHERE b.status = true AND b.occupied = false
              AND b.isnoncensus = false AND b.isot = false
              AND b.ispreop = false AND b.ispostop = false
              AND s.isrelease = true
            ORDER BY b.bedId ASC
            """)
    List<Bed> findFreeBeds();

    /** Atomic CAS claim (BR-IPD-002): returns 0 when the bed was taken concurrently. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Bed b
            SET b.occupied = true, b.bedStatusId = 5
            WHERE b.bedId = :bedId AND b.occupied = false
            """)
    int claimBed(@Param("bedId") Long bedId);

    /** Release with explicit status per dictionary §A5: 0=reset (transfer/cancel), 6=post-discharge (BR-DIS-003). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Bed b
            SET b.occupied = false, b.bedStatusId = :statusId
            WHERE b.bedId = :bedId
            """)
    int releaseBed(@Param("bedId") Long bedId, @Param("statusId") short statusId);
}
