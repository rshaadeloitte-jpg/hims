package com.hims.bcma.application;

import com.hims.bcma.domain.model.*;
import com.hims.bcma.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class StockViewService {

    private final MedicationStockRepository stockRepo;
    private final StockAlertRepository alertRepo;

    public StockViewService(MedicationStockRepository stockRepo, StockAlertRepository alertRepo) {
        this.stockRepo = stockRepo;
        this.alertRepo = alertRepo;
    }

    @Transactional
    public MedicationStock updateStock(Long stockId, String drugNdc, String batchId, BigDecimal quantityOnHand, LocalDate expiryDate, String storageLocation) {
        MedicationStock s = stockRepo.findByStockId(stockId).orElse(new MedicationStock());
        s.setStockId(stockId != null ? stockId : Instant.now().toEpochMilli());
        s.setDrugNdc(drugNdc);
        s.setBatchId(batchId);
        s.setQuantityOnHand(quantityOnHand);
        s.setExpiryDate(expiryDate);
        s.setStorageLocation(storageLocation);
        s.setLastUpdated(Timestamp.from(Instant.now()));
        return stockRepo.save(s);
    }

    @Transactional
    public StockAlert createAlert(String drugNdc, String alertType, String message, String severity) {
        StockAlert a = new StockAlert();
        a.setAlertId(Instant.now().toEpochMilli());
        a.setDrugNdc(drugNdc);
        a.setAlertType(alertType);
        a.setMessage(message);
        a.setSeverity(severity != null ? severity : "WARNING");
        a.setResolved(false);
        return alertRepo.save(a);
    }

    @Transactional
    public StockAlert resolveAlert(Long alertId) {
        StockAlert a = alertRepo.findByAlertId(alertId).orElseThrow(() -> new IllegalArgumentException("Alert not found"));
        a.setResolved(true);
        a.setResolvedAt(Timestamp.from(Instant.now()));
        return alertRepo.save(a);
    }

    public List<MedicationStock> findByDrug(String drugNdc) {
        return stockRepo.findByDrugNdc(drugNdc);
    }

    public List<StockAlert> findUnresolved() {
        return alertRepo.findByResolved(false);
    }
}
