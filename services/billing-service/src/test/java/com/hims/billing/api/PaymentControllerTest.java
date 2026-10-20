package com.hims.billing.api;

import com.hims.billing.application.PaymentService;
import com.hims.billing.configuration.SecurityConfig;
import com.hims.billing.domain.model.Bill;
import com.hims.billing.domain.model.Receipt;
import com.hims.billing.domain.model.Refund;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for the Payments API (MIG-041). Service mocked; L1/L2 cover the rules. */
@WebMvcTest(PaymentController.class)
@Import(SecurityConfig.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void endpoints_requireAuth_defaultDeny() throws Exception {
        mockMvc.perform(post("/api/v1/patients/1/advances").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/patients/1/advances/balance"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/bills/1/payments/advance-adjust").with(csrf()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/bills/1/payments").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/bills/1/refunds").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/bills/1/receipts"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/bills/1/refunds"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void applyAdvance_valid_returns201WithNewBalance() throws Exception {
        when(paymentService.applyAdvance(eq(505L), any(), any(), any(), any()))
                .thenReturn(new BigDecimal("150.00"));

        mockMvc.perform(post("/api/v1/patients/505/advances").with(user("cashier")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":100,"modeCode":"CASH","receiptNo":"RCPT-1","remarks":"deposit"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value(505))
                .andExpect(jsonPath("$.balance").value(150.00));
    }

    @Test
    void getAdvanceBalance_returns200() throws Exception {
        when(paymentService.getAdvanceBalance(505L)).thenReturn(new BigDecimal("42.50"));
        mockMvc.perform(get("/api/v1/patients/505/advances/balance")
                        .with(user("cashier")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(505))
                .andExpect(jsonPath("$.balance").value(42.50));
    }

    @Test
    void applyAdvanceAdjustment_returns200WithConsumedAmount() throws Exception {
        when(paymentService.consumeAdvanceFifo(3L)).thenReturn(new BigDecimal("100.00"));
        mockMvc.perform(post("/api/v1/bills/3/payments/advance-adjust")
                        .with(user("cashier")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billId").value(3))
                .andExpect(jsonPath("$.advanceConsumed").value(100.00));
    }

    @Test
    void recordPayment_valid_returns200WithBill() throws Exception {
        Bill b = new Bill();
        b.setBillId(3L);
        b.setBillNo("OPDBILL-1");
        b.setIsSettled(false);
        b.setBalanceSelf(new BigDecimal("70.00"));
        b.setBalanceNonSelf(new BigDecimal("30.00"));
        when(paymentService.recordPayment(eq(3L), any(), any(), any(), any(), any(), eq(false)))
                .thenReturn(b);

        mockMvc.perform(post("/api/v1/bills/3/payments").with(user("cashier")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"selfAmount":30,"nonSelfAmount":20,"modeCode":"CASH",
                                 "bankId":null,"bankRefNo":null,"applyAdvance":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billId").value(3))
                .andExpect(jsonPath("$.isSettled").value(false));
    }

    @Test
    void refund_valid_returns200() throws Exception {
        Bill b = new Bill();
        b.setBillId(3L);
        b.setBillNo("OPDBILL-1");
        b.setIsCancelled(true);
        when(paymentService.issueRefund(eq(3L), any(), any(), any(), any())).thenReturn(b);

        mockMvc.perform(post("/api/v1/bills/3/refunds").with(user("cashier")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":150,"modeCode":"CASH","reason":"patient request","transactionType":4}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isCancelled").value(true));
    }

    @Test
    void getReceipts_returns200WithList() throws Exception {
        Receipt r1 = new Receipt();
        r1.setReceiptId(1L);
        r1.setReceiptNo("RCPT-1");
        r1.setAmount(new BigDecimal("100.00"));
        r1.setModeCode("CASH");
        r1.setTransactionType(Receipt.TX_TYPE_RECEIPT);
        when(paymentService.getReceipts(3L)).thenReturn(List.of(r1));
        mockMvc.perform(get("/api/v1/bills/3/receipts").with(user("cashier")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].receiptNo").value("RCPT-1"));
    }

    @Test
    void getRefunds_returns200WithList() throws Exception {
        Refund ref = new Refund();
        ref.setRefundId(1L);
        ref.setAmount(new BigDecimal("50.00"));
        ref.setModeCode("CASH");
        ref.setTransactionType(Refund.TX_TYPE_ADVANCE_REFUND);
        when(paymentService.getRefunds(3L)).thenReturn(List.of(ref));
        mockMvc.perform(get("/api/v1/bills/3/refunds").with(user("cashier")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(50.00))
                .andExpect(jsonPath("$[0].transactionType").value(3));
    }
}
