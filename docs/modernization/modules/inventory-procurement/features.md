# M13/M14 — Features (02 §M13/§M14)

## M13 — Inventory & Stores
| Feature | Evidence |
|---|---|
| Item master & categories | itemMasterSchema, addNewItemSchema, updateItemMaster; itemcategory master |
| Stock & batches | stockListSchema, showCurrentStockListSchema, itemStockList; updateBatchExpiry |
| Indents (single/asset/direct issue) | addNewIndentSchema; showIndentDetails(Editable/OnApproved/OnFreezed/OnSecondLevelApproved/forItemIssue)Schema |
| Purchase requests & approvals | showPurchaseRequestSchema; showIndentList(onItemIssue)Schema |
| Stock adjustments | showStockAdjustmentListSchema, StoreStockAdjustment; stockAdjusmentDetails.vm |
| Item enquiry | itemEnquirySchema, getItemEnquirySchema; itemEnquiryDetails.vm |
| Expiry & damage returns | expiryItemReturnSchema, expiryItemReturnToSupplierSchema, damagedItemReturnIndent, expiredDamagedItemReturnIndent, showExpiredItemListSchema, recallExpiredItem |
| Reorder | reOrderList route, getReorderListForDirectItemIssue; reorderAndSafetyMaster |
| Store management / consumption | consumeItemforStore, showStoreComsumptionListSchema, departmentalConsumption(List); materialConsumptionDetails |
| Item issue/return | issueItem, issueItemReturn, returnItemRecieve, showAllItemIssueListSchema, directIssueItem |
| QC status | qcStatusUpdate schema |
| Stock location / hold batches | showStockLocationSchema, holdItemBatches |
| Opening balance | opeiningBalance, currentOpeningBalanceListSchema |

## M14 — Procurement
| Feature | Evidence |
|---|---|
| Purchase requests | purchaseRequest, consolidatePurchaseRequest |
| Quotations & comparison | quotation, compareQuotationSchema, getItemQuotation |
| Purchase orders (supplier/in-house/custom) | supplierPoSchema, showOrderListforInHouseSupplierSchema, customProcessOrder, showCurrentPurchaseOrderList(forOtherUnit)Schema |
| PO approval & freeze | approvePOPageSchema, freezeCurrentPOListSchema |
| GRN (receipt) & QC | showGRNSchema, showApproveGrnListSchema, qcStatusUpdate, showConsignmentGRNListSchema, multipurposeGRN |
| Rate contracts | RateContract master (+Details/Units/View) |
| Indent invoice (supplier billing) | IndentInvoice(+Details) |
| Returns to supplier | returnToSupplierSchema, showGrnReturnListSchema |
| Renegotiation | showRenegotiateSchema |
| RMRS PO requests | raiseRMRSPORequest, showRMRSPurchaseOrderRequestListSchema (M23) |

## Routes (57 inventory, 03 §2.8)
`inventory:inventory` · `currentPO` · `poRaisedForOtherUnit` · `poByHQ` · `allApproval` · `gRNList` · `indentListofStore` · `reOrderList` · `expiredDamagedItemReturnIndent` · `userStore` · `issueItem` · `consumeItemforStore` · `StoreStockAdjustment` · `holdItemBatches` · `itemReceiveList` · `returnItem` · `returnItemRecieve` · `expiredItemList` · `recallExpiredItem` · `expiryItemReturnList` · `QcStatusUpdate` · `itemSummary` · `grn` · `approveGrn` · `itemMasterList` · `itemGroupRef` · `purchaseIndent` · `consumptionList` · `stockAdjustmentList` · `quotation` · `itemEnquiry` · `opeiningBalance` · `stockLocation` · `updateBatch` · `returnToSupplier` · `expiryItemReturn(ToSupplier)` · `grnReturnList` · `issueItemReturn` · `directIssueItem` · `consignmentGRNList` · `itemStockList` · `orderList` · `receivedpolist` · `assetRegisterList` · `departmentalConsumption(List)` · `purchaseRequest` · `consolidatePurchaseRequest` · `otherUnitPOList` · `multipurposeGRN` · `updateGrn/:grnId` · `raiseDirectPO` · `stockTransfer` · `raiseRMRSPORequest` · `customOrderList` · `customProcessOrderDetails` · `stockList` · `expire`.
