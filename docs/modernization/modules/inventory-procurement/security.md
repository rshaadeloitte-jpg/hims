# M13/M14 — Security

- **`Inventory.AllApproval` — the ONLY live backend `checkPermission()` call site in the system** (InventoryServiceImpl:11588, 08 §3.1) — inventory approval is the one function with real server-side gating.
- Store-scoped access: `userstoreIds` (login gvar), `crr_Default_Store_id`, `selectDefaultStore` screens; T_USERSTOREDETAILS grants.
- Frontend keys (70 — largest non-master set): `Inventory.Indent.StoreIndent.RaiseNewIndent`, `Inventory.Procure.POLIST.ApprovePO`, `Inventory.Store.Add`, `Inventory.AllApproval`.
- Indent receive lock `INV_RI_<indentId>` (Redis 60s) guards concurrent GRN/receive.
- Audit: GRN/indent/PO events via MBase stamping + T_TRX_HISTORY.

## Migration notes
- Generalize the `checkPermission()` pattern to the whole surface (08 §10.1); keep store-scoped grants in IAM.
