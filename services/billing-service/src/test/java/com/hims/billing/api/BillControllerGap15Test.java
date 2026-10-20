package com.hims.billing.api;

import com.hims.billing.application.BillingService;
import com.hims.billing.configuration.SecurityConfig;
import com.hims.billing.domain.model.Bill;
import com.hims.billing.domain.model.CreditNote;
import com.hims.billing.domain.model.PackageSubscription;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BillController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class BillControllerGap15Test {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private BillingService billingService;

    @Test
    void gap15_endpoints_requireAuth() throws Exception {
        mockMvc.perform(get("/api/v1/bills/estimate").param("patientId", "1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/bills/estimate").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/bills/1/credit-note").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/bills/1/sponsor").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/package-subscriptions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void creditNoteSingular_returns201() throws Exception {
        CreditNote cn = new CreditNote();
        cn.setCreditNoteId(1L);
        cn.setCreditNoteNo("CN-1");
        cn.setAmount(new BigDecimal("50.00"));
        when(billingService.issueCreditNote(eq(10L), any(), any())).thenReturn(cn);
        mockMvc.perform(post("/api/v1/bills/10/credit-note").with(user("devops")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":50,\"reason\":\"test\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creditNoteNo").value("CN-1"));
    }

    @Test
    void estimateGet_returnsEstimate() throws Exception {
        BillingService.EstimateResult r = new BillingService.EstimateResult(505L, true,
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("6.00"),
                new BigDecimal("34.00"), new BigDecimal("60.00"), List.of());
        when(billingService.estimate(eq(505L), any(), anyList())).thenReturn(r);
        mockMvc.perform(get("/api/v1/bills/estimate")
                        .param("patientId", "505")
                        .param("serviceId", "900")
                        .param("rate", "100")
                        .param("qty", "1")
                        .with(user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(100.00))
                .andExpect(jsonPath("$.patientId").value(505));
    }

    @Test
    void estimatePost_returnsEstimate() throws Exception {
        BillingService.EstimateResult r = new BillingService.EstimateResult(505L, true,
                new BigDecimal("200.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("200.00"), BigDecimal.ZERO, List.of());
        when(billingService.estimate(anyLong(), any(), anyList())).thenReturn(r);
        mockMvc.perform(post("/api/v1/bills/estimate").with(user("devops")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":505,\"isSelfCompany\":true,\"items\":[{\"serviceId\":1,\"serviceName\":\"A\",\"rate\":200,\"qty\":1}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(200.00));
    }

    @Test
    void sponsorChange_returnsBill() throws Exception {
        Bill b = new Bill();
        b.setBillId(10L);
        b.setSponsorCompanyId(99L);
        b.setIsSelfCompany(false);
        when(billingService.changeSponsor(eq(10L), eq(99L), eq(7L))).thenReturn(b);
        mockMvc.perform(put("/api/v1/bills/10/sponsor").with(user("devops")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyId\":99,\"planId\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billId").value(10));
    }

    @Test
    void packageSubscription_returnsCreated() throws Exception {
        PackageSubscription ps = new PackageSubscription();
        ps.setSubscriptionId(1L);
        ps.setSubscriptionNo("PS-1");
        ps.setPatientId(505L);
        ps.setPackageId(777L);
        when(billingService.subscribePackage(eq(505L), eq(777L), isNull(), any(), any(), any())).thenReturn(ps);
        mockMvc.perform(post("/api/v1/package-subscriptions").with(user("devops")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":505,\"packageId\":777}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subscriptionNo").value("PS-1"));

        when(billingService.packageSubscriptionsForPatient(505L)).thenReturn(List.of(ps));
        mockMvc.perform(get("/api/v1/package-subscriptions").param("patientId", "505").with(user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subscriptionNo").value("PS-1"));
    }
}
