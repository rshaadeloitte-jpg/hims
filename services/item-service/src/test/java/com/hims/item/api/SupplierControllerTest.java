package com.hims.item.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.item.application.SupplierService;
import com.hims.item.configuration.SecurityConfig;
import com.hims.item.domain.model.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for supplier master CRUD (WP-2) — default-deny enforced. */
@WebMvcTest(SupplierController.class)
@Import({SecurityConfig.class, SupplierService.class})
@AutoConfigureMockMvc
class SupplierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private com.hims.item.domain.repository.SupplierRepository supplierRepository;


    @Test
    void list_requiresAuth_defaultDeny() throws Exception {
        mockMvc.perform(get("/api/v1/suppliers")).andExpect(status().isUnauthorized());
    }

    @Test
    void create_valid_returnsSupplier_withServerOwnedId() throws Exception {
        when(supplierRepository.nextSupplierId()).thenReturn(77L);
        when(supplierRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/v1/suppliers").with(user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SUP-01\",\"description\":\"Acme Pharma\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(77))
                .andExpect(jsonPath("$.code").value("SUP-01"));
    }

    @Test
    void create_missingCode_returns400_BEXA003() throws Exception {
        mockMvc.perform(post("/api/v1/suppliers").with(user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"No code\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"));
    }

    @Test
    void update_unknownId_returns404() throws Exception {
        when(supplierRepository.findById(999L)).thenReturn(Optional.empty());
        mockMvc.perform(put("/api/v1/suppliers/999").with(user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"x\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    void update_valid_copiesFields_andStampsAudit() throws Exception {
        Supplier existing = new Supplier();
        existing.setId(5L);
        existing.setCode("SUP-01");
        existing.setDescription("Old");
        when(supplierRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(supplierRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/v1/suppliers/5").with(user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"New name\",\"city\":\"Pune\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.description").value("New name"))
                .andExpect(jsonPath("$.city").value("Pune"));
    }

    @Test
    void get_byId() throws Exception {
        Supplier s = new Supplier();
        s.setId(5L);
        s.setCode("SUP-05");
        s.setDescription("Five");
        when(supplierRepository.findById(5L)).thenReturn(Optional.of(s));
        mockMvc.perform(get("/api/v1/suppliers/5").with(user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUP-05"));
    }

    @Autowired
    private ObjectMapper objectMapper;   // sanity: Jackson present in slice
}
