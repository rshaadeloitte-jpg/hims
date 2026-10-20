package com.hims.diet.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.diet.DietServiceApplication;
import com.hims.diet.application.DietMasterService;
import com.hims.diet.application.DietOrderService;
import com.hims.diet.domain.model.DietActionStatus;
import com.hims.diet.domain.model.DietItemScheduleDetails;
import com.hims.diet.domain.model.DietOrderDetails;
import com.hims.diet.domain.model.DietSessionMaster;
import com.hims.diet.domain.model.TodayDietMenu;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L2 — MIG-023 diet service on real PG16: V1 schema applies (ihms_diet), and the
 * full diet order + food item schedule lifecycle (raise -> order food -> advance
 * -> uncheck -> hold -> remove) with replace / sign semantics.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = DietServiceApplication.class)
@DirtiesContext
@Transactional
class DietServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private DietMasterService masterService;

    @Autowired
    private DietOrderService orderService;

    @Autowired
    private ObjectMapper mapper;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private org.springframework.transaction.PlatformTransactionManager txManager;

    @BeforeEach
    void resetSchema() {
        em.createNativeQuery("SELECT setval('ihms_diet.\"diet_item_master_seq\"', 1000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_diet.\"diet_session_master_seq\"', 2000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_diet.\"diet_plan_master_seq\"', 3000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_diet.\"diet_plan_details_seq\"', 4000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_diet.\"today_diet_menu_seq\"', 5000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_diet.\"diet_order_details_seq\"', 6000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_diet.\"diet_item_schedule_details_seq\"', 7000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_diet.\"diet_order_number_seq\"', 8000)").getSingleResult();
        // Service methods run REQUIRES_NEW (commit independently of the @Transactional
        // test, which rolls back), so rows persist across tests in the shared container.
        // Truncate for isolation in a committed tx.
        org.springframework.transaction.support.TransactionTemplate tt =
                new org.springframework.transaction.support.TransactionTemplate(txManager);
        tt.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tt.executeWithoutResult(status -> {
            em.createNativeQuery("""
                    TRUNCATE ihms_diet.diet_item_schedule_details, ihms_diet.diet_order_details,
                             ihms_diet.today_diet_menu, ihms_diet.diet_plan_details,
                             ihms_diet.diet_plan_master, ihms_diet.diet_session_master,
                             ihms_diet.diet_item_master CASCADE""")
                    .executeUpdate();
        });
    }

    @Test
    void v1Schema_applies() {
        List<String> tables = em.createNativeQuery("""
                        SELECT table_name FROM information_schema.tables
                        WHERE table_schema = 'ihms_diet' AND table_name IN
                          ('diet_item_master','diet_session_master','diet_plan_master','diet_plan_details',
                           'today_diet_menu','diet_order_details','diet_item_schedule_details')
                        ORDER BY table_name""").getResultList();
        assertThat(tables).containsExactly("diet_item_master", "diet_item_schedule_details",
                "diet_order_details", "diet_plan_details", "diet_plan_master", "diet_session_master",
                "today_diet_menu");
    }

    private DietSessionMaster seedSession(long unitId) throws Exception {
        DietSessionMaster session = masterService.createDietSession(mapper.readTree(
                "{\"description\":\"Breakfast\",\"dietSequenceNumber\":1}"), unitId);
        em.clear();
        return session;
    }

    private void seedMenu(long unitId, long sessionId, long dietTypeId, long itemId) throws Exception {
        masterService.upsertTodayMenuItem(mapper.readTree(
                "{\"dietTypeId\":" + dietTypeId + ",\"dietSessionId\":" + sessionId
                        + ",\"itemId\":" + itemId + ",\"description\":\"Rice\"}"), unitId);
        em.clear();
    }

    @Test
    void fullLifecycle_raiseOrderFoodAdvanceUncheckHoldRemove() throws Exception {
        long unitId = 7L;
        DietSessionMaster session = seedSession(unitId);
        seedMenu(unitId, session.getId(), 5L, 10L);

        // raise a diet order (unreleased by default)
        DietOrderDetails order = orderService.raiseDietOrder(mapper.readTree(
                "{\"patientId\":1,\"admissionId\":1,\"wardId\":1,\"bedId\":1,\"doctorId\":1,\"serviceId\":5}"), unitId);
        em.clear();
        assertThat(order.getOrderStatus()).isEqualTo(1L);
        assertThat(order.getOrderNumber()).isNotBlank();

        // sign it -> ACTIVE (2)
        orderService.signDietOrders(List.of(order.getId()), unitId);
        em.clear();
        DietOrderDetails signed = orderService.getOrder(order.getId());
        assertThat(signed.getOrderStatus()).isEqualTo(2L);

        // order food items for today
        List<DietItemScheduleDetails> created = orderService.orderFoodItems(mapper.readTree(
                "{\"admissionId\":1,\"orderId\":" + order.getId()
                        + ",\"sessions\":[{\"sessionId\":" + session.getId()
                        + ",\"itemIds\":[10,11],\"caloriesConsume\":500}]}"), unitId);
        em.clear();
        assertThat(created).hasSize(1);
        DietItemScheduleDetails row = created.get(0);
        assertThat(row.getItemScheduleStatus()).isEqualTo(2L);
        assertThat(row.getItemIds()).isEqualTo("10,11");

        // advance -> 3
        DietItemScheduleDetails advanced = orderService.advanceFoodOrder(1L, order.getId(),
                session.getId(), row.getId(), unitId);
        em.clear();
        assertThat(advanced.getItemScheduleStatus()).isEqualTo(3L);

        // uncheck -> 2
        DietItemScheduleDetails unchecked = orderService.uncheckFoodOrder(1L, order.getId(),
                session.getId(), row.getId(), unitId);
        em.clear();
        assertThat(unchecked.getItemScheduleStatus()).isEqualTo(2L);

        // hold
        DietItemScheduleDetails held = orderService.setFoodActionStatus(1L, order.getId(),
                session.getId(), row.getId(), "hold", "patient NPO", unitId);
        em.clear();
        assertThat(held.getDietActionStatus()).isEqualTo(DietActionStatus.HOLD);
        assertThat(held.getReasonForHold()).isEqualTo("patient NPO");

        // remove one item -> keeps row
        DietItemScheduleDetails removed = orderService.removeDietItem(unitId, 1L,
                session.getId(), row.getId(), "11");
        em.clear();
        assertThat(removed.getItemIds()).isEqualTo("10");
    }

    @Test
    void raiseDietOrder_replacesExistingActiveOrder() throws Exception {
        long unitId = 7L;
        DietSessionMaster session = seedSession(unitId);
        seedMenu(unitId, session.getId(), 5L, 10L);

        DietOrderDetails first = orderService.raiseDietOrder(mapper.readTree(
                "{\"patientId\":1,\"admissionId\":1,\"serviceId\":5,\"orderStatus\":2}"), unitId);
        em.clear();
        assertThat(first.getOrderStatus()).isEqualTo(2L);

        // second order for the same patient replaces the first
        DietOrderDetails second = orderService.raiseDietOrder(mapper.readTree(
                "{\"patientId\":1,\"admissionId\":1,\"serviceId\":5}"), unitId);
        em.clear();

        DietOrderDetails reloadedFirst = orderService.getOrder(first.getId());
        assertThat(reloadedFirst.getOrderStatus()).isEqualTo(3L);   // replaced
        assertThat(second.getOrderStatus()).isEqualTo(1L);
    }

    @Test
    void raiseDietOrder_noMenu_throws() throws Exception {
        long unitId = 7L;
        seedSession(unitId);   // session but no menu item

        assertThatThrownBy(() -> orderService.raiseDietOrder(mapper.readTree(
                "{\"patientId\":1,\"admissionId\":1,\"serviceId\":5}"), unitId))
                .hasMessageContaining("No today-menu items");
    }

    @Test
    void orderFoodItems_skipsWhenCompletedToday() throws Exception {
        long unitId = 7L;
        DietSessionMaster session = seedSession(unitId);
        seedMenu(unitId, session.getId(), 5L, 10L);

        DietOrderDetails order = orderService.raiseDietOrder(mapper.readTree(
                "{\"patientId\":1,\"admissionId\":1,\"serviceId\":5,\"orderStatus\":2}"), unitId);
        em.clear();

        List<DietItemScheduleDetails> created = orderService.orderFoodItems(mapper.readTree(
                "{\"admissionId\":1,\"orderId\":" + order.getId()
                        + ",\"sessions\":[{\"sessionId\":" + session.getId() + ",\"itemIds\":[10]}]}"), unitId);
        em.clear();
        DietItemScheduleDetails row = created.get(0);

        // advance to COMPLETED (6)
        for (int i = 0; i < 4; i++) {
            orderService.advanceFoodOrder(1L, order.getId(), session.getId(), row.getId(), unitId);
        }
        em.clear();

        // re-ordering the same session today is skipped
        List<DietItemScheduleDetails> again = orderService.orderFoodItems(mapper.readTree(
                "{\"admissionId\":1,\"orderId\":" + order.getId()
                        + ",\"sessions\":[{\"sessionId\":" + session.getId() + ",\"itemIds\":[10]}]}"), unitId);
        em.clear();
        assertThat(again).isEmpty();
    }

    @Test
    void todayMenu_upsertAndSoftDelete() throws Exception {
        long unitId = 7L;
        DietSessionMaster session = seedSession(unitId);

        TodayDietMenu menu = masterService.upsertTodayMenuItem(mapper.readTree(
                "{\"dietTypeId\":5,\"dietSessionId\":" + session.getId() + ",\"itemId\":10,\"description\":\"Rice\"}"), unitId);
        em.clear();
        assertThat(menu.getStatus()).isTrue();

        TodayDietMenu removed = masterService.removeTodayMenuItem(unitId, 5L, session.getId(), 10L);
        em.clear();
        assertThat(removed.getStatus()).isFalse();

        // list excludes the soft-deleted row
        List<TodayDietMenu> active = masterService.listTodayMenuByDietType(unitId, 5L);
        assertThat(active).isEmpty();
    }

    @Test
    void dietPlan_masterAndDetails() throws Exception {
        long unitId = 7L;
        var plan = masterService.createDietPlan(mapper.readTree(
                "{\"code\":\"DP1\",\"description\":\"Diabetic plan\",\"serviceId\":5}"), unitId);
        em.clear();

        var detail = masterService.addDietPlanDetail(mapper.readTree(
                "{\"planId\":" + plan.getId() + ",\"itemId\":10,\"timing\":1,\"itemQty\":\"100\"}"), unitId);
        em.clear();
        assertThat(detail.getPlanId()).isEqualTo(plan.getId());

        List<com.hims.diet.domain.model.DietPlanDetails> details =
                masterService.listDietPlanDetails(plan.getId());
        assertThat(details).hasSize(1);
    }
}
