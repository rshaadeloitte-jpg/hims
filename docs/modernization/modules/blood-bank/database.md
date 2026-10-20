# M17 — Database (05 §3, 02 §M17)

## Tables (21 blood-bank)
| Table | Notes |
|---|---|
| t_bloodbagcollection / t_bloodbagissue / t_bloodbagreturns / t_bloodbagdecross / t_bloodbagissuedemandslip | Bag lifecycle |
| T_BLOODBAGRECEIVE | Receive |
| t_blooddonorregistration(+details) / t_donorscreeningtest / t_donormedicalexaminationresult | Donor lifecycle |
| t_bloodcrossmatchingresult(+details) | Cross-match |
| T_BloodBankIndent(+Details) | Inter-unit indents |
| T_BloodBagExtraction | Component extraction |
| T_BLOODBAG / T_BLOODCOMPONENT / T_BLOODREQUISITION / T_BLOODDONOR / T_BLOODDONATIONCAMP | Core (10 §2.1 row 13 naming) |
| masters/bloodBank (~22) | bloodgroupmaster, bloodcomponentmaster, bloodbagtypemaster, bloodbagtypecomponentmaster, bloodscreeningmaster, bloodtestmaster, bloodtestresultmaster, blooddiscardreasonmaster, blooddonationcampmaster, externalbloodstoragecenter, externalhospitalmaster, eraktkoshcomponentmapping, donordefaultvitalvalue, importerMaster, modeofscarpsale, additivemaster, bloodadversereactionmaster, bloodcomponentclinic, bloodgroupcrossmatching, bloodrequisitionreasonmaster, requesttypemaster |

## Stored procedures
- None directly evidenced; eRaktkosh jobs in ApplicationJobs (1205, 1383).

## Sequences
- Per-table `<t>_seq` convention (manual PKs).

## Migration notes (10 §2.1 row 13)
- Target owns T_BLOODBAG, T_BLOODCOMPONENT, T_BLOODREQUISITION, T_BLOODDONOR, T_BLOODDONATIONCAMP, donor masters.
- Emits `BloodRequisitionCreated`, `BloodIssued`; consumes PatientAdmitted, LabResultFinalized (serology).
