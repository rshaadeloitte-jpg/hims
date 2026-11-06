# Blood Bank and RMRS API Mismatch Analysis

**Document Version:** 1.0.0  
**Date:** 2026-11-06  
**Scope:** Frontend API contracts (`frontend/src/features/bloodbank/queries.ts`, `frontend/src/features/rmrs/queries.ts`, `RmrsPage.tsx`, `BankReconciliation.tsx`, `PORequest.tsx`) vs Backend microservices (`services/clinical-support-service` on port 8119, `services/admin-service` on port 8120), legacy references in `/Users/adityabanerjee/personal_project/ihms/src`, and gateway routing in `platform/gateway/nginx.conf`.

---

## 1. Executive Summary

A comprehensive investigation was conducted into the 404 errors observed when loading the Blood Bank and RMRS frontend screens. The root causes are two-fold:
1. **URL & Gateway Disconnect:** The frontend calls paths such as `/api/v1/blood-bags`, `/api/v1/blood/donors`, `/api/v1/blood/cross-matches`, `/api/v1/blood/indents`, `/api/v1/journals`, `/api/v1/bank/...`, and `/api/v1/po/...`. Several of these routes either lack upstream proxy declarations in `platform/gateway/nginx.conf` or target backend controller path hierarchies (`/api/v1/rmrs/...`, `/api/v1/bloodbank/...`, `/api/v1/bags`, `/api/v1/donors`) that do not match the frontend route paths.
2. **Schema & Contract Divergence:** More critically, none of the existing backend domain endpoints match the schema contract expected by the modern React frontend. Backend entities represent normalized domain models (e.g. `BloodBag`, `BloodDonor`, `JournalVoucher`, `PoRequest`) serialized without frontend UI convenience fields (e.g. `bagNo`, `component`, `status` strings, nested line items, aggregate balances).

### Verdict Counts

| Domain | ALIAS-SAFE | NEEDS-ADAPTER | MISSING | Total Calls |
|:---|:---:|:---:|:---:|:---:|
| **Blood Bank** (`clinical-support-service:8119`) | 0 | 12 | 6 | 18 |
| **RMRS** (`admin-service:8120`) | 0 | 13 | 2 | 15 |
| **TOTAL** | **0** | **25** | **8** | **33** |

*Note on ALIAS-SAFE criteria:* Per architectural guidelines, an endpoint is only `ALIAS-SAFE` if the backend endpoint returns the exact same fields that the frontend reads without data loss or UI breakage. Merely aliasing URL paths would cause runtime frontend rendering failures due to missing mandatory properties (e.g., `bagNo`, `component`, `status`, `lines`). Therefore, all 25 mapped endpoints require dedicated BFF/Adapter DTO controllers (`NEEDS-ADAPTER`), and 8 endpoints require new feature implementations (`MISSING`).

---

## 2. API Mismatch Matrix

| # | Method | Frontend URL | Frontend Request / Response Fields Used | Matching Backend Endpoint | Field Differences | Legacy Behaviour Reference | Verdict |
|:--|:-------|:-------------|:----------------------------------------|:--------------------------|:------------------|:---------------------------|:--------|
| **1** | GET | `/api/v1/blood/donors` | **Resp:** `Donor[]`: `id`, `name`, `age`, `gender`, `bloodGroup`, `phone`, `weight`, `pulse`, `bpSystolic`, `bpDiastolic`, `hemoglobin`, `lastDonationAt`, `registeredAt`. UI reads: `id`, `name`, `bloodGroup`, `gender`, `age`, `phone`, `registeredAt`. | `GET /api/v1/donors` (`DonorController`) | Backend returns `BloodDonor`: `donorId`, `name`, `bloodGroup`, `donationStatus`, `lastActionAt`. Missing: `id` (keyed as `donorId`), `gender`, `age`, `phone`, `registeredAt`. | `BloodBankServiceImpl.getDonorList` / `T_BLOODDONORREGISTRATION` (demographics captured in legacy registration). | **NEEDS-ADAPTER** |
| **2** | GET | `/api/v1/blood/donors/{id}` | **Resp:** `Donor` (full donor details including vitals and demographics). | `GET /api/v1/donors/{id}` (`DonorController`) | Backend returns `BloodDonor` entity lacking demographic & clinical exam fields (`age`, `gender`, `weight`, `vitals`). | `BloodBankServiceImpl.getDonorRegistrationDetails` / `BloodDonorRegistrationDetails`. | **NEEDS-ADAPTER** |
| **3** | POST | `/api/v1/blood/donors` | **Req:** `DonorInput`: `name`, `age`, `gender`, `bloodGroup`, `phone`, `weight`, `pulse`, `bpSystolic`, `bpDiastolic`, `hemoglobin`, `lastDonationAt`.<br>**Resp:** `Donor` | `POST /api/v1/donors` (`DonorController`) | Backend body only accepts `name`, `bloodGroup`, `donationStatus`, `unitId`. All demographic & medical exam fields are discarded. Returns `BloodDonor`. | `BloodBankServiceImpl.addUpdateBloodDonorRegistration` (persisted donor master and medical examination). | **NEEDS-ADAPTER** |
| **4** | GET | `/api/v1/blood-bags` | **Resp:** `BloodBag[]`: `id`, `bagNo`, `bloodGroup`, `component`, `collectionDate`, `expiryDate`, `status`, `donorId`, `donorName`. UI displays: `bagNo`, `bloodGroup`, `component`, `volume`, `collectionDate`, `expiryDate`, `status`. | `GET /api/v1/bags` (`BagController`) | Backend returns `BloodBag`: `bagId`, `donorId`, `bloodGroup`, `volumeMl`, `collectedAt`, `expirydate`, `crossMatched`, `isbagexpired`, `issued`. Missing `id` (keyed as `bagId`), `bagNo`, `component`, `status` string (backend has booleans), `collectionDate` vs `collectedAt`, `expiryDate` vs `expirydate`. | `BloodBankServiceImpl.getBloodExtractionList` / `T_BLOODBAG` & `T_BLOODCOMPONENT` join. | **NEEDS-ADAPTER** |
| **5** | GET | `/api/v1/blood-bags/{id}` | **Resp:** `BloodBag` (single bag with component, status, collection/expiry dates). | `GET /api/v1/bags/{id}` (`BagController`) | Same as #4: backend lacks `bagNo`, `component`, `status` string, and has divergent timestamp/date field names. | `BloodBankServiceImpl.searchBloodBagListWithCode` / `BloodBagExtraction`. | **NEEDS-ADAPTER** |
| **6** | POST | `/api/v1/blood-bags` | **Req:** `BloodBagInput`: `bloodGroup`, `component`, `donorId`, `volume`, `collectionDate`, `expiryDate`.<br>**Resp:** `BloodBag` | `POST /api/v1/bags` (`BagController`) | Backend expects `donorId`, `bloodGroup`, `volumeMl` (not `volume`), `expirydate` (not `expiryDate`), `unitId`. Backend ignores `component` and `collectionDate`. Returns `BloodBag`. | `BloodBankServiceImpl.addUpdateBloodsDonorBagCollection` (`T_BLOODBAG` + `T_BLOODCOMPONENT`). | **NEEDS-ADAPTER** |
| **7** | PATCH | `/api/v1/blood-bags/{id}/status` | **Req:** `{ status: string }`<br>**Resp:** `BloodBag` | NONE | Backend has no direct bag status patch endpoint. Bag lifecycle transitions occur through requisitions, discards (`POST /api/v1/bloodbank/bag-discards`), returns (`POST /api/v1/bloodbank/bag-returns`), or automated expiry sweeps. | Status changes in legacy were discrete stored procedure actions (`cancelBloodBagComponent`, `BloodBagDiscard`). | **MISSING** |
| **8** | GET | `/api/v1/blood-bags/stock` | **Resp:** `Record<string, number>` (key: blood group, value: available bag units). | NONE | No stock aggregate endpoint exists in `clinical-support-service` for blood bags. | `BloodBankServiceImpl.getBloodBankStockCountView` / `bloodbanksWithStockSchema.json` / `showBloodStockList.html`. | **MISSING** |
| **9** | GET | `/api/v1/blood-bags/{bagId}/events` | **Resp:** `BagEvent[]`: `id`, `bagId`, `type`, `occurredAt`, `location`, `performedBy`, `note`. | `GET /api/v1/bloodbank/bags/{bagId}/trail` (`BloodBankOperationsController`) | Backend returns `BloodBagTrail`: `id`, `bagId`, `eventType` (Short numeric code 1..12), `eventAt` (Timestamp), `actorId` (Long), `refDocType`, `refDocId`, `remarks`. Frontend expects `type` (String name), `occurredAt`, `performedBy` (string actor), `note` (vs `remarks`), `location`. | `com.ohum.models.BloodBagTrail` / `T_BLOODBAG_TRAIL`. | **NEEDS-ADAPTER** |
| **10** | POST | `/api/v1/blood-bags/{bagId}/events` | **Req:** `Partial<BagEvent>`: `{ type, note, ... }`<br>**Resp:** `BagEvent` | `POST /api/v1/bloodbank/bags/{bagId}/trail` (`BloodBankOperationsController`) | Backend expects `{ eventType: short, actorId: long, refDocType: string, refDocId: long, remarks: string }`. Payload structure and field types do not match. | `BloodBagTrail` event logging. | **NEEDS-ADAPTER** |
| **11** | GET | `/api/v1/blood/cross-matches` | **Resp:** `CrossMatch[]`: `id`, `bagId`, `patientId`, `patientName`, `compatibility` ("COMPATIBLE"\|"INCOMPATIBLE"\|"PENDING"), `method` ("MAJOR"\|"MINOR"\|"BOTH"), `requestedBy`, `requestedAt`, `completedAt`, `note`. | NONE | Backend only has `POST /api/v1/requisitions/{id}/crossmatch` (bag-to-requisition linkage) and `BloodBankCrossUnit` (inter-facility transfers). There is no patient lab cross-match test results query endpoint. | `BloodBankServiceImpl.getCrossMatchingTestResults` / `BloodCrossmatchingResult` / `bloodCrossMatchingListSchema.json`. | **MISSING** |
| **12** | GET | `/api/v1/blood/cross-matches/{id}` | **Resp:** `CrossMatch` | NONE | No cross-match detail endpoint exists. | Legacy `BloodCrossMatchResultDetails`. | **MISSING** |
| **13** | POST | `/api/v1/blood/cross-matches` | **Req:** `CrossMatchInput`: `bagId`, `patientId`, `method`, `note`.<br>**Resp:** `CrossMatch` | NONE | Backend crossmatch is `POST /api/v1/requisitions/{id}/crossmatch` which accepts `{ bagId }` on an existing requisition and returns a `BloodBag`. No standalone cross-match entity or laboratory test workflow exists. | `BloodBankServiceImpl.saveBloodCrossMatchingTestResult` (`T_BLOODCROSSMATCHINGRESULT`). | **MISSING** |
| **14** | PATCH | `/api/v1/blood/cross-matches/{id}/compatibility` | **Req:** `{ compatibility: "COMPATIBLE" \| "INCOMPATIBLE" \| "PENDING" }`<br>**Resp:** `CrossMatch` | NONE | No compatibility status patch endpoint exists. | Legacy crossmatch result finalization. | **MISSING** |
| **15** | GET | `/api/v1/blood/indents` | **Resp:** `Indent[]`: `id`, `sourceStore`, `targetStore`, `bloodGroup`, `component`, `units`, `status` ("REQUESTED"\|"APPROVED"\|"DISPATCHED"\|"RECEIVED"\|"REJECTED"), `requestedBy`, `requestedAt`, `fulfilledAt`, `note`. | `GET /api/v1/bloodbank/indents` (`BloodBankOperationsController`) | Backend returns `BloodBankIndent`: `id`, `indentNo`, `fromStoreId` (Long), `toStoreId` (Long), `bloodGroup`, `component`, `quantity` (Integer), `requestedDate` (Date), `remarks`, `docStatus` (Short 1..4), `approvedBy`, `approvedDatetime`, `issuedAt`. Frontend expects string store names, `units`, string status enum, `note`. | `BloodBankServiceImpl.getHospitalWiseComponent` / `T_BLOODBANKINDENT`. | **NEEDS-ADAPTER** |
| **16** | GET | `/api/v1/blood/indents/{id}` | **Resp:** `Indent` | `GET /api/v1/bloodbank/indents/{id}` (`BloodBankOperationsController`) | Same as #15: Store IDs instead of names, `quantity` vs `units`, `docStatus` numeric code vs string enum, `remarks` vs `note`. | `T_BLOODBANKINDENT` entity lookup. | **NEEDS-ADAPTER** |
| **17** | POST | `/api/v1/blood/indents` | **Req:** `IndentInput`: `sourceStore`, `targetStore`, `bloodGroup`, `component`, `units`, `note`.<br>**Resp:** `Indent` | `POST /api/v1/bloodbank/indents` (`BloodBankOperationsController`) | Backend requires `{ fromStoreId: long, toStoreId: long, bloodGroup: string, component: string, quantity: int, requestedDate: string, remarks: string }`. Frontend sends store names and `units`. | `BloodBankServiceImpl.addBloodBankIndent` (`@Path("/addBloodBankIndent")`). | **NEEDS-ADAPTER** |
| **18** | PATCH | `/api/v1/blood/indents/{id}/status` | **Req:** `{ status: Indent["status"] }`<br>**Resp:** `Indent` | `POST /api/v1/bloodbank/indents/{id}/approve`, `/issue`, `/cancel` | Backend uses separate RPC-style POST actions for workflow steps (`/approve`, `/issue`, `/cancel`), not a generic HTTP PATCH status endpoint. | Legacy indent approval/issue workflow. | **NEEDS-ADAPTER** |
| **19** | GET | `/api/v1/journals` | **Resp (queries.ts):** `JournalEntry[]`: `journalId`, `voucherNo`, `entryType`, `entryDate`, `narration`, `status`, `totalDebit`, `totalCredit`, `isPosted`, `legs: [...]`.<br>**Resp (RmrsPage.tsx):** `j.id`, `j.voucherNo`, `j.type`, `j.amount`, `j.date`, `j.status`. | `GET /api/v1/rmrs/journal-vouchers` (`RmrsM23Controller`) | Backend returns `JournalVoucher`: `id`, `voucherId`, `voucherNo`, `narration`, `voucherDate`, `docStatus` (Short 1..3), `postedBy`, `postedDatetime`, `reversedBy`, `reversedDatetime`. Backend has NO `entryType`/`type`, NO `amount`/`totalDebit`/`totalCredit`, NO `entryDate`/`date`, NO string `status`, NO embedded `legs` (legs require separate `GET /legs` call), and NO `isPosted` boolean. | `RMRSServiceImpl.getCollectionDetailList` / `RMRSDashboardTransaction` (`T_JOURNAL_VOUCHER`). | **NEEDS-ADAPTER** |
| **20** | GET | `/api/v1/journals/{id}` | **Resp:** `JournalEntry` (including full legs array). | `GET /api/v1/rmrs/journal-vouchers/{id}` (`RmrsM23Controller`) | Backend returns single `JournalVoucher` header without legs, missing `entryType`, `entryDate`, `totalDebit`, `totalCredit`. Legs are served at `/journal-vouchers/{id}/legs`. | `RMRSServiceImpl` voucher detail view. | **NEEDS-ADAPTER** |
| **21** | POST | `/api/v1/journals` | **Req:** `JournalEntryValues`: `entryType`, `entryDate`, `narration`, `referenceType`, `referenceId`, `legs: [{ accountId, accountName, debit, credit, narration, costCenterId, costCenterName }]`.<br>**Resp:** `JournalEntry` | `POST /api/v1/rmrs/journal-vouchers` (`RmrsM23Controller`) | Backend expects `{ narration: string, voucherDate: string, legs: [{ ledgerId: long, debit: decimal, credit: decimal }] }`. Backend legs use `ledgerId` (not `accountId`), has no `costCenter`, expects `voucherDate` (not `entryDate`), ignores `entryType`, and returns `JournalVoucher` (not `JournalEntry`). | `RMRSServiceImpl.addCollection` / `addPaymentNew`. | **NEEDS-ADAPTER** |
| **22** | PUT | `/api/v1/journals/{id}/post` | **Resp:** `JournalEntry` | `POST /api/v1/rmrs/journal-vouchers/{id}/post` (`RmrsM23Controller`) | Method mismatch (PUT on frontend vs POST on backend). Response is `JournalVoucher` instead of `JournalEntry`. | `RMRSServiceImpl.approveOrRelease`. | **NEEDS-ADAPTER** |
| **23** | GET | `/api/v1/bank/accounts` | **Resp:** `BankAccount[]`: `accountId`, `accountName`, `accountNumber`, `bankName`, `branchName`, `ifscCode`, `accountType`, `openingBalance`, `currency`. UI reads: `accountId`, `bankName`, `accountNumber`, `accountType`, `openingBalance`. | NONE | No bank accounts catalog exists in `admin-service:8120`. Bank accounts in modern RMRS are represented only as foreign key references (`bank_ledger_id`) to `t_ledger_master`. | Legacy bank reconciliation modules stored bank details in master tables (`M_ACCOUNT_HEAD_MASTER`). | **MISSING** |
| **24** | GET | `/api/v1/bank/reconciliation` | **Req Params:** `bankAccountId`, `fromDate`, `toDate`, `status`, `page`, `size`.<br>**Resp:** `BankReconciliation`: `statementId`, `bankAccountId`, `accountNumber`, `statementDate`, `fromDate`, `toDate`, `openingBalance`, `closingBalance`, `totalDebits`, `totalCredits`, `netChange`, `unmatchedCount`, `matchedCount`, `disputedCount`, `lines: BankStatementRow[]`. | `GET /api/v1/rmrs/bank-statements` (`RmrsM23Controller`) | Backend returns flat `List<BankStatement>` (`statementLineId`, `bankLedgerId`, `txnDate`, `amount`, `direction`, `description`, `externalRef`, `docStatus`, `matchedEntryId`, `matchedCollectionId`). It does not compute aggregate balances, credits, debits, or counts, and does not accept `bankAccountId` or date filters. | `RMRSServiceImpl` bank reconciliation summary statements. | **NEEDS-ADAPTER** |
| **25** | POST | `/api/v1/bank/reconciliation/statement` | **Req:** `{ bankAccountId: number, lines: BankStatementLineValues[] }`<br>**Resp:** `{ statementId: number, importedRows: number }` | `POST /api/v1/rmrs/bank-statements` (`RmrsM23Controller`) | Frontend sends batch array of statement lines for an account. Backend only accepts a single statement line: `{ bankLedgerId, txnDate, amount, direction, description, externalRef }`. | Legacy bulk bank statement upload. | **NEEDS-ADAPTER** |
| **26** | POST | `/api/v1/bank/reconciliation/match` | **Req:** `{ statementLineId, journalEntryId, matchAmount, remarks }`<br>**Resp:** `{ matchId: number, status: string }` | `POST /api/v1/rmrs/bank-statements/{id}/reconcile` (`RmrsM23Controller`) | Backend uses path-based ID (`/{id}/reconcile`) and expects `{ matchedEntryId: long, matchedCollectionId: long }`. Frontend sends `{ statementLineId, journalEntryId, matchAmount }` in request body. | `RMRSServiceImpl` statement line reconciliation against accounting entries. | **NEEDS-ADAPTER** |
| **27** | POST | `/api/v1/bank/reconciliation/dispute` | **Req:** `{ statementLineId: number, reason?: string }`<br>**Resp:** status | `POST /api/v1/rmrs/bank-statements/{id}/dispute` (`RmrsM23Controller`) | Backend uses path-based ID (`/{id}/dispute`) and body `{ reason }`. Frontend sends `statementLineId` in body. | Legacy dispute marking on unreconciled statement items. | **NEEDS-ADAPTER** |
| **28** | GET | `/api/v1/po/departments` | **Resp:** `PoDepartment[]`: `departmentId`, `departmentName`, `headName`, `headEmail`. | NONE | No department master endpoint exists in `admin-service:8120`. (Department masters are in other services or `t_head_master` in `MasterController`). | Legacy `DepartmentScheduleServiceImpl` / `MasterServiceImpl`. | **MISSING** |
| **29** | GET | `/api/v1/po/items` | **Req Params:** `search`.<br>**Resp:** `PoItem[]`: `itemId`, `itemCode`, `itemName`, `uom`, `estimatedRate`, `category`, `description`. | NONE | No item catalog exists in `admin-service:8120`. Items are owned by `item-service:8082` at `/api/v1/items`. | Legacy `ItemMasterServiceImpl` / `T_ITEMMASTER`. | **MISSING** |
| **30** | GET | `/api/v1/po/requisitions` | **Resp:** `PoRequisition[]`: `requisitionId`, `requisitionNo`, `departmentId`, `departmentName`, `requestedBy`, `priority`, `status`, `justification`, `totalEstimatedAmount`, `expectedDeliveryDate`, `vendorSuggestion`, `lines: PoRequisitionLine[]`, `approvals: PoApproval[]`. | `GET /api/v1/rmrs/po-requests` (`RmrsM23Controller`) | Backend returns `PoRequest`: `requestId`, `requestNo`, `requestingDeptId`, `headId`, `ledgerId`, `amount`, `remarks`, `docStatus`, `approvedBy`, `approvedDatetime`, `issuedPoId`. Backend has no line items (`lines`), no `approvals`, no `priority`, no `justification`. | `com.ohum.services.pas.PurchaseOrderServiceImpl` / `T_PURCHASEORDER`. | **NEEDS-ADAPTER** |
| **31** | GET | `/api/v1/po/requisitions/{id}` | **Resp:** `PoRequisition` (including item lines and approval history). | `GET /api/v1/rmrs/po-requests/{id}` (`RmrsM23Controller`) | Backend returns single `PoRequest` header without line items or multi-step approval workflow data. | `PurchaseOrderServiceImpl.getPurchaseOrderList`. | **NEEDS-ADAPTER** |
| **32** | POST | `/api/v1/po/requisitions` | **Req:** `PoRequestValues`: `departmentId`, `departmentName`, `requestedBy`, `justification`, `priority`, `expectedDeliveryDate`, `vendorSuggestion`, `lines: [{ itemId, itemName, uom, quantity, estimatedRate, lineTotal }]`, `totalEstimatedAmount`.<br>**Resp:** `PoRequisition` | `POST /api/v1/rmrs/po-requests` (`RmrsM23Controller`) | Backend expects financial budget allocation: `{ requestingDeptId, headId, ledgerId, amount, remarks }`. Frontend sends procurement cart with multiple item lines. | `PurchaseOrderServiceImpl.createPurchaseOrder`. | **NEEDS-ADAPTER** |
| **33** | PUT | `/api/v1/po/requisitions/{id}/action` | **Req:** `PoRequestActionValues`: `{ action: "SUBMIT"\|"APPROVE"\|"REJECT"\|"CANCEL"\|"REVISE", remarks: string }`<br>**Resp:** `PoRequisition` | `POST /api/v1/rmrs/po-requests/{id}/approve`, `/reject`, `/issue` (`RmrsM23Controller`) | Method mismatch (PUT vs POST), and path mismatch (`/action` with payload vs dedicated lifecycle action endpoints). | Legacy purchase order approval workflow. | **NEEDS-ADAPTER** |

---

## 3. Detailed Domain Analysis

### 3.1 Blood Bank Service (`services/clinical-support-service`)

#### 1. Bag Management
- **Frontend Contract:**
  - Route: `/api/v1/blood-bags`
  - Model: `BloodBag { id, bagNo, bloodGroup, component, collectionDate, expiryDate, status, donorId, donorName }`
- **Backend Model (`com.hims.bloodbank.domain.model.BloodBag`):**
  - Table: `ihms_bloodbank.t_bloodbag`
  - Serialized fields: `bagId`, `donorId`, `bloodGroup`, `volumeMl`, `collectedAt`, `expirydate`, `crossMatched`, `crossMatchedTestDate`, `requisitionId`, `isbagexpired`, `issued`, `issuedRequisitionId`.
  - Missing in Backend: `bagNo` (clean model does not persist bag barcode as a separate column on `BloodBag`), `component` (components hang off separate legacy table `t_bloodcomponent`), `status` (represented by boolean flags `issued`, `crossMatched`, `isbagexpired`), `collectionDate` vs `collectedAt`, `expiryDate` vs `expirydate`.
- **Verdict:** `NEEDS-ADAPTER`. A BFF or adapter controller must map `BloodBag` properties and join or derive `component` and `status`.

#### 2. Donors
- **Frontend Contract:**
  - Route: `/api/v1/blood/donors`
  - Model: `Donor { id, name, age, gender, bloodGroup, phone, weight, pulse, bpSystolic, bpDiastolic, hemoglobin, lastDonationAt, registeredAt }`
- **Backend Model (`com.hims.bloodbank.domain.model.BloodDonor`):**
  - Table: `ihms_bloodbank.t_blooddonor`
  - Serialized fields: `donorId`, `name`, `bloodGroup`, `donationStatus`, `lastActionAt`.
  - Missing in Backend: `gender`, `age`, `phone`, `weight`, `pulse`, `bpSystolic`, `bpDiastolic`, `hemoglobin`, `lastDonationAt`, `registeredAt`.
- **Verdict:** `NEEDS-ADAPTER`. The backend donor registry is a minimal workflow tracker (GAP-REGISTER WP-9) for the 72-hour staleness rule (BR-BLD-005), whereas the frontend expects a full donor health intake record.

#### 3. Cross Matching
- **Frontend Contract:**
  - Route: `/api/v1/blood/cross-matches`
  - Model: `CrossMatch { id, bagId, patientId, patientName, compatibility, method, requestedBy, requestedAt, completedAt, note }`
- **Backend Model:**
  - No matching patient cross-match entity exists.
  - Existing backend endpoint `POST /api/v1/requisitions/{id}/crossmatch` merely links a bag ID to a requisition without storing method, compatibility status, or test notes.
- **Verdict:** `MISSING`. Patient crossmatch laboratory testing entity and endpoints must be ported from legacy `BloodCrossmatchingResult`.

#### 4. Indents
- **Frontend Contract:**
  - Route: `/api/v1/blood/indents`
  - Model: `Indent { id, sourceStore, targetStore, bloodGroup, component, units, status, requestedBy, requestedAt, fulfilledAt, note }`
- **Backend Model (`com.hims.bloodbank.domain.model.BloodBankIndent`):**
  - Table: `ihms_bloodbank.t_bloodbank_indent`
  - Serialized fields: `id`, `indentNo`, `fromStoreId`, `toStoreId`, `bloodGroup`, `component`, `quantity`, `requestedDate`, `remarks`, `docStatus`, `approvedBy`, `approvedDatetime`, `issuedAt`.
  - Mismatches: Store IDs (numeric) vs store names (strings); `quantity` vs `units`; `docStatus` (short integer 1..4) vs `status` enum string; `remarks` vs `note`.
- **Verdict:** `NEEDS-ADAPTER`.

---

### 3.2 RMRS Service (`services/admin-service`)

#### 1. Journals & Treasury Daybook
- **Frontend Contract:**
  - Route: `/api/v1/journals`
  - Model (`queries.ts`): `JournalEntry { journalId, voucherNo, entryType, entryDate, narration, status, totalDebit, totalCredit, isPosted, legs: [...] }`
  - Model (`RmrsPage.tsx`): `{ id, voucherNo, type, amount, date, status }`
- **Backend Model (`com.hims.rmrs.domain.model.JournalVoucher`):**
  - Table: `ihms_rmrs.t_journal_voucher`
  - Serialized fields: `id`, `voucherId`, `voucherNo`, `narration`, `voucherDate`, `docStatus`, `postedBy`, `postedDatetime`, `reversedBy`, `reversedDatetime`.
  - Mismatches: Header does not contain `amount` or `totalDebit`/`totalCredit` (amount is calculated from legs in `t_journal_voucher_leg`), no `entryType`, `voucherDate` vs `entryDate`/`date`, `docStatus` (Short) vs string status, and legs are in a separate table/endpoint (`/journal-vouchers/{id}/legs`).
- **Verdict:** `NEEDS-ADAPTER`.

#### 2. Bank Accounts & Reconciliation
- **Frontend Contract:**
  - Routes: `/api/v1/bank/accounts`, `/api/v1/bank/reconciliation`, `/api/v1/bank/reconciliation/statement`, `/api/v1/bank/reconciliation/match`
- **Backend Model:**
  - Bank accounts are not modeled as standalone entities in `admin-service`; banks exist only as general ledger entries (`ihms_rmrs.t_ledger_master`).
  - Statement lines exist in `ihms_rmrs.t_bank_statement`, but backend endpoints operate line-by-line (`/bank-statements`) rather than aggregating account statements, balances, and dispute summaries.
- **Verdict:** Bank Accounts is `MISSING`. Bank Reconciliation is `NEEDS-ADAPTER`.

#### 3. Purchase Requisitions (PO)
- **Frontend Contract:**
  - Route: `/api/v1/po/requisitions`, `/api/v1/po/departments`, `/api/v1/po/items`
  - Frontend assumes a multi-line shopping cart requisition workflow with items, UOM, rate, justification, and approval history.
- **Backend Model (`com.hims.rmrs.domain.model.PoRequest`):**
  - Table: `ihms_rmrs.t_po_request`
  - Represents a high-level budget fund requisition for procurement (`requestingDeptId`, `headId`, `ledgerId`, `amount`, `remarks`, `docStatus`). Does not store individual items or item catalog lines.
  - Item catalog (`PoItem`) belongs to `item-service:8082` (`/api/v1/items`), not `admin-service`.
- **Verdict:** Departments and Items are `MISSING` in `admin-service`. Requisition endpoints are `NEEDS-ADAPTER`.

---

## 4. Architectural Rules & Next Steps

1. **Strict Separation of Concerns:**
   - Per modernization rules, no new business logic or speculative adapter endpoints should be added directly into domain controllers without formal ticket specification.
   - `/api/v1/masters` in `platform/gateway/nginx.conf` must NEVER be rerouted to `admin-service`, as `/api/v1/masters` is owned by `reference-service:8081`.
   - `/api/v1/jobs` and `/api/v1/internal` must remain unexposed to public gateway routes.
2. **Implementation Roadmap for Adapters:**
   - **Blood Bank Adapter:** Create a BFF or query projection service in `clinical-support-service` (e.g. `BloodBankBffController` under `/api/v1/blood-bags`, `/api/v1/blood/...`) that adapts between `BloodBag`/`BloodDonor`/`BloodBankIndent` and the frontend models.
   - **RMRS Treasury Adapter:** Create an adapter controller in `admin-service` exposing `/api/v1/journals` that joins `t_journal_voucher` and `t_journal_voucher_leg` to return the enriched `JournalEntry` and `RmrsPage` DTO structure.
   - **Cross-Service Gateway Integration:** For `/api/v1/po/items`, configure the gateway to route directly to `item-service:8082`.
