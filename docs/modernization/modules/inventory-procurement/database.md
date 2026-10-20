# M13/M14 — Database (05 §3, 10 §2.1 row 10)

## Tables
| Table | Notes |
|---|---|
| M_ITEMMASTER (18 refs) | Item catalogue (shared with pharmacy) |
| T_ITEMSTOCK (15) / T_ITEMCLINIC (17) / T_ITEMBATCHMASTER | Stock, clinic mapping, batches — **concurrency hotspots** |
| T_INDENT | Indents |
| T_GRN* / T_CONSIGNMENTGRN | Goods receipt |
| T_PURCHASEORDER / T_PURCHASEORDERREQUEST | POs |
| T_QUOTATION | Quotations |
| T_SUPPLIER / T_RATECONTRACT | Suppliers & rate contracts |
| PurchaseRequest(+Details) / IndentInvoice(+Details) / TermsAndCondition | Procurement transactionals |
| StoreItemCategory / ItemSupplier / ItemClinic | Store-scoped mappings |
| masters/inventory (~30) | itemmaster, itemcategory, itemgroup, brandmaster, molecule, therapeuticclass, unitofmeasure, store, storeagetype, supplier(+master), taxmaster, rateContractSchema, reorderAndSafetyMaster, salemarkupmaster, grnitemchecklist, grnrejectreasonmaster, termsofpayment, termsAndCondition, discardrejectionmaster, empanelmaster, gastypemaster, genericbrandmapping, itemcompany, itemSupplierRate, mappedProductItems, preganancyclass, unMappedProductItems |

## Stored procedures (CIMS_*)
- `CIMS_ADDITEMSTOCK`, `CIMS_ADDGRN`, `CIMS_ADDINDENT` (live-called from InventoryJobSchedule / BillingCommonUtilServiceImpl — 05 §6).

## Sequences
- `t_reorderlevelsummery_seq`, `auto_code_seq` (05 §4).

## Migration notes (10 §2.1 row 10)
- Target owns M_ITEMMASTER, T_ITEMSTOCK, T_ITEMBATCHMASTER, T_PURCHASEORDER, T_GRN*, T_INDENT, T_SUPPLIER, T_RATECONTRACT, T_QUOTATION, T_CONSIGNMENTGRN.
- Emits `InventoryUpdated`, `StockAdjusted`, `PurchaseOrderPlaced`, `GRNPosted`; consumes `DispensingCompleted`, `MaterialConsumed` (OT/CSSD), auto-indent triggers.
- Concurrency: stock ops must retain row-lock semantics (10 §3.4).
