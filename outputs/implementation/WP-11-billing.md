# WP-11 — Billing & Settlement service (Tranche 4 Revenue Spine)

_Status: COMPLETE. `mvn -f services/billing-service/pom.xml test` exits 0 — **30 tests, 0 failures, 0 errors, 0 skipped** (Docker present; PG16 Testcontainers tests executed)._

## Scope delivered (GAP-REGISTER-4 §WP-11)

Service: `services/billing-service`, package `com.hims.billing`, artifact `billing-service`,
port 8098, schema `ihms_billing`, Flyway `V1__init_ihms_billing.sql` (pre-existing, verified).

### Compile fixes applied (PENDING-GAPS-PROMPT "Exact compile blockers")

1. `Charge.java` referenced missing `ChargeCalculation` → added
   `import com.hims.billing.application.BillingCalculator.ChargeCalculation;`
   (single shared record defined in `BillingCalculator`; no duplicate domain class invented).
2. `BillController.createCharge` passed raw `Object body.rate()` into
   `BillingService.NewCharge(BigDecimal…)` → wrapped with the existing `dec(Object)` helper.
3. `BillingBase` lacked setters for `addedDatetime` / `updatedDatetime` → added both.
4. `OutboxEvent` lacked `setProducerService(String)` → added.

### Business rules implemented & pinned by tests

| Rule | Behaviour | Pinned by |
|---|---|---|
| BR-BILL-001 | billNo placeholder `'000000000000000'` at creation; final `'OPDBILL-<seq>'` when no admissionId, `'IPDBILL-<seq>'` otherwise (separate sequences); cashCounterId default 1; re-finalize rejected (`BILL_ILLEGAL_TRANSITION`) so a second number can never be drawn | `BillingServiceTest.brBill001_*` (3), integration test asserts OPDBILL variant + sequence usage |
| BR-BILL-004 | per charge: `taxN = rate×pctN/100`, or `(rate−rate×conPct/100)×pctN/100` when `apply_after_concession` (conPct per BR-BILL-005 mode); TaxAmount = Σ taxN added to SELF side | `BillingCalculatorTest.brBill004_*` (3) incl. before/after-concession divergence between company paths |
| BR-BILL-005 | asymmetric split: coPay 0 ⇒ 100% self; SELF path: concession plan%+bill% ADDED and applied to selfRate; NON-SELF path: bill% REPLACES plan% and applies to nonSelfRate. Divergence pin: identical inputs on the two paths yield different money | `BillingCalculatorTest.brBill005_*` (4) incl. explicit RISK-R2 asymmetry pin; SELF path also asserted end-to-end in the integration test (340/60/600) |
| BR-BILL-002 | skip when doctor_share_pct=0 or `is_ot_procedure`; share = lineTotal×pct/100 **CEILING** scale 2; hospital = total − share HALF_UP scale 2; row persisted to `t_doctor_share` with `doctor_id` from request-header field prescriberDoctorId, `is_billed=false`. CEILING-vs-HALF_UP divergence pinned at 13.33 × 33.33% → 4.45 (not 4.44) | `BillingCalculatorTest.brBill002_*` (4), `BillingServiceTest.finalize_persistsDoctorShareRow_isBilledFalse`, `finalize_skipsDoctorShare_whenOtProcedure_evenWithPctAboveZero` |
| BR-BILL-006 | settlement ⇔ BOTH balances ≤ 0; internal endpoint takes CUMULATIVE paidSelf/paidNonSelf and RECOMPUTES balances (idempotent replay-safe); partial payment leaves unsettled; overpayment stays settled | `BillingServiceTest.brBill006_*` (4), integration settle→replay section |
| BR-BILL-008 | snapshot columns `total/self/nonself_before_finalization` filled at finalize (restore contract); cancellation itself stays monolith-side this tranche (documented, not silently dropped) | `BillingServiceTest.finalize_snapshotsBeforeFinalizationColumns_onEveryCharge`; DB-level check in integration test |

### E-07 InvoiceGenerated — exact contract

Emitted ONCE per bill inside the finalize transaction via transactional outbox:

- type string `InvoiceGenerated`, version/schema token `v1` / `invoice-generated.v1` (envelope `schemaName`),
- topic `ihms.billing.events` (config `app.kafka.topic.billing-events`), partition key `billId` (as string),
- idempotency key `invoice-generated:<billId>`,
- payload keys EXACTLY `{billId, billNo, patientId, encounterId|null, admissionId|null,
  header{total,tax,concession,self,nonSelf}, lines[{chargeId,serviceName,rate,qty}],
  charges{taxTotal,concessionTotal,self,nonSelf},
  doctorShare{doctorId,shareAmount,hospitalAmount}|null, isInterimOrFinal:'FINAL'}`.

Pinned by: `BillingServiceTest.finalize_emitsInvoiceGenerated_exactlyOnce_withExactContractFields`
(exactly one append; rejected second finalize emits nothing further) and the PG16 integration test
asserting the single outbox row's every column + full payload key set/values.

### APIs (match `openapi/billing-service.yaml` 1:1)

- `POST /api/v1/charges` — TEMP charge rows (bill_id NULL).
- `POST /api/v1/bills` — header with placeholder billNo, attaches charges (rejects missing/
  already-attached/cancelled charges).
- `GET /api/v1/bills/{id}` — bill with charge lines.
- `POST /api/v1/bills/{id}/finalize` — all rules above + E-07.
- `POST /api/v1/bills/{id}/concession` — pre-finalize only; 400 `BILL_ALREADY_FINALIZED` after.
- `PUT /api/v1/internal/bills/{id}/settlement` `{paidSelf, paidNonSelf}` — synchronous
  payments→billing contract, tagged `internal` in the spec; returns new state; default-deny like
  everything else until MIG-004 brings service-to-service auth.

Error envelope `ApiError{status, errorCode, message}` via `GlobalExceptionHandler`
(400 rule-code / 404 NOT_FOUND / 401 default-deny), same shape as sibling services.

## Tests

- `BillingCalculatorTest` — 11 (pre-existing L1 money-rule pins; untouched).
- `BillingServiceTest` (NEW, 12) — numbering placeholder→OPDBILL/IPDBILL + double-finalize guard,
  snapshots, E-07 once with exact contract fields, doctor-share persist/skip, BR-BILL-006 partial/
  full/replay/overpay/negative-input, concession-after-finalize rejection.
- `BillControllerTest` — 5 MockMvc tests (default-deny incl. the internal endpoint, error-free happy
  paths). One latent stub-matcher bug fixed: request body now sends `"isSelfCompany":true`
  explicitly so the Mockito matcher matches (no production change).
- `BillingServiceIntegrationTest` (NEW, 2, PG16 `postgres:16-alpine` Testcontainers,
  `@ServiceConnection`, skipped automatically without Docker) — Flyway V1 success row +
  all tables/sequences exist; full lifecycle charge→bill(placeholder)→finalize with DB-checked
  snapshots and doctor-share row; EXACT outbox row (type/version/aggregate/partition key/
  producer/idempotency key + payload keys and values); settlement partial→settled→replay with the
  persisted `is_settled` flag; exactly one outbox row end-to-end.

No existing tests were deleted or disabled.

## Deviations / deferred items (explicit, none silent)

1. **BR-BILL-003 patho/rad order auto-creation** — post-saga; `specialization` (1=patho 2=rad)
   is stored and commented as a marker ONLY, per register ("DO NOT implement, comment only").
2. **BR-BILL-008 cancellation itself** (restore-from-snapshot, settled/unsettled cancel flows) —
   remains monolith-side this tranche; only the snapshot *contract* is implemented as the register states.
3. **Internal endpoint auth** — not specially permitted; default-deny holds until MIG-004
   service-to-service auth (documented in `SecurityConfig`).
4. **DoctorShare aggregation in E-07** — payload carries a single aggregated `doctorShare` block
   (register shows `doctorShare{...}|null` singular); all rows of a bill carry the one prescriber
   doctor id, so sums are used when multiple charge rows exist.
5. Ordering→billing saga wiring and Lab/Rad OrderCreated remain tranche-level deferrals (§3),
   unchanged by this work item.

## Files touched

- Fixed: `domain/model/Charge.java`, `domain/model/BillingBase.java`,
  `infrastructure/outbox/OutboxEvent.java`, `api/BillController.java`,
  `src/test/java/com/hims/billing/api/BillControllerTest.java` (request-body stub fix).
- Added: `src/test/java/com/hims/billing/application/BillingServiceTest.java`,
  `src/test/java/com/hims/billing/infrastructure/persistence/BillingServiceIntegrationTest.java`.
- Verified (unchanged): `openapi/billing-service.yaml` (all six controller paths, verbs,
  response codes; internal endpoint tagged `internal`), `db/migration/V1__init_ihms_billing.sql`.

## Test result

```
Tests run: 30, Failures: 0, Errors: 0, Skipped: 0  — BUILD SUCCESS
mvn -f services/billing-service/pom.xml test   # exit 0
```
