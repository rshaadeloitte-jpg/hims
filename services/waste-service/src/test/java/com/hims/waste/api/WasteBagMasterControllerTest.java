package com.hims.waste.api;

import com.hims.waste.application.WasteBagMasterService;
import com.hims.waste.application.exceptions.WasteNotFoundException;
import com.hims.waste.configuration.SecurityConfig;
import com.hims.waste.domain.model.WasteBagMaster;
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

/** Contract tests for the waste bag category master API (default-deny enforced). Service is mocked. */
@WebMvcTest(WasteBagMasterController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class WasteBagMasterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WasteBagMasterService service;

    @Test
    void create_requiresAuth_defaultDeny() throws Exception {
        mockMvc.perform(post("/api/v1/waste-bag-masters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"BAG-Y\",\"name\":\"Yellow\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_returns201WithStampedFields() throws Exception {
        WasteBagMaster b = new WasteBagMaster();
        b.setId(9L);
        b.setCode("BAG-Y");
        b.setName("Yellow bag");
        when(service.createBag(any(), isNull())).thenReturn(b);

        mockMvc.perform(post("/api/v1/waste-bag-masters")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"BAG-Y\",\"name\":\"Yellow bag\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.code").value("BAG-Y"));
    }

    @Test
    void list_scopesByUnitWhenParamPresent() throws Exception {
        WasteBagMaster b = new WasteBagMaster();
        b.setId(9L);
        b.setCode("BAG-Y");
        when(service.listBags(4L)).thenReturn(List.of(b));

        mockMvc.perform(get("/api/v1/waste-bag-masters")
                        .param("unitId", "4")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("BAG-Y"));
    }

    @Test
    void update_patchSemantics_delegatesToService() throws Exception {
        WasteBagMaster b = new WasteBagMaster();
        b.setId(9L);
        b.setCode("BAG-Y");
        b.setName("Yellow bag (large)");
        when(service.updateBag(eq(9L), any())).thenReturn(b);

        mockMvc.perform(put("/api/v1/waste-bag-masters/9")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Yellow bag (large)\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Yellow bag (large)"));
    }

    @Test
    void get_unknownId_returns404Envelope() throws Exception {
        when(service.getBag(77L)).thenThrow(new WasteNotFoundException("NOT_FOUND", "Waste bag master not found"));

        mockMvc.perform(get("/api/v1/waste-bag-masters/77")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }
}
