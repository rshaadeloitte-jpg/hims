package com.hims.nursing.domain.repository;

import com.hims.nursing.domain.model.PatientVitals;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;

public interface PatientVitalsRepository extends JpaRepository<PatientVitals, Long> {

    /** Vitals for a patient within [from, to] (either bound optional). */
    @Query("""
            SELECT v FROM PatientVitals v
            WHERE v.patientId = :patientId AND v.status = true
              AND (CAST(:fromTs AS timestamp) IS NULL OR v.recordedAt >= :fromTs)
              AND (CAST(:toTs AS timestamp) IS NULL OR v.recordedAt <= :toTs)
            ORDER BY v.recordedAt DESC, v.id DESC
            """)
    List<PatientVitals> findByPatientInRange(@Param("patientId") Long patientId,
                                            @Param("fromTs") Timestamp fromTs,
                                            @Param("toTs") Timestamp toTs);
}
