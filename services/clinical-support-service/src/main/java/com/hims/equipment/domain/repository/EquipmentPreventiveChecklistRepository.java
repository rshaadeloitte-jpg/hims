package com.hims.equipment.domain.repository;

import com.hims.equipment.domain.model.EquipmentPreventiveChecklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EquipmentPreventiveChecklistRepository extends JpaRepository<EquipmentPreventiveChecklist, Long> {

    /** All active checklist templates — template order (oldest first). */
    List<EquipmentPreventiveChecklist> findByStatusTrueOrderByIdAsc();

    /** Case-insensitive code lookup (duplicate guard on create). */
    Optional<EquipmentPreventiveChecklist> findByCodeIgnoreCase(String code);
}
