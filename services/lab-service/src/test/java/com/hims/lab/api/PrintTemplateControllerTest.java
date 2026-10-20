package com.hims.lab.api;

import com.hims.lab.application.PrintTemplateService;
import com.hims.lab.application.exceptions.InvalidLabRequestException;
import com.hims.lab.application.exceptions.LabDuplicateException;
import com.hims.lab.configuration.SecurityConfig;
import com.hims.lab.domain.model.ReportPrintTemplate;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PrintTemplateController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class PrintTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PrintTemplateService printTemplateService;

    @Test
    void endpoints_requireAuth_defaultDeny() throws Exception {
        mockMvc.perform(get("/api/v1/print-templates")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/print-templates").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_valid_returns201() throws Exception {
        ReportPrintTemplate t = new ReportPrintTemplate();
        t.setTemplateId(1L);
        t.setCode("CBC-STD");
        t.setTemplateType("REPORT");
        when(printTemplateService.create(anyString(), nullable(String.class), nullable(String.class), nullable(byte[].class), nullable(Long.class), nullable(Long.class), nullable(Long.class), nullable(Boolean.class), nullable(String.class)))
                .thenReturn(t);

        mockMvc.perform(post("/api/v1/print-templates").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"CBC-STD\",\"description\":\"CBC Standard\",\"templateType\":\"REPORT\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.templateId").value(1))
                .andExpect(jsonPath("$.code").value("CBC-STD"));
    }

    @Test
    void create_duplicateCode_returns409() throws Exception {
        when(printTemplateService.create(anyString(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new LabDuplicateException("LAB_REQ_DUPLICATE", "already exists"));

        mockMvc.perform(post("/api/v1/print-templates").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"DUP\",\"templateType\":\"REPORT\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("LAB_REQ_DUPLICATE"));
    }

    @Test
    void create_invalidType_returns400() throws Exception {
        when(printTemplateService.create(anyString(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new InvalidLabRequestException("LAB_REQ_INVALID", "templateType must be one of"));

        mockMvc.perform(post("/api/v1/print-templates").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"X\",\"templateType\":\"INVALID\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_returnsTemplate() throws Exception {
        ReportPrintTemplate t = new ReportPrintTemplate();
        t.setTemplateId(5L);
        t.setCode("LFT-STD");
        when(printTemplateService.get(5L)).thenReturn(t);

        mockMvc.perform(get("/api/v1/print-templates/5").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("LFT-STD"));
    }

    @Test
    void getByCode_returnsTemplate() throws Exception {
        ReportPrintTemplate t = new ReportPrintTemplate();
        t.setTemplateId(3L);
        t.setCode("CBC-STD");
        when(printTemplateService.getByCode("CBC-STD")).thenReturn(t);

        mockMvc.perform(get("/api/v1/print-templates/by-code/CBC-STD").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.templateId").value(3));
    }

    @Test
    void listByType_returnsTemplates() throws Exception {
        when(printTemplateService.listByType("REPORT")).thenReturn(List.of(new ReportPrintTemplate()));

        mockMvc.perform(get("/api/v1/print-templates").with(csrf()).with(user("devops").roles("DEV"))
                        .param("type", "REPORT"))
                .andExpect(status().isOk());
    }

    @Test
    void update_returnsUpdatedTemplate() throws Exception {
        ReportPrintTemplate t = new ReportPrintTemplate();
        t.setTemplateId(5L);
        t.setCode("LFT-STD");
        t.setDescription("Updated");
        when(printTemplateService.update(any(), nullable(String.class), nullable(String.class), nullable(byte[].class), nullable(Long.class), nullable(Long.class), nullable(Long.class), nullable(Boolean.class), nullable(String.class)))
                .thenReturn(t);

        mockMvc.perform(put("/api/v1/print-templates/5").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated"));
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/print-templates/5").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isNoContent());
    }
}
