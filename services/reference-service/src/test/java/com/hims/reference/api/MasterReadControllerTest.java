package com.hims.reference.api;

import com.hims.reference.application.MasterEntityRegistry;
import com.hims.reference.application.MasterReadUseCase;
import com.hims.reference.configuration.SecurityConfig;
import com.hims.reference.domain.model.UnitMaster;
import com.hims.reference.domain.repository.MasterReadRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests: JSON shape parity with the legacy master entity serialization
 * (field names verbatim, NON_NULL). Real use case + registry, mocked repository,
 * real SecurityConfig (default-deny).
 */
@WebMvcTest(MasterReadController.class)
@Import({SecurityConfig.class, MasterEntityRegistry.class, MasterReadUseCase.class})
@AutoConfigureMockMvc
class MasterReadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MasterReadRepository repository;

    private UnitMaster unit() {
        UnitMaster u = new UnitMaster();
        u.setId(7L);
        u.setUnitId(1L);
        u.setCode("U001");
        u.setName("Jaipur Hospital");
        u.setStatus(true);
        return u;
    }

    @Test
    @WithMockUser
    void byId_unknownEntity_returns400WithErrorCode() throws Exception {
        mockMvc.perform(get("/api/v1/masters/bogus/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("UNKNOWN_ENTITY"));
    }

    @Test
    void byId_withoutAuth_isRejected() throws Exception {
        mockMvc.perform(get("/api/v1/masters/unitmaster/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void entitiesEndpoint_listsSupportedMasters() throws Exception {
        mockMvc.perform(get("/api/v1/masters").with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@ == 'unitmaster')]").exists());
    }

    @Test
    @WithMockUser
    void byId_returnsLegacyShapedJson() throws Exception {
        doReturn(Optional.of(unit())).when(repository)
                .findByIdScoped(eq(UnitMaster.class), eq(7L), any(), any());
        mockMvc.perform(get("/api/v1/masters/unitmaster/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.unitId").value(1))
                .andExpect(jsonPath("$.code").value("U001"))
                .andExpect(jsonPath("$.name").value("Jaipur Hospital"))
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @WithMockUser
    void byId_missingRecord_returns404() throws Exception {
        doReturn(Optional.empty()).when(repository)
                .findByIdScoped(eq(UnitMaster.class), eq(99L), any(), any());
        mockMvc.perform(get("/api/v1/masters/unitmaster/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    @WithMockUser
    void byId_zeroIsInvalidInput() throws Exception {
        mockMvc.perform(get("/api/v1/masters/unitmaster/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"));
    }

    @Test
    @WithMockUser
    void all_returnsJsonArray() throws Exception {
        doReturn(java.util.List.of(unit())).when(repository)
                .findAll(eq(UnitMaster.class), any(), any(), any(), eq(0), eq(0), any());
        mockMvc.perform(get("/api/v1/masters/unitmaster/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Jaipur Hospital"));
    }
}
