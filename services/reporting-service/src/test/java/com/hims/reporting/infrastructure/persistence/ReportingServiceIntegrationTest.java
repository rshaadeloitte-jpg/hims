package com.hims.reporting.infrastructure.persistence;

import com.hims.reporting.ReportingServiceApplication;
import com.hims.reporting.application.EventIngressService;
import com.hims.reporting.application.ReportEngineService;
import com.hims.reporting.application.exceptions.InvalidReportingRequestException;
import com.hims.reporting.application.exceptions.ReportingConflictException;
import com.hims.reporting.application.exceptions.ReportingNotFoundException;
import com.hims.reporting.domain.repository.IngestedEventRepository;
import com.hims.reporting.domain.model.RmPatientRegistry;
import com.hims.reporting.infrastructure.outbox.OutboxRepository;
import jakarta.persistence.EntityManager;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L2 — WP-T2 Reporting service on real PG16: V1 schema applies (ihms_reporting:
 * rm_patient_registry, rm_encounter_stats, rm_revenue_daily, t_report_master,
 * t_report_execution, t_ingested_event, outbox), the internal ingress feeds the
 * read models with STRICT idempotency (double-ingest => same totals — pinned),
 * the report engine executes TABULAR/SUMMARY definitions against the read models,
 * dashboard counts aggregate correctly and — per register WP-T2 EVENT POLICY —
 * the outbox stays EMPTY through EVERY flow (explicit NEGATIVE outbox assertion).
 * NOTE: requires Docker; skipped automatically when Docker is absent
 * (@Testcontainers(disabledWithoutDocker = true)) — do not remove.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = ReportingServiceApplication.class)
@DirtiesContext
@Transactional
class ReportingServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private EventIngressService ingress;

    @Autowired
    private ReportEngineService engine;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private IngestedEventRepository ledgerRepository;

    @Autowired
    private EntityManager em;

    @SuppressWarnings("unchecked")
    private List<String> tables() {
        return em.createNativeQuery("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'ihms_reporting' ORDER BY table_name
                """).getResultList();
    }

    /** Deterministic read after native upserts / entity saves. */
    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> rows(String sql) {
        flushAndClear();
        return (List<Object[]>) em.createNativeQuery(sql).getResultList();
    }

    @Test
    void flywaySchema_applies_allTablesSequencesPresent_outboxEmpty() {
        assertThat(tables()).contains("outbox", "rm_patient_registry", "rm_encounter_stats",
                "rm_revenue_daily", "t_report_master", "t_report_execution", "t_ingested_event");
        Number seqCount = (Number) em.createNativeQuery("""
                SELECT count(*) FROM information_schema.sequences
                WHERE sequence_schema = 'ihms_reporting'
                  AND sequence_name IN ('t_report_master_seq', 't_report_execution_seq',
                                        't_ingested_event_seq')
                """).getSingleResult();
        assertThat(seqCount.intValue()).isEqualTo(3);
        // EVENT POLICY: nothing has touched the outbox during migration/boot
        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void fullIngestFlow_strictIdempotency_withNegativeOutboxAssertion() {
        assertThat(outboxRepository.count()).isZero();

        // ---- routing guards before anything is written --------------------------
        assertThatThrownBy(() -> ingress.ingest("LabResultFinalized", Map.of("x", 1)))
                .isInstanceOf(InvalidReportingRequestException.class)
                .extracting(e -> ((InvalidReportingRequestException) e).getErrorCode())
                .isEqualTo("REPORTING_UNKNOWN_EVENT_TYPE");
        assertThatThrownBy(() -> ingress.ingest("InvoiceGenerated", Map.of("invoiceId", 1)))
                .isInstanceOf(InvalidReportingRequestException.class)
                .hasMessageContaining("amount");
        assertThat(outboxRepository.count()).isZero();   // rejected ingests emit nothing

        // ---- PatientRegistered: upsert semantics ---------------------------------
        var first = ingress.ingest("PatientRegistered", Map.of(
                "patientId", 501L, "mrn", "MRN-501", "name", "Ramesh",
                "registeredAt", "2026-01-05T09:30:00Z"));
        assertThat(first.status()).isEqualTo(EventIngressService.STATUS_APPLIED);

        var replay = ingress.ingest("PatientRegistered", Map.of(
                "patientId", 501L, "mrn", "MRN-501-UPDATED", "name", "Ramesh K"));
        assertThat(replay.status()).isEqualTo(EventIngressService.STATUS_DUPLICATE);
        flushAndClear();

        List<RmPatientRegistry> registry = em.createQuery(
                        "SELECT r FROM RmPatientRegistry r", RmPatientRegistry.class)
                .getResultList();
        assertThat(registry).hasSize(1);   // DOUBLE-INGEST => NO double count (pinned)
        assertThat(registry.get(0).getMrn()).isEqualTo("MRN-501");   // replay touched nothing

        assertThat(outboxRepository.count()).isZero();

        // ---- EncounterCreated: increments once per NEW encounter ------------------
        Map<String, Object> encounter = Map.of(
                "encounterId", 9001L, "statDate", "2026-02-01", "encounterType", "IPD");
        assertThat(ingress.ingest("EncounterCreated", encounter).status())
                .isEqualTo(EventIngressService.STATUS_APPLIED);
        assertThat(ingress.ingest("EncounterCreated", encounter).status())
                .isEqualTo(EventIngressService.STATUS_DUPLICATE);   // replay adds NOTHING

        // a second, genuinely new encounter on another day lands in its own bucket
        ingress.ingest("EncounterCreated", Map.of("encounterId", 9002L, "statDate", "2026-02-02"));
        flushAndClear();

        List<Object[]> stats = rows("""
                SELECT stat_date, encounter_type, cnt FROM ihms_reporting.rm_encounter_stats
                ORDER BY stat_date, encounter_type
                """);
        assertThat(stats).hasSize(2);
        assertThat(stats.get(0)[2]).isEqualTo(1);   // 2026-02-01 IPD cnt=1 despite replay
        assertThat(stats.get(1)[2]).isEqualTo(1);   // 2026-02-02 OPD (defaults applied)

        assertThat(outboxRepository.count()).isZero();

        // ---- InvoiceGenerated + PaymentReceived: accumulate by date+head ----------
        Map<String, Object> invoice = new java.util.HashMap<>(Map.of(
                "invoiceId", 7001L, "statDate", "2026-02-01",
                "headCode", "PHARMACY", "amount", "250.50"));
        assertThat(ingress.ingest("InvoiceGenerated", invoice).status())
                .isEqualTo(EventIngressService.STATUS_APPLIED);
        assertThat(ingress.ingest("InvoiceGenerated", invoice).status())
                .isEqualTo(EventIngressService.STATUS_DUPLICATE);   // replay does NOT +=

        ingress.ingest("PaymentReceived", Map.of(
                "paymentId", 8001L, "statDate", "2026-02-01", "amount", "99.75"));   // headCode default OPD
        flushAndClear();

        List<Object[]> revenue = rows("""
                SELECT head_code, total FROM ihms_reporting.rm_revenue_daily
                WHERE stat_date = '2026-02-01' ORDER BY head_code
                """);
        assertThat(revenue).hasSize(2);
        assertThat((BigDecimal) revenue.get(0)[1]).isEqualByComparingTo(new BigDecimal("99.75"));
        assertThat((BigDecimal) revenue.get(1)[1]).isEqualByComparingTo(new BigDecimal("250.50"));

        // ledger holds exactly one row per distinct source document (5 deliveries, 5 rows)
        assertThat(ledgerRepository.count()).isEqualTo(5L);

        // FINAL negative-outbox checkpoint of this flow
        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void reportEngine_executeOverReadModels_withNegativeOutboxAssertion() {
        // seed read models through the SAME ingress path
        for (long p : new long[]{501L, 502L, 503L}) {
            ingress.ingest("PatientRegistered", Map.of("patientId", p, "mrn", "MRN-" + p));
        }
        ingress.ingest("EncounterCreated", Map.of(
                "encounterId", 9100L, "statDate", "2026-03-10", "encounterType", "OPD"));
        ingress.ingest("EncounterCreated", Map.of(
                "encounterId", 9101L, "statDate", "2026-04-10", "encounterType", "IPD"));
        ingress.ingest("InvoiceGenerated", Map.of(
                "invoiceId", 7300L, "statDate", "2026-03-10", "headCode", "OPD", "amount", "500"));

        assertThat(outboxRepository.count()).isZero();

        // ---- create master ---------------------------------------------------------
        ReportEngineService engineRef = engine;
        var tabular = engineRef.create("PATIENT-LIST", "Patient registry list",
                (short) ReportMasterKind.KIND_TABULAR,
                Map.of("table", "rm_patient_registry",
                        "columns", List.of("patientId", "mrn"),
                        "dateField", "registeredAt"));

        // duplicate code -> 409 (register WP-T2)
        assertThatThrownBy(() -> engine.create("PATIENT-LIST", "Again",
                (short) ReportMasterKind.KIND_TABULAR,
                Map.of("table", "rm_patient_registry", "columns", List.of("patientId"))))
                .isInstanceOf(ReportingConflictException.class)
                .extracting(e -> ((ReportingConflictException) e).getErrorCode())
                .isEqualTo("REPORTING_DUPLICATE_REPORT_CODE");

        var summary = engine.create("ENC-CNT-SUMMARY", "Encounter count summary",
                (short) ReportMasterKind.KIND_SUMMARY,
                Map.of("table", "rm_encounter_stats", "aggregate", "cnt"));

        // ---- execute TABULAR over rm_patient_registry -------------------------------
        flushAndClear();
        var tabExec = engine.execute("PATIENT-LIST", Map.of());
        assertThat(tabExec.getStatus()).isEqualTo((short) ReportExecutionStatus.STATUS_DONE);
        assertThat(tabExec.getRowCount()).isEqualTo(3);
        List<Map<String, Object>> resultRows = rowsFrom(tabExec.getResult());
        assertThat(resultRows).hasSize(3);
        assertThat(resultRows.get(0).get("patient_id")).isEqualTo(501);
        assertThat(resultRows.get(0).get("mrn")).isEqualTo("MRN-501");

        // unknown report code -> 404
        assertThatThrownBy(() -> engine.execute("NOPE", Map.of()))
                .isInstanceOf(ReportingNotFoundException.class)
                .extracting(e -> ((ReportingNotFoundException) e).getErrorCode())
                .isEqualTo("NOT_FOUND");

        // ---- execute SUMMARY with a date filter --------------------------------------
        var sumExec = engine.execute("ENC-CNT-SUMMARY", Map.of(
                "dateFrom", "2026-03-01", "dateTo", "2026-03-31"));
        assertThat(sumExec.getRowCount()).isEqualTo(1);
        assertThat(sumExec.getResult()).contains("1");   // only the 2026-03-10 OPD encounter in range

        // execution listing by report code + status
        assertThat(engine.listExecutions("PATIENT-LIST", "DONE"))
                .extracting(e -> e.getExecutionId())
                .containsExactly(tabExec.getExecutionId());
        assertThat(engine.listExecutions(null, null)).hasSize(2);

        // ---- dashboard counts from the read models ------------------------------------
        Map<String, Object> counts = engine.dashboardCounts(java.time.LocalDate.of(2026, 3, 31));
        assertThat(counts.get("patients")).isEqualTo(3L);
        assertThat(counts.get("encounters")).isEqualTo(1L);   // asOf bounds to March
        assertThat((BigDecimal) counts.get("revenueTotal"))
                .isEqualByComparingTo(new BigDecimal("500"));

        Map<String, Object> allTime = engine.dashboardCounts(null);
        assertThat(allTime.get("encounters")).isEqualTo(2L);

        // NEGATIVE OUTBOX assertion: report creation/execution/dashboard emitted NOTHING
        assertThat(outboxRepository.count()).isZero();
    }

    /** Minimal JSON array reader for stored execution results (no extra deps). */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> rowsFrom(String resultJson) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(resultJson, List.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Kind constants mirror (keeps the test independent of entity import ordering). */
    static final class ReportMasterKind {
        static final short KIND_TABULAR = 1;
        static final short KIND_SUMMARY = 2;
        private ReportMasterKind() { }
    }

    /** Execution status constants mirror. */
    static final class ReportExecutionStatus {
        static final short STATUS_DONE = 1;
        private ReportExecutionStatus() { }
    }
}
