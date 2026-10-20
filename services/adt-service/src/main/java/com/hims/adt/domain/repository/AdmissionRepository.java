package com.hims.adt.domain.repository;

import com.hims.adt.domain.model.Admission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;

public interface AdmissionRepository extends JpaRepository<Admission, Long> {

    /** BR-IPD-001 duplicate guard: active = is_cancel=false AND not discharged. */
    @Query("""
            SELECT COUNT(a) FROM Admission a
            WHERE a.patientId = :patientId AND a.isCancel = false AND a.dischargeDate IS NULL
            """)
    long countActiveByPatientId(@Param("patientId") Long patientId);

    @Query("""
            SELECT a FROM Admission a
            WHERE (:patientId IS NULL OR a.patientId = :patientId)
              AND (:active = false OR (a.isCancel = false AND a.dischargeDate IS NULL))
            ORDER BY a.admissionId DESC
            """)
    List<Admission> search(@Param("patientId") Long patientId, @Param("active") boolean active);

    /** BR-IPD-005 abscond sweep: unbedded (interim) admissions admitted before the cutoff. */
    @Query("""
            SELECT a FROM Admission a
            WHERE a.isCancel = false AND a.dischargeDate IS NULL
              AND a.bedId IS NULL AND a.admittedAt < :cutoff
            """)
    List<Admission> findInterimUnbeddedBefore(@Param("cutoff") Timestamp cutoff);

    @Query("SELECT a FROM Admission a WHERE a.tid IS NOT NULL ORDER BY a.admissionId DESC")
    List<Admission> findWithTid();

    @Query("SELECT a FROM Admission a WHERE a.isExpired = true ORDER BY a.dischargeDate DESC")
    List<Admission> findDeaths();

    long countByWardIdAndDischargeDateIsNullAndIsCancelFalse(Long wardId);
}
