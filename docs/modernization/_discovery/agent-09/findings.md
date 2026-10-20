# AGENT 09 — Findings: Legacy iHMS/OHUM PAS NG Frontend Discovery

> Research-only evidence dump. All paths relative to `D:\ihms\src\main\resources\public\`.
> Compiled by Agent 09 (REACT/UI MODERNIZATION ARCHITECT), 14 Aug 2026.
> Purpose: primary source for the React migration design (`09-react-migration-design.md`).

---

## 0. Executive summary of evidence

| Dimension | Count | Evidence |
|---|---|---|
| fjs-config module configs | 42 | `fjs-config_*.json` (40 module-scoped + `fjs-config.json` main + `fjs-config_schemas.json`/`fjs-config_templates.json` aux) |
| Form/viewer schema JSONs | ~1,750 | `modules/<mod>/schemas/*.json` (e.g. tiuxmls=368, masters=330, inventory=101) |
| HTML templates | ~2,600 | `modules/<mod>/templates/*.html` (nursing=349, inventory=298, commons=225, billing=189) |
| JS controllers | 71+ | `js/*.js` (module controllers + shared `common-module.js`, `components.js`) |
| Hash routes (`#module:page`) | 704 refs / ~290 distinct | extracted from templates (icon-list, masterNav, wrappers) + `mastersConfigration.json` |
| REST API endpoints referenced in JS | 1,842 unique | `api/rest/<resource>/<action>` |
| Legacy app shells | 4 | `index.html` (desktop), `ohumlite/index.html` (Cordova mobile), `queue/index.html` + `queue.html` (queue board), `apps/offlinedashboard` |

Key structural facts (evidence in §4):
1. **The app is schema-driven**: `faug-min.js` (minified form engine, ~"FAUG/FJ" framework) renders forms, viewers (data tables), modals, pagination from JSON schemas + `layer/default.json` DOM skeletons. Templates are **not** standalone pages; they are fragment/micro-templates rendered inside schema containers (`#if/#for/#var/<%var%>` template language, inline `##js##`).
2. **Routing is hash-based** (`#module:page`, e.g. `#op:addAppointment`, `#masters:unitmaster/view`), driven by `enableRouteModules: true` (fjs-config.json settings) + `Fg.getRoute()`.
3. **RBAC is declarative**: menu items/actions carry `data-roleM="Configuration.Master.Unit.AddUser"` keys (menuRolesCls/genericRolesClz), and `mastersConfigration.json` maps each master screen to a `config` role key.
4. **API layer is REST** `api/rest/<resource>/<action>` called via jQuery AJAX (`Fg.get/post/put`, `Faug.ajaxGet/Post/Call`) plus a Web-Worker wrapper (`w-api-call.js`, `numApiWebWorkers: 3`).
5. **Print** is layered: `print.css` (visibility toggling), `jQuery.print`, `qz-tray.js` (WebSocket to desktop QZ app), `BrowserPrint-2.0.0.75.min.js`, `JSPrintManager.js`, printer prefs in `localStorage` (`printerSetting-ext.js`).
6. **Clinical data entry is slider/chart-driven**: vitals via `vitalsConfiguration.json` (slider/dualslider), partograph via `ODPartoGraphConfig.json`, observation flow-sheets via `charts/ODCWF_*.json` + `ChartingApplication.js`, structured clinical forms via `tiuxml.js` + `modules/tiuxmls/schemas/*.json` (368).

---

## 1. Screen inventory

### 1.1 Module → artifact matrix

Legend: C = fjs-config, S = schemas, T = templates, J = JS controller(s), R = representative routes.

| Module | fjs-config | Schemas | Templates | JS controllers | Representative routes | Legacy UX notes |
|---|---|---|---|---|---|---|
| op | fjs-config_op.json | 43 | 73 | appointment-module.js, appointment-list-module.js, clinic-module.js, opPharmacy-new.js (partial) | `op:addAppointment`, `op:appointmentList`, `op:appointmentListOnline`, `op:appointmentFlow`, `op:doctorCheckIn`, `op:mySchedule`, `op:showDepartment`, `op:showDepartmentSlot`, `op:manageDoctorSlot`, `op:patientDetail`, `op:patientHistory`, `op:followUpVisitReqList`, `op:consultOrdersList`, `op:investigation`, `op:doctorAssessment`, `op:campRegistration`, `op:uploadAppointments`, `op:nhncAppointment(List)` | Appointment list is a category-badge strip (All/CheckedOut/CheckedIn/Pending/NoShow/Walkins/Emergency/Online/Teleconsult/FPV/RGHS) with counts — see `appointment-details.html:24-96`. Walk-in registration is one giant schema form with ~60 conditional fields, `groupingTitle` section headers, layered radio-button groups (`btn-group radio-btn`). Heavy cascading select flow: Complaints→Departments→Building→Cabin→Doctor→Slot (`walkinVisit-schema.json` namedEvents). |
| ip | fjs-config_ip.json | 65 | 120 | admission-module.js, bed-management-module.js | `ip:admissionOrder(Emer)`, `ip:admissionList`, `ip:admissionReqList`, `ip:bedManagement`, `ip:bedScreen`, `ip:bedReservation`, `ip:unReservedBedList`, `ip:transferReqList`, `ip:dischargedPatients`, `ip:updateDischargeStatus`, `ip:clearanceListFlow`, `ip:MlcclearanceListFlow`, `ip:attendantPass`, `ip:motherChildLinking`, `ip:showAllCradles`, `ip:nursingOrder`, `ip:otBedManagement`, `ip:showInterimAdmissions`, `ip:showTidList`, `ip:nursingStation` | Bed board = building/department/ward tabs + color-coded bed blocks (`bed-screen-building.html`, `bed-screen-department.html`); bed color from `row.colorCode`. Clearance workflow tabbed (`clearanceTab.html`). Attendant pass = separate screens + label printing. Admission is a wizard-ish multi-tab service confirm flow (billing `addPatientMoreDetailsRouteFromBM`). |
| billing | fjs-config_billing.json | 77 | 93 | billing-module.js, billEstimationFunctionality.js, packageBilling.js | `billing:billingFlow`, `billing:opIpBillingFlow`, `billing:ipBillingFlow`, `billing:billEstimationFlow`, `billing:billingListFlow`, `billing:dischargeList`, `billing:advanceRefund`, `billing:creditNoteBillingListFlow`, `billing:receiptList`, `billing:showSponsor`, `billing:doctorShare(AddDeduct/Bill)`, `billing:otBillFlow`, `billing:packageSubscriptionFlow`, `billing:packageAppointmentList`, `billing:crossOrderService` | Service-line item tables (`addServiceRow.html`, `addServiceRowIpd.html`) with in-row editors (bootstrap-editable), estimate/provisional flow, draft→final bill summary, receipts. Very dense modal-heavy screen (`addBilling.html` = patient search → service add → confirm). |
| pharmacy | fjs-config_pharmacy.json | 39 | 143 | pharmacy-module.js, pharmacy-module-new.js, pharmacy-module-op.js, opPharmacy-new.js | `pharmacy:pharmacyOrderVerify`, `pharmacy:pharmacyOrderDispense`, `pharmacy:opPharmacyFlow`, `pharmacy:pharmacyCountersale`, `pharmacy:ivPharm`, `pharmacy:cartFillFlow`, `pharmacy:prepareCompound`, `pharmacy:pharmPresc`, `pharmacy:addPrescription`, `pharmacy:PresciptionList`, `pharmacy:itemSaleList`, `pharmacy:itemSaleReturnList`, `pharmacy:opIpPharmacyBilling`, `pharmacy:pharmacyBilling`, `pharmacy:NursePrescriptionList`, `pharmacy:nurseOrdrDispensed`, `pharmacy:ipReturnPharmFlow`, `pharmacy:pharmacyDrugMapping`, `pharmacy:pharmacyItemList`, `pharmacy:pharmInterventionreport`, `pharmacy:prescriptionUpload` | Medication rows are dynamically appended form-row fragments (`addMedication.html` is a shell; rows via `batchRow.html`, `dispenseDetailRow.html`). Barcode scanning (`.scanBarcodeCartFill`, `printSingleBarCode`). BCMA (bedside) has its own module w/ med-log + scanner screens (`bcmaMainScreen.html`, `bcmaMedicationLogForScanner.html`). |
| nursing | fjs-config_nursing.json | 28 | 371 | nursing-module.js, nursing-module-ext.js, nursing-note-module.js, nursing-charting-module.js, nursing-constant.js, alliedHealth-module.js, cwf-module.js, scwf-module.js | `nursing:roleNurse`, `nursing:roleNurseIncharge`, `nursing:roleNursePatientView`, `nursing:rolePatientNote`, `nursing:rolePlanofCare`, `nursing:orderingSet`, `nursing:discharge`, `nursing:transfer`, `nursing:mortuary`, `nursing:incidentReporting`, `nursing:addnewIncident`, `nursing:roleLab`, `nursing:roleRadiology`, `nursing:roleFrontOfficer`, `nursing:ComplaintDiagnosis`, `nursing:DiagnosisTreatment`, `nursing:MyOpPatients`, `nursing:annotation`, `nursing:roleNurseRostering` | Nurses notes = modal textarea with title tiles + sign (`addNursesNotes.html`). Cover sheet = big patient snapshot (`showPatientCoversheetSchema`, `showNurseWorklistSchema`). Notes tile matrix (`chartingTiles.html`). Signed-note workflow with electronic signature modals. TIUXML clinical forms rendered by tiuxml.js. |
| pathology | fjs-config_pathology.json | 57 | 54 | patho-modules.js, patho-booking-module.js | `pathology:sampleCollection`, `pathology:sampleAcceptReject`, `pathology:sampleResult`, `pathology:sampleResultReview`, `pathology:sampleReportRelease`, `pathology:sampleMachineWiseResult`, `pathology:sampleCriticalAlert`, `pathology:workLoadList`, `pathology:showStatus`, `pathology:updateOrderStatus`, `pathology:groupingAndTest`, `pathology:kdlVerify`, `pathology:lab`, `pathology:printAllReport`, `pathology:selectedOrderType`, `pathology:updateServiceStatus`, `pathology:showLabResult`, `pathology:showCovidLabList` | All lab stages are **filter → list → detail** screens (`pathoFilterSchema`, `pathoReportDetailsSchema`). Result entry via template-based editors (`finalize.html`, `parameterResult.html`, `microbiology-schema.json`), barcode label printing, report release with double verification (firstVerification/secondVerification). |
| radiology | fjs-config_radiology.json | 9 | 6 | (patho-modules.js shares filters) | `radiology:radlab`, `radiology:printReport` | Lighter than patho: booking (`radOrderBookingSchema`), result entry (`radReportSchema`, `radioFinalizeTemplate.html`), retest/reschedule. |
| inventory | fjs-config_inventory.json | 70 | 196 | inventory-module.js, consignmentInventory.js, procurement.js, quotationDetails.js, itemEnquiry-module.js, item-category-module.js, expiryDamageItemReturn.js, billEstimationFunctionality.js (partial) | `inventory:indentListofStore`, `inventory:purchaseIndent`, `inventory:purchaseRequest`, `inventory:consolidatePurchaseRequest`, `inventory:quotation`, `inventory:currentPO`, `inventory:grn`, `inventory:multipurposeGRN`, `inventory:approveGrn`, `inventory:reOrderList`, `inventory:itemStockList`, `inventory:itemSummary`, `inventory:stockAdjustmentList`, `inventory:stockTransfer`, `inventory:issueItem`, `inventory:directIssueItem`, `inventory:consumeItemforStore`, `inventory:departmentalConsumption(List)`, `inventory:returnItem`, `inventory:returnItemRecieve`, `inventory:returnToSupplier`, `inventory:expiryItemReturn(ToSupplier/List)`, `inventory:expiredItemList`, `inventory:expiredItems`, `inventory:expire`, `inventory:recallExpiredItem`, `inventory:holdItemBatches`, `inventory:updateBatch`, `inventory:itemEnquiry`, `inventory:itemMasterList`, `inventory:assetRegisterList`, `inventory:QcStatusUpdate`, `inventory:userStore`, `inventory:stockLocation`, `inventory:opeiningBalance`, `inventory:allApproval`, `inventory:customOrderList` | Workflow-heavy: indent freeze→approve (2-level)→PO→quotation→GRN→QC→stock; each stage a dedicated screen with distinct templates (`showIndentDetailsOnFreezed/OnApproved/Editable`). Approval panels ubiquitous. Editable in-row tables. |
| bloodBank | fjs-config_bloodBank.json | 40 | 98 | blood-bank-module.js | `bloodBank:donorRegistration`, `bloodBank:donorList`, `bloodBank:campDonation`, `bloodBank:campRequisition`, `bloodBank:patientRequisition`, `bloodBank:externalPatientRequisition/add`, `bloodBank:bloodCrossMatching`, `bloodBank:crossMatchingList`, `bloodBank:issueBloodBag`, `bloodBank:bloodBagTrail`, `bloodBank:bloodBagOpeningStock`, `bloodBank:addNewBloodComponent`, `bloodBank:bloodBagComponentExtraction`, `bloodBank:bloodBagDiscard`, `bloodBank:BloodDiscardList`, `bloodBank:retrunBloodbag`, `bloodBank:crossUnitOrder`, `bloodBank:eraktkoshRequest`, `bloodBank:replacementDonorHistory`, `bloodBank:BloodBanKReOrder`, `bloodBank:transfusionTransmittedDiseases`, `bloodBank:redCellSerology`, `bloodBank:showIndentList` | Bag-centric: barcode labels (`bloodBagBarCode.html`, `bloodBagIssueBarcode.html`), bag status lifecycle (collect→separate→crossmatch→issue→return/discard), donor forms (voluntary/replacement/autologous/camp). |
| ot | fjs-config_ot.json | 14 | 48 | operation-theatre.js, (scheduler.js) | `ot:surgList`, `ot:surgSchedule`, `ot:otScheduleEvent`, `ot:nurseScheduleEvent`, `ot:itemDetailsEvent`, `ot:patMovementAudit`, `ot:patientProcedureRateDetails`, `ot:procPerformance` | Scheduler-board screens (`otSchedular.html`, `nurseSchedular.html`) + surgery order list + procedure performance + patient movement audit. |
| cssd | fjs-config_cssd.json | 15 | 49 | cssd-module.js | `cssd:*` (10 routes) | Kit master/preparation/sterilization; kit requests to store; barcode labels. |
| allocation | fjs-config_allocation.json | 3 | 22 | nursing-module-ext.js | `allocation:*` (10 routes) | Nurse↔ward/doctor/patient allocation, shift details, rosters. |
| diet | fjs-config_diet.json | 4 | 10 | diet-kitchen-module.js | `diet:*` (4 routes) | Diet order → kitchen view → food items → schedule screens. |
| transport | fjs-config_transport.json | 11 | 15 | transport-module.js | `transport:*` (7 routes) | Vehicle/driver masters, trip requests, fuel station, maintenance. |
| equipments | fjs-config_equipments.json | 34 | 78 | equipment-modules.js | `equipments:*` (19 routes) | Asset register, preventive maintenance schedules, complaints, calibration, GRN-for-equipment. |
| queue | fjs-config_queue.json | 11 | 5 | (queue app js/) | `queue:*` (11 routes incl. `queue:departmentQueue10`) | Queue-board app (separate shell `queue/index.html` + `queue.html`); token flow, doctor-batch wrap, cabin assignment. |
| masters | fjs-config_masters.json | 330 | 197 | masters-modules.js, masters-ext.js, user-module.js, item-category-module.js, notification-template-module.js, docit-roles.js | 300 routes `#masters:<name>/view` driven by `mastersConfigration.json` | One generic CRUD pattern (`genericMasters.html` grid → `genericMastersFilters.html` → master form) covering 300 screens incl. billing/order-set/plan/company masters. |
| commons | fjs-config_commons.json | 67 | 172 | common-module.js, components.js, auth-init-module.js | 45 routes | Cross-cutting: patient search, ABHA/ABDM screens (`abha*` templates, `abdm-module.js`), mortuary, notification templates, shift/roster, file upload, geo-tagging, audit. |
| charting | fjs-config_charting.json | 1 | 26 | ChartingApplication.js, nursing-charting-module.js | `charting:*` (2 routes) | The **charting app**: tiles of notes/vitals/labs/orders, growth charts, partograph, observation flow sheets (`ODCWF_*`), vital range sliders. |
| careplan | fjs-config_careplan.json | 1 | 7 | (nursing) | via nursing rolePlanofCare | NCP problem/goal/plan-of-care editor (`ncpSubProblems.html`). |
| chemotherapy | fjs-config_chemotherapy.json | 3 | 62 | chemotherapy-module.js | `chemotherapy:*` (2 routes) | Regimen builder, cycle orders, meds confirm, MDC notes. |
| bcma | fjs-config_bcma.json | 11 | 44 | bcma-module.js | `bcma:*` (1 route `bcma:bcma` in templates) | Bedside medication admin: due list nav, med log (incl. scanner mode), IV bags chronological actions, variances, allergies report. |
| biomedicalwaste | fjs-config_biomedicalwaste.json | 1 | 11 | bioMedicalWaste-module.js | `biomedicalwaste:*` (1) | Request→receive→decompose, bag barcodes. |
| linenlaundry | fjs-config_linenlaundry.json | 1 | 9 | linen-laundry-module.js | `linenlaundry:*` (1) | Linen request/dispatch/receive, handover items. |
| rmrs | fjs-config_rmrs.json | 9 | 27 | rmrs-module.js | `rmrs:*` (7 routes) | Payments/receipts accounting: dashboard, payment collection, bank statements, journal entries, discounts, authority contracts. |
| rcm | fjs-config_rcm.json | 1 | 1 | rcm-module.js | `rcm:*` | Chiranjeevi app integration screen. |
| pmrd | fjs-config_pmrd.json | 2 | 13 | pmrd-module.js | `pmrd:*` (1) | Patient medical record document management, form J1/J2/J3 (sub-certificates). |
| fileManagement | fjs-config_fileManagement.json | 1 | 4 | file-management-module.js | via pmrd/commons | Document categories, view docs, annotated image view (markerjs). |
| grievance | fjs-config_grievance.json | 3 | 8 | grievance-module.js | `grievance:*` (2) | Ticket list → status/severity change → assign → history → action popup; feedback form. |
| enquiry | fjs-config_enquiry.json | 8 | 9 | (commons) | `enquiry:*` (6) | Hospital events, dept-doctor info, visit/admission enquiry, feedback. |
| biReport | fjs-config_biReport.json | 1 | 2 | report-module.js | `biReport:*` (45 routes — report menu) | BI report embed (`report_BI.html` — likely iframe/PowerBI). |
| reports | fjs-config_reports.json | 46 | 5 | report-module.js | `reports:*` (2) | Report wrapper + user filter (`allReportWrapper.html`, `report-user-filter.html`); 46 report schemas (filter params). |
| kiosk | fjs-config_kiosk.json | 4 | 2 | (op) | `kiosk:kiosk` | Self-registration kiosk: dept/slot selection + OP card printing. |
| nhcx | fjs-config_nhcx.json | 1 | 2 | nhcx-module.js | `nhcx:*` | Eligibility check + pre-auth (NHPS insurance). |
| shifthandover | fjs-config_shifthandover.json | 1 | 20 | (nursing) | via nursing roleNurseIncharge | Multi-patient shift handover: patient selector + per-patient sections (meds/labs/rad/procedure/transfer) done/due lists. |
| worklist | fjs-config_worklist.json | 1 | 23 | (nursing-module) | `worklist:*` (3) | Role-nurse task list: patient-wise/task-wise, goals, orders/meds/labs/rad/interventions, sign-notes-from-task. |
| tiuxmls | fjs-config_tiuxmls.json | 368 | 380 | tiuxml.js (engine) | `tiuxmls:*` (2) | **Structured clinical document library** (NHNC + OP + chemo + nursing initial assessment etc.). Rendered by `tiuxml.js` into tabbed, indented clinical forms. |
| patientManagement | fjs-config_patientManagement.json | 1 | 0 | (op/nursing) | `patientManagement:*` (1) | (route only; screens live in op/nursing) |
| paymentTransfer | fjs-config_paymentTransfer.json | 3 | 1 | payment-transfer-module.js | `paymentTransfer:*` (2) | Counter payment transfer. |
| nursingWorklist | fjs-config_nursingWorklist.json | 1 | 1 | (nursing) | via nursing | Bed transfer task (worklist variant). |

Total distinct module domains: **~40** (fjs-config count 42 incl. aux). Representative route inventory: 704 route references, ~290 distinct `module:page` targets (masters alone 300).

### 1.2 Screen archetypes (recurring, evidence)

1. **Schema form screen** — `walkinVisit-schema.json` (details.form.create, fields w/ `ui`, `custom.class`, `validations`, `events`, `crud.c` POST URL).
2. **Viewer/list screen** — `pathoReportSchema.json` (`details.viewer[0].id`, `crud.ra` POST → template). Server-side paging with client template rows (`showPatientWorkList.json`, `_t_*` table skeleton in `layer/default.json`).
3. **Filter→list→detail workflow screen** — pathology (`pathoFilterSchema` + `pathoReportDetailsSchema` + `loadPathoFilters` in patho-modules.js), inventory (`showCurrentPurchaseOrderListSchema` + `showPODetailsSchema`).
4. **Board/tabular-UI screen** — bed board (`bed-screen-building.html`), OT scheduler (`otSchedular.html`), queue board (`departmentQueue10.html`), BCMA nav (`bcmaNavDueList.html`).
5. **Tabbed work area** — IP patient detail (`ipd_patient_service_detail_schema.json`, clearance tabs `clearanceTab.html`), nursing cover sheet (`showPatientCoversheetSchema.json`).
6. **Modal fragment** — nearly every template is a modal-body fragment opened via `Faug.getHtml(template)`; e.g. `apptCancelReason.html`, `electronicSign.html`, `capturePhoto.html`, `addNursesNotes.html`.
7. **Generic master CRUD** — `genericMasters.html` (grid of config cards) → `genericMastersFilters.html` → schema form; 300 masters driven by `mastersConfigration.json`.
8. **Charting/clinical app** — `ChartingApplication.js` + `chartingTiles.html` + ODCWF JSONs + `growthCharts.html` + `partoGraph.html`.

### 1.3 Module coverage of this inventory

Full coverage (deep): op, ip, billing, pharmacy, nursing, pathology, radiology, inventory, bloodBank, ot.
Good coverage: cssd, allocation, diet, transport, equipments, queue, masters (generic pattern), commons, charting, chemotherapy, bcma.
Lighter (route+template level): biomedicalwaste, linenlaundry, rmrs, rcm, pmrd, fileManagement, grievance, enquiry, biReport, reports, kiosk, nhcx, shifthandover, worklist, tiuxmls, paymentTransfer, careplan, nursingWorklist, patientManagement.

**Unknowns:** exact wiring of `faug-min.js` internals (minified; `faug-ext.js` only covers date/currency/i18n extension hooks); exact server-side of `print.server.url` (no match in public JS — printing is client-side + QZ); masters list is driven by server-returned config (`getModuleWiseMasters`), so the 300-route table is config-derived, not hard-coded.

---

## 2. UI stack inventory (index.html + runtime)

| # | Library | File(s) | What it contributes | React replacement |
|---|---|---|---|---|
| 1 | **FAUG/FJ framework** | `faug-min.js`, `faug-ext.js`, `layer/default.json`, `faug-ext-globalize.js` | Form engine (schema→DOM), viewer engine (schema→DataTable), modals, pagination, events/callbacks/controlEvents, CRUD ops, route modules, i18n hooks, freeze loader | **Native React** (form system, table, modal) — the schema JSONs become React form configs |
| 2 | jQuery + Bootstrap 3 | `jquery.min.js`, `bootstrap.min.js/css` | Grid (col-lg-12…), panels, tabs, modals, buttons, form-control, table styles | Tailwind/shadcn or MUI grid + primitives |
| 3 | DataTables | `jquery.dataTables.js`, `dataTables.bootstrap.css`, `dataTables.tableTools.js` | Client-side sorting/search/paging/export (CSV/XLS/PDF swf) | TanStack Table |
| 4 | select2 | `select2.full.js` | Remote-option `<select>` search (`.select2Clz` classes in schemas) | `react-select` / Combobox (Radix) |
| 5 | bootstrap-select | `bootstrap-select.min.js` | Enhanced dropdowns (doctor lists, `.selectpicker`) | native + Combobox |
| 6 | bootstrap-multiselect | `bootstrap-multiselect.js` | Multi-select pills (e.g. worklist filters) | CheckboxGroup/Combobox multi |
| 7 | bootstrap-tags / tagsinput + typeahead | `bootstrap-tags.js`, `bootstrap-tagsinput.min.js`, `bootstrap-typeahead.min.js`, `bootstrap3-typeahead.min.js` | Tag entry, quick typeahead search (patient lookup) | TagInput / `downshift` |
| 8 | jquery.datetimepicker (+ supercal, moment) | `jquery.datetimepicker.js/css`, `jquery.supercal.js`, `moment.min.js`, `calendar.moment.min.js` | Date/time/calendar pickers; date-config callbacks in schemas (`dateConfig.minDate…`) | `date-fns`/dayjs + `react-day-picker` (or MUI DatePicker) |
| 9 | fullcalendar | `fullcalendar-min.js`, `fullcalendar.css`, `jquery-fullcalendar-ui-min.js`, `fullcalendar-index.js` | Appointment/schedule calendars | `react-big-calendar` or FullCalendar React wrapper |
| 10 | timelineScheduler | `timelineScheduler.js/css`, `timelineScheduler-calendar.js`, `timelineScheduler-jquery-ui-*.js` | Gantt/timeline resource scheduler (OT/nurse schedules) | `gantt-task-react` / FullCalendar timeline view / Scheduler (bryntum-style self-host) |
| 11 | jstree | `jstree.min.js/css` | Tree views (masters hierarchy, cssd kit trees) | Radix Tree / react-arborist |
| 12 | ion.rangeSlider | `ion.rangeSlider.js/css` | **Vital-entry sliders** (chartVitalRangeSlider.html) | Native range/dual-thumb slider component |
| 13 | touchspin | `jquery.bootstrap-touchspin.js` | Numeric steppers | NumberInput |
| 14 | Chart.js + highcharts/highstock | `Chart.min.js`, `highcharts.min.js`, `highstock.min.js`, `exporting.min.js` | Growth charts, partograph, trend charts | `recharts`/`visx` (or Highcharts React) |
| 15 | tinyMCE | `tinymce.min.js` | Rich notes (notes, tiuxml text) | `tiptap`/Lexical |
| 16 | webcam | `webcam.min.js`, `webcam.swf` | Photo capture (capturePhoto.html) | native `getUserMedia` hook |
| 17 | jquery-barcode / qrcode | `jquery-barcode.min.js`, `jquery.qrcode.min.js`, `qrcode.min.js`, `html5-qrcode.min.js` | Barcode labels, QR prints, QR scanning (BCMA, cart fill) | `jsbarcode`/`qrcode`/`html5-qrcode` (or ZXing WASM) |
| 18 | markerjs2 / painterro / fabric | `markerjs2.min.js`, `painterro-1.2.87.min.js`, `fabric.all.min.js` | Image annotation (annotatedImageView.html, annotation-view.js, fileManagement) | markerjs2 (has React support) / `react-paint` |
| 19 | signature_pad | `signature_pad.umd.min.js` (ohumlite/queue) | Canvas signatures | signature_pad React wrapper |
| 20 | pdf.js / pdfobject / jspdf + autotable | `pdf.min.js`, `pdf.worker.min.js`, `pdfobject.js`, `jspdf.min.js` | PDF preview/embed + client PDF generation | `pdfjs-dist` + `jspdf` (or server-side print) |
| 21 | html2canvas / dom-to-image | `html2canvas.js`, `dom-to-image.min.js` | Screen→image capture (charts, boards) | html2canvas-pro |
| 22 | **Print**: jQuery.print, BrowserPrint, qz-tray, JSPrintManager | `jQuery.print.js`, `BrowserPrint-2.0.0.75.min.js`, `js/qz-tray.js`, `JSPrintManager.js`, `printerSetting-ext.js`, `print.css` | Browser print, Zebra label printing, thermal/dot-matrix via local agent, printer prefs in localStorage | `window.print()` + QZ Tray JS API (vendor lib retained) + print hook service |
| 23 | blockUI / bootbox | `jquery.blockUI.js`, `bootbox.min.js`, `jquery.loadmask.js` | Blocking loaders, alert/confirm dialogs | Native overlay + dialog components |
| 24 | webui-popover | `jquery.webui-popover.min.js/css` | Context popovers | Radix Popover/Tooltip |
| 25 | jquery-ui | `jquery-ui.min.js` | Draggable/sortable/dialog (schedulers) | @dnd-kit |
| 26 | i18next | `i18next.min.js`, `i18nextXHRBackend.min.js`, `resources/globalize/`, `resources/cldr/` | i18n (schema `display` strings via `Fg.t()`, `translateLang` attr) | `react-i18next` |
| 27 | rsvp / bluebird / promise | `rsvp-3.1.0.min.js`, `bluebird.min.js` | Promise polyfills | native promises |
| 28 | sjcl / sha-256 / jsrsasign | `sjcl.js`, `sha-256.min.js`, `jsrsasign-all-min.js` | Client-side hashing/encryption (QZ handshake, ABHA) | WebCrypto / vendor libs |
| 29 | tableHeadFixer | `tableHeadFixer.js` | Sticky table headers | CSS `position: sticky` |
| 30 | enscroll / timeago / bxslider / iframeResizer | various | Custom scrollbars, relative time, carousels, iframe sizing | CSS / `Intl.RelativeTimeFormat` / native / iframeResizer-react |
| 31 | html5shiv/respond | (IE8 shims) | Legacy IE support | **drop** |
| 32 | aadhaarauth.js | `resources/aadhaarauth.js` | Aadhaar auth/OTP flows (ABDM) | keep as service module |
| 33 | help-navigator.js | `resources/help-navigator.js` | Contextual help | — |
| 34 | polyfill.min.js, nashorn-polyfill.js | (mobile shells) | ES6 polyfills | drop (modern browsers) |

### 2.1 Mobile/standalone shells

- `ohumlite/` — standalone web-app bundle for **Cordova/mobile**: own `index.html`, `fjs-config.json`, reduced `js/` set (app-logic.js, members-module.js, appointment-token-flow.js, abdm-module.js, billing-module.js), `w-api-call.js` (worker), `signature_pad.umd.min.js`, `markerjs2`, `painterro`. Evidence: `ohumlite/js/*` list.
- `queue/` — **queue board** bundle: own `index.html` + `queue.html`, `appointment-module.js`, `auth-init-module.js`. Evidence: `queue/js/*`.
- `apps/offlinedashboard` — offline dashboard bundle.
- `w-fetch-file.js` — file download helper used by shells.

---

## 3. Reusable component candidates (evidence)

| Candidate component | Legacy source (evidence) | API dependency (evidence) | Notes |
|---|---|---|---|
| **PatientSearch** (universal lookup by MRN/HID, name, mobile, civil/Aadhaar, driving license, passport, Bhamasha, Janaadhar, ABHA, ECTS/PCTS) | `patient-search-list.html` (commons), controlEvents `.pat-mrnsearch-box/.pat-mobsearch-box/.pat-civilId-search/.pat-passp-search/.pat-drivingl-search/.familyIdsearch/.janaadharIdSearch/.abhaIdSearch` in `walkinVisit-schema.json` callbacks; `Faug.ajaxPost("api/rest/patient/getPatientGeneralDetailsListForSearch…")` in common-module.js:14267, appointment-list-module.js:2861, billing-module.js:13407, inventory-module.js:7387 | POST `api/rest/patient/getPatientGeneralDetailsListForSearch?is_admission=&isRegSearch=` | Multi-keyword OR search; nursing variant `api/rest/nursingList/getPatientGeneralDetailsListForSearchNursing?duz=…` (nursing-module.js:1231) |
| **PatientBanner / Coversheet** | `showPatientCoversheetSchema.json` (nursing), `showPharmacyPatientCoversheetSchema.json`, `ipd_patient_service_detail_schema.json` (IP work area), `patientServiceDetailSchema.json` (billing); `loadCoverSheetDetails()` nursing-module.js | `api/rest/nursingList/*` coversheet endpoints (274 refs) | Dense identity+status header with action tabs |
| **DoctorSelector** | `doctors-schema.json`, `showDeptDoctorInformation.html`, `doctorFilterFilter` (`appointment-module.js:2169` `api/rest/master/doctorFilterFilter`), `doctorScheduleWrap.html` | GET `api/rest/master/doctorFilterFilter`; dept-wise doctor lists | Remote search combobox |
| **DepartmentSelector / CabinSelector** | `department-schema.json`, `getValidDepartments()` (appointment-module.js:2059), `getCabinsByDept` → `api/rest/addUserService/getDepartmentwisecabinById` (appointment-module.js:1332) | POST `api/rest/appointmentschedule/getDepartmentsByDate`; GET `…/getCabinWiseApointmentSlotCount` | Cascades with complaint-age-gender eligibility |
| **HospitalSelector / UnitContext** | `hospitalCreationSchema.json`, `unit_id` global (`fjs-config.json` globals), changeCashCounter.html, hospital list | `api/rest/master/*` unit masters | Session-level, not per-screen |
| **DataTable (server-paged)** | faug `viewer` engine: `layer/default.json` `"table"` skeleton (`_t_*` classes), `details.viewer` in schemas (e.g. `pathoReportSchema.json`), `serverPaginate`, `isDataTable` flags | any `crud.ra` op | Server paging + client page-size + search + download |
| **Pagination** | `layer/default.json` `table.footer` (Prev/Next, "Page X of Y" inline) | — | — |
| **DateRangePicker / Filters** | `labDateFilter.html`, `radDateFilter.html`, `generic-report-filter-list.html`, `department-report-filter-list.html`, report schemas (fjs-config_reports 46 schemas) | `api/rest/report/*` per report | Filter bar = dept/date/status selects |
| **StatusBadge** | `btnNotify` classes: `compNotify/checkInNotify/pendingNotify/noshowNotify/walkinsNotify/emergencyNotify/onlineNotify/teleconsultNotify/fpvNotify/rghsNotify` (app.css), `notifymanageCls` (appointment-details.html) | — | Color semantics in §5 |
| **ClinicalForm (schema-rendered)** | `faug-min.js` form engine + `layer/default.json` form skeletons (`form-group`, `.form-label`, `.asteriksCls`, `input-group-addon` icon+label), `validations.cond` + `errorselector`, `events` chains | `crud.c/u` URLs in each schema | The single most important engine port |
| **ApprovalPanel** | inventory approval stage screens: `approvePOPage.html`, `showIndentDetailsOnApprovedSchema.json`, `showIndentDetailsOnFreezedSchema.json`, `indentUpdateApprove()` inventory-module.js; billing `approveAmountListSchema.json`, `addApprovalAmountSchema.json` | `api/rest/inventoryStockList/*`, `api/rest/companyapprovedamount/*` | 2-level approval workflow state machine |
| **AuditHistory** | `auditSchema.json`, `newAuditSchema.json`, `patientMovementAudit.html` (ot), `getAddendumNoteList.html` (allocation) | `api/rest/audit/*` (schema name auditSchema) | — |
| **ConfirmationDialog** | `Faug.alertDialog`, `bootbox`, viewer `deleteButton.msg` ("Are you sure…?", layer/default.json), `admissionCancelReason.html` / `apptCancelReason.html` (reason-required confirms) | — | Reason-required confirm variant common |
| **PrintDialog / PrintService** | `common-module.js` `printContent()/printContentBarcode()/printContent2()/printContent2WithoutHeader()` (lines 119-588), `printerSetting-ext.js`, `qz-tray.js`, `print.css`, `billReceiptSchema.json` receipts | client-side + QZ | Printer select (system/barcode), dot-matrix flag in localStorage |
| **SignatureCapture (e-sign)** | `electronicSign.html` (password-style sign), `changeElectronicSign.html`, `signature_pad.umd.min.js` (ohumlite), `firstVerificationSchema/secondVerificationSchema.json` (double verify) | `api/rest/*` sign/verify endpoints | Two modes: typed e-signature + canvas |
| **PhotoCapture** | `capturePhoto.html` + `webcam.min.js`, `take_snapshot()`, `confirmCameraImg` | `api/rest/fileService/*` upload (18 refs) | — |
| **FileUpload / DocumentManager** | `fileUploadForm.html`, `uploadFileSchema.json`, `uploadDocumentSchema.json`, `amendmentFileUpload.json`, fileManagement module (`addDocumentCategory.html`, `filesAndDocumentsByCategory.html`, `viewDocument.html`) | `api/rest/fileService/*` | — |
| **GrowthChart** | `growthCharts.html` (charting), `charts/ODCWF_SAM_CHART.json` etc., ChartingApplication.js | `api/rest/nursingList/*` chart data | WHO growth charts; Highcharts-driven |
| **Partograph** | `partoGraph.html`, `partoVitalRangeSlider.html`, `ODPartoGraphConfig.json` (leftView/rightView chart+slider configs, chartType spline/column, `inputViews` VITAL abbreviations) | nursing vitals APIs | Config-driven rendering — port config JSON as-is |
| **VitalRangeSlider** | `chartVitalRangeSlider.html`, `vitalsConfiguration.json` (sliderType: slider/dualslider, unit ranges, qualifiers), `addVital` (chartingAddVital.html) | `api/rest/nursingList/*` | Units conversion (C/F) built in |
| **ScheduleBoard / Timeline** | `timelineScheduler.js` + `otSchedular.html`, `nurseSchedular.html`, `showDoctorOpSchedule.json`, `showDoctorFutureAppointmentSchedule.json`, `otscheduledetails.json` | `api/rest/otscheduler/*` (66 refs), `api/rest/appointmentschedule/*` | — |
| **QueueBoard** | `queue/` app, `departmentQueue10.html`, `cabinQueueListWrap.html`, `doctorBatchWrap.html`, `assignDoctorCabin.html`, `factsPopup.html`, `queueManagment` API (12 refs) | `api/rest/queueManagment/*` | Live token queue w/ sound/facts |
| **BedBoard** | `bed-screen-building.html/department.html`, `bed-managment-building.html`, `bed_management_schema.json`, `bed_screen_schema.json`, `bedBlock.html`, `bedTransferList.html` | `api/rest/bedmanagement/*` (22 refs) | Building→dept→ward tabs; color-coded beds |
| **Tree (jstree)** | `jstree.min.js`, masters hierarchy, `nurseWardAllocationDetails.html` | — | — |
| **ImageAnnotation** | `markerjs2.min.js`, `painterro-1.2.87.min.js`, `annotatedImageView.html` (fileManagement), `annotation-view.js`, `annotation` route (nursing) | `api/rest/fileService/*` | — |
| **ElectronicSign verification** | `firstVerificationSchema.json`, `secondVerificationSchema.json`, `boxCommonVerification.json` (commons) | — | Dual-auth pattern for report release |
| **Audit/Geo-tagging** | `hospitalGeoTagging.json` | — | — |
| **BarcodeLabel** | `cssdBarCodeLabel.html`, `bloodBagBarCode.html`, `batchBarCodeCount.html`, `emergencyBarcode.html`, `opLabelPrint.html`, `qRCodePrint.html`, `pharmacyLabels.html` | print service | — |
| **Telemedicine components** | `telemedicine-radio-list-department.html`, `telemedicine-speciality-unit.html`, `telemedicine-unit-list-dropdown.html` | appointmentschedule APIs | Specialty/unit cascade |
| **Roster/Shift** | `addShiftDetails-Schema.json`, `shiftRoster-Schema.json`, `showIpdRosterSlotsSchema.json`, `addIpdRosterSlotsSchema.json` | `api/rest/master/getModuleWiseMasters`-loaded masters | Nurse rostering |
| **Dashboard tiles (role home)** | `icon-list.html` (role-based tile grid), `tiles.js`, `biReport` reports, `showAllEventTypes.html` | `mastersConfigration.json`-style config | RBAC tiles → role home |
| **ChangeLog/Notes tile matrix** | `chartingTiles.html`, `chartingThumbnails.html` | — | Charting app home |
| **Token flow** | `ohumlite/js/appointment-token-flow.js`, `pat-token` controlEvents, `queueEvtCls` (stopAndStartCounterQueue) | `api/rest/walkinVisit/*`, `api/rest/queueManagment/*` | — |
| **ReportEmbed** | `report_BI.html`, `allReportWrapper.html` (iframe), `biReport:*` 45 routes | — | iframe embed pattern |

---

## 4. Architecture & flow evidence (for the React design)

### 4.1 Rendering model
- `index.html` bootstraps jQuery+Bootstrap+faug. All app DOM is generated by faug into container divs; templates are fragments bound via `Faug.getHtml(template, data)` / schema `details.form.parentEleId` (e.g. `view_cont`).
- `layer/default.json` defines: form skeleton (`.form-group`, label + `.form-label`, `.asteriksCls` required marker, `.help-block` errors, `.has-error/.has-success`), viewer (`.table.table-striped.table-bordered.table-hover`), modal template, table header/footer with paging/search/download, freeze-loader overlay HTML.
- Schema field `custom.properties.customTemplate` (walkinVisit) shows the layout atom: `input-group > input-group-addon(icon + label) + element + helptext`, 4-col grid (`col-lg-4…`).

### 4.2 Event model
- Schema `events.default/change/blur` → chains of `{type: func|ajax|full, target, url, meth, content, done:[…]}` with callbacks; `namedEvents` map (e.g. `loadDepartmentsAndCabins`).
- `callbacks.afterDraw.controlEvents` = imperative jQuery wiring (`jqselector`, `event`, `cond`, `crudop`, `func`).
- These two become React: declarative data-dependency chains (effects/derived state) + imperative hook wiring.

### 4.3 CRUD/API model
- `crud` op keys: `c` (create), `u` (update), `d` (delete), `r` (read by id), `ra` (read all), plus **named ops** per schema (e.g. `searchToken`, `patientSearchbyEctsNo` in walkinVisit-schema.json). Each: `meth`, `url`, `content`, `exResCd`, `template` (row/list template), `callbacks` (beforeOp/afterOp/failure).
- Global API families (from 1,842 endpoint inventory, top by count): nursingList 274, inventoryStockList 175, master 100, bloodtestmaster 91, pharmacy 88, otscheduler 66, pathoOrder 60, patient 46, addUserService 43, cssd 39, expiryitem 38, serviceOrder 36, equipment 35, abha 35, mdm 32, walkinVisit 27, RMRS 26, masterConfigService 25, patientTransferDischarge 24, diet 23, bedmanagement 22, transport 20, medication 18, fileService 18, packageservice 17, otconfig 17, appointment 16, patientRegAddmission 16, patientRegAppt 16, bill 16, preventivemaintenance 15, doctor 15, itemmaster 14, cwf 13, companyapprovedamount 13, billReceipt 13, queueManagment 12, linenLaundry 12, ipdadmission 11, bioMedicalWasteManagement 11.
- Transport: jQuery AJAX + Web-Worker wrapper (`w-api-call.js`: XMLHttpRequest + `X-Request-ID` idempotency header for POST/PUT/DELETE, cached API data `cachedApiData`, `cachedTime` cache with `cacheKey`).

### 4.4 State model
- Globals: `Fg.g()/Fg.addVar()` key/value bag (e.g. `unit_id`, `duz`, `isNhnc`, `app_type_w`, `config_app`, `gov_com_masters`, login role vars); `Faug.getGlobalVar()`; route via `Fg.getRoute()`; localStorage for printer prefs & login token hints.
- Module masters loaded once via `api/rest/master/getModuleWiseMasters?module=` (common-module.js:76) into caches.

### 4.5 RBAC
- `data-roleM` keys everywhere (menu, buttons, tiles); `mastersConfigration.json` `config` field maps master screens to role keys; page flow counters from `fjs-config.json` globals (e.g. `billing_Flow: "B.1"`).

### 4.6 i18n
- `data-translate="KEY"` attributes + `Fg.t("KEY")`; schema `display` strings; `notesTitle.json` (note title keys); locales via i18next + globalize/cldr.

### 4.7 Print specifics
- `printContent()` → copies element HTML into `#printable #printableContent` then `window.print()` (common-module.js:119-140); `printContentBarcode()` (142); `printContent2(htm)` (526) direct-HTML print; `printContent2WithoutHeader(htm, papersize)` (558) uses `@media print` + `@page`? (paper size param).
- `print.css`: `@media print` hides `#fixed-top`, modals, `.printBtn`, shows only target ids — a visibility-based print strategy; `.noteHeader` center header, `.tableBorder` collapse.
- QZ Tray (`qz-tray.js`): WebSocket `ws://localhost` handshake with SJCL/sha-256 signing (cert chain), streams: SERIAL/USB/HID/PRINTER/FILE/SOCKET; used for label/thermal printers. `BrowserPrint-2.0.0.75.min.js` (Zebra) + `JSPrintManager.js` as alternates.
- `printerSetting-ext.js`: `setDefaultSystemPrinter/setBarcodePrinter/getIsADotMatrixPrinter` in localStorage; QZ connection status UI (`displayPrinterConnectionStatus`).

### 4.8 Offline/mobile
- `w-api-call.js` supports cached responses (`cachedTime`, `cachedApiData`) — crude offline cache.
- `ohumlite/` (Cordova) ships reduced module set (appointments, tokens, ABDM, billing, members); `apps/offlinedashboard` for offline dashboards; `w-fetch-file.js` for file downloads.
- Mobile/iPad CSS separate files, orientation-specific media queries (§6.1).

---

## 5. Design-system evidence (from actual CSS/JS)

### 5.1 Color semantics
| Token | Hex | Evidence |
|---|---|---|
| Brand primary (teal-green) | `#01A080` / `#00715b` / `#10c39e` / `#1abb9c` | `.btn-success{background-color:#01A080;border-color:#10c39e}` (app.css); sidebar `.left_col{background:#00715b}` (navigation.css); active nav `border-right:5px solid #1abb9c` (navigation.css); brand greens throughout (`#005a48,#009b7b…`) |
| Success/Completed badge | `#bdffb4` | `.compNotify` (app.css) |
| Checked-in badge | `#f2f9fb` (light blue) | `.checkInNotify` (app.css) |
| Pending badge | `#fff4e3` (light amber) | `.pendingNotify`, `.onlineNotify` (app.css) |
| No-show badge | `#efeadf` (grey) | `.noshowNotify` (app.css) |
| Walk-in badge | `#faeffb` (light purple) | `.walkinsNotify` (app.css) |
| Emergency badge | `#ffddc9` (light orange) | `.emergencyNotify, .issuedNotify` (app.css) |
| Info links | `#59b0c3` (steel blue) | `.btn-blue-lg{border-color:#59b0c3;color:#59b0c3}` (app.css) |
| Error / required | Bootstrap `has-error`, `.asteriksCls` `*` | layer/default.json `errorClass: has-error`, `requiredHtml: <span class='asteriksCls'>*</span>` |
| Bed board colors | `row.colorCode` | bed-screen-building.html `extraCls = "background:" + row.colorCode` |

### 5.2 Typography & spacing
- Bootstrap 3 defaults (14px, `font-awesome.min.css` icons + **custom icon font** `icons.css`/`icon-style.css`: `icon-patient, icon-mrn, icon-phone_iphone, icon-date, icon-time, icon-doctor, icon-civil, icon-vehicle-number, icon-department, icon-building, icon-clinic, icon-nok1, icon-checkin, icon-appointment2, icon-admission_new, icon-billing_list, icon-medicine, icon-outpatient-pharmacy, icon-inventory1, icon-labs, icon-radiology, icon-operation, icon-kiosk`… used as field icons in schemas + dashboard tiles).
- `groupingTitle` h5 section headers with `data-translate` (walkinVisit-schema).
- 12-col grid; `colnopad`; `form-center`; `fwBtn` full-width; `btn49Perc`; margins via helper classes (`margin-top-18`).

### 5.3 Components
- Buttons: `btn btn-success fwBtn` (confirm), `btn btn-lg btn_wrap` (dashboard tiles w/ icon+text), `radio-btn`/`chk-btn` toggle groups (`data-toggle="buttons"`), `btn-primary pull-right` reset.
- Tables: `.table.table-striped.table-bordered.table-hover` (layer), `tableHeadFixer` sticky header, `.table-fixed`.
- Alerts: `alertDialog`, `printErrorMessageByErrorMsg`, `has-error` field-level.
- Tabs: Bootstrap `.nav-tabs` (`clearanceTab.html`, `bed-managment-*`, `orderSetMasterTabs.html`), `.classTab` selects styled as tabs.
- Modals: faug modal template (`layer/default.json` modal: header/body/footer, `isCloseAllwd`), used for nearly every sub-screen.
- Dashboard: `.thumbnail.thumbnailBorder` cards (genericMasters.html), tile buttons with icon+`selection_text_wrap`.
- Breadcrumbs: `bed-breadcrums.html` (commons).
- Navigation: Gentelella-style sidebar (`left_col`, `nav-sm` icon-only 70px vs expanded, `right_col` content), `masterNav.html`, `navigation.css`.

### 5.4 Responsive
- `mobile-responsive.css`: breakpoints 1366/768/992/736/620/667/568/375 px with orientations; `ipad-tablet-responsive.css`: 1024/768/600/1200 with orientations; `responsive.css`.
- Forms stack to single column (`col-xs-12`), tables scroll (`table-responsive`), footer pagination adapts.

### 5.5 Accessibility (evidence of gaps)
- No ARIA attributes in templates (`role='dialog'` on modals only; `role="tablist"` on classTab; no `aria-labelledby`, no focus management evidence).
- `tabindex="-1"` used to block focus on readonly date/time inputs (walkinVisit `appointmentDate`).
- Buttons often `<div class="btnNotify">` / `<li onclick>` (appointment-details.html:29-96) — non-semantic clickables.
- `alt="docit-img"` on preloader; images generally lack alt text.
- Labels: `.form-label` present but no `for`/`id` association evidence.
- Color-only status differentiation (badge backgrounds; bed `colorCode`).

### 5.6 i18n/format
- Global date formats in fjs-config.json globals (DD/MM/YYYY display, YYYY-MM-DD API), `CustomDateTimeFormat`, phone/pin validation rules, labels (`mrn_label: "HID"`, `nok_label`, `pin_code`).

---

## 6. UX problems (evidence) — input to §UX of design doc

| # | Problem | Evidence | Frequency |
|---|---|---|---|
| 1 | **Modal-heavy, nested flows** | Nearly all templates are modal fragments; `addBilling.html` + service rows + confirm modals; notes in modals; reasons in modals | Global |
| 2 | **Multi-click cascading selectors** | Appointment: Complaints→Dept→Building→Cabin→Doctor→Slot chain (walkinVisit namedEvents) | op, queue |
| 3 | **Dense single-form registration** | `walkinVisit-schema.json` ~60 fields incl. hidden/conditional; form width 90%; no wizard | op |
| 4 | **No explicit busy/error affordances beyond freeze overlay & alertDialog** | `freezeUi` overlays, `printErrorMessageByErrorMsg` (red text/alert) | Global |
| 5 | **Non-semantic interactive elements** | `<li onclick>`, `<div class="btnNotify">` clickable (appointment-details.html) | Global |
| 6 | **Client-side validation duplicated in JS per screen** | `validation.js` `validatePatientDetails()` checks business rules inline (NGO district rules etc.) | op, ip |
| 7 | **Long flat tables** | Service rows, patho worklists with `table-responsive` + sticky header only | billing, lab |
| 8 | **No keyboard-first support evidence** | no keydown handlers beyond blur/focus orientation hacks (`orientationChangeForFooterFocus`) | Global |
| 9 | **No undo/confirm on many destructive ops** | direct `deleteNhncService`, `cancelIndent` with only alert confirm | billing, inventory |
| 10 | **Route-based state, not URL-param state** | `Fg.getRoute()` string parsing (`rtparts[4]` in bed-screen-building.html) — deep links fragile | Global |
| 11 | **i18n partial** | many labels hard-coded English in templates despite `translateLang` | Global |

---

## 7. Open questions / unknowns
1. faug-min.js internals (minified) — schema feature surface not 100% documented; port by schema-inventory (1750 schemas) rather than engine analysis.
2. `print.server.url` — not found in public JS; printing is client-side (jQuery.print/QZ) — server print may exist in backend.
3. Exact set of 300 masters depends on server-side `mastersConfigration.json` (config file checked in, but master forms themselves are generic).
4. ABDM/ABHA flows (`abdm-module.js`, aadhaarauth.js) may be replaced wholesale by ABDM SDK (out of scope for UI design).
5. BCMA scanner hardware specifics (bcmaMedicationLogForScanner) — hardware API unknown.
6. Charting app (`ChartingApplication.js`) is a standalone canvas app on ODCWF JSONs — port scope needs its own deep-dive (recommend: agent for charting/partograph specifically).
7. `scheduler.js`, `tables.js`, `tiles.js`, `test.html`, `tiuxd/` folder — auxiliary/legacy experiments (tiuxd = TIUX designer?), unverified.

---

## 8. Supporting artifacts produced
- `api_inventory.txt` — 1,842 unique `api/rest/...` endpoints → owning JS file(s) (temp).
- `routes_inventory.txt` — 704 `#module:page` route references → source template(s) (temp).
- Screen inventory tables above; module/route/API counts verifiable by re-running the PowerShell extraction snippets in this doc's parent notes.
