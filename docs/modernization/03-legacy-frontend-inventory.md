# 03 — Legacy Frontend Inventory (FAUG/jQuery HMIS)

> **Agent 03 output.** Reverse-engineered from `D:\ihms\src\main\resources\public` (research-only, no modifications). Evidence dump and full API map: `_discovery/agent-03/` (`findings.md`, `api-map-per-file.md`, `api-map-js-with-functions.txt`, `api-map-raw-js-json.txt`, `api-endpoints-normalized.txt`, `route-inventory.md`).

---

## 1. FAUG Architecture (what a React team must replicate)

### 1.1 What FAUG is
FAUG ("Form And User-interface Generator") is a proprietary JS engine (~360 KB minified) that renders the entire HMIS UI from **schema JSON** + **HTML templates** + **jQuery**, driven by **hash routing**. There is no server-rendered HTML for screens — every screen is produced client-side.

```
index.html (shell, body m_tenant_id="1")
  └─ faug-min.js (engine) + faug-ext.js (template compiler) + faug-ext-globalize.js (i18n/date)
  └─ js/*.js (71 controllers — business logic, direct API calls, screen state machines)
  └─ fjs-config.json (router: 533 routes, schema registry, settings)
  └─ fjs-config_<module>.json ×40 (lazy per-module schema/template registry)
  └─ modules/*/schemas/*.json (1,308 declarative forms)
  └─ modules/*/templates/*.html (2,942 runtime templates)
  └─ layer/default.json (default form/table/modal chrome)
  └─ w-api-call.js (Web Worker XHR bridge, 3 workers, response cache)
```

### 1.2 Runtime flow
1. `index.html` loads vendors + engine + all controllers (globally concatenated function namespace — **screens are plain global functions**).
2. `__loadfaugconfig` fetches `fjs-config.json` (token as query param or header), then `router.firstroute` is rendered.
3. User clicks a menu → `location.hash = "#module:screen/param1/param2"`.
4. `hashchange` handler: if `fjs-config_<module>.json` not yet loaded, fetch it, register its schemas/templates, then run the route handler (`func` or `crudop`/`draw`/`load`).
5. Route handler calls a controller function (e.g. `startBillingFlow`) or executes a schema CRUD op: fetch data → render viewer/table via template → attach declared jQuery control events.

### 1.3 The three layers of a screen
- **Schema** (`*.json`): fields, types, labels, required, validations, enums, option sources, event chains (default/change), CRUD ops with URLs and callbacks, `dynamicAspects` conditional overrides.
- **Template** (`*.html`): presentation. Compiled with `<% %>`, `#js`, `##js##`, `!!include args!!` syntax; receives `rows` (list data), `arg` object; can call `Faug.*`/`Fg.*` and any global JS function.
- **Controller** (`js/*.js`): screen flow, non-declarable logic (wizards, DTO assembly, client math, printing, file ops), direct API calls via `Faug.ajaxGet/Post/Put/Delete`, `Faug.get/post/api`, `$.get`, `$.getJSON`.

### 1.4 State
- **gvars** (`Faug.addGlobalVar`/`Fg.g`): session globals seeded by the login response (`unit_id`, `login_user_role`, `tkn`, `hospitalName`, `userstoreIds`, dozens of `isXxx` feature flags). Schemas reference them as `%%Fg.g(name)%%` in strings, `gvar@name` in conditions, `fromVar` for option lists.
- **vars** (`Faug.addVar`/`getVar`): flow-scoped (route params, page state).
- **localStorage**: locale (`ls_locale_language`), printer prefs (`DefaultSystemPrinter`, `BarcodePrinter`, `NoOfCopies`, `isADotMatrixPrinter`), UI prefs (`box`, `fixed`, `CSS`).
- **Server caches** (master lists) live in gvars: `gender_master_full_new`, `prefix_master_full`, `religion_masters`, `Plan_master_full`, `config_app`…

### 1.5 Condition grammar (used everywhere)
`|<operator>|<expected>:<source>@<name>` — sources: `gvar@`, `object@`, `this@value`, `form@field`, `route@param`, `class@`/`id@`/`data@` (DOM), `func:name` (validator), `regex|<regex>:this@value`, `len|>=|n:this@value`, `|!tb|` not-blank, `|!u|` not-undefined. Used by `dynamicAspects` conds, `validations.cond`, controlEvent conds, and table-chrome conds.

### 1.6 API call taxonomy
1. **Schema-declared** (majority): `crud.*.url`, `properties.*.events.*.url` — FAUG executes XHR, chains `done` events, fills dropdowns, assigns values.
2. **Controller calls**: `Faug.ajaxPost(true, url, contentType, content, headers, successCb, errorCb, idempotency)` — first arg freezes UI; promise style `Faug.get(url).done()`.
3. **Web Worker bridge**: engine dispatches to `w-api-call.js` (XMLHttpRequest + postMessage), 3 workers, optional `cachedTime` (ms) response cache keyed by URL.
4. **Static config fetches**: `$.getJSON("vitalsConfiguration.json")`, `mastersConfigration.json`, `charts/ODCWF_*.json`, `tiuxml_mapping.json`, `v_check?param=`.

### 1.7 Authentication & roles
- `POST api/rest/auth/loginf` (form-urlencoded) → seeds gvars; `POST api/rest/auth/getuserrole` → role list; `PUT api/rest/auth/logout`; `POST api/rest/auth/changeUserRole?userId&unitId&roleId&setAsDefaultRole`.
- SSO: `api/rest/usermanagement/loginsso`; OTP variants `loginotp`, `getuserroleOtp`; MDM token bridge `getMDMTokenWithSSOToken`.
- Role-based UI: `login_user_role` gvar + `roleBasedNav.html` menus + per-role visible routes; store-scoped data via `userstoreIds`/`crr_Default_Store_id`.

---

## 2. Screen Inventory (533 routes, 40 modules)

Convention: route `#module:screen/:params`. Full list: `route-inventory.md`. Highlighted screens with 12-point docs below; every screen = (route, controller function, schema, template, endpoints) — full per-file endpoint mapping in `api-map-per-file.md`.

### 2.1 OP — Outpatient / Appointments (33 routes)
| Screen | Route / entry | Controller fn | Schema / Template | Key endpoints |
|---|---|---|---|---|
| Walk-in visit | `op:appointmentFlow` | `startAppointmentFlow` | walkinVisit-schema.json / generic-radio-list.html | POST `api/rest/walkinVisit/addwalkinVisit?pat_token_no&app_type`; GET `searchToken`, `appointmentschedule/getDepartmentsByDate`, `getvisitTypes`; POST `patient/getPatientGeneralDetailsListForSearch` |
| New appointment | `op:addAppointment/:doctor_id/:doctor_name/:department_id/:app_date/:from/:totime` | appointment-module.js | addpatientRegApptdto_schema.json / doctor-appintment-slots-schema.json | `appointmentschedule/getAppointmentScheduleNew`, `getCabinWiseApointmentSlotCount` |
| Reschedule | `op:rescheduleApppointment/:.../:appointment_id/:patient_id/:totime` | | updateAppointment_schema.json | `appointment/...` |
| Emergency | `op:addEmergency/:doctor_id/:doctor_name/:department_id` | | minimal-walkin-appointment.json | `markandaddvisit/checkin` (checkin flow) |
| Appointment list | `op:appointmentList` / `appointmentListOnline` / `appointmentListdoc` / `appointmentListNhnc` | `showAppointmentListDetails` | appointment_list-schema.json | `patientRegAppt/sendOTP`, `validateOTP`, `pdfAppointmentFaceSheet*` |
| Doctor schedules | `op:doctorSchedules/:doctor_id/:department_id` | `showActiveDoctor` | doctor-appintment-slots-schema.json | crudop `DoctorAppointmentSlots/getSlots` |
| Slot management | `op:manageDoctorSlot`, `op:manageDepartmentSlot`, `op:showDepartmentSlot` | | showDepartmentWiseSlots.json, department-schema.json | `appointmentschedule/addOrRemoveSlots` |
| Patient details/history | `op:patientDetails/:allowEdit`, `op:patientHistory/:mrno`, `op:editPatientDetail/:mrnNo/:unitId` | | patientRecordSchema.json, patientHistorySchema.json | `patient/getPatient`, `patient/getEMRAdmVisitByPatientId` |
| Confirmation screens | `op:appointment_confirmed`, `op:walkin_confirmed`, `op:emergency_confirmed`, `op:opFaceSheet` | | | `patientRegAppt/pdfAppointmentFaceSheet` |
| Upload appointments | `op:uploadAppointments` | | | file upload + parse |
| Kiosk | `kiosk:selfRegPatient`, `kiosk:selfAppointment/...`, `kiosk:searchAppointment/...`, `kiosk:accountSearchSchema` | kiosk controller | minimal-walkin-appointment.json | `markandaddvisit/checkin` |

**12-point doc — Walk-in Visit (flagship screen):**
1. **ID/Route:** `op:appointmentFlow`; opened from role menu (Appointments icon), hash nav; `op:appointmentNewForm` variant for checkin (`crud.c.url = api/rest/markandaddvisit/checkin`).
2. **Module/JS:** `js/appointment-module.js` (also `appointment-list-module.js`, `common-module.js`).
3. **Schema/Template:** `modules/op/schemas/walkinVisit-schema.json` (6,621 lines); templates `generic-radio-list.html`, `generic-custom-radio-list.html`, `patient-search-list.html`, `religion-master-list.html`, custom `customTemplate` per field.
4. **Fields (selection):** visitTypeId*, switchh, registrationToken, janaadharIdNo, familyIdNo, civilId (aadhaar), abhaSearch, mRNO, abhaRegistration (button), firstName*, fatherName/motherName/husbandName, genderid*, prefixid, age*, dateOfBirth*, religionId, mobileNo, alternateMobileNo, identityTypeId/Number, photo/cameraPhoto, isForeigner/passportNo/visaNo, patientSponsor (planId), healthId/healthIdNumber, getPolicies (eligibility button), resAddress/resCountryId/resStateId/resDistrictId/resCityId/resPinCode, departmentId/buildingId/clinicId, appointmentDate/from (slot picker), isCase/caseId, vehicleDetails, contactPerson, isLegal, caseTypeId, mlcDetailsDto (broughtBy*, broughtByName, broughtByRelationId, broughtByMobileNo, isBroughtDead, formalityRemark), referralDetails (NHNC), fpvDetailsDto/ancVisitDetails/anmAndAshaDetails (FPV/PCTS), telemedicine block. `*` = required per dynamicAspects branch.
5. **Validation:** schema regex (`^[A-Za-z][A-Za-z .]*$`), `func:validateJanaadharId`, `validateBhamashahId`, `validateAdharNumber`, `validateHIDNO`, `validateAge`, `validateMobileNoByAPI` (server-side mobile check on blur), `validateCountryCode`, pre-submit `onValidateOp: validatePatientDetails` (validation.js:1).
6. **Conditional fields:** 9 `dynamicAspects` branches keyed on gvars `isExtLMISEnabled`, `app_type_w` (`s`=self/kiosk, `k`=kiosk), `isNhncEmer`, `appointmentAgainstUnitHead`, `isTelemedicine`, `isFPV`, `isPctsRegistration`, `isCheckinFlow`, `isInsuranceCategoryMap` — each mutates required/show/order/submit label/URL.
7. **Dropdown dependencies:** gender←`gender_master_full_new`; prefix←`prefix_master_full`; religion←`religion_masters`; occupation←`occupation_masters`; planId←`Plan_master_full`; state/district/city cascade via events AJAX; department←date+unit+age+gender (`getDepartmentsByDate`); building/clinic cascade; telemedicine department/unit cascade (`getDeptsForTsu`, `getTelemedicineSpecialityUnit`).
8. **Buttons/actions:** CONFIRM (submit), mobile "search patient" (typeahead dropdown), ABHA Registration, Check Eligibility, MRNO selection icon, queue token icon.
9. **Callbacks:** `beforeOp: setWalkinObject` (assembles AddPatientRegistrationDto), `afterOp.func: setViewOpdChargesRoute` → `#billing:viewOpdCharges`, `failure: showErrorMsgForAppt`; `afterDraw: manageHMISLMISFlow` with ~35 jQuery controlEvents (search-as-you-type on MRN/mobile/aadhaar/bhamashah/janaadhar/abha/ECTS/PCTS ≥ length thresholds with `crudop` lookups).
10. **AJAX/API:** see endpoints column; responses render patient dropdown list (`patient-search-list.html`) — click handler populates form fields via `varAssign`/`valueAssign`.
11. **Side-effects:** token queue interactions (`stopAndStartCounterQueue`, `fetchLatestToken`, `fetchAbsentToken`), age/DOB two-way calculation, gender→prefix change, visit-type→department filter, RGHS verification modal, ABHA modal.
12. **UX notes:** Bootstrap button-groups styled as radio cards with icons; mobile-first `col-lg-4/6/12` custom templates; `data-translate` i18n labels; NHNC/Rajasthan schemes deep integration (Jan Aadhaar, Bhamashah, ECTS/PCTS, ABHA).

### 2.2 Billing (34 routes)
| Screen | Route | Controller fn | Schema / Template | Key endpoints |
|---|---|---|---|---|
| Billing flow (OP/IP) | `billing:billingFlow`, `billing:ipBillingFlow`, `billing:opIpBillingFlow`, `billing:patientBilling` | `startBillingFlow` (billing-module.js:707), `startIpBillingFlow` | addBillingSchema.json, addIpBillingSchema.json, addOpIpBillingSchema.json / service-search-list.html, itemDetailRow.html | POST `servicemaster/getServiceListForBill`, `patientDetailReg/getPatientGeneralDetailsSearch`, `serviceOrder/addServiceOrder`, `addServicechargesNew`, `serviceOrder/getAllService`, `master/getDoctorFrequentServices`, `ipdadmission/getSponsorByAdmissionID` |
| View OPD charges | `billing:viewOpdCharges`, `billing:viewOpdChargesonInvestigation` | `showOpdCharges` | opBillPaymentSchema.json | `serviceOrder/allServiceFastByGet?opd_id=` |
| Bill list | `billing:billingListFlow` | `startBillingListFlow` | getAllBills-schema.json | `bill/getAllBills*`, `combineBillingListFlow` |
| Checkout / finalize | `billing:checkout`, `billing:finalizeBill`, `billing:interimBill`, `billing:checkoutForHealthConsumption` | `finalizeBill` | draftSummaryBillSchema.json, cancelOpBillSummarySchema.json, cancelIpBillSummarySchema.json | `bill/finalize*`, `billSummary/*` |
| Receipts | `billing:OPDbillReceipt`, `billing:IPDbillReceipt`, `billing:advanceReceipt`, `billing:receiptList`, `billing:billSummary`, `billing:dischargeList` | `showBillReceipt`, `showIPBillRec` | billReceiptSchema.json, iPDBillReceiptSchema.json, advanceReceiptByReceiptNoSchema.json, billSummarySchema.json / billSummeryDetail.html, ipBillSummeryDetail.html | `billReceipt/*` (downloadFile), `advancebillReceipt/printAdvanceReceipt`, `bill/checkToPrintNewPass`, `submitPass` |
| Payments/advance/refund | `billing:addPayment` | `addPayment`, `addAdvance`, `showRefund` | paymentDetailSchema.json, addAdvanceSchema.json, advanceSchema.json, refundDetailSchema.json | `api/rest/addPayment`, `api/rest/addAdvance`, `cancelBill/refundReceiptByIds`, `advancePaymentDetailList` |
| Credit notes | `billing:creditNoteBillingListFlow` | | getAllCreditNoteListSchema.json, creditNoteSchema.json | `billReceipt/printCreditNoteForOP|IP` |
| Doctor share | `billing:doctorShare`, `doctorShareAddDeduct`, `doctorShareBill`, `doctorBillList` | | doctorShareSchema.json, doctorShareAddDeductSchema.json, doctorShareBillSchema.json | `addUserService/getUserByConcessionCategory` |
| Package billing | `billing:packageSubscriptionFlow`, `billing:packageAppointmentList`, `billing:healthPlanPackage(Nhnc)` | packageBilling.js | addPackageSubscriptionSchema.json, packageListForBillSchema.json, healthPlanPackageSchema.json | `serviceOrder/allPackagesFastByGet`, `packageservice/*` |
| Estimation | `billing:billEstimationFlow`, `billing:addPatientBillEstimation` | billEstimationFunctionality.js | getAllEstimationBillSchema.json, addOTBillEstimationSchema.json | `serviceOrder/*` |
| OT billing | `billing:otBillFlow` | | addOTBillEstimationSchema.json | `otscheduler/addPatientProcdure` |
| Cross-service order | `billing:crossOrderService` | | crossServiceOrderSchema.json | `serviceOrder/*` |
| Sponsors | `billing:showSponsor`, `billing:showSponsorDetails` | | changeSponsorSchema.json, sponsorSchema.json | `ipdadmission/getSponsorByAdmissionID`, `master/getAllPLanMasterByCompanyId` |

**12-point doc — OP Billing:** route `billing:billingFlow`; JS billing-module.js `startBillingFlow`; schema addBillingSchema.json; patient search typeahead (name/MRN/mobile → `patientDetailReg/getPatientGeneralDetailsSearch`), service search typeahead (→ `servicemaster/getServiceListForBill`), selected services table `#servicesTbl` (rate×qty, tax1-3, concession, self/non-self, package coverage), pharmacy order section `#pharmacyOrderTbl`, corporate approval amount, max-concession validation, totals recalculated client-side by `calculateTotalBillAmount` (billing-module.js:2050); CONFIRM → POST `serviceOrder/addServiceOrder` → `generateBillSucess` → receipt/print; payment modes modal (cash/card/cheque/online/company) via `showPaymentModes`; partial payments; advance adjust toggle `setDefaultAdvanceAdjCheck`; health-plan consumption (NHNC) variant `checkoutForHealthConsumption`; packages (RGHS) variant with `addPackages.html`.

### 2.3 IP — Inpatient / Admission (23 routes)
admission-module.js (132 API refs). Routes: `ip:admissionOrder`, `ip:admissionOrderEmer`, `ip:admissionOrderEmerV/:v`, `ip:admissionList`, `ip:admissionReqList`, `ip:transferReqList`, `ip:admissionConfirm`, `ip:attendantPass`, `ip:bedManagement/:class_id/:ward_id/:bed_id/:building_id/:filterId`, `ip:bedManagementNew/:class_id/:ward_id/:bed_id`, `ip:bedScreen/...`, `ip:otBedManagement`, `ip:bedReservation`, `ip:unReservedBedList`, `ip:motherChildLinking`, `ip:clearanceListFlow`, `ip:MlcclearanceListFlow`, `ip:nursingStation`, `ip:dischargedPatients`, `ip:showAllCradles`, `ip:summaryOpdIpd`, `ip:updateDischargeStatus`, `ip:showInterimAdmissions`, `ip:showTidList`, `ip:doctorCheckin`, `ip:cdrForm4A/4B` (death reports).
- Schemas: addpatientAdmissiontdto_schema.json (+_emergency, addBabyAdmissiondto_schema.json), interimAdmissions.json, emergency-schema.json, bed_management_schema.json, bed_screen_schema.json, bedReservationdto_schema.json, showDoctorIpSchedule.json; templates admission-icons.html, emr-admission-icon.html, bed-managment-building.html, bed-managment-ward.html, pendenceyDashboardTemplate.html, dischargeSummary/*_dis_form.html (Common_Dis_Form, diagnosisDischargeSummary, surgery/medicine variants, dischargeSummaryLabResult), qrcodeScanner.html.
- Endpoints: `patientRegAddmission/*` (22), `patientDetailReg/*`, `ipdadmission/*` (getSponsorByAdmissionID, getActiveAdmissionOfRegisteredPatient, addAdmMLCDetails), `bedmanagement/*` (34), `patientTransferDischarge/*` (29: transfer requests, clearance), `serviceOrder/allServiceFastByGet`, `patient/getPatient`, `patient/getEMRAdmVisitByPatientId`, `walkinVisit/printBHTBarcode`, `printAttendantPass`, `otscheduler/*`.
- Bed management: building→ward→class→bed drill-down with occupancy colors; routes carry `/class_id/ward_id/bed_id/building_id/filterId`; bed assignment/transfer/reservation; OT bed view.

### 2.4 Nursing (22 routes + charting/worklist)
- Routes: `nursing:roleNurse`, `roleLab`, `roleRadiology`, `roleFrontOfficer`, `roleNursePatientView`, `roleNurseIncharge`, `roleAlliedPatientView`, `doctorPatientView`, `rolePatientCoversheet/:patientId/:admId/:patientIen/:visitIen`, `ipCoversheet/.../:opdipd`, `rolePatientNote`, `rolePlanofCare`, `roleNurseRostering`, `MyOpPatients`, `showMortuary`, `showTransfer`, `showDischarge`, `patientNotes/:patientId/:admId/:patientIen/:visitIen/:noteIen/:noteTitleId`, `showCarePlan/...`, `nursingOrder/:tab`, `NursePrescriptionList/:tab`, `compliantDiagnosisMap`, `orderingSet`, `growthchartdetails/add/:id`, `incidentReporting`, `addnewIncident/:...`, `editIncident/:...`, `resolveIncident/:...`, `DiagnosisTreatment`, `viewCoversheet/:patientId/:admId/:patientIen/:visitIen/:ipdOpd`, `nhncOrdering/...`, `nhncOperatorFlow`, `docPrescriptionList`, `annotation`.
- Controllers: nursing-module.js (795 KB), nursing-module-ext.js (674 KB), nursing-charting-module.js, nursing-note-module.js (244 KB), bcma-module.js.
- Schemas: showPatientCoversheetSchema.json, showNurseWorklistSchema.json, showNursePlanOfCareSchema.json, showNursePatientNoteSchema.json, nurseListFor*AllocationSchema.json, incidentReportSchema.json + 40+ tiuxml note schemas.
- Endpoints: `nursingList/*` (~293 unique — the single largest API surface: patient lists, allocation, acuity, shift handover, EDOD, notes, ICDC M search, advice, lab/rad order fetch), `clinical/getProviderList`, `clinical/healthfactorMaster`, `serviceOrder/allServiceFastByGet`, `pharmacy/getMedications`.
- Notes engine: TIU-XML forms via `tiuxml.js` + `tiuxml.json` (219 KB) + `tiuxml_mapping.json`; note tiles from `csNoteTiles.html`, `tilesHF.html`, `noteSummary.html`; charts from `charts/ODCWF_*.json` (29 formats: ANC, coronary care, blood product transfusion, coversheet…); growth charting route `charting:chartingApp`/`chartingExpand`.
- BCMA: `bcma:unitDose`, `bcma:scanWristband/:patientMrn` — medication scanning at bedside (`html5-qrcode.min.js`, wristband barcodes).
- Incident reporting: severity/status param-driven routes with incidentReportSchema/incidentResolveSchema.

### 2.5 Pharmacy (27 routes)
pharmacy-module.js / pharmacy-module-new.js / pharmacy-module-op.js / opPharmacy-new.js.
- Routes: `pharmacy:pharmacyBilling`, `opIpPharmacyBilling`, `ipPharmacyBilling`, `pharmacyCountersale`, `pharmacyItemList`, `PresciptionList`, `prescriptionList`, `NursePrescriptionList/:tab`, `pharmacyOrderVerify`, `pharmacyOrderDispense`, `pharmacyOrderDispenseD`, `cartFillFlow/:ward_id/:patient_dfn`, `pharmacyOp`, `opPharmacyFlow`, `addPrescription`, `medicationOrder`, `itemSaleList`, `itemSaleReturnList`, `ipReturnFlow`/`iPReturnFlow`/`ipReturnPharmFlow`, `ivPharm`, `prepareCompound`, `pharmacyDrugMapping`, `prescriptionUpload`, `nurseOrdrDispensed`, `phCartFillOrderDispense`, `pharmInterventionreport`, `opPatientDetails/:patientId/:opdIpdId/:opdIpd/:pctflow/:patQId/:isPrescriptionOnly`.
- Schemas: opIpPharmacyBillSchema.json (reused cross-module), pharmacyBillRecSchema.json; templates medicationOrderSummary.html, medicine-master-list.html, token_sheet.html.
- Endpoints: `pharmacy/*` (getMedications, getItemCurrentStock, printPatientDispenseDetails, printCompoundLabel, addItemPriceNew, item sale/save/return), `medication/addItemPriceNew`, `master/filterPharmDiagnosis`, `serviceOrder/*`.
- Business logic: prescription→dispense workflow with verify/dispense steps, cart fill per ward/patient, IV admixture, compound preparation, return flows (IP return with print), drug mapping.

### 2.6 Pathology / Radiology (14 + 4 routes)
- Patho: `pathology:lab`, `sampleCollection/:opdipd/:mrnNo/:catg(/:date)`, `sampleAcceptReject/...`, `sampleResult/...`, `sampleCriticalAlert/...`, `sampleResultReview/...`, `sampleReportRelease/...`, `sampleMachineWiseResult(/:...)`, `sampleGenerateBarcode`, `LabN`, `selectedOrderType`, `printAllReport`, `workLoadList`, `groupingAndTest`, `kdlVerify`, `showStatus`, `showLabResult`, `showCovidLabList`, `updateOrderStatus`, `updateServiceStatus`.
- Controllers: patho-modules.js (429 KB), patho-booking-module.js. Endpoints: `pathoOrder/*` (114: sample save/report/release/result download), `pathotestmaster`, `pathoparameterunits`, `pathoprofilemaster`, `abha/*` (COVID/ABHA links).
- Radiology: `radiology:radlab(/:p_id/:order_no/:date/:opd_ipd)`, `printReport`, `reSchedule`, `radReTest`; templates radioFinalizeTemplate.html, showRadiologyReport.html; endpoints `radiology/*` (12) + `reports/*`.
- Both use speciman barcode scanning (qrcodeScanner.html), machine-wise results, KDL (kiosk device lab?) verification, critical alert routing.

### 2.7 Blood Bank (31 routes)
blood-bank-module.js (653 KB). Routes incl. donorRegistration, donorList, campDonation, patientRequisition(+external), crossUnitOrder(s), bloodComponent, bloodBagComponentSeperation/Extraction, bagReturns, crossMatchedBags, redCellSerology, transfusionTransmittedDiseases, crossMatchingList, newCrossMatchingTest, bloodCrossMatching, campRequisition, issueBloodBag, bloodBagDiscard, showIndentList, raiseIndent, bloodBagTrail, retrunBloodbag, eraktkoshRequest, replacementDonorHistory, BloodBanKReOrder, BloodDiscardList, bloodBagOpeningStock, addNewBloodComponent.
- Endpoints: `bloodtestmaster/*` (115 — stock counts, bag issue, districts, requisition details), `patientrequisition/*`, `eraktkosh*`, `bloodcomponent*`, `reports/*` (16 blood-bank report routes).
- Logic: bag lifecycle (donation→component separation→screening→cross-match→issue→discard/trail), district blood-bank stock exchange, e-Raktkosh national network.

### 2.8 Inventory (57 routes) — largest controller inventory-module.js (1.08 MB, 301+ API refs)
PO/GRN/indent/stock/issue/return/expiry/consignment/asset/equipment/e-Aushadi: `inventory:inventory`, `currentPO`, `poRaisedForOtherUnit`, `poByHQ`, `allApproval`, `gRNList`, `indentListofStore`, `reOrderList`, `expiredDamagedItemReturnIndent`, `userStore`, `issueItem`, `consumeItemforStore`, `StoreStockAdjustment`, `holdItemBatches`, `itemReceiveList`, `returnItem`, `returnItemRecieve`, `expiredItemList`, `recallExpiredItem`, `expiryItemReturnList`, `QcStatusUpdate`, `itemSummary`, `grn`, `approveGrn`, `itemMasterList`, `itemGroupRef`, `purchaseIndent`, `consumptionList`, `stockAdjustmentList`, `quotation`, `itemEnquiry`, `opeiningBalance`, `stockLocation`, `updateBatch`, `returnToSupplier`, `expiryItemReturn(ToSupplier)`, `grnReturnList`, `issueItemReturn`, `directIssueItem`, `consignmentGRNList`, `itemStockList`, `orderList`, `receivedpolist`, `assetRegisterList`, `departmentalConsumption(List)`, `purchaseRequest`, `consolidatePurchaseRequest`, `otherUnitPOList`, `multipurposeGRN`, `updateGrn/:grnId`, `raiseDirectPO`, `stockTransfer`, `raiseRMRSPORequest`, `customOrderList`, `customProcessOrderDetails`, `stockList`, `expire`.
- Endpoints: `inventoryStockList/*` (226), `expiryitem/*` (47), `masterConfigService/*`, `eAushadiIndent/*`, `companyapprovedamount/*`, `planmaster/*`, `mdm/*`, `itemmaster/*`, `addUserService/*`; consignment inventory (consignmentInventory.js), procurement.js, quotationDetails.js, expiryDamageItemReturn.js, item-category-module.js, itemEnquiry-module.js.

### 2.9 Masters & Configuration (63 routes) — masters-modules.js (566 KB), masters-ext.js
Generic CRUD pattern: `masters:<%arg1%>/view|add|edit/:id` where arg1 = schema name — a **single parametrized route renders all master screens** from `mastersConfigration.json`. Specific masters: addUser, showUserList, radCabinmaster, pathoCabinmaster, vitalsConfiguration, serviceMasterRef, itemMasterRef, allServiceMasterRef, unitConfiguration, admissionConfiguration, bloodConfigurationMasters, patientConfiguration, doctorShareConfiguration, equipmentConfiguration, billingConfiguration, otConfiguration, inventoryConfiguration, radiologyConfiguration, pathologyConfiguration, cssdConfiguration, bioMedicalWasteConfigration, ambulanceConfigration, medsConfigration, dietConfiguration, pharmacyConfiguration, nursingConfigration, rmrsConfiguration, ejanswathyaConfig, ejs, ruleEngine, companymasterwrapper, servicemasterparent, servicemaster/rateedit, concessiontemplatewrapper, planmasterwrapper, displayUnitmasterwrapper, pathoparametermasterwrapper, cwf (mapping/add), scwf, itemSupplierRate, RateContract, packagehealthplandefinationmaster, departmentmasterwrapper, surgeryheadwrapper, unitmaster/editNew, consentForm, showUnitConfigStatus, allmasters, queryTool, clinicalConfig, compliantDiagnosisMap, serviceDiagnosisMap, formConfiguration/:fileType, tiuxmlMapping, formdesigner, noteTileConfig, updateConfiguration, grievance, machinetestmaster, otschedule, procedurecheklistdetails, immunizationagegroupmaster, radtestmaster, termsofpayment, surgeryheadmaster, pathorganismmaster, pathoprofilemaster, store, servicemaster, diettypemaster, companymaster, proceduremaster, ordersetdefinationmaster, pathoparametermaster, pathotestmaster, growthchartmenu/details, cabinmasterwrapper, departmentmaster, events, ottheatremaster, ottablemaster, authoritymaster, headsmaster, dietitemmaster, accountheadmaster, rmrsreasonmaster, pathotemplatemaster, machineConfig, etc.
- Endpoints: `masterConfigService/*` (67), `addUserService/*` (62), `master/*` (195: filterDiagnosis, filterHealthfactor, filterComplaints, getMastersForAppointment, getVehicleDetails, getMappedServices, filterPharmDiagnosis, filterDiagnosisById), generic CRUD on `api/rest/<master>/*` (doctormaster, admissiontype, anesthesianotes, anesthesiatypemaster, classmaster, unitmaster…).

### 2.10 CSSD / Linen (14 routes + linenlaundry 1)
`cssd:kitFormulation`, `kitPreparation`, `CSSDItemReceiveandReturn`, `CSSDItemIssueNew`, `CSSDItemReceiveReturnNew`, `cssdItemValidate`, `kitMaster`, `otKitItemRequest`, `cssdItemKitSterilization`, `procedureCssdRequest`, `procedureCssdIssue`, `showCssdReportList`, `showLinenLaundryReport`, `hospitalStatistics`, `linenlaundry:showLinenLaundryStatus`. Endpoints: `cssd/*` (45), `linenLaundry/*` (14).

### 2.11 OT (10 routes)
`ot:surgList`, `surgSchedule`, `procPerformance`, `otSchedule/:otId/:tableId`, `otScheduleEvent`, `patMovementAudit`, `nurseScheduleEvent`, `itemDetailsEvent`, `surgSchdule/:otId/:tableId`, `addNewSurg`, `surgDetails`. operation-theatre.js (324 KB). Endpoints: `otscheduler/*` (79: schedules, events, procedures, patient procedure rates), `otconfig/*` (35), `anesthesianotes`, `surgerynotes`, `master/filterDiagnosis`.

### 2.12 Other modules (quick reference)
- **Equipment (20):** complaintDetails, complaintResolutionList, preventiveMaintenance(New), preventiveMaintenanceSchedule, scrapSaleList, equipmentGrn, assetDetails, equipmentIssueItemList, assetVerification, preventiveCalibrationCheckList, calibrationMaintenance, assetPreventiveList, eqpApproveGrn, updateEquipmentGrn/:grnId, eqpPurchaseOrderList, equipmentUsageList, equipMedicalGasIndent, equipMedGasIndentIssue, equipOpeningBal, eqAmcWarrantyDetails. → equipment-modules.js; `equipment/*`, `mdm/*`, `preventivemaintenance/*`, `asset*`.
- **Transport (10):** patientTransportRequest, doctorTransportRequest, tripManagement, vehicleOut/In, fuelStationDetails, vehicleMaintenance, showVehicleTripReport, showFuelConsumptionReport, vehicleStatus.
- **Queue (11):** appointmentQueue, cabinQueue, radiologyQueue, pathologyQueue, doctorBatch, callPatientList, patientQueue, userQueue, doctorQueue, nurseDesk, manageToken + standalone queue/ app (counterQueue, departmentQueue, roomQueue, roleBasedNav templates) with its own index.html/fjs-config.
- **RMRS (11):** showRMRSPORequest, rmrsAccountWiseReport, summaryAccountWiseReport, reportByDocumantNumber, planAccountWiseReport, suspenseAccountDetail, addcollection (generic), showPaymentCollection, issuePayment, showRMRSDashboard. → rmrs-module.js; `RMRS/*` (36).
- **Allocation (11):** rolePatientAllocation, doctorNurseAllocation, nurseWardAllocation, alliedWardAllocation, nurseClinicAllocation, alliedClinicAllocation, nurseDepartmentAllocation, nurseOpList, nurseIpList, nursePatientAllocation, alliedPatientAllocation.
- **Reports (44):** BI-style routes `biReport:reports(/:rid)`, reportdm(/:rid); plus ~40 `reports:showXxxReport` routes (userwise collection, servicewise collection, lab, admission, appointment, blood bank ×17, bio waste, equipment usage, OPD statistics, daily discharge, patient death, ambulance register, category-wise, age-wise, IPD statistics, lab center-wise, cancelled admission, janta clinic). report-module.js; `reports/*` (47), `biReport` iframe with token refresh.
- **Commons (50):** login1/logout/options (auth), user/:userId/:unitId, selectStore, selectDefaultStore, patientList, patientHistoryList, patientRecord, history, notificationTemplates(NotificationTemplate), shift details/roster/ipdRoster, backgroundTasks, productionIssues, coronaDetailsList/addCoronaDetails/editCoronaDetails/coronaPatientReport/coronaStats, appDocumentation, machineConfiguration(List), ipdDashboard, applicationModuleStatus, audits, newAuditsReports, editSchema/:schemaName, addForm, consentRequest, consentRequestData/:concentId, mortuaryDetailsList/BoxManagement/Release/DischargeList, hospitalGeoTagging, abdmQrGenerator, hospitalCreation, outreachusers, pDocumentsManagement, scanPatient, scanCheckin, validateToken, packageDetails, baalAbhaReKyc, deliveryReport, forgotPassword, resetPassword/:token/:unitId/:id, resetLoginPassword/:unitId/:userId, selectDefaultRole, updatePatient.
- **ABDM/ABHA (commons + abdm-module.js, 198 KB):** createAbha flows (aadhaar/demographic), ABHA linking/delinking, HPR (health professional registry), consent request (showConsentData/Form), FHIR bundle viewing, HFR facility search (`abdm-hfr/v1.5/facility/*`).
- **Chemotherapy (4):** chemoPlanningList, chemoPatientCoversheet, chemoPlanning, chemoNote, chemoTherapyNote, patNote, chemoNewRegimen, chemoPlanCycleFlow, chemoPlanningFlow — chemotherapy-module.js (733 KB) + chemo_tiuxml_mapping.json.
- **BCMA (2):** unitDose, scanWristband/:patientMrn.
- **Payment transfer (2), Charting (2), Grievance (2), PatientManagement (1: socialWorker), Tiuxmls (2: designer, tiuxmls/:txml), RCM (1: chiranjeeviapp), PMRD (1: patientMedicalRecord), Shift handover (1: multipleShiftHandOff), Bio-medical waste (1), File management (1: dms), Kiosk (3), Diet (4: dietSchedule, dietMenuItems, dietScheduleStatus, DietKichenView), Enquiry (9: inquiry, patientFeedback, addPatientFeedback, serviceInquiry, wardBedDetails, hospitalEvents, availableDoctors, showPatientFeedBackSummary, buildingFloorMap), NHNC (op:nhnc* routes + nhncModule.js 678 KB).

---

## 3. Client-Side Business Logic Hotspots (must port faithfully)

| Area | Where | What |
|---|---|---|
| Billing math | billing-module.js:2050 `calculateTotalBillAmount` | services+pharmacy totals, 3 tax buckets, concession, self/non-self split, package coverage, corporate approval, max-concession caps, VAT |
| Billing steps | billing-module.js `confirmServices`, `confirmServicesStep2/3`, `finalServiceSummary` | OPD/IPD wizard; per-step POSTs |
| Age/DOB | common-module.js:1380 `getDobFromAge`, :1653 `getAgeFromDob`, :1818 `calculateAgeFromDob`, baby/spouse/child variants | two-way Y/M/D ↔ DOB, age ≤115, NHNC baby edge cases |
| Validation | validation.js (validatePatientDetails/MLCDetails/AddressForm), schema regex + func: validators, `validateMobileNoByAPI` (server check), `validateAdharNumber`, `validateHIDNO`, `validateJanaadharId`, `validateBhamashahId` | pre-submit + field-level |
| Aadhaar privacy | `encodeAdharToBase64` serialize function (schema `faug-serialize-function`) | value transformed before POST |
| Wizard/state | billing/admission/nursing flows: route params carry entity IDs (`:patientId/:admId/:patientIen/:visitIen`); gvar `opd_Ipd`, `unit_id`, `billingCatId` | screen state machine |
| Status transitions | client triggers; server owns state; e.g. `nursingList/deleteNursePatientAllocationOnShiftHandOver`, `bedmanagement/*`, `pathoOrder/*` release/finalize, `serviceOrder/addServicechargesNew`, `updateOrderStatus`, `updateServiceStatus` | verify against backend agent |
| Printing | common-module.js printContent* (window.print on `#printable`), qz-tray.js (ZPL labels), BrowserPrint (lab), printerSetting-ext.js + localStorage printer prefs | see §4 |
| Date logic | moment via faug-ext-globalize; `Faug.getDateUtil`; date formats `%%Fg.g(GenericUIDateFormat)%%`; server date from login `generatedTime` | timezone/locale sensitive |
| i18n | i18next + `data-translate` attrs + `Fg.t()`; `%%Fg.g(lbl)%%` label injection; `ls_locale_language` | label-driven, backend-served dictionaries |

## 4. UX Observations

- **Mobile-first legacy Bootstrap 3**: custom `input-group` wrappers per field with icon, `col-lg-*` grid inside schemas; button-groups as radio cards; a separate mobile-responsive.css + ipad-tablet-responsive.css.
- **Typeahead search everywhere**: patient/service/medication lookups are `<input>` + dropdown `<ul>` (template-rendered rows, `faug-valid-row`), keyup ≥N chars triggers server search; debounce 1s for filter components.
- **Role-adaptive chrome**: menu + visible routes change with role; store-scoped screens prompt store selection (`selectDefaultStore.html`).
- **Heavy modal usage** (`#myModal`, bootbox dialogs, `showdropdownmenu`), custom popovers, `jstree` in masters, supercal/fullcalendar/timeline schedulers for appointments & OT.
- **Print culture**: receipts, labels, coversheets, discharge summaries are printed from the browser; ZPL to QZ Tray for zebra labels; dot-matrix mode flag.
- **Feature flags multiply screens**: same schema renders differently per hospital config (NHNC/FPV/PCTS/kiosk/telemedicine/LMIS) — a React rebuild must preserve *configuration-driven* rendering, not hardcode one layout.
- **Latency tricks**: Web Worker XHR + `cachedTime` caching (120 s typical for dropdown data), gvar master-list caches, `freezeUi:false` for background fetches.
- **Data conventions**: server returns `rows`/`data` arrays for lists; ids in `data-value`; forms serialize as `<SchemaName>['field']` input names (DTO-shaped JSON built client-side); `transient` fields excluded.

## 5. API Surface Summary (for backend reconciliation)

- **≈3,857 unique endpoints** under `api/rest/*` (normalized; path-variable segments collapsed), **7,404 total references** (js + schemas + queue/ohumlite sub-apps), 3,218 in the 71 main controllers.
- Big buckets: nursingList (293), inventoryStockList (226), master (195), bloodtestmaster (115), pathoOrder (114), pharmacy (98), patient (83), otscheduler (79), masterConfigService (67), addUserService (62), serviceOrder (55), mdm (52), walkinVisit (51), equipment (50), reports (47), expiryitem (47), cssd (45), abha (40)…
- Non-REST: `v_check` (version), `aadhaarauthtest.rajasthan.gov.in/GenericFaceAuthApi/api` (external face auth), static JSON configs.
- Full map: `api-map-per-file.md` (endpoint → file → line → function), machine-readable `api-map-js-with-functions.txt`.

## 6. Open Questions

1. **ext.js/ext.css**: referenced by index.html, absent from the repo — backend-injected? (affects any file-based rebuild).
2. **Backend contracts**: confirm `api/rest/auth/*` token semantics (hardcoded header token `ZDgxOTM2ZTQt…` seen in code), `v_check`, `X-Request-ID` idempotency support, and the `login_data` payload shape (it seeds ~200 gvars).
3. **TIU-XML**: note forms are XML schema-driven (`tiuxml.json` 219 KB, per-module mappings) — scope of rebuild is large; recommend reusing the data model, not the renderer.
4. **queue/ and ohumlite/** sub-apps: separate deployments sharing patterns; confirm whether in scope.
5. **Bills/receipts printed server-side or client-side** varies per receipt type (some PDFs via `pdfAppointmentFaceSheet*`, some client-side HTML print) — must be re-tested per screen.
6. **NHNC/ABDM modules** are large recent additions (nhncModule.js 678 KB, abdm-module.js 198 KB) with external integrations (Rajasthan schemes, NDHM) — prioritize separately.
7. Exact `dynamicAspects` merge semantics (multiple branches applying simultaneously, `propertyOrder` wins) — validate in faug-min.js during port.
8. `mastersConfigration.json` (57 KB) drives the generic masters screens — its runtime semantics belong to masters-ext.js:3 + faug table widget.

---
*Generated by Agent 03 (FAUG/jQuery frontend reverse engineering). All statements evidence-backed; unknowns flagged.*
