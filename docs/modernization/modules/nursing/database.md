# M09 — Database (05 §3, 02 §M09)

## Tables
| Table | Notes |
|---|---|
| T_NURSENOTE | Nurse notes (note titles, signing) |
| T_NURSETASKDETAILS / T_NURSETASKFREQUENCYDETAILS | Task lists & frequencies (shared M26) |
| T_PATIENTNOTE | Patient notes |
| T_PATIENTVITALS | Vitals |
| T_PATIENTALLERGIES / T_PATIENTCOMPLAINTS / T_PATIENTDIAGNOSISDETAILS / T_PATIENTIMMUNIZATIONDETAILS | Clinical documentation |
| T_PATIENTDISCHARGEDATA | Discharge data (advice/education) |
| T_PATIENTADVICE / T_PATIENTEORDER | Advice & orders |
| T_INTAKEOUTPUT | I/O charting (10 §2.1 row 12) |
| T_PATIENTNURSINGORDERS | Nursing orders (orderstatus DISCONTINUE — BR-PATH-001) |
| CarePlan(+Details) | NCP problems/goals |
| masters/nursing | notetitleMaster, nursingordermaster, flagMaster, vitalmaster, growthchart*(details/menu/menuchild/series/seriesdata), healthFactor*(Category/Factor), diseaseMaster, icdcmCategory, dischargeDepartmentTemplate, cwfmapping, scwfmapping, diagnosisMaster, allergyMaster, chartconfiguration, imageAnnotationMaster, immunizationagegroupmaster, notifyablediesase, vaccine*(master/immunizationagegroup), highRiskPregnancyFactorMaster, specialityMaster, noteCategoryMaster, documentClassMaster |

## Sequences
- `T_nursePatientDetails_seq` (05 §4).

## Status codes
- task status (completeNursingOrderTask); incident status; note signed status; patient-med 1=ORDERED..4=SKIPPED; medication 1=ACTIVE..3=EXPIRED (06 §A9); UNKNOWN-15 code lists (02).

## Migration notes (10 §2.1 row 12)
- Target owns T_NURSENOTE, T_PATIENTVITALS, T_NURSETASKDETAILS, T_INTAKEOUTPUT, nursing-station masters; emits `VitalsRecorded`, `NurseTaskCompleted`; consumes PatientAdmitted/PatientDischarged/OrderCreated.
