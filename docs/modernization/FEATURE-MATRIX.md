# FEATURE-MATRIX â€” Master Feature Inventory (iHMS / OHUM PAS NG)

> **Synthesized artifact (Feature & Traceability Synthesizer).** Compiled from existing discovery docs only â€” no re-exploration, no code changes.
> Sources: `02-business-module-catalog.md` (M01â€“M40), `03-legacy-frontend-inventory.md` + `_discovery/agent-03/route-inventory.md` (routes/schemas/JS),
> `04-api-catalog.md` + `_discovery/agent-04/findings.md` (APIs, class:method), `06-business-rules-catalog.md` (BR-*),
> `07-integrations-and-search.md` (Solr/Redis/integrations), `05-database-catalog.md` (tables/procs), `09-react-migration-design.md` (target React),
> `10-target-architecture.md` (target microservices, numbered bounded contexts).
>
> **Conventions:** Screen = legacy hash route (`module:page`) + FAUG schema key. API = `METHOD api/rest/<path>`. Business Rules = BR-* IDs (06).
> Target Service = numbered bounded context from 10 Â§2.1. Solr = collection touched (07 Â§1.2). Redis = key pattern (07 Â§2.1).
> `-` = no evidence found (never invented). This matrix is the primary compliance artifact for "no feature lost in migration".

---

## M01 â€” Patient Management (Registration & Demographics)

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M01 | Registration | New patient registration (MRN generation) | `op:appointmentFlow` / `addpatientDetailRegdto-schema`, `commons:patientList`, `commons:patientRecord` | POST `/patient/registerPatient` | BR-PAT-001, BR-PAT-002, BR-PAT-003 | T_REGISTRATION, T_REGISTRATION_DETAILS, T_Registration_Int, M_MRNSeries | patient (indexPatientInSolr) | current_pat_<patientId/mrno> | ABDM/ABHA, Aadhaar/Janaadhar/Bhamashah templates | 1 Patient Identity & Registration |
| M01 | Registration | Demographic search (Aadhaar/driving-licence/Janaadhar/ABHA/MRN/mobile) | `walkinVisit-schema.json` (search controlEvents), `patient-search-list.html` | POST `/patient/getPatientGeneralDetailsListForSearch`; nursing variant POST `/nursingList/getPatientGeneralDetailsListForSearchNursing` | BR-PAT-002 (duplicate detection via Solr) | - | patient (searchPatientInSolr, searchPatientInSolrByIdentity, searchExactPatientInSolr) | current_pat_ mirror | - | 27 Search & Index (reads) / 1 (writes) |
| M01 | Registration | Aadhaar OTP / biometric verification | `walkinVisit-schema.json` (aadhaar fields) | POST `/walkinVisit/aadharOtpGeneration`, `/walkinVisit/aadharOtpVarification`, `/walkinVisit/aadharBiometricVarification` | - | T_USER_FACEAUTH | - | - | Aadhaar OTP/biometric (aadhaar.auth.api, local.biometric.scan.url) | 1 |
| M01 | Registration | Patient photo capture/upload | `walkinVisit-schema.json` (photo/cameraPhoto), `capturePhoto.html` | POST `/walkinVisit/addPatientPhoto` | - | - | - | - | - | 1 |
| M01 | Verification | HID (health-ID) merge / dedup | `commons:updatePatient`, patient record screens | POST `/patient/mergeHId` (PatientServiceImpl) | BR-PAT-002 (BEXA040 on healthId duplicate) | - | patient | - | ABDM | 1 |
| M01 | Verification | ABHA number keying & ABDM patient matching | `walkinVisit-schema.json` (abhaSearch, abhaRegistration), `abhaPatientProfileList.html` | POST `/abha/findPatient`, `/abha/registerABHAProfile`, GET `/abha/startLookup`, `/abha/verifyLookupOTP` | - | T_PATIENT_ABDM(+_CC, _SHARETOKEN), T_PATIENT_CONSENT_REQUESTS_ABDM(+_DATA) | patient (searchABHAPatientHIDInSolr) | ABDM_REQ_/ABDM_RES_, ABDM_PAT_AUTH_, ABDM_SES_TKN, HIP_ABDM_KEYS, ABHANUM_<healthIdNumber> | ABDM gateway (HIP/HIU), ABHA | 1 (+25 Interop) |
| M01 | MLC | Medico-legal case (MLC) capture + documents | `mlcDetailsSchema.json`, `updateMLCDetailsSchema.json`, `addMlcDetailsSchema.json`; `op:mlcDetails` | GET `/walkinVisit/getMlcDetailsByVisit`, PUT `/walkinVisit/updateMlcDetailsbyVisitId`, GET `/walkinVisit/mlcFile/{id}`, `/walkinVisit/document/{id}` | BR-OPD-008 (MLC row + audit; brought-dead → isdeath) | T_MLCDETAILS | - | - | - | 3 ADT & Bed (T_MLCDETAILS owner) |
| M01 | MLC | MLC clearance filters (IPD) | `ip:MlcclearanceListFlow`, `mlcClearanceFilters.json`, `mlcList.html` | `patientTransferDischarge.clearance*` (30 endpoints) | - | T_MLCDETAILS | - | - | - | 3 |
| M01 | Mother-child | Motherâ€“child linking | `ip:motherChildLinking`, `motherChildLinkingSchema.json` | `patientTransferDischarge.motherChild*` | - | SpouseAndChildrenDetails, FamilyMemberDetails, T_ANCVISITDETAILS | - | - | - | 3 |
| M01 | Categories | Patient sponsor / category (BPL, insurance, RGHS, Janaadhar, Bhamashah) | `walkinVisit-schema.json` (patientSponsor/planId, getPolicies) | POST `/walkinVisit/addwalkinVisit` (sponsor DTO); `ipdadmission/getSponsorByAdmissionID` | BR-OPD-005 (sponsor defaulting), BR-OPD-001 (non-residential tariff) | T_PATIENTSPONSOR, T_SPONSOR | - | Plan_master_full cache | RGHS (getChiranjeeviDetailByJanAdhar) | 1 / 5 (sponsor) |
| M01 | Corona | COVID/Corona quarantine tracking | `commons:coronaDetailsList`, `commons:addCoronaDetails`, `commons:editCoronaDetails`, `commons:coronaPatientReport`, `commons:coronaStats`; `patientCoronaDetailsSchema.json` | `/patient` corona endpoints (PatientServiceImpl) | - | T_coronaStats, patientCoronaDetails | - | - | - | 1 |
| M01 | Social work | Social worker assessment | `patientManagement:socialWorker`, `addSocialWorker` (fjs-config_patientManagement) | `/patient` (addSocialWorker endpoints) | - | - | - | - | - | 1 |
| M01 | History | Patient detail / history / facesheet | `op:patientDetail`, `op:patientHistory/:mrno`, `op:editPatientDetail/:mrnNo/:unitId`; `patientRecordSchema.json`, `patientHistorySchema.json`, `showPatientDetailSchema.json` | GET `/patient/getPatient`, GET `/patient/getEMRAdmVisitByPatientId`, `/nursingList/getPatientEncounterById` | - | T_REGISTRATION, T_Encounter | patient/encounter | - | - | 1 / 2 |
| M01 | Reports | Patient history / health card prints | `patientHistory.vm`, `patientAllHistory.vm`, `healthCardSheet.vm` | POST `/walkinVisit/printPatientHealthCard` | - | - | - | - | - | 26 Reporting |

## M02 â€” OPD / Outpatient Department

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M02 | Walk-in | Walk-in visit registration (flagship) | `op:appointmentFlow`, `op:walkin_confirmed`; `walkinVisit-schema.json` (6,621 lines) | POST `/walkinVisit/addwalkinVisit?pat_token_no&app_type` | BR-OPD-001..006, BR-OPD-008 (MLC on visit), BR-OPD-009 (walk-in validation gates) | T_Encounter, T_Visit, T_PATIENTSPONSOR, M_VISITTYPEMASTER, T_PATIENTTOKEN | encounter (indexEncounterInSolr), patient | current_enc_<encId_E>, QMS token queues | NHNC (walkinVisitNhnc), telemedicine, e-Mitra (kiosk) | 2 Appointment & Encounter |
| M02 | Walk-in | Self/kiosk registration | `kiosk:selfRegPatient`; `minimal-walkin-appointment.json` | POST `/walkinVisit/selfPatientRegistation`, POST `/markandaddvisit/checkin` | BR-OPD-002 | T_Encounter, T_REGISTRATION | encounter | - | e-Mitra | 2 / 29 |
| M02 | Walk-in | Token search / queue token | `walkinVisit-schema.json` (queue token icon), `token_sheet.vm` | GET `/walkinVisit/searchToken`, GET `/walkinVisit/searchVisitbyToken` | - | T_PATIENTTOKEN | - | QMS token queues | - | 29 Queue & Kiosk |
| M02 | Appointment | New appointment booking (slots/cabin cascade) | `op:addAppointment/:doctor_id/...`; `addpatientRegApptdto_schema.json`, `doctor-appintment-slots-schema.json` | POST `/appointmentschedule/getAppointmentScheduleNew`, POST `/appointmentschedule/getCabinWiseApointmentSlotCount`, GET `/appointmentschedule/getDepartmentsByDate`, GET `/addUserService/getDepartmentwisecabinById` | BR-OPD-002 (slot collision: BERA028/BEXA029) | T_Appointment, T_Encounter | encounter (on confirm) | - | - | 2 |
| M02 | Appointment | Reschedule / cancel appointment | `op:rescheduleApppointment/:...`; `updateAppointment_schema.json`, `apptCancelReason.html`, `apptRescheduleReason.html` | POST `/updateappointment/cancelupdateappointment` (AddUpdateAppointmentServiceImpl) | BR-OPD-002 | T_Appointment | encounter (partial update, AddUpdateAppointmentServiceImpl:191) | - | - | 2 |
| M02 | Appointment | Appointment list (12 categories) | `op:appointmentList`, `appointmentListOnline`, `appointmentListdoc`, `appointmentListNhnc`, `appointmentListNew`; `appointment_list-schema.json` | `patientRegAppt/*` (22), POST `/patientRegAppt/sendOTP`, `/validateOTP`, POST `/billSummary/getAllBillsForOnline` | - | T_Appointment | encounter | - | ABHA OTP | 2 |
| M02 | Appointment | Appointment upload (bulk) | `op:uploadAppointments` | file upload + parse endpoints | - | T_Appointment | - | - | - | 2 |
| M02 | Schedule | Doctor schedules view | `op:doctorSchedules/:doctor_id/:department_id`, `op:mySchedule`; `showDoctorOpSchedule.json`, `doctor-appintment-slots-schema.json` | POST `/appointmentschedule/getAppointmentScheduleNew?type=D`, crudop `DoctorAppointmentSlots/getSlots` | - | T_Appointment | - | - | - | 2 |
| M02 | Schedule | Slot management (doctor/department) | `op:manageDoctorSlot`, `op:manageDepartmentSlot`, `op:showDepartmentSlot`; `showDepartmentWiseSlots.json` | POST `/appointmentschedule/addOrRemoveSlots` | BR-OPD-002 (slot duration = config.appointmentslot) | T_Appointment | - | - | - | 2 |
| M02 | Emergency | Emergency registration | `op:addEmergency/:doctor_id/...`, `op:emergency_confirmed`; `minimal-walkin-appointment.json`, `walkinVisitNhnc` | POST `/walkinVisit/addEmergencyRegistration`; POST `/markandaddvisit/checkin` | BR-OPD-003 (EV/<PREFIX>/<YY>), BR-OPD-004 (isEmergency visit type) | T_Encounter | encounter | - | NHNC | 2 |
| M02 | Doctor | Doctor check-in / check-out (cabin) | `op:doctorCheckIn`; `showDoctorOpSchedule.json`, `doctor-cabin-checkin.html` | PUT `/doctor/checkinCheckout?isCheckin=`, PUT `/walkinVisit/doctorcheckout`, PUT `/walkinVisit/updateCabinandDoctId`, PUT `/walkinVisit/releaseCabin` | BR-OPD-007 (checkoutâ†’VisitStatus 11); A1 (status 9/10) | T_Encounter | encounter (searchPatientInEncounterForCheckinCheckout) | - | - | 2 |
| M02 | Follow-up | Follow-up visit requests | `op:followUpVisitReqList`; `followupVisitReq_list-schema.json` | POST `/walkinVisit/addFollowUpVisit` | BR-OPD-004 (visit-type sequence) | FollowUpVisitInfo, T_Encounter | - | - | - | 2 |
| M02 | Referral | Referral (NHNC/doctor) | `walkinVisit-schema.json` (referralDetails), `doctorReferralSchema` | `walkinVisit.addwalkinVisit` (referral DTO) | - | - | - | - | NHNC | 2 |
| M02 | Camp | Camp registration (OP camps) | `op:campRegistration`; `opCampRegistration.html` | `patientRegAddmission/patientRegAppt.*` camp ops | - | - | - | - | - | 2 |
| M02 | Telemedicine | Telemedicine department/unit/speciality lists + room | `walkinVisit-schema.json` (telemedicine block) | GET `/appointmentschedule/getDeptsForTsu`, `/appointmentschedule/getTelemedicineSpecialityUnit`, `/appointmentschedule/getTelemedicinePatientUnitDetails`, `/appointmentschedule/getAppointmentScheduleTeleconsult` | - | TeleConsultantDetails | - | - | Telemedicine (createTeleconsultRoom) | 2 |
| M02 | Kiosk | Kiosk self-appointment / account search | `kiosk:selfAppointment/...`, `kiosk:searchAppointment/...`, `kiosk:accountSearchSchema` | `appointmentschedule.*`, `markandaddvisit/checkin` | - | T_Appointment | - | - | e-Mitra (online appointment), ABHA | 2 / 29 |
| M02 | Print | OPD card / labels / BHT barcode | `op:opFaceSheet`; `opdCard` template, `appointment_token.vm`, `appointmentOpLabel.vm` | POST `/walkinVisit/printBHTBarcode`, `/walkinVisit/printIpLable`, `/walkinVisit/rislPrintOpLable`, `/walkinVisit/rislPrintIpLable`, `/patientRegAppt/pdfAppointmentFaceSheet*` | - | - | - | - | QZ Tray print | 26 |
| M02 | Questionnaire | Pre-consult questionnaire (FPV/PCTS/assessment) | `op:assessmentForFpv`, `queue:nurseDesk`; `questionnaireSchema.json` | `/questionnaireService/*` (QuetionnaireServiceImpl) | A1 (T_Appointment status 4/6) | T_QUESTIONANSWER | - | - | FPV/PCTS | 2 |

## M03 â€” IPD / Admission, Bed & Discharge

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M03 | Admission | Admission request (planned/emergency) | `ip:admissionOrder`, `ip:admissionOrderEmer`, `ip:admissionConfirm`; `addpatientAdmissiontdto_schema.json`, `+_emergency`, `addBabyAdmissiondto_schema.json` | `/patientRegAddmission/*` (23), `/ipdadmission/*` (18: getSponsorByAdmissionID, addAdmMLCDetails) | BR-IPD-001 (BERA022/BERB009/BERB013/BERB012), BR-IPD-004 (IPD no template), BR-IPD-006 (ambulance carry-over ≤6h) | T_Admission, T_AdmissionDetails | encounter (id=<admId>_A via indexAmissionInSolr) | current_enc_<admId>_A | Chiranjeevi (INSURANCESCHEMEID=2), RGHS packages, HIS addAdmissionRequestToHIS | 3 ADT & Bed |
| M03 | Admission | Admission list / request list | `ip:admissionList`, `ip:admissionReqList`; `admission_list-schema.json`, `admissionRequest_list-schema.json` | `patientRegAddmission.list*` | - | T_Admission | encounter | - | - | 3 |
| M03 | Admission | Interim admission (unbedded) | `ip:showInterimAdmissions`; `interimAdmissions.json` | `ipdadmission.interim*` | BR-IPD-005 (auto-abscond >3 days, ApplicationJobs:991-1019); A4 (BEDID=0/null) | T_Admission | - | - | - | 3 |
| M03 | Admission | Admission cancellation | `admissionCancelReason.html` | POST `/canceladmission/cancel` (CancelAdmissionServiceImpl.cancelPatientAdmission) | A4/A5 (bed status reset to 0) | T_Admission, M_BEDMASTER | encounter | - | - | 3 |
| M03 | Bed | Bed management board (buildingâ†’wardâ†’classâ†’bed) | `ip:bedManagement/:class_id/:ward_id/:bed_id/:building_id/:filterId`, `ip:bedManagementNew/...`; `bed_management_schema.json`, `bed-screen-building.html`, `bed-managment-*.html` | POST `/bedmanagement/getBuildingMasterDetailsNew`, `/bedmanagement/getWardDetails`, `/bedmanagement/getBedDetails`, `/bedmanagement/getBedCount`, `/bedmanagement/getPendencyDashboardData` | BR-IPD-002 (auto-allocation CAS), BR-IPD-003 (isrelease gate BERB005) | M_BEDMASTER, T_ADMISSIONDETAILS | - | bed availability (cache) | - | 3 |
| M03 | Bed | Bed allocation (auto/manual) | `ip:admissionOrder` (BedPicker) | `patientRegAddmission.*` (bed claim); GET `/bedmanagement/getAvailableBeds`, `/bedmanagement/isBedAvailable` | BR-IPD-002 (OCCUPIED=false CAS, BEXB001), BR-IPD-003 | M_BEDMASTER, T_ADMISSIONDETAILS | encounter | - | - | 3 |
| M03 | Bed | Bed reservation | `ip:bedReservation`, `ip:unReservedBedList`; `bedReservationdto_schema.json`, `addBedReservation.html` | `/bedreservation/*` (4) | - | T_BEDRESERVATION | encounter (BedReservationServiceImpl:165) | - | - | 3 |
| M03 | Bed | Bed transfer / OT transfer | `ip:transferReqList`; `bedTransferSchema.json`, `transferRequest_list-schema.json` | GET `/bedmanagement/getTransferDetails`, `/bedmanagement/getOTTransferDetails`; `bedmanagement.transfer*` | A5 (BedStatusId 0/6) | T_TRANSFERREQUEST | encounter | - | - | 3 |
| M03 | Bed | Bed block / release / cradle beds | `ip:bedRelease`, `ip:showAllCradles`, `ip:otBedManagement`; `bedBlock.html`, `showAllCradlesSchema.json` | GET `/bedmanagement/getBlockSubmit`, `/bedmanagement/getAllCradles`, `/bedmanagement/getCradleCount`, `/bedmanagement/getOtBedCount`, `/bedmanagement/udateBedStatus` | BR-DIS-003 (release BedStatusId=6); BR-IPD-002 (cradle/non-census) | M_BEDMASTER, T_ADMISSIONDETAILS | - | - | - | 3 |
| M03 | Discharge | Discharge advice + clearance list | `ip:updateDischargeStatus`, `ip:clearanceListFlow`, `ip:MlcclearanceListFlow`; `dischargePatientSchema.json`, `discharge_clearance_schema.json`, `getAdmissionClearanceList.json`, `clearanceTab.html` | `patientTransferDischarge.clearance*`; POST `/bedmanagement/initiateDischargeClearance`, `/bedmanagement/markLabClearance`, GET `/bedmanagement/checkDischargeClearanceCount`, `/bedmanagement/getClearanceCount` | BR-DIS-001 (BERB023/015/011/024/025) | T_AdviseDischarge, T_Discharge, T_PATIENTDISCHARGEDATA | - | CDD_<opdipdid> discharge lock (ServiceLayerAspect:1147, 60s) | - | 3 |
| M03 | Discharge | Discharge execution (incl. death handling) | `dischargeSummary/*_dis_form.html`, `updateDischargeStatus` | `patientTransferDischarge.*` (30) | BR-DIS-002 (CIMS_ADDBEDDISCHARGE, IsExpiredâ†’death, CIMS_UPDATEIPDISDISCHARGE); BR-DIS-003 | T_Discharge, T_Admission, T_REGISTRATION_DETAILS, PatientDeathDetails, T_TRANSFERREQUEST | patient (death=true partial), encounter (cleared) | - | COVID referral (inter-hospital), EHR | 3 |
| M03 | Discharge | Discharged patients list / readmission | `ip:dischargedPatients`; `showDischargedPatientSchema.json` | `patientTransferDischarge.listDischarged*` | - | T_Admission | - | - | - | 3 |
| M03 | Attendant | Attendant pass issue/return/reprint | `ip:attendantPass`; `attendantPassSchema.json`, `attendantPass*.html` | POST `/walkinVisit/printAttendantPass` | - | T_ATTENDANTPASS | - | - | - | 3 |
| M03 | Nursing station | Nursing station / bed census | `ip:nursingStation`; `nursingStation` schemas | `/nursingstationdefination/*` (2); `bedmanagement.getBedCensusAndNonCensusList` | - | - | - | - | - | 3 / 12 |
| M03 | TID | TID (treatment identification) list | `ip:showTidList`; `tidListSchema.json` | `ipdadmission.tid*` | - | T_ADMISSION_TID | - | - | RGHS/Chiranjeevi (TID) | 23 RCM & Claims |
| M03 | Doctor | IP doctor check-in | `ip:doctorCheckin`; `showDoctorIpSchedule.json` | PUT `/doctor/checkinCheckout` | - | - | - | - | - | 2 |
| M03 | Summary | OPD/IPD summary | `ip:summaryOpdIpd` | - | - | - | - | - | - | 26 |

## M04 â€” Billing, Payments, Receipts & Refunds

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M04 | OP billing | OP billing flow (service lines â†’ confirm) | `billing:billingFlow`, `billing:patientBilling`, `billing:viewOpdCharges`; `addBillingSchema.json`, `opBillPaymentSchema.json` | POST `/servicemaster/getServiceListForBill`, POST `/serviceOrder/addServiceOrder`, POST `/serviceOrder/addServicechargesNew`, GET `/serviceOrder/allServiceFastByGet?opd_id=` | BR-BILL-001..006 | T_BILL, T_BILLDETAILS, T_CHARGES, T_CHARGES_TEMP, T_DOCTORSHARE | bill search (InvoiceGenerated) | tariff lookup | - | 5 Billing & Settlement |
| M04 | IP billing | IP billing flow (pharmacy separate) | `billing:ipBillingFlow`, `billing:opIpBillingFlow`; `addIpBillingSchema.json`, `ipBillSummeryWithoutPharmcy.vm`, `addOpIpBillingSchema.json` | POST `/serviceOrder/addServicechargesNew`, POST `/serviceOrder/addPharmacyItemtoChargeForIP` | BR-BILL-001 | T_BILL, T_CHARGES | bill search | - | - | 5 |
| M04 | Bill list | Bill list (search/filter) | `billing:billingListFlow`, `billing:combineBillingListFlow`; `getAllBills-schema.json`, `billSummarySchema.json` | POST `/billSummary/getAllBills`, `/billSummary/getAllCombineBills`, `/billSummary/getAllBillsForOnline` | - | T_BILL | bill search | - | - | 5 |
| M04 | Checkout | Finalize / checkout / interim bill | `billing:checkout`, `billing:finalizeBill`, `billing:interimBill`, `billing:checkoutForHealthConsumption`; `draftSummaryBillSchema.json` | `bill.finalize*`; `billSummary/draftBillSummary` | BR-BILL-006 (settleâ‡”balancesâ‰¤0, VisitStatus=11) | T_BILL, T_Encounter | encounter (isCheckout) | - | - | 5 / 2 |
| M04 | Receipts | Bill receipts (OP/IP/pharmacy) | `billing:OPDbillReceipt`, `billing:IPDbillReceipt`, `billing:receiptList`; `billReceiptSchema.json`, `iPDBillReceiptSchema.json` | POST `/billReceipt/` (getBillReceipt), `/billReceipt/ipdBillReceipt`, `/billReceipt/billReceiptWithPharmacy`, GET `/billReceipt/downloadFile`, POST `/billReceipt/setIsBillReceiptPrintedToTrue` | - | T_RECEIPT, T_BILL | - | - | Print server (ws://) | 6 Payments & Refunds |
| M04 | Payment | Bill payment (cash/card/cheque/online/company) | `billing:addPayment`; `paymentDetailSchema.json`, `opBillPaymentSchema.json`, `ipBillPaymentSchema.json` | POST `/addPayment/addBillPaymentNew`, PUT `/addPayment/updateCharges` | BR-BILL-006, BR-OPD-007; A6 (TransactionType=2) | T_PAYMENT, T_PAYMENTMODEDETAILS, T_BILL | encounter (checkout) | - | - | 6 |
| M04 | Payment | Partial payment | `billing:addPayment`; `partialPayBillSchema.json` | POST `/billSummary/getServiceListForPartialBillPayment`; `addPayment/addBillPaymentNew` | BR-BILL-006 | T_PAYMENT, T_BILL | - | - | - | 6 |
| M04 | Advance | Advance collection | `billing:advanceReceipt`; `addAdvanceSchema.json`, `advanceSchema.json` | POST `/addAdvance/addAdvance`; GET `/addAdvance/getAdvanceAgainstList`, `/addAdvance/getAdvancePaymentDetailList` | BR-BILL-007 (FIFO adjustment) | T_ADVANCE, T_PAYMENT | - | - | - | 6 |
| M04 | Advance | Advance receipt / adjustment on bill | `billing:advanceReceipt`, `advanceReceiptByReceiptNoSchema.json` | POST `/advancebillReceipt/getAdvanceReceiptByReceiptNo`, `/advancebillReceipt/printAdvanceReceipt` | BR-BILL-007 (payment-mode 7, CIMS_ADDPAYMENTADVANCEDETAILS) | T_ADVANCE, T_PAYMENTMODEDETAILS | - | - | - | 6 |
| M04 | Refund | Advance refund | `billing:advanceRefund`; `advance_refund.json`, `return_advance.json` | POST `/addRefund/refund` | BR-BILL-008 (TransactionType=3, BEXAR002) | T_REFUND, T_ADVANCE | - | - | - | 6 |
| M04 | Refund | Bill refund / receipt | `billing:addPayment` (showRefund); `refundDetailSchema.json`, `refund_recieptSchema.json` | POST `/cancelBill/refundReceiptByIds`, POST `/refundBillReceipt/*`, POST `/billReceipt/refundReciept`, `/billReceipt/advanceRefundReciept` | BR-BILL-008 (TransactionType=4) | T_REFUND | - | - | - | 6 |
| M04 | Cancellation | Bill cancellation (settled/unsettled) | `cancelOpBillSummarySchema.json`, `cancelIpBillSummarySchema.json` | POST `/cancelBill/` (billCancel), `/cancelBill/printCancelBillAudit` | BR-BILL-008 (restore from *BeforeFinalization); A3 (IsCancelled) | T_BILL, T_CHARGES, T_ITEMSALE, T_DOCTORSHARE | - | - | - | 5 / 6 (CIMS_SETTLEDBILLCANCELLATION / CIMS_UNSETTLEDBILLCANCELLATION) |
| M04 | Credit note | Credit note (OP/IP/package refund) | `billing:creditNoteBillingListFlow`; `creditNoteSchema.json`, `getAllCreditNoteListSchema.json` | POST `/serviceOrder/saveCreditNote`, `/serviceOrder/addPackageRefund`, POST `/billReceipt/printCreditNoteForOP`, `/billReceipt/printCreditNoteForIP`, `/billReceipt/printPackageRefundReceipt` | - | T_CREDITNOTE(+Details), T_BILL | - | - | - | 5 |
| M04 | Estimation | Bill estimation (incl. surgery heads) | `billing:billEstimationFlow`, `billing:addPatientBillEstimation`; `getAllEstimationBillSchema.json`, `addOTBillEstimationSchema.json`, `billEstimation.vm` | `/patientbillestimation/*` (4) | BR-OT-002 (pre-billing) | T_PATIENTBILLESTIMATION*, PatientBillEstimationSurgeryHead | - | - | - | 5 |
| M04 | Sponsor | Sponsor change / company billing | `billing:showSponsor`, `billing:showSponsorDetails`; `changeSponsorSchema.json`, `sponsorSchema.json` | PUT `/serviceOrder/getSponsors`, GET `/ipdadmission/getSponsorByAdmissionID`, `/companyapprovedamount/*` (18) | BR-BILL-005 (co-pay/concession split) | T_PATIENTSPONSOR, T_SPONSOR, T_COMPANYAPPROVEDAMOUNT | - | - | NHCX (plans) | 5 |
| M04 | Package | Package subscription & health-plan consumption | `billing:packageSubscriptionFlow`, `billing:packageAppointmentList`, `billing:healthPlanPackage`, `billing:healthPlanPackageNhnc`; `addPackageSubscriptionSchema.json`, `packageListForBillSchema.json`, `healthplanConsumptionDetailSchema.json` | POST `/serviceOrder/allPackagesFastByGet`, `/serviceOrder/addHealthplanConsumptionDetail`, `/serviceOrder/addMultipleHealthplanConsumptionDetail`, `/serviceOrder/getSubscribedPackageCharges`, `/packageservice/*` (22) | BR-BILL-003 (package coverage) | T_PACKAGECHARGESUBSCRIPTION, HealthplanPackageConsumptionDetails | - | - | RGHS packages, NHCX | 5 / 23 |
| M04 | Doctor share | Doctor share add/deduct/list/bill/payment | `billing:doctorShare`, `billing:doctorShareAddDeduct`, `billing:doctorShareBill`, `billing:doctorBillList`; `doctorShareSchema.json` | `/doctor/addDoctorAdditionDeduction`, `/doctor/getDoctorShareList`, `/doctor/addDoctorBill`, `/doctor/addDoctorBillPayment`, `/doctor/updateDoctorShare` | BR-BILL-002 (CIMS_ADDDOCTORSHARE, skip OT) | T_DOCTORSHARE, T_DOCTORBILL, Doctorvisitcharge, Doctorwaiver | - | - | - | 5 |
| M04 | OT billing | OT billing | `billing:otBillFlow`; `addOTBillingSchema.json` | `otscheduler/addPatientProcdure` | BR-OT-002 (otprocedure.pre.billing) | T_BILL, T_CHARGES | - | - | - | 5 / 11 |
| M04 | Cross-service | Cross-service order billing | `billing:crossOrderService`; `crossServiceOrderSchema.json` | `serviceOrder.*` (crossOrder ops) | - | T_BILL | - | - | - | 5 |
| M04 | Reports | High-billing / billing reports | `high_billing_report.vm`, `bill_receipt.vm`, `ip_bill_summary.vm` | `reports/*` | - | - | - | - | - | 26 |

## M05 â€” Tariff, Packages, Health Plans, Companies & Services (Billing Masters)

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M05 | Service | Service master (+mapping) | `masters:servicemaster`, `masters:servicemasterparent`, `masters:servicemaster/rateedit`, `commons:allServiceMasterRef`, `masters:serviceMasterRef`; `serviceMasterSchema.json`, `servicemastermapping` | `/addServiceMaster/*` (6), `/servicemaster/*` (12) | BR-BILL-004 (tax) | M_SERVICEMASTER, M_SERVICEMASTER_NEW, T_TARIFFSERVICEMASTER | - | <masterName> master cache (@CrudCached) | - | 21 Reference Master Data |
| M05 | Tariff | Tariff & copay | `masters:tariffmaster`, `tariffratecopyutility` | `/tariff/*` (5) | BR-BILL-004 (up to 3 taxes), BR-BILL-005 (copay split) | TARIFFMASTER, Tariffservicemaster, Tariffcopaydetails, Tariffpackageclasswiserates | - | tariffMaster cache | - | 21 |
| M05 | Package | Package master (service/group/medicine/item-category) | `masters:packagemaster`, `masters:packages`; `packagemaster`, `packages` | `/packageservice/*` (22) | M05 BR (package class-wise rates) | T_PACKAGEMASTER, T_PACKAGESERVICE, Packagehealthplandefinationmaster | - | package cache | RGHS (getRghsPackageList) | 21 |
| M05 | Plans | Health plans / sub-plans (exclusions) | `masters:planmasterwrapper`; `planmaster`, `subPlanmaster` | `/planmaster/*` (3) | M05 BR (Planuncovereditemdetails, Planuncoveredservicedetails) | T_PLANMASTER, T_SUBPLANMASTER | - | Plan_master_full | NHCX (insurance_plan_on_request) | 21 |
| M05 | Company | Company master & contracts | `masters:companymasterwrapper`, `masters:companymaster`; `companymaster` | `/companyapprovedamount/*` (18) | - | T_COMPANYMASTER, Companyplandetails, Companytariffdetails | - | companyMaster cache | - | 21 |
| M05 | Concession | Concession templates | `masters:concessiontemplatewrapper`; `concessiontemplate` | `/concessiontemplate/*` (4) | BR-BILL-005 (concession tiers) | T_CONCESSIONTEMPLATEMASTER(+DETAILS), T_CONCESSION | - | - | - | 21 |
| M05 | Order sets | Order set definitions (consult/lab/rad/nursing/procedure/consumables/misc) | `masters:ordersetdefinationmaster`, `nursing:orderingSet`; `ordersetdefinationmaster` | `serviceOrder.*` (order set apply) | - | T_ORDERSET* | - | - | - | 21 / 4 |
| M05 | Specialization | Services specialization / subspecialization | `masters:services`, `masters:specialization`; `services`, `specialization` | POST `/serviceOrder/getSpecializationForBill` | - | M_SPECIALIZATION, M_SUBSPECIALIZATION | - | - | - | 21 |
| M05 | Reports | Package details/consumption print | `packageDetailsPrint.vm`, `packageConsumptionDetailsPrint.vm` | - | - | - | - | - | - | 26 |

## M06 â€” Doctor Share & Incentives

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M06 | Share | Doctor share calculation/list | `billing:doctorShare`; `doctorShareSchema.json` | `/doctor/getDoctorShareList`, `/doctor/updateDoctorShare` | BR-BILL-002 | T_DOCTORSHARE, T_DOCTORBILL | - | - | - | 5 |
| M06 | Share | Doctor share add/deduct | `billing:doctorShareAddDeduct`; `doctorShareAddDeductSchema.json` | `/doctor/addDoctorAdditionDeduction` | BR-BILL-002 | T_DOCTORSHARE | - | - | - | 5 |
| M06 | Share | Doctor bills & payments | `billing:doctorShareBill`, `billing:doctorBillList`; `doctorShareBillSchema.json`, `doctorBillListSchema.json` | `/doctor/addDoctorBill`, `/doctor/addDoctorBillPayment` | BR-BILL-002 | T_DOCTORBILL | - | - | - | 5 |
| M06 | Charges | Doctor visit charges / waiver | `doctorvisitcharge`; `doctorvisitcharge` | `doctor.*` | - | T_DOCTORVISITCHARGES, T_DOCTORWAIVER | - | - | - | 5 |

## M07 â€” Pharmacy & Dispensing

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M07 | Prescription | Doctor prescription (add) | `pharmacy:addPrescription`, `nursing:docPrescriptionList`; `addPrescriptionSchema.json`, `addMedicationSchema.json` | `/prescriptionmaster/*` (5), `/prescription/*` (PrescriptionDetailsServiceImpl) | BR-PHAR-002 (closure on sale) | T_PRESCRIPTIONMASTER, T_PRESCRIPTIONDETAILS, PatientPrescptionNote | - | - | - | 4 Ordering & Prescription |
| M07 | Prescription | Nurse medication order | `pharmacy:medicationOrder`, `pharmacy:NursePrescriptionList`; `addMedicationSchema.json` (+FromNurse) | `/medication/*` (23) | - | T_PATIENTMEDICATION, T_PATIENTNONMEDORDERDETAILS | - | - | - | 4 |
| M07 | Prescription | Prescription list / scan & upload | `pharmacy:PresciptionList`, `pharmacy:prescriptionList`, `pharmacy:prescriptionUpload`; `prescriptionUploadSchema.json` | `pharmacy.prescription*` | - | T_PRESCRIPTIONMASTER | - | - | - | 4 / 9 |
| M07 | Dispense | IP order verification | `pharmacy:pharmacyOrderVerify`; `getPrescriptionDetailsSchema.json`, `dispensingtype` | `/pharmacy/*` (102: getMedications, printPatientDispenseDetails) | BR-PHAR-001, BR-PHAR-002 | T_PRESCRIPTIONDISPENSEDETAILS, T_ITEMSALE | item (searchAllItemsInSolr) | - | - | 9 Pharmacy & Dispensing |
| M07 | Dispense | IP order dispense (batch/expiry pick) | `pharmacy:pharmacyOrderDispense`, `pharmacy:pharmacyOrderDispenseD`; `itemSaleDetailSchema.json`, `batchRow.html` | `/pharmacy/*` (dispense); `/medication/dispenseItemSale` | BR-PHAR-001 (VisitStatus=12, MedicationServiceImpl:812); BR-PHAR-002 (CIMS_ADDITEMSALESDETAILS_NEW) | T_ITEMSALE, T_PRESCRIPTIONDISPENSEDETAILS, T_ITEMBATCHMASTER, T_Appointment | - | - | - | 9 |
| M07 | Dispense | OP pharmacy / counter sale | `pharmacy:opPharmacyFlow`, `pharmacy:pharmacyCountersale`, `pharmacy:pharmacyOp`; `opPharmacyAddPatient.json`, `pharmacyCounterSaleSchema.json`, `addPatientPrescMedSchema.json` | `/pharmacy.opDispense*`, `/pharmacy.pharmacyCounterSale*`; POST `/addPayment/itemSalePayment` | BR-PHAR-001 | T_ITEMSALE | item | - | - | 9 |
| M07 | Dispense | Cart-fill & ward pick lists | `pharmacy:cartFillFlow/:ward_id/:patient_dfn`, `pharmacy:phCartFillOrderDispense`; `cartFillDetails.html`, `cartFillPatientDetails.html` | `pharmacy.cartFill*`; `CIMS_GETPRESCRIPTIONWARDDETAILS*` | BR-PHAR-001 | T_ITEMSALE, ward picklist tables | - | - | - | 9 |
| M07 | Compound | IV pharmacy / compounding | `pharmacy:ivPharm`, `pharmacy:prepareCompound`; `bcmaIV.html`, `compoundDetailRow.html`, `compoundingLable.html` | `pharmacy.iv*`, `pharmacy.compound*` | BR-PHAR-002 (compound closed on sale) | T_INFUSIONMIXING(+DETAILS), T_ITEMCOMPOUNDDETAILS | - | - | - | 9 |
| M07 | Return | Item sale returns (IP/OP) | `pharmacy:itemSaleList`, `pharmacy:itemSaleReturnList`, `pharmacy:ipReturnFlow`, `pharmacy:iPReturnFlow`, `pharmacy:ipReturnPharmFlow`; `itemSaleReturnListSchema.json` | `pharmacy.itemSale*`, `pharmacy.iPReturnFlow*` | - | T_ITEMSALE, T_ITEMSTOCK | item | - | - | 9 |
| M07 | Billing | Pharmacy billing (OP/IP) | `pharmacy:pharmacyBilling`, `pharmacy:opIpPharmacyBilling`, `pharmacy:ipPharmacyBilling`; `opIpPharmacyBillSchema.json`, `ipPharmacyBillingSchema.json`, `pharmacyPaymentDetailSchema.json` | `pharmacy.bill*`; POST `/billReceipt/downloadBillPharmacyRec` | BR-BILL-001 (IsPharmacy=0/1) | T_BILL, T_ITEMSALE | - | - | - | 5 / 9 |
| M07 | Intervention | Pharmacy interventions | `pharmacy:pharmInterventionreport`; `pharmacyInterventionReport.json` | `pharmacy.intervention*` | - | - | - | - | - | 9 |
| M07 | Mapping | Drug mapping (drug â†” item) | `pharmacy:pharmacyDrugMapping`; `pharmacyDrugMapping` | `pharmacy.drugMapping*` | - | - | - | - | - | 9 / 21 |
| M07 | Home meds | IP home medications | `ipHomeMedsForm` | `pharmacy.*` | - | T_PATIENTMEDICATION | - | - | - | 4 |
| M07 | Store | Store/stock query for pharmacy | `pharmacy:pharmacyItemList` | GET `pharmacy/getItemAndItemBatchListByStoreId` | - | T_ITEMSTOCK | item | - | - | 9 / 10 |
| M07 | Print | Labels (med/compound) | `pharmacyMedLabel.vm`, `pharmacyReceipt.vm`, `printMedLabel` template | `pharmacy/printPatientDispenseDetails`, `pharmacy/printCompoundLabel` | - | - | - | - | QZ Tray | 26 |
| M07 | ABDM | Prescription to PHR | `patPrescriptionPHR.vm` | - | - | - | - | - | ABDM PHR | 25 Interop |

## M08 â€” Clinical / Service Ordering & Investigations

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M08 | Order | Service order (addServiceOrderDetails) | `op:consultOrdersList`, `billing:billingFlow` (order tab); `consultOrder_list-schema.json`, `serviceOrderDetail` template | POST `/patient/addServiceOrderDetails`, `/patient/getPatientEorders`, POST `/serviceOrder/addServicechargesNew` | BR-BILL-003 (patho/rad auto-create at billing), BR-PATH-001 (sample locks charge) | T_PATIENTEORDER, T_PATIENTADVICE, ConsultOrder, T_PENDINGCONSULTVISITS | - | - | HL7 outbound (order messages) | 4 |
| M08 | Order | Order sets application | `nursing:orderingSet`; `orderingSet`/`orderSet` templates, `addAdtOrder/addConsultOrder/addDietOrder/addLabOrder/addMedOrder/addNursingOrder/addProcedureOrder/addRadOrder.html` | `serviceOrder.add*Order*` | - | T_ORDERSET* | - | - | - | 4 |
| M08 | Investigation | Investigation list | `op:investigation`; `investigationSchema.json` | `/investigation/*` (3) | - | T_PATIENTEORDER | - | - | - | 4 |
| M08 | CWF | CWF/SCWF workflow mapping | `masters:cwf`, `masters:cwf/add`, `masters:scwf/add`; cwf schemas | `/cwf/*` (17) | - | CWF/SCWF masters | - | - | - | 21 / 4 |
| M08 | External | External order details (outsourced) | `billing:crossOrderService` | POST `/serviceOrder/addExternalOrderDetails`, POST `/serviceOrder/getExternalorders` | - | ExternalOrderDetails | - | - | EXT LIMS (state lab) | 4 |
| M08 | Status | Order/service status update | `pathology:updateOrderStatus`, `pathology:updateServiceStatus`; `pathoUpdateOrderStatus.json`, `serviceStatusSchema.json` | `pathoOrder.updateStatus*` | - | T_PATHORDERBOOKING, T_PATIENTEORDER | - | - | - | 4 / 7 |
| M08 | Machine | Machine config (KDL) | `masters:machineConfig`, `commons:machineConfiguration(List)`; `machineConfigSchema.json` | `/machineConfig/*` (3), GET `/patient/getResultsFromKDL` | - | T_MACHINECONFIGURATION | - | - | KDL instruments | 7 |

## M09 â€” Nursing (Worklist, Notes, Allocation, Care Plans)

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M09 | Worklist | Nurse worklist (patient/task views) | `nursing:roleNurse`, `nursing:roleNurseIncharge`; `showNurseWorklistSchema.json`, `roleNurseWorkList*.html` | `/nursingList/*` (300 endpoints â€” largest family) | - | T_NURSETASKDETAILS, T_NURSETASKFREQUENCYDETAILS | - | - | - | 12 Nursing & Inpatient Care |
| M09 | Coversheet | Patient coversheet | `nursing:roleNursePatientView`, `nursing:rolePatientCoversheet/:patientId/:admId/:patientIen/:visitIen`, `ipCoversheet/...`; `showPatientCoversheetSchema.json` | `nursingList.coversheet*`, `nursingList.getPatientEncounterById` | - | T_PATIENTVITALS, T_PATIENTMEDICATION, T_PATIENTNOTE | - | - | - | 12 |
| M09 | Notes | Nurse notes (initial/progress/procedure/addendum) | `nursing:rolePatientNote`, `nursing:patientNotes/:patientId/:admId/:patientIen/:visitIen/:noteIen/:noteTitleId`; `showNursePatientNoteSchema.json`, `addNursesNotes.html`, `notesTitle.json` | `nursingList.notes*`, `nursingList.sign*` (signNotesFromTask, signTemplate); `/notedetailsdata/*` (6) | - | T_PATIENTNOTE, NurseNote | - | - | TIU-XML forms engine | 4 / 12 |
| M09 | Care plan | Plan of care (NCP problems/goals) | `nursing:rolePlanofCare`, `nursing:showCarePlan/...`; `showNursePlanOfCareSchema.json`, `ncpSubProblems.html`, `carePlanDetailsConfirm.html` | `nursingList.ncp*`, `careplan.*` | - | T_CAREPLAN(+DETAILS) | - | - | - | 12 |
| M09 | Vitals | Vitals entry (qualifiers/ranges) | `vitalsConfiguration.json`, `chartVitalRangeSlider.html` | `nursingList.vitals*` | - | T_PATIENTVITALS | - | - | - | 12 |
| M09 | Charting | Charting / partograph / growth charts | `charting:chartingApp`, `charting:chartingExpand`, `growthchartdetails/add/:id`; `charts/ODCWF_*.json`, `partoGraph.html` | `nursingList.chartData*`, `nursingList.cwf*`, `nursingList.parto*` | - | T_PATIENTVITALS | - | - | - | 12 |
| M09 | Handover | Shift handover | `shifthandover:multipleShiftHandOff`; `patientDetailsForMultiShiftHandover.html` | `nursingList.handover*`; `nursingList/deleteNursePatientAllocationOnShiftHandOver` | - | - | - | - | - | 12 |
| M09 | Allocation | Nurseâ†’ward/doctor/patient, alliedâ†’clinic | `allocation:*` (11 routes); `nurseWardAllocationSchema.json`, `showNurseWardAllocationWrap.html` | `nursingList.allocation*` | - | - | - | - | - | 12 |
| M09 | Incident | Incident reporting & resolution | `nursing:incidentReporting`, `nursing:addnewIncident/:...`, `nursing:editIncident/:...`, `nursing:resolveIncident/:...`; `incidentReportSchema.json`, `incidentResolveSchema.json` | `nursingList.incident*` | - | - | - | - | - | 12 / 20 |
| M09 | Rostering | Nurse rostering / shifts | `nursing:roleNurseRostering`, `commons:shiftRoster`, `commons:addShiftDetails`, `commons:ipdRoster`; `shiftRoster-Schema.json`, `addShiftDetails-Schema.json`, `showIpdRosterSlotsSchema.json` | `master.shiftRoster*`, `addIpdRosterSlotsSchema` ops | - | T_USERDETAILS, T_USERHOSPITALAUTHORITY | - | - | - | 22 / 12 |
| M09 | Discharge prep | Nursing discharge view | `nursing:showDischarge`; `addExpectedDischargeDate.html` | `patientTransferDischarge.*` | BR-DIS-001 | T_AdviseDischarge | - | - | - | 3 / 12 |
| M09 | Transfer | Nursing bed transfer | `nursing:showTransfer`, `nursing:transfer`; `nursingBedTransfer.html` | `bedmanagement.transfer*` | A5 | T_TRANSFERREQUEST, M_BEDMASTER | - | - | - | 3 |
| M09 | Allied | Allied health (physio/OT/SLP) | `nursing:roleAlliedPatientView`; tiuxmls AH/AL forms | `nursingList.allied*` | - | - | - | - | tiuxmls forms engine | 12 |
| M09 | Mortuary | Mortuary view (nursing) | `nursing:showMortuary`, `commons:mortuaryDetailsList`, `commons:mortuaryBoxManagement`, `commons:mortuaryRelease`, `commons:mortuaryDischargeList`; `mortuarySchema.json` | `/patient/getMortuaryPatients` | BR-DIS-002 (death) | ChildDeathReview, T_PATIENTDISCHARGEDATA | patient (death partial update) | - | - | 3 |
| M09 | Annotation | Image annotation | `nursing:annotation`; `annotation-view.js` | `fileService.*` | - | Documents | - | - | markerjs2/painterro | 25 |
| M09 | Diagnosis | Complaint/diagnosis mapping | `nursing:compliantDiagnosisMap`, `nursing:DiagnosisTreatment`; `compliantDiagnosisMap` | `/mdm/*` (55) | - | M_DIAGNOSISMASTER | icd10cm (searchAllIcd10cmInSolr) | - | - | 21 / 12 |
| M09 | Temp Rx | Temp prescription approval | `pharmacy:pharmacyOrderVerify` (temp tab); `tempPrescriptionList.json` | `nursingList.*` temp Rx ops | - | T_PRESCRIPTIONMASTER | - | - | - | 4 |

## M10 â€” Diet & Kitchen

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M10 | Order | Patient diet order (IP) | `diet:DietKichenView`, `diet:dietSchedule`; `diet_foodorder_schema.json`, `addDiet.html`, `wardPatDetails.html` | `/diet/*` (30: addpatdietorder, updateDietFoodStatusForCancel) | - | T_DIETORDER, DietOrderDetails | - | - | - | 14 Diet |
| M10 | Menu | Diet item/session masters + menu | `diet:dietMenuItems`, `diet:dietScheduleStatus`; `diet_menu_item_schema.json`, `diet_schedule_schema.json` | `/diet/*` | - | T_DIETITEMSCHEDULEDETAILS, T_TODAYDIETITEMMENU, M_DIETITEMMASTER, M_DIETSESSIONMASTER, M_DIETTYPEMASTER | - | - | - | 14 |
| M10 | Kitchen | Kitchen production view | `diet:DietKichenView`; `show_diet_kichen_schema.json`, `dietKitchenViewDetailsList.html` | `/diet/*` | - | T_TODAYDIETITEMMENU | - | - | - | 14 |
| M10 | Report | Diet patient report | `getDietPatReport` (API) | `/diet/getDietPatReport` | - | - | - | - | - | 26 |

## M11 â€” CSSD (Central Sterile Supply Department)

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M11 | Kit master | Kit formation/definition | `cssd:kitMaster`, `cssd:kitFormulation`; `showCurrentKitMasterListSchema.json`, `addKitMaster.html` | POST `/cssd/addKitMaster`, `/cssd/updateKitMaster`, `/cssd/addUpdateKitItemDetails`, GET `/cssd/getKitMasterDetails`, `/cssd/isKitExists` | - | T_CSSDKITFORMATION(+DETAILS) | item (kit/compound flags) | - | - | 15 CSSD |
| M11 | Kit prep | Kit preparation list | `cssd:kitPreparation`; `showCurrentKitPreparationListSchema.json`, `addKitPreparation.html` | POST `/cssd/getCurrentKitPreparationList`, `/cssd/verifyMasterKitPreparation`, GET `/cssd/getKitDetailsForPreparation` | - | T_CSSDKITFORMATION | item | - | - | 15 |
| M11 | Sterilization | Sterilization with machine/quality test | `cssd:cssdItemKitSterilization`; `showCSSDItemKitSterilization(+Filter)`, `cssdItemKitSterilization.html` | POST `/cssd/addItemSterilizationDetails`, `/cssd/getCSSDItemSterilizeDetails` | - | T_CSSDITEMSTERILIZATIONDETAILS, M_STERILIZATIONMASTER, M_STERILIZATIONMACHINEMASTER, M_STERILIZATIONQUALITYTESTMASTER, M_STERILIZATIONEXPIRYMASTER | item | - | - | 15 |
| M11 | Issue | Procedure kit request/issue (OT) | `cssd:procedureCssdRequest`, `cssd:procedureCssdIssue`, `cssd:otKitItemRequest`; `procedureCssdKitRequestFilterSchema.json`, `showProcedureCssdIssue` | POST `/cssd/addKitRequestFromKitMaster`, `/cssd/getProcedureCSSDRequest`, `/cssd/getProcedureCSSDIssue`, `/cssd/updateCssdRequestforIssue`, `/cssd/cssdItemIssue`, `/cssd/freezeKitRequest`, `/cssd/updateStatusProcedureCSSDKitDetails` | - | T_PROCEDURECSSDREQUEST(+DETAILS), T_PROCEDURECSSDKITDETAILS, T_PROCEDURECSSDISSUE(+DETAIL) | - | - | - | 15 / 11 |
| M11 | Store | Store dispatch/receive + indent | `cssd:CSSDItemIssueNew`, `cssd:CSSDItemReceiveandReturn`, `cssd:CSSDItemReceiveReturnNew`, `cssd:cssdItemValidate`; `cssdRequisitionDetailsFormStoreDispatch.html` | GET `/cssd/updateCssdRequisitionFromStoreDispatch`, `/cssd/updateCssdRequisitionFromStoreReceive`, `/cssd/updateCssdRequisitionToStoreReceive`, `/cssd/updateCssdRequisitionForHold`, POST `/cssd/getIndentListforCssdStoreIdforItemIssue`, `/cssd/issueItemReceiveListForCSSD` | - | T_CSSDKITISSUEITEM(+DETAILS) | item | - | - | 15 / 10 |
| M11 | Report | CSSD report / kit requisition print | `cssd:showCssdReportList`; `cssdReport.vm`, `requisitionKitDetail.vm` | POST `/cssd/printKitRequisitionDetails`, GET `/cssd/getProcCSSDReqDetailsForReport` | - | - | - | - | - | 26 |

## M12 â€” Operation Theatre (OT) & Surgery

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M12 | Schedule | Surgery scheduling (slot/conflict checks) | `ot:surgSchedule`, `ot:otScheduleEvent`, `ot:addNewSurg`; `surgeryScheduleSchema.json`, `addNewSurgerySchema.json`, `otSchedular.html` | `/otscheduler/*` (80: addNewSurgery, checkDoctorAllreadyBookedForSameTime, checkOTAvailability) | BR-OT-001 (BEOT004) | T_OTSCHEDULE(+DETAILS), T_SURGERYDETAILS | - | - | - | 11 OT & Surgery |
| M12 | Schedule | OT theatre/table config + equipment booking | `masters:ottheatremaster`, `masters:ottablemaster`; `otconfig/*` (33) | `/otconfig/*`, `/otdetails/*` (3) | - | M_OTTHEATREMASTER, M_OTTABLEMASTER | - | - | - | 11 |
| M12 | Surgery | Surgery list (from nursing orders) | `ot:surgList`; `showSurgeryOrderList.json`, `SurgeryOrderList` | `otscheduler.list*` | BR-OT-002 (isOtProcessDone=false filter) | T_SURGERYDETAILS | - | - | - | 11 |
| M12 | Procedure | Procedure performance (checklist, anesthesia, equipment, staff) | `ot:procPerformance`, `ot:patMovementAudit`; `procedurePerformanceSchema.json`, `patientProcedureDetails.html`, `otCheckList.html` | `otscheduler.procedure*`; `/surgerydetails/*` (6) | - | T_PATIENTPROCEDUREANESTHESIADETAILS, T_PATIENTPROCEDURERATEDETAILS, T_PATIENTSCHEDULEANASTHESIADETAILS, T_PATIENTPROCEDURECHECKLISTITEMS(+DETAILS) | - | - | - | 11 |
| M12 | Billing | Surgery charges & OT billing estimation | `billing:otBillFlow`; `addOTCharge`, `addOTBillingSchema.json`, `surgeryheads` | `otscheduler/addPatientProcdure`, `/otconfig/getSurgeryHeadDetailsForBillEstimation`, `otscheduler/addSurgCharge` | BR-OT-002 (pre-billing) | T_SURGCHARGES, T_SURGERYHEADMASTER(+DETAILS) | - | - | - | 11 / 5 |
| M12 | Schedule | Nurse/doctor schedule events | `ot:nurseScheduleEvent`, `ot:itemDetailsEvent`; `nurseSchedular.html`, `itemDetailsForOT.html` | `otscheduler.nurse*`, `otconfig.item*` | - | T_OTSCHEDULE | - | - | - | 11 |
| M12 | Report | OT report / surgery schedule print | `surgeryScheduleListDetails.vm`, `OTReport.vm` | - | - | - | - | - | - | 26 |

## M13 â€” Inventory & Stores

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M13 | Item | Item master & categories | `inventory:itemMasterList`, `inventory:itemGroupRef`, `inventory:itemSummary`, `inventory:updateBatch`; `itemMasterSchema.json`, `addNewItemSchema.json`, `updateItemMaster.json` | `/itemmaster/*` (23), `/itemcategory/*` (1), POST `/inventoryStockList/addItemMaster`, `/inventoryStockList/addItemGroup` | - | M_ITEMMASTER, M_ITEMCATEGORY, M_ITEMGROUP, M_BRANDMASTER, M_MOLECULE, M_UNITOFMEASURE | item (syncItemsToSolrJob drainer) | itemMaster cache | e-Aushadhi item sync (updateEAushadhiItemMaster), Janta/MDM | 10 Inventory & Procurement |
| M13 | Stock | Stock list & batches | `inventory:itemStockList`, `inventory:stockList`, `inventory:holdItemBatches`; `stockListSchema.json`, `showCurrentStockListSchema.json` | POST `/inventoryStockList/getItemCurrentStockList`, `/inventoryStockList/getCurrentUnitItemStockList`, `/inventoryStockList/getBatchDetails`, `/inventoryStockList/freezItemBatchStatus` | - | T_ITEMSTOCK, T_ITEMBATCHMASTER | item | - | - | 10 |
| M13 | Indent | Indent (create â†’ freeze â†’ 2-level approve â†’ issue) | `inventory:indentListofStore`, `inventory:purchaseIndent`; `showIndentListSchema.json`, `addNewIndentSchema.json`, `showIndentDetailsSchema.json` + OnFreezed/OnApproved/Editable | POST `/inventoryStockList/addIndent`, `/inventoryStockList/freezIndentLists`, `/inventoryStockList/approveIndentLists`, GET `/inventoryStockList/getIndentDetailsById`, `/inventoryStockList/getIndentListByStoreId` | - | T_INDENT, T_INDENTDETAILS | - | INV_RI_<indentId> receive lock (ServiceLayerAspect:1192, 60s) | e-Aushadhi (eAushadiIndent/indent) | 10 |
| M13 | Issue | Item issue (indent/direct) | `inventory:issueItem`, `inventory:directIssueItem`, `inventory:itemReceiveList`; `showAllItemIssueListSchema.json`, `batchBarCodeForItemIssue.html` | POST `/inventoryStockList/getIndentListByStoreIdforItemIssue`, `/inventoryStockList/getIndentDetailsforItemIssue`, `/inventoryStockList/getIssueItemsDetailsById`, `/inventoryStockList/approvalForIndentIssue` | - | T_ITEMSTOCK, T_ITEMISSUE | item | - | - | 10 |
| M13 | Adjustment | Stock adjustment (with reason) | `inventory:stockAdjustmentList`, `inventory:StoreStockAdjustment`; `showStockAdjustmentListSchema.json`, `addItemsforStockAdjustment.html` | POST `/inventoryStockList/addItemStockAdjustmentDetails`, `/inventoryStockList/addReasonForStockAdjustment` | - | T_ITEMSTOCK, stock adjustment tables | item | - | - | 10 |
| M13 | Consumption | Material / department consumption | `inventory:consumeItemforStore`, `inventory:consumptionList`, `inventory:departmentalConsumption`, `inventory:departmentalConsumptionList`; `showStoreComsumptionListSchema.json`, `materialConsumptionDetails.json` | POST `/inventoryStockList/addItemforConsumption`, `/inventoryStockList/addExternalCosumptionStock`; `/materialconsumption/*` (3) | - | T_ITEMSTOCK | item | - | - | 10 |
| M13 | Enquiry | Item enquiry (stock across stores) | `inventory:itemEnquiry`; `itemEnquirySchema.json`, `getItemEnquirySchema.json` | GET `/inventoryStockList/getItemsEnquiryDetails`, `/inventoryStockList/getItemsEnquirySuppliers`, `/inventoryStockList/printItemEnquiryDetails`, POST `/inventoryStockList/addtemEnquiry` | - | - | item | - | - | 10 |
| M13 | Expiry | Expiry/damage return to supplier | `inventory:expiredItemList`, `inventory:expiredDamagedItemReturnIndent`, `inventory:expiryItemReturn`, `inventory:expiryItemReturnList`, `inventory:expiryItemReturnToSupplier`, `inventory:recallExpiredItem`, `inventory:expire`, `inventory:returnToSupplier`, `inventory:grnReturnList`, `inventory:returnItemRecieve`; `expiryItemReturnSchema.json`, `recallExpiredItemSchema.json` | `/expiryitem/*` (47), POST `/inventoryStockList/addGRNReturn`, `/inventoryStockList/cancelReturnIndent` | - | T_ITEMSTOCK, T_ITEMBATCHMASTER | item | - | e-Aushadhi (returnItemUpdate) | 10 |
| M13 | Reorder | Reorder lists | `inventory:reOrderList`; `getReorderListForDirectItemIssue.json`, `showCurrentReorderListSchema.json` | POST `/inventoryStockList/getItemCurrentReorderforDirectIssue`, `/inventoryStockList/getItemInfoforReorderIndent` | - | M_REORDERANDSAFETYMASTER | - | - | - | 10 |
| M13 | Opening balance | Opening balance entry | `inventory:opeiningBalance`; `addItemsforOpeningBal.html`, `currentOpeningBalanceList.json` | POST `/inventoryStockList/addOpeningBalItems`, `/inventoryStockList/completeOpeningBalItems`; `/openingbalance/*` (5) | - | T_ITEMSTOCK | - | - | e-Aushadhi (addOpeningBalanceForKaysonsStock) | 10 |
| M13 | Stock transfer | Stock transfer between stores | `inventory:stockTransfer`; `showReturnFlowStore.json` | `inventoryStockList.transfer*` | - | T_ITEMSTOCK | - | - | - | 10 |
| M13 | Asset | Asset register (indent-based) | `inventory:assetRegisterList`; `assetRegisterList.json`, `showIndentDetailsEditableonAssetSchema.json` | `inventoryStockList.asset*` | - | - | - | - | - | 10 / 18 |
| M13 | Custom orders | Custom process orders | `inventory:customOrderList`, `inventory:customProcessOrderDetails`; `customProcessOrder.json`, `customProcessItemDetails.json` | `inventoryStockList.customOrder*`, POST `/inventoryStockList/getPatientGeneralDetailsForCustomOrder` | - | - | - | - | - | 10 |
| M13 | Approvals | All approvals worklist | `inventory:allApproval`; `approvePOPage.html` | GET `/inventoryStockList/getAllApprovalCount`; `inventoryStockList.approve*` | - | - | - | - | - | 10 |
| M13 | Store | User store / stock location config | `inventory:userStore`, `inventory:stockLocation`; `userStore`, `stockLocation` | `master.userStore*`, GET `/inventoryStockList/getStoreMasterById/{id}` | - | T_STORE, T_STOREITEMCATEGORY | - | - | - | 10 / 21 |

## M14 â€” Procurement (Purchase Orders, Quotations, GRN)

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M14 | PR | Purchase request / consolidation | `inventory:purchaseRequest`, `inventory:consolidatePurchaseRequest`; `showPurchaseRequestSchema.json` | POST `/inventoryStockList/getHospitalWisePurchaseRequestList`, `/inventoryStockList/getItemWisePurchaseRequestList`, GET `/inventoryStockList/getPurchaseRequestDetailsById` | - | T_PURCHASEREQUEST(+DETAILS) | - | - | - | 10 |
| M14 | Quotation | Quotation & comparison | `inventory:quotation`; `getItemQuotation.json`, `compareQuotationSchema.json`, `addItemInQuotation.html` | `/quotation/*` (7), GET `/inventoryStockList/getQuotationList` | - | T_QUOTATION | - | - | - | 10 |
| M14 | PO | Purchase order (supplier/in-house/custom) | `inventory:currentPO`, `inventory:orderList`, `inventory:receivedpolist`, `inventory:poByHQ`, `inventory:poRaisedForOtherUnit`, `inventory:otherUnitPOList`, `inventory:raiseDirectPO`; `showCurrentPurchaseOrderListSchema.json`, `supplierPoSchema.json`, `customProcessOrder.json` | POST `/inventoryStockList/addAddPOOrder`, `/inventoryStockList/addAddPOIndent`, `/inventoryStockList/freezePurchaseOrder`, GET `/inventoryStockList/getItemCurrentPurchaseOrderList`, `/inventoryStockList/getPoStatusCounts` | - | T_PURCHASEORDER, T_PURCHASEORDERREQUEST, PurchaseOrderDetails | - | - | RMRS (showRMRSPurchaseOrderRequestListSchema) | 10 |
| M14 | PO | PO approval | `inventory:allApproval`; `approvePOPageSchema.json`, `approvePOPage.html` | POST `/inventoryStockList/freezePurchaseOrder`, `/inventoryStockList/approveIndentLists` | - | T_PURCHASEORDER | - | - | - | 10 |
| M14 | GRN | GRN receipt + QC | `inventory:grn`, `inventory:gRNList`, `inventory:approveGrn`, `inventory:multipurposeGRN`, `inventory:consignmentGRNList`, `inventory:itemReceiveList`, `inventory:QcStatusUpdate`; `showGRNSchema.json`, `showApproveGrnListSchema.json`, `qcStatusUpdate.json`, `addMultipurposeGRN.html` | POST `/inventoryStockList/addGRN`, `/inventoryStockList/addProvisionalGRN`, `/inventoryStockList/approveProvisionalGRN`, `/inventoryStockList/approveProvisionalGRNNew`, `/inventoryStockList/addConsignmentGRN`, `/inventoryStockList/convertConsignedGRNToGRN`, GET `/inventoryStockList/getGRNListForSearch`, `/inventoryStockList/getApprovGRNListForSearch` | - | T_GRN*, T_ITEMSTOCK | item | - | e-Aushadhi (updateQCStatus) | 10 |
| M14 | Rate contract | Rate contracts | `masters:RateContract`; `rateContractSchema.json` | GET `/inventoryStockList/getRateContractList`, `/inventoryStockList/getRateContractByContractId`, POST `/inventoryStockList/addRateContract`, `/inventoryStockList/addNewItemToRateContract` | - | T_RATECONTRACT(+DETAILS/UNITS/VIEW) | - | - | - | 10 |
| M14 | Invoice | Indent invoice (supplier billing) | `indentInvoiceSchema` | POST `/inventoryStockList/addIndentInvoiceDetails` | - | T_INDENTINVOICE(+DETAILS) | - | - | - | 10 |
| M14 | PO print | PO/quotation/GRN prints | `poDraft.vm`, `poDraft_New.vm`, `quotationDetails.vm`, `eqpGrnDetails(+_GST).vm`, `grnBarCode.vm`, `invGrnDetails.vm` | - | - | - | - | - | QZ Tray | 26 |

## M15 â€” Pathology / Laboratory

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M15 | Booking | Lab order booking | `pathology:lab`, `pathology:LabN`, `pathology:selectedOrderType`; `pathoSchema.json`, `addServiceSchema.json` | `/pathoOrder/*` (133) | BR-BILL-003 (auto order from billing) | T_PATHORDERBOOKING(+DETAILS) | - | - | - | 7 Lab (Pathology) |
| M15 | Collection | Sample collection + barcode | `pathology:sampleCollection/:opdipd/:mrnNo/:catg`, `pathology:sampleGenerateBarcode`; `sampleCollectSchema.json`, `pathoGenerateBarCodeSchema.json`, `printSingleBarCode.html` | `pathoOrder.*` (sample save, barcode) | BR-PATH-001 (sample locks charge removal, LEX001) | T_PATHORDERBOOKINGDETAILS | - | - | - | 7 |
| M15 | Accept/Reject | Sample acceptance/rejection | `pathology:sampleAcceptReject/...`; `acceptRejectSchema.json`, `sampleRejectionSchema.json` | `pathoOrder.acceptReject*` | BR-PATH-001 | T_PATHORDERBOOKINGDETAILS | - | - | - | 7 |
| M15 | Result | Result entry (resulting per parameter) | `pathology:sampleResult/...`; `pathoResultSchema.json`, `resultingDetailSchema.json`, `parameterResult.html`, `microbiology-schema.json` | `pathoOrder.result*`, `pathoOrder.pathoResultTemplate*` | - | T_PATHOPATIENTPARAMETERDETAILS | - | - | KDL machine (getResultsFromKDL), eUpkaran | 7 |
| M15 | Review | Result review | `pathology:sampleResultReview/...`; `resultReviewSchema.json` | `pathoOrder.resultReview*` | - | T_PATHOPATIENTPARAMETERDETAILS | - | - | - | 7 |
| M15 | Release | Report release (double verification) | `pathology:sampleReportRelease/...`; `reportReleaseSchema.json`, `firstVerificationSchema.json`, `secondVerificationSchema.json` | `pathoOrder.reportRelease*` | - | T_PATHORDERBOOKING | - | - | WhatsApp (sendCategoryWiseReportOnWhatsApp) | 7 |
| M15 | Critical | Critical alerts | `pathology:sampleCriticalAlert/...`; `criticalAlertResultSchema.json` | `pathoOrder.criticalAlert*` | - | - | - | - | WhatsApp/SMS (eSanchar) | 7 |
| M15 | Machine | Machine-wise review / KDL verify | `pathology:sampleMachineWiseResult`, `pathology:kdlVerify`; `machineWiseResultReviewSchema.json`, `pathoKdlVerify.json` | `pathoOrder.machineWise*` | - | T_MACHINECONFIGURATION | - | - | KDL, Backman DxH 500 (OLIS jar) | 7 |
| M15 | Workload | Lab workload | `pathology:workLoadList`; `showLabLoadListSchema.json`, `labloadList.vm` | `pathoOrder.labLoad*` | - | - | - | - | - | 7 |
| M15 | Status | Test status / order status | `pathology:showStatus`, `pathology:updateOrderStatus`, `pathology:updateServiceStatus`; `pathoShowTestStatusNew.json`, `pathoUpdateOrderStatus.json`, `serviceStatusSchema.json` | `pathoOrder.testStatus*`, `pathoOrder.updateStatus*` | - | T_PATHORDERBOOKING | - | - | - | 7 |
| M15 | Masters | Test/profile/parameter/template masters | `masters:pathotestmasterwrapper`, `masters:pathoparametermasterwrapper`, `masters:pathotestmaster`, `masters:pathoparametermaster`, `masters:pathoprofilemaster`, `masters:pathotemplatemaster`, `masters:pathorganismmaster`, `masters:machinetestmaster`; `groupingAndTest` | `/pathotestmaster`, `/pathoparameterunits`, `/pathoprofilemaster`, `/pathoOrder.grouping*` | - | M_PATHOTESTMASTER, M_PATHOPARAMETERMASTER, M_PATHOPROFILEMASTER, M_PATHOTEMPLATEMASTER, M_PATHOSAMPLEMASTER, M_MACHINEMASTER, M_PATHANTIBIOTICMASTER | - | - | - | 21 / 7 |
| M15 | COVID | COVID lab list | `pathology:showCovidLabList`; `pathoShowCovidStatus.json` | `pathoOrder.covid*` | - | - | - | - | ABHA/ABDM (abha links) | 7 |
| M15 | Print | Report prints | `pathology:printAllReport`, `pathology:showLabResult`; `showAllLabReport.html`, `pathoAllResult.vm`, `pathoMicroReport.vm` | `pathoOrder.printReports*` | - | - | - | - | BrowserPrint | 26 |

## M16 â€” Radiology

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M16 | Booking | Rad order booking/queue | `radiology:radlab(/:p_id/:order_no/:date/:opd_ipd)`; `radOrderBookingSchema.json`, `radFilterSchema.json`, `patientFilterRadSchema.json` | `/radiology/*` (10), `pathoOrder.rad*` | BR-BILL-003 (rad auto-order) | T_RADORDERBOOKING(+DETAILS) | - | - | PACS (addRadImg, showRadSampleImg) | 8 Radiology |
| M16 | Result | Report entry (templates) | `radiology:printReport` (entry); `radReportSchema.json`, `radReportDetailsSchema.json`, `radioFinalizeTemplate.html`, `pathoRadioTemplate.html` | `pathoOrder.radReport*` | - | T_RADORDERBOOKINGDETAILS | - | - | - | 8 |
| M16 | Reschedule | Reschedule / retest | `radiology:reSchedule`, `radiology:radReTest`; `radRescheduleSchema.json`, `radRetestSchema.json` | `radiology.*` | - | T_RADORDERBOOKING | - | - | - | 8 |
| M16 | Print | Radiology report print | `radiology:printReport`; `showRadiologyReport.html` | `pathoOrder.radPrint*` | - | - | - | - | BrowserPrint | 26 |

## M17 â€” Blood Bank

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M17 | Donor | Donor registration (voluntary/camp/replacement/autologous) | `bloodBank:donorRegistration`, `bloodBank:campDonation`; `donorRegistartion`, `VoluntaryDonationSchema.json`, `CampDonationSchema.json`, `ReplacementDonationSchema.json`, `AutologousDonationSchema.json` | `/bloodtestmaster/*` (113) | BR-BLD-005 (donor auto-cancel 72h); A8 (donationStatus 1..11) | T_BLOODDONORREGISTRATION(+DETAILS), T_DONORSCREENINGTEST, T_DONORMEDICALEXAMINATIONRESULT, T_BLOODDONATIONCAMPMASTER | patient (BloodBankServiceImpl:1429-1625) | - | e-Raktkosh (eraktkoshdataupdateservice) | 13 Blood Bank |
| M17 | Donor | Donor list / replacement history | `bloodBank:donorList`, `bloodBank:replacementDonorHistory`; `showDonorListSchema.json`, `replacementDonorHistory.json` | `bloodtestmaster.donor*` | - | T_BLOODDONORREGISTRATION | - | - | - | 13 |
| M17 | Collection | Blood collection & component separation | `bloodBank:bloodBagComponentSeperation`, `bloodBank:bloodBagComponentExtraction`, `bloodBank:addNewBloodComponent`, `bloodBank:bloodComponent`; `bloodComponentSeperationSchema.json`, `bloodComponents.json` | `bloodtestmaster.component*` | - | T_BLOODBAGCOLLECTION, T_BLOODBAGEXTRACTION | - | - | - | 13 |
| M17 | Requisition | Patient/external requisitions | `bloodBank:patientRequisition`, `bloodBank:patientRequisition/extnlhosp`, `bloodBank:campRequisition`, `bloodBank:externalPatientRequisition/add`; `patientRequisitionList.json`, `patientRequisitionDetails.json`, `ExternalPatientRequisitionSchema.json` | `/patientrequisition/*` (10) | BR-BLD-001 (status machine 1/2/3/10), BR-BLD-004 (requisition expiry) | T_BLOODREQUISITION, T_PATIENTREQUISITION | encounter (PatientRequisitionServiceImpl:746-854) | - | external hospitals/storage centers | 13 |
| M17 | Cross-match | Cross-matching & issue | `bloodBank:bloodCrossMatching`, `bloodBank:crossMatchingList`, `bloodBank:newCrossMatchingTest`, `bloodBank:crossMatchedBags`, `bloodBank:issueBloodBag`; `bloodCrossMatchingListSchema.json`, `crossMatchingForm.json`, `bloodCrossMatchingRecords.html` | `bloodtestmaster.crossMatching*`, `bloodtestmaster.issue*` | BR-BLD-001, BR-BLD-002 (auto-decross 48h, ApplicationJobs:1045) | T_BLOODCROSSMATCHINGRESULT(+DETAILS), T_BLOODBAGISSUE, T_BLOODBAGISSUEDEMANDSLIP | - | - | - | 13 |
| M17 | Bag | Bag stock / trail / return / discard | `bloodBank:bloodBagTrail`, `bloodBank:bagReturns`, `bloodBank:bloodBagDiscard`, `bloodBank:BloodDiscardList`, `bloodBank:retrunBloodbag`, `bloodBank:bloodBagOpeningStock`, `bloodBank:showIndentList`, `bloodBank:BloodBanKReOrder`; `bagReturnDetailsSchema.json`, `showBloodDiscardListSchema.json`, `addBloodBagOpeningStock.json` | `bloodtestmaster.bag*`, `bloodtestmaster.openingStock*`, `bloodtestmaster.stock*` | BR-BLD-003 (bag expiry 23:00 job) | T_BLOODBAGRETURNS, T_BLOODBAGDECROSS | - | - | - | 13 |
| M17 | Indent | Blood bank indent (between units) | `bloodBank:raiseIndent`; `showBloodBankIndentListSchema.json` | `bloodtestmaster.indent*` (issueIndent/receiveIndent/rejectIndent/cancelIndent) | - | T_BLOODBANKINDENT(+DETAILS) | - | - | - | 13 |
| M17 | Cross-unit | Cross-unit orders / district exchange | `bloodBank:crossUnitOrder`, `bloodBank:crossUnitOrdersList`; `crossUnitOrderSchema.json` | `bloodtestmaster.crossUnit*` | - | - | - | - | district blood-bank stock exchange | 13 |
| M17 | TTD | TTD / red cell serology | `bloodBank:transfusionTransmittedDiseases`, `bloodBank:redCellSerology` | `bloodtestmaster.ttd*` | - | - | - | - | - | 13 |
| M17 | e-Raktkosh | e-Raktkosh data update / request | `bloodBank:eraktkoshRequest`, `masters:getEraktkoshCompMap`; `eraktkoshRequest` | `eraktkosh*`, GET `/bloodtestmaster/eraktkoshdataupdateservice` | - | M_ERAKTKOSHCOMPONENTMAPPING | - | - | e-Raktkosh (eRaktkosht.url) | 13 / 25 |
| M17 | Report | Blood bank reports (17 routes) | `reports:bloodBankReport`, `reports:donorSerologyTestReport`, `reports:crossMatchRegisterReport`, `reports:dailyBloodstockBookReport` (etc., 17) | `reports/*` (16 blood-bank report routes) | - | - | - | - | - | 26 |

## M18 â€” Biomedical Waste (BMW)

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M18 | Waste | Waste request/status (per admission) | `biomedicalwaste:showBioMedicalWasteStatus`; `showBioMedicalWasteStatusSchema.json`, `createBioMedicalRequest.html` | `/bioMedicalWasteManagement/*` (14: addNewBioRequest, getbioMedicalWasteStatus, updateBagStatus) | - | T_BIOMEDICALWASTE(+DETAILS), M_WASTEBAGMASTER | - | - | - | 19 Bio-Medical Waste |
| M18 | Waste | Bag barcode / handover to firm | `generateBioWasteBagBarcode.html`, `receiveBioRequestDetails.html`, `decomposeBioWaste.html` | GET `/bioMedicalWasteManagement/validateBarcode`, `/bioMedicalWasteManagement/handOverBags`, `/bioMedicalWasteManagement/handedOverToFirm`, `/bioMedicalWasteManagement/getBagWeightByBarcode`; POST `/bioMedicalWasteManagement/receiveBioRequestMainStore` | - | T_BIOMEDICALWASTE | - | - | - | 19 |
| M18 | Report | Bio-waste report | `reports:showBioWasteReport`; `showBioWasteReportSchema.json` | `reports/*` | - | - | - | - | - | 26 |

## M19 â€” Linen & Laundry

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M19 | Request | Linen request + status | `linenlaundry:showLinenLaundryStatus`; `showCurrentLinenStatusSchema.json`, `createLinenRequest.html` | `/linenLaundry/*` (14) | - | T_LINENREQUEST(+DETAILS), M_LINENCATEGORY, M_LINENDETAIL | - | - | - | 16 Linen & Laundry |
| M19 | Report | Linen laundry report | `reports:showLinenLaundryReport` (cssd module), `linenLaundryReport.vm` | `reports/*` | - | - | - | - | - | 26 |

## M20 â€” Equipment & Maintenance

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M20 | Asset | Asset register / verification | `equipments:assetDetails`, `equipments:assetVerification`, `equipments:equipmentUsageList`, `equipments:equipOpeningBal`; `getAssetDetailsSchema.json`, `assetVerificationList.html` | `/equipment/*` (51) | - | T_EQUIPMENT, T_EQUIPMENTDETAILDOCS, T_EQUIPMENTTRANSFER, T_EQUIPMENTUSAGEDETAILS, T_EQIUPOPENINGBALANCE | - | - | - | 18 Equipment Management |
| M20 | Complaint | Complaint & resolution | `equipments:complaintDetails`, `equipments:complaintResolutionList`; `addNewComplaintSchema.json`, `complaintDetailsSchema.json`, `complaintDetail.html`, `complaintResolutionView.html` | `/complaintdetails/*` (11), `/complaintresolutiondetail/*` (5) | - | T_EQPCOMPLAINT(+DETAILS), T_EQPCOMPLAINTRESOLUTION(+DETAILS) | - | - | - | 18 |
| M20 | PM | Preventive maintenance & calibration | `equipments:preventiveMaintenance`, `equipments:preventiveMaintenanceNew`, `equipments:preventiveMaintenanceSchedule`, `equipments:preventiveCalibrationCheckList`, `equipments:calibrationMaintenance`, `equipments:assetPreventiveList`; `getPreventiveMaintenanceListSchema.json`, `getCalibrationMaintenanceListSchema.json`, `addNewPrevetiveMaintncSchedu.html` | `/preventivemaintenance/*` (20), `/preventivemaintenanceschedule/*` (1) | - | T_EQPPM*(+DETAILS), T_EQPPREVENTIVEMAINTENANCE, M_EQUIPCHECKLIST, M_EQUIPMENTPREVENTIVECHECKLIST, M_FUNCTIONALCHECKLISTMASTER, M_ELECTRICALCHECKLISTMASTER | - | - | - | 18 |
| M20 | Warranty | AMC/warranty/insurance | `equipments:eqAmcWarrantyDetails`; `addEqpInsuranceSchema.json` | `equipment.*` | - | T_EQUIPMENTWARRANTY | - | - | - | 18 |
| M20 | GRN | Equipment GRN / PO | `equipments:equipmentGrn`, `equipments:eqpApproveGrn`, `equipments:eqpPurchaseOrderList`; `addNewGrnForEquipment.html`, `eqpGrnDetails(+_GST).vm` | `equipment.*`, `mdm.*` | - | T_EQUIPMENT, T_GRN | - | - | - | 18 / 10 |
| M20 | Scrap | Scrap sale / med-gas indent | `equipments:scrapSaleList`, `equipments:equipMedicalGasIndent`, `equipments:equipMedGasIndentIssue`; `getScrapSale`, `addEquipMedGasIndent.html` | `equipment.*` | - | - | item (medical-gas flags) | - | - | 18 / 10 |
| M20 | Report | Equipment usage report | `reports:showEquipmentUsageReport`; `showEquipmentUsageReportSchema.json` | `reports/*` | - | - | - | - | - | 26 |

## M21 â€” Transport & Ambulance

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M21 | Request | Patient/doctor transport request | `transport:patientTransportRequest`, `transport:doctorTransportRequest`; `patientTransportRequestSchema.json`, `doctorTransportRequestSchema.json` | `/transport/*` (31) | - | T_TRANSPORTREQUEST(+DETAILS) | - | - | Jeevanvahini (108 ambulance) | 17 Transport |
| M21 | Vehicle | Vehicle allocation & in/out status | `transport:vehicleIn`, `transport:vehicleOut`, `transport:vehicleStatus`, `transport:tripManagement`; `vehicleInSchema.json`, `vehicleOutSchema.json`, `vehicleStatusSchema.json`, `showTripRequestDetails` | `/transport/*` (allocateVehicle, vehicle in/out) | - | M_AMBULANCEMASTER, M_AMBULANCETYPEMASTER, M_DRIVERMASTER | - | - | - | 17 |
| M21 | Fuel | Fuel station details | `transport:fuelStationDetails`; `showFuelStationDetailSchema.json`, `addFuelStationDetails.html` | `/transport/*` | - | FuelStationDetails | - | - | - | 17 |
| M21 | Maintenance | Vehicle maintenance & approval | `transport:vehicleMaintenance`; `addVehicleMaintenanceRequest.html` | `/transport/*` (approve/complete maintenance) | - | T_VEHICLEMAINTENANCE, M_WORKSHOPMASTER | - | - | - | 17 |
| M21 | Report | Vehicle trip / fuel consumption reports | `transport:showVehicleTripReport`, `transport:showFuelConsumptionReport`; `transportReqReceipt.vm` | `reports/*` | - | - | - | - | - | 26 |

## M22 â€” Grievance

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M22 | Ticket | Raise grievance ticket | `grievance:grievanceRaise`; `raiseGrievanceTicketSchema.json`, `grievanceActionPopup.html` | `grievance.*` (custom service â€” endpoint map UNKNOWN in 04) | - | T_PATIENTGRIEVANCEHISTORY, T_PATIENTGRIEVANCECOMMENTS, GrievanceDocuments | - | - | - | 20 Grievance & Feedback |
| M22 | Ticket | Grievance list/status/severity/history | `grievance:grievancePage`; `grievanceSchema.json`, `grievanceHistorySchema.json`, `grievanceTicketStatusList.html`, `changeTicketStatus.html` | `grievance.*` | - | T_PATIENTGRIEVANCEHISTORY, T_PATIENTGRIEVANCECOMMENTS, ResolutionAsignee | - | - | - | 20 |
| M22 | Resolution | Resolution assignment & feedback | `assignedTicketToUser.html`, `grievanceFeedbackForm.html`; `grievanceFeedbackForm.html` | `grievance.*` | - | T_PATIENTGRIEVANCEFEEDBACK, ResolutionAsignee, M_GRIEVANCESEVERITYTEMPLATEDETAILS | - | - | - | 20 |

## M23 â€” RMRS (Financial/Fund Management)

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M23 | Authorities | Authority master (contracts, EMI) | `masters:authoritymaster`; `authoritymaster` | `/RMRS/*` (40) | - | M_AUTHORITYMASTER, M_HEADSMASTER, M_ATTENDANTPASSSERVICERATEMASTER, M_RMRSREASONMASTER, T_USERHOSPITALAUTHORITY | - | - | - | 24 RMRS & PMRD |
| M23 | Collection | Payment collection (add/approve/release) | `rmrs:addcollection/add`, `rmrs:addcollection/view`, `rmrs:showPaymentCollection`, `rmrs:issuePayment`; `AddCollection`, `PaymentCollection`, `IssueCollection` | `/RMRS/*` (addCollection, IssueCollection, approveOrRelease) | - | T_COLLECTIONHEADER, T_COLLECTIONDETAIL, T_RMRSCOLLECTION | - | - | Payment transfer (incentives) | 24 |
| M23 | Accounting | Journal entries / suspense / bank statement | `rmrs:suspenseAccountDetail`; `suspenseAccountDetailsSchema.json`, `JournalEntriesListDetail.html`, `bankStatement.html` | `/RMRS/*` (journal, bank statement) | - | T_SUSPENSEACCOUNTDETAILS, T_LEDGERMASTER, T_ACCOUNTING* | - | - | - | 24 |
| M23 | PO requests | RMRS PO requests | `rmrs:showRMRSPORequest`; `showRMRSPurchaseOrderRequestListSchema.json` | `/RMRS/*`; `/rmrs` (PO request lists) | - | T_PURCHASEORDERREQUEST | - | - | - | 24 / 10 |
| M23 | Reports | RMRS reports (plan/account-wise, document no.) | `rmrs:planAccountWiseReport`, `rmrs:summaryAccountWiseReport`, `rmrs:reportByDocumantNumber`, `rmrs:rmrsAccountWiseReport`; `printBankStatement.vm`, `ExpenditureReport.vm` | `/RMRS/*` | - | - | - | - | - | 26 |

## M24 â€” PMRD (Patient Medical Record Department)

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M24 | Records | Patient encounter record (IEN) | `pmrd:patientMedicalRecord`; `patientMedicalRecordWrap.html`, `patientEncounterRecord.html` | `/pmrd/*` (4: getAllPatientEncounter) | - | T_Encounter | encounter | - | - | 24 |
| M24 | Records | Medical record documents & uploads | `pmrd:patientMedicalRecord`; `uploadPatientDocumentsPmrd.html` | `/pmrd/getPatientDocumentsForPMRD`, `fileService.*` | - | Documents, PatientDocuments | - | - | - | 24 / 25 |
| M24 | Schemes | Form J1/J2/J3 (JSY) & h-form | `pmrd:patientMedicalRecord`; `formj1.html`, `formj2.html`, `formj3.html`, `hform.html`, `record:hform` | `/pmrd/getJsyFormDetails` | - | - | - | - | - | 24 / 26 |

## M25 â€” Reports & BI

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M25 | Reports | 46 report filter schemas (reg/admission/collection/statistics/blood bank etc.) | `reports:showServicewiseCollection`, `reports:showUserwiseCollectionList`, `reports:showAdmissionReportRisl`, `reports:showAppointmentReportRisl`, `reports:ipdStatisticalReport`, `reports:patientDeathReport`, `reports:ageWiseRegistrationReport` (+ 37 more, 44 routes) | `/reports/*` (47), `/helper/getAllDataForReport` (masterConfigService) | - | s_servicewisecollection, s_userwisecollection (aggregates) | - | - | - | 26 Reporting & Analytics |
| M25 | BI | BI dashboard (iframe embed) | `biReport:reports(/:rid)`, `biReport:reportdm(/:rid)`; `report_BI.html`, `report_BI_DM.html` | BI embed URL + token refresh | - | - | - | - | External BI tool (UNKNOWN which) | 26 |
| M25 | Janta | Janta clinic reports | `reports:jantaClinicReports`; `jantaClinicReports.html` | `reports/*` | - | - | - | - | - | 26 |

## M26 â€” Worklist & Task Management

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M26 | Tasks | Nurse task list (task/patient-wise) | `worklist:NurseTaskWorklist`, `worklist:NurseTaskWorklistNew`, `worklist:NursePatientWorklist`, `worklist:showPatientWorkList`, `worklist:carerWorkList`, `worklist:AlliedStaffTaskWorklist`; `nurseTaskList.json` | `/nursingList/*` (completeNursingOrderTask) | - | T_NURSETASKDETAILS, T_NURSETASKFREQUENCYDETAILS | - | - | - | 12 |
| M26 | Goals | Goals/interventions/meds/labs/rad tabs | `roleNurseWorkList(Home/AssignView/Goals/Intervention/Meds/Orders/PatientWise/TaskWise)` templates | `nursingList.*` | - | - | - | - | - | 12 |

## M27 â€” Queue Management (OPD & Nursing Desk)

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M27 | Queue | Appointment/cabin queue boards | `queue:appointmentQueue`, `queue:cabinQueue`; `appointmentQueueSchema.json`, `cabinQueueListSchema.json`, `cabinQueueListWrap.html`, `departmentQueue10.html` | `/queueManagment/*` (23: addToQueue, callGetPatientList) | - | T_PATIENTQUEUE, T_REGISTRATIONQUEUE | - | QMS room token queues (QMSUtil:28-126, <qName>_S counters) | - | 29 Queue & Kiosk |
| M27 | Token | Token management | `queue:manageToken`; `manageTokenSchema.json`, `token_sheet.vm` | `/queueManagment/*` (token ops) | - | T_PATIENTTOKEN | - | QMS queues | - | 29 |
| M27 | Doctor | Doctor batch slots / doctor queue | `queue:doctorBatch`, `queue:doctorQueue`; `doctorBatchSlotSchema.json` | `/queueManagment/*` | - | - | - | QMS queues | - | 29 |
| M27 | Nurse desk | Nursing desk for queue | `queue:nurseDesk`; `nursingDeskForQueue` | `/queueManagment/*`, `/nursingList/*` | - | T_PATIENTQUEUE | - | QMS queues | - | 29 / 12 |
| M27 | Call | Call patient / assign next doctor | `queue:callPatientList`, `queue:patientQueue`, `queue:userQueue`; `callGetPatientListSchema.json` | POST `/queueManagment/*` (assignNextPatientToDoctor), POST `/walkinVisit/callPatient`, `/walkinVisit/callPatientNew` | A1 (visit status 3 doctor called) | T_Encounter | encounter | QMS queues | - | 29 / 2 |
| M27 | Radiology/patho queues | Department queues (rad/patho) | `queue:radiologyQueue`, `queue:pathologyQueue` | `/queueManagment/*` | - | T_PATIENTQUEUE | - | QMS queues | - | 29 |

## M28 â€” Mortuary & Death

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M28 | Mortuary | Mortuary box management | `commons:mortuaryBoxManagement`; `mortuaryBoxManagement.json` | `/patient/*` (getMortuaryPatients) | - | - | - | - | - | 3 |
| M28 | Death | Death details capture (death form) | `death_form.vm`, `death_Certificate.vm`, `PatientDeathForm.vm`, `DeathReasonForm.vm` | `/patient/updatePatientDeathDetails`, `/patient/getPatientDetailsForDeathForm` | BR-DIS-002 (IsExpiredâ†’death) | T_REGISTRATION_DETAILS (isdeath), PatientDeathDetails | patient (death=true) | - | - | 3 |
| M28 | Death | Child death review (CDR 4A/4B) | `ip:cdrForm4A/4B` (death reports); `cdr_form_4A.vm`, `cdr_form_4B.vm` | - | - | T_CHILDDEATHREVIEW | - | - | - | 3 / 26 |
| M28 | Certification | Post-mortem report / death certificate print | `Post-MortomReport.vm`, `death_Certificate.vm`; tiuxml `Post_Mortem`, `Death_Certificate`, `DEATHNOTE` | `nursingList.*` (death cert) | - | - | - | - | tiuxmls | 3 / 26 |

## M29 â€” Chemotherapy

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M29 | Planning | Chemo patient list & planning | `chemotherapy:chemoPlanningList`, `chemotherapy:chemoPlanningFlow`; `showChemoPatientListSchema.json`, `showChemoPlanningSchema.json` | `pharmacy.chemo*`, `nursingList.chemo*` | - | - | - | - | - | 4 / 9 |
| M29 | Regimen | Regimen & cycle orders | `chemotherapy:chemoNewRegimen`, `chemotherapy:chemoPlanCycleFlow`; `chemoNewRegimenView.html`, `chemoCycleMedOrdCheck.html` | `pharmacy.chemo*` | - | - | - | - | - | 4 / 9 |
| M29 | Coversheet | Chemo patient coversheet | `showChemotherapyPatientCoversheetSchema.json`, `chemoPatientCoversheet.html` | `nursingList.chemo*` | - | - | - | - | chemo_tiuxml_mapping | 12 |

## M30 â€” BCMA (Bar-Coded Medication Administration)

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M30 | Admin | Bedside medication log (scan wristband) | `bcma:unitDose`, `bcma:scanWristband/:patientMrn`; `ShowMainScreenSchema`, `bcmaNavDueList.html`, `bcmaMedicationLogNew.html` | `medication.bcma*`, `pharmacy.bcma*` | - | T_PRESCRIPTIONDISPENSEDETAILS, IHR_MEDICATION_TMP | - | - | Barcode scanner (html5-qrcode), QZ labels | 9 / 12 |
| M30 | IVP | IVP tab / IV bag chronology | `bcmaIV.html`, `bcmaIVChronolgyActionOnBag.html`; `ShowIVPTab` | `pharmacy.bcma*` | - | T_INFUSIONMIXING(+DETAILS) | - | - | - | 9 |
| M30 | Exceptions | Unable-to-scan / held / missing dose | `ShowUnableToScanSchema`, `MissingDoseRequestSchema`, `iVPUnableToScanSchema`, `bcmaNavMedVarianceLog.html` | `medication.bcma*` | - | - | - | - | - | 9 |
| M30 | Stock | BCMA item stock view | `ShowBcmaItemStock` | `pharmacy.getMedications` | - | T_ITEMSTOCK | item | - | - | 9 / 10 |

## M31 â€” Kiosk (Self-Service)

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M31 | Registration | Self-registration | `kiosk:selfRegPatient`, `kiosk:kiosk`; `showSelfRegPatientSchema.json`, `registrationOpCard.html` | POST `/walkinVisit/selfPatientRegistation` | BR-OPD-002 | T_Encounter, T_REGISTRATION | encounter | - | e-Mitra | 29 |
| M31 | Appointment | Self-appointment | `kiosk:selfAppointment/...`; `selfAppointmentSchema.json`, `showDepartmentsApptSlots.html` | `appointmentschedule.*` | BR-OPD-002 | T_Appointment | - | - | e-Mitra | 29 |
| M31 | Search | Account/appointment search | `kiosk:searchAppointment/...`, `kiosk:accountSearchSchema`; `appointmentSearchSchema.json` | `patient.getGeneralDetailsForSearch`, `appointmentschedule.*` | - | T_Appointment | patient | - | - | 29 / 2 |
| M31 | Queue | Kiosk queue | `kiosk:kiosk` | `/patient/kioskQueue` | - | T_PATIENTQUEUE | - | QMS queues | - | 29 |
| M31 | Lab | Kiosk lab result upload (COVID) | `kiosk` (lab result flow) | `/patient/saveKioskLabResult`, `/patient/uploadKioskLabResult` | - | - | - | - | - | 29 / 7 |

## M32 â€” RCM (Revenue Cycle Management / Scheme Billing)

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M32 | Eligibility | RGHS eligibility check | `rghsEligibilityCheckSchema` (fjs-config_rcm); `rghsEligibilityDetail` | `/rcm/*` (7: rghsEligibilityCheck) | - | T_ELIGIBILITYCHECK | - | - | RGHS app (rghsapp.api.url), SIMS | 23 RCM & Claims |
| M32 | Approvals | DM approval / MOIC upload | `dmApprovalUploadSchema.json`, `moicUploadSchema.json` | `/rcm/*`, `/patient/*` (getRghsDocumentsforIHMS) | - | - | - | - | RGHS/SIMS (rghs.ihmsDocs.api.url) | 23 |
| M32 | Discharge | Discharge verification (scheme) | `dischargeVerificationSchema.json` | `/rcm/*` (dischargeVerification) | - | - | - | - | - | 23 |
| M32 | Chiranjeevi | Chiranjeevi app | `rcm:chiranjeeviapp`; `chiranjeeviapp.html` | `/rcm/*` | - | T_REFRENCETID, BENEFICIARY_ENROLMENT_IHMS | - | - | Chiranjeevi Yojana (chiranjivi.*) | 23 / 28 |
| M32 | Reports | RGHS TID / metadata reports | `rghsTID.vm`, `opdMetadataForRghs.vm`, `rghsSimsData.vm`, `chiranjeeviEmrTID.vm` | `/patient/getRghsTransactionsforIHMS`, `/patient/getSimsRghsforIHMS` | - | T_ADMISSION_TID | - | - | SIMS (rghsSimsData.api.url) | 23 / 25 |

## M33 â€” Payment Transfer

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M33 | Transfer | Payment transfer requests | `paymentTransfer:PaymentTransfer`; `paymentTransferListSchema.json` | `/paymentTransfer/*` (6) | - | T_PAYMENTTRANSFER, T_PAYMENTTRANSFERREQUEST(+DETAIL) | - | - | - | 6 / 28 |
| M33 | Incentive | Incentive claims | `paymentTransfer:incentiveClaimInfo`; `incentiveClaimInfoSchema.json` | `/paymentTransfer/*` | - | - | - | - | EJS incentives (ejs.asha.incentive.schedule) | 28 Govt Programs |
| M33 | OTP | OTP verification for transfer | `generateOTPSchema.json` | `/paymentTransfer/*` | - | - | - | - | - | 6 |

## M34 â€” File Management & Document Upload

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M34 | Upload | Document upload (patient/lab/grievance/equipment) | `fileManagement:dms`; `uploadDocumentSchema.json`, `uploadFileSchema.json`, `amendmentFileUpload.json`, `fileUploadForm.html` | `/fileService/*` (24), `/uploadFile/*` (4), `/upload/*` (1) | - | T_DOCUMENTS, T_PATIENTDOCUMENTS, PathoPatientDocs, GrievanceDocuments, EquipmentDetailDocs, PathOrderBookingDetailDocuments | - | - | MinIO (S3, disabled default â†’ filesystem), scan/print app | 25 (blob storage; no dedicated context in 10) |
| M34 | View | File view / annotated image | `fileManagement:dms`; `viewDocument.html`, `annotatedImageView.html` | `/fileService/*` | - | T_DOCUMENTS | - | - | markerjs2/painterro | 25 |
| M34 | Category | Document categories | `addDocumentCategory.html`, `filesAndDocumentsByCategory.html` | `/fileService/*` | - | T_DOCUMENTCATEGORY | - | - | - | 21 / 25 |

## M35 â€” User, Role & Notification Management

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M35 | Users | User add/edit & role assignment | `masters:addUser`, `masters:showUserList`; `addUserSchema.json`, `getUserDetailsSchema.json`, `updateUserSchema.json` | `/usermanagement/*` (18), `/addUserService/*` (55: saveUser, updateUser, getUserDetailsList, changeUserRole) | - | T_USERDETAILS, T_USERROLEMASTER, T_USERROLEMASTER (roles), M_STAFFMASTER, M_DOCTORMASTER | - | <token> session, UNQ_SESS_USER_<userId> | LDAP/SSO (risl.sso.*), Aadhaar | 22 Security & IAM |
| M35 | Users | User-store/ward/unit/building mapping | `selectStore`, `selectDefaultStore`, `commons:selectStore`; `userStore` schemas | `/addUserService/*` (getUserBuilding, changeUserBuilding, getUserSpeciality) | - | T_USERSTOREDETAILS, T_USERWARDDETAILS, T_USERBUILDINGDETAILS, T_USERUNITDETAILS | - | - | - | 22 |
| M35 | Permissions | Role-based permissions (module/menu) | `masters:allmasters` (permissions); `roleBasedNav.html` | `/addUserService/getPermissions`, `/addUserService/updatePermissions`; `master.getModuleWiseMasters` | - | T_MODULEPERMISSIONMASTER, T_MENUMASTER, T_MODULENAMEMASTER | - | - | - | 22 |
| M35 | Notifications | Email/SMS/WhatsApp templates | `commons:notificationTemplate`, `commons:notificationTemplates`; `notificationTemplateSchema.json`, `emailTemplate` | `/sendEmail/*` (5) | - | T_EMAILTEMPLATE(+DETAILS), T_NOTIFICATIONTEMPLATE | - | - | SMS/WhatsApp eSanchar (send.sms.url, whatsApp.*) | 25 Interop & Notifications |
| M35 | Auth | Login/logout/OTP/SSO/role switch | `commons:login1`, `commons:logout`, `commons:options`, `commons:forgotPassword`, `commons:resetPassword/:token/:unitId/:id`; auth schemas | POST `/auth/loginf`, `/auth/loginotp`, `/auth/loginb`, `/auth/loginh`, `/auth/loginJ`, `/auth/loginPatient`, `/auth/loginextf`, `/auth/loginExt`, `/auth/getuserrole`, `/auth/changeUserRole`, PUT `/auth/logout` | - | T_USERDETAILS, T_LOGIN_AUDIT | - | <token>â†’UserView (session.timeout TTL) | SSO (SSOAuthFilter), OTP, KDL login | 22 |

## M36 â€” EJS (e-Jan Swasthya / ASHA-ANM field app)

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M36 | ASHA | ASHA work data capture | `masters:ejanswathyaConfig`, `masters:ejs`; `ejschecklistquestions`, `ejsmasteractivity`, `ejsmasterentity` | `/ejs-asha/*` (8) | - | ANDROID_ASHA_WORK, ANDROID_ATTENDANCE, ANDROID_WORK_DETAILS, ES_CHECKLISTS(+SUBMISSIONS+QUESTIONS+QUES_ANSWERS) | - | - | - | 28 Govt Programs |
| M36 | ANC/PNC | ANC/PNC/child-care tracking | `ejs-ancpncchild` (masters screens) | `/ejs-ancpncchild/*` (20) | - | ANDROID_ANC, ANDROID_PNC, ANDROID_CHILD_CARE, ANDROID_CHILD_CARE_CHECKUP, ANDROID_FAMILY, ANDROID_MEMBERS | patient (outreach reg, EjsDetailsServiceImpl:2105) | - | - | 28 |
| M36 | Incentives | Incentive computation & claims | `ejs-incentive` | `/ejs-incentive/*` (6) | - | ANDROID_INCENTIVES | - | - | Payment transfer (M33) | 28 |
| M36 | Geography | Population registers & geo masters | `ejs-masterLocation`, `ejs-population` | `/ejs-masterLocation/*` (8), `/ejs-population/*` (9), `/ejs-details/*` (54) | - | MASTER_STATES/DISTRICTS/BLOCKS/VILLAGES/PHCS/AWCS/GRAMPANCHAYATS/ZONES, ANDROID_POPULATION, ANDROID_FAMILY_PLANNING | - | - | - | 28 |

## M37 â€” Aasmaan (Field App Backend)

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M37 | Cases | Case CRUD + checklists + monitoring | - (mobile app, no FAUG screens) | `/cases/*` (44: saveCase, saveCaseChecklist1-4, saveCaseMonitoring, saveCasePdMonitoring, saveCasePncMonitoring, saveCaseDeliveryNote, saveCaseDischargeNote, finish_admission, referral_finish) | - | AASMAANCASE* (35 convention-mapped tables, no @Table) | - | - | - | 28 |
| M37 | Devices | Device registration/assets/auth | - | `/devices/*` (8) | - | AASMAANDEVICE* | - | - | - | 28 |
| M37 | Institutions | Institution levels/referrals/shifts/feedback | - | `/institutions/*` (8), `/institution_levels/*` (2) | - | AASMAANINSTITUTION* | - | - | - | 28 |
| M37 | Ops | E-tickets, RSC call logs, stats, users, reports | - | `/e_tickets/*`, `/rsc_call_logs/*`, `/stats/*` (4), `/users/*` (6), `/reports/{report_id}`, `/app_transaction_log` | - | AASMAAN* | - | - | - | 28 |

## M38 â€” Integrations (ABDM/ABHA, NHCX/HCX, EHR, e-Aushadhi, Gov Schemes)

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M38 | ABDM | ABHA create/link/delink/consent/PHr | `commons:abdmQrGenerator`, `commons:consentRequest`, `commons:baalAbhaReKyc`, `op:abha`; `abhaProfile.html`, `abhaConsent.html` | `/abha/*` (50: generateOtp, verifyOtp, registerWithAadhar, registerWithAadharDemographics, registerABHAProfile, registerChildABHAProfile, linkHealthId, linkHid, delinkHid, raiseConsentRequest, getConsentRequestData, createPhrAddress, getCard, getProfileCard, startABHADeletion) | - | T_PATIENT_ABDM(+_CC, _SHARETOKEN), T_PATIENT_CONSENT_REQUESTS_ABDM(+_DATA), T_ABDMREQUESTQUEUE | patient (searchABHAPatientHIDInSolr) | ABDM_REQ_/RES_, ABDM_PAT_AUTH_, ABDM_HPR_AUTH_, ABDM_SES_TKN, HIP/HIU_ABDM_KEYS, <node>_ABDM_S/_F counters | ABDM gateway (v2+v3 callbacks: ABDMController, HFRController) | 25 / 1 |
| M38 | HPR | Health Professional Registry | `abdmQrGenerator` (HPR) | `/hpr/*` (13: registerHPRUserProfile, registerWithAadhar, loginUser, getCard, getProfile) | - | - | - | ABDM_HPR_AUTH_<userId> | ABDM HPR | 25 / 22 |
| M38 | NHCX | Insurance plan / eligibility / pre-auth / claims | `nhcx` screens: `checkEligibilityWarp.html`, `preAuthWrap.html`; `insurance_plan_on_request` template | `/nhcx/*` (10: insurancePlanRequest, coverageEligiblityRequest, preAuthSubmit, claimSubmitRequest, fetchPolicies, getAllParticipants, fetchPayorCerts) | - | T_NHCX_REQUEST(+_DETAILS), T_ELIGIBILITYCHECK | - | - | HCX (JWE, abdm.hcx.url) | 23 / 25 |
| M38 | EHR | EHR/EWD file generation & sync jobs | `commons:applicationModuleStatus` | `/ehr/*` (8) | - | T_EHRJOBDETAILS, T_EMRDETAILS, T_EMRSERVICES, T_OUTBOUNDHIS | - | - | EHR file dir (ehr.dir.path) | 25 |
| M38 | HL7 | HL7 inbound/outbound (ADT/ORM/ORU/DFT/REF/OMP) | `commons:inbound`, `commons:outbound`; `inbound.json`, `outbound.json` | Hl7InboundMessageJob (disabled); outbound via transaction | - | T_INBOUNDHIS, T_OUTBOUNDHIS | - | - | HL7 v2 (HAPI), HIS | 25 |
| M38 | e-Aushadhi | Indent/stock/QC/return sync | `inventory` (e-Aushadhi flags) | `/eAushadiIndent/*` (15: indent, indentIssueSucces, returnIndentForEaushadi, updateQCStatus, updateEAushadhiItemMaster, getKaysonsCurrentStockDetails) | - | T_INDENT, T_ITEMSTOCK, T_ITEMBATCHMASTER | item | - | e-Aushadhi RMS, Kaysons wholesaler | 10 / 25 |
| M38 | MDM | Janta master data management | `commons:showMDM`; `masters:itemSupplierRate` | `/mdm/*` (55) | - | - | item | - | Janta | 21 / 10 |
| M38 | FHIR | Embedded FHIR server (R4) | - (service-level) | FhirServer, PatientResourceProvider (HAPI) | - | - | - | - | FHIR R4 (NDHM) | 25 |
| M38 | Messaging | WhatsApp/SMS notifications queue | `notificationTemplates` | `/sendEmail/*`, `SendNotificationServiceImpl` | - | T_WHATSAPPMESSAGEQUEUE, T_WHATSAPPMESSAGESTATUS | - | - | WhatsApp Business API, eSanchar SMS | 25 |

## M39 â€” Commons / Platform Services

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M39 | Audit | Audit trails / audit reports | `commons:audits`, `commons:newAuditsReports`; `auditSchema.json`, `newAuditSchema.json` | `/audit.*` (AuditHelper raw INSERTs) | - | T_TRX_HISTORY, T_LOGIN_AUDIT, T_ABDM_AUDIT, T_APPOINTMENT_CHANNEL, M_AUDITEVENTMASTER | - | - | - | 26 |
| M39 | Config | Application config / unit config / module status | `masters:unitConfiguration`, `masters:applicationConfigRef`, `commons:applicationModuleStatus`; `configapplication` | `/masterConfigService/*` (72), `/helper/*` (3) | - | T_CONFIG_APPLICATION, T_CONFIGAUTOCHARGES, M_UNITMASTER | - | config_app cache | - | 21 / 22 |
| M39 | Hospital | Hospital creation / geo-tagging | `commons:hospitalCreation`, `commons:hospitalGeoTagging`; `hospitalCreationSchema.json`, `hospitalGeoTagging.json` | `/masterConfigService/*`, `/fileService/uploadHospitalLocation` | - | T_HOSPITAL*, geo tables | - | - | - | 21 |
| M39 | Machine | Machine configuration list | `commons:machineConfigurationList`; `machineConfigSchema.json` | `/machineConfig/*` | - | T_MACHINECONFIGURATION | - | - | - | 21 / 7 |
| M39 | MRN | MRN series config | `commons:patientRecord` (MRN), `mrnoseries` schema | `/masterConfigService/*` | BR-PAT-001 | M_MRNSERIES | - | - | - | 1 / 21 |
| M39 | Schemas | Schema designer / form designer | `tiuxmls:designer`, `masters:formdesigner`, `commons:addForm`, `commons:editSchema/:schemaName`; `schemasDesigner` | `masterConfigService.*` | - | - | - | - | - | 26 / 21 |
| M39 | Scans | Scan patient / checkin / validate token | `commons:scanPatient`, `commons:scanCheckin`, `commons:validateToken` | `markandaddvisit/checkin`, `walkinVisit.searchVisitbyToken` | - | T_PATIENTTOKEN | - | - | scan/print app | 29 |
| M39 | Questionnaires | Social-worker questionnaire | `questionaireSocialWorker` | `/questionnaireService/*` | - | - | - | - | - | 2 / 20 |

## M40 â€” TIU/Clinical Forms Engine (tiuxmls)

| Module | Submodule | Feature | Screen (route/schema) | Backend API (method+URL) | Business Rules (BR-*) | Tables | Solr | Redis | Integration | Target Service |
|---|---|---|---|---|---|---|---|---|---|---|
| M40 | Forms | 368 clinical forms (History/Exam/Assessment/Nursing/Risk scales/Checklists/Emergency/Discharge/Handover/Social work/Death/COVID) | `tiuxmls:designer`; 368 schema keys (Acuity2, Braden_Scale, Barthel_Index_New, NURSINGINITIALASSESSMENT_V*, OBSHIST_V11, PEDS_EXAMINATIONS_V2, HANDOFF_COMMUNICATION_V13, Post_Mortem_Report, Death_Certificate) | `tiuxml.*` save/load (note endpoints) | - | (stored via note/document tables â€” UNKNOWN) | - | - | tiuxml.js engine, tiuxml_mapping.json (gender/age mapping) | 4 / 12 |
| M40 | Designer | Form designer / versioning (V10-V15, _P, _B, _AH, _AL, _nhnc, _risl) | `tiuxmls:designer`; `formdesigner`, `noteTileConfig` | `masterConfigService.*` | - | - | - | - | - | 26 / 12 |
| M40 | Mapping | Specialty mapping (chemotherapy/nhnc/op variants) | `chemo_tiuxml_mapping.json`, `nhnc_tiuxml_mapping.json`, `op_tiuxml_mapping.json`, `tiuxmlMapping` master | `masterConfigService.*` | - | - | - | - | - | 21 |

---

## Cross-cutting notes

- **Status code dictionaries** (06 Â§A) apply across rows: visit status A1, bill A2, charge A3, admission A4, bed A5, payment A6, blood A7/A8, generic enums A9.
- **Solr**: 6 collections (patient, encounter, item, icd10cm, healthfactor live; service orphaned). Encounter index is a 3-day rolling window (07 Â§1.2). Solr MUST remain in target (07 Â§1).
- **Redis**: 100s `current_pat_/current_enc_` mirrors, ~375 `@CrudCached` master keys, session tokens, ABDM correlation, QMS queues, 60s tx locks (`CDD_` discharge, `INV_RI_` indent receive). Checked-in cache bean is MemoryCacheImpl â€” deployment risk (07 Â§2.2).
- **Stored procedures**: 719 artifacts (703 procedures: 646 CIMS_* +57 other +16 functions, 30 live-called), mapped per feature in TRACEABILITY-MATRIX; PG port plan in 05 Â§8.
- **Features with no evidence in a cell are marked `-`** â€” e.g. Grievance backend endpoint map (M22), Chemotherapy tables (M29), BCMA endpoints (M30), Aasmaan screens (M37).

*End of FEATURE-MATRIX â€” synthesized from docs 02/03/04/05/06/07/09/10 and agent-03/agent-04 discovery. Row count: see TRACEABILITY-MATRIX summary.*
