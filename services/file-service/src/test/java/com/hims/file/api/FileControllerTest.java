package com.hims.file.api;

import com.hims.file.application.FileService;
import com.hims.file.configuration.SecurityConfig;
import com.hims.file.domain.model.FileDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FileController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class FileControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean FileService service;

    @Test
    void upload_requiresAuth() throws Exception {
        mockMvc.perform(post("/api/v1/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileName\":\"a.pdf\",\"fileData\":\"YWI=\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void upload_valid_returns201WithToken() throws Exception {
        FileDetails f = file(1L, "a.pdf", "tok123", "hello".getBytes());
        when(service.upload(any(), any(), any(), eq("a.pdf"), any(), any())).thenReturn(f);
        String b64 = Base64.getEncoder().encodeToString("hello".getBytes());
        mockMvc.perform(post("/api/v1/files").with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileName\":\"a.pdf\",\"fileData\":\"" + b64 + "\",\"ownerType\":\"PATIENT\",\"ownerId\":123}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileToken").value("tok123"))
                .andExpect(jsonPath("$.fileName").value("a.pdf"));
    }

    @Test
    void getById_returnsDetail() throws Exception {
        FileDetails f = file(1L, "a.pdf", "tok123", "hello".getBytes());
        when(service.getById(1L)).thenReturn(f);
        mockMvc.perform(get("/api/v1/files/1").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileToken").value("tok123"));
    }

    @Test
    void download_returnsBytes() throws Exception {
        byte[] bytes = "hello".getBytes();
        FileDetails f = file(1L, "a.pdf", "tok123", bytes);
        f.setFileType("text/plain");
        when(service.getByToken("tok123")).thenReturn(f);
        mockMvc.perform(get("/api/v1/files/token/tok123/download").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(bytes));
    }

    @Test
    void listByOwner_returnsList() throws Exception {
        FileDetails f = file(1L, "a.pdf", "tok123", "hi".getBytes());
        when(service.listByOwner(eq("PATIENT"), eq(123L))).thenReturn(List.of(f));
        mockMvc.perform(get("/api/v1/files").with(user("devops").roles("DEV"))
                        .param("ownerType", "PATIENT").param("ownerId", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileToken").value("tok123"));
    }

    @Test
    void annotate_returnsUpdated() throws Exception {
        FileDetails f = file(1L, "a.pdf", "tok123", "hi".getBytes());
        f.setAnnotationJson("{\"shapes\":[]}");
        when(service.annotate(eq(1L), any())).thenReturn(f);
        mockMvc.perform(put("/api/v1/files/1/annotation").with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"annotationJson\":\"{\\\"shapes\\\":[]}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.annotationJson").value("{\"shapes\":[]}"));
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/files/1").with(user("devops").roles("DEV")))
                .andExpect(status().isNoContent());
    }

    private static FileDetails file(long id, String name, String token, byte[] content) {
        FileDetails f = new FileDetails();
        f.setId(id); f.setFileName(name); f.setFileToken(token); f.setFileContent(content);
        f.setFileSize((long) content.length); f.setFileType("application/octet-stream");
        f.setStatus(true);
        return f;
    }
}
