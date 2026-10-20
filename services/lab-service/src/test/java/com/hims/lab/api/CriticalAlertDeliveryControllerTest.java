package com.hims.lab.api;

import com.hims.lab.application.CriticalAlertDeliveryService;
import com.hims.lab.application.exceptions.InvalidLabRequestException;
import com.hims.lab.configuration.SecurityConfig;
import com.hims.lab.domain.model.CriticalAlertDelivery;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CriticalAlertDeliveryController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class CriticalAlertDeliveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CriticalAlertDeliveryService deliveryService;

    @Test
    void endpoints_requireAuth_defaultDeny() throws Exception {
        mockMvc.perform(get("/api/v1/critical-alerts/pending")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/critical-alerts").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createDeliveries_valid_returns201WithListOfDeliveries() throws Exception {
        CriticalAlertDelivery d = new CriticalAlertDelivery();
        d.setDeliveryId(1L);
        d.setChannel("WHATSAPP");
        d.setDeliveryStatus("PENDING");
        when(deliveryService.createDeliveries(anyLong(), anyLong(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(d));

        mockMvc.perform(post("/api/v1/critical-alerts").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\":10,\"detailId\":21,\"informPassedTo\":\"Dr. Smith\",\"channels\":[\"WHATSAPP\",\"SMS\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].deliveryId").value(1));
    }

    @Test
    void createDeliveries_noCriticalValues_returns400() throws Exception {
        when(deliveryService.createDeliveries(anyLong(), anyLong(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new InvalidLabRequestException("LAB_REQ_INVALID", "has no critical values"));

        mockMvc.perform(post("/api/v1/critical-alerts").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\":10,\"detailId\":21}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("LAB_REQ_INVALID"));
    }

    @Test
    void markSent_returnsSent() throws Exception {
        CriticalAlertDelivery d = new CriticalAlertDelivery();
        d.setDeliveryId(5L);
        d.setDeliveryStatus("SENT");
        when(deliveryService.markSent(5L)).thenReturn(d);

        mockMvc.perform(post("/api/v1/critical-alerts/5/send").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryStatus").value("SENT"));
    }

    @Test
    void markDelivered_returnsDelivered() throws Exception {
        CriticalAlertDelivery d = new CriticalAlertDelivery();
        d.setDeliveryId(5L);
        d.setDeliveryStatus("DELIVERED");
        when(deliveryService.markDelivered(5L)).thenReturn(d);

        mockMvc.perform(post("/api/v1/critical-alerts/5/deliver").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryStatus").value("DELIVERED"));
    }

    @Test
    void retry_failedDelivery_returnsRetryStatus() throws Exception {
        CriticalAlertDelivery d = new CriticalAlertDelivery();
        d.setDeliveryId(5L);
        d.setDeliveryStatus("RETRY");
        d.setRetryCount(1);
        when(deliveryService.retry(5L)).thenReturn(d);

        mockMvc.perform(post("/api/v1/critical-alerts/5/retry").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryStatus").value("RETRY"));
    }

    @Test
    void listPending_returnsDeliveries() throws Exception {
        when(deliveryService.listPending()).thenReturn(List.of(new CriticalAlertDelivery()));

        mockMvc.perform(get("/api/v1/critical-alerts/pending").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isOk());
    }

    @Test
    void getByBooking_returnsDeliveries() throws Exception {
        when(deliveryService.listByBooking(10L)).thenReturn(List.of(new CriticalAlertDelivery()));

        mockMvc.perform(get("/api/v1/critical-alerts/booking/10").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isOk());
    }
}
