package com.hims.item.api;

import com.hims.item.application.ApprovalWorklistService;
import com.hims.item.application.ApprovalWorklistService.ApprovalWorklistItem;
import com.hims.item.configuration.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests for the M13 approval worklist endpoint.
 * Verifies the controller delegates to the service with the correct
 * (storeId, docType) parameters and the JSON shape matches the
 * {@link ApprovalWorklistItem} record.
 */
@WebMvcTest(ApprovalWorklistController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class ApprovalWorklistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApprovalWorklistService worklistService;

    // ApprovalWorklistService is mocked, so its repository dependencies are
    // not autowired in the test slice. They are only needed by the real
    // service bean.

    @Test
    void endpoint_requiresAuth_defaultDeny() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/approval-worklist"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void get_allTypes_returnsConsolidatedQueue_sortedByCreatedAtDesc() throws Exception {
        ApprovalWorklistItem indent = new ApprovalWorklistItem(
                "INDENT", 101L, "IND-101", 10L, (short) 1, "new batch",
                Timestamp.valueOf("2025-01-02 09:00:00"));
        ApprovalWorklistItem grn = new ApprovalWorklistItem(
                "GRN", 202L, "GRN-202", 10L, (short) 1, null,
                Timestamp.valueOf("2025-01-03 09:00:00"));
        when(worklistService.worklist(isNull(), isNull())).thenReturn(List.of(grn, indent));

        mockMvc.perform(get("/api/v1/inventory/approval-worklist").with(user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].docType").value("GRN"))
                .andExpect(jsonPath("$[0].docNo").value("GRN-202"))
                .andExpect(jsonPath("$[0].docStatus").value(1))
                .andExpect(jsonPath("$[1].docType").value("INDENT"))
                .andExpect(jsonPath("$[1].docNo").value("IND-101"));

        verify(worklistService).worklist(null, null);
    }

    @Test
    void get_filteredByStoreIdAndDocType_passesThemToService() throws Exception {
        when(worklistService.worklist(eq(10L), eq("INDENT"))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/inventory/approval-worklist")
                        .param("storeId", "10")
                        .param("docType", "INDENT")
                        .with(user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(worklistService).worklist(10L, "INDENT");
    }

    @Test
    void get_knownDocTypes_areAccepted() throws Exception {
        for (String dt : new String[]{"INDENT", "GRN", "CPO", "EXPIRY_RETURN", "STOCK_TRANSFER"}) {
            when(worklistService.worklist(isNull(), eq(dt))).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/inventory/approval-worklist")
                            .param("docType", dt).with(user("devops")))
                    .andExpect(status().isOk());
        }
    }
}
