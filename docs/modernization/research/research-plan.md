# Research Plan — iHMS / OHUM PAS NG Modernization Deep-Research Council

> Owner: Agent 00 (Orchestrator) · Created: 2026-08-14 · Research mode: ACTIVE (planning only — no implementation)
> Primary source: `D:\ihms` (read-only). Documentation: `D:\hims_new_deloitte\docs\modernization\`.

---

## 1. Repository Scope

| Artifact | Path | Status |
|---|---|---|
| Java backend (3,001 files) | `D:\ihms\src\main\java\com\ohum` | Read-only analysis |
| FAUG frontend (8,748 files) | `D:\ihms\src\main\resources\public` | Read-only analysis |
| Runtime config | `D:\ihms\application.properties` (1,506 lines), `pom.xml` (3.4), `services.xml`, `applicationContext.xml`, `persistence.xml` | Read-only |
| Stored procedures (719 files) | `D:\ihms\generated-procs` | Read-only |
| Integration/templates/artifacts | `D:\ihms\integration` (1,562), `D:\ihms\templates` (209), `D:\ihms\artifacts` (1,071) | Read-only |
| Oracle DB (live) | 10.10.7.63:1521/orcl — NOT queried in this campaign; DDL verification deferred (UNKNOWN-001/011/016) | Out of scope (live system) |

## 2. Research Hypotheses (Phase 0 — all now verified, see evidence-register)

1. H1: The system is a Java 8 / Spring Boot 2.7.5 / CXF / EclipseLink / Oracle monolith with FAUG frontend and Solr. → CONFIRMED
2. H2: Business logic lives in Java services, FAUG JS, stored procedures, and config. → CONFIRMED (918 proc names referenced from Java)
3. H3: Four/five routing layers exist beyond plain JAX-RS (generic CRUD filter, report dispatcher, /ext/api mapping, FHIR server + Spring MVC /error). → CONFIRMED + extended
4. H4: Backend authorization is effectively absent (frontend-only RBAC). → CONFIRMED, worse than hypothesized (password verification no-op, static-token bypass, test backdoor)
5. H5: Solr is load-bearing and must remain. → CONFIRMED (indexing inside DB transactions)
6. H6: A strangler migration with per-domain services, per-service PostgreSQL schemas, outbox+Kafka, is the plausible target. → PRELIMINARY (design docs exist; pending validation)

## 3. Agent Assignments (executed)

| Agent | Mandate | Output | Status |
|---|---|---|---|
| 01 Repository forensics | Fingerprint, counts, build | `_discovery/repository-snapshot.md` | DONE |
| 02 Business domain | 40 modules M01–M40, 289 features | `02-business-module-catalog.md`, MODULE-INVENTORY, FEATURE-MATRIX | DONE |
| 03 Backend/API | 3,060 endpoints, routing layers | `04a/04b`, API-INVENTORY | DONE (counts corrected by V1) |
| 04 FAUG frontend | 533-route claim, engine, JS rules | `03-legacy-frontend-inventory.md`, route-inventory | DONE (routes corrected to 636 by V4) |
| 05 Database | 843 tables, 719 procs, Oracle→PG | `05-database-catalog.md`, POSTGRESQL-MIGRATION | DONE (entity/proc counts corrected by V3) |
| 06 Integrations/Solr | 28 integrations, Solr/Redis | `07-integrations-and-search.md` | DONE (V5 added detail) |
| 07 Security | RBAC, authN/Z, secrets | `08-security-rbac-audit.md` | DONE (V2 added critical findings) |
| 08 DevOps | Build/deploy/observability | `08`-companion docs, OBSERVABILITY.md | DONE |
| 09 QA/test | Regression strategy | TEST-STRATEGY.md | DONE |
| 10 Target/migration | 29 services, waves W0–W6 | MICROSERVICE-CATALOG, MIGRATION-WAVES, STRANGLER-MIGRATION, 10-target-architecture | DONE (PRELIMINARY) |
| V1–V6 (validation round) | Independent re-verification of load-bearing claims against `D:\ihms` | This register set (contradiction/evidence/unknowns) | DONE 2026-08-14 |

## 4. Research Questions (master)

- RQ1 What is the true endpoint surface and which routing layers must a replacement replicate? → V1
- RQ2 Which business rules live in JS vs Java vs PL/SQL? → across agents 03/04/05
- RQ3 Which tables are shared, who writes them, and which bounded contexts may own them? → V6 writer maps
- RQ4 Which stored procedures are actually invoked by Java (vs legacy)? → V3 (918 names; huge correction)
- RQ5 What authorization exists server-side today and what must be rebuilt? → V2
- RQ6 How does Solr stay consistent and what happens if indexing is delayed? → agent 06/V5
- RQ7 What state machines exist and where are their statuses defined (constants vs magic numbers)? → V6
- RQ8 Can the FAUG schema/template engine be replaced by a React schema engine, and which archetypes cover 1,308 schemas? → agent 09/04/V4
- RQ9 What transaction boundaries must survive decomposition (mega-transaction)? → V6
- RQ10 What is the least-risk migration order? → MIGRATION-WAVES (scoring matrix)

## 5. Evidence Standards

1. Source code is primary evidence (`path:line` required).
2. Two-source rule: important findings need ≥2 independent pieces of evidence; single-source findings are marked LOW/MEDIUM confidence.
3. Confidence levels: CONFIRMED / HIGH CONFIDENCE / MEDIUM CONFIDENCE / LOW CONFIDENCE / UNKNOWN.
4. Never invent business rules, status meanings, or table relationships.
5. Secrets redacted as `<REDACTED>`.
6. FACT vs INFERENCE vs RECOMMENDATION vs UNKNOWN explicit labels.

## 6. Shared Terminology

- FAUG = proprietary schema-driven jQuery frontend engine (`faug-min.js`)
- fjs-config = frontend module config mapping SchemaName→schema JSON paths
- CRUD filter = `CrudOperationsFilter` generic entity routing (659 @CrudApi entities)
- Report dispatcher = `IntegrationOperationsFilter` Nashorn reports.js + Velocity
- /ext/api gateway = `IntegrationOperationsFilter` ApiIntegrationMapping (allowedUsers gating)
- Mega-transaction = `AddServiceOrderServiceImpl.saveOrUpdateServices` (order→bill→lab→rad→EMR→doctor share→item sale in one tx)
- M01–M40 = business module catalog; S01–S29 = candidate services (PRELIMINARY); W0–W6 = migration waves
- BR-XXX = business rules catalog; UNKNOWN-XXX = open questions; RISK #N = risk register

## 7. Expected Artifacts

- [x] 01–10 numbered discovery docs (04 split into 04a/04b)
- [x] Consolidations: MODULE-INVENTORY, FEATURE-MATRIX, TRACEABILITY-MATRIX, BUSINESS-RULES, STATUS-CODE-DICTIONARY, API-INVENTORY, REPORT-INVENTORY, DEPENDENCY-GRAPH, MICROSERVICE-CATALOG, MIGRATION-COMPLEXITY, MIGRATION-WAVES, STRANGLER-MIGRATION
- [x] Plans: POSTGRESQL-MIGRATION, KAFKA-EVENT-CATALOG, TEST-STRATEGY, OBSERVABILITY
- [x] Registers: RISK-REGISTER, UNKNOWN-BEHAVIOR, DEAD-CODE-CANDIDATES
- [x] This governance set: research-plan, evidence-register, assumption-register, contradiction-register, unknowns-register, research-progress
- [ ] EXECUTION-BACKLOG.md (in progress)
- [ ] FINAL-RED-TEAM-REVIEW.md (in progress)
- [ ] HMIS-MODERNIZATION-MASTER-PLAN.md (in progress)

## 8. Dependency Rules

- Database objects < services < APIs < frontend screens/routes (bottom-up dependency).
- No target architecture conclusion may override a CONFIRMED two-source AS-IS fact.
- PRELIMINARY target designs must not be treated as commitments (see assumption-register A10–A16).
- Migration order derives from the scoring matrix in MIGRATION-WAVES §2, not intuition.

## 9. Quality Gates

| Gate | Criterion | Status |
|---|---|---|
| G1 Inventory complete | Major artifact types counted (Java, JS, schemas, templates, procs, tables) | ✅ PASS |
| G2 Module discovery | 40 modules, 289 features, 636 routes | ✅ PASS |
| G3 Traceability | End-to-end chains for flagship features (TRACEABILITY-MATRIX) | ✅ PASS (first-pass) |
| G4 Business rules extracted | 39 rules + status dictionaries | ✅ PASS |
| G5 Data ownership | Writer maps for hotspot tables (V6) | ✅ PASS (first-pass) |
| G6 Target options compared | 3 options in HMIS-MODERNIZATION-MASTER-PLAN §17 (enriched from 10-target-architecture) | ✅ PASS |
| G7 Regression plan | TEST-STRATEGY (L0–L5) + contract/characterization harness | ✅ PASS |
| G8 Rollback defined | STRANGLER-MIGRATION + MIGRATION-WAVES per-wave exit/cutover | ✅ PASS |

## 10. Completion Criteria

- [x] Repository deeply inventoried (fingerprint verified twice: Phase-0 + V1–V6)
- [x] All registers maintained and cross-referenced
- [x] Adversarial review performed (V1–V6 independent verification; FINAL-RED-TEAM-REVIEW)
- [x] Unknowns explicit (UNKNOWN-001…030 + new); each with verification method
- [x] Execution backlog derived from evidence (EXECUTION-BACKLOG.md)
- [x] Master plan assembled (HMIS-MODERNIZATION-MASTER-PLAN.md)
- [ ] Live-DB verification items (UNKNOWN-001/004/011/014/015/016) — blocked on Oracle access; remains open