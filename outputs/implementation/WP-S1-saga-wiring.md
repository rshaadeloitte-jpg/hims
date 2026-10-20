# WP-S1 — Order→Billing Saga Wiring (Catalog #4 ↔ #5)

_Closed by repair worker after the first implementer died mid-WP (ordering ~80% done + red, billing 0%). Ground truth: GAP-REGISTER-6 conventions; MICROSERVICE-CATALOG #4/#5; KAFKA-EVENT-CATALOG E-13 (added) + §3 (RESOLVED annotation appended). All acceptance commands re-run green at close._

## 0. Event contract — E-13 OrderCreated

| Field | Value |
|---|---|
| Type string | `OrderCreated` (`OutboxPublisher.OrderingEvents.ORDER_CREATED`) — EXACT, pinned by L0 + L2 |
| Schema token | `order-created.v1` (`OutboxPublisher.schemaOf`) |
| Topic | `ihms.ordering.events` (`app.kafka.topic.ordering-events`) |
| Version | `v1` |
| Aggregate | type `order`, id = t_order.order_id |
| Producer service | `ordering-service`; idempotency key `order-created:<orderId>` |
| Partition key | `encounterId` with DOCUMENTED FALLBACKS: `<admissionId>` when encounterId is NULL (same pattern as E-06), `no-context:<orderId>` when both are NULL (partition_key column is NOT NULL). Pinned by `OrderServiceTest.create_partitionKeyFallback_admissionThenNoContext`. |
| Payload keys | EXACTLY `{orderId, patientId, encounterId|null, admissionId|null, lines[{itemId, serviceName, rate NUMERIC(19,2), qty int>0}], prescriberId, wardId|null}` — unsupplied context slots travel as explicit null; ids serialized as strings per sibling convention; pinned key-set assertions in both services' tests |
| Emission | CREATE ONLY, ONE row, SAME tx as t_order/t_order_line (transactional outbox `ihms_clinical.outbox`). Cancel emits NOTHING; compensation emits NOTHING. |

Consumer side (billing) validates the same line semantics producer-side guards enforce
(patientId required; ≥1 line; per-line itemId/serviceName/rate ≥ 0/qty > 0; encounter XOR
admission) so a rejected draft is always a genuine contract breach, not a transport artifact.

## 1. Ordering side (finished predecessor's work)

New files (already present, repaired): `Order`, `OrderLine(+Key)` entities;
`V2__order_saga.sql` (t_order / t_order_line / sequences; ck_t_order_ctx enforces the XOR
at DB level); `OrderController` (/api/v1/orders CRUD-ish slice); `OrderService`;
`InternalOrderController` (**moved** the compensation endpoint here — see deviations);
tests `OrderControllerTest`, `OrderServiceTest`, `saga/OrderSagaIntegrationTest`.

Repairs made:
1. **OrderServiceTest (5 errors)**: `create()` returns `get()` on the GENERATED id but the mock repo only stubbed `save()`. Fixed the TEST mocks (production logic is sound): an `AtomicReference<Order>` tracks what `save()` was handed and `findById(any)` serves it.
2. **OrderSagaIntegrationTest.negativeAssertions**: rewrote to filter `eventType='OrderCreated'` rows — E-06 PrescriptionCreated legitimately shares `ihms_clinical.outbox`, and Mockito captor-index drift in the partition-fallback test fixed with fresh captors.
3. **compensation_billingFailed (expected '4' was '1')**: production logic was correct — JdbcTemplate bypasses Hibernate auto-flush inside the test tx. Test now injects an `EntityManager` and calls `em.flush()` before the native DB reads.
4. **OrderControllerTest.billingFailed (404)**: test posted `/api/v1/internal/orders/{id}/billing-failed` but the mapping lived under `/api/v1/orders/{id}/billing-failed`. Resolved by moving the endpoint to a dedicated `InternalOrderController` mapped at the INTERNAL path (matches the javadoc, the default-deny test, and the interop internal-ingress convention); `@WebMvcTest` now loads both controllers.

## 2. Billing side (built from zero)

- **V2__billing_saga.sql** (ihms_billing): draft columns `t_bill.saga_source_order_id`,
  `t_bill.saga_event_id` + partial UNIQUE index `uq_t_bill_saga_event` (race backstop #2);
  ledger `t_consumed_event` (consumed_id PK seq `t_consumed_event_seq`, **event_id VARCHAR(64)
  UNIQUE** = race backstop #1, event_type, source_order_id, status CHECK IN ('APPLIED','REJECTED'),
  rejection_reason VARCHAR(500), ingested_at, audit cols) mirroring reporting-service
  `t_ingested_event`.
- **ConsumedEvent entity + repository**, `Bill` gains the two saga columns.
- **BillingSagaIngressService** (`POST /api/v1/internal/events`, tagged internal): consumes ONLY
  `OrderCreated` (unknown → 400 `BILL_UNKNOWN_EVENT_TYPE`, BEFORE any ledger write). Idempotency
  layers: (a) fast-path lookup → DUPLICATE/rejected-replay; (b) atomic claim via native
  `INSERT … ON CONFLICT (event_id) DO NOTHING` (DB-constraint-safe, no rollback-only poisoning);
  (c) partial UNIQUE on the bill. First delivery → bill DRAFT (placeholder billNo BR-BILL-001,
  interim=true) + one t_charge per line, 201 APPLIED. Duplicate replay → no-op 200 DUPLICATE.
  Validation failure → rejection RECORDED (ledger row flipped to REJECTED + reason, explicitly
  flushed) and structured failure `{status:"REJECTED", errorCode:"BILL_REQ_INVALID", message,
  eventId, sourceOrderId}` returned as **422** which the relay maps to
  `POST /api/v1/internal/orders/{id}/billing-failed` on ordering-service; replay of a rejected
  eventId replays the identical structured failure. The ingress EMITS NOTHING to ihms_billing.outbox
  (E-07 stays exclusively on finalize; negative outbox assertion pinned).
- **Tests** `infrastructure/saga/BillingSagaIntegrationTest.java`: PG16 Testcontainers, fed the
  EXACT `OutboxPublisher.publish()` envelope shape (see disclosure below).

## 3. Transport-stub disclosure

The Kafka consumer of `ihms.ordering.events` does NOT exist. Production is a Kafka listener in
billing-service; the implemented boundary is the INTERNAL REST ingress
`POST /api/v1/internal/events`, contract-stubbed per GAP-REGISTER-6 convention (identical to
interop/reporting/search tranches). Tests deliver envelopes byte-equivalent to
`OutboxPublisher.publish()` output ({eventId, eventType, version, occurredAt, producerService,
entityId, partitionKey, idempotencyKey, schemaName, payload}) built from a REAL outbox row in the
ordering saga test's relay-helper. Ingress logic itself is transport-agnostic. Default-deny holds
on every internal endpoint (401 pinned in both services).

## 4. Compensation flow

```
ordering.create() ── same tx ──► t_order/t_order_line + outbox(OrderCreated, order-created.v1)
        │                                   │ relay (Kafka in prod)
        ▼                                   ▼
PLACED(1)                          billing POST /api/v1/internal/events
        │                                   ├─ valid   ─► bill DRAFT + charges (idempotent per eventId) ── finalize later (E-07, unchanged)
        │                                   └─ invalid ─► 422 {status:REJECTED,…} recorded on t_consumed_event
        │                                                │ relay maps failure
        └────────────────◄── POST /api/v1/internal/orders/{id}/billing-failed {rejectionReason}
                                            BILLING_FAILED(4) + reason persisted; idempotent replay refreshes reason;
                                            ORD_ILLEGAL_TRANSITION from any other state; terminal this tranche
```
2=BILLED is reserved for the (future) bill-confirmation event. No retry storm: compensation is
relay-invoked once per rejection, redeliveries are idempotent no-ops.

## 5. Test table

| Suite | Tests | Covers |
|---|---|---|
| ordering api.OrderControllerTest | 7 | default-deny incl. internal compensation endpoint; create 201 PLACED; GET exposes statusCode=4 + rejectionReason; internal compensation 200; cancel-from-BILLING_FAILED 400 envelope; unknown order 404; named status filters incl. BILLING_FAILED |
| ordering application.OrderServiceTest | 10 | all creation guards BEXA003; E-13 exact contract (type/agg/pk/idempotencyKey/payload key-sets); partition-key fallback chain admission→no-context; cancel guard + emits nothing; markBillingFailed persists reason + idempotent replay; blank reason BEXA003 |
| ordering prescription suites | 19 | untouched predecessor behavior (regression guard) |
| ordering persistence.OrderingServiceIntegrationTest | 1 | V1 Flyway regression |
| ordering saga.OrderSagaIntegrationTest | 4 | V2 applies; real PLACED rows + EXACTLY ONE E-13 outbox row w/ full envelope; DB-level compensation persistence (status_code=4, rejection_reason); negative assertions — non-create flows add ZERO OrderCreated rows |
| billing api.BillControllerTest | 5 | untouched predecessor behavior |
| billing application.BillingCalculatorTest / BillingServiceTest | 11 / 12 | untouched predecessor behavior |
| billing persistence.BillingServiceIntegrationTest | 2 | V1 Flyway + lifecycle regression |
| billing saga.BillingSagaIntegrationTest | 4 | (1) one delivery → real draft rows asserted via SQL (bill_no placeholder, interim, saga cols) + 2 charges + ledger APPLIED + ZERO outbox rows; (2) redelivery same eventId → 200 DUPLICATE, single bill/charge-set/ledger row at DB level, both UNIQUE backstops verified present; (3) invalid payload → 422 REJECTED structured body, zero bill/charge rows, REJECTED ledger row, replay stays rejected; (4) unknown eventType 400 before any write + default-deny 401 |

**mvn results:** `services/ordering-service/pom.xml` → exit 0, **41 tests, 0 failures, 0 errors**
(20 original + 21 new, zero regressions). `services/billing-service/pom.xml` → exit 0, **34
tests, 0 failures, 0 errors** (30 original + 4 new saga tests).

## 6. Spec & catalog parity

- `openapi/ordering-service.yaml`: ALL five new endpoints added 1:1 (`POST/GET /api/v1/orders`,
  `GET /api/v1/orders/{id}`, `POST /api/v1/orders/{id}/cancel`, `POST
  /api/v1/internal/orders/{id}/billing-failed` tagged internal) with CreateOrderRequest /
  OrderLineInput / BillingFailedRequest / Order / OrderLine schemas; info.description updated
  (orders were entirely absent before).
- `openapi/billing-service.yaml`: `POST /api/v1/internal/events` added tagged internal with
  ConsumeEventRequest / IngestResult schemas and 201/200/400/422 responses; info.description +
  internal tag description updated for the saga consumer role.
- `docs/modernization/KAFKA-EVENT-CATALOG.md`: **E-13 entry ADDED to §2** (the overseer brief said
  it existed — it did NOT survive the predecessor's death; grep found only the §3 "candidate E-13"
  audit note) matching the final implementation (type/token/topic/pk + fallbacks/payload keys);
  §3 mega-txn verdict row got its RESOLVED annotation APPENDED (original text preserved) scoped to
  the ordering→billing hop; §6 note updated since E-13 is no longer free for the audit stream.

## 7. Deviations & disclosures

1. **Compensation endpoint moved** to `POST /api/v1/internal/orders/{id}/billing-failed` (new
   `InternalOrderController`) — the half-done code had the mapping under `/api/v1/orders/{id}/billing-failed`
   while its own javadoc + controller test targeted the internal path. Internal path chosen
   (interop ingress convention; keeps the public orders surface saga-free).
2. **Transport stub** (§3 above): Kafka consumer modeled as internal REST ingress fed the real
   outbox envelope in tests. In-flight continuity/cutover remains a deployment concern.
3. **Rejection HTTP status = 422** (structured `{status, errorCode, message}` body), distinct from
   envelope-level 400s; the register said "structured failure response" without pinning a status.
4. **t_consumed_event ledger table added** (not in any WP table list) — required for strict
   double-delivery protection, mirrors the disclosed reporting-service `t_ingested_event`
   precedent; flagged here and in migration comments.
5. **DRAFT == interim** (`is_interim=TRUE`, placeholder billNo): no new bill-state column invented;
   finalize/settlement semantics untouched.
6. Saga tests require Docker (`@Testcontainers(disabledWithoutDocker = true)`); Docker WAS present
   locally and all four saga integration tests RAN GREEN in the final verification runs.
