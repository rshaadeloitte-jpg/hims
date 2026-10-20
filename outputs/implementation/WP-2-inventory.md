# WP-2 — item-service procurement slice (Inventory & Procurement, Catalog #10)

_Worker: inventory-implementer · Date: 2026-08-23 · GAP-REGISTER item: "WP-2 — item-service missing procurement slice (SEV Med)"_

## What was implemented

### 1. Supplier master CRUD
- `GET /api/v1/suppliers`, `GET /api/v1/suppliers/{id}`, `POST /api/v1/suppliers`, `PUT /api/v1/suppliers/{id}`.
- Backed by a new `Supplier` entity mapped onto the **existing** `ihms_item.supplier` table from
  V1__init_ihms_item.sql (see Deviation D-1). Server-owned ids via `ihms_item.supplier_seq`
  (`SupplierRepository.nextSupplierId()` native nextval — IMP-DEC-003 convention), audit stamping and
  required-field validation identical to ItemMasterService conventions.

### 2. GRN posting
- `POST /api/v1/grn`, plus `GET /api/v1/grn/{id}` and `GET /api/v1/grn/{id}/lines`.
- One transaction per GRN: header (`t_grn`) + lines (`t_grn_detail`) + inline batch creation
  (`item_batch` when a line carries `batchCode` instead of `batchId`), then every line runs through the
  **D-1 single-writer stock path** via the new `StockService.applyMovement(...)` (default REQUIRED
  propagation; `postMovement` keeps its REQUIRES_NEW contract and now delegates to it): balance row
  SELECT … FOR UPDATE (`ItemStockBalanceRepository.findByStoreItemBatchForUpdate`), ledger append
  (movementType=2 GRN/RECEIPT, operation=1 ADD, refDocType=GRN, refDocId=grnId), balance update,
  `InventoryUpdated` outbox event per line — then one aggregate outbox event of type **`GRNPosted`**
  through the existing `OutboxService`.

### 3. Indent create/approve/list
- `POST /api/v1/indents` (doc_status=1 PENDING, auto indentNo `IND-<id>` if absent),
  `POST /api/v1/indents/{id}/approve` (PENDING→APPROVED, audit stamped, emits outbox event type
  **`IndentApproved`** in the same tx; re-approve → 400 BEXA003),
  `GET /api/v1/indents?status=` (+ `/{id}`, `/{id}/lines`).
- doc_status SMALLINT dictionary: 1=PENDING 2=APPROVED 3=REJECTED 4=CANCELLED.

### 4. Flyway migration V2__procurement_tables.sql (schema ihms_item)
- Tables `t_grn`, `t_grn_detail`, `t_indent`, `t_indent_detail` mirroring V1 conventions exactly:
  shared MBase-style audit block columns, FKs to `store`/`supplier`/`item`/`item_batch`,
  unique document numbers, indexes, explicit per-table sequences (`t_grn_seq`, `t_grn_detail_seq`,
  `t_indent_seq`, `t_indent_detail_seq`) per IMP-DEC-003, CHECK constraint on doc_status,
  COMMENTs documenting the status dictionary and legacy table mapping (T_GRN/T_INDENT → clean names).

### 5. openapi/item-service.yaml
- New tag "Procurement (WP-2)"; all new paths added in the existing style (JsonNode request bodies,
  typed response schemas); new component schemas Supplier, Grn, GrnDetail, Indent, IndentDetail.

## Files added
- services/item-service/src/main/resources/db/migration/V2__procurement_tables.sql
- domain/model: Supplier.java, Grn.java, GrnDetail.java, Indent.java, IndentDetail.java
- domain/repository: SupplierRepository.java, GrnRepository.java, GrnDetailRepository.java,
  IndentRepository.java, IndentDetailRepository.java
- application: SupplierService.java, GrnService.java, IndentService.java
- api: SupplierController.java, GrnController.java, IndentController.java
- tests: api/SupplierControllerTest.java, api/GrnControllerTest.java, api/IndentControllerTest.java

## Files changed
- application/StockService.java — extracted public `applyMovement(...)` (REQUIRED propagation); public
  behavior unchanged, `postMovement` still REQUIRES_NEW and delegates to it.
- domain/model/ItemBatch.java — added missing setters (quantity/purchaseRate/mrp) in existing style.
- infrastructure test ItemServiceIntegrationTest.java — extended with 3 tests: v2ProcurementSchema_applies,
  grnPost_oneTx_ledgerBalanceBatches_andOutboxEvents, indent_createThenApprove_emitsIndentApproved_once;
  BeforeEach truncate list extended to the new tables for isolation.
- openapi/item-service.yaml — paths + tags + schemas as above.

## Test output summary
`mvn -f services/item-service/pom.xml test` → **BUILD SUCCESS, Tests run: 43, Failures: 0, Errors: 0**:
- StockControllerTest 8, StockServiceTest 5, ItemMasterControllerTest 6, ItemServiceIntegrationTest 9
  (pre-existing + 3 new, on a real postgres:16-alpine Testcontainer — Docker present in this env, so
  NOTHING was skipped or blocked; no env-dependent failures to report).
- New controller suites: SupplierControllerTest 6, GrnControllerTest 4, IndentControllerTest 5
  (MockMvc + SecurityConfig default-deny, same pattern as StockControllerTest).
- Integration verification highlights: V2 tables+sequences exist in ihms_item; GRN post creates 2 batches,
  2 balance rows (total qty 140), 2 ledger rows referencing the GRN, exactly 2 InventoryUpdated +
  1 GRNPosted outbox rows; indent approve emits exactly 1 IndentApproved row and blocks re-approval.
- The integration suite also caught (and I fixed) an entity/schema drift: t_grn initially lacked
  approved_by/approved_datetime that the Grn entity maps — proof the migration is exercised against real PG.

## Deviations from docs (with justification)
1. **No separate `t_supplier_master` table.** The register's migration bullet lists t_supplier_master,
   but V1 already ships the full supplier master as `ihms_item.supplier` (with supplier_seq, indexes).
   Creating a second supplier table would violate ONE-TABLE-ONE-OWNER (§56) and duplicate owned data;
   catalog #10's owned data lists T_SUPPLIER once. Supplier CRUD therefore targets the existing table.
   Documented in V2 header comment and Supplier javadoc.
2. **Table naming**: legacy T_GRN/T_INDENT map to clean snake_case `t_grn(_detail)`/`t_indent(_detail)`
   (kept `t_` prefix since these are migrated legacy tables; consistent with V1 renaming style such as
   M_ITEMMASTER→item). Status column named `doc_status` because `status` is reserved by the generic
   audit block (BOOLEAN active flag), matching the SMALLINT status-dictionary intent.
3. **Event types `GRNPosted` / `IndentApproved`** are proposed-but-unconsumed events per catalog #10 /
   KAFKA-EVENT-CATALOG E-09 note; they are emitted through the EXISTING OutboxService with exact type
   strings from GAP-REGISTER. They route to the master-data topic (OutboxPublisher routes only
   InventoryUpdated to stock-movement); per-line movements additionally emit standard InventoryUpdated.
4. **StockService.applyMovement** added rather than calling REQUIRES_NEW postMovement from GrnService so
   header+lines+batches+all stock mutations stay atomic in ONE transaction (a REQUIRES_NEW movement would
   commit even if a later line fails, and could not see same-tx-created batches).

## Blockers
None.
