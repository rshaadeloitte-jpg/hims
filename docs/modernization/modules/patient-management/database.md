# M01 — Database (from 05-database-catalog.md, agent-02 findings §6)

## Tables
| Table | Notes |
|---|---|
| T_Encounter | **52 refs — highest-shared table in system** (05 §7); visit statuses (see M02) |
| T_REGISTRATION, T_REGISTRATION_DETAILS, T_REGISTRATION_INT | **No JPA entity** — native SQL only (05 §3.6); **schema truth must be dumped from Oracle**; critical migration risk (recon §7) |
| T_REGISTRATIONQUEUE | Registration queue |
| T_MLCDETAILS | Medico-legal case |
| T_ANCVISITDETAILS | ANC visit details (PCTS linkage) |
| T_PATIENTSPONSOR | Sponsor per patient (BR-OPD-005 writes via CIMS_ADDPATIENTSPONSOR_NEW_DOC) |
| T_PATIENT_ABDM (+_CC, _SHARETOKEN) | ABHA linkage |
| T_PATIENT_CONSENT_REQUESTS_ABDM (+_DATA) | ABDM consent |
| T_USER_FACEAUTH | Face auth |
| FamilyMemberDetails, SpouseAndChildrenDetails, PatientHqDetails, PatientDemoDetailsMismatch | Family/demographic mismatch tracking |
| M_MRNSeries | MRN series master |
| M_UNITMASTER | **Tenant root** (UnitID filters nearly every query; 45 refs) |
| masters/patient (~50) | area, camp, caste, city, consent, country, district, gender, incomecategory, language, maritalstatus, mothertongue, nationality, occupation, patientcategory, patientsource, patienttype, placeofbirth, prefix, qualification, race, refentity, relation, religion, riskfactor, state, triage, visittype, registrationcustomfields… |

## Identity & sequences
- PKs manually assigned from `<TABLE>_SEQ` (ServiceLayerAspect:697-750; Oracle `SELECT <seq>.NEXTVAL FROM DUAL`, PG branch already coded).
- MBase audit columns on ≥750 tables: UNITID, STATUS, CREATEDUNITID, UPDATEDUNITID, ADDEDBY, ADDEDDATETIME, UPDATEDBY, UPDATEDDATETIME, SYNCHRONIZED.

## Stored procedures
- `CIMS_ADDPATIENTREGISTRATION*` (10 §2.1 row 1 legacy sources)
- `cims_getlatestautocodeandincremetitbyone` (temp-MRN auto-code)
