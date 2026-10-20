# WP-T4 — Govt Programs service (`services/govt-service`) — Implementation Report

Tranche 6 (GAP-REGISTER-6). NEW service, Catalog #28. Package `com.hims.govt`,
port **8108**, schema **ihms_govt**, Flyway `V1__init_ihms_govt.sql`.
Conventions mirrored from `services/ordering-service` / `services/cssd-service`
(package layout, Flyway, error envelope `ApiError{status,errorCode,message}` +
BEXA-style codes, default-deny security + dev-profile override, MockMvc style,
PG16 Testcontainers integration, pom.xml incl. `-Dapi.version=1.44` pin).

## Acceptance

`mvn -f services/govt-service/pom.xml test` → **exit 0, BUILD SUCCESS**
(66 tests, 0 failures, 0 errors, 0 skipped; PG16 Testcontainers ran — Docker present).

## Implementation

### Tables (V1__init_ihms_govt.sql, all BIGINT ids per POSTGRESQL-MIGRATION)
| Table | Notes |
|---|---|
| t_beneficiary_enrolment | enrolment_id PK seq; patient_id NOT NULL; scheme CHECK IN ('CHIRANJEEVI'); enrolment_no UNIQUE NOT NULL; enrolled_on DATE NOT NULL; is_active BOOLEAN DEFAULT true; audit cols |
| t_reference_tid | tid_id PK seq; enrolment_id FK; tid VARCHAR(60) UNIQUE NOT NULL; audit cols |
| t_aasmaan_case | case_id PK seq; status SMALLINT CHECK 1..3; opened_on/closed_on DATE; **FLAG: name UNVERIFIED vs Oracle** |
| t_aasmaan_case_note | note_id PK seq; case_id FK; note TEXT NOT NULL; author VARCHAR(80); **FLAG: name UNVERIFIED vs Oracle** |
| t_ejs_asha | asha_id PK seq; asha_code UNIQUE; supervisor_id FK NULL |
| t_ejs_supervisor | supervisor_id PK seq; code UNIQUE |
| t_ejs_indicator_entry | entry_id PK seq; UNIQUE(asha_id, indicator_code, period_year, period_month); period_month CHECK 1–12; value INT DEFAULT 0 |
| t_ejs_incentive | incentive_id PK seq; amount NUMERIC(19,2) CHECK >=0; status SMALLINT 1=COMPUTED 2=APPROVED 3=PAID; computed_at |
| t_incentive_rate | indicator_code PK; rate NUMERIC(19,2); **3 seed rows** (see disclosures) |
| outbox | per schema convention — **ZERO emitters** (negative assertion) |

Sequences for all 8 id'd tables per IMP-DEC-003. Audit block = cssd-style 4-column
(added_by/added_datetime/updated_by/updated_datetime), matching the register's
generic "audit cols".

### APIs (12 paths, spec-parity verified 1:1 against controllers programmatically)
- POST `/api/v1/beneficiaries/enrol` — 201; 400 BEXA003 guards; 409 GOVT_DUPLICATE_ENROLMENT_NO
- PUT `/api/v1/beneficiaries/{enrolmentNo}/tid` — 201; 404 unknown enrolmentNo; 409 GOVT_DUPLICATE_TID
- POST `/api/v1/aasmaan/cases` — 201 OPEN, opened_on=today
- GET `/api/v1/aasmaan/cases?patientId=&status=` — numeric or name filter
- GET `/api/v1/aasmaan/cases/{id}`
- POST `/api/v1/aasmaan/cases/{id}/notes` — 201; 400 GOVT_CASE_CLOSED if CLOSED
- GET `/api/v1/aasmaan/cases/{id}/notes`
- PUT `/api/v1/aasmaan/cases/{id}/status` — forward-only; illegal 400 GOVT_ILLEGAL_TRANSITION; closing stamps closed_on
- POST `/api/v1/ejs/asha` — 201; 409 GOVT_DUPLICATE_ASHA_CODE; 400 GOVT_UNKNOWN_SUPERVISOR
- POST `/api/v1/ejs/indicators` — upsert by unique key (insert then update-in-place)
- POST `/api/v1/ejs/incentives/compute` — SUM(entry.value × rate-table); recompute replaces COMPUTED only
- PUT `/api/v1/ejs/incentives/{id}/approve` (COMPUTED→APPROVED) · PUT `.../pay` (APPROVED→PAID); else 400 GOVT_ILLEGAL_TRANSITION
- GET `/api/v1/ejs/incentives?ashaId=&status=`

### Event policy / negative outbox
The service EMITS NOTHING. `spring-kafka` is deliberately absent from pom.xml;
no `@EnableScheduling`, no relay/publisher. The outbox TABLE exists per schema
convention and the L2 integration test asserts it stays EMPTY through every flow
(migration/boot, rejected guards, enrolment+TID flow, full Aasmaan flow, full
EJS compute/approve/pay flow, rate-seed round-trip). Same contract as cssd WP-R3.

## Stub & decision disclosures (nothing silent)

1. **Aasmaan table names UNVERIFIED against Oracle** (register-flagged):
   `t_aasmaan_case` / `t_aasmaan_case_note` used as-is from the catalog; FLAGGED
   in the migration header comment, both table COMMENTs, entity javadocs, and here.
2. **Rate-table seeds invented**: register says "seed 3 rows" without naming them.
   Seeded `ANC_CHECKUP=50.00`, `DELIVERY=600.00`, `CHILD_IMMUNISATION=75.00`
   (illustrative; disclosed in migration comments too).
3. **Compute rule for missing rates**: an indicator entry whose code has no
   `t_incentive_rate` row contributes 0 to the sum (documented in code+migration+
   openapi; pinned by a dedicated test).
4. **Forward-only interpretation**: "forward-only 1->2->3" implemented as ADJACENT
   steps only (OPEN→IN_PROGRESS, IN_PROGRESS→CLOSED); repeats, backwards moves AND
   skips (OPEN→CLOSED direct) are 400 GOVT_ILLEGAL_TRANSITION. Pinned by tests.
5. **Supervisor creation API does not exist** in this slice (module-125 parity OUT
   OF SCOPE): supervisors are reference data; `supervisorId?` must reference an
   existing row when supplied → 400 GOVT_UNKNOWN_SUPERVISOR.
6. **Duplicate ashaCode → 409 GOVT_DUPLICATE_ASHA_CODE**: register doesn't state
   the status for this conflict; UNIQUE constraint exists, mapped consistently
   with the other 409s (disclosed choice).
7. **Negative indicator values rejected** (value >= 0, BEXA003): register only
   specifies `value INT DEFAULT 0`; non-negative chosen and disclosed.
8. **No unique key across t_ejs_incentive period**: deliberate — recompute keeps
   APPROVED/PAID history while replacing COMPUTED rows, so a COMPUTED and a PAID
   row may coexist for one period (documented in migration comment).
9. **TID link responds 201** (link resource created); register pins method PUT but
   not the success status.
10. **Error-code set**: BEXA003 (validation), NOT_FOUND (404),
    GOVT_DUPLICATE_ENROLMENT_NO / GOVT_DUPLICATE_TID / GOVT_DUPLICATE_ASHA_CODE
    (409), GOVT_ILLEGAL_TRANSITION / GOVT_CASE_CLOSED / GOVT_UNKNOWN_SUPERVISOR /
    BEXA003 (400). Envelope identical to siblings.
11. **AUDIT_ACTOR_ID = 1L** placeholder principal (MIG-004 will replace; sibling convention).

## OUT OF SCOPE — comment-only (no implementation, per register)
- EJS module 125 full endpoint parity (migration comment + EjsAsha javadoc).
- Aasmaan module 78 full endpoint parity + ANDROID/media/profile tables (migration header).
- External **PCTS** pushes — comment-only (migration header + application class).
- External **Chiranjeevi-Yojana** scheme pushes — comment-only (migration + BeneficiaryEnrolment javadoc).
- **IncentiveComputed** event — DEFERRED per catalog (comment-only everywhere; zero emitter).
- **CaseUpdated** event — DEFERRED per catalog (same).

## Tests (66 total, mvn test exit 0)

| Suite | Layer | Count | Covers |
|---|---|---|---|
| BeneficiaryServiceTest | L0 unit | 9 | enrol guards (patientId/scheme/enrolmentNo/enrolledOn, 409 dup), tid linkage (404 unknown, 400 blank, 409 dup), happy paths |
| AasmaanServiceTest | L0 unit | 14 | create guards, opened_on, closed-case note guard, forward-only machine (adjacent ok, backward/repeat/skip/terminal 400), status parse, 404s |
| EjsServiceTest | L0 unit | 16 | asha guards (code/name/dup 409/unknown supervisor), indicator upsert insert-vs-update, period/value validation, compute math (1100.00), missing-rate→0, recompute deletes prior COMPUTED ONLY (keeps APPROVED+PAID), approve/pay machine incl. direct-pay rejection, terminal states |
| BeneficiaryControllerTest | MockMvc | 6 | default-deny 401 on every endpoint, error envelopes (BEXA003/GOVT_DUPLICATE_ENROLMENT_NO/NOT_FOUND), 201 happy paths |
| AasmaanControllerTest | MockMvc | 8 | default-deny 401, GOVT_CASE_CLOSED + GOVT_ILLEGAL_TRANSITION envelopes, named/numeric status filters, notes listing, 404 envelope |
| EjsControllerTest | MockMvc | 8 | default-deny 401 on all 6 endpoints, compute 201 COMPUTED, approve/pay happy + guard envelopes, filters |
| GovtServiceIntegrationTest | PG16 Testcontainers | 5 | Flyway V1 applies (10 tables, 8 sequences, 3 seeded rates); beneficiary enrol+TID flow w/ DB-level uniqueness backstop; Aasmaan forward-only + closed-case guard on real data; EJS upsert/compute (1100.00 vs seeded rates)/recompute-rule/approve-pay round-trip; seeded-rate round-trip + missing-rate→0. NEGATIVE OUTBOX assertion after EVERY flow |

No test deleted or disabled to go green. Integration tests auto-skip only when
Docker is absent (`@Testcontainers(disabledWithoutDocker = true)` — sibling convention).

## Files
```
services/govt-service/
├── pom.xml                                   (no spring-kafka — zero emitters)
└── src/main/java/com/hims/govt/
    ├── GovtServiceApplication.java           (no @EnableScheduling — no relay)
    ├── api/          ApiError, GlobalExceptionHandler, BeneficiaryController,
    │                 AasmaanController, EjsController
    ├── application/  BeneficiaryService, AasmaanService, EjsService
    │   └── exceptions/ GovtRuleException, InvalidGovtRequestException,
    │                   GovtNotFoundException, GovtConflictException
    ├── configuration/ SecurityConfig (!dev default-deny), DevSecurityConfig (dev)
    ├── domain/model/ GovtBase + 9 entities
    ├── domain/repository/ 9 repositories
    └── infrastructure/ outbox/OutboxEvent + OutboxRepository (read-only,
                        negative-assertion use), web/CorrelationIdFilter
services/govt-service/src/main/resources/
    ├── application.yml (port 8108, ihms_govt), application-dev.yml
    └── db/migration/V1__init_ihms_govt.sql
services/govt-service/src/test/java/com/hims/govt/ (7 test classes, 66 tests)
openapi/govt-service.yaml                (12 paths, 1:1 with controllers — verified programmatically)
outputs/implementation/WP-T4-govt.md     (this report)
```
