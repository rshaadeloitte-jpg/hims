package com.hims.item.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.item.application.StockService;
import com.hims.item.configuration.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for the single-writer stock API (default-deny enforced). */
@WebMvcTest(StockController.class)
@Import({SecurityConfig.class, StockService.class})
@AutoConfigureMockMvc
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private com.hims.item.domain.repository.ItemStockBalanceRepository balanceRepository;

    @MockitoBean
    private com.hims.item.domain.repository.ItemStockLedgerRepository ledgerRepository;

    @MockitoBean
    private com.hims.item.domain.repository.ItemRepository itemRepository;

    @MockitoBean
    private com.hims.item.domain.repository.StoreRepository storeRepository;

    @MockitoBean
    private com.hims.item.domain.repository.ItemBatchRepository batchRepository;

    @MockitoBean
    private com.hims.item.infrastructure.outbox.OutboxService outboxService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void movement_requiresAuth_defaultDeny() throws Exception {
        mockMvc.perform(post("/api/v1/stock/movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void movement_validRequest_returnsBalance() throws Exception {
        com.hims.item.domain.model.ItemStockBalance b = new com.hims.item.domain.model.ItemStockBalance();
        b.setId(1L);
        b.setStoreId(10L);
        b.setItemId(20L);
        when(storeRepository.existsById(10L)).thenReturn(true);
        when(itemRepository.existsById(20L)).thenReturn(true);
        when(balanceRepository.findByStoreItemBatchForUpdate(anyLong(), anyLong(), isNull()))
                .thenReturn(java.util.Optional.of(b));
        when(balanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ledgerRepository.save(any())).thenAnswer(inv -> {
            com.hims.item.domain.model.ItemStockLedger l = inv.getArgument(0);
            l.setId(900L);
            return l;
        });

        mockMvc.perform(post("/api/v1/stock/movements")
                        .with(user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeId\":10,\"itemId\":20,\"movementType\":2,\"operation\":1,\"quantity\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeId").value(10))
                .andExpect(jsonPath("$.itemId").value(20));
    }

    @Test
    void movement_invalidQuantity_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/stock/movements")
                        .with(user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeId\":10,\"itemId\":20,\"movementType\":2,\"operation\":1,\"quantity\":-3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"));
    }

    @Test
    void balances_and_ledger_requireAuth() throws Exception {
        mockMvc.perform(get("/api/v1/stock/balances"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/stock/ledger?storeId=1&itemId=2"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void availableBatches_returnsFefoView() throws Exception {
        when(storeRepository.existsById(10L)).thenReturn(true);
        when(itemRepository.existsById(20L)).thenReturn(true);
        when(balanceRepository.findAvailableBatchesFefo(10L, 20L)).thenReturn(java.util.List.of(
                new com.hims.item.application.BatchAvailabilityView(
                        301L, "NEAR", java.sql.Date.valueOf("2027-01-31"), new java.math.BigDecimal("100"),
                        null, null, null, null, null, null, false, 20L, "Ceftriaxone 1g", "CEF1")));

        mockMvc.perform(get("/api/v1/stock/available-batches")
                        .param("storeId", "10").param("itemId", "20").with(user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].batchCode").value("NEAR"))
                .andExpect(jsonPath("$[0].availableQty").value(100))
                .andExpect(jsonPath("$[0].itemName").value("Ceftriaxone 1g"));
    }

    @Test
    void availableBatches_missingParams_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/stock/available-batches").with(user("devops")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void availableBatches_requiresAuth() throws Exception {
        mockMvc.perform(get("/api/v1/stock/available-batches")
                        .param("storeId", "10").param("itemId", "20"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unmappedPath_returns404_not500() throws Exception {
        mockMvc.perform(get("/api/v1/does-not-exist").with(user("devops")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    // ------------------------------------------------------------- M13 adjustment / consumption

    @Test
    void adjustment_requiresAuth_defaultDeny() throws Exception {
        mockMvc.perform(post("/api/v1/stock/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeId\":10,\"itemId\":20,\"quantity\":5,\"operation\":1,\"reason\":\"excess\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adjustment_valid_returnsBalance() throws Exception {
        com.hims.item.domain.model.ItemStockBalance b = new com.hims.item.domain.model.ItemStockBalance();
        b.setId(1L); b.setStoreId(10L); b.setItemId(20L);
        b.setQtyInHand(new java.math.BigDecimal("105")); b.setAvailableQty(new java.math.BigDecimal("105"));
        when(storeRepository.existsById(10L)).thenReturn(true);
        when(itemRepository.existsById(20L)).thenReturn(true);
        when(balanceRepository.findByStoreItemBatchForUpdate(anyLong(), anyLong(), isNull()))
                .thenReturn(java.util.Optional.of(b));
        when(balanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ledgerRepository.save(any())).thenAnswer(inv -> {
            com.hims.item.domain.model.ItemStockLedger l = inv.getArgument(0); l.setId(900L); return l;
        });

        mockMvc.perform(post("/api/v1/stock/adjustments").with(user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeId\":10,\"itemId\":20,\"quantity\":5,\"operation\":1,\"reason\":\"physical count excess\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeId").value(10))
                .andExpect(jsonPath("$.itemId").value(20));
    }

    @Test
    void adjustment_missingReason_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/stock/adjustments").with(user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeId\":10,\"itemId\":20,\"quantity\":5,\"operation\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"));
    }

    @Test
    void consumption_valid_returnsBalance() throws Exception {
        com.hims.item.domain.model.ItemStockBalance b = new com.hims.item.domain.model.ItemStockBalance();
        b.setId(1L); b.setStoreId(10L); b.setItemId(20L);
        b.setQtyInHand(new java.math.BigDecimal("45")); b.setAvailableQty(new java.math.BigDecimal("45"));
        when(storeRepository.existsById(10L)).thenReturn(true);
        when(itemRepository.existsById(20L)).thenReturn(true);
        when(balanceRepository.findByStoreItemBatchForUpdate(anyLong(), anyLong(), isNull()))
                .thenReturn(java.util.Optional.of(b));
        when(balanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ledgerRepository.save(any())).thenAnswer(inv -> {
            com.hims.item.domain.model.ItemStockLedger l = inv.getArgument(0); l.setId(901L); return l;
        });

        mockMvc.perform(post("/api/v1/stock/consumptions").with(user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeId\":10,\"itemId\":20,\"quantity\":5,\"reason\":\"ward consumption\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeId").value(10));
    }
}
