# M03 — Database (05 §3, 10 §2.1 row 3)

## Tables
| Table | Notes |
|---|---|
| T_ADMISSION / T_ADMISSION_DETAILS | Native SQL in parts (05 §3.6: `t_admission` unmapped); BR-IPD/DIS statuses |
| T_ADVISE_DISCHARGE / T_DISCHARGE | Discharge advice/execution (CIMS_ADDBEDDISCHARGE) |
| T_BEDRESERVATION | Bed reservations |
| T_TRANSFERREQUEST | Transfer requests (closed on discharge) |
| T_PATIENTDISCHARGEDATA | Discharge data (incl. death flags) |
| T_MLCDETAILS | MLC (shared M01) |
| T_ATTENDANTPASS | Attendant passes |
| T_REGISTRATION (native) | Patient root |
| M_BEDMASTER (26 refs) | **OCCUPIED char flag updated by procs**; BedStatusId |
| masters/admission (~20) | bedmaster, wardmaster, roommaster, classmaster, blockmaster, buildingfloorstructure, wingmaster, dischargetype, admissiontype, daycaremaster, modeOfDelivery, actualBedCountMaster, bedstatusmaster, wardtypemaster, floormaster, doctortypemaster, identitytype, buildingmaster, classWardMappingForAdmissionBlock |

## Stored procedures (CIMS_*)
- `CIMS_ADDADMISSIONDETAILS` (writes M_BEDMASTER.OCCUPIED)
- `CIMS_ADDADMISSIONINFORMATION` (inserts T_Admission; dead APP015 branch — RISK R4)
- `CIMS_UPDATEBEDOCCUPIEDFLAG_1`
- `CIMS_ADDBEDDISCHARGE`, `CIMS_UPDATEIPDISDISCHARGE` (discharge + death + bed release)
- Sequences: `T_AdmissionDetails_seq`, `T_PrescriptionDetails_seq` (procs)

## Migration notes
- Target service owns ADT tables; emits `PatientAdmitted`, `PatientDischarged`, `BedChanged` (bed auto-charges), `TransferRequested`.
- Bed CAS + literal BedStatusId=6 contract must be pinned (06 §C.4).
- M_BEDMASTER.OCCUPIED is `char` '1'/'0' — keep char(1) or boolean per audit (05 §8).
