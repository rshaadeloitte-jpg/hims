package com.hims.ot.api;

import com.hims.ot.application.OtScheduleService;
import com.hims.ot.application.exceptions.InvalidOtRequestException;
import com.hims.ot.configuration.SecurityConfig;
import com.hims.ot.domain.model.OtSchedule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for the OT API (default-deny enforced). Services mocked; L1 covers the rules. */
@WebMvcTest(OtScheduleController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class OtControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OtScheduleService service;

    private static OtSchedule schedule(long id, long patientId, short status) {
        OtSchedule s = new OtSchedule();
        s.setScheduleId(id);
        s.setPatientId(patientId);
        s.setEncounterId(900L);
        s.setOtTable("OT-1");
        s.setScheduledStart(Timestamp.valueOf("2026-09-01 08:00:00"));
        s.setStatus(status);
        s.setPreBilling(true);
        return s;
    }

    @Test
    void endpoints_requireAuth_defaultDeny() throws Exception {
        mockMvc.perform(get("/api/v1/ot/schedules")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/ot/schedules/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/ot/schedules").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_valid_returns201() throws Exception {
        OtSchedule saved = schedule(10L, 501L, OtSchedule.STATUS_PLANNED);
        when(service.create(eq(501L), eq(900L), isNull(), eq("OT-1"), any(), isNull(),
                eq(7L), eq(9L), eq(true), anyList())).thenReturn(saved);

        mockMvc.perform(post("/api/v1/ot/schedules").with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":501,"encounterId":900,"otTable":"OT-1",
                                 "scheduledStart":"2026-09-01T08:00:00","anesthetistId":7,
                                 "surgeonId":9,"preBilling":true,
                                 "procedures":[{"procedureCode":"P1","procedureName":"Op","rate":5000}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scheduleId").value(10))
                .andExpect(jsonPath("$.preBilling").value(true))       // BR-OT-002 echoed
                .andExpect(jsonPath("$.status").value(1));
    }

    @Test
    void create_missingAdmissionAndEncounter_returns400BEOT004() throws Exception {
        when(service.create(any(), isNull(), isNull(), any(), any(), any(),
                any(), any(), any(), anyList()))
                .thenThrow(new InvalidOtRequestException("BEOT004",
                        "BR-OT-001: either admissionId or encounterId must be supplied"));

        mockMvc.perform(post("/api/v1/ot/schedules").with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":501,"otTable":"OT-1",
                                 "scheduledStart":"2026-09-01T08:00:00",
                                 "procedures":[{"procedureCode":"P1"}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEOT004"));
    }

    @Test
    void lifecycle_guards_surfaceAs400() throws Exception {
        when(service.start(5L)).thenThrow(new InvalidOtRequestException("OT_ILLEGAL_TRANSITION",
                "start allowed only from status PLANNED but schedule 5 is DONE"));
        mockMvc.perform(post("/api/v1/ot/schedules/5/start").with(user("devops").roles("DEV")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("OT_ILLEGAL_TRANSITION"));

        when(service.complete(10L)).thenReturn(schedule(10L, 501L, OtSchedule.STATUS_DONE));
        mockMvc.perform(post("/api/v1/ot/schedules/10/complete").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(3));

        when(service.cancel(11L)).thenReturn(schedule(11L, 501L, OtSchedule.STATUS_CANCELLED));
        mockMvc.perform(post("/api/v1/ot/schedules/11/cancel").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(4));
    }

    @Test
    void search_byDateAndStatus_and_getById() throws Exception {
        when(service.search(any(java.time.LocalDate.class), eq(1)))
                .thenReturn(List.of(schedule(10L, 501L, OtSchedule.STATUS_PLANNED)));
        mockMvc.perform(get("/api/v1/ot/schedules").with(user("devops").roles("DEV"))
                        .param("date", "2026-09-01").param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].scheduleId").value(10))
                .andExpect(jsonPath("$[0].statusName").doesNotExist());

        when(service.get(10L)).thenReturn(schedule(10L, 501L, OtSchedule.STATUS_PLANNED));
        mockMvc.perform(get("/api/v1/ot/schedules/10").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(501));
    }
}
