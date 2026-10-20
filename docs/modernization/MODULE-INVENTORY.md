# iHMS / OHUM PAS NG — Master Module Inventory (M01–M40)

> **Synthesis document.** Consolidates: `02-business-module-catalog.md` (40 modules — primary source), `_discovery/agent-02/findings.md` (evidence), `03-legacy-frontend-inventory.md` (routes), `04-backend-architecture.md`, `04-api-catalog.md`, `05-database-catalog.md`, `06-business-rules-catalog.md`, `07-integrations-and-search.md`, `08-security-rbac-audit.md`, `10-target-architecture.md`, `_discovery/reconciliation-notes.md`.
> Every claim is traceable to a source doc. Fields with no evidence are marked **UNKNOWN — no evidence**; nothing is invented.
> Legend: **Screens** = FAUG schema keys (fjs-config/schemas). **Routes** = `#module:screen/:params` hash routes (agent-03). **APIs** = CXF class-level `@Path`. **BR** = business-rule IDs from `06` (BR-PAT/OPD/IPD/DIS/BILL/PHAR/PATH/OT/BLD). **Target service** = candidate bounded context from `10-target-architecture.md` §2.1. **CIMS_*** = Oracle stored procedures.
> **Known numbering inconsistency in source `02`:** several per-module "Dependencies" lines and a few cross-module-table rows use a shifted module numbering (e.g. "M08 (billing)", "M15 (nursing)", "M17 (pharmacy bill)", "M34 BCMA"). Corrected below to heading numbers; original text flagged in the conflicts section.

---

## M01 — Patient Management (Registration & Demographics)

- **Target service:** 1. Patient Identity & Registration (10 §2.1)
- **Purpose:** Patient identity, demographics, registration, encounter history, verification (Janaadhar, ABHA, Aadhaar, biometric), MLC capture, mother–child linking.
- **Business Context:** Core registry feeding all modules; multi-unit tenancy (UnitID); schemes (BPL/insurance/RGHS/Janaadhar/Bhamashah); ABDM/ABHA linkage.
- **Actors:** Registration clerk, Front office, Social worker, Admin (merge IDs), Patient (self-reg).
- **Submodules:** Registration (OPD/IPD); Encounter & visits; MLC details; Mother–child linking; Patient categories; Social worker assessment; Corona/COVID quarantine tracking; Teleconsult room creation.
- **Features:** registerPatient; demographic search (Aadhaar/driving licence/Janaadhar); MLC details + documents; ABHA/ABDM matching & ABHA keying; HID merge; family member details; corona tracking (patientCoronaDetails, geofenceViolated); MRN generation (series / temp MRN).
- **Screens:** addpatientDetailRegdto-schema, patientRecordSchema, mlcDetailsSchema, motherChildLinkingSchema, addSocialWorker, patientCoronaDetailsSchema.
- **Routes:** `op:patientDetails/:allowEdit`, `op:patientHistory/:mrno`, `op:editPatientDetail/:mrnNo/:unitId`, `commons:patientList`, `commons:patientHistoryList`, `commons:patientRecord`, `commons:updatePatient`.
- **Backend APIs (service @Path):** `/patient` (registerPatient, getPatientByAddhar, getPatientByDrivelicNo, mergeHId, searchPatientForABDM, getPatientDemographicDetails, getPatientRecord, kioskQueue, saveKioskLabResult, uploadKioskLabResult, mortuary/death endpoints), `/patientDetailReg`, `/visit`, `/markandaddvisit`, `/helper` (registerChild, registerChildDocit).
- **Business Rules (BR-* IDs):** BR-PAT-001 (MRN generation), BR-PAT-002 (duplicate identity detection — BERA011/BEXA040), BR-PAT-003 (family-Id/member coupling — BERP007).
- **Status Lifecycle:** Visit status fields on Encounter/Visit (VisitStatus codes 3/9/10/11/12 — see M02); MLC status fields (mlcDetailsSchema); UNKNOWN-11 exact code lists.
- **Database Tables:** T_Encounter (52 refs — highest-shared table), T_REGISTRATION / T_REGISTRATION_DETAILS / T_REGISTRATION_INT (native SQL only — **no entity mapping**), MLCDetails, RegistrationQueue, FamilyMemberDetails, SpouseAndChildrenDetails, PatientHqDetails, PatientDemoDetailsMismatch, T_ANCVISITDETAILS, T_PATIENTSPONSOR, T_PATIENT_ABDM(+_CC, _SHARETOKEN), T_PATIENT_CONSENT_REQUESTS_ABDM, T_USER_FACEAUTH, masters/patient (~50 M_* demographics masters), M_MRNSeries, M_UNITMASTER.
- **Stored Procedures (CIMS_*):** CIMS_ADDPATIENTREGISTRATION* (10 §2.1 row 1); `cims_getlatestautocodeandincremetitbyone` (temp-MRN auto-code).
- **Integrations:** ABDM/ABHA (searchPatientForABDM, addPatientAbhNumberKey), Aadhaar biometric/OTP, Janaadhar, Bhamashah, Chiranjeevi (getChiranjeeviDetailByJanAdhar), Solr search, e-Mitra (kiosk), Jeevanvahini (108), Mera Aspataal, RGHS/SIMS.
- **Solr Dependencies:** **Heavy.** patient collection realtime index (PatientRegAdmissionServiceImpl:875,1180,1662; PatientServiceHelper:761,4363,4481); searchPatientInSolr, searchExactPatientInSolr (dup guard), searchPatientInSolrByIdentity, getPatientByMrno (Redis-first), searchABHAPatientHIDInSolr (ABDM); nightly indexMissingPatientsInSolr backfill; APO rollback compensation deletes Solr docs.
- **Redis Dependencies:** `current_pat_<id>/<mrno>` (100s TTL mirror), `ABHANUM_<healthIdNumber>`, `gender_prefix_maritalstatus_map`, `CDD_<opdipdid>` discharge lock.
- **Scheduled Jobs:** indexMissingPatientsInSolr (nightly 18:00–07:00, SOLR_STATUS=0, batch 1000); dailyencounter job.
- **Reports:** patientHistory.vm, patientAllHistory.vm, PatientVisitDetailsForFuture.vm, healthCardSheet.vm, consentForm.vm, babiesConsentForm.vm.
- **Security:** Token-based (opaque token in cache); PHI encryptable (IdEncryptionUtil, `phi.encryption.enabled=false`); duplicate guard BERA011; no server-side role check (frontend RBAC only).
- **Permissions:** Configuration master screens (2,078 `data-roleM` keys incl. `Configuration.Master.Unit.*`).
- **Cross-module Dependencies:** Masters, M38 (ABDM/ABHA), M04 (MRN series/billing), M02/M03 (visits), M34 (documents), M28 (death details).
- **Legacy Source Files:** `fjs-config_patientManagement.json`, `fjs-config_op.json` (patientDetailReg), `services/pas/PatientServiceImpl.java`, `services/custom/PatientDetailRegistrationServiceImpl.java`, `services/custom/PatientServiceHelper.java` (BR-PAT evidence), `services/pas/VisitServiceImpl.java`, `modules/masters/patient/*`, `models/trx/*`, `js/admission-module.js`, `js/clinic-module.js`.
- **Migration Complexity:** 4/5 — identity hub; T_REGISTRATION* has no entity mapping (schema truth only in Oracle); Solr load-bearing for search + duplicates; ABDM linkage state.
- **Risks:** T_REGISTRATION schema extraction; Solr retirement breaks dup-detection & ABDM consent matching; MRNO series/sequence infra; temp→permanent MRN promotion.
- **Open Questions:** Temp-MRN auto-code proc semantics; VisitStatus initial value; encounter Solr 3-day retention vs registration search scope.

---

## M02 — OPD / Outpatient Department (Appointments, Walk-ins, Doctor Queue)

- **Target service:** 2. Appointment & Encounter (OPD); queue sub-part → 29. Queue & Kiosk
- **Purpose:** Outpatient visit lifecycle from appointment to consultation queue.
- **Business Context:** 43-key config; walk-in/NHNC/emergency registration paths; doctor schedules; camp registration; telemedicine.
- **Actors:** Appointment desk, Registration clerk, Doctor, Queue/nursing desk, Patient (kiosk), Camp organiser.
- **Submodules:** Appointments (new/reschedule/cancel/future); Walk-in visits; Emergency registration (walkinVisitNhnc); Doctor schedules & check-in; Department appointment slots; Follow-up visits; Referrals; Camps (opCampRegistration); Token/queue (M27); Telemedicine.
- **Features:** doctor-appointment slots; department/doctor schedules; walk-in & NHNC walk-in; follow-up visit requests; doctor cabin check-in/check-out; token generation; MRN re-print (opdCard); op labels; referral types; camp registration; telemedicine department/unit/speciality lists.
- **Screens:** appointment_list-schema, appointment_list_doctor-schema, doctor-appintment-slots-schema, walkinVisit-schema, followupVisitReq_list-schema, showDoctorOpSchedule, manageTokenSchema, DepartmentAppointmentSlots, DoctorReferralSchema, walkinVisitNhnc-schema, abhaPatientRegistration, childAbhaPatientRegistration, searchAbhaPatient, patientRghsPackageDetails, questionnairePatientSchema, emergencyRegType.
- **Routes:** `op:appointmentFlow` (flagship walk-in), `op:addAppointment/:doctor_id/:doctor_name/:department_id/:app_date/:from/:totime`, `op:rescheduleApppointment/...`, `op:addEmergency/:doctor_id/:doctor_name/:department_id`, `op:appointmentList|appointmentListOnline|appointmentListdoc|appointmentListNhnc`, `op:doctorSchedules/:doctor_id/:department_id`, `op:manageDoctorSlot`, `op:manageDepartmentSlot`, `op:showDepartmentSlot`, `op:appointment_confirmed`, `op:walkin_confirmed`, `op:emergency_confirmed`, `op:opFaceSheet`, `op:uploadAppointments`.
- **Backend APIs (service @Path):** `/appointment`, `/updateappointment`, `/appointmentschedule`, `/patientRegAppt`, `/walkinVisit`, `/doctor` (checkinCheckout, queue assignment), `/queueManagment`, `/patient` (kioskQueue, myWalkingVisit), `/markandaddvisit` (checkin), `/doctorlist`, `/helper`.
- **Business Rules (BR-* IDs):** BR-OPD-001 (non-residential tariff), BR-OPD-002 (slot collisions BEXA029/BERA028), BR-OPD-003 (visit number template `V/<PREFIX>/<YY>/<00001>`), BR-OPD-004 (visit-type sequence state machine), BR-OPD-005 (sponsor defaulting BERP008), BR-OPD-006 (walk-in requires doctor BERA022), BR-OPD-007 (payment advances VisitStatus→11).
- **Status Lifecycle:** T_Encounter.VisitStatus 3=doctor called, 9=doctor checkout, 10=cabin released; T_Appointment 4=assessment q saved, 6=other q saved, 11=bill paid, 12=pharmacy dispensed; visitstatus2 "2"=default walk-in, "9"=compound-med. **Split-brain risk: billing writes T_Appointment, clinical writes T_Encounter (06 §C.1).**
- **Database Tables:** T_Encounter, T_Appointment, T_Visit, FollowUpVisitInfo, RegistrationQueue, PatientQueue, T_PATIENTTOKEN, T_PATIENTSPONSOR, masters/hospital (schedulemaster, shiftmaster, opdStationMaster, roomMasterForQueue, cabinmaster, departmentCabinRoomMapping), M_VISITTYPEMASTER, M_APPOINTMENTREASONMASTER.
- **Stored Procedures (CIMS_*):** CIMS_ADDPATIENTSPONSOR_NEW_DOC, CIMS_ADDPATIENTREGISTRATION* (walk-in path).
- **Integrations:** Kiosk (M31), ABDM (ABHA reg in OP), NHNC (walkinVisitNhnc), telemedicine (createTeleconsultRoom), e-Mitra (online appointment), Jeevanvahini (emergency).
- **Solr Dependencies:** encounter collection realtime (PatientRegApptServiceImpl:986,1299; EmergencyAndWalkinVisitServiceImpl:1795-1798); searchPatientInEncouter (unit-scoped); searchPatientInEncounterForCheckinCheckout; getAppListSearchFilters/TypeFilter; closeDailyEncountersJob deletes docs > `solr.encounter.retain.days=3`.
- **Redis Dependencies:** `current_enc_<encId_E/A>` (100s), QMS room token queues (`pushToQ/removeFromQ`, `<qName>_S` counters), session token.
- **Scheduled Jobs:** closeDailyEncountersJob (ApplicationJobs:542-570).
- **Reports:** appointment_confirm.vm, appointmentLetter.vm, appointmentOpLabel.vm, appointment_token.vm, opdCard (template), patientVisitDetailsForFuture.vm.
- **Security:** Token auth; walk-in screen ships `secretTokenForOnlineAppointment` bypass (user id=1) for kiosk/e-Mitra appointments (08 §9 risk 3).
- **Permissions:** Appointment keys (27) + QueueManagement (16); `Login.Clinician`.
- **Cross-module Dependencies:** M01 (patient), M08 (orders), M27 (queue), M04 (billing), M09 (nurse desk), Masters.
- **Legacy Source Files:** `fjs-config_op.json` (43 keys), `services/pas/AppointmentServiceImpl.java`, `services/custom/AddUpdateAppointmentServiceImpl.java`, `services/custom/AppointmentScheduleServiceImpl.java`, `services/custom/PatientRegApptServiceImpl.java`, `services/custom/EmergencyAndWalkinVisitServiceImpl.java` (BR-OPD evidence), `services/pas/DoctorServiceImpl.java`, `services/custom/QueueManagmentServiceImpl.java`, `modules/op/schemas/*`, `js/appointment-module.js`, `js/appointment-list-module.js`, `js/clinic-module.js`.
- **Migration Complexity:** 4/5 — VisitStatus split-brain (T_Appointment vs T_Encounter), Solr encounter retention, slot/collision rules, NHNC/ABDM variants.
- **Risks:** VisitStatus 11/12 dual-write semantics; 3-day encounter Solr window limits search; questionnaire VisitStatus 4/6.
- **Open Questions:** NHNC program context; telemedicine flag effects; kiosk lab-result scope (UNKNOWN-10).

---

## M03 — IPD / Admission, Bed & Discharge

- **Target service:** 3. ADT & Bed (+ death flows)
- **Purpose:** Inpatient stay lifecycle: admission → bed → stay services → discharge → bed release.
- **Business Context:** 65-key config; planned/emergency/interim admission; bed hierarchy (building→floor→wing→block→ward→room→bed); clearance workflows (billing/BMW/MLC); baby admission.
- **Actors:** Admission clerk, Ward nurse, Bed manager, Nursing station, Attendant desk, Doctor (discharge advice), Billing (clearance), Mortuary (death cases).
- **Submodules:** Admission (planned/emergency/interim); Bed management (allocation, transfer, release, reservation, cradle); Discharge (advice, clearance, summary); Transfer requests; Attendant passes; MLC clearance; Baby admission & mother–child linking; Diet plan linkage.
- **Features:** admission request & admission list; bed reservation/transfer/release; cradle management; interim admission; discharge clearance list & update status; bed census (getBedCensusAndNonCensusList); attendant pass issue/return/reprint; MLC clearance filters; emergency admission; bill estimation at admission.
- **Screens:** addpatientAdmissiontdto_schema(+_emergency), admission_list-schema, admissionRequest_list-schema, bed_management_schema, bed_screen_schema, bedTransferSchema, dischargePatientSchema, discharge_clearance_schema, transferRequest_list-schema, attendantPassSchema, bedReservationdto_schema, addBabyAdmissiondto_schema, interimAdmissions, bedReleaseSchema, bedTransferRequestSchema, billEstimatiomSchema, clearancemaster, dischargeUpdateStatus, getAdmissionClearanceList, mlcClearanceFilters, motherChildLinkingSchema, releaseBedNewSchema, showAllCradlesSchema, showDischargedPatientSchema, showDoctorIpSchedule, tidListSchema, updateDoctorFromBM_Schema, ipd_patient_service_detail_schema, ipPatientCoronaDetailsSchema, MedicOrderPickListSchema, nursingOrderSchema, admissionServiceSchema, allPreviuosAdmissionsSchema, addVisitChargeSchema, emergency-schema.
- **Routes:** `ip:admissionOrder`, `ip:admissionOrderEmer(V/:v)`, `ip:admissionList`, `ip:admissionReqList`, `ip:transferReqList`, `ip:admissionConfirm`, `ip:attendantPass`, `ip:bedManagement/:class_id/:ward_id/:bed_id/:building_id/:filterId`, `ip:bedManagementNew/...`, `ip:bedScreen/...`, `ip:otBedManagement`, `ip:bedReservation`, `ip:unReservedBedList`, `ip:motherChildLinking`, `ip:clearanceListFlow`, `ip:MlcclearanceListFlow`, `ip:nursingStation`, `ip:dischargedPatients`, `ip:showAllCradles`, `ip:summaryOpdIpd`, `ip:updateDischargeStatus`, `ip:showInterimAdmissions`, `ip:showTidList`, `ip:doctorCheckin`, `ip:cdrForm4A/4B`.
- **Backend APIs (service @Path):** `/ipdadmission`, `/ipd`, `/ipdconfig`, `/bed`, `/bedreservation`, `/bedmanagement`, `/canceladmission`, `/patientTransferDischarge`, `/patientRegAddmission`, `/patientrequisition` (blood for IPD), `/patient` (mortuary/death endpoints — M28).
- **Business Rules (BR-* IDs):** BR-IPD-001 (preconditions BERA022/BERB009/BERB013/BERB012), BR-IPD-002 (bed auto-allocation + CAS claim BEXB001), BR-IPD-003 (release gate BERB005), BR-IPD-004 (IPD number template `A/<PREFIX>/<YY>/<00001>`), BR-IPD-005 (interim auto-abscond 3 days), BR-DIS-001 (discharge preconditions BERB023/BERB015/BERB011/24/25), BR-DIS-002 (discharge execution & death via CIMS_ADDBEDDISCHARGE/CIMS_UPDATEIPDISDISCHARGE), BR-DIS-003 (bed release, BedStatusId=6 hard-coded).
- **Status Lifecycle:** M_BEDMASTER.OCCUPIED (atomic CAS) + BedStatusId (0=reset, 1/3=free predicates, 5=occupied, 6=released); T_Admission ISDischarged/IsClosed; ISCANCEL=1+DISCHARGETYPEID+ABSCONDDATETIME (auto-abscond); discharge status (dischargeUpdateStatus); clearance/transfer status.
- **Database Tables:** AttendantPassDetails, TransferRequest, T_Admission, T_AdmissionDetails, T_ADVISE_DISCHARGE, T_DISCHARGE, T_BEDRESERVATION, T_PATIENTDISCHARGEDATA, T_MLCDETAILS, T_ATTENDANTPASS, masters/admission (bedmaster, wardmaster, roommaster, classmaster, blockmaster, buildingfloorstructure, wingmaster, dischargetype, admissiontype, daycaremaster, modeOfDelivery, actualBedCountMaster, bedstatusmaster, wardtypemaster, floormaster, doctortypemaster, identitytype, buildingmaster, classWardMappingForAdmissionBlock), T_REGISTRATION (native).
- **Stored Procedures (CIMS_*):** CIMS_ADDADMISSIONDETAILS (writes M_BEDMASTER.OCCUPIED), CIMS_ADDADMISSIONINFORMATION, CIMS_UPDATEBEDOCCUPIEDFLAG_1, CIMS_ADDBEDDISCHARGE, CIMS_UPDATEIPDISDISCHARGE. Note: `resultStatus==-1 → APP015` check is dead (proc ends 0) — 06 §C risk R4.
- **Integrations:** HIS admission request (addAdmissionRequestToHIS), IPCR (patientIPCRSchema), RGHS (patientRghsPackageDetails), Corona tracking, Chiranjeevi (INSURANCESCHEMEID=2).
- **Solr Dependencies:** encounter/admission index (`<admId>_A`); discharge clears encounter docs (PatientTransferDischargeServiceImpl:698-3282,4400-4413); death → partial patient update (`death=true`).
- **Redis Dependencies:** `CDD_<opdipdid>` discharge lock (60s, ServiceLayerAspect:1147-1156), `current_enc_<admId>_A`.
- **Scheduled Jobs:** updateAbscondInterimAdmissionStatusJob (ApplicationJobs:991-1019).
- **Reports:** admissionFacesheet1.vm, adm6.vm, token_sheet.vm, discharge_summary_pat.vm, wardBedInquiry.vm, bht_barcode.vm, dischargeSummaryLabResult.vm, common_dis_form/ticket.vm + specialty variants (cardiology, ent, eye, medicine, neurology, neurosurgery, oncology, ortho, rrc, urology, surgery_plasticsur, endocrinology).
- **Security:** Token auth; bed CAS updates; discharge photo+doc+feedback must come as a set (BR-DIS-001).
- **Permissions:** Admission module keys (45 in data-roleM).
- **Cross-module Dependencies:** M01, M02 (emergency), M04 (billing/clearance), M10 (diet), M09 (nursing), M08 (orders), M28 (mortuary/death), M11 (CSSD), M15 (blood requisition), M18 (BMW clearance).
- **Legacy Source Files:** `fjs-config_ip.json` (65 keys), `services/pas/IPDAdmissionServiceImpl.java`, `services/pas/IPDServiceImpl.java`, `services/pas/IPDConfigServiceImpl.java`, `services/pas/BedServiceImpl.java`, `services/pas/BedReservationServiceImpl.java`, `services/custom/BedManagementServiceImpl.java`, `services/custom/CancelAdmissionServiceImpl.java`, `services/custom/PatientRegAdmissionServiceImpl.java` (BR-IPD), `services/custom/PatientTransferDischargeServiceImpl.java` (BR-DIS), `modules/ip/schemas/*`, `modules/masters/admission/*`, `js/bed-management-module.js`, `js/admission-module.js`.
- **Migration Complexity:** 4/5 — bed CAS + 5 CIMS_* procs, discharge/death rules, interim-abscond job, dead APP015 path.
- **Risks:** BedStatusId=6 hard-coded vs master-driven `isrelease`; concurrent admission race unguarded at DB level (R4/R5).
- **Open Questions:** Bed color-code semantics (bed-screen templates); cradle re-mapping; discharge doc-set enforcement location.

---

## M04 — Billing, Payments, Receipts & Refunds

- **Target service:** 5. Billing & Settlement + 6. Payments & Refunds
- **Purpose:** Charge capture → estimation → bill generation → payment collection → receipts → refunds; doctor share settlement.
- **Business Context:** 77-key config; OP/IP billing; scheme/company billing; concessions; tariff/copay; the revenue core.
- **Actors:** Cash counter operator, Billing officer, Accountant (credit notes), Doctor share admin, Concession authority.
- **Submodules:** OP billing; IP billing; Advance (collection/refund/adjustment); Receipts (bill/advance/refund/combined); Bill cancellation (settled/unsettled); Estimation; Credit notes; Doctor share & deductions; Concessions; Company/insurance billing (approved amounts); Package subscriptions; Pharmacy bill receipt; Tariff application & copay.
- **Features:** addOpIpBilling; partial payment; advance against bills; bill estimation; IP bill with pharmacy separate (ipBillSummeryWithoutPharmcy.vm); credit notes; doctor share add/deduct & bill; sponsor change; approval amounts; package subscription; tariff copay.
- **Screens:** addBillingSchema, addOpIpBillingSchema, addIpBillingSchema, billReceiptSchema, billSummarySchema, allReceiptsSchema, advanceSchema, advance_recieptSchema, refund_recieptSchema, creditNoteSchema, doctorShareSchema, partialPayBillSchema, opBillPaymentSchema, ipBillPaymentSchema, addAdvanceSchema, addApprovalAmountSchema, addOTBillEstimationSchema, addPackageSubscriptionSchema, advanceReceiptByReceiptNoSchema, cancelIpBillSummarySchema, cancelOpBillSummarySchema, changeSponsorSchema(+List), combineBillReceiptSchema, companycontractdetails, doctorBillListSchema, doctorShareAddDeductSchema, doctorvisitcharge, draftAdvanceSummarySchema, draftSummaryBillSchema, getAllBills-schema, getAllCombinBills-schema, getAllCreditNoteListSchema, getAllEstimationBillSchema, getDischargeList-schema, getIpdChargeListSchema, healthplanConsumptionDetailSchema, healthPlanPackageSchema, iPDBillReceiptSchema, itemPackageDetails, package-order-list-schema, packageListForBillSchema, package_appointment_list-schema, paymentDetailSchema, pharmacyBillRecSchema, refundDetailSchema, return_advance, return_bill_advance-schema, service-order-list-schema(+ForHealthPlanCons), serviceListForBillEstimationSchema, serviceListForBillSchema, servicePackageDetails, showPackageDefaultServices, showSponsorSchema, tariffConfigPackageDetails, tariffcopaydetails, tariffRatePackageDetails, bedPackageDetails, diseaseGroupMaster, crossServiceOrderSchema, approveAmountListSchema, itemSaleDetailpaymentSchema.
- **Routes:** `billing:billingFlow`, `ipBillingFlow`, `opIpBillingFlow`, `patientBilling`, `viewOpdCharges(onInvestigation)`, `billingListFlow`, `checkout`, `finalizeBill`, `interimBill`, `checkoutForHealthConsumption`, `OPDbillReceipt`, `IPDbillReceipt`, `advanceReceipt`, `receiptList`, `billSummary`, `dischargeList`, `addPayment`, `creditNoteBillingListFlow`, `doctorShare(AddDeduct|Bill)`, `doctorBillList`, `packageSubscriptionFlow`, `packageAppointmentList`, `healthPlanPackage(Nhnc)`, `billEstimationFlow`, `addPatientBillEstimation`, `otBillFlow`, `crossOrderService`, `showSponsor(Details)`.
- **Backend APIs (service @Path):** `/bill`, `/billReceipt`, `/billSummary`, `/billDet`, `/cancelBill`, `/addPayment`, `/addAdvance`, `/addRefund`, `/advancebillReceipt`, `/allReceipts`, `/refundBillReceipt`, `/patientbillestimation`, `/charge`, `/payment`, `/refund`, `/companyapprovedamount`, `/concessiontemplate`, `/serviceOrder` (charges/credit-notes/packages).
- **Business Rules (BR-* IDs):** BR-BILL-001 (bill creation & numbering, CIMS_ADDBILL), BR-BILL-002 (doctor share %, CEILING/HALF_UP, skipped for OT tariff), BR-BILL-003 (patho/rad order auto-creation via CIMS_BILLFORINVESTIGATIONNEW), BR-BILL-004 (tax ≤3 buckets, applyafterconcession), BR-BILL-005 (co-pay/concession split — asymmetric self vs non-self, RISK R2), BR-BILL-006 (settlement ⇔ balances ≤0; OPD checkout VisitStatus=11; changeAmount), BR-BILL-007 (advance FIFO, payment-mode 7, CIMS_ADDPAYMENTADVANCEDETAILS), BR-BILL-008 (refund/cancellation restore, transactionType 3/4, BEXAR002).
- **Status Lifecycle:** T_Bill Status=1 active; IsSettled false→true; IsCancelled=0 default; InterORFinal; IsPharmacy=0; T_Charges IsBilled/IsCancelled/IsTemp (T_CHARGES_TEMP split) + *BeforeFinalization snapshots; T_Payment.TransactionType 2=bill payment, 3=advance refund, 4=bill refund.
- **Database Tables:** CreditNote(+Details), PackageChargeSubscription, PatientBillEstimationSurgeryHead, Doctorvisitcharge, Doctorwaiver, T_BILL, T_BILLDETAILS, T_CHARGES, T_CHARGES_TEMP, T_PAYMENT, T_PAYMENTMODEDETAILS, T_ADVANCE, T_REFUND, T_RECEIPT, T_ITEMSALE, T_TARIFFSERVICEMASTER, T_SURGCHARGES, T_SPONSOR, T_PATIENTSPONSOR, masters/billing (companymaster, companyplandetails, tariffmaster, packagemaster, planmaster, concessiontemplatemaster, paymentmodetype, cashcountermaster, cancellationreason, advanceagainst, companyassociatemaster, companytypemaster, concessioncategorymaster, billingDepartmentMaster), masters/hospital (accountheadmaster, bankmaster), s_servicewisecollection / s_userwisecollection.
- **Stored Procedures (CIMS_*):** CIMS_ADDBILL, CIMS_ADDCHARGE, CIMS_ADDCONCESSION, CIMS_UPDATEBILLFORCONCESSION, CIMS_ADDPAYMENT*, CIMS_ADDADVANCE, CIMS_ADDDOCTORSHARE, CIMS_BILLFORINVESTIGATIONNEW, CIMS_ADDITEMSALESDETAILS_NEW, CIMS_ADDPAYMENTADVANCEDETAILS, CIMS_SETTLEDBILLCANCELLATION, CIMS_UNSETTLEDBILLCANCELLATION, CIMS_ADDPATIENTSPONSOR_NEW_DOC (20 CIMS_ calls in BillingCommonUtilServiceImpl — 05 §5).
- **Integrations:** Payment transfer (M33), RCM (M32), NHCX (insurance claims via nhcx module).
- **Solr Dependencies:** patient search for bill lookup; InvoiceGenerated index events (target design).
- **Redis Dependencies:** tariff/rate master cache (tariffMaster, classMasterMap_* — owned by M05/M39, consumed here); session tokens.
- **Scheduled Jobs:** CIMS_JOBSCHEDULING (bed auto-charges, ApplicationJobs:283).
- **Reports:** bill_receipt.vm, bill_summary.vm, ip_bill_summary.vm, ipbillreceipt.vm, opbillRec.vm, ipDraftBill.vm, advance_rpt.vm, advanceRefundRec.vm, cancelIPReceipt.vm, cancelOPReceipt.vm, creditNoteDetails.vm, creditNoteServices(+ForOP).vm, packageRefundReceipt.vm, billEstimation.vm, high_billing_report.vm, billRefundReport.vm, ipReceiptList.vm, packageConsumptionDetailsPrint.vm, packageDetailsPrint.vm, ipBillSummeryWithoutPharmcy.vm, showPharmacyDraftBill.vm.
- **Security:** Cash counter identity; concession authority; settled/unsettled cancellation paths; **no backend role enforcement** (frontend `Billing.OPIPBilling` keys); `Billing.DeathPatientBilling`.
- **Permissions:** Billing keys (34): `Billing.OPIPBilling`, `Billing.DeathPatientBilling`.
- **Cross-module Dependencies:** M01–M03, M05 (tariff/packages), M07 (pharmacy bill), M06 (doctor share), M32 (RCM), M33 (payment transfer), M08 (charges via serviceOrder), M17 (blood bank billing).
- **Legacy Source Files:** `fjs-config_billing.json` (77 keys), `services/pas/BillServiceImpl.java`, `services/custom/BillReceiptServiceImpl.java`, `services/custom/BillSummaryServiceImpl.java`, `services/custom/BillDetServiceImpl.java`, `services/custom/BillCancellationServiceImpl.java`, `services/custom/AddPaymentServiceImpl.java` + `AddPaymentServiceHelper.java`, `services/custom/AddAdvanceServiceImpl.java`, `services/custom/AddRefundServiceImpl.java`, `services/custom/AdvanceBillReceiptServiceImpl.java`, `services/custom/AllReceiptsServiceImpl.java`, `services/custom/RefundBillReceiptServiceImpl.java`, `services/pas/PatientBillEstimationServiceImpl.java`, `services/pas/ChargeServiceImpl.java`, `services/pas/PaymentServiceImpl.java`, `services/pas/RefundServiceImpl.java`, `services/pas/ConcessionTemplateServiceImpl.java`, `services/pas/CompanyApprovedAmountServiceImpl.java`, `services/BaseServiceImpl.java` (GST), `util/BillInvoiceGenerator.java`, `js/billing-module.js` (calculateTotalBillAmount :2050), `modules/billing/schemas/*`.
- **Migration Complexity:** 5/5 — owns the Order→Bill→Lab→Rad→DoctorShare→EMR→HL7 mega-transaction (10 §1); procs; asymmetric concession rule; VisitStatus 11/12 split-brain.
- **Risks:** Mega-txn must become saga; concession asymmetry (R2); T_CHARGES_TEMP split; bill-number sequences; client-side billing math must be golden-tested.
- **Open Questions:** InterORFinal interim semantics; cash changeAmount handling; NHCX claim depth.

---

## M05 — Tariff, Packages, Health Plans, Companies & Services (Billing Masters)

- **Target service:** 21. Reference Master Data (cache owner)
- **Purpose:** Pricing & eligibility backbone for billing.
- **Business Context:** Service catalogue, tariffs, packages, plans, companies, concessions, order sets; Redis cache god-node consumed by ~30 services.
- **Actors:** Billing admin, Tariff officer, Company contract manager.
- **Submodules:** Service master; tariff & copay; packages (service/group/medicine/item-category); health plans (planmaster, subplan, tariff package health plan); companies & plans; concessions; order sets (ordersetdefination + service variants); specialization.
- **Features:** tariff rate copy utility; package health plan definitions; order-set definitions (consult/lab/radiology/nursing/procedure/consumables/misc); services specialization search; company contract details; plan-uncovered services; concession templates.
- **Screens:** servicemaster, tariffmaster, tariffratecopyutility, packagemaster, packages, planmaster, subPlanmaster, companymaster, ordersetdefinationmaster, services, specialization, concessiontemplatemaster, companycontractdetails, tariffcopaydetails, tariffRatePackageDetails, tariffConfigPackageDetails, ordersetservicemaster + lab/radiology/nursing/consult/procedure/consumables/miscellaneous variants, ordersetmedicinedetails, procedureordersetservicemaster, showPackageDefaultServices.
- **Routes:** `masters:<schema>/view|add|edit/:id` generic + specific (companymasterwrapper, servicemasterparent, servicemaster/rateedit, concessiontemplatewrapper, planmasterwrapper, packagehealthplandefinationmaster, RateContract, ordersetdefinationmaster).
- **Backend APIs (service @Path):** `/tariff`, `/servicemaster`, `/planmaster`, `/packageservice`, `/concessiontemplate`, `/companyapprovedamount`, `/addServiceMaster`, `/master` (MasterCachedServiceImpl — 264 endpoints), `/masterConfigService`.
- **Business Rules (BR-* IDs):** none formalized in 06; evidence: packages service/group/medicine/item-category based; tariff package rates class-wise (Tariffpackageclasswiserates); copay per tariff; plans exclude items/services (Planuncovereditemdetails, Planuncoveredservicedetails); GST rule `isGSTApplicable=true` + transDate ≥ 2017-07-01 (BaseServiceImpl:521-533).
- **Status Lifecycle:** service/tariff active flags; UNKNOWN-12.
- **Database Tables:** Servicemaster(+new), Tariffmaster, Tariffservicemaster, Tariffcopaydetails, Tariffpackage*, Packagemaster, Packagehealthplandefinationmaster, Planmaster, SubPlanmaster, Companymaster, Companyplandetails, Companytariffdetails, Concessiontemplatemaster(+details), Orderset*, Specialization, Subspecialization, M_SERVICEMASTER (15 refs), M_ITEMMASTER (pricing), T_TARIFFSERVICEMASTER.
- **Stored Procedures (CIMS_*):** CIMS_ADDSERVICEMASTER2, CIMS_ADDTARIFFSERVICE2 (10 §2.1 row 21).
- **Integrations:** RGHS packages (getRghsPackageList), NHCX plans (insurance_plan_on_request), MDM (Janta master data).
- **Solr Dependencies:** service collection **orphaned** (no indexer/reader — 07 §1.2); item/icd10cm/healthfactor collections for lookups.
- **Redis Dependencies:** **Owner of ~375 `@CrudCached` master keys** (genderMaster, prefixMaster, maritalStatusMaster, companyMaster, planMaster, itemcategory, bedMasterMap, auditEventMaster…); invalidation via CrudOperationsFilter.
- **Scheduled Jobs:** none specific; TTL resets via NoAuthCacheTTLReset.
- **Reports:** packageDetailsPrint.vm, packageConsumptionDetailsPrint.vm.
- **Security:** Admin-only UI (showmasternav); CRUD through generic filter with audit; no backend role check.
- **Permissions:** `Configuration.Master.*` family (1,424 keys — largest group).
- **Cross-module Dependencies:** M04 (billing), M01 (patient categories), M07 (pharmacy pricing), M32 (RGHS), M39 (commons masters platform).
- **Legacy Source Files:** `services/pas/TariffServiceImpl.java`, `services/pas/ServiceMasterServiceImpl.java`, `services/pas/PlanMasterServiceImpl.java`, `services/pas/PackageServiceServiceImpl.java`, `services/pas/ConcessionTemplateServiceImpl.java`, `services/pas/CompanyApprovedAmountServiceImpl.java`, `services/custom/AddServiceMasterImpl.java`, `services/custom/MasterCachedServiceImpl.java` (12,710 LOC), `services/custom/MasterCachedServiceHelper.java`, `modules/masters/billing/*`, `js/item-category-module.js`, `mastersConfigration.json`.
- **Migration Complexity:** 3/5 — huge but mechanical CRUD; god-node cache decomposition; 659 `@CrudApi` entities through generic filter.
- **Risks:** Master-cache ownership split; reflective per-node invalidation; orphaned Solr service collection.
- **Open Questions:** Cache contract for PG+Redis target; order-set config vs rule-engine migration.

---
## M06 — Doctor Share & Incentives

- **Target service:** 5. Billing & Settlement (doctor bills/shares)
- **Purpose:** Financial settlement of doctor professional fees.
- **Business Context:** Percentage share splits per charge; addition/deduction adjustments; visit charges & waivers.
- **Actors:** Billing admin, Accounts, Doctors.
- **Submodules:** Doctor share calculation, addition/deduction, doctor bills & payments, visit charges, waiver.
- **Features:** doctor share list/bill/payment; doctor add-deduct; doctor waiver; doctor visit charges.
- **Screens:** doctorShareSchema, doctorShareAddDeductSchema, doctorShareBillSchema, doctorBillListSchema, doctorvisitcharge.
- **Routes:** `billing:doctorShare(AddDeduct|Bill)`, `billing:doctorBillList`.
- **Backend APIs (service @Path):** `/doctor` (addDoctorAdditionDeduction, getDoctorShareList, getDoctorBillList, addDoctorBill, addDoctorBillPayment, updateDoctorShare).
- **Business Rules (BR-* IDs):** BR-BILL-002 (share split %, CEILING in DB / HALF_UP on DTO; skipped for OT tariff services).
- **Status Lifecycle:** share IsBilled=false→true; UNKNOWN-13 print template.
- **Database Tables:** Doctorvisitcharge, Doctorwaiver, T_DOCTORVISITCHARGES, T_DOCTORWAIVER, T_DOCTORSHARE, T_DOCTORBILL, masters/hospital/admittingdoctorclassificationmaster.
- **Stored Procedures (CIMS_*):** CIMS_ADDDOCTORSHARE.
- **Integrations:** none evidenced.
- **Solr/Redis:** none beyond master caches.
- **Scheduled Jobs:** none evidenced.
- **Reports:** doctor share print UNKNOWN-13.
- **Security:** Billing admin gating.
- **Permissions:** doctorShare screens under Billing keys.
- **Cross-module Dependencies:** M04 (billing), M01 (doctors via masters).
- **Legacy Source Files:** `services/pas/DoctorServiceImpl.java`, `modules/billing/schemas/doctorShare*`, `masters/hospital/admittingdoctorclassificationmaster`, `models/transactionals/Doctorvisitcharge`, `Doctorwaiver`.
- **Migration Complexity:** 2/5 — small surface; share math + proc port.
- **Risks:** Rounding divergence (CEILING vs HALF_UP) between DB and DTO.
- **Open Questions:** none beyond UNKNOWN-13.

---

## M07 — Pharmacy & Dispensing

- **Target service:** 9. Pharmacy & Dispensing
- **Purpose:** Medication ordering → verification → dispensing → administration → return; OTC/counter sales; pharmacy billing.
- **Business Context:** 39-key config; OP/IP/ward dispensing; IV/compounding; BCMA link; pharmacy separate billing for IP.
- **Actors:** Pharmacist, Dispensing clerk, Nurse (medication orders), Doctor (prescribing), Store in-charge, Ward pharmacist.
- **Submodules:** Prescriptions (doctor/nurse/uploaded); Dispensing (OP/IP/ward/counter); Medication administration support (BCMA link); Item sales & returns; IV/compounding; Cart-fill & ward pick lists; Pharmacy billing; Interventions; Home medications (ipHomeMedsForm); External patients (externalPatient); Store/stock queries (getItemAndItemBatchListByStoreId).
- **Features:** addPrescriptionSchema; addMedicationSchema(+FromNurse); dispensing types; pharmacy counter sale; item sale return; IV pharmacy; compounding (preparation with batch rows); ward pick lists (wardPickListOrders); pharmacy intervention reports; prescription scan & upload; label printing (printMedLabel, compoundingLable); ipPharmacyBilling.
- **Screens:** addPrescriptionSchema, addMedicationSchema, dispensingtype, pharmacyCounterSaleSchema, itemSaleListSchema, itemSaleReturnListSchema, opIpPharmacyBillSchema, ipPharmacyBillingSchema, prescriptionListSchema, prescriptionUploadSchema, addItemGroup, addMedicationFromNurseSchema, addPatientPrescMedSchema, addPatientProblems, addPrescVisit, commonVisitCreationAndMedicineDis, getEntirePrescriptionMaster, getItemAndItemBatchListByStoreId, getItemGroup, getPrescriptionDetailsSchema, itemCategoryByStoreIdSchema, itemCategorySchema, itemSaleDetail/List(+ReturnDetails/List)Schema, opPharmacyAddPatient, patientOrderListSchema, pharmacyInterventionReport, pharmacyItemSaleRetrunBIllReciptSchema, pharmacyPaymentDetailSchema, pharmacyPaymentForCounterSchema, pharmacyPaymentReturnSchema, showNursingPrescripListSchema, showPharmacyPatientCoversheetSchema, updateItemGroupSchema, updateItemMaster.
- **Routes:** `pharmacy:pharmacyBilling`, `opIpPharmacyBilling`, `ipPharmacyBilling`, `pharmacyCountersale`, `pharmacyItemList`, `PresciptionList`, `prescriptionList`, `NursePrescriptionList/:tab`, `pharmacyOrderVerify`, `pharmacyOrderDispense(D)`, `cartFillFlow/:ward_id/:patient_dfn`, `pharmacyOp`, `opPharmacyFlow`, `addPrescription`, `medicationOrder`, `itemSaleList`, `itemSaleReturnList`, `ipReturnFlow`/`iPReturnFlow`/`ipReturnPharmFlow`, `ivPharm`, `prepareCompound`, `pharmacyDrugMapping`, `prescriptionUpload`, `nurseOrdrDispensed`, `phCartFillOrderDispense`, `pharmInterventionreport`, `opPatientDetails/:patientId/:opdIpdId/:opdIpd/:pctflow/:patQId/:isPrescriptionOnly`.
- **Backend APIs (service @Path):** `/pharmacy`, `/prescriptionmaster`, `/drug`, `/medication`, `/prescription`, `/itemsales`, `/master` (filterPharmDiagnosis).
- **Business Rules (BR-* IDs):** BR-PHAR-001 (dispensing completes visit — VisitStatus=12), BR-PHAR-002 (prescription closure on sale via CIMS_ADDITEMSALESDETAILS_NEW).
- **Status Lifecycle:** prescription status; dispensing status (dispensingtype); order verification (pharmacyOrderVerification); return status (itemSaleReturn); T_Appointment.VisitStatus=12 when all item sales dispensed; UNKNOWN-14.
- **Database Tables:** PrescriptionMaster, PrescriptionDispenseDetails, PatientMedication, PatientPrescptionNote, PatientNonMedOrderDetails, T_ITEMSALE(+DETAILS), T_ITEMCOMPOUNDDETAILS, T_PRESCRIPTIONDISPENSEDETAILS, IHR_MEDICATION_TMP (BCMA), T_INFUSIONMIXING(+DETAILS), masters/Pharmacy (AdministrationSchedule, CreditAccountMaster, Drugingredients, Druginteractions, Medicationroute), masters/meds (instructionmaster, route, scheduleListMaster), M_ITEMMASTER, T_ITEMSTOCK, T_ITEMCLINIC, T_ITEMBATCHMASTER.
- **Stored Procedures (CIMS_*):** CIMS_ADDITEMSALESDETAILS_NEW, CIMS_ADDITEMSTOCK, CIMS_GETPRESCRIPTIONWARDDETAILS*.
- **Integrations:** EAushadi (item stock sync), BCMA (M30), ABDM PHR (patPrescriptionPHR.vm), WhatsApp (lab/pharmacy reports).
- **Solr Dependencies:** item collection (searchAllItemsInSolr — pharmacy/linen/gas/asset flags, 10,000 rows); patient search for OP dispense.
- **Redis Dependencies:** item stock snapshots (lookup only per 10 §2.1 row 9); `current_pat_` mirrors.
- **Scheduled Jobs:** eAushadiItemStockUpdate.schedule (04:00 daily).
- **Reports:** prescription.vm, prescription_format.vm, prescription_hindi.vm, newPrescription.vm, pharmacyReceipt.vm, pharmacyMedLabel.vm, pharmacyItemSaleBillNew(+ForCounterSale/WholeDetails).vm, pharmacySaleReturnReport.vm, pharmacyCompoundItem(+AllLabels).vm, ipOutOfStockMedications.vm, printMedNonMedDetails.vm, printRxDetails.vm, cartfillDetail.vm, dispenseItemDetail.vm, printMedLabel (template).
- **Security:** Pharmacist/verifier roles; dispensing permissions under Pharmacy keys (18).
- **Permissions:** `Pharmacy.*` keys; `Investigation.*` for lab-linked dispensing.
- **Cross-module Dependencies:** M13 (inventory stock), M04 (billing), M09 (nursing orders), M08 (services), M30 (BCMA), M01.
- **Legacy Source Files:** `fjs-config_pharmacy.json` (39 keys), `services/custom/PharmacyServiceImpl.java` (dual-write stock hotspot :2140), `services/pas/PrescriptionMasterServiceImpl.java`, `services/pas/DrugServiceImpl.java`, `services/custom/MedicationServiceImpl.java` (BR-PHAR), `services/custom/PrescriptionDetailsServiceImpl.java`, `services/pas/ItemSalesServiceImpl.java`, `modules/pharmacy/schemas|templates`, `modules/masters/Pharmacy|meds`, `js/pharmacy-module.js` (+pharmacy-module-new/op, opPharmacy-new).
- **Migration Complexity:** 4/5 — stock dual-write with inventory, dispensing state machine, VisitStatus 12 coupling, compounding/IV.
- **Risks:** Pharmacy↔Inventory stock boundary unresolved (10 §7 Q1); dual-write T_ITEMSTOCK; BCMA scan flows; prescription closure procs.
- **Open Questions:** Stock ownership split; IV admixture scope; intervention report semantics.

---

## M08 — Clinical / Service Ordering & Investigations

- **Target service:** 4. Ordering & Prescription (Clinical)
- **Purpose:** Clinician orders for services (lab, radiology, procedure, consult, nursing) across OP/IP.
- **Business Context:** Order→Bill→Lab→Rad→DoctorShare→EMR→HL7 mega-transaction (AddServiceOrderServiceHelper.java:341-516); order sets; CWF/SCWF clinical workflow.
- **Actors:** Doctor, Nurse, Lab/Radiology department staff.
- **Submodules:** Service orders (addServiceOrderDetails on /patient), Consult orders, Investigations (lab/rad), Order sets, CWF/SCWF mapping, Cross-service visits.
- **Features:** addServiceOrderDetails; consult order lists (consultOrder_list-schema); investigation ordering; order set application; service status (serviceStatusSchema); machine config (getResultsFromKDL).
- **Screens:** consultOrder_list-schema, serviceOrderDetail (template), orderingSet/orderSet (templates), serviceMasterSchema, servicemastermapping, crossServiceOrderSchema.
- **Routes:** `nursing:orderingSet`, `billing:crossOrderService`, `nursing:compliantDiagnosisMap`, `masters:cwf (mapping/add)`, `masters:scwf`.
- **Backend APIs (service @Path):** `/clinical`, `/serviceOrder` (addServicechargesNew, includeCharge, excludeCharge, removeCharge), `/investigation`, `/cwf`, `/patient` (addServiceOrderDetails, getPatientEorders).
- **Business Rules (BR-* IDs):** BR-PATH-001 (sample collection locks charge removal — LEX001; shared M15/M16), BR-BILL-003 (patho/rad order auto-creation at billing — cross-module M04).
- **Status Lifecycle:** T_Charges IsBilled/IsTemp; service status (serviceStatusSchema); order verification; UNKNOWN-40 adjacents.
- **Database Tables:** PatientEorder, ConsultOrder, ExternalOrderDetails, PendingConsultVisits, T_PATIENTADVICE, T_EMRDETAILS, T_EMRSERVICES, T_CONSULTORDER, masters (Orderset*, CwfMapping, ScwfMapping).
- **Stored Procedures (CIMS_*):** CIMS_ADDORDERDETAILS, CIMS_BILLFORINVESTIGATIONNEW, CIMS_ADDPATHORDERBOOKING / CIMS_ADDRADORDERBOOKING (auto-created orders).
- **Integrations:** EMR/HL7 outbound (T_OUTBOUNDHIS same tx), KDL machine config, CWF/SCWF.
- **Solr Dependencies:** icd10cm collection (diagnosis), encounter index (order context), patient search.
- **Redis Dependencies:** master caches (services, ordersets).
- **Scheduled Jobs:** HL7 outbound sync (disabled in config); EHRSyncJobs (mostly commented).
- **Reports:** printMedNonMedDetails.vm.
- **Security:** Doctor-initiated orders; nursing verification; no backend role check.
- **Permissions:** `Investigation.*` (16): ResultFinalize, ResultReviewConfirm, SampleCollection.Collect.
- **Cross-module Dependencies:** M04 (billing charge post), M15 (pathology), M16 (radiology), M07 (pharmacy orders), M09 (nursing), M05 (order sets).
- **Legacy Source Files:** `services/ClinicalServiceImpl.java`, `services/custom/AddServiceOrderServiceImpl.java` + `AddServiceOrderServiceHelper.java` (BR evidence), `services/custom/InvestigationServiceImpl.java`, `services/CWFServiceImpl.java`, `fjs-config_commons.json`, `models/trx/PatientEorder.java`, `transactionals/ExternalOrderDetails.java`, `js/cwf-module.js`, `js/scwf-module.js`.
- **Migration Complexity:** 5/5 — owner of the mega-transaction; order→billing atomicity must become saga; EMR/HL7 side effects.
- **Risks:** Mega-txn split (10 §4); sample-collection charge lock; CWF/SCWF semantics unknown (UNKNOWN-05).
- **Open Questions:** VISTA & CWF/SCWF semantics (02 open Q3); orderingSet runtime behavior.

---

## M09 — Nursing (Worklist, Notes, Allocation, Care Plans)

- **Target service:** 12. Nursing & Inpatient Care (+ 26. Worklist ownership)
- **Purpose:** Ward nursing operations: task management, documentation, patient assignment, handover.
- **Business Context:** 28-key config + template-only configs (nursingWorklist, careplan, charting, shifthandover); ~160 nursing templates; largest API surface (nursingList ~293 unique endpoints); docit-roles (roleNurse*).
- **Actors:** Staff nurse, Nurse in-charge, Nursing superintendent, Allied health staff (physio/OT/SLP per tiuxml AH forms), Social worker (SW forms).
- **Submodules:** Nurse worklist & task list (nurseTaskList); Patient coversheet; Notes (initial assessment, progress, procedure, addendum); Plan of care (careplan: NCP problems/goals); Charting (vitals, partograph, growth charts, health factors); Shift handover; Allocation (nurse→ward/doctor/patient, allied→clinic); Incident reporting & resolution; Notifiable disease reporting; Vitals entry (vitalmaster, vitalsConfiguration.json).
- **Features:** ShowNurseWorklistSchema; patient coversheet; nurse notes with note titles (notesTitle.json); care plan problems/goals; vitals with qualifiers & ranges; incident report/resolve; nurse task frequencies; shift handoff report (multiShiftHandoffReport); nursing bed transfer; temp prescription approval.
- **Screens:** ShowNurseWorklistSchema, ShowNursePatientNoteSchema, ShowNursePlanOfCareSchema, ShowPatientCoversheetSchema, incidentReportSchema, nurseWardAllocationSchema, outreach_data_schema, ShowNurseListSchema, nurseListForPatientAllocationschema, nurseListForDoctorAllocationSchema, nurseListForDepartmentAllocationSchema, alliedStaffListForPatientAllocationschema, staffListForClinicAllocationSchema, tempPrescriptionListSchema, consultPatientSchema, HistoryClinic, ancCoronaSurveySchema, incidentResolveSchema.
- **Routes:** `nursing:roleNurse`, `roleLab`, `roleRadiology`, `roleFrontOfficer`, `roleNursePatientView`, `roleNurseIncharge`, `roleAlliedPatientView`, `doctorPatientView`, `rolePatientCoversheet/:patientId/:admId/:patientIen/:visitIen`, `ipCoversheet/.../:opdipd`, `rolePatientNote`, `rolePlanofCare`, `roleNurseRostering`, `MyOpPatients`, `showMortuary`, `showTransfer`, `showDischarge`, `patientNotes/:patientId/:admId/:patientIen/:visitIen/:noteIen/:noteTitleId`, `showCarePlan/...`, `nursingOrder/:tab`, `NursePrescriptionList/:tab`, `compliantDiagnosisMap`, `orderingSet`, `growthchartdetails/add/:id`, `incidentReporting`, `addnewIncident/:...`, `editIncident/:...`, `resolveIncident/:...`, `DiagnosisTreatment`, `viewCoversheet/:patientId/:admId/:patientIen/:visitIen/:ipdOpd`, `nhncOrdering/...`, `nhncOperatorFlow`, `docPrescriptionList`, `annotation`, `charting:chartingApp`, `chartingExpand`.
- **Backend APIs (service @Path):** `/nursingList` (NursingServiceImpl — 300 endpoints, 17,350 LOC), `/clinical` (getNoteContent, getProviderList, healthfactorMaster, saveAppErrors), `/notedetailsdata`, `/actionNotif`, `/cwf`.
- **Business Rules (BR-* IDs):** none formalized in 06 (task/goal statuses evidenced in code; no BR-* assigned).
- **Status Lifecycle:** task status (completeNursingOrderTask); incident status (incidentResolveSchema); note signed status (signNotesFromTask, signTemplate); patient-med 1=ORDERED, 2=TAKEN, 3=MISSED, 4=SKIPPED; medication 1=ACTIVE, 2=DISCONTINUE, 3=EXPIRED; UNKNOWN-15.
- **Database Tables:** NurseNote, NurseTaskDetails, NurseTaskFrequencyDetails, PatientNote, PatientVitals, PatientAllergies, PatientComplaints, PatientDiagnosisDetails, PatientImmunizationDetails, PatientDischargeData, PatientAdvice, PatientEorder, CarePlan(+Details), T_INTAKEOUTPUT, T_PATIENTNURSINGORDERS, masters/nursing (notetitleMaster, nursingordermaster, flagMaster, vitalmaster, growthchart*, healthFactor*, diseaseMaster, icdcmCategory, dischargeDepartmentTemplate, cwfmapping, scwfmapping, diagnosisMaster, allergyMaster, chartconfiguration, imageAnnotationMaster, immunizationagegroupmaster, notifyablediesase, vaccine*, highRiskPregnancyFactorMaster, specialityMaster, noteCategoryMaster, documentClassMaster).
- **Stored Procedures:** none directly evidenced (T_nursePatientDetails_seq sequence exists).
- **Integrations:** tiuxml forms engine (368 forms — M40), EJS (outreach data), CWF/SCWF, ABDM (EHR sync of notes via syncEHR).
- **Solr Dependencies:** healthfactor collection (searchHealthfactor), icd10cm (searchAllIcd10cmInSolr), encounter collection (worklists), patient collection (coversheets).
- **Redis Dependencies:** `current_enc_` mirrors; master caches (vitalmaster etc.).
- **Scheduled Jobs:** none nursing-specific; Solr flag drainers feed icd10cm/healthfactor.
- **Reports:** dischargeSummary (plainText), patientPrescriptionPlainText, PatientNoteForAssesment.vm, PatientNoteForIntialAssesment.vm, barthelIndexReport.vm, ccmnote.vm, patientAllocationReport.vm, taskListPatientwise (template).
- **Security:** Nurse role flags (isNurseRole); docit-roles; incident severity/status; frontend-only gating (Nursing keys 26).
- **Permissions:** `Nursing.*` (26); `Investigation.*` for result views; `QueueManagement` for nurse desk.
- **Cross-module Dependencies:** M03 (IPD), M07 (med orders), M08 (service orders), M15/M16 (results), M26 (worklist), M27 (queue nurse desk), M40 (tiuxml forms), M04 (billing).
- **Legacy Source Files:** `fjs-config_nursing.json` (28 keys), `fjs-config_nursingWorklist.json`, `services/nurse/NursingServiceImpl.java` (god class), `services/ClinicalServiceImpl.java`, `services/custom/NoteDetailsServiceImpl.java`, `services/custom/ActionNotificationServiceImpl.java`, `modules/nursing/*`, `modules/worklist/*`, `modules/allocation/*`, `modules/careplan/*`, `modules/charting/*`, `modules/shifthandover/*`, `js/nursing-module.js`, `js/nursing-note-module.js`, `js/nursing-charting-module.js`, `js/incident-reporting-module.js`, `vitalsConfiguration.json`, `notesTitle.json`, `docit-roles.js`.
- **Migration Complexity:** 4/5 — god-class decomposition; tiuxml forms engine; vitals/charting configs; worklist ownership unclear (UNKNOWN-04).
- **Risks:** NursingServiceImpl single-file tx boundary; careplan/charting/shifthandover ownership ambiguity; note-sign workflow; EJS outreach coupling.
- **Open Questions:** Careplan/charting/shifthandover: features of M09 or standalone (02 open Q5); temp prescription approval.

---

## M10 — Diet & Kitchen

- **Target service:** 14. Diet
- **Purpose:** Dietetic care for inpatients and kitchen food production planning.
- **Business Context:** 4-key config; IP diet orders; kitchen production; ward-wise diet lists.
- **Actors:** Dietician, Kitchen staff, Ward nurse.
- **Submodules:** Diet item master, Diet sessions/types, Patient diet orders (IP), Menu & kitchen production, Food item orders, Ward-wise diet lists.
- **Features:** addpatdietorder; diet food order; today diet item menu; ward-wise patient diet lists; sign patient diet orders; cancel/update food orders.
- **Screens:** diet_foodorder_schema, diet_menu_item_schema, diet_schedule_schema, show_diet_kichen_schema.
- **Routes:** `diet:dietSchedule`, `dietMenuItems`, `dietScheduleStatus`, `DietKichenView`.
- **Backend APIs (service @Path):** `/diet`.
- **Business Rules (BR-* IDs):** none formalized in 06.
- **Status Lifecycle:** diet order status (updateDietFoodStatusForCancel); UNKNOWN-16.
- **Database Tables:** DietOrderDetails, DietItemScheduleDetails, TodayDietItemMenu, T_DIETORDER, T_DIETITEMSCHEDULEDETAILS, T_TODAYDIETITEMMENU, masters/diet (dietitemmaster, dietsessionmaster, diettypemaster), masters/hospital (timeofmealmaster, fooditemmaster, foodintakemaster).
- **Stored Procedures:** none evidenced.
- **Integrations:** none evidenced.
- **Solr/Redis:** none beyond master caches.
- **Scheduled Jobs:** none evidenced.
- **Reports:** getDietPatReport (API) — print template UNKNOWN-17.
- **Security:** Dietician role; frontend gating.
- **Permissions:** dietConfiguration master keys.
- **Cross-module Dependencies:** M03 (IPD), M04 (diet billing service).
- **Legacy Source Files:** `fjs-config_diet.json` (4 keys), `services/pas/DietServiceImpl.java`, `modules/diet/schemas|templates`, `modules/masters/diet/*`, `transactionals/DietOrderDetails.java`, `js/diet-kitchen-module.js`.
- **Migration Complexity:** 2/5 — small service, few tables.
- **Risks:** none significant.
- **Open Questions:** UNKNOWN-16/17.

---

## M11 — CSSD (Central Sterile Supply Department)

- **Target service:** 15. CSSD
- **Purpose:** Instrument kit lifecycle: formation → sterilization → dispatch to OT/procedures.
- **Business Context:** 15-key config; kit formation/preparation/sterilization/issue; store indents.
- **Actors:** CSSD technician, CSSD in-charge, OT staff (requests).
- **Submodules:** Kit formation, Kit preparation, Sterilization, Kit issue to procedures, Indent/stock for CSSD store.
- **Features:** kit formation/preparation lists; sterilization with machine & quality test masters; procedure CSSD kit request & issue; showItemReceiveListforCSSD (store); sterilization expiry tracking.
- **Screens:** showCurrentKitMasterListSchema, showCurrentKitFormationListSchema, showCurrentKitPreparationListSchema, showCSSDItemKitSterilization(+Filter), showProcedureCssdIssue, procedureCssdKitRequestFilterSchema, cssdkitissueitem, showCSSDKitRequestList, showIndentDetailsEditableOnCSSDSchema, showItemReceiveListforCSSD, showKitRequestLstForCssdStore, showProcedureCSSDKitDetailSchema.
- **Routes:** `cssd:kitFormulation`, `kitPreparation`, `CSSDItemReceiveandReturn`, `CSSDItemIssueNew`, `CSSDItemReceiveReturnNew`, `cssdItemValidate`, `kitMaster`, `otKitItemRequest`, `cssdItemKitSterilization`, `procedureCssdRequest`, `procedureCssdIssue`, `showCssdReportList`.
- **Backend APIs (service @Path):** `/cssd` (46 endpoints: addKitMaster, freezeKitRequest, cssdItemIssue, getProcedureCSSDIssue, updateCssdRequisition*Hold, verifyMasterKitPreparation…).
- **Business Rules (BR-* IDs):** none formalized in 06.
- **Status Lifecycle:** sterilization status; kit issue status; expiry (sterilizationexpirymaster); request hold/release (updateCssdRequisitionForHold).
- **Database Tables:** CssdKitFormation(+Details), Cssdkitissueitem(+details), CssdItemSterilizationDetails, ProcedureCssdRequest(+Details), ProcedureCssdKitDetails, ProcedureCssdIssue(+Detail), T_CSSDKITFORMATION, T_CSSDKITISSUEITEM, T_STERILIZATION*, T_PROCEDURECSSD*, masters/cssd (sterilizationmaster, sterilizationmachinemaster, sterilizationtypemaster, sterilizationqualitytestmaster, sterilizationexpirymaster).
- **Stored Procedures:** none evidenced.
- **Integrations:** inventory store (indent/issue to CSSD store).
- **Solr/Redis:** item collection (kit item search); master caches.
- **Scheduled Jobs:** none evidenced.
- **Reports:** cssdReport.vm, requisitionKitDetail.vm, showCssdReportSchema (reports).
- **Security:** CSSD operator roles; frontend gating.
- **Permissions:** cssdConfiguration master keys.
- **Cross-module Dependencies:** M13 (inventory items), M12 (OT), M04 (service charges for kits).
- **Legacy Source Files:** `fjs-config_cssd.json` (15 keys), `services/cssd/CSSDServiceImpl.java`, `modules/cssd/schemas|templates`, `modules/masters/cssd/*`, `transactionals/CssdKitFormation.java`, `js/cssd-module.js`.
- **Migration Complexity:** 2/5 — contained lifecycle; store interplay moderate.
- **Risks:** Sterilization expiry tracking correctness.
- **Open Questions:** none significant.

---

## M12 — Operation Theatre (OT) & Surgery

- **Target service:** 11. OT & Surgery
- **Purpose:** Surgical case planning, scheduling, resource allocation and charge capture.
- **Business Context:** 14-key config; ~80 OT templates; theatre/table hierarchy; procedure masters & checklists; anesthesia; surgery-head billing.
- **Actors:** Surgeon, Anesthetist, OT in-charge, OT nurse, Billing.
- **Submodules:** OT theatre/table config; Procedure master & checklist; Surgery scheduling; Anesthesia (pre/PACU/post notes); Surgery charges & billing estimation; Surgery heads; Procedure performance; Staff/doctor scheduling.
- **Features:** addNewSurgerySchema; slot/conflict checks (checkDoctorAllreadyBookedForSameTime, checkOTAvailability); procedure checklists; pre-anaesthesia check & PAC notes; surgery charges (addSurgCharge, getSurgeryHeadDetailsForBillEstimation); OT equipment booking (addProcedureEquipment); surgery order list from nursing; procedure performance tracking.
- **Screens:** surgeryScheduleSchema, addNewSurgerySchema, otscheduledetails, addOTCharge, addOTBillingSchema, AddPatientProcedureRateDetailsSchema, procedurePerformanceSchema, SurgeryOrderList, surgeryheads, addPatientProcedureRateSchema, addProcSchema, addSurgHeadSchema, itemCategorySchemaForOT, notesCollectionSchema, patientProcedureRateDetailsSchema, showSurgeryOrderList.
- **Routes:** `ot:surgList`, `surgSchedule`, `procPerformance`, `otSchedule/:otId/:tableId`, `otScheduleEvent`, `patMovementAudit`, `nurseScheduleEvent`, `itemDetailsEvent`, `surgSchdule/:otId/:tableId`, `addNewSurg`, `surgDetails`.
- **Backend APIs (service @Path):** `/otscheduler` (79), `/otconfig` (35), `/otdetails`, `/surgerydetails`.
- **Business Rules (BR-* IDs):** BR-OT-001 (scheduling prerequisites — BEOT004), BR-OT-002 (OT pre-billing `otprocedure.pre.billing`; doctor-share skipped for OT tariff services).
- **Status Lifecycle:** surgery/schedule status (operationstatusmaster, getSurgeryCompleteList, cancelSurgeryPatient); procedure `isOtProcessDone`; UNKNOWN-18.
- **Database Tables:** PatientProcedureAnesthesiadetails, PatientProcedureRateDetails, PatientScheduleAnasthesiaDetails, Patientprocedurechecklistitems/detailsDto, T_OTSCHEDULE(+DETAILS), T_SURGERYDETAILS, T_ANESTHESIANOTES, T_SURGCHARGES, PatientBillEstimationSurgeryHead, masters/ot (ottheatremaster, ottablemaster, proceduremaster, procedurechecklist*, anesthesiatypemaster, anesthesianotes, surgerynotes, surgeryheadmaster(+details), otschedule, operationresultmaster, operationstatusmaster, intra/post/preoperativeinstructionsmaster, procedurecategorymaster, proceduresubcategorymaster, proceduretypemaster, procedureequipmentdetails, procedureitemdetails).
- **Stored Procedures:** none directly evidenced beyond tariff/charge procs.
- **Integrations:** inventory (OT items/equipment issues), CSSD (kit requests).
- **Solr/Redis:** patient search (10 §2.1 row 11); master caches.
- **Scheduled Jobs:** none evidenced.
- **Reports:** surgeryScheduleListDetails.vm, OTReport.vm; anesthesia forms in tiuxmls.
- **Security:** Surgeon/anesthetist roles; conflict checks; frontend gating.
- **Permissions:** otConfiguration master keys.
- **Cross-module Dependencies:** M03 (IPD patient), M08 (orders), M04 (billing), M11 (CSSD kits), M13 (OT items/equipment), M07 (procedure prescriptions).
- **Legacy Source Files:** `fjs-config_ot.json` (14 keys), `services/pas/OTSchedulerServiceImpl.java`, `services/pas/OTConfigServiceImpl.java`, `services/pas/OTDetailsServiceImpl.java`, `services/pas/SurgeryDetailsServiceImpl.java`, `modules/ot/schemas|templates` (~80), `modules/masters/ot/*`, `transactionals/PatientProcedureAnesthesiadetails.java`, `js/operation-theatre.js`.
- **Migration Complexity:** 3/5 — scheduling + billing integration; moderate surface.
- **Risks:** OT slot conflict logic; pre-billing flag; procedure checklist versioning.
- **Open Questions:** Surgery-head billing estimation details.

---
## M13 — Inventory & Stores

- **Target service:** 10. Inventory & Procurement
- **Purpose:** Medical & general stores: item catalog, stock levels, demand and issue.
- **Business Context:** 70-key config; item/batch/stock; indents (single/asset/direct); reorder; expiry/damage; e-Aushadhi integration; largest JS controller (inventory-module.js 1.08 MB, 301+ API refs).
- **Actors:** Store in-charge, Store clerk, Indentor (ward/dept), Purchase officer, Store supervisor (approvals).
- **Submodules:** Item master & categories; Stock & batches; Indents (single/asset/direct issue); Purchase requests & approvals; Stock adjustments; Item enquiry; Expiry & damage returns; Reorder; Store management; Material consumption; Item issue/return.
- **Features:** addNewIndentSchema; showIndentDetails (editable/on-approved/on-freezed/on-second-level-approved); item issue lists; stock lists & adjustments; item enquiry; expiry/damaged item return to supplier; reorder lists; direct item issue; store consumption; batch/expiry update.
- **Screens:** itemMasterSchema, stockListSchema, showIndentListSchema, showGRNSchema, showCurrentStockListSchema, itemEnquirySchema, getReorderListForDirectItemIssue, showStockAdjustmentListSchema, expiryItemReturnSchema, addNewItemSchema, assetRegisterList, damagedItemReturnIndent, expiredDamagedItemReturnIndent, expiryItemReturnToSupplierSchema, getItemEnquirySchema, hospitalWiseDemandListSchema, issueItemReturn, itemWiseDemandListSchema, materialConsumptionDetails, returnItemRecieve, returnToSupplierSchema, showAllItemIssueListSchema, showCurrentReorderListSchema, showDirectItemIssueListSchema, showExpiredItemListSchema, showItemReceiveList, showRecallExpiredItemSchema, showReturnFlowStore, showStockLocationSchema, showStoreComsumptionListSchema, updateBatchExpiry.
- **Routes:** `inventory:inventory`, `currentPO`, `poRaisedForOtherUnit`, `poByHQ`, `allApproval`, `gRNList`, `indentListofStore`, `reOrderList`, `expiredDamagedItemReturnIndent`, `userStore`, `issueItem`, `consumeItemforStore`, `StoreStockAdjustment`, `holdItemBatches`, `itemReceiveList`, `returnItem`, `returnItemRecieve`, `expiredItemList`, `recallExpiredItem`, `expiryItemReturnList`, `QcStatusUpdate`, `itemSummary`, `grn`, `approveGrn`, `itemMasterList`, `itemGroupRef`, `purchaseIndent`, `consumptionList`, `stockAdjustmentList`, `quotation`, `itemEnquiry`, `opeiningBalance`, `stockLocation`, `updateBatch`, `returnToSupplier`, `expiryItemReturn(ToSupplier)`, `grnReturnList`, `issueItemReturn`, `directIssueItem`, `consignmentGRNList`, `itemStockList`, `orderList`, `receivedpolist`, `assetRegisterList`, `departmentalConsumption(List)`, `purchaseRequest`, `consolidatePurchaseRequest`, `otherUnitPOList`, `multipurposeGRN`, `updateGrn/:grnId`, `raiseDirectPO`, `stockTransfer`, `raiseRMRSPORequest`, `customOrderList`, `customProcessOrderDetails`, `stockList`, `expire`.
- **Backend APIs (service @Path):** `/itemmaster`, `/itemcategory`, `/itemstock`, `/openingbalance`, `/materialconsumption`, `/expiryitem`, `/inventoryStockList` (235 endpoints), `/eAushadiIndent`.
- **Business Rules (BR-* IDs):** none formalized in 06; indent state machine evidenced (draft/freezed/approved/issued per schema names); reorder from reorderAndSafetyMaster.
- **Status Lifecycle:** indent status (draft/freezed/approved/issued); PO status (M14); stock adjustment status; QC status (qcStatusUpdate); item SOLR_STATUS flag.
- **Database Tables:** ItemStock, ItemClinic, ItemSupplier, StoreItemCategory, T_ITEMSTOCK (15 refs), T_ITEMCLINIC (17), T_ITEMBATCHMASTER, T_INDENT, T_GRN*, T_QUOTATION, T_PURCHASEORDER, T_SUPPLIER, T_RATECONTRACT, masters/inventory (itemmaster, itemcategory, itemgroup, brandmaster, molecule, therapeuticclass, unitofmeasure, store, storeagetype, supplier(+master), taxmaster, rateContractSchema, reorderAndSafetyMaster, salemarkupmaster, grnitemchecklist, grnrejectreasonmaster, termsofpayment, termsAndCondition, discardrejectionmaster, empanelmaster, gastypemaster, genericbrandmapping, itemcompany, itemSupplierRate, mappedProductItems, preganancyclass, unMappedProductItems), M_ITEMMASTER (18 refs).
- **Stored Procedures (CIMS_*):** CIMS_ADDITEMSTOCK, CIMS_ADDGRN, CIMS_ADDINDENT (live-called from InventoryJobSchedule/BillingCommonUtil).
- **Integrations:** e-Aushadhi (InventoryEAushadiIntegrationImpl `/eAushadhiIndent` — indent create/issue/QC/return, item-master sync 18:00, auto-indent; Basic-auth header hard-coded), Janta/MDM (syncJantaItemMaster), Kaysons (wholesaler via e-Aushadhi RMSC).
- **Solr Dependencies:** item collection — flag-based drainer syncItemsToSolrJob (5s fixedDelay, SOLR_STATUS=0, batches 500); searchAllItemsInSolr; manual startItemSyncingToSolr.
- **Redis Dependencies:** `INV_RI_<indentId>` receive lock (60s, ServiceLayerAspect:1192-1201); item master cache; job kill-switches.
- **Scheduled Jobs:** syncItemsToSolrJob (5s); InventoryJobSchedule (5 jobs); eAushadiItemStockUpdate.schedule (04:00).
- **Reports:** indentDetails(+OfApproved/OfFreezed).vm, invGrnDetails.vm, goods_issue.vm, invIndentIssueDetails.vm, invItemCurrentStockList.vm, invRecvItemDetail.vm, returnToSupplier.vm, expiryItemDetails.vm, damageItemDetails.vm, itemBatchDetails.vm, itemEnquiryDetails.vm, stockAdjusmentDetails.vm, grnBarCode.vm, indentIssueBatchBarCode.vm, UtilizatioIndentPrint.vm.
- **Security:** `Inventory.AllApproval` — only backend `checkPermission()` call site (InventoryServiceImpl:11588); store-scoped users (userstoreIds); frontend keys 70.
- **Permissions:** `Inventory.Indent.StoreIndent.RaiseNewIndent`, `Inventory.Procure.POLIST.ApprovePO`, `Inventory.AllApproval`, `Inventory.Store.Add`.
- **Cross-module Dependencies:** M14 (procurement), M07 (pharmacy stock), M11/M12 (CSSD/OT issues), M04 (billing for sale items), M38 (e-Aushadhi).
- **Legacy Source Files:** `fjs-config_inventory.json` (70 keys), `services/custom/InventoryServiceImpl.java` (12,070 LOC — god class), `services/pas/ItemMasterServiceImpl.java`, `services/pas/ItemCategoryServiceImpl.java`, `services/pas/ItemStockServiceImpl.java`, `services/pas/OpeningBalanceServiceImpl.java`, `services/pas/MaterialConsumptionServiceImpl.java`, `services/pas/ExpiryItemServiceImpl.java`, `services/custom/InventoryEAushadiIntegrationImpl.java`, `modules/inventory/schemas|templates`, `modules/masters/inventory/*`, `js/inventory-module.js`, `js/itemEnquiry-module.js`.
- **Migration Complexity:** 4/5 — stock concurrency, pharmacy dual-write, e-Aushadhi coupling, god-class decomposition.
- **Risks:** T_ITEMSTOCK decrement race (10 §3.4); Pharmacy/Inventory stock ownership (10 §7 Q1); e-Aushadhi hard-coded secrets; SOLR_STATUS dual semantics.
- **Open Questions:** Stock ownership boundary; auto-indent triggers; reorder formula location.

---

## M14 — Procurement (Purchase Orders, Quotations, GRN)

- **Target service:** 10. Inventory & Procurement (procurement sub-domain)
- **Purpose:** Sourcing, ordering and receiving of stock items.
- **Business Context:** PO/GRN lifecycle; quotations & comparison; rate contracts; supplier/in-house/custom POs; RMRS PO requests.
- **Actors:** Purchase officer, Store in-charge, Supplier, Accounts (payment terms), QC staff.
- **Submodules:** Purchase requests; Quotations & comparison; Purchase orders (supplier/in-house/custom); PO approval & freeze; GRN (receipt) & QC; Rate contracts; Indent invoice (supplier billing); Returns to supplier; Consignment GRN.
- **Features:** compareQuotationSchema; approvePOPageSchema; freezeCurrentPOListSchema; showGRNSchema; showConsignmentGRNListSchema; showApproveGrnListSchema; paymentDetailModeForGRNSchema; supplier PO; custom orders; renegotiation (showRenegotiateSchema).
- **Screens:** showPurchaseRequestSchema, showPODetailsSchema, showPOPageSchema, approvePOPageSchema, showGRNSchema, compareQuotationSchema, supplierPoSchema, customProcessOrder, showCurrentPurchaseOrderList(forOtherUnit)Schema, showCurrentGRNListSchema, showGrnReturnListSchema, showOrderListforInHouseSupplierSchema, ShowIssuedPurchaseOrderDetails/ListForGRNSchema, showPurchaseOrderDetailsSchema, showPoIssueDetails, updatePODetailsForExpDate, updatePurchaseOrderSchema, freezeCurrentPOListSchema, customOrderAssignee, customProcessItemDetails.
- **Routes:** `inventory:currentPO`, `poRaisedForOtherUnit`, `poByHQ`, `allApproval`, `gRNList`, `grn`, `approveGrn`, `quotation`, `consignmentGRNList`, `orderList`, `receivedpolist`, `consolidatePurchaseRequest`, `otherUnitPOList`, `multipurposeGRN`, `updateGrn/:grnId`, `raiseDirectPO`, `raiseRMRSPORequest`, `customOrderList`, `customProcessOrderDetails`, `purchaseRequest`.
- **Backend APIs (service @Path):** `/purchaseorder`, `/quotation`, `/rmrs` (PO request lists), `/inventoryStockList` (GRN/PO endpoints).
- **Business Rules (BR-* IDs):** none formalized in 06; PO approve/freeze state machine; QC gate via qcStatusUpdate.
- **Status Lifecycle:** PO status (freezed/approved); GRN status (qcStatusUpdate, showApproveGrnList); quotation status; renegotiation; UNKNOWN-19.
- **Database Tables:** PurchaseRequest(+Details), PurchaseOrderRequest, RateContract(+Details/Units/View), IndentInvoice(+Details), TermsAndCondition, T_PURCHASEORDER, T_GRN*, T_CONSIGNMENTGRN, T_QUOTATION.
- **Stored Procedures (CIMS_*):** CIMS_ADDGRN, CIMS_ADDINDENT.
- **Integrations:** RMRS (funds via PO requests), e-Aushadhi (PO/GRN sync path).
- **Solr/Redis:** item collection (stock receipt updates); master caches.
- **Scheduled Jobs:** InventoryJobSchedule (GRN/expiry tasks).
- **Reports:** poDraft.vm, poDraft_New.vm, quotationDetails.vm, eqpGrnDetails(+_GST).vm, grnBarCode.vm.
- **Security:** PO approval permission (`Inventory.Procure.POLIST.ApprovePO`); QC role.
- **Permissions:** Inventory procure/approval keys.
- **Cross-module Dependencies:** M13 (items/stock), M04 (payment), M23 (RMRS funds); QC-for-lab per source "M16" — ambiguous (heading M16=Radiology; QC-for-lab evidence unclear).
- **Legacy Source Files:** `services/pas/PurchaseOrderServiceImpl.java`, `services/pas/QuotationServiceImpl.java`, `modules/inventory/schemas` (PO/quotation/GRN), `transactionals/PurchaseRequest.java`, `transactionals/RateContract.java`, `transactionals/IndentInvoice.java`, `models/trx`, `js/inventory-module.js`, `procurement.js`.
- **Migration Complexity:** 3/5 — state machine heavy, moderate size; RMRS coupling.
- **Risks:** PO/GRN status split across schemas; consignment GRN variants.
- **Open Questions:** QC-for-lab placement; in-house supplier order flows.

---

## M15 — Pathology / Laboratory

- **Target service:** 7. Lab (Pathology)
- **Purpose:** End-to-end laboratory workflow from order to released report.
- **Business Context:** 57-key config; ~100 pathology templates; machine interfacing (KDL); microbiology; critical alerts; WhatsApp report delivery.
- **Actors:** Lab technician, Lab supervisor (reviewer/release), Pathologist, Sample collector, KDL (instrument) interface.
- **Submodules:** Lab order booking; Sample collection & barcode; Sample acceptance/rejection; Result entry (resulting); Result review; Report release; Critical alerts; Microbiology (culture/sensitivity/antibiotic); Machine interfacing (KDL verify, machine-wise review); Test master/profiles/templates; Lab workload.
- **Features:** pathoOrder; sample collection with barcode; accept/reject; resulting per parameter; result review; report release; critical alert handling; microbiology with organism/antibiotic panels; KDL machine result verify; workload lists; agency master (pathagencymaster); grouping & tests.
- **Screens:** pathoOrderSchema, sampleCollectSchema, sampleRejectionSchema, resultingDetailSchema, resultReviewSchema, reportReleaseSchema, criticalAlertResultSchema, microbiology-schema, machineWiseResultReviewSchema, pathoGenerateBarCodeSchema, acceptReject(Filter/ReportDetails/Report)Schema, criticalAlert(Filter/ResultReportDetails/ResultReport/Result)Schema, getLaboratoryReports, microbiologyAntibiotic-schema, pathagencymaster, pathoFilterSchema, pathoGenerateBarCodeSchema(+ForBloodBank), pathoGroupingAndTest, pathoKdlVerify, pathoReportDetails/Report/ResultSchema, pathosamplesutability, pathoSchema, pathoShowCovidStatus, pathoShowTestStatus(+New), pathosubtestmaster, pathoUpdateOrderStatus, PatientFilterLabSchema, reportRelease(+Details/Filter/Microbiology/Report/Template)Schema, resultingDetail/Report/TemplateReportSchema, resultReview(+Filter/Microbiology/ReportDetails/Report/TemplateReportDetails/TemplateReport)Schema, sampleResult(Filter/Report)Schema, sampleRetestSchema, serviceStatusSchema, showLabLoadListSchema, showPatientWorkList.
- **Routes:** `pathology:lab`, `sampleCollection/:opdipd/:mrnNo/:catg(/:date)`, `sampleAcceptReject/...`, `sampleResult/...`, `sampleCriticalAlert/...`, `sampleResultReview/...`, `sampleReportRelease/...`, `sampleMachineWiseResult(/:...)`, `sampleGenerateBarcode`, `LabN`, `selectedOrderType`, `printAllReport`, `workLoadList`, `groupingAndTest`, `kdlVerify`, `showStatus`, `showLabResult`, `showCovidLabList`, `updateOrderStatus`, `updateServiceStatus`.
- **Backend APIs (service @Path):** `/pathoOrder` (114), `/investigation`, `/machineConfig`, `/kdl` (getResultsFromKDL on /patient), `/patientrequisition` (blood bank barcode).
- **Business Rules (BR-* IDs):** BR-PATH-001 (sample collection locks charge removal — LEX001; shared M08/M04).
- **Status Lifecycle:** test status (pathoShowTestStatus, pathoUpdateOrderStatus); sample status (acceptRejectSchema); result status (resultReviewSchema); report release (reportReleaseSchema); critical alert flag.
- **Database Tables:** T_PATHORDERBOOKING(+DETAILS), T_PATHOPATIENTPARAMETERDETAILS, T_MACHINECONFIGURATION, PathoPatientDocs, PathOrderBookingDetailDocuments, IhmsCounts, masters/pathology (pathotestmaster, pathoparametermaster, pathoprofilemaster, pathotemplatemaster, pathosamplemaster, machinemaster, organismmachinmap, pathantibioticmaster, pathoremarksmaster, pathozonemaster, pathocategory, discardreasonmaster, antibioticmachinemap, containermaster, cultureNegativeMaster, errorflagmaster, gendercategory, labconfigmaster, machinetestmaster, organmaster, parameterresultingmethod, pathoCabinMaster, patholocationmaster, pathoparameterunits, pathoparammachinemap, pathorganismmaster, pathosubspecialization, pathsensitivityresultmaster), M_PATHOTESTMASTER (12 refs).
- **Stored Procedures (CIMS_*):** CIMS_ADDPATHORDERBOOKING.
- **Integrations:** KDL machine interface, HL7/EMR (Lab_Results in tiuxml), Blood bank cross-matching linkage (pathoGenerateBarCodeSchemaForBloodBank), WhatsApp report delivery (PathoOrderServiceHelper.sendCategoryWiseReportOnWhatsApp), eUpkaran (lab equipment), EXT LIMS (state lab, disabled).
- **Solr Dependencies:** patient collection (worklists); item collection (consumables).
- **Redis Dependencies:** master caches (test/profiles); `current_pat_` mirrors.
- **Scheduled Jobs:** triggerWhatsappReportsInQueue (15-min window 14:00–07:00); EXT LIMS staging (disabled).
- **Reports:** pathoAllResult.vm, pathoMicroReport.vm, pathoParameterReport.vm, pathoTemplateReport.vm, pathoTestStatusReport.vm, showLabReport.vm, labloadList.vm, printLabResult.vm, allPathoReport.vm; plainText variants.
- **Security:** roleLab; result review/release separation (`Investigation.ResultFinalize/ResultReviewConfirm/SampleCollection.Collect`).
- **Permissions:** Investigation keys (16) + pathology master keys.
- **Cross-module Dependencies:** M08 (orders), M01 (patient), M04 (billing), M13 (consumables), M17 (blood bank barcode), M38 (KDL).
- **Legacy Source Files:** `fjs-config_pathology.json` (57 keys), `services/custom/PathoOrderServiceImpl.java` (14,280 LOC — god class), `services/custom/PathoOrderServiceHelper.java`, `services/custom/MachineConfigServiceImpl.java`, `modules/pathology/schemas|templates` (~100), `modules/masters/pathology/*`, `transactionals/PathoPatientDocs.java`, `js/patho-booking-module.js`, `js/patho-modules.js`, `docit-roles.js` (roleLab).
- **Migration Complexity:** 4/5 — god-class; machine interface; critical-alert routing; WhatsApp; sample-lock rule.
- **Risks:** KDL sync semantics; release/review separation; report templates; blood-bank barcode coupling.
- **Open Questions:** EXT LIMS data flow; machine config scope; critical alert thresholds.

---

## M16 — Radiology

- **Target service:** 8. Radiology
- **Purpose:** Imaging orders, acquisition queue, reporting and report release.
- **Business Context:** 9-key config; modality masters; template reports; PACS image handling; rad orders visible in nursing flows.
- **Actors:** Radiologist, Radiology technician, Booking clerk.
- **Submodules:** Order booking; Queue; Result entry (report); Template reports; Retest/reschedule; Image handling (addRadImg, showRadSampleImg); Test master.
- **Features:** radOrderBookingSchema; radiology queue; report entry; retest; reschedule; template-based reports (radtemplatemaster, radtemplateresult).
- **Screens:** radOrderBookingSchema, radReportSchema, radReportDetailsSchema, radRescheduleSchema, radRetestSchema, patientFilterRadSchema, getRadiologyReports, addRadImg, radFilterSchema.
- **Routes:** `radiology:radlab(/:p_id/:order_no/:date/:opd_ipd)`, `printReport`, `reSchedule`, `radReTest`.
- **Backend APIs (service @Path):** `/radiology` (12), `/reports` (getRadiologyReports), `/serviceOrder` (rad order creation at billing).
- **Business Rules (BR-* IDs):** BR-BILL-003 (rad order auto-creation at billing — with M04); BR-PATH-001 (charge removal lock applies to rad).
- **Status Lifecycle:** rad order status; report status; IsImageCaptured; retest/reschedule; UNKNOWN-20.
- **Database Tables:** T_RADORDERBOOKING(+DETAILS), masters/radiology (radtestmaster, radtestcategory, radmodalitymaster, radtemplatemaster, radtemplateresult, radfilmsize, radCabinMaster, imagediscardreasonmaster).
- **Stored Procedures (CIMS_*):** CIMS_ADDRADORDERBOOKING.
- **Integrations:** PACS/imaging (addRadImg, showRadSampleImg).
- **Solr/Redis:** patient collection (booking search); master caches.
- **Scheduled Jobs:** none evidenced.
- **Reports:** showRadiologyReport (template), pathoRadioTemplate (shared), radioFinalizeTemplate.html.
- **Security:** roleRadiology docit role; frontend gating.
- **Permissions:** radiologyConfiguration master keys; Investigation keys for review.
- **Cross-module Dependencies:** M08 (orders), M04 (billing), M01 (patient).
- **Legacy Source Files:** `fjs-config_radiology.json` (9 keys), `services/pas/RadiologyServiceImpl.java`, `modules/radiology/schemas|templates`, `modules/masters/radiology/*`, `js/operation-theatre.js` (rad order rows in nursing), `docit-roles.js` (roleRadiology).
- **Migration Complexity:** 2/5 — small service; PACS integration moderate.
- **Risks:** Image storage (MinIO disabled by default → filesystem fallback).
- **Open Questions:** PACS direction; retest/reschedule semantics.

---

## M17 — Blood Bank

- **Target service:** 13. Blood Bank
- **Purpose:** Whole blood & component management: donor to transfusion.
- **Business Context:** 40-key config; donor lifecycle; bag lifecycle; cross-matching; e-Raktkosh; district stock exchange; 21 tables.
- **Actors:** Blood bank technician, Blood bank officer, Donor counselor, Requisitioning ward, External hospital.
- **Submodules:** Donor registration & screening (voluntary/camp/replacement/autologous); Blood collection & component separation; Testing (bloodtestmaster/result); Cross-matching & issue; Requisitions (patient/external hospital/external storage center/admin/camp); Stock (opening, bag status); Indents between units; Discard; Bag returns; e-Raktkosh linkage.
- **Features:** donorRegistartion; VoluntaryDonation/CampDonation/ReplacementDonation/AutologousDonationSchema; bloodComponentSeperationSchema; bloodCrossMatchingListSchema; patient requisitions; cross-unit orders; blood bag returns; discard; indent issue/receive/reject between units; e-Raktkosh data update service (eraktkoshdataupdateservice).
- **Screens:** donorRegistartion, bloodComponents, bloodComponentSeperationSchema, bloodCrossMatchingListSchema, patientRequisitionList, showBloodStockListSchema, showBloodDiscardListSchema, showBloodBankIndentListSchema, showBagsReturnListSchema, VoluntaryDonationSchema, CampDonationSchema, addAdminRequisition, addBloodBagOpeningStock, afterAddOpeningStock, AutologousDonationSchema, bagReturnDetailsSchema, bloodBagReturnSchema, blooddonortypemaster, bloodRequisitionCrossMatchingSchema, bloodtestmaster, bloodtestresultmaster, campRequisitionSchema, crossMatchingBloodbagDetails, crossMatchingForm, crossMatchingPatientDetails, crossUnitOrderSchema, crossUnitOrdersListSchema, donorrejectionreasonmaster, externalBloodStorageCenterRequisition, externalHospitalRequisition, ExternalPatientRequisitionSchema, openingBloodStockSourceDetailsSchema, patientRequisitionDetails, ReplacementDonationSchema, replacementDonorHistory, requestDetailsGeneric, showCrossMatchedBagsSchema, showDiscardedBagsSchema, showDonorListSchema.
- **Routes:** donorRegistration, donorList, campDonation, patientRequisition(+external), crossUnitOrder(s), bloodComponent, bloodBagComponentSeperation/Extraction, bagReturns, crossMatchedBags, redCellSerology, transfusionTransmittedDiseases, crossMatchingList, newCrossMatchingTest, bloodCrossMatching, campRequisition, issueBloodBag, bloodBagDiscard, showIndentList, raiseIndent, bloodBagTrail, retrunBloodbag, eraktkoshRequest, replacementDonorHistory, BloodBanKReOrder, BloodDiscardList, bloodBagOpeningStock, addNewBloodComponent.
- **Backend APIs (service @Path):** `/bloodtestmaster` (115), `/patientrequisition`, `/patient` (requisition search).
- **Business Rules (BR-* IDs):** BR-BLD-001 (requisition status machine 1→2→3/10), BR-BLD-002 (auto decross 48h — 2880 min, daily 00:00), BR-BLD-003 (bag expiry daily 23:00), BR-BLD-004 (requisition expiry — orderValidationPeriod), BR-BLD-005 (donor auto-cancel 72h — DNR_CANCELLATION=7).
- **Status Lifecycle:** requisition 1=NEW, 2=cross-matched, 3=issued, 10=cancelled/expired; bag flags (isbagexpired, isdecrossed, isreserved, isbagextracted, isdiscard, isissued, istested, iscrossmatched); donor donationStatus 1=NEW, 2=FIT, 3=UNFIT/VOLUNTORY, 4=ACCEPTED, 5=REJECTED/DONATED (collision!), 7=CANCELLATION, 10=PERM_UNFIT, 11=TEMP_UNFIT; indent status (issue/receive/reject/cancel).
- **Database Tables:** t_bloodbagcollection, t_bloodbagissue, t_bloodbagreturns, t_bloodbagdecross, t_bloodbagissuedemandslip, T_BLOODBAGRECEIVE, t_blooddonorregistration(+details), t_donorscreeningtest, t_donormedicalexaminationresult, t_bloodcrossmatchingresult(+details), T_BloodBankIndent(+Details), T_BloodBagExtraction, T_BLOODBAG, T_BLOODCOMPONENT, T_BLOODREQUISITION, T_BLOODDONOR, T_BLOODDONATIONCAMP, masters/bloodBank (bloodgroupmaster, bloodcomponentmaster, bloodbagtypemaster, bloodbagtypecomponentmaster, bloodscreeningmaster, bloodtestmaster, bloodtestresultmaster, blooddiscardreasonmaster, blooddonationcampmaster, externalbloodstoragecenter, externalhospitalmaster, eraktkoshcomponentmapping, donordefaultvitalvalue, importerMaster, modeofscarpsale, additivemaster, bloodadversereactionmaster, bloodcomponentclinic, bloodgroupcrossmatching, bloodrequisitionreasonmaster, requesttypemaster).
- **Stored Procedures:** none directly evidenced; eRaktkosh jobs in ApplicationJobs.
- **Integrations:** e-Raktkosh (eraktkoshdataupdateservice, EraktkoshComponentcodeMapping, eRaktkoshDonation.schedule), external storage centers/hospitals, pathology serology linkage.
- **Solr Dependencies:** patient collection index from BloodBankServiceImpl:1429-1625, PatientRequisitionServiceImpl:746-854.
- **Redis Dependencies:** master caches (blood group etc.).
- **Scheduled Jobs:** updateExpiredBloodRequisitionStatusJob, updateExpiryStatusOfTheBloodBagsJob (23:00), decrossBloodBag (00:00), donor auto-cancel (ApplicationJobs:832-848), eRaktkosh nightly (ApplicationJobs:1205,1383).
- **Reports:** bloodBankIndentDetails.vm, bloodBagDemandSlip.vm, bloodCrossMatchingreport.vm, bloodDonationCertificate(+New).vm, bagLabel.vm; 17 blood-bank report routes in reports module.
- **Security:** Blood bank technician roles; discard reason masters; frontend gating.
- **Permissions:** bloodConfigurationMasters keys.
- **Cross-module Dependencies:** M01 (patient/donor search), M08 (requisition orders), M13 (consumables), M04 (billing), M15 (serology — source `02` wrote "M16 (serology?)", ambiguous).
- **Legacy Source Files:** `fjs-config_bloodBank.json` (40 keys), `services/pas/BloodBankServiceImpl.java`, `services/pas/PatientRequisitionServiceImpl.java`, `modules/bloodBank/schemas|templates`, `modules/masters/bloodBank/*`, `transactionals/t_bloodbag*`, `js/blood-bank-module.js` (653 KB).
- **Migration Complexity:** 4/5 — 21 tables, 5 BR state machines + 4 jobs, e-Raktkosh, donor status literal collisions.
- **Risks:** DNR_REJECTED==DNR_DONATED==5 literal collision (06 §C.3); requisition header/detail asymmetry (R7); decross/expiry job timing.
- **Open Questions:** Serology placement (M15 vs M17); e-Raktkosh sync direction/frequency (02 open Q10).

---

## M18 — Biomedical Waste (BMW)

- **Target service:** 19. Bio-Medical Waste
- **Purpose:** Biomedical waste tracking (per BMW rules).
- **Business Context:** 1-key config; waste generation & segregation per admission; bag status; handover to firm.
- **Actors:** Ward staff, BMW handler.
- **Submodules:** Waste generation & segregation, bag management, status tracking.
- **Features:** showBioMedicalWasteStatusSchema; waste details per admission; 14 endpoints (addNewBioRequest, processBioRequest, handOverBags, handedOverToFirm, updateBagStatus, getBagWeightByBarcode, validateBarcode…).
- **Screens:** showBioMedicalWasteStatusSchema.
- **Routes:** `biomedicalwaste:showBioMedicalWasteStatus` (1 route).
- **Backend APIs (service @Path):** `/bioMedicalWasteManagement`.
- **Business Rules (BR-* IDs):** none formalized in 06.
- **Status Lifecycle:** waste status; bag status (updateBagStatus); handover; UNKNOWN-21.
- **Database Tables:** BioMedicalWaste, BioMedicalWasteDetails, T_BIOMEDICALWASTE(+DETAILS) (18/10 refs), Wastebagmaster.
- **Stored Procedures:** none evidenced.
- **Integrations:** none external; barcode-based bag handling.
- **Solr/Redis:** none evidenced.
- **Scheduled Jobs:** none evidenced.
- **Reports:** showBioWasteReportSchema (reports).
- **Security:** BMW handler role; frontend gating.
- **Permissions:** bioMedicalWasteConfigration master keys.
- **Cross-module Dependencies:** M03 (IPD source).
- **Legacy Source Files:** `fjs-config_biomedicalwaste.json` (1 key), `services/BioMedicalWasteManagementServiceImpl.java`, `transactionals/BioMedicalWaste.java`, `masters/biomedicalwaste/wastebagmaster`, `js/bioMedicalWaste-module.js`.
- **Migration Complexity:** 1/5 — smallest module.
- **Risks:** none significant.
- **Open Questions:** UNKNOWN-21.

---

## M19 — Linen & Laundry

- **Target service:** 16. Linen & Laundry
- **Purpose:** Linen requisition & tracking for wards.
- **Business Context:** 1-key config; request lifecycle with details.
- **Actors:** Ward staff, Laundry staff.
- **Submodules:** Linen requests, status tracking, laundry processing.
- **Features:** showCurrentLinenStatusSchema; LinenRequest with details.
- **Screens:** showCurrentLinenStatusSchema.
- **Routes:** `linenlaundry:showLinenLaundryStatus` (1 route).
- **Backend APIs (service @Path):** `/linenLaundry` (14 endpoints).
- **Business Rules (BR-* IDs):** none formalized in 06.
- **Status Lifecycle:** linen status; UNKNOWN-22.
- **Database Tables:** LinenRequest(+Details), T_LINENREQUEST(+DETAILS) (20 refs), Linencategory, Linendetail.
- **Stored Procedures:** live-called from LinenLaundryServiceImpl (05 §6 — among 30 live proc callers).
- **Integrations:** none evidenced.
- **Solr/Redis:** none evidenced.
- **Scheduled Jobs:** none evidenced.
- **Reports:** linenLaundryReport.vm, showLinenLaundryReportSchema (reports).
- **Security:** frontend gating.
- **Permissions:** Configuration keys.
- **Cross-module Dependencies:** M03 (wards).
- **Legacy Source Files:** `fjs-config_linenlaundry.json` (1 key), `services/LinenLaundryServiceImpl.java`, `modules/linenlaundry/schemas|templates`, `transactionals/LinenRequest.java`, `js/linen-laundry-module.js`.
- **Migration Complexity:** 1/5 — tiny.
- **Risks:** none significant.
- **Open Questions:** UNKNOWN-22.

---

## M20 — Equipment & Maintenance

- **Target service:** 18. Equipment Management
- **Purpose:** Medical equipment lifecycle: acquisition → maintenance → complaint resolution → disposal.
- **Business Context:** 34-key config; AMC/warranty/insurance; preventive maintenance checklists; scrap sale; med-gas indents.
- **Actors:** Maintenance engineer, Biomedical engineer, Equipment in-charge, Vendor (AMC/service agent), User department.
- **Submodules:** Asset register; Complaints & resolution; Preventive maintenance & calibration; AMC/warranty & insurance; Equipment GRN; Scrap sale; Med-gas indent; Equipment usage.
- **Features:** addNewComplaintSchema; complaint resolution; preventive maintenance scheduling & checklists (electrical/functional); calibration lists; warranty & insurance docs; equipment opening balance; scrap sale; equipment usage; med-gas indent.
- **Screens:** getAssetDetailsSchema, addNewComplaintSchema, complaintDetailsSchema, getCalibrationMaintenanceListSchema, getPreventiveMaintenanceListSchema, showEquipUsageListSearch, getScrapSale, addEqpInsuranceSchema, showEquipMedGasIndentList, addComplaintResolutionSchema, addEqpDetailsList, addNewEqpPrevntSched, eqAmcWarrantyDetails, equipmentIssueList, equipmentPoList, equipOpeningBalListSchema, getAssetDtlsByItemSchema, getAssetVerificationListSchema, getComplaintResolutionListSearch, getEqpWarrantyDetailsSchema, getEquipmentGrnListSearch, getItemListSchema, getPreventiveCheckListSchema, getPreventiveMaintenanceList(+Search), getPreventiveMaintenanceScheduleListSearch, newGeneralComplaintSchema, showEquipMedGasIndentIssueList, ShowEquipmentGrnApprove, showIndentEquipmentDetOnApproved/Issued, updateComplaintSchema, updateGeneralComplaintSchema, uploadEquipmentDocument/InsuranceDocument/WarrantyDocumentSchema.
- **Routes:** complaintDetails, complaintResolutionList, preventiveMaintenance(New), preventiveMaintenanceSchedule, scrapSaleList, equipmentGrn, assetDetails, equipmentIssueItemList, assetVerification, preventiveCalibrationCheckList, calibrationMaintenance, assetPreventiveList, eqpApproveGrn, updateEquipmentGrn/:grnId, eqpPurchaseOrderList, equipmentUsageList, equipMedicalGasIndent, equipMedGasIndentIssue, equipOpeningBal, eqAmcWarrantyDetails.
- **Backend APIs (service @Path):** `/equipment` (50), `/preventivemaintenance`, `/preventivemaintenanceschedule`, `/complaintdetails`, `/complaintresolutiondetail`.
- **Business Rules (BR-* IDs):** none formalized in 06.
- **Status Lifecycle:** complaint status (complaintstatusmaster); equipworkingstatus; PM status (getPreventiveMaintenanceList); warranty status; UNKNOWN-23.
- **Database Tables:** Equipment, EquipmentDetailDocs, EquipmentWarranty, EquipmentTransfer, EquipmentUsageDetails, EqpComplaint(+Details), EqpComplaintResolution(+Details), EqpPMSchedule(+Details), EqpPreventiveMaintenance, EqiupOpeningBalance, T_EQUIPMENT, T_EQPCOMPLAINT(+RESOLUTION), T_EQPPM*, masters/equipment (amcagent, serviceagentmaster, insuranceagent, complaintcategorymaster, complaintstatusmaster, complainttypemaster, compliancemaster, compliancetypemaster, maintainancetype, equipchecklist, equipmentpreventivechecklist, functionalchecklistmaster, electricalChecklistmaster, equipworkingstatus).
- **Stored Procedures:** none evidenced.
- **Integrations:** MDM (item/asset sync).
- **Solr/Redis:** item collection (asset search); master caches.
- **Scheduled Jobs:** none evidenced.
- **Reports:** eqpGrnDetails(+_GST).vm, complaintDetailsView.vm, showEquipmentUsageReportSchema (reports).
- **Security:** Maintenance engineer roles; frontend gating.
- **Permissions:** Equipment keys (20).
- **Cross-module Dependencies:** M13 (item/asset master), M14 (equipment PO/GRN), M04 (charges for equipment services).
- **Legacy Source Files:** `fjs-config_equipments.json` (34 keys), `services/pas/EquipmentServiceImpl.java`, `services/pas/PreventiveMaintenanceServiceImpl.java`, `services/pas/PreventiveMaintenanceScheduleServiceImpl.java`, `services/pas/ComplaintDetailsServiceImpl.java`, `services/pas/ComplaintResolutionDetailServiceImpl.java`, `services/EquipmentServiceHelper.java`, `modules/equipments/schemas|templates`, `modules/masters/equipment/*`, `transactionals/Equipment.java`, `js/equipment-modules.js`.
- **Migration Complexity:** 2/5 — moderate CRUD; PM scheduling logic.
- **Risks:** AMC/warranty date tracking; calibration certification chain.
- **Open Questions:** none beyond UNKNOWN-23.

---
## M21 — Transport & Ambulance

- **Target service:** 17. Transport
- **Purpose:** Ambulance & vehicle fleet operations and patient transfer.
- **Business Context:** 11-key config; vehicle fleet, drivers, fuel, maintenance, trip charges.
- **Actors:** Transport dispatcher, Driver, Workshop, Requisitioning ward/doctor.
- **Submodules:** Patient transport requests; Doctor transport; Vehicle allocation & status (in/out); Fuel station; Vehicle maintenance; Transport charges; Trip details.
- **Features:** patientTransportRequestSchema; allocateVehicle; vehicle in/out; fuel station details; maintenance requests & approval; transport charges; trip details.
- **Screens:** patientTransportRequestSchema, doctorTransportRequestSchema, vehicleInSchema, vehicleOutSchema, vehicleStatusSchema, showTripRequestDetails, showFuelStationDetailSchema, addTransportChargesSchema, vehicleMaintenanceDetailsSchema.
- **Routes:** patientTransportRequest, doctorTransportRequest, tripManagement, vehicleOut/In, fuelStationDetails, vehicleMaintenance, showVehicleTripReport, showFuelConsumptionReport, vehicleStatus.
- **Backend APIs (service @Path):** `/transport`.
- **Business Rules (BR-* IDs):** none formalized in 06.
- **Status Lifecycle:** vehicle status (vehicleStatusSchema); request status (approveTransportRequest/cancelTransportRequest); maintenance status (approve/complete).
- **Database Tables:** TransportRequest(+Details), T_TRANSPORTREQUEST, VehicleMaintenance, T_VEHICLEMAINTENANCE, FuelStationDetails, M_AMBULANCEMASTER, M_DRIVERMASTER, masters/ambulance (ambulancemaster, ambulancetypemaster, accidenttype, driverMaster, workshopmaster, vehicleRequestTypeMaster).
- **Stored Procedures:** none evidenced.
- **Integrations:** Jeevanvahini (108 ambulance push/fetch — CommonUtilServiceImpl:224-227, PatientServiceImpl:277-281).
- **Solr/Redis:** master caches only.
- **Scheduled Jobs:** none evidenced.
- **Reports:** transportReqReceipt.vm, showVehicleTripReportSchema, showFuelConsumptionReportSchema (reports), ambulanceRegisterReportSchema (reports).
- **Security:** dispatcher roles; frontend gating.
- **Permissions:** ambulanceConfigration master keys.
- **Cross-module Dependencies:** M03 (IPD patient transport), M04 (transport charges billing).
- **Legacy Source Files:** `fjs-config_transport.json` (11 keys), `services/pas/TransportServiceImpl.java`, `modules/transport/schemas|templates`, `masters/ambulance/*`, `transactionals/TransportRequest.java`, `js/transport-module.js`.
- **Migration Complexity:** 2/5 — small; Jeevanvahini coupling.
- **Risks:** Jeevanvahini sync semantics.
- **Open Questions:** none significant.

---

## M22 — Grievance

- **Target service:** 20. Grievance & Feedback
- **Purpose:** Patient/staff grievance capture and resolution.
- **Business Context:** 3-key config; severity templates; escalation history; feedback.
- **Actors:** Grievance officer, Patient, Assignee.
- **Submodules:** Grievance tickets, escalation/history, resolution & feedback.
- **Features:** raiseGrievanceTicketSchema; grievance history; severity templates; resolution assignee.
- **Screens:** raiseGrievanceTicketSchema, grievanceSchema, grievanceHistorySchema.
- **Routes:** `grievance:grievance`, `grievanceHistory` (2 routes).
- **Backend APIs (service @Path):** grievance endpoints via custom service — UNKNOWN-25; screens in fjs-config_grievance; 10 §2.1 row 20 assigns ComplaintDetailsServiceImpl/QuetionnaireServiceImpl.
- **Business Rules (BR-* IDs):** none formalized in 06.
- **Status Lifecycle:** grievance status via history (audit event `grievance → T_GRIEVANCE_TICKET_HISTORY`); UNKNOWN-24.
- **Database Tables:** PatientGrievanceHistory, PatientGrievanceComments, PatientGrievanceFeedback, GrievanceDocuments, ResolutionAsignee, T_PATIENTGRIEVANCE*, masters/grievance (grievancecategorymaster, grievancesubcategorymaster, grievanceseveritymaster, grievanceseveritytemplatedetails).
- **Stored Procedures:** none evidenced.
- **Integrations:** none external; feedback forms.
- **Solr/Redis:** none evidenced.
- **Scheduled Jobs:** none evidenced.
- **Reports:** none evidenced.
- **Security:** Grievance officer role; audit trail.
- **Permissions:** Configuration keys.
- **Cross-module Dependencies:** M01 (patient), M35 (assignee via user module).
- **Legacy Source Files:** `fjs-config_grievance.json` (3 keys), `modules/grievance/schemas|templates`, `masters/grievance/*`, `transactionals/PatientGrievanceHistory.java`, `js/grievance-module.js`.
- **Migration Complexity:** 2/5 — small; endpoint mapping gap (UNKNOWN-25).
- **Risks:** Screens lack obvious service @Path (02 open Q11).
- **Open Questions:** UNKNOWN-24/25; API mapping via frontend JS.

---

## M23 — RMRS (Financial/Fund Management)

- **Target service:** 24. RMRS & PMRD (Treasury)
- **Purpose:** Fund/authority management & accounting — likely for scheme/authority payments (e.g., government scheme funds).
- **Business Context:** 9-key config; ~35 templates; authorities (contracts, EMI), heads, collections, journal, suspense, bank statements, PO requests.
- **Actors:** RMRS authority, Accounts officer, Cashier, Collector.
- **Submodules:** Authorities (contracts, EMI); Heads (account heads); Payment collections; Issued payments; Journal entries; Suspense accounts; Bank statements; PO requests; Dashboards.
- **Features:** addAthorities; addCollection; IssueCollection; approveOrRelease; journal entries; bank statement; suspense account; plan & account-wise reports; authority EMI details.
- **Screens:** AddCollection, PaymentCollection, IssueCollection, RMRSDashboard, suspenseAccountDetailsSchema, showRMRSPurchaseOrderRequestListSchema, CollectionIssuedList, DashboardPaymentCollectionSchema, rmrsDashboardAuthorityList.
- **Routes:** showRMRSPORequest, rmrsAccountWiseReport, summaryAccountWiseReport, reportByDocumantNumber, planAccountWiseReport, suspenseAccountDetail, addcollection (generic), showPaymentCollection, issuePayment, showRMRSDashboard (36 endpoints under `/RMRS`).
- **Backend APIs (service @Path):** `/RMRS`.
- **Business Rules (BR-* IDs):** none formalized in 06.
- **Status Lifecycle:** transaction status list (getAllTransactionStatusList); collection status; issue status; UNKNOWN-26.
- **Database Tables:** CollectionHeader, CollectionDetail, RmrsCollection, RMRSDashboardTransaction, SuspenseAccountDetails, TUserHospitalAuthority, T_RMRSCOLLECTION, T_AUTHORITYMASTER, T_HEADSMASTER, T_LEDGERMASTER, T_ACCOUNTING*/M_ACCOUNTING* (14), masters/rmrs (authoritymaster, headsmaster, attendantpassserviceratemaster, rmrsreasonmaster), masters/hospital (accountheadmaster, bankmaster, bankaccountmaster, bankbranchmaster).
- **Stored Procedures:** accounting procs (M_ACCLEDGER family per 05 §3.1); none module-specific evidenced.
- **Integrations:** payment transfer (M33), M14 (PO requests).
- **Solr/Redis:** master caches (authority/heads).
- **Scheduled Jobs:** none evidenced.
- **Reports:** rmrsIssuedPaymentReceipt.vm, authorityBillDetailsSummeryReceipt.vm, authorityBillPaidReceipt.vm, printBankStatement.vm, printJournalConfirmReceipt.vm, planAndAccountWiseReport.vm, summaryAccountWiseReport.vm, printReportByDocumentNumber.vm, ExpenditureReport.vm.
- **Security:** RMRS authority roles; frontend gating.
- **Permissions:** rmrsConfiguration master keys.
- **Cross-module Dependencies:** M04 (billing payment linkage), M14 (PO requests), M33 (payment transfer), Masters (hospital accountheadmaster, bankmaster).
- **Legacy Source Files:** `fjs-config_rmrs.json` (9 keys), `services/pas/RMRSServiceImpl.java`, `modules/rmrs/schemas|templates` (~35), `masters/rmrs/*`, `transactionals/CollectionHeader.java`, `js/rmrs-module.js`.
- **Migration Complexity:** 3/5 — accounting semantics + authority EMI; moderate.
- **Risks:** Treasury double-entry correctness; attendant-pass service rates coupling.
- **Open Questions:** Scheme-funds context confirmation; ledger master usage.

---

## M24 — PMRD (Patient Medical Record Department)

- **Target service:** 24. RMRS & PMRD (target merges; ownership unconfirmed)
- **Purpose:** Medical records management & scheme documentation.
- **Business Context:** 2-key config; IEN encounter records; JSY scheme forms (J1/J2/J3); document uploads.
- **Actors:** MRD clerk, Records officer.
- **Submodules:** Encounter record (IEN); Medical record documents; Form J1/J2/J3 (JSY scheme forms); Uploads.
- **Features:** getAllPatientEncounter; getPatientDocumentsForPMRD; getJsyFormDetails; uploads.
- **Screens:** pmrd schemas (2); templates formj1/2/3, hform, patientEncounterRecord, patientMedicalRecordWrap, uploadPatientDocumentsPmrd.
- **Routes:** `pmrd:patientMedicalRecord` (1 route).
- **Backend APIs (service @Path):** `/pmrd`.
- **Business Rules (BR-* IDs):** none formalized in 06.
- **Status Lifecycle:** UNKNOWN-27 (uses encounters + documents).
- **Database Tables:** PMRD uses encounters + documents — UNKNOWN-27 specific tables.
- **Stored Procedures:** none evidenced.
- **Integrations:** none external.
- **Solr/Redis:** encounter collection (record lookup).
- **Scheduled Jobs:** none evidenced.
- **Reports:** hform.vm (templates root: patientEncounterRecord.html etc.).
- **Security:** MRD clerk role; frontend gating.
- **Permissions:** Configuration keys.
- **Cross-module Dependencies:** M01 (encounters), M08.
- **Legacy Source Files:** `fjs-config_pmrd.json` (2 keys), `services/pas/PmrdServiceImpl.java`, `modules/pmrd/templates`, `js/pmrd-module.js`.
- **Migration Complexity:** 1/5 — thin module.
- **Risks:** none significant.
- **Open Questions:** UNKNOWN-27 tables; JSY form data model.

---

## M25 — Reports & BI

- **Target service:** 26. Reporting & Analytics
- **Purpose:** Reporting & analytics.
- **Business Context:** 46-key config; report dispatcher (Nashorn-driven, IntegrationOperationsFilter:469-571); BI dashboard (biReport); statistical aggregates.
- **Actors:** Hospital management, Department heads, Statistician.
- **Submodules:** Standard reports (registration, admissions, discharges, collection, OPD statistics, IPD statistics, death, ambulance, blood bank, bio-waste, CSSD, linen, lab); BI dashboard.
- **Features:** 46 report schemas incl. servicewise/userwise collection, userwise summary, IPD/OPD statistics, blood bank registers; report dispatcher `/reports/<reportName>`.
- **Screens:** all report schemas under reports/ (ageWiseRegistrationReportSchema, ambulanceRegisterReportSchema, bloodBagIssueReport, bloodDonorRegistrationReport, cancelledAdmissionReportSchema, categoryWisePatientReportSchema, dailyDischargeReportSchema, departmentwiseOpdPatientStatistics, getServicewiseCollectionReportSchema, getUserwiseCollectionReportSchema, ipdStatisticalReportSchema, labCenterTestWiseReportSchema, patientDeathReportSchema, showAdmissionReportSchema, showAppointmentReportSchema, showBioWasteReportSchema, showBloodBagBalanceSheetReport, showBloodBankBalanceSheetReport, showCssdReportSchema, showEquipmentUsageReportSchema, showFuelConsumptionReportSchema, showLabReportSchema, showLinenLaundryReportSchema, showListOfRecieptforCampReport, showVehicleTripReportSchema, +17 blood bank reports).
- **Routes:** `biReport:reports(/:rid)`, `reportdm(/:rid)`; ~40 `reports:showXxxReport` routes.
- **Backend APIs (service @Path):** `/reports/*` (47, dynamic dispatcher), `/masterConfigService` (getAllDataForReport on /helper), report endpoints in `js/report-module.js`.
- **Business Rules (BR-* IDs):** none formalized in 06.
- **Status Lifecycle:** n/a (read-only).
- **Database Tables:** ServiceWiseCollection, UserWiseCollection, s_servicewisecollection, s_userwisecollection, T_REPORTMASTER, stats/archive tables (addStats_Archived).
- **Stored Procedures:** report-related procs in generated-procs (usage UNKNOWN — many reports proc-driven).
- **Integrations:** NMC (statistics push nightly — updateNMCData), WhatsApp report queue (triggerWhatsappReportsInQueue).
- **Solr/Redis:** dashboards (cache); Solr collections as data source for some searches.
- **Scheduled Jobs:** updateNMCData (nightly), updateIhmsSummaryCounts, addStats_Archived, triggerWhatsappReportsInQueue (15-min window).
- **Reports:** generic_reports.vm, servicewiseCollectionReport.vm, userwiseCollectionReport.vm, showAgeWiseRegistrationReport.vm, showLabReport.vm.
- **Security:** BI iframe with token refresh; statistician role.
- **Permissions:** Report keys (7) + BIReport keys (263).
- **Cross-module Dependencies:** All modules (data source).
- **Legacy Source Files:** `fjs-config_reports.json` (46 keys), `fjs-config_biReport.json`, `js/report-module.js`, `modules/reports/schemas|templates`, statistical models, `IntegrationOperationsFilter.java` (report dispatcher), `reports.js` (Nashorn).
- **Migration Complexity:** 2/5 — read-model extraction; dispatcher rebuild.
- **Risks:** Nashorn dispatcher (removed in JDK 15+); BI tool identity unknown (UNKNOWN-03/09).
- **Open Questions:** BIReport semantics ("DM" = Data Management?); 222 unmatched URLs include report variants.

---

## M26 — Worklist & Task Management

- **Target service:** 12. Nursing & Inpatient Care (worklist sub-domain; ownership boundary per 02 open Q5)
- **Purpose:** Task-driven nursing work.
- **Business Context:** 1-key config + ~30 templates; nurse task lists task-wise/patient-wise; goals; roleNurseWorkList views.
- **Actors:** Staff nurse, Nurse in-charge.
- **Submodules:** Nurse task lists (task-wise/patient-wise), goals, interventions, meds, labs, radiology, consults/procedures, ward lists.
- **Features:** nurseTaskList; completeNursingOrderTask; unmetGoals; roleNurseWorkList(Home/AssignView/Goals/Intervention/Meds/Orders/PatientWise/TaskWise).
- **Screens:** nurseTaskList.json.
- **Routes:** `nursing:roleNurse*` worklist routes (nursing module).
- **Backend APIs (service @Path):** `/nursingList`, `/clinical`.
- **Business Rules (BR-* IDs):** none formalized in 06.
- **Status Lifecycle:** task status; goal status (unmetGoals).
- **Database Tables:** NurseTaskDetails, NurseTaskFrequencyDetails, T_NURSETASKDETAILS.
- **Stored Procedures:** none evidenced.
- **Integrations:** CWF/SCWF workflows (task generation).
- **Solr/Redis:** encounter collection (worklists); master caches.
- **Scheduled Jobs:** none evidenced.
- **Reports:** taskListPatientwise (template).
- **Security:** nurse role; frontend gating.
- **Permissions:** Nursing keys.
- **Cross-module Dependencies:** M09, M07, M15, M16 (results).
- **Legacy Source Files:** `fjs-config_worklist.json`, `modules/worklist/schemas|templates` (~30), `transactionals/NurseTaskDetails.java`, `js/nursing-module.js`, `docit-roles.js` (roleNurse worklists).
- **Migration Complexity:** 2/5 — folded into M09; boundary decision needed.
- **Risks:** Ownership ambiguity with M09 (UNKNOWN-04).
- **Open Questions:** Independent service vs M09 sub-module.

---

## M27 — Queue Management (OPD & Nursing Desk)

- **Target service:** 29. Queue & Kiosk
- **Purpose:** Patient flow control in OPD (tokens, cabin, doctor assignment).
- **Business Context:** 11-key config; appointment/cabin/doctor queues; token management; nursing desk; questionnaires; Redis-backed QMS.
- **Actors:** Queue manager, Nursing desk, Doctor, Reception.
- **Submodules:** Appointment queue, Cabin queue, Doctor batch slots, Token management, Nursing desk, User queues, Questionnaires.
- **Features:** appointmentQueueSchema; manageTokenSchema; doctorBatchSlotSchema; nursingDeskForQueue; addpatientPhoto; questionnaireSchema (pre-consult questionnaire).
- **Screens:** appointmentQueueSchema, cabinQueueListSchema, doctorBatchSlotSchema, manageTokenSchema, nursingDeskForQueue, questionnaireSchema, userQueueSchema, addpatientPhoto, appointment_list_new_schema, callGetPatientListSchema.
- **Routes:** `queue:appointmentQueue`, `cabinQueue`, `radiologyQueue`, `pathologyQueue`, `doctorBatch`, `callPatientList`, `patientQueue`, `userQueue`, `doctorQueue`, `nurseDesk`, `manageToken`; standalone `queue/` sub-app (counterQueue, departmentQueue, roomQueue).
- **Backend APIs (service @Path):** `/queueManagment`, `/doctor` (queue endpoints), `/questionnaireService` (QuetionnaireServiceImpl).
- **Business Rules (BR-* IDs):** BR-OPD-007 adjacency (VisitStatus=11 is the queue/kiosk "billed" signal, JS getLastVisitStatus).
- **Status Lifecycle:** queue status (callGetPatientListSchema, assignNextPatientToDoctor); token states (fetchLatestToken, fetchAbsentToken).
- **Database Tables:** PatientQueue, RegistrationQueue, QuestionAnswerDto, T_PATIENTTOKEN, masters/hospital (roomMasterForQueue, opdMachineMaster, opdStationMaster), T_OPDSTATIONMASTER, T_TABLETMAPPING.
- **Stored Procedures:** none evidenced.
- **Integrations:** kiosk (M31), telemedicine queue.
- **Solr/Redis:** **Redis-heavy:** QMS room token queues (`pushToQ/removeFromQ`, `<qName>_S` counters, `*_time` sums, QMSUtil:28-126, QueueManagmentServiceImpl:317-1346).
- **Scheduled Jobs:** none evidenced.
- **Reports:** token_sheet.vm (OP token).
- **Security:** Queue manager/nursing desk roles; frontend gating.
- **Permissions:** QueueManagement keys (16).
- **Cross-module Dependencies:** M02, M09 (nurse desk), M01.
- **Legacy Source Files:** `fjs-config_queue.json` (11 keys), `services/custom/QueueManagmentServiceImpl.java`, `util/QMSUtil.java`, `modules/queue/schemas|templates`, `models/trx/RegistrationQueue.java`, `transactionals/PatientQueue.java`, `js/queue-module.js`, `queue/` sub-app.
- **Migration Complexity:** 3/5 — Redis queue state machine; token flows; standalone sub-app.
- **Risks:** In-memory/Redis queue state loss; `disableBackForwardButton` navigation semantics.
- **Open Questions:** Queue sub-app deployment scope (03 open Q4).

---

## M28 — Mortuary & Death

- **Target service:** 3. ADT & Bed (death flows; no dedicated service in 10)
- **Purpose:** Death documentation, mortuary custody, certification.
- **Business Context:** Commons schemas (mortuary*); death forms; CDR (child death review) forms; coroner/MLC interface.
- **Actors:** Mortuary attendant, Doctor, RMO, Records.
- **Submodules:** Mortuary box management, death certificate, post-mortem, child death review (CDR 4A/4B).
- **Features:** getMortuaryPatients; mortuary clearance; death certificate print; CDR forms (cdr_form_4A/4B.vm); death details update (updatePatientDeathDetails).
- **Screens:** mortuarySchema, mortuaryClearanceSchema, mortuaryBoxManagement, mortuaryDischargedPatientSchema.
- **Routes:** `commons:mortuaryDetailsList/BoxManagement/Release/DischargeList`, `nursing:showMortuary`.
- **Backend APIs (service @Path):** `/patient` (getMortuaryPatients, getPatientDetailsForDeathForm, updatePatientDeathDetails); `/nursingList` (saveMarkAsExpired, addChildDeathReview).
- **Business Rules (BR-* IDs):** BR-DIS-002 (death handling on discharge — T_Registration_Details.isdeath=1, Solr death=true); no standalone BR-*.
- **Status Lifecycle:** mortuary status (mortuaryClearanceSchema); death certificate status; UNKNOWN-28.
- **Database Tables:** ChildDeathReview, PatientDischargeData (death), T_REGISTRATION_DETAILS.isdeath, death-related masters UNKNOWN-29.
- **Stored Procedures:** CIMS_ADDBEDDISCHARGE (death branch).
- **Integrations:** none external; coroner flow UNKNOWN.
- **Solr/Redis:** patient update on death (`death=true` partial); current_pat_ mirror.
- **Scheduled Jobs:** none evidenced.
- **Reports:** death_form.vm, death_Certificate.vm, PatientDeathForm.vm, Post-MortomReport.vm, cdr_form_4A.vm, cdr_form_4B.vm, DeathReasonForm.vm, death_Certificate tiuxml.
- **Security:** RMO/doctor certification; frontend gating.
- **Permissions:** commons/Configuration keys.
- **Cross-module Dependencies:** M03 (deceased patient), M01.
- **Legacy Source Files:** commons schemas (mortuarySchema etc.), `services/pas/PatientServiceImpl.java` (mortuary/death endpoints), `templates/*.vm` (death family), `transactionals/ChildDeathReview.java`, tiuxml (Post_Mortem, Death_Certificate, DEATHNOTE).
- **Migration Complexity:** 2/5 — small surface; death-flag ripple across Solr/registration.
- **Risks:** isdeath propagation across tables; CDR form compliance.
- **Open Questions:** UNKNOWN-28/29; coroner/MLC external flow.

---

## M29 — Chemotherapy

- **Target service:** none dedicated in 10 (candidate: 4. Ordering/Clinical + 9. Pharmacy) — UNKNOWN
- **Purpose:** Chemotherapy treatment planning & tracking (oncology).
- **Business Context:** 3-key config; chemo planning/cycles; coversheet; chemo tiuxml mapping.
- **Actors:** Oncologist, Chemo nurse, Pharmacist (IV compounding).
- **Submodules:** Chemo patient list, chemo planning, patient coversheet.
- **Features:** showChemoPatientListSchema; showChemoPlanningSchema; chemo coversheet.
- **Screens:** showChemoPatientListSchema, showChemoPlanningSchema, showChemotherapyPatientCoversheetSchema.
- **Routes:** `chemoPlanningList`, `chemoPatientCoversheet`, `chemoPlanning`, `chemoNote`, `chemoTherapyNote`, `patNote`, `chemoNewRegimen`, `chemoPlanCycleFlow`, `chemoPlanningFlow` (chemotherapy-module.js 733 KB).
- **Backend APIs (service @Path):** chemotherapy endpoints — UNKNOWN-31.
- **Business Rules (BR-* IDs):** none formalized in 06.
- **Status Lifecycle:** cycle/regimen statuses UNKNOWN-30 adjacency.
- **Database Tables:** chemo uses careplan/medication tables — UNKNOWN-30.
- **Stored Procedures:** none evidenced.
- **Integrations:** chemo_tiuxml_mapping.json (Chemo_Performance_Status_Assessment, Chemo_SURGONCOHIST/EXAM).
- **Solr/Redis:** patient search (coversheet).
- **Scheduled Jobs:** none evidenced.
- **Reports:** none evidenced.
- **Security:** oncologist role; frontend gating.
- **Permissions:** Configuration keys.
- **Cross-module Dependencies:** M01, M03, M07 (IV drugs), M08.
- **Legacy Source Files:** `fjs-config_chemotherapy.json` (3 keys), `modules/chemotherapy/schemas|templates`, `js/chemotherapy-module.js`, `chemo_tiuxml_mapping.json`.
- **Migration Complexity:** 1/5 — thin screens; ownership unclear.
- **Risks:** Regimen/cycle logic location unknown (JS-heavy).
- **Open Questions:** UNKNOWN-30/31; service ownership.

---

## M30 — BCMA (Bar-Coded Medication Administration)

- **Target service:** none dedicated in 10 (candidate: 9. Pharmacy + 12. Nursing) — UNKNOWN
- **Purpose:** Bedside barcode verification for medication administration.
- **Business Context:** 11-key config; scan patient/med barcodes; 5-rights verification; exception handling.
- **Actors:** Administering nurse, Pharmacy (missing doses).
- **Submodules:** Medication log, IVP tab, Unit dose tab, Unable-to-scan handling, Missing dose requests, Dermal site, Cover sheet.
- **Features:** showMainScreenSchema; PatientMedicationLogSchema; missingDoseRequestSchema; iVPUnableToScanSchema; showMedLogHeldSchema.
- **Screens:** ShowMainScreenSchema, ShowUDTab, ShowIVPTab, ShowBcmaItemStock, PatientMedicationLogSchema, ShowUnableToScanSchema, MissingDoseRequestSchema.
- **Routes:** `bcma:unitDose`, `scanWristband/:patientMrn`.
- **Backend APIs (service @Path):** bcma endpoints — UNKNOWN-33; schemas in fjs-config_bcma.
- **Business Rules (BR-* IDs):** none formalized in 06.
- **Status Lifecycle:** med log status (held); missing dose status; UNKNOWN-32.
- **Database Tables:** IHR_MEDICATION_TMP, T_INFUSIONMIXING(+DETAILS), T_PRESCRIPTIONDISPENSEDETAILS.
- **Stored Procedures:** none evidenced.
- **Integrations:** pharmacy (dispensed meds), wristband barcodes (html5-qrcode).
- **Solr/Redis:** item stock lookup (ShowBcmaItemStock); master caches.
- **Scheduled Jobs:** none evidenced.
- **Reports:** labels via pharmacy printMedLabel.
- **Security:** administering nurse identity; scan-first enforcement.
- **Permissions:** Pharmacy/Nursing keys.
- **Cross-module Dependencies:** M07 (meds), M09 (nursing), M13 (stock).
- **Legacy Source Files:** `fjs-config_bcma.json` (11 keys), `modules/bcma/schemas|templates`, `transactionals/IHR_MEDICATION_TMP.java`, `js/bcma-module.js`.
- **Migration Complexity:** 2/5 — small surface; hardware/scan flows.
- **Risks:** 5-rights verification enforcement points.
- **Open Questions:** UNKNOWN-32/33; service ownership.

---
## M31 — Kiosk (Self-Service)

- **Target service:** 29. Queue & Kiosk
- **Purpose:** Patient self-service at kiosks (registration/appointments).
- **Business Context:** 4-key config; self-registration, appointment search, account search, queue; also lab-result upload (COVID kiosk).
- **Actors:** Patient (self-service).
- **Submodules:** Self-registration, appointment search, account search, queue.
- **Features:** selfAppointmentSchema; showSelfRegPatientSchema; accountSearchSchema; kiosk queue; kiosk lab result upload (saveKioskLabResult, uploadKioskLabResult).
- **Screens:** selfAppointmentSchema, showSelfRegPatientSchema, accountSearchSchema, appointmentSearchSchema.
- **Routes:** `kiosk:selfRegPatient`, `kiosk:selfAppointment/...`, `kiosk:searchAppointment/...`, `kiosk:accountSearchSchema`.
- **Backend APIs (service @Path):** `/patient` (kioskQueue, saveKioskLabResult, uploadKioskLabResult), `/markandaddvisit` (checkin).
- **Business Rules (BR-* IDs):** none formalized in 06.
- **Status Lifecycle:** queue/token state via M27.
- **Database Tables:** RegistrationQueue, PatientQueue, T_PATIENTTOKEN (shared with M27).
- **Stored Procedures:** none evidenced.
- **Integrations:** e-Mitra (online appointment booking path).
- **Solr/Redis:** patient search (searchPatientInSolr); QMS queue state.
- **Scheduled Jobs:** none evidenced.
- **Reports:** none evidenced.
- **Security:** **`secretTokenForOnlineAppointment` static bypass (user id=1) is shipped to the browser in fjs-config.json** (08 §9 risk 3) — kiosk/e-Mitra surface.
- **Permissions:** queue/kiosk keys.
- **Cross-module Dependencies:** M01, M02, M27.
- **Legacy Source Files:** `fjs-config_kiosk.json` (4 keys), `modules/kiosk/schemas|templates`, `services/pas/PatientServiceImpl.java` (kiosk endpoints), `js/queue-module.js` (kiosk).
- **Migration Complexity:** 2/5 — thin; auth bypass must be replaced.
- **Risks:** Static token bypass; kiosk lab-result scope (UNKNOWN-10).
- **Open Questions:** Kiosk lab upload use cases (COVID testing?); device/machine mapping.

---

## M32 — RCM (Revenue Cycle Management / Scheme Billing)

- **Target service:** 23. RCM & Claims
- **Purpose:** Government scheme (RGHS/Chiranjeevi) eligibility & claim documentation.
- **Business Context:** 1-key config; RGHS eligibility; DM/MOIC approvals; discharge verification; SIMS; NHCX claims.
- **Actors:** RCM officer, DM/MOIC (approvals), Hospital admin.
- **Submodules:** RGHS eligibility check, DM approval upload, MOIC upload, Patient photo upload, Discharge verification, Chiranjeevi app.
- **Features:** rghsEligibilityCheckSchema; dmApprovalUploadSchema; moicUploadSchema; patientPhotoUploadSchema; dischargeVerificationSchema.
- **Screens:** rghsEligibilityCheckSchema, dischargeVerificationSchema, dmApprovalUploadSchema, moicUploadSchema; rcm module (chiranjeeviapp, patientVerificationDetails, rghsEligibilityDetail).
- **Routes:** `rcm:chiranjeeviapp` (1 route).
- **Backend APIs (service @Path):** `/rcm`, `/patient` (getRghsTransactionsforIHMS, getRghsDocumentByTransactionId, getSimsRghsforIHMS), `/nhcx` (eligibility/preauth/claim).
- **Business Rules (BR-* IDs):** none formalized in 06; NHCX request statuses 0=ACCEPTED..3=FAILED; request types 0=INSURANCEPLAN..4=COMMUNICATION.
- **Status Lifecycle:** eligibility status; TID lifecycle (T_ADMISSION_TID, T_REFRENCETID); claim status.
- **Database Tables:** models/rcm (2 — RGHS metadata, UNKNOWN-34), T_ELIGIBILITYCHECK, T_ADMISSION_TID, T_ADMISSIONTIDDETAILS, T_NHCX_REQUEST(+DETAILS), T_REFRENCETID, BENEFICIARY_ENROLMENT_IHMS, masters (eraktkoshcomponentmapping adjacency).
- **Stored Procedures:** rghsTID-related (generated-procs).
- **Integrations:** RGHS/SIMS (Rajasthan govt — rghsDataScheduler 23:30 nightly, simsRGHS AuthKey), Chiranjeevi, NHCX (insurance claims), DM/MOIC approval uploads.
- **Solr/Redis:** patient search (eligibility lookup); ABDM/HCX correlation keys.
- **Scheduled Jobs:** rghsDataScheduler (ApplicationJobs:1063-1092, 23:30).
- **Reports:** rghsTID.vm, documentUploadRghsReq.vm, opdMetadataForRghs.vm, rghsSimsData.vm, chiranjeeviEmrTID.vm.
- **Security:** RCM/DM/MOIC roles; upload approvals; NHCX JWE keys.
- **Permissions:** RCM-specific keys UNKNOWN (frontend-gated).
- **Cross-module Dependencies:** M01, M03 (discharge), M04, M38.
- **Legacy Source Files:** `fjs-config_rcm.json` (1 key), `services/RcmServiceImpl.java`, `modules/rcm/schemas|templates`, `models/rcm/*`, `js/rcm-module.js`, `IntegrationHelper.java` (addSimsRGHSData).
- **Migration Complexity:** 3/5 — scheme eligibility + SIMS sync + NHCX claims.
- **Risks:** Hard-coded SIMS auth keys; TID reconciliation; claim JWE encryption.
- **Open Questions:** UNKNOWN-34; DM/MOIC approval workflow details.

---

## M33 — Payment Transfer

- **Target service:** 6. Payments & Refunds
- **Purpose:** Digital payment transfers & incentive claims (e.g., ASHA incentives).
- **Business Context:** 3-key config; transfer requests, incentive claims, OTP verification, bank accounts.
- **Actors:** Finance, ASHA/incentive claimant, Bank.
- **Submodules:** Payment transfer requests, incentive claims, OTP verification, bank accounts.
- **Features:** paymentTransferListSchema; incentiveClaimInfoSchema; generateOTPSchema.
- **Screens:** paymentTransferListSchema, incentiveClaimInfoSchema, generateOTPSchema.
- **Routes:** paymentTransfer (2 routes).
- **Backend APIs (service @Path):** `/paymentTransfer` (payment/PaymentTransferServiceImpl).
- **Business Rules (BR-* IDs):** none formalized in 06.
- **Status Lifecycle:** payment transfer status (paymentTransStatus template).
- **Database Tables:** T_PAYMENTTRANSFER, T_PAYMENTTRANSFERREQUEST(+DETAIL), masters/hospital (bankmaster, bankbranchmaster, bankaccountmaster).
- **Stored Procedures:** none evidenced.
- **Integrations:** M36 (EJS incentives), RMRS.
- **Solr/Redis:** OTP state (session cache); none else.
- **Scheduled Jobs:** ejs.asha.incentive.schedule (23:00 — incentive computation feeds transfers).
- **Reports:** none evidenced.
- **Security:** OTP verification (generateOTPSchema); bank account validation.
- **Permissions:** finance keys.
- **Cross-module Dependencies:** M04, M36 (EJS incentives), M23 (RMRS).
- **Legacy Source Files:** `fjs-config_paymentTransfer.json` (3 keys), `services/payment/PaymentTransferServiceImpl.java`, `modules/paymentTransfer/schemas|templates`, `transactionals/T_PAYMENTTRANSFER.java`, `js/payment-transfer-module.js`.
- **Migration Complexity:** 2/5 — small; incentive/OTP flows.
- **Risks:** OTP/session reliance on cache; bank account data.
- **Open Questions:** none significant.

---

## M34 — File Management & Document Upload

- **Target service:** 26. Reporting (file/document services per 10 §2.1 row 26 legacy sources) — cross-cutting
- **Purpose:** Document capture & retrieval across modules.
- **Business Context:** 1-key config; upload/download for patient, lab, grievance, equipment; MinIO (disabled) → filesystem fallback.
- **Actors:** All users (per module).
- **Submodules:** Document upload (patient, lab, grievance, equipment), amendment uploads.
- **Features:** uploadDocumentSchema; uploadFileSchema; amendmentFileUpload; scan app integration.
- **Screens:** uploadDocumentSchema, uploadFileSchema.
- **Routes:** `commons:pDocumentsManagement`, `filemanagement:dms` (1 route).
- **Backend APIs (service @Path):** `/fileService` (FileManagementServiceImpl — incl. duplicate `uploadHospitalLocation`), `/uploadFile`, `/upload`.
- **Business Rules (BR-* IDs):** none formalized in 06.
- **Status Lifecycle:** n/a.
- **Database Tables:** Documents, PatientDocuments, PathoPatientDocs, GrievanceDocuments, EquipmentDetailDocs, PathOrderBookingDetailDocuments.
- **Stored Procedures:** none evidenced.
- **Integrations:** MinIO (S3, `minio.enabled=false` → filesystem), scan/print app (scan.print.api.url), TIUXML file paths.
- **Solr/Redis:** none evidenced.
- **Scheduled Jobs:** none evidenced.
- **Reports:** none evidenced.
- **Security:** per-module document permissions; amendment audit.
- **Permissions:** Configuration/Upload keys.
- **Cross-module Dependencies:** All modules.
- **Legacy Source Files:** `fjs-config_fileManagement.json` (1 key), `services/FileManagementServiceImpl.java`, `services/custom/UploadFileServiceImpl.java`, `services/custom/UploadHelperServiceImpl.java`, `util/FileStoreUtil.java`, `js/file-management-module.js`.
- **Migration Complexity:** 2/5 — storage abstraction (MinIO vs FS) must be pinned.
- **Risks:** Storage backend ambiguity; duplicate endpoint variants.
- **Open Questions:** ext.js injection mechanism (03 open Q1).

---

## M35 — User, Role & Notification Management

- **Target service:** 22. Security & IAM
- **Purpose:** Identity, access control, communication.
- **Business Context:** Users/roles/permissions DB-driven; notifications (email/SMS/WhatsApp); menu/module config; **RBAC enforced frontend-only** (backend check commented out).
- **Actors:** System admin, users.
- **Submodules:** Users & roles, permissions, notifications (email/SMS), notification templates, menu/module config.
- **Features:** addUserSchema; user role master; email templates; notification templates; user-store/ward/unit mapping; shift details; user transfer; supplier users.
- **Screens:** addUserSchema, getUserDetailsSchema, tuserrolemaster, notificationTemplateSchema, updateUserSchema, staffmaster, doctormaster, addPatientUserPassSchema, OutreachUserCreationSchema.
- **Routes:** `commons:user/:userId/:unitId`, `commons:selectStore`, `commons:selectDefaultStore`, `masters:addUser`, `masters:showUserList`, `commons:notificationTemplates`, `commons:shiftDetails/roster/ipdRoster`, `commons:forgotPassword`, `commons:resetPassword/:token/:unitId/:id`, `commons:resetLoginPassword/:unitId/:userId`, `commons:selectDefaultRole`.
- **Backend APIs (service @Path):** `/usermanagement` (incl. loginsso, setDefaultRoute), `/addUserService` (62 endpoints: saveUser, getPermissions, transferUser, syncUserApiSecret…), `/sendEmail` (SendNotificationServiceImpl), `/actionNotif`, `/auth/*` (12).
- **Business Rules (BR-* IDs):** none formalized in 06; lockout threshold (ACCOUNTLOCKTHRESHOLD), single-session enforcement, session timeout 30 min.
- **Status Lifecycle:** user status '1' active; locked flag (BEL002); token TTL sliding.
- **Database Tables:** TUserdetails, TUserrolemaster, TUserStoreDetails, TUserWardDetails, TUserBuildingDetails, TUserunitdetails, TUserHospitalAuthority, UserDistrictDetails, UserStateDetails, UserSpecialityDetails, ModulePermissionMaster, Menumaster, ModuleNameMaster, EmailTemplate(+Details), NotificationTemplate, T_USERDETAILS (25 refs), t_userroles, t_role_enabledpermission, M_STAFFMASTER, M_DOCTORMASTER.
- **Stored Procedures:** patientLoginNameExists (patient portal login).
- **Integrations:** SSO (Rajasthan), LDAP/AD (unwired), OTP SMS, WhatsApp/SMS (eSanchar), print server.
- **Solr/Redis:** session tokens in cache (`<token>` → UserView, `UNQ_SESS_USER_<id>`); notification queues (T_WHATSAPPMESSAGEQUEUE).
- **Scheduled Jobs:** triggerWhatsappReportsInQueue (15-min window).
- **Reports:** none evidenced.
- **Security:** **Critical gaps: password verification disabled (PasswordValidator no-op), static token bypass, test-user backdoor, reversible password obfuscation, CSRF disabled** (08 §9).
- **Permissions:** 2,078 `data-roleM` keys shipped to browser; `M_ModulePermissionMaster (path, apiMethod, apiPath, moduleName, action, parentId)` DB rows.
- **Cross-module Dependencies:** All modules.
- **Legacy Source Files:** `services/pas/UserManagementServiceImpl.java`, `services/custom/AddUserServiceImpl.java`, `services/SendNotificationServiceImpl.java`, `services/custom/ActionNotificationServiceImpl.java`, `services/AuthenticationServiceImpl.java`, `util/auth/DefaultAuthenticationImpl.java`, `util/auth/PasswordValidator.java`, `interceptors/CrudOperationsFilter.java` (permission tree), `modules/masters/hospital/schemas`, `js/user-module.js`, `js/notification-template-module.js`.
- **Migration Complexity:** 3/5 — IAM redesign required (JWT/sessions, real authZ, password hashing).
- **Risks:** Frontend-only RBAC; disabled password check; secrets in properties; SSO cookie flags.
- **Open Questions:** Role/permission DB dump needed for migration; LDAP future use.

---

## M36 — EJS (e-Jan Swasthya / ASHA-ANM field app)

- **Target service:** 28. Govt Programs (EJS/Aasmaan/Chiranjeevi)
- **Purpose:** Community health worker (ASHA/ANM) field data collection & incentive management.
- **Business Context:** 9 services, 61 entities; Android field app; checklists; geo masters; incentives.
- **Actors:** ASHA, ANM, Supervisor, District admin.
- **Submodules:** ASHA work, ANC/PNC/child care tracking, checklists & submissions, incentives, population registers, supervisor approvals, geo masters.
- **Features:** ejs-asha; ejs-ancpncchild; ejs-checklists; ejs-incentive; ejs-population; ejs-supervisor; ejs-masterLocation.
- **Screens:** ejschecklistquestions, ejsmasteractivity, ejsmasterentity, media.
- **Routes:** `masters:ejs`, `masters:ejanswathyaConfig`.
- **Backend APIs (service @Path):** `/ejs-asha`, `/ejs-ancpncchild`, `/ejs-checklists`, `/ejs-details`, `/ejs-incentive`, `/ejs-masterLocation`, `/ejs-population`, `/ejs-supervisor` (9 impls).
- **Business Rules (BR-* IDs):** none formalized in 06.
- **Status Lifecycle:** checklist submission/approval status; incentive status; UNKNOWN-35.
- **Database Tables:** models/ejs (61): ANDROID_* (ANC, PNC, MC, ECS, MCHN, MEMBERS, FAMILY, ASHA_WORK, WORK_DETAILS, REFER, SCHOOL, DEATH, DISEASES, INCENTIVES, HELP, SUPERVISOR_*, ATTENDANCE, CBAC, CHILD_CARE, CHILD_CARE_CHECKUP, FAMILY_PLANNING, FAMILY_AWC, ABORTION), ES_CHECKLISTS(+SUBMISSIONS+QUESTIONS+QUES_ANSWERS), MASTER_* (STATES, DISTRICTS, BLOCKS, GRAMPANCHAYATS, VILLAGES, ZONES, PHCS, SUBCENTERS, AWCS), USER_, USER_TYPES, USER_DESIGNATION, PROFILE, INDICATORS, MEDIA(+CATEGORIES+TYPES), CORONA_FAMILY/MEMBERS_DETAILS, MASTER_ACTIVITY, MASTER_ENTITY (BigInteger IDs).
- **Stored Procedures:** none evidenced.
- **Integrations:** PCTS (maternal tracking — pushDataToPCTS monthly), EJS portal data, Solr outreach registration.
- **Solr Dependencies:** patient index on outreach registration (EjsDetailsServiceImpl:2105).
- **Redis Dependencies:** master caches.
- **Scheduled Jobs:** ejs.asha.incentive.schedule (23:00), pushDataToPCTS (monthly last-day midnight).
- **Reports:** none evidenced.
- **Security:** field-user auth; supervisor approvals.
- **Permissions:** EJS keys (frontend).
- **Cross-module Dependencies:** M33 (incentive payments), geo masters.
- **Legacy Source Files:** `services/ejs/*` (9 impls), `models/ejs` (61), `masters/ejs/*`, `modules/masters/ejanswasthya (media)`, `js/vista-module.js` (unverified link).
- **Migration Complexity:** 3/5 — separate app ecosystem; incentive computation; checklists.
- **Risks:** Vista-module linkage unclear (UNKNOWN-05); field-app data volume.
- **Open Questions:** vista-module.js role; PCTS SOAP payloads.

---

## M37 — Aasmaan (Field App Backend)

- **Target service:** 28. Govt Programs (Aasmaan)
- **Purpose:** Backend for a field/mobile application (case & device management).
- **Business Context:** 10 impls; 35 entities with **no @Table** (LBase; convention-mapped names); 78 endpoints (04-api labels it "Maternal-Child/Telemedicine" — description conflicts with 02's "case & device management").
- **Actors:** Field workers, RSC staff, Institution admins.
- **Submodules:** Cases, devices, e-tickets, institutions, RSC call logs, stats, users, reports.
- **Features:** CRUD APIs for cases/devices/tickets/institutions; stats & reports; case checklists 1-4, delivery/discharge notes, CMFS, referral slips, schedules, monitoring (ANC/PNC), high-risk.
- **Screens:** none (API-only backend).
- **Routes:** none (mobile app backend).
- **Backend APIs (service @Path):** `/cases`, `/devices`, `/e_tickets`, `/institutions`, `/institution_levels`, `/rsc_call_logs`, `/stats`, `/users`, `/app_transaction_log` (78 endpoints).
- **Business Rules (BR-* IDs):** none formalized in 06.
- **Status Lifecycle:** case stage counts (getCaseStageCounts); referral finish/cancel.
- **Database Tables:** no JPA @Table — mobile app persistence, convention-mapped names (AASMAANCASE*, UNKNOWN-37); **migration must verify actual DB names** (05 §3.4).
- **Stored Procedures:** none evidenced.
- **Integrations:** none in-code (standalone).
- **Solr/Redis:** none evidenced.
- **Scheduled Jobs:** none evidenced.
- **Reports:** exportCase, exportInstitutionReferral, exportReport (endpoints).
- **Security:** device/user authenticate endpoints.
- **Permissions:** n/a (API auth per device/user).
- **Cross-module Dependencies:** none in-code (standalone).
- **Legacy Source Files:** `services/aasmaan/*` (10 impls), `models/aasmaan` (35), `js/queue-module.js` (aasmaan? UNKNOWN-36).
- **Migration Complexity:** 2/5 — isolated; DB naming verification required.
- **Risks:** Table-name verification (LBase, no @Table); purpose ambiguity (case/devices vs maternal-child).
- **Open Questions:** UNKNOWN-36/37; description conflict (02 vs 04-api).

---

## M38 — Integrations (ABDM/ABHA, NHCX/HCX, EHR, e-Aushadhi, Gov Schemes)

- **Target service:** 25. Interop & Notifications (+ 28 external programs)
- **Purpose:** National/state digital health & procurement interop.
- **Business Context:** 28 integrations cataloged (07 §3.1); ABDM biggest surface (~50 endpoints, 8 async jobs); HCX JWE; Solr/Redis correlation.
- **Actors:** System (integration), Nodal officers.
- **Submodules:** ABDM (ABHA, HIP/HIU, HPR, PHR); NHCX/HCX (insurance eligibility/pre-auth/claim); EHR sync (inbound/outbound HIS); e-Aushadhi (indents, stock, QC, returns); Aadhaar (biometric/OTP); Janaadhar/Bhamashah; RGHS/SIMS; Chiranjeevi; KDL (lab instruments); MDM (Janta master data); e-Raktkosh; PCTS; NMC; WhatsApp/SMS; HL7; FHIR; TIUXML; MinIO; SSO; print server; Jeevanvahini; Mera Aspataal; scan/print; eUpkaran; EXT LIMS; eSign (unwired).
- **Features:** ABDM patient queue (showAbdmPatientQueue); HCX FHIR bundles; EHR job execution (EHRJobDetails); inbound/outbound HIS queues; e-Aushadhi indent issue/QC/return status; aadhaar biometric auth templates.
- **Screens:** showAbdmPatientQueue, checkEligibilitySchema (nhcx), commons inbound/outbound, abdmQrGenerator, consentRequest(Data).
- **Routes:** `commons:consentRequest`, `consentRequestData/:concentId`, `abdmQrGenerator`, `masters:machineConfig`, ABDM/HFR facility screens.
- **Backend APIs (service @Path):** `/abha` (40), `/hpr`, `/nhcx` (10 HCX endpoints), `/ehr`, `/integration`, `/mdm`, `/eAushadhiIndent`, `/machineConfig`, `/sendEmail`, ABDMController callbacks (~50, dual v2/v3), HFRController (18), embedded FHIR server (dormant flag).
- **Business Rules (BR-* IDs):** none formalized in 06; NHCX request/status enums (0=ACCEPTED..3=FAILED).
- **Status Lifecycle:** EHR job status; inbound/outbound message status (RetryCount); ABDM consent/notification status; ABDM milestone gates (M1/M2).
- **Database Tables:** Inboundhis, Outboundhis (47 refs), EHRJobDetails, EmrDetails, EmrServices, EventSubmission, TeleConsultantDetails, T_PATIENT_ABDM(+_CC, _SHARETOKEN), T_PATIENT_CONSENT_REQUESTS_ABDM(+_DATA), T_USER_FACEAUTH, ABDMRequestQueue, T_WHATSAPPMESSAGEQUEUE(+STATUS), T_LOGIN_AUDIT / T_ABDM_AUDIT / T_APPOINTMENT_CHANNEL, masters (EraktkoshComponentcodeMapping).
- **Stored Procedures:** HL7/EHR-adjacent procs; none module-specific evidenced.
- **Integrations:** 28 cataloged (07 §3.1) — see Business Context.
- **Solr Dependencies:** **ABDM consent discovery queries Solr (searchABHAPatientHIDInSolr — mobile+DOB ±1yr, Levenshtein ≤3)**; healthId partial updates (CommonABDMUtil:1391); patient/encounter index events.
- **Redis Dependencies:** `ABDM_REQ_<reqId>` / `ABDM_RES_<reqId>`, `ABDM_PAT_AUTH_<txId>`, `ABDM_HPR_AUTH_<userId>`, `ABDM_SES_TKN`, `HIP_ABDM_KEYS` / `HIU_ABDM_KEYS` (365d), `<nodename>_ABDM_S/F` counters, job kill-switches (disableAutoDemoAuth etc.).
- **Scheduled Jobs:** ABDM retry jobs (5s), ABHA push jobs (5s), eRaktKosh nightly, RGHS SIMS 23:30, PCTS monthly, updateNMCData, triggerWhatsappReportsInQueue, creteEHRFiles (disabled), HL7 inbound pickup (disabled), syncSolr* drainers.
- **Reports:** templates/nhcx/* (claim_on_submit, coverage_eligibility_on_check, insurance_plan_on_request, preauth_on_submit + FHIR bundles), aadhar_*vm, janaadhar_*vm, bhamashah_*vm, rghs*.vm.
- **Security:** HCX JWE keys (abdm.keys.file.path); **hard-coded secrets (eAushadi Basic auth, WhatsApp keys, SIMS AuthKey, SSO mobile key)** (07 §3.3); ROLE_INTEGRATION gating on `/int/*`.
- **Permissions:** ROLE_INTEGRATION (backend-controlled integration surface).
- **Cross-module Dependencies:** M01 (ABHA), M04/M32 (NHCX claims), M13 (e-Aushadhi), M15 (KDL), M17 (e-Raktkosh), M36 (PCTS).
- **Legacy Source Files:** `services/abdm/*` (AbhaServiceImpl, HPRServiceImpl, NHCXServiceImpl), `services/ehr/EHRUtilServiceImpl.java`, `services/aadhar/*`, `com/ohum/xcg/**` (abdm server+client, genserv FHIR, hcx dto), `services/IntegrationServiceImpl.java`, `services/custom/MdmServiceImpl.java`, `services/custom/InventoryEAushadiIntegrationImpl.java`, `util/HL7Helper.java`, `util/SolrUtil.java`, `templates/nhcx/*`.
- **Migration Complexity:** 4/5 — ABDM/HCX dual-version callbacks; JWE; secrets hygiene; dormant integrations must be re-baselined.
- **Risks:** ABDM callback routing (catch-all fragile); Solr coupling for consent; hard-coded credentials; dormant integrations create false expectations.
- **Open Questions:** e-Raktkosh/KDL sync direction & frequency (02 open Q10); FHIR server reachability (recon §7).

---

## M39 — Commons / Shared Platform Services (Merged Mortuary → M28)

- **Target service:** 21. Reference (master/config) + 22. IAM (user/config) + cross-cutting; per `02` heading: "Mortuary & Other Commons — merged into M28; Commons platform services"
- **Purpose:** Platform-level shared services used by all modules.
- **Business Context:** 67-key config; audits, config application, shifts & rosters, inbound/outbound, MRN series, schemas designer, generic CRUD filter (659 `@CrudApi` entities), report dispatcher, `/ext/api` mapping.
- **Actors:** System admin, all users.
- **Submodules:** Audits (auditSchema, newAuditSchema); App config (configapplication, applicationModuleStatus, hospitalCreationSchema, hospitalGeoTagging, machineConfigSchema); Staff/doctors/user-pass (staffmaster, doctormaster, addPatientUserPassSchema, OutreachUserCreationSchema); Shifts & rosters (shiftDetails-Schema, shiftRoster-Schema, userShiftDetailsListSchema, addIpdRosterSlotsSchema); Inbound/outbound (inbound.json, outbound.json); Verification (first/secondVerificationSchema, boxCommonVerification); MRN series (mrnoseries); Schemas designer; Uploads; Service master mapping (servicemastermapping, uploadServiceMappingSchema); Questionnaires (questionaireSocialWorker); backGroundTasks; appDocumentation.
- **Features:** generic CRUD via CrudOperationsFilter (entity-name routes → GenericServiceImpl.crudadd/update/get); report dispatcher; `/ext/api` integration mapping.
- **Screens:** auditSchema, configapplication, hospitalCreationSchema, shiftRoster-Schema, mrnoseries, schemasDesigner, mortuarySchema (→M28), notificationTemplateSchema, machineConfigSchema, uploadDocumentSchema.
- **Routes:** `commons:login1/logout/options`, `commons:shiftDetails/roster/ipdRoster`, `commons:backgroundTasks`, `commons:productionIssues`, `commons:coronaDetailsList/addCoronaDetails/editCoronaDetails/coronaPatientReport/coronaStats`, `commons:appDocumentation`, `commons:machineConfiguration(List)`, `commons:ipdDashboard`, `commons:applicationModuleStatus`, `commons:audits`, `commons:newAuditsReports`, `commons:editSchema/:schemaName`, `commons:addForm`, `commons:hospitalGeoTagging`, `commons:hospitalCreation`, `commons:outreachusers`, `commons:scanPatient`, `commons:scanCheckin`, `commons:validateToken`, `commons:deliveryReport`, `commons:forgotPassword`, `commons:resetPassword/:token/:unitId/:id`, `commons:selectDefaultRole`.
- **Backend APIs (service @Path):** `/masterConfigService`, `/helper`, `/machineConfig`, `/mdm`, `/integration`, generic CRUD on `api/rest/<entity>/*` (659 @CrudApi entities), `reports/<reportName>` dispatcher, `/ext/api/<mapped-path>`.
- **Business Rules (BR-* IDs):** none formalized in 06; generic CRUD validates MBase.validate().
- **Status Lifecycle:** application module status (applicationModuleStatus); audit events (~60 T_TRX_HISTORY eventIds).
- **Database Tables:** masters (ConfigApplication, ConfigAutocharges, TUserdetails, Staffmaster, Doctormaster, MrnSeries, AuditEventMaster, MachineConfiguration, modulenamemaster, languageLabel), transactionals (MachineConfiguration, T_QUESTIONANSWER), T_TRX_HISTORY, T_AUDIT, T_LOGIN_AUDIT, T_ABDM_AUDIT, T_APPOINTMENT_CHANNEL, block_mst/city_mst/district_mst (Rajasthan geography), T_FREQ_ITEMS.
- **Stored Procedures:** audit-adjacent sequences (t_trx_history_seq, T_LOGIN_AUDIT_SEQ, T_ABDM_AUDIT_SEQ, T_APPOINTMENT_CHANNEL_SEQ); auto_code_seq.
- **Integrations:** all (integration broker); `/ext/api` mapping with ROLE_INTEGRATION.
- **Solr/Redis:** master cache TTL resets; gzip/nogzip response caches; session tokens; `USR_BUILD_<userId>`; `perf.current.db.locks`.
- **Scheduled Jobs:** backGroundTasks (updateProduction), audit flush per request.
- **Reports:** generic_reports.vm (dispatcher).
- **Security:** **Central: ServiceLayerAspect auth gate (401), MBase audit stamping, T_TRX_HISTORY pipeline; per-path permission check commented out** (08 §3.1).
- **Permissions:** Configuration keys (1,424); `Login.*` (9).
- **Cross-module Dependencies:** All (platform).
- **Legacy Source Files:** `fjs-config_commons.json` (67 keys), `services/pas/MasterServiceImpl.java` (/masterConfigService), `services/custom/MasterCachedServiceImpl.java` (/master), `services/custom/HelperServiceImpl.java`, `services/custom/MachineConfigServiceImpl.java`, `interceptors/CrudOperationsFilter.java`, `aspects/IntegrationOperationsFilter.java`, `services/GenericServiceImpl.java`, `services/BaseServiceImpl.java`, `audit/AuditHelper.java`, `js/common-module.js`, `modules/commons/schemas/*`.
- **Migration Complexity:** 3/5 — platform backbone; generic CRUD filter + report dispatcher + /ext mapping must be rebuilt; 659-entity registry.
- **Risks:** Nashorn dispatcher; 222 unmatched URLs triage; ext.js absence; CRUD filter security.
- **Open Questions:** schemasDesigner semantics; backGroundTasks scope; moduleStatus data.

---

## M40 — TIU/Clinical Forms Engine (tiuxmls)

- **Target service:** none dedicated in 10 (candidate: 4. Clinical notes + 12. Nursing) — UNKNOWN
- **Purpose:** Digitized clinical forms (TIU-style) rendered in patient record; specialty-specific documentation.
- **Business Context:** 368-key config (schemas/templates 1:1); tiuxml_mapping.json tile maps; versioned forms (_V10.._V15, _new, _P, _B, _AH, _AL, _nhnc, _risl, _oqa, dated variants).
- **Actors:** Physicians, Nurses, Allied health (AH/AL), Social workers.
- **Submodules:** Form families: History/Exam/Assessment per specialty (ER, CARDIO, NEURO, ORTHO, OBS/GYN, PEDS, NEO, GERIATRIC, PHYSIO, OT, SLP, PSY, RISL); Nursing documentation; Risk scales (Braden, Barthel, FLACC, Morse Fall, APGAR, Modified Aldrete, MINI_MENTAL, Neonatal Abstinence, Pain, Waterlow, Acuity2); Procedure checklists (CVC, CAUTI, CRBSI, SSI, VAP, MRSA, brain death, sterilization, IUD, vasectomy, tubectomy); Discharge/advice; Emergency/trauma; Handover (HANDOFF_COMMUNICATION, Telephonic_Handover); Social work (SW_*, SSW_*); Family welfare; Death/autopsy (Post_Mortem, Death_Certificate, DEATHNOTE); COVID.
- **Features:** 368 forms; gender/age-based mapping (tiuxml_mapping gender/fromage/toage); template flags; note tiles (csNoteTiles.html, tilesHF.html); charts from charts/ODCWF_*.json (29 formats).
- **Screens:** 368 schema keys (Acuity2, Braden_Scale, Barthel_Index_New, NURSINGINITIALASSESSMENT_V*, OBSHIST_V11, PEDS_EXAMINATIONS_V2, HANDOFF_COMMUNICATION_V13, Post_Mortem_Report, Death_Certificate, ...).
- **Routes:** `tiuxmls:designer`, `tiuxmls/:txml`, `masters:tiuxmlMapping`, `masters:formdesigner`, `masters:formConfiguration/:fileType`, `masters:noteTileConfig`.
- **Backend APIs (service @Path):** forms served via tiuxml.js / note endpoints (UNKNOWN-40); `/clinical` (getNoteContent).
- **Business Rules (BR-* IDs):** none formalized in 06.
- **Status Lifecycle:** form-specific statuses UNKNOWN-39.
- **Database Tables:** forms stored via note/document tables — UNKNOWN-40.
- **Stored Procedures:** none evidenced.
- **Integrations:** TIUXML file exchange (tele-ICU/CPRS documents), chemofile mapping.
- **Solr/Redis:** healthfactor/icd10 lookups inside forms.
- **Scheduled Jobs:** none evidenced.
- **Reports:** plainText variants + templates 1:1.
- **Security:** role-specific form visibility (docit roles).
- **Permissions:** Nursing/clinical keys; note tile config.
- **Cross-module Dependencies:** M09 (nursing), M01 (patient), M12 (OT forms), M08 (notes).
- **Legacy Source Files:** `fjs-config_tiuxmls.json` (368 keys), `modules/tiuxmls/schemas|templates` (368 each), `tiuxml_mapping.json`, `chemo_tiuxml_mapping.json`, `nhnc_tiuxml_mapping.json`, `op_tiuxml_mapping.json`, `tiuxml.json`, `tiuxml.js`, `util/tiuxml/*`.
- **Migration Complexity:** 3/5 — 368 forms; recommend reusing data model, not renderer (03 §6 Q3).
- **Risks:** Versioned form drift; renderer scope; gender/age mapping rules.
- **Open Questions:** Form storage model (UNKNOWN-40); NHNC variant context.

---

## Evidence Conflicts Between Source Docs

1. **Shifted module numbers in `02-business-module-catalog.md`** — the per-module "Dependencies" lines and several cross-module-table rows use wrong numbers vs headings: e.g. M02 "M08 (billing)/M18 (orders)/M29 (queue)", M03 "M08 (billing)/M15 (nursing)/M18 (orders)", M07 "M15"/"M34 BCMA", M08 "M16 (pathology)/M21 (radiology)/M15 (nursing)", M09 "M16/M21 (results)", M17 "M16 (serology?)". Correct per headings: billing=M04, orders=M08, queue=M27, nursing=M09, pharmacy=M07, BCMA=M30, pathology=M15, radiology=M16, serology=M15 (marked ambiguous in source). This inventory uses heading-number corrections; only M17→"M15 serology" retains the source's question mark.
2. **M37 Aasmaan description conflict** — `02` calls it "backend for field/mobile app (case & device management)"; `04-api-catalog.md` headers it "Maternal-Child/Telemedicine (78 endpoints)". Evidence (case checklists 1–4, ANC/PNC monitoring, delivery/discharge notes) leans maternal-child; 02's inventory text is broader. Both retained.
3. **M39 naming** — heading says "Mortuary & Other Commons — merged into M28; Commons platform services": mortuary content lives under M28; M39 is Commons. Agent-02 findings §4 lists commons (67 keys) incl. mortuary* schemas — ownership split between M28 (death/mortuary domain) and M39 (shared schemas).
4. **Billing "M17 (pharmacy bill)"** (02 M04 Dependencies) — M17 is Blood Bank per headings; pharmacy bill belongs to M07. Flagged in conflicts; resolved to M07.
5. **Target-arch service count** — `10-target-architecture.md` proposes 29 candidate services vs 40 frontend modules; reconciliation-notes §2 confirms the 40-module granularity is retained with service ownership mapping (each module's "Target service" field above).
6. **Table counts** — `02` says masters 609/transactionals 137; `05` says 582 M_* tables / 172 T_* (808 explicit @Table + 35 convention = ~843, ~898 entities). Reconciliation §4 confirms ~843 tables; the 609/137 numbers are entity-class counts incl. non-mapped; noted, not conflicting in substance.
7. **Grievance target service** — 10 §2.1 row 20 lists ComplaintDetailsServiceImpl (equipment complaints in 02!) under Grievance; 02 keeps equipment complaints in M20. Equipment complaint services are duplicated in both — flagged, left per 02 with target-arch note.

---
*End of MODULE-INVENTORY. 40 modules (M01–M40). Primary source: `02-business-module-catalog.md`; evidence cross-referenced from 03–08, 10, and `_discovery/`.*
