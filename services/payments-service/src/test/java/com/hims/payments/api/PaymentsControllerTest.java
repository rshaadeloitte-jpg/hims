package com.hims.payments.api;

import com.hims.payments.application.AdvanceService;
import com.hims.payments.application.PaymentService;
import com.hims.payments.application.RefundService;
import com.hims.payments.application.exceptions.BillingCallException;
import com.hims.payments.application.exceptions.InvalidPaymentRequestException;
import com.hims.payments.application.exceptions.PaymentEntityNotFoundException;
import com.hims.payments.configuration.SecurityConfig;
import com.hims.payments.domain.model.Advance;
import com.hims.payments.domain.model.Payment;
import com.hims.payments.domain.model.Refund;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for the Payments &amp; Refunds API (default-deny enforced). Services mocked; L1 covers the rules. */
@WebMvcTest(PaymentsController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class PaymentsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdvanceService advanceService;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private RefundService refundService;

    private static Advance advance(long id, String amount, String balance) {
        Advance a = new Advance();
        a.setAdvanceId(id);
        a.setPatientId(505L);
        a.setAmount(new BigDecimal(amount));
        a.setUsed(BigDecimal.ZERO);
        a.setBalance(new BigDecimal(balance));
        return a;
    }

    // ------------------------------------------------------------------ security

    @Test
    void endpoints_requireAuth_defaultDeny() throws Exception {
        mockMvc.perform(get("/api/v1/advances")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/payments")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/refunds")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/advances").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/payments").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/refunds").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ advances

    @Test
    void createAdvance_valid_returns201WithGeneratedBalance() throws Exception {
        when(advanceService.create(eq(505L), eq(new BigDecimal("500"))))
                .thenReturn(advance(11L, "500", "500"));

        mockMvc.perform(post("/api/v1/advances").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":505,\"amount\":500}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.advanceId").value(11))
                .andExpect(jsonPath("$.patientId").value(505))
                .andExpect(jsonPath("$.amount").value(500))
                .andExpect(jsonPath("$.balance").value(500));
    }

    @Test
    void listAdvances_missingPatientId_mapsTo400Envelope() throws Exception {
        when(advanceService.byPatient(null)).thenThrow(
                new InvalidPaymentRequestException("PAY_REQ_INVALID", "patientId query parameter is required"));

        mockMvc.perform(get("/api/v1/advances").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("PAY_REQ_INVALID"));
    }

    // ------------------------------------------------------------------ payments

    @Test
    void pay_modesSumMismatch_mapsTo400PayModeMismatchEnvelope() throws Exception {
        when(paymentService.pay(any(), any(), any(), anyList(), anyBoolean()))
                .thenThrow(new InvalidPaymentRequestException("PAY_MODE_MISMATCH",
                        "Sum of modes (90.00) must equal amount (100.00)"));

        mockMvc.perform(post("/api/v1/payments").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"billId":5,"patientId":505,"amount":100,"advanceUse":false,
                                 "modes":[{"mode":1,"amount":90}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("PAY_MODE_MISMATCH"));
    }

    @Test
    void pay_billingUnreachable_mapsTo502Envelope_nothingRecorded() throws Exception {
        when(paymentService.pay(any(), any(), any(), anyList(), anyBoolean()))
                .thenThrow(new BillingCallException("billing-service PUT settlement for bill 5 failed"));

        mockMvc.perform(post("/api/v1/payments").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"billId":5,"patientId":505,"amount":100,"advanceUse":false,
                                 "modes":[{"mode":1,"amount":100}]}
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("BILLING_UNREACHABLE"));
    }

    @Test
    void listPayments_returnsModesAndConsumptions() throws Exception {
        Payment p = new Payment();
        p.setPaymentId(21L);
        p.setBillId(5L);
        p.setPatientId(505L);
        p.setAmount(new BigDecimal("100"));
        p.setTransactionType(Payment.TYPE_PAYMENT);
        p.setChangeAmount(new BigDecimal("15.50"));
        PaymentModeDetailDtoHolder.modeRows(p);
        when(paymentService.byBill(5L)).thenReturn(List.of(p));

        mockMvc.perform(get("/api/v1/payments").with(csrf()).with(user("devops").roles("DEV"))
                        .param("billId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].paymentId").value(21))
                .andExpect(jsonPath("$[0].billId").value(5))
                .andExpect(jsonPath("$[0].transactionType").value(2))
                .andExpect(jsonPath("$[0].modes[0].mode").value(1))
                .andExpect(jsonPath("$[0].advanceConsumptions[0].advanceId").value(11));
    }

    @Test
    void listPayments_missingBillId_mapsTo400() throws Exception {
        when(paymentService.byBill(null)).thenThrow(
                new InvalidPaymentRequestException("PAY_REQ_INVALID", "billId query parameter is required"));

        mockMvc.perform(get("/api/v1/payments").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("PAY_REQ_INVALID"));
    }

    // ------------------------------------------------------------------ refunds

    @Test
    void refund_advanceWithoutBalance_mapsTo400Bexar002() throws Exception {
        when(refundService.create(eq(3L), eq(505L), eq(null), eq(11L),
                eq(new BigDecimal("100")), eq("refund")))
                .thenThrow(new InvalidPaymentRequestException(RefundService.ERR_ADVANCE_BALANCE,
                        "Advance 11 has no refundable balance"));

        mockMvc.perform(post("/api/v1/refunds").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transactionType\":3,\"patientId\":505,\"advanceId\":11,"
                                + "\"amount\":100,\"reason\":\"refund\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXAR002"));
    }

    @Test
    void refund_validType3_returns201() throws Exception {
        Refund r = new Refund();
        r.setRefundId(31L);
        r.setTransactionType(Refund.TYPE_ADVANCE_REFUND);
        r.setPatientId(505L);
        r.setAdvanceId(11L);
        r.setAmount(new BigDecimal("100"));
        r.setReason("refund");
        when(refundService.create(eq(3L), eq(505L), eq((Long) null), eq(11L),
                eq(new BigDecimal("100")), eq("refund"))).thenReturn(r);

        mockMvc.perform(post("/api/v1/refunds").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transactionType\":3,\"patientId\":505,\"advanceId\":11,"
                                + "\"amount\":100,\"reason\":\"refund\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.refundId").value(31))
                .andExpect(jsonPath("$.transactionType").value(3));
    }

    @Test
    void listRefunds_byBill() throws Exception {
        Refund r = new Refund();
        r.setRefundId(32L);
        r.setTransactionType(Refund.TYPE_BILL_REFUND);
        r.setBillId(9L);
        r.setPatientId(505L);
        r.setAmount(new BigDecimal("75.25"));
        r.setReason("overcharge");
        when(refundService.byBill(9L)).thenReturn(List.of(r));

        mockMvc.perform(get("/api/v1/refunds").with(csrf()).with(user("devops").roles("DEV"))
                        .param("billId", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].refundId").value(32))
                .andExpect(jsonPath("$[0].transactionType").value(4));
    }

    @Test
    void unknownEntity_mapsTo404NotFoundEnvelope() throws Exception {
        when(refundService.byBill(9L)).thenThrow(new PaymentEntityNotFoundException("Refund not found"));

        mockMvc.perform(get("/api/v1/refunds").with(csrf()).with(user("devops").roles("DEV"))
                        .param("billId", "9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    /** Small helper so the fixture above can attach transient child rows without a builder cascade. */
    private static final class PaymentModeDetailDtoHolder {
        private static void modeRows(Payment p) {
            com.hims.payments.domain.model.PaymentModeDetail m =
                    new com.hims.payments.domain.model.PaymentModeDetail();
            m.setModeDetailId(101L);
            m.setPaymentId(p.getPaymentId());
            m.setMode((short) 1);
            m.setAmount(new BigDecimal("100"));
            p.setModes(List.of(m));
            com.hims.payments.domain.model.AdvanceConsumption c =
                    new com.hims.payments.domain.model.AdvanceConsumption();
            c.setConsumptionId(201L);
            c.setAdvanceId(11L);
            c.setPaymentId(p.getPaymentId());
            c.setConsumedAmount(new BigDecimal("40"));
            p.setAdvanceConsumptions(List.of(c));
        }
    }
}
