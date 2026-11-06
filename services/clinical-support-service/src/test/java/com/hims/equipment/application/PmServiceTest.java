package com.hims.equipment.application;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.equipment.application.exceptions.EquipmentNotFoundException;
import com.hims.equipment.application.exceptions.InvalidEquipmentRequestException;
import com.hims.equipment.domain.model.Equipment;
import com.hims.equipment.domain.model.EquipmentPreventiveChecklist;
import com.hims.equipment.domain.model.EquipmentWorkingStatus;
import com.hims.equipment.domain.model.EqpPmSchedule;
import com.hims.equipment.domain.model.EqpPmScheduleDetails;
import com.hims.equipment.domain.model.EqpPreventiveMaintenance;
import com.hims.equipment.domain.model.PmStatus;
import com.hims.equipment.domain.repository.EquipmentPreventiveChecklistRepository;
import com.hims.equipment.domain.repository.EquipmentRepository;
import com.hims.equipment.domain.repository.EqpPmScheduleDetailsRepository;
import com.hims.equipment.domain.repository.EqpPmScheduleRepository;
import com.hims.equipment.domain.repository.EqpPreventiveMaintenanceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** L1 — Preventive-maintenance business rules: schedule creation (sequence ids,
 *  IHMS/EQ/PM code generation, pmType/frequency guards, month-flag defaulting,
 *  parent-first detail-line saves), gated schedule search, completion recording
 *  (pmStatus defaulting via key-absence detection, server-owned pairing,
 *  next-due roll-forward by frequency months) and checklist template CRUD. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PmServiceTest {

    /** Mirrors Spring Boot's auto-configured mapper: unknown payload keys
     *  (e.g. the nested {@code details[]} array on a schedule-create request,
     *  consumed separately by the service) must be tolerated, not failed. */
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Mock private EquipmentRepository equipmentRepository;
    @Mock private EqpPmScheduleRepository scheduleRepository;
    @Mock private EqpPmScheduleDetailsRepository scheduleDetailsRepository;
    @Mock private EqpPreventiveMaintenanceRepository maintenanceRepository;
    @Mock private EquipmentPreventiveChecklistRepository checklistRepository;
    @Mock private EntityManager em;
    @Mock private Query query;

    private PmService service;

    @BeforeEach
    void setUp() {
        service = new PmService(equipmentRepository, scheduleRepository,
                scheduleDetailsRepository, maintenanceRepository, checklistRepository, mapper);
        ReflectionTestUtils.setField(service, "em", em);
        // sequence stub: every nextval(...) yields 1
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1L);
        when(scheduleRepository.save(any(EqpPmSchedule.class))).thenAnswer(inv -> inv.getArgument(0));
        when(scheduleDetailsRepository.save(any(EqpPmScheduleDetails.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(maintenanceRepository.save(any(EqpPreventiveMaintenance.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(checklistRepository.save(any(EquipmentPreventiveChecklist.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        // duplicate-reference lookups default to free
        when(scheduleRepository.findByScheduleCodeIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(checklistRepository.findByCodeIgnoreCase(anyString())).thenReturn(Optional.empty());
    }

    // ------------------------------------------------------- create schedules

    @Test
    void createSchedule_validData_assignsSeqIdGeneratedCodeDefaultsAndAuditStamps() throws Exception {
        activeAsset(1L);

        EqpPmSchedule s = service.createSchedule(mapper.readTree("""
                {"equipmentId":1,"frequency":6,"pmType":3}
                """), 7L);

        assertThat(s.getId()).isEqualTo(1L);                       // eqp_pm_schedule_seq (mocked)
        assertThat(s.getEquipmentId()).isEqualTo(1L);
        assertThat(s.getScheduleCode())
                .isEqualTo("IHMS/EQ/PM/" + LocalDate.now().getYear() + "/00001");
        assertThat(s.getPmType()).isEqualTo(3L);                   // dictionary id honored (CALIBRATION)
        assertThat(s.getFrequency()).isEqualTo(6L);
        assertThat(s.getPmDate()).isNotNull();                     // plan start defaulted to now
        assertThat(s.getNextDueDate()).isEqualTo(s.getPmDate());   // a new plan is due immediately
        assertThat(s.getStoreId()).isEqualTo(33L);                 // snapshotted from the asset
        assertThat(s.getStatus()).isTrue();

        // legacy month flags: none supplied => exactly the plan-start month is flagged
        boolean[] flags = { s.getJan(), s.getFeb(), s.getMar(), s.getApr(), s.getMay(), s.getJun(),
                s.getJul(), s.getAug(), s.getSep(), s.getOct(), s.getNov(), s.getDec() };
        int month = LocalDate.now().getMonthValue();
        for (int i = 0; i < flags.length; i++) {
            assertThat(flags[i]).as("month flag %d", i + 1).isEqualTo(i + 1 == month);
        }

        // base audit block
        assertThat(s.getUnitId()).isEqualTo(7L);
        assertThat(s.getCreatedUnitId()).isEqualTo(7L);
        assertThat(s.getUpdatedUnitId()).isEqualTo(7L);
        assertThat(s.getAddedBy()).isEqualTo(PmService.AUDIT_ACTOR_ID);
        assertThat(s.getAddedDatetime()).isNotNull();
        assertThat(s.getUpdatedBy()).isNull();
        assertThat(s.getUpdatedDatetime()).isNull();
        assertThat(s.getSynchronizedd()).isFalse();

        verify(em, times(2)).createNativeQuery(anyString());       // code seq + row-id seq
        verify(scheduleRepository).save(any(EqpPmSchedule.class));
        verify(scheduleDetailsRepository, never()).save(any(EqpPmScheduleDetails.class));
    }

    @Test
    void createSchedule_clientSuppliedUniqueCode_trimmedAndHonored_noCodeSeqBurn() throws Exception {
        activeAsset(1L);

        EqpPmSchedule s = service.createSchedule(mapper.readTree(
                "{\"equipmentId\":1,\"scheduleCode\":\"  EQPM-001 \"}"), 7L);

        assertThat(s.getScheduleCode()).isEqualTo("EQPM-001");
        verify(scheduleRepository).findByScheduleCodeIgnoreCase("EQPM-001");
        verify(em, times(1)).createNativeQuery(anyString());       // row id only — code came from client
    }

    @Test
    void createSchedule_duplicateSuppliedCode_throwsAndDoesNotSave() throws Exception {
        activeAsset(1L);
        when(scheduleRepository.findByScheduleCodeIgnoreCase("dup"))
                .thenReturn(Optional.of(new EqpPmSchedule()));

        assertThatThrownBy(() -> service.createSchedule(mapper.readTree(
                "{\"equipmentId\":1,\"scheduleCode\":\" dup \"}"), 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class)
                .hasMessageContaining("already exists");
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void createSchedule_missingEquipmentId_throwsBeforeAssetLookupOrSave() throws Exception {
        assertThatThrownBy(() -> service.createSchedule(
                mapper.readTree("{\"frequency\":6}"), 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class)
                .hasMessageContaining("equipmentId");
        verify(equipmentRepository, never()).findById(any());
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void createSchedule_invalidPmType_throws() throws Exception {
        activeAsset(1L);
        assertThatThrownBy(() -> service.createSchedule(
                mapper.readTree("{\"equipmentId\":1,\"pmType\":9}"), 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class)
                .hasMessageContaining("Invalid pmType");
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void createSchedule_nonPositiveFrequency_throws() throws Exception {
        activeAsset(1L);
        assertThatThrownBy(() -> service.createSchedule(
                mapper.readTree("{\"equipmentId\":1,\"frequency\":0}"), 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class)
                .hasMessageContaining("positive");
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void createSchedule_unknownAsset_throwsNotFound() throws Exception {
        when(equipmentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createSchedule(
                mapper.readTree("{\"equipmentId\":99}"), 7L))
                .isInstanceOf(EquipmentNotFoundException.class)
                .hasMessageContaining("99");
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void createSchedule_inactiveAsset_throws() throws Exception {
        Equipment asset = asset(1L);
        asset.setStatus(false);
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> service.createSchedule(
                mapper.readTree("{\"equipmentId\":1}"), 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class)
                .hasMessageContaining("inactive");
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void createSchedule_condemnedAsset_throws() throws Exception {
        Equipment asset = asset(1L);
        asset.setWorkingStatus(EquipmentWorkingStatus.CONDEMNED);
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> service.createSchedule(
                mapper.readTree("{\"equipmentId\":1}"), 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class)
                .hasMessageContaining("condemned");
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void createSchedule_explicitMonthFlagsPreserved_noAutoDefault() throws Exception {
        activeAsset(1L);

        EqpPmSchedule s = service.createSchedule(mapper.readTree(
                "{\"equipmentId\":1,\"jan\":true,\"jul\":true}"), 7L);

        assertThat(s.getJan()).isTrue();
        assertThat(s.getJul()).isTrue();
        // every other flag must stay false regardless of today's date —
        // the auto-default only fires when NO flag is set
        assertThat(s.getFeb()).isFalse();
        assertThat(s.getMar()).isFalse();
        assertThat(s.getApr()).isFalse();
        assertThat(s.getMay()).isFalse();
        assertThat(s.getJun()).isFalse();
        assertThat(s.getAug()).isFalse();
        assertThat(s.getSep()).isFalse();
        assertThat(s.getOct()).isFalse();
        assertThat(s.getNov()).isFalse();
        assertThat(s.getDec()).isFalse();
    }

    @Test
    void createSchedule_detailLines_savedAfterParentWithOwnIdsAndPairing() throws Exception {
        activeAsset(1L);

        EqpPmSchedule saved = service.createSchedule(mapper.readTree("""
                {"equipmentId":1,"details":[
                   {"scheduledDate":"2026-09-01T00:00:00Z"},
                   {"scheduledDate":"2026-12-01T00:00:00Z"}]}
                """), 7L);

        // no JPA relationship mapping => header row MUST insert before its lines
        InOrder order = inOrder(scheduleRepository, scheduleDetailsRepository);
        order.verify(scheduleRepository).save(any(EqpPmSchedule.class));
        order.verify(scheduleDetailsRepository, times(2)).save(any(EqpPmScheduleDetails.class));

        ArgumentCaptor<EqpPmScheduleDetails> lines =
                ArgumentCaptor.forClass(EqpPmScheduleDetails.class);
        verify(scheduleDetailsRepository, times(2)).save(lines.capture());
        assertThat(lines.getAllValues())
                .allSatisfy(l -> {
                    assertThat(l.getId()).isEqualTo(1L);           // eqp_pm_schedule_details_seq
                    assertThat(l.getPmScheduleId()).isEqualTo(saved.getId()); // FK pairing
                    assertThat(l.getStatus()).isTrue();
                    assertThat(l.getPreventiveMaintenance()).isTrue(); // column default kept
                    assertThat(l.getAddedBy()).isEqualTo(PmService.AUDIT_ACTOR_ID);
                });
        assertThat(lines.getAllValues().get(0).getScheduledDate())
                .isEqualTo(Timestamp.from(Instant.parse("2026-09-01T00:00:00Z")));
    }

    // ------------------------------------------------------------------ search

    @Test
    void search_withFilters_passesGatedParamsAndTrimmedText() {
        Timestamp dueFrom = Timestamp.from(Instant.parse("2026-01-01T00:00:00Z"));
        Timestamp dueTo = Timestamp.from(Instant.parse("2026-03-31T00:00:00Z"));

        service.search(7L, 1L, 2L, dueFrom, dueTo, "  vent  ");

        verify(scheduleRepository).search(7L, true, 1L, true, 2L, true,
                dueFrom, true, dueTo, true, "vent", true);
    }

    @Test
    void search_withoutFilters_passesSentinelsWithFalseGates() {
        service.search(null, null, null, null, null, null);

        verify(scheduleRepository).search(0L, false, 0L, false, 0L, false,
                null, false, null, false, "", false);
    }

    @Test
    void search_invalidPmType_throws() {
        assertThatThrownBy(() -> service.search(null, null, 9L, null, null, null))
                .isInstanceOf(InvalidEquipmentRequestException.class)
                .hasMessageContaining("Invalid pmType");
    }

    // --------------------------------------------------------------- complete

    @Test
    void complete_defaultsCompletedStatus_andRollsNextDueForwardByFrequency() throws Exception {
        EqpPmSchedule schedule = scheduleWithFrequency(6L);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        activeAsset(1L);                                           // itemId snapshot source

        EqpPreventiveMaintenance m = service.complete(1L, mapper.readTree("""
                {"maintenanceDate":"2026-08-01T00:00:00Z","performedBy":77,"amountIncurred":1500.25}
                """));

        // server-owned pairing + sequence id
        assertThat(m.getId()).isEqualTo(1L);                       // eqp_preventive_maintenance_seq
        assertThat(m.getPmScheduleId()).isEqualTo(1L);
        assertThat(m.getEquipmentId()).isEqualTo(1L);
        assertThat(m.getItemId()).isEqualTo(501L);                 // snapshotted from the asset
        assertThat(m.getPmStatus()).isEqualTo(PmStatus.COMPLETED); // key absent => COMPLETED(3)
        assertThat(m.getMaintenanceNo())
                .isEqualTo("PM-" + LocalDate.now().getYear() + "-00001");
        assertThat(m.getMaintenanceDate())
                .isEqualTo(Timestamp.from(Instant.parse("2026-08-01T00:00:00Z")));
        assertThat(m.getPerformedBy()).isEqualTo(77L);
        assertThat(m.getAmountIncurred()).isEqualByComparingTo(new BigDecimal("1500.25"));
        assertThat(m.getStatus()).isTrue();
        assertThat(m.getAddedBy()).isEqualTo(PmService.AUDIT_ACTOR_ID);
        assertThat(m.getUnitId()).isEqualTo(7L);                   // stamped from the SCHEDULE's unit

        // atomic bookkeeping on the schedule
        ArgumentCaptor<EqpPmSchedule> savedSchedule = ArgumentCaptor.forClass(EqpPmSchedule.class);
        verify(scheduleRepository).save(savedSchedule.capture());
        assertThat(savedSchedule.getValue().getLastCompletedDate())
                .isEqualTo(Timestamp.from(Instant.parse("2026-08-01T00:00:00Z")));
        Timestamp expectedNextDue = Timestamp.valueOf(LocalDateTime.ofInstant(
                Instant.parse("2026-08-01T00:00:00Z"), ZoneId.systemDefault()).plusMonths(6));
        assertThat(savedSchedule.getValue().getNextDueDate()).isEqualTo(expectedNextDue);
        assertThat(savedSchedule.getValue().getUpdatedBy()).isEqualTo(PmService.AUDIT_ACTOR_ID);
        assertThat(savedSchedule.getValue().getUpdatedDatetime()).isNotNull();
    }

    @Test
    void complete_explicitPmStatusHonored() throws Exception {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(scheduleWithFrequency(6L)));

        EqpPreventiveMaintenance m = service.complete(1L, mapper.readTree("{\"pmStatus\":2}"));
        assertThat(m.getPmStatus()).isEqualTo(PmStatus.IN_PROGRESS);
    }

    @Test
    void complete_invalidExplicitPmStatus_throwsAndDoesNotSave() throws Exception {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(scheduleWithFrequency(6L)));

        assertThatThrownBy(() -> service.complete(1L, mapper.readTree("{\"pmStatus\":9}")))
                .isInstanceOf(InvalidEquipmentRequestException.class)
                .hasMessageContaining("Invalid pmStatus");
        verify(maintenanceRepository, never()).save(any());
    }

    @Test
    void complete_withoutFrequency_stampsLastCompleted_leavesNextDueAlone() throws Exception {
        EqpPmSchedule schedule = scheduleWithFrequency(null);
        Timestamp originalNextDue = schedule.getNextDueDate();
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        EqpPreventiveMaintenance m = service.complete(1L, mapper.readTree("{}"));

        assertThat(m.getPmStatus()).isEqualTo(PmStatus.COMPLETED);
        assertThat(m.getMaintenanceNo())
                .isEqualTo("PM-" + LocalDate.now().getYear() + "-00001"); // client-blank => generated
        ArgumentCaptor<EqpPmSchedule> savedSchedule = ArgumentCaptor.forClass(EqpPmSchedule.class);
        verify(scheduleRepository).save(savedSchedule.capture());
        assertThat(savedSchedule.getValue().getLastCompletedDate()).isNotNull();
        assertThat(savedSchedule.getValue().getNextDueDate()).isEqualTo(originalNextDue);
    }

    @Test
    void complete_foreignDetailId_throwsBeforeAnySaveOrRollforward() throws Exception {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(scheduleWithFrequency(6L)));
        EqpPmScheduleDetails ownLine = new EqpPmScheduleDetails();
        ownLine.setId(5L);
        ownLine.setPmScheduleId(1L);
        when(scheduleDetailsRepository.findByPmScheduleIdOrderByIdAsc(1L))
                .thenReturn(List.of(ownLine));

        assertThatThrownBy(() -> service.complete(1L,
                mapper.readTree("{\"pmScheduleDetailId\":99}")))
                .isInstanceOf(InvalidEquipmentRequestException.class)
                .hasMessageContaining("does not belong");
        verify(maintenanceRepository, never()).save(any());        // no execution row …
        verify(scheduleRepository, never()).save(any());           // … and no bookkeeping move
    }

    @Test
    void complete_belongingDetailId_passesOwnershipCheckAndIsKept() throws Exception {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(scheduleWithFrequency(6L)));
        EqpPmScheduleDetails ownLine = new EqpPmScheduleDetails();
        ownLine.setId(5L);
        ownLine.setPmScheduleId(1L);
        when(scheduleDetailsRepository.findByPmScheduleIdOrderByIdAsc(1L))
                .thenReturn(List.of(ownLine));

        EqpPreventiveMaintenance m = service.complete(1L,
                mapper.readTree("{\"pmScheduleDetailId\":5}"));
        assertThat(m.getPmScheduleDetailId()).isEqualTo(5L);
        verify(maintenanceRepository).save(any(EqpPreventiveMaintenance.class));
    }

    @Test
    void complete_unknownSchedule_throwsNotFound() throws Exception {
        when(scheduleRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.complete(404L, mapper.readTree("{}")))
                .isInstanceOf(EquipmentNotFoundException.class)
                .hasMessageContaining("PM schedule");
        verify(maintenanceRepository, never()).save(any());
    }

    // -------------------------------------------------------- history / lists

    @Test
    void history_unknownSchedule_throwsBeforeQuery() {
        when(scheduleRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.history(99L))
                .isInstanceOf(EquipmentNotFoundException.class);
        verify(maintenanceRepository, never()).findByPmScheduleIdOrderByIdAsc(any());
    }

    @Test
    void history_returnsChronologicalExecutionLog() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(scheduleWithFrequency(6L)));
        EqpPreventiveMaintenance m1 = new EqpPreventiveMaintenance();
        m1.setId(11L);
        m1.setPmScheduleId(1L);
        EqpPreventiveMaintenance m2 = new EqpPreventiveMaintenance();
        m2.setId(12L);
        m2.setPmScheduleId(1L);
        when(maintenanceRepository.findByPmScheduleIdOrderByIdAsc(1L)).thenReturn(List.of(m1, m2));

        assertThat(service.history(1L)).containsExactly(m1, m2);
    }

    @Test
    void schedulesOf_unknownAsset_throwsBeforeQuery() {
        when(equipmentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.schedulesOf(99L))
                .isInstanceOf(EquipmentNotFoundException.class);
        verify(scheduleRepository, never()).findByEquipmentIdAndStatusTrueOrderByIdDesc(any());
    }

    @Test
    void schedulesOf_returnsAssetPlansNewestFirst() {
        activeAsset(1L);
        EqpPmSchedule older = new EqpPmSchedule();
        older.setId(2L);
        EqpPmSchedule newer = new EqpPmSchedule();
        newer.setId(8L);
        when(scheduleRepository.findByEquipmentIdAndStatusTrueOrderByIdDesc(1L))
                .thenReturn(List.of(newer, older));

        assertThat(service.schedulesOf(1L)).containsExactly(newer, older);
    }

    // ------------------------------------------------------------- checklists

    @Test
    void createChecklist_trimsAssignsSeqIdAndStampsAudit() throws Exception {
        EquipmentPreventiveChecklist c = service.createChecklist(mapper.readTree(
                "{\"code\":\" PM-CHK-01 \",\"description\":\"Visual + electrical safety\"}"), 7L);

        assertThat(c.getCode()).isEqualTo("PM-CHK-01");
        assertThat(c.getDescription()).isEqualTo("Visual + electrical safety");
        assertThat(c.getId()).isEqualTo(1L);                       // equipment_preventive_checklist_seq
        assertThat(c.getStatus()).isTrue();
        assertThat(c.getAddedBy()).isEqualTo(PmService.AUDIT_ACTOR_ID);
        assertThat(c.getUnitId()).isEqualTo(7L);
        verify(checklistRepository).findByCodeIgnoreCase("PM-CHK-01");
    }

    @Test
    void createChecklist_missingDescription_throws() throws Exception {
        assertThatThrownBy(() -> service.createChecklist(
                mapper.readTree("{\"code\":\"PM-CHK-02\"}"), 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class)
                .hasMessageContaining("description");
        verify(checklistRepository, never()).save(any());
    }

    @Test
    void createChecklist_duplicateCode_throwsAndDoesNotSave() throws Exception {
        when(checklistRepository.findByCodeIgnoreCase("taken"))
                .thenReturn(Optional.of(new EquipmentPreventiveChecklist()));

        assertThatThrownBy(() -> service.createChecklist(
                mapper.readTree("{\"code\":\"taken\",\"description\":\"x\"}"), 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class)
                .hasMessageContaining("already exists");
        verify(checklistRepository, never()).save(any());
    }

    // ---------------------------------------------------- lifecycle end-to-end

    /** Spec chain: create → get due → complete → verify next due date. */
    @Test
    void lifecycle_create_dueImmediately_complete_advancesDueByExactFrequency() throws Exception {
        activeAsset(1L);

        EqpPmSchedule created = service.createSchedule(mapper.readTree(
                "{\"equipmentId\":1,\"frequency\":12}"), 7L);

        // a brand-new plan is due immediately (nextDue = plan start) …
        assertThat(created.getNextDueDate()).isEqualTo(created.getPmDate());

        // … so it lands in a "due up to today" listing window (inclusive dayAfter conversion)
        Timestamp today = Timestamp.from(Instant.now());
        service.search(null, null, null, null, today, null);
        verify(scheduleRepository).search(0L, false, 0L, false, 0L, false,
                null, false, today, true, "", false); // blank text => sentinel + false gate

        // recording the work rolls the plan forward by exactly frequency months
        when(scheduleRepository.findById(created.getId())).thenReturn(Optional.of(created));
        EqpPreventiveMaintenance m = service.complete(created.getId(),
                mapper.readTree("{\"maintenanceDate\":\"2026-08-21T10:00:00Z\"}"));

        assertThat(m.getPmStatus()).isEqualTo(PmStatus.COMPLETED);
        assertThat(created.getLastCompletedDate())
                .isEqualTo(Timestamp.from(Instant.parse("2026-08-21T10:00:00Z")));
        Timestamp expectedNext = Timestamp.valueOf(LocalDateTime.ofInstant(
                Instant.parse("2026-08-21T10:00:00Z"), ZoneId.systemDefault()).plusMonths(12));
        assertThat(created.getNextDueDate()).isEqualTo(expectedNext);
    }

    // ---------------------------------------------------------------- helpers

    private void activeAsset(long id) {
        when(equipmentRepository.findById(id)).thenReturn(Optional.of(asset(id)));
    }

    private Equipment asset(long id) {
        Equipment e = new Equipment();
        e.setId(id);
        e.setItemId(501L);
        e.setStoreId(33L);
        e.setAccessionNo("ACC-" + id);
        e.setUnitId(7L);
        e.setStatus(true);
        return e;
    }

    private EqpPmSchedule scheduleWithFrequency(Long frequency) {
        EqpPmSchedule s = new EqpPmSchedule();
        s.setId(1L);
        s.setEquipmentId(1L);
        s.setUnitId(7L);
        s.setFrequency(frequency);
        s.setNextDueDate(Timestamp.from(Instant.parse("2026-01-01T00:00:00Z")));
        return s;
    }
}
