package com.hims.equipment.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.equipment.application.EquipmentService;
import com.hims.equipment.configuration.SecurityConfig;
import com.hims.equipment.domain.model.Equipment;
import com.hims.equipment.domain.model.EquipmentTransfer;
import com.hims.equipment.domain.model.EquipmentWorkingStatus;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests for the equipment asset-register API (default-deny enforced).
 * Service is mocked; L1 covers the business logic.
 */
@WebMvcTest(EquipmentController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class EquipmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EquipmentService service;

    @Autowired
    private ObjectMapper objectMapper;

    private Equipment asset(long id) {
        Equipment e = new Equipment();
        e.setId(id);
        e.setItemId(10L);
        e.setAccessionNo("ACC-" + id);
        e.setSerialNo("SN-" + id);
        e.setModel("MX");
        e.setMake("ACME");
        e.setWorkingStatus(EquipmentWorkingStatus.WORKING);
        e.setDepartmentId(3L);
        e.setLocation("Ward A");
        e.setCustodianName("Nurse");
        return e;
    }

    @Test
    void register_requiresAuth_defaultDeny() throws Exception {
        mockMvc.perform(post("/api/v1/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":10}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_validRequest_returns201() throws Exception {
        when(service.register(any(), isNull())).thenReturn(asset(1L));

        mockMvc.perform(post("/api/v1/equipment")
                        .with(user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":10,\"accessionNo\":\"ACC-1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.accessionNo").value("ACC-1"))
                .andExpect(jsonPath("$.workingStatus").value(EquipmentWorkingStatus.WORKING));
    }

    @Test
    void list_delegatesToSearch() throws Exception {
        when(service.search(eq(7L), isNull(), isNull())).thenReturn(List.of(asset(1L), asset(2L)));

        mockMvc.perform(get("/api/v1/equipment").param("unitId", "7").with(user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].accessionNo").value("ACC-1"));
    }

    @Test
    void byAccession_delegates() throws Exception {
        when(service.findByAccession(eq("ACC-1"), isNull())).thenReturn(asset(1L));

        mockMvc.perform(get("/api/v1/equipment/by-accession/ACC-1").with(user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessionNo").value("ACC-1"));
    }

    @Test
    void byItem_delegates() throws Exception {
        when(service.findByItem(eq(10L))).thenReturn(List.of(asset(1L)));

        mockMvc.perform(get("/api/v1/equipment/by-item/10").with(user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itemId").value(10));
    }

    @Test
    void get_delegates() throws Exception {
        when(service.getEquipment(eq(1L))).thenReturn(asset(1L));

        mockMvc.perform(get("/api/v1/equipment/1").with(user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void update_delegates() throws Exception {
        when(service.update(eq(1L), any())).thenReturn(asset(1L));

        mockMvc.perform(put("/api/v1/equipment/1")
                        .with(user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"location\":\"Ward B\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void transfer_returns201() throws Exception {
        EquipmentTransfer t = new EquipmentTransfer();
        t.setId(5L);
        t.setEquipmentId(1L);
        t.setFromDepartmentId(3L);
        t.setToDepartmentId(4L);
        t.setReason("redeploy");
        when(service.transfer(eq(1L), any())).thenReturn(t);

        mockMvc.perform(post("/api/v1/equipment/1/transfer")
                        .with(user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toDepartmentId\":4,\"reason\":\"redeploy\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.toDepartmentId").value(4));
    }

    @Test
    void transfers_delegates() throws Exception {
        EquipmentTransfer t = new EquipmentTransfer();
        t.setId(5L);
        t.setEquipmentId(1L);
        when(service.transferHistory(eq(1L))).thenReturn(List.of(t));

        mockMvc.perform(get("/api/v1/equipment/1/transfers").with(user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].equipmentId").value(1));
    }

    @Test
    void unmappedPath_returns404_not500() throws Exception {
        mockMvc.perform(get("/api/v1/equipment/does/not/exist").with(user("devops")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }
}
