package com.hims.diet.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.diet.application.DietOrderService;
import com.hims.diet.configuration.SecurityConfig;
import com.hims.diet.domain.model.DietItemScheduleDetails;
import com.hims.diet.domain.model.DietOrderDetails;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for the diet order + food item schedule lifecycle API (default-deny enforced). Service is mocked; L1 covers the state machine. */
@WebMvcTest(DietOrderController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class DietOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DietOrderService service;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void raise_requiresAuth_defaultDeny() throws Exception {
        mockMvc.perform(post("/api/v1/diet-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void raise_validRequest_returns201() throws Exception {
        DietOrderDetails order = new DietOrderDetails();
        order.setId(1L);
        order.setOrderNumber("00001");
        order.setOrderStatus(1L);
        when(service.raiseDietOrder(any(), isNull())).thenReturn(order);

        mockMvc.perform(post("/api/v1/diet-orders")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":1,\"admissionId\":1,\"serviceId\":5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value("00001"))
                .andExpect(jsonPath("$.orderStatus").value(1));
    }

    @Test
    void listByAdmission_returnsArray() throws Exception {
        DietOrderDetails order = new DietOrderDetails();
        order.setId(1L);
        when(service.listOrdersByAdmission(eq(1L), eq(1L))).thenReturn(List.of(order));

        mockMvc.perform(get("/api/v1/diet-orders")
                        .param("admissionId", "1").param("unitId", "1")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void sign_returnsSignedTrue() throws Exception {
        when(service.signDietOrders(any(), eq(1L))).thenReturn(true);

        mockMvc.perform(put("/api/v1/diet-orders/sign")
                        .param("unitId", "1")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1,2]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signed").value(true));
    }

    @Test
    void orderFoodItems_returns201() throws Exception {
        DietItemScheduleDetails row = new DietItemScheduleDetails();
        row.setId(1L);
        row.setItemScheduleStatus(2L);
        when(service.orderFoodItems(any(), eq(1L))).thenReturn(List.of(row));

        mockMvc.perform(post("/api/v1/diet-orders/1/food-items")
                        .param("unitId", "1")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"admissionId\":1,\"sessions\":[{\"sessionId\":1,\"itemIds\":[10,11]}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].itemScheduleStatus").value(2));
    }

    @Test
    void advance_returnsUpdatedRow() throws Exception {
        DietItemScheduleDetails row = new DietItemScheduleDetails();
        row.setId(1L);
        row.setItemScheduleStatus(3L);
        when(service.advanceFoodOrder(anyLong(), anyLong(), anyLong(), anyLong(), anyLong()))
                .thenReturn(row);

        mockMvc.perform(put("/api/v1/diet-orders/food-items/advance")
                        .param("admissionId", "1").param("dietOrderId", "1")
                        .param("dietSessionId", "1").param("foodOrderId", "1")
                        .param("unitId", "1")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemScheduleStatus").value(3));
    }

    @Test
    void kitchen_returnsArray() throws Exception {
        DietItemScheduleDetails row = new DietItemScheduleDetails();
        row.setId(1L);
        row.setItemScheduleStatus(3L);
        when(service.listKitchen(eq(1L), isNull())).thenReturn(List.of(row));

        mockMvc.perform(get("/api/v1/diet-orders/kitchen")
                        .param("unitId", "1")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itemScheduleStatus").value(3));
    }

    @Test
    void unmappedPath_returns404_not500() throws Exception {
        mockMvc.perform(get("/api/v1/diet-orders/no-such-endpoint-xyz/1/2/3")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isNotFound());
    }
}
