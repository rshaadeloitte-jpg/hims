# IHMS to HIMS Migration Gap Analysis

**Baseline:** `/Users/adityabanerjee/personal_project/ihms/src` (`com.ohum` monolith)  
**Target:** `/Users/adityabanerjee/personal_project/hims_new_deloitte`  
**Assessment date:** 2026-08-30

## Scope and Method

This assessment triangulates four evidence sets:

- Legacy Java services, entities, JAX-RS resources, generated procedures, jobs, and SQL. The legacy inventory records **3,060 explicit endpoints**, **659 generic CRUD entities**, **843 tables/~898 entities**, **719 PL/SQL artifacts**, and **132 service classes**.
- Target Java services under `services/*` (30 services, approximately 1,000 main Java files), Flyway migrations, controllers, application services, repositories, and tests.
- Target OpenAPI contracts under `openapi/*.yaml` (29 specifications, **286 path declarations**).
- Existing modernization evidence under `docs/modernization/`, especially `GAP-ANALYSIS-AND-PLAN.md`, `API-INVENTORY.md`, `FEATURE-MATRIX.md`, `06-business-rules-catalog.md`, `05-database-catalog.md`, workflow documents, and `REMAINING-GAPS-SPEC.md`.

Parity is a delivery estimate, not a line-count ratio. It weights end-to-end workflow coverage (40%), API/contract coverage (25%), data/schema coverage (20%), and validation/integration/test evidence (15%). A service or OpenAPI file without implemented behavior is counted as partial, not complete.

## Executive Summary Matrix

| Domain | Legacy status | New status | Parity | Assessment |
|---|---|---:|---:|---|
| Patient management | Registration, identity, MRN, sponsors, documents, ABHA/Aadhaar, MLC, mother/child, portal | `patient-service` covers register/search/sponsors/merge/documents; MLC/mother-child/photo recently addressed; Aadhaar/COVID/social-work gaps | 55% | Core registration exists; identity, portal, and statutory extensions incomplete |
| OPD / appointments | Walk-in, emergency, appointment slots, queues, check-in, follow-up, telemedicine, kiosk, questionnaires, prints | `appointment-service` CRUD/slots/check-in/encounter; walk-in/emergency marked done in plan | 58% | Follow-up, telemedicine, kiosk, questionnaires, OPD print remain |
| IPD / ADT / beds | Admission, auto-bed allocation, transfer, discharge, clearance, interim, attendants, death | `adt-service` with admission/transfer/discharge, census, TID, attendant pass, interim and death V2 | 72% | Strongest clinical migration; clearance/legacy integrations and parity verification remain |
| Billing / payments | Charges, OP/IP bills, concessions, GST, receipts, advances, refunds, packages, sponsor changes, doctor share | `billing-service` and `payments-service`; notes, estimation, package, sponsor and payment-ledger paths implemented | 63% | Doctor share and pharmacy billing integration are absent; saga transport is stubbed |
| Tariff / master data | 659 generic CRUD masters plus tariff, package, plan, concession, order-set rules | `reference-service` generic CRUD and OpenAPI | 35% | CRUD shell exists; rule-rich tariff/package/concession behavior missing |
| Doctor share | Share calculation, add/deduct, payout and visit charges | No dedicated implementation | 0% | Entire legacy module absent |
| Pharmacy | Prescription, IP/OP dispense, cart-fill, IV/compound, returns, interventions, mapping, BCMA, home meds, billing | `pharmacy-service` prescription/dispense/interventions/ward picklist, sale returns, allergy/interactions | 55% | IV compounding, mapping, BCMA, home meds, ABDM PHR, billing integration missing |
| Clinical orders | Service orders, order sets, investigations, CWF/SCWF, external orders, machine config | `ordering-service` CRUD plus saga state machine | 40% | Order-set instantiation, approval workflows, external/machine configuration absent |
| Nursing | Worklists, coversheet, notes, care plans, vitals, charting, handover, allocation, incidents, rostering, allied/mortuary | `nursing-service` vitals/notes/tasks plus application services | 42% | Care plans, charting/partograph, handover, allocation, incidents, rostering absent |
| Diet | Orders, menu, kitchen, reports | `diet-service` items/sessions/plans/orders | 50% | Kitchen production and reports absent |
| CSSD | Kits, preparation, sterilization, issue/request, dispatch/receive | `cssd-service` kits/cycles/issues | 45% | Machine/quality tests and store dispatch/indent absent |
| OT / surgery | Scheduling, procedure details, checklist, anesthesia, staff/equipment, billing | `ot-service` schedule/start/complete/cancel | 38% | Clinical procedure record, WHO checklist, anesthesia, staffing, billing absent |
| Inventory / stores | Item/stock/indent/issue/adjustment/consumption/expiry/reorder/transfers/assets/approvals | `item-service` stock ledger, adjustments, consumption, reorder, opening balance, returns, transfers | 67% | Enquiry, custom orders, approvals, asset and configuration gaps |
| Procurement | PR, quotation, PO, GRN/QC, rate contract, indent invoice | Item/GRN foundations | 30% | Approval, comparison, provisional/consignment GRN, rate contracts missing |
| Lab / LIS | Booking, collection/barcode, accept/reject, result/review/release, critical alerts, machines, reports, masters | `lab-service` order lifecycle, barcode, accept/reject, verify, critical/workload/COVID | 60% | KDL/machine integration, reference ranges, masters and 201 print templates missing |
| Radiology | Booking, reports/templates, reschedule/retest, PACS, prints | `radiology-service` order/start/image/report/cancel | 45% | PACS/DICOM, reschedule/retest, templates and prints missing |
| Blood bank | Donor, collection/components, requisition, cross-match, bag trail, discard/return, TTI, e-Raktkosh, reports | `bloodbank-service` donor/requisition/cross-match/expiry jobs | 40% | Component/bag lifecycle, serology, external sync and reports missing |
| BMW / linen / equipment / transport | Operational workflows plus reports and vendor/fleet details | Services exist with basic request or asset flows | 35% | Handover, laundry, complaints/calibration, fuel/maintenance/reporting incomplete |
| Grievance | Ticket, severity, assignment, comments, resolution, feedback, documents | `grievance-service` ticket workflow | 65% | Feedback and attachments missing |
| RMRS | Collection, authorities, journals, suspense, bank, PO, reports | `rmrs-service` collection/heads/authorities/ledgers/summaries | 50% | Journal/suspense/bank statement/PO/report detail missing |
| Reporting / BI | 46+ report schemas, PDF/CSV/HTML, dashboards, Janta reports | `reporting-service` CRUD/execute/dashboard counts | 25% | Legacy report implementations and BI embedding absent |
| Queue / worklists | Cabin/dept queues, token state, doctor batches, nurse desk, call/display | `queue-service` counters/tokens/cabin/dept dashboard | 58% | Doctor batch and nurse-desk integration absent |
| RCM / schemes | RGHS/Chiranjeevi eligibility, preauth, DM approval, discharge verification, TID reports | `rcm-service` eligibility/preauth/claims/NHCX callbacks | 45% | DM/MOIC, discharge audit, Chiranjeevi and reporting gaps |
| IAM / RBAC / auth | Login variants, OTP/SSO, users/roles/permissions, notifications, audit | `iam` login/user/role/permission/OTP; SSO feature-flagged | 60% | Keycloak/SSO and legacy token compatibility incomplete |
| Files / documents | 24 file APIs, category/upload/download/annotation and module attachments | `file-service` upload/download/base64, categories, annotations | 75% | Strong functional parity; migration of legacy references and virus scanning remain |
| Interoperability | 50 ABHA, HPR, NHCX, EHR, HL7, FHIR, e-Aushadhi, WhatsApp/SMS | `interop-service` HL7/ABDM request/message; RCM NHCX | 30% | Many adapters/callbacks are contract or transport stubs |
| Clinical forms / TIU | 368 forms, designer, specialty mappings | `forms-service`, JSON Schema engine, 20 seeded forms | 35% | Engine is present; 348 forms and designer UI missing |
| PMRD / mortuary / chemo / BCMA / kiosk / payment transfer / Aasmaan | Dedicated legacy modules | No target service or only adjacent primitives | 0-10% | Deferred specialized modules |

**Overall weighted parity: approximately 45%.** Core ADT, file management, inventory ledger, billing primitives, and lab workflow have meaningful implementations; the platform is not yet functionally equivalent to IHMS because clinical breadth, integrations, reports, frontend, and rich master rules remain incomplete.

## Missing Features and Unimplemented APIs

### P0 Critical (blocks safe clinical or financial cutover)

| Gap | Evidence / files | Required outcome |
|---|---|---|
| Frontend clinical workflows | `frontend/src/features/` currently contains only `masters` | Deliver patient registration/search, OPD, ADT/bed board, billing/payment, pharmacy, nursing, lab and IAM screens before pilot |
| Cross-service transaction reliability | `billing-service` tests describe transport-stub ingress; `reporting` calls ingress a contract stub | Replace HTTP/test stubs with Kafka consumers, idempotency, retries, DLQ, and reconciliation |
| Legacy identity/MRN and duplicate detection | Legacy `PatientServiceHelper`, `BaseServiceImpl`; rules BR-PAT-001/002 in business catalog | Implement series/temp MRN, duplicate identity/health-ID prevention, offline-unit behavior and error codes |
| Bed allocation and discharge invariants | Legacy `PatientRegAdmissionServiceImpl`, `PatientTransferDischargeServiceImpl`, `CIMS_UPDATEIPDISCHARGE` | Prove atomic occupancy/CAS, ward/category priority, cleaning/release, clearance and rollback under concurrency |
| Medication safety | Legacy pharmacy/BCMA flows; target pharmacy lacks BCMA and complete interaction/override semantics | Implement five-rights scan, allergy/interaction hard-stop/override audit, IV/compound validation |
| Security parity | Legacy token/query auth and frontend-gated RBAC; target SSO disabled by flag | Keycloak/OIDC, service authorization, audit events, token migration and least-privilege tests |
| Clinical data and integrations | `interop-service` tests explicitly call Kafka consumer and ABDM pieces stubs | Implement ABHA/HPR/FHIR, NHCX callbacks, HL7, e-Aushadhi and notification queues behind flags with production adapters |

### P1 High

- Nursing care plans, charting/partograph, SBAR handover, allocation, incidents and rostering.
- Doctor-share accounting and pharmacy-to-billing charge consumption.
- Lab KDL/machine interface, multi-parameter ranges, master data, critical alert delivery and print templates.
- Order sets plus CWF/SCWF approvals and external order details.
- Tariff copay/package-class/concession authorization rules.
- Radiology PACS/DICOM, structured templates, reschedule/retest.
- Blood component separation, bag trail/discard/return, TTI serology and e-Raktkosh.
- Procurement quotation comparison, PO approval, provisional/consignment GRN and rate contracts.
- Complete statutory/reporting catalog and BI dashboards.
- Remaining 348 TIU forms and form designer UI.

### P2 Medium / deferred

PMRD, mortuary/death certificates, chemotherapy, BCMA exception workflows, kiosk, payment transfer/DBT, Aasmaan, full EJS/ANC-PNC, diet kitchen, CSSD quality tests, BMW vendor handover, linen reconciliation, equipment complaints/calibration, transport fuel/maintenance, grievance attachments/feedback, RMRS journals/suspense/bank, and remaining low-use reports.

## API and Contract Parity Findings

The legacy API surface is not comparable one-for-one with the target path count. IHMS exposes 3,060 explicit JAX-RS routes plus a generic CRUD filter serving 1,109 frontend URLs, a report dispatcher, an `/ext/api` transformation gateway, and 57 ABDM/HFR callbacks. The target declares 286 OpenAPI paths across 29 specs. This is a deliberate decomposition, but it leaves substantial contract coverage gaps.

Representative mismatches:

- Legacy `patient/registerPatient`, `getPatientGeneralDetailsListForSearch`, `searchPatientFromSolr`, and multiple portal/blood-bank searches collapse into a small `patient-service` contract; search semantics, rolling encounter window, duplicate rules, and legacy error codes must be preserved.
- Legacy generic CRUD supports list/all/get/add/bulk/upload/partial-update/delete for 659 entities. Target `reference-service` exposes generic CRUD, but no evidence establishes parity for allowed filters, CSV/XLS upload, cache invalidation, audit events, or all entity fields.
- Legacy reports include 49 `*Report` and 72 `print*` routes plus a 50-key report dispatcher. Target reporting has six OpenAPI paths and no equivalent report catalog implementation.
- Legacy pharmacy has 102 explicit endpoints; target pharmacy OpenAPI has six paths. The target application classes cover more behavior than the spec, so OpenAPI is under-described and cannot yet serve as a complete consumer contract.
- Legacy nursing has 300 endpoints; target nursing OpenAPI has five paths. Care-plan, charting, handover, incident and allocation endpoints are absent from the contract.
- Legacy API contains GET mutations, duplicate/near-duplicate paths, overloaded callback versions, and 222 unresolved frontend URLs. These require an explicit compatibility gateway or a versioned deprecation plan, not silent omission.
- Several target tests and comments identify transport stubs (`reporting IngestionController`, billing saga ingress, interop Kafka consumer). These are not production parity.

## Data Model and Schema Alignment

The legacy persistence surface is broad and relational: T_Registration/T_Registration_Details, T_Appointment/T_Encounter/T_Visit, T_Admission/T_AdmissionDetails/T_TransferRequest, M_BedMaster/M_BedStatusMaster, T_Bill/T_Charges/T_Payment/T_Receipt, T_ItemSale, pathology/radiology/blood-bank tables, 659 CRUD masters, and 719 generated procedures.

Target Flyway migrations create bounded schemas such as `ihms_rcm`, billing ledgers, `item_stock_ledger`, `ihms_file`, `ihms_forms`, queue counters/tokens, and domain tables. This is a sound ownership direction, but the following alignment work is outstanding:

- Build a column-level mapping for all cutover tables, including legacy code/status values and nullable/default behavior.
- Preserve high-risk identifiers and sequences: MRN, OPD/visit numbers, bill/receipt numbers, stock movement numbers, lab barcodes, blood bag identifiers, and attendant-pass numbers.
- Port procedure semantics, not procedure names. The legacy catalog confirms only about 30 procedures are actively called by Java; the remaining 719 need disposition (`port`, `replace with domain service`, or `retire after evidence`).
- Reconcile legacy status collisions and numeric codes documented in `06-business-rules-catalog.md` (for example blood donor status 3 and 5 collisions, admission/bed statuses, payment transaction types, GST applicability).
- Establish dual-write/read-model reconciliation for Solr-backed patient search before retiring the legacy index.
- Add migration checks for audit lineage, soft-delete semantics, file byte storage, sponsor/tariff history, and clinical form versioning.

## Complex Business Logic Porting Checklist

- [ ] MRN series/temp-MRN/offline rules and permanent promotion (BR-PAT-001).
- [ ] Duplicate identity and health-ID detection with BERA/BEXA error compatibility (BR-PAT-002).
- [ ] OPD slot collision, visit-type sequence, emergency numbering, sponsor defaulting and walk-in doctor gates (BR-OPD-001..009).
- [ ] Admission preconditions, baby/cradle rule, active-admission rule, and atomic bed priority/CAS allocation (BR-IPD-001..003).
- [ ] Interim admission auto-abscond job, attendant-pass lifecycle, census, TID and death/discharge persistence.
- [ ] Charge tariff selection, residential/non-residential tariff, concession authorization, GST date gate, package consumption, temporary-charge finalization and bill-cancellation restoration.
- [ ] Payment settlement invariant: self and non-self balances must both be non-positive before bill settlement; preserve advance adjustment/refund transaction types.
- [ ] Pharmacy dispense state machine, cart-fill/ward pick, stock single-writer movements, returns/reversals, compound/IV calculations, allergy/interaction override audit and BCMA five-rights.
- [ ] Order-to-bill-to-lab/radiology/doctor-share event choreography with idempotency and compensation.
- [ ] Nursing task/care-plan/charting/handover/allocation status transitions and audit history.
- [ ] Lab barcode sequence, sample accept/reject reason, double verification, reference-range/critical evaluation, alert fan-out, KDL result mapping and print rendering.
- [ ] Radiology accession/retest/reschedule, structured report templates and PACS/DICOM lifecycle.
- [ ] Blood donor/requisition/bag status codes, cross-match/decross expiry jobs, component volumes, TTI validation and discard/return trail.
- [ ] Inventory movement types, reorder thresholds, expiry/damage return approvals, inter-store dispatch/receive and procurement QC.
- [ ] RCM eligibility/preauth/claim status, NHCX encryption/callback correlation, DM/MOIC and discharge verification.
- [ ] Generic master CRUD filters/uploads/cache/audit and role/menu permission evaluation.
- [ ] Clinical forms JSON Schema versioning, specialty mapping, required-field validation, soft delete and submission audit.

## Phased Roadmap and Implementation Plan

### Phase 0: Contract and migration control (0-6 weeks)

Freeze a traceability matrix from legacy route/entity/procedure to target service/endpoint/event. Generate OpenAPI from controllers and add contract tests for every P0 route. Complete the column/status/sequence mapping and classify all 719 procedures. Establish observability, audit correlation IDs, retry/DLQ, and a reconciliation dashboard.

### Phase 1: Clinical and financial spine (6-16 weeks)

Finish patient identity/MRN, OPD, ADT concurrency/clearance, billing settlement, pharmacy safety, nursing minimum viable care, lab machine/result release, IAM/SSO, file service hardening, and production interop adapters. Build the nine priority frontend features listed in the modernization plan. Pilot with shadow reads and dual-write reconciliation.

### Phase 2: Operational parity (16-28 weeks)

Implement tariff rules, order sets/CWF, OT clinical records, procurement approvals, radiology PACS/templates, blood-bank lifecycle, inventory approvals/enquiry, RCM scheme workflows, queue calling/doctor batches, statutory reports, and ADT/queue/inventory/report frontends.

### Phase 3: Specialized and statutory modules (28-44 weeks)

Deliver PMRD, mortuary, chemotherapy, BCMA, kiosk, payment transfer, Aasmaan, full EJS, department auxiliary workflows, remaining reports, and the clinical-form designer. Migrate remaining legacy data and retire compatibility routes only after usage and reconciliation thresholds are met.

### Exit criteria

Cutover requires: 100% P0 contract tests passing; zero unresolved high-severity reconciliation differences for 30 days; concurrency tests for admission/bed/payment/stock; security and audit sign-off; production adapters enabled in a controlled environment; and business-owner acceptance for each domain matrix row.

## Source Evidence Index

- [Existing gap plan](docs/modernization/GAP-ANALYSIS-AND-PLAN.md)
- [Remaining gaps specification](docs/modernization/REMAINING-GAPS-SPEC.md)
- [API inventory](docs/modernization/API-INVENTORY.md)
- [Business rules catalog](docs/modernization/06-business-rules-catalog.md)
- [Database catalog](docs/modernization/05-database-catalog.md)
- [Feature matrix](docs/modernization/FEATURE-MATRIX.md)
- [Implementation status](docs/modernization/implementation/IMPLEMENTATION-STATUS.md)
- Legacy representative sources: `PatientServiceHelper.java`, `EmergencyAndWalkinVisitServiceImpl.java`, `PatientRegAdmissionServiceImpl.java`, `PatientTransferDischargeServiceImpl.java`, `AddPaymentServiceHelper.java`, `AddServiceOrderServiceHelper.java`, `MedicationServiceImpl.java`, `NursingServiceImpl.java`, `PathoOrderServiceImpl.java`, `BloodBankServiceImpl.java`, `ApplicationJobs.java`, and `src/main/resources/generated-procs/*`.
