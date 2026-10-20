# M01 — Workflows

## Registration → visit (02 §M01 Workflow)
1. Search/verify identity (Aadhaar / Janaadhar / ABHA / MRN) — Solr-first (searchPatientInSolr), Redis mirror `current_pat_` (100s TTL) to mask soft-commit lag.
2. Duplicate check: `searchExactPatientInSolr` (mobile+DOB); hit → `BERA011`; duplicate ABDM healthId → `BEXA040` (BR-PAT-002).
3. Register demographic → generate MRN (series or temp `<tempMrnPrefix>-<3-digit-seq>` via `cims_getlatestautocodeandincremetitbyone`, BR-PAT-001).
4. Create visit (OPD/IPD/emergency) — see M02/M03.
5. Optional MLC capture (broughtBy, isBroughtDead, formalityRemark).
6. Optional social-worker assessment.
7. Temp MRN promoted to permanent on visit/admission (EmergencyAndWalkinVisitServiceImpl:678-684, PatientRegAdmissionServiceImpl:378-386).

## Solr/Redis consistency (07 §1.3)
- DB save → SolrUtil.indexPatientInSolr → `cacheImpl.add(current_pat_, TTL 100s)`; on tx failure the AOP layer deletes the Solr doc + Redis keys (ServiceLayerAspect:1164-1182).
- Nightly backfill `indexMissingPatientsInSolr` (18:00–07:00, SOLR_STATUS=0, batch 1000, only `solrsync.servers` nodes).
- Registration search with ≤15 hits re-verifies against DB and deletes stale Solr docs (SolrUtil:618-628).

## Family/member coupling (BR-PAT-003)
- `familyIdNo` supplied without `memberId` → `BERP007`.
