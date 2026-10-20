package com.hims.bcma.domain.repository;

import com.hims.bcma.domain.model.StockAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockAlertRepository extends JpaRepository<StockAlert, Long> {
    Optional<StockAlert> findByAlertId(Long alertId);
    List<StockAlert> findByDrugNdc(String drugNdc);
    List<StockAlert> findByResolved(Boolean resolved);
}
