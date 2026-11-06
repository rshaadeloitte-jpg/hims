package com.hims.equipment.domain.repository;

import com.hims.equipment.domain.model.EqpPreventiveMaintenance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;

public interface EqpPreventiveMaintenanceRepository extends JpaRepository<EqpPreventiveMaintenance, Long> {

    /** Execution history of one schedule — chronological (oldest first). */
    List<EqpPreventiveMaintenance> findByPmScheduleIdOrderByIdAsc(Long pmScheduleId);

    /** Maintenance history of one asset — newest first. */
    List<EqpPreventiveMaintenance> findByEquipmentIdAndStatusTrueOrderByIdDesc(Long equipmentId);

    /**
     * Maintenance search. All filter params are gated by booleans and never
     * bound as null: PostgreSQL cannot infer the type of an untyped null
     * parameter (binds as bytea and breaks UPPER()/= comparisons), so callers
     * pass sentinel values that are ignored when the matching gate is false
     * (same convention as EquipmentRepository.search).
     *
     * <p>Free-text search matches maintenance_no and remarks.
     */
    @Query("""
            SELECT m FROM EqpPreventiveMaintenance m
            WHERE m.status = true
              AND (:hasUnit = false OR m.unitId = :unitId)
              AND (:hasEquipment = false OR m.equipmentId = :equipmentId)
              AND (:hasPmStatus = false OR m.pmStatus = :pmStatus)
              AND (:hasFrom = false OR m.maintenanceDate >= :fromDate)
              AND (:hasTo = false OR m.maintenanceDate <= :toDate)
              AND (:hasSearch = false
                   OR UPPER(m.maintenanceNo) LIKE CONCAT('%', UPPER(:search), '%')
                   OR UPPER(m.remarks) LIKE CONCAT('%', UPPER(:search), '%'))
            ORDER BY m.id DESC
            """)
    List<EqpPreventiveMaintenance> search(@Param("unitId") Long unitId,
                                          @Param("hasUnit") boolean hasUnit,
                                          @Param("equipmentId") Long equipmentId,
                                          @Param("hasEquipment") boolean hasEquipment,
                                          @Param("pmStatus") Long pmStatus,
                                          @Param("hasPmStatus") boolean hasPmStatus,
                                          @Param("fromDate") Timestamp fromDate,
                                          @Param("hasFrom") boolean hasFrom,
                                          @Param("toDate") Timestamp toDate,
                                          @Param("hasTo") boolean hasTo,
                                          @Param("search") String search,
                                          @Param("hasSearch") boolean hasSearch);
}
