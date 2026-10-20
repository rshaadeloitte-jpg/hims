package com.hims.linen.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.linen.application.LinenRequestService;
import com.hims.linen.configuration.SecurityConfig;
import com.hims.linen.domain.model.LinenRequest;
import com.hims.linen.domain.model.LinenRequestDetails;
import com.hims.linen.domain.model.LinenStatus;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for the linen lifecycle API (default-deny enforced). Service is mocked; L1 covers the state machine. */
@WebMvcTest(LinenRequestController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class LinenRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LinenRequestService service;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void raise_requiresAuth_defaultDeny() throws Exception {
        mockMvc.perform(post("/api/v1/linen-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"request\":{\"storeId\":1}}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void raise_validRequest_returns201() throws Exception {
        LinenRequest r = new LinenRequest();
        r.setId(1L);
        r.setRequisitionNo("IHMS/LR/26/00001");
        r.setRequestStatus(LinenStatus.PENDING);
        when(service.raiseRequest(any(), isNull(), isNull())).thenReturn(r);

        mockMvc.perform(post("/api/v1/linen-requests")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"request\":{\"storeId\":1},\"details\":[]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requisitionNo").value("IHMS/LR/26/00001"))
                .andExpect(jsonPath("$.requestStatus").value(1));
    }

    @Test
    void counts_returnsDashboardMap() throws Exception {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("TOTAL", 5L);
        counts.put("PENDING", 2L);
        counts.put("WASHED", 1L);
        when(service.getCounts(isNull(), isNull(), isNull(), isNull())).thenReturn(counts);

        mockMvc.perform(get("/api/v1/linen-requests/counts")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.TOTAL").value(5))
                .andExpect(jsonPath("$.PENDING").value(2))
                .andExpect(jsonPath("$.WASHED").value(1));
    }

    @Test
    void approve_delegatesToService() throws Exception {
        LinenRequest r = new LinenRequest();
        r.setId(1L);
        r.setRequestStatus(LinenStatus.ACCEPTED);
        when(service.approve(1L)).thenReturn(r);

        mockMvc.perform(post("/api/v1/linen-requests/1/approve")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestStatus").value(2));
    }

    @Test
    void handover_passesHandedOverTo() throws Exception {
        LinenRequest r = new LinenRequest();
        r.setId(1L);
        r.setRequestStatus(LinenStatus.HANDOVERED);
        r.setHandedOverTo("LaundryA");
        when(service.handover(eq(1L), eq("LaundryA"))).thenReturn(r);

        mockMvc.perform(post("/api/v1/linen-requests/1/handover")
                        .param("handedOverTo", "LaundryA")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.handedOverTo").value("LaundryA"));
    }

    @Test
    void details_returnsList() throws Exception {
        LinenRequestDetails d = new LinenRequestDetails();
        d.setId(5L);
        d.setQuantity(10L);
        d.setReceivedQuantity(8L);
        when(service.getDetails(1L)).thenReturn(List.of(d));

        mockMvc.perform(get("/api/v1/linen-requests/1/details")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantity").value(10))
                .andExpect(jsonPath("$[0].receivedQuantity").value(8));
    }

    @Test
    void complete_passesReceivedBy() throws Exception {
        LinenRequest r = new LinenRequest();
        r.setId(1L);
        r.setRequestStatus(LinenStatus.COMPLETED);
        r.setReceivedBy("WardNurse");
        when(service.complete(eq(1L), eq("WardNurse"), any())).thenReturn(r);

        mockMvc.perform(post("/api/v1/linen-requests/1/complete")
                        .param("receivedBy", "WardNurse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receivedBy").value("WardNurse"))
                .andExpect(jsonPath("$.requestStatus").value(7));
    }

    @Test
    void delete_softDelete() throws Exception {
        LinenRequest r = new LinenRequest();
        r.setId(1L);
        r.setStatus(false);
        r.setRequestStatus(0L);
        when(service.deleteRequest(1L)).thenReturn(r);

        mockMvc.perform(delete("/api/v1/linen-requests/1")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(false))
                .andExpect(jsonPath("$.requestStatus").value(0));
    }

    @Test
    void unmappedPath_returns404_not500() throws Exception {
        mockMvc.perform(get("/api/v1/linen-requests/nope/nope/nope/nope")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isNotFound());
    }

    // ---- gap-hardening additions (GAP-06): previously untested endpoints ----

    @Test
    void get_returnsRequestById() throws Exception {
        LinenRequest r = new LinenRequest();
        r.setId(1L);
        r.setRequisitionNo("IHMS/LR/26/00002");
        when(service.getRequest(1L)).thenReturn(r);

        mockMvc.perform(get("/api/v1/linen-requests/1")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requisitionNo").value("IHMS/LR/26/00002"));
    }

    @Test
    void list_passesFiltersToService() throws Exception {
        when(service.listRequests(isNull(), eq(2L), eq(1L), eq("IHMS"), isNull(), isNull()))
                .thenReturn(List.of(new LinenRequest()));

        mockMvc.perform(get("/api/v1/linen-requests")
                        .param("storeId", "2")
                        .param("requestStatus", "1")
                        .param("search", "IHMS")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void updateDetails_putReplacesLines() throws Exception {
        LinenRequestDetails d = new LinenRequestDetails();
        d.setId(5L);
        d.setQuantity(12L);
        when(service.updateDetails(eq(1L), any())).thenReturn(List.of(d));

        mockMvc.perform(put("/api/v1/linen-requests/1/details")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"id\":5,\"quantity\":12}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantity").value(12));
    }

    @Test
    void receiveMainStore_advancesToReceived() throws Exception {
        LinenRequest r = new LinenRequest();
        r.setId(1L);
        r.setRequestStatus(LinenStatus.RECEIVED);
        when(service.receiveMainStore(eq(1L), any())).thenReturn(r);

        mockMvc.perform(post("/api/v1/linen-requests/1/receive-main-store")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"id\":5,\"receivedQuantity\":8}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestStatus").value(4));
    }

    @Test
    void process_advancesToWashed() throws Exception {
        LinenRequest r = new LinenRequest();
        r.setId(1L);
        r.setRequestStatus(LinenStatus.WASHED);
        when(service.process(1L)).thenReturn(r);

        mockMvc.perform(post("/api/v1/linen-requests/1/process")
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestStatus").value(5));
    }

    @Test
    void dispatch_advancesToDispatched() throws Exception {
        LinenRequest r = new LinenRequest();
        r.setId(1L);
        r.setRequestStatus(LinenStatus.DISPATCHED);
        when(service.dispatch(eq(1L), any())).thenReturn(r);

        mockMvc.perform(post("/api/v1/linen-requests/1/dispatch")
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"id\":5,\"dispatchedQuantity\":8}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestStatus").value(6));
    }
}
