package com.hims.bcma.application;

import com.hims.bcma.domain.model.MedicationStock;
import com.hims.bcma.domain.model.StockAlert;
import com.hims.bcma.domain.repository.MedicationStockRepository;
import com.hims.bcma.domain.repository.StockAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StockViewEdgeCasesTest {

    private StockViewService service;
    private MedicationStockRepository stockRepo;
    private StockAlertRepository alertRepo;

    @BeforeEach
    void setUp() {
        stockRepo = mock(MedicationStockRepository.class);
        alertRepo = mock(StockAlertRepository.class);
        service = new StockViewService(stockRepo, alertRepo);
        when(stockRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(alertRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // --- Expiry alerts ---
    @Test
    void createAlert_withExpiryAlertType_savesCorrectly() {
        StockAlert alert = service.createAlert("NDC-EXPIRY", "EXPIRY_ALERT",
                "Medication expires in 7 days", "WARNING");

        assertThat(alert.getAlertType()).isEqualTo("EXPIRY_ALERT");
        assertThat(alert.getDrugNdc()).isEqualTo("NDC-EXPIRY");
        assertThat(alert.getMessage()).isEqualTo("Medication expires in 7 days");
        assertThat(alert.getSeverity()).isEqualTo("WARNING");
        assertThat(alert.getResolved()).isFalse();
    }

    @Test
    void createAlert_withCriticalExpiry_savesWithCriticalSeverity() {
        StockAlert alert = service.createAlert("NDC-CRITICAL", "EXPIRY_ALERT",
                "Medication expired", "CRITICAL");

        assertThat(alert.getSeverity()).isEqualTo("CRITICAL");
        assertThat(alert.getResolved()).isFalse();
    }

    @Test
    void createAlert_withAlreadyExpiredMedication() {
        StockAlert alert = service.createAlert("NDC-EXPIRED", "EXPIRED",
                "Batch B001 expired on 2024-01-01", "CRITICAL");

        assertThat(alert.getAlertType()).isEqualTo("EXPIRED");
        assertThat(alert.getSeverity()).isEqualTo("CRITICAL");
    }

    @Test
    void createAlert_withNearExpiryDate() {
        StockAlert alert = service.createAlert("NDC-NEAR", "NEAR_EXPIRY",
                "Expires tomorrow", "WARNING");

        assertThat(alert.getAlertType()).isEqualTo("NEAR_EXPIRY");
    }

    // --- Low-stock thresholds ---
    @Test
    void createAlert_withLowStockThreshold_savesCorrectly() {
        StockAlert alert = service.createAlert("NDC-LOW", "LOW_STOCK",
                "Stock below minimum threshold of 10 units", "WARNING");

        assertThat(alert.getAlertType()).isEqualTo("LOW_STOCK");
        assertThat(alert.getMessage()).contains("below minimum threshold");
    }

    @Test
    void createAlert_withCriticalLowStock() {
        StockAlert alert = service.createAlert("NDC-CRIT-LOW", "CRITICAL_LOW",
                "Only 2 units remaining", "CRITICAL");

        assertThat(alert.getSeverity()).isEqualTo("CRITICAL");
    }

    @Test
    void createAlert_withZeroStock() {
        StockAlert alert = service.createAlert("NDC-ZERO", "OUT_OF_STOCK",
                "No units available", "CRITICAL");

        assertThat(alert.getAlertType()).isEqualTo("OUT_OF_STOCK");
    }

    @Test
    void updateStock_withLowQuantity_triggersLowStockScenario() {
        MedicationStock stock = service.updateStock(1L, "NDC-LOW-Q", "B001",
                BigDecimal.valueOf(3), LocalDate.now().plusMonths(6), "FRIDGE-A");

        assertThat(stock.getQuantityOnHand()).isEqualByComparingTo(BigDecimal.valueOf(3));
    }

    // --- Batch recall ---
    @Test
    void createAlert_withBatchRecall_savesCorrectly() {
        StockAlert alert = service.createAlert("NDC-RECALL", "BATCH_RECALL",
                "Batch B999 recalled by manufacturer", "CRITICAL");

        assertThat(alert.getAlertType()).isEqualTo("BATCH_RECALL");
        assertThat(alert.getMessage()).contains("recalled");
    }

    @Test
    void createAlert_withBatchRecallForSpecificBatch() {
        StockAlert alert = service.createAlert("NDC-RECALL-2", "BATCH_RECALL",
                "Batch B888 contaminated", "CRITICAL");

        assertThat(alert.getSeverity()).isEqualTo("CRITICAL");
    }

    @Test
    void updateStock_withRecalledBatch_stillUpdates() {
        MedicationStock stock = service.updateStock(2L, "NDC-RECALL", "B999",
                BigDecimal.valueOf(50), LocalDate.now().plusMonths(3), "CABINET-1");

        assertThat(stock.getBatchId()).isEqualTo("B999");
        assertThat(stock.getDrugNdc()).isEqualTo("NDC-RECALL");
    }

    // --- Location-based queries ---
    @Test
    void findByDrug_returnsStocksAcrossLocations() {
        MedicationStock s1 = new MedicationStock();
        s1.setStockId(1L);
        s1.setStorageLocation("FRIDGE-A");
        MedicationStock s2 = new MedicationStock();
        s2.setStockId(2L);
        s2.setStorageLocation("CABINET-B");
        when(stockRepo.findByDrugNdc("NDC-LOC")).thenReturn(List.of(s1, s2));

        List<MedicationStock> result = service.findByDrug("NDC-LOC");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStorageLocation()).isEqualTo("FRIDGE-A");
        assertThat(result.get(1).getStorageLocation()).isEqualTo("CABINET-B");
    }

    @Test
    void findByDrug_returnsEmptyWhenDrugNotFound() {
        when(stockRepo.findByDrugNdc("NDC-NOT-FOUND")).thenReturn(List.of());

        List<MedicationStock> result = service.findByDrug("NDC-NOT-FOUND");

        assertThat(result).isEmpty();
    }

    @Test
    void updateStock_withSpecificLocation_savesLocation() {
        MedicationStock stock = service.updateStock(3L, "NDC-LOC", "B001",
                BigDecimal.valueOf(100), LocalDate.now().plusMonths(6), "WARD-5-FRIDGE");

        assertThat(stock.getStorageLocation()).isEqualTo("WARD-5-FRIDGE");
    }

    @Test
    void updateStock_withNullLocation_savesNull() {
        MedicationStock stock = service.updateStock(4L, "NDC-NOLOC", "B002",
                BigDecimal.valueOf(50), LocalDate.now().plusMonths(6), null);

        assertThat(stock.getStorageLocation()).isNull();
    }

    // --- Real-time decrement ---
    @Test
    void updateStock_decrementsQuantity() {
        MedicationStock existing = new MedicationStock();
        existing.setStockId(5L);
        existing.setQuantityOnHand(BigDecimal.valueOf(100));
        when(stockRepo.findByStockId(5L)).thenReturn(Optional.of(existing));

        MedicationStock updated = service.updateStock(5L, "NDC-DEC", "B001",
                BigDecimal.valueOf(95), LocalDate.now().plusMonths(6), "FRIDGE");

        assertThat(updated.getQuantityOnHand()).isEqualByComparingTo(BigDecimal.valueOf(95));
    }

    @Test
    void updateStock_decrementsToZero() {
        MedicationStock existing = new MedicationStock();
        existing.setStockId(6L);
        existing.setQuantityOnHand(BigDecimal.valueOf(1));
        when(stockRepo.findByStockId(6L)).thenReturn(Optional.of(existing));

        MedicationStock updated = service.updateStock(6L, "NDC-ZERO", "B001",
                BigDecimal.ZERO, LocalDate.now().plusMonths(6), "FRIDGE");

        assertThat(updated.getQuantityOnHand()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void updateStock_setsLastUpdatedTimestamp() {
        Timestamp before = Timestamp.from(Instant.now());
        MedicationStock stock = service.updateStock(7L, "NDC-TIME", "B001",
                BigDecimal.valueOf(50), LocalDate.now().plusMonths(6), "FRIDGE");
        Timestamp after = Timestamp.from(Instant.now());

        assertThat(stock.getLastUpdated()).isBetween(before, after);
    }

    @Test
    void updateStock_withNewStockId_createsNewRecord() {
        MedicationStock stock = service.updateStock(8L, "NDC-NEW", "B001",
                BigDecimal.valueOf(200), LocalDate.now().plusMonths(12), "STORAGE");

        assertThat(stock.getStockId()).isEqualTo(8L);
        assertThat(stock.getDrugNdc()).isEqualTo("NDC-NEW");
    }

    // --- Alert resolution ---
    @Test
    void resolveAlert_setsResolvedTrueAndTimestamp() {
        StockAlert alert = new StockAlert();
        alert.setAlertId(1L);
        alert.setResolved(false);
        when(alertRepo.findByAlertId(1L)).thenReturn(Optional.of(alert));

        Timestamp before = Timestamp.from(Instant.now());
        StockAlert resolved = service.resolveAlert(1L);
        Timestamp after = Timestamp.from(Instant.now());

        assertThat(resolved.getResolved()).isTrue();
        assertThat(resolved.getResolvedAt()).isBetween(before, after);
    }

    @Test
    void resolveAlert_whenNotFound_throwsIllegalArgumentException() {
        when(alertRepo.findByAlertId(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveAlert(404L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Alert not found");
    }

    @Test
    void resolveAlert_canOnlyResolveOnce() {
        StockAlert alert = new StockAlert();
        alert.setAlertId(2L);
        alert.setResolved(false);
        when(alertRepo.findByAlertId(2L)).thenReturn(Optional.of(alert));

        StockAlert first = service.resolveAlert(2L);
        assertThat(first.getResolved()).isTrue();

        // Second resolve should still work (idempotent)
        StockAlert second = service.resolveAlert(2L);
        assertThat(second.getResolved()).isTrue();
    }

    // --- Unresolved alerts ---
    @Test
    void findUnresolved_returnsOnlyUnresolvedAlerts() {
        StockAlert a1 = new StockAlert();
        a1.setAlertId(1L);
        a1.setResolved(false);
        StockAlert a2 = new StockAlert();
        a2.setAlertId(2L);
        a2.setResolved(false);
        when(alertRepo.findByResolved(false)).thenReturn(List.of(a1, a2));

        List<StockAlert> result = service.findUnresolved();

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(a -> !a.getResolved());
    }

    @Test
    void findUnresolved_returnsEmptyWhenAllResolved() {
        when(alertRepo.findByResolved(false)).thenReturn(List.of());

        List<StockAlert> result = service.findUnresolved();

        assertThat(result).isEmpty();
    }

    // --- Alert severity defaults ---
    @Test
    void createAlert_withNullSeverity_defaultsToWarning() {
        StockAlert alert = service.createAlert("NDC-DEF", "TYPE", "msg", null);

        assertThat(alert.getSeverity()).isEqualTo("WARNING");
    }

    @Test
    void createAlert_withInfoSeverity() {
        StockAlert alert = service.createAlert("NDC-INFO", "INFO", "informational", "INFO");

        assertThat(alert.getSeverity()).isEqualTo("INFO");
    }

    // --- Stock with various quantities ---
    @Test
    void updateStock_withLargeQuantity_succeeds() {
        MedicationStock stock = service.updateStock(9L, "NDC-BULK", "B001",
                BigDecimal.valueOf(10000), LocalDate.now().plusYears(2), "WAREHOUSE");

        assertThat(stock.getQuantityOnHand()).isEqualByComparingTo(BigDecimal.valueOf(10000));
    }

    @Test
    void updateStock_withFractionalQuantity_succeeds() {
        MedicationStock stock = service.updateStock(10L, "NDC-FRAC", "B001",
                BigDecimal.valueOf(2.5), LocalDate.now().plusMonths(6), "FRIDGE");

        assertThat(stock.getQuantityOnHand()).isEqualByComparingTo(BigDecimal.valueOf(2.5));
    }

    @Test
    void updateStock_withPastExpiryDate_savesAnyway() {
        MedicationStock stock = service.updateStock(11L, "NDC-PAST", "B001",
                BigDecimal.valueOf(50), LocalDate.now().minusDays(30), "FRIDGE");

        assertThat(stock.getExpiryDate()).isBefore(LocalDate.now());
    }

    @Test
    void updateStock_withNullExpiryDate_savesNull() {
        MedicationStock stock = service.updateStock(12L, "NDC-NOEXP", "B001",
                BigDecimal.valueOf(50), null, "FRIDGE");

        assertThat(stock.getExpiryDate()).isNull();
    }

    @Test
    void updateStock_withNullQuantity_savesNull() {
        MedicationStock stock = service.updateStock(13L, "NDC-NULLQ", "B001",
                null, LocalDate.now().plusMonths(6), "FRIDGE");

        assertThat(stock.getQuantityOnHand()).isNull();
    }

    @Test
    void createAlert_withNullMessage_savesNull() {
        StockAlert alert = service.createAlert("NDC-NULL-MSG", "TYPE", null, "WARNING");

        assertThat(alert.getMessage()).isNull();
    }

    @Test
    void createAlert_savesToRepository() {
        service.createAlert("NDC-SAVE", "TYPE", "message", "WARNING");

        verify(alertRepo).save(argThat(a ->
                a.getDrugNdc().equals("NDC-SAVE") &&
                a.getAlertType().equals("TYPE") &&
                a.getResolved().equals(false)));
    }

    @Test
    void updateStock_savesToRepository() {
        service.updateStock(14L, "NDC-SAVE", "B001",
                BigDecimal.valueOf(100), LocalDate.now().plusMonths(6), "FRIDGE");

        verify(stockRepo).save(argThat(s ->
                s.getDrugNdc().equals("NDC-SAVE") &&
                s.getBatchId().equals("B001")));
    }
}
