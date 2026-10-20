# M01 — Features (evidence from 02 §M01, agent-02 findings §2)

| Feature | Evidence |
|---|---|
| registerPatient (new patient registration) | PatientServiceImpl `/patient/registerPatient` (BR-PAT-001/002 evidence in PatientServiceHelper:373-425, 661-669) |
| Demographic search (Aadhaar / driving licence / Janaadhar) | getPatientByAddhar, getPatientByDrivelicNo; Solr identity search (SolrUtil.searchPatientInSolrByIdentity) |
| MLC (medico-legal case) details + documents | mlcDetailsSchema / mlcDetailsSchema_Form, updateMLCDetailsSchema; MLC status fields |
| ABHA/ABDM patient matching & ABHA number keying | searchPatientForABDM, addPatientAbhNumberKey; abhaPatientRegistration, searchAbhaPatient, baalAbhaReKyc (child re-KYC) |
| HID merge (duplicate/merged health IDs) | mergeHId |
| Family member details / mother–child linking | FamilyMemberDetails, SpouseAndChildrenDetails, motherChildLinkingSchema |
| Corona/COVID quarantine tracking | patientCoronaDetailsSchema, showCoronaPatientReport, geofenceViolated |
| Social worker assessment | addSocialWorker (fjs-config_patientManagement — 1 key) |
| Teleconsult room creation | PatientServiceImpl (teleconsult; shared with M02) |
| MRN generation (series / temp / offline) | BR-PAT-001: mrnoFromSeries / autoGenerateTempMrnNo / offline-unit preserved; permanent MRN `ddMMYY+patientId padded 9` |

## Client-side behavior (03 §1)
- Walk-in screen does search-as-you-type on MRN/mobile/aadhaar/bhamashah/janaadhar/abha/ECTS/PCTS (≥ length thresholds, crudop lookups).
- Aadhaar serialized as base64 (`encodeAdharToBase64`) before POST.
- Validation: `validateJanaadharId`, `validateBhamashahId`, `validateAdharNumber`, `validateHIDNO`, `validateMobileNoByAPI`, `validateAge`.
