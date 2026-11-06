package com.hims.adminsecurity;

import com.hims.admin.configuration.DevSecurityConfig;
import com.hims.admin.configuration.JwtConfig;
import com.hims.reporting.api.ReportController;
import com.hims.rmrs.api.RmrsM23Controller;
import com.hims.reporting.application.EventIngressService;
import com.hims.reporting.application.ReportEngineService;
import com.hims.rmrs.application.RmrsM23Service;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ReportController.class, RmrsM23Controller.class})
@ActiveProfiles("dev")
@Import({DevSecurityConfig.class, JwtConfig.class})
@AutoConfigureMockMvc
class DevJwtSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportEngineService reportEngineService;

    @MockBean
    private EventIngressService eventIngressService;

    @MockBean
    private RmrsM23Service rmrsM23Service;

    @Test
    void noToken_returns401OnReports() throws Exception {
        mockMvc.perform(get("/api/v1/reports"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void noToken_returns401OnDashboardCounts() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/counts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void noToken_returns401OnJournalVouchers() throws Exception {
        mockMvc.perform(get("/api/v1/rmrs/journal-vouchers"))
                .andExpect(status().isUnauthorized());
    }
}
