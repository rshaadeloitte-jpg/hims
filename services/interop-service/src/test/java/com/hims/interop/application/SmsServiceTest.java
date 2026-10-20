package com.hims.interop.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.interop.application.exceptions.InteropNotFoundException;
import com.hims.interop.application.exceptions.InvalidRequestException;
import com.hims.interop.domain.model.SmsNotificationQueue;
import com.hims.interop.domain.model.SmsNotificationStatus;
import com.hims.interop.domain.repository.SmsNotificationQueueRepository;
import com.hims.interop.domain.repository.SmsNotificationStatusRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L0 — SMS forward-only status machine, no Spring context. MIG-055 eSanchar
 * gateway queue (parity with WhatsApp per 07 §3.1 #15):
 *   - forward path 0->1->2 (strictly one step)
 *   - DELIVERY_FAILED (3) legal ONLY from 0/1
 *   - EXPIRED (4) legal from 0/1/2 (terminal)
 *   - everything else (backward, skip-ahead, DELIVERY_FAILED from DELIVERED,
 *                     EXPIRED from DELIVERY_FAILED) -> 400 INTEROP_ILLEGAL_TRANSITION
 *   - recipient required (<= 20 chars)
 *   - gateway defaults to "eSanchar"
 */
class SmsServiceTest {

    private SmsNotificationQueueRepository messageRepository;
    private SmsNotificationStatusRepository statusRepository;
    private SmsService service;

    @BeforeEach
    void setUp() {
        messageRepository = mock(SmsNotificationQueueRepository.class);
        statusRepository = mock(SmsNotificationStatusRepository.class);
        service = new SmsService(messageRepository, statusRepository, new ObjectMapper());
        java.util.concurrent.atomic.AtomicLong seq = new java.util.concurrent.atomic.AtomicLong(700);
        EntityManager em = mock(EntityManager.class);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "em", em);
        Query seqQuery = mock(Query.class);
        when(seqQuery.getSingleResult()).thenAnswer(inv -> seq.incrementAndGet());
        when(em.createNativeQuery(contains("nextval"))).thenReturn(seqQuery);
        Map<Long, SmsNotificationQueue> store = new HashMap<>();
        when(messageRepository.save(any())).thenAnswer(inv -> {
            SmsNotificationQueue m = inv.getArgument(0);
            if (m.getId() == null) m.setId((long) store.size() + 1);
            store.put(m.getId(), m);
            return m;
        });
        when(messageRepository.findById(any(Long.class)))
                .thenAnswer(inv -> Optional.ofNullable(store.get(inv.getArgument(0, Long.class))));
        List<SmsNotificationStatus> history = new ArrayList<>();
        when(statusRepository.save(any())).thenAnswer(inv -> {
            history.add(inv.getArgument(0));
            return inv.getArgument(0);
        });
    }

    private long queued(String recipient) {
        return service.create("otp_template", recipient, null, Map.of("code", "1234")).getId();
    }

    // ------------------------------------------------------------------ create

    @Test
    void create_happyPath_startsQueuedWithEsancharDefault() {
        SmsNotificationQueue msg = service.create("appt", "+91981234567", null, Map.of("name", "A"));
        assertThat(msg.getStatus()).isEqualTo(SmsNotificationQueue.STATUS_QUEUED);
        assertThat(msg.getRecipient()).isEqualTo("+91981234567");
        assertThat(msg.getGateway()).isEqualTo("eSanchar");
        assertThat(msg.getPayloadVars()).contains("name");
    }

    @Test
    void create_recipientGuards_mapTo400Bexa003() {
        assertThatThrownBy(() -> service.create("t", null, null, null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("recipient is required");
        assertThatThrownBy(() -> service.create("t", "  ", null, null))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.create("t", "X".repeat(21), null, null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("at most 20 characters");
        assertThatThrownBy(() -> service.create("T".repeat(61), "R-1", null, null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("at most 60 characters");
    }

    // ------------------------------------------------------------------ machine

    @Test
    void forwardPath_isStrictlyOneStep_0_1_2() {
        long id = queued("R-FWD");
        assertThat(service.applyStatus(id, (short) 1, "PROV-9", null).getStatus())
                .isEqualTo(SmsNotificationQueue.STATUS_SENT);
        assertThat(service.applyStatus(id, (short) 2, null, null).getStatus())
                .isEqualTo(SmsNotificationQueue.STATUS_DELIVERED);
        ArgumentCaptor<SmsNotificationStatus> hist = ArgumentCaptor.forClass(SmsNotificationStatus.class);
        verify(statusRepository, times(2)).save(hist.capture());
        assertThat(hist.getAllValues())
                .extracting(SmsNotificationStatus::getStatusCode)
                .containsExactly((short) 1, (short) 2);
    }

    @Test
    void deliveryFailed_onlyFromQueuedOrSent() {
        long a = queued("R-FAIL-Q");
        assertThat(service.applyStatus(a, (short) 3, null, "provider rejected").getStatus())
                .isEqualTo(SmsNotificationQueue.STATUS_DELIVERY_FAILED);

        long b = queued("R-FAIL-S");
        service.applyStatus(b, (short) 1, "PROV-1", null);
        assertThat(service.applyStatus(b, (short) 3, null, "timeout").getStatus())
                .isEqualTo(SmsNotificationQueue.STATUS_DELIVERY_FAILED);

        long c = queued("R-FAIL-D");
        service.applyStatus(c, (short) 1, null, null);
        service.applyStatus(c, (short) 2, null, null);
        assertThatThrownBy(() -> service.applyStatus(c, (short) 3, null, null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("2 -> 3");   // never after DELIVERED
    }

    @Test
    void expired_legalFromQueuedSentDelivered_illegalFromDeliveryFailed() {
        long q = queued("R-EXP-Q");
        assertThat(service.applyStatus(q, (short) 4, null, "ttl").getStatus())
                .isEqualTo(SmsNotificationQueue.STATUS_EXPIRED);

        long s = queued("R-EXP-S");
        service.applyStatus(s, (short) 1, null, null);
        assertThat(service.applyStatus(s, (short) 4, null, null).getStatus())
                .isEqualTo(SmsNotificationQueue.STATUS_EXPIRED);

        long d = queued("R-EXP-D");
        service.applyStatus(d, (short) 1, null, null);
        service.applyStatus(d, (short) 2, null, null);
        assertThat(service.applyStatus(d, (short) 4, null, null).getStatus())
                .isEqualTo(SmsNotificationQueue.STATUS_EXPIRED);

        long f = queued("R-EXP-F");
        service.applyStatus(f, (short) 3, null, "fail");    // DELIVERY_FAILED
        assertThatThrownBy(() -> service.applyStatus(f, (short) 4, null, null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("3 -> 4");   // never after DELIVERY_FAILED
    }

    @Test
    void terminalStates_acceptNoFurtherTransitions() {
        long id = queued("R-END");
        service.applyStatus(id, (short) 1, null, null);
        service.applyStatus(id, (short) 2, null, null);
        // from DELIVERED (2) we can still EXPIRE (4) but not DELIVERY_FAILED (3) or SENT (1)
        for (short target : new short[]{1, 2, 3}) {
            short t = target;
            assertThatThrownBy(() -> service.applyStatus(id, t, null, null))
                    .as("DELIVERED -> " + t)
                    .isInstanceOf(InvalidRequestException.class)
                    .extracting(e -> ((InvalidRequestException) e).getErrorCode())
                    .isEqualTo("INTEROP_ILLEGAL_TRANSITION");
        }
    }

    @Test
    void invalidTargets_mapTo400Bexa003_notMachineViolation() {
        long id = queued("R-ZERO");
        assertThatThrownBy(() -> service.applyStatus(id, (short) 0, null, null))
                .isInstanceOf(InvalidRequestException.class)
                .extracting(e -> ((InvalidRequestException) e).getErrorCode())
                .isEqualTo("BEXA003");
        assertThatThrownBy(() -> service.applyStatus(id, null, null, null))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.applyStatus(id, (short) 7, null, null))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void unknownMessageId_mapsTo404() {
        assertThatThrownBy(() -> service.applyStatus(9999L, (short) 1, null, null))
                .isInstanceOf(InteropNotFoundException.class)
                .extracting(e -> ((InteropNotFoundException) e).getErrorCode())
                .isEqualTo("NOT_FOUND");
    }

    @Test
    void providerMessageId_stampedWhenSupplied_andKeptWhenAbsent() {
        long id = queued("R-PROV");
        service.applyStatus(id, (short) 1, "PROV-SMS-1", null);
        assertThat(service.get(id).getProviderMessageId()).isEqualTo("PROV-SMS-1");
        service.applyStatus(id, (short) 2, null, null);
        assertThat(service.get(id).getProviderMessageId()).isEqualTo("PROV-SMS-1");
    }
}
