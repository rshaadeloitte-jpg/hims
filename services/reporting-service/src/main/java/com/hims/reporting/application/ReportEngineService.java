package com.hims.reporting.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.reporting.application.exceptions.InvalidReportingRequestException;
import com.hims.reporting.application.exceptions.ReportingConflictException;
import com.hims.reporting.application.exceptions.ReportingNotFoundException;
import com.hims.reporting.domain.model.ReportExecution;
import com.hims.reporting.domain.model.ReportMaster;
import com.hims.reporting.domain.repository.IngestedEventRepository;
import com.hims.reporting.domain.repository.RmEncounterStatsRepository;
import com.hims.reporting.domain.repository.RmOpStatsRepository;
import com.hims.reporting.domain.repository.RmPatientRegistryRepository;
import com.hims.reporting.domain.repository.RmRevenueDailyRepository;
import com.hims.reporting.domain.repository.ReportExecutionRepository;
import com.hims.reporting.domain.repository.ReportMasterRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Report master + execute engine over the rm_* read models, and dashboard counts
 * (register WP-T2).
 *
 * Definition JSONB contract:
 *   TABULAR (kind 1): {"table":"rm_patient_registry",
 *                      "columns":["patientId","mrn","name"],
 *                      "dateField":"registeredAt"}            (+ params dateFrom/dateTo)
 *   SUMMARY (kind 2): {"table":"rm_revenue_daily","aggregate":"total"}
 *                     {"table":"rm_encounter_stats","aggregate":"cnt"}
 *
 * The engine whitelists ONLY the three rm_* tables and their known columns —
 * table/column/aggregate identifiers are validated against the whitelist before
 * any SQL is assembled (no free-form SQL from definitions). Params support
 * dateFrom / dateTo.
 *
 * OUT OF SCOPE (comment-only per register WP-T2): Jasper/Velocity rendering,
 * regulatory exports (GSTR/NHM/RGHS), NMC/PCTS push jobs. The report dispatcher
 * is EMULATED inline: executions are created DONE with a JSONB result — QUEUED /
 * FAILED statuses exist in the schema for the future async dispatcher but are not
 * produced by this engine (disclosed). This service EMITS NOTHING to the outbox.
 */
@Service
public class ReportEngineService {

    private static final Logger log = LoggerFactory.getLogger(ReportEngineService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    /** Whitelisted read-model tables: allowed columns + allowed date-filter columns. */
    private record TableMeta(Set<String> columns, Set<String> dateColumns) {}

    private static final Map<String, TableMeta> TABLES = Map.of(
            "rm_patient_registry", new TableMeta(
                    Set.of("patient_id", "mrn", "name", "registered_at"),
                    Set.of("registered_at")),
            "rm_encounter_stats", new TableMeta(
                    Set.of("stat_date", "encounter_type", "cnt"),
                    Set.of("stat_date")),
            "rm_revenue_daily", new TableMeta(
                    Set.of("stat_date", "head_code", "total"),
                    Set.of("stat_date")),
            "rm_op_stats", new TableMeta(
                    Set.of("stat_date", "op_type", "cnt"),
                    Set.of("stat_date")));

    private final ReportMasterRepository masterRepository;
    private final ReportExecutionRepository executionRepository;
    private final RmPatientRegistryRepository patientRepository;
    private final RmEncounterStatsRepository encounterRepository;
    private final RmRevenueDailyRepository revenueRepository;
    private final RmOpStatsRepository opStatsRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    public ReportEngineService(ReportMasterRepository masterRepository,
                               ReportExecutionRepository executionRepository,
                               RmPatientRegistryRepository patientRepository,
                               RmEncounterStatsRepository encounterRepository,
                               RmRevenueDailyRepository revenueRepository,
                               RmOpStatsRepository opStatsRepository) {
        this.masterRepository = masterRepository;
        this.executionRepository = executionRepository;
        this.patientRepository = patientRepository;
        this.encounterRepository = encounterRepository;
        this.revenueRepository = revenueRepository;
        this.opStatsRepository = opStatsRepository;
        this.objectMapper = new ObjectMapper();
    }

    // ------------------------------------------------------------------ report master

    /**
     * Create a report definition. Guards (400 BEXA003 unless noted):
     * reportCode required (&lt;= 40 chars); title required (&lt;= 120 chars);
     * kind IN (1=TABULAR, 2=SUMMARY); definition required JSON object whose
     * table/columns/dateField/aggregate pass the rm_* whitelist. Duplicate
     * reportCode -> 409 REPORTING_DUPLICATE_REPORT_CODE (register WP-T2).
     */
    @Transactional
    public ReportMaster create(String reportCode, String title, Short kind, Map<String, Object> definition) {
        if (reportCode == null || reportCode.isBlank()) {
            throw new InvalidReportingRequestException("BEXA003", "reportCode is required");
        }
        if (reportCode.length() > 40) {
            throw new InvalidReportingRequestException("BEXA003",
                    "reportCode must be at most 40 characters");
        }
        if (title == null || title.isBlank()) {
            throw new InvalidReportingRequestException("BEXA003", "title is required");
        }
        if (title.length() > 120) {
            throw new InvalidReportingRequestException("BEXA003",
                    "title must be at most 120 characters");
        }
        if (kind == null || (kind != ReportMaster.KIND_TABULAR && kind != ReportMaster.KIND_SUMMARY)) {
            throw new InvalidReportingRequestException("BEXA003",
                    "kind must be 1 (TABULAR) or 2 (SUMMARY)");
        }
        if (definition == null || definition.isEmpty()) {
            throw new InvalidReportingRequestException("BEXA003",
                    "definition is required (JSON object)");
        }
        validateDefinition(kind, definition);
        if (masterRepository.existsByReportCode(reportCode)) {
            throw new ReportingConflictException("REPORTING_DUPLICATE_REPORT_CODE",
                    "reportCode already exists: " + reportCode);
        }

        Timestamp nowTs = now();
        ReportMaster master = new ReportMaster();
        master.setReportId(nextId("t_report_master_seq"));
        master.setReportCode(reportCode);
        master.setTitle(title);
        master.setKind(kind);
        master.setDefinition(writeJson(definition));
        master.setIsActive(Boolean.TRUE);
        master.setAddedBy(AUDIT_ACTOR_ID);
        master.setAddedDatetime(nowTs);
        try {
            masterRepository.save(master);
        } catch (DataIntegrityViolationException e) {
            // race with a concurrent create of the same reportCode — same 409 envelope
            throw new ReportingConflictException("REPORTING_DUPLICATE_REPORT_CODE",
                    "reportCode already exists: " + reportCode);
        }
        log.info("Created report {} ({}, kind {})", reportCode, master.getReportId(), kind);
        return master;
    }

    @Transactional(readOnly = true)
    public List<ReportMaster> list(Boolean active) {
        return masterRepository.search(active);
    }

    // ------------------------------------------------------------------ execute

    /**
     * Execute a report by code with {params:{dateFrom?,dateTo?}}. Unknown code ->
     * 404 NOT_FOUND. The inline dispatcher runs the definition against the read
     * models and stores the execution DONE with result JSONB + row_count.
     * Emits NOTHING to the outbox.
     */
    @Transactional
    public ReportExecution execute(String reportCode, Map<String, Object> params) {
        ReportMaster master = masterRepository.findByReportCode(reportCode)
                .orElseThrow(() -> new ReportingNotFoundException("NOT_FOUND",
                        "Report not found: " + reportCode));
        Map<String, Object> effectiveParams = params == null ? Map.of() : params;
        List<Map<String, Object>> rows = runDefinition(master.getKind(), master.getDefinition(), effectiveParams);

        Timestamp nowTs = now();
        ReportExecution execution = new ReportExecution();
        execution.setExecutionId(nextId("t_report_execution_seq"));
        execution.setReportId(master.getReportId());
        execution.setParams(effectiveParams.isEmpty() ? null : writeJson(effectiveParams));
        execution.setStatus(ReportExecution.STATUS_DONE);
        execution.setRowCount(rows.size());
        execution.setResult(writeJson(rows));
        execution.setExecutedAt(nowTs);
        execution.setAddedBy(AUDIT_ACTOR_ID);
        execution.setAddedDatetime(nowTs);
        executionRepository.save(execution);
        log.info("Executed report {}: execution {} returned {} row(s)", reportCode,
                execution.getExecutionId(), rows.size());
        return execution;
    }

    @Transactional(readOnly = true)
    public ReportExecution getExecution(long executionId) {
        return executionRepository.findById(executionId)
                .orElseThrow(() -> new ReportingNotFoundException("NOT_FOUND",
                        "Report execution not found: " + executionId));
    }

    @Transactional(readOnly = true)
    public List<ReportExecution> listExecutions(String reportCode, String rawStatus) {
        Long reportId = null;
        if (reportCode != null && !reportCode.isBlank()) {
            reportId = masterRepository.findByReportCode(reportCode)
                    .orElseThrow(() -> new ReportingNotFoundException("NOT_FOUND",
                            "Report not found: " + reportCode)).getReportId();
        }
        Short status = parseStatus(rawStatus);
        return executionRepository.search(reportId, status);
    }

    /** Status filter: 0..2 or name (QUEUED, DONE, FAILED), case-insensitive; null = all. */
    static Short parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim().toUpperCase();
        return switch (s) {
            case "0", "QUEUED" -> ReportExecution.STATUS_QUEUED;
            case "1", "DONE" -> ReportExecution.STATUS_DONE;
            case "2", "FAILED" -> ReportExecution.STATUS_FAILED;
            default -> throw new InvalidReportingRequestException("BEXA003",
                    "Unknown status: " + raw);
        };
    }

    // ------------------------------------------------------------------ dashboard

    /**
     * Dashboard counts from the read models: {patients, encounters, revenueTotal}.
     * asOf (optional yyyy-MM-dd) bounds encounters/revenue to stat_date &lt;= asOf.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> dashboardCounts(LocalDate asOf) {
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("patients", patientRepository.count());
        counts.put("encounters", encounterRepository.totalCnt(asOf));
        BigDecimal revenue = revenueRepository.totalRevenue(asOf);
        counts.put("revenueTotal", revenue);
        counts.put("otOperations", opStatsRepository.totalCnt(asOf));
        return counts;
    }

    // ------------------------------------------------------------------ engine internals

    /** Run a stored definition; returns rows as ordered maps (snake_case keys). */
    List<Map<String, Object>> runDefinition(short kind, String definitionJson, Map<String, Object> params) {
        Map<String, Object> definition = readJson(definitionJson);
        if (kind == ReportMaster.KIND_TABULAR) {
            return runTabular(definition, params);
        }
        return runSummary(definition, params);
    }

    private List<Map<String, Object>> runTabular(Map<String, Object> definition, Map<String, Object> params) {
        String table = normalizedTable(definition);
        TableMeta meta = TABLES.get(table);

        Object rawColumns = definition.get("columns");
        if (!(rawColumns instanceof List<?> columnList) || columnList.isEmpty()) {
            throw new InvalidReportingRequestException("REPORTING_INVALID_DEFINITION",
                    "TABULAR definition needs a non-empty columns array for table " + table);
        }
        List<String> columns = columnList.stream()
                .map(c -> normalizeIdentifier(String.valueOf(c)))
                .toList();
        for (String column : columns) {
            if (!meta.columns().contains(column)) {
                throw new InvalidReportingRequestException("REPORTING_INVALID_DEFINITION",
                        "Unknown column '" + column + "' for table " + table);
            }
        }
        String dateColumn = null;
        if (definition.get("dateField") != null) {
            dateColumn = normalizeIdentifier(String.valueOf(definition.get("dateField")));
            if (!meta.dateColumns().contains(dateColumn)) {
                throw new InvalidReportingRequestException("REPORTING_INVALID_DEFINITION",
                        "dateField '" + definition.get("dateField")
                                + "' is not a filterable date column of " + table);
            }
        }
        LocalDate dateFrom = parseDateParam(params.get("dateFrom"), "dateFrom");
        LocalDate dateTo = parseDateParam(params.get("dateTo"), "dateTo");

        StringBuilder sql = new StringBuilder("SELECT ")
                .append(String.join(", ", columns))
                .append(" FROM ihms_reporting.").append(table);
        List<LocalDate> bound = new ArrayList<>();
        if (dateColumn != null && dateFrom != null) {
            sql.append(" WHERE ").append(dateColumn).append(" >= ?1");
            bound.add(dateFrom);
        }
        if (dateColumn != null && dateTo != null) {
            sql.append(bound.isEmpty() ? " WHERE " : " AND ")
                    .append(dateColumn).append(" <= ?2");
            bound.add(dateTo);
        }
        sql.append(" ORDER BY ").append(columns.get(0));

        var query = em.createNativeQuery(sql.toString());
        for (int i = 0; i < bound.size(); i++) {
            query.setParameter(i + 1, bound.get(i));
        }
        return mapRows(columns, query.getResultList());
    }

    private List<Map<String, Object>> runSummary(Map<String, Object> definition, Map<String, Object> params) {
        String table = normalizedTable(definition);
        TableMeta meta = TABLES.get(table);
        Object rawAggregate = definition.get("aggregate");
        if (rawAggregate == null) {
            throw new InvalidReportingRequestException("REPORTING_INVALID_DEFINITION",
                    "SUMMARY definition needs an aggregate (cnt|total) for table " + table);
        }
        String aggregateColumn = normalizeIdentifier(String.valueOf(rawAggregate));
        if (!meta.columns().contains(aggregateColumn)
                || !aggregateColumn.equals("cnt") && !aggregateColumn.equals("total")) {
            throw new InvalidReportingRequestException("REPORTING_INVALID_DEFINITION",
                    "SUMMARY aggregate must be cnt or total — got '"
                            + rawAggregate + "' for table " + table);
        }
        LocalDate dateFrom = parseDateParam(params.get("dateFrom"), "dateFrom");
        LocalDate dateTo = parseDateParam(params.get("dateTo"), "dateTo");

        StringBuilder sql = new StringBuilder("SELECT COALESCE(SUM(").append(aggregateColumn)
                .append("), 0) FROM ihms_reporting.").append(table);
        List<LocalDate> bound = new ArrayList<>();
        if (dateFrom != null) {
            sql.append(" WHERE stat_date >= ?1");
            bound.add(dateFrom);
        }
        if (dateTo != null) {
            sql.append(bound.isEmpty() ? " WHERE " : " AND ").append("stat_date <= ?2");
            bound.add(dateTo);
        }
        var query = em.createNativeQuery(sql.toString());
        for (int i = 0; i < bound.size(); i++) {
            query.setParameter(i + 1, bound.get(i));
        }
        Object value = query.getSingleResult();

        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("value", value instanceof Number ? value : String.valueOf(value));
        rows.add(row);
        return rows;
    }

    /** Validate a NEW definition at create-time against the same whitelist the engine enforces. */
    void validateDefinition(short kind, Map<String, Object> definition) {
        String table = normalizedTable(definition);
        TableMeta meta = TABLES.get(table);
        if (kind == ReportMaster.KIND_TABULAR) {
            Object rawColumns = definition.get("columns");
            if (!(rawColumns instanceof List<?> columnList) || columnList.isEmpty()) {
                throw new InvalidReportingRequestException("REPORTING_INVALID_DEFINITION",
                        "TABULAR definition needs a non-empty columns array for table " + table);
            }
            for (Object c : columnList) {
                String column = normalizeIdentifier(String.valueOf(c));
                if (!meta.columns().contains(column)) {
                    throw new InvalidReportingRequestException("REPORTING_INVALID_DEFINITION",
                            "Unknown column '" + c + "' for table " + table);
                }
            }
            if (definition.get("dateField") != null) {
                String dateColumn = normalizeIdentifier(String.valueOf(definition.get("dateField")));
                if (!meta.dateColumns().contains(dateColumn)) {
                    throw new InvalidReportingRequestException("REPORTING_INVALID_DEFINITION",
                            "dateField '" + definition.get("dateField")
                                    + "' is not a filterable date column of " + table);
                }
            }
        } else {
            Object aggregate = definition.get("aggregate");
            if (aggregate == null
                    || !(aggregate.equals("cnt") || aggregate.equals("total"))
                    || !meta.columns().contains(String.valueOf(aggregate))) {
                throw new InvalidReportingRequestException("REPORTING_INVALID_DEFINITION",
                        "SUMMARY aggregate must be cnt or total — got '" + aggregate
                                + "' for table " + table);
            }
        }
    }

    /** Whitelisted-table lookup on the camelCase->snake normalised definition.table. */
    private String normalizedTable(Map<String, Object> definition) {
        Object raw = definition.get("table");
        if (raw == null) {
            throw new InvalidReportingRequestException("REPORTING_INVALID_DEFINITION",
                    "definition.table is required");
        }
        String table = normalizeIdentifier(String.valueOf(raw));
        if (!TABLES.containsKey(table)) {
            throw new InvalidReportingRequestException("REPORTING_INVALID_DEFINITION",
                    "Unknown table '" + raw + "' — only rm_patient_registry, "
                            + "rm_encounter_stats, rm_revenue_daily, rm_op_stats are executable");
        }
        return table;
    }

    /** camelCase -> lower_snake; then validated against the whitelist (no free-form SQL ever). */
    static String normalizeIdentifier(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidReportingRequestException("REPORTING_INVALID_DEFINITION",
                    "Blank identifier in definition");
        }
        return raw.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }

    private List<Map<String, Object>> mapRows(List<String> columns, List<?> results) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object result : results) {
            Map<String, Object> row = new LinkedHashMap<>();
            if (result instanceof Object[] tuple) {
                for (int i = 0; i < columns.size(); i++) {
                    row.put(columns.get(i), tuple[i]);
                }
            } else {
                row.put(columns.get(0), result);
            }
            rows.add(row);
        }
        return rows;
    }

    private Map<String, Object> readJson(String json) {
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class));
        } catch (JsonProcessingException e) {
            throw new InvalidReportingRequestException("REPORTING_INVALID_DEFINITION",
                    "Definition is not valid JSON");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON serialization failed", e);
        }
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_reporting." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }

    /** Shared strict yyyy-MM-dd param parser (delegates to the ingress service's static helper). */
    private static LocalDate parseDateParam(Object raw, String name) {
        return EventIngressService.parseDateStrict(raw, name);
    }
}

