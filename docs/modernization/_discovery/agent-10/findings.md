# Agent 10 — Microservices / PostgreSQL Modernization: Discovery Findings (iHMS / OHUM PAS NG)

**Status: PRELIMINARY — first-pass discovery. Service names are candidates, NOT finalized.**
**Monolith evidence base:** `D:\ihms` — Spring Boot 2.7.5 + CXF JAX-RS + EclipseLink (JPA/JTA) + Oracle DB + 719 PL/SQL stored procedures + Solr Cloud + Redis + FAUG server-side template frontend.
**Date:** 2026-08-14. **Scope:** research-only; no code modified.

---

## 1. Evidence inventory — module → services → entities → tables

### 1.1 Codebase shape (counts)

| Area | Count | Evidence |
|---|---|---|
| JAX-RS service impls under `com.ohum.services.pas` | 55 | `src/main/java/com/ohum/services/pas/*ServiceImpl.java` |
| JAX-RS service impls under `com.ohum.services.custom` | 49 | `src/main/java/com/ohum/services/custom/*.java` |
| Root/other service impls (`services`, `nurse`, `patp`, `payment`, `abdm`, `ehr`, `ejs`, `aasmaan`, `cssd`) | ~30 | `src/main/java/com/ohum/services/**` |
| Master entities | 609 | `src/main/java/com/ohum/models/masters/*.java` |
| Transactional entities | 137 | `src/main/java/com/ohum/models/transactionals/*.java` |
| trx entities (Encounter, RegistrationQueue, MLCDetails, EmergencyCaseDetails…) | 15 | `src/main/java/com/ohum/models/trx/*.java` |
| txt entities (Appointment, PatientToken, Sponsor, DoctorCheckInQueueRoom, POissue…) | 10 | `src/main/java/com/ohum/models/txt/*.java` |
| ejs (Rajasthan EJS/ASHA program) entities | 61 | `src/main/java/com/ohum/models/ejs/` |
| aasmaan entities | 35 | `src/main/java/com/ohum/models/aasmaan/` |
| rcm / hcx / statistical / chiranjeevi | 2/2/2/2 | `src/main/java/com/ohum/models/{rcm,hcx,statistical,chiranjeevi}` |
| Generated Oracle stored procedures | 719 | `generated-procs/CIMS_*.sql` (CIMS_ADDADMISSIONINFORMATION, CIMS_ADDBILL, CIMS_ADDITEMSTOCK, CIMS_ADDPATHORDERBOOKING, …) |
| Service wiring beans | ~90 | `src/main/resources/services.xml` (271 lines) |
| Solr collections | 6 | `application.properties:175-187` (patient, encounter, item, service, icd10cm, healthfactor) |
| Scheduled jobs (async side-effect backbone) | 30+ | `src/main/java/com/ohum/jobs/ApplicationJobs.java` |

**Table naming convention:** `T_*` transactional (e.g. `T_Encounter` `src/main/java/com/ohum/models/trx/Encounter.java:31`), `M_*` master (e.g. `M_SERVICEMASTER` `Servicemaster.java`, `M_ITEMMASTER` `Itemmaster.java`, `M_COMPANYMASTER` `Companymaster.java`).

**Architecture style:** business logic lives substantially in Oracle PL/SQL. JAX-RS methods are thin controllers invoking named stored procs:
`getGenericDAO().getQ("CIMS_...", Dto.class).namedStoredProcQ().namesAndValues(map).getInOutVarsAsObject()`.
Examples: `BillServiceImpl.java:70` (CIMS_UNSETTLEDBILLCANCELLATION), `AddPaymentServiceHelper.java:291` (CIMS_ADDPAYMENT), `PharmacyServiceImpl.java:660` (CIMS_ADDPHARMACYINTERVENTION).

A Spring AOP aspect wraps every `@Path`/`@WebMethod` on every `*ServiceImpl` (`ServiceLayerAspect.java:223`) and performs: param decryption, audit events, FAUG JS template rendering of the frontend (`ServiceLayerAspect.java:234` loads `integration/scripts/api.js` into Nashorn), context-map threading, and Solr delete side effects (`ServiceLayerAspect.java:1172`).

### 1.2 Module → service → entity → table mapping (candidate boundaries)

| # | Module area | Primary services (file:line for key ops) | Owned/shared entities (examples) | Key tables (from @Table + procs) |
|---|---|---|---|---|
| 1 | **Patient / Registration** | `PatientServiceImpl` (solr index on save `PatientServiceImpl.java:5610`), `PatientDetailRegistrationServiceImpl` (`save` at `:1989`), `PatientRegAdmissionServiceImpl`, `PatientServiceHelper`, `PatientRegApptServiceImpl`, `MasterServiceImpl` | Registration, PatientSponsor, FamilyMemberDetails, PatientAllergies, PatientDocuments, EmergencyCaseDetails (trx) | T_REGISTRATION, T_REGISTRATION_DETAILS, T_PATIENTSPONSOR (CIMS_ADDPATIENTREGISTRATION, CIMS_ADDPATIENTREGISTRATIONDETAILSNEW1, CIMS_ADDPATIENTSPONSOR_NEW_DOC) |
| 2 | **Appointment / OPD / Queue** | `AppointmentServiceImpl` (txn at :1207,:1596,:2017,:2042,:2186), `AddUpdateAppointmentServiceImpl`, `AppointmentScheduleServiceImpl`, `QueueManagmentServiceImpl`, `EmergencyAndWalkinVisitServiceImpl` (encounter save `:1379`, `save(ptoken)` `:468`), `DoctorListServiceImpl` | Appointment (txt), DoctorCheckInQueueRoom (txt), PatientToken (txt), RegistrationQueue (trx), DoctorCheckInIpMaster, Doctorschedule masters | T_APPOINTMENT, T_ENCOUNTER, T_DOCTORCHECKIN_QUEUEROOM (DoctorServiceImpl.java:1045), T_PATIENTTOKEN, T_REGISTRATIONQUEUE |
| 3 | **IPD / ADT / Bed** | `IPDAdmissionServiceImpl` (CIMS_ADDADMISSIONINFORMATION `:130`, CIMS_UPDATEBEDOCCUPIEDFLAG_1 `:313`), `IPDServiceImpl`, `IPDHelper`, `BedServiceImpl`, `BedReservationServiceImpl` (txn :60,:212), `BedManagementServiceImpl`, `PatientTransferDischargeServiceImpl`, `CancelAdmissionServiceImpl` | T_Admission, TransferRequest, PatientDischargeData, MLCDetails (trx), AttendantPassDetails, BedReservation, Bedmaster/Wardmaster/Roommaster/Cabinmaster (masters) | T_ADMISSION, T_BEDRESERVATION, T_TRANSFERREQUEST, T_PATIENTDISCHARGEDATA (`PatientDischargeData.java:31`), T_MLCDETAILS, T_BEDMASTER, T_WARDMASTER |
| 4 | **Billing / Payment / Refund** | `BillServiceImpl` (10 @Transactional methods :66-:579), `BillDetServiceImpl`, `BillReceiptServiceImpl`, `BillSummaryServiceImpl`, `PaymentServiceImpl`, `AddPaymentServiceImpl`/`AddPaymentServiceHelper` (CIMS_ADDPAYMENT :291), `AddAdvanceServiceImpl` (CIMS_ADDADVANCE :103), `RefundServiceImpl`, `AddRefundServiceImpl`, `RefundBillReceiptServiceImpl`, `BillCancellationServiceImpl`, `AdvanceBillReceiptServiceImpl`, `AllReceiptsServiceImpl`, `ChargeServiceImpl`, `ConcessionTemplateServiceImpl`, `PatientBillEstimationServiceImpl` | T_Bill (CIMS_ADDBILL), BillDetails (CIMS_ADDBILLDETAILS_1), T_Advance, T_Payment, PaymentModeDetails, CreditNote + CreditNoteDetails (`AddServiceOrderServiceImpl.java:2680-2702`), DoctorBill, DoctorShare, Concession, Tariffservicepackage | T_BILL, T_BILLDETAILS, T_PAYMENT, T_ADVANCE, T_CREDITNOTE, T_DOCTORBILL, T_DOCTORSHARE, T_CONCESSION, T_REFUND |
| 5 | **Ordering / Clinical** | `AddServiceOrderServiceImpl` + `AddServiceOrderServiceHelper` (THE billing/order mega-txn, see §2.1), `ClinicalServiceImpl`, `NoteDetailsServiceImpl`, `PrescriptionMasterServiceImpl`, `PrescriptionDetailsServiceImpl`, `MedicationServiceImpl`, `InvestigationServiceImpl` | Patientadvice, PatientEorder, PatientMedication, PatientNote, PatientPrescptionNote, EmrDetails, EmrServices (`AddServiceOrderServiceImpl.java:2351,2367`), ConsultOrder, Orderset masters | T_PATIENTADVICE, T_PATIENTEORDER, T_PATIENTNOTE, T_PATIENTMEDICATION, T_EMRDETAILS, T_EMRSERVICES, T_CONSULTORDER |
| 6 | **Pathology / Lab** | `PathoOrderServiceImpl` (14629 lines; CIMS_ADDPATHORDERBOOKING), `PathoOrderServiceHelper` (whatsapp report send `:1130`), `PatientRequisitionServiceImpl` | PathoOrderBooking, Pathoparametermaster/Pathotestmaster/Pathoprofilemaster (masters), PathoPatientDocs, MachineConfiguration, MachineWiseMicroResult, Organism masters | T_PATHORDERBOOKING, T_PATHORDERBOOKINGDETAIL, T_PATHOPATIENTPARAMETERDETAILS, T_MACHINECONFIGURATION (CIMS_ADDPATHORDERBOOKING, CIMS_ADDPATHPATIENTPARAMETERDETAILS_NEW) |
| 7 | **Radiology** | `RadiologyServiceImpl`, `AddServiceOrderServiceHelper.java:579-599` (CIMS_ADDRADORDERBOOKING) | Radtestmaster, Radmodalitymaster, Radtemplateresult, Raditemdetailsmaster, ImagingLocationMaster | T_RADORDERBOOKING, T_RADORDERBOOKINGDETAILS (CIMS_ADDRADORDERBOOKING, CIMS_ADDRADORDERBOOKINGDETAILS) |
| 8 | **Pharmacy / Dispensing** | `PharmacyServiceImpl` (3930 lines; CIMS_ADDPHARMACYINTERVENTION :660, CIMS_ADDITEMSTOCK :2140, CIMS_ADDWARDPICKLIST :2383, CIMS_SETEMRNO :886), `DrugServiceImpl`, `ItemSalesServiceImpl`, `MaterialConsumptionServiceImpl` | PrescriptionMaster, PrescriptionDispenseDetails, Itembatchmaster, ItemStock, ItemSales, Compoundmaster, Druginteractions, Dosefrequency, Medicationroute | T_PRESCRIPTIONMASTER, T_ITEMSALE, T_ITEMSTOCK, T_ITEMBATCHMASTER, T_PRESCRIPTIONDISPENSEDETAILS, T_ITEMCOMPOUNDDETAILS |
| 9 | **Inventory / Procurement / Store** | `InventoryServiceImpl` (12941 lines; god node 250 edges), `PurchaseOrderServiceImpl`, `QuotationServiceImpl`, `OpeningBalanceServiceImpl`, `ExpiryItemServiceImpl`, `ItemStockServiceImpl`, `ItemMasterServiceImpl`, `ItemCategoryServiceImpl`, `MachineConfigServiceImpl`, `InventoryEAushadiIntegrationImpl`, `UploadFileServiceImpl` (294 edges) | Itemmaster, Itemgroup, Itemcategory, Supplier/SupplierMaster, PurchaseOrderRequest, PurchaseRequest, RateContract, IndentInvoice, Store, Grnchecklist, ReorderAndSafetyMaster, EquiqpOpeningBalance | M_ITEMMASTER, T_ITEMSTOCK, T_PURCHASEORDER, T_GRN (CIMS_ADDGRN, CIMS_ADDGRNITEMS_IMPROVED1, CIMS_ADDGRN_PROVISIONAL), T_INDENT (CIMS_ADDINDENT, CIMS_ADDINDENTDETAIL_NEWONE), T_SUPPLIER, T_RATECONTRACT, T_OPENINGBALANCE, T_CONSIGNMENTGRN |
| 10 | **OT / Surgery** | `OTDetailsServiceImpl`, `OTSchedulerServiceImpl` (solr patient search :4879), `OtConfigServiceImpl`, `SurgeryDetailsServiceImpl`, `PctsServiceImpl` | Otschedule, Ottablemaster, Ottheatremaster, SurgeryDetails, Proceduremasters (Proceduremaster, Procedurechecklist…), Anesthesianotes | T_OTSCHEDULE, T_OTSCHEDULEDETAILS, T_SURGERYDETAILS, T_PROCEDUREMASTER, T_ANESTHESIANOTES |
| 11 | **Nursing / Inpatient care** | `NursingServiceImpl` (349 edges; ROWNUM query `:7124`), `NursePatientDetails` (txt) | NurseNote, NurseTaskDetails, NurseTaskFrequencyDetails, PatientVitals, Intakeoutput, Nursingstation masters, IpdRosterDetails | T_NURSENOTE, T_PATIENTVITALS, T_NURSETASKDETAILS, T_NURSINGSTATIONMASTER |
| 12 | **Blood Bank** | `BloodBankServiceImpl` (~50 @Transactional methods; SYSDATE queries `:6214`, eRaktkosh jobs `ApplicationJobs.java:1205,1383`) | Bloodbag, Bloodcomponent, Bloodgroup, Donor masters, BloodDonationCampMaster, Externalbloodstoragecenter | T_BLOODBAG, T_BLOODCOMPONENT, T_BLOODREQUISITION, T_BLOODDONATIONCAMP, T_BLOODDONOR |
| 13 | **Diet** | `DietServiceImpl` (SYSDATE/to_char queries `:154-161`) | Dietorder, DietItemScheduleDetails, TodayDietItemMenu, Dietplanmaster, Fooditemmaster | T_DIETORDER, T_DIETITEMSCHEDULEDETAILS, T_TODAYDIETITEMMENU, M_DIETPLANMASTER |
| 14 | **CSSD / Sterilization** | `CSSDServiceImpl`, `ProcedureCssdRequest` (transactionals) | CssdKitFormation, Cssdkitissueitem, Sterilization masters, ProcedureCssdIssue | T_CSSDKITFORMATION, T_CSSDKITISSUEITEM, T_STERILIZATIONMASTER, T_PROCEDURECSSDREQUEST |
| 15 | **Linen / Laundry** | `LinenLaundryServiceImpl` | LinenRequest, LinenRequestDetails, Linencategory, Linendetail | T_LINENREQUEST, T_LINENCATEGORY, T_LINENDETAIL |
| 16 | **Transport** | `TransportServiceImpl` | TransportRequest, TransportRequestDetails, VehicleMaintenance, DriverMaster, Ambulancemaster, VehicleRequestTypeMaster | T_TRANSPORTREQUEST, T_VEHICLEMAINTENANCE, M_AMBULANCEMASTER |
| 17 | **Bio-Medical Waste** | `BioMedicalWasteManagementServiceImpl` | BioMedicalWaste, BioMedicalWasteDetails, Wastebagmaster | T_BIOMEDICALWASTE, T_BIOMEDICALWASTEDETAILS |
| 18 | **Equipment** | `EquipmentServiceImpl`, `PreventiveMaintenanceServiceImpl`, `PreventiveMaintenanceScheduleServiceImpl`, `EquipmentServiceHelper` | Equipment, EquipmentTransfer, EquipmentWarranty, EqpComplaint(+Resolution), EqpPMSchedule, Equipinsurance, Equipspares, Serviceagent masters | T_EQUIPMENT, T_EQPCOMPLAINT, T_EQPPREVENTIVEMAINTENANCE, T_EQUIPMENTWARRANTY, M_SERVICEAGENTMASTER |
| 19 | **Grievance / Feedback** | `ComplaintDetailsServiceImpl` (txn :221,:245,:365,:532), `ComplaintResolutionDetailServiceImpl`, `QuetionnaireServiceImpl`, `PatientFeedback` (transactionals) | PatientComplaints, PatientGrievance*, Complaintmaster, GrievanceCategoryMaster, Feedbacktemplate | T_PATIENTCOMPLAINTS, T_PATIENTGRIEVANCE*, T_COMPLAINTMASTER, T_FEEDBACKTEMPLATE |
| 20 | **RCM / Claims** | `RcmServiceImpl`, `NHCXServiceImpl` (`src/main/java/com/ohum/services/abdm/NHCXServiceImpl.java:30`), models/rcm (EligibilityCheck, AdmissionTIDDetails), models/hcx (NHCXRequest) | EligibilityCheck, AdmissionTIDDetails, NHCXRequest, NHCXRequestDetails | T_ELIGIBILITYCHECK, T_ADMISSIONTIDDETAILS, T_NHCXREQUEST, T_NHCXREQUESTDETAILS |
| 21 | **RMRS / treasury** | `RMRSServiceImpl` (ROWNUM=1 `:1361-1366`), `PmrdServiceImpl` | RmrsCollection, RMRSDashboardTransaction, AuthorityMaster, HeadsMaster, Ledgermaster, Accounting* masters | T_RMRSCOLLECTION, T_AUTHORITYMASTER, T_HEADSMASTER, T_LEDGERMASTER, T_ACCOUNTINGLEDGER |
| 22 | **Queue / Kiosk** | `QueueManagmentServiceImpl` (TRUNC/to_date queries `:1477-1759`), `PatientPortalServiceImpl` | PatientQueue, DoctorCheckInQueueRoom, OpdStationMaster, OpdMachineMaster, Tabletmapping | T_PATIENTQUEUE, T_OPDSTATIONMASTER, T_TABLETMAPPING |
| 23 | **Masters / Reference** | `MasterServiceImpl`, `MasterCachedServiceImpl` (GOD NODE — 514 edges per graphify-out), `MasterCachedServiceHelper`, `MdmServiceImpl`, `AddServiceMasterImpl`, `ServiceMasterServiceImpl`, `TariffServiceImpl`, `RefEntityServiceImpl`, `PackageServiceServiceImpl`, `PlanMasterServiceImpl`, `CompanyApprovedAmountServiceImpl`, `ItemMasterServiceImpl` | 609 master entities: Servicemaster, Tariffmaster, Unitmaster, Departmentmaster, Companymaster, Doctormaster, Staffmaster, Itemmaster, IcdCmMaster, Healthfactormaster, Taxmaster, Store, Bankmaster, etc. | M_SERVICEMASTER, M_TARIFFMASTER, M_UNITMASTER, M_DEPARTMENTMASTER, M_COMPANYMASTER, M_DOCTORMASTER, M_STAFFMASTER, M_ITEMMASTER, M_ICD10CMMASTER, M_HEALTHFACTORMASTER |
| 24 | **Security / User** | `AuthenticationServiceImpl` (login/LDAP `:66-:293`), `UserManagementServiceImpl`, `AddUserServiceImpl` (user+staff+doctor creation `:789-:1070`), `Encrypter`, `SessionKeyDetails` | TUserdetails, TUserrolemaster, TUserunitdetails, TUserStoreDetails, TUserWardDetails, Menumaster, ModulePermissionMaster, Staffmaster, Doctormaster | T_USERDETAILS, T_USERROLES, T_USERROLEMASTER, T_USERUNITDETAILS, T_MENUMASTER, M_STAFFMASTER, M_DOCTORMASTER |
| 25 | **Reports** | `ReportServiceImpl`, `FileManagementServiceImpl`, `BillInvoiceGenerator` (JasperReports `util/BillInvoiceGenerator.java:19-25`), Velocity templates (`templates/*.vm`) | Reportmaster, Reportparametermaster, ReportSummary, AllReceipts | T_REPORTMASTER, generated PDFs stored via `CommonUtilServiceImpl.savePdfToTable` (`:6067`) |
| 26 | **EHR / HL7 interop** | `EHRUtilServiceImpl`, `EHRSyncJobs` (NVL/SYSDATE queries `:54-:314`), `Hl7InboundMessageJob` (`jobs/Hl7InboundMessageJob.java:40`), `HL7Helper` (A01/A45/ORM creators, seq from DUAL `:246`), `IntegrationHelper` | Outboundhis, Inboundhis, EHRJobDetails, PatientDischargeData, TempPrescriptionNote | T_OUTBOUNDHIS, T_INBOUNDHIS, T_EHRJOBDETAILS |
| 27 | **ABDM / ABHA** | `AbhaServiceImpl`, `HPRServiceImpl`, `ABDMController`, `HFRController` (`services.xml`), `CommonABDMUtil` (commented MERGE `:1089`), ABDM queue jobs (`ApplicationJobs.java:1661,1688,1899,2539`) | T_PATIENT_ABDM (token check `EmergencyAndWalkinVisitServiceImpl.java:652`), T_ABDMREQUESTQUEUE (`ApplicationJobs.java:1665`) | T_PATIENT_ABDM, T_ABDMREQUESTQUEUE, TBL_JAN_MEMBER_AADHAAR_REF_NO |
| 28 | **Government programs** | `EjsDetailsServiceImpl`, `EjsIncentiveServiceImpl` (ASHA incentive job `ApplicationJobs.java:1035`), `EjsAncPncChildServiceImpl`, `EjsAshaServiceImpl`, `EjsSupervisorServiceImpl`; `AasmaanCaseServiceImpl` (+9 Aasmaan services); Chiranjeevi (BeneficiaryEnrolment, RefrenceTid) | Ejs* (61), Aasmaan* (35), BeneficiaryEnrolment, RefrenceTid | T_EJS_*, T_AASMAAN_*, T_BENEFICIARYENROLMENT |
| 29 | **State integrations** | `IntegrationServiceImpl` (whatsapp send `:1258-1410`, status callback `:1462`), `SendNotificationServiceImpl` (consent on WhatsApp `:3203-3294`), EAushadhi (`ApplicationJobs.java:2376,2963`), eRaktkosh, RGHS SIMS (`:1065`), PCTS (`:2955`), NMC (`:1733`), JanAadhaar (`:2539`) | WhatsappMessageQueue/Status (transactionals), EAushadi* entities | T_WHATSAPPMESSAGEQUEUE, T_WHATSAPPMESSAGESTATUS, T_EAUSHADHI*, T_OUTBOUNDHIS |

### 1.3 Frontend (FAUG)

- Server-side rendering engine by Sumeet Chhetri executed inside `ServiceLayerAspect` (Nashorn, `integration/scripts/api.js`).
- Templates under `integration/templates/` (`.vm` Velocity templates for reports), `apps/ohumlite_resources/`, `integration/tiuxml/` (module templates).
- **Implication:** every API call returns pre-rendered HTML — service boundary extraction must first decouple rendering (return JSON to a SPA/re-render layer) before API reuse is possible.

---

## 2. Transaction boundaries (atomicity evidence)

### 2.1 The Order→Bill→Lab→Rad→DoctorShare mega-transaction
`AddServiceOrderServiceHelper.java` — one `@Transactional(rollbackOn=Exception.class)` method chains (line refs):
- `:265` CIMS_ADDCHARGE (charges)
- `:341` CIMS_ADDBILL (bill header)
- `:370` CIMS_ADDBILLDETAILS_1 (bill lines)
- `:417` CIMS_ADDDOCTORSHARE (doctor share)
- `:473` CIMS_ADDPATHORDERBOOKING + `:516` CIMS_ADDPATHORDERBOOKINGDETAIL (lab order)
- `:579` CIMS_ADDRADORDERBOOKING + `:599` CIMS_ADDRADORDERBOOKINGDETAILS (rad order)
- `:722-737` CIMS_ADDCONCESSION / CIMS_ADDCONCESSIONDETAIL
- `:908-991` CIMS_UPDATECHARGES / CIMS_UPDATECHARGESTEMP
- Same pattern in `AddServiceOrderServiceImpl.java:2206` (CIMS_ADDBILL), `:2254` (CIMS_ADDCHARGE), `:2680-2702` (CreditNote+Details), `:2526` (package charge subscription), `:2351/:2367` (EmrDetails/EmrServices), `:2299-2318` + `:2379-2399` (HL7 ORM messages for procedure/nursing orders).

**Meaning:** billing, lab-order creation, radiology-order creation, doctor-share accrual, EMR write, package subscription, and HL7 emission are ONE atomic unit today. This is the single hardest coupling for decomposition (§7 risks).

### 2.2 Payment transaction
`AddPaymentServiceHelper.java`:
- `:291` CIMS_ADDPAYMENT → `:309` CIMS_ADDPAYMENTBILLDETAILS → `:333` CIMS_ADDPAYMENTMODEDETAILS → `:348` CIMS_ADDPAYMENTADVANCEDETAILS → `:463` CIMS_UPDATEBILLFORCONCESSION.
- `AddAdvanceServiceImpl.java:103-178` — advance + payment + mode details + advance details in one txn.

### 2.3 Visit / Encounter transaction with side effects
`EmergencyAndWalkinVisitServiceImpl.java` (one method, lines): `:1379` encounter save, `:1411` queue update, `:1463` MLC details, `:1525` FPV, `:1614` HL7 A45 message, `:1677-1695` corona stats, `:1726` patient diagnosis, `:1796-1931` Solr encounter/patient indexing, `:1937` ABDM queue entry.

### 2.4 Admission transaction
`IPDAdmissionServiceImpl.java`: `:130` CIMS_ADDADMMLCDETAILS, `:196` CIMS_ADDADVISEDDISCHARGELIST, `:313` CIMS_UPDATEBEDOCCUPIEDFLAG_1 + `@Transactional(rollbackOn=Exception.class)` at `:186,:446,:461`. Bed + admission + MLC + advised-discharge coupled.

### 2.5 Registration → HL7 → Solr
`HelperServiceImpl.java:221` (registration HL7), `:252` (visit A45), `:307` (admission A01) — HL7 sent synchronously right after DB commit in same txn. `BedReservationServiceImpl.java:166-180` (solr index + HL7 registration message in one method).

### 2.6 Stored procedures are multi-table transactions
- `generated-procs/CIMS_ADDADMISSIONINFORMATION.sql` — inserts `T_Admission` (via `T_Admission_seq.nextval` from dual), reads `M_DoctorMaster`, `M_DepartmentMaster`, `T_Visit`, computes visit-date diff (contains commented-out PostgreSQL `DATE_PART(...)` lines — evidence of prior PG porting consideration).
- `generated-procs/CIMS_ADDITEMSTOCK.sql` — updates `T_ReceivedItemAgainstIssueDetails` (:301,:308), `T_ReceivedItemAgainstReturnDetails` (:318,:325), inserts `T_ItemStock` (:335).
- `generated-procs/CIMS_ADDBILL.sql` — `SELECT T_Bill_seq.nextval, p_billno INTO p_ID, p_billno from dual` (:51).
- `generated-procs/CIMS_ADDADVANCE.sql` — `SELECT T_Advance_seq.nextval INTO p_ID from dual` (:22).

**Conclusion:** DB procedures are the transactional heart; the Java layer adds side effects (Solr, HL7, WhatsApp, ABDM) around proc calls in the same Spring transaction. Splitting services means splitting procs AND side effects.


---

## 3. Shared-table / cross-cutting hotspots

| Table (evidence) | Written by | Read by (evidence) | Assessment |
|---|---|---|---|
| **T_ENCOUNTER / T_APPOINTMENT** (trx models) | Visit/OPD services (`EmergencyAndWalkinVisitServiceImpl.java:1379`, `AddUpdateAppointmentServiceImpl.java:70`) | BillSummary (solr), Pharmacy (dispense by visit), Lab/Rad orders (by opdipdid), Nursing, BloodBank (`BloodBankServiceImpl.java:1430,1624` indexes encounter), Diet, PatientSearch (solr `BaseServiceImpl.java:656`) | **Shared tenant record** — every clinical/financial module keyed by encounter/admission |
| **T_ADMISSION** (CIMS_ADDADMISSIONINFORMATION) | ADT services | Billing (sponsor by admission `AddServiceOrderServiceHelper.java:1213`), Pharmacy (ward dispense `PharmacyServiceImpl.java:482`), Lab/Rad, Nursing, Diet, OT, BloodBank, RMRS | Shared tenant record |
| **T_BILL / T_BILLDETAILS** (CIMS_ADDBILL) | Ordering/billing services | Payment/refund (`AddPaymentServiceHelper.java:309`), Pharmacy (item sale bill), Lab/Rad (order billing), RCM (claims), RMRS (receipts), reports | **Settlement spine** |
| **M_ITEMMASTER + T_ITEMSTOCK + batch** | Inventory (`InventoryServiceImpl`), Pharmacy (`PharmacyServiceImpl.java:2140` CIMS_ADDITEMSTOCK) | Pharmacy, Lab (consumables), OT (procedure equipment `Procedureequipmentdetails`), CSSD, BloodBank, Diet, UploadFile (bulk upload `UploadFileServiceImpl.java:462`) | **Highest contention** — stock decrement is the unit-of-work |
| **M_SERVICEMASTER / M_TARIFFMASTER / packages** | Master services (`AddServiceMasterImpl.java:207` CIMS_ADDSERVICEMASTER2, `:231` CIMS_ADDTARIFFSERVICE2) | Billing (rate lookup `BillServiceImpl.java:104`), all ordering modules, estimation (`PatientBillEstimationServiceImpl`), RCM | Shared pricing contract |
| **T_PATIENT* (registration, details, sponsor)** | Registration services | All clinical modules, billing, ABDM (`CommonABDMUtil.java:1089` MERGE comment), EHR, reports | Shared tenant record |
| **T_USERDETAILS / roles** | AddUserServiceImpl | Everything (auth `AuthenticationServiceImpl.java:228`, per-unit access TUserStoreDetails/TUserWardDetails) | Cross-cutting identity |
| **T_WHATSAPPMESSAGEQUEUE / STATUS** | `AddServiceOrderServiceHelper.java:3924-3926` | `ApplicationJobs.java:1611` (triggerWhatsappReportsInQueue poller) | **Existing async pattern -> Kafka candidate** |
| **T_ABDMREQUESTQUEUE** | `EmergencyAndWalkinVisitServiceImpl.java:1937` (addEntryToABDMQueue) | `ApplicationJobs.java:1661-1688` (addAbhaRecordsNode, sendPatientRecordToAbha) | Existing async pattern -> Kafka candidate |
| **T_OUTBOUNDHIS / T_INBOUNDHIS** | HL7Helper (all `create*HL7Message`) | `Hl7InboundMessageJob.java:40`, EHRSyncJobs | Existing outbox pattern |
| **Solr collections** (patient, encounter, item, service, icd10cm, healthfactor) | `BaseServiceImpl.java:656-660`, `CommonUtilServiceImpl.java:2704`, `SolrUtil.java:174-333`, jobs (`ApplicationJobs.java:867,884,976,2122`) | Search everywhere (`PatientServiceImpl.java:502-509`, `BillSummaryServiceImpl.java:811`, `OTSchedulerServiceImpl.java:4879`, ...) | Cross-cutting index |
| **Redis cache keys** (itemMaster, tariffMaster, unitMaster, classMasterMap_*, specializationMaster, doctor schedules JSON...) | `MasterCachedServiceImpl` (+ `checkService` endpoint `:6411`), `MasterCachedServiceHelper` | ~30 services (`InventoryServiceImpl.java:555`, `BillReceiptServiceImpl.java:703`, `MdmServiceImpl.java:3109`, `UploadFileServiceImpl.java:462`) | Read-through lookup only — never source of truth |

---

## 4. Cross-cutting subsystems (evidence)

### 4.1 Solr
- Collections configured: `application.properties:175-187` — patient, encounter, item, service, icd10cm, healthfactor.
- **Writers today (contexts that will need to emit events or call index API):** Patient save (`PatientServiceImpl.java:5610,5689`, `SolrUtil.java:174-188`), encounter creation (`EmergencyAndWalkinVisitServiceImpl.java:1796-1799`, `SolrUtil.java:245-333`), item master (`SolrUtil.java:990` addBeans itemSolrName), ICD10 (`SolrUtil.java:1596`), health factor (`SolrUtil.java:2052`), batch backfill jobs `syncItemsToSolrJob`, `syncHealthFactorToSolrJob`, `syncIcd10cmToSolrJob`, `indexMissingPatientsInSolr` (`ApplicationJobs.java:867,884,976,2122`), delete hooks (`ServiceLayerAspect.java:1172`).
- **Readers:** patient/encounter search (`PatientServiceImpl.java:502`, `BillSummaryServiceImpl.java:811`), queue, OT scheduler (`:4879`), dashboard counts (`CommonUtilServiceImpl.java:4272-4302`).
- **Design implication:** Solr = search index owned by an indexing pipeline; producers publish events, not direct `addBean` calls.

### 4.2 Redis
- Abstraction: `cache/CacheInterface` with `MemoryCacheImpl`, `RedisCacheImpl`, `RedisClusterCacheImpl` (jedis). Config: `application.properties:109-153` (standalone or 6-node cluster).
- Usage is **exclusively read-through caching and short-lived state** (file-upload attempt counters `BillReceiptServiceImpl.java:3137-3145`, session/token `BillReceiptServiceImpl.java:3120`), plus master-data lookups (evidence in §3 row "Redis cache keys").
- **Design implication:** keep Redis as cache/lookup only; never source of truth. A Reference Master service owns the cache contract + invalidation events.

### 4.3 Async queues already present (DB-poll based -> Kafka candidates)
- WhatsApp reports: `T_WHATSAPPMESSAGEQUEUE` written in billing flow (`AddServiceOrderServiceHelper.java:3926`), polled every 15 min by `triggerWhatsappReportsInQueue` (`ApplicationJobs.java:1611`), PDF generated + sent (`PathoOrderServiceHelper.java:1130-1208`), status callback API (`IntegrationServiceImpl.java:1462`).
- ABDM: `T_ABDMREQUESTQUEUE` polled 5s (`ApplicationJobs.java:1661,1688`); JanAadhaar push job (`:2539`).
- HL7: `T_OUTBOUNDHIS` written synchronously in services; `EHRSyncJobs` (file generation `creteEHRFiles` `ApplicationJobs.java:1096`); `Hl7InboundMessageJob` inbound pickup.
- **Design implication:** these are proven "queue-table + poller" patterns that map cleanly to Kafka topics (outbox pattern) — the strongest evidence that event-driven decomposition is viable.

### 4.4 File storage
- PDFs stored in DB table via `CommonUtilServiceImpl.savePdfToTable` (`:6067-6082`, writes whatsappMsgQueId reference); JasperReports for invoice (`BillInvoiceGenerator.java`), Velocity `.vm` templates for reports, uploads via `UploadFileServiceImpl` (294 edges) with dedup/attempt counters in Redis (`BillReceiptServiceImpl.java:3137`).

---

## 5. Oracle -> PostgreSQL pattern inventory (evidence-backed)

| # | Oracle pattern | PostgreSQL target | Evidence (file:line) |
|---|---|---|---|
| 1 | `NUMBER(19)` PK/FK (`number(19)` proc params; Long entity IDs) | `BIGINT` | `generated-procs/CIMS_ADDADMISSIONINFORMATION.sql` (`v_EAID number(19)`) |
| 2 | `NUMBER(10)` counters, flags | `INTEGER` / `SMALLINT`; char flags `CHAR(1)` -> keep `CHAR(1)` or `BOOLEAN` at app layer | `CIMS_ADDADMISSIONINFORMATION.sql` (`p_isbaby char`, `v_IsUsedForANC number(10)`) |
| 3 | `VARCHAR2(n)` | `VARCHAR(n)` (size audit: `varchar2(200/100/20)` in procs; names/addresses <= 500 in practice) | `CIMS_ADDADMISSIONINFORMATION.sql` params |
| 4 | `CLOB` / `@Lob` | `TEXT` | `models/FileDetails.java:55`, `models/transactionals/EquipmentDetailDocs.java:43`, `models/transactionals/WarrantyList.java:39-47`, `models/masters/Equipmentdetail.java:101-109`, `models/masters/Equipinsurancedetails.java:76-84`, `models/masters/ImageAnnotationMaster.java:54` |
| 5 | `TIMESTAMP(3)` / `DATE` columns | `TIMESTAMP(3)`; keep `DATE` only for date-only semantics (DOB, RegistrationDate); **semantic analysis required** — many `DATE` columns carry time (comparisons with `TRUNC`) | proc signatures (`p_date timestamp`), `ApplicationJobs.java:594` `TRUNC(ADDEDDATETIME)` |
| 6 | `SEQUENCE.NEXTVAL FROM DUAL` | `BIGSERIAL`/`IDENTITY`, **or preserved explicit sequences** where legacy IDs must continue / procs reuse them | `ServiceLayerAspect.java:725` (dynamic `SELECT <seq>.NEXTVAL FROM DUAL`), `HelperServiceImpl.java:1175`, `HL7Helper.java:246` (`t_outboundhis_seq`), `CIMS_ADDADMISSIONINFORMATION.sql`, `CIMS_ADDBILL.sql:51`, `CIMS_ADDADVANCE.sql:22` |
| 7 | `NVL(a, b)` | `COALESCE(a, b)` | `EHRSyncJobs.java:54-55` (`NVL(pd.resstateid,0)`), `ApplicationJobs.java:2249-2254` (`NVL(SUM(iss.availableStock),0)`), `PatientServiceImpl.java:5119-5155` |
| 8 | `SYSDATE` | `CURRENT_TIMESTAMP` / `NOW()` | `ApplicationJobs.java:2263` (`"date" >= SYSDATE - 15`), `EHRSyncJobs.java:102` (`SYSDATE,SYSDATE` inserts), `BloodBankServiceImpl.java:6214` |
| 9 | `TRUNC(SYSDATE)` (day-level) | `CURRENT_DATE` or `DATE_TRUNC(''day'', NOW())` | `ApplicationJobs.java:594,1158-1166,1665`, `DoctorServiceImpl.java:1045`, `QueueManagmentServiceImpl.java:1477-1759` |
| 10 | `TRUNC(d,''MM'')`, `ADD_MONTHS(SYSDATE,-1)` | `DATE_TRUNC(''month'', d)`, `NOW() - INTERVAL ''1 month''` | `IntegrationServiceImpl.java:2225`, `ApplicationJobs.java:2446` |
| 11 | `ROWNUM <= n` / `ROWNUM = 1` | `LIMIT n` / `LIMIT 1` (or `FETCH FIRST`) | `ApplicationJobs.java:1679` (`ROWNUM <=`), `NursingServiceImpl.java:7124` (`ROWNUM <= ?`), `RMRSServiceImpl.java:1361-1366` (`ROWNUM = 1`) |
| 12 | `paginate(0, 500)` (EclipseLink) | `LIMIT ? OFFSET ?` | `ApplicationJobs.java:533,554,631` |
| 13 | `MERGE INTO` | `INSERT ... ON CONFLICT (...) DO UPDATE` | `CommonABDMUtil.java:1089` (commented legacy MERGE on T_REGISTRATION_DETAILS/T_PATIENT_ABDM) |
| 14 | `to_char(cast(SYSDATE as date),''hh24:mi:ss'')` | `TO_CHAR(NOW(),''HH24:MI:SS'')` | `DietServiceImpl.java:154` |
| 15 | `to_date(''...'',''ddmmyyyy'')` | `TO_DATE(''...'',''DDMMYYYY'')` (identical) | `QueueManagmentServiceImpl.java:1477-1487` |
| 16 | `DATE 'yyyy-mm-dd'` literal | identical | `VisitServiceImpl.java:406-411`, `ApplicationJobs.java:554` |
| 17 | `EXTRACT(YEAR FROM SYSDATE)` | `EXTRACT(YEAR FROM NOW())` | `BloodBankServiceImpl.java:7367-7379` |
| 18 | `interval ''1'' day` | identical | `BloodBankServiceImpl.java:6556` |
| 19 | Quoted reserved words `"date"`, `"Comment"` | identical quoted identifiers (avoid `"date"` in new schema) | `CIMS_ADDADMISSIONINFORMATION.sql`, `ApplicationJobs.java:2263` |
| 20 | Boolean as `1/0` char | keep app-side boolean mapping (`getBooleanValueForQuery`); `BOOLEAN` on PG if no proc cross-talk | `ApplicationJobs.java:1615` (`SYNCHRONIZED = ...false`) |
| 21 | `DECODE`, `CONNECT BY`, `CONNECT BY PRIOR` | `CASE`, recursive CTE (`WITH RECURSIVE`) | **not observed in Java samples** — must audit `generated-procs/*.sql` (719 files) during deep-dive |
| 22 | Stored procedures (719) | strategy TBD: (a) keep as PG functions during strangler, (b) rewrite as service logic, (c) hybrid per module | `generated-procs/*`; called via `namedStoredProcQ` (e.g. `BillServiceImpl.java:70`) |
| 23 | JDBC `oracle.jdbc.OracleDriver`, `spring.jpa.database=ORACLE` | `org.postgresql.Driver`, EclipseLink -> Hibernate or EclipseLink PG dialect | `application.properties:63-77` |
| 24 | DUAL table | drop (not needed) | `ServiceLayerAspect.java:725` etc. |
| 25 | Old Postgres hint: commented `DATE_PART(''day'',...)` inside Oracle proc | validates feasibility of PG target; keep expression parity list per proc | `CIMS_ADDADMISSIONINFORMATION.sql` (comment near visit-date diff) |

**Missing evidence (gaps to close in next pass):** DECODE/CONNECT BY/analytic functions inside procs (grep `generated-procs/`); full VARCHAR2 size histogram; identity columns vs sequences per table; triggers; Oracle-specific indexes (function-based, bitmap).


---

## 6. Preliminary candidate bounded contexts (first-pass, evidence-driven)

Derived from: business capability (service/entity grouping §1.2), transaction boundaries (§2), data ownership (which service writes which tables), change frequency (masters low-frequency vs billing high-frequency), integration requirements (§4). **CANDIDATE NAMES — not finalized.**

| # | Candidate context | Absorbs (legacy evidence) | Owns (tables) | Must share/borrow | Key evidence |
|---|---|---|---|---|---|
| 1 | **Patient Identity & Registration** | PatientServiceImpl, PatientDetailRegistrationServiceImpl, PatientRegAppt/Admission, MasterServiceImpl (registration masters) | T_REGISTRATION(+DETAILS), T_PATIENTSPONSOR, T_PATIENT_ABDM, T_REGISTRATIONQUEUE | PatientSolr index (borrows search), ABDM queue (emits) | `PatientServiceImpl.java:5610`; `EmergencyAndWalkinVisitServiceImpl.java:652` |
| 2 | **Appointment & Encounter (OPD)** | AppointmentServiceImpl, AddUpdateAppointmentServiceImpl, EmergencyAndWalkinVisitServiceImpl, QueueManagmentServiceImpl | T_APPOINTMENT, T_ENCOUNTER, T_PATIENTTOKEN, doctor queue/cabin tables | Patient (reads), Solr (emits index event), HL7 (emits) | `AppointmentServiceImpl.java:1207`; `EmergencyAndWalkinVisitServiceImpl.java:1379-1937` |
| 3 | **ADT & Bed** | IPDAdmissionServiceImpl, IPDServiceImpl, Bed*, BedReservationServiceImpl, PatientTransferDischargeServiceImpl | T_ADMISSION, bed/ward/room/cabin, T_MLCDETAILS, T_PATIENTDISCHARGEDATA, T_ATTENDANTPASS | Encounter (reads), Solr (emits), HL7 A01 (emits) | `IPDAdmissionServiceImpl.java:130,313`; `HelperServiceImpl.java:307` |
| 4 | **Ordering & Prescription (Clinical)** | AddServiceOrderServiceImpl, ClinicalServiceImpl, PrescriptionMaster/Details, MedicationServiceImpl, NoteDetailsServiceImpl, InvestigationServiceImpl | T_PATIENTADVICE, T_PATIENTEORDER, T_PATIENTMEDICATION, T_PATIENTNOTE, T_EMRDETAILS, T_EMRSERVICES, T_CONSULTORDER | **Bill (today same txn!)** — must split via saga | `AddServiceOrderServiceImpl.java:2206-2399` |
| 5 | **Billing & Settlement** | BillServiceImpl, BillDet/Receipt/Summary, ChargeServiceImpl, Concession, DoctorBill/Share, CreditNote, PatientBillEstimation | T_BILL, T_BILLDETAILS, T_CHARGE, T_DOCTORBILL, T_DOCTORSHARE, T_CONCESSION, T_CREDITNOTE | Tariff/Service master (reads), Payment (consumes InvoiceGenerated) | `BillServiceImpl.java:66-579`; `AddServiceOrderServiceHelper.java:341-516` |
| 6 | **Payments & Refunds** | PaymentServiceImpl, AddPayment(+Helper), AddAdvance, Refund*, BillCancellation, AllReceipts | T_PAYMENT, T_PAYMENTMODEDETAILS, T_ADVANCE, T_REFUND, T_RECEIPT | Bill (reads/updates settlement state), RMRS (treasury events) | `AddPaymentServiceHelper.java:291-348` |
| 7 | **Lab (Pathology)** | PathoOrderServiceImpl, PathoOrderServiceHelper, PatientRequisitionServiceImpl | T_PATHORDERBOOKING(+DETAIL), patho parameter/result tables, T_MACHINECONFIGURATION | Bill (consumes LabOrderCreated for billing), WhatsApp/EHR (emits LabResultFinalized) | `PathoOrderServiceImpl.java:2851-3183`; `PathoOrderServiceHelper.java:1130` |
| 8 | **Radiology** | RadiologyServiceImpl (+ rad booking inside AddServiceOrderServiceHelper:579) | T_RADORDERBOOKING(+DETAILS), rad masters | Bill, Solr (patient search), WhatsApp | `AddServiceOrderServiceHelper.java:579-599` |
| 9 | **Pharmacy & Dispensing** | PharmacyServiceImpl, DrugServiceImpl, ItemSalesServiceImpl, MaterialConsumptionServiceImpl | T_PRESCRIPTIONMASTER, T_ITEMSALE, T_PRESCRIPTIONDISPENSEDETAILS, T_ITEMCOMPOUNDDETAILS, ward picklist | **ItemStock (borrowed from Inventory — dual-write risk)**, PatientMedication (borrows) | `PharmacyServiceImpl.java:2140` (CIMS_ADDITEMSTOCK), `:2383` |
| 10 | **Inventory & Procurement** | InventoryServiceImpl, PurchaseOrderServiceImpl, QuotationServiceImpl, OpeningBalance, ExpiryItem, ItemStock/ItemMaster/ItemCategory, UploadFileServiceImpl (item uploads), EAushadhi integration | M_ITEMMASTER, T_ITEMSTOCK, T_ITEMBATCHMASTER, T_PURCHASEORDER, T_GRN*, T_INDENT, T_SUPPLIER, T_RATECONTRACT, T_QUOTATION, T_CONSIGNMENTGRN | ItemSolr (emits), Pharmacy/OT/CSSD consume InventoryUpdated | `InventoryServiceImpl.java` (god node), `generated-procs/CIMS_ADDITEMSTOCK.sql` |
| 11 | **OT & Surgery** | OTDetailsServiceImpl, OTSchedulerServiceImpl, OtConfig, SurgeryDetailsServiceImpl | T_OTSCHEDULE, T_SURGERYDETAILS, procedure masters, anesthesianotes | Solr (patient search), Inventory (consumables), Billing (procedure rates) | `OTSchedulerServiceImpl.java:4879` |
| 12 | **Nursing & Inpatient Care** | NursingServiceImpl, NursePatientDetails | T_NURSENOTE, T_PATIENTVITALS, T_NURSETASKDETAILS, nursing station masters | Encounter/Admission (reads) | `NursingServiceImpl.java` (349 edges, near-god node) |
| 13 | **Blood Bank** | BloodBankServiceImpl, eRaktkosh jobs | T_BLOODBAG, T_BLOODCOMPONENT, T_BLOODREQUISITION, T_BLOODDONOR, donation camps | Inventory (stock of blood products?), Solr encounter (visit link), ABDM (notifications) | `BloodBankServiceImpl.java` (~50 txns) |
| 14 | **Diet** | DietServiceImpl | T_DIETORDER, T_DIETITEMSCHEDULEDETAILS, diet plan/food masters | Admission (reads) | `DietServiceImpl.java:78-161` |
| 15 | **CSSD** | CSSDServiceImpl | T_CSSDKITFORMATION, T_CSSDKITISSUEITEM, T_STERILIZATION*, T_PROCEDURECSSD* | OT procedure requests (borrows), Inventory (consume InventoryUpdated) | `CSSDServiceImpl.java`; `models/transactionals/CssdKitFormation.java` |
| 16 | **Linen & Laundry** | LinenLaundryServiceImpl | T_LINENREQUEST, T_LINENCATEGORY, T_LINENDETAIL | Admission (reads) | `LinenLaundryServiceImpl.java` |
| 17 | **Transport** | TransportServiceImpl | T_TRANSPORTREQUEST, T_VEHICLEMAINTENANCE, M_AMBULANCEMASTER, M_DRIVERMASTER | Admission/patient (reads) | `TransportServiceImpl.java` |
| 18 | **Equipment Management** | EquipmentServiceImpl, PreventiveMaintenance*, EquipmentServiceHelper | T_EQUIPMENT, T_EQPCOMPLAINT(+RESOLUTION), T_EQPPM*, warranty/insurance/AMC | Inventory (consumables), Finance (AMC cost) | `EquipmentServiceImpl.java`; `models/masters/Equipmentdetail.java` |
| 19 | **Bio-Medical Waste** | BioMedicalWasteManagementServiceImpl | T_BIOMEDICALWASTE(+DETAILS), T_WASTEBAGMASTER | Ward (reads) | `BioMedicalWasteManagementServiceImpl.java` |
| 20 | **Grievance & Feedback** | ComplaintDetailsServiceImpl, ComplaintResolutionDetailServiceImpl, QuetionnaireServiceImpl | T_PATIENTCOMPLAINTS, T_PATIENTGRIEVANCE*, T_COMPLAINTMASTER, T_FEEDBACKTEMPLATE | Patient (reads) | `ComplaintDetailsServiceImpl.java:221-532` |
| 21 | **Reference Master Data** | MasterServiceImpl, MasterCachedServiceImpl (god node), MasterCachedServiceHelper, MdmServiceImpl, AddServiceMasterImpl, ServiceMaster/Tariff/Plan/Package/Company/ItemCategory services | M_* masters (609 entities): service, tariff, unit, department, company, tax, store, bank, ICD10, health factor, patho/rad masters, OT masters, drug masters | **Everyone borrows (cache contract + invalidation events)**; Redis keys owned here | `MasterCachedServiceImpl.java:6411` (checkService cache endpoint); graphify: 514 edges |
| 22 | **Security & IAM** | AuthenticationServiceImpl, UserManagementServiceImpl, AddUserServiceImpl, Encrypter | T_USERDETAILS, T_USERROLES, T_USERROLEMASTER, T_USERUNITDETAILS, T_USERSTOREDETAILS, T_USERWARDDETAILS, M_MENUMASTER, M_STAFFMASTER, M_DOCTORMASTER (profiles) | LDAP (external), all services consume identity tokens | `AuthenticationServiceImpl.java:66-293` |
| 23 | **RCM & Claims** | RcmServiceImpl, NHCXServiceImpl, models/rcm+hcx | T_ELIGIBILITYCHECK, T_ADMISSIONTIDDETAILS, T_NHCXREQUEST(+DETAILS) | Bill/Admission (reads), Payment (events) | `NHCXServiceImpl.java:45-124` |
| 24 | **RMRS & PMRD (Treasury)** | RMRSServiceImpl, PmrdServiceImpl | T_RMRSCOLLECTION, T_AUTHORITYMASTER, T_HEADSMASTER, T_LEDGERMASTER, T_ACCOUNTING* | Payments (consumes PaymentReceived), receipts | `RMRSServiceImpl.java:1361-1366` |
| 25 | **Interop & Notifications (EHR/HL7/ABDM/WhatsApp)** | EHRUtilServiceImpl, EHRSyncJobs, Hl7InboundMessageJob, HL7Helper, AbhaServiceImpl, HPRServiceImpl, ABDMController, IntegrationServiceImpl (whatsapp), SendNotificationServiceImpl | T_OUTBOUNDHIS, T_INBOUNDHIS, T_ABDMREQUESTQUEUE, T_WHATSAPPMESSAGEQUEUE/STATUS, T_EHRJOBDETAILS | Consumes ALL domain events; owns external endpoints | `ApplicationJobs.java:1611,1661-1688`; `HL7Helper.java:246` |
| 26 | **Reporting & Analytics** | ReportServiceImpl, FileManagementServiceImpl, BillInvoiceGenerator, dashboard counts | T_REPORTMASTER, generated PDF/blob store | Reads from all (analytics views/ELT), stats tables | `BillInvoiceGenerator.java:19-25`; `CommonUtilServiceImpl.java:4272-4302` |
| 27 | **Search & Index (Solr pipeline)** | SolrUtil indexing paths, sync jobs | Solr collections only (no tables) | Consumes PatientRegistered/EncounterCreated/ItemUpdated/DiagnosisUpdated events | `SolrUtil.java:174-333`; `ApplicationJobs.java:867-977,2122` |
| 28 | **Govt Programs (EJS / Aasmaan / Chiranjeevi)** | Ejs* services, Aasmaan* services, chiranjeevi | T_EJS_*, T_AASMAAN_*, T_BENEFICIARYENROLMENT | Patient (reads), Incentives -> Finance | `models/ejs` (61), `models/aasmaan` (35), `ApplicationJobs.java:1035` |
| 29 | **Queue & Kiosk** | QueueManagmentServiceImpl, PatientPortalServiceImpl | T_PATIENTQUEUE, T_OPDSTATIONMASTER, T_TABLETMAPPING | Encounter (reads) | `QueueManagmentServiceImpl.java:1477-1759` |

Notes:
- Contexts 4/5/6 (Order/Bill/Payment) and 7/8 (Lab/Rad) share the **same transaction today** (§2.1-2.2) — decomposition requires saga + outbox; boundary between Billing and Payments is drawn at the existing proc split (CIMS_ADDBILL* vs CIMS_ADDPAYMENT*), which is defensible.
- Contexts 9 vs 10: Pharmacy writes stock (CIMS_ADDITEMSTOCK at `PharmacyServiceImpl.java:2140`) while Inventory owns the tables — the classic dual-write hotspot; decision needed: move stock ops to Inventory (API) or split item-stock per store type.
- Context 21 (Reference Master) is the natural home for the Redis cache keys and the Solr service/icd10cm/healthfactor collections.
- Context 25 (Interop) already *is* an async consumer of several tables (queues) — natural Kafka consumer group.

---

## 7. Communication design (preliminary)

### 7.1 Evaluation of candidate Kafka events against legacy side-effects

| Event | Legacy side-effect evidence | Consumers (legacy) | Verdict |
|---|---|---|---|
| PatientRegistered | Solr patient index (`PatientServiceImpl.java:5610`), HL7 registration msg (`HelperServiceImpl.java:221`, `BedReservationServiceImpl.java:180`), ABDM consent/queue, WhatsApp consent (`PatientDetailRegistrationServiceImpl.java:892`) | Search, Interop, ABDM, Notifications | **Event (multi-consumer)** — 4+ independent consumers today |
| PatientAdmitted | HL7 A01 (`HelperServiceImpl.java:307`), Solr encounter index (`SolrUtil.java:245-333`), bed occupancy updates, auto-charge jobs, ABDM | Interop, Search, Billing (auto charges), ABDM | **Event** |
| PatientDischarged | `T_PATIENTDISCHARGEDATA` (`PatientDischargeData.java:31`), final bill settlement, EHR discharge files (`ApplicationJobs.java:1096` creteEHRFiles), Solr | Billing, EHR, Search | **Event** |
| LabOrderCreated | CIMS_ADDPATHORDERBOOKING in *same txn* as bill (`AddServiceOrderServiceHelper.java:473-516`) | Billing (create bill line), Lab worklist, EMR | **Event BUT** — today atomic with billing; needs saga before eventing; strong candidate after split |
| LabResultFinalized | WhatsApp report send (`PathoOrderServiceImpl.java:2851-3183`, `PathoOrderServiceHelper.java:1130-1208`), Solr patient doc update, EHR | Notifications (replaces T_WHATSAPPMESSAGEQUEUE polling), EHR, Search | **Event** — replaces proven DB-poll pattern |
| PrescriptionCreated | PrescriptionMaster saves, ward dispense list (`PharmacyServiceImpl.java:482-550`), billing | Pharmacy (dispense), Billing, Ward | **Event** |
| InvoiceGenerated | Bill create (`CIMS_ADDBILL`), DoctorShare accrual (`:417`), package subscription (`:2526`), HL7 ORM (`:2299-2399`), WhatsApp bill PDF, EMR write (`:2351`) | Payments, Doctor share, RCM, Interop, Notifications, EMR | **Event** (internal multi-consumer; receipt printing stays sync) |
| PaymentReceived | CIMS_ADDPAYMENT+BILLDETAILS+MODEDETAILS+ADVANCEDETAILS (`AddPaymentServiceHelper.java:291-348`), `CIMS_UPDATEBILLFORCONCESSION` (`:463`) | Bill settlement state, RMRS/treasury, RCM claim stage, Notifications | **Event** |
| InventoryUpdated | stock add/update procs (`CIMS_ADDITEMSTOCK.sql`), reorder/auto-indent jobs (`ApplicationJobs.java:2197,2376`), Solr item index (`SolrUtil.java:990`) | Procurement (reorder/indent), Search, EAushadhi sync, Pharmacy | **Event** |

### 7.2 REST vs async guidance
- **Synchronous REST** where a result is needed to continue and side effects are few: patient lookup (Solr-backed search API), tariff/rate lookup, bed availability, login/auth, master-data fetch, bill print data. Justification: legacy calls these inline and needs the value immediately.
- **Kafka (event)**: all rows of §7.1 — where legacy already had DB-queue+poller (WhatsApp, ABDM) or where >1 independent consumer exists (PatientRegistered, InvoiceGenerated).
- **Redis**: keep as cache/lookup + rate-limit/session state; master-data invalidation events published by Reference Master service. Never source of truth.
- **Solr**: owned by Search pipeline service consuming events; readers call its search API.
- **Reports**: synchronous request-response for on-demand reports; nightly/scheduled jobs (IhmsCounts, stats) become streaming consumers.
- **HL7/ABDM/WhatsApp**: consumers of domain events inside Interop service; outbound HL7 keeps an outbox table (PG) with idempotent senders.

### 7.3 Anti-corruption layer & strangler notes
- Phase 1: FAUG rendering decoupled — expose JSON API alongside HTML; frontend moves to SPA/re-render.
- Phase 2: Strangler: new services own new PG schema; monolith remains on Oracle; Interop service mirrors queues and routes events; dual-read/write with reconciliation for T_ENCOUNTER/T_BILL until cutover.
- Stored procedures: prefer (c) hybrid — convert only procs on the extraction path; keep remaining procs on Oracle until their module moves. Ban cross-service DB calls; shared tables become APIs.
- ACID today is per-service-txn; after split, per-request sagas + outbox (PG logical replication of outbox table to Kafka, or Debezium-style).

---

## 8. Migration wave draft (preliminary classification)

Criteria: business criticality, complexity (proc/side-effect coupling), coupling to other modules.

| Wave | Modules (candidate contexts) | Rationale / evidence | Complexity | Coupling |
|---|---|---|---|---|
| **Wave 0 — Foundation** | Reference Master Data (21), Security/IAM (22), Search pipeline (27), Interop/Notifications (25) | God node MasterCachedServiceImpl (514 edges) must be stabilized first; IAM is prerequisite for all; Interop gives event backbone | High (cache contract, LDAP, queues) | Very high (everyone depends on them) |
| **Wave 1 — Admission Front** | Patient (1), Appointment/Encounter (2), ADT/Bed (3), Queue/Kiosk (29) | Self-contained txn (2.3, 2.4), high business criticality, moderate proc footprint; establishes patient/encounter event streams | Medium | Medium (read patient; write Solr/HL7 via events) |
| **Wave 2 — Revenue** | Ordering/Clinical (4), Billing (5), Payments (6), RCM (23) | Highest criticality; must break §2.1 mega-txn into saga — the core decomposition bet | **Very high** | **Very high** (bill+lab+rad+pharmacy+share) |
| **Wave 3 — Clinical Care** | Lab (7), Radiology (8), Pharmacy (9), OT (11), Nursing (12) | Order-execution modules; medium coupling to Billing (via events after Wave 2) | Medium-High | Medium |
| **Wave 3b — Support Clinical** | Blood Bank (13), Diet (14), CSSD (15), BMW (19) | Self-contained domains, low cross-coupling | Medium | Low-Medium |
| **Wave 4 — Facilities & Assets** | Inventory/Procurement (10) — could move earlier if pharmacy split forces it; Equipment (18), Transport (17), Linen (16) | Asset lifecycle, low clinical coupling; Inventory is god-node (250 edges) — needs care | High (Inventory) / Low (rest) | Medium |
| **Wave 5 — External & Govt** | Govt Programs (28), RMRS/PMRD (24), Reporting (26) | Lowest criticality to core care flow; RMRS depends on PaymentReceived stream | Medium | Low (reports read-all) |

**Dependencies:** Wave 2 cannot start before Wave 0 (master/tariff contract) and needs Wave 1 events (encounter/admission IDs). Wave 3 depends on Wave 2 events (LabOrderCreated → billing). Inventory (10) may move to Wave 2 if Pharmacy (9) stock-split decision forces it.

---

## 9. Top decomposition risks (preliminary)

1. **Billing mega-transaction (Order→Bill→Lab→Rad→DoctorShare→EMR→HL7)** — `AddServiceOrderServiceHelper.java:341-516`, `AddServiceOrderServiceImpl.java:2206-2399` — one ACID unit across 4+ future services; requires saga + outbox + compensation design before any boundary is safe to cut.
2. **Master-data god coupling** — `MasterCachedServiceImpl` (514 edges), 609 master entities, Redis cache keys consumed by ~30 services; split risks cache-contract breakage across the whole platform unless Reference Master service + invalidation events land first (hence Wave 0).
3. **Stored-procedure-centric logic + in-txn side effects** — 719 procs + Solr/HL7/ABDM/WhatsApp writes inside Spring transactions (`EmergencyAndWalkinVisitServiceImpl.java:1379-1937`); any service split must convert procs and move side effects to an outbox, else dual-write inconsistency is guaranteed.

---

## 10. Open questions (for discovery continuation)

1. Which modules have true SLA-criticality requiring sync (e.g., billing counter throughput)? (Evidence needed from ops/perf.)
2. Is T_ITEMSTOCK single-table across stores, or per-store partitioning possible? (Affects Pharmacy/Inventory boundary, §6 note.)
3. Which reports are regulatory (GSTR, NHM, RGHS) vs internal? (Determines Reporting service scope & data retention.)
4. Volume of T_ENCOUNTER/T_BILL rows per year — needed for PG partitioning design and event retention.
5. Do government schemes (EJS/Aasmaan/Chiranjeevi) need to be one service or three? (They share only Patient master.)
6. Does the state govt plan to keep Oracle for reporting warehouse while services write PG? (Hybrid-read option.)
7. Confirm audit requirements on T_AUDITEVENT (ServiceLayerAspect audit) — cross-cutting audit service needed?
