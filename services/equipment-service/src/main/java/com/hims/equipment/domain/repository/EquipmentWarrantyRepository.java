package com.hims.equipment.domain.repository;

import com.hims.equipment.domain.model.EquipmentWarranty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;

public interface EquipmentWarrantyRepository extends JpaRepository<EquipmentWarranty, Long> {

    /** All active covers of one asset — newest first (asset warranty history). */
    List<EquipmentWarranty> findByEquipmentIdAndStatusTrueOrderByIdDesc(Long equipmentId);

    /**
     * Expiry-alert window: active covers whose {@code end_date} falls in
     * [from, to) — rows with a NULL end_date never expire and are correctly
     * excluded. Optional unit / asset filters are gated by booleans (same
     * sentinel convention as EquipmentRepository.search — the gates keep the
     * query readable; JPQL params are typed so nulls would also bind safely,
     * but the house pattern keeps every list/search query identical).
     */
    @Query("""
            SELECT w FROM EquipmentWarranty w
            WHERE w.status = true
              AND w.endDate >= :from
              AND w.endDate < :to
              AND (:hasUnit = false OR w.unitId = :unitId)
              AND (:hasEquipment = false OR w.equipmentId = :equipmentId)
            ORDER BY w.endDate ASC, w.id ASC
            """)
    List<EquipmentWarranty> findExpiring(@Param("from") Timestamp from,
                                         @Param("to") Timestamp to,
                                         @Param("unitId") Long unitId,
                                         @Param("hasUnit") boolean hasUnit,
                                         @Param("equipmentId") Long equipmentId,
                                         @Param("hasEquipment") boolean hasEquipment);

    /** Active covers for the optional unit scope — counts endpoint denominator. */
    @Query("""
            SELECT COUNT(w) FROM EquipmentWarranty w
            WHERE w.status = true
              AND (:hasUnit = false OR w.unitId = :unitId)
            """)
    long countActive(@Param("unitId") Long unitId, @Param("hasUnit") boolean hasUnit);

    /** Active covers already lapsed before the cutoff (unit-gated). */
    @Query("""
            SELECT COUNT(w) FROM EquipmentWarranty w
            WHERE w.status = true
              AND w.endDate < :cutoff
              AND (:hasUnit = false OR w.unitId = :unitId)
            """)
    long countExpiredBefore(@Param("cutoff") Timestamp cutoff,
                            @Param("unitId") Long unitId,
                            @Param("hasUnit") boolean hasUnit);

    /** Active covers lapsing inside the window [from, to) (unit-gated). */
    @Query("""
            SELECT COUNT(w) FROM EquipmentWarranty w
            WHERE w.status = true
              AND w.endDate >= :from
              AND w.endDate < :to
              AND (:hasUnit = false OR w.unitId = :unitId)
            """)
    long countExpiringBetween(@Param("from") Timestamp from,
                              @Param("to") Timestamp to,
                              @Param("unitId") Long unitId,
                              @Param("hasUnit") boolean hasUnit);

    /** Per-type breakdown of active covers for the counts summary. */
    @Query("""
            SELECT w.warrantyType, COUNT(w)
            FROM EquipmentWarranty w
            WHERE w.status = true
              AND (:hasUnit = false OR w.unitId = :unitId)
            GROUP BY w.warrantyType
            ORDER BY COUNT(w) DESC, w.warrantyType ASC
            """)
    List<Object[]> countActiveGroupedByType(@Param("unitId") Long unitId,
                                            @Param("hasUnit") boolean hasUnit);
}
