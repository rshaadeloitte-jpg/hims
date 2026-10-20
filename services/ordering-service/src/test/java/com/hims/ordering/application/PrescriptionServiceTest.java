package com.hims.ordering.application;

import com.hims.ordering.application.exceptions.InvalidPrescriptionException;
import com.hims.ordering.application.exceptions.PrescriptionNotFoundException;
import com.hims.ordering.domain.model.Prescription;
import com.hims.ordering.domain.model.PrescriptionItem;
import com.hims.ordering.domain.repository.PrescriptionItemRepository;
import com.hims.ordering.domain.repository.PrescriptionRepository;
import com.hims.ordering.infrastructure.outbox.OutboxPublisher;
import com.hims.ordering.infrastructure.outbox.OutboxService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

/** L0 — prescription placement + BR-ORD-005 clinical safety gate (dose/frequency required). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PrescriptionServiceTest {

    @Mock private PrescriptionRepository prescriptionRepository;
    @Mock private PrescriptionItemRepository itemRepository;
    @Mock private OutboxService outboxService;
    @Mock private EntityManager em;
    @Mock private jakarta.persistence.Query seqQuery;

    private PrescriptionService service;

    private Prescription current;
    private final java.util.List<PrescriptionItem> currentItems = new java.util.ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new PrescriptionService(prescriptionRepository, itemRepository, outboxService);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "em", em);
        lenient().when(em.createNativeQuery(org.mockito.ArgumentMatchers.anyString())).thenReturn(seqQuery);
        lenient().when(seqQuery.getSingleResult()).thenReturn(200L);
        lenient().when(prescriptionRepository.save(any())).thenAnswer(inv -> {
            current = inv.getArgument(0);
            return current;
        });
        lenient().when(itemRepository.save(any())).thenAnswer(inv -> {
            currentItems.add(inv.getArgument(0));
            return inv.getArgument(0);
        });
        lenient().when(prescriptionRepository.findById(anyLong()))
                .thenAnswer(inv -> Optional.ofNullable(current));
        lenient().when(itemRepository.findByPrescriptionIdOrderByLineNoAsc(anyLong()))
                .thenAnswer(inv -> currentItems);
    }

    private static PrescriptionService.LineInput line(long itemId, String qty,
                                                      String dose, String freq) {
        return new PrescriptionService.LineInput(itemId, new BigDecimal(qty), dose, freq);
    }

    @Test
    void happyPathCreatesActiveHeaderItemsAndE06() {
        Prescription p = service.create(501L, 7001L, null, 11L, 2L,
                List.of(line(10, "2.5", "500mg", "TDS"), line(11, "1", "10ml", "OD")));

        assertThat(p.getStatusCode()).isEqualTo(Prescription.STATUS_ACTIVE);
        assertThat(p.getDetails()).hasSize(2);
        assertThat(p.getDetails().get(0).getDose()).isEqualTo("500mg");
        assertThat(p.getDetails().get(1).getFrequency()).isEqualTo("OD");

        ArgumentCaptor<String> payloadCap = ArgumentCaptor.forClass(String.class);
        verify(outboxService).append(eq(OutboxPublisher.OrderingEvents.PRESCRIPTION_CREATED),
                eq("prescription"), eq(p.getPrescriptionId()), eq("7001"),
                payloadCap.capture(), contains("prescription-created"));
        // exact E-06 payload keys (encounterId supplied, admissionId null slot travels explicitly)
        assertThat(payloadCap.getValue())
                .contains("\"prescriptionId\"").contains("\"patientId\":\"501\"")
                .contains("\"encounterId\":\"7001\"").contains("\"admissionId\":null")
                .contains("\"lines\"").contains("\"dose\":\"500mg\"")
                .contains("\"prescriberId\":\"11\"");
    }

    @Test
    void admissionFallbackPartitionKeyWhenNoEncounter() {
        service.create(501L, null, 8001L, 11L, null, List.of(line(10, "1", "5mg", "BD")));
        verify(outboxService).append(any(), any(), anyLong(), eq("8001"), any(), any());
    }

    @Test
    void noContextFallbackPartitionKey() {
        service.create(501L, null, null, 11L, null, List.of(line(10, "1", "5mg", "BD")));
        verify(outboxService).append(any(), any(), anyLong(),
                eq("no-context:" + current.getPrescriptionId()), any(), any());
    }

    @Test
    void patientRequired() {
        assertThatThrownBy(() -> service.create(null, 1L, null, 11L, null,
                List.of(line(1, "1", "d", "f"))))
                .isInstanceOfSatisfying(InvalidPrescriptionException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("BEXA003"));
    }

    @Test
    void prescriberRequired() {
        assertThatThrownBy(() -> service.create(501L, 1L, null, null, null,
                List.of(line(1, "1", "d", "f"))))
                .isInstanceOfSatisfying(InvalidPrescriptionException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("BEXA003"));
    }

    @Test
    void bothContextsRejected() {
        assertThatThrownBy(() -> service.create(501L, 1L, 2L, 11L, null,
                List.of(line(1, "1", "d", "f"))))
                .isInstanceOfSatisfying(InvalidPrescriptionException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("BEXA003"));
    }

    @Test
    void emptyLinesRejected() {
        assertThatThrownBy(() -> service.create(501L, 1L, null, 11L, null, List.of()))
                .isInstanceOfSatisfying(InvalidPrescriptionException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("BEXA003"));
    }

    @Test
    void zeroQtyRejected_BR_ORD_005() {
        assertThatThrownBy(() -> service.create(501L, 1L, null, 11L, null,
                List.of(line(1, "0", "500mg", "TDS"))))
                .isInstanceOfSatisfying(InvalidPrescriptionException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("BEXA003"));
    }

    @Test
    void missingDoseRejected_BR_ORD_005() {
        assertThatThrownBy(() -> service.create(501L, 1L, null, 11L, null,
                List.of(line(1, "2", null, "TDS"))))
                .isInstanceOfSatisfying(InvalidPrescriptionException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("BR-ORD-005"));
    }

    @Test
    void missingFrequencyRejected_BR_ORD_005() {
        assertThatThrownBy(() -> service.create(501L, 1L, null, 11L, null,
                List.of(line(1, "2", "500mg", ""))))
                .isInstanceOfSatisfying(InvalidPrescriptionException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("BR-ORD-005"));
    }

    @Test
    void cancelGuardRejectsNonActive() {
        current = new Prescription();
        current.setPrescriptionId(9L);
        current.setStatusCode(Prescription.STATUS_CANCELLED);
        when(prescriptionRepository.findById(9L)).thenReturn(Optional.of(current));
        assertThatThrownBy(() -> service.cancel(9L))
                .isInstanceOfSatisfying(InvalidPrescriptionException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo("ORD_ILLEGAL_TRANSITION"));
    }

    @Test
    void cancelFlipsActiveToCancelledWithoutEvent() {
        current = new Prescription();
        current.setPrescriptionId(8L);
        current.setPatientId(501L);
        current.setPrescriberId(11L);
        current.setStatusCode(Prescription.STATUS_ACTIVE);
        Prescription cancelled = service.cancel(8L);
        assertThat(cancelled.getStatusCode()).isEqualTo(Prescription.STATUS_CANCELLED);
        // E-06 is create-only: cancel must NOT append anything to the outbox
        verify(outboxService, never()).append(any(), any(), any(), any(), any(), any());
    }

    @Test
    void unknownPrescriptionNotFound() {
        when(prescriptionRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.cancel(404L))
                .isInstanceOf(PrescriptionNotFoundException.class);
    }
}
