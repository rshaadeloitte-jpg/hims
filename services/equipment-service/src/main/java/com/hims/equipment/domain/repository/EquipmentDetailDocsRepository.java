package com.hims.equipment.domain.repository;

import com.hims.equipment.domain.model.EquipmentDetailDocs;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EquipmentDetailDocsRepository extends JpaRepository<EquipmentDetailDocs, Long> {

    /** Active documents of one asset — newest first (asset document folder). */
    List<EquipmentDetailDocs> findByEquipmentIdAndStatusTrueOrderByIdDesc(Long equipmentId);

    /** Active document by id — already-deleted (status=false) rows read as absent. */
    Optional<EquipmentDetailDocs> findByIdAndStatusTrue(Long id);
}
