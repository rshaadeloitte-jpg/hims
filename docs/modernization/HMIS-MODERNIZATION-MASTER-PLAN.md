# HMIS-MODERNIZATION-MASTER-PLAN — iHMS / OHUM PAS NG

> **Program**: Evidence-based modernization of the Government-of-Rajasthan iHMS ("ohumpasng" v3.4) hospital management information system.
> **Primary source**: `D:\ihms` (research-only). **Research governance**: `research/` registers (evidence, assumption, contradiction, unknowns, progress).
> **Status**: RESEARCH COMPLETE (first-pass) · EXECUTION NOT STARTED · All TO-BE designs PRELIMINARY pending W0 gates.
> **Version**: 2026-08-14 (validation round V1–V6 incorporated; see contradiction-register).
> This document is the **integration index + decision record**; deep facts live in the 24 numbered/consolidation documents it cites. It supersedes HMIS-MODERNIZATION-BLUEPRINT.md as the single master entry point.

---

## 1. Executive Summary

iHMS is a single-module hospital management information system for the Rajasthan government health network (RISL overlay: e-Mitra, SSO, Jan-Aadhaar, Bhamashah, ABDM/NHCX, e-Aushadhi). Measured at: **3,001 Java files · 155 service files (231,791 LOC) · ~3,000 explicit JAX-RS verbs + 1,109 generic-CRUD routes + dynamic report dispatcher (5 REST surfaces total) · 1,049 JPA entities / 894 distinct @Table names · 719 PL/SQL source files · 918 stored-procedure names referenced from Java (1,343 call sites) · 636 frontend routes · 1,308 schemas + 2,943 templates · 40 business modules / 289 features · 28+ external integrations · 6 Solr collections · 35 open unknowns · 40+ risks**.

The system must be modernized because the stack is end-of-life and structurally unsafe: CXF 3.1.0 (2015) on Spring Boot 2.7.5 + Java 8, EclipseLink 2.6.4, a frontend that runs business-critical arithmetic client-side, **no backend authorization** (frontend-only RBAC; enforcement commented out; password verification is a no-op; a static-token bypass and a test backdoor exist), 34 plaintext secrets committed, and state-machine ambiguities (magic-number visit statuses split across two tables) frozen into the data model.

**Strategy**: strangler-fig migration, hybrid frontend-first/backend-first, event-first decomposition, gateway fronting both sides, PostgreSQL per-service schemas behind transactional outboxes feeding Kafka, **Solr must remain** (it is the patient-identity query path embedded in transactional flows), **no dual-write** (every table has exactly one owner; the other side reads via API/event/CDC), Wave-gated cutovers W0→W6, full contract-fixture regression harness (L0–L5).

**Program posture**: this is a behavior-preservation program, not a redesign. The API contract, business rules, status codes, number templates, print culture, and integration contracts are the specification.

---

## 2. Research Methodology

- **Phase 0**: repository fingerprint (repository-snapshot.md; counts re-verified 2026-08-14: 3,001 Java / 8,748 frontend / 719 procs / 1,308 schemas / 2,943 templates / 42 fjs-config).
- **Phase 1**: 10 parallel research agents (A01–A10) — raw evidence in `_discovery/agent-01..10/`.
- **Phase 2**: cross-layer reconciliation (reconciliation-notes.md; TRACEABILITY-MATRIX).
- **Phase 3 (this round)**: 6 independent validation agents (V1–V6) re-verified every load-bearing claim directly against `D:\ihms` source. Results: 20 contradiction entries (CONTR-001…020), 5 numeric corrections, 4 new critical security findings. **Every master-plan number below is the corrected value.**
- **Evidence standard**: two-source rule; FACT/INFERENCE/RECOMMENDATION/UNKNOWN labels; confidence levels CONFIRMED…UNKNOWN (evidence-register E-01…E-20).
- **Anti-hallucination**: no dead-code verdicts without evidence (DEAD-CODE-CANDIDATES); no invented rules (BUSINESS-RULES); no guessed statuses (STATUS-CODE-DICTIONARY; UNKNOWN register).

## 3. Legacy System Baseline

| Dimension | Value | Evidence |
|---|---|---|
| Artifact | `com:ohumpasng:3.4` (JAR→WAR packaging) | pom.xml:6-9 |
| Stack | Java 8 · Spring Boot 2.7.5 · CXF JAX-RS 3.1.x · EclipseLink 2.6.4 (Hibernate excluded) · Oracle (ojdbc7) | pom.xml:14,32-35; E-02 |
| Context/port | `/IHMS` : 8080 | application.properties:3-4 |
| Frontend | FAUG (faug-min.js) — schema-driven jQuery SPA, hash routing, custom template language | E-15 |
| Search | Solr Cloud (ZK 10.10.5.151-153), 6 collections, 5 code-bound | E-16 |
| Cache | Redis config present; **active impl = MemoryCacheImpl** (repo default) | E-17/CONTR-008 |
| Search-embedded | Solr writes inside DB transactions; 3-day encounter rolling window | E-16 |
| Jobs | ApplicationJobs 2,968 LOC / 46 @Scheduled + 4 more classes; 28 cron keys + 10 hardcoded | E-18 |
| Async | 68 @Async (SendNotificationServiceImpl 41); executor 20/40/4500 | E-19 |
| Integrations | 28+ with config AND client code (ABDM, HCX, eSanchar, e-Aushadhi, RGHS, Chiranjeevi, SSO, LDAP (disabled), Aadhaar, HL7 (dormant), FHIR 5.7.0, eSign, MinIO (disabled), EWD/EHR, govt schemes) | E-18 |

## 4. Repository Analysis

Topology (annotated) in repository-snapshot.md §3. Key structural facts:
- Single Maven module; source under `src/main/java/com/ohum` (~2,600 files) + vendored org.eclipse / org.abdm / ca.uhn.fhir / com.fasterxml code.
- 155 service files / 135 `*ServiceImpl`; **16 god classes >2,000 LOC**; top: UploadFileServiceImpl 19,941 · NursingServiceImpl 18,119 · PathoOrderServiceImpl 14,629 · MasterCachedServiceImpl 13,331 · InventoryServiceImpl 12,941 (E-13).
- Exactly 2 DAO classes (GenericDAO + GenericQuery); stored-proc usage pervasive (1,343 call sites; E-08).
- Frontend: 42 fjs-configs + root config; 71 module JS controllers; 1,308 schemas (two dialects); 2,943 templates; backend-injected ext.js (E-15).
- Non-Java: generated-procs (719), integration/ (1,562), templates/ (209), artifacts/ (1,071), locales/ (175), scripts/ (Node tooling).
- 17 @Component services unwired from CXF; 1 orphaned JAX-RS resource (ActionNotificationServiceImpl) (CONTR-007).

## 5. Current Architecture (AS-IS)

- **5 REST surfaces** (E-04): (1) CXF JAX-RS bus — 137 beans at `/rest`; (2) CrudOperationsFilter generic CRUD — 659 @CrudApi entities, 7 verbs; (3) IntegrationOperationsFilter report dispatcher — Nashorn reports.js + Velocity generic_reports.vm; (4) `/ext/api` integration gateway — ApiIntegrationMapping + allowedUsers; (5) HAPI FHIR server + Spring MVC `/error`.
- **Transactions**: method-level `@Transactional(rollbackOn=Exception.class)` (1,265 of 1,309); 7 REQUIRES_NEW; **mega-transaction** `AddServiceOrderServiceImpl:2037` — order→bill(CIMS_ADDBILL)→charges→lab→radiology→EMR→doctor-share→item-sale in one tx (HL7 leg disabled) (E-10).
- **Data**: no entities for T_BILL/T_BILLDETAILS/T_ADMISSION/T_VISIT/T_CHARGES — proc/native-SQL only (E-11); T_REGISTRATION* DB-only (UNKNOWN-016); 0 indexes/views/triggers in repo DDL (UNKNOWN-032 live-DB question).
- **AuthN**: opaque UUID-base64 tokens as cache keys; SSO/eMitra/LDAP front-ends; **authZ: none server-side** (E-06/E-07).
- **Search**: Solr write-through in transactions; encounter 3-day rolling window; patient search embedded in registration/admission (E-16).
- Container/component/data-flow diagrams: `architecture/01..05`.

## 6. Business Capability Map

40 modules M01–M40 (02-business-module-catalog.md; MODULE-INVENTORY.md) grouped into capability domains:

| Domain | Modules (M#) | Tier |
|---|---|---|
| Patient identity | Patient Management, Registration, OPD | T0/T1 |
| Care delivery | OPD, IPD, ADT, Nursing, OT, Chemotherapy, Careplan, Charting | T1 |
| Clinical support | Pathology, Radiology, Blood Bank, Pharmacy, CSSD, Diet, Transport, Linen, BioWaste, Equipment | T1/T2 |
| Financial | Billing, Payments, RCM/Insurance, RMRS/PMRD | T1 |
| Supply | Inventory, Procurement | T2 |
| Reference | Masters, Commons | T2/T3 |
| Engagement | Queue, Kiosk, Grievance, FileMgmt | T3 |
| Interop | ABDM/HCX/NHCX, HL7/FHIR, govt schemes, notifications | T1 (interop) |
| Reporting | Reports, BI | T2 |

289 features (FEATURE-MATRIX.md) with end-to-end traces; 17 clinical/financial workflows documented (`workflows/`).

## 7. Complete Module Catalog

M01–M40 master inventory in MODULE-INVENTORY.md: each module → purpose, submodules, screens, APIs, tables, reports, Solr/Redis usage, target service S##, complexity, risks. Module-level deep-dives: `modules/<module>/` (README, features, business-rules, database, APIs, screens, integrations, security, migration, tests).

## 8. Feature Inventory

289 features F### (FEATURE-MATRIX.md), each mapped screen → API → BR → tables → Solr/Redis → integration → target service. Per-wave feature sets defined in MIGRATION-WAVES §3 and EXECUTION-BACKLOG MIG-XXX work packages.

## 9. Frontend / FAUG Analysis

- Engine: faug-min.js (360,613 B) — schema-driven rendering, custom templatize, crudop, hash routing, onBeforeRouteChange permission gate (E-15).
- **636 routes** (635 unique) in fjs-config.json `router.routes` — one case-variant duplicate is a data defect to fix (CONTR-004).
- 1,308 schemas in two dialects: classic (serializeTemplate/viewer/crud.callbacks) and JSON-schema style (properties/dynamicAspects). dynamicAspects in 40 files, conditionals on injected globals (EXT_DATA).
- 2,943 templates in custom `<%...%>`/`#var/#for/#if` language — transpiler required (CONTR-015).
- Business logic in JS: billing totals/concessions (billing-module.js:2094-2111,1628,1383-1396), role gating (common-module.js:3389-3427), OTP flows, print/barcode.
- Detailed: 03-legacy-frontend-inventory.md, 04-route inventory, 09-react-migration-design.md.

## 10. Backend / CXF Analysis

- 04a-backend-architecture.md + 04b-api-catalog.md: bus wiring, layering, transactions, jobs, aspects, god classes, debt.
- Corrected numbers (CONTR-005): 3,041 method-level verbs / 3,167 @Path; 270 endpoints MasterCachedServiceImpl; 137 CXF beans; 132 JAX-RS classes.
- ServiceLayerAspect: auth/audit/DAO/cache advice — the single most important extension point (authz restore anchor).
- 1,343 namedStoredProcQ call sites; 918 unique proc names (CONTR-001).

## 11. Oracle / Stored Procedure Analysis

- 719 files: 702 procedures + 16 functions + 1 mixed; **no packages**; 646 CIMS_* (CONTR-002).
- 918 proc names referenced from Java (DTOQueries_1..5 metadata registry: 204/202/208/208/110); direct `CIMS_GETcounter` (AddServiceOrderServiceHelper:3627).
- Oracle constructs in Java: ROWNUM 489, SYSDATE 247, DUAL 47, NVL 21, MERGE INTO 4, CONNECT BY 1 (in procs).
- Sequences: 3 active @SequenceGenerator + 50 hardcoded names + 155 in procs + dynamic per-unit names.
- Classification (05-database-catalog §5; DEAD-CODE-CANDIDATES): operational tx / business logic / bulk / integration / reporting / maintenance — per-proc triage required at W0 (UNKNOWN-015).
- **Decision**: convert only procs whose logic is business-critical to Java/plpgsql with golden tests; keep genuinely legacy ones classified (never silently deleted).

## 12. Integration Landscape

28+ integrations, each with config+code evidence (E-18): ABDM (gateway/CM/HIE-CM/HFR/HPR/ABHA/PHR v3), HCX (JWE), eSanchar SMS/WhatsApp (esanchar.wabot.rajasthan.gov.in), e-Aushadhi (4 endpoints, 2 schedules), RGHS (Sims + Docs), Chiranjeevi, Bhamashah, JanAadhaar, e-Mitra, SSO (web+mobile), LDAP (disabled), Aadhaar (sewadwaar), HL7 v2.5 (dormant), FHIR R4 (HAPI 5.7.0, DSTU2/3), eSign, MinIO (disabled), EWD/EHR, PCTS, Mera Aspataal, Jeevanvahini, Kaysons, e-Raktkosh. Full matrix: 07-integrations-and-search.md + 06-integration-catalog.

## 13. Security Landscape

**Current state is a rebuild target, not a port source** (E-06/E-07):
- permitAll(/**), CSRF off, stateless; frontend-only RBAC (2,078 data-roleM keys, UNKNOWN-014).
- Enforcement commented out (ServiceLayerAspect:421-426; IntegrationOperationsFilter:733-738).
- Password verification no-op; test backdoor (test.user.internal.001); static-token bypass (secretTokenForOnlineAppointment); ~50 @NotSecure endpoints as global user 1; portal grants "Administrator"; passwords near-plaintext (char*2-5); 34 plaintext secrets; key files committed; PII in logs/URLs; __token cookie no flags; /ext/api empty-allowlist bypass.
- Audit pipeline exists (AuditHandler/AuditHelper → T_TRX_HISTORY/T_LOGIN_AUDIT/T_ABDM_AUDIT/T_APPOINTMENT_CHANNEL) but attribution is untrustworthy until authN is real.
- Full audit: 08-security-rbac-audit.md + RISK #1/#6/#7 + FINAL-RED-TEAM RT-40…44.

## 14. Infrastructure Landscape

- Build: Maven (build_war.sh), staticweave (EclipseLink), surefire, sonar. Deployment: single WAR, context /IHMS.
- No containers, no CI/CD pipeline in repo (scripts/ Node helpers only); .github/java-upgrade hooks.
- Environment: Oracle 10.10.7.63:1521/orcl (user <REDACTED>), Redis oracle.sms:6379, Solr ZK cluster, NAS paths (int.files.path, document.files.path), print WS localhost:8182.
- Observability: logback only; docit.ctrl heartbeat (UNKNOWN-029); no health/metrics.
- Detailed: 01-system-landscape + OBSERVABILITY.md + FINAL-RED-TEAM RT-60…63.

## 15. Business Rule Catalog

39 rules BR-### (06-business-rules-catalog.md, BUSINESS-RULES.md) + status dictionaries A1–A9 (STATUS-CODE-DICTIONARY.md). Five riskiest cross-layer rules (BR-BILL-005 concession rounding with UNKNOWN-020; tax buckets UNKNOWN-028; visit lifecycle UNKNOWN-001/002; order validation UNKNOWN-004; MRN generation). Rule IDs used in EXECUTION-BACKLOG per work package.

## 16. Dependency Analysis

- DEPENDENCY-GRAPH.md: module DAG (Patient → Visit → Appointment → Clinical → Orders → {Lab, Radiology, Pharmacy} → Billing → Insurance) + shared planes: database (T_ENCOUNTER, T_ADMISSION, T_BILL family, M_ITEMMASTER/T_ITEMSTOCK — V6 writer maps), Solr (in-tx writes), Redis (session cache), external (28 integrations), infra (print/NAS).
- Transaction coupling: mega-transaction (E-10) + 7 REQUIRES_NEW sites (CONTR-019) + 1,309 in-tx call chains define the decomposition constraints.
- False boundaries identified: 40 modules ≠ 29 services (CONTR-016); masters/unit/config boundary needs ownership arbitration (MIG-011).

## 17. Architecture Options

**Option A — Modular Monolith First**: one deployable, modular (domain packages + per-domain PG schemas in one DB instance), gateway+React frontend, outbox in-process. Pros: simplest transaction fidelity (mega-transaction intact), fastest to W1. Cons: no independent scaling; later split re-cost. Risk: LOW-MED. Effort: 0.8× baseline.

**Option B — Domain Microservices (29 services)**: full decomposition per MICROSERVICE-CATALOG. Pros: scaling, team autonomy. Cons: mega-transaction must become saga (highest-risk change in the program); 918-proc port across service boundaries; distributed-monolith risk high without ownership validation. Risk: HIGH. Effort: 1.5×.

**Option C — Hybrid Strangler (RECOMMENDED)**: gateway-fronted strangler; **modular-monolith-shaped domains initially** (patient, encounter/ADT, clinical ordering, financial, supply, reference, interop, reporting as 8 domains, not 29 services); decompose to services only where scaling/security/team boundaries demand; per-domain PG schemas + outbox + Kafka from day one so the later split is mechanical. Preserves mega-transaction as an internal domain transaction; gives event backbone. Risk: MED. Effort: 1.0×.

Comparison detail + evaluation: 10-target-architecture.md, HMIS §17 of blueprint, MICROSERVICE-CATALOG §8 coupling matrix.

## 18. Recommended Target Architecture (PRELIMINARY)

**Hybrid Strangler (Option C) with 8 domain groups → 29 candidate services when justified.**

- Communication: synchronous REST within domain groups; events (Kafka) between domains for identity, encounter, bill, stock, notification, search-index; outbox per domain schema; no dual-write.
- Ownership: every table one owner (V6 writer maps as working input); T_BILL family owned by financial domain (contract-first, E-11); T_ENCOUNTER owned by encounter domain (multi-writer arbitration pending UNKNOWN-001/002); M_ITEMMASTER/T_ITEMSTOCK owned by item domain (MIG-012).
- Persistence: PostgreSQL, schema-per-domain, Flyway, seeded sequences (MIG-006), PII rules, plpgsql where porting is safer than re-implementation.
- Search: Solr remains; outbox→Kafka→indexer replaces in-tx writes (MIG-058) with reconciliation via existing solr_status marker.
- Cache: Redis for tokens/session + idempotency keys + hot masters (NOT primary storage).
- Security: OIDC/OAuth2 (MIG-004), Spring Security 6 resource servers, RBAC extracted from 2,078 data-roleM keys, facility/department/store scoping, default-deny gateway, audit parity.
- ADR index: target-side ADRs (Java 21+ (LTS policy), Spring Boot 3.x, React+TypeScript+Vite, React Hook Form+Zod+TanStack Query, schema-driven forms with 2-dialect transpiler, tenancy, outbox, events E-01…E-12) — decision records to be finalized at W0 per the ADR template in 10-target-architecture.md.

## 19. React Architecture (PRELIMINARY)

- 09-react-migration-design.md: 1:1 screen mapping (636 routes), 37 component families, hooks, design tokens, print tiers (qz-tray WebSocket tier design), mobile (ohumlite/patportal parity), role-gated nav (data-roleM equivalent), schema-driven form engine covering the two FAUG schema dialects, EXT_DATA contract emulation, i18n (CLDR), barcode/QR.
- Strategy: build the schema engine first (W1 pilot on masters), prove archetype coverage, then bulk-convert by archetype (never 2,943 hand-written screens).

## 20. Spring Boot Architecture (PRELIMINARY)

- Target: Spring Boot 3.x + Spring Security 6 + Jakarta EE + Hibernate? — **no**: stay with EclipseLink OR switch JPA provider only after PG dialect validation; EclipseLink 3.x on PG is viable but verify at W0 (persistence POC).
- Replicate 5 routing surfaces at the gateway; services expose clean REST; rollbackOn→rollbackFor translation rule; @Async→spring task executor; scheduled jobs → per-domain scheduler with cron parity table (E-18).
- Contract-first: OpenAPI per domain; CRUD-filter family as generated contract family (654 entities, 7 verbs).

## 21. PostgreSQL Architecture (PRELIMINARY)

- POSTGRESQL-MIGRATION.md: schema-per-service, type mapping, construct catalog (ROWNUM→LIMIT/ROW_NUMBER, SYSDATE→now(), DUAL→drop, NVL→COALESCE, MERGE→INSERT..ON CONFLICT, CONNECT BY→recursive CTE), sequence strategy (seed LAST_NUMBER; dynamic names), PII handling, 894-table mapping ledger (MIG-006), view/materialized-view policies, LOB strategy.
- Do not lift-and-shift bad legacy design (e.g. 1,518-line Encounter entity, 3,960-line ConfigApplication) — target domain schema per domain.

## 22. Solr Architecture (MUST REMAIN)

- Current: 6 collections (patient, encounter, item, service, icd10cm, healthfactor — service unbound in code), ZK, in-tx write-through, 3-day encounter window, solr_status bookkeeping (SolrUtil:955), clearClosedEncountersSync daily.
- Target: transactional outbox → Kafka → indexer → Solr; same collections/schema; query semantics frozen (no boosting/facet changes without business sign-off); outage behavior defined (search degraded with DB fallback flag, never blocking registration).
- Full: 07-integrations-and-search.md + MIG-058 + KAFKA-EVENT-CATALOG E-08/E-09.

## 23. Kafka Architecture (PRELIMINARY — evidence-gated)

- KAFKA-EVENT-CATALOG: 12 accepted events (E-01 patient registered, E-02 encounter created/updated, E-03 order placed, E-04 bill created, E-05 stock movement, E-06 notification, E-07 audit, E-08 search-patient, E-09 search-encounter, E-10 search-item, E-11 search-icd10cm, E-12 search-healthfactor); rejected/deferred candidates recorded.
- Use ONLY for outbox-fed synchronization between domains + search index; partition keys = patientId/encounterId/unitId; ordering per partition; idempotent consumers; DLQ + retry policy per event; no Kafka in request/response paths (CONTR/assumption A12).

## 24. Redis Architecture (PRELIMINARY)

- Use cases (A13): session/token cache (replacing MemoryCacheImpl; UNKNOWN-008 production check), idempotency keys for /ext/api and integration retries, hot masters cache (M_*), short TTL, no primary storage; stampede protection for master cache; invalidation via domain events. Sizing baseline: jedis 200/200/100 (CONTR-012).

## 25. IAM Architecture (PRELIMINARY)

- OIDC/OAuth2 IdP; BFF for SPA; opaque-token legacy client compatibility layer during strangler (tokens still validated by gateway during dual-run); role/permission catalog from data-roleM export (UNKNOWN-014); facility/department/store scopes; default-deny; audit attribution real; secrets in Vault with rotation; "do-not-replicate" list enforced (E-07). Full: 08-security-rbac-audit.md + MIG-004 + RT-40…44.

## 26. Migration Pattern

STRANGLER-MIGRATION.md: gateway routing (per-route to target), shared identity (dual-run token validation), legacy adapters + anti-corruption layers, CDC/outbox data sync (direction per table before W1 — MIG-C04), no permanent dual-write, feature toggles (incl. HL7 legacy flags), rollback = gateway switch-back + per-schema backfill.

## 27. Migration Waves

MIGRATION-WAVES.md (scoring matrix 29 services × 8 dimensions, /40):
- **W0 Foundation**: evidence gates (MIG-001), fixtures (MIG-002), platform+CI/CD+Vault (MIG-003), IAM (MIG-004), gateway 5-surface replication (MIG-005), PG foundation + plpgsql scaffold (MIG-006).
- **W1 Reference/Master Data**: masters (MIG-010/011/012).
- **W2 Operational quick wins**: grievance, files/MinIO, queue/kiosk, transport/linen/diet/biowaste/equipment (MIG-020…023).
- **W3 Clinical spine**: patient identity, appointment/encounter, ADT/bed, ordering mega-transaction, nursing, OT, chemo/careplan (MIG-030…036).
- **W4 Financial**: billing/settlement, payments, RCM/claims, RMRS/PMRD (MIG-040…043).
- **W5 Highly-integrated**: pharmacy, inventory, lab, radiology, blood bank, interop/notifications, govt programs, reporting, Solr pipeline (MIG-050…058).
- **W6 Retirement**: residue, Oracle retirement, decommission, hardening (MIG-060…063).
Exit criteria per wave (§5 of MIGRATION-WAVES); sequencing constraints in EXECUTION-BACKLOG.

## 28. Data Migration Strategy

POSTGRESQL-MIGRATION + STRANGLER §8: per-schema jobs (MIG-C04) with reconciliation queries; sequence seeding; T_REGISTRATION* (UNKNOWN-016) handled at W0 DDL gate; PII encryption at rest; big tables (T_BILL, T_ITEMSTOCK, encounters) chunked CDC; referential mapping ledger; no global cutover — per-domain switch-overs with dual-run parity reports.

## 29. Integration Migration Strategy

Per integration (E-18 matrix): contract fixtures first (MIG-002), sandbox parity tests, credential migration to Vault, `/ext/api` mappings → OAuth-scoped gateway routes (default-deny), retry/queue semantics preserved (WhatsApp retrigger, eAusadh indents, eRaktkosh), ABDM/HCX callback surfaces replicated, FHIR reachability gate (UNKNOWN-009), HL7 behind feature flag (RT-32).

## 30. Testing & Regression Strategy

TEST-STRATEGY.md: L0 (unit) → L1 (contract replay) → L2 (service integration, PG) → L3 (end-to-end with gateway+React) → L4 (performance/soak) → L5 (UAT/clinic pilots). Characterization fixtures from running legacy (MIG-002 — scope expanded to 918-referenced procs, RT-20); golden harnesses for billing math (BR-BILL-001…005), status transitions (MIG-031), mega-transaction failure injection (MIG-033), Solr consistency drills (MIG-058), security regression suite (authN/Z, default-deny). Contract testing per domain; test data strategy (§5 of TEST-STRATEGY; synthetic + de-identified production samples).

## 31. Deployment Strategy

Target: containerized, OpenShift/K8s-ready, GitOps (ArgoCD), image per domain service, gateway, React SPA (static CDN/nginx), stateful sets for Solr/PG/Redis/Kafka; Helm charts; env-per-stage (dev/test/staging/prod-parity); zero-downtime cutover via gateway routing; print tier per node; NAS→object storage migration (MIG-021); cron parity via K8s CronJobs (E-18 table). Full: OBSERVABILITY.md + 08-current-deployment + architecture/05-deployment.

## 32. Observability

OBSERVABILITY.md: log schema + redaction (PII rule from E-07), trace propagation across gateway/services (W3C), metrics (RED/USE) + SLOs, health probes (readiness/liveness per service), docit.ctrl replacement (UNKNOWN-029), audit stream parity (UNKNOWN-035), alerting on Solr lag, queue depth, outbox lag, orphaned sessions; DORA metrics on the platform.

## 33. Rollback / Disaster Recovery

Per-wave rollback = gateway switch-back + legacy unchanged + per-schema backfill (time-boxed, MIG-C03); DR: PG streaming replicas, Solr collections replicas, Kafka geo considerations, RPO/RTO targets to be set at W0; sequence drift monitoring; reconciliation queries per switch-over; kill-switch (MIG-062). Detailed: STRANGLER §8 + MIGRATION-WAVES exit criteria.

## 34. Risk Register

RISK-REGISTER.md — 40+ entries. Top risks (re-ranked post-validation):
1. **Stored-proc port scope** (918 referenced; CONTR-001) — Mitigation: golden fixtures + call-site-density order.
2. **Mega-transaction decomposition** (E-10) — Mitigation: domain-internal transaction first (Option C); saga only if justified.
3. **Solr continuity** (E-16) — Mitigation: frozen query semantics + reconciliation marker.
4. **Security rebuild** (E-07) — Mitigation: IAM first (MIG-004), do-not-replicate list, pen-test gate.
5. **Status/state machines** (UNKNOWN-001/002/003/011) — Mitigation: contract lock at W0 DDL gate.
6. **Client-side billing math** (E-15) — Mitigation: server-computed totals with golden parity.
7. **Entity-less core tables** (E-11) — Mitigation: contract-first modeling.
8. **Deployed-env drift** (UNKNOWN-008/009/010/013) — Mitigation: W0 env audit.
9. **Frontend scale** (636 routes/1,308 schemas/2,943 templates) — Mitigation: schema-engine-first, archetype proof.
10. **Data migration volume** (894 tables) — Mitigation: per-schema jobs + reconciliation.

## 35. Unknowns & Blockers

35 registered unknowns (unknowns-register.md; UNKNOWN-001…035 detail in UNKNOWN-BEHAVIOR.md). Hard blockers before W1: UNKNOWN-001/004/011/014/015/016/031/032 (Oracle DDL + permission semantics), UNKNOWN-008/009/010/013 (deployed env), UNKNOWN-005 (222 URLs). Each maps to MIG-001/MIG-005 acceptance criteria. Resolved in validation round: UNKNOWN-006 (ext.js), directionally UNKNOWN-008 (repo default Memory).

## 36. Execution Backlog

EXECUTION-BACKLOG.md — MIG-001…MIG-063 + cross-cutting MIG-C01…C05, each with objective, legacy scope, target scope, dependencies, files, business rules, DB objects, APIs, frontend, integrations, tests, steps, rollback, risks, acceptance, complexity, blockers. Work packages are the contract between this plan and execution agents.

## 37. Timeline / Sprint Planning (PRELIMINARY)

| Wave | Focus | Duration (est.) | Sprint count (2wk) |
|---|---|---|---|
| W0 | Foundation + gates | 8–12 wk | 4–6 |
| W1 | Reference/masters | 6–8 wk | 3–4 |
| W2 | Operational | 8–10 wk | 4–5 |
| W3 | Clinical spine | 20–28 wk | 10–14 |
| W4 | Financial | 12–16 wk | 6–8 |
| W5 | Integrated | 16–24 wk | 8–12 |
| W6 | Retirement | 8–12 wk | 4–6 |
| **Total** | | **~18–24 months** | ~40–55 |

Timeline is indicative; W0 evidence gates can shorten/lengthen subsequent waves. Milestone M0 = W0 exit; M1 = first clinical domain dual-run (W3 partial); M2 = billing dual-run (W4); M3 = Oracle retirement (W6).

## 38. Definition of Done

Per work package (EXECUTION-BACKLOG acceptance criteria) + per wave (MIGRATION-WAVES exit criteria): (a) contract fixtures green; (b) dual-run parity report ≥ threshold (per-domain KPI); (c) rollback drill passed; (d) security gate passed (no secrets, default-deny, authN real); (e) observability checklist (OBSERVABILITY §10); (f) living-docs updated in same change (README §6); (g) no dual-write introduced; (h) Solr queries unchanged. Wave DONE = all items + register updates + red-team sign-off on that wave's slice.

## 39. Monolith Retirement Criteria

- All 636 routes served by target; legacy WAR serving 0% business traffic (gateway 100% target).
- Oracle retired: all 894 mapped tables copied/final-synced; CDC drained; legacy DB read-only for archive (legal retention), queryable via archive service.
- All 918-referenced procs either ported or classified/archived with fixtures (DEAD-CODE rule: never deleted).
- 28 integrations cut over (per MIG-055/056/057 acceptance); credentials revoked from legacy.
- Solr fully fed by target pipeline; legacy writers removed.
- Security: legacy network egress blocked; secrets purged; pen-test clean.
- Rollback window formally closed with sign-off (gateway switch-back no longer possible).

---

### Appendices (index)
- Research governance: `research/` (plan, evidence E-01…20, assumptions A01…20, contradictions CONTR-001…20, unknowns UNKNOWN-001…35, progress)
- Discovery evidence: `_discovery/` (repository-snapshot, reconciliation-notes, agent-01…10 findings)
- Numbered docs 01–10 · consolidations 11–24 · registers · architecture/ · modules/ · workflows/
- FINAL-RED-TEAM-REVIEW.md (RT-01…74)
- EXECUTION-BACKLOG.md (MIG-001…063)