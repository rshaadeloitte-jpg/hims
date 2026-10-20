package com.hims.ordering.application;

import com.hims.ordering.application.exceptions.InvalidOrderException;
import com.hims.ordering.domain.model.ClinicalOrder;
import com.hims.ordering.domain.model.OrderItem;
import com.hims.ordering.domain.model.SagaState;
import com.hims.ordering.domain.repository.ClinicalOrderRepository;
import com.hims.ordering.domain.repository.OrderItemRepository;
import com.hims.ordering.domain.repository.SagaStateRepository;
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
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L0 — saga_state transitions of the MIG-033 orchestrator: step applicability,
 * confirm idempotency, failure compensation (SUCCEEDED -> COMPENSATED + OrderFailed
 * in the SAME tx), cancel compensation and terminal-saga guards.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderSagaStateMachineTest {

    @Mock private SagaStateRepository sagaRepository;
    @Mock private ClinicalOrderRepository orderRepository;
    @Mock private OrderItemRepository itemRepository;
    @Mock private OutboxService outboxService;
    @Mock private EntityManager em;
    @Mock private jakarta.persistence.Query seqQuery;

    private SagaService service;

    private ClinicalOrder order;
    private SagaState saga;

    @BeforeEach
    void setUp() {
        service = new SagaService(sagaRepository, orderRepository, itemRepository, outboxService);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "em", em);
        lenient().when(em.createNativeQuery(org.mockito.ArgumentMatchers.anyString())).thenReturn(seqQuery);
        lenient().when(seqQuery.getSingleResult()).thenReturn(500L);
        order = new ClinicalOrder();
        order.setOrderId(42L);
        order.setPatientId(501L);
        order.setEncounterId(7001L);
        order.setPrescriberId(11L);
        order.setStatusCode(ClinicalOrder.STATUS_INITIATED);
        order.setNetTotal(new BigDecimal("600.00"));
        lenient().when(orderRepository.findById(42L)).thenReturn(Optional.of(order));
        lenient().when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(sagaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private OrderItem item(String type) {
        OrderItem i = new OrderItem();
        i.setOrderId(42L);
        i.setItemType(type);
        i.setItemId(1L);
        i.setRate(new BigDecimal("300"));
        i.setQty(2);
        return i;
    }

    // ------------------------------------------------------------------ start

    @Test
    void startBuildsOnlyApplicableSteps() {
        service.start(order, List.of(item("LAB"), item("CONSULTATION")));
        ArgumentCaptor<SagaState> cap = ArgumentCaptor.forClass(SagaState.class);
        verify(sagaRepository).save(cap.capture());
        SagaState saved = cap.getValue();
        assertThat(saved.getSagaStatus()).isEqualTo(SagaState.IN_PROGRESS);
        assertThat(saved.applicableSteps()).containsExactly("BILLING", "LAB");
        assertThat(saved.getBillingStep()).isEqualTo(SagaState.PENDING);
        assertThat(saved.getLabStep()).isEqualTo(SagaState.PENDING);
        assertThat(saved.getRadiologyStep()).isNull();
        assertThat(saved.getNursingStep()).isNull();
        assertThat(saved.getPharmacyStep()).isNull();
        verify(outboxService).append(eq(OutboxPublisher.OrderingEvents.ORDER_INITIATED),
                eq("order"), eq(42L), eq("7001"), any(), contains("order-initiated:42"));
    }

    @Test
    void startAddsNursingRadAndPharmacyStepsWhenApplicable() {
        order.setAdmissionId(8001L);
        order.setEncounterId(null);
        order.setPharmacyPrescriptionId(77L);
        service.start(order, List.of(item("RADIOLOGY"), item("NURSING")));
        ArgumentCaptor<SagaState> cap = ArgumentCaptor.forClass(SagaState.class);
        verify(sagaRepository).save(cap.capture());
        assertThat(cap.getValue().applicableSteps())
                .containsExactly("BILLING", "RADIOLOGY", "NURSING", "PHARMACY");
        // partition key fallback '<admissionId>' when encounterId is null
        verify(outboxService).append(any(), any(), eq(42L), eq("8001"), any(), any());
    }

    // ------------------------------------------------------------------ confirm

    private SagaState started(String... steps) {
        saga = new SagaState();
        saga.setSagaId(500L);
        saga.setOrderId(42L);
        saga.setSagaStatus(SagaState.IN_PROGRESS);
        for (String s : steps) {
            saga.setStep(s, SagaState.PENDING);
        }
        when(sagaRepository.findByOrderId(42L)).thenReturn(Optional.of(saga));
        return saga;
    }

    @Test
    void confirmAllStepsCompletesOrderAndSaga() {
        started(SagaState.BILLING, SagaState.LAB);
        service.confirmStep(42L, SagaState.BILLING);
        assertThat(saga.step(SagaState.LAB)).isEqualTo(SagaState.PENDING);
        SagaState done = service.confirmStep(42L, SagaState.LAB);   // final step
        assertThat(done.getSagaStatus()).isEqualTo(SagaState.COMPLETED);
        assertThat(done.allStepsSucceeded()).isTrue();
        assertThat(order.getStatusCode()).isEqualTo(ClinicalOrder.STATUS_CONFIRMED);
    }

    @Test
    void confirmRedeliveryIsIdempotent() {
        started(SagaState.BILLING, SagaState.LAB);   // two steps: first confirm stays mid-flight
        service.confirmStep(42L, SagaState.BILLING);
        SagaState again = service.confirmStep(42L, SagaState.BILLING);   // consumer retry
        assertThat(again.getSagaStatus()).isEqualTo(SagaState.IN_PROGRESS);   // no side effects on retry
        assertThat(saga.step(SagaState.BILLING)).isEqualTo(SagaState.SUCCEEDED);
        // redelivery AFTER completion (all steps done) also stays quiet — no ORD_ILLEGAL_TRANSITION
        service.confirmStep(42L, SagaState.LAB);
        SagaState postCompletion = service.confirmStep(42L, SagaState.BILLING);
        assertThat(postCompletion.getSagaStatus()).isEqualTo(SagaState.COMPLETED);
    }

    @Test
    void confirmNonApplicableStepRejected() {
        started(SagaState.BILLING);
        assertThatThrownBy(() -> service.confirmStep(42L, SagaState.RADIOLOGY))
                .isInstanceOfSatisfying(InvalidOrderException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo("ORD_SAGA_STEP_INVALID"));
    }

    @Test
    void confirmUnknownStepNameRejected() {
        started(SagaState.BILLING);
        assertThatThrownBy(() -> service.confirmStep(42L, "TELEPORT"))
                .isInstanceOfSatisfying(InvalidOrderException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo("ORD_SAGA_STEP_INVALID"));
    }

    // ------------------------------------------------------------------ fail / compensate

    @Test
    void failureCompensatesSucceededStepsAndEmitsOrderFailedSameTx() {
        saga = started(SagaState.BILLING, SagaState.LAB);
        service.confirmStep(42L, SagaState.BILLING);

        SagaState failed = service.failStep(42L, SagaState.LAB, "PACS unreachable");

        assertThat(failed.getSagaStatus()).isEqualTo(SagaState.FAILED);
        assertThat(failed.getFailedStep()).isEqualTo(SagaState.LAB);
        assertThat(failed.getFailureReason()).isEqualTo("PACS unreachable");
        // BILLING had SUCCEEDED before LAB failed -> compensated ("roll back reservations")
        assertThat(failed.step(SagaState.BILLING)).isEqualTo(SagaState.COMPENSATED);
        assertThat(failed.step(SagaState.LAB)).isEqualTo(SagaState.COMPENSATED);
        assertThat(order.getStatusCode()).isEqualTo(ClinicalOrder.STATUS_FAILED);
        assertThat(order.getRejectionReason()).isEqualTo("PACS unreachable");
        // OrderFailed written in the SAME tx as the FAILED flip
        ArgumentCaptor<String> payloadCap = ArgumentCaptor.forClass(String.class);
        verify(outboxService).append(eq(OutboxPublisher.OrderingEvents.ORDER_FAILED),
                eq("order"), eq(42L), eq("7001"), payloadCap.capture(), contains("order-failed:42:LAB"));
        assertThat(payloadCap.getValue()).contains("\"failedStep\":\"LAB\"")
                .contains("BILLING").contains("PACS unreachable");
    }

    @Test
    void failureReplayAgainstFailedSagaIsIdempotent() {
        started(SagaState.BILLING);
        service.failStep(42L, SagaState.BILLING, "first rejection");
        SagaState replay = service.failStep(42L, SagaState.BILLING, "second rejection");
        assertThat(replay.getSagaStatus()).isEqualTo(SagaState.FAILED);
        assertThat(replay.getFailureReason()).isEqualTo("second rejection");   // refreshed, no error
    }

    @Test
    void failAfterCompletedSagaRejected() {
        started(SagaState.BILLING);
        service.confirmStep(42L, SagaState.BILLING);
        assertThatThrownBy(() -> service.failStep(42L, SagaState.BILLING, "late failure"))
                .isInstanceOfSatisfying(InvalidOrderException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo("ORD_ILLEGAL_TRANSITION"));
        verify(outboxService, never()).append(eq(OutboxPublisher.OrderingEvents.ORDER_FAILED),
                any(), any(), any(), any(), any());
    }

    @Test
    void failRequiresReason() {
        started(SagaState.BILLING);
        assertThatThrownBy(() -> service.failStep(42L, SagaState.BILLING, "  "))
                .isInstanceOfSatisfying(InvalidOrderException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo("BEXA003"));
    }

    // ------------------------------------------------------------------ cancel

    @Test
    void cancelCompensatesAndEmitsOrderFailedWithCancelStep() {
        saga = started(SagaState.BILLING, SagaState.LAB);
        service.confirmStep(42L, SagaState.BILLING);

        SagaState cancelled = service.cancel(42L);

        assertThat(cancelled.getSagaStatus()).isEqualTo(SagaState.CANCELLED);
        assertThat(cancelled.step(SagaState.BILLING)).isEqualTo(SagaState.COMPENSATED);
        assertThat(cancelled.getFailedStep()).isEqualTo("CANCELLED_BY_USER");
        // NOTE: the order-status flip is the CALLER's job (OrderService.cancel sets CANCELLED
        // before delegating) — SagaService only owns saga_state + the compensation event.
        verify(outboxService).append(eq(OutboxPublisher.OrderingEvents.ORDER_FAILED),
                eq("order"), eq(42L), eq("7001"), any(), contains("CANCELLED_BY_USER"));
    }

    @Test
    void cancelTerminalSagaRejected() {
        started(SagaState.BILLING);
        service.failStep(42L, SagaState.BILLING, "boom");
        assertThatThrownBy(() -> service.cancel(42L))
                .isInstanceOfSatisfying(InvalidOrderException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo("ORD_ILLEGAL_TRANSITION"));
    }
}
