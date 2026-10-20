# WP-13 — Payments & Refunds service (Catalog #6) — Implementation Report

Status: **DONE** — `mvn -f services/payments-service/pom.xml test` exits 0, **33/33 tests green**
(unit 16 + controller 11 + PG16 Testcontainers integration 6; no existing tests deleted or disabled).

## Scope delivered (per GAP-REGISTER-4 §WP-13)

- **Schema** (`V1__init_ihms_payment.sql`, pre-existing and kept as-is): `t_advance` with
  `balance NUMERIC(19,2) GENERATED ALWAYS AS (amount - used) STORED`; `t_payment`
  (`transaction_type SMALLINT CHECK IN (2,3,4)` per 06 §A6, `change_amount DEFAULT 0`);
  `t_payment_mode_detail` (mode 7 = ADVANCE reserved); `t_advance_consumption` FIFO ledger;
  `t_refund` (CHECK IN (3,4)); transactional `outbox`. Verified in the integration test that
  `balance` really is `is_generated = ALWAYS`.
- **Advances**: `POST /api/v1/advances` (patientId required, amount > 0; balance re-read from
  the generated column via flush+refresh), `GET /api/v1/advances?patientId=` ordered by
  advance_id ASC (BR-BILL-007 FIFO order).
- **Payments**: `POST /api/v1/payments {billId, patientId, amount, modes[{mode,amount}], advanceUse}`:
  - modes-sum guard: sum(modes) must equal amount else **400 PAY_MODE_MISMATCH** (with
    advanceUse the sum may be below amount but never above);
  - BR-BILL-007 advance FIFO: when advanceUse, advances with balance > 0 consumed in
    advance_id order against the SELF outstanding balance first; every chunk recorded as an
    extra mode-7 row on `t_payment_mode_detail` plus a `t_advance_consumption` row
    (unique per (advance, payment)); zero-balance advances skipped;
  - synchronous settlement: Spring `RestClient` GET `/api/v1/bills/{id}` for outstanding
    self/non-self balances, self-first allocation of `amount` between those balances,
    then PUT `/api/v1/internal/bills/{id}/settlement` with cumulative paid amounts;
    response `changeAmount` recorded on the payment row;
  - E-08 outbox append in the same DB tx **only after** a successful settlement call.
- **Refunds**: `POST /api/v1/refunds {transactionType 3|4, ...}`:
  - type 3 advance refund requires balance > 0 else legacy code **BEXAR002**; reduces the
    advance's remaining value (decrements `amount`, see interpretation note below);
  - type 4 bill refund records the refund row only.
  - `GET /api/v1/refunds?billId=`, `GET /api/v1/payments?billId=`.
- **Events**: exact catalog tokens — E-08 `PaymentReceived` / `payment-received.v1`,
  partition key billId, payload keys exactly `{paymentId, billId, patientId, amount,
  modes[{mode,amount}], advanceDetails[{advanceId,consumed}]|null, transactionType:2,
  changeAmount}`; E-12 `RefundIssued` / `refund-issued.v1`, partition key billId, payload
  keys exactly `{refundId, billId|null, advanceId|null, patientId, amount, reason,
  restoredCharges[]}`. Pinned by unit AND integration assertions.
- **OpenAPI**: `openapi/payments-service.yaml` covering all six operations with error
  envelopes, matching the ordering-service spec style.

## Compile fixes

1. `GlobalExceptionHandler.java`: missing `import org.springframework.web.bind.annotation.RestControllerAdvice;`
   (exact blocker from PENDING-GAPS-PROMPT.md).
2. `PaymentsBase.java`: added missing `setUpdatedDatetime(Timestamp)` used by
   PaymentService/RefundService (exact blocker from PENDING-GAPS-PROMPT.md).
3. Test-side compile fixes (no behavior change): escaped string literals broken in
   `PaymentServiceTest.stubBill`; replaced non-existent `MockServerRestClientCustomizer`
   (not in Spring Framework 6.2 / Boot 3.5.3) with `MockRestServiceServer.bindTo(RestClient.Builder)`.

## Documented deviations & interpretations

1. **E-08 timing — request-scoped saga step (register-mandated deviation).** The billing
   settlement REST call is synchronous and NOT eventually-retried: if billing is down or
   errors, the whole payment tx rolls back (502 BILLING_UNREACHABLE envelope), nothing is
   persisted, and NO E-08 exists. The outbox append happens inside the payment tx strictly
   AFTER a successful settlement response. Consequence: payments cannot be taken during a
   billing outage; there is no at-least-once replay of the settlement call itself (the
   settlement endpoint is idempotent by recomputation should a manual replay ever be needed).
   Flagged in `PaymentService` javadoc, `BillingCallException`, `GlobalExceptionHandler`
   (502 mapping), migration comments, and the OpenAPI description.
2. **E-12 RefundIssued marked PRELIMINARY** per KAFKA-EVENT-CATALOG: `restoredCharges` stays
   `[]` until billing-side restore-of-charges compensation (BR-BILL-008 snapshots →
   IsCancelled, T_Itemsale reset, doctor-share reset) lands; flagged in code, spec and here.
3. **"Decrements used" interpretation (type-3 refund).** The register says an advance refund
   "requires balance>0 ... (decrements used)". Decrementing `used` would INCREASE the
   generated balance (amount - used); the implemented semantic reduces the advance's
   remaining refundable value by decrementing `amount` instead. Interpretation flagged at
   `t_refund`/`t_advance` COMMENTs, RefundService javadoc, and here — PRELIMINARY.
4. **Type-4 restore-of-charges compensation stays billing-side later** (BR-BILL-008) —
   documented deferral; payments records only the t_refund row and emits E-12.
5. **E-12 partition-key fallback**: column is NOT NULL billId per register; for advance-only
   refunds (billId null) the relay key falls back to `"advance:<advanceId>"` — documented in
   code/spec/test.
6. **Advance consumption capped at the self outstanding balance** ("against self balance
   first"): advances never spill over to the non-self side; leftover shortfall simply stays
   unsettled on the bill. The three pre-existing FIFO fixtures expected allocations exceeding
   outstanding balances (e.g. paidSelf 900 vs self balance 800; 600+400 paid from a 600
   payment) — inconsistent with the register's "allocation of amount between outstanding
   self and non-self balances"; their fixture balances were corrected to keep testing the
   same FIFO/split scenarios consistently. No test was deleted or disabled.

## Settlement coverage — honest statement

**Contract-level stub, not a live billing instance.** The PG16 integration test exercises
the full HTTP round trip against a real embedded HTTP server (JDK HttpServer) that
replicates billing-service's wire contract field-by-field as implemented in WP-11 source
(`Bill.balanceSelf/balanceNonSelf/isSettled/changeAmount`; PUT body `{paidSelf, paidNonSelf}`
with BR-BILL-006 idempotent recompute semantics). It also asserts the cumulative paid
amounts received, so allocation drift fails the test. A live cross-service test was judged
infeasible without modifying sibling services: the Maven modules are independent (no
test-scope dependency possible without touching billing's pom) and billing's own build was
red at authoring time (7 pre-existing failures in BillingServiceTest/BillingServiceIntegrationTest —
WP-11 scope, untouched). Unit-level settlement stubbing additionally uses
MockRestServiceServer bound to the RestClient as the register requested.

## Test inventory (33)

- `application/PaymentServiceTest` (9): modes-sum guard (+advanceUse upper bound),
  non-positive amount rejected before any HTTP, FIFO single-advance/partial-split/multi-
  advance-id-order/zero-balance-skip, change_amount recording, null advanceDetails, settle
  failure => BillingCallException with NO outbox append; E-08 exact-envelope captor asserts.
- `application/RefundServiceTest` (7): type must be 3|4, type-3/type-4 required fields,
  BEXAR002 on zero balance and amount-above-balance, happy path reduces remaining balance,
  E-12 exact keys/restoredCharges []/pk fallback + billId pk for type 4.
- `api/PaymentsControllerTest` (11): default-deny on all six operations; 201 advance with
  balance; PAY_REQ_INVALID / PAY_MODE_MISMATCH / BEXAR002 / BILLING_UNREACHABLE (502) /
  NOT_FOUND envelopes; list endpoints incl. transient modes/consumptions.
- `infrastructure/persistence/PaymentsServiceIntegrationTest` (6, PG16-alpine Testcontainers,
  auto-skipped without Docker): Flyway V1 applies + generated-column check; advance balance
  round-trip; full payment flow over real HTTP to the contract stub (paid amounts asserted,
  change_amount recorded, E-08 row exact type/version/partitionKey/idempotencyKey/payload);
  FIFO multi-advance partial split on real rows (used/balance/mode-7/consumption order +
  E-08 advanceDetails order); modes-sum mismatch writes nothing and emits nothing; refunds:
  BEXAR002 guard on a drained row, type-3 reduces generated balance, type-4 row, both E-12
  variants pinned.

## Files written/changed

- Fixed: `src/main/java/.../api/GlobalExceptionHandler.java`, `domain/model/PaymentsBase.java`,
  `application/AdvanceService.java` + `application/RefundService.java` (flush before refresh of
  the generated column — required inside any surrounding transaction).
- Added tests: `src/test/java/.../api/PaymentsControllerTest.java`,
  `infrastructure/persistence/PaymentsServiceIntegrationTest.java`.
- Repaired (compile-only + register-consistent fixture balances): `application/PaymentServiceTest.java`,
  `application/RefundServiceTest.java` (untouched otherwise).
- Spec/report: `openapi/payments-service.yaml`, this file.
