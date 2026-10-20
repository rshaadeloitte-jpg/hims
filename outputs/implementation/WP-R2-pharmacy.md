# WP-R2 — Pharmacy & Dispensing service (Catalog #9) — IMPLEMENTATION REPORT

_GAP-REGISTER-5 Wave-5 tranche 5. Ground truth: MICROSERVICE-CATALOG #9, 06-business-rules-catalog BR-PHAR-001/002, KAFKA-EVENT-CATALOG §3 deferrals. Reference conventions: services/ordering-service._

## Result

- `mvn -f services/pharmacy-service/pom.xml test` → **BUILD SUCCESS (exit 0), 36 tests, 0 failures, 0 errors, 0 skipped**.
- `openapi/pharmacy-service.yaml` created — **8 operations matching the 4 controllers 1:1**:
  | Controller | Operation | Path |
  |---|---|---|
  | ItemSaleController | POST create | `/api/v1/item-sales` |
  | ItemSaleController | GET list (`?status=&patientId=`) | `/api/v1/item-sales` |
  | ItemSaleController | GET by id | `/api/v1/item-sales/{id}` |
  | ItemSaleController | POST dispense (partial allowed) | `/api/v1/item-sales/{id}/dispense` |
  | InterventionController | POST record | `/api/v1/interventions` |
  | InterventionController | GET list per patient | `/api/v1/interventions?patientId=` |
  | PicklistController | POST pick list | `/api/v1/picklists` |
  | PrescriptionProjectionController | PUT EMR number | `/api/v1/prescriptions/{id}/emr-no` |

## What was implemented

- Spring Boot 3.5.x service, package `com.hims.pharmacy`, port **8101**, schema **ihms_pharmacy**, Flyway `V1__init_ihms_pharmacy.sql` (t_itemsale + t_item_sale_detail, t_prescription_dispense + t_prescription_dispense_detail, t_drug_intervention, t_ward_picklist, transactional outbox, sequences; audit cols added_datetime/updated_datetime).
- Status machine on t_itemsale.status_code SMALLINT: 1=PENDING → 2=PARTIALLY_DISPENSED → 3=FULLY_DISPENSED (terminal; further dispense → 400 PH_ILLEGAL_TRANSITION).
- Default-deny security config (+ dev-profile HTTP Basic replacement), error envelope + BEXA-style codes via GlobalExceptionHandler (BEXA003 validation, NOT_FOUND 404, STOCK_SERVICE_UNAVAILABLE 502), CorrelationIdFilter.
- **STOCK STAYS IN item-service** (single-writer D-1): every dispensed qty synchronously POSTs a SUBTRACT movement to item-service's `/api/v1/stock/movements` (ItemStockClient on RestClient). On ANY failure: StockIssueException → whole tx rolls back → sale stays PENDING, nothing marked dispensed (pinned by unit AND integration tests). The retired legacy dual-write hotspot CIMS_ADDITEMSTOCK (PharmacyServiceImpl:2140) is NOT revived — comment-only.
- No spring-kafka dependency (documented deviation in pom): nothing is emitted this tranche; outbox table is provisioned and asserted EMPTY.

## BR-PHAR-001 coverage (pinned by tests)

| Rule | Where pinned |
|---|---|
| Each detail dispensed=true when its pending_qty reaches 0 | `ItemSaleServiceTest.brPhar001_fullDispense_singleLine_closesSale`, `brPhar001_partialDispense…`, PG16 `brPhar001_and_brPhar002_fullFlow…` |
| Partial dispense keeps PARTIALLY_DISPENSED + decrements pending_qty (+is_partial_dispense=true) | unit partial test; PG16 full flow (partial then boundary close) |
| ALL details dispensed → header dispensed=true + status FULLY_DISPENSED (flips exactly at last line) | `brPhar001_boundary_lastLineClosing_flipsHeaderExactlyOnce`; PG16 full flow |
| Guards: empty/unknown lineNo/qty>pending/qty≤0 → 400 BEXA003; FULLY_DISPENSED terminal → 400 PH_ILLEGAL_TRANSITION | `dispense_unknownLine_orEmptyRequest_orQtyBeyondPending_rejected`, `dispense_fullyDispensedSale_rejected`; DB CHECKs ck_t_item_sale_detail_qty/pending |

## BR-PHAR-002 coverage (pinned by tests)

| Rule | Where pinned |
|---|---|
| Full-qty sale line closes projection detail (IsClosed='1', PendingQty=0) | `ItemSaleServiceTest.brPhar002_fullQtyLine_closesProjectionDetail_pendingZero`; PG16 flow asserts real rows |
| Partial → PendingQty -= qty, detail stays open | `brPhar002_partialLine_decrementsProjectionPending_staysOpen`; PG16 flow |
| Master IsClosed='1' exactly when ALL details closed | PG16 full flow (open after first close; flips after last) |
| Sales without prescriptionId never touch the projection | `brPhar002_salesWithoutPrescription_neverTouchProjection` |
| Projection seeded from each prescription-linked sale's lines | PG16 flow (master+details exist right after create); `create_withPrescription_seedsLocalProjection` |

## Stock-borrow stub disclosure (like WP-13)

The pharmacy→item-service call is tested against a **contract-level stub, not a live item-service**:

- Unit level: MockRestServiceServer bound to the RestClient — pins exact wire shape `{storeId, itemId, batchId?, movementType:4(SALE), operation:2(SUBTRACT), quantity, refDocType:"PHARMACY_SALE", refDocId:saleId, remarks}` plus failure path (500/error-envelope → StockIssueException, nothing persisted).
- PG16 integration level: a real-HTTP stub (JDK HttpServer on an ephemeral port, injected via `app.item-service.base-url`) replicating item-service's wire shape field-by-field including its rejection envelope. Wire shape of actual calls asserted from captured request bodies.

This mirrors the register's "contract-level stub w/ MockRestServiceServer + integration HTTP stub replicating item-service wire shape — disclose like WP-13".

## Events policy (negative assertions)

- **DispensingCompleted is PROPOSED** (no multi-consumer evidence — not in KAFKA-EVENT-CATALOG): NOTHING is emitted to the outbox anywhere in the code path. The PG16 integration test asserts `outboxRepository.count()` stays **0** after creation, every dispense outcome, failures, interventions, picklists and EMR stamping — any future accidental emission fails CI. Outbox table itself is provisioned per sibling convention.
- Consuming PrescriptionCreated (E-06) / InventoryUpdated post-saga: comment-only (projection currently seeded from sales that carry a prescriptionId).

## Deviations / deferred (flagged, none silent)

1. **Prescription truth stays in ordering-service** (E-06 payload owner). The pharmacy-local T_PRESCRIPTIONDISPENSE projection is a read-side tracking copy; cross-service write-back of IsClosed/PendingQty into ihms_clinical is DEFERRED post-saga (register: "comment + report"). Automatic seeding from E-06 consumption likewise post-saga.
2. **Stock decrement via REST stubs** as disclosed above — no live item-service in CI.
3. **No events emitted this tranche** (PROPOSED event) — hence no spring-kafka dependency, unlike ordering/lab siblings.
4. `visit_ref` column provisioned for legacy parity but not carried by the POST /api/v1/item-sales contract (stays NULL); `is_verified` provisioned, no API sets it yet (verification flow = later slice).

## Repair notes (this session)

- Fixed main compile error: missing `import …domain.repository.ItemSaleRepository` in `WardPicklistService.java` (lines 30/36).
- Fixed two broken L0 test stubs in `ItemSaleServiceTest` (service behavior was correct): expected quantity is the DISPENSED qty (not pending qty), and both sequential MockRestServiceServer expectations are now queued before the first request (Spring forbids adding expectations after actual requests).
- Added the previously absent register-mandated test layers: `ItemSaleControllerTest` (8), `SupportingControllersTest` (8), `PharmacyServiceIntegrationTest` (4, PG16 Testcontainers + real-HTTP stock stub + negative outbox assertions).

## Test table (36 total, mvn exit 0)

| Test class | Layer | Count | Covers |
|---|---|---|---|
| application.ItemSaleServiceTest | L0 unit | 16 | creation guards, BR-PHAR-001 full/partial/boundary, BR-PHAR-002 delegation, lifecycle guards, stock happy/failure wire shape, batch-ref forwarding, 404 mapping |
| api.ItemSaleControllerTest | MockMvc (default-deny) | 8 | unauthorized default-deny, 201 create, 400 BEXA003 envelope, named/numeric status filter, dispense 200, PH_ILLEGAL_TRANSITION 400, 404, 502 STOCK_SERVICE_UNAVAILABLE |
| api.SupportingControllersTest | MockMvc (default-deny) | 8 | default-deny on interventions/picklists/emr-no, intervention 201/400/list, picklist 201/400/404 + server-stamped picked_at, EMR stamping 200/404 |
| infrastructure.persistence.PharmacyServiceIntegrationTest | PG16 Testcontainers | 4 | Flyway V1 applies (all tables), BR-PHAR-001+002 full flow on real rows incl. wire-shape capture and terminal guard, stock-failure rollback, supporting flows (interventions/picklists/EMR) with guards; outbox EMPTY negative assertion throughout |

Requires Docker (auto-skipped when absent — `@Testcontainers(disabledWithoutDocker = true)`), same as ordering-service reference build.
