package com.hims.billing.api;

import com.hims.billing.application.DoctorShareService;
import com.hims.billing.application.DoctorShareService.DoctorShareCalculation;
import com.hims.billing.configuration.SecurityConfig;
import com.hims.billing.domain.model.DoctorShare;
import com.hims.billing.domain.model.DoctorIncentive;
import com.hims.billing.domain.model.VisitCharge;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests for M06 Doctor Share REST API.
 */
@WebMvcTest(DoctorShareController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class DoctorShareControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DoctorShareService doctorShareService;

    // ---- POST /api/v1/doctor-share-calculations ----
    @Test
    void calculateShare_validRequest_returnsCalculation() throws Exception {
        DoctorShareCalculation calc = new DoctorShareCalculation(
                null, null, new BigDecimal("1000.00"), new BigDecimal("10"), null,
                new BigDecimal("100.00"), new BigDecimal("900.00"), "PERCENTAGE");
        when(doctorShareService.calculateShare(any(), any(), any())).thenReturn(calc);

        mockMvc.perform(post("/api/v1/doctor-share-calculations")
                        .with(user("devops")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"totalAmount\":1000.00,\"sharePercentage\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shareAmount").value(100.00))
                .andExpect(jsonPath("$.hospitalAmount").value(900.00))
                .andExpect(jsonPath("$.calculationType").value("PERCENTAGE"));
    }

    // ---- GET /api/v1/doctor-share-calculations/{billId} ----
    @Test
    void getCalculatedShares_returnsList() throws Exception {
        DoctorShareCalculation calc = new DoctorShareCalculation(
                7L, 42L, new BigDecimal("1000.00"), new BigDecimal("10"), null,
                new BigDecimal("100.00"), new BigDecimal("900.00"), "PERCENTAGE");
        when(doctorShareService.calculateSharesForBill(10L)).thenReturn(List.of(calc));

        mockMvc.perform(get("/api/v1/doctor-share-calculations/10")
                        .with(user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].shareAmount").value(100.00));
    }

    // ---- PUT /api/v1/doctor-share-add ----
    @Test
    void addIncentive_createsAndReturns201() throws Exception {
        DoctorIncentive incentive = new DoctorIncentive();
        incentive.setIncentiveId(1L);
        incentive.setDoctorId(42L);
        incentive.setAmount(new BigDecimal("1000.00"));
        incentive.setIncentiveType("ADDITION");
        when(doctorShareService.addIncentive(any())).thenReturn(incentive);

        mockMvc.perform(put("/api/v1/doctor-share-add")
                        .with(user("devops")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"doctorId\":42,\"amount\":1000.00,\"remark\":\"bonus\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.incentiveType").value("ADDITION"));
    }

    // ---- DELETE /api/v1/doctor-share-deduct ----
    @Test
    void deductIncentive_createsDeduction() throws Exception {
        DoctorIncentive incentive = new DoctorIncentive();
        incentive.setIncentiveId(2L);
        incentive.setDoctorId(42L);
        incentive.setAmount(new BigDecimal("500.00"));
        incentive.setIncentiveType("DEDUCTION");
        when(doctorShareService.deductIncentive(any())).thenReturn(incentive);

        mockMvc.perform(delete("/api/v1/doctor-share-deduct")
                        .with(user("devops")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"doctorId\":42,\"amount\":500.00,\"remark\":\"penalty\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incentiveType").value("DEDUCTION"));
    }

    // ---- GET /api/v1/doctor-share/bills ----
    @Test
    void getBillsByDoctorId_returnsShares() throws Exception {
        DoctorShare share = new DoctorShare();
        share.setShareId(1L);
        share.setDoctorId(42L);
        share.setShareAmount(new BigDecimal("100.00"));
        when(doctorShareService.getBillsByDoctorId(42L)).thenReturn(List.of(share));

        mockMvc.perform(get("/api/v1/doctor-share/bills?doctorId=42")
                        .with(user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].shareAmount").value(100.00));
    }

    // ---- GET /api/v1/doctor-share/visit-charges ----
    @Test
    void getVisitChargesByDoctorId_returnsCharges() throws Exception {
        VisitCharge vc = new VisitCharge();
        vc.setVisitChargeId(1L);
        vc.setDoctorId(42L);
        vc.setAmount(new BigDecimal("500.00"));
        when(doctorShareService.getVisitChargesByDoctorId(42L)).thenReturn(List.of(vc));

        mockMvc.perform(get("/api/v1/doctor-share/visit-charges?doctorId=42")
                        .with(user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(500.00));
    }

    // ---- Auth tests ----
    @Test
    void endpoints_requireAuth_defaultDeny() throws Exception {
        mockMvc.perform(get("/api/v1/doctor-share/bills?doctorId=42"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/doctor-share-calculations").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/doctor-share-add").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/doctor-share-deduct").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
