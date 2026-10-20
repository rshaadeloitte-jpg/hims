package com.hims.grievance.api;

import com.hims.grievance.application.GrievanceMasterService;
import com.hims.grievance.configuration.SecurityConfig;
import com.hims.grievance.domain.model.GrievanceCategory;
import com.hims.grievance.domain.model.GrievanceSeverity;
import com.hims.grievance.domain.model.GrievanceSubCategory;
import com.hims.grievance.domain.repository.GrievanceCategoryRepository;
import com.hims.grievance.domain.repository.GrievanceSeverityRepository;
import com.hims.grievance.domain.repository.GrievanceSubCategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for the grievance classification masters API (default-deny enforced). Service + repositories mocked. */
@WebMvcTest(GrievanceMasterController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class GrievanceMasterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GrievanceMasterService service;

    @MockitoBean
    private GrievanceCategoryRepository categoryRepository;

    @MockitoBean
    private GrievanceSubCategoryRepository subCategoryRepository;

    @MockitoBean
    private GrievanceSeverityRepository severityRepository;

    @Test
    void createCategory_requiresAuth_defaultDeny() throws Exception {
        mockMvc.perform(post("/api/v1/grievance-categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"CAT-01\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createCategory_returns201() throws Exception {
        GrievanceCategory c = new GrievanceCategory();
        c.setId(3L);
        c.setCode("CAT-01");
        when(service.createCategory(any(), isNull())).thenReturn(c);

        mockMvc.perform(post("/api/v1/grievance-categories")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"CAT-01\",\"description\":\"Environment\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.code").value("CAT-01"));
    }

    @Test
    void listCategories_delegatesToRepository() throws Exception {
        GrievanceCategory c = new GrievanceCategory();
        c.setId(1L);
        c.setCode("CAT-01");
        when(categoryRepository.findByStatusTrueOrderByIdAsc()).thenReturn(List.of(c));

        mockMvc.perform(get("/api/v1/grievance-categories")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("CAT-01"));
    }

    @Test
    void getCategory_unknownId_returns404Envelope() throws Exception {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/grievance-categories/99")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    void listSubCategories_filtersByCategoryWhenParamPresent() throws Exception {
        GrievanceSubCategory s = new GrievanceSubCategory();
        s.setId(11L);
        s.setCategoryId(7L);
        s.setCode("SUB-01");
        when(subCategoryRepository.findByCategoryIdOrderByIdAsc(7L)).thenReturn(List.of(s));

        mockMvc.perform(get("/api/v1/grievance-sub-categories")
                        .param("categoryId", "7")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryId").value(7))
                .andExpect(jsonPath("$[0].code").value("SUB-01"));
    }

    @Test
    void updateSeverity_delegatesToService() throws Exception {
        GrievanceSeverity v = new GrievanceSeverity();
        v.setId(2L);
        v.setCode("SEV-1");
        when(service.updateSeverity(eq(2L), any())).thenReturn(v);

        mockMvc.perform(put("/api/v1/grievance-severities/2")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SEV-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.code").value("SEV-1"));
    }
}
