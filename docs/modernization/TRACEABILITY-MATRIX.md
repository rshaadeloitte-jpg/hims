# TRACEABILITY-MATRIX â€” End-to-End Feature Traceability (iHMS / OHUM PAS NG)

> **Synthesized artifact (Feature & Traceability Synthesizer).** Chain per major feature: Business capability â†’ Legacy screen â†’ FAUG schema â†’ JS function â†’ API â†’ Java controller/service â†’ Repository/DAO â†’ SQL/Stored proc â†’ Tables â†’ Integration â†’ Business rules â†’ Target React component â†’ Target microservice â†’ Target PG entity/table.
> Evidence cited from: `02-business-module-catalog.md`, `03-legacy-frontend-inventory.md`, `_discovery/agent-03/route-inventory.md`, `04-api-catalog.md`, `_discovery/agent-04/findings.md`, `06-business-rules-catalog.md`, `07-integrations-and-search.md`, `05-database-catalog.md`, `09-react-migration-design.md`, `10-target-architecture.md`.
> **UNKNOWN** = link not established in the source docs (never fabricated). Repository/DAO is uniformly `GenericDAO`/`GenericQuery` (05 Â§1) unless noted. API base = `/api/rest` (04 Â§0).

---

## F01 â€” Patient Registration (MRN Generation)

| Chain element | Evidence |
|---|---|
| Business capability | Create patient identity + demographics; assign MRN (permanent or temp); detect duplicates (M01) |
| Legacy screen | `op:appointmentFlow` (walk-in registration), `commons:patientList`/`commons:patientRecord`; schema `addpatientDetailRegdto-schema.json`, `walkinVisit-schema.json` (02 M01, 03 Â§2.1) |
| FAUG schema | `fjs-config_op.json` (`patientDetailReg`), `fjs-config_patientManagement.json` (02 M01 evidence) |
| JS function | `startAppointmentFlow`/`setWalkinObject` (appointment-module.js); `validatePatientDetails` (validation.js:1); `getDobFromAge`/`getAgeFromDob` (common-module.js:1380, :1653) (03 Â§2.1, Â§3) |
| API | POST `api/rest/patient/registerPatient` (04 findings: PatientServiceImpl 94 endpoints; 06 BR-PAT-001) |
| Java controller/service | `custom.PatientServiceHelper.registerPatient` (PatientServiceHelper.java:373-425, :661-669) (06 BR-PAT-001/002) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | `CIMS_ADDPATIENTREGISTRATION*` (10 Â§2.1 legacy sources); `cims_getlatestautocodeandincremetitbyone` for temp MRN (06 BR-PAT-001) |
| Tables | T_REGISTRATION, T_REGISTRATION_DETAILS, T_Registration_Int, M_MRNSeries (05 Â§3.6, 06 BR-PAT-001) |
| Integration | Solr `patient` collection realtime index (`indexPatientInSolr*`, SolrUtil.java:340) + Redis mirror `current_pat_<id/mrno>` 100s (07 Â§1.3/Â§2.1); ABDM (ABHA reg), Aadhaar/Janaadhar/Bhamashah (07 Â§3.1) |
| Business rules | BR-PAT-001 (MRN strategy), BR-PAT-002 (duplicate via Solr `findPatientByIdentity` â†’ BERA011; healthId â†’ BEXA040), BR-PAT-003 (familyId/memberId â†’ BERP007) |
| Target React component | `WalkinVisitPage` + `useWalkinVisit`/`usePatientLookup` (09 Â§2.1) |
| Target microservice | 1 Patient Identity & Registration (10 Â§2.1) |
| Target PG entity/table | T_REGISTRATION(+DETAILS), T_PATIENTSPONSOR, T_PATIENT_ABDM (10 Â§2.1 svc 1; 05 Â§8 mapping) |

## F02 â€” Patient Identity Search & Verification (Solr / Aadhaar / Janaadhar)

| Chain element | Evidence |
|---|---|
| Business capability | Multi-identifier patient lookup (MRN/mobile/Aadhaar/licence/Janaadhar/ABHA) + duplicate guard + Aadhaar OTP/biometric verify (M01) |
| Legacy screen | `walkinVisit-schema.json` search controlEvents (`.pat-mrnsearch-box/.pat-mobsearch-box/.pat-civilId-search/...`), `patient-search-list.html` (03 Â§2.1, 09 Â§3 PatientSearch) |
| FAUG schema | `walkinVisit-schema.json` (6,621 lines; min-length triggers 8/3/10/12/7/10/14/4) (09 Â§3) |
| JS function | `getPatientGeneralDetailsListForSearch` (common-module.js:14267, billing-module.js:13407); nursing variant (nursing-module.js:1231) (09 Â§3) |
| API | POST `api/rest/patient/getPatientGeneralDetailsListForSearch`; POST `api/rest/nursingList/getPatientGeneralDetailsListForSearchNursing`; POST `/walkinVisit/aadharOtpGeneration`, `/aadharOtpVarification`, `/aadharBiometricVarification` (03 Â§2.1, 04 Custom) |
| Java controller/service | `EmergencyAndWalkinVisitServiceImpl` (aadhaar endpoints); `BaseServiceImpl:259-304` identity lookup; `SolrUtil.searchPatientInSolr:340`, `searchPatientInSolrByIdentity:1061`, `searchExactPatientInSolr:1015` (07 Â§1.4, 06 BR-PAT-002) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | T_REGISTRATION, T_USER_FACEAUTH (05 Â§3.5) |
| Integration | Solr `patient` (edismax boosts mrno^18/mobile^16/janaadhar^17, Redis-first MRNO lookup `getPatientByMrno:1147`); Aadhaar OTP/biometric (07 Â§1.4, Â§3.1 #3); ABDM ABHA lookup (searchABHAPatientHIDInSolr:2150) |
| Business rules | BR-PAT-002 (Solr duplicate detection; BERA011/BEXA040); BR-OPD-002 (identity validation pre-visit) |
| Target React component | `C:PatientSearch` + `usePatientLookup` (09 Â§3) |
| Target microservice | 27 Search & Index (read), 1 Patient (write), 25 Interop (Aadhaar/ABDM) |
| Target PG entity/table | T_REGISTRATION family; Solr retained as external index (07 Â§1 â€” MUST remain) |

## F03 â€” ABHA / ABDM Patient Linking & Consent

| Chain element | Evidence |
|---|---|
| Business capability | ABHA create (Aadhaar/demographic), link/delink HID, PHR address, consent request/artefacts, patient queue (M38, M01) |
| Legacy screen | `commons:abdmQrGenerator`, `commons:consentRequest`, `commons:baalAbhaReKyc`; `op:abha`; `abhaProfile.html`, `abhaConsent.html`, `abhaPatientProfileList.html` (03 Â§2.12, 09 Â§2.1) |
| FAUG schema | abdm schemas in `fjs-config_commons.json` + abdm-module.js (198 KB) (03 Â§2.12) |
| JS function | abdm-module.js (198 KB; OTP flows) â†’ `useAbha` (09 Â§2.1) |
| API | GET/POST/PUT `/abha/*` (50: generateOtp, verifyOtp, registerWithAadhar, registerWithAadharDemographics, registerABHAProfile, registerChildABHAProfile, linkHealthId, linkHid, delinkHid, raiseConsentRequest, getConsentRequestData, createPhrAddress, getCard, getProfileCard, startABHADeletion); GET `/hpr/*` (13); ABDM callbacks `/{versionNumber}/...on_*` (ABDMController, 42; HFRController, 15) (04 Â§ABDM, 04 findings Â§2) |
| Java controller/service | `abdm.AbhaServiceImpl` (50), `abdm.HPRServiceImpl` (13), `CommonABDMUtil` (Redis correlation :350-394, :444-499), `xcg.abdm.server.ABDMController.java:160` (07 Â§3.1 #1) |
| Repository/DAO | GenericDAO/GenericQuery; `ABDMRequestQueue` DB queue with ISSEND/SYNCHRONIZED/NODE (07 Â§3.2) |
| SQL/Stored proc | - |
| Tables | T_PATIENT_ABDM(+_CC, _SHARETOKEN), T_PATIENT_CONSENT_REQUESTS_ABDM(+_DATA), T_ABDMREQUESTQUEUE, T_ABDM_AUDIT (05 Â§3.5, 07 Â§3.1) |
| Integration | Solr `patient` (searchABHAPatientHIDInSolr:2150,2218 â€” mobile+DOBÂ±1yr+Levenshteinâ‰¤3); Redis `ABDM_REQ_/ABDM_RES_`, `ABDM_PAT_AUTH_`, `ABDM_SES_TKN`, `HIP/HIU_ABDM_KEYS`, `ABHANUM_<healthIdNumber>`; ABDM gateway v2/v3 (07 Â§2.1, Â§3.2) |
| Business rules | BR-PAT-002 (healthId uniqueness â†’ BEXA040); A1 statuses (ABDM consent statuses) |
| Target React component | `AbhaPage` + `useAbha` (OTP flows), `AbhaCenterPage` (09 Â§2.1, Â§2.11) |
| Target microservice | 25 Interop & Notifications (ABDM), 1 Patient (ABHA linkage data) |
| Target PG entity/table | T_PATIENT_ABDM, T_PATIENT_CONSENT_REQUESTS_ABDM (10 Â§2.1 svc 1/25) |

## F04 â€” Walk-in OPD Visit

| Chain element | Evidence |
|---|---|
| Business capability | Register walk-in patient, create OPD encounter, sponsor/tariff defaulting, visit-type sequence (M02) |
| Legacy screen | `op:appointmentFlow` (flagship), `op:walkin_confirmed`; `walkinVisit-schema.json` (03 Â§2.1) |
| FAUG schema | `walkinVisit-schema.json` (6,621 lines; 9 dynamicAspects branches) (03 Â§2.1) |
| JS function | `startAppointmentFlow` â†’ `setWalkinObject` (assembles AddPatientRegistrationDto); `beforeOp: setWalkinObject`, `afterOp.func: setViewOpdChargesRoute` (03 Â§2.1) |
| API | POST `api/rest/walkinVisit/addwalkinVisit?pat_token_no&app_type` (03 Â§2.1, 04 Custom: EmergencyAndWalkinVisitServiceImpl 45 endpoints) |
| Java controller/service | `custom.EmergencyAndWalkinVisitServiceImpl.addwalkinVisit`/`addwalkinVisitNew` (visitstatus2="2" at :1039, "9" at :1141; BR-OPD-004 :1052-1107; BR-OPD-003 :1291-1308; sponsor :1359-1365,1729-1794; tariff :410-419) (06 Â§A1, Â§B) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | `CIMS_ADDPATIENTSPONSOR_NEW_DOC` (BR-OPD-005); `CIMS_ADDPATIENTREGISTRATION*` (walk-in reg path) |
| Tables | T_Encounter, T_Visit, T_PATIENTSPONSOR, M_VISITTYPEMASTER, T_PATIENTTOKEN, T_REGISTRATION (05 Â§3.6, 06 BR-OPD-004/005) |
| Integration | Solr `encounter` (`indexEncounterInSolr`, id=`<encId>_E`; EmergencyAndWalkinVisitServiceImpl:1795-1798,3953-4145) + Redis `current_enc_<encId_E>`; ABDM ABHA (reg in OP), NHNC (walkinVisitNhnc), telemedicine (createTeleconsultRoom), e-Mitra (07 Â§1.3, 03 Â§2.1) |
| Business rules | BR-OPD-001 (non-residential tariff), BR-OPD-002 (slot/encounter collisions BEXA029/BERA028), BR-OPD-003 (OPD no `V/<PREFIX>/<YY>/<seq>`), BR-OPD-004 (visit-type sequence), BR-OPD-005 (sponsor defaulting BERP008), BR-OPD-006 (doctor required BERA022) |
| Target React component | `WalkinVisitPage` + `useWalkinVisit` (stepped sections), `C:PatientSearch`, `F:MLCDetailsSection`, `F:SponsorSection` (09 Â§2.1) |
| Target microservice | 2 Appointment & Encounter (OPD) |
| Target PG entity/table | T_APPOINTMENT, T_ENCOUNTER, T_PATIENTTOKEN (10 Â§2.1 svc 2) |

## F05 â€” Appointment Lifecycle (Book / Reschedule / Cancel / List)

| Chain element | Evidence |
|---|---|
| Business capability | Slot-based appointment booking, reschedule/cancel with reasons, 12-category list (M02) |
| Legacy screen | `op:addAppointment/:doctor_id/:doctor_name/:department_id/:app_date/:from/:totime`, `op:rescheduleApppointment/...`, `op:appointmentList(+Online/doc/Nhnc/New)`; `addpatientRegApptdto_schema.json`, `doctor-appintment-slots-schema.json`, `appointment_list-schema.json`, `appointment-details.html` badges (03 Â§2.1) |
| FAUG schema | `fjs-config_op.json` (43 keys); `updateAppointment_schema.json` (03 Â§2.1) |
| JS function | `showAppointmentListDetails` (appointment-list-module.js); `useAppointmentActions` (reschedule/cancel reason modals) (03 Â§2.1, 09 Â§2.1) |
| API | POST `/appointmentschedule/getAppointmentScheduleNew`, `/getCabinWiseApointmentSlotCount`, `/addOrRemoveSlots`; POST `/updateappointment/cancelupdateappointment`; `patientRegAppt/sendOTP`, `/validateOTP`; GET `/appointment/*` (29) (03 Â§2.1, 04 pas/pas) |
| Java controller/service | `custom.AppointmentScheduleServiceImpl` (25), `custom.AddUpdateAppointmentServiceImpl.canceladdappt` (encounter partial update :191), `pas.AppointmentServiceImpl` (29) (04 findings Â§2) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | T_Appointment, T_Encounter (03 Â§2.1; 05 Â§3.2 T_Encounter 52 refs) |
| Integration | Solr `encounter` (index on confirm; AddUpdateAppointmentServiceImpl:191 partial update); ABHA OTP for online lists (03 Â§2.1); e-Mitra (online/kiosk) |
| Business rules | BR-OPD-002 (same-hour BERA028; duplicate dept/date BEXA029; slot â‰¥30 min; appointmentslot config); A1 (T_Appointment VisitStatus 4/6/11/12) |
| Target React component | `AddAppointmentPage` (`AppointmentWizard`, `SlotPicker`), `AppointmentListPage` (filter chips, 12 categories) (09 Â§2.1) |
| Target microservice | 2 Appointment & Encounter |
| Target PG entity/table | T_APPOINTMENT, T_ENCOUNTER (10 Â§2.1 svc 2) |

## F06 â€” Doctor Check-in / Check-out & Cabin Release

| Chain element | Evidence |
|---|---|
| Business capability | Doctor cabin assignment, check-in/out, cabin release, queue status advance (M02/M27) |
| Legacy screen | `op:doctorCheckIn`; `showDoctorOpSchedule.json`, `doctor-cabin-checkin.html`, `doctor-cabin-checkin-list.html` (03 Â§2.1) |
| FAUG schema | `showDoctorOpSchedule.json`, `doctorScheduleWrap.html` (09 Â§2.1) |
| JS function | `checkinCheckout` port â†’ `useDoctorCheckin` (09 Â§2.1) |
| API | PUT `/doctor/checkinCheckout?isCheckin=`; PUT `/walkinVisit/doctorcheckout`, `/walkinVisit/updateCabinandDoctId`, `/walkinVisit/releaseCabin`; GET `/doctor/*` (25) (03 Â§2.1, 04) |
| Java controller/service | `pas.DoctorServiceImpl` (25); `EmergencyAndWalkinVisitServiceImpl.updateDoctorCheckout` (:2270,:2351), `releaseCabin` (:2146), `updateCabinandDoctId` (06 Â§A1) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | T_Encounter, M_CABINMASTER (06 Â§A1 status 9/10) |
| Integration | Solr `encounter` â€” `searchPatientInEncounterForCheckinCheckout` (SolrUtil.java:1516) cabin/department/doctor/date filters (07 Â§1.4); QMS queues |
| Business rules | A1 (T_Encounter VisitStatus 3=called, 9=doctor checkout, 10=cabin released); BR-OPD-007 (checkout flag on billing) |
| Target React component | `DoctorCheckinPage` (`DoctorList`, `CabinStatusGrid`, `C:ScheduleBoard`) (09 Â§2.1) |
| Target microservice | 2 Appointment & Encounter; 29 Queue & Kiosk (queue state) |
| Target PG entity/table | T_ENCOUNTER, doctor queue/cabin tables (10 Â§2.1 svc 2) |

## F07 â€” Emergency Registration & NHNC Walk-in

| Chain element | Evidence |
|---|---|
| Business capability | Emergency OPD registration (EV numbering), NHNC scheme walk-in variants (M02) |
| Legacy screen | `op:addEmergency/:doctor_id/:doctor_name/:department_id`, `op:emergency_confirmed`; `minimal-walkin-appointment.json`, `walkinVisitNhnc-schema.json` (03 Â§2.1) |
| FAUG schema | `walkinVisitNhnc` (dynamicAspects `isNhncEmer`) (03 Â§2.1) |
| JS function | `startAppointmentFlow` (appointment-module.js); `useNhncRules` (validation.js district/foreigner) (09 Â§2.1) |
| API | POST `/walkinVisit/addEmergencyRegistration`; POST `/markandaddvisit/checkin`; `nhcx.checkEligibility` (03 Â§2.1, 09 Â§2.1) |
| Java controller/service | `custom.EmergencyAndWalkinVisitServiceImpl.addEmergencyRegistration`; `custom.MarkAndAddVisitServiceImpl` (4 endpoints) (04 findings Â§2) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | T_Encounter (emergency visit type), T_Appointment (06 BR-OPD-003/004) |
| Integration | Solr `encounter` (index on emergency visit); NHNC (Rajasthan scheme), ABDM ABHA (03 Â§2.1) |
| Business rules | BR-OPD-003 (EV/<PREFIX>/<YY>/<00001> numbering), BR-OPD-004 (isEmergency=true visit type) |
| Target React component | `EmergencyPanel` within `AdmissionOrderPage`/`WalkinVisitPage`; `NhncAppointmentPage` (09 Â§2.1) |
| Target microservice | 2 Appointment & Encounter |
| Target PG entity/table | T_ENCOUNTER, T_APPOINTMENT (10 Â§2.1 svc 2) |

## F08 â€” MLC Capture & Clearance

| Chain element | Evidence |
|---|---|
| Business capability | Medico-legal case details, brought-by info, documents; IPD MLC clearance filter (M01/M03) |
| Legacy screen | `op:mlcDetails`; `mlcDetailsSchema.json`, `updateMLCDetailsSchema.json`, `addMlcDetailsSchema.json`; `ip:MlcclearanceListFlow`, `mlcClearanceFilters.json`, `mlcList.html` (09 Â§2.1, 03 Â§2.3) |
| FAUG schema | `mlcDetailsSchema` (02 M01), `mlcClearanceFilters` (02 M03) |
| JS function | `useMlcDetails`; walk-in form `mlcDetailsDto` (broughtBy*, isBroughtDead, formalityRemark) (09 Â§2.1, 03 Â§2.1) |
| API | GET `/walkinVisit/getMlcDetailsByVisit`; PUT `/walkinVisit/updateMlcDetailsbyVisitId`; GET `/walkinVisit/mlcFile/{id}`, `/walkinVisit/document/{id}`; `patientTransferDischarge.clearance*` (04 Custom) |
| Java controller/service | `custom.EmergencyAndWalkinVisitServiceImpl.getMlcDetailsByVisit`, `updateMlcDetailsbyVisitId`; `custom.PatientTransferDischargeServiceImpl` (clearance endpoints) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | T_MLCDETAILS (02 M01; 10 Â§2.1 svc 3 owned) |
| Integration | - (file endpoints serve MLC docs) |
| Business rules | BR-DIS-001 (MLC clearance in discharge gate) |
| Target React component | `MLCDetailsPage` + `F:MLCForm`, `C:AuditHistory`; `ClearancePage` MlcClearanceTable (09 Â§2.1/Â§2.2) |
| Target microservice | 3 ADT & Bed (T_MLCDETAILS owner) |
| Target PG entity/table | T_MLCDETAILS (10 Â§2.1 svc 3) |

## F09 â€” Motherâ€“Child Linking & Baby Admission

| Chain element | Evidence |
|---|---|
| Business capability | Link motherâ€“baby records, baby admission, cradle wards (M01/M03) |
| Legacy screen | `ip:motherChildLinking`; `motherChildLinkingSchema.json`; `addBabyAdmissiondto_schema.json` (03 Â§2.3) |
| FAUG schema | `motherChildLinkingSchema` (02 M01), `addBabyAdmissiondto_schema` (02 M03) |
| JS function | `useMotherChildLink` (09 Â§2.2) |
| API | `patientTransferDischarge.motherChild*`; `/bedmanagement/getBabiesDetails`, `/bedmanagement/getAllCradles`, `/bedmanagement/getCradleCount` (04 Custom) |
| Java controller/service | `custom.PatientTransferDischargeServiceImpl` (mother-child ops); `custom.BedManagementServiceImpl.getBabiesDetails` |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | `CIMS_ADDADMISSIONINFORMATION` (baby admission path, BR-IPD-004) |
| Tables | SpouseAndChildrenDetails, FamilyMemberDetails, T_ANCVISITDETAILS, T_Admission (02 M01) |
| Integration | Solr `encounter` (admission index `indexAmissionInSolr`) |
| Business rules | BR-IPD-001 (baby admission requires cradle ward â†’ BERB012); BR-DIS-002 (baby delink on discharge) |
| Target React component | `MotherChildLinkPage` + `F:MotherChildPairing`; `CradlesPage` (09 Â§2.2) |
| Target microservice | 3 ADT & Bed |
| Target PG entity/table | T_ADMISSION, baby/cradle ward tables (10 Â§2.1 svc 3) |

## F10 â€” Admission Request & Execution

| Chain element | Evidence |
|---|---|
| Business capability | Planned/emergency admission with sponsor, IPD number, admission confirmation (M03) |
| Legacy screen | `ip:admissionOrder`, `ip:admissionOrderEmer`, `ip:admissionConfirm`, `ip:admissionList`, `ip:admissionReqList`; `addpatientAdmissiontdto_schema.json` (+_emergency), `admission_list-schema.json`, `admission-confirmation.html` (03 Â§2.3) |
| FAUG schema | `fjs-config_ip.json` (65 keys) (02 M03) |
| JS function | admission-module.js (132 API refs) (03 Â§2.3) |
| API | `/patientRegAddmission/*` (23), `/ipdadmission/*` (18: getSponsorByAdmissionID, addAdmMLCDetails) (03 Â§2.3, 04 findings Â§2) |
| Java controller/service | `custom.PatientRegAdmissionServiceImpl` (BR-IPD-001 :249-281; BR-IPD-002 :283-358; BR-IPD-003 :1289-1301; BR-IPD-004 :1236-1268; bed CAS :344; Solr :875,1180,1662-1665) (06 Â§B, 07 Â§1.3) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | `CIMS_ADDADMISSIONINFORMATION` (inserts T_Admission; BR-IPD-004), `CIMS_ADDADMISSIONDETAILS` (M_BEDMASTER OCCUPIED flag, 05 Â§3.1), `CIMS_UPDATEBEDOCCUPIEDFLAG_1` (10 Â§2.1 svc 3) |
| Tables | T_Admission, T_AdmissionDetails, M_BEDMASTER, M_WARDMASTER, M_ROOMMASTER, M_CLASSMASTER (02 M03; 05 Â§3.1) |
| Integration | Solr `encounter` (id=`<admId>_A` via indexAmissionInSolr) + Redis `current_enc_<admId>_A`; Chiranjeevi (INSURANCESCHEMEID=2), RGHS (patientRghsPackageDetails), HIS addAdmissionRequestToHIS, IPCR (02 M03) |
| Business rules | BR-IPD-001 (BERA022/BERB009/BERB013/BERB012), BR-IPD-004 (IPD no `A/<PREFIX>/<YY>/<seq>`; RISK R4 dead APP015), BR-IPD-002 (bed claim) |
| Target React component | `AdmissionOrderPage` (`AdmissionForm`, `BedPicker`, `SponsorPanel`, `EmergencyPanel`); `AdmissionListPage` (09 Â§2.2) |
| Target microservice | 3 ADT & Bed |
| Target PG entity/table | T_ADMISSION, T_BEDRESERVATION, T_TRANSFERREQUEST, T_PATIENTDISCHARGEDATA (10 Â§2.1 svc 3) |

## F11 â€” Bed Assignment (Auto-Allocation)

| Chain element | Evidence |
|---|---|
| Business capability | Allocate free bed per priority chain with atomic CAS claim (M03) |
| Legacy screen | `ip:bedManagement/:class_id/:ward_id/:bed_id/:building_id/:filterId`; `bed_management_schema.json`, `bed-managment-*.html`, `bed-managment-building.html` (03 Â§2.3) |
| FAUG schema | `bed_management_schema.json` (occupancy colors colorCode) (03 Â§2.3, 09 Â§3 BedBoard) |
| JS function | `useBedBoard` (buildingâ†’deptâ†’ward drill-down), `useBedTransfer` (09 Â§2.2) |
| API | GET `/bedmanagement/getAvailableBeds`, `/bedmanagement/isBedAvailable`, `/bedmanagement/getBedStatusById`; POST `/bedmanagement/getBuildingMasterDetailsNew`, `/getWardDetails`, `/getBedDetails`, `/getBedCount`; `patientRegAddmission.*` bed claim (04 Custom: BedManagementServiceImpl 33) |
| Java controller/service | `custom.PatientRegAdmissionServiceImpl` (allocation algorithm :283-358, :1400-1402; bed status gate :1289-1301); `custom.BedManagementServiceImpl` (status predicates :1859-1864,:2763) (06 BR-IPD-002/003, A5) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | `CIMS_ADDADMISSIONDETAILS`; atomic `UPDATE ... SET OCCUPIED=true WHERE OCCUPIED=false` (06 BR-IPD-002); `CIMS_UPDATEBEDOCCUPIEDFLAG_1` |
| Tables | M_BEDMASTER (OCCUPIED char flag, 05 Â§3.1), T_ADMISSIONDETAILS, M_WARDMASTER, M_CLASSMASTER, M_BEDSTATUSMASTER (06 A5) |
| Integration | Solr `encounter` admission index; Redis bed availability (cache, 10 Â§2.1 svc 3) |
| Business rules | BR-IPD-002 (priority chain + CAS â†’ BEXB001), BR-IPD-003 (isrelease gate â†’ BERB005), A5 (BedStatusId 0/1/3/5/6) |
| Target React component | `C:BedBoard` (+ `BedBlockModal`, `BedTransferDialog`) (09 Â§2.2, Â§3) |
| Target microservice | 3 ADT & Bed |
| Target PG entity/table | M_BEDMASTER, T_ADMISSIONDETAILS (10 Â§2.1 svc 3; 05 Â§8 concurrency note row-level locks) |

## F12 â€” Bed Reservation & Transfer

| Chain element | Evidence |
|---|---|
| Business capability | Reserve beds (incl. cradle/OT), process transfer requests (M03) |
| Legacy screen | `ip:bedReservation`, `ip:unReservedBedList`, `ip:transferReqList`, `ip:otBedManagement`; `bedReservationdto_schema.json`, `addBedReservation.html`, `bedTransferSchema.json`, `transferRequest_list-schema.json` (03 Â§2.3) |
| FAUG schema | `bedReservationdto_schema`, `bedTransferSchema`, `transferRequest_list-schema` (02 M03) |
| JS function | `useBedReservation`, `useTransferRequest` (09 Â§2.2) |
| API | `/bedreservation/*` (4); GET `/bedmanagement/getTransferDetails`, `/bedmanagement/getOTTransferDetails`; `bedmanagement.transfer*` (04 findings Â§2) |
| Java controller/service | `pas.BedReservationServiceImpl` (4; Solr index :165), `custom.BedManagementServiceImpl` (transfer lists), `custom.PatientTransferDischargeServiceImpl` (transfer execution :1074-1102) (06 A5) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | T_BEDRESERVATION, T_TRANSFERREQUEST, M_BEDMASTER (02 M03, 06 A5) |
| Integration | Solr `encounter` (BedReservationServiceImpl:165 indexes reservation) |
| Business rules | A5 (BedStatusId 0 reset on transfer ops; 6 = released) |
| Target React component | `BedReservationPage`, `TransferRequestsPage` (`TransferDialog`), `NursingTransferPage` (09 Â§2.2) |
| Target microservice | 3 ADT & Bed |
| Target PG entity/table | T_BEDRESERVATION, T_TRANSFERREQUEST (10 Â§2.1 svc 3) |

## F13 â€” Interim Admission & Auto-Abscond

| Chain element | Evidence |
|---|---|
| Business capability | Unbedded interim admissions auto-absconded after 3 days by job (M03) |
| Legacy screen | `ip:showInterimAdmissions`; `interimAdmissions.json`, `interimAdmissionFilterwrap.html` (03 Â§2.3) |
| FAUG schema | `interimAdmissions.json` (02 M03) |
| JS function | `useInterimAdmissions` (`PromoteToAdmission`) (09 Â§2.2) |
| API | `ipdadmission.interim*` (04 findings: IPDAdmissionServiceImpl 18) |
| Java controller/service | `pas.IPDAdmissionServiceImpl` (interim ops); `jobs.ApplicationJobs.updateAbscondInterimAdmissionStatusJob` (:1007-1016, ApplicationJobs.java:991-1019) (06 BR-IPD-005) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - (job uses native SQL predicates: BEDID=0/null, ISCANCEL=0) |
| Tables | T_Admission (BEDID, ISCANCEL, ABSCONDDATETIME, DISCHARGETYPEID) (06 A4) |
| Integration | - |
| Business rules | BR-IPD-005 (3-day threshold, `<=`), A4 (ISCANCEL=1 + auto-abscond discharge type) |
| Target React component | `InterimAdmissionsPage` + `F:PromoteToAdmission` (09 Â§2.2) |
| Target microservice | 3 ADT & Bed (scheduled job â†’ Kafka or scheduler) |
| Target PG entity/table | T_ADMISSION (10 Â§2.1 svc 3; job SQL port per 05 Â§5) |

## F14 â€” Discharge Advice & Clearance

| Chain element | Evidence |
|---|---|
| Business capability | Discharge advice, clearance checklist (billing/BMW/lab/MLC), discharge lock (M03) |
| Legacy screen | `ip:updateDischargeStatus`, `ip:clearanceListFlow`; `dischargePatientSchema.json`, `discharge_clearance_schema.json`, `getAdmissionClearanceList.json`, `clearanceTab.html` (03 Â§2.3) |
| FAUG schema | `discharge_clearance_schema` (02 M03) |
| JS function | `useDischarge` (clearance checklist state machine) (09 Â§2.2) |
| API | `patientTransferDischarge.clearance*`; POST `/bedmanagement/initiateDischargeClearance`, `/bedmanagement/markLabClearance`; GET `/bedmanagement/checkDischargeClearanceCount`, `/bedmanagement/getClearanceCount`, `/bedmanagement/getDeadDischargeTransferPatientList` (04 Custom) |
| Java controller/service | `custom.PatientTransferDischargeServiceImpl` (BR-DIS-001 :389-509); `custom.BedManagementServiceImpl.clearanceCount` (:checkDischargeClearanceCount) (06 Â§B) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | T_AdviseDischarge, T_Discharge, T_PATIENTDISCHARGEDATA (06 BR-DIS-001) |
| Integration | Redis `CDD_<opdipdid>` discharge lock (ServiceLayerAspect:1147-1156, 60s) |
| Business rules | BR-DIS-001 (BERB023 advice required; BERB015 pending amount; BERB011 cradle bills; BERB023/24/25 photo+doc+feedback set; transfer-to-other-hospital â†’ inter-hospital admission + COVID referral) |
| Target React component | `DischargePage` (`ClearanceTabs`, `DischargeSummary`, `BillCheckPanel`, `C:ApprovalPanel`); `ClearancePage` (09 Â§2.2) |
| Target microservice | 3 ADT & Bed |
| Target PG entity/table | T_PATIENTDISCHARGEDATA, T_ADMISSION (10 Â§2.1 svc 3) |

## F15 â€” Discharge Execution & Death Handling

| Chain element | Evidence |
|---|---|
| Business capability | Execute discharge (normal/death), update admission/registration state, release bed, clear Solr (M03) |
| Legacy screen | `ip:updateDischargeStatus`, `ip:dischargedPatients`; `dischargeSummary/*_dis_form.html` (Common_Dis_Form, diagnosisDischargeSummary, surgery/medicine variants, dischargeSummaryLabResult) (03 Â§2.3) |
| FAUG schema | `dischargePatientSchema.json`, `updateDischargeStatus.json` (03 Â§2.3) |
| JS function | `useDischarge`; print discharge summary (03 Â§2.3) |
| API | `patientTransferDischarge.*` (30 endpoints) (04 findings Â§2) |
| Java controller/service | `custom.PatientTransferDischargeServiceImpl` (BR-DIS-002 :720-831; BR-DIS-003 :779-803; Solr discharge/death :698-3282, :4400-4413) (06 Â§B, 07 Â§1.3) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | `CIMS_ADDBEDDISCHARGE` (inserts T_Discharge; IsExpiredâ†’death path :24-41), `CIMS_UPDATEIPDISDISCHARGE` (ISDischarged=1, IsClosed='1', closes T_TransferRequest; 06 BR-DIS-002) |
| Tables | T_Discharge, T_Admission, T_REGISTRATION_DETAILS (isdeath=1), PatientDeathDetails, T_TRANSFERREQUEST, T_PATIENTDISCHARGEDATA (06 BR-DIS-002) |
| Integration | Solr `patient` (death=true partial update), `encounter` cleared; Redis CDD_ lock; COVID referral on transfer (06 BR-DIS-001); EHR (discharge summary TIUXML) |
| Business rules | BR-DIS-002 (CIMS_ADDBEDDISCHARGE; baby delink/decouple), BR-DIS-003 (bed OCCUPIED=false + BedStatusId=6; universal/cradle re-map; RISK R5 hard-coded 6) |
| Target React component | `DischargePage` + `DischargedListPage` (`ReadmissionAction`) (09 Â§2.2) |
| Target microservice | 3 ADT & Bed (events PatientDischarged) |
| Target PG entity/table | T_PATIENTDISCHARGEDATA, T_ADMISSION, T_TRANSFERREQUEST (10 Â§2.1 svc 3) |

## F16 â€” Attendant Pass

| Chain element | Evidence |
|---|---|
| Business capability | Attendant pass issue/return/reprint for IPD visitors (M03) |
| Legacy screen | `ip:attendantPass`; `attendantPassSchema.json`, `attendantPass*.html` + label (03 Â§2.3) |
| FAUG schema | `attendantPassSchema` (02 M03) |
| JS function | `useAttendantPass` (PassForm, PassHistory, `C:PrintDialog`) (09 Â§2.2) |
| API | POST `/walkinVisit/printAttendantPass`; `patientTransferDischarge.attendantPass*` (04 Custom) |
| Java controller/service | `custom.EmergencyAndWalkinVisitServiceImpl.printAttendantPass`; `custom.PatientTransferDischargeServiceImpl` (attendant pass endpoints) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | T_ATTENDANTPASS (10 Â§2.1 svc 3 owned) |
| Integration | QZ Tray label print |
| Business rules | - |
| Target React component | `AttendantPassPage` (`F:PassForm`, `F:PassHistory`, `C:PrintDialog`) (09 Â§2.2) |
| Target microservice | 3 ADT & Bed |
| Target PG entity/table | T_ATTENDANTPASS (10 Â§2.1 svc 3) |

## F17 â€” Service Ordering / Charge Capture

| Chain element | Evidence |
|---|---|
| Business capability | Clinician service orders (consult/lab/rad/procedure/nursing) â†’ charges (M08) |
| Legacy screen | `op:consultOrdersList`, `nursing:orderingSet`; `consultOrder_list-schema.json`, `addNursingOrder.html`, `serviceOrderDetail` template (03 Â§2.1, Â§2.4) |
| FAUG schema | `consultOrder_list-schema`, `nursingOrderSchema.json` (02 M08, 09 Â§2.2) |
| JS function | `useOrderComposer` (ADT/consults/diet/lab/med/nursing/procedure/rad tabs) (09 Â§2.2) |
| API | POST `/patient/addServiceOrderDetails`, `/patient/getPatientEorders`; POST `/serviceOrder/addServicechargesNew`, `/serviceOrder/addVisitCharges`, `/serviceOrder/getAllService`; DELETE `/serviceOrder/removeCharge` (04 Custom: AddServiceOrderServiceImpl 49) |
| Java controller/service | `custom.AddServiceOrderServiceImpl.addServices` (temp-charge split :2233-2241); `custom.AddServiceOrderServiceHelper.addservicecharge` (BR-BILL-001 :217-341; BR-BILL-002 :383-419; BR-BILL-003 :421-633; BR-BILL-004 :2767-2798; BR-BILL-005 :2800-2851; BR-PATH-001 :3693-3862) (06 Â§B) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | `CIMS_ADDORDERDETAILS`, `CIMS_ADDCHARGE`, `CIMS_BILLFORINVESTIGATIONNEW` (patho/rad auto-booking; BR-BILL-003) |
| Tables | T_PATIENTEORDER, T_PATIENTADVICE, T_CHARGES, T_CHARGES_TEMP, T_EMRDETAILS, T_EMRSERVICES, ConsultOrder, T_PENDINGCONSULTVISITS (02 M08; 10 Â§2.1 svc 4) |
| Integration | HL7 outbound (order messages, dormant); Solr bill index (InvoiceGenerated); mega-transaction Orderâ†’Billâ†’Labâ†’Radâ†’DoctorShareâ†’EMRâ†’HL7 (10 Â§1) |
| Business rules | BR-BILL-003 (patho/rad order auto-create per M_PathoTestSample), BR-PATH-001 (sample-collected charge cannot be removed â†’ LEX001), A3 (T_CHARGES_TEMP split) |
| Target React component | `OrderingPage` + `useOrderComposer`; `ConsultOrdersPage` (09 Â§2.1/Â§2.2) |
| Target microservice | 4 Ordering & Prescription (Clinical) |
| Target PG entity/table | T_PATIENTADVICE, T_PATIENTEORDER, T_PATIENTMEDICATION, T_EMRDETAILS, T_EMRSERVICES, T_CONSULTORDER (10 Â§2.1 svc 4) |

## F18 â€” Order Sets & Consult Orders

| Chain element | Evidence |
|---|---|
| Business capability | Predefined order-set application (consult/lab/rad/nursing/procedure/consumables/misc) (M05/M08) |
| Legacy screen | `nursing:orderingSet`, `masters:ordersetdefinationmaster`; `addAdtOrder/addConsultOrder/addDietOrder/addLabOrder/addMedOrder/addNursingOrder/addProcedureOrder/addRadOrder.html` (03 Â§2.4) |
| FAUG schema | `orderingSet`/`orderSet` templates; `ordersetdefinationmaster` (02 M05/M08) |
| JS function | `useOrderComposer` (09 Â§2.2) |
| API | `serviceOrder.*` (order set apply: getPackageServices, saveServices) (04 Custom) |
| Java controller/service | `custom.AddServiceOrderServiceImpl` (order set ops) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | T_ORDERSETDEFINATION(+DETAILS), service variants (02 M05) |
| Integration | - |
| Business rules | BR-BILL-003 (orderset lines â†’ charges at billing) |
| Target React component | `OrderingPage` (tab-based `F:OrderComposer`) (09 Â§2.2) |
| Target microservice | 4 Ordering & Prescription; 21 Reference (order-set master) |
| Target PG entity/table | T_ORDERSET* (10 Â§2.1 svc 4 owned orderset data) |

## F19 â€” Bill Generation (OP / IP / Pharmacy)

| Chain element | Evidence |
|---|---|
| Business capability | Draft â†’ final bill with numbering, tax, concession, doctor share (M04) |
| Legacy screen | `billing:billingFlow`, `billing:ipBillingFlow`, `billing:opIpBillingFlow`; `addBillingSchema.json`, `addIpBillingSchema.json`, `ipBillSummeryWithoutPharmcy.vm` (03 Â§2.2) |
| FAUG schema | `fjs-config_billing.json` (77 keys) (02 M04) |
| JS function | `startBillingFlow` (billing-module.js:707), `calculateTotalBillAmount` (billing-module.js:2050 â€” 3 tax buckets, concession, self/non-self), `confirmServices/Step2/3`, `finalServiceSummary` (03 Â§3) |
| API | POST `/serviceOrder/addServicechargesNew`, `/serviceOrder/addPharmacyItemtoChargeForIP`; `/bill/*` (20); POST `/billSummary/getAllBills`; GET `/billReceipt/downloadFile` (04) |
| Java controller/service | `custom.AddServiceOrderServiceHelper.addservicecharge` (BR-BILL-001 :217-341); `pas.BillServiceImpl` (20; CIMS_SETTLED/UNSETTLEDBILLCANCELLATION refs :70-91); `custom.BillSummaryServiceImpl` (7) (06 Â§B, 04 findings Â§2) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | `CIMS_ADDBILL` (IsCancelled=0, Status=true, BillNo placeholder; generated-procs/CIMS_ADDBILL.sql:87,129), `CIMS_UPDATEBILLFORCONCESSION` (settle flag), `CIMS_ADDCHARGE` |
| Tables | T_BILL, T_BILLDETAILS, T_CHARGES, T_CHARGES_TEMP, T_ITEMSALE (06 A2/A3) |
| Integration | Solr bill search index (InvoiceGenerated â†’ svc 27 per 10 Â§2.1); Redis tariff lookup |
| Business rules | BR-BILL-001 (bill no from getLatestAutocodeSeq T_OPDBill/IP; net = Totalâˆ’Taxâˆ’Concession), BR-BILL-004 (tax up to 3, applyafterconcession), BR-BILL-005 (copay/concession split; RISK R2), A2 (Status=1, IsSettled=false) |
| Target React component | `BillingPage` (`ServiceLineEditor`, `SponsorPanel`, `PackagePanel`) + `useBilling` (09 Â§2.3) |
| Target microservice | 5 Billing & Settlement |
| Target PG entity/table | T_BILL, T_BILLDETAILS, T_CHARGE, T_CONCESSION, T_DOCTORSHARE (10 Â§2.1 svc 5) |

## F20 â€” Bill Settlement / Payment / OPD Checkout

| Chain element | Evidence |
|---|---|
| Business capability | Collect payment (modes), settle bill, advance adjust FIFO, OPD checkout VisitStatus=11 (M04) |
| Legacy screen | `billing:addPayment`; `paymentDetailSchema.json`, `opBillPaymentSchema.json`, `partialPayBillSchema.json`, `showPaymentModes` modal (03 Â§2.2) |
| FAUG schema | `paymentDetailSchema`, `partialPayBillSchema`, `ipBillPaymentSchema` (02 M04) |
| JS function | `addPayment`/`addAdvance`/`showRefund` (billing-module.js), `setDefaultAdvanceAdjCheck` (03 Â§2.2) |
| API | POST `/addPayment/addBillPaymentNew`; PUT `/addPayment/updateCharges`; POST `/billSummary/getServiceListForPartialBillPayment`; `/payment/*` (5) (04) |
| Java controller/service | `custom.AddPaymentServiceHelper.addBillPayment` (BR-BILL-006 :438-506; BR-BILL-007 :178-349; BR-OPD-007 :438-447,477,820); `custom.AddPaymentServiceImpl.addBillPaymentNew` (06 Â§B, 04 findings) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | `CIMS_ADDPAYMENT*`, `CIMS_ADDPAYMENTADVANCEDETAILS` (advance consumption), `CIMS_UPDATEBILLFORCONCESSION` (IsSettled) (06 BR-BILL-006/007) |
| Tables | T_PAYMENT, T_PAYMENTMODEDETAILS, T_BILL, T_ADVANCE, T_Encounter/T_Appointment (VisitStatus) (06 A2/A6) |
| Integration | Solr `encounter` (isCheckout=true + index), Redis (payment dedup attempt keys); print server (receipts) |
| Business rules | BR-BILL-006 (settled â‡” self&nonSelf â‰¤ 0; changeAmount on T_Bill), BR-BILL-007 (FIFO advance, payment-mode 7), BR-OPD-007 (T_Appointment.VisitStatus=11; isCheckout), A6 (TransactionType=2) |
| Target React component | `OpBillPaymentPage`/`IpBillSummaryPage` (`F:PaymentPanel`, `F:PartialPay`) (09 Â§2.3) |
| Target microservice | 6 Payments & Refunds (payment) + 5 Billing (settlement via event) |
| Target PG entity/table | T_PAYMENT, T_PAYMENTMODEDETAILS, T_ADVANCE, T_REFUND, T_RECEIPT (10 Â§2.1 svc 6) |

## F21 â€” Advance Collection & Adjustment

| Chain element | Evidence |
|---|---|
| Business capability | Collect advances against bills/estimations, adjust FIFO at settlement (M04) |
| Legacy screen | `billing:advanceReceipt`; `addAdvanceSchema.json`, `advanceSchema.json`, `advanceReceiptByReceiptNoSchema.json`, `addAdvancePaymentList.html` (03 Â§2.2) |
| FAUG schema | `addAdvanceSchema`, `advanceSchema` (02 M04) |
| JS function | `addAdvance` (billing-module.js) (03 Â§2.2) |
| API | POST `/addAdvance/addAdvance`; GET `/addAdvance/getAdvanceAgainstList`, `/addAdvance/getAdvancePaymentDetailList`, `/addAdvance/getPatientAdvanceByBillEstimation` (04 Custom: AddAdvanceServiceImpl 5) |
| Java controller/service | `custom.AddAdvanceServiceImpl.addAdvance` (tx); `custom.AddPaymentServiceHelper` (adjustment :178-349) (06 BR-BILL-007) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | `CIMS_ADDADVANCE`, `CIMS_ADDPAYMENTADVANCEDETAILS` (BR-BILL-007) |
| Tables | T_ADVANCE, T_PAYMENT, T_PAYMENTMODEDETAILS (06 A6) |
| Integration | - |
| Business rules | BR-BILL-007 (balances>0 consumed in id order; partial split; mode 7) |
| Target React component | `AdvancePage` (`F:AdvanceForm`, `F:AdvanceList`, `C:PrintDialog`) (09 Â§2.3) |
| Target microservice | 6 Payments & Refunds |
| Target PG entity/table | T_ADVANCE (10 Â§2.1 svc 6) |

## F22 â€” Advance Refund & Bill Cancellation

| Chain element | Evidence |
|---|---|
| Business capability | Refund advances (TT 3) and bills (TT 4); restore charges on settled cancellation (M04) |
| Legacy screen | `billing:advanceRefund`; `advance_refund.json`, `return_advance.json`, `advanceRefundReceipt.json`; cancel summary schemas (03 Â§2.2) |
| FAUG schema | `refundDetailSchema.json`, `cancelOpBillSummarySchema.json`, `cancelIpBillSummarySchema.json` (03 Â§2.2) |
| JS function | `showRefund` (billing-module.js) (03 Â§2.2) |
| API | POST `/addRefund/refund`; POST `/cancelBill/` (billCancel), `/cancelBill/refundReceiptByIds`; POST `/refundBillReceipt/*` (04 Custom) |
| Java controller/service | `custom.AddRefundServiceImpl.addRefund` (:82-155; BEXAR002); `custom.BillCancellationServiceImpl.billCancel` (:231-295,:347,:470-509; *BeforeFinalization restore) (06 BR-BILL-008) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | `CIMS_SETTLEDBILLCANCELLATION`, `CIMS_UNSETTLEDBILLCANCELLATION` (10 Â§5; BillServiceImpl.java:70-91) |
| Tables | T_REFUND, T_ADVANCE, T_BILL, T_CHARGES, T_ITEMSALE, T_DOCTORSHARE (06 A3/A6) |
| Integration | - |
| Business rules | BR-BILL-008 (advance refund TT=3 requires balance>0; bill refund TT=4; settled-cancel restores T_Advance, charges from *BeforeFinalization, T_Itemsale BillID=0, doctor-share status=false) |
| Target React component | `AdvanceRefundPage` (`F:RefundForm`, `F:RefundReceipt`) (09 Â§2.3) |
| Target microservice | 6 Payments & Refunds (refund/cancellation) |
| Target PG entity/table | T_REFUND, T_PAYMENT, T_ADVANCE (10 Â§2.1 svc 6) |

## F23 â€” Credit Note

| Chain element | Evidence |
|---|---|
| Business capability | Credit notes for OP/IP bills and package refunds (M04) |
| Legacy screen | `billing:creditNoteBillingListFlow`; `creditNoteSchema.json`, `getAllCreditNoteListSchema.json` (03 Â§2.2) |
| FAUG schema | `creditNoteSchema` (02 M04) |
| JS function | `useCreditNote` (09 Â§2.3) |
| API | POST `/serviceOrder/saveCreditNote`, `/serviceOrder/addPackageRefund`, `/serviceOrder/getChargesForCreditNote`; POST `/billReceipt/printCreditNoteForOP`, `/printCreditNoteForIP`, `/printPackageRefundReceipt` (04) |
| Java controller/service | `custom.AddServiceOrderServiceImpl.saveCreditNote`; `custom.BillReceiptServiceImpl.printCreditNoteForIP/OP` (04 findings Â§2) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | T_CREDITNOTE(+DETAILS), T_BILL (02 M04) |
| Integration | - |
| Business rules | - |
| Target React component | `CreditNotePage` (`F:CreditNoteForm`, `C:DataTable`) (09 Â§2.3) |
| Target microservice | 5 Billing & Settlement |
| Target PG entity/table | T_CREDITNOTE (10 Â§2.1 svc 5) |

## F24 â€” Doctor Share Settlement

| Chain element | Evidence |
|---|---|
| Business capability | Doctor professional-fee share split, add/deduct, doctor bills & payments (M04/M06) |
| Legacy screen | `billing:doctorShare`, `billing:doctorShareAddDeduct`, `billing:doctorShareBill`, `billing:doctorBillList`; `doctorShareSchema.json`, `doctorShareBillSchema.json` (03 Â§2.2) |
| FAUG schema | `doctorShare*` schemas (02 M06) |
| JS function | `useDoctorShare` (09 Â§2.3) |
| API | `/doctor/addDoctorAdditionDeduction`, `/doctor/getDoctorShareList`, `/doctor/addDoctorBill`, `/doctor/addDoctorBillPayment`, `/doctor/updateDoctorShare` (02 M06) |
| Java controller/service | `pas.DoctorServiceImpl` (share endpoints); `custom.AddServiceOrderServiceHelper` (split at billing :383-419) (06 BR-BILL-002) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | `CIMS_ADDDOCTORSHARE` (IsBilled=false; CEILING/HALF_UP rounding) (06 BR-BILL-002) |
| Tables | T_DOCTORSHARE, T_DOCTORBILL, T_DOCTORVISITCHARGES, T_DOCTORWAIVER (02 M06) |
| Integration | - |
| Business rules | BR-BILL-002 (skip otProcedure=true; share = totalÃ—%/100; hospital = totalâˆ’share) |
| Target React component | `DoctorSharePage` (`F:DoctorShareTable`, `F:AdjustDialog`) (09 Â§2.3) |
| Target microservice | 5 Billing & Settlement |
| Target PG entity/table | T_DOCTORBILL, T_DOCTORSHARE (10 Â§2.1 svc 5) |

## F25 â€” Package Subscription & Health-Plan Consumption

| Chain element | Evidence |
|---|---|
| Business capability | Package subscription (RGHS), health-plan consumption marking (NHNC) (M04/M05) |
| Legacy screen | `billing:packageSubscriptionFlow`, `billing:packageAppointmentList`, `billing:healthPlanPackage`, `billing:healthPlanPackageNhnc`; `addPackageSubscriptionSchema.json`, `packageListForBillSchema.json`, `healthplanConsumptionDetailSchema.json` (03 Â§2.2) |
| FAUG schema | `fjs-config_billing.json` package keys (02 M04) |
| JS function | packageBilling.js; `checkoutForHealthConsumption` (03 Â§2.2) |
| API | POST `/serviceOrder/allPackagesFastByGet`, `/serviceOrder/addHealthplanConsumptionDetail`, `/serviceOrder/addMultipleHealthplanConsumptionDetail`, `/serviceOrder/addVisitForHealthplanConsumption`, `/serviceOrder/updateHealthplanConsumptionSchedule`, `/serviceOrder/getSubscribedPackageCharges`; `/packageservice/*` (22) (04) |
| Java controller/service | `custom.AddServiceOrderServiceImpl` (package ops, addHealthplanPackageConsumptionDetails); `pas.PackageServiceServiceImpl` (22) (04 findings Â§2) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | T_PACKAGECHARGESUBSCRIPTION, HealthplanPackageConsumptionDetails, T_PACKAGEMASTER (02 M04/M05) |
| Integration | RGHS packages (getRghsPackageList), NHCX (insurance_plan_on_request) |
| Business rules | BR-BILL-003 (package coverage in billing); M05 BR (plan exclusions) |
| Target React component | `PackageSubscriptionPage` (`PackagePick`, `PackageDetail`, `HealthPlanConsumption`) (09 Â§2.3) |
| Target microservice | 5 Billing & Settlement; 23 RCM (NHCX) |
| Target PG entity/table | T_PACKAGECHARGESUBSCRIPTION (10 Â§2.1 svc 5) |

## F26 â€” Prescription (Doctor / Nurse)

| Chain element | Evidence |
|---|---|
| Business capability | Prescription creation (doctor/nurse/temp), medication orders, home meds (M07) |
| Legacy screen | `pharmacy:addPrescription`, `pharmacy:medicationOrder`, `nursing:docPrescriptionList`, `pharmacy:NursePrescriptionList`; `addPrescriptionSchema.json`, `addMedicationSchema.json` (+FromNurse), `ipHomeMedsForm` (03 Â§2.5) |
| FAUG schema | `addPrescriptionSchema`, `addMedicationSchema` (02 M07) |
| JS function | pharmacy-module.js / pharmacy-module-new.js; `usePrescriptionList` (09 Â§2.4) |
| API | `/prescriptionmaster/*` (5), `/prescription/*` (1), `/medication/*` (23), `/drug/*` (1) (04 findings Â§2) |
| Java controller/service | `pas.PrescriptionMasterServiceImpl` (5), `custom.PrescriptionDetailsServiceImpl`, `custom.MedicationServiceImpl` (23) (04 findings Â§2) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | `CIMS_ADDORDERDETAILS` (medication order path); `CIMS_ADDITEMSALESDETAILS_NEW` (closure, BR-PHAR-002) |
| Tables | T_PRESCRIPTIONMASTER, T_PRESCRIPTIONDETAILS, T_PATIENTMEDICATION, T_PATIENTPRESCPTIONNOTE, T_PATIENTNONMEDORDERDETAILS (02 M07) |
| Integration | Solr `item` (drug search, searchAllItemsInSolr:1173); ABDM PHR (patPrescriptionPHR.vm) |
| Business rules | BR-PHAR-002 (IsClosed/PendingQty on sale; isverified/isdispensed/ispartialdispense flags); A9 (medication 1/2/3, patient med 1/2/3/4) |
| Target React component | `PrescriptionListPage` (`F:PrescriptionDetail`, `F:PrescriptionUpload`); `usePrescriptionList` (09 Â§2.4) |
| Target microservice | 4 Ordering & Prescription (creation); 9 Pharmacy (dispense) |
| Target PG entity/table | T_PATIENTMEDICATION, T_PRESCRIPTIONMASTER, T_PRESCRIPTIONDISPENSEDETAILS (10 Â§2.1 svc 4/9) |

## F27 â€” Pharmacy Order Verification & Dispense (IP)

| Chain element | Evidence |
|---|---|
| Business capability | Verify â†’ dispense IP orders with batch/expiry check; VisitStatus=12 on completion (M07) |
| Legacy screen | `pharmacy:pharmacyOrderVerify`, `pharmacy:pharmacyOrderDispense`, `pharmacy:pharmacyOrderDispenseD`; `getPrescriptionDetailsSchema.json`, `itemSaleDetailSchema.json`, `batchRow.html`, `dispenseDetailRow.html` (03 Â§2.5) |
| FAUG schema | `dispensingtype`, `opIpPharmacyBillSchema.json` (02 M07) |
| JS function | `usePharmacyOrder` (ports of showOrdersFunc, populateIPOrders, showIVOrders) (09 Â§2.4) |
| API | `/pharmacy/*` (102), `/medication/dispenseItemSale` (04 findings Â§2) |
| Java controller/service | `custom.MedicationServiceImpl.dispenseItemSale` (:799-821 â€” sets T_ITEMSALE.DISPENSED, then T_Appointment.VisitStatus=12); `custom.PharmacyServiceImpl` (102; :735-746 prescription closure) (06 BR-PHAR-001/002) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | `CIMS_ADDITEMSALESDETAILS_NEW` (sale detail + batch, closes prescription; :103-121) |
| Tables | T_ITEMSALE, T_ITEMSALEDETAILS, T_PRESCRIPTIONDISPENSEDETAILS, T_ITEMBATCHMASTER, T_Appointment (06 BR-PHAR-001) |
| Integration | Solr `item` (searchAllItemsInSolr); stock decrement on T_ITEMSTOCK (10 Â§2.1 svc 9 borrows stock ops) |
| Business rules | BR-PHAR-001 (all details DISPENSED â†’ T_ITEMSALE.DISPENSED â†’ VisitStatus=12), BR-PHAR-002 (CIMS_ADDITEMSALESDETAILS_NEW closes prescription) |
| Target React component | `PharmacyOrderPage` (`F:OrderLines`, `F:BatchPicker` scan, `F:DispensePanel`) (09 Â§2.4) |
| Target microservice | 9 Pharmacy & Dispensing |
| Target PG entity/table | T_PRESCRIPTIONMASTER, T_ITEMSALE, T_PRESCRIPTIONDISPENSEDETAILS, T_ITEMCOMPOUNDDETAILS (10 Â§2.1 svc 9) |

## F28 â€” OP Pharmacy / Counter Sale & Cart-Fill

| Chain element | Evidence |
|---|---|
| Business capability | OPD dispensing, counter (OTC) sales, ward cart-fill pick lists (M07) |
| Legacy screen | `pharmacy:opPharmacyFlow`, `pharmacy:pharmacyCountersale`, `pharmacy:pharmacyOp`, `pharmacy:cartFillFlow/:ward_id/:patient_dfn`, `pharmacy:phCartFillOrderDispense`; `pharmacyCounterSaleSchema.json`, `cartFillDetails.html`, `cartFillPatientDetails.html` (03 Â§2.5) |
| FAUG schema | `pharmacyCounterSaleSchema`, `opPharmacyAddPatient.json`, `addPatientPrescMedSchema.json` (02 M07) |
| JS function | `useOpPharmacy`, `useCartFill` (scanBarcodeCartFill) (09 Â§2.4) |
| API | `/pharmacy.opDispense*`, `/pharmacy.pharmacyCounterSale*`, `/pharmacy.cartFill*`; POST `/addPayment/itemSalePayment` (04) |
| Java controller/service | `custom.PharmacyServiceImpl` (counter sale, item sale); `custom.AddPaymentServiceImpl.addItemSalePayment` (tx) (04 findings Â§2) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | `CIMS_ADDITEMSALESDETAILS_NEW`; `CIMS_GETPRESCRIPTIONWARDDETAILS*` (ward pick lists) |
| Tables | T_ITEMSALE, T_ITEMSTOCK, T_PRESCRIPTIONDISPENSEDETAILS, ward picklist tables (10 Â§2.1 svc 9) |
| Integration | Solr `item`; QZ label print (pharmacyItemSaleBillNew) |
| Business rules | BR-PHAR-001 (visit-status advance on OP dispense) |
| Target React component | `OpPharmacyPage`, `CartFillPage` (`CartPatientList`, `CartOrderDetail`, `C:BarcodeScannerField`) (09 Â§2.4) |
| Target microservice | 9 Pharmacy & Dispensing |
| Target PG entity/table | T_ITEMSALE, ward picklist (10 Â§2.1 svc 9) |

## F29 â€” Pharmacy Returns & Compounding (IV)

| Chain element | Evidence |
|---|---|
| Business capability | IP/OP item-sale returns; IV admixture & compound preparation (M07) |
| Legacy screen | `pharmacy:itemSaleReturnList`, `pharmacy:ipReturnFlow`, `pharmacy:iPReturnFlow`, `pharmacy:ipReturnPharmFlow`, `pharmacy:ivPharm`, `pharmacy:prepareCompound`; `itemSaleReturnListSchema.json`, `bcmaIV.html`, `compoundDetailRow.html` (03 Â§2.5) |
| FAUG schema | `itemSaleReturnListSchema`, `pharmacyItemSaleRetrunBIllReciptSchema.json` (02 M07) |
| JS function | `useIpReturn` (startIPReturnFlow), `useIvPharmacy`, `useCompoundPrep` (09 Â§2.4) |
| API | `pharmacy.iPReturnFlow*`, `pharmacy.itemSale*`, `pharmacy.iv*`, `pharmacy.compound*` (04) |
| Java controller/service | `custom.PharmacyServiceImpl` (return flows); `custom.MedicationServiceImpl` (IV/compound orders) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | `CIMS_ADDITEMSALESDETAILS_NEW` (compound details closed) |
| Tables | T_ITEMSALE, T_ITEMSTOCK (restock), T_INFUSIONMIXING(+DETAILS), T_ITEMCOMPOUNDDETAILS (02 M07) |
| Integration | Solr `item` |
| Business rules | BR-PHAR-002 (compound closure) |
| Target React component | `IpReturnPage` (`ReturnLines`, `ReturnReasonPanel`), `IvPharmacyPage`, `CompoundPrepPage` (09 Â§2.4) |
| Target microservice | 9 Pharmacy & Dispensing |
| Target PG entity/table | T_ITEMSALE, T_ITEMCOMPOUNDDETAILS, T_INFUSIONMIXING (10 Â§2.1 svc 9) |

## F30 â€” Lab Order Booking & Sample Collection

| Chain element | Evidence |
|---|---|
| Business capability | Patho order booking (auto from billing), barcode sample collection (M15) |
| Legacy screen | `pathology:lab`, `pathology:labN`, `pathology:sampleCollection/:opdipd/:mrnNo/:catg`, `pathology:sampleGenerateBarcode`; `pathoSchema.json`, `sampleCollectSchema.json`, `pathoGenerateBarCodeSchema.json` (03 Â§2.6) |
| FAUG schema | `fjs-config_pathology.json` (57 keys) (02 M15) |
| JS function | patho-booking-module.js; `useSampleCollection` (09 Â§2.6) |
| API | `/pathoOrder/*` (133) (04 findings Â§2) |
| Java controller/service | `custom.PathoOrderServiceImpl` (133); `custom.AddServiceOrderServiceHelper` (auto-booking at billing :421-633 â†’ CIMS_BILLFORINVESTIGATIONNEW) (06 BR-BILL-003) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | `CIMS_BILLFORINVESTIGATIONNEW` (0 â†’ order + detail per M_PathoTestSample sample; SampleCollectionNO=1..n) |
| Tables | T_PATHORDERBOOKING(+DETAILS), T_PATHOPATIENTPARAMETERDETAILS, PathoPatientDocs (02 M15) |
| Integration | KDL machine interface, HL7/emr (Lab_Results), blood-bank cross-match barcode (pathoGenerateBarCodeSchemaForBloodBank) |
| Business rules | BR-BILL-003 (patho auto-order; IsOutSourced=false, PathologistId=0), BR-PATH-001 (collected sample locks charge â†’ LEX001) |
| Target React component | `LabBookingPage`, `SampleCollectionPage` (`PathoFilterBar`, `BarcodePrint`) (09 Â§2.6) |
| Target microservice | 7 Lab (Pathology) |
| Target PG entity/table | T_PATHORDERBOOKING(+DETAIL), T_PATHOPATIENTPARAMETERDETAILS, T_MACHINECONFIGURATION (10 Â§2.1 svc 7) |

## F31 â€” Lab Result Entry / Review / Release / Critical Alerts

| Chain element | Evidence |
|---|---|
| Business capability | Parameter-level resulting, reviewer approval, double-verified release, critical alert routing (M15) |
| Legacy screen | `pathology:sampleResult/...`, `pathology:sampleResultReview/...`, `pathology:sampleReportRelease/...`, `pathology:sampleCriticalAlert/...`, `pathology:kdlVerify`; `pathoResultSchema.json`, `resultReviewSchema.json`, `reportReleaseSchema.json`, `criticalAlertResultSchema.json`, `firstVerificationSchema.json`, `secondVerificationSchema.json` (03 Â§2.6) |
| FAUG schema | `fjs-config_pathology.json` (resulting/review/release schemas) (02 M15) |
| JS function | patho-modules.js (429 KB) â€” loadPathoFilters/loadXResults; `useResultEntry`, `useResultReview`, `useReportRelease` (09 Â§2.6) |
| API | `pathoOrder.result*`, `pathoOrder.resultReview*`, `pathoOrder.reportRelease*`, `pathoOrder.criticalAlert*`, `pathoOrder.machineWise*` (04 findings: PathoOrderServiceImpl 133) |
| Java controller/service | `custom.PathoOrderServiceImpl` (result save/release); `custom.MachineConfigServiceImpl`; WhatsApp trigger `PathoOrderServiceHelper.sendCategoryWiseReportOnWhatsApp` (07 Â§3.1 #15) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | T_PATHOPATIENTPARAMETERDETAILS, T_PATHORDERBOOKINGDETAILS (02 M15) |
| Integration | WhatsApp/SMS (eSanchar) on release (T_WHATSAPPMESSAGEQUEUE, ApplicationJobs:1611-1643); KDL instruments; ABDM EHR (LabResultFinalized event) |
| Business rules | BR-PATH-001 (sample state gates charge ops); A7 blood cross-match linkage |
| Target React component | `SampleResultPage`, `ResultReviewPage`, `ReportReleasePage` (SignatureCapture double-verify), `CriticalAlertPage` (09 Â§2.6) |
| Target microservice | 7 Lab (Pathology) |
| Target PG entity/table | T_PATHOPATIENTPARAMETERDETAILS (10 Â§2.1 svc 7; LabResultFinalized â†’ Kafka) |

## F32 â€” Radiology Order, Report & Reschedule

| Chain element | Evidence |
|---|---|
| Business capability | Rad order booking, queue, template report entry, retest/reschedule, print (M16) |
| Legacy screen | `radiology:radlab(/:p_id/:order_no/:date/:opd_ipd)`, `radiology:reSchedule`, `radiology:radReTest`, `radiology:printReport`; `radOrderBookingSchema.json`, `radReportSchema.json`, `radRescheduleSchema.json`, `radRetestSchema.json`, `showRadiologyReport.html` (03 Â§2.6) |
| FAUG schema | `fjs-config_radiology.json` (9 keys) (02 M16) |
| JS function | `useRadiology` (shares pathology filter hooks), `useRadResult` (09 Â§2.7) |
| API | `/radiology/*` (10); `pathoOrder.rad*`, `pathoOrder.radReport*`, `pathoOrder.radPrint*` (04 findings Â§2) |
| Java controller/service | `pas.RadiologyServiceImpl` (10); `custom.AddServiceOrderServiceHelper` (rad auto-booking :579-599 â†’ CIMS_ADDRADORDERBOOKING) (06 BR-BILL-003) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | `CIMS_ADDRADORDERBOOKING` (BR-BILL-003; IsImageCaptured=false default) |
| Tables | T_RADORDERBOOKING(+DETAILS), M_RADTESTMASTER, M_RADTEMPLATEMASTER, M_RADTEMPLATERESULT (02 M16) |
| Integration | PACS/imaging (addRadImg, showRadSampleImg) (02 M16) |
| Business rules | BR-BILL-003 (rad analog of patho auto-order) |
| Target React component | `RadiologyPage` (`BookingForm`, `Reschedule`, `Retest`), `RadResultPage` (`TemplateRenderer`, `ImageUpload`), `RadPrintPage` (09 Â§2.7) |
| Target microservice | 8 Radiology |
| Target PG entity/table | T_RADORDERBOOKING(+DETAILS), rad masters (10 Â§2.1 svc 8) |

## F33 â€” OT Scheduling

| Chain element | Evidence |
|---|---|
| Business capability | Surgery scheduling with theatre/table, doctor-slot conflict checks (M12) |
| Legacy screen | `ot:surgSchedule`, `ot:otScheduleEvent`, `ot:addNewSurg`, `ot:surgList`; `surgeryScheduleSchema.json`, `addNewSurgerySchema.json`, `otSchedular.html`, `cancelOTSchedule.html` (03 Â§2.11) |
| FAUG schema | `fjs-config_ot.json` (14 keys) (02 M12) |
| JS function | `useOtSchedule` (timeline `C:ScheduleBoard`) (09 Â§2.10) |
| API | `/otscheduler/*` (80: addNewSurgery, checkDoctorAllreadyBookedForSameTime, checkOTAvailability, cancelSurgeryPatient) (04 findings Â§2) |
| Java controller/service | `pas.OTSchedulerServiceImpl` (80; BR-OT-001 :616-800; BR-OT-002 :4069-4077) (06 Â§B) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | T_OTSCHEDULE(+DETAILS), T_SURGERYDETAILS, M_OTTHEATREMASTER, M_OTTABLEMASTER, M_OPERATIONSTATUSMASTER (02 M12) |
| Integration | CSSD (ProcedureCssdRequest), inventory (OT items) |
| Business rules | BR-OT-001 (active admission else visit â†’ BEOT004; dependent-surgery discount; procedure rate from tariff), BR-OT-002 (isOtProcessDone filter) |
| Target React component | `OtSchedulePage` (`C:ScheduleBoard` timeline, `SurgeryForm`, `CancelReason`); `SurgeryListPage` (09 Â§2.10) |
| Target microservice | 11 OT & Surgery |
| Target PG entity/table | T_OTSCHEDULE(+DETAILS), T_SURGERYDETAILS, T_ANESTHESIANOTES (10 Â§2.1 svc 11) |

## F34 â€” OT Procedure Performance & OT Billing

| Chain element | Evidence |
|---|---|
| Business capability | Procedure execution (checklist, anesthesia, equipment, staff), pre-billing charges (M12) |
| Legacy screen | `ot:procPerformance`, `ot:patMovementAudit`, `billing:otBillFlow`; `procedurePerformanceSchema.json`, `patientProcedureDetails.html`, `otCheckList.html`, `addOTBillingSchema.json`, `addOTCharge.json` (03 Â§2.11, Â§2.2) |
| FAUG schema | `procedurePerformanceSchema`, `addOTBillingSchema`, `surgeryheads` (02 M12) |
| JS function | `useProcedurePerformance` (ProcedureForm, AnesthesiaPanel, EquipmentPanel, StaffPanel, IntraOpNotes) (09 Â§2.10) |
| API | `otscheduler.procedure*`, `otscheduler/addPatientProcdure`, `/otconfig/getSurgeryHeadDetailsForBillEstimation`, `otscheduler/addSurgCharge`; `/surgerydetails/*` (6) (04) |
| Java controller/service | `pas.OTSchedulerServiceImpl` (procedure/schedule ops); `custom.AddServiceOrderServiceImpl` (pre-billing :2202-2204 consumed=true) (06 BR-OT-002) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | T_PATIENTPROCEDUREANESTHESIADETAILS, T_PATIENTPROCEDURERATEDETAILS, T_PATIENTSCHEDULEANASTHESIADETAILS, T_PATIENTPROCEDURECHECKLISTITEMS(+DETAILS), T_SURGCHARGES, T_SURGERYHEADMASTER(+DETAILS) (02 M12) |
| Integration | CSSD kits (ProcedureCssdIssue), inventory equipment |
| Business rules | BR-OT-002 (otprocedure.pre.billing â†’ consumed charges; doctor-share skipped for OT tariff services) |
| Target React component | `ProcedurePerformancePage` (`F:ProcedureForm`, `AnesthesiaPanel`, `EquipmentPanel`, `StaffPanel`); `OtBillingPage` (09 Â§2.10/Â§2.3) |
| Target microservice | 11 OT & Surgery; 5 Billing (pre-billing) |
| Target PG entity/table | T_OTSCHEDULE, T_SURGERYDETAILS, procedure masters (10 Â§2.1 svc 11) |

## F35 â€” Blood Requisition & Cross-Match

| Chain element | Evidence |
|---|---|
| Business capability | Patient/external requisitions, cross-match workflow, status machine 1/2/3/10 (M17) |
| Legacy screen | `bloodBank:patientRequisition`, `bloodBank:patientRequisition/extnlhosp`, `bloodBank:campRequisition`, `bloodBank:bloodCrossMatching`, `bloodBank:crossMatchingList`, `bloodBank:newCrossMatchingTest`, `bloodBank:crossMatchedBags`; `patientRequisitionList.json`, `bloodCrossMatchingListSchema.json`, `crossMatchingForm.json` (03 Â§2.7) |
| FAUG schema | `fjs-config_bloodBank.json` (40 keys) (02 M17) |
| JS function | blood-bank-module.js (653 KB); `useRequisition`, `useCrossMatching` (09 Â§2.9) |
| API | `/patientrequisition/*` (10); `/bloodtestmaster/*` (113) (04 findings Â§2) |
| Java controller/service | `pas.PatientRequisitionServiceImpl` (10; Solr index :746-854); `pas.BloodBankServiceImpl` (113; updateRequisitionStatus :6290-6317; bag predicates :2273-2277) (06 BR-BLD-001, 07 Â§1.3) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | T_BLOODREQUISITION, T_PATIENTREQUISITION, T_BLOODCROSSMATCHINGRESULT(+DETAILS) (02 M17) |
| Integration | Solr `encounter` (requisition indexing); external hospitals/storage centers; blood-bank barcode via patho (pathoGenerateBarCodeSchemaForBloodBank) |
| Business rules | BR-BLD-001 (1=new â†’ 2=cross-matched volâ‰¥ordered â†’ 3=issued; 10=cancelled/expired), BR-BLD-004 (requisition expiry by orderValidationPeriod), A7 (auto-decross predicates) |
| Target React component | `RequisitionPage` (`RequisitionForm`, `CrossMatchRequest`), `CrossMatchingPage` (CrossMatchWizard, BagAccept) (09 Â§2.9) |
| Target microservice | 13 Blood Bank |
| Target PG entity/table | T_BLOODREQUISITION, T_BLOODBAG, T_BLOODCOMPONENT, T_BLOODDONOR (10 Â§2.1 svc 13) |

## F36 â€” Blood Bag Issue / Trail / Discard & e-Raktkosh

| Chain element | Evidence |
|---|---|
| Business capability | Bag lifecycle (issue, return, discard, trail), expiry/decross jobs, national e-Raktkosh sync (M17) |
| Legacy screen | `bloodBank:issueBloodBag`, `bloodBank:bagReturns`, `bloodBank:bloodBagDiscard`, `bloodBank:BloodDiscardList`, `bloodBank:bloodBagTrail`, `bloodBank:retrunBloodbag`, `bloodBank:bloodBagOpeningStock`, `bloodBank:eraktkoshRequest`; `bagIssueConfirmation.html`, `bagReturnDetailsSchema.json`, `bloodBagTrail` (03 Â§2.7) |
| FAUG schema | `bagReturnDetailsSchema`, `showBloodDiscardListSchema`, `addBloodBagOpeningStock.json` (02 M17) |
| JS function | `useBagLifecycle`, `useIssueBag` (BagPick, SignatureCapture, issue label/barcode) (09 Â§2.9) |
| API | `bloodtestmaster.issue*`, `bloodtestmaster.bag*`, `bloodtestmaster.openingStock*`, `eraktkosh*` (04) |
| Java controller/service | `pas.BloodBankServiceImpl` (issue/decross :7657-7701); `jobs.ApplicationJobs.updateExpiryStatusOfTheBloodBagsJob` (23:00, :769-815), `decrossBloodBag` (00:00, :1045-1061), `updateExpiredBloodRequisitionStatusJob` (:734-766) (06 BR-BLD-002/003/004) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | T_BLOODBAGISSUE, T_BLOODBAGISSUEDEMANDSLIP, T_BLOODBAGRETURNS, T_BLOODBAGDECROSS, t_bloodbagcollection (02 M17) |
| Integration | e-Raktkosh (eraktkoshdataupdateservice, EraktkoshComponentcodeMapping; ApplicationJobs:1205,1383), Solr `patient` (donor/bag indexing BloodBankServiceImpl:1429-1625) |
| Business rules | BR-BLD-002 (auto-decros after 2880 min), BR-BLD-003 (bag expiry today â†’ isbagexpired; crossmatchedâ†’decross), BR-BLD-004 (requisition expiry), A7 (issuable bag predicate) |
| Target React component | `IssueBagPage`, `BagTrailPage`, `DiscardPage`, `ReturnBagPage` (09 Â§2.9) |
| Target microservice | 13 Blood Bank; 25 Interop (e-Raktkosh) |
| Target PG entity/table | T_BLOODBAG, T_BLOODCOMPONENT (10 Â§2.1 svc 13) |

## F37 â€” Diet Order

| Chain element | Evidence |
|---|---|
| Business capability | IP diet orders, kitchen menu production, ward-wise diet lists, sign-off (M10) |
| Legacy screen | `diet:dietSchedule`, `diet:DietKichenView`, `diet:dietMenuItems`, `diet:dietScheduleStatus`; `diet_foodorder_schema.json`, `diet_menu_item_schema.json`, `diet_schedule_schema.json`, `addDiet.html`, `dietKitchenViewDetailsList.html` (03 Â§2.12) |
| FAUG schema | `fjs-config_diet.json` (4 keys) (02 M10) |
| JS function | `useDietOrder`, `useKitchen` (09 Â§2.11) |
| API | `/diet/*` (30: addpatdietorder, updateDietFoodStatusForCancel) (04 findings Â§2) |
| Java controller/service | `pas.DietServiceImpl` (30) (04 findings Â§2) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | T_DIETORDER, T_DIETITEMSCHEDULEDETAILS, T_TODAYDIETITEMMENU, M_DIETITEMMASTER, M_DIETSESSIONMASTER, M_DIETTYPEMASTER (02 M10) |
| Integration | - |
| Business rules | - |
| Target React component | `DietOrderPage` (`C:PatientBanner`, `F:DietLines`), `KitchenPage`, `MenuPage` (09 Â§2.11) |
| Target microservice | 14 Diet |
| Target PG entity/table | T_DIETORDER, T_DIETITEMSCHEDULEDETAILS, T_TODAYDIETITEMMENU (10 Â§2.1 svc 14) |

## F38 â€” Linen Request

| Chain element | Evidence |
|---|---|
| Business capability | Ward linen requisition, dispatch/handover/receive tracking (M19) |
| Legacy screen | `linenlaundry:showLinenLaundryStatus`; `showCurrentLinenStatusSchema.json`, `createLinenRequest.html`, `dispatchLinenItems.html`, `handOverLinenItems.html` (03 Â§2.12) |
| FAUG schema | `fjs-config_linenlaundry.json` (1 key) (02 M19) |
| JS function | `useLinen` (09 Â§2.11) |
| API | `/linenLaundry/*` (14) (04 findings Â§2) |
| Java controller/service | `pas.LinenLaundryServiceImpl` (14) (04 findings Â§2) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - (LinenLaundryServiceImpl is one of 30 live-proc callers â€” 05 Â§6) |
| Tables | T_LINENREQUEST(+DETAILS), M_LINENCATEGORY, M_LINENDETAIL (02 M19; 05 Â§3.2 20 refs) |
| Integration | - |
| Business rules | - |
| Target React component | `LinenPage` (`RequestForm`, `DispatchLines`, `ReceivePanel`) (09 Â§2.11) |
| Target microservice | 16 Linen & Laundry |
| Target PG entity/table | T_LINENREQUEST, T_LINENCATEGORY, T_LINENDETAIL (10 Â§2.1 svc 16) |

## F39 â€” Inventory Indent (2-Level Approval)

| Chain element | Evidence |
|---|---|
| Business capability | Ward indent â†’ freeze â†’ level-1/2 approval â†’ issue; e-Aushadhi sync (M13) |
| Legacy screen | `inventory:indentListofStore`, `inventory:purchaseIndent`; `showIndentListSchema.json`, `addNewIndentSchema.json`, `showIndentDetailsSchema.json` + OnFreezed/OnApproved/Editable variants (03 Â§2.8) |
| FAUG schema | `fjs-config_inventory.json` (70 keys) (02 M13) |
| JS function | `useIndent` (addNewIndent, IndentFreezeOption, indentUpdateApprove, cancelIndent) (09 Â§2.8) |
| API | POST `/inventoryStockList/addIndent`, `/freezIndentLists`, `/approveIndentLists`, `/approvalForIndentIssue`; GET `/getIndentDetailsById`, `/getIndentListByStoreId`, `/getAllApprovalCount` (04: InventoryServiceImpl 235) |
| Java controller/service | `custom.InventoryServiceImpl` (indent lifecycle); AOP hook `ServiceLayerAspect:1204-1213` (e-Aushadhi reTrigger) (07 Â§3.2 #8) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | `CIMS_ADDINDENT` (10 Â§2.1 svc 10 legacy sources) |
| Tables | T_INDENT, T_INDENTDETAILS, T_ITEMSTOCK (02 M13) |
| Integration | Redis `INV_RI_<indentId>` receive lock (ServiceLayerAspect:1192, 60s); e-Aushadhi (`/eAushadiIndent/indent`, `indentIssueSucces`, `returnIndentForEaushadi`) |
| Business rules | M13 status machine (draft/freezed/approved/issued per schema names; 02 M13) |
| Target React component | `IndentPage` (`F:IndentComposer`, `C:ApprovalPanel` 2-level, `F:FreezePanel`) (09 Â§2.8) |
| Target microservice | 10 Inventory & Procurement |
| Target PG entity/table | T_INDENT, T_PURCHASEORDER, T_GRN*, T_ITEMSTOCK, T_ITEMBATCHMASTER (10 Â§2.1 svc 10) |

## F40 â€” Purchase Order & GRN

| Chain element | Evidence |
|---|---|
| Business capability | PR â†’ quotation â†’ PO (approve/freeze) â†’ GRN receipt + QC â†’ stock (M14) |
| Legacy screen | `inventory:purchaseRequest`, `inventory:quotation`, `inventory:currentPO`, `inventory:allApproval`, `inventory:grn`, `inventory:gRNList`, `inventory:approveGrn`, `inventory:multipurposeGRN`, `inventory:consignmentGRNList`, `inventory:QcStatusUpdate`; `showPurchaseRequestSchema.json`, `compareQuotationSchema.json`, `approvePOPage.html`, `showGRNSchema.json`, `qcStatusUpdate.json` (03 Â§2.8) |
| FAUG schema | `fjs-config_inventory.json` PO/GRN schemas (02 M14) |
| JS function | `usePurchaseOrder` (showCurrentPurchaseOrderList1, cancelCurrentPOList, calculateTotalAmounts), `useGrn` (showRelatedGRN), `useQuotation` (09 Â§2.8) |
| API | POST `/inventoryStockList/addAddPOOrder`, `/addAddPOIndent`, `/freezePurchaseOrder`, `/addGRN`, `/addProvisionalGRN`, `/approveProvisionalGRN`, `/approveProvisionalGRNNew`, `/addConsignmentGRN`, `/convertConsignedGRNToGRN`; GET `/getGRNListForSearch`, `/getApprovGRNListForSearch`, `/getPoStatusCounts`; `/quotation/*` (7), `/purchaseorder/*` (1) (04) |
| Java controller/service | `custom.InventoryServiceImpl` (PO/GRN ops, 235 endpoints); `pas.PurchaseOrderServiceImpl` (1); `pas.QuotationServiceImpl` (7) (04 findings Â§2) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | `CIMS_ADDGRN` (10 Â§2.1 svc 10 legacy sources) |
| Tables | T_PURCHASEORDER(+DETAILS), T_PURCHASEREQUEST(+DETAILS), T_QUOTATION, T_RATECONTRACT(+DETAILS), T_GRN*, T_ITEMSTOCK, T_INDENTINVOICE(+DETAILS) (02 M14) |
| Integration | e-Aushadhi (`/eAushadiIndent/updateQCStatus`, stock sync), RMRS PO requests; Solr `item` (stock/expiry flags) |
| Business rules | M14 statuses (PO freezed/approved; GRN QC) (02 M14) |
| Target React component | `PurchaseRequestPage`, `QuotationPage`, `PurchaseOrderPage` (`C:ApprovalPanel`), `GrnPage` (`F:QcPanel`, `C:ApprovalPanel`) (09 Â§2.8) |
| Target microservice | 10 Inventory & Procurement |
| Target PG entity/table | T_PURCHASEORDER, T_GRN*, T_INDENT, T_SUPPLIER, T_RATECONTRACT, T_QUOTATION (10 Â§2.1 svc 10) |

## F41 â€” Stock Adjustment & Item Enquiry

| Chain element | Evidence |
|---|---|
| Business capability | Stock corrections with reason codes; multi-store item enquiry (M13) |
| Legacy screen | `inventory:stockAdjustmentList`, `inventory:StoreStockAdjustment`, `inventory:itemEnquiry`; `showStockAdjustmentListSchema.json`, `addItemsforStockAdjustment.html`, `itemEnquirySchema.json` (03 Â§2.8) |
| FAUG schema | `stockAdjustmentListSchema`, `itemEnquirySchema` (02 M13) |
| JS function | `useStockAdjustment`, `useItemEnquiry` (09 Â§2.8) |
| API | POST `/inventoryStockList/addItemStockAdjustmentDetails`, `/addReasonForStockAdjustment`; GET `/getItemsEnquiryDetails`, `/getItemsEnquirySuppliers`, `/printItemEnquiryDetails`; POST `/addtemEnquiry` (04) |
| Java controller/service | `custom.InventoryServiceImpl` (adjustment/enquiry ops) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | T_ITEMSTOCK, stock-adjustment tables, M_REORDERANDSAFETYMASTER (02 M13) |
| Integration | Solr `item` (enquiry over item index) |
| Business rules | - |
| Target React component | `StockAdjustmentPage`, `ItemEnquiryPage` (09 Â§2.8) |
| Target microservice | 10 Inventory & Procurement |
| Target PG entity/table | T_ITEMSTOCK, T_ITEMBATCHMASTER (10 Â§2.1 svc 10) |

## F42 â€” Nurse Worklist & Task Completion

| Chain element | Evidence |
|---|---|
| Business capability | Task-driven nursing worklists, order-task completion, goals (M09/M26) |
| Legacy screen | `nursing:roleNurse`, `nursing:roleNurseIncharge`, `worklist:NurseTaskWorklist(+New)`, `worklist:NursePatientWorklist`, `worklist:showPatientWorkList`, `worklist:AlliedStaffTaskWorklist`; `showNurseWorklistSchema.json`, `nurseTaskList.json` (03 Â§2.4, Â§2.12) |
| FAUG schema | `fjs-config_nursing.json` (28 keys), `fjs-config_worklist.json` (02 M09/M26) |
| JS function | `useNurseWorklist` (getRoleNursePatientList, getAllIPPatientFilteredList ports) (09 Â§2.5) |
| API | `/nursingList/*` (300 endpoints â€” largest family) (04 findings Â§2) |
| Java controller/service | `nurse.NursingServiceImpl` (300: worklist, completeNursingOrderTask); `custom.ClinicalServiceImpl` (4) (04 findings Â§2) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | T_NURSETASKDETAILS, T_NURSETASKFREQUENCYDETAILS, T_PATIENTVITALS (02 M09/M26) |
| Integration | - |
| Business rules | A9 (task statuses); nurse task frequencies |
| Target React component | `NurseWorklistPage` (`WardSelector`, `PatientTaskList`, `GoalsPanel`) (09 Â§2.5) |
| Target microservice | 12 Nursing & Inpatient Care |
| Target PG entity/table | T_NURSENOTE, T_PATIENTVITALS, T_NURSETASKDETAILS (10 Â§2.1 svc 12) |

## F43 â€” Nurse Notes (TIU-XML) & Signatures

| Chain element | Evidence |
|---|---|
| Business capability | Note tiles (initial/progress/procedure/addendum), sign/verify, 368 TIU forms (M09/M40) |
| Legacy screen | `nursing:rolePatientNote`, `nursing:patientNotes/:patientId/:admId/:patientIen/:visitIen/:noteIen/:noteTitleId`; `showNursePatientNoteSchema.json`, `addNursesNotes.html`, `notesTitle.json`, `noteSummary.html` (03 Â§2.4) |
| FAUG schema | `tiuxml.json` (219 KB) + `tiuxml_mapping.json`; 368 tiuxmls schema keys (02 M40) |
| JS function | tiuxml.js `processTemplate`; `useNotes` (sign/verify flows), `useTiuxmlForm` (09 Â§2.5) |
| API | `nursingList.notes*`, `nursingList.sign*` (signNotesFromTask, signTemplate); `/notedetailsdata/*` (6); `tiuxml.*` save/load (04 findings Â§2) |
| Java controller/service | `nurse.NursingServiceImpl` (notes); `custom.NoteDetailsServiceImpl` (6); `custom.ClinicalServiceImpl.getNoteContent` (04 findings Â§2) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | T_PATIENTNOTE, NurseNote, T_EMRDETAILS (02 M09) |
| Integration | tiuxmls forms engine (gender/age mapped), ABDM EHR sync of notes (syncEHR) |
| Business rules | A1 (note/visit status interplay) |
| Target React component | `NotesPage` (`NoteTiles`, `NoteEditor`, `SignatureCapture`, `AddendumPanel`); `TiuxmlFormPage` (09 Â§2.5) |
| Target microservice | 4 Ordering & Prescription (notes/EMR) / 12 Nursing |
| Target PG entity/table | T_PATIENTNOTE, T_EMRDETAILS, T_EMRSERVICES (10 Â§2.1 svc 4) |

## F44 â€” Vitals & Charting

| Chain element | Evidence |
|---|---|
| Business capability | Vitals entry with qualifiers/ranges, observation flow-sheets, partograph, growth charts (M09) |
| Legacy screen | `charting:chartingApp`, `charting:chartingExpand`, `growthchartdetails/add/:id`; `charts/ODCWF_*.json` (29 formats), `partoGraph.html`, `chartVitalRangeSlider.html`, `vitalsConfiguration.json` (03 Â§2.4) |
| FAUG schema | `vitalsConfiguration.json` (sliderType slider/dualslider, unit ranges, qualifiers) (09 Â§3) |
| JS function | ChartingApplication.js; `useVitals`, `useChartConfig` (09 Â§4) |
| API | `nursingList.vitals*`, `nursingList.chartData*`, `nursingList.cwf*`, `nursingList.parto*` (04) |
| Java controller/service | `nurse.NursingServiceImpl` (vitals/charting endpoints); `custom.CWFServiceImpl` (17) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | T_PATIENTVITALS, T_INTAKEOUTPUT (10 Â§2.1 svc 12 owned) |
| Integration | - (chart JSON configs are static assets) |
| Business rules | Vital qualifier/range config semantics (09 Â§3 VitalRangeSlider) |
| Target React component | `VitalRangeSlider`, `Partograph`, `GrowthChart`, `ObservationFlowSheet` (09 Â§3) |
| Target microservice | 12 Nursing & Inpatient Care |
| Target PG entity/table | T_PATIENTVITALS, T_INTAKEOUTPUT (10 Â§2.1 svc 12) |

## F45 â€” Shift Handover

| Chain element | Evidence |
|---|---|
| Business capability | Multi-patient shift handoff (meds/labs/rad/procedure/transfer done-due lists) (M09) |
| Legacy screen | `shifthandover:multipleShiftHandOff`; `patientDetailsForMultiShiftHandover.html`, shifthandover templates (03 Â§2.12) |
| FAUG schema | `fjs-config_shifthandover` (02 M09 submodule) |
| JS function | `useShiftHandover` (09 Â§2.5) |
| API | `nursingList.handover*`; `nursingList/deleteNursePatientAllocationOnShiftHandOver` (03 Â§3) |
| Java controller/service | `nurse.NursingServiceImpl` (handover endpoints) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | T_NURSETASKDETAILS (allocation release), nursing station masters (02 M09) |
| Integration | - |
| Business rules | - |
| Target React component | `ShiftHandoverPage` (`PatientMultiSelect`, `HandoverSections`) (09 Â§2.5) |
| Target microservice | 12 Nursing & Inpatient Care |
| Target PG entity/table | T_NURSETASKDETAILS, nursing station masters (10 Â§2.1 svc 12) |

## F46 â€” Incident Reporting

| Chain element | Evidence |
|---|---|
| Business capability | Nursing incident capture, severity/status, resolution (M09) |
| Legacy screen | `nursing:incidentReporting`, `nursing:addnewIncident/:...`, `nursing:editIncident/:...`, `nursing:resolveIncident/:...`; `incidentReportSchema.json`, `incidentResolveSchema.json` (03 Â§2.4) |
| FAUG schema | `incidentReportSchema`, `incidentResolveSchema` (02 M09) |
| JS function | `useIncidentReport` (IncidentForm, IncidentResolve, `C:AuditHistory`) (09 Â§2.5) |
| API | `nursingList.incident*` (04) |
| Java controller/service | `nurse.NursingServiceImpl` (incident endpoints) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | - (incident tables not enumerated in 05) |
| Integration | - |
| Business rules | Incident statuses (02 M09 statuses) |
| Target React component | `IncidentReportPage` (09 Â§2.5) |
| Target microservice | 12 Nursing / 20 Grievance (resolution) |
| Target PG entity/table | UNKNOWN (not listed in 10 Â§2.1) |

## F47 â€” Nurse / Patient Allocation

| Chain element | Evidence |
|---|---|
| Business capability | Nurseâ†”ward/doctor/patient, alliedâ†”clinic allocation (M09) |
| Legacy screen | `allocation:*` (11 routes: nurseWardAllocation, doctorNurseAllocation, nursePatientAllocation, alliedClinicAllocation, ...); `nurseWardAllocationSchema.json`, `showNurseWardAllocationWrap.html` (03 Â§2.12) |
| FAUG schema | `nurseListFor*AllocationSchema.json` (03 Â§2.4) |
| JS function | `useNurseAllocation` (09 Â§2.11) |
| API | `nursingList.allocation*` (04) |
| Java controller/service | `nurse.NursingServiceImpl` (allocation endpoints; deleteNursePatientAllocationOnShiftHandOver) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | T_NURSEPATIENTDETAILS (seq T_nursePatientDetails_seq â€” 05 Â§4) |
| Integration | - |
| Business rules | - |
| Target React component | `AllocationPage` per-type (`AllocationTable`, `ShiftEditor`) (09 Â§2.11) |
| Target microservice | 12 Nursing & Inpatient Care |
| Target PG entity/table | T_NURSEPATIENTDETAILS (10 Â§2.1 svc 12) |

## F48 â€” CSSD Kit Lifecycle (Formation â†’ Sterilization â†’ Issue)

| Chain element | Evidence |
|---|---|
| Business capability | Kit formation, preparation, sterilization (machine/quality test), issue to OT procedures (M11) |
| Legacy screen | `cssd:kitMaster`, `cssd:kitFormulation`, `cssd:kitPreparation`, `cssd:cssdItemKitSterilization`, `cssd:procedureCssdRequest`, `cssd:procedureCssdIssue`, `cssd:CSSDItemIssueNew`, `cssd:CSSDItemReceiveandReturn`; `addKitMaster.html`, `addKitPreparation.html`, `cssdItemKitSterilization.html` (03 Â§2.10) |
| FAUG schema | `fjs-config_cssd.json` (15 keys) (02 M11) |
| JS function | `useKitMaster`, `useSterilization` (09 Â§2.11) |
| API | `/cssd/*` (46: addKitMaster, addItemSterilizationDetails, addKitRequestFromKitMaster, cssdItemIssue, freezeKitRequest, updateStatusProcedureCSSDKitDetails) (04 findings Â§2) |
| Java controller/service | `cssd.CSSDServiceImpl` (46) (04 findings Â§2) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | T_CSSDKITFORMATION(+DETAILS), T_CSSDITEMSTERILIZATIONDETAILS, T_CSSDKITISSUEITEM(+DETAILS), T_PROCEDURECSSDREQUEST(+DETAILS), T_PROCEDURECSSDKITDETAILS, T_PROCEDURECSSDISSUE(+DETAIL) (02 M11) |
| Integration | Solr `item` (kit/compound flags in ItemSolrView, 07 Â§1.2); inventory store dispatch |
| Business rules | Sterilization expiry master (M_STERILIZATIONEXPIRYMASTER) |
| Target React component | `KitMasterPage` (`F:KitTree`), `SterilizationPage` (`C:ApprovalPanel`), `DispatchPage` (09 Â§2.11) |
| Target microservice | 15 CSSD |
| Target PG entity/table | T_CSSDKITFORMATION, T_CSSDKITISSUEITEM, T_STERILIZATION*, T_PROCEDURECSSD* (10 Â§2.1 svc 15) |

## F49 â€” Equipment Complaint & Preventive Maintenance

| Chain element | Evidence |
|---|---|
| Business capability | Equipment complaints â†’ resolution; PM schedules & checklists; calibration (M20) |
| Legacy screen | `equipments:complaintDetails`, `equipments:complaintResolutionList`, `equipments:preventiveMaintenance`, `equipments:preventiveMaintenanceSchedule`, `equipments:preventiveCalibrationCheckList`, `equipments:calibrationMaintenance`; `addNewComplaintSchema.json`, `complaintResolutionView.html`, `addNewPrevetiveMaintncSchedu.html` (03 Â§2.12) |
| FAUG schema | `fjs-config_equipments.json` (34 keys) (02 M20) |
| JS function | `useComplaints`, `usePmSchedule` (09 Â§2.11) |
| API | `/complaintdetails/*` (11), `/complaintresolutiondetail/*` (5), `/preventivemaintenance/*` (20), `/preventivemaintenanceschedule/*` (1), `/equipment/*` (51) (04 findings Â§2) |
| Java controller/service | `pas.ComplaintDetailsServiceImpl` (11), `pas.ComplaintResolutionDetailServiceImpl` (5), `pas.PreventiveMaintenanceServiceImpl` (20), `pas.EquipmentServiceImpl` (51) (04 findings Â§2) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | T_EQPCOMPLAINT(+DETAILS), T_EQPCOMPLAINTRESOLUTION(+DETAILS), T_EQPPM*(+DETAILS), T_EQPPREVENTIVEMAINTENANCE, M_EQUIPCHECKLIST, M_EQUIPMENTPREVENTIVECHECKLIST, M_FUNCTIONALCHECKLISTMASTER, M_ELECTRICALCHECKLISTMASTER (02 M20) |
| Integration | - |
| Business rules | Complaint status machine (M_COMPLAINTSTATUSMASTER) |
| Target React component | `ComplaintPage` (`F:ComplaintForm`, `C:AuditHistory`), `PmSchedulePage` (09 Â§2.11) |
| Target microservice | 18 Equipment Management |
| Target PG entity/table | T_EQUIPMENT, T_EQPCOMPLAINT(+RESOLUTION), T_EQPPM* (10 Â§2.1 svc 18) |

## F50 â€” Transport Request & Vehicle Allocation

| Chain element | Evidence |
|---|---|
| Business capability | Patient/doctor ambulance requests, vehicle in/out, fuel, maintenance (M21) |
| Legacy screen | `transport:patientTransportRequest`, `transport:doctorTransportRequest`, `transport:vehicleIn`, `transport:vehicleOut`, `transport:vehicleStatus`, `transport:tripManagement`, `transport:fuelStationDetails`, `transport:vehicleMaintenance`; `patientTransportRequestSchema.json`, `vehicleInSchema.json`, `vehicleStatusSchema.json` (03 Â§2.12) |
| FAUG schema | `fjs-config_transport.json` (11 keys) (02 M21) |
| JS function | `useTransport` (09 Â§2.11) |
| API | `/transport/*` (31: allocateVehicle, approveTransportRequest, cancelTransportRequest) (04 findings Â§2) |
| Java controller/service | `pas.TransportServiceImpl` (31) (04 findings Â§2) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | T_TRANSPORTREQUEST(+DETAILS), T_VEHICLEMAINTENANCE, FuelStationDetails, M_AMBULANCEMASTER, M_DRIVERMASTER, M_ACCIDENTTYPE, M_WORKSHOPMASTER (02 M21) |
| Integration | Jeevanvahini (108 ambulance push/fetch, risl.jeevanvahini.api.*) (07 Â§3.1 #19) |
| Business rules | Vehicle status machine (vehicleStatusSchema) |
| Target React component | `TransportRequestPage` (`RequestForm`, `VehicleDriverPick`), `VehicleMasterPage`, `TripPage` (09 Â§2.11) |
| Target microservice | 17 Transport |
| Target PG entity/table | T_TRANSPORTREQUEST, T_VEHICLEMAINTENANCE, M_AMBULANCEMASTER, M_DRIVERMASTER (10 Â§2.1 svc 17) |

## F51 â€” BMW Tracking

| Chain element | Evidence |
|---|---|
| Business capability | Biomedical waste request, bag barcode, handover to firm (M18) |
| Legacy screen | `biomedicalwaste:showBioMedicalWasteStatus`; `showBioMedicalWasteStatusSchema.json`, `createBioMedicalRequest.html`, `generateBioWasteBagBarcode.html`, `decomposeBioWaste.html` (03 Â§2.12) |
| FAUG schema | `fjs-config_biomedicalwaste.json` (1 key) (02 M18) |
| JS function | `useBioWaste` (09 Â§2.11) |
| API | `/bioMedicalWasteManagement/*` (14: addNewBioRequest, updateBagStatus, validateBarcode, handOverBags, handedOverToFirm, getBagWeightByBarcode) (04 findings Â§2) |
| Java controller/service | `BioMedicalWasteManagementServiceImpl` (14) (04 findings Â§2) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | T_BIOMEDICALWASTE(+DETAILS), M_WASTEBAGMASTER (02 M18; 05 Â§3.2 18/10 refs) |
| Integration | - |
| Business rules | BMW waste status (02 M18) |
| Target React component | `BioWastePage` (`RequestForm`, `BagBarcode`, `DecomposePanel`) (09 Â§2.11) |
| Target microservice | 19 Bio-Medical Waste |
| Target PG entity/table | T_BIOMEDICALWASTE(+DETAILS), T_WASTEBAGMASTER (10 Â§2.1 svc 19) |

## F52 â€” Mortuary & Death Certification (CDR)

| Chain element | Evidence |
|---|---|
| Business capability | Mortuary box custody, death form/certificate, child death review 4A/4B (M28) |
| Legacy screen | `commons:mortuaryDetailsList`, `commons:mortuaryBoxManagement`, `commons:mortuaryRelease`, `commons:mortuaryDischargeList`, `nursing:showMortuary`, `ip:cdrForm4A/4B`; `mortuarySchema.json`, `mortuaryBoxManagement.json`, `death_form.vm`, `death_Certificate.vm`, `cdr_form_4A.vm` (03 Â§2.12, 02 M28) |
| FAUG schema | `mortuarySchema`, `mortuaryClearanceSchema`, `mortuaryBoxManagement` (02 M28) |
| JS function | `useMortuary` (09 Â§2.5) |
| API | `/patient/getMortuaryPatients`, `/patient/getPatientDetailsForDeathForm`, `/patient/updatePatientDeathDetails` (04: PatientServiceImpl 94) |
| Java controller/service | `pas.PatientServiceImpl` (mortuary/death); `custom.PatientTransferDischargeServiceImpl` (death handling BR-DIS-002) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | `CIMS_ADDBEDDISCHARGE` (death path :24-41), `CIMS_UPDATEIPDISDISCHARGE` |
| Tables | ChildDeathReview, T_REGISTRATION_DETAILS (isdeath), PatientDeathDetails, T_PATIENTDISCHARGEDATA (02 M28, 06 BR-DIS-002) |
| Integration | Solr `patient` (death=true partial update); tiuxml Post_Mortem/Death_Certificate/DEATHNOTE |
| Business rules | BR-DIS-002 (IsExpired=1 death flow), A4 (IsClosed) |
| Target React component | `MortuaryPage` (`MortuaryBoxGrid`, `BoxAllocation`, `ClearancePanel`) (09 Â§2.5) |
| Target microservice | 3 ADT & Bed |
| Target PG entity/table | T_PATIENTDISCHARGEDATA (10 Â§2.1 svc 3) |

## F53 â€” Grievance Ticket & Resolution

| Chain element | Evidence |
|---|---|
| Business capability | Grievance tickets, severity/escalation, resolution assignment, feedback (M22) |
| Legacy screen | `grievance:grievancePage`, `grievance:grievanceRaise`; `raiseGrievanceTicketSchema.json`, `grievanceSchema.json`, `grievanceTicketStatusList.html`, `changeTicketStatus.html`, `assignedTicketToUser.html`, `grievanceFeedbackForm.html` (03 Â§2.12) |
| FAUG schema | `fjs-config_grievance.json` (02 M22) |
| JS function | `useGrievance` (09 Â§2.11) |
| API | `grievance.*` â€” **UNKNOWN** backend class/path (02 M22 UNKNOWN-25; screens exist in fjs-config_grievance; endpoint mapping not established in 04) |
| Java controller/service | UNKNOWN (not enumerated in 04 findings Â§2 service registry) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | T_PATIENTGRIEVANCEHISTORY, T_PATIENTGRIEVANCECOMMENTS, T_PATIENTGRIEVANCEFEEDBACK, GrievanceDocuments, ResolutionAsignee, M_GRIEVANCECATEGORYMASTER (+sub/severity) (02 M22) |
| Integration | - |
| Business rules | Grievance status via history (02 M22) |
| Target React component | `GrievancePage` (`TicketDetail`, `AssignPanel`, `AuditHistory`) (09 Â§2.11) |
| Target microservice | 20 Grievance & Feedback |
| Target PG entity/table | T_PATIENTGRIEVANCE*, T_PATIENTCOMPLAINTS, T_FEEDBACKTEMPLATE (10 Â§2.1 svc 20) |

## F54 â€” RMRS Collection & Issue

| Chain element | Evidence |
|---|---|
| Business capability | Authority contracts, payment collections, issued payments, journal/suspense (M23) |
| Legacy screen | `rmrs:addcollection/add`, `rmrs:addcollection/view`, `rmrs:showPaymentCollection`, `rmrs:issuePayment`, `rmrs:suspenseAccountDetail`, `rmrs:showRMRSDashboard`; `AddCollection`, `PaymentCollection`, `IssueCollection`, `suspenseAccountDetailsSchema.json`, `RMRSDashboard` (03 Â§2.12) |
| FAUG schema | `fjs-config_rmrs.json` (9 keys) (02 M23) |
| JS function | `useRmrs` (09 Â§2.11) |
| API | `/RMRS/*` (40: addCollection, IssueCollection, approveOrRelease, getAllTransactionStatusList, journal, bank statement) (04 findings Â§2) |
| Java controller/service | `pas.RMRSServiceImpl` (40; reads collection details :1361-1366) (04 findings Â§2, 10 Â§4) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | T_COLLECTIONHEADER, T_COLLECTIONDETAIL, T_RMRSCOLLECTION, T_SUSPENSEACCOUNTDETAILS, RMRSDashboardTransaction, M_AUTHORITYMASTER, M_HEADSMASTER (02 M23) |
| Integration | Payment â†’ RMRS via PaymentReceived event (10 Â§4); RMRS PO requests (M14) |
| Business rules | Transaction status list (getAllTransactionStatusList) |
| Target React component | `RmrsDashboardPage` (`PaymentForm`, `JournalGrid`), `RmrsIssuePage` (09 Â§2.11) |
| Target microservice | 24 RMRS & PMRD (Treasury) |
| Target PG entity/table | T_RMRSCOLLECTION, T_AUTHORITYMASTER, T_HEADSMASTER, T_LEDGERMASTER, T_ACCOUNTING* (10 Â§2.1 svc 24) |

## F55 â€” Queue / Token Management & Kiosk

| Chain element | Evidence |
|---|---|
| Business capability | Tokens, queue boards (appointment/cabin/doctor), nurse desk, kiosk self-service (M27/M31) |
| Legacy screen | `queue:appointmentQueue`, `queue:cabinQueue`, `queue:doctorBatch`, `queue:callPatientList`, `queue:manageToken`, `queue:nurseDesk`, `queue:patientQueue`, `queue:userQueue`, `kiosk:selfRegPatient`, `kiosk:selfAppointment/...`; `appointmentQueueSchema.json`, `manageTokenSchema.json`, `departmentQueue10.html` (03 Â§2.12) |
| FAUG schema | `fjs-config_queue.json` (11 keys), `fjs-config_kiosk.json` (4 keys) (02 M27/M31) |
| JS function | queueEvtCls `stopAndStartCounterQueue` (queue app), `useTokenFlow`, `useQueueBoard` (live polling), `useKiosk` (09 Â§2.1, Â§3) |
| API | `/queueManagment/*` (23: addToQueue, assignNextPatientToDoctor, callGetPatientList); `/patient/kioskQueue`; `/walkinVisit/searchToken`, `/markandaddvisit/checkin` (04 findings Â§2) |
| Java controller/service | `QueueManagmentServiceImpl` (23); `pas.PatientServiceImpl` (kioskQueue, saveKioskLabResult, uploadKioskLabResult) (04 findings Â§2) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | T_PATIENTQUEUE, T_REGISTRATIONQUEUE, T_PATIENTTOKEN, T_OPDSTATIONMASTER (02 M27/M31; 10 Â§2.1 svc 29) |
| Integration | Redis QMS room token queues (`pushToQ/removeFromQ`, `<qName>_S` counters, `*_time` sums; QMSUtil:28-126); e-Mitra (kiosk appointments) |
| Business rules | A1 (visit status 3 = doctor called; 11 = billed) drives queue display |
| Target React component | `QueueBoardPage` (`QueueBoard`, `TokenCard`, `FactsPopup`), `AssignCabinPage`, `KioskPage` (`SelfRegistration`, `SlotPick`) (09 Â§2.11, Â§2.1) |
| Target microservice | 29 Queue & Kiosk |
| Target PG entity/table | T_PATIENTQUEUE, T_OPDSTATIONMASTER, T_TABLETMAPPING (10 Â§2.1 svc 29) |

## F56 â€” BCMA Medication Administration

| Chain element | Evidence |
|---|---|
| Business capability | Bedside barcode medication administration (5-rights), med log, IV chronology, missing doses (M30) |
| Legacy screen | `bcma:unitDose`, `bcma:scanWristband/:patientMrn`; `ShowMainScreenSchema`, `ShowUDTab`, `ShowIVPTab`, `bcmaMedicationLogNew.html`, `bcmaIVChronolgyHeld/Refused/MissingDose.html`, `bcmaNavMedVarianceLog.html` (03 Â§2.4/Â§2.12) |
| FAUG schema | `fjs-config_bcma.json` (11 keys) (02 M30) |
| JS function | bcma-module.js; `useBcma` (barcode + QZ label), `useIvChronology` (09 Â§2.11) |
| API | `medication.bcma*`, `pharmacy.bcma*` â€” **UNKNOWN** specific paths (02 M30 UNKNOWN-33) |
| Java controller/service | `custom.MedicationServiceImpl` / `custom.PharmacyServiceImpl` (BCMA endpoints assumed under these families â€” UNKNOWN exact methods) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | IHR_MEDICATION_TMP, T_INFUSIONMIXING(+DETAILS), T_PRESCRIPTIONDISPENSEDETAILS (02 M30) |
| Integration | html5-qrcode scanner (wristband barcodes), QZ Tray labels |
| Business rules | Med log statuses (held/missing dose; A9 patient-med 1/2/3/4) |
| Target React component | `BcmaPage` (`DueListNav`, `MedLog` scan mode, `IVBagChronology`, `VarianceLog`) (09 Â§2.11) |
| Target microservice | 9 Pharmacy (dispensing) / 12 Nursing (administration) |
| Target PG entity/table | T_PRESCRIPTIONDISPENSEDETAILS, T_INFUSIONMIXING (10 Â§2.1 svc 9) |

## F57 â€” Chemotherapy Planning

| Chain element | Evidence |
|---|---|
| Business capability | Chemo patient list, planning, regimen/cycle orders, meds entry, notes (M29) |
| Legacy screen | `chemotherapy:chemoPlanningList`, `chemotherapy:chemoPlanningFlow`, `chemotherapy:chemoNewRegimen`, `chemotherapy:chemoPlanCycleFlow`; `showChemoPatientListSchema.json`, `showChemoPlanningSchema.json`, `chemoNewRegimenView.html`, `chemoCycleMedOrdCheck.html`, `chemoMedsOrderEntry.html` (03 Â§2.12) |
| FAUG schema | `fjs-config_chemotherapy.json` (3 keys) + chemo_tiuxml_mapping.json (02 M29) |
| JS function | chemotherapy-module.js (733 KB); `useRegimen`, `useChemoMeds`, `useChemoNotes` (09 Â§2.11) |
| API | `pharmacy.chemo*`, `nursingList.chemo*` â€” **UNKNOWN** exact paths (02 M29 UNKNOWN-31) |
| Java controller/service | UNKNOWN (not enumerated in 04 findings Â§2 registry) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | (chemo uses careplan/medication tables â€” UNKNOWN, 02 M29 UNKNOWN-30) |
| Integration | chemo_tiuxml_mapping (Chemo_Performance_Status_Assessment, Chemo_SURGONCOHIST/EXAM) |
| Business rules | - |
| Target React component | `RegimenPage`, `ChemoMedsPage`, `ChemoNotesPage` (09 Â§2.11) |
| Target microservice | 4 Ordering & Prescription / 9 Pharmacy (IV compounding) |
| Target PG entity/table | UNKNOWN (not listed in 10 Â§2.1) |

## F58 â€” PMRD Records & Form J

| Chain element | Evidence |
|---|---|
| Business capability | Encounter record (IEN), medical record documents, JSY Form J1/J2/J3 (M24) |
| Legacy screen | `pmrd:patientMedicalRecord`, `record:hform`; `patientMedicalRecordWrap.html`, `patientEncounterRecord.html`, `uploadPatientDocumentsPmrd.html`, `formj1.html`, `hform.html` (03 Â§2.12) |
| FAUG schema | `fjs-config_pmrd.json` (2 keys) (02 M24) |
| JS function | `usePmrd` (09 Â§2.11) |
| API | `/pmrd/*` (4: getAllPatientEncounter, getPatientDocumentsForPMRD, getJsyFormDetails) (04 findings Â§2) |
| Java controller/service | `pas.PmrdServiceImpl` (4) (04 findings Â§2) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | (uses encounters + documents â€” UNKNOWN specific tables, 02 M24 UNKNOWN-27) |
| Integration | fileService (documents) |
| Business rules | - |
| Target React component | `PmrdPage` (`EncounterList`, `FileUpload`, `ImageAnnotation`, `PrintDialog` form J) (09 Â§2.11) |
| Target microservice | 24 RMRS & PMRD |
| Target PG entity/table | UNKNOWN (10 Â§2.1 svc 24 lists treasury tables only) |

## F59 â€” Interop: EHR / HL7 / WhatsApp Messaging

| Chain element | Evidence |
|---|---|
| Business capability | EHR file generation, HL7 v2 outbound queue, WhatsApp/SMS notifications with write-behind queue (M38) |
| Legacy screen | `commons:inbound`, `commons:outbound`, `commons:notificationTemplate`; `inbound.json`, `outbound.json` (03 Â§2.12) |
| FAUG schema | commons inbound/outbound schemas (02 M39) |
| JS function | `useNotificationBadges` (09 Â§4) |
| API | `/ehr/*` (8); `/sendEmail/*` (5); HL7 via Hl7InboundMessageJob (disabled); WhatsApp via T_WHATSAPPMESSAGEQUEUE drain (04 findings Â§2, 07 Â§3.2) |
| Java controller/service | `ehr.EHRUtilServiceImpl` (8; :91-1205); `SendNotificationServiceImpl` (5; :125-174); `jobs.Hl7InboundMessageJob` (:39-205); `util.HL7Helper` (outbound :217-1091); `jobs.ApplicationJobs.triggerWhatsappReportsInQueue` (:1598-1643) (07 Â§3.1 #9/#10/#15) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | T_EHRJOBDETAILS, T_OUTBOUNDHIS (47 refs â€” 05 Â§3.2), T_INBOUNDHIS, T_WHATSAPPMESSAGEQUEUE/STATUS, T_EMRDETAILS, T_EMRSERVICES (02 M38, 07 Â§3.2) |
| Integration | EHR dir (ehr.dir.path), HL7 v2 (HAPI; dormant), WhatsApp Business API + eSanchar SMS; ABDM EHR sync |
| Business rules | A9 (message queue retry counts, max trigger 5) |
| Target React component | `NotificationPage` (09 Â§2.11 commons) |
| Target microservice | 25 Interop & Notifications (Kafka topics replacing DB polls â€” 10 Â§2.1 svc 25) |
| Target PG entity/table | T_OUTBOUNDHIS, T_INBOUNDHIS, T_ABDMREQUESTQUEUE, T_WHATSAPPMESSAGEQUEUE/STATUS, T_EHRJOBDETAILS (10 Â§2.1 svc 25) |

## F60 â€” EJS / Aasmaan Field Programs

| Chain element | Evidence |
|---|---|
| Business capability | ASHA/ANC-PNC/child-care data, checklists, incentives; Aasmaan case/device backend (M36/M37) |
| Legacy screen | `masters:ejanswathyaConfig`, `masters:ejs`; `ejschecklistquestions`, `ejsmasteractivity`, `ejsmasterentity`; Aasmaan: no FAUG screens (02 M36/M37) |
| FAUG schema | `fjs-config_commons.json` ejs keys (02 M39) |
| JS function | vista-module.js (unverified, 02 M36); mobile apps (ohumlite) |
| API | `/ejs-asha/*` (8), `/ejs-ancpncchild/*` (20), `/ejs-checklists/*` (8), `/ejs-details/*` (54), `/ejs-incentive/*` (6), `/ejs-population/*` (9), `/ejs-supervisor/*` (12), `/ejs-masterLocation/*` (8); `/cases/*` (44), `/devices/*` (8), `/institutions/*` (8), `/e_tickets/*`, `/stats/*`, `/users/*` (Aasmaan) (04 findings Â§2) |
| Java controller/service | `ejs.EjsAshaServiceImpl`, `EjsAncPncChildServiceImpl`, `EjsChecklistsServiceImpl`, `EjsDetailsServiceImpl` (Solr :2105), `EjsIncentiveServiceImpl`, `EjsSupervisorServiceImpl`, `EjsPopulationServiceImpl`, `EjsMasterLocationServiceImpl`; `aasmaan.*` (10 impls) (04 findings Â§2, 02 M36/M37) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | ANDROID_* (ANC, PNC, CHILD_CARE, ASHA_WORK, INCENTIVES, FAMILY...), ES_CHECKLISTS*, MASTER_* geography, USER_*, PROFILE, INDICATORS, MEDIA* (57 EJS entities, 05 Â§3.3); AASMAANCASE* etc. (35, 05 Â§3.4) |
| Integration | Solr `patient` (outreach registration, EjsDetailsServiceImpl:2105); Payment transfer for incentives (M33); EJS incentive schedule job (ejs.asha.incentive.schedule) |
| Business rules | EJS checklist submission/approval status; incentive status (02 M36) |
| Target React component | `EjsPage`/field-app APIs (no UI mapping in 09); Aasmaan: mobile backend only |
| Target microservice | 28 Govt Programs (EJS/Aasmaan/Chiranjeevi) |
| Target PG entity/table | T_EJS_*, T_AASMAAN_*, T_BENEFICIARYENROLMENT, T_REFRENCETID (10 Â§2.1 svc 28) |

## F61 â€” NHCX Insurance / RGHS Scheme Flows

| Chain element | Evidence |
|---|---|
| Business capability | NHCX insurance-plan/eligibility/pre-auth/claim with JWE; RGHS/SIMS eligibility & TID (M32/M38) |
| Legacy screen | `nhcx` screens: `checkEligibilityWarp.html`, `preAuthWrap.html`; `rghsEligibilityCheckSchema`, `chiranjeeviapp.html`, `rcm:chiranjeeviapp` (03 Â§2.12) |
| FAUG schema | `fjs-config_rcm.json` (1 key rghsEligibilityCheckSchema); nhcx templates (insurance_plan_on_request, coverage_eligibility_on_check, preauth_on_submit, claim_on_submit) (02 M32/M38) |
| JS function | `useNhcEligibility` (`EligibilityForm`, `PolicyList`), `useRcm` (09 Â§2.11) |
| API | `/nhcx/*` (10: insurancePlanRequest, coverageEligiblityRequest, preAuthSubmit, claimSubmitRequest, fetchPolicies, allParticipants, fetchPayorCerts); `/rcm/*` (7); `/patient/getRghsTransactionsforIHMS`, `/getRghsDocumentByTransactionId`, `/getSimsRghsforIHMS` (04 findings Â§2) |
| Java controller/service | `abdm.NHCXServiceImpl` (10); `RcmServiceImpl` (7); `jobs.ApplicationJobs.rghsDataScheduler` (:1063-1092); `IntegrationHelper.addSimsRGHSData`; HCX crypto `xcg.hcx.*` (HCXPayloadEncryptor/Decryptor, KeyLoader) (07 Â§3.1 #2/#7) |
| Repository/DAO | GenericDAO/GenericQuery (05 Â§1) |
| SQL/Stored proc | - |
| Tables | T_NHCX_REQUEST(+_DETAILS), T_ELIGIBILITYCHECK, T_ADMISSION_TID, T_REFRENCETID, BENEFICIARY_ENROLMENT_IHMS (05 Â§3.5) |
| Integration | ABDM gateway/HCX (JWE, abdm.hcx.url), RGHS app + SIMS (rghsSimsData.api.url), Chiranjeevi (chiranjivi.*) |
| Business rules | A9 (NHCX request 0..4 types; status 0..3) |
| Target React component | `NhcEligibilityPage`, `RcmPage` (`F:ChiranjeeviForm`) (09 Â§2.11) |
| Target microservice | 23 RCM & Claims; 25 Interop (NHCX) |
| Target PG entity/table | T_ELIGIBILITYCHECK, T_ADMISSIONTIDDETAILS, T_NHCXREQUEST(+DETAILS) (10 Â§2.1 svc 23) |

---

## Summary: Traceability Coverage & Gaps

- **Features traced end-to-end: 61** (F01â€“F61), covering the required majors (registration, walk-in, appointment, admission, bed assignment, discharge, service ordering, bill generation, payment/advance/refund, prescription, dispensing, lab, radiology, OT, blood, diet, linen, indent/GRN, nurse tasks).
- **Strongest chains** (full evidence at every hop): F01/F04/F10/F15/F19/F20/F27/F35/F39/F40/F61.
- **Top gaps found** (links UNKNOWN in source docs):
  1. **Grievance (F53)** â€” no backend class/path in the 132-service registry (02 M22 UNKNOWN-25); frontend `grievance.*` calls unmapped.
  2. **Chemotherapy (F57)** â€” no API family, no tables (02 M29 UNKNOWN-30/31); only JS + tiuxml evidence.
  3. **BCMA (F56)** â€” no exact endpoint paths (02 M30 UNKNOWN-33); tables IHR_MEDICATION_TMP/T_INFUSIONMIXING only.
  4. **Aasmaan (M37)** â€” no `@Table` annotations, no FAUG screens (05 Â§3.4); DB names must be verified from Oracle.
  5. **PMRD tables (F58)** â€” UNKNOWN-27; only encounter+document usage evidenced.
  6. **File/Document service (M34)** â€” no dedicated bounded context in 10 Â§2.1 (blob target assigned provisionally to 25).
  7. **Nursing incident tables** â€” not enumerated in 05 (F46 tables "-").
  8. **grievance/chemo/bcma/PMRD target PG entities** â€” absent from 10 Â§2.1 ownership table.
  9. **EHR/HL7 outbound sender** â€” dormant/disabled in config (07 Â§3.2): no active outbound HL7 writer; eSign unwired (07 Â§3.1 #14).
  10. **Solr `service` collection** â€” orphaned (no indexer/reader; 07 Â§1.2) â€” dead config to drop in target.
- **Cross-cutting risks for migration** (from 06 Â§C): VisitStatus 11/12 split-brain (T_Appointment vs T_Encounter), non-self concession overwrite (BR-BILL-005 R2), DNR_REJECTED==DNR_DONATED==5 (A8), hard-coded BedStatusId=6 (BR-DIS-003 R5), dead APP015 admission check (BR-IPD-004 R4).

*End of TRACEABILITY-MATRIX â€” synthesized from docs 02/03/04/05/06/07/09/10 and agent-03/agent-04 discovery. No code re-exploration performed.*
