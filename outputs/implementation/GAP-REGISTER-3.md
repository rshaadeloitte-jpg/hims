# GAP REGISTER 3 — Wave-3 Remainder Tranche (Lab #7 result-side, OT #11, Blood Bank #13)
_Generated 2026-08-23. Ground truth: MICROSERVICE-CATALOG.md #7/#11/#13, KAFKA-EVENT-CATALOG.md E-05 (+§3 deferred/proposed lists), 06-business-rules-catalog.md BR-BLD-001..005, BR-OT-001/002. Structural template: services/adt-service (latest conventions incl. surefire argLine). Ports used through 8093._

## WP-7 — Lab result-side slice absent (Catalog #7) — SEV High
NEW services/lab-service, package com.hims.lab, artifact lab-service, server.port **8094**, schema **ihms_lab**, sibling conventions, Flyway V1__init_ihms_lab.sql.
Scope (result/worklist side ONLY — order BOOKING stays monolith until Wave 4 per MIGRATION-WAVES):
- t_pathorderbooking (booking_id PK seq, patient_id, encounter_id, booking_no 'LAB-<id>', status SMALLINT: 1=ORDERED 2=SAMPLE_COLLECTED 3=IN_PROGRESS 4=FINALIZED 5=CANCELLED, is_out_sourced BOOLEAN DEFAULT false, audit cols) + t_pathorderdetail (detail_id, booking_id FK, test_code, test_name, status mirrors header).
- Result parameters: t_pathopatientparameterdetails (param_id PK seq, detail_id FK, parameter_name, value VARCHAR, unit, reference_range, is_final BOOLEAN DEFAULT false, finalized_at, audit cols).
- Machine config: t_machineconfiguration (machine_id PK seq, name, protocol, active BOOLEAN) + CRUD /api/v1/machines.
- APIs: POST /api/v1/lab/orders (creates booking+details; patientId required; encounterId nullable), POST /api/v1/lab/orders/{id}/collect|start|cancel (status guards, illegal transition -> 400), POST /api/v1/lab/orders/{id}/details/{detailId}/results (upsert parameter rows), POST /api/v1/lab/orders/{id}/details/{detailId}/finalize -> marks detail final; when ALL details of booking final -> booking status=FINALIZED and emit **LabResultFinalized (E-05)** via outbox: type string exactly 'LabResultFinalized', schema token 'lab-result-finalized.v1', partition key **encounterId**, payload keys EXACTLY {bookingId, detailIds[], patientId, encounterId, results[{parameterName,value,unit}], status, finalizedAt}.
- GET /api/v1/lab/orders?status=&patientId= worklist; GET /api/v1/lab/orders/{id} full w/ details+parameters.
- openapi/lab-service.yaml.
Tests: unit (status machine, E-05 emission only-on-final-detail, partition key) + MockMvc + PG16 integration asserting exact outbox row.

## WP-8 — OT & Surgery service absent (Catalog #11) — SEV Med
NEW services/ot-service, package com.hims.ot, artifact ot-service, server.port **8095**, schema **ihms_ot**, Flyway V1__init_ihms_ot.sql.
- t_otschedule (schedule_id PK seq, patient_id, encounter_id NULL, admission_id NULL, ot_table, scheduled_start TIMESTAMP, scheduled_end TIMESTAMP, anesthetist_id, surgeon_id, status SMALLINT 1=PLANNED 2=IN_PROGRESS 3=DONE 4=CANCELLED, pre_billing BOOLEAN DEFAULT false, audit cols) + t_otscheduledetail (procedure_code, procedure_name, rate NUMERIC(12,2) DEFAULT 0, dependent_of BIGINT NULL referencing sibling detail for BR-OT-001 discount, discount_pct NUMERIC(5,2) DEFAULT 0).
- POST /api/v1/ot/schedules implements BR-OT-001: caller must pass admissionId OR encounterId else 400 code **BEOT004** (cross-service validation of the referenced id deferred — opaque ids, documented boundary like nursing). Dependent-surgery rows (dependent_of set) get discount_pct applied from request; procedure rate defaults 0 if not supplied (tariff lookup deferred to Reference integration).
- Lifecycle: POST /api/v1/ot/schedules/{id}/start|complete|cancel (guards; complete sets status=3).
- BR-OT-002: pre_billing flag on schedule persisted and echoed in responses; direct-charge creation itself deferred to Billing (Wave 4) — document in code comment + report.
- Events: **ProcedureScheduled** on create and **SurgeryCompleted** on complete via outbox — PROPOSED per catalog §3 (weak consumer evidence): mark in code comments + report; topic ihms.ot.events, partition key patientId.
- GET /api/v1/ot/schedules?date=&status= ; GET /{id}.
- openapi/ot-service.yaml. Tests: unit + MockMvc + PG16 integration asserting outbox rows.

## WP-9 — Blood Bank service absent (Catalog #13) — SEV Med
NEW services/bloodbank-service, package com.hims.bloodbank, artifact bloodbank-service, server.port **8096**, schema **ihms_bloodbank**, Flyway V1__init_ihms_bloodbank.sql.
- Donor: t_blooddonor (donor_id PK seq, name, blood_group, donation_status SMALLINT 1=DNR_NEW 2=DNR_FIT 7=DNR_CANCELLATION 3..6 other stages, last_action_at, audit cols). POST/GET /api/v1/donors; POST /api/v1/donors/{id}/touch (records action timestamp — supports BR-BLD-005 semantics).
- Requisition: t_bloodrequisition (requisition_id PK seq, patient_id, blood_group, ordered_volume_ml INT, requesteddate, sample_collected BOOLEAN DEFAULT false, status SMALLINT **1=new, 2=cross-matched, 3=issued, 10=cancelled/expired** per BR-BLD-001, order_expired BOOLEAN DEFAULT false, audit cols) + details child (component, volume, status mirror). POST /api/v1/requisitions; GET ?status=.
- Bags/components: t_bloodbag (bag_id PK seq, donor_id, blood_group, volume_ml, collected_at, expirydate DATE, crosSMatched BOOLEAN DEFAULT false, isbagexpired BOOLEAN DEFAULT false, issued BOOLEAN DEFAULT false), t_bloodcomponent (component_id, bag_id, type, volume_ml).
- Endpoints implementing BR-BLD-001 volume rules:
  * POST /api/v1/requisitions/{id}/crossmatch {bagId} -> mark bag crossmatched, add volume; if cumulative crossmatched volume >= ordered_volume -> requisition status 2 else stays 1 (409 if bag already crossmatched/issued/expired).
  * POST /api/v1/requisitions/{id}/issue {bagId} -> issued=true; if cumulative issued volume >= ordered_volume -> status 3 else stays current; only crossmatched bags issuable (400 otherwise).
  * POST /api/v1/requisitions/{id}/cancel -> status 10.
  * POST /api/v1/jobs/decross-expired — one-shot job endpoint implementing BR-BLD-002 (crossmatched & unissued & crossMatchedTestDate >= 48h -> decross, requisition back toward 1) + BR-BLD-003 (expirydate = today -> isbagexpired=true unless issued; crossmatched unissued expired -> decross + requisition status recalc). Returns summary counts. NOTE in code/report: legacy runs these as daily crons; scheduling wiring deferred, endpoint kept idempotent.
  * BR-BLD-004: on GET requisition detail, evaluate expiry predicate (requesteddate + validation period <= now AND NOT sample_collected AND status=1 -> status=10, order_expired=true, reason 'Order is expired') — implement as explicit POST /api/v1/jobs/expire-requisitions scan instead of hidden read-side mutation.
  * BR-BLD-005: POST /api/v1/jobs/cancel-stale-donors (DNR_NEW/DNR_FIT with no touch within 72h -> donation_status=7).
- NO BloodRequisitionCreated event emitted (catalog DEFERS it — §3); note this in code comment + report.
- openapi/bloodbank-service.yaml. Tests: unit tests per BR rule incl. partial-cross-match stays 1, full -> 2, issue -> 3, expire/cancel -> 10, decross <48h untouched vs >=48h; PG16 integration.

## Acceptance (all WPs)
Register-1 items 1–5 as before. Event TYPE strings/schema tokens/partition keys MUST match this register verbatim. Deferred/proposed items must be flagged in code comments + reports, never silently invented.

## Overseer mandate (tranche 3)
As before: monitor lab-implementer (WP-7), ot-implementer (WP-8), bloodbank-implementer (WP-9); independently re-run builds; verify spec/controller parity + exact event tokens vs KAFKA-EVENT-CATALOG E-05 and this register; dispatch precise fixes; maintain hive/board.md; write outputs/implementation/FINAL-VERIFICATION-3.md; reply DONE/BLOCKED to parent.