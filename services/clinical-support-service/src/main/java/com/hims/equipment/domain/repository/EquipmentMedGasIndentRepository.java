package com.hims.equipment.domain.repository;
import com.hims.equipment.domain.model.EquipmentMedGasIndent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface EquipmentMedGasIndentRepository extends JpaRepository<EquipmentMedGasIndent, Long> {}