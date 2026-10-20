package com.hims.patient.domain.repository;

import com.hims.patient.domain.model.PatientDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;

public interface PatientDocumentRepository extends JpaRepository<PatientDocument, Long> {

    List<PatientDocument> findByPatientIdAndStatusTrueOrderByIdAsc(Long patientId);

    /** Merge support: re-point document rows from the source to the target patient. */
    @Modifying
    @Query("UPDATE PatientDocument d SET d.patientId = :targetId, d.updatedBy = :actor, "
            + "d.updatedDatetime = :now WHERE d.patientId = :sourceId AND d.status = true")
    int repointToTarget(@Param("targetId") Long targetId,
                        @Param("sourceId") Long sourceId,
                        @Param("actor") Long actor,
                        @Param("now") Timestamp now);
}
