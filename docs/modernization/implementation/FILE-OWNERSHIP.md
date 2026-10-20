# FILE-OWNERSHIP — iHMS Modernization (Agent 00)

> Per system prompt §35: agents must NOT concurrently edit the same source files.
> Serialize changes when two tasks need the same file. Updated: 2026-08-21.

| Path | Owner | Notes |
|---|---|---|
| `docs/modernization/implementation/**` | AGT-00 | Sole writer (trackers, decisions, blockers, status) |
| `docs/modernization/*.md` (research plans) | AGT-00 / AGT-10 | Changes only via ADR + tracker; research docs frozen |
| `platform/**` | AGT-08 | compose, env, README, image pins |
| `.github/workflows/**` | AGT-08 | CI/CD (Podman-based) |
| `services/reference-service/pom.xml` | AGT-01 | Backend engineer |
| `services/reference-service/src/main/java/**/api/**` | AGT-01 | Controllers, error envelope, OpenAPI annotations |
| `services/reference-service/src/main/java/**/application/**` | AGT-01 | Use cases, registry, validation |
| `services/reference-service/src/main/java/**/domain/**` | AGT-01/AGT-02 | Models (AGT-02 reviews mappings) |
| `services/reference-service/src/main/java/**/infrastructure/persistence/**` | AGT-02 | JPA repos, query semantics |
| `services/reference-service/src/main/java/**/infrastructure/outbox/**` | AGT-06 | Outbox/relay/publisher |
| `services/reference-service/src/main/java/**/infrastructure/web/**` | AGT-01 | Filters (correlation) |
| `services/reference-service/src/main/java/**/configuration/**` | AGT-01/AGT-05 | Security (AGT-05 reviews) |
| `services/reference-service/src/main/resources/db/migration/**` | AGT-02 | Flyway — serialize: one writer at a time |
| `services/reference-service/src/main/resources/application*.yml` | AGT-01 | Config |
| `services/reference-service/src/test/**` | AGT-07 | QA owner; AGT-01/02 coordinate |
| `services/reference-service/Dockerfile` | AGT-08 | Container build |
| `services/item-service/**` | AGT-01 (service) / AGT-02 (schema+migrations) / AGT-06 (outbox) / AGT-07 (tests) / AGT-08 (Dockerfile) | MIG-012 extraction (IMP-DEC-026); same role split as reference-service; sole owner of `ihms_item` |
| `services/item-service/src/main/resources/db/migration/**` | AGT-02 | Flyway ihms_item — serialize: one writer at a time |
| `services/grievance-service/**` | AGT-01 (service) / AGT-02 (schema+migrations) / AGT-07 (tests) / AGT-08 (Dockerfile) | MIG-020 extraction (IMP-DEC-032); same role split as item-service minus outbox (REST-only); sole owner of `ihms_grievance` |
| `services/grievance-service/src/main/resources/db/migration/**` | AGT-02 | Flyway ihms_grievance — serialize: one writer at a time |
| `services/waste-service/**` | AGT-01 (service) / AGT-02 (schema+migrations) / AGT-07 (tests) / AGT-08 (Dockerfile) | MIG-023 sub-slice extraction (IMP-DEC-033); same role split as grievance-service (REST-only); sole owner of `ihms_waste` |
| `services/waste-service/src/main/resources/db/migration/**` | AGT-02 | Flyway ihms_waste — serialize: one writer at a time |
| `openapi/waste-service.yaml` | AGT-01 | Generated from running waste-service (`/v3/api-docs.yaml`); regenerate on API change |
| `services/linen-service/**` | AGT-01 (service) / AGT-02 (schema+migrations) / AGT-07 (tests) / AGT-08 (Dockerfile) | MIG-023 sub-slice extraction (IMP-DEC-034); same role split as waste-service (REST-only); sole owner of `ihms_linen` |
| `services/transport-service/**` | AGT-01 (service) / AGT-02 (schema+migrations) / AGT-07 (tests) / AGT-08 (Dockerfile) | MIG-023 sub-slice extraction (IMP-DEC-035); same role split as linen-service (REST-only); sole owner of `ihms_transport` |
| `services/transport-service/src/main/resources/db/migration/**` | AGT-02 | Flyway ihms_transport — serialize: one writer at a time |
| `openapi/transport-service.yaml` | AGT-01 | Generated from running transport-service (`/v3/api-docs.yaml`); regenerate on API change |
| `services/diet-service/**` | AGT-01 (service) / AGT-02 (schema+migrations) / AGT-07 (tests) / AGT-08 (Dockerfile) | MIG-023 sub-slice extraction (IMP-DEC-036); same role split as transport-service (REST-only); sole owner of `ihms_diet` |
| `services/diet-service/src/main/resources/db/migration/**` | AGT-02 | Flyway ihms_diet — serialize: one writer at a time |
| `openapi/diet-service.yaml` | AGT-01 | Generated from running diet-service (`/v3/api-docs.yaml`); regenerate on API change |
| `services/linen-service/src/main/resources/db/migration/**` | AGT-02 | Flyway ihms_linen — serialize: one writer at a time |
| `openapi/linen-service.yaml` | AGT-01 | Generated from running linen-service (`/v3/api-docs.yaml`); regenerate on API change |
| `openapi/**` | AGT-01 | Contract of record (regenerated from running service) |
| `frontend/**` | AGT-03 | React workspace — sole owner (no other agent edits) |
| `outputs/**` | all agents | Read-only evidence/report artifacts; child agents write here |
| `docs/modernization/api/**` | AGT-04 | Interop/API inventory (later waves) |
