# WP-3 — patient-service sponsor + merge APIs
_Worker: patient-implementer · Scope: Catalog #1 (Patient Identity & Registration) gaps per GAP-REGISTER WP-3._

## What was implemented
1. **Sponsor endpoints** (T_PATIENTSPONSOR clean target):
   - `POST /api/v1/patients/{id}/sponsors` — link a sponsor/scheme row (`sponsorType` required → BEXA003 otherwise); audit fields server-set (IMP-DEC-023).
   - `GET /api/v1/patients/{id}/sponsors` — list active sponsor rows for the patient.
2. **Merge**: `POST /api/v1/patients/merge {sourceId,targetId}` — re-points document rows AND sponsor rows to the target (bulk JPQL `repointToTarget`, status-true rows only), marks source `status=false`, emits **PatientMerged** through the existing transactional outbox in the SAME tx. Partition key = TARGET patientId (consumers see merge ordered after the surviving patient's events). Guards: null/same ids → BEXA003; unknown ids → 404 NOT_FOUND ("Source…"/"Target…"); already-inactive source → BEXA012. Response: `{sourceId, sourceMrno, targetId, targetMrno, documentsRepointed, sponsorsRepointed}`.
3. **Event-catalog drift check**: outbox TYPE strings for register/update were verified against KAFKA-EVENT-CATALOG §2 E-01 — code emits exactly `'PatientRegistered'` / `'PatientUpdated'` (aggregate `PatientRegistration`, version v1, partition key patientId). **No drift found; no fix needed.** New event uses `'PatientMerged'`, same envelope.
4. **Flyway V2__patient_sponsor.sql** (schema ihms_patient): `patient_sponsor` table following V1 conventions (full audit block, explicit `patient_sponsor_seq`, FK → `patient_registration(id)`, index on `patient_id`, COMMENT ON TABLE).
5. **openapi/patient-service.yaml** updated: new paths `/api/v1/patients/{id}/sponsors` (GET+POST) and `/api/v1/patients/merge` (+400/404), schemas `PatientSponsor`, `PatientMergeResult`. YAML parses clean.

## Files added
- `services/patient-service/src/main/java/com/hims/patient/domain/model/PatientSponsor.java`
- `services/patient-service/src/main/java/com/hims/patient/domain/repository/PatientSponsorRepository.java`
- `services/patient-service/src/main/java/com/hims/patient/api/PatientMergeResult.java`
- `services/patient-service/src/main/resources/db/migration/V2__patient_sponsor.sql`

## Files changed
- `application/PatientRegistrationService.java` — sponsorRepository dep; `linkSponsor`/`sponsors`/`merge` + merge-event append; sequence helper.
- `api/PatientController.java` — 3 new endpoints.
- `domain/repository/PatientDocumentRepository.java` — `repointToTarget` @Modifying query.
- `openapi/patient-service.yaml`.
- Tests: `PatientRegistrationServiceTest` (sponsor-seq stub + 3 new tests: sponsor validation, merge repoint/deactivate/PatientMerged envelope incl. idempotency key `PatientMerged:M{src}:M{tgt}`, merge guards incl. never-emitting on inactive source), `PatientControllerTest` (+2 contract tests).

## Test output summary
`mvn -f services/patient-service/pom.xml test` → **BUILD SUCCESS, Tests run: 31, Failures: 0, Errors: 0, Skipped: 0**
- PatientControllerTest: 9/9 · PatientRegistrationServiceTest: 16/16 · PatientRegistrationIntegrationTest (Testcontainers PG16+Kafka, Docker present): 6/6 — V2 migration applied on live PG and full flow verified end-to-end.

## Env-dependent tests
None failing; nothing deleted or skipped. The L2 Testcontainers class auto-disables only without Docker (`@Testcontainers(disabledWithoutDocker = true)`); in this env Docker was available and all 6 passed.

## Deviations from docs
- GAP-REGISTER names no error codes for merge guards. Chose existing legacy-style codes: BEXA003 (invalid input) consistent with sibling validation; **BEXA012** invented for "source already merged/inactive" — flagged here for overseer review (no STATUS-CODE-DICTIONARY entry exists yet).
- Sponsor table columns (sponsor_type/scheme_code/policy_no/valid_from/valid_to) are a minimal clean-target subset of legacy T_PATIENTSPONSOR; legacy exact column dump not in scope of this repo's discovery artifacts.
- Merge does not copy/dedup demographics from source to target (pure re-point + deactivate), matching the register wording exactly.
