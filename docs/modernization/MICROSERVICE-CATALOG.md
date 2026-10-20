# iHMS / OHUM PAS NG — Microservice Catalog (29 candidate services, PRELIMINARY)

**Status: PRELIMINARY — every service entry is a candidate under validation; pending discovery completion (per `10-target-architecture.md` §1).**
**Sources:** `10-target-architecture.md` §2, `_discovery/agent-10/findings.md` §1/§6, `05-database-catalog.md`, `07-integrations-and-search.md` (Redis keys §2.1, integrations §3.1), `08-security-rbac-audit.md`, `04-api-catalog.md` (3,060 endpoints / 132 service classes; module counts cited), `POSTGRESQL-MIGRATION.md`, `KAFKA-EVENT-CATALOG.md`.
**Legend:** E = produces event, C = consumes event, R = reads API/cache, S = synchronous REST call (per `10 §2.1`).

---

## 0. Cross-cutting facts applied to every service

- **API counts**: endpoint counts below are per **API-catalog module** (`04`), which ≠ service boundaries; per-service re-mapping of the 3,060 endpoints is pending. Where a count maps cleanly to a service it is cited, else `[re-mapping pending]`.
- **Security (AS-IS)**: opaque bearer token (Base64 UUID) in cache; referer allowlist; **backend authorization commented out** — RBAC is frontend-only (2,078 `data-roleM` keys) (`08 §1, §3`). Target: IAM-issued JWT + server-side authorization; all services consume tokens via IAM.
- **Redis (AS-IS)**: read-through cache + short-lived state only, **never source of truth** (`07 §2`, `10 §4`). Checked-in bean is `MemoryCacheImpl` (Redis impls commented) — deployment risk (`07 §2.2`).
- **Solr (AS-IS)**: 6 configured collections, 5 live (patient, encounter, item, icd10cm, healthfactor; `service` orphaned) (`07 §1.2`).
- **Audit**: 59 services write audit events (568 call sites) to `T_AUDIT`/`T_TRX_HISTORY` etc. (`08 §1`) — cross-cutting audit topic candidate (`KAFKA-EVENT-CATALOG §6 Q4`).

---

# 1. Patient Identity & Registration — PRELIMINARY
- **Business capability**: Patient registry, demographics, sponsor, ABHA linkage, family, duplicate detection.
- **Legacy modules**: `PatientServiceImpl` (Solr index :5610), `PatientDetailRegistrationServiceImpl` (:1989), `PatientRegAdmissionServiceImpl`, `PatientRegApptServiceImpl`, `PatientServiceHelper`, `MasterServiceImpl` (registration masters); `CIMS_ADDPATIENTREGISTRATION*`.
- **Owned data**: T_REGISTRATION(+DETAILS) [native-only], T_PATIENTSPONSOR, T_PATIENT_ABDM(+_CC,_SHARETOKEN), T_PATIENT_CONSENT_REQUESTS_ABDM(+_DATA), T_REGISTRATIONQUEUE, T_PATIENT* (~30), T_NA_ITEMDETAILS.
- **PostgreSQL objects**: schema `ihms_patient`; ~40 tables; sequences `<table>_seq`; live procs: CIMS_GETPATIENTGENERALDETAILSLISTFORSEARCHNEW/_STRICT.
- **APIs**: PAS module (998 endpoints, shared with other services) `[re-mapping pending]`; notable: `POST /patient/registerPatient` (MRN rules BR-PAT-001/002/003), patient search, sponsor endpoints, merge.
- **Events**: **E** PatientRegistered (E-01); **C** —.
- **Redis**: `ABHANUM_<healthIdNumber>` → patientId (`PatientServiceImpl.java:6743`); none else.
- **Solr**: **writes** patient collection realtime; **reads** searchPatientInSolr (duplicate detection BR-PAT-002, ABDM matching).
- **External integrations**: Aadhaar/Janaadhaar/biometric (`07 #3`), Bhamashah (`#26`), Mera Aspataal (`#20`), Jeevanvahini push/fetch (`#19`).
- **Security**: PII-heavy (identity numbers, ~100-field Solr doc); phi/base64 columns (POSTGRESQL-MIGRATION §6.4); target: JWT + server-side authorization + field-level PII handling.
- **Dependencies**: IAM (token), Reference (geography/country/state masters R), ABDM via Interop (E), Search (E→C by Search).
- **Migration strategy**: Wave 1 (`10 §6`); T_REGISTRATION DDL must be dumped from Oracle (native-SQL-only); cutover critical (shared tenant record).

# 2. Appointment & Encounter (OPD) — PRELIMINARY
- **Business capability**: Appointment booking, walk-in/emergency visits, encounter lifecycle, doctor queues.
- **Legacy modules**: `AppointmentServiceImpl` (txn :1207,:1596,:2017,:2042,:2186), `AddUpdateAppointmentServiceImpl`, `AppointmentScheduleServiceImpl`, `EmergencyAndWalkinVisitServiceImpl` (encounter :1379, queue :1411, MLC :1463, HL7 A45 :1614, corona :1677, Solr :1796-1931, ABDM queue :1937), `DoctorListServiceImpl`.
- **Owned data**: T_ENCOUNTER, T_APPOINTMENT, T_PATIENTTOKEN, T_DOCTORCHECKIN_QUEUEROOM, doctor queue/cabin tables, T_APPOINTMENT_CHANNEL.
- **PostgreSQL objects**: schema `ihms_encounter`; encounter partitioning open (volume `[UNKNOWN]`, `10 §7 Q2`); VisitStatus SMALLINT+CHECK (06 §A1).
- **APIs**: PAS/Custom module counts `[re-mapping pending]`; notable: `addwalkinVisitNew` (visit-type machine BR-OPD-004, slot rules BR-OPD-002, sponsor default BR-OPD-005), `updateCallPatientData`, `updateDoctorCheckout`, `releaseCabin`.
- **Events**: **E** EncounterCreated (E-02); **C** PatientRegistered (C).
- **Redis**: QMS room queues (`QMSUtil` — shared with service 29); no other.
- **Solr**: **writes** encounter collection realtime (3-day rolling window — retention risk `07 §1.6`); reads via patient search.
- **External integrations**: e-Mitra kiosk (sync, `07 #4`), Jeevanvahini emergency fetch.
- **Security**: token; unit-scoped access.
- **Dependencies**: Patient (S/R), IAM, Interop (E HL7/ABDM), Search (E), Queue (S queue state).
- **Migration strategy**: Wave 1; T_ENCOUNTER cutover-critical (52 refs, `05 §7`).

# 3. ADT & Bed — PRELIMINARY
- **Business capability**: Admission, transfer, discharge, bed/ward state, MLC, absentee tracking.
- **Legacy modules**: `IPDAdmissionServiceImpl` (CIMS_ADDADMISSIONINFORMATION :130, CIMS_UPDATEBEDOCCUPIEDFLAG_1 :313), `IPDServiceImpl`, `IPDHelper`, `BedServiceImpl`, `BedReservationServiceImpl` (txn :60,:212; Solr+HL7 :165-180), `BedManagementServiceImpl`, `PatientTransferDischargeServiceImpl`, `CancelAdmissionServiceImpl`.
- **Owned data**: T_ADMISSION (`t_admission` native), T_BEDRESERVATION, T_TRANSFERREQUEST (`t_transfer`), T_PATIENTDISCHARGEDATA, T_MLCDETAILS, T_ATTENDANTPASS, T_ADMISSIONDETAILS (proc-side); **M_BEDMASTER/M_WARDMASTER recommended to move here** (ownership conflict, POSTGRESQL-MIGRATION §1.1).
- **PostgreSQL objects**: schema `ihms_adt`; bed-state CAS on OCCUPIED (BR-IPD-002), BedStatusId 0/1/3/5/6 semantics (06 §A5).
- **APIs**: PAS `[re-mapping pending]`; notable: admission (preconditions BR-IPD-001/003), discharge (BR-DIS-001/002/003), bed allocation (BR-IPD-002), interim-abscond (BR-IPD-005).
- **Events**: **E** PatientAdmitted (E-03), PatientDischarged (E-04), BedChanged (E-11, PRELIMINARY); **C** EncounterCreated (C).
- **Redis**: `CDD_<opdipdid>` discharge lock (60s, aspect); bed availability cache (short-lived).
- **Solr**: **writes** encounter index (admission/discharge variants, death handling).
- **External integrations**: Chiranjeevi (INSURANCESCHEMEID=2, `07 #6`), Jeevanvahini (emergency).
- **Security**: token; ward-scope grants via IAM.
- **Dependencies**: Patient (S), Encounter (S/C), IAM, Billing (E bed charges), Interop (E HL7 A01), Search (E).
- **Migration strategy**: Wave 1; bed-state + admission procs port to PL/pgSQL early (live procs CIMS_ADDADMISSIONINFORMATION, CIMS_UPDATEBEDOCCUPIEDFLAG_1).

# 4. Ordering & Prescription (Clinical) — PRELIMINARY
- **Business capability**: Clinical orders, prescriptions, notes, EMR details, order sets.
- **Legacy modules**: `AddServiceOrderServiceImpl` + `AddServiceOrderServiceHelper` (**mega-txn** :341-516, :2206-2399), `ClinicalServiceImpl`, `NoteDetailsServiceImpl`, `PrescriptionMasterServiceImpl`/`PrescriptionDetailsServiceImpl`, `MedicationServiceImpl` (VisitStatus 12 :799-821), `InvestigationServiceImpl`.
- **Owned data**: T_PATIENTADVICE, T_PATIENTEORDER, T_PATIENTMEDICATION, T_PATIENTNOTE, T_EMRDETAILS, T_EMRSERVICES, T_CONSULTORDER, T_PRESCRIPTIONNOTES.
- **PostgreSQL objects**: schema `ihms_clinical`; med status enums SMALLINT+CHECK (06 §A9).
- **APIs**: Custom module 1204 endpoints `[re-mapping pending]`; notable: `/serviceOrder/*` (~60 ordering endpoints incl. `addServicechargesNew`, `saveCreditNote`, `includeCharge`), `/clinical/*` (4 endpoints).
- **Events**: **E** PrescriptionCreated (E-06); **C** PatientRegistered, EncounterCreated, PatientAdmitted; (post-saga) OrderCreated deferred (KAFKA-EVENT-CATALOG §3).
- **Redis**: none (cache via Reference).
- **Solr**: reads (patient search); writes none.
- **External integrations**: none direct (HL7 ORM via Interop).
- **Security**: clinical role gating (frontend-only today).
- **Dependencies**: Patient (R), Encounter/Admission (R), Reference (R tariff), Billing (S today — the mega-txn; **must become saga**), Interop (E HL7), Lab/Rad (E post-saga).
- **Migration strategy**: Wave 2; **the decomposition bet** — mega-txn split is prerequisite for everything revenue.

# 5. Billing & Settlement — PRELIMINARY
- **Business capability**: Bill header/lines, charges, doctor bills & shares, concessions, credit notes, estimation.
- **Legacy modules**: `BillServiceImpl` (10 @Transactional :66-579), `BillDetServiceImpl`, `BillReceiptServiceImpl`, `BillSummaryServiceImpl`, `ChargeServiceImpl`, `ConcessionTemplateServiceImpl`, `PatientBillEstimationServiceImpl`, `DoctorShare` accrual in helper (:417).
- **Owned data**: T_BILL, T_BILLDETAILS, T_CHARGE, T_CHARGES_TEMP, T_DOCTORBILL, T_DOCTORSHARE, T_CONCESSION, T_CREDITNOTE(+Details, case-mixed), T_FREQ_ITEMS.
- **PostgreSQL objects**: schema `ihms_billing`; money NUMERIC(19,2); BillNo formatted string; charge `*BeforeFinalization` snapshot cols; numbering via auto-code sequence (BR-BILL-001).
- **APIs**: PAS/Custom `[re-mapping pending]`; notable: `addServicechargesNew`, `/bill/*`, estimation, concession (BR-BILL-005), settlement (BR-BILL-006), cancellation restore (BR-BILL-008).
- **Events**: **E** InvoiceGenerated (E-07); **C** PrescriptionCreated (C), BedChanged (C), PaymentReceived (C), post-saga LabOrderCreated/RadOrderCreated (deferred).
- **Redis**: tariff/rate lookups (owned keys by Reference).
- **Solr**: reads (bill/encounter search via API); no direct writes.
- **External integrations**: none direct.
- **Security**: cashier roles (frontend); target server-side.
- **Dependencies**: Reference (S tariff), Payments (E/C), Clinical (S today), IAM, Search (E bill docs).
- **Migration strategy**: Wave 2; bill settlement stays **synchronous REST** until saga/outbox (KAFKA-EVENT-CATALOG §3).

# 6. Payments & Refunds — PRELIMINARY
- **Business capability**: Payment capture, modes, advances, allocation, refunds, receipts, cancellations.
- **Legacy modules**: `PaymentServiceImpl`, `AddPaymentServiceImpl`/`AddPaymentServiceHelper` (CIMS_ADDPAYMENT :291 → :309 → :333 → :348 → CIMS_UPDATEBILLFORCONCESSION :463), `AddAdvanceServiceImpl` (CIMS_ADDADVANCE :103-178), `RefundServiceImpl`, `AddRefundServiceImpl`, `RefundBillReceiptServiceImpl`, `BillCancellationServiceImpl`, `AllReceiptsServiceImpl`, `AdvanceBillReceiptServiceImpl`.
- **Owned data**: T_PAYMENT, T_PAYMENTMODEDETAILS, T_ADVANCE, T_REFUND, T_RECEIPT.
- **PostgreSQL objects**: schema `ihms_payment`; TransactionType 2/3/4 SMALLINT+CHECK (06 §A6); advance FIFO logic (BR-BILL-007).
- **APIs**: module Payments 6 endpoints + Custom addPayment/addAdvance/addRefund `[re-mapping pending]`; notable: `addBillPaymentNew`, `addAdvance`, `refund`, bill cancellation.
- **Events**: **E** PaymentReceived (E-08), RefundIssued (E-12 PRELIMINARY); **C** InvoiceGenerated (C).
- **Redis**: upload-attempt dedup counters + session/token (`BillReceiptServiceImpl.java:3137-3145`), short-lived.
- **Solr**: none.
- **External integrations**: none direct (receipt printing via print server `07 #17`).
- **Security**: cashier; token.
- **Dependencies**: Billing (S/E settlement update), RMRS (E), RCM (E/C), IAM.
- **Migration strategy**: Wave 2; payment procs port (CIMS_ADDPAYMENT chain — live).

# 7. Lab (Pathology) — PRELIMINARY
- **Business capability**: Lab order booking, specimen, parameters, results, machine interface, reports.
- **Legacy modules**: `PathoOrderServiceImpl` (14,629 lines; CIMS_ADDPATHORDERBOOKING), `PathoOrderServiceHelper` (WhatsApp report :1130-1208), `PatientRequisitionServiceImpl`.
- **Owned data**: T_PATHORDERBOOKING(+DETAIL), T_PATHOPATIENTPARAMETERDETAILS, T_MACHINECONFIGURATION, T_PATIENTREQUISITION.
- **PostgreSQL objects**: schema `ihms_lab`; sample/parameter numerics; IsOutSourced boolean; patho masters stay in Reference (API-borrowed).
- **APIs**: PAS `[re-mapping pending]`; notable: lab order booking, result entry, report endpoints (CIMS_rpt_PathoResultEntry_new1 etc.), machine config.
- **Events**: **E** LabResultFinalized (E-05); **C** (post-saga) OrderCreated; PatientRegistered/EncounterCreated (R).
- **Redis**: none.
- **Solr**: reads (patient search for requisition).
- **External integrations**: eUpkaran (`07 #26`), EXT LIMS staging (disabled, `#28`), Backman OLIS instrument (jar, `#18`).
- **Security**: clinical role; machine-interface auth `[UNKNOWN]`.
- **Dependencies**: Patient (R), Reference (R), Interop (E WhatsApp/EHR), Billing (E post-saga).
- **Migration strategy**: Wave 3.

# 8. Radiology — PRELIMINARY
- **Business capability**: Rad order booking, modality scheduling, templates, results.
- **Legacy modules**: `RadiologyServiceImpl`, rad booking inside `AddServiceOrderServiceHelper.java:579-599` (CIMS_ADDRADORDERBOOKING).
- **Owned data**: T_RADORDERBOOKING(+DETAILS), rad masters (borrowed).
- **PostgreSQL objects**: schema `ihms_rad`; IsImageCaptured flag.
- **APIs**: PAS `[re-mapping pending]`.
- **Events**: **E** (deferred) RadOrderCreated; **C** OrderCreated (post-saga).
- **Redis / Solr**: none / patient search (R).
- **External integrations**: none direct.
- **Security**: clinical role.
- **Dependencies**: Patient (R), Reference (R), Billing (E post-saga).
- **Migration strategy**: Wave 3; smaller than Lab.

# 9. Pharmacy & Dispensing — PRELIMINARY
- **Business capability**: Prescription dispense, compound, ward picklist, drug intervention, EMR number.
- **Legacy modules**: `PharmacyServiceImpl` (3,930 lines; CIMS_ADDPHARMACYINTERVENTION :660, **CIMS_ADDITEMSTOCK :2140 — dual-write hotspot**, CIMS_ADDWARDPICKLIST :2383, CIMS_SETEMRNO :886), `DrugServiceImpl`, `ItemSalesServiceImpl`, `MaterialConsumptionServiceImpl`.
- **Owned data**: T_PRESCRIPTIONMASTER, T_ITEMSALE, T_PRESCRIPTIONDISPENSEDETAILS, T_ITEMCOMPOUNDDETAILS, ward picklist; **stock tables stay in Inventory (10) — API-borrowed**.
- **PostgreSQL objects**: schema `ihms_pharmacy`; DISPENSED booleans; prescription closure semantics (BR-PHAR-002).
- **APIs**: PAS `[re-mapping pending]`; notable: dispense (VisitStatus 12 — BR-PHAR-001), interventions, picklist, setEMRNo.
- **Events**: **E** DispensingCompleted (proposed — no multi-consumer evidence yet → not in catalog); **C** PrescriptionCreated (C), InventoryUpdated (C).
- **Redis**: stock snapshot lookup only (`10 §2.2`).
- **Solr**: reads item search (`searchAllItemsInSolr` :1173).
- **External integrations**: none direct.
- **Security**: pharmacy roles.
- **Dependencies**: Inventory (S stock ops — **boundary decision pending, `10 §7 Q1`**), Clinical (C), Billing (E).
- **Migration strategy**: Wave 3; Inventory boundary decision may pull it earlier (`10 §6`).

# 10. Inventory & Procurement — PRELIMINARY
- **Business capability**: Item master, stock, batch, PO/GRN, indent, supplier, rate contract, EAushadhi.
- **Legacy modules**: `InventoryServiceImpl` (12,941 lines, god node 250 edges), `PurchaseOrderServiceImpl`, `QuotationServiceImpl`, `OpeningBalanceServiceImpl`, `ExpiryItemServiceImpl`, `ItemStockServiceImpl`, `ItemMasterServiceImpl`, `ItemCategoryServiceImpl`, `UploadFileServiceImpl` (294 edges, bulk uploads), `InventoryEAushadiIntegrationImpl`.
- **Owned data**: M_ITEMMASTER, T_ITEMSTOCK, T_ITEMBATCHMASTER, T_PURCHASEORDER, T_GRN*, T_INDENT, T_SUPPLIER, T_RATECONTRACT, T_QUOTATION, T_CONSIGNMENTGRN, T_ITEMCLINIC, T_OPENINGBALANCE, T_POISSUE* temp tables.
- **PostgreSQL objects**: schema `ihms_inventory`; stock row-lock pattern (SELECT…FOR UPDATE — `10 §3.4`); itemSolr status flag; reorder logic (`BR`-free, job-driven `ApplicationJobs:2197,2376`).
- **APIs**: PAS `[re-mapping pending]`; notable: stock ops, GRN/indent, EAushadhi sync endpoints, bulk item upload (`UploadFileServiceImpl.java:462`).
- **Events**: **E** InventoryUpdated (E-09), PurchaseOrderPlaced/GRNPosted (proposed, no consumers evidence yet); **C** DispensingCompleted (C), MaterialConsumed (C), auto-indent triggers.
- **Redis**: itemMaster cache reads (Reference-owned keys).
- **Solr**: **writes** item collection via 5s drainer `syncItemsToSolrJob` (SOLR_STATUS=0).
- **External integrations**: e-Aushadhi RMS (Basic-auth, `07 #8`), Kaysons wholesaler (`#24`).
- **Security**: `Inventory.AllApproval` permission check — **only 1 active server-side check** (`MasterCachedServiceImpl:10356-10362`, used at `InventoryServiceImpl:11588`).
- **Dependencies**: Reference (R item/company/tax), Pharmacy (S via stock API), OT/CSSD (C), IAM.
- **Migration strategy**: Wave 4 (or 2 if Pharmacy split forces — `10 §6`); god-node risk.

# 11. OT & Surgery — PRELIMINARY
- **Business capability**: OT scheduling, procedure notes, anesthesiology, surgery details, pre-billing.
- **Legacy modules**: `OTDetailsServiceImpl`, `OTSchedulerServiceImpl` (Solr search :4879), `OtConfigServiceImpl`, `SurgeryDetailsServiceImpl`, `PctsServiceImpl`.
- **Owned data**: T_OTSCHEDULE(+DETAILS), T_SURGERYDETAILS, procedure masters (borrowed), T_ANESTHESIANOTES.
- **PostgreSQL objects**: schema `ihms_ot`; dependent-surgery discount (BR-OT-001), pre-billing flag (BR-OT-002).
- **APIs**: PAS `[re-mapping pending]`; notable: schedule, procedures, pre-billing charges.
- **Events**: **E** ProcedureScheduled/SurgeryCompleted (proposed — CSSD/Inventory consume; weak evidence → not in catalog yet); **C** OrderCreated (procedure), PatientAdmitted (C).
- **Redis / Solr**: none / patient search (R).
- **External integrations**: PCTS (`07 #23`, maternal tracking).
- **Security**: clinical role.
- **Dependencies**: Patient (R), Inventory (E consumption), Billing (E), CSSD (E).
- **Migration strategy**: Wave 3.

# 12. Nursing & Inpatient Care — PRELIMINARY
- **Business capability**: Nursing notes, vitals, task planning, I/O charting, station roster.
- **Legacy modules**: `NursingServiceImpl` (349 edges, ROWNUM :7124), `NursePatientDetails`.
- **Owned data**: T_NURSENOTE, T_PATIENTVITALS, T_NURSETASKDETAILS, T_INTAKEOUTPUT, T_NURSEPATIENTDETAILS, nursing station masters, IpdRoster.
- **PostgreSQL objects**: schema `ihms_nursing`; task frequency semantics (06 §A).
- **APIs**: **Nursing module 300 endpoints** (highest single-module count after PAS/Custom).
- **Events**: **E** VitalsRecorded/NurseTaskCompleted (proposed, no consumers); **C** PatientAdmitted, PatientDischarged, OrderCreated.
- **Redis / Solr**: none / none.
- **External integrations**: none direct.
- **Security**: nurse roles (frontend role-name checks).
- **Dependencies**: Encounter/Admission (R).
- **Migration strategy**: Wave 3.

# 13. Blood Bank — PRELIMINARY
- **Business capability**: Donor registry, blood bag/component lifecycle, cross-match, issue, eRaktkosh.
- **Legacy modules**: `BloodBankServiceImpl` (~50 @Transactional; SYSDATE :6214, EXTRACT :7367), eRaktkosh jobs (`ApplicationJobs.java:1205,1383`).
- **Owned data**: T_BLOODBAG, T_BLOODCOMPONENT, T_BLOODREQUISITION, T_BLOODDONOR, T_BLOODDONATIONCAMP, donor masters (21 tables).
- **PostgreSQL objects**: schema `ihms_bloodbank`; requisition status 1/2/3/10 SMALLINT (BR-BLD-001), decross 2880-min timer (BR-BLD-002), expiry (BR-BLD-003/004), donor DNR enum collisions documented (06 §A8).
- **APIs**: PAS `[re-mapping pending]`; notable: requisition, cross-match, issue, donor registration.
- **Events**: **E** BloodRequisitionCreated (deferred — KAFKA-EVENT-CATALOG §3); **C** PatientAdmitted (C), LabResultFinalized (C serology).
- **Redis / Solr**: none / encounter link writes (BloodBankServiceImpl:1429-1625).
- **External integrations**: eRaktkosh (`07 #21`).
- **Security**: blood-bank role.
- **Dependencies**: Patient (R), Inventory (C products), Interop (E eRaktkosh via event).
- **Migration strategy**: Wave 3b.

# 14. Diet — PRELIMINARY
- **Business capability**: Diet orders, item scheduling, kitchen menu, intake tracking.
- **Legacy modules**: `DietServiceImpl` (SYSDATE/to_char :154-161).
- **Owned data**: T_DIETORDER, T_DIETITEMSCHEDULEDETAILS, T_TODAYDIETITEMMENU, diet/food masters.
- **PostgreSQL objects**: schema `ihms_diet`; date-only menu dates → DATE type.
- **APIs**: PAS `[re-mapping pending]` (~4).
- **Events**: **E** DietOrderCreated (single-consumer; not in catalog); **C** PatientAdmitted (C).
- **Redis / Solr / Integrations**: none.
- **Dependencies**: Admission (R).
- **Migration strategy**: Wave 3b.

# 15. CSSD — PRELIMINARY
- **Business capability**: Kit formation, sterilization cycles, issue to OT/procedure.
- **Legacy modules**: `CSSDServiceImpl`, `ProcedureCssdRequest`.
- **Owned data**: T_CSSDKITFORMATION, T_CSSDKITISSUEITEM, T_STERILIZATION*, T_PROCEDURECSSD*.
- **PostgreSQL objects**: schema `ihms_cssd`.
- **APIs**: **CSSD module 46 endpoints**.
- **Events**: **E** KitIssued/SterilizationCompleted (rejected phase 1 — no cross-service consumer evidence); **C** ProcedureScheduled (C), OrderCreated (procedure) (C), InventoryUpdated (C).
- **Redis / Solr / Integrations**: none.
- **Dependencies**: Inventory (C), OT (R).
- **Migration strategy**: Wave 3b.

# 16. Linen & Laundry — PRELIMINARY
- **Business capability**: Linen category/detail, ward requests.
- **Legacy modules**: `LinenLaundryServiceImpl` (32 getQ).
- **Owned data**: T_LINENREQUEST(+DETAILS), T_LINENCATEGORY, T_LINENDETAIL.
- **PostgreSQL objects**: schema `ihms_linen`.
- **APIs**: **Linen & Laundry module 14 endpoints**.
- **Events**: none accepted; **C** PatientAdmitted.
- **Dependencies**: Ward (R).
- **Migration strategy**: Wave 3b.

# 17. Transport — PRELIMINARY
- **Business capability**: Ambulance, vehicle, driver, transport requests, maintenance.
- **Legacy modules**: `TransportServiceImpl`.
- **Owned data**: T_TRANSPORTREQUEST(+DETAILS), T_VEHICLEMAINTENANCE, M_AMBULANCEMASTER, M_DRIVERMASTER.
- **PostgreSQL objects**: schema `ihms_transport`.
- **APIs**: PAS `[re-mapping pending]`.
- **Events**: none accepted.
- **Dependencies**: Patient/Admission (R).
- **Migration strategy**: Wave 4.

# 18. Equipment Management — PRELIMINARY
- **Business capability**: Equipment registry, complaint/resolution, PM schedules, warranty/AMC/insurance.
- **Legacy modules**: `EquipmentServiceImpl`, `PreventiveMaintenanceServiceImpl`, `PreventiveMaintenanceScheduleServiceImpl`, `EquipmentServiceHelper`.
- **Owned data**: T_EQUIPMENT, T_EQPCOMPLAINT(+RESOLUTION), T_EQPPM*, warranty/insurance/AMC tables.
- **PostgreSQL objects**: schema `ihms_equipment`; CLOB docs → TEXT (12 @Lob entities incl. Equipmentdetail/Equipinsurancedetails — `agent-10 §5 row 4`).
- **APIs**: PAS `[re-mapping pending]`.
- **Events**: none accepted.
- **Dependencies**: Inventory (C spares), Finance (AMC cost, R).
- **Migration strategy**: Wave 4.

# 19. Bio-Medical Waste — PRELIMINARY
- **Business capability**: Waste segregation, bag tracking, disposal.
- **Legacy modules**: `BioMedicalWasteManagementServiceImpl` (FETCH FIRST :347).
- **Owned data**: T_BIOMEDICALWASTE(+DETAILS), T_WASTEBAGMASTER.
- **PostgreSQL objects**: schema `ihms_bmw`.
- **APIs**: **Biomedical Waste module 14 endpoints**.
- **Events**: none accepted.
- **Dependencies**: Ward (R).
- **Migration strategy**: Wave 3b.

# 20. Grievance & Feedback — PRELIMINARY
- **Business capability**: Complaints, resolutions, feedback questionnaires.
- **Legacy modules**: `ComplaintDetailsServiceImpl` (txn :221,:245,:365,:532), `ComplaintResolutionDetailServiceImpl`, `QuetionnaireServiceImpl` (VisitStatus 4/6 side-effects!), `PatientFeedback`.
- **Owned data**: T_PATIENTCOMPLAINTS, T_PATIENTGRIEVANCE*, T_COMPLAINTMASTER, T_FEEDBACKTEMPLATE.
- **PostgreSQL objects**: schema `ihms_grievance`.
- **APIs**: PAS `[re-mapping pending]`.
- **Events**: **C** PatientDischarged (satisfaction trigger).
- **Dependencies**: Patient (R).
- **Migration strategy**: Wave 5 (low).

# 21. Reference Master Data — PRELIMINARY
- **Business capability**: All M_* masters; **cache-key ownership**; tariffs/packages/plans; MDM.
- **Legacy modules**: `MasterServiceImpl`, `MasterCachedServiceImpl` (**god node, 514 edges**; checkService :6411), `MasterCachedServiceHelper`, `MdmServiceImpl`, `AddServiceMasterImpl` (CIMS_ADDSERVICEMASTER2 :207, CIMS_ADDTARIFFSERVICE2 :231), `ServiceMasterServiceImpl`, `TariffServiceImpl`, `PlanMasterServiceImpl`, `PackageServiceServiceImpl`, `CompanyApprovedAmountServiceImpl`, `RefEntityServiceImpl`, `ItemMasterServiceImpl`.
- **Owned data**: M_* masters (~582, **minus ownership-conflict tables** — POSTGRESQL-MIGRATION §1.1: M_BEDMASTER→ADT, M_ITEMMASTER→Inventory, M_STAFF/DOCTORMASTER→IAM), CONFIG_APPLICATION, block/city/district_mst, M_MRNSERIES, numbering sequences (`auto_code_seq`).
- **PostgreSQL objects**: schema `ihms_master`; ~580 tables; 17 functions (incl. auto-code counter `cims_getlatestautocodeandincremetitbyone` used by MRN/OPDNo/IPDNo/BillNo — BR-PAT-001, BR-OPD-003, BR-IPD-004, BR-BILL-001).
- **APIs**: PAS masters + Custom addServiceMaster (6) `[re-mapping pending]`; notable: master CRUD via generic `@CrudApi`, `/masters/*`, checkService (cache health).
- **Events**: **E** MasterDataChanged incl. TariffChanged/PackageChanged (E-10); **C** —.
- **Redis**: **owns all master cache keys** (~375 `@CrudCached` masters: genderMaster, itemMaster, tariffMaster, unitMaster, classMasterMap_*, specializationMaster…; written via `CrudOperationsFilter.java:139-330`).
- **Solr**: content owner for item/icd10cm/healthfactor collections (drainers `syncItemMaster`, `syncIcd10cmMaster`, `syncHealthFactorToSolr` — `07 §1.3`); collection infra owned by Search.
- **External integrations**: none direct (bulk uploads via UploadFile).
- **Security**: admin roles; `checkService` cache endpoint.
- **Dependencies**: IAM (admin), Upload (bulk), Search (E), all services (R).
- **Migration strategy**: **Wave 0** — cache contract + invalidation events must land before any split (`10 §6`).

# 22. Security & IAM — PRELIMINARY
- **Business capability**: AuthN/AuthZ, LDAP, users, roles, per-unit/per-store/per-ward grants, staff/doctor profiles.
- **Legacy modules**: `AuthenticationServiceImpl` (login/LDAP :66-293), `UserManagementServiceImpl`, `AddUserServiceImpl` (user+staff+doctor :789-1070), `Encrypter`, `SSOAuthFilter`, `IntegrationOperationsFilter`, `DefaultAuthenticationImpl` (:193-319).
- **Owned data**: T_USERDETAILS, T_USERROLES, T_USERROLEMASTER, T_USERUNITDETAILS, T_USERSTOREDETAILS, T_USERWARDDETAILS, M_MENUMASTER, M_MODULEPERMISSIONMASTER, t_role_enabledpermission, M_STAFFMASTER/M_DOCTORMASTER (profiles, conflict §1.1), T_USER_FACEAUTH? (decision), user_detail_temp, T_LOGIN_AUDIT? (decision).
- **PostgreSQL objects**: schema `ihms_iam`; ~12 tables; login joins (DefaultAuthenticationImpl:225-230); session tokens in Redis (not PG).
- **APIs**: **Authentication module 12 endpoints** (`/auth/loginf`, `loginotp`, `loginJ`, `logout`…) + AddUserService (~50 in Custom) + usermanagement; **57 ABDM/HFR callbacks are NOT IAM** (Interop).
- **Events**: **E** UserCreated/RoleChanged (proposed, no multi-consumer evidence); **C** —.
- **Redis**: session/token cache (`<token>`→UserView, `UNQ_SESS_USER_<id>`), 30-min TTL, single-session (08 §2.4).
- **Solr**: none.
- **External integrations**: Rajasthan SSO (`07 #16`), LDAP/AD (inactive — `ldap.auth.enabled=false`, 08 §2.2), OTP SMS.
- **Security**: **the service being built** — current backend enforces only token validity (08 §3.1); target JWT + server-side authorization + secret remediation (hard-coded keys — `07 §3.3`).
- **Dependencies**: LDAP (external); all services depend on IAM.
- **Migration strategy**: **Wave 0**.

# 23. RCM & Claims — PRELIMINARY
- **Business capability**: Eligibility, pre-auth, claims (NHCX/HCX), TID tracking.
- **Legacy modules**: `RcmServiceImpl`, `NHCXServiceImpl` (`services/abdm/NHCXServiceImpl.java:30`), models/rcm+hcx.
- **Owned data**: T_ELIGIBILITYCHECK, T_ADMISSION_TID (a.k.a. T_ADMISSIONTIDDETAILS — naming variant), T_NHCX_REQUEST(+_DETAILS).
- **PostgreSQL objects**: schema `ihms_rcm`; NHCX status 0-3 enums (06 §A9).
- **APIs**: **RCM module 7 endpoints** + nhcx endpoints (ABDM module) `[re-mapping pending]`; notable: `coverageEligiblityRequest`, `claimSubmitRequest`, `preAuthSubmit`.
- **Events**: **E** ClaimSubmitted (deferred — KAFKA-EVENT-CATALOG §3); **C** PatientAdmitted, InvoiceGenerated, PaymentReceived.
- **Redis**: ABDM/HCX keys via Interop.
- **External integrations**: HCX via ABDM gateway (JWE, `07 #2`).
- **Dependencies**: Billing (R), Payment (C), Interop (E to NHCX).
- **Migration strategy**: Wave 2 (revenue).

# 24. RMRS & PMRD (Treasury) — PRELIMINARY
- **Business capability**: Treasury head/authority mapping, collections, receipts for govt accounts.
- **Legacy modules**: `RMRSServiceImpl` (ROWNUM=1 :1361-1366), `PmrdServiceImpl`.
- **Owned data**: T_RMRSCOLLECTION, T_AUTHORITYMASTER, T_HEADSMASTER, T_LEDGERMASTER, T_ACCOUNTING* (14), s_servicewisecollection, s_userwisecollection.
- **PostgreSQL objects**: schema `ihms_rmrs`; money NUMERIC(19,2); accounting-ledger integrity.
- **APIs**: PAS `[re-mapping pending]` (~6).
- **Events**: **C** PaymentReceived (E-08); **E** CollectionPosted (proposed, no consumers).
- **Dependencies**: Payment (C).
- **Migration strategy**: Wave 5.

# 25. Interop & Notifications — PRELIMINARY
- **Business capability**: HL7 out/in, EHR file gen, ABDM/ABHA, HPR, WhatsApp/SMS/email, webhook callbacks, FHIR.
- **Legacy modules**: `EHRUtilServiceImpl` (:91-1205), `EHRSyncJobs`, `Hl7InboundMessageJob` (:40), `HL7Helper` (A01/A45/ORM, seq from DUAL :246), `AbhaServiceImpl`, `HPRServiceImpl`, `ABDMController` (40+ endpoints + callbacks), `HFRController`, `IntegrationServiceImpl` (WhatsApp :1258-1410, status callback :1462), `SendNotificationServiceImpl` (36 getQ; consent :3203-3294), `CommonABDMUtil` (Redis correlation :350-591), `IntegrationHelper`.
- **Owned data**: T_OUTBOUNDHIS, T_INBOUNDHIS, T_ABDMREQUESTQUEUE, T_WHATSAPPMESSAGEQUEUE/STATUS, T_EHRJOBDETAILS, T_ABDM_AUDIT.
- **PostgreSQL objects**: schema `ihms_interop`; **queue tables become read-model during strangler then drop** (KAFKA-EVENT-CATALOG §5); keep PG HL7 outbox with idempotent senders (`10 §7.2`).
- **APIs**: **Integrations module 22 endpoints** + **ABDM module 73** + **ABDM callbacks 57** + **EHR 8** + **Notifications 5** ≈ 165 endpoints.
- **Events**: **C** all domain events (PatientRegistered, EncounterCreated, PatientAdmitted, PatientDischarged, LabResultFinalized, InvoiceGenerated, PaymentReceived…); **E** none (external outbound calls, not topics).
- **Redis**: ABDM correlation store (`ABDM_REQ_/ABDM_RES_`, `ABDM_SES_TKN`, `HIP/HIU_ABDM_KEYS`), per-node counters — becomes consumer-group state (KAFKA-EVENT-CATALOG §5.1).
- **Solr**: none.
- **External integrations**: **ABDM (HIP/HIU/CM/HFR/HPR/PHR/ABHA)** `07 #1`, HCX `#2`, HL7 v2 `#10`, embedded FHIR `#11`, EHR/EWD `#9`, SMS/WhatsApp eSanchar `#15`, TIUXML `#12`, print server `#17`, scan app `#25`, SSO (inbound) `#16`.
- **Security**: **ROLE_INTEGRATION** gating for `/int/*` (the only real backend role check — 08 §2.2); JWE/JWS key material; hard-coded secrets to remediate.
- **Dependencies**: all domain services (C), external endpoints.
- **Migration strategy**: **Wave 0** — runs as integration broker/event relay during strangler (`10 §5`).

# 26. Reporting & Analytics — PRELIMINARY
- **Business capability**: On-demand reports (Jasper/Velocity), dashboards, stats counts, regulatory exports.
- **Legacy modules**: `ReportServiceImpl`, `FileManagementServiceImpl` (27 getQ), `BillInvoiceGenerator` (JasperReports :19-25), Velocity templates; `ApplicationJobs` (updateIhmsSummaryCounts, addStats_Archived :2556-2943).
- **Owned data**: T_REPORTMASTER, stats/archive tables, PDF blob tables (`savePdfToTable` `CommonUtilServiceImpl.java:6067`).
- **PostgreSQL objects**: schema `ihms_reporting`; read-model tables populated by ELT/event consumption; CLOB/TEXT PDFs.
- **APIs**: **File Management module 24 endpoints** + ~60 `xxxReport` endpoints across modules + dashboard counts (`CommonUtilServiceImpl.java:4272-4302`).
- **Events**: **C** all domain events (read-model); **E** ReportGenerated (rejected — KAFKA-EVENT-CATALOG §3).
- **Redis**: dashboard caches (short-lived).
- **Solr**: reads (patient/encounter dashboards).
- **External integrations**: NMC (`07 #22`), PCTS (`#23`), RGHS/SIMS (`#7`), EXT LIMS staging (`#28`).
- **Security**: BIReport permission keys (263 data-roleM).
- **Dependencies**: All (read-model); regulatory report SLAs open (`10 §7 Q3`).
- **Migration strategy**: Wave 5.

# 27. Search & Index (Solr pipeline) — PRELIMINARY
- **Business capability**: Solr ingestion pipeline + search API.
- **Legacy modules**: `SolrUtil` (index* :174-333, search* 15 methods, sync* drainers :899,:965,:1579,:2032), sync jobs (`ApplicationJobs.java:867,884,976,2122`), `PatientSolrView`/`EncounterSolrView`/`ItemSolrView`/`Icd10cmSolrView`/`HealthFactorSolrView` DTOs.
- **Owned data**: **no PG tables** — owns 6 Solr collections (5 live; `service` orphaned — drop, `07 §1.2`).
- **PostgreSQL objects**: none (index state in Solr; `SOLR_STATUS` flags stay on source tables during strangler).
- **APIs**: exposes `/search/*` to consumers (new); legacy search is embedded in each service (`07 §1.4`).
- **Events**: **C** PatientRegistered, EncounterCreated, PatientAdmitted, PatientDischarged, LabResultFinalized, InventoryUpdated, MasterDataChanged.
- **Redis**: `current_pat_*`/`current_enc_*` mirrors (100s TTL — hides Solr soft-commit lag; promote or drop, `07 §2.3`).
- **Solr**: **the service IS the Solr pipeline**; edismax boosts load-bearing (`searchPatientInSolr` bq weights — `07 §1.7`).
- **External integrations**: none.
- **Security**: search API auth via IAM token.
- **Dependencies**: domain services (C), SolrCloud 7.5.0 (target 9.x, `07 §1.7`).
- **Migration strategy**: **Wave 0**; **Solr must remain** (`07 §1` — non-negotiable).

# 28. Govt Programs (EJS / Aasmaan / Chiranjeevi) — PRELIMINARY
- **Business capability**: ASHA/ANC-PNC/child program + incentives; AASMAAN case management; Chiranjeevi enrolment.
- **Legacy modules**: `EjsDetailsServiceImpl` (:2105), `EjsIncentiveServiceImpl` (ASHA incentive job `ApplicationJobs.java:1035`), `EjsAncPncChildServiceImpl`, `EjsAshaServiceImpl`, `EjsSupervisorServiceImpl`, `AasmaanCaseServiceImpl` (+9 Aasmaan services), Chiranjeevi (BeneficiaryEnrolment, RefrenceTid).
- **Owned data**: T_EJS_* (ANDROID_*, ES_*, MASTER_*, MEDIA*, USER_*, PROFILE, INDICATORS — 57 entities, BigInteger ids), T_AASMAAN_* (35, convention-named), T_BENEFICIARY_ENROLMENT_IHMS, T_REFRENCETID.
- **PostgreSQL objects**: schema `ihms_govt`; EJS ids NUMERIC(38) or BIGINT; **Aasmaan table names must be verified in Oracle** (POSTGRESQL-MIGRATION §4).
- **APIs**: **EJS module 125 endpoints** + **AASMAAN module 78 endpoints** ≈ 203.
- **Events**: **E** IncentiveComputed (deferred); CaseUpdated (proposed); **C** PatientRegistered.
- **Redis / Solr**: none / outreach registration writes patient index (`EjsDetailsServiceImpl.java:2105`).
- **External integrations**: PCTS (maternal tracking, `07 #23`), Chiranjeevi Yojana (`#6`).
- **Security**: program-role gating.
- **Dependencies**: Patient (R), Finance (E incentives).
- **Migration strategy**: Wave 5; one-service-vs-three open (`10 §7 Q5`).

# 29. Queue & Kiosk — PRELIMINARY
- **Business capability**: Registration queue, doctor queue rooms, kiosk/machine mapping, tokens.
- **Legacy modules**: `QueueManagmentServiceImpl` (TRUNC/to_date :1477-1759; QMSUtil), `PatientPortalServiceImpl`.
- **Owned data**: T_PATIENTQUEUE, T_OPDSTATIONMASTER, T_TABLETMAPPING.
- **PostgreSQL objects**: schema `ihms_queue`; queue state also in Redis (short-lived).
- **APIs**: **Queue Management module 23 endpoints** + **Patient Portal 23**.
- **Events**: **E** TokenIssued (proposed, no consumers); **C** EncounterCreated, AppointmentBooked (rejected event — sync R).
- **Redis**: QMS room token queues (`pushToQ/removeFromQ`, `<qName>_S` counters, `*_time` sums — `07 §2.1`).
- **Solr**: reads (queue lists via encounter search).
- **External integrations**: e-Mitra kiosk (via OPD).
- **Dependencies**: Encounter (R).
- **Migration strategy**: Wave 1.

---

## 7. Shared-Data Strategy (no shared PG tables)

| Category | Tables / data | Owner | Mechanism (evidence) |
|---|---|---|---|
| **Borrowed tables (API)** | Stock/batch (T_ITEMSTOCK) | Inventory (10) | Pharmacy/CSSD/OT call stock API — Pharmacy's CIMS_ADDITEMSTOCK (`PharmacyServiceImpl.java:2140`) moves to Inventory-owned endpoint (decision `10 §7 Q1`) |
| Borrowed tables (API) | Tariff/service/package rates | Reference (21) | rate-lookup API + owned Redis keys; `AddServiceMasterImpl.java:207-244`; `BillServiceImpl.java:104` |
| Borrowed tables (API) | Bed master definition vs occupancy state | Reference vs ADT | §1.1 conflict; API boundary to be defined |
| Borrowed tables (API) | M_STAFFMASTER/M_DOCTORMASTER profiles | IAM vs Reference | §1.1 conflict; identity API |
| Borrowed tables (API) | Patho/rad/OT/drug masters | Reference (21) | Lab/Rad/OT/Pharmacy read via /masters API |
| **Read-model tables (event-fed)** | Patient identity (T_REGISTRATION family) | Patient (1) | PatientRegistered payload + read-model copies (`10 §2.2`); all modules join T_REGISTRATION today (`agent-10 §3`) |
| Read-model tables | Encounter/Admission keys | OPD (2) / ADT (3) | EncounterCreated/PatientAdmitted carry keys; other services keep local reference copies |
| Read-model tables | Bill settlement state | Billing (5) | InvoiceGenerated/PaymentReceived events; payment updates bill via REST (`AddPaymentServiceHelper.java:463`) |
| Read-model tables | Reporting/analytics | Reporting (26) | ELT from all domain events |
| **API-facade tables (strangler)** | T_ENCOUNTER, T_BILL, T_ITEMSTOCK, T_REGISTRATION* | respective owners | cutover protocol: freeze writes → event-forward → backfill → cut reads (`10 §5`) |
| **Outbox tables** | `outbox` per PG schema | each producer | transactional outbox → Kafka (KAFKA-EVENT-CATALOG §4) |
| Legacy queue tables | T_WHATSAPPMESSAGEQUEUE, T_ABDMREQUESTQUEUE, T_OUTBOUNDHIS | Interop (25) | replaced by Kafka topics; kept as read-model during strangler (KAFKA-EVENT-CATALOG §5) |

**Rule (from `10 §2.2`):** no direct cross-service DB access; no shared tables in PG; borrowed data is read-model fed by events; cache keys have a single owner (Reference).

## 8. Service-Coupling Matrix (PRELIMINARY)

Row = consumer, column = dependency. **S** = sync REST, **E** = event, **R** = read-only (API/cache). Blank = no evidence of coupling.

| Service | Patient(1) | Encounter(2) | ADT(3) | Clinical(4) | Billing(5) | Payment(6) | Inventory(10) | Reference(21) | IAM(22) | Interop(25) | Reporting(26) | Search(27) |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 Patient | — | | | | | | | R | R | E | | E |
| 2 OPD | S | — | | | | | | R | R | E | | E |
| 3 ADT | S | S/C | — | | E | | | R | R | E | | E |
| 4 Clinical | R | R | R | — | **S→E(2)** | | | R | R | E | | |
| 5 Billing | R | R | C | C | — | E/C | | R/S | R | E | | E |
| 6 Payments | | R | | | S/E | — | | R | R | | E | |
| 7 Lab | R | R | | C | E(2) | | | R | R | E | | R |
| 8 Rad | R | R | | C | E(2) | | | R | R | | | R |
| 9 Pharmacy | | R | | C | E | | S | R | R | | | R |
| 10 Inventory | | | | | | | — | R | R | | | E |
| 11 OT | R | | C | C | E | | C | R | R | | | R |
| 12 Nursing | | R | R | C | | | | | R | | | |
| 13 Blood Bank | R | | C | | | | C | | R | E | | E |
| 14 Diet | | | R | | | | | | R | | | |
| 15 CSSD | | | | C | | | C | | R | | | |
| 16 Linen | | | R | | | | | | R | | | |
| 17 Transport | R | | R | | | | | | R | | | |
| 18 Equipment | | | | | | | C | | R | | | |
| 19 BMW | | | R | | | | | | R | | | |
| 20 Grievance | R | | | | | | | | R | | | |
| 21 Reference | | | | | | | | — | R | | | E |
| 22 IAM | | | | | | | | R | — | | | |
| 23 RCM | | | C | | R | C | | | R | E | | |
| 24 RMRS | | | | | | C | | | R | | | |
| 25 Interop | C | C | C | C | C | C | C | | R | — | | |
| 26 Reporting | C | C | C | C | C | C | C | R | R | | — | R |
| 27 Search | C | C | C | C | C | | C | C | R | | | — |
| 28 Govt | R | | | | | | | | R | | | |
| 29 Queue | R | R | | | | | | | R | | | R |

Notes: (1) Clinical→Billing is **S today, E post-saga** (`10 §4`); (2) Lab/Rad→Billing are same-txn today, event post-saga (KAFKA-EVENT-CATALOG §3). Interop consumes **all** domain events; Reporting/Search consume all read-model events. IAM is the universal dependency (token introspect).

## 9. Open Questions (carried from `10 §7` + this synthesis)

1. Pharmacy/Inventory stock boundary (Q1) — blocks services 9/10 DDL.
2. M_* ownership conflicts (POSTGRESQL-MIGRATION §1.1) — blocks Reference/ADT/IAM/Inventory table allocation.
3. T_ENCOUNTER/T_BILL volume → partitioning + retention (Q2).
4. Regulatory reporting SLA → Reporting architecture (Q3).
5. Audit service (T_AUDIT, 568 call sites) — dedicated service/topic? (Q4/Q7).
6. Multi-unit tenancy design on PG (Q6).
7. Govt programs: 1 vs 3 services (Q5).
8. Per-service endpoint re-mapping of 3,060 endpoints (04 catalog module→service) — pending.
9. Solr `service` collection drop + encounter 3-day retention policy (`07 §1.6`).
