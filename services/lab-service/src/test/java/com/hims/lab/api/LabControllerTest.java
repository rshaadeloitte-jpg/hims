package com.hims.lab.api;

import com.hims.lab.application.LabOrderService;
import com.hims.lab.application.MachineService;
import com.hims.lab.application.exceptions.InvalidLabRequestException;
import com.hims.lab.application.exceptions.LabEntityNotFoundException;
import com.hims.lab.configuration.SecurityConfig;
import com.hims.lab.domain.model.MachineConfiguration;
import com.hims.lab.domain.model.PathOrderBooking;
import com.hims.lab.domain.model.PathOrderDetail;
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
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for the Lab API (default-deny enforced). Services mocked; L1 covers the rules. */
@WebMvcTest({LabOrderController.class, MachineController.class})
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class LabControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LabOrderService labOrderService;

    @MockitoBean
    private MachineService machineService;

    private static PathOrderBooking booking(long id, long patientId, long encounterId) {
        PathOrderBooking b = new PathOrderBooking();
        b.setBookingId(id);
        b.setPatientId(patientId);
        b.setEncounterId(encounterId);
        b.setBookingNo("LAB-" + id);
        b.setBookingStatus(PathOrderBooking.STATUS_ORDERED);
        return b;
    }

    private static PathOrderDetail detail(long id, long bookingId) {
        PathOrderDetail d = new PathOrderDetail();
        d.setDetailId(id);
        d.setBookingId(bookingId);
        d.setTestCode("CBC");
        d.setTestName("Complete Blood Count");
        d.setDetailStatus(PathOrderBooking.STATUS_ORDERED);
        return d;
    }

    @Test
    void endpoints_requireAuth_defaultDeny() throws Exception {
        mockMvc.perform(get("/api/v1/lab/orders")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/machines")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/lab/orders").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_valid_returns201WithBookingNo() throws Exception {
        PathOrderBooking saved = booking(10L, 505L, 7777L);
        saved.setDetails(List.of(detail(21L, 10L)));
        when(labOrderService.create(eq(505L), eq(7777L), nullable(Boolean.class), anyList())).thenReturn(saved);

        mockMvc.perform(post("/api/v1/lab/orders").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"patientId":505,"encounterId":7777,
                                 "tests":[{"testCode":"CBC","testName":"Complete Blood Count"}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingNo").value("LAB-10"))
                .andExpect(jsonPath("$.bookingStatus").value(1))
                .andExpect(jsonPath("$.details[0].detailId").value(21));
    }

    @Test
    void create_missingPatient_returns400() throws Exception {
        when(labOrderService.create(any(), any(), any(), any()))
                .thenThrow(new InvalidLabRequestException("LAB_REQ_INVALID", "patientId is required"));

        mockMvc.perform(post("/api/v1/lab/orders").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tests\":[{\"testCode\":\"CBC\",\"testName\":\"CBC\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("LAB_REQ_INVALID"))
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    @Test
    void collect_illegalTransition_mapsTo400WithCode() throws Exception {
        when(labOrderService.collect(10L))
                .thenThrow(new InvalidLabRequestException("LAB_ILLEGAL_TRANSITION",
                        "Cannot collect booking 10 from status 3"));

        mockMvc.perform(post("/api/v1/lab/orders/10/collect").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("LAB_ILLEGAL_TRANSITION"));
    }

    @Test
    void get_unknownBooking_mapsTo404() throws Exception {
        when(labOrderService.get(404L)).thenThrow(new LabEntityNotFoundException("NOT_FOUND", "not found"));

        mockMvc.perform(get("/api/v1/lab/orders/404").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    void worklist_acceptsNamedOrNumericStatusFilter() throws Exception {
        when(labOrderService.worklist(eq((short) 3), eq(505L)))
                .thenReturn(List.of(booking(11L, 505L, 1L)));

        mockMvc.perform(get("/api/v1/lab/orders").with(csrf()).with(user("devops").roles("DEV"))
                        .param("status", "IN_PROGRESS").param("patientId", "505"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookingId").value(11));

        mockMvc.perform(get("/api/v1/lab/orders").with(csrf()).with(user("devops").roles("DEV"))
                        .param("status", "3").param("patientId", "505"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookingId").value(11));
    }

    @Test
    void upsertResults_returnsDetail() throws Exception {
        PathOrderDetail d = detail(21L, 10L);
        when(labOrderService.upsertResults(eq(10L), eq(21L), anyList())).thenReturn(d);

        mockMvc.perform(post("/api/v1/lab/orders/10/details/21/results")
                        .with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"results\":[{\"parameterName\":\"Hb\",\"value\":\"13.5\",\"unit\":\"g/dL\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detailId").value(21));
    }

    @Test
    void finalize_returnsFinalizedBooking() throws Exception {
        PathOrderBooking finalized = booking(10L, 505L, 7777L);
        finalized.setBookingStatus(PathOrderBooking.STATUS_FINALIZED);
        when(labOrderService.finalizeDetail(10L, 21L)).thenReturn(finalized);

        mockMvc.perform(post("/api/v1/lab/orders/10/details/21/finalize")
                        .with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingStatus").value(4));
    }

    // ------------------------------------------------------------- M15 workflow contracts

    @Test
    void barcode_requiresAuth() throws Exception {
        mockMvc.perform(post("/api/v1/lab/orders/1/details/21/barcode").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void barcode_valid_returnsBarcode() throws Exception {
        PathOrderDetail d = detail(21L, 10L);
        d.setBarcode("2026080000001");
        d.setSampleNo("2026080000001");
        when(labOrderService.generateBarcode(10L, 21L)).thenReturn(d);

        mockMvc.perform(post("/api/v1/lab/orders/10/details/21/barcode").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.barcode").value("2026080000001"))
                .andExpect(jsonPath("$.sampleNo").value("2026080000001"));
    }

    @Test
    void acceptSample_valid_returnsAcceptedDetail() throws Exception {
        PathOrderDetail d = detail(21L, 10L);
        d.setSampleNo("2026080000001");
        d.setIsAccepted(true);
        when(labOrderService.acceptSample(eq(10L), eq(21L), any())).thenReturn(d);

        mockMvc.perform(post("/api/v1/lab/orders/10/details/21/accept").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"acceptanceReason\":\"OK\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAccepted").value(true));
    }

    @Test
    void rejectSample_missingReason_mapsTo400() throws Exception {
        when(labOrderService.rejectSample(eq(10L), eq(21L), any()))
                .thenThrow(new InvalidLabRequestException("LAB_REQ_INVALID", "rejectionReason is required"));

        mockMvc.perform(post("/api/v1/lab/orders/10/details/21/reject").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("LAB_REQ_INVALID"));
    }

    @Test
    void verify_valid_returnsVerifiedDetail() throws Exception {
        PathOrderDetail d = detail(21L, 10L);
        d.setIsVerified(true);
        when(labOrderService.verifyDetail(10L, 21L)).thenReturn(d);

        mockMvc.perform(post("/api/v1/lab/orders/10/details/21/verify").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isVerified").value(true));
    }

    @Test
    void notifyCritical_valid_returnsDetail() throws Exception {
        PathOrderDetail d = detail(21L, 10L);
        when(labOrderService.notifyCritical(10L, 21L)).thenReturn(d);

        mockMvc.perform(post("/api/v1/lab/orders/10/details/21/notify-critical").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detailId").value(21));
    }

    @Test
    void workload_returnsCounts() throws Exception {
        when(labOrderService.workload()).thenReturn(java.util.Map.of("ORDERED", 5L, "TOTAL", 5L));

        mockMvc.perform(get("/api/v1/lab/orders/workload").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.TOTAL").value(5));
    }

    @Test
    void covidList_returnsBookings() throws Exception {
        when(labOrderService.covidList()).thenReturn(List.of(booking(10L, 505L, 1L)));

        mockMvc.perform(get("/api/v1/lab/orders/covid").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookingId").value(10));
    }

    @Test
    void machine_crud_roundTrip() throws Exception {
        MachineConfiguration m = new MachineConfiguration();
        m.setMachineId(3L);
        m.setName("Sysmex XN-1000");
        m.setProtocol("HL7");
        m.setActive(true);
        when(machineService.create(eq("Sysmex XN-1000"), eq("HL7"), eq(true))).thenReturn(m);

        mockMvc.perform(post("/api/v1/machines").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sysmex XN-1000\",\"protocol\":\"HL7\",\"active\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.machineId").value(3));

        when(machineService.list(true)).thenReturn(List.of(m));
        mockMvc.perform(get("/api/v1/machines").with(csrf()).with(user("devops").roles("DEV")).param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].protocol").value("HL7"));
    }
}
