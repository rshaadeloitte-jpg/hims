# WP-7 — Lab (Pathology) service, result-side slice (Catalog #7) — IMPLEMENTATION REPORT

_Worker: lab-implementer · Tranche 3 (GAP-REGISTER-3) · 2026-08-23_

## Deliverable

NEW `services/lab-service` (`com.hims.lab`, artifact `lab-service`, `server.port 8094`,
schema `ihms_lab`, topic `ihms.lab.events` + DLQ `dlt.ihms.lab.events`). Structure is a
sibling clone of `services/adt-service` (latest conventions incl. surefire `-Dapi.version=1.44`
argLine): pom, DevSecurityConfig (profile `dev`, HTTP Basic IMP-DEC-019) + default-deny
SecurityConfig (IMP-DEC-006), GlobalExceptionHandler + ApiError envelope, CorrelationIdFilter,
transactional outbox infra (OutboxEvent / OutboxRepository / OutboxService /
OutboxPublisher w/ CAS claim + DLQ routing / KafkaOutboxRelay), layering
api/application/domain/infrastructure, Flyway `V1__init_ihms_lab.sql`,
`openapi/lab-service.yaml`.

**Scope discipline:** RESULT/WORKLIST side ONLY per register WP-7 — order BOOKING stays
monolith until Wave 4 per MIGRATION-WAVES (noted in V1 header comment + service javadoc).
`t_patientrequisition` is NOT owned here; patho masters stay in Reference (API-borrowed).

## Acceptance items

### 1. `mvn -f services/lab-service/pom.xml test` → exit 0

```
Tests run: 27, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS (exit 0)
  LabControllerTest ............ 9   (MockMvc contract tests, default-deny enforced)
  LabOrderServiceTest ......... 17   (L1 status machine + E-05 contract, no Spring context)
  LabServiceIntegrationTest ...  1   (PG16 Testcontainers — EXECUTED, Docker present)
```

Integration test is `@Testcontainers(disabledWithoutDocker = true)` on `postgres:16-alpine`
with `@ServiceConnection`; it ran to completion (Flyway V1 applied on real PG16).

### 2. Domain (Flyway V1__init_ihms_lab.sql)

- `t_pathorderbooking` (booking_id seq PK, patient_id NOT NULL, encounter_id nullable,
  booking_no UNIQUE, **status_code SMALLINT 1..5 CHECK**, is_out_sourced DEFAULT false,
  finalized_at, audit block). booking_no = `'LAB-' || booking_id`.
- `t_pathorderdetail` (detail_id seq PK, booking_id FK, test_code, test_name,
  detail_status SMALLINT mirrors header, is_final/finalized_at, audit block).
- `t_pathopatientparameterdetails` (param_id seq PK, detail_id FK, parameter_name,
  value VARCHAR, unit, reference_range, is_final DEFAULT false, finalized_at, audit block).
  UNIQUE `(detail_id, parameter_name)` backs the service upsert.
- `t_machineconfiguration` (machine_id seq PK, name, protocol, active BOOLEAN DEFAULT true).
- Transactional outbox (identical DDL to siblings incl. event_id UNIQUE, partial dispatch
  index) + sequences for all four tables (IMP-DEC-003).
- Naming note: the SMALLINT machine column is `status_code`/`detail_status` because the
  shared sibling audit block already owns the boolean `status` column in every table
  (`status BOOLEAN NOT NULL DEFAULT TRUE`). Semantics are exactly the register's
  1=ORDERED 2=SAMPLE_COLLECTED 3=IN_PROGRESS 4=FINALIZED 5=CANCELLED.

**Documented extension:** `t_pathorderdetail.is_final` + `finalized_at` were added beyond
the register's minimal column list so per-detail result finality survives without inventing
a marker table; the finalize endpoint also stamps every parameter row (`is_final=true`,
`finalized_at`) as the register requires.

### 3. APIs & status machine

| Endpoint | Behaviour |
|---|---|
| POST `/api/v1/lab/orders` | patientId required (400 `LAB_REQ_INVALID`), encounterId nullable, ≥1 test line required; creates header ORDERED + details mirrored ORDERED; bookingNo `LAB-<id>` |
| POST `/api/v1/lab/orders/{id}/collect` | 1→2 only; illegal → 400 `LAB_ILLEGAL_TRANSITION`; mirrors onto details |
| POST `/api/v1/lab/orders/{id}/start` | 2→3 only (skipping sample collection rejected); illegal → 400 |
| POST `/api/v1/lab/orders/{id}/cancel` | 1|2|3→5; from FINALIZED/CANCELLED → 400; mirrors onto details |
| POST `/api/v1/lab/orders/{id}/details/{detailId}/results` | upsert keyed (detailId, parameter_name case-insensitive); re-entry replaces value/unit/reference_range and resets is_final; rows absent from payload removed; guards: cancelled/finalized booking → 400, detail of another booking → 400, already-final detail → 400 "locked", empty/duplicate payload → 400 |
| POST `/api/v1/lab/orders/{id}/details/{detailId}/finalize` | requires header IN_PROGRESS (else 400 LAB_ILLEGAL_TRANSITION); detail must belong to booking and not be final; ≥1 parameter required; stamps parameter rows final; when ALL details final → header FINALIZED + finalized_at + **E-05 emitted exactly once** |
| GET `/api/v1/lab/orders?status=&patientId=` | worklist; status accepts numeric (1..5) or name (ORDERED..CANCELLED) |
| GET `/api/v1/lab/orders/{id}` | full booking with details + parameters |
| POST/GET/GET{id}/PUT{id}/DELETE{id} `/api/v1/machines` | machine config CRUD; DELETE is soft-deactivate (active=false) |

Error envelope identical to siblings: `{status:"ERROR", errorCode, message}`; codes
`LAB_REQ_INVALID`, `LAB_ILLEGAL_TRANSITION` (400), `NOT_FOUND` (404).

### 4. E-05 LabResultFinalized (exact KAFKA-EVENT-CATALOG contract)

Emitted via transactional outbox **ONLY when the whole booking finalizes** (last detail);
per-detail finalization emits nothing (asserted by unit test
`finalize_firstOfTwoDetails_noEventYet` and integration step after detail 1).

| Contract item | Value (verbatim) |
|---|---|
| Type string | `LabResultFinalized` |
| Schema token | `lab-result-finalized.v1` (OutboxPublisher.schemaOf mapping, envelope field `schemaName`) |
| Partition key | `encounterId` (`String.valueOf(encounterId)`; integration asserts `"7777"`) |
| Payload keys | EXACTLY `{bookingId, detailIds[], patientId, encounterId, results[{parameterName,value,unit}], status, finalizedAt}` (asserted `containsExactlyInAnyOrder`) |
| Idempotency key | `lab-result-finalized:<bookingId>` |
| Aggregate | type `labOrder`, id bookingId; version v1; producer `lab-service`; topic `ihms.lab.events` |

Documented fallback: when encounterId is NULL the partition key is `no-encounter:<bookingId>`
(outbox partition_key column is NOT NULL; catalog assumes encounter-scoped orders). Flagged
in code comment + here.

### 5. Tests

- Unit `LabOrderServiceTest` (17): creation guards, full status-machine matrix incl. illegal
  transitions, cancel mirrors details, result-upsert guards (cancelled booking, locked
  finalized detail, cross-booking detail, case-insensitive re-entry), finalize-before-start,
  finalize-without-parameters, no-event-before-last-detail, exact E-05 capture via
  ArgumentCaptor (type string, aggregate, partition key, idempotency key, payload keys +
  results shape), null-encounter partition fallback.
- MockMvc `LabControllerTest` (9): default-deny unauth, create 201 with bookingNo/details,
  missing patientId → 400 LAB_REQ_INVALID, illegal transition → 400 code, unknown → 404,
  worklist named+numeric status filters, results upsert, finalize, machine CRUD round-trip.
- Integration `LabServiceIntegrationTest` (PG16, executed): Flyway V1 on real PG16, machine
  CRUD, booking → collect → start → upsert/re-entry → finalize(detail1)=no event →
  finalize(detail2)=header FINALIZED + **exactly one** outbox row asserted field-by-field
  (event_type, version, aggregate_type/id, partition_key='7777', producer_service,
  idempotency_key, PENDING status, exact payload keys/values incl. 3-row results array),
  post-finalize guards emit nothing, worklist filters.

### 6. Deferred / flagged (never silently invented)

- Order BOOKING monolith-side flows (CIMS_ADDPATHORDERBOOKING order entry, requisition,
  tariff/pricing) stay monolith until Wave 4 — MIGRATION-WAVES; noted in V1 header.
- Instrument integrations: eUpkaran, EXT LIMS staging (disabled), Backman OLIS — external
  integrations deferred (Catalog #7); machine-interface auth `[UNKNOWN]` per catalog —
  t_machineconfiguration stores protocol config only (commented in entity + migration).
- WhatsApp report delivery consumer side is Interop's E-05 concern; not produced here.
- Encounter-less bookings get the documented `no-encounter:<bookingId>` partition-key fallback.

## Verification

- `mvn -f services/lab-service/pom.xml test` → **exit 0**, 27/27 green (Docker present;
  PG16 container executed the integration flow end-to-end).
- Event tokens cross-checked against KAFKA-EVENT-CATALOG E-05 verbatim.
