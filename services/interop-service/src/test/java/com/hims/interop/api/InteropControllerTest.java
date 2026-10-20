package com.hims.interop.api;

import com.hims.interop.application.AbdmService;
import com.hims.interop.application.EhrJobQueryService;
import com.hims.interop.application.InboundService;
import com.hims.interop.application.InteropEventService;
import com.hims.interop.application.InteropKafkaConsumer;
import com.hims.interop.application.SmsService;
import com.hims.interop.application.WhatsappService;
import com.hims.interop.application.exceptions.ConflictException;
import com.hims.interop.application.exceptions.InteropNotFoundException;
import com.hims.interop.application.exceptions.InvalidRequestException;
import com.hims.interop.configuration.SecurityConfig;
import com.hims.interop.domain.model.AbdmRequestQueue;
import com.hims.interop.domain.model.EhrJobDetails;
import com.hims.interop.domain.model.InboundHis;
import com.hims.interop.domain.model.OutboundHis;
import com.hims.interop.domain.model.WhatsappMessageQueue;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests for ALL interop APIs (default-deny enforced). Services mocked;
 * L0 covers the rules. Register WP-T1: MockMvc default-deny + error envelopes.
 */
@WebMvcTest({InternalEventController.class, OutboundController.class, InboundController.class,
        AbdmController.class, WhatsappController.class, HealthJobController.class, SmsController.class})
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class InteropControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private InteropEventService eventService;
    @MockitoBean private InboundService inboundService;
    @MockitoBean private AbdmService abdmService;
    @MockitoBean private WhatsappService whatsappService;
    @MockitoBean private EhrJobQueryService ehrJobService;
    @MockitoBean private SmsService smsService;
    @MockitoBean private InteropKafkaConsumer kafkaConsumer;

    private static OutboundHis outbound(long id, String eventType, String target) {
        OutboundHis o = new OutboundHis();
        o.setId(id);
        o.setEventType(eventType);
        o.setAggregateId("AGG-1");
        o.setTargetSystem(target);
        o.setPayload("{}");
        return o;
    }

    // ------------------------------------------------------------- default deny

    @Test
    void allEndpoints_requireAuth_defaultDeny() throws Exception {
        mockMvc.perform(get("/api/v1/internal/events")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/internal/events").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/outbound")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/inbound")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/inbound/hl7").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/abdm/requests").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/abdm/requests/C-1/callback").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/whatsapp/messages").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/whatsapp/messages/1/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/whatsapp/messages")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/whatsapp/messages/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/health/jobs")).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------- internal events

    @Test
    void ingest_newEvent_returns201WithRow() throws Exception {
        when(eventService.ingest(eq("InvoiceGenerated"), eq("BILL-1"), any()))
                .thenReturn(new InteropEventService.IngestResult(outbound(7L, "InvoiceGenerated", "HL7"), true));

        mockMvc.perform(post("/api/v1/internal/events").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventType":"InvoiceGenerated","aggregateId":"BILL-1",
                                 "payload":{"billId":1,"total":500.0}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.targetSystem").value("HL7"))
                .andExpect(jsonPath("$.status").value(0));
    }

    @Test
    void ingest_duplicateEvent_returns200WithExistingRow() throws Exception {
        when(eventService.ingest(eq("InvoiceGenerated"), eq("BILL-1"), any()))
                .thenReturn(new InteropEventService.IngestResult(outbound(7L, "InvoiceGenerated", "HL7"), false));

        mockMvc.perform(post("/api/v1/internal/events").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventType\":\"InvoiceGenerated\",\"aggregateId\":\"BILL-1\",\"payload\":{}}"))
                .andExpect(status().isOk())   // duplicate ingress: existing row, 200
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void ingest_unknownEventType_mapsTo400Envelope() throws Exception {
        when(eventService.ingest(eq("Nope"), any(), any()))
                .thenThrow(new InvalidRequestException("INTEROP_UNKNOWN_EVENT_TYPE", "Unknown eventType: Nope"));

        mockMvc.perform(post("/api/v1/internal/events").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventType\":\"Nope\",\"aggregateId\":\"A\",\"payload\":{}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("INTEROP_UNKNOWN_EVENT_TYPE"));
    }

    // ------------------------------------------------------------- outbound

    @Test
    void outboundList_filtersPassThrough() throws Exception {
        when(eventService.list((short) 0, "HL7"))
                .thenReturn(List.of(outbound(3L, "InvoiceGenerated", "HL7")));

        mockMvc.perform(get("/api/v1/outbound").with(user("devops").roles("DEV"))
                        .param("status", "0").param("target", "HL7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3));
    }

    @Test
    void outboundList_badStatus_mapsTo400Bexa003() throws Exception {
        mockMvc.perform(get("/api/v1/outbound").with(user("devops").roles("DEV")).param("status", "9"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"));
    }

    // ------------------------------------------------------------- inbound

    @Test
    void inboundReceive_validType_returns201Received() throws Exception {
        InboundHis row = new InboundHis();
        row.setId(11L);
        row.setMessageType("ADT^A01");
        row.setSender("HIS-LEGACY");
        row.setRawPayload("MSH|^~\\&|...");
        when(inboundService.receive("ADT^A01", "HIS-LEGACY", "MSH|^~\\&|...")).thenReturn(row);

        mockMvc.perform(post("/api/v1/inbound/hl7").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"messageType\":\"ADT^A01\",\"sender\":\"HIS-LEGACY\",\"rawPayload\":\"MSH|^~\\\\&|...\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.status").value(0));
    }

    @Test
    void inboundReceive_badType_mapsTo400Envelope() throws Exception {
        when(inboundService.receive(eq("ADT^A99"), any(), any()))
                .thenThrow(new InvalidRequestException("INTEROP_INVALID_MESSAGE_TYPE",
                        "Unsupported HL7 messageType: ADT^A99"));

        mockMvc.perform(post("/api/v1/inbound/hl7").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"messageType\":\"ADT^A99\",\"rawPayload\":\"MSH\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INTEROP_INVALID_MESSAGE_TYPE"));
    }

    @Test
    void inboundList_returnsAll() throws Exception {
        InboundHis row = new InboundHis();
        row.setId(2L);
        row.setMessageType("ORM^O01");
        when(inboundService.list()).thenReturn(List.of(row));

        mockMvc.perform(get("/api/v1/inbound").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].messageType").value("ORM^O01"));
    }

    // ------------------------------------------------------------- abdm

    @Test
    void abdmCreate_returns201_andDuplicateMapsTo409Envelope() throws Exception {
        AbdmRequestQueue row = new AbdmRequestQueue();
        row.setId(5L);
        row.setRequestKind("HIP");
        row.setCorrelationId("CORR-9");
        when(abdmService.create("HIP", "CORR-9", Map.of("x", 1), null)).thenReturn(row);

        mockMvc.perform(post("/api/v1/abdm/requests").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestKind\":\"HIP\",\"correlationId\":\"CORR-9\",\"payload\":{\"x\":1}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correlationId").value("CORR-9"))
                .andExpect(jsonPath("$.status").value(0));

        when(abdmService.create(eq("HIU"), eq("CORR-9"), any(), any()))
                .thenThrow(new ConflictException("INTEROP_DUPLICATE_CORRELATION_ID",
                        "ABDM correlationId already exists: CORR-9"));
        mockMvc.perform(post("/api/v1/abdm/requests").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestKind\":\"HIU\",\"correlationId\":\"CORR-9\",\"payload\":{\"y\":2}}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorCode").value("INTEROP_DUPLICATE_CORRELATION_ID"));
    }

    @Test
    void abdmCallback_applies_andUnknownMapsTo404() throws Exception {
        AbdmRequestQueue row = new AbdmRequestQueue();
        row.setId(5L);
        row.setCorrelationId("CORR-9");
        when(abdmService.callback(eq("CORR-9"), any(), eq((short) 2))).thenReturn(row);
        row.setCallbackPayload("{\"resp\":\"ok\"}");
        row.setStatus(AbdmRequestQueue.STATUS_COMPLETED);

        mockMvc.perform(put("/api/v1/abdm/requests/CORR-9/callback").with(csrf())
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payload\":{\"resp\":\"ok\"},\"status\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2));

        when(abdmService.callback(eq("GHOST"), any(), any()))
                .thenThrow(new InteropNotFoundException("NOT_FOUND",
                        "ABDM request not found for correlationId: GHOST"));
        mockMvc.perform(put("/api/v1/abdm/requests/GHOST/callback").with(csrf())
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"payload\":{},\"status\":2}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    // ------------------------------------------------------------- whatsapp

    @Test
    void whatsappCreate_returns201Queued_andGuardsMapTo400() throws Exception {
        WhatsappMessageQueue msg = new WhatsappMessageQueue();
        msg.setId(9L);
        msg.setRecipient("+91981234567");
        msg.setTemplate("otp");
        when(whatsappService.create("otp", "+91981234567", Map.of("code", "1234"))).thenReturn(msg);

        mockMvc.perform(post("/api/v1/whatsapp/messages").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"template\":\"otp\",\"recipient\":\"+91981234567\",\"payloadVars\":{\"code\":\"1234\"}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(0));

        when(whatsappService.create(any(), any(), any()))
                .thenThrow(new InvalidRequestException("BEXA003", "recipient is required"));
        mockMvc.perform(post("/api/v1/whatsapp/messages").with(csrf()).with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"template\":\"otp\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"));
    }

    @Test
    void whatsappStatusUpdate_applies_andIllegalTransitionMapsTo400() throws Exception {
        WhatsappMessageQueue sent = new WhatsappMessageQueue();
        sent.setId(9L);
        sent.setRecipient("+91981234567");
        sent.setStatus(WhatsappMessageQueue.STATUS_SENT);
        sent.setProviderMessageId("PROV-1");
        when(whatsappService.applyStatus(9L, (short) 1, "PROV-1", null)).thenReturn(sent);

        mockMvc.perform(put("/api/v1/whatsapp/messages/9/status").with(csrf())
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statusCode\":1,\"providerMessageId\":\"PROV-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1))
                .andExpect(jsonPath("$.providerMessageId").value("PROV-1"));

        when(whatsappService.applyStatus(eq(9L), eq((short) 3), any(), isNull()))
                .thenThrow(new InvalidRequestException("INTEROP_ILLEGAL_TRANSITION",
                        "Illegal WhatsApp transition 1 -> 3"));
        mockMvc.perform(put("/api/v1/whatsapp/messages/9/status").with(csrf())
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statusCode\":3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INTEROP_ILLEGAL_TRANSITION"));

        when(whatsappService.applyStatus(eq(404L), any(), any(), any()))
                .thenThrow(new InteropNotFoundException("NOT_FOUND", "WhatsApp message not found: 404"));
        mockMvc.perform(put("/api/v1/whatsapp/messages/404/status").with(csrf())
                        .with(user("devops").roles("DEV"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statusCode\":1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    void whatsappList_filtersByStatus() throws Exception {
        WhatsappMessageQueue failed = new WhatsappMessageQueue();
        failed.setId(4L);
        failed.setRecipient("+9198");
        when(whatsappService.list((short) 4)).thenReturn(List.of(failed));

        mockMvc.perform(get("/api/v1/whatsapp/messages").with(user("devops").roles("DEV"))
                        .param("status", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(4));
    }

    @Test
    void whatsappGet_returnsMessageWithHistory() throws Exception {
        WhatsappMessageQueue msg = new WhatsappMessageQueue();
        msg.setId(6L);
        msg.setRecipient("+9198");
        when(whatsappService.get(6L)).thenReturn(msg);

        mockMvc.perform(get("/api/v1/whatsapp/messages/6").with(user("devops").roles("DEV")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(6));
    }

    // ------------------------------------------------------------- health jobs

    @Test
    void healthJobs_listsReadModel() throws Exception {
        EhrJobDetails job = new EhrJobDetails();
        job.setId(1L);
        job.setJobKind("LAB_EXPORT");
        job.setPatientId(12L);
        when(ehrJobService.list((short) 0)).thenReturn(List.of(job));

        mockMvc.perform(get("/api/v1/health/jobs").with(user("devops").roles("DEV")).param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jobKind").value("LAB_EXPORT"));
    }

    @Test
    void healthJobs_badStatus_mapsTo400Bexa003() throws Exception {
        mockMvc.perform(get("/api/v1/health/jobs").with(user("devops").roles("DEV")).param("status", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BEXA003"));
    }

    @Test
    void unknownRoute_mapsTo404Envelope() throws Exception {
        mockMvc.perform(get("/api/v1/definitely-not-here").with(user("devops").roles("DEV")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }
}
