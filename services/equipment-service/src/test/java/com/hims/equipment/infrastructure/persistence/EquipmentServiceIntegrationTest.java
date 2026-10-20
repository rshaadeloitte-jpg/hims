package com.hims.equipment.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.equipment.EquipmentServiceApplication;
import com.hims.equipment.application.EquipmentDocumentService;
import com.hims.equipment.application.EquipmentService;
import com.hims.equipment.application.EquipmentUsageService;
import com.hims.equipment.api.UsageReport;
import com.hims.equipment.application.MasterDataService;
import com.hims.equipment.application.OpeningBalanceService;
import com.hims.equipment.application.exceptions.EquipmentNotFoundException;
import com.hims.equipment.application.exceptions.InvalidEquipmentRequestException;
import com.hims.equipment.domain.model.ComplaintCategoryMaster;
import com.hims.equipment.domain.model.Equipment;
import com.hims.equipment.domain.model.EquipmentBase;
import com.hims.equipment.domain.model.EquipmentChecklist;
import com.hims.equipment.domain.model.EquipmentDetailDocs;
import com.hims.equipment.domain.model.EquipmentOpeningBalance;
import com.hims.equipment.domain.model.EquipmentTransfer;
import com.hims.equipment.domain.model.EquipmentUsageDetails;
import com.hims.equipment.domain.model.EquipmentWarranty;
import com.hims.equipment.domain.model.EquipmentWorkingStatus;
import com.hims.equipment.domain.model.EqpPmSchedule;
import com.hims.equipment.domain.model.EqpPmScheduleDetails;
import com.hims.equipment.domain.model.EqpPreventiveMaintenance;
import com.hims.equipment.domain.repository.EqpPmScheduleDetailsRepository;
import com.hims.equipment.domain.repository.EqpPmScheduleRepository;
import com.hims.equipment.domain.repository.EqpPreventiveMaintenanceRepository;
import com.hims.equipment.domain.repository.EquipmentDetailDocsRepository;
import com.hims.equipment.domain.repository.EquipmentMasterRepository;
import com.hims.equipment.domain.repository.EquipmentRepository;
import com.hims.equipment.domain.repository.EquipmentUsageDetailsRepository;
import com.hims.equipment.domain.repository.EquipmentWarrantyRepository;
import com.hims.equipment.domain.repository.EquipmentOpeningBalanceRepository;
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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L2 — MIG-023 equipment service on real PG16: V1 schema applies (ihms_equipment),
 * asset-register CRUD round-trip via sequences, search/filter queries, and the
 * guarded transfer lifecycle (log row + atomic department move).
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = EquipmentServiceApplication.class)
@DirtiesContext
@Transactional
class EquipmentServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private EquipmentService service;

    @Autowired
    private ObjectMapper mapper;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private org.springframework.transaction.PlatformTransactionManager txManager;

    @Autowired
    private EqpPmScheduleRepository pmScheduleRepository;
    @Autowired
    private EqpPmScheduleDetailsRepository pmScheduleDetailsRepository;
    @Autowired
    private EqpPreventiveMaintenanceRepository pmMaintenanceRepository;
    @Autowired
    private EquipmentWarrantyRepository warrantyRepository;
    @Autowired
    private EquipmentDetailDocsRepository docsRepository;
    @Autowired
    private EquipmentDocumentService documentService;
    @Autowired
    private EquipmentUsageDetailsRepository usageRepository;
    @Autowired
    private EquipmentUsageService usageService;
    @Autowired
    private EquipmentRepository equipmentRepository;
    @Autowired
    private EquipmentOpeningBalanceRepository balanceRepository;
    @Autowired
    private OpeningBalanceService balanceService;
    @Autowired
    private MasterDataService masterService;
    @Autowired
    private EquipmentMasterRepository masterRepository;

    @BeforeEach
    void resetSchema() {
        // setval is non-transactional in PG: survives the test-tx rollback, so each
        // test deterministically starts ids at fixed bases.
        em.createNativeQuery("SELECT setval('ihms_equipment.\"equipment_seq\"', 1000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_equipment.\"equipment_transfer_seq\"', 2000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_equipment.\"eqp_pm_schedule_seq\"', 3000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_equipment.\"eqp_pm_schedule_details_seq\"', 4000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_equipment.\"eqp_preventive_maintenance_seq\"', 5000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_equipment.\"equipment_opening_balance_seq\"', 6000)").getSingleResult();
        // Service methods run REQUIRES_NEW (commit independently of the @Transactional
        // test, which rolls back), so rows persist across tests in the shared container.
        // Truncate for isolation in a committed tx.
        org.springframework.transaction.support.TransactionTemplate tt =
                new org.springframework.transaction.support.TransactionTemplate(txManager);
        tt.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tt.executeWithoutResult(status -> {
            em.createNativeQuery("""
                    TRUNCATE ihms_equipment.eqp_preventive_maintenance, ihms_equipment.eqp_pm_schedule_details,
                             ihms_equipment.eqp_pm_schedule,
                             ihms_equipment.equipment_transfer, ihms_equipment.equipment,
                             ihms_equipment.equipment_warranty, ihms_equipment.equipment_detail_docs,
                             ihms_equipment.equipment_usage_details, ihms_equipment.equipment_opening_balance CASCADE""")
                    .executeUpdate();
        });
    }

    // ------------------------------------------------------------- V1 schema

    @Test
    void v1Schema_applies() {
        List<String> tables = em.createNativeQuery("""
                        SELECT table_name FROM information_schema.tables
                        WHERE table_schema = 'ihms_equipment' AND table_name IN
                          ('equipment','equipment_transfer','equipment_warranty','equipment_detail_docs',
                           'equipment_usage_details','equipment_opening_balance',
                           'complaint_status_master','maintenance_type_master',
                           'equipment_working_status_master','amc_agent_master')
                        ORDER BY table_name""").getResultList();
        assertThat(tables).containsExactly(
                "amc_agent_master",
                "complaint_status_master",
                "equipment",
                "equipment_detail_docs",
                "equipment_opening_balance",
                "equipment_transfer",
                "equipment_usage_details",
                "equipment_warranty",
                "equipment_working_status_master",
                "maintenance_type_master");

        // explicit per-table sequences (IMP-DEC-003)
        List<String> sequences = em.createNativeQuery("""
                        SELECT sequence_name FROM information_schema.sequences
                        WHERE sequence_schema = 'ihms_equipment'
                          AND sequence_name IN ('equipment_seq','equipment_transfer_seq')""").getResultList();
        assertThat(sequences).containsExactlyInAnyOrder("equipment_seq", "equipment_transfer_seq");

        // seeded status dictionaries
        assertThat(count("ihms_equipment.equipment_working_status_master")).isEqualTo(4L);
        assertThat(count("ihms_equipment.complaint_status_master")).isEqualTo(4L);
    }

    // ---------------------------------------------------------- CRUD round-trip

    @Test
    void register_readUpdate_roundTrip() throws Exception {
        Equipment saved = service.register(mapper.readTree("""
                {"itemId":501,"accessionNo":"ACC-IT-001","serialNo":"SN-77",
                 "model":"Ventilator V200","make":"Dräger",
                 "purchaseDate":"2024-01-15T00:00:00Z","installationDate":"2024-02-01T00:00:00Z",
                 "warrantyExpiry":"2027-01-15T00:00:00Z","departmentId":10,"storeId":3,
                 "location":"ICU Bay 2","cost":250000.50,"supplierId":9,
                 "custodianName":"Ravi Kumar"}
                """), 7L);

        // sequence-assigned id (setval'd to 1000 in BeforeEach), WORKING default, audit stamps
        assertThat(saved.getId()).isEqualTo(1001L);
        assertThat(saved.getItemId()).isEqualTo(501L);
        assertThat(saved.getAccessionNo()).isEqualTo("ACC-IT-001");
        assertThat(saved.getWorkingStatus()).isEqualTo(EquipmentWorkingStatus.WORKING);
        assertThat(saved.getStatus()).isTrue();
        assertThat(saved.getSynchronizedd()).isFalse();
        assertThat(saved.getUnitId()).isEqualTo(7L);
        assertThat(saved.getCreatedUnitId()).isEqualTo(7L);
        assertThat(saved.getAddedBy()).isEqualTo(EquipmentService.AUDIT_ACTOR_ID);
        assertThat(saved.getAddedDatetime()).isNotNull();

        // read back fresh from the database (REQUIRES_NEW commit outside test tx)
        em.clear();
        Equipment loaded = service.getEquipment(saved.getId());
        assertThat(loaded.getModel()).isEqualTo("Ventilator V200");
        assertThat(loaded.getCost()).isEqualByComparingTo(new BigDecimal("250000.50"));
        assertThat(loaded.getPurchaseDate()).isEqualTo(Timestamp.from(Instant.parse("2024-01-15T00:00:00Z")));

        // update: null-safe patch; server-owned columns untouched
        Timestamp addedAt = loaded.getAddedDatetime();
        Equipment updated = service.update(saved.getId(), mapper.readTree("""
                {"model":"V300","cost":260000.00,"location":"ICU Bay 5","workingStatus":3}
                """));
        assertThat(updated.getModel()).isEqualTo("V300");
        assertThat(updated.getCost()).isEqualByComparingTo(new BigDecimal("260000.00"));
        assertThat(updated.getLocation()).isEqualTo("ICU Bay 5");
        assertThat(updated.getWorkingStatus()).isEqualTo(EquipmentWorkingStatus.UNDER_REPAIR);
        assertThat(updated.getSerialNo()).isEqualTo("SN-77");           // untouched by patch
        assertThat(updated.getId()).isEqualTo(saved.getId());           // server-owned
        assertThat(updated.getUnitId()).isEqualTo(7L);                  // server-owned
        assertThat(updated.getUpdatedBy()).isEqualTo(EquipmentService.AUDIT_ACTOR_ID);
        assertThat(updated.getUpdatedDatetime()).isNotNull();

        em.clear();
        Equipment reloaded = service.getEquipment(saved.getId());
        assertThat(reloaded.getModel()).isEqualTo("V300");
        assertThat(reloaded.getAddedDatetime()).isEqualTo(addedAt);     // create stamp preserved

        // unknown id -> 404-class domain exception
        assertThatThrownBy(() -> service.getEquipment(424242L))
                .isInstanceOf(EquipmentNotFoundException.class);
    }

    // ------------------------------------------------------------ search/filters

    @Test
    void search_filters_byUnit_workingStatus_andFreeText() throws Exception {
        Equipment a = service.register(mapper.readTree(
                "{\"itemId\":501,\"accessionNo\":\"ACC-A\",\"serialNo\":\"SN-SEARCH-1\",\"model\":\"Monitor M9\"}"), 7L);
        service.register(mapper.readTree(
                "{\"itemId\":502,\"accessionNo\":\"ACC-B\",\"workingStatus\":3}"), 7L);   // UNDER_REPAIR
        service.register(mapper.readTree(
                "{\"itemId\":503,\"accessionNo\":\"ACC-Z\",\"model\":\"Defib D2\"}"), 8L); // other unit

        assertThat(service.search(7L, null, null)).extracting(Equipment::getId)
                .containsExactly(a.getId() + 1L, a.getId());            // newest first
        assertThat(service.search(8L, null, null)).hasSize(1);
        assertThat(service.search(7L, EquipmentWorkingStatus.UNDER_REPAIR, null)).hasSize(1);
        assertThat(service.search(7L, EquipmentWorkingStatus.WORKING, null))
                .allSatisfy(e -> assertThat(e.getWorkingStatus()).isEqualTo(EquipmentWorkingStatus.WORKING));

        // free text matches model/serial/accession, case-insensitive
        assertThat(service.search(null, null, "m9")).hasSize(1);
        assertThat(service.search(null, null, "sn-search")).hasSize(1);
        assertThat(service.search(null, null, "acc-z")).hasSize(1);
        assertThat(service.search(null, null, "ZZZ")).isEmpty();

        // invalid dictionary id rejected before hitting the query
        assertThatThrownBy(() -> service.search(7L, 9L, null))
                .isInstanceOfSatisfying(InvalidEquipmentRequestException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("BEXA003"));
    }

    @Test
    void accessionLookup_and_byItem() throws Exception {
        Equipment scoped = service.register(mapper.readTree(
                "{\"itemId\":601,\"accessionNo\":\"ACC-DUP\"}"), 7L);
        service.register(mapper.readTree(
                "{\"itemId\":601,\"accessionNo\":\"acc-lower\"}"), 7L);
        service.register(mapper.readTree("{\"itemId\":602,\"accessionNo\":\"OTHER\"}"), 7L);

        // case-insensitive, unit-scoped
        assertThat(service.findByAccession("acc-dup", 7L).getId()).isEqualTo(scoped.getId());
        // global fallback (no unit scope)
        assertThat(service.findByAccession("ACC-LOWER", null).getAccessionNo()).isEqualTo("acc-lower");
        assertThatThrownBy(() -> service.findByAccession("NOPE", 7L))
                .isInstanceOf(EquipmentNotFoundException.class);

        // by-item lists active assets only, newest first
        Equipment inactive = service.register(mapper.readTree(
                "{\"itemId\":601,\"accessionNo\":\"INACT\"}"), 7L);
        service.update(inactive.getId(), mapper.readTree("{\"status\":false}"));
        em.clear();
        List<Equipment> forItem = service.findByItem(601L);
        assertThat(forItem).hasSize(2);
        assertThat(forItem).allSatisfy(e -> assertThat(e.getStatus()).isTrue());
        assertThat(forItem.get(0).getId()).isGreaterThan(forItem.get(1).getId());

        // duplicate accession in the same unit is rejected by pre-validation
        assertThatThrownBy(() -> service.register(mapper.readTree(
                "{\"itemId\":601,\"accessionNo\":\"ACC-DUP\"}"), 7L))
                .isInstanceOfSatisfying(InvalidEquipmentRequestException.class,
                        e -> assertThat(e.getMessage()).contains("already exists in this unit"));
    }

    // -------------------------------------------------------- transfer lifecycle

    @Test
    void transferLifecycle_movesDepartmentAndWritesHistory() throws Exception {
        Equipment eq = service.register(mapper.readTree(
                "{\"itemId\":701,\"accessionNo\":\"ACC-T\",\"departmentId\":10,\"location\":\"Ward 1\"}"), 7L);

        EquipmentTransfer t1 = service.transfer(eq.getId(), mapper.readTree("""
                {"toDepartmentId":22,"reason":"ICU reallocation",
                 "transferDate":"2026-08-21T10:15:30Z","toLocation":"ICU-2","toStoreId":33}
                """));

        assertThat(t1.getId()).isEqualTo(2001L);                          // transfer seq base
        assertThat(t1.getEquipmentId()).isEqualTo(eq.getId());
        assertThat(t1.getFromDepartmentId()).isEqualTo(10L);
        assertThat(t1.getToDepartmentId()).isEqualTo(22L);
        assertThat(t1.getTransferDate()).isEqualTo(Timestamp.from(Instant.parse("2026-08-21T10:15:30Z")));
        assertThat(t1.getReason()).isEqualTo("ICU reallocation");
        assertThat(t1.getStatus()).isTrue();
        assertThat(t1.getUnitId()).isEqualTo(7L);

        // atomic move on the asset row (committed in the REQUIRES_NEW tx)
        em.clear();
        Equipment moved = service.getEquipment(eq.getId());
        assertThat(moved.getDepartmentId()).isEqualTo(22L);
        assertThat(moved.getLocation()).isEqualTo("ICU-2");
        assertThat(moved.getStoreId()).isEqualTo(33L);
        assertThat(moved.getUpdatedBy()).isEqualTo(EquipmentService.AUDIT_ACTOR_ID);

        // second hop; history chronological ascending
        service.transfer(eq.getId(), mapper.readTree("{\"toDepartmentId\":30}"));
        em.clear();
        List<EquipmentTransfer> history = service.transferHistory(eq.getId());
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getId()).isLessThan(history.get(1).getId());
        assertThat(history.get(0).getToDepartmentId()).isEqualTo(22L);
        assertThat(history.get(1).getFromDepartmentId()).isEqualTo(22L);
        assertThat(history.get(1).getToDepartmentId()).isEqualTo(30L);
        assertThat(history.get(1).getTransferDate()).isNotNull();         // defaulted to now
    }

    @Test
    void transferGuards_rejectAndWriteNothing() throws Exception {
        Equipment condemned = service.register(mapper.readTree(
                "{\"itemId\":801,\"accessionNo\":\"ACC-C\",\"workingStatus\":4}"), 7L);
        Equipment inactive = service.register(mapper.readTree(
                "{\"itemId\":802,\"accessionNo\":\"ACC-I\"}"), 7L);
        service.update(inactive.getId(), mapper.readTree("{\"status\":false}"));
        // active, working asset for the missing-target guard (earlier guards don't fire)
        Equipment plain = service.register(mapper.readTree(
                "{\"itemId\":803,\"accessionNo\":\"ACC-P\"}"), 7L);
        em.clear();

        assertThatThrownBy(() -> service.transfer(condemned.getId(),
                mapper.readTree("{\"toDepartmentId\":22}")))
                .isInstanceOfSatisfying(InvalidEquipmentRequestException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo("BEXA003");
                    assertThat(e.getMessage()).contains("condemned");
                });
        assertThatThrownBy(() -> service.transfer(inactive.getId(),
                mapper.readTree("{\"toDepartmentId\":22}")))
                .hasMessageContaining("inactive");
        assertThatThrownBy(() -> service.transfer(plain.getId(),
                mapper.readTree("{\"reason\":\"no target\"}")))
                .hasMessageContaining("toDepartmentId");
        assertThatThrownBy(() -> service.transfer(424242L,
                mapper.readTree("{\"toDepartmentId\":22}")))
                .isInstanceOf(EquipmentNotFoundException.class);

        // no log rows written for any rejected transfer
        assertThat(service.transferHistory(condemned.getId())).isEmpty();
        assertThat(service.transferHistory(inactive.getId())).isEmpty();
    }

    // ---------------------------------------------------- V3 PM schema + entities

    @Test
    void v3PmSchema_applies() {
        List<String> tables = em.createNativeQuery("""
                        SELECT table_name FROM information_schema.tables
                        WHERE table_schema = 'ihms_equipment' AND table_name IN
                          ('eqp_pm_schedule','eqp_pm_schedule_details','eqp_preventive_maintenance')
                        ORDER BY table_name""").getResultList();
        assertThat(tables).containsExactly(
                "eqp_pm_schedule",
                "eqp_pm_schedule_details",
                "eqp_preventive_maintenance");

        List<String> sequences = em.createNativeQuery("""
                        SELECT sequence_name FROM information_schema.sequences
                        WHERE sequence_schema = 'ihms_equipment'
                          AND sequence_name IN ('eqp_pm_schedule_seq','eqp_pm_schedule_details_seq',
                                                'eqp_preventive_maintenance_seq')""").getResultList();
        assertThat(sequences).containsExactlyInAnyOrder(
                "eqp_pm_schedule_seq", "eqp_pm_schedule_details_seq", "eqp_preventive_maintenance_seq");
    }

    @Test
    void pmEntities_persistAndReadBack_onRealColumns() throws Exception {
        Equipment asset = service.register(mapper.readTree(
                "{\"itemId\":901,\"accessionNo\":\"ACC-PM\",\"departmentId\":10}"), 7L);
        em.clear();

        EqpPmSchedule sched = new EqpPmSchedule();
        sched.setId(nextVal("ihms_equipment.eqp_pm_schedule_seq"));
        sched.setEquipmentId(asset.getId());
        sched.setScheduleCode("PM-SCH-IT");
        sched.setFrequency(6L);
        sched.setPmDate(Timestamp.from(Instant.parse("2026-01-10T00:00:00Z")));
        sched.setJan(true);
        sched.setJul(true);          // non-default month flags must round-trip
        sched.setNextDueDate(Timestamp.from(Instant.parse("2026-07-10T00:00:00Z")));
        sched.setStoreId(3L);
        pmScheduleRepository.save(sched);

        EqpPmScheduleDetails planned = new EqpPmScheduleDetails();
        planned.setId(nextVal("ihms_equipment.eqp_pm_schedule_details_seq"));
        planned.setPmScheduleId(sched.getId());
        planned.setScheduledDate(Timestamp.from(Instant.parse("2026-07-10T00:00:00Z")));
        pmScheduleDetailsRepository.save(planned);   // defaults is_preventive_maintenance=true

        EqpPreventiveMaintenance done = new EqpPreventiveMaintenance();
        done.setId(nextVal("ihms_equipment.eqp_preventive_maintenance_seq"));
        done.setEquipmentId(asset.getId());
        done.setPmScheduleId(sched.getId());
        done.setMaintenanceNo("PM-2026-00001");
        done.setMaintenanceDate(Timestamp.from(Instant.parse("2026-07-11T09:30:00Z")));
        done.setPerformedBy(42L);
        done.setFunctionalCheckRemark("All checks OK");
        done.setRemarks("Filter replaced");
        done.setAmountIncurred(new BigDecimal("1234.56"));
        pmMaintenanceRepository.save(done);
        em.flush();
        em.clear();

        EqpPmSchedule s = pmScheduleRepository.findById(sched.getId()).orElseThrow();
        assertThat(s.getEquipmentId()).isEqualTo(asset.getId());
        assertThat(s.getScheduleCode()).isEqualTo("PM-SCH-IT");
        assertThat(s.getFrequency()).isEqualTo(6L);
        assertThat(s.getPmType()).isEqualTo(1L);      // PREVENTIVE default from DDL/entity
        assertThat(s.getJan()).isTrue();
        assertThat(s.getFeb()).isFalse();             // unflagged months persist false
        assertThat(s.getNextDueDate()).isEqualTo(Timestamp.from(Instant.parse("2026-07-10T00:00:00Z")));
        assertThat(s.getStatus()).isTrue();
        assertThat(s.getSynchronizedd()).isFalse();

        List<EqpPmScheduleDetails> lines = pmScheduleDetailsRepository.findByPmScheduleIdOrderByIdAsc(sched.getId());
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).getPreventiveMaintenance()).isTrue();

        EqpPreventiveMaintenance m = pmMaintenanceRepository.findById(done.getId()).orElseThrow();
        assertThat(m.getPmStatus()).isEqualTo(1L);    // SCHEDULED default
        assertThat(m.getAmountIncurred()).isEqualByComparingTo("1234.56");   // NUMERIC(19,2)
        assertThat(m.getMaintenanceDate()).isEqualTo(Timestamp.from(Instant.parse("2026-07-11T09:30:00Z")));
        assertThat(m.getFunctionalCheckRemark()).isEqualTo("All checks OK");  // TEXT column
    }

    @Test
    void pmQueries_dueList_gatedSearch_andHistory() throws Exception {
        Equipment assetA = service.register(mapper.readTree(
                "{\"itemId\":911,\"accessionNo\":\"ACC-PMA\",\"departmentId\":10}"), 7L);
        Equipment assetB = service.register(mapper.readTree(
                "{\"itemId\":912,\"accessionNo\":\"ACC-PMB\",\"departmentId\":22}"), 7L);
        em.clear();

        EqpPmSchedule overdue = scheduleFor(assetA.getId(), "PM-DUE-A",
                Timestamp.from(Instant.parse("2026-06-01T00:00:00Z")), 7L);
        EqpPmSchedule future = scheduleFor(assetB.getId(), "PM-LATER-B",
                Timestamp.from(Instant.parse("2026-12-01T00:00:00Z")), 22L);
        EqpPmSchedule calibration = scheduleFor(assetA.getId(), "PM-CAL-A",
                Timestamp.from(Instant.parse("2026-05-01T00:00:00Z")), 7L);
        calibration.setPmType(3L);   // CALIBRATION
        pmScheduleRepository.save(overdue);
        pmScheduleRepository.save(future);
        pmScheduleRepository.save(calibration);

        EqpPreventiveMaintenance first = new EqpPreventiveMaintenance();
        first.setId(nextVal("ihms_equipment.eqp_preventive_maintenance_seq"));
        first.setEquipmentId(assetA.getId());
        first.setPmScheduleId(overdue.getId());
        first.setMaintenanceDate(Timestamp.from(Instant.parse("2025-12-01T00:00:00Z")));
        pmMaintenanceRepository.save(first);
        EqpPreventiveMaintenance second = new EqpPreventiveMaintenance();
        second.setId(nextVal("ihms_equipment.eqp_preventive_maintenance_seq"));
        second.setEquipmentId(assetA.getId());
        second.setPmScheduleId(overdue.getId());
        second.setMaintenanceDate(Timestamp.from(Instant.parse("2026-01-05T00:00:00Z")));
        pmMaintenanceRepository.save(second);
        em.flush();
        em.clear();

        // due list: only schedules due on/before the cutoff, chronological
        List<EqpPmSchedule> due = pmScheduleRepository
                .findByStatusTrueAndNextDueDateLessThanEqualOrderByNextDueDateAsc(
                        Timestamp.from(Instant.parse("2026-08-01T00:00:00Z")));
        assertThat(due).extracting(EqpPmSchedule::getScheduleCode)
                .containsExactly("PM-CAL-A", "PM-DUE-A");

        // gated search: unit + free text on schedule_code (case-insensitive)
        List<EqpPmSchedule> hit = pmScheduleRepository.search(
                7L, true, 0L, false, 0L, false,
                new Timestamp(0), false, new Timestamp(0), false, "pm-due", true);
        assertThat(hit).extracting(EqpPmSchedule::getScheduleCode).containsExactly("PM-DUE-A");

        // gated search: no filters (sentinels) returns every active schedule
        List<EqpPmSchedule> all = pmScheduleRepository.search(
                0L, false, 0L, false, 0L, false,
                new Timestamp(0), false, new Timestamp(0), false, "", false);
        assertThat(all).hasSize(3);

        // gated search: pm_type + due-date window
        List<EqpPmSchedule> cal = pmScheduleRepository.search(
                0L, false, 0L, false, 3L, true,
                Timestamp.from(Instant.parse("2026-04-01T00:00:00Z")), true,
                Timestamp.from(Instant.parse("2026-06-01T00:00:00Z")), true, "", false);
        assertThat(cal).extracting(EqpPmSchedule::getScheduleCode).containsExactly("PM-CAL-A");

        // execution history of one schedule — chronological (oldest first)
        List<EqpPreventiveMaintenance> history =
                pmMaintenanceRepository.findByPmScheduleIdOrderByIdAsc(overdue.getId());
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getMaintenanceDate())
                .isBefore(history.get(1).getMaintenanceDate());

        // asset maintenance history — newest first
        List<EqpPreventiveMaintenance> byAsset =
                pmMaintenanceRepository.findByEquipmentIdAndStatusTrueOrderByIdDesc(assetA.getId());
        assertThat(byAsset).extracting(EqpPreventiveMaintenance::getId)
                .containsExactly(second.getId(), first.getId());
    }

    /**
     * TAS-12 entity-column parity on real columns (ddl-auto=none proves nothing
     * at boot): direct persist/flush/clear/read-back of every equipment_warranty
     * column shape, then the expiry-window query + gated counts behavior.
     */
    @Test
    void warrantyEntity_persistAndReadBack_expiryQueries_onRealColumns() throws Exception {
        Equipment asset = service.register(mapper.readTree(
                "{\"itemId\":921,\"accessionNo\":\"ACC-WAR\",\"departmentId\":10}"), 7L);
        em.clear();
        java.time.LocalDate today = java.time.LocalDate.now();

        EquipmentWarranty amc = new EquipmentWarranty();
        amc.setId(nextVal("ihms_equipment.equipment_warranty_seq"));
        amc.setEquipmentId(asset.getId());
        amc.setWarrantyType("AMC");
        amc.setVendor("Acme Medical Ltd");
        amc.setStartDate(Timestamp.from(today.minusDays(355).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
        amc.setEndDate(Timestamp.from(today.plusDays(10).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
        amc.setTerms("Comprehensive cover incl. spares & labour");   // TEXT round-trip
        amc.setUnitId(7L);
        warrantyRepository.save(amc);

        EquipmentWarranty lapsed = new EquipmentWarranty();
        lapsed.setId(nextVal("ihms_equipment.equipment_warranty_seq"));
        lapsed.setEquipmentId(asset.getId());
        lapsed.setWarrantyType("WARRANTY");
        lapsed.setEndDate(Timestamp.from(today.minusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
        lapsed.setUnitId(7L);
        warrantyRepository.save(lapsed);

        EquipmentWarranty openEnded = new EquipmentWarranty();      // no end_date: never expires
        openEnded.setId(nextVal("ihms_equipment.equipment_warranty_seq"));
        openEnded.setEquipmentId(asset.getId());
        openEnded.setWarrantyType("CMC");                            // default type NOT applied on raw persist
        openEnded.setUnitId(22L);                                    // different unit on purpose
        warrantyRepository.save(openEnded);
        em.flush();
        em.clear();

        // full column parity read-back
        EquipmentWarranty w = warrantyRepository.findById(amc.getId()).orElseThrow();
        assertThat(w.getEquipmentId()).isEqualTo(asset.getId());
        assertThat(w.getWarrantyType()).isEqualTo("AMC");
        assertThat(w.getVendor()).isEqualTo("Acme Medical Ltd");
        assertThat(w.getTerms()).isEqualTo("Comprehensive cover incl. spares & labour");
        assertThat(w.getStartDate()).isEqualTo(Timestamp.from(today.minusDays(355)
                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
        assertThat(w.getEndDate()).isEqualTo(Timestamp.from(today.plusDays(10)
                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
        assertThat(w.getStatus()).isTrue();          // audit block defaults
        assertThat(w.getSynchronizedd()).isFalse();
        assertThat(w.getUnitId()).isEqualTo(7L);

        // expiry window [today, today+30): catches the AMC cover, skips lapsed + open-ended
        Timestamp from = Timestamp.from(today.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        Timestamp to = Timestamp.from(today.plusDays(31).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        List<EquipmentWarranty> expiring = warrantyRepository.findExpiring(from, to, 0L, false, 0L, false);
        assertThat(expiring).extracting(EquipmentWarranty::getId).containsExactly(amc.getId());

        // unit gate keeps the other unit's rows out even inside its own window
        List<EquipmentWarranty> unit7 = warrantyRepository.findExpiring(from, to, 7L, true, 0L, false);
        assertThat(unit7).extracting(EquipmentWarranty::getId).containsExactly(amc.getId());

        // per-asset history — newest first
        List<EquipmentWarranty> byAsset = warrantyRepository
                .findByEquipmentIdAndStatusTrueOrderByIdDesc(asset.getId());
        assertThat(byAsset).extracting(EquipmentWarranty::getId)
                .containsExactly(openEnded.getId(), lapsed.getId(), amc.getId());

        // gated counts (same queries WarrantyService.counts uses)
        assertThat(warrantyRepository.countActive(0L, false)).isEqualTo(3);
        assertThat(warrantyRepository.countActive(7L, true)).isEqualTo(2);
        assertThat(warrantyRepository.countActive(99L, true)).isZero();
        assertThat(warrantyRepository.countExpiredBefore(from, 0L, false)).isEqualTo(1);
        assertThat(warrantyRepository.countExpiringBetween(from, to, 0L, false)).isEqualTo(1);
        Map<String, Long> byType = new java.util.LinkedHashMap<>();
        for (Object[] row : warrantyRepository.countActiveGroupedByType(0L, false)) {
            byType.put((String) row[0], ((Number) row[1]).longValue());
        }
        assertThat(byType).containsEntry("AMC", 1L).containsEntry("WARRANTY", 1L).containsEntry("CMC", 1L);
    }

    @Test
    void docsEntity_persistReadBack_softDelete_onRealColumns() throws Exception {
        Equipment asset = service.register(mapper.readTree(
                "{\"itemId\":931,\"accessionNo\":\"ACC-DOC\",\"departmentId\":10}"), 7L);
        em.clear();

        // ---- raw persistence: full column parity on real V1 columns
        EquipmentDetailDocs manual = new EquipmentDetailDocs();
        manual.setId(nextVal("ihms_equipment.equipment_detail_docs_seq"));
        manual.setEquipmentId(asset.getId());
        manual.setDocType("MANUAL");
        manual.setFileName("user-manual.pdf");
        manual.setFilePath("/dms/eq/manual.pdf");
        manual.setDescription("OEM operation manual rev 4 — includes calibration chapter");  // TEXT round-trip
        manual.setUnitId(7L);

        EquipmentDetailDocs cert = new EquipmentDetailDocs();      // minimal row: only FK mandatory
        cert.setId(nextVal("ihms_equipment.equipment_detail_docs_seq"));
        cert.setEquipmentId(asset.getId());
        cert.setUnitId(7L);
        // Committed in their OWN tx (REQUIRES_NEW, same propagation as service
        // writes) so every later service call sees production-like committed rows
        // instead of outer-test-tx state.
        org.springframework.transaction.support.TransactionTemplate seedTx =
                new org.springframework.transaction.support.TransactionTemplate(txManager);
        seedTx.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        seedTx.executeWithoutResult(status -> {
            docsRepository.save(manual);
            docsRepository.save(cert);
        });
        em.clear();

        EquipmentDetailDocs readBack = docsRepository.findById(manual.getId()).orElseThrow();
        assertThat(readBack.getEquipmentId()).isEqualTo(asset.getId());
        assertThat(readBack.getDocType()).isEqualTo("MANUAL");
        assertThat(readBack.getFileName()).isEqualTo("user-manual.pdf");
        assertThat(readBack.getFilePath()).isEqualTo("/dms/eq/manual.pdf");
        assertThat(readBack.getDescription())
                .isEqualTo("OEM operation manual rev 4 — includes calibration chapter");
        assertThat(readBack.getStatus()).isTrue();                 // audit defaults
        assertThat(readBack.getSynchronizedd()).isFalse();
        assertThat(readBack.getUnitId()).isEqualTo(7L);
        // newest-first listing sees raw rows + nothing else for this fresh asset
        assertThat(docsRepository.findByEquipmentIdAndStatusTrueOrderByIdDesc(asset.getId()))
                .extracting(EquipmentDetailDocs::getId)
                .containsExactly(cert.getId(), manual.getId());

        // ---- committed fixture flips so the service's REQUIRES_NEW tx sees them:
        // one CONDEMNED asset (documents still allowed) and one inactive (rejected)
        Equipment condemnedAsset = service.register(mapper.readTree(
                "{\"itemId\":932,\"accessionNo\":\"ACC-DOC-CD\",\"departmentId\":10}"), 7L);
        Equipment retiredAsset = service.register(mapper.readTree(
                "{\"itemId\":933,\"accessionNo\":\"ACC-DOC-RT\",\"departmentId\":10}"), 7L);
        org.springframework.transaction.support.TransactionTemplate tt =
                new org.springframework.transaction.support.TransactionTemplate(txManager);
        tt.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tt.executeWithoutResult(status -> {
            Equipment cd = equipmentRepository.findById(condemnedAsset.getId()).orElseThrow();
            cd.setWorkingStatus(EquipmentWorkingStatus.CONDEMNED);
            equipmentRepository.save(cd);
            Equipment rt = equipmentRepository.findById(retiredAsset.getId()).orElseThrow();
            rt.setStatus(false);
            equipmentRepository.save(rt);
        });
        em.clear();

        // ---- service upload on real committed rows: trim + server-owned pairing
        EquipmentDetailDocs uploaded = documentService.upload(asset.getId(), mapper.readTree(
                "{\"fileName\":\" condemnation-report.pdf \",\"docType\":\"CONDEMNATION_REPORT\","
                        + "\"filePath\":\"/dms/eq/condemn.pdf\",\"description\":\"Board approval minute 44/2\","
                        + "\"equipmentId\":999}"), 7L);
        em.clear();
        assertThat(uploaded.getId()).isNotNull();
        assertThat(uploaded.getEquipmentId()).isEqualTo(asset.getId());   // path wins over payload hijack
        assertThat(uploaded.getFileName()).isEqualTo("condemnation-report.pdf");   // trimmed
        assertThat(uploaded.getDocType()).isEqualTo("CONDEMNATION_REPORT");
        assertThat(uploaded.getStatus()).isTrue();

        // guards — never save: missing fileName, unknown asset, inactive asset
        assertThatThrownBy(() -> documentService.upload(asset.getId(),
                mapper.readTree("{\"docType\":\"MANUAL\"}"), 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class);
        assertThatThrownBy(() -> documentService.upload(99_999L,
                mapper.readTree("{\"fileName\":\"x.pdf\"}"), 7L))
                .isInstanceOf(EquipmentNotFoundException.class);
        assertThatThrownBy(() -> documentService.upload(retiredAsset.getId(),
                mapper.readTree("{\"fileName\":\"x.pdf\"}"), 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class);
        // condemned assets still accept documents (condemnation paperwork!)
        EquipmentDetailDocs condemnedDoc = documentService.upload(condemnedAsset.getId(), mapper.readTree(
                "{\"fileName\":\"disposal-form.pdf\",\"docType\":\"DISPOSAL\"}"), 7L);
        em.clear();
        assertThat(condemnedDoc.getEquipmentId()).isEqualTo(condemnedAsset.getId());

        // service listing newest-first across seeded + uploaded committed rows
        assertThat(documentService.listByEquipment(asset.getId()))
                .extracting(EquipmentDetailDocs::getId)
                .containsExactly(uploaded.getId(), cert.getId(), manual.getId());
        // reads work even for the inactive asset (existence-only guard)
        assertThat(documentService.listByEquipment(retiredAsset.getId())).isEmpty();

        // ---- soft delete: tombstone survives audit trail, repeats read as absent
        assertThatThrownBy(() -> documentService.delete(99_999L))
                .isInstanceOf(EquipmentNotFoundException.class);       // unknown id
        EquipmentDetailDocs tombstoned = documentService.delete(uploaded.getId());
        em.clear();
        assertThat(tombstoned.getStatus()).isFalse();
        assertThat(tombstoned.getUpdatedBy()).isNotNull();             // update stamps applied
        // tombstone hides ONLY that row; seeded docs still listed
        assertThat(documentService.listByEquipment(asset.getId()))
                .extracting(EquipmentDetailDocs::getId)
                .containsExactly(cert.getId(), manual.getId());
        assertThat(docsRepository.findByIdAndStatusTrue(uploaded.getId())).isEmpty();
        assertThat(docsRepository.findById(uploaded.getId())).isPresent();   // row still there
        assertThatThrownBy(() -> documentService.delete(uploaded.getId()))
                .isInstanceOf(EquipmentNotFoundException.class);       // repeat delete
        // untouched rows keep listing normally
        assertThat(documentService.listByEquipment(condemnedAsset.getId()))
                .extracting(EquipmentDetailDocs::getId)
                .containsExactly(condemnedDoc.getId());
    }

    /**
     * TAS-15 entity-column parity on real columns (ddl-auto=none proves nothing
     * at boot): persist/read-back of every equipment_usage_details column shape
     * (NUMERIC(8,2) -> BigDecimal, TEXT remarks), the report window semantics
     * [from, to) with NULL-usage_date exclusion + gated sentinels, and the
     * guarded log/history/report lifecycle on committed rows.
     */
    @Test
    void usageEntity_persistReadBack_reportWindow_onRealColumns() throws Exception {
        Equipment asset = service.register(mapper.readTree(
                "{\"itemId\":941,\"accessionNo\":\"ACC-USG\",\"departmentId\":10}"), 7L);
        em.clear();
        java.time.LocalDate today = java.time.LocalDate.now();
        long rowsBefore = count("ihms_equipment.equipment_usage_details");

        // ---- raw persistence: full column parity on real V1 columns
        EquipmentUsageDetails early = new EquipmentUsageDetails();   // outside default 30d window
        early.setId(nextVal("ihms_equipment.equipment_usage_details_seq"));
        early.setEquipmentId(asset.getId());
        early.setUsageDate(Timestamp.from(today.minusDays(40)
                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
        early.setHoursUsed(new BigDecimal("7.25"));                 // NUMERIC(8,2) round-trip
        early.setOperator("tech.a");
        early.setRemarks("baseline run before calibration — pre-service burn-in");   // TEXT round-trip
        early.setUnitId(7L);

        EquipmentUsageDetails mid = new EquipmentUsageDetails();     // inside window, no remarks
        mid.setId(nextVal("ihms_equipment.equipment_usage_details_seq"));
        mid.setEquipmentId(asset.getId());
        mid.setUsageDate(Timestamp.from(today.minusDays(5)
                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
        mid.setHoursUsed(new BigDecimal("3.50"));
        mid.setOperator("tech.b");
        mid.setUnitId(7L);

        EquipmentUsageDetails nowRow = new EquipmentUsageDetails();  // minimal: FK + date only (NULL hours/operator/remarks)
        nowRow.setId(nextVal("ihms_equipment.equipment_usage_details_seq"));
        nowRow.setEquipmentId(asset.getId());
        nowRow.setUsageDate(new Timestamp(System.currentTimeMillis()));
        nowRow.setUnitId(7L);

        EquipmentUsageDetails noDate = new EquipmentUsageDetails();  // NULL usage_date: in history, out of any window
        noDate.setId(nextVal("ihms_equipment.equipment_usage_details_seq"));
        noDate.setEquipmentId(asset.getId());
        noDate.setUnitId(7L);

        // Committed in their OWN tx (REQUIRES_NEW, same propagation as service
        // writes) so every later service call sees production-like committed rows
        // instead of outer-test-tx state.
        org.springframework.transaction.support.TransactionTemplate seedTx =
                new org.springframework.transaction.support.TransactionTemplate(txManager);
        seedTx.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        seedTx.executeWithoutResult(status -> {
            usageRepository.save(early);
            usageRepository.save(mid);
            usageRepository.save(nowRow);
            usageRepository.save(noDate);
        });
        em.clear();

        // full column parity read-back
        EquipmentUsageDetails u = usageRepository.findById(early.getId()).orElseThrow();
        assertThat(u.getEquipmentId()).isEqualTo(asset.getId());
        assertThat(u.getUsageDate()).isEqualTo(Timestamp.from(today.minusDays(40)
                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
        assertThat(u.getHoursUsed()).isEqualByComparingTo("7.25");
        assertThat(u.getOperator()).isEqualTo("tech.a");
        assertThat(u.getRemarks()).isEqualTo("baseline run before calibration — pre-service burn-in");
        assertThat(u.getStatus()).isTrue();          // audit block defaults
        assertThat(u.getSynchronizedd()).isFalse();
        assertThat(u.getUnitId()).isEqualTo(7L);
        assertThat(usageRepository.findById(nowRow.getId()).orElseThrow().getHoursUsed()).isNull();

        // report window [from, to): chronological; early(-40d) + NULL-date row excluded
        Timestamp from30 = Timestamp.from(today.minusDays(29)
                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        Timestamp toExcl = Timestamp.from(today.plusDays(1)
                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        assertThat(usageRepository.findReportRows(from30, toExcl, 0L, false, 0L, false))
                .extracting(EquipmentUsageDetails::getId)
                .containsExactly(mid.getId(), nowRow.getId());
        // gated sentinels both directions
        assertThat(usageRepository.findReportRows(from30, toExcl, 7L, true, 0L, false))
                .extracting(EquipmentUsageDetails::getId)
                .containsExactly(mid.getId(), nowRow.getId());
        assertThat(usageRepository.findReportRows(from30, toExcl, 99L, true, 0L, false)).isEmpty();
        assertThat(usageRepository.findReportRows(from30, toExcl, 0L, false, asset.getId(), true))
                .extracting(EquipmentUsageDetails::getId)
                .containsExactly(mid.getId(), nowRow.getId());

        // per-asset history newest-first — includes NULL-date + outside-window rows
        assertThat(usageRepository.findByEquipmentIdAndStatusTrueOrderByIdDesc(asset.getId()))
                .extracting(EquipmentUsageDetails::getId)
                .containsExactly(noDate.getId(), nowRow.getId(), mid.getId(), early.getId());

        // ---- committed fixture flips so the service's REQUIRES_NEW tx sees them:
        // one CONDEMNED asset and one inactive asset (both reject fresh usage)
        Equipment condemnedAsset = service.register(mapper.readTree(
                "{\"itemId\":942,\"accessionNo\":\"ACC-USG-CD\",\"departmentId\":10}"), 7L);
        Equipment retiredAsset = service.register(mapper.readTree(
                "{\"itemId\":943,\"accessionNo\":\"ACC-USG-RT\",\"departmentId\":10}"), 7L);
        org.springframework.transaction.support.TransactionTemplate tt =
                new org.springframework.transaction.support.TransactionTemplate(txManager);
        tt.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tt.executeWithoutResult(status -> {
            Equipment cd = equipmentRepository.findById(condemnedAsset.getId()).orElseThrow();
            cd.setWorkingStatus(EquipmentWorkingStatus.CONDEMNED);
            equipmentRepository.save(cd);
            Equipment rt = equipmentRepository.findById(retiredAsset.getId()).orElseThrow();
            rt.setStatus(false);
            equipmentRepository.save(rt);
        });
        em.clear();

        // ---- service log on real committed data: trims, blank->NULL remarks,
        // server-owned pairing, hours normalized to NUMERIC(8,2) scale
        EquipmentUsageDetails logged = usageService.log(asset.getId(), mapper.readTree(
                "{\"hoursUsed\":2.5,\"operator\":\" op.c \",\"remarks\":\"   \",\"equipmentId\":999}"), 7L);
        em.clear();
        assertThat(logged.getId()).isNotNull();
        assertThat(logged.getEquipmentId()).isEqualTo(asset.getId());   // path wins over payload hijack
        assertThat(logged.getHoursUsed()).isEqualByComparingTo("2.50"); // scale normalization
        assertThat(logged.getOperator()).isEqualTo("op.c");             // trimmed
        assertThat(logged.getRemarks()).isNull();                       // blank -> NULL
        assertThat(logged.getUsageDate()).isNotNull();                  // defaulted to now (key absent)
        assertThat(logged.getStatus()).isTrue();

        // guards — never save: bind failure, missing/zero/negative hours, unknown/inactive/condemned asset
        assertThatThrownBy(() -> usageService.log(asset.getId(),
                mapper.readTree("{\"hoursUsed\":\"abc\"}"), 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class);           // unbindable payload
        assertThatThrownBy(() -> usageService.log(asset.getId(),
                mapper.readTree("{\"operator\":\"x\"}"), 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class);           // hoursUsed required
        assertThatThrownBy(() -> usageService.log(asset.getId(),
                mapper.readTree("{\"hoursUsed\":0}"), 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class);           // must be positive
        assertThatThrownBy(() -> usageService.log(asset.getId(),
                mapper.readTree("{\"hoursUsed\":-3}"), 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class);
        assertThatThrownBy(() -> usageService.log(99_999L,
                mapper.readTree("{\"hoursUsed\":1}"), 7L))
                .isInstanceOf(EquipmentNotFoundException.class);                 // unknown asset
        assertThatThrownBy(() -> usageService.log(retiredAsset.getId(),
                mapper.readTree("{\"hoursUsed\":1}"), 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class);           // inactive
        assertThatThrownBy(() -> usageService.log(condemnedAsset.getId(),
                mapper.readTree("{\"hoursUsed\":1}"), 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class);           // condemned machines don't operate
        assertThat(count("ihms_equipment.equipment_usage_details")).isEqualTo(rowsBefore + 5);

        // history through the service — seeded + logged, newest first
        assertThat(usageService.history(asset.getId()))
                .extracting(EquipmentUsageDetails::getId)
                .containsExactly(logged.getId(), noDate.getId(), nowRow.getId(), mid.getId(), early.getId());
        assertThatThrownBy(() -> usageService.history(99_999L))
                .isInstanceOf(EquipmentNotFoundException.class);

        // ---- reports through the service on committed rows:
        // default trailing-30-days window: mid + nowRow + logged (early/noDate excluded)
        UsageReport def = usageService.report(null, null, 7L, null);
        assertThat(def.from()).isEqualTo(today.minusDays(29));       // effective bounds echoed
        assertThat(def.to()).isEqualTo(today);
        assertThat(def.entryCount()).isEqualTo(3);
        assertThat(def.totalHours()).isEqualByComparingTo("6.00");   // 3.50 + NULL skipped + 2.50
        assertThat(def.entries()).extracting(EquipmentUsageDetails::getId)
                .containsExactly(mid.getId(), nowRow.getId(), logged.getId());

        // explicit wide window per asset: everything incl. early (-40d)
        UsageReport all = usageService.report(today.minusDays(45), today, null, asset.getId());
        assertThat(all.from()).isEqualTo(today.minusDays(45));
        assertThat(all.to()).isEqualTo(today);
        assertThat(all.equipmentId()).isEqualTo(asset.getId());      // scope echoed
        assertThat(all.unitId()).isNull();
        assertThat(all.entryCount()).isEqualTo(4);                   // still no NULL-date row
        assertThat(all.totalHours()).isEqualByComparingTo("13.25");  // 7.25 + 3.50 + 2.50
        assertThat(all.entries()).extracting(EquipmentUsageDetails::getId)
                .containsExactly(early.getId(), mid.getId(), nowRow.getId(), logged.getId());

        // empty scope + inverted window guard
        UsageReport empty = usageService.report(null, null, 99L, null);
        assertThat(empty.entryCount()).isZero();
        assertThat(empty.totalHours()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThatThrownBy(() -> usageService.report(today, today.minusDays(1), null, null))
                .isInstanceOf(InvalidEquipmentRequestException.class);           // from after to
    }

    /**
     * Opening-balance register (Task 16) on real V1 columns: entity<->column
     * parity (NUMERIC(19,2) money, BIGINT quantity, TEXT remarks), gated
     * register queries, then the service lifecycle on committed fixture rows —
     * single add defaults/guards/trims, atomic bulk import (one bad row rolls
     * back the whole file) and the server-owned patch update.
     */
    @Test
    void openingBalanceEntity_persistReadBack_bulkImport_onRealColumns() throws Exception {
        // ---- raw persistence: full column parity on real V1 columns
        EquipmentOpeningBalance full = new EquipmentOpeningBalance();
        full.setId(nextVal("ihms_equipment.equipment_opening_balance_seq"));
        full.setItemId(771L);
        full.setOpeningBalance(25L);
        full.setRate(new BigDecimal("149.99"));                    // NUMERIC(19,2) round-trip
        full.setAmount(new BigDecimal("3749.75"));
        full.setAccessionNo("ACC-OB-1");
        full.setSerialNo("SN-OB-00025");
        full.setModel("Ventilator V500");
        full.setStoreId(5L);
        full.setSupplierId(9L);
        full.setRemarks("stock take at go-live — main OT store");  // TEXT round-trip
        full.setUnitId(7L);

        EquipmentOpeningBalance minimal = new EquipmentOpeningBalance();   // NOT NULL qty set explicitly:
        minimal.setId(nextVal("ihms_equipment.equipment_opening_balance_seq")); // raw JPA has no DEFAULT —
        minimal.setItemId(772L);                                   // the service supplies it on key-absence
        minimal.setOpeningBalance(0L);
        minimal.setUnitId(7L);

        EquipmentOpeningBalance nullUnit = new EquipmentOpeningBalance();  // NULL unit: excluded from
        nullUnit.setId(nextVal("ihms_equipment.equipment_opening_balance_seq")); // unit-scoped listings
        nullUnit.setItemId(773L);
        nullUnit.setOpeningBalance(4L);

        EquipmentOpeningBalance retired = new EquipmentOpeningBalance();   // inactive: never listed
        retired.setId(nextVal("ihms_equipment.equipment_opening_balance_seq"));
        retired.setItemId(774L);
        retired.setOpeningBalance(9L);
        retired.setStatus(false);
        retired.setUnitId(7L);

        // Committed in their OWN tx (REQUIRES_NEW, same propagation as service
        // writes) so every later service call sees production-like committed rows
        org.springframework.transaction.support.TransactionTemplate seedTx =
                new org.springframework.transaction.support.TransactionTemplate(txManager);
        seedTx.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        seedTx.executeWithoutResult(status -> {
            balanceRepository.save(full);
            balanceRepository.save(minimal);
            balanceRepository.save(nullUnit);
            balanceRepository.save(retired);
        });
        em.clear();

        // full column parity read-back
        EquipmentOpeningBalance b = balanceRepository.findById(full.getId()).orElseThrow();
        assertThat(b.getItemId()).isEqualTo(771L);
        assertThat(b.getOpeningBalance()).isEqualTo(25L);
        assertThat(b.getRate()).isEqualByComparingTo("149.99");
        assertThat(b.getAmount()).isEqualByComparingTo("3749.75");
        assertThat(b.getAccessionNo()).isEqualTo("ACC-OB-1");
        assertThat(b.getModel()).isEqualTo("Ventilator V500");
        assertThat(b.getStoreId()).isEqualTo(5L);
        assertThat(b.getSupplierId()).isEqualTo(9L);
        assertThat(b.getRemarks()).isEqualTo("stock take at go-live — main OT store");
        assertThat(b.getStatus()).isTrue();          // audit block defaults
        assertThat(b.getSynchronizedd()).isFalse();
        assertThat(b.getUnitId()).isEqualTo(7L);
        assertThat(balanceRepository.findById(minimal.getId()).orElseThrow().getRate()).isNull();

        // register query: active rows grouped per item; inactive + NULL-unit handling
        assertThat(balanceRepository.findRegisterRows(0L, false, 0L, false))
                .extracting(EquipmentOpeningBalance::getId)
                .containsExactly(full.getId(), minimal.getId(), nullUnit.getId());   // itemId ASC order
        assertThat(balanceRepository.findRegisterRows(7L, true, 0L, false))
                .extracting(EquipmentOpeningBalance::getId)
                .containsExactly(full.getId(), minimal.getId());                     // NULL unit excluded
        assertThat(balanceRepository.findRegisterRows(99L, true, 0L, false)).isEmpty();
        assertThat(balanceRepository.findRegisterRows(0L, false, 772L, true))
                .extracting(EquipmentOpeningBalance::getId)
                .containsExactly(minimal.getId());

        // ---- service add on real committed data: seq id, hijack-proof pairing,
        // key-absence defaulting, money scale normalization, audit block, trims
        long rowsBefore = count("ihms_equipment.equipment_opening_balance");
        EquipmentOpeningBalance added = balanceService.add(mapper.readTree(
                "{\"itemId\":780,\"rate\":12.5,\"accessionNo\":\" ACC-X \",\"serialNo\":\"   \","
                        + "\"remarks\":\" counted twice \",\"id\":999,\"unitId\":42}"), 7L);
        em.clear();
        assertThat(added.getId()).isNotNull().isNotEqualTo(999L);       // server-owned id from sequence
        assertThat(added.getItemId()).isEqualTo(780L);
        assertThat(added.getOpeningBalance()).isZero();                 // key absent -> column DEFAULT 0
        assertThat(added.getRate()).isEqualByComparingTo("12.50");      // NUMERIC(19,2) scale
        assertThat(added.getAccessionNo()).isEqualTo("ACC-X");          // trimmed
        assertThat(added.getSerialNo()).isNull();                       // blank -> NULL
        assertThat(added.getRemarks()).isEqualTo("counted twice");
        assertThat(added.getUnitId()).isEqualTo(7L);                    // path/query wins over payload hijack
        assertThat(added.getStatus()).isTrue();
        assertThat(added.getAddedBy()).isEqualTo(1L);
        assertThat(added.getCreatedUnitId()).isEqualTo(7L);
        assertThat(added.getSynchronizedd()).isFalse();

        // guards — never save (row-count invariant across every rejection)
        assertThatThrownBy(() -> balanceService.add(null, 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class);
        assertThatThrownBy(() -> balanceService.add(mapper.readTree("{\"openingBalance\":3}"), 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class);   // missing itemId
        assertThatThrownBy(() -> balanceService.add(mapper.readTree("{\"itemId\":781,\"openingBalance\":-1}"), 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class);   // negative quantity
        assertThatThrownBy(() -> balanceService.add(mapper.readTree("{\"itemId\":781,\"rate\":-5}"), 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class);   // negative money
        assertThatThrownBy(() -> balanceService.add(mapper.readTree(
                "{\"itemId\":781,\"amount\":1000000000000000000}"), 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class);   // exceeds NUMERIC(19,2)
        assertThatThrownBy(() -> balanceService.add(mapper.readTree(
                "{\"itemId\":781,\"rate\":\"not-a-number\"}"), 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class);   // bind failure before save
        assertThat(count("ihms_equipment.equipment_opening_balance")).isEqualTo(rowsBefore + 1);

        // ---- bulk import: array body -> many rows, one REQUIRES_NEW tx
        List<EquipmentOpeningBalance> imported = balanceService.importBulk(mapper.readTree(
                "[{\"itemId\":790,\"openingBalance\":10,\"rate\":99.99},"
                        + "{\"itemId\":791,\"openingBalance\":2,\"amount\":500}]"), 7L);
        em.clear();
        assertThat(imported).extracting(EquipmentOpeningBalance::getItemId)
                .containsExactly(790L, 791L);
        assertThat(imported).extracting(EquipmentOpeningBalance::getUnitId)
                .containsOnly(7L);                                       // import inherits the scope
        assertThat(imported.get(0).getRate()).isEqualByComparingTo("99.99");
        long afterImport = count("ihms_equipment.equipment_opening_balance");
        assertThat(afterImport).isEqualTo(rowsBefore + 3);

        // atomicity proof: one valid row + one bad row -> BEXA003 and NEITHER persisted
        assertThatThrownBy(() -> balanceService.importBulk(mapper.readTree(
                "[{\"itemId\":792,\"openingBalance\":6},{\"openingBalance\":6}]"), 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class);
        assertThat(count("ihms_equipment.equipment_opening_balance")).isEqualTo(afterImport);
        assertThatThrownBy(() -> balanceService.importBulk(mapper.readTree("[]"), 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class);   // empty file
        assertThatThrownBy(() -> balanceService.importBulk(mapper.readTree(
                "{\"itemId\":793}"), 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class);   // non-array body

        // ---- update: null-safe patch + server-owned skip-list
        EquipmentOpeningBalance patched = balanceService.update(full.getId(), mapper.readTree(
                "{\"id\":888,\"itemId\":999,\"unitId\":42,\"createdUnitId\":42,\"updatedUnitId\":42,"
                        + "\"addedBy\":42,\"updatedBy\":42,\"synchronizedd\":true,"
                        + "\"openingBalance\":30,\"rate\":\"199.99\",\"remarks\":\"recounted after audit\"}"));
        em.clear();
        assertThat(patched.getOpeningBalance()).isEqualTo(30L);         // business keys patch
        assertThat(patched.getRate()).isEqualByComparingTo("199.99");
        assertThat(patched.getRemarks()).isEqualTo("recounted after audit");
        assertThat(patched.getAmount()).isEqualByComparingTo("3749.75"); // untouched by patch
        assertThat(patched.getItemId()).isEqualTo(771L);                 // pairing key NOT re-pointed
        assertThat(patched.getUnitId()).isEqualTo(7L);                   // tenant block intact
        assertThat(patched.getCreatedUnitId()).isNull();                 // hijack "createdUnitId":42 neutralized
                                                                        // (raw seed row carries no create-stamp)
        assertThat(patched.getAddedDatetime()).isEqualTo(full.getAddedDatetime()); // untouched (NULL pre and post)
        assertThat(patched.getUpdatedBy()).isEqualTo(1L);                // update stamped
        assertThat(patched.getUpdatedDatetime()).isNotNull();

        assertThatThrownBy(() -> balanceService.update(987654L, mapper.readTree("{\"openingBalance\":1}")))
                .isInstanceOf(EquipmentNotFoundException.class);          // unknown id
        assertThatThrownBy(() -> balanceService.update(full.getId(), null))
                .isInstanceOf(InvalidEquipmentRequestException.class);   // empty payload
    }

    // ------------------------------------------------- master data (Task 17)

    /**
     * Generic master-data CRUD on real PG columns: registry routing (incl.
     * case-insensitive masterType + unknown-type rejection), server-owned ids
     * drawn from each table's own sequence, hijack-proof create/update,
     * whitelist search + unit/status gates, soft delete and the
     * equipment_checklist.quantity column default on key-absence.
     */
    @Test
    void masterData_genericCrud_onRealColumns() throws Exception {
        // isolate the two UNSEEDED master tables this scenario writes to —
        // v1Schema_applies counts only the seeded dictionaries, so truncating
        // these keeps every test order-safe (rows commit in REQUIRES_NEW txs).
        org.springframework.transaction.support.TransactionTemplate setupTx =
                new org.springframework.transaction.support.TransactionTemplate(txManager);
        setupTx.setPropagationBehavior(
                org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        setupTx.executeWithoutResult(status -> em.createNativeQuery(
                        "TRUNCATE ihms_equipment.complaint_category_master, " +
                                "ihms_equipment.equipment_checklist")
                .executeUpdate());

        // ---- supplied id is rejected outright (reference-service F-3 hardening:
        // a client id could overwrite an existing dictionary row via merge) —
        // 400 BEXA003 BEFORE any sequence burn or save
        long beforeIdHijack = count("ihms_equipment.complaint_category_master");
        assertThatThrownBy(() -> masterService.create("complaint-category",
                mapper.readTree("{\"id\":999,\"code\":\"MD-HIJACK\"," +
                        "\"description\":\"hijack attempt\"}"), 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class)
                .hasMessageContaining("id is server-assigned");
        assertThat(count("ihms_equipment.complaint_category_master")).isEqualTo(beforeIdHijack);

        // ---- create A: unitId hijack neutralized, trims, status default, audit block
        ComplaintCategoryMaster catA = (ComplaintCategoryMaster) masterService.create("complaint-category",
                mapper.readTree("{\"code\":\" MD-IT-A \",\"description\":\"Integration category A\","
                        + "\"unitId\":42}"), 7L);
        long idA = catA.getId();
        assertThat(idA).isPositive();                          // id burned from complaint_category_master_seq
        assertThat(catA.getCode()).isEqualTo("MD-IT-A");       // trimmed
        assertThat(catA.getDescription()).isEqualTo("Integration category A");
        assertThat(catA.getStatus()).isTrue();                 // defaulted active on key-absence
        assertThat(catA.getUnitId()).isEqualTo(7L);            // payload "unitId":42 hijack ignored
        assertThat(catA.getCreatedUnitId()).isEqualTo(7L);
        assertThat(catA.getUpdatedUnitId()).isEqualTo(7L);
        assertThat(catA.getAddedBy()).isEqualTo(1L);
        assertThat(catA.getAddedDatetime()).isNotNull();
        assertThat(catA.getUpdatedBy()).isNull();              // create stamps only added_*
        assertThat(catA.getSynchronizedd()).isFalse();

        // ---- create B: masterType is case-insensitive; no unit scope -> NULL tenant columns
        ComplaintCategoryMaster catB = (ComplaintCategoryMaster) masterService.create("COMPLAINT-CATEGORY",
                mapper.readTree("{\"code\":\"MD-IT-B\",\"description\":\"Integration category B\","
                        + "\"status\":false}"), null);
        long idB = catB.getId();
        assertThat(idB).isGreaterThan(idA);                    // same sequence, monotonic
        assertThat(catB.getStatus()).isFalse();                // explicit status honored (seed-inactive parity)
        assertThat(catB.getUnitId()).isNull();
        assertThat(catB.getCreatedUnitId()).isNull();

        em.clear();                                            // drop managed snapshots before re-reads

        // ---- get parity by id
        EquipmentBase fetched = masterService.get("complaint-category", idA);
        assertThat(fetched).isInstanceOf(ComplaintCategoryMaster.class);
        assertThat(((ComplaintCategoryMaster) fetched).getCode()).isEqualTo("MD-IT-A");

        // ---- listing ordered by id (seed order), gates and whitelist search
        List<EquipmentBase> all = masterService.list("complaint-category", null, null, null);
        assertThat(all).extracting(EquipmentBase::getId).containsSubsequence(idA, idB);
        assertThat(masterService.list("complaint-category", null, true, null))   // active only
                .extracting(EquipmentBase::getId).containsExactly(idA);
        assertThat(masterService.list("complaint-category", null, false, null))  // deactivated only
                .extracting(EquipmentBase::getId).containsExactly(idB);
        assertThat(masterService.list("complaint-category", 7L, null, null))     // unit gate keeps scoped row
                .extracting(EquipmentBase::getId).containsExactly(idA);
        assertThat(masterService.list("complaint-category", 99L, null, null)).isEmpty();
        assertThat(masterService.list("complaint-category", null, null, "md-it-b")) // LOWER(code) LIKE
                .extracting(EquipmentBase::getId).containsExactly(idB);
        assertThat(masterService.list("complaint-category", null, null, "no-such-term")).isEmpty();

        // ---- update: business patch applies, full server-owned hijack attack neutralized
        EquipmentBase patched = masterService.update("complaint-category", idB, mapper.readTree(
                "{\"id\":777,\"code\":null,\"description\":\"Renamed category B\","
                        + "\"unitId\":43,\"createdUnitId\":43,\"updatedUnitId\":43,"
                        + "\"addedBy\":99,\"addedDatetime\":\"2000-01-01T00:00:00Z\","
                        + "\"updatedBy\":88,\"updatedDatetime\":\"2000-01-01T00:00:00Z\","
                        + "\"synchronizedd\":true}"));
        ComplaintCategoryMaster asB = (ComplaintCategoryMaster) patched;
        assertThat(asB.getDescription()).isEqualTo("Renamed category B");   // business field patched
        assertThat(asB.getCode()).isEqualTo("MD-IT-B");                     // null patch ignored
        assertThat(asB.getId()).isEqualTo(idB);                             // id NOT re-pointed
        assertThat(asB.getUnitId()).isNull();                               // tenant block intact
        assertThat(asB.getCreatedUnitId()).isNull();                        // hijack "createdUnitId":43 neutralized
        assertThat(asB.getAddedBy()).isEqualTo(1L);                         // create audit untouched
        assertThat(asB.getSynchronizedd()).isFalse();                       // sync flag not client-settable
        assertThat(asB.getUpdatedBy()).isEqualTo(1L);                       // update stamped
        assertThat(asB.getUpdatedDatetime()).isNotNull();
        assertThat(asB.getStatus()).isFalse();  // REGRESSION GUARD: payload omits status ->
                                                // NOT flipped active (EquipmentBase initializer
                                                // must never leak through a business patch)
        em.clear();

        // ---- status is patchable ONLY when the payload carries the key:
        // an explicit "status":true reactivates the seeded-inactive row ...
        EquipmentBase reactivated = masterService.update("complaint-category", idB,
                mapper.readTree("{\"status\":true,\"description\":\"Renamed category B\"}"));
        assertThat(reactivated.getStatus()).isTrue();
        em.clear();
        assertThatThrownBy(() -> masterService.update("complaint-category", 987654L,
                mapper.readTree("{\"description\":\"x\"}")))
                .isInstanceOf(EquipmentNotFoundException.class);             // unknown id -> 404

        // ---- delete is a soft tombstone: dictionary rows stay referentially intact
        EquipmentBase tombstoned = masterService.deactivate("complaint-category", idA);
        assertThat(tombstoned.getStatus()).isFalse();
        em.clear();
        assertThat(masterService.get("complaint-category", idA).getStatus()).isFalse(); // persisted flip
        assertThat(masterService.list("complaint-category", null, true, null))   // only reactivated B active
                .extracting(EquipmentBase::getId).containsExactly(idB);
        assertThat(masterService.list("complaint-category", null, false, null))  // A tombstoned, B active again
                .extracting(EquipmentBase::getId).containsExactly(idA);

        // ---- required-field validation BEFORE any save (row count invariant)
        long beforeInvalid = count("ihms_equipment.complaint_category_master");
        assertThatThrownBy(() -> masterService.create("complaint-category",
                mapper.readTree("{\"description\":\"no code given\"}"), 7L))
                .isInstanceOf(InvalidEquipmentRequestException.class)
                .hasMessageContaining("Missing required field 'code'");
        assertThat(count("ihms_equipment.complaint_category_master")).isEqualTo(beforeInvalid);

        // ---- registry routing: unknown type -> BEXA003 naming the supported set
        assertThatThrownBy(() -> masterService.list("no-such-master", null, null, null))
                .isInstanceOf(InvalidEquipmentRequestException.class)
                .hasMessageContaining("Unknown master type")
                .hasMessageContaining("amc-agent");

        // ---- equipment_checklist.quantity column DEFAULT rides key-absence (note #17)
        EquipmentChecklist chkDefaulted = (EquipmentChecklist) masterService.create("equipment-checklist",
                mapper.readTree("{\"itemId\":501,\"serialNo\":\"SN-MD-1\"," +
                        "\"description\":\"Calibration weight set\"}"), 7L);
        assertThat(chkDefaulted.getQuantity()).isEqualTo(1L);               // column DEFAULT 1 applied in service
        EquipmentChecklist chkExplicit = (EquipmentChecklist) masterService.create("equipment-checklist",
                mapper.readTree("{\"itemId\":502,\"quantity\":4," +
                        "\"partNo\":\"PN-9\",\"description\":\"Spare probe\"}"), 7L);
        assertThat(chkExplicit.getQuantity()).isEqualTo(4L);                // explicit value honored

        // ---- per-table sequence draws are monotonic (server-owned ids)
        long drawOne = masterRepository.nextId("complaint_category_master_seq");
        long drawTwo = masterRepository.nextId("complaint_category_master_seq");
        assertThat(drawTwo).isGreaterThan(drawOne);
    }

    private EqpPmSchedule scheduleFor(Long equipmentId, String code, Timestamp nextDue, Long unitId) {
        EqpPmSchedule s = new EqpPmSchedule();
        s.setId(nextVal("ihms_equipment.eqp_pm_schedule_seq"));
        s.setEquipmentId(equipmentId);
        s.setScheduleCode(code);
        s.setNextDueDate(nextDue);
        s.setUnitId(unitId);
        return s;
    }

    private long nextVal(String sequence) {
        return ((Number) em.createNativeQuery("SELECT nextval('" + sequence + "')").getSingleResult()).longValue();
    }

    private long count(String table) {
        return ((Number) em.createNativeQuery("SELECT count(*) FROM " + table).getSingleResult()).longValue();
    }
}
