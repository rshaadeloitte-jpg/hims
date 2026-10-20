# M13/M14 — Backend APIs (service @Path)

| Service | @Path | Notes |
|---|---|---|
| InventoryServiceImpl | `/inventoryStockList` | **235 endpoints / 12,070 LOC** (04 §9.1 god class); indents, issue, GRN, stock lists; `Inventory.AllApproval` checkPermission at :11588 (only backend check in system) |
| ItemMasterServiceImpl | `/itemmaster` | item master CRUD |
| ItemCategoryServiceImpl | `/itemcategory` | categories |
| ItemStockServiceImpl | `/itemstock` | stock ops |
| OpeningBalanceServiceImpl | `/openingbalance` | opening balance |
| MaterialConsumptionServiceImpl | `/materialconsumption` | consumption |
| ExpiryItemServiceImpl | `/expiryitem` | 47 endpoints: expiry/damage returns, getGrnNoForRetToSupplier |
| PurchaseOrderServiceImpl | `/purchaseorder` | PO lifecycle |
| QuotationServiceImpl | `/quotation` | quotations/comparison |
| InventoryEAushadiIntegrationImpl | `/eAushadhiIndent` | e-Aushadhi indent issue/QC/return; item sync |
| MdmServiceImpl | `/mdm` | MDM/Janta master sync |
| RMRSServiceImpl | `/RMRS` | PO request lists (showRMRSPurchaseOrderRequestListSchema) |

**Client controllers (03 §2.8):** inventory-module.js (1.08 MB), consignmentInventory.js, procurement.js, quotationDetails.js, expiryDamageItemReturn.js, item-category-module.js, itemEnquiry-module.js.
