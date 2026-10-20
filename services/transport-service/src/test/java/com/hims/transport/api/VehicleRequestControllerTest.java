package com.hims.transport.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.transport.application.VehicleRequestService;
import com.hims.transport.configuration.SecurityConfig;
import com.hims.transport.domain.model.VehicleRequest;
import com.hims.transport.domain.model.VehicleRequestDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for the transport request lifecycle API (default-deny enforced). Service is mocked; L1 covers the state machine. */
@WebMvcTest(VehicleRequestController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class VehicleRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VehicleRequestService service;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void raise_requiresAuth_defaultDeny() throws Exception {
        mockMvc.perform(post("/api/v1/transport-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requisitionBy\":\"Asha\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void raise_validRequest_returns201_autoApproved() throws Exception {
        VehicleRequest r = new VehicleRequest();
        r.setId(1L);
        r.setRequisitionBy("Asha");
        r.setIsApproved(true);
        when(service.raiseRequest(any(), isNull())).thenReturn(r);

        mockMvc.perform(post("/api/v1/transport-requests")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requisitionBy\":\"Asha\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requisitionBy").value("Asha"))
                .andExpect(jsonPath("$.isApproved").value(true));
    }

    @Test
    void counts_returnsDashboardMap() throws Exception {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("TOTAL", 4L);
        counts.put("EMERGENCY", 1L);
        counts.put("NORMAL", 2L);
        counts.put("SCHEDULED", 1L);
        when(service.getCounts(isNull(), isNull())).thenReturn(counts);

        mockMvc.perform(get("/api/v1/transport-requests/counts")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.TOTAL").value(4))
                .andExpect(jsonPath("$.EMERGENCY").value(1));
    }

    @Test
    void allocate_delegatesAndReturnsRequest() throws Exception {
        VehicleRequest r = new VehicleRequest();
        r.setId(7L);
        r.setVehicleId(10L);
        r.setDriverId(20L);
        when(service.allocateVehicle(any(), isNull())).thenReturn(r);

        mockMvc.perform(post("/api/v1/transport-requests/7/allocate")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleId\":10,\"driverId\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleId").value(10))
                .andExpect(jsonPath("$.driverId").value(20));
    }

    @Test
    void vehicleOut_delegates() throws Exception {
        VehicleRequest r = new VehicleRequest();
        r.setId(7L);
        r.setIsVehicleOut(true);
        when(service.vehicleOut(any(), isNull())).thenReturn(r);

        mockMvc.perform(post("/api/v1/transport-requests/7/vehicle-out")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startOdmReading\":1000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isVehicleOut").value(true));
    }

    @Test
    void vehicleIn_delegates() throws Exception {
        VehicleRequest r = new VehicleRequest();
        r.setId(7L);
        r.setCompleted(true);
        when(service.vehicleIn(any(), isNull())).thenReturn(r);

        mockMvc.perform(post("/api/v1/transport-requests/7/vehicle-in")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actualFare\":500}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));
    }

    @Test
    void cancel_delegates() throws Exception {
        VehicleRequest r = new VehicleRequest();
        r.setId(7L);
        r.setIsCancelled(true);
        when(service.cancel(eq(7L))).thenReturn(r);

        mockMvc.perform(post("/api/v1/transport-requests/7/cancel")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isCancelled").value(true));
    }

    @Test
    void details_returnsList() throws Exception {
        VehicleRequestDetails d = new VehicleRequestDetails();
        d.setId(1L);
        d.setVehicleRequestId(7L);
        d.setStartOdmReading(1000L);
        when(service.getDetails(eq(7L))).thenReturn(List.of(d));

        mockMvc.perform(get("/api/v1/transport-requests/7/details")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].startOdmReading").value(1000));
    }

    @Test
    void unmappedPath_returns404_not500() throws Exception {
        mockMvc.perform(get("/api/v1/transport-requests/nope/nope/nope")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }
}
