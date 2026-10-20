package com.hims.equipment.domain.repository;

import com.hims.equipment.domain.model.EquipmentUsageDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;

public interface EquipmentUsageDetailsRepository extends JpaRepository<EquipmentUsageDetails, Long> {

    /** Active usage entries of one asset — newest first (asset usage history). */
    List<EquipmentUsageDetails> findByEquipmentIdAndStatusTrueOrderByIdDesc(Long equipmentId);

    /**
     * Report window: active usage entries whose {@code usage_date} falls in
     * [from, to) — chronological (oldest first) so the report reads as a
     * timeline. Rows with a NULL usage_date are naturally excluded (an entry
     * without a timestamp cannot sit inside a date range). Optional unit /
     * asset filters are gated by booleans (same sentinel convention as
     * EquipmentRepository.search / EquipmentWarrantyRepository.findExpiring).
     */
    @Query("""
            SELECT u FROM EquipmentUsageDetails u
            WHERE u.status = true
              AND u.usageDate >= :from
              AND u.usageDate < :to
              AND (:hasUnit = false OR u.unitId = :unitId)
              AND (:hasEquipment = false OR u.equipmentId = :equipmentId)
            ORDER BY u.usageDate ASC, u.id ASC
            """)
    List<EquipmentUsageDetails> findReportRows(@Param("from") Timestamp from,
                                               @Param("to") Timestamp to,
                                               @Param("unitId") Long unitId,
                                               @Param("hasUnit") boolean hasUnit,
                                               @Param("equipmentId") Long equipmentId,
                                               @Param("hasEquipment") boolean hasEquipment);
}
