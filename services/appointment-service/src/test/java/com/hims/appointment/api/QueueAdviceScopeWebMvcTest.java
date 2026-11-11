package com.hims.appointment.api;

import com.hims.appointment.configuration.JwtConfig;
import com.hims.appointment.configuration.SecurityConfig;
import com.hims.queue.api.QueueCounterController;
import com.hims.queue.application.QueueService;
import com.hims.queue.application.exceptions.InvalidQueueRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves that in appointment-service bundle, absorbed module queue's domain exception
 * is handled by com.hims.queue.api.GlobalExceptionHandler (400) and NOT swallowed
 * into 500 by com.hims.appointment.api.GlobalExceptionHandler.
 */
@WebMvcTest(QueueCounterController.class)
@Import({
        SecurityConfig.class,
        JwtConfig.class,
        com.hims.appointment.api.GlobalExceptionHandler.class,
        com.hims.queue.api.GlobalExceptionHandler.class
})
@AutoConfigureMockMvc
class QueueAdviceScopeWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QueueService queueService;

    @Test
    void postQueueCounters_withEmptyBody_returns400_not500() throws Exception {
        when(queueService.createCounter(any(), any()))
                .thenThrow(new InvalidQueueRequestException("BEXA003", "code and name are required"));

        mockMvc.perform(post("/api/v1/queue/counters")
                        .with(user("devops"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("BEXA003"))
                .andExpect(jsonPath("$.message").value("code and name are required"));
    }
}
