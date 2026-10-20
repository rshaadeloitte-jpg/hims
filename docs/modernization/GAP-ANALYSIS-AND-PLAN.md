# Gap Analysis: Old IHMS vs New HIMS — Implementation Plan

## Summary

Old IHMS (Java monolith, `com.ohum`) has 40 modules (M01–M40). New HIMS has 29 microservices + React frontend. Backend services cover most core domains but many are skeleton-level. Frontend only has `masters` feature. Major gaps exist in frontend pages, integrations, clinical forms, and several backend sub-features.

---

## Gap Matrix

| Module | Old IHMS Feature | New HIMS Status | Gap | Priority |
|--------|-----------------|-----------------|-----|----------|
| M01 Patient Mgmt | Registration, ABHA/Aadhaar, MLC, mother-child, COVID, social work | patient-service exists (register/search/sponsors/merge/docs) | ~~MLC capture~~ DONE, ~~mother-child linking~~ DONE, ~~photo capture~~ DONE, Aadhaar OTP/biometric, COVID tracking, social worker assessment | HIGH |
| M02 OPD | Walk-in, appointments, slots, emergency, doctor check-in, follow-up, telemedicine, kiosk, questionnaire | appointment-service exists (appt CRUD, schedule, slots, checkin/complete/cancel/noshow, encounter) | ~~Walk-in visit flow~~ DONE, ~~emergency registration~~ DONE, ~~follow-up visits~~ DONE, ~~telemedicine rooms~~ DONE, ~~kiosk self-reg~~ DONE, ~~pre-consult questionnaire~~ DONE, ~~OPD card print~~ DONE | HIGH |
| M03 IPD/ADT | Admission, bed mgmt, transfer, discharge, clearance, attendant pass, interim admission | **adt-service M03 V2 (73 tests)** `t_attendant_pass` + `is_interim/tid/death_cause` | ~~Interim admission~~ DONE (`POST /admissions/interim` bed_id NULL `is_interim` `BR-IPD-005` sweep, `admitInterim` `V2`), ~~attendant pass~~ DONE (`POST /admissions/{id}/attendant-pass` `ATT/yy/seq` `ISSUED` + `GET /{id}/attendant-passes` + `POST /attendant-passes/{id}/return` `RETURNED` + `reprint` printCount, `T_ATTENDANTPASSDETAILS` parity), ~~nursing station census~~ DONE (`GET /adt/census` per-ward `totalBeds/occupied/free/cleaning/activeAdmissions` `iscensus` + `interimCount`), ~~TID list~~ DONE (`GET /adt/tids` `findWithTid`, `POST /admissions/{id}/tid`), ~~death handling in discharge~~ DONE (`POST /admissions/{id}/discharge` `isExpired`+`deathDate`+`deathCause`/`deathType` → `t_admission.is_expired/death_date/death_cause`, `GET /adt/deaths`) — `adt-service/src/main/java/com/hims/adt:73 tests` V2 | MEDIUM |
| M04 Billing | OP/IP billing, receipts, payments, advances, refunds, credit notes, estimation, doctor share, package, sponsor change | billing-service + payments-service exist (charges, bills, finalize, concession, cancel, advances, payments, refunds) | ~~Credit notes~~ DONE (`POST /bills/{id}/credit-notes` + V2 migration), ~~Debit notes~~ DONE (`POST /bills/{id}/debit-notes` + approve/apply + V4 migration), ~~Bill estimation~~ DONE (`POST /bills/estimate` stateless), ~~Sponsor change~~ DONE (`PUT /bills/{id}/sponsor`), ~~Package subscription~~ DONE (`POST /bills/{id}/package-subscription`), ~~Partial payment ledger~~ DONE (`t_bill_payment_ledger` V4 + `POST /bills/{id}/payment-ledger`), doctor share/add-deduct payout module (M06 scope), pharmacy billing integration | HIGH | | HIGH |
| M05 Tariff Masters | Service/tariff/package/plan/company/concession/order-set masters | reference-service M05 V6 (91 tests) | ~~Concession templates~~ DONE (`POST /api/v1/masters/concession-templates` + deactivate; V6 `concession_template` table with scheme + maxPercentOff + requiresApproval; 1=ACTIVE 2=INACTIVE), tariff copay logic, package class-wise rates, health plan exclusions, order set definitions | MEDIUM |
| M06 Doctor Share | Share calc, add/deduct, bills, visit charges | NOT IMPLEMENTED | Entire module | LOW |
| M07 Pharmacy | Prescription, dispense (IP/OP/cart-fill), compound/IV, returns, billing, interventions, drug mapping, BCMA, home meds | pharmacy-service exists (prescription CRUD, dispense, interventions, ward picklist, **sale returns + drug allergy/interaction V2**) | ~~Sale returns~~ DONE (`POST /api/v1/sale-returns` initiate/complete/reject; `t_sale_return`+`t_sale_return_line`; BR-PHAR-003 stock-ADD on complete; status 1=INITIATED 2=APPROVED 3=COMPLETED 4=REJECTED), ~~Drug allergy & interactions~~ DONE (`/api/v1/drug-allergies`, `/api/v1/drug-interactions` + acknowledge; BR-PHAR-004 pharmacist-override acknowledged flag), IV compounding, pharmacy billing, drug mapping, BCMA, home meds, ABDM PHR push | HIGH |
| M08 Clinical Orders | Service orders, order sets, investigations, CWF/SCWF, external orders, machine config | ordering-service exists (order/prescription CRUD, saga state machine) | Order sets application, CWF/SCWF workflow, external order details, machine config (KDL) | MEDIUM |
| M09 Nursing | Worklist, coversheet, notes, care plan, vitals, charting, handover, allocation, incident, rostering, allied, mortuary, annotation, diagnosis map | nursing-service exists (vitals, notes, tasks) | Care plans, charting/partograph, shift handover, nurse allocation, incident reporting, rostering, allied health, mortuary view, image annotation, diagnosis/complaint map | HIGH |
| M10 Diet | Diet orders, menu items, kitchen view, diet report | diet-service (53 tests) | ~~Kitchen production view~~ already done (`GET /api/v1/diet/order/kitchen`), ~~diet patient report~~ DONE (`GET /api/v1/diet/order/report` aggregates counts by orderStatus: UNRELEASED/ACTIVE/CANCELLED + TOTAL) | LOW |
| M11 CSSD | Kit master, prep, sterilization, issue/request, store dispatch/receive | cssd-service M11 V2 (30 tests) | ~~Sterilization with machine/quality test~~ DONE (V2 `t_sterilization_cycle.biological_indicator` + `chemical_indicator`; `POST /api/v1/cycles/{id}/quality-complete` records BI+CI + PASS/FAIL on the existing cycle), ~~store dispatch/receive~~ DONE (V2 `t_kit_issue.dispatched_to_store_id`/`dispatched_at`/`received_at`/`received_by`; `POST /api/v1/issues/{id}/dispatch` + `/{id}/receive` enforce dispatch-before-receive), indent flow | LOW |
| M12 OT/Surgery | Surgery scheduling, procedure performance, checklist, anesthesia, OT billing, nurse/equipment schedule | ot-service exists (surgery CRUD + **WHO checklist ~~SurgicalChecklistService~~ DONE + **AnesthesiaRecordService~~ DONE + **OtStaffSchedulingService~~ DONE + **OtBillingEstimationService~~ DONE + **OtNurseScheduleService~~ DONE + V5 migration + controllers + repos) | ~~Procedure performance details (checklist, anesthesia, staff, equipment)~~ DONE, ~~OT billing estimation~~ DONE, ~~Nurse schedule events~~ DONE | LOW |
| M13 Inventory | Item master, stock, indent, issue, adjustment, consumption, enquiry, expiry return, reorder, opening balance, stock transfer, asset, custom orders, approvals, store config | **item-service M13 (61 tests)** `item_stock_ledger` D-1 | ~~Stock adjustment~~ DONE (`POST /stock/adjustments` `movementType 8 ADJUSTMENT` `operation 1/2` `reason` required, `StockService.java:215` `adjustStock` → `postMovement` `ItemStockLedger` `stockAdjusmentDetails.vm` parity), ~~consumption~~ DONE (`POST /stock/consumptions` `movementType 13` `StockService.java:233` `consumeStock`), ~~approval worklist~~ DONE (`GET /api/v1/inventory/approval-worklist?storeId=&docType=` `ApprovalWorklistService.java` `ApprovalWorklistController.java` 200-row cap, normalised 1=PENDING/2=APPROVED/3=REJECTED,RECEIVED/4=CANCELLED space, merges PENDING queues across INDENT/GRN/CPO/EXPIRY_RETURN/STOCK_TRANSFER); ~~item enquiry~~ DONE (`GET /api/v1/items/search?name=&storeId=&includeZeroStock=&page=&size=` + `GET /items/{id}/batches?storeId=` + `GET /items/{id}/stock?storeId=` + `GET /items/{id}/suppliers`, trigram + similarity ordering on `item_name`); ~~custom process orders~~ DONE (`POST/GET /inventory/custom-process-orders` + approve/cancel/receive; V4 migration `t_custom_process_order` + detail; PENDING→APPROVED→{PARTIALLY_RECEIVED,RECEIVED}; receive posts via D-1 single-writer movement type 2 GRN_RECEIPT); ~~reorder lists~~ DONE (`PUT/GET /inventory/reorder-policies` + `GET /inventory/reorder-alerts`, available stock threshold evaluation); ~~opening balance~~ DONE (`POST /inventory/opening-balances`, movementType 1 via D-1 single-writer); ~~expiry/damage return~~ DONE (`POST /inventory/expiry-returns` + approve/reject/cancel/return-to-supplier, movementType 9/10); ~~stock transfer~~ DONE (`POST /inventory/transfers` + dispatch/receive, movementType 7/6; D-1 single-writer stock path) | MEDIUM |
| M14 Procurement | Purchase request, quotation, PO, GRN+QC, rate contract, indent invoice | item-service M14 V5 (PO/Quotation/Rate Contract, provisional+consignment GRN flags) — 112 tests | ~~Quotation comparison~~ DONE (`GET /inventory/quotations/compare?itemId=` joins SUBMITTED/AWARDED quotations across suppliers; `QuotationService.compare`); ~~PO approval workflow~~ DONE (`POST /inventory/purchase-orders` DRAFT → PENDING_APPROVAL → APPROVED → ISSUED → CLOSED, with `submit/approve/reject/issue/cancel` endpoints; approving an awarded quotation flips its docStatus to AWARDED); ~~Provisional GRN~~ DONE (`Grn.provisional` flag, V5 `ALTER TABLE` adds column; `GrnService.postGrn` accepts `provisional:true`); ~~Consignment GRN~~ DONE (`Grn.consignment` flag, V5 `ALTER TABLE` adds column; same D-1 stock path); ~~Rate contracts~~ DONE (`POST /inventory/rate-contracts` + `/cancel` + `/lookup?supplierId=&itemId=&onDate=`; active-overlap guard); indent invoice | MEDIUM |
| M15 Lab/Pathology | Booking, sample collection/barcode, accept/reject, result entry, review, release, critical alerts, machine-wise, workload, COVID lab, masters, prints | lab-service exists (test CRUD, collect/start/result, barcode/accept-reject/verify/critical/workload/covid — V2) | ~~Sample barcode generation~~ DONE (V2 `lab_barcode_seq` yyyymm+7-digit, idempotent `POST /lab/orders/{id}/details/{detailId}/barcode`), ~~accept/reject workflow~~ DONE (`POST .../accept` + `.../reject` with reason, CIMS_SampleAccept parity), ~~double verification release~~ DONE (`POST .../verify` + LabResultVerified outbox), ~~critical alerts~~ DONE (`is_critical` on value vs reference_range `min-max`, `POST .../notify-critical` → LabCriticalAlert outbox for WhatsApp/SMS), ~~lab workload~~ DONE (`GET /lab/orders/workload`), ~~COVID lab list~~ DONE (`GET /lab/orders/covid`), machine-wise review/KDL, report prints (201 .vm templates), masters | HIGH |
| M16 Radiology | Booking, report entry/templates, reschedule/retest, PACS integration, prints | radiology-service M16 V2 (25 tests) | ~~Reschedule/retest~~ DONE (V2 adds `retest_requested`/`retest_reason`/`rescheduled_to` to `t_radorder_detail`; `POST /api/v1/radiology/orders/{orderId}/lines/{lineNo}/retest` + `/reschedule`), ~~Radiology report templates~~ DONE (V2 `t_rad_template` master with `template_code` + `modality` + `body_template`; `POST/GET /api/v1/radiology/templates`), PACS image integration, prints | MEDIUM |
| M17 Blood Bank | Donor reg, collection/component, requisitions, cross-match, bag trail/return/discard, indent, cross-unit, TTD, e-Raktkosh, reports (17) | bloodbank-service M17 V2 (49 tests) | ~~Component separation~~ DONE (`t_bloodcomponent` from V1, registered via bag trail event_type 6 SEPARATED), ~~Bag trail~~ DONE (append-only `t_bloodbag_trail` with event_type 1..12 covering collected/crossmatched/issued/returned/discarded/separated/indent*/cross-unit*), ~~Bag return~~ DONE (`POST /bloodbank/bag-returns` + complete/reject; doc_status 1=PENDING 2=RETURNED 3=REJECTED; trail EVENT_RETURNED on raise), ~~Bag discard~~ DONE (`POST /bloodbank/bag-discards` + complete; doc_status 1=PENDING 2=DISCARDED 3=REJECTED; trail EVENT_DISCARDED on raise), ~~Blood bank indent~~ DONE (`POST /bloodbank/indents` + approve/issue/cancel; doc_status 1=NEW 2=APPROVED 3=ISSUED 4=CANCELLED; inter-store component request), ~~Cross-unit orders~~ DONE (`POST /bloodbank/cross-unit-orders` + acknowledge/receive/cancel; doc_status 1=REQUESTED 2=ACKNOWLEDGED 3=RECEIVED 4=CANCELLED), TTD/red cell serology, e-Raktkosh sync, 17 blood bank reports | MEDIUM |
| M18 BMW | Waste request/status, bag barcode, handover to firm | waste-service (33 tests) | ~~Bag barcode generation~~ DONE (V1 `bio_medical_waste_details.barcode_no` 13-char {yyyyMM}{%07d monthly counter, allocated inside the request tx so it rolls back), ~~handover-to-firm workflow~~ DONE (`POST /api/v1/bmw/{id}/handover` + `/receive` stamp `handed_over_to` / `received_by`), ~~decompose flow~~ DONE (`POST /api/v1/bmw/{id}/process` stamps `firm_name` / `contact_person` / `contact_no` and transitions request + every bag to COMPLETED) | LOW |
| M19 Linen | Linen request/status, report | linen-service (38 tests) | ~~Linen laundry report~~ DONE (`GET /api/v1/linen/request/report` aggregates counts by `LinenStatus` + TOTAL) | LOW |
| M20 Equipment | Asset register, complaint/resolution, PM/calibration, warranty, equipment GRN, scrap/med-gas, usage report | equipment-service (112 tests, ComplaintService + ComplaintResolutionService + PmService) | ~~Complaint & resolution~~ already done (V2 tables `t_equipmentcomplaint` + `_details` + `_resolution` + `_resolution_details`), ~~calibration maintenance~~ already done (V3 `t_eqp_pm_schedule` + `_details` + `t_eqp_preventive_maintenance`), equipment GRN, scrap sale, med-gas indent | LOW |
| M21 Transport | Patient/doctor transport, vehicle alloc/in/out, fuel station, maintenance, trip/fuel reports | transport-service (47 tests) | ~~Fuel station details~~ already done (`POST/GET /api/v1/transport/fuel` with `t_fuel_station_details`), ~~vehicle maintenance & approval~~ already done (`POST/GET /api/v1/transport/maintenance` + `/approve` + `/complete` with `t_vehicle_maintenance`), trip/fuel consumption reports | LOW |
| M22 Grievance | Raise ticket, list/status/severity, resolution assignment, feedback | grievance-service M22 (35 tests) | ~~Feedback form~~ already exposed (`POST /grievances/{id}/feedback` with 5-question satisfaction + free text), ~~Grievance documents upload~~ DONE (`POST /grievances/{id}/documents` upload + `GET /grievances/{id}/documents` list; `t_grievance_document` metadata-only — blob storage via MIG-021) | LOW |
| M23 RMRS | Authority master, collection, journal/suspense/bank, PO requests, reports | rmrs-service M23 V2 (44 tests) | ~~Journal entries~~ DONE (`POST /rmrs/journal-vouchers` with multi-leg debit/credit, balance enforced, `post`/`reverse` lifecycle 1=DRAFT 2=POSTED 3=REVERSED), ~~suspense account~~ DONE (`POST /rmrs/ledgers/{id}/suspense` + `suspense/clear`, `t_ledger_master.suspense` flag), ~~bank statement~~ DONE (`POST /rmrs/bank-statements` import + `reconcile` (matchedEntryId/matchedCollectionId) + `dispute`, status 1=IMPORTED 2=RECONCILED 3=DISPUTED), ~~PO requests~~ DONE (`POST /rmrs/po-requests` + approve/reject/issue, status 1=NEW 2=APPROVED 3=ISSUED 4=REJECTED), detailed RMRS reports | LOW |
| M24 PMRD | Encounter record, document upload, JSY forms (J1/J2/J3/H-form) | NOT IMPLEMENTED | Entire module | LOW |
| M25 Reports/BI | 46+ report schemas, BI dashboard embed, Janta clinic reports | reporting-service M25 V2 (38 tests, 15 seeded reports) | ~~Top 15 legacy report definitions~~ DONE (TABULAR on rm_patient_registry/rm_encounter_stats/rm_revenue_daily/rm_op_stats, SUMMARY on cnt/total, idempotent on reportCode; `ReportSeeder` boots DAILY_OPD/IPD/ENCOUNTERS/REVENUE/OT/LAB/BLOOD + PATIENT_REGISTRY + OPD_LIST/IPD_LIST + 5 SUMMARY totals = 15); ~~BI dashboard counts~~ extended with `otOperations`; OtCompleted / LabVerified / BloodBagIssued event types landed (`POST /api/v1/internal/events` + `rm_op_stats` V2), BI dashboard embed, Janta clinic reports, remaining 31 legacy report implementations | MEDIUM |
| M26 Worklist | Nurse task/patient worklist, goals/interventions tabs | nursing-service M26 (18 tests) | ~~Patient-wise worklist~~ DONE (`GET /api/v1/nursing/worklist/{admissionId}` aggregates tasks/vitals/notes/carePlan with per-tab open/total counts; meds/labs/rad tabs are out of scope — those belong to pharmacy/lab/radiology services, not nursing) | MEDIUM |
| M27 Queue | Appointment/cabin queue boards, token mgmt, doctor batch, nurse desk, call patient, dept queues | **queue-service M27 V2 (43 tests)** dept queues for rad/patho + cabin board | ~~Cabin queue boards~~ DONE (`GET /queue/cabin/{counterId}/board` per-cabin `tokens`+`departmentWaiting`), ~~department-specific queues (rad/patho)~~ DONE (`GET /queue/tokens/department/{deptId}`, `GET /queue/department/{deptId}/dashboard` `waiting/called/nowServing/counters`, `GET /department/{deptId}/counters` `V2` `9001=RAD` `9002=PATHOLOGY` seeds, `QueueService.java:215` `dashboardByDepartment`+`cabinBoard`), doctor batch slots, nurse desk integration (`ponytail: batch slots + nurse desk deferred until scheduling/ADT ward integration`) | MEDIUM |
| M28 Mortuary/Death | Mortuary box mgmt, death details, child death review, post-mortem/death cert print | NOT IMPLEMENTED | Entire module | LOW |
| M29 Chemotherapy | Chemo planning, regimen/cycle, coversheet | NOT IMPLEMENTED | Entire module | LOW |
| M30 BCMA | Bedside medication scan, IVP, exceptions, stock view | NOT IMPLEMENTED | Entire module | MEDIUM |
| M31 Kiosk | Self-reg, self-appointment, account search, kiosk queue, lab result upload | **DONE** (kiosk-service, 28 files, 14 tests, DDD layers, V1 flyway) | Entire module | LOW |
| M32 RCM | RGHS eligibility, DM approval, discharge verification, Chiranjeevi, TID reports | rcm-service M32 V2 (58 tests) | ~~DM approval~~ DONE (`POST /api/v1/rcm/dm-approvals` + approve/reject; status 1=PENDING 2=APPROVED 3=REJECTED; approved_amount required for approve), ~~discharge verification~~ DONE (`POST /api/v1/rcm/discharge-verifications` + bill-settled/clearance-done/complete; complete() enforces both flags before VERIFIED), ~~Chiranjeevi~~ DONE (`POST /api/v1/rcm/chiranjeevi-cases` + approve/reject/close; status 1=NEW 2=APPROVED 3=REJECTED 4=CLOSED), RGHS TID/metadata reports | MEDIUM |
| M33 Payment Transfer | Transfer requests, incentive claims, OTP verification | NOT IMPLEMENTED | Entire module | LOW |
| M34 File/Document Mgmt | Upload/view/category for patient/lab/grievance/equipment docs | **file-service NEW (M34 V1, 26 tests)** owns `ihms_file` `t_file_category` + `t_file_details` (8004) | ~~Document upload/download service~~ DONE (`POST /files` base64 10 MB `data URI` + PG bytea `file_token` UUID, `GET /files/token/{token}/download` bytes, `GET .../base64`), ~~annotated image view~~ DONE (`PUT /files/{id}/annotation` + `GET /{id}/annotation` JSON, `ImageAnnotationMaster` parity), ~~document categories~~ DONE (`POST/GET/PUT/DELETE /file-categories`, `GET /file-categories/tree` parentId nesting, 5 seeds PATIENT/LAB/GRIEVANCE/EQUIPMENT/GENERIC, ownerType/ownerId scoping) — `file-service/src/main/java/com/hims/file:26 tests` V1 `ihms_file` `FileService.java:1` | HIGH |
| M35 User/Role/Auth | User CRUD, role assignment, permissions, notifications, login/SSO/OTP | iam-service exists (login/logout/validate, user CRUD, roles, permissions M35, OTP M35 — V2) | ~~Module/menu permissions~~ DONE (`t_module_permission_master` + `t_role_enabledpermission` JSON set, `PUT /roles/{id}/permissions`, `GET /users/{id}/permissions` union, M35 V2), ~~notification templates~~ DONE (`t_notification_template` CRUD `GET/POST/PUT /notification-templates`, SMS/EMAIL/WHATSAPP, `T_EMAILTEMPLATE` parity), ~~OTP login~~ DONE (`POST /auth/otp/generate` 6-digit BCrypt 5-min TTL 3-attempts `devOtp` in dev, `POST /auth/otp/verify` → JWT `OtpService.java:1`), ~~user-store/ward/unit mapping~~ DONE (`t_userdetails.store_id/ward_id/department_id` + `PUT /users/{id}` extended), SSO integration (feature-flag `app.sso.enabled=false`, `ponytail: SSO deferred until IdP/Keycloak ready`) | HIGH |
| M36 EJS (ASHA) | ASHA work data, ANC/PNC/child-care, incentives, geography/population | govt-service exists (ASHA, indicators, incentives, enrolment) | Full ANC/PNC/child-care tracking, population registers, geo masters, checklist submissions | LOW |
| M37 Aasmaan | Case CRUD, checklists, monitoring, devices, institutions, e-tickets, RSC logs | NOT IMPLEMENTED | Entire module | LOW |
| M38 Integrations | ABDM/ABHA (50 endpoints), HPR, NHCX, EHR, HL7, e-Aushadhi, MDM/Janta, FHIR, WhatsApp/SMS | interop-service M38 V3 (95 tests) | ~~ABHA create/link/delink/consent/PHR~~ DONE (`POST /api/v1/interop/abha-m38/links` + verify/delink/revoke; `POST /consents` + revoke/expire with validFrom/validTo + HIU/HIP; `POST /phr-push` + pushed/failed), HPR registration, remaining 46 ABHA endpoints, EHR file sync, e-Aushadhi indent/stock/QC sync, MDM/Janta, FHIR R4 server, WhatsApp Business API queue | HIGH |
| M39 Commons/Platform | Audit trails, app/unit config, hospital creation/geo-tag, machine config, MRN series, schema designer, scan/checkin | reference-service M39 V7 (96 tests) | ~~Application config service~~ DONE (V7 `app_config` per-unit key/value; `PUT/GET /api/v1/commons/config`), ~~Audit trail service~~ DONE (V7 `audit_log` append-only JSONB diff per entity+action; `POST /api/v1/commons/audit` + `/by-entity` + `/by-actor/{id}`), ~~hospital geo-tagging~~ already done (`UnitMaster.LONGITUDE` + `LATIUTDE`), schema/form designer, scan patient/checkin | MEDIUM |
| M40 TIU/Clinical Forms | 368 clinical forms, designer, specialty mapping | **forms-service NEW (M40, 27 tests)** owns `ihms_forms` `t_form_schema` + `t_form_submission` | ~~JSON-schema engine~~ DONE (`t_form_schema` JSONB draft-07 with version+status+specialty; `t_form_submission` validated against `required[]`; specialty filter + patient/encounter cross-form lookup + soft delete; 20 seeded forms across NURSING/EMERGENCY/OBGYN/PEDIATRICS/FORENSIC/ANESTHESIA/SURGERY/NEUROLOGY incl. NURSING_INITIAL_ASSESSMENT, EMERGENCY_TRIAGE, DISCHARGE_SUMMARY, PRE_ANESTHETIC_CHECK, DAILY_NURSING_REPORT, PHYSICIAN_ASSESSMENT, OBSTETRICS_PROGRESS, PEDIATRIC_PROGRESS, INJURY_REPORT_MLC, SEXUAL_ASSAULT_REPORT, ALCOHOL_DRUG_REPORT, VASECTOMY_NOTE, INTAKE_OUTPUT_CHART, FEEDING_CHART, STROKE_ICU_CHART, ANC_VISIT, WHO_SURGICAL_SAFETY_CHECKLIST, IUD_INSERTION_NOTE, WOUND_ASSESSMENT, MORSE_FALL_SCALE), remaining 348 forms + form designer UI | HIGH |

---

## Frontend Gaps (Critical)

Current frontend: Only `features/masters/` (MasterListPage, MasterEditPage, MasterForm, MasterTable).

**All business module UIs are missing.** Priority frontend pages:

1. Patient registration & search (M01)
2. OPD walk-in / appointment booking (M02)
3. Admission & bed management board (M03)
4. Billing flow & payment (M04)
5. Pharmacy dispensing (M07)
6. Nursing worklist & vitals (M09)
7. Lab order & result entry (M15)
8. Queue management dashboard (M27)
9. Login / auth pages (M35)

---

## Implementation Prompt

Use this prompt to guide implementation of remaining gaps:

```
You are implementing the HIMS modernization project. The old IHMS codebase is at
/Users/adityabanerjee/personal_project/ihms/src and the new HIMS microservices
codebase is at /Users/adityabanerjee/personal_project/hims_new_deloitte.

Follow these rules:
1. Each gap from plan.md must be implemented as a bounded addition to the
   EXISTING microservice that owns that domain. Do NOT create new services
   unless the domain has no service at all.
2. Follow existing patterns in each service (DDD layers: api/application/
   domain/infrastructure, outbox pattern, CorrelationIdFilter, GlobalExceptionHandler).
3. For frontend, follow the existing React+Vite+Zod+TanStack Query pattern
   in frontend/src/features/masters/. Create new feature folders per module.
4. Reference the old IHMS code for business logic, field names, validation
   rules, and stored procedure behavior. Port logic, do not copy Java.
5. Implement in priority order: HIGH > MEDIUM > LOW.
6. Each feature must include: controller endpoint(s), service method(s),
   repository, domain model, Flyway migration, and unit test.
7. For integrations (ABDM, NHCX, e-Aushadhi, WhatsApp), implement behind
   feature flags so they can be toggled per environment.
8. The TIU clinical forms engine (M40) should be implemented as a standalone
   library/service since it spans ordering + nursing + reporting.
9. Run lint/typecheck/tests after every change.
10. Never add speculative features. Only implement what the gap matrix lists.
```

---

## Phase Plan

### Phase 1 — Core Clinical Flow (HIGH priority)
- M01: Patient registration enhancements (Aadhaar, photo, MLC)
- M02: Walk-in visit, emergency registration
- M04: Credit notes, partial payment, package billing
- M07: Cart-fill, returns, pharmacy billing integration
- M09: Care plans, shift handover, nurse allocation, incident reporting
- M15: Sample barcode, accept/reject, double verification, critical alerts
- M34: Document upload/download service
- M35: Permissions, notification templates, SSO
- M38: ABHA create/link/consent, WhatsApp queue
- M40: Clinical forms engine (minimum viable: 20 most-used forms)
- Frontend: Patient reg, OPD walk-in, billing, pharmacy dispense, nursing worklist, lab, login

### Phase 2 — Operational Modules (MEDIUM priority)
- M03: Interim admission, attendant pass, death handling
- M05: Tariff copay, package rates, concession templates
- M08: Order sets, CWF/SCWF, machine config
- M12: Procedure performance details, OT billing ~~DONE~~ (SurgicalChecklist, AnesthesiaRecord, OtStaffScheduling, OtBillingEstimation, OtNurseSchedule services + V5 + 25+ tests)
- M13: Stock adjustment, consumption, expiry return, reorder
- M14: Quotation, PO approval, provisional/consignment GRN, rate contracts
- M16: Reschedule, PACS integration, report templates
- M17: Component separation, bag trail, indent, e-Raktkosh
- M25: Top 15 legacy reports
- M26: Goals/interventions tabs
- M27: Cabin queue, doctor batch, dept queues
- M32: DM approval, discharge verification, Chiranjeevi
- M39: Audit trails, app config, hospital geo-tag
- Frontend: Admission/bed board, OT schedule, inventory, queue dashboard, reports

### Phase 3 — Auxiliary Modules (LOW priority)
- M06: Doctor share & incentives
- M10: Kitchen view, diet report
- M11: Sterilization, store dispatch
- M18: Bag barcode, handover-to-firm
- M19: Linen report
- M20: Complaint/resolution, calibration, equipment GRN
- M21: Fuel station, vehicle maintenance, reports
- M22: Feedback form, document upload
- M23: Journal entries, suspense, bank statement
- M24: PMRD module
- M28: Mortuary & death module
- M29: Chemotherapy
- M30: BCMA
- M31: Kiosk
- M33: Payment transfer
- M36: Full EJS/ASHA tracking
- M37: Aasmaan field app backend
- Remaining reports (M25)
- Remaining frontend pages

</content>