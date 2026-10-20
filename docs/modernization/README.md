# iHMS / OHUM PAS NG — Modernization Documentation (Entry Point)

> **Read this first.** This directory documents the evidence-based modernization program for iHMS ("ohumpasng" v3.4) — the Government-of-Rajasthan hospital management information system currently running as a Spring Boot 2.7.5 / Java 8 / CXF JAX-RS / EclipseLink / Oracle monolith with a FAUG/jQuery frontend. Everything here was produced from discovery of `D:\ihms` (research-only; no source modified) and synthesized with an evidence-first rule: **no statement is invented, unknowns are marked UNKNOWN**.
> **Single-entry master document: [`HMIS-MODERNIZATION-MASTER-PLAN.md`](HMIS-MODERNIZATION-MASTER-PLAN.md)** — the 39-section program plan (research methodology → current state → target architecture → waves → backlog → red-team review), incorporating the 2026-08-14 independent validation round (V1–V6) whose corrections are recorded in `research/contradiction-register.md`. [`HMIS-MODERNIZATION-BLUEPRINT.md`](HMIS-MODERNIZATION-BLUEPRINT.md) remains the 26-section orientation companion.

---

## 1. Executive Overview

iHMS is a single-module hospital management information system serving the Rajasthan government health network through the RISL overlay (e-Mitra, SSO, Jan-Aadhaar, Bhamashah, ABDM/NHCX, e-Aushadi). It is a **monolith of monoliths**: 3,001 Java files, 3,060 REST endpoints behind a single CXF bus, ~843 Oracle tables with 719 stored-proc artifacts, a 533-route schema-driven jQuery frontend engine (FAUG), Solr Cloud as the load-bearing patient-search backbone, and 28 external integrations (ABDM/HCX/HL7/FHIR/RGHS/e-Aushadhi and more). Discovery — completed in this directory — measured the system at 40 business modules, 289 traceable features, 39 documented business rules, and 132 service classes, with **four distinct routing layers** (explicit JAX-RS, a 659-entity generic CRUD filter, a Nashorn report dispatcher, and an `/ext/api` integration gateway) that any replacement must replicate.

The system must be modernized because the stack is end-of-life and structurally unsafe: CXF 3.1.0 (2015) on Spring Boot 2.7.5 with Java 8, a frontend that runs business-critical arithmetic (billing totals, concessions) client-side, no backend authorization (RBAC is frontend-only, and the server-side check is commented out), password verification disabled by default configuration, secrets committed to the repository, and state-machine ambiguities (VisitStatus split across two tables, DNR status collisions) frozen into the data model. The modernization is therefore a **behavior-preservation program**, not a redesign: the API contract, business rules, status codes, number templates, and print culture are the specification.

The strategy is a **strangler-fig migration** with an event-first decomposition: a gateway fronting both sides, hybrid frontend-first/backend-first sequencing, PostgreSQL per-service schemas behind transactional outboxes feeding Kafka, and Wave-gated cutovers (W0 foundation → W6 Oracle retirement). Two constraints are non-negotiable: **Solr stays** (it is the patient-identity query path embedded in transactional flows) and **no dual-write** (every table has exactly one owner; the other side reads via API/event/CDC). All target-side designs are explicitly **PRELIMINARY** pending validation; the AS-IS picture is evidence-based and complete to first-pass depth, with open questions and risks registered rather than guessed.

---

## 2. How to Navigate

The documentation set is organized as a **24-point structure**: 10 numbered discovery documents (04 exists as two files) + 14 consolidation/plan documents, plus 5 supporting registers and the raw agent evidence under `_discovery/`. Each entry: one-line description + link.

### Part A — Numbered discovery documents (10)

| # | Document | One-line description |
|---|---|---|
| 01 | [01-system-landscape.md](01-system-landscape.md) | System identity, technology stack, build/deployment model, Spring context, frontend asset map, non-Java source areas |
| 02 | [02-business-module-catalog.md](02-business-module-catalog.md) | The 40 business modules M01–M40 (purpose, submodules, screens, APIs, tables, reports, dependencies) |
| 03 | [03-legacy-frontend-inventory.md](03-legacy-frontend-inventory.md) | FAUG architecture, 533-route screen inventory, JS business-logic hotspots, API consumer map |
| 04a | [04-backend-architecture.md](04-backend-architecture.md) | CXF bus wiring, service/DAO/entity layering, transactions, jobs, aspects, god classes, debt |
| 04b | [04-api-catalog.md](04-api-catalog.md) | Full 3,060-endpoint dump (module roll-ups, duplicates, anomalies, tx hotspots) |
| 05 | [05-database-catalog.md](05-database-catalog.md) | 843 tables, naming conventions, sequences, stored procs, Oracle-SQL hotspots, PG mapping recommendations |
| 06 | [06-business-rules-catalog.md](06-business-rules-catalog.md) | Status-code dictionaries (A1–A9) and 39 business rules BR-* with evidence, plus the 5 riskiest cross-layer rules |
| 07 | [07-integrations-and-search.md](07-integrations-and-search.md) | Solr (MUST REMAIN), Redis key map, the 28 external integrations with failure modes |
| 08 | [08-security-rbac-audit.md](08-security-rbac-audit.md) | Opaque tokens, frontend-only RBAC, audit pipeline, 15 ranked security gaps |
| 09 | [09-react-migration-design.md](09-react-migration-design.md) | Target React SPA: 1:1 screen mapping, 37 components, hooks, design system, print tiers, mobile |
| 10 | [10-target-architecture.md](10-target-architecture.md) | Preliminary target: 29 bounded contexts, ownership, PG principles, communication matrix, waves draft |

### Part B — Consolidation / plan documents (14)

| # | Document | One-line description |
|---|---|---|
| 11 | [MODULE-INVENTORY.md](MODULE-INVENTORY.md) | M01–M40 master inventory — every module enriched with target service, BR-IDs, Solr/Redis, jobs, complexity, risks |
| 12 | [FEATURE-MATRIX.md](FEATURE-MATRIX.md) | 289 feature rows: screen → API → BR → tables → Solr/Redis → integration → target service |
| 13 | [TRACEABILITY-MATRIX.md](TRACEABILITY-MATRIX.md) | End-to-end chains per major feature (F01…): capability → screen → schema → JS → API → Java → SQL → tables → target React/microservice/PG |
| 14 | [BUSINESS-RULES.md](BUSINESS-RULES.md) | All 39 rules consolidated with triggers, evidence, migration requirements, test cases |
| 15 | [STATUS-CODE-DICTIONARY.md](STATUS-CODE-DICTIONARY.md) | Numeric status meanings A1–A9 with state diagrams and verification checklists |
| 16 | [API-INVENTORY.md](API-INVENTORY.md) | Synthesized API surface: module roll-ups, the 4 routing layers, duplicates, consumers, target notes |
| 17 | [REPORT-INVENTORY.md](REPORT-INVENTORY.md) | Report dispatcher mechanics, 50 config keys, print/PDF endpoint families, template inventory, risks |
| 18 | [POSTGRESQL-MIGRATION.md](POSTGRESQL-MIGRATION.md) | Oracle→PG: schema-per-service, type mapping, construct catalog, sequence strategy, PII handling |
| 19 | [KAFKA-EVENT-CATALOG.md](KAFKA-EVENT-CATALOG.md) | 12 accepted events E-01…E-12, rejected/deferred candidates, outbox pattern, poller→event migration |
| 20 | [MICROSERVICE-CATALOG.md](MICROSERVICE-CATALOG.md) | 29 candidate services (PRELIMINARY): capability, owned data, events, dependencies, waves |
| 21 | [DEPENDENCY-GRAPH.md](DEPENDENCY-GRAPH.md) | Shared planes (DB/Solr/Redis/external) and module dependency graphs |
| 22 | [MIGRATION-WAVES.md](MIGRATION-WAVES.md) | W0–W6 wave plan with scoring matrix, exit criteria, sequencing constraints |
| 23 | [MIGRATION-COMPLEXITY.md](MIGRATION-COMPLEXITY.md) | Per-service complexity scores (1–5 × 8 dimensions, totals /40) to calibrate risk budgets |
| 24 | [STRANGLER-MIGRATION.md](STRANGLER-MIGRATION.md) | Strangler strategy: gateway routing, 4-layer ACL replication, CDC protocol, no dual-write, rollback |

### Part C — Supporting registers (5)

| Document | One-line description |
|---|---|
| [RISK-REGISTER.md](RISK-REGISTER.md) | 40 risks with severity/probability, evidence, mitigation (top 20 summarized in blueprint §24) |
| [UNKNOWN-BEHAVIOR.md](UNKNOWN-BEHAVIOR.md) | 30 unresolved behaviors UNKNOWN-001…030 with evidence, impact, verification method |
| [DEAD-CODE-CANDIDATES.md](DEAD-CODE-CANDIDATES.md) | Classification (confirmed/probably/unknown/dead) for procs, beans, jobs, config — nothing deleted |
| [TEST-STRATEGY.md](TEST-STRATEGY.md) | L0–L5 pyramid, contract/characterization/golden harnesses, CI gates, regression priority |
| [OBSERVABILITY.md](OBSERVABILITY.md) | TO-BE logging/tracing/metrics/health, docit.ctrl replacement, audit stream, delivery checklist |

### Part D — Research governance & evidence (do not edit)

- `research/` — the governance registers: `research-plan.md`, `evidence-register.md` (E-01…E-20), `assumption-register.md` (A01…A20), `contradiction-register.md` (CONTR-001…020, incl. the V1–V6 validation corrections), `unknowns-register.md` (UNKNOWN-001…035), `research-progress.md`.
- [EXECUTION-BACKLOG.md](EXECUTION-BACKLOG.md) — MIG-001…MIG-063 work packages (waves W0–W6) with acceptance criteria, rollback, complexity, dependencies.
- [FINAL-RED-TEAM-REVIEW.md](FINAL-RED-TEAM-REVIEW.md) — adversarial review RT-01…RT-74; every CRITICAL/HIGH gap resolved or blocked.
- `_discovery/reconciliation-notes.md` — cross-agent reconciliation: URL matching (2,322 explicit / 1,109 CRUD / 222 unresolved), the 4 routing layers, cross-cutting facts.
- `_discovery/repository-snapshot.md` — Phase-0 size/identity snapshot (3,001 Java files, 8,748 frontend files, etc.).
- `_discovery/agent-01..10/findings.md` + artifacts (`api-endpoints-normalized.txt`, `api-map-per-file.md`, `route-inventory.md`, `recon-unmatched-urls.txt`…) — raw agent evidence.
- `architecture/` (context/container/component/data-flow/deployment), `modules/<module>/` (per-module feature/business-rules/database/API/security/migration docs), `workflows/` — deep-dive companions.
- `workflows/` — **17 clinical/financial workflow traces** (per-flow step tables: screen → JS → API → service/proc → tables, with BR-* IDs): [patient-registration](workflows/patient-registration.md), [appointment](workflows/appointment.md), [opd-consultation](workflows/opd-consultation.md), [admission](workflows/admission.md), [bed-transfer](workflows/bed-transfer.md), [discharge](workflows/discharge.md), [billing](workflows/billing.md), [insurance-rcm](workflows/insurance-rcm.md), [prescription](workflows/prescription.md), [pharmacy-dispensing](workflows/pharmacy-dispensing.md), [lab-order](workflows/lab-order.md), [lab-result](workflows/lab-result.md), [radiology-order](workflows/radiology-order.md), [ot-surgery](workflows/ot-surgery.md), [blood-bank](workflows/blood-bank.md), [inventory](workflows/inventory.md), [nursing](workflows/nursing.md).

---

## 3. Document Dependency Graph (what to read first)

```
01 system-landscape ──► 04a backend + 05 database + 03 frontend        (what it is)
        │                      │                  │
        │                      └──────┬───────────┘
        │                             ▼
        │                 06 business rules + 07 Solr/Redis/integrations + 08 security   (how it behaves)
        │                             │
        │                             ▼
        │                  09 React design + 10 target architecture                      (where it goes — PRELIMINARY)
        │                             │
        ▼                             ▼
  consolidations:  MODULE-INVENTORY · FEATURE-MATRIX · TRACEABILITY-MATRIX · BUSINESS-RULES ·
                   STATUS-CODE-DICTIONARY · API-INVENTORY · REPORT-INVENTORY · DEPENDENCY-GRAPH
        │
        ▼
  plans:  POSTGRESQL-MIGRATION · KAFKA-EVENT-CATALOG · MICROSERVICE-CATALOG ·
          MIGRATION-COMPLEXITY · MIGRATION-WAVES · STRANGLER-MIGRATION
        │
        ▼
  registers: RISK-REGISTER · UNKNOWN-BEHAVIOR · DEAD-CODE-CANDIDATES · TEST-STRATEGY · OBSERVABILITY
```

Recommended reading order: **01 → 04a/04b/05/03 → 06 → 07 → 08 → 09/10 → matrices (11–16) → plans (17–24) → registers**. A new engineer needing orientation in 30 minutes should read **`HMIS-MODERNIZATION-BLUEPRINT.md`** instead and use this README for depth pointers.

---

## 4. Status Legend

| Status | Meaning |
|---|---|
| **AS-IS (evidence-based)** | Statements verified against `D:\ihms` source/config by agents 01–10; each doc cites `file:line` or an evidence dump. First-pass discovery is **complete** across all 10 numbered documents, all 24 consolidation/plan documents, and all 5 registers. |
| **PRELIMINARY (TO-BE)** | Target-side design — React (§09), microservices/ownership/communication (10, MICROSERVICE-CATALOG), PostgreSQL (POSTGRESQL-MIGRATION), Kafka events (KAFKA-EVENT-CATALOG), waves/strangler/deployment/observability. These are first-pass designs under validation: service names, schemas, topics, and wave plans are candidates, not commitments. |
| **UNKNOWN** | Explicitly unresolved (UNKNOWN-001…030 in UNKNOWN-BEHAVIOR.md) — e.g. VisitStatus DB default, deployed cache impl, T_REGISTRATION DDL, proc usage. Never treated as a fact; each carries a verification method. |
| **Numbers** | Scale figures (3,001 Java files, 3,060 endpoints, 843 tables, 719 procs, 533 routes, 289 features, 39 rules, 28 integrations…) are counts from the cited evidence files; small deltas between sources are flagged inline (e.g. 582 vs 609 masters; 165 vs 137 transactionals) and listed in POSTGRESQL-MIGRATION §8. |

**DoD-gating caveat:** the AS-IS docs are discovery-complete, but migration *readiness* requires closing the registers — see §5.

---

## 5. Definition of Done Checklist State

> **Note on source §44:** the source set contains **no §44 "Definition of Done" checklist** — a full-text search for "Definition of Done", "§44" and checklist headings found none. Per the evidence-first rule, no such list is invented here. The closest traceable equivalents are the **Wave exit criteria** (MIGRATION-WAVES §3/§5) and the **observability delivery checklist** (OBSERVABILITY §10). The state below is therefore compiled from those + the registers.

**DONE — discovery phase (all items complete, first-pass):**

| Item | Status | Where |
|---|---|---|
| System landscape, stack, build model | ✅ Done | 01 |
| 40-module catalog + 289-feature matrix + end-to-end traceability | ✅ Done | 02, MODULE-INVENTORY, FEATURE-MATRIX, TRACEABILITY-MATRIX |
| Full backend endpoint catalog (3,060) + routing-layer discovery (4 layers) | ✅ Done | 04a/04b, API-INVENTORY, reconciliation-notes |
| Frontend inventory (533 routes, schemas/templates, JS logic hotspots) | ✅ Done | 03 |
| Database catalog (843 tables, 719 procs, sequences) + PG mapping design | ✅ Done (design PRELIMINARY) | 05, POSTGRESQL-MIGRATION |
| Business rules (39) + status dictionaries | ✅ Done | 06, BUSINESS-RULES, STATUS-CODE-DICTIONARY |
| Solr/Redis/integrations (28) + security audit | ✅ Done | 07, 08 |
| Target designs (React / microservices / Kafka / waves / strangler / testing / observability) | ✅ First-pass authored — **PRELIMINARY, pending validation** | 09, 10, 09–24 |
| Risk register (40) + unknown register (30) + dead-code classification | ✅ Done | RISK-REGISTER, UNKNOWN-BEHAVIOR, DEAD-CODE-CANDIDATES |

**NOT DONE — required before any wave can start (evidence-gated):**

| Item | Evidence gate |
|---|---|
| Oracle-side verification: DDL dump of `T_REGISTRATION*`, `ALL_TAB_COLUMNS`/`user_sequences`/`ALL_DEPENDENCIES`, VisitStatus defaults, M_ModulePermissionMaster rows | UNKNOWN-001/004/011/014/015/016; RISK #11 |
| Deployed-environment verification: cache impl (Memory vs Redis), FhirServer reachability, `int.files.path` report config drift, eSign/OLIS wiring, HL7/EHR dormant state | UNKNOWN-006/007/008/009/010/013; DEAD-CODE §8 |
| Triage of 222 unmatched frontend URLs | UNKNOWN-005; API-INVENTORY §5 |
| Contract/characterization fixtures captured from the running legacy (132 families, flagship screens, 30 live procs) | TEST-STRATEGY §3 |
| Target-side decisions: stock ownership, cache topology, audit stream, multi-unit tenancy, partition/retention, saga design for the mega-transaction | WAVES §6; STRANGLER §8; KAFKA §6 |
| Security remediation (backend authz, password comparator, secrets to vault) | RISK #1/#6/#7 |

**Bottom line:** discovery is complete; **no wave has been executed** — W0 exit criteria are all unmet by definition. The program is at the "planning validated, execution not started" state.

---

## 6. Living-Documentation Rule

**The docs must be updated in the same change as the code.** Every code change, config change, decision, or verification performed during the migration must update the corresponding documentation in the same commit/change-set — the registers and PRELIMINARY designs are part of the deliverable, not a snapshot:

1. **One change = one doc update.** Touching a service, table, schema, route, or rule? Update the affected numbered doc, consolidation doc, and registers in the same change.
2. **Registers move first.** Verification results remove items from `UNKNOWN-BEHAVIOR.md` and `DEAD-CODE-CANDIDATES.md`; new risks and unknowns are appended with the established templates.
3. **PRELIMINARY becomes design as it lands.** When a decision is made (stock ownership, cache topology, saga design, wave sequencing), mark it in the owning plan doc (POSTGRESQL-MIGRATION / KAFKA-EVENT-CATALOG / MICROSERVICE-CATALOG / MIGRATION-WAVES / STRANGLER-MIGRATION) and strip the PRELIMINARY tag for that item.
4. **Never delete evidence.** Removed features, retired endpoints, and dead procs are archived in the registers with the verification performed (DEAD-CODE §8 "do-not-delete rule"); history is the safety net for golden tests.
5. **The blueprint and this README are indexes, not sources.** They must not accrue facts — facts live in the cited documents; the blueprint/README only point to them.

---

*Index generated as part of the modernization documentation program. Master document: `HMIS-MODERNIZATION-BLUEPRINT.md`.*
