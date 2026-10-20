# M13/M14 — Migration

**Target service:** 10. Inventory & Procurement — **Wave 4 (Facilities)** (10 §6; may move to Wave 2/3 if Pharmacy split forces).

**Complexity:** M13 4/5 (stock concurrency, e-Aushadhi coupling, god-class 12,070 LOC) · M14 3/5 (state machines, RMRS coupling).

**Steps:**
1. **Resolve Pharmacy/Inventory stock boundary first (10 §7 Q1):** single stock service (Inventory owns stock) recommended; Pharmacy consumes `/stock` API + InventoryUpdated events; eliminate dual-write.
2. **Concurrency:** T_ITEMSTOCK decrement with row-level locks identical to legacy procs (10 §3.4); indent receive lock (INV_RI_) keeps guarding.
3. **God-class decomposition:** InventoryServiceImpl (235 endpoints) → item/stock/indent/issue sub-services.
4. **Procs:** CIMS_ADDITEMSTOCK / CIMS_ADDGRN / CIMS_ADDINDENT → PL/pgSQL on extraction path.
5. **Events:** InventoryUpdated, StockAdjusted, PurchaseOrderPlaced, GRNPosted; consume DispensingCompleted (M07), MaterialConsumed (OT/CSSD); auto-indent triggers.
6. **e-Aushadhi:** move hard-coded Basic-auth to vault (07 §3.3); keep idempotent ack DTOs + retrigger hook.
7. **State machines:** indent (draft/freezed/approved/issued) + PO/GRN/QC states → SMALLINT + CHECK enum maps (10 §3.5).
8. **Solr item index:** move drainer to event pipeline; keep SOLR_STATUS at-least-once semantics.

**Exit criteria (Wave 4):** stock API contract live (Pharmacy uses it); InventoryUpdated stream live.
