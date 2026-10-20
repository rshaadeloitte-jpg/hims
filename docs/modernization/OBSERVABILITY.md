# iHMS / OHUM PAS NG — Observability Design (Future State, TO-BE)

**Status: PRELIMINARY — target observability design synthesized from legacy evidence. TO-BE components are preliminary proposals.**
**Legacy evidence anchors:** logback-spring.xml (3 appenders, MDC meth/uri/corr/rip) · `application.properties:13` `logging.pattern.console` · `application.properties:802-806` `log.internal.genericquery.statisctics.*` (legacy spelling) · `App.java:273-286` (docit.ctrl watchdog) · AuditHelper (`T_TRX_HISTORY` 40+ variants, `T_LOGIN_AUDIT`, `T_ABDM_AUDIT`) · ApplicationJobs (`perf.current.db.locks`, WhatsApp/ABDM queue jobs, Solr sync jobs) · artifacts/risl solr/redis/server `check.sh` scripts.

---

## 1. Principles

1. **Correlation-id semantics are sacred.** The legacy pipeline already threads a correlation id through MDC (`%X{corr}`) plus method/URI/remote-IP — this is the observation contract operators and support read logs with today; the target must keep the same keys so existing grep patterns keep working across the transition.
2. **Preserve every existing signal** (query statistics, DB-lock perf key, queue depths, job statuses) — modernize the transport, not the meaning.
3. **One pane of glass during coexistence**: legacy monolith AND new services must emit to the same aggregation with the same correlation semantics, or the strangler window is a blind spot.
4. **Everything new is probe-ready**: health, readiness, metrics, audit, business events as first-class deliverables of every wave (Wave-0 exit criterion).

---

## 2. Structured logging

**Legacy (keep):** logback-spring with 3 appenders (CONSOLE, FILE 200MB/30-files/4GB cap, ERROR_FILE) and MDC pattern:
`logging.pattern.console=%d{yyyy-MM-dd}|%d{HH:mm:ss.SSS}|%t|%5p|%logger{25}|M=%X{meth}|R=%X{uri}|C=%X{corr}|R=%X{rip}|%m|%n` (application.properties:13).

**Target (TO-BE):**
- New services: structured JSON logging (logstash-encoder) with the SAME field names: `meth`, `uri`, `corr`, `rip` as first-class fields, plus `service`, `module`, `unitId`, `user`, `traceId`, `spanId`.
- Correlation propagation: gateway generates the correlation id on first entry, passes as `X-Request-ID` (already used for idempotency by w-api-call.js — reuse it) and as `X-Correlation-Id`; the legacy monolith keeps its own MDC population (its aspect threads `UserTrxContextHolder`); a filter on the legacy side maps the incoming header into `%X{corr}` so traces are continuous end-to-end.
- The legacy FILE/ERROR_FILE rollover pattern (200 MB × 30, 4 GB cap) becomes the aggregation retention policy, not a file on disk; committed `logs/` (~500 MB, up to 118 MB/day error files) must be purged and stopped — log dirs out of the repo (agent-01 §7.4).
- Log aggregation: EFK or Loki+Grafana (TO-BE) with the legacy keys as indexed labels; support grepping `M=`/`C=` the way operators do today.

---

## 3. Distributed tracing (TO-BE)

- **W3C `traceparent`** end-to-end: gateway injects; new services (OpenTelemetry SDK) propagate; the legacy monolith gets the **OpenTelemetry Java agent** attached (no code change — its single-threaded-per-request aspect + `UserTrxContextHolder` threading make span correlation tractable) so a browser → gateway → legacy call produces the same trace as browser → gateway → new service.
- Mapping: `corr` ↔ `traceId` (1:1 at the gateway; legacy `corr` becomes the trace root name). This gives the "was the slowness in billing proc or in the new saga?" answer during coexistence.

---

## 4. Metrics

**Legacy signals to preserve (evidence):**
| Signal | Legacy source | Target |
|---|---|---|
| Query statistics | `log.internal.genericquery.statisctics.enable=true` (:802), `size.threshold=100` (:804), `time.threshold=1000` (:806) — slow/large query logging on GenericQuery | Promote to per-service metrics: query count, slow-query histogram (>1s), large-result histogram (>100 rows); keep the same thresholds as alert defaults; keep the legacy spelling of the property in config docs to avoid confusion |
| DB lock monitoring | `perf.current.db.locks` Redis key written by ApplicationJobs (:925-973) | Prometheus gauge for DB lock waiters; alert on trend |
| Cache stats | ehcache-core dep; `MemoryCacheImpl`/`RedisCacheImpl`; `@Cached` TTL management; ~375 master cache keys | Cache hit/miss/eviction per key family (Reference service owns the contract); TTL-reset events tracked |
| Solr ops | per-minute health checks + midnight restart cron + rsync DR (`artifacts/risl/solr-scripts`) | SolrCloud metrics (QPS, latency, commit lag, collection sizes) + proper replication; retire the restart cron (agent-07 §1.7) |
| Redis ops | `artifacts/risl/redis-scripts/check.sh` | Redis metrics (hit rate, keys, latency) per key family |
| Server ops | `server-scripts/check.sh` | Node/host metrics via standard exporters |
| Job health | ~55 cron schedules in application.properties (auto.charges.schedule, ejs.asha.incentive.schedule, eAushadiItemStockUpdate.schedule…) | Scheduled-job registry with per-job success/failure/duration/lag metrics — every migrated job reports its cron identity unchanged |

**Standard stack (TO-BE):** Prometheus + Grafana; OpenTelemetry metrics from new services; a metrics exporter (JMX/Prometheus JMX exporter or Micrometer bridge) for the legacy monolith.

---

## 5. Health checks / liveness / readiness

- **New services:** Spring Boot Actuator — `/health/live` (process) and `/health/ready` (DB, outbox, Kafka producer, Solr dependency in degraded mode, Redis, queue consumer) per service; readiness gates the gateway route flip for that module (a service reports not-ready → gateway keeps/returns traffic to legacy — this is the operational heart of the strangler).
- **Legacy monolith:** expose equivalent live/ready endpoints (the app already has health-ish ops scripts; add actuator-style endpoints to the legacy shard — small, high-value change) so the orchestrator treats both sides uniformly.
- **Per-service dependency degradation semantics** must mirror legacy behavior, not hide it: Solr down = empty search today (agent-07 §1.6.4) — new Search service must report a distinct "degraded: no fallback" state until the DB-fallback decision (agent-07 §1.7) lands, and alert accordingly.

### docit.ctrl watchdog replacement
The legacy kill-switch (`App.main()` writes `docit.ctrl`, polls every 5 s, deletes → clean shutdown, App.java:273-286) maps to the orchestration model (TO-BE):

| Legacy capability | Replacement |
|---|---|
| 5-s poll loop | Orchestrator (K8s/docker-compose — TO-BE) liveness probe on the legacy shard |
| Delete file → graceful shutdown | `/shutdown` (POST, guarded) + graceful drain; orchestrator eviction with same drain semantics |
| Ops kill-switch (kill an instance fast) | `docit.ctrl`-equivalent config flag (e.g., a control file/ConfigMap watched by the legacy shard) retained during transition; retired in Wave 6 |
| Process crash detection | Liveness + restart policies (crash → restart is the modern equivalent of the watchdog's absence-detection) |
| Graceful context close | Readiness gate + connection drain (Oracle pool, Solr, Redis, Kafka consumers) before termination |

---

## 6. Audit logging (map to event-driven audit)

| Legacy table (evidence) | Writer | Target (TO-BE) |
|---|---|---|
| `T_AUDIT` / `T_AUDITEVENT` | `ServiceLayerAspect` audit advice around every `@Path/@WebMethod` (audit/audit of calls; open question agent-10 §7.4) | Audit topic (Kafka) from the outbox; audit store (PG append-only or object store); event-driven, not in-txn |
| `T_LOGIN_AUDIT` (seq `T_LOGIN_AUDIT_SEQ`) | AuditHelper:917 | Login/Logout events from IAM service; alert on failed-login bursts (existing `Login` annotation + `AuditEvent(LOGIN)`) |
| `T_TRX_HISTORY` (seq `t_trx_history_seq`, 40+ raw INSERT variants) | AuditHelper:156-434 | Replace raw inserts with outbox → audit topic → store; preserve `addeddatetime` semantics and the template-driven audit payloads |
| `T_ABDM_AUDIT` (seq `T_ABDM_AUDIT_SEQ`) | AuditHelper:961 | ABDM request/response correlation events from Interop; required for ABDM compliance |
| `T_APPOINTMENT_CHANNEL` (seq `T_APPOINTMENT_CHANNEL_SEQ`) | AuditHelper | Appointment channel events from OPD service |
| `MBase` audit columns on ~750 tables (`ADDEDBY/ADDEDDATETIME/UPDATEDBY/UPDATEDDATETIME/SYNCHRONIZED`) | GenericDAO writes | Keep columns in PG (agent-05 §8) — they are the per-row audit trail; `SYNCHRONIZED` doubles as a CDC/migration flag |

**Rules:** audit events are async (outbox), immutable, and carry `corr`/`traceId`; error codes stay opaque `BEXA*` (agent-04 §9.6) so support tooling keeps working; audit retention is set by legal/ABDM requirements (open question — confirm before Wave 0 exit).

---

## 7. Business event monitoring

The legacy already has business-observable queues — these become Kafka topics + lag/DLQ metrics (agent-10 §4.3):

| Business pipeline | Legacy (evidence) | Target monitoring |
|---|---|---|
| WhatsApp report delivery | `T_WHATSAPPMESSAGEQUEUE` written in billing flow (`AddServiceOrderServiceHelper.java:3926`), drained 15-min window 14:00–07:00 (`ApplicationJobs.java:1611`), max trigger 5, status table | Topic lag, DLQ, delivery-rate SLO (per hospital), trigger-count=5 failures alerted, status-callback anomalies (`IntegrationServiceImpl.java:1462`) |
| ABDM request queue | `T_ABDMREQUESTQUEUE`, 5-s poller, node-sharded (8 servers), per-node counters `ABDM_S/ABDM_F` (`ApplicationJobs.java:2033`) | Consumer lag, per-node success/failure counters (preserve the legacy Redis counter names), gateway timeout alerts |
| HL7 outbox | `T_OUTBOUNDHIS` (47 refs), `RetryCount`, sync writes in txn (`HL7Helper`), inbound pickup job (dormant) | Outbox depth, retry histogram, stuck-message alerts; inbound pickup decision (activate vs retire, agent-07 §3.2) |
| Solr sync failures | `SOLR_STATUS` flags per table, drainers 5-s fixedDelay, nightly `indexMissingPatientsInSolr` window 18:00–07:00 | Sync lag per collection, reindex progress, drift alerts (dual-write window) |
| eAushadhi | ack DTOs + `reTriggerEAushadhiIndent` AOP hook (`ServiceLayerAspect.java:1204`) | Indent ack failure rate, timeout (10s) alerts |
| Scheduled jobs (55 crons) | ApplicationJobs + InventoryJobSchedule | Job registry: success/failure/lag per cron identity (auto.charges, ejs.asha.incentive, eRaktkosh, RGHS SIMS, PCTS, NMC…) |
| Mega-txn→saga (Wave 4) | — | Saga state machine metrics: per-leg duration, compensation counts (bill cancellation rates), stuck-saga alerts |

---

## 8. Alerting (TO-BE)

- **SLOs proposed** (baseline from legacy behavior; calibrate with ops before Wave 0): patient search P95 (Solr) · registration flow latency (has in-txn Solr/HL7 today — target must beat it) · bill settlement latency · WhatsApp delivery success rate · ABDM success rate · CDC lag < threshold · queue depths vs thresholds (the legacy `size.threshold=100`/`time.threshold=1000` values as defaults) · DB lock waiters trend (from `perf.current.db.locks`) · Solr outage (silent empty-search today — make it loud).
- **Alert channels:** PagerDuty-style pages for clinical-path outages (registration, billing, search, dispensing); dashboards for operational trends; integration-specific dashboards for the 28 external integrations (agent-07 §3.3 failure modes: silent degradation list — MinIO disabled, eSign unwired, HL7 inbound disabled, EHR dual-property conflict — each must become a visible state, not a silent one).
- **Watchdog parity:** an "is the monolith alive and processing" synthetic transaction (login → patient search → create temp visit) runs against the legacy shard through the whole strangler window — the docit.ctrl philosophy (fail fast, observable) modernized.

---

## 9. Coexistence observability (strangler-specific)

- Route-level telemetry on the gateway: % traffic to legacy vs new per module, flip events, rollback events — the migration itself is an observed process.
- Dual-read comparison metrics per moved table: row counts, checksum mismatches, drift age — surfaced on the migration dashboard (Wave exit criteria depend on it).
- CDC lag and outbox depth per schema — a silent CDC stall is a divergence bomb.
- Legacy-side tracing (OTel agent) must be load-tested before Wave 3 (high-traffic registration) — the agent must not regress the monolith's latency budget.
- Log-budget guardrails: the 118 MB/day legacy error-log burst must be an alert, not a file (agent-01 §7.4).

---

## 10. Delivery checklist (TO-BE, per wave)

| Item | Wave |
|---|---|
| Structured logs + corr/trace bridge (gateway→legacy) | 0 |
| OTel agent on monolith; traceparent E2E | 0 |
| Prometheus/Grafana + metrics for platform services | 0 |
| Health/readiness endpoints (new + legacy shard); watchdog replacement mapping | 0 |
| Audit pipeline (outbox → audit topic/store; T_* audit parity) | 0 (design), per-module in its wave |
| Business event monitoring for migrated pipelines | each wave (WhatsApp W3, ABDM W3, HL7 W5, eAushadhi W5) |
| Saga observability | Wave 4 |
| Regulatory report SLAs dashboards | Wave 5 |
| Log aggregation cutover; repo log purge | 0–6 (purge at 0) |
