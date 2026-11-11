package com.hims.reference.api;

import com.hims.reference.configuration.JwtConfig;
import com.hims.reference.configuration.SecurityConfig;
import com.hims.search.api.SearchController;
import com.hims.search.application.SearchIngestService;
import com.hims.search.application.exceptions.InvalidSearchRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves that in reference-service bundle, absorbed module search's domain exception
 * is handled by com.hims.search.api.GlobalExceptionHandler (400) and NOT swallowed
 * into 500 by com.hims.reference.api.GlobalExceptionHandler or file handler.
 */
@WebMvcTest(SearchController.class)
@Import({
        SearchController.class,
        SecurityConfig.class,
        JwtConfig.class,
        com.hims.reference.api.GlobalExceptionHandler.class,
        com.hims.search.api.GlobalExceptionHandler.class,
        com.hims.file.api.GlobalExceptionHandler.class
})
@AutoConfigureMockMvc
class SearchAdviceScopeWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchIngestService service;

    @Test
    void search_unknownCollection_returns400_not500() throws Exception {
        when(service.search(anyString(), anyString(), anyInt(), anyInt()))
                .thenThrow(new InvalidSearchRequestException("SEARCH_UNKNOWN_COLLECTION", "Unknown collection: service"));

        mockMvc.perform(get("/api/v1/search")
                        .with(user("devops").roles("DEV"))
                        .param("collection", "service")
                        .param("q", "x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("SEARCH_UNKNOWN_COLLECTION"))
                .andExpect(jsonPath("$.message").value("Unknown collection: service"));
    }
}
