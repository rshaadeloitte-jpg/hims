# WP-6 — Nursing service (phase-1 slice) — IMPLEMENTATION REPORT

_Service: `services/nursing-service` · package `com.hims.nursing` · artifact `nursing-service` · port **8093** · schema **ihms_nursing**
Scope: GAP-REGISTER-2 WP-6 (SEV Med), MICROSERVICE-CATALOG #12 "Nursing & Inpatient Care — PRELIMINARY", phase-1 slice.
Structural template: services/appointment-service (pom incl. surefire argLine, outbox infra, layering, security, tests)._

## Status: DONE — `mvn -f services/nursing-service/pom.xml test` exit 0 (18/18 tests)

## What was built

| Layer | Files |
|---|---|
| App | `NursingServiceApplication.java` (`@EnableScheduling` for the outbox relay) |
| API | `NursingController`, `GlobalExceptionHandler`, `ApiError` |
| Application | `NursingChartingService` + exceptions `InvalidNursingRequestException` (400), `NursingResourceNotFoundException` (404) |
| Domain | `PatientVitals` (t_patientvitals), `NurseNote` (t_nursenote), `NurseTask` (t_nursetaskdetails), `NursingBase` audit block; Spring Data repositories |
| Infra | Transactional outbox: `OutboxEvent/OutboxService/OutboxRepository/KafkaOutboxRelay/OutboxPublisher` (CAS claim, DLQ topic after maxAttempts) — sibling-identical; `CorrelationIdFilter`; `SecurityConfig` (default-deny) + `DevSecurityConfig` (dev HTTP Basic) |
| Config | `application.yml` (port 8093, schema ihms_nursing, topics `ihms.nursing.events` / `dlt.ihms.nursing.events`), `application-dev.yml` |
| Migration | `V1__init_ihms_nursing.sql` — 3 domain tables + outbox (event_id UNIQUE, jsonb payload, partial PENDING dispatch index) + 3 explicit sequences (IMP-DEC-003) |
| Spec | `openapi/nursing-service.yaml` — all 7 operations, verified 1:1 against controller mappings |

## Endpoints
- `POST /api/v1/patients/{patientId}/vitals` (201; BEXA003-style 400 on missing/non-numeric value or missing vitalsType)
- `GET /api/v1/patients/{patientId}/vitals?from=&to=` (ISO date-time range)
- `POST /api/v1/admissions/{admissionId}/notes`, `GET .../notes`
- `POST /api/v1/tasks` (starts PENDING=1), `GET /api/v1/tasks?status=&admissionId=` (invalid status filter → BEXA003)
- `POST /api/v1/tasks/{id}/complete` (**PENDING→COMPLETED only**, else 400 `NURSING_ILLEGAL_TRANSITION`; 404 unknown task)
- `POST /api/v1/tasks/{id}/cancel` (PENDING→CANCELLED, no event)

## Events — BOTH ARE PROPOSED (per KAFKA-EVENT-CATALOG §3 / catalog #12 "E VitalsRecorded/NurseTaskCompleted (proposed, no consumers)")
- **VitalsRecorded** — type string exactly `'VitalsRecorded'`, aggregate `vitals`, partition key patientId, payload {vitalId, patientId, admissionId, vitalsType, value, unit, recordedAt, recordedBy}. Marked PROPOSED in code comment on `EVENT_VITALS_RECORDED` and in migration/outbox comments; topic name provisional until ratified.
- **NurseTaskCompleted** — type string exactly `'NurseTaskCompleted'`, aggregate `task`, partition key admissionId, payload {taskId, admissionId, taskDesc, frequency, dueAt, completedAt, completedBy}. Marked PROPOSED in code comment on `EVENT_NURSE_TASK_COMPLETED`.
- No consumers assumed in phase 1; contracts may change before ratification.

## Opaque-id boundary (documented in report + code comments)
`patient_id` / `admission_id` are accepted as **opaque ids** in phase 1 — stored WITHOUT FK constraints;
cross-service validation against adt-service (admission exists / belongs to patient / not discharged) is deferred
to the inter-service phase. Documented on all three entities, on `NursingChartingService` javadoc,
in the migration header, in openapi descriptions, and in application.yml info block.

## Deviation from register (deliberate, documented)
Register names the task lifecycle column `status SMALLINT`. It is materialized as **`task_status`**
(Java property `taskStatus`) because the shared sibling audit block already owns a row-active BOOLEAN
column `status` — identical resolution to appointment-service's `appt_status`. Lifecycle codes unchanged:
1=PENDING 2=COMPLETED 3=CANCELLED.

## Tests (18 total, all green)
- L1 unit `NursingChartingServiceTest` (11): BEXA003 validation paths, exact PROPOSED event type strings + payloads via ArgumentCaptor, PENDING→COMPLETED guard (from COMPLETED/CANCELLED → 400), cancel path emits no event, 404s, invalid status filter.
- MockMvc `NursingControllerTest` (6): contract incl. default-deny 401 without auth, ApiError envelopes, transition error mapping.
- L2 integration `NursingServiceIntegrationTest` (1): PG16 Testcontainers (`postgres:16-alpine`, Docker present — executed, not skipped); asserts Flyway applied all 4 tables, full vitals→notes→task flow, persisted `task_status`, illegal re-completion rejected, and **exact outbox rows**: types `VitalsRecorded` + `NurseTaskCompleted`, status PENDING, partition keys, payload contents.

## Verification evidence
- `mvn -f services/nursing-service/pom.xml test` → BUILD SUCCESS, exit 0, Tests run: 18, Failures: 0, Errors: 0, Skipped: 0.
- Grep parity check: every controller `/api/v1/...` mapping present in `openapi/nursing-service.yaml`.

## Blockers
None.
