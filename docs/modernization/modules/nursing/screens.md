# M09 — Screens & Routes

## Screens (schemas, 28-key config + template-only)
ShowNurseWorklistSchema · ShowNursePatientNoteSchema · ShowNursePlanOfCareSchema · ShowPatientCoversheetSchema · incidentReportSchema · incidentResolveSchema · nurseWardAllocationSchema · outreach_data_schema · ShowNurseListSchema · nurseListForPatientAllocationschema · nurseListForDoctorAllocationSchema · nurseListForDepartmentAllocationSchema · alliedStaffListForPatientAllocationschema · staffListForClinicAllocationSchema · tempPrescriptionListSchema · consultPatientSchema · HistoryClinic · ancCoronaSurveySchema · nurseTaskList (worklist) · +40 tiuxml note schemas.

## Routes (22+, 03 §2.4)
`nursing:roleNurse` · `roleLab` · `roleRadiology` · `roleFrontOfficer` · `roleNursePatientView` · `roleNurseIncharge` · `roleAlliedPatientView` · `doctorPatientView` · `rolePatientCoversheet/:patientId/:admId/:patientIen/:visitIen` · `ipCoversheet/.../:opdipd` · `rolePatientNote` · `rolePlanofCare` · `roleNurseRostering` · `MyOpPatients` · `showMortuary` · `showTransfer` · `showDischarge` · `patientNotes/:patientId/:admId/:patientIen/:visitIen/:noteIen/:noteTitleId` · `showCarePlan/...` · `nursingOrder/:tab` · `NursePrescriptionList/:tab` · `compliantDiagnosisMap` · `orderingSet` · `growthchartdetails/add/:id` · `incidentReporting` · `addnewIncident/:...` · `editIncident/:...` · `resolveIncident/:...` · `DiagnosisTreatment` · `viewCoversheet/...` · `nhncOrdering/...` · `nhncOperatorFlow` · `docPrescriptionList` · `annotation` · `charting:chartingApp` · `chartingExpand`.

## Key endpoints (03 §2.4)
- `nursingList/*` (~293 unique — **the single largest API surface**: patient lists, allocation, acuity, shift handover, EDOD, notes, ICDCM search, advice, lab/rad order fetch).
- `clinical/getProviderList`, `clinical/healthfactorMaster`, `serviceOrder/allServiceFastByGet`, `pharmacy/getMedications`.
