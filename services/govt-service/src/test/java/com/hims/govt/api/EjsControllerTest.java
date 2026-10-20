package com.hims.govt.api;

import com.hims.govt.application.EjsService;
import com.hims.govt.application.exceptions.GovtNotFoundException;
import com.hims.govt.application.exceptions.InvalidGovtRequestException;
import com.hims.govt.configuration.SecurityConfig;
import com.hims.govt.domain.model.EjsAsha;
import com.hims.govt.domain.model.EjsIndicatorEntry;
import com.hims.govt.domain.model.EjsIncentive;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for the EJS API (default-deny enforced). Services mocked; L0 covers the rules. */
@WebMvcTest(EjsController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class EjsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EjsService ejsService;

    private static EjsIncentive incentive(long id, short status) {
        EjsIncentive i = new EjsIncentive();
        i.setIncentiveId(id);
        i.setAshaId(1L);
        i.setPeriodYear(2025);
        i.setPeriodMonth(4);
        i.setAmount(new BigDecimal("1000.00"));
        i.setStatus(status);
        return i;
    }

    @Test
    void endpoints_requireAuth_defaultDeny() throws Exception {
        mockMvc.perform(get("/api/v1/ejs/incentives")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/ejs/asha").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/ejs/indicators").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/ejs/incentives/compute").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/ejs/incentives/1/approve").with(csrf()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/ejs/incentives/1/pay").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createAsha_valid_returns201() throws Exception {
        EjsAsha asha = new EjsAsha();
        asha.setAshaId(7L);
        asha.setAshaCode("ASHA-1");
        asha.setName("Asha");
        when(ejsService.createAsha("ASHA-1", "Asha", null)).thenReturn(asha);

        mockMvc.perform(post("/api/v1/ejs/asha").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ASHA-1\",\"name\":\"Asha\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ashaId").value(7))
                .andExpect(jsonPath("$.ashaCode").value("ASHA-1"));
    }

    @Test
    void createAsha_unknownSupervisor_mapsTo400Envelope() throws Exception {
        when(ejsService.createAsha(any(), any(), eq(42L)))
                .thenThrow(new InvalidGovtRequestException("GOVT_UNKNOWN_SUPERVISOR",
                        "supervisor not found: 42"));

        mockMvc.perform(post("/api/v1/ejs/asha").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ASHA-1\",\"name\":\"Asha\",\"supervisorId\":42}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("GOVT_UNKNOWN_SUPERVISOR"));
    }

    @Test
    void upsertIndicator_valid_returnsUpdatedEntry() throws Exception {
        EjsIndicatorEntry entry = new EjsIndicatorEntry();
        entry.setEntryId(21L);
        entry.setAshaId(1L);
        entry.setIndicatorCode("ANC_CHECKUP");
        entry.setPeriodYear(2025);
        entry.setPeriodMonth(4);
        entry.setValue(8);
        when(ejsService.upsertIndicator(1L, "ANC_CHECKUP", 2025, 4, 8)).thenReturn(entry);

        mockMvc.perform(post("/api/v1/ejs/indicators").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ashaId":1,"indicatorCode":"ANC_CHECKUP","periodYear":2025,
                                 "periodMonth":4,"value":8}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryId").value(21))
                .andExpect(jsonPath("$.value").value(8));
    }

    @Test
    void compute_valid_returns201Computed() throws Exception {
        when(ejsService.compute(1L, 2025, 4)).thenReturn(incentive(30L, EjsIncentive.STATUS_COMPUTED));

        mockMvc.perform(post("/api/v1/ejs/incentives/compute").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ashaId\":1,\"periodYear\":2025,\"periodMonth\":4}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.incentiveId").value(30))
                .andExpect(jsonPath("$.status").value(1))
                .andExpect(jsonPath("$.amount").value(1000.0));
    }

    @Test
    void approve_andPay_happyPaths() throws Exception {
        when(ejsService.approve(30L)).thenReturn(incentive(30L, EjsIncentive.STATUS_APPROVED));
        when(ejsService.pay(30L)).thenReturn(incentive(30L, EjsIncentive.STATUS_PAID));

        mockMvc.perform(put("/api/v1/ejs/incentives/30/approve").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2));

        mockMvc.perform(put("/api/v1/ejs/incentives/30/pay").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(3));
    }

    @Test
    void approve_illegalTransition_mapsTo400Envelope_andUnknownTo404() throws Exception {
        when(ejsService.approve(31L))
                .thenThrow(new InvalidGovtRequestException("GOVT_ILLEGAL_TRANSITION",
                        "Cannot pay incentive 31 from status PAID"));
        when(ejsService.pay(404L))
                .thenThrow(new GovtNotFoundException("NOT_FOUND", "Incentive not found: 404"));

        mockMvc.perform(put("/api/v1/ejs/incentives/31/approve").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("GOVT_ILLEGAL_TRANSITION"));

        mockMvc.perform(put("/api/v1/ejs/incentives/404/pay").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    void listIncentives_acceptsNamedOrNumericStatusAndAshaFilters() throws Exception {
        when(ejsService.listIncentives((short) 3, 1L)).thenReturn(List.of(incentive(9L, EjsIncentive.STATUS_PAID)));

        mockMvc.perform(get("/api/v1/ejs/incentives").with(csrf()).with(user("devops").roles("DEV"))
                        .param("status", "PAID").param("ashaId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].incentiveId").value(9));

        mockMvc.perform(get("/api/v1/ejs/incentives").with(csrf()).with(user("devops").roles("DEV"))
                        .param("status", "3").param("ashaId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].incentiveId").value(9));
    }
}
