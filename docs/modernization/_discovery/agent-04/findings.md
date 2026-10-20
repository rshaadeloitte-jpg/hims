# AGENT-04 Findings - iHMS/OHUM PAS NG Backend API Surface (raw evidence)

Scope: D:\ihms (Spring Boot 2.7.5, Java 8, EclipseLink JPA, Apache CXF JAX-RS). Research-only; no code modified.

## 0. Evidence base (commands used)
```
rg -n "@Path|@GET|@POST|@PUT|@DELETE" src/main/java/com/ohum/services -g *.java   # 6,390 annotation lines
rg -n "@Scheduled" src/main/java -g *.java                                          # 55 schedules, 5 job classes
rg -n "@Transactional" src/main/java -g *.java -c                                   # 1,445 occurrences
rg -n "@Component(" src/main/java/com/ohum -g *.java                                # 96 component beans
Parser: parse_endpoints_tx.ps1 (skips comments/Javadoc, binds verb->@Path->signature, flags @Transactional)
```

## 1. Endpoint count

- **Total endpoints: 3,060** = 3,003 in 132 service classes (GET 1,441 / POST 1,369 / PUT 162 / DELETE 31) + 57 ABDM/HFR callbacks (ABDMController 42, HFRController 15) in com.ohum.xcg.abdm.server.
- **@Transactional endpoints: 1,056** (35.2% of endpoints carry explicit method/class tx).
- Base URL: http://<host>:8080/IHMS/api/rest (App.java:149-150 CXFServlet /api/*; services.xml:93 <jaxrs:server address="/rest">).

## 2. Service registry (services.xml + component scan)

services.xml defines the JAX-RS bus "restServices" (address /rest) with **134 service bean refs** (lines 95-227) and
**65 XML bean definitions** (lines 27-92). Beans not defined in XML are @Component("beanId")-registered (96 total, of which 65 are REST service beans).

### Service classes with endpoints (132) - class-level @Path, endpoint count, verb split

| Service file | Class @Path | Endpoints | GET | POST | PUT | DELETE |
|---|---|---|---|---|---|---|
| `` | `` | 1 | 0 POST=0 PUT=0 DELETE=0 |  |  |  |
| `aasmaan/AasmaanAppTransactionLogServiceImpl.java` | `/app_transaction_log` | 1 | 0 POST=1 PUT=0 DELETE=0 |  |  |  |
| `aasmaan/AasmaanCaseServiceImpl.java` | `/cases` | 44 | 24 POST=19 PUT=1 DELETE=0 |  |  |  |
| `aasmaan/AasmaanDeviceServiceImpl.java` | `/devices` | 8 | 5 POST=0 PUT=2 DELETE=1 |  |  |  |
| `aasmaan/AasmaanETicketServiceImpl.java` | `/e_tickets` | 1 | 0 POST=1 PUT=0 DELETE=0 |  |  |  |
| `aasmaan/AasmaanInstitutionLevelServiceImpl.java` | `/institution_levels` | 2 | 2 POST=0 PUT=0 DELETE=0 |  |  |  |
| `aasmaan/AasmaanInstitutionServiceImpl.java` | `/institutions` | 8 | 6 POST=2 PUT=0 DELETE=0 |  |  |  |
| `aasmaan/AasmaanReportServiceImpl.java` | `` | 2 | 2 POST=0 PUT=0 DELETE=0 |  |  |  |
| `aasmaan/AasmaanRscCallLogServiceImpl.java` | `/rsc_call_logs` | 2 | 1 POST=1 PUT=0 DELETE=0 |  |  |  |
| `aasmaan/AasmaanStatServiceImpl.java` | `/stats` | 4 | 4 POST=0 PUT=0 DELETE=0 |  |  |  |
| `aasmaan/AasmaanUserServiceImpl.java` | `/users` | 6 | 4 POST=1 PUT=1 DELETE=0 |  |  |  |
| `abdm/AbhaServiceImpl.java` | `/abha` | 50 | 33 POST=13 PUT=3 DELETE=1 |  |  |  |
| `abdm/HPRServiceImpl.java` | `/hpr` | 13 | 11 POST=2 PUT=0 DELETE=0 |  |  |  |
| `abdm/NHCXServiceImpl.java` | `/nhcx` | 10 | 3 POST=7 PUT=0 DELETE=0 |  |  |  |
| `AuthenticationServiceImpl.java` | `/auth` | 12 | 0 POST=11 PUT=1 DELETE=0 |  |  |  |
| `BioMedicalWasteManagementServiceImpl.java` | `/bioMedicalWasteManagement` | 14 | 8 POST=6 PUT=0 DELETE=0 |  |  |  |
| `ClinicalServiceImpl.java` | `/clinical` | 4 | 3 POST=1 PUT=0 DELETE=0 |  |  |  |
| `cssd/CSSDServiceImpl.java` | `/cssd` | 46 | 18 POST=27 PUT=1 DELETE=0 |  |  |  |
| `custom/ActionNotificationServiceImpl.java` | `/actionNotif` | 3 | 1 POST=1 PUT=1 DELETE=0 |  |  |  |
| `custom/AddAdvanceServiceImpl.java` | `/addAdvance` | 5 | 3 POST=2 PUT=0 DELETE=0 |  |  |  |
| `custom/AddPaymentServiceImpl.java` | `/addPayment` | 3 | 0 POST=2 PUT=1 DELETE=0 |  |  |  |
| `custom/AddRefundServiceImpl.java` | `/addRefund` | 1 | 0 POST=1 PUT=0 DELETE=0 |  |  |  |
| `custom/AddServiceMasterImpl.java` | `/addServiceMaster` | 6 | 2 POST=2 PUT=1 DELETE=1 |  |  |  |
| `custom/AddServiceOrderServiceImpl.java` | `/serviceOrder` | 49 | 17 POST=27 PUT=2 DELETE=3 |  |  |  |
| `custom/AddUpdateAppointmentServiceImpl.java` | `/updateappointment` | 1 | 0 POST=1 PUT=0 DELETE=0 |  |  |  |
| `custom/AddUserServiceImpl.java` | `/addUserService` | 55 | 32 POST=18 PUT=3 DELETE=2 |  |  |  |
| `custom/AdvanceBillReceiptServiceImpl.java` | `/advancebillReceipt` | 4 | 0 POST=4 PUT=0 DELETE=0 |  |  |  |
| `custom/AllReceiptsServiceImpl.java` | `/allReceipts` | 2 | 0 POST=2 PUT=0 DELETE=0 |  |  |  |
| `custom/AppointmentScheduleServiceImpl.java` | `/appointmentschedule` | 25 | 14 POST=11 PUT=0 DELETE=0 |  |  |  |
| `custom/BedManagementServiceImpl.java` | `/bedmanagement` | 33 | 13 POST=20 PUT=0 DELETE=0 |  |  |  |
| `custom/BillCancellationServiceImpl.java` | `/cancelBill` | 3 | 0 POST=3 PUT=0 DELETE=0 |  |  |  |
| `custom/BillDetServiceImpl.java` | `/billDet` | 1 | 0 POST=1 PUT=0 DELETE=0 |  |  |  |
| `custom/BillReceiptServiceImpl.java` | `/billReceipt` | 19 | 5 POST=14 PUT=0 DELETE=0 |  |  |  |
| `custom/BillSummaryServiceImpl.java` | `/billSummary` | 7 | 0 POST=7 PUT=0 DELETE=0 |  |  |  |
| `custom/CancelAdmissionServiceImpl.java` | `/canceladmission` | 1 | 0 POST=1 PUT=0 DELETE=0 |  |  |  |
| `custom/DoctorListServiceImpl.java` | `/doctorlist` | 2 | 0 POST=2 PUT=0 DELETE=0 |  |  |  |
| `custom/EmergencyAndWalkinVisitServiceImpl.java` | `/walkinVisit` | 45 | 21 POST=20 PUT=4 DELETE=0 |  |  |  |
| `custom/InventoryEAushadiIntegrationImpl.java` | `/eAushadiIndent` | 15 | 2 POST=13 PUT=0 DELETE=0 |  |  |  |
| `custom/InventoryServiceImpl.java` | `/inventoryStockList` | 235 | 90 POST=118 PUT=25 DELETE=2 |  |  |  |
| `custom/InvestigationServiceImpl.java` | `/investigation` | 3 | 0 POST=3 PUT=0 DELETE=0 |  |  |  |
| `custom/MachineConfigServiceImpl.java` | `/machineConfig` | 3 | 0 POST=3 PUT=0 DELETE=0 |  |  |  |
| `custom/MarkAndAddVisitServiceImpl.java` | `/markandaddvisit` | 4 | 0 POST=4 PUT=0 DELETE=0 |  |  |  |
| `custom/MasterCachedServiceImpl.java` | `/master` | 264 | 195 POST=61 PUT=7 DELETE=1 |  |  |  |
| `custom/MdmServiceImpl.java` | `/mdm` | 55 | 18 POST=35 PUT=1 DELETE=1 |  |  |  |
| `custom/MedicationServiceImpl.java` | `/medication` | 23 | 9 POST=11 PUT=3 DELETE=0 |  |  |  |
| `custom/NoteDetailsServiceImpl.java` | `/notedetailsdata` | 6 | 0 POST=5 PUT=1 DELETE=0 |  |  |  |
| `custom/PathoOrderServiceImpl.java` | `/pathoOrder` | 133 | 31 POST=97 PUT=5 DELETE=0 |  |  |  |
| `custom/PatientDetailRegistrationServiceImpl.java` | `/patientDetailReg` | 11 | 2 POST=9 PUT=0 DELETE=0 |  |  |  |
| `custom/PatientRegAdmissionServiceImpl.java` | `/patientRegAddmission` | 23 | 17 POST=5 PUT=0 DELETE=1 |  |  |  |
| `custom/PatientRegApptServiceImpl.java` | `/patientRegAppt` | 22 | 17 POST=5 PUT=0 DELETE=0 |  |  |  |
| `custom/PatientTransferDischargeServiceImpl.java` | `/patientTransferDischarge` | 30 | 5 POST=24 PUT=1 DELETE=0 |  |  |  |
| `custom/PharmacyServiceImpl.java` | `/pharmacy` | 102 | 48 POST=53 PUT=0 DELETE=1 |  |  |  |
| `custom/PrescriptionDetailsServiceImpl.java` | `/prescription` | 1 | 1 POST=0 PUT=0 DELETE=0 |  |  |  |
| `custom/QuetionnaireServiceImpl.java` | `/questionnaireService` | 3 | 2 POST=1 PUT=0 DELETE=0 |  |  |  |
| `custom/RefundBillReceiptServiceImpl.java` | `/refundBillReceipt` | 1 | 0 POST=1 PUT=0 DELETE=0 |  |  |  |
| `custom/UploadFileServiceImpl.java` | `/uploadFile` | 4 | 0 POST=4 PUT=0 DELETE=0 |  |  |  |
| `custom/UploadHelperServiceImpl.java` | `/upload` | 1 | 0 POST=1 PUT=0 DELETE=0 |  |  |  |
| `CWFServiceImpl.java` | `/cwf` | 17 | 8 POST=7 PUT=0 DELETE=2 |  |  |  |
| `ehr/EHRUtilServiceImpl.java` | `/ehr` | 8 | 5 POST=3 PUT=0 DELETE=0 |  |  |  |
| `ejs/EjsAncPncChildServiceImpl.java` | `/ejs-ancpncchild` | 20 | 14 POST=6 PUT=0 DELETE=0 |  |  |  |
| `ejs/EjsAshaServiceImpl.java` | `/ejs-asha` | 8 | 5 POST=3 PUT=0 DELETE=0 |  |  |  |
| `ejs/EjsChecklistsServiceImpl.java` | `/ejs-checklists` | 8 | 6 POST=2 PUT=0 DELETE=0 |  |  |  |
| `ejs/EjsDetailsServiceImpl.java` | `/ejs-details` | 54 | 28 POST=25 PUT=0 DELETE=1 |  |  |  |
| `ejs/EjsIncentiveServiceImpl.java` | `/ejs-incentive` | 6 | 5 POST=1 PUT=0 DELETE=0 |  |  |  |
| `ejs/EjsMasterLocationServiceImpl.java` | `/ejs-masterLocation` | 8 | 8 POST=0 PUT=0 DELETE=0 |  |  |  |
| `ejs/EjsPopulationServiceImpl.java` | `/ejs-population` | 9 | 8 POST=1 PUT=0 DELETE=0 |  |  |  |
| `ejs/EjsSupervisorServiceImpl.java` | `/ejs-supervisor` | 12 | 6 POST=6 PUT=0 DELETE=0 |  |  |  |
| `FileManagementServiceImpl.java` | `/fileService` | 24 | 6 POST=18 PUT=0 DELETE=0 |  |  |  |
| `IntegrationServiceImpl.java` | `/integration` | 22 | 7 POST=15 PUT=0 DELETE=0 |  |  |  |
| `LinenLaundryServiceImpl.java` | `/linenLaundry` | 14 | 7 POST=7 PUT=0 DELETE=0 |  |  |  |
| `nurse/NursingServiceImpl.java` | `/nursingList` | 300 | 154 POST=131 PUT=10 DELETE=5 |  |  |  |
| `pas/AppointmentServiceImpl.java` | `/appointment` | 29 | 13 POST=15 PUT=1 DELETE=0 |  |  |  |
| `pas/BedReservationServiceImpl.java` | `/bedreservation` | 4 | 1 POST=3 PUT=0 DELETE=0 |  |  |  |
| `pas/BedServiceImpl.java` | `/bed` | 2 | 1 POST=1 PUT=0 DELETE=0 |  |  |  |
| `pas/BillServiceImpl.java` | `/bill` | 20 | 11 POST=7 PUT=2 DELETE=0 |  |  |  |
| `pas/BloodBankServiceImpl.java` | `/bloodtestmaster` | 113 | 59 POST=51 PUT=3 DELETE=0 |  |  |  |
| `pas/ChargeServiceImpl.java` | `/charge` | 4 | 1 POST=3 PUT=0 DELETE=0 |  |  |  |
| `pas/CompanyApprovedAmountServiceImpl.java` | `/companyapprovedamount` | 18 | 12 POST=3 PUT=2 DELETE=1 |  |  |  |
| `pas/ComplaintDetailsServiceImpl.java` | `/complaintdetails` | 11 | 6 POST=5 PUT=0 DELETE=0 |  |  |  |
| `pas/ComplaintResolutionDetailServiceImpl.java` | `/complaintresolutiondetail` | 5 | 3 POST=1 PUT=1 DELETE=0 |  |  |  |
| `pas/ConcessionTemplateServiceImpl.java` | `/concessiontemplate` | 4 | 1 POST=1 PUT=2 DELETE=0 |  |  |  |
| `pas/DepartmentScheduleServiceImpl.java` | `/departmentschedule` | 4 | 0 POST=3 PUT=1 DELETE=0 |  |  |  |
| `pas/DietServiceImpl.java` | `/diet` | 30 | 12 POST=13 PUT=5 DELETE=0 |  |  |  |
| `pas/DoctorServiceImpl.java` | `/doctor` | 25 | 9 POST=13 PUT=3 DELETE=0 |  |  |  |
| `pas/DrugServiceImpl.java` | `/drug` | 1 | 0 POST=1 PUT=0 DELETE=0 |  |  |  |
| `pas/EquipmentServiceImpl.java` | `/equipment` | 51 | 21 POST=26 PUT=3 DELETE=1 |  |  |  |
| `pas/ExpiryItemServiceImpl.java` | `/expiryitem` | 47 | 10 POST=29 PUT=8 DELETE=0 |  |  |  |
| `pas/HelperServiceImpl.java` | `/helper` | 3 | 1 POST=2 PUT=0 DELETE=0 |  |  |  |
| `pas/IPDAdmissionServiceImpl.java` | `/ipdadmission` | 18 | 8 POST=7 PUT=1 DELETE=2 |  |  |  |
| `pas/IPDConfigServiceImpl.java` | `/ipdconfig` | 35 | 7 POST=18 PUT=8 DELETE=2 |  |  |  |
| `pas/IPDServiceImpl.java` | `/ipd` | 2 | 0 POST=2 PUT=0 DELETE=0 |  |  |  |
| `pas/ItemCategoryServiceImpl.java` | `/itemcategory` | 1 | 1 POST=0 PUT=0 DELETE=0 |  |  |  |
| `pas/ItemMasterServiceImpl.java` | `/itemmaster` | 23 | 12 POST=9 PUT=2 DELETE=0 |  |  |  |
| `pas/ItemSalesServiceImpl.java` | `/itemsales` | 15 | 4 POST=11 PUT=0 DELETE=0 |  |  |  |
| `pas/ItemStockServiceImpl.java` | `/itemstock` | 8 | 4 POST=4 PUT=0 DELETE=0 |  |  |  |
| `pas/MasterServiceImpl.java` | `/masterConfigService` | 72 | 42 POST=20 PUT=10 DELETE=0 |  |  |  |
| `pas/MaterialConsumptionServiceImpl.java` | `/materialconsumption` | 3 | 1 POST=2 PUT=0 DELETE=0 |  |  |  |
| `pas/NursingStationDefinationServiceImpl.java` | `/nursingstationdefination` | 2 | 1 POST=1 PUT=0 DELETE=0 |  |  |  |
| `pas/OpeningBalanceServiceImpl.java` | `/openingbalance` | 5 | 0 POST=5 PUT=0 DELETE=0 |  |  |  |
| `pas/OrganismMasterServiceImpl.java` | `/organismmaster` | 2 | 0 POST=2 PUT=0 DELETE=0 |  |  |  |
| `pas/OtConfigServiceImpl.java` | `/otconfig` | 33 | 19 POST=9 PUT=5 DELETE=0 |  |  |  |
| `pas/OTDetailsServiceImpl.java` | `/otdetails` | 3 | 3 POST=0 PUT=0 DELETE=0 |  |  |  |
| `pas/OTSchedulerServiceImpl.java` | `/otscheduler` | 80 | 38 POST=32 PUT=10 DELETE=0 |  |  |  |
| `pas/PackageServiceServiceImpl.java` | `/packageservice` | 22 | 12 POST=5 PUT=5 DELETE=0 |  |  |  |
| `pas/PatientBillEstimationServiceImpl.java` | `/patientbillestimation` | 4 | 1 POST=3 PUT=0 DELETE=0 |  |  |  |
| `pas/PatientRequisitionServiceImpl.java` | `/patientrequisition` | 10 | 3 POST=6 PUT=1 DELETE=0 |  |  |  |
| `pas/PatientServiceImpl.java` | `/patient` | 94 | 50 POST=42 PUT=1 DELETE=1 |  |  |  |
| `pas/PaymentServiceImpl.java` | `/payment` | 5 | 1 POST=2 PUT=2 DELETE=0 |  |  |  |
| `pas/PctsServiceImpl.java` | `/pcts` | 5 | 1 POST=3 PUT=1 DELETE=0 |  |  |  |
| `pas/PlanMasterServiceImpl.java` | `/planmaster` | 3 | 0 POST=1 PUT=2 DELETE=0 |  |  |  |
| `pas/PmrdServiceImpl.java` | `/pmrd` | 4 | 4 POST=0 PUT=0 DELETE=0 |  |  |  |
| `pas/PrescriptionMasterServiceImpl.java` | `/prescriptionmaster` | 5 | 3 POST=2 PUT=0 DELETE=0 |  |  |  |
| `pas/PreventiveMaintenanceScheduleServiceImpl.java` | `/preventivemaintenanceschedule` | 1 | 0 POST=1 PUT=0 DELETE=0 |  |  |  |
| `pas/PreventiveMaintenanceServiceImpl.java` | `/preventivemaintenance` | 20 | 10 POST=10 PUT=0 DELETE=0 |  |  |  |
| `pas/PurchaseOrderServiceImpl.java` | `/purchaseorder` | 1 | 1 POST=0 PUT=0 DELETE=0 |  |  |  |
| `pas/QuotationServiceImpl.java` | `/quotation` | 7 | 1 POST=6 PUT=0 DELETE=0 |  |  |  |
| `pas/RadiologyServiceImpl.java` | `/radiology` | 10 | 5 POST=3 PUT=2 DELETE=0 |  |  |  |
| `pas/RefEntityServiceImpl.java` | `/refentity` | 3 | 2 POST=1 PUT=0 DELETE=0 |  |  |  |
| `pas/RefundServiceImpl.java` | `/refund` | 7 | 0 POST=5 PUT=1 DELETE=1 |  |  |  |
| `pas/RMRSServiceImpl.java` | `/RMRS` | 40 | 32 POST=6 PUT=2 DELETE=0 |  |  |  |
| `pas/ServiceMasterServiceImpl.java` | `/servicemaster` | 12 | 6 POST=6 PUT=0 DELETE=0 |  |  |  |
| `pas/SurgeryDetailsServiceImpl.java` | `/surgerydetails` | 6 | 3 POST=2 PUT=1 DELETE=0 |  |  |  |
| `pas/TariffServiceImpl.java` | `/tariff` | 5 | 2 POST=2 PUT=1 DELETE=0 |  |  |  |
| `pas/TransportServiceImpl.java` | `/transport` | 31 | 12 POST=19 PUT=0 DELETE=0 |  |  |  |
| `pas/UserManagementServiceImpl.java` | `/usermanagement` | 18 | 13 POST=5 PUT=0 DELETE=0 |  |  |  |
| `pas/VisitServiceImpl.java` | `/visit` | 12 | 6 POST=6 PUT=0 DELETE=0 |  |  |  |
| `patp/PatientHistoryServiceImpl.java` | `/patientHistory` | 1 | 1 POST=0 PUT=0 DELETE=0 |  |  |  |
| `patp/PatientPortalServiceImpl.java` | `/patientPortalService` | 22 | 7 POST=14 PUT=0 DELETE=1 |  |  |  |
| `payment/PaymentTransferServiceImpl.java` | `/paymentTransfer` | 6 | 3 POST=3 PUT=0 DELETE=0 |  |  |  |
| `QueueManagmentServiceImpl.java` | `/queueManagment` | 23 | 18 POST=3 PUT=2 DELETE=0 |  |  |  |
| `RcmServiceImpl.java` | `/rcm` | 7 | 6 POST=1 PUT=0 DELETE=0 |  |  |  |
| `SendNotificationServiceImpl.java` | `/sendEmail` | 5 | 1 POST=4 PUT=0 DELETE=0 |  |  |  |

## 3. FULL ENDPOINT DUMP (services/, 3,003 endpoints) (raw, evidence-first)

Format: file<TAB>classPath<TAB>verb<TAB>methodPath<TAB>tx<TAB>signature

```
aasmaan\AasmaanAppTransactionLogServiceImpl.java	/app_transaction_log	POST	/	0	public AasmaanResponseDTO<Object> saveAppTransactionLog() 
aasmaan\AasmaanCaseServiceImpl.java	/cases	GET	/	0	public AasmaanResponseDTO<AasmaanCase> getCases(@QueryParam("adm_time") String admTime, @QueryParam("ipd_no") String ipdNo, @QueryParam("length") int length, @QueryParam("page") in
aasmaan\AasmaanCaseServiceImpl.java	/cases	GET	/{case_id}	1	public AasmaanResponseDTO<AasmaanCase> getCase(@PathParam("case_id") String caseId) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	GET	/{case_id}/administration_drugs	1	public AasmaanResponseDTO<AasmaanCaseAdministrationDrug> getCaseAdministrationDrugs(@QueryParam("pres_date") String presDate, @PathParam("case_id") String caseId) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	GET	/{case_id}/checklist1	0	public AasmaanResponseDTO<AasmaanCaseChecklist1> getCaseChecklist1(@PathParam("case_id") String caseId) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	GET	/{case_id}/checklist2	1	public AasmaanResponseDTO<AasmaanCaseChecklist2> getCaseChecklist2(@PathParam("case_id") String caseId) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	GET	/{case_id}/checklist3	1	public AasmaanResponseDTO<AasmaanCaseChecklist3> getCaseChecklist3(@PathParam("case_id") String caseId) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	GET	/{case_id}/checklist4	1	public AasmaanResponseDTO<AasmaanCaseChecklist4> getCaseChecklist4(@PathParam("case_id") String caseId) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	GET	/{case_id}/delivery_notes	1	public AasmaanResponseDTO<AasmaanCaseDeliveryNote> getCaseDeliveryNotes(@PathParam("case_id") String caseId) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	GET	/{case_id}/discharge_notes	1	public AasmaanResponseDTO<AasmaanCaseDischargeNote> getCaseDischargeNotes(@PathParam("case_id") String caseId) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	GET	/{case_id}/events	1	public AasmaanResponseDTO<AasmaanCaseEvent> getCaseEvents(@PathParam("case_id") String caseId) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	GET	/{case_id}/export	1	public AasmaanResponseDTO<Object> exportCase(@PathParam("case_id") String caseId, @QueryParam("lang") String lang, @QueryParam("type") String type) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	GET	/{case_id}/field_monitoring	1	public AasmaanResponseDTO<AasmaanCaseFieldMonitoring> getCaseFieldMonitoring(@PathParam("case_id") String caseId, @QueryParam("cat_code") String catCode) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	GET	/{case_id}/highrisk	0	public AasmaanResponseDTO<Object> getCaseHighrisk(@PathParam("case_id") String caseId) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	GET	/{case_id}/info/{parameter}	0	public AasmaanResponseDTO<Object> getCaseInfoParameter(@PathParam("parameter") String parameter, @PathParam("case_id") String caseId) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	GET	/{case_id}/io_forms	1	public AasmaanResponseDTO<AasmaanCaseIoForm> getCaseIoForms(@PathParam("case_id") String caseId) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	GET	/{case_id}/monitoring	0	public AasmaanResponseDTO<AasmaanCaseMonitoring> getCaseMonitoring(@PathParam("case_id") String caseId, @QueryParam("active_status") String activeStatus, @QueryParam("vital_code") 
aasmaan\AasmaanCaseServiceImpl.java	/cases	GET	/{case_id}/pd_monitoring	1	public AasmaanResponseDTO<AasmaanCasePdMonitoring> getCasePdMonitoring(@PathParam("case_id") String caseId) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	GET	/{case_id}/pnc_monitoring	1	public AasmaanResponseDTO<AasmaanCasePncMonitoring> getCasePncMonitoring(@PathParam("case_id") String caseId) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	GET	/{case_id}/prescribed_drugs	1	public AasmaanResponseDTO<AasmaanCasePrescribedDrug> getCasePrescribedDrugs(@QueryParam("pres_date") String presDate, @PathParam("case_id") String caseId, @QueryParam("drug_of") St
aasmaan\AasmaanCaseServiceImpl.java	/cases	GET	/{case_id}/referral_slip	0	public AasmaanResponseDTO<AasmaanCaseReferralSlip> getCaseReferralSlip(@PathParam("case_id") String caseId) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	GET	/{case_id}/schedule	1	public AasmaanResponseDTO<AasmaanCaseSchedule> getCaseSchedule(@PathParam("case_id") String caseId, @QueryParam("vital_code") String vitalCode) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	GET	/cmfs	1	public AasmaanResponseDTO<AasmaanCaseCmfs> getCaseCmfs(@QueryParam("institution_id") String institutionId, @QueryParam("asman_code") String asmanCode) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	GET	/is_unique_ipd	0	public AasmaanResponseDTO<Object> getCaseIsUniqueIpd(@QueryParam("case_id") String caseId, @QueryParam("ipd_no") String ipdNo, @QueryParam("institution_id") int institutionId) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	GET	/stage_counts	1	public AasmaanResponseDTO<AasmaanCaseStageCount> getCaseStageCounts(@QueryParam("case_name") String caseName, @QueryParam("adm_time") String admTime, @QueryParam("ipd_no") String i
aasmaan\AasmaanCaseServiceImpl.java	/cases	POST	/	0	public AasmaanResponseDTO<AasmaanCase> saveCase(@BodyParam("body") AasmaanCase body) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	POST	/{case_id}/administration_drugs	0	public AasmaanResponseDTO<AasmaanCaseAdministrationDrug> saveCaseAdministrationDrug(@BodyParam("body") List<AasmaanCaseAdministrationDrug> body, @PathParam("case_id") String caseId
aasmaan\AasmaanCaseServiceImpl.java	/cases	POST	/{case_id}/checklist1	0	public AasmaanResponseDTO<AasmaanCaseChecklist1> saveCaseChecklist1(@PathParam("case_id") String caseId, @BodyParam("body") AasmaanCaseChecklist1 body) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	POST	/{case_id}/checklist2	0	public AasmaanResponseDTO<AasmaanCaseChecklist2> saveCaseChecklist2(@PathParam("case_id") String caseId, @BodyParam("body") AasmaanCaseChecklist2 body) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	POST	/{case_id}/checklist3	0	public AasmaanResponseDTO<AasmaanCaseChecklist3> saveCaseChecklist3(@BodyParam("body") AasmaanCaseChecklist3 body, @PathParam("case_id") String caseId) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	POST	/{case_id}/checklist4	0	public AasmaanResponseDTO<AasmaanCaseChecklist4> saveCaseChecklist4(@BodyParam("body") AasmaanCaseChecklist4 body, @PathParam("case_id") String caseId) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	POST	/{case_id}/delivery_notes	0	public AasmaanResponseDTO<AasmaanCaseDeliveryNote> saveCaseDeliveryNote(@PathParam("case_id") String caseId, @BodyParam("body") AasmaanCaseDeliveryNote body) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	POST	/{case_id}/discharge_notes	0	public AasmaanResponseDTO<AasmaanCaseDischargeNote> saveCaseDischargeNote(@BodyParam("body") AasmaanCaseDischargeNote body, @PathParam("case_id") String caseId) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	POST	/{case_id}/field_monitoring	0	public AasmaanResponseDTO<AasmaanCaseFieldMonitoring> saveCaseFieldMonitoring(@PathParam("case_id") String caseId, @BodyParam("body") List<AasmaanCaseFieldMonitoring> body) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	POST	/{case_id}/finish_admission	1	public AasmaanResponseDTO<AasmaanCaseFinishAdmission> saveCaseFinishAdmission(@PathParam("case_id") String caseId, @BodyParam("body") AasmaanCaseFinishAdmission body) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	POST	/{case_id}/highrisk	0	public AasmaanResponseDTO<AasmaanCaseHighrisk> saveCaseHighrisk(@PathParam("case_id") String caseId, @BodyParam("body") AasmaanCaseHighrisk body) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	POST	/{case_id}/io_forms	0	public AasmaanResponseDTO<AasmaanCaseIoForm> saveCaseIoForm(@PathParam("case_id") String caseId, @BodyParam("body") List<AasmaanCaseIoForm> body) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	POST	/{case_id}/monitoring	0	public AasmaanResponseDTO<AasmaanCaseMonitoring> saveCaseMonitoring(@BodyParam("body") List<AasmaanCaseMonitoring> body, @PathParam("case_id") String caseId) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	POST	/{case_id}/pd_monitoring	0	public AasmaanResponseDTO<AasmaanCasePdMonitoring> saveCasePdMonitoring(@PathParam("case_id") String caseId, @BodyParam("body") List<AasmaanCasePdMonitoring> body) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	POST	/{case_id}/pnc_monitoring	0	public AasmaanResponseDTO<AasmaanCasePncMonitoring> saveCasePncMonitoring(@BodyParam("body") List<AasmaanCasePncMonitoring> body, @PathParam("case_id") String caseId) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	POST	/{case_id}/prescribed_drugs	0	public AasmaanResponseDTO<AasmaanCasePrescribedDrug> saveCasePrescribedDrug(@PathParam("case_id") String caseId, @BodyParam("body") List<AasmaanCasePrescribedDrug> body) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	POST	/{case_id}/referral_cancel	0	public AasmaanResponseDTO<AasmaanCaseReferralCancel> saveCaseReferralCancel(@PathParam("case_id") String caseId, @BodyParam("body") AasmaanCaseReferralCancel body) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	POST	/{case_id}/referral_finish	1	public AasmaanResponseDTO<AasmaanCaseReferralFinish> saveCaseReferralFinish(@BodyParam("body") AasmaanCaseReferralFinish body, @PathParam("case_id") String caseId) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	POST	/{case_id}/referral_slip	0	public AasmaanResponseDTO<AasmaanCaseReferralSlip> saveCaseReferralSlip(@BodyParam("body") AasmaanCaseReferralSlip body, @PathParam("case_id") String caseId) 
aasmaan\AasmaanCaseServiceImpl.java	/cases	PUT	/{case_id}	0	public AasmaanResponseDTO<Object> updateCase(@PathParam("case_id") String caseId) 
aasmaan\AasmaanDeviceServiceImpl.java	/devices	DELETE	/{deviceId}	0	public void deleteDevice(@PathParam("deviceId") String deviceid) 
aasmaan\AasmaanDeviceServiceImpl.java	/devices	GET	/	0	public AasmaanResponseDTO<AasmaanDevice> getDevices() 
aasmaan\AasmaanDeviceServiceImpl.java	/devices	GET	/{deviceId}	1	public AasmaanResponseDTO<AasmaanDevice> getDevice(@PathParam("deviceId") String deviceid) 
aasmaan\AasmaanDeviceServiceImpl.java	/devices	GET	/{deviceId}/assets	0	public AasmaanResponseDTO<Object> getDeviceAssets(@PathParam("deviceId") String deviceid) 
aasmaan\AasmaanDeviceServiceImpl.java	/devices	GET	/{deviceId}/assets/{assetVersion}	0	public AasmaanResponseDTO<Object> getDeviceAssetsByVersion(@PathParam("assetVersion") String assetversion, @PathParam("deviceId") String deviceid) 
aasmaan\AasmaanDeviceServiceImpl.java	/devices	GET	/{deviceId}/authenticate	1	public AasmaanResponseDTO<Object> authenticateDevice(@PathParam("deviceId") String deviceid) 
aasmaan\AasmaanDeviceServiceImpl.java	/devices	PUT	/{deviceId}	0	public AasmaanResponseDTO<AasmaanDevice> updateDevice(@BodyParam("body") AasmaanDevice body, @PathParam("deviceId") String deviceid) 
aasmaan\AasmaanDeviceServiceImpl.java	/devices	PUT	/{deviceId}/assets/{assetVersion}	0	public AasmaanResponseDTO<Object> updateDeviceAssetsByVersion(@PathParam("assetVersion") String assetversion, @PathParam("deviceId") String deviceid) 
aasmaan\AasmaanETicketServiceImpl.java	/e_tickets	POST	/	0	public AasmaanResponseDTO<AasmaanETicket> saveETicket(@BodyParam("body") AasmaanETicket body) 
aasmaan\AasmaanInstitutionLevelServiceImpl.java	/institution_levels	GET	/	0	public AasmaanResponseDTO<AasmaanInstitutionLevel> getInstitutionLevels() 
aasmaan\AasmaanInstitutionLevelServiceImpl.java	/institution_levels	GET	/{institution_level_id}/institutions	0	public AasmaanResponseDTO<AasmaanInstitutionLevelInstitution> getInstitutionLevelInstitutions(@PathParam("institution_level_id") int institutionLevelId) 
aasmaan\AasmaanInstitutionServiceImpl.java	/institutions	GET	/	0	public AasmaanResponseDTO<AasmaanInstitution> getInstitutions() 
aasmaan\AasmaanInstitutionServiceImpl.java	/institutions	GET	/{institution_id}	0	public AasmaanResponseDTO<AasmaanInstitution> getInstitution(@PathParam("institution_id") int institutionId) 
aasmaan\AasmaanInstitutionServiceImpl.java	/institutions	GET	/{institution_id}/referrals	1	public AasmaanResponseDTO<AasmaanInstitutionReferral> getInstitutionReferrals(@PathParam("institution_id") int institutionId, @PathParam("device_code") int deviceCode, @QueryParam(
aasmaan\AasmaanInstitutionServiceImpl.java	/institutions	GET	/{institution_id}/referrals/{id}	0	public AasmaanResponseDTO<AasmaanInstitutionReferral> getInstitutionReferral(@PathParam("institution_id") int institutionId, @PathParam("id") int id) 
aasmaan\AasmaanInstitutionServiceImpl.java	/institutions	GET	/{institution_id}/referrals/{id}/export	0	public AasmaanResponseDTO<Object> exportInstitutionReferral(@PathParam("institution_id") String institutionId, @PathParam("id") String id) 
aasmaan\AasmaanInstitutionServiceImpl.java	/institutions	GET	/{institution_id}/shifts	0	public AasmaanResponseDTO<AasmaanInstitutionShift> getInstitutionShifts(@PathParam("institution_id") int institutionId) 
aasmaan\AasmaanInstitutionServiceImpl.java	/institutions	POST	/{institution_id}/feedback	1	public AasmaanResponseDTO<AasmaanInstitutionFeedback> saveInstitutionFeedback(@PathParam("institution_id") int institutionId, @BodyParam("body") AasmaanInstitutionFeedback body) 
aasmaan\AasmaanInstitutionServiceImpl.java	/institutions	POST	/{institution_id}/shifts	0	public AasmaanResponseDTO<AasmaanInstitutionShift> saveInstitutionShift(@PathParam("institution_id") int institutionId, @BodyParam("body") AasmaanInstitutionShift body) 
aasmaan\AasmaanReportServiceImpl.java		GET	/reports/{report_id}	0	public AasmaanResponseDTO<AasmaanReport> getReport(@QueryParam("area_id") int areaId, @QueryParam("institution_id") int institutionId, @QueryParam("frequency") String frequency, @Q
aasmaan\AasmaanReportServiceImpl.java		GET	/reports/{report_id}/export	0	public AasmaanResponseDTO<Object> exportReport(@QueryParam("area_id") int areaId, @QueryParam("institution_id") int institutionId, @QueryParam("frequency") String frequency, @PathP
aasmaan\AasmaanRscCallLogServiceImpl.java	/rsc_call_logs	GET	/	0	public AasmaanResponseDTO<AasmaanRscCallLog> getRscCallLogs(@QueryParam("from_institution_id") int fromInstitutionId, @QueryParam("case_id") String caseId, @QueryParam("to_area_id"
aasmaan\AasmaanRscCallLogServiceImpl.java	/rsc_call_logs	POST	/	0	public AasmaanResponseDTO<Object> saveRscCallLog() 
aasmaan\AasmaanStatServiceImpl.java	/stats	GET	/	0	public AasmaanResponseDTO<AasmaanStat> getStats(@QueryParam("institute_id") String instituteId, @QueryParam("area_id") String areaId) 
aasmaan\AasmaanStatServiceImpl.java	/stats	GET	/{variable_name}	0	public AasmaanResponseDTO<Object> getStatVariableName(@QueryParam("institute_id") String instituteId, @QueryParam("area_id") String areaId, @PathParam("variable_name") String varia
aasmaan\AasmaanStatServiceImpl.java	/stats	GET	/areas/{areaId}	0	public AasmaanResponseDTO<AasmaanStat> getStatArea(@PathParam("areaId") String areaid) 
aasmaan\AasmaanStatServiceImpl.java	/stats	GET	/institutes/{instituteId}	0	public AasmaanResponseDTO<AasmaanStat> getStatInstitutes(@PathParam("instituteId") String instituteid) 
aasmaan\AasmaanUserServiceImpl.java	/users	GET	/	0	public AasmaanResponseDTO<AasmaanUser> getUsers(@QueryParam("area_id") String areaId, @QueryParam("institution_id") String institutionId) 
aasmaan\AasmaanUserServiceImpl.java	/users	GET	/{userId}	1	public AasmaanResponseDTO<AasmaanUser> getUser(@PathParam("userId") String userid) 
aasmaan\AasmaanUserServiceImpl.java	/users	GET	/{userId}/authenticate	1	public AasmaanResponseDTO<Object> authenticateUser(@PathParam("userId") String userid) 
aasmaan\AasmaanUserServiceImpl.java	/users	GET	/authenticate	0	public AasmaanResponseDTO<Object> authenticateUser(@QueryParam("username") String username, @QueryParam("password") String password) 
aasmaan\AasmaanUserServiceImpl.java	/users	POST	/	0	public AasmaanResponseDTO<AasmaanUser> saveUser(@BodyParam("body") List<AasmaanUser> body) 
aasmaan\AasmaanUserServiceImpl.java	/users	PUT	/{userId}	0	public AasmaanResponseDTO<Object> updateUser(@PathParam("userId") String userid) 
abdm\AbhaServiceImpl.java	/abha	DELETE	/consent/{id}	0	public void removeConsent(@PathParam("id") Long id) throws Exception 
abdm\AbhaServiceImpl.java	/abha	GET	/abhaLinkProfile	0	public String linkProfile(@QueryParam("txnId") String txnId) throws GenericApplicationException 
abdm\AbhaServiceImpl.java	/abha	GET	/allPhrAddresses	1	public List<String> allPhrAddresses(@QueryParam("healthIdNumber") String healthIdNumber) throws GenericApplicationException, InterruptedException 
abdm\AbhaServiceImpl.java	/abha	GET	/authenticateLink	0	public UserDTO authenticateLink(@QueryParam("reqId") String reqId, @QueryParam("authMode") String authMode, @QueryParam("healthId") String healthId) throws GenericApplicationExcept
abdm\AbhaServiceImpl.java	/abha	GET	/baalAbhaReKycVerifyOtp	0	public List<PatientAbhaSearchDto> baalAbhaReKycVerifyOtp(@QueryParam("txnId") String txnId, @QueryParam("otp") String otp, @QueryParam("patToken") String patToken) throws GenericAp
abdm\AbhaServiceImpl.java	/abha	GET	/capturePIDForFaceAuth	0	public CapturePidForFaceAuthResponse capturePIDForFaceAuth(@QueryParam("txnId") String txnId) throws GenericApplicationException 
abdm\AbhaServiceImpl.java	/abha	GET	/checkAbhaAddressExist	0	public Boolean checkAbhaAddressExist(@QueryParam("phrAddress") String phrAddress) throws GenericApplicationException 
abdm\AbhaServiceImpl.java	/abha	GET	/consentRequestData	0	public Map<String, List<Object[]>> getConsentRequestData(@QueryParam("concentId") Long concentId) throws GenericApplicationException, InterruptedException 
abdm\AbhaServiceImpl.java	/abha	GET	/consentRequestDataBundle	0	public PatientAbdmCRData getConsentRequestDataBundle(@QueryParam("consentDetId") Long consentDetId,
abdm\AbhaServiceImpl.java	/abha	GET	/consentRequestList	1	public List<PatientAbdmCR> getRaiseConsentRequestList(@QueryParam("page") Integer page, @QueryParam("count") Integer count, @QueryParam("search") String search) throws GenericAppli
abdm\AbhaServiceImpl.java	/abha	GET	/createTestBundle	0	public List<String> createTestBundle(@QueryParam("patientId") Long patientId, @QueryParam("opdIpdId") Long opdIpdId, @QueryParam("opdIpd") Long opdIpd, @QueryParam("unitId") Long u
abdm\AbhaServiceImpl.java	/abha	GET	/encrollByfaceAuthInit	0	public String encrollByfaceAuthInit() throws GenericApplicationException 
abdm\AbhaServiceImpl.java	/abha	GET	/generateLookupOtp	0	public String[] generateABHALookupOtp(@QueryParam("id") String id, @QueryParam("authMode") String authMode, @QueryParam("type") String type) throws GenericApplicationException 
abdm\AbhaServiceImpl.java	/abha	GET	/generateOtp	0	public String[] generateOtp(@QueryParam("type") String type, @QueryParam("txnId") String txnId, @QueryParam("typeText") String typeText) throws GenericApplicationException 
abdm\AbhaServiceImpl.java	/abha	GET	/getAbhaPatientQueueCount	1	public long getAbhaPatientQueueCount(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
abdm\AbhaServiceImpl.java	/abha	GET	/getAbhaSuggestions	0	public EnrollSuggestionsResponse getAbhaSuggestions(@QueryParam("txnId") String txnId) throws GenericApplicationException 
abdm\AbhaServiceImpl.java	/abha	GET	/getAuthModes	1	public List<AuthenticationMode> getAuthModes(@QueryParam("reqId") String reqId) throws GenericApplicationException 
abdm\AbhaServiceImpl.java	/abha	GET	/getCard	0	public byte[] getCard(@QueryParam("patToken") String patToken, @QueryParam("healthIdNumber") String healthIdNumber, @QueryParam("isCreateAbha") Boolean isCreateAbha) throws Generic
abdm\AbhaServiceImpl.java	/abha	GET	/getCareContextsEncounters/{patientId}	0	public PatientRepresentation getCareContextsEncounters(@PathParam("patientId") Long patientId, @QueryParam("unitId") Long unitId, @QueryParam("fromData") Date fromDate, @QueryParam
abdm\AbhaServiceImpl.java	/abha	GET	/getChildProfile	0	public UserDTO getChildProfile(@QueryParam("patToken") String patToken) throws GenericApplicationException 
abdm\AbhaServiceImpl.java	/abha	GET	/getPatientEncountersByHealthId	0	public Long getPatientByHealthId(@QueryParam("healthId") String healthId, @QueryParam("unitId") Long unitId) throws Exception 
abdm\AbhaServiceImpl.java	/abha	GET	/getProfile	0	public UserDTO getProfile(@QueryParam("patToken") String patToken, @QueryParam("searchHealthId") String searchHealthId, @QueryParam("isCreateAbha") Boolean isCreateAbha, @QueryPara
abdm\AbhaServiceImpl.java	/abha	GET	/getProfileCard	1	public byte[] getProfileCard(@QueryParam("healthId") String healthId) throws GenericApplicationException 
abdm\AbhaServiceImpl.java	/abha	GET	/getRegisteredAbhaToken	0	public String getRegisteredAbhaToken(@QueryParam("healthIdNumber") String healthIdNumber) throws GenericApplicationException 
abdm\AbhaServiceImpl.java	/abha	GET	/linkHealthId	0	public String linkHealthId(@QueryParam("healthId") String healthId, @QueryParam("authMode") String authMode, @QueryParam("reqId") String reqId)
abdm\AbhaServiceImpl.java	/abha	GET	/patientQueue	1	public List<PatientAbdm> getPatientQueue(@QueryParam("unitId") Long unitId, @QueryParam("page") Integer page, @QueryParam("count") Integer count, @QueryParam("search") String searc
abdm\AbhaServiceImpl.java	/abha	GET	/resendLoginOtp	0	public String[] resendLoginOtp(@QueryParam("type") String type, @QueryParam("loginHint") String loginHint, @QueryParam("typeText") String typeText) throws GenericApplicationExcepti
abdm\AbhaServiceImpl.java	/abha	GET	/resendOtp	0	public String[] resendOtp(@QueryParam("type") String type, @QueryParam("txnId") String txnId, @QueryParam("typeText") String typeText) throws GenericApplicationException 
abdm\AbhaServiceImpl.java	/abha	GET	/startABHADeletion	0	public String startABHADeletionOrDeactivation(@QueryParam("patTokenId") String patTokenId, @QueryParam("authMode") String authMode) throws GenericApplicationException 
abdm\AbhaServiceImpl.java	/abha	GET	/startLookup	0	public String[] searchAbhaNumberOrId(@QueryParam("id") String id, @QueryParam("authMode") String authMode, @QueryParam("searchType") String searchType) throws GenericApplicationExc
abdm\AbhaServiceImpl.java	/abha	GET	/verifylinkHealthOTP	0	public UserDTO verifylinkHealthOTP(@QueryParam("reqId") String authReqId, @QueryParam("code") String code, @QueryParam("healthId") String healthId) throws GenericApplicationExcepti
abdm\AbhaServiceImpl.java	/abha	GET	/verifyLookupOTP	0	public List<PatientAbhaSearchDto> confirmAbha(@QueryParam("authMode") String authMode, @QueryParam("txnId") String txnId, @QueryParam("otp") String otp, @QueryParam("verifyType") S
abdm\AbhaServiceImpl.java	/abha	GET	/verifyOtp	0	public String[] verifyOtp(@QueryParam("type") String type, @QueryParam("txnId") String txnId, @QueryParam("otp") String otp, @QueryParam("mobileNo") String mobileNo) throws Generic
abdm\AbhaServiceImpl.java	/abha	GET	/verifyPatientShareToken	1	public UserDTO verifyPatientShareToken(@QueryParam("patientAbdmId") Long patientAbdmId, @QueryParam("otp") String otp) throws GenericApplicationException 
abdm\AbhaServiceImpl.java	/abha	POST	/baalAbhaReKycRequestOtp	0	public String[] baalAbhaReKycRequestOtp(@QueryParam("patToken") String patToken, @QueryParam("loginId") String loginId) throws GenericApplicationException 
abdm\AbhaServiceImpl.java	/abha	POST	/createPhrAddress	0	public String createPhrAddress(@QueryParam("txnId") String txnId, @QueryParam("phrAddress") String phrAddress) throws Exception 
abdm\AbhaServiceImpl.java	/abha	POST	/encrollByfaceAuth	0	public String[] abhaErollByFaceAuth(@BodyParam("body") AbhaEnrollByFaceAuthRequest body, @QueryParam("isUser") @DefaultValue("false") Boolean isUser) throws GenericApplicationExcep
abdm\AbhaServiceImpl.java	/abha	POST	/findPatient	1	public List<SearchPatientGeneralDetailsResultDto> findPatient(@BodyParam("body") PatientGeneralDetailsListForSearchDto body) throws GenericApplicationException, InterruptedExceptio
abdm\AbhaServiceImpl.java	/abha	POST	/linkAbdmFacility	0	public Boolean linkAbdmFacility(@QueryParam("facilityName") String facilityName, @QueryParam("hfrId") String hfrId, @QueryParam("unitId") Long unitId) throws GenericApplicationExce
abdm\AbhaServiceImpl.java	/abha	POST	/linkHipAPI	0	public void linkHipAPI() throws GenericApplicationException 
abdm\AbhaServiceImpl.java	/abha	POST	/loginVerifyUser	0	public String loginVerifyUser(@BodyParam("body") PatientAbhaSearchDto body) throws GenericApplicationException 
abdm\AbhaServiceImpl.java	/abha	POST	/raiseConsentRequest	0	public String raiseConsentRequest(@BodyParam("body") PatientAbdmCR body) throws GenericApplicationException 
abdm\AbhaServiceImpl.java	/abha	POST	/registerABHAProfile	0	public PatientAppointmentView registerABHAProfileApi(@BodyParam("body") UserDTO body, @QueryParam("txnId") String txnId,
abdm\AbhaServiceImpl.java	/abha	POST	/registerChildABHAProfile	1	public UserDTO registerChildABHAProfile(@BodyParam("body") SearchPatientGeneralDetailsResultDto body, @QueryParam("parentToken") String parentToken) throws GenericApplicationExcept
abdm\AbhaServiceImpl.java	/abha	POST	/registerWithAadhar	0	public CreateAccountJwtResponse registerWithAadhar(@BodyParam("body") PatientAppointmentandRegDto body, @QueryParam("txnId") String txnId) throws GenericApplicationException 
abdm\AbhaServiceImpl.java	/abha	POST	/registerWithAadharDemographics	0	public UserDTO registerWithAadharDemographics(@BodyParam("body") SearchPatientGeneralDetailsResultDto body, @QueryParam("txnId") String txnId) throws GenericApplicationException 
abdm\AbhaServiceImpl.java	/abha	POST	/startPatientRecordLinkWithAbha	0	public String startPatientRecordLinkWithAbha(@BodyParam("body") PatientAbhaSearchDto body, @QueryParam("hipId") String hipId) throws GenericApplicationException 
abdm\AbhaServiceImpl.java	/abha	PUT	/confirmABHADeletion	0	public Boolean confirmABHADeletion(@QueryParam("patTokenId") String patTokenId, @QueryParam("txnId") String txnId, @QueryParam("otp") String otp) throws GenericApplicationException
abdm\AbhaServiceImpl.java	/abha	PUT	/delinkHid	0	public Boolean delinkHid(@QueryParam("patToken") String patToken, @QueryParam("phrAddress") String phrAddress) throws GenericApplicationException 
abdm\AbhaServiceImpl.java	/abha	PUT	/linkHid	0	public Boolean linkHid(@QueryParam("patToken") String patToken, @QueryParam("phrAddress") String phrAddress) throws GenericApplicationException 
abdm\HPRServiceImpl.java	/hpr	GET	/checkHPRIdExist	1	public String checkHPRIdExist(@QueryParam("hprId") String hprId) throws GenericApplicationException 
abdm\HPRServiceImpl.java	/hpr	GET	/generateLookupOtp	0	public String generateLookupOtp(@QueryParam("id") String id, @QueryParam("authMode") String authMode) throws GenericApplicationException 
abdm\HPRServiceImpl.java	/hpr	GET	/generateOtp	0	public String[] generateOtp(@QueryParam("type") String type, @QueryParam("txnId") String txnId, @QueryParam("typeText") String typeText) throws GenericApplicationException, IOExcep
abdm\HPRServiceImpl.java	/hpr	GET	/getCard	0	public byte[] getCard(@QueryParam("userId") Long userId) throws GenericApplicationException 
abdm\HPRServiceImpl.java	/hpr	GET	/getProfile	0	public UserDTO getProfile(@QueryParam("userId") Long userId) throws GenericApplicationException 
abdm\HPRServiceImpl.java	/hpr	GET	/loginUser	0	public Long loginHPRUser(@QueryParam("id") String id, @QueryParam("userId") Long userId, @QueryParam("password") String password) throws GenericApplicationException 
abdm\HPRServiceImpl.java	/hpr	GET	/resendOtp	0	public String resendOtp(@QueryParam("type") String type, @QueryParam("txnId") String txnId) throws GenericApplicationException, IOException 
abdm\HPRServiceImpl.java	/hpr	GET	/startLookup	0	public String startLookup(@QueryParam("id") String id, @QueryParam("authMode") String authMode) throws GenericApplicationException 
abdm\HPRServiceImpl.java	/hpr	GET	/verifyAadhaarOtp	0	public TxnResponseDetails verifyAadhaarOtp(@QueryParam("type") String type, @QueryParam("txnId") String txnId, @QueryParam("otp") String otp) throws GenericApplicationException 
abdm\HPRServiceImpl.java	/hpr	GET	/verifyLookupOTP	0	public Long confirmHPR(@QueryParam("userId") Long userId, @QueryParam("authMode") String authMode, @QueryParam("txnId") String txnId, @QueryParam("otp") String otp) throws GenericA
abdm\HPRServiceImpl.java	/hpr	GET	/verifyOtp	0	public String verifyOtp(@QueryParam("type") String type, @QueryParam("txnId") String txnId, @QueryParam("otp") String otp) throws GenericApplicationException 
abdm\HPRServiceImpl.java	/hpr	POST	/registerHPRUserProfile	0	public boolean registerHPRUserProfile(@BodyParam("body") UserDTO body, @QueryParam("userId") Long userId) throws Exception 
abdm\HPRServiceImpl.java	/hpr	POST	/registerWithAadhar	0	public CreateAccountJwtResponse registerWithAadhar(@BodyParam("body") PatientAppointmentandRegDto body, @QueryParam("txnId") String txnId) throws GenericApplicationException 
abdm\NHCXServiceImpl.java	/nhcx	GET	/insurancePlanRequest	0	public NHCXRequest fetchInsurancePlanRequestById(@QueryParam("requestId") Long requestId) throws GenericApplicationException 
abdm\NHCXServiceImpl.java	/nhcx	GET	/previousEligiblities	0	public List<NHCXRequest> fetchPreviousEligiblities(@QueryParam("requestId") Long planRequestId) throws GenericApplicationException 
abdm\NHCXServiceImpl.java	/nhcx	GET	/showNhcxDocument	0	public String showNhcxDocument(@QueryParam("requestId") Long requestId, @QueryParam("requestType") String requestType) throws GenericApplicationException 
abdm\NHCXServiceImpl.java	/nhcx	POST	/allParticipants	0	public ParticipantListResponse getAllParticipants() throws GenericApplicationException 
abdm\NHCXServiceImpl.java	/nhcx	POST	/claimSubmitRequest	0	public void claimSubmit() throws GenericApplicationException 
abdm\NHCXServiceImpl.java	/nhcx	POST	/coverageEligiblityRequest	0	public NHCXRequest checkCoverageEligiblityRequest(@BodyParam("body") CheckCoverageEligiblityRequestDto body) throws GenericApplicationException 
abdm\NHCXServiceImpl.java	/nhcx	POST	/fetchPayorCerts	0	public void fetchPayorCerts() throws GenericApplicationException 
abdm\NHCXServiceImpl.java	/nhcx	POST	/fetchPolicies	0	public List<ParticipantPolicyDetails> fetchPolicies(@QueryParam("mobile") String mobile) throws GenericApplicationException 
abdm\NHCXServiceImpl.java	/nhcx	POST	/insurancePlanRequest	0	public NHCXRequest fetchInsurancePlanRequest(GenericHCXSearchDTO genericHCXSearchDTO) throws GenericApplicationException 
abdm\NHCXServiceImpl.java	/nhcx	POST	/preAuthSubmit	0	public void preAuthSubmit() throws GenericApplicationException 
AuthenticationServiceImpl.java	/auth	POST	/changeUserRole	0	public UserView changeUserRole(@QueryParam("userId") Long userId, @QueryParam("unitId") Long unitId,
AuthenticationServiceImpl.java	/auth	POST	/getuserrole	0	public List<UserShiftDetails> getUserRole(@FormParam("param1") String username, @FormParam("unitId") String unitIdStr)
AuthenticationServiceImpl.java	/auth	POST	/getuserroleOtp	0	public List<UserShiftDetails> getUserRoleOtp(@FormParam("param1") String username, @FormParam("unitId") String unitIdStr)
AuthenticationServiceImpl.java	/auth	POST	/loginb	0	public UserView loginBasicAuth(@HeaderParam("authorization") String authorization)
AuthenticationServiceImpl.java	/auth	POST	/loginExt	0	public LoginResponseView loginFormForKDL(@FormParam("param1") String username,
AuthenticationServiceImpl.java	/auth	POST	/loginextf	0	public UserView loginFormParamsExt(@FormParam("param1") String username,
AuthenticationServiceImpl.java	/auth	POST	/loginf	0	public UserView loginFormParams(@FormParam("param1") String username,
AuthenticationServiceImpl.java	/auth	POST	/loginh	0	public UserView loginHeaderParams(@HeaderParam("param1") String username, @HeaderParam("param2") String password, @HeaderParam("unitId") Long unitId) throws GenericApplicationExcep
AuthenticationServiceImpl.java	/auth	POST	/loginJ	0	public UserView externalLogin(@HeaderParam("apiToken") String apiToken, @HeaderParam("apiKey") String apiKey) throws GenericApplicationException, UnsupportedEncodingException 
AuthenticationServiceImpl.java	/auth	POST	/loginotp	0	public UserView loginotp(@FormParam("param1") String username,
AuthenticationServiceImpl.java	/auth	POST	/loginPatient	0	public UserView loginPatientFormParams(@FormParam("param1") String username,
AuthenticationServiceImpl.java	/auth	PUT	/logout	0	public void logout() throws GenericApplicationException 
BioMedicalWasteManagementServiceImpl.java	/bioMedicalWasteManagement	GET	/approveBioMedicalRequest	1	public Boolean approveBioMedicalRequest(@QueryParam("id") Long id, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
BioMedicalWasteManagementServiceImpl.java	/bioMedicalWasteManagement	GET	/deleteBioRequest	0	public Boolean deleteBioRequest(@QueryParam("id") Long id, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
BioMedicalWasteManagementServiceImpl.java	/bioMedicalWasteManagement	GET	/getBagWeightByBarcode	1	public BigDecimal getBagWeightByBarcode(@QueryParam("id") Long id, @QueryParam("barcode") String barcode) throws GenericApplicationException 
BioMedicalWasteManagementServiceImpl.java	/bioMedicalWasteManagement	GET	/getBioRequestDetails	1	public List<BioMedicalWasteDetails> getBioRequestDetails(@QueryParam("id") Long id, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
BioMedicalWasteManagementServiceImpl.java	/bioMedicalWasteManagement	GET	/handedOverToFirm	1	public Supplier handedOverToFirm() throws GenericApplicationException 
BioMedicalWasteManagementServiceImpl.java	/bioMedicalWasteManagement	GET	/handOverBags	1	public String handOverBags(@QueryParam("id") Long id, @QueryParam("unitId") Long unitId, @QueryParam("handedOverTo") String handedOverTo) throws GenericApplicationException 
BioMedicalWasteManagementServiceImpl.java	/bioMedicalWasteManagement	GET	/processBioRequest	1	public Boolean processBioRequest(@QueryParam("id") Long id, @QueryParam("unitId") Long unitId, @QueryParam("firmName") String firmName,
BioMedicalWasteManagementServiceImpl.java	/bioMedicalWasteManagement	GET	/validateBarcode	0	public boolean validateBarcode(@QueryParam("id") Long id, @QueryParam("barcode") String barcode) throws GenericApplicationException 
BioMedicalWasteManagementServiceImpl.java	/bioMedicalWasteManagement	POST	/addNewBioRequest	0	public BioMedicalWaste addNewBioRequest(@BodyParam("body") BioMedicalWasteView body) throws GenericApplicationException 
BioMedicalWasteManagementServiceImpl.java	/bioMedicalWasteManagement	POST	/getbioMedicalWasteStatus	0	public List<BioMedicalWaste> getbioMedicalWasteStatus(@BodyParam("body") CurrentLinenStatusDto body, @QueryParam("page") Integer page, @QueryParam("count") Integer count,
BioMedicalWasteManagementServiceImpl.java	/bioMedicalWasteManagement	POST	/getBioMedWasteCount	0	public Map<String, Long> getBioMedWasteCount(@BodyParam("body") CurrentLinenStatusDto body) throws GenericApplicationException 
BioMedicalWasteManagementServiceImpl.java	/bioMedicalWasteManagement	POST	/receiveBioRequestMainStore	1	public void receiveBioRequestMainStore(@BodyParam("body") BioMedicalWasteView body) throws GenericApplicationException 
BioMedicalWasteManagementServiceImpl.java	/bioMedicalWasteManagement	POST	/updateBagStatus	0	public void updateBagStatus(@BodyParam("body") List<BioMedicalWasteDetails> body) throws GenericApplicationException 
BioMedicalWasteManagementServiceImpl.java	/bioMedicalWasteManagement	POST	/updateBioRequest	1	public void updateBioRequest(@BodyParam("body") BioMedicalWasteView body) throws GenericApplicationException 
ClinicalServiceImpl.java	/clinical	GET	/getNoteContent	0	public Object[] getNoteContent(@QueryParam("patientId") Long patientId, @QueryParam("unitId") Long unitId,@QueryParam("opdIpdId") Long opdIpdId, @QueryParam("opdipd") Long opdIpd) 
ClinicalServiceImpl.java	/clinical	GET	/getProviderList	0	public ProviderList getProviderList(@QueryParam("unitid") Long unitId) throws GenericApplicationException 
ClinicalServiceImpl.java	/clinical	GET	/healthfactorMaster	1	public List<Healthfactormaster> fetchHealthfactor(@QueryParam("seed") String seed, @QueryParam("exact") @DefaultValue("true") Boolean isExactMatch, @QueryParam("size") @DefaultValu
ClinicalServiceImpl.java	/clinical	POST	/saveAppErrors	0	public void saveAppErrors(AppErrorDetails appErrorDetails) throws GenericApplicationException 
cssd\CSSDServiceImpl.java	/cssd	GET	/deleteKitItemDetails	0	public void deleteKitItemDetails(@QueryParam("compoundDetailsId") Long compoundDetailsId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
cssd\CSSDServiceImpl.java	/cssd	GET	/getAllKitDetailsList	0	public List<CssdKitFormation> getAllKitDetailsList(@QueryParam("unitId") Long unitId) 
cssd\CSSDServiceImpl.java	/cssd	GET	/getFreeAvaliableQtyforKit	0	public BigDecimal getFreeAvaliableQtyforKit(@QueryParam("itemid") Long itemid, @QueryParam("storeid") Long storeid) throws GenericApplicationException 
cssd\CSSDServiceImpl.java	/cssd	GET	/getItemCurrentStockForCSSDKit	0	public List<ItemByNameResultDto> getItemCurrentStockForCSSDKit(@QueryParam("storeId") Long storeId, @QueryParam("itemId") Long itemId) throws GenericApplicationException, ParseExce
cssd\CSSDServiceImpl.java	/cssd	GET	/getItemNameForKitFormation	1	public List<ItemByNameResultDto> getItemForKitFormation(@QueryParam("searchText") @StoredProcNamedParam("p_searchtext") String itemName,
cssd\CSSDServiceImpl.java	/cssd	GET	/getKitDetailsForPreparation	0	public List<CssdKitFormationDetails> getKitDetailsForPreparation(@QueryParam("compoundId") Long compoundId, @QueryParam("unitId") Long unitId,
cssd\CSSDServiceImpl.java	/cssd	GET	/getKitItemsList	0	public List<CssdKitFormationDetails> getKitItemsList(@QueryParam("storeId") Long storeId, @QueryParam("kitIds") String kitIds) throws GenericApplicationException 
cssd\CSSDServiceImpl.java	/cssd	GET	/getKitMasterDetails	0	public KitFormationView getCompoundMasterDetailsList(@QueryParam("kitFormationId") Long kitFormationId, @QueryParam("unitId") Long unitId, @QueryParam("reqNumber") String reqNumber
cssd\CSSDServiceImpl.java	/cssd	GET	/getProcCSSDReqDetailsForReport	1	public List<ProcedureCssdKitDetails> getProcCSSDReqDetailsForReport(@QueryParam("reqNumber") String reqNumber) throws GenericApplicationException 
cssd\CSSDServiceImpl.java	/cssd	GET	/getProcedureCSSDIssueReceivedDetails	0	public List<ProcedureCssdKitDetails> getProcedureCSSDIssueReceivedDetails(@QueryParam("issueNumber") String issueNumber, @QueryParam("issueId") Long issueId) throws GenericApplicat
cssd\CSSDServiceImpl.java	/cssd	GET	/getProcedureCSSDRequestDetails	0	public List<ProcedureCssdKitDetails> getProcedureCSSDRequestDetails(@QueryParam("reqNumber") String reqNumber, @QueryParam("reqId") Long reqId) throws GenericApplicationException 
cssd\CSSDServiceImpl.java	/cssd	GET	/isKitExists	1	public long isKitExists(@QueryParam("kitName") String kitName, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
cssd\CSSDServiceImpl.java	/cssd	GET	/updateCssdRequisitionForRinsing	1	public void updateCssdRequisitionForRinsing(@QueryParam("reqNumber") String reqNumber, @QueryParam("reqId") Long reqId, @QueryParam("frmstore") Long frmstore,
cssd\CSSDServiceImpl.java	/cssd	GET	/updateCssdRequisitionFromStoreDispatch	1	public void updateCssdRequisitionFormStoreDispatch(@QueryParam("reqNumber") String reqNumber, @QueryParam("reqId") Long reqId, @QueryParam("frmstore") Long frmstore,
cssd\CSSDServiceImpl.java	/cssd	GET	/updateCssdRequisitionFromStoreReceive	1	public void updateCssdRequisitionFromStoreReceive(@QueryParam("reqNumber") String reqNumber, @QueryParam("reqId") Long reqId, @QueryParam("frmstore") Long frmstore,
cssd\CSSDServiceImpl.java	/cssd	GET	/updateCssdRequisitionToStoreReceive	1	public void updateCssdRequisitionToStoreReceive(@QueryParam("reqNumber") String reqNumber, @QueryParam("reqId") Long reqId, @QueryParam("frmstore") Long frmstore,
cssd\CSSDServiceImpl.java	/cssd	GET	/updateRequestAndIssueWhileDispatch	1	public void updateRequestAndIssueWhileDispatch(@QueryParam("reqNumber") String reqNumber, @QueryParam("reqId") Long reqId, @QueryParam("frmstore") Long frmstore,
cssd\CSSDServiceImpl.java	/cssd	GET	/verifyMasterKitPreparation	1	public boolean verifyMasterKitPreparation(@QueryParam("verifyPas") String verifyPas, @QueryParam("verifyId") Long verifyId) throws GenericApplicationException, NoSuchAlgorithmExcep
cssd\CSSDServiceImpl.java	/cssd	POST	/addItemSterilizationDetails	0	public CssdItemSterilizationDetails addItemSterilizationDetails(@BodyParam("body") CssdItemSterilizationDetails body) throws GenericApplicationException, ParseException 
cssd\CSSDServiceImpl.java	/cssd	POST	/addKitMaster	1	public KitFormationView addKitFormation(@BodyParam("body") KitFormationView body) throws GenericApplicationException, ParseException 
cssd\CSSDServiceImpl.java	/cssd	POST	/addKitRequestFromKitMaster	1	public ProcedureCSSDRequestView addKitRequestFromKitMaster(@BodyParam("body") ProcedureCSSDRequestView body) throws GenericApplicationException 
cssd\CSSDServiceImpl.java	/cssd	POST	/addNewItemInExisitingKit	1	public KitFormationView addNewItemInExisitingKit(@BodyParam("body") KitFormationView body) throws GenericApplicationException 
cssd\CSSDServiceImpl.java	/cssd	POST	/addUpdateKitItemDetails	1	public List<CssdKitFormationDetails> addUpdateKitItemDetails(@BodyParam("body") KitFormationView body) throws GenericApplicationException 
cssd\CSSDServiceImpl.java	/cssd	POST	/cssdItemIssue	0	public IssueView cssdItemIssue(@BodyParam("body") CompleteIndentView body) throws GenericApplicationException 
cssd\CSSDServiceImpl.java	/cssd	POST	/freezeKitRequest	1	public void freezeKitRequest(@QueryParam("storeId") Long storeId, @QueryParam("kitId") Long kitId) throws GenericApplicationException 
cssd\CSSDServiceImpl.java	/cssd	POST	/getCSSDItemSterilizeDetails	0	public List<ProcedureCssdKitDetails> getCSSDItemSterilizeDetails(@QueryParam("storeId") Long storeId) throws GenericApplicationException 
cssd\CSSDServiceImpl.java	/cssd	POST	/getCssdKitRequestListCount	0	public CssdKitRequestListCountSummary getCssdKitRequestListCount(@BodyParam("body") ProcedureCssdKitDetails body) throws GenericApplicationException 
cssd\CSSDServiceImpl.java	/cssd	POST	/getCssdStoreKitRequestListCount	0	public CssdKitRequestListCountSummary getCssdStoreKitRequestListCount(@BodyParam("body") ProcedureCssdKitDetails body) throws GenericApplicationException 
cssd\CSSDServiceImpl.java	/cssd	POST	/getCurrentKitMasterList	0	public List<CssdKitFormation> getCurrentKitMasterList(@BodyParam("body") CssdKitFormation body, @QueryParam("page") Integer page,
cssd\CSSDServiceImpl.java	/cssd	POST	/getCurrentKitPreparationList	1	public List<CssdKitFormation> getCurrentKitCreationList(@BodyParam("body") Compoundmaster body, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
cssd\CSSDServiceImpl.java	/cssd	POST	/getIndentListforCssdStoreIdforItemIssue	0	public List<IndentListByStoreIdResultDto> getIndentListforCssdStoreIdforItemIssue(@BodyParam("body") IndentListByStoreIdDto body) throws GenericApplicationException 
cssd\CSSDServiceImpl.java	/cssd	POST	/getKitListSummary	0	public KitMasterListSummary getkitListSummary(@BodyParam("body") CssdKitFormation body) throws GenericApplicationException 
cssd\CSSDServiceImpl.java	/cssd	POST	/getProcedureCSSDIssue	1	public List<ProcedureCssdRequest> getProcedureCSSDIssue(@BodyParam("body") ProcedureCssdKitDetails body) throws GenericApplicationException 
cssd\CSSDServiceImpl.java	/cssd	POST	/getProcedureCSSDKitDetails	0	public List<ProcedureCssdKitDetails> getProcedureCSSDKitDetails(@BodyParam("body") ProcedureCssdKitDetails body) throws GenericApplicationException 
cssd\CSSDServiceImpl.java	/cssd	POST	/getProcedureCSSDRequest	1	public List<ProcedureCssdKitDetails> getProcedureCSSDRequest(@BodyParam("body") ProcedureCssdKitDetails body, @QueryParam("page") Integer page,
cssd\CSSDServiceImpl.java	/cssd	POST	/getProcedureCSSDRequestDetailsforIssue	0	public ProcedureCSSDRequestView getProcedureCSSDRequestDetailsforIssue(@BodyParam("body") ProcedureCSSDRequestView body) throws GenericApplicationException, ParseException 
cssd\CSSDServiceImpl.java	/cssd	POST	/getProcedureCSSDStoreRequest	0	public List<ProcedureCssdKitDetails> getProcedureCSSDStoreRequest(@BodyParam("body") ProcedureCssdKitDetails body, @QueryParam("page") Integer page,
cssd\CSSDServiceImpl.java	/cssd	POST	/issueItemReceiveListForCSSD	0	public List<GetDirectIssueItemDetailsResultDto> issueItemReceiveList(@BodyParam("body") GetDirectIssueItemDetailsDto body) throws GenericApplicationException 
cssd\CSSDServiceImpl.java	/cssd	POST	/printKitRequisitionDetails	0	public String printKitRequisitionDetails(@BodyParam("body") ProcedureCSSDRequestView body) throws GenericApplicationException 
cssd\CSSDServiceImpl.java	/cssd	POST	/unHoldCSSDRequestDetailsforKit	0	public void unHoldCSSDRequestDetailsforKit(@BodyParam("body") KitFormationView body) throws GenericApplicationException 
cssd\CSSDServiceImpl.java	/cssd	POST	/updateCssdRequestforIssue	0	public ProcedureCSSDRequestView updateCssdRequestforIssue(@BodyParam("body") ProcedureCSSDRequestView body) throws GenericApplicationException, ParseException 
cssd\CSSDServiceImpl.java	/cssd	POST	/updateCssdRequestReceiving	0	public void updateCssdRequestReceiving(@BodyParam("body") ProcedureCSSDRequestView body) throws GenericApplicationException, ParseException 
cssd\CSSDServiceImpl.java	/cssd	POST	/updateCssdRequisitionForHold	1	public void updateCssdRequisitionForHold(@BodyParam("body") List<CssdKitFormationDetails> body,
cssd\CSSDServiceImpl.java	/cssd	POST	/updateCssdRequisitionForToStoreHold	1	public void updateCssdRequisitionForToStoreHold(@BodyParam("body") List<CssdKitFormationDetails> body, @QueryParam("reqNumber") String reqNumber, @QueryParam("reqId") Long reqId, @
cssd\CSSDServiceImpl.java	/cssd	POST	/updateKitMaster	1	public CssdKitFormation updateKitFormation(@BodyParam("body") KitFormationView body) throws GenericApplicationException, ParseException 
cssd\CSSDServiceImpl.java	/cssd	PUT	/updateStatusProcedureCSSDKitDetails	0	public void updateStatusProcedureCSSDKitDetails(@BodyParam("body") ProcedureCssdKitDetails body) throws GenericApplicationException 
custom\ActionNotificationServiceImpl.java	/actionNotif	GET	/	0	public List<ActionNotification> getAll(@QueryParam("module") String module, @QueryParam("stage") String stage) throws GenericApplicationException 
custom\ActionNotificationServiceImpl.java	/actionNotif	POST	/	0	public ActionNotification add(ActionNotification notif) throws GenericApplicationException 
custom\ActionNotificationServiceImpl.java	/actionNotif	PUT	/	1	public ActionNotification update(ActionNotification notif) throws GenericApplicationException 
custom\AddAdvanceServiceImpl.java	/addAdvance	GET	/getAdvanceAgainstList	0	public List<Advanceagainst> getAdvanceAgainstList(@QueryParam("unitId")Long unitId) throws GenericApplicationException 
custom\AddAdvanceServiceImpl.java	/addAdvance	GET	/getAdvancePaymentDetailList	0	public List<Object> getPaymentAdvanceDetail(@QueryParam("unitId")Long unitId, @QueryParam("patientId")Long patientId, @QueryParam("opd_Ipd_Id")Long opd_Ipd_Id) 
custom\AddAdvanceServiceImpl.java	/addAdvance	GET	/getPatientAdvanceByBillEstimation	0	public List<Object> getPatientAdvanceByBillEstimation(@QueryParam("patientBillEstimationId") Long patientBillEstimationId, @QueryParam("unitId")Long unitId) throws GenericApplicat
custom\AddAdvanceServiceImpl.java	/addAdvance	POST	/	1	public AdvanceView addAdvance(@BodyParam("body") AdvanceView body) throws GenericApplicationException 
custom\AddAdvanceServiceImpl.java	/addAdvance	POST	/getAdvance	0	public BigDecimal getBillSummary(@BodyParam("body") PatientAdvanceDto body) throws GenericApplicationException 
custom\AddPaymentServiceImpl.java	/addPayment	POST	/	1	public BillListForSearch_NewResultDto_3 addBillPaymentNew(@BodyParam("body") AddPaymentDto body, @QueryParam("refund") Boolean refund, @QueryParam("com_pay") Boolean compayPayment,
custom\AddPaymentServiceImpl.java	/addPayment	POST	/itemSalePayment	1	public List<Long> addItemSalePayment(@BodyParam("body") ServiceOrderDto body) throws GenericApplicationException 
custom\AddPaymentServiceImpl.java	/addPayment	PUT	/updateCharges	1	public GenericResultDto updateCharge(@BodyParam("body") UpdateCharge body) throws GenericApplicationException 
custom\AddRefundServiceImpl.java	/addRefund	POST	/refund	1	public RefundView addRefund(@BodyParam("body") RefundView body) throws GenericApplicationException 
custom\AddServiceMasterImpl.java	/addServiceMaster	DELETE	/removeServiceToById/{serviceId}	0	public void removeServiceToById(@PathParam("serviceId") Long serviceId) throws GenericApplicationException 
custom\AddServiceMasterImpl.java	/addServiceMaster	GET	/getServiceList	0	public List<ServiceListByUnitIdResultDto> getServiceList() throws GenericApplicationException 
custom\AddServiceMasterImpl.java	/addServiceMaster	GET	/getServiceToByServiceID/{serviceId}	0	public AddServiceToMasterView getServiceToByServiceID(@PathParam("serviceId") Long serviceId) throws GenericApplicationException 
custom\AddServiceMasterImpl.java	/addServiceMaster	POST	/addServiceToMaster	0	public AddServiceToMasterView addServiceToMaster(@BodyParam("body") AddServiceToMasterView body) throws GenericApplicationException 
custom\AddServiceMasterImpl.java	/addServiceMaster	POST	/getServiceListByUnitID	0	public List<ServiceListByUnitIdResultDto> getServiceListByUnitID() throws GenericApplicationException 
custom\AddServiceMasterImpl.java	/addServiceMaster	PUT	/updateServiceToListByServiceID	1	public AddServiceToMasterView updateServiceToListByServiceID(@BodyParam("body") AddServiceToMasterView body) throws GenericApplicationException 
custom\AddServiceOrderServiceImpl.java	/serviceOrder	DELETE	/deleteService	1	public boolean deleteService(@QueryParam("serviceId") Long serviceId, @QueryParam("opdIpdId") Long opdIpdId) throws GenericApplicationException 
custom\AddServiceOrderServiceImpl.java	/serviceOrder	DELETE	/excludeCharge	0	public void excluseChargeNew(@QueryParam("id") Long id, @QueryParam("consumed") boolean consumed) throws GenericApplicationException 
custom\AddServiceOrderServiceImpl.java	/serviceOrder	DELETE	/removeCharge	1	public Long removeCharge(@QueryParam("id") Long id, @QueryParam("consumed") boolean consumed, @BodyParam("body") ServiceOrderDto body) throws GenericApplicationException 
custom\AddServiceOrderServiceImpl.java	/serviceOrder	GET	/allPackagesFastByGet	0	public List<PackageListForBillResultDto> getAllPackagesForBillFastByGet(@QueryParam("opd_id") int opdIpd, @QueryParam("opd_ipd_id") Long opdIpdId,
custom\AddServiceOrderServiceImpl.java	/serviceOrder	GET	/allServiceFastByGet	0	public List<ServiceListForBillWithoutCopayConcessionFastResultDto> getAllServicesForBillFastByGet(@QueryParam("opd_id") int opdIpd, @QueryParam("opd_ipd_id") Long opdIpdId,
custom\AddServiceOrderServiceImpl.java	/serviceOrder	GET	/checkChargeStatusForCancelBill	0	public boolean checkChargeStatusForCancelBill(@QueryParam("billId") Long billId) throws GenericApplicationException 
custom\AddServiceOrderServiceImpl.java	/serviceOrder	GET	/checkCrossUnitVisit	0	public Boolean checkCrossUnitVisitApi(@QueryParam("opdIpdId") Long opdIpdId) throws GenericApplicationException 
custom\AddServiceOrderServiceImpl.java	/serviceOrder	GET	/checkSampleStatusForCreditNote	0	public boolean checkSampleStatusForCreditNote(@QueryParam("id") Long chargeId, @QueryParam("serviceId") Long serviceId) throws GenericApplicationException 
custom\AddServiceOrderServiceImpl.java	/serviceOrder	GET	/getAdmissionCharges	1	public BigDecimal getAdmissionCharges(@QueryParam("plaId") Long planId, @QueryParam("classId") Long classId,
custom\AddServiceOrderServiceImpl.java	/serviceOrder	GET	/getAllServiceMasterDetailsByDescription	0	public List<AllServiceMasterDetailsByDescriptionResultDto> getAllServiceMasterDetailsSrv(@QueryParam("description") @StoredProcNamedParam("p_description") String description,
custom\AddServiceOrderServiceImpl.java	/serviceOrder	GET	/getDoctorByBillingDepartmentId	1	public List<Doctormaster> getDoctorsByBillingDepartmentId(@QueryParam("departmentId") Long departmentId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\AddServiceOrderServiceImpl.java	/serviceOrder	GET	/getOutsourceServiceStatus	0	public Boolean getOutSourcedServiceStatus(@QueryParam("serviceId") Long serviceId, @QueryParam("outSourceUnit") Long outSourceUnit,@QueryParam("tariffId") Long tariffId) throws Gen
custom\AddServiceOrderServiceImpl.java	/serviceOrder	GET	/getPackageCharges	1	public List<PkgCoverageDto> getPackageCharges(@QueryParam("opdIpdId") Long opdIpdId, @QueryParam("opdIpd") Long opdIpd) throws GenericApplicationException 
custom\AddServiceOrderServiceImpl.java	/serviceOrder	GET	/getPackageDetail	0	public List<PackageDetailView> getPackageDetailBill(@QueryParam("mrno") String mRNO, @QueryParam("unit_code") String unitCode) throws GenericApplicationException 
custom\AddServiceOrderServiceImpl.java	/serviceOrder	GET	/getRunningBill	1	public Object[] getRunningBill(@QueryParam("mrno") String mRNO, @QueryParam("unit_code") String unitCode) throws GenericApplicationException 
custom\AddServiceOrderServiceImpl.java	/serviceOrder	GET	/getServicePrice	0	public ServiceListForBillResultDto getServicePriceDetails(@QueryParam("service_code") String serviceCode, @QueryParam("mrno") String mRNO, @QueryParam("visit_adm_number") String vi
custom\AddServiceOrderServiceImpl.java	/serviceOrder	GET	/getSponsorDetail	0	public SponsorDetailDto getSponsorDetail(@QueryParam("mrno") String mRNO, @QueryParam("unit_code") String unitCode) throws GenericApplicationException 
custom\AddServiceOrderServiceImpl.java	/serviceOrder	GET	/getSubscribedPackageCharges	0	public List<PkgCoverageDto> getSubscribedPackageCharges(@QueryParam("opdIpdId") Long opdIpdId, @QueryParam("opdIpd") Long opdIpd) throws GenericApplicationException 
custom\AddServiceOrderServiceImpl.java	/serviceOrder	GET	/getvisitCharges	0	public BigDecimal getvisitCharges(@QueryParam("plaId") Long planId, @QueryParam("visitTypeId") Long visitTypeId, @QueryParam("departmentId") Long departmentId,
custom\AddServiceOrderServiceImpl.java	/serviceOrder	GET	/services/{id}	1	public Servicemaster getServiceMasterListById(@PathParam("id") Long id) throws GenericApplicationException 
custom\AddServiceOrderServiceImpl.java	/serviceOrder	POST	/addExternalOrderDetails	0	public BillInfoDto addExternalOrderDetails(@BodyParam("body") ServiceOrderDto body) throws GenericApplicationException 
custom\AddServiceOrderServiceImpl.java	/serviceOrder	POST	/addHealthplanConsumptionDetail	0	public HealthplanPackageConsumptionDetailsDto addHealthplanPackageConsumptionDetails(@BodyParam("body") HealthplanPackageConsumptionDetailsDto body) throws GenericApplicationExcept
custom\AddServiceOrderServiceImpl.java	/serviceOrder	POST	/addMultipleHealthplanConsumptionDetail	1	public List<HealthplanPackageConsumptionDetailsDto> addMultipleHealthplanPackageConsumptionDetails(@BodyParam("body") List<HealthplanPackageConsumptionDetailsDto> body)
custom\AddServiceOrderServiceImpl.java	/serviceOrder	POST	/addPackageRefund	0	public CreditNote addPackageRefund(@BodyParam("body") CreditNote body) throws GenericApplicationException 
custom\AddServiceOrderServiceImpl.java	/serviceOrder	POST	/addPharmacyItemtoChargeForIP	1	public void addPharmacyItemtoChargeForIP(@BodyParam("body") AddVisitChargeDto body) throws GenericApplicationException 
custom\AddServiceOrderServiceImpl.java	/serviceOrder	POST	/addServicechargesNew	1	public BillInfoDto addServices(@BodyParam("body") ServiceOrderDto body) throws GenericApplicationException 
custom\AddServiceOrderServiceImpl.java	/serviceOrder	POST	/addVisitCharges	1	public BigDecimal addVisitCharges(@BodyParam("body") AddVisitChargeDto body) throws GenericApplicationException 
custom\AddServiceOrderServiceImpl.java	/serviceOrder	POST	/addVisitForHealthplanConsumption	1	public MarkVisitForPackageServiceConsumptionDto addVisitForHealthplanConsumption(@BodyParam("body") MarkVisitForPackageServiceConsumptionDto body) throws GenericApplicationExceptio
custom\AddServiceOrderServiceImpl.java	/serviceOrder	POST	/getAllPrevCharges	0	public BillChargesView getAllPrevCharges(@BodyParam("body") BillChargeDto body) throws GenericApplicationException 
custom\AddServiceOrderServiceImpl.java	/serviceOrder	POST	/getAllService	0	public List<ServiceListForBillResultDto> getAllServicesForBill(@BodyParam("body") SearchServiceDto search, @DefaultValue("true") @QueryParam("checkInsuranceExpiry") Boolean checkIn
custom\AddServiceOrderServiceImpl.java	/serviceOrder	POST	/getAllServiceFast	0	public List<ServiceListForBillWithoutCopayConcessionFastResultDto> getAllServicesForBillFast(@BodyParam("body") SearchServiceDto searchServiceDto) throws GenericApplicationExceptio
custom\AddServiceOrderServiceImpl.java	/serviceOrder	POST	/getCharges	1	public BillChargesView getCharges(@BodyParam("body") BillChargeDto body) throws GenericApplicationException 
custom\AddServiceOrderServiceImpl.java	/serviceOrder	POST	/getChargesForCreditNote	1	public BillChargesView getChargesForCreditNote(@BodyParam("body") BillChargeDto body) throws GenericApplicationException 
custom\AddServiceOrderServiceImpl.java	/serviceOrder	POST	/getChargesForHealthConsumption	0	public
custom\AddServiceOrderServiceImpl.java	/serviceOrder	POST	/getExternalorders	1	public List<ExternalOrderDetails> getExternalorders(@BodyParam("body") BillChargeDto body) throws GenericApplicationException 
custom\AddServiceOrderServiceImpl.java	/serviceOrder	POST	/getHealthplanConsumption	1	public
custom\AddServiceOrderServiceImpl.java	/serviceOrder	POST	/getLabOrderDetailsOfPatient	0	public List<GetChargesResultDto> getLabOrderDetailsOfPatient(@BodyParam("body") BillChargeDto body) throws GenericApplicationException 
custom\AddServiceOrderServiceImpl.java	/serviceOrder	POST	/getPackageChargeMap	0	public Map<Long, List<PackageServiceFromPackageIDResultDto_1>> getPackageChargeMap(@BodyParam("body") List<ChargeDto> body) throws GenericApplicationException 
custom\AddServiceOrderServiceImpl.java	/serviceOrder	POST	/getPackages	0	public List<PackageListForBillResultDto> getPackageListForBillApi(@BodyParam("body") SearchServiceDto search) throws GenericApplicationException 
custom\AddServiceOrderServiceImpl.java	/serviceOrder	POST	/getPackageServices	1	public PackageServiceFromPackageIDResultDto getPackageServices(@BodyParam("body") SearchServiceDto searchServiceDto, @QueryParam("package_id") Long packageId)
custom\AddServiceOrderServiceImpl.java	/serviceOrder	POST	/getSpecializationForBill	0	public List<SpecializationDTO> getSpecializationForBill(@BodyParam("body") SearchServiceDto searchServiceDto) throws GenericApplicationException 
custom\AddServiceOrderServiceImpl.java	/serviceOrder	POST	/includeCharge	1	public void includeChargeNew(@BodyParam("body") IncluseChargeDto incluseChargeDto) throws GenericApplicationException 
custom\AddServiceOrderServiceImpl.java	/serviceOrder	POST	/releaseAllLab	0	public void releaseAllLab(@BodyParam("body") ReleaseAllView releaseAllView) throws GenericApplicationException 
custom\AddServiceOrderServiceImpl.java	/serviceOrder	POST	/saveCreditNote	0	public CreditNote saveCreditNote(CreditNote creditNoteDtoObj) throws GenericApplicationException 
custom\AddServiceOrderServiceImpl.java	/serviceOrder	POST	/saveServices	0	public ServiceRateDto saveServiceAPI(ServiceRateDto serviceRateDto) throws GenericApplicationException 
custom\AddServiceOrderServiceImpl.java	/serviceOrder	POST	/updateHealthplanConsumptionSchedule	1	public MarkVisitForPackageServiceConsumptionDto updateHealthplanConsumptionSchedule(@BodyParam("body") HealthplanPackageConsumptionDetailsDto body) throws GenericApplicationExcepti
custom\AddServiceOrderServiceImpl.java	/serviceOrder	POST	/updatePathoClinicalDetails	0	public void updatePathoClinicalDetails(@BodyParam("body") ServiceOrderDto body) throws GenericApplicationException 
custom\AddServiceOrderServiceImpl.java	/serviceOrder	PUT	/getSponsors	0	public List<SponsorByAdmissionIDResultDto> getSponsors(@QueryParam("opd_ipd_id") Long opd_ipd_id, @QueryParam("unit_id") Long unit_id, @QueryParam("opd_ipd") boolean opd_id)
custom\AddServiceOrderServiceImpl.java	/serviceOrder	PUT	/moveOPChargesToIP	0	public void moveOPChargesToIP(Long visitId, Long admId, Long admUnitId) throws GenericApplicationException 
custom\AddUpdateAppointmentServiceImpl.java	/updateappointment	POST	/cancelupdateappointment	0	public CancelUpdateAppointmentDto canceladdappt(@BodyParam("body") PatientAppointmentandRegDto body) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	DELETE	/removeUserBuilding/{id}	0	public void removeUserBuilding(@PathParam("id") Long id) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	DELETE	/removeUserSpeciality/{id}	0	public void removeUserSpeciality(@PathParam("id") Long id) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	GET	/checkDescriptionInMoleculeMaster	1	public Long checkDescriptionInMoleculeMaster(@QueryParam("description") String description) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	GET	/checkDescriptionInPregnencyClassMaster	0	public Long checkDescriptionInPregnencyClassMaster(@QueryParam("description") String description) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	GET	/checkDescriptionInProcedureMaster	0	public Long checkDescriptionInProcedureMaster(@QueryParam("description") String description) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	GET	/checkDescriptionInServiceMaster	1	public Long getActiveAdmissionOfRegisteredPatient(@QueryParam("description") String description) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	GET	/checkItemGroupDescription	0	public Long checkItemGroupDescription(@QueryParam("description") String description) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	GET	/checkItemGroupPrefix	0	public Number checkItemGroupPrefix(@QueryParam("prefix") String prefix) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	GET	/checkLoginNameForUser	1	public Number checkLoginNameForUser(@QueryParam("description") String description) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	GET	/getAllDepartments	0	public List<Departmentmaster> getAllDepartments(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	GET	/getAllRole	0	public List<TUserrolemaster> getAllRole() throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	GET	/getAllUnitMasterList	1	public List<Unitmaster> getAllUnitMasterList(@QueryParam("status") Boolean status) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	GET	/getAllUnits	0	public List<Unitmaster> getAllUnits() throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	GET	/getCabinDetailsById	1	public Cabinmaster getCabinmasterDetails(@QueryParam("Id") Long id) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	GET	/getCadredesignationDetailsById	0	public String getCadreDesigantionMasterDetails(@QueryParam("id") Long id) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	GET	/getCliniciansBySpecialityId	0	public List<Doctormaster> getCliniciansBySpecialityId(@QueryParam("specialityId") Long specialityId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	GET	/getDepartmentDoctor	0	public List<Departmentwisedoctor> getDepartmentWiseDoctor(@QueryParam("unitId") Long unitid) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	GET	/getDepartmentwisecabinById	0	public List<Cabinmaster> getDepartmentwisecabinById(@QueryParam("Id") Long id) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	GET	/getDSCRByUnitId	0	public List<UserShiftDetails> getDSCRByUnitId(@QueryParam("unitId") Integer unitId, @QueryParam("tabType") String tabType) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	GET	/getFrequantlyListByUser	0	public List <Servicemaster> getFrequantlyListByUser(@QueryParam("specilizationId") Long specilizationId, @QueryParam("count") Long count, @QueryParam("doctorId") Long doctorId) thr
custom\AddUserServiceImpl.java	/addUserService	GET	/getMrnoByJanMemberId	0	public SearchPatientGeneralDetailsResultDto getMrnoByJanMemberId(@QueryParam("userId") Long userId) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	GET	/getPermissions	0	public List<RoleBasedPermissionJsTreeStateDTO> getPermissions(@QueryParam("roleId") Long roleId) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	GET	/getPermissionsOfWard	0	public Boolean getPermissionsOfWard(@QueryParam("description") String description, @QueryParam("roleId") Long roleId) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	GET	/getStaffDetailsByRoleId	0	public List<Staffmaster> getCurrentStaffDetailsByRoleAndUnitId(@QueryParam("levelId") Long levelId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	GET	/getStateUserMappedStateList	0	public List<UserStateDetails> getStateUserMappedStateList() throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	GET	/getUserBuilding	0	public List<TUserBuildingDetails> getUserBuilding(@QueryParam("userId") Long userID, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	GET	/getUserDefaultRole	0	public Map<Long, List<UserShiftDetails>> getUserRole(@QueryParam("userId") Integer userID, @QueryParam("unitId") Integer unitId) throws GenericApplicationException, UnsupportedEnco
custom\AddUserServiceImpl.java	/addUserService	GET	/getUserDetailsByRoleId	1	public List<Staffmaster> getUserDetailsByRoleId(@QueryParam("roleId") Long roleId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	GET	/getUserSpeciality	1	public List<UserSpecialityDetails> getUserSpeciality(@QueryParam("userId") Long userID, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	GET	/syncUserApiSecret	0	public boolean syncUserApiSecret(@QueryParam("userId") Long userID) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	GET	/updateVistaPassword	0	public boolean updateVistaPassword(@QueryParam("newUserPassword") String userPassword) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	GET	/validateCodePayerMaster	0	public Long validateCodePayerMaster(@QueryParam("code") String code) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	GET	/validateUnitHeadUser	0	public Doctordepartmentdetails validateUnitHeadUser(@QueryParam("departmentId") Long departmentId, @QueryParam("buildingId") Long buildingId, @QueryParam("doctorId") Long doctorId)
custom\AddUserServiceImpl.java	/addUserService	GET	/validateUserClinicalLevel	0	public Doctordepartmentdetails validateUserClinicalLevel(@QueryParam("departmentId") Long departmentId, @QueryParam("buildingId") Long buildingId, @QueryParam("levelId") Long level
custom\AddUserServiceImpl.java	/addUserService	POST	/acceptTransferUser	1	public boolean acceptTransferUser(UserTransferDetails userTransferDetails) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	POST	/addCabinMaster	1	public void addCabinNew(@BodyParam("body") Cabinmaster cabinmaster)throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	POST	/addSupplierUser	0	public Boolean addSupplierUser(@BodyParam("body") TUserdetails body) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	POST	/changeUserBuilding	1	public boolean changeUserBuilding(@QueryParam("userWingId") Long userWingId, @QueryParam("clinicianId") Long clinicianId,
custom\AddUserServiceImpl.java	/addUserService	POST	/changeUserClinician	0	public boolean changeUserClinician(@QueryParam("clinicianId") Long clinicianId, @QueryParam("clinicianDesc") String clinicianDesc) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	POST	/getUserByConcessionCategory	0	public List<GetUserByConcessionCategoryResultDto> getUserByConcessionCategory(@BodyParam("body") GetUserByConcessionCategoryDto body) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	POST	/getUserDetailsForUpdate	1	public AddUserView getUserDetailsForUpdateNew(@QueryParam("userId") Long userId) throws GenericApplicationException, UnsupportedEncodingException 
custom\AddUserServiceImpl.java	/addUserService	POST	/getUserDetailsList	1	public List<GetUserDetailsResultDto> getUserDetailsList(@BodyParam("body") GetUserDetailDto body, @QueryParam("page") Integer page, @QueryParam("count") Integer count,
custom\AddUserServiceImpl.java	/addUserService	POST	/rejectTransferUser	1	public boolean rejectTransferUser(UserTransferDetails userTransferDetails) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	POST	/saveUser	0	public void saveUser(@BodyParam("body") AddUserView body) throws GenericApplicationException, UnsupportedEncodingException 
custom\AddUserServiceImpl.java	/addUserService	POST	/sendOtpToUserMobileNo	1	public String sendOtpToUserMobileNo(@QueryParam("userId") Long userId, @QueryParam("mobileNo") String mobileNo) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	POST	/transferUser	1	public boolean transferUser(UserTransferDetails userTransferDetails) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	POST	/transferUserCancel	1	public boolean transferUserCancel(UserTransferDetails userTransferDetails) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	POST	/updateJanaadharDetailsForEHR	0	public boolean updateJanaadharDetailsForEHR(@BodyParam("body") UpdateJanaadharView body) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	POST	/updatePermissions	0	public void updatePermissions(UpdateRolePermissionDTO dto) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	POST	/updateUser	0	public void updateUser(@BodyParam("body") AddUserView body) throws GenericApplicationException, UnsupportedEncodingException 
custom\AddUserServiceImpl.java	/addUserService	POST	/verifyUserByFaceauth	1	public String verifyUserByFaceauth(@QueryParam("userId") Long userId) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	POST	/verifyUserMobileNo	0	public String verifyUserMobileNo(@QueryParam("userId") Long userId, @QueryParam("mobileNo") String mobileNo, @QueryParam("otpVal") String otp, @QueryParam("isUpdate") Boolean isUp
custom\AddUserServiceImpl.java	/addUserService	PUT	/deleteCabinListById/{id}	0	public void deleteCabinListById(@PathParam("id") Long id) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	PUT	/deleteUnitListById/{id}	0	public boolean deleteUnitListById(@PathParam("id") Long id) throws GenericApplicationException 
custom\AddUserServiceImpl.java	/addUserService	PUT	/updateCabinMaster/{id}	1	public void updateCabin(@PathParam("id") Long id, @BodyParam("body") Cabinmaster cabin) throws GenericApplicationException 
custom\AdvanceBillReceiptServiceImpl.java	/advancebillReceipt	POST	/getAdvanceReceipt	0	public List<RptGetAdvanceResultDto> getAdvance(@BodyParam("body") RptGetAdvanceDto body, @QueryParam("is_print") boolean print, @Context HttpServletResponse response)
custom\AdvanceBillReceiptServiceImpl.java	/advancebillReceipt	POST	/getAdvanceReceiptByReceiptNo	0	public List<RptGetAdvanceResultDto> getAdvanceByReceiptNo(@BodyParam("body") AdvanceReceiptByReceiptNoDto body, @QueryParam("is_print") boolean print, @Context HttpServletResponse 
custom\AdvanceBillReceiptServiceImpl.java	/advancebillReceipt	POST	/getAdvanceReceiptHtml	0	public Map<String, Object> getAdvanceReceiptHtml(@BodyParam("body") RptGetAdvanceDto body) throws GenericApplicationException 
custom\AdvanceBillReceiptServiceImpl.java	/advancebillReceipt	POST	/printAdvanceReceipt	0	public List<RptGetAdvanceResultDto> printAdvanceReceipt(List<RptGetAdvanceResultDto> rptGetAdvanceResultDtos) throws GenericApplicationException 
custom\AllReceiptsServiceImpl.java	/allReceipts	POST	/getAllAdvanceList	0	public List<AllAdvanceListResultDto> getAllAdvanceList(@BodyParam("body") AllAdvanceListDto body) throws GenericApplicationException 
custom\AllReceiptsServiceImpl.java	/allReceipts	POST	/getAllReceipts	0	public List<AllReceiptsListResultDto> getAllReceipts(@BodyParam("body") BillListForSearch_NewDto body, @QueryParam("page") Integer page,
custom\AppointmentScheduleServiceImpl.java	/appointmentschedule	GET	/checkDoctorIdForTheaterId	0	public long checkDoctorIdForTheaterId(@QueryParam("doctorId") Long doctorId, @QueryParam("fromTime") String fromTime,
custom\AppointmentScheduleServiceImpl.java	/appointmentschedule	GET	/getCabinsByDepartmentId	0	public List<Cabinmaster> getCabinsByDepartmentIdDep(@QueryParam("deptId") Long deptId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\AppointmentScheduleServiceImpl.java	/appointmentschedule	GET	/getCabinsByDepartmentIdOld	0	public List<Cabinmaster> getCabinsByDepartmentIdOld(
custom\AppointmentScheduleServiceImpl.java	/appointmentschedule	GET	/getDayWiseBuildingMasterByDepartmentId	0	public List<Buildingmaster> getDayWiseBuildingMasterByDepartmentId(@QueryParam("departmentId") String departmentIdStr, @QueryParam("unitId") Long unitId) throws GenericApplicationE
custom\AppointmentScheduleServiceImpl.java	/appointmentschedule	GET	/getDepartmentsAppointmentSchedule	0	public List<AppointmentScheduleSummary> getDepartmentsAppointmentSchedule(@QueryParam("page") Integer page, @QueryParam("count") Integer count,
custom\AppointmentScheduleServiceImpl.java	/appointmentschedule	GET	/getDepartmentsAppointmentScheduleById	0	public AppointmentScheduleSummary getDepartmentsAppointmentScheduleById(@QueryParam("scheduleId") Long scheduleId)
custom\AppointmentScheduleServiceImpl.java	/appointmentschedule	GET	/getDepartmentScheduleSummary	1	public List<AppointmentScheduleSummary> getDepartmentScheduleSummary(@QueryParam("unitId") Integer unitId, @QueryParam("doctorId") Integer doctorId,
custom\AppointmentScheduleServiceImpl.java	/appointmentschedule	GET	/getDeptsForTsu	0	public List<Departmentmaster> getDeptsForTsu()
custom\AppointmentScheduleServiceImpl.java	/appointmentschedule	GET	/getTelemedicinePatientUnit	0	public List<TeleconsultPatientUnitDetails> getTelemedicinePatientUnit()
custom\AppointmentScheduleServiceImpl.java	/appointmentschedule	GET	/getTelemedicinePatientUnitDetails	0	public Object[] getTelemedicinePatientUnitDetails() throws GenericApplicationException 
custom\AppointmentScheduleServiceImpl.java	/appointmentschedule	GET	/getTelemedicineSpecialityUnit	0	public List<Unitmaster> getTelemedicineSpecialityUnit(@QueryParam("departmentName") String departmentName)
custom\AppointmentScheduleServiceImpl.java	/appointmentschedule	GET	/getTelemedicineSpecialityUnitById	0	public List<Unitmaster> getTelemedicineSpecialityUnitById(@QueryParam("departmentId") Long departmentId)
custom\AppointmentScheduleServiceImpl.java	/appointmentschedule	GET	/getUnitsForTelemedicine	0	public List<Unitmaster> getUnitsForTelemedicine()
custom\AppointmentScheduleServiceImpl.java	/appointmentschedule	GET	/getUnitsForTelemedicineByDesc	0	public List<Unitmaster> getUnitsForTelemedicineById(@QueryParam("departmentName") String departmentName)
custom\AppointmentScheduleServiceImpl.java	/appointmentschedule	POST	/addOrRemoveSlots	0	public void addAndRemoveAppointmentSchedule(@HeaderParam("token") String token,
custom\AppointmentScheduleServiceImpl.java	/appointmentschedule	POST	/deleteAllDepartmentsSchedule	1	public void deleteAllDepartmentsSchedule(@QueryParam("isDoctorSlot") boolean isDoctorSlot) 
custom\AppointmentScheduleServiceImpl.java	/appointmentschedule	POST	/deleteDepartmentsAppointmentScheduleById	0	public Boolean deleteDepartmentsAppointmentScheduleById(@QueryParam("scheduleId") Long scheduleId) throws GenericApplicationException
custom\AppointmentScheduleServiceImpl.java	/appointmentschedule	POST	/getAppointmentSchedule	0	public Map<java.util.Date, Map<String, List<AppointmentSlot>>> getAppointmentScheduleService(
custom\AppointmentScheduleServiceImpl.java	/appointmentschedule	POST	/getAppointmentScheduleNew	1	public Map<java.util.Date, Map<String, List<AppointmentSlot>>> getAppointmentScheduleServiceNew(
custom\AppointmentScheduleServiceImpl.java	/appointmentschedule	POST	/getAppointmentScheduleTeleconsult	0	public Map<java.util.Date, Map<String, List<AppointmentSlot>>> getAppointmentScheduleTeleconsult(
custom\AppointmentScheduleServiceImpl.java	/appointmentschedule	POST	/getAvailableAppointmentSchedule	0	public DepartmentSlot getAvailableAppointmentSchedule(
custom\AppointmentScheduleServiceImpl.java	/appointmentschedule	POST	/getCabinWiseApointmentSlotCount	0	public List<Cabinmaster> getCabinWiseApointmentSlotCount(@BodyParam("body") AppointmentListByDoctorIDAndDateRangeDto body,
custom\AppointmentScheduleServiceImpl.java	/appointmentschedule	POST	/getCabinWiseSlotForTelemedicine	0	public List<Cabinmaster> getCabinWiseSlotForTelemedicine(@BodyParam("body") AppointmentListByDoctorIDAndDateRangeDto body,
custom\AppointmentScheduleServiceImpl.java	/appointmentschedule	POST	/getDepartmentAppointmentScheduleNew	0	public Map<java.util.Date, Map<String, List<AppointmentSlot>>> getDepartmentAppointmentScheduleServiceNew(
custom\AppointmentScheduleServiceImpl.java	/appointmentschedule	POST	/getDepartmentsByDate	0	public List<Departmentmaster> getDepartmentsByDate(@BodyParam("body") DoctorScheduleDto body) throws GenericApplicationException 
custom\BedManagementServiceImpl.java	/bedmanagement	GET	/checkBedNameExistsinMaster	0	public Number checkBedNameExistsinMaster(@QueryParam("description") String description, @QueryParam("wardId") Long wardId,
custom\BedManagementServiceImpl.java	/bedmanagement	GET	/checkDischargeClearanceCount	1	public Long clearanceCount() throws GenericApplicationException 
custom\BedManagementServiceImpl.java	/bedmanagement	GET	/getAdditionalServicesByAdmissionId	0	public PatientRegAdmissionDto getAdditionalServicesByAdmissionId(@QueryParam("admissionId") Long admissionId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\BedManagementServiceImpl.java	/bedmanagement	GET	/getAllCradles	0	public WardBedDto getAllCradles(@QueryParam("filter") String filter, @QueryParam("page") Integer page, @QueryParam("count") Integer count,
custom\BedManagementServiceImpl.java	/bedmanagement	GET	/getAvailableBeds	0	public List<ClassWardBedDto> getAvailableBeds() throws GenericApplicationException 
custom\BedManagementServiceImpl.java	/bedmanagement	GET	/getBedStatusById	0	public Long getBedStatusById(@QueryParam("id") Long id, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\BedManagementServiceImpl.java	/bedmanagement	GET	/getBlockSubmit	0	public Long udateBedStatus(@QueryParam("bedId") Long bedId, @QueryParam("unitId") Long unitId, @QueryParam("bedStatus") Long bedStatus) throws GenericApplicationException 
custom\BedManagementServiceImpl.java	/bedmanagement	GET	/getClearanceCount	0	public Long getClearanceCount() throws GenericApplicationException 
custom\BedManagementServiceImpl.java	/bedmanagement	GET	/getCradleCount	0	public Map<String, Long> getCradleCount(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\BedManagementServiceImpl.java	/bedmanagement	GET	/getDeadDischargeTransferPatientList	0	public List<DeathPatientSearchDto> getDeadDischargeTransferPatientList(@QueryParam("searchExp") String searchExp, @QueryParam("type") String type) throws GenericApplicationExceptio
custom\BedManagementServiceImpl.java	/bedmanagement	GET	/getOTTransferDetails	0	public List<TransferResultDto> getOTTransferDetails(@QueryParam("admId") @StoredProcNamedParam("p_admid") Long admId, @QueryParam("unitId") @StoredProcNamedParam("p_unitid") Long u
custom\BedManagementServiceImpl.java	/bedmanagement	GET	/getTransferDetails	0	public List<TransferResultDto> getTransferDetails(@QueryParam("admId") @StoredProcNamedParam("p_admid") Long admId, @QueryParam("unitId") @StoredProcNamedParam("p_unitid") Long uni
custom\BedManagementServiceImpl.java	/bedmanagement	GET	/isBedAvailable	0	public Boolean checkBedAvailability(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\BedManagementServiceImpl.java	/bedmanagement	POST	/addTempVerification	0	public String addTempVerificationDataToChiranjeeviApp(@BodyParam("body") PatientVerificationDto patientVerificationDto) throws GenericApplicationException 
custom\BedManagementServiceImpl.java	/bedmanagement	POST	/addVerification	0	public PatientVerificationDto addVerificationDataToChiranjeeviApp(@BodyParam("body") PatientVerificationDto patientVerificationDto) throws GenericApplicationException 
custom\BedManagementServiceImpl.java	/bedmanagement	POST	/getBabiesDetails	0	public Map<Long, List<PatientAdmissionListResultDto>> getBabiesDetails(@BodyParam("body") BabySearchDto babySearchDto) throws GenericApplicationException 
custom\BedManagementServiceImpl.java	/bedmanagement	POST	/getBedAvailabilityCount	0	public ShowBedAvailabilityDto getBedAvailabilityCount(@BodyParam("body") ShowBedAvailabilityDto body) throws GenericApplicationException 
custom\BedManagementServiceImpl.java	/bedmanagement	POST	/getBedCount	0	public Map<String,Long> getBedCount(@BodyParam("body") ClassMasterDetailsDto body, @QueryParam("available") boolean available, @QueryParam("age") Integer age,
custom\BedManagementServiceImpl.java	/bedmanagement	POST	/getBedDetails	0	public List<Bedmaster> getBedDetails(@BodyParam("body") ClassMasterDetailsDto body, @QueryParam("available") boolean available, @QueryParam("age") Integer age,
custom\BedManagementServiceImpl.java	/bedmanagement	POST	/getBuildingBedDetails	0	public List<ClassWardBedDto> getBuildingBedDetails(@BodyParam("body") ClassMasterDetailsDto body, @QueryParam("available") boolean available,
custom\BedManagementServiceImpl.java	/bedmanagement	POST	/getBuildingMasterDetails	0	public List<ClassWardBedDto> getBuildingMasterDetails(@BodyParam("body") ClassMasterDetailsDto body, @QueryParam("available") boolean available, @QueryParam("age") Integer age,
custom\BedManagementServiceImpl.java	/bedmanagement	POST	/getBuildingMasterDetailsNew	0	public List<WardBedDto> getBuildingMasterDetailsNew(@BodyParam("body") ClassMasterDetailsDto body, @QueryParam("available") Boolean available, @QueryParam("age") Integer age,
custom\BedManagementServiceImpl.java	/bedmanagement	POST	/getBuildingWardDetails	0	public List<ClassWardBedDto> getBuildingWardDetails(@BodyParam("body") ClassMasterDetailsDto body, @QueryParam("available") boolean available, @QueryParam("age") Integer age,
custom\BedManagementServiceImpl.java	/bedmanagement	POST	/getClassMasterDetails	0	public
custom\BedManagementServiceImpl.java	/bedmanagement	POST	/getIsRetainable	1	public Boolean getIsRetainable(@QueryParam("bedId") Long bedId, @QueryParam("wardId") Long wardId) throws GenericApplicationException 
custom\BedManagementServiceImpl.java	/bedmanagement	POST	/getMortuaryWards	0	public List<Wardmaster> getMortuaryWards() throws GenericApplicationException
custom\BedManagementServiceImpl.java	/bedmanagement	POST	/getOtBedCount	0	public Map<String, Long> getOtBedCount(@BodyParam("body") ClassMasterDetailsDto body, @QueryParam("available") boolean available, @QueryParam("age") Integer age, @QueryParam("nursi
custom\BedManagementServiceImpl.java	/bedmanagement	POST	/getPendencyDashboardData	0	public PendencyDashboardView getPendencyDashboardData(@QueryParam("departmentId") Long departmentId,@QueryParam("builduingId") Long builduingId, @QueryParam("wardId") Long wardId, 
custom\BedManagementServiceImpl.java	/bedmanagement	POST	/getWardDetails	0	public List<ClassWardBedDto> getWardBedDetails(@BodyParam("body") ClassMasterDetailsDto body, @QueryParam("available") boolean available, @QueryParam("age") Integer age,
custom\BedManagementServiceImpl.java	/bedmanagement	POST	/getWardDetailsForAdmissionBlockMaster	0	public List<WardBedDto> getWardDetailsForAdmissionBlockMaster(@BodyParam("body") ClassMasterDetailsDto body, @QueryParam("available") Boolean available,
custom\BedManagementServiceImpl.java	/bedmanagement	POST	/initiateDischargeClearance	0	public void initiateDischargeClearance(@BodyParam("body") DischargeClearanceDto body) throws GenericApplicationException 
custom\BedManagementServiceImpl.java	/bedmanagement	POST	/markLabClearance	1	public void markLabClearance(@BodyParam("body") MarkClearanceDto body) throws GenericApplicationException 
custom\BedManagementServiceImpl.java	/bedmanagement	POST	/printWardBedDetailsInquiry	0	public String[] printWardBedDetailsInquiry(@QueryParam("unitId") Long unitId, @QueryParam("deptId") Long deptId) throws GenericApplicationException 
custom\BillCancellationServiceImpl.java	/cancelBill	POST	/	0	public CancelBillDto billCancel(@BodyParam("body") CancelBillDto body, @Context HttpServletResponse response) throws GenericApplicationException 
custom\BillCancellationServiceImpl.java	/cancelBill	POST	/printCancelBillAudit	0	public void printCancelBillAudit(@BodyParam("body") RefundReceiptByRefundIdListDto body) throws GenericApplicationException 
custom\BillCancellationServiceImpl.java	/cancelBill	POST	/refundReceiptByIds	1	public BillReceiptView refundReceiptByIds(@BodyParam("body") RefundReceiptByRefundIdListDto body, @DefaultValue("false") @QueryParam("is_print") boolean print,
custom\BillDetServiceImpl.java	/billDet	POST	/getBillDetails	0	public TariffWiseClassRateNewDto findBillDetails(
custom\BillReceiptServiceImpl.java	/billReceipt	GET	/authorityBillPaidReceipt	1	public String authorityBillPaidReceipt(@QueryParam("paymentId") Long paymentId
custom\BillReceiptServiceImpl.java	/billReceipt	GET	/downloadActualFile	0	public void downloadReceipt(@QueryParam("file_name") String fileNameHash, @QueryParam("isBase64") Boolean isBase64, @Context HttpServletResponse response) throws GenericApplication
custom\BillReceiptServiceImpl.java	/billReceipt	GET	/downloadFile	0	public String downloadReceiptToken(@HeaderParam("token") String token, @QueryParam("file_name") String fileName, @QueryParam("cancel_bill_id") Long cancelBillId) throws GenericAppl
custom\BillReceiptServiceImpl.java	/billReceipt	GET	/downloadFileOpen	0	public byte[] downloadReceiptOpen(@QueryParam("file_name") String fileName, @QueryParam("cancel_bill_id") Long cancelBillId, @Context HttpServletResponse response) throws GenericAp
custom\BillReceiptServiceImpl.java	/billReceipt	GET	/printAuthorityPaymentDetail	0	public String printAuthorityPaymentDetail(@QueryParam("dashboardId")Long dashboardId, @QueryParam("unitId")Long unitId) throws GenericApplicationException 
custom\BillReceiptServiceImpl.java	/billReceipt	POST	/	0	public BillReceiptView getBillReceipt(@BodyParam("body") OPDBillReceiptDto body, @DefaultValue("false") @QueryParam("is_print") boolean print,
custom\BillReceiptServiceImpl.java	/billReceipt	POST	/advanceRefundReciept	1	public BillReceiptView getAdvanceRefundReceipt(@BodyParam("body") AdvanceRefundReceiptDto body, @DefaultValue("false") @QueryParam("is_print") boolean print, @Context HttpServletRe
custom\BillReceiptServiceImpl.java	/billReceipt	POST	/allAdvance	0	public List<AllAdvanceResultDto> getRefundReceipt(@BodyParam("body") AllAdvanceDto body) throws GenericApplicationException 
custom\BillReceiptServiceImpl.java	/billReceipt	POST	/billReceiptWithPharmacy	1	public BillReceiptView getBillReceiptWithPharmacy(@BodyParam("body") OPDBillReceiptDto body, @DefaultValue("true") @QueryParam("is_print") boolean print,
custom\BillReceiptServiceImpl.java	/billReceipt	POST	/cancelIpBill	1	public BillReceiptView getCancelIpReceipt(@BodyParam("body") CancelIpBillDto body, @DefaultValue("false") @QueryParam("is_print") boolean print, @Context HttpServletResponse respon
custom\BillReceiptServiceImpl.java	/billReceipt	POST	/cancelOpBill	1	public BillReceiptView getCancelOpReceipt(@BodyParam("body") CancelOpBillDto body, @DefaultValue("false") @QueryParam("is_print") boolean print, @Context HttpServletResponse respon
custom\BillReceiptServiceImpl.java	/billReceipt	POST	/downloadBillPharmacyRec	0	public PharmacyBillListView downloadBillPharmacyReceipt(@BodyParam("body") List<Long> getItemSaleIds, @Context HttpServletResponse response)
custom\BillReceiptServiceImpl.java	/billReceipt	POST	/getPatientPackageConsumption	0	public List<HealthplanPackageConsumptionDetailsDto> getPatientPackageConsumption(@BodyParam("body") List<HealthplanPackageConsumptionDetailsDto> body,
custom\BillReceiptServiceImpl.java	/billReceipt	POST	/ipdBillReceipt	0	public BillReceiptView getIPBillReceipt(@BodyParam("body") IPDBillReceiptDto body, @QueryParam("is_billReciept") boolean isBillReciept, @QueryParam("is_recieptList") boolean isReci
custom\BillReceiptServiceImpl.java	/billReceipt	POST	/printCreditNoteForIP	1	public CreditNote printCreditNoteForIP(@BodyParam("body") CreditNote creditNoteDtoObj, @DefaultValue("false") @QueryParam("is_print") boolean print) throws GenericApplicationExcept
custom\BillReceiptServiceImpl.java	/billReceipt	POST	/printCreditNoteForOP	1	public CreditNote printCreditNoteForOP(@BodyParam("body") CreditNote creditNoteDtoObj, @DefaultValue("false") @QueryParam("is_print") boolean print) throws GenericApplicationExcept
custom\BillReceiptServiceImpl.java	/billReceipt	POST	/printPackageRefundReceipt	1	public CreditNote printPackageRefundReceipt(@BodyParam("body") CreditNote body, @DefaultValue("false") @QueryParam("is_print") boolean print)
custom\BillReceiptServiceImpl.java	/billReceipt	POST	/refundReciept	0	public List<RefundReceiptResultDto> getRefundReceipt(@BodyParam("body") RefundReceiptDto body) throws GenericApplicationException 
custom\BillReceiptServiceImpl.java	/billReceipt	POST	/setIsBillReceiptPrintedToTrue	1	public void setIsBillReceiptPrintedToTrue(@QueryParam("billID") Long billID,@QueryParam("unitID") Long unitID) throws GenericApplicationException 
custom\BillSummaryServiceImpl.java	/billSummary	POST	/	0	public
custom\BillSummaryServiceImpl.java	/billSummary	POST	/draftBillSummary	0	public DraftBillView getDraftBillSummary(@BodyParam("body") PatientAdmissionListDto body, @DefaultValue("false") @QueryParam("is_print") boolean print,
custom\BillSummaryServiceImpl.java	/billSummary	POST	/getAllBills	1	public List<OPDBillReceiptResultNewDto> getAllBills(@BodyParam("body") BillListForSearch_NewDto body, @QueryParam("page") Integer page,
custom\BillSummaryServiceImpl.java	/billSummary	POST	/getAllBillsForOnline	0	public List<InvestigationAndBillResultDto> getAllBillsForOnline(@BodyParam("body") BillListForSearch_NewDto body, @QueryParam("page") Integer page,
custom\BillSummaryServiceImpl.java	/billSummary	POST	/getAllCombineBills	0	public List<BillListForSearch_NewResultDto_3> getAllCombineBills(@BodyParam("body") BillListForSearch_NewDto body) throws GenericApplicationException 
custom\BillSummaryServiceImpl.java	/billSummary	POST	/getAllCreditNoteBills	0	public List<BillListForSearch_CreditList> getAllCreditNoteBills(@BodyParam("body") CreditNoteBillListForSearch_NewDto body) throws GenericApplicationException 
custom\BillSummaryServiceImpl.java	/billSummary	POST	/getServiceListForPartialBillPayment	0	public BillSummaryView getServiceListForPartialBillPayment(@BodyParam("body") BillListForSearch_NewDto body) throws GenericApplicationException 
custom\CancelAdmissionServiceImpl.java	/canceladmission	POST	/cancel	1	public CancelAdmissionView cancelPatientAdmission(@BodyParam("body") CancelAdmissionView body) throws GenericApplicationException 
custom\DoctorListServiceImpl.java	/doctorlist	POST	/getDoctorMasterDetailsListbyDepartmentId	0	public Collection<DoctorMasterDetailsListResultDto_1_New> getDoctorMasterDetailsListbyDepartmentId(
custom\DoctorListServiceImpl.java	/doctorlist	POST	/getDoctorMasterDetailsListbyDoctorId	0	public Collection<DoctorMasterDetailsListResultDto_1_New> getDoctorMasterDetailsList(@BodyParam("body") DoctorMasterDetailsListDto body) throws GenericApplicationException 
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	GET	/activePatientAppointmentList	0	public List<PatientVisitHistoryDto> getActivePatientAppointmentList(
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	GET	/document/{id}	1	public String getImageById(@HeaderParam("token") String token, @PathParam("id") String id) throws GenericApplicationException 
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	GET	/documentActual/{id}	0	public void getImageById(@PathParam("id") String hashId, @Context HttpServletResponse response) throws GenericApplicationException 
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	GET	/fetchDrugInstruction	0	public String fetchDrugInstruction(@QueryParam("visitId") Long visitId, @QueryParam("patientId") Long patientId, @Context HttpServletResponse response) throws GenericApplicationExc
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	GET	/getAvaliableCabinAndBookedDoctorIds	0	public
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	GET	/getLastVisitDetails	1	public Encounter getLastVisitDetails(@QueryParam("patientId") Long patientId) throws GenericApplicationException 
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	GET	/getLastVisitStatus	1	public String getLastVisitStatus(@QueryParam("patientId") Long patientId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	GET	/getLastVisitTypeId	0	public Object[] getLastVisitTypeId(@QueryParam("patientId") Long patientId, @QueryParam("doctorId") Long doctorId) throws GenericApplicationException 
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	GET	/getMlcDetailsByVisit	0	public MLCDetails getMlcDetailsByVisit(@QueryParam("visitId") Long visitId)
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	GET	/getMrnoSeriesWithoutExistingMRNo	0	public List<String> getMrnoSeriesWithoutExistingMRNo(@QueryParam("placeOfBirthId") Long placeOfBirthId) throws GenericApplicationException 
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	GET	/getMrnSeries	0	public List<MrnSeries> getMrnoSeries(@QueryParam("placeOfBirthId") Long placeOfBirthId)
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	GET	/getSolrSyncStatus____	0	public String[] getSolrSyncStatus(@QueryParam("job") String jobName) 
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	GET	/getvisitTypes	1	public List<Visittypemaster> getvisitTypes(@QueryParam("isCheckinFlow") Boolean isCheckinFlow, @QueryParam("isTeleconsultOnly") Boolean isTeleconsultOnly,
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	GET	/indexMissingEncounterInSolr____	0	public void indexMissingEncounterInSolr(@QueryParam("opdipdid") Long opdipdid, @QueryParam("opdipd") Long opdipd, @QueryParam("opdipdno") String opdipdno) throws GenericApplication
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	GET	/indexMissingEncountersInSolr____	0	public void indexMissingEncountersInSolr(@QueryParam("days") Long days, @QueryParam("fromDate") Date fromDate,
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	GET	/indexMissingPatientInSolr____	0	public void indexMissingPatientInSolr(@QueryParam("mrno") String hid, @QueryParam("pid") Long pid) throws GenericApplicationException 
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	GET	/mlcFile/{id}	0	public String getImageByName(@HeaderParam("token") String token, @PathParam("id") String id) throws GenericApplicationException 
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	GET	/mlcFileActual/{id}	0	public void getImageByName(@PathParam("id") String hashId, @Context HttpServletResponse response) throws GenericApplicationException 
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	GET	/searchToken	0	public List<PatientToken> searchToken(@QueryParam("pat_token_no") String token) throws GenericApplicationException 
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	GET	/searchVisitbyToken	1	public PatientAppointmentandRegDto searchVisitbyToken(@QueryParam("pat_token_no") String token)
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	GET	/stopSolrSync____	0	public void stopSolrSync(@QueryParam("job") String jobName) throws GenericApplicationException 
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	POST	/aadharBiometricVarification	0	public Map<String, String> aadharBiometricVarification(@BodyParam("body") AadharObjectDto body)
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	POST	/aadharOtpGeneration	0	public Map<String, String> aadharOtpGeneration(@BodyParam("body") AadharObjectDto body) throws GenericApplicationException 
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	POST	/aadharOtpVarification	0	public Map<String, String> aadharOtpVarification(@BodyParam("body") AadharObjectDto body)
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	POST	/addEmergencyRegistration	1	public PatientAppointmentandRegDto addEmergencyRegistration(@BodyParam("body") PatientAppointmentandRegDto body, @QueryParam("pat_token_no") String token,
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	POST	/addFollowUpVisit	0	public void addFollowUpVisit(@BodyParam("body") AddFollowUpVisitDetailsDto body) throws GenericApplicationException 
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	POST	/addPatientPhoto	1	public AddPatientImageDto addPatientPhoto(@BodyParam("body") AddPatientImageDto body) throws GenericApplicationException 
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	POST	/addwalkinVisit	0	public PatientAppointmentandRegDto addwalkinVisit(@BodyParam("body") PatientAppointmentandRegDto body, @QueryParam("pat_token_no") String token,
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	POST	/callPatient	1	public void updateCallPatientData(@BodyParam("body") CallPatientDto body) throws GenericApplicationException 
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	POST	/callPatientNew	1	public void updateCallPatientDataNew(@BodyParam("body") CallPatientDto body) throws GenericApplicationException 
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	POST	/printAttendantPass	0	public String[] printAttendantPass(@BodyParam("body") SearchPatientAndVisitByNameMobNoResultDto body, @QueryParam("passNo") String passNo, @QueryParam("isReprint") Boolean isReprin
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	POST	/printBHTBarcode	0	public String[] printBHTBarcode(@BodyParam("body") SearchPatientAndVisitByNameMobNoResultDto body, @QueryParam("isVmPrint") Boolean isVmPrint, @DefaultValue("false") @QueryParam("i
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	POST	/printConsentForm	1	public String[] printConsentForm(@QueryParam("fileName") String fileName, @BodyParam("body") SearchPatientAndVisitByNameMobNoResultDto body) throws GenericApplicationException 
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	POST	/printDischargeSummary	1	public String[] printDischargeSummary(@BodyParam("body") SearchPatientAndVisitByNameMobNoResultDto body) throws GenericApplicationException 
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	POST	/printIpLable	0	public String[] printIpLable(@BodyParam("body") SearchPatientAndVisitByNameMobNoResultDto body) throws GenericApplicationException 
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	POST	/printPatientHealthCard	0	public Map<String, Object> printPatientHealthCard(@BodyParam("body") SearchPatientAndVisitByNameMobNoResultDto body) throws GenericApplicationException 
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	POST	/rislPrintIpLable	0	public Map<String, Object> rislPrintIpLable(@BodyParam("body") SearchPatientAndVisitByNameMobNoResultDto body) throws GenericApplicationException 
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	POST	/rislPrintOpLable	0	public Map<String, Object> rislPrintOpLable(@BodyParam("body") SearchPatientAndVisitByNameMobNoResultDto body) throws GenericApplicationException 
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	POST	/saveDrugInstruction	0	public void saveDrugInstruction(@QueryParam("visitId") Long visitId, @QueryParam("patientId") Long patientId, @BodyParam("data") String data) throws GenericApplicationException 
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	POST	/selfPatientRegistation	0	public PatientAppointmentandRegDto selfPatientRegistration(@BodyParam("body") PatientAppointmentandRegDto body) throws GenericApplicationException 
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	POST	/updateVisitType	1	public void updateVisitType(PatientAppointmentandRegDto body) throws GenericApplicationException 
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	PUT	/doctorcheckout	1	public void updateDoctorCheckout(@QueryParam("appId") Long appointmentId) throws GenericApplicationException 
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	PUT	/releaseCabin	0	public List<GetChargesResultDto> releaseCabin(@QueryParam("cabinId") Long cabinId, @QueryParam("visitId") Long visitId) throws GenericApplicationException 
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	PUT	/updateCabinandDoctId	1	public void updateCabinandDoctId(@BodyParam("body") CabinAssignAppointDto body) throws GenericApplicationException 
custom\EmergencyAndWalkinVisitServiceImpl.java	/walkinVisit	PUT	/updateMlcDetailsbyVisitId	0	public void updateMlcDetailsbyVisitId(@BodyParam("body") MLCDetails body)
custom\InventoryEAushadiIntegrationImpl.java	/eAushadiIndent	GET	/getConsumptionDataForItemSalePatients	0	public List<ItemSaleConsumptionView> getItemConsumptionData(@QueryParam("unitId") Long unitId, @QueryParam("closingDate") java.sql.Date closingDate,@QueryParam("amountRange") Long 
custom\InventoryEAushadiIntegrationImpl.java	/eAushadiIndent	GET	/getKaysonsCurrentStockDetails	1	public List<EAushadhiCurrentStockResponseDto> getCurrentStockKaysonsDetails(@QueryParam("storeId") Long storeId) throws GenericApplicationException 
custom\InventoryEAushadiIntegrationImpl.java	/eAushadiIndent	POST	/addCurrentStockDetailsByStoreId	0	public List<EAushadhiCurrentStockResponseDto> addCurrentStockDetailsByStoreId(@QueryParam("isReSync") @DefaultValue("false") Boolean isReSync) throws GenericApplicationException 
custom\InventoryEAushadiIntegrationImpl.java	/eAushadiIndent	POST	/addOpeningBalanceForKaysonsStock	0	public void addOpeningBalanceForKaysonsStock(@BodyParam("body") List<EAushadhiCurrentStockResponseDto> body) throws GenericApplicationException, ParseException 
custom\InventoryEAushadiIntegrationImpl.java	/eAushadiIndent	POST	/createunMappedStores	0	public Boolean createunMappedStores(@BodyParam("body") Store body) throws GenericApplicationException 
custom\InventoryEAushadiIntegrationImpl.java	/eAushadiIndent	POST	/getItemStockListByStoreId	0	public List<ItemConsumptionView> getItemStockListByStoreId(@BodyParam("body") CurrentUnitItemStockListSearchDto body) throws GenericApplicationException 
custom\InventoryEAushadiIntegrationImpl.java	/eAushadiIndent	POST	/indent	0	public IndentEAushadiResponse indent(@BodyParam("body") IndentEAushadiDto body) throws Exception 
custom\InventoryEAushadiIntegrationImpl.java	/eAushadiIndent	POST	/indentIssueSucces	1	public IndentReturnEAushadiResponse indentIssueSucces(@BodyParam("body") IndentReturnItemListToEAushadi body) throws Exception 
custom\InventoryEAushadiIntegrationImpl.java	/eAushadiIndent	POST	/returnIndentAfterApproveForEaushadi	1	public IssueView returnIndentAfterApproveForEaushadi(@QueryParam("isEAushadhi") @DefaultValue("false") boolean isEAushadhi, @QueryParam("issueId") Long issueId) throws Exception 
custom\InventoryEAushadiIntegrationImpl.java	/eAushadiIndent	POST	/returnIndentForEaushadi	1	public IssueView returnIndentForEaushadi(@BodyParam("body") List<IssueItemWithoutIndent> body, @QueryParam("isEAushadhi") @DefaultValue ("false") boolean isEAushadhi) throws Except
custom\InventoryEAushadiIntegrationImpl.java	/eAushadiIndent	POST	/returnItemUpdate	1	public IndentIssueEAushadiResponse returnItemUpdate(@BodyParam("body") IndentreturnStatusEaushadi body) throws GenericApplicationException 
custom\InventoryEAushadiIntegrationImpl.java	/eAushadiIndent	POST	/sendOTPToUserForKaysonsStockUpdate	0	public String sendOTPToUserForKaysonsStockUpdate() throws GenericApplicationException 
custom\InventoryEAushadiIntegrationImpl.java	/eAushadiIndent	POST	/updateEAushadhiItemMaster	1	public void updateEAushadhiItemMaster() 
custom\InventoryEAushadiIntegrationImpl.java	/eAushadiIndent	POST	/updateQCStatus	1	public IndentIssueEAushadiResponse updateQCStatusAgainstItemBatch(@BodyParam("body") List<EAushadhiQCStatus> body) throws Exception 
custom\InventoryEAushadiIntegrationImpl.java	/eAushadiIndent	POST	/validateOTPToUserForKaysonsStockUpdate	0	public String validateOTPToUserForKaysonsStockUpdate(@QueryParam("otpVal") String otp) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	DELETE	/removeItemGroupCategoryForSupplier	0	public boolean deleteMedicationOrder(@QueryParam("supplierId") Long supplierId, @QueryParam("groupId") Long groupId, @QueryParam("categoryId") Long categoryId) throws GenericApplic
custom\InventoryServiceImpl.java	/inventoryStockList	DELETE	/updatePurchaseOrder	1	public DeletePurchaseOrderDetailsDto deletePurchaseOrderDetails(@BodyParam("body") DeletePurchaseOrderDetailsDto body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/addSupplierItemsByItemCategory	0	public boolean addSupplierItemsByItemCategory(@QueryParam("itemCategoryIdList") String itemCategoryIdListStr, @QueryParam("supplierId") Long supplierId) throws GenericApplicationEx
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/addSupplierItemsByItemGroup	0	public boolean addSupplierItemsByItemGroup(@QueryParam("groupIdList") String groupIdListStr, @QueryParam("supplierId") Long supplierId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/checkItemCode	1	public boolean checkItemCode(@QueryParam("itemCode") String itemCode) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/checkLastGrnDetails	0	public List<PreviousPOGRNDetailsDto> showLastGrnDetails(@QueryParam("itemId") Long itemId, @QueryParam("storeId") Long storeId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/checkPORaisedDetails	0	public List<PreviousPOGRNDetailsDto> checkPORaisedDetails(@QueryParam("itemId") Long itemId, @QueryParam("storeId") Long storeId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/checkReffItemIdForAsset	1	public Number checkReffItemIdForAsset (@QueryParam("itemId") Long itemId, @QueryParam("requestedUnitId") Long requestedUnitId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/deletePlanById	0	public String deletePlanId(@QueryParam("planId") Long planId, @QueryParam("isPlanService") Boolean isPlanService, @QueryParam("isPlanItem") Boolean isPlanItem,
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/deleteRateContractItemDetails	1	public Long deleteRateContractItemDetails(@QueryParam("rateContractDetailsId") Long rateContractDetailsId, @QueryParam("unitId") Long unitId) 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/freezItemBatchStatus	1	public boolean freezItemBatchStatus(@QueryParam("itemId") Long itemId, @QueryParam("batchId") Long batchId, @QueryParam("isFreez") boolean isFreez, @QueryParam("remarks") String re
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getAllApprovalCount	0	public Map<String,Number> getAllApprovalCount(@QueryParam("storeId") Long storeId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getAllItemByNameforStore	0	public List<ItemByNameResultDto> getItemByNameforStoreAll(@QueryParam("searchText") @StoredProcNamedParam("p_searchtext") String itemName,
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getAllItemCategories	0	public List<Itemcategory> getAllItemCategories() throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getAllStoresBySameCategoryId	0	public List<StoreListByCategoryResultDto> getDirectItemIssueList(@QueryParam("storeId") Long storeId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getAllStoresItemCategiries	1	public List<StoreItemCategoryDetailsDto> getAllStoresItemCategiries() throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getAttachedItemsBySupplierAndCategory	0	public SupplierItemResultDto getAttachedItemsBySupplierAndCategory(@QueryParam("supplierId") Long supplierId, @QueryParam("categoryIdStr") String categoryIdStr)
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getBatchDetails	0	public BatchDetailsResultDto getBatchDetails(@QueryParam("batchCode") String batchCode, @QueryParam("unitId") Long unitId, @QueryParam("storeId") Long storeId,
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getBatchNoForAsset	0	public List<String> getBatchNoForAsset(@QueryParam("itemId") Long itemId, @QueryParam("brandId") Long brandId, @QueryParam("modalNo") String modalNo) 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getBatchNoForAssetIssueIndent	0	public Map<Long,List<String>> getBatchNoForAssetIssueIndent(@QueryParam("itemId") Long itemId, @QueryParam("brandId") Long brandId, @QueryParam("modalNo") String modalNo) 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getBatchWiseDocument	1	public PatientIDDocumentDto getBatchWiseDocument(@QueryParam("batchId") Long batchId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getBrandIdForAsset	0	public List<String> getBrandIdForAsset(@QueryParam("itemId") Long itemId , @QueryParam("batchNo") String batchNo,@QueryParam("modalNo") String modalNo) 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getBrandIdForAssetIssueIndent	0	public Map<Long,List<String>> getBrandIdForAssetIssueIndent(@QueryParam("itemId") Long itemId, @QueryParam("brandId") Long brandId, @QueryParam("batchNo") String batchNo) 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getConsumptionDataForUnit	0	public List<ItemConsumptionView> getItemConsumptionData(@QueryParam("unitId") Long unitId, @QueryParam("closingDate") java.sql.Date closingDate) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getDepartmentById	1	public List<Unitdepartmentdetails> getDepartmentDetailsById(@QueryParam("id") Long id) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getEAushadiItemStockDetails	1	public EAushadiItemStock getEAushadiItemStockDetails(@QueryParam("eAushadiItemId") Long eAushadiItemId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getGRNItemsByGrnId	0	public List<GRNItemListResultDto> getGRNItemsByItemId(@QueryParam("grn_id") Long grnId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getGrnItemsDetails	1	public List<GRNResultRptforDirectGRNDto> getGrnItemsDetails(@QueryParam("storeId") Long storeId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getHospitalWisePurchaseRequestList	0	public List<HospitalWisePurchaseRequestDto> getHospitalWisePurchaseRequestList(@QueryParam("toStoreId") Long toStoreId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getIndentIssueDetailsById	1	public List<GetIssueItemDetailsResultDto> getIndentIssueDetailsById(@QueryParam("indent_id") @StoredProcNamedParam("p_indentid") Long indentId,
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getIndentListByStoreIdforIndent	1	public ShowItemListforIndentView getItemByNameforIndent(@QueryParam("searchText") @StoredProcNamedParam("p_searchtext") String itemName,
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getIssueItemsDetailsById	1	public List<GetIssueItemDetailsforItemReceiveListResultDto> getIssueItemsDetailsById(@QueryParam("issue_id") @StoredProcNamedParam("p_issueid") Long issueId,
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getIssueItemsDetailsByIdForRecive	0	public IssueItemDetailsView getIssueItemsDetailsByIdForRecive(@QueryParam("issue_id") @StoredProcNamedParam("p_issueid") Long issueId,
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getIssueNumberforGRN	0	public List<Object[]> getIssueNumberforGRN(@QueryParam("unitid") Long unitid, @QueryParam("storeid") Long storeid) 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getItemByNameforAssetStore	1	public List<ItemByNameResultEADto> getItemByNameforAssetStore(@QueryParam("searchText") @StoredProcNamedParam("p_searchtext") String itemName,
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getItemByNameforDirectPO	0	public List<ItemByNameResultDto> getItemByName(@QueryParam("searchText") @StoredProcNamedParam("p_searchtext") String itemName,
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getItemByNameforStore	0	public List<ItemByNameResultEADto> getItemByNameforStore(@QueryParam("searchText") @StoredProcNamedParam("p_searchtext") String itemName,
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getItemByNameforStoreFormEaushadiItems	0	public List<ItemByNameResultEADto> getItemByNameforStoreFormEaushadiItems(@QueryParam("searchText") @StoredProcNamedParam("p_searchtext") String itemName, @QueryParam("storeId") @S
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getItemCategoriesByItemGroup	1	public List<Itemcategory> getItemCategoriesByItemGroup(@QueryParam("itemGroupId") Long itemGroupId, @QueryParam("storeId") Long storeId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getItemCategorysByGroupId	0	public List<ItemCategoryResultDto> getItemCategorysByGroupId(@QueryParam("groupId") Long groupId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getItemCategorysByGroupIdAndStoreId	0	public List<StoreListByCategoryResultDto> getItemCategorysByGroupIdAndStoreId(@QueryParam("groupId") Long groupId, @QueryParam("storeId") Long storeId) throws GenericApplicationExc
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getItemCurrentPurchaseOrderIndentList	1	public List<CurrentPurchaseOrderIndentListResultDto> getItemCurrentPurchaseOrderIndentList() throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getItemCurrentReorderListforIndent	0	public ShowItemListforIndentReorderView getItemCurrentReorderListforIndent(@QueryParam("unitId") @StoredProcNamedParam("p_unitid") Long unitId,
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getItemDetailsByBatchForOpBal	0	public BillReceiptView getItemDetailsByBatchForOpBal(@DefaultValue("false") @QueryParam("is_print") boolean print, @QueryParam("batchId") Long batchId,
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getItemDetailsByBatchId	0	public BillReceiptView getItemDetailsByBatchId(@DefaultValue("false") @QueryParam("is_print") boolean print, @QueryParam("batchId") Long batchId,
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getItemGroupByStoreId	1	public List<Itemgroup> getItemGroupByStoreId(@QueryParam("storeId") Long storeId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getItemLatestBatchDetails	0	public Itembatchmaster getItemLatestBatchDetails (@QueryParam("itemId") Long itemId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getItemListbySupplierId	1	public List<ItemListbySupplierIdResultDto> getItemListbySupplierId(@QueryParam("searchText") @StoredProcNamedParam("p_searchtext") String searchText,
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getItemNameBySupplier	1	public List<ItemBySupplierResultDto> getItemByNameBySupplier(@QueryParam("searchText") @StoredProcNamedParam("p_searchtext") String itemName,
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getItemsByCategoryList	0	public List<Itemmaster> getItemGroupList(@QueryParam("categoryIdList") Long[] categoryIdList) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getItemsByItemCategory	0	public List<ItemSupplierForRateContractDto> getItemsByItemCategory(@QueryParam("itemCategoryId") Long itemCategoryId, @QueryParam("storeId") Long storeId,
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getItemsByItemCategoryWithoutRateContract	0	public List<ItemSupplierForRateContractDto> getItemsByItemCategoryWithoutRateContract(@QueryParam("itemCategoryId") Long itemCategoryId) 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getItemsEnquiryDetails	1	public List<GetItemsEnquiryDetailsResultDto_1> getItemsEnquiryDetails(@QueryParam("id") @StoredProcNamedParam("p_id") Long id, @QueryParam("unitId") @StoredProcNamedParam("p_unitid
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getItemsEnquiryDetailsAndSuppliers	0	public GetEnquiryDetailsView getEnquiryDetailsViewItemsEnquiryDetailsAndSuppliers(@QueryParam("id") @StoredProcNamedParam("p_id") Long id,
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getItemsEnquirySuppliers	1	public List<EnquirySuppliersResultDto> getItemsEnquirySuppliers(@QueryParam("id") @StoredProcNamedParam("p_id") Long id, @QueryParam("unitId") @StoredProcNamedParam("p_unitid") Lon
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getItemSingleAccessionNum	0	public boolean getItemSingleAccessionNum(@QueryParam("itemId") Long itemId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getItemWisePurchaseRequestList	0	public List<HospitalWisePurchaseRequestDto> getItemWisePurchaseRequestList(@QueryParam("toStoreId") Long toStoreId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getModalForAsset	1	public List<String> getModalForAsset(@QueryParam("itemId") Long itemId, @QueryParam("brandId") Long brandId, @QueryParam("batchNo") String batchNo) 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getModalForAssetIssueIndent	0	public Map<Long,List<String>> getModalForAssetIssueIndent(@QueryParam("itemId") Long itemId, @QueryParam("brandId") Long brandId, @QueryParam("batchNo") String batchNo) 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getNAIssueItemsDetailsByIdForRecive	1	public IssueItemDetailsView getNAIssueItemsDetailsByIdForRecive(@QueryParam("indentId") Long indentId, @DefaultValue("false") @QueryParam("is_print") boolean print) throws GenericA
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getPoNumberforGRN	0	public List<String> getPoNumberforGRN(@QueryParam("unitid") Long unitid, @QueryParam("storeid") Long storeid, @QueryParam("supplierid") Long supplierid) throws GenericApplicationEx
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getPoNumberForPoScanGrn	0	public PurchaseOrderDto getPoNumberForPoScanGrn(@QueryParam("unitid") Long unitid, @QueryParam("storeid") Long storeid, @QueryParam("pono") String pono) throws GenericApplicationEx
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getPOTermsAndCondionsBySupplierID	1	public List<Termandcondition> getPOTermsAndCondionsBySupplierID(@QueryParam("supplierId") Long supplierId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getPurchaseIndentItemsDetails	0	public List<CurrentPurchaseOrderIndentDetailsResultDto> getPurchaseIndentItemsDetails(@QueryParam("poIndentId") @StoredProcNamedParam("p_poindentid") Long poIndentId)
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getPurchaseRequestDetailsById	0	public PurchaseRequest getPurchaseRequestDetailsById(@QueryParam("requestId") Long requestId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getQuotationList	1	public List<GetItemsEnquiryDetailsResultDto> getAvailableQuotationList(@QueryParam("id") @StoredProcNamedParam("p_id") Long id, @QueryParam("unitId") @StoredProcNamedParam("p_uniti
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getRateContractByContractId	1	public RateContract getRateContractByContract(@QueryParam("rateContractId") Long rateContractId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getRateContractDetailsByContractId	1	public List<RateContractDetails> getRateContractByContractId(@QueryParam("rateContractId") Long rateContractId, @QueryParam("unitId") Long unitId) throws GenericApplicationExceptio
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getRateContractList	0	public List<RateContract> getRateContractList(@QueryParam("expireStatus") boolean expireStatus) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getSelectedItemCategories	1	public List<Long> getSelectedItemCategories(@QueryParam("supplierId") Long supplierId) throws GenericApplicationException
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getStateIdByDistrict	0	public Long getStateIdByDistrict(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getStockAdjustmentDetailsPrint	0	public StockAdjustmentDetailsPrintView getStockAdjustmentDetailsPrint(@QueryParam("stockAdjustmentNo") String stockAdjustmentNo) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getStoreCategoryIdByStoreId	1	public List<StoreListByCategoryResultDto> getStoreCategoryIdByStoreId(@QueryParam("storeId") Long storeId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getStoreIdByEashadiStoreId	1	public Long getStoreIdByEashadiStoreId(@QueryParam("eashadiStoreId") Long eashadiStoreId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getStoreItemCategoryMappingDetails	0	public List<StoreItemCategoryDetailsDto> getStoreItemCategoryMappingDetails(@QueryParam("storeId") Long storeId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getStoreMasterById/{id}	1	public Store getStoreMasterById(@PathParam("id") Long id) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getSupplierItemCategoriesBySupplier	1	public List<Itemcategory> getSupplierItemCategoriesBySupplier(@QueryParam("supplierId") Long supplierId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getSupplierListForRateContract	1	public List<Supplier> getSupplierListForRateContract() throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getSuppliersByItemId	1	public List<GetSupplierListByItemIdResultDto> getSupplierListByItemId(@QueryParam("itemId") @StoredProcNamedParam("p_itemid") Long itemId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getUnitMappedCentralStores	0	public List<Store> getUnitMappedCentralStores() throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getUnitStoreListNewIndent	0	public List<UnitStoreListResultDto> getUnitStoreListNewIndent(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getWholePlanMaster	0	public Planmaster getwholePlanMasterId(@QueryParam("planId") Long planId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/getZonebyUnitId	1	public Long getZonebyUnitId(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/printBarCodeGeneric	0	public String[] printBarCodeGeneric(@QueryParam("barCode") String barCode, @QueryParam("name") String name, @QueryParam("age") String age, @QueryParam("gender") String gender,
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/printItemEnquiryDetails	0	public GetEnquiryDetailsView printItemEnquiryDetails(@QueryParam("id") @StoredProcNamedParam("p_id") Long id, @QueryParam("unitId") @StoredProcNamedParam("p_unitid") Long unitId)
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/startEAushadhiItemUpdateJob	0	public void startEAushadhiItemUpdateJob() throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/storeCategoryMapping	0	public String storeCategoryMapping(@QueryParam("storeId") Long storeId, @QueryParam("categoryIds") String catagoryIds) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	/updateOpRequestFlag	0	public Boolean updateOpRequestFlag(@QueryParam("poID") Long poID, @QueryParam("flagValue") Long flagValue) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	GET	getbatchNoSearch	0	public List<Itembatchmaster> getItembatchmaster(@QueryParam("batchcode") String batchcode, @QueryParam("isUnitWise") @DefaultValue("false") Boolean isUnitWise) throws GenericApplic
custom\InventoryServiceImpl.java	/inventoryStockList	GET	getEAushadhiIndentIssueDetails	1	public IndentIssueEaushadi getEAushadhiIndentIssueDetails(@QueryParam("eAushadhiIndentNo") Long eAushadhiIndentNo, @QueryParam("storeId") Long storeId) throws GenericApplicationExc
custom\InventoryServiceImpl.java	/inventoryStockList	GET	getUploadedMaterialConsDocuments	0	public PatientIDDocumentDto getUploadedMaterialConsDocuments(@QueryParam("materialConsNo") String materialConsNo) throws GenericApplicationException
custom\InventoryServiceImpl.java	/inventoryStockList	GET	getUploadedStckAdjDocuments	1	public PatientIDDocumentDto getUploadedStckAdjDocuments(@QueryParam("stockAdjustmentNo") String stockAdjustmentNo) throws GenericApplicationException
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/addAddPOIndent	0	public PurchaseOrderView addAddPOIndent(@BodyParam("body") PurchaseOrderView body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/addAddPOOrder	1	public PurchaseOrderView addPurchaseOrder(@BodyParam("body") PurchaseOrderView body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/addAddPOOrderIndent	0	public PurchaseOrderIndentView addAddPOOrderIndent(@BodyParam("body") PurchaseOrderIndentView body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/addConsignmentGRN	1	public AddConsignmentGRNView addConsignmentGRN(@BodyParam("body") AddConsignmentGRNView body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/addExternalCosumptionStock	0	public void addExternalCosumptionStock(@BodyParam("body") List<ItemStockDto> body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/addGRN	1	public AddGRNView addGRN(@BodyParam("body") AddGRNView body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/addGRNReturn	1	public GrnReturnView addGRNReturn(@BodyParam("body") GrnReturnView body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/addIndent	1	public AddIndentView addIndent(@BodyParam("body") AddIndentView body, @QueryParam("isEAushadiUnit") boolean isEAushadiUnit, @QueryParam("isAutoIndent") @DefaultValue ("false") bool
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/addIndentInvoiceDetails	0	public IndentInvoice addIndentInvoiceDetails(@BodyParam("body") CompleteIndentView body , Long isReturnIndent) throws GenericApplicationException, ParseException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/addItemforConsumption	0	public AddItemforConsumptionView addItemforConsumption(@BodyParam("body") AddItemforConsumptionView body) throws GenericApplicationException, IOException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/addItemGroup	0	public void addItemGroup(@BodyParam("body") UpdateItemGroupDto body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/addItemMaster	1	public void addItemMaster(@BodyParam("body") InsertItemMasterDto body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/addItemStockAdjustmentDetails	1	public List<ItemStockAdjustmentDetailsDto> addItemStockAdjustmentDetails(@BodyParam("body") List<ItemStockAdjustmentDetailsDto> body) throws GenericApplicationException, IOExceptio
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/addItemSupplier	0	public void addItemSupplier(@BodyParam("body") List<AddItemSupplierDto> itemSupplierDtos) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/addItemTax	1	public void addOrUpdateItemTax(@BodyParam("body") ItemTaxDto itemTaxDto, @DefaultValue("true") @QueryParam("is_new") boolean isNew) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/addNewItemToRateContract	0	public Long addNewItemToRateContract(@BodyParam("body") List<RateContractDetails> rateContractDetailsList) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/addOpeningBalItems	0	public AddOpeningBalView addOpeningBalApi(@BodyParam("body") AddOpeningBalView body,@QueryParam("isExternalParam") boolean isExternalParam) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/addPOOrder	1	public List<GetItemResultDto> getPOOrder(@BodyParam("body") GetItemDto body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/addProvisionalGRN	1	public AddGRNView saveProvisionalGrn(@BodyParam("body") AddGRNView body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/addRateContract	0	public Boolean addRateContract(@BodyParam("body") RateContract rateContract) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/addReasonForStockAdjustment	1	public Boolean addReasonForStockAdjustment(@BodyParam("body") ItemStockAdjustmentDetailsDto body)
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/addtemEnquiry	0	public ItemEnquiryView addtemEnquiry(@BodyParam("body") ItemEnquiryView body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/approvalForIndentIssue	1	public void approvalForIndentIssue(@BodyParam("body") CompleteIndentView body ,@QueryParam("indentId") Long indentId,@QueryParam("status") Long status) throws GenericApplicationExc
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/approveIndentLists	1	public void approveIndentLists(@BodyParam("indentdetailByIdResultDto") List<IndentdetailByIdResultDto> indentdetailByIdResultDto) throws Exception 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/approveProvisionalGRN	1	public AddGRNView approveProvisionalGRN(@BodyParam("body") AddGRNView body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/approveProvisionalGRNNew	1	public UpdateGRNDto approveProvisionalGRNNew(@BodyParam("body") AddGRNView body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/cancelReturnIndent	1	public Boolean cancelReturnIndent(@QueryParam("issueId") Long issueId) throws Exception 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/completeOpeningBalItems	1	public AddOpeningBalView completeOpeningBal(@BodyParam("body") List<AddItemBatchDto> body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/convertConsignedGRNToGRN	0	public AddConsignmentGRNView convertConsignedGRNToGRN(@BodyParam("body") AddConsignmentGRNView body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/freezePurchaseOrder	0	public FreezPurchaseOrderDto freezPurchaseOrder(@BodyParam("body") FreezPurchaseOrderDto body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/freezIndentLists	1	public void freezIndentLists(@BodyParam("body") List<IndentdetailByIdResultDto> indentdetailByIdResultDto) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getAccesionByModalNoOrBrandId	0	public List<Object[]> getAccesionByModalNoOrBrandId(@BodyParam("body") IndentdetailByIdDto body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getAllProductItemsByStoreIdAndGroupIdAndItemCategoryId	0	public List<ItemClinicDetailsDto> getAllProductItemsByStoreIdAndGroupIdAndItemCategoryId(@BodyParam("body") ItemClinicDetailsDto body, @QueryParam("page") Integer page,
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getAllProductItemsByStoreIdAndGroupIdFromSafetyAndReorderMst	1	public List<ReorderAndSafetyMaster> getAllProductItemsByStoreIdAndGroupIdFromSafetyAndReorderMst(@BodyParam("body") ItemClinicDetailsDto body, @QueryParam("page") Integer page,
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getApprovGRNListForCount	1	public List<ApproveGrnListForCount> getApprovGRNListForCount(@BodyParam("body") GRNListForSearchDto body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getApprovGRNListForSearch	1	public List<GRNListForSearchResultDto> getApproveGRNListForSearch(@BodyParam("body") GRNListForSearchDto body, @QueryParam("status") Long status,
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getCategoryCountOfStockList	1	public List<StockListCategoryCountResultDto> getCategoryCountOfStockList(@BodyParam("body") CurrentUnitItemStockListSearchDto body, @DefaultValue("false") @QueryParam("is_print") b
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getConsignedGRNList	0	public List<ConsignmentGRNResultDtoNew> getConsignedGRNList(@BodyParam("body") ConsignmentGRNListForSearchDto body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getConsignmentGRNDetails	0	public List<ConsignmentGRNItemsListResultDto> getConsignmentGRNDetails(@BodyParam("body") ConsignmentGRNItemsListDto body, @DefaultValue("false") @QueryParam("is_print") boolean pr
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getCurrentStockForRecallBatchWise	1	public String getCurrentStockForRecallBatchWise(@BodyParam("body") GetcurrentStockBatchWiseDto body, @DefaultValue("0L") @QueryParam("batchId") Long batchId) throws GenericApplicat
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getCurrentUnitItemStockList	1	public List<ItemCurrentStockListResultNewDto> getItemCurrentUnitStockList(@BodyParam("body") CurrentUnitItemStockListSearchDto body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getDirectIssueListByStoreId	0	public List<DirectItemIssueListResultDto> getDirectItemIssueList(@BodyParam("body") DirectItemIssueListResultDto body,
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getDirectItemIssueListByStoreId	0	public List<IssueItemDetailsListResultDto> getDirectItemIssueListByStoreIdforItemIssue(@BodyParam("body") IndentListByStoreIdDto body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getGRNForGrnNosforDirectGRN	0	public List<GRNResultRptforDirectGRNDto> getGRNForGrnNosforDirectGRN(@BodyParam("body") GRNRptDto body, @DefaultValue("false") @QueryParam("is_print") boolean print)
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getGRNForGrnNoss	0	public List<GRNResultRptforDirectGRNDto> getGRNForGrnNo(@BodyParam("body") GRNRptDto body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getGRNListForSearch	1	public List<GRNListForSearchResultDto> getGRNListForSearch(@BodyParam("body") GRNListForSearchDto body,
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getGRNListForSearchByItemId	0	public List<GRNListForSearchResultDto> getGRNListForSearchByItemId(@BodyParam("body") GRNListForSearchDto body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getGRNListForSearchPre	0	public List<GRNListForSearchResultDto> getGRNListForSearchPre(@BodyParam("body") GRNListForSearchDto body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getIndentDetailsById	1	public List<IndentItemDetailView> getIndentDetailsById(@BodyParam("body") IndentdetailByIdDto body, @DefaultValue("false") @QueryParam("is_print") boolean print,
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getIndentDetailsforItemIssue	1	public List<IndentItemDetailView> getIndentDetailsforItemIssue(@BodyParam("body") IndentdetailByIdDto body, @DefaultValue("false") @QueryParam("is_print") boolean print, @DefaultVa
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getIndentListByStoreId	0	public List<IndentListByStoreIdResultDto> getIndentListByStoreId(@BodyParam("body") IndentListByStoreIdDto body, @QueryParam("search") String search,
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getIndentListByStoreIdforItemIssue	0	public List<IndentListByStoreIdResultDto> getIndentListByStoreIdforItemIssue(@BodyParam("body") IndentListByStoreIdDto body, @QueryParam("search") String search,
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getIssuedPurchaseOrders	0	public List<IssuedPurchaseOrderResultDto> getIssuedPurchaseOrders(@BodyParam("body") IssuedPurchaseOrdersSearchDto body) 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getItemCurrentPurchaseOrderList	1	public List<CurrentPurchaseOrderListResultDto> getItemCurrentPurchaseOrderList(@BodyParam("body") CurrentPurchaseOrderListDto body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getItemCurrentPurchaseOrderListforOtherUnit	0	public List<CurrentPurchaseOrderListResultDto> getItemCurrentPurchaseOrderListforOtherUnit(@BodyParam("body") CurrentPurchaseOrderListDto body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getItemCurrentReorderforDirectIssue	0	public List<ItemBatchListForSearchwithTaxResultDto> getItemCurrentReorderforDirectIssue(@BodyParam("body") DirectIssueItemDto body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getItemCurrentReorderListforIndentDetails	1	public List<CurrentItemReoderforIndentResultDto> getItemCurrentReorderListforIndentDetails(@BodyParam("body") List<CurrentItemReoderforIndentDto> body) throws GenericApplicationExc
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getItemCurrentStockList	0	public List<ItemCurrentStockListResultNewDto> getItemCurrentStockList(@BodyParam("body") CurrentUnitItemStockListSearchDto body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getItemCurrentStockListByStore	0	public List<GetCurrentStockListByStoreIdDto> getItemCurrentStockListByStore(@QueryParam("storeId") Long storeId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getItemCurrentStockListData	1	public List<ItemStockView> getItemCurrentStockListDat(@BodyParam("body") ItemCurrentStockListDto body, @DefaultValue("false") @QueryParam("is_print") boolean print)
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getItemgroup	0	public List<ItemGroupResultDto> getItemGroup(@BodyParam("body") ItemGroupDto body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getItemInfoforReorderIndent	1	public Map<Long, Map<Long, Map<Long, List<ItemReorderListNewResultDto>>>> getItemInfoforReorderIndent(@BodyParam("body") ItemCurrentStockListAvailableDto body) throws GenericApplic
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getItemMasterList	0	public List<ItemMasterSearchListResultDtoNew> getItemMasterList(@BodyParam("body") ItemMasterSearchListDto body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getItemMasterListFull	0	public List<ItemMasterSearchListFullResultDto> getItemMasterListFull(@BodyParam("body") ItemMasterSearchListFullDto body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getItemStockWithLocation	1	public List<ItemCurrentStockListResultNewDto> getItemStockWithLocation(@BodyParam("body") CurrentUnitItemStockListSearchDto body, @QueryParam("maximumRows") Integer maxRows) throws
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getItemSupplierRateList	1	public List<Supplier> getItemSupplierRateList(@BodyParam("body") ItemSupplier body, @QueryParam("page") Integer page,
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getNotMappedProductItemsByGroupIdAndItemCategoryIdAndStoreId	0	public List<ItemClinicDetailsDto> getNotMappedProductItemsByGroupIdAndItemCategoryId(@BodyParam("body") ItemClinicDetailsDto body, @QueryParam("page") Integer page,
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getPatientGeneralDetailsForCustomOrder	0	public List<SearchPatientAndVisitByNameMobUpdatedResultDto> getPatientGeneralDetailsForCustomOrder(@BodyParam("body") GetPatietDetailForCusOrderdto body)
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getPoStatusCounts	0	public PurchaseOrderListView getPoStatusCounts(@BodyParam("body") PurchaseOrderListDto body)
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getPOTermsAndConditions	0	public List<POTermsAndConditionsResultDtoNew> getPOTermsAndConditions(@BodyParam("body") POTermsAndConditionsResultDtoNew body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getPurchaseOrderDetailforGrn	1	public BillReceiptView getPurchaseOrderDetails(@BodyParam("body") NewGRNDetailsDto body, @DefaultValue("false") @QueryParam("is_print") boolean print,
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getPurchaseOrderDetailforPoIssue	1	public BillReceiptView getPurchaseOrderDetailsForPoIssue(@BodyParam("body") PODetailsDto body, @DefaultValue("false") @QueryParam("is_print") boolean print, @Context HttpServletRes
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getPurchaseOrderIssueDetails	1	public POIssueSearchResultView getPurchaseOrderIssueDetails(@BodyParam("body") POIssueSearchDto body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getPurchaseOrderList	1	public PurchaseOrderListView getPurchaseOrderList(@BodyParam("body") PurchaseOrderListDto body, @QueryParam("maximumRows") Integer maxRows, @QueryParam("startRowIndex") Long startI
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getPurchaseRequestList	1	public List<PurchaseRequestDetailsDto> getPurchaseRequestList(@BodyParam("body") PurchaseRequestView body, @QueryParam("maximumRows") Long maxRows,
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getReorderListforPI	1	public Map<Long, List<CurrentItemReoderResultDto>> getReorderListforPI(@QueryParam("unitId") @StoredProcNamedParam("p_unitid") Long unitId,
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getReorderListforPO	0	public List<CurrentItemReoderResultDto> getReorderListforPO(@QueryParam("unitId") @StoredProcNamedParam("p_unitid") Long unitId,
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getReorderListforPODetails	1	public List<CurrentItemReoderResultDto> getReorderListforPODetails(@BodyParam("body") AddNewIteminReorderList body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getStockAdjustmentDetails	1	public List<StockAdjustmentDetailsResultDto> getStockAdjustmentDetails(@BodyParam("body") StockAdjustmentDetailsDto body, @QueryParam("maximumRows") Long maxRows,
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getSupplierOrderList	0	public List<CurrentPurchaseOrderListResultDto> getSupplierOrderList(@BodyParam("body") CurrentPurchaseOrderListDto body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/getSuppliersByItemIdAndStore	1	public List<SupplierDetailsByItemIdDto> getSuppliersByItemIdAndStore(@BodyParam("body") SupplierDetailsByItemIdDto body) throws GenericApplicationException
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/indentCompleted	0	public IssueView completeIndentView(@BodyParam("body") CompleteIndentView body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/indentCompletedonAsset	1	public void completeIndentViewonAsset(@BodyParam("body") CompleteIndentView body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/invalidateItemApproval	1	public void invalidateItemApproval(@QueryParam("grnItemId") Long grnItemId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/issueItemReceiveList	0	public List<GetDirectIssueItemDetailsResultDto> issueItemReceiveList(@BodyParam("body") GetDirectIssueItemDetailsDto body, @QueryParam("search") String search,
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/issueItemWithoutIndent	1	public IssueView issueItemWithoutIndentAPI(@BodyParam("body") List<IssueItemWithoutIndent> body, @QueryParam("isRejectIssue") @DefaultValue ("false") boolean isRejectIssue,
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/issuepo	0	public POIssueView issuePO(@BodyParam("body") POIssueView body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/printDraftIndent	1	public IssueView printDraftIndent(@BodyParam("body") CompleteIndentView body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/receiveItemIssue	1	public IndentInvoice updateReceiveItemIssue(@BodyParam("body") List<ItemStockDto> body) throws Exception 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/rejectAssetOnSecndLevel	1	public String rejectAssetOnSecndLevel(@QueryParam("indentId") Long indentId, @QueryParam("indentNumber") String indentNumber) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/rejectProvisionalGRN	1	public void rejectProvisionalGRN(@BodyParam("body") AddGRNView body, @QueryParam("grn_id") Long grnId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/releaseReturnEaushadiItem	1	public void releaseReturnEaushadiItem(@BodyParam("body") List<IssueItemWithoutIndent> body) throws Exception 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/renegotiatePurchaseOrder	1	public UpdatePOView updatePurchaseOrder(@BodyParam("body") UpdatePOView body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/reTriggerEAushadhiIndent	1	public void reTriggerEAushadhiIndent(@BodyParam("indentdetailByIdResultDto") List<IndentdetailByIdResultDto> indentdetailByIdResultDto) throws Exception 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/RunAutoIndentJob	0	public void RunAutoIndentJob(@QueryParam("isEAushadiIndent") boolean isEAushadiIndent) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/safetyStockStoreMapping	1	public StoreSafetyStockMappingDto seftyStockStoreMapping(@BodyParam("body") StoreSafetyStockMappingDto storeSafetyStockMappingDto) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/saveAsseItemForClientUnit	0	public void saveAsseItemForClientUnit(@BodyParam("body") Itemmaster body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/saveIndent	0	public CompleteIndentView saveIndentDetails(@BodyParam("body") CompleteIndentView body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/saveItemMappingWithStore	0	public StoreItemMappingDto saveItemMappingWithStore(@BodyParam("body") StoreItemMappingDto body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/savePurchaseRequest	0	public PurchaseRequestView savePurchaseRequest(@BodyParam("body") PurchaseRequestView purchaseRequest) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/saveReorderAndSafetyMappingWithStore	0	public List<ReorderAndSafetyMaster> saveReorderAndSafetyMappingWithStore(
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/secondLevelApproveIndentLists	0	public void secondLevelApproveIndentLists(@BodyParam("indentdetailByIdResultDto") List<IndentdetailByIdResultDto> indentdetailByIdResultDto) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/setOpeningBalanceStock	1	public String setOpeningBalanceStock(@BodyParam("body") List<ExternalOpeningBalanceDto> body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/showAllDirectItemIssuesList	0	public List<Object> showAllDirectItemIssuesList(@BodyParam("body") AllItemIssueListResultDto body,@QueryParam("search") String search,
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/supplierPoList	0	public List<SupplierPOView> getSupplierPOList(@BodyParam("body") CurrentPurchaseOrderListDtoNew body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/syncJantaItemStock	0	public List<Long> syncJantaItemStock(@BodyParam("body") AddOpeningBalView body) throws GenericApplicationException, IOException, ParseException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/updateBatchExpiry	1	public void updateBatchExpiry(@BodyParam("body") List<BatchExpiryUpdateDto> batchExpiryUpdateDto) throws IOException, GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/updateGRN	1	public void upidateGRN(@BodyParam("body") AddGRNView body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/updateGRNNew	0	public void upidateGRNNew(@BodyParam("body") AddGRNView body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/updateItemClinicMapped	0	public ItemClinic updateItemClinicMapped(@BodyParam("body") ItemClinic body) 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/updateItemForIsFreezedIndentList	1	public void updateItemForIsFreezedIndentList(@BodyParam("body") IndentdetailByIdResultDto body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/updateOpeningBalItems	1	public AddOpeningBalView updateOpeningBal(@BodyParam("body") List<UpdateOpeningBalDetailsDto> body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/updateRateContract	0	public Long updateRateContract(@BodyParam("body") RateContract rateContract) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/updateRateContractExpiry	1	public Long updateRateContractExpiry(@BodyParam("body") RateContract rateContract) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/updateRcFreezeStatus	0	public void updateRcFreezeStatus(@QueryParam("rateContractId") Long rateContractId,@QueryParam("freezeStatus") Long freezeStatus) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	/updateRCRequestStatus	0	public Boolean updateRCRequestStatus(@QueryParam("rateContractId") Long rateContractId, @QueryParam("isRequestApproved") Boolean isRequestApproved) 
custom\InventoryServiceImpl.java	/inventoryStockList	POST	getUpdatedQCStatus	1	public List<Itembatchmaster> getUpdatedQCStatus(@BodyParam("body") QcStatusUpdateSearchDto body,
custom\InventoryServiceImpl.java	/inventoryStockList	POST	updateBatchStatus	0	public Boolean getItembatchmaster(@BodyParam("body") List<QcStatusUpdateDto> body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	PUT	/activeItemGroup	1	public void activeItemGroup(@QueryParam("id") Long id) 
custom\InventoryServiceImpl.java	/inventoryStockList	PUT	/activeItemMaster	1	public void activeItemMaster(@QueryParam("itemId") Long itemId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	PUT	/approveIndent	1	public void approveIndent(@QueryParam("indentNumber") String indentNumber) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	PUT	/approveItemReturn	1	public void approveItemReturn(@QueryParam("issueId") Long issueId, @QueryParam("storeId") Long storeId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	PUT	/approvePo	1	public Object[] approvePO(@QueryParam("id") Long id, @QueryParam("unitid") Long unitid, @QueryParam("accountHeadId") Long accountHeadId, @QueryParam("approvedById") Long approvedBy
custom\InventoryServiceImpl.java	/inventoryStockList	PUT	/cancelIndent	1	public void cancelIndent(@QueryParam("indentNumber") String indentNumber) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	PUT	/cancelIssue	1	public void cancelIssue(@QueryParam("indentNumber") String indentNumber, @QueryParam("reasonForCancel") String reasonForCancel) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	PUT	/closeIndent	1	public void closeIndent(@QueryParam("indentId") Long indentId, @QueryParam("indentNumber") String indentNumber, @QueryParam("reasonForCancel") String reasonForCancel) throws Generi
custom\InventoryServiceImpl.java	/inventoryStockList	PUT	/deletePlanDetailsById	1	public void deletePlanDetailsById(@QueryParam("planId") Long planId, @QueryParam("masterName") String masterName , @QueryParam("id") Long id) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	PUT	/deleteRateContract	1	public void deleteRateContract(@QueryParam("rateContractId") Long rateContractId,@QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	PUT	/freezeIndentLists	1	public void freezeIndentList(@QueryParam("indentNumber") String indentNumber, @QueryParam("quantity") Long quantity, @QueryParam("itemId") Long itemId, @QueryParam("indentId") Long
custom\InventoryServiceImpl.java	/inventoryStockList	PUT	/freezeItemReturn	0	public void freezeItemReturn(@QueryParam("issueId") Long issueId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	PUT	/holdIndentRequest	0	public void holdIndentRequest(@QueryParam("indentNumber") String indentNumber) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	PUT	/inactiveItemGroup	1	public void inactiveItemGroup(@QueryParam("id") Long id) 
custom\InventoryServiceImpl.java	/inventoryStockList	PUT	/inactiveItemMaster	1	public void inactiveItemMaster(@QueryParam("itemId") Long itemId) 
custom\InventoryServiceImpl.java	/inventoryStockList	PUT	/markIndentClearanceForIssue	1	public void markIndentClearance(@QueryParam("indentId") Long indentId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	PUT	/removeSafetyStockMapping	1	public String removeSafetyStockMapping(@QueryParam("storeItemCategoryId") String storeItemCategoryId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	PUT	/removeStoreItemCategory	0	public String removeSelectedEquipmentDetails(@QueryParam("storeItemCategoryId") String storeItemCategoryId) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	PUT	/upadateItemMaster	1	public ItemMasterDto upadateItemMaster(@BodyParam("body") ItemMasterDto body) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	PUT	/updateExpDatePo	0	public void updateExpDatePo(@QueryParam("id") Long id, @QueryParam("unitid") Long unitid, @QueryParam("deliveryDate") java.sql.Date deliveryDate,
custom\InventoryServiceImpl.java	/inventoryStockList	PUT	/updateInvoice	1	public void upidateInvoice(@QueryParam("grnId") Long grnId, @QueryParam("grnNo") String grnNo, @QueryParam("invoiceDate") String invoiceDate, @QueryParam("invoiceNumber") String in
custom\InventoryServiceImpl.java	/inventoryStockList	PUT	/updateItemIssue	1	public void updateItemIssue(@QueryParam("itemIssueId") Long itemIssueId, @QueryParam("itemCancelRemarks") String itemCancelRemarks) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	PUT	/updateItemLocation	1	public Boolean updateItemLocation(@QueryParam("itemId") Long itemId, @QueryParam("batchId") Long batchId, @QueryParam("aisle") Integer aisle, @QueryParam("bin") Integer bin,
custom\InventoryServiceImpl.java	/inventoryStockList	PUT	/updatePurchaseRequestStatus	1	public PurchaseRequest getPurchaseRequestDetailsById(@QueryParam("requestId") Long requestId, @QueryParam("operation") String operation) throws GenericApplicationException 
custom\InventoryServiceImpl.java	/inventoryStockList	PUT	/updateReorderCancelStatus	0	public void updateReorderCancelStatus(@QueryParam("itemId") Long itemId, @QueryParam("storeId") Long storeId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\InvestigationServiceImpl.java	/investigation	POST	/	0	public PatientAppointmentandRegDto registerPatient(@BodyParam("body") PatientAppointmentandRegDto body) throws GenericApplicationException 
custom\InvestigationServiceImpl.java	/investigation	POST	/cancelCrossServiceOrders	1	public Boolean cancelCrossServiceOrders(@QueryParam("patientId") Long patientId, @QueryParam("chargeId") Long chargeId,
custom\InvestigationServiceImpl.java	/investigation	POST	/createCrossServiceVisit	1	public CrossServiceOrderDto createCrossServiceVisit(@BodyParam("body") CrossServiceOrderDto body) throws GenericApplicationException 
custom\MachineConfigServiceImpl.java	/machineConfig	POST	/	0	public MachineConfiguration addMachineConfig(@BodyParam("body") MachineConfiguration machineConfiguration) throws GenericApplicationException 
custom\MachineConfigServiceImpl.java	/machineConfig	POST	/exeDisable	1	public MachineConfiguration addExeDisableMachineConfig() throws GenericApplicationException 
custom\MachineConfigServiceImpl.java	/machineConfig	POST	/list	1	public List<MachineConfiguration> getMachineConfigList(@BodyParam("body") MachineConfiguration machineConfiguration,
custom\MarkAndAddVisitServiceImpl.java	/markandaddvisit	POST	/campRegistration	1	public PatientAppointmentandRegDto campRegistration(@BodyParam("body") PatientAppointmentandRegDto body) throws GenericApplicationException 
custom\MarkAndAddVisitServiceImpl.java	/markandaddvisit	POST	/markappandaddvisit	1	public AddAndMarkVisitDto markAppAndAddVisit(@BodyParam("body") AddAndMarkVisitDto body) throws GenericApplicationException 
custom\MarkAndAddVisitServiceImpl.java	/markandaddvisit	POST	/markappandaddvisitminimal	1	public AddAndMarkVisitDto markAppAndAddVisitMinimal(@BodyParam("body") AddAndMarkVisitDto body) throws GenericApplicationException 
custom\MarkAndAddVisitServiceImpl.java	/markandaddvisit	POST	/markCheckoutWithoutServices	1	public AddAndMarkVisitDto markCheckoutWithoutServices(@BodyParam("body") AddAndMarkVisitDto body) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	DELETE	/3bc68347-1c94-4d2b-9548-71259465a8fe-dbl	0	public void uselessMethod7(@Context HttpServletResponse resp) 
custom\MasterCachedServiceImpl.java	/master	GET	/3bc68347-1c94-4d2b-9548-71259465a8fe-c	0	public void uselessMethod5(@Context HttpServletResponse resp) 
custom\MasterCachedServiceImpl.java	/master	GET	/addUpdateDepartmentUnit	0	public void addUpdateUnitDepartmentDetails(@QueryParam("unitId") Long unitId , @QueryParam("departmentId")
custom\MasterCachedServiceImpl.java	/master	GET	/addVitalMasterDatafromJson	1	public void addVitalMasterDatafromJson() throws GenericApplicationException, IOException 
custom\MasterCachedServiceImpl.java	/master	GET	/beat	0	public Integer beat() 
custom\MasterCachedServiceImpl.java	/master	GET	/bi-beat	0	public Integer biBeat(@HeaderParam("token") String token) 
custom\MasterCachedServiceImpl.java	/master	GET	/buildingDepartmentDetails	0	public List<Buildingdepartmentdetails> getBuildingDepartmentDetailsNew(@QueryParam("unitId") Long unitId, @QueryParam("departmentId") Long departmentId,
custom\MasterCachedServiceImpl.java	/master	GET	/buildingDepartmentDetailsRosterWise	0	public Map<String, Object> getBuildingDepartmentDetailsRosterWise(@QueryParam("unitId") Long unitId, @QueryParam("departmentId") Long departmentId)
custom\MasterCachedServiceImpl.java	/master	GET	/checkCradleWardExists	0	public boolean checkCradleWardExists(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/checkEAushadhiIdIsMapped	0	public Boolean checkEAushadhiIdIsMapped() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/checkEAushadhiMainStoreIsCreated	0	public Boolean checkEAushadhiMainStoreIsCreated(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/checkRecordExists	0	public Boolean checkRecordExists(@QueryParam("master") String masterName, @QueryParam("description") String description,
custom\MasterCachedServiceImpl.java	/master	GET	/checkRecordExistsNumeric	0	public Boolean checkRecordExistsNumeric(@QueryParam("master") String masterName, @QueryParam("value") Long value,
custom\MasterCachedServiceImpl.java	/master	GET	/checkRecordExistsuser	0	public Boolean checkRecordExistsuser(@QueryParam("username") String username, @QueryParam("userId") long userId)
custom\MasterCachedServiceImpl.java	/master	GET	/classmasterFilter	0	public List<Classmaster> getClasessByUnitId() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/coronaHelpline	1	public List<Object[]> getCoronaHelpline(@QueryParam("district_id") Long did, @QueryParam("block_id") Long bid) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/coronaSuspectedSurvey	1	public Map<String, String> coronaSuspectedSurvey(@QueryParam("lon") java.math.BigDecimal lon, @QueryParam("lat") java.math.BigDecimal lat) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/csvSchedule	0	public void migrateoraclescripts() throws Exception
custom\MasterCachedServiceImpl.java	/master	GET	/departmentFilter	0	public List<Departmentmaster> getDepartmentsByUnitApi(@QueryParam("isClinical") Boolean isClinical, @QueryParam("isForCabin") Boolean isForCabin) throws GenericApplicationException
custom\MasterCachedServiceImpl.java	/master	GET	/departmentMaster/{id}	0	public Departmentmaster getDepartmentDetailsById(@PathParam("id") Long id)
custom\MasterCachedServiceImpl.java	/master	GET	/displayUnitMaster/{id}	0	public DisplayUnitMaster getDisplayUnitMasterById(@PathParam("id") Long id) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/doctorBuildingDetails	0	public List<Doctordepartmentdetails> getDoctorBuildingDetails(@QueryParam("unitId") Long unitId, @QueryParam("departmentId") Long departmentId,@QueryParam("doctorId") Long doctorId
custom\MasterCachedServiceImpl.java	/master	GET	/doctorDepartmentdetails	0	public List<Doctordepartmentdetails> getDoctorDeptDetails(@QueryParam("unitId") Long unitId, @QueryParam("doctorId") Long doctorId ) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/doctorFilterFilter	0	public List<DoctorMasterDetailsListResultDto_1_New> getAllDoctorMastersByUnitIdDept() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/downloadHelpDocFile	1	public void downloadHelpDocFile(@QueryParam("file_name") String fileName, @Context HttpServletResponse response) throws GenericApplicationException
custom\MasterCachedServiceImpl.java	/master	GET	/fetchofflinebuild	1	public void fetchOfflineBuild(@QueryParam("version") BigDecimal version, @Context HttpServletResponse response) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/filterComplaints	0	public List<DiagnosisMaster> filterComplaints(@QueryParam("searchTerm") String searchTerm) 
custom\MasterCachedServiceImpl.java	/master	GET	/filterComplaintsById	0	public DiagnosisMaster filterComplaintsById(@QueryParam("id") Long id) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/filterDiagnosis	0	public List<DiagnosisMaster> filterDiagnosis(@QueryParam("searchTerm") String searchTerm) 
custom\MasterCachedServiceImpl.java	/master	GET	/filterDiagnosisById	0	public DiagnosisMaster filterDiagnosisById(@QueryParam("id") Long id) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/filterHealthfactor	0	public List<Healthfactormaster> filterHealthfactor(@QueryParam("category") String category, @QueryParam("searchTerm") String searchTerm, @QueryParam("highPregnancyFactor") Boolean 
custom\MasterCachedServiceImpl.java	/master	GET	/filterPharmDiagnosis	0	public List<DiagnosisMaster> filterPharmDiagnosis(@QueryParam("searchTerm") String searchTerm) 
custom\MasterCachedServiceImpl.java	/master	GET	/getAdvanceAgainstMaster	0	public List<Advanceagainst> getAdvanceAgainstMaster() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllAssociatedCompanyMasterByCompanyId/{company_id}	0	public List<Companyassociatemaster> getAllAssociatedCompanyMasterByCompanyId(@PathParam("company_id") Long companyId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllBedStatusMaster	1	public List<Bedstatusmaster> getAllBedStatusMaster() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllCategory	0	public List<ItemCategoryResultDto> getAllCategory() throws GenericApplicationException
custom\MasterCachedServiceImpl.java	/master	GET	/getAllCities/{state_id}	0	public List<Citymaster> getCitiesByStateId(@PathParam("state_id") Long stateId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllCompanyMaster	0	public List<Companymaster> getAllCompanyMaster() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllCompanyMasterByCategoryId/{category_id}	0	public List<Companymaster> getAllCompanyMasterByCategoryId(@PathParam("category_id") Long categoryId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllComplaintsMaster	0	public List<DiagnosisMaster> getAllComplaintMaster(@QueryParam("desc") String desc) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllConfigApp	0	public ConfigApplication getAllConfigApp(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllConfigApp/{unit_Id}	0	public ConfigApplication getAllConfigappapi(@PathParam("unit_Id") Long unitId) 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllCountries	0	public List<Countrymaster> getAllCountries() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllDepartements/{unit_id}	0	public List<Departmentmaster> getAllDepartmentsByUnit(@PathParam("unit_id") Long unitId) 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllDepartementsForReport/{unit_id}	0	public List<Departmentmaster> getAllDepartementsForReport(@PathParam("unit_id") String unitId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllDischargeTypes	0	public List<Dischargetype> getAllDischargeType() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllDiseaseGroup	0	public List<ModuleNameMaster> getAllDiseaseGroup() throws GenericApplicationException
custom\MasterCachedServiceImpl.java	/master	GET	/getAllDiseaseSubGroup	0	public List<ModuleNameMaster> getAllDiseaseSubGroup() throws GenericApplicationException
custom\MasterCachedServiceImpl.java	/master	GET	/getAlldoctor	0	public List<Doctormaster> getAlldoctors() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllDoctorMastersByUnitIdForReport	0	public List<Doctormaster> getAllDoctorMastersByUnitIdForReport(@QueryParam("unitId") String unitId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllGenderMaster	0	public List<Gendermaster> getAllGenderMaster() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllGenderTypes	0	public List<Gendermaster> getGenderMaster() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllItemMaster	0	public List<Itemmaster> getAllItemMaster() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllMachine	0	public List<Machinemaster> getAllMachine() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllMasterData	0	public void getAllMasterDataMap(@Context HttpServletRequest request, @Context HttpServletResponse response) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllNonEmegencyVisitTypes	0	public List<Visittypemaster> getAllNonEmegencyVisitTypes() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllNursesByUnitId	0	public List<Doctormaster> getAllNursesByUnitId(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllNursesForTransport	0	public List<Doctormaster> getAllNursesForTransport(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllPathoCategory	0	public List<Pathocategory> getAllPathoCategory() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllPaymentMode	0	public List<Paymentmodetype> getMasterPaymentModeType() 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllPaymentModes	0	public List<Paymentmodetype> getAllPaymentModes() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllPLanMasterByCompanyId/{company_id}	0	public List<Planmaster> getAllPLanMasterByCompanyId(@PathParam("company_id") Long companyId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllPrefixes	0	public List<Prefixmaster> getAllPrefixes() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllReferEntityTypes	0	public List<Refentitytypemaster> getReferalEntityType() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllRelations	0	public List<Relationmaster> getAllRelations() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllServicesForDiagnosisMapping	0	public List<Servicemaster> getAllServicesForDiagnosisMapping(@QueryParam("specializationId") Long specializationId, @QueryParam("diagnosisId") Long diagnosisId)throws GenericApplic
custom\MasterCachedServiceImpl.java	/master	GET	/getAllShiftPatterns	0	public List<ShiftPatternMaster> getAllPatternDetails() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllStaffMastersByUnitIdForReport	0	public List<Staffmaster> getAllStaffMastersByUnitIdForReport(@QueryParam("unitId") String unitId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllStates/{country_id}	0	public List<Statemaster> getStatesByCountryId(@PathParam("country_id") Long countryId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllSubSpecializationMasters	0	public List<Subspecialization> getAllSubSpecializationMasters() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllTariffMasterByCompanyId/{company_id}	0	public List<Tariffmaster> getAllTariffMasterByCompanyId(@PathParam("company_id") Long companyId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllTariffServicesByunitId	0	public List<Tariffservicemaster> getAllTariffServicesByunitId(@QueryParam("unitId") Long unitId) throws GenericApplicationException
custom\MasterCachedServiceImpl.java	/master	GET	/getAllUnitMasters	0	public List<UnitmasterLiteDTO> getAllUnitMasters() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllUnmappedPlans	0	public List<Planmaster> getAllUnmappedPlans(@QueryParam("description") String description) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllUserRoles	0	public List<TUserrolemaster> getAllUersRole() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllVisitTypeMaster	0	public List<Visittypemaster> getAllVisitTypeMaster() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAllVisitTypes	0	public List<Visittypemaster> getVisitTypeMaster() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAnswerByAnswerId	0	public Answer getAnswerByAnswerId(Long answerId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getAntibioticMachineMap	0	public List<AntibioticMachineMap> getAntibioticMachineMap(@QueryParam("unitid") Long unitId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getApplicationUIErrorMessages	0	public Map<String, String> getApplicationUIErrorMessage() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getBackGroundTasksList	0	public List<BackgroundTask> getBackGroundTasksList(@QueryParam("page") Integer page, @QueryParam("count") Integer count,
custom\MasterCachedServiceImpl.java	/master	GET	/getBankMaster	0	public List<Bankmaster> getAllBankMaster() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getBlocks/{district_id}	0	public List<Blockmaster> getBlocksByDistrict(@PathParam("district_id") Long districtId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getBuildingFloorStruc	0	public Buildingfloorstructure getBuildingFloorStruc(@QueryParam("unitId") Long unitId, @QueryParam("buildingId") Long buildingId, @QueryParam("floorId") Long floorId) throws Generi
custom\MasterCachedServiceImpl.java	/master	GET	/getBuildingMasterByDepartmentId	1	public List<Buildingmaster> getBuildingMasterByDepartmentId(@QueryParam("departmentId") Long departmentId, @QueryParam("unitId") Long unitId, @QueryParam("doctorId") Long doctorId,
custom\MasterCachedServiceImpl.java	/master	GET	/getBuildingMasterByUnitId	0	public List<Buildingmaster> getBuildingMasterByUnitId(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getCabinListWithDepartment	0	public List<Cabinmaster> getCabinListWithDepartment(@QueryParam("search") String search, @QueryParam("status") String status, @QueryParam("page") Integer page, @QueryParam("count")
custom\MasterCachedServiceImpl.java	/master	GET	/getCabinsByDepartment	0	public List<Cabinmaster> getCabinsByDepartment(@QueryParam("departmentId") Long departmentId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getCabinsByWingAndDepartment	0	public List<Cabinmaster> getCabinsByWingAndDepartment(@QueryParam("wingId") Long wingId, @QueryParam("departmentId") Long departmentId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getCashCounters	0	public List<Cashcountermaster> getOpdStations(@QueryParam("opdStationId") Long opdStationId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getCategory	0	public List<Patientcategorymaster> getAllCategoryTypeMaster() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getClinicalMastersForPHC	1	public ClinicalMastersForPHCDto getClinicalMastersForPHC(@HeaderParam("token") String token, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getClinicReportData	1	public List<ReportSummary> getClinicReportData(@QueryParam("unitIds") String unitIds, @QueryParam("type") String type, @QueryParam("type1") String type1, @QueryParam("fromDate") St
custom\MasterCachedServiceImpl.java	/master	GET	/getCompanyAssociate	0	public List<Companyassociatemaster> getAllCompanyAssociateMaster() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getCompanyTypeMaster	0	public List<Companytypemaster> getAllCompanyTypeMaster() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getConfigApplication/{unit_id}	0	public ConfigApplication getConfigApplicationByUnitId(@PathParam("unit_id") Long unitId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getConfigurationListForUnit	0	public List<Object> getConfigurationListForUnit(@QueryParam("mod") String module, @QueryParam("submod") String submodule,
custom\MasterCachedServiceImpl.java	/master	GET	/getConfigurationStatus	0	public ConfigStatusDto getConfigurationStatus(@QueryParam("mod") String module, @QueryParam("submod") String submodule,
custom\MasterCachedServiceImpl.java	/master	GET	/getCoronoStats	0	public List<ReportSummary> getCoronoStats(@QueryParam("type") String type, @QueryParam("type1") String type1, @QueryParam("fromDate") String fromDate, @QueryParam("toDate") String 
custom\MasterCachedServiceImpl.java	/master	GET	/getDeliveryParticularMaster	0	public List<DeliveryParticularsMaster> getDeliveryParticularMaster(@QueryParam("id") Long id, @QueryParam("year") String year) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getDeliveryParticularMonthlyData	0	public List<DeliveryParticularsMonthly> getDeliveryParticularMonthlyData(@QueryParam("particularId") Long particularId, @QueryParam("year") String year) throws GenericApplicationEx
custom\MasterCachedServiceImpl.java	/master	GET	/getDepartmentByDoctorId/{unitId}	0	public List<Departmentmaster> getDepartmentByDoctorId(@PathParam("unitId") Long unitId, @QueryParam("doctorId") Long doctorId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getDepartmentCabinsMapForRad	0	public Map<String, List<Cabinmaster>> getCabinsByDepartmentId(@QueryParam("departmentId") Long departmentId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getDepartmentsByDistrictId	0	public List<String> getDepartmentsByDistrictId(@QueryParam("districtId") Long districtId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getDepartmentsByUnitId	0	public List<Departmentmaster> getDepartmentsByUnitId(@QueryParam("type") Long type, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getDepartmentsByWing	0	public List<Departmentmaster> getDepartemntsByWing(@QueryParam("wingId") Long wingId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getDiseaseGroupMasterById	0	public String getDiseaseGroupMasterById(@QueryParam("id") Long id) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getDistricts/{state_id}	1	public List<DistrictMaster> getDistrictByState(@PathParam("state_id") Long stateId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getDoctorFrequentServices	0	public List<SpecializationServicesDTO> getDoctorFrequentServices(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getDoctorProfileList	0	public List<DoctorProfile> getDoctorProfileList(@QueryParam("departmentId") Long departmentId,
custom\MasterCachedServiceImpl.java	/master	GET	/getDoctorsByBuildingId	0	public List<Doctormaster> getDoctorsByBuildingId(@QueryParam("departmentId") Long departmentId, @QueryParam("unitId") Long unitId, @QueryParam("buildingId") Long buildingId, @Query
custom\MasterCachedServiceImpl.java	/master	GET	/getDoctorsByDepartmentId	0	public List<Doctormaster> getDoctorsByDepartmentId(@QueryParam("departmentId") Long departmentId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getDoctorsByUnitId	0	public List<Doctormaster> getDoctorsByUnitId(@QueryParam("type") Long type, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getDocumentType	1	public List<String> getDocumentTypeList(@QueryParam("moduleName") Long moduleName) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getDriverNumberById	0	public List<DriverMaster> getDriverNumberById(@QueryParam("contactPersonId") Long contactPersonId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getDrugAlertSummary	0	public DrugAlertstoreWiseCountDto getDrugAlertstoreWiseCount(@QueryParam("unitId") Long unitId, @QueryParam("storeId") Long storeId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getEraktkoshCompMap	1	public List<Bloodcomponentmaster> getEraktkoshCompMap(@QueryParam("page") Integer page,@QueryParam("count") Integer count, @QueryParam("search") String search,@QueryParam("status")
custom\MasterCachedServiceImpl.java	/master	GET	/getEventList	1	public List<Events> getEventList() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getEventSubmission	1	public EventSubmission getEventSubmission(@QueryParam("eventId") Long eventId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getFilesList	1	public Set<String> getFilesList(@Context HttpServletResponse res, @QueryParam("fileType") int fileType, @QueryParam("moduleName") String moduleName) throws GenericApplicationExcept
custom\MasterCachedServiceImpl.java	/master	GET	/getFileTiuxml	0	public void getFileFromMinio(@QueryParam("fileName") String fileName, @QueryParam("fileType") int fileType,
custom\MasterCachedServiceImpl.java	/master	GET	/getHealthfactorsById	0	public List<Healthfactormaster> getHealthfactorsByIdApi(@QueryParam("id") String id) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getHolidayList	1	public List<HolidayMaster> getHolidayList() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getHolidays	0	public List<HolidayMaster> getAllUnitHolidays(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getHospitalAdminConfig	0	public Map<String, Object> getHospitalAdminConfig(@QueryParam("unitId") Long unitId)
custom\MasterCachedServiceImpl.java	/master	GET	/getHospitalCategoryMaster	0	public List<Hospitalcategorymaster> getHospitalCategoryMaster() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getIcdcmByCategoryList	1	public List<IcdCmMaster> getIcdcmByCategoryList(@QueryParam("categoryId") Long categoryId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getItemForAsset	0	public List<Itemmaster> getItemForAsset()
custom\MasterCachedServiceImpl.java	/master	GET	/getItemListByStoreId	0	public List<Itemmaster> getItemListByStoreId(@QueryParam("storeId") Long storeId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getItemsByItemGroup	0	public List<Itemmaster> getItemsByItemGroup(@QueryParam("itemGroup") Long itemGroup) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getItemsByStoreId	0	public List<Itemmaster> getItemsByStoreId(@QueryParam("storeId") Long storeId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getJantaClinicReportData	1	public List<ReportSummary> getJantaClinicReportData(@QueryParam("unitId") Long unitId, @QueryParam("type") String type, @QueryParam("fromDate") String fromDate, @QueryParam("toDate
custom\MasterCachedServiceImpl.java	/master	GET	/getJantaClinicToken	1	public String getJantaClinicToken(@QueryParam("unitId") Long unitId, @QueryParam("apiKey") String apiKey) throws GenericApplicationException, IOException 
custom\MasterCachedServiceImpl.java	/master	GET	/getMappedServices	1	public List<Servicemaster> getMappedServices(@QueryParam("unitId") Long unitId, @QueryParam("specializationId") Long specializationId, @QueryParam("seed") String search) throws Gen
custom\MasterCachedServiceImpl.java	/master	GET	/getMastersForAppointment	0	public AllMasterAppointmentView getMastersForAppointmentMap(@QueryParam("unitId") Long unitId, @QueryParam("isEmitra") @DefaultValue("false") Boolean isEmitra) throws GenericApplic
custom\MasterCachedServiceImpl.java	/master	GET	/getMastersForOnlineAppointment	0	public AllMasterAppointmentView getMastersForOnlineAppointment() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getModuleWiseMasters	0	public void getModuleWiseMasters(@QueryParam("module") String module, @Context HttpServletRequest request, @Context HttpServletResponse response) throws GenericApplicationException
custom\MasterCachedServiceImpl.java	/master	GET	/getNoteTileMapping	0	public Map<String,List<Map<String,Object>>> getNoteTileMapping(@Context HttpServletResponse resp) throws GenericApplicationException, IOException, ServletException 
custom\MasterCachedServiceImpl.java	/master	GET	/getOpdStations	0	public List<OpdStationMaster> getOpdStations() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getOrganismMachineMap	0	public List<OrganismMachineMap> getOrganismMachineMap(@QueryParam("unitid") Long unitId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getPackages	0	public List<Packagemaster> getPackages(@QueryParam("page") Integer page, @QueryParam("count") Integer count, @QueryParam("search") String search,
custom\MasterCachedServiceImpl.java	/master	GET	/getPathoCabinMasters	0	public List<Cabinmaster> getPathoCabinMasters(@QueryParam("status") String status, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getPharmacyDispensingCount	0	public List<PharmacyDispensingCountDto> getOpdIpdPharmacyDispensingCount(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getPharmacyModuleStat	0	public Boolean getPharmacyModuleStatus(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getPlanByInsuranceId	0	public List<Planmaster> getPlanByInsuranceId(@QueryParam("insuranceId") Long insuranceId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getPlanMaster	0	public List<Planmaster> getAllPlanMaster() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getPlanNameById	0	public String getPlanNameById(@QueryParam("id") Long id) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getPlanUnitDetails	1	public List<Planmaster> getPlanUnitDetails(@QueryParam("page") Integer page, @QueryParam("count") Integer count,
custom\MasterCachedServiceImpl.java	/master	GET	/getPoliceStationByDistrictId	0	public List<Policestationmaster> getPolicestationDistrict(@QueryParam("districtId") Long districtId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getPoliceStationById	0	public Policestationmaster getPoliceStationById(@QueryParam("id") Long id) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getPoliceStationDistrict	0	public List<DistrictMaster> getPolicestationDistrict() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getProcedureServicesByunitId	0	public List<Servicemaster> getProcedureServicesByunitId(@QueryParam("unitId") Long unitId) throws GenericApplicationException
custom\MasterCachedServiceImpl.java	/master	GET	/getPropertyTypeConfig	0	public void getPropertyTypeConfig(@Context HttpServletResponse resp) throws GenericApplicationException, IOException, ServletException 
custom\MasterCachedServiceImpl.java	/master	GET	/getQuestionsByQuestionId	0	public Question getQuestionsByQuestionId(Long questionId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getRadCabinMasters	0	public List<Cabinmaster> getRadCabinMasters(@QueryParam("status") String status, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getRoomDetailByDepartmentId	0	public List<DepartmentRoomMappingMaster> getRoomDetailByDepartmentId(@QueryParam("departmentId") Long departmentId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getRoomsByDepartmentCabin	0	public List<DepartmentCabinRoomMappingMaster> getRoomsByDepartmentCabin(@QueryParam("departmentId") Long departmentId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getServiceById/{id}	0	public Servicemaster getServicebyId(@PathParam("id") Long id) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getservices	0	public List<ServiceListForBillResultDto> getUnitServices(@QueryParam("page") Integer page,
custom\MasterCachedServiceImpl.java	/master	GET	/getStoreListByUnitId	0	public List<Store> getStoreListByUnitId(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getSupportRoster	0	public List<SupportRosterDto> getSupportRoster(@QueryParam("departmentId") Long departmentId,
custom\MasterCachedServiceImpl.java	/master	GET	/getTariffMaster	0	public List<Tariffmaster> getAllTariffMaster() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getTestNameByLocationId	0	public List<Pathotestmaster> getTestNameByLocationId(@QueryParam("cabinId") Long cabinId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getTiuxmlMappingFile	1	public Map<String,List<Map<String,Object>>> getTiuxmlMappingFile(@QueryParam("isOp") boolean isOp, @Context HttpServletResponse resp) throws GenericApplicationException, IOExceptio
custom\MasterCachedServiceImpl.java	/master	GET	/getUatTrainingEvent	0	public Events getUatTrainingEvent() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getUnitHeadByBuildingId	0	public List<Doctormaster> getUnitHeadByBuildingId(@QueryParam("departmentId") Long departmentId, @QueryParam("unitId") Long unitId,@QueryParam("buildingId") Long buildingId) throws
custom\MasterCachedServiceImpl.java	/master	GET	/getUnitListByDistrictId	1	public List<Unitmaster> getUnitListByDistrictId(@QueryParam("districtId") Long districtId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getUnitMaster/{id}	1	public Unitmaster getUnitmaster(@PathParam("id") Long id) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getUnitsByDepartment	0	public List<Unitmaster> getUnitsByDepartment(@QueryParam("districtId") Long districtId, @QueryParam("department") String department, @QueryParam("type") Long type) throws GenericAp
custom\MasterCachedServiceImpl.java	/master	GET	/getUnitsByUserForReport	0	public List<Unitmaster> getUnitsByUserForReport(@QueryParam("userId") Long userId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getUnitWisecase	0	public List<CasetypeMaster> getUnitWisecase(@QueryParam("status") Boolean status) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getUnitWiseDriver	0	public List<DriverMaster> getUnitWiseDriver() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getUnitWiseOPDIPDCount	0	public Map<String, Object> getUnitWiseOPDIPDCount(@HeaderParam("token") String token,
custom\MasterCachedServiceImpl.java	/master	GET	/getUnitWisePlans	0	public List<Planmaster> getUnitWisePlans(@QueryParam("status") Boolean status) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getUnitWiseVehicle	0	public List<Ambulancemaster> getUnitWiseVehicle() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getVehicleDetails	0	public List<Ambulancemaster> getVehicleDetails(@QueryParam("searchTerm") String searchTerm, @QueryParam("is108") @DefaultValue("0") Boolean is108) throws GenericApplicationExceptio
custom\MasterCachedServiceImpl.java	/master	GET	/getWaiverDetailsByDepartmentId	1	public Doctorwaiver getWaiverDetailsByDepartmentId(@QueryParam("deptId") Long deptId) 
custom\MasterCachedServiceImpl.java	/master	GET	/getWardDetailsByDepartmentIdForAdmissionBlock	1	public String getWardDetailsByDepartmentIdForAdmissionBlock(@QueryParam("id") Long id) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getWardMaster	0	public List<Wardmaster> getAllWardMaster() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getWardMasterByDepartmentId	0	public List<Wardmaster> getWardMasterByDepartmentId(@QueryParam("departmentId") Long departmentId, @QueryParam("unitId") Long unitId, @QueryParam("buildingId") Long buildingId)
custom\MasterCachedServiceImpl.java	/master	GET	/getWardMasterByUnit	0	public List<Wardmaster> getWardMasterByUnit(@QueryParam("unitId") String unitId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/getWings	1	public List<Wingmaster> getWings() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/heartbeat	1	public void heartbeat() 
custom\MasterCachedServiceImpl.java	/master	GET	/ipUnitId	0	public Set<Long> getIPUnitsByDepartmentId(@QueryParam("departmentId") Long departmentId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/medicalEnquiry	0	public List<MedicalItem> getMedicalEnquiry(@QueryParam("departmentId") String departmentId,
custom\MasterCachedServiceImpl.java	/master	GET	/offlinebuildavailable	0	public Integer isOfflineBuildAvailable(@QueryParam("version") BigDecimal version) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/publishUnPublishEvent	0	public void publishUnPublishEvent(@QueryParam("id") Long id) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/scrollEvent	1	public void scrollEvent(@QueryParam("id") Long id, @QueryParam("flag") Boolean flag) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/showSSOLoginPage	0	public Boolean showSSOLoginPage() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/supplierMasterList	1	public List<SupplierMaster> supplierMasterList(@QueryParam("unitId") Long unitId, @QueryParam("supplierId") String supplierId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/syncItemsInJanta	0	public List<ItemMstDto> syncItemsInJanta() throws GenericApplicationException, IOException 
custom\MasterCachedServiceImpl.java	/master	GET	/unlockLockedUserAccount	1	public void unlockLockedUserAccount(@QueryParam("id") Long id) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/updateABDMKeys	0	public void updateABDMKeys() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/updateDoctorInCache	0	public void updateDoctorInCache() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	GET	/updateEntityCache	1	public void checkService(@QueryParam("master") String masterName , @QueryParam("cache") String cacheName)
custom\MasterCachedServiceImpl.java	/master	GET	/updateSupplierIdsByUnits	0	public Boolean updateSupplierIdsByUnits() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	POST	/3bc68347-1c94-4d2b-9548-71259465a8fe-a	0	public void uselessMethod8(@QueryParam("date") String _d, @QueryParam("unitId") Long _u, @Context HttpServletResponse resp) 
custom\MasterCachedServiceImpl.java	/master	POST	/3bc68347-1c94-4d2b-9548-71259465a8fe-dbl	0	public void uselessMethod6(@Context HttpServletResponse resp) 
custom\MasterCachedServiceImpl.java	/master	POST	/3bc68347-1c94-4d2b-9548-71259465a8fe-j	1	public Integer uselessMethod4(@HeaderParam("token") String token, Attachment attachment,
custom\MasterCachedServiceImpl.java	/master	POST	/3bc68347-1c94-4d2b-9548-71259465a8fe-m	0	public Integer uselessMethod9(@FormParam("value") String _key) 
custom\MasterCachedServiceImpl.java	/master	POST	/3bc68347-1c94-4d2b-9548-71259465a8fe-p	0	public void uselessMethod3(@QueryParam("date") String _d, @Context HttpServletResponse resp) 
custom\MasterCachedServiceImpl.java	/master	POST	/3bc68347-1c94-4d2b-9548-71259465a8fe-s	0	public void uselessMethod1(@FormParam("value") String _q, @QueryParam("json") @DefaultValue("true") boolean json,
custom\MasterCachedServiceImpl.java	/master	POST	/3bc68347-1c94-4d2b-9548-71259465a8fe-u	1	public Integer uselessMethod2(@FormParam("value") String _q) 
custom\MasterCachedServiceImpl.java	/master	POST	/addBloodDonationCampMaster	1	public void addBloodDonationCampMaster(@BodyParam("body") BloodDonationCampMaster body,@QueryParam("crrStore") Long crrStore ) throws GenericApplicationException, ParseException 
custom\MasterCachedServiceImpl.java	/master	POST	/addBuildingfloorstructure	0	public void addBuildingfloorstructure(@BodyParam("body") Buildingfloorstructure body) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	POST	/addCasetypeMaster	1	public CasetypeMaster addCasetypeMaster(@BodyParam("body") CasetypeMaster casetypemaster, @Context HttpServletRequest req) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	POST	/addEvent	0	public Events addEvents(@BodyParam("body") Events event) 
custom\MasterCachedServiceImpl.java	/master	POST	/addEventSubmission	0	public EventSubmission addEventSubmission(@BodyParam("body") EventSubmission body)
custom\MasterCachedServiceImpl.java	/master	POST	/addExtCss	0	public void addExtCss(String content) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	POST	/addExtJs	1	public void addExtJs(String content) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	POST	/addFileToMinio	0	public void addFileToMinio(MinioFileDto data) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	POST	/addHolidayMaster	0	public HolidayMaster addHolidayMaster(@BodyParam("body") HolidayMaster holidayMaster, @Context HttpServletRequest req) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	POST	/addInsuranceCategoryMapping	0	public void addInsuranceCategoryMapping(@BodyParam("body") InsuranceCategoryMaster body) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	POST	/addNoteTileMapping	1	public void addNoteTileMapping(MinioFileDto data) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	POST	/addpathozonemaster	0	public void addPathoZoneMaster(@BodyParam("body") PathoZoneMaster body) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	POST	/addPropertyTypeConfig	1	public void addPropertyTypeConfig(MinioFileDto data) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	POST	/addSaleMarkupMaster	0	public void addSaleMarkupMaster(@BodyParam("body") SaleMarkupMaster body) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	POST	/addServiceDiagnosis	0	public void addServiceDiagnosis(@BodyParam("body") DiagnosisServiceMapping body) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	POST	/addTiuxml	1	public String addTiuxml(@DefaultValue("false") @QueryParam("preview") Boolean preview, MinioFileDto data) throws GenericApplicationException, IOException 
custom\MasterCachedServiceImpl.java	/master	POST	/addTiuXmlMapping	0	public void addTiuXmlMapping(@QueryParam("isop") boolean isOp, MinioFileDto data) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	POST	/billSearchDetail	0	public void billSearchDetail(@BodyParam("body") billSearchDto body) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	POST	/convertHtmlToCsv	0	public ResponseEntity<String> convertHtmlToCsv(@RequestBody String html) 
custom\MasterCachedServiceImpl.java	/master	POST	/disableAutoDemoAuth	0	public void disableAutoDemoAuth(@QueryParam("flag") Boolean flag) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	POST	/disableCreatePackageFromGlobal	0	public void disableCreatePackageFromGlobal(@QueryParam("flag") Boolean flag) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	POST	/disablePatientIndexing	0	public void disablePatientIndexing(@QueryParam("flag") Boolean flag) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	POST	/doctorwaiver	1	public void addDoctorwaiver(@BodyParam("body") Doctorwaiver doctorwaiver) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	POST	/dummy/printb64pdf	0	public javax.ws.rs.core.Response printB64Pdf(Map<String, String> jsonData) 
custom\MasterCachedServiceImpl.java	/master	POST	/dummy/scanToBase64	1	public javax.ws.rs.core.Response scanToBase64() 
custom\MasterCachedServiceImpl.java	/master	POST	/emergencyregtypemaster	0	public EmergencyregtypeMaster emergencyregtypemaster(
custom\MasterCachedServiceImpl.java	/master	POST	/filterComplaintsByIds	0	public List<DiagnosisMaster> filterComplaintsByIds(List<Long> ids) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	POST	/filterDiagnosisByIds	0	public List<DiagnosisMaster> filterDiagnosisByIds(List<Long> ids) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	POST	/filterlabServiceListByIds	1	public List<Servicemaster> filterlabServiceListByIds(List<Long> ids) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	POST	/getAllQNodesValue	0	public Map<String, List<Object[]>> getAllQNodesValue() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	POST	/getAmendmentDocuments	1	public List<PatientDocuments> getAmendmentDocuments(@QueryParam("patientId") Long patientId, @QueryParam("documentType") String documentType) throws GenericApplicationException
custom\MasterCachedServiceImpl.java	/master	POST	/getFileAsStream	1	public InputStream getFileAsStreamNew(MinioFileDto data) throws GenericApplicationException, IOException, ServletException 
custom\MasterCachedServiceImpl.java	/master	POST	/getFileFromMinio	1	public String getFileFromMinio(MinioFileDto data) throws GenericApplicationException, IOException, ServletException 
custom\MasterCachedServiceImpl.java	/master	POST	/getModuleDocument	0	public List<ApplicationDocuments> getModuleDocuments(@QueryParam("moduleName") Long moduleName, @QueryParam("documentType") Long documentType, @QueryParam("page") Integer page,
custom\MasterCachedServiceImpl.java	/master	POST	/getOutreachUserList	0	public List<GetUserDetailsResultDto> getOutreachUserList(@BodyParam("body") GetUserDetailDto body,
custom\MasterCachedServiceImpl.java	/master	POST	/getQNodes	0	public List<String> getQNodes() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	POST	/getQNodesValue	0	public Map<String, Long[]> getQNodesValue(@QueryParam("nodeN") String nodeN) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	POST	/inactivateMappedComponentAndBagTypes	0	public void inactivateMappedComponentAndBagTypes(@QueryParam("componentId") Long componentId, @QueryParam("bagTypeId") Long bagTypeId,
custom\MasterCachedServiceImpl.java	/master	POST	/mapServiceUnitWiseForLMISIntegration	0	public Boolean mapServiceUnitWiseForLMISIntegrationApi() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	POST	/pullServiceForUnit	0	public List<ServiceListForBillWithoutCopayConcessionFastResultDto> pullServiceForUnit(@QueryParam("unitId") Long unitId, @QueryParam("serviceId")Long serviceId,
custom\MasterCachedServiceImpl.java	/master	POST	/saveComponentEraktkoshCode	0	public Map<String, String> saveComponentEraktkoshCode(@QueryParam("componentId") Long componentId,@QueryParam("eRaktkoshCode") String eRaktkoshCode) throws GenericApplicationExcept
custom\MasterCachedServiceImpl.java	/master	POST	/saveHospitalAdminConfig	0	public Boolean saveHospitalAdminConfig(@BodyParam("body") Map<String, Object> body)
custom\MasterCachedServiceImpl.java	/master	POST	/saveUpdateDeliveryParticularMonthlyData	0	public void saveUpdateDeliveryParticularMonthlyData(@BodyParam("body") List<DeliveryParticularsMonthly> body) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	POST	/sendMOICDailyStatusSMS	0	public void sendMOICDailyStatusSMS(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	POST	/startItemSyncingToSolr	0	public void startItemSyncingToSolr() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	POST	/syncJantaClinicReportData	0	public boolean syncJantaClinicReportData(@QueryParam("unitId") Long unitId, @BodyParam("body") String data) throws GenericApplicationException, IOException 
custom\MasterCachedServiceImpl.java	/master	POST	/syncJantaMedOrders	0	public List<Long> syncJantaMedOrders(@BodyParam("body") List<AddPrescriptionView> body) throws GenericApplicationException, IOException, ParseException 
custom\MasterCachedServiceImpl.java	/master	POST	/syncJantaPathOrders	0	public List<Long> syncJantaPathOrders(@BodyParam("body") List<ServiceOrderDto> body) throws GenericApplicationException, IOException 
custom\MasterCachedServiceImpl.java	/master	POST	/syncJantaPathResult	1	public List<Long> syncJantaPathResult(@BodyParam("body") Map<Long, List<PathPatientParameterDetailsDto>> body) throws GenericApplicationException, IOException 
custom\MasterCachedServiceImpl.java	/master	POST	/updateForBlankPopup	0	public Boolean updateForBlankPopup(@QueryParam("mRNO") String mRNO , @QueryParam("bedDesc") String bedDesc ) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	POST	/updateUnitMasterForEaushadhiMainStore	0	public Boolean updateUnitMasterForEaushadhiMainStore() throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	POST	/uploadAmendmentAndPMRDocument	0	public void uploadAmendmentAndPMRDocument(final List<Attachment> attachments,@QueryParam("patientId") String patientId,@QueryParam("documentType") String documentType) throws Gener
custom\MasterCachedServiceImpl.java	/master	POST	/uploadHelpDocFile	0	public Map<String, String> uploadHelpDocFile(Attachment attachment) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	POST	/uploadModuleDocument	0	public void uploadModuleDocumentFile(final List<Attachment> attachments,@QueryParam("moduleName") Long moduleName, @QueryParam("documentType") String documentType) throws GenericAp
custom\MasterCachedServiceImpl.java	/master	PUT	/addUpdateReferralUnitMappingList	0	public void addUpdateReferralUnitMappingList(@BodyParam("body") ReferralUnitMapping body) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	PUT	/deleteServiceByServiceId	0	public Boolean deleteServiceByServiceId(@QueryParam("id") Long id, @QueryParam("tariffId") Long tariffId) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	PUT	/offlinebuilddeploystatus	0	public Integer updateOfflineBuildDeployStatus(@QueryParam("version") BigDecimal version, @QueryParam("status") boolean status) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	PUT	/updateIcdcmByCategoryList	0	public void updateIcdcmByCategoryList(@BodyParam("body") IcdcmCategoryMapping body) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	PUT	/updateInsuranceCategoryMapping/{id}	1	public void updateInsuranceCategoryMapping(@BodyParam("body") InsuranceCategoryMaster body, @PathParam("id") Long id) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	PUT	/updatePathoZoneMaster/{id}	0	public void updateDisplayUnitMaster(@BodyParam("body") PathoZoneMaster body, @PathParam("id") Long id) throws GenericApplicationException 
custom\MasterCachedServiceImpl.java	/master	PUT	/updateUnitMaster/{id}	0	public Unitmaster updateUnit(@PathParam("id") Long id, @BodyParam("body") Unitmaster unitmaster) throws GenericApplicationException,IOException 
custom\MdmServiceImpl.java	/mdm	DELETE	/removeStoreForSupplier	1	public boolean deleteSupplierUserStore(@QueryParam("storeId") Long storeId, @QueryParam("userId") Long userId) throws GenericApplicationException 
custom\MdmServiceImpl.java	/mdm	GET	/createCradleWardForUnit	0	public List<Long> createCradleWardForUnit(@QueryParam("wardId") String wardId, @QueryParam("unitId") String unitId, @Context HttpServletRequest req) throws GenericApplicationExcept
custom\MdmServiceImpl.java	/mdm	GET	/deleteIpdRosterById	0	public void deleteIpdRosterById(@QueryParam("ipdRosterId") Long ipdRosterId)
custom\MdmServiceImpl.java	/mdm	GET	/getAllDesignationByCadreId	1	public List<CadreDesignationMaster> getAllDesignationByCadreIdApi(@QueryParam("cadreId") Long cadreId) throws GenericApplicationException 
custom\MdmServiceImpl.java	/mdm	GET	/getAllGrievanceSubCategoryByCategoryId	1	public List<GrievanceSubCategoryMaster> getAllGrievanceSubCategoryByCategoryId(@QueryParam("categoryId") Long categoryId) throws GenericApplicationException 
custom\MdmServiceImpl.java	/mdm	GET	/getAllGrievanceTicketCount	1	public Map<String,Long> getAllGrievanceTicketCount() throws GenericApplicationException 
custom\MdmServiceImpl.java	/mdm	GET	/getDepartmentCabinRoomMappingMaster	1	public List<DepartmentCabinRoomMappingMaster> getDepartmentCabinRoomMappingMaster(@QueryParam("page") Integer page, @QueryParam("count") Integer count, @QueryParam("search") String
custom\MdmServiceImpl.java	/mdm	GET	/getDepartmentRoomMappingMaster	0	public List<DepartmentRoomMappingMaster> getDepartmentRoomMappingMaster(@QueryParam("page") Integer page, @QueryParam("count") Integer count, @QueryParam("search") String search,
custom\MdmServiceImpl.java	/mdm	GET	/getDoctorIdByBillingDepartmentId	0	public String getDoctorIdByBillingDepartmentId(@QueryParam("id") Long id) throws GenericApplicationException 
custom\MdmServiceImpl.java	/mdm	GET	/getGrievanceSubCategoryDetailsById	0	public String getGrievanceSubCategoryMasterDetails(@QueryParam("id") Long id) throws GenericApplicationException 
custom\MdmServiceImpl.java	/mdm	GET	/getHelpdeskUsers	0	public List<Staffmaster> getHelpdeskUsers() throws GenericApplicationException 
custom\MdmServiceImpl.java	/mdm	GET	/getIpdRosterById	0	public IpdRosterDetails getDepartmentsAppointmentScheduleById(@QueryParam("ipdRosterId") Long ipdRosterId)
custom\MdmServiceImpl.java	/mdm	GET	/getPatientAllGrievanceTicketCount	1	public Map<String,Long> getPatientAllGrievanceTicketCount(@QueryParam("mobile") Long mobile, @HeaderParam("token") String token, @QueryParam("unitId") Long unitId) throws GenericAp
custom\MdmServiceImpl.java	/mdm	GET	/getPatientGrievanceTicketHistoryById	0	public List<GrievanceTicketHistoryDto> getPatientGrievanceTicketHistory(@QueryParam("grievanceId") Long grievanceId, @HeaderParam("token") String token) throws GenericApplicationEx
custom\MdmServiceImpl.java	/mdm	GET	/getRoomDetailById	0	public Long getRoomDetailByIdForQueue(@QueryParam("id") Long id) throws GenericApplicationException 
custom\MdmServiceImpl.java	/mdm	GET	/getRoomMasterForQueueList	0	public List<RoomMasterForQueue> getRoomMasterForQueueList(@QueryParam("page") Integer page, @QueryParam("count") Integer count, @QueryParam("search") String search,
custom\MdmServiceImpl.java	/mdm	GET	/getSSOIdListForSearchForGRN	0	public List<TUserdetails> getSSOIdListForSearchForGRN(@QueryParam("ssoId") String ssoId) throws GenericApplicationException 
custom\MdmServiceImpl.java	/mdm	GET	/getTicketsForDashboard	1	public Map<String,Object> getTicketsForDashboard(@QueryParam("status") Long status, @QueryParam("count") Long count) throws GenericApplicationException 
custom\MdmServiceImpl.java	/mdm	GET	/saveRghsData	0	public void saveSimsRghsData() throws GenericApplicationException 
custom\MdmServiceImpl.java	/mdm	POST	/addBedMaster	1	public List<String> addBedMaster(@HeaderParam("token") String token, @BodyParam("body") Bedmaster bedmaster, @Context HttpServletRequest req) throws GenericApplicationException 
custom\MdmServiceImpl.java	/mdm	POST	/addDepartmentCabinRoomMappingMaster	1	public void addDepartmentCabinRoomMappingMaster(@BodyParam("body") DepartmentCabinRoomMappingMaster body) throws GenericApplicationException 
custom\MdmServiceImpl.java	/mdm	POST	/addDepartmentMaster	1	public List<Departmentmaster> addDepartmentMaster(@BodyParam("body") Departmentmaster body, @Context HttpServletRequest req) throws GenericApplicationException 
custom\MdmServiceImpl.java	/mdm	POST	/addGrievanceDocument	1	public Boolean addGrievanceDocument(@QueryParam("grievanceId") Long grievanceId, @QueryParam("userId") Long userId,
custom\MdmServiceImpl.java	/mdm	POST	/addGrievenceComment	1	public Boolean addGrievenceComment(@BodyParam("body") PatientGrievanceComments body, @HeaderParam("token") String token, @QueryParam("isPatient") Boolean isPatient) throws GenericA
custom\MdmServiceImpl.java	/mdm	POST	/addGrievenceFeedback	1	public Boolean addGrievenceFeedback(@BodyParam("body") PatientGrievanceFeedback body, @HeaderParam("token") String token) throws GenericApplicationException 
custom\MdmServiceImpl.java	/mdm	POST	/addGrievenceTicket	0	public Boolean addGrievenceTicket(@BodyParam("body") PatientGrievanceHistory body, @HeaderParam("token") String token, @QueryParam("isCitizen") @DefaultValue("false") Boolean isCit
custom\MdmServiceImpl.java	/mdm	POST	/addIpdRosterDetails	1	public List<IpdRosterDetails> addIpdRosterDetails(@BodyParam("body") IpdRosterDetailsDto body) throws GenericApplicationException 
custom\MdmServiceImpl.java	/mdm	POST	/addservicemasterDetails	1	public List<String> addservicemasterDetails(@BodyParam("body") Servicemaster body, @Context HttpServletRequest req) throws GenericApplicationException 
custom\MdmServiceImpl.java	/mdm	POST	/addUnitMaster	0	public Unitmaster addUnit(@BodyParam("body") Unitmaster unitmaster, @Context HttpServletRequest req) throws GenericApplicationException, UnsupportedEncodingException 
custom\MdmServiceImpl.java	/mdm	POST	/addWardMaster	1	public Wardmaster addWardMaster(@BodyParam("body") Wardmaster wardmaster, @Context HttpServletRequest req) throws GenericApplicationException 
custom\MdmServiceImpl.java	/mdm	POST	/assignedTicketToUser	0	public Boolean assignedTicketToUser(@QueryParam("grievanceId") Long grievanceId, @QueryParam("employeeId") Long employeeId, @QueryParam("expectedDate") String expectedDate, @QueryP
custom\MdmServiceImpl.java	/mdm	POST	/changeGrievanceSeverity	1	public Boolean changeGrievanceSeverity(@QueryParam("grievanceId") Long grievanceId, @QueryParam("severityId") Long severityId) throws GenericApplicationException 
custom\MdmServiceImpl.java	/mdm	POST	/changeGrievanceTicketStatus	0	public Boolean changeGrievanceTicketStatus(@QueryParam("grievanceId") Long grievanceId, @QueryParam("issueStatus") Long issueStatus, @QueryParam("assignToCreator") @DefaultValue("f
custom\MdmServiceImpl.java	/mdm	POST	/changeServiceStatusById	0	public void changeServiceStatusById(@QueryParam("id") Long id) throws GenericApplicationException 
custom\MdmServiceImpl.java	/mdm	POST	/exposeBuckets	1	public Map<String, String> exposeBuckets() throws GenericApplicationException 
custom\MdmServiceImpl.java	/mdm	POST	/getAllGrievanceTickets	0	public List<PatientGrievanceHistory> getAllGrievanceTickets(@BodyParam("body") PatientDetailsListForSearchDto body, @QueryParam("page") Integer page,
custom\MdmServiceImpl.java	/mdm	POST	/getAllServicesBySpecId	1	public List<Servicemaster> getAllServicesBySpecId(@QueryParam("spec_id") Long specializationId, @QueryParam("unit_id") Long unitId) throws GenericApplicationException 
custom\MdmServiceImpl.java	/mdm	POST	/getIpdRosterDetails	1	public List<IpdRosterDetails> getIpdRosterDetailsList(@QueryParam("departmentId") String departmentIdStr, @QueryParam("buildingId") String buildingIdStr, @QueryParam("rosterType") 
custom\MdmServiceImpl.java	/mdm	POST	/getPatientGrievanceHistory	0	public List<PatientGrievanceHistory> getPatientGrievanceHistory(@BodyParam("body") PatientDetailsListForSearchDto body, @QueryParam("page") Integer page, @QueryParam("count") Integ
custom\MdmServiceImpl.java	/mdm	POST	/getSSOdetailsListForSearch	0	public Map<String,String> getSSOUSerDetailsall(@QueryParam("SSO_ID") String ssoId, @QueryParam("emp_id") String empId) throws GenericApplicationException 
custom\MdmServiceImpl.java	/mdm	POST	/getSSOEMPIdListForSearch	0	public List<SearchSSODetailsResultDto> getSSOEMPDetails(@BodyParam("body") Staffmaster body) throws GenericApplicationException 
custom\MdmServiceImpl.java	/mdm	POST	/getSSOIdListForSearch	1	public List<SearchSSODetailsResultDto> getSSODetails(@BodyParam("body") Staffmaster body) throws GenericApplicationException 
custom\MdmServiceImpl.java	/mdm	POST	/getTrxHistory	1	public List<TrxHistory> getTrxHistory(@BodyParam("body") TrxHistory body,@QueryParam("page") String pageStr, @QueryParam("count") String countStr) throws GenericApplicationExceptio
custom\MdmServiceImpl.java	/mdm	POST	/reopenGrievanceTicket	0	public Boolean reopenGrievanceTicket(@BodyParam("body") PatientGrievanceComments body, @HeaderParam("token") String token, @QueryParam("isRaiseTicket") Boolean isRaiseTicket) throw
custom\MdmServiceImpl.java	/mdm	POST	/saveUser	1	public void saveUser(@BodyParam("body") AddUserView body, @Context HttpServletRequest req) throws GenericApplicationException, UnsupportedEncodingException 
custom\MdmServiceImpl.java	/mdm	POST	/updateBedMaster/{id}	0	public Bedmaster updateBedMaster(@BodyParam("body") Bedmaster body, @PathParam("id") Long id, @Context HttpServletRequest req)
custom\MdmServiceImpl.java	/mdm	POST	/updateIpdRosterDetails	1	public List<IpdRosterDetails> updateIpdRosterDetailsList(@BodyParam("body") IpdRosterDetailsDto body) throws GenericApplicationException 
custom\MdmServiceImpl.java	/mdm	POST	/updateRadServices	1	public void updateRadServices(@BodyParam("body") Set<Long> radIds, @QueryParam("radCabinId") Long radCabinId) throws GenericApplicationException 
custom\MdmServiceImpl.java	/mdm	POST	/updateServicemasterList	1	public void updateServicemasterList(@BodyParam("body") Servicemaster body, @Context HttpServletRequest req) throws GenericApplicationException 
custom\MdmServiceImpl.java	/mdm	POST	/updateUser	1	public void updateUser(@BodyParam("body") AddUserView body, @Context HttpServletRequest req) throws GenericApplicationException, IOException 
custom\MdmServiceImpl.java	/mdm	POST	/updateUserStatus	1	public boolean updateUserStatus(@QueryParam("userId") Long userId) throws GenericApplicationException 
custom\MdmServiceImpl.java	/mdm	POST	/updateUserStores/{userId}	1	public void updateUserStores(@BodyParam("body") AddUserView body, @PathParam("userId") Long userId) throws GenericApplicationException 
custom\MdmServiceImpl.java	/mdm	POST	/updateUserVistaSign	1	public void updateUserVistaSign(@QueryParam("userId") Long userId, @QueryParam("newVistaSign") String newVistaSign) throws GenericApplicationException 
custom\MdmServiceImpl.java	/mdm	POST	/updateWardMaster/{id}	0	public void updateWardMaster(@PathParam("id") Long id) throws GenericApplicationException 
custom\MdmServiceImpl.java	/mdm	PUT	/deleteServiceListById/{id}	1	public void deleteServiceListById(@PathParam("id") Long id, @Context HttpServletRequest req) throws GenericApplicationException 
custom\MedicationServiceImpl.java	/medication	GET	/getItemByCategoryId	0	public List<ItemByItemCategoryResultDto> getItemsByCategoryID(@QueryParam("id") Long id, @QueryParam("storeId") @StoredProcNamedParam("p_storeid") Long storeId)
custom\MedicationServiceImpl.java	/medication	GET	/getItemByCategoryIdFast	0	public List<ItemByItemCategoryResultDto> getItemByCategoryIdFast(@QueryParam("id") Long id,
custom\MedicationServiceImpl.java	/medication	GET	/getItemByNameForNursing	0	public List<ItemByNameResultImprDto> getItemByNameForNursing(@QueryParam("storeId") @StoredProcNamedParam("p_storeid") Long storeId) throws GenericApplicationException 
custom\MedicationServiceImpl.java	/medication	GET	/getItemCategoryId	0	public List<ItemByItemCategoryResultDto> getItemsByCategoryID(@QueryParam("id") @StoredProcNamedParam("p_itemcategory") Long id, @QueryParam("itemids") @StoredProcNamedParam("p_ite
custom\MedicationServiceImpl.java	/medication	GET	/getItemSaleDetailByVisitId	0	public List<ItemSalesDetailsResultDto> getItemSaleDetailByItemSaleId(@QueryParam("visitId") Long visitId, @QueryParam("opd_Ipd") Long opd_Ipd) throws GenericApplicationException 
custom\MedicationServiceImpl.java	/medication	GET	/itemcategoryAndStore	0	public ItemCategoryStoreView getItemCategoryAndStore() throws GenericApplicationException 
custom\MedicationServiceImpl.java	/medication	GET	/itemcategoryByStoreIdAndStore	0	public ItemCategoryStoreView itemcategoryByStoreIdAndStore(@QueryParam("storeId") @StoredProcNamedParam("p_storeid") Long storeId) throws GenericApplicationException 
custom\MedicationServiceImpl.java	/medication	GET	/removeNurseItem	0	public void removeNurseItem(@QueryParam("id") Long id) throws GenericApplicationException 
custom\MedicationServiceImpl.java	/medication	GET	/updateItemSaleOrder	1	public void updateItemSaleOrder(@QueryParam("itemSaleId") Long itemSaleId) throws GenericApplicationException 
custom\MedicationServiceImpl.java	/medication	POST	/addItemPriceNew	0	public ItemSalesDto addItemPrice(@BodyParam("body") AddItemSalesView body) throws GenericApplicationException 
custom\MedicationServiceImpl.java	/medication	POST	/addItemSaleForCounterSale	0	public ItemSalesDto addItemSaleForCounterSale(@BodyParam("body") AddItemSalesView body) throws GenericApplicationException 
custom\MedicationServiceImpl.java	/medication	POST	/getAllItemAndItemBatchListByVisitID	0	public List<ItemAndItemBatchListByVisitIDResultDto> getAllItemAndItemBatchListByVisitID(@BodyParam("body") ItemAndItemBatchListByVisitIDDto body) throws GenericApplicationException
custom\MedicationServiceImpl.java	/medication	POST	/getItemAndItemBatchListByStoreId	0	public List<ItemAndItemBatchListByStoreIDResultDto> getItemAndItemBatchListByStoreId(@BodyParam("body") ItemAndItemBatchListByStoreIdDto body) throws GenericApplicationException 
custom\MedicationServiceImpl.java	/medication	POST	/getItemAndItemBatchListByVisitID	1	public List<ItemAndItemBatchListByVisitIDResultDto> getItemAndItemBatchListByVisitID(@BodyParam("body") ItemAndItemBatchListByVisitIDDto body) throws GenericApplicationException 
custom\MedicationServiceImpl.java	/medication	POST	/getItemAndItemBatchListByVisitIDForNurse	1	public List<ItemAndItemBatchListByVisitIDResultDto> getItemAndItemBatchListByVisitIDForNurse(@BodyParam("body") ItemAndItemBatchListByVisitIDDto body) throws GenericApplicationExce
custom\MedicationServiceImpl.java	/medication	POST	/getItemAndItemBatchListForNurseOrder	0	public List<ItemAndItemBatchListByStoreIDResultDto> getItemAndItemBatchListForNurseOrder(@BodyParam("body") ItemAndItemBatchListByStoreIdNewDto body) throws GenericApplicationExcep
custom\MedicationServiceImpl.java	/medication	POST	/getPharmacyItemSaleBill	1	public PharmacyBillListViewNew getPharmacyItemSaleBillService(@BodyParam("body") PharmacyBillDto body, @DefaultValue("false") @QueryParam("is_print") boolean print, @QueryParam("n
custom\MedicationServiceImpl.java	/medication	POST	/getPharmacyItemSaleBillForCounterSale	0	public PharmacyBillListViewNew getPharmacyItemSaleBillForCounterSale(@BodyParam("body") PharmacyBillDto body, @DefaultValue("false") @QueryParam("is_print") boolean print)
custom\MedicationServiceImpl.java	/medication	POST	/getPharmacySaleReturn	0	public BillReceiptView getPharmacySaleReturn(@BodyParam("body") PharmacySaleReturnBillDto body, @DefaultValue("false") @QueryParam("is_print") boolean print) throws GenericApplicat
custom\MedicationServiceImpl.java	/medication	POST	/updateItemReturns	0	public List<TemSaleReturnDto> updateItemReturns(@BodyParam("body") AddItemSalesView body) throws GenericApplicationException 
custom\MedicationServiceImpl.java	/medication	PUT	/dispenseItemSale	0	public void dispenseItemSale(@QueryParam("itemSaleId") Long itemSaleId, @QueryParam("itemSaleDetailId") Long itemSaleDetailId) throws GenericApplicationException 
custom\MedicationServiceImpl.java	/medication	PUT	/updateCreditPaymentItemSale	1	public ItemSalesItemResultDto updateCreditPaymentItemSale(@BodyParam("body") ItemSalePartialPayView body) throws GenericApplicationException 
custom\MedicationServiceImpl.java	/medication	PUT	/updatePrescInstruction	1	public void updatePrescInstruction(@QueryParam("presId") Long presId, @QueryParam("instruction") String instruction) 
custom\NoteDetailsServiceImpl.java	/notedetailsdata	POST	/addUpdateNoteTitleMaster	0	public NotetitleMaster addUpdateNoteTitleMaster(@BodyParam("body") NotetitleMaster noteTitle) throws GenericApplicationException 
custom\NoteDetailsServiceImpl.java	/notedetailsdata	POST	/checkUnsignedNoteIsAvailable	0	public NoteDetails checkUnsignedNoteIsAvailable(@BodyParam("body") NoteDetails noteDetails) throws GenericApplicationException 
custom\NoteDetailsServiceImpl.java	/notedetailsdata	POST	/getOPNoteDetails	1	public NoteDetails getOPNoteDetails(@BodyParam("body") NoteDetails noteDetails) throws GenericApplicationException 
custom\NoteDetailsServiceImpl.java	/notedetailsdata	POST	/savenoteDetails	0	public NoteDetails saveNoteDetails(@BodyParam("body") NoteDetails noteDetails) throws GenericApplicationException 
custom\NoteDetailsServiceImpl.java	/notedetailsdata	POST	/saveOPNoteDetails	1	public NoteDetails saveOPNoteDetails(@BodyParam("body") NoteDetails noteDetails) throws GenericApplicationException 
custom\NoteDetailsServiceImpl.java	/notedetailsdata	PUT	/note/{id}	0	public NoteDetails notedetailsById(@BodyParam("body") NoteDetails noteDetails,@PathParam("id") Long id) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	GET	/document/{id}	1	public void getImageById(@PathParam("id") String id, @Context HttpServletResponse response) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	GET	/documentActualImage/{id}	0	public void getActualImageByIdForLabResult(@PathParam("id") String hashId, @Context HttpServletResponse response) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	GET	/documentImage/{id}	0	public String getImageByIdForLabResult(@HeaderParam("token") String token, @PathParam("id") String id) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	GET	/downloadPathoResultApi	0	public List<String> getPathoResultApiPrint(@QueryParam("unitId") Long unitId, @QueryParam("histoNoSrch") String histoNoSrch, @QueryParam("mrnNo") String mrnNo,@QueryParam("billNo")
custom\PathoOrderServiceImpl.java	/pathoOrder	GET	/getAllOrganUnmapped	0	public List<Organmaster> getAllOrganUnmapped() throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	GET	/getAllPathoTestUnmapped	0	public List<Pathotestmaster> getAllPathoTestUnmapped() throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	GET	/getAllServiceUnmapped	0	public List<Servicemaster> getAllServiceUnmapped() throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	GET	/getMachineTestMaster/{id}	1	public Machinetestmaster getMachineTestMaster(@PathParam("id") Long id) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	GET	/getMachineTestMasterList	1	public List<Machinetestmaster> getMachineTestMasterList(@QueryParam("page") Integer page, @QueryParam("count") Integer count,
custom\PathoOrderServiceImpl.java	/pathoOrder	GET	/getMrnNo	0	public PathoSampleSearchResultDto getMrnNo(@QueryParam("sampleNoStr") String sampleNoStr, @QueryParam("unitId") Long unitId, @QueryParam("isBloodBagNo") boolean isBloodBagNo, @Quer
custom\PathoOrderServiceImpl.java	/pathoOrder	GET	/getMrnNoRad	1	public PathoSampleSearchResultDto getMrnNoRad(@QueryParam("sampleNoStr") String sampleNoStr, @QueryParam("unitId") Long unitId, @QueryParam("billNoStr") String billNoStr) throws Ge
custom\PathoOrderServiceImpl.java	/pathoOrder	GET	/getOrderDetails	1	public List<TestOrderDetailResultDto> getOrderDetails(@QueryParam("orderNo") @StoredProcNamedParam("p_orderno") Long orderNo, @QueryParam("unitId") @StoredProcNamedParam("p_unitid"
custom\PathoOrderServiceImpl.java	/pathoOrder	GET	/getPathologyQueue	0	public List<Cabinmaster> getPathologyQueue(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	GET	/getPathoOrganismListById/{id}	1	public Pathorganismmaster getPathoOrganismListById(@PathParam("id") Long id) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	GET	/getPathoParameterListById	1	public Pathoparametermaster getPathoParameterListById(@QueryParam("id") Long id) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	GET	/getPathoProfileListById/{id}	1	public Pathoprofilemaster getPathoProfileListById(@PathParam("id") Long id) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	GET	/getPathoTestDetailsById/{id}	1	public Pathotestmaster getwholePatoTestMasterId(@PathParam("id") Long Id) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	GET	/getPatientDocumentsList	1	public List<PathOrderBookingDetailDocuments> getPatientDocumentsList(@QueryParam("orderid") Long orderid, @QueryParam("sampleNoStr") String sampleNoStr,
custom\PathoOrderServiceImpl.java	/pathoOrder	GET	/getRadImage	0	public byte[] findRadImage(@QueryParam("radOrderId") Long radOrderId, @QueryParam("radtestId") Long radtestId, @QueryParam("unitId") Long unitId) throws GenericApplicationException
custom\PathoOrderServiceImpl.java	/pathoOrder	GET	/getRadResult	1	public List<GetRadResult> getRadResult(@QueryParam("testId") @StoredProcNamedParam("p_testid") Long testId, @QueryParam("unitId") @StoredProcNamedParam("p_unitid") Long unitId,
custom\PathoOrderServiceImpl.java	/pathoOrder	GET	/getRadTestImage	1	public String getRadTestImage(@QueryParam("radOrderId") Long radOrderId, @QueryParam("radtestId") Long radtestId, @QueryParam("unitId") Long unitId) throws GenericApplicationExcept
custom\PathoOrderServiceImpl.java	/pathoOrder	GET	/getTemplateAndItemDetails	1	public List<TemplateAndItemDetailsResultDto_1> getTemplateAndItemDetails(@QueryParam("testId") @StoredProcNamedParam("p_testid") Long testId,
custom\PathoOrderServiceImpl.java	/pathoOrder	GET	/getTemplateResult	0	public List<List<PathTemplateTestResultDto>> getTemplateResult(@QueryParam("orderId") Long orderId, @QueryParam("sampleNoStr") String sampleNoStr, @QueryParam("unitId") Long unitId
custom\PathoOrderServiceImpl.java	/pathoOrder	GET	/getTestDetailsByOrderId	0	public PatientLabTestDetailsDto getTestDetailsByOrderId(@QueryParam("orderId") Long orderId,@QueryParam("opdIpd") Long opdIpd) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	GET	/getUnitsForCovidApproval	1	public List<Unitmaster> getUnitsForCovidApproval(@QueryParam("fromDate") String fromDate, @QueryParam("toDate") String toDate) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	GET	/getUnitsForOutSourcedTest	0	public List<Unitmaster> getUnitsForOutSourcedTest(@QueryParam("fromDate") String fromDate, @QueryParam("toDate") String toDate,
custom\PathoOrderServiceImpl.java	/pathoOrder	GET	/getUnitsForOutSourcedTestAll	0	public List<Unitmaster> getUnitsForOutSourcedTestAll(@QueryParam("showLoginUnit") Boolean showLoginUnit) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	GET	/getUnitWiseServices	0	public List<Servicemaster> getUnitWiseService(@QueryParam("pathoLocationId") Long pathoLocationId) throws GenericApplicationException
custom\PathoOrderServiceImpl.java	/pathoOrder	GET	/printPathoResultApi	0	public List<String> getprintPathoResultApi(@QueryParam("unitId") Long unitId,@QueryParam("mrnNo") String mrnNo,@QueryParam("billNo") String billNo,@QueryParam("opdIpdNo") String op
custom\PathoOrderServiceImpl.java	/pathoOrder	GET	/reportReleaseConfirmation	0	public Long reportReleaseConfirmation(@QueryParam("pass") String pass) throws GenericApplicationException, Exception 
custom\PathoOrderServiceImpl.java	/pathoOrder	GET	/updatePriority	0	public Void updatePriority(@QueryParam("chargeid") Long chargeid, @QueryParam("priority") Long priority) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/acceptRadOrder	0	public List<RadOrderBookingListForWorkOrderResultDto_New> acceptRadOrder(@BodyParam("body") AddRadResultEntryDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/addMachineTestMaster	0	public List<Machinetestmaster> addMachineTestMaster(@BodyParam("body") Machinetestmaster body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/addMachineWiseMicroResult	0	public List<String> addMachineWiseMicroResult(@BodyParam("body") List<MachineWiseMicroResult> body, @QueryParam("token") String token) throws GenericApplicationException, Interrupt
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/addMachineWiseResult	1	public Boolean addMachineWiseResult(@BodyParam("body") AddMachineResultParameterView body, @QueryParam("token") String token) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/addPathoOrganismList	0	public void addPathoOrganismList(@BodyParam("body") Pathorganismmaster body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/addPathoParameterList	0	public void addPathoParameterList(@BodyParam("body") Pathoparametermaster body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/addPathoProfileList	0	public void addPathoProfileList(@BodyParam("body") Pathoprofilemaster body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/addPathoResultEntryTemplate	0	public PathoResultEntryTemplateDto addPathoResultEntryTemplate(@BodyParam("body") PathoResultEntryTemplateDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/addPathoTestMaster	0	public void addPathoTestMaster(@BodyParam("body") Pathotestmaster body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/addPathPatientTestParameterDetails	1	public void addPathPatientTestParameterDetails(@BodyParam("body") List<PathPatientParameterDetailsDto> body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/addPatientMicroResult	0	public List<PathPatientCultureandSensitivityDetailsDto> addPatientMicroResult(@BodyParam("body") List<PathPatientCultureandSensitivityDetailsDto> body) throws GenericApplicationExc
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/addRadResultEntry	1	public AddRadResultEntryDto addRadResultEntry(@BodyParam("body") AddRadResultEntryDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/addResultReviewHoldRecord	1	public void addResultReviewHoldRecord(@BodyParam("body") PathOrderDto2 body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/addResultReviewNotes	1	public void addResultReviewNotes(@BodyParam("body") PathOrderDto2 body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/approveCovidLabTest	0	public void approveCovidLabTest(@BodyParam("body") List<PathOrderDto2> body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/checkChargeInfo	0	public Boolean checkChargeInfo(@BodyParam("body") Map<String, List<UpdatePathOrderBookingDetailDto>> updatePathDetailDtos) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/checkChargeInfoForRad	0	public Boolean checkChargeInfoForRad(@BodyParam("body") ImageCapturedForRadiologyDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/fetchLabTestId	0	public List<Pathotestmaster> fetchLabTest(@BodyParam("body") PathoWorkOrderListResultDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/finalizeRadResult	1	public void finalizeRadResult(@BodyParam("body") UpdateRadResultEntryDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/generateBarCode	0	public Map<String, List<PathOrderBookingDetailList_NEWResultDto_1>> generateBarCodeApi(@BodyParam("body") PathOrderBookingListDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/getAllPathoListByUnitId	1	public List<PathoWorkOrderListResultDto> getAllPathoListByUnitId(@BodyParam("body") PathoWorkOrderListResultDto body,@QueryParam("page") Integer page, @QueryParam("count") Integer 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/getCultureSenResultForTest	1	public PathoCutureView getCultureSenResultForTest(@BodyParam("body") PathOrderBookingListDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/getCultureSentResult	1	public PathoCutureView getCultureSenResult(@BodyParam("body") PathOrderBookingListDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/getCultureSentResult_lastresult	1	public PathoCutureView getCultureSenResult_lastresult(@BodyParam("body") PathOrderBookingListDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/getGroupingTestParametersforDonorServices	1	public Map<String, List<PathTestAndSubTestParameterResultDto>> getGroupingTestParametersforDonorServices(@BodyParam("body") PathoSampleSearchResultDto body, @QueryParam("page") Int
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/getKdlVerifyList	0	public LabWorkLoadListView getKdlVerifyList(@BodyParam("body") PathoWorkOrderListResultDto body,@QueryParam("page") Integer page,
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/getLabBucketNoForMicro	1	public String getLabBucketNoForMicro(@BodyParam("body") PathdtoforAPI body, @QueryParam("token") String token) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/getMachineWiseResultReview	0	public Map<String, List<MachineWiseTestResultDto>> getMachineWiseResultReview(@BodyParam("body") PathNormalTestDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/getNormalResult	0	public List<List<PathNormalTestResultDto>> getNormalResult(@BodyParam("body") List<PathFinalizeDto> body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/getPacOrderSetResult	0	public PacLabResultStatus getPacOrderSetResult(@QueryParam("orderId") Long orderId,@QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/getParameterResultValue	1	public PathParamsView getParameterResultValue(@BodyParam("body") PathOrderBookingListDto body, @QueryParam("token") String token) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/getPathoResultApi	0	public GetPathoResultApi getPathoResultApi(@QueryParam("unitId") Long unitId,@QueryParam("mrnNo") String mrnNo,@QueryParam("billNo") String billNo,@QueryParam("billId") @DefaultVal
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/getPathoResultApiForComparisonSheet	0	public Map<String,Object> getPathoResultApiForComparisonSheet(@QueryParam("unitId") Long unitId,@QueryParam("mrnNo") String mrnNo,@QueryParam("billNo") String billNo,@QueryParam("b
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/getPathoResultApiForDischargeSummary	0	public Map<String,Object> getPathoResultApiForDischargeSummaryPrint(@QueryParam("unitId") Long unitId,@QueryParam("mrnNo") String mrnNo,@QueryParam("billNo") String billNo,@QueryPa
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/getPathoResultPrint	0	public List<PathResultView> getPathoResultPrint(@BodyParam("body") GetPathoResultApi body,@QueryParam("opdIpd") Long opdIpd, @DefaultValue("false") boolean mergePdfReports) throws 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/getPathoTemplateList	0	public PathTempletView getPathoTemplateList(@BodyParam("body") PathOrderBookingListDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/getPathoTemplateListForTest	0	public PathTempletView getPathoTemplateListForTest(@BodyParam("body") PathOrderBookingListDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/getPathoWorklist	0	public List<PathoWorkOrderListResultDto> getPathoWorklist(@BodyParam("body") PathWorklistDtoMachineInterface body, @QueryParam("token") String token) throws GenericApplicationExcep
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/getPreviousDetailsForPat	0	public Map<java.sql.Timestamp, List<PathPreviousNormalTestResultDto>> getPreviousDetailsForPat(@BodyParam("body") ResultReviewDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/getServices	1	public List<ServiceHoldRequest> getServices(@QueryParam("pathoLocationId") Long pathoLocationId, @QueryParam("tariffId") Long tariffId, @BodyParam("body") ServiceHoldRequest body)
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/getTestParameterAndSubTestDetail	0	public PathParamsView getTestParameterAndSubTestDetail(@BodyParam("body") PathOrderBookingListDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/getTestParameterAndSubTestDetails	1	public PathParamsView getTestParameterAndSubTestDetails(@BodyParam("body") PathOrderBookingListDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/getTestParameterAndSubTestDetailsForTest	0	public PathParamsView getTestParameterAndSubTestDetailsForTest(@BodyParam("body") PathOrderBookingListDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/getTestParametersAndTemplates	1	public PathoTypeView getTestParametersAndTemplatesApi(@BodyParam("body") PathOrderBookingListDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/getUpdatedResultReview	1	public Map<java.sql.Timestamp, List<PathNormalTestResultDto>> getUpdatedResultReview(@BodyParam("body") ResultReviewDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/getUserDataFromBarCode	0	public PathResultdtoforAPI getUserDataFromBarCode(@BodyParam("body") PathdtoforAPI body, @QueryParam("token") String token) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/holdServiceforBilling	0	public Boolean holdServiceforBilling(@BodyParam("body") ServiceHoldRequest body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/imageCaptured	0	public Long setImageCapturedForRadiology(@BodyParam("body") ImageCapturedForRadiologyDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/labPatientDemographicDetail	1	public Map<Long, Map<String, List<PathOrderBookingDetailList_NEWResultDto_1>>> labPatientDemographicDetail(@BodyParam("body") PathOrderBookingListDto body,
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/notifyPatientDetails	0	public String notifyPatientDetails(@BodyParam("body") NotifyPatientCriticalDetails body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/pathLabCovidStatusList	0	public List<PathoWorkOrderListResultDto> pathLabCovidStatusList(@BodyParam("body") PathoWorkOrderListResultDto body,@QueryParam("page") Integer page,
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/pathLabList	0	public Map<Long, Map<String, List<PathOrderBookingDetailList_NEWResultDto_1>>> pathLabList_New(@BodyParam("body") PathOrderBookingListDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/pathLabList_AcceptFilter	0	public PathologyFilterCount pathLabList_AcceptFilter(@BodyParam("body") PathOrderBookingListDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/pathLabList_CollectionFilter	0	public PathologyFilterCount pathLabList_CollectionFilter(@BodyParam("body") PathOrderBookingListDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/pathLabList_filter	0	public Map<Long, Map<String, List<PathOrderBookingDetailList_NEWResultDto_1>>> pathLabList_filter(@BodyParam("body") PathOrderBookingListDto body) throws GenericApplicationExceptio
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/pathLabList_MachineFilter	0	public PathologyFilterCount pathLabList_MachineFilter(@BodyParam("body") PathOrderBookingListDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/pathLabList_old	1	public Map<Long, Map<String, List<PathOrderBookingDetailList_NEWResultDto_1>>> pathLabList_old(@BodyParam("body") PathOrderBookingListDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/pathLabList_ReleaseFilter	0	public PathologyFilterCount pathLabList_ReleaseFilter(@BodyParam("body") PathOrderBookingListDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/pathLabList_ResultFilter	0	public PathologyFilterCount pathLabList_ResultFilter(@BodyParam("body") PathOrderBookingListDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/pathLabList_ReviewFilter	0	public PathologyFilterCount pathLabList_ReviewFilter(@BodyParam("body") PathOrderBookingListDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/pathLabResultForOthermodule	0	public PathApiResultView pathLabResultForOthermodule(@BodyParam("body") PathOrderBookingListDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/pathLabStatusList	0	public List<PathoWorkOrderListResultDto> pathLabStatusList(@BodyParam("body") PathoWorkOrderListResultDto body,@QueryParam("page") Integer page,
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/pathLabStatusListNew	0	public List<PathoWorkOrderListResultDto> pathLabStatusListNew(@BodyParam("body") PathoWorkOrderListResultDto body,@QueryParam("page") Integer page,
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/pathLabWorkLoadList	1	public LabWorkLoadListView labWorkLoadListNew(@BodyParam("body") PathoWorkOrderListResultDto body,@QueryParam("page") Integer page,
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/pathoCollectionReports	0	public Map<String, List<PathOrderBookingDetailList_NEWResultDto_1>> pathoCollectionReports(@BodyParam("body") PathOrderBookingListDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/pathoCollectionReportsNew	0	public Map<String, List<PathOrderBookingDetailList_NEWResultDto_1>> pathoCollectionReportsNew(@BodyParam("body") PathOrderBookingListDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/pathoCollectionSampleReports	0	public List<PathOrderBookingDetailList_NEWResultDto_1> getPatientSampleDetailList(@BodyParam("body") PathOrderBookingListDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/pathoSamplePrint	1	public Boolean pathoSamplePrint(@BodyParam("body") List<PathLabResultDto> body, @DefaultValue("True") @QueryParam("is_print") boolean print) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/pathoTemplateReport	0	public PathResultView pathoLabReport(@BodyParam("body") List<PathLabResultDto> body, @DefaultValue("True") @QueryParam("is_print") boolean print,@DefaultValue("false") @QueryParam(
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/pathoTestInfoDetails	0	public List<PathoTestInfoDetailsDto> getpathoTestInfoDetails(@QueryParam("orderId") Long orderId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/pathPatientWorkList	1	public List<PathoWorkOrderListResultDto> pathPatientWorkList(@BodyParam("body") PathoWorkOrderListResultDto body,@QueryParam("page") Integer page,
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/patientMachinePathLabList	0	public Map<Long, Map<String, List<PathOrderBookingDetailList_NEWResultDto_1>>> patientMachinePathLabList(@QueryParam("page") Integer page,
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/patientPathLabList	0	public Map<Long, Map<String, List<PathOrderBookingDetailList_NEWResultDto_1>>> patientPathLabList(@QueryParam("page") Integer page,
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/patientPathoCollectionReports	0	public Map<String, List<PathOrderBookingDetailList_NEWResultDto_1>> patientPathoCollectionReports(@BodyParam("body") PathOrderBookingListDto body) throws GenericApplicationExceptio
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/patientRadList	0	public Map<Long, Map<String, List<RadOrderBookingList_New2ResultDto>>> getPatientRadList(@QueryParam("page") Integer page,
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/patientRadTestDetailsList	0	public List<RadOrderBookingListForWorkOrderResultDto_New> getPatientRadTestDetailsList(@BodyParam("body") PathOrderBookingListDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/patientRadTestList	0	public Map<Long, List<RadOrderBookingListForWorkOrderResultDto_New>> getPatientRadTestList(@BodyParam("body") PathOrderBookingListDto body, @QueryParam("sampleNoStr") String sample
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/patientRadTestListForHL7	0	public List<Outboundhis> getPatientRadTestListForHL7(@BodyParam("body") PathOrderBookingListDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/printPathoResultApiVM	1	public PathApiResultView printPathoResultApiVM(@BodyParam("body") PathApiResultView body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/printPatientLabTestDetails	1	public String printPatientLabTestDetails(@BodyParam("body") List<PathTemplateReportResultDto> body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/radLabListNew	0	public Map<Long, List<RadOrderBookingListForWorkOrderResultDto_New>> radLabListNew(@BodyParam("body") PathOrderBookingListDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/radListFilter	0	public PathologyFilterCount radListFilter(@BodyParam("body") PathOrderBookingListDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/radReschedule	0	public void radReschedule(@BodyParam("body") RescheduleRadOrderDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/radRetest	0	public RadRejectAndRetestDto radRetest(@BodyParam("body") RadRejectAndRetestDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/sampleAccepted	0	public void saveAccepted(@BodyParam("body") SampleAcceptDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/sampleRejected	1	public Long sampleRejected(@BodyParam("body") RejectResultDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/sendOTPForKDLVerify	0	public OnlineAppointmentDto sendOTPForKDLVerify(@QueryParam("otpSendType") String otpSendType, @QueryParam("userId") Long userId) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/statusScreenLabReportPrint	0	public String nursingLabReportPrint(@BodyParam("body") List<PathLabResultDto> body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/updateKDLVerification	0	public Boolean updateKDLVerification(@BodyParam("body") List<IntegrationPrescriptions> body) 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/updatePathologyTestMaster	0	public Pathotestmaster updatePathoTestMaster(@BodyParam("body") Pathotestmaster body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/updatePathoOrderStatus	0	public boolean updatePathoOrderStatus(@BodyParam("body") PathOrderBookingListDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/updatePathoParameterList	1	public Pathoparametermaster updatePathoParameterList(@BodyParam("body") Pathoparametermaster body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/updatePathOrderBookingDetail	0	public Map<String, List<UpdatePathOrderBookingDetailDto>> pathoOrderBookingDetailUpdate(@BodyParam("body") Map<String, List<UpdatePathOrderBookingDetailDto>> updatePathDetailDtos)
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/updatePathoResultEntryTemplate	0	public void updatePathoResultEntryTemplate(@BodyParam("body") PathoResultUpdateTemplateDto body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/updatePathPatientTestParameterDetails	1	public void updatePathPatientTestParameterDetails(@BodyParam("body") UpdateLabResultView body) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/updateServices	0	public void updateServices(@QueryParam("requestStatus") Long requestStatus, @QueryParam("id") Long id)
custom\PathoOrderServiceImpl.java	/pathoOrder	POST	/validateOTPForKDLVerify	0	public OnlineAppointmentDto validateOTPForKDLVerify(@QueryParam("otpSendType") String otpSendType, @QueryParam("userId") Long userId, @QueryParam("otp") String otp, @QueryParam("un
custom\PathoOrderServiceImpl.java	/pathoOrder	PUT	/addPatientLabResultDocument	1	public PatientIDDocumentDto addPatientLabResultDocument(@BodyParam("body") PatientIDDocumentDto body, @QueryParam("orderid") Long orderid, @QueryParam("sampleNoStr") String sampleN
custom\PathoOrderServiceImpl.java	/pathoOrder	PUT	/removePatientPortalDocument	0	public void removePatientPortalDocument(@QueryParam("orderid") Long orderid, @QueryParam("sampleNoStr") String sampleNoStr, @QueryParam("unitId") Long unitId)
custom\PathoOrderServiceImpl.java	/pathoOrder	PUT	/updateMachineTestMaster/{id}	0	public Machinetestmaster updateMachineTestMaster(@BodyParam("body") Machinetestmaster body, @PathParam("id") Long id) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	PUT	/updatePathoOrganismList/{id}	1	public Pathorganismmaster updatePathoOrganismList(@BodyParam("body") Pathorganismmaster body, @PathParam("id") Long id) throws GenericApplicationException 
custom\PathoOrderServiceImpl.java	/pathoOrder	PUT	/updatePathoProfileList/{id}	1	public Pathoprofilemaster updatePathoProfileList(@BodyParam("body") Pathoprofilemaster body, @PathParam("id") Long id) throws GenericApplicationException 
custom\PatientDetailRegistrationServiceImpl.java	/patientDetailReg	GET	/getPatientLatestBilling	0	public PatientBillSearchView findPatientLatestBilling(@QueryParam("patientId") Long patientId, @QueryParam("isPharmacy") Boolean isPharmacy, @QueryParam("isCrossVisit") Boolean isC
custom\PatientDetailRegistrationServiceImpl.java	/patientDetailReg	GET	/getPatientLatestVisit	1	public Object[] getPatientLatestVisit(@QueryParam("mrNO") String mRNO) throws GenericApplicationException 
custom\PatientDetailRegistrationServiceImpl.java	/patientDetailReg	POST	/getIPDPatientGeneralDetailsSearch	0	public List<SearchPatientAndVisitByNameMobNoResultDto> getIPDPatientGeneralDetailsForService(@BodyParam("body") SearchPatientAndAdmissionByNameMobNoDto body) throws GenericApplicat
custom\PatientDetailRegistrationServiceImpl.java	/patientDetailReg	POST	/getIPDPatientGeneralDetailsSearchforOpBill	0	public CheckoutView getIPDPatientGeneralDetailsForServiceformBM(@BodyParam("body") SearchPatientAndAdmissionByNameMobNoDto body) throws GenericApplicationException 
custom\PatientDetailRegistrationServiceImpl.java	/patientDetailReg	POST	/getMotherChildLinkingDetailsDocit	0	public
custom\PatientDetailRegistrationServiceImpl.java	/patientDetailReg	POST	/getPatient	0	public PatientAdmissionView getPatient(@BodyParam("body") GetPatientDto body) throws GenericApplicationException 
custom\PatientDetailRegistrationServiceImpl.java	/patientDetailReg	POST	/getPatientGeneralDetailsSearch	1	public List<SearchPatientAndVisitByNameMobUpdatedResultDto> getPatientGeneralDetailsForService(@BodyParam("body") SearchPatientAndVisitByNameMobNoDto body) throws GenericApplicatio
custom\PatientDetailRegistrationServiceImpl.java	/patientDetailReg	POST	/getPatientGeneralDetailsSearchforIpbill	0	public CheckoutView getPatientGeneralDetailsForServicefromCheckout(@BodyParam("body") SearchPatientAndVisitByNameMobNoDto body) throws GenericApplicationException 
custom\PatientDetailRegistrationServiceImpl.java	/patientDetailReg	POST	/getPatientHealthPlanConsumptionSearch	0	public List<SearchHealthPlanConsumptionResultDTO> getPatientHealthPlanConsumptionSearch(@BodyParam("body") SearchHealthPlanConsumption body) throws GenericApplicationException 
custom\PatientDetailRegistrationServiceImpl.java	/patientDetailReg	POST	/updatePatient	0	public void updatePatient(@BodyParam("body") PatientAppointmentandRegDto body) throws GenericApplicationException 
custom\PatientDetailRegistrationServiceImpl.java	/patientDetailReg	POST	/updatePatientAddressDetails	0	public void updatePatientAddressDetails(@BodyParam("body") PatientAppointmentandRegDto body) throws GenericApplicationException 
custom\PatientRegAdmissionServiceImpl.java	/patientRegAddmission	DELETE	/removeMLCFileToById/{id}	0	public void removeMLCFileToById(@PathParam("id") Long id) throws GenericApplicationException 
custom\PatientRegAdmissionServiceImpl.java	/patientRegAddmission	GET	/checkAprovedOrReject	1	public Object checkAprovedOrReject(@QueryParam("admId") Long admId) throws GenericApplicationException 
custom\PatientRegAdmissionServiceImpl.java	/patientRegAddmission	GET	/DepartmentWiseIPDCount	0	public Map<Object,Object> departmentWiseIpdCounts(@QueryParam("index") Long index, @QueryParam("type") String type) throws GenericApplicationException 
custom\PatientRegAdmissionServiceImpl.java	/patientRegAddmission	GET	/getAllPreviuosAdmissionsOfUnit	1	public List<AllPreviuosAdmissionsOfUnitDto> getAllPreviuosAdmissionsOfUnit(@QueryParam("page") Integer page, @QueryParam("count") Integer count, @QueryParam("unitId") Long unitId) 
custom\PatientRegAdmissionServiceImpl.java	/patientRegAddmission	GET	/getMLCInfo	1	public MLCDetails getMLCInfo(@QueryParam("unitId") Long unitId, @QueryParam("admmId") Long admId, @QueryParam("opdIpd") Long opdIpd) throws GenericApplicationException 
custom\PatientRegAdmissionServiceImpl.java	/patientRegAddmission	GET	/getMlcStatusForIpList	1	public List<MlcClearanceDto> getMlcStatusForIpList(@QueryParam("admId") Long admId,@QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\PatientRegAdmissionServiceImpl.java	/patientRegAddmission	GET	/getPatientInvestigations	1	public List<InvestigationAndBillResultDto> getPatientInvestigations(@QueryParam("opdIpdId") Long opdIpdId,@QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\PatientRegAdmissionServiceImpl.java	/patientRegAddmission	GET	/IpdDashboard	1	public Map<String, Map<Object, Object>> IpdCounts() throws GenericApplicationException 
custom\PatientRegAdmissionServiceImpl.java	/patientRegAddmission	GET	/PatientCount	1	public IhmsDetailsResponseDto patientCount(@QueryParam("token") String token) throws GenericApplicationException
custom\PatientRegAdmissionServiceImpl.java	/patientRegAddmission	GET	/pdfAdmFaceSheet	1	public String[] pdfAppointmentFaceSheet(@QueryParam("pat_id") Long patientId, @QueryParam("adm_id") Long adm_id) throws GenericApplicationException 
custom\PatientRegAdmissionServiceImpl.java	/patientRegAddmission	GET	/reIntiateMlcStatus	1	public void updateRejectStatus(@QueryParam("id") Long id) throws GenericApplicationException 
custom\PatientRegAdmissionServiceImpl.java	/patientRegAddmission	GET	/unitWiseIPDCount	0	public Map<Object,Object> unitWiseIpdCounts(@QueryParam("index") Long index, @QueryParam("departmentId") Long departmentId, @QueryParam("type") String type) throws GenericApplicati
custom\PatientRegAdmissionServiceImpl.java	/patientRegAddmission	GET	/updateAprrovedStatus	0	public void updateApproveStatus(@QueryParam("id") Long id,@QueryParam("remark") String remark, @QueryParam("mlcclearancetype") Long mlcclearancetype) throws GenericApplicationExcep
custom\PatientRegAdmissionServiceImpl.java	/patientRegAddmission	GET	/updateRejectStatus	1	public void updateRejectStatus(@QueryParam("id") Long id,@QueryParam("remark") String remark, @QueryParam("mlcclearancetype") Long mlcclearancetype) throws GenericApplicationExcept
custom\PatientRegAdmissionServiceImpl.java	/patientRegAddmission	GET	/updateReleaseAcceptStatus	0	public void updateReleaseAcceptStatus(@QueryParam("admId") Long admId,@QueryParam("remark") String remark) throws GenericApplicationException 
custom\PatientRegAdmissionServiceImpl.java	/patientRegAddmission	GET	/updateReleaseRejectStatus	1	public void updateReleaseRejectStatus(@QueryParam("admId") Long admId,@QueryParam("remark") String remark) throws GenericApplicationException 
custom\PatientRegAdmissionServiceImpl.java	/patientRegAddmission	GET	/WardWiseIPDCount	0	public Map<Object,Object> wardWiseIpdCounts(@QueryParam("index") Long index, @QueryParam("departmentId") Long departmentId, @QueryParam("buildingId") Long buildingId, @QueryParam("
custom\PatientRegAdmissionServiceImpl.java	/patientRegAddmission	GET	/WardWiseIPDDetail	0	public List<Object []> wardWiseIpdDetails(@QueryParam("index") Long index, @QueryParam("departmentId" ) Long departmentId, @QueryParam("wardId" ) Long wardId, @QueryParam("building
custom\PatientRegAdmissionServiceImpl.java	/patientRegAddmission	POST	/adddata	0	public PatientAdmissionView admitPatient(@BodyParam("body") PatientAdmissionView body) throws GenericApplicationException 
custom\PatientRegAdmissionServiceImpl.java	/patientRegAddmission	POST	/addUpdateMLCDetails	0	public MLCDetails addUpdateMLCDetails(MLCDetails body) throws GenericApplicationException 
custom\PatientRegAdmissionServiceImpl.java	/patientRegAddmission	POST	/getAllMlcCleranceList	1	public List<MlcClearanceDto> getAllMlcClearanceList(@BodyParam("body") MLCDetails body,@QueryParam("page") Integer page,
custom\PatientRegAdmissionServiceImpl.java	/patientRegAddmission	POST	/updateDoctor	0	public UpdateDoctorInformation updateDoctorInformation(UpdateDoctorInformation body) throws GenericApplicationException 
custom\PatientRegAdmissionServiceImpl.java	/patientRegAddmission	POST	/updateMLCRemark	1	public void updateMLCRemark(@QueryParam("id") Long id,@QueryParam("remark") String remark, @QueryParam("mlcclearancetype") Long mlcclearancetype) throws GenericApplicationException
custom\PatientRegApptServiceImpl.java	/patientRegAppt	GET	/getOperatorMobileNumberMappedUnit	0	public Long getOperatorMobileNumberMappedUnit(@QueryParam("mobile") String mobile, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\PatientRegApptServiceImpl.java	/patientRegAppt	GET	/getPatientAgeForPackageConsumption	0	public Object[] getPatientAgeForPackageConsumption(@QueryParam("visitAdmId") Long visitAdmId, @QueryParam("visitAdmUnitId") Long visitAdmUnitId) throws GenericApplicationException 
custom\PatientRegApptServiceImpl.java	/patientRegAppt	GET	/getPatientTransactionDetailsList	1	public PatientTransactionDetailsListView getPatientTransactionDetailsList(@QueryParam("PatientID") @StoredProcNamedParam("p_patientid") Long patientId,
custom\PatientRegApptServiceImpl.java	/patientRegAppt	GET	/getreferentitytype	1	public List<Refentitymaster> getReferalEntityType(@QueryParam("refTypeId") Long refTypeId) 
custom\PatientRegApptServiceImpl.java	/patientRegAppt	GET	/pdfAppointmentFaceSheet	0	public String[] pdfAppointmentFaceSheet(@QueryParam("mRNO") String mRNO) throws GenericApplicationException 
custom\PatientRegApptServiceImpl.java	/patientRegAppt	GET	/pdfAppointmentFaceSheetFromAppList	0	public String[] pdfAppointmentFaceSheetFromAppList(@QueryParam("mRNO") String mRNO,
custom\PatientRegApptServiceImpl.java	/patientRegAppt	GET	/pdfAppointmentFaceSheetHtml	0	public Map<String, Object> pdfAppointmentFaceSheetHtml(@QueryParam("mRNO") String mRNO,
custom\PatientRegApptServiceImpl.java	/patientRegAppt	GET	/pdfOpdCard	0	public String[] pdfOpdCard(@QueryParam("mRNO") String mRNO) throws GenericApplicationException 
custom\PatientRegApptServiceImpl.java	/patientRegAppt	GET	/printTokenSheet	0	public String[] printTokenSheet(@QueryParam("id") Long id, @QueryParam("tokenNo") String tokenNo, @HeaderParam("token") String token) throws GenericApplicationException 
custom\PatientRegApptServiceImpl.java	/patientRegAppt	GET	/searchPatientBasicDetailsForOnline	0	public OnlineAppointmentDto searchPatientBasicDetailsForOnlineAPI(@QueryParam("mobile") String mobile, @QueryParam("searchType") String searchType, @QueryParam("searchValue") Strin
custom\PatientRegApptServiceImpl.java	/patientRegAppt	GET	/searchPatientDetailsByMobileNo	0	public OnlineAppointmentDto searchPatientDetailsByMobileNo(@QueryParam("mobile") Long mobile) throws GenericApplicationException
custom\PatientRegApptServiceImpl.java	/patientRegAppt	GET	/searchPatientDetailsForOnline	0	public OnlineAppointmentDto searchPatientDetailsForOnline(@QueryParam("mobile") String mobile, @QueryParam("searchType") String searchType,
custom\PatientRegApptServiceImpl.java	/patientRegAppt	GET	/searchPatientPhotoGraph	0	public String searchPatientPhotoGraph(@QueryParam("familyId") String familyId, @QueryParam("memberId") String memberId) throws GenericApplicationException 
custom\PatientRegApptServiceImpl.java	/patientRegAppt	GET	/searchPatientPhotoGraphJanaadhar	0	public String searchPatientPhotoGraphJanaadhar(@QueryParam("janaadharId") String janaadharId, @QueryParam("janaadharMemberId") String janaadharMemberId) throws GenericApplicationEx
custom\PatientRegApptServiceImpl.java	/patientRegAppt	GET	/sendOTP	0	public OnlineAppointmentDto sendOTP(@QueryParam("mobile") String mobile, @QueryParam("provValue") String provValue, @QueryParam("type") @DefaultValue("mobile") String type, @QueryP
custom\PatientRegApptServiceImpl.java	/patientRegAppt	GET	/sendOTPForEnableLoginWithoutSSO	0	public OnlineAppointmentDto sendOTPForEnableLoginWithoutSSO(@QueryParam("otpSendType") @DefaultValue("isSSOLoginOTP") String otpSendType, @QueryParam("SSOID") String SSOID) throws 
custom\PatientRegApptServiceImpl.java	/patientRegAppt	GET	/validateMobileNumber	0	public boolean validateMobileNumber(@QueryParam("mobileNumber") String mobileNumber, @HeaderParam("token") String token) throws GenericApplicationException 
custom\PatientRegApptServiceImpl.java	/patientRegAppt	POST	/createAppointment	0	public PatientAppointmentandRegDto addAppointment(@BodyParam("body") PatientAppointmentandRegDto body) throws GenericApplicationException 
custom\PatientRegApptServiceImpl.java	/patientRegAppt	POST	/getHealthCardSheet	0	public String getHealthCardSheet(@BodyParam("body") HealthCardDto body) throws Exception 
custom\PatientRegApptServiceImpl.java	/patientRegAppt	POST	/pdfAppointmentLetterFromAppList	0	public String[] pdfAppointmentLetterFromAppList(@BodyParam("body") AppointmentMrNoListResultDto body, @QueryParam("visitId") Long visitId) throws GenericApplicationException 
custom\PatientRegApptServiceImpl.java	/patientRegAppt	POST	/validateOTP	0	public OnlineAppointmentDto validateOTP(@QueryParam("mobile") String mobile, @QueryParam("otp") String otp, @QueryParam("provValue") String provValue, @QueryParam("type") @DefaultV
custom\PatientRegApptServiceImpl.java	/patientRegAppt	POST	/validateOTPForLoginWithoutSSO	0	public OnlineAppointmentDto validateOTPForLoginWithoutSSO(@QueryParam("type") @DefaultValue("isSSOLoginOTP") String type, @QueryParam("otp") String otp, @QueryParam("SSOID") String
custom\PatientTransferDischargeServiceImpl.java	/patientTransferDischarge	GET	/fetchBedsByWardId	1	public List<Bedmaster> getBedsList(@QueryParam("wardId") Long wardId) throws GenericApplicationException 
custom\PatientTransferDischargeServiceImpl.java	/patientTransferDischarge	GET	/getLeaveDaysDetails	1	public List<TransferDetailsDto> getLeaveDaysDetails(@QueryParam("admId") Long admId, @QueryParam("patientId") Long patientId) throws GenericApplicationException 
custom\PatientTransferDischargeServiceImpl.java	/patientTransferDischarge	GET	/markNotReportedAsDischarged	1	public Boolean markNotReportedAsDischarged(@QueryParam("admmId") Long admId, @QueryParam("doctorId") Long doctorId,@QueryParam("unitId")Long unitId, @QueryParam("dischargeTypeId") 
custom\PatientTransferDischargeServiceImpl.java	/patientTransferDischarge	GET	/showExtPackageDetails	1	public List<ExtPackageSubscription> showExtPackageDetails(@QueryParam("opdIpdId") Long opdIpdId, @QueryParam("opdIpd") Long opdIpd) throws GenericApplicationException 
custom\PatientTransferDischargeServiceImpl.java	/patientTransferDischarge	GET	/validatePatientDischarge	1	public boolean validatePatientDischarge(@QueryParam("admId") Long admId) throws GenericApplicationException 
custom\PatientTransferDischargeServiceImpl.java	/patientTransferDischarge	POST	/acceptPatientOnLeaveRequest	1	public boolean acceptPatientOnLeaveRequest(@BodyParam("body") BedTransferDto body) throws GenericApplicationException 
custom\PatientTransferDischargeServiceImpl.java	/patientTransferDischarge	POST	/addAbscondDischarge	1	public PatientTransferDischargeView addAbscondDischarge(@BodyParam("body") PatientTransferDischargeView body) throws GenericApplicationException 
custom\PatientTransferDischargeServiceImpl.java	/patientTransferDischarge	POST	/addAdvisedDischargeList	0	public AdvisedDischargeListDto addAdvisedDischargeList(@BodyParam("body") AdvisedDischargeListDto body) throws GenericApplicationException 
custom\PatientTransferDischargeServiceImpl.java	/patientTransferDischarge	POST	/allocateBed	0	public boolean allocateBed(@BodyParam("body") BedTransferDto body) throws GenericApplicationException 
custom\PatientTransferDischargeServiceImpl.java	/patientTransferDischarge	POST	/cancelAdvisedDischarge	0	public void cancelAdvisedDischarge(@BodyParam("body") AdvisedDischargeListDto body) throws GenericApplicationException 
custom\PatientTransferDischargeServiceImpl.java	/patientTransferDischarge	POST	/cancelDischarge	1	public Long cancelDischarge(@QueryParam("admissionId") Long admissionId, @QueryParam("unitId") Long unitId, @QueryParam("patientId") Long patientId,@QueryParam("disCancelReason")St
custom\PatientTransferDischargeServiceImpl.java	/patientTransferDischarge	POST	/cancelInterimTransfer	0	public boolean cancelInterimTransfer(@QueryParam("admId") Long admId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\PatientTransferDischargeServiceImpl.java	/patientTransferDischarge	POST	/cancelPatientOnLeaveRequest	1	public boolean cancelPatientOnLeaveRequest(@QueryParam("admId") Long admId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\PatientTransferDischargeServiceImpl.java	/patientTransferDischarge	POST	/changeSponsorDetails	1	public ChangePatientSponsorDto changeSponsorDetails(ChangePatientSponsorDto body) throws GenericApplicationException 
custom\PatientTransferDischargeServiceImpl.java	/patientTransferDischarge	POST	/discharge	0	public PatientTransferDischargeView dischargePatient(@BodyParam("body") PatientTransferDischargeView body) throws GenericApplicationException 
custom\PatientTransferDischargeServiceImpl.java	/patientTransferDischarge	POST	/generateDischargeReqNO	1	public String generateDischargeReqNO(@QueryParam("opdIpdId") Long opdIpdId, @QueryParam("opdIpd") Long opdIpd) throws GenericApplicationException 
custom\PatientTransferDischargeServiceImpl.java	/patientTransferDischarge	POST	/getDischargedPatientList	0	public List<InquiryDetailsResultDto> getDischargedPatientList(@BodyParam("body") InquiryDetailsDto body) throws GenericApplicationException 
custom\PatientTransferDischargeServiceImpl.java	/patientTransferDischarge	POST	/getInterimAdmissionsList	1	public List<PatientAdmissionListResultDto> getInterimAdmissionList(@BodyParam("body") InquiryDetailsDto body, @QueryParam("search") String search, @QueryParam("page") Integer page,
custom\PatientTransferDischargeServiceImpl.java	/patientTransferDischarge	POST	/getMortuaryDischargedPatientList	1	public List<MortuaryPatientDetialsDto> getMortuaryDischargedPatientList(@BodyParam("body") MortuaryPatientDetialsDto body,@QueryParam("page") Integer page,
custom\PatientTransferDischargeServiceImpl.java	/patientTransferDischarge	POST	/markAsNotReported	1	public Boolean addMarkAsNotReported(@QueryParam("admId") Long admId, @QueryParam("unitId")Long unitId) throws GenericApplicationException 
custom\PatientTransferDischargeServiceImpl.java	/patientTransferDischarge	POST	/markPatientOnLeave	1	public PatientTransferDischargeView markPatientOnLeave(@BodyParam("body") PatientTransferDischargeView body) throws GenericApplicationException 
custom\PatientTransferDischargeServiceImpl.java	/patientTransferDischarge	POST	/printPatientDeathForm	1	public String printPatientDeathForm(@BodyParam("body") AddPatientDeathDetails body, @QueryParam("isDischarged") @DefaultValue("false") Boolean isDischarged) throws GenericApplicati
custom\PatientTransferDischargeServiceImpl.java	/patientTransferDischarge	POST	/transfer	0	public PatientTransferDischargeView transferPatient(@BodyParam("body") PatientTransferDischargeView body) throws GenericApplicationException 
custom\PatientTransferDischargeServiceImpl.java	/patientTransferDischarge	POST	/transferBaby	0	public PatientTransferDischargeView transferBaby(@BodyParam("body") PatientTransferDischargeView body) throws GenericApplicationException 
custom\PatientTransferDischargeServiceImpl.java	/patientTransferDischarge	POST	/transferToPreOtPatient	1	public PatientTransferDischargeView transferToPreOtPatient(@BodyParam("body") PatientTransferDischargeView body) throws GenericApplicationException 
custom\PatientTransferDischargeServiceImpl.java	/patientTransferDischarge	POST	/updateDischargeDetails	0	public Boolean updateDischargeDetails(@BodyParam("body") BedDischargeDto body) throws GenericApplicationException 
custom\PatientTransferDischargeServiceImpl.java	/patientTransferDischarge	POST	/updateExaminedByDoctor	1	public boolean updateExaminedByDoctor(@QueryParam("ipdId") Long ipdId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\PatientTransferDischargeServiceImpl.java	/patientTransferDischarge	POST	/updatePatientDischargeStatus	0	public boolean updatePatientDischargeStatus(@BodyParam("body") BedDischargeDto body) throws GenericApplicationException 
custom\PatientTransferDischargeServiceImpl.java	/patientTransferDischarge	POST	/updatePatientInterimAdmission	1	public boolean updatePatientInterimAdmission(@QueryParam("mrno") String mrno, @QueryParam("ipdId") Long ipdId, @QueryParam("unitId") Long unitId) throws GenericApplicationException
custom\PatientTransferDischargeServiceImpl.java	/patientTransferDischarge	PUT	/releaseBed	0	public BedOccupiedFlagDto updateBedOccupiedFlag(@BodyParam("body") BedOccupiedFlagDto body) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	DELETE	/removePrescription	1	public void removePrescription(@QueryParam("ptId") Long ptId) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	GET	/checkDispenseStatus	1	public CheckOrderDispenseAndStock checkDispenseStatus(@QueryParam("orderNo") String orderNo, @QueryParam("storeId") Long storeId, @QueryParam("itemCode") String itemCode) throws Ge
custom\PharmacyServiceImpl.java	/pharmacy	GET	/deleteInterventionById	0	public void deleteInterventionById(@QueryParam("orderId") String orderId, @QueryParam("Id") Long Id) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getAllAdmittedPatients	0	public List<AdmittedPatientsPharmacyResultDto> getAllAdmittedPatients(@QueryParam("storeMapped") Long storeMapped) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getAllUersUnderRole	1	public List<UserForRoleResultDto> getAllUersUnderRole(@QueryParam("roleId") Long roleId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getCartFillQnt	0	public String getCartFillQnt(@QueryParam("orderNo") String orderNo) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getDetailedMedHistory	0	public GetPrescriptionWardDetails getDetailedMedHistory(@QueryParam("orderNo") String orderNo) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getDispensedIVOrders	1	public List<PharmacyGetDispensedIvOrdersDto> getDispensedIVOrders(@QueryParam("mrno") String mrno, @QueryParam("itemCode") String itemCode) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getDispensedOrders	0	public List<GetDispsenseOrders> getDispensedOrders(@QueryParam("opd_Ipd") Long opd_Ipd, @QueryParam("fromDate") java.sql.Timestamp fromDate, @QueryParam("toDate") java.sql.Timestam
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getDoctorDetails	1	public List<Object[]> getDoctorDetails() throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getDrugSubstituteByDrugId	1	public List<Object[]> getDrugSubstituteByDrugId(@QueryParam("itemId") Long itemId) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getIntervention	1	public List<GetPharmacyInterventionList> getIntervention(@QueryParam("orderId") String orderId) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getInterventionOrderById	1	public GetPharmacyInterventionList getInterventionOrderById(@QueryParam("orderId") String orderId, @QueryParam("Id") Long Id) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getIsHighRiskFlag	0	public Object[] getIsHighRiskFlag(@QueryParam("itemcode") String itemcode) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getItemCategoryItemCode	0	public List<Object[]> getItemsByCategoryID(@QueryParam("itemCode") String itemCode) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getItemForCompoundDetails	0	public List<ItemsForAddMaintenanceResultDto> getItemForCompoundDetails() 
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getItemSalesIP	0	public List<GetIPItemSalesDetailsDto> getItemSalesIP(@QueryParam("patientId") Long patientId, @QueryParam("wardId") Long wardId) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getMedications	0	public List<ItemSolrView> getMedications(@QueryParam("seed") String search) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getMedsAndItemStockList	1	public List<PatientPrescriptionDetailsDto> getMedsAndItemStockList(@QueryParam("patientId") Long patientId, @QueryParam("opd_Ipd_Id") Long opd_Ipd_Id,
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getMedsAndItemStockListForNAItems	0	public List<PatientPrescriptionNAItemDetailsDto> getMedsAndItemStockListForNAItems(@QueryParam("patientId") Long patientId, @QueryParam("opd_Ipd_Id") Long opd_Ipd_Id,
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getMedsAndItemStockListOld	1	public List<PatientPrescriptionDetailsDto> getMedsAndItemStockListOld(@QueryParam("patientId") Long patientId, @QueryParam("opd_Ipd_Id") Long opd_Ipd_Id, @QueryParam("unitId") Long
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getNonMedItemListForNurse	0	public List<NonMedItemListForNurse> getNonMedItemListForNurse()
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getOpdispensedOrderDetails	1	public List<OpdispensedOrderDetails> getOpdispensedOrderDetails(@QueryParam("ItemsaleDetid") Long itemSaleDetId) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getOutMedication	0	public List<PatientPrescriptionDetailsDto> getOutMedication(@QueryParam("limit") int limit,@QueryParam("maxRecords") int maxRecords, @QueryParam("patientId") Long patientId) throws
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getOutSideDocRegSearch	1	public List<Object[]> getOutSideDocReg(@QueryParam("regNo") String regNo) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getOutSideVisit	1	public List<GetOutSideVisit> getOutSideVisit(@QueryParam("patientId") Long patientId, @QueryParam("doctorId") Long doctorId) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getPatienAndVisitDetails	1	public List<Object[]> getPatienAndVisitDetails(@QueryParam("patientIen") String patientIen) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getPatientDetailsForPharmacy	1	public PatientPharmacyDetailsDto getPatientDetailsForPharmacy(@QueryParam("patientId") Long patientId, @QueryParam("opd_ipd_id") Long opd_ipd_id, @QueryParam("opd_ipd") Long opd_ip
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getPatientDetailsForPharmacyNewScreen	0	public PatientPharmacyDetailsDto getPatientDetailsForPharmacyNewScreen(@QueryParam("patientId") Long patientId, @QueryParam("opd_ipd_id") Long opd_ipd_id, @QueryParam("opd_ipd") Lo
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getPatientDispensedOrders	1	public List<GetPatientDispensedOrders> getPatientDispensedOrders(@QueryParam("patientId") Long patientId) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getPatientNonMedOrders	1	public List<PatientNonMedOrdersResultDto> getPatientNonMedOrders(@QueryParam("patientId") Long patientId, @QueryParam("fromDate") java.sql.Timestamp fromDate, @QueryParam("toDate")
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getPatPharmPrescriptionDetails	0	public List<GetPrescriptionPharmOrdersDto> getPrescriptionPharmOrderDetails(@QueryParam("opd_Ipd") Long opd_Ipd, @QueryParam("opd_Ipd_Id") Long opd_Ipd_Id) throws GenericApplicatio
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getPharmacyBillingAmount	1	public Number getPharmacyBillingAmount(@QueryParam("visitId") Long visitId) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getPharmacyInterventionReport	0	public List<PharmacyInterventionReportDto> getPharmacyInterventionReport(@QueryParam("opd_Ipd") Long opd_Ipd) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getPrescId	0	public void getPrescId(@QueryParam("orderId") String orderId) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getPrescriptionDetailsToDispence	0	public List<PatientPrescriptionDetailsDto> getPrescriptionDetailsToDispence(@QueryParam("opd_Ipd") Long opd_Ipd, @QueryParam("opd_Ipd_Id") Long opd_Ipd_Id) throws GenericApplicatio
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getPreviuosFlagComments	1	public String getPreviuosFlagComments(@QueryParam("prescDetailId") Long prescDetailId) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getSetPharmacyUserSession	0	public String getSetPharmacyUserSession(@QueryParam("patientId") Long patientId, @QueryParam("userId") Long userId, @QueryParam("opd_Ipd") Long opd_Ipd, @QueryParam("visitAdmId") L
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getUploadedPrescriptionDocuments	1	public List<PatientIDDocumentDto> getUploadedPrescriptionDocuments(@QueryParam("patientId") Long PatientId, @QueryParam("opdIpdId") Long opdIpdId, @QueryParam("DocType") String Doc
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getWardByWardGroupId	0	public List<Wardmaster> getWardByWardGroupId(@QueryParam("wardGrpId") Long wardGrpId) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	GET	/getWardPickList	1	public List<GetWardPickListResultDto> getWardPickList(@QueryParam("fromDate") java.sql.Timestamp fromDate, @QueryParam("toDate") java.sql.Timestamp toDate, @QueryParam("wardname") 
custom\PharmacyServiceImpl.java	/pharmacy	GET	/inactivePrescriptionById	0	public Boolean inactivePrescriptionById(@QueryParam("prescDetaildId") Long prescDetaildId) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	GET	/itemcategoryAndStoreMeds	0	public ItemCategoryStoreView getItemCategoryAndStoreMeds(@QueryParam("storeId") Long storeId) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	GET	/resetPharmacyUserSession	1	public void resetPharmacyUserSession(@QueryParam("userId") Long userId) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	GET	/resoleveIntervention	1	public void updateRejectFlagForIntervention(@QueryParam("orderId") String orderId) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	GET	/updateAcceptedByNurse	0	public void updateAcceptedByNurse(@QueryParam("prescriptionId") Long prescriptionId) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	GET	/updateDiscOrderStatus	0	public void updateDiscOrderStatus(@QueryParam("orderIen") String orderIen, @QueryParam("prescDetailId") Long prescdetailId) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	GET	/updateDispensedData	0	public void updateDispensedData(@QueryParam("prescDetailId") Long prescDetailId, @QueryParam("stockid") Long stockid, @QueryParam("quantity") java.math.BigDecimal quantity,
custom\PharmacyServiceImpl.java	/pharmacy	GET	/updateMedOrderDetails	1	public void updateMedOrderDetails(@QueryParam("orderNo") String orderNo, @QueryParam("quantity") String quantity, @QueryParam("dosage") String dosage) throws GenericApplicationExce
custom\PharmacyServiceImpl.java	/pharmacy	POST	/addDrugSubstitute	0	public void addDrugSubstitute(@BodyParam("body") List<AddDrugSubstituteDto> body) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/addInfusionMixture	0	public InfusoinMixtureView addInfusionMixture(@BodyParam("body") InfusoinMixtureView body) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/addIntervention	1	public AddPharmacyInterventionDto addIntervention(@BodyParam("body") AddPharmacyInterventionDto body) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/addItemMaster	0	public ItemCompoundDetailsView addItemCompoundDetails(@BodyParam("body") ItemCompoundDetailsView body) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/addManufacturedItem	1	public List<AddPharmacyManufacturedItemDto> addManufacturedItem(@BodyParam("addPharmacyManufacturedItemDto") AddPharmacyManufacturedItemDto addPharmacyManufacturedItemDto)
custom\PharmacyServiceImpl.java	/pharmacy	POST	/addPatientNonMedDetails	1	public void addPatientNonMedDetails(@BodyParam("body") List<PatientNonMedOrderDetails> body) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/addPatientPrescOrder	1	public List<String> addPatientPrescOrder(@BodyParam("body") AddPatientPrescOrderView body) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/addPatientProblems	0	public void addPatientProblems(@BodyParam("body") AddPatientProblemsDto body) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/addPrescriptionOrderDetails	1	public void addPrescriptionOrderDetails(@BodyParam("body") List<AddPrescriptionDispenseDetailsDto> body) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/addWardPickList	1	public List<AddWardPickListDto> addWardPickList(@BodyParam("body") List<AddWardPickListDto> body) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/cancelItemSalePrice	1	public void cancelItemSalePrice(@QueryParam("itemSaleId") Long itemSaleId) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/cancelItemSalePriceNew	0	public void cancelItemSalePriceNew(@QueryParam("orderNumber") String orderNumber) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/cartfillItemSale	0	public void cartfillItemSale(@BodyParam("body") AddItemSalesView body) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/createNewPresc	0	public void createNewPresc(@BodyParam("body") CreateNewPrescVisit body) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/getAllInterventionList	0	public List<GetPharmacyInterventionList> getAllInterventionList(@QueryParam("admId") Long admId) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/getDispenseOrderItems	1	public List<GetOrderDispenseItemDetails> getDispenseOrderItems(@QueryParam("UnitID") Long UnitID, @QueryParam("ItemId") Long ItemId, @QueryParam("PrescdetId") Long prescdetId,
custom\PharmacyServiceImpl.java	/pharmacy	POST	/getDispensingDetails	1	public List<GetDispensingDetails> getDispensingDetails(@QueryParam("visitid") Long visitid) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/getItemCurrentStock	0	public List<GetPharmItemCurrentStock> getItemCurrentStock(@QueryParam("storeId") Long storeId, @QueryParam("itemId") Long itemId, @QueryParam("opd_Ipd_Id") Long opd_Ipd_Id,
custom\PharmacyServiceImpl.java	/pharmacy	POST	/getItemForCartfillDispense	0	public List<GetPharmItemCurrentStock> getItemForCartfillDispense(@QueryParam("itemCode") String itemCode, @QueryParam("storeId") Long storeId) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/getIVOrdersList	1	public List<GetIVOrdersDetailsDispeneseDto> getIVOrdersList(@QueryParam("unitid") Long unitid) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/getPharmacyCompoundDetailsByPatientId	0	public List<GetCompoundOrderDetails> getPharmacyCompoundDetailsByPatientId(@QueryParam("opd_Ipd") Long opd_Ipd, @QueryParam("patientId") Long patientId,@QueryParam("isDisepnsed") L
custom\PharmacyServiceImpl.java	/pharmacy	POST	/getPharmacyItemCompoundDetails	1	public List<GetItemCompoundDetailsResultDto> getPharmacyItemCompoundDetails(@QueryParam("itemId") Long itemId) 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/getPharmacyWardDetails	0	public GetExtInternalPrescriptionView getPharmacyWardDetails(@QueryParam("opd_Ipd") Long opd_Ipd, @QueryParam("count") Long pageCount) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/getPharmacyWardDetailsbyPatientid	1	public List<GetPrescriptionWardDetails> getPharmacyWardDetailsbyPatientid(@QueryParam("opd_Ipd") Long opd_Ipd, @QueryParam("patient_Id") Long patient_Id) throws GenericApplicationE
custom\PharmacyServiceImpl.java	/pharmacy	POST	/getPharmacyWardDetailsDispense	0	public List<GetPrescriptionWardDetailsDispense> getPharmacyWardDetailsDispense(@QueryParam("isdispensed") boolean isdispensed, @QueryParam("isPickList") boolean isPickList,
custom\PharmacyServiceImpl.java	/pharmacy	POST	/getPharmacyWardDetailsDispenseByPatientId	1	public List<GetPrescriptionWardDetailsDispense> getPharmacyWardDetailsDispenseByPatientId(@QueryParam("isdispensed") boolean isdispensed, @QueryParam("isPickList") boolean isPickLi
custom\PharmacyServiceImpl.java	/pharmacy	POST	/getPharmacyWardDetailsPartial	0	public List<GetPrescriptionWardDetailsDispense> getPharmacyWardDetailsPartial(@QueryParam("fromDate") java.sql.Timestamp fromDate, @QueryParam("toDate") java.sql.Timestamp toDate)
custom\PharmacyServiceImpl.java	/pharmacy	POST	/getPharmacyWardDetailsVerified	1	public List<GetCompoundOrderDetails> getPharmacyWardDetailsVerified(@QueryParam("opd_Ipd") Long opd_Ipd, @QueryParam("fromDate") java.sql.Timestamp fromDate,
custom\PharmacyServiceImpl.java	/pharmacy	POST	/opPharmacyPatientRegistrationandUpadation	0	public PatientAppointmentandRegDto registerPatient(@BodyParam("body") PatientAppointmentandRegDto body) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/printBagLabel	0	public InfusoinMixtureView printBagLabel(@BodyParam("body") InfusoinMixtureView body) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/printCompoundLabel	0	public BillReceiptView printSingleCompoundLabel(@QueryParam("is_print") boolean print, @QueryParam("pateintName") String pateintName, @QueryParam("brand") String brand,
custom\PharmacyServiceImpl.java	/pharmacy	POST	/printHighBillingPatients	0	public Map<String, Object> printHighBillingPatients(@BodyParam("body") Map<String, Object> body)
custom\PharmacyServiceImpl.java	/pharmacy	POST	/printOutOfStockMedDetails	0	public String printOutOfStockMedDetails(@BodyParam("body") PatientDispenseDetails body) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/printPatientDispenseDetails	0	public String printPatientDispenseDetails(@BodyParam("body") PatientDispenseDetails body) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/printPatientMedNonMedDetails	1	public String printPatientMedNonMedDetails(@BodyParam("body") PrintPatientRxDetails body) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/printPatientRxDetails	0	public String printPatientRxDetails(@BodyParam("body") PrintPatientRxDetails body) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/printPharmacyMedLabel	0	public PharmacyLableView printPharmacyMedLabel(@BodyParam("body") PharmacyLableView body) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/setFlagForOrder	0	public void setFlagForOrder(@QueryParam("prescDetailId") Long prescDetailId, @QueryParam("flagged") boolean flagged, @QueryParam("flagReason") String flagReason)
custom\PharmacyServiceImpl.java	/pharmacy	POST	/setNurseReturnAck	0	public void setNurseReturnAck(@QueryParam("iteSaleDetailId") Long iteSaleDetailId, @QueryParam("wasteQuantity") java.math.BigDecimal wasteQuantity, @QueryParam("wasteReason") Strin
custom\PharmacyServiceImpl.java	/pharmacy	POST	/setPatientIen	0	public void setPatientIen(@QueryParam("mrno") String mrno, @QueryParam("patientIen") Long patientIen) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/setVerifiedFlag	0	public void setVerifiedFlag(@QueryParam("prescDetailId") Long prescDetailId, @QueryParam("preExDose") java.math.BigDecimal preExDose, @QueryParam("orderNo") String orderNo,
custom\PharmacyServiceImpl.java	/pharmacy	POST	/syncJantaMedDispenseOrders	0	public List<Long> syncJantaMedDispenseOrders(@BodyParam("body") List<AddPrescriptionDispenseDetailsDto> body) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/updateCompoundStatus	1	public void updateCompoundStatus(@QueryParam("prescDetID") Long prescDetID, @QueryParam("compoundOrderDetailsId") Long compoundOrderDetailsId, @QueryParam("noOfPackages") Long noOf
custom\PharmacyServiceImpl.java	/pharmacy	POST	/updateDispenseFlagStockforOP	1	public void updateDispenseFlagStockforOP(@QueryParam("prescDetailId") Long prescDetailId) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/updateInterventionById	1	public void updateInterventionById(@BodyParam("body") GetPharmacyInterventionList body) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/updateItemSaleforDispense	1	public GenericResultDto updateItemSaleforDispense(@BodyParam("body") UpdateItemSaleBfrDispense body) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/updateItemStock	1	public ItemCompoundDetailsView updateItemStock(@BodyParam("body") ItemCompoundDetailsView body) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/updateRejectFlagForIntervention	0	public void updateRejectFlagForIntervention(@QueryParam("prescDetailId") Long prescDetailId) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/updateVerifyFlagOP	1	public void updateVerifyFlagOP(@QueryParam("prescDetailId") Long prescDetailId) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/updateVerifyFlagOPAutoDispense	1	public void updateVerifyFlagOPAutoDispense(@QueryParam("prescDetailId") Long prescDetailId) throws GenericApplicationException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	/uploadPrescription	1	public PatientIDDocumentDto uploadPrescription(@BodyParam("body") PatientIDDocumentDto idDocumentDto,
custom\PharmacyServiceImpl.java	/pharmacy	POST	commonAppAndMedicineDispensing	0	public void commonAppAndMedicineDispensing(@BodyParam("body") CommonAppAndMedicineDis body, @HeaderParam("token") String token) throws GenericApplicationException, ParseException 
custom\PharmacyServiceImpl.java	/pharmacy	POST	getSingleItemBatchListForPharmacy	0	public List<ItemBatchListForSearchwithTaxResultDto> getSingleItemBatchListForPharmacy(@BodyParam("body") ItemBatchListForSearchDto body) throws GenericApplicationException, ParseEx
custom\PrescriptionDetailsServiceImpl.java	/prescription	GET	/getPrescriptionsByFamliyNoAndMemberId	0	public List<PatientDetailsForPrescriptionDto> getPrescriptionsByFamliyNoAndMemberId(@QueryParam("familyIdNo") String familyIdNo, @QueryParam("memberId") String memberId) throws Gen
custom\QuetionnaireServiceImpl.java	/questionnaireService	GET	/getAllQuestionAndAnswers	0	public List<QuestionAnswerDto> getAllQuestionAndAnswers(@QueryParam("patientId") Long patientId, @QueryParam("doctorId") Long doctorId, @QueryParam("module") String module,
custom\QuetionnaireServiceImpl.java	/questionnaireService	GET	/getAllQuestionAndAnswersForPatient	1	public QuestionAnswerView getAllQuestions() throws GenericApplicationException 
custom\QuetionnaireServiceImpl.java	/questionnaireService	POST	/addUserAnswers	0	public void addUserAnswers(@BodyParam("questionAnswerDtos") List<QuestionAnswerDto> questionAnswerDtos, @QueryParam("opd_ipd_id") Long opd_ipd_id, @QueryParam("opd_ipd") Long opd_i
custom\RefundBillReceiptServiceImpl.java	/refundBillReceipt	POST	/getRefundReceipt	0	public List<CompanyAdvanceRefundReceiptResultDto> getAdvance(@BodyParam("body") CompanyAdvanceRefundReceiptDto body) throws GenericApplicationException 
custom\UploadFileServiceImpl.java	/uploadFile	POST	/{mastername}	0	public List<String> upload(@HeaderParam("token") String token, @QueryParam("isHeaderRow") @DefaultValue("true") boolean isHdrRow, File file1, String fileName, String fileType, @Pat
custom\UploadFileServiceImpl.java	/uploadFile	POST	/servicemapping	1	public List<Servicemaster> serviceMasterMapping(@QueryParam("isHeaderRow") @DefaultValue("true") boolean isHdrRow, Attachment attachment) throws GenericApplicationException 
custom\UploadFileServiceImpl.java	/uploadFile	POST	/serviceMappingTo	1	public List<Servicemaster> serviceMasterMappingTo(@BodyParam("body") List<Servicemaster> body) 
custom\UploadFileServiceImpl.java	/uploadFile	POST	/serviceMappingUpload	1	public List<String> serviceMasterMappingUpload(@BodyParam("body") List<CustomerServiceMapping> body) throws GenericApplicationException 
custom\UploadHelperServiceImpl.java	/upload	POST	/{mastername}	0	public List<String> upload(@HeaderParam("token") String token, @QueryParam("isHeaderRow") @DefaultValue("true") boolean isHdrRow, Attachment attachment, @PathParam("mastername") St
CWFServiceImpl.java	/cwf	DELETE	/deleteCWFMapping	0	public void deleteCWFMapping(@QueryParam("id") Long id) throws GenericApplicationException 
CWFServiceImpl.java	/cwf	DELETE	/deleteSCWFMapping	0	public void deleteSCWFMapping(@QueryParam("id") Long id) throws GenericApplicationException 
CWFServiceImpl.java	/cwf	GET	/getCWFDetails/{id}	1	public CWFMapping getCWFDetails(@PathParam("id") Long id) throws GenericApplicationException 
CWFServiceImpl.java	/cwf	GET	/getCWFDetailsByDiagId/{id}	0	public SCWFMappingDTO getCWFDetailsByDiagId(@PathParam("id") Long id) throws GenericApplicationException 
CWFServiceImpl.java	/cwf	GET	/getCWFDetailsByDiagIdw/{id}	0	public SCWFMapping getCWFDetailsByDiagIdw(@PathParam("id") Long id) throws GenericApplicationException 
CWFServiceImpl.java	/cwf	GET	/getCWFDiagnosisByComplaints	0	public Map<Long, String> getCWFDetails(@QueryParam("complaintIds") String complaintIds) throws GenericApplicationException 
CWFServiceImpl.java	/cwf	GET	/getListOfCWFMapping	0	public List<CWFMapping> getListOfCWFMapping(@QueryParam("page") Integer page,
CWFServiceImpl.java	/cwf	GET	/getListOfSCWFMapping	1	public List<SCWFMapping> getListOfSCWFMapping(@QueryParam("page") Integer page, @QueryParam("count") Integer count,
CWFServiceImpl.java	/cwf	GET	/getSCWFDetails/{id}/{specialityId}	0	public List<SCWFMapping> getSCWFDetails(@PathParam("id") Long id, @PathParam("specialityId") Long specialityId) throws GenericApplicationException 
CWFServiceImpl.java	/cwf	GET	/getSCWFDetailsByHospitalType	1	public SCWFMappingDTO getSCWFDetailsByHospitalType(@QueryParam("hospital_type") Long hospitalType) throws GenericApplicationException 
CWFServiceImpl.java	/cwf	POST	/addCWFMapping	0	public CWFMapping addCWFMapping(@BodyParam("body") CWFMapping cwfMapping) throws GenericApplicationException 
CWFServiceImpl.java	/cwf	POST	/addSCWFMapping	0	public SCWFMapping addSCWFMapping(@BodyParam("body") SCWFMapping scwfMapping) throws GenericApplicationException 
CWFServiceImpl.java	/cwf	POST	/consolidatedLookup	1	public Map<Long, String> consolidatedLookup(CWFSearchDTO searchDTO) throws GenericApplicationException 
CWFServiceImpl.java	/cwf	POST	/getSCWFDetailsFull/{id}/{doctorId}	0	public SCWFMappingDTO getSCWFDetailsFull(@PathParam("id") Long id, @PathParam("doctorId") Long doctorId, CWFSearchDTO patEncDto) throws GenericApplicationException 
CWFServiceImpl.java	/cwf	POST	/saveTempPrescription	1	public void saveTempPrescription(@QueryParam("patDocQueueId") Long patDocQueueId, TempPrescriptionNote tempNote) throws GenericApplicationException 
CWFServiceImpl.java	/cwf	POST	/updateCWFMapping	1	public CWFMapping updateCWFMapping(@BodyParam("body") CWFMapping cwfMapping) throws GenericApplicationException 
CWFServiceImpl.java	/cwf	POST	/updateSCWFMapping	1	public SCWFMapping updateSCWFMapping(@BodyParam("body") SCWFMapping scwfMapping) throws GenericApplicationException 
ehr\EHRUtilServiceImpl.java	/ehr	GET	/fetchDepartments/{unitId}/{consultType}	0	public List<Departmentmaster> fetchDepartmentsByUnit(@PathParam("unitId") Long unitId, @PathParam("consultType") Integer consultType) throws GenericApplicationException 
ehr\EHRUtilServiceImpl.java	/ehr	GET	/findSSOUserDetails	0	public EhrSyncView findSSOUserDetails (@QueryParam("ssoId") String ssoId, @QueryParam("ehrCode") String ehrCode) throws Exception 
ehr\EHRUtilServiceImpl.java	/ehr	GET	/getAllPlatformUnitCount	1	public EhrSyncView getAllPlatformUnitCount (@QueryParam("ehrcode") String ehrcode) throws Exception 
ehr\EHRUtilServiceImpl.java	/ehr	GET	/getAllPlatformUnits	0	public EhrSyncView handleSelfRegPatientData (@QueryParam("ehrcode") String ehrcode, @QueryParam("page") int page,
ehr\EHRUtilServiceImpl.java	/ehr	GET	/handleSyncReq	0	public EhrSyncView handleSyncReq (@QueryParam("patientId") Long patientId, @QueryParam("ehrcode") String ehrcode) throws Exception 
ehr\EHRUtilServiceImpl.java	/ehr	POST	/createVisit	0	public PatientAppointmentandRegDto createVisit(@BodyParam("body") PatientAppointmentandRegDto body) throws GenericApplicationException, ParseException 
ehr\EHRUtilServiceImpl.java	/ehr	POST	/fetchAppointmentSchedule	0	public Map<java.util.Date, Map<String, List<AppointmentSlot>>> getAppointmentScheduleServiceNew(@QueryParam("type") @DefaultValue("D") String type,
ehr\EHRUtilServiceImpl.java	/ehr	POST	/fetchAvailableDepartments	0	public List<Departmentmaster> fetchAvailableDepartments(SearchDoctorDto searchDoctorDto) throws GenericApplicationException 
ejs\EjsAncPncChildServiceImpl.java	/ejs-ancpncchild	GET	/ancsahyogsearch	0	public EjsResponseDto<EjsSupervisorAncDto> getAncSearchSahyog(@QueryParam("filter") String filter, @QueryParam("filter_id") BigInteger filterId, @QueryParam("page_size") Integer pa
ejs\EjsAncPncChildServiceImpl.java	/ejs-ancpncchild	GET	/ancsearch	0	public EjsResponseDto<EjsAnc> ancSearch(@QueryParam("sub_center_id") BigInteger subCenterId, @QueryParam("page_size") Integer pageSize,
ejs\EjsAncPncChildServiceImpl.java	/ejs-ancpncchild	GET	/ancvisitssahyogsearch	0	public EjsResponseDto<EjsAncVisitsDto> getAncVisitsSearchSahyog(@QueryParam("filter") String filter, @QueryParam("filter_id") BigInteger filterId, @QueryParam("page_size") Integer 
ejs\EjsAncPncChildServiceImpl.java	/ejs-ancpncchild	GET	/ancvisitssearch	1	public EjsResponseDto<EjsAncVisits> ancVisitsSearch(@QueryParam("sub_center_id") BigInteger subCenterId, @QueryParam("page_size") Integer pageSize,
ejs\EjsAncPncChildServiceImpl.java	/ejs-ancpncchild	GET	/childcarecheckupsearch	1	public EjsResponseDto<EjsChildCareCheckup> childCareCheckupSearch(@QueryParam("sub_center_id") BigInteger subCenterId, @QueryParam("page_size") Integer pageSize,
ejs\EjsAncPncChildServiceImpl.java	/ejs-ancpncchild	GET	/childcarecountsahyogsearch	0	public EjsResponseDto<EjsSupervisorChildDto> getChildCareCountSahyog(@QueryParam("filter") String filter, @QueryParam("filter_id") BigInteger filterId, @QueryParam("page_size") Int
ejs\EjsAncPncChildServiceImpl.java	/ejs-ancpncchild	GET	/childcaresahyogsearch	0	public EjsResponseDto<EjsSupervisorChildDto> getChildCareSahyog(@QueryParam("filter") String filter, @QueryParam("filter_id") BigInteger filterId, @QueryParam("page_size") Integer 
ejs\EjsAncPncChildServiceImpl.java	/ejs-ancpncchild	GET	/childcaresearch	1	public EjsResponseDto<EjsChildCare> searchChildCare(@QueryParam("sub_center_id") BigInteger subCenterId, @QueryParam("page_size") Integer pageSize,
ejs\EjsAncPncChildServiceImpl.java	/ejs-ancpncchild	GET	/childcountimmunsahyogsearch	0	public EjsResponseDto<EjsSupervisorChildDto> getChildCareCountImmuneSahyog(@QueryParam("filter") String filter, @QueryParam("filter_id") BigInteger filterId, @QueryParam("page_size
ejs\EjsAncPncChildServiceImpl.java	/ejs-ancpncchild	GET	/ecssahyogsearch	0	public EjsResponseDto<EjsEcs> getEcsSearchSahyog(@QueryParam("filter") String filter, @QueryParam("filter_id") BigInteger filterId, @QueryParam("page_size") Integer pageSize,
ejs\EjsAncPncChildServiceImpl.java	/ejs-ancpncchild	GET	/pncsahyogsearch	0	public EjsResponseDto<EjsSupervisorPncDto> getPncSearchSahyog(@QueryParam("filter") String filter, @QueryParam("filter_id") BigInteger filterId, @QueryParam("page_size") Integer pa
ejs\EjsAncPncChildServiceImpl.java	/ejs-ancpncchild	GET	/pncsearch	1	public EjsResponseDto<EjsPnc> pncSearch(@QueryParam("sub_center_id") BigInteger subCenterId, @QueryParam("page_size") Integer pageSize,
ejs\EjsAncPncChildServiceImpl.java	/ejs-ancpncchild	GET	/pncvisitssahyogsearch	0	public EjsResponseDto<EjsPncVisitsDto> getPncVisitsSearchSahyog(@QueryParam("filter") String filter, @QueryParam("filter_id") BigInteger filterId, @QueryParam("page_size") Integer 
ejs\EjsAncPncChildServiceImpl.java	/ejs-ancpncchild	GET	/pncvisitssearch	1	public EjsResponseDto<EjsPncVisits> pncVisitsSearch(@QueryParam("sub_center_id") BigInteger subCenterId, @QueryParam("page_size") Integer pageSize,
ejs\EjsAncPncChildServiceImpl.java	/ejs-ancpncchild	POST	/ancdata	0	public List<ResponseEjs> ancData(@BodyParam("body") List<EjsAnc> body) throws GenericApplicationException 
ejs\EjsAncPncChildServiceImpl.java	/ejs-ancpncchild	POST	/ancvisitsdata	0	public List<ResponseEjs> ancVisitsData(@BodyParam("body") List<EjsAncVisits> body) throws GenericApplicationException 
ejs\EjsAncPncChildServiceImpl.java	/ejs-ancpncchild	POST	/childcarecheckupdata	0	public List<ResponseEjs> childCareCheckupData(@BodyParam("body") List<EjsChildCareCheckup> body) throws GenericApplicationException 
ejs\EjsAncPncChildServiceImpl.java	/ejs-ancpncchild	POST	/childcaredata	0	public List<ResponseEjs> childCareData(@BodyParam("body") List<EjsChildCare> body) throws GenericApplicationException 
ejs\EjsAncPncChildServiceImpl.java	/ejs-ancpncchild	POST	/pncdata	0	public List<ResponseEjs> pncData(@BodyParam("body") List<EjsPnc> body) throws GenericApplicationException 
ejs\EjsAncPncChildServiceImpl.java	/ejs-ancpncchild	POST	/pncvisitsdata	0	public List<ResponseEjs> pncVisitsData(@BodyParam("body") List<EjsPncVisits> body) throws GenericApplicationException 
ejs\EjsAshaServiceImpl.java	/ejs-asha	GET	/ashamotivationsearch	1	public EjsResponseDto<EjsAshaMotivation> ashaMotivationSearch(@QueryParam("sub_center_id") BigInteger subCenterId, @QueryParam("page_size") Integer pageSize,
ejs\EjsAshaServiceImpl.java	/ejs-asha	GET	/ashaworksearch	0	public EjsResponseDto<EjsAshaWork> ashaWorkSearch(@QueryParam("sub_center_id") BigInteger subCenterId, @QueryParam("page_size") Integer pageSize,
ejs\EjsAshaServiceImpl.java	/ejs-asha	GET	/attendancesearch	1	public EjsResponseDto<EjsAttendance> attendanceSearch(@QueryParam("sub_center_id") BigInteger subCenterId, @QueryParam("page_size") Integer pageSize,
ejs\EjsAshaServiceImpl.java	/ejs-asha	GET	/incentivessearch	1	public EjsResponseDto<EjsIncentives> incentivesSearch(@QueryParam("ashawork_id") BigInteger ashaWorkId, @QueryParam("page_size") Integer pageSize,
ejs\EjsAshaServiceImpl.java	/ejs-asha	GET	/userdesignationsearch	0	public EjsResponseDto<EjsUserDesignationDto> userDesignationSearch(@QueryParam("sub_center_id") BigInteger subCenterId, @QueryParam("page_size") Integer pageSize,
ejs\EjsAshaServiceImpl.java	/ejs-asha	POST	/ashamotivationdata	0	public List<BigInteger> ashaMotivationData(@BodyParam("body") List<EjsAshaMotivation> body) throws GenericApplicationException 
ejs\EjsAshaServiceImpl.java	/ejs-asha	POST	/ashaworkdata	0	public List<ResponseEjs> ashaWorkData(@BodyParam("body") List<EjsAshaWork> body) throws GenericApplicationException 
ejs\EjsAshaServiceImpl.java	/ejs-asha	POST	/attendancedata	0	public List<ResponseEjs> attendanceData(@BodyParam("body") List<EjsAttendance> body) throws GenericApplicationException 
ejs\EjsChecklistsServiceImpl.java	/ejs-checklists	GET	/checklistquesanswersgetfeedbacks	1	public EjsResponseDto<EjsChecklistQuesAnswersDto> checklistQuesAnswersGetFeedbacks(@QueryParam("village_id") BigInteger villageId, @QueryParam("page_size") Integer pageSize,
ejs\EjsChecklistsServiceImpl.java	/ejs-checklists	GET	/checklistquesanswerssearch	1	public EjsResponseDto<EjsChecklistQuesAnswers> checklistQuesAnswersSearch(@QueryParam("sub_center_id") BigInteger subCenterId, @QueryParam("page_size") Integer pageSize,
ejs\EjsChecklistsServiceImpl.java	/ejs-checklists	GET	/checklistquestionssearch	0	public EjsResponseDto<EjsChecklistQuestions> checklistQuestionsSearch(@QueryParam("visible_on") String visibleOn, @QueryParam("page_size") Integer pageSize,
ejs\EjsChecklistsServiceImpl.java	/ejs-checklists	GET	/checklistssearch	0	public EjsResponseDto<EjsChecklists> checklistsSearch(@QueryParam("visible_on") String visibleOn, @QueryParam("page_size") Integer pageSize,
ejs\EjsChecklistsServiceImpl.java	/ejs-checklists	GET	/checklistsubmissionssearch	0	public EjsResponseDto<EjsChecklistsSubmissions> checklistSubmissionsSearch(@QueryParam("sub_center_id") BigInteger subCenterId, @QueryParam("page_size") Integer pageSize,
ejs\EjsChecklistsServiceImpl.java	/ejs-checklists	GET	/submissionsinfo	0	public EjsChecklistsSubmissionsDto submissionsInfo(@QueryParam("state_id") BigInteger stateId, @QueryParam("zone_id") BigInteger zoneId,
ejs\EjsChecklistsServiceImpl.java	/ejs-checklists	POST	/checklistquesanswersdata	0	public List<ResponseEjs> checklistQuesAnswersata(@BodyParam("body") List<EjsChecklistQuesAnswers> body) throws GenericApplicationException 
ejs\EjsChecklistsServiceImpl.java	/ejs-checklists	POST	/checklistsubmissionsdata	0	public List<ResponseEjs> checklistSubmissionsData(@BodyParam("body") List<EjsChecklistsSubmissions> body) throws GenericApplicationException 
ejs\EjsDetailsServiceImpl.java	/ejs-details	DELETE	/deleteMediaById	1	public void deleteMediaById(@QueryParam("id") BigInteger id) throws GenericApplicationException
ejs\EjsDetailsServiceImpl.java	/ejs-details	GET	/abortiondetailssearch	1	public EjsResponseDto<EjsAbortionDetails> abortionDetailsSearch(@QueryParam("sub_center_id") BigInteger subCenterId, @QueryParam("page_size") Integer pageSize,
ejs\EjsDetailsServiceImpl.java	/ejs-details	GET	/cbacsearch	1	public EjsResponseDto<EjsCbac> cbacSearch(@QueryParam("sub_center_id") BigInteger subCenterId, @QueryParam("page_size") Integer pageSize,
ejs\EjsDetailsServiceImpl.java	/ejs-details	GET	/deathdetailsinfo	0	public EjsDeathDetailsInfoDto deathDetailsInfo(@QueryParam("state_id") BigInteger stateId, @QueryParam("zone_id") BigInteger zoneId,
ejs\EjsDetailsServiceImpl.java	/ejs-details	GET	/deathdetailssearch	1	public EjsResponseDto<EjsDeathDetails> deathDetailsSearch(@QueryParam("sub_center_id") BigInteger subCenterId, @QueryParam("page_size") Integer pageSize,
ejs\EjsDetailsServiceImpl.java	/ejs-details	GET	/diseasessearch	1	public EjsResponseDto<EjsDisease> diseasesSearch(@QueryParam("sub_center_id") BigInteger subCenterId, @QueryParam("page_size") Integer pageSize,
ejs\EjsDetailsServiceImpl.java	/ejs-details	GET	/downloadMedia	0	public void dowloadMedia(@QueryParam("file_name") String fileName, @Context HttpServletResponse response) throws GenericApplicationException
ejs\EjsDetailsServiceImpl.java	/ejs-details	GET	/ecssearch	1	public EjsResponseDto<EjsEcs> ecsSearch(@QueryParam("sub_center_id") BigInteger subCenterId, @QueryParam("page_size") Integer pageSize,
ejs\EjsDetailsServiceImpl.java	/ejs-details	GET	/familyawcdetailssearch	1	public EjsResponseDto<EjsFamilyAwcDetails> familyAwcDetailsSearch(@QueryParam("sub_center_id") BigInteger subCenterId, @QueryParam("page_size") Integer pageSize,
ejs\EjsDetailsServiceImpl.java	/ejs-details	GET	/familydetailssearch	1	public EjsResponseDto<EjsFamilyDetails> getFamilyDetails(@QueryParam("sub_center_id") BigInteger subCenterId, @QueryParam("page_size") Integer pageSize,
ejs\EjsDetailsServiceImpl.java	/ejs-details	GET	/familyplanningsearch	1	public EjsResponseDto<EjsFamilyPlanning> familyPlanningSearch(@QueryParam("sub_center_id") BigInteger subCenterId, @QueryParam("page_size") Integer pageSize,
ejs\EjsDetailsServiceImpl.java	/ejs-details	GET	/getAllEjsChecklistQuestions	1	public List<EjsChecklistQuestions> getAllEjsChecklistQuestions(@QueryParam("page") Integer page, @QueryParam("count") Integer count, @QueryParam("search") String search) 
ejs\EjsDetailsServiceImpl.java	/ejs-details	GET	/getAllEjsmasteractivity	0	public List<EjsMasterActivity> getAllEjsmasteractivity(@QueryParam("page") Integer page, @QueryParam("count") Integer count, @QueryParam("search") String search) 
ejs\EjsDetailsServiceImpl.java	/ejs-details	GET	/getAllEjsmasterentity	1	public List<EjsMasterEntity> getAllEjsmasterentity(@QueryParam("page") Integer page, @QueryParam("count") Integer count, @QueryParam("search") String search) 
ejs\EjsDetailsServiceImpl.java	/ejs-details	GET	/getEjsAWCDetails	0	public List<EjsMasterAwcs> getEjsAWCDetails(@QueryParam("subcenterid") BigInteger subcenterid, @QueryParam("villageid") BigInteger villageId,@QueryParam("id") BigInteger id) throws
ejs\EjsDetailsServiceImpl.java	/ejs-details	GET	/getEjsSubcenter	0	public List<EjsMasterSubcenters> getEjsSubcenter(@QueryParam("subcenter") String subcenter,@QueryParam("id") BigInteger id) throws GenericApplicationException 
ejs\EjsDetailsServiceImpl.java	/ejs-details	GET	/getEjsUserTypes	0	public List<EjsUserTypes> getEjsUserTypes(@QueryParam("subcenter") String subcenter,@QueryParam("id") BigInteger id) throws GenericApplicationException 
ejs\EjsDetailsServiceImpl.java	/ejs-details	GET	/getEjsVillage	0	public List<EjsMasterVillages> getEjsVillage(@QueryParam("subcenterid") BigInteger subcenterid, @QueryParam("id") BigInteger id) throws GenericApplicationException 
ejs\EjsDetailsServiceImpl.java	/ejs-details	GET	/helpsearch	1	public EjsResponseDto<EjsHelp> helpSearch(@QueryParam("sub_center_id") BigInteger subCenterId, @QueryParam("page_size") Integer pageSize,
ejs\EjsDetailsServiceImpl.java	/ejs-details	GET	/indicatorsinfo	1	public EjsIndicatorsInfoDto indicatorsInfo(@QueryParam("state_id") BigInteger stateId, @QueryParam("zone_id") BigInteger zoneId, @QueryParam("district_id") BigInteger districtId)
ejs\EjsDetailsServiceImpl.java	/ejs-details	GET	/mcdetailssearch	1	public EjsResponseDto<EjsMcDetails> mcdetailsSearch(@QueryParam("sub_center_id") BigInteger subCenterId, @QueryParam("page_size") Integer pageSize,
ejs\EjsDetailsServiceImpl.java	/ejs-details	GET	/mchnsearch	1	public EjsResponseDto<EjsMchn> mchnSearch(@QueryParam("sub_center_id") BigInteger subCenterId, @QueryParam("page_size") Integer pageSize,
ejs\EjsDetailsServiceImpl.java	/ejs-details	GET	/mediasearch	1	public List<MediaDto> mediaSearch(@QueryParam("category_id") String categoryId) throws GenericApplicationException 
ejs\EjsDetailsServiceImpl.java	/ejs-details	GET	/mediasearchejan	0	public List<Media> getAllMedia(@QueryParam("page") Integer page,
ejs\EjsDetailsServiceImpl.java	/ejs-details	GET	/memberssearch	0	public EjsResponseDto<EjsMembers> membersSearch(@QueryParam("sub_center_id") BigInteger subCenterId, @QueryParam("page_size") Integer pageSize,
ejs\EjsDetailsServiceImpl.java	/ejs-details	GET	/referdetailssearch	1	public EjsResponseDto<EjsReferDetails> referDetailsSearch(@QueryParam("sub_center_id") BigInteger subCenterId, @QueryParam("page_size") Integer pageSize,
ejs\EjsDetailsServiceImpl.java	/ejs-details	GET	/schooldetailssearch	1	public EjsResponseDto<EjsSchoolDetails> schoolDetailsSearch(@QueryParam("sub_center_id") BigInteger subCenterId, @QueryParam("page_size") Integer pageSize,
ejs\EjsDetailsServiceImpl.java	/ejs-details	GET	/workdetailssearch	1	public EjsResponseDto<EjsWorkDetails> workDetailsSearch(@QueryParam("sub_center_id") BigInteger subCenterId, @QueryParam("page_size") Integer pageSize,
ejs\EjsDetailsServiceImpl.java	/ejs-details	GET	getPatOutreachData/{patientId}	1	public OutReachDataView getPatOutreachData(@PathParam("patientId") BigInteger patientId) throws GenericApplicationException 
ejs\EjsDetailsServiceImpl.java	/ejs-details	POST	/abortiondetailsdata	0	public List<ResponseEjs> abortionDetailsData(@BodyParam("body") List<EjsAbortionDetails> body) throws GenericApplicationException 
ejs\EjsDetailsServiceImpl.java	/ejs-details	POST	/cbacdata	0	public List<ResponseEjs> cbacData(@BodyParam("body") List<EjsCbac> body) throws GenericApplicationException 
ejs\EjsDetailsServiceImpl.java	/ejs-details	POST	/coronaFamilyDetails	0	public List<BigInteger> coronaFamilyDetails(@BodyParam("body") List<EjsCoronaFamilyDetails> body) throws GenericApplicationException 
ejs\EjsDetailsServiceImpl.java	/ejs-details	POST	/coronaMembersDetails	1	public List<BigInteger> coronaMembersDetails(@BodyParam("body") List<EjsCoronaMembersDetails> body) throws GenericApplicationException 
ejs\EjsDetailsServiceImpl.java	/ejs-details	POST	/deathdetailsdata	0	public List<ResponseEjs> deathDetailsData(@BodyParam("body") List<EjsDeathDetails> body)
ejs\EjsDetailsServiceImpl.java	/ejs-details	POST	/diseasesdata	0	public List<ResponseEjs> diseasesData(@BodyParam("body") List<EjsDisease> body) throws GenericApplicationException 
ejs\EjsDetailsServiceImpl.java	/ejs-details	POST	/ecsdata	0	public List<ResponseEjs> ecsData(@BodyParam("body") List<EjsEcs> body) throws GenericApplicationException 
ejs\EjsDetailsServiceImpl.java	/ejs-details	POST	/familyawcdetailsdata	0	public List<BigInteger> familyAwcDetailsData(@BodyParam("body") List<EjsFamilyAwcDetails> body) throws GenericApplicationException 
ejs\EjsDetailsServiceImpl.java	/ejs-details	POST	/familydetailsdata	0	public List<ResponseEjs> familyDetailsData(@BodyParam("body") List<EjsFamilyDetails> body) throws GenericApplicationException
ejs\EjsDetailsServiceImpl.java	/ejs-details	POST	/familyplanningdata	0	public List<ResponseEjs> familyPlanningData(@BodyParam("body") List<EjsFamilyPlanning> body) throws GenericApplicationException 
ejs\EjsDetailsServiceImpl.java	/ejs-details	POST	/helpdata	0	public List<ResponseEjs> helpData(@BodyParam("body") List<EjsHelp> body) throws GenericApplicationException 
ejs\EjsDetailsServiceImpl.java	/ejs-details	POST	/mapEjsChecklistQuestionsActivity	0	public boolean mapEjsChecklistQuestionsActivity(@QueryParam("id") BigInteger id, @QueryParam("activityId") BigInteger activityId) throws GenericApplicationException 
ejs\EjsDetailsServiceImpl.java	/ejs-details	POST	/mcdetailsdata	0	public List<BigInteger> mcdetailsData(@BodyParam("body") List<EjsMcDetails> body) throws GenericApplicationException 
ejs\EjsDetailsServiceImpl.java	/ejs-details	POST	/mchndata	0	public List<BigInteger> mchnData(@BodyParam("body") List<EjsMchn> body) throws GenericApplicationException 
ejs\EjsDetailsServiceImpl.java	/ejs-details	POST	/membersdata	0	public List<ResponseEjs> membersData(@BodyParam("body") List<EjsMembers> body) throws GenericApplicationException 
ejs\EjsDetailsServiceImpl.java	/ejs-details	POST	/migrationdetailsdata	1	public void migrationDetailsData(@BodyParam("body") EjsMigrationDetailsDto body) 
ejs\EjsDetailsServiceImpl.java	/ejs-details	POST	/referdetailsdata	0	public List<ResponseEjs> referDetailsData(@BodyParam("body") List<EjsReferDetails> body) throws GenericApplicationException 
ejs\EjsDetailsServiceImpl.java	/ejs-details	POST	/saveEjsmasteractivity	0	public EjsMasterActivity saveEjsmasteractivity(@BodyParam("body") EjsMasterActivity body) throws GenericApplicationException 
ejs\EjsDetailsServiceImpl.java	/ejs-details	POST	/saveEjsmasterentity	0	public EjsMasterEntity saveEjsmasterentity(@BodyParam("body") EjsMasterEntity body) throws GenericApplicationException 
ejs\EjsDetailsServiceImpl.java	/ejs-details	POST	/savemedia	0	public void saveMedia(Attachment attachment, @QueryParam("name") String name, @QueryParam("typeIds") String typeIds, @QueryParam("categoryIds") String categoryIds, @QueryParam("id"
ejs\EjsDetailsServiceImpl.java	/ejs-details	POST	/schooldetailsdata	0	public List<ResponseEjs> schoolDetailsData(@BodyParam("body") List<EjsSchoolDetails> body) throws GenericApplicationException 
ejs\EjsDetailsServiceImpl.java	/ejs-details	POST	/uploadphotochild	1	public Map<String, String> uploadPhotoChild(List<Attachment> attachments) throws GenericApplicationException 
ejs\EjsDetailsServiceImpl.java	/ejs-details	POST	/uploadphotoecs	1	public Map<String, String> uploadPhotoEcs(List<Attachment> attachments) throws GenericApplicationException 
ejs\EjsDetailsServiceImpl.java	/ejs-details	POST	/uploadphotopnc	1	public Map<String, String> uploadPhotoPnc(List<Attachment> attachments) throws GenericApplicationException 
ejs\EjsDetailsServiceImpl.java	/ejs-details	POST	/workdetailsdata	0	public List<ResponseEjs> workDetailsData(@BodyParam("body") List<EjsWorkDetails> body) throws GenericApplicationException 
ejs\EjsIncentiveServiceImpl.java	/ejs-incentive	GET	/calculatePendingIncentives	0	public void calculatePendingIncentives() throws GenericApplicationException 
ejs\EjsIncentiveServiceImpl.java	/ejs-incentive	GET	/getAshaIncentiveInfo	1	public EjsClaimDetailDto getAshaIncentiveInfo(@QueryParam("user_id") Long userId, @QueryParam("from_date") String fromDate, @QueryParam("to_date") String toDate) throws GenericAppl
ejs\EjsIncentiveServiceImpl.java	/ejs-incentive	GET	/getAshaWorkIncentiveByActivity	0	public List<EjsAshaIncentiveInfoDto> getAshaWorkIncentiveByActivity(@QueryParam("activity_id") Long activityId) throws GenericApplicationException 
ejs\EjsIncentiveServiceImpl.java	/ejs-incentive	GET	/reSubmitIncentiveCalculation/{ashaWorkId}	1	public boolean reSubmitIncentiveCalculation(@PathParam("ashaWorkId") String ashaWorkId) throws GenericApplicationException 
ejs\EjsIncentiveServiceImpl.java	/ejs-incentive	GET	/reSubmitIncentivePaymentTransfer/{paymentId}	1	public boolean reSubmitIncentivePaymentTransfer(@PathParam("paymentId") Long paymentId) throws GenericApplicationException 
ejs\EjsIncentiveServiceImpl.java	/ejs-incentive	POST	/getIncentiveandClaimDetails	0	public List<IncentiveClaimDetailsDto> getIncentiveandClaimDetails(@BodyParam("body") SearchIncentiveClaimDto body, @QueryParam("startRowIndex") Integer page, @QueryParam("maxRows")
ejs\EjsMasterLocationServiceImpl.java	/ejs-masterLocation	GET	/awcs	0	public EjsResponseDto<EjsMasterAwcs> awcs(@QueryParam("id") Long id, @QueryParam("village_id") Long villageId, @QueryParam("page_size") Integer pageSize,
ejs\EjsMasterLocationServiceImpl.java	/ejs-masterLocation	GET	/blocks	0	public EjsResponseDto<EjsMasterBlocks> blocks(@QueryParam("id") Long id, @QueryParam("district_id") Long districtId, @QueryParam("page_size") Integer pageSize,
ejs\EjsMasterLocationServiceImpl.java	/ejs-masterLocation	GET	/districts	0	public EjsResponseDto<EjsMasterDistricts> getDistricts(@QueryParam("id") Long id, @QueryParam("zone_id") Long zoneId, @QueryParam("page_size") Integer pageSize,
ejs\EjsMasterLocationServiceImpl.java	/ejs-masterLocation	GET	/grampanchayats	0	public EjsResponseDto<EjsMasterGrampanchayats> grampanchayats(@QueryParam("id") Long id, @QueryParam("phc_id") Long phcId, @QueryParam("page_size") Integer pageSize,
ejs\EjsMasterLocationServiceImpl.java	/ejs-masterLocation	GET	/phcs	0	public EjsResponseDto<EjsMasterPhcs> phcs(@QueryParam("id") Long id, @QueryParam("block_id") Long blockId, @QueryParam("page_size") Integer pageSize,
ejs\EjsMasterLocationServiceImpl.java	/ejs-masterLocation	GET	/subcenters	0	public EjsResponseDto<EjsMasterSubcenters> subcenters(@QueryParam("id") Long id, @QueryParam("grampanchayat_id") Long grampanchayatId, @QueryParam("page_size") Integer pageSize,
ejs\EjsMasterLocationServiceImpl.java	/ejs-masterLocation	GET	/villages	0	public EjsResponseDto<EjsMasterVillages> villages(@QueryParam("id") Long id, @QueryParam("subcenter_id") Long subcenterId, @QueryParam("page_size") Integer pageSize,
ejs\EjsMasterLocationServiceImpl.java	/ejs-masterLocation	GET	/zones	0	public EjsResponseDto<EjsMasterZones> zones(@QueryParam("id") Long id, @QueryParam("page_size") Integer pageSize,
ejs\EjsPopulationServiceImpl.java	/ejs-population	GET	/count	1	public EjsPopulationCountDto getCount(@QueryParam("state_id") BigInteger stateId,@QueryParam("zone_id") BigInteger zoneId, @QueryParam("district_id") BigInteger districtId, @QueryP
ejs\EjsPopulationServiceImpl.java	/ejs-population	GET	/countage	0	public EjsAgePopulationDto getAgeWisePopulation(@QueryParam("state_id") BigInteger stateId,@QueryParam("zone_id") BigInteger zoneId, @QueryParam("district_id") BigInteger district
ejs\EjsPopulationServiceImpl.java	/ejs-population	GET	/countanc	0	public EjsPopulationCountAncDto getAncCount(@QueryParam("state_id") BigInteger stateId,@QueryParam("zone_id") BigInteger zoneId, @QueryParam("district_id") BigInteger districtId, @
ejs\EjsPopulationServiceImpl.java	/ejs-population	GET	/countchild	0	public EjsPopulationCountChildDto getChildCount(@QueryParam("state_id") BigInteger stateId,@QueryParam("zone_id") BigInteger zoneId, @QueryParam("district_id") BigInteger districtI
ejs\EjsPopulationServiceImpl.java	/ejs-population	GET	/countchildimmun	0	public EjsPopulationCountChildImmunDto getChildImmunCount(@QueryParam("state_id") BigInteger stateId,@QueryParam("zone_id") BigInteger zoneId, @QueryParam("district_id") BigInteger
ejs\EjsPopulationServiceImpl.java	/ejs-population	GET	/countecs	0	public EjsPopulationCountEcsDto getEcsCount(@QueryParam("state_id") BigInteger stateId,@QueryParam("zone_id") BigInteger zoneId, @QueryParam("district_id") BigInteger districtId, @
ejs\EjsPopulationServiceImpl.java	/ejs-population	GET	/countpnc	0	public EjsPopulationCountPncDto getPncCount(@QueryParam("state_id") BigInteger stateId,@QueryParam("zone_id") BigInteger zoneId, @QueryParam("district_id") BigInteger districtId, @
ejs\EjsPopulationServiceImpl.java	/ejs-population	GET	/masterawcspopulation	0	public Map<String, BigInteger> getMasterAwcsPopulation(@QueryParam("subcenter_id") BigInteger subCenterId, @QueryParam("awc_id") BigInteger awcId) throws GenericApplicationExceptio
ejs\EjsPopulationServiceImpl.java	/ejs-population	POST	/masterawcsdata	0	public List<BigInteger> masterAwcsData(@BodyParam("body") List<EjsMasterAwcs> body) throws GenericApplicationException 
ejs\EjsSupervisorServiceImpl.java	/ejs-supervisor	GET	/ancsearchbysupervisor	0	public EjsResponseDto<EjsSupervisorAncDto> ancSearchBySupervisor(@QueryParam("village_id") BigInteger villageId, @QueryParam("page_size") Integer pageSize,
ejs\EjsSupervisorServiceImpl.java	/ejs-supervisor	GET	/childcaresearchbysupervisor	0	public EjsResponseDto<EjsSupervisorChildDto> childCareSearchBySupervisor(@QueryParam("village_id") BigInteger villageId, @QueryParam("page_size") Integer pageSize,
ejs\EjsSupervisorServiceImpl.java	/ejs-supervisor	GET	/ecssearchbysupervisor	0	public EjsResponseDto<EjsEcs> searchEcsBySupervisor(@QueryParam("village_id") BigInteger villageId, @QueryParam("page_size") Integer pageSize,
ejs\EjsSupervisorServiceImpl.java	/ejs-supervisor	GET	/memberssearchbysupervisor	0	public EjsResponseDto<EjsMembers> membersSearchBySupervisor(@QueryParam("village_id") BigInteger villageId, @QueryParam("page_size") Integer pageSize,
ejs\EjsSupervisorServiceImpl.java	/ejs-supervisor	GET	/pncsearchbysupervisor	0	public EjsResponseDto<EjsSupervisorPncDto> pncSearchBySupervisor(@QueryParam("village_id") BigInteger villageId, @QueryParam("page_size") Integer pageSize,
ejs\EjsSupervisorServiceImpl.java	/ejs-supervisor	GET	/visitssearch	0	public EjsResponseDto<EjsSupervisorVisits> visitsSearch(@QueryParam("page_size") Integer pageSize,
ejs\EjsSupervisorServiceImpl.java	/ejs-supervisor	POST	/ancdata	1	public List<BigInteger> ancData(@BodyParam("body") List<EjsSupervisorAnc> body) throws GenericApplicationException 
ejs\EjsSupervisorServiceImpl.java	/ejs-supervisor	POST	/childdata	1	public List<BigInteger> childData(@BodyParam("body") List<EjsSupervisorChild> body) throws GenericApplicationException 
ejs\EjsSupervisorServiceImpl.java	/ejs-supervisor	POST	/ecsdata	1	public List<BigInteger> ecsData(@BodyParam("body") List<EjsSupervisorEcs> body) 
ejs\EjsSupervisorServiceImpl.java	/ejs-supervisor	POST	/membersdata	1	public List<BigInteger> membersData(@BodyParam("body") List<EjsSupervisorMember> body) throws GenericApplicationException 
ejs\EjsSupervisorServiceImpl.java	/ejs-supervisor	POST	/pncdata	1	public List<BigInteger> pncData(@BodyParam("body") List<EjsSupervisorPnc> body) throws GenericApplicationException 
ejs\EjsSupervisorServiceImpl.java	/ejs-supervisor	POST	/visitsdata	0	public List<BigInteger> visitsData(@BodyParam("body") List<EjsSupervisorVisits> body) throws GenericApplicationException 
FileManagementServiceImpl.java	/fileService	GET	/downloadActualFile	0	public void getActualDownload(@QueryParam("fileDetailId") String fileDetailIdHash, @Context HttpServletResponse response) throws GenericApplicationException 
FileManagementServiceImpl.java	/fileService	GET	/downloadFile	1	public String getDownload(@HeaderParam("token") String token, @QueryParam("fileDetailId") Long fileDetailId) throws GenericApplicationException, IOException 
FileManagementServiceImpl.java	/fileService	GET	/getAllHospitalLocation	1	public List<HospitalGeoLocationMapping> getAllHospitalLocation()
FileManagementServiceImpl.java	/fileService	GET	/getFilesAndDocumentsByCategory	0	public FileCategoriesAndDocumentDto getFileDocumentByCategory(@QueryParam("parentFileCategoryId") Long parentFileCategoryId,@QueryParam("patientId") String patientId ,@QueryParam("
FileManagementServiceImpl.java	/fileService	GET	/getFilesAndDocumentsByCategoryAndDocumentType	0	public FileCategoriesAndDocumentDto getFilesAndDocumentsByCategoryAndDocumentType(@QueryParam("parentFileCategoryId") Long parentFileCategoryId,@QueryParam("documentType") String d
FileManagementServiceImpl.java	/fileService	GET	/getImageAnnotationList	0	public List<ImageAnnotationMaster> getImageAnnotationList(@QueryParam("specialityId") Long specialityId)
FileManagementServiceImpl.java	/fileService	POST	/addDocumentCategory	0	public FileStore addDocumentCategory(@BodyParam("body") FileStore body) throws GenericApplicationException 
FileManagementServiceImpl.java	/fileService	POST	/deleteDocument	0	public void deleteDocument(@QueryParam("fileDetailId") Long fileDetailId,@QueryParam("fileStoreId") Long fileStoreId,@QueryParam("documentType") String documentType) throws Generic
FileManagementServiceImpl.java	/fileService	POST	/deleteDocumentCategory	1	public FileStore deleteDocumentCategory(@QueryParam("fileStoreId") Long fileStoreId) throws GenericApplicationException 
FileManagementServiceImpl.java	/fileService	POST	/fileUpload	0	public List<StorageRequest> fileUpload(@QueryParam("fileStoreId") Long fileStoreId, final List<Attachment> attachments, @QueryParam("documentType") String documentType ,@QueryParam
FileManagementServiceImpl.java	/fileService	POST	/getAllFileCategories	0	public List<FileStore> getAllFileCategory() throws GenericApplicationException 
FileManagementServiceImpl.java	/fileService	POST	/getChildFileCategoriesAndDocument	0	public List<FileCategoriesAndDocumentDto> getChildFileAndDocument(@QueryParam("parentFileCategoryId") Long parentFileCategoryId) throws GenericApplicationException 
FileManagementServiceImpl.java	/fileService	POST	/getFileCategories	1	public List<FileStore> getFileCategory(@QueryParam("documentType") String documentType) throws GenericApplicationException 
FileManagementServiceImpl.java	/fileService	POST	/getFileCategoryTree	0	public List<FileCategoriesAndDocumentDto> getFileCategoryTree(@QueryParam("documentType") String documentType) throws GenericApplicationException 
FileManagementServiceImpl.java	/fileService	POST	/getHospitalLocationById	0	public HospitalGeoLocationMapping getHospitalLocationById(@QueryParam("id") Long id) throws GenericApplicationException
FileManagementServiceImpl.java	/fileService	POST	/getImageToAnnotate	0	public ImageAnnotationMaster getImageToAnnotate(@QueryParam("id") Long id) throws GenericApplicationException
FileManagementServiceImpl.java	/fileService	POST	/getPatientDocuments	1	public List<PatientDocuments> getPatientDocuments(@BodyParam("body") PatientDocuments patientDocuments) throws GenericApplicationException, IOException 
FileManagementServiceImpl.java	/fileService	POST	/renameDocumentCategory1	1	public FileStore renameDocumentCategory(@QueryParam("fileStoreId") Long fileStoreId,@QueryParam("fileCategory") String fileCategory) throws GenericApplicationException 
FileManagementServiceImpl.java	/fileService	POST	/savePatientDocuments	1	public Boolean savePatientDocuments(@BodyParam("body") List<PatientDocuments> patientDocuments) throws GenericApplicationException, IOException 
FileManagementServiceImpl.java	/fileService	POST	/uploadFileThroughMachine	1	public Boolean uploadFileThroughMachine(@QueryParam("fileStoreName") String fileStoreName, final List<Attachment> attachments, @QueryParam("documentType") String documentType ,@Que
FileManagementServiceImpl.java	/fileService	POST	/uploadHospitalLocation	1	public HospitalGeoLocationMapping uploadHospitalLocation(final List<Attachment> attachments, @QueryParam("unitId") Long unitId, @QueryParam("exactHospitalName") String exactHospita
FileManagementServiceImpl.java	/fileService	POST	/uploadHospitalLocation	1	public HospitalGeoLocationMapping uploadHospitalLocation(HospitalGeoLocationMapping body) throws GenericApplicationException 
FileManagementServiceImpl.java	/fileService	POST	/uploadImageAnnotationMaster	0	public ImageAnnotationMaster uploadImageAnnotationMaster(final List<Attachment> attachments, @QueryParam("unitId") Long unitId, @QueryParam("specialityId") Long specialityId, @Quer
FileManagementServiceImpl.java	/fileService	POST	/viewDocument	0	public FileDetails viewDocument(@QueryParam("fileDetailId") Long fileDetailId) throws GenericApplicationException, IOException 
IntegrationServiceImpl.java	/integration	GET	/checkAadharNoByRefAadharNo	1	public String checkAadharNoByRefAadharNoApi(@QueryParam("aadharRefNo") String aadharRefNo) throws GenericApplicationException 
IntegrationServiceImpl.java	/integration	GET	/consent/{patientId}/{token}	1	public Boolean handleConsent(@PathParam("patientId") String patientId, @PathParam("token") String token) throws GenericApplicationException 
IntegrationServiceImpl.java	/integration	GET	/getCurrentlyAdmittedPatientsByInsScheme	0	public CurrentlyAdmittedPatientForSchemeResponse getCurrentlyAdmittedPatientsByInsScheme(@HeaderParam("token") String token, @QueryParam("hfrId") String hfrId, @QueryParam("scheme"
IntegrationServiceImpl.java	/integration	GET	/getEraktkoshRequest	1	public List<EraktkoshRequest> getEraktkoshRequest() throws GenericApplicationException 
IntegrationServiceImpl.java	/integration	GET	/getOpdIpdData	0	public NMCDashboardOpdIpdDto getOpdIpdData(@HeaderParam("hfidABDM") String hfidABDM, @HeaderParam("hfidHMIS") String hfidHMIS, @HeaderParam("fromDate") String fromDate,
IntegrationServiceImpl.java	/integration	GET	/getTokenPrint/{transactionId}	0	public Map<String, String> getTokenPrint(@PathParam("transactionId") String transactionId)
IntegrationServiceImpl.java	/integration	GET	/sendOtpToJanaadhaarMobileNo	1	public String sendOtpToJanaadhaarMobileNo(@QueryParam("provValue") String provValue, @QueryParam("janaadhaarId") String janaadhaarId, @QueryParam("isPatHistory") @DefaultValue("fal
IntegrationServiceImpl.java	/integration	POST	/aadhaarAuthenticate	0	public AadhaarAuthResponse aadhaarAuthenticate(@BodyParam("body") AadhaarAuthRequest body) throws Exception 
IntegrationServiceImpl.java	/integration	POST	/addeRaktkoshDonation	1	public void addeRaktkoshDonation(@BodyParam("body") List<ERaktkoshDonationRequestDto> body , @QueryParam("eRaktkoshRequestId") Long eRaktkoshRequestId ) throws Exception 
IntegrationServiceImpl.java	/integration	POST	/addPHCClinicalData	1	public UpdateExtPackageResponseDto addPHCClinicalData(@BodyParam("body") PHCClinicalRequestDto body, @HeaderParam("token") String token) throws GenericApplicationException 
IntegrationServiceImpl.java	/integration	POST	/addRghsPackages	0	public BillInfoDto addRghsPackages(@BodyParam("body") ServiceOrderDto body) throws GenericApplicationException 
IntegrationServiceImpl.java	/integration	POST	/addTokenPrintObject	0	public String addTokenPrintObject(@BodyParam("body") Map<String, String> tokenPrintObject) throws GenericApplicationException, ParseException 
IntegrationServiceImpl.java	/integration	POST	/checkRGHSEligiblity	0	public EligibilityCheck checkRGHSEligiblity(@BodyParam("body") EligibilityCheck eligibilityCheck) throws GenericApplicationException, ParseException 
IntegrationServiceImpl.java	/integration	POST	/decryptResponse	0	public String validateResponse(@BodyParam("body") AadhaarEncryptedResponseRequest encryptedResponse) throws Exception 
IntegrationServiceImpl.java	/integration	POST	/eUpkaranItemStockDetails	1	public UpdateExtPackageResponseDto eUpkaranItemStockDetails(@BodyParam("body") List<EUpkaranItemDetailsDto> body, @HeaderParam("token") String token) throws GenericApplicationExcep
IntegrationServiceImpl.java	/integration	POST	/generateEncryptedData	1	public String[] generateEncryptedData(@BodyParam("body") AadhaarAuthValidationDto body) throws GenericApplicationException 
IntegrationServiceImpl.java	/integration	POST	/pushEmergencyCaseDetails	0	public UpdateExtPackageResponseDto getEmergencyCaseDetails(@HeaderParam("token") String token, @BodyParam("body") List<EmergencyCaseDetailsDto> body) throws GenericApplicationExcep
IntegrationServiceImpl.java	/integration	POST	/tokenEndpointforDataSyncing	0	public NMCDashboardTokenResponseDto tokenEndpointforDataSyncing(@BodyParam("body") NMCDashboardTokenDto body) throws GenericApplicationException 
IntegrationServiceImpl.java	/integration	POST	/updateExtPackages	0	public UpdateExtPackageResponseDto updateExtPackages(@BodyParam("body") UpdateExtPackageRequestDto body, @HeaderParam("token") String token) throws GenericApplicationException 
IntegrationServiceImpl.java	/integration	POST	/uploadRghsPackageDocuments	1	public Boolean uploadRghsPackageDocuments(@QueryParam("eligibilityId")Long id, @QueryParam("patientId")Long patientId) throws GenericApplicationException 
IntegrationServiceImpl.java	/integration	POST	/verifyOtpForJanaadhaarMobileNo	0	public OnlineAppointmentDto verifyOtpForJanaadhaarMobileNo(@QueryParam("otp") String otp, @QueryParam("provValue") String provValue, @QueryParam("janaadhaarId") String janaadhaarId
IntegrationServiceImpl.java	/integration	POST	/whatsAppMessageStatusUpdate	1	public UpdateExtPackageResponseDto whatsAppMessageStatusUpdateTrigger(@HeaderParam("token") String token, @BodyParam("body") WhatsAppBusinessAccountPayload body) throws GenericAppl
LinenLaundryServiceImpl.java	/linenLaundry	GET	/approveLinenRequest	1	public Boolean approveLinenRequest(@QueryParam("linenId") Long linenId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
LinenLaundryServiceImpl.java	/linenLaundry	GET	/deleteLinenRequest	0	public Boolean deleteLinenRequest(@QueryParam("linenId") Long linenId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
LinenLaundryServiceImpl.java	/linenLaundry	GET	/getItemsForLinenRequest	0	public List<ItemByNameResultDto> getItemsForLinenRequest(@QueryParam("storeid") Long storeid) throws GenericApplicationException 
LinenLaundryServiceImpl.java	/linenLaundry	GET	/getLinenItemDetails	1	public List<LinenRequestDetails> getLinenItemDetails(@QueryParam("linenId") Long linenId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
LinenLaundryServiceImpl.java	/linenLaundry	GET	/getLinenItemDetailsForReport	1	public List<LinenRequestDetails> getLinenItemDetailsForReport(@QueryParam("reqNo") String reqNo, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
LinenLaundryServiceImpl.java	/linenLaundry	GET	/handOverLinenItems	1	public String handOverLinenItems(@QueryParam("linenId") Long linenId, @QueryParam("unitId") Long unitId, @QueryParam("handedOverTo") String handedOverTo) throws GenericApplicationE
LinenLaundryServiceImpl.java	/linenLaundry	GET	/processLinenRequest	1	public Boolean processLinenRequest(@QueryParam("linenId") Long linenId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
LinenLaundryServiceImpl.java	/linenLaundry	POST	/addNewLinenRequest	1	public LinenRequest addNewLinenRequest(@BodyParam("body") LinenRequestView body) throws GenericApplicationException 
LinenLaundryServiceImpl.java	/linenLaundry	POST	/dispatchLinenRequest	1	public void dispatchLinenRequest(@BodyParam("body") LinenRequestView body) throws GenericApplicationException 
LinenLaundryServiceImpl.java	/linenLaundry	POST	/getCurrentLinenStatus	0	public List<LinenRequest> getCurrentLinenStatus(@BodyParam("body") CurrentLinenStatusDto body, @QueryParam("page") Integer page, @QueryParam("count") Integer count,
LinenLaundryServiceImpl.java	/linenLaundry	POST	/getLinenRequestCount	0	public Map<String, Long> getLinenRequestCount(@BodyParam("body") CurrentLinenStatusDto body) throws GenericApplicationException 
LinenLaundryServiceImpl.java	/linenLaundry	POST	/receiveLinenRequest	1	public void receiveLinenRequest(@BodyParam("body") LinenRequestView body) throws GenericApplicationException 
LinenLaundryServiceImpl.java	/linenLaundry	POST	/receiveLinenRequestMainStore	1	public void receiveLinenRequestMainStore(@BodyParam("body") LinenRequestView body) throws GenericApplicationException 
LinenLaundryServiceImpl.java	/linenLaundry	POST	/updateItemsForLinenRequest	1	public void updateItemsForLinenRequest(@BodyParam("body") LinenRequestView body) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	DELETE	/deleteNote	1	public boolean deleteNote(@QueryParam("noteId") Long noteId) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	DELETE	/deleteNursePatientAllocation	0	public Integer deleteNursePatientAllocation(@BodyParam("body") NursePatientDto body)
nurse\NursingServiceImpl.java	/nursingList	DELETE	/deleteNursePatientAllocationOnShiftHandOver	1	public Integer deleteNursePatientAllocationOnShiftHandOver(@BodyParam("body") NursePatientDto body)
nurse\NursingServiceImpl.java	/nursingList	DELETE	/deleteService	1	public boolean deleteService(@QueryParam("chargeId") Long chargeId, @QueryParam("opdIpdId") Long opdIpdId)
nurse\NursingServiceImpl.java	/nursingList	DELETE	/removeMappedDiagnosis	1	public Boolean removeMappedDiagnosis(@QueryParam("complaintId") Long complaintId,
nurse\NursingServiceImpl.java	/nursingList	GET	/ancChartConfiguration	1	public JSONArray ancChartConfiguration(@QueryParam("chartType") String chartType)
nurse\NursingServiceImpl.java	/nursingList	GET	/assignBodyToHimSelf	0	public Boolean assignBodyToHimSelf(@QueryParam("admId") Long admId, @QueryParam("doctorId") Long doctorId, @QueryParam("isReleased") boolean isReleased) throws GenericApplicationEx
nurse\NursingServiceImpl.java	/nursingList	GET	/assignNurseToTask	0	public Long assignNurseToTask(@QueryParam("nurseUserId") Long nurseUserId, @QueryParam("taskId") Long taskId)
nurse\NursingServiceImpl.java	/nursingList	GET	/checkEctsNumberIsExist	0	public Boolean checkEctsNumberIsExist(@QueryParam("patientId") Long patientId,
nurse\NursingServiceImpl.java	/nursingList	GET	/checkedInPatientOnBed	1	public Boolean checkInAdmitedPatient(@QueryParam("admissionId") Long admissionId)
nurse\NursingServiceImpl.java	/nursingList	GET	/checkFamilyPlanningCompleted	1	public Boolean checkFamilyPlanningCompleted(@QueryParam("patientId") Long patientId)
nurse\NursingServiceImpl.java	/nursingList	GET	/checkIsTsuEncounterClosed	0	public boolean checkIsTsuEncounterClosed(@QueryParam("encounterId") Long encounterId)
nurse\NursingServiceImpl.java	/nursingList	GET	/checkNurseAllocationForPatient	1	public Boolean checkNurseAllocationForPatient(@QueryParam("staffid") Long staffid,
nurse\NursingServiceImpl.java	/nursingList	GET	/checkNurseAllocationForShift	0	public Boolean checkNurseAllocationForShift(@QueryParam("staffid") Long staffid,
nurse\NursingServiceImpl.java	/nursingList	GET	/checkNurseAllocationForShiftCnt	0	public Boolean checkNurseAllocationForShiftCnt(@QueryParam("staffid") Long staffid,
nurse\NursingServiceImpl.java	/nursingList	GET	/checkNurseShiftTime	1	public Boolean checkNurseShiftTime(@QueryParam("staffid") Long staffid, @QueryParam("patientId") Long patientId,
nurse\NursingServiceImpl.java	/nursingList	GET	/checkNurseWardAllocation	0	public String checkNurseWardAllocation(@QueryParam("nurseId") Long nurseId, @QueryParam("wardId") Long wardId,
nurse\NursingServiceImpl.java	/nursingList	GET	/checkoutEncounterVisit	0	public Boolean checkoutEncounterVisit(@QueryParam("patientId") Long patientId, @QueryParam("opdIpdId") Long opdIpdId) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	GET	/checkVaccineInterval	0	public Boolean checkVaccineInterval(@QueryParam("opdIpdId") Long opdIpdId, @QueryParam("opdIpd") Long opdIpd,
nurse\NursingServiceImpl.java	/nursingList	GET	/checkValidSign	1	public boolean checkValidSign(@QueryParam("sign") String sign) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	GET	/deleteCapturedMachineVitals	0	public List<MachineVitals> deleteCapturedMachineVitals(@QueryParam("patientId") Long patientId)
nurse\NursingServiceImpl.java	/nursingList	GET	/deleteNurseNote	0	public boolean deleteNurseNote(@QueryParam("noteId") Long noteId) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	GET	/deletePatientAdvice	1	public void deletePatientAdvice(@QueryParam("id") Long id) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	GET	/deletePatientAllergies	1	public void deletePatientAllergies(@QueryParam("allergyIen") Long allergyIen,
nurse\NursingServiceImpl.java	/nursingList	GET	/deletePatientComplaint	0	public void deletePatientComplaint(@QueryParam("id") Long id) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	GET	/deletePatientDiagnosis	1	public void deletePatientDiagnosis(@QueryParam("id") Long id, @QueryParam("patientId") @DefaultValue("0") Long patientId,
nurse\NursingServiceImpl.java	/nursingList	GET	/deletePatientNote	0	public Boolean deletePatientNote(@QueryParam("patientNoteId") Long patientNoteId) 
nurse\NursingServiceImpl.java	/nursingList	GET	/deleteVitals	0	public void deleteVitals(@QueryParam("reason") String reason, @QueryParam("vitalIds") String vitalIds)
nurse\NursingServiceImpl.java	/nursingList	GET	/discontinueNursingOrders	0	public Boolean discontinueNursingOrders(@QueryParam("id") Long id) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	GET	/doctorAvailableSlots	1	public List<Doctorschedule> doctorAvailableSlots(@QueryParam("doctorId") Long doctorId,
nurse\NursingServiceImpl.java	/nursingList	GET	/getAllANCVisitByPatientId	0	public JSONObject getAllANCVisitByPatientId(@QueryParam("patientId") Long patientId,
nurse\NursingServiceImpl.java	/nursingList	GET	/getAllClinicalService	0	public List<ClinicalOrders> getAllClinicalService(@QueryParam("opdIpdId") Long opdIpdId, @QueryParam("patientId") Long patientId, @QueryParam("opdIpd") Long opdIpd,
nurse\NursingServiceImpl.java	/nursingList	GET	/getAllEmptyBedsByWardId	1	public List<Bedmaster> getAllEmptyBedsByWardId(@QueryParam("wardId") Long wardId) 
nurse\NursingServiceImpl.java	/nursingList	GET	/getAllIncidentTypes	0	public Map<String, List<IncidentTypeMaster>> getAllIncidentTypes() throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	GET	/getAllocatedAlliedStaffByClinicId	0	public List<NurseClinicAllocationDetailsDto> getAllocatedAlliedStaffByClinicId(
nurse\NursingServiceImpl.java	/nursingList	GET	/getAllocatedAlliedStaffByWardId	0	public List<NurseWardAllocationDetailsDto> getAllocatedAlliedStaffByWardId(@QueryParam("wardId") Long wardId)
nurse\NursingServiceImpl.java	/nursingList	GET	/getAllocatedClincsByNurseId	1	public List<NurseClinicAllocationDetailsDto> getAllocatedClincsByNurseId(
nurse\NursingServiceImpl.java	/nursingList	GET	/getAllocatedNursesByClinicId	0	public List<NurseClinicAllocationDetailsDto> getAllocatedNursesByClinicId(@QueryParam("clinicId") Long clinicId)
nurse\NursingServiceImpl.java	/nursingList	GET	/getAllocatedNursesByDepartmentId	0	public List<NurseClinicAllocationDetailsDto> getAllocatedNursesByDepartmentId(
nurse\NursingServiceImpl.java	/nursingList	GET	/getAllocatedNursesByPatientId	0	public List<NurseShiftDetailsResultDto> getAllocatedNursesByPatientId(@QueryParam("date") Date date,
nurse\NursingServiceImpl.java	/nursingList	GET	/getAllocatedNursesByWardId	0	public List<NurseWardAllocationDetailsDto> getAllocatedNursesByWardId(@QueryParam("wardId") Long wardId)
nurse\NursingServiceImpl.java	/nursingList	GET	/getAllocatedWardDetails	1	public List<NurseWardAllocationDetailsDto> getAllocatedWardDetails(@QueryParam("nurseUserId") Long nurseUserId)
nurse\NursingServiceImpl.java	/nursingList	GET	/getAllPatientAdvice	0	public List<Patientadvice> getAllPatientAdvice(@QueryParam("encounterId") Long encounterId,
nurse\NursingServiceImpl.java	/nursingList	GET	/getAllPatientComplaint	0	public List<PatientComplaints> getAllPatientComplaint(@QueryParam("encounterId") Long encounterId, @QueryParam("patientId") Long patientId,
nurse\NursingServiceImpl.java	/nursingList	GET	/getAllVitalUnits	0	public List<VitalUnitDetailsDto> getAllVitalUnits() throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	GET	/getANCData	1	public JSONObject getANCData(@QueryParam("patientId") Long patientId,
nurse\NursingServiceImpl.java	/nursingList	GET	/getAvailableCradleCountByWardId	0	public Long getAvailableCradleCountByWardId(@QueryParam("wardId") Long wardId) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	GET	/getAvailableCradles	0	public Number getAvailableCradles() throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	GET	/getBedManagementDetails	1	public List<PatientBedDetailsResultDto> getClassMasterDetails(@QueryParam("admissionId") Long admissionId,
nurse\NursingServiceImpl.java	/nursingList	GET	/getCallRecords	1	public List<TeleConsultantDetails> getCallRecordDetails(@QueryParam("patientId") Long patientId,
nurse\NursingServiceImpl.java	/nursingList	GET	/getCapturedMachineVitals	0	public List<MachineVitals> getCapturedMachineVitals(@QueryParam("patientId") Long patientId)
nurse\NursingServiceImpl.java	/nursingList	GET	/getChildDeathReviewByPatient	1	public ChildDeathReview getChildDeathReviewByPatient(@QueryParam("patientId") Long patientId,
nurse\NursingServiceImpl.java	/nursingList	GET	/getClinicByDoctor	1	public Object[] getClinicByDoctor(@QueryParam("mrno") String mrno) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	GET	/getComplaintsBySiteId	0	public List<DiagnosisMaster> getComplaintsBySiteId(@QueryParam("siteId") Long siteId)
nurse\NursingServiceImpl.java	/nursingList	GET	/getCWFDetails	0	public PatientMiniDetailsView getCWFDetails(@QueryParam("patientId") Long patientId, @QueryParam("visitId") Long visitId, @QueryParam("compltList") String compltList) throws Generi
nurse\NursingServiceImpl.java	/nursingList	GET	/getDeliveryKitStatus	1	public Boolean getDeliveryKitStatus(@QueryParam("patientId") Long patientId, @QueryParam("unitId") Long unitId)
nurse\NursingServiceImpl.java	/nursingList	GET	/getDepartmentDoctorDetails	0	public List<Doctordepartmentdetails> getDepartmentDoctorDetails(@QueryParam("departmentId") Long departmentId)
nurse\NursingServiceImpl.java	/nursingList	GET	/getDepartmentRelatedDoctors	1	public List<NurseListResultDto> getDepartmentRelatedDoctors(
nurse\NursingServiceImpl.java	/nursingList	GET	/getDiagnosisBySpecialityId	0	public List<DiagnosisMaster> getDiagnosisBySpecialityId(@QueryParam("specialityId") Long specialityId)
nurse\NursingServiceImpl.java	/nursingList	GET	/getDischageFormTicketandVmByDeptId	0	public DepartmentDischargeDetails getDischageFormTicketandVmByDeptId(@QueryParam("departmentId") Long departmentId,
nurse\NursingServiceImpl.java	/nursingList	GET	/getDischargeSummaryForDischargedPat	1	public PatientDischargeData getDischargeSummaryForDischargedPat(@QueryParam("opdIpdId") Long opdIpdId, @QueryParam("patientId")Long patientId) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	GET	/getEctsNumberForPat	0	public String getEctsNumberForPat(@QueryParam("patientId") Long patientId) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	GET	/getEDDByLMP	0	public List<String> getEDDByLMPStatus(@QueryParam("patientId") Long patientId, @QueryParam("unitId") Long unitId)
nurse\NursingServiceImpl.java	/nursingList	GET	/getFamilyPlanningChecklist	0	public List<Procedurechecklistitems> getFamilyPlanningChecklist(@QueryParam("patientId") Long patientId,
nurse\NursingServiceImpl.java	/nursingList	GET	/getFreqAllergies	0	public List<PatientAllergies> getFreqAllergies(@QueryParam("addedBy") Long addedBy,
nurse\NursingServiceImpl.java	/nursingList	GET	/getFrequentItems	0	public List<FrequentItemDto> getFrequentItems(@QueryParam("type") String type, @QueryParam("userId") Long userId)
nurse\NursingServiceImpl.java	/nursingList	GET	/getGrowthChartByParentId	0	public List<GrowthChartMenu> getGrowthChart(@QueryParam("growthChartMenuId") Long growthChartMenuId)
nurse\NursingServiceImpl.java	/nursingList	GET	/getGrowthChartDetails	0	public List<Growthchartdetails> getGrowthChartDetails(@QueryParam("growthChartId") Long growthChartId,
nurse\NursingServiceImpl.java	/nursingList	GET	/getGrowthChartMenu	1	public List<GrowthChartMenu> getGrowthChartMenu() throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	GET	/getGrowthChartValue	0	public Map<String, Object> getGrowthChartValue(@QueryParam("patientId") Long patientId,
nurse\NursingServiceImpl.java	/nursingList	GET	/getImmunizationDetails	0	public List<Immunizationagegroupmaster> getImmunizationDetails(@QueryParam("patientId") Long patientId)
nurse\NursingServiceImpl.java	/nursingList	GET	/getInActivePatientComplaint	1	public List<PatientComplaints> getInActivePatientComplaint(@QueryParam("encounterId") Long encounterId,
nurse\NursingServiceImpl.java	/nursingList	GET	/getIsMLCDetails	0	public Boolean getIsMLCDetails(@QueryParam("opdIpdId") Long opdIpdId, @QueryParam("opdIpd") Long opdIpd)
nurse\NursingServiceImpl.java	/nursingList	GET	/getIsMLCDetailsForDoctor	0	public Boolean getIsMLCDetailsForDoctor(@QueryParam("doctorId") Long doctorId, @QueryParam("unitId") Long unitId)
nurse\NursingServiceImpl.java	/nursingList	GET	/getItemCurrentStockByBatchCode	0	public List<GetBcmaItemCurrentStock> getItemCurrentStockByBatchCode(@QueryParam("storeId") Long storeId,
nurse\NursingServiceImpl.java	/nursingList	GET	/getItemsByTherapeuticClassId	0	public List<Itemmaster> getItemsByTherapeuticClassId(@QueryParam("thrClassId") Long thrClassId)
nurse\NursingServiceImpl.java	/nursingList	GET	/getLabAndRadOrderDetailsByQuery	0	public PatientLabRadDetailsView getLabAndRadOrderDetails(@QueryParam("patientId") Long patientId,
nurse\NursingServiceImpl.java	/nursingList	GET	/getLabTestCollectionDetails	0	public PathOrderBookingDetailList_NEWResultDto_1 getLabTestCollectionDetails(
nurse\NursingServiceImpl.java	/nursingList	GET	/getLangForPatient	0	public LanguagePreferedMasterDto getLangForPatient(@QueryParam("patientId") Long patientId)
nurse\NursingServiceImpl.java	/nursingList	GET	/getLastCallRecord	0	public TeleConsultantDetails getLastCallRecord(@QueryParam("patientId") Long patientId,
nurse\NursingServiceImpl.java	/nursingList	GET	/getLastRecordedPatientVisit	0	public Long getLastRecordedPatientVisit(@QueryParam("patientId") Long patientId, @QueryParam("arg") String arg) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	GET	/getMlcDetailsValue	0	public Number getMlcDetailsValue(@QueryParam("id") Long id, @QueryParam("unitId") Long unitId)
nurse\NursingServiceImpl.java	/nursingList	GET	/getmoleculeMasterDetailsById	1	public String getmoleculeMasterDetails(@QueryParam("id") Long id) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	GET	/getNoteDetailsByNoteTitle	0	public PatientNote getNoteDetailsByNoteTitle(@QueryParam("patientId") Long patientId,
nurse\NursingServiceImpl.java	/nursingList	GET	/getNurseCarerListByUnitId	0	public List<NurseListResultDto> getNurseCarerListByUnitId(@QueryParam("wardId") Long wardId)
nurse\NursingServiceImpl.java	/nursingList	GET	/getNurseCarerTaskDetailsCount	1	public Map<String, Integer> getNurseCarerTaskDetailsCount(@QueryParam("wardId") Long wardId,
nurse\NursingServiceImpl.java	/nursingList	GET	/getNurseListByUnitIdAndAllocatedPatientId	0	public List<NurseShiftDetailsResultDto> getNurseListByUnitIdAndAllocatedPatientId(
nurse\NursingServiceImpl.java	/nursingList	GET	/getNurseListByUnitIdAndAllocatedWardId	0	public List<NurseShiftDetailsResultDto> getNurseListByUnitIdAndWardId(@QueryParam("wardId") Long wardId,
nurse\NursingServiceImpl.java	/nursingList	GET	/getNurseNote	1	public List<NurseNote> getNurseNote() throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	GET	/getNurseNoteById	1	public NurseNote getNurseNoteById(@QueryParam("id") Long id) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	GET	/getNurseWards	0	public List<NurseWardListResultDto> getNurseWards(@QueryParam("unitId") Long unitId)
nurse\NursingServiceImpl.java	/nursingList	GET	/getOrderSetById	1	public Orderset getOrderSetById(@QueryParam("id") Long id) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	GET	/getOrderSetDetails	0	public Ordersetdefinationmaster getOrderSetDetails(@QueryParam("orderSetId") Long orderSetId) 
nurse\NursingServiceImpl.java	/nursingList	GET	/getOrderSetMasterList	0	public List<Ordersetdefinationmaster> getOrderSetMasterList() 
nurse\NursingServiceImpl.java	/nursingList	GET	/getPatDetailsForFpvReg	0	public PatientSpouseDetails getPatDetailsForEctsReg(@QueryParam("mrNo") String mrNo,
nurse\NursingServiceImpl.java	/nursingList	GET	/getPatDetailsForNonEligibleCertificate	0	public PatientAppointmentandRegDto getPatDetailsForNonEligibleCertificate(@QueryParam("patientId") Long patientId,
nurse\NursingServiceImpl.java	/nursingList	GET	/getPatient	0	public List<SearchPatientGeneralDetailsResultDto> getPatient(@QueryParam("search") String search,
nurse\NursingServiceImpl.java	/nursingList	GET	/getPatientAddendumNote	0	public PatientNote getPatientAddendumNote(@QueryParam("patientNoteId") Long patientNoteId)
nurse\NursingServiceImpl.java	/nursingList	GET	/getPatientAllergies	0	public List<PatientAllergies> getPatientAllergies(@QueryParam("oPD_IPD") Long oPD_IPD,
nurse\NursingServiceImpl.java	/nursingList	GET	/getPatientAllergiesByPatientId	0	public List<PatientAllergies> getPatientAllergiesByPatientIdApi(@QueryParam("patientId") Long patientId) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	GET	/getPatientAquity	1	public Number getPatientAquity(@QueryParam("patientId") Long patientId, @QueryParam("opdIpd") Long opdIpd,
nurse\NursingServiceImpl.java	/nursingList	GET	/getPatientComplaint	1	public List<PatientComplaints> getPatientComplaint(@QueryParam("encounterId") Long encounterId,
nurse\NursingServiceImpl.java	/nursingList	GET	/getPatientComplaintDetailsById	0	public PatientComplaints getPatientComplaintDetailsById(@QueryParam("complaintId") Long complaintId)
nurse\NursingServiceImpl.java	/nursingList	GET	/getPatientCurrentVisitClinicalData	0	public PatientClinicalDataView getPatientCurrentVisitClinicalData(@QueryParam("patientId") Long patientId, @QueryParam("visitId") Long visitId) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	GET	/getPatientDetailDateWise	1	public List<CustomBedManagementResultDto> getPatientDetailDateWise(@QueryParam("wardId") Long wardId,
nurse\NursingServiceImpl.java	/nursingList	GET	/getPatientDetailsByPatientIen	1	public GetPatientDetailsBcmaView getPatientDetailByPatientIen(@QueryParam("patientIen") Long patientIen)
nurse\NursingServiceImpl.java	/nursingList	GET	/getPatientDetailsForDemographics	1	public PatientDemographicDetails getPatientDetails(@QueryParam("patientId") Long patientId,
nurse\NursingServiceImpl.java	/nursingList	GET	/getPatientDetailsListWardWise	1	public PatientDetailsListWardWiseView getPatientDetailsListWardWise(@QueryParam("available") boolean available,
nurse\NursingServiceImpl.java	/nursingList	GET	/getPatientDetailsListWardWiseDetails	1	public PatientDetailsListWardWiseView getPatientDetailsListWardWiseDetails(
nurse\NursingServiceImpl.java	/nursingList	GET	/getPatientDiagnosis	1	public List<PatientDiagnosisDetails> getPatientDiagnosisApi(@QueryParam("encounterId") Long encounterId,
nurse\NursingServiceImpl.java	/nursingList	GET	/getPatientDtoMrno	1	public GetPatientResultDto_1 getPatientDtoMrno(@QueryParam("mrnno") String mrnno, @QueryParam("token") String token)
nurse\NursingServiceImpl.java	/nursingList	GET	/getPatientEDOD	1	public Timestamp getPatientEDOD(@QueryParam("addmissionId") Long AddmissionId) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	GET	/getPatientEncounter	1	public Encounter getPatientEncounter(@QueryParam("admVisitId") Long admVisitId) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	GET	/getPatientEncounterById	0	public Encounter getPatientEncounterById(@QueryParam("visitId") Long visitId) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	GET	/getPatientFlagDetails	1	public List<PatientFlagDetails> getPatientFlagDetails(@QueryParam("patientId") Long patientId)
nurse\NursingServiceImpl.java	/nursingList	GET	/getPatientImmunizationDetails	1	public List<PatientImmunizationDto> getPatientImmunizationDetails(@QueryParam("patientId") Long patientId)
nurse\NursingServiceImpl.java	/nursingList	GET	/getPatientListByWardId	0	public List<PatientListByWardIdResultDto> getPatientListByWardId(@QueryParam("wardId") Long wardId)
nurse\NursingServiceImpl.java	/nursingList	GET	/getPatientmarkAsExpiredDetails	0	public DepartmentDischargeDetails getPatientmarkAsExpiredDetails(@QueryParam("opdIpdId") Long opdIpdId,
nurse\NursingServiceImpl.java	/nursingList	GET	/getPatientMLCDetails	0	public MLCDetails getPatientMLCDetails(@QueryParam("opdIpdId") Long opdIpdId, @QueryParam("opdIpd") Long opdIpd)
nurse\NursingServiceImpl.java	/nursingList	GET	/getPatientNote	1	public List<PatientNote> getPatientNote(@QueryParam("patientId") Long patientId,
nurse\NursingServiceImpl.java	/nursingList	GET	/getPatientNote/{id}	0	public PatientNote getPatientNoteById(@PathParam("id") Long id) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	GET	/getPatientNoteByPatNoteIen	0	public PatientNote getPatientNoteByPatNoteIen(@QueryParam("patientId") Long patientId,
nurse\NursingServiceImpl.java	/nursingList	GET	/getPatientPodDate	1	public List<GetpatientPodDateDto> getPatientDetailsList(@QueryParam("patientId") Long patientId)
nurse\NursingServiceImpl.java	/nursingList	GET	/getPatientPrescriptionByPatientId	0	public List<PatientPrescriptionNotesResultDto> getPatientPrescriptionByPatientId(
nurse\NursingServiceImpl.java	/nursingList	GET	/getPatientPresction	1	public List<PatientPrescptionNote> getPatientPresction() throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	GET	/getPatientRabiesImmunizationDetails	1	public PatientNote getPatientRabiesImmunizationDetails(@QueryParam("patientId") Long patientId, @QueryParam("noteTitle") String noteTitle)
nurse\NursingServiceImpl.java	/nursingList	GET	/getPatientVitalDetails	0	public List<PatientVitals> getPatientVitalDetails(@QueryParam("patientId") Long patientId,
nurse\NursingServiceImpl.java	/nursingList	GET	/getPrescriptionPharmOrderDetails	0	public List<GetPrescriptionPharmOrdersDto> findPrescriptionPharmOrderDetails(@QueryParam("patientId") Long patientId, @QueryParam("opd_Ipd") Long opd_Ipd, @QueryParam("opd_Ipd_Id")
nurse\NursingServiceImpl.java	/nursingList	GET	/getPrevMedication	1	public List<GetPrevPharmOrderPerscription> getPrevMedication(@QueryParam("patientId") Long patientId,
nurse\NursingServiceImpl.java	/nursingList	GET	/getProcedureDetailsByGet	0	public Map<String, Object> getProcedureDetails(@QueryParam("patientId") Long patientId)
nurse\NursingServiceImpl.java	/nursingList	GET	/getRemarksSCWF	1	public String getRemarksSCWF(@QueryParam("opdIpdId") Long opdIpdId, @QueryParam("opdipd") Long opdipd, @QueryParam("patientId")Long patientId) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	GET	/getRoleNursePatientListTotalCount	0	public Number getTotalCount(@QueryParam("opdIpd") Long opdIpd, @QueryParam("patientId") Long patientId)
nurse\NursingServiceImpl.java	/nursingList	GET	/getScore	0	public String getScore(@QueryParam("registryType") String registryType, @QueryParam("strJson") String strJson,
nurse\NursingServiceImpl.java	/nursingList	GET	/getTabletPrescription	0	public PatCaseSheetView generateTabletPresciptionApi(@QueryParam("encounterId") Long encounterId, @QueryParam("opdipd") Long opdipd,
nurse\NursingServiceImpl.java	/nursingList	GET	/getTabletPrescriptionDMP	1	public Map<String,Object> getTabletPrescriptionDMP(@QueryParam("encounterId") Long encounterId, @QueryParam("opdipd") Long opdipd,
nurse\NursingServiceImpl.java	/nursingList	GET	/getTempPrescriptionNote	0	public SCWFMappingDTO getTempPrescriptionNoteById(@QueryParam("opdipd") Long opdIpd, @QueryParam("opdipdid") Long opdIpdId) throws GenericApplicationException, UnsupportedEncodingE
nurse\NursingServiceImpl.java	/nursingList	GET	/getTempPrescriptionNote/{id}	0	public SCWFMappingDTO getTempPrescriptionNoteById(@PathParam("id") Long id) throws GenericApplicationException, UnsupportedEncodingException 
nurse\NursingServiceImpl.java	/nursingList	GET	/getTiuXmlFile	0	public void getTiuXmlFile(@QueryParam("preview") Boolean preview, @QueryParam("file") String file, @Context HttpServletResponse resp) throws GenericApplicationException, IOExceptio
nurse\NursingServiceImpl.java	/nursingList	GET	/getVisitByVisitCause	0	public Encounter getVisitByVisitCause(@QueryParam("patientId") Long patientId,
nurse\NursingServiceImpl.java	/nursingList	GET	/getWardWiseActivePatientList	0	public List<AdmittedPatientDto> getWardWiseActivePatientList(@QueryParam("wardId") Long wardId)
nurse\NursingServiceImpl.java	/nursingList	GET	/getWardWisePatientDetailsList	0	public PatientDetailsListWardWiseView getWardWisePatientDetailsList(@QueryParam("available") boolean available,
nurse\NursingServiceImpl.java	/nursingList	GET	/getWardWisePatientDetailsListCount	1	public int getWardWisePatientDetailsListCount(@QueryParam("available") boolean available,
nurse\NursingServiceImpl.java	/nursingList	GET	/holdPatientMedication	1	public void holdPatientMedication(@QueryParam("prescriptionId") Long prescriptionId)
nurse\NursingServiceImpl.java	/nursingList	GET	/markPatientForVideoCall	1	public void markPatientAsArrived(@QueryParam("visitId") Long visitId, @QueryParam("unitId") Long unitId)
nurse\NursingServiceImpl.java	/nursingList	GET	/patientFutureAppointmentStatus	0	public Boolean patientFutureAppointmentStatus(@QueryParam("patientId") Long patientId,
nurse\NursingServiceImpl.java	/nursingList	GET	/removeCarePlanDetails	0	public String removeCarePlanDetails(@QueryParam("carePlanId") Long carePlanId, @QueryParam("unitId") Long unitId)
nurse\NursingServiceImpl.java	/nursingList	GET	/removeIcd10fromSolr	0	public void removeIcd10fromSolr()
nurse\NursingServiceImpl.java	/nursingList	GET	/removePatientComplaint	1	public void removePatientComplaint(@QueryParam("id") Long id, @QueryParam("reason") String reason)
nurse\NursingServiceImpl.java	/nursingList	GET	/saveVisitCause	0	public Encounter saveVisitCause(@QueryParam("patientId") Long patientId, @QueryParam("admVisitId") Long admVisitId,
nurse\NursingServiceImpl.java	/nursingList	GET	/searchAdvice	0	public List<Healthfactormaster> searchAdvice(@QueryParam("seed") String search) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	GET	/searchIcdCM	0	public List<Icd10cmSolrView> searchIcdCM(@QueryParam("seed") String search,
nurse\NursingServiceImpl.java	/nursingList	GET	/showPatientDetails	1	public PatientDetailsResultDto showPatientDetails(@QueryParam("patientId") Long patientId,
nurse\NursingServiceImpl.java	/nursingList	GET	/updateDeliveryKitStatus	0	public Boolean updateDeliveryKitStatus(@QueryParam("patientId") Long patientId, @QueryParam("unitId") Long unitId,
nurse\NursingServiceImpl.java	/nursingList	GET	/updateEDDByLMP	1	public String updateEDDByLMP(@QueryParam("patientId") Long patientId, @QueryParam("unitId") Long unitId,
nurse\NursingServiceImpl.java	/nursingList	GET	/updateMedictaionStatus	1	public void updateMedictaionStatus(@QueryParam("prescriptionIds") String prescriptionIds)
nurse\NursingServiceImpl.java	/nursingList	GET	/updatePatientMedication	1	public void updatePatientMedication(@QueryParam("prescriptionId") Long prescriptionId, @QueryParam("prescriptiondetailsid") Long prescriptiondetailsid)
nurse\NursingServiceImpl.java	/nursingList	GET	/updatePatientRegestrationIen	0	public List<String> updatePatientRegestrationIen(@QueryParam("patientId") Long patientId,
nurse\NursingServiceImpl.java	/nursingList	GET	/updateReleaseRequestStatus	1	public void updateReleaseRequestStatus(@QueryParam("admId") Long admId) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	GET	/updateUSGEDDByLMP	1	public String updateUSGEDDByLMP(@QueryParam("patientId") Long patientId, @QueryParam("unitId") Long unitId,
nurse\NursingServiceImpl.java	/nursingList	POST	/addAquityByPatientDetails	0	public Boolean addAquityByPatientDetails(// ,
nurse\NursingServiceImpl.java	/nursingList	POST	/addCallRecordsDetails	0	public TeleConsultantDetails addCallRecordsDetails(@BodyParam("body") TeleConsultantDetails body)
nurse\NursingServiceImpl.java	/nursingList	POST	/addCarePlanDetails	1	public CarePlanDetailsDto addCarePlanDetails(@BodyParam("body") CarePlanDetailsDto body)
nurse\NursingServiceImpl.java	/nursingList	POST	/addChildDeathReview	1	public ChildDeathReview addChildDeathReview(@BodyParam("body") ChildDeathReview body)
nurse\NursingServiceImpl.java	/nursingList	POST	/addConsultOrder	0	public Long addConsultOrder(@BodyParam("body") List<PendingConsultVisits> pendingConsultVisits)
nurse\NursingServiceImpl.java	/nursingList	POST	/addConsumabled	0	public AddPrescriptionView addUpdatePrescriptionMasterNew(
nurse\NursingServiceImpl.java	/nursingList	POST	/addItemSaleForBcma	1	public ItemSalesDto addItemSaleForBcma(@BodyParam("body") AddItemSalesView body)
nurse\NursingServiceImpl.java	/nursingList	POST	/addNurseDoctorAllocationInfo	0	public Map<DateTime, String> addNurseDoctorAllocationInfo(
nurse\NursingServiceImpl.java	/nursingList	POST	/addNurseNote	0	public NurseNote addNurseNote(@BodyParam("body") NurseNote body) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	POST	/addNursePatientAllocation	0	public Integer saveNursePatientAllocation(@BodyParam("body") NursePatientDto body)
nurse\NursingServiceImpl.java	/nursingList	POST	/addNurseTaskDetails	0	public void addNurseTaskDetails(@BodyParam("body") NurseTaskDetails nurseTaskDetails)
nurse\NursingServiceImpl.java	/nursingList	POST	/addOfflinePatientClinicalData	0	public List<Long> addOfflinePatientClinicalData(@BodyParam("body") List<PatientClinicalDataView> body)
nurse\NursingServiceImpl.java	/nursingList	POST	/addOrderSet	0	public String addOrderSet(@BodyParam("body") Orderset body) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	POST	/addPatientAddendumNote	0	public PatientNote addPatientAddendumNote(@BodyParam("body") PatientNote body) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	POST	/addPatientDischargeData	0	public PatientDischargeData addPatientDischargeData(@BodyParam("body") PatientDischargeData body)
nurse\NursingServiceImpl.java	/nursingList	POST	/addPatientNote	0	public PatientNote addPatientNoteApi(@BodyParam("body") PatientNote body) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	POST	/addPatientNoteForPrescription	0	public PatientPrescptionNote addPatientNoteForPrescription(@BodyParam("body") PatientPrescptionNote body)
nurse\NursingServiceImpl.java	/nursingList	POST	/addRemarksSCWF	1	public Boolean addRemarksSCWF(@BodyParam("body") ClinicalOrders body) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	POST	/addTaperedDose	1	public List<AddPrescriptionView> addtapereddoseApi(@BodyParam("body") List<AddPrescriptionView> addPrescriptionView, @DefaultValue("false") @QueryParam ("doctorPermission") Boolean
nurse\NursingServiceImpl.java	/nursingList	POST	/barthelIndexReport	0	public NursingReportView barthelIndexReport(@BodyParam("body") NursingReportView body)
nurse\NursingServiceImpl.java	/nursingList	POST	/cancelPMRReport	1	public boolean cancelPMRReport(@BodyParam("body") PatientNote body) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	POST	/captureMachineVitals	0	public Boolean captureMachineVitals(@BodyParam("body") MachineVitalsParameterView body,
nurse\NursingServiceImpl.java	/nursingList	POST	/cdrFormPrint	0	public Map<String, Object> cdrFormPrint(@BodyParam("body") Map<String, Object> body)
nurse\NursingServiceImpl.java	/nursingList	POST	/checkDuplicateUnsingedNote	1	public PatientNote checkDuplicateUnsingedNote(@BodyParam("body") PatientNote body)
nurse\NursingServiceImpl.java	/nursingList	POST	/checkNoteForPatient	0	public Boolean checkNoteForPatient(@BodyParam("body") List<PatientPrescptionNote> body)
nurse\NursingServiceImpl.java	/nursingList	POST	/checkSignedPrescription	0	public Boolean checkSignedPrescription(@QueryParam("patientId") Long patientId) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	POST	/completeNursingOrders	1	public void completeNursingOrders(@BodyParam("body") List<PatientNursingOrder> patientNursingOrderList)
nurse\NursingServiceImpl.java	/nursingList	POST	/deleteLabService	1	public void deleteLabService(@BodyParam("body") ServiceOrderDto body) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	POST	/departmentDischargePrint	1	public DepartmentDischargeDto departmentDischargePrint(@QueryParam("departmentId") Long departmentId,
nurse\NursingServiceImpl.java	/nursingList	POST	/doctorPatientAllocation	0	public NurseClinicAllocationDetailsDto doctorPatientAllocation(
nurse\NursingServiceImpl.java	/nursingList	POST	/downloadPMRReportAndDeathForm	1	public PatientDocuments downloadPMRReportAndDeathForm(@QueryParam("patientId") Long patientId, @QueryParam("documentType") String documentType) throws GenericApplicationException
nurse\NursingServiceImpl.java	/nursingList	POST	/getAddedServiceCharges	0	public BillChargesView getCharges(@BodyParam("body") BillChargeDto body) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	POST	/getAddedUnmappedClinicalData	1	public UnmappedClinicalDataView getAddedUnmappedClinicalData(@BodyParam("body") PatientClinicalDataView body)
nurse\NursingServiceImpl.java	/nursingList	POST	/getAllActivePatientMedications	0	public List<GetPrescriptionPharmOrdersDto> getAllActivePatientMedications(
nurse\NursingServiceImpl.java	/nursingList	POST	/getAllDiagnosisMappedToComplaint	1	public List<PrimarySymptomToDiagnosisMapping> getAllDiagnosisMappedToComplaint()
nurse\NursingServiceImpl.java	/nursingList	POST	/getAlliedStaffListByUnitId	0	public List<NurseListResultDto> getAlliedStaffListByUnitId(@BodyParam("body") NurseListDto body)
nurse\NursingServiceImpl.java	/nursingList	POST	/getAllIpPatientList	0	public List<PatientListForNurseRoleResultDto> getAllIPPatientList(
nurse\NursingServiceImpl.java	/nursingList	POST	/getAllNurseListByUnitId	0	public List<NurseListResultDto> getAllNurseListByUnitId(@BodyParam("body") NurseListDto body)
nurse\NursingServiceImpl.java	/nursingList	POST	/getAllocatedDepartmentByNurseId	0	public List<NurseDepartmentAllocationDetailsResultDto> getAllocatedDepartmentByNurseId(
nurse\NursingServiceImpl.java	/nursingList	POST	/getAllocatedDoctorsByNurseId	1	public List<NurseDoctorAllocationDetailsDto> getDoctorsNurseAllocation(@BodyParam("nurseList") List<Long> nurseIds)
nurse\NursingServiceImpl.java	/nursingList	POST	/getAllocatedNursesByDoctorId	0	public Map<Long, List<NurseDoctorAllocationDetailsDto>> getAllocatedNursesByDoctorId(
nurse\NursingServiceImpl.java	/nursingList	POST	/getAllPatientComplaintHistory	0	public List<PatientComplaints> getAllPatientComplaintHistory(@BodyParam("body") PatientComplaints body)
nurse\NursingServiceImpl.java	/nursingList	POST	/getAllPatientNotesByTitle	1	public List<PatientNote> getAllPatientNotesByTitle(PatientNote patientNote) 
nurse\NursingServiceImpl.java	/nursingList	POST	/getAllPatientOrderDetails	1	public NurseOrderView getAllPatientOrderDetails(@BodyParam("body") PatientLabOrderDto body)
nurse\NursingServiceImpl.java	/nursingList	POST	/getAllRoleNursePatientList	0	public List<PatientListForNurseRoleResultDto> getAllRoleNursePatientList(
nurse\NursingServiceImpl.java	/nursingList	POST	/getAllWardInchargeByUnitId	0	public List<NurseListResultDto> getAllWardInchargeByUnitId() throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	POST	/getAllWardList	0	public Map<String, List<NurseWardSpecializationListResultDto>> getAllWardList() throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	POST	/getAnnotatedImage	0	public ImageAnnotationDto getAnnotatedImage(@QueryParam("id") Long id) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	POST	/getAnnotatedImageLst	0	public List<ImageAnnotationDto> getAnnotatedImageLst(@BodyParam("body") ImageAnnotationDto body) 
nurse\NursingServiceImpl.java	/nursingList	POST	/getBabiesDetailsList	0	public List<PatientHealthFactor> getBabiesDetailsList(@BodyParam("body") PatientNote body)
nurse\NursingServiceImpl.java	/nursingList	POST	/getCategoryWisePatientNoteList	1	public Map<String, List<PatientNote>> getCategoryWisePatientNoteList(@BodyParam("body") PatientNote body)
nurse\NursingServiceImpl.java	/nursingList	POST	/getConfirmedCarePlanDetails	1	public List<CarePlanDetailsDto> getConfirmedCarePlanDetails(@BodyParam("body") CarePlanDetailsDto body)
nurse\NursingServiceImpl.java	/nursingList	POST	/getHealthFactorList	0	public List<PatientHealthFactor> getHealthFactorListApi(@BodyParam("body") PatientNote body)
nurse\NursingServiceImpl.java	/nursingList	POST	/getLabAndRadOrderDetails	0	public PatientLabRadDetailsView getLabAndRadOrderDetails(@BodyParam("body") PatientLabOrderDto body)
nurse\NursingServiceImpl.java	/nursingList	POST	/getLabAndRadOrderDetailsForNote	0	public PatientLabRadDetailsView getLabAndRadOrderDetailsForNote(@BodyParam("body") PatientLabOrderDto body)
nurse\NursingServiceImpl.java	/nursingList	POST	/getLatestPatientVitals	0	public List<PatientVitals> getLatestPatientVitalsApi(@BodyParam("body") PatientVitals body)
nurse\NursingServiceImpl.java	/nursingList	POST	/getNoteDetailsListByNoteTitle	0	public List<PatientNote> getNoteDetailsListByNoteTitle(@BodyParam("body") PatientNote patientNote)
nurse\NursingServiceImpl.java	/nursingList	POST	/getNurseCarerTaskDetails	1	public List<NurseTaskDetails> getNurseCarerTaskDetails(@BodyParam("body") NurseTaskDetails body)
nurse\NursingServiceImpl.java	/nursingList	POST	/getNurseListByUnitId	0	public List<NurseListResultDto> getNurseListByUnitId(@BodyParam("body") NurseListDto body)
nurse\NursingServiceImpl.java	/nursingList	POST	/getPatientAdtOrders	1	public Map<String, Object> getPatientAdtOrders(@BodyParam("body") TransferRequest transferRequest)
nurse\NursingServiceImpl.java	/nursingList	POST	/getPatientAllocationReport	0	public BillReceiptView getPatientAllocationReport(@BodyParam("body") NurseShiftDetailsDto body)
nurse\NursingServiceImpl.java	/nursingList	POST	/getPatientFollowUpDetails	0	public String getPatientFollowUpDetails(@QueryParam("patientId") Long patientId)
nurse\NursingServiceImpl.java	/nursingList	POST	/getPatientGeneralDetailsListForSearchNursing	0	public List<SearchPatientGeneralDetailsResultDto> getPatientGeneralDetailsListForSearchNursing(
nurse\NursingServiceImpl.java	/nursingList	POST	/getPatientHealthFactorsByHFName	0	public List<PatientHealthFactor> getPatientHealthFactors(@BodyParam("body") PatientNoteDto body)
nurse\NursingServiceImpl.java	/nursingList	POST	/getPatientNursingOrders	1	public List<PatientNursingOrder> getPatientNursingOrders(@BodyParam("body") PatientNursingOrder patientNursingOrder)
nurse\NursingServiceImpl.java	/nursingList	POST	/getPatientPartographDetails	1	public PatientPartographDetailsDto getPatientPartographDetails(@BodyParam("body") PatientPartographDetailsDto body)
nurse\NursingServiceImpl.java	/nursingList	POST	/getPatientPrescriptionDetails	0	public NursingReportView getPatientPrescriptionDetails(@QueryParam("patientId") Long patientId,
nurse\NursingServiceImpl.java	/nursingList	POST	/getPatientVitals	1	public List<PatientVitals> getPatientVitals(@BodyParam("body") PatientVitals body)
nurse\NursingServiceImpl.java	/nursingList	POST	/getPatientVitalsByDate	0	public List<PatientVitals> getPatientVitalsByDate(@BodyParam("body") PatientVitals body)
nurse\NursingServiceImpl.java	/nursingList	POST	/getPatientVitalsByDateRange	0	public List<PatientVitals> getPatientVitalsByDateRange(@BodyParam("body") PatientVitalDto body)
nurse\NursingServiceImpl.java	/nursingList	POST	/getPatientVitalsByVitalIen	0	public List<PatientVitals> getPatientVitalsByVitalIen(@BodyParam("body") PatientVitalDto body)
nurse\NursingServiceImpl.java	/nursingList	POST	/getPMRReport	1	public Map<String, Object> getPMRReport(@BodyParam("body") PatientNote body, @QueryParam("isPrint") Boolean isPrint, @QueryParam("isPMR") Boolean isPMR)
nurse\NursingServiceImpl.java	/nursingList	POST	/getPreviousPatientVisitsAndAdmissions	0	public List<PatientVisitAdmissionDto> getPreviousPatientVisitsAndAdmissions(
nurse\NursingServiceImpl.java	/nursingList	POST	/getProcedureDetails	0	public Map<String, Object> getProcedureDetails(@BodyParam("body") PatientLabOrderDto body)
nurse\NursingServiceImpl.java	/nursingList	POST	/getRadOrderDetails	1	public List<RadOrderBookingListForWorkOrderResultDto_New> getRadOrderDetails(
nurse\NursingServiceImpl.java	/nursingList	POST	/getRoleNursePatientList	0	public List<PatientListForNurseRoleResultDto> getRoleNursePatientList(
nurse\NursingServiceImpl.java	/nursingList	POST	/getStatusWisePatientComplaintList	0	public List<PatientComplaints> getStatusWisePatientComplaintListApi(@BodyParam("body") PatientComplaints patientComplaints) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	POST	/getTempOrderDetailsOfPatient	0	public List<GetChargesResultDto> findTempCurrentCharges(BillChargeDto body) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	POST	/getTempPrescriptionNote	1	public List<TempPrescriptionNote> getTempPrescriptionNote(@QueryParam("page") Integer page, @QueryParam("count") Integer count,
nurse\NursingServiceImpl.java	/nursingList	POST	/getVitalUnitsByVital	1	public VitalUnitDetailsDto getVitalUnitsByVital(@BodyParam("body") VitalUnitDetailsDto vitalUnitDetailsDto)
nurse\NursingServiceImpl.java	/nursingList	POST	/getVitalUnitsByVitalArrayList	1	public List<VitalUnitDetailsDto> getVitalUnitsByVitalArrayList(
nurse\NursingServiceImpl.java	/nursingList	POST	/getWardListByspecializationForWardIncharge	0	public Map<String, List<NurseWardSpecializationListResultDto>> getWardListByspecializationForWardIncarge(
nurse\NursingServiceImpl.java	/nursingList	POST	/labTestReport	0	public NursingReportView labTestReport(@QueryParam("orderId") Long orderId, @QueryParam("opdIpd") Long opdIpd)
nurse\NursingServiceImpl.java	/nursingList	POST	/nurseClinicAllocation	0	public NurseClinicAllocationDetailsDto nurseClinicAllocation(
nurse\NursingServiceImpl.java	/nursingList	POST	/nurseDepartmentAllocation	0	public NurseClinicAllocationDetailsDto nurseDepartmentAllocation(
nurse\NursingServiceImpl.java	/nursingList	POST	/nurseWardAllocation	1	public NurseWardAllocationDetailsDto nurseDoctorDeAllocation(
nurse\NursingServiceImpl.java	/nursingList	POST	/patientCheckout	0	public Boolean patientCheckout(@BodyParam("body") PatientRegAdmissionDto body) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	POST	/patientDetailsByNewMRN	1	private GetPatientResultDto_1 getPatientDetailsByMRN(@BodyParam("body") GetPatientDto body,
nurse\NursingServiceImpl.java	/nursingList	POST	/patientDoctorAllocation	0	public NursePatientDetails patientDoctorAllocation(@BodyParam("body") NursePatientDetails body)
nurse\NursingServiceImpl.java	/nursingList	POST	/printPatientNote	1	public NursingReportView printPatientNote(@BodyParam("body") PatientNote patientNoteObj)
nurse\NursingServiceImpl.java	/nursingList	POST	/printRabiesReport	0	public String printRabiesReport(@BodyParam("patientNote") PatientNote body)
nurse\NursingServiceImpl.java	/nursingList	POST	/printSimplifiedQuickOrder	0	public String printSimplifiedQuickOrder(@BodyParam("body") SimplifiedQuickOrderDto body) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	POST	/releaseBodyFromBox	1	public boolean releaseBodyFromBox(@BodyParam("body") Mortuary body) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	POST	/saveAndDownloadDeathForm	0	public PatientNote saveAndDownloadDeathForm(@BodyParam("body") PatientNote body, @QueryParam("isFinalDeathForm") Boolean isFinalDeathForm)
nurse\NursingServiceImpl.java	/nursingList	POST	/saveAndPrintPMRReport	1	public PatientNote saveAndPrintPMRReport(@BodyParam("body") PatientNote body, @QueryParam("isPrint") Boolean isPrint,
nurse\NursingServiceImpl.java	/nursingList	POST	/saveAnnotatedImage	0	public ImageAnnotationDto saveAnnotatedImage(@BodyParam("body") ImageAnnotationDto body) 
nurse\NursingServiceImpl.java	/nursingList	POST	/saveBabiesHealthFactors	0	public List<PatientHealthFactor> saveBabiesHealthFactors(@BodyParam("body") List<PatientHealthFactor> body)
nurse\NursingServiceImpl.java	/nursingList	POST	/saveMarkAsExpired	0	public boolean saveMarkAsExpired(@BodyParam("body") PatientTransferDischargeView body)
nurse\NursingServiceImpl.java	/nursingList	POST	/saveMortuaryPatient	0	public boolean saveMortuaryPatient(@BodyParam("body") Mortuary body) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	POST	/saveOrUpdateFpvChecklistDetails	0	public boolean saveOrUpdateFpvChecklistDetails(
nurse\NursingServiceImpl.java	/nursingList	POST	/saveOrUpdatePatientMlcDetails	0	public MLCDetails saveOrUpdatePatientMlcDetails(@BodyParam("body") MLCDetails mlcDetails)
nurse\NursingServiceImpl.java	/nursingList	POST	/savePatientAdvice	1	public List<Patientadvice> createPatientAdvice(@BodyParam("body") List<Patientadvice> body)
nurse\NursingServiceImpl.java	/nursingList	POST	/savePatientAllergies	0	public PatientAllergies createPatientAllergies(@BodyParam("body") PatientAllergies body)
nurse\NursingServiceImpl.java	/nursingList	POST	/savePatientComplaints	1	public PatientComplaints createPatientComplaints(@BodyParam("body") PatientComplaints body) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	POST	/savePatientDiagnosis	1	public PatientDiagnosisDetails createPatientDiagnosis(@BodyParam("body") PatientDiagnosisDetails body)
nurse\NursingServiceImpl.java	/nursingList	POST	/savePatientFlagDetails	0	public Boolean createPatientFlagDetails(@BodyParam("body") List<PatientFlagDetails> body)
nurse\NursingServiceImpl.java	/nursingList	POST	/savePatientImmunizationDetails	0	public boolean savePatientImmunizationDetails(
nurse\NursingServiceImpl.java	/nursingList	POST	/savePatientNoteHealthFactors	1	public Boolean savePatientHealthFactors(@BodyParam("body") List<PatientHealthFactor> body)
nurse\NursingServiceImpl.java	/nursingList	POST	/savePatientNursingOrders	0	public PatientNursingOrder savePatientNursingOrders(@BodyParam("body") PatientNursingOrder patientNursingOrder)
nurse\NursingServiceImpl.java	/nursingList	POST	/savePatientStatus	1	public Boolean savePatientStatus(@BodyParam("body") List<PatientHealthFactor> body)
nurse\NursingServiceImpl.java	/nursingList	POST	/savePatientVerificationDetails	1	public Boolean savePatientVerificationDetails(@BodyParam("body") MortuaryVerifications body) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	POST	/savePatientVitals	0	public Boolean createPatientVitals(@BodyParam("body") List<PatientVitals> body) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	POST	/saveTransferRequest	0	public Boolean saveTransferRequest(@BodyParam("body") TransferRequest transferRequest)
nurse\NursingServiceImpl.java	/nursingList	POST	/saveVitalDisplayUnit	1	public VitalUnitDetailsDto saveVitalDisplayUnit(@BodyParam("body") VitalUnitDetailsDto vitalUnitDetailsDto)
nurse\NursingServiceImpl.java	/nursingList	POST	/saveVitalUnits	0	public VitalUnitDetailsDto vitalUnitDetails(@BodyParam("body") VitalUnitDetailsDto vitalUnitDetailsDto)
nurse\NursingServiceImpl.java	/nursingList	POST	/setPrefLangForPatient	0	public void setPrefLangForPatient(@QueryParam("patientId") Long patientId, @QueryParam("langId") Long langId)
nurse\NursingServiceImpl.java	/nursingList	POST	/signPatientNote	1	public Boolean signPatientNoteApi(@BodyParam("body") PatientClinicalRecordDTO body) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	POST	/updateCallRecordsDetails	0	public boolean updateCallRecordsDetails(@QueryParam("callId") Long callId, @QueryParam("duration") Long duration)
nurse\NursingServiceImpl.java	/nursingList	POST	/updateCarePlanDetails	1	public CarePlanDetailsDto updateCarePlanDetails(@BodyParam("body") CarePlanDetailsDto body)
nurse\NursingServiceImpl.java	/nursingList	POST	/updateConsentFilled	0	public void updateConsentFilled(@QueryParam("patientId") Long patientId,
nurse\NursingServiceImpl.java	/nursingList	POST	/updateCoversheetAudit	1	public Boolean updateCoversheetAudit(@BodyParam("body") AppointmentMrNoListResultDto body)
nurse\NursingServiceImpl.java	/nursingList	POST	/updateMedicines	1	public List<UpdatePrescriptionEntity> updateMedicines(
nurse\NursingServiceImpl.java	/nursingList	POST	/updateNurseAssistment	1	public void updateNurseAssistment(@QueryParam("patientId") @StoredProcNamedParam("p_visitid") Long visitId)
nurse\NursingServiceImpl.java	/nursingList	POST	/updateNursingOrderTask	1	public Long updateNursingOrderTask(@BodyParam("body") NurseTaskDetails details) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	POST	/updateOrderSet	0	public void updateOrderSet(@BodyParam("body") Orderset body)
nurse\NursingServiceImpl.java	/nursingList	POST	/updatePatientAcceptance	1	public Boolean updatePatientAcceptance(@BodyParam("body") Mortuary body)throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	POST	/updatePatientComplaints	1	public List<PatientComplaints> createPatientComplaints(@BodyParam("body") List<PatientComplaints> body) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	POST	/updatePatientDiagnosis	1	public void updatePatientDiagnosis(@BodyParam("body") List<PatientDiagnosisDetails> body)
nurse\NursingServiceImpl.java	/nursingList	POST	/updatePatientForFpv	1	public void updatePatientForFpv(@BodyParam("body") PatientAppointmentandRegDto body)
nurse\NursingServiceImpl.java	/nursingList	POST	/updateRabiesVaccine	1	public String updateRabiesVaccine(@BodyParam("patientNote") PatientNote body, @QueryParam("selectedDoseDate") String selectedDoseDate, @QueryParam("nextDoseDate") String nextDoseDa
nurse\NursingServiceImpl.java	/nursingList	POST	/vacatBedForExpiredPatient	1	public void vacatBedForExpiredPatient(@BodyParam("body") PatientTransferDischargeView body)
nurse\NursingServiceImpl.java	/nursingList	PUT	/deletePlanDetailsById	1	public void deletePlanDetailsById(@QueryParam("planId") Long planId, @QueryParam("masterName") String masterName,
nurse\NursingServiceImpl.java	/nursingList	PUT	/nurseClinicDeAllocation	0	public Boolean nurseClinicDeAllocation(@QueryParam("nurseClinicAllocationId") Long nurseClinicAllocationId)
nurse\NursingServiceImpl.java	/nursingList	PUT	/nurseDepartmentDeAllocation	1	public Boolean nurseDepartmentDeAllocation(
nurse\NursingServiceImpl.java	/nursingList	PUT	/nurseDoctorDeAllocation	0	public Boolean nurseDoctorDeAllocation(@QueryParam("doctorNurseAllocationId") Long doctorNurseAllocationId)
nurse\NursingServiceImpl.java	/nursingList	PUT	/nurseDoctorDeAllocationByDoctorAndNurse	1	public Boolean nurseDoctorDeAllocation(@QueryParam("doctorId") Long doctorId, @QueryParam("nurseId") Long nurseId)
nurse\NursingServiceImpl.java	/nursingList	PUT	/nurseWardDeAllocation	0	public Boolean nurseWardDeAllocation(@QueryParam("nurseWardAllocationId") Long nurseWardAllocationId)
nurse\NursingServiceImpl.java	/nursingList	PUT	/savePatientEDOD	0	public Timestamp savePatientEDOD(@QueryParam("patientEDOD") String patientEDOD,
nurse\NursingServiceImpl.java	/nursingList	PUT	/updatemoleculeMaster	1	public void updatemoleculeMaster(@BodyParam("body") Molecule body) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	PUT	/updatePatientFpvDetails	1	public void updatePatientFpvDetails(@QueryParam("patientId") Long patientId) throws GenericApplicationException 
nurse\NursingServiceImpl.java	/nursingList	PUT	/updateTempPrescriptionNote/{id}	0	public TempPrescriptionNote approveRejectTempPrescriptionNote(@PathParam("id") Long id) throws GenericApplicationException, UnsupportedEncodingException 
pas\AppointmentServiceImpl.java	/appointment	GET	/buildingListForQueue	0	public List<Buildingmaster> getBuildingListForQueue(@QueryParam("deptId") Long departmentId) throws GenericApplicationException 
pas\AppointmentServiceImpl.java	/appointment	GET	/cabinListForQueue	0	public List<Cabinmaster> getCabinListForQueueByDepartementId(@QueryParam("departmentId") Long departmentId) throws GenericApplicationException 
pas\AppointmentServiceImpl.java	/appointment	GET	/cabinQueueByDisplaUnit	0	public Map<String, Encounter> getUnitWiseCabinMaster(@QueryParam("displayUnitId") Long displayUnitId, @QueryParam("unitId") Long unitId)
pas\AppointmentServiceImpl.java	/appointment	GET	/cabinQueueListByDept	0	public Map<String, List<?>> getCabinQueueListByDept(@QueryParam("departmentId") Long departmentId, @QueryParam("cabinId") Long cabinId, @QueryParam("buildingId") Long buildingId, @
pas\AppointmentServiceImpl.java	/appointment	GET	/departmentBatchListForQueue	0	public List<DoctorBatchSlotDto> getDepartmentBatchListForQueue(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\AppointmentServiceImpl.java	/appointment	GET	/departmentListForQueue	0	public List<Departmentmaster> getDepartmentListForQueue() throws GenericApplicationException 
pas\AppointmentServiceImpl.java	/appointment	GET	/getCabinQueueListByDeptForDisplay	1	public Map<String, List<CabinQueueDto>> getCabinQueueListByDeptForDisplay(@QueryParam("departmentId") Long departmentId, @QueryParam("cabinId") Long cabinId, @QueryParam("buildingI
pas\AppointmentServiceImpl.java	/appointment	GET	/getLastSponsor	0	public SponsorByAdmissionIDResultDto getLastVisitSponsorByPatientId(@QueryParam("patientId") Long patientId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\AppointmentServiceImpl.java	/appointment	GET	/getLastSponsorByVisitId	0	public Object[] getLastSponsorByVisitId(
pas\AppointmentServiceImpl.java	/appointment	GET	/getPatientVistaDetails	0	public PatientVistaDetails getPatientVistaDetails(@QueryParam("patientId") Long patientId, @QueryParam("visitId") Long visitId) throws GenericApplicationException 
pas\AppointmentServiceImpl.java	/appointment	GET	/markPatientAsArrived	1	public void markPatientAsArrived(@QueryParam("visitId") Long visitId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\AppointmentServiceImpl.java	/appointment	GET	/printPatientAppointmentDetails	0	public PatientVisitHistoryView getPatientVisitDetails(@QueryParam("patientId") Long patientId) throws GenericApplicationException 
pas\AppointmentServiceImpl.java	/appointment	GET	/skipUnskipFromQueue	0	public void skipUnskipFromQueue(@QueryParam("id") Long visitId) throws GenericApplicationException 
pas\AppointmentServiceImpl.java	/appointment	POST	/getAppointmentListSummary	0	public AppointmentListSummary getAppointmentMrNoListSummaryNew(@BodyParam("body") AppointmentMrNoListDto body) throws GenericApplicationException 
pas\AppointmentServiceImpl.java	/appointment	POST	/getAppointmentMrNoList	0	public List<AppointmentMrNoListResultDto> getAppointmentMrNoListNew(@BodyParam("body") AppointmentMrNoListDto body, @QueryParam("page") Integer page, @QueryParam("count") Integer c
pas\AppointmentServiceImpl.java	/appointment	POST	/getAppointmentMrNoList/department/docotr	0	public List<AppointmentView> getAppointmentMrNoListWithDepartmentDoctor(@BodyParam("body") AppointmentMrNoListDto body) throws GenericApplicationException 
pas\AppointmentServiceImpl.java	/appointment	POST	/getAppointmentMrNoListDocit	0	private List<AppointmentView> getAppointmentMrNoListDocit_Nouse_Delete(@BodyParam("body") AppointmentMrNoListDto body, @QueryParam("token") String token) throws GenericApplicationE
pas\AppointmentServiceImpl.java	/appointment	POST	/getAppointmentMrNoListOld	0	public List<AppointmentView> getAppointmentMrNoListOld(@BodyParam("body") AppointmentMrNoListDto body) throws GenericApplicationException 
pas\AppointmentServiceImpl.java	/appointment	POST	/getAppointmentQueue	1	public List<AppointmentQueueResultDto> getAppointmentQueue(@BodyParam("body") AppointmentQueueDto body) throws GenericApplicationException 
pas\AppointmentServiceImpl.java	/appointment	POST	/getAppointmentVisitList	0	public List<AppointmentVisitView> getAppointmentVisitListMrNoList(@BodyParam("body") AppointmentMrNoListDto body) throws GenericApplicationException 
pas\AppointmentServiceImpl.java	/appointment	POST	/getCallPatientList	0	public
pas\AppointmentServiceImpl.java	/appointment	POST	/getPackageAppointmentMrNoList	0	public List<PackageAppointmentListResultDto> getPackageAppointmentMrNoList(@BodyParam("body") AppointmentMrNoListDto body) throws GenericApplicationException 
pas\AppointmentServiceImpl.java	/appointment	POST	/getPatientAllocationDetails	1	public List<PatientAllocationDetails> getPatientAllocationDetails(@QueryParam("patientId") Long patientId) throws GenericApplicationException, ParseException, UnsupportedEncodingEx
pas\AppointmentServiceImpl.java	/appointment	POST	/getPatientListForPackages	1	public List<AppointmentMrNoListResultDto> getPatientListForPackages(@BodyParam("body") AppointmentMrNoListDto body, @QueryParam("page") Integer page, @QueryParam("count") Integer c
pas\AppointmentServiceImpl.java	/appointment	POST	/printPatientIntialNote	0	public PrintPatientIntialAssessmentNoteDTO printPatientIntialNote(@BodyParam("body") PrintPatientIntialAssessmentNoteDTO body) throws GenericApplicationException 
pas\AppointmentServiceImpl.java	/appointment	POST	/printPatientNote	0	public PrintPatientAssesmentNoteDto printPatientNote(@BodyParam("body") PrintPatientAssesmentNoteDto body) throws GenericApplicationException 
pas\AppointmentServiceImpl.java	/appointment	POST	/updateDoctorForAppointment	0	public void updateDoctorForAppointment(@BodyParam("body") NurseClinicAllocationDetailsDto nurseClinicAllocationDetailsDto) throws GenericApplicationException, ParseException 
pas\AppointmentServiceImpl.java	/appointment	POST	/uploadPrescriptionFromOPList	0	public Boolean uploadPrescriptionFromOPList(@BodyParam("body") IDDocumentDto documentDto, @QueryParam("visitId") Long visitId, @QueryParam("patientId") Long patientId) throws Gener
pas\AppointmentServiceImpl.java	/appointment	PUT	/updateAppointmentIsEnabledFlag	0	public GenericResultDto updateAppointmentIsEnabledFlag(@BodyParam("body") AppointmentIsEnabledFlagDto body) throws GenericApplicationException 
pas\BedReservationServiceImpl.java	/bedreservation	GET	/getBedUnReservation	1	public
pas\BedReservationServiceImpl.java	/bedreservation	POST	/addBedReservation	1	public AddBedReservationDto addBedReservation(@BodyParam("body") BedReservationView body) throws GenericApplicationException 
pas\BedReservationServiceImpl.java	/bedreservation	POST	/cancelBedReservation	0	public void cancelBedReservation(@QueryParam("bedId") Long bedId, @QueryParam("patientId") Long patientId) throws GenericApplicationException 
pas\BedReservationServiceImpl.java	/bedreservation	POST	/getIPDBedReservationList	0	public List<IPDBedReservationListDto> getIPDBedReservationList(@BodyParam("body") IPDBedReservationListDto body) throws GenericApplicationException 
pas\BedServiceImpl.java	/bed	GET	/getWardByClassID	0	public List<WardByClassIDResultDto> getWardByClassID(@QueryParam("classId") @StoredProcNamedParam("p_classid") Long classId, @QueryParam("unitId") @StoredProcNamedParam("p_unitid")
pas\BedServiceImpl.java	/bed	POST	/getIPDDischargeBedList	0	public List<IPDDischargeBedListResultDto> getIPDDischargeBedList(@BodyParam("body") IPDDischargeBedListDto body) throws GenericApplicationException 
pas\BillServiceImpl.java	/bill	GET	/checkToPrintNewPass	0	public AttendantPassDetailsDto checkToPrintNewPass(@QueryParam("admissionID") Long admissionID, @QueryParam("mrno") String mrno) throws GenericApplicationException 
pas\BillServiceImpl.java	/bill	GET	/getAllEligiblePassesForReturn	1	public List<AttendantPassDetailsDto> getAllEligiblePassesForReturn(@QueryParam("admissionID") Long admissionID,@QueryParam("patientID") Long patientID, @QueryParam("mrno") String m
pas\BillServiceImpl.java	/bill	GET	/getBaseHealthPlanOrBasePackageByType	1	public List<Servicemaster> getBaseHealthPlanOrBasePackageByType(@QueryParam("isPackage") Boolean isPackage, @QueryParam("isHealthPlan") Boolean isHealthPlan) throws GenericApplicat
pas\BillServiceImpl.java	/bill	GET	/getGatePassChargesBillId	0	public Long getGatePassChargesForCreditNote(@QueryParam("admissionId") Long admissionId, @QueryParam("passNo") String passNo, @QueryParam("isRefund") Boolean isRefund) throws Gener
pas\BillServiceImpl.java	/bill	GET	/getIssuedPassData	0	public AttendantPassDetailsDto getIssuedPassData(@QueryParam("admissionID") Long admissionID, @QueryParam("mrno") String mrno, @QueryParam("passNo") String passNo, @QueryParam("reP
pas\BillServiceImpl.java	/bill	GET	/getOrderSetById/{id}	1	public Ordersetdefinationmaster getProcedureById(@PathParam("id") Long id) throws GenericApplicationException 
pas\BillServiceImpl.java	/bill	GET	/listAllAttendantPasses	1	public List<AttendantPassDetailsDto> listAllAttendantPasses(@QueryParam("admissionID") Long admissionID, @QueryParam("mrno") String mrno) 
pas\BillServiceImpl.java	/bill	GET	/searchItemsBySpecializationId	0	public List<Itemmaster> searchItemsBySpecializationId(@QueryParam("specializationId") Long specializationId) throws GenericApplicationException 
pas\BillServiceImpl.java	/bill	GET	/searchServicesBySpecializationAndSubspecialization	0	public List<Servicemaster> searchServicesBySpecializationAndSubspecialization(@QueryParam("specializationId") Long specializationId, @QueryParam("subSpecializationId") Long subSpec
pas\BillServiceImpl.java	/bill	GET	/searchServicesBySpecializationId	0	public List<Servicemaster> searchServicesBySpecializationId(@QueryParam("specializationId") Long specializationId) throws GenericApplicationException 
pas\BillServiceImpl.java	/bill	GET	/validateGatePassNo	1	public Boolean validateGatePassNo(@QueryParam("admissionId") Long admissionId, @QueryParam("passId") Long passId, @QueryParam("gatePassNo") String gatePassNo) throws GenericApplica
pas\BillServiceImpl.java	/bill	POST	/addAndUpdateOrderSetMaster	1	public void addAndUpdateOrderSetMaster(@BodyParam("body") Ordersetdefinationmaster body) throws GenericApplicationException 
pas\BillServiceImpl.java	/bill	POST	/addServicesForPackage	0	public List<Servicepackagemaster> settledBillCancellation(@BodyParam("body") List<Servicepackagemaster> body) throws GenericApplicationException 
pas\BillServiceImpl.java	/bill	POST	/addTariffPackageHealthPlan	1	public Tariffpackagehealthplandefinationmaster settledBillCancellation(@BodyParam("body") Tariffpackagehealthplandefinationmaster body) throws GenericApplicationException 
pas\BillServiceImpl.java	/bill	POST	/generateNewAttendantPass	0	public AttendantPassDetails generateNewAttendantPass(AttendantPassDetailsDto body) throws GenericApplicationException 
pas\BillServiceImpl.java	/bill	POST	/settledBillCancellation	1	public List<TledBillCancellationResultDto> settledBillCancellation(@BodyParam("body") TledBillCancellationDto body) throws GenericApplicationException 
pas\BillServiceImpl.java	/bill	POST	/unsettledBillCancellation	0	public List<LedBillCancellationDto> unsettledBillCancellation(@BodyParam("body") LedBillCancellationDto body) throws GenericApplicationException 
pas\BillServiceImpl.java	/bill	POST	/updateReprintAttendantPassStatus	0	public boolean requestReprintAttendantPass(@QueryParam("attendantPassId") Long attendantPassId, @QueryParam("reprintStatus") Long reprintStatus) throws GenericApplicationException 
pas\BillServiceImpl.java	/bill	PUT	/deleteOrderSetById/{id}	0	public void deleteOrderSetById(@PathParam("id") Long id) throws GenericApplicationException 
pas\BillServiceImpl.java	/bill	PUT	/submitPass	0	public void submitPass(AttendantPassDetailsDto body) 
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/acceptBloodBag	1	public boolean acceptBloodBag(@QueryParam("returnId") Long returnId,
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/authenticateStaffAndUpdateThePatientBloodGroup	1	public BloodBagExtraction athunticateStaffAndUpdateThePatientBloodGroup(@QueryParam("userId") Long userId,@QueryParam("bloodGroupId") Long bloodGroupId,
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/cancelBloodBagComponent	0	public void cancelBloodBagComponent(@QueryParam("extractedBloodBagNo") String extractedBloodBagNo, @QueryParam("componentId") Long componentId) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/cancelDonor	1	public BloodDonorRegistration cancelDonor(@QueryParam("donorId") Long id, @QueryParam("unitId") Long unitid,
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/cancelRequisition	1	public PatientBloodRequisition cancelRequisition(@QueryParam("reqId") Long reqId, @QueryParam("unitId") Long unitid, @QueryParam("reasonId") Long reasonId) throws GenericApplicatio
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/cancelRequisitionDetails	1	public PatientBloodRequisitionDetails cancelRequisitionDetails(@QueryParam("reqDetailsId") Long reqDetailsId, @QueryParam("reqId") Long reqId,
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/checkBagIsCrossmatched	1	public boolean checkBagIsCrossmatched(@QueryParam("bagNo") String bagNo, @QueryParam("component") String component, @QueryParam("unitid") Long unitid) throws GenericApplicationExce
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/checkBloodBankServicesConfigurationStatus	1	public boolean checkBloodBankServicesConfigurationStatus(@QueryParam("unitId") Long unitId, @QueryParam("isDonorServiceCheck") boolean isDonorServiceCheck) throws GenericApplicati
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/checkComponentStock	0	public Number getBloodStockListBycomponentId(@QueryParam("componentId") Long componentId, @QueryParam("bloodGroup") String bloodGroup,
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/checkComponentStockInBloodBank	1	public Number checkComponentStockInBloodBank(@QueryParam("bloodGroup") Long bloodGroup,@QueryParam("componentId") Long componentId,@QueryParam("unitId") Long unitId,@QueryParam("st
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/checkTheBagExistenceInStock	0	public Long checkTheBagExistenceInStock(@QueryParam("bagNo") String bagNo,@DefaultValue("false") @QueryParam("isMedExam") boolean isMedExam, @QueryParam("component") String compone
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/checkTheComponentBagChargeService	0	public Bloodcomponentmaster checkTheComponentBagChargeService(@QueryParam("componentId") Long componentId) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/checkTheSampleExistence	1	public boolean checkTheSampleExistence(@QueryParam("sampleNo") String sampleNo) throws GenericApplicationException, UnsupportedEncodingException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/checkTheTubeNo	1	public boolean checkTheTubeNo(@QueryParam("tubeNo") String tubeNo, @QueryParam("isMedicalExamination") boolean isMedicalExamination) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/checkWBComponentMappingStatus	0	public boolean checkWBComponentMappingStatus(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/discardBloodBag	0	public boolean discardBloodBag(@QueryParam("id") Long id, @QueryParam("unitId") Long unitId, @QueryParam("reasonId") Long reasonId, @QueryParam("reasonText") String reasonText,@Qu
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/discardBloodBagFromStock	1	public boolean discardBloodBagFromStock(@QueryParam("bagId") Long bagId, @QueryParam("reasonId") String reasonId,@QueryParam("reasonText") String reasonText) throws GenericApplica
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/eraktkoshdataupdateservice	0	public Map<String,List<HashMap<String,String>>> eraktkoshdataupdateservice(@QueryParam("hospCode") String hospCode,@QueryParam("key") String key) throws GenericApplicationException
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getBagDetails	1	public List<BloodBagExtraction> getBagDetails(@QueryParam("bagNo") String bloodBagNo) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getBagListForIssue	0	public List<BloodBagCollectionDto> getBagListForIssue(@QueryParam("code") String code, @QueryParam("component") Long component, @QueryParam("storeId") Long storeId) throws Generic
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getBloodBagStockCountByBloodBankId	0	public Map<Long, Map<Long, Map<String, Long>>> getBloodstockDtoCountByBloodBankIdNew(@QueryParam("districtId") Long districtId, @QueryParam("bloodBankId") Long storeId, @QueryPara
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getBloodbankReorderList	0	public List<CurrentBloodBankReoderResultDto> getBloodbankReorderList(@QueryParam("unitId") @StoredProcNamedParam("p_unitid") Long unitId,
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getBloodRequisitionDetailsForPatient	0	public List<PatientRequsitionListResultDto> getBloodRequisitionDetailsForPatient(@QueryParam("mrNo") String mrNo)
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getBloodstockDtoCountByBloodBankId	0	public Map<Long,Map<Long,Map<String,Long>>> getBloodstockDtoCountByBloodBankId(@QueryParam("districtId") Long districtId,@QueryParam("bloodBankId") Long storeId,@QueryParam("unitId
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getBloodStockListBycomponentId	0	public List<GetBloodStockListBycomponentIdDto> getBloodStockListBycomponentId(@QueryParam("id") @StoredProcNamedParam("p_id") Long id,
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getCrossMarchTestDetailsForIssueLabelPrint	0	public List<BloodCrossMatchingTestDetailsDto> getCrossMarchTestDetailsForIssueLabelPrint(@QueryParam("reqDetailsId") Long reqDetailsId,
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getCrossMatchingDetails	0	public List<BloodCrossMatchingTestDetailsDto> getCrossMatchingDetails(@QueryParam("requisitionDetailsId") Long requisitionDetailsId,
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getDemandSlipDetails	1	public List<DemandSlip> getDemandSlipDetails(@QueryParam("requisitionId") Long reqId, @QueryParam("requisitionUnitId") Long reqUnitId,@QueryParam("reqDetailsUnitId") Long reqDetail
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getDistrictListWiseComponent	0	public List<DistrictMaster> getDistrictListWiseComponent(@QueryParam("componentId") Long componentId, @QueryParam("bloodGroupId") Long bloodGroupId, @QueryParam("quantity") Long qu
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getDonorDefaultVitalValues	1	public DonorDefaultVitalValue getDonorDefaultVitalValues(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getDonorDetailsByBloodBagNo	1	public List<DonorDetailResultByBloodBagNoDto> getDonorDetailsByBloodBagNo(@BodyParam("body") DonorDetaiByBloodBagNoDto body) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getDonorDetailsByDonorNo	0	public List<DonorDetails> getDonorDetailsByDonorNo(@QueryParam("mrNo") String donorNo,@QueryParam("patientId") Long patientId) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getDonorDetailsForCrossMatching	0	public DonorDetailResultDto getDonorDetailsForCrossMatching(@QueryParam("id") String extractionId, @QueryParam("unitId") String unitId) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getDonorList	1	public List<DonorDetails> getDonorList(@QueryParam("donationType") Long donationType,@QueryParam("firstName") String firstName,@QueryParam("middleName") String middleName,@QueryPar
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getDonorMedicalExamResult	0	public List<DonorMedicalExamDetailsResultDto> getDonorMedicalExamResult(@QueryParam("donorId") Long donorId,@QueryParam("donorUnitid") Long unitId) throws GenericApplicationExcepti
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getDonorRegistrationDetails	1	public BloodDonorRegistration getDonorRegistrationDetails(@QueryParam("donorId") @StoredProcNamedParam("P_donorId") Long donorId,@QueryParam("donorUnitId") @StoredProcNamedParam("
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getDonorRegistrationDetailsByMRNo	1	public BloodDonorRegistration getDonorRegistrationDetailsByMRNo(@QueryParam("mrNo") String mrNo, @QueryParam("firstName") String firstName, @QueryParam("lastName") String lastName,
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getDonorTestServicesStatus	0	public GetOrderIdbyPatientIdResultDto getDonorTestServicesStatus(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getHospitalWiseComponent	0	public List<RaiseBloodBankIndentDto> getHospitalWiseComponent(@QueryParam("componentId") Long componentId, @QueryParam("bloodGroupId") Long bloodGroupId,
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getIssuedBagDetailsForLabelPrint	0	public List<BloodCrossMatchingTestDetailsDto> getIssuedBagDetailsForLabelPrint(@QueryParam("requisitionDetailsId") Long requisitionDetailsId, @QueryParam("requisitionDetailsUnitId"
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getPatientBloodgroup	0	public Number getPatientBloodgroup(@QueryParam("patientId") Long patientId) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getPatientBloodSampleDetails	1	public List<PatientBloodSampleDetailsDto> getPatientBloodSampleDetails(@QueryParam("sampleNo") @StoredProcNamedParam("p_sampleNo") String sampleNo) throws GenericApplicationExcepti
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getPatientDetailsBySampleNo	0	public List<PatientBloodRequisition> getPatientDetailsBySampleNo(@QueryParam("sampleNo") String sampleNo) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getPatientPreviousTransfusionHistory	1	public List<BloodBagReceive> getPatientPreviousTransfusionHistory(@QueryParam("mrNo") String mrNo, @QueryParam("patientId") Long patientId) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getPatientRequisitionAndCrossMatchResultDetails	1	public List<PatientBloodRequisitionReportResultDto> getPatientRequisitionDetails(@QueryParam("patientId") @StoredProcNamedParam("p_patientId") Long patientId, @QueryParam("mrnno"
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getRequisitionDetailsById	0	public PatientBloodRequisition getRequisitionDetailsById(@QueryParam("requisitionId") Long reqId, @QueryParam("requisitionUnitId") Long reqUnitId) throws GenericApplicationExceptio
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getRequisitionDetailsForReplacementHistory	0	public List<PatientBloodRequisitionDetails> getRequisitionDetailsForReplacementHistory(@QueryParam("reqId") Long reqId, @QueryParam("reqDetailsId") Long reqDetailsId, @QueryParam("
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getRequisitionListByMRNo	0	public List<PatientBloodRequisition> getRequisitionListByMRNo(@QueryParam("mrNo") String mrNo)
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getReservedBagDetailsForAutologousTypeOrder	0	public Number getReservedBagDetailsForAutologousTypeOrder(@QueryParam("requisitionDetailsId") Long requisitionDetailsId,
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getReturnedBagDetails	0	public BloodBagExtraction getReturnedBagDetails(@QueryParam("bagReturnId") Long bagReturnId,
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getTheBloodBankExistedDistricts	0	public DonorRegistrationView getTheBloodBankExistedDistricts(@QueryParam("isDistrictSearch") boolean isDistrictSearch,
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getUnitListWiseComponent	0	public List<Unitmaster> getUnitListWiseComponent(@QueryParam("componentId") Long componentId,
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getUnitSpecificBloodStores	0	public List<Store> getUnitStoreListNewIndent(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/getUserDetailsList	0	public List<TUserdetails> getUserDetailsList(@QueryParam("isBloodBagDecross") boolean isBloodBagDecross) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/printBloodBagCrossMatchReport	1	public String printBloodBagCrossMatchReport(@QueryParam("requisitionDetailsId") Long requisitionDetailsId, @QueryParam("requisitionDetailsUnitId") Long requisitionDetailsUnitId)
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/searchBagInTheStock	0	public List<BloodExtractionResultDto> searchBagInTheStock(@QueryParam("code") String code, @QueryParam("componentId") String componentId, @QueryParam("bloodGroupId") String bloodG
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/searchBloodBagListWithCode	0	public List<BloodExtractionResultDto> searchBloodBagListWithCode(@QueryParam("code") String code, @QueryParam("storeId") String storeId) throws GenericApplicationException
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/sellBloodBagFromStock	0	public boolean sellBloodBagFromStock(@QueryParam("bagId") String bagIdList, @QueryParam("reasonId") String reasonId) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	GET	/TransferRequisition	0	public PatientBloodRequisitionDetails transferRequisitionToOtherStore(@QueryParam("reqDetailsId") Long reqDetailsId, @QueryParam("reqId") Long reqId,
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/addBagReturn	0	public BloodBagReturn addBagReturn(@BodyParam("body") List<BloodBagReturn> body) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/addBloodBagCollection	0	public BloodDonorBagCollectionDto addUpdateBloodsDonorBagCollection(@BodyParam("body") BloodBagCollectionDto body) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/addBloodBankIndent	0	public BloodBankIndent addBloodBankIndent(@BodyParam("body") BloodBankIndent body) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/addBloodComponants	0	public List<BloodBagExtraction> updateBloodBagCompoOpeningStock(@BodyParam("body") BloodBagExtractionView body) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/addBloodComponantsDetails	0	public BloodBagExtraction updateBloodBagCompoOpeningStocks(@BodyParam("body") BloodBagCollectionDto body) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/addBloodGroup	1	public void addBloodGroup(@BodyParam("body") Bloodgroupcrossmatching body) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/addOpeningStockToBloodBankStock	0	public List<BloodBagExtraction> addOpeningStockToBloodBankStock(@BodyParam("body") BloodBagExtractionView body) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/addUpdateBloodDonorRegistration	1	public BloodDonorRegistration addUpdateBloodDonorRegistration(@BodyParam("body") DonorRegistrationView body,
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/athunticateStaffAndDecrossTheBloodBag	0	public BloodBagDecross athunticateStaffAndDecrossTheBloodBag(@BodyParam("body") BloodBagDecross body) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/bloodbagcomponentcheck	1	public List<Bloodbagtypecomponentmaster> bloodbagcomponentcheck(@BodyParam("body") Bloodbagtypecomponentmaster body) throws GenericApplicationException
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/bloodcomponentcliniccheck	1	public List<BloodComponentClinic> bloodcomponentcliniccheck(@BodyParam("body") BloodComponentClinic body) throws GenericApplicationException
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/cancelIndent	1	public boolean cancelIndent(@QueryParam("id") Long id) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/generateDemandSlip	0	public String[] generateDemandSlip(@BodyParam("body") DemandSlip body) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/getBagsReturnList	0	public List<ReturnedBagDetailsResultDto> getBagsReturnList(@BodyParam("body") BagsReturnListSearchView body, @QueryParam("maximumRows") Long maxRows,
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/getBloodBagTrail/{id}	0	public List<Map<String,Object>> getBloodBagTrail(@QueryParam("bagStatus") String bagStatus, @QueryParam("bagName") String bagName, @PathParam("id") Long id) throws GenericApplicati
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/getBloodBankIndentListByUnit	1	public List<BloodBankIndent> getBloodBankIndentListByUnit(@BodyParam("body") BagsReturnListSearchView body, @QueryParam("id") Long id, @QueryParam("unitId") Long unitId, @QueryPara
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/getBloodDiscardedList	1	public List<DiscardedBagsDetailsResultDto> getBloodDiscardedList(@BodyParam("body") DiscradBagSearchDto body,@QueryParam("maximumRows") Long maxRows,
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/getBloodExtractionList	1	public List<BloodExtractionResultDto> getBloodExtractionList(@BodyParam("body") BloodExtractionDto body) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/getBloodOpeningStockDetails	0	public List<BloodExtractionResultDto> getBloodOpeningStockDetails(@BodyParam("body") IndentListByStoreIdDto body, @QueryParam("page") Integer page,
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/getCrossMatchedBagList	0	public List<CrossMatchedBagDetailsResultDto> getCrossMatchedBagList(@BodyParam("body") BagsReturnListSearchView body, @QueryParam("maximumRows") Long maxRows,
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/getCrossMatchingTestResults	1	public List<PatientRequsitionListResultDto> getCrossMatchingTestResults(@BodyParam("body") CrossMatchingListSearchDto body, @QueryParam("maximumRows") Long maxRows,
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/getCurrentBloodStockListForExtraction	0	public List<BloodExtractionResultDto> getCurrentBloodStockListForExtraction(@BodyParam("body") BloodUnitStockListSearchDto body) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/getCurrentBloodStockListForExtractionCount	1	public List<BloodExtractionCountResultDto> getCurrentBloodStockListForExtractionCount(@BodyParam("body") BloodUnitStockListSearchDto body) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/getCurrentUnitBloodStockList	0	public BloodBagExtractionView getCategoryCountOfStockList(@BodyParam("body") BloodUnitStockListSearchDto body, @DefaultValue("false") @QueryParam("is_print") boolean print) 
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/getDoctorList	0	public List<DoctorDetails> getDoctorList(@BodyParam("body") DoctorDetails body) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/getDonorDetails	0	public List<DonorDetailResultDto> getDonorDetails(@BodyParam("body") DonorSearchView body, @QueryParam("maximumRows") Long maxRows,
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/getExternalPatientDetails	0	public List<SearchPatientGeneralDetailsResultDto> getExternalPatientDetails(@BodyParam("body") PatientGeneralDetailsListForSearchDto body, @QueryParam("is_admission") Boolean issAd
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/getPathTestResultBloodBank	0	public Map<String,Map<String,String>> getPathTestResultBloodBank(@BodyParam("body") PathOrderBookingListDto body) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/getPatientRequisitionListCount	0	public BloodBagExtractionView getPatientRequisitionListCount(@BodyParam("body") PatientRequsitionListDto body, @QueryParam("maximumRows") Long maxRows,
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/getPatientTestList	0	public List<GetOrderIdbyPatientIdResultDto> getPatientTestList(@BodyParam("body") PathOrderBookingListDto body) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/getReplacementDonorHistory	0	public List<ReplacementDonorHistoryDto> getReplacementDonorHistory(@BodyParam("body") PatientDetailsListForSearchDto body,
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/getUnitWiseAdmittedPatientSearch	0	public List<SearchPatientGeneralDetailsResultDto> getUnitWiseAdmittedPatientSearch(@BodyParam("body") SearchPatientAndAdmissionByNameMobNoDto body) throws GenericApplicationExcepti
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/getUnitWisePatientGeneralDetailsListForSearch	0	public List<SearchPatientGeneralDetailsResultDto> getUnitWisePatientGeneralDetailsListForSearch(@BodyParam("body") PatientGeneralDetailsListForSearchDto body, @QueryParam("is_admis
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/issueBagForExternalOrders	0	public void issueBagForExternalOrders(@BodyParam("body") BloodBagCollectionDto bloodBagCollectionDto,
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/issueBagToExternalHospitalAndStorageCenters	1	public BloodBagCollectionDto issueBagToExternalHospitalAndStorageCenters(@BodyParam("body") BloodBagCollectionDto bloodBagCollectionDto,
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/issueBloodBag	1	public BloodBagCollectionDto issueBloodBag(@BodyParam("body") BloodBagCollectionDto bloodBagCollectionDto,
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/issueIndent	0	public boolean issueIndent(@BodyParam("body") BloodBagCollectionDto body, @QueryParam("indentId") Long indentId, @QueryParam("issueQty") Long issueQty) throws GenericApplicationExc
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/pdfBloodDonationCertificate	1	public String[] pdfBloodDonationCertificate(@BodyParam("body") BloodDonorCertificate body) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/printBloodBankIndentDetails	1	public String[] printBloodBankIndentDetails(@QueryParam("id") Long id) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/receiveBloodBag	0	public void receiveBloodBag(@BodyParam("body") BloodBagReceive body) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/receiveIndent	1	public boolean receiveIndent(@BodyParam("body") List<BloodBagReceive> body, @QueryParam("indentId") Long indentId) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/recordAdverseReactionOfTheTransfusion	1	public BloodBagReceive recordAdverseReactionOfTheTransfusion(@BodyParam("body") BloodBagReceive body) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/rejectIndent	1	public boolean rejectIndent(@BodyParam("body") BloodBankIndent body) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/saveBloodCrossMatchingTestResult	0	public BloodCrossmatchingResult saveBloodCrossMatchingTestResult(@BodyParam("body") BloodCrossMatchingView body) throws GenericApplicationException
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/saveDonorMedicalExaminationResult	0	public DonorMedicalExamination saveOrUpdateDonorMedicalExaminationResult(@BodyParam("body") DonorMedicalExamination body, @QueryParam("isTestFinilized") boolean isTestFinilized) t
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/searchPatientList	1	public List<PatientSearchList> getPatientSearchList(@BodyParam("body") PatientSearchList body) throws GenericApplicationException
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/updateBagNo	1	public Boolean updateBagNo(@QueryParam("donorId") Long id, @QueryParam("unitId") Long unitid,
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/updateBagStatus	1	public BloodBagExtraction updateBagStatus(@BodyParam("body") BloodBagReceive body) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/updateIndentStoreandUnit	1	public boolean updateIndentStoreandUnit(@BodyParam("body") BloodBankIndent body) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/updateReturnedBagStatus	0	public boolean updateReturnedBagStatus(@BodyParam("body") BloodBagReturn body) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	POST	/updateSingleReturnedBloodBag	1	public boolean updateSingleReturnedBloodBag(@BodyParam("body") BloodBagReturn body) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	PUT	/updateBloodBagBloodGroup	0	public BloodBagExtraction updateBloodBagBloodGroup(@QueryParam("userId") Long userId, @QueryParam("password") String password, @QueryParam("bloodGroup") Long bloodGroup,@QueryParam
pas\BloodBankServiceImpl.java	/bloodtestmaster	PUT	/updatebloodgroupcross	1	public void updateBloodGroupCrossMatching(@BodyParam("body") Bloodgroupcrossmatching body) throws GenericApplicationException 
pas\BloodBankServiceImpl.java	/bloodtestmaster	PUT	/updateBloodSampleCollectionStatus	0	public void updateBloodSampleCollectionStatusApi(@QueryParam("orderId") Long orderId, @QueryParam("sampleNo") String sampleNo,
pas\ChargeServiceImpl.java	/charge	GET	/getTariffClassRateByClassID	0	public List<TariffClassRateByClassIDResultDto> getTariffClassRateByClassID(@QueryParam("tariffServiceId") @StoredProcNamedParam("p_tariffserviceid") Long tariffServiceId,
pas\ChargeServiceImpl.java	/charge	POST	/getCharges	0	public List<GetChargesResultDto> getCharges(@BodyParam("body") GetChargesDto body) throws GenericApplicationException 
pas\ChargeServiceImpl.java	/charge	POST	/getPackageServiceFromPackageID	0	public List<PackageServiceFromPackageIDResultDto> getPackageServiceFromPackageID(@BodyParam("body") PackageServiceFromPackageIDDto body,
pas\ChargeServiceImpl.java	/charge	POST	/getServicesByBillID	0	public List<ServicesByBillIDResultDto> findServicesByBillID(@BodyParam("body") ServicesByBillIDDto body) throws GenericApplicationException 
pas\CompanyApprovedAmountServiceImpl.java	/companyapprovedamount	DELETE	/{Id}	1	public void removeCorporateApprovedAmt(@PathParam("Id") Long Id) throws GenericApplicationException 
pas\CompanyApprovedAmountServiceImpl.java	/companyapprovedamount	GET	/checkAliasDiagnosisMasters	0	public List<Number> checkAliasDiagnosisMasters(@QueryParam("alias") String alias, @QueryParam("masterName") String masterName, @QueryParam("column") String column) throws GenericAp
pas\CompanyApprovedAmountServiceImpl.java	/companyapprovedamount	GET	/checkBedAndWardInMasters	0	public Number checkBedAndWardInMasters(@QueryParam("wardid") String wardid, @QueryParam("masterName") String masterName, @QueryParam("bedName") String bedName) throws GenericAppli
pas\CompanyApprovedAmountServiceImpl.java	/companyapprovedamount	GET	/checkCodeInMasters	1	public Number checkCodeInMasters(@QueryParam("code") String code, @QueryParam("masterName") String masterName, @QueryParam("column") String column, @QueryParam("id") Long id) throw
pas\CompanyApprovedAmountServiceImpl.java	/companyapprovedamount	GET	/checkDescriptionInMasters	0	public Number checkDescriptionInMasters(@QueryParam("description") String description, @QueryParam("masterName") String masterName, @QueryParam("column") String column, @QueryParam
pas\CompanyApprovedAmountServiceImpl.java	/companyapprovedamount	GET	/checkParticularForYear	0	public Number checkParticularForYear(@QueryParam("description") String description, @QueryParam("year") String year) throws GenericApplicationException 
pas\CompanyApprovedAmountServiceImpl.java	/companyapprovedamount	GET	/checkServiceCodeInMasters	0	public Number checkServiceCodeInMasters(@QueryParam("code") String code, @QueryParam("masterName") String masterName, @QueryParam("column") String column) throws GenericApplication
pas\CompanyApprovedAmountServiceImpl.java	/companyapprovedamount	GET	/checkTestPrintSequence	0	public Number checkTestPrintSequence(@QueryParam("sequenceId") String sequenceId, @QueryParam("column") String column, @QueryParam("masterName") String masterName) throws GenericAp
pas\CompanyApprovedAmountServiceImpl.java	/companyapprovedamount	GET	/checkUniqueIdInCadreDesignationMasters	0	public List<Object> checkUniqueIdInCadreDesignation(@QueryParam("cadreId") Long cadreId) throws GenericApplicationException 
pas\CompanyApprovedAmountServiceImpl.java	/companyapprovedamount	GET	/checkUniqueIdInGrievanceCategoryMaster	0	public List<Object> checkUniqueIdInGrievanceCategoryMaster(@QueryParam("categoryName") String categoryName) throws GenericApplicationException 
pas\CompanyApprovedAmountServiceImpl.java	/companyapprovedamount	GET	/checkUnitwiseDescriptionInMasters	0	public Number checkUnitwiseDescriptionInMasters(@QueryParam("description") String description, @QueryParam("masterName") String masterName, @QueryParam("column") String column, @Qu
pas\CompanyApprovedAmountServiceImpl.java	/companyapprovedamount	GET	/checkUnitwisePassIDInMasters	0	public Number checkUnitwisePassIDInMasters(@QueryParam("passID") Long passID, @QueryParam("id") Long id) throws GenericApplicationException 
pas\CompanyApprovedAmountServiceImpl.java	/companyapprovedamount	GET	/getPayerById/{id}	1	public Companymaster getPayerById(@PathParam("id") Long id) throws GenericApplicationException 
pas\CompanyApprovedAmountServiceImpl.java	/companyapprovedamount	POST	/addCompanyApprovedAmountDetails	0	public CompanyApprovedAmountDetailsDto addCompanyApprovedAmountDetails(@BodyParam("body") CompanyApprovedAmountDetailsDto body) throws GenericApplicationException 
pas\CompanyApprovedAmountServiceImpl.java	/companyapprovedamount	POST	/addPayerList	1	public void addPayerList(@BodyParam("body") Companymaster body) throws GenericApplicationException 
pas\CompanyApprovedAmountServiceImpl.java	/companyapprovedamount	POST	/getCompanyApprovedAmtListByAdmissionID	1	public List<CompanyApprovedAmtListByAdmissionIDNewResultDto> getCompanyApprovedAmtListByAdmissionID(@BodyParam("body") CompanyApprovedAmtListByAdmissionIDDto body)
pas\CompanyApprovedAmountServiceImpl.java	/companyapprovedamount	PUT	/deletePayerListById/{id}	0	public void deletePayerListById(@PathParam("id") Long id) throws GenericApplicationException 
pas\CompanyApprovedAmountServiceImpl.java	/companyapprovedamount	PUT	/updatePayerList	1	public Companymaster updatePayerList(@BodyParam("body") Companymaster body) throws GenericApplicationException 
pas\ComplaintDetailsServiceImpl.java	/complaintdetails	GET	/getComplaintDetailsByComptIDAndUnitID	0	public EqpComplaintView getComplaintDetailsByComptIDAndUnitID(@QueryParam("eqpComplaintId") @StoredProcNamedParam("p_eqpcomplaintid") Long eqpComplaintId,
pas\ComplaintDetailsServiceImpl.java	/complaintdetails	GET	/getDistrictFromZone	0	public List<DistrictMaster> getDistrictFromZone(@QueryParam("zoneId") Long zoneId, @QueryParam("stateId") Long stateId) throws GenericApplicationException 
pas\ComplaintDetailsServiceImpl.java	/complaintdetails	GET	/getEquipmentPreviousHistory	0	public List<EqpPreviousHistoryResultDto> getEquipmentPreviousHistory(@QueryParam("itemId") Long itemId, @QueryParam("storeId") Long storeId, @QueryParam("unitId") Long unitId)
pas\ComplaintDetailsServiceImpl.java	/complaintdetails	GET	/getPatVistAIenforInc	0	public Number getPatVistAIenforInc(@QueryParam("patmrn") String patmrn) throws GenericApplicationException 
pas\ComplaintDetailsServiceImpl.java	/complaintdetails	GET	/getUnitDepartmentDetails	1	public List<Departmentmaster> getUnitDepartmentDetails() throws GenericApplicationException 
pas\ComplaintDetailsServiceImpl.java	/complaintdetails	GET	/getUnitsByZone	0	public List<Unitmaster> getUnitsByZone(@QueryParam("zoneId") Long zoneId, @QueryParam("stateId") Long stateId
pas\ComplaintDetailsServiceImpl.java	/complaintdetails	POST	/addComplaintResolution	0	public ComplaintResolutionView addComplaintResolution(@BodyParam("body") ComplaintResolutionView body, @QueryParam("isUpdate") boolean isResolutionUpdate) throws GenericApplication
pas\ComplaintDetailsServiceImpl.java	/complaintdetails	POST	/addNewComplaint	1	public EqpComplaintDetailsDto addNewComplaint(@BodyParam("body") EqpComplaintDetailsDto body, @QueryParam("update") boolean update) throws GenericApplicationException 
pas\ComplaintDetailsServiceImpl.java	/complaintdetails	POST	/addNewEqpPreventSchedule	1	public List<EquipmentDetailsforSearchResultDto> addNewEqpPreventSchedule(@BodyParam("body") EqpPMScheduleDto body) throws GenericApplicationException 
pas\ComplaintDetailsServiceImpl.java	/complaintdetails	POST	/getEquiComplaintDetails	1	public List<ComplaintDetailsResultDto> getEquipmentComplaintDetails(@BodyParam("body") ComplaintResolutionDetailsDtoNew body, @QueryParam("complaintNo") String complaintNo,
pas\ComplaintDetailsServiceImpl.java	/complaintdetails	POST	/updateComplaintResolution	0	public List<EqpComplaintDetails> updateComplaintResolution(@BodyParam("body") EqpComplaintDetailsDto body) throws GenericApplicationException 
pas\ComplaintResolutionDetailServiceImpl.java	/complaintresolutiondetail	GET	/getAssetItemsWithAccessionCode	0	public List<AssetItemWithAccessCodeDto> getAssetItemsFormComplaintRaisedStore(@QueryParam("storeId") @StoredProcNamedParam("p_storeid") Long storeId) 
pas\ComplaintResolutionDetailServiceImpl.java	/complaintresolutiondetail	GET	/getMaintainanceDepUserList	0	public List<Staffdepartmentdetails> getMaintainanceDepUserList(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\ComplaintResolutionDetailServiceImpl.java	/complaintresolutiondetail	GET	/getStaffDetails	0	public Staffmaster getStaffDetails(@QueryParam("staffId") Long staffId) 
pas\ComplaintResolutionDetailServiceImpl.java	/complaintresolutiondetail	POST	/getComplaintResolutionDetails	0	public List<ComplaintResolutionDetailsResultDto> getComplaintResolutionDetails(@BodyParam("body") ComplaintResolutionDetailsDto body,@QueryParam("complaintNo") String complaintNo,
pas\ComplaintResolutionDetailServiceImpl.java	/complaintresolutiondetail	PUT	/updateStaffDetails	0	public void updateStaffDetails(@QueryParam("mobileNo") Long mobileNo, @QueryParam("mailID") String mailID,@QueryParam("staffID") Long staffID) throws GenericApplicationException 
pas\ConcessionTemplateServiceImpl.java	/concessiontemplate	GET	/getPathoConcessionTemplateById/{id}	1	public Concessiontemplatemaster getPathoConcessionTemplateById(@PathParam("id") Long id) throws GenericApplicationException 
pas\ConcessionTemplateServiceImpl.java	/concessiontemplate	POST	/addConcessionTemplateList	0	public void addConcession(@BodyParam("body") Concessiontemplatemaster body) throws GenericApplicationException 
pas\ConcessionTemplateServiceImpl.java	/concessiontemplate	PUT	/deleteConcessionTemplateListById/{id}	1	public void deleteConcessionTemplateListById(@PathParam("id") Long id) throws GenericApplicationException 
pas\ConcessionTemplateServiceImpl.java	/concessiontemplate	PUT	/updateConcessionTemplate/{id}	0	public Concessiontemplatemaster updateConcession(@BodyParam("body") Concessiontemplatemaster body, @PathParam("id") Long id) throws GenericApplicationException 
pas\DepartmentScheduleServiceImpl.java	/departmentschedule	POST	/addDepartmentMaster	0	public void addPlanMasterDetails(@BodyParam("body") Departmentmaster body) throws GenericApplicationException 
pas\DepartmentScheduleServiceImpl.java	/departmentschedule	POST	/getDepartmentsByUnit	1	public List<DepartmentMst> getDepartmentsByUnit(@BodyParam("body") Departmentmaster body) throws GenericApplicationException 
pas\DepartmentScheduleServiceImpl.java	/departmentschedule	POST	/updateDepartmentMaster	1	public Departmentmaster updateDepartmentMaster(@BodyParam("body") Departmentmaster body, @Context HttpServletRequest req) throws GenericApplicationException 
pas\DepartmentScheduleServiceImpl.java	/departmentschedule	PUT	/updateDepartmentMst/{id}	0	public DepartmentMst updateDepartmentMst(@BodyParam("body") DepartmentMst body, @PathParam("id") Long id, @Context HttpServletRequest req)
pas\DietServiceImpl.java	/diet	GET	/getActiveWards	1	public List<Wardmaster> getActiveWards(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\DietServiceImpl.java	/diet	GET	/getDietFoodListCount	1	public Map<String,Long> getDietFoodListCount(@QueryParam("unitId") Long unitId, @QueryParam("isForKitchenScreen") Boolean isForKitchenScreen) throws GenericApplicationException 
pas\DietServiceImpl.java	/diet	GET	/getDietKichenDetails	1	public DietKitchenView getKichenDetail(@QueryParam("unitId") Long unitId, @QueryParam("wardId") Long wardId, @QueryParam("sortDietCategoryType") String sortDietCategoryType) throws
pas\DietServiceImpl.java	/diet	GET	/getDietOrderDetailByAdmissionId	0	public DietOrderDetails getDietOrderDetailByAdmissionId(@QueryParam("unitId") Long unitId, @QueryParam("admissionId") Long admissionId) throws GenericApplicationException
pas\DietServiceImpl.java	/diet	GET	/getDietOrderListCount	1	public DietOrderDetailsSummary getDietOrderListCount(@QueryParam("unitid") Long unitid, @QueryParam("wardId") Long wardId) throws GenericApplicationException 
pas\DietServiceImpl.java	/diet	GET	/getDietPatReport	0	public List<DietOrderScheduleDetails> getDietPatReport(@QueryParam("admissionId") Long admissionId) throws GenericApplicationException 
pas\DietServiceImpl.java	/diet	GET	/getItemsByDietTypeId	1	public List<TodayDietItemMenu> removeDietMenuItem(@QueryParam("DietTypeId") Long dietTypeId , @QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\DietServiceImpl.java	/diet	GET	/getPatientDietOrderList	1	public List<DietOrderDetails> getPatientDietOrderList(@QueryParam("opdIpdId") Long opdIpdId) throws GenericApplicationException 
pas\DietServiceImpl.java	/diet	GET	/getservicemaster	0	public List<Servicemaster> getServiceMasterData() throws GenericApplicationException 
pas\DietServiceImpl.java	/diet	GET	/getTodayDietItem	1	public JSONArray getTodayDietItems(@QueryParam("page") Integer page,
pas\DietServiceImpl.java	/diet	GET	/getWardsForFilter	0	public List<Wardmaster> getWardsForFilter(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\DietServiceImpl.java	/diet	GET	/getWardWisePatDtls	1	public List<DietOrderScheduleDetails> getWardWisePatDtls(@QueryParam("wardId") Long wardId, @QueryParam("dietSessionId") Long dietSessionId) throws GenericApplicationException 
pas\DietServiceImpl.java	/diet	POST	/adddietitemmaster	0	public void addDietItem(@BodyParam("body") Dietitemmaster body) throws GenericApplicationException 
pas\DietServiceImpl.java	/diet	POST	/addDietItemMenu	1	public void addDietItemMenu(@BodyParam("body") Dietitemmaster body) throws GenericApplicationException 
pas\DietServiceImpl.java	/diet	POST	/addDietMenuItem	0	public TodayDietItemMenu getTodayDietItems(@BodyParam("body") TodayDietItemMenu body) throws GenericApplicationException 
pas\DietServiceImpl.java	/diet	POST	/addpatdietorder	1	public List<DietOrderDetails> addPatDietOrder(@BodyParam("body") DietOrderDetails body) throws GenericApplicationException 
pas\DietServiceImpl.java	/diet	POST	/addPatFooddItemOrder	0	public void addPatFooddItemOrder(@BodyParam("body") DietOrderView body) throws GenericApplicationException, ParseException 
pas\DietServiceImpl.java	/diet	POST	/getDietFoodOrderList	0	public List<DietOrderScheduleDetails> getDietFoodOrderList(@QueryParam("page") Integer page,
pas\DietServiceImpl.java	/diet	POST	/getDietOrderList	0	public List<DietOrderScheduleDetails> getDietOrderList(@QueryParam("page") Integer page,
pas\DietServiceImpl.java	/diet	POST	/getFilterwiseDietOrderList	1	public List<DietOrderDetails> getFilterwiseDietOrderList(@BodyParam("body") DietOrderDetails dietOrderDetails) throws GenericApplicationException 
pas\DietServiceImpl.java	/diet	POST	/removeDietItem	1	public DietItemScheduleDetails removeDietItem(@BodyParam("body") DietOrderScheduleDetails body) throws GenericApplicationException
pas\DietServiceImpl.java	/diet	POST	/removeDietMenuItem	1	public TodayDietItemMenu removeDietMenuItem(@BodyParam("body") TodayDietItemMenu body) throws GenericApplicationException 
pas\DietServiceImpl.java	/diet	POST	/signPatientDietOrders	0	public Boolean signPatientDietOrders(@BodyParam("body") List<DietOrderDetails> body) throws GenericApplicationException 
pas\DietServiceImpl.java	/diet	POST	/updateDietFoodOrderForAll	0	public Boolean updateDietFoodOrderForAll(@BodyParam("body") DietOrderView body, @QueryParam("wardId") Long wardId, @QueryParam("acceptAll") @DefaultValue("false") Boolean acceptAll
pas\DietServiceImpl.java	/diet	POST	/updateDietOrderDetailsById	0	public Boolean updateDietOrderDetailsById(@BodyParam("body") DietOrderDetails dietOrderDetails) throws GenericApplicationException 
pas\DietServiceImpl.java	/diet	PUT	/updateDietFoodOrder	0	public void updateDietFoodOrder(@QueryParam("admissionId") Long admissionId,@QueryParam("dietOrderId") Long dietOrderId,
pas\DietServiceImpl.java	/diet	PUT	/updateDietFoodStatusForCancel	0	public void updateDietFoodStatusForCancel(@BodyParam("body") DietOrderScheduleDetails body) throws GenericApplicationException 
pas\DietServiceImpl.java	/diet	PUT	/updatedietitemmaster	1	public void updateDietItem(@BodyParam("body") Dietitemmaster body) throws GenericApplicationException 
pas\DietServiceImpl.java	/diet	PUT	/updateDietOrderDetails	1	public void updateDietOrderDetails(@QueryParam("orderid") Long orderid, @QueryParam("orderStatus") Long orderStatus) throws GenericApplicationException 
pas\DietServiceImpl.java	/diet	PUT	/updateUncheckFoodOrder	0	public void updateUncheckFoodOrder(@QueryParam("admissionId") Long admissionId,@QueryParam("dietOrderId") Long dietOrderId,
pas\DoctorServiceImpl.java	/doctor	GET	/checkDoctorIsCheckedIn	1	public DoctorCheckInQueueRoom checkDoctorIsCheckedIn(@QueryParam("doctorId") Long doctorId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\DoctorServiceImpl.java	/doctor	GET	/checkoutQMSDoctor	0	public void checkoutQMSDoctor(@QueryParam("departmentId") Long departmentId) throws GenericApplicationException 
pas\DoctorServiceImpl.java	/doctor	GET	/getAssignedPatientDetailForDoctorQueue	1	public List<PatientDetailsForDoctorQueue> getAssignedPatientDetailForDoctorQueue(@QueryParam("roomId") Long roomId, @QueryParam("unitId") Long unitId, @QueryParam("departmentId") L
pas\DoctorServiceImpl.java	/doctor	GET	/getAssignedPatientDetailForDoctorQueueFromNurse	1	public List<PatientDetailsForDoctorQueue> getAssignedPatientDetailForDoctorQueueFromNurse(@QueryParam("roomId") Long roomId, @QueryParam("unitId") Long unitId, @QueryParam("departm
pas\DoctorServiceImpl.java	/doctor	GET	/getDepartmentListByDoctorID	0	public List<DepartmentListByDoctorIDResultDto_1> getDepartmentListByDoctorID(@QueryParam("doctorId") @StoredProcNamedParam("p_doctorid") Long doctorId,
pas\DoctorServiceImpl.java	/doctor	GET	/getDepartmentsByDoctorID	0	public List<DepartmentListByDoctorIDResultDto_1> getDepartmentsByDoctorID(@QueryParam("doctorId") @StoredProcNamedParam("p_doctorid") Long doctorId, @QueryParam("unitId") @StoredPr
pas\DoctorServiceImpl.java	/doctor	GET	/getDoctorFutureAppointmentSchedule	1	public List<AppointmentMrNoListResultDto> getDoctorFutureAppointmentSchedule(@QueryParam("doctorId") Long doctorId, @QueryParam("page") Integer page, @QueryParam("count") Integer c
pas\DoctorServiceImpl.java	/doctor	GET	/getMappedRoomFromDepartmentId	0	public List<RoomMasterForQueue> getMappedRoomFromDepartmentId(@QueryParam("departmentId") Long departmentId, @QueryParam("unitId") Long unitId, @DefaultValue("false") @QueryParam("
pas\DoctorServiceImpl.java	/doctor	GET	/getPatientDetailByTokenAndDepartment	1	public List<PatientDetailsForDoctorQueue> getPatientDetailByTokenAndDepartment(@QueryParam("token") String token, @QueryParam("mrno") String mrno) throws GenericApplicationExceptio
pas\DoctorServiceImpl.java	/doctor	POST	/addDoctorAdditionDeduction	1	public DoctorAdditionDeductionDto addDoctorAdditionDeduction(@BodyParam("body") DoctorAdditionDeductionDto body) throws GenericApplicationException 
pas\DoctorServiceImpl.java	/doctor	POST	/addDoctorBill	0	public void addDoctorBill(@BodyParam("body") DoctorBillView body) throws GenericApplicationException 
pas\DoctorServiceImpl.java	/doctor	POST	/addDoctorBillPayment	1	public void addDoctorBillPayment(@BodyParam("body") AddPaymentDto body) throws GenericApplicationException 
pas\DoctorServiceImpl.java	/doctor	POST	/assignedRoomToDoctorForQueue	0	public DoctorCheckInQueueRoom assignedRoomToDoctorForQueueApi(@QueryParam("departmentId") Long departmentId, @QueryParam("doctorId") Long doctorId, @QueryParam("roomId") Long roomI
pas\DoctorServiceImpl.java	/doctor	POST	/assignNextPatientToDoctor	0	public String assignNextPatientToDoctor(@BodyParam("body") List<PatientDetailsForDoctorQueue> body, @QueryParam("departmentId") Long departmentId, @QueryParam("roomId") Long roomId
pas\DoctorServiceImpl.java	/doctor	POST	/assignNextPatientToDoctorFromNurseDesk	0	public String assignNextPatientToDoctorFromNurseDesk(@QueryParam("departmentId") Long departmentId, @QueryParam("roomId") Long roomId, @QueryParam("unitId") Long unitId, @QueryPara
pas\DoctorServiceImpl.java	/doctor	POST	/assignPatientToDoctorQueueByNurse	0	public String assignPatientToDoctorQueueByNurse(@BodyParam("body") PatientDetailsForDoctorQueue body, @QueryParam("roomId") Long roomId, @QueryParam("unitId") Long unitId) throws G
pas\DoctorServiceImpl.java	/doctor	POST	/checkoutSinglePatientFromQueue	1	public Boolean checkoutSinglePatientFromQueue(@QueryParam("patientId") Long patientId, @QueryParam("opdIpdId") Long opdIpdId, @QueryParam("departmentId") Long departmentId, @QueryP
pas\DoctorServiceImpl.java	/doctor	POST	/getDoctorAddDeductList	0	public List<DoctorAddDeductResultListDto> getDoctorAddDeductList(@BodyParam("body") DoctorAddDeductListDto body) throws GenericApplicationException 
pas\DoctorServiceImpl.java	/doctor	POST	/getDoctorBillList	0	public
pas\DoctorServiceImpl.java	/doctor	POST	/getDoctorShareChargeDetailList	0	public List<DoctorShareChargeDetailResultListDto> getDoctorShareChargeDetailList(@BodyParam("body") DoctorShareChargeDetailListDto body) throws GenericApplicationException 
pas\DoctorServiceImpl.java	/doctor	POST	/getDoctorShareList	0	public
pas\DoctorServiceImpl.java	/doctor	POST	/updateDoctorbusyFlag	1	public Boolean updateDoctorbusyFlag(@QueryParam("departmentId") Long departmentId, @QueryParam("roomId") Long roomId, @QueryParam("doctorId") Long doctorId, @QueryParam("unitId") L
pas\DoctorServiceImpl.java	/doctor	PUT	/assignPatientVisit	1	public void assignPatientVisit(@QueryParam("appointmentId") Long appointmentId, @QueryParam("unitId") Long unitId, @QueryParam("cabinId") Long cabinId) throws GenericApplicationExc
pas\DoctorServiceImpl.java	/doctor	PUT	/checkinCheckout	1	public Boolean checkinCheckout(@QueryParam("isCheckin") boolean isCheckin, @QueryParam("cabinId") Long cabinId, @QueryParam("doctorId") Long doctorId,
pas\DoctorServiceImpl.java	/doctor	PUT	/updateDoctorShare	0	public void updateDoctorShare(@BodyParam("body") List<UpdateDoctorShareDto> body) throws GenericApplicationException 
pas\DrugServiceImpl.java	/drug	POST	/getPrescriptionList	0	public
pas\EquipmentServiceImpl.java	/equipment	DELETE	/rejectEquipmentProvGRN	1	public void rejectProvisionalGRN(@QueryParam("grn_id") Long grnId) throws GenericApplicationException 
pas\EquipmentServiceImpl.java	/equipment	GET	/deleteEquipmentDoc	0	public void deleteEquipmentDoc(@QueryParam("equipmentId") Long equipmentId, @QueryParam("eqpFileName") String eqpFileName,
pas\EquipmentServiceImpl.java	/equipment	GET	/downloadActualEquipmentDoc	0	public void downloadActualEquipmentDoc(@QueryParam("eqpContentFile") String fileNameHash, @Context HttpServletResponse response)
pas\EquipmentServiceImpl.java	/equipment	GET	/downloadEquipmentDoc	1	public String downloadEquipmentDoc(@HeaderParam("token") String token, @QueryParam("equipmentId") Long equipmentId, @QueryParam("eqpFileName") String eqpFileName,
pas\EquipmentServiceImpl.java	/equipment	GET	/getAccessionNoForMedGass	1	public Equipmentdetail getAccessionNoForMedGass(@QueryParam("equipmentId") String equipmentId, @QueryParam("itemId") Long itemId,
pas\EquipmentServiceImpl.java	/equipment	GET	/getAssetDetailsByAccessionNumber	0	public EqpComplaintView getAssetDetailsByAccessionNumber(@QueryParam("accessionNumber") @StoredProcNamedParam("p_accessionnumber") String accessionNumber,
pas\EquipmentServiceImpl.java	/equipment	GET	/getAssetDtlsByItem	0	public List<AssetDetailsView> getAssetDetailsByItem(@QueryParam("equipId") @StoredProcNamedParam("p_equipid") Long eqipId) throws GenericApplicationException 
pas\EquipmentServiceImpl.java	/equipment	GET	/getAssetGrnUploadedDocument	1	public List<PatientIDDocumentDto> getAssetGrnUploadedDocument(@QueryParam("grnId") Long grnId) throws GenericApplicationException 
pas\EquipmentServiceImpl.java	/equipment	GET	/getAssetVerificationList	1	public List<EquipmentListDetailsDto> getAssetVerificationList(@QueryParam("unitId") @StoredProcNamedParam("p_unitid") Long unitId,
pas\EquipmentServiceImpl.java	/equipment	GET	/getEqAmcWarrantyCount	1	public Map<String, Long> getEqAmcWarrantyCount(@QueryParam("amcWarrantyCategory") String amcWarrantyCategory) throws GenericApplicationException 
pas\EquipmentServiceImpl.java	/equipment	GET	/getEqAmcWarrantyDetails	0	public List<EquipAmcWarrantyDetails> getEqAmcWarrantyDetails(@QueryParam("page") Integer page, @QueryParam("count") Integer count,
pas\EquipmentServiceImpl.java	/equipment	GET	/getEqpDetailsByEqpId	0	public List<GetEqpDetailsByEqpById> getEqpDetailsByEqpId(@QueryParam("unitId") Long unitId,
pas\EquipmentServiceImpl.java	/equipment	GET	/getEquipDetailsByItemID	0	public List<EquipDetailsByItemIDResultDto> getEquipDetailsByItemID(@QueryParam("equipId") @StoredProcNamedParam("p_equipid") Long equipId,
pas\EquipmentServiceImpl.java	/equipment	GET	/getEquipmentItemDetails	1	public List<GetEquipmentItemDetailsDto> getEquipmentItemDetails(@QueryParam("storeId") @StoredProcNamedParam("p_storeid") Long storeId,
pas\EquipmentServiceImpl.java	/equipment	GET	/getIndentEquipmentByIndentNo	0	public List<EquipmentIndentDetails> getIndentEquipmentByIndentNo(@QueryParam("indentNumber") String indentNumber) throws GenericApplicationException 
pas\EquipmentServiceImpl.java	/equipment	GET	/getIndentIssueEquipmentByIndentNo	0	public List<EquipmentIndentDetails> getIndentIssueEquipmentByIndentNo(@QueryParam("indentNumber") String indentNumber) throws GenericApplicationException 
pas\EquipmentServiceImpl.java	/equipment	GET	/getIsAccession	0	public boolean getIsAccession(@QueryParam("unitId") Long unitId, @QueryParam("itemId") Long itemId) throws GenericApplicationException 
pas\EquipmentServiceImpl.java	/equipment	GET	/getPoNumberforEquipmentGRN	0	public List<String> getPoNumberforGRN(@QueryParam("unitid") Long unitid, @QueryParam("storeid") Long storeid, @QueryParam("supplierid") Long supplierid) throws GenericApplicationEx
pas\EquipmentServiceImpl.java	/equipment	GET	/getSupplierForEquipment	1	public List<Object> getSupplierForEquipment(@QueryParam("itemId") Long itemId,@QueryParam("equipmentId") Long equipmentId) throws GenericApplicationException 
pas\EquipmentServiceImpl.java	/equipment	GET	/getUploadedEquipmentFiles	0	public List<EquipmentDetailDocs> getUploadedEquipmentFiles(@QueryParam("equipmentId") Long equipmentId) throws GenericApplicationException 
pas\EquipmentServiceImpl.java	/equipment	GET	/recieveEquipment	0	public List<GetEquipmentItemDetailsDto> recieveEquipment(@QueryParam("itemId") Long itemId, @QueryParam("unitId") Long unitId, @QueryParam("equipmemtId") Long equipmemtId,
pas\EquipmentServiceImpl.java	/equipment	GET	/updateMedIndentApprove	0	public Equipmentdetail updateMedIndentApprove(@QueryParam("indentReqNo") String indentReqNo, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\EquipmentServiceImpl.java	/equipment	POST	/addEquipmentCheckList	0	public void addEquipmentCheckList(@BodyParam("body") Equipchecklist body) throws GenericApplicationException 
pas\EquipmentServiceImpl.java	/equipment	POST	/addEquipmentIndent	0	public String addEquipmentIndent(@BodyParam("body") AddIndentView body) throws GenericApplicationException 
pas\EquipmentServiceImpl.java	/equipment	POST	/addEquipOpeningBalance	1	public void addEquipOpeningBalanceApi(@BodyParam("body") CompleteIndentView body) throws GenericApplicationException 
pas\EquipmentServiceImpl.java	/equipment	POST	/addEqupmentUsageMaster	1	public void addEqupmentUsageMaster(@BodyParam("body") List<EquipmentServiceMaster> body) throws IOException, GenericApplicationException 
pas\EquipmentServiceImpl.java	/equipment	POST	/addProvisionlEquipmentGRN	0	public AddGRNView addProvisionlEquipmentGRN(@BodyParam("body") AddGRNView body) throws GenericApplicationException 
pas\EquipmentServiceImpl.java	/equipment	POST	/approveProvisionalEquipmentGRNNew	1	public AddGRNView approveProvisionalGRNNew(@BodyParam("body") AddGRNView body) throws GenericApplicationException 
pas\EquipmentServiceImpl.java	/equipment	POST	/cancelEquipDetails	0	public String cancelEquipDetails(@BodyParam("body") CancelEquipmentDetailsDto body) throws GenericApplicationException 
pas\EquipmentServiceImpl.java	/equipment	POST	/equipCompleteOpenBalItems	1	public void equipCompleteOpenBalItems(@BodyParam("body") List<EqiupOpeningBalance> body) throws GenericApplicationException 
pas\EquipmentServiceImpl.java	/equipment	POST	/equipIndentIssueComplete	0	public String equipIndentIssueComplete(@BodyParam("body") CompleteIndentView body, @QueryParam("indentNumber") String indentNumber) throws GenericApplicationException 
pas\EquipmentServiceImpl.java	/equipment	POST	/generateCodeForRegistredAsset	0	public String[] generateCodeForRegistredAsset(@BodyParam("body") RegisteredAssetDetailsDto body) throws IOException, GenericApplicationException 
pas\EquipmentServiceImpl.java	/equipment	POST	/getApprovEquipmenGRNListForSearch	1	public List<GRNListForSearchResultDto> getApproveGRNListForSearch(@BodyParam("body") GRNListForSearchDto body, @QueryParam("status") Long status,
pas\EquipmentServiceImpl.java	/equipment	POST	/getAssetDetails	1	public List<AssetDetailsResultDto> getAssetDetails(@BodyParam("body") AssetDetailsNewDto body, @QueryParam("equipmentName") String name, @QueryParam("maximumRows") Long maximumRows
pas\EquipmentServiceImpl.java	/equipment	POST	/getEquipDetForAssetRegister	0	public List<EquipmentListDetailsDto> getEquipDetByAccessionNo(@BodyParam("body") EquipmentRegisterDto body, @QueryParam("equipmentName") String equipmentName,
pas\EquipmentServiceImpl.java	/equipment	POST	/getEquipMedicalIndetIssueList	1	public List<EquipmentIndentDetails> getEquipMedicalIndetIssueList(@BodyParam("body") IndentListByStoreIdDto body, @QueryParam("search") String search,
pas\EquipmentServiceImpl.java	/equipment	POST	/getEquipMedicalIndetList	1	public List<EquipmentIndentDetails> getEquipMedicalIndetList(@BodyParam("body") IndentListByStoreIdDto body, @QueryParam("search") String search,
pas\EquipmentServiceImpl.java	/equipment	POST	/getEquipmentDetails	1	public List<GetDirectIssueItemDetailsResultDto> getEquipmentDetails(@BodyParam("body") GetDirectIssueItemDetailsDto body) throws GenericApplicationException 
pas\EquipmentServiceImpl.java	/equipment	POST	/getEquipmentPOrderList	1	public PurchaseOrderView getEquipmentPOrderList(@BodyParam("body") PurchaseOrderView body) throws GenericApplicationException 
pas\EquipmentServiceImpl.java	/equipment	POST	/getEquipUsageServiceList	1	public List<EquipmentUsageDetails> getEquipUsageServiceList(@BodyParam("body") GRNListForSearchDto body, @QueryParam("search") String search) throws GenericApplicationException 
pas\EquipmentServiceImpl.java	/equipment	POST	/getGRNForGrnNosforDirectGRN	1	public List<GRNResultRptforDirectGRNDto> getGRNForGrnNosforDirectGRN(@BodyParam("body") GRNRptDto body, @DefaultValue("false") @QueryParam("is_print") boolean print)
pas\EquipmentServiceImpl.java	/equipment	POST	/receiveItemIssueForEquipment	0	public void updateReceiveItemIssue(@BodyParam("body") List<ItemStockDto> body) throws GenericApplicationException 
pas\EquipmentServiceImpl.java	/equipment	POST	/updateEqpmentAprGRNNew	1	public void upidateGRNNew(@BodyParam("body") AddGRNView body) throws GenericApplicationException 
pas\EquipmentServiceImpl.java	/equipment	POST	/updateEquipmentGRN	1	public EquipmentDetailMasterDto updateEquipmentGRN(@BodyParam("body") EquipmentDetailMasterDto body) throws GenericApplicationException 
pas\EquipmentServiceImpl.java	/equipment	POST	/updateEquipmentWarranty	1	public List<EquipmentWarranty> updateEquipmentWarranty(@BodyParam("body") List<AddEqpWarrantyDto> body) throws GenericApplicationException 
pas\EquipmentServiceImpl.java	/equipment	POST	/updateEquipOpenBalItems	1	public void updateEquipOpenBalItems(@BodyParam("body") List<EqiupOpeningBalance> body) throws GenericApplicationException 
pas\EquipmentServiceImpl.java	/equipment	POST	/uploadAssetGrnDocument	1	public Boolean uploadAssetGrnDocument(@BodyParam("body") PatientIDDocumentDto documentDto, @QueryParam("grnId") Long grnId) throws IOException 
pas\EquipmentServiceImpl.java	/equipment	POST	/uploadEquipmentFiles	0	public List<StorageRequest> uploadEquipmentFiles(@QueryParam("equipmentId") Long equipmentId, final List<Attachment> attachments, @QueryParam("documentType") String documentType,
pas\EquipmentServiceImpl.java	/equipment	PUT	/equipMedicalGassesUpdate	0	public String equipMedicalGassesUpdate(@QueryParam("accessionNo") String accessionNo, @QueryParam("unitId") Long unitId, @QueryParam("equipmentId") Long equipmentId, @QueryParam("i
pas\EquipmentServiceImpl.java	/equipment	PUT	/updateEquipmentCheckList	0	public void updateEquipmentCheckList(@BodyParam("body") List<Equipchecklist> body) throws GenericApplicationException 
pas\EquipmentServiceImpl.java	/equipment	PUT	/updateInvoiceForAsset	0	public void updateInvoiceForAsset(@QueryParam("grnId") Long grnId, @QueryParam("invoiceDate") String invoiceDate, @QueryParam("invoiceNumber") String invoiceNo)
pas\ExpiryItemServiceImpl.java	/expiryitem	GET	/getExpiredDamagedItemListById	0	public
pas\ExpiryItemServiceImpl.java	/expiryitem	GET	/getExpiredItemList	1	public List<ItemExpiryList> getExpiredItems(@QueryParam("storeId") Long storeId,@QueryParam("maximumRows") Long maxRows,
pas\ExpiryItemServiceImpl.java	/expiryitem	GET	/getExpiryReturnDetails	0	public List<ExpiryItemReturnDetailsDto> getExpiryReturnDetails(@QueryParam("unitId") @StoredProcNamedParam("p_unitid") Long unitId,
pas\ExpiryItemServiceImpl.java	/expiryitem	GET	/getGRNItemBatchesToReturn	0	public List<ItemBatchesResultDto> getGRNItemBatchesToReturn(@QueryParam("itemId") @StoredProcNamedParam("p_itemid") Long itemId,
pas\ExpiryItemServiceImpl.java	/expiryitem	GET	/getGrnNoforRetToSupplier	0	public List<String> getPoNumberforGRN(@QueryParam("unitId") Long unitId, @QueryParam("storeId") Long storeId, @QueryParam("supplierId") Long supplierId) 
pas\ExpiryItemServiceImpl.java	/expiryitem	GET	/getGRNReturnDetailsById	0	public List<GrnReturnDetailsByIdResultDto> getGRNReturnDetailsById(@QueryParam("grnReturnId") @StoredProcNamedParam("p_id") Long grnReturnId) throws GenericApplicationException 
pas\ExpiryItemServiceImpl.java	/expiryitem	GET	/getGRNReturnDetailsByIdReceiveCredit	0	public List<GrnReturnDetailsByIdResultDto> getGRNReturnDetailsByIdREceiveCredit(@QueryParam("grnReturnId") @StoredProcNamedParam("p_id") Long grnReturnId,
pas\ExpiryItemServiceImpl.java	/expiryitem	GET	/getItemInfoforReturntoSupplier	1	public List<PreviousItemGRNDetailsforGRNReturnDto> getItemInfoforReturntoSupplier(@QueryParam("itemId") Long itemId, @QueryParam("batchId") Long batchId, @QueryParam("supplierId") 
pas\ExpiryItemServiceImpl.java	/expiryitem	GET	/getStoreListByBatchId	1	public List<RecallExpiredItemView> getStoreListByBatchId(@QueryParam("batchId") @StoredProcNamedParam("p_id") Long batchId) throws GenericApplicationException 
pas\ExpiryItemServiceImpl.java	/expiryitem	GET	/getSupplierCreditNoteDetailsById	0	public List<SupplierCreditNoteDetailsByIdResultDto> getSupplierCreditNoteDetailsById(@QueryParam("grnReturnId") @StoredProcNamedParam("p_id") Long grnReturnId) throws GenericApplic
pas\ExpiryItemServiceImpl.java	/expiryitem	POST	/addExpiredDamagedItemIndent	0	public AddIndentView addExpiredDamagedItemIndent(@BodyParam("body") AddIndentView body) throws GenericApplicationException 
pas\ExpiryItemServiceImpl.java	/expiryitem	POST	/addExpiryItemReturn	0	public AddExpiryItemReturnView addExpiryItem(@BodyParam("body") AddExpiryItemReturnView body) throws GenericApplicationException 
pas\ExpiryItemServiceImpl.java	/expiryitem	POST	/addItemBatchesOnHold	0	public AddItemBatchesForHoldView addItemforConsumption(@BodyParam("body") AddItemBatchesForHoldView body) throws GenericApplicationException 
pas\ExpiryItemServiceImpl.java	/expiryitem	POST	/addSupplierGRNReturn	1	public AddGRNReturnView addSupplierGRNReturn(@BodyParam("body") AddGRNReturnView body) throws GenericApplicationException 
pas\ExpiryItemServiceImpl.java	/expiryitem	POST	/expiryItemReturnToSupplier	1	public RecallExpiredItemView expiryItemReturnToSupplier(@QueryParam("storeId") @StoredProcNamedParam("p_storeid") Long storeId, @QueryParam("unitId") @StoredProcNamedParam("p_uniti
pas\ExpiryItemServiceImpl.java	/expiryitem	POST	/getDamagedItemIndentList	0	public List<DamagedItemIndentListResultDto> getDamagedItemIndentList(@BodyParam("body") DamagedItemIndentListDto body, @QueryParam("search") String search,
pas\ExpiryItemServiceImpl.java	/expiryitem	POST	/getDamagedReturnedItemDetailsById	0	public
pas\ExpiryItemServiceImpl.java	/expiryitem	POST	/getExpiredAndDamagedItemIndentList	0	public List<ExpiredItemIndentListResultDto> getExpiredAndDamagedItemIndentList(@BodyParam("body") ExpiredItemIndentListDto body) throws GenericApplicationException 
pas\ExpiryItemServiceImpl.java	/expiryitem	POST	/getExpiredDamagedItemAvaStock	0	public
pas\ExpiryItemServiceImpl.java	/expiryitem	POST	/getExpiredItemIndentList	0	public List<ExpiredItemIndentListResultDto> getExpiredItemIndentList(@BodyParam("body") ExpiredItemIndentListDto body, @QueryParam("search") String search,
pas\ExpiryItemServiceImpl.java	/expiryitem	POST	/getExpiredReturnedItemDetailsById	1	public
pas\ExpiryItemServiceImpl.java	/expiryitem	POST	/getExpiryItemList	1	public List<ExpiryItemListResultDto> getExpiryItemList(@BodyParam("body") ExpiryItemListDto body) throws GenericApplicationException 
pas\ExpiryItemServiceImpl.java	/expiryitem	POST	/getExpiryItemsReturnList	0	public List<ItemBatchesResultDto> getExpiryItemsReturnList(@BodyParam("body") ExpiryItemReturnListSearchDto body,
pas\ExpiryItemServiceImpl.java	/expiryitem	POST	/getExpiryReturnItemDetailList	1	public
pas\ExpiryItemServiceImpl.java	/expiryitem	POST	/getGRNReturnList	0	public List<GrnReturnListResultDto> getGRNReturnList() throws GenericApplicationException 
pas\ExpiryItemServiceImpl.java	/expiryitem	POST	/getRecallExpiredItemList	1	public List<RecallExpiredItemView> getRecallExpiredItemList(@BodyParam("body") RecallExpiredItemStockListDto body, @DefaultValue("false") @QueryParam("is_print") boolean print)
pas\ExpiryItemServiceImpl.java	/expiryitem	POST	/getRecallExpiredItemListById	0	public List<RecallExpiredItemView> getRecallExpiredItemListById(@BodyParam("body") RecallExpiredItemListByIdDto body, @DefaultValue("false") @QueryParam("is_print") boolean print)
pas\ExpiryItemServiceImpl.java	/expiryitem	POST	/getRecallExpiredItemListNew	0	public RecallExpiredItemView getRecallExpiredItemListNew(@BodyParam("body") RecallExpiredItemStockListDto body, @DefaultValue("false") @QueryParam("is_print") boolean print,
pas\ExpiryItemServiceImpl.java	/expiryitem	POST	/getReturnToSupplierList	1	public List<GetGRNReturnListResultDto> getReturnToSupplierList(@BodyParam("body") ExpiredItemIndentListDto body, @QueryParam("search") String search,
pas\ExpiryItemServiceImpl.java	/expiryitem	POST	/getSupplierProductList	0	public List<SupplierProductListResultDto> getSupplierProductList(@BodyParam("body") SupplierProductListResultDto body) throws GenericApplicationException 
pas\ExpiryItemServiceImpl.java	/expiryitem	POST	/getSupplierReturnItemDetailsById	1	public
pas\ExpiryItemServiceImpl.java	/expiryitem	POST	/getSupplierReturnItemDetailsByIdforPrint	0	public BillReceiptView getSupplierReturnItemDetailsByIdforPrint(@BodyParam("body") SupplierReturnItemDetailsResultDto body, @DefaultValue("false") @QueryParam("is_print") boolean p
pas\ExpiryItemServiceImpl.java	/expiryitem	POST	/getSuppliersByItemIdForExpiryItemReturn	1	public List<ExpiredItemSupplierResultDto> getSuppliersByItemIdForExpiryItemReturn(@BodyParam("body") ExpiryItemSuppliersSearchDto body) throws GenericApplicationException 
pas\ExpiryItemServiceImpl.java	/expiryitem	POST	/issueExpiredItems	1	public IssueView issueExpiredItems(@BodyParam("body") CompleteIndentView body) throws GenericApplicationException 
pas\ExpiryItemServiceImpl.java	/expiryitem	POST	/printCreditNoteDetails	0	public CreditNoteDetailsView printCreditNoteDetails(@QueryParam("grnReturnId") @StoredProcNamedParam("p_id") Long grnReturnId, @BodyParam("body") GRNReturnDetailsDto body)
pas\ExpiryItemServiceImpl.java	/expiryitem	POST	/printDamagedReturnedItemDetails	0	public ExpiryItemDEtailsView printDamagedReturnedItemDetailsById(@BodyParam("body") DamagedReturnedItemDetailsResultDto body) throws GenericApplicationException 
pas\ExpiryItemServiceImpl.java	/expiryitem	POST	/printExpiredReturnedItemDetails	0	public
pas\ExpiryItemServiceImpl.java	/expiryitem	POST	/receiveCreditNote	0	public SupplierCreditNoteDto receiveCreditNote(@BodyParam("body") SupplierCreditNoteView body) throws GenericApplicationException 
pas\ExpiryItemServiceImpl.java	/expiryitem	POST	/receiveExpiredItems	1	public IssueView receiveExpiredItems(@BodyParam("body") CompleteIndentView body) throws GenericApplicationException 
pas\ExpiryItemServiceImpl.java	/expiryitem	PUT	/approveExpiredItemIndent	0	public void approveExpiredItemIndent(@QueryParam("indentNumber") String indentNumber, @QueryParam("transactionStatus") String transactionStatus) throws GenericApplicationException 
pas\ExpiryItemServiceImpl.java	/expiryitem	PUT	/approveSupplierReturn	0	public void approveSupplierReturn(@BodyParam("body") SupplierReturnItemDetailsResultDto body) throws GenericApplicationException 
pas\ExpiryItemServiceImpl.java	/expiryitem	PUT	/expDamageReturnAceptance	0	public void expDamageReturnAceptance(@QueryParam("expiryItemReturnId") Long expiryItemReturnId, @QueryParam("itemReturnStatus") String itemReturnStatus, @QueryParam("remark") Strin
pas\ExpiryItemServiceImpl.java	/expiryitem	PUT	/freezeRecallExpiredItems	0	public void freezeRecallExpiredItems(@BodyParam("body") AddIndentView body) throws GenericApplicationException 
pas\ExpiryItemServiceImpl.java	/expiryitem	PUT	/freezeRecallExpiredItemsNew	1	public AddIndentView freezeRecallExpiredItemsNew(@BodyParam("body") AddIndentView body) throws GenericApplicationException 
pas\ExpiryItemServiceImpl.java	/expiryitem	PUT	/issueExpiredItemIndent	1	public void issueExpiredItemIndent(@QueryParam("indentNumber") String indentNumber, @QueryParam("transactionStatus") String transactionStatus) throws GenericApplicationException 
pas\ExpiryItemServiceImpl.java	/expiryitem	PUT	/rejectSupplierReturn	1	public void rejectSupplierReturn(@BodyParam("body") SupplierReturnItemDetailsResultDto body) throws GenericApplicationException 
pas\ExpiryItemServiceImpl.java	/expiryitem	PUT	/updateExpiryItemReturnStatus	0	public void updateExpiryItemReturnStatus(@BodyParam("body") ExpiryReturnDetailsForUpdateView body) throws GenericApplicationException 
pas\HelperServiceImpl.java	/helper	GET	/getAllDataForReport	0	public HospitalReportSummary getAllDataForReport(@QueryParam("unitId") Long unitId, @QueryParam("fromDate") java.sql.Date fromDate, @QueryParam("toDate") java.sql.Date toDate) thro
pas\HelperServiceImpl.java	/helper	POST	/registerChild	0	public List<ChildResultDto> registerChild(@BodyParam("body") ChildDto body, @QueryParam("token") String token) throws GenericApplicationException 
pas\HelperServiceImpl.java	/helper	POST	/registerChildDocit	1	public List<ChildResultDto> registerChildDocit(@BodyParam("body") ChildDto body)
pas\IPDAdmissionServiceImpl.java	/ipdadmission	DELETE	/removeMLCFile	0	public void removeMLCFile(@QueryParam("id") Long admid,
pas\IPDAdmissionServiceImpl.java	/ipdadmission	DELETE	/removeMLCPhoto	1	public void removeMLCPhoto(@QueryParam("id") Long admid,
pas\IPDAdmissionServiceImpl.java	/ipdadmission	GET	/checkPatientExpired	1	public boolean checkPatientExpired(@QueryParam("patientId") Long patientId) throws GenericApplicationException 
pas\IPDAdmissionServiceImpl.java	/ipdadmission	GET	/getActiveAdmissionOfRegisteredPatient	0	public List<GetActiveAdmissionOfRegisteredPatientResultDto> getActiveAdmissionOfRegisteredPatient(@QueryParam("patientId") Long patientId,
pas\IPDAdmissionServiceImpl.java	/ipdadmission	GET	/getActiveVisitOfRegisteredPatient	0	public List<GetActiveAdmissionOfRegisteredPatientResultDto> getActiveVisitOfRegisteredPatient(@QueryParam("patientId") Long patientId, @QueryParam("patientUnitId") Long patientUnit
pas\IPDAdmissionServiceImpl.java	/ipdadmission	GET	/getAdmissionRequestedPatient	1	public
pas\IPDAdmissionServiceImpl.java	/ipdadmission	GET	/getAdmissionRequestToHIS	0	public Long getAdmissionRequestToHIS(@QueryParam("patientId") Long patientId,
pas\IPDAdmissionServiceImpl.java	/ipdadmission	GET	/getChargesByAdmIDAndAdmUnitID	0	public List<ChargesByAdmIDAndAdmUnitIDResultDto> getChargesByAdmIDAndAdmUnitID(@QueryParam("admId") @StoredProcNamedParam("p_admid") Long admId,
pas\IPDAdmissionServiceImpl.java	/ipdadmission	GET	/getSponsorByAdmissionID	0	public List<SponsorByAdmissionIDResultDto> getSponsorByAdmissionIDApi(@QueryParam("visitAdmissionId") @StoredProcNamedParam("p_visitadmissionid") Long visitAdmissionId,
pas\IPDAdmissionServiceImpl.java	/ipdadmission	GET	/getTransferRequestedPatient	0	public List<TransferRequestResultDto> getTransferRequestedPatient(@QueryParam("unitId") @StoredProcNamedParam("p_unitid") Long unitId) throws GenericApplicationException 
pas\IPDAdmissionServiceImpl.java	/ipdadmission	POST	/addAdmissionRequestToHIS	0	public Boolean addAdmissionRequestToHIS(@BodyParam("body") AdmissionRequesetBODto body) throws GenericApplicationException 
pas\IPDAdmissionServiceImpl.java	/ipdadmission	POST	/addAdmMLCDetails	0	public AdmMLCDetailsDto addAdmMLCDetails(@BodyParam("body") AdmMLCDetailsDto body) throws GenericApplicationException 
pas\IPDAdmissionServiceImpl.java	/ipdadmission	POST	/addAdvisedDischargeList	0	public AdvisedDischargeListDto addAdvisedDischargeList(@BodyParam("body") AdvisedDischargeListDto body) throws GenericApplicationException 
pas\IPDAdmissionServiceImpl.java	/ipdadmission	POST	/getPatientAdmissionList	0	public List<PatientAdmissionListResultDto> getPatientAdmissionList(@BodyParam("body") PatientAdmissionListDto body) throws GenericApplicationException 
pas\IPDAdmissionServiceImpl.java	/ipdadmission	POST	/getPatientAdmissionList_improved	0	public List<PatientAdmissionListResultDto_New> getPatientAdmissionList_improved(@BodyParam("body") PatientAdmissionListDto_New body) throws GenericApplicationException 
pas\IPDAdmissionServiceImpl.java	/ipdadmission	POST	/getPatientAdmissionListByDate	0	public List<PatientAdmissionListByDateResultDto> getPatientAdmissionListByDate(@BodyParam("body") PatientAdmissionListDto body) throws GenericApplicationException 
pas\IPDAdmissionServiceImpl.java	/ipdadmission	POST	/getPatientAdmissionListForClearance	0	public List<PatientAdmissionListResultDto> getPatientAdmissionListForClearance(@BodyParam("body") PatientAdmissionListDto body, @QueryParam("clearanceType") String clearanceType)
pas\IPDAdmissionServiceImpl.java	/ipdadmission	PUT	/updateBedOccupiedFlag	0	public UpdateBedOccupiedFlagDto updateBedOccupiedFlag(@BodyParam("body") UpdateBedOccupiedFlagDto body) throws GenericApplicationException 
pas\IPDConfigServiceImpl.java	/ipdconfig	DELETE	/deleteBedAmmenities	0	public GenericResultDto deleteBedAmmenities(@BodyParam("body") BedAmmenitiesDto body) throws GenericApplicationException 
pas\IPDConfigServiceImpl.java	/ipdconfig	DELETE	/deleteDietPlanDetails	0	public GenericResultDto deleteDietPlanDetails(@BodyParam("body") DietPlanDetailsDto body) throws GenericApplicationException 
pas\IPDConfigServiceImpl.java	/ipdconfig	GET	/getBedMasterAmmenitiesDetails	0	public List<BedMasterAmmenitiesDetailsResultDto> getBedMasterAmmenitiesDetails(@QueryParam("id") @StoredProcNamedParam("p_id") Long id) throws GenericApplicationException 
pas\IPDConfigServiceImpl.java	/ipdconfig	GET	/getClassOrderDetails	0	public List<ClassOrderDetailsResultDto> getClassOrderDetails(@QueryParam("classIds") @StoredProcNamedParam("p_classids") String classIds) throws GenericApplicationException 
pas\IPDConfigServiceImpl.java	/ipdconfig	GET	/getCodeTypeListByApplicableOn	0	public List<CodeTypeListByApplicableOnResultDto> getCodeTypeListByApplicableOn(@QueryParam("applicableOn") @StoredProcNamedParam("p_applicableon") Long applicableOn)
pas\IPDConfigServiceImpl.java	/ipdconfig	GET	/getDietPlanDetails	0	public List<GetDietPlanDetailsResultDto> getDietPlanDetails(@QueryParam("id") @StoredProcNamedParam("p_id") Long id) throws GenericApplicationException 
pas\IPDConfigServiceImpl.java	/ipdconfig	GET	/getDietPlanDetailsUsingID	0	public List<DietPlanDetailsUsingIDResultDto> getDietPlanDetailsUsingID(@QueryParam("id") @StoredProcNamedParam("p_id") Long id) throws GenericApplicationException 
pas\IPDConfigServiceImpl.java	/ipdconfig	GET	/getFoodItems	0	public GenericResultDto getFoodItems(@QueryParam("foodName") @StoredProcNamedParam("p_foodname") String foodName, @QueryParam("id") @StoredProcNamedParam("p_id") String id)
pas\IPDConfigServiceImpl.java	/ipdconfig	GET	/getRoomMasterAmmenitiesDetails	0	public List<RoomMasterAmmenitiesDetailsResultDto> getRoomMasterAmmenitiesDetails(@QueryParam("id") @StoredProcNamedParam("p_id") Long id) throws GenericApplicationException 
pas\IPDConfigServiceImpl.java	/ipdconfig	POST	/addBedAmmenitiesDetails	0	public GenericResultDto addBedAmmenitiesDetails(@BodyParam("body") BedAmmenitiesDetailsDto body) throws GenericApplicationException 
pas\IPDConfigServiceImpl.java	/ipdconfig	POST	/addDietPlanMaster	0	public AddDietPlanMasterDto addDietPlanMaster(@BodyParam("body") AddDietPlanMasterDto body) throws GenericApplicationException 
pas\IPDConfigServiceImpl.java	/ipdconfig	POST	/addUpdateBedMaster	0	public UpdateBedMasterDto addUpdateBedMaster(@BodyParam("body") UpdateBedMasterDto body) throws GenericApplicationException 
pas\IPDConfigServiceImpl.java	/ipdconfig	POST	/addUpdateBlockMaster	0	public UpdateBlockMasterDto addUpdateBlockMaster(@BodyParam("body") UpdateBlockMasterDto body) throws GenericApplicationException 
pas\IPDConfigServiceImpl.java	/ipdconfig	POST	/addUpdateClassMaster	0	public UpdateClassMasterDto addUpdateClassMaster(@BodyParam("body") UpdateClassMasterDto body) throws GenericApplicationException 
pas\IPDConfigServiceImpl.java	/ipdconfig	POST	/addUpdateDietPlanDetails	0	public UpdateDietPlanDetailsDto addUpdateDietPlanDetails(@BodyParam("body") UpdateDietPlanDetailsDto body) throws GenericApplicationException 
pas\IPDConfigServiceImpl.java	/ipdconfig	POST	/addUpdateFoodItemMaster	0	public UpdateFoodItemMasterDto addUpdateFoodItemMaster(@BodyParam("body") UpdateFoodItemMasterDto body) throws GenericApplicationException 
pas\IPDConfigServiceImpl.java	/ipdconfig	POST	/addUpdateRoomMaster	0	public UpdateRoomMasterDto addUpdateRoomMaster(@BodyParam("body") UpdateRoomMasterDto body) throws GenericApplicationException 
pas\IPDConfigServiceImpl.java	/ipdconfig	POST	/addUpdateWardMaster	0	public UpdateWardMasterDto addUpdateWardMaster(@BodyParam("body") UpdateWardMasterDto body) throws GenericApplicationException 
pas\IPDConfigServiceImpl.java	/ipdconfig	POST	/getBedCensusAndNonCensusList	1	public List<BedCensusAndNonCensusListResultDto> getBedCensusAndNonCensusList(@BodyParam("body") BedCensusAndNonCensusListDto body) throws GenericApplicationException 
pas\IPDConfigServiceImpl.java	/ipdconfig	POST	/getBedMasterDetails	0	public BedMasterDetailsDto getBedMasterDetails(@BodyParam("body") BedMasterDetailsDto body) throws GenericApplicationException 
pas\IPDConfigServiceImpl.java	/ipdconfig	POST	/getBlockMasterDetails	0	public BlockMasterDetailsDto getBlockMasterDetails(@BodyParam("body") BlockMasterDetailsDto body) throws GenericApplicationException 
pas\IPDConfigServiceImpl.java	/ipdconfig	POST	/getClassMasterDetails	1	public List<ClassMasterDetailsResultDto> getClassMasterDetails(@BodyParam("body") ClassMasterDetailsDto body) throws GenericApplicationException 
pas\IPDConfigServiceImpl.java	/ipdconfig	POST	/getDietPlan	0	public DietPlanDto getDietPlan(@BodyParam("body") DietPlanDto body) throws GenericApplicationException 
pas\IPDConfigServiceImpl.java	/ipdconfig	POST	/getFoodItemList	1	public FoodItemListDto getFoodItemList(@BodyParam("body") FoodItemListDto body) throws GenericApplicationException 
pas\IPDConfigServiceImpl.java	/ipdconfig	POST	/getIPDBedList	0	public List<IPDBedListResultDto> getIPDBedList(@BodyParam("body") IPDBedListDto body) throws GenericApplicationException 
pas\IPDConfigServiceImpl.java	/ipdconfig	POST	/getRoomMasterDetails	0	public List<RoomMasterDetailsResultDto> getRoomMasterDetails(@BodyParam("body") RoomMasterDetailsDto body) throws GenericApplicationException 
pas\IPDConfigServiceImpl.java	/ipdconfig	POST	/getWardMasterDetails	0	public List<WardMasterDetailsResultDto> getWardMasterDetails(@BodyParam("body") WardMasterDetailsDto body) throws GenericApplicationException 
pas\IPDConfigServiceImpl.java	/ipdconfig	PUT	/updateBedMasterStatus	0	public BedMasterStatusDto updateBedMasterStatus(@BodyParam("body") BedMasterStatusDto body) throws GenericApplicationException 
pas\IPDConfigServiceImpl.java	/ipdconfig	PUT	/updateBlockMasterStatus	0	public BlockMasterStatusDto updateBlockMasterStatus(@BodyParam("body") BlockMasterStatusDto body) throws GenericApplicationException 
pas\IPDConfigServiceImpl.java	/ipdconfig	PUT	/updateClassMasterStatus	1	public ClassMasterStatusDto updateClassMasterStatus(@BodyParam("body") ClassMasterStatusDto body) throws GenericApplicationException 
pas\IPDConfigServiceImpl.java	/ipdconfig	PUT	/updateDietPlanMaster	0	public DietPlanMasterDto updateDietPlanMaster(@BodyParam("body") DietPlanMasterDto body) throws GenericApplicationException 
pas\IPDConfigServiceImpl.java	/ipdconfig	PUT	/updateDietPlanMasterStatus	0	public DietPlanMasterStatusDto updateDietPlanMasterStatus(@BodyParam("body") DietPlanMasterStatusDto body) throws GenericApplicationException 
pas\IPDConfigServiceImpl.java	/ipdconfig	PUT	/updateFoodItemMasterStatus	0	public FoodItemMasterStatusDto updateFoodItemMasterStatus(@BodyParam("body") FoodItemMasterStatusDto body) throws GenericApplicationException 
pas\IPDConfigServiceImpl.java	/ipdconfig	PUT	/updateRoomMasterStatus	0	public RoomMasterStatusDto updateRoomMasterStatus(@BodyParam("body") RoomMasterStatusDto body) throws GenericApplicationException 
pas\IPDConfigServiceImpl.java	/ipdconfig	PUT	/updateWardMasterStatus	0	public WardMasterStatusDto updateWardMasterStatus(@BodyParam("body") WardMasterStatusDto body) throws GenericApplicationException 
pas\IPDServiceImpl.java	/ipd	POST	/getDischargeList	0	public List<DischargeListResultDto> getDischargeList(@BodyParam("body") DischargeListDto body) throws GenericApplicationException 
pas\IPDServiceImpl.java	/ipd	POST	/getDischargeListByDate	0	public List<DischargeListResultDto> getDischargeListByDate(@BodyParam("body") DischargeListDto body) throws GenericApplicationException 
pas\ItemCategoryServiceImpl.java	/itemcategory	GET	/getItemByCategoryId	0	public List<ItemByItemCategoryResultDto> getItemsByCategoryID(@QueryParam("id") @StoredProcNamedParam("p_itemcategory") Long id) throws GenericApplicationException 
pas\ItemMasterServiceImpl.java	/itemmaster	GET	/addItemtoItemClinic	1	public ItemClinicDto addItemToItemClinic(@QueryParam("itemId") Long itemId, @QueryParam("brandIds") String brandIds,@QueryParam("itemMstId") Long itemMstId) throws GenericApplicati
pas\ItemMasterServiceImpl.java	/itemmaster	GET	/getBrandMappedByItemId	0	public List<Brandmaster> getBrandMappedByItemId(@QueryParam("itemId") Long itemId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\ItemMasterServiceImpl.java	/itemmaster	GET	/getItemByName	0	public List<ItemByNameResultDto> getItemByName(@QueryParam("searchText") @StoredProcNamedParam("p_searchtext") String itemName,
pas\ItemMasterServiceImpl.java	/itemmaster	GET	/getItemByNameAndUnitId	1	public List<ItemByNameResultDto> getItemByNameAndUnitId(@QueryParam("searchText") @StoredProcNamedParam("p_searchtext") String itemName,
pas\ItemMasterServiceImpl.java	/itemmaster	GET	/getItemByNameForCSSD	0	public CSSDIndentView getItemByNameForCSSD(@QueryParam("unitId") @StoredProcNamedParam("p_unitid") Long unitId, @QueryParam("storeId") @StoredProcNamedParam("p_storeid") Long store
pas\ItemMasterServiceImpl.java	/itemmaster	GET	/getItemByNameIsAsset	0	public List<ItemByNameImprovedResultDto> getItemByNameIsAsset(@QueryParam("searchText") @StoredProcNamedParam("p_searchtext") String searchText,
pas\ItemMasterServiceImpl.java	/itemmaster	GET	/getItemDetailsByItemId	0	public
pas\ItemMasterServiceImpl.java	/itemmaster	GET	/getItemNameForMedGas	0	public List<Equipmentdetail> getItemNameForMedGas(@QueryParam("searchText") @StoredProcNamedParam("p_searchtext") String itemName,
pas\ItemMasterServiceImpl.java	/itemmaster	GET	/getItemSupplierList	0	public List<ItemSupplier> getItemSupplierList(@QueryParam("id") @StoredProcNamedParam("p_id") Long id, @QueryParam("unitId") @StoredProcNamedParam("p_unitid") Long unitId)
pas\ItemMasterServiceImpl.java	/itemmaster	GET	/getItemTaxList	1	public List<ItemTaxListResultDto> getItemTaxList(@QueryParam("id") @StoredProcNamedParam("p_id") Long id, @QueryParam("unitId") @StoredProcNamedParam("p_unitid") Long unitId)
pas\ItemMasterServiceImpl.java	/itemmaster	GET	/getKitItemByKitIdForCSSD	0	public List<GetKitItemByKitIdCSSDIndentResultDto> getKitItemByKitIdCSSDIndentResultDto(@QueryParam("unitId") @StoredProcNamedParam("p_unitid") Long unitId,
pas\ItemMasterServiceImpl.java	/itemmaster	GET	/saveItemBrandMapping	1	public void saveItemBrandMapping(@QueryParam("itemId") Long itemId, @QueryParam("brandIds") String brandIds) throws GenericApplicationException 
pas\ItemMasterServiceImpl.java	/itemmaster	POST	/addEquipmentDetailMaster	0	public EquipmentDetailMasterDto addEquipmentDetailMaster(@BodyParam("body") EquipmentDetailMasterDto body) throws GenericApplicationException 
pas\ItemMasterServiceImpl.java	/itemmaster	POST	/addInsuranceDetails	0	public InsuranceDetailsDto addInsuranceDetails(@BodyParam("body") InsuranceDetailsDto body) throws GenericApplicationException 
pas\ItemMasterServiceImpl.java	/itemmaster	POST	/addtemEnquiry	0	public TemEnquiryDto addtemEnquiry(@BodyParam("body") TemEnquiryDto temEnquiryDto) throws GenericApplicationException 
pas\ItemMasterServiceImpl.java	/itemmaster	POST	/getAllEnquirys	0	public List<GetAllEnquiriesResultDto> getAllEnquirys(@BodyParam("body") ItemsEnquiryDto body, @QueryParam("maximumRows") Long maxRows, @QueryParam("startRowIndex") Long startIndex)
pas\ItemMasterServiceImpl.java	/itemmaster	POST	/getItemListByItemName	1	public List<ItemListByItemNameResultDto> getItemListByItemName(@BodyParam("body") ItemListByItemNameDto body) throws GenericApplicationException 
pas\ItemMasterServiceImpl.java	/itemmaster	POST	/getItemListForSearch	0	public ItemListForSearchDto getItemListForSearch(@BodyParam("body") ItemListForSearchDto body) throws GenericApplicationException 
pas\ItemMasterServiceImpl.java	/itemmaster	POST	/getItemMst	0	public List<DepartmentMst> getDepartmentsByUnit(@BodyParam("body") Departmentmaster body) throws GenericApplicationException 
pas\ItemMasterServiceImpl.java	/itemmaster	POST	/getScrapSaleDetails	0	public List<ScrapSaleDetailsResultDto> getScrapSaleDetails(@BodyParam("body") ScrapSaleDetailsDto body) throws GenericApplicationException 
pas\ItemMasterServiceImpl.java	/itemmaster	POST	/syncJantaItemMaster	0	public Map<Long,Long> syncJantaItemMaster(@BodyParam("body") List<Itemmaster> body) throws GenericApplicationException, IOException, ParseException 
pas\ItemMasterServiceImpl.java	/itemmaster	PUT	/deleteItemmasterListById/{id}	0	public boolean deleteItemmasterListById(@PathParam("id") Long id) throws GenericApplicationException 
pas\ItemMasterServiceImpl.java	/itemmaster	PUT	/updateDiscardReasonById	1	public void updateDiscardReasonById(@QueryParam("id") Long id,@QueryParam("discardRejectionId") Long discardRejectionId) throws GenericApplicationException 
pas\ItemSalesServiceImpl.java	/itemsales	GET	/getAvailableItemStockStoreWise	0	public java.math.BigDecimal getAvailableItemStockStoreWise(@QueryParam("storeId") Long storeId, @QueryParam("itemId") Long itemId) throws GenericApplicationException 
pas\ItemSalesServiceImpl.java	/itemsales	GET	/getItemByStoreCategory	0	public List<Long> getItemByStoreCategory(@QueryParam("storeId") Long storeId) throws GenericApplicationException, JsonMappingException, JsonProcessingException 
pas\ItemSalesServiceImpl.java	/itemsales	GET	/getItemSaleByItemSale	0	public
pas\ItemSalesServiceImpl.java	/itemsales	GET	/getItemSalesReturnDetails	0	public List<ItemSalesReturnDetailsResultDto> getItemSalesReturnDetails(@QueryParam("id") @StoredProcNamedParam("p_id") Long id, @QueryParam("unitId") @StoredProcNamedParam("p_uniti
pas\ItemSalesServiceImpl.java	/itemsales	POST	/addItemSales	0	public ItemSalesDto addItemSales(@BodyParam("body") ItemSalesDto body) throws GenericApplicationException 
pas\ItemSalesServiceImpl.java	/itemsales	POST	/addItemSalesDetails	0	public AddItemSalesDetailsDto addItemSalesDetails(@BodyParam("body") AddItemSalesDetailsDto body) throws GenericApplicationException 
pas\ItemSalesServiceImpl.java	/itemsales	POST	/addtemSaleReturn	0	public TemSaleReturnDto addtemSaleReturn(@BodyParam("body") TemSaleReturnDto body) throws GenericApplicationException 
pas\ItemSalesServiceImpl.java	/itemsales	POST	/addtemSaleReturnDetails	0	public List<TemSaleReturnDetailsDto> addtemSaleReturnDetails(@BodyParam("body") TemSaleReturnDetailsDto body) throws GenericApplicationException 
pas\ItemSalesServiceImpl.java	/itemsales	POST	/getItemSales	1	public List<GetItemSalesResultDto> getItemSales(@BodyParam("body") GetItemSalesDto body, @QueryParam("page") Integer page,
pas\ItemSalesServiceImpl.java	/itemsales	POST	/getItemSalesDetails	0	public List<ItemSalesDetailsResultDto> getItemSalesDetails(@BodyParam("body") ItemSalesDetailsDto body) throws GenericApplicationException 
pas\ItemSalesServiceImpl.java	/itemsales	POST	/getItemSalesForCancelBill	0	public List<ItemSaleReturnsForIPResultDto> getItemSalesForCancelBill(@BodyParam("body") GetItemSaleForCancelBillDto body) throws GenericApplicationException 
pas\ItemSalesServiceImpl.java	/itemsales	POST	/getItemSalesForOT	0	public List<GetItemSalesOTResultDtoOld> getItemSalesForOT(@BodyParam("body") GetItemSalesDto body) throws GenericApplicationException 
pas\ItemSalesServiceImpl.java	/itemsales	POST	/getItemSalesReturn	1	public List<ItemSalesReturnResultDto1> getItemSalesReturn(@BodyParam("body") ItemSalesReturnDto body) throws GenericApplicationException 
pas\ItemSalesServiceImpl.java	/itemsales	POST	/getItemSalesReturnForIP	0	public
pas\ItemSalesServiceImpl.java	/itemsales	POST	/getItemsaleWholeDetailsByAdmId	0	public
pas\ItemStockServiceImpl.java	/itemstock	GET	/findIndentItemMovementDetails	0	public ItemUtilizationDetailsView findIndentItemUtilizationDetails(@QueryParam("indentId") Long indentId, @QueryParam("locationId") Long locationId,
pas\ItemStockServiceImpl.java	/itemstock	GET	/getIndentItemMovementDetails	0	public Map<Long,List<ItemUtilizationView>> getIndentItemUtilization(@QueryParam("indentId") Long indentId, @QueryParam("locationId") Long locationId,
pas\ItemStockServiceImpl.java	/itemstock	GET	/getItemBySupplierAndStore	1	public List<ItemsBySupplierStoreResultDto> getItemBySupplierAndStore(@QueryParam("supplierId") @StoredProcNamedParam("p_supplierid") Long supplierId,
pas\ItemStockServiceImpl.java	/itemstock	GET	/getItemRCDetails	0	public List<RateContractView> getItemRCComparisonDeatils(@QueryParam("itemId") Long itemId) throws GenericApplicationException 
pas\ItemStockServiceImpl.java	/itemstock	POST	/addItemStock	0	public List<ItemStockDto> saveItemStock(@BodyParam("body") ItemStockDto body) throws GenericApplicationException 
pas\ItemStockServiceImpl.java	/itemstock	POST	/getAllItemCategory	1	public List<Itemcategory> getAllItemCategory() throws GenericApplicationException 
pas\ItemStockServiceImpl.java	/itemstock	POST	/getItemBatchListForSearchWithTax	1	public List<ItemBatchListForSearchwithTaxResultDto> getItemBatchListForSearchWithTax(@BodyParam("body") ItemBatchListForSearchDto body, @QueryParam("isFromApp") @DefaultValue("fals
pas\ItemStockServiceImpl.java	/itemstock	POST	/getItemBatchListForSearchWithTaxForPo	0	public List<ItemBatchListForSearchwithTaxResultDto> getItemBatchListForSearchWithTaxForPo(@BodyParam("body") ItemBatchListForSearchDto body) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	GET	/authenticateDisplayUnit	1	public DisplayUnitMaster authenticateDisplayUnit(@QueryParam("key") String key) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	GET	/authenticateUnit	0	public Unitmaster authenticateUnit(@QueryParam("key") String key) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	GET	/checkChartSequence	0	public Number checkChartSequence(@QueryParam("chartType") String chartType, @QueryParam("sequence") Long sequence, @QueryParam("id") Long id) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	GET	/checkChartService	0	public Number checkChartService(@QueryParam("chartType") String chartType, @QueryParam("serviceName") String serviceName, @QueryParam("id") Long id) throws GenericApplicationExcept
pas\MasterServiceImpl.java	/masterConfigService	GET	/checkDescriptionInCasetypeMaster	1	public Number checkDescriptionInCasetypeMaster(@QueryParam("description") String description, @QueryParam("id") Long id) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	GET	/checkDescriptionInHolidayMaster	1	public Number checkDescriptionInHolidayMaster(@QueryParam("description") String description, @QueryParam("date") String date, @QueryParam("id") Long id) throws GenericApplicationEx
pas\MasterServiceImpl.java	/masterConfigService	GET	/checkIfItemTransaction	0	public Boolean checkIfItemTransaction(@QueryParam("itemId") Long itemId) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	GET	/checkSubSpecialization	1	public Number checkSubSpecialization(@QueryParam("description") String description, @QueryParam("fkSpecializationId") Long fkSpecializationId) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	GET	/checkUnitwiseItemNameForAsset	0	public Number checkUnitwiseItemNameForAsset(@QueryParam("itemName") String itemName, @QueryParam("id") Long id) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	GET	/createBloodStoreSequences	1	public void createBloodStoreSequences(@QueryParam("storeId") Long storeId) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	GET	/editProcedureCheklistDetail/{id}	1	public Procedurecheklistdetails editProcedureCheklistDetail(@PathParam("id") Long id) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	GET	/exportRuleSetFile	0	public void exportBillServices(@QueryParam("ruleId") Long ruleId, @Context HttpServletResponse response) throws GenericApplicationException, FileNotFoundException 
pas\MasterServiceImpl.java	/masterConfigService	GET	/getAllDiagnosisMaster	1	public List<DiagnosisMaster> getAllDiagnosisMaster(@QueryParam("page") Integer page, @QueryParam("count") Integer count, @QueryParam("status") String status,
pas\MasterServiceImpl.java	/masterConfigService	GET	/getCentralStoreList	0	public List<Store> getCentralStoreList() throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	GET	/getChartDetailsById/{id}	0	public List<Growthchartdetails> getChartDetailsById(@PathParam("id") Long id) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	GET	/getChartMenuById/{id}	1	public GrowthChartMenu getChartMenuById(@PathParam("id") Long id) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	GET	/getClinicalIEN	0	public Number getClinicalIEN(@QueryParam("clinicid") Long clinicId,@QueryParam("id") Long id) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	GET	/getDiagnosisMaster/{id}	0	public DiagnosisMaster getAllDiagnosisMaster(@PathParam("id") Long id) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	GET	/getDietTypeList	0	public List<Servicemaster> getDietTypeList(@QueryParam("pageStr") Integer pageStr,@QueryParam("countStr") Integer countStr,
pas\MasterServiceImpl.java	/masterConfigService	GET	/getDrugInteractionList	1	public List<Druginteractions> getDrugInteractionList(@QueryParam("page") Integer page, @QueryParam("count") Integer count, @QueryParam("search") String search, @QueryParam("status"
pas\MasterServiceImpl.java	/masterConfigService	GET	/getEmailTemplateDetailsByTemplateId	0	public EmailTemplateDetails getEmailTemplateDetailsByTemplateId(@QueryParam("templateId") Long templateId, @QueryParam("unitId") Long unitId)throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	GET	/getEmailtemplates	0	public List<EmailTemplate> getEmailtemplates(@QueryParam("page") Integer page, @QueryParam("count") Integer count, @QueryParam("search") String search,
pas\MasterServiceImpl.java	/masterConfigService	GET	/getGrievanceSeverityTemplateDetailsById	0	public GrievanceSeverityTemplateDetails getGrievanceSeverityTemplateDetailsById(@QueryParam("severityId") Long severityId)throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	GET	/getGrowthChartMenuList	1	public List<GrowthChartMenu> getGrowthChartMenuList(@QueryParam("pageStr") Integer pageStr,@QueryParam("countStr") Integer countStr,@QueryParam("status") boolean status) throws Gen
pas\MasterServiceImpl.java	/masterConfigService	GET	/getIHMSMasters	1	public AllMasterDataView getIHMSMasters() throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	GET	/getImmunAgeGrpMasterById/{id}	1	public Immunizationagegroupmaster getImmunAgeGrpMasterById(@PathParam("id") Long id) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	GET	/getInboundList/{msgid}	0	public Inboundhis getInboundList(@PathParam("msgid") Long msgid) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	GET	/getItemGroupByAsset	1	public List<Itemgroup> getItemGroupByAsset() throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	GET	/getItemMasterById/{id}	0	public Itemmaster getItemMasterById(@PathParam("id") Long id) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	GET	/getItemmasterListAsset	0	public List<Itemmaster> getItemmasterListAsset(@QueryParam("search") String search,@QueryParam("page") Integer pageStr,@QueryParam("count") Integer countStr,
pas\MasterServiceImpl.java	/masterConfigService	GET	/getListOfAuditEventIds	0	public List<String> getListOfAuditEventIds(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	GET	/getListOfInboundmsgs	1	public List<Inboundhis> getListOfInboundmsgs(@QueryParam("page") Integer page,
pas\MasterServiceImpl.java	/masterConfigService	GET	/getListOfOutboundmsgs	0	public List<Outboundhis> getListOfOutboundmsgs(@QueryParam("page") Integer page,
pas\MasterServiceImpl.java	/masterConfigService	GET	/getModuleNameMasterById	1	public String getModuleNameMasterDetails(@QueryParam("id") String moduleId) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	GET	/getOutboundList/{msgid}	0	public Outboundhis getOutboundList(@PathParam("msgid") Long msgid) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	GET	/getParentUnitList	1	public List<Unitmaster> getParentUnitList() throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	GET	/getSupplierListByCategory	0	public List<ItemSupplier> getSupplierListByCategory(@QueryParam("categoryId") Long categoryId, @QueryParam("unitId") Long unitId)
pas\MasterServiceImpl.java	/masterConfigService	GET	/getUnitsForAudits	0	public List<Unitmaster> getUnitsForAudits(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	GET	/getVirtualUnitList	0	public List<Unitmaster> getVirtualUnitList() throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	GET	/getVitalType	1	public List<Vitaltype> getVitalType(@QueryParam("page") Integer page, @QueryParam("count") Integer count,
pas\MasterServiceImpl.java	/masterConfigService	GET	/reports_mapping	1	public Map<String, String[]> getReportsMapping() 
pas\MasterServiceImpl.java	/masterConfigService	GET	/updateStoreIdInUnitMaster	0	public void updateStoreIdInUnitMaster(@QueryParam("eAParentStoreId") Long eAParentStoreId, @QueryParam("eAStoreId") Long eAStoreId) 
pas\MasterServiceImpl.java	/masterConfigService	POST	/addandUpdateChartDetails	0	public void addandUpdateChartDetails(@BodyParam("body") Growthchartdetails body) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	POST	/addandUpdateChartMenu	1	public void addandUpdateChartMenu(@BodyParam("body") GrowthChartMenu body) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	POST	/addAndUpdateImmunAgeGrpMaster	1	public void addAndUpdateImmunAgeGrpMaster(@BodyParam("body") Immunizationagegroupmaster body) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	POST	/addDiagnosisMaster	0	public DiagnosisMaster addDiagnosisMaster(@BodyParam("body") DiagnosisMaster ds) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	POST	/addDisplayUnitMaster	1	public DisplayUnitMaster addDisplayUnitMaster(@BodyParam("body") DisplayUnitMaster ds) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	POST	/addEmailTemplate	0	public AddEmailTemplateDto addEmailTemplate(@BodyParam("body") AddEmailTemplateDto body) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	POST	/addItemSupplierBycategory	0	public void addItemSupplierBycategory(@BodyParam("body") List<ItemSupplier> itemSupplierDtos) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	POST	/addPackageWithDefinationDetails	0	public Packagemaster addPackageWithDefinationDetails(@BodyParam("body") Packagemaster body) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	POST	/addProcedureCheklistDetail	1	public void OTprocedurechecklistitem(@BodyParam("body") Procedurecheklistdetails body) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	POST	/addUpdateEmailTemplateDetails	0	public EmailTemplateDetails addUpdateEmailTemplateDetails(@BodyParam("body") EmailTemplateDetails body) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	POST	/addUpdateGrievanceTemplateDetails	0	public GrievanceSeverityTemplateDetails addUpdateGrievanceTemplateDetails(@BodyParam("body") GrievanceSeverityTemplateDetails body) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	POST	/addUpdatePctsregistration	1	public void addUpdatePctsregistration(@BodyParam("body") Ptcregistration body) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	POST	/addUpdateSingleVitalType	0	public void addUpdateSingleVitalType(@BodyParam("body") List<Vitaltype> body) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	POST	/configureOfflineUnit	1	public OfflineDataDto configureOfflineUnit(@QueryParam("id") Long id, @QueryParam("cwfMaxId") Long cwfMaxId, @QueryParam("healthFactorMaxId") Long healthFactorMaxId) throws Generic
pas\MasterServiceImpl.java	/masterConfigService	POST	/getEmailTemplateListForSearch	0	public List<EmailTemplateListForSearchResultDto> getEmailTemplateListForSearch(@BodyParam("body") EmailTemplateListForSearchDto body) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	POST	/mapUnitwiseServiceForLMISIntegration	0	public Boolean mapUnitwiseServiceForLMISIntegration(@BodyParam("body") List<Long> body) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	POST	/printAudit	1	public AuditView printAudit(@BodyParam("body") Audit body,@QueryParam("page") String pageStr, @QueryParam("count") String countStr, @QueryParam("search") String searchStr,
pas\MasterServiceImpl.java	/masterConfigService	POST	/saveOutboundById	1	public void saveOutboundById(@BodyParam("body") Outboundhis body) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	POST	/updateDiagnosisMaster/{id}	1	public DiagnosisMaster updateDiagnosisMaster(@BodyParam("body") DiagnosisMaster ds, @PathParam("id") Long id) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	POST	/updateLanglabel	1	public void updateLanglabel(@BodyParam("body") Languagelabel body) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	PUT	/deleteBedMaster/{id}	0	public boolean deleteBedMaster(@PathParam("id") Long id) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	PUT	/deleteChartMenuById/{id}	0	public void deleteChartMenuById(@PathParam("id") Long id) throws GenericApplicationException
pas\MasterServiceImpl.java	/masterConfigService	PUT	/deleteHospitalDepartment/{id}	1	public boolean deleteHospitalDepartment(@QueryParam("isDepartment") Boolean masterName, @PathParam("id") Long id) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	PUT	/deleteImmuneAgeGroupById/{id}	0	public void deleteImmuneAgeGroupById(@PathParam("id") Long id) throws GenericApplicationException
pas\MasterServiceImpl.java	/masterConfigService	PUT	/deleteProcedureChecklistDetails/{id}	1	public void deleteProcedureChecklistDetails(@PathParam("id") Long id) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	PUT	/deleteWardBedMaster/{id}	1	public void deleteWardBedMaster(@PathParam("id") Long id, @QueryParam("masterName") String masterName) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	PUT	/updateDisplayUnitMaster/{id}	0	public DisplayUnitMaster updateDisplayUnitMaster(@BodyParam("body") DisplayUnitMaster ds, @PathParam("id") Long id) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	PUT	/updateInboundById/{msgid}	0	public void updateInboundById(@PathParam("msgid") Long msgid, @BodyParam("body") Inboundhis body) throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	PUT	checkIsAdministrativeExists	1	public boolean checkIsAdministrativeExists() throws GenericApplicationException 
pas\MasterServiceImpl.java	/masterConfigService	PUT	deleteDiagnosisMaster/{id}	1	public void deleteDiagnosisMaster(@PathParam("id") Long id) throws GenericApplicationException 
pas\MaterialConsumptionServiceImpl.java	/materialconsumption	GET	/getMaterialConsumptionItem	0	public
pas\MaterialConsumptionServiceImpl.java	/materialconsumption	POST	/getDepartmentalMaterialConsumption	0	public List<GetMaterialConsumptionResultDto> getDepartmentalMaterialConsumption(@BodyParam("body") GetMaterialConsumptionDto body, @QueryParam("maximumRows") Long maxRows,
pas\MaterialConsumptionServiceImpl.java	/materialconsumption	POST	/getMaterialConsumption	0	public List<GetMaterialConsumptionResultDto> getMaterialConsumption(@BodyParam("body") GetMaterialConsumptionDto body, @QueryParam("maximumRows") Long maxRows,
pas\NursingStationDefinationServiceImpl.java	/nursingstationdefination	GET	/getWardByNursingStation	0	public
pas\NursingStationDefinationServiceImpl.java	/nursingstationdefination	POST	/getNursingStationDefMaster	0	public List<NursingStationDefMasResultDto> getNursingStationDefMaster(@BodyParam("body") GetNursingStationDefMasterDto body) throws GenericApplicationException 
pas\OpeningBalanceServiceImpl.java	/openingbalance	POST	/addOpeningbalancedetails	0	public AddOpeningBalView getStockDetailsForOpeningBalance(@BodyParam("body") List<AddOpeningBalDetailsDto> body) throws GenericApplicationException 
pas\OpeningBalanceServiceImpl.java	/openingbalance	POST	/getEquipOpeningBalListCount	0	public List<OpeningBalanceCountListResutDto> getEquipOpeningBalListCount(@BodyParam("body") StockDetailsForOpeningBalanceDto body, @QueryParam("search") String search,
pas\OpeningBalanceServiceImpl.java	/openingbalance	POST	/getOpeningBalListForEquipment	1	public List<EquipmentOpeningBalanceResultDto> getOpeningBalListForEquipment(@BodyParam("body") StockDetailsForOpeningBalanceDto body, @QueryParam("search") String search,
pas\OpeningBalanceServiceImpl.java	/openingbalance	POST	/getStockDetailsForOpeningBalance	0	public StockDetailsOpeningBalanceResponse getStockDetailsForOpeningBalance(@BodyParam("body") StockDetailsForOpeningBalanceDto body, @QueryParam("search") String search,
pas\OpeningBalanceServiceImpl.java	/openingbalance	POST	/getStockDetailsForOpeningBalListCount	1	public List<OpeningBalanceCountListResutDto> getStockDetailsForOpeningBalListCount(@BodyParam("body") StockDetailsForOpeningBalanceDto body, @QueryParam("search") String search,
pas\OrganismMasterServiceImpl.java	/organismmaster	POST	/getOrganismDetailMaster	0	public List<GetOrganismMasterDetailResultDto> getOrganismDetailMaster(@BodyParam("body") OrganismDetailMasterByIdDto body) throws GenericApplicationException 
pas\OrganismMasterServiceImpl.java	/organismmaster	POST	/getOrganismMaster	0	public List<OrganismMasterResultDto_2> getOrganismMaster(@BodyParam("body") OrganismMasterDto body) throws GenericApplicationException 
pas\OtConfigServiceImpl.java	/otconfig	GET	/checkSurrgeryHeadsUnitWise	0	public Number checkSurrgeryHeadsUnitWise(@QueryParam("surgeryheadid") Long surgeryheadid) throws GenericApplicationException 
pas\OtConfigServiceImpl.java	/otconfig	GET	/checkUnitWiseForOTtable	0	public Map<String , Number> checkUnitWiseForOTtable(@QueryParam("description") String description) throws GenericApplicationException 
pas\OtConfigServiceImpl.java	/otconfig	GET	/checkUnitWiseForOTtheatre	0	public Map<String , Number> checkUnitWiseForOTtheatre(@QueryParam("description") String description) throws GenericApplicationException 
pas\OtConfigServiceImpl.java	/otconfig	GET	/getAllAvailableSlots	0	public List<String> getAllAvailableSlots(@QueryParam("operationTheaterId") Long operationTheaterId, @QueryParam("date") String fromTime) throws GenericApplicationException, ParseEx
pas\OtConfigServiceImpl.java	/otconfig	GET	/getAllUnmappedServices	0	public List<Servicemaster> getAllUnmappedServices(@QueryParam("description") String description) throws GenericApplicationException 
pas\OtConfigServiceImpl.java	/otconfig	GET	/getConsentForProcedureID	0	public
pas\OtConfigServiceImpl.java	/otconfig	GET	/getEquipmentName	0	public List<Itemmaster> getEquipmentName(@QueryParam("itemName") String itemName) throws GenericApplicationException 
pas\OtConfigServiceImpl.java	/otconfig	GET	/getIsMaintenanceDept	1	public List<Departmentmaster> getIsMaintenanceDept(@QueryParam("deptName") String deptName) throws GenericApplicationException 
pas\OtConfigServiceImpl.java	/otconfig	GET	/getKitName	0	public List<Itemmaster> getKitName(@QueryParam("itemName") String itemName) throws GenericApplicationException 
pas\OtConfigServiceImpl.java	/otconfig	GET	/getOTScheduleTime	0	public
pas\OtConfigServiceImpl.java	/otconfig	GET	/getOttableById/{id}	0	public Ottablemaster getOttableById(@PathParam("id") Long id) throws GenericApplicationException 
pas\OtConfigServiceImpl.java	/otconfig	GET	/getOttheatreById/{id}	1	public Ottheatremaster getOttheatreById(@PathParam("id") Long id) throws GenericApplicationException 
pas\OtConfigServiceImpl.java	/otconfig	GET	/getProcDetailsByProcID	0	public List<ProcDetailsByProcIDResultDto> getProcDetailsByProcID(@QueryParam("procId") @StoredProcNamedParam("p_procid") Long procId,
pas\OtConfigServiceImpl.java	/otconfig	GET	/getProcDetailsByProcIDForConsentDtls	0	public
pas\OtConfigServiceImpl.java	/otconfig	GET	/getProcDetailsByProcIDForInstruction	0	public
pas\OtConfigServiceImpl.java	/otconfig	GET	/getProcDetailsByProcIDForItemIssued	1	public
pas\OtConfigServiceImpl.java	/otconfig	GET	/getProcDetailsByProcIDForserviceDetails	0	public
pas\OtConfigServiceImpl.java	/otconfig	GET	/getProcedureById/{id}	0	public Proceduremaster getProcedureById(@PathParam("id") Long id) throws GenericApplicationException 
pas\OtConfigServiceImpl.java	/otconfig	GET	/getStaffForOTScheduling	0	public List<StaffForOTSchedulingResultDto> getStaffForOTScheduling() throws GenericApplicationException 
pas\OtConfigServiceImpl.java	/otconfig	POST	/addAndUpdateProcedureMaster	1	public void addAndUpdateProcedureMaster(@BodyParam("body") Proceduremaster body) throws GenericApplicationException 
pas\OtConfigServiceImpl.java	/otconfig	POST	/addConsentDetails	0	public ConsentDetailsDto addConsentDetails(@BodyParam("body") ConsentDetailsDto body) throws GenericApplicationException 
pas\OtConfigServiceImpl.java	/otconfig	POST	/addOperationTable	1	public Ottablemaster addOperationTable(@BodyParam("body") Ottablemaster body, @Context HttpServletRequest req) throws GenericApplicationException 
pas\OtConfigServiceImpl.java	/otconfig	POST	/addOperationTheatre	0	public Ottheatremaster addOperationTheatre(@BodyParam("body") Ottheatremaster body, @Context HttpServletRequest req) throws GenericApplicationException 
pas\OtConfigServiceImpl.java	/otconfig	POST	/AddOTDetailsInstruction	0	public void addOtDetailsForInstruction(@BodyParam("body") OtDetailsForInstructionResultDto body) throws GenericApplicationException 
pas\OtConfigServiceImpl.java	/otconfig	POST	/getPatientConsents	0	public
pas\OtConfigServiceImpl.java	/otconfig	POST	/getProcedureMasterList	1	public List<ProcedureMasterListResultDto> getProcedureMasterList(@BodyParam("body") ProcedureMasterListDto body) throws GenericApplicationException 
pas\OtConfigServiceImpl.java	/otconfig	POST	/getScheduleDetails	0	public OTSchedulerView getScheduleDetails(@QueryParam("patientId") Long patientId, @QueryParam("opd_ipd") Long opd_ipd, @QueryParam("scheduleDate") Date scheduleDate,
pas\OtConfigServiceImpl.java	/otconfig	POST	/getSurgeryDoctors	0	public List<ScheduledDoctorForOTSchedulingResultDto> getSurgeryDoctors(@BodyParam("body") ScheduledDoctorForOTSchedulingResultDto body) throws GenericApplicationException 
pas\OtConfigServiceImpl.java	/otconfig	PUT	/deleteProcedureById/{id}	0	public void deleteProcedureById(@PathParam("id") Long id) throws GenericApplicationException
pas\OtConfigServiceImpl.java	/otconfig	PUT	/updateOperationTable/{id}	1	public Ottablemaster updateOperationTable(@BodyParam("body") Ottablemaster body, @PathParam("id") Long id, @Context HttpServletRequest req) throws GenericApplicationException 
pas\OtConfigServiceImpl.java	/otconfig	PUT	/updateOperationTheatre/{id}	1	public Ottheatremaster updateOperationTheatre(@BodyParam("body") Ottheatremaster body, @PathParam("id") Long id, @Context HttpServletRequest req) throws GenericApplicationException
pas\OtConfigServiceImpl.java	/otconfig	PUT	/updateStatusOperationTable/{id}	1	public void updateStatusOperationTable(@PathParam("id") Long id, @Context HttpServletRequest req) throws GenericApplicationException 
pas\OtConfigServiceImpl.java	/otconfig	PUT	/updateStatusOperationTheatre/{id}	1	public void updateStatusOperationTheatre(@PathParam("id") Long id, @Context HttpServletRequest req) throws GenericApplicationException 
pas\OTDetailsServiceImpl.java	/otdetails	GET	/getConsentDetailsForConsetID	0	public List<ConsentDetailsForConsetIDResultDto> getConsentDetailsForConsetID(@QueryParam("consentId") @StoredProcNamedParam("p_consentid") Long consentId) throws GenericApplication
pas\OTDetailsServiceImpl.java	/otdetails	GET	/getOTDetailsForOTDetailsID	0	public List<OTDetailsForOTDetailsIDResultDto_1> getOTDetailsForOTDetailsID(@QueryParam("oTDetailsId") @StoredProcNamedParam("p_otdetailsid") Long oTDetailsId,
pas\OTDetailsServiceImpl.java	/otdetails	GET	/getOTDetailsForSurgery	0	public
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/addPacNoteIen	0	public void addPacNoteIen(@QueryParam("scheduleId") Long scheduleId, @QueryParam("pacNoteIen") Long pacNoteIen) 
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/checkAvailabilityOfBookingSlots	0	public List<OTBookingTimeSlotsDetailsDto> checkAvailabilityOfBookingSlots_backup(@QueryParam("startDateTime") Timestamp startDateTime, @QueryParam("endDateTime") Timestamp endDateT
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/checkDoctorAllreadyBookedForSameTime	0	public Boolean checkDoctorAllreadyBookedForSameTime(@QueryParam("startDateTime") Timestamp startDateTime, @QueryParam("endDateTime") Timestamp endDateTime,
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/checkOTAvailability	0	public List<OTScheduleTimeResultDto> checkOTAvailability(@QueryParam("unitId") @StoredProcNamedParam("p_unitid") Long unitId, @QueryParam("oTId") @StoredProcNamedParam("p_otid") Lo
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/checkOTSchedule	0	public Boolean checkOTSchedule(@QueryParam("operationTheaterId") Long operationTheaterId, @QueryParam("otDate") String otDate, @QueryParam("otStartTime") String otStartTime,
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/checkOtThreaterInOTTable	1	public Number checkOtThreaterInOTTable(@QueryParam("ottheatreid") Long ottheatreid) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/checkPatientAdmissionStatus	1	public List<ActiveAdmissionOfRegisteredPatientResultDto> getActiveAdmissionOfRegisteredPatient(@QueryParam("patientId") @StoredProcNamedParam("p_patientid") Long patientId,
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/checkPatientAllreadyBookedForSameTime	1	public List<OTBookingTimeSlotsDetailsDto> checkPatientAllreadyBookedForSameTime(@QueryParam("startDateTime") Timestamp startDateTime, @QueryParam("endDateTime") Timestamp endDateTi
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/checkPatientForOt	0	public Boolean checkPatientForOt(@QueryParam("UnitID") Long unitId, @QueryParam("patientAdmId") Long visitAdmId, @QueryParam("patientId") Long patientId) throws GenericApplicationE
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/geProcedureWiseEquipmentList	0	public List<GetProcedureEquipmentList> geProcedureWiseEquipmentList(@QueryParam("unitId") @StoredProcNamedParam("p_unitid") Long unitId) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/getDoctorBySurgeryHead	0	public List<OTSurgeryDoctorsDto> getDoctorBySurgeryHead(@QueryParam("surgeryHeadId") Long surgeryHeadId) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/getOrderedPatientProcedure	1	public List<PatientProcedureDto> getOrderedPatientProcedure(@QueryParam("patientId") Long patientId, @QueryParam("visitAdmId") Long visitAdmId, @QueryParam("opdIpd") Long opdIpd, @
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/getOtAnesthesiaDetails	1	public List<PatientProcedureAnesthesiadetails> getOtAnesthesiaDetails(@QueryParam("unitId") Long unitId, @QueryParam("patientProcedureId") Long patientProcedureId)
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/getotDetails	1	public AddPatientProcdureDto getotDetails(@QueryParam("patientId") Long patientId, @QueryParam("opdIpdId") Long opdIpdId) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/getOTDetailsSurgeryNotes	1	public List<OTDetailsSurgeryNotes> getOTDetailsSurgeryNotes(@QueryParam("unitId") @StoredProcNamedParam("p_unitid") Long unitId,
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/getOtEquipmentDetails	1	public List<PatientProcedureEquipmentDetailsResultDto> getOtEquipmentDetails(@QueryParam("unitId") Long unitId, @QueryParam("patientProcedureId") Long patientProcedureId)
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/getOTNotesDetails	0	public PatientNote getOTNotesDetails(@QueryParam("patientId") Long patientId, @QueryParam("opd_Ipd_Id") Long opd_Ipd_Id, @QueryParam("mrNo") Long mrNo, @QueryParam("OTDetailsId") L
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/getOTSchedulingListById/{id}	1	public Otschedule getOTSchedulingListById(@PathParam("id") Long id) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/getOTStaffDetails	0	public List<ScheduledStaffForOTSchedulingResultDto> getOTStaffDetails(@QueryParam("unitId") Long unitId, @QueryParam("patientProcedureId") Long patientProcedureId)
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/getOTSurgeryDocTypeList	0	public List<Surgeryheadmaster> getOTSurgeryDocTypeList() throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/getOtTablesByTheaterId	0	public Map<Long, String> getOtTablesByTheatreId(@QueryParam("unitId") Long unitId, @QueryParam("theaterId") Long theaterId) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/getPacDetailsByScheduleId	0	public PatientProcedureScheduleListResultDto getPacDetailsByScheduleId(@QueryParam("scheduleId") Long scheduleId) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/getPatientProcedureListByScheduleId	0	public List<PatientProcedureDto> getPatientProcedureListByScheduleId(@QueryParam("patientProcedureScheduleId") Long patientProcedureScheduleId) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/getPatientScheduleAnasthesiaDetails	1	public List<PatientScheduleAnasthesiaDetails> getPatientScheduleAnasthesiaDetails(@QueryParam("scheduleId") Long scheduleId) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/getPatientScheduleByVisitAdmId	1	public List<AddPatientProcdureDto> getPatientScheduleByVisitAdmId(@QueryParam("visitAdmId") Long visitAdmId) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/getPatientScheduleTheaterDetails	0	public PatientProcedureScheduleDto getPatientScheduleTheaterDetails(@QueryParam("admissionId") Long admissionId) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/getPendingPacScheduleByVisitAdmId	1	public List<AddPatientProcdureDto> getPendingPacScheduleByVisitAdmId(@QueryParam("visitAdmId") Long visitAdmId) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/getProcedureChecklist	0	public List<PatientProcedureDto> getProcedureChecklist(@QueryParam("admId") Long admId, @QueryParam("patId") Long patId, @QueryParam("ot_type") Long ot_type) throws GenericApplicat
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/getProcedureEquipmentList	1	public PatientProcedureDto GetProcedureEquipmentList(@QueryParam("procId") @StoredProcNamedParam("p_procid") Long procId,
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/getRetainBedId	0	public List<TransferResultDto> getRetainBedId(@QueryParam("admId") Long admId) throws GenericApplicationException
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/getServiceRate	0	public ServiceListForBillResultDto getServicePriceDetails(@QueryParam("service_code") String serviceCode, @QueryParam("mrno") String mRNO, @QueryParam("visit_adm_number") String vi
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/getSurgeryDetails	0	public UpdateOTSheetDetailsDto getSurgeryDetails(@QueryParam("OTDetailsId") Long OTDetailsId) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/getSurgeryDetailsNew	0	public List<GetSurgeryDetailsNewResultDto> getProcDetailsByProcID(@QueryParam("unitId") @StoredProcNamedParam("p_unitid") Long unitId,
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/getSurgeryHead	1	public List<SurgeryHeadDetail> getSurgeryHead(@QueryParam("UnitID") @StoredProcNamedParam("p_unitid") Long unitId) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/getSurgeryHeadDetailsForBillEstimation	0	public List<Surgeryheadmaster> getSurgeryHeadDetailsForBillEstimation(@QueryParam("unitId") Long unitId, @QueryParam("tariffId") Long tariffId, @QueryParam("classId") Long classId)
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/isCurrentProcedureismainprocedure	1	public boolean getisCurrentProcedureismainprocedure(@QueryParam("patientProcedureId") Long patientProcedureId, @QueryParam("procedureScheduleId") Long procedureScheduleId)
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/updatePreAnaesthesiaChk	1	public void updatePreAnaesthesiaChk(@QueryParam("UnitID") Long unitId, @QueryParam("id") Long id,
pas\OTSchedulerServiceImpl.java	/otscheduler	GET	/viewSurgeryCharges	1	public List<PatientProcedureDto> viewSurgeryCharges(@QueryParam("procedureScheduleId") Long procedureScheduleId) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	POST	/addanesthesiaDtlsForOtScheduling	0	public Boolean addanesthesiaDtlsForOtScheduling(@BodyParam("body") AllOTSchedulesDetailsDto body) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	POST	/addDoctorDetailsForOtScheduling	1	public Boolean addDoctorDetailsForOtScheduling(@BodyParam("body") AllOTSchedulesDetailsDto body) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	POST	/addNewPatientProcdure	0	public OTSchedulerView addNewPatientProcdure(@BodyParam("body") AddPatientProcdureDto body) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	POST	/addOTSchedulingList	0	public void addOTSchedulingList(@BodyParam("body") Otschedule body) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	POST	/addPatientProcdure	0	public UpdatePatientProcedureScheduleMasterDto addPatientProcdure(@BodyParam("body") AddPatientProcdureDto body) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	POST	/addPatientProcedureChecklist	0	public void addPatientProcedureChecklist(@BodyParam("body") List<PatientProcedureDto> body) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	POST	/addPatientScheduleAnasthesiaDetails	0	public void addPatientScheduleAnasthesiaDetails(@BodyParam("body") List<PatientScheduleAnasthesiaDetails> body) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	POST	/addProcedureEquipment	0	public void addProcedureEquipment(@BodyParam("patientProcedureDto") PatientProcedureDto patientProcedureDto) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	POST	/addStaffDetailsForOt	0	public List<UpdatePatientProcStaffDetailsDto> addStaffDetailsForOt(@BodyParam("body") AllOTSchedulesDetailsDto body) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	POST	/addSurgCharge	1	public SurgeryChargesDto getSurgServicePriceDetails(@BodyParam("body") Map<Long, List<SurgeryCompResultDto>> surgeryChargesDto, Long chargeId, Long classId) throws GenericApplicati
pas\OTSchedulerServiceImpl.java	/otscheduler	POST	/addSurgeryChargesTemp	1	public void addSurgeryChargesTemp(@BodyParam("body") Map<Long, List<SurgeryCompResultDto>> surgeryChargesDto) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	POST	/addUpdateOTDetailsSurgeryNotesDetails	1	public UpdateOTDetailsSurgeryNotesDetailsDto addUpdateOTDetailsSurgeryNotesDetails(@BodyParam("body") UpdateOTDetailsSurgeryNotesDetailsDto body) throws GenericApplicationException
pas\OTSchedulerServiceImpl.java	/otscheduler	POST	/checkEqpAvailability	0	public List<EqpAvlbResultForSurgScheduleDto> checkEquipAvailability(@BodyParam("body") EquipmentAvlbDetailsDto body) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	POST	/checkSurgeryComp	0	public List<CheckSurgeryCompDto> checkSurgeryComp(@BodyParam("body") AllOTSchedulesDetailsDto body) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	POST	/getDetailsForReschdule	0	public Boolean getDetailsForReschdule(@BodyParam("body") StaffListValidation body) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	POST	/getDoctorList	1	public List<DoctorTimeResultDto> GetProcedureEquipmentList(@BodyParam("body") OTDoctorSearchDto otDoctorSearchDto) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	POST	/getOTScheduleTime	0	public Map<java.util.Date, OTScheduleView> getOTScheduleTime(@BodyParam("body") OTScheduleSearch otScheduleSearch) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	POST	/getPatientProcedureRateDetails	1	public OTProcedureRateView getPatientProcedureRateDetails(@BodyParam("body") PatientProcedureDto body) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	POST	/getPatientProcedureScheduleList	0	public
pas\OTSchedulerServiceImpl.java	/otscheduler	POST	/getPatientProcedureWisePrescriptionItems	0	public List<GetConsumableItemsForProcedureResultDto> getPatientProcedureWisePrescriptionItems(@BodyParam("body") GetConsumableItemsForProcedureDto body) 
pas\OTSchedulerServiceImpl.java	/otscheduler	POST	/getProcedureMasterList	0	public List<ProcedureMasterListResultDto> getProcedureMasterList(@BodyParam("body") ProcedureMasterListDto body) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	POST	/getsurgCompList	0	public List<SurgeryCompResultDto> GetProcedureCompList(@BodyParam("body") SurgeryCompDto body) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	POST	/getSurgeryCompleteList	0	public List<SurgeryCompResultDto> getSurgeryCompleteList(@BodyParam("body") SurgeryCompDto body) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	POST	/getSurgeryHeadDetails	1	public List<SurgeryHeadDetailResult> getSurgeryHeadDetails(@BodyParam("body") SurgeryHeadDetailDto body) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	POST	/getSurgServicePriceDetails	0	public ServiceListForBillResultDto getSurgServicePriceDetails(@QueryParam("serviceId") Long serviceId, @QueryParam("classId") Long classId, @QueryParam("tariffId") Long tariffId,
pas\OTSchedulerServiceImpl.java	/otscheduler	POST	/printOTReport	0	public String printOTReport(@BodyParam("patientNote") PatientNote body)
pas\OTSchedulerServiceImpl.java	/otscheduler	POST	/printScheduleList	1	public OTSchedulerView printScheduleList(@BodyParam("body") PatientProcedureScheduleListDto body, @Context HttpServletResponse response)
pas\OTSchedulerServiceImpl.java	/otscheduler	POST	/removePatientProcedure	1	public OTSchedulerView removePatientProcdure(@BodyParam("body") PatientProcedureDto body) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	POST	/saveSurgeryRateDetails	0	public Boolean saveSurgeryRateDetails(@BodyParam("body") List<PatientProcedureRateDetails> body) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	POST	/setOTScheduleDetails	1	public AllOTSchedulesDetailsDto setOTScheduleDetails(@BodyParam("body") AllOTSchedulesDetailsDto body) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	POST	/updateOTNotes	0	public Boolean updateOTNotes(@BodyParam("patientNote") PatientNote body,@QueryParam("patientId") Long patientId, @QueryParam("noteTitle") String noteTitle, @QueryParam("patientProc
pas\OTSchedulerServiceImpl.java	/otscheduler	POST	/updateOTScheduleDetails	1	public UpdateOTSheetDetailsDto updateOTScheduleDetails(@BodyParam("body") UpdateOTSheetDetailsDto body) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	PUT	/cancelSurgeryPatient	1	public PatientProcedureScheduleListDto cancelSurgeryPatient(@QueryParam("patientId") Long patientId, @QueryParam("otDetailsId") Long otDetailsId,
pas\OTSchedulerServiceImpl.java	/otscheduler	PUT	/deleteOTSchedulingListById/{id}	0	public void deleteOTSchedulingListById(@PathParam("id") Long id) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	PUT	/getpatientProcedureScheduleByPatId	0	public void getpatientProcedureScheduleByPatId(@QueryParam("patientId") Long patientId, @QueryParam("visitAdmId") Long visitAdmId) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	PUT	/removeAnesthesia	1	public String removeAnesthesia(@QueryParam("id") Long id, @QueryParam("unitId") Long unitid) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	PUT	/removeScheduleDoctorDetails	1	public String removeScheduleDoctorDetails(@QueryParam("id") String patientProcDocScheduleDetailsId) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	PUT	/removeScheduleStaffDetails	1	public String removeScheduleStaffDetails(@QueryParam("id") String patientProcStaffDetailsId) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	PUT	/removeSelectedCssdItemDetail	0	public String removeSelectedCssdItemDetail(@QueryParam("id") String patientProcEquipmentDetailId) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	PUT	/removeSelectedEquipmentDetails	1	public String removeSelectedEquipmentDetails(@QueryParam("id") String patientProcEquipmentDetailId) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	PUT	/updateForItemProcedures	1	public String updateForItemProcedures(@QueryParam("admissionId") Long admissionId) throws GenericApplicationException 
pas\OTSchedulerServiceImpl.java	/otscheduler	PUT	/updateOTSchedulingList/{id}	1	public Otschedule updateOTSList(@BodyParam("body") Otschedule body, @PathParam("id") Long id) throws GenericApplicationException 
pas\PackageServiceServiceImpl.java	/packageservice	GET	/checkServiceInPackageMaster	0	public Packagehealthplandefinationmaster checkServiceInPackageMaster(@QueryParam("serviceid") Long serviceid) throws GenericApplicationException 
pas\PackageServiceServiceImpl.java	/packageservice	GET	/getGroupPackagesMst	0	public GetPackageGroupView getGroupPackagesMst(@QueryParam("packagehealthplandefid") Long packagehealthplandefid) throws GenericApplicationException 
pas\PackageServiceServiceImpl.java	/packageservice	GET	/getItemCategoryPackagesMst	0	public GetPackageItemCategory getItemCategoryPackagesMst(@QueryParam("packagehealthplandefid") Long packagehealthplandefid) throws GenericApplicationException 
pas\PackageServiceServiceImpl.java	/packageservice	GET	/getMedicinePackagesMst	0	public GetPackageMedicine getMedicinePackagesMst(@QueryParam("packagehealthplandefid") Long packagehealthplandefid) throws GenericApplicationException 
pas\PackageServiceServiceImpl.java	/packageservice	GET	/getPackageDetails	1	public List<Packagehealthplandefinationmaster> getPackageDetails(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\PackageServiceServiceImpl.java	/packageservice	GET	/getPackageDetailsById	1	public Packagehealthplandefinationmaster getPackageDetailById(@QueryParam("id") Long id) throws GenericApplicationException 
pas\PackageServiceServiceImpl.java	/packageservice	GET	/getPackageDetailsByUnitId	1	public List<Packagehealthplandefinationmaster> getPackageDetailsByUnitId(@QueryParam("unitId") Long unitId)
pas\PackageServiceServiceImpl.java	/packageservice	GET	/getPackageServiceMst/{id}	1	public Packagehealthplandefinationmaster getPackageServiceMst(@PathParam("id") Long id) throws GenericApplicationException 
pas\PackageServiceServiceImpl.java	/packageservice	GET	/getServiceDataInPackageMaster	0	public Packagehealthplandefinationmaster getServiceDataInPackageMaster(@QueryParam("id") Long id) throws GenericApplicationException 
pas\PackageServiceServiceImpl.java	/packageservice	GET	/getServicePackageMst	1	public GetServicePackageView getServicePackageMst(@QueryParam("packagehealthplandefid") Long packagehealthplandefid) throws GenericApplicationException 
pas\PackageServiceServiceImpl.java	/packageservice	GET	/gettariffListByserviceID	0	public List<Tariffmaster> getTariffListID(@QueryParam("serviceId") Long serviceId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\PackageServiceServiceImpl.java	/packageservice	GET	/getTariffServicePackageByServiceId	0	public Servicemaster getTariffServicePackageByServiceId(@QueryParam("id") Long id) throws GenericApplicationException 
pas\PackageServiceServiceImpl.java	/packageservice	POST	/addGroupPackagesMst	1	public void addGroupPackagesMst(@BodyParam("body") List<Servicepackagegroupmaster> body) throws GenericApplicationException 
pas\PackageServiceServiceImpl.java	/packageservice	POST	/addItemCategoryPackagesMst	1	public void addItemCategoryPackagesMst(@BodyParam("body") List<Packageitemcategorymaster> body) throws GenericApplicationException 
pas\PackageServiceServiceImpl.java	/packageservice	POST	/addMedicinePackagesMst	1	public void addMedicinePackagesMst(@BodyParam("body") List<Servicepackagemedicinedetails> body) throws GenericApplicationException 
pas\PackageServiceServiceImpl.java	/packageservice	POST	/addPackageDetails	0	public void addPackageDetails(@BodyParam("body") Packagemaster body) throws GenericApplicationException 
pas\PackageServiceServiceImpl.java	/packageservice	POST	/addPackagesMst	0	public void addPackagesMst(@BodyParam("body") Packagehealthplandefinationmaster body) throws GenericApplicationException 
pas\PackageServiceServiceImpl.java	/packageservice	PUT	/deletePackageDetailsById/{id}	0	public void deletePackageDetailById(@PathParam("id") Long id) throws GenericApplicationException 
pas\PackageServiceServiceImpl.java	/packageservice	PUT	/updateGroupPackagesMst	1	public List<Servicepackagegroupmaster> updateGroupPackagesMst(@BodyParam("body") List<Servicepackagegroupmaster> body) throws GenericApplicationException 
pas\PackageServiceServiceImpl.java	/packageservice	PUT	/updateItemCategoryPackagesMst	0	public List<Packageitemcategorymaster> updateItemCategoryPackagesMst(@BodyParam("body") List<Packageitemcategorymaster> body) throws GenericApplicationException 
pas\PackageServiceServiceImpl.java	/packageservice	PUT	/updateMedicinePackagesMst	1	public List<Servicepackagemedicinedetails> updateMedicinePackagesMst(@BodyParam("body") List<Servicepackagemedicinedetails> body) throws GenericApplicationException 
pas\PackageServiceServiceImpl.java	/packageservice	PUT	/updatePackageMstList	0	public Packagehealthplandefinationmaster updatePackageMstList(@BodyParam("body") Packagehealthplandefinationmaster body) throws GenericApplicationException 
pas\PatientBillEstimationServiceImpl.java	/patientbillestimation	GET	/getPatientBillEstimationDetails	0	public BillReceiptView getPatientBillEstimationDetails(@QueryParam("isPrint") Boolean isPrint,
pas\PatientBillEstimationServiceImpl.java	/patientbillestimation	POST	/addPatientBillEstimationAndDetails	1	public AddPatientBillEstimationView addPatientBillEstimationAndDetails(@BodyParam("body") AddPatientBillEstimationView body) throws GenericApplicationException 
pas\PatientBillEstimationServiceImpl.java	/patientbillestimation	POST	/getAllEstimatedBills	0	public List<GetAllBillPatientEstimationResultDto> getAllEstimatedBills(@BodyParam("body") GetAllBillPatientEstimationDto body) throws GenericApplicationException 
pas\PatientBillEstimationServiceImpl.java	/patientbillestimation	POST	/getAllServiceForBillEstimation	0	public List<ServiceListForBillResultDto> getAllServicesForBillEstimation(@BodyParam("body") SearchServiceDto search) throws GenericApplicationException 
pas\PatientRequisitionServiceImpl.java	/patientrequisition	GET	/checkAutologousTypeOrders	1	public PatientBloodRequisition checkAutologousTypeOrders(@QueryParam("patientId") Long patientId,@QueryParam("requestTypeId") Long requestTypeId, @QueryParam("encounterId") Long e
pas\PatientRequisitionServiceImpl.java	/patientrequisition	GET	/getPendingBloodRequisition	0	public List<PatientRequsitionListResultDto> getPendingBloodRequisition(@QueryParam("mrnNo") String mrnno, @QueryParam("opdIpdId") Long opdIpdId) throws GenericApplicationException
pas\PatientRequisitionServiceImpl.java	/patientrequisition	GET	/getRequestionDetailsById	0	public List<GetPatientRequisitionDetailsById> getRequestionDetailsById( @QueryParam("id") Long id, @QueryParam("unitId") Long unitId)
pas\PatientRequisitionServiceImpl.java	/patientrequisition	POST	/addBloodComponantForRequisition	0	public void addBloodComponantForRequisition(@BodyParam("body") AddBloodComponantForPatientRequisitionDto body) throws GenericApplicationException 
pas\PatientRequisitionServiceImpl.java	/patientrequisition	POST	/addPatientBloodRequisition	0	public PatientBloodRequisition addPatientBloodRequisition(@BodyParam("body") BloodRequisitionDetailsDto body) throws GenericApplicationException 
pas\PatientRequisitionServiceImpl.java	/patientrequisition	POST	/getBloodComponantsListForRequisition	1	public List<BloodCompListForReqResultDto> getBloodComponantsForRequisition(@QueryParam("patientID") @StoredProcNamedParam("p_patientid") Long patientID,
pas\PatientRequisitionServiceImpl.java	/patientrequisition	POST	/getCrossUnitOrderList	1	public BloodBagExtractionView getCrossUnitOrderList(@BodyParam("body") PatientRequsitionListDto body, @QueryParam("maximumRows") Long maxRows,
pas\PatientRequisitionServiceImpl.java	/patientrequisition	POST	/getPatientRequisitionList	1	public BloodBagExtractionView getPatientRequisitionList(@BodyParam("body") PatientRequsitionListDto body, @QueryParam("maximumRows") Long maxRows,
pas\PatientRequisitionServiceImpl.java	/patientrequisition	POST	/placeCrossUnitOrder	0	public PatientBloodRequisition placeCrossUnitOrder(@BodyParam("body") BloodRequisitionDetailsDto body) throws GenericApplicationException 
pas\PatientRequisitionServiceImpl.java	/patientrequisition	PUT	/updateBloodSampleCollectionStatusForRequisition	0	public boolean updateBloodSampleCollectionStatusForRequisition(@QueryParam("reasonId") Long reasonId, @QueryParam("reqId") Long reqId, @QueryParam("reqUnitId") Long reqUnitId, @Que
pas\PatientServiceImpl.java	/patient	DELETE	/deallocateShiftDetails	0	public void deallocateShiftDetails(@BodyParam("body") UserShiftDetails body) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	GET	/{filename}	0	public void servePdf(@PathParam("filename") String filename, @Context HttpServletResponse response) throws GenericApplicationException, IOException 
pas\PatientServiceImpl.java	/patient	GET	/addIPDOPDPatientToQ	1	public void addIPDPatientToQ(@QueryParam("isAdmission") boolean isAdmission, @QueryParam("mrnno") String mrnNo, @QueryParam("departmentId") Long departmentId) throws GenericApplica
pas\PatientServiceImpl.java	/patient	GET	/addPatientAbhNumberKey	0	public void addPatientAbhNumberKey(@QueryParam("abhanumber") String abhanumber, @QueryParam("patientId") Long patientId) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	GET	/checkMotherhasChildAdmitted	1	public List<Long> checkMotherhasChildAdmitted(@QueryParam("admId") Long admId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	GET	/checkMotherhasChildAdmittedOnCradle	0	public List<Long> checkMotherhasChildAdmittedOnCredle(@QueryParam("admId") Long admId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	GET	/checkMotherhasChildAdmittedOnSameBed	0	public List<Long> checkMotherhasChildAdmittedOnSameBed(@QueryParam("admId") Long admId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	GET	/checkshiftdetails	0	public List<UserShiftDetailsDto> checkshiftdetails(@QueryParam("userId") Integer userId, @QueryParam("shiftStartDate") String shiftStartDate, @QueryParam("shiftEndDate") String shi
pas\PatientServiceImpl.java	/patient	GET	/getActiveAdmissionOfPatientForInquiry	0	public List<InquiryDetailsResultDto> getActiveAdmissionOfPatientForInquiry(@QueryParam("patientId") Long patientId, @QueryParam("patientUnitId") Long patientUnitId) throws GenericA
pas\PatientServiceImpl.java	/patient	GET	/getActiveAdmissionOfRegisteredPatient	1	public List<ActiveAdmissionOfRegisteredPatientResultDto> getActiveAdmissionOfRegisteredPatient(@QueryParam("patientId") @StoredProcNamedParam("p_patientid") Long patientId,
pas\PatientServiceImpl.java	/patient	GET	/getAllHospitalEvents	0	public List<Events> getAllHospitalEvents(@QueryParam("unitId") Long unitId, @QueryParam("type") Long type) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	GET	/getAnmAncVisitDetails	1	public JsyFormDetailsView getAnmAncVisitDetails(@QueryParam("patientId") Long patientId) 
pas\PatientServiceImpl.java	/patient	GET	/getBabiesMotherAdmissionStatus	0	public List<ActiveAdmissionOfRegisteredPatientResultDto> getBabiesMotherAdmissionStatus(@QueryParam("patientId") Long patientId, @QueryParam("unitId") Long unitId,
pas\PatientServiceImpl.java	/patient	GET	/getBiometricScanDetails	0	public String getBiometricScanDetails() throws IOException 
pas\PatientServiceImpl.java	/patient	GET	/getChiranjeeviDetailByJanAdhar	0	public List<ChiranjeeviJanadharBasedDto> getChiranjeeviDetailByJanAdhar(@QueryParam("mrno") String mrno) throws GenericApplicationException, IOException
pas\PatientServiceImpl.java	/patient	GET	/getChiranjeeviDocumentByTid	0	public Object getChiranjeeviDocumentByTid(@QueryParam("tid") String tid) throws GenericApplicationException, IOException
pas\PatientServiceImpl.java	/patient	GET	/getEnconterBatchCount	0	public PatientEncounterDetailsDto getEnconterBatchCount(@QueryParam("date") String inDateStr) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	GET	/getHospitalEventById	0	public Events getHospitalEventById(@QueryParam("id") Long id) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	GET	/getLatestPatientVisit	0	public PatientVisitHistoryView getLatestPatientVisit(@QueryParam("patientId") Long patientId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	GET	/getMotherAndChildOnCradleBillStatus	0	public List<Long> getMotherAndChildOnCredleBillStatus(@QueryParam("admId") Long admId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	GET	/getPatientAddressDetails	0	public List<GetPatientResultDto> getPatientAddressDetails(@QueryParam("patientId") Long patientId) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	GET	/getPatientAllNotes	1	public List<PatientNote> getPatientAllNotes(@QueryParam("patientId") Long patientId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	GET	/getPatientByAddhar	0	public Long getPatientByAddhar(@QueryParam("patientId") Long patientId, @QueryParam("civilNo") String civilNo) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	GET	/getPatientByDrivelicNo	0	public Long getPatientByDrivelicNo(@QueryParam("patientId") Long patientId, @QueryParam("drivlic") String drivlic) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	GET	/getPatientCoronaList	1	public List<CoronaStats> getPatientCoronaList(@QueryParam("districtId") Long districtId, @QueryParam("blockId") Long blockId, @QueryParam("unitId") Long unitId,
pas\PatientServiceImpl.java	/patient	GET	/getPatientCrossServiceVisit	0	public Boolean getPatientCrossServiceVisit(@QueryParam("patientId") Long patientId, @QueryParam("patientUnitId") Long patientUnitId) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	GET	/getPatientDemoDetails	1	public PatientDemographicDetailsForKDL getPatientDemoDetails(@QueryParam("hid") String mrno, @QueryParam("unitId") String reqUnitId) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	GET	/getPatientDemographicDetails	0	public PatientDemographicDetailsDto getPatientDemographicDetails(@QueryParam("crNo") String mrno) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	GET	/getPatientDetailListNew	0	public List<PatientDetailsListForSearchResultDto> getPatientDetailsListNew(@QueryParam("page") Integer page, @QueryParam("count") Integer count, @QueryParam("search") String search
pas\PatientServiceImpl.java	/patient	GET	/getPatientDetailsByAdhaarId	0	public List<SearchPatientGeneralDetailsResultDto> populatePatientDetails(@QueryParam("aadharId") String adhaarId, @QueryParam("janaadharIdNo") String janaadharIdNo) throws GenericA
pas\PatientServiceImpl.java	/patient	GET	/getPatientDetailsForDeathForm	0	public Map<String, Object> getPatientDetailsForDeathForm(@QueryParam("patientId") Long patientId) throws GenericApplicationException, ParseException 
pas\PatientServiceImpl.java	/patient	GET	/getPatientEnconters	0	public PatientEncounterDto getPatientEnconters(@QueryParam("batchNo") Integer batchNo, @QueryParam("date") String inDateStr) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	GET	/getPatientEorders	1	public List<ServiceListForBillResultDto> getPatientByAddhar(@QueryParam("patientId") Long patientId, @QueryParam("unitId") Long unitId,
pas\PatientServiceImpl.java	/patient	GET	/getPatientFeedbackDetails	0	public PatientFeedbackView getPatientFeedbackDetails(@QueryParam("id") Long id, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	GET	/getPatientHistory	0	public PatientHistoryView getPatientHistory(@QueryParam("patientId") Long patientId, @QueryParam("billId") Long billId,
pas\PatientServiceImpl.java	/patient	GET	/getPatientMlcDetails	0	public Object getPatientMlcDetails(@QueryParam("patientId") Long patientId) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	GET	/getPatientUnsettledBill	0	public PatientPendingBillsView getPatientUnsettedBill(@QueryParam("patientId") @StoredProcNamedParam("p_patientid") Long patientId, @QueryParam("unitId") @StoredProcNamedParam("p_u
pas\PatientServiceImpl.java	/patient	GET	/getPatientVisitHistory	0	public PatientVisitHistoryView getPatientVisitHistory(@QueryParam("patientId") Long patientId) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	GET	/getQuestionsForFeedback	0	public List<Feedbacktemplate> getQuestionsForFeedback(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	GET	/getRghsDocumentByTransactionId	0	public Object getRghsDocumentByTransactionId(@QueryParam("transactionId") String transactionId, @QueryParam("department") String department) throws GenericApplicationException, IOE
pas\PatientServiceImpl.java	/patient	GET	/getRghsTransactionsforIHMS	0	public List<ChiranjeeviJanadharBasedDto> getRghsTransactionsforIHMS(@QueryParam("mrno") String mrno) throws GenericApplicationException, IOException
pas\PatientServiceImpl.java	/patient	GET	/getShiftDetailsByUserId	0	public List<UserShiftDetails> getShiftDetailsByUserId(@QueryParam("unitId") Integer unitId, @QueryParam("userId") Integer userId) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	GET	/getSimsRghsforIHMS	0	public List<List<RGHSSims>> getSimsRghsforIHMS(@QueryParam("mrno") String mrno) throws GenericApplicationException, IOException 
pas\PatientServiceImpl.java	/patient	GET	/getUserShiftDetailsByUserId	0	public List<UserShiftDetails> getUserShiftDetailsByUserId(@QueryParam("unitId") Integer unitId, @QueryParam("userId") Integer userId) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	GET	/getVerificationDetails	0	public MortuaryVerifications getVerificationDetails(@QueryParam("admId") Long admId) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	GET	/kioskQueue	0	public Map<String, Object> kioskQueue(@QueryParam("kioskId") Long kioskId, @QueryParam("tokenNo") String tokenNo, @QueryParam("fromDate") String fromDate) throws GenericApplication
pas\PatientServiceImpl.java	/patient	GET	/patientCoronaReport	1	public CoronaStats patientCoronaReport(@QueryParam("patientId") Long patientId) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	GET	/patientHQDetails	1	public List<PatientHqDetails> patientHQDetails(@QueryParam("patientId") Long patientId, @QueryParam("numquestions") Integer numquestions) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	GET	/searchPatientForABDM	0	public SearchPatientGeneralDetailsResultDto searchPatientForABDM(@QueryParam("patientAbdmId") Long patientAbdmId) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	GET	/searchPatientFromSolr	0	public List<SearchPatientGeneralDetailsResultDto> searchPatientFromSolr(@QueryParam("dob") String dob, @QueryParam("name") String name, @QueryParam("mobileNo") String mobileNo) thr
pas\PatientServiceImpl.java	/patient	GET	/syncEHR	0	public void syncEHR() throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	POST	/addandUpdatePatientCoronaDetails	0	public Patientcoronadetails addandUpdatePatientCoronaDetails(@BodyParam("body") Patientcoronadetails body) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	POST	/addandUpdatePatientCoronaSTATDetails	0	public CoronaStats addandUpdatePatientCoronaSTATDetails(@BodyParam("body") CoronaStats body) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	POST	/addandUpdatePatientHQDetails	0	public PatientHqDetails addandUpdatePatientHQDetails(@BodyParam("body") PatientHqDetails patD) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	POST	/addMotherChildLinking	0	public MotherChildLinkingDto addMotherChildLinking(@BodyParam("body") MotherChildLinkingDto body) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	POST	/addPatientFeedback	0	public void addPatientFeedback(@BodyParam("body") PatientFeedbackView body) 
pas\PatientServiceImpl.java	/patient	POST	/addServiceOrderDetails	0	public BillInfoDtoForKDL addServiceOrderDetails(@BodyParam("body") ServiceOrderDtoForKDL body) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	POST	/addshiftdetails	0	public UserShiftDetailsDto addShiftDetails(@BodyParam("body") UserShiftDetailsDto body) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	POST	/addshiftpatterns	1	public ShiftPatternMaster addShiftPatternsWithDetails(@BodyParam("body") ShiftPatternMaster body) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	POST	/addUpdateMemberDetails	0	public FamilyMemberDetailsDto addUpdateMemberDetails(@BodyParam("body") FamilyMemberDetailsDto body) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	POST	/createTeleconsultRoom	0	public String createTeleconsultRoom(@QueryParam("opdIpdId") Long opdIpdId) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	POST	/geofenceViolated	1	public Integer geofenceViolated(@QueryParam("patientId") Long patientId, @QueryParam("lat") java.math.BigDecimal lat,
pas\PatientServiceImpl.java	/patient	POST	/getAmbulanceCaseDetails	0	public AmbulanceCaseDetailsResultDto getAmbulanceCaseDetails(@QueryParam("claimId") String claimId) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	POST	/getDeptDoctorInformation	0	public Map<Long, Map <Long, List<DoctorDepartmentInformationDto>>> getDeptDoctorInformation(@BodyParam("body") InquiryDetailsDto body, @QueryParam("name") String searchText) throws
pas\PatientServiceImpl.java	/patient	POST	/getDoctorEnquiryDetails	0	public List<DoctorEnquiryDetailsResultDto> getDoctorEnquiryDetails(@BodyParam("body") DoctorEnquiryDetailsDto body, @QueryParam("page") Integer page,
pas\PatientServiceImpl.java	/patient	POST	/getDoctorInformation	1	public Map<String, Map <String, List<DoctorDepartmentInformationDto>>> getDoctorInformation(@BodyParam("body") InquiryDetailsDto body) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	POST	/getEMRAdmVisitByPatientId	0	public List<EMRAdmVisitListByPatientIDResultDto> getEMRAdmVisitByPatientId(@BodyParam("body") EMRAdmVisitListByPatientIDDto body, @DefaultValue("false") @QueryParam("isPharmacy") B
pas\PatientServiceImpl.java	/patient	POST	/getInquiredDetails	0	public List<InquiryDetailsResultDto> getInquiredDetails(@BodyParam("body") InquiryDetailsDto body, @QueryParam("page") Integer page,
pas\PatientServiceImpl.java	/patient	POST	/getMembersDetails	1	public List<FamilyMemberDetailsDto> getMembersDetails(@BodyParam("body") SearchMemberDetailsDto body, @QueryParam("page") Integer page,
pas\PatientServiceImpl.java	/patient	POST	/getMortuaryPatients	0	public List<Object> getMortuaryPatients(@BodyParam("body") Mortuary body,@QueryParam("page") Integer page, @QueryParam("isAccepted") Boolean isAccepted,
pas\PatientServiceImpl.java	/patient	POST	/getPatient	0	public List<GetPatientResultDto> getPatient(@BodyParam("body") GetPatientDto body) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	POST	/getPatientDetailList	0	public List<PatientDetailsListForSearchResultDto> getPatientDetailsList(@BodyParam("body") PatientDetailsListForSearchDto body) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	POST	/getPatientDetailListSearchSpecific	0	public List<PatientDetailsListForSearchResultDto> getPatientDetailListSearchSpecific(@BodyParam("body") PatientDetailsListForSearchDto body,
pas\PatientServiceImpl.java	/patient	POST	/getPatientFeedbackList	1	public List<Object[]> getPatientFeedbackList(@BodyParam("body") InquiryDetailsDto body, @QueryParam("page") Integer page,
pas\PatientServiceImpl.java	/patient	POST	/getPatientGeneralDetails	0	public List<SearchPatientGeneralDetailsResultDto> getPatientGeneralDetails(@BodyParam("body") PatientGeneralDetailsListForSearchDto body) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	POST	/getPatientGeneralDetailsListForSearch	0	public List<SearchPatientGeneralDetailsResultDto> findPatientGeneralDetailsListForSearch(@BodyParam("body") PatientGeneralDetailsListForSearchDto body,
pas\PatientServiceImpl.java	/patient	POST	/getPatientIpcrDetails	0	public Mortuary getPatientIpcrDetails(@QueryParam("admId") Long admId) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	POST	/getPatientRecord	0	public PatientRecordView getPatientRecord(@BodyParam("body") PatientSearchListForHistory body,@QueryParam("page") Integer page,
pas\PatientServiceImpl.java	/patient	POST	/getReleasedPatientDetails	0	public MortuaryPatientDetialsDto getReleasedPatientDetails(@QueryParam("admId") Long admId) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	POST	/getRequestRaisedPatients	0	public List<Mortuary> getRequestRaisedPatients(@BodyParam("body") Mortuary body,@QueryParam("page") Integer page,
pas\PatientServiceImpl.java	/patient	POST	/getResultsFromKDL	1	public LabResultResponseDto getResultsFromKDL(@BodyParam("body") ReleaseAllLabKDLDto releaseAllView) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	POST	/getServicesForInquiry	0	public List<ServiceListForBillResultDto> getServicesForInquiry(@BodyParam("body") ServiceListForBillDtoForMaster body,
pas\PatientServiceImpl.java	/patient	POST	/getUserAllShiftDetails	1	public List<UserShiftDetails> getUserAllShiftDetails(@BodyParam("body") UserShiftDetails body) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	POST	/mergeHId	0	public void mergedHIDs(@HeaderParam("token") String token, @BodyParam("body") MergeHIDDto body) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	POST	/registerPatient	0	public Map<Long,List<Object>> registerPatient(@BodyParam("body") List<PatientAppointmentandRegDto> pdto, @QueryParam("isVisit") Boolean isVisit) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	POST	/saveDemographicsDetailsOfPatientWhoseDataMismatch	0	public void saveDemographicsDetailsOfPatientWhoseDataMismatch(@BodyParam("body") PatientDemoDetailsMismatch body) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	POST	/saveKioskLabResult	1	public Map<String, Object> saveKioskLabResult(@BodyParam("body") List<OrderedServicesKioskDto> orderSet) throws GenericApplicationException, org.json.simple.parser.ParseException, 
pas\PatientServiceImpl.java	/patient	POST	/saveVitalData	1	public Map<String, Object> saveVitalData(@BodyParam("body") PatientEncounterKioskDto body) throws GenericApplicationException, org.json.simple.parser.ParseException, IOException 
pas\PatientServiceImpl.java	/patient	POST	/searchPatientForBloodBank	0	public List<SearchPatientGeneralDetailsResultDto> searchPatientForBloodBank(@BodyParam("body") PatientGeneralDetailsListForSearchDto body, @QueryParam("isOPSearch") Boolean isOPSea
pas\PatientServiceImpl.java	/patient	POST	/searchPatientForHistoryDetails	0	public List<PatientSearchListForHistory> searchPatient(@BodyParam("body") PatientSearchListForHistory body,@QueryParam("page") Integer page,
pas\PatientServiceImpl.java	/patient	POST	/updateJanaadharDetails	0	public SearchPatientGeneralDetailsResultDto getPatient(@BodyParam("body") UpdateJanaadharView body) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	POST	/updatePatientDeathDetails	0	public Boolean updatePatientDeathDetails(@QueryParam("patientId") Long patientId, @QueryParam("isDeath") boolean isDeath) throws GenericApplicationException 
pas\PatientServiceImpl.java	/patient	POST	/uploadKioskLabResult	1	public Map<String, Object> uploadKioskLabResult(@QueryParam("orderId") Long orderId, @QueryParam("testCode") String testCode, Attachment attachment) throws GenericApplicationExcept
pas\PatientServiceImpl.java	/patient	PUT	/updateBedOccupiedFlag	0	public UpdateBedOccupiedFlagDto updateBedOccupiedFlag(@BodyParam("body") UpdateBedOccupiedFlagDto body) throws GenericApplicationException 
pas\PaymentServiceImpl.java	/payment	GET	/getTermsOfPaymentListById/{id}	1	public Termsofpayment getTermsOfPaymentListById(@PathParam("id") Long id) throws GenericApplicationException 
pas\PaymentServiceImpl.java	/payment	POST	/addRefund	0	public RefundDto addRefund(@BodyParam("body") RefundDto body) throws GenericApplicationException 
pas\PaymentServiceImpl.java	/payment	POST	/addTermsOfPaymentList	0	public void addTermsOfPaymentList(@BodyParam("body") Termsofpayment body) throws GenericApplicationException 
pas\PaymentServiceImpl.java	/payment	PUT	/deleteTermsOfPaymentListById/{id}	1	public void deleteTermsOfPaymentListById(@PathParam("id") Long id) throws GenericApplicationException 
pas\PaymentServiceImpl.java	/payment	PUT	/updateTermsOfPaymentList/{id}	0	public Termsofpayment updateTermsOfPaymentList(@BodyParam("body") Termsofpayment body, @PathParam("id") Long id) throws GenericApplicationException 
pas\PctsServiceImpl.java	/pcts	GET	/getHighRiskPregnancy	1	public Boolean getHighRiskPregnancy(@QueryParam("patientId") Long patientId, @QueryParam("opdIpdId") Long opdIpdId) throws GenericApplicationException 
pas\PctsServiceImpl.java	/pcts	POST	/getEDDByLMP	1	public List<String> getEDDByLMPStatus(@BodyParam("body") AncVisitDetails body) throws GenericApplicationException 
pas\PctsServiceImpl.java	/pcts	POST	/saveOrUpdateAncDetailsForPat	1	public Boolean saveOrUpdateAncDetailsForPat(@BodyParam("body") AncVisitDetails body) throws GenericApplicationException 
pas\PctsServiceImpl.java	/pcts	POST	/updateUSGEDDByLMP	1	public Boolean updateUSGEDDByLMP(@BodyParam("body") AncVisitDetails body) throws GenericApplicationException 
pas\PctsServiceImpl.java	/pcts	PUT	/updateIsHighRiskForPatient	0	public void updateIsHighRiskForPatient(@QueryParam("isHighRisk") Boolean isHighRisk, @QueryParam("patientId") Long patientId, @QueryParam("opdIpdId") Long opdIpdId) throws GenericA
pas\PlanMasterServiceImpl.java	/planmaster	POST	/addAndUpdatePlanMasterDetails	1	public UpdatePlanMasterDto addPlanMasterDetails(@BodyParam("body") AddPlanMasterDetailsDto body) throws GenericApplicationException 
pas\PlanMasterServiceImpl.java	/planmaster	PUT	/deletePlanListById/{id}	0	public void deletePlanListById(@PathParam("id") Long id, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\PlanMasterServiceImpl.java	/planmaster	PUT	/updatePlanById/{id}	1	public void updatePlanById(@PathParam("id") Long id) throws GenericApplicationException 
pas\PmrdServiceImpl.java	/pmrd	GET	/getAllPatientEncounter	0	public JSONObject getAllPatientEncounter(@QueryParam("patientId") Long patientId) throws GenericApplicationException 
pas\PmrdServiceImpl.java	/pmrd	GET	/getJsyFormDetails	0	public JsyFormDetailsView getJsyFormDetails(@QueryParam("patientId") Long patientId, @QueryParam("formType") Long formType) throws GenericApplicationException 
pas\PmrdServiceImpl.java	/pmrd	GET	/getPatientDocumentsForPMRD	0	public JSONObject getPatientDocumentsForPMRD(@QueryParam("patientId") Long patientId) throws GenericApplicationException 
pas\PmrdServiceImpl.java	/pmrd	GET	/getPatientIenById	0	public Long getPatientIenById(@QueryParam("patientId") Long patientId) throws GenericApplicationException 
pas\PrescriptionMasterServiceImpl.java	/prescriptionmaster	GET	/downloadPrescriptionDetails	1	public List<String> downloadPrescriptionDetails(@QueryParam("prescriptionId") Long prescriptionId, @QueryParam("prescriptionUnitId") Long prescriptionUnitId, @QueryParam("mRNo") St
pas\PrescriptionMasterServiceImpl.java	/prescriptionmaster	GET	/downloadPrescriptionDetailsByVisitId	0	public List<String> downloadPrescriptionDetailsByVisitId(@QueryParam("visitId") Long visitId, @QueryParam("opd_Ipd") Long opd_Ipd, @QueryParam("mRNo") String mRNo,
pas\PrescriptionMasterServiceImpl.java	/prescriptionmaster	GET	/getPrescriptionDetails	0	public
pas\PrescriptionMasterServiceImpl.java	/prescriptionmaster	POST	/addUpdatePrescriptionMasterNew	0	public AddPrescriptionView addUpdatePrescriptionMasterNew(@BodyParam("body") AddPrescriptionView addPrescriptionView) throws GenericApplicationException 
pas\PrescriptionMasterServiceImpl.java	/prescriptionmaster	POST	/getEntirePrescriptionMasterList	0	public List<PrescriptionMasterListResultDto> getEntirePrescriptionMasterList(@BodyParam("body") PrescriptionMasterListDto body) throws GenericApplicationException 
pas\PreventiveMaintenanceScheduleServiceImpl.java	/preventivemaintenanceschedule	POST	/preventiveMaintenanceScheduleEquipment	0	public List<PreventiveMaintenanceScheduleEquipmentResultDto> preventiveMaintenanceScheduleEquipment(@BodyParam("body") PreventiveMaintenanceScheduleEquipmentDto body)
pas\PreventiveMaintenanceServiceImpl.java	/preventivemaintenance	GET	/deleteCheckListDetails	1	public void deleteCheckListDetails(@QueryParam("id") Long checkListDetailsId, @QueryParam("status") Boolean status, @QueryParam("unitId") Long unitId) throws GenericApplicationExce
pas\PreventiveMaintenanceServiceImpl.java	/preventivemaintenance	GET	/getAsssetCalibrationChecklistDetailsById	0	public List<GetAssetCalibrationChecklistDetailsById> getAsssetCalibrationChecklistDetailsById(@QueryParam("equipmentId") @StoredProcNamedParam("p_equipmentid") Long equipmentId,
pas\PreventiveMaintenanceServiceImpl.java	/preventivemaintenance	GET	/getAsssetPreventiveChecklistDetailsById	1	public AssetMaintenanceView getAsssetPreventiveChecklistDetailsById(@QueryParam("equipmentId") @StoredProcNamedParam("p_equipmentid") Long equipmentId,
pas\PreventiveMaintenanceServiceImpl.java	/preventivemaintenance	GET	/getCalibrationCheckList	0	public List<PreventiveCalibrationListResultDto> getCalibrationCheckList(@QueryParam("unitId") @StoredProcNamedParam("p_unitid") Long unitId) throws GenericApplicationException 
pas\PreventiveMaintenanceServiceImpl.java	/preventivemaintenance	GET	/getChecklistCount	1	public long getChecklistCount(@QueryParam("code") String code, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\PreventiveMaintenanceServiceImpl.java	/preventivemaintenance	GET	/getItemCheckListDetailsById	1	public AddCheckListView getItemCheckListDetailsById(@QueryParam("itemId") @StoredProcNamedParam("p_itemid") Long itemId) 
pas\PreventiveMaintenanceServiceImpl.java	/preventivemaintenance	GET	/getItemForAddMaintenance	0	public List<ItemsForAddMaintenanceResultDto> getItemForAddMaintenance() 
pas\PreventiveMaintenanceServiceImpl.java	/preventivemaintenance	GET	/getPreventiveCheckList	1	public CheckListView getPreventiveCheckList(@QueryParam("unitId") @StoredProcNamedParam("p_unitid") Long unitId, @QueryParam("itemId") @StoredProcNamedParam("p_itemid") Long itemId
pas\PreventiveMaintenanceServiceImpl.java	/preventivemaintenance	GET	/getPrevmaintenanceChkListDetail	0	public List<PreventivemaintenanceDetailResultDto> getPreventivemaintenanceDetail(@QueryParam("unitId") @StoredProcNamedParam("p_unitid") Long unitId,
pas\PreventiveMaintenanceServiceImpl.java	/preventivemaintenance	GET	/updateCheckListStatus	1	public void updateCheckListStatus(@QueryParam("id") Long checkListId, @QueryParam("status") Boolean status, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\PreventiveMaintenanceServiceImpl.java	/preventivemaintenance	POST	/addCheckList	0	public AddCheckListView addCheckList(@BodyParam("body") AddCheckListView body) throws GenericApplicationException 
pas\PreventiveMaintenanceServiceImpl.java	/preventivemaintenance	POST	/addUpdatePreventiveMaintenance	0	public UpdatePreventiveMaintenanceDto addUpdatePreventiveMaintenance(@BodyParam("body") UpdatePreventiveMaintenanceDto body) throws GenericApplicationException 
pas\PreventiveMaintenanceServiceImpl.java	/preventivemaintenance	POST	/getAssetCalibrationMaintenanceList	1	public AssetMaintenanceView getAssetCalibrationMaintenanceList(@BodyParam("body") GetCalibrationMaintenanceListDetailsDto body,
pas\PreventiveMaintenanceServiceImpl.java	/preventivemaintenance	POST	/getAssetPreventiveMaintenanceList	1	public AssetMaintenanceView getAssetPreventiveMaintenanceList(@BodyParam("body") GetCalibrationMaintenanceListDetailsDto body,
pas\PreventiveMaintenanceServiceImpl.java	/preventivemaintenance	POST	/getCheckListDetailsById	1	public List<GetEquipmentChecklistDetailsById> getCheckListDetailsById(@BodyParam("body") GetEquipmentChecklistDetailsById body) throws GenericApplicationException 
pas\PreventiveMaintenanceServiceImpl.java	/preventivemaintenance	POST	/getPreventiveCheckListDetails	0	public List<CheckListResultDto> getPreventiveCheckListDetails(@BodyParam("body") CheckListDto body) throws GenericApplicationException 
pas\PreventiveMaintenanceServiceImpl.java	/preventivemaintenance	POST	/getPreventivemaintenanceDetail	0	public List<PreventivemaintenanceDetailResultDto> getPreventivemaintenanceDetail(@BodyParam("body") GetPrevMaintenanceDetailsDto body) throws GenericApplicationException 
pas\PreventiveMaintenanceServiceImpl.java	/preventivemaintenance	POST	/mapCheckListWithAsset	1	public List<PreventiveCalibrationCheckListMappingDto> mapChecklListWithAsset(@BodyParam("body") List<PreventiveCalibrationCheckListMappingDto> body) throws GenericApplicationExcept
pas\PreventiveMaintenanceServiceImpl.java	/preventivemaintenance	POST	/saveEquipmentMaintenance	1	public AddEquipmentMaintenanceView addEquipmentMaintenance(@BodyParam("body") AddEquipmentMaintenanceView body) throws GenericApplicationException 
pas\PreventiveMaintenanceServiceImpl.java	/preventivemaintenance	POST	/updateCheckListDetails	1	public AddCheckListView updateCheckListDetails(@BodyParam("body") AddCheckListView body) throws GenericApplicationException 
pas\PurchaseOrderServiceImpl.java	/purchaseorder	GET	/getLastPurchaseOrderList	0	public
pas\QuotationServiceImpl.java	/quotation	GET	/getQuotationsForCompare	0	public AddQuotationView getQuotationsForCompare (@QueryParam("quotationIds") String quotationIds, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\QuotationServiceImpl.java	/quotation	POST	/addNewQuotation	1	public AddQuotationView addNewQuotation(@BodyParam("body") AddQuotationView body) throws GenericApplicationException 
pas\QuotationServiceImpl.java	/quotation	POST	/getQuotationDetails	0	public
pas\QuotationServiceImpl.java	/quotation	POST	/getQuotations	0	public
pas\QuotationServiceImpl.java	/quotation	POST	/getQuotationTermsAndConditions	0	public
pas\QuotationServiceImpl.java	/quotation	POST	/printQuotationDetails	0	public
pas\QuotationServiceImpl.java	/quotation	POST	/updateQuotationItemDetails	1	public AddQuotationView updateQuotationItemDetails(@BodyParam("body") AddQuotationView body) throws GenericApplicationException 
pas\RadiologyServiceImpl.java	/radiology	GET	/getAllRadServiceUnmapped	1	public List<Servicemaster> getAllRadServiceUnmapped() throws GenericApplicationException 
pas\RadiologyServiceImpl.java	/radiology	GET	/getPatientRadReport	0	public GetPathoResultApi getPatientRadReport(@QueryParam("unitId") Long unitId,@QueryParam("mrnNo") String mrnNo,@QueryParam("billNo") String billNo,@QueryParam("billId") @DefaultV
pas\RadiologyServiceImpl.java	/radiology	GET	/getRadiologyQueue	0	public List<Cabinmaster> getRadiologyCabinQueue(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\RadiologyServiceImpl.java	/radiology	GET	/getRadiologyQueueNew	0	public List<PatientRadiologyEncounterDto> getRadiologyCabinQueueNew(@QueryParam("unitId") Long unitId, @QueryParam("cabinId") Long cabinId) throws GenericApplicationException 
pas\RadiologyServiceImpl.java	/radiology	GET	/getRadTestDetailsById/{id}	1	public Radtestmaster getRadTestDetailsById(@PathParam("Id") Long Id) throws GenericApplicationException 
pas\RadiologyServiceImpl.java	/radiology	POST	/addRadioTestMaster	0	public void addRadTestMaster(@BodyParam("body") Radtestmaster body) throws GenericApplicationException 
pas\RadiologyServiceImpl.java	/radiology	POST	/addRadTestMaster	1	public AddRadTestMasterDto addRadTestMaster(@BodyParam("body") AddRadTestMasterDto body) throws GenericApplicationException 
pas\RadiologyServiceImpl.java	/radiology	POST	/updateRadioTestMaster	0	public Radtestmaster updateRadTestMaster(@BodyParam("body") Radtestmaster body) throws GenericApplicationException 
pas\RadiologyServiceImpl.java	/radiology	PUT	/deleteTestMaster/{id}	1	public void deleteTestMasters(@PathParam("id") Long id) throws GenericApplicationException 
pas\RadiologyServiceImpl.java	/radiology	PUT	/updateRadTestMaster	0	public GenericResultDto updateRadTestMaster(@BodyParam("body") RadTestMasterDto body) throws GenericApplicationException 
pas\RefEntityServiceImpl.java	/refentity	GET	/getRefEntityByID	0	public List<RefEntityByIDResultDto> getRefEntityByID(@QueryParam("id") @StoredProcNamedParam("p_id") Integer id) throws GenericApplicationException 
pas\RefEntityServiceImpl.java	/refentity	GET	/getRefEntityMasterByTypeId	1	public List<GetRefEntityMasterByTypeId> getRefEntityMasterByTypeId(@QueryParam("refEntityTypeId") @StoredProcNamedParam("p_refentitytypeid") Long refEntityTypeId,
pas\RefEntityServiceImpl.java	/refentity	POST	/addUpdateRefEntityMaster	0	public UpdateRefEntityMasterDto addUpdateRefEntityMaster(@BodyParam("body") UpdateRefEntityMasterDto body) throws GenericApplicationException 
pas\RefundServiceImpl.java	/refund	DELETE	/deleteRefund	1	public DeleteRefundDto deleteRefund(@BodyParam("body") DeleteRefundDto body) throws GenericApplicationException 
pas\RefundServiceImpl.java	/refund	POST	/addPaymentAdvanceDetails	0	public GenericResultDto addPaymentAdvanceDetails(@BodyParam("body") AddPaymentAdvanceDetailsDto body) throws GenericApplicationException 
pas\RefundServiceImpl.java	/refund	POST	/addRefund	1	public AddRefundDto addRefund(@BodyParam("body") AddRefundDto body) throws GenericApplicationException 
pas\RefundServiceImpl.java	/refund	POST	/addRefundDetails	0	public AddRefundDetailsDto addRefundDetails(@BodyParam("body") AddRefundDetailsDto body) throws GenericApplicationException 
pas\RefundServiceImpl.java	/refund	POST	/details	1	public RefundDetails_detailsDto details(@BodyParam("body") RefundDetails_detailsDto body) throws GenericApplicationException 
pas\RefundServiceImpl.java	/refund	POST	/getRefund	0	public List<GetRefundResultDto> getRefund(@BodyParam("body") GetRefundDto body) throws GenericApplicationException 
pas\RefundServiceImpl.java	/refund	PUT	/updatePatientReqCrossMatchbyChargeID	0	public GenericResultDto updatePatientReqCrossMatchbyChargeID(@BodyParam("body") PatientReqCrossMatchbyChargeIDDto body) throws GenericApplicationException 
pas\RMRSServiceImpl.java	/RMRS	GET	/approveOrRelease	0	public String approveOrRelease(@QueryParam("collectionId") Long collectionId,
pas\RMRSServiceImpl.java	/RMRS	GET	/getAccountByColAndVal	0	public List<Accountheadmaster> getCustomizedAccount(@QueryParam("unitId") Long unitId,
pas\RMRSServiceImpl.java	/RMRS	GET	/getAccountById	0	public Accountheadmaster getAccountById(@QueryParam("unitId") Long unitId,
pas\RMRSServiceImpl.java	/RMRS	GET	/getAccountBySummaryId	0	public List<Accountheadmaster> getAccountBySummaryId(@QueryParam("summaryAccountId") Long summaryAccountId,
pas\RMRSServiceImpl.java	/RMRS	GET	/getAccountReport	0	public ArrayList<JournalCollectionView> getAccountReport(@QueryParam("non_summary_ahId") Long fromAcc,
pas\RMRSServiceImpl.java	/RMRS	GET	/getAllAccountTypes	0	public Map<Integer, String> getAllAccountTypes(@QueryParam("allowedTypes") String allowedTypes)
pas\RMRSServiceImpl.java	/RMRS	GET	/getAllTransactionStatusList	1	public List<TransactionStatus> getAllTransactionStatus()
pas\RMRSServiceImpl.java	/RMRS	GET	/getAthorities	1	public List<AuthorityMaster> getAuthority(@QueryParam("unitId") Long unitId) 
pas\RMRSServiceImpl.java	/RMRS	GET	/getAthoritiesByCount	0	public List<AuthorityMaster> getAthoritiesByCount(@QueryParam("unitId") Long unitId,
pas\RMRSServiceImpl.java	/RMRS	GET	/getAthoritiesPendingPaymentDetails	0	public List<JournalCollectionView> getAthoritiesPaymentDetails(@QueryParam("unitId") Long unitId,
pas\RMRSServiceImpl.java	/RMRS	GET	/getAuthorityEmiDetailsByAuthortiyId	0	public List<RMRSDashboardTransaction> getAuthorityEmiDetailsByAuthortiyId( @QueryParam("authorityId")Long authorityId,
pas\RMRSServiceImpl.java	/RMRS	GET	/getAuthorityPaymentDetails	1	public List<RMRSCollectionDto> RMRSAuthorityPaymentDetails(@QueryParam("authorityId") Long authorityId
pas\RMRSServiceImpl.java	/RMRS	GET	/getCustomizedAccount	0	public List<Accountheadmaster> getCustomizedAccount(@QueryParam("unitId") Long unitId, @QueryParam("allowedAccountType") String allowedAccount) throws GenericApplicationException
pas\RMRSServiceImpl.java	/RMRS	GET	/getHeads	0	public List<Accountheadmaster> getHeads(@QueryParam("unitId") Long unitId) 
pas\RMRSServiceImpl.java	/RMRS	GET	/getHeadsByCountAndSearch	0	public List<HeadsMaster> getHeadsByCountAndSearch(@QueryParam("unitId") Long unitId,
pas\RMRSServiceImpl.java	/RMRS	GET	/getIssuedPayment	0	public ArrayList<RMRSCollectionDto> getIssuedPayment(@QueryParam("collectionFormType") String collectionFormType, @QueryParam("unitId") Long unitId,
pas\RMRSServiceImpl.java	/RMRS	GET	/getJournalEntriesList	0	public List<JournalCollectionView> getJournalList(@QueryParam("type") String type, @QueryParam("unitId") Long unitId,
pas\RMRSServiceImpl.java	/RMRS	GET	/getJournalListDetail	0	public JournalCollectionView getJournalListDetail(@QueryParam("recievedAgainstId") Long recievedAgainstId,
pas\RMRSServiceImpl.java	/RMRS	GET	/getPaymentCollectionByAuthority	1	public JournalCollectionView getAuthorityPendingAmt(AuthorityMaster am, Timestamp lastPayDate, CollectionDetail details , Boolean applyAffectiveDate) throws GenericApplicationExcep
pas\RMRSServiceImpl.java	/RMRS	GET	/getPlanAndAccountWiseReport	0	public JournalCollectionView getPlanAndAccountWiseReport(@QueryParam("planId") Long planId,
pas\RMRSServiceImpl.java	/RMRS	GET	/getPlanMasters	0	public List<Planmaster> getPlanMaster(@QueryParam("unitId") Long unitId)
pas\RMRSServiceImpl.java	/RMRS	GET	/getReportByDocumentNumber	0	public List<JournalCollectionView> getReportByDocumentNumber(@QueryParam("documentNumber") String documentNumber) throws GenericApplicationException
pas\RMRSServiceImpl.java	/RMRS	GET	/getRMRSDashboardByDate	1	public List<RMRSDashboardTransactionView> getRMRSDashboard(@QueryParam("searchFld") String searchFld ,
pas\RMRSServiceImpl.java	/RMRS	GET	/getSearchDataByAccountHead	1	public List<JournalCollectionView> getCollectionDetailList(@QueryParam("unitId") Long unitId,
pas\RMRSServiceImpl.java	/RMRS	GET	/getSummaryAccountReport	1	public List<JournalCollectionView> getSummaryAccountReport(@QueryParam("summaryAccountHeadId") Long summaryAccountId,
pas\RMRSServiceImpl.java	/RMRS	GET	/getSuspenseAccountDetail	0	public List<JournalCollectionView> getSuspenseAccountDetail(
pas\RMRSServiceImpl.java	/RMRS	GET	/printPlanAndAccountWiseReport	0	public String printPlanAndAccountWiseReport(@QueryParam("planId") Long planId,
pas\RMRSServiceImpl.java	/RMRS	GET	/printReportByDocumentNumber	0	public String printReportByDocumentNumber(@QueryParam("documentNumber") String documentNumber) throws GenericApplicationException
pas\RMRSServiceImpl.java	/RMRS	GET	/printSummaryAccountReport	0	public String printSummaryAccountReport(@QueryParam("summaryAccountHeadId") Long summaryAccountId,
pas\RMRSServiceImpl.java	/RMRS	GET	/report_printBankStatement	0	public String printBankStatement(@QueryParam("non_summary_ahId") Long fromAcc,
pas\RMRSServiceImpl.java	/RMRS	GET	/validateDuplicateAccount	0	public Boolean validateDuplicateAccount(@QueryParam("accountType") Long accountType) throws GenericApplicationException
pas\RMRSServiceImpl.java	/RMRS	GET	/validatePlanMappingWithAccount	0	public Boolean validatePlanMappingWithAccount(@QueryParam("accountHeadId") Long accountHeadId,
pas\RMRSServiceImpl.java	/RMRS	POST	/addAthorities	0	public void saveAuthority(@BodyParam("body")AuthorityMaster body) throws ParseException
pas\RMRSServiceImpl.java	/RMRS	POST	/addAuthorityPaymentNew	1	public String addPaymentNew(@BodyParam("body") AddPaymentDto body) throws Exception 
pas\RMRSServiceImpl.java	/RMRS	POST	/addCollection	0	public String addCollection(@BodyParam("body")JournalCollectionView body) throws GenericApplicationException 
pas\RMRSServiceImpl.java	/RMRS	POST	/addDiscount	0	public RMRSDashboardTransaction addDiscount(@QueryParam("dashboardId")Long dashboardId,
pas\RMRSServiceImpl.java	/RMRS	POST	/collectionUserSearch	0	public List<RmrsCollection> collectionUserSearch(@BodyParam("body")RmrsCollection body)
pas\RMRSServiceImpl.java	/RMRS	POST	/getStaffPendingAmt	0	public JournalCollectionView getStaffPendingAmt(@BodyParam("body")CollectionDetail body) throws GenericApplicationException, ParseException 
pas\RMRSServiceImpl.java	/RMRS	PUT	/updateAuthorityMaster	1	public void updateAuthorityMaster(@BodyParam("body")AuthorityMaster body)
pas\RMRSServiceImpl.java	/RMRS	PUT	/updateHeadsMaster	1	public void updateHeadsMaster(@BodyParam("body")HeadsMaster body) 
pas\ServiceMasterServiceImpl.java	/servicemaster	GET	/checkExistingService	1	public List<Long> checkExistingService(@QueryParam("serviceName") String serviceName, @QueryParam("printName") String printName, @QueryParam("serviceCode") String serviceCode,
pas\ServiceMasterServiceImpl.java	/servicemaster	GET	/getAllServiceMasterDetails	0	public // @Transactional(rollbackOn = Exception.class)
pas\ServiceMasterServiceImpl.java	/servicemaster	GET	/getClassDetailsByLatestAdmission	0	public
pas\ServiceMasterServiceImpl.java	/servicemaster	GET	/getClassDetailsByLatestAdmissionId	0	public List<ClassDetailsByLatestAdmissionResultDto> getClassDetailsByLatestAdmissionId(@QueryParam("opd_ipd_id") @StoredProcNamedParam("p_opd_ipd_id") Long opd_ipd_id,
pas\ServiceMasterServiceImpl.java	/servicemaster	GET	/getSpecialization	0	public List<SpecializationResultDto> getSpecialization() throws GenericApplicationException 
pas\ServiceMasterServiceImpl.java	/servicemaster	GET	/getUnitWiseCabinMaster	0	public List<UnitWiseCabinMasterResultDto> getUnitWiseCabinMaster(@QueryParam("unitId") @StoredProcNamedParam("p_unitid") Long unitId, @QueryParam("cabinId") Long cabinId)
pas\ServiceMasterServiceImpl.java	/servicemaster	POST	/addCompanyDetails	0	public void addCompanyDetails(@BodyParam("body") AddCompanyMasterDetails body) throws GenericApplicationException 
pas\ServiceMasterServiceImpl.java	/servicemaster	POST	/getPackageListForBill	0	public List<PackageListForBillResultDto> getPackageListForBill(@BodyParam("body") PackageListForBillDto body) throws GenericApplicationException 
pas\ServiceMasterServiceImpl.java	/servicemaster	POST	/getPackageServiceFromPackageID	0	public List<GetPackageServiceFromPackageIDResultDto> getPackageServiceFromPackageID(@BodyParam("body") GetPackageServiceFromPackageIDDto body) throws GenericApplicationException 
pas\ServiceMasterServiceImpl.java	/servicemaster	POST	/getRghsPackageList	0	public List<PackageListForBillResultDto> getRghsPackageList(@BodyParam("body") PackageListForBillDto body) throws GenericApplicationException 
pas\ServiceMasterServiceImpl.java	/servicemaster	POST	/getServiceListForBillEstimation	0	public List<ServiceListForBillResultDto> getServiceListForBillEstimation(@BodyParam("body") ServiceListForBillDto body) throws GenericApplicationException 
pas\ServiceMasterServiceImpl.java	/servicemaster	POST	/getServiceListForBillWithoutCopayAndConcession	0	public List<ServiceListForBillResultDto> getServiceListForBillWithoutCopayAndConcession(@BodyParam("body") ServiceListForBillDto body) throws GenericApplicationException 
pas\SurgeryDetailsServiceImpl.java	/surgerydetails	GET	/getSurgeryheadmaster	1	public List<Surgeryheadmaster> getSurgeryheadmaster() throws GenericApplicationException 
pas\SurgeryDetailsServiceImpl.java	/surgerydetails	GET	/getSurgeryheadmasterbyId	0	public List<Surgeryheadmaster> getSurgeryheadmasterbyId(@QueryParam("id") Long id) throws GenericApplicationException 
pas\SurgeryDetailsServiceImpl.java	/surgerydetails	GET	/getSurgeryheadsListById/{id}	1	public Surgeryheadmaster getSurgeryheadsListById(@PathParam("id") Long id) throws GenericApplicationException 
pas\SurgeryDetailsServiceImpl.java	/surgerydetails	POST	/addSurgeryheadsList	0	public void addSurgeryheadsList(@BodyParam("body") Surgeryheadmaster body) throws GenericApplicationException 
pas\SurgeryDetailsServiceImpl.java	/surgerydetails	POST	/updateSurgeryheadsList	1	public Surgeryheadmaster updateSurgeryheadsList(@BodyParam("body") Surgeryheadmaster body) throws GenericApplicationException 
pas\SurgeryDetailsServiceImpl.java	/surgerydetails	PUT	/deleteSurgeryheadsById/{id}	0	public void deleteSurgeryheadsById(@PathParam("id") Long id) throws GenericApplicationException 
pas\TariffServiceImpl.java	/tariff	GET	/getAllTariff	1	public List<Tariffmaster> getAllTariff() throws GenericApplicationException 
pas\TariffServiceImpl.java	/tariff	GET	/getServiceMasterListById/{id}	1	public Servicemaster getServiceMasterListById(@PathParam("id") Long id) throws GenericApplicationException 
pas\TariffServiceImpl.java	/tariff	POST	/addservicemasterDetails	0	public List<String> addservicemasterDetails(@BodyParam("body") Servicemaster body) throws GenericApplicationException 
pas\TariffServiceImpl.java	/tariff	POST	/updateServicemasterList	1	public void updateServicemasterList(@BodyParam("body") Servicemaster body) throws GenericApplicationException 
pas\TariffServiceImpl.java	/tariff	PUT	/deleteServiceListById/{id}	0	public void deleteServiceListById(@PathParam("id") Long id) throws GenericApplicationException 
pas\TransportServiceImpl.java	/transport	GET	/approveTransportRequest	0	public void approveTransportRequest(@QueryParam("id") Long reqId, @QueryParam("unitId") Long unitId,@QueryParam("approvedBy") String approvedBy) throws GenericApplicationException 
pas\TransportServiceImpl.java	/transport	GET	/approveVehicleMaintenanceRequest	1	public void approveVehicleMaintenanceRequest(@QueryParam("id") Long id, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\TransportServiceImpl.java	/transport	GET	/cancelTransportRequest	0	public void cancelTransportRequest(@QueryParam("id") Long reqId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\TransportServiceImpl.java	/transport	GET	/checkTransitTransportReuest	1	public boolean checkTransitTransportReuest(@QueryParam("patientId") Long patientId, @QueryParam("opdIpdId") Long opdIpdId, @QueryParam("opdIpd") Long opdIpd) throws GenericApplicat
pas\TransportServiceImpl.java	/transport	GET	/getAllDriverDetails	1	public List<DriverMaster> getAllDriverDetails(@QueryParam("unitId") String unitId) throws GenericApplicationException 
pas\TransportServiceImpl.java	/transport	GET	/getAllocatedVehicleStatus	0	public List<VehicleStatusDto> getAllocatedVehicleStatus(@QueryParam("page") Integer page,
pas\TransportServiceImpl.java	/transport	GET	/getAllVehicleDetails	0	public List<Ambulancemaster> getAllVehicleDetails(@QueryParam("unitId") Long unitId, @QueryParam("vehicleTypeId") Long vehicleTypeId) throws GenericApplicationException 
pas\TransportServiceImpl.java	/transport	GET	/getAllVehicleDetailsForReport	0	public List<Ambulancemaster> getAllVehicleDetailsForReport(@QueryParam("unitId") String unitId) throws GenericApplicationException 
pas\TransportServiceImpl.java	/transport	GET	/getAvailableVehicleDriverList	0	public JSONArray getAvailableVehicleDriverList(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\TransportServiceImpl.java	/transport	GET	/getDetailsForVehicleFare	1	public TransportRequestDetails getDetailsForVehicleFare(@QueryParam("unitId") Long unitId, @QueryParam("id") Long id, @QueryParam("vehicleId") Long vehicleId) throws GenericApplica
pas\TransportServiceImpl.java	/transport	GET	/getDriverDetails	0	public List<DriverMaster> getDriverDetails(@QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\TransportServiceImpl.java	/transport	GET	/getVehicleMaintenanceRequest	1	public VehicleMaintenance getVehicleMaintenanceRequest(@QueryParam("id") Long id, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\TransportServiceImpl.java	/transport	POST	/addFuelStationDetails	0	public void addFuelStationDetails(@BodyParam("body") FuelStationDetails body) throws GenericApplicationException 
pas\TransportServiceImpl.java	/transport	POST	/addPatientConsultOrder	0	public Boolean addPatientConsultOrder(@BodyParam("body") ConsultOrder body) throws GenericApplicationException, IOException 
pas\TransportServiceImpl.java	/transport	POST	/addPatientTansportRequest	0	public void addPatientTansportRequest(@BodyParam("body") TransportRequest body) throws GenericApplicationException 
pas\TransportServiceImpl.java	/transport	POST	/addTransportCharge	0	public TransportChargeDto addTransportCharge(@BodyParam("body") TransportChargeDto body) throws GenericApplicationException 
pas\TransportServiceImpl.java	/transport	POST	/addVehicleMaintenanceRequest	0	public void addVehicleMaintenanceRequest(@BodyParam("body") VehicleMaintenance body) throws GenericApplicationException 
pas\TransportServiceImpl.java	/transport	POST	/allocateVehicle	0	public void allocateVehicle(@BodyParam("body") TransportRequest body) throws GenericApplicationException 
pas\TransportServiceImpl.java	/transport	POST	/completeVehicleMaintenanceRequest	0	public void completeVehicleMaintenanceRequest(@BodyParam("body") VehicleMaintenance body) throws GenericApplicationException 
pas\TransportServiceImpl.java	/transport	POST	/getFuelStationDetails	1	public List<FuelStationDetails> getFuelStationDetails(@BodyParam("body") TripRequestListSummary body, @QueryParam("page") Integer page,
pas\TransportServiceImpl.java	/transport	POST	/getIPDPatientDetailsForTransport	1	public List<SearchPatientAndVisitByNameMobNoResultDto> getIPDPatientDetailsForTransport(@BodyParam("body") SearchPatientAndAdmissionByNameMobNoDto body) throws GenericApplicationEx
pas\TransportServiceImpl.java	/transport	POST	/getPatientDetailsListForTransport	0	public List<SearchPatientGeneralDetailsResultDto> getPatientDetailsListForTransport(@BodyParam("body") PatientGeneralDetailsListForSearchDto body) throws GenericApplicationExceptio
pas\TransportServiceImpl.java	/transport	POST	/getTripRequestCount	1	public TripRequestListSummary getTripRequestCount(@BodyParam("body") TripRequestListSummary body) throws GenericApplicationException 
pas\TransportServiceImpl.java	/transport	POST	/getTripRequestDetails	0	public List<TransportRequest> getTripRequestDetails(@BodyParam("body") TripRequestListSummary body, @QueryParam("page") Integer page,
pas\TransportServiceImpl.java	/transport	POST	/getUnitWisePatientDetailsList	0	public List<SearchPatientGeneralDetailsResultDto> getUnitWisePatientDetailsList(@BodyParam("body") PatientGeneralDetailsListForSearchDto body) throws GenericApplicationException 
pas\TransportServiceImpl.java	/transport	POST	/getVehicleMaintenanceDetails	1	public List<VehicleMaintenance> getVehicleMaintenanceDetails(@BodyParam("body") TripRequestListSummary body, @QueryParam("page") Integer page,
pas\TransportServiceImpl.java	/transport	POST	/getVehicleOutDetails	1	public List<TransportRequest> getVehicleOutDetails(@BodyParam("body") TransportRequest body, @QueryParam("page") Integer page,
pas\TransportServiceImpl.java	/transport	POST	/getVisitAdmissionDetailsForTransport	0	public List<EMRAdmVisitListByPatientIDResultDto> getVisitAdmissionDetailsForTransport(@BodyParam("body") EMRAdmVisitListByPatientIDDto body) throws GenericApplicationException 
pas\TransportServiceImpl.java	/transport	POST	/printTransportReqReceipt	1	public String[] printTransportReqReceipt(@BodyParam("body") TransportChargeDto body) throws GenericApplicationException 
pas\TransportServiceImpl.java	/transport	POST	/saveVehicleInDetails	1	public void saveVehicleInDetails(@BodyParam("body") TransportRequestDetails body) throws GenericApplicationException 
pas\TransportServiceImpl.java	/transport	POST	/saveVehicleOutDetails	0	public void saveVehicleOutDetails(@BodyParam("body") TransportRequestDetails body) throws GenericApplicationException 
pas\UserManagementServiceImpl.java	/usermanagement	GET	/flushCache	0	public void flush(@HeaderParam("token") String token) throws GenericApplicationException 
pas\UserManagementServiceImpl.java	/usermanagement	GET	/flushVCRates	0	public void flushVCRates(@QueryParam("planId") Long planId, @QueryParam("visitTypeId") Long visitTypeId,
pas\UserManagementServiceImpl.java	/usermanagement	GET	/getMDMTokenWithSSOToken	1	public Map<String, String> getMDMTokenWithSSOToken(@QueryParam("userdetails") String userdetails) throws GenericApplicationException 
pas\UserManagementServiceImpl.java	/usermanagement	GET	/getPermissions/{roleId}	0	public List<RoleBasedPermissionDTO> getUserByUserId(@PathParam("roleId") Long roleId) throws GenericApplicationException 
pas\UserManagementServiceImpl.java	/usermanagement	GET	/getSessionDetails	0	public UserView getSessionDetails(@HeaderParam("token") String token, @QueryParam("unitId") Long unitId,
pas\UserManagementServiceImpl.java	/usermanagement	GET	/getUnitRoleDetails	1	public UserView getUnitRoleDetails(@HeaderParam("token") String token) throws GenericApplicationException 
pas\UserManagementServiceImpl.java	/usermanagement	GET	/getUnitRoleDetailsForChange	0	public UserView getUnitRoleDetails() throws GenericApplicationException 
pas\UserManagementServiceImpl.java	/usermanagement	GET	/getUnitStoreStatusList	1	public List<UnitStoreStatusListResultDto> getUnitStoreStatusList(@QueryParam("userId") @StoredProcNamedParam("p_userid") Long userId,
pas\UserManagementServiceImpl.java	/usermanagement	GET	/getUserBuildingDetails	0	public List<Wingmaster> getUserBuildingDetails(@QueryParam("unitId") String unitIdStr) throws GenericApplicationException 
pas\UserManagementServiceImpl.java	/usermanagement	GET	/getUserByUserId	0	public String getUserByUserId(@QueryParam("userId") Long userId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
pas\UserManagementServiceImpl.java	/usermanagement	GET	/getUserMappedWards	1	public List<Wardmaster> getUserMappedWards(@QueryParam("userId") Long userId, @QueryParam("departmentId") Long departmentId, @QueryParam("buildingId") Long buildingId) throws Gener
pas\UserManagementServiceImpl.java	/usermanagement	GET	/getUserMappedWardsForMlc	0	public List<Wardmaster> getUserMappedWardsForMLc(@QueryParam("userId") Long userId, @QueryParam("departmentId") Long departmentId, @QueryParam("buildingId") Long buildingId) throws
pas\UserManagementServiceImpl.java	/usermanagement	GET	/getUserName	0	public UserNameDto getUserName(@QueryParam("loginName") @StoredProcNamedParam("p_loginname") String loginName,
pas\UserManagementServiceImpl.java	/usermanagement	POST	/authenticateUser	0	public List<EnticateUserResultDto_2> authenticateUser(@BodyParam("body") EnticateUserDto body, @QueryParam("sso") @DefaultValue("false") Boolean isSSO) throws GenericApplicationExc
pas\UserManagementServiceImpl.java	/usermanagement	POST	/getUser	0	public List<GetUserResultDto> getUser(@BodyParam("body") GetUserDto body) throws GenericApplicationException 
pas\UserManagementServiceImpl.java	/usermanagement	POST	/loginsso	1	public UserView loginsso(@FormParam("param1") String ssousername, @FormParam("param2") String ssopassword) throws GenericApplicationException 
pas\UserManagementServiceImpl.java	/usermanagement	POST	/patientLoginNameExists	1	public List<PatientExistsResultDto> patientLoginNameExists(@BodyParam("body") PatientExistsDto body) throws GenericApplicationException 
pas\UserManagementServiceImpl.java	/usermanagement	POST	/setDefaultRoute	0	public void setDefaultRoute(@BodyParam("body") DefaultRouteDto body) throws GenericApplicationException 
pas\VisitServiceImpl.java	/visit	GET	/getActiveConsultOrderDepartmentWardWise	0	public List<ConsultOrder> getActiveConsultOrderDepartmentWardWise(@QueryParam("fromDepartmentId") Long fromDepartmentId, @QueryParam("fromWardId") Long fromWardId) throws GenericAp
pas\VisitServiceImpl.java	/visit	GET	/getAllFollowUpVisitReq	0	public List<GetFollowUpVisitPatientDto> getAllFollowUpVisitReq() throws GenericApplicationException, ParseException 
pas\VisitServiceImpl.java	/visit	GET	/getAllPendingConsultVisit	0	public List<AllPendingConsultVisitResultDto> getAllPendingConsultVisit(@QueryParam("unitId") @StoredProcNamedParam("p_unitid") Long unitId) throws GenericApplicationException 
pas\VisitServiceImpl.java	/visit	GET	/getPatientActiveConsultOrder	0	public List<ConsultOrder> getPatientActiveConsultOrder(@QueryParam("patientId") Long patientId) throws GenericApplicationException 
pas\VisitServiceImpl.java	/visit	GET	/getPatientActiveConsultOrderByDoctor	0	public Long getPatientActiveConsultOrderByDoctor(@QueryParam("doctorId") Long doctorId, @QueryParam("isNew") Boolean isNew) throws GenericApplicationException 
pas\VisitServiceImpl.java	/visit	GET	/notifyConsultOrder	0	public ConsultOrder notifyConsultOrder(@QueryParam("coId") Long coId) throws GenericApplicationException 
pas\VisitServiceImpl.java	/visit	POST	/getActiveDoctorForConsult	0	public List<DoctorCheckinIpMaster> getActiveDoctorForConsult(@BodyParam("body") DoctorCheckinIpMaster body) throws GenericApplicationException 
pas\VisitServiceImpl.java	/visit	POST	/getAllConsultOrders	0	public List<ConsultOrderResultDto> getAllConsultOrders(@BodyParam("body") ConsultOrderResultDto body, @QueryParam("unitId") Long unitId, @QueryParam("page") Integer page,
pas\VisitServiceImpl.java	/visit	POST	/getConsultantForConsultOrder	0	public List<Doctormaster> getConsultantForConsultOrder(@BodyParam("body") DoctorCheckinIpMaster body) throws GenericApplicationException 
pas\VisitServiceImpl.java	/visit	POST	/getDoctorForConsultOrder	1	public List<Doctormaster> getDoctorForConsultOrder(@BodyParam("body") DoctorCheckinIpMaster body) throws GenericApplicationException 
pas\VisitServiceImpl.java	/visit	POST	/updateConsultOrder	0	public void updateConsultOrder(@BodyParam("body") ConsultOrder body) throws GenericApplicationException 
pas\VisitServiceImpl.java	/visit	POST	/updateDoctorCheckInIp	1	public DoctorCheckinIpMaster updateDoctorCheckInIp(@BodyParam("body") DoctorCheckinIpMaster body) throws GenericApplicationException 
patp\PatientHistoryServiceImpl.java	/patientHistory	GET	/getPatientHistory	0	public List<HealthFactorDto> getPatientApptandVisit(@QueryParam("ewdPatientId") Long ewdPatientId, @QueryParam("reqId") String reqId, @QueryParam("unitId") Long unitId,
patp\PatientPortalServiceImpl.java	/patientPortalService	DELETE	/removePatientPortalDocument	1	public void removePatientPortalDocument(@QueryParam("mRNO") String mRNO, @QueryParam("documentName") String labimageval, @QueryParam("documentPath") String labimagepath,
patp\PatientPortalServiceImpl.java	/patientPortalService	GET	/document/{id}	0	public void getImageById(@PathParam("id") String id, @Context HttpServletResponse response) throws GenericApplicationException 
patp\PatientPortalServiceImpl.java	/patientPortalService	GET	/getAllergiesGenericListForSearch	0	public List<AllergyDto> getAllergiesGenericListForSearch(@QueryParam("allergyText") String allergyText, @QueryParam("reqId") String reqId) throws GenericApplicationException 
patp\PatientPortalServiceImpl.java	/patientPortalService	GET	/getAllPatientDocumentsList	0	public List<PatientDocuments> getAllPatientDocumentsList(@QueryParam("mRNo") String mRNo, @QueryParam("unitId") Long unitId)
patp\PatientPortalServiceImpl.java	/patientPortalService	GET	/getMedicationGenericListForSearch	1	public List<SearchDto> getAllMedicationGenericListForSearch(@QueryParam("medicinText") String allergyText, @QueryParam("reqId") String reqId) throws GenericApplicationException 
patp\PatientPortalServiceImpl.java	/patientPortalService	GET	/getNormalResultPP	0	public List<List<PathNormalTestResultDto>> getNormalResultPP(@QueryParam("orderId") @StoredProcNamedParam("p_orderid") String orderId,
patp\PatientPortalServiceImpl.java	/patientPortalService	GET	/getpatientapptandvisit	1	public PatDoctorScheduleView getPatientApptandVisit(@QueryParam("patientID") Long patientId, @QueryParam("ewdPatientId") Long ewdPatientId, @QueryParam("reqId") String reqId,
patp\PatientPortalServiceImpl.java	/patientPortalService	GET	/getPatientDocumentsList	1	public List<PatientDocuments> getPatientDocumentsList(@QueryParam("mRNo") String mRNo, @QueryParam("category") String category, @QueryParam("unitId") Long unitId)
patp\PatientPortalServiceImpl.java	/patientPortalService	POST	/addPatientPortalDocument	0	public List<PatientIDDocumentDto> addPatientPortalDocument(@BodyParam("body") PatientIDDocumentDto body, @QueryParam("mRNO") String mRNO, @QueryParam("category") String category,
patp\PatientPortalServiceImpl.java	/patientPortalService	POST	/addPatientRegistration	0	public void updatePatient(@BodyParam("body") PatientAppointmentandRegDto body) throws GenericApplicationException 
patp\PatientPortalServiceImpl.java	/patientPortalService	POST	/addUserAnswers	0	public void addUserAnswers(@BodyParam("questionAnswerDtos") List<QuestionAnswerDto> questionAnswerDtos, @QueryParam("opd_ipd_id") Long opd_ipd_id, @QueryParam("opd_ipd") Long opd_i
patp\PatientPortalServiceImpl.java	/patientPortalService	POST	/addwalkinVisit	0	public PatientAppointmentandRegDto bookAppintment(@BodyParam("body") PatientAppointmentandRegDto body, @QueryParam("pat_token_no") String token,
patp\PatientPortalServiceImpl.java	/patientPortalService	POST	/addwalkinVisit_Old	1	public PatientAppointmentandRegDto addwalkinVisit(@BodyParam("body") PatientAppointmentandRegDto body, @QueryParam("pat_token_no") String token, @QueryParam("app_type") String type
patp\PatientPortalServiceImpl.java	/patientPortalService	POST	/feedbackQuery	0	public Integer feedbackQuery(@QueryParam("opd_ipd_id") Long opd_ipd_id, @QueryParam("opd_ipd") Long opd_ipd, @QueryParam("unitId") Long unitId,
patp\PatientPortalServiceImpl.java	/patientPortalService	POST	/getAllBills	1	public List<BillListForSearch_NewResultDto_3> getAllBills(@BodyParam("body") BillListForSearch_NewDto body) throws GenericApplicationException 
patp\PatientPortalServiceImpl.java	/patientPortalService	POST	/getAppointmentScheduleNew	1	public Map<java.util.Date, Map<String, List<AppointmentSlot>>> getAppointmentScheduleServiceNew(@QueryParam("type") @DefaultValue("D") String type,
patp\PatientPortalServiceImpl.java	/patientPortalService	POST	/getLabsGeneralDetailsListForSearch	0	public List<ServiceListForBillResultDto> getLabsGeneralDetailsListForSearch(@BodyParam("body") SearchServiceDto searchServiceDto, @QueryParam("specializationId") Long specializatio
patp\PatientPortalServiceImpl.java	/patientPortalService	POST	/getPatient	1	public List<GetPatientResultDto> getPatient(@BodyParam("body") GetPatientDto body) throws GenericApplicationException 
patp\PatientPortalServiceImpl.java	/patientPortalService	POST	/pathLabListByVisitPP	0	public List<PathOrderBookingDetailList_NEWResultDto_1> pathLabListByVisitPP(@BodyParam("body") PathOrderBookingListDto body, Long opdIpdId) throws GenericApplicationException 
patp\PatientPortalServiceImpl.java	/patientPortalService	POST	/pathLabListPP	0	public List<PathOrderBookingDetailList_NEWResultDto_1> pathLabListPP(@BodyParam("body") PathOrderBookingListDto body) throws GenericApplicationException 
patp\PatientPortalServiceImpl.java	/patientPortalService	POST	/radLabListPP	0	public PathoRadOrderBookingView radLabListPP(@BodyParam("body") PathOrderBookingListDto body) throws GenericApplicationException 
patp\PatientPortalServiceImpl.java	/patientPortalService	POST	/radListByVisitPP	0	public PathoRadOrderBookingView radListByVisitPP(@BodyParam("body") PathOrderBookingListDto body, Long opdIpdId) throws GenericApplicationException 
payment\PaymentTransferServiceImpl.java	/paymentTransfer	GET	/generateOTP	0	public String generateOTP() throws GenericApplicationException, UnsupportedEncodingException 
payment\PaymentTransferServiceImpl.java	/paymentTransfer	GET	/getDisburseAccounts	0	public List<BankAccountMaster> getDisburseAccounts() throws GenericApplicationException, UnsupportedEncodingException 
payment\PaymentTransferServiceImpl.java	/paymentTransfer	GET	/getPaymentStatus	0	public JSONObject getPaymentStatusById(@QueryParam("id") Long paymentTransferId) throws GenericApplicationException, UnsupportedEncodingException 
payment\PaymentTransferServiceImpl.java	/paymentTransfer	POST	/	0	public PaymentTransferListView getPaymentTransferList(@BodyParam("body") PaymentTransSearchView body, @QueryParam("startRowIndex") Integer page, @QueryParam("maxRows") Integer coun
payment\PaymentTransferServiceImpl.java	/paymentTransfer	POST	/createPaymentTransfer	1	public void createPaymentTransfer(@BodyParam("body") PaymentTransfer paymentTransfer, @QueryParam("beneficiaryId") Long beneficiaryId) throws GenericApplicationException, Unsupport
payment\PaymentTransferServiceImpl.java	/paymentTransfer	POST	/verifyOTP	0	public String verifyOTP(@BodyParam("body") PaymentTransferRequest paymentTransferRequest) throws GenericApplicationException, UnsupportedEncodingException 
QueueManagmentServiceImpl.java	/queueManagment	GET	/activeDepartments	0	public List<Departmentmaster> activeDepartments() throws GenericApplicationException 
QueueManagmentServiceImpl.java	/queueManagment	GET	/activeRoomQueue	0	public List<String> activeRoomQueue(@QueryParam("roomId") Long roomId) throws GenericApplicationException 
QueueManagmentServiceImpl.java	/queueManagment	GET	/activeRoomQueueForDoctor	1	public List<DoctorRoomDto> activeRoomQueueForDoctor(@QueryParam("roomId") Long roomId, @QueryParam("depId") Long depId, @QueryParam("token") String token, @QueryParam("mrno") Strin
QueueManagmentServiceImpl.java	/queueManagment	GET	/activeRooms	0	public List<Roommaster> activeRooms(@QueryParam("departmentId") Long departmentId) throws GenericApplicationException 
QueueManagmentServiceImpl.java	/queueManagment	GET	/addToQueue	1	public Map<String, String> createOPDQueueByGET(@QueryParam("machineId") String machineId, @QueryParam("unitId") Long unitId, @QueryParam("token") String token) throws GenericApplic
QueueManagmentServiceImpl.java	/queueManagment	GET	/assignTokenPatientToQ	1	public void assignTokenPatientToQ(@QueryParam("queueId") Long queueId, @QueryParam("departmentId") Long departmentId,
QueueManagmentServiceImpl.java	/queueManagment	GET	/checkoutPatientFromQueue	1	public void checkoutSinglePatientFromQueue(@QueryParam("mrnno") String mrnNo, @QueryParam("tokenNo") String tokenNo, @QueryParam("pccRoomId") Long pccRoomId) throws GenericApplicat
QueueManagmentServiceImpl.java	/queueManagment	GET	/counterQueueMI	0	public RecordDetailsDTO<CounterQueueDTO> getOPDQueue(@QueryParam("machineId") Long machineId, @QueryParam("unitId") Long unitId) throws GenericApplicationException 
QueueManagmentServiceImpl.java	/queueManagment	GET	/departmentQueueMI	0	public RecordDetailsDTO<DeptQueueDTO> getDepartmentQueue(@QueryParam("machineId") Long machineId, @QueryParam("unitId") Long unitId, @QueryParam("dpq") String dpq) throws GenericAp
QueueManagmentServiceImpl.java	/queueManagment	GET	/fetchAbsentOPDToken	1	public RegistrationQueue findTokenFromQueue(@QueryParam("token") String tokenNo,
QueueManagmentServiceImpl.java	/queueManagment	GET	/findPatient	0	public PatientDetailsForDoctorQueue findPatient(@QueryParam("mrnno") String mrnNo, @QueryParam("tokenNo") String tokenNo) throws GenericApplicationException 
QueueManagmentServiceImpl.java	/queueManagment	GET	/findPatientByVisitId	0	public PatientDetailsForDoctorQueue findPatientByVisitId(@QueryParam("visitId") Long visitId) throws GenericApplicationException 
QueueManagmentServiceImpl.java	/queueManagment	GET	/findPatientY	0	public PatientDetailsForDoctorQueue findPatientY(@QueryParam("mrnno") String mrnNo, @QueryParam("tokenNo") String tokenNo) throws GenericApplicationException 
QueueManagmentServiceImpl.java	/queueManagment	GET	/opdGetNextToken	0	public String getNextToken(@QueryParam("qSeriesN") String qSeriesN, @QueryParam("opdStId") Long opdStId, @QueryParam("counterId") Long counterId) throws GenericApplicationException
QueueManagmentServiceImpl.java	/queueManagment	GET	/printToken	1	public Map<String, String> queueTemplate(@QueryParam("id") Long id) throws GenericApplicationException 
QueueManagmentServiceImpl.java	/queueManagment	GET	/seenPatientCount	1	public List<Object[]> getSeenPatientCount(@QueryParam("unitId") Long unitId, @QueryParam("departmentIds") String departmentIds,
QueueManagmentServiceImpl.java	/queueManagment	GET	/updateFillQueue	0	public void stopFillQueue(@QueryParam("cashCounterId") Long cashCounterId, @QueryParam("opdStationId") Long opdStationId, @QueryParam("stop") Boolean isStop) throws GenericApplicat
QueueManagmentServiceImpl.java	/queueManagment	GET	/validatePatientTokenExistsInQ	0	public Map<String, Object> validatePatientTokenExistsInQ(@QueryParam("mrnno") String mrnNo, @QueryParam("tokenNo") String tokenNo, @QueryParam("count") int count) throws GenericApp
QueueManagmentServiceImpl.java	/queueManagment	POST	/addToQueue	1	public RegistrationQueue addToQueue(QueueManagmentDto body) throws GenericApplicationException 
QueueManagmentServiceImpl.java	/queueManagment	POST	/assignPatientToDoctorQueueByNurse	1	public String assignPatientToDoctorQueueByNurse(@QueryParam("doctorQueueId") Long doctorQueueId,
QueueManagmentServiceImpl.java	/queueManagment	POST	/markOPDTokenAsAbsent	0	public void markOPDTokenAsAbsent(@QueryParam("tokenId") Long tokenId) throws GenericApplicationException 
QueueManagmentServiceImpl.java	/queueManagment	PUT	/allocatePatientToDepartmentalQ	1	public void allocatePatientToDepartmentalQ(@QueryParam("patQId") Long patQId, @QueryParam("visitId") Long visitId) throws GenericApplicationException 
QueueManagmentServiceImpl.java	/queueManagment	PUT	/clearRoomToken	0	public void clearRoomToken(@QueryParam("roomId") Long roomId, @QueryParam("depId") Long depId, @QueryParam("tokenNo") String tokenNo) throws GenericApplicationException 
RcmServiceImpl.java	/rcm	GET	/checkEligibleForChiranjeevi	0	public Boolean checkEligibleForChiranjeevi(@QueryParam("admId") Long admId, @QueryParam("patientId") Long patientId, @Context HttpServletResponse response)
RcmServiceImpl.java	/rcm	GET	/getAdmPatientVerification	0	public GetPatientResultDto_1 getAdmPatientVerification(@QueryParam("patientId") Long patientId,
RcmServiceImpl.java	/rcm	GET	/getEligibilityCheckById/{eligibilityCheckById}	0	public EligibilityCheck getEligibilityCheckById(@PathParam("eligibilityCheckById") Long eligibilityCheckById)
RcmServiceImpl.java	/rcm	GET	/getRequestResponse	0	public void getRequest(@QueryParam("id") Long id, @QueryParam("type") String type, @Context HttpServletResponse response)
RcmServiceImpl.java	/rcm	GET	/getRghsPackageDetails	0	public EligibilityCheck getRghsPackageDetails(@QueryParam("eligibilityId")Long id, @QueryParam("patientId")Long patientId) throws GenericApplicationException 
RcmServiceImpl.java	/rcm	GET	/getTidByPatientId	0	public String getTidByPatientId(@QueryParam("unitId")Long unitId, @QueryParam("patientId")Long patientId) throws GenericApplicationException 
RcmServiceImpl.java	/rcm	POST	/getTidListStatus	0	public List<EligibilityCheck> getTidListStatus(@BodyParam("body") EligibilityCheck body, @QueryParam("page") Integer page, @QueryParam("count") Integer count)
SendNotificationServiceImpl.java	/sendEmail	GET	/sendForgotEmail	0	public void sendPasswordForgotMail(@QueryParam("userEmail") String userEmail) throws GenericApplicationException 
SendNotificationServiceImpl.java	/sendEmail	POST	/sendFinalBillEmail	1	public Boolean sendFinalBillEmailApi(DischargeClearanceDto body) throws GenericApplicationException 
SendNotificationServiceImpl.java	/sendEmail	POST	/sendPatientCritcalMail	0	public void sendPatientCritcalMail(NotifyPatientCriticalDetails body) throws GenericApplicationException 
SendNotificationServiceImpl.java	/sendEmail	POST	/sendPatientNoteMail	0	public void sendPatientNoteMail(@BodyParam("body") PatientNote body) throws GenericApplicationException 
SendNotificationServiceImpl.java	/sendEmail	POST	/validateToken	1	public TokenValidateDto resetPassword(TokenValidateDto body) throws GenericApplicationException 
```

## 4. Scheduled jobs (@Scheduled)

Job classes (5): ApplicationJobs.java (largest, 50+ schedules), InventoryJobSchedule.java, Hl7InboundMessageJob.java (commented out), EHRSyncJobs.java (commented out), AutoNextDayAllocation.java (commented out). Scheduler enabled via @EnableScheduling (App.java:78). Cron values resolved from application.properties.

| Job method | Schedule (cron/fixedDelay) | Evidence | Purpose (from name/body) |
|---|---|---|---|
| ApplicationJobs.bedAutoChargesJob | 0 59 11 * * 0 (auto.charges.schedule) | ApplicationJobs.java:248 | Auto bed charges |
| ApplicationJobs.syncPatientsToSolrJob | fixedDelay 5s | ApplicationJobs.java:520 (commented) | Solr patient index |
| ApplicationJobs.closeDailyEncountersJob | 0 0 0 * * * (dailyencounter.schedule) | ApplicationJobs.java:542 | Close daily encounters |
| ApplicationJobs.doctorScheduleLoadToCache | 0 0 * ? * * | ApplicationJobs.java:572 | Doctor schedule cache warm |
| ApplicationJobs.clearCounterUserJob | 0 0 2 * * * | ApplicationJobs.java:578 | Reset counter users |
| ApplicationJobs.doctorCheckoutJob / doctorCheckoutJobOld | 0 0 * ? * * | ApplicationJobs.java:585,614 | Doctor checkout (old variant commented) |
| ApplicationJobs.updateExpiredBloodRequisitionStatusJob | 0 1 1 * * ? | ApplicationJobs.java:734 | Expire blood requisitions |
| ApplicationJobs.updateExpiryStatusOfTheBloodBagsJob | 0 0 23 * * ? | ApplicationJobs.java:769 | Blood bag expiry |
| ApplicationJobs.cleanPerformanceStatsFromCache | fixedDelay 1h | ApplicationJobs.java:818 | Cache perf stats cleanup |
| ApplicationJobs.cancelInactiveDonors | 0 0 * ? * * | ApplicationJobs.java:832 | Inactive donor cancel |
| ApplicationJobs.syncItemsToSolrJob | fixedDelay 5s | ApplicationJobs.java:867 | Solr item index |
| ApplicationJobs.syncHealthFactorToSolrJob | fixedDelay 5s | ApplicationJobs.java:884 | Solr health factor index |
| ApplicationJobs.notifyDueOfMaintenanceJob | 0 0 11 * * ? | ApplicationJobs.java:901 | Equipment maintenance due |
| ApplicationJobs.listDBLocks | fixedDelay 20s | ApplicationJobs.java:927 | DB lock watchdog |
| ApplicationJobs.syncIcd10cmToSolrJob | fixedDelay 5s | ApplicationJobs.java:976 | Solr ICD-10 index |
| ApplicationJobs.updateAbscondInterimAdmissionStatusJob | 0 0 * ? * * | ApplicationJobs.java:991 | Interim admission abscond |
| ApplicationJobs.addEJSAshaIncentives | 0 0 23 * * ? (ejs.asha.incentive.schedule) | ApplicationJobs.java:1031 | ASHA incentive calc |
| ApplicationJobs.decrossBloodBag | 0 0 * ? * * (bloodbag.auto.decross) | ApplicationJobs.java:1045 | Blood bag de-crossmatch |
| ApplicationJobs.rghsDataScheduler | 0 30 23 * * ? (rghsSims.schedule) | ApplicationJobs.java:1063 | RGHS SIMS data push |
| ApplicationJobs.creteEHRFiles | fixedDelay ehr.sync.schedule (2h) | ApplicationJobs.java:1094 | EHR file generation |
| ApplicationJobs.updateIhmsSummaryCounts | 0 30 23 * * ? + 0 0 0 * * ? | ApplicationJobs.java:1109 | IHMS dashboard counts |
| ApplicationJobs.updateIhmsSummaryCountsForCurrentDay | 0 0 */4 ? * * | ApplicationJobs.java:1144 | Dashboard counts (current day) |
| ApplicationJobs.cancelCrossServiceVisit | 0 0 * ? * * | ApplicationJobs.java:1174 | Cancel stale cross-service visits |
| ApplicationJobs.eRaktkoshSaveDonation / updateERaktkoshDonation | 0 59 11 * * 0 | ApplicationJobs.java:1203,1381 | eRaktKosh donation sync |
| ApplicationJobs.updateIhmsAllCounts | 0 30 23 * * ? (ihms.allcount.schedule) | ApplicationJobs.java:1555 | All-count stats |
| ApplicationJobs.triggerWhatappLabReportOne | 0 0/15 0-6,14-23 * * ? | ApplicationJobs.java:1598 | WhatsApp lab report push |
| ApplicationJobs.triggerWhatsappReportsInQueue | fixedDelay 18000ms + cron (commented) | ApplicationJobs.java:1608-1609 | WhatsApp queue drain |
| ApplicationJobs.addAbhaRecordsNode | fixedDelay 5s | ApplicationJobs.java:1658 | ABDM record nodes |
| ApplicationJobs.sendPatientRecordToAbha | fixedDelay 5s | ApplicationJobs.java:1686 | ABDM patient push |
| ApplicationJobs.updateNMCData | 0 0 3 * * ? | ApplicationJobs.java:1730 | NMC (nursing) data |
| ApplicationJobs.abhaDemoAuthProcessor | fixedDelay 5s | ApplicationJobs.java:1897 | ABHA demo auth |
| ApplicationJobs.updateHealthIds | fixedDelay 5s | ApplicationJobs.java:2120 | Health ID sync |
| ApplicationJobs.updatePatientNoteFromTempPrescriptionJob | 0 * * ? * * (operator.ehr.sync.cron) | ApplicationJobs.java:2102 | Temp prescription -> note |
| ApplicationJobs.indexMissingPatientsInSolr | fixedDelay 5s | ApplicationJobs.java:2175 | Solr backfill |
| ApplicationJobs.createPackageMasterOnAllUnits | fixedDelay 5s | ApplicationJobs.java:2194 | Package master fan-out |
| ApplicationJobs.createAutoIndentForSunStoreToMain | 0 0/10 * * * ? (unitWise.autoIndent.schedule) | ApplicationJobs.java:2194 | Auto indent |
| ApplicationJobs.createAutoIndentForMainToEAushadhi | eAushadiItemStockUpdate.schedule 0 0 4 * * ? | ApplicationJobs.java:2373 | eAushadhi auto indent |
| ApplicationJobs.jobForPushingAbhaIdToJanAadhar | 0 */15 14-23 * * ? | ApplicationJobs.java:2537 | ABHA->Jan Aadhar push |
| ApplicationJobs.addStats_Archived | 0 0 23 * * ? | ApplicationJobs.java:2945 | Stats archiving |
| ApplicationJobs.dailySMSToMOIC | 0 30 23 * * ? | ApplicationJobs.java:2953 | Daily SMS to MOIC |
| ApplicationJobs.pushDataToPCTS | 0 0 0 L * ? | ApplicationJobs.java:2961 | PCTS monthly push |
| ApplicationJobs.updateItemMasterFromEaushadhi | 0 0 18 * * ? | ApplicationJobs.java:2961 (line per grep) | eAushadhi item master sync |
| InventoryJobSchedule.averagedailyDemandJob | 0 0 0 * * ? (update.dailydemand.schedule) | InventoryJobSchedule.java:68 | Daily demand avg |
| InventoryJobSchedule.hourlyRemainderCheckJob | fixedDelay 1800000 (30 min) | InventoryJobSchedule.java:371 | Stock remainder |
| InventoryJobSchedule.nursePatientAllocation | 0 0 0 * * * | InventoryJobSchedule.java:469 | Nurse allocation |
| InventoryJobSchedule.readInvenStockExternal | fixedDelay 5s | InventoryJobSchedule.java:503 (commented) | External stock read |
| InventoryJobSchedule.eAushadiItemStockUpdate | 0 0 4 * * ? | InventoryJobSchedule.java:564 | eAushadhi stock |
| Hl7InboundMessageJob.hl7MessageHandlerMain | 0 0 1 * * ? (commented) | Hl7InboundMessageJob.java:51 | HL7 inbound pickup |
| EHRSyncJobs.syncEHR | ehr.sync.schedule (commented) | EHRSyncJobs.java:28 | EHR sync |
| AutoNextDayAllocation.nextDayAllocation | 0 10 1 * * ? (commented) | AutoNextDayAllocation.java:19 | Next-day allocation |

## 5. Transaction hotspots (@Transactional counts per file, top 20)

Total @Transactional occurrences: 1,445 (javax.transaction.Transactional; rollbackOn = Exception.class is the norm; requires @EnableTransactionManagement App.java:73 + JpaTransactionManager App.java:195).

| File | @Transactional count | Notes |
|---|---|---|
| nurse/NursingServiceImpl | 135 | largest single-file tx surface (300 endpoints) |
| custom/InventoryServiceImpl | 126 | stock movements |
| custom/MasterCachedServiceImpl | 54 | masters, mostly cached reads |
| custom/PathoOrderServiceImpl | 50 | pathology orders/results |
| pas/BloodBankServiceImpl | 48 | blood bank, bag lifecycle |
| custom/PharmacyServiceImpl | 49 | dispensing |
| pas/OTSchedulerServiceImpl | 35 | OT scheduling |
| custom/MdmServiceImpl | 34 | MDM data |
| pas/MasterServiceImpl | 33 | masters |
| ejs/EjsDetailsServiceImpl | 27 | ASHA incentive details |
| pas/EquipmentServiceImpl | 27 | equipment |
| util/hl7/HL7InboundHelperUtil | 26 | HL7 inbound (util, not REST) |
| pas/PatientServiceImpl | 24 | patient |
| custom/AddServiceOrderServiceImpl | 22 | charges/orders |
| util/HL7Helper | 22 | HL7 (util) |
| custom/PatientTransferDischargeServiceImpl | 21 | discharge |
| CommonUtilServiceImpl | 21 | shared utils |
| cssd/CSSDServiceImpl | 20 | sterilization |
| aasmaan/AasmaanCaseServiceImpl | 20 | AASMAAN cases |
| custom/AddUserServiceImpl | 19 | user mgmt |
| pas/PreventiveMaintenanceServiceImpl | 18 | PM |

Notable multi-entity tx methods (evidence):
- PatientRegAdmissionServiceImpl.admitPatientInt - admission + bed + advance + charges + bedmaster updates (PatientRegAdmissionServiceImpl.java:241, 1305, 1880)
- AddPaymentServiceImpl.addBillPaymentNew @Transactional - bill payment, advance settle, receipts (AddPaymentServiceImpl.java:30-33)
- AddServiceOrderServiceImpl.addServices (addServicechargesNew) - service order + charge + bill (AddServiceOrderServiceImpl.java)
- PatientTransferDischargeServiceImpl - discharge + bill settle + bed release (PatientTransferDischargeServiceImpl.java:147)
- GenericServiceImpl.crudadd/crudupdate/crudaddMultiple @Transactional (GenericServiceImpl.java:40,59,66) - generic CRUD
- ApplicationJobs (jobs) use @Transactional for long-running multi-entity batches (e.g. ABHA push, eRaktKosh)

## 6. Filters / interceptors / aspects / annotations

| Component | Kind | Evidence | Role |
|---|---|---|---|
| com.ohum.GzipFilter | servlet Filter | GzipFilter.java:53 | GZIP response compression (Terracotta-derived) |
| com.ohum.aspects.IntegrationOperationsFilter | servlet Filter | IntegrationOperationsFilter.java:103 | request/response wrapper for integration ops |
| com.ohum.interceptors.CrudOperationsFilter | servlet Filter | CrudOperationsFilter.java:76 | CRUD filter (__crfls/__crfli/__crflm params) |
| com.ohum.interceptors.emitra.EmitraRedirectFilter | servlet Filter | EmitraRedirectFilter.java | emitra redirect (Rajasthan e-mitra portal) |
| com.ohum.interceptors.sso.SSOAuthFilter | servlet Filter | SSOAuthFilter.java | SSO auth filter |
| com.ohum.interceptors.PreProtocolInterceptor | CXF in-interceptor | PreProtocolInterceptor.java:17 (wired services.xml:235) | forces _type=json, captures CRUD filter params |
| com.ohum.interceptors.RequestResponseInterceptor | CXF provider (JacksonJsonProvider) | RequestResponseInterceptor.java:33 (services.xml:238) | JSON serialization + request/response interceptor |
| com.ohum.mappers.ServiceJAXRSExceptionMapper | JAX-RS ExceptionMapper | ServiceJAXRSExceptionMapper.java:28 (services.xml:239) | wraps all service exceptions |
| com.ohum.mappers.NullableLongParamConverterProvider | ParamConverter | services.xml:242 | nullable Long params |
| com.ohum.aspects.ServiceLayerAspect | Spring @Aspect | ServiceLayerAspect.java:135-137,223 | central around-advice: auth (UserView), @NotSecure skip, audit (AuditEvent/@AuditTrail), PHI decryption of @QueryParam/@FormParam (getEncryptedValue), SP param context, cache TTL reset, @Async/@Scheduled wrappers |
| com.ohum.aspects.UserTrxContextHolder | ThreadLocal helper | UserTrxContextHolder.java | per-request user context |
| com.ohum.audit.AuditHandler / AuditHelper | audit service | AuditHandler.java:34, AuditHelper.java:58 | template-driven (Velocity) audit event persistence; AuditHelper @Transactional saveAuditEvent |
| com.ohum.cache.MemoryCacheImpl (active) / RedisCacheImpl / RedisClusterCacheImpl | cache | applicationContext.xml (MemoryCacheImpl active, expiry 86400s) | @Cached annotation-driven master data cache |
| SpringSecurityConfig | Spring Security | SpringSecurityConfig.java:27 | CSP headers, allowed URLs/referrers (NOT used for endpoint auth; auth is in ServiceLayerAspect) |

Annotation inventory (com.ohum.annotations): AuditEvent, AuditTrail, BackgroundQueue, Base64Data, BodyParam, Cached, CrudApi, CrudCached, CrudCachedIgnore, CrudCachedSet, EncodeAsBase64, ForcedJob, IgnoreApiForAuth, JobEvent, Login, MasterData, NoAuthCacheTTLReset, NotSecure, RequestValidator, RestSuccessResponseStatusCode, RuleParam, Secure, StoredProcDto, StoredProcMultiResultProperty, StoredProcNamedOutParam, StoredProcNamedParam, StoredProcResultProperty.

## 7. Non-REST surfaces (FHIR / ABDM / HCX / HL7)

### ABDM callbacks (com.ohum.xcg.abdm.server.ABDMController, @Path("/abdm"), wired as abdmController bean)
- v2 callback paths `/{versionNumber}/patients/...`, `/users/auth/...`, `/links/...`, `/consent-requests/...`, `/consents/...`, `/health-information/...`, `/subscription-requests/...` (ABDMController.java:243-1213)
- v3 mirror paths `/api/v3/...` (ABDMController.java:454,499,599,689,751,802,896,950,984,1052,1226,1250,1309)
- Catch-all: POST `/{versionNumber}/{endpoint}/on_{action}` (ABDMController.java:1340)
- GET `/{versionNumber}/heartbeat` (ABDMController.java:1218); all callbacks return void and write directly to HttpServletResponse
- Client utils: abdm/client/* (AbhaUtil, HIPUtil, HIUUtil, HPRUtil, PHRUtil, NHCXUtil, FhirDataUtil, CommonABDMUtil - 12 @Transactional)

### HFR (Healthcare Facility Registry) - HFRController @Path("/abdm-hfr")
- 16 endpoints: facility master-data, LGD states/districts/subdistricts, facility-type, owner-subtype, specialities, search, 4-step creation (basic/additional/detailed/submit), software-details, saveFacilityNumber (HFRController.java:60-382)

### HCX (NHCXServiceImpl @Path("/nhcx"), 10 endpoints)
- participants, payor certs, policies, insurancePlanRequest, coverageEligiblityRequest, preAuthSubmit, claimSubmitRequest, showNhcxDocument (NHCXServiceImpl.java)
- JWE RSA-OAEP-256 encryption/decryption with nimbus-jose (HCXPayloadEncryptor.java:23-47); KeyLoader, HCXJWEPayloadParser, InsurancePlanRequest

### FHIR server (HAPI FHIR vendored: ca.uhn.fhir.jaxrs.server.interceptor.JaxRsResponseException)
- com.ohum.xcg.genserv.FhirServer extends ca.uhn.fhir.rest.server.RestfulServer, FHIR R4, registers PatientResourceProvider (IResourceProvider) + TokenBasedSecurityInterceptor + ResponseHighlighterInterceptor (FhirServer.java:41-66)
- Registered as @Bean genFhirServer() @ConditionalOnProperty(abdm.enabled=true) (App.java:261) - NO ServletRegistrationBean found for it; only CXFServlet at /api/* is registered (App.java:149-150). FHIR servlet appears dormant/unreachable unless wired elsewhere. FLAG.
- PatientTransformer + FhirDataUtil map entities <-> FHIR resources
- genserv/interceptors: RbacSecurityInterceptor (commented out), TokenBasedSecurityInterceptor

### HL7 (util/hl7)
- HL7Helper (22 tx), HL7InboundHelperUtil (26 tx), ProcessHL7InboundMessagesUtil (17 tx), Hl7InboundMessageJob (disabled cron), hl7inbound.pickup.freq = 0 0 1 * * ?
- MLLP / file pickup inbound HL7 processing

## 8. Authentication (summary; detail owned by agent 08)
- services.xml:27-31: defAuthImpl = DefaultAuthenticationImpl, customLDAPAuthImpl = CustomLDAPAuthenticationImpl; AuthenticationServiceImpl delegates via AuthenticationInterface
- Endpoints: /auth/loginh (header), /auth/loginf (form), /auth/loginotp, /auth/loginextf, /auth/loginPatient, /auth/loginb (basic), /auth/loginJ (apiToken/apiKey), /auth/loginExt, /auth/logout (PUT), /auth/getuserrole, /auth/getuserroleOtp, /auth/changeUserRole (AuthenticationServiceImpl.java:42-290)
- Auth enforcement is in ServiceLayerAspect.handleWorkflow (ServiceLayerAspect.java:362,403,442): session-based UserView lookup, @NotSecure to bypass, DummyHTTPStatusException(UNAUTHORIZED)
- auth.method property empty; auth.pwd.manager=password; LDAP: LDAPAuthenticationUtil, LDAPBasedRestrictivePasswordValidator, ldap.server.port=636
- PHI: IdEncryptionUtil (@ConfigurationProperties phi.encryption) encrypts PHI fields; aspect decrypts QueryParam/FormParam values via genericDAO.getEncryptedValue (ServiceLayerAspect.java:400-411)

## 9. Top 10 risks / flags

1. 3,003 hand-annotated JAX-RS endpoints with inconsistent conventions - any rebuild must prioritize the 300-endpoint monoliths: NursingServiceImpl, MasterCachedServiceImpl, InventoryServiceImpl, PathoOrderServiceImpl.
2. God classes: NursingServiceImpl (300 endpoints, 17,350 LOC), PathoOrderServiceImpl (133, 14,280), MasterCachedServiceImpl (264, 12,710), InventoryServiceImpl (235, 12,070) - single file = single tx boundary = contention and unmaintainability.
3. SQL Server-unfriendly storage: JPA queries + native SQL + stored procedures mixed; GenericDAO supports 6 query types incl. storedProcQ() - stored procedures are a hard dependency for many ResultDto endpoints.
4. Auth is aspect-based (ServiceLayerAspect), not declarative security - @NotSecure scattered; new endpoints default to secured; Spring Security only sets CSP headers (SpringSecurityConfig).
5. @Transactional(rollbackOn=Exception.class) on 1,056 endpoints with JPA-managed long transactions - risk of long-held locks; jobs run inside tx (ApplicationJobs has 14 tx) - batch jobs may lock tables for minutes.
6. ABDM callback endpoints write raw HttpServletResponse and swallow errors - no structured error contract; catch-all /{endpoint}/on_{action} makes routing unpredictable.
7. FhirServer bean is created but appears to have no servlet registration - dead/dormant FHIR surface or registration is external.
8. Report/print endpoints return raw HTML/String (Velocity + jasperreports) - tight coupling of rendering to backend; ~60+ report endpoints.
9. PHI encryption/decryption is custom (IdEncryptionUtil + aspect) - key rotation and field-level encryption strategy must be preserved in rebuild.
10. Duplicate/overloaded endpoints (uploadHospitalLocation x2, addToQueue GET+POST) and dual-version ABDM paths increase regression risk; commented-out endpoints litter code (parser skipped them) - dead code volume is high.



### 3b. ABDM / HFR callback endpoints (57) — com.ohum.xcg.abdm.server (NOT under services/; wired as abdmController/hfrController beans)

```
ABDMController.java	/abdm	GET	/{versionNumber}/heartbeat	0	public String onHeartbeat(@PathParam("versionNumber") String versionNumber, @Context HttpServletRequest req) 
ABDMController.java	/abdm	POST	/{versionNumber}/{endpoint}/on_{action}	0	public void nhcxCallbacks(@BodyParam("body") Map<String, Object> payload,
ABDMController.java	/abdm	POST	/{versionNumber}/care-contexts/discover	0	public void onCareContextsDiscover(@BodyParam("body") String body, @PathParam("versionNumber") String versionNumber, @Context HttpServletRequest hreq, @Context HttpServletResponse 
ABDMController.java	/abdm	POST	/{versionNumber}/consent-requests/on-init	0	public void onConsentRequestInit(@BodyParam("body") String body, @PathParam("versionNumber") String versionNumber, @Context HttpServletRequest hreq, @Context HttpServletResponse hr
ABDMController.java	/abdm	POST	/{versionNumber}/consents/hip/notify	0	public void onConsentHIPNotify(@BodyParam("body") String body, @PathParam("versionNumber") String versionNumber, @Context HttpServletRequest hreq, @Context HttpServletResponse hres
ABDMController.java	/abdm	POST	/{versionNumber}/consents/hiu/notify	0	public void onConsentHIUNotify(@BodyParam("body") String body, @PathParam("versionNumber") String versionNumber, @Context HttpServletRequest hreq, @Context HttpServletResponse hres
ABDMController.java	/abdm	POST	/{versionNumber}/consents/on-fetch	0	public void onConsentFetch(@BodyParam("body") String body, @PathParam("versionNumber") String versionNumber, @Context HttpServletRequest hreq, @Context HttpServletResponse hres) 
ABDMController.java	/abdm	POST	/{versionNumber}/health-information/hip/request	0	public void onHealthInfoHIPRequest(@BodyParam("body") String body, @PathParam("versionNumber") String versionNumber, @Context HttpServletRequest hreq, @Context HttpServletResponse 
ABDMController.java	/abdm	POST	/{versionNumber}/health-information/hiu/on-request	0	public void onHealthInfoHIURequest(@BodyParam("body") String body, @PathParam("versionNumber") String versionNumber, @Context HttpServletRequest hreq, @Context HttpServletResponse 
ABDMController.java	/abdm	POST	/{versionNumber}/links/context/notify	0	public void onLinkContextNotify(@BodyParam("body") String body, @PathParam("versionNumber") String versionNumber, @Context HttpServletRequest hreq, @Context HttpServletResponse hre
ABDMController.java	/abdm	POST	/{versionNumber}/links/context/on-notify	0	public void onLinkContextOnNotify(@BodyParam("body") String body, @PathParam("versionNumber") String versionNumber, @Context HttpServletRequest hreq, @Context HttpServletResponse h
ABDMController.java	/abdm	POST	/{versionNumber}/links/link/confirm	0	public void onLinkConfirm(@BodyParam("body") String body, @PathParam("versionNumber") String versionNumber, @Context HttpServletRequest hreq, @Context HttpServletResponse hres) 
ABDMController.java	/abdm	POST	/{versionNumber}/links/link/init	0	public void onLinkInit(@BodyParam("body") String body, @PathParam("versionNumber") String versionNumber, @Context HttpServletRequest hreq, @Context HttpServletResponse hres) 
ABDMController.java	/abdm	POST	/{versionNumber}/links/link/on-add-contexts	0	public void onLinkAddContext(@BodyParam("body") String body, @PathParam("versionNumber") String versionNumber, @Context HttpServletRequest hreq, @Context HttpServletResponse hres) 
ABDMController.java	/abdm	POST	/{versionNumber}/links/link/on-add-contexts	0	public void onLinkAddContextv3(@BodyParam("body") String body, @PathParam("versionNumber") String versionNumber, @Context HttpServletRequest hreq, @Context HttpServletResponse hres
ABDMController.java	/abdm	POST	/{versionNumber}/patients/on-find	0	public void onPatientsFind(@BodyParam("body") String body, @PathParam("versionNumber") String versionNumber, @Context HttpServletRequest hreq, @Context HttpServletResponse hres) 
ABDMController.java	/abdm	POST	/{versionNumber}/patients/profile/on-share	0	public void onPatientsProfileOnShare(@BodyParam("body") String body, @PathParam("versionNumber") String versionNumber, @Context HttpServletRequest hreq, @Context HttpServletRespons
ABDMController.java	/abdm	POST	/{versionNumber}/patients/profile/share	0	public void onPatientsProfileShare(@BodyParam("body") String body, @PathParam("versionNumber") String versionNumber, @Context HttpServletRequest hreq, @Context HttpServletResponse 
ABDMController.java	/abdm	POST	/{versionNumber}/patients/sms/on-notify	0	public void onPatientsSMSNotify(@BodyParam("body") String body, @PathParam("versionNumber") String versionNumber, @Context HttpServletRequest hreq, @Context HttpServletResponse hre
ABDMController.java	/abdm	POST	/{versionNumber}/patients/status/notify	0	public void onPatientStatusNotify(@BodyParam("body") String body, @PathParam("versionNumber") String versionNumber, @Context HttpServletRequest hreq, @Context HttpServletResponse h
ABDMController.java	/abdm	POST	/{versionNumber}/subscription-requests/hiu/notify	0	public void onSubscriptionRequestHIUNotify(@BodyParam("body") String body, @PathParam("versionNumber") String versionNumber, @Context HttpServletRequest hreq, @Context HttpServletR
ABDMController.java	/abdm	POST	/{versionNumber}/subscriptions/hiu/notify	0	public void onSubscriptionsHIUNotify(@BodyParam("body") String body, @PathParam("versionNumber") String versionNumber, @Context HttpServletRequest hreq, @Context HttpServletRespons
ABDMController.java	/abdm	POST	/{versionNumber}/users/auth/notify	0	public void onUserAuthNotify(@BodyParam("body") String body, @PathParam("versionNumber") String versionNumber, @Context HttpServletRequest hreq, @Context HttpServletResponse hres) 
ABDMController.java	/abdm	POST	/{versionNumber}/users/auth/on-confirm	0	public void onUserAuthConfirm(@BodyParam("body") String body, @PathParam("versionNumber") String versionNumber, @Context HttpServletRequest hreq, @Context HttpServletResponse hres)
ABDMController.java	/abdm	POST	/{versionNumber}/users/auth/on-fetch-modes	0	public void onUserAuthFetchModes(@BodyParam("body") String body, @PathParam("versionNumber") String versionNumber, @Context HttpServletRequest hreq, @Context HttpServletResponse hr
ABDMController.java	/abdm	POST	/{versionNumber}/users/auth/on-init	0	public void onUserAuthInit(@BodyParam("body") String body, @PathParam("versionNumber") String versionNumber, @Context HttpServletRequest hreq, @Context HttpServletResponse hres) 
ABDMController.java	/abdm	POST	/api/v3/consent/request/hip/notify	0	public void onConsentHIPNotifyv3(@BodyParam("body") String body, @Context HttpServletRequest hreq, @Context HttpServletResponse hres) 
ABDMController.java	/abdm	POST	/api/v3/hip/health-information/request	0	public void onHealthInfoHIPRequestv3(@BodyParam("body") String body, @Context HttpServletRequest hreq, @Context HttpServletResponse hres) throws GenericApplicationException 
ABDMController.java	/abdm	POST	/api/v3/hip/link/care-context/confirm	0	public void onLinkConfirmv3(@BodyParam("body") String body, @Context HttpServletRequest hreq, @Context HttpServletResponse hres) 
ABDMController.java	/abdm	POST	/api/v3/hip/link/care-context/init	0	public void onLinkInitv3(@BodyParam("body") String body, @Context HttpServletRequest hreq, @Context HttpServletResponse hres) 
ABDMController.java	/abdm	POST	/api/v3/hip/patient/care-context/discover	0	public void onCareContextsDiscoverv3(@BodyParam("body") String body, @Context HttpServletRequest hreq, @Context HttpServletResponse hres) throws GenericApplicationException 
ABDMController.java	/abdm	POST	/api/v3/hip/patient/share	0	public void onPatientsProfileSharev3(@BodyParam("body") String body, @Context HttpServletRequest hreq, @Context HttpServletResponse hres) 
ABDMController.java	/abdm	POST	/api/v3/hip/token/on-generate-token	0	public void onGenerateToken(@BodyParam("body") String body, @Context HttpServletRequest hreq, @Context HttpServletResponse hres) 
ABDMController.java	/abdm	POST	/api/v3/hiu/consent/on-fetch	0	public void onConsentFetchv3(@BodyParam("body") String body, @Context HttpServletRequest hreq, @Context HttpServletResponse hres) 
ABDMController.java	/abdm	POST	/api/v3/hiu/consent/request/notify	0	public void onConsentHIUNotifyv3(@BodyParam("body") String body, @Context HttpServletRequest hreq, @Context HttpServletResponse hres) 
ABDMController.java	/abdm	POST	/api/v3/hiu/consent/request/on-init	0	public void onConsentRequestInitv3(@BodyParam("body") String body, @Context HttpServletRequest hreq, @Context HttpServletResponse hres) 
ABDMController.java	/abdm	POST	/api/v3/hiu/health-information/on-request	0	public void onHealthInfoHIURequestv3(@BodyParam("body") String body, @Context HttpServletRequest hreq, @Context HttpServletResponse hres) 
ABDMController.java	/abdm	POST	/api/v3/link/on_carecontext	0	public void onCareContextv3(@BodyParam("body") String body, @Context HttpServletRequest hreq, @Context HttpServletResponse hres) 
ABDMController.java	/abdm	POST	/api/v3/links/context/on-notify	0	public void onLinkContextOnNotifyV3(@BodyParam("body") String body, @Context HttpServletRequest hreq, @Context HttpServletResponse hres) 
ABDMController.java	/abdm	POST	/api/v3/patients/sms/on-notify	0	public void onPatientsSMSNotifyv3(@BodyParam("body") String body, @Context HttpServletRequest hreq, @Context HttpServletResponse hres) 
ABDMController.java	/abdm	POST	/health-information/transfer/{uniqId}	0	public void onHealthInfoDataRx(@BodyParam("body") String body, @PathParam("uniqId") String uniqId, @Context HttpServletRequest hreq, @Context HttpServletResponse hres) 
ABDMController.java	/abdm	POST	/health-information/transfer/uniqId/{uniqId}	0	public void onHealthInfoDataRxOld(@BodyParam("body") String body, @PathParam("uniqId") String uniqId, @Context HttpServletRequest hreq, @Context HttpServletResponse hres) 
HFRController.java	/abdm-hfr	GET	/{versionNumber}/facility/get-master-data	0	public String getMasterData(@PathParam("versionNumber") String versionNumber, @QueryParam("type") String type) throws GenericApplicationException 
HFRController.java	/abdm-hfr	GET	/{versionNumber}/facility/lgd/districts	0	public String getLGDDistricts(@PathParam("versionNumber") String versionNumber, @QueryParam("stateCode") String stateCode) throws GenericApplicationException 
HFRController.java	/abdm-hfr	GET	/{versionNumber}/facility/lgd/states	0	public String getLGDStates(@PathParam("versionNumber") String versionNumber) throws GenericApplicationException 
HFRController.java	/abdm-hfr	GET	/{versionNumber}/facility/lgd/subdistricts	0	public String getLGDSubDistricts(@PathParam("versionNumber") String versionNumber, @QueryParam("districtCode") String districtCode) throws GenericApplicationException 
HFRController.java	/abdm-hfr	GET	/{versionNumber}/facility/software-details	0	public String getSoftwareDetails(@PathParam("versionNumber") String versionNumber) throws GenericApplicationException 
HFRController.java	/abdm-hfr	POST	/{versionNumber}/facility/additional-information	0	public String createFaciltityAdditionalStep2(@PathParam("versionNumber") String versionNumber, @BodyParam("body") FacilityAdditionalInformationRequest body) throws GenericApplicati
HFRController.java	/abdm-hfr	POST	/{versionNumber}/facility/basic-information	0	public String createFaciltityBasicStep1(@PathParam("versionNumber") String versionNumber, @BodyParam("body") FacilityBasicInformationRequest body) throws GenericApplicationExceptio
HFRController.java	/abdm-hfr	POST	/{versionNumber}/facility/detailed-information	0	public String createFaciltityDetailedStep3(@PathParam("versionNumber") String versionNumber, @BodyParam("body") FacilityDetailedInformationRequest body) throws GenericApplicationEx
HFRController.java	/abdm-hfr	POST	/{versionNumber}/facility/fetch-facility-Sub-type	0	public String getFacilitySubType(@PathParam("versionNumber") String versionNumber, @BodyParam("body") FacilitySubTypeRequest body) throws GenericApplicationException 
HFRController.java	/abdm-hfr	POST	/{versionNumber}/facility/fetch-facility-type	0	public String getFacilityType(@PathParam("versionNumber") String versionNumber, @BodyParam("body") FacilityTypeRequest body) throws GenericApplicationException 
HFRController.java	/abdm-hfr	POST	/{versionNumber}/facility/get-owner-subtype	0	public String getOwnerSubTypes(@PathParam("versionNumber") String versionNumber, @BodyParam("body") OwnershipSubTypeRequest body) throws GenericApplicationException 
HFRController.java	/abdm-hfr	POST	/{versionNumber}/facility/get-specialities	0	public String getSpecialities(@PathParam("versionNumber") String versionNumber, @BodyParam("body") SpecialitiesRequest body) throws GenericApplicationException 
HFRController.java	/abdm-hfr	POST	/{versionNumber}/facility/search	0	public String searchFaciltity(@PathParam("versionNumber") String versionNumber, @BodyParam("body") SearchForFacilitiesRequest body) throws GenericApplicationException 
HFRController.java	/abdm-hfr	POST	/{versionNumber}/facility/submit-facility	0	public String createFaciltityFinal(@PathParam("versionNumber") String versionNumber, @BodyParam("body") FacilitySubmitDetails body) throws GenericApplicationException 
HFRController.java	/abdm-hfr	POST	/saveFacilityNumber	0	public boolean saveFacilityNumber(@QueryParam("unitId") Long unitId, @QueryParam("facilityId") String facilityId) throws GenericApplicationException 
```


