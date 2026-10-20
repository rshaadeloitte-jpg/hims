# WP-10 — Ordering & Prescription service (GAP-REGISTER-4) — DONE
_Date: 2026-08-23. Scope: NEW `services/ordering-service` (Catalog #4, register WP-10). Template: services/lab-service conventions._

## Delivered
- **NEW `services/ordering-service`** — `com.hims.ordering`, artifact `ordering-service`, `server.port 8097`, schema `ihms_clinical`, Spring Boot 3.5.3 / Java 21, same outbox/security/correlation-id conventions as lab-service (WP-7).
- **Flyway `V1__init_ihms_clinical.sql`**:
  - `t_prescription` (prescription_id PK seq, patient_id, encounter_id NULL, admission_id NULL, prescriber_id, ward_id NULL, status_code SMALLINT 1=ACTIVE 2=COMPLETED 3=CANCELLED, full audit block) + DB-level guard `ck_t_prescription_ctx` enforcing encounterId XOR admissionId.
  - `t_prescription_detail` (composite PK (prescription_id, line_no), `UNIQUE(prescription_id,line_no)`, item_id BIGINT NOT NULL, qty NUMERIC(12,2) CHECK qty>0, dose/frequency VARCHAR(50)).
  - `ihms_clinical.outbox` (sibling DDL incl. event_id UNIQUE) + explicit sequences (IMP-DEC-003).
- **APIs** (`/api/v1/prescriptions`): POST (201), GET /{id}, GET ?patientId=&status= (numeric or ACTIVE/COMPLETED/CANCELLED), POST /{id}/cancel.
- **Validation guards** (all 400 `BEXA003`, legacy generic validation code): patientId required; prescriberId required; **>=1 line required**; **encounterId XOR admissionId** (400 when both); per-line itemId required and qty>0 (mirrored by DB CHECK).
- **Cancel guard**: only ACTIVE (1) -> CANCELLED (3); otherwise 400 `ORD_ILLEGAL_TRANSITION`. Cancel emits NO event.
- **E-06 PrescriptionCreated on CREATE ONLY**, same-tx outbox: type string EXACTLY `PrescriptionCreated`, schema token `prescription-created.v1` (OutboxPublisher.schemaOf), topic `ihms.ordering.events` (DLQ `dlt.ihms.ordering.events`), partition key **encounterId**; **documented fallback `'<admissionId>'` when encounterId is NULL** (register/lab-WP-7 pattern) and `'no-context:<id>'` when both are null (column NOT NULL). Payload keys EXACTLY `{prescriptionId, patientId, encounterId, admissionId, lines[{itemId,qty,dose,frequency}], prescriberId, wardId}` — the unsupplied context slot travels as explicit **null** (sibling convention; register's `encounterId|admissionId` slot expanded to both keys, one always null). Idempotency key `prescription-created:<id>`. Ids serialized as strings, qty numeric (NUMERIC(12,2)) — same as lab E-05.
- **openapi/ordering-service.yaml** at repo root (3 paths + schemas, guards and event semantics documented).
- **Deferred, flagged not invented**: service orders / EMR / notes, Lab/Rad OrderCreated, ordering→billing saga wiring (KAFKA-EVENT-CATALOG §3); status 2=COMPLETED reserved (no endpoint yet — completion flow belongs to a later slice).

## Verification
- `mvn -f services/ordering-service/pom.xml test` → **exit 0, 20/20 green**:
  - L0 `PrescriptionServiceTest` (12): all validation guards, cancel guard from CANCELLED/COMPLETED, E-06 emitted exactly once with exact type/partition-key/payload-keys, `<admissionId>` and `no-context:` partition fallbacks, no event on guard rejection.
  - L1 `PrescriptionControllerTest` (7): default-deny 401 on all endpoints, 201 create shape, BEXA003 envelope (not 500), ORD_ILLEGAL_TRANSITION, 404, named/numeric status filter.
  - L2 `OrderingServiceIntegrationTest` (1, PG16 `postgres:16-alpine` Testcontainers, skipped-without-Docker): V1 applies on real PG, full flow, **exact outbox row asserted** (type `PrescriptionCreated`, version v1, aggregateType prescription, partitionKey `7777`, producer `ordering-service`, idempotency key, payload keys exactly + line contents), `<admissionId>` fallback row (`8888`) for an admission-scoped prescription, cancel emits nothing, guards emit nothing.
- Honest scope note: L2 covers the service against real PG16 via Testcontainers; Kafka relay itself is not asserted end-to-end here (same as WP-7 — relay is shared, lab-tested code).

## Files
- `services/ordering-service/pom.xml`, `src/main/java/com/hims/ordering/**` (api/application/configuration/domain/infrastructure), `src/main/resources/application.yml` + `-dev.yml`, `src/main/resources/db/migration/V1__init_ihms_clinical.sql`
- `src/test/java/com/hims/ordering/**` (L0/L1/L2), `openapi/ordering-service.yaml`
