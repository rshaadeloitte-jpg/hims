# WP-R4 — RCM & Claims service (Catalog #23) — implementation report

Tranche 5, GAP-REGISTER-5 WP-R4. NEW `services/rcm-service` (port **8103**, schema **ihms_rcm**),
package `com.hims.rcm`, mirroring services/ordering-service conventions exactly
(Spring Boot 3.5.x / Java 21, Flyway `V1__init_ihms_rcm.sql` with explicit sequences
(IMP-DEC-003), default-deny security config + dev-profile Basic fallback, error envelope +
BEXA-style codes, CorrelationIdFilter, MockMvc default-deny tests, PG16 Testcontainers).

## What was implemented

### Schema (V1__init_ihms_rcm.sql)
| Table | Notes |
|---|---|
| `t_eligibility_check` | eligibility_id PK seq; patient_id NOT NULL; payer_scheme VARCHAR(60); `check_status SMALLINT DEFAULT 0 CHECK 0..3`; request_payload JSONB NOT NULL; response_payload JSONB NULL; audit cols |
| `t_admission_tid` | tid_id PK seq; admission_id NOT NULL UNIQUE (`uq_t_admission_tid_admission`, DB-level backstop for the 409); tid VARCHAR(60) NOT NULL; audit cols |
| `t_nhcx_request` | request_id PK seq; kind SMALLINT CHECK IN (1,2,3) = COVERAGE_ELIGIBILITY/PRE_AUTH/CLAIM_SUBMIT; patient_id NOT NULL; bill_id/admission_id NULL; `request_status SMALLINT DEFAULT 0 CHECK 0..3`; request_payload JSONB NOT NULL; response_payload JSONB NULL; correlation_id VARCHAR(64) UNIQUE (`uq_t_nhcx_request_correlation` — callback idempotency anchor); audit cols |
| `t_nhcx_request_detail` | composite PK (request_id, line_no) mirroring sibling prescription-detail convention; detail JSONB NOT NULL; one row per payload.lines entry, else a single full-payload row (documented interpretation of the register's bare table spec) |
| `outbox` | transactional outbox TABLE per schema convention — see event policy below |

**Column-naming note:** the base audit block carries the legacy boolean `status` column
(MBase parity, same as sibling appointment-service). The domain NHCX status columns are
therefore named `check_status` / `request_status` — exactly the sibling pattern used for
`t_appointment.appt_status`. Register semantics ("status SMALLINT 0..3") are unchanged;
only the physical name avoids the duplicate-column collision. Pinned by entity mappings.

### NHCX status machine (register WP-R4, 06 §A9 mapping: 0..3)
**0=INITIATED → 1=SENT → 2=ACKNOWLEDGED → 3=COMPLETED/ERROR**, forward-only and monotonic:
- callback on unknown correlation_id → **404 envelope**
- status null/outside 0..3 → **400 BEXA003**
- re-applying the CURRENT status → **idempotent no-op success** (response_payload NOT overwritten)
- backward/different-after-terminal transition → **400 RCM_ILLEGAL_TRANSITION**
- `PUT /api/v1/coverage-eligibility/{id}/response` pins the CHECK directly to 2=ACKNOWLEDGED;
  guard: only from INITIATED (second response → RCM_ILLEGAL_TRANSITION). The companion
  t_nhcx_request row keeps its own status 0 until a callback arrives (documented; tested).
- DEVIATION NOTE (also in NhcxStatus javadoc): docs §A9 as captured only evidences
  0=ACCEPTED .. 3=FAILED with labels 1..2 UNKNOWN (STATUS-CODE-DICTIONARY "NHCX request
  status 1–2"); the register's binding mapping is authoritative here and pinned by tests.

### APIs (openapi/rcm-service.yaml matches controller paths 1:1 — 7 paths / 8 operations)
- POST `/api/v1/coverage-eligibility` {patientId, payerScheme} → 201, check status 0 + companion kind=1 request with fresh correlation id
- PUT `/api/v1/coverage-eligibility/{id}/response` {payload} → check pinned to 2
- POST `/api/v1/pre-auths` {patientId, admissionId?, payload} → kind=2 record
- POST `/api/v1/claims` {patientId, billId, payload} → kind=3 record (billId required)
- PUT `/api/v1/nhcx/{correlationId}/callback` {payload, status} → idempotent state machine above
- GET `/api/v1/nhcx?kind=&status=` (numeric or name filters, both validated BEXA003)
- GET `/api/v1/admissions/{admissionId}/tid` · PUT `/api/v1/admissions/{admissionId}/tid` {tid} (duplicate → 409 TID_EXISTS)

### Event policy (register: ClaimSubmitted DEFERRED per KAFKA-EVENT-CATALOG §3)
- NOTHING reaches the outbox from any code path. The application services take no emitter
  dependency (pinned structurally by an L0 test); the PG16 integration test asserts explicit
  NEGATIVE outbox assertions: count == 0 at boot, after every rejected guard, mid-flow after
  eligibility ack + pre-auth/claim creation, and after the full callback machine.
- HCX/NHCX external gateway calls OUT OF SCOPE this tranche — requests persisted with
  correlation ids, callbacks simulated locally (contract-level; disclosed per register).
- Consuming InvoiceGenerated / PaymentReceived lands post-saga — comment-only.

### Tests (50 total: 21 L0 service + 27 controller/default-deny + 6 TID + 2 PG16 integration)
- L0: creation guards (patientId/payerScheme/billId/payload/correlation length), status
  machine forward/idempotent/backward/terminal, unique-correlation-per-request, lines→detail
  split, filter parsers.
- Controller MockMvc: default-deny enforced (401/403 without role), happy paths, all mapped
  error envelopes (BEXA003 / RCM_ILLEGAL_TRANSITION / NOT_FOUND).
- Integration (PG16 Testcontainers, auto-skipped without Docker): Flyway applies all 4 tables +
  outbox + 4 sequences; full eligibility→pre-auth→claim→callback→TID flow; callback idempotency
  leaves row-count unchanged; DB-level unique backstop on uq_t_admission_tid_admission asserted
  via direct JdbcTemplate insert expecting DataIntegrityViolationException.

## Repair pass note (tranche-5 completion)
This WP was completed by a repair worker after the original implementer died mid-task. Main
sources compiled; test sources had leftover broken symbols (nonexistent OutboxRepositoryPlaceholder
import, `Nhxs.` typos, an org.assertj.db placeholder statement, missing imports/statics) which were
fixed TEST-SIDE against the real production classes — plus two production-side fixes required for
correctness: the duplicate-column collision in V1__init_ihms_rcm.sql described above, and nothing else.
No tests were deleted or disabled to go green; final `mvn -f services/rcm-service/pom.xml test`
exits 0 with **Tests run: 50, Failures: 0, Errors: 0, Skipped: 0** (PG16 integration included).

## Deliverables
- services/rcm-service (37 main+test java files, V1 migration, configs)
- openapi/rcm-service.yaml (controller-path parity 1:1)
- outputs/implementation/WP-R4-rcm.md (this file)
