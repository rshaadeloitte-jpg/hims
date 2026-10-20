# FINAL VERIFICATION 4 — Tranche 4 Revenue Spine (WP-10 Ordering, WP-11 Billing, WP-13 Payments)

_Verified 2026-08-23 by the prime agent (independent re-run; children billing-implementer sub-8c74173e and payments-implementer sub-5da0cf05 executed the WP-11/WP-13 work). Ground truth: outputs/implementation/GAP-REGISTER-4.md, KAFKA-EVENT-CATALOG E-06/E-07/E-08/E-12, 06-business-rules-catalog BR-BILL-001..008._

## 1. Builds (all run independently by overseer, exit codes observed directly)

| Service | Command | Result |
|---|---|---|
| ordering | `mvn -f services/ordering-service/pom.xml test` | **exit 0 — 20/20** (7 MockMvc controller + 12 service unit + 1 PG16 integration) |
| billing | `mvn -f services/billing-service/pom.xml test` | **exit 0 — 30/30** (5 MockMvc + 12 service unit + 11 calculator + 2 PG16 integration incl. exact outbox row) |
| payments | `mvn -f services/payments-service/pom.xml test` | **exit 0 — 33/33** (11 MockMvc + 9 payment unit + 7 refund unit + 6 PG16 integration over real HTTP to a contract-level billing stub) |

No tests were deleted or disabled to force green. Billing compile blockers fixed: missing `ChargeCalculation` import, `Object body.rate()` -> `BigDecimal`, `BillingBase` datetime setters, `OutboxEvent.setProducerService`. Payments compile blockers fixed: `@RestControllerAdvice` import, `PaymentsBase.setUpdatedDatetime`.

## 2. Spec/controller parity (grep-verified)

- `openapi/ordering-service.yaml` ↔ PrescriptionController: **1:1** (POST /api/v1/prescriptions, GET /api/v1/prescriptions, GET /{id}, POST /{id}/cancel).
- `openapi/billing-service.yaml` ↔ BillController: **6 ops = 6 ops**, including internal `PUT /api/v1/internal/bills/{id}/settlement` tagged `internal`.
- `openapi/payments-service.yaml` (NEW, created this tranche) ↔ controllers: **6 ops = 6 ops** (advances POST+GET, payments POST+GET, refunds POST+GET).

## 3. Event token audit (exact per GAP-REGISTER-4 / KAFKA-EVENT-CATALOG)

| Event | Type string | Schema token | Topic | Partition key | Verified in |
|---|---|---|---|---|---|
| E-06 | `PrescriptionCreated` | `prescription-created.v1` | `ihms.ordering.events` | encounterId (documented `<admissionId>` fallback when NULL) | code + tests + migration comment; emit-once asserted |
| E-07 | `InvoiceGenerated` | `invoice-generated.v1` | `ihms.billing.events` | billId | code + tests; PG16 asserts the EXACT single outbox row; finalize guard makes double-emit illegal |
| E-08 | `PaymentReceived` | `payment-received.v1` | `ihms.payment.events` | billId (`String.valueOf(payment.getBillId())`) | code + tests; outbox written in same TX AFTER successful settlement only (failure path asserted to produce NO outbox row) |
| E-12 | `RefundIssued` | `refund-issued.v1` | (per catalog §3 deferral) | billId, documented `advance:<id>` fallback for advance refunds | code + tests; **PRELIMINARY** marked in code comments, spec description, and report per catalog |

## 4. Business-rule verification

- **BR-BILL-001**: placeholder `'000000000000000'` at creation -> `OPDBILL-<seq>` / `IPDBILL-<seq>` at finalize; placeholder->final numbering test present; double-finalize guarded.
- **BR-BILL-002**: doctor share = totalAmount×pct CEILING scale 2; hospital = total−share HALF_UP; skip when pct=0 or `is_ot_procedure`; t_doctor_share row with is_billed=false. Rounding pinned by tests.
- **BR-BILL-004**: taxN = rate×pctN/100, base `(rate−rate×conPct/100)` when apply_after_concession; taxes added to SELF side.
- **BR-BILL-005 asymmetry PINNED both paths** (the register's RISK R2): SELF-company path concession plan% then bill% ADDED on selfRate; NON-SELF path bill% REPLACES plan% on nonSelfRate; explicit divergence test; deductable-0 => 100% self.
- **BR-BILL-006**: settled ⇔ both balances ≤ 0; settlement recomputes from cumulative paid => replay/idempotency tested at unit AND integration level (settle/unsettle replay).
- **BR-BILL-007**: advance FIFO in id order against balance>0, full/partial/multi-advance order tested on real PG16 rows, mode-7 split rows + t_advance_consumption recorded.
- **BR-BILL-008**: snapshot columns (total/self/nonself_before_finalization) filled at finalize.

## 5. Cross-service settlement coverage — HONEST DISCLOSURE

Payments->billing settlement was verified via a **contract-level stub** (MockRestServiceServer in unit tests; integration test serves real HTTP through an embedded stub that replicates the WP-11 Bill JSON field-by-field and asserts received paidSelf/paidNonSelf), NOT against a live billing-service instance in a Testcontainers network. Rationale: the two services are independent Maven units (no shared test-scope artifact without cross-module modification) — disclosed in WP-13-payments.md and test javadoc as permitted by the register's acceptance clause ("OR documented as covered by contract-level stub — state which"). The wire contract matches billing's actual `Bill` JSON and `PUT /internal/bills/{id}/settlement` signature.

## 6. Documented deviations (none silent)

1. **E-08 timing**: same-TX outbox AFTER synchronous settlement REST call — request-scoped saga step, not eventually-retried. Documented in PaymentService comments, spec description, WP-13 report.
2. **E-12 RefundIssued PRELIMINARY** per KAFKA-EVENT-CATALOG; restoredCharges[] empty array; type-4 restore-of-charges compensation deferred billing-side (documented).
3. **BR-BILL-003 / post-saga order creation / specialization column**: comment-only, explicitly NOT implemented this tranche per register scope decision.
4. **Advance-refund "decrements used"** interpreted as decrementing used amount by refund value — flagged in WP-13-payments.md.
5. Three pre-existing FIFO fixtures had expected allocations exceeding outstanding balances; corrected fixture balances per register semantics (disclosed in WP-13-payments.md). No assertions weakened.

## Verdict

**ALL TRANCHE-4 ACCEPTANCE CRITERIA PASS.** All three Maven builds exit 0; billing & payments compile cleanly; openapi/payments-service.yaml exists; WP-11-billing.md, WP-13-payments.md, FINAL-VERIFICATION-4.md exist; hive/board.md tranche 4 CLOSED. Cumulative program state: 12 WPs, 10 new microservices (+iam rebuild), 2 extended services, 324 tests green.
