package com.hims.reference.api;

import com.hims.reference.application.Icd11Service;
import com.hims.reference.application.exceptions.MasterNotFoundException;
import com.hims.reference.configuration.JwtConfig;
import com.hims.reference.configuration.SecurityConfig;
import com.hims.reference.domain.model.Icd11Mms;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(Icd11Controller.class)
@Import({SecurityConfig.class, JwtConfig.class})
@AutoConfigureMockMvc
class Icd11ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Icd11Service service;

    private Icd11Mms sampleEntry(Long id, String code, String title, String classKind) {
        Icd11Mms entry = new Icd11Mms();
        entry.setId(id);
        entry.setCode(code);
        entry.setTitle(title);
        entry.setClassKind(classKind);
        entry.setLinearizationUri("http://id.who.int/icd/release/11/mms/" + id);
        entry.setReleaseVersion("2025-01");
        return entry;
    }

    @Test
    void search_byCodePrefix_returnsMatchingResults() throws Exception {
        Icd11Mms c1 = sampleEntry(1L, "1A00", "Cholera", "category");
        Icd11Mms c2 = sampleEntry(2L, "1A01", "Intestinal infection due to other Vibrio", "category");
        when(service.search(eq("1A"), any())).thenReturn(List.of(c1, c2));

        mockMvc.perform(get("/api/v1/icd11/search")
                        .param("q", "1A")
                        .with(user("test-user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].code").value("1A00"))
                .andExpect(jsonPath("$[0].title").value("Cholera"))
                .andExpect(jsonPath("$[0].attribution").value("ICD-11 MMS, World Health Organization, licensed CC BY-ND 3.0 IGO"))
                .andExpect(jsonPath("$[0].release_version").value("2025-01"));
    }

    @Test
    void search_byTitle_returnsMatchingResults() throws Exception {
        Icd11Mms c1 = sampleEntry(1L, "1A00", "Cholera", "category");
        when(service.search(eq("cholera"), any())).thenReturn(List.of(c1));

        mockMvc.perform(get("/api/v1/icd11/search")
                        .param("q", "cholera")
                        .with(user("test-user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("1A00"))
                .andExpect(jsonPath("$[0].title").value("Cholera"))
                .andExpect(jsonPath("$[0].attribution").value("ICD-11 MMS, World Health Organization, licensed CC BY-ND 3.0 IGO"));
    }

    @Test
    void search_limitCap_passesLimitToService() throws Exception {
        when(service.search("test", 100)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/icd11/search")
                        .param("q", "test")
                        .param("limit", "100")
                        .with(user("test-user")))
                .andExpect(status().isOk());

        verify(service).search(eq("test"), eq(100));
    }

    @Test
    void getByCode_found_returnsAttributionAndReleaseVersion() throws Exception {
        Icd11Mms entry = sampleEntry(1L, "1A00", "Cholera", "category");
        when(service.getByCode("1A00")).thenReturn(entry);

        mockMvc.perform(get("/api/v1/icd11/1A00")
                        .with(user("test-user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1A00"))
                .andExpect(jsonPath("$.title").value("Cholera"))
                .andExpect(jsonPath("$.attribution").value("ICD-11 MMS, World Health Organization, licensed CC BY-ND 3.0 IGO"))
                .andExpect(jsonPath("$.release_version").value("2025-01"));
    }

    @Test
    void getByCode_unknown_returns404() throws Exception {
        when(service.getByCode("INVALID-CODE"))
                .thenThrow(new MasterNotFoundException("ICD11_NOT_FOUND", "ICD-11 code not found: INVALID-CODE"));

        mockMvc.perform(get("/api/v1/icd11/INVALID-CODE")
                        .with(user("test-user")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ICD11_NOT_FOUND"));
    }

    @Test
    void unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/icd11/1A00"))
                .andExpect(status().isUnauthorized());
    }
}
