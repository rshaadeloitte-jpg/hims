package com.hims.equipment.domain.repository;

import com.hims.equipment.domain.model.EquipmentTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentTransferRepository extends JpaRepository<EquipmentTransfer, Long> {

    /** Chronological transfer history of one asset. */
    List<EquipmentTransfer> findByEquipmentIdOrderByIdAsc(Long equipmentId);
}
