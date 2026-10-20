# M01 — Business Rules (from 06-business-rules-catalog.md §B)

| ID | Rule summary | Evidence | Migration note |
|---|---|---|---|
| BR-PAT-001 | MRN generation strategy: `mrnoFromSeries` → series; `autoGenerateTempMrnNo` → temp MRN `<tempMrnPrefix>-<3-digit-seq>` (auto-code proc); offline unit → client MRN preserved; permanent MRN = `ddMMYY + patientId padded to 9` | PatientServiceHelper:373-425; BaseServiceImpl:987-1017; `cims_getlatestautocodeandincremetitbyone` | Sequence/auto-code infra must survive; temp→permanent promotion on visit/admission |
| BR-PAT-002 | Duplicate identity detection: Solr lookup `findPatientByIdentity` before insert → BERA011; duplicate healthId → BEXA040 with existing MRN | PatientServiceHelper:661-669, 783-790; Solr collection `patient_risl_qa` | Duplicate heuristics + healthId uniqueness must survive Solr retirement |
| BR-PAT-003 | Family-Id/member coupling: `familyIdNo` without `memberId` → BERP007 | PatientServiceHelper:704-706 | — |

## Status codes (06 §A1, applies to visits)
- T_Encounter.VisitStatus: 3=doctor called, 9=doctor checkout, 10=cabin released; T_Appointment: 11=bill paid, 12=pharmacy dispensed (see M02).
- MLC status fields (mlcDetailsSchema): codes UNKNOWN-11.
