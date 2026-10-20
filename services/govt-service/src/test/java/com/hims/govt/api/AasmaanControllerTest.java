package com.hims.govt.api;

import com.hims.govt.application.AasmaanService;
import com.hims.govt.configuration.SecurityConfig;
import com.hims.govt.application.exceptions.InvalidGovtRequestException;
import com.hims.govt.domain.model.AasmaanCase;
import com.hims.govt.domain.model.AasmaanCaseNote;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for the Aasmaan API (default-deny enforced). Services mocked; L0 covers the rules. */
@WebMvcTest(AasmaanController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class AasmaanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AasmaanService aasmaanService;

    private static AasmaanCase kase(long id, short status) {
        AasmaanCase c = new AasmaanCase();
        c.setCaseId(id);
        c.setPatientId(11L);
        c.setCaseType("ANC");
        c.setStatus(status);
        return c;
    }

    @Test
    void endpoints_requireAuth_defaultDeny() throws Exception {
        mockMvc.perform(get("/api/v1/aasmaan/cases")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/aasmaan/cases").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/aasmaan/cases/1/notes").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"n\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/aasmaan/cases/1/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_valid_returns201Open() throws Exception {
        when(aasmaanService.create(11L, "ANC")).thenReturn(kase(3L, AasmaanCase.STATUS_OPEN));

        mockMvc.perform(post("/api/v1/aasmaan/cases").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":11,\"caseType\":\"ANC\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.caseId").value(3))
                .andExpect(jsonPath("$.status").value(1));
    }

    @Test
    void addNote_closedCase_mapsTo400GovtCaseClosed() throws Exception {
        when(aasmaanService.addNote(1L, "late note", "asha-7"))
                .thenThrow(new InvalidGovtRequestException("GOVT_CASE_CLOSED",
                        "Cannot add a note to CLOSED case 1"));

        mockMvc.perform(post("/api/v1/aasmaan/cases/1/notes").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"late note\",\"author\":\"asha-7\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("GOVT_CASE_CLOSED"))
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    @Test
    void updateStatus_illegalTransition_mapsTo400Envelope() throws Exception {
        when(aasmaanService.updateStatus(2L, "OPEN"))
                .thenThrow(new InvalidGovtRequestException("GOVT_ILLEGAL_TRANSITION",
                        "Illegal status move for case 2: IN_PROGRESS -> OPEN"));

        mockMvc.perform(put("/api/v1/aasmaan/cases/2/status").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("GOVT_ILLEGAL_TRANSITION"));
    }

    @Test
    void updateStatus_forwardMove_returnsUpdatedCase() throws Exception {
        when(aasmaanService.updateStatus(1L, "IN_PROGRESS")).thenReturn(kase(1L, AasmaanCase.STATUS_IN_PROGRESS));

        mockMvc.perform(put("/api/v1/aasmaan/cases/1/status").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2));
    }

    @Test
    void list_acceptsNamedOrNumericStatusAndPatientFilters() throws Exception {
        when(aasmaanService.list((short) 2, 11L)).thenReturn(List.of(kase(5L, AasmaanCase.STATUS_IN_PROGRESS)));

        mockMvc.perform(get("/api/v1/aasmaan/cases").with(csrf()).with(user("devops").roles("DEV"))
                        .param("status", "IN_PROGRESS").param("patientId", "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].caseId").value(5));

        mockMvc.perform(get("/api/v1/aasmaan/cases").with(csrf()).with(user("devops").roles("DEV"))
                        .param("status", "2").param("patientId", "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].caseId").value(5));
    }

    @Test
    void notes_listReturned() throws Exception {
        AasmaanCaseNote n = new AasmaanCaseNote();
        n.setNoteId(8L);
        n.setCaseId(1L);
        n.setNote("first contact");
        n.setAuthor("asha-7");
        when(aasmaanService.notes(1L)).thenReturn(List.of(n));

        mockMvc.perform(get("/api/v1/aasmaan/cases/1/notes").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].noteId").value(8))
                .andExpect(jsonPath("$[0].note").value("first contact"));
    }

    @Test
    void unknownCase_mapsTo404Envelope() throws Exception {
        when(aasmaanService.get(99L)).thenThrow(new com.hims.govt.application.exceptions.GovtNotFoundException(
                "NOT_FOUND", "Aasmaan case not found: 99"));

        mockMvc.perform(get("/api/v1/aasmaan/cases/99").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }
}
