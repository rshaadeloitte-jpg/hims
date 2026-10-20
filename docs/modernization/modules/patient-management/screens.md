# M01 — Screens (FAUG schema keys) & Routes

## Screens (schemas)
| Schema | Purpose |
|---|---|
| addpatientDetailRegdto-schema | Patient detail registration form |
| patientRecordSchema | Patient record view |
| mlcDetailsSchema / mlcDetailsSchema_Form | MLC details capture |
| updateMLCDetailsSchema | MLC update |
| motherChildLinkingSchema | Mother–child link |
| addSocialWorker | Social worker assessment (fjs-config_patientManagement) |
| patientCoronaDetailsSchema | COVID quarantine tracking |
| showPatientDetailSchema / showPatientHistorySchema / patientHistorySchema / patientMedicalRecordSchema | History & record views |
| updateJanaadharSchema | Janaadhar update |
| sponsorSchema | Sponsor details |
| abhaPatientRegistration / childAbhaPatientRegistration / baalAbhaReKyc / searchAbhaPatient | ABHA flows |

## Routes (03 §2.1/§2.12)
- `op:patientDetails/:allowEdit`, `op:patientHistory/:mrno`, `op:editPatientDetail/:mrnNo/:unitId`
- `commons:patientList`, `commons:patientHistoryList`, `commons:patientRecord`, `commons:updatePatient`

## Key screens with 12-point docs (03 §2.1)
- Walk-in visit `op:appointmentFlow` — flagship; patient search typeahead, ABHA modal, RGHS verification modal (owned jointly with M02).
