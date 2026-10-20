package com.hims.equipment.api;

import com.hims.equipment.application.MasterDataService;
import com.hims.equipment.application.exceptions.EquipmentNotFoundException;
import com.hims.equipment.application.exceptions.InvalidEquipmentRequestException;
import com.hims.equipment.configuration.SecurityConfig;
import com.hims.equipment.domain.model.ComplaintCategoryMaster;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests for the generic master-data API (Task 17). Service is mocked;
 * L1 covers the generic use cases. Default-deny is enforced by SecurityConfig.
 */
@WebMvcTest(MasterDataController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class MasterDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MasterDataService masterDataService;

    private ComplaintCategoryMaster category(long id, String code) {
        ComplaintCategoryMaster c = new ComplaintCategoryMaster();
        c.setId(id);
        c.setCode(code);
        c.setDescription("Category " + code);
        c.setStatus(true);
        return c;
    }

    @Test
    void list_requiresAuth_defaultDeny() throws Exception {
        mockMvc.perform(get("/api/v1/equipment/masters/complaint-category"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void supportedTypes_returnsRegistryNames() throws Exception {
        when(masterDataService.supportedTypes()).thenReturn(
                Set.of("complaint-category", "amc-agent", "working-status"));

        mockMvc.perform(get("/api/v1/equipment/masters").with(user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@ == 'complaint-category')]").exists())
                .andExpect(jsonPath("$[?(@ == 'amc-agent')]").exists());
    }

    @Test
    void list_passesAllFiltersThrough_andSerializesEntityFields() throws Exception {
        when(masterDataService.list("complaint-category", 7L, true, "vent")).thenReturn(
                List.of(category(3L, "VENT")));

        mockMvc.perform(get("/api/v1/equipment/masters/complaint-category")
                        .with(user("devops"))
                        .param("unitId", "7")
                        .param("status", "true")
                        .param("search", "vent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].code").value("VENT"))
                .andExpect(jsonPath("$[0].description").value("Category VENT"))
                .andExpect(jsonPath("$[0].status").value(true));
    }

    @Test
    void list_unknownMasterType_maps400BEXA003() throws Exception {
        when(masterDataService.list(eq("no-such-master"), isNull(), isNull(), isNull()))
                .thenThrow(new InvalidEquipmentRequestException("BEXA003",
                        "Unknown master type: no-such-master"));

        mockMvc.perform(get("/api/v1/equipment/masters/no-such-master").with(user("devops")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"));
    }

    @Test
    void get_byId_returnsRow() throws Exception {
        when(masterDataService.get("complaint-status", 2L)).thenReturn(category(2L, "OPEN"));

        mockMvc.perform(get("/api/v1/equipment/masters/complaint-status/2").with(user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.code").value("OPEN"));
    }

    @Test
    void get_unknownId_maps404() throws Exception {
        when(masterDataService.get("complaint-status", 99L)).thenThrow(
                new EquipmentNotFoundException("NOT_FOUND", "Master record not found: complaint-status/99"));

        mockMvc.perform(get("/api/v1/equipment/masters/complaint-status/99").with(user("devops")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    void get_nonNumericId_maps400InvalidParameter() throws Exception {
        mockMvc.perform(get("/api/v1/equipment/masters/complaint-status/abc").with(user("devops")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"))
                .andExpect(jsonPath("$.message").value("Invalid parameter: id"));
    }

    @Test
    void create_validRequest_returns201_withServerId() throws Exception {
        when(masterDataService.create(eq("amc-agent"), any(), eq(7L))).thenReturn(category(11L, "AMC-01"));

        mockMvc.perform(post("/api/v1/equipment/masters/amc-agent")
                        .with(user("devops"))
                        .param("unitId", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"AMC-01\",\"name\":\"Acme Biomed\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.code").value("AMC-01"));
    }

    @Test
    void create_missingRequiredField_maps400BEXA003() throws Exception {
        when(masterDataService.create(eq("complaint-category"), any(), isNull()))
                .thenThrow(new InvalidEquipmentRequestException("BEXA003",
                        "Missing required field 'code' for complaint-category"));

        mockMvc.perform(post("/api/v1/equipment/masters/complaint-category")
                        .with(user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"no code given\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"))
                .andExpect(jsonPath("$.message").value(
                        "Missing required field 'code' for complaint-category"));
    }

    @Test
    void update_patch_appliesAndReturnsRow() throws Exception {
        when(masterDataService.update(eq("maintenance-type"), eq(5L), any()))
                .thenReturn(category(5L, "PREVENTIVE"));

        mockMvc.perform(put("/api/v1/equipment/masters/maintenance-type/5")
                        .with(user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.code").value("PREVENTIVE"));
    }

    @Test
    void delete_softDeletes_returnsTombstonedRow() throws Exception {
        when(masterDataService.deactivate("insurance-agent", 8L)).thenReturn(category(8L, "INS-01"));

        mockMvc.perform(delete("/api/v1/equipment/masters/insurance-agent/8").with(user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(8));

        verify(masterDataService).deactivate("insurance-agent", 8L);
    }
}
