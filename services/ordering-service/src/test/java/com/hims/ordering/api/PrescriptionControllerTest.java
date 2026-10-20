package com.hims.ordering.api;

import com.hims.ordering.application.PrescriptionService;
import com.hims.ordering.application.exceptions.InvalidPrescriptionException;
import com.hims.ordering.configuration.SecurityConfig;
import com.hims.ordering.domain.model.Prescription;
import com.hims.ordering.domain.model.PrescriptionItem;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** L1 — prescription API contract (default-deny; BR-ORD-005 errors surface as legacy envelope). */
@WebMvcTest(PrescriptionController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class PrescriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PrescriptionService prescriptionService;

    private static final String BODY = """
            {
              "patientId": 501, "encounterId": 7001, "prescriberId": 11,
              "lines": [
                {"itemId": 10, "qty": 2.5, "dose": "500mg", "frequency": "TDS"},
                {"itemId": 11, "qty": 1,   "dose": "10ml",  "frequency": "OD"}
              ]
            }
            """;

    private Prescription sample(short status) {
        Prescription p = new Prescription();
        p.setPrescriptionId(200L);
        p.setPatientId(501L);
        p.setEncounterId(7001L);
        p.setPrescriberId(11L);
        p.setStatusCode(status);
        PrescriptionItem d = new PrescriptionItem();
        d.setPrescriptionId(200L);
        d.setLineNo(1L);
        d.setItemId(10L);
        d.setDose("500mg");
        d.setFrequency("TDS");
        p.setDetails(List.of(d));
        return p;
    }

    @Test
    void createReturns201WithMultiItemLines() throws Exception {
        when(prescriptionService.create(any(), any(), any(), any(), any(), any()))
                .thenReturn(sample(Prescription.STATUS_ACTIVE));
        mockMvc.perform(post("/api/v1/prescriptions").with(user("u")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prescriptionId").value(200))
                .andExpect(jsonPath("$.statusCode").value(1))
                .andExpect(jsonPath("$.details.length()").value(1))
                .andExpect(jsonPath("$.details[0].dose").value("500mg"));
    }

    @Test
    void defaultDenyRejectsUnauthenticatedCreate() throws Exception {
        mockMvc.perform(post("/api/v1/prescriptions")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void brOrd005SurfacesAsLegacyEnvelope() throws Exception {
        when(prescriptionService.create(any(), any(), any(), any(), any(), anyList()))
                .thenThrow(new InvalidPrescriptionException("BR-ORD-005",
                        "dose is required for itemId 10"));
        mockMvc.perform(post("/api/v1/prescriptions").with(user("u")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("BR-ORD-005"));
    }

    @Test
    void listAndGet() throws Exception {
        when(prescriptionService.list((short) 1, 501L)).thenReturn(List.of(sample(Prescription.STATUS_ACTIVE)));
        when(prescriptionService.get(200L)).thenReturn(sample(Prescription.STATUS_ACTIVE));
        mockMvc.perform(get("/api/v1/prescriptions").with(user("u"))
                        .param("status", "ACTIVE").param("patientId", "501"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].prescriptionId").value(200));
        mockMvc.perform(get("/api/v1/prescriptions/200").with(user("u")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encounterId").value(7001));
    }

    @Test
    void cancelDelegates() throws Exception {
        when(prescriptionService.cancel(200L)).thenReturn(sample(Prescription.STATUS_CANCELLED));
        mockMvc.perform(post("/api/v1/prescriptions/200/cancel").with(user("u")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(3));
    }
}
