package com.hims.diet.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.diet.application.exceptions.DietNotFoundException;
import com.hims.diet.application.exceptions.InvalidDietRequestException;
import com.hims.diet.domain.model.DietActionStatus;
import com.hims.diet.domain.model.DietItemScheduleDetails;
import com.hims.diet.domain.model.DietOrderDetails;
import com.hims.diet.domain.model.DietSessionMaster;
import com.hims.diet.domain.model.TodayDietMenu;
import com.hims.diet.domain.repository.DietItemScheduleDetailsRepository;
import com.hims.diet.domain.repository.DietOrderDetailsRepository;
import com.hims.diet.domain.repository.DietSessionMasterRepository;
import com.hims.diet.domain.repository.TodayDietMenuRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/** L1 — diet order + food item schedule lifecycle business rules (raise/sign/replace, order/advance/uncheck/hold/remove, guards). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DietOrderServiceTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Mock private DietOrderDetailsRepository orderRepository;
    @Mock private DietItemScheduleDetailsRepository scheduleRepository;
    @Mock private DietSessionMasterRepository sessionRepository;
    @Mock private TodayDietMenuRepository todayMenuRepository;
    @Mock private EntityManager em;
    @Mock private Query query;

    private DietOrderService service;

    @BeforeEach
    void setUp() {
        service = new DietOrderService(orderRepository, scheduleRepository, sessionRepository,
                todayMenuRepository, mapper);
        ReflectionTestUtils.setField(service, "em", em);
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1L);
        when(orderRepository.save(any(DietOrderDetails.class))).thenAnswer(inv -> inv.getArgument(0));
        when(scheduleRepository.save(any(DietItemScheduleDetails.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void seedSessionAndMenu(long unitId, long sessionId, long dietTypeId) {
        DietSessionMaster session = new DietSessionMaster();
        session.setId(sessionId);
        session.setUnitId(unitId);
        when(sessionRepository.findScoped(eq(unitId), eq(true))).thenReturn(List.of(session));

        TodayDietMenu menu = new TodayDietMenu();
        menu.setId(1L);
        menu.setDietSessionId(sessionId);
        menu.setDietTypeId(dietTypeId);
        when(todayMenuRepository.findByDietType(eq(unitId), eq(dietTypeId))).thenReturn(List.of(menu));
    }

    @Test
    void raiseDietOrder_missingServiceId_throwsBera035() {
        assertThatThrownBy(() -> service.raiseDietOrder(mapper.readTree(
                "{\"patientId\":1,\"admissionId\":1}"), 7L))
                .isInstanceOf(InvalidDietRequestException.class)
                .hasMessageContaining("Diet type");
    }

    @Test
    void raiseDietOrder_missingPatient_throwsBexa003() {
        assertThatThrownBy(() -> service.raiseDietOrder(mapper.readTree(
                "{\"serviceId\":5,\"admissionId\":1}"), 7L))
                .isInstanceOf(InvalidDietRequestException.class);
    }

    @Test
    void raiseDietOrder_noMenuForSession_throwsBexa003() {
        // session exists but no menu item for the diet type
        DietSessionMaster session = new DietSessionMaster();
        session.setId(1L);
        when(sessionRepository.findScoped(eq(7L), eq(true))).thenReturn(List.of(session));
        when(todayMenuRepository.findByDietType(eq(7L), eq(5L))).thenReturn(List.of());

        assertThatThrownBy(() -> service.raiseDietOrder(mapper.readTree(
                "{\"patientId\":1,\"admissionId\":1,\"serviceId\":5}"), 7L))
                .isInstanceOf(InvalidDietRequestException.class)
                .hasMessageContaining("No today-menu items");
    }

    @Test
    void raiseDietOrder_default_unreleased_status1() throws Exception {
        seedSessionAndMenu(7L, 1L, 5L);
        when(orderRepository.findActiveByPatient(eq(1L), eq(7L))).thenReturn(Optional.empty());

        DietOrderDetails order = service.raiseDietOrder(mapper.readTree(
                "{\"patientId\":1,\"admissionId\":1,\"serviceId\":5}"), 7L);
        assertThat(order.getOrderStatus()).isEqualTo(1L);
        assertThat(order.getOrderNumber()).isNotBlank();
        assertThat(order.getUnitId()).isEqualTo(7L);
    }

    @Test
    void raiseDietOrder_signed_status2() throws Exception {
        seedSessionAndMenu(7L, 1L, 5L);
        when(orderRepository.findActiveByPatient(eq(1L), eq(7L))).thenReturn(Optional.empty());

        DietOrderDetails order = service.raiseDietOrder(mapper.readTree(
                "{\"patientId\":1,\"admissionId\":1,\"serviceId\":5,\"orderStatus\":2}"), 7L);
        assertThat(order.getOrderStatus()).isEqualTo(2L);
    }

    @Test
    void raiseDietOrder_replacesExistingActiveOrder() throws Exception {
        seedSessionAndMenu(7L, 1L, 5L);

        DietOrderDetails existing = new DietOrderDetails();
        existing.setId(99L);
        existing.setAdmissionId(1L);
        existing.setOrderStatus(2L);
        when(orderRepository.findActiveByPatient(eq(1L), eq(7L))).thenReturn(Optional.of(existing));

        DietItemScheduleDetails raised = new DietItemScheduleDetails();
        raised.setId(5L);
        raised.setItemScheduleStatus(2L);
        when(scheduleRepository.findReplaceable(eq(1L), eq(99L), eq(7L), anyLong(), anyBoolean(), any(), anyBoolean()))
                .thenReturn(List.of(raised));

        DietOrderDetails order = service.raiseDietOrder(mapper.readTree(
                "{\"patientId\":1,\"admissionId\":1,\"serviceId\":5}"), 7L);

        assertThat(existing.getOrderStatus()).isEqualTo(3L);   // replaced
        assertThat(raised.getStatus()).isFalse();              // invalidated
        assertThat(order.getOrderStatus()).isEqualTo(1L);
    }

    @Test
    void signDietOrders_firstActive_restReplaced() {
        DietOrderDetails a = new DietOrderDetails();
        a.setId(1L);
        DietOrderDetails b = new DietOrderDetails();
        b.setId(2L);
        when(orderRepository.findById(eq(1L))).thenReturn(Optional.of(a));
        when(orderRepository.findById(eq(2L))).thenReturn(Optional.of(b));

        boolean signed = service.signDietOrders(List.of(1L, 2L), 7L);
        assertThat(signed).isTrue();
        assertThat(a.getOrderStatus()).isEqualTo(2L);
        assertThat(b.getOrderStatus()).isEqualTo(3L);
    }

    @Test
    void signDietOrders_empty_throws() {
        assertThatThrownBy(() -> service.signDietOrders(List.of(), 7L))
                .isInstanceOf(InvalidDietRequestException.class);
    }

    @Test
    void orderFoodItems_createsRaisedRows() throws Exception {
        DietOrderDetails order = new DietOrderDetails();
        order.setId(1L);
        when(orderRepository.findById(eq(1L))).thenReturn(Optional.of(order));
        when(scheduleRepository.findReplaceable(anyLong(), anyLong(), anyLong(), anyLong(), anyBoolean(), any(), anyBoolean()))
                .thenReturn(List.of());
        when(scheduleRepository.findCompletedToday(anyLong(), anyLong(), anyLong(), any()))
                .thenReturn(Optional.empty());

        List<DietItemScheduleDetails> created = service.orderFoodItems(mapper.readTree(
                "{\"admissionId\":1,\"orderId\":1,\"sessions\":[{\"sessionId\":1,\"itemIds\":[10,11],\"caloriesConsume\":500}]}"), 7L);

        assertThat(created).hasSize(1);
        assertThat(created.get(0).getItemScheduleStatus()).isEqualTo(2L);
        assertThat(created.get(0).getItemIds()).isEqualTo("10,11");
        assertThat(created.get(0).getPresentCalories()).isEqualTo(500L);
    }

    @Test
    void orderFoodItems_skipsWhenCompletedToday() throws Exception {
        DietOrderDetails order = new DietOrderDetails();
        order.setId(1L);
        when(orderRepository.findById(eq(1L))).thenReturn(Optional.of(order));
        when(scheduleRepository.findReplaceable(anyLong(), anyLong(), anyLong(), anyLong(), anyBoolean(), any(), anyBoolean()))
                .thenReturn(List.of());
        DietItemScheduleDetails completed = new DietItemScheduleDetails();
        completed.setItemScheduleStatus(6L);
        when(scheduleRepository.findCompletedToday(anyLong(), anyLong(), anyLong(), any()))
                .thenReturn(Optional.of(completed));

        List<DietItemScheduleDetails> created = service.orderFoodItems(mapper.readTree(
                "{\"admissionId\":1,\"orderId\":1,\"sessions\":[{\"sessionId\":1,\"itemIds\":[10]}]}"), 7L);
        assertThat(created).isEmpty();
    }

    @Test
    void orderFoodItems_invalidatesReplaceableRows() throws Exception {
        DietOrderDetails order = new DietOrderDetails();
        order.setId(1L);
        when(orderRepository.findById(eq(1L))).thenReturn(Optional.of(order));

        DietItemScheduleDetails replaceable = new DietItemScheduleDetails();
        replaceable.setId(5L);
        replaceable.setItemScheduleStatus(2L);
        when(scheduleRepository.findReplaceable(anyLong(), anyLong(), anyLong(), anyLong(), anyBoolean(), any(), anyBoolean()))
                .thenReturn(List.of(replaceable));
        when(scheduleRepository.findCompletedToday(anyLong(), anyLong(), anyLong(), any()))
                .thenReturn(Optional.empty());

        service.orderFoodItems(mapper.readTree(
                "{\"admissionId\":1,\"orderId\":1,\"sessions\":[{\"sessionId\":1,\"itemIds\":[10]}]}"), 7L);
        assertThat(replaceable.getStatus()).isFalse();
    }

    @Test
    void advanceFoodOrder_incrementsStatus() {
        DietItemScheduleDetails row = new DietItemScheduleDetails();
        row.setId(1L);
        row.setItemScheduleStatus(2L);
        when(scheduleRepository.findByCompositeKey(anyLong(), anyLong(), anyLong(), anyLong(), any(), anyBoolean()))
                .thenReturn(Optional.of(row));

        DietItemScheduleDetails updated = service.advanceFoodOrder(1L, 1L, 1L, 1L, 7L);
        assertThat(updated.getItemScheduleStatus()).isEqualTo(3L);
        assertThat(updated.getDietStatusTime()).isNotNull();
    }

    @Test
    void uncheckFoodOrder_decrementsStatus() {
        DietItemScheduleDetails row = new DietItemScheduleDetails();
        row.setId(1L);
        row.setItemScheduleStatus(3L);
        when(scheduleRepository.findByCompositeKey(anyLong(), anyLong(), anyLong(), anyLong(), any(), anyBoolean()))
                .thenReturn(Optional.of(row));

        DietItemScheduleDetails updated = service.uncheckFoodOrder(1L, 1L, 1L, 1L, 7L);
        assertThat(updated.getItemScheduleStatus()).isEqualTo(2L);
    }

    @Test
    void advanceFoodOrder_missingRow_throwsNotFound() {
        when(scheduleRepository.findByCompositeKey(anyLong(), anyLong(), anyLong(), anyLong(), any(), anyBoolean()))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.advanceFoodOrder(1L, 1L, 1L, 1L, 7L))
                .isInstanceOf(DietNotFoundException.class);
    }

    @Test
    void setFoodActionStatus_hold() {
        DietItemScheduleDetails row = new DietItemScheduleDetails();
        row.setId(1L);
        when(scheduleRepository.findByCompositeKey(anyLong(), anyLong(), anyLong(), anyLong(), any(), anyBoolean()))
                .thenReturn(Optional.of(row));

        DietItemScheduleDetails updated = service.setFoodActionStatus(1L, 1L, 1L, 1L,
                "hold", "patient NPO", 7L);
        assertThat(updated.getDietActionStatus()).isEqualTo(DietActionStatus.HOLD);
        assertThat(updated.getReasonForHold()).isEqualTo("patient NPO");
    }

    @Test
    void setFoodActionStatus_invalid_throws() {
        assertThatThrownBy(() -> service.setFoodActionStatus(1L, 1L, 1L, 1L, "BOGUS", null, 7L))
                .isInstanceOf(InvalidDietRequestException.class);
    }

    @Test
    void removeDietItem_removesOne_keepsRow() {
        DietItemScheduleDetails row = new DietItemScheduleDetails();
        row.setId(1L);
        row.setItemIds("10,11,12");
        when(scheduleRepository.findForRemove(eq(7L), eq(1L), eq(1L), eq(1L)))
                .thenReturn(Optional.of(row));

        DietItemScheduleDetails updated = service.removeDietItem(7L, 1L, 1L, 1L, "11");
        assertThat(updated.getItemIds()).isEqualTo("10,12");
    }

    @Test
    void removeDietItem_lastItem_deletesRow() {
        DietItemScheduleDetails row = new DietItemScheduleDetails();
        row.setId(1L);
        row.setItemIds("10");
        when(scheduleRepository.findForRemove(eq(7L), eq(1L), eq(1L), eq(1L)))
                .thenReturn(Optional.of(row));

        DietItemScheduleDetails updated = service.removeDietItem(7L, 1L, 1L, 1L, "10");
        assertThat(updated.getItemIds()).isEmpty();
    }

    @Test
    void updateOrderStatusById_setsStatus() {
        DietOrderDetails order = new DietOrderDetails();
        order.setId(1L);
        order.setOrderStatus(1L);
        when(orderRepository.findById(eq(1L))).thenReturn(Optional.of(order));

        boolean updated = service.updateOrderStatusById(1L, 3L, 7L);
        assertThat(updated).isTrue();
        assertThat(order.getOrderStatus()).isEqualTo(3L);
    }

    @Test
    void getLatestActiveOrder_missing_throwsNotFound() {
        when(orderRepository.findLatestActiveByAdmission(eq(1L), eq(7L))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getLatestActiveOrder(1L, 7L))
                .isInstanceOf(DietNotFoundException.class);
    }
}
