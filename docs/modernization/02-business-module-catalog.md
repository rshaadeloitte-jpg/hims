# iHMS/OHUM PAS NG — Business Module Catalog

Evidence-based catalog derived from the codebase at `D:\ihms` (Spring Boot 2.7.5 + Java 8 + EclipseLink JPA + CXF JAX-RS + FAUG jQuery). Every claim is backed by one or more of: `fjs-config_*.json`, `modules/*/schemas`, `modules/*/templates`, `services/*ServiceImpl.java` (`@Path`), `models/*` entities (`@Table`), root `templates/*.vm`, `js/*-module.js`.

Companion evidence dump: `_discovery/agent-02/findings.md`.

Legend: Screens = FAUG schema names (from fjs-config / schemas folder). APIs = CXF class-level `@Path`. Statuses = status-related fields/endpoints observed (codes only; meaning → agent 06). UNKNOWN = not determinable from code in this pass.

---

## M01 — Patient Management (Registration & Demographics)

- **Evidence**: `fjs-config_patientManagement.json` (addSocialWorker), `fjs-config_op.json` (patientDetailReg), PatientServiceImpl `/patient` (registerPatient, getPatientByAddhar, getPatientByDrivelicNo, mergeHId, searchPatientForABDM, getPatientDemographicDetails, getPatientRecord), PatientDetailRegistrationServiceImpl `/patientDetailReg`, `modules/masters/patient/*` (40+ master schemas), models/trx (Encounter, MLCDetails, RegistrationQueue), `js/admission-module.js`, `js/clinic-module.js`.
- **Submodules**: Registration (OPD/IPD), Encounter & visits, MLC details, Mother–child linking, Patient categories (BPL/insurance/RGHS/Janaadhar/Bhamashah), Social worker (patientManagement).
- **Purpose**: Patient identity, demographics, registration, encounter history, verification (Janaadhar, ABHA, Aadhaar, biometric), MLC (medico-legal case) capture.
- **Actors**: Registration clerk, Front office, Social worker, Admin (merge IDs).
- **Features**: registerPatient; demographic search (Aadhaar/driving licence/Janaadhar); MLC details + documents; ABHA/ABDM patient matching & ABHA number keying; HID merge; corona/COVID quarantine tracking (patientCoronaDetails, geofenceViolated); family member details; teleconsult room creation.
- **Workflow**: Search/verify identity (Aadhaar/Janaadhar/ABHA) → register demographic → generate MRN → create visit (OPD/IPD/emergency) → optional MLC capture → optional social-worker assessment.
- **Statuses**: visit status fields on Encounter/Visit; MLC status fields (mlcDetailsSchema); UNKNOWN-11 exact code lists.
- **Dependencies**: Masters (patient/demographics), Billing (MRN series), ABDM integration (ABHA), e-Aushadhi/Janaadhar integrations.
- **Tables**: Encounter, MLCDetails, RegistrationQueue, FamilyMemberDetails, SpouseAndChildrenDetails, PatientHqDetails, PatientDemoDetailsMismatch, T_ANCVISITDETAILS; masters/patient.*
- **APIs**: `/patient`, `/patientDetailReg`, `/visit`, `/helper` (registerChild, registerChildDocit).
- **Screens**: addpatientDetailRegdto-schema, patientRecordSchema, mlcDetailsSchema, motherChildLinkingSchema, addSocialWorker.
- **Reports**: patientHistory.vm, patientAllHistory.vm, PatientVisitDetailsForFuture.vm, healthCardSheet.vm.
- **Integrations**: ABDM/ABHA (searchPatientForABDM, addPatientAbhNumberKey), Aadhaar/Janaadhar/Bhamashah templates, Chiranjeevi (getChiranjeeviDetailByJanAdhar), Solr search (searchPatientFromSolr).

---

## M02 — OPD / Outpatient Department (Appointments, Walk-ins, Doctor Queue)

- **Evidence**: `fjs-config_op.json` (43 keys), AppointmentServiceImpl `/appointment`, AddUpdateAppointmentServiceImpl `/updateappointment`, AppointmentScheduleServiceImpl `/appointmentschedule`, PatientRegApptServiceImpl `/patientRegAppt`, EmergencyAndWalkinVisitServiceImpl `/walkinVisit`, DoctorServiceImpl `/doctor` (checkinCheckout, queue assignment endpoints), QueueManagmentServiceImpl `/queueManagment`, `modules/op/schemas/*`, `js/appointment-module.js`, `js/appointment-list-module.js`, `js/clinic-module.js`.
- **Submodules**: Appointments (new/reschedule/cancel/future), Walk-in visits, Emergency registration (walkinVisitNhnc), Doctor schedules & check-in, Department appointment slots, Follow-up visits, Referrals (doctorReferralSchema), Camps (opCampRegistration), Queue/token (see M27), Telemedicine.
- **Purpose**: Outpatient visit lifecycle from appointment to consultation queue.
- **Actors**: Appointment desk, Registration clerk, Doctor, Queue/nursing desk, Patient (self-service kiosk), Camp organiser.
- **Features**: doctor-appointment slots; department/doctor schedules; walk-in & NHNC walk-in; follow-up visit requests; doctor cabin check-in/check-out; token generation; MRN re-print (opdCard); op labels; referral types; camp registration; telemedicine department/unit/speciality lists.
- **Workflow**: Book appointment (walk-in/phone/kiosk/camp) → validate identity → token/queue → doctor check-in → consultation → orders (lab/rad/pharmacy) → billing → follow-up or discharge home.
- **Statuses**: appointment status (cancelledAppointmentList, apptCancelReason, apptRescheduleReason); visit status; queue status (see M27).
- **Dependencies**: M01 (patient), M04 (billing for charges), M08 (orders), M27 (queue), Masters (hospital, patient, billing services).
- **Tables**: models/trx (Encounter, FollowUpVisitInfo, RegistrationQueue), PatientQueue, masters/hospital (schedulemaster, shiftmaster, opdStationMaster, roomMasterForQueue).
- **APIs**: `/appointment`, `/updateappointment`, `/appointmentschedule`, `/patientRegAppt`, `/walkinVisit`, `/doctor`, `/queueManagment`, `/patient` (kioskQueue, myWalkingVisit).
- **Screens**: appointment_list-schema, appointment_list_doctor-schema, doctor-appintment-slots-schema, walkinVisit-schema, followupVisitReq_list-schema, showDoctorOpSchedule, manageTokenSchema.
- **Reports**: appointment_confirm.vm, appointmentLetter.vm, appointmentOpLabel.vm, appointment_token.vm, opdCard (template).
- **Integrations**: Kiosk (M31), ABDM (ABHA reg in OP), NHNC (walkinVisitNhnc), telemedicine (createTeleconsultRoom).

---

## M03 — IPD / Admission, Bed & Discharge

- **Evidence**: `fjs-config_ip.json` (65 keys), IPDAdmissionServiceImpl `/ipdadmission`, IPDServiceImpl `/ipd`, IPDConfigServiceImpl `/ipdconfig`, BedServiceImpl `/bed`, BedReservationServiceImpl `/bedreservation`, BedManagementServiceImpl `/bedmanagement`, CancelAdmissionServiceImpl `/canceladmission`, PatientTransferDischargeServiceImpl `/patientTransferDischarge`, `modules/ip/schemas/*`, `modules/masters/admission/*`, `js/bed-management-module.js`, `js/admission-module.js`.
- **Submodules**: Admission (planned/emergency/interim), Bed management (allocation, transfer, release, reservation, cradle), Discharge (advice, clearance, summary), Transfer requests, Attendant passes, MLC clearance, Baby admission & mother–child linking, Diet plan linkage.
- **Purpose**: Inpatient stay lifecycle: admission → bed → stay services → discharge → bed release.
- **Actors**: Admission clerk, Ward nurse, Bed manager, Nursing station, Attendant desk, Doctor (discharge advice), Billing (clearance), Mortuary (death cases).
- **Features**: admission request & admission list; bed reservation/transfer/release; cradle management; interim admission; discharge clearance list & update status; bed census (getBedCensusAndNonCensusList); attendant pass issue/return/reprint; MLC clearance filters; emergency admission; bill estimation at admission.
- **Workflow**: Admission request → bed allocation → admit (generate admission no) → ward services (nursing, diet, pharmacy, orders) → discharge advised → clearance (billing/BMW/etc.) → discharge summary → bed release.
- **Statuses**: admission status, discharge status (dischargeUpdateStatus), bed occupied flag (updateBedOccupiedFlag), bed status master, clearance status, transfer request status.
- **Dependencies**: M01, M02 (emergency), M04 (billing/clearance), M10 (diet), M09 (nursing), M08 (orders), M28 (mortuary for deaths).
- **Tables**: AttendantPassDetails, TransferRequest, masters/admission (bedmaster, wardmaster, roommaster, classmaster, blockmaster, buildingfloorstructure, wingmaster, dischargetype, admissiontype, daycaremaster, modeOfDelivery, actualBedCountMaster).
- **APIs**: `/ipdadmission`, `/ipd`, `/ipdconfig`, `/bed`, `/bedreservation`, `/bedmanagement`, `/canceladmission`, `/patientTransferDischarge`, `/patientrequisition` (blood for IPD).
- **Screens**: addpatientAdmissiontdto_schema(+_emergency), admission_list-schema, admissionRequest_list-schema, bed_management_schema, bed_screen_schema, bedTransferSchema, dischargePatientSchema, discharge_clearance_schema, transferRequest_list-schema, attendantPassSchema, bedReservationdto_schema.
- **Reports**: admissionFacesheet1.vm, adm6.vm, token_sheet.vm, discharge_summary_pat.vm, wardBedInquiry.vm, bht_barcode.vm.
- **Integrations**: HIS admission request (addAdmissionRequestToHIS), IPCR (patientIPCRSchema), RGHS (patientRghsPackageDetails), Corona tracking.

---

## M04 — Billing, Payments, Receipts & Refunds

- **Evidence**: `fjs-config_billing.json` (77 keys), BillServiceImpl `/bill`, BillReceiptServiceImpl `/billReceipt`, BillSummaryServiceImpl `/billSummary`, BillDetServiceImpl `/billDet`, BillCancellationServiceImpl `/cancelBill`, AddPaymentServiceImpl `/addPayment`, AddAdvanceServiceImpl `/addAdvance`, AddRefundServiceImpl `/addRefund`, AdvanceBillReceiptServiceImpl `/advancebillReceipt`, AllReceiptsServiceImpl `/allReceipts`, RefundBillReceiptServiceImpl `/refundBillReceipt`, PatientBillEstimationServiceImpl `/patientbillestimation`, ChargeServiceImpl `/charge`, PaymentServiceImpl `/payment`, RefundServiceImpl `/refund`, `js/billing-module.js`.
- **Submodules**: OP billing, IP billing, Advance (collection/refund/adjustment), Receipts (bill/advance/refund/combined), Bill cancellation (settled/unsettled), Estimation, Credit notes, Doctor share & deductions, Concessions, Company/insurance billing (approved amounts), Package subscriptions, Pharmacy bill receipt, Tariff application & copay.
- **Purpose**: Charge capture → estimation → bill generation → payment collection → receipts → refunds; doctor share settlement.
- **Actors**: Cash counter operator, Billing officer, Accountant (credit notes), Doctor share admin, Concession authority.
- **Features**: addOpIpBilling; partial payment; advance against bills; bill estimation; IP bill with pharmacy separate (ipBillSummeryWithoutPharmcy.vm); credit notes; doctor share add/deduct & bill; sponsor change (changeSponsorSchema); approval amounts for company billing; package subscription; tariff copay details.
- **Workflow**: Services charged (orders/consumables/bed) → bill summary (draft) → payment (cash/card/cheque/advance/company) → receipt print → refund on cancellation (unsettled/settled paths).
- **Statuses**: settled/unsettled (unsettledBillCancellation, settledBillCancellation), bill status (getAllBills-schema), advance status (draftAdvanceSummarySchema), credit note status, refund status.
- **Dependencies**: M01, M02, M03, M05 (tariff/packages), M07 (pharmacy bill), M06 (doctor share), M33 (payment transfer), M32 (RCM).
- **Tables**: CreditNote(+Details), PackageChargeSubscription, PatientBillEstimationSurgeryHead, Doctorvisitcharge, Doctorwaiver, masters/billing (companymaster, companyplandetails, tariffmaster, packagemaster, planmaster, concessiontemplatemaster, paymentmodetype, cashcountermaster, cancellationreason, advanceagainst).
- **APIs**: `/bill`, `/billReceipt`, `/billSummary`, `/billDet`, `/cancelBill`, `/addPayment`, `/addAdvance`, `/addRefund`, `/advancebillReceipt`, `/allReceipts`, `/refundBillReceipt`, `/patientbillestimation`, `/charge`, `/payment`, `/refund`, `/companyapprovedamount`, `/concessiontemplate`.
- **Screens**: addBillingSchema, addOpIpBillingSchema, addIpBillingSchema, billReceiptSchema, billSummarySchema, allReceiptsSchema, advanceSchema, advance_recieptSchema, refund_recieptSchema, creditNoteSchema, doctorShareSchema, partialPayBillSchema, opBillPaymentSchema, ipBillPaymentSchema.
- **Reports**: bill_receipt.vm, bill_summary.vm, ip_bill_summary.vm, ipbillreceipt.vm, opbillRec.vm, ipDraftBill.vm, advance_rpt.vm, advanceRefundRec.vm, cancelIPReceipt.vm, cancelOPReceipt.vm, creditNoteDetails.vm, creditNoteServices.vm, packageRefundReceipt.vm, billEstimation.vm, high_billing_report.vm.
- **Integrations**: Payment transfer (M33), RCM (M32), NHCX (insurance claims, via nhcx module).

---

## M05 — Tariff, Packages, Health Plans, Companies & Services (Billing Masters)

- **Evidence**: TariffServiceImpl `/tariff`, ServiceMasterServiceImpl `/servicemaster`, PlanMasterServiceImpl `/planmaster`, PackageServiceServiceImpl `/packageservice`, ConcessionTemplateServiceImpl `/concessiontemplate`, CompanyApprovedAmountServiceImpl `/companyapprovedamount`, AddServiceMasterImpl `/addServiceMaster`, MasterCachedServiceImpl `/master`, `modules/masters/billing/*`, `js/item-category-module.js`.
- **Submodules**: Service master; tariff & copay; packages (service/group/medicine/item-category); health plans (planmaster, subplan, tariff package health plan); companies & plans; concessions; order sets (ordersetdefination + service variants); specialization.
- **Purpose**: Pricing & eligibility backbone for billing.
- **Actors**: Billing admin, Tariff officer, Company contract manager.
- **Features**: tariff rate copy utility; package health plan definitions; order-set definitions (consult/lab/radiology/nursing/procedure/consumables/misc); services specialization search; company contract details; plan-uncovered services; concession templates.
- **Business rules (evidence)**: packages can be service/group/medicine/item-category based; tariff package rates class-wise (Tariffpackageclasswiserates); copay per tariff; plans can exclude items/services (Planuncovereditemdetails, Planuncoveredservicedetails).
- **Statuses**: service/tariff active flags; UNKNOWN-12.
- **Dependencies**: M04 (billing), M01 (patient categories), M07 (pharmacy pricing).
- **Tables**: Servicemaster(+new), Tariffmaster, Tariffservicemaster, Tariffcopaydetails, Tariffpackage*, Packagemaster, Packagehealthplandefinationmaster, Planmaster, SubPlanmaster, Companymaster, Companyplandetails, Companytariffdetails, Concessiontemplatemaster(+details), Orderset*, Specialization, Subspecialization.
- **APIs**: `/tariff`, `/servicemaster`, `/planmaster`, `/packageservice`, `/concessiontemplate`, `/companyapprovedamount`, `/addServiceMaster`, `/master`.
- **Screens**: servicemaster, tariffmaster, tariffratecopyutility, packagemaster, packages, planmaster, subPlanmaster, companymaster, ordersetdefinationmaster, services, specialization.
- **Reports**: packageDetailsPrint.vm, packageConsumptionDetailsPrint.vm.
- **Integrations**: RGHS packages (getRghsPackageList), NHCX plans (insurance_plan_on_request).

---

## M06 — Doctor Share & Incentives

- **Evidence**: DoctorServiceImpl `/doctor` (addDoctorAdditionDeduction, getDoctorShareList, getDoctorBillList, addDoctorBill, addDoctorBillPayment, updateDoctorShare), `modules/billing/schemas/doctorShare*`, masters/hospital (admittingdoctorclassificationmaster), models (Doctorvisitcharge, Doctorwaiver).
- **Submodules**: Doctor share calculation, addition/deduction, doctor bills & payments, visit charges, waiver.
- **Purpose**: Financial settlement of doctor professional fees.
- **Actors**: Billing admin, Accounts, Doctors.
- **Features**: doctor share list/bill/payment; doctor add-deduct; doctor waiver; doctor visit charges.
- **Dependencies**: M04 (billing), M01 (doctors via masters).
- **Tables**: Doctorvisitcharge, Doctorwaiver, masters/hospital/admittingdoctorclassificationmaster, T_DOCTORVISITCHARGES, T_DOCTORWAIVER.
- **APIs**: `/doctor` (share endpoints).
- **Screens**: doctorShareSchema, doctorShareAddDeductSchema, doctorShareBillSchema, doctorBillListSchema, doctorvisitcharge.
- **Reports**: (doctor share print UNKNOWN-13).

---

## M07 — Pharmacy & Dispensing

- **Evidence**: `fjs-config_pharmacy.json` (39 keys), PharmacyServiceImpl `/pharmacy`, PrescriptionMasterServiceImpl `/prescriptionmaster`, DrugServiceImpl `/drug`, MedicationServiceImpl `/medication`, PrescriptionDetailsServiceImpl `/prescription`, ItemSalesServiceImpl `/itemsales`, `modules/pharmacy/schemas|templates`, `modules/masters/Pharmacy|meds`, `js/pharmacy-module.js`.
- **Submodules**: Prescriptions (doctor/nurse/uploaded), Dispensing (OP/IP/ward/counter), Medication administration support (BCMA link), Item sales & returns, IV/compounding, Cart-fill & ward pick lists, Pharmacy billing, Interventions, Home medications (ipHomeMedsForm), External patients (externalPatient), Store/stock queries (getItemAndItemBatchListByStoreId).
- **Purpose**: Medication ordering → verification → dispensing → administration → return; OTC/counter sales; pharmacy billing.
- **Actors**: Pharmacist, Dispensing clerk, Nurse (medication orders), Doctor (prescribing), Store in-charge, Ward pharmacist.
- **Features**: addPrescriptionSchema; addMedicationSchema(+FromNurse); dispensing types; pharmacy counter sale; item sale return; IV pharmacy; compounding (preparation with batch rows); ward pick lists (wardPickListOrders); pharmacy intervention reports; prescription scan & upload; label printing (printMedLabel, compoundingLable); ipPharmacyBilling.
- **Workflow**: Prescription (doctor/nurse/upload) → pharmacist verify → dispense (batch/expiry check from stock) → bill/payment → issue labels → returns (IP/OP) → interventions.
- **Statuses**: prescription status; dispensing status (dispensingtype); order verification status (pharmacyOrderVerification); return status (itemSaleReturn); UNKNOWN-14 code lists.
- **Dependencies**: M13 (inventory stock), M04 (billing), M09 (nursing orders), M08 (services), BCMA (M30).
- **Tables**: PrescriptionMaster, PrescriptionDispenseDetails, PatientMedication, PatientPrescptionNote, PatientNonMedOrderDetails, masters/Pharmacy (AdministrationSchedule, CreditAccountMaster, Drugingredients, Druginteractions, Medicationroute), masters/meds (instructionmaster, route, scheduleListMaster).
- **APIs**: `/pharmacy`, `/prescriptionmaster`, `/drug`, `/medication`, `/prescription`, `/itemsales`.
- **Screens**: addPrescriptionSchema, addMedicationSchema, dispensingtype, pharmacyCounterSaleSchema, itemSaleListSchema, itemSaleReturnListSchema, opIpPharmacyBillSchema, ipPharmacyBillingSchema, prescriptionListSchema, prescriptionUploadSchema.
- **Reports**: prescription.vm, prescription_format.vm, prescription_hindi.vm, pharmacyReceipt.vm, pharmacyMedLabel.vm, pharmacyItemSaleBillNew(+Counter/Whole).vm, pharmacySaleReturnReport.vm, cartfillDetail.vm, printRxDetails.vm, printMedLabel (template).
- **Integrations**: EAushadi (item stock sync), BCMA (M30), ABDM PHR (patPrescriptionPHR.vm).

---

## M08 — Clinical / Service Ordering & Investigations

- **Evidence**: ClinicalServiceImpl `/clinical`, AddServiceOrderServiceImpl `/serviceOrder`, InvestigationServiceImpl `/investigation`, CWFServiceImpl `/cwf`, `fjs-config_commons.json` (serviceMasterSchema, servicemastermapping), `modules/commons/schemas`, models/trx (PatientEorder), transactionals (PatientEorder, ExternalOrderDetails, PendingConsultVisits), `js/cwf-module.js`, `js/scwf-module.js`.
- **Submodules**: Service orders (addServiceOrderDetails on /patient), Consult orders, Investigations (lab/rad), Order sets, CWF/SCWF (clinical workflow) mapping, Cross-service visits.
- **Purpose**: Clinician orders for services (lab, radiology, procedure, consult, nursing) across OP/IP.
- **Actors**: Doctor, Nurse, Lab/Radiology department staff.
- **Features**: addServiceOrderDetails; consult order lists (consultOrder_list-schema); investigation ordering; order set application; service status (serviceStatusSchema); machine config (getResultsFromKDL).
- **Dependencies**: M04 (billing charge post), M15 (pathology), M16 (radiology), M07 (pharmacy orders), M09 (nursing).
- **Tables**: PatientEorder, ConsultOrder, ExternalOrderDetails, PendingConsultVisits, masters (Orderset*).
- **APIs**: `/clinical`, `/serviceOrder`, `/investigation`, `/cwf`, `/patient` (addServiceOrderDetails, getPatientEorders).
- **Screens**: consultOrder_list-schema, serviceOrderDetail (template), orderingSet/orderSet (templates), serviceMasterSchema.
- **Reports**: printMedNonMedDetails.vm.

---

## M09 — Nursing (Worklist, Notes, Allocation, Care Plans)

- **Evidence**: `fjs-config_nursing.json` (28 keys), `fjs-config_nursingWorklist.json`, nurse/NursingServiceImpl `/nursingList`, ClinicalServiceImpl `/clinical`, NoteDetailsServiceImpl `/notedetailsdata`, ActionNotificationServiceImpl `/actionNotif`, `modules/nursing/schemas|templates` (~160 templates), `modules/worklist/schemas|templates`, `modules/allocation/templates`, `modules/careplan/templates`, `modules/charting/templates`, `modules/shifthandover/templates`, `js/nursing-module.js`, `js/nursing-note-module.js`, `js/nursing-charting-module.js`, `js/incident-reporting-module.js`, docit-roles.js (roleNurse*).
- **Submodules**: Nurse worklist & task list (nurseTaskList), Patient coversheet, Notes (initial assessment, progress, procedure, addendum), Plan of care (careplan: NCP problems/goals), Charting (vitals, partograph, growth charts, health factors), Shift handover (shifthandover templates), Allocation (nurse→ward/doctor/patient, allied staff→clinic), Incident reporting & resolution, Notifiable disease reporting, Vitals entry (vitalmaster, vitalsConfiguration.json).
- **Purpose**: Ward nursing operations: task management, documentation, patient assignment, handover.
- **Actors**: Staff nurse, Nurse in-charge, Nursing superintendent, Allied health staff (physio/OT/SLP per tiuxml AH forms), Social worker (SW forms).
- **Features**: ShowNurseWorklistSchema; patient coversheet; nurse notes with note titles (notesTitle.json); care plan problems/goals; vitals with qualifiers & ranges; incident report/resolve; nurse task frequencies; shift handoff report (multiShiftHandoffReport); nursing bed transfer; temp prescription approval.
- **Workflow**: Ward roster → patient allocation → worklist (orders/vitals/notes/tasks) → documentation → shift handover → (incidents → resolution).
- **Statuses**: task status (completeNursingOrderTask), incident status (incidentResolveSchema), note signed status (signNotesFromTask, signTemplate), UNKNOWN-15.
- **Dependencies**: M03 (IPD), M07 (med orders), M08 (service orders), M15/M16 (results), M26 (worklist), M27 (queue nurse desk).
- **Tables**: NurseNote, NurseTaskDetails, NurseTaskFrequencyDetails, PatientNote, PatientVitals, PatientAllergies, PatientComplaints, PatientDiagnosisDetails, PatientImmunizationDetails, PatientDischargeData, CarePlan(+Details), masters/nursing (notetitleMaster, nursingordermaster, flagMaster, vitalmaster, growthchart*, healthFactor*, diseaseMaster, icdcmCategory, dischargeDepartmentTemplate).
- **APIs**: `/nursingList`, `/clinical`, `/notedetailsdata`, `/actionNotif`, `/cwf`.
- **Screens**: ShowNurseWorklistSchema, ShowNursePatientNoteSchema, ShowNursePlanOfCareSchema, ShowPatientCoversheetSchema, incidentReportSchema, nurseWardAllocationSchema, outreach_data_schema, ShowNurseListSchema.
- **Reports**: dischargeSummary (plainText), patientPrescriptionPlainText (plainText), PatientNoteForAssesment.vm, PatientNoteForIntialAssesment.vm, barthelIndexReport.vm, ccmnote.vm, patientAllocationReport.vm.
- **Integrations**: tiuxmls forms engine (nursing forms), EJS (outreach data), CWF/SCWF workflows, ABDM (EHR sync of notes via syncEHR).

---

## M10 — Diet & Kitchen

- **Evidence**: `fjs-config_diet.json` (4 keys), DietServiceImpl `/diet`, `modules/diet/schemas|templates`, `modules/masters/diet/*`, transactionals (DietOrderDetails, DietItemScheduleDetails, TodayDietItemMenu), `js/diet-kitchen-module.js`.
- **Submodules**: Diet item master, Diet sessions/types, Patient diet orders (IP), Menu & kitchen production, Food item orders, Ward-wise diet lists.
- **Purpose**: Dietetic care for inpatients and kitchen food production planning.
- **Actors**: Dietician, Kitchen staff, Ward nurse.
- **Features**: addpatdietorder; diet food order; today diet item menu; ward-wise patient diet lists; sign patient diet orders; cancel/update food orders.
- **Workflow**: Diet plan per admission (diet type/session) → kitchen production list → food item orders → ward delivery → sign-off.
- **Statuses**: diet order status (updateDietFoodStatusForCancel); UNKNOWN-16.
- **Dependencies**: M03 (IPD), M10 masters, M04 (diet billing service).
- **Tables**: DietOrderDetails, DietItemScheduleDetails, TodayDietItemMenu, masters/diet (dietitemmaster, dietsessionmaster, diettypemaster), masters/hospital (timeofmealmaster, fooditemmaster, foodintakemaster).
- **APIs**: `/diet`.
- **Screens**: diet_foodorder_schema, diet_menu_item_schema, diet_schedule_schema, show_diet_kichen_schema.
- **Reports**: getDietPatReport (API) — print template UNKNOWN-17.

---

## M11 — CSSD (Central Sterile Supply Department)

- **Evidence**: `fjs-config_cssd.json` (15 keys), cssd/CSSDServiceImpl `/cssd`, `modules/cssd/schemas|templates`, `modules/masters/cssd/*`, transactionals (CssdKitFormation(+Details), Cssdkitissueitem(+details), CssdItemSterilizationDetails, ProcedureCssdRequest(+Details), ProcedureCssdKitDetails, ProcedureCssdIssue(+Detail)), `js/cssd-module.js`.
- **Submodules**: Kit formation, Kit preparation, Sterilization, Kit issue to procedures, Indent/stock for CSSD store.
- **Purpose**: Instrument kit lifecycle: formation → sterilization → dispatch to OT/procedures.
- **Actors**: CSSD technician, CSSD in-charge, OT staff (requests).
- **Features**: kit formation/preparation lists; sterilization with machine & quality test masters; procedure CSSD kit request & issue; showItemReceiveListforCSSD (store); sterilization expiry tracking.
- **Workflow**: Procedure requests kit → CSSD forms/prepares kit → sterilize (machine, quality test) → issue → track return/expiry.
- **Statuses**: sterilization status; kit issue status; expiry (sterilizationexpirymaster).
- **Dependencies**: M13 (inventory items), M12 (OT), M04 (service charges for kits).
- **Tables**: CssdKitFormation(+Details), Cssdkitissueitem(+details), CssdItemSterilizationDetails, ProcedureCssdRequest(+Details), ProcedureCssdKitDetails, ProcedureCssdIssue(+Detail), masters/cssd (sterilizationmaster, sterilizationmachinemaster, sterilizationtypemaster, sterilizationqualitytestmaster, sterilizationexpirymaster).
- **APIs**: `/cssd`.
- **Screens**: showCurrentKitMasterListSchema, showCurrentKitFormationListSchema, showCurrentKitPreparationListSchema, showCSSDItemKitSterilization(+Filter), showProcedureCssdIssue, procedureCssdKitRequestFilterSchema.
- **Reports**: cssdReport.vm, requisitionKitDetail.vm.

---

## M12 — Operation Theatre (OT) & Surgery

- **Evidence**: `fjs-config_ot.json` (14 keys), OTSchedulerServiceImpl `/otscheduler`, OTConfigServiceImpl `/otconfig`, OTDetailsServiceImpl `/otdetails`, SurgeryDetailsServiceImpl `/surgerydetails`, `modules/ot/schemas|templates` (~80 templates), `modules/masters/ot/*`, transactionals (PatientProcedureAnesthesiadetails, PatientProcedureRateDetails, PatientScheduleAnasthesiaDetails, Patientprocedurechecklistitems/detailsDto), `js/operation-theatre.js` (referenced; OT controllers in js/).
- **Submodules**: OT theatre/table config, Procedure master & checklist, Surgery scheduling, Anesthesia (pre/PACU/post notes), Surgery charges & billing estimation, Surgery heads, Procedure performance, Staff/doctor scheduling.
- **Purpose**: Surgical case planning, scheduling, resource allocation and charge capture.
- **Actors**: Surgeon, Anesthetist, OT in-charge, OT nurse, Billing.
- **Features**: addNewSurgerySchema; surgery scheduling with slot/conflict checks (checkDoctorAllreadyBookedForSameTime, checkOTAvailability); procedure checklists; pre-anaesthesia check & PAC notes; surgery charges (addSurgCharge, getSurgeryHeadDetailsForBillEstimation); OT equipment booking (addProcedureEquipment); surgery order list from nursing; procedure performance tracking.
- **Workflow**: Surgery order (nursing/doctor) → schedule (theatre, table, staff, equipment) → pre-op (PAC) → procedure (checklist, anesthesia notes) → post-op (recovery) → charges → billing estimation.
- **Statuses**: surgery/schedule status (operationstatusmaster, getSurgeryCompleteList, cancelSurgeryPatient); OT schedule status; UNKNOWN-18.
- **Dependencies**: M03 (IPD patient), M08 (orders), M04 (billing), M11 (CSSD kits), M13 (OT items/equipment), M07 (procedure prescriptions).
- **Tables**: PatientProcedureAnesthesiadetails, PatientProcedureRateDetails, PatientScheduleAnasthesiaDetails, Patientprocedurechecklistitems/detailsDto, masters/ot (ottheatremaster, ottablemaster, proceduremaster, procedurechecklist*, anesthesiatypemaster, anesthesianotes, surgerynotes, surgeryheadmaster(+details), otschedule, operationresultmaster, operationstatusmaster, intra/post/preoperativeinstructionsmaster).
- **APIs**: `/otscheduler`, `/otconfig`, `/otdetails`, `/surgerydetails`.
- **Screens**: surgeryScheduleSchema, addNewSurgerySchema, otscheduledetails, addOTCharge, addOTBillingSchema, AddPatientProcedureRateDetailsSchema, procedurePerformanceSchema, SurgeryOrderList, surgeryheads.
- **Reports**: surgeryScheduleListDetails.vm, OTReport.vm.

---

## M13 — Inventory & Stores

- **Evidence**: `fjs-config_inventory.json` (70 keys), ItemMasterServiceImpl `/itemmaster`, ItemCategoryServiceImpl `/itemcategory`, ItemStockServiceImpl `/itemstock`, OpeningBalanceServiceImpl `/openingbalance`, MaterialConsumptionServiceImpl `/materialconsumption`, ExpiryItemServiceImpl `/expiryitem`, InventoryServiceImpl `/inventoryStockList`, `modules/inventory/schemas|templates`, `modules/masters/inventory/*`, `js/inventory-module.js`, `js/itemEnquiry-module.js`, `js/expiryDamageItemReturn` (referenced via schemas).
- **Submodules**: Item master & categories, Stock & batches, Indents (single/asset/direct issue), Purchase requests & approvals, Stock adjustments, Item enquiry, Expiry & damage returns, Reorder, Store management, Material consumption, Item issue/return.
- **Purpose**: Medical & general stores: item catalog, stock levels, demand and issue.
- **Actors**: Store in-charge, Store clerk, Indentor (ward/dept), Purchase officer, Store supervisor (approvals).
- **Features**: addNewIndentSchema; showIndentDetails (editable/on-approved/on-freezed/on-second-level-approved); item issue lists; stock lists & adjustments; item enquiry; expiry/damaged item return to supplier; reorder lists; direct item issue; store consumption; batch/expiry update.
- **Workflow**: Indent (ward) → freeze → level-1/level-2 approval → issue → receipt/consumption → reorder → (expiry/damage return).
- **Statuses**: indent status (draft/freezed/approved/issued per schema names), PO status (see M14), stock adjustment status, QC status (qcStatusUpdate).
- **Dependencies**: M14 (procurement), M07 (pharmacy stock), M11/M12 (CSSD/OT issues), M04 (billing for sale items).
- **Tables**: ItemStock, ItemClinic, ItemSupplier, StoreItemCategory, masters/inventory (itemmaster, itemcategory, itemgroup, brandmaster, molecule, therapeuticclass, unitofmeasure, store, storeagetype, supplier(+master), taxmaster, rateContractSchema, reorderAndSafetyMaster, salemarkupmaster, grnitemchecklist, grnrejectreasonmaster, termsofpayment, termsAndCondition).
- **APIs**: `/itemmaster`, `/itemcategory`, `/itemstock`, `/openingbalance`, `/materialconsumption`, `/expiryitem`, `/inventoryStockList`.
- **Screens**: itemMasterSchema, stockListSchema, showIndentListSchema, showGRNSchema, showCurrentStockListSchema, itemEnquirySchema, getReorderListForDirectItemIssue, showStockAdjustmentListSchema, expiryItemReturnSchema.
- **Reports**: indentDetails.vm, indentDetailsOfApproved.vm, indentDetailsOfFreezed.vm, invGrnDetails.vm, goods_issue.vm, invItemCurrentStockList.vm, invRecvItemDetail.vm, returnToSupplier.vm, expiryItemDetails.vm, damageItemDetails.vm, itemBatchDetails.vm, itemEnquiryDetails.vm, stockAdjusmentDetails.vm.
- **Integrations**: e-Aushadhi (InventoryEAushadiIntegrationImpl `/eAushadhiIndent`, syncJantaItemMaster), Janta (item sync).

---

## M14 — Procurement (Purchase Orders, Quotations, GRN)

- **Evidence**: PurchaseOrderServiceImpl `/purchaseorder`, QuotationServiceImpl `/quotation`, `modules/inventory/schemas` (PO/quotation/GRN schemas), transactionals (PurchaseRequest(+Details), PurchaseOrderRequest, RateContract(+Details/Units/View), IndentInvoice(+Details), TermsAndCondition), models/trx, `js/inventory-module.js` (procurement screens).
- **Submodules**: Purchase requests, Quotations & comparison, Purchase orders (supplier/in-house/custom), PO approval & freeze, GRN (receipt) & QC, Rate contracts, Indent invoice (supplier billing), Returns to supplier, Consignment GRN.
- **Purpose**: Sourcing, ordering and receiving of stock items.
- **Actors**: Purchase officer, Store in-charge, Supplier, Accounts (payment terms), QC staff.
- **Features**: compareQuotationSchema; approvePOPageSchema; freezeCurrentPOListSchema; showGRNSchema; showConsignmentGRNListSchema; showApproveGrnListSchema; paymentDetailModeForGRNSchema; supplier PO; custom orders; renegotiation (showRenegotiateSchema).
- **Workflow**: PR → quotation collection → comparison → PO (approve/freeze) → GRN → QC → stock receipt → supplier invoice (indent invoice) → payment.
- **Statuses**: PO status (freezed/approved), GRN status (qcStatusUpdate, showApproveGrnList), quotation status, UNKNOWN-19.
- **Dependencies**: M13 (items/stock), M04 (payment), M16 (QC for lab), RMRS (funds via PO requests — showRMRSPurchaseOrderRequestListSchema).
- **Tables**: PurchaseRequest(+Details), PurchaseOrderRequest, RateContract(+Details/Units/View), IndentInvoice(+Details), TermsAndCondition.
- **APIs**: `/purchaseorder`, `/quotation`, `/rmrs` (PO request lists).
- **Screens**: showPurchaseRequestSchema, showPODetailsSchema, showPOPageSchema, approvePOPageSchema, showGRNSchema, compareQuotationSchema, supplierPoSchema, customProcessOrder.
- **Reports**: poDraft.vm, poDraft_New.vm, quotationDetails.vm, eqpGrnDetails(+_GST).vm, grnBarCode.vm.

---

## M15 — Pathology / Laboratory

- **Evidence**: `fjs-config_pathology.json` (57 keys), PathoOrderServiceImpl `/pathoOrder`, `modules/pathology/schemas|templates` (~100 templates), `modules/masters/pathology/*`, transactionals (PathoPatientDocs, PathOrderBookingDetailDocuments), models/trx (Encounter lab data), `js/patho-booking-module.js`, docit-roles.js (roleLab).
- **Submodules**: Lab order booking, Sample collection & barcode, Sample acceptance/rejection, Result entry (resulting), Result review (reviewer), Report release, Critical alerts, Microbiologgy (culture/sensitivity/antibiotic), Machine interfacing (KDL verify, machine-wise review), Test master/profiles/templates, Lab workload.
- **Purpose**: End-to-end laboratory workflow from order to released report.
- **Actors**: Lab technician, Lab supervisor (reviewer/release), Pathologist, Sample collector, KDL (instrument) interface.
- **Features**: pathoOrder; sample collection with barcode; accept/reject; resulting per parameter; result review; report release; critical alert handling; microbiology with organism/antibiotic panels; KDL machine result verify; workload lists; agency master (pathagencymaster); grouping & tests.
- **Workflow**: Order → sample collect (barcode) → accept/reject → run (instrument) → resulting → review → release → report print/HL7.
- **Statuses**: test status (pathoShowTestStatus, pathoUpdateOrderStatus), sample status (acceptRejectSchema), result status (resultReviewSchema), report release status (reportReleaseSchema), critical alert flag.
- **Dependencies**: M08 (orders), M01 (patient), M04 (billing), M13 (consumables), Blood bank (barcode for blood bank — pathoGenerateBarCodeSchemaForBloodBank).
- **Tables**: masters/pathology (pathotestmaster, pathoparametermaster, pathoprofilemaster, pathotemplatemaster, pathosamplemaster, machinemaster, organismmachinmap, pathantibioticmaster, pathoremarksmaster, pathozonemaster, pathocategory, discards reason), transactionals (PathoPatientDocs, PathOrderBookingDetailDocuments), models/trx (IhmsCounts).
- **APIs**: `/pathoOrder`, `/investigation`, `/machineConfig`, `/kdl` (getResultsFromKDL on /patient).
- **Screens**: pathoOrderSchema, sampleCollectSchema, sampleRejectionSchema, resultingDetailSchema, resultReviewSchema, reportReleaseSchema, criticalAlertResultSchema, microbiology-schema, machineWiseResultReviewSchema, pathoGenerateBarCodeSchema.
- **Reports**: pathoAllResult.vm, pathoMicroReport.vm, pathoParameterReport.vm, pathoTemplateReport.vm, pathoTestStatusReport.vm, showLabReport.vm, labloadList.vm, printLabResult.vm, plainText (pathoParameterReport, pathoMicroReport, pathoTemplateReport, showAllLabReport).
- **Integrations**: KDL machine interface, HL7/emr (Lab_Results in tiuxml), Blood bank cross-matching linkage.

---

## M16 — Radiology

- **Evidence**: `fjs-config_radiology.json` (9 keys), RadiologyServiceImpl `/radiology`, `modules/radiology/schemas|templates`, `modules/masters/radiology/*`, `js/operation-theatre.js` (rad order rows in nursing), docit-roles.js (roleRadiology).
- **Submodules**: Order booking, Queue, Result entry (report), Template reports, Retest/reschedule, Image handling (addRadImg, showRadSampleImg), Test master.
- **Purpose**: Imaging orders, acquisition queue, reporting and report release.
- **Actors**: Radiologist, Radiology technician, Booking clerk.
- **Features**: radOrderBookingSchema; radiology queue; report entry; retest; reschedule; template-based reports (radtemplatemaster, radtemplateresult).
- **Statuses**: rad order status; report status; UNKNOWN-20.
- **Dependencies**: M08 (orders), M04 (billing), M01 (patient).
- **Tables**: masters/radiology (radtestmaster, radtestcategory, radmodalitymaster, radtemplatemaster, radtemplateresult, radfilmsize, radCabinMaster, imagediscardreasonmaster).
- **APIs**: `/radiology`.
- **Screens**: radOrderBookingSchema, radReportSchema, radReportDetailsSchema, radRescheduleSchema, radRetestSchema, patientFilterRadSchema, getRadiologyReports.
- **Reports**: showRadiologyReport (template), pathoRadioTemplate (shared template).
- **Integrations**: PACS/imaging (addRadImg, showRadSampleImg).

---

## M17 — Blood Bank

- **Evidence**: `fjs-config_bloodBank.json` (40 keys), BloodBankServiceImpl `/bloodtestmaster`, PatientRequisitionServiceImpl `/patientrequisition`, `modules/bloodBank/schemas|templates`, `modules/masters/bloodBank/*`, transactionals (t_bloodbag*, t_blooddonor*, t_bloodcrossmatching*, T_BloodBankIndent*), `js/blood-bank-module.js`.
- **Submodules**: Donor registration & screening (voluntary/camp/replacement/autologous), Blood collection & component separation, Testing (bloodtestmaster/result), Cross-matching & issue, Requisitions (patient/external hospital/external storage center/admin/camp), Stock (opening, bag status), Indents between units, Discard, Bag returns, e-Raktkosh linkage.
- **Purpose**: Whole blood & component management: donor to transfusion.
- **Actors**: Blood bank technician, Blood bank officer, Donor counselor, Requisitioning ward, External hospital.
- **Features**: donorRegistartion; VoluntaryDonation/CampDonation/ReplacementDonation/AutologousDonationSchema; bloodComponentSeperationSchema; bloodCrossMatchingListSchema; patient requisitions; cross-unit orders; blood bag returns; discard; indent issue/receive/reject between units; e-Raktkosh data update service (eraktkoshdataupdateservice).
- **Workflow**: Requisition (patient/ward) → donor match/cross-match → issue bag → transfusion record → return/discard; donor cycle: registration → screening → collection → component separation → testing → stock.
- **Statuses**: bag status (bagReturnDetailsSchema, updateReturnedBagStatus, getBagIsCrossmatched), requisition status, discard status, indent status (issueIndent/receiveIndent/rejectIndent/cancelIndent).
- **Dependencies**: M01 (patient/donor search), M08 (requisition orders), M13 (consumables), M04 (billing), M15 (serology? via bloodtestmaster).
- **Tables**: t_bloodbagcollection, t_bloodbagissue, t_bloodbagreturns, t_bloodbagdecross, t_bloodbagissuedemandslip, t_blooddonorregistration(+details), t_donorscreeningtest, t_donormedicalexaminationresult, t_bloodcrossmatchingresult(+details), T_BloodBankIndent(+Details), T_BloodBagExtraction, masters/bloodBank (bloodgroupmaster, bloodcomponentmaster, bloodbagtypemaster, bloodbagtypecomponentmaster, bloodscreeningmaster, bloodtestmaster, bloodtestresultmaster, blooddiscardreasonmaster, blooddonationcampmaster, externalbloodstoragecenter, externalhospitalmaster, eraktkoshcomponentmapping, donordefaultvitalvalue, importerMaster, modeofscarpsale).
- **APIs**: `/bloodtestmaster`, `/patientrequisition`.
- **Screens**: donorRegistartion, bloodComponents, bloodComponentSeperationSchema, bloodCrossMatchingListSchema, patientRequisitionList, showBloodStockListSchema, showBloodDiscardListSchema, showBloodBankIndentListSchema, showBagsReturnListSchema, VoluntaryDonationSchema, CampDonationSchema.
- **Reports**: bloodBankIndentDetails.vm, bloodBagDemandSlip.vm, bloodCrossMatchingreport.vm, bloodDonationCertificate(+New).vm, bagLabel.vm.
- **Integrations**: e-Raktkosh (eraktkoshdataupdateservice, EraktkoshComponentcodeMapping), external storage centers/hospitals.

---

## M18 — Biomedical Waste (BMW)

- **Evidence**: `fjs-config_biomedicalwaste.json` (1 key), BioMedicalWasteManagementServiceImpl `/bioMedicalWasteManagement`, transactionals (BioMedicalWaste, BioMedicalWasteDetails), masters/biomedicalwaste (wastebagmaster), `js/bioMedicalWaste-module.js`.
- **Submodules**: Waste generation & segregation, bag management, status tracking.
- **Purpose**: Biomedical waste tracking (per BMW rules).
- **Actors**: Ward staff, BMW handler.
- **Features**: showBioMedicalWasteStatusSchema; waste details per admission (T_BIOMEDICALWASTEDETAILS).
- **Statuses**: waste status (UNKNOWN-21).
- **Dependencies**: M03 (IPD source).
- **Tables**: BioMedicalWaste, BioMedicalWasteDetails, Wastebagmaster.
- **APIs**: `/bioMedicalWasteManagement`.
- **Screens**: showBioMedicalWasteStatusSchema.
- **Reports**: showBioWasteReportSchema (reports module).

---

## M19 — Linen & Laundry

- **Evidence**: `fjs-config_linenlaundry.json` (1 key), LinenLaundryServiceImpl `/linenLaundry`, `modules/linenlaundry/schemas|templates`, transactionals (LinenRequest(+Details)), masters (linencategory, linendetail), `js/linen-laundry-module.js`.
- **Submodules**: Linen requests, status tracking, laundry processing.
- **Purpose**: Linen requisition & tracking for wards.
- **Actors**: Ward staff, Laundry staff.
- **Features**: showCurrentLinenStatusSchema; LinenRequest with details.
- **Statuses**: linen status (UNKNOWN-22).
- **Dependencies**: M03 (wards).
- **Tables**: LinenRequest(+Details), Linencategory, Linendetail.
- **APIs**: `/linenLaundry`.
- **Screens**: showCurrentLinenStatusSchema.
- **Reports**: linenLaundryReport.vm, showLinenLaundryReportSchema (reports).

---

## M20 — Equipment & Maintenance

- **Evidence**: `fjs-config_equipments.json` (34 keys), EquipmentServiceImpl `/equipment`, PreventiveMaintenanceServiceImpl `/preventivemaintenance`, PreventiveMaintenanceScheduleServiceImpl `/preventivemaintenanceschedule`, ComplaintDetailsServiceImpl `/complaintdetails`, ComplaintResolutionDetailServiceImpl `/complaintresolutiondetail`, `modules/equipments/schemas|templates`, `modules/masters/equipment/*`, transactionals (Equipment, EquipmentDetailDocs, EquipmentWarranty, EquipmentTransfer, EquipmentUsageDetails, EqpComplaint(+Details), EqpComplaintResolution(+Details), EqpPMSchedule(+Details), EqpPreventiveMaintenance, EqiupOpeningBalance), `js/equipment-module` (via equipments schemas).
- **Submodules**: Asset register, Complaints & resolution, Preventive maintenance & calibration, AMC/warranty & insurance, Equipment GRN, Scrap sale, Med-gas indent, Equipment usage.
- **Purpose**: Medical equipment lifecycle: acquisition → maintenance → complaint resolution → disposal.
- **Actors**: Maintenance engineer, Biomedical engineer, Equipment in-charge, Vendor (AMC/service agent), User department.
- **Features**: addNewComplaintSchema; complaint resolution; preventive maintenance scheduling & checklists (electrical/functional); calibration lists; warranty & insurance docs; equipment opening balance; scrap sale; equipment usage; med-gas indent.
- **Workflow**: Complaint → diagnosis → resolution; PM schedule → checklist execution → certification; equipment PO → GRN → asset register.
- **Statuses**: complaint status (complaintstatusmaster), equipment working status (equipworkingstatus), PM status (getPreventiveMaintenanceList), warranty status, UNKNOWN-23.
- **Dependencies**: M13 (item/asset master), M14 (equipment PO/GRN), M04 (charges for equipment services).
- **Tables**: Equipment, EquipmentDetailDocs, EquipmentWarranty, EquipmentTransfer, EquipmentUsageDetails, EqpComplaint(+Details), EqpComplaintResolution(+Details), EqpPMSchedule(+Details), EqpPreventiveMaintenance, masters/equipment (amcagent, serviceagentmaster, insuranceagent, complaintcategorymaster, complaintstatusmaster, complainttypemaster, compliancemaster, maintainancetype, equipchecklist, equipmentpreventivechecklist, functionalchecklistmaster, electricalChecklistmaster, equipworkingstatus).
- **APIs**: `/equipment`, `/preventivemaintenance`, `/preventivemaintenanceschedule`, `/complaintdetails`, `/complaintresolutiondetail`.
- **Screens**: getAssetDetailsSchema, addNewComplaintSchema, complaintDetailsSchema, getCalibrationMaintenanceListSchema, getPreventiveMaintenanceListSchema, showEquipUsageListSearch, getScrapSale, addEqpInsuranceSchema, showEquipMedGasIndentList.
- **Reports**: eqpGrnDetails(+_GST).vm, complaintDetailsView.vm, showEquipmentUsageReportSchema (reports).

---

## M21 — Transport & Ambulance

- **Evidence**: `fjs-config_transport.json` (11 keys), TransportServiceImpl `/transport`, `modules/transport/schemas|templates`, masters/ambulance (ambulancemaster, ambulancetypemaster, accidenttype, driverMaster, vehicleRequestTypeMaster, workshopmaster), transactionals (TransportRequest(+Details), VehicleMaintenance, FuelStationDetails), `js/transport-module.js`.
- **Submodules**: Patient transport requests, Doctor transport, Vehicle allocation & status (in/out), Fuel station, Vehicle maintenance, Transport charges, Trip details.
- **Purpose**: Ambulance & vehicle fleet operations and patient transfer.
- **Actors**: Transport dispatcher, Driver, Workshop, Requisitioning ward/doctor.
- **Features**: patientTransportRequestSchema; allocateVehicle; vehicle in/out; fuel station details; maintenance requests & approval; transport charges; trip details.
- **Statuses**: vehicle status (vehicleStatusSchema), request status (approveTransportRequest/cancelTransportRequest), maintenance status (approve/complete).
- **Dependencies**: M03 (IPD patient transport), M04 (transport charges billing).
- **Tables**: TransportRequest(+Details), VehicleMaintenance, FuelStationDetails, masters/ambulance (ambulancemaster, ambulanceTypeMaster, driverMaster, accidenttype, workshopmaster, vehicleRequestTypeMaster).
- **APIs**: `/transport`.
- **Screens**: patientTransportRequestSchema, doctorTransportRequestSchema, vehicleInSchema, vehicleOutSchema, vehicleStatusSchema, showTripRequestDetails, showFuelStationDetailSchema.
- **Reports**: transportReqReceipt.vm, showVehicleTripReportSchema, showFuelConsumptionReportSchema (reports), ambulanceRegisterReportSchema (reports).

---

## M22 — Grievance

- **Evidence**: `fjs-config_grievance.json` (3 keys), `modules/grievance/schemas|templates`, masters/grievance (grievancecategorymaster, grievancesubcategorymaster, grievanceseveritymaster, grievanceseveritytemplatedetails), transactionals (PatientGrievanceHistory, PatientGrievanceComments, PatientGrievanceFeedback, GrievanceDocuments, ResolutionAsignee), `js/grievance-module.js`.
- **Submodules**: Grievance tickets, escalation/history, resolution & feedback.
- **Purpose**: Patient/staff grievance capture and resolution.
- **Actors**: Grievance officer, Patient, Assignee.
- **Features**: raiseGrievanceTicketSchema; grievance history; severity templates; resolution assignee.
- **Statuses**: grievance status via history (UNKNOWN-24).
- **Dependencies**: M01 (patient), user module (assignee).
- **Tables**: PatientGrievanceHistory, PatientGrievanceComments, PatientGrievanceFeedback, GrievanceDocuments, ResolutionAsignee, masters/grievance.*
- **APIs**: (grievance endpoints via custom service — UNKNOWN-25; screens in fjs-config_grievance).
- **Screens**: raiseGrievanceTicketSchema, grievanceSchema, grievanceHistorySchema.

---

## M23 — RMRS (Financial/Fund Management)

- **Evidence**: `fjs-config_rmrs.json` (9 keys), RMRSServiceImpl `/RMRS`, `modules/rmrs/schemas|templates` (~35 templates), masters/rmrs (authoritymaster, headsmaster, attendantpassserviceratemaster, rmrsreasonmaster), transactionals (CollectionHeader, CollectionDetail, RmrsCollection, RMRSDashboardTransaction, SuspenseAccountDetails, TUserHospitalAuthority), `js/rmrs-module.js`.
- **Submodules**: Authorities (contracts, EMI), Heads (account heads), Payment collections, Issued payments, Journal entries, Suspense accounts, Bank statements, PO requests, Dashboards.
- **Purpose**: Fund/authority management & accounting — likely for scheme/authority payments (e.g., government scheme funds).
- **Actors**: RMRS authority, Accounts officer, Cashier, Collector.
- **Features**: addAthorities; addCollection; IssueCollection; approveOrRelease; journal entries; bank statement; suspense account; plan & account-wise reports; authority EMI details.
- **Workflow**: Authority contract → collections (from authority) → issue payments → journal → reconcile.
- **Statuses**: transaction status list (getAllTransactionStatusList), collection status, issue status, UNKNOWN-26.
- **Dependencies**: M04 (billing payment linkage), M14 (PO requests), Masters (hospital accountheadmaster, bankmaster).
- **Tables**: CollectionHeader, CollectionDetail, RmrsCollection, RMRSDashboardTransaction, SuspenseAccountDetails, TUserHospitalAuthority, masters/rmrs.*
- **APIs**: `/RMRS`.
- **Screens**: AddCollection, PaymentCollection, IssueCollection, RMRSDashboard, suspenseAccountDetailsSchema, showRMRSPurchaseOrderRequestListSchema.
- **Reports**: rmrsIssuedPaymentReceipt.vm, authorityBillDetailsSummeryReceipt.vm, authorityBillPaidReceipt.vm, printBankStatement.vm, printJournalConfirmReceipt.vm, planAndAccountWiseReport.vm, summaryAccountWiseReport.vm, printReportByDocumentNumber.vm, ExpenditureReport.vm.

---

## M24 — PMRD (Patient Medical Record Department)

- **Evidence**: `fjs-config_pmrd.json` (2 keys), PmrdServiceImpl `/pmrd`, `modules/pmrd/templates` (formj1, formj2, formj3, hform, patientEncounterRecord, patientMedicalRecordWrap, uploadPatientDocumentsPmrd), `js/pmrd-module.js`.
- **Submodules**: Encounter record (IEN), Medical record documents, Form J1/J2/J3 (JSY scheme forms), Uploads.
- **Purpose**: Medical records management & scheme documentation.
- **Actors**: MRD clerk, Records officer.
- **Features**: getAllPatientEncounter; getPatientDocumentsForPMRD; getJsyFormDetails; uploads.
- **Dependencies**: M01 (encounters), M08.
- **Tables**: (PMRD uses encounters + documents — UNKNOWN-27 specific tables).
- **APIs**: `/pmrd`.
- **Screens**: pmrd schemas (2); templates formj1/2/3, hform.
- **Reports**: hform.vm (in templates root: patientEncounterRecord.html etc.).

---

## M25 — Reports & BI

- **Evidence**: `fjs-config_reports.json` (46 keys), report-module.js, `modules/reports/schemas|templates`, statistical models (ServiceWiseCollection, UserWiseCollection), `fjs-config_biReport.json` (report_BI.html, report_BI_DM.html), root templates generic_reports.vm, showAgeWiseRegistrationReport.vm, etc.
- **Submodules**: Standard reports (registration, admissions, discharges, collection, OPD statistics, IPD statistics, death, ambulance, blood bank, bio-waste, CSSD, linen, lab), BI dashboard.
- **Purpose**: Reporting & analytics.
- **Actors**: Hospital management, Department heads, Statistician.
- **Features**: 46 report schemas incl. servicewise/userwise collection, userwise summary, IPD/OPD statistics, blood bank registers.
- **Dependencies**: All modules (data source).
- **Tables**: ServiceWiseCollection, UserWiseCollection (aggregates).
- **APIs**: `/masterConfigService` (getAllDataForReport on /helper), report endpoints in `js/report-module.js`.
- **Screens**: all report schemas under reports/.
- **Reports**: generic_reports.vm, servicewiseCollectionReport.vm, userwiseCollectionReport.vm, showAgeWiseRegistrationReport.vm, showLabReport.vm, showBioWasteReportSchema, showCssdReportSchema, showLinenLaundryReportSchema.

---

## M26 — Worklist & Task Management

- **Evidence**: `fjs-config_worklist.json` (nurseTaskList), `modules/worklist/schemas|templates` (~30 templates), transactionals (NurseTaskDetails, NurseTaskFrequencyDetails), `js/nursing-module.js`, docit-roles.js (roleNurse worklists).
- **Submodules**: Nurse task lists (task-wise/patient-wise), goals, interventions, meds, labs, radiology, consults/procedures, ward lists.
- **Purpose**: Task-driven nursing work.
- **Actors**: Staff nurse, Nurse in-charge.
- **Features**: nurseTaskList; completeNursingOrderTask; unmetGoals; roleNurseWorkList(Home/AssignView/Goals/Intervention/Meds/Orders/PatientWise/TaskWise).
- **Statuses**: task status, goal status (unmetGoals).
- **Dependencies**: M09, M07, M15, M16, M18.
- **Tables**: NurseTaskDetails, NurseTaskFrequencyDetails.
- **APIs**: `/nursingList`, `/clinical`.
- **Screens**: nurseTaskList.json.
- **Reports**: taskListPatientwise (template).

---

## M27 — Queue Management (OPD & Nursing Desk)

- **Evidence**: `fjs-config_queue.json` (11 keys), QueueManagmentServiceImpl `/queueManagment`, `modules/queue/schemas|templates`, models/trx (RegistrationQueue), transactionals (PatientQueue), `js/queue-module.js`.
- **Submodules**: Appointment queue, Cabin queue, Doctor batch slots, Token management, Nursing desk, User queues, Questionnaires.
- **Purpose**: Patient flow control in OPD (tokens, cabin, doctor assignment).
- **Actors**: Queue manager, Nursing desk, Doctor, Reception.
- **Features**: appointmentQueueSchema; manageTokenSchema; doctorBatchSlotSchema; nursingDeskForQueue; addpatientPhoto; questionnaireSchema (pre-consult questionnaire).
- **Workflow**: Arrival → token → questionnaire → cabin queue → doctor assignment (by nurse or system) → consultation.
- **Statuses**: queue status (callGetPatientListSchema, assignNextPatientToDoctor).
- **Dependencies**: M02, M09 (nurse desk), M01.
- **Tables**: PatientQueue, RegistrationQueue, QuestionAnswerDto.
- **APIs**: `/queueManagment`, `/doctor` (queue endpoints).
- **Screens**: appointmentQueueSchema, cabinQueueListSchema, doctorBatchSlotSchema, manageTokenSchema, nursingDeskForQueue, questionnaireSchema, userQueueSchema.
- **Reports**: token_sheet.vm (OP token).

---

## M28 — Mortuary & Death

- **Evidence**: commons schemas (mortuarySchema, mortuaryClearanceSchema, mortuaryBoxManagement, mortuaryDischargedPatientSchema), PatientServiceImpl (`/patient` getMortuaryPatients, getPatientDetailsForDeathForm, updatePatientDeathDetails), templates (death_form.vm, death_Certificate.vm, DeathReasonForm.vm, PatientDeathForm.vm, Post-MortomReport.vm), transactionals (ChildDeathReview), tiuxml (Post_Mortem, Death_Certificate, DEATHNOTE).
- **Submodules**: Mortuary box management, death certificate, post-mortem, child death review (CDR 4A/4B).
- **Purpose**: Death documentation, mortuary custody, certification.
- **Actors**: Mortuary attendant, Doctor, RMO, Records.
- **Features**: getMortuaryPatients; mortuary clearance; death certificate print; CDR forms (cdr_form_4A/4B.vm).
- **Statuses**: mortuary status (mortuaryClearanceSchema), death certificate status, UNKNOWN-28.
- **Dependencies**: M03 (deceased patient), M01.
- **Tables**: ChildDeathReview, PatientDischargeData (death), masters (death-related masters UNKNOWN-29).
- **APIs**: `/patient` (mortuary/death endpoints).
- **Screens**: mortuarySchema, mortuaryClearanceSchema, mortuaryBoxManagement.
- **Reports**: death_form.vm, death_Certificate.vm, PatientDeathForm.vm, Post-MortomReport.vm, cdr_form_4A.vm, cdr_form_4B.vm.

---

## M29 — Chemotherapy

- **Evidence**: `fjs-config_chemotherapy.json` (3 keys), `modules/chemotherapy/schemas|templates`, `js/chemotherapy-module.js`, chemo_tiuxml_mapping.json (Chemo_Performance_Status_Assessment, Chemo_SURGONCOHIST/EXAM), tiuxml (chemo forms).
- **Submodules**: Chemo patient list, chemo planning, patient coversheet.
- **Purpose**: Chemotherapy treatment planning & tracking (oncology).
- **Actors**: Oncologist, Chemo nurse, Pharmacist (IV compounding).
- **Features**: showChemoPatientListSchema; showChemoPlanningSchema; chemo coversheet.
- **Dependencies**: M01, M03, M07 (IV drugs), M08.
- **Tables**: (chemo uses careplan/medication tables — UNKNOWN-30).
- **APIs**: (chemotherapy endpoints — UNKNOWN-31).
- **Screens**: showChemoPatientListSchema, showChemoPlanningSchema, showChemotherapyPatientCoversheetSchema.

---

## M30 — BCMA (Bar-Coded Medication Administration)

- **Evidence**: `fjs-config_bcma.json` (11 keys), `modules/bcma/schemas|templates`, transactionals (IHR_MEDICATION_TMP, T_INFUSIONMIXING(+DETAILS)), `js/bcma-module.js`.
- **Submodules**: Medication log, IVP tab, Unit dose tab, Unable-to-scan handling, Missing dose requests, Dermal site, Cover sheet.
- **Purpose**: Bedside barcode verification for medication administration.
- **Actors**: Administering nurse, Pharmacy (missing doses).
- **Features**: showMainScreenSchema; PatientMedicationLogSchema; missingDoseRequestSchema; iVPUnableToScanSchema; showMedLogHeldSchema.
- **Workflow**: Scan patient/med barcode → verify (5 rights) → administer → log → handle exceptions (unable to scan, held, missing dose).
- **Statuses**: med log status (held), missing dose status, UNKNOWN-32.
- **Dependencies**: M07 (meds), M09 (nursing), M13 (stock).
- **Tables**: IHR_MEDICATION_TMP, T_INFUSIONMIXING(+DETAILS), T_PRESCRIPTIONDISPENSEDETAILS.
- **APIs**: (bcma endpoints — UNKNOWN-33; schemas in fjs-config_bcma).
- **Screens**: ShowMainScreenSchema, ShowUDTab, ShowIVPTab, ShowBcmaItemStock, PatientMedicationLogSchema, ShowUnableToScanSchema, MissingDoseRequestSchema.
- **Reports**: (BCMA labels via pharmacy printMedLabel).

---

## M31 — Kiosk (Self-Service)

- **Evidence**: `fjs-config_kiosk.json` (4 keys), `modules/kiosk/schemas|templates`, PatientServiceImpl (/patient kioskQueue, saveKioskLabResult, uploadKioskLabResult), `js/queue-module.js` (kiosk).
- **Submodules**: Self-registration, appointment search, account search, queue.
- **Purpose**: Patient self-service at kiosks (registration/appointments).
- **Actors**: Patient (self-service).
- **Features**: selfAppointmentSchema; showSelfRegPatientSchema; accountSearchSchema; kiosk queue; kiosk lab result upload (COVID testing kiosk).
- **Dependencies**: M01, M02, M27.
- **APIs**: `/patient` (kioskQueue, saveKioskLabResult, uploadKioskLabResult).
- **Screens**: selfAppointmentSchema, showSelfRegPatientSchema, accountSearchSchema, appointmentSearchSchema.

---

## M32 — RCM (Revenue Cycle Management / Scheme Billing)

- **Evidence**: `fjs-config_rcm.json` (1 key rghsEligibilityCheckSchema), RcmServiceImpl `/rcm`, `modules/rcm/schemas|templates` (chiranjeeviapp, patientVerificationDetails, rghsEligibilityDetail), models/rcm (2 entities), `js/rcm-module.js`.
- **Submodules**: RGHS eligibility check, DM approval upload, MOIC upload, Patient photo upload, Discharge verification, Chiranjeevi app.
- **Purpose**: Government scheme (RGHS/Chiranjeevi) eligibility & claim documentation.
- **Actors**: RCM officer, DM/MOIC (approvals), Hospital admin.
- **Features**: rghsEligibilityCheckSchema; dmApprovalUploadSchema; moicUploadSchema; patientPhotoUploadSchema; dischargeVerificationSchema.
- **Dependencies**: M01, M03 (discharge), M04.
- **Tables**: models/rcm (2 — RGHS metadata, UNKNOWN-34).
- **APIs**: `/rcm`, `/patient` (getRghsTransactionsforIHMS, getRghsDocumentByTransactionId, getSimsRghsforIHMS).
- **Screens**: rghsEligibilityCheckSchema, dischargeVerificationSchema, dmApprovalUploadSchema, moicUploadSchema.
- **Reports**: rghsTID.vm, documentUploadRghsReq.vm, opdMetadataForRghs.vm, rghsSimsData.vm, chiranjeeviEmrTID.vm.
- **Integrations**: RGHS/SIMS (Rajasthan govt), Chiranjeevi, NHCX.

---

## M33 — Payment Transfer

- **Evidence**: `fjs-config_paymentTransfer.json` (3 keys), payment/PaymentTransferServiceImpl `/paymentTransfer`, `modules/paymentTransfer/schemas|templates`, transactionals (T_PAYMENTTRANSFER, T_PAYMENTTRANSFERREQUEST(+DETAIL)), `js/payment-transfer-module.js`.
- **Submodules**: Payment transfer requests, incentive claims, OTP verification, bank accounts.
- **Purpose**: Digital payment transfers & incentive claims (e.g., ASHA incentives).
- **Actors**: Finance, ASHA/incentive claimant, Bank.
- **Features**: paymentTransferListSchema; incentiveClaimInfoSchema; generateOTPSchema.
- **Statuses**: payment transfer status (paymentTransStatus template).
- **Dependencies**: M04, M36 (EJS incentives), RMRS.
- **Tables**: T_PAYMENTTRANSFER, T_PAYMENTTRANSFERREQUEST(+DETAIL), masters/hospital (bankmaster, bankbranchmaster, bankaccountmaster).
- **APIs**: `/paymentTransfer`.
- **Screens**: paymentTransferListSchema, incentiveClaimInfoSchema, generateOTPSchema.

---

## M34 — File Management & Document Upload

- **Evidence**: `fjs-config_fileManagement.json` (1 key), FileManagementServiceImpl `/fileService`, UploadFileServiceImpl `/uploadFile`, UploadHelperServiceImpl `/upload`, commons uploadDocumentSchema/uploadFileSchema/amendmentFileUpload, transactionals (PatientDocuments, PathoPatientDocs, GrievanceDocuments, EquipmentDetailDocs, Documents), `js/file-management-module.js`.
- **Submodules**: Document upload (patient, lab, grievance, equipment), amendment uploads.
- **Purpose**: Document capture & retrieval across modules.
- **Actors**: All users (per module).
- **Dependencies**: All modules.
- **Tables**: Documents, PatientDocuments, PathoPatientDocs, GrievanceDocuments, EquipmentDetailDocs, PathOrderBookingDetailDocuments.
- **APIs**: `/fileService`, `/uploadFile`, `/upload`.
- **Screens**: uploadDocumentSchema.

---

## M35 — User, Role & Notification Management

- **Evidence**: UserManagementServiceImpl `/usermanagement`, AddUserServiceImpl `/addUserService`, SendNotificationServiceImpl `/sendEmail`, `modules/masters/hospital/schemas` (addUserSchema, getUserDetailsSchema, tuserrolemaster, modulenamemaster, emailtemplate, emailTemplateDetails, notificationTemplateSchema), masters (TUserdetails, TUserrolemaster, TUserStoreDetails, TUserWardDetails, TUserBuildingDetails, TUserunitdetails, ModulePermissionMaster, Menumaster), `js/user-module.js`, `js/notification-template-module.js`.
- **Submodules**: Users & roles, permissions, notifications (email/SMS), notification templates, menu/module config.
- **Purpose**: Identity, access control, communication.
- **Actors**: System admin, users.
- **Features**: addUserSchema; user role master; email templates; notification templates; user-store/ward/unit mapping; shift details.
- **Dependencies**: All modules.
- **Tables**: TUserdetails, TUserrolemaster, TUserStoreDetails, TUserWardDetails, TUserBuildingDetails, TUserunitdetails, TUserHospitalAuthority, UserDistrictDetails, UserStateDetails, UserSpecialityDetails, ModulePermissionMaster, Menumaster, ModuleNameMaster, EmailTemplate(+Details), NotificationTemplate.
- **APIs**: `/usermanagement`, `/addUserService`, `/sendEmail`, `/actionNotif`.
- **Screens**: addUserSchema, getUserDetailsSchema, tuserrolemaster, notificationTemplateSchema, updateUserSchema.

---

## M36 — EJS (e-Jan Swasthya / ASHA-ANM field app)

- **Evidence**: services/ejs/* (9 impls: EjsAsha, EjsAncPncChild, EjsChecklists, EjsDetails, EjsIncentive, EjsMasterLocation, EjsPopulation, EjsSupervisor), models/ejs (61 entities: ANDROID_ANC, ANDROID_PNC, ANDROID_CHILD_CARE, ANDROID_ASHA_WORK, ANDROID_FAMILY, ANDROID_ATTENDANCE, ANDROID_INCENTIVES, ES_CHECKLISTS*, MASTER_* geography, CORONA_*), masters/ejs (ejschecklistquestions, ejsmasteractivity, ejsmasterentity), `modules/masters/ejanswasthya` (media), `js/vista-module.js` (unverified).
- **Submodules**: ASHA work, ANC/PNC/child care tracking, checklists & submissions, incentives, population registers, supervisor approvals, geo masters.
- **Purpose**: Community health worker (ASHA/ANM) field data collection & incentive management.
- **Actors**: ASHA, ANM, Supervisor, District admin.
- **Features**: ejs-asha; ejs-ancpncchild; ejs-checklists; ejs-incentive; ejs-population; ejs-supervisor; ejs-masterLocation.
- **Statuses**: checklist submission/approval status, incentive status, UNKNOWN-35.
- **Dependencies**: M33 (incentive payments), geo masters.
- **Tables**: models/ejs (61): ANDROID_* (ANC, PNC, MC, ECS, MCHN, MEMBERS, FAMILY, ASHA_WORK, WORK_DETAILS, REFER, SCHOOL, DEATH, DISEASES, INCENTIVES, HELP, SUPERVISOR_*, ATTENDANCE, CBAC, CHILD_CARE, CHILD_CARE_CHECKUP, FAMILY_PLANNING, FAMILY_AWC, ABORTION), ES_CHECKLISTS(+SUBMISSIONS+QUESTIONS+QUES_ANSWERS), MASTER_* (STATES, DISTRICTS, BLOCKS, GRAMPANCHAYATS, VILLAGES, ZONES, PHCS, SUBCENTERS, AWCS), USER_, USER_TYPES, USER_DESIGNATION, PROFILE, INDICATORS, MEDIA(+CATEGORIES+TYPES), CORONA_FAMILY/MEMBERS_DETAILS, MASTER_ACTIVITY, MASTER_ENTITY.
- **APIs**: `/ejs-asha`, `/ejs-ancpncchild`, `/ejs-checklists`, `/ejs-details`, `/ejs-incentive`, `/ejs-masterLocation`, `/ejs-population`, `/ejs-supervisor`.
- **Screens**: ejschecklistquestions, ejsmasteractivity, ejsmasterentity, media.

---

## M37 — Aasmaan (Field App Backend)

- **Evidence**: services/aasmaan (10 impls: AasmaanAppTransactionLog, AasmaanCase, AasmaanDevice, AasmaanETicket, AasmaanInstitution(+Level), AasmaanReport, AasmaanRscCallLog, AasmaanStat, AasmaanUser), models/aasmaan (35 entities, no @Table annotations — mobile app DTOs), `js/queue-module.js` (aasmaan? UNKNOWN-36).
- **Submodules**: Cases, devices, e-tickets, institutions, RSC call logs, stats, users, reports.
- **Purpose**: Backend for a field/mobile application (case & device management).
- **Actors**: Field workers, RSC staff, Institution admins.
- **Features**: CRUD APIs for cases/devices/tickets/institutions; stats & reports.
- **Dependencies**: None in-code (standalone).
- **Tables**: (no JPA @Table — mobile app persistence, UNKNOWN-37).
- **APIs**: `/cases`, `/devices`, `/e_tickets`, `/institutions`, `/institution_levels`, `/rsc_call_logs`, `/stats`, `/users`, `/app_transaction_log`.

---

## M38 — Integrations (ABDM/ABHA, NHCX/HCX, EHR, e-Aushadhi, Gov Schemes)

- **Evidence**: services/abdm (AbhaServiceImpl `/abha`, HPRServiceImpl `/hpr`, NHCXServiceImpl `/nhcx`), services/ehr (EHRUtilServiceImpl `/ehr`), services/aadhar (templates), com/ohum/xcg (abdm server+client: CallbackAPI, ABDMController, HFRController, HIPUtil, HIUUtil, HPRUtil, PHRUtil, HealthIdUtil, AbhaUtil, CommonABDMUtil, FhirDataUtil; genserv: FhirServer, PatientResourceProvider, PatientTransformer, RbacSecurityInterceptor, TokenBasedSecurityInterceptor; hcx: CheckCoverageEligiblityRequestDto, PreAuthBundleRequest, HCX* payload encrypt/decrypt, InsurancePlanRequest, NHCXServiceHelper), templates/nhcx (claim/preauth/eligibility/plan templates + FHIR bundles), InventoryEAushadiIntegrationImpl `/eAushadhiIndent`, IntegrationServiceImpl `/integration`, MdmServiceImpl `/mdm`, transactionals (Inboundhis, Outboundhis, EHRJobDetails, EmrDetails, EmrServices, EventSubmission, TeleConsultantDetails).
- **Submodules**: ABDM (ABHA, HIP/HIU, HPR, PHR), NHCX/HCX (insurance eligibility/pre-auth/claim), EHR sync (inbound/outbound HIS), e-Aushadhi (indents, stock, QC, returns), Aadhaar (biometric/OTP), Janaadhar/Bhamashah, RGHS/SIMS, Chiranjeevi, KDL (lab instruments), MDM (Janta master data).
- **Purpose**: National/state digital health & procurement interop.
- **Actors**: System (integration), Nodal officers.
- **Features**: ABDM patient queue (showAbdmPatientQueue); HCX FHIR bundles; EHR job execution (EHRJobDetails); inbound/outbound HIS queues; e-Aushadhi indent issue/QC/return status; aadhaar biometric auth templates.
- **Statuses**: EHR job status, inbound/outbound message status, ABDM consent/notification status, UNKNOWN-38.
- **Dependencies**: M01 (ABHA), M04/M32 (NHCX claims), M13 (e-Aushadhi), M15 (KDL).
- **Tables**: Inboundhis, Outboundhis, EHRJobDetails, EmrDetails, EmrServices, EventSubmission, TeleConsultantDetails, masters (EraktkoshComponentcodeMapping for e-Raktkosh).
- **APIs**: `/abha`, `/hpr`, `/nhcx`, `/ehr`, `/integration`, `/mdm`, `/eAushadhiIndent`, `/machineConfig`.
- **Templates**: templates/nhcx/* (claim_on_submit, coverage_eligibility_on_check, insurance_plan_on_request, preauth_on_submit + Claim_onSubmit_Bundle.json, EligiblityCheck_onCheck_Bundle.json, InsurancePlan_FHIR_Bundle.json, PreAuth_onSubmit_Bundle.json), aadhar_*vm, janaadhar_*vm, bhamashah_*vm, rghs*.vm.

---

## M39 — Mortuary & Other Commons (shared/utility) — merged into M28; Commons platform services

- **Evidence**: `fjs-config_commons.json` (67 keys): audits (auditSchema, newAuditSchema), config (configapplication, applicationModuleStatus, hospitalCreationSchema, hospitalGeoTagging, machineConfigSchema), staff/doctors/user-pass (staffmaster, doctormaster, tuserrolemaster, addPatientUserPassSchema, OutreachUserCreationSchema), shifts & rosters (shiftDetails-Schema, shiftRoster-Schema, userShiftDetailsListSchema, addIpdRosterSlotsSchema), inbound/outbound (inbound.json, outbound.json), notifications (notificationTemplateSchema), verification (firstVerificationSchema, secondVerificationSchema, boxCommonVerification), mortuary (mortuary*), corona (patientCoronaDetailsSchema, showCoronaPatientReport), MRN series (mrnoseries), schemas designer (schemasDesigner), uploads (uploadDocumentSchema, uploadFileSchema, amendmentFileUpload), service master mapping (servicemastermapping, uploadServiceMappingSchema), procedure list for OT, questionnaires (questionaireSocialWorker), items (itemSaleDetailpaymentSchema), backGroundTasks, appDocumentation.
- **Purpose**: Platform-level shared services used by all modules.
- **Actors**: System admin, all users.
- **Tables**: masters (ConfigApplication, ConfigAutocharges, TUserdetails, Staffmaster, Doctormaster, MrnSeries, AuditEventMaster, MachineConfiguration), transactionals (MachineConfiguration, T_QUESTIONANSWER...).
- **APIs**: `/masterConfigService`, `/helper`, `/machineConfig`, `/mdm`, `/integration`.
- **Screens**: auditSchema, configapplication, hospitalCreationSchema, shiftRoster-Schema, mrnoseries, schemasDesigner, mortuarySchema, notificationTemplateSchema.

---

## M40 — TIU/Clinical Forms Engine (tiuxmls)

- **Evidence**: `fjs-config_tiuxmls.json` (368 keys), `modules/tiuxmls/schemas|templates` (1:1, 368 each), tiuxml_mapping.json, chemo/nhnc/op_tiuxml_mapping.json, tiuxml.js.
- **Submodules**: Form families (see findings §7): History/Exam/Assessment per specialty, Nursing documentation, Risk scales, Procedure checklists, Emergency/trauma, Discharge/advice, Handover, Social work, Family welfare, Death/autopsy, COVID.
- **Purpose**: Digitized clinical forms (TIU-style) rendered in patient record; specialty-specific documentation.
- **Actors**: Physicians, Nurses, Allied health (AH/AL), Social workers.
- **Features**: 368 forms; gender/age-based mapping (tiuxml_mapping gender/fromage/toage); template flags; versioned forms (_V10.._V15, _new, _P print, _B, _AH, _AL, _nhnc, _risl variants).
- **Statuses**: form-specific statuses UNKNOWN-39.
- **Dependencies**: M09 (nursing), M01 (patient), M12 (OT forms), M08 (notes).
- **Tables**: (forms stored via note/document tables — UNKNOWN-40).
- **APIs**: (forms served via tiuxml.js / note endpoints).
- **Screens**: 368 schema keys (Acuity2, Braden_Scale, Barthel_Index_New, NURSINGINITIALASSESSMENT_V*, OBSHIST_V11, PEDS_EXAMINATIONS_V2, HANDOFF_COMMUNICATION_V13, Post_Mortem_Report, Death_Certificate, ...).
- **Reports**: plainText variants + templates 1:1.

---

## Cross-module dependency table

| Module | Depends on |
|---|---|
| M01 Patient | Masters, M38 ABDM, Billing (MRN) |
| M02 OPD | M01, M08, M29 Queue, M04, Masters |
| M03 IPD | M01, M02, M04, M10, M15, M18, M28, M11 |
| M04 Billing | M01–M03, M05, M07, M17, M32, M33, M06 |
| M05 Billing Masters | M04, M17, M32 |
| M06 Doctor Share | M04, M01 |
| M07 Pharmacy | M13, M04, M15, M08, M34 BCMA |
| M08 Clinical Orders | M04, M16, M21, M07, M15 |
| M09 Nursing | M03, M07, M08, M16, M21, M26, M29, M40 |
| M10 Diet | M03, M04 |
| M11 CSSD | M13, M12, M04 |
| M12 OT | M03, M08, M04, M11, M13, M07 |
| M13 Inventory | M14, M07, M11, M12, M04 |
| M14 Procurement | M13, M04, M23 |
| M15 Pathology | M08, M01, M04, M13, M17 |
| M16 Radiology | M08, M04, M01 |
| M17 Blood Bank | M01, M08, M13, M04, M16 |
| M18 BMW | M03 |
| M19 Linen | M03 |
| M20 Equipment | M13, M14, M04 |
| M21 Transport | M03, M04 |
| M22 Grievance | M01, M35 |
| M23 RMRS | M04, M14, M33, Masters |
| M24 PMRD | M01, M08 |
| M25 Reports | All |
| M26 Worklist | M09, M07, M15, M16 |
| M27 Queue | M02, M09, M01 |
| M28 Mortuary/Death | M03, M01 |
| M29 Chemo | M01, M03, M07, M08 |
| M30 BCMA | M07, M09, M13 |
| M31 Kiosk | M01, M02, M27 |
| M32 RCM | M01, M03, M04, M38 |
| M33 Payment Transfer | M04, M36, M23 |
| M34 Documents | All |
| M35 Users/Notify | All |
| M36 EJS | M33, geo masters |
| M37 Aasmaan | none (standalone) |
| M38 Integrations | M01, M04, M13, M15, M32 |
| M39 Commons | All (platform) |
| M40 TIU Forms | M09, M01, M12, M08 |

## Open questions (for later agents)

1. **Status code semantics**: status fields exist (indent/PO/GRN/surgery/admission/discharge/bag/requisition/queue/task...) — agent 06 should extract code lists from DB seed scripts, not code.
2. **Docit permissions matrix**: role→screen→action mapping is DB-driven (`ModulePermissionMaster`); not visible in code — needs DB dump analysis.
3. **VISTA & CWF/SCWF**: `vista-module.js`, `cwf-module.js`, `scwf-module.js` and CWF/SCWF masters exist; semantics (clinical workflow engine vs. config) need service-level reading.
4. **Rule engine**: masters/ruleengine (ruleSet.json) + RuleEnginHelper — rules purpose/lifecycle unknown.
5. **Careplan/charting/shifthandover ownership**: template-only modules — confirm whether they're features of Nursing (M09) or standalone.
6. **BiReport**: report_BI.html/report_BI_DM.html — BI tool integration (which tool?) unknown.
7. **Aasmaan & models/txt**: no @Table annotations / not enumerated — check purpose & DB mapping.
8. **Kiosk scope**: includes lab result upload — confirm kiosk use cases (COVID testing?).
9. **NHNC variant forms** (walkinVisitNhnc, ExaminationNHNC, OBSEXAM_V102_nhnc...): "NHNC" = Non-Hospital Nursing Centre? confirm program context.
10. **e-Raktkosh & KDL**: integration direction, sync frequency, and failure handling not in code.
11. **Screens vs. APIs**: many screens (e.g., grievance) lack an obvious service @Path — map via frontend JS to confirm endpoints.
