package com.hims.item.api;

import com.hims.item.application.GrnService;
import com.hims.item.configuration.SecurityConfig;
import com.hims.item.domain.model.Grn;
import com.hims.item.infrastructure.outbox.OutboxService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests for GRN posting (WP-2): one tx per GRN, every line through the
 * D-1 single-writer path (applyMovement), one GRNPosted outbox event.
 */
@WebMvcTest(GrnController.class)
@Import({SecurityConfig.class, GrnService.class})
@AutoConfigureMockMvc
class GrnControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private com.hims.item.domain.repository.GrnRepository grnRepository;

    @MockitoBean
    private com.hims.item.domain.repository.GrnDetailRepository grnDetailRepository;

    @MockitoBean
    private com.hims.item.domain.repository.ItemBatchRepository batchRepository;

    @MockitoBean
    private com.hims.item.domain.repository.ItemRepository itemRepository;

    @MockitoBean
    private com.hims.item.domain.repository.StoreRepository storeRepository;

    @MockitoBean
    private com.hims.item.domain.repository.SupplierRepository supplierRepository;

    @MockitoBean
    private com.hims.item.application.StockService stockService;

    @MockitoBean
    private OutboxService outboxService;


    @Test
    void post_requiresAuth_defaultDeny() throws Exception {
        mockMvc.perform(post("/api/v1/grn")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void post_valid_grn_movesStock_perLine_andEmitsGRNPosted() throws Exception {
        when(grnRepository.nextGrnId()).thenReturn(501L);
        when(grnDetailRepository.nextGrnDetailId()).thenReturn(502L);
        when(storeRepository.existsById(10L)).thenReturn(true);
        when(supplierRepository.existsById(30L)).thenReturn(true);
        when(itemRepository.existsById(20L)).thenReturn(true);
        when(itemRepository.existsById(21L)).thenReturn(true);
        when(batchRepository.existsById(300L)).thenReturn(true);
        when(batchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(grnRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(grnDetailRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(stockService.applyMovement(anyLong(), anyLong(), any(), any(), anyShort(),
                anyShort(), any(), anyString(), anyLong(), any()))
                .thenAnswer(inv -> new com.hims.item.domain.model.ItemStockBalance());

        String body = """
                {"storeId":10,"supplierId":30,
                 "lines":[
                   {"itemId":20,"quantity":100,"batchId":300},
                   {"itemId":21,"quantity":25,"batchCode":"B-NEW","expiryDate":"2027-12-31"}
                 ]}
                """;

        mockMvc.perform(post("/api/v1/grn").with(user("devops"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grnNo").value("GRN-501"))
                .andExpect(jsonPath("$.storeId").value(10));

        // two lines -> two D-1 movements, first ADDs to the referenced batch
        verify(stockService, times(2)).applyMovement(eq(10L), anyLong(), any(), any(),
                eq(GrnService.MOVEMENT_GRN_RECEIPT), eq(com.hims.item.application.StockService.OP_ADD),
                any(), eq("GRN"), eq(501L), any());

        ArgumentCaptor<String> eventType = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> aggregateId = ArgumentCaptor.forClass(Long.class);
        verify(outboxService).append(eventType.capture(), anyString(), aggregateId.capture(),
                anyString(), anyString(), anyString());
        assertEquals("GRNPosted", eventType.getValue());
        assertEquals(501L, aggregateId.getValue());
    }

    @Test
    void post_emptyLines_returns400() throws Exception {
        when(storeRepository.existsById(10L)).thenReturn(true);
        mockMvc.perform(post("/api/v1/grn").with(user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeId\":10,\"lines\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"));
    }

    @Test
    void post_unknownStore_returns404() throws Exception {
        when(storeRepository.existsById(99L)).thenReturn(false);
        mockMvc.perform(post("/api/v1/grn").with(user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeId\":99,\"lines\":[{\"itemId\":20,\"quantity\":1}]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }
}
