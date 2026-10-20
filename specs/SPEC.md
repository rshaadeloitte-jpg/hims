# HIMS Modernization: Remaining Gaps & Full Implementation Specification

## Overview
This specification defines the complete remaining gap implementations for modernizing the legacy IHMS monolith (`/Users/adityabanerjee/personal_project/ihms/src`) into the new HIMS microservices architecture (`services/*` Java Spring Boot microservices with DDD + PostgreSQL + Outbox) and modern React frontend (`frontend/src/features/*` with TypeScript, Zod, and TanStack Query).

All tasks in this specification follow the rules in `AGENTS.md` and the prioritization established in `docs/modernization/GAP-ANALYSIS-AND-PLAN.md`.

---

## Architectural Guardrails & Development Standards

### 1. Legacy Cross-Reference
* Review the legacy codebase at `/Users/adityabanerjee/personal_project/ihms/src` (`com.ohum`) for business rules, stored procedures, column mappings, and validation logic.
* Port business logic into target DDD layers (`api`, `application`, `domain`, `infrastructure`); do not copy legacy patterns verbatim.

### 2. Backend Microservice Standards
* Bounded additions to existing microservices in `services/<service-name>`.
* Outbox pattern for asynchronous domain event publishing via PostgreSQL and Kafka/debezium.
* `CorrelationIdFilter`, `GlobalExceptionHandler`, and structured logging across all REST controllers.
* Flyway migrations in `src/main/resources/db/migration/` for any DDL additions.
* JUnit 5 unit and slice tests for all new domain entities, services, and REST controllers.

### 3. Frontend Architecture Standards
* Feature-folder structure under `frontend/src/features/<feature-name>/` matching `features/masters/`.
* TypeScript strict types + Zod validation schemas for all form inputs and API payloads.
* TanStack Query (`@tanstack/react-query`) for all data fetching, mutations, and optimistic cache updates.
* Tailwind CSS + shadcn/ui component styling with responsive layouts.

### 4. Integration Gateways
* External integration endpoints (ABDM, e-Aushadhi, WhatsApp, NHCX, PACS) must be wrapped with circuit breakers and toggled via Spring configuration feature flags (`app.integration.<name>.enabled`).

---

## Phase 1: Core Clinical & Financial Spine (HIGH Priority)

### Module M01: Patient Management
- [ ] Task M01-1: Implement Aadhaar OTP & Biometric verification endpoints in `patient-service` (port logic from `PatientRegApptServiceImpl:3362` behind feature flag)
- [ ] Task M01-2: Add COVID-19 vaccination & symptom tracking to patient profile in `patient-service`
- [ ] Task M01-3: Implement Social Worker Assessment model, repository, and REST endpoints in `patient-service`
- [ ] Task M01-4: Create Frontend `features/patient/` feature with Registration Form, Patient Search, Duplicate Merge UI, and Document Uploader

### Module M02: OPD Management
- [ ] Task M02-1: Implement Follow-up visit scheduling, slot reservation, and recurrence rules in `appointment-service`
- [ ] Task M02-2: Add Telemedicine consultation room creation and video link dispatch in `appointment-service`
- [ ] Task M02-3: Add Pre-consultation symptom questionnaire submission and clinician review in `appointment-service`
- [ ] Task M02-4: Implement OPD visit card printing endpoint with PDF/HTML template rendering in `appointment-service`
- [ ] Task M02-5: Create Frontend `features/opd/` with Walk-in Desk, Appointment Booking Calendar, and Doctor Consultation Desk

### Module M04: Billing & Settlement
- [ ] Task M04-1: Implement Credit Notes and Debit Notes ledger adjustment endpoints in `billing-service`
- [ ] Task M04-2: Add IP/OP Bill Estimation calculator and advance deposit forecasting in `billing-service`
- [ ] Task M04-3: Port Doctor Share calculation, add/deduct rules, and visit charge accounting into `billing-service`
- [ ] Task M04-4: Implement Health Package subscription, component tracking, and multi-encounter consumption in `billing-service`
- [ ] Task M04-5: Add mid-billing Sponsor / TPA change workflow and retrospective rate recalculation in `billing-service`
- [ ] Task M04-6: Implement partial payment settlement, split-tender receipting, and remaining balance tracking in `payments-service`
- [ ] Task M04-7: Create Frontend `features/billing/` with Cashier Counter, IP Bill Estimation, Credit Notes, and Advance Management

### Module M07: Pharmacy & Dispensing
- [ ] Task M07-1: Implement Inpatient Cart-Fill / Ward Pick worklist and batch dispensing in `pharmacy-service`
- [ ] Task M07-2: Add IV Compounding and sterile admixture preparation workflow in `pharmacy-service`
- [ ] Task M07-3: Implement Item Sale Returns with stock ledger reversal and credit memo generation in `pharmacy-service`
- [ ] Task M07-4: Add Pharmacist Clinical Interventions, drug allergy checking, and drug-drug interaction warnings in `pharmacy-service`
- [ ] Task M07-5: Implement Drug Master generic-to-brand mapping and substitution rules in `pharmacy-service`
- [ ] Task M07-6: Add Bedside Medication Administration (BCMA) barcode scan verification endpoint in `pharmacy-service`
- [ ] Task M07-7: Implement Home Medication reconciliation and ABDM PHR prescription push in `pharmacy-service`
- [ ] Task M07-8: Create Frontend `features/pharmacy/` with Prescription Queue, Dispensing Workbench, Cart-Fill Manager, and Drug Returns

### Module M09: Nursing & Inpatient Care
- [ ] Task M09-1: Implement Inpatient Care Plans with nursing diagnosis taxonomy, interventions, and outcome goals in `nursing-service`
- [ ] Task M09-2: Add Vitals Charting and Partograph graphical visualization in `nursing-service`
- [ ] Task M09-3: Implement Shift Handover (SBAR) workflow with electronic signature and pending task rollover in `nursing-service`
- [ ] Task M09-4: Add Ward Nurse-to-Patient Allocation matrix in `nursing-service`
- [ ] Task M09-5: Implement Clinical Incident Reporting (medication errors, falls, adverse events) with review status in `nursing-service`
- [ ] Task M09-6: Add Nursing Staff Shift Rostering and duty roster management in `nursing-service`
- [ ] Task M09-7: Implement Allied Health assessment notes (Physiotherapy, Dietetics, Medical Social Work) in `nursing-service`
- [ ] Task M09-8: Create Frontend `features/nursing/` with Ward Inpatient Worklist, Bedside Vitals Recorder, Care Plan Editor, and Handover Screen

### Module M15: Lab & Pathology
- [ ] Task M15-1: Implement Machine-wise Lab Test Review and KDL/OLIS analyzer interface parser in `lab-service`
- [ ] Task M15-2: Port legacy pathology report print templates into Velocity/HTML printable engine in `lab-service`
- [ ] Task M15-3: Implement Lab Test Master and multi-parameter normal reference ranges (age/sex/method specific) in `lab-service`
- [ ] Task M15-4: Create Frontend `features/lab/` with Accessioning Desk, Batch Result Entry, Consultant Verification Queue, and Critical Alerts Board

### Module M35: Identity, Access & Auth
- [ ] Task M35-1: Implement Single Sign-On (SSO / OIDC Keycloak) token validation adapter in `iam` (behind `app.sso.enabled`)
- [ ] Task M35-2: Add user biometric credential registration and audit flag in `iam`
- [ ] Task M35-3: Create Frontend `features/auth/` with Login Screen, OTP Challenge Modal, User Management, and Role Permission Matrix

### Module M38: Interoperability & Integrations
- [ ] Task M38-1: Implement ABHA M1/M2/M3 endpoints (Create ABHA, Link Care Context, Consent Artifacts, PHR Push) in `interop-service`
- [ ] Task M38-2: Add Healthcare Professional Registry (HPR) practitioner lookup and facility linking in `interop-service`
- [ ] Task M38-3: Implement e-Aushadhi drug inventory, indent sync, and QC status synchronization in `interop-service`
- [ ] Task M38-4: Implement FHIR R4 server endpoints (`/fhir/Patient`, `/fhir/Encounter`, `/fhir/Observation`, `/fhir/DiagnosticReport`, `/fhir/MedicationRequest`) in `interop-service`
- [ ] Task M38-5: Implement WhatsApp Business API transactional notification queue and delivery callback handler in `interop-service`

### Module M40: Clinical Forms Engine (TIU)
- [ ] Task M40-1: Implement JSON-schema driven Dynamic Clinical Form Engine with schema versioning and validation
- [ ] Task M40-2: Port top 20 high-volume clinical specialty forms (Pediatrics, OBGYN, Cardiology, Trauma, General Medicine, Pre-Op)
- [ ] Task M40-3: Create Frontend `features/clinical-forms/` with Form Schema Renderer, Note Preview, and PDF Exporter

---

## Phase 2: Operational & Departmental Support (MEDIUM Priority)

### Module M03 & M27: ADT & Queue Frontend
- [ ] Task M03-F: Create Frontend `features/adt/` with Bed Management Board (Ward/Room/Bed visualizer), Admission/Transfer/Discharge flow, and Attendant Pass Print
- [ ] Task M27-F: Create Frontend `features/queue/` with Central Queue Token Display, Doctor Cabin Token Calling Console, and Department Waiting Boards

### Module M05: Tariff & Concession Masters
- [ ] Task M05-1: Implement Tariff Copay rule evaluation engine in `reference-service`
- [ ] Task M05-2: Add Class-wise Package rates and bed-category tier multipliers in `reference-service`
- [ ] Task M05-3: Implement Concession Templates and authorizer threshold rules in `reference-service`
- [ ] Task M05-4: Implement Clinical Order Set templates (pre-defined investigation/medication bundles) in `reference-service`

### Module M08: Clinical Ordering
- [ ] Task M08-1: Implement Order Set instantiation and batch saga dispatch in `ordering-service`
- [ ] Task M08-2: Add Clinical Workflow (CWF/SCWF) multi-step order approval engine in `ordering-service`
- [ ] Task M08-3: Implement External Lab/Diagnostic order referral details and result attachment in `ordering-service`

### Module M12: OT & Surgery
- [ ] Task M12-1: Implement Surgical Procedure Performance checklist (WHO Surgical Safety Checklist) in `ot-service`
- [ ] Task M12-2: Add Anesthesia Pre-Op assessment, intra-op monitoring, and post-anesthesia recovery scoring in `ot-service`
- [ ] Task M12-3: Add Surgical Equipment and Surgeon/Nurse staffing scheduling in `ot-service`
- [ ] Task M12-4: Implement OT Billing cost estimation and implant usage ledger in `ot-service`
- [ ] Task M12-5: Create Frontend `features/ot/` with OT Room Schedule Board, Surgical Checklist, and Anesthesia Record

### Module M13: Inventory Management
- [ ] Task M13-1: Implement Item Enquiry and batch-wise stock visibility across hospital stores in `item-service`
- [ ] Task M13-2: Add Expiry & Damage Return workflow with supplier debit note triggers in `item-service`
- [ ] Task M13-3: Implement Automated Reorder Level (ROL) calculation and min-max stock alerts in `item-service`
- [ ] Task M13-4: Implement Inter-Store Stock Transfer Indent, Dispatch, and Acceptance in `item-service`
- [ ] Task M13-5: Create Frontend `features/inventory/` with Stock Overview, Indent Generator, Stock Issue/Receive, and Adjustment Worklist

### Module M14: Procurement
- [ ] Task M14-1: Implement Purchase Quotation entry and comparative statement generator in `item-service`
- [ ] Task M14-2: Add Multi-level Purchase Order (PO) approval workflow and vendor dispatch in `item-service`
- [ ] Task M14-3: Implement Provisional GRN, Consignment GRN, and QC Inspection approval in `item-service`
- [ ] Task M14-4: Add Rate Contract master, validity tracking, and price ceiling enforcement in `item-service`

### Module M16: Radiology
- [ ] Task M16-1: Implement Radiology Reschedule and Retest workflows in `radiology-service`
- [ ] Task M16-2: Add PACS / DICOM viewer web integration link and accessioning bridge in `radiology-service`
- [ ] Task M16-3: Implement Structured Radiology Report Templates with speech-to-text placeholder support in `radiology-service`
- [ ] Task M16-4: Create Frontend `features/radiology/` with Radiology Worklist, Report Editor, and DICOM Viewer Bridge

### Module M17: Blood Bank
- [ ] Task M17-1: Implement Blood Component Separation (PRBC, FFP, Platelets, Cryoprecipitate) and bag barcode generation in `bloodbank-service`
- [ ] Task M17-2: Add Blood Bag lifecycle trail, temperature breach discard, and transfusion reaction logging in `bloodbank-service`
- [ ] Task M17-3: Implement Transfusion Transmissible Infections (TTI / TTD) serology testing validation before issue in `bloodbank-service`
- [ ] Task M17-4: Add e-Raktkosh national portal synchronization client in `bloodbank-service`
- [ ] Task M17-5: Port 17 legacy statutory Blood Bank reports into `bloodbank-service`
- [ ] Task M17-6: Create Frontend `features/bloodbank/` with Donor Registration, Component Preparation, Cross-Matching Desk, and Blood Issue Board

### Module M25 & M26: Reporting & Patient Tabs
- [ ] Task M25-1: Implement top 15 statutory and operational reports in `reporting-service` (OPD Census, IPD Bed Occupancy, Revenue Summary, Morbidity, Birth/Death)
- [ ] Task M25-2: Add embedded BI dashboard aggregation endpoints in `reporting-service`
- [ ] Task M26-1: Implement Patient Comprehensive Care Tabs (Goals, Interventions, Meds, Investigations) in `nursing-service`
- [ ] Task M25-F: Create Frontend `features/reports/` with Report Catalog, Parameter Filter Form, and Multi-Format Exporter (PDF/Excel/CSV)

### Module M32: Revenue Cycle & Schemes
- [ ] Task M32-1: Implement District Medical Officer (DM) Approval and MOIC document upload in `rcm-service`
- [ ] Task M32-2: Add Pre-Discharge Insurance Claim Verification and package audit in `rcm-service`
- [ ] Task M32-3: Implement Chiranjeevi / state health scheme verification and claim packet generation in `rcm-service`

### Module M39: Platform & Commons
- [ ] Task M39-1: Implement Centralized System Audit Trail logger with tamper-evident record hashing in `reference-service`
- [ ] Task M39-2: Add Hospital Facility Geotagging and Sub-center hierarchy management in `reference-service`
- [ ] Task M39-3: Implement 2D Barcode Patient Card scan & rapid check-in endpoint in `reference-service`

---

## Phase 3: Auxiliary & Specialized Services (LOW Priority)

### Modules M06, M10, M11: Shared Clinical Services
- [ ] Task M06-1: Implement Doctor Incentive & Share Ledger calculation module
- [ ] Task M10-1: Implement Kitchen Diet Production order board and meal delivery checklist in `diet-service`
- [ ] Task M11-1: Implement CSSD Kit Sterilization machine log, autoclave biological indicator tracking, and store issue in `cssd-service`

### Modules M18, M19, M20, M21, M22, M23, M24: Operations & Support
- [ ] Task M18-1: Implement Bio-Medical Waste bag barcoding and disposal vendor handover in `waste-service`
- [ ] Task M19-1: Implement Hospital Linen laundry cycle tracking and vendor reconciliation in `linen-service`
- [ ] Task M20-1: Implement Biomedical Equipment breakdown ticketing, preventive maintenance (PM) calibration schedule, and medical gas log in `equipment-service`
- [ ] Task M21-1: Implement Ambulance / Transport fleet vehicle allocation, fuel log, and maintenance in `transport-service`
- [ ] Task M22-1: Implement Citizen Grievance feedback form and document attachment upload in `grievance-service`
- [ ] Task M23-1: Implement RMRS Treasury journal entries, suspense accounts, and bank reconciliation in `rmrs-service`
- [ ] Task M24-1: Implement PMRD Janani Suraksha Yojana (JSY) statutory forms (J1, J2, J3, H-Form) in `govt-service`

### Modules M28, M29, M30, M31, M33, M36, M37: Specialized Programs
- [ ] Task M28-1: Implement Mortuary cold chamber allocation, post-mortem notes, and death certificate generation in `adt-service`
- [ ] Task M29-1: Implement Chemotherapy regimen cycle planning and oncology nurse flow sheet in `ot-service`
- [ ] Task M30-1: Implement BCMA 5-Rights bedside verification rule engine in `pharmacy-service`
- [ ] Task M31-1: Implement Patient Self-Service Kiosk registration and queue check-in API
- [ ] Task M33-1: Implement Direct Benefit Transfer (DBT) incentive claim processing in `payments-service`
- [ ] Task M36-1: Implement ASHA Maternal & Child Tracking (ANC/PNC) and immunization registers in `govt-service`
- [ ] Task M37-1: Implement Aasmaan Field Worker sync and e-ticket submission in `govt-service`
- [ ] Task M25-3: Implement remaining 31 legacy operational and statistical report schemas in `reporting-service`

---

## Verification & Completion Criteria
1. **Compilation & Build:** All 30 microservices build with `./mvnw clean compile` and pass checkstyle/linter rules.
2. **Automated Testing:** All new microservice features contain unit tests (`mvn test`) achieving $>80\%$ domain logic coverage.
3. **Frontend Integration:** All frontend pages in `frontend/src/features/` pass `npm run typecheck`, `npm run lint`, and build cleanly with `npm run build`.
4. **Gap Analysis Register:** As tasks complete, update `docs/modernization/GAP-ANALYSIS-AND-PLAN.md` with module completion status.
