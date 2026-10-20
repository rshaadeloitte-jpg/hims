package com.hims.item.api;

import com.hims.item.application.IndentService;
import com.hims.item.configuration.SecurityConfig;
import com.hims.item.domain.model.Indent;
import com.hims.item.infrastructure.outbox.OutboxService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for indent create/approve/list (WP-2). */
@WebMvcTest(IndentController.class)
@Import({SecurityConfig.class, IndentService.class})
@AutoConfigureMockMvc
class IndentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private com.hims.item.domain.repository.IndentRepository indentRepository;

    @MockitoBean
    private com.hims.item.domain.repository.IndentDetailRepository indentDetailRepository;

    @MockitoBean
    private com.hims.item.domain.repository.ItemRepository itemRepository;

    @MockitoBean
    private com.hims.item.domain.repository.StoreRepository storeRepository;

    @MockitoBean
    private OutboxService outboxService;


    private static final String CREATE_BODY = """
            {"storeId":10,
             "lines":[{"itemId":20,"quantity":5},{"itemId":21,"quantity":2}]}
            """;

    @Test
    void endpoints_requireAuth_defaultDeny() throws Exception {
        mockMvc.perform(post("/api/v1/indents")
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/indents")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/indents/1/approve")).andExpect(status().isUnauthorized());
    }

    @Test
    void create_valid_returnsPendingIndent() throws Exception {
        when(indentRepository.nextIndentId()).thenReturn(601L);
        when(indentDetailRepository.nextIndentDetailId()).thenReturn(602L, 603L);
        when(storeRepository.existsById(10L)).thenReturn(true);
        when(itemRepository.existsById(anyLong())).thenReturn(true);
        when(indentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(indentDetailRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/v1/indents").with(user("devops"))
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indentNo").value("IND-601"))
                .andExpect(jsonPath("$.docStatus").value(1));
    }

    @Test
    void approve_pending_emitsIndentApproved_sameTx() throws Exception {
        Indent pending = new Indent();
        pending.setId(7L);
        pending.setIndentNo("IND-7");
        pending.setStoreId(10L);
        pending.setDocStatus(Indent.STATUS_PENDING);
        when(indentRepository.findById(7L)).thenReturn(Optional.of(pending));
        when(indentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/v1/indents/7/approve").with(user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.docStatus").value(2));

        ArgumentCaptor<String> eventType = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> aggregateId = ArgumentCaptor.forClass(Long.class);
        verify(outboxService).append(eventType.capture(), eq("Indent"), aggregateId.capture(),
                anyString(), anyString(), anyString());
        assertEquals("IndentApproved", eventType.getValue());
        assertEquals(7L, aggregateId.getValue());
    }

    @Test
    void approve_alreadyApproved_returns400() throws Exception {
        Indent approved = new Indent();
        approved.setId(8L);
        approved.setDocStatus(Indent.STATUS_APPROVED);
        when(indentRepository.findById(8L)).thenReturn(Optional.of(approved));
        mockMvc.perform(post("/api/v1/indents/8/approve").with(user("devops")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"));
    }

    @Test
    void list_filtersByStatus() throws Exception {
        Indent open = new Indent();
        open.setId(1L);
        open.setDocStatus(Indent.STATUS_PENDING);
        when(indentRepository.findByDocStatus(Indent.STATUS_PENDING)).thenReturn(List.of(open));
        when(indentRepository.findAllByOrderByIdDesc()).thenReturn(List.of(open));

        mockMvc.perform(get("/api/v1/indents").param("status", "1").with(user("devops")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].docStatus").value(1));
        verify(indentRepository).findByDocStatus((short) 1);
    }
}
