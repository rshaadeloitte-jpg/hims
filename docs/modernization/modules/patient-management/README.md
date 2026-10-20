# Module: patient-management (M01 — Patient Management / Registration & Demographics)

> Part of the iHMS modernization module inventory. Master reference: `../../MODULE-INVENTORY.md` §M01; catalog: `../../02-business-module-catalog.md` §M01; evidence dump: `../../_discovery/agent-02/findings.md`.

**Target service (10-target-architecture.md §2.1):** 1. Patient Identity & Registration — owns `T_REGISTRATION(+DETAILS)`, `T_PATIENTSPONSOR`, `T_PATIENT_ABDM`, `T_REGISTRATIONQUEUE`; emits `PatientRegistered`.

**One-line purpose:** Patient identity, demographics, registration, encounter history, verification (Janaadhar/ABHA/Aadhaar/biometric), MLC capture, mother–child linking.

| Aspect | Summary (evidence) |
|---|---|
| Config | `fjs-config_patientManagement.json` (1 key: addSocialWorker), `fjs-config_op.json` (patientDetailReg) |
| Services | `/patient` (PatientServiceImpl), `/patientDetailReg`, `/visit`, `/markandaddvisit`, `/helper` (registerChild, registerChildDocit) |
| Key helpers | `custom/PatientServiceHelper.java` (BR-PAT rules), `BaseServiceImpl.generateMrno` |
| Business rules | BR-PAT-001 MRN generation · BR-PAT-002 duplicate detection (BERA011/BEXA040) · BR-PAT-003 family-Id coupling (BERP007) |
| Highest-risk data | `T_REGISTRATION`/`T_REGISTRATION_DETAILS`/`T_REGISTRATION_INT` — **no entity mapping** (schema only in Oracle DB), native-SQL-only (05 §3.6, recon §7) |
| Complexity | 4/5 — identity hub; Solr load-bearing; ABDM linkage; T_REGISTRATION schema extraction |

**Key dependencies:** Masters (patient demographics), M38 (ABDM/ABHA), M04 (MRN series), M02/M03 (visits), M34 (documents), M28 (death details).

**Solr:** patient collection realtime index + `searchPatientInSolr`, `searchExactPatientInSolr` (dup guard), `searchPatientInSolrByIdentity`, `searchABHAPatientHIDInSolr` (ABDM), nightly backfill `indexMissingPatientsInSolr` (07 §1.3).

**Open questions:** Temp-MRN auto-code proc semantics; VisitStatus initial value; encounter Solr 3-day retention interplay.

**Per-module files:** features.md · workflows.md · business-rules.md · screens.md · APIs.md · database.md · integrations.md · security.md · migration.md · tests.md
