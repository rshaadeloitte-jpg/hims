# WP-4 — Appointment & Encounter service (Catalog #2, first Wave-3 service)

Worker: appointment-implementer (WP-4). Scope per GAP-REGISTER WP-4.
Status: **DONE** — `mvn -f services/appointment-service/pom.xml test` exits 0 with 17/17 tests passing (incl. PG16 Testcontainers integration test).

## What was implemented

New service `services/appointment-service` (package `com.hims.appointment`, artifact
`appointment-service`, server.port **8091**, schema **ihms_appointment**), modeled on
services/grievance-service (pom, layering api/application/domain/infrastructure,
SecurityConfig default-deny + DevSecurityConfig HTTP-Basic dev profile,
GlobalExceptionHandler + ApiError, CorrelationIdFilter, explicit-sequence ID style,
audit columns).

### Domain & persistence (Flyway V1__init_ihms_appointment.sql)
- `m_doctor_schedule`: doctor_id, unit_id, day_of_week SMALLINT (ISO 1=MON..7=SUN),
  start/end TIME, slot_minutes (default 15), dept_id/clinic_id; CHECKs on window/dow/slot.
- `t_appointment` (clean target of T_APPOINTMENT; token_no replaces T_PATIENTTOKEN,
  auto per schedule/day): patient_id, doctor_id, schedule_id FK, appt_date DATE,
  slot_start TIME, appt_status SMALLINT with CHECK 1..5, encounter_id.
  - **Unique-slot constraint**: partial unique index `uq_appointment_slot`
    (schedule_id, appt_date, slot_start) WHERE status AND appt_status <> 4 — active rows only,
    so CANCELLED frees the slot for rebooking (documented in migration COMMENT).
  - Appointment status lifecycle: **1=BOOKED 2=CHECKED_IN 3=COMPLETED 4=CANCELLED 5=NO_SHOW**.
    STATUS-CODE-DICTIONARY has no dedicated appointment-status table; its §1 VisitStatus space
    (codes 3..12) is the legacy encounter-flow space shared between T_Encounter/T_Appointment and is
    deliberately NOT reused — choice documented in code comment on the entity and migration header.
- Transactional outbox table (`outbox`, BIGSERIAL identity, event_id UNIQUE — mirrors
  patient-service V1 / IMP-DEC-004), dispatch index.

### APIs (all present in openapi/appointment-service.yaml)
- `POST /api/v1/doctors/{doctorId}/schedules` — create weekly schedule.
- `GET  /api/v1/doctors/schedules?doctorId=` — list schedules.
- `GET  /api/v1/doctors/{doctorId}/slots?date=yyyy-MM-dd` — slot search; expands schedule window
  into slotMinutes grid with booked flags (**BR-OPD-002** slot rules).
- `POST /api/v1/appointments` — booking; validates day-of-week/window/grid alignment and slot-free
  at both service level and DB level (409 APPT_SLOT_TAKEN via handler incl.
  DataIntegrityViolationException race fallback); assigns sequential token per schedule/day;
  BR comment referencing BR-OPD rules doc in code/spec.
- `POST /api/v1/appointments/{id}/checkin|complete|cancel|noshow` — lifecycle transitions:
  checkin 1→2, complete 2→3, cancel 1|2→4, noshow 1→5; illegal transitions → 400.
- `GET  /api/v1/appointments?date=&doctorId=` — day listing.

### Events (Kafka outbox, topic ihms.appointment.events, DLQ dlt.ihms.appointment.events)
- **AppointmentBooked** appended to outbox in the booking transaction.
- **EncounterCreated** appended on checkin — type string and payload exactly match KAFKA-EVENT-CATALOG
  E-02 (`encounterId, patientId, unitId, doctorId, deptId, visitMark, appointmentType, opdNo/token,
  visitstatus2, clinicId`, version/schema encounter-created.v1, partition key = patientId).
  visitstatus2="2" per STATUS-CODE-DICTIONARY §1 legacy walk-in default.
- Relay/publisher/CAS-claim/DLQ machinery copied from patient-service pattern (IMP-DEC-004 envelope).

## Files added
- services/appointment-service/pom.xml (+ full source tree under src/main/java/com/hims/appointment/
  : Application, configuration/SecurityConfig+DevSecurityConfig, api/ApiError+
  GlobalExceptionHandler+AppointmentController+DoctorScheduleController,
  application/AppointmentBookingService + exceptions x3, domain/model x3 + repository x2,
  infrastructure/web/CorrelationIdFilter + infrastructure/outbox x5)
- src/main/resources: application.yml (port 8091, ihms_appointment, kafka topics),
  application-dev.yml, db/migration/V1__init_ihms_appointment.sql
- Tests: api/AppointmentControllerTest (6 MockMvc contract tests), application/AppointmentBookingServiceTest
  (10 unit tests), infrastructure/persistence/AppointmentServiceIntegrationTest (PG16 Testcontainers).
- openapi/appointment-service.yaml (new spec, same style as sibling specs).

## Files changed
None outside services/appointment-service and openapi/appointment-service.yaml.

## Test output summary (post-overseer fix, final)
```
[INFO] Tests run: 6,  Failures: 0, Errors: 0, Skipped: 0 -- AppointmentControllerTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0 -- AppointmentBookingServiceTest
[INFO] Tests run: 1,  Failures: 0, Errors: 0, Skipped: 0 -- AppointmentServiceIntegrationTest
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```
Env-dependent tests: `AppointmentServiceIntegrationTest` (PG16 Testcontainers) RUNS GREEN on real
Docker — it covers V1 migration apply, unique-slot DB enforcement, full lifecycle,
cancel-frees-slot rebooking, and outbox rows for both event types.

Overseer-deficiency fix (recorded): the initial run SKIPPED the integration test with
"Could not find a valid Docker environment" because the pom lacked the sibling surefire block
`<argLine>-Dapi.version=1.44</argLine>` (present in patient-service/grievance-service poms);
docker-java needs api.version pinned for this Docker Desktop or its /info probe returns HTTP 400 and
Testcontainers disables itself. The exact surefire plugin block was added to
services/appointment-service/pom.xml; without Docker the test still auto-skips via
`@Testcontainers(disabledWithoutDocker = true)` (never fails the build on Docker-less machines).

## Deviations from docs (with justification)
1. **Schema name**: catalog #2 says target schema `ihms_encounter` for the full OPD/encounter domain;
   GAP-REGISTER WP-4 explicitly mandates `V1__init_ihms_appointment.sql` with schema `ihms_appointment`.
   Followed the register; renaming/re-partitioning can land with the encounter slice (T_ENCOUNTER
   partitioning is open per catalog).
2. **Partial (not total) unique-slot constraint**: register says unique (schedule_id, appt_date,
   slot_start); implemented as partial unique over ACTIVE rows only so cancellation frees the slot —
   otherwise a cancelled booking could never be rebooked. Documented in migration COMMENT.
3. **EncounterCreated without a T_ENCOUNTER entity**: this slice emits E-02 with encounterId =
   appointment id (encounter_id column stamped); the full T_ENCOUNTER model/partitioning belongs to the
   follow-up encounter slice. Payload fields match the E-02 catalog row exactly.
4. **AppointmentBooked despite KAFKA-EVENT-CATALOG "REJECTED (phase 1)" note**: the register's WP-4
   scope explicitly requires an AppointmentBooked outbox event on create, so it is emitted locally via
   the outbox (no external sync consumer wired). Revisit when queue/token flows split.
5. **Token numbering**: token auto per schedule/day (register wording); legacy global-day tokens are a
   possible later tweak.
