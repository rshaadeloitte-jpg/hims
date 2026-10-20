package com.hims.billing.api;

import com.hims.billing.application.BillingService;
import com.hims.billing.configuration.SecurityConfig;
import com.hims.billing.domain.model.Bill;
import com.hims.billing.domain.model.Charge;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for the Billing API (default-deny enforced). Service mocked; L1/L2 cover the rules. */
@WebMvcTest(BillController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class BillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BillingService billingService;

    private static Charge charge(long id, String rate, int qty) {
        Charge c = new Charge();
        c.setChargeId(id);
        c.setServiceId(900L);
        c.setServiceName("X-Ray Chest");
        c.setRate(new BigDecimal(rate));
        c.setQty(qty);
        return c;
    }

    private static Bill bill(long id, String billNo, boolean interim) {
        Bill b = new Bill();
        b.setBillId(id);
        b.setPatientId(505L);
        b.setEncounterId(7777L);
        b.setBillNo(billNo);
        b.setIsInterim(interim);
        b.setIsSettled(false);
        return b;
    }

    @Test
    void endpoints_requireAuth_defaultDeny() throws Exception {
        mockMvc.perform(get("/api/v1/bills/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/bills")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/charges").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/bills").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        // internal settlement endpoint is NOT exempt from default-deny
        mockMvc.perform(put("/api/v1/internal/bills/1/settlement").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createCharge_valid_returns201() throws Exception {
        when(billingService.createCharge(any())).thenReturn(charge(7L, "350.00", 1));
        mockMvc.perform(post("/api/v1/charges").with(user("devops")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"serviceId":900,"serviceName":"X-Ray Chest","rate":350.00,"qty":1,
                                 "coPayPct":40,"doctorSharePct":10}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.chargeId").value(7))
                .andExpect(jsonPath("$.billId").doesNotExist());
    }

    @Test
    void createBill_valid_returns201WithPlaceholderBillNo() throws Exception {
        Bill created = bill(3L, Bill.PLACEHOLDER_BILL_NO, true);
        created.setCharges(List.of(charge(7L, "350.00", 1)));
        when(billingService.createBill(eq(505L), eq(7777L), isNull(), isNull(),
                eq(42L), eq(true), anyList())).thenReturn(created);
        mockMvc.perform(post("/api/v1/bills").with(user("devops")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":505,"encounterId":7777,"prescriberDoctorId":42,
                                 "isSelfCompany":true,"chargeIds":[7]}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.billNo").value("000000000000000"))
                .andExpect(jsonPath("$.interim").doesNotExist())     // field visibility via getters
                .andExpect(jsonPath("$.charges[0].chargeId").value(7));
    }

    @Test
    void finalize_returnsFinalBillNo() throws Exception {
        Bill finalized = bill(3L, "OPDBILL-1", false);
        when(billingService.finalizeBill(3L)).thenReturn(finalized);
        mockMvc.perform(post("/api/v1/bills/3/finalize").with(user("devops")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billNo").value("OPDBILL-1"));
    }

    @Test
    void settlement_put_returnsNewState() throws Exception {
        Bill settled = bill(3L, "OPDBILL-1", false);
        settled.setIsSettled(true);
        settled.setBalanceSelf(new BigDecimal("0.00"));
        settled.setBalanceNonSelf(new BigDecimal("-12.50"));
        when(billingService.settle(eq(3L), any(), any())).thenReturn(settled);
        mockMvc.perform(put("/api/v1/internal/bills/3/settlement").with(user("payments-svc")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paidSelf\":450.00,\"paidNonSelf\":612.50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settled").doesNotExist())
                .andExpect(jsonPath("$.balanceNonSelf").value(-12.50));
    }
}
