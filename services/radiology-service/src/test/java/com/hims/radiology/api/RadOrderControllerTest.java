package com.hims.radiology.api;

import com.hims.radiology.application.RadOrderService;
import com.hims.radiology.application.exceptions.InvalidRadOrderException;
import com.hims.radiology.application.exceptions.RadOrderNotFoundException;
import com.hims.radiology.configuration.SecurityConfig;
import com.hims.radiology.domain.model.RadOrderBooking;
import com.hims.radiology.domain.model.RadOrderDetail;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
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

/** Contract tests for the Rad Order API (default-deny enforced). Services mocked; L0 covers the rules. */
@WebMvcTest(RadOrderController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class RadOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RadOrderService radOrderService;

    private static RadOrderBooking order(long id, long patientId, String modality, short status) {
        RadOrderBooking o = new RadOrderBooking();
        o.setRadOrderId(id);
        o.setPatientId(patientId);
        o.setEncounterId(7777L);
        o.setModality(modality);
        o.setPriority(RadOrderBooking.PRIORITY_ROUTINE);
        o.setStatus(status);
        return o;
    }

    @Test
    void endpoints_requireAuth_defaultDeny() throws Exception {
        mockMvc.perform(get("/api/v1/rad-orders")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/rad-orders/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/rad-orders").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/rad-orders/1/start").with(csrf()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/rad-orders/1/capture-image").with(csrf()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/rad-orders/1/report").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/rad-orders/1/cancel").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_valid_returns201ScheduledWithLines() throws Exception {
        RadOrderBooking saved = order(10L, 505L, "CT", RadOrderBooking.STATUS_SCHEDULED);
        RadOrderDetail d = new RadOrderDetail();
        d.setRadOrderId(10L);
        d.setLineNo(1L);
        d.setItemId(501L);
        d.setQty(new BigDecimal("2.50"));
        saved.setDetails(List.of(d));
        when(radOrderService.create(eq(505L), eq(7777L), isNull(), eq("CT"), isNull(), anyList()))
                .thenReturn(saved);

        mockMvc.perform(post("/api/v1/rad-orders").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":505,"encounterId":7777,"modality":"CT",
                                 "lines":[{"itemId":501,"qty":2.5}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.radOrderId").value(10))
                .andExpect(jsonPath("$.status").value(1))
                .andExpect(jsonPath("$.details[0].lineNo").value(1))
                .andExpect(jsonPath("$.details[0].itemId").value(501));
    }

    @Test
    void create_unknownModality_mapsTo400Bexa003() throws Exception {
        when(radOrderService.create(any(), any(), any(), any(), any(), any()))
                .thenThrow(new InvalidRadOrderException("BEXA003",
                        "Unknown modality: PET — allowed: XRAY, CT, MRI, US"));

        mockMvc.perform(post("/api/v1/rad-orders").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":505,"modality":"PET","lines":[{"itemId":501,"qty":1}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"))
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void create_noLines_mapsTo400Bexa003() throws Exception {
        when(radOrderService.create(any(), any(), any(), any(), any(), any()))
                .thenThrow(new InvalidRadOrderException("BEXA003",
                        "At least one rad order line {itemId,qty} is required"));

        mockMvc.perform(post("/api/v1/rad-orders").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":505,"modality":"XRAY","lines":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"));
    }

    @Test
    void create_encounterAndAdmissionTogether_mapsTo400() throws Exception {
        when(radOrderService.create(any(), any(), any(), any(), any(), any()))
                .thenThrow(new InvalidRadOrderException("BEXA003",
                        "encounterId and admissionId are mutually exclusive — supply at most one"));

        mockMvc.perform(post("/api/v1/rad-orders").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":505,"encounterId":7777,"admissionId":8888,"modality":"MRI",
                                 "lines":[{"itemId":501,"qty":1}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"));
    }

    @Test
    void start_illegalTransition_mapsTo400WithCode() throws Exception {
        when(radOrderService.start(10L))
                .thenThrow(new InvalidRadOrderException("RAD_ILLEGAL_TRANSITION",
                        "Cannot start rad order 10 from status COMPLETED"));

        mockMvc.perform(post("/api/v1/rad-orders/10/start")
                        .with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("RAD_ILLEGAL_TRANSITION"));
    }

    @Test
    void captureImage_onCompleted_mapsTo400() throws Exception {
        when(radOrderService.captureImage(11L))
                .thenThrow(new InvalidRadOrderException("RAD_ILLEGAL_TRANSITION",
                        "Cannot capture image for rad order 11 — order is already COMPLETED"));

        mockMvc.perform(post("/api/v1/rad-orders/11/capture-image")
                        .with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("RAD_ILLEGAL_TRANSITION"));
    }

    @Test
    void report_valid_returnsCompletedOrder() throws Exception {
        RadOrderBooking reported = order(12L, 505L, "US", RadOrderBooking.STATUS_COMPLETED);
        when(radOrderService.report(eq(12L), eq("No acute findings"), eq("Dr. Ray")))
                .thenReturn(reported);

        mockMvc.perform(post("/api/v1/rad-orders/12/report")
                        .with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reportText":"No acute findings","reportedBy":"Dr. Ray"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.radOrderId").value(12))
                .andExpect(jsonPath("$.status").value(3));
    }

    @Test
    void cancel_illegalTransition_mapsTo400WithCode() throws Exception {
        when(radOrderService.cancel(13L))
                .thenThrow(new InvalidRadOrderException("RAD_ILLEGAL_TRANSITION",
                        "Cannot cancel rad order 13 from status COMPLETED"));

        mockMvc.perform(post("/api/v1/rad-orders/13/cancel")
                        .with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("RAD_ILLEGAL_TRANSITION"));
    }

    @Test
    void get_unknownRadOrder_mapsTo404() throws Exception {
        when(radOrderService.get(404L))
                .thenThrow(new RadOrderNotFoundException("NOT_FOUND", "not found"));

        mockMvc.perform(get("/api/v1/rad-orders/404").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    void list_acceptsNamedOrNumericStatusFilter_andModality() throws Exception {
        when(radOrderService.list((short) 4, 505L, null)).thenReturn(List.of(order(14L, 505L, "XRAY", RadOrderBooking.STATUS_CANCELLED)));

        mockMvc.perform(get("/api/v1/rad-orders").with(csrf()).with(user("devops").roles("DEV"))
                        .param("status", "CANCELLED").param("patientId", "505"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].radOrderId").value(14));

        mockMvc.perform(get("/api/v1/rad-orders").with(csrf()).with(user("devops").roles("DEV"))
                        .param("status", "4").param("patientId", "505"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].radOrderId").value(14));

        // modality travels raw to the service layer (normalization is an L0/L2 concern)
        when(radOrderService.list(isNull(), isNull(), eq("mri"))).thenReturn(List.of(order(15L, 9L, "MRI", RadOrderBooking.STATUS_SCHEDULED)));
        mockMvc.perform(get("/api/v1/rad-orders").with(csrf()).with(user("devops").roles("DEV"))
                        .param("modality", "mri"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].modality").value("MRI"));
    }

    @Test
    void list_unknownStatusName_mapsTo400() throws Exception {
        mockMvc.perform(get("/api/v1/rad-orders").with(csrf()).with(user("devops").roles("DEV"))
                        .param("status", "BOGUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"));
    }
}
