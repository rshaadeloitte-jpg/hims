package com.hims.interop.application;

import com.hims.interop.application.exceptions.ConflictException;
import com.hims.interop.application.exceptions.InvalidRequestException;
import com.hims.interop.domain.model.AbdmRequestQueue;
import com.hims.interop.domain.model.InboundHis;
import com.hims.interop.domain.repository.AbdmRequestQueueRepository;
import com.hims.interop.domain.repository.InboundHisRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * L0 — Kafka consumer stub unit tests (MIG-055 in-flight continuity).
 * Pinned to the zero-message-loss contract: the queue table is the
 * durability boundary, and the UNIQUE(envelope_event_id) index is the
 * race backstop.
 */
class InteropKafkaConsumerTest {

    private InboundHisRepository inboundRepository;
    private AbdmRequestQueueRepository abdmRepository;
    private AbdmService abdmService;
    private InteropKafkaConsumer consumer;
    private EntityManager em;

    @BeforeEach
    void setUp() {
        inboundRepository = mock(InboundHisRepository.class);
        abdmRepository = mock(AbdmRequestQueueRepository.class);
        abdmService = mock(AbdmService.class);
        consumer = new InteropKafkaConsumer(inboundRepository, abdmRepository, abdmService);
        em = mock(EntityManager.class);
        org.springframework.test.util.ReflectionTestUtils.setField(consumer, "em", em);
        Query seqQuery = mock(Query.class);
        when(seqQuery.getSingleResult()).thenReturn(1L);
        when(em.createNativeQuery(contains("nextval"))).thenReturn(seqQuery);
    }

    // ------------------------------------------------------------------ ABDM

    @Test
    void consumeAbdmRequest_dedupHit_returnsExistingRow_doesNotCallService() {
        AbdmRequestQueue existing = new AbdmRequestQueue();
        existing.setId(99L);
        existing.setCorrelationId("CORR-EXISTING");
        existing.setMilestone(AbdmRequestQueue.MILESTONE_M2);
        when(abdmRepository.findByEnvelopeEventId("EVT-1")).thenReturn(Optional.of(existing));

        AbdmRequestQueue out = consumer.consumeAbdmRequest("EVT-1", "HIP", "CORR-1", Map.of("k", 1));
        assertThat(out.getId()).isEqualTo(99L);
        assertThat(out.getMilestone()).isEqualTo("M2");
    }

    @Test
    void consumeAbdmRequest_miss_delegatesToAbdmService() {
        when(abdmRepository.findByEnvelopeEventId("EVT-NEW")).thenReturn(Optional.empty());
        AbdmRequestQueue created = new AbdmRequestQueue();
        created.setId(7L);
        created.setCorrelationId("CORR-NEW");
        when(abdmService.create("HIP", "CORR-NEW", Map.of("k", 1), "EVT-NEW"))
                .thenReturn(created);

        AbdmRequestQueue out = consumer.consumeAbdmRequest("EVT-NEW", "HIP", "CORR-NEW", Map.of("k", 1));
        assertThat(out.getId()).isEqualTo(7L);
    }

    @Test
    void consumeAbdmRequest_blankEnvelopeEventId_400Bexa003() {
        assertThatThrownBy(() -> consumer.consumeAbdmRequest("", "HIP", "CORR-1", Map.of("k", 1)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("envelopeEventId is required");
        assertThatThrownBy(() -> consumer.consumeAbdmRequest(null, "HIP", "CORR-1", Map.of("k", 1)))
                .isInstanceOf(InvalidRequestException.class);
    }

    // ------------------------------------------------------------------ HL7

    @Test
    void consumeInboundHl7_dedupHit_returnsExistingRow() {
        InboundHis existing = new InboundHis();
        existing.setId(7L);
        existing.setMessageType("ADT^A01");
        existing.setConsumerGroup(InteropKafkaConsumer.CONSUMER_GROUP_HL7);
        when(inboundRepository.findByEnvelopeEventId("EVT-1")).thenReturn(Optional.of(existing));

        InboundHis out = consumer.consumeInboundHl7("EVT-1", "ADT^A01", "HIS-1", "MSH|...");
        assertThat(out.getId()).isEqualTo(7L);
        assertThat(out.getConsumerGroup()).isEqualTo(InteropKafkaConsumer.CONSUMER_GROUP_HL7);
    }

    @Test
    void consumeInboundHl7_miss_persistsWithConsumerGroup() {
        when(inboundRepository.findByEnvelopeEventId("EVT-NEW")).thenReturn(Optional.empty());
        when(inboundRepository.save(any())).thenAnswer(inv -> {
            InboundHis h = inv.getArgument(0);
            h.setId(42L);
            return h;
        });

        InboundHis out = consumer.consumeInboundHl7("EVT-NEW", "ORM^O01", "LIS-1",
                "MSH|^~\\&|GHH|ELABs|RIH|src|20240815120000||ORM^O01|MSG-1|P|2.5\r" +
                "PID|1||100200300\r" +
                "ORC|NW|ORD-1\r" +
                "OBR|1|ORD-1||CBC");
        assertThat(out.getId()).isEqualTo(42L);
        assertThat(out.getMessageType()).isEqualTo("ORM^O01");
        assertThat(out.getConsumerGroup()).isEqualTo(InteropKafkaConsumer.CONSUMER_GROUP_HL7);
        assertThat(out.getEnvelopeEventId()).isEqualTo("EVT-NEW");
    }

    @Test
    void consumeInboundHl7_rejectsUnsupportedMessageType() {
        when(inboundRepository.findByEnvelopeEventId("EVT-BAD")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> consumer.consumeInboundHl7("EVT-BAD", "ADT^A99", "HIS-1", "MSH|..."))
                .isInstanceOf(InvalidRequestException.class)
                .extracting(e -> ((InvalidRequestException) e).getErrorCode())
                .isEqualTo("INTEROP_INVALID_MESSAGE_TYPE");
    }
}
