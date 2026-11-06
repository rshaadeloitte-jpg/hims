package com.hims.equipment.domain.repository;

import com.hims.equipment.domain.model.EquipmentComplaintResolutionDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentComplaintResolutionDetailsRepository extends JpaRepository<EquipmentComplaintResolutionDetails, Long> {

    /** Line items of one resolution — entry order. */
    List<EquipmentComplaintResolutionDetails> findByResolutionIdOrderByIdAsc(Long resolutionId);
}
