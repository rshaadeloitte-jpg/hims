# Unknowns Register — iHMS Modernization Research

> UNKNOWN-001…030: full detail in `../UNKNOWN-BEHAVIOR.md` (evidence + verification method). This register maintains the consolidated view + new unknowns from validation round (031+). Rule: nothing is invented; every unknown has a verification method and a blocking impact.

## Consolidated (details in UNKNOWN-BEHAVIOR.md)

| ID | Question | Status after validation round | Blocks |
|---|---|---|---|
| UNKNOWN-001 | VisitStatus initial value (T_Appointment/T_Encounter) | OPEN — V6 confirmed magic numbers, no constants | Status contracts |
| UNKNOWN-002 | Visit lifecycle split-brain semantics (two tables) | OPEN | Encounter service design |
| UNKNOWN-003 | DNR_REJECTED == DNR_DONATED == 5 collision | OPEN | Blood bank port |
| UNKNOWN-004 | orderValidationPeriod=72 semantics | OPEN | Ordering port |
| UNKNOWN-005 | 222 unmatched frontend URLs | OPEN — V1/V4 confirmed parser gaps vs genuine typos | API contract completeness |
| UNKNOWN-006 | ext.js injection mechanism | CLOSED — V4: IntegrationOperationsFilter:760-782 serves from cache (EXT_JS_CONTENT) | React EXT_DATA contract |
| UNKNOWN-007 | persistenceContext.xml runtime role | OPEN — not imported by App.java per prior agent-01; verify again at W0 | Persistence design |
| UNKNOWN-008 | Memory vs Redis cache in PRODUCTION | PARTIAL — repo defaults MemoryCacheImpl (V5); deployed env unverified | Cache topology |
| UNKNOWN-009 | FhirServer (HAPI R4) reachability — no servlet registration found | OPEN — V1 found bean `App.java:261`; runtime path unverified | FHIR surface |
| UNKNOWN-010 | HL7/eSign/OLIS dormancy | PARTIAL — HL7 job disabled (V5); eSign/OLIS unverified | Interop wave |
| UNKNOWN-011 | VisitStatus on T_Appointment column-type drift | OPEN (needs DDL) | Encounter contracts |
| UNKNOWN-012 | Batch/expiry ordering inside CIMS procs | OPEN | Inventory port |
| UNKNOWN-013 | Report config source of truth (int.files.path vs packaged) | OPEN | Reporting wave |
| UNKNOWN-014 | data-roleM permission semantics (2,078 keys) | OPEN | RBAC extraction |
| UNKNOWN-015 | Usage of procs beyond the 918 referenced | OPEN — V3: 918 referenced; remainder ~legacy; per-proc classification pending | Dead-code & port scope |
| UNKNOWN-016 | T_REGISTRATION* schema & insertion semantics | OPEN — no entity (V3); native SQL sites ~120 | Patient identity port |
| UNKNOWN-017 | GST applicability property effect | OPEN | Billing port |
| UNKNOWN-018 | "sync" role & sha256 password path | OPEN | IAM design |
| UNKNOWN-019 | Eclipselink.jpa.naming_into_indexed effect | OPEN | Column mapping on PG |
| UNKNOWN-020 | Multi-tier concession rounding (CEILING vs HALF_UP) | OPEN | BR-BILL-005 |
| UNKNOWN-021 | ABDM catch-all routing collisions | OPEN | ABDM wave |
| UNKNOWN-022 | Whitelist vs dynamicAspects merge semantics | OPEN | React schema engine |
| UNKNOWN-023 | saveAppErrors telemetry usage | OPEN | Observability |
| UNKNOWN-024 | configStatus.json / v_check semantics | OPEN | Config management |
| UNKNOWN-025 | getUserWardList parser-gap family | OPEN | API inventory |
| UNKNOWN-026 | getLastVisitStatus semantics (queue/kiosk) | OPEN | Queue port |
| UNKNOWN-027 | eRaktkosh sync direction/semantics | OPEN | Blood bank integrations |
| UNKNOWN-028 | allowNumberOfTaxCount / 3-tax-bucket | OPEN | Billing port |
| UNKNOWN-029 | docit.ctrl semantics | OPEN | Observability/deploy |
| UNKNOWN-030 | getServicewiseCollectionReportOld family usage | OPEN | Report port |

## New from validation round (2026-08-14)

### UNKNOWN-031 — True Oracle table count & DDL
- Observed: 894 distinct @Table names; 843 figure unverifiable (V3).
- Missing: ALL_TAB_COLUMNS / user_sequences / ALL_DEPENDENCIES dump from the live DB.
- Potential answers: 843 was a DB dump count; repo count 894 includes DTO-mapped tables.
- Business impact: LOW (sizing only). Migration risk: MEDIUM (missing DDL for T_REGISTRATION*).
- How to verify: W0 data-extraction job against Oracle (read-only).

### UNKNOWN-032 — Live-DB objects not present in repo (indexes, triggers, views, sequences)
- Observed: repo SQL contains 0 CREATE INDEX/TRIGGER/VIEW (V3).
- Missing: live ALL_INDEXES/ALL_TRIGGERS/ALL_VIEWS; production DDL may diverge from repo.
- Migration risk: HIGH if production has DB-side invariants the repo never shows.
- How to verify: W0 read-only DDL dump; add to W0 exit criteria (assumption A17).

### UNKNOWN-033 — Timezone/locale semantics
- Observed: timezones file, 175 CLDR locale files; no documented TZ handling (A20).
- Missing: server JVM TZ, DB SESSIONTIMEZONE, encounter-date truncation rules.
- Migration risk: MEDIUM (date boundaries drive billing/day-cutover jobs).
- How to verify: config dump + job review at W0.

### UNKNOWN-034 — Solr service_risl_qa collection usage
- Observed: 6 collection keys, 5 code-bound (V5).
- Missing: whether any runtime query uses the service collection (maybe via CRUD filter or dynamic query).
- How to verify: grep dynamic queries + live env query logs. Risk: LOW-MEDIUM.

### UNKNOWN-035 — Audit stream completeness
- Observed: AuditHandler/AuditHelper + T_TRX_HISTORY/T_LOGIN_AUDIT/T_ABDM_AUDIT/T_APPOINTMENT_CHANNEL (V2).
- Missing: which business operations actually flow through @AuditEvent vs bypass; row counts in prod.
- Migration risk: MEDIUM (target audit contract must match).
- How to verify: count @AuditEvent annotations; compare with audit-table volumes at W0.

## Resolution protocol
1. Each W0-gated unknown gets a verification task ID in EXECUTION-BACKLOG (W0-06 … W0-12).
2. When verified, move entry to evidence-register with the two-source evidence; keep a one-line tombstone here.
3. Never delete an unknown; supersede with reference to resolution.