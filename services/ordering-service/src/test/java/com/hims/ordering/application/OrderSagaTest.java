package com.hims.ordering.application;

import com.hims.ordering.application.exceptions.InvalidOrderException;
import com.hims.ordering.domain.model.ClinicalOrder;
import com.hims.ordering.domain.model.OrderItem;
import com.hims.ordering.domain.model.SagaState;
import com.hims.ordering.domain.repository.ClinicalOrderRepository;
import com.hims.ordering.domain.repository.OrderItemRepository;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L0 — BR-ORD-001..004 validation matrix and saga delegation for the MIG-033
 * order placement service. Saga state-machine transitions themselves are covered
 * by {@link OrderSagaStateMachineTest}; persistence semantics by the L2 suite.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderSagaTest {

    @Mock private ClinicalOrderRepository orderRepository;
    @Mock private OrderItemRepository itemRepository;
    @Mock private SagaService sagaService;
    @Mock private EntityManager em;
    @Mock private jakarta.persistence.Query seqQuery;

    private OrderService service;

    private static final SagaState STARTED_SAGA = new SagaState();

    /** The single "current" order served by both save() and findById() (get() reloads). */
    private ClinicalOrder current;
    private final java.util.List<OrderItem> currentItems = new java.util.ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new OrderService(orderRepository, itemRepository, sagaService);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "em", em);
        when(em.createNativeQuery(org.mockito.ArgumentMatchers.anyString())).thenReturn(seqQuery);
        when(seqQuery.getSingleResult()).thenReturn(100L);
        when(sagaService.byOrder(anyLong())).thenReturn(STARTED_SAGA);
        when(orderRepository.save(any())).thenAnswer(inv -> {
            current = inv.getArgument(0);
            return current;
        });
        when(itemRepository.save(any())).thenAnswer(inv -> {
            currentItems.add(inv.getArgument(0));
            return inv.getArgument(0);
        });
        when(orderRepository.findById(anyLong()))
                .thenAnswer(inv -> Optional.ofNullable(current));
        when(itemRepository.findByOrderIdOrderByLineNoAsc(anyLong())).thenReturn(currentItems);
    }

    private static OrderService.ItemInput item(String type, long itemId, String rate, int qty,
                                               String share) {
        return new OrderService.ItemInput(type, itemId, "Svc " + itemId,
                new BigDecimal(rate), qty, share == null ? null : new BigDecimal(share));
    }

    private ClinicalOrder create(OrderService.ItemInput... items) {
        return service.create(501L, 7001L, null, 11L, 2L, null, null, List.of(items));
    }

    // ------------------------------------------------------------ BR-ORD-001/002

    @Test
    void happyPathComputesTotalsAndStartsSaga() {
        ClinicalOrder o = create(
                item("LAB", 10, "500.00", 1, "10"),
                item("CONSULTATION", 20, "100.00", 2, null));
        assertThat(o.getStatusCode()).isEqualTo(ClinicalOrder.STATUS_INITIATED);
        // gross = 500*1 + 100*2 = 700; no concession; net == gross
        assertThat(o.getGrossTotal()).isEqualByComparingTo("700.00");
        assertThat(o.getConcessionAmount()).isEqualByComparingTo("0.00");
        assertThat(o.getNetTotal()).isEqualByComparingTo("700.00");
        ArgumentCaptor<List<OrderItem>> itemsCap = ArgumentCaptor.forClass(List.class);
        verify(sagaService).start(eq(o), itemsCap.capture());
        assertThat(itemsCap.getValue()).hasSize(2);
        // doctor-share default 0 (BR-ORD-004)
        assertThat(itemsCap.getValue().get(1).getDoctorSharePercent()).isEqualByComparingTo("0");
        assertThat(o.getItems()).extracting(OrderItem::getItemType)
                .containsExactly("LAB", "CONSULTATION");
    }

    @Test
    void concessionComputedHalfUpServerSide_BR_ORD_003() {
        ClinicalOrder o = service.create(501L, 7001L, null, 11L, 2L,
                new BigDecimal("12.5"), null,
                List.of(item("LAB", 10, "333.33", 3, null)));
        assertThat(o.getGrossTotal()).isEqualByComparingTo("999.99");
        // 999.99 * 12.5 / 100 = 124.99875 -> HALF_UP 2dp = 125.00
        assertThat(o.getConcessionAmount()).isEqualByComparingTo("125.00");
        assertThat(o.getNetTotal()).isEqualByComparingTo("874.99");
    }

    @Test
    void patientRequired_BR_ORD_001() {
        assertThatThrownBy(() -> service.create(null, 1L, null, 11L, null, null, null,
                List.of(item("LAB", 1, "10", 1, null))))
                .isInstanceOfSatisfying(InvalidOrderException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("BEXA003"));
    }

    @Test
    void prescriberRequired_BR_ORD_001() {
        assertThatThrownBy(() -> service.create(501L, 1L, null, null, null, null, null,
                List.of(item("LAB", 1, "10", 1, null))))
                .isInstanceOfSatisfying(InvalidOrderException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("BEXA003"));
    }

    @Test
    void bothContextsRejected_BR_ORD_001() {
        assertThatThrownBy(() -> service.create(501L, 1L, 2L, 11L, null, null, null,
                List.of(item("LAB", 1, "10", 1, null))))
                .isInstanceOfSatisfying(InvalidOrderException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("BEXA003"));
    }

    @Test
    void emptyItemsRejected_BR_ORD_002() {
        assertThatThrownBy(() -> service.create(501L, 1L, null, 11L, null, null, null, List.of()))
                .isInstanceOfSatisfying(InvalidOrderException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("BEXA003"));
    }

    @Test
    void unknownItemTypeRejected_BR_ORD_002() {
        assertThatThrownBy(() -> create(item("PHYSIO", 1, "10", 1, null)))
                .isInstanceOfSatisfying(InvalidOrderException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo("BEXA003"));
    }

    @Test
    void negativeRateRejected_BR_ORD_002() {
        assertThatThrownBy(() -> create(item("LAB", 1, "-1", 1, null)))
                .isInstanceOfSatisfying(InvalidOrderException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("BEXA003"));
    }

    @Test
    void zeroQtyRejected_BR_ORD_002() {
        assertThatThrownBy(() -> create(item("LAB", 1, "10", 0, null)))
                .isInstanceOfSatisfying(InvalidOrderException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("BEXA003"));
    }

    // ------------------------------------------------------------ BR-ORD-003/004

    @Test
    void concessionOver100Rejected_BR_ORD_003() {
        assertThatThrownBy(() -> service.create(501L, 1L, null, 11L, null,
                new BigDecimal("100.01"), null, List.of(item("LAB", 1, "10", 1, null))))
                .isInstanceOfSatisfying(InvalidOrderException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("BR-ORD-003"));
    }

    @Test
    void doctorShareOver100Rejected_BR_ORD_004() {
        assertThatThrownBy(() -> create(item("LAB", 1, "10", 1, "150")))
                .isInstanceOfSatisfying(InvalidOrderException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("BR-ORD-004"));
    }

    // ------------------------------------------------------------ lifecycle

    @Test
    void cancelGuardRejectsNonInitiated() {
        current = order(5L, ClinicalOrder.STATUS_CONFIRMED);
        assertThatThrownBy(() -> service.cancel(5L))
                .isInstanceOfSatisfying(InvalidOrderException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo("ORD_ILLEGAL_TRANSITION"));
    }

    @Test
    void cancelFlipsStatusAndCompensatesViaSaga() {
        current = order(7L, ClinicalOrder.STATUS_INITIATED);
        ClinicalOrder cancelled = service.cancel(7L);
        assertThat(cancelled.getStatusCode()).isEqualTo(ClinicalOrder.STATUS_CANCELLED);
        verify(sagaService).cancel(7L);
    }

    @Test
    void confirmStepDelegatesToSagaService() {
        current = order(9L, ClinicalOrder.STATUS_INITIATED);
        ClinicalOrder result = service.confirmStep(9L, SagaState.BILLING);
        verify(sagaService).confirmStep(9L, SagaState.BILLING);
        assertThat(result).isSameAs(current);
    }

    @Test
    void failStepDelegatesToSagaService() {
        current = order(9L, ClinicalOrder.STATUS_FAILED);
        ClinicalOrder result = service.failStep(9L, SagaState.LAB, "lab down");
        verify(sagaService).failStep(9L, SagaState.LAB, "lab down");
        assertThat(result).isSameAs(current);
    }

    // ------------------------------------------------------------------ helpers

    private ClinicalOrder order(long id, short status) {
        ClinicalOrder o = new ClinicalOrder();
        o.setOrderId(id);
        o.setPatientId(501L);
        o.setPrescriberId(11L);
        o.setStatusCode(status);
        return o;
    }

}
