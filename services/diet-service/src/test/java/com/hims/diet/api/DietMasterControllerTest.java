package com.hims.diet.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.diet.application.DietMasterService;
import com.hims.diet.configuration.SecurityConfig;
import com.hims.diet.domain.model.DietItemMaster;
import com.hims.diet.domain.model.DietSessionMaster;
import com.hims.diet.domain.model.TodayDietMenu;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for the diet masters + today-menu API (default-deny enforced). Service is mocked; L1 covers the logic. */
@WebMvcTest(DietMasterController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class DietMasterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DietMasterService service;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createItem_requiresAuth_defaultDeny() throws Exception {
        mockMvc.perform(post("/api/v1/diet/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Rice\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createItem_validRequest_returns201() throws Exception {
        DietItemMaster item = new DietItemMaster();
        item.setId(1L);
        item.setDescription("Rice");
        when(service.createDietItem(any(), isNull())).thenReturn(item);

        mockMvc.perform(post("/api/v1/diet/items")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Rice\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Rice"));
    }

    @Test
    void listItems_returnsArray() throws Exception {
        DietItemMaster item = new DietItemMaster();
        item.setId(1L);
        item.setDescription("Rice");
        when(service.listDietItems(eq(1L))).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/diet/items").param("unitId", "1")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Rice"));
    }

    @Test
    void createSession_validRequest_returns201() throws Exception {
        DietSessionMaster session = new DietSessionMaster();
        session.setId(1L);
        session.setDescription("Breakfast");
        when(service.createDietSession(any(), isNull())).thenReturn(session);

        mockMvc.perform(post("/api/v1/diet/sessions")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Breakfast\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Breakfast"));
    }

    @Test
    void upsertTodayMenu_validRequest_returns201() throws Exception {
        TodayDietMenu menu = new TodayDietMenu();
        menu.setId(1L);
        menu.setDescription("Rice");
        when(service.upsertTodayMenuItem(any(), isNull())).thenReturn(menu);

        mockMvc.perform(post("/api/v1/diet/today-menu")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dietTypeId\":1,\"dietSessionId\":1,\"itemId\":1,\"description\":\"Rice\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Rice"));
    }

    @Test
    void listTodayMenu_requiresUnitId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/diet/today-menu")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"));
    }
}
