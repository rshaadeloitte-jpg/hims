package com.hims.diet.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.diet.application.exceptions.DietNotFoundException;
import com.hims.diet.application.exceptions.InvalidDietRequestException;
import com.hims.diet.domain.model.DietItemMaster;
import com.hims.diet.domain.model.DietPlanDetails;
import com.hims.diet.domain.model.DietPlanMaster;
import com.hims.diet.domain.model.DietSessionMaster;
import com.hims.diet.domain.model.TodayDietMenu;
import com.hims.diet.domain.repository.DietItemMasterRepository;
import com.hims.diet.domain.repository.DietPlanDetailsRepository;
import com.hims.diet.domain.repository.DietPlanMasterRepository;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/** L1 — diet masters + today-menu business rules (create/list/get, menu upsert/remove guards). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DietMasterServiceTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Mock private DietItemMasterRepository dietItemRepository;
    @Mock private DietSessionMasterRepository sessionRepository;
    @Mock private DietPlanMasterRepository planRepository;
    @Mock private DietPlanDetailsRepository planDetailsRepository;
    @Mock private TodayDietMenuRepository todayMenuRepository;
    @Mock private EntityManager em;
    @Mock private Query query;

    private DietMasterService service;

    @BeforeEach
    void setUp() {
        service = new DietMasterService(dietItemRepository, sessionRepository, planRepository,
                planDetailsRepository, todayMenuRepository, mapper);
        ReflectionTestUtils.setField(service, "em", em);
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1L);
        when(dietItemRepository.save(any(DietItemMaster.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sessionRepository.save(any(DietSessionMaster.class))).thenAnswer(inv -> inv.getArgument(0));
        when(planRepository.save(any(DietPlanMaster.class))).thenAnswer(inv -> inv.getArgument(0));
        when(planDetailsRepository.save(any(DietPlanDetails.class))).thenAnswer(inv -> inv.getArgument(0));
        when(todayMenuRepository.save(any(TodayDietMenu.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createDietItem_assignsIdAndStamps() throws Exception {
        DietItemMaster item = service.createDietItem(mapper.readTree(
                "{\"description\":\"Rice\",\"energy\":200}"), 7L);
        assertThat(item.getId()).isEqualTo(1L);
        assertThat(item.getUnitId()).isEqualTo(7L);
        assertThat(item.getAddedBy()).isNotNull();
        assertThat(item.getStatus()).isTrue();
    }

    @Test
    void createDietItem_invalidPayload_throwsBexa003() {
        assertThatThrownBy(() -> service.createDietItem(null, 7L))
                .isInstanceOf(InvalidDietRequestException.class);
    }

    @Test
    void getDietItem_missing_throwsNotFound() {
        when(dietItemRepository.findById(eq(99L))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getDietItem(99L))
                .isInstanceOf(DietNotFoundException.class);
    }

    @Test
    void createDietSession_assignsId() throws Exception {
        DietSessionMaster session = service.createDietSession(mapper.readTree(
                "{\"description\":\"Breakfast\",\"dietSequenceNumber\":1}"), 7L);
        assertThat(session.getId()).isEqualTo(1L);
        assertThat(session.getDescription()).isEqualTo("Breakfast");
    }

    @Test
    void addDietPlanDetail_requiresPlanId() {
        assertThatThrownBy(() -> service.addDietPlanDetail(mapper.readTree(
                "{\"itemId\":1}"), 7L))
                .isInstanceOf(InvalidDietRequestException.class)
                .hasMessageContaining("planId");
    }

    @Test
    void addDietPlanDetail_unknownPlan_throwsNotFound() {
        when(planRepository.findById(eq(99L))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.addDietPlanDetail(mapper.readTree(
                "{\"planId\":99,\"itemId\":1}"), 7L))
                .isInstanceOf(DietNotFoundException.class);
    }

    @Test
    void addDietPlanDetail_valid_saves() throws Exception {
        DietPlanMaster plan = new DietPlanMaster();
        plan.setId(1L);
        when(planRepository.findById(eq(1L))).thenReturn(Optional.of(plan));

        DietPlanDetails detail = service.addDietPlanDetail(mapper.readTree(
                "{\"planId\":1,\"itemId\":10,\"timing\":1}"), 7L);
        assertThat(detail.getId()).isEqualTo(1L);
        assertThat(detail.getPlanId()).isEqualTo(1L);
    }

    @Test
    void upsertTodayMenuItem_newRow_inserts() throws Exception {
        when(todayMenuRepository.findByKey(eq(7L), eq(1L), eq(1L), eq(10L)))
                .thenReturn(Optional.empty());

        TodayDietMenu menu = service.upsertTodayMenuItem(mapper.readTree(
                "{\"dietTypeId\":1,\"dietSessionId\":1,\"itemId\":10,\"description\":\"Rice\"}"), 7L);
        assertThat(menu.getId()).isEqualTo(1L);
        assertThat(menu.getStatus()).isTrue();
    }

    @Test
    void upsertTodayMenuItem_addNewItemDescription_rejected() {
        when(todayMenuRepository.findByKey(eq(7L), eq(1L), eq(1L), eq(10L)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upsertTodayMenuItem(mapper.readTree(
                "{\"dietTypeId\":1,\"dietSessionId\":1,\"itemId\":10,\"description\":\"Add New Item\"}"), 7L))
                .isInstanceOf(InvalidDietRequestException.class);
    }

    @Test
    void upsertTodayMenuItem_existingRow_reactivates() throws Exception {
        TodayDietMenu existing = new TodayDietMenu();
        existing.setId(5L);
        existing.setStatus(false);
        existing.setDescription("Old");
        when(todayMenuRepository.findByKey(eq(7L), eq(1L), eq(1L), eq(10L)))
                .thenReturn(Optional.of(existing));

        TodayDietMenu menu = service.upsertTodayMenuItem(mapper.readTree(
                "{\"dietTypeId\":1,\"dietSessionId\":1,\"itemId\":10,\"description\":\"New\"}"), 7L);
        assertThat(menu.getId()).isEqualTo(5L);
        assertThat(menu.getStatus()).isTrue();
        assertThat(menu.getDescription()).isEqualTo("New");
    }

    @Test
    void removeTodayMenuItem_softDeletes() {
        TodayDietMenu existing = new TodayDietMenu();
        existing.setId(5L);
        existing.setStatus(true);
        when(todayMenuRepository.findByKey(eq(7L), eq(1L), eq(1L), eq(10L)))
                .thenReturn(Optional.of(existing));

        TodayDietMenu removed = service.removeTodayMenuItem(7L, 1L, 1L, 10L);
        assertThat(removed.getStatus()).isFalse();
    }

    @Test
    void removeTodayMenuItem_missing_throwsNotFound() {
        when(todayMenuRepository.findByKey(eq(7L), eq(1L), eq(1L), eq(10L)))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.removeTodayMenuItem(7L, 1L, 1L, 10L))
                .isInstanceOf(DietNotFoundException.class);
    }
}
