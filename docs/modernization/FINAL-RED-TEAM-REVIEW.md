# FINAL-RED-TEAM-REVIEW — iHMS Modernization Plan

> Final adversarial pass over the research + plan. Each reviewer searched for omissions in their specialty. Every CRITICAL/HIGH gap is either RESOLVED (with evidence pointer) or BLOCKED (tracked in EXECUTION-BACKLOG/UNKNOWN register). Review date: 2026-08-14.

---

## 1. Agent 04 (Frontend) vs the React plan — omissions in FAUG behavior

| # | Gap found | Verdict |
|---|---|---|
| RT-01 | `dynamicAspects` conditions reference backend-injected globals (e.g. `gvar@maltaApplication` in doctorTransportRequestSchema) — React engine must replicate the EXT_DATA contract, not just schemas. | RESOLVED — EXECUTION-BACKLOG MIG-005 (EXT_JS injection replication); evidence E-15/CONTR-013. |
| RT-02 | 2,943 templates use a custom `<%...%>`/`#var/#for/#if` language — no off-the-shelf template engine compatible. | RESOLVED — MIG-005 + 09-react-migration-design transpiler approach; CONTR-015. |
| RT-03 | Route duplicate `pharmacy:ipReturnFlow`/`pharmacy:iPReturnFlow` breaks strict JSON parsing — target router must handle case-insensitive or legacy-cased route keys. | RESOLVED — CONTR-004; router design records both aliases. |
| RT-04 | Print culture: qz-tray WebSocket printing, ZPL labels, barcode/QR per module (12+ modules) — omitted from most service docs. | RESOLVED — MIG-057 + REPORT-INVENTORY; per-screen print tier in 09-design. |
| RT-05 | 222 unmatched frontend URLs — if they are dead typos vs live routes, silent breakage. | BLOCKED — UNKNOWN-005 (triage in MIG-005; acceptance criterion). |
| RT-06 | Client-side billing math (billing-module.js:2094-2111) — if removed before server parity, receipt totals change. | RESOLVED — MIG-040 contract-first + BR-BILL golden tests (UNKNOWN-020/028 gates). |

## 2. Agent 03 (Backend) vs the service decomposition

| # | Gap found | Verdict |
|---|---|---|
| RT-10 | 5 REST surfaces (E-04) must be replicated by the gateway; FHIR RestfulServer reachability unknown. | RESOLVED (gateway scope) / BLOCKED (UNKNOWN-009 reachability). |
| RT-11 | 17 unwired @Component services — ActionNotificationServiceImpl is dead REST surface; other 16 are helpers/utilities consumed by wired services via DI. Decomposition must not lose helper DI graphs. | RESOLVED — CONTR-007; DEAD-CODE-CANDIDATES updated; helper slicing part of each MIG-XXX. |
| RT-12 | 1,309 @Transactional incl. `rollbackOn=Exception` (checked exceptions roll back) and 7 REQUIRES_NEW sites — target Spring translation is not 1:1. | RESOLVED — CONTR-019; migration rules in POSTGRESQL-MIGRATION + MIG-033. |
| RT-13 | MasterCachedServiceImpl (13,331 LOC, 270 endpoints, 4th largest) is the master-data cache backbone — naive slicing risks cache-stampede + consistency. | RESOLVED — MIG-010 (cache semantics port; findConfigApplicationMaster 179 call sites is top consumer). |
| RT-14 | Generic CRUD filter (654 registered entities) has behavior beyond CRUD: crudaddMultiple, crudgetAllAll, crudremove + audit events (ADD.MASTER/UPDATE.MASTER). Contract family must include these verbs. | RESOLVED — MIG-005 contract family scope (7 verbs, V1 evidence). |

## 3. Agent 05 (Database) vs the PostgreSQL plan

| # | Gap found | Verdict |
|---|---|---|
| RT-20 | **918-referenced procs (not 30) change the port budget by an order of magnitude.** Golden fixtures for ~30 procs are insufficient (prior TEST-STRATEGY). | RESOLVED — CONTR-001 supersedes; MIG-002 fixture scope expanded (top-30 call-dense first, then full); MIG-006 plpgsql scaffold. |
| RT-21 | T_BILL/T_BILLDETAILS/T_ADMISSION/T_VISIT/T_CHARGES have no entities — any target PG model built "from the ORM" is impossible; contract-first modeling mandatory. | RESOLVED — E-11 + MIG-040/032 contract-first; mapping ledger (MIG-006). |
| RT-22 | Live DB may contain indexes/triggers/views absent from repo (0 found in source). | BLOCKED — UNKNOWN-032 (W0 DDL dump gate; MIG-001). |
| RT-23 | Sequence numbering: 50 hardcoded `getNextSequenceValue` names + 155 in procs + dynamic per-unit names (Unitmaster.getOPDSequenceName) — PG seeding must cover dynamic names. | RESOLVED — MIG-006 sequence strategy; UNKNOWN-free gap monitoring. |
| RT-24 | PII (Aadhaar/Janaadhaar/eMitra payloads) in logs/URLs today (E-07) — target must log-redact. | RESOLVED — MIG-004 security gates; observability rules (OBSERVABILITY). |
| RT-25 | ROWNUM 489 / SYSDATE 247 / DUAL 47 / NVL 21 — port construct catalog exists (POSTGRESQL-MIGRATION); CONNECT BY (1 in procs) needs recursive-CTE translation. | RESOLVED — construct catalog; MIG-006 pilot covers them. |

## 4. Agent 06 (Integrations) vs the wave plan

| # | Gap found | Verdict |
|---|---|---|
| RT-30 | Solr in-tx writes (E-16) — decoupling to outbox+Kafka (MIG-058) changes failure semantics: today DB rollback implies Solr rollback; after decoupling a Kafka/Solr failure after DB commit creates drift. | RESOLVED — MIG-058 explicitly preserves search semantics; adds reconciliation job (solr_status bookkeeping exists: SolrUtil:955 — reuse as reconciliation marker). |
| RT-31 | Encounter 3-day rolling window is a *query-time* contract — must not change. | RESOLVED — MIG-058 acceptance criterion; ApplicationJobs:542-563 semantics ported. |
| RT-32 | HL7 dormant but configurable (E-05/09): enabling `hl7.billingOrder.messages.enabled` later must not break the target ordering service. | RESOLVED — MIG-033 keeps the flag as a feature toggle with the HL7 adapter behind it. |
| RT-33 | e-Mitra `?param1=<cacheKey>` bearer-URL flow and SSO `userdetails` in query string — target must replace, and integration gateways must not copy the pattern. | RESOLVED — MIG-004 (security) + MIG-055 (interop) explicit "do not replicate" list. |
| RT-34 | WhatsApp retrigger queue (WhatsappMessageQueue) + esanchar — retry semantics must be preserved (0 */30 * ? * *). | RESOLVED — MIG-055 notification service owns retrigger contract. |
| RT-35 | ABDM consent/HIP-HIU flows (milestones M1/M2) — not exercised in waves beyond MIG-055; ensure no regression in HFR/ABHA endpoints. | RESOLVED — MIG-055 acceptance includes ABDM callback parity tests. |

## 5. Agent 07 (Security) — hardest-hitting review

| # | Gap found | Verdict |
|---|---|---|
| RT-40 | **Password verification is a no-op** (PasswordValidator.java:13-24) + test backdoor + static-token bypass + portal "Administrator" role (E-07) — the plan must treat current authz as absent, and the target as a re-build, not a port. | RESOLVED — MIG-004 explicit "do not replicate" list + independent pen-test gate (MIG-063). |
| RT-41 | 34 plaintext secrets + key files in repo — target repo must be created fresh (no history carryover). | RESOLVED — MIG-063 + W0 secret vault (MIG-003). |
| RT-42 | `/ext/api` empty-allowlist bypass (IntegrationOperationsFilter:442-447) — target mapping must default-deny. | RESOLVED — MIG-005 gateway rule: allowlist required; default 401. |
| RT-43 | Audit attribution is attacker-controllable today (E-07) — target audit parity is meaningless until authN is real. | RESOLVED — MIG-004 sequencing: IAM before audit-parity waves; UNKNOWN-035 audit completeness check. |
| RT-44 | RBAC is frontend-only; extracting 2,078 data-roleM keys (UNKNOWN-014) into backend-enforced permissions is the single biggest behavior-preservation risk (over/under-granting). | BLOCKED — UNKNOWN-014; MIG-004 acceptance = role-permission matrix export verified by module owners. |

## 6. Agent 09 (QA) vs test strategy

| # | Gap found | Verdict |
|---|---|---|
| RT-50 | No existing automated tests in repo (surefire JUnit5 scaffold only) — characterization testing is the only regression net. | RESOLVED — TEST-STRATEGY L0-L5 + MIG-002 fixtures; add record/playback of live user sessions before W3. |
| RT-51 | Status magic numbers (E-12) — contracts must lock status enums before any port; tests must assert transitions, not values. | RESOLVED — MIG-031 status dictionary lock gate. |
| RT-52 | Concurrency: zero @Version, zero flush() — lost-update semantics today; target must not silently improve OR regress behavior without business sign-off. | RESOLVED — noted in MIG-033/040 as explicit "optimistic locking decision" gate (documented behavior change with owner sign-off). |
| RT-53 | RollbackOn=Exception covers checked exceptions — fixture tests must include checked-exception paths (e.g. file IO in EMR leg). | RESOLVED — MIG-033 test matrix includes per-step failure injection. |

## 7. Agent 08 (DevOps) vs deployment plan

| # | Gap found | Verdict |
|---|---|---|
| RT-60 | Legacy has no health endpoints, no metrics, docit.ctrl (UNKNOWN-029) as heartbeat — target probes must be defined before W1. | RESOLVED — OBSERVABILITY + MIG-003; UNKNOWN-029 resolution in W0. |
| RT-61 | Print WebSocket (ws://localhost:8182) is machine-local — containerized target needs per-node print service or browser-based qz-tray config. | RESOLVED — MIG-057 print tier design (09-design §print). |
| RT-62 | `int.files.path=D:/RISL/ihms/integration` NAS dependency + reports config drift (UNKNOWN-013). | BLOCKED — UNKNOWN-013; MIG-001 gate. |
| RT-63 | Timezone: daily jobs (dailyencounter 0 0 0 * * *) and bill day-cutover depend on server TZ (UNKNOWN-033). | BLOCKED — UNKNOWN-033; W0 config dump. |

## 8. Agent 10 (Migration) — architectural self-review

| # | Gap found | Verdict |
|---|---|---|
| RT-70 | Distributed-monolith risk: 29 services with 1,309 in-tx call chains — W4 billing and W3 ordering both touch T_BILL via procs today. Boundary test: which service owns CIMS_ADDBILL port? | RESOLVED — MIG-033 (ordering) owns the order→bill chain in target; billing service receives via event/API, NOT shared writes (STRANGLER no-dual-write rule). |
| RT-71 | No-dual-write + strangler: patient created in legacy today must propagate to target pre-cutover (CDC/outbox). Direction must be defined per table before W1. | RESOLVED — STRANGLER §8 CDC protocol; MIG-C04 per-schema jobs. |
| RT-72 | Kafka event catalog (12 events) vs 918 procs — event boundaries must map to proc-side effects, not just endpoints; procs with multi-table writes (e.g. CIMS_ADDBILL) define the outbox granularity. | RESOLVED — KAFKA-EVENT-CATALOG revision note: outbox entries keyed by proc write-sets (MIG-006 dependency capture). |
| RT-73 | Rollback per wave relies on gateway routing + dual run; DB rollback needs per-schema backfill jobs — time-boxed. | RESOLVED — MIG-C03 + STRANGLER §8. |
| RT-74 | 16 god-class services (>2,000 LOC each; 5 above 10k) cannot be ported whole; slicing per flow must precede any wave touching them (Nursing 18,119 LOC has 135 tx — slice by flow not by line). | RESOLVED — each MIG-XXX includes a slicing plan step; flow-based slicing rule recorded in MIGRATION-COMPLEXITY. |

---

## Summary verdict

- **CRITICAL gaps resolved**: 0 outstanding. The four CRITICAL security findings (E-07 family) are contained in MIG-004/005/063 with explicit "do-not-replicate" gates.
- **HIGH gaps blocked (all tracked)**: UNKNOWN-005 (222 URLs), UNKNOWN-008/009/010/013 (deployed env), UNKNOWN-014 (permission semantics), UNKNOWN-016 (T_REGISTRATION DDL), UNKNOWN-031/032 (live DDL), UNKNOWN-033 (TZ). Each is a W0 gate (MIG-001) with acceptance criteria.
- **Plan changes resulting from this review**:
  1. Fixture scope expanded from ~30 to 918-referenced procs (RT-20).
  2. Contract-first modeling mandated for entity-less tables (RT-21).
  3. IAM re-sequenced ahead of all cutovers (RT-40/43).
  4. Print/NAS/TZ environmental unknowns elevated to W0 blockers (RT-61/62/63).
  5. Flow-based slicing rule added to all god-class migrations (RT-74).