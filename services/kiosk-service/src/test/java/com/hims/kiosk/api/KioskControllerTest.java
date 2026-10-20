package com.hims.kiosk.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.kiosk.application.KioskService;
import com.hims.kiosk.domain.model.KioskRegistration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({KioskRegistrationController.class, KioskAppointmentController.class,
        KioskQueueController.class, KioskLabResultController.class})
class KioskControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private KioskService service;
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void register_returns201() throws Exception {
        KioskRegistration r = new KioskRegistration();
        r.setId(1L); r.setPatientName("A"); r.setRegistrationToken("tok");
        when(service.register(any(), any())).thenReturn(r);
        mockMvc.perform(post("/api/v1/kiosk/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientName\":\"A\",\"contactPhone\":\"1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void byToken_returns200WhenFound() throws Exception {
        KioskRegistration r = new KioskRegistration();
        r.setRegistrationToken("T1"); r.setPatientName("X");
        when(service.findByToken("T1")).thenReturn(Optional.of(r));
        mockMvc.perform(get("/api/v1/kiosk/registration/token/T1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientName").value("X"));
    }

    @Test
    void queue_dashboard_returnsCounts() throws Exception {
        when(service.kioskDashboard()).thenReturn(java.util.Map.of("date","2026-09-09","waiting",3L));
        mockMvc.perform(get("/api/v1/kiosk/queue/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waiting").value(3));
    }
}
