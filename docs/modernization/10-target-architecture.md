# iHMS / OHUM PAS NG — Preliminary Target Architecture: Microservices + PostgreSQL

**Status: PRELIMINARY — first-pass design. All service names are candidates under validation; discovery continues (see `_discovery/agent-10/findings.md` for the evidence base).**
**Input:** `D:\ihms` monolith — Spring Boot 2.7.5, CXF JAX-RS (~135 service impls), EclipseLink JPA/JTA, Oracle + 719 PL/SQL procs, Solr Cloud (6 collections), Redis (cache), FAUG server-side-rendered frontend.
**Author:** Agent 10 (Microservices / PostgreSQL Modernization Architect). **Date:** 2026-08-14.

---

## 1. Executive summary

The legacy monolith is *stored-procedure-centric*: Java services are thin JAX-RS controllers around 719 Oracle procs, with Solr/HL7/ABDM/WhatsApp side effects bolted on inside the same Spring transactions. Boundary analysis (evidence in findings.md §2, §6) yields ~29 candidate contexts. The two structural facts that shape everything:

1. **One mega-transaction** couples Order → Bill → Lab order → Rad order → DoctorShare → EMR → HL7 (`AddServiceOrderServiceHelper.java:341-516`).
2. **Master data is a god-node** (`MasterCachedServiceImpl`, 514 graph edges) with Redis-cached reference data consumed by ~30 services.

The decomposition therefore must be *event-first*: the legacy already contains three DB-polled async pipelines (WhatsApp queue, ABDM queue, HL7 outbox) that are proven patterns to replace with Kafka, and Solr indexing that is already asynchronous via jobs. PostgreSQL conversion follows the mapping rules in §5 (evidence table in findings.md §5).

---

## 2. Bounded context map (preliminary)

### 2.1 Ownership table

Legend: **E**=emits event, **C**=consumes event, **R**=reads API/cache, **S**=synchronous REST call.

| Service (candidate) | Business responsibility | Owned entities / tables | REST APIs (preliminary) | Kafka produced | Kafka consumed | Redis | Solr | Depends on | Legacy sources |
|---|---|---|---|---|---|---|---|---|---|
| **1. Patient Identity & Registration** | Patient registry, demographics, sponsor, ABHA linkage, family | T_REGISTRATION(+DETAILS), T_PATIENTSPONSOR, T_PATIENT_ABDM, T_REGISTRATIONQUEUE | /patients (CRUD), /patients/{id}/sponsor, /patients/merge | PatientRegistered | (none) | patient lookup? (no — Solr) | — | IAM (token), ABDM via Interop | PatientServiceImpl, PatientDetailRegistrationServiceImpl, PatientRegAdmissionServiceImpl, CIMS_ADDPATIENTREGISTRATION* |
| **2. Appointment & Encounter (OPD)** | Appointment booking, walk-in/emergency visits, encounter lifecycle, doctor queues | T_APPOINTMENT, T_ENCOUNTER, T_PATIENTTOKEN, doctor queue/cabin tables | /appointments, /encounters, /visits, /queues | EncounterCreated, AppointmentBooked, VisitClosed | PatientRegistered | session/queue state (short-lived) | — | Patient (S), IAM | AppointmentServiceImpl, EmergencyAndWalkinVisitServiceImpl, QueueManagmentServiceImpl, AddUpdateAppointmentServiceImpl |
| **3. ADT & Bed** | Admission, transfer, discharge, bed/ward state, MLC | T_ADMISSION, T_BEDRESERVATION, T_TRANSFERREQUEST, T_PATIENTDISCHARGEDATA, T_MLCDETAILS, T_ATTENDANTPASS, bed/ward/room/cabin masters | /admissions, /beds, /transfers, /discharges | PatientAdmitted, PatientDischarged, BedChanged, TransferRequested | EncounterCreated | bed availability (cache) | — | Encounter (S), IAM | IPDAdmissionServiceImpl, BedServiceImpl, BedReservationServiceImpl, BedManagementServiceImpl, PatientTransferDischargeServiceImpl, CIMS_ADDADMISSIONINFORMATION, CIMS_UPDATEBEDOCCUPIEDFLAG_1 |
| **4. Ordering & Prescription (Clinical)** | Clinical orders, prescriptions, notes, EMR details, ordersets | T_PATIENTADVICE, T_PATIENTEORDER, T_PATIENTMEDICATION, T_PATIENTNOTE, T_EMRDETAILS, T_EMRSERVICES, T_CONSULTORDER | /orders, /prescriptions, /notes, /emr | PrescriptionCreated, OrderCreated | PatientRegistered, EncounterCreated, PatientAdmitted | — | — | Patient, Encounter/Admission (S), Reference (R), Billing (E) | AddServiceOrderServiceImpl, ClinicalServiceImpl, PrescriptionMasterServiceImpl, MedicationServiceImpl, CIMS_ADDORDERDETAILS |
| **5. Billing & Settlement** | Bill header/lines, charges, doctor bills & shares, concessions, credit notes, estimation | T_BILL, T_BILLDETAILS, T_CHARGE, T_DOCTORBILL, T_DOCTORSHARE, T_CONCESSION, T_CREDITNOTE | /bills, /charges, /estimates, /concessions, /creditnotes | InvoiceGenerated, BillCancelled, ChargeAdded | LabOrderCreated, PrescriptionCreated, OrderCreated (post-saga), BedChanged (bed charges) | tariff/rate lookup (cache) | — | Reference (S tariff), Payment (E), Patient/Encounter (R) | BillServiceImpl, BillDetServiceImpl, ChargeServiceImpl, ConcessionTemplateServiceImpl, PatientBillEstimationServiceImpl, CIMS_ADDBILL, CIMS_ADDCHARGE, CIMS_ADDCONCESSION |
| **6. Payments & Refunds** | Payment capture, modes, advances, allocation, refunds, receipts, cancellations | T_PAYMENT, T_PAYMENTMODEDETAILS, T_ADVANCE, T_REFUND, T_RECEIPT | /payments, /advances, /refunds, /receipts, /cancellations | PaymentReceived, RefundIssued, AdvanceCreated | InvoiceGenerated | short-lived dedup (upload attempts, token) | — | Billing (S/E), RMRS (E), RCM (C) | PaymentServiceImpl, AddPaymentServiceImpl+Helper, AddAdvanceServiceImpl, RefundServiceImpl, BillCancellationServiceImpl, CIMS_ADDPAYMENT*, CIMS_ADDADVANCE |
| **7. Lab (Pathology)** | Lab order booking, specimen, parameters, results, machine interface, reports | T_PATHORDERBOOKING(+DETAIL), T_PATHOPATIENTPARAMETERDETAILS, T_MACHINECONFIGURATION, patho masters | /lab/orders, /lab/results, /lab/machines | LabOrderCreated, LabResultFinalized | OrderCreated, PatientRegistered, EncounterCreated | — | — | Patient (R), Reference (R), Interop (E WhatsApp/EHR) | PathoOrderServiceImpl, PathoOrderServiceHelper, PatientRequisitionServiceImpl, CIMS_ADDPATHORDERBOOKING |
| **8. Radiology** | Rad order booking, modality scheduling, templates, results | T_RADORDERBOOKING(+DETAILS), rad masters | /rad/orders, /rad/results | RadOrderCreated, RadResultFinalized | OrderCreated | — | — | Patient (R), Reference (R) | RadiologyServiceImpl, AddServiceOrderServiceHelper:579-599, CIMS_ADDRADORDERBOOKING |
| **9. Pharmacy & Dispensing** | Prescription dispense, compound, ward picklist, drug intervention, EMR number | T_PRESCRIPTIONMASTER, T_ITEMSALE, T_PRESCRIPTIONDISPENSEDETAILS, T_ITEMCOMPOUNDDETAILS, ward picklist | /pharmacy/dispense, /pharmacy/picklist, /pharmacy/interventions | DispensingCompleted | PrescriptionCreated, InventoryUpdated | stock snapshot (lookup only) | — | Inventory (S for stock ops), Clinical (C), Billing (E) | PharmacyServiceImpl, DrugServiceImpl, ItemSalesServiceImpl, CIMS_GETPRESCRIPTIONWARDDETAILS*, CIMS_ADDITEMSTOCK |
| **10. Inventory & Procurement** | Item master, stock, batch, PO/GRN, indent, supplier, rate contract, EAushadhi | M_ITEMMASTER, T_ITEMSTOCK, T_ITEMBATCHMASTER, T_PURCHASEORDER, T_GRN*, T_INDENT, T_SUPPLIER, T_RATECONTRACT, T_QUOTATION, T_CONSIGNMENTGRN | /items, /stock, /purchase-orders, /grn, /indents, /suppliers | InventoryUpdated, StockAdjusted, PurchaseOrderPlaced, GRNPosted | DispensingCompleted, MaterialConsumed (OT/CSSD), auto-indent triggers | itemMaster (cache, owned by Reference actually) | item collection | Reference (R item/company/tax), EAushadhi (external) | InventoryServiceImpl, PurchaseOrderServiceImpl, QuotationServiceImpl, ItemStockServiceImpl, ItemMasterServiceImpl, InventoryEAushadiIntegrationImpl, CIMS_ADDITEMSTOCK, CIMS_ADDGRN, CIMS_ADDINDENT |
| **11. OT & Surgery** | OT scheduling, procedure notes, anesthesiology, surgery details | T_OTSCHEDULE(+DETAILS), T_SURGERYDETAILS, procedure masters, T_ANESTHESIANOTES | /ot/schedules, /ot/surgeries, /procedures | ProcedureScheduled, SurgeryCompleted | OrderCreated (procedure), PatientAdmitted | — | patient search (R) | Patient (R via Search), Inventory (E consumption), Billing (E) | OTDetailsServiceImpl, OTSchedulerServiceImpl, OtConfigServiceImpl, SurgeryDetailsServiceImpl |
| **12. Nursing & Inpatient Care** | Nursing notes, vitals, task planning, I/O charting, station roster | T_NURSENOTE, T_PATIENTVITALS, T_NURSETASKDETAILS, T_INTAKEOUTPUT, nursing station masters | /nursing/notes, /nursing/vitals, /nursing/tasks | VitalsRecorded, NurseTaskCompleted | PatientAdmitted, PatientDischarged, OrderCreated | — | — | Encounter/Admission (R) | NursingServiceImpl, NursePatientDetails |
| **13. Blood Bank** | Donor registry, blood bag/component lifecycle, cross-match, issue, eRaktkosh | T_BLOODBAG, T_BLOODCOMPONENT, T_BLOODREQUISITION, T_BLOODDONOR, T_BLOODDONATIONCAMP, donor masters | /bloodbank/bags, /bloodbank/requisitions, /bloodbank/donors | BloodRequisitionCreated, BloodIssued | PatientAdmitted, LabResultFinalized (serology) | — | — | Patient (R), Inventory (C for products) | BloodBankServiceImpl, eRaktkosh jobs (ApplicationJobs.java:1205,1383) |
| **14. Diet** | Diet orders, item scheduling, kitchen menu, intake tracking | T_DIETORDER, T_DIETITEMSCHEDULEDETAILS, T_TODAYDIETITEMMENU, diet/food masters | /diet/orders, /diet/menu | DietOrderCreated | PatientAdmitted | — | — | Admission (R) | DietServiceImpl |
| **15. CSSD** | Kit formation, sterilization cycles, issue to OT/procedure | T_CSSDKITFORMATION, T_CSSDKITISSUEITEM, T_STERILIZATION*, T_PROCEDURECSSD* | /cssd/kits, /cssd/sterilization, /cssd/issues | KitIssued, SterilizationCompleted | ProcedureScheduled, OrderCreated (procedure) | — | — | Inventory (C), OT (R) | CSSDServiceImpl |
| **16. Linen & Laundry** | Linen category/detail, ward requests | T_LINENREQUEST, T_LINENCATEGORY, T_LINENDETAIL | /linen/requests | LinenRequested | PatientAdmitted | — | — | Ward (R) | LinenLaundryServiceImpl |
| **17. Transport** | Ambulance, vehicle, driver, transport requests, vehicle maintenance | T_TRANSPORTREQUEST, T_VEHICLEMAINTENANCE, M_AMBULANCEMASTER, M_DRIVERMASTER | /transport/requests, /vehicles | TransportRequested | (none) | — | — | Patient/Admission (R) | TransportServiceImpl |
| **18. Equipment Management** | Equipment registry, complaint/resolution, PM schedules, warranty/AMC/insurance | T_EQUIPMENT, T_EQPCOMPLAINT(+RESOLUTION), T_EQPPM*, warranty/insurance tables | /equipment, /equipment/complaints, /equipment/pm | EquipmentComplaintRaised, PMDue | (none) | — | — | Inventory (C for spares) | EquipmentServiceImpl, PreventiveMaintenanceServiceImpl, EquipmentServiceHelper |
| **19. Bio-Medical Waste** | Waste segregation, bag tracking, disposal | T_BIOMEDICALWASTE(+DETAILS), T_WASTEBAGMASTER | /bmw/entries | BMWRecorded | (none) | — | — | Ward (R) | BioMedicalWasteManagementServiceImpl |
| **20. Grievance & Feedback** | Complaints, resolutions, feedback questionnaires | T_PATIENTCOMPLAINTS, T_PATIENTGRIEVANCE*, T_COMPLAINTMASTER, T_FEEDBACKTEMPLATE | /grievances, /complaints, /feedback | GrievanceRaised, GrievanceResolved | PatientDischarged (satisfaction trigger) | — | — | Patient (R) | ComplaintDetailsServiceImpl, ComplaintResolutionDetailServiceImpl, QuetionnaireServiceImpl |
| **21. Reference Master Data** | All M_* masters: service, tariff, unit, department, company, tax, store, bank, ICD10CM, health factor, drug/patho/rad masters; package/plan; cache ownership | M_* (609 entities) + cache-key contract | /masters (CRUD per master), /tariffs, /packages, /plans, /services | MasterDataChanged, TariffChanged, PackageChanged | (none) | **owns all master cache keys** (itemMaster, tariffMaster, unitMaster, classMasterMap_*…) | service, icd10cm, healthfactor collections | IAM (admin), Upload (bulk) | MasterServiceImpl, MasterCachedServiceImpl, MasterCachedServiceHelper, MdmServiceImpl, AddServiceMasterImpl, ServiceMasterServiceImpl, TariffServiceImpl, PackageServiceServiceImpl, PlanMasterServiceImpl, CIMS_ADDSERVICEMASTER2, CIMS_ADDTARIFFSERVICE2 |
| **22. Security & IAM** | AuthN/AuthZ, LDAP, users, roles, per-unit/per-store/per-ward grants, staff/doctor profiles | T_USERDETAILS, T_USERROLES, T_USERROLEMASTER, T_USERUNITDETAILS, T_USERSTOREDETAILS, T_USERWARDDETAILS, M_MENUMASTER, M_STAFFMASTER, M_DOCTORMASTER | /auth/login, /auth/otp, /users, /roles, /grants | UserCreated, RoleChanged | (none) | session/token (short-lived) | — | LDAP (external) | AuthenticationServiceImpl, UserManagementServiceImpl, AddUserServiceImpl, Encrypter |
| **23. RCM & Claims** | Eligibility, pre-auth, claims (NHCX/HCX), TID tracking | T_ELIGIBILITYCHECK, T_ADMISSIONTIDDETAILS, T_NHCXREQUEST(+DETAILS) | /rcm/eligibility, /rcm/preauth, /rcm/claims | ClaimSubmitted, EligibilityChecked | PatientAdmitted, InvoiceGenerated, PaymentReceived | — | — | Billing (R), Payment (C), Interop (E to NHCX) | RcmServiceImpl, NHCXServiceImpl |
| **24. RMRS & PMRD (Treasury)** | Treasury head/authority mapping, collections, receipts for govt accounts | T_RMRSCOLLECTION, T_AUTHORITYMASTER, T_HEADSMASTER, T_LEDGERMASTER, T_ACCOUNTING* | /rmrs/collections, /treasury/heads | CollectionPosted | PaymentReceived | — | — | Payment (C) | RMRSServiceImpl, PmrdServiceImpl |
| **25. Interop & Notifications** | HL7 out/in, EHR file gen, ABDM/ABHA, HPR, WhatsApp/SMS/email, webhook callbacks | T_OUTBOUNDHIS, T_INBOUNDHIS, T_ABDMREQUESTQUEUE, T_WHATSAPPMESSAGEQUEUE/STATUS, T_EHRJOBDETAILS | /interop/hl7, /interop/abdm, /interop/whatsapp/callback, /interop/fhir | (consumes; emits external calls) | PatientRegistered, PatientAdmitted, LabResultFinalized, InvoiceGenerated, PaymentReceived, PatientDischarged | dedup/token caches | — | External: HIS, ABDM gateway, WhatsApp Business API, FHIR | EHRUtilServiceImpl, EHRSyncJobs, Hl7InboundMessageJob, HL7Helper, AbhaServiceImpl, HPRServiceImpl, IntegrationServiceImpl, SendNotificationServiceImpl, ApplicationJobs.java:1611-1688 |
| **26. Reporting & Analytics** | On-demand reports (Jasper/Velocity), dashboards, stats counts, regulatory exports | T_REPORTMASTER, stats/archive tables, PDF blob store | /reports, /dashboard, /exports | ReportGenerated | all domain events (materialized views via ELT) | dashboards (cache) | — | All (read-model) | ReportServiceImpl, FileManagementServiceImpl, BillInvoiceGenerator, ApplicationJobs (updateIhmsSummaryCounts, addStats_Archived) |
| **27. Search & Index** | Solr ingestion pipeline + search API | (no tables — owns Solr collections) | /search/patients, /search/encounters, /search/items, /search/icd10cm | — | PatientRegistered, EncounterCreated, ItemUpdated, DiagnosisUpdated, LabResultFinalized, InvoiceGenerated (bill search) | — | **owns all 6 collections** | Domain services (C) | SolrUtil, BaseServiceImpl:656-660, CommonUtilServiceImpl:2704, ApplicationJobs:867-977,2122 |
| **28. Govt Programs (EJS/Aasmaan/Chiranjeevi)** | ASHA/ANC-PNC/child program, incentives; AASMAAN case management; Chiranjeevi enrolment | T_EJS_*, T_AASMAAN_*, T_BENEFICIARYENROLMENT, T_REFRENCETID | /ejs/*, /aasmaan/*, /chiranjeevi/* | IncentiveComputed, CaseUpdated | PatientRegistered | — | — | Patient (R), Finance (E) | Ejs* services, Aasmaan* services, EjsIncentiveServiceImpl, ApplicationJobs.java:1035 |
| **29. Queue & Kiosk** | Registration queue, doctor queue rooms, kiosk/machine mapping, tokens | T_PATIENTQUEUE, T_OPDSTATIONMASTER, T_TABLETMAPPING | /queue, /kiosk, /tokens | TokenIssued | EncounterCreated, AppointmentBooked | live queue state (short-lived) | — | Encounter (R) | QueueManagmentServiceImpl, PatientPortalServiceImpl |

### 2.2 Shared / borrowed data strategy

| Shared data | Owner | Sharing mechanism | Notes / evidence |
|---|---|---|---|
| Patient identity (T_PATIENT*) | Patient (1) | REST /patients/{id} + PatientRegistered event payload (read-model) | Legacy: all modules join T_REGISTRATION directly (findings §3) |
| Encounter / Admission keys | OPD (2) / ADT (3) | Events (EncounterCreated, PatientAdmitted) carry the key; other services keep **local reference copies** (encounterId, admissionId) | Legacy: every module keys off opdIpdId/admissionId (findings §3) |
| Tariff / Service / Package rates | Reference (21) | REST rate-lookup API + Redis cache keys (owned by 21); TariffChanged event invalidates | `AddServiceMasterImpl.java:207-244`; cache keys §3 findings |
| Item + Stock + Batch | Inventory (10) | REST /stock APIs; InventoryUpdated event; **Pharmacy borrows stock ops via API (decision in §6.3)** | Dual-write hotspot `PharmacyServiceImpl.java:2140` |
| Bill status / settlement state | Billing (5) | InvoiceGenerated/PaymentReceived events; payment-service updates bill via REST | `AddPaymentServiceHelper.java:463` (CIMS_UPDATEBILLFORCONCESSION) |
| Users / grants | IAM (22) | JWT + /users API; UserCreated event for local caches | `AuthenticationServiceImpl.java:66-293` |
| Solr collections | Search (27) | Event-driven pipeline; read via /search API | `SolrUtil.java:174-333` |
| Queues (whatsapp/ABDM/HL7) | Interop (25) | Kafka topics (replacing DB polls) | `ApplicationJobs.java:1611,1661` |

Rules: **no direct cross-service DB access; no shared tables in PG; borrowed data is read-model fed by events; cache keys have a single owner (Reference).**

---

## 3. PostgreSQL design principles

1. **Schema-per-service**: each bounded context owns its PG schema (`ihms_patient`, `ihms_billing`, `ihms_master`, …). Monolith-era `T_*`/`M_*` tables are redistributed to the owning schema.
2. **IDs**: prefer preserved sequences for legacy tables during strangler (external references: MRNO, IPDNO, bill no, HL7 control IDs); new tables use `BIGSERIAL`/`IDENTITY`.
3. **Type mapping** (full evidence table in findings.md §5):

| Oracle | PostgreSQL | Notes |
|---|---|---|
| NUMBER(19) | BIGINT | PKs/FKs (all Long IDs) |
| NUMBER(10) / NUMBER | INTEGER / NUMERIC(p,s) | counters; money stays NUMERIC(19,2) |
| VARCHAR2(n) | VARCHAR(n) | size audit needed (n≤4000 typical in procs) |
| CHAR(1) '1'/'0' | CHAR(1) or BOOLEAN | keep app-side mapping (`getBooleanValueForQuery`) |
| CLOB | TEXT | 12 @Lob entities found |
| DATE | TIMESTAMP(3) (or DATE where date-only semantics: DOB, RegistrationDate) | TRUNC() comparisons imply time semantics (findings §5 row 5) |
| TIMESTAMP(3) | TIMESTAMP(3) | procs already use TIMESTAMP(3) |
| SEQUENCE.NEXTVAL FROM DUAL | BIGSERIAL / explicit sequence | legacy ID continuity (findings §5 row 6) |
| NVL | COALESCE | |
| SYSDATE | CURRENT_TIMESTAMP / NOW() | |
| TRUNC(SYSDATE) / TRUNC(d,'MM') | CURRENT_DATE / DATE_TRUNC('day'/'month', d) | |
| ADD_MONTHS | NOW() - INTERVAL '1 month' | |
| ROWNUM<=n / ROWNUM=1 | LIMIT n / LIMIT 1 | |
| paginate(a,b) | LIMIT b OFFSET a | |
| MERGE INTO | INSERT … ON CONFLICT … DO UPDATE | |
| to_char(to_date(...)) | TO_CHAR/TO_DATE (portable) | |
| DECODE / CONNECT BY | CASE / WITH RECURSIVE | **unverified — audit procs** |
| Stored procedures | phased: PG functions (strangler) → service logic | 719 procs; conversion per extraction path |
| Oracle driver/dialect | PostgreSQL driver; EclipseLink → Hibernate (recommended) or EclipseLink PG dialect | |

4. **Concurrency hotspots**: T_ITEMSTOCK decrement must use row-level locking (SELECT … FOR UPDATE / UPDATE … WHERE returning) same as legacy procs; T_ENCOUNTER/T_BILL get partitioning by year (volume open question §10.4 findings).
5. **Boolean/state enums**: legacy char-coded states (e.g. itemschedulestatus < 6, indents 1/3) become SMALLINT + CHECK constraints with a documented enum map.
6. **Migration mechanics**: `pg_dump`-compatible DDL from converted entities; data move per wave with idempotent backfill; sequence reseed to max(id)+1.

---

## 4. Communication matrix (preliminary rationale per dependency)

| From → To | Type | Rationale (legacy evidence) |
|---|---|---|
| Clinical(4) → Billing(5) | **REST (phase 1) → Kafka InvoiceGenerated (phase 2)** | Today atomic (`AddServiceOrderServiceHelper.java:341-516`); start with REST+local txn via saga choreography; eventing after outbox lands |
| Billing(5) → Payments(6) | Kafka InvoiceGenerated + REST settlement update | Legacy: payment procs update bill rows (`AddPaymentServiceHelper.java:463`) |
| Lab(7)/Rad(8) → Billing(5) | Kafka LabOrderCreated/RadOrderCreated (post-split) | Currently same txn as bill |
| Lab(7) → Interop(25) | Kafka LabResultFinalized | Replaces T_WHATSAPPMESSAGEQUEUE polling (`ApplicationJobs.java:1611`) |
| Patient(1)/OPD(2)/ADT(3) → Search(27) | Kafka PatientRegistered/EncounterCreated/PatientAdmitted | Legacy solr index calls (`PatientServiceImpl.java:5610`, `SolrUtil.java:174-333`) |
| ADT(3) → Billing(5) | Kafka BedChanged (bed auto-charges) | Legacy job CIMS_JOBSCHEDULING (`ApplicationJobs.java:283`) |
| Payments(6) → RMRS(24) | Kafka PaymentReceived | Legacy RMRS reads collection details (`RMRSServiceImpl.java:1361-1366`) |
| All → IAM(22) | REST authn (token introspect) | Legacy header-based auth per call (`AuthenticationServiceImpl`) |
| All → Reference(21) | REST read + Redis cache (owned keys) + Kafka MasterDataChanged | Legacy `cacheImpl.get("itemMaster")` etc. |
| Search(27) → consumers | REST /search API | Solr queries stay server-side |
| Interop(25) → external | HTTP (HL7 files, ABDM gateway, WhatsApp API, FHIR) | Legacy IntegrationServiceImpl HTTP calls (`:1258-1410`) |
| Domain services → Redis | **never write source-of-truth**; only cache/lookup/short-lived state | Evidence: Redis is read-through cache only (findings §4.2) |
| Cross-service reads (patient, tariff, item) | REST read-model APIs | Breaks the god-node coupling |

---

## 5. Anti-corruption layer & strangler notes

- **FAUG decoupling first**: expose the same endpoints with `Accept: application/json`; migrate frontend screens to SPA progressively; FAUG HTML rendering retires per screen.
- **Strangler pattern**: new services own PG; the monolith keeps Oracle. Interop(25) runs as the "integration broker" early (event source + outbox relay), letting old and new sides exchange events.
- **Shared-table breaks**: every table move = (a) freeze writes from old side, (b) event-forward new writes, (c) backfill, (d) cut reads. T_ENCOUNTER, T_BILL, T_ITEMSTOCK are the critical cutover list.
- **Proc conversion**: convert procs on the extraction path to service code with the same transaction boundaries first, then to PG; keep untouched procs on Oracle until their module migrates.
- **Saga scaffolding**: Choreographed sagas for Order→Bill→Payment with outbox on PG (`pg_logical` → Kafka or Debezium) and compensating flows (bill cancellation already exists: CIMS_SETTLEDBILLCANCELLATION / CIMS_UNSETTLEDBILLCANCELLATION `BillServiceImpl.java:70-91`).

---

## 6. Migration wave draft

> **⚠️ SUPERSEDED — draft retained for traceability only (G-07/C-06 closed 2026-08-28).** Canonical wave plan is **[MIGRATION-WAVES.md](MIGRATION-WAVES.md) §3/§5** and **[HMIS-MODERNIZATION-BLUEPRINT.md](HMIS-MODERNIZATION-BLUEPRINT.md) §16/§20** (W0–W6 gate-based). Do not use the table below for planning — it is stale: Patient W1→W3, Interop W0→W5, Pharmacy W3→W5, Inventory W4→W5, RCM W2→W4, Grievance W5→W2, Transport/Equipment W4→W2, Diet/Linen/BMW W3b→W2 per `DISCOVERY-GAPS.md` G-07.

| Wave | Contexts | Business criticality | Complexity | Coupling | Key exit criteria |
|---|---|---|---|---|---|
| 0 — Foundation | 21 Reference, 22 IAM, 27 Search, 25 Interop | High (everyone depends) | High | Very high | Master/cache contract live; JWT auth; event backbone; FAUG→JSON for first screens |
| 1 — Admission Front | 1 Patient, 2 OPD, 3 ADT, 29 Queue/Kiosk | High | Medium | Medium | PatientRegistered/EncounterCreated/PatientAdmitted streams live; Solr fed by events |
| 2 — Revenue | 4 Clinical, 5 Billing, 6 Payments, 23 RCM | **Highest** | **Very high** | **Very high** | Mega-txn split into saga; InvoiceGenerated/PaymentReceived events live |
| 3 — Clinical Care | 7 Lab, 8 Rad, 9 Pharmacy, 11 OT, 12 Nursing, 13 Blood Bank | High | Med-High | Medium | LabOrderCreated/LabResultFinalized/PrescriptionCreated streams live |
| 3b — Support Clinical | 14 Diet, 15 CSSD, 16 Linen, 19 BMW | Medium | Medium | Low-Med | Independent schemas live |
| 4 — Facilities | 10 Inventory (may move to 2/3 if Pharmacy split forces), 17 Transport, 18 Equipment | Medium-High | High (Inventory) / Low | Medium | Stock API contract live (Pharmacy uses it) |
| 5 — External & Govt | 28 Govt Programs, 24 RMRS/PMRD, 26 Reporting | Low-Med | Medium | Low | PaymentReceived→RMRS stream; report read-models on PG |

Dependencies: Wave 2 ≥ Wave 0+1; Wave 3 ≥ Wave 2 events; Inventory ≥ Pharmacy decision (§6.3); Reporting ≥ everything (read-model only).

> *Table above is the superseded draft — retained verbatim for audit. Use `MIGRATION-WAVES.md` as the single canonical wave table.*

---

## 7. Open questions (continued from findings.md §10)

1. Pharmacy/Inventory stock boundary — keep single stock service (Inventory owns stock) vs per-store ownership? Evidence: Pharmacy writes stock today (`PharmacyServiceImpl.java:2140`).
2. Volume & growth of T_ENCOUNTER/T_BILL → partitioning + retention for PG.
3. Regulatory reporting (GSTR/NHM/RGHS) latency SLA → Reporting service architecture.
4. Audit requirements (T_AUDITEVENT via `ServiceLayerAspect`) — dedicated audit stream?
5. Oracle coexistence duration (strangler window) — affects proc-conversion sequencing.
6. Single unit vs multi-unit tenancy on PG (unitId is on nearly every table) — schema design implication.
