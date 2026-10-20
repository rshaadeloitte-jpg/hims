package com.hims.pharmacy.api;

import com.hims.pharmacy.application.ItemSaleService;
import com.hims.pharmacy.application.exceptions.IllegalSaleStateException;
import com.hims.pharmacy.application.exceptions.InvalidSaleRequestException;
import com.hims.pharmacy.application.exceptions.SaleNotFoundException;
import com.hims.pharmacy.application.exceptions.StockIssueException;
import com.hims.pharmacy.configuration.SecurityConfig;
import com.hims.pharmacy.domain.model.ItemSale;
import com.hims.pharmacy.domain.model.ItemSaleDetail;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for the Item Sale API (default-deny enforced). Services mocked; L0 covers the rules. */
@WebMvcTest(ItemSaleController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class ItemSaleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemSaleService itemSaleService;

    private static ItemSale sale(long id, short status, boolean dispensed) {
        ItemSale s = new ItemSale();
        s.setSaleId(id);
        s.setPatientId(505L);
        s.setPrescriptionId(null);
        s.setStatusCode(status);
        s.setDispensed(dispensed);
        return s;
    }

    @Test
    void endpoints_requireAuth_defaultDeny() throws Exception {
        mockMvc.perform(get("/api/v1/item-sales")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/item-sales/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/item-sales").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/item-sales/1/dispense").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_valid_returns201Pending() throws Exception {
        ItemSale saved = sale(10L, ItemSale.STATUS_PENDING, false);
        when(itemSaleService.create(eq(505L), isNull(), anyList())).thenReturn(saved);

        mockMvc.perform(post("/api/v1/item-sales").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":505,"lines":[{"itemId":501,"qty":5}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saleId").value(10))
                .andExpect(jsonPath("$.statusCode").value(1))
                .andExpect(jsonPath("$.dispensed").value(false));
    }

    @Test
    void create_noLines_mapsTo400Bexa003() throws Exception {
        when(itemSaleService.create(any(), any(), any()))
                .thenThrow(new InvalidSaleRequestException("BEXA003",
                        "At least one sale line {itemId,qty} is required"));

        mockMvc.perform(post("/api/v1/item-sales").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":505,\"lines\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"))
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    @Test
    void list_acceptsNamedOrNumericStatusFilter() throws Exception {
        when(itemSaleService.list((short) 2, 505L))
                .thenReturn(List.of(sale(11L, ItemSale.STATUS_PARTIALLY_DISPENSED, false)));

        mockMvc.perform(get("/api/v1/item-sales").with(csrf()).with(user("devops").roles("DEV"))
                        .param("status", "PARTIALLY_DISPENSED").param("patientId", "505"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].saleId").value(11))
                .andExpect(jsonPath("$[0].statusCode").value(2));

        mockMvc.perform(get("/api/v1/item-sales").with(csrf()).with(user("devops").roles("DEV"))
                        .param("status", "2").param("patientId", "505"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].saleId").value(11));
    }

    @Test
    void get_unknownSale_mapsTo404() throws Exception {
        when(itemSaleService.get(404L)).thenThrow(new SaleNotFoundException("NOT_FOUND", "not found"));

        mockMvc.perform(get("/api/v1/item-sales/404").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    void dispense_returnsUpdatedSale() throws Exception {
        ItemSale closed = sale(10L, ItemSale.STATUS_FULLY_DISPENSED, true);
        ItemSaleDetail d = new ItemSaleDetail();
        d.setSaleId(10L);
        d.setLineNo(1L);
        d.setItemId(501L);
        d.setQty(5);
        d.setPendingQty(0);
        d.setDispensed(true);
        closed.setDetails(List.of(d));
        when(itemSaleService.dispense(eq(10L), anyList())).thenReturn(closed);

        mockMvc.perform(post("/api/v1/item-sales/10/dispense")
                        .with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[{\"lineNo\":1,\"qty\":5}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(3))
                .andExpect(jsonPath("$.dispensed").value(true))
                .andExpect(jsonPath("$.details[0].dispensed").value(true));
    }

    @Test
    void dispense_illegalTransition_mapsTo400WithCode() throws Exception {
        when(itemSaleService.dispense(eq(10L), anyList()))
                .thenThrow(new IllegalSaleStateException("PH_ILLEGAL_TRANSITION",
                        "Sale 10 is FULLY_DISPENSED (terminal)"));

        mockMvc.perform(post("/api/v1/item-sales/10/dispense")
                        .with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[{\"lineNo\":1,\"qty\":1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("PH_ILLEGAL_TRANSITION"));
    }

    @Test
    void dispense_stockCallFailure_mapsTo502StockServiceUnavailable() throws Exception {
        when(itemSaleService.dispense(eq(10L), anyList()))
                .thenThrow(new StockIssueException("item-service rejected the stock issue"));

        mockMvc.perform(post("/api/v1/item-sales/10/dispense")
                        .with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[{\"lineNo\":1,\"qty\":1}]}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.errorCode").value("STOCK_SERVICE_UNAVAILABLE"));
    }
}
