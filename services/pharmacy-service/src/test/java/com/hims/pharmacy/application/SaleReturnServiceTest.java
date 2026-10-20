package com.hims.pharmacy.application;

import com.hims.pharmacy.application.exceptions.IllegalSaleStateException;
import com.hims.pharmacy.application.exceptions.InvalidSaleRequestException;
import com.hims.pharmacy.application.exceptions.ReturnNotFoundException;
import com.hims.pharmacy.domain.model.ItemSale;
import com.hims.pharmacy.domain.model.ItemSaleDetail;
import com.hims.pharmacy.domain.model.SaleReturn;
import com.hims.pharmacy.domain.model.SaleReturnLine;
import com.hims.pharmacy.domain.repository.ItemSaleRepository;
import com.hims.pharmacy.domain.repository.SaleReturnLineRepository;
import com.hims.pharmacy.domain.repository.SaleReturnRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaleReturnServiceTest {

    SaleReturnRepository returnRepo;
    SaleReturnLineRepository lineRepo;
    ItemSaleRepository saleRepo;
    ItemStockClient stockClient;
    SaleReturnService service;
    AtomicLong seq = new AtomicLong(0);

    @BeforeEach
    void setUp() {
        returnRepo = mock(SaleReturnRepository.class);
        lineRepo = mock(SaleReturnLineRepository.class);
        saleRepo = mock(ItemSaleRepository.class);
        stockClient = mock(ItemStockClient.class);

        EntityManager em = mock(EntityManager.class);
        Query seqQuery = mock(Query.class);
        when(seqQuery.getSingleResult()).thenAnswer(inv -> seq.incrementAndGet());
        when(em.createNativeQuery(contains("nextval"))).thenReturn(seqQuery);

        service = new SaleReturnService(returnRepo, lineRepo, saleRepo, stockClient, 1L);
        // Inject the mocked EntityManager
        org.springframework.test.util.ReflectionTestUtils.setField(service, "em", em);

        when(returnRepo.save(any(SaleReturn.class))).thenAnswer(inv -> {
            SaleReturn r = inv.getArgument(0);
            return r;
        });
        when(lineRepo.save(any(SaleReturnLine.class))).thenAnswer(inv -> {
            SaleReturnLine l = inv.getArgument(0);
            return l;
        });
    }

    private ItemSale sale(long id, long patientId, short status, List<ItemSaleDetail> details) {
        ItemSale s = new ItemSale();
        s.setSaleId(id);
        s.setPatientId(patientId);
        s.setStatusCode(status);
        s.setDetails(details);
        return s;
    }

    @Test
    void initiate_validSale_createsHeaderAndLines() {
        ItemSaleDetail detail = new ItemSaleDetail();
        detail.setItemId(5L);
        detail.setQty(5);
        when(saleRepo.findById(100L)).thenReturn(Optional.of(sale(100L, 50L, ItemSale.STATUS_FULLY_DISPENSED, List.of(detail))));

        SaleReturn ret = service.initiate(100L, 50L, "DAMAGED", "smashed box",
                List.of(new SaleReturnService.LineInput(5L, "B1", 2, new BigDecimal("12.50"), "broken")));

        assertThat(ret.getStatusCode()).isEqualTo(SaleReturn.STATUS_INITIATED);
        assertThat(ret.getOriginalSaleId()).isEqualTo(100L);
        assertThat(ret.getPatientId()).isEqualTo(50L);
        assertThat(ret.getTotalRefundAmount()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(ret.getLines()).hasSize(1);
        assertThat(ret.getLines().get(0).getRefundAmount()).isEqualByComparingTo(new BigDecimal("25.00"));
        verify(returnRepo, times(1)).save(any());
        verify(lineRepo, times(1)).save(any());
    }

    @Test
    void initiate_unknownSale_throws404() {
        when(saleRepo.findById(100L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.initiate(100L, 50L, "DAMAGED", null,
                List.of(new SaleReturnService.LineInput(5L, null, 1, BigDecimal.ONE, null))))
                .isInstanceOf(com.hims.pharmacy.application.exceptions.SaleNotFoundException.class);
        verify(returnRepo, never()).save(any());
    }

    @Test
    void initiate_patientMismatch_throws400() {
        when(saleRepo.findById(100L)).thenReturn(Optional.of(sale(100L, 999L, ItemSale.STATUS_FULLY_DISPENSED, List.of(new ItemSaleDetail()))));
        assertThatThrownBy(() -> service.initiate(100L, 50L, "DAMAGED", null,
                List.of(new SaleReturnService.LineInput(5L, null, 1, BigDecimal.ONE, null))))
                .isInstanceOf(InvalidSaleRequestException.class)
                .hasMessageContaining("patientId");
    }

    @Test
    void initiate_badReason_throws400() {
        when(saleRepo.findById(100L)).thenReturn(Optional.of(sale(100L, 50L, ItemSale.STATUS_FULLY_DISPENSED, List.of(new ItemSaleDetail()))));
        assertThatThrownBy(() -> service.initiate(100L, 50L, "BOGUS", null,
                List.of(new SaleReturnService.LineInput(5L, null, 1, BigDecimal.ONE, null))))
                .isInstanceOf(InvalidSaleRequestException.class)
                .hasMessageContaining("returnReason");
    }

    @Test
    void initiate_emptyLines_throws400() {
        when(saleRepo.findById(100L)).thenReturn(Optional.of(sale(100L, 50L, ItemSale.STATUS_FULLY_DISPENSED, List.of(new ItemSaleDetail()))));
        assertThatThrownBy(() -> service.initiate(100L, 50L, "DAMAGED", null, List.of()))
                .isInstanceOf(InvalidSaleRequestException.class)
                .hasMessageContaining("At least one");
    }

    @Test
    void initiate_zeroQty_throws400() {
        when(saleRepo.findById(100L)).thenReturn(Optional.of(sale(100L, 50L, ItemSale.STATUS_FULLY_DISPENSED, List.of(new ItemSaleDetail()))));
        assertThatThrownBy(() -> service.initiate(100L, 50L, "DAMAGED", null,
                List.of(new SaleReturnService.LineInput(5L, null, 0, BigDecimal.ONE, null))))
                .isInstanceOf(InvalidSaleRequestException.class)
                .hasMessageContaining("returnQty");
    }

    @Test
    void complete_initiated_callsStockAndSetsCompleted() {
        SaleReturn ret = new SaleReturn();
        ret.setReturnId(1L);
        ret.setStatusCode(SaleReturn.STATUS_INITIATED);
        ret.setTotalRefundAmount(new BigDecimal("25.00"));
        when(returnRepo.findById(1L)).thenReturn(Optional.of(ret));

        SaleReturnLine line = new SaleReturnLine();
        line.setReturnId(1L);
        line.setLineNo(1L);
        line.setItemId(5L);
        line.setBatchId("B1");
        line.setReturnQty(2);
        line.setUnitPrice(new BigDecimal("12.50"));
        line.setRefundAmount(new BigDecimal("25.00"));
        when(lineRepo.findByReturnId(1L)).thenReturn(List.of(line));

        SaleReturn completed = service.complete(1L, 9L, null);
        assertThat(completed.getStatusCode()).isEqualTo(SaleReturn.STATUS_COMPLETED);
        assertThat(completed.getApprovedBy()).isEqualTo(9L);
        verify(stockClient, times(1)).returnForSale(anyLong(), anyLong(), anyString(), anyInt(), anyLong(), anyLong());
    }

    @Test
    void complete_alreadyCompleted_throws400() {
        SaleReturn ret = new SaleReturn();
        ret.setReturnId(1L);
        ret.setStatusCode(SaleReturn.STATUS_COMPLETED);
        when(returnRepo.findById(1L)).thenReturn(Optional.of(ret));
        assertThatThrownBy(() -> service.complete(1L, 9L, null))
                .isInstanceOf(IllegalSaleStateException.class);
        verify(stockClient, never()).returnForSale(anyLong(), anyLong(), anyString(), anyInt(), anyLong(), anyLong());
    }

    @Test
    void complete_unknownReturn_throws404() {
        when(returnRepo.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.complete(999L, 9L, null))
                .isInstanceOf(ReturnNotFoundException.class);
    }

    @Test
    void reject_initiated_setsRejected() {
        SaleReturn ret = new SaleReturn();
        ret.setReturnId(1L);
        ret.setStatusCode(SaleReturn.STATUS_INITIATED);
        when(returnRepo.findById(1L)).thenReturn(Optional.of(ret));
        SaleReturn rejected = service.reject(1L, "out of policy");
        assertThat(rejected.getStatusCode()).isEqualTo(SaleReturn.STATUS_REJECTED);
        assertThat(rejected.getRemarks()).isEqualTo("out of policy");
        verify(stockClient, never()).returnForSale(anyLong(), anyLong(), anyString(), anyInt(), anyLong(), anyLong());
    }

    @Test
    void reject_unknownReturn_throws404() {
        when(returnRepo.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.reject(999L, null)).isInstanceOf(ReturnNotFoundException.class);
    }
}
