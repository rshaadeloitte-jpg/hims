package com.hims.billing.api;

import com.hims.billing.application.BillPaymentLedgerService;
import com.hims.billing.configuration.SecurityConfig;
import com.hims.billing.domain.model.BillPaymentLedger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests for BillPaymentLedgerController.
 */
@WebMvcTest(BillPaymentLedgerController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class BillPaymentLedgerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BillPaymentLedgerService ledgerService;

    @Test
    void record_validRequest_returns201() throws Exception {
        BillPaymentLedger ledger = new BillPaymentLedger();
        ledger.setLedgerId(1L);
        ledger.setBillId(3L);
        ledger.setEventType("PAYMENT");
        when(ledgerService.record(eq(3L), any(), any(), any(), any(), any(), any()))
                .thenReturn(ledger);

        mockMvc.perform(post("/api/v1/bills/3/payment-ledger")
                        .with(user("devops")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventType":"PAYMENT","paidSelf":100.00,"paidNonSelf":50.00}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ledgerId").value(1));
    }

    @Test
    void list_returns200WithEntries() throws Exception {
        BillPaymentLedger ledger = new BillPaymentLedger();
        ledger.setLedgerId(1L);
        ledger.setBillId(3L);
        ledger.setEventType("PAYMENT");
        when(ledgerService.listForBill(3L)).thenReturn(List.of(ledger));

        mockMvc.perform(get("/api/v1/bills/3/payment-ledger")
                        .with(user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value("PAYMENT"));
    }

    @Test
    void status_returns200WithSummary() throws Exception {
        BillPaymentLedgerService.PaymentStatus ps = new BillPaymentLedgerService.PaymentStatus(
                3L, "OPDBILL-1", new BigDecimal("1000.00"), new BigDecimal("500.00"),
                new BigDecimal("400.00"), new BigDecimal("500.00"), new BigDecimal("600.00"),
                false, 1L, null, 1);
        when(ledgerService.paymentStatus(3L)).thenReturn(ps);

        mockMvc.perform(get("/api/v1/bills/3/payment-ledger/status")
                        .with(user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billId").value(3))
                .andExpect(jsonPath("$.settled").value(false));
    }

    @Test
    void endpoints_requireAuth_defaultDeny() throws Exception {
        mockMvc.perform(get("/api/v1/bills/3/payment-ledger"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/bills/3/payment-ledger/status"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/bills/3/payment-ledger").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
