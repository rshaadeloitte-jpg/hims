package com.hims.equipment.domain.repository;

import com.hims.equipment.domain.model.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    List<Equipment> findByItemIdAndStatusTrueOrderByIdDesc(Long itemId);

    /**
     * Accession lookup scoped to a unit (matches the partial unique index
     * uq_equipment_unit_accession). Case-insensitive.
     */
    Optional<Equipment> findByUnitIdAndAccessionNoIgnoreCase(Long unitId, String accessionNo);

    /** Fallback accession lookup when no unit scope is supplied — newest wins. */
    Optional<Equipment> findFirstByAccessionNoIgnoreCaseOrderByIdDesc(String accessionNo);

    /**
     * Asset-register search. All filter params are gated by booleans and never
     * bound as null: PostgreSQL cannot infer the type of an untyped null
     * parameter (binds as bytea and breaks UPPER()/= comparisons), so the
     * service passes sentinel values (0L / "") that are ignored when the
     * matching gate is false (same convention as waste-service).
     *
     * <p>Free-text search matches accession_no, serial_no, model, make,
     * location and custodian_name.
     */
    @Query("""
            SELECT e FROM Equipment e
            WHERE e.status = true
              AND (:hasUnit = false OR e.unitId = :unitId)
              AND (:hasWorkingStatus = false OR e.workingStatus = :workingStatus)
              AND (:hasSearch = false
                   OR UPPER(e.accessionNo) LIKE CONCAT('%', UPPER(:search), '%')
                   OR UPPER(e.serialNo) LIKE CONCAT('%', UPPER(:search), '%')
                   OR UPPER(e.model) LIKE CONCAT('%', UPPER(:search), '%')
                   OR UPPER(e.make) LIKE CONCAT('%', UPPER(:search), '%')
                   OR UPPER(e.location) LIKE CONCAT('%', UPPER(:search), '%')
                   OR UPPER(e.custodianName) LIKE CONCAT('%', UPPER(:search), '%'))
            ORDER BY e.id DESC
            """)
    List<Equipment> search(@Param("unitId") Long unitId,
                           @Param("hasUnit") boolean hasUnit,
                           @Param("workingStatus") Long workingStatus,
                           @Param("hasWorkingStatus") boolean hasWorkingStatus,
                           @Param("search") String search,
                           @Param("hasSearch") boolean hasSearch);
}
