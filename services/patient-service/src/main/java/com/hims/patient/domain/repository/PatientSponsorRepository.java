package com.hims.patient.domain.repository;

import com.hims.patient.domain.model.PatientSponsor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;

public interface PatientSponsorRepository extends JpaRepository<PatientSponsor, Long> {

    List<PatientSponsor> findByPatientIdAndStatusTrueOrderByIdAsc(Long patientId);

    /** Merge support: re-point sponsor rows from the source to the target patient. */
    @Modifying
    @Query("UPDATE PatientSponsor s SET s.patientId = :targetId, s.updatedBy = :actor, "
            + "s.updatedDatetime = :now WHERE s.patientId = :sourceId AND s.status = true")
    int repointToTarget(@Param("targetId") Long targetId,
                        @Param("sourceId") Long sourceId,
                        @Param("actor") Long actor,
                        @Param("now") Timestamp now);
}
