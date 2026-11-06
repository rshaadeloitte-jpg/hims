package com.hims.equipment.domain.repository;

import com.hims.equipment.domain.model.EquipmentComplaintDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentComplaintDetailsRepository extends JpaRepository<EquipmentComplaintDetails, Long> {

    /** Line items of one complaint — oldest first (entry order). */
    List<EquipmentComplaintDetails> findByComplaintIdOrderByIdAsc(Long complaintId);
}
