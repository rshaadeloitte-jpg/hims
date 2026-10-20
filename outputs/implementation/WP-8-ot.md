# WP-8 — OT & Surgery service (Catalog #11) — IMPLEMENTATION REPORT

Worker: ot-implementer (replacement run) · Date: 2026-08-23 · Register: GAP-REGISTER-3 WP-8 (SEV Med)

## What was built (from scratch — nothing existed)

`services/ot-service` (package `com.hims.ot`, artifact `ot-service`, `server.port 8095`,
schema `ihms_ot`, Flyway `V1__init_ihms_ot.sql`), structurally cloned from
`services/adt-service` including the surefire `argLine -Dapi.version=1.44` Docker fix,
default-deny SecurityConfig + dev-profile HTTP Basic, CorrelationIdFilter, and the
transactional-outbox stack (OutboxEvent/OutboxService/OutboxRepository/OutboxPublisher/KafkaOutboxRelay).

## Data model (ihms_ot)

- `t_otschedule`: schedule_id PK (explicit sequence, IMP-DEC-003), patient_id, encounter_id NULL,
  admission_id NULL, ot_table, scheduled_start, scheduled_end, anesthetist_id, surgeon_id,
  **status SMALLINT 1=PLANNED 2=IN_PROGRESS 3=DONE 4=CANCELLED** (register-pinned column name
  `status`; the shared boolean audit `status` of sibling convention is omitted on this one table
  to avoid the name clash), pre_billing BOOLEAN DEFAULT false, full audit block.
  DB backstops: `ck_t_otschedule_episode` (encounter OR admission NOT NULL), window check.
- `t_otscheduledetail`: detail_id PK, schedule_id FK, procedure_code, procedure_name,
  rate NUMERIC(12,2) DEFAULT 0, **dependent_of BIGINT NULL -> sibling detail row** (deferrable FK
  so a request batch inserts parent+dependent in one tx), discount_pct NUMERIC(5,2) DEFAULT 0
  (CHECK 0..100).
- `outbox` (IMP-DEC-004, sibling DDL incl. event_id UNIQUE + PENDING dispatch partial index).
- Sequences: t_otschedule_seq, t_otscheduledetail_seq.

## APIs

- `POST /api/v1/ot/schedules` — **BR-OT-001**: requires `admissionId` OR `encounterId`,
  else **HTTP 400 code `BEOT004`** (no rows saved, no event). Cross-service validation of the
  referenced ids is DEFERRED — ids are opaque here (documented boundary, same as nursing);
  ADT/IPD owns admission truth. Body carries `procedures[]`; a row with `dependentOfIndex`
  (0-based index of an earlier row in the same request) is a dependent surgery and gets
  `discountPct` applied (rejected on non-dependent rows); `rate` defaults **0** when not supplied
  (tariff lookup deferred to Reference integration). Emits ProcedureScheduled (below).
- `POST /api/v1/ot/schedules/{id}/start|complete|cancel` — lifecycle guards:
  start PLANNED->IN_PROGRESS only; complete IN_PROGRESS->DONE (sets status=3) only;
  cancel PLANNED|IN_PROGRESS->CANCELLED only, never from DONE, double-cancel rejected.
  Illegal transition -> HTTP 400 `OT_ILLEGAL_TRANSITION`. Cancel emits NO outbox event.
- `GET /api/v1/ot/schedules?date=&status=` — worklist (day of scheduled_start + status, both
  optional; JPQL params explicitly CAST to pin PG parameter types).
- `GET /api/v1/ot/schedules/{id}` — full schedule incl. details (netRate = rate after discount).

## BR-OT-002 — pre_billing

`pre_billing` is persisted on create and echoed in every response and in both event payloads.
**The actual direct-charge creation (legacy `consumed=true` procedure-specialization charges,
doctor-share skip) is DEFERRED to Billing (Wave 4)** — documented in
`OtScheduleService` javadoc, `OtSchedule.preBilling` field, V1 SQL column comment, and this report.
This service intentionally never creates charges.

## Events — PROPOSED (not ratified)

- `ProcedureScheduled` on create — type string exactly `ProcedureScheduled`, schema token
  `procedure-scheduled.v1`, topic `ihms.ot.events`, **partition key patientId**.
  Payload keys: {scheduleId, patientId, encounterId, admissionId, otTable, scheduledStart,
  scheduledEnd, surgeonId, anesthetistId, preBilling, procedures[{detailId, procedureCode,
  procedureName, rate, dependentOf, discountPct}], scheduledAt}.
- `SurgeryCompleted` on complete — type string exactly `SurgeryCompleted`, schema token
  `surgery-completed.v1`, same topic, **partition key patientId**. Payload keys: {scheduleId,
  patientId, encounterId, admissionId, completedAt, preBilling, procedureCodes[]}.
- **Both are PROPOSED per KAFKA-EVENT-CATALOG §3** ("ProcedureScheduled/SurgeryCompleted …
  proposed — CSSD/Inventory consume; weak evidence → not in catalog yet"). Marked in
  `OutboxEvents`/`OtEvents` constants, publisher javadoc, V1 SQL header, OpenAPI tag
  description, controller @Operation text, and this report. Promote or drop when consumer
  evidence lands — never silently invented as ratified.

## Tests — `mvn -f services/ot-service/pom.xml test` exit 0 (15 tests, 0 failures, 0 skipped)

- L1 `OtScheduleServiceTest` (9): BEOT004 (no rows, no event); admission-only / encounter-only
  accepted; dependent discount applied + rate default 0 + discount rejected on non-dependent rows;
  BR-OT-002 flag persisted/default-false + echoed in ProcedureScheduled payload; lifecycle guards
  (start only from PLANNED; complete only from IN_PROGRESS; cancel from PLANNED/IN_PROGRESS only,
  never DONE, double-cancel rejected); exact ProcedureScheduled/SurgeryCompleted contracts
  (type strings, aggregate, partition key patientId, idempotency key); cancel emits no event.
- L1 `OtControllerTest` MockMvc (5): default-deny 401s; create 201 with preBilling echoed;
  missing admissionId+encounterId -> 400 `errorCode=BEOT004`; lifecycle guards surface as 400
  `OT_ILLEGAL_TRANSITION`; GET list ?date=&status= + GET /{id}.
- L2 `OtServiceIntegrationTest` (1, PG16 Testcontainers `postgres:16-alpine`, Docker available,
  ran for real): V1 applies on real PostgreSQL; BEOT004 path; dependent-surgery FK + discount
  persisted; **exact outbox rows** asserted — ProcedureScheduled & SurgeryCompleted type strings,
  version v1, aggregate type/id, partition key = patientId, full payload key sets
  (containsExactlyInAnyOrder), producer_service `ot-service`, status PENDING; cancel path adds
  exactly one event (its own ProcedureScheduled, no cancel event); search by date/status; cancel
  after DONE rejected.

## Deferred / boundary notes (explicit, none silently invented)

1. BR-OT-001 cross-service validation of admissionId/encounterId existence — deferred (opaque ids).
2. Procedure rate tariff lookup — deferred to Reference integration (rate defaults 0).
3. BR-OT-002 charge creation — deferred to Billing Wave 4 (flag only here).
4. ProcedureScheduled / SurgeryCompleted — PROPOSED per catalog §3, flagged everywhere.
5. PAS `[re-mapping pending]` per catalog #11 — paths use new `/api/v1/ot/schedules` convention.

## Acceptance mapping (register item 4 / Register-1 items)

- Event TYPE strings, schema tokens, partition keys verbatim vs register/catalog §3. ✔
- 8095 / ihms_ot / com.hims.ot / ot-service. ✔
- openapi/ot-service.yaml written in sibling style (paths match controller 1:1). ✔
- Build green: `mvn -f services/ot-service/pom.xml test` exit 0 incl. PG16 integration. ✔

No blockers.
