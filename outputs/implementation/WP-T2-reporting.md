# WP-T2 — Reporting & Analytics service (Catalog #26) — Implementation Report

_Tranche 6, GAP-REGISTER-6 WP-T2. Service: `services/reporting-service`, package `com.hims.reporting`, port **8106**, schema **ihms_reporting**, Flyway `V1__init_ihms_reporting.sql`. Conventions mirrored from ordering-service / cssd-service (reference implementations)._

## 1. What was implemented

### 1.1 Read models (rm_*) + engine tables — exactly per register
| Table | Notes |
|---|---|
| `rm_patient_registry` | patient_id BIGINT PK (the SOURCE id), mrn VARCHAR(30), name VARCHAR(120), registered_at TIMESTAMP, audit cols. Upsert-by-PK semantics. |
| `rm_encounter_stats` | PK (stat_date DATE, encounter_type VARCHAR(20)), cnt INT CHECK >= 0. Native PG upsert: `INSERT .. ON CONFLICT DO UPDATE cnt = cnt + 1`. |
| `rm_revenue_daily` | PK (stat_date DATE, head_code VARCHAR(20)), total NUMERIC(19,2) CHECK >= 0. Native PG upsert: `total = rm_revenue_daily.total + EXCLUDED.total`. |
| `t_report_master` | report_id PK seq `t_report_master_seq`, report_code UNIQUE, title, kind SMALLINT (1=TABULAR 2=SUMMARY, CHECK-enforced), definition JSONB, is_active BOOLEAN DEFAULT true, audit cols. |
| `t_report_execution` | execution_id PK seq `t_report_execution_seq`, FK -> t_report_master, params JSONB, status SMALLINT 0=QUEUED 1=DONE 2=FAILED (CHECK), row_count INT NULL, result JSONB NULL, executed_at, audit cols. |
| `outbox` | Transactional-outbox TABLE per schema convention (IMP-DEC-004 envelope DDL identical to siblings). **ZERO emitters ever** — see §3 EVENT POLICY. |

### 1.2 Internal event ingress (production would be a Kafka consumer — contract stub)
`POST /api/v1/internal/events {eventType, payload}`:
- `PatientRegistered {patientId, mrn?, name?, registeredAt?}` → upsert `rm_patient_registry` by patient_id.
- `EncounterCreated {encounterId, statDate?, encounterType?}` → increment `rm_encounter_stats` (+1). Defaults: statDate=today, type=OPD.
- `InvoiceGenerated {invoiceId, statDate?, headCode?, amount}` and `PaymentReceived {paymentId, statDate?, headCode?, amount}` → accumulate `rm_revenue_daily` total += amount per stat_date+head_code; headCode defaults to `'OPD'`.
- Unknown eventType → 400 envelope `REPORTING_UNKNOWN_EVENT_TYPE`.

**STRICT idempotency** (register: "re-ingest same event => no double count; pinned"):
- PatientRegistered is naturally idempotent (source id IS the PK; replay updates in place).
- The accumulating read models are guarded by the ingest ledger (`t_ingested_event`, see §4 disclosure): the dedupe key is the deterministic source-document key (`patient:<id>` / `encounter:<id>` / `invoice:<id>` / `payment:<id>`); a replay returns `{status:"DUPLICATE"}` and touches NO read model. Double-ingest pinned at L0 AND L2.

### 1.3 Report master/execute engine
- `POST /api/v1/reports {reportCode, title, kind, definition}` — 201; guards: code/title required (≤40/≤120 chars), kind ∈ {1,2}, definition whitelist-validated at create time against the rm_* table/column set; duplicate code → **409 REPORTING_DUPLICATE_REPORT_CODE** (envelope pattern per rcm-service).
- `GET /api/v1/reports?active=` — optional active filter, newest first.
- `POST /api/v1/reports/{code}/execute {params:{dateFrom?,dateTo?}}` — unknown code → 404; runs the definition over the read models and stores the execution DONE with result JSONB + rowCount. TABULAR: SELECT whitelisted columns with optional dateFrom/dateTo on definition.dateField. SUMMARY: `COALESCE(SUM(cnt|total),0)` → single `[{"value": n}]` row. All identifiers normalized camelCase→snake and validated against a hard whitelist before SQL assembly (no free-form SQL from definitions).
- `GET /api/v1/reports/executions/{id}` (404 unknown) · `GET /api/v1/reports/executions?reportCode=&status=` (status as 0..2 or QUEUED/DONE/FAILED).
- `GET /api/v1/dashboard/counts?asOf=` → `{patients, encounters, revenueTotal}` from the read models; asOf bounds encounters/revenue to stat_date ≤ asOf.

### 1.4 Security / infra conventions (mirrored from reference)
Default-deny SecurityConfig (`!dev`) + DevSecurityConfig HTTP Basic (dev-only, IMP-DEC-019/IMP-DEC-006); ApiError envelope + GlobalExceptionHandler with BEXA-style codes incl. a dedicated 409 handler; CorrelationIdFilter (X-Request-ID); springdoc; actuator probes; Flyway create-schemas; server-owned sequences (IMP-DEC-003).

## 2. Stub disclosures (external boundaries — nothing silently invented)
1. **Event consumption is an INTERNAL REST ingress**, not Kafka. Production consumes PatientRegistered/EncounterCreated/InvoiceGenerated/PaymentReceived from sibling topics (KAFKA-EVENT-CATALOG); `POST /api/v1/internal/events` is the contract-stubbed relay stand-in, documented in controller/OpenAPI/migration comments.
2. **Report dispatcher emulated inline**: executions are created DONE directly; the register's "dispatcher emulated as JSON result". QUEUED/FAILED statuses exist in schema for the future async dispatcher but are never produced here (disclosed in OpenAPI description too).
3. **Jasper/Velocity rendering, regulatory exports (GSTR/NHM/RGHS), NMC/PCTS push jobs**: OUT OF SCOPE — comment-only (ReportEngineService javadoc + migration COMMENTs). No implementation invented.

## 3. EVENT POLICY — negative outbox assertion (service emits NOTHING)
Per register WP-T2 ("Events: emits NOTHING — negative outbox assertion"), mirroring cssd-service:
- pom has **no spring-kafka**; no OutboxService/Publisher exists anywhere in the dependency graph of either service class (structural impossibility, asserted at L0).
- L2 (`ReportingServiceIntegrationTest`, PG16 Testcontainers): `outboxRepository.count()` asserted **isZero() after migration/boot, after every rejected guard, after every applied/duplicate ingest, after report create/execute/dashboard** — through EVERY flow.

## 4. Deliberate deviations from the register text (all disclosed, none silent)
1. **ADDED TABLE `t_ingested_event`** (ledger, UNIQUE(event_type, event_key), seq `t_ingested_event_seq`). The register lists five tables, but its STRICT-idempotency requirement for ACCUMULATING read models cannot be met without durable dedupe (natural keys alone cannot detect a replayed invoice/payment/encounter). FLAGGED in migration header comment + entity javadoc.
2. **409 handling**: register says "409 dup code"; cssd/ordering handlers map all rule exceptions to 400, so a dedicated `ReportingConflictException` + handler was added (pattern already exists in rcm-service TidConflictException). No convention broken.
3. Register typo "optional dateFrom/dateFrom filters" read as dateFrom/dateTo.

## 5. Deferred items (comment-only, listed per rules)
- Jasper/Velocity rendering engines — comment-only.
- Regulatory exports GSTR/NHM/RGHS — comment-only.
- NMC/PCTS push jobs — comment-only.
- Async dispatcher producing QUEUED→DONE transitions — comment-only (schema supports it).
- Any event EMISSION (e.g., hypothetical ReportExecuted) — none proposed by KAFKA-EVENT-CATALOG, not invented.

## 6. Test table — `mvn -f services/reporting-service/pom.xml test` → **BUILD SUCCESS, exit 0**

| Suite | Level | Tests | Covers |
|---|---|---|---|
| `EventIngressServiceTest` | L0 unit | 12 | routing guards (unknown eventType 400 code, missing ids, amount ≥ 0); **strict double-ingest => no double count per event type (pinned)**; accumulation math incl. OPD default; ledger keys; negative-outbox structural assertion |
| `ReportEngineServiceTest` | L0 unit | 12 | create guards (code/title/kind/definition whitelist), dup code 409; execute happy TABULAR (SQL + params + DONE result) & SUMMARY; invalid stored def 400; unknown code/id 404; status parsing; dashboard counts; negative-outbox structural assertion |
| `ReportingControllerTest` | MockMvc (@WebMvcTest, default-deny SecurityConfig) | 8 | ALL endpoints 401 unauthenticated (default-deny incl. internal ingress); ingest APPLIED shape + 400 envelope; create 201 + dup 409; execute 200 + 404; executions list/by-id 404; dashboard counts shape |
| `ReportingServiceIntegrationTest` | L2 PG16 Testcontainers (postgres:16-alpine, Docker ran) | 3 | Flyway V1 applies (7 tables + 3 sequences); full ingest flow on real PG with strict idempotency pinned at DB level (replay leaves cnt/total unchanged, registry single row); report engine executes over seeded read models (TABULAR rows, SUMMARY with date range, listings, dashboard asOf math); **negative outbox assertions at every checkpoint** |

**Total: 35 tests, 35 passed, 0 failed, 0 skipped.** No test deleted or disabled to go green.

## 7. Files
```
services/reporting-service/
├── pom.xml                                   (Boot 3.5.3 / Java 21 / no spring-kafka / surefire api.version pin)
├── src/main/resources/application.yml        (port 8106, ihms_reporting)
├── src/main/resources/application-dev.yml
├── src/main/resources/db/migration/V1__init_ihms_reporting.sql
├── src/main/java/com/hims/reporting/
│   ├── ReportingServiceApplication.java      (no @EnableScheduling — no relay, emits nothing)
│   ├── api/{ApiError,GlobalExceptionHandler,IngestionController,ReportController}.java
│   ├── application/{EventIngressService,ReportEngineService}.java
│   ├── application/exceptions/{ReportingRuleException,InvalidReportingRequestException,
│   │                            ReportingNotFoundException,ReportingConflictException}.java
│   ├── configuration/{SecurityConfig,DevSecurityConfig}.java
│   ├── domain/model/{ReportingBase,RmPatientRegistry,RmEncounterStats,RmRevenueDaily,
│   │                 ReportMaster,ReportExecution,IngestedEvent}.java
│   ├── domain/repository/*.java              (6 repos incl. native ON CONFLICT upserts)
│   ├── infrastructure/outbox/{OutboxEvent,OutboxRepository}.java   (read-side only)
│   └── infrastructure/web/CorrelationIdFilter.java
└── src/test/java/com/hims/reporting/
    ├── api/ReportingControllerTest.java
    ├── application/{EventIngressServiceTest,ReportEngineServiceTest}.java
    └── infrastructure/persistence/ReportingServiceIntegrationTest.java
openapi/reporting-service.yaml                 (verified programmatically 1:1 vs controllers)
```

## 8. Acceptance checklist
- [x] Only `services/reporting-service/**`, `openapi/reporting-service.yaml`, `outputs/implementation/WP-T2-reporting.md` touched — no other service or shared file modified.
- [x] Every table/API/guard in the GAP-REGISTER-6 WP-T2 section implemented (deviations disclosed in §4).
- [x] Negative outbox assertion present at L0 and L2.
- [x] Unit tests for every guard/state machine; MockMvc default-deny + error envelopes; PG16 Testcontainers integration (Flyway + negative outbox).
- [x] `openapi/reporting-service.yaml` matches controller paths+methods 1:1 (programmatic check).
- [x] `mvn -f services/reporting-service/pom.xml test` exits 0 (BUILD SUCCESS, 35/35).
