# REMAINING GAPS SPECIFICATION — HIMS MODERNIZATION

> **Source**: `docs/modernization/GAP-ANALYSIS-AND-PLAN.md`, `EXECUTION-BACKLOG.md`, `specs/SPEC.md`  
> **Target**: `services/*` (Java 21 + Spring Boot 3 + DDD) and `frontend/src/features/*` (React + Vite + TypeScript + Zod + TanStack Query)  
> **Legacy Monolith Source**: `/Users/adityabanerjee/personal_project/ihms/src` (`com.ohum`)

---

## 1. Specification Overview
This document specifies the remaining gaps and task breakdown for completing the modernization of the legacy IHMS hospital management system into the new microservices architecture. It is designed to be executed iteratively using `iteratr` (`iteratr build --spec specs/SPEC.md`).

---

## 2. Gap Summary by Priority

### Phase 1: High Priority Clinical & Financial Spine
* **M01 (Patient Management):** Aadhaar OTP/Biometric, COVID tracking, Social Worker Assessment, Frontend Patient feature.
* **M02 (OPD):** Follow-up visits, Telemedicine, Pre-consult questionnaires, OPD card PDF generation, Frontend OPD feature.
* **M04 (Billing & Payments):** Credit/Debit notes, IP/OP Bill estimation, Doctor share accounting, Package consumption, Sponsor change, Split payments, Frontend Billing feature.
* **M07 (Pharmacy):** Cart-fill/ward pick, IV compounding, Item returns & reversals, Clinical interventions & allergy checks, BCMA barcode scan, ABDM PHR push, Frontend Pharmacy feature.
* **M09 (Nursing):** Inpatient care plans, Vitals & partograph charts, SBAR shift handover, Nurse allocation matrix, Incident reporting, Staff rostering, Frontend Nursing feature.
* **M15 (Pathology/Lab):** Machine analyzer interface (KDL), 201 legacy print templates, Multi-parameter reference ranges, Frontend Lab feature.
* **M35 (IAM/Auth):** SSO/Keycloak OIDC adapter, Biometric audit flags, Frontend Auth & permissions feature.
* **M38 (Interoperability):** ABHA M1/M2/M3 endpoints, HPR registry, e-Aushadhi sync, FHIR R4 server endpoints, WhatsApp notification queue.
* **M40 (Clinical Forms / TIU):** Dynamic JSON schema forms engine, Top 20 clinical specialty forms, Frontend Clinical Forms feature.

### Phase 2: Medium Priority Operations & Departments
* **M03/M27 Frontend:** ADT Bed Management visualizer board, Queue Calling Console & Department Displays.
* **M05 (Tariffs & Concessions):** Copay rules engine, Package class rates, Concession authorizer thresholds, Order sets.
* **M08 (Clinical Orders):** Order set batch instantiation, CWF/SCWF multi-step order approvals.
* **M12 (OT & Surgery):** WHO surgical safety checklist, Anesthesia pre/intra/post-op records, Staff scheduling, OT bill estimation, Frontend OT feature.
* **M13 (Inventory):** Item enquiry, Expiry/damage returns, Reorder level alerts, Inter-store transfers, Frontend Inventory feature.
* **M14 (Procurement):** Quotation comparative statement, Multi-level PO approvals, Provisional/Consignment GRN, Rate contracts.
* **M16 (Radiology):** Reschedule/retest, PACS/DICOM web bridge, Structured report templates, Frontend Radiology feature.
* **M17 (Blood Bank):** Component separation, Blood bag lifecycle trail, TTI serology validation, e-Raktkosh sync, 17 reports, Frontend Blood Bank feature.
* **M25 (Reporting & BI):** Top 15 core statutory reports, BI dashboards, Frontend Reports feature.
* **M26 (Worklists):** Patient Comprehensive Care Tabs (Goals, Meds, Labs, Investigations).
* **M32 (RCM & Schemes):** DM/MOIC document approvals, Pre-discharge insurance audits, Chiranjeevi claim packets.
* **M39 (Platform & Commons):** Audit trail hashing, Hospital geotagging, Barcode patient scan check-in.

### Phase 3: Low Priority Specialized Modules
* **M06 (Doctor Share):** Commission rules & payout ledger.
* **M10 (Diet):** Kitchen production order board.
* **M11 (CSSD):** Kit sterilization machine logs & biological indicator tests.
* **M18 (BMW):** Waste bag barcoding & vendor handover.
* **M19 (Linen):** Laundry cycle tracking & vendor reconciliation.
* **M20 (Equipment):** Asset breakdown ticketing & PM calibration schedules.
* **M21 (Transport):** Ambulance fleet logs, fuel tracking, maintenance approvals.
* **M22 (Grievance):** Citizen feedback forms & document attachments.
* **M23 (RMRS):** Treasury journal vouchers, suspense reconciliation, bank statements.
* **M24 (PMRD):** JSY statutory forms (J1, J2, J3, H-form).
* **M28 (Mortuary):** Cold chamber allocation & death certificates.
* **M29 (Chemotherapy):** Regimen cycle planning & infusion coversheet.
* **M30 (BCMA):** 5-Rights bedside verification rules.
* **M31 (Kiosk):** Touchscreen patient self-registration.
* **M33 (Payment Transfer):** Direct benefit transfer (DBT) claims.
* **M36 (EJS/ASHA):** Maternal & child tracking registers (ANC/PNC).
* **M37 (Aasmaan):** Field app synchronization & e-tickets.
* **M25 (Remaining Reports):** 31 operational & statistical report schemas.

---

## 3. Execution Instructions for Iteratr
To run the automated agent build loop using `iteratr`:

```bash
# Verify environment dependencies
iteratr doctor

# Run the modernization loop with the master spec
iteratr build --spec specs/SPEC.md --model anthropic/claude-sonnet-4-5
```