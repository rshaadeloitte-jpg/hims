package com.hims.search.api;

import com.hims.search.application.SearchIngestService;
import com.hims.search.application.exceptions.InvalidSearchRequestException;
import com.hims.search.application.exceptions.SearchNotFoundException;
import com.hims.search.configuration.SecurityConfig;
import com.hims.search.domain.model.IndexRequest;
import com.hims.search.infrastructure.indexer.SearchIndexer.SearchResultPage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests for the search-service API (default-deny enforced). Services
 * mocked; L0 covers the rules. Endpoints under test:
 *   POST /api/v1/internal/events · GET /api/v1/search · GET /api/v1/search/collections
 *   GET /api/v1/index-requests · PUT /api/v1/index-requests/{id}/retry
 */
@WebMvcTest(controllers = {InternalEventController.class, SearchController.class,
        IndexRequestController.class})
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class SearchApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchIngestService service;

    private static IndexRequest row(long id, short status) {
        IndexRequest r = new IndexRequest();
        r.setId(id);
        r.setCollection("patient");
        r.setAction(IndexRequest.ACTION_INDEX);
        r.setDocId("P-" + id);
        r.setPayload("{\"docId\":\"P-" + id + "\"}");
        r.setStatus(status);
        return r;
    }

    // ------------------------------------------------------------------ default deny

    @Test
    void endpoints_requireAuth_defaultDeny() throws Exception {
        mockMvc.perform(get("/api/v1/search")
                        .param("collection", "patient").param("q", "x"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/search/collections")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/internal/events").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/index-requests")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/index-requests/1/retry").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ internal events

    @Test
    void ingest_newRow_returns201WithJournalRow() throws Exception {
        when(service.ingest(eq("PatientRegistered"), eq(Map.of("docId", "P-1"))))
                .thenReturn(new SearchIngestService.IngestResult(row(10L, IndexRequest.STATUS_INDEXED), true));

        mockMvc.perform(post("/api/v1/internal/events").with(csrf())
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventType\":\"PatientRegistered\",\"payload\":{\"docId\":\"P-1\"}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.collection").value("patient"))
                .andExpect(jsonPath("$.status").value((int) IndexRequest.STATUS_INDEXED));
    }

    @Test
    void ingest_duplicate_returns200ExistingRow() throws Exception {
        when(service.ingest(eq("PatientRegistered"), eq(Map.of("docId", "DUP"))))
                .thenReturn(new SearchIngestService.IngestResult(row(7L, IndexRequest.STATUS_INDEXED), false));

        mockMvc.perform(post("/api/v1/internal/events").with(csrf())
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventType\":\"PatientRegistered\",\"payload\":{\"docId\":\"DUP\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void ingest_unknownEventType_mapsTo400Envelope() throws Exception {
        when(service.ingest(eq("InvoiceGenerated"), any()))
                .thenThrow(new InvalidSearchRequestException("SEARCH_UNKNOWN_EVENT_TYPE",
                        "Unknown eventType: InvoiceGenerated"));

        mockMvc.perform(post("/api/v1/internal/events").with(csrf())
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventType\":\"InvoiceGenerated\",\"payload\":{\"docId\":\"X\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("SEARCH_UNKNOWN_EVENT_TYPE"));
    }

    // ------------------------------------------------------------------ search

    @Test
    void search_returnsResultsEnvelope() throws Exception {
        when(service.search("patient", "aditya", 10, 0)).thenReturn(new SearchResultPage(
                2L, List.of(Map.of("id", "P-1"), Map.of("id", "P-2"))));

        mockMvc.perform(get("/api/v1/search").with(user("devops").roles("DEV"))
                        .param("collection", "patient").param("q", "aditya"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numFound").value(2))
                .andExpect(jsonPath("$.docs.length()").value(2))
                .andExpect(jsonPath("$.docs[0].id").value("P-1"));
    }

    @Test
    void search_unknownCollection_mapsTo400Envelope() throws Exception {
        when(service.search(eq("service"), anyString(), anyInt(), anyInt()))
                .thenThrow(new InvalidSearchRequestException("SEARCH_UNKNOWN_COLLECTION",
                        "Unknown collection: service"));

        mockMvc.perform(get("/api/v1/search").with(user("devops").roles("DEV"))
                        .param("collection", "service").param("q", "x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SEARCH_UNKNOWN_COLLECTION"));
    }

    @Test
    void search_missingQ_mapsTo400Bexa003() throws Exception {
        mockMvc.perform(get("/api/v1/search").with(user("devops").roles("DEV"))
                        .param("collection", "patient"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"));
    }

    @Test
    void collections_listsTheFiveLiveCollections() throws Exception {
        when(service.liveCollections()).thenReturn(
                List.of("patient", "encounter", "item", "icd10cm", "healthfactor"));

        mockMvc.perform(get("/api/v1/search/collections").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0]").value("patient"));
    }

    // ------------------------------------------------------------------ index requests

    @Test
    void list_returnsJournalRows() throws Exception {
        when(service.list("patient", (short) 0)).thenReturn(List.of(row(3L, IndexRequest.STATUS_PENDING)));

        mockMvc.perform(get("/api/v1/index-requests").with(user("devops").roles("DEV"))
                        .param("collection", "patient").param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].status").value((int) IndexRequest.STATUS_PENDING));
    }

    @Test
    void retry_returnsRow_andUnknownIdMapsTo404() throws Exception {
        when(service.retry(6L)).thenReturn(row(6L, IndexRequest.STATUS_INDEXED));

        mockMvc.perform(put("/api/v1/index-requests/6/retry").with(csrf())
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(6))
                .andExpect(jsonPath("$.status").value((int) IndexRequest.STATUS_INDEXED));

        when(service.retry(404L)).thenThrow(new SearchNotFoundException("NOT_FOUND",
                "Index request not found: 404"));
        mockMvc.perform(put("/api/v1/index-requests/404/retry").with(csrf())
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    void retry_illegalTransition_mapsTo400Envelope() throws Exception {
        when(service.retry(9L)).thenThrow(new InvalidSearchRequestException("SEARCH_ILLEGAL_TRANSITION",
                "Cannot retry index request 9 from status INDEXED"));

        mockMvc.perform(put("/api/v1/index-requests/9/retry").with(csrf())
                        .with(user("devops").roles("DEV")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SEARCH_ILLEGAL_TRANSITION"));
    }
}
