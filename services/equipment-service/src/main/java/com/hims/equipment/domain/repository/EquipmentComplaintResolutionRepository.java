package com.hims.equipment.domain.repository;

import com.hims.equipment.domain.model.EquipmentComplaintResolution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentComplaintResolutionRepository extends JpaRepository<EquipmentComplaintResolution, Long> {

    /** Resolutions applied to one complaint — chronological (oldest first). */
    List<EquipmentComplaintResolution> findByComplaintIdOrderByIdAsc(Long complaintId);
}
