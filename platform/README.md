# iHMS Modernization — Platform (Wave 0, MIG-003)

Local development infrastructure for the target platform. See `docs/modernization/implementation/IMPLEMENTATION-PLAN.md` for slice S0 scope.

## Stack

| Service | Image (docker.io, pinned) | Port | Purpose (target state) |
|---|---|---|---|
| postgres | docker.io/library/postgres:16-alpine | 5432 | Transactional DB, schema-per-domain (ihms_master, ihms_patient, ...) |
| redis | docker.io/library/redis:7-alpine | 6379 | Cache ONLY (tokens/session, hot masters, idempotency keys) — never source of truth |
| kafka | docker.io/apache/kafka:3.7.1 (KRaft) | 29092 (host) / 9092 (in-cluster) | Outbox-fed domain events (E-01..E-12 catalog) + DLQ topics `dlt.*` |
| solr | docker.io/library/solr:9.6 (cloud, ZK) | 8983 | Search — MUST remain; 6 legacy collections created at W5 (MIG-058) |
| solr-zk | docker.io/library/zookeeper:3.9 | 9983:2181 | SolrCloud coordination |

## Quick start (Podman — the platform runs on Podman; docker CLI is not required)

The compose file is standard Compose syntax; all images are pinned and pulled
from **docker.io**. Run it with Podman (recommended) or any Docker-compatible
engine:

```bash
# 1. toolchain: podman + a compose provider
podman --version                                   # Podman >= 4 (verified 5.x)
#    compose provider: docker-compose v2 binary (https://github.com/docker/compose)
#    or podman-compose; Podman's docker-compat API socket must be listening:
systemctl --user start podman.socket               # -> /run/user/$UID/podman/podman.sock

# 2. start the stack (images pulled from docker.io automatically)
cp .env.example .env                               # adjust dev credentials (dev-only)
export DOCKER_HOST=unix:///run/user/$(id -u)/podman/podman.sock   # compat engine
docker-compose up -d                               # podman compose up -d also works
docker-compose ps                                  # wait until all healthy (healthchecks below)

# 3. verify each container's health
podman ps --format 'table {{.Names}}\t{{.Status}}'
#    expected: ihms-pg (healthy), ihms-redis (healthy), ihms-kafka (healthy),
#              ihms-solr (healthy), ihms-solr-zk (healthy)

# 4. tear down (reset data)
docker-compose down -v                             # removes named volumes (pg-data, solr-*, ...)
```

> Prior sessions used `docker compose`; the stack is engine-agnostic. Tests in
> reference-service use Testcontainers against the same Podman compat socket
> (`DOCKER_HOST=unix:///run/user/$(id -u)/podman/podman.sock`).

- PostgreSQL: `ihms` / `ihms-dev-only` (dev only) at localhost:5432 — Flyway creates `ihms_master` schema on service startup.
- Kafka bootstrap: **in-cluster** `kafka:9092` (services on the compose network), **host** `localhost:29092` (tools/CLIs). KRaft single node, auto-create topics for dev. (Dual advertised listeners: `PLAINTEXT://kafka:9092`, `PLAINTEXT_HOST://localhost:29092`.)
- SolrCloud requires ZK; create collections only via the Search pipeline (W5) or explicit dev scripts.

## Conventions

- Credentials are dev placeholders; production secrets go to Vault/External Secrets (MIG-003 acceptance).
- No dual-write: each PG schema has exactly one owning service.
- Volumes persist under named volumes (pg-data, solr-data, solr-zk-data); `docker compose down -v` resets.
