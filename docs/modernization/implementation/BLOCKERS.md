# BLOCKERS — iHMS Modernization

> Policy (system prompt §61): block only the affected task; never stop unrelated work. Record here with owner.

**Updated**: 2026-08-20

| ID | Date | Blocker | Affects | Owner | Workaround / Path to unblock |
|---|---|---|---|---|---|
| BLK-001 | 2026-08-14 | No access to live Oracle 10.10.7.63:1521/orcl from dev machine (MIG-001 DDL dump, LAST_NUMBER capture, dependency graph). **Re-verified 2026-08-20 (AGT-00): still UNREACHABLE** (TCP probe, no listener on 1521) | MIG-001 (and MIG-001-gated items) | AGT-09 | Derive DDL from D:\ihms JPA entities + generated-procs/SEQ_CREATE_SCRIPT.sql (research-only). Live-DB acceptance items deferred with owner; re-run MIG-001 when DB access provisioned |
| BLK-002 | 2026-08-14 | No running legacy instance → no contract fixture capture (MIG-002). **Re-verified 2026-08-20 (AGT-00): still no runtime** on localhost:8080 — D:\ihms\logs show the monolith ran locally (Tomcat :8080/IHMS) until 2026-04-30; fixtures recoverable if the WAR is redeployed | MIG-002, MIG-005 (gateway fixture parity) | AGT-07 | Characterize contract shape from GenericServiceImpl/CrudOperationsFilter source + frontend call sites; replace with captured fixtures when instance available |
| BLK-003 | 2026-08-14 | Docker daemon not running on dev machine (Docker Desktop stopped) | S0 integration tests requiring PG | AGT-08 | ✅ **RESOLVED 2026-08-17** — switched container engine to **Podman** (rootless, docker-compat API socket). `docker-compose up` (5/5 healthy), `podman build` image, and Testcontainers PG16/Kafka all green under Podman (IMP-DEC-013). Integration tests no longer gated |
| BLK-004 | 2026-08-14 | First `compose up` requires network image pulls (postgres:16-alpine, redis:7, bitnami/kafka, solr:9) | S0 infra validation | AGT-08 | ✅ **RESOLVED 2026-08-17** — all 5 platform images + Testcontainers images pulled from docker.io (pinned tags recorded in compose) |

## Blocked-item quarantine
Items above are the ONLY blocked tasks. Per policy, all other waves continue unaffected.
