package com.hims.reporting.application;

import com.hims.reporting.application.exceptions.InvalidReportingRequestException;
import com.hims.reporting.application.exceptions.ReportingConflictException;
import com.hims.reporting.application.exceptions.ReportingNotFoundException;
import com.hims.reporting.domain.model.ReportExecution;
import com.hims.reporting.domain.model.ReportMaster;
import com.hims.reporting.domain.repository.IngestedEventRepository;
import com.hims.reporting.domain.repository.RmEncounterStatsRepository;
import com.hims.reporting.domain.repository.RmPatientRegistryRepository;
import com.hims.reporting.domain.repository.RmOpStatsRepository;
import com.hims.reporting.domain.repository.RmRevenueDailyRepository;
import com.hims.reporting.domain.repository.ReportExecutionRepository;
import com.hims.reporting.domain.repository.ReportMasterRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L0 — report master/execute engine + dashboard counts, no Spring context.
 * Register WP-T2 assertions pinned here:
 *   - create: reportCode/title required, kind IN (1,2), definition whitelist-
 *     validated against rm_* tables/columns/aggregate; dup code -> 409
 *     REPORTING_DUPLICATE_REPORT_CODE
 *   - execute happy path (TABULAR rows + SUMMARY aggregate) and unknown-code 404
 *   - dashboard counts {patients, encounters, revenueTotal} from the read models
 *   - EVENT POLICY structural assertion: no emitter in the dependency graph.
 */
class ReportEngineServiceTest {

    private ReportMasterRepository masterRepository;
    private ReportExecutionRepository executionRepository;
    private RmPatientRegistryRepository patientRepository;
    private RmEncounterStatsRepository encounterRepository;
    private RmRevenueDailyRepository revenueRepository;
    private RmOpStatsRepository opStatsRepository;
    private EntityManager em;
    private ReportEngineService engine;
    private final AtomicLong seq = new AtomicLong(100);

    @BeforeEach
    void setUp() {
        masterRepository = mock(ReportMasterRepository.class);
        executionRepository = mock(ReportExecutionRepository.class);
        patientRepository = mock(RmPatientRegistryRepository.class);
        encounterRepository = mock(RmEncounterStatsRepository.class);
        revenueRepository = mock(RmRevenueDailyRepository.class);
        opStatsRepository = mock(RmOpStatsRepository.class);
        em = mock(EntityManager.class);
        engine = new ReportEngineService(masterRepository, executionRepository,
                patientRepository, encounterRepository, revenueRepository, opStatsRepository);
        ReflectionTestUtils.setField(engine, "em", em);

        Query seqQuery = mock(Query.class);
        when(seqQuery.getSingleResult()).thenAnswer(inv -> seq.incrementAndGet());
        when(em.createNativeQuery(contains("nextval"))).thenReturn(seqQuery);

        when(masterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(executionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(patientRepository.count()).thenReturn(2L);
        when(encounterRepository.totalCnt(any())).thenReturn(5L);
        when(revenueRepository.totalRevenue(any())).thenReturn(new BigDecimal("12345.67"));
    }

    // ------------------------------------------------------------- create guards

    @Test
    void create_requiresCodeTitleKind_bexa003() {
        Map<String, Object> def = Map.of("table", "rm_patient_registry",
                "columns", List.of("patientId"));
        assertThatThrownBy(() -> engine.create(null, "T", (short) 1, def))
                .hasMessageContaining("reportCode");
        assertThatThrownBy(() -> engine.create("R-1", " ", (short) 1, def))
                .hasMessageContaining("title");
        assertThatThrownBy(() -> engine.create("R-1", "T", (short) 3, def))
                .hasMessageContaining("kind must be 1");
        assertThatThrownBy(() -> engine.create("R-1", "T", null, def))
                .hasMessageContaining("kind must be 1");
        assertThatThrownBy(() -> engine.create("R-1", "T", (short) 1, null))
                .hasMessageContaining("definition is required");
    }

    @Test
    void create_validatesDefinitionAgainstReadModelWhitelist() {
        assertThatThrownBy(() -> engine.create("R-BAD-TABLE", "T", (short) 1,
                Map.of("table", "t_secret_table", "columns", List.of("x"))))
                .isInstanceOf(InvalidReportingRequestException.class)
                .extracting(e -> ((InvalidReportingRequestException) e).getErrorCode())
                .isEqualTo("REPORTING_INVALID_DEFINITION");

        assertThatThrownBy(() -> engine.create("R-BAD-COL", "T", (short) 1,
                Map.of("table", "rm_revenue_daily", "columns", List.of("password_hash"))))
                .isInstanceOf(InvalidReportingRequestException.class)
                .extracting(e -> ((InvalidReportingRequestException) e).getErrorCode())
                .isEqualTo("REPORTING_INVALID_DEFINITION");

        assertThatThrownBy(() -> engine.create("R-BAD-AGG", "T", (short) 2,
                Map.of("table", "rm_encounter_stats", "aggregate", "SUM(cnt)")))
                .isInstanceOf(InvalidReportingRequestException.class)
                .extracting(e -> ((InvalidReportingRequestException) e).getErrorCode())
                .isEqualTo("REPORTING_INVALID_DEFINITION");
    }

    @Test
    void create_duplicateCode_mapsTo409WithCode() {
        Map<String, Object> def = Map.of("table", "rm_patient_registry",
                "columns", List.of("patientId"));
        engine.create("R-DUP", "First", (short) 1, def);
        when(masterRepository.existsByReportCode("R-DUP")).thenReturn(true);

        assertThatThrownBy(() -> engine.create("R-DUP", "Second", (short) 1, def))
                .isInstanceOf(ReportingConflictException.class)
                .extracting(e -> ((ReportingConflictException) e).getErrorCode())
                .isEqualTo("REPORTING_DUPLICATE_REPORT_CODE");
    }

    @Test
    void create_happy_stampsKindAndActiveDefault() {
        ReportMaster created = engine.create("PATIENT-LIST", "Patient registry list",
                (short) ReportMaster.KIND_TABULAR,
                Map.of("table", "rm_patient_registry",
                       "columns", List.of("patientId", "mrn", "name"),
                       "dateField", "registeredAt"));
        assertThat(created.getReportId()).isNotNull();
        assertThat(created.getKind()).isEqualTo((short) 1);
        assertThat(created.getIsActive()).isTrue();
        assertThat(created.getDefinition()).contains("\"patientId\"");
    }

    // ------------------------------------------------------------- execute

    private void seedMaster(String code, short kind, String definitionJson) {
        ReportMaster master = new ReportMaster();
        master.setReportId(10L);
        master.setReportCode(code);
        master.setTitle("T");
        master.setKind(kind);
        master.setDefinition(definitionJson);
        master.setIsActive(true);
        when(masterRepository.findByReportCode(code)).thenReturn(Optional.of(master));
    }

    @Test
    void execute_unknownCode_mapsTo404() {
        when(masterRepository.findByReportCode("NOPE")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> engine.execute("NOPE", Map.of()))
                .isInstanceOf(ReportingNotFoundException.class)
                .extracting(e -> ((ReportingNotFoundException) e).getErrorCode())
                .isEqualTo("NOT_FOUND");
    }

    @Test
    void execute_tabular_runsWhitelistedSql_andStoresDoneExecution() {
        seedMaster("PATIENT-LIST", ReportMaster.KIND_TABULAR,
                "{\"table\":\"rm_patient_registry\",\"columns\":[\"patientId\",\"name\"],"
                        + "\"dateField\":\"registeredAt\"}");

        Query dataQuery = mock(Query.class);
        when(dataQuery.getResultList()).thenReturn(List.<Object[]>of(
                new Object[]{501L, "Ramesh"},
                new Object[]{502L, "Sunita"}));
        when(em.createNativeQuery(contains("SELECT patient_id, name FROM "
                + "ihms_reporting.rm_patient_registry"))).thenReturn(dataQuery);

        ReportExecution execution = engine.execute("PATIENT-LIST",
                Map.of("dateFrom", "2026-01-01"));

        assertThat(execution.getStatus()).isEqualTo(ReportExecution.STATUS_DONE);
        assertThat(execution.getRowCount()).isEqualTo(2);
        assertThat(execution.getResult()).contains("Ramesh").contains("patient_id");
        assertThat(execution.getExecutedAt()).isNotNull();

        verify(dataQuery).setParameter(1, java.time.LocalDate.of(2026, 1, 1));
    }

    @Test
    void execute_summary_returnsAggregateValueRow() {
        seedMaster("REVENUE-SUMMARY", ReportMaster.KIND_SUMMARY,
                "{\"table\":\"rm_revenue_daily\",\"aggregate\":\"total\"}");

        Query aggQuery = mock(Query.class);
        when(aggQuery.getSingleResult()).thenReturn(new BigDecimal("999.99"));
        when(em.createNativeQuery(contains(
                "COALESCE(SUM(total), 0) FROM ihms_reporting.rm_revenue_daily")))
                .thenReturn(aggQuery);

        ReportExecution execution = engine.execute("REVENUE-SUMMARY", Map.of());
        assertThat(execution.getRowCount()).isEqualTo(1);
        assertThat(execution.getResult()).contains("999.99");
        assertThat(execution.getStatus()).isEqualTo(ReportExecution.STATUS_DONE);
    }

    @Test
    void execute_invalidStoredDefinition_mapsTo400Not500() {
        seedMaster("BROKEN", ReportMaster.KIND_TABULAR, "{\"table\":\"rm_encounter_stats\"}");
        assertThatThrownBy(() -> engine.execute("BROKEN", Map.of()))
                .isInstanceOf(InvalidReportingRequestException.class)
                .hasMessageContaining("columns");
    }

    @Test
    void getExecution_unknownId_404() {
        when(executionRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> engine.getExecution(999L))
                .isInstanceOf(ReportingNotFoundException.class)
                .hasMessageContaining("execution not found");
    }

    // ------------------------------------------------------------- status parsing

    @Test
    void parseStatus_acceptsNumericAndNames_andRejectsGarbage() {
        assertThat(ReportEngineService.parseStatus(null)).isNull();
        assertThat(ReportEngineService.parseStatus("0"))
                .isEqualTo(ReportExecution.STATUS_QUEUED);
        assertThat(ReportEngineService.parseStatus("done"))
                .isEqualTo(ReportExecution.STATUS_DONE);
        assertThat(ReportEngineService.parseStatus("FAILED"))
                .isEqualTo(ReportExecution.STATUS_FAILED);
        assertThatThrownBy(() -> ReportEngineService.parseStatus("MAYBE"))
                .isInstanceOf(InvalidReportingRequestException.class)
                .extracting(e -> ((InvalidReportingRequestException) e).getErrorCode())
                .isEqualTo("BEXA003");
    }

    // ------------------------------------------------------------- dashboard

    @Test
    void dashboard_countsComeFromTheReadModels() {
        Map<String, Object> counts = engine.dashboardCounts(java.time.LocalDate.of(2026, 3, 31));
        assertThat(counts)
                .containsEntry("patients", 2L)
                .containsEntry("encounters", 5L)
                .containsEntry("revenueTotal", new BigDecimal("12345.67"));
        verify(encounterRepository).totalCnt(java.time.LocalDate.of(2026, 3, 31));
    }

    // ------------------------------------------------------------- event policy

    /**
     * NEGATIVE OUTBOX assertion at L0: report execution / dashboard flows touch NO
     * event infrastructure — ReportEngineService takes only read-model and report
     * repositories (no OutboxService/emitter exists in its constructor graph), so
     * no code path CAN produce an outbox row (L2 asserts the same on real PG16).
     */
    @Test
    void executeAndDashboard_emitsNothing_noEventInfrastructureTouched() {
        execute_tabular_runsWhitelistedSql_andStoresDoneExecution();
        engine.dashboardCounts(null);

        // only domain writes happened: exactly one execution save, zero outbox deps
        verify(executionRepository, times(1)).save(any(ReportExecution.class));
    }
}
