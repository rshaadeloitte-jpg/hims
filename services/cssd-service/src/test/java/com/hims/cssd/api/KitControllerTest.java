package com.hims.cssd.api;

import com.hims.cssd.application.CssdKitService;
import com.hims.cssd.application.exceptions.CssdNotFoundException;
import com.hims.cssd.application.exceptions.InvalidKitRequestException;
import com.hims.cssd.configuration.SecurityConfig;
import com.hims.cssd.domain.model.Kit;
import com.hims.cssd.domain.model.KitIssue;
import com.hims.cssd.domain.model.SterilizationCycle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
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

/** Contract tests for the CSSD Kit API (default-deny enforced). Services mocked; L0 covers the rules. */
@WebMvcTest(KitController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class KitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CssdKitService cssdKitService;

    private static Kit kit(long id, String code, short status) {
        Kit k = new Kit();
        k.setKitId(id);
        k.setKitCode(code);
        k.setProcedureType("GENERAL");
        k.setStatus(status);
        k.setExpiryDays(14);
        return k;
    }

    @Test
    void endpoints_requireAuth_defaultDeny() throws Exception {
        mockMvc.perform(get("/api/v1/kits")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/kits/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/kits").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/kits/1/cycles").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/cycles/1/complete").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/kits/1/issues").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/kits/1/issues")).andExpect(status().isUnauthorized());
    }

    @Test
    void create_valid_returns201AssembledWithItems() throws Exception {
        Kit saved = kit(10L, "K-100", Kit.STATUS_ASSEMBLED);
        when(cssdKitService.create(eq("K-100"), eq("ORTHOPEDIC"), anyList(), isNull()))
                .thenReturn(saved);

        mockMvc.perform(post("/api/v1/kits").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"kitCode":"K-100","procedureType":"ORTHOPEDIC",
                                 "items":[{"itemId":501,"qty":2}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kitId").value(10))
                .andExpect(jsonPath("$.status").value(1))
                .andExpect(jsonPath("$.expiryDays").value(14));
    }

    @Test
    void create_noItems_mapsTo400Bexa003() throws Exception {
        when(cssdKitService.create(any(), any(), anyList(), any()))
                .thenThrow(new InvalidKitRequestException("BEXA003",
                        "At least one kit item {itemId,qty} is required"));

        mockMvc.perform(post("/api/v1/kits").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kitCode\":\"K-X\",\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"))
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    @Test
    void create_duplicateKitCode_mapsTo400WithCode() throws Exception {
        when(cssdKitService.create(any(), any(), anyList(), any()))
                .thenThrow(new InvalidKitRequestException("CSSD_DUPLICATE_KIT_CODE",
                        "kitCode already exists: K-DUP"));

        mockMvc.perform(post("/api/v1/kits").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kitCode\":\"K-DUP\",\"items\":[{\"itemId\":1,\"qty\":1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("CSSD_DUPLICATE_KIT_CODE"));
    }

    @Test
    void startCycle_illegalTransition_mapsTo400WithCode() throws Exception {
        when(cssdKitService.startCycle(eq(5L), eq("AUTOCLAVE")))
                .thenThrow(new InvalidKitRequestException("CSSD_ILLEGAL_TRANSITION",
                        "Cannot start a sterilization cycle for kit 5 from status ISSUED"));

        mockMvc.perform(post("/api/v1/kits/5/cycles").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"AUTOCLAVE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("CSSD_ILLEGAL_TRANSITION"))
                .andExpect(jsonPath("$.message").value(
                        "Cannot start a sterilization cycle for kit 5 from status ISSUED"));
    }

    @Test
    void completeCycle_passAndFail_mapTo200() throws Exception {
        SterilizationCycle done = new SterilizationCycle();
        done.setCycleId(7L);
        done.setKitId(10L);
        done.setMethod("AUTOCLAVE");
        done.setCompletedAt(Timestamp.valueOf("2026-01-01 10:00:00"));
        when(cssdKitService.completeCycle(7L, "PASS")).thenAnswer(inv -> {
            done.setResult(SterilizationCycle.RESULT_PASS);
            return done;
        });

        mockMvc.perform(put("/api/v1/cycles/7/complete").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"result\":\"PASS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cycleId").value(7))
                .andExpect(jsonPath("$.result").value(1))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
    }

    @Test
    void completeCycle_unknownCycle_mapsTo404() throws Exception {
        when(cssdKitService.completeCycle(eq(999L), any()))
                .thenThrow(new CssdNotFoundException("NOT_FOUND", "Sterilization cycle not found: 999"));

        mockMvc.perform(put("/api/v1/cycles/999/complete").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"result\":\"PASS\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    void issue_returns201_andUnknownKitMapsTo404() throws Exception {
        KitIssue issue = new KitIssue();
        issue.setIssueId(3L);
        issue.setKitId(10L);
        issue.setIssuedToRef("OT-SCHED-42");
        when(cssdKitService.issue(10L, "OT-SCHED-42")).thenReturn(issue);

        mockMvc.perform(post("/api/v1/kits/10/issues").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"issuedToRef\":\"OT-SCHED-42\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.issueId").value(3))
                .andExpect(jsonPath("$.issuedToRef").value("OT-SCHED-42"));

        when(cssdKitService.issue(eq(404L), any()))
                .thenThrow(new CssdNotFoundException("NOT_FOUND", "Kit not found: 404"));
        mockMvc.perform(post("/api/v1/kits/404/issues").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"issuedToRef\":\"X\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    void list_acceptsNamedOrNumericStatusFilter() throws Exception {
        when(cssdKitService.list((short) Kit.STATUS_REJECTED, null))
                .thenReturn(List.of(kit(11L, "K-BAD", Kit.STATUS_REJECTED)));

        mockMvc.perform(get("/api/v1/kits").with(csrf()).with(user("devops").roles("DEV"))
                        .param("status", "REJECTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].kitId").value(11));

        mockMvc.perform(get("/api/v1/kits").with(csrf()).with(user("devops").roles("DEV"))
                        .param("status", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].kitId").value(11));
    }
}
