# iHMS / OHUM PAS NG — API Catalog (04)

Reverse-engineered from source (D:\ihms, Spring Boot 2.7.5 / CXF JAX-RS). All 3,060 REST endpoints
mapped by static annotation extraction. Base URL: `http://<host>:8080/IHMS/api/rest` (server.port=8080,
context-path=/IHMS, CXFServlet at `/api/*`, `<jaxrs:server address="/rest">`).

- **Method/URL**: JAX-RS annotations as declared (`file:line` evidence in findings.md).
- **Request**: `Q`=QueryParam, `P`=PathParam, `JSON`=BodyParam DTO, `FORM`=FormParam, `HDR`=HeaderParam.
- **Response**: declared return type (full signature in findings.md).
- **Tx**: Y = method carries `@Transactional(rollbackOn = Exception.class)` (javax.transaction) or class-level tx.
- **Tables**: filled for high-value endpoints; otherwise resolve via GenericDAO/GenericQuery in method body.

## Endpoint counts by verb
GET 1,441 | POST 1,369 | PUT 162 | DELETE 31 (+ 57 ABDM/HFR callbacks) = **3,060 endpoints** in **132 service classes** + ABDMController + HFRController (120 beans wired in services.xml, 11 more service beans registered by @Component but not wired into CXF — see findings.md §2).

## Notable cross-cutting flags
1. **Duplicated endpoints**: `POST /fileService/uploadHospitalLocation` exists twice in FileManagementServiceImpl (multipart vs JSON body) — CXF selects by content-type. Also `GET/POST /queueManagment/addToQueue` (intentional overload).
2. **ABDM dual-versioning**: ABDMController exposes `/{versionNumber}/...` (v2) AND `/api/v3/...` (v3) paths for the same callbacks, plus a catch-all `POST /{versionNumber}/{endpoint}/on_{action}` (ABDMController.java:1340).
3. **Stored-proc driven**: many endpoints annotate params `@StoredProcNamedParam` and return `*ResultDto` views generated from stored procedures (SP params in aspect `ServiceLayerAspect`).
4. **Report endpoints**: ~60+ `/xxxReport`-suffixed endpoints return print-ready data or String (HTML/PDF via Velocity templates).
5. Only **2 exact (verb+path) duplicate groups**; **28 same-path/different-verb groups** (mostly CRUD GET/POST pairs).

## Module table of contents
- AASMAAN 
- ABDM 
- ABDM callbacks (ABDMController / HFRController) 
- Authentication  
- Biomedical Waste  
- Clinical  
- CSSD 
- Custom 
- CWF 
- EHR 
- EJS 
- File Management  
- Integrations 
- Linen & Laundry  
- Notifications  
- Nursing  
- PAS 
- Patient Portal  
- Payments  
- Queue Management  
- RCM 

---

### Module: AASMAAN (Maternal-Child/Telemedicine)  (78 endpoints)

| Method | URL (under /api/rest) | Class : method | Request | Response | Tx | Tables | Notes |
|---|---|---|---|---|---|
| POST | `/app_transaction_log/` | `aasmaan.AasmaanAppTransactionLogServiceImpl.saveAppTransactionLog` | - | - | - | `AasmaanResponseDTO<Object>` | - |
| GET | `/cases/` | `aasmaan.AasmaanCaseServiceImpl.getCases` | Q Q Q Q  | `AasmaanResponseDTO<AasmaanCase>` | - | - | - |
| GET | `/cases/{case_id}` | `aasmaan.AasmaanCaseServiceImpl.getCase` | P  | `AasmaanResponseDTO<AasmaanCase>` | Y | - | - |
| GET | `/cases/{case_id}/administration_drugs` | `aasmaan.AasmaanCaseServiceImpl.getCaseAdministrationDrugs` | Q P  | `AasmaanResponseDTO<AasmaanCaseAdministrationDrug>` | Y | - | - |
| GET | `/cases/{case_id}/checklist1` | `aasmaan.AasmaanCaseServiceImpl.getCaseChecklist1` | P  | `AasmaanResponseDTO<AasmaanCaseChecklist1>` | - | - | - |
| GET | `/cases/{case_id}/checklist2` | `aasmaan.AasmaanCaseServiceImpl.getCaseChecklist2` | P  | `AasmaanResponseDTO<AasmaanCaseChecklist2>` | Y | - | - |
| GET | `/cases/{case_id}/checklist3` | `aasmaan.AasmaanCaseServiceImpl.getCaseChecklist3` | P  | `AasmaanResponseDTO<AasmaanCaseChecklist3>` | Y | - | - |
| GET | `/cases/{case_id}/checklist4` | `aasmaan.AasmaanCaseServiceImpl.getCaseChecklist4` | P  | `AasmaanResponseDTO<AasmaanCaseChecklist4>` | Y | - | - |
| GET | `/cases/{case_id}/delivery_notes` | `aasmaan.AasmaanCaseServiceImpl.getCaseDeliveryNotes` | P  | `AasmaanResponseDTO<AasmaanCaseDeliveryNote>` | Y | - | - |
| GET | `/cases/{case_id}/discharge_notes` | `aasmaan.AasmaanCaseServiceImpl.getCaseDischargeNotes` | P  | `AasmaanResponseDTO<AasmaanCaseDischargeNote>` | Y | - | - |
| GET | `/cases/{case_id}/events` | `aasmaan.AasmaanCaseServiceImpl.getCaseEvents` | P  | `AasmaanResponseDTO<AasmaanCaseEvent>` | Y | - | - |
| GET | `/cases/{case_id}/export` | `aasmaan.AasmaanCaseServiceImpl.exportCase` | P Q Q  | `AasmaanResponseDTO<Object>` | Y | - | - |
| GET | `/cases/{case_id}/field_monitoring` | `aasmaan.AasmaanCaseServiceImpl.getCaseFieldMonitoring` | P Q  | `AasmaanResponseDTO<AasmaanCaseFieldMonitoring>` | Y | - | - |
| GET | `/cases/{case_id}/highrisk` | `aasmaan.AasmaanCaseServiceImpl.getCaseHighrisk` | P  | `AasmaanResponseDTO<Object>` | - | - | - |
| GET | `/cases/{case_id}/info/{parameter}` | `aasmaan.AasmaanCaseServiceImpl.getCaseInfoParameter` | P P  | `AasmaanResponseDTO<Object>` | - | - | - |
| GET | `/cases/{case_id}/io_forms` | `aasmaan.AasmaanCaseServiceImpl.getCaseIoForms` | P  | `AasmaanResponseDTO<AasmaanCaseIoForm>` | Y | - | - |
| GET | `/cases/{case_id}/monitoring` | `aasmaan.AasmaanCaseServiceImpl.getCaseMonitoring` | P Q Q  | `AasmaanResponseDTO<AasmaanCaseMonitoring>` | - | - | - |
| GET | `/cases/{case_id}/pd_monitoring` | `aasmaan.AasmaanCaseServiceImpl.getCasePdMonitoring` | P  | `AasmaanResponseDTO<AasmaanCasePdMonitoring>` | Y | - | - |
| GET | `/cases/{case_id}/pnc_monitoring` | `aasmaan.AasmaanCaseServiceImpl.getCasePncMonitoring` | P  | `AasmaanResponseDTO<AasmaanCasePncMonitoring>` | Y | - | - |
| GET | `/cases/{case_id}/prescribed_drugs` | `aasmaan.AasmaanCaseServiceImpl.getCasePrescribedDrugs` | Q P Q  | `AasmaanResponseDTO<AasmaanCasePrescribedDrug>` | Y | - | - |
| GET | `/cases/{case_id}/referral_slip` | `aasmaan.AasmaanCaseServiceImpl.getCaseReferralSlip` | P  | `AasmaanResponseDTO<AasmaanCaseReferralSlip>` | - | - | - |
| GET | `/cases/{case_id}/schedule` | `aasmaan.AasmaanCaseServiceImpl.getCaseSchedule` | P Q  | `AasmaanResponseDTO<AasmaanCaseSchedule>` | Y | - | - |
| GET | `/cases/cmfs` | `aasmaan.AasmaanCaseServiceImpl.getCaseCmfs` | Q Q  | `AasmaanResponseDTO<AasmaanCaseCmfs>` | Y | - | - |
| GET | `/cases/is_unique_ipd` | `aasmaan.AasmaanCaseServiceImpl.getCaseIsUniqueIpd` | Q Q Q  | `AasmaanResponseDTO<Object>` | - | - | - |
| GET | `/cases/stage_counts` | `aasmaan.AasmaanCaseServiceImpl.getCaseStageCounts` | Q Q Q  | `AasmaanResponseDTO<AasmaanCaseStageCount>` | Y | - | - |
| POST | `/cases/` | `aasmaan.AasmaanCaseServiceImpl.saveCase` | JSON  | `AasmaanResponseDTO<AasmaanCase>` | - | - | - |
| POST | `/cases/{case_id}/administration_drugs` | `aasmaan.AasmaanCaseServiceImpl.saveCaseAdministrationDrug` | JSON P  | `AasmaanResponseDTO<AasmaanCaseAdministrationDrug>` | - | - | - |
| POST | `/cases/{case_id}/checklist1` | `aasmaan.AasmaanCaseServiceImpl.saveCaseChecklist1` | P JSON  | `AasmaanResponseDTO<AasmaanCaseChecklist1>` | - | - | - |
| POST | `/cases/{case_id}/checklist2` | `aasmaan.AasmaanCaseServiceImpl.saveCaseChecklist2` | P JSON  | `AasmaanResponseDTO<AasmaanCaseChecklist2>` | - | - | - |
| POST | `/cases/{case_id}/checklist3` | `aasmaan.AasmaanCaseServiceImpl.saveCaseChecklist3` | JSON P  | `AasmaanResponseDTO<AasmaanCaseChecklist3>` | - | - | - |
| POST | `/cases/{case_id}/checklist4` | `aasmaan.AasmaanCaseServiceImpl.saveCaseChecklist4` | JSON P  | `AasmaanResponseDTO<AasmaanCaseChecklist4>` | - | - | - |
| POST | `/cases/{case_id}/delivery_notes` | `aasmaan.AasmaanCaseServiceImpl.saveCaseDeliveryNote` | P JSON  | `AasmaanResponseDTO<AasmaanCaseDeliveryNote>` | - | - | - |
| POST | `/cases/{case_id}/discharge_notes` | `aasmaan.AasmaanCaseServiceImpl.saveCaseDischargeNote` | JSON P  | `AasmaanResponseDTO<AasmaanCaseDischargeNote>` | - | - | - |
| POST | `/cases/{case_id}/field_monitoring` | `aasmaan.AasmaanCaseServiceImpl.saveCaseFieldMonitoring` | P JSON  | `AasmaanResponseDTO<AasmaanCaseFieldMonitoring>` | - | - | - |
| POST | `/cases/{case_id}/finish_admission` | `aasmaan.AasmaanCaseServiceImpl.saveCaseFinishAdmission` | P JSON  | `AasmaanResponseDTO<AasmaanCaseFinishAdmission>` | Y | - | - |
| POST | `/cases/{case_id}/highrisk` | `aasmaan.AasmaanCaseServiceImpl.saveCaseHighrisk` | P JSON  | `AasmaanResponseDTO<AasmaanCaseHighrisk>` | - | - | - |
| POST | `/cases/{case_id}/io_forms` | `aasmaan.AasmaanCaseServiceImpl.saveCaseIoForm` | P JSON  | `AasmaanResponseDTO<AasmaanCaseIoForm>` | - | - | - |
| POST | `/cases/{case_id}/monitoring` | `aasmaan.AasmaanCaseServiceImpl.saveCaseMonitoring` | JSON P  | `AasmaanResponseDTO<AasmaanCaseMonitoring>` | - | - | - |
| POST | `/cases/{case_id}/pd_monitoring` | `aasmaan.AasmaanCaseServiceImpl.saveCasePdMonitoring` | P JSON  | `AasmaanResponseDTO<AasmaanCasePdMonitoring>` | - | - | - |
| POST | `/cases/{case_id}/pnc_monitoring` | `aasmaan.AasmaanCaseServiceImpl.saveCasePncMonitoring` | JSON P  | `AasmaanResponseDTO<AasmaanCasePncMonitoring>` | - | - | - |
| POST | `/cases/{case_id}/prescribed_drugs` | `aasmaan.AasmaanCaseServiceImpl.saveCasePrescribedDrug` | P JSON  | `AasmaanResponseDTO<AasmaanCasePrescribedDrug>` | - | - | - |
| POST | `/cases/{case_id}/referral_cancel` | `aasmaan.AasmaanCaseServiceImpl.saveCaseReferralCancel` | P JSON  | `AasmaanResponseDTO<AasmaanCaseReferralCancel>` | - | - | - |
| POST | `/cases/{case_id}/referral_finish` | `aasmaan.AasmaanCaseServiceImpl.saveCaseReferralFinish` | JSON P  | `AasmaanResponseDTO<AasmaanCaseReferralFinish>` | Y | - | - |
| POST | `/cases/{case_id}/referral_slip` | `aasmaan.AasmaanCaseServiceImpl.saveCaseReferralSlip` | JSON P  | `AasmaanResponseDTO<AasmaanCaseReferralSlip>` | - | - | - |
| PUT | `/cases/{case_id}` | `aasmaan.AasmaanCaseServiceImpl.updateCase` | P  | `AasmaanResponseDTO<Object>` | - | - | - |
| DELETE | `/devices/{deviceId}` | `aasmaan.AasmaanDeviceServiceImpl.deleteDevice` | P  | `void` | - | - | - |
| GET | `/devices/` | `aasmaan.AasmaanDeviceServiceImpl.getDevices` | - | `AasmaanResponseDTO<AasmaanDevice>` | - | - | - |
| GET | `/devices/{deviceId}` | `aasmaan.AasmaanDeviceServiceImpl.getDevice` | P  | `AasmaanResponseDTO<AasmaanDevice>` | Y | - | - |
| GET | `/devices/{deviceId}/assets` | `aasmaan.AasmaanDeviceServiceImpl.getDeviceAssets` | P  | `AasmaanResponseDTO<Object>` | - | - | - |
| GET | `/devices/{deviceId}/assets/{assetVersion}` | `aasmaan.AasmaanDeviceServiceImpl.getDeviceAssetsByVersion` | P P  | `AasmaanResponseDTO<Object>` | - | - | - |
| GET | `/devices/{deviceId}/authenticate` | `aasmaan.AasmaanDeviceServiceImpl.authenticateDevice` | P  | `AasmaanResponseDTO<Object>` | Y | - | - |
| PUT | `/devices/{deviceId}` | `aasmaan.AasmaanDeviceServiceImpl.updateDevice` | JSON P  | `AasmaanResponseDTO<AasmaanDevice>` | - | - | - |
| PUT | `/devices/{deviceId}/assets/{assetVersion}` | `aasmaan.AasmaanDeviceServiceImpl.updateDeviceAssetsByVersion` | P P  | `AasmaanResponseDTO<Object>` | - | - | - |
| POST | `/e_tickets/` | `aasmaan.AasmaanETicketServiceImpl.saveETicket` | JSON  | `AasmaanResponseDTO<AasmaanETicket>` | - | - | - |
| GET | `/institution_levels/` | `aasmaan.AasmaanInstitutionLevelServiceImpl.getInstitutionLevels` | - | `AasmaanResponseDTO<AasmaanInstitutionLevel>` | - | - | - |
| GET | `/institution_levels/{institution_level_id}/institutions` | `aasmaan.AasmaanInstitutionLevelServiceImpl.getInstitutionLevelInstitutions` | P  | `AasmaanResponseDTO<AasmaanInstitutionLevelInstitution>` | - | - | - |
| GET | `/institutions/` | `aasmaan.AasmaanInstitutionServiceImpl.getInstitutions` | - | `AasmaanResponseDTO<AasmaanInstitution>` | - | - | - |
| GET | `/institutions/{institution_id}` | `aasmaan.AasmaanInstitutionServiceImpl.getInstitution` | P  | `AasmaanResponseDTO<AasmaanInstitution>` | - | - | - |
| GET | `/institutions/{institution_id}/referrals` | `aasmaan.AasmaanInstitutionServiceImpl.getInstitutionReferrals` | P P  | `AasmaanResponseDTO<AasmaanInstitutionReferral>` | Y | - | - |
| GET | `/institutions/{institution_id}/referrals/{id}` | `aasmaan.AasmaanInstitutionServiceImpl.getInstitutionReferral` | P P  | `AasmaanResponseDTO<AasmaanInstitutionReferral>` | - | - | - |
| GET | `/institutions/{institution_id}/referrals/{id}/export` | `aasmaan.AasmaanInstitutionServiceImpl.exportInstitutionReferral` | P P  | `AasmaanResponseDTO<Object>` | - | - | - |
| GET | `/institutions/{institution_id}/shifts` | `aasmaan.AasmaanInstitutionServiceImpl.getInstitutionShifts` | P  | `AasmaanResponseDTO<AasmaanInstitutionShift>` | - | - | - |
| POST | `/institutions/{institution_id}/feedback` | `aasmaan.AasmaanInstitutionServiceImpl.saveInstitutionFeedback` | P JSON  | `AasmaanResponseDTO<AasmaanInstitutionFeedback>` | Y | - | - |
| POST | `/institutions/{institution_id}/shifts` | `aasmaan.AasmaanInstitutionServiceImpl.saveInstitutionShift` | P JSON  | `AasmaanResponseDTO<AasmaanInstitutionShift>` | - | - | - |
| GET | `/reports/{report_id}` | `aasmaan.AasmaanReportServiceImpl.getReport` | Q Q Q  | `AasmaanResponseDTO<AasmaanReport>` | - | - | - |
| GET | `/reports/{report_id}/export` | `aasmaan.AasmaanReportServiceImpl.exportReport` | Q Q Q  | `AasmaanResponseDTO<Object>` | - | - | - |
| GET | `/rsc_call_logs/` | `aasmaan.AasmaanRscCallLogServiceImpl.getRscCallLogs` | Q Q  | `AasmaanResponseDTO<AasmaanRscCallLog>` | - | - | - |
| POST | `/rsc_call_logs/` | `aasmaan.AasmaanRscCallLogServiceImpl.saveRscCallLog` | - | `AasmaanResponseDTO<Object>` | - | - | - |
| GET | `/stats/` | `aasmaan.AasmaanStatServiceImpl.getStats` | Q Q  | `AasmaanResponseDTO<AasmaanStat>` | - | - | - |
| GET | `/stats/{variable_name}` | `aasmaan.AasmaanStatServiceImpl.getStatVariableName` | Q Q P  | `AasmaanResponseDTO<Object>` | - | - | - |
| GET | `/stats/areas/{areaId}` | `aasmaan.AasmaanStatServiceImpl.getStatArea` | P  | `AasmaanResponseDTO<AasmaanStat>` | - | - | - |
| GET | `/stats/institutes/{instituteId}` | `aasmaan.AasmaanStatServiceImpl.getStatInstitutes` | P  | `AasmaanResponseDTO<AasmaanStat>` | - | - | - |
| GET | `/users/` | `aasmaan.AasmaanUserServiceImpl.getUsers` | Q Q  | `AasmaanResponseDTO<AasmaanUser>` | - | - | - |
| GET | `/users/{userId}` | `aasmaan.AasmaanUserServiceImpl.getUser` | P  | `AasmaanResponseDTO<AasmaanUser>` | Y | - | - |
| GET | `/users/{userId}/authenticate` | `aasmaan.AasmaanUserServiceImpl.authenticateUser` | P  | `AasmaanResponseDTO<Object>` | Y | - | - |
| GET | `/users/authenticate` | `aasmaan.AasmaanUserServiceImpl.authenticateUser` | Q Q  | `AasmaanResponseDTO<Object>` | - | - | - |
| POST | `/users/` | `aasmaan.AasmaanUserServiceImpl.saveUser` | JSON  | `AasmaanResponseDTO<AasmaanUser>` | - | - | - |
| PUT | `/users/{userId}` | `aasmaan.AasmaanUserServiceImpl.updateUser` | P  | `AasmaanResponseDTO<Object>` | - | - | - |

### Module: ABDM (ABHA/NHCX/HPR)  (73 endpoints)

| Method | URL (under /api/rest) | Class : method | Request | Response | Tx | Tables | Notes |
|---|---|---|---|---|---|
| DELETE | `/abha/consent/{id}` | `abdm.AbhaServiceImpl.removeConsent` | P  | `void` | - | - | - |
| GET | `/abha/abhaLinkProfile` | `abdm.AbhaServiceImpl.linkProfile` | Q  | `String` | - | - | - |
| GET | `/abha/allPhrAddresses` | `abdm.AbhaServiceImpl.allPhrAddresses` | Q  | `List<String>` | Y | - | - |
| GET | `/abha/authenticateLink` | `abdm.AbhaServiceImpl.authenticateLink` | Q Q Q  | `UserDTO` | - | - | - |
| GET | `/abha/baalAbhaReKycVerifyOtp` | `abdm.AbhaServiceImpl.baalAbhaReKycVerifyOtp` | Q Q Q  | `List<PatientAbhaSearchDto>` | - | - | - |
| GET | `/abha/capturePIDForFaceAuth` | `abdm.AbhaServiceImpl.capturePIDForFaceAuth` | Q  | `CapturePidForFaceAuthResponse` | - | - | - |
| GET | `/abha/checkAbhaAddressExist` | `abdm.AbhaServiceImpl.checkAbhaAddressExist` | Q  | `Boolean` | - | - | - |
| GET | `/abha/consentRequestData` | `abdm.AbhaServiceImpl.getConsentRequestData` | Q  | `Map<String, List<Object[]>>` | - | - | - |
| GET | `/abha/consentRequestDataBundle` | `abdm.AbhaServiceImpl.getConsentRequestDataBundle` | Q  | `PatientAbdmCRData` | - | - | - |
| GET | `/abha/consentRequestList` | `abdm.AbhaServiceImpl.getRaiseConsentRequestList` | Q Q Q  | `List<PatientAbdmCR>` | Y | - | - |
| GET | `/abha/createTestBundle` | `abdm.AbhaServiceImpl.createTestBundle` | Q Q Q Q  | `List<String>` | - | - | - |
| GET | `/abha/encrollByfaceAuthInit` | `abdm.AbhaServiceImpl.encrollByfaceAuthInit` | - | `String` | - | - | - |
| GET | `/abha/generateLookupOtp` | `abdm.AbhaServiceImpl.generateABHALookupOtp` | Q Q Q  | `String[]` | - | - | - |
| GET | `/abha/generateOtp` | `abdm.AbhaServiceImpl.generateOtp` | Q Q Q  | `String[]` | - | - | - |
| GET | `/abha/getAbhaPatientQueueCount` | `abdm.AbhaServiceImpl.getAbhaPatientQueueCount` | Q  | `long` | Y | - | - |
| GET | `/abha/getAbhaSuggestions` | `abdm.AbhaServiceImpl.getAbhaSuggestions` | Q  | `EnrollSuggestionsResponse` | - | - | - |
| GET | `/abha/getAuthModes` | `abdm.AbhaServiceImpl.getAuthModes` | Q  | `List<AuthenticationMode>` | Y | - | - |
| GET | `/abha/getCard` | `abdm.AbhaServiceImpl.getCard` | Q Q Q  | `byte[]` | - | - | - |
| GET | `/abha/getCareContextsEncounters/{patientId}` | `abdm.AbhaServiceImpl.getCareContextsEncounters` | P Q Q  | `PatientRepresentation` | - | - | - |
| GET | `/abha/getChildProfile` | `abdm.AbhaServiceImpl.getChildProfile` | Q  | `UserDTO` | - | - | - |
| GET | `/abha/getPatientEncountersByHealthId` | `abdm.AbhaServiceImpl.getPatientByHealthId` | Q Q  | `Long` | - | - | - |
| GET | `/abha/getProfile` | `abdm.AbhaServiceImpl.getProfile` | Q Q Q  | `UserDTO` | - | - | - |
| GET | `/abha/getProfileCard` | `abdm.AbhaServiceImpl.getProfileCard` | Q  | `byte[]` | Y | - | - |
| GET | `/abha/getRegisteredAbhaToken` | `abdm.AbhaServiceImpl.getRegisteredAbhaToken` | Q  | `String` | - | - | - |
| GET | `/abha/linkHealthId` | `abdm.AbhaServiceImpl.linkHealthId` | Q Q Q  | `String` | - | - | - |
| GET | `/abha/patientQueue` | `abdm.AbhaServiceImpl.getPatientQueue` | Q Q Q Q  | `List<PatientAbdm>` | Y | - | - |
| GET | `/abha/resendLoginOtp` | `abdm.AbhaServiceImpl.resendLoginOtp` | Q Q Q  | `String[]` | - | - | - |
| GET | `/abha/resendOtp` | `abdm.AbhaServiceImpl.resendOtp` | Q Q Q  | `String[]` | - | - | - |
| GET | `/abha/startABHADeletion` | `abdm.AbhaServiceImpl.startABHADeletionOrDeactivation` | Q Q  | `String` | - | - | - |
| GET | `/abha/startLookup` | `abdm.AbhaServiceImpl.searchAbhaNumberOrId` | Q Q Q  | `String[]` | - | - | - |
| GET | `/abha/verifylinkHealthOTP` | `abdm.AbhaServiceImpl.verifylinkHealthOTP` | Q Q Q  | `UserDTO` | - | - | - |
| GET | `/abha/verifyLookupOTP` | `abdm.AbhaServiceImpl.confirmAbha` | Q Q Q Q  | `List<PatientAbhaSearchDto>` | - | - | - |
| GET | `/abha/verifyOtp` | `abdm.AbhaServiceImpl.verifyOtp` | Q Q Q Q  | `String[]` | - | - | - |
| GET | `/abha/verifyPatientShareToken` | `abdm.AbhaServiceImpl.verifyPatientShareToken` | Q Q  | `UserDTO` | Y | - | - |
| POST | `/abha/baalAbhaReKycRequestOtp` | `abdm.AbhaServiceImpl.baalAbhaReKycRequestOtp` | Q Q  | `String[]` | - | - | - |
| POST | `/abha/createPhrAddress` | `abdm.AbhaServiceImpl.createPhrAddress` | Q Q  | `String` | - | - | - |
| POST | `/abha/encrollByfaceAuth` | `abdm.AbhaServiceImpl.abhaErollByFaceAuth` | JSON Q  | `String[]` | - | - | - |
| POST | `/abha/findPatient` | `abdm.AbhaServiceImpl.findPatient` | JSON  | `List<SearchPatientGeneralDetailsResultDto>` | Y | - | - |
| POST | `/abha/linkAbdmFacility` | `abdm.AbhaServiceImpl.linkAbdmFacility` | Q Q Q  | `Boolean` | - | - | - |
| POST | `/abha/linkHipAPI` | `abdm.AbhaServiceImpl.linkHipAPI` | - | `void` | - | - | - |
| POST | `/abha/loginVerifyUser` | `abdm.AbhaServiceImpl.loginVerifyUser` | JSON  | `String` | - | - | - |
| POST | `/abha/raiseConsentRequest` | `abdm.AbhaServiceImpl.raiseConsentRequest` | JSON  | `String` | - | - | - |
| POST | `/abha/registerABHAProfile` | `abdm.AbhaServiceImpl.registerABHAProfileApi` | JSON Q  | `PatientAppointmentView` | - | - | - |
| POST | `/abha/registerChildABHAProfile` | `abdm.AbhaServiceImpl.registerChildABHAProfile` | JSON Q  | `UserDTO` | Y | - | - |
| POST | `/abha/registerWithAadhar` | `abdm.AbhaServiceImpl.registerWithAadhar` | JSON Q  | `CreateAccountJwtResponse` | - | - | - |
| POST | `/abha/registerWithAadharDemographics` | `abdm.AbhaServiceImpl.registerWithAadharDemographics` | JSON Q  | `UserDTO` | - | - | - |
| POST | `/abha/startPatientRecordLinkWithAbha` | `abdm.AbhaServiceImpl.startPatientRecordLinkWithAbha` | JSON Q  | `String` | - | - | - |
| PUT | `/abha/confirmABHADeletion` | `abdm.AbhaServiceImpl.confirmABHADeletion` | Q Q Q  | `Boolean` | - | - | - |
| PUT | `/abha/delinkHid` | `abdm.AbhaServiceImpl.delinkHid` | Q Q  | `Boolean` | - | - | - |
| PUT | `/abha/linkHid` | `abdm.AbhaServiceImpl.linkHid` | Q Q  | `Boolean` | - | - | - |
| GET | `/hpr/checkHPRIdExist` | `abdm.HPRServiceImpl.checkHPRIdExist` | Q  | `String` | Y | - | - |
| GET | `/hpr/generateLookupOtp` | `abdm.HPRServiceImpl.generateLookupOtp` | Q Q  | `String` | - | - | - |
| GET | `/hpr/generateOtp` | `abdm.HPRServiceImpl.generateOtp` | Q Q Q  | `String[]` | - | - | - |
| GET | `/hpr/getCard` | `abdm.HPRServiceImpl.getCard` | Q  | `byte[]` | - | - | - |
| GET | `/hpr/getProfile` | `abdm.HPRServiceImpl.getProfile` | Q  | `UserDTO` | - | - | - |
| GET | `/hpr/loginUser` | `abdm.HPRServiceImpl.loginHPRUser` | Q Q Q  | `Long` | - | - | - |
| GET | `/hpr/resendOtp` | `abdm.HPRServiceImpl.resendOtp` | Q Q  | `String` | - | - | - |
| GET | `/hpr/startLookup` | `abdm.HPRServiceImpl.startLookup` | Q Q  | `String` | - | - | - |
| GET | `/hpr/verifyAadhaarOtp` | `abdm.HPRServiceImpl.verifyAadhaarOtp` | Q Q Q  | `TxnResponseDetails` | - | - | - |
| GET | `/hpr/verifyLookupOTP` | `abdm.HPRServiceImpl.confirmHPR` | Q Q Q Q  | `Long` | - | - | - |
| GET | `/hpr/verifyOtp` | `abdm.HPRServiceImpl.verifyOtp` | Q Q Q  | `String` | - | - | - |
| POST | `/hpr/registerHPRUserProfile` | `abdm.HPRServiceImpl.registerHPRUserProfile` | JSON Q  | `boolean` | - | - | - |
| POST | `/hpr/registerWithAadhar` | `abdm.HPRServiceImpl.registerWithAadhar` | JSON Q  | `CreateAccountJwtResponse` | - | - | - |
| GET | `/nhcx/insurancePlanRequest` | `abdm.NHCXServiceImpl.fetchInsurancePlanRequestById` | Q  | `NHCXRequest` | - | - | - |
| GET | `/nhcx/previousEligiblities` | `abdm.NHCXServiceImpl.fetchPreviousEligiblities` | Q  | `List<NHCXRequest>` | - | - | - |
| GET | `/nhcx/showNhcxDocument` | `abdm.NHCXServiceImpl.showNhcxDocument` | Q Q  | `String` | - | - | - |
| POST | `/nhcx/allParticipants` | `abdm.NHCXServiceImpl.getAllParticipants` | - | `ParticipantListResponse` | - | - | - |
| POST | `/nhcx/claimSubmitRequest` | `abdm.NHCXServiceImpl.claimSubmit` | - | `void` | - | - | - |
| POST | `/nhcx/coverageEligiblityRequest` | `abdm.NHCXServiceImpl.checkCoverageEligiblityRequest` | JSON  | `NHCXRequest` | - | - | - |
| POST | `/nhcx/fetchPayorCerts` | `abdm.NHCXServiceImpl.fetchPayorCerts` | - | `void` | - | - | - |
| POST | `/nhcx/fetchPolicies` | `abdm.NHCXServiceImpl.fetchPolicies` | Q  | `List<ParticipantPolicyDetails>` | - | - | - |
| POST | `/nhcx/insurancePlanRequest` | `abdm.NHCXServiceImpl.fetchInsurancePlanRequest` | ?  | `NHCXRequest` | - | - | - |
| POST | `/nhcx/preAuthSubmit` | `abdm.NHCXServiceImpl.preAuthSubmit` | - | `void` | - | - | - |

### Module: Authentication  (12 endpoints)

| Method | URL (under /api/rest) | Class : method | Request | Response | Tx | Tables | Notes |
|---|---|---|---|---|---|
| POST | `/auth/changeUserRole` | `AuthenticationServiceImpl.changeUserRole` | Q Q  | `UserView` | - | - | - |
| POST | `/auth/getuserrole` | `AuthenticationServiceImpl.getUserRole` | FORM FORM  | `List<UserShiftDetails>` | - | - | - |
| POST | `/auth/getuserroleOtp` | `AuthenticationServiceImpl.getUserRoleOtp` | FORM FORM  | `List<UserShiftDetails>` | - | - | - |
| POST | `/auth/loginb` | `AuthenticationServiceImpl.loginBasicAuth` | HDR  | `UserView` | - | - | - |
| POST | `/auth/loginExt` | `AuthenticationServiceImpl.loginFormForKDL` | FORM  | `LoginResponseView` | - | - | - |
| POST | `/auth/loginextf` | `AuthenticationServiceImpl.loginFormParamsExt` | FORM  | `UserView` | - | - | - |
| POST | `/auth/loginf` | `AuthenticationServiceImpl.loginFormParams` | FORM  | `UserView` | - | - | - |
| POST | `/auth/loginh` | `AuthenticationServiceImpl.loginHeaderParams` | HDR HDR HDR  | `UserView` | - | - | - |
| POST | `/auth/loginJ` | `AuthenticationServiceImpl.externalLogin` | HDR HDR  | `UserView` | - | - | - |
| POST | `/auth/loginotp` | `AuthenticationServiceImpl.loginotp` | FORM  | `UserView` | - | - | - |
| POST | `/auth/loginPatient` | `AuthenticationServiceImpl.loginPatientFormParams` | FORM  | `UserView` | - | - | - |
| PUT | `/auth/logout` | `AuthenticationServiceImpl.logout` | - | `void` | - | - | - |

### Module: Biomedical Waste  (14 endpoints)

| Method | URL (under /api/rest) | Class : method | Request | Response | Tx | Tables | Notes |
|---|---|---|---|---|---|
| GET | `/bioMedicalWasteManagement/approveBioMedicalRequest` | `BioMedicalWasteManagementServiceImpl.approveBioMedicalRequest` | Q Q  | `Boolean` | Y | - | - |
| GET | `/bioMedicalWasteManagement/deleteBioRequest` | `BioMedicalWasteManagementServiceImpl.deleteBioRequest` | Q Q  | `Boolean` | - | - | - |
| GET | `/bioMedicalWasteManagement/getBagWeightByBarcode` | `BioMedicalWasteManagementServiceImpl.getBagWeightByBarcode` | Q Q  | `BigDecimal` | Y | - | - |
| GET | `/bioMedicalWasteManagement/getBioRequestDetails` | `BioMedicalWasteManagementServiceImpl.getBioRequestDetails` | Q Q  | `List<BioMedicalWasteDetails>` | Y | - | - |
| GET | `/bioMedicalWasteManagement/handedOverToFirm` | `BioMedicalWasteManagementServiceImpl.handedOverToFirm` | - | `Supplier` | Y | - | - |
| GET | `/bioMedicalWasteManagement/handOverBags` | `BioMedicalWasteManagementServiceImpl.handOverBags` | Q Q Q  | `String` | Y | - | - |
| GET | `/bioMedicalWasteManagement/processBioRequest` | `BioMedicalWasteManagementServiceImpl.processBioRequest` | Q Q Q  | `Boolean` | Y | - | - |
| GET | `/bioMedicalWasteManagement/validateBarcode` | `BioMedicalWasteManagementServiceImpl.validateBarcode` | Q Q  | `boolean` | - | - | - |
| POST | `/bioMedicalWasteManagement/addNewBioRequest` | `BioMedicalWasteManagementServiceImpl.addNewBioRequest` | JSON  | `BioMedicalWaste` | - | - | - |
| POST | `/bioMedicalWasteManagement/getbioMedicalWasteStatus` | `BioMedicalWasteManagementServiceImpl.getbioMedicalWasteStatus` | JSON Q Q  | `List<BioMedicalWaste>` | - | - | - |
| POST | `/bioMedicalWasteManagement/getBioMedWasteCount` | `BioMedicalWasteManagementServiceImpl.getBioMedWasteCount` | JSON  | `Map<String, Long>` | - | - | - |
| POST | `/bioMedicalWasteManagement/receiveBioRequestMainStore` | `BioMedicalWasteManagementServiceImpl.receiveBioRequestMainStore` | JSON  | `void` | Y | - | - |
| POST | `/bioMedicalWasteManagement/updateBagStatus` | `BioMedicalWasteManagementServiceImpl.updateBagStatus` | JSON  | `void` | - | - | - |
| POST | `/bioMedicalWasteManagement/updateBioRequest` | `BioMedicalWasteManagementServiceImpl.updateBioRequest` | JSON  | `void` | Y | - | - |

### Module: Clinical  (4 endpoints)

| Method | URL (under /api/rest) | Class : method | Request | Response | Tx | Tables | Notes |
|---|---|---|---|---|---|
| GET | `/clinical/getNoteContent` | `ClinicalServiceImpl.getNoteContent` | Q Q Q Q  | `Object[]` | - | - | - |
| GET | `/clinical/getProviderList` | `ClinicalServiceImpl.getProviderList` | Q  | `ProviderList` | - | - | - |
| GET | `/clinical/healthfactorMaster` | `ClinicalServiceImpl.fetchHealthfactor` | Q Q Q  | `List<Healthfactormaster>` | Y | - | - |
| POST | `/clinical/saveAppErrors` | `ClinicalServiceImpl.saveAppErrors` | ?  | `void` | - | - | - |

### Module: CSSD (Sterilization)  (46 endpoints)

| Method | URL (under /api/rest) | Class : method | Request | Response | Tx | Tables | Notes |
|---|---|---|---|---|---|
| GET | `/cssd/deleteKitItemDetails` | `cssd.CSSDServiceImpl.deleteKitItemDetails` | Q Q  | `void` | - | - | - |
| GET | `/cssd/getAllKitDetailsList` | `cssd.CSSDServiceImpl.getAllKitDetailsList` | Q  | `List<CssdKitFormation>` | - | - | - |
| GET | `/cssd/getFreeAvaliableQtyforKit` | `cssd.CSSDServiceImpl.getFreeAvaliableQtyforKit` | Q Q  | `BigDecimal` | - | - | - |
| GET | `/cssd/getItemCurrentStockForCSSDKit` | `cssd.CSSDServiceImpl.getItemCurrentStockForCSSDKit` | Q Q  | `List<ItemByNameResultDto>` | - | - | - |
| GET | `/cssd/getItemNameForKitFormation` | `cssd.CSSDServiceImpl.getItemForKitFormation` | Q  | `List<ItemByNameResultDto>` | Y | - | - |
| GET | `/cssd/getKitDetailsForPreparation` | `cssd.CSSDServiceImpl.getKitDetailsForPreparation` | Q Q  | `List<CssdKitFormationDetails>` | - | - | - |
| GET | `/cssd/getKitItemsList` | `cssd.CSSDServiceImpl.getKitItemsList` | Q Q  | `List<CssdKitFormationDetails>` | - | - | - |
| GET | `/cssd/getKitMasterDetails` | `cssd.CSSDServiceImpl.getCompoundMasterDetailsList` | Q Q Q  | `KitFormationView` | - | - | - |
| GET | `/cssd/getProcCSSDReqDetailsForReport` | `cssd.CSSDServiceImpl.getProcCSSDReqDetailsForReport` | Q  | `List<ProcedureCssdKitDetails>` | Y | - | - |
| GET | `/cssd/getProcedureCSSDIssueReceivedDetails` | `cssd.CSSDServiceImpl.getProcedureCSSDIssueReceivedDetails` | Q Q  | `List<ProcedureCssdKitDetails>` | - | - | - |
| GET | `/cssd/getProcedureCSSDRequestDetails` | `cssd.CSSDServiceImpl.getProcedureCSSDRequestDetails` | Q Q  | `List<ProcedureCssdKitDetails>` | - | - | - |
| GET | `/cssd/isKitExists` | `cssd.CSSDServiceImpl.isKitExists` | Q Q  | `long` | Y | - | - |
| GET | `/cssd/updateCssdRequisitionForRinsing` | `cssd.CSSDServiceImpl.updateCssdRequisitionForRinsing` | Q Q Q  | `void` | Y | - | - |
| GET | `/cssd/updateCssdRequisitionFromStoreDispatch` | `cssd.CSSDServiceImpl.updateCssdRequisitionFormStoreDispatch` | Q Q Q  | `void` | Y | - | - |
| GET | `/cssd/updateCssdRequisitionFromStoreReceive` | `cssd.CSSDServiceImpl.updateCssdRequisitionFromStoreReceive` | Q Q Q  | `void` | Y | - | - |
| GET | `/cssd/updateCssdRequisitionToStoreReceive` | `cssd.CSSDServiceImpl.updateCssdRequisitionToStoreReceive` | Q Q Q  | `void` | Y | - | - |
| GET | `/cssd/updateRequestAndIssueWhileDispatch` | `cssd.CSSDServiceImpl.updateRequestAndIssueWhileDispatch` | Q Q Q  | `void` | Y | - | - |
| GET | `/cssd/verifyMasterKitPreparation` | `cssd.CSSDServiceImpl.verifyMasterKitPreparation` | Q Q  | `boolean` | Y | - | - |
| POST | `/cssd/addItemSterilizationDetails` | `cssd.CSSDServiceImpl.addItemSterilizationDetails` | JSON  | `CssdItemSterilizationDetails` | - | - | - |
| POST | `/cssd/addKitMaster` | `cssd.CSSDServiceImpl.addKitFormation` | JSON  | `KitFormationView` | Y | - | - |
| POST | `/cssd/addKitRequestFromKitMaster` | `cssd.CSSDServiceImpl.addKitRequestFromKitMaster` | JSON  | `ProcedureCSSDRequestView` | Y | - | - |
| POST | `/cssd/addNewItemInExisitingKit` | `cssd.CSSDServiceImpl.addNewItemInExisitingKit` | JSON  | `KitFormationView` | Y | - | - |
| POST | `/cssd/addUpdateKitItemDetails` | `cssd.CSSDServiceImpl.addUpdateKitItemDetails` | JSON  | `List<CssdKitFormationDetails>` | Y | - | - |
| POST | `/cssd/cssdItemIssue` | `cssd.CSSDServiceImpl.cssdItemIssue` | JSON  | `IssueView` | - | - | - |
| POST | `/cssd/freezeKitRequest` | `cssd.CSSDServiceImpl.freezeKitRequest` | Q Q  | `void` | Y | - | - |
| POST | `/cssd/getCSSDItemSterilizeDetails` | `cssd.CSSDServiceImpl.getCSSDItemSterilizeDetails` | Q  | `List<ProcedureCssdKitDetails>` | - | - | - |
| POST | `/cssd/getCssdKitRequestListCount` | `cssd.CSSDServiceImpl.getCssdKitRequestListCount` | JSON  | `CssdKitRequestListCountSummary` | - | - | - |
| POST | `/cssd/getCssdStoreKitRequestListCount` | `cssd.CSSDServiceImpl.getCssdStoreKitRequestListCount` | JSON  | `CssdKitRequestListCountSummary` | - | - | - |
| POST | `/cssd/getCurrentKitMasterList` | `cssd.CSSDServiceImpl.getCurrentKitMasterList` | JSON Q  | `List<CssdKitFormation>` | - | - | - |
| POST | `/cssd/getCurrentKitPreparationList` | `cssd.CSSDServiceImpl.getCurrentKitCreationList` | JSON Q  | `List<CssdKitFormation>` | Y | - | - |
| POST | `/cssd/getIndentListforCssdStoreIdforItemIssue` | `cssd.CSSDServiceImpl.getIndentListforCssdStoreIdforItemIssue` | JSON  | `List<IndentListByStoreIdResultDto>` | - | - | - |
| POST | `/cssd/getKitListSummary` | `cssd.CSSDServiceImpl.getkitListSummary` | JSON  | `KitMasterListSummary` | - | - | - |
| POST | `/cssd/getProcedureCSSDIssue` | `cssd.CSSDServiceImpl.getProcedureCSSDIssue` | JSON  | `List<ProcedureCssdRequest>` | Y | - | - |
| POST | `/cssd/getProcedureCSSDKitDetails` | `cssd.CSSDServiceImpl.getProcedureCSSDKitDetails` | JSON  | `List<ProcedureCssdKitDetails>` | - | - | - |
| POST | `/cssd/getProcedureCSSDRequest` | `cssd.CSSDServiceImpl.getProcedureCSSDRequest` | JSON Q  | `List<ProcedureCssdKitDetails>` | Y | - | - |
| POST | `/cssd/getProcedureCSSDRequestDetailsforIssue` | `cssd.CSSDServiceImpl.getProcedureCSSDRequestDetailsforIssue` | JSON  | `ProcedureCSSDRequestView` | - | - | - |
| POST | `/cssd/getProcedureCSSDStoreRequest` | `cssd.CSSDServiceImpl.getProcedureCSSDStoreRequest` | JSON Q  | `List<ProcedureCssdKitDetails>` | - | - | - |
| POST | `/cssd/issueItemReceiveListForCSSD` | `cssd.CSSDServiceImpl.issueItemReceiveList` | JSON  | `List<GetDirectIssueItemDetailsResultDto>` | - | - | - |
| POST | `/cssd/printKitRequisitionDetails` | `cssd.CSSDServiceImpl.printKitRequisitionDetails` | JSON  | `String` | - | - | - |
| POST | `/cssd/unHoldCSSDRequestDetailsforKit` | `cssd.CSSDServiceImpl.unHoldCSSDRequestDetailsforKit` | JSON  | `void` | - | - | - |
| POST | `/cssd/updateCssdRequestforIssue` | `cssd.CSSDServiceImpl.updateCssdRequestforIssue` | JSON  | `ProcedureCSSDRequestView` | - | - | - |
| POST | `/cssd/updateCssdRequestReceiving` | `cssd.CSSDServiceImpl.updateCssdRequestReceiving` | JSON  | `void` | - | - | - |
| POST | `/cssd/updateCssdRequisitionForHold` | `cssd.CSSDServiceImpl.updateCssdRequisitionForHold` | JSON  | `void` | Y | - | - |
| POST | `/cssd/updateCssdRequisitionForToStoreHold` | `cssd.CSSDServiceImpl.updateCssdRequisitionForToStoreHold` | JSON Q Q  | `void` | Y | - | - |
| POST | `/cssd/updateKitMaster` | `cssd.CSSDServiceImpl.updateKitFormation` | JSON  | `CssdKitFormation` | Y | - | - |
| PUT | `/cssd/updateStatusProcedureCSSDKitDetails` | `cssd.CSSDServiceImpl.updateStatusProcedureCSSDKitDetails` | JSON  | `void` | - | - | - |

### Module: Custom (OPD/IPD/Billing workflows)  (1204 endpoints)

| Method | URL (under /api/rest) | Class : method | Request | Response | Tx | Tables | Notes |
|---|---|---|---|---|---|
| GET | `/actionNotif/` | `custom.ActionNotificationServiceImpl.getAll` | Q Q  | `List<ActionNotification>` | - | - | - |
| POST | `/actionNotif/` | `custom.ActionNotificationServiceImpl.add` | ?  | `ActionNotification` | - | - | - |
| PUT | `/actionNotif/` | `custom.ActionNotificationServiceImpl.update` | ?  | `ActionNotification` | Y | - | - |
| GET | `/addAdvance/getAdvanceAgainstList` | `custom.AddAdvanceServiceImpl.getAdvanceAgainstList` | Q  | `List<Advanceagainst>` | - | - | - |
| GET | `/addAdvance/getAdvancePaymentDetailList` | `custom.AddAdvanceServiceImpl.getPaymentAdvanceDetail` | Q Q Q  | `List<Object>` | - | - | - |
| GET | `/addAdvance/getPatientAdvanceByBillEstimation` | `custom.AddAdvanceServiceImpl.getPatientAdvanceByBillEstimation` | Q Q  | `List<Object>` | - | - | - |
| POST | `/addAdvance/` | `custom.AddAdvanceServiceImpl.addAdvance` | JSON  | `AdvanceView` | Y | - | - |
| POST | `/addAdvance/getAdvance` | `custom.AddAdvanceServiceImpl.getBillSummary` | JSON  | `BigDecimal` | - | - | - |
| POST | `/addPayment/` | `custom.AddPaymentServiceImpl.addBillPaymentNew` | JSON Q Q  | `BillListForSearch_NewResultDto_3` | Y | - | - |
| POST | `/addPayment/itemSalePayment` | `custom.AddPaymentServiceImpl.addItemSalePayment` | JSON  | `List<Long>` | Y | - | - |
| PUT | `/addPayment/updateCharges` | `custom.AddPaymentServiceImpl.updateCharge` | JSON  | `GenericResultDto` | Y | - | - |
| POST | `/addRefund/refund` | `custom.AddRefundServiceImpl.addRefund` | JSON  | `RefundView` | Y | - | - |
| DELETE | `/addServiceMaster/removeServiceToById/{serviceId}` | `custom.AddServiceMasterImpl.removeServiceToById` | P  | `void` | - | - | - |
| GET | `/addServiceMaster/getServiceList` | `custom.AddServiceMasterImpl.getServiceList` | - | `List<ServiceListByUnitIdResultDto>` | - | - | - |
| GET | `/addServiceMaster/getServiceToByServiceID/{serviceId}` | `custom.AddServiceMasterImpl.getServiceToByServiceID` | P  | `AddServiceToMasterView` | - | - | - |
| POST | `/addServiceMaster/addServiceToMaster` | `custom.AddServiceMasterImpl.addServiceToMaster` | JSON  | `AddServiceToMasterView` | - | - | - |
| POST | `/addServiceMaster/getServiceListByUnitID` | `custom.AddServiceMasterImpl.getServiceListByUnitID` | - | `List<ServiceListByUnitIdResultDto>` | - | - | - |
| PUT | `/addServiceMaster/updateServiceToListByServiceID` | `custom.AddServiceMasterImpl.updateServiceToListByServiceID` | JSON  | `AddServiceToMasterView` | Y | - | - |
| DELETE | `/serviceOrder/deleteService` | `custom.AddServiceOrderServiceImpl.deleteService` | Q Q  | `boolean` | Y | - | - |
| DELETE | `/serviceOrder/excludeCharge` | `custom.AddServiceOrderServiceImpl.excluseChargeNew` | Q Q  | `void` | - | - | - |
| DELETE | `/serviceOrder/removeCharge` | `custom.AddServiceOrderServiceImpl.removeCharge` | Q Q JSON  | `Long` | Y | - | - |
| GET | `/serviceOrder/allPackagesFastByGet` | `custom.AddServiceOrderServiceImpl.getAllPackagesForBillFastByGet` | Q Q  | `List<PackageListForBillResultDto>` | - | - | - |
| GET | `/serviceOrder/allServiceFastByGet` | `custom.AddServiceOrderServiceImpl.getAllServicesForBillFastByGet` | Q Q  | `List<ServiceListForBillWithoutCopayConcessionFastResultDto>` | - | - | - |
| GET | `/serviceOrder/checkChargeStatusForCancelBill` | `custom.AddServiceOrderServiceImpl.checkChargeStatusForCancelBill` | Q  | `boolean` | - | - | - |
| GET | `/serviceOrder/checkCrossUnitVisit` | `custom.AddServiceOrderServiceImpl.checkCrossUnitVisitApi` | Q  | `Boolean` | - | - | - |
| GET | `/serviceOrder/checkSampleStatusForCreditNote` | `custom.AddServiceOrderServiceImpl.checkSampleStatusForCreditNote` | Q Q  | `boolean` | - | - | - |
| GET | `/serviceOrder/getAdmissionCharges` | `custom.AddServiceOrderServiceImpl.getAdmissionCharges` | Q Q  | `BigDecimal` | Y | - | - |
| GET | `/serviceOrder/getAllServiceMasterDetailsByDescription` | `custom.AddServiceOrderServiceImpl.getAllServiceMasterDetailsSrv` | Q  | `List<AllServiceMasterDetailsByDescriptionResultDto>` | - | - | - |
| GET | `/serviceOrder/getDoctorByBillingDepartmentId` | `custom.AddServiceOrderServiceImpl.getDoctorsByBillingDepartmentId` | Q Q  | `List<Doctormaster>` | Y | - | - |
| GET | `/serviceOrder/getOutsourceServiceStatus` | `custom.AddServiceOrderServiceImpl.getOutSourcedServiceStatus` | Q Q Q  | `Boolean` | - | - | - |
| GET | `/serviceOrder/getPackageCharges` | `custom.AddServiceOrderServiceImpl.getPackageCharges` | Q Q  | `List<PkgCoverageDto>` | Y | - | - |
| GET | `/serviceOrder/getPackageDetail` | `custom.AddServiceOrderServiceImpl.getPackageDetailBill` | Q Q  | `List<PackageDetailView>` | - | - | - |
| GET | `/serviceOrder/getRunningBill` | `custom.AddServiceOrderServiceImpl.getRunningBill` | Q Q  | `Object[]` | Y | - | - |
| GET | `/serviceOrder/getServicePrice` | `custom.AddServiceOrderServiceImpl.getServicePriceDetails` | Q Q Q  | `ServiceListForBillResultDto` | - | - | - |
| GET | `/serviceOrder/getSponsorDetail` | `custom.AddServiceOrderServiceImpl.getSponsorDetail` | Q Q  | `SponsorDetailDto` | - | - | - |
| GET | `/serviceOrder/getSubscribedPackageCharges` | `custom.AddServiceOrderServiceImpl.getSubscribedPackageCharges` | Q Q  | `List<PkgCoverageDto>` | - | - | - |
| GET | `/serviceOrder/getvisitCharges` | `custom.AddServiceOrderServiceImpl.getvisitCharges` | Q Q Q  | `BigDecimal` | - | - | - |
| GET | `/serviceOrder/services/{id}` | `custom.AddServiceOrderServiceImpl.getServiceMasterListById` | P  | `Servicemaster` | Y | - | - |
| POST | `/serviceOrder/addExternalOrderDetails` | `custom.AddServiceOrderServiceImpl.addExternalOrderDetails` | JSON  | `BillInfoDto` | - | - | - |
| POST | `/serviceOrder/addHealthplanConsumptionDetail` | `custom.AddServiceOrderServiceImpl.addHealthplanPackageConsumptionDetails` | JSON  | `HealthplanPackageConsumptionDetailsDto` | - | - | - |
| POST | `/serviceOrder/addMultipleHealthplanConsumptionDetail` | `custom.AddServiceOrderServiceImpl.addMultipleHealthplanPackageConsumptionDetails` | JSON  | `List<HealthplanPackageConsumptionDetailsDto>` | Y | - | - |
| POST | `/serviceOrder/addPackageRefund` | `custom.AddServiceOrderServiceImpl.addPackageRefund` | JSON  | `CreditNote` | - | - | - |
| POST | `/serviceOrder/addPharmacyItemtoChargeForIP` | `custom.AddServiceOrderServiceImpl.addPharmacyItemtoChargeForIP` | JSON  | `void` | Y | - | - |
| POST | `/serviceOrder/addServicechargesNew` | `custom.AddServiceOrderServiceImpl.addServices` | JSON  | `BillInfoDto` | Y | - | - |
| POST | `/serviceOrder/addVisitCharges` | `custom.AddServiceOrderServiceImpl.addVisitCharges` | JSON  | `BigDecimal` | Y | - | - |
| POST | `/serviceOrder/addVisitForHealthplanConsumption` | `custom.AddServiceOrderServiceImpl.addVisitForHealthplanConsumption` | JSON  | `MarkVisitForPackageServiceConsumptionDto` | Y | - | - |
| POST | `/serviceOrder/getAllPrevCharges` | `custom.AddServiceOrderServiceImpl.getAllPrevCharges` | JSON  | `BillChargesView` | - | - | - |
| POST | `/serviceOrder/getAllService` | `custom.AddServiceOrderServiceImpl.getAllServicesForBill` | JSON Q  | `List<ServiceListForBillResultDto>` | - | - | - |
| POST | `/serviceOrder/getAllServiceFast` | `custom.AddServiceOrderServiceImpl.getAllServicesForBillFast` | JSON  | `List<ServiceListForBillWithoutCopayConcessionFastResultDto>` | - | - | - |
| POST | `/serviceOrder/getCharges` | `custom.AddServiceOrderServiceImpl.getCharges` | JSON  | `BillChargesView` | Y | - | - |
| POST | `/serviceOrder/getChargesForCreditNote` | `custom.AddServiceOrderServiceImpl.getChargesForCreditNote` | JSON  | `BillChargesView` | Y | - | - |
| POST | `/serviceOrder/getChargesForHealthConsumption` | `custom.AddServiceOrderServiceImpl.` |  | `?` | - | - | - |
| POST | `/serviceOrder/getExternalorders` | `custom.AddServiceOrderServiceImpl.getExternalorders` | JSON  | `List<ExternalOrderDetails>` | Y | - | - |
| POST | `/serviceOrder/getHealthplanConsumption` | `custom.AddServiceOrderServiceImpl.` |  | `?` | Y | - | - |
| POST | `/serviceOrder/getLabOrderDetailsOfPatient` | `custom.AddServiceOrderServiceImpl.getLabOrderDetailsOfPatient` | JSON  | `List<GetChargesResultDto>` | - | - | - |
| POST | `/serviceOrder/getPackageChargeMap` | `custom.AddServiceOrderServiceImpl.getPackageChargeMap` | JSON  | `Map<Long, List<PackageServiceFromPackageIDResultDto_1>>` | - | - | - |
| POST | `/serviceOrder/getPackages` | `custom.AddServiceOrderServiceImpl.getPackageListForBillApi` | JSON  | `List<PackageListForBillResultDto>` | - | - | - |
| POST | `/serviceOrder/getPackageServices` | `custom.AddServiceOrderServiceImpl.getPackageServices` | JSON Q  | `PackageServiceFromPackageIDResultDto` | Y | - | - |
| POST | `/serviceOrder/getSpecializationForBill` | `custom.AddServiceOrderServiceImpl.getSpecializationForBill` | JSON  | `List<SpecializationDTO>` | - | - | - |
| POST | `/serviceOrder/includeCharge` | `custom.AddServiceOrderServiceImpl.includeChargeNew` | JSON  | `void` | Y | - | - |
| POST | `/serviceOrder/releaseAllLab` | `custom.AddServiceOrderServiceImpl.releaseAllLab` | JSON  | `void` | - | - | - |
| POST | `/serviceOrder/saveCreditNote` | `custom.AddServiceOrderServiceImpl.saveCreditNote` | ?  | `CreditNote` | - | - | - |
| POST | `/serviceOrder/saveServices` | `custom.AddServiceOrderServiceImpl.saveServiceAPI` | ?  | `ServiceRateDto` | - | - | - |
| POST | `/serviceOrder/updateHealthplanConsumptionSchedule` | `custom.AddServiceOrderServiceImpl.updateHealthplanConsumptionSchedule` | JSON  | `MarkVisitForPackageServiceConsumptionDto` | Y | - | - |
| POST | `/serviceOrder/updatePathoClinicalDetails` | `custom.AddServiceOrderServiceImpl.updatePathoClinicalDetails` | JSON  | `void` | - | - | - |
| PUT | `/serviceOrder/getSponsors` | `custom.AddServiceOrderServiceImpl.getSponsors` | Q Q Q  | `List<SponsorByAdmissionIDResultDto>` | - | - | - |
| PUT | `/serviceOrder/moveOPChargesToIP` | `custom.AddServiceOrderServiceImpl.moveOPChargesToIP` | ? ? ?  | `void` | - | - | - |
| POST | `/updateappointment/cancelupdateappointment` | `custom.AddUpdateAppointmentServiceImpl.canceladdappt` | JSON  | `CancelUpdateAppointmentDto` | - | - | - |
| DELETE | `/addUserService/removeUserBuilding/{id}` | `custom.AddUserServiceImpl.removeUserBuilding` | P  | `void` | - | - | - |
| DELETE | `/addUserService/removeUserSpeciality/{id}` | `custom.AddUserServiceImpl.removeUserSpeciality` | P  | `void` | - | - | - |
| GET | `/addUserService/checkDescriptionInMoleculeMaster` | `custom.AddUserServiceImpl.checkDescriptionInMoleculeMaster` | Q  | `Long` | Y | - | - |
| GET | `/addUserService/checkDescriptionInPregnencyClassMaster` | `custom.AddUserServiceImpl.checkDescriptionInPregnencyClassMaster` | Q  | `Long` | - | - | - |
| GET | `/addUserService/checkDescriptionInProcedureMaster` | `custom.AddUserServiceImpl.checkDescriptionInProcedureMaster` | Q  | `Long` | - | - | - |
| GET | `/addUserService/checkDescriptionInServiceMaster` | `custom.AddUserServiceImpl.getActiveAdmissionOfRegisteredPatient` | Q  | `Long` | Y | - | - |
| GET | `/addUserService/checkItemGroupDescription` | `custom.AddUserServiceImpl.checkItemGroupDescription` | Q  | `Long` | - | - | - |
| GET | `/addUserService/checkItemGroupPrefix` | `custom.AddUserServiceImpl.checkItemGroupPrefix` | Q  | `Number` | - | - | - |
| GET | `/addUserService/checkLoginNameForUser` | `custom.AddUserServiceImpl.checkLoginNameForUser` | Q  | `Number` | Y | - | - |
| GET | `/addUserService/getAllDepartments` | `custom.AddUserServiceImpl.getAllDepartments` | Q  | `List<Departmentmaster>` | - | - | - |
| GET | `/addUserService/getAllRole` | `custom.AddUserServiceImpl.getAllRole` | - | `List<TUserrolemaster>` | - | - | - |
| GET | `/addUserService/getAllUnitMasterList` | `custom.AddUserServiceImpl.getAllUnitMasterList` | Q  | `List<Unitmaster>` | Y | - | - |
| GET | `/addUserService/getAllUnits` | `custom.AddUserServiceImpl.getAllUnits` | - | `List<Unitmaster>` | - | - | - |
| GET | `/addUserService/getCabinDetailsById` | `custom.AddUserServiceImpl.getCabinmasterDetails` | Q  | `Cabinmaster` | Y | - | - |
| GET | `/addUserService/getCadredesignationDetailsById` | `custom.AddUserServiceImpl.getCadreDesigantionMasterDetails` | Q  | `String` | - | - | - |
| GET | `/addUserService/getCliniciansBySpecialityId` | `custom.AddUserServiceImpl.getCliniciansBySpecialityId` | Q Q  | `List<Doctormaster>` | - | - | - |
| GET | `/addUserService/getDepartmentDoctor` | `custom.AddUserServiceImpl.getDepartmentWiseDoctor` | Q  | `List<Departmentwisedoctor>` | - | - | - |
| GET | `/addUserService/getDepartmentwisecabinById` | `custom.AddUserServiceImpl.getDepartmentwisecabinById` | Q  | `List<Cabinmaster>` | - | - | - |
| GET | `/addUserService/getDSCRByUnitId` | `custom.AddUserServiceImpl.getDSCRByUnitId` | Q Q  | `List<UserShiftDetails>` | - | - | - |
| GET | `/addUserService/getFrequantlyListByUser` | `custom.AddUserServiceImpl.getFrequantlyListByUser` | Q Q Q  | `List <Servicemaster>` | - | - | - |
| GET | `/addUserService/getMrnoByJanMemberId` | `custom.AddUserServiceImpl.getMrnoByJanMemberId` | Q  | `SearchPatientGeneralDetailsResultDto` | - | - | - |
| GET | `/addUserService/getPermissions` | `custom.AddUserServiceImpl.getPermissions` | Q  | `List<RoleBasedPermissionJsTreeStateDTO>` | - | - | - |
| GET | `/addUserService/getPermissionsOfWard` | `custom.AddUserServiceImpl.getPermissionsOfWard` | Q Q  | `Boolean` | - | - | - |
| GET | `/addUserService/getStaffDetailsByRoleId` | `custom.AddUserServiceImpl.getCurrentStaffDetailsByRoleAndUnitId` | Q Q  | `List<Staffmaster>` | - | - | - |
| GET | `/addUserService/getStateUserMappedStateList` | `custom.AddUserServiceImpl.getStateUserMappedStateList` | - | `List<UserStateDetails>` | - | - | - |
| GET | `/addUserService/getUserBuilding` | `custom.AddUserServiceImpl.getUserBuilding` | Q Q  | `List<TUserBuildingDetails>` | - | - | - |
| GET | `/addUserService/getUserDefaultRole` | `custom.AddUserServiceImpl.getUserRole` | Q Q  | `Map<Long, List<UserShiftDetails>>` | - | - | - |
| GET | `/addUserService/getUserDetailsByRoleId` | `custom.AddUserServiceImpl.getUserDetailsByRoleId` | Q Q  | `List<Staffmaster>` | Y | - | - |
| GET | `/addUserService/getUserSpeciality` | `custom.AddUserServiceImpl.getUserSpeciality` | Q Q  | `List<UserSpecialityDetails>` | Y | - | - |
| GET | `/addUserService/syncUserApiSecret` | `custom.AddUserServiceImpl.syncUserApiSecret` | Q  | `boolean` | - | - | - |
| GET | `/addUserService/updateVistaPassword` | `custom.AddUserServiceImpl.updateVistaPassword` | Q  | `boolean` | - | - | - |
| GET | `/addUserService/validateCodePayerMaster` | `custom.AddUserServiceImpl.validateCodePayerMaster` | Q  | `Long` | - | - | - |
| GET | `/addUserService/validateUnitHeadUser` | `custom.AddUserServiceImpl.validateUnitHeadUser` | Q Q Q  | `Doctordepartmentdetails` | - | - | - |
| GET | `/addUserService/validateUserClinicalLevel` | `custom.AddUserServiceImpl.validateUserClinicalLevel` | Q Q Q  | `Doctordepartmentdetails` | - | - | - |
| POST | `/addUserService/acceptTransferUser` | `custom.AddUserServiceImpl.acceptTransferUser` | ?  | `boolean` | Y | - | - |
| POST | `/addUserService/addCabinMaster` | `custom.AddUserServiceImpl.addCabinNew` | JSON  | `void` | Y | - | - |
| POST | `/addUserService/addSupplierUser` | `custom.AddUserServiceImpl.addSupplierUser` | JSON  | `Boolean` | - | - | - |
| POST | `/addUserService/changeUserBuilding` | `custom.AddUserServiceImpl.changeUserBuilding` | Q Q  | `boolean` | Y | - | - |
| POST | `/addUserService/changeUserClinician` | `custom.AddUserServiceImpl.changeUserClinician` | Q Q  | `boolean` | - | - | - |
| POST | `/addUserService/getUserByConcessionCategory` | `custom.AddUserServiceImpl.getUserByConcessionCategory` | JSON  | `List<GetUserByConcessionCategoryResultDto>` | - | - | - |
| POST | `/addUserService/getUserDetailsForUpdate` | `custom.AddUserServiceImpl.getUserDetailsForUpdateNew` | Q  | `AddUserView` | Y | - | - |
| POST | `/addUserService/getUserDetailsList` | `custom.AddUserServiceImpl.getUserDetailsList` | JSON Q Q  | `List<GetUserDetailsResultDto>` | Y | - | - |
| POST | `/addUserService/rejectTransferUser` | `custom.AddUserServiceImpl.rejectTransferUser` | ?  | `boolean` | Y | - | - |
| POST | `/addUserService/saveUser` | `custom.AddUserServiceImpl.saveUser` | JSON  | `void` | - | - | - |
| POST | `/addUserService/sendOtpToUserMobileNo` | `custom.AddUserServiceImpl.sendOtpToUserMobileNo` | Q Q  | `String` | Y | - | - |
| POST | `/addUserService/transferUser` | `custom.AddUserServiceImpl.transferUser` | ?  | `boolean` | Y | - | - |
| POST | `/addUserService/transferUserCancel` | `custom.AddUserServiceImpl.transferUserCancel` | ?  | `boolean` | Y | - | - |
| POST | `/addUserService/updateJanaadharDetailsForEHR` | `custom.AddUserServiceImpl.updateJanaadharDetailsForEHR` | JSON  | `boolean` | - | - | - |
| POST | `/addUserService/updatePermissions` | `custom.AddUserServiceImpl.updatePermissions` | ?  | `void` | - | - | - |
| POST | `/addUserService/updateUser` | `custom.AddUserServiceImpl.updateUser` | JSON  | `void` | - | - | - |
| POST | `/addUserService/verifyUserByFaceauth` | `custom.AddUserServiceImpl.verifyUserByFaceauth` | Q  | `String` | Y | - | - |
| POST | `/addUserService/verifyUserMobileNo` | `custom.AddUserServiceImpl.verifyUserMobileNo` | Q Q Q Q  | `String` | - | - | - |
| PUT | `/addUserService/deleteCabinListById/{id}` | `custom.AddUserServiceImpl.deleteCabinListById` | P  | `void` | - | - | - |
| PUT | `/addUserService/deleteUnitListById/{id}` | `custom.AddUserServiceImpl.deleteUnitListById` | P  | `boolean` | - | - | - |
| PUT | `/addUserService/updateCabinMaster/{id}` | `custom.AddUserServiceImpl.updateCabin` | P JSON  | `void` | Y | - | - |
| POST | `/advancebillReceipt/getAdvanceReceipt` | `custom.AdvanceBillReceiptServiceImpl.getAdvance` | JSON Q ?  | `List<RptGetAdvanceResultDto>` | - | - | - |
| POST | `/advancebillReceipt/getAdvanceReceiptByReceiptNo` | `custom.AdvanceBillReceiptServiceImpl.getAdvanceByReceiptNo` | JSON Q  | `List<RptGetAdvanceResultDto>` | - | - | - |
| POST | `/advancebillReceipt/getAdvanceReceiptHtml` | `custom.AdvanceBillReceiptServiceImpl.getAdvanceReceiptHtml` | JSON  | `Map<String, Object>` | - | - | - |
| POST | `/advancebillReceipt/printAdvanceReceipt` | `custom.AdvanceBillReceiptServiceImpl.printAdvanceReceipt` | ?  | `List<RptGetAdvanceResultDto>` | - | - | - |
| POST | `/allReceipts/getAllAdvanceList` | `custom.AllReceiptsServiceImpl.getAllAdvanceList` | JSON  | `List<AllAdvanceListResultDto>` | - | - | - |
| POST | `/allReceipts/getAllReceipts` | `custom.AllReceiptsServiceImpl.getAllReceipts` | JSON Q  | `List<AllReceiptsListResultDto>` | - | - | - |
| GET | `/appointmentschedule/checkDoctorIdForTheaterId` | `custom.AppointmentScheduleServiceImpl.checkDoctorIdForTheaterId` | Q Q  | `long` | - | - | - |
| GET | `/appointmentschedule/getCabinsByDepartmentId` | `custom.AppointmentScheduleServiceImpl.getCabinsByDepartmentIdDep` | Q Q  | `List<Cabinmaster>` | - | - | - |
| GET | `/appointmentschedule/getCabinsByDepartmentIdOld` | `custom.AppointmentScheduleServiceImpl.getCabinsByDepartmentIdOld` |  | `?` | - | - | - |
| GET | `/appointmentschedule/getDayWiseBuildingMasterByDepartmentId` | `custom.AppointmentScheduleServiceImpl.getDayWiseBuildingMasterByDepartmentId` | Q Q  | `List<Buildingmaster>` | - | - | - |
| GET | `/appointmentschedule/getDepartmentsAppointmentSchedule` | `custom.AppointmentScheduleServiceImpl.getDepartmentsAppointmentSchedule` | Q Q  | `List<AppointmentScheduleSummary>` | - | - | - |
| GET | `/appointmentschedule/getDepartmentsAppointmentScheduleById` | `custom.AppointmentScheduleServiceImpl.getDepartmentsAppointmentScheduleById` | Q  | `AppointmentScheduleSummary` | - | - | - |
| GET | `/appointmentschedule/getDepartmentScheduleSummary` | `custom.AppointmentScheduleServiceImpl.getDepartmentScheduleSummary` | Q Q  | `List<AppointmentScheduleSummary>` | Y | - | - |
| GET | `/appointmentschedule/getDeptsForTsu` | `custom.AppointmentScheduleServiceImpl.getDeptsForTsu` | - | `List<Departmentmaster>` | - | - | - |
| GET | `/appointmentschedule/getTelemedicinePatientUnit` | `custom.AppointmentScheduleServiceImpl.getTelemedicinePatientUnit` | - | `List<TeleconsultPatientUnitDetails>` | - | - | - |
| GET | `/appointmentschedule/getTelemedicinePatientUnitDetails` | `custom.AppointmentScheduleServiceImpl.getTelemedicinePatientUnitDetails` | - | `Object[]` | - | - | - |
| GET | `/appointmentschedule/getTelemedicineSpecialityUnit` | `custom.AppointmentScheduleServiceImpl.getTelemedicineSpecialityUnit` | Q  | `List<Unitmaster>` | - | - | - |
| GET | `/appointmentschedule/getTelemedicineSpecialityUnitById` | `custom.AppointmentScheduleServiceImpl.getTelemedicineSpecialityUnitById` | Q  | `List<Unitmaster>` | - | - | - |
| GET | `/appointmentschedule/getUnitsForTelemedicine` | `custom.AppointmentScheduleServiceImpl.getUnitsForTelemedicine` | - | `List<Unitmaster>` | - | - | - |
| GET | `/appointmentschedule/getUnitsForTelemedicineByDesc` | `custom.AppointmentScheduleServiceImpl.getUnitsForTelemedicineById` | Q  | `List<Unitmaster>` | - | - | - |
| POST | `/appointmentschedule/addOrRemoveSlots` | `custom.AppointmentScheduleServiceImpl.addAndRemoveAppointmentSchedule` | HDR  | `void` | - | - | - |
| POST | `/appointmentschedule/deleteAllDepartmentsSchedule` | `custom.AppointmentScheduleServiceImpl.deleteAllDepartmentsSchedule` | Q  | `void` | Y | - | - |
| POST | `/appointmentschedule/deleteDepartmentsAppointmentScheduleById` | `custom.AppointmentScheduleServiceImpl.deleteDepartmentsAppointmentScheduleById` | Q  | `Boolean` | - | - | - |
| POST | `/appointmentschedule/getAppointmentSchedule` | `custom.AppointmentScheduleServiceImpl.getAppointmentScheduleService` |  | `?` | - | - | - |
| POST | `/appointmentschedule/getAppointmentScheduleNew` | `custom.AppointmentScheduleServiceImpl.getAppointmentScheduleServiceNew` |  | `?` | Y | - | - |
| POST | `/appointmentschedule/getAppointmentScheduleTeleconsult` | `custom.AppointmentScheduleServiceImpl.getAppointmentScheduleTeleconsult` |  | `?` | - | - | - |
| POST | `/appointmentschedule/getAvailableAppointmentSchedule` | `custom.AppointmentScheduleServiceImpl.getAvailableAppointmentSchedule` |  | `?` | - | - | - |
| POST | `/appointmentschedule/getCabinWiseApointmentSlotCount` | `custom.AppointmentScheduleServiceImpl.getCabinWiseApointmentSlotCount` | JSON  | `List<Cabinmaster>` | - | - | - |
| POST | `/appointmentschedule/getCabinWiseSlotForTelemedicine` | `custom.AppointmentScheduleServiceImpl.getCabinWiseSlotForTelemedicine` | JSON  | `List<Cabinmaster>` | - | - | - |
| POST | `/appointmentschedule/getDepartmentAppointmentScheduleNew` | `custom.AppointmentScheduleServiceImpl.getDepartmentAppointmentScheduleServiceNew` |  | `?` | - | - | - |
| POST | `/appointmentschedule/getDepartmentsByDate` | `custom.AppointmentScheduleServiceImpl.getDepartmentsByDate` | JSON  | `List<Departmentmaster>` | - | - | - |
| GET | `/bedmanagement/checkBedNameExistsinMaster` | `custom.BedManagementServiceImpl.checkBedNameExistsinMaster` | Q Q  | `Number` | - | - | - |
| GET | `/bedmanagement/checkDischargeClearanceCount` | `custom.BedManagementServiceImpl.clearanceCount` | - | `Long` | Y | - | - |
| GET | `/bedmanagement/getAdditionalServicesByAdmissionId` | `custom.BedManagementServiceImpl.getAdditionalServicesByAdmissionId` | Q Q  | `PatientRegAdmissionDto` | - | - | - |
| GET | `/bedmanagement/getAllCradles` | `custom.BedManagementServiceImpl.getAllCradles` | Q Q Q  | `WardBedDto` | - | - | - |
| GET | `/bedmanagement/getAvailableBeds` | `custom.BedManagementServiceImpl.getAvailableBeds` | - | `List<ClassWardBedDto>` | - | - | - |
| GET | `/bedmanagement/getBedStatusById` | `custom.BedManagementServiceImpl.getBedStatusById` | Q Q  | `Long` | - | - | - |
| GET | `/bedmanagement/getBlockSubmit` | `custom.BedManagementServiceImpl.udateBedStatus` | Q Q Q  | `Long` | - | - | - |
| GET | `/bedmanagement/getClearanceCount` | `custom.BedManagementServiceImpl.getClearanceCount` | - | `Long` | - | - | - |
| GET | `/bedmanagement/getCradleCount` | `custom.BedManagementServiceImpl.getCradleCount` | Q  | `Map<String, Long>` | - | - | - |
| GET | `/bedmanagement/getDeadDischargeTransferPatientList` | `custom.BedManagementServiceImpl.getDeadDischargeTransferPatientList` | Q Q  | `List<DeathPatientSearchDto>` | - | - | - |
| GET | `/bedmanagement/getOTTransferDetails` | `custom.BedManagementServiceImpl.getOTTransferDetails` | Q Q  | `List<TransferResultDto>` | - | - | - |
| GET | `/bedmanagement/getTransferDetails` | `custom.BedManagementServiceImpl.getTransferDetails` | Q Q  | `List<TransferResultDto>` | - | - | - |
| GET | `/bedmanagement/isBedAvailable` | `custom.BedManagementServiceImpl.checkBedAvailability` | Q  | `Boolean` | - | - | - |
| POST | `/bedmanagement/addTempVerification` | `custom.BedManagementServiceImpl.addTempVerificationDataToChiranjeeviApp` | JSON  | `String` | - | - | - |
| POST | `/bedmanagement/addVerification` | `custom.BedManagementServiceImpl.addVerificationDataToChiranjeeviApp` | JSON  | `PatientVerificationDto` | - | - | - |
| POST | `/bedmanagement/getBabiesDetails` | `custom.BedManagementServiceImpl.getBabiesDetails` | JSON  | `Map<Long, List<PatientAdmissionListResultDto>>` | - | - | - |
| POST | `/bedmanagement/getBedAvailabilityCount` | `custom.BedManagementServiceImpl.getBedAvailabilityCount` | JSON  | `ShowBedAvailabilityDto` | - | - | - |
| POST | `/bedmanagement/getBedCount` | `custom.BedManagementServiceImpl.getBedCount` | JSON Q Q  | `Map<String,Long>` | - | - | - |
| POST | `/bedmanagement/getBedDetails` | `custom.BedManagementServiceImpl.getBedDetails` | JSON Q Q  | `List<Bedmaster>` | - | - | - |
| POST | `/bedmanagement/getBuildingBedDetails` | `custom.BedManagementServiceImpl.getBuildingBedDetails` | JSON Q  | `List<ClassWardBedDto>` | - | - | - |
| POST | `/bedmanagement/getBuildingMasterDetails` | `custom.BedManagementServiceImpl.getBuildingMasterDetails` | JSON Q Q  | `List<ClassWardBedDto>` | - | - | - |
| POST | `/bedmanagement/getBuildingMasterDetailsNew` | `custom.BedManagementServiceImpl.getBuildingMasterDetailsNew` | JSON Q Q  | `List<WardBedDto>` | - | - | - |
| POST | `/bedmanagement/getBuildingWardDetails` | `custom.BedManagementServiceImpl.getBuildingWardDetails` | JSON Q Q  | `List<ClassWardBedDto>` | - | - | - |
| POST | `/bedmanagement/getClassMasterDetails` | `custom.BedManagementServiceImpl.` |  | `?` | - | - | - |
| POST | `/bedmanagement/getIsRetainable` | `custom.BedManagementServiceImpl.getIsRetainable` | Q Q  | `Boolean` | Y | - | - |
| POST | `/bedmanagement/getMortuaryWards` | `custom.BedManagementServiceImpl.getMortuaryWards` | - | `List<Wardmaster>` | - | - | - |
| POST | `/bedmanagement/getOtBedCount` | `custom.BedManagementServiceImpl.getOtBedCount` | JSON Q Q  | `Map<String, Long>` | - | - | - |
| POST | `/bedmanagement/getPendencyDashboardData` | `custom.BedManagementServiceImpl.getPendencyDashboardData` | Q Q Q  | `PendencyDashboardView` | - | - | - |
| POST | `/bedmanagement/getWardDetails` | `custom.BedManagementServiceImpl.getWardBedDetails` | JSON Q Q  | `List<ClassWardBedDto>` | - | - | - |
| POST | `/bedmanagement/getWardDetailsForAdmissionBlockMaster` | `custom.BedManagementServiceImpl.getWardDetailsForAdmissionBlockMaster` | JSON Q  | `List<WardBedDto>` | - | - | - |
| POST | `/bedmanagement/initiateDischargeClearance` | `custom.BedManagementServiceImpl.initiateDischargeClearance` | JSON  | `void` | - | - | - |
| POST | `/bedmanagement/markLabClearance` | `custom.BedManagementServiceImpl.markLabClearance` | JSON  | `void` | Y | - | - |
| POST | `/bedmanagement/printWardBedDetailsInquiry` | `custom.BedManagementServiceImpl.printWardBedDetailsInquiry` | Q Q  | `String[]` | - | - | - |
| POST | `/cancelBill/` | `custom.BillCancellationServiceImpl.billCancel` | JSON ?  | `CancelBillDto` | - | - | - |
| POST | `/cancelBill/printCancelBillAudit` | `custom.BillCancellationServiceImpl.printCancelBillAudit` | JSON  | `void` | - | - | - |
| POST | `/cancelBill/refundReceiptByIds` | `custom.BillCancellationServiceImpl.refundReceiptByIds` | JSON Q  | `BillReceiptView` | Y | - | - |
| POST | `/billDet/getBillDetails` | `custom.BillDetServiceImpl.findBillDetails` |  | `?` | - | - | - |
| GET | `/billReceipt/authorityBillPaidReceipt` | `custom.BillReceiptServiceImpl.authorityBillPaidReceipt` | Q  | `String` | Y | - | - |
| GET | `/billReceipt/downloadActualFile` | `custom.BillReceiptServiceImpl.downloadReceipt` | Q Q ?  | `void` | - | - | - |
| GET | `/billReceipt/downloadFile` | `custom.BillReceiptServiceImpl.downloadReceiptToken` | HDR Q Q  | `String` | - | - | - |
| GET | `/billReceipt/downloadFileOpen` | `custom.BillReceiptServiceImpl.downloadReceiptOpen` | Q Q ?  | `byte[]` | - | - | - |
| GET | `/billReceipt/printAuthorityPaymentDetail` | `custom.BillReceiptServiceImpl.printAuthorityPaymentDetail` | Q Q  | `String` | - | - | - |
| POST | `/billReceipt/` | `custom.BillReceiptServiceImpl.getBillReceipt` | JSON Q  | `BillReceiptView` | - | - | - |
| POST | `/billReceipt/advanceRefundReciept` | `custom.BillReceiptServiceImpl.getAdvanceRefundReceipt` | JSON Q  | `BillReceiptView` | Y | - | - |
| POST | `/billReceipt/allAdvance` | `custom.BillReceiptServiceImpl.getRefundReceipt` | JSON  | `List<AllAdvanceResultDto>` | - | - | - |
| POST | `/billReceipt/billReceiptWithPharmacy` | `custom.BillReceiptServiceImpl.getBillReceiptWithPharmacy` | JSON Q  | `BillReceiptView` | Y | - | - |
| POST | `/billReceipt/cancelIpBill` | `custom.BillReceiptServiceImpl.getCancelIpReceipt` | JSON Q  | `BillReceiptView` | Y | - | - |
| POST | `/billReceipt/cancelOpBill` | `custom.BillReceiptServiceImpl.getCancelOpReceipt` | JSON Q  | `BillReceiptView` | Y | - | - |
| POST | `/billReceipt/downloadBillPharmacyRec` | `custom.BillReceiptServiceImpl.downloadBillPharmacyReceipt` | JSON ?  | `PharmacyBillListView` | - | - | - |
| POST | `/billReceipt/getPatientPackageConsumption` | `custom.BillReceiptServiceImpl.getPatientPackageConsumption` | JSON  | `List<HealthplanPackageConsumptionDetailsDto>` | - | - | - |
| POST | `/billReceipt/ipdBillReceipt` | `custom.BillReceiptServiceImpl.getIPBillReceipt` | JSON Q Q  | `BillReceiptView` | - | - | - |
| POST | `/billReceipt/printCreditNoteForIP` | `custom.BillReceiptServiceImpl.printCreditNoteForIP` | JSON Q  | `CreditNote` | Y | - | - |
| POST | `/billReceipt/printCreditNoteForOP` | `custom.BillReceiptServiceImpl.printCreditNoteForOP` | JSON Q  | `CreditNote` | Y | - | - |
| POST | `/billReceipt/printPackageRefundReceipt` | `custom.BillReceiptServiceImpl.printPackageRefundReceipt` | JSON Q  | `CreditNote` | Y | - | - |
| POST | `/billReceipt/refundReciept` | `custom.BillReceiptServiceImpl.getRefundReceipt` | JSON  | `List<RefundReceiptResultDto>` | - | - | - |
| POST | `/billReceipt/setIsBillReceiptPrintedToTrue` | `custom.BillReceiptServiceImpl.setIsBillReceiptPrintedToTrue` | Q Q  | `void` | Y | - | - |
| POST | `/billSummary/` | `custom.BillSummaryServiceImpl.` |  | `?` | - | - | - |
| POST | `/billSummary/draftBillSummary` | `custom.BillSummaryServiceImpl.getDraftBillSummary` | JSON Q  | `DraftBillView` | - | - | - |
| POST | `/billSummary/getAllBills` | `custom.BillSummaryServiceImpl.getAllBills` | JSON Q  | `List<OPDBillReceiptResultNewDto>` | Y | - | - |
| POST | `/billSummary/getAllBillsForOnline` | `custom.BillSummaryServiceImpl.getAllBillsForOnline` | JSON Q  | `List<InvestigationAndBillResultDto>` | - | - | - |
| POST | `/billSummary/getAllCombineBills` | `custom.BillSummaryServiceImpl.getAllCombineBills` | JSON  | `List<BillListForSearch_NewResultDto_3>` | - | - | - |
| POST | `/billSummary/getAllCreditNoteBills` | `custom.BillSummaryServiceImpl.getAllCreditNoteBills` | JSON  | `List<BillListForSearch_CreditList>` | - | - | - |
| POST | `/billSummary/getServiceListForPartialBillPayment` | `custom.BillSummaryServiceImpl.getServiceListForPartialBillPayment` | JSON  | `BillSummaryView` | - | - | - |
| POST | `/canceladmission/cancel` | `custom.CancelAdmissionServiceImpl.cancelPatientAdmission` | JSON  | `CancelAdmissionView` | Y | - | - |
| POST | `/doctorlist/getDoctorMasterDetailsListbyDepartmentId` | `custom.DoctorListServiceImpl.getDoctorMasterDetailsListbyDepartmentId` |  | `?` | - | - | - |
| POST | `/doctorlist/getDoctorMasterDetailsListbyDoctorId` | `custom.DoctorListServiceImpl.getDoctorMasterDetailsList` | JSON  | `Collection<DoctorMasterDetailsListResultDto_1_New>` | - | - | - |
| GET | `/walkinVisit/activePatientAppointmentList` | `custom.EmergencyAndWalkinVisitServiceImpl.getActivePatientAppointmentList` |  | `?` | - | - | - |
| GET | `/walkinVisit/document/{id}` | `custom.EmergencyAndWalkinVisitServiceImpl.getImageById` | HDR P  | `String` | Y | - | - |
| GET | `/walkinVisit/documentActual/{id}` | `custom.EmergencyAndWalkinVisitServiceImpl.getImageById` | P ?  | `void` | - | - | - |
| GET | `/walkinVisit/fetchDrugInstruction` | `custom.EmergencyAndWalkinVisitServiceImpl.fetchDrugInstruction` | Q Q ?  | `String` | - | - | - |
| GET | `/walkinVisit/getAvaliableCabinAndBookedDoctorIds` | `custom.EmergencyAndWalkinVisitServiceImpl.` |  | `?` | - | - | - |
| GET | `/walkinVisit/getLastVisitDetails` | `custom.EmergencyAndWalkinVisitServiceImpl.getLastVisitDetails` | Q  | `Encounter` | Y | - | - |
| GET | `/walkinVisit/getLastVisitStatus` | `custom.EmergencyAndWalkinVisitServiceImpl.getLastVisitStatus` | Q Q  | `String` | Y | - | - |
| GET | `/walkinVisit/getLastVisitTypeId` | `custom.EmergencyAndWalkinVisitServiceImpl.getLastVisitTypeId` | Q Q  | `Object[]` | - | - | - |
| GET | `/walkinVisit/getMlcDetailsByVisit` | `custom.EmergencyAndWalkinVisitServiceImpl.getMlcDetailsByVisit` | Q  | `MLCDetails` | - | - | - |
| GET | `/walkinVisit/getMrnoSeriesWithoutExistingMRNo` | `custom.EmergencyAndWalkinVisitServiceImpl.getMrnoSeriesWithoutExistingMRNo` | Q  | `List<String>` | - | - | - |
| GET | `/walkinVisit/getMrnSeries` | `custom.EmergencyAndWalkinVisitServiceImpl.getMrnoSeries` | Q  | `List<MrnSeries>` | - | - | - |
| GET | `/walkinVisit/getSolrSyncStatus____` | `custom.EmergencyAndWalkinVisitServiceImpl.getSolrSyncStatus` | Q  | `String[]` | - | - | - |
| GET | `/walkinVisit/getvisitTypes` | `custom.EmergencyAndWalkinVisitServiceImpl.getvisitTypes` | Q Q  | `List<Visittypemaster>` | Y | - | - |
| GET | `/walkinVisit/indexMissingEncounterInSolr____` | `custom.EmergencyAndWalkinVisitServiceImpl.indexMissingEncounterInSolr` | Q Q Q  | `void` | - | - | - |
| GET | `/walkinVisit/indexMissingEncountersInSolr____` | `custom.EmergencyAndWalkinVisitServiceImpl.indexMissingEncountersInSolr` | Q Q  | `void` | - | - | - |
| GET | `/walkinVisit/indexMissingPatientInSolr____` | `custom.EmergencyAndWalkinVisitServiceImpl.indexMissingPatientInSolr` | Q Q  | `void` | - | - | - |
| GET | `/walkinVisit/mlcFile/{id}` | `custom.EmergencyAndWalkinVisitServiceImpl.getImageByName` | HDR P  | `String` | - | - | - |
| GET | `/walkinVisit/mlcFileActual/{id}` | `custom.EmergencyAndWalkinVisitServiceImpl.getImageByName` | P ?  | `void` | - | - | - |
| GET | `/walkinVisit/searchToken` | `custom.EmergencyAndWalkinVisitServiceImpl.searchToken` | Q  | `List<PatientToken>` | - | - | - |
| GET | `/walkinVisit/searchVisitbyToken` | `custom.EmergencyAndWalkinVisitServiceImpl.searchVisitbyToken` | Q  | `PatientAppointmentandRegDto` | Y | - | - |
| GET | `/walkinVisit/stopSolrSync____` | `custom.EmergencyAndWalkinVisitServiceImpl.stopSolrSync` | Q  | `void` | - | - | - |
| POST | `/walkinVisit/aadharBiometricVarification` | `custom.EmergencyAndWalkinVisitServiceImpl.aadharBiometricVarification` | JSON  | `Map<String, String>` | - | - | - |
| POST | `/walkinVisit/aadharOtpGeneration` | `custom.EmergencyAndWalkinVisitServiceImpl.aadharOtpGeneration` | JSON  | `Map<String, String>` | - | - | - |
| POST | `/walkinVisit/aadharOtpVarification` | `custom.EmergencyAndWalkinVisitServiceImpl.aadharOtpVarification` | JSON  | `Map<String, String>` | - | - | - |
| POST | `/walkinVisit/addEmergencyRegistration` | `custom.EmergencyAndWalkinVisitServiceImpl.addEmergencyRegistration` | JSON Q  | `PatientAppointmentandRegDto` | Y | - | - |
| POST | `/walkinVisit/addFollowUpVisit` | `custom.EmergencyAndWalkinVisitServiceImpl.addFollowUpVisit` | JSON  | `void` | - | - | - |
| POST | `/walkinVisit/addPatientPhoto` | `custom.EmergencyAndWalkinVisitServiceImpl.addPatientPhoto` | JSON  | `AddPatientImageDto` | Y | - | - |
| POST | `/walkinVisit/addwalkinVisit` | `custom.EmergencyAndWalkinVisitServiceImpl.addwalkinVisit` | JSON Q  | `PatientAppointmentandRegDto` | - | - | - |
| POST | `/walkinVisit/callPatient` | `custom.EmergencyAndWalkinVisitServiceImpl.updateCallPatientData` | JSON  | `void` | Y | - | - |
| POST | `/walkinVisit/callPatientNew` | `custom.EmergencyAndWalkinVisitServiceImpl.updateCallPatientDataNew` | JSON  | `void` | Y | - | - |
| POST | `/walkinVisit/printAttendantPass` | `custom.EmergencyAndWalkinVisitServiceImpl.printAttendantPass` | JSON Q Q  | `String[]` | - | - | - |
| POST | `/walkinVisit/printBHTBarcode` | `custom.EmergencyAndWalkinVisitServiceImpl.printBHTBarcode` | JSON Q ?  | `String[]` | - | - | - |
| POST | `/walkinVisit/printConsentForm` | `custom.EmergencyAndWalkinVisitServiceImpl.printConsentForm` | Q JSON  | `String[]` | Y | - | - |
| POST | `/walkinVisit/printDischargeSummary` | `custom.EmergencyAndWalkinVisitServiceImpl.printDischargeSummary` | JSON  | `String[]` | Y | - | - |
| POST | `/walkinVisit/printIpLable` | `custom.EmergencyAndWalkinVisitServiceImpl.printIpLable` | JSON  | `String[]` | - | - | - |
| POST | `/walkinVisit/printPatientHealthCard` | `custom.EmergencyAndWalkinVisitServiceImpl.printPatientHealthCard` | JSON  | `Map<String, Object>` | - | - | - |
| POST | `/walkinVisit/rislPrintIpLable` | `custom.EmergencyAndWalkinVisitServiceImpl.rislPrintIpLable` | JSON  | `Map<String, Object>` | - | - | - |
| POST | `/walkinVisit/rislPrintOpLable` | `custom.EmergencyAndWalkinVisitServiceImpl.rislPrintOpLable` | JSON  | `Map<String, Object>` | - | - | - |
| POST | `/walkinVisit/saveDrugInstruction` | `custom.EmergencyAndWalkinVisitServiceImpl.saveDrugInstruction` | Q Q JSON  | `void` | - | - | - |
| POST | `/walkinVisit/selfPatientRegistation` | `custom.EmergencyAndWalkinVisitServiceImpl.selfPatientRegistration` | JSON  | `PatientAppointmentandRegDto` | - | - | - |
| POST | `/walkinVisit/updateVisitType` | `custom.EmergencyAndWalkinVisitServiceImpl.updateVisitType` | ?  | `void` | Y | - | - |
| PUT | `/walkinVisit/doctorcheckout` | `custom.EmergencyAndWalkinVisitServiceImpl.updateDoctorCheckout` | Q  | `void` | Y | - | - |
| PUT | `/walkinVisit/releaseCabin` | `custom.EmergencyAndWalkinVisitServiceImpl.releaseCabin` | Q Q  | `List<GetChargesResultDto>` | - | - | - |
| PUT | `/walkinVisit/updateCabinandDoctId` | `custom.EmergencyAndWalkinVisitServiceImpl.updateCabinandDoctId` | JSON  | `void` | Y | - | - |
| PUT | `/walkinVisit/updateMlcDetailsbyVisitId` | `custom.EmergencyAndWalkinVisitServiceImpl.updateMlcDetailsbyVisitId` | JSON  | `void` | - | - | - |
| GET | `/eAushadiIndent/getConsumptionDataForItemSalePatients` | `custom.InventoryEAushadiIntegrationImpl.getItemConsumptionData` | Q Q Q  | `List<ItemSaleConsumptionView>` | - | - | - |
| GET | `/eAushadiIndent/getKaysonsCurrentStockDetails` | `custom.InventoryEAushadiIntegrationImpl.getCurrentStockKaysonsDetails` | Q  | `List<EAushadhiCurrentStockResponseDto>` | Y | - | - |
| POST | `/eAushadiIndent/addCurrentStockDetailsByStoreId` | `custom.InventoryEAushadiIntegrationImpl.addCurrentStockDetailsByStoreId` | Q  | `List<EAushadhiCurrentStockResponseDto>` | - | - | - |
| POST | `/eAushadiIndent/addOpeningBalanceForKaysonsStock` | `custom.InventoryEAushadiIntegrationImpl.addOpeningBalanceForKaysonsStock` | JSON  | `void` | - | - | - |
| POST | `/eAushadiIndent/createunMappedStores` | `custom.InventoryEAushadiIntegrationImpl.createunMappedStores` | JSON  | `Boolean` | - | - | - |
| POST | `/eAushadiIndent/getItemStockListByStoreId` | `custom.InventoryEAushadiIntegrationImpl.getItemStockListByStoreId` | JSON  | `List<ItemConsumptionView>` | - | - | - |
| POST | `/eAushadiIndent/indent` | `custom.InventoryEAushadiIntegrationImpl.indent` | JSON  | `IndentEAushadiResponse` | - | - | - |
| POST | `/eAushadiIndent/indentIssueSucces` | `custom.InventoryEAushadiIntegrationImpl.indentIssueSucces` | JSON  | `IndentReturnEAushadiResponse` | Y | - | - |
| POST | `/eAushadiIndent/returnIndentAfterApproveForEaushadi` | `custom.InventoryEAushadiIntegrationImpl.returnIndentAfterApproveForEaushadi` | Q Q  | `IssueView` | Y | - | - |
| POST | `/eAushadiIndent/returnIndentForEaushadi` | `custom.InventoryEAushadiIntegrationImpl.returnIndentForEaushadi` | JSON Q  | `IssueView` | Y | - | - |
| POST | `/eAushadiIndent/returnItemUpdate` | `custom.InventoryEAushadiIntegrationImpl.returnItemUpdate` | JSON  | `IndentIssueEAushadiResponse` | Y | - | - |
| POST | `/eAushadiIndent/sendOTPToUserForKaysonsStockUpdate` | `custom.InventoryEAushadiIntegrationImpl.sendOTPToUserForKaysonsStockUpdate` | - | `String` | - | - | - |
| POST | `/eAushadiIndent/updateEAushadhiItemMaster` | `custom.InventoryEAushadiIntegrationImpl.updateEAushadhiItemMaster` | - | `void` | Y | - | - |
| POST | `/eAushadiIndent/updateQCStatus` | `custom.InventoryEAushadiIntegrationImpl.updateQCStatusAgainstItemBatch` | JSON  | `IndentIssueEAushadiResponse` | Y | - | - |
| POST | `/eAushadiIndent/validateOTPToUserForKaysonsStockUpdate` | `custom.InventoryEAushadiIntegrationImpl.validateOTPToUserForKaysonsStockUpdate` | Q  | `String` | - | - | - |
| DELETE | `/inventoryStockList/removeItemGroupCategoryForSupplier` | `custom.InventoryServiceImpl.deleteMedicationOrder` | Q Q Q  | `boolean` | - | - | - |
| DELETE | `/inventoryStockList/updatePurchaseOrder` | `custom.InventoryServiceImpl.deletePurchaseOrderDetails` | JSON  | `DeletePurchaseOrderDetailsDto` | Y | - | - |
| GET | `/inventoryStockList/addSupplierItemsByItemCategory` | `custom.InventoryServiceImpl.addSupplierItemsByItemCategory` | Q Q  | `boolean` | - | - | - |
| GET | `/inventoryStockList/addSupplierItemsByItemGroup` | `custom.InventoryServiceImpl.addSupplierItemsByItemGroup` | Q Q  | `boolean` | - | - | - |
| GET | `/inventoryStockList/checkItemCode` | `custom.InventoryServiceImpl.checkItemCode` | Q  | `boolean` | Y | - | - |
| GET | `/inventoryStockList/checkLastGrnDetails` | `custom.InventoryServiceImpl.showLastGrnDetails` | Q Q  | `List<PreviousPOGRNDetailsDto>` | - | - | - |
| GET | `/inventoryStockList/checkPORaisedDetails` | `custom.InventoryServiceImpl.checkPORaisedDetails` | Q Q  | `List<PreviousPOGRNDetailsDto>` | - | - | - |
| GET | `/inventoryStockList/checkReffItemIdForAsset` | `custom.InventoryServiceImpl.` | Q Q  | `Number` | Y | - | - |
| GET | `/inventoryStockList/deletePlanById` | `custom.InventoryServiceImpl.deletePlanId` | Q Q Q  | `String` | - | - | - |
| GET | `/inventoryStockList/deleteRateContractItemDetails` | `custom.InventoryServiceImpl.deleteRateContractItemDetails` | Q Q  | `Long` | Y | - | - |
| GET | `/inventoryStockList/freezItemBatchStatus` | `custom.InventoryServiceImpl.freezItemBatchStatus` | Q Q Q Q  | `boolean` | Y | - | - |
| GET | `/inventoryStockList/getAllApprovalCount` | `custom.InventoryServiceImpl.getAllApprovalCount` | Q Q  | `Map<String,Number>` | - | - | - |
| GET | `/inventoryStockList/getAllItemByNameforStore` | `custom.InventoryServiceImpl.getItemByNameforStoreAll` | Q  | `List<ItemByNameResultDto>` | - | - | - |
| GET | `/inventoryStockList/getAllItemCategories` | `custom.InventoryServiceImpl.getAllItemCategories` | - | `List<Itemcategory>` | - | - | - |
| GET | `/inventoryStockList/getAllStoresBySameCategoryId` | `custom.InventoryServiceImpl.getDirectItemIssueList` | Q  | `List<StoreListByCategoryResultDto>` | - | - | - |
| GET | `/inventoryStockList/getAllStoresItemCategiries` | `custom.InventoryServiceImpl.getAllStoresItemCategiries` | - | `List<StoreItemCategoryDetailsDto>` | Y | - | - |
| GET | `/inventoryStockList/getAttachedItemsBySupplierAndCategory` | `custom.InventoryServiceImpl.getAttachedItemsBySupplierAndCategory` | Q Q  | `SupplierItemResultDto` | - | - | - |
| GET | `/inventoryStockList/getBatchDetails` | `custom.InventoryServiceImpl.getBatchDetails` | Q Q Q  | `BatchDetailsResultDto` | - | - | - |
| GET | `/inventoryStockList/getBatchNoForAsset` | `custom.InventoryServiceImpl.getBatchNoForAsset` | Q Q Q  | `List<String>` | - | - | - |
| GET | `/inventoryStockList/getBatchNoForAssetIssueIndent` | `custom.InventoryServiceImpl.getBatchNoForAssetIssueIndent` | Q Q Q  | `Map<Long,List<String>>` | - | - | - |
| GET | `/inventoryStockList/getBatchWiseDocument` | `custom.InventoryServiceImpl.getBatchWiseDocument` | Q  | `PatientIDDocumentDto` | Y | - | - |
| GET | `/inventoryStockList/getBrandIdForAsset` | `custom.InventoryServiceImpl.getBrandIdForAsset` | Q Q Q  | `List<String>` | - | - | - |
| GET | `/inventoryStockList/getBrandIdForAssetIssueIndent` | `custom.InventoryServiceImpl.getBrandIdForAssetIssueIndent` | Q Q Q  | `Map<Long,List<String>>` | - | - | - |
| GET | `/inventoryStockList/getConsumptionDataForUnit` | `custom.InventoryServiceImpl.getItemConsumptionData` | Q Q  | `List<ItemConsumptionView>` | - | - | - |
| GET | `/inventoryStockList/getDepartmentById` | `custom.InventoryServiceImpl.getDepartmentDetailsById` | Q  | `List<Unitdepartmentdetails>` | Y | - | - |
| GET | `/inventoryStockList/getEAushadiItemStockDetails` | `custom.InventoryServiceImpl.getEAushadiItemStockDetails` | Q  | `EAushadiItemStock` | Y | - | - |
| GET | `/inventoryStockList/getGRNItemsByGrnId` | `custom.InventoryServiceImpl.getGRNItemsByItemId` | Q  | `List<GRNItemListResultDto>` | - | - | - |
| GET | `/inventoryStockList/getGrnItemsDetails` | `custom.InventoryServiceImpl.getGrnItemsDetails` | Q  | `List<GRNResultRptforDirectGRNDto>` | Y | - | - |
| GET | `/inventoryStockList/getHospitalWisePurchaseRequestList` | `custom.InventoryServiceImpl.getHospitalWisePurchaseRequestList` | Q  | `List<HospitalWisePurchaseRequestDto>` | - | - | - |
| GET | `/inventoryStockList/getIndentIssueDetailsById` | `custom.InventoryServiceImpl.getIndentIssueDetailsById` | Q  | `List<GetIssueItemDetailsResultDto>` | Y | - | - |
| GET | `/inventoryStockList/getIndentListByStoreIdforIndent` | `custom.InventoryServiceImpl.getItemByNameforIndent` | Q  | `ShowItemListforIndentView` | Y | - | - |
| GET | `/inventoryStockList/getIssueItemsDetailsById` | `custom.InventoryServiceImpl.getIssueItemsDetailsById` | Q  | `List<GetIssueItemDetailsforItemReceiveListResultDto>` | Y | - | - |
| GET | `/inventoryStockList/getIssueItemsDetailsByIdForRecive` | `custom.InventoryServiceImpl.getIssueItemsDetailsByIdForRecive` | Q  | `IssueItemDetailsView` | - | - | - |
| GET | `/inventoryStockList/getIssueNumberforGRN` | `custom.InventoryServiceImpl.getIssueNumberforGRN` | Q Q  | `List<Object[]>` | - | - | - |
| GET | `/inventoryStockList/getItemByNameforAssetStore` | `custom.InventoryServiceImpl.getItemByNameforAssetStore` | Q  | `List<ItemByNameResultEADto>` | Y | - | - |
| GET | `/inventoryStockList/getItemByNameforDirectPO` | `custom.InventoryServiceImpl.getItemByName` | Q  | `List<ItemByNameResultDto>` | - | - | - |
| GET | `/inventoryStockList/getItemByNameforStore` | `custom.InventoryServiceImpl.getItemByNameforStore` | Q  | `List<ItemByNameResultEADto>` | - | - | - |
| GET | `/inventoryStockList/getItemByNameforStoreFormEaushadiItems` | `custom.InventoryServiceImpl.getItemByNameforStoreFormEaushadiItems` | Q Q  | `List<ItemByNameResultEADto>` | - | - | - |
| GET | `/inventoryStockList/getItemCategoriesByItemGroup` | `custom.InventoryServiceImpl.getItemCategoriesByItemGroup` | Q Q  | `List<Itemcategory>` | Y | - | - |
| GET | `/inventoryStockList/getItemCategorysByGroupId` | `custom.InventoryServiceImpl.getItemCategorysByGroupId` | Q  | `List<ItemCategoryResultDto>` | - | - | - |
| GET | `/inventoryStockList/getItemCategorysByGroupIdAndStoreId` | `custom.InventoryServiceImpl.getItemCategorysByGroupIdAndStoreId` | Q Q  | `List<StoreListByCategoryResultDto>` | - | - | - |
| GET | `/inventoryStockList/getItemCurrentPurchaseOrderIndentList` | `custom.InventoryServiceImpl.getItemCurrentPurchaseOrderIndentList` | - | `List<CurrentPurchaseOrderIndentListResultDto>` | Y | - | - |
| GET | `/inventoryStockList/getItemCurrentReorderListforIndent` | `custom.InventoryServiceImpl.getItemCurrentReorderListforIndent` | Q  | `ShowItemListforIndentReorderView` | - | - | - |
| GET | `/inventoryStockList/getItemDetailsByBatchForOpBal` | `custom.InventoryServiceImpl.getItemDetailsByBatchForOpBal` | Q Q  | `BillReceiptView` | - | - | - |
| GET | `/inventoryStockList/getItemDetailsByBatchId` | `custom.InventoryServiceImpl.getItemDetailsByBatchId` | Q Q  | `BillReceiptView` | - | - | - |
| GET | `/inventoryStockList/getItemGroupByStoreId` | `custom.InventoryServiceImpl.getItemGroupByStoreId` | Q  | `List<Itemgroup>` | Y | - | - |
| GET | `/inventoryStockList/getItemLatestBatchDetails` | `custom.InventoryServiceImpl.` | Q Q  | `Itembatchmaster` | - | - | - |
| GET | `/inventoryStockList/getItemListbySupplierId` | `custom.InventoryServiceImpl.getItemListbySupplierId` | Q  | `List<ItemListbySupplierIdResultDto>` | Y | - | - |
| GET | `/inventoryStockList/getItemNameBySupplier` | `custom.InventoryServiceImpl.getItemByNameBySupplier` | Q  | `List<ItemBySupplierResultDto>` | Y | - | - |
| GET | `/inventoryStockList/getItemsByCategoryList` | `custom.InventoryServiceImpl.getItemGroupList` | Q  | `List<Itemmaster>` | - | - | - |
| GET | `/inventoryStockList/getItemsByItemCategory` | `custom.InventoryServiceImpl.getItemsByItemCategory` | Q Q  | `List<ItemSupplierForRateContractDto>` | - | - | - |
| GET | `/inventoryStockList/getItemsByItemCategoryWithoutRateContract` | `custom.InventoryServiceImpl.getItemsByItemCategoryWithoutRateContract` | Q  | `List<ItemSupplierForRateContractDto>` | - | - | - |
| GET | `/inventoryStockList/getItemsEnquiryDetails` | `custom.InventoryServiceImpl.getItemsEnquiryDetails` | Q Q  | `List<GetItemsEnquiryDetailsResultDto_1>` | Y | - | - |
| GET | `/inventoryStockList/getItemsEnquiryDetailsAndSuppliers` | `custom.InventoryServiceImpl.getEnquiryDetailsViewItemsEnquiryDetailsAndSuppliers` | Q  | `GetEnquiryDetailsView` | - | - | - |
| GET | `/inventoryStockList/getItemsEnquirySuppliers` | `custom.InventoryServiceImpl.getItemsEnquirySuppliers` | Q Q  | `List<EnquirySuppliersResultDto>` | Y | - | - |
| GET | `/inventoryStockList/getItemSingleAccessionNum` | `custom.InventoryServiceImpl.getItemSingleAccessionNum` | Q Q  | `boolean` | - | - | - |
| GET | `/inventoryStockList/getItemWisePurchaseRequestList` | `custom.InventoryServiceImpl.getItemWisePurchaseRequestList` | Q  | `List<HospitalWisePurchaseRequestDto>` | - | - | - |
| GET | `/inventoryStockList/getModalForAsset` | `custom.InventoryServiceImpl.getModalForAsset` | Q Q Q  | `List<String>` | Y | - | - |
| GET | `/inventoryStockList/getModalForAssetIssueIndent` | `custom.InventoryServiceImpl.getModalForAssetIssueIndent` | Q Q Q  | `Map<Long,List<String>>` | - | - | - |
| GET | `/inventoryStockList/getNAIssueItemsDetailsByIdForRecive` | `custom.InventoryServiceImpl.getNAIssueItemsDetailsByIdForRecive` | Q Q  | `IssueItemDetailsView` | Y | - | - |
| GET | `/inventoryStockList/getPoNumberforGRN` | `custom.InventoryServiceImpl.getPoNumberforGRN` | Q Q Q  | `List<String>` | - | - | - |
| GET | `/inventoryStockList/getPoNumberForPoScanGrn` | `custom.InventoryServiceImpl.getPoNumberForPoScanGrn` | Q Q Q  | `PurchaseOrderDto` | - | - | - |
| GET | `/inventoryStockList/getPOTermsAndCondionsBySupplierID` | `custom.InventoryServiceImpl.getPOTermsAndCondionsBySupplierID` | Q  | `List<Termandcondition>` | Y | - | - |
| GET | `/inventoryStockList/getPurchaseIndentItemsDetails` | `custom.InventoryServiceImpl.getPurchaseIndentItemsDetails` | Q  | `List<CurrentPurchaseOrderIndentDetailsResultDto>` | - | - | - |
| GET | `/inventoryStockList/getPurchaseRequestDetailsById` | `custom.InventoryServiceImpl.getPurchaseRequestDetailsById` | Q  | `PurchaseRequest` | - | - | - |
| GET | `/inventoryStockList/getQuotationList` | `custom.InventoryServiceImpl.getAvailableQuotationList` | Q Q  | `List<GetItemsEnquiryDetailsResultDto>` | Y | - | - |
| GET | `/inventoryStockList/getRateContractByContractId` | `custom.InventoryServiceImpl.getRateContractByContract` | Q Q  | `RateContract` | Y | - | - |
| GET | `/inventoryStockList/getRateContractDetailsByContractId` | `custom.InventoryServiceImpl.getRateContractByContractId` | Q Q  | `List<RateContractDetails>` | Y | - | - |
| GET | `/inventoryStockList/getRateContractList` | `custom.InventoryServiceImpl.getRateContractList` | Q  | `List<RateContract>` | - | - | - |
| GET | `/inventoryStockList/getSelectedItemCategories` | `custom.InventoryServiceImpl.getSelectedItemCategories` | Q  | `List<Long>` | Y | - | - |
| GET | `/inventoryStockList/getStateIdByDistrict` | `custom.InventoryServiceImpl.getStateIdByDistrict` | Q  | `Long` | - | - | - |
| GET | `/inventoryStockList/getStockAdjustmentDetailsPrint` | `custom.InventoryServiceImpl.getStockAdjustmentDetailsPrint` | Q  | `StockAdjustmentDetailsPrintView` | - | - | - |
| GET | `/inventoryStockList/getStoreCategoryIdByStoreId` | `custom.InventoryServiceImpl.getStoreCategoryIdByStoreId` | Q  | `List<StoreListByCategoryResultDto>` | Y | - | - |
| GET | `/inventoryStockList/getStoreIdByEashadiStoreId` | `custom.InventoryServiceImpl.getStoreIdByEashadiStoreId` | Q  | `Long` | Y | - | - |
| GET | `/inventoryStockList/getStoreItemCategoryMappingDetails` | `custom.InventoryServiceImpl.getStoreItemCategoryMappingDetails` | Q  | `List<StoreItemCategoryDetailsDto>` | - | - | - |
| GET | `/inventoryStockList/getStoreMasterById/{id}` | `custom.InventoryServiceImpl.getStoreMasterById` | P  | `Store` | Y | - | - |
| GET | `/inventoryStockList/getSupplierItemCategoriesBySupplier` | `custom.InventoryServiceImpl.getSupplierItemCategoriesBySupplier` | Q Q  | `List<Itemcategory>` | Y | - | - |
| GET | `/inventoryStockList/getSupplierListForRateContract` | `custom.InventoryServiceImpl.getSupplierListForRateContract` | - | `List<Supplier>` | Y | - | - |
| GET | `/inventoryStockList/getSuppliersByItemId` | `custom.InventoryServiceImpl.getSupplierListByItemId` | Q  | `List<GetSupplierListByItemIdResultDto>` | Y | - | - |
| GET | `/inventoryStockList/getUnitMappedCentralStores` | `custom.InventoryServiceImpl.getUnitMappedCentralStores` | - | `List<Store>` | - | - | - |
| GET | `/inventoryStockList/getUnitStoreListNewIndent` | `custom.InventoryServiceImpl.getUnitStoreListNewIndent` | Q  | `List<UnitStoreListResultDto>` | - | - | - |
| GET | `/inventoryStockList/getWholePlanMaster` | `custom.InventoryServiceImpl.getwholePlanMasterId` | Q Q  | `Planmaster` | - | - | - |
| GET | `/inventoryStockList/getZonebyUnitId` | `custom.InventoryServiceImpl.getZonebyUnitId` | Q  | `Long` | Y | - | - |
| GET | `/inventoryStockList/printBarCodeGeneric` | `custom.InventoryServiceImpl.printBarCodeGeneric` | Q Q Q Q  | `String[]` | - | - | - |
| GET | `/inventoryStockList/printItemEnquiryDetails` | `custom.InventoryServiceImpl.printItemEnquiryDetails` | Q Q  | `GetEnquiryDetailsView` | - | - | - |
| GET | `/inventoryStockList/startEAushadhiItemUpdateJob` | `custom.InventoryServiceImpl.startEAushadhiItemUpdateJob` | - | `void` | - | - | - |
| GET | `/inventoryStockList/storeCategoryMapping` | `custom.InventoryServiceImpl.storeCategoryMapping` | Q Q  | `String` | - | - | - |
| GET | `/inventoryStockList/updateOpRequestFlag` | `custom.InventoryServiceImpl.updateOpRequestFlag` | Q Q  | `Boolean` | - | - | - |
| GET | `/inventoryStockListgetbatchNoSearch` | `custom.InventoryServiceImpl.getItembatchmaster` | Q Q  | `List<Itembatchmaster>` | - | - | - |
| GET | `/inventoryStockListgetEAushadhiIndentIssueDetails` | `custom.InventoryServiceImpl.getEAushadhiIndentIssueDetails` | Q Q  | `IndentIssueEaushadi` | Y | - | - |
| GET | `/inventoryStockListgetUploadedMaterialConsDocuments` | `custom.InventoryServiceImpl.getUploadedMaterialConsDocuments` | Q  | `PatientIDDocumentDto` | - | - | - |
| GET | `/inventoryStockListgetUploadedStckAdjDocuments` | `custom.InventoryServiceImpl.getUploadedStckAdjDocuments` | Q  | `PatientIDDocumentDto` | Y | - | - |
| POST | `/inventoryStockList/addAddPOIndent` | `custom.InventoryServiceImpl.addAddPOIndent` | JSON  | `PurchaseOrderView` | - | - | - |
| POST | `/inventoryStockList/addAddPOOrder` | `custom.InventoryServiceImpl.addPurchaseOrder` | JSON  | `PurchaseOrderView` | Y | - | - |
| POST | `/inventoryStockList/addAddPOOrderIndent` | `custom.InventoryServiceImpl.addAddPOOrderIndent` | JSON  | `PurchaseOrderIndentView` | - | - | - |
| POST | `/inventoryStockList/addConsignmentGRN` | `custom.InventoryServiceImpl.addConsignmentGRN` | JSON  | `AddConsignmentGRNView` | Y | - | - |
| POST | `/inventoryStockList/addExternalCosumptionStock` | `custom.InventoryServiceImpl.addExternalCosumptionStock` | JSON  | `void` | - | - | - |
| POST | `/inventoryStockList/addGRN` | `custom.InventoryServiceImpl.addGRN` | JSON  | `AddGRNView` | Y | - | - |
| POST | `/inventoryStockList/addGRNReturn` | `custom.InventoryServiceImpl.addGRNReturn` | JSON  | `GrnReturnView` | Y | - | - |
| POST | `/inventoryStockList/addIndent` | `custom.InventoryServiceImpl.addIndent` | JSON Q Q  | `AddIndentView` | Y | - | - |
| POST | `/inventoryStockList/addIndentInvoiceDetails` | `custom.InventoryServiceImpl.addIndentInvoiceDetails` | JSON ?  | `IndentInvoice` | - | - | - |
| POST | `/inventoryStockList/addItemforConsumption` | `custom.InventoryServiceImpl.addItemforConsumption` | JSON  | `AddItemforConsumptionView` | - | - | - |
| POST | `/inventoryStockList/addItemGroup` | `custom.InventoryServiceImpl.addItemGroup` | JSON  | `void` | - | - | - |
| POST | `/inventoryStockList/addItemMaster` | `custom.InventoryServiceImpl.addItemMaster` | JSON  | `void` | Y | - | - |
| POST | `/inventoryStockList/addItemStockAdjustmentDetails` | `custom.InventoryServiceImpl.addItemStockAdjustmentDetails` | JSON  | `List<ItemStockAdjustmentDetailsDto>` | Y | - | - |
| POST | `/inventoryStockList/addItemSupplier` | `custom.InventoryServiceImpl.addItemSupplier` | JSON  | `void` | - | - | - |
| POST | `/inventoryStockList/addItemTax` | `custom.InventoryServiceImpl.addOrUpdateItemTax` | JSON Q  | `void` | Y | - | - |
| POST | `/inventoryStockList/addNewItemToRateContract` | `custom.InventoryServiceImpl.addNewItemToRateContract` | JSON  | `Long` | - | - | - |
| POST | `/inventoryStockList/addOpeningBalItems` | `custom.InventoryServiceImpl.addOpeningBalApi` | JSON Q  | `AddOpeningBalView` | - | - | - |
| POST | `/inventoryStockList/addPOOrder` | `custom.InventoryServiceImpl.getPOOrder` | JSON  | `List<GetItemResultDto>` | Y | - | - |
| POST | `/inventoryStockList/addProvisionalGRN` | `custom.InventoryServiceImpl.saveProvisionalGrn` | JSON  | `AddGRNView` | Y | - | - |
| POST | `/inventoryStockList/addRateContract` | `custom.InventoryServiceImpl.addRateContract` | JSON  | `Boolean` | - | - | - |
| POST | `/inventoryStockList/addReasonForStockAdjustment` | `custom.InventoryServiceImpl.addReasonForStockAdjustment` | JSON  | `Boolean` | Y | - | - |
| POST | `/inventoryStockList/addtemEnquiry` | `custom.InventoryServiceImpl.addtemEnquiry` | JSON  | `ItemEnquiryView` | - | - | - |
| POST | `/inventoryStockList/approvalForIndentIssue` | `custom.InventoryServiceImpl.approvalForIndentIssue` | JSON Q Q  | `void` | Y | - | - |
| POST | `/inventoryStockList/approveIndentLists` | `custom.InventoryServiceImpl.approveIndentLists` | JSON  | `void` | Y | - | - |
| POST | `/inventoryStockList/approveProvisionalGRN` | `custom.InventoryServiceImpl.approveProvisionalGRN` | JSON  | `AddGRNView` | Y | - | - |
| POST | `/inventoryStockList/approveProvisionalGRNNew` | `custom.InventoryServiceImpl.approveProvisionalGRNNew` | JSON  | `UpdateGRNDto` | Y | - | - |
| POST | `/inventoryStockList/cancelReturnIndent` | `custom.InventoryServiceImpl.cancelReturnIndent` | Q  | `Boolean` | Y | - | - |
| POST | `/inventoryStockList/completeOpeningBalItems` | `custom.InventoryServiceImpl.completeOpeningBal` | JSON  | `AddOpeningBalView` | Y | - | - |
| POST | `/inventoryStockList/convertConsignedGRNToGRN` | `custom.InventoryServiceImpl.convertConsignedGRNToGRN` | JSON  | `AddConsignmentGRNView` | - | - | - |
| POST | `/inventoryStockList/freezePurchaseOrder` | `custom.InventoryServiceImpl.freezPurchaseOrder` | JSON  | `FreezPurchaseOrderDto` | - | - | - |
| POST | `/inventoryStockList/freezIndentLists` | `custom.InventoryServiceImpl.freezIndentLists` | JSON  | `void` | Y | - | - |
| POST | `/inventoryStockList/getAccesionByModalNoOrBrandId` | `custom.InventoryServiceImpl.getAccesionByModalNoOrBrandId` | JSON  | `List<Object[]>` | - | - | - |
| POST | `/inventoryStockList/getAllProductItemsByStoreIdAndGroupIdAndItemCategoryId` | `custom.InventoryServiceImpl.getAllProductItemsByStoreIdAndGroupIdAndItemCategoryId` | JSON Q  | `List<ItemClinicDetailsDto>` | - | - | - |
| POST | `/inventoryStockList/getAllProductItemsByStoreIdAndGroupIdFromSafetyAndReorderMst` | `custom.InventoryServiceImpl.getAllProductItemsByStoreIdAndGroupIdFromSafetyAndReorderMst` | JSON Q  | `List<ReorderAndSafetyMaster>` | Y | - | - |
| POST | `/inventoryStockList/getApprovGRNListForCount` | `custom.InventoryServiceImpl.getApprovGRNListForCount` | JSON  | `List<ApproveGrnListForCount>` | Y | - | - |
| POST | `/inventoryStockList/getApprovGRNListForSearch` | `custom.InventoryServiceImpl.getApproveGRNListForSearch` | JSON Q  | `List<GRNListForSearchResultDto>` | Y | - | - |
| POST | `/inventoryStockList/getCategoryCountOfStockList` | `custom.InventoryServiceImpl.getCategoryCountOfStockList` | JSON Q  | `List<StockListCategoryCountResultDto>` | Y | - | - |
| POST | `/inventoryStockList/getConsignedGRNList` | `custom.InventoryServiceImpl.getConsignedGRNList` | JSON  | `List<ConsignmentGRNResultDtoNew>` | - | - | - |
| POST | `/inventoryStockList/getConsignmentGRNDetails` | `custom.InventoryServiceImpl.getConsignmentGRNDetails` | JSON Q  | `List<ConsignmentGRNItemsListResultDto>` | - | - | - |
| POST | `/inventoryStockList/getCurrentStockForRecallBatchWise` | `custom.InventoryServiceImpl.getCurrentStockForRecallBatchWise` | JSON Q  | `String` | Y | - | - |
| POST | `/inventoryStockList/getCurrentUnitItemStockList` | `custom.InventoryServiceImpl.getItemCurrentUnitStockList` | JSON  | `List<ItemCurrentStockListResultNewDto>` | Y | - | - |
| POST | `/inventoryStockList/getDirectIssueListByStoreId` | `custom.InventoryServiceImpl.getDirectItemIssueList` | JSON  | `List<DirectItemIssueListResultDto>` | - | - | - |
| POST | `/inventoryStockList/getDirectItemIssueListByStoreId` | `custom.InventoryServiceImpl.getDirectItemIssueListByStoreIdforItemIssue` | JSON  | `List<IssueItemDetailsListResultDto>` | - | - | - |
| POST | `/inventoryStockList/getGRNForGrnNosforDirectGRN` | `custom.InventoryServiceImpl.getGRNForGrnNosforDirectGRN` | JSON Q  | `List<GRNResultRptforDirectGRNDto>` | - | - | - |
| POST | `/inventoryStockList/getGRNForGrnNoss` | `custom.InventoryServiceImpl.getGRNForGrnNo` | JSON  | `List<GRNResultRptforDirectGRNDto>` | - | - | - |
| POST | `/inventoryStockList/getGRNListForSearch` | `custom.InventoryServiceImpl.getGRNListForSearch` | JSON  | `List<GRNListForSearchResultDto>` | Y | - | - |
| POST | `/inventoryStockList/getGRNListForSearchByItemId` | `custom.InventoryServiceImpl.getGRNListForSearchByItemId` | JSON  | `List<GRNListForSearchResultDto>` | - | - | - |
| POST | `/inventoryStockList/getGRNListForSearchPre` | `custom.InventoryServiceImpl.getGRNListForSearchPre` | JSON  | `List<GRNListForSearchResultDto>` | - | - | - |
| POST | `/inventoryStockList/getIndentDetailsById` | `custom.InventoryServiceImpl.getIndentDetailsById` | JSON Q  | `List<IndentItemDetailView>` | Y | - | - |
| POST | `/inventoryStockList/getIndentDetailsforItemIssue` | `custom.InventoryServiceImpl.getIndentDetailsforItemIssue` | JSON Q  | `List<IndentItemDetailView>` | Y | - | - |
| POST | `/inventoryStockList/getIndentListByStoreId` | `custom.InventoryServiceImpl.getIndentListByStoreId` | JSON Q  | `List<IndentListByStoreIdResultDto>` | - | - | - |
| POST | `/inventoryStockList/getIndentListByStoreIdforItemIssue` | `custom.InventoryServiceImpl.getIndentListByStoreIdforItemIssue` | JSON Q  | `List<IndentListByStoreIdResultDto>` | - | - | - |
| POST | `/inventoryStockList/getIssuedPurchaseOrders` | `custom.InventoryServiceImpl.getIssuedPurchaseOrders` | JSON  | `List<IssuedPurchaseOrderResultDto>` | - | - | - |
| POST | `/inventoryStockList/getItemCurrentPurchaseOrderList` | `custom.InventoryServiceImpl.getItemCurrentPurchaseOrderList` | JSON  | `List<CurrentPurchaseOrderListResultDto>` | Y | - | - |
| POST | `/inventoryStockList/getItemCurrentPurchaseOrderListforOtherUnit` | `custom.InventoryServiceImpl.getItemCurrentPurchaseOrderListforOtherUnit` | JSON  | `List<CurrentPurchaseOrderListResultDto>` | - | - | - |
| POST | `/inventoryStockList/getItemCurrentReorderforDirectIssue` | `custom.InventoryServiceImpl.getItemCurrentReorderforDirectIssue` | JSON  | `List<ItemBatchListForSearchwithTaxResultDto>` | - | - | - |
| POST | `/inventoryStockList/getItemCurrentReorderListforIndentDetails` | `custom.InventoryServiceImpl.getItemCurrentReorderListforIndentDetails` | JSON  | `List<CurrentItemReoderforIndentResultDto>` | Y | - | - |
| POST | `/inventoryStockList/getItemCurrentStockList` | `custom.InventoryServiceImpl.getItemCurrentStockList` | JSON  | `List<ItemCurrentStockListResultNewDto>` | - | - | - |
| POST | `/inventoryStockList/getItemCurrentStockListByStore` | `custom.InventoryServiceImpl.getItemCurrentStockListByStore` | Q  | `List<GetCurrentStockListByStoreIdDto>` | - | - | - |
| POST | `/inventoryStockList/getItemCurrentStockListData` | `custom.InventoryServiceImpl.getItemCurrentStockListDat` | JSON Q  | `List<ItemStockView>` | Y | - | - |
| POST | `/inventoryStockList/getItemgroup` | `custom.InventoryServiceImpl.getItemGroup` | JSON  | `List<ItemGroupResultDto>` | - | - | - |
| POST | `/inventoryStockList/getItemInfoforReorderIndent` | `custom.InventoryServiceImpl.getItemInfoforReorderIndent` | JSON  | `Map<Long, Map<Long, Map<Long, List<ItemReorderListNewResultDto>>>>` | Y | - | - |
| POST | `/inventoryStockList/getItemMasterList` | `custom.InventoryServiceImpl.getItemMasterList` | JSON  | `List<ItemMasterSearchListResultDtoNew>` | - | - | - |
| POST | `/inventoryStockList/getItemMasterListFull` | `custom.InventoryServiceImpl.getItemMasterListFull` | JSON  | `List<ItemMasterSearchListFullResultDto>` | - | - | - |
| POST | `/inventoryStockList/getItemStockWithLocation` | `custom.InventoryServiceImpl.getItemStockWithLocation` | JSON Q  | `List<ItemCurrentStockListResultNewDto>` | Y | - | - |
| POST | `/inventoryStockList/getItemSupplierRateList` | `custom.InventoryServiceImpl.getItemSupplierRateList` | JSON Q  | `List<Supplier>` | Y | - | - |
| POST | `/inventoryStockList/getNotMappedProductItemsByGroupIdAndItemCategoryIdAndStoreId` | `custom.InventoryServiceImpl.getNotMappedProductItemsByGroupIdAndItemCategoryId` | JSON Q  | `List<ItemClinicDetailsDto>` | - | - | - |
| POST | `/inventoryStockList/getPatientGeneralDetailsForCustomOrder` | `custom.InventoryServiceImpl.getPatientGeneralDetailsForCustomOrder` | JSON  | `List<SearchPatientAndVisitByNameMobUpdatedResultDto>` | - | - | - |
| POST | `/inventoryStockList/getPoStatusCounts` | `custom.InventoryServiceImpl.getPoStatusCounts` | JSON  | `PurchaseOrderListView` | - | - | - |
| POST | `/inventoryStockList/getPOTermsAndConditions` | `custom.InventoryServiceImpl.getPOTermsAndConditions` | JSON  | `List<POTermsAndConditionsResultDtoNew>` | - | - | - |
| POST | `/inventoryStockList/getPurchaseOrderDetailforGrn` | `custom.InventoryServiceImpl.getPurchaseOrderDetails` | JSON Q  | `BillReceiptView` | Y | - | - |
| POST | `/inventoryStockList/getPurchaseOrderDetailforPoIssue` | `custom.InventoryServiceImpl.getPurchaseOrderDetailsForPoIssue` | JSON Q  | `BillReceiptView` | Y | - | - |
| POST | `/inventoryStockList/getPurchaseOrderIssueDetails` | `custom.InventoryServiceImpl.getPurchaseOrderIssueDetails` | JSON  | `POIssueSearchResultView` | Y | - | - |
| POST | `/inventoryStockList/getPurchaseOrderList` | `custom.InventoryServiceImpl.getPurchaseOrderList` | JSON Q Q  | `PurchaseOrderListView` | Y | - | - |
| POST | `/inventoryStockList/getPurchaseRequestList` | `custom.InventoryServiceImpl.getPurchaseRequestList` | JSON Q  | `List<PurchaseRequestDetailsDto>` | Y | - | - |
| POST | `/inventoryStockList/getReorderListforPI` | `custom.InventoryServiceImpl.getReorderListforPI` | Q  | `Map<Long, List<CurrentItemReoderResultDto>>` | Y | - | - |
| POST | `/inventoryStockList/getReorderListforPO` | `custom.InventoryServiceImpl.getReorderListforPO` | Q  | `List<CurrentItemReoderResultDto>` | - | - | - |
| POST | `/inventoryStockList/getReorderListforPODetails` | `custom.InventoryServiceImpl.getReorderListforPODetails` | JSON  | `List<CurrentItemReoderResultDto>` | Y | - | - |
| POST | `/inventoryStockList/getStockAdjustmentDetails` | `custom.InventoryServiceImpl.getStockAdjustmentDetails` | JSON Q  | `List<StockAdjustmentDetailsResultDto>` | Y | - | - |
| POST | `/inventoryStockList/getSupplierOrderList` | `custom.InventoryServiceImpl.getSupplierOrderList` | JSON  | `List<CurrentPurchaseOrderListResultDto>` | - | - | - |
| POST | `/inventoryStockList/getSuppliersByItemIdAndStore` | `custom.InventoryServiceImpl.getSuppliersByItemIdAndStore` | JSON  | `List<SupplierDetailsByItemIdDto>` | Y | - | - |
| POST | `/inventoryStockList/indentCompleted` | `custom.InventoryServiceImpl.completeIndentView` | JSON  | `IssueView` | - | - | - |
| POST | `/inventoryStockList/indentCompletedonAsset` | `custom.InventoryServiceImpl.completeIndentViewonAsset` | JSON  | `void` | Y | - | - |
| POST | `/inventoryStockList/invalidateItemApproval` | `custom.InventoryServiceImpl.invalidateItemApproval` | Q  | `void` | Y | - | - |
| POST | `/inventoryStockList/issueItemReceiveList` | `custom.InventoryServiceImpl.issueItemReceiveList` | JSON Q  | `List<GetDirectIssueItemDetailsResultDto>` | - | - | - |
| POST | `/inventoryStockList/issueItemWithoutIndent` | `custom.InventoryServiceImpl.issueItemWithoutIndentAPI` | JSON Q  | `IssueView` | Y | - | - |
| POST | `/inventoryStockList/issuepo` | `custom.InventoryServiceImpl.issuePO` | JSON  | `POIssueView` | - | - | - |
| POST | `/inventoryStockList/printDraftIndent` | `custom.InventoryServiceImpl.printDraftIndent` | JSON  | `IssueView` | Y | - | - |
| POST | `/inventoryStockList/receiveItemIssue` | `custom.InventoryServiceImpl.updateReceiveItemIssue` | JSON  | `IndentInvoice` | Y | - | - |
| POST | `/inventoryStockList/rejectAssetOnSecndLevel` | `custom.InventoryServiceImpl.rejectAssetOnSecndLevel` | Q Q  | `String` | Y | - | - |
| POST | `/inventoryStockList/rejectProvisionalGRN` | `custom.InventoryServiceImpl.rejectProvisionalGRN` | JSON Q  | `void` | Y | - | - |
| POST | `/inventoryStockList/releaseReturnEaushadiItem` | `custom.InventoryServiceImpl.releaseReturnEaushadiItem` | JSON  | `void` | Y | - | - |
| POST | `/inventoryStockList/renegotiatePurchaseOrder` | `custom.InventoryServiceImpl.updatePurchaseOrder` | JSON  | `UpdatePOView` | Y | - | - |
| POST | `/inventoryStockList/reTriggerEAushadhiIndent` | `custom.InventoryServiceImpl.reTriggerEAushadhiIndent` | JSON  | `void` | Y | - | - |
| POST | `/inventoryStockList/RunAutoIndentJob` | `custom.InventoryServiceImpl.RunAutoIndentJob` | Q  | `void` | - | - | - |
| POST | `/inventoryStockList/safetyStockStoreMapping` | `custom.InventoryServiceImpl.seftyStockStoreMapping` | JSON  | `StoreSafetyStockMappingDto` | Y | - | - |
| POST | `/inventoryStockList/saveAsseItemForClientUnit` | `custom.InventoryServiceImpl.saveAsseItemForClientUnit` | JSON  | `void` | - | - | - |
| POST | `/inventoryStockList/saveIndent` | `custom.InventoryServiceImpl.saveIndentDetails` | JSON  | `CompleteIndentView` | - | - | - |
| POST | `/inventoryStockList/saveItemMappingWithStore` | `custom.InventoryServiceImpl.saveItemMappingWithStore` | JSON  | `StoreItemMappingDto` | - | - | - |
| POST | `/inventoryStockList/savePurchaseRequest` | `custom.InventoryServiceImpl.savePurchaseRequest` | JSON  | `PurchaseRequestView` | - | - | - |
| POST | `/inventoryStockList/saveReorderAndSafetyMappingWithStore` | `custom.InventoryServiceImpl.saveReorderAndSafetyMappingWithStore` |  | `?` | - | - | - |
| POST | `/inventoryStockList/secondLevelApproveIndentLists` | `custom.InventoryServiceImpl.secondLevelApproveIndentLists` | JSON  | `void` | - | - | - |
| POST | `/inventoryStockList/setOpeningBalanceStock` | `custom.InventoryServiceImpl.setOpeningBalanceStock` | JSON  | `String` | Y | - | - |
| POST | `/inventoryStockList/showAllDirectItemIssuesList` | `custom.InventoryServiceImpl.showAllDirectItemIssuesList` | JSON Q  | `List<Object>` | - | - | - |
| POST | `/inventoryStockList/supplierPoList` | `custom.InventoryServiceImpl.getSupplierPOList` | JSON  | `List<SupplierPOView>` | - | - | - |
| POST | `/inventoryStockList/syncJantaItemStock` | `custom.InventoryServiceImpl.syncJantaItemStock` | JSON  | `List<Long>` | - | - | - |
| POST | `/inventoryStockList/updateBatchExpiry` | `custom.InventoryServiceImpl.updateBatchExpiry` | JSON  | `void` | Y | - | - |
| POST | `/inventoryStockList/updateGRN` | `custom.InventoryServiceImpl.upidateGRN` | JSON  | `void` | Y | - | - |
| POST | `/inventoryStockList/updateGRNNew` | `custom.InventoryServiceImpl.upidateGRNNew` | JSON  | `void` | - | - | - |
| POST | `/inventoryStockList/updateItemClinicMapped` | `custom.InventoryServiceImpl.updateItemClinicMapped` | JSON  | `ItemClinic` | - | - | - |
| POST | `/inventoryStockList/updateItemForIsFreezedIndentList` | `custom.InventoryServiceImpl.updateItemForIsFreezedIndentList` | JSON  | `void` | Y | - | - |
| POST | `/inventoryStockList/updateOpeningBalItems` | `custom.InventoryServiceImpl.updateOpeningBal` | JSON  | `AddOpeningBalView` | Y | - | - |
| POST | `/inventoryStockList/updateRateContract` | `custom.InventoryServiceImpl.updateRateContract` | JSON  | `Long` | - | - | - |
| POST | `/inventoryStockList/updateRateContractExpiry` | `custom.InventoryServiceImpl.updateRateContractExpiry` | JSON  | `Long` | Y | - | - |
| POST | `/inventoryStockList/updateRcFreezeStatus` | `custom.InventoryServiceImpl.updateRcFreezeStatus` | Q Q  | `void` | - | - | - |
| POST | `/inventoryStockList/updateRCRequestStatus` | `custom.InventoryServiceImpl.updateRCRequestStatus` | Q Q  | `Boolean` | - | - | - |
| POST | `/inventoryStockListgetUpdatedQCStatus` | `custom.InventoryServiceImpl.getUpdatedQCStatus` | JSON  | `List<Itembatchmaster>` | Y | - | - |
| POST | `/inventoryStockListupdateBatchStatus` | `custom.InventoryServiceImpl.getItembatchmaster` | JSON  | `Boolean` | - | - | - |
| PUT | `/inventoryStockList/activeItemGroup` | `custom.InventoryServiceImpl.activeItemGroup` | Q  | `void` | Y | - | - |
| PUT | `/inventoryStockList/activeItemMaster` | `custom.InventoryServiceImpl.activeItemMaster` | Q  | `void` | Y | - | - |
| PUT | `/inventoryStockList/approveIndent` | `custom.InventoryServiceImpl.approveIndent` | Q  | `void` | Y | - | - |
| PUT | `/inventoryStockList/approveItemReturn` | `custom.InventoryServiceImpl.approveItemReturn` | Q Q  | `void` | Y | - | - |
| PUT | `/inventoryStockList/approvePo` | `custom.InventoryServiceImpl.approvePO` | Q Q Q Q  | `Object[]` | Y | - | - |
| PUT | `/inventoryStockList/cancelIndent` | `custom.InventoryServiceImpl.cancelIndent` | Q  | `void` | Y | - | - |
| PUT | `/inventoryStockList/cancelIssue` | `custom.InventoryServiceImpl.cancelIssue` | Q Q  | `void` | Y | - | - |
| PUT | `/inventoryStockList/closeIndent` | `custom.InventoryServiceImpl.closeIndent` | Q Q Q  | `void` | Y | - | - |
| PUT | `/inventoryStockList/deletePlanDetailsById` | `custom.InventoryServiceImpl.deletePlanDetailsById` | Q Q Q  | `void` | Y | - | - |
| PUT | `/inventoryStockList/deleteRateContract` | `custom.InventoryServiceImpl.deleteRateContract` | Q Q  | `void` | Y | - | - |
| PUT | `/inventoryStockList/freezeIndentLists` | `custom.InventoryServiceImpl.freezeIndentList` | Q Q Q Q  | `void` | Y | - | - |
| PUT | `/inventoryStockList/freezeItemReturn` | `custom.InventoryServiceImpl.freezeItemReturn` | Q  | `void` | - | - | - |
| PUT | `/inventoryStockList/holdIndentRequest` | `custom.InventoryServiceImpl.holdIndentRequest` | Q  | `void` | - | - | - |
| PUT | `/inventoryStockList/inactiveItemGroup` | `custom.InventoryServiceImpl.inactiveItemGroup` | Q  | `void` | Y | - | - |
| PUT | `/inventoryStockList/inactiveItemMaster` | `custom.InventoryServiceImpl.inactiveItemMaster` | Q  | `void` | Y | - | - |
| PUT | `/inventoryStockList/markIndentClearanceForIssue` | `custom.InventoryServiceImpl.markIndentClearance` | Q  | `void` | Y | - | - |
| PUT | `/inventoryStockList/removeSafetyStockMapping` | `custom.InventoryServiceImpl.removeSafetyStockMapping` | Q  | `String` | Y | - | - |
| PUT | `/inventoryStockList/removeStoreItemCategory` | `custom.InventoryServiceImpl.removeSelectedEquipmentDetails` | Q  | `String` | - | - | - |
| PUT | `/inventoryStockList/upadateItemMaster` | `custom.InventoryServiceImpl.upadateItemMaster` | JSON  | `ItemMasterDto` | Y | - | - |
| PUT | `/inventoryStockList/updateExpDatePo` | `custom.InventoryServiceImpl.updateExpDatePo` | Q Q Q  | `void` | - | - | - |
| PUT | `/inventoryStockList/updateInvoice` | `custom.InventoryServiceImpl.upidateInvoice` | Q Q Q Q  | `void` | Y | - | - |
| PUT | `/inventoryStockList/updateItemIssue` | `custom.InventoryServiceImpl.updateItemIssue` | Q Q  | `void` | Y | - | - |
| PUT | `/inventoryStockList/updateItemLocation` | `custom.InventoryServiceImpl.updateItemLocation` | Q Q Q Q  | `Boolean` | Y | - | - |
| PUT | `/inventoryStockList/updatePurchaseRequestStatus` | `custom.InventoryServiceImpl.getPurchaseRequestDetailsById` | Q Q  | `PurchaseRequest` | Y | - | - |
| PUT | `/inventoryStockList/updateReorderCancelStatus` | `custom.InventoryServiceImpl.updateReorderCancelStatus` | Q Q Q  | `void` | - | - | - |
| POST | `/investigation/` | `custom.InvestigationServiceImpl.registerPatient` | JSON  | `PatientAppointmentandRegDto` | - | - | - |
| POST | `/investigation/cancelCrossServiceOrders` | `custom.InvestigationServiceImpl.cancelCrossServiceOrders` | Q Q  | `Boolean` | Y | - | - |
| POST | `/investigation/createCrossServiceVisit` | `custom.InvestigationServiceImpl.createCrossServiceVisit` | JSON  | `CrossServiceOrderDto` | Y | - | - |
| POST | `/machineConfig/` | `custom.MachineConfigServiceImpl.addMachineConfig` | JSON  | `MachineConfiguration` | - | - | - |
| POST | `/machineConfig/exeDisable` | `custom.MachineConfigServiceImpl.addExeDisableMachineConfig` | - | `MachineConfiguration` | Y | - | - |
| POST | `/machineConfig/list` | `custom.MachineConfigServiceImpl.getMachineConfigList` | JSON  | `List<MachineConfiguration>` | Y | - | - |
| POST | `/markandaddvisit/campRegistration` | `custom.MarkAndAddVisitServiceImpl.campRegistration` | JSON  | `PatientAppointmentandRegDto` | Y | - | - |
| POST | `/markandaddvisit/markappandaddvisit` | `custom.MarkAndAddVisitServiceImpl.markAppAndAddVisit` | JSON  | `AddAndMarkVisitDto` | Y | - | - |
| POST | `/markandaddvisit/markappandaddvisitminimal` | `custom.MarkAndAddVisitServiceImpl.markAppAndAddVisitMinimal` | JSON  | `AddAndMarkVisitDto` | Y | - | - |
| POST | `/markandaddvisit/markCheckoutWithoutServices` | `custom.MarkAndAddVisitServiceImpl.markCheckoutWithoutServices` | JSON  | `AddAndMarkVisitDto` | Y | - | - |
| DELETE | `/master/3bc68347-1c94-4d2b-9548-71259465a8fe-dbl` | `custom.MasterCachedServiceImpl.uselessMethod7` | ?  | `void` | - | - | - |
| GET | `/master/3bc68347-1c94-4d2b-9548-71259465a8fe-c` | `custom.MasterCachedServiceImpl.uselessMethod5` | ?  | `void` | - | - | - |
| GET | `/master/addUpdateDepartmentUnit` | `custom.MasterCachedServiceImpl.addUpdateUnitDepartmentDetails` | Q Q  | `void` | - | - | - |
| GET | `/master/addVitalMasterDatafromJson` | `custom.MasterCachedServiceImpl.addVitalMasterDatafromJson` | - | `void` | Y | - | - |
| GET | `/master/beat` | `custom.MasterCachedServiceImpl.beat` | - | `Integer` | - | - | - |
| GET | `/master/bi-beat` | `custom.MasterCachedServiceImpl.biBeat` | HDR  | `Integer` | - | - | - |
| GET | `/master/buildingDepartmentDetails` | `custom.MasterCachedServiceImpl.getBuildingDepartmentDetailsNew` | Q Q  | `List<Buildingdepartmentdetails>` | - | - | - |
| GET | `/master/buildingDepartmentDetailsRosterWise` | `custom.MasterCachedServiceImpl.getBuildingDepartmentDetailsRosterWise` | Q Q  | `Map<String, Object>` | - | - | - |
| GET | `/master/checkCradleWardExists` | `custom.MasterCachedServiceImpl.checkCradleWardExists` | Q  | `boolean` | - | - | - |
| GET | `/master/checkEAushadhiIdIsMapped` | `custom.MasterCachedServiceImpl.checkEAushadhiIdIsMapped` | - | `Boolean` | - | - | - |
| GET | `/master/checkEAushadhiMainStoreIsCreated` | `custom.MasterCachedServiceImpl.checkEAushadhiMainStoreIsCreated` | Q  | `Boolean` | - | - | - |
| GET | `/master/checkRecordExists` | `custom.MasterCachedServiceImpl.checkRecordExists` | Q Q  | `Boolean` | - | - | - |
| GET | `/master/checkRecordExistsNumeric` | `custom.MasterCachedServiceImpl.checkRecordExistsNumeric` | Q Q  | `Boolean` | - | - | - |
| GET | `/master/checkRecordExistsuser` | `custom.MasterCachedServiceImpl.checkRecordExistsuser` | Q Q  | `Boolean` | - | - | - |
| GET | `/master/classmasterFilter` | `custom.MasterCachedServiceImpl.getClasessByUnitId` | - | `List<Classmaster>` | - | - | - |
| GET | `/master/coronaHelpline` | `custom.MasterCachedServiceImpl.getCoronaHelpline` | Q Q  | `List<Object[]>` | Y | - | - |
| GET | `/master/coronaSuspectedSurvey` | `custom.MasterCachedServiceImpl.coronaSuspectedSurvey` | Q Q  | `Map<String, String>` | Y | - | - |
| GET | `/master/csvSchedule` | `custom.MasterCachedServiceImpl.migrateoraclescripts` | - | `void` | - | - | - |
| GET | `/master/departmentFilter` | `custom.MasterCachedServiceImpl.getDepartmentsByUnitApi` | Q Q  | `List<Departmentmaster>` | - | - | - |
| GET | `/master/departmentMaster/{id}` | `custom.MasterCachedServiceImpl.getDepartmentDetailsById` | P  | `Departmentmaster` | - | - | - |
| GET | `/master/displayUnitMaster/{id}` | `custom.MasterCachedServiceImpl.getDisplayUnitMasterById` | P  | `DisplayUnitMaster` | - | - | - |
| GET | `/master/doctorBuildingDetails` | `custom.MasterCachedServiceImpl.getDoctorBuildingDetails` | Q Q Q  | `List<Doctordepartmentdetails>` | - | - | - |
| GET | `/master/doctorDepartmentdetails` | `custom.MasterCachedServiceImpl.getDoctorDeptDetails` | Q Q  | `List<Doctordepartmentdetails>` | - | - | - |
| GET | `/master/doctorFilterFilter` | `custom.MasterCachedServiceImpl.getAllDoctorMastersByUnitIdDept` | - | `List<DoctorMasterDetailsListResultDto_1_New>` | - | - | - |
| GET | `/master/downloadHelpDocFile` | `custom.MasterCachedServiceImpl.downloadHelpDocFile` | Q ?  | `void` | Y | - | - |
| GET | `/master/fetchofflinebuild` | `custom.MasterCachedServiceImpl.fetchOfflineBuild` | Q ?  | `void` | Y | - | - |
| GET | `/master/filterComplaints` | `custom.MasterCachedServiceImpl.filterComplaints` | Q  | `List<DiagnosisMaster>` | - | - | - |
| GET | `/master/filterComplaintsById` | `custom.MasterCachedServiceImpl.filterComplaintsById` | Q  | `DiagnosisMaster` | - | - | - |
| GET | `/master/filterDiagnosis` | `custom.MasterCachedServiceImpl.filterDiagnosis` | Q  | `List<DiagnosisMaster>` | - | - | - |
| GET | `/master/filterDiagnosisById` | `custom.MasterCachedServiceImpl.filterDiagnosisById` | Q  | `DiagnosisMaster` | - | - | - |
| GET | `/master/filterHealthfactor` | `custom.MasterCachedServiceImpl.filterHealthfactor` | Q Q Q  | `List<Healthfactormaster>` | - | - | - |
| GET | `/master/filterPharmDiagnosis` | `custom.MasterCachedServiceImpl.filterPharmDiagnosis` | Q  | `List<DiagnosisMaster>` | - | - | - |
| GET | `/master/getAdvanceAgainstMaster` | `custom.MasterCachedServiceImpl.getAdvanceAgainstMaster` | - | `List<Advanceagainst>` | - | - | - |
| GET | `/master/getAllAssociatedCompanyMasterByCompanyId/{company_id}` | `custom.MasterCachedServiceImpl.getAllAssociatedCompanyMasterByCompanyId` | P  | `List<Companyassociatemaster>` | - | - | - |
| GET | `/master/getAllBedStatusMaster` | `custom.MasterCachedServiceImpl.getAllBedStatusMaster` | - | `List<Bedstatusmaster>` | Y | - | - |
| GET | `/master/getAllCategory` | `custom.MasterCachedServiceImpl.getAllCategory` | - | `List<ItemCategoryResultDto>` | - | - | - |
| GET | `/master/getAllCities/{state_id}` | `custom.MasterCachedServiceImpl.getCitiesByStateId` | P  | `List<Citymaster>` | - | - | - |
| GET | `/master/getAllCompanyMaster` | `custom.MasterCachedServiceImpl.getAllCompanyMaster` | - | `List<Companymaster>` | - | - | - |
| GET | `/master/getAllCompanyMasterByCategoryId/{category_id}` | `custom.MasterCachedServiceImpl.getAllCompanyMasterByCategoryId` | P  | `List<Companymaster>` | - | - | - |
| GET | `/master/getAllComplaintsMaster` | `custom.MasterCachedServiceImpl.getAllComplaintMaster` | Q  | `List<DiagnosisMaster>` | - | - | - |
| GET | `/master/getAllConfigApp` | `custom.MasterCachedServiceImpl.getAllConfigApp` | Q  | `ConfigApplication` | - | - | - |
| GET | `/master/getAllConfigApp/{unit_Id}` | `custom.MasterCachedServiceImpl.getAllConfigappapi` | P  | `ConfigApplication` | - | - | - |
| GET | `/master/getAllCountries` | `custom.MasterCachedServiceImpl.getAllCountries` | - | `List<Countrymaster>` | - | - | - |
| GET | `/master/getAllDepartements/{unit_id}` | `custom.MasterCachedServiceImpl.getAllDepartmentsByUnit` | P  | `List<Departmentmaster>` | - | - | - |
| GET | `/master/getAllDepartementsForReport/{unit_id}` | `custom.MasterCachedServiceImpl.getAllDepartementsForReport` | P  | `List<Departmentmaster>` | - | - | - |
| GET | `/master/getAllDischargeTypes` | `custom.MasterCachedServiceImpl.getAllDischargeType` | - | `List<Dischargetype>` | - | - | - |
| GET | `/master/getAllDiseaseGroup` | `custom.MasterCachedServiceImpl.getAllDiseaseGroup` | - | `List<ModuleNameMaster>` | - | - | - |
| GET | `/master/getAllDiseaseSubGroup` | `custom.MasterCachedServiceImpl.getAllDiseaseSubGroup` | - | `List<ModuleNameMaster>` | - | - | - |
| GET | `/master/getAlldoctor` | `custom.MasterCachedServiceImpl.getAlldoctors` | - | `List<Doctormaster>` | - | - | - |
| GET | `/master/getAllDoctorMastersByUnitIdForReport` | `custom.MasterCachedServiceImpl.getAllDoctorMastersByUnitIdForReport` | Q  | `List<Doctormaster>` | - | - | - |
| GET | `/master/getAllGenderMaster` | `custom.MasterCachedServiceImpl.getAllGenderMaster` | - | `List<Gendermaster>` | - | - | - |
| GET | `/master/getAllGenderTypes` | `custom.MasterCachedServiceImpl.getGenderMaster` | - | `List<Gendermaster>` | - | - | - |
| GET | `/master/getAllItemMaster` | `custom.MasterCachedServiceImpl.getAllItemMaster` | - | `List<Itemmaster>` | - | - | - |
| GET | `/master/getAllMachine` | `custom.MasterCachedServiceImpl.getAllMachine` | - | `List<Machinemaster>` | - | - | - |
| GET | `/master/getAllMasterData` | `custom.MasterCachedServiceImpl.getAllMasterDataMap` | ? ?  | `void` | - | - | - |
| GET | `/master/getAllNonEmegencyVisitTypes` | `custom.MasterCachedServiceImpl.getAllNonEmegencyVisitTypes` | - | `List<Visittypemaster>` | - | - | - |
| GET | `/master/getAllNursesByUnitId` | `custom.MasterCachedServiceImpl.getAllNursesByUnitId` | Q  | `List<Doctormaster>` | - | - | - |
| GET | `/master/getAllNursesForTransport` | `custom.MasterCachedServiceImpl.getAllNursesForTransport` | Q  | `List<Doctormaster>` | - | - | - |
| GET | `/master/getAllPathoCategory` | `custom.MasterCachedServiceImpl.getAllPathoCategory` | - | `List<Pathocategory>` | - | - | - |
| GET | `/master/getAllPaymentMode` | `custom.MasterCachedServiceImpl.getMasterPaymentModeType` | - | `List<Paymentmodetype>` | - | - | - |
| GET | `/master/getAllPaymentModes` | `custom.MasterCachedServiceImpl.getAllPaymentModes` | - | `List<Paymentmodetype>` | - | - | - |
| GET | `/master/getAllPLanMasterByCompanyId/{company_id}` | `custom.MasterCachedServiceImpl.getAllPLanMasterByCompanyId` | P  | `List<Planmaster>` | - | - | - |
| GET | `/master/getAllPrefixes` | `custom.MasterCachedServiceImpl.getAllPrefixes` | - | `List<Prefixmaster>` | - | - | - |
| GET | `/master/getAllReferEntityTypes` | `custom.MasterCachedServiceImpl.getReferalEntityType` | - | `List<Refentitytypemaster>` | - | - | - |
| GET | `/master/getAllRelations` | `custom.MasterCachedServiceImpl.getAllRelations` | - | `List<Relationmaster>` | - | - | - |
| GET | `/master/getAllServicesForDiagnosisMapping` | `custom.MasterCachedServiceImpl.getAllServicesForDiagnosisMapping` | Q Q  | `List<Servicemaster>` | - | - | - |
| GET | `/master/getAllShiftPatterns` | `custom.MasterCachedServiceImpl.getAllPatternDetails` | - | `List<ShiftPatternMaster>` | - | - | - |
| GET | `/master/getAllStaffMastersByUnitIdForReport` | `custom.MasterCachedServiceImpl.getAllStaffMastersByUnitIdForReport` | Q  | `List<Staffmaster>` | - | - | - |
| GET | `/master/getAllStates/{country_id}` | `custom.MasterCachedServiceImpl.getStatesByCountryId` | P  | `List<Statemaster>` | - | - | - |
| GET | `/master/getAllSubSpecializationMasters` | `custom.MasterCachedServiceImpl.getAllSubSpecializationMasters` | - | `List<Subspecialization>` | - | - | - |
| GET | `/master/getAllTariffMasterByCompanyId/{company_id}` | `custom.MasterCachedServiceImpl.getAllTariffMasterByCompanyId` | P  | `List<Tariffmaster>` | - | - | - |
| GET | `/master/getAllTariffServicesByunitId` | `custom.MasterCachedServiceImpl.getAllTariffServicesByunitId` | Q  | `List<Tariffservicemaster>` | - | - | - |
| GET | `/master/getAllUnitMasters` | `custom.MasterCachedServiceImpl.getAllUnitMasters` | - | `List<UnitmasterLiteDTO>` | - | - | - |
| GET | `/master/getAllUnmappedPlans` | `custom.MasterCachedServiceImpl.getAllUnmappedPlans` | Q  | `List<Planmaster>` | - | - | - |
| GET | `/master/getAllUserRoles` | `custom.MasterCachedServiceImpl.getAllUersRole` | - | `List<TUserrolemaster>` | - | - | - |
| GET | `/master/getAllVisitTypeMaster` | `custom.MasterCachedServiceImpl.getAllVisitTypeMaster` | - | `List<Visittypemaster>` | - | - | - |
| GET | `/master/getAllVisitTypes` | `custom.MasterCachedServiceImpl.getVisitTypeMaster` | - | `List<Visittypemaster>` | - | - | - |
| GET | `/master/getAnswerByAnswerId` | `custom.MasterCachedServiceImpl.getAnswerByAnswerId` | ?  | `Answer` | - | - | - |
| GET | `/master/getAntibioticMachineMap` | `custom.MasterCachedServiceImpl.getAntibioticMachineMap` | Q  | `List<AntibioticMachineMap>` | - | - | - |
| GET | `/master/getApplicationUIErrorMessages` | `custom.MasterCachedServiceImpl.getApplicationUIErrorMessage` | - | `Map<String, String>` | - | - | - |
| GET | `/master/getBackGroundTasksList` | `custom.MasterCachedServiceImpl.getBackGroundTasksList` | Q Q  | `List<BackgroundTask>` | - | - | - |
| GET | `/master/getBankMaster` | `custom.MasterCachedServiceImpl.getAllBankMaster` | - | `List<Bankmaster>` | - | - | - |
| GET | `/master/getBlocks/{district_id}` | `custom.MasterCachedServiceImpl.getBlocksByDistrict` | P  | `List<Blockmaster>` | - | - | - |
| GET | `/master/getBuildingFloorStruc` | `custom.MasterCachedServiceImpl.getBuildingFloorStruc` | Q Q Q  | `Buildingfloorstructure` | - | - | - |
| GET | `/master/getBuildingMasterByDepartmentId` | `custom.MasterCachedServiceImpl.getBuildingMasterByDepartmentId` | Q Q Q  | `List<Buildingmaster>` | Y | - | - |
| GET | `/master/getBuildingMasterByUnitId` | `custom.MasterCachedServiceImpl.getBuildingMasterByUnitId` | Q  | `List<Buildingmaster>` | - | - | - |
| GET | `/master/getCabinListWithDepartment` | `custom.MasterCachedServiceImpl.getCabinListWithDepartment` | Q Q Q Q  | `List<Cabinmaster>` | - | - | - |
| GET | `/master/getCabinsByDepartment` | `custom.MasterCachedServiceImpl.getCabinsByDepartment` | Q  | `List<Cabinmaster>` | - | - | - |
| GET | `/master/getCabinsByWingAndDepartment` | `custom.MasterCachedServiceImpl.getCabinsByWingAndDepartment` | Q Q  | `List<Cabinmaster>` | - | - | - |
| GET | `/master/getCashCounters` | `custom.MasterCachedServiceImpl.getOpdStations` | Q  | `List<Cashcountermaster>` | - | - | - |
| GET | `/master/getCategory` | `custom.MasterCachedServiceImpl.getAllCategoryTypeMaster` | - | `List<Patientcategorymaster>` | - | - | - |
| GET | `/master/getClinicalMastersForPHC` | `custom.MasterCachedServiceImpl.getClinicalMastersForPHC` | HDR Q  | `ClinicalMastersForPHCDto` | Y | - | - |
| GET | `/master/getClinicReportData` | `custom.MasterCachedServiceImpl.getClinicReportData` | Q Q Q Q  | `List<ReportSummary>` | Y | - | - |
| GET | `/master/getCompanyAssociate` | `custom.MasterCachedServiceImpl.getAllCompanyAssociateMaster` | - | `List<Companyassociatemaster>` | - | - | - |
| GET | `/master/getCompanyTypeMaster` | `custom.MasterCachedServiceImpl.getAllCompanyTypeMaster` | - | `List<Companytypemaster>` | - | - | - |
| GET | `/master/getConfigApplication/{unit_id}` | `custom.MasterCachedServiceImpl.getConfigApplicationByUnitId` | P  | `ConfigApplication` | - | - | - |
| GET | `/master/getConfigurationListForUnit` | `custom.MasterCachedServiceImpl.getConfigurationListForUnit` | Q Q  | `List<Object>` | - | - | - |
| GET | `/master/getConfigurationStatus` | `custom.MasterCachedServiceImpl.getConfigurationStatus` | Q Q  | `ConfigStatusDto` | - | - | - |
| GET | `/master/getCoronoStats` | `custom.MasterCachedServiceImpl.getCoronoStats` | Q Q Q Q  | `List<ReportSummary>` | - | - | - |
| GET | `/master/getDeliveryParticularMaster` | `custom.MasterCachedServiceImpl.getDeliveryParticularMaster` | Q Q  | `List<DeliveryParticularsMaster>` | - | - | - |
| GET | `/master/getDeliveryParticularMonthlyData` | `custom.MasterCachedServiceImpl.getDeliveryParticularMonthlyData` | Q Q  | `List<DeliveryParticularsMonthly>` | - | - | - |
| GET | `/master/getDepartmentByDoctorId/{unitId}` | `custom.MasterCachedServiceImpl.getDepartmentByDoctorId` | P Q  | `List<Departmentmaster>` | - | - | - |
| GET | `/master/getDepartmentCabinsMapForRad` | `custom.MasterCachedServiceImpl.getCabinsByDepartmentId` | Q  | `Map<String, List<Cabinmaster>>` | - | - | - |
| GET | `/master/getDepartmentsByDistrictId` | `custom.MasterCachedServiceImpl.getDepartmentsByDistrictId` | Q  | `List<String>` | - | - | - |
| GET | `/master/getDepartmentsByUnitId` | `custom.MasterCachedServiceImpl.getDepartmentsByUnitId` | Q Q  | `List<Departmentmaster>` | - | - | - |
| GET | `/master/getDepartmentsByWing` | `custom.MasterCachedServiceImpl.getDepartemntsByWing` | Q  | `List<Departmentmaster>` | - | - | - |
| GET | `/master/getDiseaseGroupMasterById` | `custom.MasterCachedServiceImpl.getDiseaseGroupMasterById` | Q  | `String` | - | - | - |
| GET | `/master/getDistricts/{state_id}` | `custom.MasterCachedServiceImpl.getDistrictByState` | P  | `List<DistrictMaster>` | Y | - | - |
| GET | `/master/getDoctorFrequentServices` | `custom.MasterCachedServiceImpl.getDoctorFrequentServices` | Q  | `List<SpecializationServicesDTO>` | - | - | - |
| GET | `/master/getDoctorProfileList` | `custom.MasterCachedServiceImpl.getDoctorProfileList` | Q  | `List<DoctorProfile>` | - | - | - |
| GET | `/master/getDoctorsByBuildingId` | `custom.MasterCachedServiceImpl.getDoctorsByBuildingId` | Q Q Q  | `List<Doctormaster>` | - | - | - |
| GET | `/master/getDoctorsByDepartmentId` | `custom.MasterCachedServiceImpl.getDoctorsByDepartmentId` | Q Q  | `List<Doctormaster>` | - | - | - |
| GET | `/master/getDoctorsByUnitId` | `custom.MasterCachedServiceImpl.getDoctorsByUnitId` | Q Q  | `List<Doctormaster>` | - | - | - |
| GET | `/master/getDocumentType` | `custom.MasterCachedServiceImpl.getDocumentTypeList` | Q  | `List<String>` | Y | - | - |
| GET | `/master/getDriverNumberById` | `custom.MasterCachedServiceImpl.getDriverNumberById` | Q  | `List<DriverMaster>` | - | - | - |
| GET | `/master/getDrugAlertSummary` | `custom.MasterCachedServiceImpl.getDrugAlertstoreWiseCount` | Q Q  | `DrugAlertstoreWiseCountDto` | - | - | - |
| GET | `/master/getEraktkoshCompMap` | `custom.MasterCachedServiceImpl.getEraktkoshCompMap` | Q Q Q Q  | `List<Bloodcomponentmaster>` | Y | - | - |
| GET | `/master/getEventList` | `custom.MasterCachedServiceImpl.getEventList` | - | `List<Events>` | Y | - | - |
| GET | `/master/getEventSubmission` | `custom.MasterCachedServiceImpl.getEventSubmission` | Q  | `EventSubmission` | Y | - | - |
| GET | `/master/getFilesList` | `custom.MasterCachedServiceImpl.getFilesList` | ? Q Q  | `Set<String>` | Y | - | - |
| GET | `/master/getFileTiuxml` | `custom.MasterCachedServiceImpl.getFileFromMinio` | Q Q  | `void` | - | - | - |
| GET | `/master/getHealthfactorsById` | `custom.MasterCachedServiceImpl.getHealthfactorsByIdApi` | Q  | `List<Healthfactormaster>` | - | - | - |
| GET | `/master/getHolidayList` | `custom.MasterCachedServiceImpl.getHolidayList` | - | `List<HolidayMaster>` | Y | - | - |
| GET | `/master/getHolidays` | `custom.MasterCachedServiceImpl.getAllUnitHolidays` | Q  | `List<HolidayMaster>` | - | - | - |
| GET | `/master/getHospitalAdminConfig` | `custom.MasterCachedServiceImpl.getHospitalAdminConfig` | Q  | `Map<String, Object>` | - | - | - |
| GET | `/master/getHospitalCategoryMaster` | `custom.MasterCachedServiceImpl.getHospitalCategoryMaster` | - | `List<Hospitalcategorymaster>` | - | - | - |
| GET | `/master/getIcdcmByCategoryList` | `custom.MasterCachedServiceImpl.getIcdcmByCategoryList` | Q Q  | `List<IcdCmMaster>` | Y | - | - |
| GET | `/master/getItemForAsset` | `custom.MasterCachedServiceImpl.getItemForAsset` | - | `List<Itemmaster>` | - | - | - |
| GET | `/master/getItemListByStoreId` | `custom.MasterCachedServiceImpl.getItemListByStoreId` | Q Q  | `List<Itemmaster>` | - | - | - |
| GET | `/master/getItemsByItemGroup` | `custom.MasterCachedServiceImpl.getItemsByItemGroup` | Q  | `List<Itemmaster>` | - | - | - |
| GET | `/master/getItemsByStoreId` | `custom.MasterCachedServiceImpl.getItemsByStoreId` | Q  | `List<Itemmaster>` | - | - | - |
| GET | `/master/getJantaClinicReportData` | `custom.MasterCachedServiceImpl.getJantaClinicReportData` | Q Q Q  | `List<ReportSummary>` | Y | - | - |
| GET | `/master/getJantaClinicToken` | `custom.MasterCachedServiceImpl.getJantaClinicToken` | Q Q  | `String` | Y | - | - |
| GET | `/master/getMappedServices` | `custom.MasterCachedServiceImpl.getMappedServices` | Q Q Q  | `List<Servicemaster>` | Y | - | - |
| GET | `/master/getMastersForAppointment` | `custom.MasterCachedServiceImpl.getMastersForAppointmentMap` | Q Q  | `AllMasterAppointmentView` | - | - | - |
| GET | `/master/getMastersForOnlineAppointment` | `custom.MasterCachedServiceImpl.getMastersForOnlineAppointment` | - | `AllMasterAppointmentView` | - | - | - |
| GET | `/master/getModuleWiseMasters` | `custom.MasterCachedServiceImpl.getModuleWiseMasters` | Q ? ?  | `void` | - | - | - |
| GET | `/master/getNoteTileMapping` | `custom.MasterCachedServiceImpl.getNoteTileMapping` | ?  | `Map<String,List<Map<String,Object>>>` | - | - | - |
| GET | `/master/getOpdStations` | `custom.MasterCachedServiceImpl.getOpdStations` | - | `List<OpdStationMaster>` | - | - | - |
| GET | `/master/getOrganismMachineMap` | `custom.MasterCachedServiceImpl.getOrganismMachineMap` | Q  | `List<OrganismMachineMap>` | - | - | - |
| GET | `/master/getPackages` | `custom.MasterCachedServiceImpl.getPackages` | Q Q Q  | `List<Packagemaster>` | - | - | - |
| GET | `/master/getPathoCabinMasters` | `custom.MasterCachedServiceImpl.getPathoCabinMasters` | Q Q  | `List<Cabinmaster>` | - | - | - |
| GET | `/master/getPharmacyDispensingCount` | `custom.MasterCachedServiceImpl.getOpdIpdPharmacyDispensingCount` | Q  | `List<PharmacyDispensingCountDto>` | - | - | - |
| GET | `/master/getPharmacyModuleStat` | `custom.MasterCachedServiceImpl.getPharmacyModuleStatus` | Q  | `Boolean` | - | - | - |
| GET | `/master/getPlanByInsuranceId` | `custom.MasterCachedServiceImpl.getPlanByInsuranceId` | Q  | `List<Planmaster>` | - | - | - |
| GET | `/master/getPlanMaster` | `custom.MasterCachedServiceImpl.getAllPlanMaster` | - | `List<Planmaster>` | - | - | - |
| GET | `/master/getPlanNameById` | `custom.MasterCachedServiceImpl.getPlanNameById` | Q  | `String` | - | - | - |
| GET | `/master/getPlanUnitDetails` | `custom.MasterCachedServiceImpl.getPlanUnitDetails` | Q Q  | `List<Planmaster>` | Y | - | - |
| GET | `/master/getPoliceStationByDistrictId` | `custom.MasterCachedServiceImpl.getPolicestationDistrict` | Q  | `List<Policestationmaster>` | - | - | - |
| GET | `/master/getPoliceStationById` | `custom.MasterCachedServiceImpl.getPoliceStationById` | Q  | `Policestationmaster` | - | - | - |
| GET | `/master/getPoliceStationDistrict` | `custom.MasterCachedServiceImpl.getPolicestationDistrict` | - | `List<DistrictMaster>` | - | - | - |
| GET | `/master/getProcedureServicesByunitId` | `custom.MasterCachedServiceImpl.getProcedureServicesByunitId` | Q  | `List<Servicemaster>` | - | - | - |
| GET | `/master/getPropertyTypeConfig` | `custom.MasterCachedServiceImpl.getPropertyTypeConfig` | ?  | `void` | - | - | - |
| GET | `/master/getQuestionsByQuestionId` | `custom.MasterCachedServiceImpl.getQuestionsByQuestionId` | ?  | `Question` | - | - | - |
| GET | `/master/getRadCabinMasters` | `custom.MasterCachedServiceImpl.getRadCabinMasters` | Q Q  | `List<Cabinmaster>` | - | - | - |
| GET | `/master/getRoomDetailByDepartmentId` | `custom.MasterCachedServiceImpl.getRoomDetailByDepartmentId` | Q  | `List<DepartmentRoomMappingMaster>` | - | - | - |
| GET | `/master/getRoomsByDepartmentCabin` | `custom.MasterCachedServiceImpl.getRoomsByDepartmentCabin` | Q  | `List<DepartmentCabinRoomMappingMaster>` | - | - | - |
| GET | `/master/getServiceById/{id}` | `custom.MasterCachedServiceImpl.getServicebyId` | P  | `Servicemaster` | - | - | - |
| GET | `/master/getservices` | `custom.MasterCachedServiceImpl.getUnitServices` | Q  | `List<ServiceListForBillResultDto>` | - | - | - |
| GET | `/master/getStoreListByUnitId` | `custom.MasterCachedServiceImpl.getStoreListByUnitId` | Q  | `List<Store>` | - | - | - |
| GET | `/master/getSupportRoster` | `custom.MasterCachedServiceImpl.getSupportRoster` | Q  | `List<SupportRosterDto>` | - | - | - |
| GET | `/master/getTariffMaster` | `custom.MasterCachedServiceImpl.getAllTariffMaster` | - | `List<Tariffmaster>` | - | - | - |
| GET | `/master/getTestNameByLocationId` | `custom.MasterCachedServiceImpl.getTestNameByLocationId` | Q  | `List<Pathotestmaster>` | - | - | - |
| GET | `/master/getTiuxmlMappingFile` | `custom.MasterCachedServiceImpl.getTiuxmlMappingFile` | Q ?  | `Map<String,List<Map<String,Object>>>` | Y | - | - |
| GET | `/master/getUatTrainingEvent` | `custom.MasterCachedServiceImpl.getUatTrainingEvent` | - | `Events` | - | - | - |
| GET | `/master/getUnitHeadByBuildingId` | `custom.MasterCachedServiceImpl.getUnitHeadByBuildingId` | Q Q Q  | `List<Doctormaster>` | - | - | - |
| GET | `/master/getUnitListByDistrictId` | `custom.MasterCachedServiceImpl.getUnitListByDistrictId` | Q  | `List<Unitmaster>` | Y | - | - |
| GET | `/master/getUnitMaster/{id}` | `custom.MasterCachedServiceImpl.getUnitmaster` | P  | `Unitmaster` | Y | - | - |
| GET | `/master/getUnitsByDepartment` | `custom.MasterCachedServiceImpl.getUnitsByDepartment` | Q Q Q  | `List<Unitmaster>` | - | - | - |
| GET | `/master/getUnitsByUserForReport` | `custom.MasterCachedServiceImpl.getUnitsByUserForReport` | Q Q  | `List<Unitmaster>` | - | - | - |
| GET | `/master/getUnitWisecase` | `custom.MasterCachedServiceImpl.getUnitWisecase` | Q  | `List<CasetypeMaster>` | - | - | - |
| GET | `/master/getUnitWiseDriver` | `custom.MasterCachedServiceImpl.getUnitWiseDriver` | - | `List<DriverMaster>` | - | - | - |
| GET | `/master/getUnitWiseOPDIPDCount` | `custom.MasterCachedServiceImpl.getUnitWiseOPDIPDCount` | HDR  | `Map<String, Object>` | - | - | - |
| GET | `/master/getUnitWisePlans` | `custom.MasterCachedServiceImpl.getUnitWisePlans` | Q  | `List<Planmaster>` | - | - | - |
| GET | `/master/getUnitWiseVehicle` | `custom.MasterCachedServiceImpl.getUnitWiseVehicle` | - | `List<Ambulancemaster>` | - | - | - |
| GET | `/master/getVehicleDetails` | `custom.MasterCachedServiceImpl.getVehicleDetails` | Q Q  | `List<Ambulancemaster>` | - | - | - |
| GET | `/master/getWaiverDetailsByDepartmentId` | `custom.MasterCachedServiceImpl.getWaiverDetailsByDepartmentId` | Q  | `Doctorwaiver` | Y | - | - |
| GET | `/master/getWardDetailsByDepartmentIdForAdmissionBlock` | `custom.MasterCachedServiceImpl.getWardDetailsByDepartmentIdForAdmissionBlock` | Q  | `String` | Y | - | - |
| GET | `/master/getWardMaster` | `custom.MasterCachedServiceImpl.getAllWardMaster` | - | `List<Wardmaster>` | - | - | - |
| GET | `/master/getWardMasterByDepartmentId` | `custom.MasterCachedServiceImpl.getWardMasterByDepartmentId` | Q Q Q  | `List<Wardmaster>` | - | - | - |
| GET | `/master/getWardMasterByUnit` | `custom.MasterCachedServiceImpl.getWardMasterByUnit` | Q  | `List<Wardmaster>` | - | - | - |
| GET | `/master/getWings` | `custom.MasterCachedServiceImpl.getWings` | - | `List<Wingmaster>` | Y | - | - |
| GET | `/master/heartbeat` | `custom.MasterCachedServiceImpl.heartbeat` | - | `void` | Y | - | - |
| GET | `/master/ipUnitId` | `custom.MasterCachedServiceImpl.getIPUnitsByDepartmentId` | Q  | `Set<Long>` | - | - | - |
| GET | `/master/medicalEnquiry` | `custom.MasterCachedServiceImpl.getMedicalEnquiry` | Q  | `List<MedicalItem>` | - | - | - |
| GET | `/master/offlinebuildavailable` | `custom.MasterCachedServiceImpl.isOfflineBuildAvailable` | Q  | `Integer` | - | - | - |
| GET | `/master/publishUnPublishEvent` | `custom.MasterCachedServiceImpl.publishUnPublishEvent` | Q  | `void` | - | - | - |
| GET | `/master/scrollEvent` | `custom.MasterCachedServiceImpl.scrollEvent` | Q Q  | `void` | Y | - | - |
| GET | `/master/showSSOLoginPage` | `custom.MasterCachedServiceImpl.showSSOLoginPage` | - | `Boolean` | - | - | - |
| GET | `/master/supplierMasterList` | `custom.MasterCachedServiceImpl.supplierMasterList` | Q Q  | `List<SupplierMaster>` | Y | - | - |
| GET | `/master/syncItemsInJanta` | `custom.MasterCachedServiceImpl.syncItemsInJanta` | - | `List<ItemMstDto>` | - | - | - |
| GET | `/master/unlockLockedUserAccount` | `custom.MasterCachedServiceImpl.unlockLockedUserAccount` | Q  | `void` | Y | - | - |
| GET | `/master/updateABDMKeys` | `custom.MasterCachedServiceImpl.updateABDMKeys` | - | `void` | - | - | - |
| GET | `/master/updateDoctorInCache` | `custom.MasterCachedServiceImpl.updateDoctorInCache` | - | `void` | - | - | - |
| GET | `/master/updateEntityCache` | `custom.MasterCachedServiceImpl.checkService` | Q Q  | `void` | Y | - | - |
| GET | `/master/updateSupplierIdsByUnits` | `custom.MasterCachedServiceImpl.updateSupplierIdsByUnits` | - | `Boolean` | - | - | - |
| POST | `/master/3bc68347-1c94-4d2b-9548-71259465a8fe-a` | `custom.MasterCachedServiceImpl.uselessMethod8` | Q Q ?  | `void` | - | - | - |
| POST | `/master/3bc68347-1c94-4d2b-9548-71259465a8fe-dbl` | `custom.MasterCachedServiceImpl.uselessMethod6` | ?  | `void` | - | - | - |
| POST | `/master/3bc68347-1c94-4d2b-9548-71259465a8fe-j` | `custom.MasterCachedServiceImpl.uselessMethod4` | HDR  | `Integer` | Y | - | - |
| POST | `/master/3bc68347-1c94-4d2b-9548-71259465a8fe-m` | `custom.MasterCachedServiceImpl.uselessMethod9` | FORM  | `Integer` | - | - | - |
| POST | `/master/3bc68347-1c94-4d2b-9548-71259465a8fe-p` | `custom.MasterCachedServiceImpl.uselessMethod3` | Q ?  | `void` | - | - | - |
| POST | `/master/3bc68347-1c94-4d2b-9548-71259465a8fe-s` | `custom.MasterCachedServiceImpl.uselessMethod1` | FORM Q  | `void` | - | - | - |
| POST | `/master/3bc68347-1c94-4d2b-9548-71259465a8fe-u` | `custom.MasterCachedServiceImpl.uselessMethod2` | FORM  | `Integer` | Y | - | - |
| POST | `/master/addBloodDonationCampMaster` | `custom.MasterCachedServiceImpl.addBloodDonationCampMaster` | JSON Q  | `void` | Y | - | - |
| POST | `/master/addBuildingfloorstructure` | `custom.MasterCachedServiceImpl.addBuildingfloorstructure` | JSON  | `void` | - | - | - |
| POST | `/master/addCasetypeMaster` | `custom.MasterCachedServiceImpl.addCasetypeMaster` | JSON ?  | `CasetypeMaster` | Y | - | - |
| POST | `/master/addEvent` | `custom.MasterCachedServiceImpl.addEvents` | JSON  | `Events` | - | - | - |
| POST | `/master/addEventSubmission` | `custom.MasterCachedServiceImpl.addEventSubmission` | JSON  | `EventSubmission` | - | - | - |
| POST | `/master/addExtCss` | `custom.MasterCachedServiceImpl.addExtCss` | ?  | `void` | - | - | - |
| POST | `/master/addExtJs` | `custom.MasterCachedServiceImpl.addExtJs` | ?  | `void` | Y | - | - |
| POST | `/master/addFileToMinio` | `custom.MasterCachedServiceImpl.addFileToMinio` | ?  | `void` | - | - | - |
| POST | `/master/addHolidayMaster` | `custom.MasterCachedServiceImpl.addHolidayMaster` | JSON ?  | `HolidayMaster` | - | - | - |
| POST | `/master/addInsuranceCategoryMapping` | `custom.MasterCachedServiceImpl.addInsuranceCategoryMapping` | JSON  | `void` | - | - | - |
| POST | `/master/addNoteTileMapping` | `custom.MasterCachedServiceImpl.addNoteTileMapping` | ?  | `void` | Y | - | - |
| POST | `/master/addpathozonemaster` | `custom.MasterCachedServiceImpl.addPathoZoneMaster` | JSON  | `void` | - | - | - |
| POST | `/master/addPropertyTypeConfig` | `custom.MasterCachedServiceImpl.addPropertyTypeConfig` | ?  | `void` | Y | - | - |
| POST | `/master/addSaleMarkupMaster` | `custom.MasterCachedServiceImpl.addSaleMarkupMaster` | JSON  | `void` | - | - | - |
| POST | `/master/addServiceDiagnosis` | `custom.MasterCachedServiceImpl.addServiceDiagnosis` | JSON  | `void` | - | - | - |
| POST | `/master/addTiuxml` | `custom.MasterCachedServiceImpl.addTiuxml` | Q ?  | `String` | Y | - | - |
| POST | `/master/addTiuXmlMapping` | `custom.MasterCachedServiceImpl.addTiuXmlMapping` | Q ?  | `void` | - | - | - |
| POST | `/master/billSearchDetail` | `custom.MasterCachedServiceImpl.billSearchDetail` | JSON  | `void` | - | - | - |
| POST | `/master/convertHtmlToCsv` | `custom.MasterCachedServiceImpl.convertHtmlToCsv` | ?  | `ResponseEntity<String>` | - | - | - |
| POST | `/master/disableAutoDemoAuth` | `custom.MasterCachedServiceImpl.disableAutoDemoAuth` | Q  | `void` | - | - | - |
| POST | `/master/disableCreatePackageFromGlobal` | `custom.MasterCachedServiceImpl.disableCreatePackageFromGlobal` | Q  | `void` | - | - | - |
| POST | `/master/disablePatientIndexing` | `custom.MasterCachedServiceImpl.disablePatientIndexing` | Q  | `void` | - | - | - |
| POST | `/master/doctorwaiver` | `custom.MasterCachedServiceImpl.addDoctorwaiver` | JSON  | `void` | Y | - | - |
| POST | `/master/dummy/printb64pdf` | `custom.MasterCachedServiceImpl.printB64Pdf` | ? ?  | `javax.ws.rs.core.Response` | - | - | - |
| POST | `/master/dummy/scanToBase64` | `custom.MasterCachedServiceImpl.scanToBase64` | - | `javax.ws.rs.core.Response` | Y | - | - |
| POST | `/master/emergencyregtypemaster` | `custom.MasterCachedServiceImpl.emergencyregtypemaster` |  | `?` | - | - | - |
| POST | `/master/filterComplaintsByIds` | `custom.MasterCachedServiceImpl.filterComplaintsByIds` | ?  | `List<DiagnosisMaster>` | - | - | - |
| POST | `/master/filterDiagnosisByIds` | `custom.MasterCachedServiceImpl.filterDiagnosisByIds` | ?  | `List<DiagnosisMaster>` | - | - | - |
| POST | `/master/filterlabServiceListByIds` | `custom.MasterCachedServiceImpl.filterlabServiceListByIds` | ?  | `List<Servicemaster>` | Y | - | - |
| POST | `/master/getAllQNodesValue` | `custom.MasterCachedServiceImpl.getAllQNodesValue` | - | `Map<String, List<Object[]>>` | - | - | - |
| POST | `/master/getAmendmentDocuments` | `custom.MasterCachedServiceImpl.getAmendmentDocuments` | Q Q  | `List<PatientDocuments>` | Y | - | - |
| POST | `/master/getFileAsStream` | `custom.MasterCachedServiceImpl.getFileAsStreamNew` | ?  | `InputStream` | Y | - | - |
| POST | `/master/getFileFromMinio` | `custom.MasterCachedServiceImpl.getFileFromMinio` | ?  | `String` | Y | - | - |
| POST | `/master/getModuleDocument` | `custom.MasterCachedServiceImpl.getModuleDocuments` | Q Q Q  | `List<ApplicationDocuments>` | - | - | - |
| POST | `/master/getOutreachUserList` | `custom.MasterCachedServiceImpl.getOutreachUserList` | JSON  | `List<GetUserDetailsResultDto>` | - | - | - |
| POST | `/master/getQNodes` | `custom.MasterCachedServiceImpl.getQNodes` | - | `List<String>` | - | - | - |
| POST | `/master/getQNodesValue` | `custom.MasterCachedServiceImpl.getQNodesValue` | Q  | `Map<String, Long[]>` | - | - | - |
| POST | `/master/inactivateMappedComponentAndBagTypes` | `custom.MasterCachedServiceImpl.inactivateMappedComponentAndBagTypes` | Q Q  | `void` | - | - | - |
| POST | `/master/mapServiceUnitWiseForLMISIntegration` | `custom.MasterCachedServiceImpl.mapServiceUnitWiseForLMISIntegrationApi` | - | `Boolean` | - | - | - |
| POST | `/master/pullServiceForUnit` | `custom.MasterCachedServiceImpl.pullServiceForUnit` | Q Q  | `List<ServiceListForBillWithoutCopayConcessionFastResultDto>` | - | - | - |
| POST | `/master/saveComponentEraktkoshCode` | `custom.MasterCachedServiceImpl.saveComponentEraktkoshCode` | Q Q  | `Map<String, String>` | - | - | - |
| POST | `/master/saveHospitalAdminConfig` | `custom.MasterCachedServiceImpl.saveHospitalAdminConfig` | JSON ?  | `Boolean` | - | - | - |
| POST | `/master/saveUpdateDeliveryParticularMonthlyData` | `custom.MasterCachedServiceImpl.saveUpdateDeliveryParticularMonthlyData` | JSON  | `void` | - | - | - |
| POST | `/master/sendMOICDailyStatusSMS` | `custom.MasterCachedServiceImpl.sendMOICDailyStatusSMS` | Q  | `void` | - | - | - |
| POST | `/master/startItemSyncingToSolr` | `custom.MasterCachedServiceImpl.startItemSyncingToSolr` | - | `void` | - | - | - |
| POST | `/master/syncJantaClinicReportData` | `custom.MasterCachedServiceImpl.syncJantaClinicReportData` | Q JSON  | `boolean` | - | - | - |
| POST | `/master/syncJantaMedOrders` | `custom.MasterCachedServiceImpl.syncJantaMedOrders` | JSON  | `List<Long>` | - | - | - |
| POST | `/master/syncJantaPathOrders` | `custom.MasterCachedServiceImpl.syncJantaPathOrders` | JSON  | `List<Long>` | - | - | - |
| POST | `/master/syncJantaPathResult` | `custom.MasterCachedServiceImpl.syncJantaPathResult` | JSON ?  | `List<Long>` | Y | - | - |
| POST | `/master/updateForBlankPopup` | `custom.MasterCachedServiceImpl.updateForBlankPopup` | Q Q  | `Boolean` | - | - | - |
| POST | `/master/updateUnitMasterForEaushadhiMainStore` | `custom.MasterCachedServiceImpl.updateUnitMasterForEaushadhiMainStore` | - | `Boolean` | - | - | - |
| POST | `/master/uploadAmendmentAndPMRDocument` | `custom.MasterCachedServiceImpl.uploadAmendmentAndPMRDocument` | ? Q Q  | `void` | - | - | - |
| POST | `/master/uploadHelpDocFile` | `custom.MasterCachedServiceImpl.uploadHelpDocFile` | ?  | `Map<String, String>` | - | - | - |
| POST | `/master/uploadModuleDocument` | `custom.MasterCachedServiceImpl.uploadModuleDocumentFile` | ? Q Q  | `void` | - | - | - |
| PUT | `/master/addUpdateReferralUnitMappingList` | `custom.MasterCachedServiceImpl.addUpdateReferralUnitMappingList` | JSON  | `void` | - | - | - |
| PUT | `/master/deleteServiceByServiceId` | `custom.MasterCachedServiceImpl.deleteServiceByServiceId` | Q Q  | `Boolean` | - | - | - |
| PUT | `/master/offlinebuilddeploystatus` | `custom.MasterCachedServiceImpl.updateOfflineBuildDeployStatus` | Q Q  | `Integer` | - | - | - |
| PUT | `/master/updateIcdcmByCategoryList` | `custom.MasterCachedServiceImpl.updateIcdcmByCategoryList` | JSON  | `void` | - | - | - |
| PUT | `/master/updateInsuranceCategoryMapping/{id}` | `custom.MasterCachedServiceImpl.updateInsuranceCategoryMapping` | JSON P  | `void` | Y | - | - |
| PUT | `/master/updatePathoZoneMaster/{id}` | `custom.MasterCachedServiceImpl.updateDisplayUnitMaster` | JSON P  | `void` | - | - | - |
| PUT | `/master/updateUnitMaster/{id}` | `custom.MasterCachedServiceImpl.updateUnit` | P JSON  | `Unitmaster` | - | - | - |
| DELETE | `/mdm/removeStoreForSupplier` | `custom.MdmServiceImpl.deleteSupplierUserStore` | Q Q  | `boolean` | Y | - | - |
| GET | `/mdm/createCradleWardForUnit` | `custom.MdmServiceImpl.createCradleWardForUnit` | Q Q ?  | `List<Long>` | - | - | - |
| GET | `/mdm/deleteIpdRosterById` | `custom.MdmServiceImpl.deleteIpdRosterById` | Q  | `void` | - | - | - |
| GET | `/mdm/getAllDesignationByCadreId` | `custom.MdmServiceImpl.getAllDesignationByCadreIdApi` | Q  | `List<CadreDesignationMaster>` | Y | - | - |
| GET | `/mdm/getAllGrievanceSubCategoryByCategoryId` | `custom.MdmServiceImpl.getAllGrievanceSubCategoryByCategoryId` | Q  | `List<GrievanceSubCategoryMaster>` | Y | - | - |
| GET | `/mdm/getAllGrievanceTicketCount` | `custom.MdmServiceImpl.getAllGrievanceTicketCount` | - | `Map<String,Long>` | Y | - | - |
| GET | `/mdm/getDepartmentCabinRoomMappingMaster` | `custom.MdmServiceImpl.getDepartmentCabinRoomMappingMaster` | Q Q Q  | `List<DepartmentCabinRoomMappingMaster>` | Y | - | - |
| GET | `/mdm/getDepartmentRoomMappingMaster` | `custom.MdmServiceImpl.getDepartmentRoomMappingMaster` | Q Q Q  | `List<DepartmentRoomMappingMaster>` | - | - | - |
| GET | `/mdm/getDoctorIdByBillingDepartmentId` | `custom.MdmServiceImpl.getDoctorIdByBillingDepartmentId` | Q  | `String` | - | - | - |
| GET | `/mdm/getGrievanceSubCategoryDetailsById` | `custom.MdmServiceImpl.getGrievanceSubCategoryMasterDetails` | Q  | `String` | - | - | - |
| GET | `/mdm/getHelpdeskUsers` | `custom.MdmServiceImpl.getHelpdeskUsers` | - | `List<Staffmaster>` | - | - | - |
| GET | `/mdm/getIpdRosterById` | `custom.MdmServiceImpl.getDepartmentsAppointmentScheduleById` | Q  | `IpdRosterDetails` | - | - | - |
| GET | `/mdm/getPatientAllGrievanceTicketCount` | `custom.MdmServiceImpl.getPatientAllGrievanceTicketCount` | Q HDR Q  | `Map<String,Long>` | Y | - | - |
| GET | `/mdm/getPatientGrievanceTicketHistoryById` | `custom.MdmServiceImpl.getPatientGrievanceTicketHistory` | Q HDR  | `List<GrievanceTicketHistoryDto>` | - | - | - |
| GET | `/mdm/getRoomDetailById` | `custom.MdmServiceImpl.getRoomDetailByIdForQueue` | Q  | `Long` | - | - | - |
| GET | `/mdm/getRoomMasterForQueueList` | `custom.MdmServiceImpl.getRoomMasterForQueueList` | Q Q Q  | `List<RoomMasterForQueue>` | - | - | - |
| GET | `/mdm/getSSOIdListForSearchForGRN` | `custom.MdmServiceImpl.getSSOIdListForSearchForGRN` | Q  | `List<TUserdetails>` | - | - | - |
| GET | `/mdm/getTicketsForDashboard` | `custom.MdmServiceImpl.getTicketsForDashboard` | Q Q  | `Map<String,Object>` | Y | - | - |
| GET | `/mdm/saveRghsData` | `custom.MdmServiceImpl.saveSimsRghsData` | - | `void` | - | - | - |
| POST | `/mdm/addBedMaster` | `custom.MdmServiceImpl.addBedMaster` | HDR JSON ?  | `List<String>` | Y | - | - |
| POST | `/mdm/addDepartmentCabinRoomMappingMaster` | `custom.MdmServiceImpl.addDepartmentCabinRoomMappingMaster` | JSON  | `void` | Y | - | - |
| POST | `/mdm/addDepartmentMaster` | `custom.MdmServiceImpl.addDepartmentMaster` | JSON ?  | `List<Departmentmaster>` | Y | - | - |
| POST | `/mdm/addGrievanceDocument` | `custom.MdmServiceImpl.addGrievanceDocument` | Q Q  | `Boolean` | Y | - | - |
| POST | `/mdm/addGrievenceComment` | `custom.MdmServiceImpl.addGrievenceComment` | JSON HDR Q  | `Boolean` | Y | - | - |
| POST | `/mdm/addGrievenceFeedback` | `custom.MdmServiceImpl.addGrievenceFeedback` | JSON HDR  | `Boolean` | Y | - | - |
| POST | `/mdm/addGrievenceTicket` | `custom.MdmServiceImpl.addGrievenceTicket` | JSON HDR Q  | `Boolean` | - | - | - |
| POST | `/mdm/addIpdRosterDetails` | `custom.MdmServiceImpl.addIpdRosterDetails` | JSON  | `List<IpdRosterDetails>` | Y | - | - |
| POST | `/mdm/addservicemasterDetails` | `custom.MdmServiceImpl.addservicemasterDetails` | JSON ?  | `List<String>` | Y | - | - |
| POST | `/mdm/addUnitMaster` | `custom.MdmServiceImpl.addUnit` | JSON ?  | `Unitmaster` | - | - | - |
| POST | `/mdm/addWardMaster` | `custom.MdmServiceImpl.addWardMaster` | JSON ?  | `Wardmaster` | Y | - | - |
| POST | `/mdm/assignedTicketToUser` | `custom.MdmServiceImpl.assignedTicketToUser` | Q Q Q  | `Boolean` | - | - | - |
| POST | `/mdm/changeGrievanceSeverity` | `custom.MdmServiceImpl.changeGrievanceSeverity` | Q Q  | `Boolean` | Y | - | - |
| POST | `/mdm/changeGrievanceTicketStatus` | `custom.MdmServiceImpl.changeGrievanceTicketStatus` | Q Q Q  | `Boolean` | - | - | - |
| POST | `/mdm/changeServiceStatusById` | `custom.MdmServiceImpl.changeServiceStatusById` | Q  | `void` | - | - | - |
| POST | `/mdm/exposeBuckets` | `custom.MdmServiceImpl.exposeBuckets` | - | `Map<String, String>` | Y | - | - |
| POST | `/mdm/getAllGrievanceTickets` | `custom.MdmServiceImpl.getAllGrievanceTickets` | JSON Q  | `List<PatientGrievanceHistory>` | - | - | - |
| POST | `/mdm/getAllServicesBySpecId` | `custom.MdmServiceImpl.getAllServicesBySpecId` | Q Q  | `List<Servicemaster>` | Y | - | - |
| POST | `/mdm/getIpdRosterDetails` | `custom.MdmServiceImpl.getIpdRosterDetailsList` | Q Q Q  | `List<IpdRosterDetails>` | Y | - | - |
| POST | `/mdm/getPatientGrievanceHistory` | `custom.MdmServiceImpl.getPatientGrievanceHistory` | JSON Q Q  | `List<PatientGrievanceHistory>` | - | - | - |
| POST | `/mdm/getSSOdetailsListForSearch` | `custom.MdmServiceImpl.getSSOUSerDetailsall` | Q Q  | `Map<String,String>` | - | - | - |
| POST | `/mdm/getSSOEMPIdListForSearch` | `custom.MdmServiceImpl.getSSOEMPDetails` | JSON  | `List<SearchSSODetailsResultDto>` | - | - | - |
| POST | `/mdm/getSSOIdListForSearch` | `custom.MdmServiceImpl.getSSODetails` | JSON  | `List<SearchSSODetailsResultDto>` | Y | - | - |
| POST | `/mdm/getTrxHistory` | `custom.MdmServiceImpl.getTrxHistory` | JSON Q Q  | `List<TrxHistory>` | Y | - | - |
| POST | `/mdm/reopenGrievanceTicket` | `custom.MdmServiceImpl.reopenGrievanceTicket` | JSON HDR Q  | `Boolean` | - | - | - |
| POST | `/mdm/saveUser` | `custom.MdmServiceImpl.saveUser` | JSON ?  | `void` | Y | - | - |
| POST | `/mdm/updateBedMaster/{id}` | `custom.MdmServiceImpl.updateBedMaster` | JSON P ?  | `Bedmaster` | - | - | - |
| POST | `/mdm/updateIpdRosterDetails` | `custom.MdmServiceImpl.updateIpdRosterDetailsList` | JSON  | `List<IpdRosterDetails>` | Y | - | - |
| POST | `/mdm/updateRadServices` | `custom.MdmServiceImpl.updateRadServices` | JSON Q  | `void` | Y | - | - |
| POST | `/mdm/updateServicemasterList` | `custom.MdmServiceImpl.updateServicemasterList` | JSON ?  | `void` | Y | - | - |
| POST | `/mdm/updateUser` | `custom.MdmServiceImpl.updateUser` | JSON ?  | `void` | Y | - | - |
| POST | `/mdm/updateUserStatus` | `custom.MdmServiceImpl.updateUserStatus` | Q  | `boolean` | Y | - | - |
| POST | `/mdm/updateUserStores/{userId}` | `custom.MdmServiceImpl.updateUserStores` | JSON P  | `void` | Y | - | - |
| POST | `/mdm/updateUserVistaSign` | `custom.MdmServiceImpl.updateUserVistaSign` | Q Q  | `void` | Y | - | - |
| POST | `/mdm/updateWardMaster/{id}` | `custom.MdmServiceImpl.updateWardMaster` | P  | `void` | - | - | - |
| PUT | `/mdm/deleteServiceListById/{id}` | `custom.MdmServiceImpl.deleteServiceListById` | P ?  | `void` | Y | - | - |
| GET | `/medication/getItemByCategoryId` | `custom.MedicationServiceImpl.getItemsByCategoryID` | Q Q  | `List<ItemByItemCategoryResultDto>` | - | - | - |
| GET | `/medication/getItemByCategoryIdFast` | `custom.MedicationServiceImpl.getItemByCategoryIdFast` | Q  | `List<ItemByItemCategoryResultDto>` | - | - | - |
| GET | `/medication/getItemByNameForNursing` | `custom.MedicationServiceImpl.getItemByNameForNursing` | Q  | `List<ItemByNameResultImprDto>` | - | - | - |
| GET | `/medication/getItemCategoryId` | `custom.MedicationServiceImpl.getItemsByCategoryID` | Q Q  | `List<ItemByItemCategoryResultDto>` | - | - | - |
| GET | `/medication/getItemSaleDetailByVisitId` | `custom.MedicationServiceImpl.getItemSaleDetailByItemSaleId` | Q Q  | `List<ItemSalesDetailsResultDto>` | - | - | - |
| GET | `/medication/itemcategoryAndStore` | `custom.MedicationServiceImpl.getItemCategoryAndStore` | - | `ItemCategoryStoreView` | - | - | - |
| GET | `/medication/itemcategoryByStoreIdAndStore` | `custom.MedicationServiceImpl.itemcategoryByStoreIdAndStore` | Q  | `ItemCategoryStoreView` | - | - | - |
| GET | `/medication/removeNurseItem` | `custom.MedicationServiceImpl.removeNurseItem` | Q  | `void` | - | - | - |
| GET | `/medication/updateItemSaleOrder` | `custom.MedicationServiceImpl.updateItemSaleOrder` | Q  | `void` | Y | - | - |
| POST | `/medication/addItemPriceNew` | `custom.MedicationServiceImpl.addItemPrice` | JSON  | `ItemSalesDto` | - | - | - |
| POST | `/medication/addItemSaleForCounterSale` | `custom.MedicationServiceImpl.addItemSaleForCounterSale` | JSON  | `ItemSalesDto` | - | - | - |
| POST | `/medication/getAllItemAndItemBatchListByVisitID` | `custom.MedicationServiceImpl.getAllItemAndItemBatchListByVisitID` | JSON  | `List<ItemAndItemBatchListByVisitIDResultDto>` | - | - | - |
| POST | `/medication/getItemAndItemBatchListByStoreId` | `custom.MedicationServiceImpl.getItemAndItemBatchListByStoreId` | JSON  | `List<ItemAndItemBatchListByStoreIDResultDto>` | - | - | - |
| POST | `/medication/getItemAndItemBatchListByVisitID` | `custom.MedicationServiceImpl.getItemAndItemBatchListByVisitID` | JSON  | `List<ItemAndItemBatchListByVisitIDResultDto>` | Y | - | - |
| POST | `/medication/getItemAndItemBatchListByVisitIDForNurse` | `custom.MedicationServiceImpl.getItemAndItemBatchListByVisitIDForNurse` | JSON  | `List<ItemAndItemBatchListByVisitIDResultDto>` | Y | - | - |
| POST | `/medication/getItemAndItemBatchListForNurseOrder` | `custom.MedicationServiceImpl.getItemAndItemBatchListForNurseOrder` | JSON  | `List<ItemAndItemBatchListByStoreIDResultDto>` | - | - | - |
| POST | `/medication/getPharmacyItemSaleBill` | `custom.MedicationServiceImpl.getPharmacyItemSaleBillService` | JSON Q  | `PharmacyBillListViewNew` | Y | - | - |
| POST | `/medication/getPharmacyItemSaleBillForCounterSale` | `custom.MedicationServiceImpl.getPharmacyItemSaleBillForCounterSale` | JSON Q  | `PharmacyBillListViewNew` | - | - | - |
| POST | `/medication/getPharmacySaleReturn` | `custom.MedicationServiceImpl.getPharmacySaleReturn` | JSON Q  | `BillReceiptView` | - | - | - |
| POST | `/medication/updateItemReturns` | `custom.MedicationServiceImpl.updateItemReturns` | JSON  | `List<TemSaleReturnDto>` | - | - | - |
| PUT | `/medication/dispenseItemSale` | `custom.MedicationServiceImpl.dispenseItemSale` | Q Q  | `void` | - | - | - |
| PUT | `/medication/updateCreditPaymentItemSale` | `custom.MedicationServiceImpl.updateCreditPaymentItemSale` | JSON  | `ItemSalesItemResultDto` | Y | - | - |
| PUT | `/medication/updatePrescInstruction` | `custom.MedicationServiceImpl.updatePrescInstruction` | Q Q  | `void` | Y | - | - |
| POST | `/notedetailsdata/addUpdateNoteTitleMaster` | `custom.NoteDetailsServiceImpl.addUpdateNoteTitleMaster` | JSON  | `NotetitleMaster` | - | - | - |
| POST | `/notedetailsdata/checkUnsignedNoteIsAvailable` | `custom.NoteDetailsServiceImpl.checkUnsignedNoteIsAvailable` | JSON  | `NoteDetails` | - | - | - |
| POST | `/notedetailsdata/getOPNoteDetails` | `custom.NoteDetailsServiceImpl.getOPNoteDetails` | JSON  | `NoteDetails` | Y | - | - |
| POST | `/notedetailsdata/savenoteDetails` | `custom.NoteDetailsServiceImpl.saveNoteDetails` | JSON  | `NoteDetails` | - | - | - |
| POST | `/notedetailsdata/saveOPNoteDetails` | `custom.NoteDetailsServiceImpl.saveOPNoteDetails` | JSON  | `NoteDetails` | Y | - | - |
| PUT | `/notedetailsdata/note/{id}` | `custom.NoteDetailsServiceImpl.notedetailsById` | JSON P  | `NoteDetails` | - | - | - |
| GET | `/pathoOrder/document/{id}` | `custom.PathoOrderServiceImpl.getImageById` | P ?  | `void` | Y | - | - |
| GET | `/pathoOrder/documentActualImage/{id}` | `custom.PathoOrderServiceImpl.getActualImageByIdForLabResult` | P ?  | `void` | - | - | - |
| GET | `/pathoOrder/documentImage/{id}` | `custom.PathoOrderServiceImpl.getImageByIdForLabResult` | HDR P  | `String` | - | - | - |
| GET | `/pathoOrder/downloadPathoResultApi` | `custom.PathoOrderServiceImpl.getPathoResultApiPrint` | Q Q Q Q  | `List<String>` | - | - | - |
| GET | `/pathoOrder/getAllOrganUnmapped` | `custom.PathoOrderServiceImpl.getAllOrganUnmapped` | - | `List<Organmaster>` | - | - | - |
| GET | `/pathoOrder/getAllPathoTestUnmapped` | `custom.PathoOrderServiceImpl.getAllPathoTestUnmapped` | - | `List<Pathotestmaster>` | - | - | - |
| GET | `/pathoOrder/getAllServiceUnmapped` | `custom.PathoOrderServiceImpl.getAllServiceUnmapped` | - | `List<Servicemaster>` | - | - | - |
| GET | `/pathoOrder/getMachineTestMaster/{id}` | `custom.PathoOrderServiceImpl.getMachineTestMaster` | P  | `Machinetestmaster` | Y | - | - |
| GET | `/pathoOrder/getMachineTestMasterList` | `custom.PathoOrderServiceImpl.getMachineTestMasterList` | Q Q  | `List<Machinetestmaster>` | Y | - | - |
| GET | `/pathoOrder/getMrnNo` | `custom.PathoOrderServiceImpl.getMrnNo` | Q Q Q  | `PathoSampleSearchResultDto` | - | - | - |
| GET | `/pathoOrder/getMrnNoRad` | `custom.PathoOrderServiceImpl.getMrnNoRad` | Q Q Q  | `PathoSampleSearchResultDto` | Y | - | - |
| GET | `/pathoOrder/getOrderDetails` | `custom.PathoOrderServiceImpl.getOrderDetails` | Q Q  | `List<TestOrderDetailResultDto>` | Y | - | - |
| GET | `/pathoOrder/getPathologyQueue` | `custom.PathoOrderServiceImpl.getPathologyQueue` | Q  | `List<Cabinmaster>` | - | - | - |
| GET | `/pathoOrder/getPathoOrganismListById/{id}` | `custom.PathoOrderServiceImpl.getPathoOrganismListById` | P  | `Pathorganismmaster` | Y | - | - |
| GET | `/pathoOrder/getPathoParameterListById` | `custom.PathoOrderServiceImpl.getPathoParameterListById` | Q  | `Pathoparametermaster` | Y | - | - |
| GET | `/pathoOrder/getPathoProfileListById/{id}` | `custom.PathoOrderServiceImpl.getPathoProfileListById` | P  | `Pathoprofilemaster` | Y | - | - |
| GET | `/pathoOrder/getPathoTestDetailsById/{id}` | `custom.PathoOrderServiceImpl.getwholePatoTestMasterId` | P  | `Pathotestmaster` | Y | - | - |
| GET | `/pathoOrder/getPatientDocumentsList` | `custom.PathoOrderServiceImpl.getPatientDocumentsList` | Q Q  | `List<PathOrderBookingDetailDocuments>` | Y | - | - |
| GET | `/pathoOrder/getRadImage` | `custom.PathoOrderServiceImpl.findRadImage` | Q Q Q  | `byte[]` | - | - | - |
| GET | `/pathoOrder/getRadResult` | `custom.PathoOrderServiceImpl.getRadResult` | Q Q  | `List<GetRadResult>` | Y | - | - |
| GET | `/pathoOrder/getRadTestImage` | `custom.PathoOrderServiceImpl.getRadTestImage` | Q Q Q  | `String` | Y | - | - |
| GET | `/pathoOrder/getTemplateAndItemDetails` | `custom.PathoOrderServiceImpl.getTemplateAndItemDetails` | Q  | `List<TemplateAndItemDetailsResultDto_1>` | Y | - | - |
| GET | `/pathoOrder/getTemplateResult` | `custom.PathoOrderServiceImpl.getTemplateResult` | Q Q Q  | `List<List<PathTemplateTestResultDto>>` | - | - | - |
| GET | `/pathoOrder/getTestDetailsByOrderId` | `custom.PathoOrderServiceImpl.getTestDetailsByOrderId` | Q Q  | `PatientLabTestDetailsDto` | - | - | - |
| GET | `/pathoOrder/getUnitsForCovidApproval` | `custom.PathoOrderServiceImpl.getUnitsForCovidApproval` | Q Q  | `List<Unitmaster>` | Y | - | - |
| GET | `/pathoOrder/getUnitsForOutSourcedTest` | `custom.PathoOrderServiceImpl.getUnitsForOutSourcedTest` | Q Q  | `List<Unitmaster>` | - | - | - |
| GET | `/pathoOrder/getUnitsForOutSourcedTestAll` | `custom.PathoOrderServiceImpl.getUnitsForOutSourcedTestAll` | Q  | `List<Unitmaster>` | - | - | - |
| GET | `/pathoOrder/getUnitWiseServices` | `custom.PathoOrderServiceImpl.getUnitWiseService` | Q  | `List<Servicemaster>` | - | - | - |
| GET | `/pathoOrder/printPathoResultApi` | `custom.PathoOrderServiceImpl.getprintPathoResultApi` | Q Q Q Q  | `List<String>` | - | - | - |
| GET | `/pathoOrder/reportReleaseConfirmation` | `custom.PathoOrderServiceImpl.reportReleaseConfirmation` | Q  | `Long` | - | - | - |
| GET | `/pathoOrder/updatePriority` | `custom.PathoOrderServiceImpl.updatePriority` | Q Q  | `Void` | - | - | - |
| POST | `/pathoOrder/acceptRadOrder` | `custom.PathoOrderServiceImpl.acceptRadOrder` | JSON  | `List<RadOrderBookingListForWorkOrderResultDto_New>` | - | - | - |
| POST | `/pathoOrder/addMachineTestMaster` | `custom.PathoOrderServiceImpl.addMachineTestMaster` | JSON  | `List<Machinetestmaster>` | - | - | - |
| POST | `/pathoOrder/addMachineWiseMicroResult` | `custom.PathoOrderServiceImpl.addMachineWiseMicroResult` | JSON Q  | `List<String>` | - | - | - |
| POST | `/pathoOrder/addMachineWiseResult` | `custom.PathoOrderServiceImpl.addMachineWiseResult` | JSON Q  | `Boolean` | Y | - | - |
| POST | `/pathoOrder/addPathoOrganismList` | `custom.PathoOrderServiceImpl.addPathoOrganismList` | JSON  | `void` | - | - | - |
| POST | `/pathoOrder/addPathoParameterList` | `custom.PathoOrderServiceImpl.addPathoParameterList` | JSON  | `void` | - | - | - |
| POST | `/pathoOrder/addPathoProfileList` | `custom.PathoOrderServiceImpl.addPathoProfileList` | JSON  | `void` | - | - | - |
| POST | `/pathoOrder/addPathoResultEntryTemplate` | `custom.PathoOrderServiceImpl.addPathoResultEntryTemplate` | JSON  | `PathoResultEntryTemplateDto` | - | - | - |
| POST | `/pathoOrder/addPathoTestMaster` | `custom.PathoOrderServiceImpl.addPathoTestMaster` | JSON  | `void` | - | - | - |
| POST | `/pathoOrder/addPathPatientTestParameterDetails` | `custom.PathoOrderServiceImpl.addPathPatientTestParameterDetails` | JSON  | `void` | Y | - | - |
| POST | `/pathoOrder/addPatientMicroResult` | `custom.PathoOrderServiceImpl.addPatientMicroResult` | JSON  | `List<PathPatientCultureandSensitivityDetailsDto>` | - | - | - |
| POST | `/pathoOrder/addRadResultEntry` | `custom.PathoOrderServiceImpl.addRadResultEntry` | JSON  | `AddRadResultEntryDto` | Y | - | - |
| POST | `/pathoOrder/addResultReviewHoldRecord` | `custom.PathoOrderServiceImpl.addResultReviewHoldRecord` | JSON  | `void` | Y | - | - |
| POST | `/pathoOrder/addResultReviewNotes` | `custom.PathoOrderServiceImpl.addResultReviewNotes` | JSON  | `void` | Y | - | - |
| POST | `/pathoOrder/approveCovidLabTest` | `custom.PathoOrderServiceImpl.approveCovidLabTest` | JSON  | `void` | - | - | - |
| POST | `/pathoOrder/checkChargeInfo` | `custom.PathoOrderServiceImpl.checkChargeInfo` | JSON ?  | `Boolean` | - | - | - |
| POST | `/pathoOrder/checkChargeInfoForRad` | `custom.PathoOrderServiceImpl.checkChargeInfoForRad` | JSON  | `Boolean` | - | - | - |
| POST | `/pathoOrder/fetchLabTestId` | `custom.PathoOrderServiceImpl.fetchLabTest` | JSON  | `List<Pathotestmaster>` | - | - | - |
| POST | `/pathoOrder/finalizeRadResult` | `custom.PathoOrderServiceImpl.finalizeRadResult` | JSON  | `void` | Y | - | - |
| POST | `/pathoOrder/generateBarCode` | `custom.PathoOrderServiceImpl.generateBarCodeApi` | JSON  | `Map<String, List<PathOrderBookingDetailList_NEWResultDto_1>>` | - | - | - |
| POST | `/pathoOrder/getAllPathoListByUnitId` | `custom.PathoOrderServiceImpl.getAllPathoListByUnitId` | JSON Q Q  | `List<PathoWorkOrderListResultDto>` | Y | - | - |
| POST | `/pathoOrder/getCultureSenResultForTest` | `custom.PathoOrderServiceImpl.getCultureSenResultForTest` | JSON  | `PathoCutureView` | Y | - | - |
| POST | `/pathoOrder/getCultureSentResult` | `custom.PathoOrderServiceImpl.getCultureSenResult` | JSON  | `PathoCutureView` | Y | - | - |
| POST | `/pathoOrder/getCultureSentResult_lastresult` | `custom.PathoOrderServiceImpl.getCultureSenResult_lastresult` | JSON  | `PathoCutureView` | Y | - | - |
| POST | `/pathoOrder/getGroupingTestParametersforDonorServices` | `custom.PathoOrderServiceImpl.getGroupingTestParametersforDonorServices` | JSON Q  | `Map<String, List<PathTestAndSubTestParameterResultDto>>` | Y | - | - |
| POST | `/pathoOrder/getKdlVerifyList` | `custom.PathoOrderServiceImpl.getKdlVerifyList` | JSON Q  | `LabWorkLoadListView` | - | - | - |
| POST | `/pathoOrder/getLabBucketNoForMicro` | `custom.PathoOrderServiceImpl.getLabBucketNoForMicro` | JSON Q  | `String` | Y | - | - |
| POST | `/pathoOrder/getMachineWiseResultReview` | `custom.PathoOrderServiceImpl.getMachineWiseResultReview` | JSON  | `Map<String, List<MachineWiseTestResultDto>>` | - | - | - |
| POST | `/pathoOrder/getNormalResult` | `custom.PathoOrderServiceImpl.getNormalResult` | JSON  | `List<List<PathNormalTestResultDto>>` | - | - | - |
| POST | `/pathoOrder/getPacOrderSetResult` | `custom.PathoOrderServiceImpl.getPacOrderSetResult` | Q Q  | `PacLabResultStatus` | - | - | - |
| POST | `/pathoOrder/getParameterResultValue` | `custom.PathoOrderServiceImpl.getParameterResultValue` | JSON Q  | `PathParamsView` | Y | - | - |
| POST | `/pathoOrder/getPathoResultApi` | `custom.PathoOrderServiceImpl.getPathoResultApi` | Q Q Q Q  | `GetPathoResultApi` | - | - | - |
| POST | `/pathoOrder/getPathoResultApiForComparisonSheet` | `custom.PathoOrderServiceImpl.getPathoResultApiForComparisonSheet` | Q Q Q  | `Map<String,Object>` | - | - | - |
| POST | `/pathoOrder/getPathoResultApiForDischargeSummary` | `custom.PathoOrderServiceImpl.getPathoResultApiForDischargeSummaryPrint` | Q Q Q  | `Map<String,Object>` | - | - | - |
| POST | `/pathoOrder/getPathoResultPrint` | `custom.PathoOrderServiceImpl.getPathoResultPrint` | JSON Q ?  | `List<PathResultView>` | - | - | - |
| POST | `/pathoOrder/getPathoTemplateList` | `custom.PathoOrderServiceImpl.getPathoTemplateList` | JSON  | `PathTempletView` | - | - | - |
| POST | `/pathoOrder/getPathoTemplateListForTest` | `custom.PathoOrderServiceImpl.getPathoTemplateListForTest` | JSON  | `PathTempletView` | - | - | - |
| POST | `/pathoOrder/getPathoWorklist` | `custom.PathoOrderServiceImpl.getPathoWorklist` | JSON Q  | `List<PathoWorkOrderListResultDto>` | - | - | - |
| POST | `/pathoOrder/getPreviousDetailsForPat` | `custom.PathoOrderServiceImpl.getPreviousDetailsForPat` | JSON  | `Map<java.sql.Timestamp, List<PathPreviousNormalTestResultDto>>` | - | - | - |
| POST | `/pathoOrder/getServices` | `custom.PathoOrderServiceImpl.getServices` | Q Q JSON  | `List<ServiceHoldRequest>` | Y | - | - |
| POST | `/pathoOrder/getTestParameterAndSubTestDetail` | `custom.PathoOrderServiceImpl.getTestParameterAndSubTestDetail` | JSON  | `PathParamsView` | - | - | - |
| POST | `/pathoOrder/getTestParameterAndSubTestDetails` | `custom.PathoOrderServiceImpl.getTestParameterAndSubTestDetails` | JSON  | `PathParamsView` | Y | - | - |
| POST | `/pathoOrder/getTestParameterAndSubTestDetailsForTest` | `custom.PathoOrderServiceImpl.getTestParameterAndSubTestDetailsForTest` | JSON  | `PathParamsView` | - | - | - |
| POST | `/pathoOrder/getTestParametersAndTemplates` | `custom.PathoOrderServiceImpl.getTestParametersAndTemplatesApi` | JSON  | `PathoTypeView` | Y | - | - |
| POST | `/pathoOrder/getUpdatedResultReview` | `custom.PathoOrderServiceImpl.getUpdatedResultReview` | JSON  | `Map<java.sql.Timestamp, List<PathNormalTestResultDto>>` | Y | - | - |
| POST | `/pathoOrder/getUserDataFromBarCode` | `custom.PathoOrderServiceImpl.getUserDataFromBarCode` | JSON Q  | `PathResultdtoforAPI` | - | - | - |
| POST | `/pathoOrder/holdServiceforBilling` | `custom.PathoOrderServiceImpl.holdServiceforBilling` | JSON  | `Boolean` | - | - | - |
| POST | `/pathoOrder/imageCaptured` | `custom.PathoOrderServiceImpl.setImageCapturedForRadiology` | JSON  | `Long` | - | - | - |
| POST | `/pathoOrder/labPatientDemographicDetail` | `custom.PathoOrderServiceImpl.labPatientDemographicDetail` | JSON  | `Map<Long, Map<String, List<PathOrderBookingDetailList_NEWResultDto_1>>` | Y | - | - |
| POST | `/pathoOrder/notifyPatientDetails` | `custom.PathoOrderServiceImpl.notifyPatientDetails` | JSON  | `String` | - | - | - |
| POST | `/pathoOrder/pathLabCovidStatusList` | `custom.PathoOrderServiceImpl.pathLabCovidStatusList` | JSON Q  | `List<PathoWorkOrderListResultDto>` | - | - | - |
| POST | `/pathoOrder/pathLabList` | `custom.PathoOrderServiceImpl.pathLabList_New` | JSON  | `Map<Long, Map<String, List<PathOrderBookingDetailList_NEWResultDto_1>>` | - | - | - |
| POST | `/pathoOrder/pathLabList_AcceptFilter` | `custom.PathoOrderServiceImpl.pathLabList_AcceptFilter` | JSON  | `PathologyFilterCount` | - | - | - |
| POST | `/pathoOrder/pathLabList_CollectionFilter` | `custom.PathoOrderServiceImpl.pathLabList_CollectionFilter` | JSON  | `PathologyFilterCount` | - | - | - |
| POST | `/pathoOrder/pathLabList_filter` | `custom.PathoOrderServiceImpl.pathLabList_filter` | JSON  | `Map<Long, Map<String, List<PathOrderBookingDetailList_NEWResultDto_1>>` | - | - | - |
| POST | `/pathoOrder/pathLabList_MachineFilter` | `custom.PathoOrderServiceImpl.pathLabList_MachineFilter` | JSON  | `PathologyFilterCount` | - | - | - |
| POST | `/pathoOrder/pathLabList_old` | `custom.PathoOrderServiceImpl.pathLabList_old` | JSON  | `Map<Long, Map<String, List<PathOrderBookingDetailList_NEWResultDto_1>>` | Y | - | - |
| POST | `/pathoOrder/pathLabList_ReleaseFilter` | `custom.PathoOrderServiceImpl.pathLabList_ReleaseFilter` | JSON  | `PathologyFilterCount` | - | - | - |
| POST | `/pathoOrder/pathLabList_ResultFilter` | `custom.PathoOrderServiceImpl.pathLabList_ResultFilter` | JSON  | `PathologyFilterCount` | - | - | - |
| POST | `/pathoOrder/pathLabList_ReviewFilter` | `custom.PathoOrderServiceImpl.pathLabList_ReviewFilter` | JSON  | `PathologyFilterCount` | - | - | - |
| POST | `/pathoOrder/pathLabResultForOthermodule` | `custom.PathoOrderServiceImpl.pathLabResultForOthermodule` | JSON  | `PathApiResultView` | - | - | - |
| POST | `/pathoOrder/pathLabStatusList` | `custom.PathoOrderServiceImpl.pathLabStatusList` | JSON Q  | `List<PathoWorkOrderListResultDto>` | - | - | - |
| POST | `/pathoOrder/pathLabStatusListNew` | `custom.PathoOrderServiceImpl.pathLabStatusListNew` | JSON Q  | `List<PathoWorkOrderListResultDto>` | - | - | - |
| POST | `/pathoOrder/pathLabWorkLoadList` | `custom.PathoOrderServiceImpl.labWorkLoadListNew` | JSON Q  | `LabWorkLoadListView` | Y | - | - |
| POST | `/pathoOrder/pathoCollectionReports` | `custom.PathoOrderServiceImpl.pathoCollectionReports` | JSON  | `Map<String, List<PathOrderBookingDetailList_NEWResultDto_1>>` | - | - | - |
| POST | `/pathoOrder/pathoCollectionReportsNew` | `custom.PathoOrderServiceImpl.pathoCollectionReportsNew` | JSON  | `Map<String, List<PathOrderBookingDetailList_NEWResultDto_1>>` | - | - | - |
| POST | `/pathoOrder/pathoCollectionSampleReports` | `custom.PathoOrderServiceImpl.getPatientSampleDetailList` | JSON  | `List<PathOrderBookingDetailList_NEWResultDto_1>` | - | - | - |
| POST | `/pathoOrder/pathoSamplePrint` | `custom.PathoOrderServiceImpl.pathoSamplePrint` | JSON Q  | `Boolean` | Y | - | - |
| POST | `/pathoOrder/pathoTemplateReport` | `custom.PathoOrderServiceImpl.pathoLabReport` | JSON Q ?  | `PathResultView` | - | - | - |
| POST | `/pathoOrder/pathoTestInfoDetails` | `custom.PathoOrderServiceImpl.getpathoTestInfoDetails` | Q Q  | `List<PathoTestInfoDetailsDto>` | - | - | - |
| POST | `/pathoOrder/pathPatientWorkList` | `custom.PathoOrderServiceImpl.pathPatientWorkList` | JSON Q  | `List<PathoWorkOrderListResultDto>` | Y | - | - |
| POST | `/pathoOrder/patientMachinePathLabList` | `custom.PathoOrderServiceImpl.patientMachinePathLabList` | Q  | `Map<Long, Map<String, List<PathOrderBookingDetailList_NEWResultDto_1>>` | - | - | - |
| POST | `/pathoOrder/patientPathLabList` | `custom.PathoOrderServiceImpl.patientPathLabList` | Q  | `Map<Long, Map<String, List<PathOrderBookingDetailList_NEWResultDto_1>>` | - | - | - |
| POST | `/pathoOrder/patientPathoCollectionReports` | `custom.PathoOrderServiceImpl.patientPathoCollectionReports` | JSON  | `Map<String, List<PathOrderBookingDetailList_NEWResultDto_1>>` | - | - | - |
| POST | `/pathoOrder/patientRadList` | `custom.PathoOrderServiceImpl.getPatientRadList` | Q  | `Map<Long, Map<String, List<RadOrderBookingList_New2ResultDto>>>` | - | - | - |
| POST | `/pathoOrder/patientRadTestDetailsList` | `custom.PathoOrderServiceImpl.getPatientRadTestDetailsList` | JSON  | `List<RadOrderBookingListForWorkOrderResultDto_New>` | - | - | - |
| POST | `/pathoOrder/patientRadTestList` | `custom.PathoOrderServiceImpl.getPatientRadTestList` | JSON Q  | `Map<Long, List<RadOrderBookingListForWorkOrderResultDto_New>>` | - | - | - |
| POST | `/pathoOrder/patientRadTestListForHL7` | `custom.PathoOrderServiceImpl.getPatientRadTestListForHL7` | JSON  | `List<Outboundhis>` | - | - | - |
| POST | `/pathoOrder/printPathoResultApiVM` | `custom.PathoOrderServiceImpl.printPathoResultApiVM` | JSON  | `PathApiResultView` | Y | - | - |
| POST | `/pathoOrder/printPatientLabTestDetails` | `custom.PathoOrderServiceImpl.printPatientLabTestDetails` | JSON  | `String` | Y | - | - |
| POST | `/pathoOrder/radLabListNew` | `custom.PathoOrderServiceImpl.radLabListNew` | JSON  | `Map<Long, List<RadOrderBookingListForWorkOrderResultDto_New>>` | - | - | - |
| POST | `/pathoOrder/radListFilter` | `custom.PathoOrderServiceImpl.radListFilter` | JSON  | `PathologyFilterCount` | - | - | - |
| POST | `/pathoOrder/radReschedule` | `custom.PathoOrderServiceImpl.radReschedule` | JSON  | `void` | - | - | - |
| POST | `/pathoOrder/radRetest` | `custom.PathoOrderServiceImpl.radRetest` | JSON  | `RadRejectAndRetestDto` | - | - | - |
| POST | `/pathoOrder/sampleAccepted` | `custom.PathoOrderServiceImpl.saveAccepted` | JSON  | `void` | - | - | - |
| POST | `/pathoOrder/sampleRejected` | `custom.PathoOrderServiceImpl.sampleRejected` | JSON  | `Long` | Y | - | - |
| POST | `/pathoOrder/sendOTPForKDLVerify` | `custom.PathoOrderServiceImpl.sendOTPForKDLVerify` | Q Q  | `OnlineAppointmentDto` | - | - | - |
| POST | `/pathoOrder/statusScreenLabReportPrint` | `custom.PathoOrderServiceImpl.nursingLabReportPrint` | JSON  | `String` | - | - | - |
| POST | `/pathoOrder/updateKDLVerification` | `custom.PathoOrderServiceImpl.updateKDLVerification` | JSON  | `Boolean` | - | - | - |
| POST | `/pathoOrder/updatePathologyTestMaster` | `custom.PathoOrderServiceImpl.updatePathoTestMaster` | JSON  | `Pathotestmaster` | - | - | - |
| POST | `/pathoOrder/updatePathoOrderStatus` | `custom.PathoOrderServiceImpl.updatePathoOrderStatus` | JSON  | `boolean` | - | - | - |
| POST | `/pathoOrder/updatePathoParameterList` | `custom.PathoOrderServiceImpl.updatePathoParameterList` | JSON  | `Pathoparametermaster` | Y | - | - |
| POST | `/pathoOrder/updatePathOrderBookingDetail` | `custom.PathoOrderServiceImpl.pathoOrderBookingDetailUpdate` | JSON ?  | `Map<String, List<UpdatePathOrderBookingDetailDto>>` | - | - | - |
| POST | `/pathoOrder/updatePathoResultEntryTemplate` | `custom.PathoOrderServiceImpl.updatePathoResultEntryTemplate` | JSON  | `void` | - | - | - |
| POST | `/pathoOrder/updatePathPatientTestParameterDetails` | `custom.PathoOrderServiceImpl.updatePathPatientTestParameterDetails` | JSON  | `void` | Y | - | - |
| POST | `/pathoOrder/updateServices` | `custom.PathoOrderServiceImpl.updateServices` | Q Q  | `void` | - | - | - |
| POST | `/pathoOrder/validateOTPForKDLVerify` | `custom.PathoOrderServiceImpl.validateOTPForKDLVerify` | Q Q Q  | `OnlineAppointmentDto` | - | - | - |
| PUT | `/pathoOrder/addPatientLabResultDocument` | `custom.PathoOrderServiceImpl.addPatientLabResultDocument` | JSON Q Q  | `PatientIDDocumentDto` | Y | - | - |
| PUT | `/pathoOrder/removePatientPortalDocument` | `custom.PathoOrderServiceImpl.removePatientPortalDocument` | Q Q Q  | `void` | - | - | - |
| PUT | `/pathoOrder/updateMachineTestMaster/{id}` | `custom.PathoOrderServiceImpl.updateMachineTestMaster` | JSON P  | `Machinetestmaster` | - | - | - |
| PUT | `/pathoOrder/updatePathoOrganismList/{id}` | `custom.PathoOrderServiceImpl.updatePathoOrganismList` | JSON P  | `Pathorganismmaster` | Y | - | - |
| PUT | `/pathoOrder/updatePathoProfileList/{id}` | `custom.PathoOrderServiceImpl.updatePathoProfileList` | JSON P  | `Pathoprofilemaster` | Y | - | - |
| GET | `/patientDetailReg/getPatientLatestBilling` | `custom.PatientDetailRegistrationServiceImpl.findPatientLatestBilling` | Q Q Q  | `PatientBillSearchView` | - | - | - |
| GET | `/patientDetailReg/getPatientLatestVisit` | `custom.PatientDetailRegistrationServiceImpl.getPatientLatestVisit` | Q  | `Object[]` | Y | - | - |
| POST | `/patientDetailReg/getIPDPatientGeneralDetailsSearch` | `custom.PatientDetailRegistrationServiceImpl.getIPDPatientGeneralDetailsForService` | JSON  | `List<SearchPatientAndVisitByNameMobNoResultDto>` | - | - | - |
| POST | `/patientDetailReg/getIPDPatientGeneralDetailsSearchforOpBill` | `custom.PatientDetailRegistrationServiceImpl.getIPDPatientGeneralDetailsForServiceformBM` | JSON  | `CheckoutView` | - | - | - |
| POST | `/patientDetailReg/getMotherChildLinkingDetailsDocit` | `custom.PatientDetailRegistrationServiceImpl.` |  | `?` | - | - | - |
| POST | `/patientDetailReg/getPatient` | `custom.PatientDetailRegistrationServiceImpl.getPatient` | JSON  | `PatientAdmissionView` | - | - | - |
| POST | `/patientDetailReg/getPatientGeneralDetailsSearch` | `custom.PatientDetailRegistrationServiceImpl.getPatientGeneralDetailsForService` | JSON  | `List<SearchPatientAndVisitByNameMobUpdatedResultDto>` | Y | - | - |
| POST | `/patientDetailReg/getPatientGeneralDetailsSearchforIpbill` | `custom.PatientDetailRegistrationServiceImpl.getPatientGeneralDetailsForServicefromCheckout` | JSON  | `CheckoutView` | - | - | - |
| POST | `/patientDetailReg/getPatientHealthPlanConsumptionSearch` | `custom.PatientDetailRegistrationServiceImpl.getPatientHealthPlanConsumptionSearch` | JSON  | `List<SearchHealthPlanConsumptionResultDTO>` | - | - | - |
| POST | `/patientDetailReg/updatePatient` | `custom.PatientDetailRegistrationServiceImpl.updatePatient` | JSON  | `void` | - | - | - |
| POST | `/patientDetailReg/updatePatientAddressDetails` | `custom.PatientDetailRegistrationServiceImpl.updatePatientAddressDetails` | JSON  | `void` | - | - | - |
| DELETE | `/patientRegAddmission/removeMLCFileToById/{id}` | `custom.PatientRegAdmissionServiceImpl.removeMLCFileToById` | P  | `void` | - | - | - |
| GET | `/patientRegAddmission/checkAprovedOrReject` | `custom.PatientRegAdmissionServiceImpl.checkAprovedOrReject` | Q  | `Object` | Y | - | - |
| GET | `/patientRegAddmission/DepartmentWiseIPDCount` | `custom.PatientRegAdmissionServiceImpl.departmentWiseIpdCounts` | Q Q  | `Map<Object,Object>` | - | - | - |
| GET | `/patientRegAddmission/getAllPreviuosAdmissionsOfUnit` | `custom.PatientRegAdmissionServiceImpl.getAllPreviuosAdmissionsOfUnit` | Q Q Q  | `List<AllPreviuosAdmissionsOfUnitDto>` | Y | - | - |
| GET | `/patientRegAddmission/getMLCInfo` | `custom.PatientRegAdmissionServiceImpl.getMLCInfo` | Q Q Q  | `MLCDetails` | Y | - | - |
| GET | `/patientRegAddmission/getMlcStatusForIpList` | `custom.PatientRegAdmissionServiceImpl.getMlcStatusForIpList` | Q Q  | `List<MlcClearanceDto>` | Y | - | - |
| GET | `/patientRegAddmission/getPatientInvestigations` | `custom.PatientRegAdmissionServiceImpl.getPatientInvestigations` | Q Q  | `List<InvestigationAndBillResultDto>` | Y | - | - |
| GET | `/patientRegAddmission/IpdDashboard` | `custom.PatientRegAdmissionServiceImpl.IpdCounts` | - | `Map<String, Map<Object, Object>>` | Y | - | - |
| GET | `/patientRegAddmission/PatientCount` | `custom.PatientRegAdmissionServiceImpl.patientCount` | Q  | `IhmsDetailsResponseDto` | Y | - | - |
| GET | `/patientRegAddmission/pdfAdmFaceSheet` | `custom.PatientRegAdmissionServiceImpl.pdfAppointmentFaceSheet` | Q Q  | `String[]` | Y | - | - |
| GET | `/patientRegAddmission/reIntiateMlcStatus` | `custom.PatientRegAdmissionServiceImpl.updateRejectStatus` | Q  | `void` | Y | - | - |
| GET | `/patientRegAddmission/unitWiseIPDCount` | `custom.PatientRegAdmissionServiceImpl.unitWiseIpdCounts` | Q Q Q  | `Map<Object,Object>` | - | - | - |
| GET | `/patientRegAddmission/updateAprrovedStatus` | `custom.PatientRegAdmissionServiceImpl.updateApproveStatus` | Q Q Q  | `void` | - | - | - |
| GET | `/patientRegAddmission/updateRejectStatus` | `custom.PatientRegAdmissionServiceImpl.updateRejectStatus` | Q Q Q  | `void` | Y | - | - |
| GET | `/patientRegAddmission/updateReleaseAcceptStatus` | `custom.PatientRegAdmissionServiceImpl.updateReleaseAcceptStatus` | Q Q  | `void` | - | - | - |
| GET | `/patientRegAddmission/updateReleaseRejectStatus` | `custom.PatientRegAdmissionServiceImpl.updateReleaseRejectStatus` | Q Q  | `void` | Y | - | - |
| GET | `/patientRegAddmission/WardWiseIPDCount` | `custom.PatientRegAdmissionServiceImpl.wardWiseIpdCounts` | Q Q Q  | `Map<Object,Object>` | - | - | - |
| GET | `/patientRegAddmission/WardWiseIPDDetail` | `custom.PatientRegAdmissionServiceImpl.wardWiseIpdDetails` | Q Q Q  | `List<Object []>` | - | - | - |
| POST | `/patientRegAddmission/adddata` | `custom.PatientRegAdmissionServiceImpl.admitPatient` | JSON  | `PatientAdmissionView` | - | - | - |
| POST | `/patientRegAddmission/addUpdateMLCDetails` | `custom.PatientRegAdmissionServiceImpl.addUpdateMLCDetails` | ?  | `MLCDetails` | - | - | - |
| POST | `/patientRegAddmission/getAllMlcCleranceList` | `custom.PatientRegAdmissionServiceImpl.getAllMlcClearanceList` | JSON Q  | `List<MlcClearanceDto>` | Y | - | - |
| POST | `/patientRegAddmission/updateDoctor` | `custom.PatientRegAdmissionServiceImpl.updateDoctorInformation` | ?  | `UpdateDoctorInformation` | - | - | - |
| POST | `/patientRegAddmission/updateMLCRemark` | `custom.PatientRegAdmissionServiceImpl.updateMLCRemark` | Q Q Q  | `void` | Y | - | - |
| GET | `/patientRegAppt/getOperatorMobileNumberMappedUnit` | `custom.PatientRegApptServiceImpl.getOperatorMobileNumberMappedUnit` | Q Q  | `Long` | - | - | - |
| GET | `/patientRegAppt/getPatientAgeForPackageConsumption` | `custom.PatientRegApptServiceImpl.getPatientAgeForPackageConsumption` | Q Q  | `Object[]` | - | - | - |
| GET | `/patientRegAppt/getPatientTransactionDetailsList` | `custom.PatientRegApptServiceImpl.getPatientTransactionDetailsList` | Q  | `PatientTransactionDetailsListView` | Y | - | - |
| GET | `/patientRegAppt/getreferentitytype` | `custom.PatientRegApptServiceImpl.getReferalEntityType` | Q  | `List<Refentitymaster>` | Y | - | - |
| GET | `/patientRegAppt/pdfAppointmentFaceSheet` | `custom.PatientRegApptServiceImpl.pdfAppointmentFaceSheet` | Q  | `String[]` | - | - | - |
| GET | `/patientRegAppt/pdfAppointmentFaceSheetFromAppList` | `custom.PatientRegApptServiceImpl.pdfAppointmentFaceSheetFromAppList` | Q  | `String[]` | - | - | - |
| GET | `/patientRegAppt/pdfAppointmentFaceSheetHtml` | `custom.PatientRegApptServiceImpl.pdfAppointmentFaceSheetHtml` | Q  | `Map<String, Object>` | - | - | - |
| GET | `/patientRegAppt/pdfOpdCard` | `custom.PatientRegApptServiceImpl.pdfOpdCard` | Q  | `String[]` | - | - | - |
| GET | `/patientRegAppt/printTokenSheet` | `custom.PatientRegApptServiceImpl.printTokenSheet` | Q Q HDR  | `String[]` | - | - | - |
| GET | `/patientRegAppt/searchPatientBasicDetailsForOnline` | `custom.PatientRegApptServiceImpl.searchPatientBasicDetailsForOnlineAPI` | Q Q Q  | `OnlineAppointmentDto` | - | - | - |
| GET | `/patientRegAppt/searchPatientDetailsByMobileNo` | `custom.PatientRegApptServiceImpl.searchPatientDetailsByMobileNo` | Q  | `OnlineAppointmentDto` | - | - | - |
| GET | `/patientRegAppt/searchPatientDetailsForOnline` | `custom.PatientRegApptServiceImpl.searchPatientDetailsForOnline` | Q Q  | `OnlineAppointmentDto` | - | - | - |
| GET | `/patientRegAppt/searchPatientPhotoGraph` | `custom.PatientRegApptServiceImpl.searchPatientPhotoGraph` | Q Q  | `String` | - | - | - |
| GET | `/patientRegAppt/searchPatientPhotoGraphJanaadhar` | `custom.PatientRegApptServiceImpl.searchPatientPhotoGraphJanaadhar` | Q Q  | `String` | - | - | - |
| GET | `/patientRegAppt/sendOTP` | `custom.PatientRegApptServiceImpl.sendOTP` | Q Q Q  | `OnlineAppointmentDto` | - | - | - |
| GET | `/patientRegAppt/sendOTPForEnableLoginWithoutSSO` | `custom.PatientRegApptServiceImpl.sendOTPForEnableLoginWithoutSSO` | Q Q  | `OnlineAppointmentDto` | - | - | - |
| GET | `/patientRegAppt/validateMobileNumber` | `custom.PatientRegApptServiceImpl.validateMobileNumber` | Q HDR  | `boolean` | - | - | - |
| POST | `/patientRegAppt/createAppointment` | `custom.PatientRegApptServiceImpl.addAppointment` | JSON  | `PatientAppointmentandRegDto` | - | - | - |
| POST | `/patientRegAppt/getHealthCardSheet` | `custom.PatientRegApptServiceImpl.getHealthCardSheet` | JSON  | `String` | - | - | - |
| POST | `/patientRegAppt/pdfAppointmentLetterFromAppList` | `custom.PatientRegApptServiceImpl.pdfAppointmentLetterFromAppList` | JSON Q  | `String[]` | - | - | - |
| POST | `/patientRegAppt/validateOTP` | `custom.PatientRegApptServiceImpl.validateOTP` | Q Q Q Q  | `OnlineAppointmentDto` | - | - | - |
| POST | `/patientRegAppt/validateOTPForLoginWithoutSSO` | `custom.PatientRegApptServiceImpl.validateOTPForLoginWithoutSSO` | Q Q Q  | `OnlineAppointmentDto` | - | - | - |
| GET | `/patientTransferDischarge/fetchBedsByWardId` | `custom.PatientTransferDischargeServiceImpl.getBedsList` | Q  | `List<Bedmaster>` | Y | - | - |
| GET | `/patientTransferDischarge/getLeaveDaysDetails` | `custom.PatientTransferDischargeServiceImpl.getLeaveDaysDetails` | Q Q  | `List<TransferDetailsDto>` | Y | - | - |
| GET | `/patientTransferDischarge/markNotReportedAsDischarged` | `custom.PatientTransferDischargeServiceImpl.markNotReportedAsDischarged` | Q Q Q Q  | `Boolean` | Y | - | - |
| GET | `/patientTransferDischarge/showExtPackageDetails` | `custom.PatientTransferDischargeServiceImpl.showExtPackageDetails` | Q Q  | `List<ExtPackageSubscription>` | Y | - | - |
| GET | `/patientTransferDischarge/validatePatientDischarge` | `custom.PatientTransferDischargeServiceImpl.validatePatientDischarge` | Q  | `boolean` | Y | - | - |
| POST | `/patientTransferDischarge/acceptPatientOnLeaveRequest` | `custom.PatientTransferDischargeServiceImpl.acceptPatientOnLeaveRequest` | JSON  | `boolean` | Y | - | - |
| POST | `/patientTransferDischarge/addAbscondDischarge` | `custom.PatientTransferDischargeServiceImpl.addAbscondDischarge` | JSON  | `PatientTransferDischargeView` | Y | - | - |
| POST | `/patientTransferDischarge/addAdvisedDischargeList` | `custom.PatientTransferDischargeServiceImpl.addAdvisedDischargeList` | JSON  | `AdvisedDischargeListDto` | - | - | - |
| POST | `/patientTransferDischarge/allocateBed` | `custom.PatientTransferDischargeServiceImpl.allocateBed` | JSON  | `boolean` | - | - | - |
| POST | `/patientTransferDischarge/cancelAdvisedDischarge` | `custom.PatientTransferDischargeServiceImpl.cancelAdvisedDischarge` | JSON  | `void` | - | - | - |
| POST | `/patientTransferDischarge/cancelDischarge` | `custom.PatientTransferDischargeServiceImpl.cancelDischarge` | Q Q Q Q  | `Long` | Y | - | - |
| POST | `/patientTransferDischarge/cancelInterimTransfer` | `custom.PatientTransferDischargeServiceImpl.cancelInterimTransfer` | Q Q  | `boolean` | - | - | - |
| POST | `/patientTransferDischarge/cancelPatientOnLeaveRequest` | `custom.PatientTransferDischargeServiceImpl.cancelPatientOnLeaveRequest` | Q Q  | `boolean` | Y | - | - |
| POST | `/patientTransferDischarge/changeSponsorDetails` | `custom.PatientTransferDischargeServiceImpl.changeSponsorDetails` | ?  | `ChangePatientSponsorDto` | Y | - | - |
| POST | `/patientTransferDischarge/discharge` | `custom.PatientTransferDischargeServiceImpl.dischargePatient` | JSON  | `PatientTransferDischargeView` | - | - | - |
| POST | `/patientTransferDischarge/generateDischargeReqNO` | `custom.PatientTransferDischargeServiceImpl.generateDischargeReqNO` | Q Q  | `String` | Y | - | - |
| POST | `/patientTransferDischarge/getDischargedPatientList` | `custom.PatientTransferDischargeServiceImpl.getDischargedPatientList` | JSON  | `List<InquiryDetailsResultDto>` | - | - | - |
| POST | `/patientTransferDischarge/getInterimAdmissionsList` | `custom.PatientTransferDischargeServiceImpl.getInterimAdmissionList` | JSON Q Q  | `List<PatientAdmissionListResultDto>` | Y | - | - |
| POST | `/patientTransferDischarge/getMortuaryDischargedPatientList` | `custom.PatientTransferDischargeServiceImpl.getMortuaryDischargedPatientList` | JSON Q  | `List<MortuaryPatientDetialsDto>` | Y | - | - |
| POST | `/patientTransferDischarge/markAsNotReported` | `custom.PatientTransferDischargeServiceImpl.addMarkAsNotReported` | Q Q  | `Boolean` | Y | - | - |
| POST | `/patientTransferDischarge/markPatientOnLeave` | `custom.PatientTransferDischargeServiceImpl.markPatientOnLeave` | JSON  | `PatientTransferDischargeView` | Y | - | - |
| POST | `/patientTransferDischarge/printPatientDeathForm` | `custom.PatientTransferDischargeServiceImpl.printPatientDeathForm` | JSON Q  | `String` | Y | - | - |
| POST | `/patientTransferDischarge/transfer` | `custom.PatientTransferDischargeServiceImpl.transferPatient` | JSON  | `PatientTransferDischargeView` | - | - | - |
| POST | `/patientTransferDischarge/transferBaby` | `custom.PatientTransferDischargeServiceImpl.transferBaby` | JSON  | `PatientTransferDischargeView` | - | - | - |
| POST | `/patientTransferDischarge/transferToPreOtPatient` | `custom.PatientTransferDischargeServiceImpl.transferToPreOtPatient` | JSON  | `PatientTransferDischargeView` | Y | - | - |
| POST | `/patientTransferDischarge/updateDischargeDetails` | `custom.PatientTransferDischargeServiceImpl.updateDischargeDetails` | JSON  | `Boolean` | - | - | - |
| POST | `/patientTransferDischarge/updateExaminedByDoctor` | `custom.PatientTransferDischargeServiceImpl.updateExaminedByDoctor` | Q Q  | `boolean` | Y | - | - |
| POST | `/patientTransferDischarge/updatePatientDischargeStatus` | `custom.PatientTransferDischargeServiceImpl.updatePatientDischargeStatus` | JSON  | `boolean` | - | - | - |
| POST | `/patientTransferDischarge/updatePatientInterimAdmission` | `custom.PatientTransferDischargeServiceImpl.updatePatientInterimAdmission` | Q Q Q  | `boolean` | Y | - | - |
| PUT | `/patientTransferDischarge/releaseBed` | `custom.PatientTransferDischargeServiceImpl.updateBedOccupiedFlag` | JSON  | `BedOccupiedFlagDto` | - | - | - |
| DELETE | `/pharmacy/removePrescription` | `custom.PharmacyServiceImpl.removePrescription` | Q  | `void` | Y | - | - |
| GET | `/pharmacy/checkDispenseStatus` | `custom.PharmacyServiceImpl.checkDispenseStatus` | Q Q Q  | `CheckOrderDispenseAndStock` | Y | - | - |
| GET | `/pharmacy/deleteInterventionById` | `custom.PharmacyServiceImpl.deleteInterventionById` | Q Q  | `void` | - | - | - |
| GET | `/pharmacy/getAllAdmittedPatients` | `custom.PharmacyServiceImpl.getAllAdmittedPatients` | Q  | `List<AdmittedPatientsPharmacyResultDto>` | - | - | - |
| GET | `/pharmacy/getAllUersUnderRole` | `custom.PharmacyServiceImpl.getAllUersUnderRole` | Q Q  | `List<UserForRoleResultDto>` | Y | - | - |
| GET | `/pharmacy/getCartFillQnt` | `custom.PharmacyServiceImpl.getCartFillQnt` | Q  | `String` | - | - | - |
| GET | `/pharmacy/getDetailedMedHistory` | `custom.PharmacyServiceImpl.getDetailedMedHistory` | Q  | `GetPrescriptionWardDetails` | - | - | - |
| GET | `/pharmacy/getDispensedIVOrders` | `custom.PharmacyServiceImpl.getDispensedIVOrders` | Q Q  | `List<PharmacyGetDispensedIvOrdersDto>` | Y | - | - |
| GET | `/pharmacy/getDispensedOrders` | `custom.PharmacyServiceImpl.getDispensedOrders` | Q Q Q  | `List<GetDispsenseOrders>` | - | - | - |
| GET | `/pharmacy/getDoctorDetails` | `custom.PharmacyServiceImpl.getDoctorDetails` | - | `List<Object[]>` | Y | - | - |
| GET | `/pharmacy/getDrugSubstituteByDrugId` | `custom.PharmacyServiceImpl.getDrugSubstituteByDrugId` | Q  | `List<Object[]>` | Y | - | - |
| GET | `/pharmacy/getIntervention` | `custom.PharmacyServiceImpl.getIntervention` | Q  | `List<GetPharmacyInterventionList>` | Y | - | - |
| GET | `/pharmacy/getInterventionOrderById` | `custom.PharmacyServiceImpl.getInterventionOrderById` | Q Q  | `GetPharmacyInterventionList` | Y | - | - |
| GET | `/pharmacy/getIsHighRiskFlag` | `custom.PharmacyServiceImpl.getIsHighRiskFlag` | Q  | `Object[]` | - | - | - |
| GET | `/pharmacy/getItemCategoryItemCode` | `custom.PharmacyServiceImpl.getItemsByCategoryID` | Q  | `List<Object[]>` | - | - | - |
| GET | `/pharmacy/getItemForCompoundDetails` | `custom.PharmacyServiceImpl.getItemForCompoundDetails` | - | `List<ItemsForAddMaintenanceResultDto>` | - | - | - |
| GET | `/pharmacy/getItemSalesIP` | `custom.PharmacyServiceImpl.getItemSalesIP` | Q Q  | `List<GetIPItemSalesDetailsDto>` | - | - | - |
| GET | `/pharmacy/getMedications` | `custom.PharmacyServiceImpl.getMedications` | Q  | `List<ItemSolrView>` | - | - | - |
| GET | `/pharmacy/getMedsAndItemStockList` | `custom.PharmacyServiceImpl.getMedsAndItemStockList` | Q Q  | `List<PatientPrescriptionDetailsDto>` | Y | - | - |
| GET | `/pharmacy/getMedsAndItemStockListForNAItems` | `custom.PharmacyServiceImpl.getMedsAndItemStockListForNAItems` | Q Q  | `List<PatientPrescriptionNAItemDetailsDto>` | - | - | - |
| GET | `/pharmacy/getMedsAndItemStockListOld` | `custom.PharmacyServiceImpl.getMedsAndItemStockListOld` | Q Q Q  | `List<PatientPrescriptionDetailsDto>` | Y | - | - |
| GET | `/pharmacy/getNonMedItemListForNurse` | `custom.PharmacyServiceImpl.getNonMedItemListForNurse` | - | `List<NonMedItemListForNurse>` | - | - | - |
| GET | `/pharmacy/getOpdispensedOrderDetails` | `custom.PharmacyServiceImpl.getOpdispensedOrderDetails` | Q  | `List<OpdispensedOrderDetails>` | Y | - | - |
| GET | `/pharmacy/getOutMedication` | `custom.PharmacyServiceImpl.getOutMedication` | Q Q Q  | `List<PatientPrescriptionDetailsDto>` | - | - | - |
| GET | `/pharmacy/getOutSideDocRegSearch` | `custom.PharmacyServiceImpl.getOutSideDocReg` | Q  | `List<Object[]>` | Y | - | - |
| GET | `/pharmacy/getOutSideVisit` | `custom.PharmacyServiceImpl.getOutSideVisit` | Q Q  | `List<GetOutSideVisit>` | Y | - | - |
| GET | `/pharmacy/getPatienAndVisitDetails` | `custom.PharmacyServiceImpl.getPatienAndVisitDetails` | Q  | `List<Object[]>` | Y | - | - |
| GET | `/pharmacy/getPatientDetailsForPharmacy` | `custom.PharmacyServiceImpl.getPatientDetailsForPharmacy` | Q Q Q  | `PatientPharmacyDetailsDto` | Y | - | - |
| GET | `/pharmacy/getPatientDetailsForPharmacyNewScreen` | `custom.PharmacyServiceImpl.getPatientDetailsForPharmacyNewScreen` | Q Q Q  | `PatientPharmacyDetailsDto` | - | - | - |
| GET | `/pharmacy/getPatientDispensedOrders` | `custom.PharmacyServiceImpl.getPatientDispensedOrders` | Q  | `List<GetPatientDispensedOrders>` | Y | - | - |
| GET | `/pharmacy/getPatientNonMedOrders` | `custom.PharmacyServiceImpl.getPatientNonMedOrders` | Q Q Q  | `List<PatientNonMedOrdersResultDto>` | Y | - | - |
| GET | `/pharmacy/getPatPharmPrescriptionDetails` | `custom.PharmacyServiceImpl.getPrescriptionPharmOrderDetails` | Q Q  | `List<GetPrescriptionPharmOrdersDto>` | - | - | - |
| GET | `/pharmacy/getPharmacyBillingAmount` | `custom.PharmacyServiceImpl.getPharmacyBillingAmount` | Q  | `Number` | Y | - | - |
| GET | `/pharmacy/getPharmacyInterventionReport` | `custom.PharmacyServiceImpl.getPharmacyInterventionReport` | Q  | `List<PharmacyInterventionReportDto>` | - | - | - |
| GET | `/pharmacy/getPrescId` | `custom.PharmacyServiceImpl.getPrescId` | Q  | `void` | - | - | - |
| GET | `/pharmacy/getPrescriptionDetailsToDispence` | `custom.PharmacyServiceImpl.getPrescriptionDetailsToDispence` | Q Q  | `List<PatientPrescriptionDetailsDto>` | - | - | - |
| GET | `/pharmacy/getPreviuosFlagComments` | `custom.PharmacyServiceImpl.getPreviuosFlagComments` | Q  | `String` | Y | - | - |
| GET | `/pharmacy/getSetPharmacyUserSession` | `custom.PharmacyServiceImpl.getSetPharmacyUserSession` | Q Q Q Q  | `String` | - | - | - |
| GET | `/pharmacy/getUploadedPrescriptionDocuments` | `custom.PharmacyServiceImpl.getUploadedPrescriptionDocuments` | Q Q Q  | `List<PatientIDDocumentDto>` | Y | - | - |
| GET | `/pharmacy/getWardByWardGroupId` | `custom.PharmacyServiceImpl.getWardByWardGroupId` | Q  | `List<Wardmaster>` | - | - | - |
| GET | `/pharmacy/getWardPickList` | `custom.PharmacyServiceImpl.getWardPickList` | Q Q Q  | `List<GetWardPickListResultDto>` | Y | - | - |
| GET | `/pharmacy/inactivePrescriptionById` | `custom.PharmacyServiceImpl.inactivePrescriptionById` | Q  | `Boolean` | - | - | - |
| GET | `/pharmacy/itemcategoryAndStoreMeds` | `custom.PharmacyServiceImpl.getItemCategoryAndStoreMeds` | Q  | `ItemCategoryStoreView` | - | - | - |
| GET | `/pharmacy/resetPharmacyUserSession` | `custom.PharmacyServiceImpl.resetPharmacyUserSession` | Q  | `void` | Y | - | - |
| GET | `/pharmacy/resoleveIntervention` | `custom.PharmacyServiceImpl.updateRejectFlagForIntervention` | Q  | `void` | Y | - | - |
| GET | `/pharmacy/updateAcceptedByNurse` | `custom.PharmacyServiceImpl.updateAcceptedByNurse` | Q  | `void` | - | - | - |
| GET | `/pharmacy/updateDiscOrderStatus` | `custom.PharmacyServiceImpl.updateDiscOrderStatus` | Q Q  | `void` | - | - | - |
| GET | `/pharmacy/updateDispensedData` | `custom.PharmacyServiceImpl.updateDispensedData` | Q Q Q  | `void` | - | - | - |
| GET | `/pharmacy/updateMedOrderDetails` | `custom.PharmacyServiceImpl.updateMedOrderDetails` | Q Q Q  | `void` | Y | - | - |
| POST | `/pharmacy/addDrugSubstitute` | `custom.PharmacyServiceImpl.addDrugSubstitute` | JSON  | `void` | - | - | - |
| POST | `/pharmacy/addInfusionMixture` | `custom.PharmacyServiceImpl.addInfusionMixture` | JSON  | `InfusoinMixtureView` | - | - | - |
| POST | `/pharmacy/addIntervention` | `custom.PharmacyServiceImpl.addIntervention` | JSON  | `AddPharmacyInterventionDto` | Y | - | - |
| POST | `/pharmacy/addItemMaster` | `custom.PharmacyServiceImpl.addItemCompoundDetails` | JSON  | `ItemCompoundDetailsView` | - | - | - |
| POST | `/pharmacy/addManufacturedItem` | `custom.PharmacyServiceImpl.addManufacturedItem` | JSON  | `List<AddPharmacyManufacturedItemDto>` | Y | - | - |
| POST | `/pharmacy/addPatientNonMedDetails` | `custom.PharmacyServiceImpl.addPatientNonMedDetails` | JSON  | `void` | Y | - | - |
| POST | `/pharmacy/addPatientPrescOrder` | `custom.PharmacyServiceImpl.addPatientPrescOrder` | JSON  | `List<String>` | Y | - | - |
| POST | `/pharmacy/addPatientProblems` | `custom.PharmacyServiceImpl.addPatientProblems` | JSON  | `void` | - | - | - |
| POST | `/pharmacy/addPrescriptionOrderDetails` | `custom.PharmacyServiceImpl.addPrescriptionOrderDetails` | JSON  | `void` | Y | - | - |
| POST | `/pharmacy/addWardPickList` | `custom.PharmacyServiceImpl.addWardPickList` | JSON  | `List<AddWardPickListDto>` | Y | - | - |
| POST | `/pharmacy/cancelItemSalePrice` | `custom.PharmacyServiceImpl.cancelItemSalePrice` | Q  | `void` | Y | - | - |
| POST | `/pharmacy/cancelItemSalePriceNew` | `custom.PharmacyServiceImpl.cancelItemSalePriceNew` | Q  | `void` | - | - | - |
| POST | `/pharmacy/cartfillItemSale` | `custom.PharmacyServiceImpl.cartfillItemSale` | JSON  | `void` | - | - | - |
| POST | `/pharmacy/createNewPresc` | `custom.PharmacyServiceImpl.createNewPresc` | JSON  | `void` | - | - | - |
| POST | `/pharmacy/getAllInterventionList` | `custom.PharmacyServiceImpl.getAllInterventionList` | Q  | `List<GetPharmacyInterventionList>` | - | - | - |
| POST | `/pharmacy/getDispenseOrderItems` | `custom.PharmacyServiceImpl.getDispenseOrderItems` | Q Q Q  | `List<GetOrderDispenseItemDetails>` | Y | - | - |
| POST | `/pharmacy/getDispensingDetails` | `custom.PharmacyServiceImpl.getDispensingDetails` | Q  | `List<GetDispensingDetails>` | Y | - | - |
| POST | `/pharmacy/getItemCurrentStock` | `custom.PharmacyServiceImpl.getItemCurrentStock` | Q Q Q  | `List<GetPharmItemCurrentStock>` | - | - | - |
| POST | `/pharmacy/getItemForCartfillDispense` | `custom.PharmacyServiceImpl.getItemForCartfillDispense` | Q Q  | `List<GetPharmItemCurrentStock>` | - | - | - |
| POST | `/pharmacy/getIVOrdersList` | `custom.PharmacyServiceImpl.getIVOrdersList` | Q  | `List<GetIVOrdersDetailsDispeneseDto>` | Y | - | - |
| POST | `/pharmacy/getPharmacyCompoundDetailsByPatientId` | `custom.PharmacyServiceImpl.getPharmacyCompoundDetailsByPatientId` | Q Q Q  | `List<GetCompoundOrderDetails>` | - | - | - |
| POST | `/pharmacy/getPharmacyItemCompoundDetails` | `custom.PharmacyServiceImpl.getPharmacyItemCompoundDetails` | Q  | `List<GetItemCompoundDetailsResultDto>` | Y | - | - |
| POST | `/pharmacy/getPharmacyWardDetails` | `custom.PharmacyServiceImpl.getPharmacyWardDetails` | Q Q  | `GetExtInternalPrescriptionView` | - | - | - |
| POST | `/pharmacy/getPharmacyWardDetailsbyPatientid` | `custom.PharmacyServiceImpl.getPharmacyWardDetailsbyPatientid` | Q Q  | `List<GetPrescriptionWardDetails>` | Y | - | - |
| POST | `/pharmacy/getPharmacyWardDetailsDispense` | `custom.PharmacyServiceImpl.getPharmacyWardDetailsDispense` | Q Q  | `List<GetPrescriptionWardDetailsDispense>` | - | - | - |
| POST | `/pharmacy/getPharmacyWardDetailsDispenseByPatientId` | `custom.PharmacyServiceImpl.getPharmacyWardDetailsDispenseByPatientId` | Q Q  | `List<GetPrescriptionWardDetailsDispense>` | Y | - | - |
| POST | `/pharmacy/getPharmacyWardDetailsPartial` | `custom.PharmacyServiceImpl.getPharmacyWardDetailsPartial` | Q Q  | `List<GetPrescriptionWardDetailsDispense>` | - | - | - |
| POST | `/pharmacy/getPharmacyWardDetailsVerified` | `custom.PharmacyServiceImpl.getPharmacyWardDetailsVerified` | Q Q  | `List<GetCompoundOrderDetails>` | Y | - | - |
| POST | `/pharmacy/opPharmacyPatientRegistrationandUpadation` | `custom.PharmacyServiceImpl.registerPatient` | JSON  | `PatientAppointmentandRegDto` | - | - | - |
| POST | `/pharmacy/printBagLabel` | `custom.PharmacyServiceImpl.printBagLabel` | JSON  | `InfusoinMixtureView` | - | - | - |
| POST | `/pharmacy/printCompoundLabel` | `custom.PharmacyServiceImpl.printSingleCompoundLabel` | Q Q Q  | `BillReceiptView` | - | - | - |
| POST | `/pharmacy/printHighBillingPatients` | `custom.PharmacyServiceImpl.printHighBillingPatients` | JSON ?  | `Map<String, Object>` | - | - | - |
| POST | `/pharmacy/printOutOfStockMedDetails` | `custom.PharmacyServiceImpl.printOutOfStockMedDetails` | JSON  | `String` | - | - | - |
| POST | `/pharmacy/printPatientDispenseDetails` | `custom.PharmacyServiceImpl.printPatientDispenseDetails` | JSON  | `String` | - | - | - |
| POST | `/pharmacy/printPatientMedNonMedDetails` | `custom.PharmacyServiceImpl.printPatientMedNonMedDetails` | JSON  | `String` | Y | - | - |
| POST | `/pharmacy/printPatientRxDetails` | `custom.PharmacyServiceImpl.printPatientRxDetails` | JSON  | `String` | - | - | - |
| POST | `/pharmacy/printPharmacyMedLabel` | `custom.PharmacyServiceImpl.printPharmacyMedLabel` | JSON  | `PharmacyLableView` | - | - | - |
| POST | `/pharmacy/setFlagForOrder` | `custom.PharmacyServiceImpl.setFlagForOrder` | Q Q Q  | `void` | - | - | - |
| POST | `/pharmacy/setNurseReturnAck` | `custom.PharmacyServiceImpl.setNurseReturnAck` | Q Q Q  | `void` | - | - | - |
| POST | `/pharmacy/setPatientIen` | `custom.PharmacyServiceImpl.setPatientIen` | Q Q  | `void` | - | - | - |
| POST | `/pharmacy/setVerifiedFlag` | `custom.PharmacyServiceImpl.setVerifiedFlag` | Q Q Q  | `void` | - | - | - |
| POST | `/pharmacy/syncJantaMedDispenseOrders` | `custom.PharmacyServiceImpl.syncJantaMedDispenseOrders` | JSON  | `List<Long>` | - | - | - |
| POST | `/pharmacy/updateCompoundStatus` | `custom.PharmacyServiceImpl.updateCompoundStatus` | Q Q Q  | `void` | Y | - | - |
| POST | `/pharmacy/updateDispenseFlagStockforOP` | `custom.PharmacyServiceImpl.updateDispenseFlagStockforOP` | Q  | `void` | Y | - | - |
| POST | `/pharmacy/updateInterventionById` | `custom.PharmacyServiceImpl.updateInterventionById` | JSON  | `void` | Y | - | - |
| POST | `/pharmacy/updateItemSaleforDispense` | `custom.PharmacyServiceImpl.updateItemSaleforDispense` | JSON  | `GenericResultDto` | Y | - | - |
| POST | `/pharmacy/updateItemStock` | `custom.PharmacyServiceImpl.updateItemStock` | JSON  | `ItemCompoundDetailsView` | Y | - | - |
| POST | `/pharmacy/updateRejectFlagForIntervention` | `custom.PharmacyServiceImpl.updateRejectFlagForIntervention` | Q  | `void` | - | - | - |
| POST | `/pharmacy/updateVerifyFlagOP` | `custom.PharmacyServiceImpl.updateVerifyFlagOP` | Q  | `void` | Y | - | - |
| POST | `/pharmacy/updateVerifyFlagOPAutoDispense` | `custom.PharmacyServiceImpl.updateVerifyFlagOPAutoDispense` | Q  | `void` | Y | - | - |
| POST | `/pharmacy/uploadPrescription` | `custom.PharmacyServiceImpl.uploadPrescription` | JSON  | `PatientIDDocumentDto` | Y | - | - |
| POST | `/pharmacycommonAppAndMedicineDispensing` | `custom.PharmacyServiceImpl.commonAppAndMedicineDispensing` | JSON HDR  | `void` | - | - | - |
| POST | `/pharmacygetSingleItemBatchListForPharmacy` | `custom.PharmacyServiceImpl.getSingleItemBatchListForPharmacy` | JSON  | `List<ItemBatchListForSearchwithTaxResultDto>` | - | - | - |
| GET | `/prescription/getPrescriptionsByFamliyNoAndMemberId` | `custom.PrescriptionDetailsServiceImpl.getPrescriptionsByFamliyNoAndMemberId` | Q Q  | `List<PatientDetailsForPrescriptionDto>` | - | - | - |
| GET | `/questionnaireService/getAllQuestionAndAnswers` | `custom.QuetionnaireServiceImpl.getAllQuestionAndAnswers` | Q Q Q  | `List<QuestionAnswerDto>` | - | - | - |
| GET | `/questionnaireService/getAllQuestionAndAnswersForPatient` | `custom.QuetionnaireServiceImpl.getAllQuestions` | - | `QuestionAnswerView` | Y | - | - |
| POST | `/questionnaireService/addUserAnswers` | `custom.QuetionnaireServiceImpl.addUserAnswers` | JSON Q Q  | `void` | - | - | - |
| POST | `/refundBillReceipt/getRefundReceipt` | `custom.RefundBillReceiptServiceImpl.getAdvance` | JSON  | `List<CompanyAdvanceRefundReceiptResultDto>` | - | - | - |
| POST | `/uploadFile/{mastername}` | `custom.UploadFileServiceImpl.upload` | HDR Q  | `List<String>` | - | - | - |
| POST | `/uploadFile/servicemapping` | `custom.UploadFileServiceImpl.serviceMasterMapping` | Q ?  | `List<Servicemaster>` | Y | - | - |
| POST | `/uploadFile/serviceMappingTo` | `custom.UploadFileServiceImpl.serviceMasterMappingTo` | JSON  | `List<Servicemaster>` | Y | - | - |
| POST | `/uploadFile/serviceMappingUpload` | `custom.UploadFileServiceImpl.serviceMasterMappingUpload` | JSON  | `List<String>` | Y | - | - |
| POST | `/upload/{mastername}` | `custom.UploadHelperServiceImpl.upload` | HDR Q ? P  | `List<String>` | - | - | - |

### Module: CWF (Clinical Workflow)  (17 endpoints)

| Method | URL (under /api/rest) | Class : method | Request | Response | Tx | Tables | Notes |
|---|---|---|---|---|---|
| DELETE | `/cwf/deleteCWFMapping` | `CWFServiceImpl.deleteCWFMapping` | Q  | `void` | - | - | - |
| DELETE | `/cwf/deleteSCWFMapping` | `CWFServiceImpl.deleteSCWFMapping` | Q  | `void` | - | - | - |
| GET | `/cwf/getCWFDetails/{id}` | `CWFServiceImpl.getCWFDetails` | P  | `CWFMapping` | Y | - | - |
| GET | `/cwf/getCWFDetailsByDiagId/{id}` | `CWFServiceImpl.getCWFDetailsByDiagId` | P  | `SCWFMappingDTO` | - | - | - |
| GET | `/cwf/getCWFDetailsByDiagIdw/{id}` | `CWFServiceImpl.getCWFDetailsByDiagIdw` | P  | `SCWFMapping` | - | - | - |
| GET | `/cwf/getCWFDiagnosisByComplaints` | `CWFServiceImpl.getCWFDetails` | Q  | `Map<Long, String>` | - | - | - |
| GET | `/cwf/getListOfCWFMapping` | `CWFServiceImpl.getListOfCWFMapping` | Q  | `List<CWFMapping>` | - | - | - |
| GET | `/cwf/getListOfSCWFMapping` | `CWFServiceImpl.getListOfSCWFMapping` | Q Q  | `List<SCWFMapping>` | Y | - | - |
| GET | `/cwf/getSCWFDetails/{id}/{specialityId}` | `CWFServiceImpl.getSCWFDetails` | P P  | `List<SCWFMapping>` | - | - | - |
| GET | `/cwf/getSCWFDetailsByHospitalType` | `CWFServiceImpl.getSCWFDetailsByHospitalType` | Q  | `SCWFMappingDTO` | Y | - | - |
| POST | `/cwf/addCWFMapping` | `CWFServiceImpl.addCWFMapping` | JSON  | `CWFMapping` | - | - | - |
| POST | `/cwf/addSCWFMapping` | `CWFServiceImpl.addSCWFMapping` | JSON  | `SCWFMapping` | - | - | - |
| POST | `/cwf/consolidatedLookup` | `CWFServiceImpl.consolidatedLookup` | ?  | `Map<Long, String>` | Y | - | - |
| POST | `/cwf/getSCWFDetailsFull/{id}/{doctorId}` | `CWFServiceImpl.getSCWFDetailsFull` | P P ?  | `SCWFMappingDTO` | - | - | - |
| POST | `/cwf/saveTempPrescription` | `CWFServiceImpl.saveTempPrescription` | Q ?  | `void` | Y | - | - |
| POST | `/cwf/updateCWFMapping` | `CWFServiceImpl.updateCWFMapping` | JSON  | `CWFMapping` | Y | - | - |
| POST | `/cwf/updateSCWFMapping` | `CWFServiceImpl.updateSCWFMapping` | JSON  | `SCWFMapping` | Y | - | - |

### Module: EHR (SSO/Sync)  (8 endpoints)

| Method | URL (under /api/rest) | Class : method | Request | Response | Tx | Tables | Notes |
|---|---|---|---|---|---|
| GET | `/ehr/fetchDepartments/{unitId}/{consultType}` | `ehr.EHRUtilServiceImpl.fetchDepartmentsByUnit` | P P  | `List<Departmentmaster>` | - | - | - |
| GET | `/ehr/findSSOUserDetails` | `ehr.EHRUtilServiceImpl.` | Q Q  | `EhrSyncView` | - | - | - |
| GET | `/ehr/getAllPlatformUnitCount` | `ehr.EHRUtilServiceImpl.` | Q  | `EhrSyncView` | Y | - | - |
| GET | `/ehr/getAllPlatformUnits` | `ehr.EHRUtilServiceImpl.` | Q Q  | `EhrSyncView` | - | - | - |
| GET | `/ehr/handleSyncReq` | `ehr.EHRUtilServiceImpl.` | Q Q  | `EhrSyncView` | - | - | - |
| POST | `/ehr/createVisit` | `ehr.EHRUtilServiceImpl.createVisit` | JSON  | `PatientAppointmentandRegDto` | - | - | - |
| POST | `/ehr/fetchAppointmentSchedule` | `ehr.EHRUtilServiceImpl.getAppointmentScheduleServiceNew` | Q  | `Map<java.util.Date, Map<String, List<AppointmentSlot>>>` | - | - | - |
| POST | `/ehr/fetchAvailableDepartments` | `ehr.EHRUtilServiceImpl.fetchAvailableDepartments` | ?  | `List<Departmentmaster>` | - | - | - |

### Module: EJS (ASHA/Outreach)  (125 endpoints)

| Method | URL (under /api/rest) | Class : method | Request | Response | Tx | Tables | Notes |
|---|---|---|---|---|---|
| GET | `/ejs-ancpncchild/ancsahyogsearch` | `ejs.EjsAncPncChildServiceImpl.getAncSearchSahyog` | Q Q Q  | `EjsResponseDto<EjsSupervisorAncDto>` | - | - | - |
| GET | `/ejs-ancpncchild/ancsearch` | `ejs.EjsAncPncChildServiceImpl.ancSearch` | Q Q  | `EjsResponseDto<EjsAnc>` | - | - | - |
| GET | `/ejs-ancpncchild/ancvisitssahyogsearch` | `ejs.EjsAncPncChildServiceImpl.getAncVisitsSearchSahyog` | Q Q Q  | `EjsResponseDto<EjsAncVisitsDto>` | - | - | - |
| GET | `/ejs-ancpncchild/ancvisitssearch` | `ejs.EjsAncPncChildServiceImpl.ancVisitsSearch` | Q Q  | `EjsResponseDto<EjsAncVisits>` | Y | - | - |
| GET | `/ejs-ancpncchild/childcarecheckupsearch` | `ejs.EjsAncPncChildServiceImpl.childCareCheckupSearch` | Q Q  | `EjsResponseDto<EjsChildCareCheckup>` | Y | - | - |
| GET | `/ejs-ancpncchild/childcarecountsahyogsearch` | `ejs.EjsAncPncChildServiceImpl.getChildCareCountSahyog` | Q Q Q  | `EjsResponseDto<EjsSupervisorChildDto>` | - | - | - |
| GET | `/ejs-ancpncchild/childcaresahyogsearch` | `ejs.EjsAncPncChildServiceImpl.getChildCareSahyog` | Q Q Q  | `EjsResponseDto<EjsSupervisorChildDto>` | - | - | - |
| GET | `/ejs-ancpncchild/childcaresearch` | `ejs.EjsAncPncChildServiceImpl.searchChildCare` | Q Q  | `EjsResponseDto<EjsChildCare>` | Y | - | - |
| GET | `/ejs-ancpncchild/childcountimmunsahyogsearch` | `ejs.EjsAncPncChildServiceImpl.getChildCareCountImmuneSahyog` | Q Q  | `EjsResponseDto<EjsSupervisorChildDto>` | - | - | - |
| GET | `/ejs-ancpncchild/ecssahyogsearch` | `ejs.EjsAncPncChildServiceImpl.getEcsSearchSahyog` | Q Q Q  | `EjsResponseDto<EjsEcs>` | - | - | - |
| GET | `/ejs-ancpncchild/pncsahyogsearch` | `ejs.EjsAncPncChildServiceImpl.getPncSearchSahyog` | Q Q Q  | `EjsResponseDto<EjsSupervisorPncDto>` | - | - | - |
| GET | `/ejs-ancpncchild/pncsearch` | `ejs.EjsAncPncChildServiceImpl.pncSearch` | Q Q  | `EjsResponseDto<EjsPnc>` | Y | - | - |
| GET | `/ejs-ancpncchild/pncvisitssahyogsearch` | `ejs.EjsAncPncChildServiceImpl.getPncVisitsSearchSahyog` | Q Q Q  | `EjsResponseDto<EjsPncVisitsDto>` | - | - | - |
| GET | `/ejs-ancpncchild/pncvisitssearch` | `ejs.EjsAncPncChildServiceImpl.pncVisitsSearch` | Q Q  | `EjsResponseDto<EjsPncVisits>` | Y | - | - |
| POST | `/ejs-ancpncchild/ancdata` | `ejs.EjsAncPncChildServiceImpl.ancData` | JSON  | `List<ResponseEjs>` | - | - | - |
| POST | `/ejs-ancpncchild/ancvisitsdata` | `ejs.EjsAncPncChildServiceImpl.ancVisitsData` | JSON  | `List<ResponseEjs>` | - | - | - |
| POST | `/ejs-ancpncchild/childcarecheckupdata` | `ejs.EjsAncPncChildServiceImpl.childCareCheckupData` | JSON  | `List<ResponseEjs>` | - | - | - |
| POST | `/ejs-ancpncchild/childcaredata` | `ejs.EjsAncPncChildServiceImpl.childCareData` | JSON  | `List<ResponseEjs>` | - | - | - |
| POST | `/ejs-ancpncchild/pncdata` | `ejs.EjsAncPncChildServiceImpl.pncData` | JSON  | `List<ResponseEjs>` | - | - | - |
| POST | `/ejs-ancpncchild/pncvisitsdata` | `ejs.EjsAncPncChildServiceImpl.pncVisitsData` | JSON  | `List<ResponseEjs>` | - | - | - |
| GET | `/ejs-asha/ashamotivationsearch` | `ejs.EjsAshaServiceImpl.ashaMotivationSearch` | Q Q  | `EjsResponseDto<EjsAshaMotivation>` | Y | - | - |
| GET | `/ejs-asha/ashaworksearch` | `ejs.EjsAshaServiceImpl.ashaWorkSearch` | Q Q  | `EjsResponseDto<EjsAshaWork>` | - | - | - |
| GET | `/ejs-asha/attendancesearch` | `ejs.EjsAshaServiceImpl.attendanceSearch` | Q Q  | `EjsResponseDto<EjsAttendance>` | Y | - | - |
| GET | `/ejs-asha/incentivessearch` | `ejs.EjsAshaServiceImpl.incentivesSearch` | Q Q  | `EjsResponseDto<EjsIncentives>` | Y | - | - |
| GET | `/ejs-asha/userdesignationsearch` | `ejs.EjsAshaServiceImpl.userDesignationSearch` | Q Q  | `EjsResponseDto<EjsUserDesignationDto>` | - | - | - |
| POST | `/ejs-asha/ashamotivationdata` | `ejs.EjsAshaServiceImpl.ashaMotivationData` | JSON  | `List<BigInteger>` | - | - | - |
| POST | `/ejs-asha/ashaworkdata` | `ejs.EjsAshaServiceImpl.ashaWorkData` | JSON  | `List<ResponseEjs>` | - | - | - |
| POST | `/ejs-asha/attendancedata` | `ejs.EjsAshaServiceImpl.attendanceData` | JSON  | `List<ResponseEjs>` | - | - | - |
| GET | `/ejs-checklists/checklistquesanswersgetfeedbacks` | `ejs.EjsChecklistsServiceImpl.checklistQuesAnswersGetFeedbacks` | Q Q  | `EjsResponseDto<EjsChecklistQuesAnswersDto>` | Y | - | - |
| GET | `/ejs-checklists/checklistquesanswerssearch` | `ejs.EjsChecklistsServiceImpl.checklistQuesAnswersSearch` | Q Q  | `EjsResponseDto<EjsChecklistQuesAnswers>` | Y | - | - |
| GET | `/ejs-checklists/checklistquestionssearch` | `ejs.EjsChecklistsServiceImpl.checklistQuestionsSearch` | Q Q  | `EjsResponseDto<EjsChecklistQuestions>` | - | - | - |
| GET | `/ejs-checklists/checklistssearch` | `ejs.EjsChecklistsServiceImpl.checklistsSearch` | Q Q  | `EjsResponseDto<EjsChecklists>` | - | - | - |
| GET | `/ejs-checklists/checklistsubmissionssearch` | `ejs.EjsChecklistsServiceImpl.checklistSubmissionsSearch` | Q Q  | `EjsResponseDto<EjsChecklistsSubmissions>` | - | - | - |
| GET | `/ejs-checklists/submissionsinfo` | `ejs.EjsChecklistsServiceImpl.submissionsInfo` | Q Q  | `EjsChecklistsSubmissionsDto` | - | - | - |
| POST | `/ejs-checklists/checklistquesanswersdata` | `ejs.EjsChecklistsServiceImpl.checklistQuesAnswersata` | JSON  | `List<ResponseEjs>` | - | - | - |
| POST | `/ejs-checklists/checklistsubmissionsdata` | `ejs.EjsChecklistsServiceImpl.checklistSubmissionsData` | JSON  | `List<ResponseEjs>` | - | - | - |
| DELETE | `/ejs-details/deleteMediaById` | `ejs.EjsDetailsServiceImpl.deleteMediaById` | Q  | `void` | Y | - | - |
| GET | `/ejs-details/abortiondetailssearch` | `ejs.EjsDetailsServiceImpl.abortionDetailsSearch` | Q Q  | `EjsResponseDto<EjsAbortionDetails>` | Y | - | - |
| GET | `/ejs-details/cbacsearch` | `ejs.EjsDetailsServiceImpl.cbacSearch` | Q Q  | `EjsResponseDto<EjsCbac>` | Y | - | - |
| GET | `/ejs-details/deathdetailsinfo` | `ejs.EjsDetailsServiceImpl.deathDetailsInfo` | Q Q  | `EjsDeathDetailsInfoDto` | - | - | - |
| GET | `/ejs-details/deathdetailssearch` | `ejs.EjsDetailsServiceImpl.deathDetailsSearch` | Q Q  | `EjsResponseDto<EjsDeathDetails>` | Y | - | - |
| GET | `/ejs-details/diseasessearch` | `ejs.EjsDetailsServiceImpl.diseasesSearch` | Q Q  | `EjsResponseDto<EjsDisease>` | Y | - | - |
| GET | `/ejs-details/downloadMedia` | `ejs.EjsDetailsServiceImpl.dowloadMedia` | Q ?  | `void` | - | - | - |
| GET | `/ejs-details/ecssearch` | `ejs.EjsDetailsServiceImpl.ecsSearch` | Q Q  | `EjsResponseDto<EjsEcs>` | Y | - | - |
| GET | `/ejs-details/familyawcdetailssearch` | `ejs.EjsDetailsServiceImpl.familyAwcDetailsSearch` | Q Q  | `EjsResponseDto<EjsFamilyAwcDetails>` | Y | - | - |
| GET | `/ejs-details/familydetailssearch` | `ejs.EjsDetailsServiceImpl.getFamilyDetails` | Q Q  | `EjsResponseDto<EjsFamilyDetails>` | Y | - | - |
| GET | `/ejs-details/familyplanningsearch` | `ejs.EjsDetailsServiceImpl.familyPlanningSearch` | Q Q  | `EjsResponseDto<EjsFamilyPlanning>` | Y | - | - |
| GET | `/ejs-details/getAllEjsChecklistQuestions` | `ejs.EjsDetailsServiceImpl.getAllEjsChecklistQuestions` | Q Q Q  | `List<EjsChecklistQuestions>` | Y | - | - |
| GET | `/ejs-details/getAllEjsmasteractivity` | `ejs.EjsDetailsServiceImpl.getAllEjsmasteractivity` | Q Q Q  | `List<EjsMasterActivity>` | - | - | - |
| GET | `/ejs-details/getAllEjsmasterentity` | `ejs.EjsDetailsServiceImpl.getAllEjsmasterentity` | Q Q Q  | `List<EjsMasterEntity>` | Y | - | - |
| GET | `/ejs-details/getEjsAWCDetails` | `ejs.EjsDetailsServiceImpl.getEjsAWCDetails` | Q Q Q  | `List<EjsMasterAwcs>` | - | - | - |
| GET | `/ejs-details/getEjsSubcenter` | `ejs.EjsDetailsServiceImpl.getEjsSubcenter` | Q Q  | `List<EjsMasterSubcenters>` | - | - | - |
| GET | `/ejs-details/getEjsUserTypes` | `ejs.EjsDetailsServiceImpl.getEjsUserTypes` | Q Q  | `List<EjsUserTypes>` | - | - | - |
| GET | `/ejs-details/getEjsVillage` | `ejs.EjsDetailsServiceImpl.getEjsVillage` | Q Q  | `List<EjsMasterVillages>` | - | - | - |
| GET | `/ejs-details/helpsearch` | `ejs.EjsDetailsServiceImpl.helpSearch` | Q Q  | `EjsResponseDto<EjsHelp>` | Y | - | - |
| GET | `/ejs-details/indicatorsinfo` | `ejs.EjsDetailsServiceImpl.indicatorsInfo` | Q Q Q  | `EjsIndicatorsInfoDto` | Y | - | - |
| GET | `/ejs-details/mcdetailssearch` | `ejs.EjsDetailsServiceImpl.mcdetailsSearch` | Q Q  | `EjsResponseDto<EjsMcDetails>` | Y | - | - |
| GET | `/ejs-details/mchnsearch` | `ejs.EjsDetailsServiceImpl.mchnSearch` | Q Q  | `EjsResponseDto<EjsMchn>` | Y | - | - |
| GET | `/ejs-details/mediasearch` | `ejs.EjsDetailsServiceImpl.mediaSearch` | Q  | `List<MediaDto>` | Y | - | - |
| GET | `/ejs-details/mediasearchejan` | `ejs.EjsDetailsServiceImpl.getAllMedia` | Q  | `List<Media>` | - | - | - |
| GET | `/ejs-details/memberssearch` | `ejs.EjsDetailsServiceImpl.membersSearch` | Q Q  | `EjsResponseDto<EjsMembers>` | - | - | - |
| GET | `/ejs-details/referdetailssearch` | `ejs.EjsDetailsServiceImpl.referDetailsSearch` | Q Q  | `EjsResponseDto<EjsReferDetails>` | Y | - | - |
| GET | `/ejs-details/schooldetailssearch` | `ejs.EjsDetailsServiceImpl.schoolDetailsSearch` | Q Q  | `EjsResponseDto<EjsSchoolDetails>` | Y | - | - |
| GET | `/ejs-details/workdetailssearch` | `ejs.EjsDetailsServiceImpl.workDetailsSearch` | Q Q  | `EjsResponseDto<EjsWorkDetails>` | Y | - | - |
| GET | `/ejs-detailsgetPatOutreachData/{patientId}` | `ejs.EjsDetailsServiceImpl.getPatOutreachData` | P  | `OutReachDataView` | Y | - | - |
| POST | `/ejs-details/abortiondetailsdata` | `ejs.EjsDetailsServiceImpl.abortionDetailsData` | JSON  | `List<ResponseEjs>` | - | - | - |
| POST | `/ejs-details/cbacdata` | `ejs.EjsDetailsServiceImpl.cbacData` | JSON  | `List<ResponseEjs>` | - | - | - |
| POST | `/ejs-details/coronaFamilyDetails` | `ejs.EjsDetailsServiceImpl.coronaFamilyDetails` | JSON  | `List<BigInteger>` | - | - | - |
| POST | `/ejs-details/coronaMembersDetails` | `ejs.EjsDetailsServiceImpl.coronaMembersDetails` | JSON  | `List<BigInteger>` | Y | - | - |
| POST | `/ejs-details/deathdetailsdata` | `ejs.EjsDetailsServiceImpl.deathDetailsData` | JSON  | `List<ResponseEjs>` | - | - | - |
| POST | `/ejs-details/diseasesdata` | `ejs.EjsDetailsServiceImpl.diseasesData` | JSON  | `List<ResponseEjs>` | - | - | - |
| POST | `/ejs-details/ecsdata` | `ejs.EjsDetailsServiceImpl.ecsData` | JSON  | `List<ResponseEjs>` | - | - | - |
| POST | `/ejs-details/familyawcdetailsdata` | `ejs.EjsDetailsServiceImpl.familyAwcDetailsData` | JSON  | `List<BigInteger>` | - | - | - |
| POST | `/ejs-details/familydetailsdata` | `ejs.EjsDetailsServiceImpl.familyDetailsData` | JSON  | `List<ResponseEjs>` | - | - | - |
| POST | `/ejs-details/familyplanningdata` | `ejs.EjsDetailsServiceImpl.familyPlanningData` | JSON  | `List<ResponseEjs>` | - | - | - |
| POST | `/ejs-details/helpdata` | `ejs.EjsDetailsServiceImpl.helpData` | JSON  | `List<ResponseEjs>` | - | - | - |
| POST | `/ejs-details/mapEjsChecklistQuestionsActivity` | `ejs.EjsDetailsServiceImpl.mapEjsChecklistQuestionsActivity` | Q Q  | `boolean` | - | - | - |
| POST | `/ejs-details/mcdetailsdata` | `ejs.EjsDetailsServiceImpl.mcdetailsData` | JSON  | `List<BigInteger>` | - | - | - |
| POST | `/ejs-details/mchndata` | `ejs.EjsDetailsServiceImpl.mchnData` | JSON  | `List<BigInteger>` | - | - | - |
| POST | `/ejs-details/membersdata` | `ejs.EjsDetailsServiceImpl.membersData` | JSON  | `List<ResponseEjs>` | - | - | - |
| POST | `/ejs-details/migrationdetailsdata` | `ejs.EjsDetailsServiceImpl.migrationDetailsData` | JSON  | `void` | Y | - | - |
| POST | `/ejs-details/referdetailsdata` | `ejs.EjsDetailsServiceImpl.referDetailsData` | JSON  | `List<ResponseEjs>` | - | - | - |
| POST | `/ejs-details/saveEjsmasteractivity` | `ejs.EjsDetailsServiceImpl.saveEjsmasteractivity` | JSON  | `EjsMasterActivity` | - | - | - |
| POST | `/ejs-details/saveEjsmasterentity` | `ejs.EjsDetailsServiceImpl.saveEjsmasterentity` | JSON  | `EjsMasterEntity` | - | - | - |
| POST | `/ejs-details/savemedia` | `ejs.EjsDetailsServiceImpl.saveMedia` | ? Q Q Q  | `void` | - | - | - |
| POST | `/ejs-details/schooldetailsdata` | `ejs.EjsDetailsServiceImpl.schoolDetailsData` | JSON  | `List<ResponseEjs>` | - | - | - |
| POST | `/ejs-details/uploadphotochild` | `ejs.EjsDetailsServiceImpl.uploadPhotoChild` | ?  | `Map<String, String>` | Y | - | - |
| POST | `/ejs-details/uploadphotoecs` | `ejs.EjsDetailsServiceImpl.uploadPhotoEcs` | ?  | `Map<String, String>` | Y | - | - |
| POST | `/ejs-details/uploadphotopnc` | `ejs.EjsDetailsServiceImpl.uploadPhotoPnc` | ?  | `Map<String, String>` | Y | - | - |
| POST | `/ejs-details/workdetailsdata` | `ejs.EjsDetailsServiceImpl.workDetailsData` | JSON  | `List<ResponseEjs>` | - | - | - |
| GET | `/ejs-incentive/calculatePendingIncentives` | `ejs.EjsIncentiveServiceImpl.calculatePendingIncentives` | - | `void` | - | - | - |
| GET | `/ejs-incentive/getAshaIncentiveInfo` | `ejs.EjsIncentiveServiceImpl.getAshaIncentiveInfo` | Q Q Q  | `EjsClaimDetailDto` | Y | - | - |
| GET | `/ejs-incentive/getAshaWorkIncentiveByActivity` | `ejs.EjsIncentiveServiceImpl.getAshaWorkIncentiveByActivity` | Q  | `List<EjsAshaIncentiveInfoDto>` | - | - | - |
| GET | `/ejs-incentive/reSubmitIncentiveCalculation/{ashaWorkId}` | `ejs.EjsIncentiveServiceImpl.reSubmitIncentiveCalculation` | P  | `boolean` | Y | - | - |
| GET | `/ejs-incentive/reSubmitIncentivePaymentTransfer/{paymentId}` | `ejs.EjsIncentiveServiceImpl.reSubmitIncentivePaymentTransfer` | P  | `boolean` | Y | - | - |
| POST | `/ejs-incentive/getIncentiveandClaimDetails` | `ejs.EjsIncentiveServiceImpl.getIncentiveandClaimDetails` | JSON Q Q  | `List<IncentiveClaimDetailsDto>` | - | - | - |
| GET | `/ejs-masterLocation/awcs` | `ejs.EjsMasterLocationServiceImpl.awcs` | Q Q Q  | `EjsResponseDto<EjsMasterAwcs>` | - | - | - |
| GET | `/ejs-masterLocation/blocks` | `ejs.EjsMasterLocationServiceImpl.blocks` | Q Q Q  | `EjsResponseDto<EjsMasterBlocks>` | - | - | - |
| GET | `/ejs-masterLocation/districts` | `ejs.EjsMasterLocationServiceImpl.getDistricts` | Q Q Q  | `EjsResponseDto<EjsMasterDistricts>` | - | - | - |
| GET | `/ejs-masterLocation/grampanchayats` | `ejs.EjsMasterLocationServiceImpl.grampanchayats` | Q Q Q  | `EjsResponseDto<EjsMasterGrampanchayats>` | - | - | - |
| GET | `/ejs-masterLocation/phcs` | `ejs.EjsMasterLocationServiceImpl.phcs` | Q Q Q  | `EjsResponseDto<EjsMasterPhcs>` | - | - | - |
| GET | `/ejs-masterLocation/subcenters` | `ejs.EjsMasterLocationServiceImpl.subcenters` | Q Q Q  | `EjsResponseDto<EjsMasterSubcenters>` | - | - | - |
| GET | `/ejs-masterLocation/villages` | `ejs.EjsMasterLocationServiceImpl.villages` | Q Q Q  | `EjsResponseDto<EjsMasterVillages>` | - | - | - |
| GET | `/ejs-masterLocation/zones` | `ejs.EjsMasterLocationServiceImpl.zones` | Q Q  | `EjsResponseDto<EjsMasterZones>` | - | - | - |
| GET | `/ejs-population/count` | `ejs.EjsPopulationServiceImpl.getCount` | Q Q Q  | `EjsPopulationCountDto` | Y | - | - |
| GET | `/ejs-population/countage` | `ejs.EjsPopulationServiceImpl.getAgeWisePopulation` | Q Q Q  | `EjsAgePopulationDto` | - | - | - |
| GET | `/ejs-population/countanc` | `ejs.EjsPopulationServiceImpl.getAncCount` | Q Q Q  | `EjsPopulationCountAncDto` | - | - | - |
| GET | `/ejs-population/countchild` | `ejs.EjsPopulationServiceImpl.getChildCount` | Q Q Q  | `EjsPopulationCountChildDto` | - | - | - |
| GET | `/ejs-population/countchildimmun` | `ejs.EjsPopulationServiceImpl.getChildImmunCount` | Q Q Q  | `EjsPopulationCountChildImmunDto` | - | - | - |
| GET | `/ejs-population/countecs` | `ejs.EjsPopulationServiceImpl.getEcsCount` | Q Q Q  | `EjsPopulationCountEcsDto` | - | - | - |
| GET | `/ejs-population/countpnc` | `ejs.EjsPopulationServiceImpl.getPncCount` | Q Q Q  | `EjsPopulationCountPncDto` | - | - | - |
| GET | `/ejs-population/masterawcspopulation` | `ejs.EjsPopulationServiceImpl.getMasterAwcsPopulation` | Q Q  | `Map<String, BigInteger>` | - | - | - |
| POST | `/ejs-population/masterawcsdata` | `ejs.EjsPopulationServiceImpl.masterAwcsData` | JSON  | `List<BigInteger>` | - | - | - |
| GET | `/ejs-supervisor/ancsearchbysupervisor` | `ejs.EjsSupervisorServiceImpl.ancSearchBySupervisor` | Q Q  | `EjsResponseDto<EjsSupervisorAncDto>` | - | - | - |
| GET | `/ejs-supervisor/childcaresearchbysupervisor` | `ejs.EjsSupervisorServiceImpl.childCareSearchBySupervisor` | Q Q  | `EjsResponseDto<EjsSupervisorChildDto>` | - | - | - |
| GET | `/ejs-supervisor/ecssearchbysupervisor` | `ejs.EjsSupervisorServiceImpl.searchEcsBySupervisor` | Q Q  | `EjsResponseDto<EjsEcs>` | - | - | - |
| GET | `/ejs-supervisor/memberssearchbysupervisor` | `ejs.EjsSupervisorServiceImpl.membersSearchBySupervisor` | Q Q  | `EjsResponseDto<EjsMembers>` | - | - | - |
| GET | `/ejs-supervisor/pncsearchbysupervisor` | `ejs.EjsSupervisorServiceImpl.pncSearchBySupervisor` | Q Q  | `EjsResponseDto<EjsSupervisorPncDto>` | - | - | - |
| GET | `/ejs-supervisor/visitssearch` | `ejs.EjsSupervisorServiceImpl.visitsSearch` | Q  | `EjsResponseDto<EjsSupervisorVisits>` | - | - | - |
| POST | `/ejs-supervisor/ancdata` | `ejs.EjsSupervisorServiceImpl.ancData` | JSON  | `List<BigInteger>` | Y | - | - |
| POST | `/ejs-supervisor/childdata` | `ejs.EjsSupervisorServiceImpl.childData` | JSON  | `List<BigInteger>` | Y | - | - |
| POST | `/ejs-supervisor/ecsdata` | `ejs.EjsSupervisorServiceImpl.ecsData` | JSON  | `List<BigInteger>` | Y | - | - |
| POST | `/ejs-supervisor/membersdata` | `ejs.EjsSupervisorServiceImpl.membersData` | JSON  | `List<BigInteger>` | Y | - | - |
| POST | `/ejs-supervisor/pncdata` | `ejs.EjsSupervisorServiceImpl.pncData` | JSON  | `List<BigInteger>` | Y | - | - |
| POST | `/ejs-supervisor/visitsdata` | `ejs.EjsSupervisorServiceImpl.visitsData` | JSON  | `List<BigInteger>` | - | - | - |

### Module: File Management  (24 endpoints)

| Method | URL (under /api/rest) | Class : method | Request | Response | Tx | Tables | Notes |
|---|---|---|---|---|---|
| GET | `/fileService/downloadActualFile` | `FileManagementServiceImpl.getActualDownload` | Q ?  | `void` | - | - | - |
| GET | `/fileService/downloadFile` | `FileManagementServiceImpl.getDownload` | HDR Q  | `String` | Y | - | - |
| GET | `/fileService/getAllHospitalLocation` | `FileManagementServiceImpl.getAllHospitalLocation` | - | `List<HospitalGeoLocationMapping>` | Y | - | - |
| GET | `/fileService/getFilesAndDocumentsByCategory` | `FileManagementServiceImpl.getFileDocumentByCategory` | Q Q  | `FileCategoriesAndDocumentDto` | - | - | - |
| GET | `/fileService/getFilesAndDocumentsByCategoryAndDocumentType` | `FileManagementServiceImpl.getFilesAndDocumentsByCategoryAndDocumentType` | Q Q  | `FileCategoriesAndDocumentDto` | - | - | - |
| GET | `/fileService/getImageAnnotationList` | `FileManagementServiceImpl.getImageAnnotationList` | Q  | `List<ImageAnnotationMaster>` | - | - | - |
| POST | `/fileService/addDocumentCategory` | `FileManagementServiceImpl.addDocumentCategory` | JSON  | `FileStore` | - | - | - |
| POST | `/fileService/deleteDocument` | `FileManagementServiceImpl.deleteDocument` | Q Q Q  | `void` | - | - | - |
| POST | `/fileService/deleteDocumentCategory` | `FileManagementServiceImpl.deleteDocumentCategory` | Q  | `FileStore` | Y | - | - |
| POST | `/fileService/fileUpload` | `FileManagementServiceImpl.fileUpload` | Q ? Q  | `List<StorageRequest>` | - | - | - |
| POST | `/fileService/getAllFileCategories` | `FileManagementServiceImpl.getAllFileCategory` | - | `List<FileStore>` | - | - | - |
| POST | `/fileService/getChildFileCategoriesAndDocument` | `FileManagementServiceImpl.getChildFileAndDocument` | Q  | `List<FileCategoriesAndDocumentDto>` | - | - | - |
| POST | `/fileService/getFileCategories` | `FileManagementServiceImpl.getFileCategory` | Q  | `List<FileStore>` | Y | - | - |
| POST | `/fileService/getFileCategoryTree` | `FileManagementServiceImpl.getFileCategoryTree` | Q  | `List<FileCategoriesAndDocumentDto>` | - | - | - |
| POST | `/fileService/getHospitalLocationById` | `FileManagementServiceImpl.getHospitalLocationById` | Q  | `HospitalGeoLocationMapping` | - | - | - |
| POST | `/fileService/getImageToAnnotate` | `FileManagementServiceImpl.getImageToAnnotate` | Q  | `ImageAnnotationMaster` | - | - | - |
| POST | `/fileService/getPatientDocuments` | `FileManagementServiceImpl.getPatientDocuments` | JSON  | `List<PatientDocuments>` | Y | - | - |
| POST | `/fileService/renameDocumentCategory1` | `FileManagementServiceImpl.renameDocumentCategory` | Q Q  | `FileStore` | Y | - | - |
| POST | `/fileService/savePatientDocuments` | `FileManagementServiceImpl.savePatientDocuments` | JSON  | `Boolean` | Y | - | - |
| POST | `/fileService/uploadFileThroughMachine` | `FileManagementServiceImpl.uploadFileThroughMachine` | Q ? Q  | `Boolean` | Y | - | - |
| POST | `/fileService/uploadHospitalLocation` | `FileManagementServiceImpl.uploadHospitalLocation` | ? Q Q  | `HospitalGeoLocationMapping` | Y | - | - |
| POST | `/fileService/uploadHospitalLocation` | `FileManagementServiceImpl.uploadHospitalLocation` | ?  | `HospitalGeoLocationMapping` | Y | - | - |
| POST | `/fileService/uploadImageAnnotationMaster` | `FileManagementServiceImpl.uploadImageAnnotationMaster` | ? Q Q  | `ImageAnnotationMaster` | - | - | - |
| POST | `/fileService/viewDocument` | `FileManagementServiceImpl.viewDocument` | Q  | `FileDetails` | - | - | - |

### Module: Integrations (RGHS/NMC/eRaktKosh/eUpkaran/ABDM)  (22 endpoints)

| Method | URL (under /api/rest) | Class : method | Request | Response | Tx | Tables | Notes |
|---|---|---|---|---|---|
| GET | `/integration/checkAadharNoByRefAadharNo` | `IntegrationServiceImpl.checkAadharNoByRefAadharNoApi` | Q  | `String` | Y | - | - |
| GET | `/integration/consent/{patientId}/{token}` | `IntegrationServiceImpl.handleConsent` | P P  | `Boolean` | Y | - | - |
| GET | `/integration/getCurrentlyAdmittedPatientsByInsScheme` | `IntegrationServiceImpl.getCurrentlyAdmittedPatientsByInsScheme` | HDR Q  | `CurrentlyAdmittedPatientForSchemeResponse` | - | - | - |
| GET | `/integration/getEraktkoshRequest` | `IntegrationServiceImpl.getEraktkoshRequest` | - | `List<EraktkoshRequest>` | Y | - | - |
| GET | `/integration/getOpdIpdData` | `IntegrationServiceImpl.getOpdIpdData` | HDR HDR HDR  | `NMCDashboardOpdIpdDto` | - | - | - |
| GET | `/integration/getTokenPrint/{transactionId}` | `IntegrationServiceImpl.getTokenPrint` | P  | `Map<String, String>` | - | - | - |
| GET | `/integration/sendOtpToJanaadhaarMobileNo` | `IntegrationServiceImpl.sendOtpToJanaadhaarMobileNo` | Q Q Q  | `String` | Y | - | - |
| POST | `/integration/aadhaarAuthenticate` | `IntegrationServiceImpl.aadhaarAuthenticate` | JSON  | `AadhaarAuthResponse` | - | - | - |
| POST | `/integration/addeRaktkoshDonation` | `IntegrationServiceImpl.addeRaktkoshDonation` | JSON Q  | `void` | Y | - | - |
| POST | `/integration/addPHCClinicalData` | `IntegrationServiceImpl.addPHCClinicalData` | JSON HDR  | `UpdateExtPackageResponseDto` | Y | - | - |
| POST | `/integration/addRghsPackages` | `IntegrationServiceImpl.addRghsPackages` | JSON  | `BillInfoDto` | - | - | - |
| POST | `/integration/addTokenPrintObject` | `IntegrationServiceImpl.addTokenPrintObject` | JSON ?  | `String` | - | - | - |
| POST | `/integration/checkRGHSEligiblity` | `IntegrationServiceImpl.checkRGHSEligiblity` | JSON  | `EligibilityCheck` | - | - | - |
| POST | `/integration/decryptResponse` | `IntegrationServiceImpl.validateResponse` | JSON  | `String` | - | - | - |
| POST | `/integration/eUpkaranItemStockDetails` | `IntegrationServiceImpl.eUpkaranItemStockDetails` | JSON HDR  | `UpdateExtPackageResponseDto` | Y | - | - |
| POST | `/integration/generateEncryptedData` | `IntegrationServiceImpl.generateEncryptedData` | JSON  | `String[]` | Y | - | - |
| POST | `/integration/pushEmergencyCaseDetails` | `IntegrationServiceImpl.getEmergencyCaseDetails` | HDR JSON  | `UpdateExtPackageResponseDto` | - | - | - |
| POST | `/integration/tokenEndpointforDataSyncing` | `IntegrationServiceImpl.tokenEndpointforDataSyncing` | JSON  | `NMCDashboardTokenResponseDto` | - | - | - |
| POST | `/integration/updateExtPackages` | `IntegrationServiceImpl.updateExtPackages` | JSON HDR  | `UpdateExtPackageResponseDto` | - | - | - |
| POST | `/integration/uploadRghsPackageDocuments` | `IntegrationServiceImpl.uploadRghsPackageDocuments` | Q Q  | `Boolean` | Y | - | - |
| POST | `/integration/verifyOtpForJanaadhaarMobileNo` | `IntegrationServiceImpl.verifyOtpForJanaadhaarMobileNo` | Q Q Q  | `OnlineAppointmentDto` | - | - | - |
| POST | `/integration/whatsAppMessageStatusUpdate` | `IntegrationServiceImpl.whatsAppMessageStatusUpdateTrigger` | HDR JSON  | `UpdateExtPackageResponseDto` | Y | - | - |

### Module: Linen & Laundry  (14 endpoints)

| Method | URL (under /api/rest) | Class : method | Request | Response | Tx | Tables | Notes |
|---|---|---|---|---|---|
| GET | `/linenLaundry/approveLinenRequest` | `LinenLaundryServiceImpl.approveLinenRequest` | Q Q  | `Boolean` | Y | - | - |
| GET | `/linenLaundry/deleteLinenRequest` | `LinenLaundryServiceImpl.deleteLinenRequest` | Q Q  | `Boolean` | - | - | - |
| GET | `/linenLaundry/getItemsForLinenRequest` | `LinenLaundryServiceImpl.getItemsForLinenRequest` | Q  | `List<ItemByNameResultDto>` | - | - | - |
| GET | `/linenLaundry/getLinenItemDetails` | `LinenLaundryServiceImpl.getLinenItemDetails` | Q Q  | `List<LinenRequestDetails>` | Y | - | - |
| GET | `/linenLaundry/getLinenItemDetailsForReport` | `LinenLaundryServiceImpl.getLinenItemDetailsForReport` | Q Q  | `List<LinenRequestDetails>` | Y | - | - |
| GET | `/linenLaundry/handOverLinenItems` | `LinenLaundryServiceImpl.handOverLinenItems` | Q Q Q  | `String` | Y | - | - |
| GET | `/linenLaundry/processLinenRequest` | `LinenLaundryServiceImpl.processLinenRequest` | Q Q  | `Boolean` | Y | - | - |
| POST | `/linenLaundry/addNewLinenRequest` | `LinenLaundryServiceImpl.addNewLinenRequest` | JSON  | `LinenRequest` | Y | - | - |
| POST | `/linenLaundry/dispatchLinenRequest` | `LinenLaundryServiceImpl.dispatchLinenRequest` | JSON  | `void` | Y | - | - |
| POST | `/linenLaundry/getCurrentLinenStatus` | `LinenLaundryServiceImpl.getCurrentLinenStatus` | JSON Q Q  | `List<LinenRequest>` | - | - | - |
| POST | `/linenLaundry/getLinenRequestCount` | `LinenLaundryServiceImpl.getLinenRequestCount` | JSON  | `Map<String, Long>` | - | - | - |
| POST | `/linenLaundry/receiveLinenRequest` | `LinenLaundryServiceImpl.receiveLinenRequest` | JSON  | `void` | Y | - | - |
| POST | `/linenLaundry/receiveLinenRequestMainStore` | `LinenLaundryServiceImpl.receiveLinenRequestMainStore` | JSON  | `void` | Y | - | - |
| POST | `/linenLaundry/updateItemsForLinenRequest` | `LinenLaundryServiceImpl.updateItemsForLinenRequest` | JSON  | `void` | Y | - | - |

### Module: Notifications  (5 endpoints)

| Method | URL (under /api/rest) | Class : method | Request | Response | Tx | Tables | Notes |
|---|---|---|---|---|---|
| GET | `/sendEmail/sendForgotEmail` | `SendNotificationServiceImpl.sendPasswordForgotMail` | Q  | `void` | - | - | - |
| POST | `/sendEmail/sendFinalBillEmail` | `SendNotificationServiceImpl.sendFinalBillEmailApi` | ?  | `Boolean` | Y | - | - |
| POST | `/sendEmail/sendPatientCritcalMail` | `SendNotificationServiceImpl.sendPatientCritcalMail` | ?  | `void` | - | - | - |
| POST | `/sendEmail/sendPatientNoteMail` | `SendNotificationServiceImpl.sendPatientNoteMail` | JSON  | `void` | - | - | - |
| POST | `/sendEmail/validateToken` | `SendNotificationServiceImpl.resetPassword` | ?  | `TokenValidateDto` | Y | - | - |

### Module: Nursing  (300 endpoints)

| Method | URL (under /api/rest) | Class : method | Request | Response | Tx | Tables | Notes |
|---|---|---|---|---|---|
| DELETE | `/nursingList/deleteNote` | `nurse.NursingServiceImpl.deleteNote` | Q  | `boolean` | Y | - | - |
| DELETE | `/nursingList/deleteNursePatientAllocation` | `nurse.NursingServiceImpl.deleteNursePatientAllocation` | JSON  | `Integer` | - | - | - |
| DELETE | `/nursingList/deleteNursePatientAllocationOnShiftHandOver` | `nurse.NursingServiceImpl.deleteNursePatientAllocationOnShiftHandOver` | JSON  | `Integer` | Y | - | - |
| DELETE | `/nursingList/deleteService` | `nurse.NursingServiceImpl.deleteService` | Q Q  | `boolean` | Y | - | - |
| DELETE | `/nursingList/removeMappedDiagnosis` | `nurse.NursingServiceImpl.removeMappedDiagnosis` | Q  | `Boolean` | Y | - | - |
| GET | `/nursingList/ancChartConfiguration` | `nurse.NursingServiceImpl.ancChartConfiguration` | Q  | `JSONArray` | Y | - | - |
| GET | `/nursingList/assignBodyToHimSelf` | `nurse.NursingServiceImpl.assignBodyToHimSelf` | Q Q Q  | `Boolean` | - | - | - |
| GET | `/nursingList/assignNurseToTask` | `nurse.NursingServiceImpl.assignNurseToTask` | Q Q  | `Long` | - | - | - |
| GET | `/nursingList/checkEctsNumberIsExist` | `nurse.NursingServiceImpl.checkEctsNumberIsExist` | Q  | `Boolean` | - | - | - |
| GET | `/nursingList/checkedInPatientOnBed` | `nurse.NursingServiceImpl.checkInAdmitedPatient` | Q  | `Boolean` | Y | - | - |
| GET | `/nursingList/checkFamilyPlanningCompleted` | `nurse.NursingServiceImpl.checkFamilyPlanningCompleted` | Q  | `Boolean` | Y | - | - |
| GET | `/nursingList/checkIsTsuEncounterClosed` | `nurse.NursingServiceImpl.checkIsTsuEncounterClosed` | Q  | `boolean` | - | - | - |
| GET | `/nursingList/checkNurseAllocationForPatient` | `nurse.NursingServiceImpl.checkNurseAllocationForPatient` | Q  | `Boolean` | Y | - | - |
| GET | `/nursingList/checkNurseAllocationForShift` | `nurse.NursingServiceImpl.checkNurseAllocationForShift` | Q  | `Boolean` | - | - | - |
| GET | `/nursingList/checkNurseAllocationForShiftCnt` | `nurse.NursingServiceImpl.checkNurseAllocationForShiftCnt` | Q  | `Boolean` | - | - | - |
| GET | `/nursingList/checkNurseShiftTime` | `nurse.NursingServiceImpl.checkNurseShiftTime` | Q Q  | `Boolean` | Y | - | - |
| GET | `/nursingList/checkNurseWardAllocation` | `nurse.NursingServiceImpl.checkNurseWardAllocation` | Q Q  | `String` | - | - | - |
| GET | `/nursingList/checkoutEncounterVisit` | `nurse.NursingServiceImpl.checkoutEncounterVisit` | Q Q  | `Boolean` | - | - | - |
| GET | `/nursingList/checkVaccineInterval` | `nurse.NursingServiceImpl.checkVaccineInterval` | Q Q  | `Boolean` | - | - | - |
| GET | `/nursingList/checkValidSign` | `nurse.NursingServiceImpl.checkValidSign` | Q  | `boolean` | Y | - | - |
| GET | `/nursingList/deleteCapturedMachineVitals` | `nurse.NursingServiceImpl.deleteCapturedMachineVitals` | Q  | `List<MachineVitals>` | - | - | - |
| GET | `/nursingList/deleteNurseNote` | `nurse.NursingServiceImpl.deleteNurseNote` | Q  | `boolean` | - | - | - |
| GET | `/nursingList/deletePatientAdvice` | `nurse.NursingServiceImpl.deletePatientAdvice` | Q  | `void` | Y | - | - |
| GET | `/nursingList/deletePatientAllergies` | `nurse.NursingServiceImpl.deletePatientAllergies` | Q  | `void` | Y | - | - |
| GET | `/nursingList/deletePatientComplaint` | `nurse.NursingServiceImpl.deletePatientComplaint` | Q  | `void` | - | - | - |
| GET | `/nursingList/deletePatientDiagnosis` | `nurse.NursingServiceImpl.deletePatientDiagnosis` | Q Q  | `void` | Y | - | - |
| GET | `/nursingList/deletePatientNote` | `nurse.NursingServiceImpl.deletePatientNote` | Q  | `Boolean` | - | - | - |
| GET | `/nursingList/deleteVitals` | `nurse.NursingServiceImpl.deleteVitals` | Q Q  | `void` | - | - | - |
| GET | `/nursingList/discontinueNursingOrders` | `nurse.NursingServiceImpl.discontinueNursingOrders` | Q  | `Boolean` | - | - | - |
| GET | `/nursingList/doctorAvailableSlots` | `nurse.NursingServiceImpl.doctorAvailableSlots` | Q  | `List<Doctorschedule>` | Y | - | - |
| GET | `/nursingList/getAllANCVisitByPatientId` | `nurse.NursingServiceImpl.getAllANCVisitByPatientId` | Q  | `JSONObject` | - | - | - |
| GET | `/nursingList/getAllClinicalService` | `nurse.NursingServiceImpl.getAllClinicalService` | Q Q Q  | `List<ClinicalOrders>` | - | - | - |
| GET | `/nursingList/getAllEmptyBedsByWardId` | `nurse.NursingServiceImpl.getAllEmptyBedsByWardId` | Q  | `List<Bedmaster>` | Y | - | - |
| GET | `/nursingList/getAllIncidentTypes` | `nurse.NursingServiceImpl.getAllIncidentTypes` | - | `Map<String, List<IncidentTypeMaster>>` | - | - | - |
| GET | `/nursingList/getAllocatedAlliedStaffByClinicId` | `nurse.NursingServiceImpl.getAllocatedAlliedStaffByClinicId` |  | `?` | - | - | - |
| GET | `/nursingList/getAllocatedAlliedStaffByWardId` | `nurse.NursingServiceImpl.getAllocatedAlliedStaffByWardId` | Q  | `List<NurseWardAllocationDetailsDto>` | - | - | - |
| GET | `/nursingList/getAllocatedClincsByNurseId` | `nurse.NursingServiceImpl.getAllocatedClincsByNurseId` |  | `?` | Y | - | - |
| GET | `/nursingList/getAllocatedNursesByClinicId` | `nurse.NursingServiceImpl.getAllocatedNursesByClinicId` | Q  | `List<NurseClinicAllocationDetailsDto>` | - | - | - |
| GET | `/nursingList/getAllocatedNursesByDepartmentId` | `nurse.NursingServiceImpl.getAllocatedNursesByDepartmentId` |  | `?` | - | - | - |
| GET | `/nursingList/getAllocatedNursesByPatientId` | `nurse.NursingServiceImpl.getAllocatedNursesByPatientId` | Q  | `List<NurseShiftDetailsResultDto>` | - | - | - |
| GET | `/nursingList/getAllocatedNursesByWardId` | `nurse.NursingServiceImpl.getAllocatedNursesByWardId` | Q  | `List<NurseWardAllocationDetailsDto>` | - | - | - |
| GET | `/nursingList/getAllocatedWardDetails` | `nurse.NursingServiceImpl.getAllocatedWardDetails` | Q  | `List<NurseWardAllocationDetailsDto>` | Y | - | - |
| GET | `/nursingList/getAllPatientAdvice` | `nurse.NursingServiceImpl.getAllPatientAdvice` | Q  | `List<Patientadvice>` | - | - | - |
| GET | `/nursingList/getAllPatientComplaint` | `nurse.NursingServiceImpl.getAllPatientComplaint` | Q Q  | `List<PatientComplaints>` | - | - | - |
| GET | `/nursingList/getAllVitalUnits` | `nurse.NursingServiceImpl.getAllVitalUnits` | - | `List<VitalUnitDetailsDto>` | - | - | - |
| GET | `/nursingList/getANCData` | `nurse.NursingServiceImpl.getANCData` | Q  | `JSONObject` | Y | - | - |
| GET | `/nursingList/getAvailableCradleCountByWardId` | `nurse.NursingServiceImpl.getAvailableCradleCountByWardId` | Q  | `Long` | - | - | - |
| GET | `/nursingList/getAvailableCradles` | `nurse.NursingServiceImpl.getAvailableCradles` | - | `Number` | - | - | - |
| GET | `/nursingList/getBedManagementDetails` | `nurse.NursingServiceImpl.getClassMasterDetails` | Q  | `List<PatientBedDetailsResultDto>` | Y | - | - |
| GET | `/nursingList/getCallRecords` | `nurse.NursingServiceImpl.getCallRecordDetails` | Q  | `List<TeleConsultantDetails>` | Y | - | - |
| GET | `/nursingList/getCapturedMachineVitals` | `nurse.NursingServiceImpl.getCapturedMachineVitals` | Q  | `List<MachineVitals>` | - | - | - |
| GET | `/nursingList/getChildDeathReviewByPatient` | `nurse.NursingServiceImpl.getChildDeathReviewByPatient` | Q  | `ChildDeathReview` | Y | - | - |
| GET | `/nursingList/getClinicByDoctor` | `nurse.NursingServiceImpl.getClinicByDoctor` | Q  | `Object[]` | Y | - | - |
| GET | `/nursingList/getComplaintsBySiteId` | `nurse.NursingServiceImpl.getComplaintsBySiteId` | Q  | `List<DiagnosisMaster>` | - | - | - |
| GET | `/nursingList/getCWFDetails` | `nurse.NursingServiceImpl.getCWFDetails` | Q Q Q  | `PatientMiniDetailsView` | - | - | - |
| GET | `/nursingList/getDeliveryKitStatus` | `nurse.NursingServiceImpl.getDeliveryKitStatus` | Q Q  | `Boolean` | Y | - | - |
| GET | `/nursingList/getDepartmentDoctorDetails` | `nurse.NursingServiceImpl.getDepartmentDoctorDetails` | Q  | `List<Doctordepartmentdetails>` | - | - | - |
| GET | `/nursingList/getDepartmentRelatedDoctors` | `nurse.NursingServiceImpl.getDepartmentRelatedDoctors` |  | `?` | Y | - | - |
| GET | `/nursingList/getDiagnosisBySpecialityId` | `nurse.NursingServiceImpl.getDiagnosisBySpecialityId` | Q  | `List<DiagnosisMaster>` | - | - | - |
| GET | `/nursingList/getDischageFormTicketandVmByDeptId` | `nurse.NursingServiceImpl.getDischageFormTicketandVmByDeptId` | Q  | `DepartmentDischargeDetails` | - | - | - |
| GET | `/nursingList/getDischargeSummaryForDischargedPat` | `nurse.NursingServiceImpl.getDischargeSummaryForDischargedPat` | Q Q  | `PatientDischargeData` | Y | - | - |
| GET | `/nursingList/getEctsNumberForPat` | `nurse.NursingServiceImpl.getEctsNumberForPat` | Q  | `String` | - | - | - |
| GET | `/nursingList/getEDDByLMP` | `nurse.NursingServiceImpl.getEDDByLMPStatus` | Q Q  | `List<String>` | - | - | - |
| GET | `/nursingList/getFamilyPlanningChecklist` | `nurse.NursingServiceImpl.getFamilyPlanningChecklist` | Q  | `List<Procedurechecklistitems>` | - | - | - |
| GET | `/nursingList/getFreqAllergies` | `nurse.NursingServiceImpl.getFreqAllergies` | Q  | `List<PatientAllergies>` | - | - | - |
| GET | `/nursingList/getFrequentItems` | `nurse.NursingServiceImpl.getFrequentItems` | Q Q  | `List<FrequentItemDto>` | - | - | - |
| GET | `/nursingList/getGrowthChartByParentId` | `nurse.NursingServiceImpl.getGrowthChart` | Q  | `List<GrowthChartMenu>` | - | - | - |
| GET | `/nursingList/getGrowthChartDetails` | `nurse.NursingServiceImpl.getGrowthChartDetails` | Q  | `List<Growthchartdetails>` | - | - | - |
| GET | `/nursingList/getGrowthChartMenu` | `nurse.NursingServiceImpl.getGrowthChartMenu` | - | `List<GrowthChartMenu>` | Y | - | - |
| GET | `/nursingList/getGrowthChartValue` | `nurse.NursingServiceImpl.getGrowthChartValue` | Q  | `Map<String, Object>` | - | - | - |
| GET | `/nursingList/getImmunizationDetails` | `nurse.NursingServiceImpl.getImmunizationDetails` | Q  | `List<Immunizationagegroupmaster>` | - | - | - |
| GET | `/nursingList/getInActivePatientComplaint` | `nurse.NursingServiceImpl.getInActivePatientComplaint` | Q  | `List<PatientComplaints>` | Y | - | - |
| GET | `/nursingList/getIsMLCDetails` | `nurse.NursingServiceImpl.getIsMLCDetails` | Q Q  | `Boolean` | - | - | - |
| GET | `/nursingList/getIsMLCDetailsForDoctor` | `nurse.NursingServiceImpl.getIsMLCDetailsForDoctor` | Q Q  | `Boolean` | - | - | - |
| GET | `/nursingList/getItemCurrentStockByBatchCode` | `nurse.NursingServiceImpl.getItemCurrentStockByBatchCode` | Q  | `List<GetBcmaItemCurrentStock>` | - | - | - |
| GET | `/nursingList/getItemsByTherapeuticClassId` | `nurse.NursingServiceImpl.getItemsByTherapeuticClassId` | Q  | `List<Itemmaster>` | - | - | - |
| GET | `/nursingList/getLabAndRadOrderDetailsByQuery` | `nurse.NursingServiceImpl.getLabAndRadOrderDetails` | Q  | `PatientLabRadDetailsView` | - | - | - |
| GET | `/nursingList/getLabTestCollectionDetails` | `nurse.NursingServiceImpl.getLabTestCollectionDetails` |  | `?` | - | - | - |
| GET | `/nursingList/getLangForPatient` | `nurse.NursingServiceImpl.getLangForPatient` | Q  | `LanguagePreferedMasterDto` | - | - | - |
| GET | `/nursingList/getLastCallRecord` | `nurse.NursingServiceImpl.getLastCallRecord` | Q  | `TeleConsultantDetails` | - | - | - |
| GET | `/nursingList/getLastRecordedPatientVisit` | `nurse.NursingServiceImpl.getLastRecordedPatientVisit` | Q Q  | `Long` | - | - | - |
| GET | `/nursingList/getMlcDetailsValue` | `nurse.NursingServiceImpl.getMlcDetailsValue` | Q Q  | `Number` | - | - | - |
| GET | `/nursingList/getmoleculeMasterDetailsById` | `nurse.NursingServiceImpl.getmoleculeMasterDetails` | Q  | `String` | Y | - | - |
| GET | `/nursingList/getNoteDetailsByNoteTitle` | `nurse.NursingServiceImpl.getNoteDetailsByNoteTitle` | Q  | `PatientNote` | - | - | - |
| GET | `/nursingList/getNurseCarerListByUnitId` | `nurse.NursingServiceImpl.getNurseCarerListByUnitId` | Q  | `List<NurseListResultDto>` | - | - | - |
| GET | `/nursingList/getNurseCarerTaskDetailsCount` | `nurse.NursingServiceImpl.getNurseCarerTaskDetailsCount` | Q  | `Map<String, Integer>` | Y | - | - |
| GET | `/nursingList/getNurseListByUnitIdAndAllocatedPatientId` | `nurse.NursingServiceImpl.getNurseListByUnitIdAndAllocatedPatientId` |  | `?` | - | - | - |
| GET | `/nursingList/getNurseListByUnitIdAndAllocatedWardId` | `nurse.NursingServiceImpl.getNurseListByUnitIdAndWardId` | Q  | `List<NurseShiftDetailsResultDto>` | - | - | - |
| GET | `/nursingList/getNurseNote` | `nurse.NursingServiceImpl.getNurseNote` | - | `List<NurseNote>` | Y | - | - |
| GET | `/nursingList/getNurseNoteById` | `nurse.NursingServiceImpl.getNurseNoteById` | Q  | `NurseNote` | Y | - | - |
| GET | `/nursingList/getNurseWards` | `nurse.NursingServiceImpl.getNurseWards` | Q  | `List<NurseWardListResultDto>` | - | - | - |
| GET | `/nursingList/getOrderSetById` | `nurse.NursingServiceImpl.getOrderSetById` | Q  | `Orderset` | Y | - | - |
| GET | `/nursingList/getOrderSetDetails` | `nurse.NursingServiceImpl.getOrderSetDetails` | Q  | `Ordersetdefinationmaster` | - | - | - |
| GET | `/nursingList/getOrderSetMasterList` | `nurse.NursingServiceImpl.getOrderSetMasterList` | - | `List<Ordersetdefinationmaster>` | - | - | - |
| GET | `/nursingList/getPatDetailsForFpvReg` | `nurse.NursingServiceImpl.getPatDetailsForEctsReg` | Q  | `PatientSpouseDetails` | - | - | - |
| GET | `/nursingList/getPatDetailsForNonEligibleCertificate` | `nurse.NursingServiceImpl.getPatDetailsForNonEligibleCertificate` | Q  | `PatientAppointmentandRegDto` | - | - | - |
| GET | `/nursingList/getPatient` | `nurse.NursingServiceImpl.getPatient` | Q  | `List<SearchPatientGeneralDetailsResultDto>` | - | - | - |
| GET | `/nursingList/getPatientAddendumNote` | `nurse.NursingServiceImpl.getPatientAddendumNote` | Q  | `PatientNote` | - | - | - |
| GET | `/nursingList/getPatientAllergies` | `nurse.NursingServiceImpl.getPatientAllergies` | Q  | `List<PatientAllergies>` | - | - | - |
| GET | `/nursingList/getPatientAllergiesByPatientId` | `nurse.NursingServiceImpl.getPatientAllergiesByPatientIdApi` | Q  | `List<PatientAllergies>` | - | - | - |
| GET | `/nursingList/getPatientAquity` | `nurse.NursingServiceImpl.getPatientAquity` | Q Q  | `Number` | Y | - | - |
| GET | `/nursingList/getPatientComplaint` | `nurse.NursingServiceImpl.getPatientComplaint` | Q  | `List<PatientComplaints>` | Y | - | - |
| GET | `/nursingList/getPatientComplaintDetailsById` | `nurse.NursingServiceImpl.getPatientComplaintDetailsById` | Q  | `PatientComplaints` | - | - | - |
| GET | `/nursingList/getPatientCurrentVisitClinicalData` | `nurse.NursingServiceImpl.getPatientCurrentVisitClinicalData` | Q Q  | `PatientClinicalDataView` | - | - | - |
| GET | `/nursingList/getPatientDetailDateWise` | `nurse.NursingServiceImpl.getPatientDetailDateWise` | Q  | `List<CustomBedManagementResultDto>` | Y | - | - |
| GET | `/nursingList/getPatientDetailsByPatientIen` | `nurse.NursingServiceImpl.getPatientDetailByPatientIen` | Q  | `GetPatientDetailsBcmaView` | Y | - | - |
| GET | `/nursingList/getPatientDetailsForDemographics` | `nurse.NursingServiceImpl.getPatientDetails` | Q  | `PatientDemographicDetails` | Y | - | - |
| GET | `/nursingList/getPatientDetailsListWardWise` | `nurse.NursingServiceImpl.getPatientDetailsListWardWise` | Q  | `PatientDetailsListWardWiseView` | Y | - | - |
| GET | `/nursingList/getPatientDetailsListWardWiseDetails` | `nurse.NursingServiceImpl.getPatientDetailsListWardWiseDetails` |  | `?` | Y | - | - |
| GET | `/nursingList/getPatientDiagnosis` | `nurse.NursingServiceImpl.getPatientDiagnosisApi` | Q  | `List<PatientDiagnosisDetails>` | Y | - | - |
| GET | `/nursingList/getPatientDtoMrno` | `nurse.NursingServiceImpl.getPatientDtoMrno` | Q Q  | `GetPatientResultDto_1` | Y | - | - |
| GET | `/nursingList/getPatientEDOD` | `nurse.NursingServiceImpl.getPatientEDOD` | Q  | `Timestamp` | Y | - | - |
| GET | `/nursingList/getPatientEncounter` | `nurse.NursingServiceImpl.getPatientEncounter` | Q  | `Encounter` | Y | - | - |
| GET | `/nursingList/getPatientEncounterById` | `nurse.NursingServiceImpl.getPatientEncounterById` | Q  | `Encounter` | - | - | - |
| GET | `/nursingList/getPatientFlagDetails` | `nurse.NursingServiceImpl.getPatientFlagDetails` | Q  | `List<PatientFlagDetails>` | Y | - | - |
| GET | `/nursingList/getPatientImmunizationDetails` | `nurse.NursingServiceImpl.getPatientImmunizationDetails` | Q  | `List<PatientImmunizationDto>` | Y | - | - |
| GET | `/nursingList/getPatientListByWardId` | `nurse.NursingServiceImpl.getPatientListByWardId` | Q  | `List<PatientListByWardIdResultDto>` | - | - | - |
| GET | `/nursingList/getPatientmarkAsExpiredDetails` | `nurse.NursingServiceImpl.getPatientmarkAsExpiredDetails` | Q  | `DepartmentDischargeDetails` | - | - | - |
| GET | `/nursingList/getPatientMLCDetails` | `nurse.NursingServiceImpl.getPatientMLCDetails` | Q Q  | `MLCDetails` | - | - | - |
| GET | `/nursingList/getPatientNote` | `nurse.NursingServiceImpl.getPatientNote` | Q  | `List<PatientNote>` | Y | - | - |
| GET | `/nursingList/getPatientNote/{id}` | `nurse.NursingServiceImpl.getPatientNoteById` | P  | `PatientNote` | - | - | - |
| GET | `/nursingList/getPatientNoteByPatNoteIen` | `nurse.NursingServiceImpl.getPatientNoteByPatNoteIen` | Q  | `PatientNote` | - | - | - |
| GET | `/nursingList/getPatientPodDate` | `nurse.NursingServiceImpl.getPatientDetailsList` | Q  | `List<GetpatientPodDateDto>` | Y | - | - |
| GET | `/nursingList/getPatientPrescriptionByPatientId` | `nurse.NursingServiceImpl.getPatientPrescriptionByPatientId` |  | `?` | - | - | - |
| GET | `/nursingList/getPatientPresction` | `nurse.NursingServiceImpl.getPatientPresction` | - | `List<PatientPrescptionNote>` | Y | - | - |
| GET | `/nursingList/getPatientRabiesImmunizationDetails` | `nurse.NursingServiceImpl.getPatientRabiesImmunizationDetails` | Q Q  | `PatientNote` | Y | - | - |
| GET | `/nursingList/getPatientVitalDetails` | `nurse.NursingServiceImpl.getPatientVitalDetails` | Q  | `List<PatientVitals>` | - | - | - |
| GET | `/nursingList/getPrescriptionPharmOrderDetails` | `nurse.NursingServiceImpl.findPrescriptionPharmOrderDetails` | Q Q Q  | `List<GetPrescriptionPharmOrdersDto>` | - | - | - |
| GET | `/nursingList/getPrevMedication` | `nurse.NursingServiceImpl.getPrevMedication` | Q  | `List<GetPrevPharmOrderPerscription>` | Y | - | - |
| GET | `/nursingList/getProcedureDetailsByGet` | `nurse.NursingServiceImpl.getProcedureDetails` | Q  | `Map<String, Object>` | - | - | - |
| GET | `/nursingList/getRemarksSCWF` | `nurse.NursingServiceImpl.getRemarksSCWF` | Q Q Q  | `String` | Y | - | - |
| GET | `/nursingList/getRoleNursePatientListTotalCount` | `nurse.NursingServiceImpl.getTotalCount` | Q Q  | `Number` | - | - | - |
| GET | `/nursingList/getScore` | `nurse.NursingServiceImpl.getScore` | Q Q  | `String` | - | - | - |
| GET | `/nursingList/getTabletPrescription` | `nurse.NursingServiceImpl.generateTabletPresciptionApi` | Q Q  | `PatCaseSheetView` | - | - | - |
| GET | `/nursingList/getTabletPrescriptionDMP` | `nurse.NursingServiceImpl.getTabletPrescriptionDMP` | Q Q  | `Map<String,Object>` | Y | - | - |
| GET | `/nursingList/getTempPrescriptionNote` | `nurse.NursingServiceImpl.getTempPrescriptionNoteById` | Q Q  | `SCWFMappingDTO` | - | - | - |
| GET | `/nursingList/getTempPrescriptionNote/{id}` | `nurse.NursingServiceImpl.getTempPrescriptionNoteById` | P  | `SCWFMappingDTO` | - | - | - |
| GET | `/nursingList/getTiuXmlFile` | `nurse.NursingServiceImpl.getTiuXmlFile` | Q Q ?  | `void` | - | - | - |
| GET | `/nursingList/getVisitByVisitCause` | `nurse.NursingServiceImpl.getVisitByVisitCause` | Q  | `Encounter` | - | - | - |
| GET | `/nursingList/getWardWiseActivePatientList` | `nurse.NursingServiceImpl.getWardWiseActivePatientList` | Q  | `List<AdmittedPatientDto>` | - | - | - |
| GET | `/nursingList/getWardWisePatientDetailsList` | `nurse.NursingServiceImpl.getWardWisePatientDetailsList` | Q  | `PatientDetailsListWardWiseView` | - | - | - |
| GET | `/nursingList/getWardWisePatientDetailsListCount` | `nurse.NursingServiceImpl.getWardWisePatientDetailsListCount` | Q  | `int` | Y | - | - |
| GET | `/nursingList/holdPatientMedication` | `nurse.NursingServiceImpl.holdPatientMedication` | Q  | `void` | Y | - | - |
| GET | `/nursingList/markPatientForVideoCall` | `nurse.NursingServiceImpl.markPatientAsArrived` | Q Q  | `void` | Y | - | - |
| GET | `/nursingList/patientFutureAppointmentStatus` | `nurse.NursingServiceImpl.patientFutureAppointmentStatus` | Q  | `Boolean` | - | - | - |
| GET | `/nursingList/removeCarePlanDetails` | `nurse.NursingServiceImpl.removeCarePlanDetails` | Q Q  | `String` | - | - | - |
| GET | `/nursingList/removeIcd10fromSolr` | `nurse.NursingServiceImpl.removeIcd10fromSolr` | - | `void` | - | - | - |
| GET | `/nursingList/removePatientComplaint` | `nurse.NursingServiceImpl.removePatientComplaint` | Q Q  | `void` | Y | - | - |
| GET | `/nursingList/saveVisitCause` | `nurse.NursingServiceImpl.saveVisitCause` | Q Q  | `Encounter` | - | - | - |
| GET | `/nursingList/searchAdvice` | `nurse.NursingServiceImpl.searchAdvice` | Q  | `List<Healthfactormaster>` | - | - | - |
| GET | `/nursingList/searchIcdCM` | `nurse.NursingServiceImpl.searchIcdCM` | Q  | `List<Icd10cmSolrView>` | - | - | - |
| GET | `/nursingList/showPatientDetails` | `nurse.NursingServiceImpl.showPatientDetails` | Q  | `PatientDetailsResultDto` | Y | - | - |
| GET | `/nursingList/updateDeliveryKitStatus` | `nurse.NursingServiceImpl.updateDeliveryKitStatus` | Q Q  | `Boolean` | - | - | - |
| GET | `/nursingList/updateEDDByLMP` | `nurse.NursingServiceImpl.updateEDDByLMP` | Q Q  | `String` | Y | - | - |
| GET | `/nursingList/updateMedictaionStatus` | `nurse.NursingServiceImpl.updateMedictaionStatus` | Q  | `void` | Y | - | - |
| GET | `/nursingList/updatePatientMedication` | `nurse.NursingServiceImpl.updatePatientMedication` | Q Q  | `void` | Y | - | - |
| GET | `/nursingList/updatePatientRegestrationIen` | `nurse.NursingServiceImpl.updatePatientRegestrationIen` | Q  | `List<String>` | - | - | - |
| GET | `/nursingList/updateReleaseRequestStatus` | `nurse.NursingServiceImpl.updateReleaseRequestStatus` | Q  | `void` | Y | - | - |
| GET | `/nursingList/updateUSGEDDByLMP` | `nurse.NursingServiceImpl.updateUSGEDDByLMP` | Q Q  | `String` | Y | - | - |
| POST | `/nursingList/addAquityByPatientDetails` | `nurse.NursingServiceImpl.addAquityByPatientDetails` |  | `?` | - | - | - |
| POST | `/nursingList/addCallRecordsDetails` | `nurse.NursingServiceImpl.addCallRecordsDetails` | JSON  | `TeleConsultantDetails` | - | - | - |
| POST | `/nursingList/addCarePlanDetails` | `nurse.NursingServiceImpl.addCarePlanDetails` | JSON  | `CarePlanDetailsDto` | Y | - | - |
| POST | `/nursingList/addChildDeathReview` | `nurse.NursingServiceImpl.addChildDeathReview` | JSON  | `ChildDeathReview` | Y | - | - |
| POST | `/nursingList/addConsultOrder` | `nurse.NursingServiceImpl.addConsultOrder` | JSON  | `Long` | - | - | - |
| POST | `/nursingList/addConsumabled` | `nurse.NursingServiceImpl.addUpdatePrescriptionMasterNew` |  | `?` | - | - | - |
| POST | `/nursingList/addItemSaleForBcma` | `nurse.NursingServiceImpl.addItemSaleForBcma` | JSON  | `ItemSalesDto` | Y | - | - |
| POST | `/nursingList/addNurseDoctorAllocationInfo` | `nurse.NursingServiceImpl.addNurseDoctorAllocationInfo` |  | `?` | - | - | - |
| POST | `/nursingList/addNurseNote` | `nurse.NursingServiceImpl.addNurseNote` | JSON  | `NurseNote` | - | - | - |
| POST | `/nursingList/addNursePatientAllocation` | `nurse.NursingServiceImpl.saveNursePatientAllocation` | JSON  | `Integer` | - | - | - |
| POST | `/nursingList/addNurseTaskDetails` | `nurse.NursingServiceImpl.addNurseTaskDetails` | JSON  | `void` | - | - | - |
| POST | `/nursingList/addOfflinePatientClinicalData` | `nurse.NursingServiceImpl.addOfflinePatientClinicalData` | JSON  | `List<Long>` | - | - | - |
| POST | `/nursingList/addOrderSet` | `nurse.NursingServiceImpl.addOrderSet` | JSON  | `String` | - | - | - |
| POST | `/nursingList/addPatientAddendumNote` | `nurse.NursingServiceImpl.addPatientAddendumNote` | JSON  | `PatientNote` | - | - | - |
| POST | `/nursingList/addPatientDischargeData` | `nurse.NursingServiceImpl.addPatientDischargeData` | JSON  | `PatientDischargeData` | - | - | - |
| POST | `/nursingList/addPatientNote` | `nurse.NursingServiceImpl.addPatientNoteApi` | JSON  | `PatientNote` | - | - | - |
| POST | `/nursingList/addPatientNoteForPrescription` | `nurse.NursingServiceImpl.addPatientNoteForPrescription` | JSON  | `PatientPrescptionNote` | - | - | - |
| POST | `/nursingList/addRemarksSCWF` | `nurse.NursingServiceImpl.addRemarksSCWF` | JSON  | `Boolean` | Y | - | - |
| POST | `/nursingList/addTaperedDose` | `nurse.NursingServiceImpl.addtapereddoseApi` | JSON Q  | `List<AddPrescriptionView>` | Y | - | - |
| POST | `/nursingList/barthelIndexReport` | `nurse.NursingServiceImpl.barthelIndexReport` | JSON  | `NursingReportView` | - | - | - |
| POST | `/nursingList/cancelPMRReport` | `nurse.NursingServiceImpl.cancelPMRReport` | JSON  | `boolean` | Y | - | - |
| POST | `/nursingList/captureMachineVitals` | `nurse.NursingServiceImpl.captureMachineVitals` | JSON  | `Boolean` | - | - | - |
| POST | `/nursingList/cdrFormPrint` | `nurse.NursingServiceImpl.cdrFormPrint` | JSON ?  | `Map<String, Object>` | - | - | - |
| POST | `/nursingList/checkDuplicateUnsingedNote` | `nurse.NursingServiceImpl.checkDuplicateUnsingedNote` | JSON  | `PatientNote` | Y | - | - |
| POST | `/nursingList/checkNoteForPatient` | `nurse.NursingServiceImpl.checkNoteForPatient` | JSON  | `Boolean` | - | - | - |
| POST | `/nursingList/checkSignedPrescription` | `nurse.NursingServiceImpl.checkSignedPrescription` | Q  | `Boolean` | - | - | - |
| POST | `/nursingList/completeNursingOrders` | `nurse.NursingServiceImpl.completeNursingOrders` | JSON  | `void` | Y | - | - |
| POST | `/nursingList/deleteLabService` | `nurse.NursingServiceImpl.deleteLabService` | JSON  | `void` | Y | - | - |
| POST | `/nursingList/departmentDischargePrint` | `nurse.NursingServiceImpl.departmentDischargePrint` | Q  | `DepartmentDischargeDto` | Y | - | - |
| POST | `/nursingList/doctorPatientAllocation` | `nurse.NursingServiceImpl.doctorPatientAllocation` |  | `?` | - | - | - |
| POST | `/nursingList/downloadPMRReportAndDeathForm` | `nurse.NursingServiceImpl.downloadPMRReportAndDeathForm` | Q Q  | `PatientDocuments` | Y | - | - |
| POST | `/nursingList/getAddedServiceCharges` | `nurse.NursingServiceImpl.getCharges` | JSON  | `BillChargesView` | - | - | - |
| POST | `/nursingList/getAddedUnmappedClinicalData` | `nurse.NursingServiceImpl.getAddedUnmappedClinicalData` | JSON  | `UnmappedClinicalDataView` | Y | - | - |
| POST | `/nursingList/getAllActivePatientMedications` | `nurse.NursingServiceImpl.getAllActivePatientMedications` |  | `?` | - | - | - |
| POST | `/nursingList/getAllDiagnosisMappedToComplaint` | `nurse.NursingServiceImpl.getAllDiagnosisMappedToComplaint` | - | `List<PrimarySymptomToDiagnosisMapping>` | Y | - | - |
| POST | `/nursingList/getAlliedStaffListByUnitId` | `nurse.NursingServiceImpl.getAlliedStaffListByUnitId` | JSON  | `List<NurseListResultDto>` | - | - | - |
| POST | `/nursingList/getAllIpPatientList` | `nurse.NursingServiceImpl.getAllIPPatientList` |  | `?` | - | - | - |
| POST | `/nursingList/getAllNurseListByUnitId` | `nurse.NursingServiceImpl.getAllNurseListByUnitId` | JSON  | `List<NurseListResultDto>` | - | - | - |
| POST | `/nursingList/getAllocatedDepartmentByNurseId` | `nurse.NursingServiceImpl.getAllocatedDepartmentByNurseId` |  | `?` | - | - | - |
| POST | `/nursingList/getAllocatedDoctorsByNurseId` | `nurse.NursingServiceImpl.getDoctorsNurseAllocation` | JSON  | `List<NurseDoctorAllocationDetailsDto>` | Y | - | - |
| POST | `/nursingList/getAllocatedNursesByDoctorId` | `nurse.NursingServiceImpl.getAllocatedNursesByDoctorId` |  | `?` | - | - | - |
| POST | `/nursingList/getAllPatientComplaintHistory` | `nurse.NursingServiceImpl.getAllPatientComplaintHistory` | JSON  | `List<PatientComplaints>` | - | - | - |
| POST | `/nursingList/getAllPatientNotesByTitle` | `nurse.NursingServiceImpl.getAllPatientNotesByTitle` | ?  | `List<PatientNote>` | Y | - | - |
| POST | `/nursingList/getAllPatientOrderDetails` | `nurse.NursingServiceImpl.getAllPatientOrderDetails` | JSON  | `NurseOrderView` | Y | - | - |
| POST | `/nursingList/getAllRoleNursePatientList` | `nurse.NursingServiceImpl.getAllRoleNursePatientList` |  | `?` | - | - | - |
| POST | `/nursingList/getAllWardInchargeByUnitId` | `nurse.NursingServiceImpl.getAllWardInchargeByUnitId` | - | `List<NurseListResultDto>` | - | - | - |
| POST | `/nursingList/getAllWardList` | `nurse.NursingServiceImpl.getAllWardList` | - | `Map<String, List<NurseWardSpecializationListResultDto>>` | - | - | - |
| POST | `/nursingList/getAnnotatedImage` | `nurse.NursingServiceImpl.getAnnotatedImage` | Q  | `ImageAnnotationDto` | - | - | - |
| POST | `/nursingList/getAnnotatedImageLst` | `nurse.NursingServiceImpl.getAnnotatedImageLst` | JSON  | `List<ImageAnnotationDto>` | - | - | - |
| POST | `/nursingList/getBabiesDetailsList` | `nurse.NursingServiceImpl.getBabiesDetailsList` | JSON  | `List<PatientHealthFactor>` | - | - | - |
| POST | `/nursingList/getCategoryWisePatientNoteList` | `nurse.NursingServiceImpl.getCategoryWisePatientNoteList` | JSON  | `Map<String, List<PatientNote>>` | Y | - | - |
| POST | `/nursingList/getConfirmedCarePlanDetails` | `nurse.NursingServiceImpl.getConfirmedCarePlanDetails` | JSON  | `List<CarePlanDetailsDto>` | Y | - | - |
| POST | `/nursingList/getHealthFactorList` | `nurse.NursingServiceImpl.getHealthFactorListApi` | JSON  | `List<PatientHealthFactor>` | - | - | - |
| POST | `/nursingList/getLabAndRadOrderDetails` | `nurse.NursingServiceImpl.getLabAndRadOrderDetails` | JSON  | `PatientLabRadDetailsView` | - | - | - |
| POST | `/nursingList/getLabAndRadOrderDetailsForNote` | `nurse.NursingServiceImpl.getLabAndRadOrderDetailsForNote` | JSON  | `PatientLabRadDetailsView` | - | - | - |
| POST | `/nursingList/getLatestPatientVitals` | `nurse.NursingServiceImpl.getLatestPatientVitalsApi` | JSON  | `List<PatientVitals>` | - | - | - |
| POST | `/nursingList/getNoteDetailsListByNoteTitle` | `nurse.NursingServiceImpl.getNoteDetailsListByNoteTitle` | JSON  | `List<PatientNote>` | - | - | - |
| POST | `/nursingList/getNurseCarerTaskDetails` | `nurse.NursingServiceImpl.getNurseCarerTaskDetails` | JSON  | `List<NurseTaskDetails>` | Y | - | - |
| POST | `/nursingList/getNurseListByUnitId` | `nurse.NursingServiceImpl.getNurseListByUnitId` | JSON  | `List<NurseListResultDto>` | - | - | - |
| POST | `/nursingList/getPatientAdtOrders` | `nurse.NursingServiceImpl.getPatientAdtOrders` | JSON  | `Map<String, Object>` | Y | - | - |
| POST | `/nursingList/getPatientAllocationReport` | `nurse.NursingServiceImpl.getPatientAllocationReport` | JSON  | `BillReceiptView` | - | - | - |
| POST | `/nursingList/getPatientFollowUpDetails` | `nurse.NursingServiceImpl.getPatientFollowUpDetails` | Q  | `String` | - | - | - |
| POST | `/nursingList/getPatientGeneralDetailsListForSearchNursing` | `nurse.NursingServiceImpl.getPatientGeneralDetailsListForSearchNursing` |  | `?` | - | - | - |
| POST | `/nursingList/getPatientHealthFactorsByHFName` | `nurse.NursingServiceImpl.getPatientHealthFactors` | JSON  | `List<PatientHealthFactor>` | - | - | - |
| POST | `/nursingList/getPatientNursingOrders` | `nurse.NursingServiceImpl.getPatientNursingOrders` | JSON  | `List<PatientNursingOrder>` | Y | - | - |
| POST | `/nursingList/getPatientPartographDetails` | `nurse.NursingServiceImpl.getPatientPartographDetails` | JSON  | `PatientPartographDetailsDto` | Y | - | - |
| POST | `/nursingList/getPatientPrescriptionDetails` | `nurse.NursingServiceImpl.getPatientPrescriptionDetails` | Q  | `NursingReportView` | - | - | - |
| POST | `/nursingList/getPatientVitals` | `nurse.NursingServiceImpl.getPatientVitals` | JSON  | `List<PatientVitals>` | Y | - | - |
| POST | `/nursingList/getPatientVitalsByDate` | `nurse.NursingServiceImpl.getPatientVitalsByDate` | JSON  | `List<PatientVitals>` | - | - | - |
| POST | `/nursingList/getPatientVitalsByDateRange` | `nurse.NursingServiceImpl.getPatientVitalsByDateRange` | JSON  | `List<PatientVitals>` | - | - | - |
| POST | `/nursingList/getPatientVitalsByVitalIen` | `nurse.NursingServiceImpl.getPatientVitalsByVitalIen` | JSON  | `List<PatientVitals>` | - | - | - |
| POST | `/nursingList/getPMRReport` | `nurse.NursingServiceImpl.getPMRReport` | JSON Q Q  | `Map<String, Object>` | Y | - | - |
| POST | `/nursingList/getPreviousPatientVisitsAndAdmissions` | `nurse.NursingServiceImpl.getPreviousPatientVisitsAndAdmissions` |  | `?` | - | - | - |
| POST | `/nursingList/getProcedureDetails` | `nurse.NursingServiceImpl.getProcedureDetails` | JSON  | `Map<String, Object>` | - | - | - |
| POST | `/nursingList/getRadOrderDetails` | `nurse.NursingServiceImpl.getRadOrderDetails` |  | `?` | Y | - | - |
| POST | `/nursingList/getRoleNursePatientList` | `nurse.NursingServiceImpl.getRoleNursePatientList` |  | `?` | - | - | - |
| POST | `/nursingList/getStatusWisePatientComplaintList` | `nurse.NursingServiceImpl.getStatusWisePatientComplaintListApi` | JSON  | `List<PatientComplaints>` | - | - | - |
| POST | `/nursingList/getTempOrderDetailsOfPatient` | `nurse.NursingServiceImpl.findTempCurrentCharges` | ?  | `List<GetChargesResultDto>` | - | - | - |
| POST | `/nursingList/getTempPrescriptionNote` | `nurse.NursingServiceImpl.getTempPrescriptionNote` | Q Q  | `List<TempPrescriptionNote>` | Y | - | - |
| POST | `/nursingList/getVitalUnitsByVital` | `nurse.NursingServiceImpl.getVitalUnitsByVital` | JSON  | `VitalUnitDetailsDto` | Y | - | - |
| POST | `/nursingList/getVitalUnitsByVitalArrayList` | `nurse.NursingServiceImpl.getVitalUnitsByVitalArrayList` |  | `?` | Y | - | - |
| POST | `/nursingList/getWardListByspecializationForWardIncharge` | `nurse.NursingServiceImpl.getWardListByspecializationForWardIncarge` |  | `?` | - | - | - |
| POST | `/nursingList/labTestReport` | `nurse.NursingServiceImpl.labTestReport` | Q Q  | `NursingReportView` | - | - | - |
| POST | `/nursingList/nurseClinicAllocation` | `nurse.NursingServiceImpl.nurseClinicAllocation` |  | `?` | - | - | - |
| POST | `/nursingList/nurseDepartmentAllocation` | `nurse.NursingServiceImpl.nurseDepartmentAllocation` |  | `?` | - | - | - |
| POST | `/nursingList/nurseWardAllocation` | `nurse.NursingServiceImpl.nurseDoctorDeAllocation` |  | `?` | Y | - | - |
| POST | `/nursingList/patientCheckout` | `nurse.NursingServiceImpl.patientCheckout` | JSON  | `Boolean` | - | - | - |
| POST | `/nursingList/patientDetailsByNewMRN` | `nurse.NursingServiceImpl.` | JSON  | `private GetPatientResultDto_1` | Y | - | - |
| POST | `/nursingList/patientDoctorAllocation` | `nurse.NursingServiceImpl.patientDoctorAllocation` | JSON  | `NursePatientDetails` | - | - | - |
| POST | `/nursingList/printPatientNote` | `nurse.NursingServiceImpl.printPatientNote` | JSON  | `NursingReportView` | Y | - | - |
| POST | `/nursingList/printRabiesReport` | `nurse.NursingServiceImpl.printRabiesReport` | JSON  | `String` | - | - | - |
| POST | `/nursingList/printSimplifiedQuickOrder` | `nurse.NursingServiceImpl.printSimplifiedQuickOrder` | JSON  | `String` | - | - | - |
| POST | `/nursingList/releaseBodyFromBox` | `nurse.NursingServiceImpl.releaseBodyFromBox` | JSON  | `boolean` | Y | - | - |
| POST | `/nursingList/saveAndDownloadDeathForm` | `nurse.NursingServiceImpl.saveAndDownloadDeathForm` | JSON Q  | `PatientNote` | - | - | - |
| POST | `/nursingList/saveAndPrintPMRReport` | `nurse.NursingServiceImpl.saveAndPrintPMRReport` | JSON Q  | `PatientNote` | Y | - | - |
| POST | `/nursingList/saveAnnotatedImage` | `nurse.NursingServiceImpl.saveAnnotatedImage` | JSON  | `ImageAnnotationDto` | - | - | - |
| POST | `/nursingList/saveBabiesHealthFactors` | `nurse.NursingServiceImpl.saveBabiesHealthFactors` | JSON  | `List<PatientHealthFactor>` | - | - | - |
| POST | `/nursingList/saveMarkAsExpired` | `nurse.NursingServiceImpl.saveMarkAsExpired` | JSON  | `boolean` | - | - | - |
| POST | `/nursingList/saveMortuaryPatient` | `nurse.NursingServiceImpl.saveMortuaryPatient` | JSON  | `boolean` | - | - | - |
| POST | `/nursingList/saveOrUpdateFpvChecklistDetails` | `nurse.NursingServiceImpl.saveOrUpdateFpvChecklistDetails` |  | `?` | - | - | - |
| POST | `/nursingList/saveOrUpdatePatientMlcDetails` | `nurse.NursingServiceImpl.saveOrUpdatePatientMlcDetails` | JSON  | `MLCDetails` | - | - | - |
| POST | `/nursingList/savePatientAdvice` | `nurse.NursingServiceImpl.createPatientAdvice` | JSON  | `List<Patientadvice>` | Y | - | - |
| POST | `/nursingList/savePatientAllergies` | `nurse.NursingServiceImpl.createPatientAllergies` | JSON  | `PatientAllergies` | - | - | - |
| POST | `/nursingList/savePatientComplaints` | `nurse.NursingServiceImpl.createPatientComplaints` | JSON  | `PatientComplaints` | Y | - | - |
| POST | `/nursingList/savePatientDiagnosis` | `nurse.NursingServiceImpl.createPatientDiagnosis` | JSON  | `PatientDiagnosisDetails` | Y | - | - |
| POST | `/nursingList/savePatientFlagDetails` | `nurse.NursingServiceImpl.createPatientFlagDetails` | JSON  | `Boolean` | - | - | - |
| POST | `/nursingList/savePatientImmunizationDetails` | `nurse.NursingServiceImpl.savePatientImmunizationDetails` |  | `?` | - | - | - |
| POST | `/nursingList/savePatientNoteHealthFactors` | `nurse.NursingServiceImpl.savePatientHealthFactors` | JSON  | `Boolean` | Y | - | - |
| POST | `/nursingList/savePatientNursingOrders` | `nurse.NursingServiceImpl.savePatientNursingOrders` | JSON  | `PatientNursingOrder` | - | - | - |
| POST | `/nursingList/savePatientStatus` | `nurse.NursingServiceImpl.savePatientStatus` | JSON  | `Boolean` | Y | - | - |
| POST | `/nursingList/savePatientVerificationDetails` | `nurse.NursingServiceImpl.savePatientVerificationDetails` | JSON  | `Boolean` | Y | - | - |
| POST | `/nursingList/savePatientVitals` | `nurse.NursingServiceImpl.createPatientVitals` | JSON  | `Boolean` | - | - | - |
| POST | `/nursingList/saveTransferRequest` | `nurse.NursingServiceImpl.saveTransferRequest` | JSON  | `Boolean` | - | - | - |
| POST | `/nursingList/saveVitalDisplayUnit` | `nurse.NursingServiceImpl.saveVitalDisplayUnit` | JSON  | `VitalUnitDetailsDto` | Y | - | - |
| POST | `/nursingList/saveVitalUnits` | `nurse.NursingServiceImpl.vitalUnitDetails` | JSON  | `VitalUnitDetailsDto` | - | - | - |
| POST | `/nursingList/setPrefLangForPatient` | `nurse.NursingServiceImpl.setPrefLangForPatient` | Q Q  | `void` | - | - | - |
| POST | `/nursingList/signPatientNote` | `nurse.NursingServiceImpl.signPatientNoteApi` | JSON  | `Boolean` | Y | - | - |
| POST | `/nursingList/updateCallRecordsDetails` | `nurse.NursingServiceImpl.updateCallRecordsDetails` | Q Q  | `boolean` | - | - | - |
| POST | `/nursingList/updateCarePlanDetails` | `nurse.NursingServiceImpl.updateCarePlanDetails` | JSON  | `CarePlanDetailsDto` | Y | - | - |
| POST | `/nursingList/updateConsentFilled` | `nurse.NursingServiceImpl.updateConsentFilled` | Q  | `void` | - | - | - |
| POST | `/nursingList/updateCoversheetAudit` | `nurse.NursingServiceImpl.updateCoversheetAudit` | JSON  | `Boolean` | Y | - | - |
| POST | `/nursingList/updateMedicines` | `nurse.NursingServiceImpl.updateMedicines` |  | `?` | Y | - | - |
| POST | `/nursingList/updateNurseAssistment` | `nurse.NursingServiceImpl.updateNurseAssistment` | Q  | `void` | Y | - | - |
| POST | `/nursingList/updateNursingOrderTask` | `nurse.NursingServiceImpl.updateNursingOrderTask` | JSON  | `Long` | Y | - | - |
| POST | `/nursingList/updateOrderSet` | `nurse.NursingServiceImpl.updateOrderSet` | JSON  | `void` | - | - | - |
| POST | `/nursingList/updatePatientAcceptance` | `nurse.NursingServiceImpl.updatePatientAcceptance` | JSON  | `Boolean` | Y | - | - |
| POST | `/nursingList/updatePatientComplaints` | `nurse.NursingServiceImpl.createPatientComplaints` | JSON  | `List<PatientComplaints>` | Y | - | - |
| POST | `/nursingList/updatePatientDiagnosis` | `nurse.NursingServiceImpl.updatePatientDiagnosis` | JSON  | `void` | Y | - | - |
| POST | `/nursingList/updatePatientForFpv` | `nurse.NursingServiceImpl.updatePatientForFpv` | JSON  | `void` | Y | - | - |
| POST | `/nursingList/updateRabiesVaccine` | `nurse.NursingServiceImpl.updateRabiesVaccine` | JSON Q Q  | `String` | Y | - | - |
| POST | `/nursingList/vacatBedForExpiredPatient` | `nurse.NursingServiceImpl.vacatBedForExpiredPatient` | JSON  | `void` | Y | - | - |
| PUT | `/nursingList/deletePlanDetailsById` | `nurse.NursingServiceImpl.deletePlanDetailsById` | Q Q  | `void` | Y | - | - |
| PUT | `/nursingList/nurseClinicDeAllocation` | `nurse.NursingServiceImpl.nurseClinicDeAllocation` | Q  | `Boolean` | - | - | - |
| PUT | `/nursingList/nurseDepartmentDeAllocation` | `nurse.NursingServiceImpl.nurseDepartmentDeAllocation` |  | `?` | Y | - | - |
| PUT | `/nursingList/nurseDoctorDeAllocation` | `nurse.NursingServiceImpl.nurseDoctorDeAllocation` | Q  | `Boolean` | - | - | - |
| PUT | `/nursingList/nurseDoctorDeAllocationByDoctorAndNurse` | `nurse.NursingServiceImpl.nurseDoctorDeAllocation` | Q Q  | `Boolean` | Y | - | - |
| PUT | `/nursingList/nurseWardDeAllocation` | `nurse.NursingServiceImpl.nurseWardDeAllocation` | Q  | `Boolean` | - | - | - |
| PUT | `/nursingList/savePatientEDOD` | `nurse.NursingServiceImpl.savePatientEDOD` | Q  | `Timestamp` | - | - | - |
| PUT | `/nursingList/updatemoleculeMaster` | `nurse.NursingServiceImpl.updatemoleculeMaster` | JSON  | `void` | Y | - | - |
| PUT | `/nursingList/updatePatientFpvDetails` | `nurse.NursingServiceImpl.updatePatientFpvDetails` | Q  | `void` | Y | - | - |
| PUT | `/nursingList/updateTempPrescriptionNote/{id}` | `nurse.NursingServiceImpl.approveRejectTempPrescriptionNote` | P  | `TempPrescriptionNote` | - | - | - |

### Module: PAS (Patient Admin / Billing / Masters)  (998 endpoints)

| Method | URL (under /api/rest) | Class : method | Request | Response | Tx | Tables | Notes |
|---|---|---|---|---|---|
| GET | `/appointment/buildingListForQueue` | `pas.AppointmentServiceImpl.getBuildingListForQueue` | Q  | `List<Buildingmaster>` | - | - | - |
| GET | `/appointment/cabinListForQueue` | `pas.AppointmentServiceImpl.getCabinListForQueueByDepartementId` | Q  | `List<Cabinmaster>` | - | - | - |
| GET | `/appointment/cabinQueueByDisplaUnit` | `pas.AppointmentServiceImpl.getUnitWiseCabinMaster` | Q Q  | `Map<String, Encounter>` | - | - | - |
| GET | `/appointment/cabinQueueListByDept` | `pas.AppointmentServiceImpl.getCabinQueueListByDept` |  | `?` | - | - | - |
| GET | `/appointment/departmentBatchListForQueue` | `pas.AppointmentServiceImpl.getDepartmentBatchListForQueue` | Q  | `List<DoctorBatchSlotDto>` | - | - | - |
| GET | `/appointment/departmentListForQueue` | `pas.AppointmentServiceImpl.getDepartmentListForQueue` | - | `List<Departmentmaster>` | - | - | - |
| GET | `/appointment/getCabinQueueListByDeptForDisplay` | `pas.AppointmentServiceImpl.getCabinQueueListByDeptForDisplay` | Q Q  | `Map<String, List<CabinQueueDto>>` | Y | - | - |
| GET | `/appointment/getLastSponsor` | `pas.AppointmentServiceImpl.getLastVisitSponsorByPatientId` | Q Q  | `SponsorByAdmissionIDResultDto` | - | - | - |
| GET | `/appointment/getLastSponsorByVisitId` | `pas.AppointmentServiceImpl.getLastSponsorByVisitId` |  | `?` | - | - | - |
| GET | `/appointment/getPatientVistaDetails` | `pas.AppointmentServiceImpl.getPatientVistaDetails` | Q Q  | `PatientVistaDetails` | - | - | - |
| GET | `/appointment/markPatientAsArrived` | `pas.AppointmentServiceImpl.markPatientAsArrived` | Q Q  | `void` | Y | - | - |
| GET | `/appointment/printPatientAppointmentDetails` | `pas.AppointmentServiceImpl.getPatientVisitDetails` | Q  | `PatientVisitHistoryView` | - | - | - |
| GET | `/appointment/skipUnskipFromQueue` | `pas.AppointmentServiceImpl.skipUnskipFromQueue` | Q  | `void` | - | - | - |
| POST | `/appointment/getAppointmentListSummary` | `pas.AppointmentServiceImpl.getAppointmentMrNoListSummaryNew` | JSON  | `AppointmentListSummary` | - | - | - |
| POST | `/appointment/getAppointmentMrNoList` | `pas.AppointmentServiceImpl.getAppointmentMrNoListNew` | JSON Q Q  | `List<AppointmentMrNoListResultDto>` | - | - | - |
| POST | `/appointment/getAppointmentMrNoList/department/docotr` | `pas.AppointmentServiceImpl.getAppointmentMrNoListWithDepartmentDoctor` | JSON  | `List<AppointmentView>` | - | - | - |
| POST | `/appointment/getAppointmentMrNoListDocit` | `pas.AppointmentServiceImpl.` | JSON Q  | `private List<AppointmentView>` | - | - | - |
| POST | `/appointment/getAppointmentMrNoListOld` | `pas.AppointmentServiceImpl.getAppointmentMrNoListOld` | JSON  | `List<AppointmentView>` | - | - | - |
| POST | `/appointment/getAppointmentQueue` | `pas.AppointmentServiceImpl.getAppointmentQueue` | JSON  | `List<AppointmentQueueResultDto>` | Y | - | - |
| POST | `/appointment/getAppointmentVisitList` | `pas.AppointmentServiceImpl.getAppointmentVisitListMrNoList` | JSON  | `List<AppointmentVisitView>` | - | - | - |
| POST | `/appointment/getCallPatientList` | `pas.AppointmentServiceImpl.` |  | `?` | - | - | - |
| POST | `/appointment/getPackageAppointmentMrNoList` | `pas.AppointmentServiceImpl.getPackageAppointmentMrNoList` | JSON  | `List<PackageAppointmentListResultDto>` | - | - | - |
| POST | `/appointment/getPatientAllocationDetails` | `pas.AppointmentServiceImpl.getPatientAllocationDetails` | Q  | `List<PatientAllocationDetails>` | Y | - | - |
| POST | `/appointment/getPatientListForPackages` | `pas.AppointmentServiceImpl.getPatientListForPackages` | JSON Q Q  | `List<AppointmentMrNoListResultDto>` | Y | - | - |
| POST | `/appointment/printPatientIntialNote` | `pas.AppointmentServiceImpl.printPatientIntialNote` | JSON  | `PrintPatientIntialAssessmentNoteDTO` | - | - | - |
| POST | `/appointment/printPatientNote` | `pas.AppointmentServiceImpl.printPatientNote` | JSON  | `PrintPatientAssesmentNoteDto` | - | - | - |
| POST | `/appointment/updateDoctorForAppointment` | `pas.AppointmentServiceImpl.updateDoctorForAppointment` | JSON  | `void` | - | - | - |
| POST | `/appointment/uploadPrescriptionFromOPList` | `pas.AppointmentServiceImpl.uploadPrescriptionFromOPList` | JSON Q Q  | `Boolean` | - | - | - |
| PUT | `/appointment/updateAppointmentIsEnabledFlag` | `pas.AppointmentServiceImpl.updateAppointmentIsEnabledFlag` | JSON  | `GenericResultDto` | - | - | - |
| GET | `/bedreservation/getBedUnReservation` | `pas.BedReservationServiceImpl.` |  | `?` | Y | - | - |
| POST | `/bedreservation/addBedReservation` | `pas.BedReservationServiceImpl.addBedReservation` | JSON  | `AddBedReservationDto` | Y | - | - |
| POST | `/bedreservation/cancelBedReservation` | `pas.BedReservationServiceImpl.cancelBedReservation` | Q Q  | `void` | - | - | - |
| POST | `/bedreservation/getIPDBedReservationList` | `pas.BedReservationServiceImpl.getIPDBedReservationList` | JSON  | `List<IPDBedReservationListDto>` | - | - | - |
| GET | `/bed/getWardByClassID` | `pas.BedServiceImpl.getWardByClassID` | Q Q  | `List<WardByClassIDResultDto>` | - | - | - |
| POST | `/bed/getIPDDischargeBedList` | `pas.BedServiceImpl.getIPDDischargeBedList` | JSON  | `List<IPDDischargeBedListResultDto>` | - | - | - |
| GET | `/bill/checkToPrintNewPass` | `pas.BillServiceImpl.checkToPrintNewPass` | Q Q  | `AttendantPassDetailsDto` | - | - | - |
| GET | `/bill/getAllEligiblePassesForReturn` | `pas.BillServiceImpl.getAllEligiblePassesForReturn` | Q Q Q  | `List<AttendantPassDetailsDto>` | Y | - | - |
| GET | `/bill/getBaseHealthPlanOrBasePackageByType` | `pas.BillServiceImpl.getBaseHealthPlanOrBasePackageByType` | Q Q  | `List<Servicemaster>` | Y | - | - |
| GET | `/bill/getGatePassChargesBillId` | `pas.BillServiceImpl.getGatePassChargesForCreditNote` | Q Q Q  | `Long` | - | - | - |
| GET | `/bill/getIssuedPassData` | `pas.BillServiceImpl.getIssuedPassData` | Q Q Q  | `AttendantPassDetailsDto` | - | - | - |
| GET | `/bill/getOrderSetById/{id}` | `pas.BillServiceImpl.getProcedureById` | P  | `Ordersetdefinationmaster` | Y | - | - |
| GET | `/bill/listAllAttendantPasses` | `pas.BillServiceImpl.listAllAttendantPasses` | Q Q  | `List<AttendantPassDetailsDto>` | Y | - | - |
| GET | `/bill/searchItemsBySpecializationId` | `pas.BillServiceImpl.searchItemsBySpecializationId` | Q  | `List<Itemmaster>` | - | - | - |
| GET | `/bill/searchServicesBySpecializationAndSubspecialization` | `pas.BillServiceImpl.searchServicesBySpecializationAndSubspecialization` | Q Q  | `List<Servicemaster>` | - | - | - |
| GET | `/bill/searchServicesBySpecializationId` | `pas.BillServiceImpl.searchServicesBySpecializationId` | Q  | `List<Servicemaster>` | - | - | - |
| GET | `/bill/validateGatePassNo` | `pas.BillServiceImpl.validateGatePassNo` | Q Q Q  | `Boolean` | Y | - | - |
| POST | `/bill/addAndUpdateOrderSetMaster` | `pas.BillServiceImpl.addAndUpdateOrderSetMaster` | JSON  | `void` | Y | - | - |
| POST | `/bill/addServicesForPackage` | `pas.BillServiceImpl.settledBillCancellation` | JSON  | `List<Servicepackagemaster>` | - | - | - |
| POST | `/bill/addTariffPackageHealthPlan` | `pas.BillServiceImpl.settledBillCancellation` | JSON  | `Tariffpackagehealthplandefinationmaster` | Y | - | - |
| POST | `/bill/generateNewAttendantPass` | `pas.BillServiceImpl.generateNewAttendantPass` | ?  | `AttendantPassDetails` | - | - | - |
| POST | `/bill/settledBillCancellation` | `pas.BillServiceImpl.settledBillCancellation` | JSON  | `List<TledBillCancellationResultDto>` | Y | - | - |
| POST | `/bill/unsettledBillCancellation` | `pas.BillServiceImpl.unsettledBillCancellation` | JSON  | `List<LedBillCancellationDto>` | - | - | - |
| POST | `/bill/updateReprintAttendantPassStatus` | `pas.BillServiceImpl.requestReprintAttendantPass` | Q Q  | `boolean` | - | - | - |
| PUT | `/bill/deleteOrderSetById/{id}` | `pas.BillServiceImpl.deleteOrderSetById` | P  | `void` | - | - | - |
| PUT | `/bill/submitPass` | `pas.BillServiceImpl.submitPass` | ?  | `void` | - | - | - |
| GET | `/bloodtestmaster/acceptBloodBag` | `pas.BloodBankServiceImpl.acceptBloodBag` | Q  | `boolean` | Y | - | - |
| GET | `/bloodtestmaster/authenticateStaffAndUpdateThePatientBloodGroup` | `pas.BloodBankServiceImpl.athunticateStaffAndUpdateThePatientBloodGroup` | Q Q  | `BloodBagExtraction` | Y | - | - |
| GET | `/bloodtestmaster/cancelBloodBagComponent` | `pas.BloodBankServiceImpl.cancelBloodBagComponent` | Q Q  | `void` | - | - | - |
| GET | `/bloodtestmaster/cancelDonor` | `pas.BloodBankServiceImpl.cancelDonor` | Q Q  | `BloodDonorRegistration` | Y | - | - |
| GET | `/bloodtestmaster/cancelRequisition` | `pas.BloodBankServiceImpl.cancelRequisition` | Q Q Q  | `PatientBloodRequisition` | Y | - | - |
| GET | `/bloodtestmaster/cancelRequisitionDetails` | `pas.BloodBankServiceImpl.cancelRequisitionDetails` | Q Q  | `PatientBloodRequisitionDetails` | Y | - | - |
| GET | `/bloodtestmaster/checkBagIsCrossmatched` | `pas.BloodBankServiceImpl.checkBagIsCrossmatched` | Q Q Q  | `boolean` | Y | - | - |
| GET | `/bloodtestmaster/checkBloodBankServicesConfigurationStatus` | `pas.BloodBankServiceImpl.checkBloodBankServicesConfigurationStatus` | Q Q  | `boolean` | Y | - | - |
| GET | `/bloodtestmaster/checkComponentStock` | `pas.BloodBankServiceImpl.getBloodStockListBycomponentId` | Q Q  | `Number` | - | - | - |
| GET | `/bloodtestmaster/checkComponentStockInBloodBank` | `pas.BloodBankServiceImpl.checkComponentStockInBloodBank` | Q Q Q  | `Number` | Y | - | - |
| GET | `/bloodtestmaster/checkTheBagExistenceInStock` | `pas.BloodBankServiceImpl.checkTheBagExistenceInStock` | Q Q Q  | `Long` | - | - | - |
| GET | `/bloodtestmaster/checkTheComponentBagChargeService` | `pas.BloodBankServiceImpl.checkTheComponentBagChargeService` | Q  | `Bloodcomponentmaster` | - | - | - |
| GET | `/bloodtestmaster/checkTheSampleExistence` | `pas.BloodBankServiceImpl.checkTheSampleExistence` | Q  | `boolean` | Y | - | - |
| GET | `/bloodtestmaster/checkTheTubeNo` | `pas.BloodBankServiceImpl.checkTheTubeNo` | Q Q  | `boolean` | Y | - | - |
| GET | `/bloodtestmaster/checkWBComponentMappingStatus` | `pas.BloodBankServiceImpl.checkWBComponentMappingStatus` | Q  | `boolean` | - | - | - |
| GET | `/bloodtestmaster/discardBloodBag` | `pas.BloodBankServiceImpl.discardBloodBag` | Q Q Q Q  | `boolean` | - | - | - |
| GET | `/bloodtestmaster/discardBloodBagFromStock` | `pas.BloodBankServiceImpl.discardBloodBagFromStock` | Q Q Q  | `boolean` | Y | - | - |
| GET | `/bloodtestmaster/eraktkoshdataupdateservice` | `pas.BloodBankServiceImpl.eraktkoshdataupdateservice` | Q Q  | `Map<String,List<HashMap<String,String>>>` | - | - | - |
| GET | `/bloodtestmaster/getBagDetails` | `pas.BloodBankServiceImpl.getBagDetails` | Q  | `List<BloodBagExtraction>` | Y | - | - |
| GET | `/bloodtestmaster/getBagListForIssue` | `pas.BloodBankServiceImpl.getBagListForIssue` | Q Q Q  | `List<BloodBagCollectionDto>` | - | - | - |
| GET | `/bloodtestmaster/getBloodBagStockCountByBloodBankId` | `pas.BloodBankServiceImpl.getBloodstockDtoCountByBloodBankIdNew` | Q Q  | `Map<Long, Map<Long, Map<String, Long>>>` | - | - | - |
| GET | `/bloodtestmaster/getBloodbankReorderList` | `pas.BloodBankServiceImpl.getBloodbankReorderList` | Q  | `List<CurrentBloodBankReoderResultDto>` | - | - | - |
| GET | `/bloodtestmaster/getBloodRequisitionDetailsForPatient` | `pas.BloodBankServiceImpl.getBloodRequisitionDetailsForPatient` | Q  | `List<PatientRequsitionListResultDto>` | - | - | - |
| GET | `/bloodtestmaster/getBloodstockDtoCountByBloodBankId` | `pas.BloodBankServiceImpl.getBloodstockDtoCountByBloodBankId` | Q Q  | `Map<Long,Map<Long,Map<String,Long>>>` | - | - | - |
| GET | `/bloodtestmaster/getBloodStockListBycomponentId` | `pas.BloodBankServiceImpl.getBloodStockListBycomponentId` | Q  | `List<GetBloodStockListBycomponentIdDto>` | - | - | - |
| GET | `/bloodtestmaster/getCrossMarchTestDetailsForIssueLabelPrint` | `pas.BloodBankServiceImpl.getCrossMarchTestDetailsForIssueLabelPrint` | Q  | `List<BloodCrossMatchingTestDetailsDto>` | - | - | - |
| GET | `/bloodtestmaster/getCrossMatchingDetails` | `pas.BloodBankServiceImpl.getCrossMatchingDetails` | Q  | `List<BloodCrossMatchingTestDetailsDto>` | - | - | - |
| GET | `/bloodtestmaster/getDemandSlipDetails` | `pas.BloodBankServiceImpl.getDemandSlipDetails` | Q Q Q  | `List<DemandSlip>` | Y | - | - |
| GET | `/bloodtestmaster/getDistrictListWiseComponent` | `pas.BloodBankServiceImpl.getDistrictListWiseComponent` | Q Q Q  | `List<DistrictMaster>` | - | - | - |
| GET | `/bloodtestmaster/getDonorDefaultVitalValues` | `pas.BloodBankServiceImpl.getDonorDefaultVitalValues` | Q  | `DonorDefaultVitalValue` | Y | - | - |
| GET | `/bloodtestmaster/getDonorDetailsByBloodBagNo` | `pas.BloodBankServiceImpl.getDonorDetailsByBloodBagNo` | JSON  | `List<DonorDetailResultByBloodBagNoDto>` | Y | - | - |
| GET | `/bloodtestmaster/getDonorDetailsByDonorNo` | `pas.BloodBankServiceImpl.getDonorDetailsByDonorNo` | Q Q  | `List<DonorDetails>` | - | - | - |
| GET | `/bloodtestmaster/getDonorDetailsForCrossMatching` | `pas.BloodBankServiceImpl.getDonorDetailsForCrossMatching` | Q Q  | `DonorDetailResultDto` | - | - | - |
| GET | `/bloodtestmaster/getDonorList` | `pas.BloodBankServiceImpl.getDonorList` | Q Q Q  | `List<DonorDetails>` | Y | - | - |
| GET | `/bloodtestmaster/getDonorMedicalExamResult` | `pas.BloodBankServiceImpl.getDonorMedicalExamResult` | Q Q  | `List<DonorMedicalExamDetailsResultDto>` | - | - | - |
| GET | `/bloodtestmaster/getDonorRegistrationDetails` | `pas.BloodBankServiceImpl.getDonorRegistrationDetails` | Q Q  | `BloodDonorRegistration` | Y | - | - |
| GET | `/bloodtestmaster/getDonorRegistrationDetailsByMRNo` | `pas.BloodBankServiceImpl.getDonorRegistrationDetailsByMRNo` | Q Q Q  | `BloodDonorRegistration` | Y | - | - |
| GET | `/bloodtestmaster/getDonorTestServicesStatus` | `pas.BloodBankServiceImpl.getDonorTestServicesStatus` | Q  | `GetOrderIdbyPatientIdResultDto` | - | - | - |
| GET | `/bloodtestmaster/getHospitalWiseComponent` | `pas.BloodBankServiceImpl.getHospitalWiseComponent` | Q Q  | `List<RaiseBloodBankIndentDto>` | - | - | - |
| GET | `/bloodtestmaster/getIssuedBagDetailsForLabelPrint` | `pas.BloodBankServiceImpl.getIssuedBagDetailsForLabelPrint` | Q  | `List<BloodCrossMatchingTestDetailsDto>` | - | - | - |
| GET | `/bloodtestmaster/getPatientBloodgroup` | `pas.BloodBankServiceImpl.getPatientBloodgroup` | Q  | `Number` | - | - | - |
| GET | `/bloodtestmaster/getPatientBloodSampleDetails` | `pas.BloodBankServiceImpl.getPatientBloodSampleDetails` | Q  | `List<PatientBloodSampleDetailsDto>` | Y | - | - |
| GET | `/bloodtestmaster/getPatientDetailsBySampleNo` | `pas.BloodBankServiceImpl.getPatientDetailsBySampleNo` | Q  | `List<PatientBloodRequisition>` | - | - | - |
| GET | `/bloodtestmaster/getPatientPreviousTransfusionHistory` | `pas.BloodBankServiceImpl.getPatientPreviousTransfusionHistory` | Q Q  | `List<BloodBagReceive>` | Y | - | - |
| GET | `/bloodtestmaster/getPatientRequisitionAndCrossMatchResultDetails` | `pas.BloodBankServiceImpl.getPatientRequisitionDetails` | Q  | `List<PatientBloodRequisitionReportResultDto>` | Y | - | - |
| GET | `/bloodtestmaster/getRequisitionDetailsById` | `pas.BloodBankServiceImpl.getRequisitionDetailsById` | Q Q  | `PatientBloodRequisition` | - | - | - |
| GET | `/bloodtestmaster/getRequisitionDetailsForReplacementHistory` | `pas.BloodBankServiceImpl.getRequisitionDetailsForReplacementHistory` | Q Q  | `List<PatientBloodRequisitionDetails>` | - | - | - |
| GET | `/bloodtestmaster/getRequisitionListByMRNo` | `pas.BloodBankServiceImpl.getRequisitionListByMRNo` | Q  | `List<PatientBloodRequisition>` | - | - | - |
| GET | `/bloodtestmaster/getReservedBagDetailsForAutologousTypeOrder` | `pas.BloodBankServiceImpl.getReservedBagDetailsForAutologousTypeOrder` | Q  | `Number` | - | - | - |
| GET | `/bloodtestmaster/getReturnedBagDetails` | `pas.BloodBankServiceImpl.getReturnedBagDetails` | Q  | `BloodBagExtraction` | - | - | - |
| GET | `/bloodtestmaster/getTheBloodBankExistedDistricts` | `pas.BloodBankServiceImpl.getTheBloodBankExistedDistricts` | Q  | `DonorRegistrationView` | - | - | - |
| GET | `/bloodtestmaster/getUnitListWiseComponent` | `pas.BloodBankServiceImpl.getUnitListWiseComponent` | Q  | `List<Unitmaster>` | - | - | - |
| GET | `/bloodtestmaster/getUnitSpecificBloodStores` | `pas.BloodBankServiceImpl.getUnitStoreListNewIndent` | Q  | `List<Store>` | - | - | - |
| GET | `/bloodtestmaster/getUserDetailsList` | `pas.BloodBankServiceImpl.getUserDetailsList` | Q  | `List<TUserdetails>` | - | - | - |
| GET | `/bloodtestmaster/printBloodBagCrossMatchReport` | `pas.BloodBankServiceImpl.printBloodBagCrossMatchReport` | Q Q  | `String` | Y | - | - |
| GET | `/bloodtestmaster/searchBagInTheStock` | `pas.BloodBankServiceImpl.searchBagInTheStock` | Q Q Q  | `List<BloodExtractionResultDto>` | - | - | - |
| GET | `/bloodtestmaster/searchBloodBagListWithCode` | `pas.BloodBankServiceImpl.searchBloodBagListWithCode` | Q Q  | `List<BloodExtractionResultDto>` | - | - | - |
| GET | `/bloodtestmaster/sellBloodBagFromStock` | `pas.BloodBankServiceImpl.sellBloodBagFromStock` | Q Q  | `boolean` | - | - | - |
| GET | `/bloodtestmaster/TransferRequisition` | `pas.BloodBankServiceImpl.transferRequisitionToOtherStore` | Q Q  | `PatientBloodRequisitionDetails` | - | - | - |
| POST | `/bloodtestmaster/addBagReturn` | `pas.BloodBankServiceImpl.addBagReturn` | JSON  | `BloodBagReturn` | - | - | - |
| POST | `/bloodtestmaster/addBloodBagCollection` | `pas.BloodBankServiceImpl.addUpdateBloodsDonorBagCollection` | JSON  | `BloodDonorBagCollectionDto` | - | - | - |
| POST | `/bloodtestmaster/addBloodBankIndent` | `pas.BloodBankServiceImpl.addBloodBankIndent` | JSON  | `BloodBankIndent` | - | - | - |
| POST | `/bloodtestmaster/addBloodComponants` | `pas.BloodBankServiceImpl.updateBloodBagCompoOpeningStock` | JSON  | `List<BloodBagExtraction>` | - | - | - |
| POST | `/bloodtestmaster/addBloodComponantsDetails` | `pas.BloodBankServiceImpl.updateBloodBagCompoOpeningStocks` | JSON  | `BloodBagExtraction` | - | - | - |
| POST | `/bloodtestmaster/addBloodGroup` | `pas.BloodBankServiceImpl.addBloodGroup` | JSON  | `void` | Y | - | - |
| POST | `/bloodtestmaster/addOpeningStockToBloodBankStock` | `pas.BloodBankServiceImpl.addOpeningStockToBloodBankStock` | JSON  | `List<BloodBagExtraction>` | - | - | - |
| POST | `/bloodtestmaster/addUpdateBloodDonorRegistration` | `pas.BloodBankServiceImpl.addUpdateBloodDonorRegistration` | JSON  | `BloodDonorRegistration` | Y | - | - |
| POST | `/bloodtestmaster/athunticateStaffAndDecrossTheBloodBag` | `pas.BloodBankServiceImpl.athunticateStaffAndDecrossTheBloodBag` | JSON  | `BloodBagDecross` | - | - | - |
| POST | `/bloodtestmaster/bloodbagcomponentcheck` | `pas.BloodBankServiceImpl.bloodbagcomponentcheck` | JSON  | `List<Bloodbagtypecomponentmaster>` | Y | - | - |
| POST | `/bloodtestmaster/bloodcomponentcliniccheck` | `pas.BloodBankServiceImpl.bloodcomponentcliniccheck` | JSON  | `List<BloodComponentClinic>` | Y | - | - |
| POST | `/bloodtestmaster/cancelIndent` | `pas.BloodBankServiceImpl.cancelIndent` | Q  | `boolean` | Y | - | - |
| POST | `/bloodtestmaster/generateDemandSlip` | `pas.BloodBankServiceImpl.generateDemandSlip` | JSON  | `String[]` | - | - | - |
| POST | `/bloodtestmaster/getBagsReturnList` | `pas.BloodBankServiceImpl.getBagsReturnList` | JSON Q  | `List<ReturnedBagDetailsResultDto>` | - | - | - |
| POST | `/bloodtestmaster/getBloodBagTrail/{id}` | `pas.BloodBankServiceImpl.getBloodBagTrail` | Q Q P  | `List<Map<String,Object>>` | - | - | - |
| POST | `/bloodtestmaster/getBloodBankIndentListByUnit` | `pas.BloodBankServiceImpl.getBloodBankIndentListByUnit` | JSON Q Q  | `List<BloodBankIndent>` | Y | - | - |
| POST | `/bloodtestmaster/getBloodDiscardedList` | `pas.BloodBankServiceImpl.getBloodDiscardedList` | JSON Q  | `List<DiscardedBagsDetailsResultDto>` | Y | - | - |
| POST | `/bloodtestmaster/getBloodExtractionList` | `pas.BloodBankServiceImpl.getBloodExtractionList` | JSON  | `List<BloodExtractionResultDto>` | Y | - | - |
| POST | `/bloodtestmaster/getBloodOpeningStockDetails` | `pas.BloodBankServiceImpl.getBloodOpeningStockDetails` | JSON Q  | `List<BloodExtractionResultDto>` | - | - | - |
| POST | `/bloodtestmaster/getCrossMatchedBagList` | `pas.BloodBankServiceImpl.getCrossMatchedBagList` | JSON Q  | `List<CrossMatchedBagDetailsResultDto>` | - | - | - |
| POST | `/bloodtestmaster/getCrossMatchingTestResults` | `pas.BloodBankServiceImpl.getCrossMatchingTestResults` | JSON Q  | `List<PatientRequsitionListResultDto>` | Y | - | - |
| POST | `/bloodtestmaster/getCurrentBloodStockListForExtraction` | `pas.BloodBankServiceImpl.getCurrentBloodStockListForExtraction` | JSON  | `List<BloodExtractionResultDto>` | - | - | - |
| POST | `/bloodtestmaster/getCurrentBloodStockListForExtractionCount` | `pas.BloodBankServiceImpl.getCurrentBloodStockListForExtractionCount` | JSON  | `List<BloodExtractionCountResultDto>` | Y | - | - |
| POST | `/bloodtestmaster/getCurrentUnitBloodStockList` | `pas.BloodBankServiceImpl.getCategoryCountOfStockList` | JSON Q  | `BloodBagExtractionView` | - | - | - |
| POST | `/bloodtestmaster/getDoctorList` | `pas.BloodBankServiceImpl.getDoctorList` | JSON  | `List<DoctorDetails>` | - | - | - |
| POST | `/bloodtestmaster/getDonorDetails` | `pas.BloodBankServiceImpl.getDonorDetails` | JSON Q  | `List<DonorDetailResultDto>` | - | - | - |
| POST | `/bloodtestmaster/getExternalPatientDetails` | `pas.BloodBankServiceImpl.getExternalPatientDetails` | JSON Q  | `List<SearchPatientGeneralDetailsResultDto>` | - | - | - |
| POST | `/bloodtestmaster/getPathTestResultBloodBank` | `pas.BloodBankServiceImpl.getPathTestResultBloodBank` | JSON  | `Map<String,Map<String,String>>` | - | - | - |
| POST | `/bloodtestmaster/getPatientRequisitionListCount` | `pas.BloodBankServiceImpl.getPatientRequisitionListCount` | JSON Q  | `BloodBagExtractionView` | - | - | - |
| POST | `/bloodtestmaster/getPatientTestList` | `pas.BloodBankServiceImpl.getPatientTestList` | JSON  | `List<GetOrderIdbyPatientIdResultDto>` | - | - | - |
| POST | `/bloodtestmaster/getReplacementDonorHistory` | `pas.BloodBankServiceImpl.getReplacementDonorHistory` | JSON  | `List<ReplacementDonorHistoryDto>` | - | - | - |
| POST | `/bloodtestmaster/getUnitWiseAdmittedPatientSearch` | `pas.BloodBankServiceImpl.getUnitWiseAdmittedPatientSearch` | JSON  | `List<SearchPatientGeneralDetailsResultDto>` | - | - | - |
| POST | `/bloodtestmaster/getUnitWisePatientGeneralDetailsListForSearch` | `pas.BloodBankServiceImpl.getUnitWisePatientGeneralDetailsListForSearch` | JSON  | `List<SearchPatientGeneralDetailsResultDto>` | - | - | - |
| POST | `/bloodtestmaster/issueBagForExternalOrders` | `pas.BloodBankServiceImpl.issueBagForExternalOrders` | JSON  | `void` | - | - | - |
| POST | `/bloodtestmaster/issueBagToExternalHospitalAndStorageCenters` | `pas.BloodBankServiceImpl.issueBagToExternalHospitalAndStorageCenters` | JSON  | `BloodBagCollectionDto` | Y | - | - |
| POST | `/bloodtestmaster/issueBloodBag` | `pas.BloodBankServiceImpl.issueBloodBag` | JSON  | `BloodBagCollectionDto` | Y | - | - |
| POST | `/bloodtestmaster/issueIndent` | `pas.BloodBankServiceImpl.issueIndent` | JSON Q Q  | `boolean` | - | - | - |
| POST | `/bloodtestmaster/pdfBloodDonationCertificate` | `pas.BloodBankServiceImpl.pdfBloodDonationCertificate` | JSON  | `String[]` | Y | - | - |
| POST | `/bloodtestmaster/printBloodBankIndentDetails` | `pas.BloodBankServiceImpl.printBloodBankIndentDetails` | Q  | `String[]` | Y | - | - |
| POST | `/bloodtestmaster/receiveBloodBag` | `pas.BloodBankServiceImpl.receiveBloodBag` | JSON  | `void` | - | - | - |
| POST | `/bloodtestmaster/receiveIndent` | `pas.BloodBankServiceImpl.receiveIndent` | JSON Q  | `boolean` | Y | - | - |
| POST | `/bloodtestmaster/recordAdverseReactionOfTheTransfusion` | `pas.BloodBankServiceImpl.recordAdverseReactionOfTheTransfusion` | JSON  | `BloodBagReceive` | Y | - | - |
| POST | `/bloodtestmaster/rejectIndent` | `pas.BloodBankServiceImpl.rejectIndent` | JSON  | `boolean` | Y | - | - |
| POST | `/bloodtestmaster/saveBloodCrossMatchingTestResult` | `pas.BloodBankServiceImpl.saveBloodCrossMatchingTestResult` | JSON  | `BloodCrossmatchingResult` | - | - | - |
| POST | `/bloodtestmaster/saveDonorMedicalExaminationResult` | `pas.BloodBankServiceImpl.saveOrUpdateDonorMedicalExaminationResult` | JSON Q  | `DonorMedicalExamination` | - | - | - |
| POST | `/bloodtestmaster/searchPatientList` | `pas.BloodBankServiceImpl.getPatientSearchList` | JSON  | `List<PatientSearchList>` | Y | - | - |
| POST | `/bloodtestmaster/updateBagNo` | `pas.BloodBankServiceImpl.updateBagNo` | Q Q  | `Boolean` | Y | - | - |
| POST | `/bloodtestmaster/updateBagStatus` | `pas.BloodBankServiceImpl.updateBagStatus` | JSON  | `BloodBagExtraction` | Y | - | - |
| POST | `/bloodtestmaster/updateIndentStoreandUnit` | `pas.BloodBankServiceImpl.updateIndentStoreandUnit` | JSON  | `boolean` | Y | - | - |
| POST | `/bloodtestmaster/updateReturnedBagStatus` | `pas.BloodBankServiceImpl.updateReturnedBagStatus` | JSON  | `boolean` | - | - | - |
| POST | `/bloodtestmaster/updateSingleReturnedBloodBag` | `pas.BloodBankServiceImpl.updateSingleReturnedBloodBag` | JSON  | `boolean` | Y | - | - |
| PUT | `/bloodtestmaster/updateBloodBagBloodGroup` | `pas.BloodBankServiceImpl.updateBloodBagBloodGroup` | Q Q Q  | `BloodBagExtraction` | - | - | - |
| PUT | `/bloodtestmaster/updatebloodgroupcross` | `pas.BloodBankServiceImpl.updateBloodGroupCrossMatching` | JSON  | `void` | Y | - | - |
| PUT | `/bloodtestmaster/updateBloodSampleCollectionStatus` | `pas.BloodBankServiceImpl.updateBloodSampleCollectionStatusApi` | Q Q  | `void` | - | - | - |
| GET | `/charge/getTariffClassRateByClassID` | `pas.ChargeServiceImpl.getTariffClassRateByClassID` | Q  | `List<TariffClassRateByClassIDResultDto>` | - | - | - |
| POST | `/charge/getCharges` | `pas.ChargeServiceImpl.getCharges` | JSON  | `List<GetChargesResultDto>` | - | - | - |
| POST | `/charge/getPackageServiceFromPackageID` | `pas.ChargeServiceImpl.getPackageServiceFromPackageID` | JSON  | `List<PackageServiceFromPackageIDResultDto>` | - | - | - |
| POST | `/charge/getServicesByBillID` | `pas.ChargeServiceImpl.findServicesByBillID` | JSON  | `List<ServicesByBillIDResultDto>` | - | - | - |
| DELETE | `/companyapprovedamount/{Id}` | `pas.CompanyApprovedAmountServiceImpl.removeCorporateApprovedAmt` | P  | `void` | Y | - | - |
| GET | `/companyapprovedamount/checkAliasDiagnosisMasters` | `pas.CompanyApprovedAmountServiceImpl.checkAliasDiagnosisMasters` | Q Q Q  | `List<Number>` | - | - | - |
| GET | `/companyapprovedamount/checkBedAndWardInMasters` | `pas.CompanyApprovedAmountServiceImpl.checkBedAndWardInMasters` | Q Q Q  | `Number` | - | - | - |
| GET | `/companyapprovedamount/checkCodeInMasters` | `pas.CompanyApprovedAmountServiceImpl.checkCodeInMasters` | Q Q Q Q  | `Number` | Y | - | - |
| GET | `/companyapprovedamount/checkDescriptionInMasters` | `pas.CompanyApprovedAmountServiceImpl.checkDescriptionInMasters` | Q Q Q  | `Number` | - | - | - |
| GET | `/companyapprovedamount/checkParticularForYear` | `pas.CompanyApprovedAmountServiceImpl.checkParticularForYear` | Q Q  | `Number` | - | - | - |
| GET | `/companyapprovedamount/checkServiceCodeInMasters` | `pas.CompanyApprovedAmountServiceImpl.checkServiceCodeInMasters` | Q Q Q  | `Number` | - | - | - |
| GET | `/companyapprovedamount/checkTestPrintSequence` | `pas.CompanyApprovedAmountServiceImpl.checkTestPrintSequence` | Q Q Q  | `Number` | - | - | - |
| GET | `/companyapprovedamount/checkUniqueIdInCadreDesignationMasters` | `pas.CompanyApprovedAmountServiceImpl.checkUniqueIdInCadreDesignation` | Q  | `List<Object>` | - | - | - |
| GET | `/companyapprovedamount/checkUniqueIdInGrievanceCategoryMaster` | `pas.CompanyApprovedAmountServiceImpl.checkUniqueIdInGrievanceCategoryMaster` | Q  | `List<Object>` | - | - | - |
| GET | `/companyapprovedamount/checkUnitwiseDescriptionInMasters` | `pas.CompanyApprovedAmountServiceImpl.checkUnitwiseDescriptionInMasters` | Q Q Q  | `Number` | - | - | - |
| GET | `/companyapprovedamount/checkUnitwisePassIDInMasters` | `pas.CompanyApprovedAmountServiceImpl.checkUnitwisePassIDInMasters` | Q Q  | `Number` | - | - | - |
| GET | `/companyapprovedamount/getPayerById/{id}` | `pas.CompanyApprovedAmountServiceImpl.getPayerById` | P  | `Companymaster` | Y | - | - |
| POST | `/companyapprovedamount/addCompanyApprovedAmountDetails` | `pas.CompanyApprovedAmountServiceImpl.addCompanyApprovedAmountDetails` | JSON  | `CompanyApprovedAmountDetailsDto` | - | - | - |
| POST | `/companyapprovedamount/addPayerList` | `pas.CompanyApprovedAmountServiceImpl.addPayerList` | JSON  | `void` | Y | - | - |
| POST | `/companyapprovedamount/getCompanyApprovedAmtListByAdmissionID` | `pas.CompanyApprovedAmountServiceImpl.getCompanyApprovedAmtListByAdmissionID` | JSON  | `List<CompanyApprovedAmtListByAdmissionIDNewResultDto>` | Y | - | - |
| PUT | `/companyapprovedamount/deletePayerListById/{id}` | `pas.CompanyApprovedAmountServiceImpl.deletePayerListById` | P  | `void` | - | - | - |
| PUT | `/companyapprovedamount/updatePayerList` | `pas.CompanyApprovedAmountServiceImpl.updatePayerList` | JSON  | `Companymaster` | Y | - | - |
| GET | `/complaintdetails/getComplaintDetailsByComptIDAndUnitID` | `pas.ComplaintDetailsServiceImpl.getComplaintDetailsByComptIDAndUnitID` | Q  | `EqpComplaintView` | - | - | - |
| GET | `/complaintdetails/getDistrictFromZone` | `pas.ComplaintDetailsServiceImpl.getDistrictFromZone` | Q Q  | `List<DistrictMaster>` | - | - | - |
| GET | `/complaintdetails/getEquipmentPreviousHistory` | `pas.ComplaintDetailsServiceImpl.getEquipmentPreviousHistory` | Q Q Q  | `List<EqpPreviousHistoryResultDto>` | - | - | - |
| GET | `/complaintdetails/getPatVistAIenforInc` | `pas.ComplaintDetailsServiceImpl.getPatVistAIenforInc` | Q  | `Number` | - | - | - |
| GET | `/complaintdetails/getUnitDepartmentDetails` | `pas.ComplaintDetailsServiceImpl.getUnitDepartmentDetails` | - | `List<Departmentmaster>` | Y | - | - |
| GET | `/complaintdetails/getUnitsByZone` | `pas.ComplaintDetailsServiceImpl.getUnitsByZone` | Q Q  | `List<Unitmaster>` | - | - | - |
| POST | `/complaintdetails/addComplaintResolution` | `pas.ComplaintDetailsServiceImpl.addComplaintResolution` | JSON Q  | `ComplaintResolutionView` | - | - | - |
| POST | `/complaintdetails/addNewComplaint` | `pas.ComplaintDetailsServiceImpl.addNewComplaint` | JSON Q  | `EqpComplaintDetailsDto` | Y | - | - |
| POST | `/complaintdetails/addNewEqpPreventSchedule` | `pas.ComplaintDetailsServiceImpl.addNewEqpPreventSchedule` | JSON  | `List<EquipmentDetailsforSearchResultDto>` | Y | - | - |
| POST | `/complaintdetails/getEquiComplaintDetails` | `pas.ComplaintDetailsServiceImpl.getEquipmentComplaintDetails` | JSON Q  | `List<ComplaintDetailsResultDto>` | Y | - | - |
| POST | `/complaintdetails/updateComplaintResolution` | `pas.ComplaintDetailsServiceImpl.updateComplaintResolution` | JSON  | `List<EqpComplaintDetails>` | - | - | - |
| GET | `/complaintresolutiondetail/getAssetItemsWithAccessionCode` | `pas.ComplaintResolutionDetailServiceImpl.getAssetItemsFormComplaintRaisedStore` | Q  | `List<AssetItemWithAccessCodeDto>` | - | - | - |
| GET | `/complaintresolutiondetail/getMaintainanceDepUserList` | `pas.ComplaintResolutionDetailServiceImpl.getMaintainanceDepUserList` | Q  | `List<Staffdepartmentdetails>` | - | - | - |
| GET | `/complaintresolutiondetail/getStaffDetails` | `pas.ComplaintResolutionDetailServiceImpl.getStaffDetails` | Q  | `Staffmaster` | - | - | - |
| POST | `/complaintresolutiondetail/getComplaintResolutionDetails` | `pas.ComplaintResolutionDetailServiceImpl.getComplaintResolutionDetails` | JSON Q  | `List<ComplaintResolutionDetailsResultDto>` | - | - | - |
| PUT | `/complaintresolutiondetail/updateStaffDetails` | `pas.ComplaintResolutionDetailServiceImpl.updateStaffDetails` | Q Q Q  | `void` | - | - | - |
| GET | `/concessiontemplate/getPathoConcessionTemplateById/{id}` | `pas.ConcessionTemplateServiceImpl.getPathoConcessionTemplateById` | P  | `Concessiontemplatemaster` | Y | - | - |
| POST | `/concessiontemplate/addConcessionTemplateList` | `pas.ConcessionTemplateServiceImpl.addConcession` | JSON  | `void` | - | - | - |
| PUT | `/concessiontemplate/deleteConcessionTemplateListById/{id}` | `pas.ConcessionTemplateServiceImpl.deleteConcessionTemplateListById` | P  | `void` | Y | - | - |
| PUT | `/concessiontemplate/updateConcessionTemplate/{id}` | `pas.ConcessionTemplateServiceImpl.updateConcession` | JSON P  | `Concessiontemplatemaster` | - | - | - |
| POST | `/departmentschedule/addDepartmentMaster` | `pas.DepartmentScheduleServiceImpl.addPlanMasterDetails` | JSON  | `void` | - | - | - |
| POST | `/departmentschedule/getDepartmentsByUnit` | `pas.DepartmentScheduleServiceImpl.getDepartmentsByUnit` | JSON  | `List<DepartmentMst>` | Y | - | - |
| POST | `/departmentschedule/updateDepartmentMaster` | `pas.DepartmentScheduleServiceImpl.updateDepartmentMaster` | JSON ?  | `Departmentmaster` | Y | - | - |
| PUT | `/departmentschedule/updateDepartmentMst/{id}` | `pas.DepartmentScheduleServiceImpl.updateDepartmentMst` | JSON P ?  | `DepartmentMst` | - | - | - |
| GET | `/diet/getActiveWards` | `pas.DietServiceImpl.getActiveWards` | Q  | `List<Wardmaster>` | Y | - | - |
| GET | `/diet/getDietFoodListCount` | `pas.DietServiceImpl.getDietFoodListCount` | Q Q  | `Map<String,Long>` | Y | - | - |
| GET | `/diet/getDietKichenDetails` | `pas.DietServiceImpl.getKichenDetail` | Q Q Q  | `DietKitchenView` | Y | - | - |
| GET | `/diet/getDietOrderDetailByAdmissionId` | `pas.DietServiceImpl.getDietOrderDetailByAdmissionId` | Q Q  | `DietOrderDetails` | - | - | - |
| GET | `/diet/getDietOrderListCount` | `pas.DietServiceImpl.getDietOrderListCount` | Q Q  | `DietOrderDetailsSummary` | Y | - | - |
| GET | `/diet/getDietPatReport` | `pas.DietServiceImpl.getDietPatReport` | Q  | `List<DietOrderScheduleDetails>` | - | - | - |
| GET | `/diet/getItemsByDietTypeId` | `pas.DietServiceImpl.removeDietMenuItem` | Q Q  | `List<TodayDietItemMenu>` | Y | - | - |
| GET | `/diet/getPatientDietOrderList` | `pas.DietServiceImpl.getPatientDietOrderList` | Q  | `List<DietOrderDetails>` | Y | - | - |
| GET | `/diet/getservicemaster` | `pas.DietServiceImpl.getServiceMasterData` | - | `List<Servicemaster>` | - | - | - |
| GET | `/diet/getTodayDietItem` | `pas.DietServiceImpl.getTodayDietItems` | Q  | `JSONArray` | Y | - | - |
| GET | `/diet/getWardsForFilter` | `pas.DietServiceImpl.getWardsForFilter` | Q  | `List<Wardmaster>` | - | - | - |
| GET | `/diet/getWardWisePatDtls` | `pas.DietServiceImpl.getWardWisePatDtls` | Q Q  | `List<DietOrderScheduleDetails>` | Y | - | - |
| POST | `/diet/adddietitemmaster` | `pas.DietServiceImpl.addDietItem` | JSON  | `void` | - | - | - |
| POST | `/diet/addDietItemMenu` | `pas.DietServiceImpl.addDietItemMenu` | JSON  | `void` | Y | - | - |
| POST | `/diet/addDietMenuItem` | `pas.DietServiceImpl.getTodayDietItems` | JSON  | `TodayDietItemMenu` | - | - | - |
| POST | `/diet/addpatdietorder` | `pas.DietServiceImpl.addPatDietOrder` | JSON  | `List<DietOrderDetails>` | Y | - | - |
| POST | `/diet/addPatFooddItemOrder` | `pas.DietServiceImpl.addPatFooddItemOrder` | JSON  | `void` | - | - | - |
| POST | `/diet/getDietFoodOrderList` | `pas.DietServiceImpl.getDietFoodOrderList` | Q  | `List<DietOrderScheduleDetails>` | - | - | - |
| POST | `/diet/getDietOrderList` | `pas.DietServiceImpl.getDietOrderList` | Q  | `List<DietOrderScheduleDetails>` | - | - | - |
| POST | `/diet/getFilterwiseDietOrderList` | `pas.DietServiceImpl.getFilterwiseDietOrderList` | JSON  | `List<DietOrderDetails>` | Y | - | - |
| POST | `/diet/removeDietItem` | `pas.DietServiceImpl.removeDietItem` | JSON  | `DietItemScheduleDetails` | Y | - | - |
| POST | `/diet/removeDietMenuItem` | `pas.DietServiceImpl.removeDietMenuItem` | JSON  | `TodayDietItemMenu` | Y | - | - |
| POST | `/diet/signPatientDietOrders` | `pas.DietServiceImpl.signPatientDietOrders` | JSON  | `Boolean` | - | - | - |
| POST | `/diet/updateDietFoodOrderForAll` | `pas.DietServiceImpl.updateDietFoodOrderForAll` | JSON Q Q  | `Boolean` | - | - | - |
| POST | `/diet/updateDietOrderDetailsById` | `pas.DietServiceImpl.updateDietOrderDetailsById` | JSON  | `Boolean` | - | - | - |
| PUT | `/diet/updateDietFoodOrder` | `pas.DietServiceImpl.updateDietFoodOrder` | Q Q  | `void` | - | - | - |
| PUT | `/diet/updateDietFoodStatusForCancel` | `pas.DietServiceImpl.updateDietFoodStatusForCancel` | JSON  | `void` | - | - | - |
| PUT | `/diet/updatedietitemmaster` | `pas.DietServiceImpl.updateDietItem` | JSON  | `void` | Y | - | - |
| PUT | `/diet/updateDietOrderDetails` | `pas.DietServiceImpl.updateDietOrderDetails` | Q Q  | `void` | Y | - | - |
| PUT | `/diet/updateUncheckFoodOrder` | `pas.DietServiceImpl.updateUncheckFoodOrder` | Q Q  | `void` | - | - | - |
| GET | `/doctor/checkDoctorIsCheckedIn` | `pas.DoctorServiceImpl.checkDoctorIsCheckedIn` | Q Q  | `DoctorCheckInQueueRoom` | Y | - | - |
| GET | `/doctor/checkoutQMSDoctor` | `pas.DoctorServiceImpl.checkoutQMSDoctor` | Q  | `void` | - | - | - |
| GET | `/doctor/getAssignedPatientDetailForDoctorQueue` | `pas.DoctorServiceImpl.getAssignedPatientDetailForDoctorQueue` | Q Q Q  | `List<PatientDetailsForDoctorQueue>` | Y | - | - |
| GET | `/doctor/getAssignedPatientDetailForDoctorQueueFromNurse` | `pas.DoctorServiceImpl.getAssignedPatientDetailForDoctorQueueFromNurse` | Q Q  | `List<PatientDetailsForDoctorQueue>` | Y | - | - |
| GET | `/doctor/getDepartmentListByDoctorID` | `pas.DoctorServiceImpl.getDepartmentListByDoctorID` | Q  | `List<DepartmentListByDoctorIDResultDto_1>` | - | - | - |
| GET | `/doctor/getDepartmentsByDoctorID` | `pas.DoctorServiceImpl.getDepartmentsByDoctorID` | Q Q  | `List<DepartmentListByDoctorIDResultDto_1>` | - | - | - |
| GET | `/doctor/getDoctorFutureAppointmentSchedule` | `pas.DoctorServiceImpl.getDoctorFutureAppointmentSchedule` | Q Q Q  | `List<AppointmentMrNoListResultDto>` | Y | - | - |
| GET | `/doctor/getMappedRoomFromDepartmentId` | `pas.DoctorServiceImpl.getMappedRoomFromDepartmentId` | Q Q ?  | `List<RoomMasterForQueue>` | - | - | - |
| GET | `/doctor/getPatientDetailByTokenAndDepartment` | `pas.DoctorServiceImpl.getPatientDetailByTokenAndDepartment` | Q Q  | `List<PatientDetailsForDoctorQueue>` | Y | - | - |
| POST | `/doctor/addDoctorAdditionDeduction` | `pas.DoctorServiceImpl.addDoctorAdditionDeduction` | JSON  | `DoctorAdditionDeductionDto` | Y | - | - |
| POST | `/doctor/addDoctorBill` | `pas.DoctorServiceImpl.addDoctorBill` | JSON  | `void` | - | - | - |
| POST | `/doctor/addDoctorBillPayment` | `pas.DoctorServiceImpl.addDoctorBillPayment` | JSON  | `void` | Y | - | - |
| POST | `/doctor/assignedRoomToDoctorForQueue` | `pas.DoctorServiceImpl.assignedRoomToDoctorForQueueApi` | Q Q Q  | `DoctorCheckInQueueRoom` | - | - | - |
| POST | `/doctor/assignNextPatientToDoctor` | `pas.DoctorServiceImpl.assignNextPatientToDoctor` | JSON Q Q  | `String` | - | - | - |
| POST | `/doctor/assignNextPatientToDoctorFromNurseDesk` | `pas.DoctorServiceImpl.assignNextPatientToDoctorFromNurseDesk` | Q Q Q  | `String` | - | - | - |
| POST | `/doctor/assignPatientToDoctorQueueByNurse` | `pas.DoctorServiceImpl.assignPatientToDoctorQueueByNurse` | JSON Q Q  | `String` | - | - | - |
| POST | `/doctor/checkoutSinglePatientFromQueue` | `pas.DoctorServiceImpl.checkoutSinglePatientFromQueue` | Q Q Q  | `Boolean` | Y | - | - |
| POST | `/doctor/getDoctorAddDeductList` | `pas.DoctorServiceImpl.getDoctorAddDeductList` | JSON  | `List<DoctorAddDeductResultListDto>` | - | - | - |
| POST | `/doctor/getDoctorBillList` | `pas.DoctorServiceImpl.` |  | `?` | - | - | - |
| POST | `/doctor/getDoctorShareChargeDetailList` | `pas.DoctorServiceImpl.getDoctorShareChargeDetailList` | JSON  | `List<DoctorShareChargeDetailResultListDto>` | - | - | - |
| POST | `/doctor/getDoctorShareList` | `pas.DoctorServiceImpl.` |  | `?` | - | - | - |
| POST | `/doctor/updateDoctorbusyFlag` | `pas.DoctorServiceImpl.updateDoctorbusyFlag` | Q Q Q Q  | `Boolean` | Y | - | - |
| PUT | `/doctor/assignPatientVisit` | `pas.DoctorServiceImpl.assignPatientVisit` | Q Q Q  | `void` | Y | - | - |
| PUT | `/doctor/checkinCheckout` | `pas.DoctorServiceImpl.checkinCheckout` | Q Q Q  | `Boolean` | Y | - | - |
| PUT | `/doctor/updateDoctorShare` | `pas.DoctorServiceImpl.updateDoctorShare` | JSON  | `void` | - | - | - |
| POST | `/drug/getPrescriptionList` | `pas.DrugServiceImpl.` |  | `?` | - | - | - |
| DELETE | `/equipment/rejectEquipmentProvGRN` | `pas.EquipmentServiceImpl.rejectProvisionalGRN` | Q  | `void` | Y | - | - |
| GET | `/equipment/deleteEquipmentDoc` | `pas.EquipmentServiceImpl.deleteEquipmentDoc` | Q Q  | `void` | - | - | - |
| GET | `/equipment/downloadActualEquipmentDoc` | `pas.EquipmentServiceImpl.downloadActualEquipmentDoc` | Q ?  | `void` | - | - | - |
| GET | `/equipment/downloadEquipmentDoc` | `pas.EquipmentServiceImpl.downloadEquipmentDoc` | HDR Q Q  | `String` | Y | - | - |
| GET | `/equipment/getAccessionNoForMedGass` | `pas.EquipmentServiceImpl.getAccessionNoForMedGass` | Q Q  | `Equipmentdetail` | Y | - | - |
| GET | `/equipment/getAssetDetailsByAccessionNumber` | `pas.EquipmentServiceImpl.getAssetDetailsByAccessionNumber` | Q  | `EqpComplaintView` | - | - | - |
| GET | `/equipment/getAssetDtlsByItem` | `pas.EquipmentServiceImpl.getAssetDetailsByItem` | Q  | `List<AssetDetailsView>` | - | - | - |
| GET | `/equipment/getAssetGrnUploadedDocument` | `pas.EquipmentServiceImpl.getAssetGrnUploadedDocument` | Q  | `List<PatientIDDocumentDto>` | Y | - | - |
| GET | `/equipment/getAssetVerificationList` | `pas.EquipmentServiceImpl.getAssetVerificationList` | Q  | `List<EquipmentListDetailsDto>` | Y | - | - |
| GET | `/equipment/getEqAmcWarrantyCount` | `pas.EquipmentServiceImpl.getEqAmcWarrantyCount` | Q  | `Map<String, Long>` | Y | - | - |
| GET | `/equipment/getEqAmcWarrantyDetails` | `pas.EquipmentServiceImpl.getEqAmcWarrantyDetails` | Q Q  | `List<EquipAmcWarrantyDetails>` | - | - | - |
| GET | `/equipment/getEqpDetailsByEqpId` | `pas.EquipmentServiceImpl.getEqpDetailsByEqpId` | Q  | `List<GetEqpDetailsByEqpById>` | - | - | - |
| GET | `/equipment/getEquipDetailsByItemID` | `pas.EquipmentServiceImpl.getEquipDetailsByItemID` | Q  | `List<EquipDetailsByItemIDResultDto>` | - | - | - |
| GET | `/equipment/getEquipmentItemDetails` | `pas.EquipmentServiceImpl.getEquipmentItemDetails` | Q  | `List<GetEquipmentItemDetailsDto>` | Y | - | - |
| GET | `/equipment/getIndentEquipmentByIndentNo` | `pas.EquipmentServiceImpl.getIndentEquipmentByIndentNo` | Q  | `List<EquipmentIndentDetails>` | - | - | - |
| GET | `/equipment/getIndentIssueEquipmentByIndentNo` | `pas.EquipmentServiceImpl.getIndentIssueEquipmentByIndentNo` | Q  | `List<EquipmentIndentDetails>` | - | - | - |
| GET | `/equipment/getIsAccession` | `pas.EquipmentServiceImpl.getIsAccession` | Q Q  | `boolean` | - | - | - |
| GET | `/equipment/getPoNumberforEquipmentGRN` | `pas.EquipmentServiceImpl.getPoNumberforGRN` | Q Q Q  | `List<String>` | - | - | - |
| GET | `/equipment/getSupplierForEquipment` | `pas.EquipmentServiceImpl.getSupplierForEquipment` | Q Q  | `List<Object>` | Y | - | - |
| GET | `/equipment/getUploadedEquipmentFiles` | `pas.EquipmentServiceImpl.getUploadedEquipmentFiles` | Q  | `List<EquipmentDetailDocs>` | - | - | - |
| GET | `/equipment/recieveEquipment` | `pas.EquipmentServiceImpl.recieveEquipment` | Q Q Q  | `List<GetEquipmentItemDetailsDto>` | - | - | - |
| GET | `/equipment/updateMedIndentApprove` | `pas.EquipmentServiceImpl.updateMedIndentApprove` | Q Q  | `Equipmentdetail` | - | - | - |
| POST | `/equipment/addEquipmentCheckList` | `pas.EquipmentServiceImpl.addEquipmentCheckList` | JSON  | `void` | - | - | - |
| POST | `/equipment/addEquipmentIndent` | `pas.EquipmentServiceImpl.addEquipmentIndent` | JSON  | `String` | - | - | - |
| POST | `/equipment/addEquipOpeningBalance` | `pas.EquipmentServiceImpl.addEquipOpeningBalanceApi` | JSON  | `void` | Y | - | - |
| POST | `/equipment/addEqupmentUsageMaster` | `pas.EquipmentServiceImpl.addEqupmentUsageMaster` | JSON  | `void` | Y | - | - |
| POST | `/equipment/addProvisionlEquipmentGRN` | `pas.EquipmentServiceImpl.addProvisionlEquipmentGRN` | JSON  | `AddGRNView` | - | - | - |
| POST | `/equipment/approveProvisionalEquipmentGRNNew` | `pas.EquipmentServiceImpl.approveProvisionalGRNNew` | JSON  | `AddGRNView` | Y | - | - |
| POST | `/equipment/cancelEquipDetails` | `pas.EquipmentServiceImpl.cancelEquipDetails` | JSON  | `String` | - | - | - |
| POST | `/equipment/equipCompleteOpenBalItems` | `pas.EquipmentServiceImpl.equipCompleteOpenBalItems` | JSON  | `void` | Y | - | - |
| POST | `/equipment/equipIndentIssueComplete` | `pas.EquipmentServiceImpl.equipIndentIssueComplete` | JSON Q  | `String` | - | - | - |
| POST | `/equipment/generateCodeForRegistredAsset` | `pas.EquipmentServiceImpl.generateCodeForRegistredAsset` | JSON  | `String[]` | - | - | - |
| POST | `/equipment/getApprovEquipmenGRNListForSearch` | `pas.EquipmentServiceImpl.getApproveGRNListForSearch` | JSON Q  | `List<GRNListForSearchResultDto>` | Y | - | - |
| POST | `/equipment/getAssetDetails` | `pas.EquipmentServiceImpl.getAssetDetails` | JSON Q Q  | `List<AssetDetailsResultDto>` | Y | - | - |
| POST | `/equipment/getEquipDetForAssetRegister` | `pas.EquipmentServiceImpl.getEquipDetByAccessionNo` | JSON Q  | `List<EquipmentListDetailsDto>` | - | - | - |
| POST | `/equipment/getEquipMedicalIndetIssueList` | `pas.EquipmentServiceImpl.getEquipMedicalIndetIssueList` | JSON Q  | `List<EquipmentIndentDetails>` | Y | - | - |
| POST | `/equipment/getEquipMedicalIndetList` | `pas.EquipmentServiceImpl.getEquipMedicalIndetList` | JSON Q  | `List<EquipmentIndentDetails>` | Y | - | - |
| POST | `/equipment/getEquipmentDetails` | `pas.EquipmentServiceImpl.getEquipmentDetails` | JSON  | `List<GetDirectIssueItemDetailsResultDto>` | Y | - | - |
| POST | `/equipment/getEquipmentPOrderList` | `pas.EquipmentServiceImpl.getEquipmentPOrderList` | JSON  | `PurchaseOrderView` | Y | - | - |
| POST | `/equipment/getEquipUsageServiceList` | `pas.EquipmentServiceImpl.getEquipUsageServiceList` | JSON Q  | `List<EquipmentUsageDetails>` | Y | - | - |
| POST | `/equipment/getGRNForGrnNosforDirectGRN` | `pas.EquipmentServiceImpl.getGRNForGrnNosforDirectGRN` | JSON Q  | `List<GRNResultRptforDirectGRNDto>` | Y | - | - |
| POST | `/equipment/receiveItemIssueForEquipment` | `pas.EquipmentServiceImpl.updateReceiveItemIssue` | JSON  | `void` | - | - | - |
| POST | `/equipment/updateEqpmentAprGRNNew` | `pas.EquipmentServiceImpl.upidateGRNNew` | JSON  | `void` | Y | - | - |
| POST | `/equipment/updateEquipmentGRN` | `pas.EquipmentServiceImpl.updateEquipmentGRN` | JSON  | `EquipmentDetailMasterDto` | Y | - | - |
| POST | `/equipment/updateEquipmentWarranty` | `pas.EquipmentServiceImpl.updateEquipmentWarranty` | JSON  | `List<EquipmentWarranty>` | Y | - | - |
| POST | `/equipment/updateEquipOpenBalItems` | `pas.EquipmentServiceImpl.updateEquipOpenBalItems` | JSON  | `void` | Y | - | - |
| POST | `/equipment/uploadAssetGrnDocument` | `pas.EquipmentServiceImpl.uploadAssetGrnDocument` | JSON Q  | `Boolean` | Y | - | - |
| POST | `/equipment/uploadEquipmentFiles` | `pas.EquipmentServiceImpl.uploadEquipmentFiles` | Q ? Q  | `List<StorageRequest>` | - | - | - |
| PUT | `/equipment/equipMedicalGassesUpdate` | `pas.EquipmentServiceImpl.equipMedicalGassesUpdate` | Q Q Q  | `String` | - | - | - |
| PUT | `/equipment/updateEquipmentCheckList` | `pas.EquipmentServiceImpl.updateEquipmentCheckList` | JSON  | `void` | - | - | - |
| PUT | `/equipment/updateInvoiceForAsset` | `pas.EquipmentServiceImpl.updateInvoiceForAsset` | Q Q Q  | `void` | - | - | - |
| GET | `/expiryitem/getExpiredDamagedItemListById` | `pas.ExpiryItemServiceImpl.` |  | `?` | - | - | - |
| GET | `/expiryitem/getExpiredItemList` | `pas.ExpiryItemServiceImpl.getExpiredItems` | Q Q  | `List<ItemExpiryList>` | Y | - | - |
| GET | `/expiryitem/getExpiryReturnDetails` | `pas.ExpiryItemServiceImpl.getExpiryReturnDetails` | Q  | `List<ExpiryItemReturnDetailsDto>` | - | - | - |
| GET | `/expiryitem/getGRNItemBatchesToReturn` | `pas.ExpiryItemServiceImpl.getGRNItemBatchesToReturn` | Q  | `List<ItemBatchesResultDto>` | - | - | - |
| GET | `/expiryitem/getGrnNoforRetToSupplier` | `pas.ExpiryItemServiceImpl.getPoNumberforGRN` | Q Q Q  | `List<String>` | - | - | - |
| GET | `/expiryitem/getGRNReturnDetailsById` | `pas.ExpiryItemServiceImpl.getGRNReturnDetailsById` | Q  | `List<GrnReturnDetailsByIdResultDto>` | - | - | - |
| GET | `/expiryitem/getGRNReturnDetailsByIdReceiveCredit` | `pas.ExpiryItemServiceImpl.getGRNReturnDetailsByIdREceiveCredit` | Q  | `List<GrnReturnDetailsByIdResultDto>` | - | - | - |
| GET | `/expiryitem/getItemInfoforReturntoSupplier` | `pas.ExpiryItemServiceImpl.getItemInfoforReturntoSupplier` | Q Q Q  | `List<PreviousItemGRNDetailsforGRNReturnDto>` | Y | - | - |
| GET | `/expiryitem/getStoreListByBatchId` | `pas.ExpiryItemServiceImpl.getStoreListByBatchId` | Q  | `List<RecallExpiredItemView>` | Y | - | - |
| GET | `/expiryitem/getSupplierCreditNoteDetailsById` | `pas.ExpiryItemServiceImpl.getSupplierCreditNoteDetailsById` | Q  | `List<SupplierCreditNoteDetailsByIdResultDto>` | - | - | - |
| POST | `/expiryitem/addExpiredDamagedItemIndent` | `pas.ExpiryItemServiceImpl.addExpiredDamagedItemIndent` | JSON  | `AddIndentView` | - | - | - |
| POST | `/expiryitem/addExpiryItemReturn` | `pas.ExpiryItemServiceImpl.addExpiryItem` | JSON  | `AddExpiryItemReturnView` | - | - | - |
| POST | `/expiryitem/addItemBatchesOnHold` | `pas.ExpiryItemServiceImpl.addItemforConsumption` | JSON  | `AddItemBatchesForHoldView` | - | - | - |
| POST | `/expiryitem/addSupplierGRNReturn` | `pas.ExpiryItemServiceImpl.addSupplierGRNReturn` | JSON  | `AddGRNReturnView` | Y | - | - |
| POST | `/expiryitem/expiryItemReturnToSupplier` | `pas.ExpiryItemServiceImpl.expiryItemReturnToSupplier` | Q Q  | `RecallExpiredItemView` | Y | - | - |
| POST | `/expiryitem/getDamagedItemIndentList` | `pas.ExpiryItemServiceImpl.getDamagedItemIndentList` | JSON Q  | `List<DamagedItemIndentListResultDto>` | - | - | - |
| POST | `/expiryitem/getDamagedReturnedItemDetailsById` | `pas.ExpiryItemServiceImpl.` |  | `?` | - | - | - |
| POST | `/expiryitem/getExpiredAndDamagedItemIndentList` | `pas.ExpiryItemServiceImpl.getExpiredAndDamagedItemIndentList` | JSON  | `List<ExpiredItemIndentListResultDto>` | - | - | - |
| POST | `/expiryitem/getExpiredDamagedItemAvaStock` | `pas.ExpiryItemServiceImpl.` |  | `?` | - | - | - |
| POST | `/expiryitem/getExpiredItemIndentList` | `pas.ExpiryItemServiceImpl.getExpiredItemIndentList` | JSON Q  | `List<ExpiredItemIndentListResultDto>` | - | - | - |
| POST | `/expiryitem/getExpiredReturnedItemDetailsById` | `pas.ExpiryItemServiceImpl.` |  | `?` | Y | - | - |
| POST | `/expiryitem/getExpiryItemList` | `pas.ExpiryItemServiceImpl.getExpiryItemList` | JSON  | `List<ExpiryItemListResultDto>` | Y | - | - |
| POST | `/expiryitem/getExpiryItemsReturnList` | `pas.ExpiryItemServiceImpl.getExpiryItemsReturnList` | JSON  | `List<ItemBatchesResultDto>` | - | - | - |
| POST | `/expiryitem/getExpiryReturnItemDetailList` | `pas.ExpiryItemServiceImpl.` |  | `?` | Y | - | - |
| POST | `/expiryitem/getGRNReturnList` | `pas.ExpiryItemServiceImpl.getGRNReturnList` | - | `List<GrnReturnListResultDto>` | - | - | - |
| POST | `/expiryitem/getRecallExpiredItemList` | `pas.ExpiryItemServiceImpl.getRecallExpiredItemList` | JSON Q  | `List<RecallExpiredItemView>` | Y | - | - |
| POST | `/expiryitem/getRecallExpiredItemListById` | `pas.ExpiryItemServiceImpl.getRecallExpiredItemListById` | JSON Q  | `List<RecallExpiredItemView>` | - | - | - |
| POST | `/expiryitem/getRecallExpiredItemListNew` | `pas.ExpiryItemServiceImpl.getRecallExpiredItemListNew` | JSON Q  | `RecallExpiredItemView` | - | - | - |
| POST | `/expiryitem/getReturnToSupplierList` | `pas.ExpiryItemServiceImpl.getReturnToSupplierList` | JSON Q  | `List<GetGRNReturnListResultDto>` | Y | - | - |
| POST | `/expiryitem/getSupplierProductList` | `pas.ExpiryItemServiceImpl.getSupplierProductList` | JSON  | `List<SupplierProductListResultDto>` | - | - | - |
| POST | `/expiryitem/getSupplierReturnItemDetailsById` | `pas.ExpiryItemServiceImpl.` |  | `?` | Y | - | - |
| POST | `/expiryitem/getSupplierReturnItemDetailsByIdforPrint` | `pas.ExpiryItemServiceImpl.getSupplierReturnItemDetailsByIdforPrint` | JSON Q  | `BillReceiptView` | - | - | - |
| POST | `/expiryitem/getSuppliersByItemIdForExpiryItemReturn` | `pas.ExpiryItemServiceImpl.getSuppliersByItemIdForExpiryItemReturn` | JSON  | `List<ExpiredItemSupplierResultDto>` | Y | - | - |
| POST | `/expiryitem/issueExpiredItems` | `pas.ExpiryItemServiceImpl.issueExpiredItems` | JSON  | `IssueView` | Y | - | - |
| POST | `/expiryitem/printCreditNoteDetails` | `pas.ExpiryItemServiceImpl.printCreditNoteDetails` | Q JSON  | `CreditNoteDetailsView` | - | - | - |
| POST | `/expiryitem/printDamagedReturnedItemDetails` | `pas.ExpiryItemServiceImpl.printDamagedReturnedItemDetailsById` | JSON  | `ExpiryItemDEtailsView` | - | - | - |
| POST | `/expiryitem/printExpiredReturnedItemDetails` | `pas.ExpiryItemServiceImpl.` |  | `?` | - | - | - |
| POST | `/expiryitem/receiveCreditNote` | `pas.ExpiryItemServiceImpl.receiveCreditNote` | JSON  | `SupplierCreditNoteDto` | - | - | - |
| POST | `/expiryitem/receiveExpiredItems` | `pas.ExpiryItemServiceImpl.receiveExpiredItems` | JSON  | `IssueView` | Y | - | - |
| PUT | `/expiryitem/approveExpiredItemIndent` | `pas.ExpiryItemServiceImpl.approveExpiredItemIndent` | Q Q  | `void` | - | - | - |
| PUT | `/expiryitem/approveSupplierReturn` | `pas.ExpiryItemServiceImpl.approveSupplierReturn` | JSON  | `void` | - | - | - |
| PUT | `/expiryitem/expDamageReturnAceptance` | `pas.ExpiryItemServiceImpl.expDamageReturnAceptance` | Q Q Q  | `void` | - | - | - |
| PUT | `/expiryitem/freezeRecallExpiredItems` | `pas.ExpiryItemServiceImpl.freezeRecallExpiredItems` | JSON  | `void` | - | - | - |
| PUT | `/expiryitem/freezeRecallExpiredItemsNew` | `pas.ExpiryItemServiceImpl.freezeRecallExpiredItemsNew` | JSON  | `AddIndentView` | Y | - | - |
| PUT | `/expiryitem/issueExpiredItemIndent` | `pas.ExpiryItemServiceImpl.issueExpiredItemIndent` | Q Q  | `void` | Y | - | - |
| PUT | `/expiryitem/rejectSupplierReturn` | `pas.ExpiryItemServiceImpl.rejectSupplierReturn` | JSON  | `void` | Y | - | - |
| PUT | `/expiryitem/updateExpiryItemReturnStatus` | `pas.ExpiryItemServiceImpl.updateExpiryItemReturnStatus` | JSON  | `void` | - | - | - |
| GET | `/helper/getAllDataForReport` | `pas.HelperServiceImpl.getAllDataForReport` | Q Q Q  | `HospitalReportSummary` | - | - | - |
| POST | `/helper/registerChild` | `pas.HelperServiceImpl.registerChild` | JSON Q  | `List<ChildResultDto>` | - | - | - |
| POST | `/helper/registerChildDocit` | `pas.HelperServiceImpl.registerChildDocit` | JSON  | `List<ChildResultDto>` | Y | - | - |
| DELETE | `/ipdadmission/removeMLCFile` | `pas.IPDAdmissionServiceImpl.removeMLCFile` | Q  | `void` | - | - | - |
| DELETE | `/ipdadmission/removeMLCPhoto` | `pas.IPDAdmissionServiceImpl.removeMLCPhoto` | Q  | `void` | Y | - | - |
| GET | `/ipdadmission/checkPatientExpired` | `pas.IPDAdmissionServiceImpl.checkPatientExpired` | Q  | `boolean` | Y | - | - |
| GET | `/ipdadmission/getActiveAdmissionOfRegisteredPatient` | `pas.IPDAdmissionServiceImpl.getActiveAdmissionOfRegisteredPatient` | Q  | `List<GetActiveAdmissionOfRegisteredPatientResultDto>` | - | - | - |
| GET | `/ipdadmission/getActiveVisitOfRegisteredPatient` | `pas.IPDAdmissionServiceImpl.getActiveVisitOfRegisteredPatient` | Q Q  | `List<GetActiveAdmissionOfRegisteredPatientResultDto>` | - | - | - |
| GET | `/ipdadmission/getAdmissionRequestedPatient` | `pas.IPDAdmissionServiceImpl.` |  | `?` | Y | - | - |
| GET | `/ipdadmission/getAdmissionRequestToHIS` | `pas.IPDAdmissionServiceImpl.getAdmissionRequestToHIS` | Q  | `Long` | - | - | - |
| GET | `/ipdadmission/getChargesByAdmIDAndAdmUnitID` | `pas.IPDAdmissionServiceImpl.getChargesByAdmIDAndAdmUnitID` | Q  | `List<ChargesByAdmIDAndAdmUnitIDResultDto>` | - | - | - |
| GET | `/ipdadmission/getSponsorByAdmissionID` | `pas.IPDAdmissionServiceImpl.getSponsorByAdmissionIDApi` | Q  | `List<SponsorByAdmissionIDResultDto>` | - | - | - |
| GET | `/ipdadmission/getTransferRequestedPatient` | `pas.IPDAdmissionServiceImpl.getTransferRequestedPatient` | Q  | `List<TransferRequestResultDto>` | - | - | - |
| POST | `/ipdadmission/addAdmissionRequestToHIS` | `pas.IPDAdmissionServiceImpl.addAdmissionRequestToHIS` | JSON  | `Boolean` | - | - | - |
| POST | `/ipdadmission/addAdmMLCDetails` | `pas.IPDAdmissionServiceImpl.addAdmMLCDetails` | JSON  | `AdmMLCDetailsDto` | - | - | - |
| POST | `/ipdadmission/addAdvisedDischargeList` | `pas.IPDAdmissionServiceImpl.addAdvisedDischargeList` | JSON  | `AdvisedDischargeListDto` | - | - | - |
| POST | `/ipdadmission/getPatientAdmissionList` | `pas.IPDAdmissionServiceImpl.getPatientAdmissionList` | JSON  | `List<PatientAdmissionListResultDto>` | - | - | - |
| POST | `/ipdadmission/getPatientAdmissionList_improved` | `pas.IPDAdmissionServiceImpl.getPatientAdmissionList_improved` | JSON  | `List<PatientAdmissionListResultDto_New>` | - | - | - |
| POST | `/ipdadmission/getPatientAdmissionListByDate` | `pas.IPDAdmissionServiceImpl.getPatientAdmissionListByDate` | JSON  | `List<PatientAdmissionListByDateResultDto>` | - | - | - |
| POST | `/ipdadmission/getPatientAdmissionListForClearance` | `pas.IPDAdmissionServiceImpl.getPatientAdmissionListForClearance` | JSON Q  | `List<PatientAdmissionListResultDto>` | - | - | - |
| PUT | `/ipdadmission/updateBedOccupiedFlag` | `pas.IPDAdmissionServiceImpl.updateBedOccupiedFlag` | JSON  | `UpdateBedOccupiedFlagDto` | - | - | - |
| DELETE | `/ipdconfig/deleteBedAmmenities` | `pas.IPDConfigServiceImpl.deleteBedAmmenities` | JSON  | `GenericResultDto` | - | - | - |
| DELETE | `/ipdconfig/deleteDietPlanDetails` | `pas.IPDConfigServiceImpl.deleteDietPlanDetails` | JSON  | `GenericResultDto` | - | - | - |
| GET | `/ipdconfig/getBedMasterAmmenitiesDetails` | `pas.IPDConfigServiceImpl.getBedMasterAmmenitiesDetails` | Q  | `List<BedMasterAmmenitiesDetailsResultDto>` | - | - | - |
| GET | `/ipdconfig/getClassOrderDetails` | `pas.IPDConfigServiceImpl.getClassOrderDetails` | Q  | `List<ClassOrderDetailsResultDto>` | - | - | - |
| GET | `/ipdconfig/getCodeTypeListByApplicableOn` | `pas.IPDConfigServiceImpl.getCodeTypeListByApplicableOn` | Q  | `List<CodeTypeListByApplicableOnResultDto>` | - | - | - |
| GET | `/ipdconfig/getDietPlanDetails` | `pas.IPDConfigServiceImpl.getDietPlanDetails` | Q  | `List<GetDietPlanDetailsResultDto>` | - | - | - |
| GET | `/ipdconfig/getDietPlanDetailsUsingID` | `pas.IPDConfigServiceImpl.getDietPlanDetailsUsingID` | Q  | `List<DietPlanDetailsUsingIDResultDto>` | - | - | - |
| GET | `/ipdconfig/getFoodItems` | `pas.IPDConfigServiceImpl.getFoodItems` | Q Q  | `GenericResultDto` | - | - | - |
| GET | `/ipdconfig/getRoomMasterAmmenitiesDetails` | `pas.IPDConfigServiceImpl.getRoomMasterAmmenitiesDetails` | Q  | `List<RoomMasterAmmenitiesDetailsResultDto>` | - | - | - |
| POST | `/ipdconfig/addBedAmmenitiesDetails` | `pas.IPDConfigServiceImpl.addBedAmmenitiesDetails` | JSON  | `GenericResultDto` | - | - | - |
| POST | `/ipdconfig/addDietPlanMaster` | `pas.IPDConfigServiceImpl.addDietPlanMaster` | JSON  | `AddDietPlanMasterDto` | - | - | - |
| POST | `/ipdconfig/addUpdateBedMaster` | `pas.IPDConfigServiceImpl.addUpdateBedMaster` | JSON  | `UpdateBedMasterDto` | - | - | - |
| POST | `/ipdconfig/addUpdateBlockMaster` | `pas.IPDConfigServiceImpl.addUpdateBlockMaster` | JSON  | `UpdateBlockMasterDto` | - | - | - |
| POST | `/ipdconfig/addUpdateClassMaster` | `pas.IPDConfigServiceImpl.addUpdateClassMaster` | JSON  | `UpdateClassMasterDto` | - | - | - |
| POST | `/ipdconfig/addUpdateDietPlanDetails` | `pas.IPDConfigServiceImpl.addUpdateDietPlanDetails` | JSON  | `UpdateDietPlanDetailsDto` | - | - | - |
| POST | `/ipdconfig/addUpdateFoodItemMaster` | `pas.IPDConfigServiceImpl.addUpdateFoodItemMaster` | JSON  | `UpdateFoodItemMasterDto` | - | - | - |
| POST | `/ipdconfig/addUpdateRoomMaster` | `pas.IPDConfigServiceImpl.addUpdateRoomMaster` | JSON  | `UpdateRoomMasterDto` | - | - | - |
| POST | `/ipdconfig/addUpdateWardMaster` | `pas.IPDConfigServiceImpl.addUpdateWardMaster` | JSON  | `UpdateWardMasterDto` | - | - | - |
| POST | `/ipdconfig/getBedCensusAndNonCensusList` | `pas.IPDConfigServiceImpl.getBedCensusAndNonCensusList` | JSON  | `List<BedCensusAndNonCensusListResultDto>` | Y | - | - |
| POST | `/ipdconfig/getBedMasterDetails` | `pas.IPDConfigServiceImpl.getBedMasterDetails` | JSON  | `BedMasterDetailsDto` | - | - | - |
| POST | `/ipdconfig/getBlockMasterDetails` | `pas.IPDConfigServiceImpl.getBlockMasterDetails` | JSON  | `BlockMasterDetailsDto` | - | - | - |
| POST | `/ipdconfig/getClassMasterDetails` | `pas.IPDConfigServiceImpl.getClassMasterDetails` | JSON  | `List<ClassMasterDetailsResultDto>` | Y | - | - |
| POST | `/ipdconfig/getDietPlan` | `pas.IPDConfigServiceImpl.getDietPlan` | JSON  | `DietPlanDto` | - | - | - |
| POST | `/ipdconfig/getFoodItemList` | `pas.IPDConfigServiceImpl.getFoodItemList` | JSON  | `FoodItemListDto` | Y | - | - |
| POST | `/ipdconfig/getIPDBedList` | `pas.IPDConfigServiceImpl.getIPDBedList` | JSON  | `List<IPDBedListResultDto>` | - | - | - |
| POST | `/ipdconfig/getRoomMasterDetails` | `pas.IPDConfigServiceImpl.getRoomMasterDetails` | JSON  | `List<RoomMasterDetailsResultDto>` | - | - | - |
| POST | `/ipdconfig/getWardMasterDetails` | `pas.IPDConfigServiceImpl.getWardMasterDetails` | JSON  | `List<WardMasterDetailsResultDto>` | - | - | - |
| PUT | `/ipdconfig/updateBedMasterStatus` | `pas.IPDConfigServiceImpl.updateBedMasterStatus` | JSON  | `BedMasterStatusDto` | - | - | - |
| PUT | `/ipdconfig/updateBlockMasterStatus` | `pas.IPDConfigServiceImpl.updateBlockMasterStatus` | JSON  | `BlockMasterStatusDto` | - | - | - |
| PUT | `/ipdconfig/updateClassMasterStatus` | `pas.IPDConfigServiceImpl.updateClassMasterStatus` | JSON  | `ClassMasterStatusDto` | Y | - | - |
| PUT | `/ipdconfig/updateDietPlanMaster` | `pas.IPDConfigServiceImpl.updateDietPlanMaster` | JSON  | `DietPlanMasterDto` | - | - | - |
| PUT | `/ipdconfig/updateDietPlanMasterStatus` | `pas.IPDConfigServiceImpl.updateDietPlanMasterStatus` | JSON  | `DietPlanMasterStatusDto` | - | - | - |
| PUT | `/ipdconfig/updateFoodItemMasterStatus` | `pas.IPDConfigServiceImpl.updateFoodItemMasterStatus` | JSON  | `FoodItemMasterStatusDto` | - | - | - |
| PUT | `/ipdconfig/updateRoomMasterStatus` | `pas.IPDConfigServiceImpl.updateRoomMasterStatus` | JSON  | `RoomMasterStatusDto` | - | - | - |
| PUT | `/ipdconfig/updateWardMasterStatus` | `pas.IPDConfigServiceImpl.updateWardMasterStatus` | JSON  | `WardMasterStatusDto` | - | - | - |
| POST | `/ipd/getDischargeList` | `pas.IPDServiceImpl.getDischargeList` | JSON  | `List<DischargeListResultDto>` | - | - | - |
| POST | `/ipd/getDischargeListByDate` | `pas.IPDServiceImpl.getDischargeListByDate` | JSON  | `List<DischargeListResultDto>` | - | - | - |
| GET | `/itemcategory/getItemByCategoryId` | `pas.ItemCategoryServiceImpl.getItemsByCategoryID` | Q  | `List<ItemByItemCategoryResultDto>` | - | - | - |
| GET | `/itemmaster/addItemtoItemClinic` | `pas.ItemMasterServiceImpl.addItemToItemClinic` | Q Q Q  | `ItemClinicDto` | Y | - | - |
| GET | `/itemmaster/getBrandMappedByItemId` | `pas.ItemMasterServiceImpl.getBrandMappedByItemId` | Q Q  | `List<Brandmaster>` | - | - | - |
| GET | `/itemmaster/getItemByName` | `pas.ItemMasterServiceImpl.getItemByName` | Q  | `List<ItemByNameResultDto>` | - | - | - |
| GET | `/itemmaster/getItemByNameAndUnitId` | `pas.ItemMasterServiceImpl.getItemByNameAndUnitId` | Q  | `List<ItemByNameResultDto>` | Y | - | - |
| GET | `/itemmaster/getItemByNameForCSSD` | `pas.ItemMasterServiceImpl.getItemByNameForCSSD` | Q Q  | `CSSDIndentView` | - | - | - |
| GET | `/itemmaster/getItemByNameIsAsset` | `pas.ItemMasterServiceImpl.getItemByNameIsAsset` | Q  | `List<ItemByNameImprovedResultDto>` | - | - | - |
| GET | `/itemmaster/getItemDetailsByItemId` | `pas.ItemMasterServiceImpl.` |  | `?` | - | - | - |
| GET | `/itemmaster/getItemNameForMedGas` | `pas.ItemMasterServiceImpl.getItemNameForMedGas` | Q  | `List<Equipmentdetail>` | - | - | - |
| GET | `/itemmaster/getItemSupplierList` | `pas.ItemMasterServiceImpl.getItemSupplierList` | Q Q  | `List<ItemSupplier>` | - | - | - |
| GET | `/itemmaster/getItemTaxList` | `pas.ItemMasterServiceImpl.getItemTaxList` | Q Q  | `List<ItemTaxListResultDto>` | Y | - | - |
| GET | `/itemmaster/getKitItemByKitIdForCSSD` | `pas.ItemMasterServiceImpl.getKitItemByKitIdCSSDIndentResultDto` | Q  | `List<GetKitItemByKitIdCSSDIndentResultDto>` | - | - | - |
| GET | `/itemmaster/saveItemBrandMapping` | `pas.ItemMasterServiceImpl.saveItemBrandMapping` | Q Q  | `void` | Y | - | - |
| POST | `/itemmaster/addEquipmentDetailMaster` | `pas.ItemMasterServiceImpl.addEquipmentDetailMaster` | JSON  | `EquipmentDetailMasterDto` | - | - | - |
| POST | `/itemmaster/addInsuranceDetails` | `pas.ItemMasterServiceImpl.addInsuranceDetails` | JSON  | `InsuranceDetailsDto` | - | - | - |
| POST | `/itemmaster/addtemEnquiry` | `pas.ItemMasterServiceImpl.addtemEnquiry` | JSON  | `TemEnquiryDto` | - | - | - |
| POST | `/itemmaster/getAllEnquirys` | `pas.ItemMasterServiceImpl.getAllEnquirys` | JSON Q Q  | `List<GetAllEnquiriesResultDto>` | - | - | - |
| POST | `/itemmaster/getItemListByItemName` | `pas.ItemMasterServiceImpl.getItemListByItemName` | JSON  | `List<ItemListByItemNameResultDto>` | Y | - | - |
| POST | `/itemmaster/getItemListForSearch` | `pas.ItemMasterServiceImpl.getItemListForSearch` | JSON  | `ItemListForSearchDto` | - | - | - |
| POST | `/itemmaster/getItemMst` | `pas.ItemMasterServiceImpl.getDepartmentsByUnit` | JSON  | `List<DepartmentMst>` | - | - | - |
| POST | `/itemmaster/getScrapSaleDetails` | `pas.ItemMasterServiceImpl.getScrapSaleDetails` | JSON  | `List<ScrapSaleDetailsResultDto>` | - | - | - |
| POST | `/itemmaster/syncJantaItemMaster` | `pas.ItemMasterServiceImpl.syncJantaItemMaster` | JSON  | `Map<Long,Long>` | - | - | - |
| PUT | `/itemmaster/deleteItemmasterListById/{id}` | `pas.ItemMasterServiceImpl.deleteItemmasterListById` | P  | `boolean` | - | - | - |
| PUT | `/itemmaster/updateDiscardReasonById` | `pas.ItemMasterServiceImpl.updateDiscardReasonById` | Q Q  | `void` | Y | - | - |
| GET | `/itemsales/getAvailableItemStockStoreWise` | `pas.ItemSalesServiceImpl.getAvailableItemStockStoreWise` | Q Q  | `java.math.BigDecimal` | - | - | - |
| GET | `/itemsales/getItemByStoreCategory` | `pas.ItemSalesServiceImpl.getItemByStoreCategory` | Q  | `List<Long>` | - | - | - |
| GET | `/itemsales/getItemSaleByItemSale` | `pas.ItemSalesServiceImpl.` |  | `?` | - | - | - |
| GET | `/itemsales/getItemSalesReturnDetails` | `pas.ItemSalesServiceImpl.getItemSalesReturnDetails` | Q Q  | `List<ItemSalesReturnDetailsResultDto>` | - | - | - |
| POST | `/itemsales/addItemSales` | `pas.ItemSalesServiceImpl.addItemSales` | JSON  | `ItemSalesDto` | - | - | - |
| POST | `/itemsales/addItemSalesDetails` | `pas.ItemSalesServiceImpl.addItemSalesDetails` | JSON  | `AddItemSalesDetailsDto` | - | - | - |
| POST | `/itemsales/addtemSaleReturn` | `pas.ItemSalesServiceImpl.addtemSaleReturn` | JSON  | `TemSaleReturnDto` | - | - | - |
| POST | `/itemsales/addtemSaleReturnDetails` | `pas.ItemSalesServiceImpl.addtemSaleReturnDetails` | JSON  | `List<TemSaleReturnDetailsDto>` | - | - | - |
| POST | `/itemsales/getItemSales` | `pas.ItemSalesServiceImpl.getItemSales` | JSON Q  | `List<GetItemSalesResultDto>` | Y | - | - |
| POST | `/itemsales/getItemSalesDetails` | `pas.ItemSalesServiceImpl.getItemSalesDetails` | JSON  | `List<ItemSalesDetailsResultDto>` | - | - | - |
| POST | `/itemsales/getItemSalesForCancelBill` | `pas.ItemSalesServiceImpl.getItemSalesForCancelBill` | JSON  | `List<ItemSaleReturnsForIPResultDto>` | - | - | - |
| POST | `/itemsales/getItemSalesForOT` | `pas.ItemSalesServiceImpl.getItemSalesForOT` | JSON  | `List<GetItemSalesOTResultDtoOld>` | - | - | - |
| POST | `/itemsales/getItemSalesReturn` | `pas.ItemSalesServiceImpl.getItemSalesReturn` | JSON  | `List<ItemSalesReturnResultDto1>` | Y | - | - |
| POST | `/itemsales/getItemSalesReturnForIP` | `pas.ItemSalesServiceImpl.` |  | `?` | - | - | - |
| POST | `/itemsales/getItemsaleWholeDetailsByAdmId` | `pas.ItemSalesServiceImpl.` |  | `?` | - | - | - |
| GET | `/itemstock/findIndentItemMovementDetails` | `pas.ItemStockServiceImpl.findIndentItemUtilizationDetails` | Q Q  | `ItemUtilizationDetailsView` | - | - | - |
| GET | `/itemstock/getIndentItemMovementDetails` | `pas.ItemStockServiceImpl.getIndentItemUtilization` | Q Q  | `Map<Long,List<ItemUtilizationView>>` | - | - | - |
| GET | `/itemstock/getItemBySupplierAndStore` | `pas.ItemStockServiceImpl.getItemBySupplierAndStore` | Q  | `List<ItemsBySupplierStoreResultDto>` | Y | - | - |
| GET | `/itemstock/getItemRCDetails` | `pas.ItemStockServiceImpl.getItemRCComparisonDeatils` | Q  | `List<RateContractView>` | - | - | - |
| POST | `/itemstock/addItemStock` | `pas.ItemStockServiceImpl.saveItemStock` | JSON  | `List<ItemStockDto>` | - | - | - |
| POST | `/itemstock/getAllItemCategory` | `pas.ItemStockServiceImpl.getAllItemCategory` | - | `List<Itemcategory>` | Y | - | - |
| POST | `/itemstock/getItemBatchListForSearchWithTax` | `pas.ItemStockServiceImpl.getItemBatchListForSearchWithTax` | JSON Q  | `List<ItemBatchListForSearchwithTaxResultDto>` | Y | - | - |
| POST | `/itemstock/getItemBatchListForSearchWithTaxForPo` | `pas.ItemStockServiceImpl.getItemBatchListForSearchWithTaxForPo` | JSON  | `List<ItemBatchListForSearchwithTaxResultDto>` | - | - | - |
| GET | `/masterConfigService/authenticateDisplayUnit` | `pas.MasterServiceImpl.authenticateDisplayUnit` | Q  | `DisplayUnitMaster` | Y | - | - |
| GET | `/masterConfigService/authenticateUnit` | `pas.MasterServiceImpl.authenticateUnit` | Q  | `Unitmaster` | - | - | - |
| GET | `/masterConfigService/checkChartSequence` | `pas.MasterServiceImpl.checkChartSequence` | Q Q Q  | `Number` | - | - | - |
| GET | `/masterConfigService/checkChartService` | `pas.MasterServiceImpl.checkChartService` | Q Q Q  | `Number` | - | - | - |
| GET | `/masterConfigService/checkDescriptionInCasetypeMaster` | `pas.MasterServiceImpl.checkDescriptionInCasetypeMaster` | Q Q  | `Number` | Y | - | - |
| GET | `/masterConfigService/checkDescriptionInHolidayMaster` | `pas.MasterServiceImpl.checkDescriptionInHolidayMaster` | Q Q Q  | `Number` | Y | - | - |
| GET | `/masterConfigService/checkIfItemTransaction` | `pas.MasterServiceImpl.checkIfItemTransaction` | Q  | `Boolean` | - | - | - |
| GET | `/masterConfigService/checkSubSpecialization` | `pas.MasterServiceImpl.checkSubSpecialization` | Q Q  | `Number` | Y | - | - |
| GET | `/masterConfigService/checkUnitwiseItemNameForAsset` | `pas.MasterServiceImpl.checkUnitwiseItemNameForAsset` | Q Q  | `Number` | - | - | - |
| GET | `/masterConfigService/createBloodStoreSequences` | `pas.MasterServiceImpl.createBloodStoreSequences` | Q  | `void` | Y | - | - |
| GET | `/masterConfigService/editProcedureCheklistDetail/{id}` | `pas.MasterServiceImpl.editProcedureCheklistDetail` | P  | `Procedurecheklistdetails` | Y | - | - |
| GET | `/masterConfigService/exportRuleSetFile` | `pas.MasterServiceImpl.exportBillServices` | Q ?  | `void` | - | - | - |
| GET | `/masterConfigService/getAllDiagnosisMaster` | `pas.MasterServiceImpl.getAllDiagnosisMaster` | Q Q Q  | `List<DiagnosisMaster>` | Y | - | - |
| GET | `/masterConfigService/getCentralStoreList` | `pas.MasterServiceImpl.getCentralStoreList` | - | `List<Store>` | - | - | - |
| GET | `/masterConfigService/getChartDetailsById/{id}` | `pas.MasterServiceImpl.getChartDetailsById` | P  | `List<Growthchartdetails>` | - | - | - |
| GET | `/masterConfigService/getChartMenuById/{id}` | `pas.MasterServiceImpl.getChartMenuById` | P  | `GrowthChartMenu` | Y | - | - |
| GET | `/masterConfigService/getClinicalIEN` | `pas.MasterServiceImpl.getClinicalIEN` | Q Q  | `Number` | - | - | - |
| GET | `/masterConfigService/getDiagnosisMaster/{id}` | `pas.MasterServiceImpl.getAllDiagnosisMaster` | P  | `DiagnosisMaster` | - | - | - |
| GET | `/masterConfigService/getDietTypeList` | `pas.MasterServiceImpl.getDietTypeList` | Q Q  | `List<Servicemaster>` | - | - | - |
| GET | `/masterConfigService/getDrugInteractionList` | `pas.MasterServiceImpl.getDrugInteractionList` | Q Q Q  | `List<Druginteractions>` | Y | - | - |
| GET | `/masterConfigService/getEmailTemplateDetailsByTemplateId` | `pas.MasterServiceImpl.getEmailTemplateDetailsByTemplateId` | Q Q  | `EmailTemplateDetails` | - | - | - |
| GET | `/masterConfigService/getEmailtemplates` | `pas.MasterServiceImpl.getEmailtemplates` | Q Q Q  | `List<EmailTemplate>` | - | - | - |
| GET | `/masterConfigService/getGrievanceSeverityTemplateDetailsById` | `pas.MasterServiceImpl.getGrievanceSeverityTemplateDetailsById` | Q  | `GrievanceSeverityTemplateDetails` | - | - | - |
| GET | `/masterConfigService/getGrowthChartMenuList` | `pas.MasterServiceImpl.getGrowthChartMenuList` | Q Q Q  | `List<GrowthChartMenu>` | Y | - | - |
| GET | `/masterConfigService/getIHMSMasters` | `pas.MasterServiceImpl.getIHMSMasters` | - | `AllMasterDataView` | Y | - | - |
| GET | `/masterConfigService/getImmunAgeGrpMasterById/{id}` | `pas.MasterServiceImpl.getImmunAgeGrpMasterById` | P  | `Immunizationagegroupmaster` | Y | - | - |
| GET | `/masterConfigService/getInboundList/{msgid}` | `pas.MasterServiceImpl.getInboundList` | P  | `Inboundhis` | - | - | - |
| GET | `/masterConfigService/getItemGroupByAsset` | `pas.MasterServiceImpl.getItemGroupByAsset` | - | `List<Itemgroup>` | Y | - | - |
| GET | `/masterConfigService/getItemMasterById/{id}` | `pas.MasterServiceImpl.getItemMasterById` | P  | `Itemmaster` | - | - | - |
| GET | `/masterConfigService/getItemmasterListAsset` | `pas.MasterServiceImpl.getItemmasterListAsset` | Q Q Q  | `List<Itemmaster>` | - | - | - |
| GET | `/masterConfigService/getListOfAuditEventIds` | `pas.MasterServiceImpl.getListOfAuditEventIds` | Q  | `List<String>` | - | - | - |
| GET | `/masterConfigService/getListOfInboundmsgs` | `pas.MasterServiceImpl.getListOfInboundmsgs` | Q  | `List<Inboundhis>` | Y | - | - |
| GET | `/masterConfigService/getListOfOutboundmsgs` | `pas.MasterServiceImpl.getListOfOutboundmsgs` | Q  | `List<Outboundhis>` | - | - | - |
| GET | `/masterConfigService/getModuleNameMasterById` | `pas.MasterServiceImpl.getModuleNameMasterDetails` | Q  | `String` | Y | - | - |
| GET | `/masterConfigService/getOutboundList/{msgid}` | `pas.MasterServiceImpl.getOutboundList` | P  | `Outboundhis` | - | - | - |
| GET | `/masterConfigService/getParentUnitList` | `pas.MasterServiceImpl.getParentUnitList` | - | `List<Unitmaster>` | Y | - | - |
| GET | `/masterConfigService/getSupplierListByCategory` | `pas.MasterServiceImpl.getSupplierListByCategory` | Q Q  | `List<ItemSupplier>` | - | - | - |
| GET | `/masterConfigService/getUnitsForAudits` | `pas.MasterServiceImpl.getUnitsForAudits` | Q  | `List<Unitmaster>` | - | - | - |
| GET | `/masterConfigService/getVirtualUnitList` | `pas.MasterServiceImpl.getVirtualUnitList` | - | `List<Unitmaster>` | - | - | - |
| GET | `/masterConfigService/getVitalType` | `pas.MasterServiceImpl.getVitalType` | Q Q  | `List<Vitaltype>` | Y | - | - |
| GET | `/masterConfigService/reports_mapping` | `pas.MasterServiceImpl.getReportsMapping` | - | `Map<String, String[]>` | Y | - | - |
| GET | `/masterConfigService/updateStoreIdInUnitMaster` | `pas.MasterServiceImpl.updateStoreIdInUnitMaster` | Q Q  | `void` | - | - | - |
| POST | `/masterConfigService/addandUpdateChartDetails` | `pas.MasterServiceImpl.addandUpdateChartDetails` | JSON  | `void` | - | - | - |
| POST | `/masterConfigService/addandUpdateChartMenu` | `pas.MasterServiceImpl.addandUpdateChartMenu` | JSON  | `void` | Y | - | - |
| POST | `/masterConfigService/addAndUpdateImmunAgeGrpMaster` | `pas.MasterServiceImpl.addAndUpdateImmunAgeGrpMaster` | JSON  | `void` | Y | - | - |
| POST | `/masterConfigService/addDiagnosisMaster` | `pas.MasterServiceImpl.addDiagnosisMaster` | JSON  | `DiagnosisMaster` | - | - | - |
| POST | `/masterConfigService/addDisplayUnitMaster` | `pas.MasterServiceImpl.addDisplayUnitMaster` | JSON  | `DisplayUnitMaster` | Y | - | - |
| POST | `/masterConfigService/addEmailTemplate` | `pas.MasterServiceImpl.addEmailTemplate` | JSON  | `AddEmailTemplateDto` | - | - | - |
| POST | `/masterConfigService/addItemSupplierBycategory` | `pas.MasterServiceImpl.addItemSupplierBycategory` | JSON  | `void` | - | - | - |
| POST | `/masterConfigService/addPackageWithDefinationDetails` | `pas.MasterServiceImpl.addPackageWithDefinationDetails` | JSON  | `Packagemaster` | - | - | - |
| POST | `/masterConfigService/addProcedureCheklistDetail` | `pas.MasterServiceImpl.OTprocedurechecklistitem` | JSON  | `void` | Y | - | - |
| POST | `/masterConfigService/addUpdateEmailTemplateDetails` | `pas.MasterServiceImpl.addUpdateEmailTemplateDetails` | JSON  | `EmailTemplateDetails` | - | - | - |
| POST | `/masterConfigService/addUpdateGrievanceTemplateDetails` | `pas.MasterServiceImpl.addUpdateGrievanceTemplateDetails` | JSON  | `GrievanceSeverityTemplateDetails` | - | - | - |
| POST | `/masterConfigService/addUpdatePctsregistration` | `pas.MasterServiceImpl.addUpdatePctsregistration` | JSON  | `void` | Y | - | - |
| POST | `/masterConfigService/addUpdateSingleVitalType` | `pas.MasterServiceImpl.addUpdateSingleVitalType` | JSON  | `void` | - | - | - |
| POST | `/masterConfigService/configureOfflineUnit` | `pas.MasterServiceImpl.configureOfflineUnit` | Q Q Q  | `OfflineDataDto` | Y | - | - |
| POST | `/masterConfigService/getEmailTemplateListForSearch` | `pas.MasterServiceImpl.getEmailTemplateListForSearch` | JSON  | `List<EmailTemplateListForSearchResultDto>` | - | - | - |
| POST | `/masterConfigService/mapUnitwiseServiceForLMISIntegration` | `pas.MasterServiceImpl.mapUnitwiseServiceForLMISIntegration` | JSON  | `Boolean` | - | - | - |
| POST | `/masterConfigService/printAudit` | `pas.MasterServiceImpl.printAudit` | JSON Q Q Q  | `AuditView` | Y | - | - |
| POST | `/masterConfigService/saveOutboundById` | `pas.MasterServiceImpl.saveOutboundById` | JSON  | `void` | Y | - | - |
| POST | `/masterConfigService/updateDiagnosisMaster/{id}` | `pas.MasterServiceImpl.updateDiagnosisMaster` | JSON P  | `DiagnosisMaster` | Y | - | - |
| POST | `/masterConfigService/updateLanglabel` | `pas.MasterServiceImpl.updateLanglabel` | JSON  | `void` | Y | - | - |
| PUT | `/masterConfigService/deleteBedMaster/{id}` | `pas.MasterServiceImpl.deleteBedMaster` | P  | `boolean` | - | - | - |
| PUT | `/masterConfigService/deleteChartMenuById/{id}` | `pas.MasterServiceImpl.deleteChartMenuById` | P  | `void` | - | - | - |
| PUT | `/masterConfigService/deleteHospitalDepartment/{id}` | `pas.MasterServiceImpl.deleteHospitalDepartment` | Q P  | `boolean` | Y | - | - |
| PUT | `/masterConfigService/deleteImmuneAgeGroupById/{id}` | `pas.MasterServiceImpl.deleteImmuneAgeGroupById` | P  | `void` | - | - | - |
| PUT | `/masterConfigService/deleteProcedureChecklistDetails/{id}` | `pas.MasterServiceImpl.deleteProcedureChecklistDetails` | P  | `void` | Y | - | - |
| PUT | `/masterConfigService/deleteWardBedMaster/{id}` | `pas.MasterServiceImpl.deleteWardBedMaster` | P Q  | `void` | Y | - | - |
| PUT | `/masterConfigService/updateDisplayUnitMaster/{id}` | `pas.MasterServiceImpl.updateDisplayUnitMaster` | JSON P  | `DisplayUnitMaster` | - | - | - |
| PUT | `/masterConfigService/updateInboundById/{msgid}` | `pas.MasterServiceImpl.updateInboundById` | P JSON  | `void` | - | - | - |
| PUT | `/masterConfigServicecheckIsAdministrativeExists` | `pas.MasterServiceImpl.checkIsAdministrativeExists` | - | `boolean` | Y | - | - |
| PUT | `/masterConfigServicedeleteDiagnosisMaster/{id}` | `pas.MasterServiceImpl.deleteDiagnosisMaster` | P  | `void` | Y | - | - |
| GET | `/materialconsumption/getMaterialConsumptionItem` | `pas.MaterialConsumptionServiceImpl.` |  | `?` | - | - | - |
| POST | `/materialconsumption/getDepartmentalMaterialConsumption` | `pas.MaterialConsumptionServiceImpl.getDepartmentalMaterialConsumption` | JSON Q  | `List<GetMaterialConsumptionResultDto>` | - | - | - |
| POST | `/materialconsumption/getMaterialConsumption` | `pas.MaterialConsumptionServiceImpl.getMaterialConsumption` | JSON Q  | `List<GetMaterialConsumptionResultDto>` | - | - | - |
| GET | `/nursingstationdefination/getWardByNursingStation` | `pas.NursingStationDefinationServiceImpl.` |  | `?` | - | - | - |
| POST | `/nursingstationdefination/getNursingStationDefMaster` | `pas.NursingStationDefinationServiceImpl.getNursingStationDefMaster` | JSON  | `List<NursingStationDefMasResultDto>` | - | - | - |
| POST | `/openingbalance/addOpeningbalancedetails` | `pas.OpeningBalanceServiceImpl.getStockDetailsForOpeningBalance` | JSON  | `AddOpeningBalView` | - | - | - |
| POST | `/openingbalance/getEquipOpeningBalListCount` | `pas.OpeningBalanceServiceImpl.getEquipOpeningBalListCount` | JSON Q  | `List<OpeningBalanceCountListResutDto>` | - | - | - |
| POST | `/openingbalance/getOpeningBalListForEquipment` | `pas.OpeningBalanceServiceImpl.getOpeningBalListForEquipment` | JSON Q  | `List<EquipmentOpeningBalanceResultDto>` | Y | - | - |
| POST | `/openingbalance/getStockDetailsForOpeningBalance` | `pas.OpeningBalanceServiceImpl.getStockDetailsForOpeningBalance` | JSON Q  | `StockDetailsOpeningBalanceResponse` | - | - | - |
| POST | `/openingbalance/getStockDetailsForOpeningBalListCount` | `pas.OpeningBalanceServiceImpl.getStockDetailsForOpeningBalListCount` | JSON Q  | `List<OpeningBalanceCountListResutDto>` | Y | - | - |
| POST | `/organismmaster/getOrganismDetailMaster` | `pas.OrganismMasterServiceImpl.getOrganismDetailMaster` | JSON  | `List<GetOrganismMasterDetailResultDto>` | - | - | - |
| POST | `/organismmaster/getOrganismMaster` | `pas.OrganismMasterServiceImpl.getOrganismMaster` | JSON  | `List<OrganismMasterResultDto_2>` | - | - | - |
| GET | `/otconfig/checkSurrgeryHeadsUnitWise` | `pas.OtConfigServiceImpl.checkSurrgeryHeadsUnitWise` | Q  | `Number` | - | - | - |
| GET | `/otconfig/checkUnitWiseForOTtable` | `pas.OtConfigServiceImpl.checkUnitWiseForOTtable` | Q  | `Map<String , Number>` | - | - | - |
| GET | `/otconfig/checkUnitWiseForOTtheatre` | `pas.OtConfigServiceImpl.checkUnitWiseForOTtheatre` | Q  | `Map<String , Number>` | - | - | - |
| GET | `/otconfig/getAllAvailableSlots` | `pas.OtConfigServiceImpl.getAllAvailableSlots` | Q Q  | `List<String>` | - | - | - |
| GET | `/otconfig/getAllUnmappedServices` | `pas.OtConfigServiceImpl.getAllUnmappedServices` | Q  | `List<Servicemaster>` | - | - | - |
| GET | `/otconfig/getConsentForProcedureID` | `pas.OtConfigServiceImpl.` |  | `?` | - | - | - |
| GET | `/otconfig/getEquipmentName` | `pas.OtConfigServiceImpl.getEquipmentName` | Q  | `List<Itemmaster>` | - | - | - |
| GET | `/otconfig/getIsMaintenanceDept` | `pas.OtConfigServiceImpl.getIsMaintenanceDept` | Q  | `List<Departmentmaster>` | Y | - | - |
| GET | `/otconfig/getKitName` | `pas.OtConfigServiceImpl.getKitName` | Q  | `List<Itemmaster>` | - | - | - |
| GET | `/otconfig/getOTScheduleTime` | `pas.OtConfigServiceImpl.` |  | `?` | - | - | - |
| GET | `/otconfig/getOttableById/{id}` | `pas.OtConfigServiceImpl.getOttableById` | P  | `Ottablemaster` | - | - | - |
| GET | `/otconfig/getOttheatreById/{id}` | `pas.OtConfigServiceImpl.getOttheatreById` | P  | `Ottheatremaster` | Y | - | - |
| GET | `/otconfig/getProcDetailsByProcID` | `pas.OtConfigServiceImpl.getProcDetailsByProcID` | Q  | `List<ProcDetailsByProcIDResultDto>` | - | - | - |
| GET | `/otconfig/getProcDetailsByProcIDForConsentDtls` | `pas.OtConfigServiceImpl.` |  | `?` | - | - | - |
| GET | `/otconfig/getProcDetailsByProcIDForInstruction` | `pas.OtConfigServiceImpl.` |  | `?` | - | - | - |
| GET | `/otconfig/getProcDetailsByProcIDForItemIssued` | `pas.OtConfigServiceImpl.` |  | `?` | Y | - | - |
| GET | `/otconfig/getProcDetailsByProcIDForserviceDetails` | `pas.OtConfigServiceImpl.` |  | `?` | - | - | - |
| GET | `/otconfig/getProcedureById/{id}` | `pas.OtConfigServiceImpl.getProcedureById` | P  | `Proceduremaster` | - | - | - |
| GET | `/otconfig/getStaffForOTScheduling` | `pas.OtConfigServiceImpl.getStaffForOTScheduling` | - | `List<StaffForOTSchedulingResultDto>` | - | - | - |
| POST | `/otconfig/addAndUpdateProcedureMaster` | `pas.OtConfigServiceImpl.addAndUpdateProcedureMaster` | JSON  | `void` | Y | - | - |
| POST | `/otconfig/addConsentDetails` | `pas.OtConfigServiceImpl.addConsentDetails` | JSON  | `ConsentDetailsDto` | - | - | - |
| POST | `/otconfig/addOperationTable` | `pas.OtConfigServiceImpl.addOperationTable` | JSON ?  | `Ottablemaster` | Y | - | - |
| POST | `/otconfig/addOperationTheatre` | `pas.OtConfigServiceImpl.addOperationTheatre` | JSON ?  | `Ottheatremaster` | - | - | - |
| POST | `/otconfig/AddOTDetailsInstruction` | `pas.OtConfigServiceImpl.addOtDetailsForInstruction` | JSON  | `void` | - | - | - |
| POST | `/otconfig/getPatientConsents` | `pas.OtConfigServiceImpl.` |  | `?` | - | - | - |
| POST | `/otconfig/getProcedureMasterList` | `pas.OtConfigServiceImpl.getProcedureMasterList` | JSON  | `List<ProcedureMasterListResultDto>` | Y | - | - |
| POST | `/otconfig/getScheduleDetails` | `pas.OtConfigServiceImpl.getScheduleDetails` | Q Q Q  | `OTSchedulerView` | - | - | - |
| POST | `/otconfig/getSurgeryDoctors` | `pas.OtConfigServiceImpl.getSurgeryDoctors` | JSON  | `List<ScheduledDoctorForOTSchedulingResultDto>` | - | - | - |
| PUT | `/otconfig/deleteProcedureById/{id}` | `pas.OtConfigServiceImpl.deleteProcedureById` | P  | `void` | - | - | - |
| PUT | `/otconfig/updateOperationTable/{id}` | `pas.OtConfigServiceImpl.updateOperationTable` | JSON P ?  | `Ottablemaster` | Y | - | - |
| PUT | `/otconfig/updateOperationTheatre/{id}` | `pas.OtConfigServiceImpl.updateOperationTheatre` | JSON P ?  | `Ottheatremaster` | Y | - | - |
| PUT | `/otconfig/updateStatusOperationTable/{id}` | `pas.OtConfigServiceImpl.updateStatusOperationTable` | P ?  | `void` | Y | - | - |
| PUT | `/otconfig/updateStatusOperationTheatre/{id}` | `pas.OtConfigServiceImpl.updateStatusOperationTheatre` | P ?  | `void` | Y | - | - |
| GET | `/otdetails/getConsentDetailsForConsetID` | `pas.OTDetailsServiceImpl.getConsentDetailsForConsetID` | Q  | `List<ConsentDetailsForConsetIDResultDto>` | - | - | - |
| GET | `/otdetails/getOTDetailsForOTDetailsID` | `pas.OTDetailsServiceImpl.getOTDetailsForOTDetailsID` | Q  | `List<OTDetailsForOTDetailsIDResultDto_1>` | - | - | - |
| GET | `/otdetails/getOTDetailsForSurgery` | `pas.OTDetailsServiceImpl.` |  | `?` | - | - | - |
| GET | `/otscheduler/addPacNoteIen` | `pas.OTSchedulerServiceImpl.addPacNoteIen` | Q Q  | `void` | - | - | - |
| GET | `/otscheduler/checkAvailabilityOfBookingSlots` | `pas.OTSchedulerServiceImpl.checkAvailabilityOfBookingSlots_backup` | Q Q  | `List<OTBookingTimeSlotsDetailsDto>` | - | - | - |
| GET | `/otscheduler/checkDoctorAllreadyBookedForSameTime` | `pas.OTSchedulerServiceImpl.checkDoctorAllreadyBookedForSameTime` | Q Q  | `Boolean` | - | - | - |
| GET | `/otscheduler/checkOTAvailability` | `pas.OTSchedulerServiceImpl.checkOTAvailability` | Q Q  | `List<OTScheduleTimeResultDto>` | - | - | - |
| GET | `/otscheduler/checkOTSchedule` | `pas.OTSchedulerServiceImpl.checkOTSchedule` | Q Q Q  | `Boolean` | - | - | - |
| GET | `/otscheduler/checkOtThreaterInOTTable` | `pas.OTSchedulerServiceImpl.checkOtThreaterInOTTable` | Q  | `Number` | Y | - | - |
| GET | `/otscheduler/checkPatientAdmissionStatus` | `pas.OTSchedulerServiceImpl.getActiveAdmissionOfRegisteredPatient` | Q  | `List<ActiveAdmissionOfRegisteredPatientResultDto>` | Y | - | - |
| GET | `/otscheduler/checkPatientAllreadyBookedForSameTime` | `pas.OTSchedulerServiceImpl.checkPatientAllreadyBookedForSameTime` | Q Q  | `List<OTBookingTimeSlotsDetailsDto>` | Y | - | - |
| GET | `/otscheduler/checkPatientForOt` | `pas.OTSchedulerServiceImpl.checkPatientForOt` | Q Q Q  | `Boolean` | - | - | - |
| GET | `/otscheduler/geProcedureWiseEquipmentList` | `pas.OTSchedulerServiceImpl.geProcedureWiseEquipmentList` | Q  | `List<GetProcedureEquipmentList>` | - | - | - |
| GET | `/otscheduler/getDoctorBySurgeryHead` | `pas.OTSchedulerServiceImpl.getDoctorBySurgeryHead` | Q  | `List<OTSurgeryDoctorsDto>` | - | - | - |
| GET | `/otscheduler/getOrderedPatientProcedure` | `pas.OTSchedulerServiceImpl.getOrderedPatientProcedure` | Q Q Q  | `List<PatientProcedureDto>` | Y | - | - |
| GET | `/otscheduler/getOtAnesthesiaDetails` | `pas.OTSchedulerServiceImpl.getOtAnesthesiaDetails` | Q Q  | `List<PatientProcedureAnesthesiadetails>` | Y | - | - |
| GET | `/otscheduler/getotDetails` | `pas.OTSchedulerServiceImpl.getotDetails` | Q Q  | `AddPatientProcdureDto` | Y | - | - |
| GET | `/otscheduler/getOTDetailsSurgeryNotes` | `pas.OTSchedulerServiceImpl.getOTDetailsSurgeryNotes` | Q  | `List<OTDetailsSurgeryNotes>` | Y | - | - |
| GET | `/otscheduler/getOtEquipmentDetails` | `pas.OTSchedulerServiceImpl.getOtEquipmentDetails` | Q Q  | `List<PatientProcedureEquipmentDetailsResultDto>` | Y | - | - |
| GET | `/otscheduler/getOTNotesDetails` | `pas.OTSchedulerServiceImpl.getOTNotesDetails` | Q Q Q Q  | `PatientNote` | - | - | - |
| GET | `/otscheduler/getOTSchedulingListById/{id}` | `pas.OTSchedulerServiceImpl.getOTSchedulingListById` | P  | `Otschedule` | Y | - | - |
| GET | `/otscheduler/getOTStaffDetails` | `pas.OTSchedulerServiceImpl.getOTStaffDetails` | Q Q  | `List<ScheduledStaffForOTSchedulingResultDto>` | - | - | - |
| GET | `/otscheduler/getOTSurgeryDocTypeList` | `pas.OTSchedulerServiceImpl.getOTSurgeryDocTypeList` | - | `List<Surgeryheadmaster>` | - | - | - |
| GET | `/otscheduler/getOtTablesByTheaterId` | `pas.OTSchedulerServiceImpl.getOtTablesByTheatreId` | Q Q  | `Map<Long, String>` | - | - | - |
| GET | `/otscheduler/getPacDetailsByScheduleId` | `pas.OTSchedulerServiceImpl.getPacDetailsByScheduleId` | Q  | `PatientProcedureScheduleListResultDto` | - | - | - |
| GET | `/otscheduler/getPatientProcedureListByScheduleId` | `pas.OTSchedulerServiceImpl.getPatientProcedureListByScheduleId` | Q  | `List<PatientProcedureDto>` | - | - | - |
| GET | `/otscheduler/getPatientScheduleAnasthesiaDetails` | `pas.OTSchedulerServiceImpl.getPatientScheduleAnasthesiaDetails` | Q  | `List<PatientScheduleAnasthesiaDetails>` | Y | - | - |
| GET | `/otscheduler/getPatientScheduleByVisitAdmId` | `pas.OTSchedulerServiceImpl.getPatientScheduleByVisitAdmId` | Q  | `List<AddPatientProcdureDto>` | Y | - | - |
| GET | `/otscheduler/getPatientScheduleTheaterDetails` | `pas.OTSchedulerServiceImpl.getPatientScheduleTheaterDetails` | Q  | `PatientProcedureScheduleDto` | - | - | - |
| GET | `/otscheduler/getPendingPacScheduleByVisitAdmId` | `pas.OTSchedulerServiceImpl.getPendingPacScheduleByVisitAdmId` | Q  | `List<AddPatientProcdureDto>` | Y | - | - |
| GET | `/otscheduler/getProcedureChecklist` | `pas.OTSchedulerServiceImpl.getProcedureChecklist` | Q Q Q  | `List<PatientProcedureDto>` | - | - | - |
| GET | `/otscheduler/getProcedureEquipmentList` | `pas.OTSchedulerServiceImpl.GetProcedureEquipmentList` | Q  | `PatientProcedureDto` | Y | - | - |
| GET | `/otscheduler/getRetainBedId` | `pas.OTSchedulerServiceImpl.getRetainBedId` | Q  | `List<TransferResultDto>` | - | - | - |
| GET | `/otscheduler/getServiceRate` | `pas.OTSchedulerServiceImpl.getServicePriceDetails` | Q Q Q  | `ServiceListForBillResultDto` | - | - | - |
| GET | `/otscheduler/getSurgeryDetails` | `pas.OTSchedulerServiceImpl.getSurgeryDetails` | Q  | `UpdateOTSheetDetailsDto` | - | - | - |
| GET | `/otscheduler/getSurgeryDetailsNew` | `pas.OTSchedulerServiceImpl.getProcDetailsByProcID` | Q  | `List<GetSurgeryDetailsNewResultDto>` | - | - | - |
| GET | `/otscheduler/getSurgeryHead` | `pas.OTSchedulerServiceImpl.getSurgeryHead` | Q  | `List<SurgeryHeadDetail>` | Y | - | - |
| GET | `/otscheduler/getSurgeryHeadDetailsForBillEstimation` | `pas.OTSchedulerServiceImpl.getSurgeryHeadDetailsForBillEstimation` | Q Q Q  | `List<Surgeryheadmaster>` | - | - | - |
| GET | `/otscheduler/isCurrentProcedureismainprocedure` | `pas.OTSchedulerServiceImpl.getisCurrentProcedureismainprocedure` | Q Q  | `boolean` | Y | - | - |
| GET | `/otscheduler/updatePreAnaesthesiaChk` | `pas.OTSchedulerServiceImpl.updatePreAnaesthesiaChk` | Q Q  | `void` | Y | - | - |
| GET | `/otscheduler/viewSurgeryCharges` | `pas.OTSchedulerServiceImpl.viewSurgeryCharges` | Q  | `List<PatientProcedureDto>` | Y | - | - |
| POST | `/otscheduler/addanesthesiaDtlsForOtScheduling` | `pas.OTSchedulerServiceImpl.addanesthesiaDtlsForOtScheduling` | JSON  | `Boolean` | - | - | - |
| POST | `/otscheduler/addDoctorDetailsForOtScheduling` | `pas.OTSchedulerServiceImpl.addDoctorDetailsForOtScheduling` | JSON  | `Boolean` | Y | - | - |
| POST | `/otscheduler/addNewPatientProcdure` | `pas.OTSchedulerServiceImpl.addNewPatientProcdure` | JSON  | `OTSchedulerView` | - | - | - |
| POST | `/otscheduler/addOTSchedulingList` | `pas.OTSchedulerServiceImpl.addOTSchedulingList` | JSON  | `void` | - | - | - |
| POST | `/otscheduler/addPatientProcdure` | `pas.OTSchedulerServiceImpl.addPatientProcdure` | JSON  | `UpdatePatientProcedureScheduleMasterDto` | - | - | - |
| POST | `/otscheduler/addPatientProcedureChecklist` | `pas.OTSchedulerServiceImpl.addPatientProcedureChecklist` | JSON  | `void` | - | - | - |
| POST | `/otscheduler/addPatientScheduleAnasthesiaDetails` | `pas.OTSchedulerServiceImpl.addPatientScheduleAnasthesiaDetails` | JSON  | `void` | - | - | - |
| POST | `/otscheduler/addProcedureEquipment` | `pas.OTSchedulerServiceImpl.addProcedureEquipment` | JSON  | `void` | - | - | - |
| POST | `/otscheduler/addStaffDetailsForOt` | `pas.OTSchedulerServiceImpl.addStaffDetailsForOt` | JSON  | `List<UpdatePatientProcStaffDetailsDto>` | - | - | - |
| POST | `/otscheduler/addSurgCharge` | `pas.OTSchedulerServiceImpl.getSurgServicePriceDetails` | JSON ? ? ?  | `SurgeryChargesDto` | Y | - | - |
| POST | `/otscheduler/addSurgeryChargesTemp` | `pas.OTSchedulerServiceImpl.addSurgeryChargesTemp` | JSON ?  | `void` | Y | - | - |
| POST | `/otscheduler/addUpdateOTDetailsSurgeryNotesDetails` | `pas.OTSchedulerServiceImpl.addUpdateOTDetailsSurgeryNotesDetails` | JSON  | `UpdateOTDetailsSurgeryNotesDetailsDto` | Y | - | - |
| POST | `/otscheduler/checkEqpAvailability` | `pas.OTSchedulerServiceImpl.checkEquipAvailability` | JSON  | `List<EqpAvlbResultForSurgScheduleDto>` | - | - | - |
| POST | `/otscheduler/checkSurgeryComp` | `pas.OTSchedulerServiceImpl.checkSurgeryComp` | JSON  | `List<CheckSurgeryCompDto>` | - | - | - |
| POST | `/otscheduler/getDetailsForReschdule` | `pas.OTSchedulerServiceImpl.getDetailsForReschdule` | JSON  | `Boolean` | - | - | - |
| POST | `/otscheduler/getDoctorList` | `pas.OTSchedulerServiceImpl.GetProcedureEquipmentList` | JSON  | `List<DoctorTimeResultDto>` | Y | - | - |
| POST | `/otscheduler/getOTScheduleTime` | `pas.OTSchedulerServiceImpl.getOTScheduleTime` | JSON  | `Map<java.util.Date, OTScheduleView>` | - | - | - |
| POST | `/otscheduler/getPatientProcedureRateDetails` | `pas.OTSchedulerServiceImpl.getPatientProcedureRateDetails` | JSON  | `OTProcedureRateView` | Y | - | - |
| POST | `/otscheduler/getPatientProcedureScheduleList` | `pas.OTSchedulerServiceImpl.` |  | `?` | - | - | - |
| POST | `/otscheduler/getPatientProcedureWisePrescriptionItems` | `pas.OTSchedulerServiceImpl.getPatientProcedureWisePrescriptionItems` | JSON  | `List<GetConsumableItemsForProcedureResultDto>` | - | - | - |
| POST | `/otscheduler/getProcedureMasterList` | `pas.OTSchedulerServiceImpl.getProcedureMasterList` | JSON  | `List<ProcedureMasterListResultDto>` | - | - | - |
| POST | `/otscheduler/getsurgCompList` | `pas.OTSchedulerServiceImpl.GetProcedureCompList` | JSON  | `List<SurgeryCompResultDto>` | - | - | - |
| POST | `/otscheduler/getSurgeryCompleteList` | `pas.OTSchedulerServiceImpl.getSurgeryCompleteList` | JSON  | `List<SurgeryCompResultDto>` | - | - | - |
| POST | `/otscheduler/getSurgeryHeadDetails` | `pas.OTSchedulerServiceImpl.getSurgeryHeadDetails` | JSON  | `List<SurgeryHeadDetailResult>` | Y | - | - |
| POST | `/otscheduler/getSurgServicePriceDetails` | `pas.OTSchedulerServiceImpl.getSurgServicePriceDetails` | Q Q Q  | `ServiceListForBillResultDto` | - | - | - |
| POST | `/otscheduler/printOTReport` | `pas.OTSchedulerServiceImpl.printOTReport` | JSON  | `String` | - | - | - |
| POST | `/otscheduler/printScheduleList` | `pas.OTSchedulerServiceImpl.printScheduleList` | JSON ?  | `OTSchedulerView` | Y | - | - |
| POST | `/otscheduler/removePatientProcedure` | `pas.OTSchedulerServiceImpl.removePatientProcdure` | JSON  | `OTSchedulerView` | Y | - | - |
| POST | `/otscheduler/saveSurgeryRateDetails` | `pas.OTSchedulerServiceImpl.saveSurgeryRateDetails` | JSON  | `Boolean` | - | - | - |
| POST | `/otscheduler/setOTScheduleDetails` | `pas.OTSchedulerServiceImpl.setOTScheduleDetails` | JSON  | `AllOTSchedulesDetailsDto` | Y | - | - |
| POST | `/otscheduler/updateOTNotes` | `pas.OTSchedulerServiceImpl.updateOTNotes` | JSON Q Q  | `Boolean` | - | - | - |
| POST | `/otscheduler/updateOTScheduleDetails` | `pas.OTSchedulerServiceImpl.updateOTScheduleDetails` | JSON  | `UpdateOTSheetDetailsDto` | Y | - | - |
| PUT | `/otscheduler/cancelSurgeryPatient` | `pas.OTSchedulerServiceImpl.cancelSurgeryPatient` | Q Q  | `PatientProcedureScheduleListDto` | Y | - | - |
| PUT | `/otscheduler/deleteOTSchedulingListById/{id}` | `pas.OTSchedulerServiceImpl.deleteOTSchedulingListById` | P  | `void` | - | - | - |
| PUT | `/otscheduler/getpatientProcedureScheduleByPatId` | `pas.OTSchedulerServiceImpl.getpatientProcedureScheduleByPatId` | Q Q  | `void` | - | - | - |
| PUT | `/otscheduler/removeAnesthesia` | `pas.OTSchedulerServiceImpl.removeAnesthesia` | Q Q  | `String` | Y | - | - |
| PUT | `/otscheduler/removeScheduleDoctorDetails` | `pas.OTSchedulerServiceImpl.removeScheduleDoctorDetails` | Q  | `String` | Y | - | - |
| PUT | `/otscheduler/removeScheduleStaffDetails` | `pas.OTSchedulerServiceImpl.removeScheduleStaffDetails` | Q  | `String` | Y | - | - |
| PUT | `/otscheduler/removeSelectedCssdItemDetail` | `pas.OTSchedulerServiceImpl.removeSelectedCssdItemDetail` | Q  | `String` | - | - | - |
| PUT | `/otscheduler/removeSelectedEquipmentDetails` | `pas.OTSchedulerServiceImpl.removeSelectedEquipmentDetails` | Q  | `String` | Y | - | - |
| PUT | `/otscheduler/updateForItemProcedures` | `pas.OTSchedulerServiceImpl.updateForItemProcedures` | Q  | `String` | Y | - | - |
| PUT | `/otscheduler/updateOTSchedulingList/{id}` | `pas.OTSchedulerServiceImpl.updateOTSList` | JSON P  | `Otschedule` | Y | - | - |
| GET | `/packageservice/checkServiceInPackageMaster` | `pas.PackageServiceServiceImpl.checkServiceInPackageMaster` | Q  | `Packagehealthplandefinationmaster` | - | - | - |
| GET | `/packageservice/getGroupPackagesMst` | `pas.PackageServiceServiceImpl.getGroupPackagesMst` | Q  | `GetPackageGroupView` | - | - | - |
| GET | `/packageservice/getItemCategoryPackagesMst` | `pas.PackageServiceServiceImpl.getItemCategoryPackagesMst` | Q  | `GetPackageItemCategory` | - | - | - |
| GET | `/packageservice/getMedicinePackagesMst` | `pas.PackageServiceServiceImpl.getMedicinePackagesMst` | Q  | `GetPackageMedicine` | - | - | - |
| GET | `/packageservice/getPackageDetails` | `pas.PackageServiceServiceImpl.getPackageDetails` | Q  | `List<Packagehealthplandefinationmaster>` | Y | - | - |
| GET | `/packageservice/getPackageDetailsById` | `pas.PackageServiceServiceImpl.getPackageDetailById` | Q  | `Packagehealthplandefinationmaster` | Y | - | - |
| GET | `/packageservice/getPackageDetailsByUnitId` | `pas.PackageServiceServiceImpl.getPackageDetailsByUnitId` | Q  | `List<Packagehealthplandefinationmaster>` | Y | - | - |
| GET | `/packageservice/getPackageServiceMst/{id}` | `pas.PackageServiceServiceImpl.getPackageServiceMst` | P  | `Packagehealthplandefinationmaster` | Y | - | - |
| GET | `/packageservice/getServiceDataInPackageMaster` | `pas.PackageServiceServiceImpl.getServiceDataInPackageMaster` | Q  | `Packagehealthplandefinationmaster` | - | - | - |
| GET | `/packageservice/getServicePackageMst` | `pas.PackageServiceServiceImpl.getServicePackageMst` | Q  | `GetServicePackageView` | Y | - | - |
| GET | `/packageservice/gettariffListByserviceID` | `pas.PackageServiceServiceImpl.getTariffListID` | Q Q  | `List<Tariffmaster>` | - | - | - |
| GET | `/packageservice/getTariffServicePackageByServiceId` | `pas.PackageServiceServiceImpl.getTariffServicePackageByServiceId` | Q  | `Servicemaster` | - | - | - |
| POST | `/packageservice/addGroupPackagesMst` | `pas.PackageServiceServiceImpl.addGroupPackagesMst` | JSON  | `void` | Y | - | - |
| POST | `/packageservice/addItemCategoryPackagesMst` | `pas.PackageServiceServiceImpl.addItemCategoryPackagesMst` | JSON  | `void` | Y | - | - |
| POST | `/packageservice/addMedicinePackagesMst` | `pas.PackageServiceServiceImpl.addMedicinePackagesMst` | JSON  | `void` | Y | - | - |
| POST | `/packageservice/addPackageDetails` | `pas.PackageServiceServiceImpl.addPackageDetails` | JSON  | `void` | - | - | - |
| POST | `/packageservice/addPackagesMst` | `pas.PackageServiceServiceImpl.addPackagesMst` | JSON  | `void` | - | - | - |
| PUT | `/packageservice/deletePackageDetailsById/{id}` | `pas.PackageServiceServiceImpl.deletePackageDetailById` | P  | `void` | - | - | - |
| PUT | `/packageservice/updateGroupPackagesMst` | `pas.PackageServiceServiceImpl.updateGroupPackagesMst` | JSON  | `List<Servicepackagegroupmaster>` | Y | - | - |
| PUT | `/packageservice/updateItemCategoryPackagesMst` | `pas.PackageServiceServiceImpl.updateItemCategoryPackagesMst` | JSON  | `List<Packageitemcategorymaster>` | - | - | - |
| PUT | `/packageservice/updateMedicinePackagesMst` | `pas.PackageServiceServiceImpl.updateMedicinePackagesMst` | JSON  | `List<Servicepackagemedicinedetails>` | Y | - | - |
| PUT | `/packageservice/updatePackageMstList` | `pas.PackageServiceServiceImpl.updatePackageMstList` | JSON  | `Packagehealthplandefinationmaster` | - | - | - |
| GET | `/patientbillestimation/getPatientBillEstimationDetails` | `pas.PatientBillEstimationServiceImpl.getPatientBillEstimationDetails` | Q  | `BillReceiptView` | - | - | - |
| POST | `/patientbillestimation/addPatientBillEstimationAndDetails` | `pas.PatientBillEstimationServiceImpl.addPatientBillEstimationAndDetails` | JSON  | `AddPatientBillEstimationView` | Y | - | - |
| POST | `/patientbillestimation/getAllEstimatedBills` | `pas.PatientBillEstimationServiceImpl.getAllEstimatedBills` | JSON  | `List<GetAllBillPatientEstimationResultDto>` | - | - | - |
| POST | `/patientbillestimation/getAllServiceForBillEstimation` | `pas.PatientBillEstimationServiceImpl.getAllServicesForBillEstimation` | JSON  | `List<ServiceListForBillResultDto>` | - | - | - |
| GET | `/patientrequisition/checkAutologousTypeOrders` | `pas.PatientRequisitionServiceImpl.checkAutologousTypeOrders` | Q Q Q  | `PatientBloodRequisition` | Y | - | - |
| GET | `/patientrequisition/getPendingBloodRequisition` | `pas.PatientRequisitionServiceImpl.getPendingBloodRequisition` | Q Q  | `List<PatientRequsitionListResultDto>` | - | - | - |
| GET | `/patientrequisition/getRequestionDetailsById` | `pas.PatientRequisitionServiceImpl.getRequestionDetailsById` | Q Q  | `List<GetPatientRequisitionDetailsById>` | - | - | - |
| POST | `/patientrequisition/addBloodComponantForRequisition` | `pas.PatientRequisitionServiceImpl.addBloodComponantForRequisition` | JSON  | `void` | - | - | - |
| POST | `/patientrequisition/addPatientBloodRequisition` | `pas.PatientRequisitionServiceImpl.addPatientBloodRequisition` | JSON  | `PatientBloodRequisition` | - | - | - |
| POST | `/patientrequisition/getBloodComponantsListForRequisition` | `pas.PatientRequisitionServiceImpl.getBloodComponantsForRequisition` | Q  | `List<BloodCompListForReqResultDto>` | Y | - | - |
| POST | `/patientrequisition/getCrossUnitOrderList` | `pas.PatientRequisitionServiceImpl.getCrossUnitOrderList` | JSON Q  | `BloodBagExtractionView` | Y | - | - |
| POST | `/patientrequisition/getPatientRequisitionList` | `pas.PatientRequisitionServiceImpl.getPatientRequisitionList` | JSON Q  | `BloodBagExtractionView` | Y | - | - |
| POST | `/patientrequisition/placeCrossUnitOrder` | `pas.PatientRequisitionServiceImpl.placeCrossUnitOrder` | JSON  | `PatientBloodRequisition` | - | - | - |
| PUT | `/patientrequisition/updateBloodSampleCollectionStatusForRequisition` | `pas.PatientRequisitionServiceImpl.updateBloodSampleCollectionStatusForRequisition` | Q Q Q  | `boolean` | - | - | - |
| DELETE | `/patient/deallocateShiftDetails` | `pas.PatientServiceImpl.deallocateShiftDetails` | JSON  | `void` | - | - | - |
| GET | `/patient/{filename}` | `pas.PatientServiceImpl.servePdf` | P ?  | `void` | - | - | - |
| GET | `/patient/addIPDOPDPatientToQ` | `pas.PatientServiceImpl.addIPDPatientToQ` | Q Q Q  | `void` | Y | - | - |
| GET | `/patient/addPatientAbhNumberKey` | `pas.PatientServiceImpl.addPatientAbhNumberKey` | Q Q  | `void` | - | - | - |
| GET | `/patient/checkMotherhasChildAdmitted` | `pas.PatientServiceImpl.checkMotherhasChildAdmitted` | Q Q  | `List<Long>` | Y | - | - |
| GET | `/patient/checkMotherhasChildAdmittedOnCradle` | `pas.PatientServiceImpl.checkMotherhasChildAdmittedOnCredle` | Q Q  | `List<Long>` | - | - | - |
| GET | `/patient/checkMotherhasChildAdmittedOnSameBed` | `pas.PatientServiceImpl.checkMotherhasChildAdmittedOnSameBed` | Q Q  | `List<Long>` | - | - | - |
| GET | `/patient/checkshiftdetails` | `pas.PatientServiceImpl.checkshiftdetails` | Q Q Q  | `List<UserShiftDetailsDto>` | - | - | - |
| GET | `/patient/getActiveAdmissionOfPatientForInquiry` | `pas.PatientServiceImpl.getActiveAdmissionOfPatientForInquiry` | Q Q  | `List<InquiryDetailsResultDto>` | - | - | - |
| GET | `/patient/getActiveAdmissionOfRegisteredPatient` | `pas.PatientServiceImpl.getActiveAdmissionOfRegisteredPatient` | Q  | `List<ActiveAdmissionOfRegisteredPatientResultDto>` | Y | - | - |
| GET | `/patient/getAllHospitalEvents` | `pas.PatientServiceImpl.getAllHospitalEvents` | Q Q  | `List<Events>` | - | - | - |
| GET | `/patient/getAnmAncVisitDetails` | `pas.PatientServiceImpl.getAnmAncVisitDetails` | Q  | `JsyFormDetailsView` | Y | - | - |
| GET | `/patient/getBabiesMotherAdmissionStatus` | `pas.PatientServiceImpl.getBabiesMotherAdmissionStatus` | Q Q  | `List<ActiveAdmissionOfRegisteredPatientResultDto>` | - | - | - |
| GET | `/patient/getBiometricScanDetails` | `pas.PatientServiceImpl.getBiometricScanDetails` | - | `String` | - | - | - |
| GET | `/patient/getChiranjeeviDetailByJanAdhar` | `pas.PatientServiceImpl.getChiranjeeviDetailByJanAdhar` | Q  | `List<ChiranjeeviJanadharBasedDto>` | - | - | - |
| GET | `/patient/getChiranjeeviDocumentByTid` | `pas.PatientServiceImpl.getChiranjeeviDocumentByTid` | Q  | `Object` | - | - | - |
| GET | `/patient/getEnconterBatchCount` | `pas.PatientServiceImpl.getEnconterBatchCount` | Q  | `PatientEncounterDetailsDto` | - | - | - |
| GET | `/patient/getHospitalEventById` | `pas.PatientServiceImpl.getHospitalEventById` | Q  | `Events` | - | - | - |
| GET | `/patient/getLatestPatientVisit` | `pas.PatientServiceImpl.getLatestPatientVisit` | Q Q  | `PatientVisitHistoryView` | - | - | - |
| GET | `/patient/getMotherAndChildOnCradleBillStatus` | `pas.PatientServiceImpl.getMotherAndChildOnCredleBillStatus` | Q Q  | `List<Long>` | - | - | - |
| GET | `/patient/getPatientAddressDetails` | `pas.PatientServiceImpl.getPatientAddressDetails` | Q  | `List<GetPatientResultDto>` | - | - | - |
| GET | `/patient/getPatientAllNotes` | `pas.PatientServiceImpl.getPatientAllNotes` | Q Q  | `List<PatientNote>` | Y | - | - |
| GET | `/patient/getPatientByAddhar` | `pas.PatientServiceImpl.getPatientByAddhar` | Q Q  | `Long` | - | - | - |
| GET | `/patient/getPatientByDrivelicNo` | `pas.PatientServiceImpl.getPatientByDrivelicNo` | Q Q  | `Long` | - | - | - |
| GET | `/patient/getPatientCoronaList` | `pas.PatientServiceImpl.getPatientCoronaList` | Q Q Q  | `List<CoronaStats>` | Y | - | - |
| GET | `/patient/getPatientCrossServiceVisit` | `pas.PatientServiceImpl.getPatientCrossServiceVisit` | Q Q  | `Boolean` | - | - | - |
| GET | `/patient/getPatientDemoDetails` | `pas.PatientServiceImpl.getPatientDemoDetails` | Q Q  | `PatientDemographicDetailsForKDL` | Y | - | - |
| GET | `/patient/getPatientDemographicDetails` | `pas.PatientServiceImpl.getPatientDemographicDetails` | Q  | `PatientDemographicDetailsDto` | - | - | - |
| GET | `/patient/getPatientDetailListNew` | `pas.PatientServiceImpl.getPatientDetailsListNew` | Q Q Q  | `List<PatientDetailsListForSearchResultDto>` | - | - | - |
| GET | `/patient/getPatientDetailsByAdhaarId` | `pas.PatientServiceImpl.populatePatientDetails` | Q Q  | `List<SearchPatientGeneralDetailsResultDto>` | - | - | - |
| GET | `/patient/getPatientDetailsForDeathForm` | `pas.PatientServiceImpl.getPatientDetailsForDeathForm` | Q  | `Map<String, Object>` | - | - | - |
| GET | `/patient/getPatientEnconters` | `pas.PatientServiceImpl.getPatientEnconters` | Q Q  | `PatientEncounterDto` | - | - | - |
| GET | `/patient/getPatientEorders` | `pas.PatientServiceImpl.getPatientByAddhar` | Q Q  | `List<ServiceListForBillResultDto>` | Y | - | - |
| GET | `/patient/getPatientFeedbackDetails` | `pas.PatientServiceImpl.getPatientFeedbackDetails` | Q Q  | `PatientFeedbackView` | - | - | - |
| GET | `/patient/getPatientHistory` | `pas.PatientServiceImpl.getPatientHistory` | Q Q  | `PatientHistoryView` | - | - | - |
| GET | `/patient/getPatientMlcDetails` | `pas.PatientServiceImpl.getPatientMlcDetails` | Q  | `Object` | - | - | - |
| GET | `/patient/getPatientUnsettledBill` | `pas.PatientServiceImpl.getPatientUnsettedBill` | Q Q  | `PatientPendingBillsView` | - | - | - |
| GET | `/patient/getPatientVisitHistory` | `pas.PatientServiceImpl.getPatientVisitHistory` | Q  | `PatientVisitHistoryView` | - | - | - |
| GET | `/patient/getQuestionsForFeedback` | `pas.PatientServiceImpl.getQuestionsForFeedback` | Q  | `List<Feedbacktemplate>` | - | - | - |
| GET | `/patient/getRghsDocumentByTransactionId` | `pas.PatientServiceImpl.getRghsDocumentByTransactionId` | Q Q  | `Object` | - | - | - |
| GET | `/patient/getRghsTransactionsforIHMS` | `pas.PatientServiceImpl.getRghsTransactionsforIHMS` | Q  | `List<ChiranjeeviJanadharBasedDto>` | - | - | - |
| GET | `/patient/getShiftDetailsByUserId` | `pas.PatientServiceImpl.getShiftDetailsByUserId` | Q Q  | `List<UserShiftDetails>` | - | - | - |
| GET | `/patient/getSimsRghsforIHMS` | `pas.PatientServiceImpl.getSimsRghsforIHMS` | Q  | `List<List<RGHSSims>>` | - | - | - |
| GET | `/patient/getUserShiftDetailsByUserId` | `pas.PatientServiceImpl.getUserShiftDetailsByUserId` | Q Q  | `List<UserShiftDetails>` | - | - | - |
| GET | `/patient/getVerificationDetails` | `pas.PatientServiceImpl.getVerificationDetails` | Q  | `MortuaryVerifications` | - | - | - |
| GET | `/patient/kioskQueue` | `pas.PatientServiceImpl.kioskQueue` | Q Q Q  | `Map<String, Object>` | - | - | - |
| GET | `/patient/patientCoronaReport` | `pas.PatientServiceImpl.patientCoronaReport` | Q  | `CoronaStats` | Y | - | - |
| GET | `/patient/patientHQDetails` | `pas.PatientServiceImpl.patientHQDetails` | Q Q  | `List<PatientHqDetails>` | Y | - | - |
| GET | `/patient/searchPatientForABDM` | `pas.PatientServiceImpl.searchPatientForABDM` | Q  | `SearchPatientGeneralDetailsResultDto` | - | - | - |
| GET | `/patient/searchPatientFromSolr` | `pas.PatientServiceImpl.searchPatientFromSolr` | Q Q Q  | `List<SearchPatientGeneralDetailsResultDto>` | - | - | - |
| GET | `/patient/syncEHR` | `pas.PatientServiceImpl.syncEHR` | - | `void` | - | - | - |
| POST | `/patient/addandUpdatePatientCoronaDetails` | `pas.PatientServiceImpl.addandUpdatePatientCoronaDetails` | JSON  | `Patientcoronadetails` | - | - | - |
| POST | `/patient/addandUpdatePatientCoronaSTATDetails` | `pas.PatientServiceImpl.addandUpdatePatientCoronaSTATDetails` | JSON  | `CoronaStats` | - | - | - |
| POST | `/patient/addandUpdatePatientHQDetails` | `pas.PatientServiceImpl.addandUpdatePatientHQDetails` | JSON  | `PatientHqDetails` | - | - | - |
| POST | `/patient/addMotherChildLinking` | `pas.PatientServiceImpl.addMotherChildLinking` | JSON  | `MotherChildLinkingDto` | - | - | - |
| POST | `/patient/addPatientFeedback` | `pas.PatientServiceImpl.addPatientFeedback` | JSON  | `void` | - | - | - |
| POST | `/patient/addServiceOrderDetails` | `pas.PatientServiceImpl.addServiceOrderDetails` | JSON  | `BillInfoDtoForKDL` | - | - | - |
| POST | `/patient/addshiftdetails` | `pas.PatientServiceImpl.addShiftDetails` | JSON  | `UserShiftDetailsDto` | - | - | - |
| POST | `/patient/addshiftpatterns` | `pas.PatientServiceImpl.addShiftPatternsWithDetails` | JSON  | `ShiftPatternMaster` | Y | - | - |
| POST | `/patient/addUpdateMemberDetails` | `pas.PatientServiceImpl.addUpdateMemberDetails` | JSON  | `FamilyMemberDetailsDto` | - | - | - |
| POST | `/patient/createTeleconsultRoom` | `pas.PatientServiceImpl.createTeleconsultRoom` | Q  | `String` | - | - | - |
| POST | `/patient/geofenceViolated` | `pas.PatientServiceImpl.geofenceViolated` | Q Q  | `Integer` | Y | - | - |
| POST | `/patient/getAmbulanceCaseDetails` | `pas.PatientServiceImpl.getAmbulanceCaseDetails` | Q  | `AmbulanceCaseDetailsResultDto` | - | - | - |
| POST | `/patient/getDeptDoctorInformation` | `pas.PatientServiceImpl.getDeptDoctorInformation` | JSON Q  | `Map<Long, Map <Long, List<DoctorDepartmentInformationDto>>>` | - | - | - |
| POST | `/patient/getDoctorEnquiryDetails` | `pas.PatientServiceImpl.getDoctorEnquiryDetails` | JSON Q  | `List<DoctorEnquiryDetailsResultDto>` | - | - | - |
| POST | `/patient/getDoctorInformation` | `pas.PatientServiceImpl.getDoctorInformation` | JSON  | `Map<String, Map <String, List<DoctorDepartmentInformationDto>>>` | Y | - | - |
| POST | `/patient/getEMRAdmVisitByPatientId` | `pas.PatientServiceImpl.getEMRAdmVisitByPatientId` | JSON Q  | `List<EMRAdmVisitListByPatientIDResultDto>` | - | - | - |
| POST | `/patient/getInquiredDetails` | `pas.PatientServiceImpl.getInquiredDetails` | JSON Q  | `List<InquiryDetailsResultDto>` | - | - | - |
| POST | `/patient/getMembersDetails` | `pas.PatientServiceImpl.getMembersDetails` | JSON Q  | `List<FamilyMemberDetailsDto>` | Y | - | - |
| POST | `/patient/getMortuaryPatients` | `pas.PatientServiceImpl.getMortuaryPatients` | JSON Q Q  | `List<Object>` | - | - | - |
| POST | `/patient/getPatient` | `pas.PatientServiceImpl.getPatient` | JSON  | `List<GetPatientResultDto>` | - | - | - |
| POST | `/patient/getPatientDetailList` | `pas.PatientServiceImpl.getPatientDetailsList` | JSON  | `List<PatientDetailsListForSearchResultDto>` | - | - | - |
| POST | `/patient/getPatientDetailListSearchSpecific` | `pas.PatientServiceImpl.getPatientDetailListSearchSpecific` | JSON  | `List<PatientDetailsListForSearchResultDto>` | - | - | - |
| POST | `/patient/getPatientFeedbackList` | `pas.PatientServiceImpl.getPatientFeedbackList` | JSON Q  | `List<Object[]>` | Y | - | - |
| POST | `/patient/getPatientGeneralDetails` | `pas.PatientServiceImpl.getPatientGeneralDetails` | JSON  | `List<SearchPatientGeneralDetailsResultDto>` | - | - | - |
| POST | `/patient/getPatientGeneralDetailsListForSearch` | `pas.PatientServiceImpl.findPatientGeneralDetailsListForSearch` | JSON  | `List<SearchPatientGeneralDetailsResultDto>` | - | - | - |
| POST | `/patient/getPatientIpcrDetails` | `pas.PatientServiceImpl.getPatientIpcrDetails` | Q  | `Mortuary` | - | - | - |
| POST | `/patient/getPatientRecord` | `pas.PatientServiceImpl.getPatientRecord` | JSON Q  | `PatientRecordView` | - | - | - |
| POST | `/patient/getReleasedPatientDetails` | `pas.PatientServiceImpl.getReleasedPatientDetails` | Q  | `MortuaryPatientDetialsDto` | - | - | - |
| POST | `/patient/getRequestRaisedPatients` | `pas.PatientServiceImpl.getRequestRaisedPatients` | JSON Q  | `List<Mortuary>` | - | - | - |
| POST | `/patient/getResultsFromKDL` | `pas.PatientServiceImpl.getResultsFromKDL` | JSON  | `LabResultResponseDto` | Y | - | - |
| POST | `/patient/getServicesForInquiry` | `pas.PatientServiceImpl.getServicesForInquiry` | JSON  | `List<ServiceListForBillResultDto>` | - | - | - |
| POST | `/patient/getUserAllShiftDetails` | `pas.PatientServiceImpl.getUserAllShiftDetails` | JSON  | `List<UserShiftDetails>` | Y | - | - |
| POST | `/patient/mergeHId` | `pas.PatientServiceImpl.mergedHIDs` | HDR JSON  | `void` | - | - | - |
| POST | `/patient/registerPatient` | `pas.PatientServiceImpl.registerPatient` | JSON Q  | `Map<Long,List<Object>>` | - | - | - |
| POST | `/patient/saveDemographicsDetailsOfPatientWhoseDataMismatch` | `pas.PatientServiceImpl.saveDemographicsDetailsOfPatientWhoseDataMismatch` | JSON  | `void` | - | - | - |
| POST | `/patient/saveKioskLabResult` | `pas.PatientServiceImpl.saveKioskLabResult` | JSON  | `Map<String, Object>` | Y | - | - |
| POST | `/patient/saveVitalData` | `pas.PatientServiceImpl.saveVitalData` | JSON  | `Map<String, Object>` | Y | - | - |
| POST | `/patient/searchPatientForBloodBank` | `pas.PatientServiceImpl.searchPatientForBloodBank` | JSON Q  | `List<SearchPatientGeneralDetailsResultDto>` | - | - | - |
| POST | `/patient/searchPatientForHistoryDetails` | `pas.PatientServiceImpl.searchPatient` | JSON Q  | `List<PatientSearchListForHistory>` | - | - | - |
| POST | `/patient/updateJanaadharDetails` | `pas.PatientServiceImpl.getPatient` | JSON  | `SearchPatientGeneralDetailsResultDto` | - | - | - |
| POST | `/patient/updatePatientDeathDetails` | `pas.PatientServiceImpl.updatePatientDeathDetails` | Q Q  | `Boolean` | - | - | - |
| POST | `/patient/uploadKioskLabResult` | `pas.PatientServiceImpl.uploadKioskLabResult` | Q Q ?  | `Map<String, Object>` | Y | - | - |
| PUT | `/patient/updateBedOccupiedFlag` | `pas.PatientServiceImpl.updateBedOccupiedFlag` | JSON  | `UpdateBedOccupiedFlagDto` | - | - | - |
| GET | `/payment/getTermsOfPaymentListById/{id}` | `pas.PaymentServiceImpl.getTermsOfPaymentListById` | P  | `Termsofpayment` | Y | - | - |
| POST | `/payment/addRefund` | `pas.PaymentServiceImpl.addRefund` | JSON  | `RefundDto` | - | - | - |
| POST | `/payment/addTermsOfPaymentList` | `pas.PaymentServiceImpl.addTermsOfPaymentList` | JSON  | `void` | - | - | - |
| PUT | `/payment/deleteTermsOfPaymentListById/{id}` | `pas.PaymentServiceImpl.deleteTermsOfPaymentListById` | P  | `void` | Y | - | - |
| PUT | `/payment/updateTermsOfPaymentList/{id}` | `pas.PaymentServiceImpl.updateTermsOfPaymentList` | JSON P  | `Termsofpayment` | - | - | - |
| GET | `/pcts/getHighRiskPregnancy` | `pas.PctsServiceImpl.getHighRiskPregnancy` | Q Q  | `Boolean` | Y | - | - |
| POST | `/pcts/getEDDByLMP` | `pas.PctsServiceImpl.getEDDByLMPStatus` | JSON  | `List<String>` | Y | - | - |
| POST | `/pcts/saveOrUpdateAncDetailsForPat` | `pas.PctsServiceImpl.saveOrUpdateAncDetailsForPat` | JSON  | `Boolean` | Y | - | - |
| POST | `/pcts/updateUSGEDDByLMP` | `pas.PctsServiceImpl.updateUSGEDDByLMP` | JSON  | `Boolean` | Y | - | - |
| PUT | `/pcts/updateIsHighRiskForPatient` | `pas.PctsServiceImpl.updateIsHighRiskForPatient` | Q Q Q  | `void` | - | - | - |
| POST | `/planmaster/addAndUpdatePlanMasterDetails` | `pas.PlanMasterServiceImpl.addPlanMasterDetails` | JSON  | `UpdatePlanMasterDto` | Y | - | - |
| PUT | `/planmaster/deletePlanListById/{id}` | `pas.PlanMasterServiceImpl.deletePlanListById` | P Q  | `void` | - | - | - |
| PUT | `/planmaster/updatePlanById/{id}` | `pas.PlanMasterServiceImpl.updatePlanById` | P  | `void` | Y | - | - |
| GET | `/pmrd/getAllPatientEncounter` | `pas.PmrdServiceImpl.getAllPatientEncounter` | Q  | `JSONObject` | - | - | - |
| GET | `/pmrd/getJsyFormDetails` | `pas.PmrdServiceImpl.getJsyFormDetails` | Q Q  | `JsyFormDetailsView` | - | - | - |
| GET | `/pmrd/getPatientDocumentsForPMRD` | `pas.PmrdServiceImpl.getPatientDocumentsForPMRD` | Q  | `JSONObject` | - | - | - |
| GET | `/pmrd/getPatientIenById` | `pas.PmrdServiceImpl.getPatientIenById` | Q  | `Long` | - | - | - |
| GET | `/prescriptionmaster/downloadPrescriptionDetails` | `pas.PrescriptionMasterServiceImpl.downloadPrescriptionDetails` | Q Q Q  | `List<String>` | Y | - | - |
| GET | `/prescriptionmaster/downloadPrescriptionDetailsByVisitId` | `pas.PrescriptionMasterServiceImpl.downloadPrescriptionDetailsByVisitId` | Q Q Q  | `List<String>` | - | - | - |
| GET | `/prescriptionmaster/getPrescriptionDetails` | `pas.PrescriptionMasterServiceImpl.` |  | `?` | - | - | - |
| POST | `/prescriptionmaster/addUpdatePrescriptionMasterNew` | `pas.PrescriptionMasterServiceImpl.addUpdatePrescriptionMasterNew` | JSON  | `AddPrescriptionView` | - | - | - |
| POST | `/prescriptionmaster/getEntirePrescriptionMasterList` | `pas.PrescriptionMasterServiceImpl.getEntirePrescriptionMasterList` | JSON  | `List<PrescriptionMasterListResultDto>` | - | - | - |
| POST | `/preventivemaintenanceschedule/preventiveMaintenanceScheduleEquipment` | `pas.PreventiveMaintenanceScheduleServiceImpl.preventiveMaintenanceScheduleEquipment` | JSON  | `List<PreventiveMaintenanceScheduleEquipmentResultDto>` | - | - | - |
| GET | `/preventivemaintenance/deleteCheckListDetails` | `pas.PreventiveMaintenanceServiceImpl.deleteCheckListDetails` | Q Q Q  | `void` | Y | - | - |
| GET | `/preventivemaintenance/getAsssetCalibrationChecklistDetailsById` | `pas.PreventiveMaintenanceServiceImpl.getAsssetCalibrationChecklistDetailsById` | Q  | `List<GetAssetCalibrationChecklistDetailsById>` | - | - | - |
| GET | `/preventivemaintenance/getAsssetPreventiveChecklistDetailsById` | `pas.PreventiveMaintenanceServiceImpl.getAsssetPreventiveChecklistDetailsById` | Q  | `AssetMaintenanceView` | Y | - | - |
| GET | `/preventivemaintenance/getCalibrationCheckList` | `pas.PreventiveMaintenanceServiceImpl.getCalibrationCheckList` | Q  | `List<PreventiveCalibrationListResultDto>` | - | - | - |
| GET | `/preventivemaintenance/getChecklistCount` | `pas.PreventiveMaintenanceServiceImpl.getChecklistCount` | Q Q  | `long` | Y | - | - |
| GET | `/preventivemaintenance/getItemCheckListDetailsById` | `pas.PreventiveMaintenanceServiceImpl.getItemCheckListDetailsById` | Q  | `AddCheckListView` | Y | - | - |
| GET | `/preventivemaintenance/getItemForAddMaintenance` | `pas.PreventiveMaintenanceServiceImpl.getItemForAddMaintenance` | - | `List<ItemsForAddMaintenanceResultDto>` | - | - | - |
| GET | `/preventivemaintenance/getPreventiveCheckList` | `pas.PreventiveMaintenanceServiceImpl.getPreventiveCheckList` | Q Q  | `CheckListView` | Y | - | - |
| GET | `/preventivemaintenance/getPrevmaintenanceChkListDetail` | `pas.PreventiveMaintenanceServiceImpl.getPreventivemaintenanceDetail` | Q  | `List<PreventivemaintenanceDetailResultDto>` | - | - | - |
| GET | `/preventivemaintenance/updateCheckListStatus` | `pas.PreventiveMaintenanceServiceImpl.updateCheckListStatus` | Q Q Q  | `void` | Y | - | - |
| POST | `/preventivemaintenance/addCheckList` | `pas.PreventiveMaintenanceServiceImpl.addCheckList` | JSON  | `AddCheckListView` | - | - | - |
| POST | `/preventivemaintenance/addUpdatePreventiveMaintenance` | `pas.PreventiveMaintenanceServiceImpl.addUpdatePreventiveMaintenance` | JSON  | `UpdatePreventiveMaintenanceDto` | - | - | - |
| POST | `/preventivemaintenance/getAssetCalibrationMaintenanceList` | `pas.PreventiveMaintenanceServiceImpl.getAssetCalibrationMaintenanceList` | JSON  | `AssetMaintenanceView` | Y | - | - |
| POST | `/preventivemaintenance/getAssetPreventiveMaintenanceList` | `pas.PreventiveMaintenanceServiceImpl.getAssetPreventiveMaintenanceList` | JSON  | `AssetMaintenanceView` | Y | - | - |
| POST | `/preventivemaintenance/getCheckListDetailsById` | `pas.PreventiveMaintenanceServiceImpl.getCheckListDetailsById` | JSON  | `List<GetEquipmentChecklistDetailsById>` | Y | - | - |
| POST | `/preventivemaintenance/getPreventiveCheckListDetails` | `pas.PreventiveMaintenanceServiceImpl.getPreventiveCheckListDetails` | JSON  | `List<CheckListResultDto>` | - | - | - |
| POST | `/preventivemaintenance/getPreventivemaintenanceDetail` | `pas.PreventiveMaintenanceServiceImpl.getPreventivemaintenanceDetail` | JSON  | `List<PreventivemaintenanceDetailResultDto>` | - | - | - |
| POST | `/preventivemaintenance/mapCheckListWithAsset` | `pas.PreventiveMaintenanceServiceImpl.mapChecklListWithAsset` | JSON  | `List<PreventiveCalibrationCheckListMappingDto>` | Y | - | - |
| POST | `/preventivemaintenance/saveEquipmentMaintenance` | `pas.PreventiveMaintenanceServiceImpl.addEquipmentMaintenance` | JSON  | `AddEquipmentMaintenanceView` | Y | - | - |
| POST | `/preventivemaintenance/updateCheckListDetails` | `pas.PreventiveMaintenanceServiceImpl.updateCheckListDetails` | JSON  | `AddCheckListView` | Y | - | - |
| GET | `/purchaseorder/getLastPurchaseOrderList` | `pas.PurchaseOrderServiceImpl.` |  | `?` | - | - | - |
| GET | `/quotation/getQuotationsForCompare` | `pas.QuotationServiceImpl.` | Q Q  | `AddQuotationView` | - | - | - |
| POST | `/quotation/addNewQuotation` | `pas.QuotationServiceImpl.addNewQuotation` | JSON  | `AddQuotationView` | Y | - | - |
| POST | `/quotation/getQuotationDetails` | `pas.QuotationServiceImpl.` |  | `?` | - | - | - |
| POST | `/quotation/getQuotations` | `pas.QuotationServiceImpl.` |  | `?` | - | - | - |
| POST | `/quotation/getQuotationTermsAndConditions` | `pas.QuotationServiceImpl.` |  | `?` | - | - | - |
| POST | `/quotation/printQuotationDetails` | `pas.QuotationServiceImpl.` |  | `?` | - | - | - |
| POST | `/quotation/updateQuotationItemDetails` | `pas.QuotationServiceImpl.updateQuotationItemDetails` | JSON  | `AddQuotationView` | Y | - | - |
| GET | `/radiology/getAllRadServiceUnmapped` | `pas.RadiologyServiceImpl.getAllRadServiceUnmapped` | - | `List<Servicemaster>` | Y | - | - |
| GET | `/radiology/getPatientRadReport` | `pas.RadiologyServiceImpl.getPatientRadReport` | Q Q Q Q  | `GetPathoResultApi` | - | - | - |
| GET | `/radiology/getRadiologyQueue` | `pas.RadiologyServiceImpl.getRadiologyCabinQueue` | Q  | `List<Cabinmaster>` | - | - | - |
| GET | `/radiology/getRadiologyQueueNew` | `pas.RadiologyServiceImpl.getRadiologyCabinQueueNew` | Q Q  | `List<PatientRadiologyEncounterDto>` | - | - | - |
| GET | `/radiology/getRadTestDetailsById/{id}` | `pas.RadiologyServiceImpl.getRadTestDetailsById` | P  | `Radtestmaster` | Y | - | - |
| POST | `/radiology/addRadioTestMaster` | `pas.RadiologyServiceImpl.addRadTestMaster` | JSON  | `void` | - | - | - |
| POST | `/radiology/addRadTestMaster` | `pas.RadiologyServiceImpl.addRadTestMaster` | JSON  | `AddRadTestMasterDto` | Y | - | - |
| POST | `/radiology/updateRadioTestMaster` | `pas.RadiologyServiceImpl.updateRadTestMaster` | JSON  | `Radtestmaster` | - | - | - |
| PUT | `/radiology/deleteTestMaster/{id}` | `pas.RadiologyServiceImpl.deleteTestMasters` | P  | `void` | Y | - | - |
| PUT | `/radiology/updateRadTestMaster` | `pas.RadiologyServiceImpl.updateRadTestMaster` | JSON  | `GenericResultDto` | - | - | - |
| GET | `/refentity/getRefEntityByID` | `pas.RefEntityServiceImpl.getRefEntityByID` | Q  | `List<RefEntityByIDResultDto>` | - | - | - |
| GET | `/refentity/getRefEntityMasterByTypeId` | `pas.RefEntityServiceImpl.getRefEntityMasterByTypeId` | Q  | `List<GetRefEntityMasterByTypeId>` | Y | - | - |
| POST | `/refentity/addUpdateRefEntityMaster` | `pas.RefEntityServiceImpl.addUpdateRefEntityMaster` | JSON  | `UpdateRefEntityMasterDto` | - | - | - |
| DELETE | `/refund/deleteRefund` | `pas.RefundServiceImpl.deleteRefund` | JSON  | `DeleteRefundDto` | Y | - | - |
| POST | `/refund/addPaymentAdvanceDetails` | `pas.RefundServiceImpl.addPaymentAdvanceDetails` | JSON  | `GenericResultDto` | - | - | - |
| POST | `/refund/addRefund` | `pas.RefundServiceImpl.addRefund` | JSON  | `AddRefundDto` | Y | - | - |
| POST | `/refund/addRefundDetails` | `pas.RefundServiceImpl.addRefundDetails` | JSON  | `AddRefundDetailsDto` | - | - | - |
| POST | `/refund/details` | `pas.RefundServiceImpl.details` | JSON  | `RefundDetails_detailsDto` | Y | - | - |
| POST | `/refund/getRefund` | `pas.RefundServiceImpl.getRefund` | JSON  | `List<GetRefundResultDto>` | - | - | - |
| PUT | `/refund/updatePatientReqCrossMatchbyChargeID` | `pas.RefundServiceImpl.updatePatientReqCrossMatchbyChargeID` | JSON  | `GenericResultDto` | - | - | - |
| GET | `/RMRS/approveOrRelease` | `pas.RMRSServiceImpl.approveOrRelease` | Q  | `String` | - | - | - |
| GET | `/RMRS/getAccountByColAndVal` | `pas.RMRSServiceImpl.getCustomizedAccount` | Q  | `List<Accountheadmaster>` | - | - | - |
| GET | `/RMRS/getAccountById` | `pas.RMRSServiceImpl.getAccountById` | Q  | `Accountheadmaster` | - | - | - |
| GET | `/RMRS/getAccountBySummaryId` | `pas.RMRSServiceImpl.getAccountBySummaryId` | Q  | `List<Accountheadmaster>` | - | - | - |
| GET | `/RMRS/getAccountReport` | `pas.RMRSServiceImpl.getAccountReport` | Q  | `ArrayList<JournalCollectionView>` | - | - | - |
| GET | `/RMRS/getAllAccountTypes` | `pas.RMRSServiceImpl.getAllAccountTypes` | Q  | `Map<Integer, String>` | - | - | - |
| GET | `/RMRS/getAllTransactionStatusList` | `pas.RMRSServiceImpl.getAllTransactionStatus` | - | `List<TransactionStatus>` | Y | - | - |
| GET | `/RMRS/getAthorities` | `pas.RMRSServiceImpl.getAuthority` | Q  | `List<AuthorityMaster>` | Y | - | - |
| GET | `/RMRS/getAthoritiesByCount` | `pas.RMRSServiceImpl.getAthoritiesByCount` | Q  | `List<AuthorityMaster>` | - | - | - |
| GET | `/RMRS/getAthoritiesPendingPaymentDetails` | `pas.RMRSServiceImpl.getAthoritiesPaymentDetails` | Q  | `List<JournalCollectionView>` | - | - | - |
| GET | `/RMRS/getAuthorityEmiDetailsByAuthortiyId` | `pas.RMRSServiceImpl.getAuthorityEmiDetailsByAuthortiyId` | Q  | `List<RMRSDashboardTransaction>` | - | - | - |
| GET | `/RMRS/getAuthorityPaymentDetails` | `pas.RMRSServiceImpl.RMRSAuthorityPaymentDetails` | Q  | `List<RMRSCollectionDto>` | Y | - | - |
| GET | `/RMRS/getCustomizedAccount` | `pas.RMRSServiceImpl.getCustomizedAccount` | Q Q  | `List<Accountheadmaster>` | - | - | - |
| GET | `/RMRS/getHeads` | `pas.RMRSServiceImpl.getHeads` | Q  | `List<Accountheadmaster>` | - | - | - |
| GET | `/RMRS/getHeadsByCountAndSearch` | `pas.RMRSServiceImpl.getHeadsByCountAndSearch` | Q  | `List<HeadsMaster>` | - | - | - |
| GET | `/RMRS/getIssuedPayment` | `pas.RMRSServiceImpl.getIssuedPayment` | Q Q  | `ArrayList<RMRSCollectionDto>` | - | - | - |
| GET | `/RMRS/getJournalEntriesList` | `pas.RMRSServiceImpl.getJournalList` | Q Q  | `List<JournalCollectionView>` | - | - | - |
| GET | `/RMRS/getJournalListDetail` | `pas.RMRSServiceImpl.getJournalListDetail` | Q  | `JournalCollectionView` | - | - | - |
| GET | `/RMRS/getPaymentCollectionByAuthority` | `pas.RMRSServiceImpl.getAuthorityPendingAmt` | ? ? ? ?  | `JournalCollectionView` | Y | - | - |
| GET | `/RMRS/getPlanAndAccountWiseReport` | `pas.RMRSServiceImpl.getPlanAndAccountWiseReport` | Q  | `JournalCollectionView` | - | - | - |
| GET | `/RMRS/getPlanMasters` | `pas.RMRSServiceImpl.getPlanMaster` | Q  | `List<Planmaster>` | - | - | - |
| GET | `/RMRS/getReportByDocumentNumber` | `pas.RMRSServiceImpl.getReportByDocumentNumber` | Q  | `List<JournalCollectionView>` | - | - | - |
| GET | `/RMRS/getRMRSDashboardByDate` | `pas.RMRSServiceImpl.getRMRSDashboard` | Q  | `List<RMRSDashboardTransactionView>` | Y | - | - |
| GET | `/RMRS/getSearchDataByAccountHead` | `pas.RMRSServiceImpl.getCollectionDetailList` | Q  | `List<JournalCollectionView>` | Y | - | - |
| GET | `/RMRS/getSummaryAccountReport` | `pas.RMRSServiceImpl.getSummaryAccountReport` | Q  | `List<JournalCollectionView>` | Y | - | - |
| GET | `/RMRS/getSuspenseAccountDetail` | `pas.RMRSServiceImpl.getSuspenseAccountDetail` |  | `?` | - | - | - |
| GET | `/RMRS/printPlanAndAccountWiseReport` | `pas.RMRSServiceImpl.printPlanAndAccountWiseReport` | Q  | `String` | - | - | - |
| GET | `/RMRS/printReportByDocumentNumber` | `pas.RMRSServiceImpl.printReportByDocumentNumber` | Q  | `String` | - | - | - |
| GET | `/RMRS/printSummaryAccountReport` | `pas.RMRSServiceImpl.printSummaryAccountReport` | Q  | `String` | - | - | - |
| GET | `/RMRS/report_printBankStatement` | `pas.RMRSServiceImpl.printBankStatement` | Q  | `String` | - | - | - |
| GET | `/RMRS/validateDuplicateAccount` | `pas.RMRSServiceImpl.validateDuplicateAccount` | Q  | `Boolean` | - | - | - |
| GET | `/RMRS/validatePlanMappingWithAccount` | `pas.RMRSServiceImpl.validatePlanMappingWithAccount` | Q  | `Boolean` | - | - | - |
| POST | `/RMRS/addAthorities` | `pas.RMRSServiceImpl.saveAuthority` | JSON  | `void` | - | - | - |
| POST | `/RMRS/addAuthorityPaymentNew` | `pas.RMRSServiceImpl.addPaymentNew` | JSON  | `String` | Y | - | - |
| POST | `/RMRS/addCollection` | `pas.RMRSServiceImpl.addCollection` | JSON  | `String` | - | - | - |
| POST | `/RMRS/addDiscount` | `pas.RMRSServiceImpl.addDiscount` | Q  | `RMRSDashboardTransaction` | - | - | - |
| POST | `/RMRS/collectionUserSearch` | `pas.RMRSServiceImpl.collectionUserSearch` | JSON  | `List<RmrsCollection>` | - | - | - |
| POST | `/RMRS/getStaffPendingAmt` | `pas.RMRSServiceImpl.getStaffPendingAmt` | JSON  | `JournalCollectionView` | - | - | - |
| PUT | `/RMRS/updateAuthorityMaster` | `pas.RMRSServiceImpl.updateAuthorityMaster` | JSON  | `void` | Y | - | - |
| PUT | `/RMRS/updateHeadsMaster` | `pas.RMRSServiceImpl.updateHeadsMaster` | JSON  | `void` | Y | - | - |
| GET | `/servicemaster/checkExistingService` | `pas.ServiceMasterServiceImpl.checkExistingService` | Q Q Q  | `List<Long>` | Y | - | - |
| GET | `/servicemaster/getAllServiceMasterDetails` | `pas.ServiceMasterServiceImpl.` |  | `?` | - | - | - |
| GET | `/servicemaster/getClassDetailsByLatestAdmission` | `pas.ServiceMasterServiceImpl.` |  | `?` | - | - | - |
| GET | `/servicemaster/getClassDetailsByLatestAdmissionId` | `pas.ServiceMasterServiceImpl.getClassDetailsByLatestAdmissionId` | Q  | `List<ClassDetailsByLatestAdmissionResultDto>` | - | - | - |
| GET | `/servicemaster/getSpecialization` | `pas.ServiceMasterServiceImpl.getSpecialization` | - | `List<SpecializationResultDto>` | - | - | - |
| GET | `/servicemaster/getUnitWiseCabinMaster` | `pas.ServiceMasterServiceImpl.getUnitWiseCabinMaster` | Q Q  | `List<UnitWiseCabinMasterResultDto>` | - | - | - |
| POST | `/servicemaster/addCompanyDetails` | `pas.ServiceMasterServiceImpl.addCompanyDetails` | JSON  | `void` | - | - | - |
| POST | `/servicemaster/getPackageListForBill` | `pas.ServiceMasterServiceImpl.getPackageListForBill` | JSON  | `List<PackageListForBillResultDto>` | - | - | - |
| POST | `/servicemaster/getPackageServiceFromPackageID` | `pas.ServiceMasterServiceImpl.getPackageServiceFromPackageID` | JSON  | `List<GetPackageServiceFromPackageIDResultDto>` | - | - | - |
| POST | `/servicemaster/getRghsPackageList` | `pas.ServiceMasterServiceImpl.getRghsPackageList` | JSON  | `List<PackageListForBillResultDto>` | - | - | - |
| POST | `/servicemaster/getServiceListForBillEstimation` | `pas.ServiceMasterServiceImpl.getServiceListForBillEstimation` | JSON  | `List<ServiceListForBillResultDto>` | - | - | - |
| POST | `/servicemaster/getServiceListForBillWithoutCopayAndConcession` | `pas.ServiceMasterServiceImpl.getServiceListForBillWithoutCopayAndConcession` | JSON  | `List<ServiceListForBillResultDto>` | - | - | - |
| GET | `/surgerydetails/getSurgeryheadmaster` | `pas.SurgeryDetailsServiceImpl.getSurgeryheadmaster` | - | `List<Surgeryheadmaster>` | Y | - | - |
| GET | `/surgerydetails/getSurgeryheadmasterbyId` | `pas.SurgeryDetailsServiceImpl.getSurgeryheadmasterbyId` | Q  | `List<Surgeryheadmaster>` | - | - | - |
| GET | `/surgerydetails/getSurgeryheadsListById/{id}` | `pas.SurgeryDetailsServiceImpl.getSurgeryheadsListById` | P  | `Surgeryheadmaster` | Y | - | - |
| POST | `/surgerydetails/addSurgeryheadsList` | `pas.SurgeryDetailsServiceImpl.addSurgeryheadsList` | JSON  | `void` | - | - | - |
| POST | `/surgerydetails/updateSurgeryheadsList` | `pas.SurgeryDetailsServiceImpl.updateSurgeryheadsList` | JSON  | `Surgeryheadmaster` | Y | - | - |
| PUT | `/surgerydetails/deleteSurgeryheadsById/{id}` | `pas.SurgeryDetailsServiceImpl.deleteSurgeryheadsById` | P  | `void` | - | - | - |
| GET | `/tariff/getAllTariff` | `pas.TariffServiceImpl.getAllTariff` | - | `List<Tariffmaster>` | Y | - | - |
| GET | `/tariff/getServiceMasterListById/{id}` | `pas.TariffServiceImpl.getServiceMasterListById` | P  | `Servicemaster` | Y | - | - |
| POST | `/tariff/addservicemasterDetails` | `pas.TariffServiceImpl.addservicemasterDetails` | JSON  | `List<String>` | - | - | - |
| POST | `/tariff/updateServicemasterList` | `pas.TariffServiceImpl.updateServicemasterList` | JSON  | `void` | Y | - | - |
| PUT | `/tariff/deleteServiceListById/{id}` | `pas.TariffServiceImpl.deleteServiceListById` | P  | `void` | - | - | - |
| GET | `/transport/approveTransportRequest` | `pas.TransportServiceImpl.approveTransportRequest` | Q Q Q  | `void` | - | - | - |
| GET | `/transport/approveVehicleMaintenanceRequest` | `pas.TransportServiceImpl.approveVehicleMaintenanceRequest` | Q Q  | `void` | Y | - | - |
| GET | `/transport/cancelTransportRequest` | `pas.TransportServiceImpl.cancelTransportRequest` | Q Q  | `void` | - | - | - |
| GET | `/transport/checkTransitTransportReuest` | `pas.TransportServiceImpl.checkTransitTransportReuest` | Q Q Q  | `boolean` | Y | - | - |
| GET | `/transport/getAllDriverDetails` | `pas.TransportServiceImpl.getAllDriverDetails` | Q  | `List<DriverMaster>` | Y | - | - |
| GET | `/transport/getAllocatedVehicleStatus` | `pas.TransportServiceImpl.getAllocatedVehicleStatus` | Q  | `List<VehicleStatusDto>` | - | - | - |
| GET | `/transport/getAllVehicleDetails` | `pas.TransportServiceImpl.getAllVehicleDetails` | Q Q  | `List<Ambulancemaster>` | - | - | - |
| GET | `/transport/getAllVehicleDetailsForReport` | `pas.TransportServiceImpl.getAllVehicleDetailsForReport` | Q  | `List<Ambulancemaster>` | - | - | - |
| GET | `/transport/getAvailableVehicleDriverList` | `pas.TransportServiceImpl.getAvailableVehicleDriverList` | Q  | `JSONArray` | - | - | - |
| GET | `/transport/getDetailsForVehicleFare` | `pas.TransportServiceImpl.getDetailsForVehicleFare` | Q Q Q  | `TransportRequestDetails` | Y | - | - |
| GET | `/transport/getDriverDetails` | `pas.TransportServiceImpl.getDriverDetails` | Q  | `List<DriverMaster>` | - | - | - |
| GET | `/transport/getVehicleMaintenanceRequest` | `pas.TransportServiceImpl.getVehicleMaintenanceRequest` | Q Q  | `VehicleMaintenance` | Y | - | - |
| POST | `/transport/addFuelStationDetails` | `pas.TransportServiceImpl.addFuelStationDetails` | JSON  | `void` | - | - | - |
| POST | `/transport/addPatientConsultOrder` | `pas.TransportServiceImpl.addPatientConsultOrder` | JSON  | `Boolean` | - | - | - |
| POST | `/transport/addPatientTansportRequest` | `pas.TransportServiceImpl.addPatientTansportRequest` | JSON  | `void` | - | - | - |
| POST | `/transport/addTransportCharge` | `pas.TransportServiceImpl.addTransportCharge` | JSON  | `TransportChargeDto` | - | - | - |
| POST | `/transport/addVehicleMaintenanceRequest` | `pas.TransportServiceImpl.addVehicleMaintenanceRequest` | JSON  | `void` | - | - | - |
| POST | `/transport/allocateVehicle` | `pas.TransportServiceImpl.allocateVehicle` | JSON  | `void` | - | - | - |
| POST | `/transport/completeVehicleMaintenanceRequest` | `pas.TransportServiceImpl.completeVehicleMaintenanceRequest` | JSON  | `void` | - | - | - |
| POST | `/transport/getFuelStationDetails` | `pas.TransportServiceImpl.getFuelStationDetails` | JSON Q  | `List<FuelStationDetails>` | Y | - | - |
| POST | `/transport/getIPDPatientDetailsForTransport` | `pas.TransportServiceImpl.getIPDPatientDetailsForTransport` | JSON  | `List<SearchPatientAndVisitByNameMobNoResultDto>` | Y | - | - |
| POST | `/transport/getPatientDetailsListForTransport` | `pas.TransportServiceImpl.getPatientDetailsListForTransport` | JSON  | `List<SearchPatientGeneralDetailsResultDto>` | - | - | - |
| POST | `/transport/getTripRequestCount` | `pas.TransportServiceImpl.getTripRequestCount` | JSON  | `TripRequestListSummary` | Y | - | - |
| POST | `/transport/getTripRequestDetails` | `pas.TransportServiceImpl.getTripRequestDetails` | JSON Q  | `List<TransportRequest>` | - | - | - |
| POST | `/transport/getUnitWisePatientDetailsList` | `pas.TransportServiceImpl.getUnitWisePatientDetailsList` | JSON  | `List<SearchPatientGeneralDetailsResultDto>` | - | - | - |
| POST | `/transport/getVehicleMaintenanceDetails` | `pas.TransportServiceImpl.getVehicleMaintenanceDetails` | JSON Q  | `List<VehicleMaintenance>` | Y | - | - |
| POST | `/transport/getVehicleOutDetails` | `pas.TransportServiceImpl.getVehicleOutDetails` | JSON Q  | `List<TransportRequest>` | Y | - | - |
| POST | `/transport/getVisitAdmissionDetailsForTransport` | `pas.TransportServiceImpl.getVisitAdmissionDetailsForTransport` | JSON  | `List<EMRAdmVisitListByPatientIDResultDto>` | - | - | - |
| POST | `/transport/printTransportReqReceipt` | `pas.TransportServiceImpl.printTransportReqReceipt` | JSON  | `String[]` | Y | - | - |
| POST | `/transport/saveVehicleInDetails` | `pas.TransportServiceImpl.saveVehicleInDetails` | JSON  | `void` | Y | - | - |
| POST | `/transport/saveVehicleOutDetails` | `pas.TransportServiceImpl.saveVehicleOutDetails` | JSON  | `void` | - | - | - |
| GET | `/usermanagement/flushCache` | `pas.UserManagementServiceImpl.flush` | HDR  | `void` | - | - | - |
| GET | `/usermanagement/flushVCRates` | `pas.UserManagementServiceImpl.flushVCRates` | Q Q  | `void` | - | - | - |
| GET | `/usermanagement/getMDMTokenWithSSOToken` | `pas.UserManagementServiceImpl.getMDMTokenWithSSOToken` | Q  | `Map<String, String>` | Y | - | - |
| GET | `/usermanagement/getPermissions/{roleId}` | `pas.UserManagementServiceImpl.getUserByUserId` | P  | `List<RoleBasedPermissionDTO>` | - | - | - |
| GET | `/usermanagement/getSessionDetails` | `pas.UserManagementServiceImpl.getSessionDetails` | HDR Q  | `UserView` | - | - | - |
| GET | `/usermanagement/getUnitRoleDetails` | `pas.UserManagementServiceImpl.getUnitRoleDetails` | HDR  | `UserView` | Y | - | - |
| GET | `/usermanagement/getUnitRoleDetailsForChange` | `pas.UserManagementServiceImpl.getUnitRoleDetails` | - | `UserView` | - | - | - |
| GET | `/usermanagement/getUnitStoreStatusList` | `pas.UserManagementServiceImpl.getUnitStoreStatusList` | Q  | `List<UnitStoreStatusListResultDto>` | Y | - | - |
| GET | `/usermanagement/getUserBuildingDetails` | `pas.UserManagementServiceImpl.getUserBuildingDetails` | Q  | `List<Wingmaster>` | - | - | - |
| GET | `/usermanagement/getUserByUserId` | `pas.UserManagementServiceImpl.getUserByUserId` | Q Q  | `String` | - | - | - |
| GET | `/usermanagement/getUserMappedWards` | `pas.UserManagementServiceImpl.getUserMappedWards` | Q Q Q  | `List<Wardmaster>` | Y | - | - |
| GET | `/usermanagement/getUserMappedWardsForMlc` | `pas.UserManagementServiceImpl.getUserMappedWardsForMLc` | Q Q Q  | `List<Wardmaster>` | - | - | - |
| GET | `/usermanagement/getUserName` | `pas.UserManagementServiceImpl.getUserName` | Q  | `UserNameDto` | - | - | - |
| POST | `/usermanagement/authenticateUser` | `pas.UserManagementServiceImpl.authenticateUser` | JSON Q  | `List<EnticateUserResultDto_2>` | - | - | - |
| POST | `/usermanagement/getUser` | `pas.UserManagementServiceImpl.getUser` | JSON  | `List<GetUserResultDto>` | - | - | - |
| POST | `/usermanagement/loginsso` | `pas.UserManagementServiceImpl.loginsso` | FORM FORM  | `UserView` | Y | - | - |
| POST | `/usermanagement/patientLoginNameExists` | `pas.UserManagementServiceImpl.patientLoginNameExists` | JSON  | `List<PatientExistsResultDto>` | Y | - | - |
| POST | `/usermanagement/setDefaultRoute` | `pas.UserManagementServiceImpl.setDefaultRoute` | JSON  | `void` | - | - | - |
| GET | `/visit/getActiveConsultOrderDepartmentWardWise` | `pas.VisitServiceImpl.getActiveConsultOrderDepartmentWardWise` | Q Q  | `List<ConsultOrder>` | - | - | - |
| GET | `/visit/getAllFollowUpVisitReq` | `pas.VisitServiceImpl.getAllFollowUpVisitReq` | - | `List<GetFollowUpVisitPatientDto>` | - | - | - |
| GET | `/visit/getAllPendingConsultVisit` | `pas.VisitServiceImpl.getAllPendingConsultVisit` | Q  | `List<AllPendingConsultVisitResultDto>` | - | - | - |
| GET | `/visit/getPatientActiveConsultOrder` | `pas.VisitServiceImpl.getPatientActiveConsultOrder` | Q  | `List<ConsultOrder>` | - | - | - |
| GET | `/visit/getPatientActiveConsultOrderByDoctor` | `pas.VisitServiceImpl.getPatientActiveConsultOrderByDoctor` | Q Q  | `Long` | - | - | - |
| GET | `/visit/notifyConsultOrder` | `pas.VisitServiceImpl.notifyConsultOrder` | Q  | `ConsultOrder` | - | - | - |
| POST | `/visit/getActiveDoctorForConsult` | `pas.VisitServiceImpl.getActiveDoctorForConsult` | JSON  | `List<DoctorCheckinIpMaster>` | - | - | - |
| POST | `/visit/getAllConsultOrders` | `pas.VisitServiceImpl.getAllConsultOrders` | JSON Q Q  | `List<ConsultOrderResultDto>` | - | - | - |
| POST | `/visit/getConsultantForConsultOrder` | `pas.VisitServiceImpl.getConsultantForConsultOrder` | JSON  | `List<Doctormaster>` | - | - | - |
| POST | `/visit/getDoctorForConsultOrder` | `pas.VisitServiceImpl.getDoctorForConsultOrder` | JSON  | `List<Doctormaster>` | Y | - | - |
| POST | `/visit/updateConsultOrder` | `pas.VisitServiceImpl.updateConsultOrder` | JSON  | `void` | - | - | - |
| POST | `/visit/updateDoctorCheckInIp` | `pas.VisitServiceImpl.updateDoctorCheckInIp` | JSON  | `DoctorCheckinIpMaster` | Y | - | - |

### Module: Patient Portal  (23 endpoints)

| Method | URL (under /api/rest) | Class : method | Request | Response | Tx | Tables | Notes |
|---|---|---|---|---|---|
| GET | `/patientHistory/getPatientHistory` | `patp.PatientHistoryServiceImpl.getPatientApptandVisit` | Q Q Q  | `List<HealthFactorDto>` | - | - | - |
| DELETE | `/patientPortalService/removePatientPortalDocument` | `patp.PatientPortalServiceImpl.removePatientPortalDocument` | Q Q Q  | `void` | Y | - | - |
| GET | `/patientPortalService/document/{id}` | `patp.PatientPortalServiceImpl.getImageById` | P ?  | `void` | - | - | - |
| GET | `/patientPortalService/getAllergiesGenericListForSearch` | `patp.PatientPortalServiceImpl.getAllergiesGenericListForSearch` | Q Q  | `List<AllergyDto>` | - | - | - |
| GET | `/patientPortalService/getAllPatientDocumentsList` | `patp.PatientPortalServiceImpl.getAllPatientDocumentsList` | Q Q  | `List<PatientDocuments>` | - | - | - |
| GET | `/patientPortalService/getMedicationGenericListForSearch` | `patp.PatientPortalServiceImpl.getAllMedicationGenericListForSearch` | Q Q  | `List<SearchDto>` | Y | - | - |
| GET | `/patientPortalService/getNormalResultPP` | `patp.PatientPortalServiceImpl.getNormalResultPP` | Q  | `List<List<PathNormalTestResultDto>>` | - | - | - |
| GET | `/patientPortalService/getpatientapptandvisit` | `patp.PatientPortalServiceImpl.getPatientApptandVisit` | Q Q Q  | `PatDoctorScheduleView` | Y | - | - |
| GET | `/patientPortalService/getPatientDocumentsList` | `patp.PatientPortalServiceImpl.getPatientDocumentsList` | Q Q Q  | `List<PatientDocuments>` | Y | - | - |
| POST | `/patientPortalService/addPatientPortalDocument` | `patp.PatientPortalServiceImpl.addPatientPortalDocument` | JSON Q Q  | `List<PatientIDDocumentDto>` | - | - | - |
| POST | `/patientPortalService/addPatientRegistration` | `patp.PatientPortalServiceImpl.updatePatient` | JSON  | `void` | - | - | - |
| POST | `/patientPortalService/addUserAnswers` | `patp.PatientPortalServiceImpl.addUserAnswers` | JSON Q Q  | `void` | - | - | - |
| POST | `/patientPortalService/addwalkinVisit` | `patp.PatientPortalServiceImpl.bookAppintment` | JSON Q  | `PatientAppointmentandRegDto` | - | - | - |
| POST | `/patientPortalService/addwalkinVisit_Old` | `patp.PatientPortalServiceImpl.addwalkinVisit` | JSON Q Q  | `PatientAppointmentandRegDto` | Y | - | - |
| POST | `/patientPortalService/feedbackQuery` | `patp.PatientPortalServiceImpl.feedbackQuery` | Q Q Q  | `Integer` | - | - | - |
| POST | `/patientPortalService/getAllBills` | `patp.PatientPortalServiceImpl.getAllBills` | JSON  | `List<BillListForSearch_NewResultDto_3>` | Y | - | - |
| POST | `/patientPortalService/getAppointmentScheduleNew` | `patp.PatientPortalServiceImpl.getAppointmentScheduleServiceNew` | Q  | `Map<java.util.Date, Map<String, List<AppointmentSlot>>>` | Y | - | - |
| POST | `/patientPortalService/getLabsGeneralDetailsListForSearch` | `patp.PatientPortalServiceImpl.getLabsGeneralDetailsListForSearch` | JSON Q  | `List<ServiceListForBillResultDto>` | - | - | - |
| POST | `/patientPortalService/getPatient` | `patp.PatientPortalServiceImpl.getPatient` | JSON  | `List<GetPatientResultDto>` | Y | - | - |
| POST | `/patientPortalService/pathLabListByVisitPP` | `patp.PatientPortalServiceImpl.pathLabListByVisitPP` | JSON ?  | `List<PathOrderBookingDetailList_NEWResultDto_1>` | - | - | - |
| POST | `/patientPortalService/pathLabListPP` | `patp.PatientPortalServiceImpl.pathLabListPP` | JSON  | `List<PathOrderBookingDetailList_NEWResultDto_1>` | - | - | - |
| POST | `/patientPortalService/radLabListPP` | `patp.PatientPortalServiceImpl.radLabListPP` | JSON  | `PathoRadOrderBookingView` | - | - | - |
| POST | `/patientPortalService/radListByVisitPP` | `patp.PatientPortalServiceImpl.radListByVisitPP` | JSON ?  | `PathoRadOrderBookingView` | - | - | - |

### Module: Payments  (6 endpoints)

| Method | URL (under /api/rest) | Class : method | Request | Response | Tx | Tables | Notes |
|---|---|---|---|---|---|
| GET | `/paymentTransfer/generateOTP` | `payment.PaymentTransferServiceImpl.generateOTP` | - | - | - | `String` | - |
| GET | `/paymentTransfer/getDisburseAccounts` | `payment.PaymentTransferServiceImpl.getDisburseAccounts` | - | - | - | `List<BankAccountMaster>` | - |
| GET | `/paymentTransfer/getPaymentStatus` | `payment.PaymentTransferServiceImpl.getPaymentStatusById` | Q  | `JSONObject` | - | - | - |
| POST | `/paymentTransfer/` | `payment.PaymentTransferServiceImpl.getPaymentTransferList` | JSON Q Q  | `PaymentTransferListView` | - | - | - |
| POST | `/paymentTransfer/createPaymentTransfer` | `payment.PaymentTransferServiceImpl.createPaymentTransfer` | JSON Q  | `void` | Y | - | - |
| POST | `/paymentTransfer/verifyOTP` | `payment.PaymentTransferServiceImpl.verifyOTP` | JSON  | `String` | - | - | - |

### Module: Queue Management  (23 endpoints)

| Method | URL (under /api/rest) | Class : method | Request | Response | Tx | Tables | Notes |
|---|---|---|---|---|---|
| GET | `/queueManagment/activeDepartments` | `QueueManagmentServiceImpl.activeDepartments` | - | - | - | `List<Departmentmaster>` | - |
| GET | `/queueManagment/activeRoomQueue` | `QueueManagmentServiceImpl.activeRoomQueue` | Q  | `List<String>` | - | - | - |
| GET | `/queueManagment/activeRoomQueueForDoctor` | `QueueManagmentServiceImpl.activeRoomQueueForDoctor` | Q Q Q Q  | `List<DoctorRoomDto>` | Y | - | - |
| GET | `/queueManagment/activeRooms` | `QueueManagmentServiceImpl.activeRooms` | Q  | `List<Roommaster>` | - | - | - |
| GET | `/queueManagment/addToQueue` | `QueueManagmentServiceImpl.createOPDQueueByGET` | Q Q Q  | `Map<String, String>` | Y | - | - |
| GET | `/queueManagment/assignTokenPatientToQ` | `QueueManagmentServiceImpl.assignTokenPatientToQ` | Q Q  | `void` | Y | - | - |
| GET | `/queueManagment/checkoutPatientFromQueue` | `QueueManagmentServiceImpl.checkoutSinglePatientFromQueue` | Q Q Q  | `void` | Y | - | - |
| GET | `/queueManagment/counterQueueMI` | `QueueManagmentServiceImpl.getOPDQueue` | Q Q  | `RecordDetailsDTO<CounterQueueDTO>` | - | - | - |
| GET | `/queueManagment/departmentQueueMI` | `QueueManagmentServiceImpl.getDepartmentQueue` | Q Q Q  | `RecordDetailsDTO<DeptQueueDTO>` | - | - | - |
| GET | `/queueManagment/fetchAbsentOPDToken` | `QueueManagmentServiceImpl.findTokenFromQueue` | Q  | `RegistrationQueue` | Y | - | - |
| GET | `/queueManagment/findPatient` | `QueueManagmentServiceImpl.findPatient` | Q Q  | `PatientDetailsForDoctorQueue` | - | - | - |
| GET | `/queueManagment/findPatientByVisitId` | `QueueManagmentServiceImpl.findPatientByVisitId` | Q  | `PatientDetailsForDoctorQueue` | - | - | - |
| GET | `/queueManagment/findPatientY` | `QueueManagmentServiceImpl.findPatientY` | Q Q  | `PatientDetailsForDoctorQueue` | - | - | - |
| GET | `/queueManagment/opdGetNextToken` | `QueueManagmentServiceImpl.getNextToken` | Q Q Q  | `String` | - | - | - |
| GET | `/queueManagment/printToken` | `QueueManagmentServiceImpl.queueTemplate` | Q  | `Map<String, String>` | Y | - | - |
| GET | `/queueManagment/seenPatientCount` | `QueueManagmentServiceImpl.getSeenPatientCount` | Q Q  | `List<Object[]>` | Y | - | - |
| GET | `/queueManagment/updateFillQueue` | `QueueManagmentServiceImpl.stopFillQueue` | Q Q Q  | `void` | - | - | - |
| GET | `/queueManagment/validatePatientTokenExistsInQ` | `QueueManagmentServiceImpl.validatePatientTokenExistsInQ` | Q Q Q  | `Map<String, Object>` | - | - | - |
| POST | `/queueManagment/addToQueue` | `QueueManagmentServiceImpl.addToQueue` | ?  | `RegistrationQueue` | Y | - | - |
| POST | `/queueManagment/assignPatientToDoctorQueueByNurse` | `QueueManagmentServiceImpl.assignPatientToDoctorQueueByNurse` | Q  | `String` | Y | - | - |
| POST | `/queueManagment/markOPDTokenAsAbsent` | `QueueManagmentServiceImpl.markOPDTokenAsAbsent` | Q  | `void` | - | - | - |
| PUT | `/queueManagment/allocatePatientToDepartmentalQ` | `QueueManagmentServiceImpl.allocatePatientToDepartmentalQ` | Q Q  | `void` | Y | - | - |
| PUT | `/queueManagment/clearRoomToken` | `QueueManagmentServiceImpl.clearRoomToken` | Q Q Q  | `void` | - | - | - |

### Module: RCM (Revenue Cycle)  (7 endpoints)

| Method | URL (under /api/rest) | Class : method | Request | Response | Tx | Tables | Notes |
|---|---|---|---|---|---|
| GET | `/rcm/checkEligibleForChiranjeevi` | `RcmServiceImpl.checkEligibleForChiranjeevi` | Q Q ?  | `Boolean` | - | - | - |
| GET | `/rcm/getAdmPatientVerification` | `RcmServiceImpl.getAdmPatientVerification` | Q  | `GetPatientResultDto_1` | - | - | - |
| GET | `/rcm/getEligibilityCheckById/{eligibilityCheckById}` | `RcmServiceImpl.getEligibilityCheckById` | P  | `EligibilityCheck` | - | - | - |
| GET | `/rcm/getRequestResponse` | `RcmServiceImpl.getRequest` | Q Q ?  | `void` | - | - | - |
| GET | `/rcm/getRghsPackageDetails` | `RcmServiceImpl.getRghsPackageDetails` | Q Q  | `EligibilityCheck` | - | - | - |
| GET | `/rcm/getTidByPatientId` | `RcmServiceImpl.getTidByPatientId` | Q Q  | `String` | - | - | - |
| POST | `/rcm/getTidListStatus` | `RcmServiceImpl.getTidListStatus` | JSON Q Q  | `List<EligibilityCheck>` | - | - | - |




## High-value endpoint notes (tables / tx / behavior evidence)

| Endpoint | Evidence (file:line) | Key tables / behavior |
|---|---|---|
| POST /api/rest/auth/loginf | AuthenticationServiceImpl.java:70-87 | DefaultAuthenticationImpl.attempAuthentication; session UserView cached; @Login + AuditEvent(LOGIN) |
| POST /api/rest/patientRegAddmission/adddata | PatientRegAdmissionServiceImpl.java:237-241 | admitPatient -> admitPatientInt: T_Admission, T_AdmissionDetails, bed (m_bedmaster ward update line 1305), T_Advance (line 1880); multi-entity tx |
| POST /api/rest/addPayment/ | AddPaymentServiceImpl.java:30-33 | addBillPaymentNew @Transactional: bill ledger T_Bill/T_LedBill/T_TledBill + advances + receipts; AddPaymentServiceHelper |
| POST /api/rest/serviceOrder/addServicechargesNew | AddServiceOrderServiceImpl.java (addServices) | ServiceOrderDto -> charges (T_Charges), bill, package/health-plan consumption; 22 tx methods |
| POST /api/rest/patientTransferDischarge/<...> | PatientTransferDischargeServiceImpl.java:147 | discharge: T_Admission status, bill settle, bed release, doctor checkout |
| GET /api/rest/master/<...> (264 endpoints) | MasterCachedServiceImpl.java:214 | master data served from @Cached cache (MemoryCacheImpl); TTL resets on change |
| POST /api/rest/inventoryStockList/<...> (235) | InventoryServiceImpl.java:336 | stock in/out, indents, expiry; 126 tx methods |
| POST /api/rest/nursingList/<...> (300) | NursingServiceImpl.java:360 | nurse allocation, meds administration, notes, PMR; 135 tx methods |
| POST /api/rest/pathoOrder/<...> (133) | PathoOrderServiceImpl.java:258 | lab orders, results, report release; 50 tx methods |
| GET/POST /api/rest/abdm/** (callbacks) | ABDMController.java:243-1344 | writes HttpServletResponse directly; v2 + v3 + catch-all; consent/links/health-info flows |
| POST /api/rest/nhcx/coverageEligiblityRequest | NHCXServiceImpl.java (CheckCoverageEligiblityRequestDto) | HCX JWE RSA-OAEP-256 encrypt/decrypt; NHCXRequest persisted |
| GET /api/rest/abha/<...> (50) | AbhaServiceImpl.java | ABHA create/link/consent/lookup flows; 9 tx methods; token mgmt |
| POST /api/rest/integration/checkRGHSEligiblity | IntegrationServiceImpl.java | RGHS SIMS eligibility; EligibilityCheck entity; also aadhaarAuthenticate (AadhaarAuthRequest) |
| GET /api/rest/ehr/handleSyncReq | EHRUtilServiceImpl.java | cross-platform EHR sync; EhrSyncView; ehr.sync via jobs + api |
| POST /api/rest/queueManagment/addToQueue | QueueManagmentServiceImpl.java (GET+POST overload) | RegistrationQueue; token/queue state machine |

Full per-endpoint request/response types and signatures: see findings.md section 3 (raw dump).
### Module: ABDM callbacks (ABDMController @Path /abdm + HFRController @Path /abdm-hfr, class com.ohum.xcg.abdm.server)  (57 endpoints)

| Method | URL (under /api/rest) | Class : method | Request | Response | Tx | Tables | Notes |
|---|---|---|---|---|---|---|---|
| GET | `/abdm/{versionNumber}/heartbeat` | `ABDMController.onHeartbeat` | P ?  | `String` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/{versionNumber}/{endpoint}/on_{action}` | `ABDMController.nhcxCallbacks` | JSON  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/{versionNumber}/care-contexts/discover` | `ABDMController.onCareContextsDiscover` | JSON P  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/{versionNumber}/consent-requests/on-init` | `ABDMController.onConsentRequestInit` | JSON P  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/{versionNumber}/consents/hip/notify` | `ABDMController.onConsentHIPNotify` | JSON P  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/{versionNumber}/consents/hiu/notify` | `ABDMController.onConsentHIUNotify` | JSON P  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/{versionNumber}/consents/on-fetch` | `ABDMController.onConsentFetch` | JSON P ? ?  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/{versionNumber}/health-information/hip/request` | `ABDMController.onHealthInfoHIPRequest` | JSON P  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/{versionNumber}/health-information/hiu/on-request` | `ABDMController.onHealthInfoHIURequest` | JSON P  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/{versionNumber}/links/context/notify` | `ABDMController.onLinkContextNotify` | JSON P  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/{versionNumber}/links/context/on-notify` | `ABDMController.onLinkContextOnNotify` | JSON P  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/{versionNumber}/links/link/confirm` | `ABDMController.onLinkConfirm` | JSON P ? ?  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/{versionNumber}/links/link/init` | `ABDMController.onLinkInit` | JSON P ? ?  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/{versionNumber}/links/link/on-add-contexts` | `ABDMController.onLinkAddContext` | JSON P ? ?  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/{versionNumber}/links/link/on-add-contexts` | `ABDMController.onLinkAddContextv3` | JSON P  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/{versionNumber}/patients/on-find` | `ABDMController.onPatientsFind` | JSON P ? ?  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/{versionNumber}/patients/profile/on-share` | `ABDMController.onPatientsProfileOnShare` | JSON P  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/{versionNumber}/patients/profile/share` | `ABDMController.onPatientsProfileShare` | JSON P  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/{versionNumber}/patients/sms/on-notify` | `ABDMController.onPatientsSMSNotify` | JSON P  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/{versionNumber}/patients/status/notify` | `ABDMController.onPatientStatusNotify` | JSON P  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/{versionNumber}/subscription-requests/hiu/notify` | `ABDMController.onSubscriptionRequestHIUNotify` | JSON P  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/{versionNumber}/subscriptions/hiu/notify` | `ABDMController.onSubscriptionsHIUNotify` | JSON P  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/{versionNumber}/users/auth/notify` | `ABDMController.onUserAuthNotify` | JSON P ? ?  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/{versionNumber}/users/auth/on-confirm` | `ABDMController.onUserAuthConfirm` | JSON P ? ?  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/{versionNumber}/users/auth/on-fetch-modes` | `ABDMController.onUserAuthFetchModes` | JSON P  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/{versionNumber}/users/auth/on-init` | `ABDMController.onUserAuthInit` | JSON P ? ?  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/api/v3/consent/request/hip/notify` | `ABDMController.onConsentHIPNotifyv3` | JSON ? ?  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/api/v3/hip/health-information/request` | `ABDMController.onHealthInfoHIPRequestv3` | JSON ? ?  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/api/v3/hip/link/care-context/confirm` | `ABDMController.onLinkConfirmv3` | JSON ? ?  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/api/v3/hip/link/care-context/init` | `ABDMController.onLinkInitv3` | JSON ? ?  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/api/v3/hip/patient/care-context/discover` | `ABDMController.onCareContextsDiscoverv3` | JSON ? ?  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/api/v3/hip/patient/share` | `ABDMController.onPatientsProfileSharev3` | JSON ? ?  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/api/v3/hip/token/on-generate-token` | `ABDMController.onGenerateToken` | JSON ? ?  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/api/v3/hiu/consent/on-fetch` | `ABDMController.onConsentFetchv3` | JSON ? ?  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/api/v3/hiu/consent/request/notify` | `ABDMController.onConsentHIUNotifyv3` | JSON ? ?  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/api/v3/hiu/consent/request/on-init` | `ABDMController.onConsentRequestInitv3` | JSON ? ?  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/api/v3/hiu/health-information/on-request` | `ABDMController.onHealthInfoHIURequestv3` | JSON ? ?  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/api/v3/link/on_carecontext` | `ABDMController.onCareContextv3` | JSON ? ?  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/api/v3/links/context/on-notify` | `ABDMController.onLinkContextOnNotifyV3` | JSON ? ?  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/api/v3/patients/sms/on-notify` | `ABDMController.onPatientsSMSNotifyv3` | JSON ? ?  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/health-information/transfer/{uniqId}` | `ABDMController.onHealthInfoDataRx` | JSON P ? ?  | `void` | - | - | writes HttpServletResponse directly (callback) |
| POST | `/abdm/health-information/transfer/uniqId/{uniqId}` | `ABDMController.onHealthInfoDataRxOld` | JSON P ? ?  | `void` | - | - | writes HttpServletResponse directly (callback) |
| GET | `/abdm-hfr/{versionNumber}/facility/get-master-data` | `HFRController.getMasterData` | P Q  | `String` | - | - | facility registry callbacks |
| GET | `/abdm-hfr/{versionNumber}/facility/lgd/districts` | `HFRController.getLGDDistricts` | P Q  | `String` | - | - | facility registry callbacks |
| GET | `/abdm-hfr/{versionNumber}/facility/lgd/states` | `HFRController.getLGDStates` | P  | `String` | - | - | facility registry callbacks |
| GET | `/abdm-hfr/{versionNumber}/facility/lgd/subdistricts` | `HFRController.getLGDSubDistricts` | P Q  | `String` | - | - | facility registry callbacks |
| GET | `/abdm-hfr/{versionNumber}/facility/software-details` | `HFRController.getSoftwareDetails` | P  | `String` | - | - | facility registry callbacks |
| POST | `/abdm-hfr/{versionNumber}/facility/additional-information` | `HFRController.createFaciltityAdditionalStep2` | P JSON  | `String` | - | - | facility registry callbacks |
| POST | `/abdm-hfr/{versionNumber}/facility/basic-information` | `HFRController.createFaciltityBasicStep1` | P JSON  | `String` | - | - | facility registry callbacks |
| POST | `/abdm-hfr/{versionNumber}/facility/detailed-information` | `HFRController.createFaciltityDetailedStep3` | P JSON  | `String` | - | - | facility registry callbacks |
| POST | `/abdm-hfr/{versionNumber}/facility/fetch-facility-Sub-type` | `HFRController.getFacilitySubType` | P JSON  | `String` | - | - | facility registry callbacks |
| POST | `/abdm-hfr/{versionNumber}/facility/fetch-facility-type` | `HFRController.getFacilityType` | P JSON  | `String` | - | - | facility registry callbacks |
| POST | `/abdm-hfr/{versionNumber}/facility/get-owner-subtype` | `HFRController.getOwnerSubTypes` | P JSON  | `String` | - | - | facility registry callbacks |
| POST | `/abdm-hfr/{versionNumber}/facility/get-specialities` | `HFRController.getSpecialities` | P JSON  | `String` | - | - | facility registry callbacks |
| POST | `/abdm-hfr/{versionNumber}/facility/search` | `HFRController.searchFaciltity` | P JSON  | `String` | - | - | facility registry callbacks |
| POST | `/abdm-hfr/{versionNumber}/facility/submit-facility` | `HFRController.createFaciltityFinal` | P JSON  | `String` | - | - | facility registry callbacks |
| POST | `/abdm-hfr/saveFacilityNumber` | `HFRController.saveFacilityNumber` | Q Q  | `boolean` | - | - | facility registry callbacks |


