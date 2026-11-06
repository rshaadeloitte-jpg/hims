package com.hims.equipment.domain.repository;

import com.hims.equipment.domain.model.EquipmentOpeningBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EquipmentOpeningBalanceRepository extends JpaRepository<EquipmentOpeningBalance, Long> {

    /**
     * Opening-balance register: active rows optionally scoped by unit and/or
     * item (the ix_equipment_opening_balance_unit_item index covers exactly
     * this shape). Ordered by item then id so the listing reads as a
     * per-item stock register rather than an insertion log. Optional filters
     * are gated by booleans (same sentinel convention as
     * EquipmentRepository.search / EquipmentUsageDetailsRepository.findReportRows).
     */
    @Query("""
            SELECT b FROM EquipmentOpeningBalance b
            WHERE b.status = true
              AND (:hasUnit = false OR b.unitId = :unitId)
              AND (:hasItem = false OR b.itemId = :itemId)
            ORDER BY b.itemId ASC, b.id ASC
            """)
    List<EquipmentOpeningBalance> findRegisterRows(@Param("unitId") Long unitId,
                                                   @Param("hasUnit") boolean hasUnit,
                                                   @Param("itemId") Long itemId,
                                                   @Param("hasItem") boolean hasItem);
}
