# M01 — Backend APIs (CXF class-level @Path)

Base: `http://<host>:8080/IHMS/api/rest`

| Service | @Path | Representative methods (evidence) |
|---|---|---|
| PatientServiceImpl | `/patient` | registerPatient, getPatientByAddhar, getPatientByDrivelicNo, mergeHId, searchPatientForABDM, addPatientAbhNumberKey, getPatientDemographicDetails, getPatientRecord, getPatientCrossServiceVisit, getChiranjeeviDetailByJanAdhar, kioskQueue, saveKioskLabResult, uploadKioskLabResult, getMortuaryPatients, getPatientDetailsForDeathForm, updatePatientDeathDetails (03 §5: ~83 endpoints) |
| PatientDetailRegistrationServiceImpl | `/patientDetailReg` | getPatientGeneralDetailsSearch (typeahead), patientDetailReg flow |
| VisitServiceImpl | `/visit` | visit creation |
| MarkAndAddVisitServiceImpl | `/markandaddvisit` | checkin flow (used by OP emergency + kiosk) |
| HelperServiceImpl | `/helper` | registerChild, registerChildDocit |
| MasterCachedServiceImpl | `/master` | patient master lookups (gender/prefix/religion caches) |

## ABDM adjacency (M38)
- `/abha` (40 endpoints: registerWithAadhar, getProfile, linkHealthId, raiseConsentRequest…), `/hpr`, `/nhcx` — see module `integrations.md`.

## Notes
- All patient search goes through REST endpoints; the frontend never calls Solr directly (07 §1.4).
- `getPatientGeneralDetailsForSearch` is called from billing/admission flows too (cross-module reuse).
