# GAP REGISTER 2 — Wave-3 Tranche (ADT & Bed #3, Nursing #12)
_Generated 2026-08-23 following completion of tranche 1 (see outputs/implementation/FINAL-VERIFICATION.md). Ground truth: MICROSERVICE-CATALOG.md #3/#12, KAFKA-EVENT-CATALOG.md E-03/E-04/E-11, 06-business-rules-catalog.md BR-IPD-001/002/003/005, BR-DIS-001/002/003._
_Structural template: services/appointment-service (outbox infra, layering, security, tests). Ports used: 8080-8091._

## WP-5 — ADT & Bed service absent (Catalog #3) — SEV High
Create NEW services/adt-service, package com.hims.adt, artifact adt-service, server.port **8092**, schema **ihms_adt**, full sibling conventions (pom modeled on appointment-service incl. surefire argLine fix, DevSecurityConfig, GlobalExceptionHandler, ApiError, CorrelationIdFilter, Flyway V1__init_ihms_adt.sql).

Domain & APIs:
- Ward master: m_ward (ward_id PK seq, name, unit_id, ward_type, census flag, audit cols). CRUD: POST/GET/PUT /api/v1/wards.
- Bed status master: m_bed_status_master (bed_status_id SMALLINT, description, isrelease BOOLEAN) seeded 0=Free?/1/3/5/6 semantics from STATUS-CODE-DICTIONARY §A5 (6 = post-discharge). Seed rows in migration.
- Bed master: m_bed (bed_id PK seq, bed_no, ward_id FK, category, building, occupied BOOLEAN DEFAULT false, bed_status_id FK, isnoncensus/isot/ispreop/ispostop BOOLEAN DEFAULT false, audit cols). CRUD: POST/GET/PUT /api/v1/beds; GET /api/v1/beds/available?wardId= returns beds matching BR-IPD-002 free-bed predicate.
- Admission: POST /api/v1/admissions {patientId, encounterId?, doctorId(admitting), admissionType, unitId} implementing:
  * BR-IPD-001: admitting doctor required -> error code BERA022; reject if another ACTIVE admission exists for patient (is_cancel=false AND not discharged) -> BERB013.
  * BR-IPD-002 auto-allocation when bedId omitted: pick free bed (occupied=false AND status=true AND isnoncensus/isot/ispreop/ispostop=false) preferring same ward/category, claim via atomic UPDATE m_bed SET occupied=true WHERE bed_id=? AND occupied=false (CAS; 409 if lost).
  * BR-IPD-003: chosen bed's status must have isrelease=true else BERB005. Optional explicit bedId input path too.
  * ipdNo generated 'IPD-<id>'; row t_admission (admission_id PK seq, patient_id, encounter_id, bed_id, ward_id, unit_id, admission_type, ipd_no, doctor_id, admitted_at, is_cancel BOOLEAN DEFAULT false, discharge fields null, audit cols).
  * Emits **PatientAdmitted (E-03)** via outbox: type string exactly 'PatientAdmitted', schema field patient-admitted.v1, topic ihms.adt.events, partition key patientId, payload EXACTLY {admissionId, patientId, encounterId, bedId, wardId, unitId, admissionType, ipdNo, doctorId, admittedAt}.
- Transfer: POST /api/v1/admissions/{id}/transfer {toBedId} — releases old bed (occupied=false, status per dictionary), claims new via same CAS, updates admission.bed_id/ward_id, emits **BedChanged (E-11)**: type 'BedChanged', schema bed-changed.v1, partition key BEDID (per catalog), payload {bedId, admissionId, oldStatus, newStatus, changedAt}.
- Discharge: POST /api/v1/admissions/{id}/discharge {dischargeType, isExpired} — sets discharge fields on t_admission, releases bed per BR-DIS-003 (occupied=false, bed_status_id=6), emits **PatientDischarged (E-04)**: type 'PatientDischarged', patient-discharged.v1, partition key patientId, payload {dischargeId, patientId, admissionId, encounterId, dischargeType, isExpired, date, balanceDue(null ok phase 1)}; also emit BedChanged for the released bed. Reject discharge of already-discharged/nonexistent admission (404/400).
- GET /api/v1/admissions/{id}, GET /api/v1/admissions?patientId=&active=
- openapi/adt-service.yaml (same style; ALL above paths).
Tests: service-layer unit tests for each BR error path (BERA022/BERB013/BERB005/CAS-conflict) + controller MockMvc + PG16 Testcontainers integration asserting exact outbox payloads/types for E-03/E-04/E-11.

## WP-6 — Nursing service absent (Catalog #12, phase-1 slice) — SEV Med
Create NEW services/nursing-service, package com.hims.nursing, artifact nursing-service, server.port **8093**, schema **ihms_nursing**, sibling conventions, Flyway V1__init_ihms_nursing.sql.

Scope (documented slice of the 300-endpoint module):
- Vitals: t_patientvitals (vital_id PK seq, patient_id, admission_id BIGINT NULL, vitals_type, value NUMERIC(10,2), unit, recorded_at, recorded_by, audit cols). POST /api/v1/patients/{patientId}/vitals, GET /api/v1/patients/{patientId}/vitals?from=&to=. Validation: value numeric + required fields -> BEXA003-style 400; emits **VitalsRecorded** outbox (PROPOSED event per catalog §3 — mark PROPOSED in a code comment and report).
- Notes: t_nursenote (note_id PK seq, admission_id, note TEXT, nurse_id, noted_at, audit cols). POST/GET /api/v1/admissions/{admissionId}/notes.
- Tasks: t_nursetaskdetails (task_id PK seq, admission_id, task_desc, frequency, due_at, status SMALLINT 1=PENDING 2=COMPLETED 3=CANCELLED, audit cols). POST /api/v1/tasks, GET /api/v1/tasks?status=&admissionId=, POST /api/v1/tasks/{id}/complete (PENDING->COMPLETED only; illegal transition 400) emitting **NurseTaskCompleted** outbox (PROPOSED — comment + report).
- Boundary note (report + code comment): admission_id/patient_id accepted as opaque ids in phase 1 — cross-service validation against adt-service deferred to inter-service phase.
- openapi/nursing-service.yaml.
Tests: unit (validation, transitions, event emission) + MockMvc + PG16 integration asserting outbox rows.

## Acceptance (both WPs)
Identical to register 1 items 1–5: mvn test exit 0 (unit; integration executes given Docker), endpoints present in matching openapi yaml, sibling conventions, REPORT at outputs/implementation/WP-{5,6}-*.md, parent reply summary. Event TYPE strings and payload keys MUST match this register verbatim.

## Overseer mandate (tranche 2)
Same as tranche 1: monitor siblings (adt-implementer WP-5, nursing-implementer WP-6) via observation + reports, independently re-run `mvn test` for both services, grep-verify endpoint/spec parity and exact event type strings vs KAFKA-EVENT-CATALOG E-03/E-04/E-11, dispatch precise fixes, update hive/board.md, write outputs/implementation/FINAL-VERIFICATION-2.md, then reply DONE/BLOCKED to parent.