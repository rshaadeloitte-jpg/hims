package com.hims.interop.application;

import com.hims.interop.application.exceptions.InvalidRequestException;
import com.hims.interop.domain.model.InboundHis;
import com.hims.interop.domain.repository.InboundHisRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

/**
 * L0 — inbound HL7 ingress guards, no Spring context. Register WP-T1 assertions
 * pinned here:
 *   - messageType validated against the allowed ADT/ORM set else 400
 *     INTEROP_INVALID_MESSAGE_TYPE (register examples ADT^A01 / ADT^A45 / ORM)
 *   - rawPayload required; sender <= 40 chars
 *   - rows land 0=RECEIVED with processed_at NULL (dispatcher is comment-only).
 */
class InboundServiceTest {

    private InboundHisRepository repository;
    private InboundService service;

    @BeforeEach
    void setUp() {
        repository = mock(InboundHisRepository.class);
        service = new InboundService(repository);
        EntityManager em = mock(EntityManager.class);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "em", em);
        Query seqQuery = mock(Query.class);
        Mockito.when(seqQuery.getSingleResult()).thenReturn(42L);
        Mockito.when(em.createNativeQuery(org.mockito.ArgumentMatchers.contains("nextval"))).thenReturn(seqQuery);
        Mockito.when(repository.save(any())).thenAnswer(inv -> {
            InboundHis r = inv.getArgument(0);
            r.setId(42L);
            return r;
        });
    }

    @Test
    void allowedAdtAndOrmTypes_areAccepted_landingReceived() {
        for (String type : new String[]{"ADT^A01", "ADT^A02", "ADT^A03",
                "ADT^A04", "ADT^A08", "ADT^A45", "ORM^O01", "ORU^R01"}) {
            InboundHis row = service.receive(type, "HIS-LEGACY", "MSH|^~\\&|...");
            assertThat(row.getStatus()).as(type).isEqualTo(InboundHis.STATUS_RECEIVED);
            assertThat(row.getProcessedAt()).as(type).isNull();   // dispatcher OUT OF SCOPE
        }
    }

    @Test
    void unsupportedType_mapsTo400InvalidMessageType() {
        for (String bad : new String[]{"ADT^A99", "BLG^B01", null, "adt^a01"}) {  // case-sensitive HL7 type
            assertThatThrownBy(() -> service.receive(bad, "S", "MSH"))
                    .as(bad)
                    .isInstanceOf(InvalidRequestException.class)
                    .extracting(e -> ((InvalidRequestException) e).getErrorCode())
                    .isEqualTo("INTEROP_INVALID_MESSAGE_TYPE");
        }
    }

    @Test
    void missingPayloadOrOversizedSender_mapTo400Bexa003() {
        assertThatThrownBy(() -> service.receive("ADT^A01", "S", null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("rawPayload is required");
        assertThatThrownBy(() -> service.receive("ADT^A01", "S", "   "))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.receive("ADT^A01", "X".repeat(41), "MSH"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("at most 40 characters");
    }

    @Test
    void receive_persistsRawMessageWithSenderAndNullProcessedAt() {
        InboundHis row = service.receive("ORM^O01", "LIS-1", "MSH|^~\\&|GHH LAB|ELABs");
        ArgumentCaptor<InboundHis> cap = ArgumentCaptor.forClass(InboundHis.class);
        Mockito.verify(repository).save(cap.capture());
        assertThat(cap.getValue().getMessageType()).isEqualTo("ORM^O01");
        assertThat(cap.getValue().getSender()).isEqualTo("LIS-1");
        assertThat(cap.getValue().getRawPayload()).startsWith("MSH|");
        assertThat(row.getId()).isNotNull();
    }
}
