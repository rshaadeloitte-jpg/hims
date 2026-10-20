# IMPLEMENTATION-PLAN — iHMS Modernization (Agent 00 Master Plan of Execution)

> **Authority**: HMIS-MODERNIZATION-MASTER-PLAN.md (research complete) · MIGRATION-WAVES.md (execution authority) · EXECUTION-BACKLOG.md (MIG-### work packages) · STRANGLER-MIGRATION.md.
> **Version**: 1.0 · **Date**: 2026-08-14 · **Status**: ACTIVE — Wave 0 in progress.
> Companion files: IMPLEMENTATION-STATUS.md · MIGRATION-TRACKER.md · BLOCKERS.md · DECISIONS.md · TEST-STATUS.md · CUTOVER-STATUS.md.

---

## 1. Current Migration Wave

**WAVE 0 — FOUNDATION & EVIDENCE GATES** (MIG-001…006).

Wave 0 exit criteria (MIGRATION-WAVES §3 W0) — target for this wave:
- [ ] Gateway routes 100% of traffic to legacy with zero behavioral diff (contract parity harness)
- [ ] Login → gateway → legacy token flow end-to-end (opaque bearer preserved)
- [ ] Master cache contract frozen + documented
- [ ] Kafka cluster + outbox + CDC running for 3 pilot tables (idempotent, tested failover)
- [ ] Structured logs carry `meth/uri/corr/rip` semantics; correlation-id spans gateway→legacy
- [ ] Health/readiness + alerting live; docit.ctrl watchdog replaced
- [ ] IAM service extracted (T_USERDETAILS/T_USERROLES/T_USERUNITDETAILS/T_USERSTOREDETAILS/T_USERWARDDETAILS/M_MENUMASTER in PG, roles/grants by API, UserCreated event)
- [ ] CI/CD for new services + feature-flag/release train established

## 2. Modules / Work Packages in Scope (Wave 0)

| WP | Package | Status | Owner |
|---|---|---|---|
| MIG-001 | W0 evidence-gate verification (Oracle + deployed env) | ⛔ BLOCKED (no live Oracle/DB access from dev env) | AGT-09 |
| MIG-002 | Contract fixtures capture from running legacy | ⛔ BLOCKED (no running legacy env) | AGT-07 |
| MIG-003 | Target platform foundation (compose, CI/CD, observability stack) | ◐ IN PROGRESS | AGT-08 |
| MIG-004 | IAM extraction (identity first) | ⛔ BLOCKED (requires MIG-001 env + Keycloak decision) | AGT-05 |
| MIG-005 | API gateway & 5-surface routing replication | ⛔ BLOCKED (requires fixture replay MIG-002) | AGT-01 |
| MIG-006 | PostgreSQL foundation schema & migration tooling | ◐ IN PROGRESS | AGT-02 |
| MIG-058 (seed) | Search & index pipeline foundation (outbox scaffold) | ◐ IN PROGRESS (scaffold only) | AGT-06 |

**Blocking note**: MIG-001/002 require access to the live Oracle 10.10.7.63:1521/orcl and a running legacy instance — not available from this development machine. Per BLOCKERS.md, only those tasks are blocked; everything implementable without live Oracle proceeds.

## 3. First Vertical Slice (selected)

**Slice S0: "Platform + First Domain Service Seed" — W0, dependency-ready.**

Rationale: no slice from W1+ (masters, patient) may start before W0 gates. The first *dependency-ready* executable slice per the approved plan is the W0 foundation itself, executed as a vertical slice:

```
Docker Compose infra (PG16 · Redis · Kafka · Solr)
        ↓
reference-service (Spring Boot 3 / Java 21)
        ↓
ihms_master PostgreSQL schema (Flyway, schema-per-domain, sequence strategy)
        ↓
REST read path (crudget/crudgetAll parity semantics for master entities)
        ↓
Transactional outbox scaffold (event-first backbone)
        ↓
Observability (actuator health, structured logs, correlation id)
        ↓
Tests (unit + integration) → Deployment (container-ready Dockerfile)
```

This validates: Spring Boot 3 / Java 21 / Jakarta / Flyway / PostgreSQL type mapping / outbox pattern / observability / container build — every platform decision W1+ depends on.

**Selection rationale (per §79 criteria):** the slice touches React→API→Domain→DB→Search→Security→Deploy. For S0 the "React" end is replaced by the gateway contract (masters screens are W1, MIG-010); S0 deliberately stops at the read API + observability so the masters frontend (W1) has a tested contract to consume. Search (Solr) wiring is scaffolded via outbox events (MIG-058 pattern) but actual Solr ingestion is W5.

**Slice exit criteria (mini wave-gate):**
- [ ] `docker compose up` brings PG/Redis/Kafka/Solr healthy
- [ ] reference-service compiles, unit + integration tests green
- [ ] ihms_master schema created by Flyway with seeded sequences
- [ ] crudget/crudgetAll endpoints return legacy-shaped payloads (field names 1:1)
- [ ] outbox table populated transactionally; relay publishes to Kafka (integration test)
- [ ] /actuator/health + /actuator/health/liveness + /actuator/health/readiness green
- [ ] structured logs with correlation-id filter
- [ ] Dockerfile multi-stage, non-root, healthcheck
- [ ] no secrets committed (env-driven config only)

## 4. Implementation Dependencies

- MIG-006 (PG foundation) ← none (can start now; legacy DDL evidence from D:\ihms entities + POSTGRESQL-MIGRATION.md)
- MIG-003 (platform) ← none (can start now)
- MIG-058 seed (outbox scaffold) ← MIG-006 (outbox table lives in service schema)
- MIG-010 (W1 masters) ← MIG-006 + MIG-003 + MIG-005 (gateway) + MIG-004 (IAM)
- MIG-004/005 ← MIG-001/002 (fixtures + env access) — currently blocked

## 5. Parallelizable Work (this wave)

| Track | Work | Agent |
|---|---|---|
| A | platform/docker-compose.yml + init + README | AGT-08 |
| B | reference-service skeleton (pom, app, config, actuator) | AGT-01 |
| C | ihms_master Flyway DDL + sequence strategy + entity mapping | AGT-02 |
| D | outbox table + OutboxRepository + relay scaffold | AGT-06 |
| E | tests (unit + integration, H2-free / Testcontainers when Docker up) | AGT-07 |
| F | docs (trackers, DECISIONS.md entries) | AGT-00 |

Tracks A–F are independent except C→B and D→C (outbox table in same schema). B/C/D land in the same module (reference-service) — sequential commits, one owner at a time (file-level allocation rule §40).

## 6. Blocking Dependencies

1. Live Oracle access (MIG-001) — needed for DDL dump, LAST_NUMBER capture, proc dependency graph. **Blocked**: no DB access from dev machine.
2. Running legacy instance (MIG-002) — fixture capture. **Blocked**.
3. Docker daemon — started via Docker Desktop; first `compose up` pulls images (network).

Workaround for 1/2: derive DDL from JPA entity annotations + generated-procs/SEQ_CREATE_SCRIPT.sql in D:\ihms (research-only reads); mark all LIVE-DB-derived acceptance items as deferred with owner (see BLOCKERS.md).

## 7. Database Changes

- New PG schema `ihms_master` (schema-per-domain convention; MIG-006 §3)
- Tables (slice scope, 1:1 quoted legacy names): `M_UNITMASTER`, `M_DEPARTMENTMASTER`, `M_COMPANYMASTER` + `outbox` (event backbone) — names verified against D:\ihms entities in DECISIONS.md
- Flyway: `V1__init_ihms_master.sql` (tables + sequences + audit columns), `V2__seed_master_sequences.sql` (sequence strategy: seed from legacy LAST_NUMBER, BIGSERIAL-free — explicit sequences preserved for MRNO-style continuity)
- Type mapping per POSTGRESQL-MIGRATION.md (NUMBER→BIGINT/NUMERIC, CHAR(1)→CHAR(1), DATE→TIMESTAMP(3) where time semantics)

## 8. API Changes

- New contract (OpenAPI 3): `GET /api/v1/masters/{entity}` and `GET /api/v1/masters/{entity}/{id}` — crudget/crudgetAll parity for master entities (legacy `CrudOperationsFilter` route pattern `{context}/api/rest/<entity>/(all|{id})`); payload field names 1:1 legacy JSON
- Legacy contract unchanged (monolith untouched)
- No breaking changes to any legacy endpoint

## 9. Frontend Changes

- None in S0 (masters React screens are MIG-010 / W1)
- Contract documented for future React client generation

## 10. Integration / Search / Cache Changes

- Kafka: topic `ihms.master.master-data-changed` (E-06 pattern, KAFKA-EVENT-CATALOG) produced via transactional outbox (scaffold)
- Solr: NOT yet wired (W5 MIG-058); outbox event schema prepared with partition key = entity id
- Redis: NOT used in S0 (cache contract freeze is a separate W0 item — documented in DECISIONS.md)

## 11. Security Changes

- Spring Security 6 with default-deny posture: actuator health open, everything else requires bearer token (contract-first authz — legacy has none, MIG-004/RT-40..44)
- No secrets in code: datasource/redis/kafka credentials via environment variables (`.env` for local dev, excluded from git)
- CORS disabled by default; CSRF stateless tokens
- PII: masters data only (no patient data in S0)

## 12. Test Requirements

- L0 unit: repository, use-case, outbox service
- L1 contract: GET /masters/{entity}/all returns legacy-shaped JSON (fixture from D:\ihms GenericServiceImpl shape)
- L2 integration: Flyway migration on PG (Testcontainers when Docker up; fallback H2 is NOT acceptable for PG-specific SQL — slice tests gate on PG via docker), outbox→Kafka round trip
- Observability assertions: correlation-id present in logs, health endpoints 200

## 13. Deployment Strategy

- Container-first: multi-stage Dockerfile (eclipse-temurin:21-jre-alpine, non-root, HEALTHCHECK)
- Local dev: docker-compose (PG/Redis/Kafka/Solr) + `mvn spring-boot:run` / `docker compose up --build`
- CI/CD: GitHub Actions placeholder (W0 exit item, MIG-003) — documented, not yet implemented
- OpenShift/K8s + GitOps deferred to platform wave completion

## 14. Cutover Strategy

- S0 is a *new platform seed*: no traffic to cut over. It establishes the deployable artifact the gateway (MIG-005) will later route to.
- Shadow mode: reference-service runs beside monolith; masters reads stay on legacy until W1 exit criteria.

## 15. Rollback Strategy

- S0 introduces no production traffic; rollback = remove service from compose/registry (no data ownership switched, no legacy write path touched)
- Outbox relay: idempotent consumers, DLQ per topic, replayable from outbox table
- PG schema drop via Flyway `undo` not used — rollback is schema-reset (dev-only) + no data ownership switched

## 16. Definition of Done for S0 (slice gate)

See §3 slice exit criteria; wave-level gates per MIGRATION-WAVES §5 remain the binding authority.
