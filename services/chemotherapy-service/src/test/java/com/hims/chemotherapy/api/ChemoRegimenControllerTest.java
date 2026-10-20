package com.hims.chemotherapy.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.chemotherapy.application.ChemoRegimenService;
import com.hims.chemotherapy.configuration.SecurityConfig;
import com.hims.chemotherapy.domain.model.ChemoRegimen;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChemoRegimenController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class ChemoRegimenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChemoRegimenService service;

    @Autowired
    private ObjectMapper objectMapper;

    private static ChemoRegimen sampleRegimen(Long id, Long patientId, String patientName, String regimenName, int currentCycle, int totalCycles, String status) {
        ChemoRegimen regimen = new ChemoRegimen();
        regimen.setId(id);
        regimen.setPatientId(patientId);
        regimen.setPatientName(patientName);
        regimen.setRegimenName(regimenName);
        regimen.setCurrentCycle(currentCycle);
        regimen.setTotalCycles(totalCycles);
        regimen.setStatus(status);
        regimen.setStartDate(LocalDate.of(2026, 9, 1));
        regimen.setNextDueDate(LocalDate.of(2026, 9, 15));
        return regimen;
    }

    @Test
    void unauthenticatedRequests_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/regimens"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/regimens/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/regimens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":100,\"regimenName\":\"FOLFOX\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listRegimens_authenticated_shouldReturnList() throws Exception {
        ChemoRegimen r1 = sampleRegimen(1L, 101L, "Jane Doe", "FOLFOX", 1, 6, "ACTIVE");
        ChemoRegimen r2 = sampleRegimen(2L, 102L, "John Smith", "AC-T", 2, 4, "PLANNED");
        when(service.getAllRegimens(null, null)).thenReturn(List.of(r1, r2));

        mockMvc.perform(get("/api/v1/regimens")
                        .with(user("oncologist").roles("DOCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].patientName").value("Jane Doe"))
                .andExpect(jsonPath("$[0].regimenName").value("FOLFOX"))
                .andExpect(jsonPath("$[0].currentCycle").value(1))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].nextDueDate").value("2026-09-15"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].patientName").value("John Smith"))
                .andExpect(jsonPath("$[1].regimenName").value("AC-T"));
    }

    @Test
    void getRegimenById_found_shouldReturn200() throws Exception {
        ChemoRegimen r = sampleRegimen(1L, 101L, "Jane Doe", "FOLFOX", 1, 6, "ACTIVE");
        when(service.findById(1L)).thenReturn(Optional.of(r));

        mockMvc.perform(get("/api/v1/regimens/1")
                        .with(user("oncologist").roles("DOCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.regimenName").value("FOLFOX"))
                .andExpect(jsonPath("$.currentCycle").value(1))
                .andExpect(jsonPath("$.cycles").value(6))
                .andExpect(jsonPath("$.patientName").value("Jane Doe"));
    }

    @Test
    void getRegimenById_notFound_shouldReturn404() throws Exception {
        when(service.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/regimens/999")
                        .with(user("oncologist").roles("DOCTOR")))
                .andExpect(status().isNotFound());
    }

    @Test
    void createRegimen_valid_shouldReturn201() throws Exception {
        ChemoRegimen created = sampleRegimen(10L, 101L, "Jane Doe", "CHOP", 1, 6, "PLANNED");
        when(service.createRegimen(any(ChemoRegimen.class))).thenReturn(created);

        String requestBody = """
                {
                    "patientId": 101,
                    "patientName": "Jane Doe",
                    "regimenName": "CHOP",
                    "cycles": 6
                }
                """;

        mockMvc.perform(post("/api/v1/regimens")
                        .with(user("oncologist").roles("DOCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.regimenName").value("CHOP"))
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.totalCycles").value(6));
    }

    @Test
    void advanceCycle_valid_shouldReturn200() throws Exception {
        ChemoRegimen advanced = sampleRegimen(1L, 101L, "Jane Doe", "FOLFOX", 2, 6, "ACTIVE");
        when(service.advanceCycle(1L)).thenReturn(advanced);

        mockMvc.perform(post("/api/v1/regimens/1/advance-cycle")
                        .with(user("oncologist").roles("DOCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.currentCycle").value(2))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void deleteRegimen_valid_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/v1/regimens/1")
                        .with(user("oncologist").roles("DOCTOR")))
                .andExpect(status().isNoContent());

        verify(service).deleteRegimen(1L);
    }
}
