# Table-Ownership Verification (GAP-05)

Verified G-01/G-02/G-23 table-name claims against legacy iHMS JPA annotations.
Ground truth: all `@Table(name = "...")` under `/Users/adityabanerjee/personal_project/ihms/src/main/java/com/ohum/models/` — 937 annotated models, indexed and matched case-insensitively (exact + prefix). Evidence paths are relative to that `models/` root. Legacy repo uncommitted-state as of 2026-08-22.

Verdicts: **confirmed** = claimed name exists in legacy `@Table`; **corrected** = claim wrong, real counterpart identified; **unresolved** = no legacy counterpart found by name search.

| # | Claimed name | Gap | Verdict | Legacy evidence |
|---|---|---|---|---|
| 1 | T_PATIENTREQUISITION | G-01 | **confirmed** (ownership → blood bank) | `transactionals/bloodBank/PatientBloodRequisition.java:29` → `t_patientRequisition`; details child `transactionals/bloodBank/PatientBloodRequisitionDetails.java:30` → `t_patientRequisitionDetails`. Model lives in the **bloodBank package**, supporting 02 M17 / STATUS-CODE-DICTIONARY §7 ownership; MICROSERVICE-CATALOG lab svc#7 claim is contradicted. |
| 2 | T_BLOODREQUISITION | G-01 | **corrected** (name does not exist — G-01's "invented" assessment confirmed) | Zero hits; only near-name is `masters/BloodRequisitionReasonMaster.java:25` → `M_BLOODREQUISITIONREASONMASTER`. |
| 3 | T_GRN* (svc 10) | G-02 | **unresolved** | No `T_*GRN*` tables at all; only masters `masters/GrnRejectReasonMaster.java:35` → `M_GrnRejectReasonMaster`, `masters/Grnitemchecklist.java:32` → `M_GRNITEMCHECKLIST`. GRN transactional data likely lands in `T_ITEMSTOCK` (`transactionals/ItemStock.java:27`) — proc-caller check needed. |
| 4 | T_INDENT | G-02 | **unresolved** | No bare indent table. Actuals: `EaushadhiIndentTrx.java:21` → `T_EAUSHADHI_INDENT_TRX`; `transactionals/bloodBank/BloodBankIndent.java:29` → `T_BloodBankIndent`; `transactionals/IndentInvoice.java:24` → `T_INDENTINVOICE`. |
| 5 | T_SUPPLIER | G-02 | **corrected** | `masters/Supplier.java:41` → `M_SUPPLIER`; `masters/SupplierMaster.java:31` → `M_SUPPLIERMASTER`; `transactionals/ItemSupplier.java:28` → `T_ITEMSUPPLIER`. |
| 6 | T_QUOTATION | G-02 | **unresolved** | Zero hits for any quotation table in legacy `@Table`. |
| 7 | T_PURCHASEORDER | G-02 | **corrected** | `transactionals/PurchaseOrderRequest.java:30` → `T_purchaseOrderRequest`. No standalone PO table found. |
| 8 | T_ITEMBATCHMASTER | G-02 | **corrected** | `masters/Itembatchmaster.java:33` → `M_ITEMBATCHMASTER` (master, not T_); issued-batch trx `txt/IssuedItemBatchDetails.java:33` → `t_poissueitembatchdetails`. |
| 9 | T_DOCTORBILL | G-02 | **corrected** | `transactionals/Doctorvisitcharge.java:29` → `T_DOCTORVISITCHARGES`; also `transactionals/Doctorwaiver.java:33` → `T_DOCTORWAIVER`. Matches 02 M06/M04 evidence per G-02. |
| 10 | T_DOCTORSHARE | G-02 | **unresolved** | Zero hits (only unrelated `trx/abdm/PatientAbdmToken.java:17` → `T_PATIENT_ABDM_SHARETOKEN`). |
| 11 | T_CONCESSION | G-02 | **corrected** | Concession family is master-prefixed: `masters/Concessiontemplatemaster.java:39` → `M_ConcessionTemplateMaster`, `masters/Concessiontemplatedetails.java:33` → `M_ConcessionTemplateDetails`. No `T_CONCESSION` table. |
| 12 | T_USERROLES | G-02 | **corrected** | `masters/TUserrolemaster.java:38` → `T_USERROLEMASTER` (singular). Matches 02 M35. |
| 13 | T_AUTHORITYMASTER | G-02 | **corrected** | `masters/AuthorityMaster.java:26` → `M_AUTHORITYMASTER` (M_-prefixed, as G-02 hypothesized for svc 24 RMRS masters). |
| 14 | T_HEADSMASTER | G-02 | **corrected** | `masters/HeadsMaster.java:27` → `M_HEADSMASTER`. |
| 15 | T_LEDGERMASTER | G-02 | **corrected** | `masters/Ledgermaster.java:32` → `M_LEDGERMASTER`. |
| 16 | T_OPDSTATIONMASTER | G-02 | **corrected** | `masters/OpdStationMaster.java:25` → `M_OPDSTATIONMASTER`. Matches 02 M02 opdStationMaster. |
| 17 | T_EJS_* (svc 28) | G-02 | **corrected** | ejs package uses non-T_ names: `ejs/EjsMasterSubcenters.java:18` → `MASTER_SUBCENTERS`, `ejs/EjsUserTypes.java:18` → `USER_TYPES`, `ejs/EjsMasterActivity.java:31` → `MASTER_ACTIVITY`, `ejs/EjsIncentives.java:22` → `ANDROID_INCENTIVES`. Consistent with 05 §3.3/3.4. |
| 18 | T_AASMAAN_* (svc 28) | G-02 | **corrected** | All aasmaan models ship with `@Table` **commented out**: e.g. `aasmaan/AasmaanCaseHighrisk.java:17` → `//@Table(name = "")`. No active aasmaan table mappings exist in legacy JPA. |
| 19 | T_ADVANCE (svc 6) | G-02/G-23 | **unresolved** | Only `masters/Advanceagainst.java:37` → `M_ADVANCEAGAINST`. No advance-transaction table by this name. |
| 20 | T_REFUND (svc 6) | G-02/G-23 | **unresolved** | Zero hits. |
| 21 | T_RECEIPT (svc 6) | G-02/G-23 | **unresolved** | Zero hits. |
| 22 | T_CHARGE (G-23) vs corpus "T_Charges/T_CHARGES_TEMP" | G-23 | **corrected** (both sides drift) | Neither `T_CHARGE` nor any `T_CHARGES*` exists in legacy `@Table` (zero hits). Charge-named actuals: `T_DOCTORVISITCHARGES`, `T_PACKAGECHARGESUBSCRIPTION` (`transactionals/PackageChargeSubscription.java:23`), `M_CONFIG_AUTOCHARGES` (`masters/ConfigAutocharges.java:32`). |

## Summary

- Confirmed: 1 of 22 (T_PATIENTREQUISITION, with blood-bank ownership supported by package placement).
- Corrected: 13 (rows 2, 5, 7, 8, 9, 11–18, plus row 22 correcting both the ownership model and the corpus contrast).
- Unresolved: 8 (T_GRN*, T_INDENT, T_QUOTATION, T_DOCTORSHARE, T_ADVANCE, T_REFUND, T_RECEIPT, + GRN transactional placement) — these need procurement-proc caller analysis or DB-schema dumps, not more doc work.

Pattern: the 29-service ownership model's invented `T_*` names overwhelmingly correspond to real tables with different names/prefixes (`M_*` masters, camelCase transactionals), confirming G-02's suspicion systematically. No legacy table anywhere uses `T_EJS_*` or `T_AASMAAN_*`.

Scope note: additive-only verification per GAP-05 contract — no existing docs modified; corrections to MICROSERVICE-CATALOG/10-target-architecture land as follow-ups once god accepts this baseline.

## Native-SQL cross-check (2026-08-23)

Supplement to the @Table-scoped sweep above (GAP-09, jim-mt4k8u34): native-SQL string references in `src/main/java/com/ohum/services/` prove several "unresolved" tables exist **at DB level but have no JPA entity mapping**. All paths relative to `/Users/adityabanerjee/personal_project/ihms/src/main/java/com/ohum/`.

| Name | Above verdict | Native-SQL finding | Evidence |
|---|---|---|---|
| T_ADVANCE | unresolved | **exists, unmapped** — full SELECT with column list + join | `services/pas/PatientServiceImpl.java:5067-5072` (`SELECT T_ADVANCE.ID ... FROM T_ADVANCE INNER JOIN T_PAYMENTADVANCEDETAILS`); enum key `ADVANCE("T_ADVANCE")` at `services/MergePatientUtils.java:50` |
| T_GRN | unresolved | **exists, unmapped** — SELECT/JOIN refs | `services/custom/InventoryServiceImpl.java:5907` (`SELECT T_GRN.GRNNO FROM T_GRN`), `:10168` (`SELECT TRANSSTATUS FROM T_GRN WHERE id=?`), `:11044` (`FROM T_GRN inner join M_Store`) |
| T_PURCHASEORDER | corrected (#7) | **exists, unmapped** — a live PO table beyond `T_purchaseOrderRequest`; UPDATE/SELECT throughout | `services/custom/InventoryServiceImpl.java:750`, `:771`, `:2124` (`select ExpectedDateRemark, otherAmount, PATIENTID from T_PurchaseOrder where id = ?`), `:11054` (`T_PurchaseOrderItems.POID = T_PurchaseOrder.ID`) |
| T_CHARGES_TEMP / T_CHARGES | corrected (#22) | **exist as runtime temp-table pair, unmapped** — G-23's corpus names are real at DB level | `services/custom/AddServiceOrderServiceHelper.java:3725-3726` (`tblName = "T_CHARGES_TEMP"; if(consumed) tblName = "T_CHARGES"`); direct SQL at `:911` (`UPDATE T_CHARGES SET ADDEDBY...`), `:2237-2238` (`SELECT/DELETE FROM T_CHARGES_TEMP WHERE ID`) |
| T_REFUND | unresolved | **still not evidenced as a table** — all hits are autocode-sequence labels passed to `getLatestAutocodeSeq`, not SQL table refs | `services/custom/BillCancellationServiceImpl.java:322`; `services/custom/AddServiceOrderServiceImpl.java:2766`, `:3654` |
| T_QUOTATION | unresolved | **absent everywhere** — zero hits incl. quoted SQL across `src/main/java` | exhaustive grep, 0 results |
| T_DOCTORSHARE | unresolved | **absent everywhere** — zero hits incl. quoted SQL | exhaustive grep, 0 results |
| T_RECEIPT | unresolved | **absent everywhere** — zero hits incl. quoted SQL | exhaustive grep, 0 results |

Net effect on the Summary above: rows 19–22 move from "unresolved/corrected" to **exists-at-DB-level-unmapped** (T_ADVANCE, T_GRN, T_PURCHASEORDER+Items, T_CHARGES/_TEMP); only T_QUOTATION, T_DOCTORSHARE, T_RECEIPT remain genuinely unexplained. Migration planning should treat the unmapped four as schema sources even though DDL cannot be derived from entities.
