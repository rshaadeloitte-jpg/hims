package com.hims.waste.domain.repository;

import com.hims.waste.domain.model.BioMedicalWaste;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BioMedicalWasteRepository extends JpaRepository<BioMedicalWaste, Long> {

    List<BioMedicalWaste> findByUnitIdAndStatusTrueOrderByIdDesc(Long unitId);

    List<BioMedicalWaste> findByStatusTrueOrderByIdDesc();

    long countByStatusTrue();

    long countByStatusTrueAndRequestStatus(Long requestStatus);

    /**
     * Legacy getbioMedicalWasteStatus search semantics: numeric search matches
     * requisition_no OR any bag barcode of the request; non-numeric matches
     * requisition_no only. The numeric check is done in the service layer and
     * passed as {@code barcodeSearch}.
     *
     * <p>All filter params are gated by booleans and never bound as null:
     * PostgreSQL cannot infer the type of an untyped null parameter (binds as
     * bytea and breaks UPPER()/= comparisons), so the service passes sentinel
     * values (0L / "") that are ignored when the matching gate is false.
     */
    @Query("""
            SELECT b FROM BioMedicalWaste b
            WHERE b.status = true
              AND (:hasUnit = false OR b.unitId = :unitId)
              AND (:hasStatus = false OR b.requestStatus = :requestStatus)
              AND (:hasSearch = false
                   OR UPPER(b.requisitionNo) LIKE CONCAT('%', UPPER(:search), '%')
                   OR (:barcodeSearch = true
                       AND EXISTS (SELECT 1 FROM BioMedicalWasteDetails d
                                   WHERE d.bioMedicalWasteId = b.id
                                     AND UPPER(d.barcodeNo) LIKE CONCAT('%', UPPER(:search), '%'))))
            ORDER BY b.id DESC
            """)
    List<BioMedicalWaste> search(@Param("unitId") Long unitId,
                                 @Param("hasUnit") boolean hasUnit,
                                 @Param("requestStatus") Long requestStatus,
                                 @Param("hasStatus") boolean hasStatus,
                                 @Param("search") String search,
                                 @Param("hasSearch") boolean hasSearch,
                                 @Param("barcodeSearch") boolean barcodeSearch);
}
