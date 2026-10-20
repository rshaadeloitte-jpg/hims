# 09 — React Migration Design: iHMS / OHUM PAS NG

> Author: Agent 09 (REACT/UI MODERNIZATION ARCHITECT). Derived from primary analysis of the legacy
> jQuery/FAUG frontend at `D:\ihms\src\main\resources\public\` (evidence dump: `_discovery/agent-09/findings.md`).
> Scope: web (desktop/tablet) React SPA modernization; mobile/offline via legacy Cordova shells (ohumlite/queue/offlinedashboard) mapped to the same React core where feasible.
> Principles: **map legacy screens 1:1 first, never invent features, clinical speed first, cosmetic changes second.** Every route, component, hook, and API function below cites legacy evidence.

---

## 1. Target architecture

```
┌────────────────────────────────────────────────────────────────────┐
│  React 18 SPA (Vite)  ·  TypeScript strict  ·  feature-sliced dirs │
│  Shell: AppLayout (sidebar nav + top bar + breadcrumbs + printer   │
│         status) → route outlet → module pages                      │
├────────────────────────────────────────────────────────────────────┤
│  State: React Query (server cache)  ·  Zustand (session/global:    │
│          unit_id, duz, isNhnc, app_type_w, route flow counters,    │
│          printer prefs = legacy Fg.g()/localStorage equivalents)   │
│  Forms: react-hook-form + zod (schema JSON → typed resolver)       │
│  Router: react-router v6 with legacy hash-compatible paths         │
├────────────────────────────────────────────────────────────────────┤
│  API client: one generated module per legacy api/rest/<resource>   │
│  family (1,842 endpoints) — typed fetch wrappers with              │
│  X-Request-ID idempotency + optional TTL cache (legacy w-api-      │
│  call.js semantics), auth header, error envelope normalization     │
├────────────────────────────────────────────────────────────────────┤
│  Cross-cutting: PatientSearch, PatientBanner, DataTable,           │
│  ClinicalForm (schema-driven), ApprovalPanel, PrintService,        │
│  SignatureCapture, VitalRangeSlider, Partograph, GrowthChart,      │
│  ScheduleBoard, QueueBoard, BedBoard, FileUpload/Annotate          │
├────────────────────────────────────────────────────────────────────┤
│  Design system: design-tokens.css (teal brand, status palette) +   │
│  component library (shadcn/ui-style primitives themed to legacy)   │
└────────────────────────────────────────────────────────────────────┘
        │            │              │
  Legacy REST       QZ Tray        BrowserPrint/print CSS
  api/rest/*        (labels,       (thermal receipts —
                    thermal)        visibility-based @media print)
```

### 1.1 Routing strategy
Legacy routes are `#module:page` hash routes (`fjs-config.json` setting `enableRouteModules: true`; route inventory = 533 distinct definitions (704 refs / ~290 non-master distinct +300 generic masters)). React maps them 1:1:

- `/m/:module/:page/:params*` — one route per legacy page (e.g. `/m/op/addAppointment`, `/m/masters/unitmaster/view`).
- Read the **route list** from a generated `routes.generated.ts` produced from template scan (see findings §1.1) so no legacy page is lost.
- Legacy in-page params (`rtparts[4]` in `bed-screen-building.html`) become real URL params — fixes fragile deep links (UX problem #10).
- Lazy-load by module (`React.lazy`) — each module folder mirrors a legacy fjs-config.

### 1.2 Legacy page → React composition rule
Every legacy "page" = **schema(s) + template fragment(s) + controller functions**. The mapping contract:

| Legacy | React |
|---|---|
| schema `details.form` (create/update) | Page component + `ClinicalForm` with `useSchemaForm` |
| schema `crud` ops (`c/u/d/r/ra/<named>`) | API client functions + `useSchemaCrud` hook |
| schema `events` chains / `namedEvents` | declarative effect chains (`useSchemaEffects`) |
| `callbacks.afterDraw.controlEvents` (jqselector wiring) | hook per interaction (e.g. `usePatientLookupField`) |
| template fragment | React component (fragment components are composed, not pages) |
| controller functions (e.g. `selectFirstDepartment`, `showDoctorForCabin`) | pure functions/selectors in `useWalkinVisit`-style hooks |
| viewer (`details.viewer` + `crud.ra`) | `DataTable` with `useServerTable` |
| viewer template rows (`showPatientWorkList.json`) | cell/row render props |

---

## 2. Screen-to-React mapping (full)

Conventions: **P** = page component, **F** = feature component (module-level), **C** = reusable component (cross-cutting, §3), **H** = hook, **API** = client function. Route column = React path suffix (module prefix `/m/<module>`).

### 2.1 OP (Outpatient / Appointment) — legacy `fjs-config_op.json`, 43 schemas, 73 templates

| Legacy screen (evidence) | React route | Page | Components | Hooks | API functions (legacy endpoint) |
|---|---|---|---|---|---|
| Walk-in registration (walkinVisit-schema.json; walkinVisit templates) | `/m/op/walkinVisit` | `WalkinVisitPage` | `C:PatientSearch`, `F:MLCDetailsSection`, `F:ReferralSection`, `F:AmbulanceSection`, `F:SponsorSection` | `useWalkinVisit` (validatePatientDetails port), `usePatientLookup` | `walkinVisit.add` (POST `api/rest/walkinVisit/addwalkinVisit`), `walkinVisit.searchToken` (GET `…/searchToken`), `patient.getGeneralDetailsForSearch` (POST `api/rest/patient/getPatientGeneralDetailsListForSearch`) |
| Appointment add (addAppointment.html; appointmentNewForm; `addpatientRegApptdto_schema.json`, `minimal-walkin-appointment.json`) | `/m/op/addAppointment` | `AddAppointmentPage` | `F:AppointmentWizard`, `C:DoctorSelector`, `C:DepartmentSelector`, `C:CabinSelector`, `F:SlotPicker` (dateTimeIconSlotView1/2 → `DoctorAppointmentSlots.getSlotsWithoutDoctor`) | `useAppointmentSlotCascade`, `useDoctorFilter` | `appointmentschedule.getDepartmentsByDate` (POST), `appointmentschedule.getDayWiseBuildingMasterByDepartmentId` (GET), `appointmentschedule.getCabinWiseApointmentSlotCount`, `walkinVisit.getAvaliableCabinAndBookedDoctorIds`, `walkinVisit.addwalkinVisit` |
| Appointment list (appointment_list-schema.json; appointment-details.html badges; appointment-list-module.js) | `/m/op/appointmentList` (+ `appointmentListOnline`, `appointmentListNew`) | `AppointmentListPage` | `F:AppointmentCategoryStrip` (All/CheckedOut/CheckedIn/Pending/NoShow/Walkins/Emergency/Online/Teleconsult/FPV/RGHS — legacy `btnNotify` badges), `F:AppointmentTable`, `F:AppointmentActions` (reschedule/cancel with reason modals: apptRescheduleReason.html, apptCancelReason.html) | `useAppointmentList` (per-category server paging), `useAppointmentActions` | `appointment.*` (16 refs), `appointmentList` read-all ops |
| Doctor check-in (doctor-cabin-checkin.html, doctor-cabin-checkin-list.html, doctorScheduleWrap.html; `showDoctorOpSchedule.json`) | `/m/op/doctorCheckIn` | `DoctorCheckinPage` | `F:DoctorList`, `F:CabinStatusGrid`, `C:ScheduleBoard` | `useDoctorCheckin` (checkinCheckout) | `doctor.checkinCheckout` (PUT `api/rest/doctor/checkinCheckout?isCheckin=…`), `master.doctorFilterFilter` (GET) |
| Doctor schedule (showDoctorOpSchedule.json, showDoctorFutureAppointmentSchedule.json; single-doctor.html) | `/m/op/mySchedule`, `/m/op/showDepartmentSlot` | `DoctorSchedulePage` | `C:ScheduleBoard`, `C:DoctorSelector` | `useDoctorSchedule` | `appointmentschedule.getAppointmentScheduleNew?type=D` (POST), `otscheduler.*` shared |
| Patient detail / facesheet (showPatientDetailSchema.json, showPatientHistorySchema.json, patientHistorySchema.json, patientMedicalRecordSchema.json; PatientAllHistory.html) | `/m/op/patientDetail`, `/m/op/patientHistory` | `PatientDetailPage`, `PatientHistoryPage` | `C:PatientBanner`, `F:VisitHistoryTimeline`, `F:PrescriptionHistory` | `usePatientDetail`, `usePatientHistory` | `patient.getPatient` (GET), `nursingList.getPatientEncounterById` |
| Follow-up visit requests (followupVisitReq_list-schema.json, followupVisitReq-detail.html) | `/m/op/followUpVisitReqList` | `FollowUpRequestsPage` | `F:RequestTable`, `F:RequestDetail` | `useFollowUpRequests` | `walkinVisit.*`/`patientTransferDischarge.*` followup ops |
| Consult orders (consultOrder_list-schema.json, consultationPackageDetails.json) | `/m/op/consultOrdersList` | `ConsultOrdersPage` | `C:DataTable`, `F:OrderActions` | `useConsultOrders` | `serviceOrder.*` (36 refs) |
| Investigation list (investigationSchema.json) | `/m/op/investigation` | `InvestigationPage` | `C:DataTable` | `useInvestigation` | `pathoOrder.*` booking list |
| Camp registration (opCampRegistration.html, opCampRegistrationRow.html) | `/m/op/campRegistration` | `CampRegistrationPage` | `F:CampPatientGrid`, `F:CampRowForm` | `useCampRegistration` | `patientRegAddmission/patientRegAppt.*` camp ops |
| Follow-up visit creation (visitCreationAndMedicalDispensing.html, followup-dept cascade) | `/m/op/followupVisit` | `FollowUpPage` | `C:PatientSearch`, `F:FollowUpForm`, `F:PrescriptionQuick` | `useFollowUp` | `pharmacy.commonVisitCreationAndMedicineDis` |
| MLC details (mlcDetailsSchema.json, updateMLCDetailsSchema.json, addMlcDetailsSchema.json) | `/m/op/mlcDetails` | `MLCDetailsPage` | `F:MLCForm`, `C:AuditHistory` | `useMlcDetails` | `walkinVisit.mlc*` |
| Doctor assessment (doctorAssessment route; addPatientIntialAssesment.html) | `/m/op/doctorAssessment` | `DoctorAssessmentPage` | `F:InitialAssessmentForm` | `useAssessment` | `nursingList.addAssessment`-family |
| Appointment upload / manage slots (manageQueue.html, uploadAppointments route) | `/m/op/uploadAppointments`, `/m/op/manageDoctorSlot` | `SlotAdminPage` | `F:SlotUpload`, `C:ScheduleBoard` | `useSlotAdmin` | `appointmentschedule.*` |
| ABHA/ABDM patient profile (abhaPatientProfileList.html, abhaProfile.html, abhaConsent.html; abdm-module.js) | `/m/op/abha` | `AbhaPage` | `F:AbhaRegister`, `F:AbhaProfileCard`, `F:AbhaConsent` | `useAbha` (OTP flows) | `abha.*` (35 refs) |
| Kiosk (registrationOpCard.html, showDepartmentsApptSlots.html) | `/m/kiosk` | `KioskPage` | `F:SelfRegistration`, `F:SlotPick` | `useKiosk` | `appointmentschedule.*`, kiosk print |
| Queue token flow (queueEvtCls stopAndStartCounterQueue; ohumlite token-flow) | `/m/op/queueCounter` | `QueueCounterPage` | `F:TokenBoard`, `C:QueueBoard` | `useTokenFlow` | `queueManagment.*` (12 refs) |
| NHNC variants (walkinVisitNhnc-schema.json, showAppointment-list-nhnc.html, nhncAppointment routes) | `/m/op/nhncAppointment(List)` | `NhncAppointmentPage` (reuses OP pages with `isNhnc` feature flag) | same as above | `useNhncRules` (validation.js district/foreigner rules) | same + `nhcx.checkEligibility` |

### 2.2 IP (Inpatient / Admission) — `fjs-config_ip.json`, 65 schemas, 120 templates

| Legacy screen | React route | Page | Components | Hooks | API |
|---|---|---|---|---|---|
| Admission order (addpatientAdmissiontdto_schema.json, emergency variant; admission-confirmation.html) | `/m/ip/admissionOrder`, `/m/ip/admissionOrderEmer` | `AdmissionOrderPage` | `C:PatientSearch`, `F:AdmissionForm`, `F:BedPicker`, `F:SponsorPanel`, `F:EmergencyPanel` | `useAdmissionOrder` | `patientRegAddmission.*` (16 refs), `ipdadmission.*` (11 refs) |
| Admission list (admission_list-schema.json, admissionRequest_list-schema.json) | `/m/ip/admissionList`, `/m/ip/admissionReqList` | `AdmissionListPage` | `C:DataTable`, `F:AdmissionActions` | `useAdmissionList` | `patientRegAddmission.list*` |
| Bed management (bed_management_schema.json; bed-managment-building/department.html) | `/m/ip/bedManagement` | `BedManagementPage` | `C:BedBoard` (building→dept→ward tabs, colorCode beds), `F:BedBlockModal`, `F:BedTransferDialog` (bed-transfer-*.html) | `useBedBoard`, `useBedTransfer` | `bedmanagement.*` (22 refs) |
| Bed screen (bed_screen_schema.json; bed-screen-building/department.html) | `/m/ip/bedScreen` | `BedScreenPage` | `C:BedBoard` (read-only view) | `useBedScreen` | `bedmanagement.*` |
| Bed reservation (bedReservationdto_schema.json, addBedReservation.html) | `/m/ip/bedReservation`, `/m/ip/unReservedBedList` | `BedReservationPage` | `F:ReservationForm`, `C:BedBoard` | `useBedReservation` | `bedmanagement.reserve*` |
| Transfer requests (transferRequest_list-schema.json, bedTransferSchema.json, bedTransferRequestSchema.json) | `/m/ip/transferReqList` | `TransferRequestsPage` | `C:DataTable`, `F:TransferDialog` | `useTransferRequest` | `bedmanagement.transfer*` |
| Discharge (dischargePatientSchema.json, dischargeUpdateStatus.json, discharge_clearance_schema.json, clearanceTab.html, dischargeSummary templates) | `/m/ip/discharge`, `/m/ip/updateDischargeStatus` | `DischargePage` | `F:ClearanceTabs`, `F:DischargeSummary`, `F:BillCheckPanel`, `C:ApprovalPanel` | `useDischarge` (clearance checklist state machine) | `patientTransferDischarge.*` (24 refs) |
| Discharged patients (showDischargedPatientSchema.json) | `/m/ip/dischargedPatients` | `DischargedListPage` | `C:DataTable`, `F:ReadmissionAction` | `useDischargedPatients` | `patientTransferDischarge.listDischarged*` |
| Clearance / MLC clearance (getAdmissionClearanceList.json, mlcClearanceFilters.json, mlcList.html) | `/m/ip/clearanceListFlow`, `/m/ip/MlcclearanceListFlow` | `ClearancePage` | `F:ClearanceTabs`, `F:MlcClearanceTable`, `C:ApprovalPanel` | `useClearance` | `patientTransferDischarge.clearance*` |
| Attendant pass (attendantPassSchema.json; attendantPass*.html + label) | `/m/ip/attendantPass` | `AttendantPassPage` | `F:PassForm`, `F:PassHistory`, `C:PrintDialog` (label) | `useAttendantPass` | `patientTransferDischarge.attendantPass*` |
| Mother-child linking (motherChildLinkingSchema.json) | `/m/ip/motherChildLinking` | `MotherChildLinkPage` | `F:MotherChildPairing` | `useMotherChildLink` | `patientTransferDischarge.motherChild*` |
| Cradles (showAllCradlesSchema.json, allCradlesWrap.html) | `/m/ip/showAllCradles` | `CradlesPage` | `F:CradleGrid`, `C:BedBoard` variant | `useCradles` | `bedmanagement.cradles*` |
| Nursing orders (nursingOrderSchema.json, addNursingOrder.html) | `/m/ip/nursingOrder` | `NursingOrderPage` | `F:OrderComposer` (reuses nursing order rows) | `useNursingOrder` | `serviceOrder.addNursingOrder*` |
| Interim admissions (interimAdmissions.json, interimAdmissionFilterwrap.html) | `/m/ip/showInterimAdmissions` | `InterimAdmissionsPage` | `C:DataTable`, `F:PromoteToAdmission` | `useInterimAdmissions` | `ipdadmission.interim*` |
| TID list (tidListSchema.json, showTidList route) | `/m/ip/showTidList` | `TidListPage` | `C:DataTable` | `useTidList` | `ipdadmission.tid*` |
| OT bed management / nursing station (routes) | `/m/ip/otBedManagement`, `/m/ip/nursingStation` | `OtBedPage`, `NursingStationPage` | `C:BedBoard`, `F:WardStationPanel` | `useOtBed` | `bedmanagement.*` |
| Bed block (bedBlock.html) | `/m/ip/bedBlock` | `BedBlockPage` | `F:BlockForm` | `useBedBlock` | `bedmanagement.block*` |

### 2.3 Billing — `fjs-config_billing.json`, 77 schemas, 93 templates

| Legacy screen | React route | Page | Components | Hooks | API |
|---|---|---|---|---|---|
| Billing flow (addBillingSchema.json, addOpIpBillingSchema.json, addIpBillingSchema.json; addBilling.html, addServiceRow.html/Ipd) | `/m/billing/billingFlow`, `/m/billing/opIpBillingFlow`, `/m/billing/ipBillingFlow` | `BillingPage` (patient→service lines→confirm) | `C:PatientSearch`, `F:ServiceLineEditor` (in-row editable rows), `F:SponsorPanel`, `F:PackagePanel`, `F:ProcedurePanel`, `C:ConfirmationDialog` | `useBilling` (startBillingFlow/addToServiceTable/confirmServices ports), `useServiceCatalogue` | `bill.*` (16), `serviceOrder.*`, `packageservice.*` (17) |
| Bill estimation (billEstimatiomSchema.json, addPaymentEstimationAdvanceSchema.json; billEstimationWrap.html, addEstimationProcedureRow.html) | `/m/billing/billEstimationFlow` | `BillEstimationPage` | `F:EstimateLines`, `F:EstimateDetail`, `C:ApprovalPanel` (approved amounts) | `useBillEstimation` (billEstimationFunctionality.js ports: calculateTotalBillAmount) | `billEstimation.*`/`serviceListForBillEstimationSchema` ops |
| Billing list (getAllBills-schema.json, billSummarySchema.json, getAllEstimationBillSchema.json) | `/m/billing/billingListFlow` | `BillingListPage` | `C:DataTable`, `F:BillSummaryDrawer` | `useBillList` | `bill.getAllBills*` |
| Discharge billing (getDischargeList-schema.json) | `/m/billing/dischargeList` | `DischargeBillingPage` | `C:DataTable`, `F:BillSummary` | `useDischargeBilling` | `bill.dischargeList*` |
| Advance payment (addAdvanceSchema.json, advancePaymentListSchema.json; addAdvancePaymentList.html, addAdvanceView.html) | `/m/billing/advance` | `AdvancePage` | `F:AdvanceForm`, `F:AdvanceList`, `C:PrintDialog` (advance receipts) | `useAdvance` | `billReceipt.advance*`, `advanceReceiptByReceiptNoSchema` |
| Advance refund (advance_refund.json, advanceRefundReceipt.json, return_advance.json) | `/m/billing/advanceRefund` | `AdvanceRefundPage` | `F:RefundForm`, `F:RefundReceipt` | `useAdvanceRefund` | `billReceipt.refund*` |
| Receipt list (allReceiptsSchema.json, billReceiptSchema.json; allReceipts-row.html) | `/m/billing/receiptList` | `ReceiptListPage` | `C:DataTable`, `F:ReceiptView`, `C:PrintDialog` | `useReceiptList` | `billReceipt.*` (13) |
| Credit notes (creditNoteSchema.json, getAllCreditNoteListSchema.json) | `/m/billing/creditNoteBillingListFlow` | `CreditNotePage` | `F:CreditNoteForm`, `C:DataTable` | `useCreditNote` | `bill.creditNote*` |
| Doctor share (doctorShareSchema.json, doctorShareBillSchema.json, addDeductDoctorShareSchema.json, doctorBillListSchema.json) | `/m/billing/doctorShare`, `/m/billing/doctorShareBill`, `/m/billing/doctorShareAddDeduct` | `DoctorSharePage` | `F:DoctorShareTable`, `F:AdjustDialog` | `useDoctorShare` | `bill.doctorShare*` |
| Sponsor view (showSponsorSchema.json, changeSponsorSchema.json) | `/m/billing/showSponsor` | `SponsorPage` | `F:SponsorList`, `F:SponsorChange` | `useSponsor` | `bill.sponsor*`, `showSponsorSchema` ops |
| OT billing (addOTBillEstimationSchema.json, addOTBillingSchema.json; addItemRowOTBilling.html) | `/m/billing/otBillFlow` | `OtBillingPage` | `F:OtServiceLines` | `useOtBilling` | `otconfig.otBilling*` |
| Package subscription (addPackageSubscriptionSchema.json, package-order-list-schema.json, packageListForBillSchema.json, package_appointment_list-schema.json) | `/m/billing/packageSubscriptionFlow`, `/m/billing/packageAppointmentList` | `PackageSubscriptionPage` | `F:PackagePick`, `F:PackageDetail`, `F:HealthPlanConsumption` (healthplanConsumptionDetailSchema.json) | `usePackageSubscription` | `packageservice.*` (17) |
| Cross order service (crossServiceOrderSchema.json) | `/m/billing/crossOrderService` | `CrossOrderPage` | `F:CrossOrderComposer` | `useCrossOrder` | `serviceOrder.crossOrder*` |
| IP bill summary (ipBillSummarySchema.json, cancelIpBillSummarySchema.json, partialPayBillSchema.json, ipBillPaymentSchema.json, iPDBillReceiptSchema.json) | `/m/billing/ipBillSummary` | `IpBillSummaryPage` | `F:BillSummary`, `F:PartialPay`, `C:PrintDialog` (IPD receipt) | `useIpBillSummary` | `bill.ipBill*` |
| OP bill payment (opBillPaymentSchema.json, pharmacyBillRecSchema.json, billCumAdvanceRefundReceipt.html) | `/m/billing/opBillPayment` | `OpBillPaymentPage` | `F:PaymentPanel`, `C:PrintDialog` | `useOpBillPayment` | `bill.opBillPayment*` |

### 2.4 Pharmacy — `fjs-config_pharmacy.json`, 39 schemas, 143 templates

| Legacy screen | React route | Page | Components | Hooks | API |
|---|---|---|---|---|---|
| IP order verification/dispense (addMedicationSchema.json, getPrescriptionDetailsSchema.json, itemSaleDetailSchema.json; addMedication.html, batchRow.html, dispenseDetailRow.html) | `/m/pharmacy/pharmacyOrderVerify`, `/m/pharmacy/pharmacyOrderDispense` | `PharmacyOrderPage` | `C:PatientBanner`, `F:OrderLines`, `F:BatchPicker` (scan batch barcode), `F:DispensePanel` | `usePharmacyOrder` (showOrdersFunc, populateIPOrders, showIVOrders ports) | `pharmacy.*` (88 refs), `medication.*` (18) |
| OP pharmacy (opPharmacyAddPatient.json, addMedicationOp.html, addPatientPrescMedSchema.json, pharmacyCounterSaleSchema.json) | `/m/pharmacy/opPharmacyFlow`, `/m/pharmacy/pharmacyCountersale` | `OpPharmacyPage` | `C:PatientSearch`, `F:PrescriptionLines`, `F:CounterSale` | `useOpPharmacy` | `pharmacy.opDispense*`, `pharmacy.pharmacyCounterSale*` |
| Cart-fill (cartFillDetails.html, cartFillOrderDetails.html, cartFillPatientDetails.html) | `/m/pharmacy/cartFillFlow` | `CartFillPage` | `F:CartPatientList`, `F:CartOrderDetail`, `C:BarcodeScannerField` | `useCartFill` (scanBarcodeCartFill) | `pharmacy.cartFill*` |
| Compound prep (compoundDetailRow.html, compoundingGuidlines.html, compoundingLable.html, prepareCompound route) | `/m/pharmacy/prepareCompound` | `CompoundPrepPage` | `F:CompoundEditor`, `C:PrintDialog` (label) | `useCompoundPrep` | `pharmacy.compound*` |
| IV pharmacy (bcmaIV.html, bcmaIVPWardStock.html; ivPharm route) | `/m/pharmacy/ivPharm` | `IvPharmacyPage` | `F:IVBagList`, `F:IVChronology` | `useIvPharmacy` | `pharmacy.iv*` |
| IP returns (ipReturnPharmFlow route, startIPReturnFlow) | `/m/pharmacy/ipReturnPharmFlow` | `IpReturnPage` | `F:ReturnLines`, `F:ReturnReasonPanel` | `useIpReturn` | `pharmacy.iPReturnFlow*` |
| Prescription lists (prescriptionListSchema.json, prescriptionUploadSchema.json, showNursingPrescripListSchema.json, tempPrescriptionList.json) | `/m/pharmacy/PresciptionList`, `/m/pharmacy/prescriptionList`, `/m/pharmacy/NursePrescriptionList`, `/m/pharmacy/prescriptionUpload` | `PrescriptionListPage` | `C:DataTable`, `F:PrescriptionDetail`, `F:PrescriptionUpload` | `usePrescriptionList` | `pharmacy.prescription*`, `addPrescriptionSchema` ops |
| Drug mapping (pharmacyDrugMapping route; dispensingtype.json) | `/m/pharmacy/pharmacyDrugMapping` | `DrugMappingPage` | `F:MappingEditor` | `useDrugMapping` | `pharmacy.drugMapping*` |
| Item sales & returns (itemSaleListSchema.json, itemSaleReturnListSchema.json, itemSaleReturnDetailsSchema.json, pharmacyItemSaleRetrunBIllReciptSchema.json) | `/m/pharmacy/itemSaleList`, `/m/pharmacy/itemSaleReturnList` | `ItemSalePage`, `ItemSaleReturnPage` | `C:DataTable`, `F:SaleDetail`, `C:PrintDialog` | `useItemSale`, `useItemSaleReturn` | `pharmacy.itemSale*` |
| Pharmacy billing (opIpPharmacyBillSchema.json, ipPharmacyBillingSchema.json, pharmacyPaymentDetailSchema.json, pharmacyPaymentForCounterSchema.json, pharmacyPaymentReturnSchema.json) | `/m/pharmacy/opIpPharmacyBilling`, `/m/pharmacy/pharmacyBilling` | `PharmacyBillingPage` | `F:BillLines`, `F:PaymentPanel` | `usePharmacyBilling` | `pharmacy.bill*` |
| Interventions (pharmacyInterventionReport.json, showIntervFunc) | `/m/pharmacy/pharmInterventionreport` | `InterventionReportPage` | `C:DataTable`, `F:InterventionForm` | `useIntervention` | `pharmacy.intervention*` |

### 2.5 Nursing — `fjs-config_nursing.json`, 28 schemas, 371 templates

| Legacy screen | React route | Page | Components | Hooks | API |
|---|---|---|---|---|---|
| Role-nurse home/worklist (showNurseWorklistSchema.json, roleNurseWorkList*.html) | `/m/nursing/roleNurse`, `/m/nursing/roleNurseIncharge` | `NurseWorklistPage` | `F:WardSelector`, `F:PatientTaskList` (patient-wise/task-wise), `F:GoalsPanel`, `C:DataTable` | `useNurseWorklist` (getRoleNursePatientList, getAllIPPatientFilteredList) | `nursingList.*` (274 refs — largest family) |
| Patient coversheet (showPatientCoversheetSchema.json; loadCoverSheetDetails) | `/m/nursing/roleNursePatientView` | `NursePatientViewPage` | `C:PatientBanner`, `F:VitalsPanel`, `F:OrdersPanel`, `F:NotesPanel`, `F:MedsPanel` (tile matrix → chartingTiles pattern) | `usePatientCoversheet` | `nursingList.coversheet*`, `showPatientCoversheetSchema` ops |
| Notes entry (addNursesNotes.html, addNewNote.html, notesTitle.json, noteSummary.html, showNoteDetails.html) | `/m/nursing/rolePatientNote` | `NotesPage` | `F:NoteTiles`, `F:NoteEditor` (tinyMCE→tiptap), `C:SignatureCapture`, `F:AddendumPanel` (addendumAgainstSignedCarePlanNote.html) | `useNotes` (sign/verify flows) | `nursingList.notes*`, `nursingList.sign*` |
| Plan of care (ncpSubProblems.html, carePlanDetailsConfirm.html, rolePlanofCare route) | `/m/nursing/rolePlanofCare` | `PlanOfCarePage` | `F:ProblemList`, `F:GoalEditor`, `F:InterventionEditor` | `usePlanOfCare` (loadncpProblems) | `nursingList.ncp*`, `careplan.*` |
| Ordering set (orderingSet route; addAdtOrder/addConsultOrder/addDietOrder/addLabOrder/addMedOrder/addNursingOrder/addProcedureOrder/addRadOrder.html family) | `/m/nursing/orderingSet` | `OrderingPage` | `F:OrderComposer` (ADT/consults/diet/lab/med/nursing/procedure/rad tabs) | `useOrderComposer` | `serviceOrder.add*Order*`, `nursingOrderSchema` ops |
| Discharge (discharge route; addExpectedDischargeDate.html, addAllDiagnosisNhnc.html) | `/m/nursing/discharge` | `NursingDischargePage` | `F:DischargePrep`, `F:DiagnosisPanel` | `useNursingDischarge` | `patientTransferDischarge.*` |
| Transfer (transfer route; nursingBedTransfer.html) | `/m/nursing/transfer` | `NursingTransferPage` | `F:TransferDialog` | `useNursingTransfer` | `bedmanagement.transfer*` |
| Mortuary (mortuarySchema.json, mortuaryBoxManagement.json, mortuaryClearanceSchema.json, mortuaryDischargedPatientSchema.json) | `/m/nursing/mortuary`, `/m/nursing/showMortuary` | `MortuaryPage` | `F:MortuaryBoxGrid`, `F:BoxAllocation`, `F:ClearancePanel` | `useMortuary` | `nursingList.mortuary*` |
| Incident reporting (incidentReportSchema.json, incidentResolveSchema.json, addIncidentReport.html, incidentReporting route) | `/m/nursing/incidentReporting`, `/m/nursing/addnewIncident` | `IncidentReportPage` | `F:IncidentForm`, `F:IncidentResolve`, `C:AuditHistory` | `useIncidentReport` | `nursingList.incident*` |
| Complaint/Diagnosis mapping (ComplaintDiagnosis, compliantDiagnosisMap, DiagnosisTreatment routes) | `/m/nursing/ComplaintDiagnosis` | `DiagnosisMapPage` | `F:MapEditor` | `useDiagnosisMap` | `mdm.*` (32 refs) |
| Annotation (annotation route; annotation-view.js) | `/m/nursing/annotation` | `AnnotationPage` | `C:ImageAnnotation`, `C:FileUpload` | `useAnnotation` | `fileService.*` |
| Rostering (shiftRoster-Schema.json, addShiftDetails-Schema.json, showIpdRosterSlotsSchema.json, roleNurseRostering route) | `/m/nursing/roleNurseRostering` | `RosteringPage` | `F:ShiftCalendar`, `F:SlotAllocation` | `useRostering` | `master.shiftRoster*`, `addIpdRosterSlotsSchema` ops |
| Allied health (alliedHealth-module.js, roleAlliedPatientView route) | `/m/nursing/roleAlliedPatientView` | `AlliedHealthPage` | `C:PatientBanner`, `F:TherapySessions` | `useAlliedHealth` | `nursingList.allied*` |
| TIUXML clinical forms (tiuxml.js engine; tiuxmls 368 schemas; NURSINGINITIALID_final_V1_0_P.html etc.) | `/m/nursing/tiuxml/:formId` | `TiuxmlFormPage` | `F:TiuxmlRenderer` (tabs, indented elements, boilerplate text, one-item radio groups — port of processTemplate) | `useTiuxmlForm` | `tiuxml.*` save/load |
| Shift handover (shifthandover templates; patientDetailsForMultiShiftHandover.html etc.) | `/m/nursing/shiftHandover` | `ShiftHandoverPage` | `F:PatientMultiSelect`, `F:HandoverSections` (meds/labs/rad/procedure/transfer with done/due lists) | `useShiftHandover` | `nursingList.handover*` |

### 2.6 Pathology — `fjs-config_pathology.json`, 57 schemas, 54 templates

All stages share the pattern **FilterBar → WorkList → Detail** (pathoFilterSchema, pathoReportDetailsSchema; patho-modules.js loadPathoFilters/loadXResults).

| Legacy screen | React route | Page | Components | Hooks | API |
|---|---|---|---|---|---|
| Sample collection (sampleCollectSchema.json, pathoGenerateBarCodeSchema.json, patientWorkListWrap.html) | `/m/pathology/sampleCollection` | `SampleCollectionPage` | `F:PathoFilterBar`, `C:DataTable`, `F:BarcodePrint` (printSingleBarCode.html), `F:SampleDrawForm` | `useSampleCollection` | `pathoOrder.*` (60 refs) |
| Accept/reject (acceptRejectSchema.json + filters/details) | `/m/pathology/sampleAcceptReject` | `SampleAcceptRejectPage` | `F:AcceptRejectTable`, `F:RejectReasonPanel` (sampleRejectionSchema.json) | `useAcceptReject` | `pathoOrder.acceptReject*` |
| Result entry (pathoResultSchema.json, resultingReportSchema.json, resultingDetailSchema.json, parameterResult.html, microbiology-schema.json, pathoResultTemplate.html, addSentivityResultRow.html) | `/m/pathology/sampleResult` | `SampleResultPage` | `F:ResultTemplateRenderer` (parameter/text/microbiology variants), `F:PreviousResults` (lastResult.html), `F:CriticalFlag` | `useResultEntry` (loadPathoResult) | `pathoOrder.result*`, `pathoOrder.pathoResultTemplate*` |
| Result review (resultReviewSchema.json + details; sampleResultReview route) | `/m/pathology/sampleResultReview` | `ResultReviewPage` | `F:ReviewTable`, `F:ResultDiffView`, `C:ApprovalPanel` | `useResultReview` | `pathoOrder.resultReview*` |
| Report release (reportReleaseSchema.json + details; labReportReleaseConfirmation.html, reportReleaseTemplateReport.html) | `/m/pathology/sampleReportRelease` | `ReportReleasePage` | `F:ReleaseTable`, `C:SignatureCapture` (first/second verification), `C:PrintDialog` (plainText/pathoParameterReport.html etc.) | `useReportRelease` | `pathoOrder.reportRelease*` |
| Machine-wise review (machineWiseResultReviewSchema.json; KDL machine interface) | `/m/pathology/sampleMachineWiseResult` | `MachineWiseReviewPage` | `F:MachineResultTable`, `F:ResultSyncPanel` | `useMachineWiseReview` | `pathoOrder.machineWise*`, `pathoKdlVerify.json` ops |
| Critical alerts (criticalAlertResultSchema.json + details) | `/m/pathology/sampleCriticalAlert` | `CriticalAlertPage` | `F:AlertList`, `F:AlertAction` | `useCriticalAlert` | `pathoOrder.criticalAlert*` |
| Workload (showLabLoadListSchema.json, showPatientWorkList.json) | `/m/pathology/workLoadList` | `WorkloadPage` | `C:DataTable`, `F:LoadSummary` | `useWorkload` | `pathoOrder.labLoad*` |
| Test status (pathoShowTestStatusNew.json, labTestStatusListNew.html) | `/m/pathology/showStatus` | `TestStatusPage` | `C:DataTable` | `useTestStatus` | `pathoOrder.testStatus*` |
| Grouping & test master (pathoGroupingAndTest.json, pathosubtestmaster.json) | `/m/pathology/groupingAndTest` | `GroupingTestPage` | `F:GroupTree` (jstree→tree), `F:TestMasterEditor` | `useGroupingTest` | `pathoOrder.grouping*`, `pathosubtestmaster` ops |
| Booking (pathoSchema.json, addServiceSchema.json, patho-booking-module.js) | `/m/pathology/lab` | `LabBookingPage` | `C:PatientSearch`, `F:TestOrderLines` | `useLabBooking` | `pathoOrder.booking*` |
| Order status update (pathoUpdateOrderStatus.json, serviceStatusSchema.json, updateOrderStatus route) | `/m/pathology/updateOrderStatus` | `OrderStatusPage` | `F:StatusUpdateTable` | `useOrderStatus` | `pathoOrder.updateStatus*` |
| Print all reports (printAllReport route) | `/m/pathology/printAllReport` | `PrintReportsPage` | `C:PrintDialog` (showAllLabReport.html) | `usePrintReports` | `pathoOrder.printReports*` |
| COVID lab list (pathoShowCovidStatus.json, showCovidLabList route) | `/m/pathology/showCovidLabList` | `CovidLabPage` | `C:DataTable` | `useCovidLab` | `pathoOrder.covid*` |

### 2.7 Radiology — `fjs-config_radiology.json`, 9 schemas, 6 templates

| Legacy screen | React route | Page | Components | Hooks | API |
|---|---|---|---|---|---|
| Radiology list/booking (radFilterSchema.json, patientFilterRadSchema.json, radOrderBookingSchema.json, radOrderDetail.html) | `/m/radiology/radlab` | `RadiologyPage` | `F:RadFilterBar`, `C:DataTable`, `F:BookingForm`, `F:Reschedule` (radRescheduleSchema.json), `F:Retest` (radRetestSchema.json) | `useRadiology` (shares pathology filter hooks) | `pathoOrder.rad*` / `radiology.*` |
| Report entry (radReportSchema.json, radReportDetailsSchema.json, radioFinalizeTemplate.html, pathoRadioTemplate.html) | `/m/radiology/radResult` | `RadResultPage` | `F:TemplateRenderer`, `F:ImageUpload` (showRadSampleImg.html) | `useRadResult` | `pathoOrder.radReport*` |
| Print report (printReport route, showRadiologyReport.html) | `/m/radiology/printReport` | `RadPrintPage` | `C:PrintDialog` | `useRadPrint` | `pathoOrder.radPrint*` |

### 2.8 Inventory — `fjs-config_inventory.json`, 70 schemas, 196 templates

| Legacy screen | React route | Page | Components | Hooks | API |
|---|---|---|---|---|---|
| Indent list (showIndentListSchema.json, addNewIndentSchema.json, addIndentItems.html, showIndentDetailsSchema.json + OnFreezed/OnApproved/Editable variants) | `/m/inventory/indentListofStore`, `/m/inventory/purchaseIndent` | `IndentPage` | `F:IndentComposer` (line editor w/ batch), `C:ApprovalPanel` (2-level), `F:FreezePanel` | `useIndent` (addNewIndent, IndentFreezeOption, indentUpdateApprove, cancelIndent) | `inventoryStockList.*` (175 refs), `expiryitem.*` (38) |
| Purchase request (showPurchaseRequestSchema.json, consolidatePurchaseRequest route) | `/m/inventory/purchaseRequest`, `/m/inventory/consolidatePurchaseRequest` | `PurchaseRequestPage` | `C:DataTable`, `F:RequestEditor` | `usePurchaseRequest` | `inventoryStockList.purchaseRequest*` |
| Quotation (getItemQuotation.json, addItemInQuotation.html, compareQuotationSchema.json, quotationDetails.js) | `/m/inventory/quotation` | `QuotationPage` | `F:QuotationEditor`, `F:QuotationCompare` | `useQuotation` | `inventoryStockList.quotation*` |
| PO list (showCurrentPurchaseOrderListSchema.json, updatePurchaseOrderSchema.json, supplierPoSchema.json, addNewItemsforDirectPO.html, approvePOPage.html) | `/m/inventory/currentPO`, `/m/inventory/orderList`, `/m/inventory/otherUnitPOList`, `/m/inventory/receivedpolist` | `PurchaseOrderPage` | `C:ApprovalPanel`, `F:PoLineEditor`, `F:SupplierPanel` | `usePurchaseOrder` (showCurrentPurchaseOrderList1, cancelCurrentPOList, calculateTotalAmounts) | `inventoryStockList.po*`, `showPOPageSchema` ops |
| GRN (showGRNSchema.json, showApproveGrnListSchema.json, addMultipurposeGRN.html, addNewItemsforDirectGRN.html, addNewItemsforConsignmentGRN.html, showConsignmentGRNListSchema.json) | `/m/inventory/grn`, `/m/inventory/multipurposeGRN`, `/m/inventory/approveGrn`, `/m/inventory/gRNList` | `GrnPage` | `F:GrnEditor`, `F:QcPanel` (qcStatusUpdate.json), `C:ApprovalPanel`, `C:PrintDialog` (GRN) | `useGrn` (showRelatedGRN) | `inventoryStockList.grn*`, `paymentDetailModeForGRNSchema` ops |
| Reorder (getReorderListForDirectItemIssue.json, showCurrentReorderListSchema.json, reOrderList route) | `/m/inventory/reOrderList` | `ReorderPage` | `C:DataTable`, `F:ReorderEditor` | `useReorder` | `inventoryStockList.reorder*` |
| Stock list (stockListSchema.json, showCurrentStockListSchema.json, showOnlyNearExpiryItems, itemStockList route) | `/m/inventory/itemStockList`, `/m/inventory/stockList`, `/m/inventory/expiredItemList` | `StockListPage` | `C:DataTable`, `F:BatchDetail`, `F:ExpiryFilter` | `useStockList` | `inventoryStockList.stock*` |
| Stock adjustment (showStockAdjustmentListSchema.json, addItemsforStockAdjustment.html) | `/m/inventory/stockAdjustmentList` | `StockAdjustmentPage` | `F:AdjustmentLines`, `F:ReasonPanel` | `useStockAdjustment` | `inventoryStockList.adjustment*` |
| Stock transfer (stockTransfer route; showReturnFlowStore.json) | `/m/inventory/stockTransfer` | `StockTransferPage` | `F:TransferLines` | `useStockTransfer` | `inventoryStockList.transfer*` |
| Issue item (showAllItemIssueListSchema.json, showIndentListonItemIssueSchema.json, batchBarCodeForItemIssue.html, directIssueItem route) | `/m/inventory/issueItem`, `/m/inventory/directIssueItem` | `IssueItemPage` | `F:IssueLines`, `C:BarcodeScannerField` | `useIssueItem` | `inventoryStockList.issue*` |
| Consumption (showStoreComsumptionListSchema.json, showDepartmentalConsumptionListSchema.json, materialConsumptionDetails.json, addItemsforConsumption.html) | `/m/inventory/consumeItemforStore`, `/m/inventory/departmentalConsumption(List)` | `ConsumptionPage` | `F:ConsumptionLines` | `useConsumption` | `inventoryStockList.consumption*` |
| Returns (issueItemReturn.json, returnItemRecieve.json, returnToSupplierSchema.json, addReturnToSupplier.html, acceptRejectExpDamgedReturn.html, damagedItemReturnIndent.json, expiredDamagedItemReturnIndent.json, expiryItemReturnSchema.json, expiryItemReturnToSupplierSchema.json, addExpiredDamagedItemIndent.html, recallExpiredItemSchema.json, showRecallExpiredItemSchema.json) | `/m/inventory/returnItem`, `/m/inventory/returnItemRecieve`, `/m/inventory/returnToSupplier`, `/m/inventory/expiryItemReturn(List/ToSupplier)`, `/m/inventory/expiredItems`, `/m/inventory/expire`, `/m/inventory/recallExpiredItem`, `/m/inventory/holdItemBatches` | `ReturnItemPage`, `ExpiryReturnPage` | `F:ReturnLines`, `F:AcceptRejectPanel`, `F:SupplierPanel`, `C:ApprovalPanel` | `useItemReturn`, `useExpiryReturn` (expiryDamageItemReturn.js ports) | `expiryitem.*` (38), `inventoryStockList.return*` |
| Item master (itemMasterSchema.json, addNewItemSchema.json, updateItemMaster.json, updateBatchExpiry.json) | `/m/inventory/itemMasterList`, `/m/inventory/updateBatch`, `/m/inventory/itemSummary` | `ItemMasterPage` | `F:ItemEditor`, `F:BatchEditor` | `useItemMaster` | `itemmaster.*` (14), `inventoryStockList.item*` |
| Item enquiry (itemEnquirySchema.json, getItemEnquirySchema.json, itemEnquiry-module.js) | `/m/inventory/itemEnquiry` | `ItemEnquiryPage` | `F:EnquiryFilters`, `C:DataTable` | `useItemEnquiry` | `inventoryStockList.enquiry*` |
| Opening balance (addItemsforOpeningBal.html, currentOpeningBalanceList.json, batchBarCodeCountForOB.html) | `/m/inventory/opeiningBalance` | `OpeningBalancePage` | `F:BalanceLines`, `C:BarcodeScannerField` | `useOpeningBalance` | `inventoryStockList.openingBalance*` |
| Asset register (assetRegisterList.json, showIndentDetailsEditableonAssetSchema.json) | `/m/inventory/assetRegisterList` | `AssetRegisterPage` | `C:DataTable` | `useAssetRegister` | `inventoryStockList.asset*` |
| Custom orders (customProcessOrder.json, customProcessItemDetails.json, customOrderAssignee.json, showCustomOrderList.json, addNewCustomOrderPresc.html) | `/m/inventory/customOrderList` | `CustomOrderPage` | `F:CustomOrderEditor`, `F:AssigneePanel` | `useCustomOrder` | `inventoryStockList.customOrder*` |
| All approvals (allApproval route) | `/m/inventory/allApproval` | `AllApprovalsPage` | `C:ApprovalPanel` (worklist across stages) | `useAllApprovals` | `inventoryStockList.approve*` |
| User store / stock location (userStore, stockLocation routes) | `/m/inventory/userStore`, `/m/inventory/stockLocation` | `StoreConfigPage` | `F:StoreEditor`, `F:LocationEditor` | `useStoreConfig` | `master.userStore*` |

### 2.9 Blood Bank — `fjs-config_bloodBank.json`, 40 schemas, 98 templates

| Legacy screen | React route | Page | Components | Hooks | API |
|---|---|---|---|---|---|
| Donor registration (donorRegistartion.json, VoluntaryDonationSchema.json, ReplacementDonationSchema.json, AutologousDonationSchema.json, CampDonationSchema.json, donorrejectionreasonmaster.json) | `/m/bloodBank/donorRegistration`, `/m/bloodBank/campDonation` | `DonorRegistrationPage` | `C:PatientSearch`, `F:DonorForm` (type switch), `F:RejectionReason`, `C:PrintDialog` (donor card) | `useDonorRegistration` | `bloodtestmaster.*` (91 refs — the dominant family) |
| Donor list (showDonorListSchema.json, replacementDonorHistory.json) | `/m/bloodBank/donorList`, `/m/bloodBank/replacementDonorHistory` | `DonorListPage` | `C:DataTable`, `F:DonorDetail` | `useDonorList` | `bloodtestmaster.donor*` |
| Requisitions (patientRequisitionDetails.json, patientRequisitionList.json, ExternalPatientRequisitionSchema.json, campRequisitionSchema.json, externalHospitalRequisition.json, externalBloodStorageCenterRequisition.json, addPatientRequisition.html, addAdminRequisition.json) | `/m/bloodBank/patientRequisition`, `/m/bloodBank/campRequisition`, `/m/bloodBank/eraktkoshRequest`, `/m/bloodBank/externalPatientRequisition/add` | `RequisitionPage` | `F:RequisitionForm`, `F:CrossMatchRequest`, `C:PrintDialog` (requisition) | `useRequisition` | `bloodtestmaster.requisition*` |
| Blood bag opening stock (addBloodBagOpeningStock.json, afterAddOpeningStock.json, bloodBagOpeningStock.html, openingBloodStockSourceDetailsSchema.json) | `/m/bloodBank/bloodBagOpeningStock` | `OpeningStockPage` | `F:BagStockLines` | `useOpeningStock` | `bloodtestmaster.openingStock*` |
| Component extraction (bloodComponents.json, bloodComponentSeperationSchema.json, bloodBagComponanentExtraction.html, addBloodNewComponent.html, addBloodComponentRow.html) | `/m/bloodBank/addNewBloodComponent`, `/m/bloodBank/bloodBagComponentExtraction` | `ComponentExtractionPage` | `F:ComponentGrid`, `F:ExtractionForm` | `useComponentExtraction` | `bloodtestmaster.component*` |
| Cross matching (bloodCrossMatchingListSchema.json, bloodRequisitionCrossMatchingSchema.json, crossMatchingForm.json, crossMatchingPatientDetails.json, crossMatchingBloodbagDetails.json, bloodCrossMatchingRecords.html, bagAcceptForm.html) | `/m/bloodBank/bloodCrossMatching`, `/m/bloodBank/crossMatchingList` | `CrossMatchingPage` | `F:CrossMatchWizard` (requisition→bag→patient), `F:BagAccept` | `useCrossMatching` | `bloodtestmaster.crossMatching*` |
| Issue bags (bagIssueConfirmation.html, bloodBagIssueLabel.html, bloodBagIssueBarcode.html, bagIssueTblRow.html, bagIssueLabelForExternalOrders.html) | `/m/bloodBank/issueBloodBag` | `IssueBagPage` | `F:BagPick`, `C:PrintDialog` (issue label/barcode), `C:SignatureCapture` | `useIssueBag` | `bloodtestmaster.issue*` |
| Bag trail / discard / return (bloodBagTrail route; showBloodDiscardListSchema.json, showDiscardedBagsSchema.json, bagReturnDetailsSchema.json, bloodBagReturnSchema.json, bloodBagDiscardRemarks.html, bagReturnRow.html) | `/m/bloodBank/bloodBagTrail`, `/m/bloodBank/bloodBagDiscard`, `/m/bloodBank/BloodDiscardList`, `/m/bloodBank/retrunBloodbag` | `BagTrailPage`, `DiscardPage`, `ReturnBagPage` | `F:BagTimeline`, `F:DiscardReason`, `F:ReturnReason` | `useBagLifecycle` | `bloodtestmaster.bag*` |
| Cross unit order (crossUnitOrderSchema.json, crossUnitOrdersListSchema.json) | `/m/bloodBank/crossUnitOrder` | `CrossUnitOrderPage` | `F:OrderForm`, `C:DataTable` | `useCrossUnitOrder` | `bloodtestmaster.crossUnit*` |
| Stock / indent (showBloodStockListSchema.json, showBloodBankIndentListSchema.json, BloodBanKReOrder route) | `/m/bloodBank/showIndentList`, `/m/bloodBank/BloodBanKReOrder` | `BloodStockPage` | `C:DataTable`, `F:ReOrderForm` | `useBloodStock` | `bloodtestmaster.stock*` |
| TTD / serology (transfusionTransmittedDiseases, redCellSerology routes) | `/m/bloodBank/transfusionTransmittedDiseases`, `/m/bloodBank/redCellSerology` | `TtdPage`, `SerologyPage` | `F:TestResultGrid` | `useTtd`, `useSerology` | `bloodtestmaster.ttd*` |

### 2.10 OT — `fjs-config_ot.json`, 14 schemas, 48 templates

| Legacy screen | React route | Page | Components | Hooks | API |
|---|---|---|---|---|---|
| Surgery schedule board (surgeryScheduleSchema.json, otSchedular.html, OtBookedSlotsDetails.html, otscheduledetails.json, addNewSurgerySchema.json, cancelOTSchedule.html) | `/m/ot/surgSchedule`, `/m/ot/otScheduleEvent` | `OtSchedulePage` | `C:ScheduleBoard` (timeline view), `F:SurgeryForm`, `F:CancelReason` | `useOtSchedule` | `otscheduler.*` (66 refs) |
| Surgery list (showSurgeryOrderList.json, surgList route) | `/m/ot/surgList` | `SurgeryListPage` | `C:DataTable`, `F:SurgeryDetail` | `useSurgeryList` | `otscheduler.list*` |
| Procedure performance (procedurePerformanceSchema.json, patientProcedureDetails.html, addPatientProcedureAnesthesia.html, addPatientProcedureEquipment.html, addRowForBloodDetails.html, addStaffDetailsForOt.html, addDoctorDetailsForOt.html, addIntraInstruction.html, otCheckList.html) | `/m/ot/procPerformance`, `/m/ot/patMovementAudit` | `ProcedurePerformancePage` | `F:ProcedureForm`, `F:AnesthesiaPanel`, `F:EquipmentPanel`, `F:StaffPanel`, `F:IntraOpNotes`, `C:AuditHistory` | `useProcedurePerformance` | `otscheduler.procedure*` |
| Procedure rates (patientProcedureRateDetailsSchema.json, addPatientProcedureRateSchema.json, addSurgHeadSchema.json, surgeryheads.json, addOTCharge.json) | `/m/ot/patientProcedureRateDetails` | `ProcedureRatePage` | `F:RateEditor` | `useProcedureRate` | `otconfig.*` (17 refs) |
| Nurse schedule (nurseSchedular.html, nurseScheduleEvent route) | `/m/ot/nurseScheduleEvent` | `NurseSchedulePage` | `C:ScheduleBoard` | `useNurseSchedule` | `otscheduler.nurse*` |
| Item details event (itemDetailsEvent route, itemDetailsForOT.html, otEquipmentList.html) | `/m/ot/itemDetailsEvent` | `OtItemDetailsPage` | `F:ItemLines`, `F:EquipmentList` | `useOtItemDetails` | `otconfig.item*` |

### 2.11 Other modules (representative depth)

| Module | Legacy screens (evidence) | React routes | Page → components | Hooks | API |
|---|---|---|---|---|---|
| cssd | Kit master (`addKitMaster.html`), kit preparation (`addKitPreparation.html`), sterilization (`cssdItemKitSterilization.html`), store dispatch/receive (`cssdRequisitionDetailsFormStoreDispatch.html`), issue/return (`cssdItemIssueNew.html`) | `/m/cssd/kitMaster`, `/m/cssd/kitPreparation`, `/m/cssd/sterilization`, `/m/cssd/storeDispatch`, `/m/cssd/itemIssue`, `/m/cssd/itemReturn` | `KitMasterPage` (`F:KitTree` tree, `F:KitItems`), `SterilizationPage` (`C:ApprovalPanel`), `DispatchPage` | `useKitMaster`, `useSterilization` | `cssd.*` (39 refs) |
| allocation | Nurse↔ward/doctor/patient allocation (`nurseWardAllocationDetails.html`, `showNurseWardAllocationWrap.html`, `showNurseDoctorAllocationWrap.html`, `showNurseDepartmentAllocationWrap.html`, `showPatientListOfPatientAllocationWrap.html`, `showNurseShiftDetails.html`) | `/m/allocation/nurseWard`, `/m/allocation/nurseDoctor`, `/m/allocation/nurseDepartment`, `/m/allocation/patient`, `/m/allocation/shift` | `AllocationPage` (per-type: `F:AllocationTable`, `F:ShiftEditor`) | `useNurseAllocation` | `nursingList.allocation*` |
| diet | Diet order (`addDiet.html`), menu items (`dietMenuItems.html`), kitchen view (`dietKitchenViewDetailsList.html`), food order list (`dietFoodOrderList.html`), schedule (`dietScheduleScreen.html`), ward details (`wardPatDetails.html`) | `/m/diet/order`, `/m/diet/menu`, `/m/diet/kitchen`, `/m/diet/foodOrderList`, `/m/diet/schedule` | `DietOrderPage` (`C:PatientBanner`, `F:DietLines`), `KitchenPage`, `MenuPage` | `useDietOrder`, `useKitchen` | `diet.*` (23 refs) |
| transport | Vehicle/driver masters (`vehicle-master-list.html`, `driver-master-list.html`), patient transport request (`patientTransportRequest.html`), trip request summary (`showTripRequestSummary.html`), vehicle in/out (`vehicleInDetails.html`, `vehicleOutDetails.html`), fuel station (`addFuelStationDetails.html`), maintenance (`addVehicleMaintenanceRequest.html`) | `/m/transport/vehicles`, `/m/transport/drivers`, `/m/transport/request`, `/m/transport/trips`, `/m/transport/fuel`, `/m/transport/maintenance` | `VehicleMasterPage`, `TransportRequestPage` (`F:RequestForm`, `F:VehicleDriverPick`), `TripPage` | `useTransport` | `transport.*` (20 refs) |
| equipments | Asset verification (`assetVerificationList.html`, `addAssetForVerification.html`), preventive maintenance (`addNewPrevetiveMaintncSchedu.html`, `addPrevMaintenanceDtls.html`, `calibrationMaintenanceList.html`), complaints (`addNewComplaint.html`, `complaintDetail.html`, `complaintResolutionView.html`), equipment GRN (`addNewGrnForEquipment.html`), med-gas indent (`addEquipMedGasIndent.html`) | `/m/equipments/assets`, `/m/equipments/pmSchedule`, `/m/equipments/complaints`, `/m/equipments/grn`, `/m/equipments/medGas` | `AssetPage` (`C:DataTable`), `PmSchedulePage` (`C:ScheduleBoard`), `ComplaintPage` (`F:ComplaintForm`, `C:AuditHistory`) | `useAssets`, `usePmSchedule`, `useComplaints` | `equipment.*` (35), `preventivemaintenance.*` (15) |
| queue | Queue board (`departmentQueue10.html`), cabin queue (`cabinQueueListWrap.html`), doctor batch (`doctorBatchWrap.html`), assign cabin (`assignDoctorCabin.html`), facts popup (`factsPopup.html`) | `/m/queue/board/:deptId`, `/m/queue/cabins`, `/m/queue/doctorBatch`, `/m/queue/assignCabin` | `QueueBoardPage` (`C:QueueBoard`, `F:TokenCard`, `F:FactsPopup`), `AssignCabinPage` | `useQueueBoard` (live polling), `useTokenCall` | `queueManagment.*` (12) |
| masters | 300 master CRUD screens driven by `mastersConfigration.json` (`genericMasters.html`, `genericMastersFilters.html`, `dynamicMastersTable.html`, `genericMastersFilters`); masters-modules.js, user-module.js, notification-template-module.js | `/m/masters/:masterName` (param-driven) | `GenericMasterPage` (config-driven: `F:MasterCardGrid` → `F:FilterBar` → `C:DataTable` → `F:MasterForm`), `UserPage`, `NotificationTemplatePage` | `useMasterConfig` (loads `getModuleWiseMasters`), `useGenericCrud` | `master.*` (100), `masterConfigService.*` (25), `api/rest/master/getModuleWiseMasters?module=` (common-module.js:76) |
| commons | ABHA (`abhaPatientProfileList.html`…), mortuary, shift/roster, notification templates, file upload (`fileUploadForm.html`), geo-tagging (`hospitalGeoTagging.json`), hospital creation (`hospitalCreationSchema.json`), encounter types, audit | `/m/commons/abha`, `/m/commons/mortuary`, `/m/commons/shifts`, `/m/commons/notifications`, `/m/commons/upload`, `/m/commons/hospitals`, `/m/commons/audit` | `AbhaCenterPage`, `MortuaryPage`, `ShiftPage`, `NotificationPage`, `HospitalAdminPage` | `useAbhaCenter`, `useShiftRoster` | `abha.*` (35), `master.*` |
| careplan | NCP problems/goals (`ncpSubProblems.html`, `ncpSubProblemDetails-template.html`, `ncpSubProblemDetails-goalsTemplate.html`, `carePlanPreview.html`, `ncpPatientDetails.html`) | `/m/careplan/plan` | `PlanOfCarePage` (same as nursing but standalone entry) | `usePlanOfCare` | `nursingList.ncp*` |
| chemotherapy | Regimen/new comparison (`chemoNewRegimenView.html`, `chemoNewRegComparison.html`), cycle orders (`chemoCycleMedOrdCheck.html`, `chemoCycleMedOrdSignCheck.html`), meds entry (`chemoMedsOrderEntry.html`, `chemoMedicationRow.html`, `chemoAddMedication.html`), notes (`chemoNotes.html`, `chemoNoteSummury.html`), clinical staging (`chemoClinicalStaging.html`), diagnosis (`chemoDiagnosisDetails.html`), MDC notes (`chemoMdcNotes.html`) | `/m/chemotherapy/regimens`, `/m/chemotherapy/cycleOrders`, `/m/chemotherapy/medsEntry`, `/m/chemotherapy/notes`, `/m/chemotherapy/clinicalStaging` | `RegimenPage` (`F:RegimenEditor`, `F:CyclePanel`), `ChemoMedsPage` (`F:MedLines`, `F:ByPassPanel`), `ChemoNotesPage` | `useRegimen`, `useChemoMeds`, `useChemoNotes` | `pharmacy.chemo*`, `nursingList.chemo*` |
| bcma | Bedside medication admin: due list (`bcmaNavDueList.html`), med log (`bcmaMedicationLogNew.html`, `bcmaMedicationLogForScanner.html`), IV bags (`bcmaIV.html`, `bcmaIVChronolgyActionOnBag.html`, `bcmaIVChronolgyHeld/Refused/MissingDose.html`), variances (`bcmaNavMedVarianceLog.html`), allergies report (`bcmaAllergiesReport.html`), main screen (`bcmaMainScreen.html`) | `/m/bcma/admin` | `BcmaPage` (`F:DueListNav`, `F:MedLog` w/ scan mode, `F:IVBagChronology`, `F:VarianceLog`, `C:AuditHistory`) | `useBcma` (barcode + QZ label), `useIvChronology` | `medication.bcma*`, `pharmacy.bcma*` |
| biomedicalwaste | Request (`createBioMedicalRequest.html`), receive (`receiveBioRequestDetails.html`), decompose (`decomposeBioWaste.html`), bag barcode (`generateBioWasteBagBarcode.html`) | `/m/biomedicalwaste/requests` | `BioWastePage` (`F:RequestForm`, `F:BagBarcode`, `F:DecomposePanel`) | `useBioWaste` | `bioMedicalWasteManagement.*` (11) |
| linenlaundry | Request (`createLinenRequest.html`), dispatch (`dispatchLinenItems.html`), handover (`handOverLinenItems.html`), receive (`receiveLinenItemDetails.html`) | `/m/linenlaundry/requests` | `LinenPage` (`F:RequestForm`, `F:DispatchLines`, `F:ReceivePanel`) | `useLinen` | `linenLaundry.*` (12) |
| rmrs | Dashboard (`RMRSDashboardHeader/List.html`), payment collection (`addPaymentCollection.html`, `paymentCollectionHeader/List.html`), issue payment (`issuePayment.html`), bank statement (`bankStatement.html`), journal entries (`JournalEntriesListDetail.html`), discount (`addDiscount.html`), reports (`planAndAccountReportHeader.html`, `summaryAccountWiseReport.html`) | `/m/rmrs/dashboard`, `/m/rmrs/payments`, `/m/rmrs/issue`, `/m/rmrs/bank`, `/m/rmrs/journal`, `/m/rmrs/discounts`, `/m/rmrs/reports` | `RmrsDashboardPage` (`C:DataTable`, `F:PaymentForm`, `F:JournalGrid`) | `useRmrs` | `RMRS.*` (26) |
| rcm | Chiranjeevi app (`chiranjeeviapp.html`) | `/m/rcm/chiranjeevi` | `RcmPage` (`F:ChiranjeeviForm`) | `useRcm` | `rcm.*` |
| pmrd | Patient medical record (`patientMedicalRecordWrap.html`, `patientEncounterRecord.html`), docs upload (`uploadPatientDocumentsPmrd.html`), form J1/J2/J3 (`formj1.html`…) | `/m/pmrd/records`, `/m/pmrd/documents`, `/m/pmrd/formJ/:n` | `PmrdPage` (`F:EncounterList`, `C:FileUpload`, `C:ImageAnnotation`, `C:PrintDialog` for form J) | `usePmrd` | `fileService.*`, `nursingList.pmrd*` |
| fileManagement | Document categories (`addDocumentCategory.html`), files by category (`filesAndDocumentsByCategory.html`), view (`viewDocument.html`), annotated image (`annotatedImageView.html`) | `/m/fileManagement/categories`, `/m/fileManagement/documents`, `/m/fileManagement/annotate/:docId` | `DocumentPage` (`C:FileUpload`, `C:ImageAnnotation`) | `useDocuments` | `fileService.*` (18) |
| grievance | Ticket list (`grievanceTicketStatusList.html`), status/severity change (`changeTicketStatus.html`, `changeGrievanceSeverity.html`), assign (`assignedTicketToUser.html`), history (`grievanceTicketHistory.html`), feedback (`grievanceFeedbackForm.html`), action (`grievanceActionPopup.html`) | `/m/grievance/tickets`, `/m/grievance/feedback` | `GrievancePage` (`C:DataTable`, `F:TicketDetail`, `F:AssignPanel`, `C:AuditHistory`) | `useGrievance` | `grievance.*` |
| enquiry | Hospital events (`showAllHospitalEvents.html`, `showAllEventTypes.html`), dept-doctor info (`showDeptDoctorInformationWrap.html`), visit/admission enquiry (`showVisitAdmissionDetails.html`), feedback (`showPatientFeedbackList.html`) | `/m/enquiry/events`, `/m/enquiry/deptDoctor`, `/m/enquiry/visits`, `/m/enquiry/feedback` | `EnquiryPage` (`C:DataTable`) | `useEnquiry` | `master.*` events |
| reports | Report wrapper (`allReportWrapper.html`, `allHospitalReportWrap.html`), user filter (`report-user-filter.html`), 46 report filter schemas, BI report (`report_BI.html`, `report_BI_DM.html`), janta clinic reports (`jantaClinicReports.html`) | `/m/reports/:reportKey` | `ReportPage` (`F:ReportFilterBar` (schema-driven), `F:ReportEmbed` (iframe), `F:ReportTable`, `C:PrintDialog`) | `useReport` (param serialization) | `report.*` per report schema; BI embed URL |
| kiosk | (See OP — kiosk templates) | `/m/kiosk` | `KioskPage` | `useKiosk` | `appointmentschedule.*` |
| nhcx | Eligibility (`checkEligibilityWarp.html`), pre-auth (`preAuthWrap.html`) | `/m/nhcx/eligibility`, `/m/nhcx/preAuth` | `NhcEligibilityPage` (`F:EligibilityForm`, `F:PolicyList`) | `useNhcEligibility` | `nhcx.*` |
| shifthandover | (See nursing) | `/m/nursing/shiftHandover` | — | — | — |
| worklist | (See nursing) | `/m/worklist/tasks` | — | — | — |
| tiuxmls | (See nursing) | `/m/tiuxmls/:formId` | — | — | — |

**Coverage statement:** 40 modules mapped; full depth for op/ip/billing/pharmacy/nursing/pathology/radiology/inventory/bloodBank/ot; representative depth elsewhere; masters handled via one config-driven generic pattern (300 screens).

---

## 3. Reusable component catalog

Component contract: **component | legacy source | API dependency | business-rule dependency**.

| Component | Legacy source (evidence) | API dependency | Business rules |
|---|---|---|---|
| `PatientSearch` | `patient-search-list.html`; controlEvents `.pat-mrnsearch-box/.pat-mobsearch-box/.pat-civilId-search/.pat-passp-search/.pat-drivingl-search/.familyIdsearch/.janaadharIdSearch/.abhaIdSearch/.pctsNumberSearch/.ectsNumberSearch` (walkinVisit-schema.json); `getPatientGeneralDetailsListForSearch` calls (common-module.js:14267, billing-module.js:13407) | POST `api/rest/patient/getPatientGeneralDetailsListForSearch?is_admission=&isRegSearch=`; nursing variant `api/rest/nursingList/getPatientGeneralDetailsListForSearchNursing?duz=` (nursing-module.js:1231) | min-length triggers (8/3/10/12/7/10/14/4), id-type parsing, `validatePatientDetails` (validation.js) |
| `PatientBanner` | coversheet schemas (`showPatientCoversheetSchema.json`, `showPharmacyPatientCoversheetSchema.json`), `ipd_patient_service_detail_schema.json`, `loadCoverSheetDetails()` (nursing-module.js) | `nursingList.coversheet*` | encounter type, active flags |
| `DoctorSelector` | `doctors-schema.json`, `doctorFilterFilter` (appointment-module.js:2169) | GET `api/rest/master/doctorFilterFilter`; dept variants | role-based doctor visibility |
| `DepartmentSelector` | `department-schema.json`; `getValidDepartments()` (appointment-module.js:2059) | POST `api/rest/appointmentschedule/getDepartmentsByDate` | age/gender/isEmergency eligibility |
| `CabinSelector` | `getCabinsByDept` (appointment-module.js:1332); `generic-radio-list-cabin.html` | GET `api/rest/addUserService/getDepartmentwisecabinById`; `getCabinWiseApointmentSlotCount` | slot counts, least-full-cabin defaulting |
| `HospitalSelector` | `hospitalCreationSchema.json`, unit_id globals | `master.*` | session tenant (`m_tenant_id`) |
| `DataTable` | faug viewer engine (`layer/default.json` `_t_*` skeleton; `details.viewer` in schemas; `pathoReportSchema.json`) | any `crud.ra` op | server paging flags, search_enabled, dwnld_enabled |
| `Pagination` | `layer/default.json` table.footer | — | infinite-scroll option (`tablePagingInfiniteEnabled`) |
| `FilterBar` (DateRangePicker+selects) | `labDateFilter.html`, `generic-report-filter-list.html`, `department-report-filter-list.html` | per-workflow list APIs | date format globals |
| `StatusBadge` | `btnNotify` classes + `notifymanageCls` (appointment-details.html:29-96; app.css) | — | category→color map (fixed palette) |
| `ClinicalForm` (schema-driven) | faug form engine; `layer/default.json` form skeleton; `walkinVisit-schema.json` field model (`validations`, `events`, `ui`, `custom.class`) | `crud.c/u` per schema | `validateHiddenElements=false`, required-marker, regex validators |
| `ApprovalPanel` | inventory stage screens (`approvePOPage.html`, `showIndentDetailsOnApprovedSchema.json`), billing (`approveAmountListSchema.json`) | `inventoryStockList.approve*`, `companyapprovedamount.*` | 2-level workflow state machine |
| `AuditHistory` | `auditSchema.json`, `newAuditSchema.json`, `patientMovementAudit.html`, `getAddendumNoteList.html` | `audit.*` | — |
| `ConfirmationDialog` (+reason variant) | `Faug.alertDialog`, bootbox, `admissionCancelReason.html`, `apptCancelReason.html` | — | required-reason flows |
| `PrintDialog` + `PrintService` | `printContent()/printContentBarcode()/printContent2()/printContent2WithoutHeader()` (common-module.js:119-588); `printerSetting-ext.js`; `qz-tray.js`; `print.css` | client-side + QZ WebSocket + BrowserPrint | printer prefs in localStorage; dot-matrix flag; paper size |
| `SignatureCapture` | `electronicSign.html`, `changeElectronicSign.html`, `signature_pad.umd.min.js` | verify/sign endpoints per screen | e-sign + canvas modes |
| `DoubleVerify` (release) | `firstVerificationSchema.json`, `secondVerificationSchema.json`, `boxCommonVerification.json` | per-domain release APIs | patho report release, box verification |
| `FileUpload` | `fileUploadForm.html`, `uploadFileSchema.json`, `uploadDocumentSchema.json`, `amendmentFileUpload.json` | `fileService.*` | 10MB cap (`layer/default.json file.maxSizeMb`) |
| `ImageAnnotation` | `markerjs2.min.js`, `painterro-1.2.87.min.js`, `annotatedImageView.html`, `annotation-view.js` | `fileService.*` | — |
| `PhotoCapture` | `capturePhoto.html`, `webcam.min.js` | `fileService.*` | — |
| `BarcodeScannerField` | `.scanBarcodeCartFill`, `bcmaMedicationLogForScanner.html`, `html5-qrcode.min.js` | per-screen scan APIs | hardware/camera modes |
| `BarcodeLabel` | `cssdBarCodeLabel.html`, `bloodBagBarCode.html`, `batchBarCodeCount.html`, `emergencyBarcode.html`, `qRCodePrint.html` | print service | label templates per module |
| `VitalRangeSlider` | `chartVitalRangeSlider.html`, `vitalsConfiguration.json` (sliderType slider/dualslider, unit ranges, qualifiers) | `nursingList.vitals*` | unit conversion C/F, qualifier flags |
| `GrowthChart` | `growthCharts.html`, `charts/ODCWF_SAM_CHART.json` et al., ChartingApplication.js | `nursingList.chartData*` | WHO chart curves |
| `Partograph` | `partoGraph.html`, `partoVitalRangeSlider.html`, `ODPartoGraphConfig.json` | `nursingList.parto*` | config-driven chart+slider composition |
| `ObservationFlowSheet` | `ChartingApplication.js` + `charts/ODCWF_*.json` | `nursingList.cwf*` | per-chart JSON config |
| `ScheduleBoard` (timeline) | `timelineScheduler.js`, `otSchedular.html`, `nurseSchedular.html`, `showDoctorOpSchedule.json` | `otscheduler.*`, `appointmentschedule.*` | slot conflicts |
| `QueueBoard` | `queue/` app, `departmentQueue10.html`, `cabinQueueListWrap.html` | `queueManagment.*` | token state machine, facts popup |
| `BedBoard` | `bed-screen-building.html/department.html`, `bed-managment-*`, `bed_management_schema.json` | `bedmanagement.*` | bed colorCode, filters |
| `Tree` | `jstree.min.js`, masters hierarchy, allocation trees | — | — |
| `TiuxmlRenderer` | `tiuxml.js` `processTemplate` (tabs, indents, boilerplate, one-item groups), `modules/tiuxmls/schemas/*.json` | `tiuxml.*` save/load | form template semantics |
| `DashboardTiles` (role home) | `icon-list.html`, `tiles.js`, `genericMasters.html` | `getModuleWiseMasters`, mastersConfigration | RBAC `data-roleM` keys |
| `RoleGate` (RBAC) | `data-roleM` attributes throughout, `mastersConfigration.json config` | `master.getModuleWiseMasters` | role-key hierarchy |
| `TokenFlow` | `ohumlite/js/appointment-token-flow.js`, `queueEvtCls` | `walkinVisit.*`, `queueManagment.*` | counter queue start/stop |
| `ReportEmbed` | `report_BI.html`, `allReportWrapper.html` (iframe) | report URLs | — |
| `CascadingSelects` (unit→dept→cabin→doctor→slot) | walkinVisit `namedEvents` chain | appointmentschedule APIs | least-full-cabin policy |

---

## 4. Hooks strategy

Per-page: `use<Page>Data` (React Query), `use<Page>Form` (RHF+zod), `use<Page>Actions` (mutations + legacy function ports). Shared:

| Hook | Purpose | Legacy port source |
|---|---|---|
| `useSchemaForm<T>` | schema JSON → RHF fields, validation, conditional show/hide (`events` hide/how), errorselector | faug form engine + `layer/default.json` skeleton |
| `useSchemaEffects` | schema `events` chains + `namedEvents` → declarative dependencies (watch→fetch→derive) | walkinVisit namedEvents |
| `useSchemaCrud` | `crud` ops incl. named ops (`c/u/d/r/ra/<named>`) with beforeOp/afterOp/failure callbacks | walkinVisit crud block |
| `useServerTable` | server pagination/search/download for DataTable | faug viewer + `layer/default.json` table footer |
| `usePatientLookup` | multi-identifier patient search w/ min-length triggers | controlEvents (walkinVisit-schema.json) |
| `usePrint` | browser/QZ/BrowserPrint dispatch, printer prefs, paper size | common-module printContent* + printerSetting-ext |
| `useSignaturePad` | canvas signature + typed e-sign | signature_pad + electronicSign.html |
| `useWebcam` | getUserMedia capture | capturePhoto.html |
| `useBarcodeScanner` | camera/manual input modes | html5-qrcode + scan inputs |
| `useVitals` | slider-based vital entry w/ config | vitalsConfiguration.json |
| `useChartConfig` | ODCWF/partograph JSON → chart + slider layout | ChartingApplication.js |
| `useRoleAccess` | RBAC keys → UI gating | data-roleM, mastersConfigration |
| `useNotificationBadges` | category counts per list screen | `notifymanageCls` (appointment-details.html) |
| `useTokenQueue` | token call/next/status | queue + token-flow |
| `useUnitContext` | session unit/hospital/counter state | `unit_id`, changeCashCounter.html |
| `useOnlineSync` | offline queue → sync (mobile) | w-api-call cachedApiData + offlinedashboard |

---

## 5. API client layer

- **Generation:** one module per legacy resource family (from the 1,842-endpoint inventory, e.g. `api/nursingList.ts`, `api/inventoryStockList.ts`, `api/master.ts` …). Each function: `GET/POST/PUT/DELETE api/rest/<path>` with typed request/response derived from the matching schema (`crud.url` + `content` template).
- **Transport:** `fetch` wrapper replicating legacy semantics: `Content-Type: application/json`, `X-Request-ID` idempotency UUID for POST/PUT/DELETE (w-api-call.js), optional `cachedTime` TTL cache keyed by URL+body (w-api-call `cachedApiData`), error envelope → `{message, status}` normalized to legacy `printErrorMessageByErrorMsg` convention.
- **Auth:** same header mechanism as legacy (token from login; `onlineToken` in fjs-config globals) — keep server contract unchanged.
- **Concurrency:** legacy `numApiWebWorkers: 3` → React Query request dedup + optional worker pool for heavy exports.
- **Route-name API aliases** preserved: e.g. `patient.getGeneralDetailsForSearch` maps to the schema op name `patientSearchbyEctsNo`/`patientMobSearch` etc. so business logic ports stay traceable.

---

## 6. State management

| Concern | Legacy | React |
|---|---|---|
| Server state | jQuery callbacks | TanStack Query (per-endpoint cache, TTL mirrors `cachedTime`) |
| Session/global | `Fg.g()/Fg.addVar()` bag | Zustand store `useAppStore`: `unit_id`, `duz`, `isNhnc`, `app_type_w`, `login_user_role_l`, printer prefs, counter, route flow counters (`fjs-config.json` globals) |
| Form state | DOM serialize + `serializeObject` | react-hook-form |
| Workflow state (indent freeze/approve, clearance, release) | route+global flags | Zustand per-domain stores or URL state |
| URL state | `Fg.getRoute()` string parsing | react-router params/query |
| i18n | i18next + `data-translate` + `Fg.t()` | react-i18next, schema `display` as keys; format constants from fjs-config globals |
| RBAC | `data-roleM` DOM keys | centralized `useRoleAccess` registry generated from `mastersConfigration.json` |

---

## 7. Print strategy

Three-tier service `usePrint` mirroring legacy:

1. **Browser print (default):** render printable React tree into a print portal; `@media print` visibility rules ported from `print.css` (hide `.printBtn`, nav, modals; show only target ids; `.noteHeader`, `.tableBorder` rules). Covers receipts, summaries, notes, form J, reports.
2. **Label/barcode print:** QZ Tray WebSocket (keep `qz-tray.js`-compatible client; SJCL/sha-256 signing handshake), printer selection via legacy `localStorage` keys (`DefaultSystemPrinter`, `BarcodePrinter`, `isADotMatrixPrinter`); fallback `BrowserPrint-2.0.0.75.min.js` for Zebra.
3. **Server-assisted print:** reserve hook (`print.server.url` unresolved in public JS — mark unknown; recommend backend team confirms).

Paper-size/header variants preserved (`printContent2WithoutHeader(htm, papersize)`).

---

## 8. Offline / mobile strategy (ohumlite / queue / patportal)

- **Shared core:** move the mobile shells (`ohumlite/`, `queue/`) onto the same React core with feature-flagged routes: `ohumlite` = registration/appointment/token/ABDM/billing subset (`ohumlite/js` evidence); `queue/` = `QueueBoard` only; `apps/offlinedashboard` = dashboard widgets.
- **Offline:** PWA/service-worker cache keyed by the legacy cached-API pattern (w-api-call `cachedTime`/`cachedApiData`); write-behind queue for POST/PUT with `X-Request-ID` idempotency replay; schema+master cache via `getModuleWiseMasters`.
- **Device:** keep `signature_pad`, camera, barcode scanning as web APIs; legacy device JS (markerjs2, painterro) remain vendor libs.
- **Cordova:** same build; legacy `polyfill.min.js` dropped (modern webview).

---

## 9. Design system spec (derived from legacy, not invented)

### 9.1 Tokens
- **Brand:** primary `--color-primary: #01A080` (btn-success evidence, app.css); deep teal `--color-primary-deep: #00715b` (sidebar `left_col`, navigation.css); accent `--color-primary-light: #10c39e`; hover `#1abb9c` (active nav, navigation.css).
- **Status palette (badge semantics, app.css):**
  - `--status-completed: #bdffb4` (compNotify — Checked Out)
  - `--status-checked-in: #f2f9fb` (checkInNotify)
  - `--status-pending: #fff4e3` (pendingNotify, onlineNotify)
  - `--status-noshow: #efeadf` (noshowNotify)
  - `--status-walkin: #faeffb` (walkinsNotify)
  - `--status-emergency: #ffddc9` (emergencyNotify, issuedNotify)
  - `--status-info: #59b0c3` (btn-blue-lg, app.css)
- **State colors:** success/error via `has-success`/`has-error` (layer/default.json); required marker `.asteriksCls`.
- **Bed status:** `colorCode` field per bed record (bed-screen-building.html) — theme as data-driven chips.
- **Typography:** system stack (Bootstrap 3 default 14px); section headers `groupingTitle` h5; label style `.form-label`; dashboard tile text small caps. Fonts: `fonts/` (DejaVuSans for labels, digital-7 for OPD/queue displays) — keep digital-7 for token/counter screens.
- **Icons:** migrate the custom icon font (`icons.css`/`icon-style.css`; `icon-patient`, `icon-mrn`, `icon-date`, `icon-checkin`, …) to inline SVG components with the same names — schema `custom.icon.class` values map directly.
- **Spacing/grid:** 12-col grid with legacy widths (`col-lg-4` etc.) → responsive CSS grid/12-col utilities; `colnopad` variant; `margin-top-18` style helpers.
- **Radius:** subtle (legacy `border-radius:2px` on `.btn-blue-lg`); sharp tables (legacy square corners).

### 9.2 Primitives (all themed to tokens)
`Button` (btn-success/btn-lg/btn_wrap/btn-blue variants), `Badge` (status palette), `Table` (stripped/bordered/hover + sticky header `tableHeadFixer` → `position:sticky`), `Tabs` (nav-tabs + `.classTab` select-as-tabs), `Modal` (faug modal skeleton: header/body/footer, `isCloseAllwd`), `Drawer` (new; legacy used modals — **note as UX modernization**, see §11), `Field` (form-group + input-group-addon icon+label layout), `RadioGroup`/`CheckboxGroup` toggle styles (`data-toggle=buttons` look), `RangeSlider` (ion.rangeSlider look), `Tree`, `Breadcrumb` (bed-breadcrums.html), `DashboardTile` (thumbnail cards + tile buttons), `Alert`/`Toast` (alertDialog replacement), `Pagination` (Prev/Next + "Page X of Y").

### 9.3 Clinical data presentation
- **Vitals:** `VitalRangeSlider` per `vitalsConfiguration.json` (abbreviation T/BP/P/R/SpO2…, sliderType slider/dualslider, unit ranges, qualifiers) — render config verbatim.
- **Partograph:** `Partograph` composed from `ODPartoGraphConfig.json` leftView/rightView (chartType spline/column, setOfCodes rows, detailViewController keys preserved as component ids).
- **Growth/observation charts:** `GrowthChart` + `ObservationFlowSheet` from `charts/ODCWF_*.json`.
- **Cover sheets / banners:** `PatientBanner` — compact identity + status + quick actions (nursing coversheet evidence).
- **Charts lib:** recharts (or Highcharts React) with legacy colors; highstock-equivalent range selectors for trends.

### 9.4 Responsive behavior
- Port breakpoint behavior from `mobile-responsive.css` (≤992/768/736/667/620/568/375, orientations) and `ipad-tablet-responsive.css` (1024/768/600/1200): forms stack to single column at xs, tables horizontal scroll (`table-responsive`), sidebar collapses to icon rail (`nav-sm` 70px, navigation.css), touch-friendly targets ≥44px (legacy iPad CSS), orientation-specific layouts retained for clinical entry.
- Keep `Table` sticky header + frozen action column where legacy `tableHeadFixer` applied.

### 9.5 Accessibility plan (fixing evidenced gaps)
- Replace `<li onclick>`/`<div class="btnNotify">` clickables with real `<button>`/`<a>` (appointment-details.html evidence) — keeps visuals, adds semantics.
- Add `aria-label`/`aria-describedby` to icon-only buttons, `role="dialog"` + `aria-modal` + `aria-labelledby` on modals (legacy only had `role="dialog"`), focus trap + return-focus (faug modal had none), `for`/`id` label association (legacy labels lacked it).
- Field-level errors: keep `help-block`/`has-error` visual + add `aria-invalid` and `role="alert"`.
- Status colors: pair badges with text/icons (color-only evidence: badge backgrounds, bed colorCode).
- Keyboard: all flows reachable by tab order; remove `tabindex="-1"` on date/time fields, replace with real pickers; provide skip-nav.
- Screen-reader labels for search/print/download controls (icon-only today).
- Contrast: check teal-on-white (legacy `#01A080` on white fails AA for small text — bump to darker `#00715b` for text, keep brand for surfaces).
- Respect `prefers-reduced-motion` (legacy has spinner loaders).
- `lang`/`dir` + `data-translate` keys via i18next (already present).

### 9.6 Print CSS
Port `print.css`: hide app chrome, show print target; `.noteHeader` centered band; `.tableBorder` collapse; page-size handling per receipt/label; QZ labels unchanged.

---

## 10. Testing & quality gates
- Per module: `*.stories.tsx` visual + a11y (axe) checks against legacy screenshots (golden files).
- `useSchemaForm` unit tests against 3 sample schemas (walkinVisit, addBilling, pathoReport).
- API client generated types validated against real `api/rest` responses (contract tests).

---

## 11. UX modernization (per-screen problem → target)

Principles: **clinical workflow speed first; no cosmetic redesigns; every change tied to an evidenced problem.**

| # | Legacy problem (evidence) | Target |
|---|---|---|
| 1 | Walk-in registration = one 60-field scroll form (`walkinVisit-schema.json`) | Keep 1:1 fields; split into **stepped sections within the same route** (Patient → Visit/Complaint → Appointment → Billing/Sponsor) with autosave draft; section anchors = legacy `groupingTitle`. Progress indicator added; all legacy conditional-show rules preserved. |
| 2 | Cascading multi-click selector chain (Complaints→Dept→Building→Cabin→Doctor→Slot, walkinVisit namedEvents) | Replace the repeated pickers with a single **cascade component** (auto-advance selection, prefetch next options via React Query) — fewer clicks, same order. |
| 3 | Appointment list = 12 category badges, each a full reload (appointment-details.html) | Single table with **filter chips** (legacy badge palette) — same 12 categories, one server call with `categ` param; badge counts via one counts endpoint. |
| 4 | Modal-heavy notes (addNursesNotes.html modal + tile list) | Notes editor as **inline drawer** from the coversheet tiles; same title-based template (`notesTitle.json`), sign flow unchanged. |
| 5 | Inventory multi-stage screens with duplicated row editors (indent→freeze→approve→PO→GRN, separate templates per stage) | One **workflow-aware page per document type** with stage tabs (legacy stage templates become stage panels) + shared `ApprovalPanel`; state machine from `showIndentDetailsOnFreezed/OnApproved/Editable` evidence. |
| 6 | Pathology filter→list→detail repeated 7× (collection/accept-reject/resulting/review/machine/release/critical) | One **stage-parameterized WorklistPage** (stage id from legacy schema names) — filter bar and detail stay 1:1. |
| 7 | Bed board multi-tab navigation with route-string parsing (`rtparts[4]`, bed-screen-building.html) | Bed board with **breadcrumb-driven drill-down** (building→dept→ward) in URL params; same blocks/colors. |
| 8 | No destructive-action undo/confirm consistency (direct `deleteNhncService`, `cancelIndent`) | Shared `ConfirmationDialog` with required-reason variants (legacy reason modals exist: apptCancelReason.html, admissionCancelReason.html). |
| 9 | Deep-link fragility (`Fg.getRoute()` string parsing) | URL params + `useSearchParams`; back/forward supported (legacy disabled back/forward: `disableBackForwardButton: true` — **keep disabled semantics for clinical screens**, enable only where safe). |
| 10 | Slow page feedback (freeze overlay only) | Skeleton loaders + optimistic UI for confirm actions (same blocking semantics on critical saves). |
| 11 | Barcode/label flows scattered across 6+ modules (cssd/bloodBank/inventory/op/pharmacy) | Shared `PrintDialog` + `BarcodeLabel` components per module config (label templates preserved). |
| 12 | Non-semantic clickables / no a11y (see §9.5) | Semantics fixed without visual change. |
| 13 | Multi-tab work areas with hidden state (clearanceTab.html etc.) | Keep tabs; add **unsaved-changes guards** and per-tab validation summaries. |
| 14 | Queue board (queue app) manual refresh | Polling/SSE with same token state machine; keep facts popup. |
| 15 | Kiosk self-registration | Keep minimal flow; enlarge touch targets (kiosk evidence: registrationOpCard.html). |
| 16 | BCMA scanner workflows | Same flow; modern barcode scanning; label print unchanged. |

**Explicit non-goals (per instructions):** no new clinical features; no workflow redesigns beyond the evidenced interaction-level fixes above; doctors' ordering flow semantics (order sets, consults) unchanged.

---

## 12. Migration sequencing (recommendation)

1. **Core platform**: design tokens, primitives, `ClinicalForm`, `DataTable`, `usePrint`, RBAC, router shell → **OP** (walk-in + appointment list) as pilot.
2. **IP + billing** (bed board, admission, service billing) — highest clinical+revenue overlap.
3. **Pharmacy + nursing** (orders, notes, coversheet, vitals).
4. **Pathology + radiology** (stage-parameterized worklist).
5. **Inventory** (workflow pages + approvals).
6. **Blood bank, OT, remaining modules** (queue, cssd, diet, transport, equipments, masters generic, misc).
7. **Mobile shells** (ohumlite/queue/offlinedashboard) re-pointed to the React core.

---

## 13. Risks & unknowns (carried from findings)
1. faug-min.js internals unverified (minified) — schema feature surface enumerated from 1,750 schemas instead; edge-case schema features may require iterative schema validation during `ClinicalForm` development.
2. `print.server.url` unresolved in public JS — confirm backend print service before building server-print tier.
3. Masters screen set is server-config-driven (`getModuleWiseMasters`) — the 300-route master list is config, not code; generic master page must render from config shape.
4. ABDM/ABHA (abdm-module.js, aadhaarauth.js) likely superseded by ABDM SDK — confirm product direction before porting UI.
5. BCMA scanner hardware protocol unknown — keep manual+standard-camera modes, integrate vendor SDK behind a port.
6. Charting app (`ChartingApplication.js` + ODCWF JSONs) is a large standalone canvas surface — recommend a dedicated follow-up agent for charting/partograph port depth.
7. `scheduler.js`, `tables.js`, `tiles.js`, `tiuxd/` are auxiliary/experimental — excluded pending confirmation.

---

## 14. Delivery summary

- **Legacy screens mapped:** 533 distinct route definitions across 40 modules (704 refs; 290 non-master distinct +300 generic masters =590 incl. masters), every module represented; full-depth mapping for 10 core modules.
- **Reusable components cataloged:** 37 cross-cutting components, each with legacy source, API dependency, and business-rule dependency evidence.
- **Design system:** derived from legacy CSS/JS evidence — teal brand palette, 8 status badge colors, icon-font → SVG mapping, form/table/modal skeletons, responsive breakpoints, print rules.
- **Top UX risks (evidence-based):** (1) one-shot 60-field registration + cascading selector chain; (2) modal-heavy nested flows; (3) route-string state & disabled back/forward; (4) color-only status semantics and non-semantic clickables; (5) duplicated stage screens (lab/inventory) begging for consolidation — all fixable within the 1:1 mapping contract.
