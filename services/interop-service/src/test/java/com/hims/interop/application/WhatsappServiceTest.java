package com.hims.interop.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.interop.application.exceptions.InteropNotFoundException;
import com.hims.interop.application.exceptions.InvalidRequestException;
import com.hims.interop.domain.model.WhatsappMessageQueue;
import com.hims.interop.domain.model.WhatsappMessageStatus;
import com.hims.interop.domain.repository.WhatsappMessageQueueRepository;
import com.hims.interop.domain.repository.WhatsappMessageStatusRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L0 — WhatsApp FORWARD-ONLY status machine, no Spring context. Register WP-T1
 * assertions pinned here:
 *   - forward path 0->1->2->3 (strictly one step at a time; skips are illegal)
 *   - FAILED (4) legal ONLY from 0/1; from 2/3 -> 400 INTEROP_ILLEGAL_TRANSITION
 *   - backward transitions and statusCode 0/null/out-of-range -> 400
 *   - every ACCEPTED transition appends exactly one history row
 *   - providerMessageId stamped when supplied
 *   - create: recipient required (<= 20 chars); new rows QUEUED
 */
class WhatsappServiceTest {

    private WhatsappMessageQueueRepository messageRepository;
    private WhatsappMessageStatusRepository statusRepository;
    private WhatsappService service;

    @BeforeEach
    void setUp() {
        messageRepository = mock(WhatsappMessageQueueRepository.class);
        statusRepository = mock(WhatsappMessageStatusRepository.class);
        service = new WhatsappService(messageRepository, statusRepository, new ObjectMapper());
        java.util.concurrent.atomic.AtomicLong seq = new java.util.concurrent.atomic.AtomicLong(500);
        EntityManager em = mock(EntityManager.class);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "em", em);
        Query seqQuery = mock(Query.class);
        when(seqQuery.getSingleResult()).thenAnswer(inv -> seq.incrementAndGet());
        when(em.createNativeQuery(contains("nextval"))).thenReturn(seqQuery);
        Map<Long, WhatsappMessageQueue> store = new HashMap<>();
        when(messageRepository.save(any())).thenAnswer(inv -> {
            WhatsappMessageQueue m = inv.getArgument(0);
            if (m.getId() == null) {
                m.setId((long) store.size() + 1);
            }
            store.put(m.getId(), m);
            return m;
        });
        when(messageRepository.findById(any(Long.class)))
                .thenAnswer(inv -> Optional.ofNullable(store.get(inv.getArgument(0, Long.class))));
        List<WhatsappMessageStatus> history = new java.util.ArrayList<>();
        when(statusRepository.save(any())).thenAnswer(inv -> {
            history.add(inv.getArgument(0));
            return inv.getArgument(0);
        });
    }

    private long queued(String recipient) {
        return service.create("otp_template", recipient, Map.of("code", "1234")).getId();
    }

    // ------------------------------------------------------------------ create

    @Test
    void create_happyPath_startsQueued() {
        WhatsappMessageQueue msg = service.create("appt_reminder", "+91981234567", Map.of("name", "A"));
        assertThat(msg.getStatus()).isEqualTo(WhatsappMessageQueue.STATUS_QUEUED);
        assertThat(msg.getRecipient()).isEqualTo("+91981234567");
        assertThat(msg.getPayloadVars()).contains("\"name\"");
    }

    @Test
    void create_recipientGuards_mapTo400Bexa003() {
        assertThatThrownBy(() -> service.create("t", null, null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("recipient is required");
        assertThatThrownBy(() -> service.create("t", "  ", null))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.create("t", "X".repeat(21), null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("at most 20 characters");
        assertThatThrownBy(() -> service.create("T".repeat(61), "R-1", null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("at most 60 characters");
    }

    // ------------------------------------------------------------------ machine

    @Test
    void forwardPath_isStrictlyOneStep_0_1_2_3() {
        long id = queued("R-FWD");

        assertThat(service.applyStatus(id, (short) 1, "PROV-9", null).getStatus())
                .isEqualTo(WhatsappMessageQueue.STATUS_SENT);
        assertThat(service.applyStatus(id, (short) 2, null, null).getStatus())
                .isEqualTo(WhatsappMessageQueue.STATUS_DELIVERED);
        assertThat(service.applyStatus(id, (short) 3, null, null).getStatus())
                .isEqualTo(WhatsappMessageQueue.STATUS_READ);

        ArgumentCaptor<WhatsappMessageStatus> hist = ArgumentCaptor.forClass(WhatsappMessageStatus.class);
        verify(statusRepository, times(3)).save(hist.capture());
        assertThat(hist.getAllValues())
                .extracting(WhatsappMessageStatus::getStatusCode)
                .containsExactly((short) 1, (short) 2, (short) 3);
    }

    @Test
    void skipAhead_isIllegal_400IllegalTransition() {
        long id = queued("R-SKIP");
        assertThatThrownBy(() -> service.applyStatus(id, (short) 2, null, null))   // 0 -> 2
                .isInstanceOf(InvalidRequestException.class)
                .extracting(e -> ((InvalidRequestException) e).getErrorCode())
                .isEqualTo("INTEROP_ILLEGAL_TRANSITION");
        assertThatThrownBy(() -> service.applyStatus(id, (short) 3, null, null))   // 0 -> 3
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("0->1->2->3");
    }

    @Test
    void backwardTransitions_areIllegal_400() {
        long id = queued("R-BACK");
        service.applyStatus(id, (short) 1, null, null);   // -> SENT
        assertThatThrownBy(() -> service.applyStatus(id, (short) 1, null, null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("1 -> 1");   // repeat same step is not a transition
        service.applyStatus(id, (short) 2, null, null);   // -> DELIVERED
        assertThatThrownBy(() -> service.applyStatus(id, (short) 1, null, null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("2 -> 1");   // backward
    }

    @Test
    void failed_onlyFromQueuedOrSent() {
        long a = queued("R-FAIL-Q");
        assertThat(service.applyStatus(a, (short) 4, null, "provider rejected").getStatus())
                .isEqualTo(WhatsappMessageQueue.STATUS_FAILED);

        long b = queued("R-FAIL-S");
        service.applyStatus(b, (short) 1, "PROV-1", null);
        assertThat(service.applyStatus(b, (short) 4, null, "timeout").getStatus())
                .isEqualTo(WhatsappMessageQueue.STATUS_FAILED);

        long c = queued("R-FAIL-D");
        service.applyStatus(c, (short) 1, null, null);
        service.applyStatus(c, (short) 2, null, null);
        assertThatThrownBy(() -> service.applyStatus(c, (short) 4, null, null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("2 -> 4");   // never after DELIVERED

        long d = queued("R-FAIL-R");
        service.applyStatus(d, (short) 1, null, null);
        service.applyStatus(d, (short) 2, null, null);
        service.applyStatus(d, (short) 3, null, null);
        assertThatThrownBy(() -> service.applyStatus(d, (short) 4, null, null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("3 -> 4");   // never after READ
    }

    @Test
    void terminalStates_acceptNoFurtherTransitions() {
        long id = queued("R-END");
        service.applyStatus(id, (short) 1, null, null);
        service.applyStatus(id, (short) 2, null, null);
        service.applyStatus(id, (short) 3, null, null);
        for (short target : new short[]{1, 2, 3, 4}) {
            short t = target;
            assertThatThrownBy(() -> service.applyStatus(id, t, null, null))
                    .as("READ -> " + t)
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
                .isEqualTo("BEXA003");   // 0 is the initial state, never a transition target
        assertThatThrownBy(() -> service.applyStatus(id, null, null, null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("statusCode must be one of");
        assertThatThrownBy(() -> service.applyStatus(id, (short) 7, null, null))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void unknownMessageId_mapsTo404() {
        assertThatThrownBy(() -> service.applyStatus(999L, (short) 1, null, null))
                .isInstanceOf(InteropNotFoundException.class)
                .extracting(e -> ((InteropNotFoundException) e).getErrorCode())
                .isEqualTo("NOT_FOUND");
    }

    @Test
    void providerMessageId_stampedWhenSupplied_andKeptWhenAbsent() {
        long id = queued("R-PROV");
        service.applyStatus(id, (short) 1, "PROV-ABC", null);
        assertThat(service.get(id).getProviderMessageId()).isEqualTo("PROV-ABC");
        service.applyStatus(id, (short) 2, null, null);
        assertThat(service.get(id).getProviderMessageId()).isEqualTo("PROV-ABC");
    }
}
