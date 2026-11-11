package com.hims.bcma.application;

import com.hims.bcma.domain.model.MedicationStock;
import com.hims.bcma.domain.model.StockAlert;
import com.hims.bcma.domain.repository.MedicationStockRepository;
import com.hims.bcma.domain.repository.StockAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StockViewServiceTest {

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

    @Test
    void updateStockCreatesRecord() {
        MedicationStock s = service.updateStock(1L, "NDC-1", "B001", BigDecimal.valueOf(100), LocalDate.now().plusMonths(6), "FRIDGE");
        assertThat(s.getDrugNdc()).isEqualTo("NDC-1");
    }

    @Test
    void createAlertSuccess() {
        StockAlert a = service.createAlert("NDC-1", "LOW_STOCK", "Below threshold", "WARNING");
        assertThat(a.getAlertType()).isEqualTo("LOW_STOCK");
        assertThat(a.getResolved()).isFalse();
    }

    @Test
    void resolveAlertSetsResolved() {
        StockAlert a = new StockAlert(); a.setAlertId(1L); a.setResolved(false);
        when(alertRepo.findByAlertId(1L)).thenReturn(Optional.of(a));
        StockAlert done = service.resolveAlert(1L);
        assertThat(done.getResolved()).isTrue();
    }

    @Test
    void findByDrugReturnsEmpty() {
        when(stockRepo.findByDrugNdc("NONE")).thenReturn(Collections.emptyList());
        assertThat(service.findByDrug("NONE")).isEmpty();
    }
}
