package com.hims.rmrs.api;

import com.hims.rmrs.application.CollectionService;
import com.hims.rmrs.application.ServiceWiseSummaryRow;
import com.hims.rmrs.application.UserWiseSummaryRow;
import com.hims.rmrs.application.exceptions.DuplicateRmrsResourceException;
import com.hims.rmrs.application.exceptions.InvalidRmrsRequestException;
import com.hims.rmrs.application.exceptions.RmrsNotFoundException;
import com.hims.rmrs.configuration.SecurityConfig;
import com.hims.rmrs.domain.model.AccountingEntry;
import com.hims.rmrs.domain.model.RmrsCollection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for the collections API (default-deny + error envelopes). Services mocked; L0 covers the rules. */
@WebMvcTest(CollectionController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class CollectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CollectionService collectionService;

    private static RmrsCollection collection(long id, String receiptNo, String amount) {
        RmrsCollection c = new RmrsCollection();
        c.setCollectionId(id);
        c.setReceiptNo(receiptNo);
        c.setCollectionDate(LocalDate.of(2026, 2, 10));
        c.setPatientId(505L);
        c.setAmount(new BigDecimal(amount));
        c.setHeadId(1L);
        c.setLedgerId(11L);
        c.setCollectedBy("counter-staff-1");

        AccountingEntry dr = new AccountingEntry();
        dr.setEntryId(900L);
        dr.setCollectionId(id);
        dr.setLedgerId(11L);
        dr.setDebit(new BigDecimal(amount));
        dr.setCredit(BigDecimal.ZERO);
        dr.setEntryDate(LocalDate.of(2026, 2, 10));
        AccountingEntry cr = new AccountingEntry();
        cr.setEntryId(901L);
        cr.setCollectionId(id);
        cr.setLedgerId(11L);
        cr.setDebit(BigDecimal.ZERO);
        cr.setCredit(new BigDecimal(amount));
        cr.setEntryDate(LocalDate.of(2026, 2, 10));
        c.setEntries(List.of(dr, cr));
        return c;
    }

    @Test
    void endpoints_requireAuth_defaultDeny() throws Exception {
        mockMvc.perform(get("/api/v1/collections")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/collections/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/collections/summary/service-wise")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/collections/summary/user-wise")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/collections").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_valid_returns201WithBalancedEntries() throws Exception {
        RmrsCollection saved = collection(7L, "RCP-0001", "100.00");
        when(collectionService.create(any(CollectionService.CollectionInput.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/collections").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiptNo":"RCP-0001","collectionDate":"2026-02-10","patientId":505,
                                 "amount":100.00,"headCode":"PHARMACY","ledgerCode":"PH-LEDGER",
                                 "authorityCode":null,"paymentRef":"PAY-1","collectedBy":"counter-staff-1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.collectionId").value(7))
                .andExpect(jsonPath("$.receiptNo").value("RCP-0001"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.entries.length()").value(2))
                .andExpect(jsonPath("$.entries[0].debit").value(100.00))
                .andExpect(jsonPath("$.entries[0].credit").value(0))
                .andExpect(jsonPath("$.entries[1].debit").value(0))
                .andExpect(jsonPath("$.entries[1].credit").value(100.00));
    }

    @Test
    void create_headLedgerMismatch_mapsTo400Bexa003Envelope() throws Exception {
        when(collectionService.create(any(CollectionService.CollectionInput.class)))
                .thenThrow(new InvalidRmrsRequestException(
                        "Ledger OTHER does not belong to head PHARMACY — head-ledger mismatch"));

        mockMvc.perform(post("/api/v1/collections").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiptNo":"RCP-0002","collectionDate":"2026-02-10",
                                 "amount":50,"headCode":"PHARMACY","ledgerCode":"OTHER"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("BEXA003"));
    }

    @Test
    void create_duplicateReceipt_mapsTo409Envelope() throws Exception {
        when(collectionService.create(any(CollectionService.CollectionInput.class)))
                .thenThrow(new DuplicateRmrsResourceException("RMRS_DUPLICATE_RECEIPT",
                        "receiptNo already exists: RCP-0001"));

        mockMvc.perform(post("/api/v1/collections").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"receiptNo":"RCP-0001","collectionDate":"2026-02-10",
                                 "amount":50,"headCode":"PHARMACY","ledgerCode":"PH-LEDGER"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RMRS_DUPLICATE_RECEIPT"));
    }

    @Test
    void get_unknownCollection_mapsTo404Envelope() throws Exception {
        when(collectionService.get(404L))
                .thenThrow(new RmrsNotFoundException("Collection not found: 404"));

        mockMvc.perform(get("/api/v1/collections/404").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RMRS_NOT_FOUND"));
    }

    @Test
    void list_paginationAndFilters_accepted() throws Exception {
        when(collectionService.search(eq(LocalDate.of(2026, 2, 1)), eq(LocalDate.of(2026, 2, 28)),
                eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(collection(7L, "RCP-0001", "100.00")),
                        PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/collections").with(csrf()).with(user("devops").roles("DEV"))
                        .param("dateFrom", "2026-02-01")
                        .param("dateTo", "2026-02-28")
                        .param("headId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].receiptNo").value("RCP-0001"))
                .andExpect(jsonPath("$.totalElements").value(1));

        // no filters -> service called with nulls
        when(collectionService.search(isNull(), isNull(), isNull(), any()))
                .thenReturn(Page.empty());
        mockMvc.perform(get("/api/v1/collections").with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void summaries_returnRows() throws Exception {
        when(collectionService.summaryServiceWise(isNull(), isNull())).thenReturn(List.of(
                new ServiceWiseSummaryRow(1L, "PHARMACY", "Pharmacy cash head",
                        new BigDecimal("150.00"), 2L)));
        when(collectionService.summaryUserWise(isNull(), isNull())).thenReturn(List.of(
                new UserWiseSummaryRow("counter-staff-1", new BigDecimal("150.00"), 2L)));

        mockMvc.perform(get("/api/v1/collections/summary/service-wise")
                        .with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].headCode").value("PHARMACY"))
                .andExpect(jsonPath("$[0].totalAmount").value(150.00))
                .andExpect(jsonPath("$[0].collectionCount").value(2));

        mockMvc.perform(get("/api/v1/collections/summary/user-wise")
                        .with(csrf()).with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].collectedBy").value("counter-staff-1"))
                .andExpect(jsonPath("$[0].totalAmount").value(150.00))
                .andExpect(jsonPath("$[0].collectionCount").value(2));
    }
}
