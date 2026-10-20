package com.hims.transport.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.transport.application.FleetService;
import com.hims.transport.configuration.SecurityConfig;
import com.hims.transport.domain.model.Driver;
import com.hims.transport.domain.model.FuelStationDetails;
import com.hims.transport.domain.model.Vehicle;
import com.hims.transport.domain.model.VehicleMaintenance;
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

/** Contract tests for the transport fleet API (default-deny enforced). Service is mocked; L1 covers the lifecycle. */
@WebMvcTest(FleetController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class FleetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FleetService service;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createVehicle_requiresAuth_defaultDeny() throws Exception {
        mockMvc.perform(post("/api/v1/transport/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleName\":\"Amb1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createVehicle_valid_returns201() throws Exception {
        Vehicle v = new Vehicle();
        v.setId(1L);
        v.setVehicleName("Amb1");
        v.setIsAvailable(true);
        when(service.createVehicle(any(), isNull())).thenReturn(v);

        mockMvc.perform(post("/api/v1/transport/vehicles")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleName\":\"Amb1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vehicleName").value("Amb1"))
                .andExpect(jsonPath("$.isAvailable").value(true));
    }

    @Test
    void listDrivers_returnsList() throws Exception {
        Driver d = new Driver();
        d.setId(1L);
        d.setFirstName("Ravi");
        when(service.listDrivers(eq(7L))).thenReturn(List.of(d));

        mockMvc.perform(get("/api/v1/transport/drivers?unitId=7")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("Ravi"));
    }

    @Test
    void addFuel_returns201() throws Exception {
        FuelStationDetails f = new FuelStationDetails();
        f.setId(1L);
        f.setReceiptNo("R1");
        when(service.addFuel(any(), isNull())).thenReturn(f);

        mockMvc.perform(post("/api/v1/transport/fuel")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiptNo\":\"R1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receiptNo").value("R1"));
    }

    @Test
    void addMaintenance_returns201() throws Exception {
        VehicleMaintenance m = new VehicleMaintenance();
        m.setId(1L);
        m.setWorkshopName("WS1");
        when(service.addMaintenance(any(), isNull())).thenReturn(m);

        mockMvc.perform(post("/api/v1/transport/maintenance")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workshopName\":\"WS1\",\"vehicleId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workshopName").value("WS1"));
    }

    @Test
    void approveMaintenance_delegates() throws Exception {
        VehicleMaintenance m = new VehicleMaintenance();
        m.setId(1L);
        m.setIsApproved(true);
        when(service.approveMaintenance(eq(1L))).thenReturn(m);

        mockMvc.perform(post("/api/v1/transport/maintenance/1/approve")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isApproved").value(true));
    }

    @Test
    void completeMaintenance_delegates() throws Exception {
        VehicleMaintenance m = new VehicleMaintenance();
        m.setId(1L);
        m.setIsCompleted(true);
        when(service.completeMaintenance(eq(1L), any())).thenReturn(m);

        mockMvc.perform(post("/api/v1/transport/maintenance/1/complete")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actualCost\":1500}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isCompleted").value(true));
    }

    @Test
    void unmappedPath_returns404_not500() throws Exception {
        mockMvc.perform(get("/api/v1/transport/nope/nope")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }
}
