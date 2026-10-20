# REPORT-INVENTORY — iHMS Discoverable Outputs (print/report/export surface)

> Synthesized 2026-08-14 by the Report & API Inventory Synthesizer. Evidence-first; every row traced to source (D:\ihms) or the
> discovery docs (04-api-catalog.md, agent-03/04 findings, reconciliation-notes.md). Unknowns are marked `[UNKNOWN]`. No code changed.

---

## 1. Executive summary

| # | Statement | Evidence |
|---|---|---|
| 1 | There is **one central report dispatcher**: `IntegrationOperationsFilter` (servlet Filter, `@Order(HIGHEST_PRECEDENCE-2)`), serving `POST /IHMS/api/rest/reports/<reportName>` (`rep.api.path.prefix = /IHMS/api/rest/reports`, application.properties:369) | IntegrationOperationsFilter.java:469-617; application.properties:367-371 |
| 2 | The dispatcher is **config-driven**: reportMap loaded at startup from `int.files.path/reports/*.json` (**1 file: `reports.json` with 50 report keys**) + `reports/reports.js` (Nashorn; **currently an empty `Report = {}` object** — no helper scripts active) | IntegrationOperationsFilter.java:267-278, 280-321; `D:\ihms\integration\reports\reports.json`; `reports.js` (1 line) |
| 3 | `int.files.path` = `D:/RISL/ihms/integration` at runtime (application.properties:375); the repo carries the deployable copy at **`D:\ihms\integration\`** (reports/, scripts/api.js, templates/, mappings.json, tiuxml/) | directory listing |
| 4 | Dispatcher output modes: **(a) HTML** (otemplate, `contentType` per config, default text/html, gzip), **(b) PDF** (Velocity template → HTML → PDF; `is_print=true` or >1,000 rows), **(c) CSV** (`isCsv=true` → CSV file, `{"fileName": ...}` JSON) | IntegrationOperationsFilter.java:504-574 |
| 5 | Report SQL is **raw Oracle SQL in JSON config** (driver key `oracle.jdbc.OracleDriver`); `@N` → comma-list substitution, `?N` → JDBC bind; executed via `ReportServiceImpl.handleReportRequests` (native query) | ReportServiceImpl.java:31-88; reports.json |
| 6 | **No Jasper (`.jrxml`/`.jasper`) files exist in the repo.** Jasper is *configured* (jasperreports + jasper-jdt in pom.xml:583-613, `jasperreports_extension.properties`, `fonts/fonts.xml` + DejaVuSans.ttf) and *used* by `BillInvoiceGenerator` (JasperCompileManager, classpath `/jrxml/<file>`, `bill.jrxml.prefix=` empty) but only **3 jrxml usages are live** (`prescription.jrxml`, `doctorPrescription.jrxml` in PrescriptionMasterServiceImpl:242-244,372-374; `opdCard.jrxml` in PatientRegApptServiceImpl:1631). ~40 other jrxml references are **commented out**. Template files must be deployed at runtime outside the repo → **[UNKNOWN] location** | pom.xml; DownloadFileUtil.java:212-218,221; BillInvoiceGenerator.java:117 |
| 7 | **No `.vm` (Velocity) templates in repo**, although the dispatcher references `templates/generic_reports.vm` (default) and `userwiseCollectionReport.vm`/`servicewiseCollectionReport.vm` (reports.json `reporttemplate`). Velocity templates are resolved at runtime → **deployed externally [UNKNOWN]** | PdfGenerator.java:129-208; DownloadFileUtil.java:254; reports.json |
| 8 | Excel: **POI is used for *reading* only** (master uploads: `FileStoreUtil.readCsvFamilyFile/readXlsFamilyFile/readXlsFileBySheetName`, GenericConstants.java:356). **No xlsx writer** found; "Excel export" is dispatcher CSV (`isCsv=true`) or `DownloadFileUtil.convertHtmlToCsv` | FileStoreUtil.java:272-612; IntegrationOperationsFilter.java:540-569 |
| 9 | Printer routing is **client-driven**: QZ Tray (qz-tray.js; `print.server.url = ws://localhost:8182/`), Zebra BrowserPrint SDK (patho-modules.js:8587+), window.print() over `#printable`, and dot-matrix `plainText` templates; printer prefs in localStorage (`DefaultSystemPrinter`, `BarcodePrinter`, `NoOfCopies`, `isADotMatrixPrinter`) | 03-legacy-frontend-inventory.md §1.4/§3/§4; qz-tray.js; printerSetting-ext.js |
| 10 | **None of the 50 dispatcher reports are scheduled** — all on-demand POSTs. Background "reports" data (e.g. janta clinic, e-RaktKosh, auto-indents) is produced by the **5 @Scheduled job classes** (ApplicationJobs ~40 cron/fixedDelay, InventoryJobSchedule ~6, EHRSyncJobs, Hl7InboundMessageJob, AutoNextDayAllocation) | agent-04 findings §0 (55 @Scheduled, 5 job classes); job files |
| 11 | Beyond the dispatcher, the JAX-RS catalog itself exposes **49 `/xxxReport`-suffixed + 72 print/PDF-suffixed endpoints** returning print-ready DTOs or HTML/PDF strings | 04-api-catalog.md (row counts) |
| 12 | Frontend trigger pattern: report screen schemas (`modules/reports/schemas/*.json`) declare `raReport` crudop (`is_print=false` → HTML) and `printReceipt` crudop (`is_print=true` → `{"fileName"}` → `downloadFileData`/`downloadReportPdf`); report-module.js also calls RMRS print endpoints directly | e.g. ageWiseRegistrationReportSchema.json:181-220 |

---

## 2. The report dispatcher (IntegrationOperationsFilter) — mechanics

Routing precedence (highest first): **IntegrationOperationsFilter** (`HIGHEST_PRECEDENCE-2`) → **CrudOperationsFilter** (`HIGHEST_PRECEDENCE-1`) → CXF JAX-RS bus `/rest` (services.xml).

```
POST /IHMS/api/rest/reports/<reportName>          (rep.api.path.prefix)
  ├─ token required (UserView from cacheImpl) else 401; POST-only else 401      (lines 477-482)
  ├─ reportMap lookup (loaded from int.files.path/reports/*.json at init)        (line 483)
  ├─ itemplate: file → Faug.templatizeWithMaps; else JS fn in reports.js (empty) (487-502)
  ├─ rp.handleReportRequests(struc, inmap)  → native SQL → List<Map>             (507, ReportServiceImpl:31-88)
  ├─ if is_print=true OR rows > 1000 → "print" branch:                          (508)
  │     • isCsv=true  → CSV written to tmp → {"fileName": "xxx.csv"}            (540-569)
  │     • else        → Velocity (templates/<reporttemplate> or generic_reports.vm)
  │                    → createHtmlToPdfFile → PDF → {"fileName": ...}           (571-573)
  └─ else → otemplate (file or JS fn) → HTML/`contentType` response (gzip)      (576-607)
```

`ReportStructureDto` fields (config schema): `query` (Map<driver, SQL>), `inParams` (`name:Type`), `outParams`, `outParamLabels`, `itemplate`, `otemplate` (+ `isItScript`/`isOtScript`), `contentType`, `reportHeader`, `reporttemplate` (Velocity template name). — ReportStructureDto.java:22-49

---

## 3. Dispatcher report catalog (`integration/reports/reports.json` — 50 report keys)

All: **Source** = `D:\ihms\integration\reports\reports.json` + matching HTML otemplate in same dir; **Output** = HTML by default, PDF (`is_print=true`), CSV (`isCsv=true`); **Actor** = logged-in role permitted by frontend menu (RBAC is frontend-gated; backend checks only token); **Scheduled** = No (all rows); **Frontend trigger** = `modules/reports/schemas/*.json` crudops unless noted. Tables column lists tables seen in the config SQL (`[UNKNOWN]` = SQL not inspected; query source is reports.json).

### 3.1 PAS / administrative reports (13)

| Report (dispatcher key) | Module | Parameters (inParams) | Query/SQL — key tables | Output | Business rule refs / notes |
|---|---|---|---|---|---|
| `getAppointmentReport` | OP/Appointments | unitId, date, genderid, visitType, isOnline, doctorId, departmentId, visitMark, planId, isCsv | [UNKNOWN — SQL in reports.json] | HTML/PDF/CSV | FE schema: showAppointmentReportSchema.json |
| `getAdmissionReport` | IP/Admission | unitId, fromDate, genderid, planId, admDoctorId, wardId, toDate, isAdmitted, isCsv | [UNKNOWN] | HTML/PDF/CSV | FE schema: showAdmissionReportSchema.json |
| `getCancelledAdmissionReport` | IP/Admission | unitId, fromDate, genderid, planId, admDoctorId, wardId, toDate, isAdmitted, isCsv | T_ADMISSION, T_ADMISSIONDETAILS, T_REGISTRATION, T_REGISTRATION_DETAILS, M_DOCTORMASTER, M_GENDERMASTER, T_SPONSOR, M_PLANMASTER, M_BEDMASTER, M_WARDMASTER, T_CancelledAdmissions, M_UNITMASTER, M_CLASSMASTER, M_BUILDINGMASTER, M_PREFIXMASTER | HTML/PDF/CSV | Cancellation reason joined from T_CancelledAdmissions |
| `getDailyDischargeReport` | IP/Discharge | unitId, fromDate, toDate | [UNKNOWN] | HTML/PDF/CSV | FE schema: dailyDischargeReportSchema.json |
| `getPatientDeathReport` | IP/Mortality | unitId, fromDate, toDate | [UNKNOWN] | HTML/PDF/CSV | FE schema: patientDeathReportSchema.json |
| `getAgeWiseRegistrationReport` | Registration | unitId, fromDate, toDate | [UNKNOWN] (special-case: dispatcher builds `awrMap` via ReportServiceImpl.getAgeWiseRegistrationReportMap) | HTML/PDF/CSV | Hard-coded branch IntegrationOperationsFilter.java:517-520; FE schema: ageWiseRegistrationReportSchema.json |
| `getCategoryWisePatientReport` | Registration | unitId, fromDate, toDate | [UNKNOWN] | HTML/PDF/CSV | FE schema: categoryWisePatientReportSchema.json |
| `getAmbulanceRegisterReport` | Transport | unitId, fromDate, toDate | [UNKNOWN] | HTML/PDF/CSV | FE schema: ambulanceRegisterReportSchema.json |
| `getDepartmentwiseOpdPatientStatistics` | OP/Stats | unitId, fromDate, toDate | [UNKNOWN] | HTML/PDF/CSV | FE schema: departmentwiseOpdPatientStatistics.json |
| `getIpdStatisticalReport` | IP/Stats | unitId, fromDate, toDate | [UNKNOWN] | HTML/PDF/CSV | FE schema: ipdStatisticalReportSchema.json |
| `getPatientFeedBackSummary` | Enquiry/Feedback | unitId | [UNKNOWN] | HTML/PDF/CSV | FE schema: patientFeedBackSummarySchema.json |
| `getUserDetailsList` | Users (config export) | unitId, isCsv | t_userdetails, m_staffmaster, m_prefixmaster, m_gendermaster, m_staffdepartmentdetails, m_doctordepartmentdetails, m_departmentmaster, t_userroles, t_userrolemaster | **CSV** (contentType text/csv, no otemplate) | Role IDs LISTAGG — exposes `password` column in output |
| `getUnitDetailsList` | Units (config export) | unitId, isCsv | m_unitmaster | **CSV** | Includes `apiKey` in output — secrets exposure risk |

### 3.2 Collection / billing reports (6)

| Report | Module | Parameters | Key tables | Output | Notes |
|---|---|---|---|---|---|
| `getUserwiseCollectionReport` | Billing/Collections | unitId, fromDate, user, toDate, billType, mode, transactionType, isCsv | [UNKNOWN — SQL in reports.json; "Old" variant verified: t_payment, T_PaymentBillDetails, T_PaymentModeDetails, T_PaymentAdvanceDetails, t_bill, t_itemsale, T_Visit, T_Registration_Int, T_Registration_Details_Int, t_refund/t_refunddetail, t_advance, M_PaymentModeType, M_CompanyMaster, T_UserDetails, m_staffmaster, m_unitmaster, M_PREFIXMASTER, t_itemsalereturn, t_creditnote] | HTML/PDF/CSV | BillType filter: Service vs Pharmacy; modes Payment/Refund; FE: getUserwiseCollectionReportSchema.json (+_Old) |
| `getUserwiseSummaryReport` | Billing/Collections | unitId, fromDate, user, toDate, isCsv | [UNKNOWN; Old variant verified: t_charges, m_servicemaster, M_SPECIALIZATION, t_billdetails, t_bill, T_PAYMENTBILLDETAILS, t_payment, T_USERDETAILS, m_staffmaster, t_refund, t_refunddetail, t_creditnote/t_creditnotedetails] | HTML/PDF/CSV | reporttemplate `userwiseCollectionReport.vm` on Old variant |
| `getServicewiseCollectionReport` | Billing/Collections | unitId, fromDate, user, toDate, specialization, isCsv | [UNKNOWN; Old variant: same payment/bill/charge chain] | HTML/PDF/CSV | reporttemplate `servicewiseCollectionReport.vm` on Old variant |
| `getUserwiseCollectionReportOld` / `getUserwiseSummaryReportOld` / `getServicewiseCollectionReportOld` | Billing (legacy) | same as above | see above | HTML/PDF/CSV | **Duplicate of the 3 above** — legacy variants kept; FE schemas `...SchemaOld.json` still POST these keys |

### 3.3 Lab & CSSD reports (3)

| Report | Module | Parameters | Key tables | Output | Notes |
|---|---|---|---|---|---|
| `getLabReport` | Pathology | unitId, date, genderid, opd_Ipd, isRejected, testType, planId, testName, isCsv | [UNKNOWN] | HTML/PDF/CSV | FE: showLabReportSchema.json |
| `getLabCenterTestWiseReport` | Pathology | unitId, fromDate, toDate | [UNKNOWN] | HTML/PDF/CSV | FE: labCenterTestWiseReportSchema.json |
| `getCssdReport` | CSSD | unitId, fromDate, toDate | [UNKNOWN] | HTML/PDF/CSV | FE: showCssdReportSchema.json |

### 3.4 Support services (5)

| Report | Module | Parameters | Key tables | Output | Notes |
|---|---|---|---|---|---|
| `getLinenLaundryReport` | Linen | unitId, fromDate, toDate, storeId, reqStatus | [UNKNOWN] | HTML/PDF/CSV | FE: showLinenLaundryReportSchema.json |
| `getBioWasteReport` | Bio-medical waste | unitId, fromDate, toDate, fromStoreId, toStoreId, bagId | [UNKNOWN] | HTML/PDF/CSV | FE: showBioWasteReportSchema.json |
| `getEquipmentUsageReport` | Equipment | unitId, storeid | [UNKNOWN] | HTML/PDF/CSV | FE: showEquipmentUsageReportSchema.json |
| `getVehicleTripReport` | Transport | unitId, fromDate, toDate, vehicleId, driverId | [UNKNOWN] | HTML/PDF/CSV | FE: showVehicleTripReportSchema.json |
| `getFuelConsumptionReport` | Transport | unitId, fromDate, toDate, vehicleId | [UNKNOWN] | HTML/PDF/CSV | FE: showFuelConsumptionReportSchema.json |

### 3.5 Blood bank reports (21) — largest dispatcher cluster

All parameters: `unitId, fromDate, toDate` + filters below. Verified table families: T_BLOODDONORREGISTRATION(DETAILS), T_BLOODBAGCOLLECTION, T_BLOODBAGEXTRACTION, T_BLOODBAGISSUE, T_PATIENTREQUISITION(DETAILS), T_PATIENTREQUISITIONDETAILS, t_pathOrderBooking(Details), t_encounter, t_registration, M_BLOODGROUPMASTER, M_BLOODCOMPONENTMASTER, m_pathoTestMaster, M_SERVICEMASTER, M_PATHOTESTPARAMETER, M_PATHOPARAMETERMASTER, T_pathpatientreport, T_PathPatientParameterDetails, m_blooddiscardreasonmaster, m_blooddonationcampmaster, T_DONORMEDICALEXAMINATIONRESULT, m_unitmaster.

| Report | Extra params | Key tables (verified where noted) | Output | Notes |
|---|---|---|---|---|
| `getbloodDonorRegistrationReport` | isExternal, gender, donorType, bloodGroup | [UNKNOWN] | HTML/PDF/CSV | FE: bloodDonorRegistrationReport.json |
| `getBloodBagIssueReport` | externalOrInternal, issueType, componentId, bloodGroup | [UNKNOWN] | HTML/PDF/CSV | FE: bloodBagIssueReport.json |
| `getBloodGroupWiseStaticReport` | bloodGroup, externalOrInternal | [UNKNOWN] | HTML/PDF/CSV | FE: bloodGroupWiseStaticReport.json |
| `getDonationBloodGroupWiseReport` | bloodGroup | T_BLOODBAGCOLLECTION + donors | HTML/PDF/CSV | otemplate donationBloodGroupWiseReport.html (in repo) |
| `getBloodBagTypeReport` | bagType | [UNKNOWN] | HTML/PDF/CSV | FE: bloodBagTypeDetailsReport.json |
| `getCampDonorStatisticReport` | nameOfCamp | [UNKNOWN] | HTML/PDF/CSV | FE: showCampDonorStatisticReport.json |
| `getCampDonorDetailsReport` | gender, bloodGroup | [UNKNOWN] | HTML/PDF/CSV | FE: showCampDonorDetailsReport.json |
| `getDailyBloodstockBookReport` | bloodGroup, componentId | [UNKNOWN] | HTML/PDF/CSV | FE: showDailyBloodstockBookReport.json |
| `getDonationStatusReport` | — | [UNKNOWN] | HTML/PDF/CSV | FE: showDonationStatusReport.json |
| `getCampBagIssueDetailsReport` | bloodGroup, componentId, nameOfCamp | [UNKNOWN] | HTML/PDF/CSV | FE: showCampBagIssueDetailsReport.json |
| `getDonorPhysicalStatusReport` | bloodGroup | [UNKNOWN] | HTML/PDF/CSV | FE: showDonorPhysicalStatusReport.json |
| `getBloodBankReqBalanceSheetReport` | bloodGroup | [UNKNOWN] | HTML/PDF/CSV | FE: showBloodBankReqBalanceSheetReport.json |
| `getCrossMatchRegisterReport` | DonorbloodGroup, RecbloodGroup | [UNKNOWN] | HTML/PDF/CSV | FE: showCrossMatchRegisterReport.json |
| `getSupplyComponentTypeWiseReport` | bloodGroup, componentId, issueType | [UNKNOWN] | HTML/PDF/CSV | FE: showSupplyComponentTypeWiseReport.json |
| `getHemoglobinopathyTransfusionReport` | bloodGroup | [UNKNOWN] | HTML/PDF/CSV | FE: showHemoglobinopathyTransfusionReport.json |
| `getBloodBagBalanceSheetReport` | bloodGroup | T_BLOODBAGEXTRACTION, T_BLOODBAGCOLLECTION, T_BLOODDONORREGISTRATION, T_DONORMEDICALEXAMINATIONRESULT | HTML/PDF/CSV | Verified SQL |
| `getDonorSerologyTestReport` | bloodGroup, gender | T_BLOODDONORREGISTRATION(DETAILS), T_BLOODBAGCOLLECTION, t_pathOrderBooking, t_encounter, t_registration, m_pathoTestMaster, M_SERVICEMASTER, M_PATHOPARAMETERMASTER, T_pathpatientreport, T_PathPatientParameterDetails | HTML/PDF/CSV | Verified SQL |
| `getBloodComponentMasterRecord` | bloodGroup, componentId | T_BLOODDONORREGISTRATION(DETAILS), T_BLOODBAGCOLLECTION, T_BLOODBAGISSUE, t_pathOrderBooking, t_pathOrderBookingDetails, t_encounter, t_registration, m_pathoTestMaster, M_SERVICEMASTER, M_PATHOTESTPARAMETER, M_PATHOPARAMETERMASTER, T_pathpatientreport, T_PathPatientParameterDetails, T_PATIENTREQUISITION(DETAILS) | HTML/PDF/CSV | Verified SQL |
| `getReceivedBackBloodReport` | bloodGroup, componentId | T_BLOODBAGISSUE, T_PATIENTREQUISITION(DETAILS), T_BloodBagExtraction, M_BLOODCOMPONENTMASTER, m_blooddiscardreasonmaster | HTML/PDF/CSV | Verified SQL |
| `getListOfRecieptforCampReport` | externalOrInternal | T_BLOODDONORREGISTRATION, T_BLOODBAGCOLLECTION, T_BloodBagExtraction, T_BLOODBAGISSUE, T_PATIENTREQUISITION(DETAILS), m_blooddonationcampmaster | HTML/PDF/CSV | Verified SQL |
| `getBloodBankBalanceSheetReport` | bloodGroup, componentId | T_BLOODBAGEXTRACTION, T_BLOODBAGCOLLECTION, T_BLOODDONORREGISTRATION, T_DONORMEDICALEXAMINATIONRESULT, M_BLOODGROUPMASTER, M_BLOODCOMPONENTMASTER | HTML/PDF/CSV | Verified SQL |

> Note: dispatcher keys are distinct from the 16 blood-bank report routes listed in 03-legacy-frontend-inventory §2.7 (`reports/*` endpoints in blood-bank-module.js) — the module routes drive the schemas above.

### 3.6 RMRS / accounting (2)

| Report | Parameters | Key tables | Output | Notes |
|---|---|---|---|---|
| `getRMRSSingleAccountReport` | fromAcc, unitId, fromDate, toDate | T_COLLECTIONDETAILS, T_COLLECTIONHEADER, M_ACCOUNTHEADMASTER, M_PAYMENTMODETYPE, M_RMRSREASONMASTER, T_REGISTRATION, M_STAFFMASTER, M_AUTHORITYMASTER | HTML/PDF/CSV | otemplate `bankStatement.html`; "Bank Statement" |
| `getRMRSAccountToAccountReport` | fromAcc, unitId, fromDate, toDate | same (ch.ACCTOACCTRANSCTIONKEY filter) | HTML/PDF/CSV | otemplate `bankStatement.html` |

### 3.7 Dispatcher output templates on disk (`integration/reports/`)

44 HTML otemplates present: donationBloodGroupWiseReport.html, servicewiseCollectionReport.html, showAdmissionReport.html, showAgeWiseRegistrationReport.html, showAmbulanceRegisterReport.html, showAppointmentReport.html, showBioWasteReport.html, showBloodBagBalanceSheetReport.html, showBloodBagIssueReport.html, showBloodBagTypeDetailsReport.html, showBloodBankBalanceSheetReport.html, showBloodBankReqBalanceSheetReport.html, showBloodComponentMasterRecord.html, showBloodDonorRegistrationReport.html, showBloodGroupWiseStaticReport.html, showCampBagIssueDetailsReport.html, showCampDonorDetailsReport.html, showCampDonorStatisticReport.html, showCancelledAdmissionReport.html, showCategoryWisePatientReport.html, showCrossMatchRegisterReport.html, showCssdReport.html, showDailyBloodstockBookReport.html, showDailyDischargeReport.html, showDepartmentwiseOpdPatientStatistics.html, showDonationStatusReport.html, showDonorPhysicalStatusReport.html, showDonorSerologyTestReport.html, showEquipmentUsageReport.html, showFuelConsumptionReport.html, showHemoglobinopathyTransfusionReport.html, showIpdStatisticalReport.html, showLabCenterTestWiseReport.html, showLabReport.html, showLinenLaundryReport.html, showListOfRecieptforCampReport.html, showPatientDeathReport.html, showPatientFeedBackSummary.html, showReceivedBackBloodReport.html, showSupplyComponentTypeWiseReport.html, showVehicleTripReport.html, userwiseCollectionReport.html, userwiseSummaryReport.html, bankStatement.html.

---

## 4. Explicit JAX-RS report/print endpoints (catalog evidence, 49 `Report` + 72 `print` URL rows)

These bypass the dispatcher; they return print-ready DTOs or String HTML/PDF. High-value families:

| Family | Examples (Controller) | Output evidence |
|---|---|---|
| **Receipts / bills** | `billReceipt/downloadFile`, `billReceipt/downloadFileOpen` (byte[]), `billReceipt/printCreditNoteForIP\|OP`, `advancebillReceipt/printAdvanceReceipt`, `allReceipts/*` | 04-api-catalog rows 435, 502-515 |
| **Appointment/admission sheets** | `patientRegAppt/pdfAppointmentFaceSheet`(Html), `pdfAppointmentFaceSheetFromAppList`, `pdfAppointmentLetterFromAppList`, `pdfOpdCard` (String[]), `getHealthCardSheet`, `printTokenSheet`, `patientRegAddmission/pdfAdmFaceSheet` | custom.* services |
| **Pharmacy prints** | `pharmacy/printPatientDispenseDetails`, `printPatientMedNonMedDetails`, `printPatientRxDetails`, `printCompoundLabel`, `printBagLabel`, `printPharmacyMedLabel`, `printHighBillingPatients`, `printOutOfStockMedDetails`, `printMedLabel`(frontend), `medication/print*` | String / view DTOs |
| **Lab/Radiology reports** | `pathoOrder/printAllReport`, `statusScreenLabReportPrint`, `radiology/getPatientRadReport`, `reports/*` radiology routes | String / GetPathoResultApi |
| **Nursing/clinical** | `nursingList/printPatientNote`, `cdrFormPrint`, `departmentDischargePrint`, `saveAndPrintPMRReport`, `downloadPMRReportAndDeathForm`, `barthelIndexReport`, `labTestReport`, `getPatientPrescriptionDetails`, `printRabiesReport`, `printSimplifiedQuickOrder` | NursingReportView / Map / PatientDocuments |
| **CSSD / OT / Transport** | `cssd/printKitRequisitionDetails`, `otscheduler/printOTReport`, `printScheduleList`, `transport/printTransportReqReceipt` | String[] / String |
| **Inventory** | `expiryitem/printCreditNoteDetails`, inventory print DTOs (poDraft etc. via JasperDataView — commented) | views |
| **RMRS** | `RMRS/printSummaryAccountReport`, `printReportByDocumentNumber`, `printPlanAndAccountWiseReport`, `report_printBankStatement` | String |
| **Janta clinic / BI** | `master/getJantaClinicReportData`, `master/syncJantaClinicReportData`, `master/getClinicReportData`, `master/getCoronoStats`; `helper/getAllDataForReport`; BI reports `biReport:reports(/:rid)` iframe (external BI tool, token refresh) | report-module.js:1026,142; 03-inventory §2.12 |
| **AASMAAN** | `aasmaan /reports/{report_id}` + `/export` (area/institution/frequency filtered) | AasmaanReportServiceImpl |
| **File serving** | `fileService/downloadFile`, `patient/{filename}` servePdf, `ejs-details/downloadMedia` | streams |

---

## 5. Frontend print template inventory (`src/main/resources/public/modules/*/templates/`)

| Template family | Location | Purpose | Output path |
|---|---|---|---|
| **dischargeSummary** | `ip/templates/dischargeSummary/` — 14 specialty pairs `*_dis_form.html`/`*_dis_ticket.html` (cardiology, endocrinology, ent, eye, medicine, neurology, neurosurgery, oncology, ortho, rrc, surgery_plasticsur, urology) + `Common_Dis_Form/Ticket`, `diagnosisDischargeSummary.html`, `dischargeSummaryLabResult.html` | Specialty discharge summaries (IP) | window.print() client-side |
| **plainText (dot-matrix)** | `billing/…/plainText/` (adv_receipt, billCancellationRefundReceipt, billCancelReceiptIP/OP, bill_receipt, creditNoteIP, creditNoteOP, ipdBillReceipt, opBillPaymentReceiptPlainText, packageRefundReceipt); `ip/plainText/` (attendantPassPlaintextLabel, ipdLabelPlainText); `nursing/plainText/` (dischargeSummary, patientPrescriptionPlainText); `op/plainText/` (appointment_confirm, newPrescriptionDmp, prescription_format); `pharmacy/plainText/` (itemSaleListReceipt, itemSale_receipt); `pathology/plainText/` (pathoMicroReport, pathoParameterReport, pathoTemplateReport, showAllLabReport); `bloodBank/plainText/` (bloodBagIssueLabelPlainText, DonorSlipPlainText); `inventory/plainText/` (IndentInvoiceReciept) | Receipts/labels for **dot-matrix printers** (`isADotMatrixPrinter` gvar) | window.print() |
| **zebraLabels** | `commons/templates/zebraLabels/`, `pathology/templates/zebraLabels/` | ZPL labels (wristbands, bag labels) | QZ Tray (`qz.print(config, zplCMDs)`) |
| **printReports** | `commons/templates/printReports/risl_header.html` | Shared print header (RISL logo) | print wrapper |
| **Prescription** | `nursing/templates/` patientPrescription.html, prescriptionPreview.html, prescriptionScreen.html, printMdRxIP.html, patientPrescription-nhnc.html; `pharmacy/templates/` printMedLabel.html, printMedRx.html, printHISIdBarCode.html; `commons/` addDoctorPrescription.html | Rx print (OP/IP, NHNC) | window.print() / `prescription.jrxml` Jasper (backend) |
| **Receipts/Bills/Credit notes** | `billing/templates/` billReceipt.html, iPDBillReceipt.html, iPDPaymentReceipt.html, opBillPaymentReceipt.html, combineBillReceipt.html, billCumAdvanceRefundReceipt.html, billRefundReceipt.html, refundBillReceipt.html, advancebillReceipt.html, advance_refund_receipt.html, pharmacyBillRecipt.html, pharmacyBillRecpForCounterSale.html, pharmacySaleReturnRecipt.html, bill_receipt.html, bill_summary.html, billDraftSummary.html, ipBillSummeryDetail.html, billSummeryDetail.html, partialPayment.html, allReceipts.html, credit-note-charge.html | Bills/receipts/credit notes (HTML print) | window.print() |
| **Lab reports** | `pathology/templates/` (plainText + showAllLabReport etc.); `billing/templates/addLabServicetempRowNhnc.html`; pathology schemas | Lab report print incl. **BrowserPrint** (Zebra SDK) in patho-modules.js:8587 (`printBarCodePathoZebra`) | window.print() / BrowserPrint / QZ |
| **Radiology reports** | `radiology/templates/` radioFinalizeTemplate.html, showRadiologyReport.html | Rad report print | window.print() |
| **MRD forms** | `pmrd/templates/` formj1.html, formj2.html, formj3.html, hform.html, patientEncounterRecord.html, patientMedicalRecordWrap.html, uploadPatientDocumentsPmrd.html; `nursing` PMR endpoints (getPMRReport, saveAndPrintPMRReport, cancelPMRReport) | Medical record / PMR / death forms | print/PDF (DownloadFileUtil pdf paths) |
| **TIU-XML discharge** | `tiuxmls/templates/` IUD_DISCHARGEV10.html, IUD_DISCHARGE_INSTRUCTIONSV11.html, VASECTOMY_DISCHARGE_V11.html, Discharge_checklist_govt_institution.html, Discharge_checklist_home.html, Trail_discharge.html | Govt. scheme discharge instructions (FPV/ECTS) | window.print() |
| **Misc labels** | `op/` opLabelPrint.html, qRCodePrint.html; `ip/` ipLabelPrint.html, dischargeReqNoBarcode.html; `pharmacy/` token_sheet.html; `bloodBank/` bloodBagRecvLabelPrint.html; `pathology/` printSingleBarCode.html; `queue` token print (`queueManagment/printToken` → Map) | OPD/IP labels, tokens, barcodes, QR | QZ / window.print |

**Frontend print plumbing**: qz-tray.js (websocket to `print.server.url = ws://localhost:8182/`); printerSetting-ext.js + `commons/templates/printerSettings.html` (printer selection UI); `auth-init-module.js:19-20` auto-starts QZ connection; localStorage prefs `DefaultSystemPrinter`/`BarcodePrinter`/`NoOfCopies`/`isADotMatrixPrinter` (03-inventory §1.4).

---

## 6. Scheduled / background report-producing jobs (no dispatcher report is scheduled)

| Job class | # @Scheduled | Representative work |
|---|---|---|
| `ApplicationJobs.java` | ~40 | auto charges, daily encounter sync, blood order cancellation, donor cancellation, interim admission abscond, EJS ASHA incentive, blood bag de-crossmatch, RGHS/SIMS sync, EHR sync, e-RaktKosh donation, all-count summary, WhatsApp retrigger, unitwise auto-indent, e-Aushadi indent, operator EHR sync, monthly (0 0 0 L), daily 18:00 |
| `InventoryJobSchedule.java` | ~6 | daily demand update, nurse-patient allocation, e-Aushadi item stock |
| `EHRSyncJobs.java` | 0 active | (commented out) |
| `Hl7InboundMessageJob.java` | 0 active | (commented out) |
| `AutoNextDayAllocation.java` | 0 active | (commented out) |

---

## 7. Risks & unknowns

1. **[HIGH] Deployed report assets absent from repo**: `reports/*.json`+HTML (repo copy present at `integration/`, but runtime path is `D:/RISL/ihms/integration`), Velocity `.vm` templates (incl. `generic_reports.vm`), and all `/jrxml/*` files are external — a clean-environment build cannot render reports/PDFs. Must be versioned in target platform.
2. **[HIGH] 3 dispatcher report keys point to Velocity templates not present in repo** (`userwiseCollectionReport.vm`, `servicewiseCollectionReport.vm` — reports.json; plus default `generic_reports.vm`).
3. **[MED] Jasper surface is legacy/dormant**: `jasperreports` + `jasper-jdt` deps and font registry remain, but only prescription/opdCard jrxml are live; ~40 references commented out — decide keep vs drop.
4. **[MED] Secret leakage**: `getUserDetailsList` outputs `ud.password`; `getUnitDetailsList` outputs `apiKey`. Confirm redaction in target.
5. **[MED] Duplicate legacy report keys** (`...ReportOld` ×3) still wired to FE schemas.
6. **[LOW] `reports.js` is empty** (`Report = {}`) — itemplate/otemplate JS-function mode never fires today; remove or populate deliberately.
7. **[MED] Printer dependency**: QZ Tray + Zebra BrowserPrint + dot-matrix flows are client-side; modern browser restrictions (local websocket / plugin) must be re-architected (e.g. server-side spooling).
8. **[LOW] is_print threshold** `outlst.size() > 1000` silently forces PDF instead of HTML — behavior must be preserved or documented in target.

---

*Sources: IntegrationOperationsFilter.java, ReportServiceImpl.java, DownloadFileUtil.java, PdfGenerator.java, BillInvoiceGenerator.java, FileStoreUtil.java, application.properties, pom.xml, D:\ihms\integration\reports\* (reports.json 50 keys, reports.js, 44 HTML templates), D:\ihms\integration\mappings.json, public/modules/*/templates, qz-tray.js, printerSetting-ext.js, patho-modules.js:8587, report-module.js, 04-api-catalog.md, agent-03/04 findings, reconciliation-notes.md.*
