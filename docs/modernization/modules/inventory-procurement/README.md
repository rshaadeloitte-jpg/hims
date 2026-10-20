# Module: inventory-procurement (M13 — Inventory & Stores + M14 — Procurement)

> Master reference: `../../MODULE-INVENTORY.md` §M13 & §M14; catalog: `../../02-business-module-catalog.md` §M13/§M14; frontend: `../../03-legacy-frontend-inventory.md` §2.8; integrations: `../../07-integrations-and-search.md` §3.1.

**Target service (10 §2.1):** 10. Inventory & Procurement (single candidate context covering both modules).

**One-line purpose:** Item catalog + stock levels + demand/issue (M13); sourcing, ordering, receiving (M14).

| Aspect | Summary (evidence) |
|---|---|
| Config | `fjs-config_inventory.json` (70 keys) |
| Services | `/itemmaster`, `/itemcategory`, `/itemstock`, `/openingbalance`, `/materialconsumption`, `/expiryitem`, `/inventoryStockList` (235 endpoints), `/purchaseorder`, `/quotation`, `/eAushadiIndent` |
| Procs | CIMS_ADDITEMSTOCK, CIMS_ADDGRN, CIMS_ADDINDENT |
| Complexity | M13: 4/5 · M14: 3/5 |

**Key risks:** T_ITEMSTOCK decrement race; Pharmacy↔Inventory stock ownership (10 §7 Q1); e-Aushadhi hard-coded secrets; largest JS controller (inventory-module.js 1.08 MB, 301+ API refs).

**Dependencies:** M14↔M13 (interlock), M07 (pharmacy stock), M11/M12 (CSSD/OT issues), M04 (billing for sale items), M23 (RMRS funds via PO requests), M38 (e-Aushadhi/MDM).

**Per-module files:** features.md · workflows.md · screens.md · APIs.md · database.md · integrations.md · security.md · migration.md (business-rules.md: no BR-* IDs in 06 — state machines documented in workflows.md)
