package com.hims.waste.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.waste.application.BioMedicalWasteService;
import com.hims.waste.configuration.SecurityConfig;
import com.hims.waste.domain.model.BioMedicalWaste;
import com.hims.waste.domain.model.BioMedicalWasteDetails;
import com.hims.waste.domain.model.WasteStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for the BMW lifecycle API (default-deny enforced). Service is mocked; L1 covers the state machine. */
@WebMvcTest(BioMedicalWasteController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class BioMedicalWasteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BioMedicalWasteService service;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void raise_requiresAuth_defaultDeny() throws Exception {
        mockMvc.perform(post("/api/v1/biomedical-waste")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"request\":{\"storeId\":1}}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void raise_validRequest_returns201() throws Exception {
        BioMedicalWaste r = new BioMedicalWaste();
        r.setId(1L);
        r.setRequisitionNo("IHMS/BW/26/00001");
        r.setRequestStatus(WasteStatus.PENDING);
        when(service.raiseRequest(any(), isNull(), isNull())).thenReturn(r);

        mockMvc.perform(post("/api/v1/biomedical-waste")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"request\":{\"storeId\":1},\"details\":[]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requisitionNo").value("IHMS/BW/26/00001"))
                .andExpect(jsonPath("$.requestStatus").value(1));
    }

    @Test
    void counts_returnsDashboardMap() throws Exception {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("TOTAL", 5L);
        counts.put("PENDING", 2L);
        when(service.getCounts()).thenReturn(counts);

        mockMvc.perform(get("/api/v1/biomedical-waste/counts")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.TOTAL").value(5))
                .andExpect(jsonPath("$.PENDING").value(2));
    }

    @Test
    void approve_delegatesToService() throws Exception {
        BioMedicalWaste r = new BioMedicalWaste();
        r.setId(1L);
        r.setRequestStatus(WasteStatus.ACCEPTED);
        when(service.approve(1L)).thenReturn(r);

        mockMvc.perform(post("/api/v1/biomedical-waste/1/approve")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestStatus").value(2));
    }

    @Test
    void handover_passesHandedOverTo() throws Exception {
        BioMedicalWaste r = new BioMedicalWaste();
        r.setId(1L);
        r.setRequestStatus(WasteStatus.HANDOVERED);
        r.setHandedOverTo("FirmA");
        when(service.handover(eq(1L), eq("FirmA"))).thenReturn(r);

        mockMvc.perform(post("/api/v1/biomedical-waste/1/handover")
                        .param("handedOverTo", "FirmA")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.handedOverTo").value("FirmA"));
    }

    @Test
    void bags_returnsList() throws Exception {
        BioMedicalWasteDetails d = new BioMedicalWasteDetails();
        d.setId(5L);
        d.setBarcodeNo("2026080000001");
        d.setBagStatus(WasteStatus.PENDING);
        when(service.getDetails(1L)).thenReturn(List.of(d));

        mockMvc.perform(get("/api/v1/biomedical-waste/1/bags")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].barcodeNo").value("2026080000001"));
    }

    @Test
    void barcodeWeight_returnsWeight() throws Exception {
        when(service.getBagWeightByBarcode(1L, "2026080000001")).thenReturn(new BigDecimal("1.500"));

        mockMvc.perform(get("/api/v1/biomedical-waste/1/barcode/2026080000001/weight")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weight").value(1.5));
    }

    @Test
    void delete_softDelete() throws Exception {
        BioMedicalWaste r = new BioMedicalWaste();
        r.setId(1L);
        r.setStatus(false);
        r.setRequestStatus(0L);
        when(service.deleteRequest(1L)).thenReturn(r);

        mockMvc.perform(delete("/api/v1/biomedical-waste/1")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(false))
                .andExpect(jsonPath("$.requestStatus").value(0));
    }

    @Test
    void unmappedPath_returns404_not500() throws Exception {
        mockMvc.perform(get("/api/v1/biomedical-waste/nope/nope/nope/nope")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isNotFound());
    }
}
