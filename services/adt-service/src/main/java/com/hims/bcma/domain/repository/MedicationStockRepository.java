package com.hims.bcma.domain.repository;

import com.hims.bcma.domain.model.MedicationStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicationStockRepository extends JpaRepository<MedicationStock, Long> {
    Optional<MedicationStock> findByStockId(Long stockId);
    List<MedicationStock> findByDrugNdc(String drugNdc);
    List<MedicationStock> findByBatchId(String batchId);
    Optional<MedicationStock> findByDrugIenAndWardId(String drugIen, Long wardId);
    List<MedicationStock> findByDrugIen(String drugIen);
    List<MedicationStock> findByWardId(Long wardId);
}
