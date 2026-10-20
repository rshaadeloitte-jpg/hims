package com.hims.file.api;

import com.hims.file.application.FileCategoryService;
import com.hims.file.configuration.SecurityConfig;
import com.hims.file.domain.model.FileCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FileCategoryController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class FileCategoryControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean FileCategoryService service;

    @Test
    void list_requiresAuth() throws Exception {
        mockMvc.perform(get("/api/v1/file-categories")).andExpect(status().isUnauthorized());
    }

    @Test
    void create_valid_returns201() throws Exception {
        FileCategory c = cat(1L, "PATIENT_DOCS", "Patient Documents");
        when(service.create(eq("PATIENT_DOCS"), eq("Patient Documents"), any(), any())).thenReturn(c);
        mockMvc.perform(post("/api/v1/file-categories").with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"PATIENT_DOCS\",\"name\":\"Patient Documents\",\"documentType\":\"PATIENT\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("PATIENT_DOCS"));
    }

    @Test
    void tree_returnsRoots() throws Exception {
        when(service.tree(any())).thenReturn(List.of(Map.of("id", 1, "code", "PATIENT_DOCS", "children", List.of())));
        mockMvc.perform(get("/api/v1/file-categories/tree").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("PATIENT_DOCS"));
    }

    @Test
    void rename_delegates() throws Exception {
        FileCategory c = cat(1L, "PATIENT_DOCS", "Renamed");
        when(service.rename(eq(1L), eq("Renamed"))).thenReturn(c);
        mockMvc.perform(put("/api/v1/file-categories/1").with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"));
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/file-categories/1").with(user("devops").roles("DEV")))
                .andExpect(status().isNoContent());
    }

    private static FileCategory cat(long id, String code, String name) {
        FileCategory c = new FileCategory();
        c.setId(id); c.setCode(code); c.setName(name); c.setDocumentType("PATIENT"); c.setStatus(true);
        return c;
    }
}
