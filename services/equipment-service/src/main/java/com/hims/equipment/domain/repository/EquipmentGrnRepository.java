package com.hims.equipment.domain.repository;

import com.hims.equipment.domain.model.EquipmentGrn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EquipmentGrnRepository extends JpaRepository<EquipmentGrn, Long> {
}