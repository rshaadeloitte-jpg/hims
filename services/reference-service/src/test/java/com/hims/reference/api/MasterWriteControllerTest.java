package com.hims.reference.api;

import com.hims.reference.application.MasterEntityRegistry;
import com.hims.reference.application.MasterWriteUseCase;
import com.hims.reference.configuration.SecurityConfig;
import com.hims.reference.domain.repository.MasterReadRepository;
import com.hims.reference.infrastructure.outbox.OutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests for the MIG-010 write endpoints (crudadd/crudupdate/crudremove).
 * Default-deny security is exercised: unauthenticated -> 401.
 */
@WebMvcTest(MasterWriteController.class)
@Import({SecurityConfig.class, MasterEntityRegistry.class, MasterWriteUseCase.class})
@AutoConfigureMockMvc
class MasterWriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MasterReadRepository repository;

    @MockitoBean
    private OutboxService outboxService;

    @Test
    void create_returns201WithEntity() throws Exception {
        doReturn(501L).when(repository).nextId("M_UNITMASTER_SEQ");
        mockMvc.perform(post("/api/v1/masters/unitmaster")
                        .with(user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"U1\",\"name\":\"Unit One\",\"status\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(501))
                .andExpect(jsonPath("$.code").value("U1"))
                .andExpect(jsonPath("$.name").value("Unit One"));
    }

    @Test
    void update_returns200WithEntity() throws Exception {
        doReturn(java.util.Optional.of(new com.hims.reference.domain.model.UnitMaster())).when(repository)
                .findByIdScoped(any(), anyLong(), any(), any());
        mockMvc.perform(put("/api/v1/masters/unitmaster/7")
                        .with(user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"));
    }

    @Test
    void remove_returns200WithEntity() throws Exception {
        com.hims.reference.domain.model.UnitMaster u = new com.hims.reference.domain.model.UnitMaster();
        u.setId(7L);
        doReturn(java.util.Optional.of(u)).when(repository).findByIdScoped(any(), anyLong(), any(), any());
        mockMvc.perform(delete("/api/v1/masters/unitmaster/7")
                        .with(user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void writeEndpoints_requireAuth_defaultDeny() throws Exception {
        mockMvc.perform(post("/api/v1/masters/unitmaster")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"U1\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/masters/unitmaster/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/masters/unitmaster/7"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_unknownEntity_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/masters/bogusmaster")
                        .with(user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"X\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("UNKNOWN_ENTITY"));
    }
}
