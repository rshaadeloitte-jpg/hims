package com.hims.equipment.domain.repository;
import com.hims.equipment.domain.model.EquipmentScrap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface EquipmentScrapRepository extends JpaRepository<EquipmentScrap, Long> {}