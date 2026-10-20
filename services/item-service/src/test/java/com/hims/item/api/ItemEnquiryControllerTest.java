package com.hims.item.api;

import com.hims.item.application.BatchAvailabilityView;
import com.hims.item.application.ItemEnquiryService;
import com.hims.item.application.ItemEnquiryView;
import com.hims.item.configuration.SecurityConfig;
import com.hims.item.domain.model.ItemStockBalance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemEnquiryController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class ItemEnquiryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemEnquiryService enquiryService;

    @Test
    void search_requiresAuth_defaultDeny() throws Exception {
        mockMvc.perform(get("/api/v1/items/search?name=cef"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void search_delegatesToService_withQueryParams() throws Exception {
        when(enquiryService.search(eq("cef"), eq(10L), eq(false), eq(0), eq(25)))
                .thenReturn(List.of(new ItemEnquiryView.SearchRow(
                        1001L, "CEF1", "Ceftriaxone 1g", "ACME", "1g",
                        1L, 11L,
                        new BigDecimal("23.50"), new BigDecimal("28.00"), new BigDecimal("28.00"),
                        10L, new BigDecimal("42"), 7L)));

        mockMvc.perform(get("/api/v1/items/search")
                        .param("name", "cef").param("storeId", "10")
                        .param("includeZeroStock", "false").param("page", "0").param("size", "25")
                        .with(user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itemId").value(1001))
                .andExpect(jsonPath("$[0].itemName").value("Ceftriaxone 1g"))
                .andExpect(jsonPath("$[0].availableQty").value(42))
                .andExpect(jsonPath("$[0].storeId").value(10))
                .andExpect(jsonPath("$[0].totalRows").value(7));
    }

    @Test
    void search_nameOnly_omitsStoreAndQty() throws Exception {
        when(enquiryService.search(eq("par"), isNull(), anyBoolean(), anyInt(), anyInt()))
                .thenReturn(List.of(new ItemEnquiryView.SearchRow(
                        2001L, "PCM500", "Paracetamol 500mg", null, "500mg",
                        1L, 11L,
                        new BigDecimal("1.20"), new BigDecimal("1.50"), new BigDecimal("1.50"),
                        null, null, 1L)));

        mockMvc.perform(get("/api/v1/items/search").param("name", "par").with(user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itemId").value(2001))
                .andExpect(jsonPath("$[0].storeId").doesNotExist())
                .andExpect(jsonPath("$[0].availableQty").doesNotExist());
    }

    @Test
    void batches_requiresAuth_defaultDeny() throws Exception {
        mockMvc.perform(get("/api/v1/items/1001/batches").param("storeId", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void batches_delegatesToStockService() throws Exception {
        when(enquiryService.getBatches(1001L, 10L)).thenReturn(List.of(
                new BatchAvailabilityView(
                        301L, "NEAR", java.sql.Date.valueOf("2027-01-31"),
                        new BigDecimal("100"), null, null, null, null, null, null, false,
                        1001L, "Ceftriaxone 1g", "CEF1")));

        mockMvc.perform(get("/api/v1/items/1001/batches")
                        .param("storeId", "10").with(user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].batchCode").value("NEAR"))
                .andExpect(jsonPath("$[0].availableQty").value(100));
        verify(enquiryService).getBatches(1001L, 10L);
    }

    @Test
    void batches_missingStoreId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/items/1001/batches").with(user("devops")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void stock_requiresAuth_defaultDeny() throws Exception {
        mockMvc.perform(get("/api/v1/items/1001/stock"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void stock_acrossStores_whenNoStoreId() throws Exception {
        ItemStockBalance b = new ItemStockBalance();
        b.setId(1L);
        b.setItemId(1001L);
        b.setStoreId(10L);
        b.setAvailableQty(new BigDecimal("42"));
        when(enquiryService.getStock(1001L, null)).thenReturn(List.of(b));

        mockMvc.perform(get("/api/v1/items/1001/stock").with(user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itemId").value(1001))
                .andExpect(jsonPath("$[0].storeId").value(10))
                .andExpect(jsonPath("$[0].availableQty").value(42));
    }

    @Test
    void stock_filteredByStore() throws Exception {
        ItemStockBalance b = new ItemStockBalance();
        b.setId(1L);
        b.setItemId(1001L);
        b.setStoreId(10L);
        when(enquiryService.getStock(1001L, 10L)).thenReturn(List.of(b));

        mockMvc.perform(get("/api/v1/items/1001/stock")
                        .param("storeId", "10").with(user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].storeId").value(10));
    }

    @Test
    void suppliers_requiresAuth_defaultDeny() throws Exception {
        mockMvc.perform(get("/api/v1/items/1001/suppliers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void suppliers_returnsJoinedLinks() throws Exception {
        when(enquiryService.getSuppliers(1001L)).thenReturn(List.of(
                new ItemEnquiryView.SupplierLink(
                        9001L, 501L, "ACME", "Acme Pharma",
                        true, false, true, 11L),
                new ItemEnquiryView.SupplierLink(
                        9002L, 502L, "ZED", "Zed Medical",
                        false, false, true, 11L)));

        mockMvc.perform(get("/api/v1/items/1001/suppliers").with(user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].supplierCode").value("ACME"))
                .andExpect(jsonPath("$[0].supplierName").value("Acme Pharma"))
                .andExpect(jsonPath("$[0].isManufacturer").value(true))
                .andExpect(jsonPath("$[1].supplierCode").value("ZED"));
    }

    @Test
    void unmappedPath_returns404_not500() throws Exception {
        mockMvc.perform(get("/api/v1/items/search-unknown").with(user("devops")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }
}
