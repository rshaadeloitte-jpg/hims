package com.hims.billing.api;

import com.hims.billing.application.DoctorShareService;
import com.hims.billing.application.DoctorShareService.DoctorShareCalculation;
import com.hims.billing.configuration.SecurityConfig;
import com.hims.billing.domain.model.DoctorIncentive;
import com.hims.billing.domain.model.DoctorShare;
import com.hims.billing.domain.model.VisitCharge;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Extended contract tests for DoctorShareController.
 */
@WebMvcTest(DoctorShareController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class DoctorShareControllerExtendedTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DoctorShareService doctorShareService;

    @Nested
    class ListSharesTests {
        @Test
        void listShares_returnsPage() throws Exception {
            DoctorShare share = new DoctorShare();
            share.setShareId(1L);
            Page<DoctorShare> page = new PageImpl<>(List.of(share));
            when(doctorShareService.listShares(0, 20)).thenReturn(page);

            mockMvc.perform(get("/api/v1/doctor-shares")
                            .with(user("devops")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].shareId").value(1));
        }

        @Test
        void listShares_withPaginationParams() throws Exception {
            when(doctorShareService.listShares(1, 10)).thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/v1/doctor-shares?page=1&size=10")
                            .with(user("devops")))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class GetShareTests {
        @Test
        void getShare_returnsShare() throws Exception {
            DoctorShare share = new DoctorShare();
            share.setShareId(1L);
            when(doctorShareService.getShare(1L)).thenReturn(share);

            mockMvc.perform(get("/api/v1/doctor-shares/1")
                            .with(user("devops")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.shareId").value(1));
        }
    }

    @Nested
    class CreateShareTests {
        @Test
        void createShare_returns201() throws Exception {
            DoctorShare share = new DoctorShare();
            share.setShareId(1L);
            when(doctorShareService.createShare(any())).thenReturn(share);

            mockMvc.perform(post("/api/v1/doctor-shares")
                            .with(user("devops")).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"billId":1,"chargeId":2,"doctorId":42,"shareAmount":100.00}"""))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.shareId").value(1));
        }
    }

    @Nested
    class MarkPaidTests {
        @Test
        void markPaid_returns200() throws Exception {
            DoctorShare share = new DoctorShare();
            share.setShareId(1L);
            share.setIsBilled(true);
            when(doctorShareService.markPaid(1L)).thenReturn(share);

            mockMvc.perform(patch("/api/v1/doctor-shares/1/payout")
                            .with(user("devops")).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isBilled").value(true));
        }
    }

    @Nested
    class GetSharesByBillIdTests {
        @Test
        void getSharesByBillId_returnsList() throws Exception {
            DoctorShare share = new DoctorShare();
            share.setShareId(1L);
            share.setBillId(10L);
            when(doctorShareService.getSharesByBillId(10L)).thenReturn(List.of(share));

            mockMvc.perform(get("/api/v1/doctor-shares/bill/10")
                            .with(user("devops")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].billId").value(10));
        }
    }

    @Nested
    class VisitChargeEndpointsTests {
        @Test
        void createVisitCharge_returns201() throws Exception {
            VisitCharge vc = new VisitCharge();
            vc.setVisitChargeId(1L);
            when(doctorShareService.createVisitCharge(any())).thenReturn(vc);

            mockMvc.perform(post("/api/v1/visit-charges")
                            .with(user("devops")).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"doctorId":42,"amount":500.00,"visitType":1}"""))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.visitChargeId").value(1));
        }

        @Test
        void getVisitCharge_returns200() throws Exception {
            VisitCharge vc = new VisitCharge();
            vc.setVisitChargeId(1L);
            when(doctorShareService.getVisitCharge(1L)).thenReturn(vc);

            mockMvc.perform(get("/api/v1/visit-charges/1")
                            .with(user("devops")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.visitChargeId").value(1));
        }
    }

    @Nested
    class IncentiveEndpointsTests {
        @Test
        void getIncentives_returns200() throws Exception {
            DoctorIncentive inc = new DoctorIncentive();
            inc.setIncentiveId(1L);
            when(doctorShareService.getIncentivesByDoctorId(42L)).thenReturn(List.of(inc));

            mockMvc.perform(get("/api/v1/doctor-incentives?doctorId=42")
                            .with(user("devops")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].incentiveId").value(1));
        }

        @Test
        void getUnbilledIncentives_returns200() throws Exception {
            DoctorIncentive inc = new DoctorIncentive();
            inc.setIncentiveId(1L);
            inc.setIsBilled(false);
            when(doctorShareService.getUnbilledIncentivesByDoctorId(42L)).thenReturn(List.of(inc));

            mockMvc.perform(get("/api/v1/doctor-incentives/unbilled?doctorId=42")
                            .with(user("devops")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].isBilled").value(false));
        }

        @Test
        void markIncentiveBilled_returns200() throws Exception {
            DoctorIncentive inc = new DoctorIncentive();
            inc.setIncentiveId(1L);
            inc.setIsBilled(true);
            when(doctorShareService.markIncentiveBilled(1L)).thenReturn(inc);

            mockMvc.perform(patch("/api/v1/doctor-incentives/1/bill")
                            .with(user("devops")).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isBilled").value(true));
        }
    }

    @Nested
    class CalculateShareTests {
        @Test
        void calculateShare_withFixedAmount_returnsFixed() throws Exception {
            DoctorShareCalculation calc = new DoctorShareCalculation(
                    null, null, new BigDecimal("1000.00"), null, new BigDecimal("250.00"),
                    new BigDecimal("250.00"), new BigDecimal("750.00"), "FIXED");
            when(doctorShareService.calculateShare(any(), any(), any())).thenReturn(calc);

            mockMvc.perform(post("/api/v1/doctor-share-calculations")
                            .with(user("devops")).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"totalAmount":1000.00,"fixedAmount":250.00}"""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.calculationType").value("FIXED"));
        }
    }
}
