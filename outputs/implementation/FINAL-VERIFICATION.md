# FINAL VERIFICATION — Implementation Run 2026-08-23
_Verifier: implementation-overseer. Ground truth: outputs/implementation/GAP-REGISTER.md, docs/modernization/MICROSERVICE-CATALOG.md (#1, #2, #10, #22), docs/modernization/KAFKA-EVENT-CATALOG.md §2 (E-01, E-02). All builds/tests below were re-run independently by the overseer, not taken on trust from worker reports._

## Overall verdict: **ALL FOUR WPs PASS Acceptance 1–5 — RUN COMPLETE**

| WP | Service | mvn test (overseer re-run) | Endpoints vs openapi | Flyway / outbox checks | Verdict |
|----|---------|---------------------------|----------------------|------------------------|---------|
| WP-1 | services/iam | BUILD SUCCESS — Tests run: 32, Failures: 0, Errors: 0, Skipped: 0 (incl. IamServiceIntegrationTest 2/2 on real PG16 after fix) | PASS — all 7 spec paths exist in Auth/User/Role controllers: /api/v1/auth/login·logout·validate, /api/v1/users(+/{id},+/{id}/roles), /api/v1/roles | V1__init_ihms_iam.sql: ihms_iam.t_userdetails/t_userrolemaster/t_userroles (BIGSERIAL ids, unit_id, audit cols). JWT HS256 w/ server-side secret (`iam.jwt.secret`, ≥32-byte guard), revoke-list interface + in-memory impl | **PASS** |
| WP-2 | services/item-service | BUILD SUCCESS — Tests run: 43, Failures: 0, Errors: 0 (incl. ItemServiceIntegrationTest 9/9 on real PG16 asserting exact outbox rows) | PASS — /api/v1/suppliers(+/ {id}), /api/v1/grn(+/ {id},/lines), /api/v1/indents(+/approve,/ {id},/lines) present in controllers AND openapi/item-service.yaml | V2__procurement_tables.sql: t_grn, t_grn_detail, t_indent, t_indent_detail per V1 conventions. Outbox types exact: `GRNPosted` (aggregate event per GRN), `IndentApproved` (on approve), per-line `InventoryUpdated` via existing OutboxService; GRN uses SELECT…FOR UPDATE single-writer stock path in ONE tx | **PASS** |
| WP-3 | services/patient-service | BUILD SUCCESS — Tests run: 31, Failures: 0, Errors: 0 (incl. integration 6/6 on real PG16+Kafka) | PASS — POST/GET /api/v1/patients/{id}/sponsors, POST /api/v1/patients/merge in PatientController AND openapi/patient-service.yaml | V2__patient_sponsor.sql ok. Event TYPE strings verified against E-01: exactly `PatientRegistered` / `PatientUpdated` (no drift). Merge re-points documents+sponsors, marks source status=false, emits `PatientMerged` via existing outbox in same tx | **PASS** |
| WP-4 | services/appointment-service | BUILD SUCCESS — Tests run: 17, Failures: 0, Errors: 0, Skipped: 0 (incl. AppointmentServiceIntegrationTest 1/1 on real PG16: migration apply, unique-slot enforcement, full lifecycle, cancel-frees-slot, both outbox events) | PASS — GET /api/v1/doctors/{doctorId}/slots?date=, POST /api/v1/appointments, checkin/complete/cancel/noshow, GET /api/v1/appointments?date=&doctorId= (+schedule endpoints) in controllers AND openapi/appointment-service.yaml | V1__init_ihms_appointment.sql: m_doctor_schedule, t_appointment (status SMALLINT 1..5 documented; partial unique index uq_appointment_slot on schedule_id,appt_date,slot_start for active rows). `EncounterCreated` type string + payload fields match KAFKA-EVENT-CATALOG E-02 exactly (encounterId, patientId, unitId, doctorId, deptId, visitMark, appointmentType, opdNo/token, visitstatus2, clinicId); AppointmentBooked on create — both via transactional outbox | **PASS** |

## Acceptance criteria 1–5 (per register)
1. **mvn test exits 0** — PASS ×4 (see table). No test deleted or disabled to achieve this.
2. **New endpoints in matching openapi/*.yaml** — PASS ×4 (grep-verified path-by-path).
3. **Sibling conventions** — PASS ×4: api/application/domain layering, ApiError + GlobalExceptionHandler, CorrelationIdFilter, SecurityConfig default-deny + DevSecurityConfig, audit columns, explicit-sequence or BIGSERIAL ids per register, transactional outbox wherever events are required.
4. **REPORT files** — PASS ×4: outputs/implementation/WP-{1-iam,2-inventory,3-patient,4-appointment}.md with implementations, file lists, test summaries, justified deviations.
5. **Worker replies to parent** — delivered by all four workers.

## Remediation cycles (both resolved, re-verified by overseer)
- **WP-4 appointment (cycle 1)**: integration test silently skipped — Docker available but docker-java `/info` probe got HTTP 400 from Docker Desktop. Root cause: missing surefire `<argLine>-Dapi.version=1.44</argLine>` (present in patient-service pom). Fix dispatched → applied → 17/17 incl. integration green.
- **WP-1 iam (cycle 1)**: identical root cause. Fix dispatched → applied → 32/32 incl. integration green.

## Documented deviations accepted by overseer (highlights)
- WP-2 D-1: supplier CRUD maps the EXISTING `ihms_item.supplier` (V1) instead of creating duplicate `t_supplier_master` — correct under one-table-one-owner.
- WP-4: partial (active-rows-only) unique-slot index so cancellation frees slots (register's total-unique wording would block rebooking); schema named `ihms_appointment` per register despite catalog #2's eventual `ihms_encounter`.
- WP-3: merge guard error code BEXA012 invented (no STATUS-CODE-DICTIONARY entry yet) — flagged in report for dictionary follow-up.

## Environment notes
- Integration suites ran on real postgres:16-alpine Testcontainers (Docker Desktop present).
