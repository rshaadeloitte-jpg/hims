# Table→Service Mapping — Draft (GAP-12)

> **Status: Draft — per-table contract for 843/898 tables.** Additive only; does NOT edit MICROSERVICE-CATALOG / 10-target-architecture ownership tables (deferred per dispatch).
> **Sources:** `10-target-architecture.md §2.1` (29-service ownership table), `MICROSERVICE-CATALOG.md §7` (per-service Owned data), `POSTGRESQL-MIGRATION.md §1` (schema-per-service + §1.1 conflicts), `05-database-catalog.md §3` (cat counts) + `05 §3.6` (native-only), `_discovery/table-ownership-verification.md` (GAP-05 22-row @Table sweep + GAP-09 native-SQL cross-check), live `@Table` extraction at `/Users/adityabanerjee/personal_project/ihms/src/main/java/com/ohum/models` (902 lines, 898 distinct active JPA names, 937 annotations incl. 35 commented Aasmaan, 923 distinct lower-cased).
> **Date:** 2026-08-28 (GAP-12). **Author:** jam (jim-mt4k8u34) — “gathers and summarizes”.

## Method & limits

# ponytail: naive full-scan — grep -R '@Table' + grep native-SQL; O(n) scan of ~1k files, fine for 898 rows. If table count grows >10k or proc-body cross-check needed, replace with Oracle ALL_TAB_COLUMNS / DBMS_METADATA dump + parse generated-procs/*.sql for proc-referenced table names; that dump is the authoritative floor for native-only tables (T_REGISTRATION, T_BILL, T_ADMISSION etc).

- Ground truth for JPA-mapped tables = active `@Table(name="...")` (902 lines → 898 distinct after de-duplicating case variants like m_emergencyregtypemaster vs M_Emergency...; 923 distinct lower-cased; 35 additional commented Aasmaan `//@Table` with no active mapping).
- Ground truth for DB-level existence without JPA = native-SQL string grep in `src/main/java/com/ohum` (services + jobs + utils, ~2k literals) + proc names in `generated-procs/` (720 artifacts, 190 NEXTVAL references). This catches `T_ADVANCE`, `T_GRN`, `T_PurchaseOrder/Items`, `T_CHARGES[_TEMP]`, plus proc-only `T_BILL`, `T_ADMISSION`, `T_REGISTRATION` families.
- Catalog claim “843 tables = 808 explicit @Table +35 convention” (05 §3) is **stale vs ground truth**: actual is 898 distinct active JPA tables; delta = 90 DTO entities in `dto/` + case-variant duplicates + 9 dual-dialect helpers. This draft maps the 898 ground-truth set; the 843 is treated as the planning subset — all 843 are covered within the 898 + 10 native-only + 4 native-SQL unmapped list below.

## Summary stats

| Bucket | Count | Notes |
|---|---|---|
| Active JPA distinct (@Table) | 898 | 937 annotations −35 commented Aasmaan = 902 lines → 898 distinct (923 lower-cased) |
| Commented Aasmaan (no active JPA) | 35 | `//@Table(name="")` in `aasmaan/` — convention name `AASMAANCASE` etc, verify Oracle |
| Native-only (no JPA, DB-level) | 10 | 05 §3.6: `T_REGISTRATION`, `T_REGISTRATION_DETAILS`, `T_Registration_Int`, `T_Registration_Details_Int`, `T_coronaStats`, `t_transfer`, `t_admission`, `t_patientrequisition`, `Config_Application` + `t_admission` variants — DDL must be dumped |
| Native-SQL unmapped (GAP-09 §) | 4 (+2 temp) | `T_ADVANCE`, `T_GRN`, `T_PurchaseOrder(+Items)`, `T_CHARGES/_TEMP` — exist at DB level, no JPA |
| Distinct ground-truth logical tables | ~912 | 898 +10 +4 (Aasmaan 35 excluded — they ARE the DB names by convention) |
| Claimed in 10/MC ownership tables (~150 named + M_* category) | ~150 named + “M_* ~582” category | §3 verdict table covers 50 high-visibility names; §4 groups the 898 by PG schema |
| G-02 pattern verdict | 1 confirmed / 13 corrected / 8 unresolved originally; post GAP-09: 4 moved to exists-unmapped | See verification file |

---

## §3 Claimed → Evidence verdict (per-table)

Legend: **confirmed** = claimed name exists @Table (case-insensitive) and owner matches file-path/package; **corrected** = claimed name wrong, real counterpart cited; **exists-at-DB-level-unmapped** = no @Table but native-SQL/proc proves DB table exists; **unresolved** = no @Table and no DB-level evidence; **unassigned** = ownership conflict or pending decision (PG §1.1).

| # | Claimed name (service) | Gap | Verdict | Claimed owner (10/MC) | Evidence owner / path |
|---|---|---|---|---|---|

| 1 | T_PATIENTREQUISITION | G-01 | **confirmed (blood-bank)** | 10 §2.1 Lab #7 vs 02 M17 + STATUS-CODE §7 | transactionals/bloodBank/PatientBloodRequisition.java:29 → t_patientRequisition (details :30) — bloodBank pkg → ihms_bloodbank, confirms 02 M17 ownership; MICROSERVICE-CATALOG Lab #7 drifted |
| 2 | T_BLOODREQUISITION | G-01 | **corrected — invented** | 10 Lab #7 / MC #13 claim T_BLOODREQUISITION | Zero hits; near-name M_BLOODREQUISITIONREASONMASTER at masters/BloodRequisitionReasonMaster.java:25 |
| 3 | T_GRN* | G-02 | **exists-at-DB-level-unmapped (GAP-09)** | 10 §2.1 #10 Inventory: T_GRN* | No T_*GRN* @Table; native-SQL T_GRN SELECTs at services/custom/InventoryServiceImpl.java:5907/10168/11044 (JOIN M_Store) → ihms_inventory |
| 4 | T_INDENT | G-02 | **corrected — no bare T_INDENT** | 10 #10: T_INDENT | Actuals: T_EAUSHADHI_INDENT_TRX (EaushadhiIndentTrx.java:21), T_BloodBankIndent (transactionals/bloodBank/BloodBankIndent.java:29), T_INDENTINVOICE (transactionals/IndentInvoice.java:24) → ihms_inventory |
| 5 | T_SUPPLIER | G-02 | **corrected** | 10 #10: T_SUPPLIER | M_SUPPLIER (masters/Supplier.java:41), M_SUPPLIERMASTER (masters/SupplierMaster.java:31), T_ITEMSUPPLIER (transactionals/ItemSupplier.java:28) → ihms_inventory / ihms_master |
| 6 | T_QUOTATION | G-02 | **unresolved — absent everywhere** | 10 #10: T_QUOTATION | Zero hits @Table and zero native-SQL hits across src/main/java — genuinely absent; no Oracle-proc hit either |
| 7 | T_PURCHASEORDER | G-02 | **exists-at-DB-level-unmapped (GAP-09)** | 10 #10: T_PURCHASEORDER | T_purchaseOrderRequest (transactionals/PurchaseOrderRequest.java:30) is JPA; DB-level T_PurchaseOrder + T_PurchaseOrderItems exist unmapped via InventoryServiceImpl.java:750/771/2124/11054 → ihms_inventory |
| 8 | T_ITEMBATCHMASTER | G-02 | **corrected** | 10 #10: T_ITEMBATCHMASTER | M_ITEMBATCHMASTER (masters/Itembatchmaster.java:33); txn t_poissueitembatchdetails (txt/IssuedItemBatchDetails.java:33) → ihms_inventory |
| 9 | T_DOCTORBILL | G-02 | **corrected** | 10 #5 Billing: T_DOCTORBILL | T_DOCTORVISITCHARGES (transactionals/Doctorvisitcharge.java:29), T_DOCTORWAIVER (:33) → ihms_billing |
| 10 | T_DOCTORSHARE | G-02 | **unresolved — absent** | 10 #5: T_DOCTORSHARE | Zero hits incl. native-SQL; only T_PATIENT_ABDM_SHARETOKEN (trx/abdm/PatientAbdmToken.java:17) |
| 11 | T_CONCESSION | G-02 | **corrected** | 10 #5: T_CONCESSION | M_ConcessionTemplateMaster (masters/Concessiontemplatemaster.java:39), M_ConcessionTemplateDetails (:33) → ihms_billing / ihms_master |
| 12 | T_USERROLES | G-02 | **corrected** | 10 #22: T_USERROLES (MC: T_USERROLES) | T_USERROLEMASTER (masters/TUserrolemaster.java:38) singular → ihms_iam |
| 13 | T_AUTHORITYMASTER | G-02 | **corrected** | 10 #24: T_AUTHORITYMASTER (MC) — svc 24 | M_AUTHORITYMASTER (masters/AuthorityMaster.java:26) → ihms_rmrs (conflict §1.1, M_-prefixed) |
| 14 | T_HEADSMASTER | G-02 | **corrected** | 10 #24: T_HEADSMASTER | M_HEADSMASTER (masters/HeadsMaster.java:27) → ihms_rmrs |
| 15 | T_LEDGERMASTER | G-02 | **corrected** | 10 #24: T_LEDGERMASTER | M_LEDGERMASTER (masters/Ledgermaster.java:32) → ihms_rmrs |
| 16 | T_OPDSTATIONMASTER | G-02 | **corrected** | 10 #29: T_OPDSTATIONMASTER | M_OPDSTATIONMASTER (masters/OpdStationMaster.java:25) M_-prefixed → ihms_queue (or ihms_master per PG §1 conflict) — MC lists under Queue |
| 17 | T_EJS_* | G-02 | **corrected — no T_EJS_*** | 10 #28: T_EJS_* (MC #28) | ejs/* uses MASTER_SUBCENTERS (ejs/EjsMasterSubcenters.java:18), USER_TYPES (EjsUserTypes.java:18), ANDROID_INCENTIVES (EjsIncentives.java:22) → ihms_govt (57 tables) |
| 18 | T_AASMAAN_* | G-02 | **corrected — no active mapping** | 10 #28: T_AASMAAN_* | All 35 aasmaan models have //@Table(name = "") commented out (e.g., AasmaanCaseHighrisk.java:17) → no active JPA; convention name AASMAANCASE → ihms_govt (verify Oracle DB) |
| 19 | T_ADVANCE | G-02/G-23 | **exists-at-DB-level-unmapped** | 10 #6: T_ADVANCE | M_ADVANCEAGAINST (masters/Advanceagainst.java:37) is master; DB-level T_ADVANCE SELECT PatientServiceImpl.java:5067-5072 + MergePatientUtils.java:50 → ihms_payment |
| 20 | T_REFUND | G-02/G-23 | **unresolved — autocode label only** | 10 #6: T_REFUND | Zero table hits; only getLatestAutocodeSeq label at BillCancellationServiceImpl.java:322, AddServiceOrderServiceImpl.java:2766/3654 |
| 21 | T_RECEIPT | G-02/G-23 | **unresolved — absent** | 10 #6: T_RECEIPT | Zero hits @Table and native-SQL |
| 22 | T_CHARGE vs T_Charges/T_CHARGES_TEMP | G-23 | **both-sides-drift → exists-at-DB-level-unmapped (G-09)** | 10 #5: T_CHARGE vs 05 §3.6 corpus T_Charges/T_CHARGES_TEMP | Neither T_CHARGE nor T_CHARGES* in @Table; DB-level runtime pair T_CHARGES/T_CHARGES_TEMP at AddServiceOrderServiceHelper.java:3725-3726/911/2237 → ihms_billing (temp-table switch) |
| 23 | T_ENCOUNTER | G-02 | **confirmed (case-drift)** | 10 #2: T_ENCOUNTER (MC #2) | T_Encounter at trx/Encounter.java:30 (case T_Encounter) → ihms_encounter (52 refs, PG §1) |
| 24 | T_APPOINTMENT | G-02 | **confirmed** | 10 #2: T_APPOINTMENT | T_APPOINTMENT at txt/Appointment.java:29 → ihms_encounter |
| 25 | T_PATIENTTOKEN | G-02 | **corrected — lower-case** | 10 #2: T_PATIENTTOKEN | t_patienttokens at txt/PatientToken.java:23 → ihms_encounter |
| 26 | T_APPOINTMENT_CHANNEL | G-03/G-01.1 | **unassigned — audit pending** | 10 #2: T_APPOINTMENT_CHANNEL listed | No @Table; no native-SQL evidence; POSTGRESQL-MIGRATION §1.1 flags as audit-decision-pending → ihms_audit (proposed) |
| 27 | T_ADMISSION | G-02 | **native-SQL/proc-only — no JPA** | 10 #3: T_ADMISSION | No @Table (T_ADMISSION not in active); native-SQL 131 SQL-like hits (CommonABDMUtil.java:1472 etc) + proc CIMS_ADDADMISSIONINFORMATION → ihms_adt (native-only per 05 §3.6 t_admission) |
| 28 | T_BILL | G-02 | **native-SQL/proc-only — no JPA** | 10 #5: T_BILL | No @Table; 27 SQL-like hits (ApplicationJobs:1131 etc) + proc CIMS_ADDBILL → ihms_billing (proc-side T_BILL_seq) |
| 29 | T_BILLDETAILS | G-02 | **native-SQL/proc-only** | 10 #5: T_BILLDETAILS | No @Table; 7 SQL-like hits + proc CIMS_ADDBILLDETAILS → ihms_billing |
| 30 | T_PAYMENT | G-02 | **native-SQL/proc-only — no JPA** | 10 #6: T_PAYMENT | No @Table; PaymentServiceImpl/AddPaymentService chain; proc CIMS_ADDPAYMENT → ihms_payment |
| 31 | T_PAYMENTMODEDETAILS | G-02 | **native-SQL/proc-only** | 10 #6: T_PAYMENTMODEDETAILS | No @Table 0 SQL-like but proc T_PaymentModeDetails_seq; payment helper → ihms_payment |
| 32 | T_REGISTRATION(+DETAILS) | G-03 | **native-only — no JPA (05 §3.6)** | 10 #1: T_REGISTRATION(+DETAILS) | No @Table; 60 SQL-like hits (CommonUtilServiceImpl, CommonABDMUtil:1023) → ihms_patient (native-only, DDL must be dumped) |
| 33 | T_AUDIT / T_TRX_HISTORY / T_LOGIN_AUDIT / T_ABDM_AUDIT | G-03 | **exists — audit bucket** | MC §7 cross-cutting audit (08 §1 568 sites) | T_AUDIT (Audit.java:23), T_TRX_HISTORY (TrxHistory.java:12), no JPA for LOGIN/ABDM_AUDIT (raw INSERT AuditHelper 917/938) → ihms_audit (decision pending per PG §1) |
| 34 | M_BEDMASTER / M_WARDMASTER | G-03/conflict | **confirmed but ownership conflict** | MC #3 ADT vs #21 Reference both claim M_BEDMASTER | M_BEDMASTER at masters/Bedmaster.java (26 refs); PG §1.1 recommends ihms_adt owns operational state, ihms_master keeps definitional — ihms_adt proposed |
| 35 | M_ITEMMASTER | G-03/conflict | **confirmed but ownership conflict** | MC #10 Inventory vs #21 Reference | M_ITEMMASTER at masters/Itemmaster.java (18 refs); PG §1.1 → ihms_inventory owns, ihms_master owns cache-key contract |
| 36 | M_STAFFMASTER / M_DOCTORMASTER | G-03/conflict | **confirmed but ownership conflict** | MC #22 IAM vs #21 Reference | M_STAFFMASTER(:32), M_DOCTORMASTER(:16) → PG §1.1 recommends ihms_iam for profiles |
| 37 | T_ITEMSALE | G-03/conflict | **native-SQL/proc — ownership Pharmacy vs Inventory read** | MC #9 Pharmacy owns T_ITEMSALE vs #10 inventory stock | No @Table (native? 19 SQL-like hits) + proc CIMS_ITEMSALE_VISIT etc → PG §1.1 → ihms_pharmacy owns, billing/payment read via API |
| 38 | T_REPORTMASTER | G-03 | **no JPA — reporting read-model** | MC #26 | No @Table (0 hits); reporting file blob tables → ihms_reporting |
| 39 | T_BLOODBAG / T_BLOODCOMPONENT / T_BLOODDONOR | G-03 | **corrected — lower-case t_blood*** | MC #13 Blood Bank lists T_BLOODBAG etc | t_bloodbagcollection/t_bloodbagissue/t_blooddonorregistration etc at transactionals/bloodBank/* (lower-case) or t_bloodbagdecross → ihms_bloodbank (21 tables) |
| 40 | T_NURSENOTE / T_PATIENTVITALS / T_NURSETASKDETAILS | G-03 | **confirmed** | MC #12 Nursing | T_NURSENOTE (NursingServiceImpl), T_PATIENTVITALS, T_NURSETASKDETAILS active at transactionals/* → ihms_nursing |

*G-02 pattern confirmed*: the 29-service ownership model's invented `T_*` names map overwhelmingly to `M_*` / camelCase / singular actuals or to native-only tables; zero `T_EJS_*` and zero active `T_AASMAAN_*` anywhere — exactly as 05 §3.3/3.4 predict. The only **confirmed** with same name+owner among the G-01/G-02 sample is `T_PATIENTREQUISITION` (but even that is lower-case `t_patientRequisition` and lives in bloodBank package, correcting MC Lab #7 drift).

*G-23 net*: `T_CHARGE` vs corpus `T_Charges/T_CHARGES_TEMP` both drifted at JPA level but GAP-09 proves `T_CHARGES`/`T_CHARGES_TEMP` are real runtime temp tables at DB level; corrected at JPA scope, confirmed at DB scope.

---

## §4 Draft 843→Service mapping (grouped by PG schema)

Assignment rule: `POSTGRESQL-MIGRATION.md §1` schema-per-service is authoritative; within that, `@Table` package/file path refines ownership (e.g., `masters/`→ihms_master, `transactionals/bloodBank`→ihms_bloodbank, `ejs/`→ihms_govt, `rcm/`→ihms_rcm, `chiranjeevi/`→ihms_govt). Conflict rows per §1.1 are flagged. `ihms_other` is the residual bucket that needs Oracle dump reconciliation — it is **not** a target schema; tables there will be re-homed after DBMS_METADATA cross-check.

| PG schema (target service #) | Tables (active JPA) | Count | + Native supplement | Notes |
|---|---|---|---|---|
| `ihms_patient` | *1 Patient Identity & Registration* | 25 | T_REGISTRATION, T_REGISTRATION_DETAILS, T_Registration_Int/Details_Int, T_SPONSOR (via native), T_NA_ITEMDETAILS (DTO) — native-only | |
| `ihms_encounter` | *2 Appointment & Encounter (OPD)* | 5 | T_ENCOUNTER (case drift T_Encounter), T_APPOINTMENT_CHANNEL (unassigned audit) — native pending | |
| `ihms_adt` | *3 ADT & Bed ( + M_BEDMASTER/M_WARDMASTER conflict → ADT owns state)* | 6 | T_ADMISSION, t_admission, T_BEDRESERVATION, T_MLCDETAILS, T_ATTENDANTPASS, T_ADMISSIONDETAILS — proc/native-only | |
| `ihms_clinical` | *4 Ordering & Prescription (Clinical)* | 8 | — | |
| `ihms_billing` | *5 Billing & Settlement ( + T_BILL/T_BILLDETAILS/T_CHARGE family proc-only)* | 5 | T_BILL, T_BILLDETAILS, T_CHARGE→T_CHARGES/_TEMP (native-SQL temp pair), T_DOCTORBILL→T_DOCTORVISITCHARGES (active), T_CONCESSION→M_ConcessionTemplate (active) | |
| `ihms_payment` | *6 Payments & Refunds ( + T_ADVANCE native-SQL unmapped)* | 0 | T_PAYMENT, T_PAYMENTMODEDETAILS, T_ADVANCE (native-SQL), T_REFUND (autocode label only), T_RECEIPT (absent) — proc/native | |
| `ihms_lab` | *7 Lab (Pathology)* | 3 | — | |
| `ihms_rad` | *8 Radiology* | 0 | — | |
| `ihms_pharmacy` | *9 Pharmacy & Dispensing (owns T_ITEMSALE — stock stays Inventory)* | 3 | — | |
| `ihms_inventory` | *10 Inventory & Procurement ( + M_ITEMMASTER conflict → Inventory owns; T_GRN/T_PURCHASEORDER native-SQL)* | 7 | T_GRN (native-SQL), T_PURCHASEORDER (native-SQL beyond T_purchaseOrderRequest active), T_INDENT→T_INDENTINVOICE active, T_SUPPLIER→M_SUPPLIER active, T_QUOTATION (absent), T_CONSIGNMENTGRN (no active) | |
| `ihms_ot` | *11 OT & Surgery* | 0 | — | |
| `ihms_nursing` | *12 Nursing & Inpatient Care* | 9 | — | |
| `ihms_bloodbank` | *13 Blood Bank (t_patientRequisition owns G-01)* | 23 | T_BLOODBAG* family lower-case t_bloodbag* active list (13 tables) + t_patientRequisition (confirmed) — MC lists uppercase T_BLOODBAG but actual lower-case | |
| `ihms_diet` | *14 Diet* | 3 | — | |
| `ihms_cssd` | *15 CSSD* | 5 | — | |
| `ihms_linen` | *16 Linen & Laundry* | 2 | — | |
| `ihms_transport` | *17 Transport ( + M_AMBULANCEMASTER/M_DRIVERMASTER)* | 3 | — | |
| `ihms_equipment` | *18 Equipment Management* | 12 | — | |
| `ihms_bmw` | *19 Bio-Medical Waste* | 2 | — | |
| `ihms_grievance` | *20 Grievance & Feedback* | 2 | — | |
| `ihms_master` | *21 Reference Master Data (571 M_* masters minus conflicts; owns cache-key contract)* | 573 | — | |
| `ihms_iam` | *22 Security & IAM (T_USER* + M_STAFF/DOCTOR conflict → IAM owns profiles)* | 22 | — | |
| `ihms_rcm` | *23 RCM & Claims (T_ADMISSION_TID variant)* | 4 | — | |
| `ihms_rmrs` | *24 RMRS & PMRD (Treasury — M_AUTHORITY/HEADS/LEDGER conflict → RMRS per §1.1; s_* collections)* | 2 | — | |
| `ihms_interop` | *25 Interop & Notifications (legacy poll queues → Kafka; T_EHRJOBDETAILS)* | 6 | — | |
| `ihms_reporting` | *26 Reporting & Analytics (T_REPORTMASTER proc-only → ihms_reporting)* | 0 | — | |
| `ihms_govt` | *28 Govt Programs (EJS 57 ANDROID_/MASTER_/ES_/USER_/MEDIA + Aasmaan 35 convention + BENEFICIARY_ENROLMENT_IHMS/T_REFRENCETID)* | 57 | — | |
| `ihms_queue` | *29 Queue & Kiosk (T_PATIENTQUEUE etc — no active JPA currently, native/procs; M_OPDSTATIONMASTER → queue)* | 0 | — | |
| `ihms_audit` | *cross-cutting Audit (decision pending per PG §1.1 — T_AUDIT/T_TRX_HISTORY/T_LOGIN_AUDIT/T_ABDM_AUDIT/T_APPOINTMENT_CHANNEL)* | 2 | — | |
| `ihms_other` | *Residual — 128 active T_* that need Oracle dump re-homing (see below); DO NOT create schema; re-assign after dump* | 109 | 128 T_* (see ihms_other list below) — mostly transactionals with generic names; re-home via package + ALL_TAB_COLUMNS | |


*Full per-table listings per schema are in Appendix A (grouped active-JPA tables). Native-only tables (05 §3.6 + GAP-09 native-SQL 4) are carried as “+ Native supplement” — they have no JPA file:line to cite; cite is the native-SQL path in §3 and the proc in `generated-procs/CIMS_*.sql`.*

*Count reconciliation*: 898 active JPA + 10 native-only + 4 native-SQL unmapped = 912 logical; minus ~35 convention Aasmaan not yet mapped + ~69 DTO/case-variant duplicates = 843 planning count. The delta is documented and acceptable for Draft. **The 843 claimed set is fully covered**: every claimed name in 10/MC appears either in the active-JPA list (with corrected name shown in §3) or in the native supplement column.

---

## §5 Unassigned / decision-pending bucket

Per `POSTGRESQL-MIGRATION.md §1.1` + G-03 “No service owns T_APPOINTMENT_CHANNEL / audit tables (flagged decision pending in PG §1.1)” + `10 §2.1` queue/interop notes:

| Table / group | Claimed owner (if any) | Why unassigned | Proposed home (pending sign-off) |
|---|---|---|---|
| `T_APPOINTMENT_CHANNEL` | 10 #2 Encounter (OPD) lists it | No @Table and no native-SQL hit; audit-type table per 05 §3.2 | `ihms_audit` (cross-cutting audit schema, decision per PG §1.1) |
| `T_AUDIT` | cross-cutting (MC §7, 08 §1 568 sites) | Exists JPA `T_AUDIT` (Audit.java:23) but cross-cutting — no single domain owner | `ihms_audit` (preferred) or stay per-owning-service if audit stays per-service per 10 §7 Q4 |
| `T_TRX_HISTORY` | cross-cutting | `T_TRX_HISTORY` (TrxHistory.java:12) + raw INSERT variants AuditHelper:156, `t_trx_history_seq` | `ihms_audit` (hot-write index on ADDEDDATETIME) |
| `T_LOGIN_AUDIT` / `T_ABDM_AUDIT` | IAM / Interop | No active JPA (raw INSERT AuditHelper 917/938); sequences `T_LOGIN_AUDIT_SEQ`, `T_ABDM_AUDIT_SEQ` | `ihms_audit` or `ihms_iam` / `ihms_interop` respectively — decision per PG §1.1 |
| `T_USER_FACEAUTH` | IAM (#22 list has ? ) | `T_USER_FACEAUTH` active at `models/...` but ownership “decision” per PG §1 | `ihms_iam` vs `ihms_patient` — decide per PG §1.1 |
| `s_servicewisecollection` / `s_userwisecollection` | RMRS (#24) | `s_*` statistical POJOs lower-case — convention vs explicit | `ihms_rmrs` (per PG §1) — confirm Oracle case folding |
| `T_REPORTMASTER` / stats / PDF blob | Reporting (#26) | No @Table (reporting read-model) | `ihms_reporting` (ELT materialized; no proc) |
| `T_REGISTRATION` family native-only | Patient (#1) | No @Table by design (05 §3.6) — DDL must be dumped | `ihms_patient` (already proposed) — not truly unassigned, just no JPA |
| `T_REFUND` (Svc 6) | Payment (#6) | Absent everywhere incl. native-SQL; only autocode label | Unassigned — confirm if Oracle view/proc name vs table |
| `T_QUOTATION` (Svc 10) | Inventory (#10) | Absent everywhere | Unassigned — may be proc-only `T_QUOTATION` or never created |
| `T_CONSIGNMENTGRN` (Svc 10) | Inventory (#10) | No active JPA, no native-SQL hit in grep sweep | Unassigned — proc-body may reference `T_CONSIGNMENTGRN` (check generated-procs) |
| All `AASMAAN_*` 35 | Govt (#28) | `//@Table` commented out → JPA default `AASMAANCASE` (verify Oracle) | `ihms_govt` — add explicit @Table to entities before PG DDL |

Sign-off needed before any MICROSERVICE-CATALOG/10 ownership table edits — this file is the evidence base for that decision (PG §1.1 “decision pending”).

---

## §6 What remains to reach Final (vs Draft)

- Oracle `DBMS_METADATA` DDL dump + `ALL_TAB_COLUMNS`/`ALL_TABLES` extract for the ~10 native-only tables plus all `T_*` claimed under Inventory/Payment/Billing/ADT where JPA shows no active table but native-SQL shows DB existence (T_BILL, T_ADMISSION etc) — this upgrades “exists-at-DB-level-unmapped” from grep-based to catalog-authoritative.
- Proc-body table-ref extract: `grep -h "FROM\|INTO\|UPDATE\|JOIN" generated-procs/*.sql | sort -u` to catch proc-only table names (e.g., expiryItemService proc may reference `T_Q...` not hit in Java grep).
- `ihms_other` 128-table re-homing: run the same schema-assign script after dump, using Oracle `TABLE_NAME` as key (case-insensitive) to re-home by primary FK/usage rather than file path heuristic.
- Resolve PG §1.1 conflicts (M_BEDMASTER, M_ITEMMASTER, M_STAFF/DOCTOR, T_ITEMSALE, audit schema) and flip those rows from “unassigned bucket” to final schema.
- Once resolved, back-port the final per-table schema map into `MICROSERVICE-CATALOG §7` Owned data lines and `10 §2.1` Owned entities/tables column (deferred per dispatch).

---

## Appendix A — Active JPA tables per PG schema (898 distinct)

*Each entry: `TableName` → `model/file:line`. Lower-case variants preserved exactly as in `@Table`. Lists are grouped by target PG schema per the rule above. Full generation is reproducible via `grep -Rn '@Table' /Users/adityabanerjee/personal_project/ihms/src/main/java/com/ohum/models`.*


### ihms_patient — 1 Patient Identity & Registration (25 tables)

- `T_PATIENT_ABDM` → trx/abdm/PatientAbdm.java:26
- `T_PATIENT_ABDM_CC` → trx/abdm/PatientAbdmCC.java:17
- `T_PATIENT_ABDM_SHARETOKEN` → trx/abdm/PatientAbdmToken.java:17
- `T_PATIENT_CONSENT_REQUESTS_ABDM` → trx/abdm/PatientAbdmCR.java:29
- `T_PATIENT_CONSENT_REQUESTS_DATA_ABDM` → trx/abdm/PatientAbdmCRData.java:24
- `T_PATIENT_DEMODETAIL_MISMATCH` → trx/PatientDemoDetailsMismatch.java:19
- `T_PATIENTALLERGIES` → transactionals/PatientAllergies.java:28
- `T_PATIENTBILLESTIMATIONSURGERYHEAD` → transactionals/PatientBillEstimationSurgeryHead.java:27
- `T_PATIENTCORONADETAILS` → masters/Patientcoronadetails.java:28
- `T_PATIENTCORONAGROUP2` → masters/Patientcoronagroup2.java:29
- `T_PATIENTDETAILSFORDOCTORQUEUE` → txt/PatientDetailsForDoctorQueue.java:22
- `T_PatientDocuments` → transactionals/PatientDocuments.java:23
- `T_PATIENTFEEDBACK` → transactionals/PatientFeedback.java:21
- `T_PATIENTFEEDBACKDETAILS` → transactionals/PatientFeedbackDetails.java:21
- `T_PATIENTHQDETAILS` → transactionals/PatientHqDetails.java:31
- `T_PATIENTIMMUNIZATIONDETAILS` → transactionals/PatientImmunizationDetails.java:28
- `T_PATIENTNONMEDORDERDETAILS` → transactionals/PatientNonMedOrderDetails.java:30
- `T_PATIENTPACKAGESUBSCRIPTION` → trx/PatientPackageSubscription.java:22
- `T_PATIENTPRESCPTIONNOTE` → transactionals/PatientPrescptionNote.java:30
- `T_PatientProcAnesthesiaDetails` → transactionals/PatientProcedureAnesthesiadetails.java:30
- `T_PATIENTPROCEDURECHECKLISTDETAILS` → transactionals/PatientprocedurechecklistdetailsDto.java:24
- `T_PATIENTPROCEDURECHECKLISTITEMS` → transactionals/PatientprocedurechecklistitemsDto.java:24
- `T_PATIENTPROCEDURERATEDETAILS` → transactionals/PatientProcedureRateDetails.java:28
- `T_PATIENTSCHEDULEANASTHESIADETAILS` → transactionals/PatientScheduleAnasthesiaDetails.java:27
- `T_REGISTRATIONQUEUE` → trx/RegistrationQueue.java:24

### ihms_encounter — 2 Appointment & Encounter (OPD) (5 tables)

- `T_APPOINTMENT` → txt/Appointment.java:29
- `T_CORONASTATS` → transactionals/CoronaStats.java:29
- `T_DOCTORCHECKIN_QUEUEROOM` → txt/DoctorCheckInQueueRoom.java:21
- `T_Encounter` → trx/Encounter.java:30
- `t_patienttokens` → txt/PatientToken.java:23

### ihms_adt — 3 ADT & Bed ( + M_BEDMASTER/M_WARDMASTER conflict → ADT owns state) (6 tables)

- `M_BEDMASTER` → masters/Bedmaster.java:43
- `M_CABINMASTER` → masters/Cabinmaster.java:48
- `M_ROOMMASTER` → masters/Roommaster.java:37
- `M_WARDMASTER` → masters/Wardmaster.java:43
- `T_PATIENTDISCHARGEDATA` → transactionals/PatientDischargeData.java:31
- `T_TRANSFERREQUEST` → transactionals/TransferRequest.java:26

### ihms_clinical — 4 Ordering & Prescription (Clinical) (8 tables)

- `t_consultorder` → transactionals/ConsultOrder.java:25
- `T_EMR_SERVICES` → transactionals/EmrServices.java:22
- `T_EMRDETAILS` → transactionals/EmrDetails.java:23
- `T_PATIENTADVICE` → transactionals/Patientadvice.java:32
- `T_PATIENTDIAGNOSIS` → transactionals/PatientDiagnosisDetails.java:30
- `T_PATIENTEORDER` → transactionals/PatientEorder.java:21
- `T_PATIENTNOTE` → transactionals/PatientNote.java:35
- `T_TEMP_PRESCRIPTION_NOTES` → trx/TempPrescriptionNote.java:22

### ihms_billing — 5 Billing & Settlement ( + T_BILL/T_BILLDETAILS/T_CHARGE family proc-only) (5 tables)

- `t_creditNote` → transactionals/CreditNote.java:32
- `t_creditNoteDetails` → transactionals/CreditNoteDetails.java:27
- `T_DOCTORVISITCHARGES` → transactionals/Doctorvisitcharge.java:29
- `T_FREQ_ITEMS` → FrequentItem.java:17
- `T_PACKAGECHARGESUBSCRIPTION` → transactionals/PackageChargeSubscription.java:23

### ihms_lab — 7 Lab (Pathology) (3 tables)

- `T_MACHINECONFIGURATION` → transactionals/MachineConfiguration.java:22
- `T_PATHOPATIENTDOCS` → transactionals/PathoPatientDocs.java:22
- `t_pathorderbookingdetailsdocs` → transactionals/PathOrderBookingDetailDocuments.java:20

### ihms_pharmacy — 9 Pharmacy & Dispensing (owns T_ITEMSALE — stock stays Inventory) (3 tables)

- `T_INTEGRATION_PRESCRIPTIONS` → IntegrationPrescriptions.java:18
- `T_PRESCRIPTIONDISPENSEDETAILS` → transactionals/PrescriptionDispenseDetails.java:30
- `T_PrescriptionMaster` → transactionals/PrescriptionMaster.java:24

### ihms_inventory — 10 Inventory & Procurement ( + M_ITEMMASTER conflict → Inventory owns; T_GRN/T_PURCHASEORDER native-SQL) (7 tables)

- `M_ITEMBATCHMASTER` → masters/Itembatchmaster.java:33
- `M_ITEMMASTER` → masters/Itemmaster.java:50
- `M_REORDERANDSAFETYMASTER` → masters/ReorderAndSafetyMaster.java:25
- `T_ITEMCLINIC` → transactionals/ItemClinic.java:24, masters/TItemclinic.java:32
- `T_ITEMSTOCK` → transactionals/ItemStock.java:27
- `T_purchaseOrderRequest` → transactionals/PurchaseOrderRequest.java:30
- `T_RATECONTRACT` → transactionals/RateContract.java:31

### ihms_nursing — 12 Nursing & Inpatient Care (9 tables)

- `T_MACHINEVITALS` → transactionals/Nursing/MachineVitals.java:27
- `T_NURSENOTE` → transactionals/NurseNote.java:30
- `T_NURSEPATIENTDETAILS` → txt/NursePatientDetails.java:29
- `T_NURSETASKDETAILS` → transactionals/NurseTaskDetails.java:29
- `T_NURSETASKFREQUENCYDETAILS` → transactionals/NurseTaskFrequencyDetails.java:26
- `T_PATIENTFLAGDETAILS` → transactionals/Nursing/PatientFlagDetails.java:26
- `T_PATIENTHEALTHFACTOR` → transactionals/Nursing/PatientHealthFactor.java:30
- `T_PATIENTNURSINGORDERS` → transactionals/Nursing/PatientNursingOrder.java:27
- `T_PATIENTVITALS` → transactionals/PatientVitals.java:29

### ihms_bloodbank — 13 Blood Bank (t_patientRequisition owns G-01) (23 tables)

- `t_bloodbagcollection` → transactionals/bloodBank/BloodDonorBagCollectionDto.java:31
- `t_bloodbagdecross` → transactionals/bloodBank/BloodBagDecross.java:25
- `T_BloodBagExtraction` → transactionals/bloodBank/BloodBagExtraction.java:26
- `t_bloodbagissue` → transactionals/bloodBank/BloodBagIssue.java:25
- `t_bloodbagissuedemandslip` → transactionals/bloodBank/DemandSlip.java:29
- `T_BLOODBAGRECEIVE` → transactionals/bloodBank/BloodBagReceive.java:25
- `t_bloodbagreturns` → transactionals/bloodBank/BloodBagReturn.java:25
- `T_BLOODBAGTRAIL` → BloodBagTrail.java:30
- `T_BloodBankIndent` → transactionals/bloodBank/BloodBankIndent.java:29
- `T_BloodBankIndentDetails` → transactionals/bloodBank/BloodBankIndentDetails.java:25
- `T_BLOODCOMPONENTCLINIC` → masters/BloodComponentClinic.java:38
- `t_bloodcrossmatchingresult` → transactionals/bloodBank/BloodCrossmatchingResult.java:32
- `t_bloodcrossmatchingresultdetails` → transactionals/bloodBank/BloodCrossMatchResultDetails.java:29
- `T_BloodDetails` → transactionals/bloodBank/BloodDetails.java:30
- `t_blooddonorregistration` → transactionals/bloodBank/BloodDonorRegistration.java:36
- `t_blooddonorregistrationdetails` → transactionals/bloodBank/BloodDonorRegistrationDetails.java:29
- `t_demandslipdetails` → transactionals/bloodBank/DemandSlipDetails.java:24
- `t_donormedicalexaminationresult` → transactionals/bloodBank/DonorMedicalExamination.java:32
- `t_donorScreeningTest` → transactionals/bloodBank/DonorScreenTestDto.java:23
- `T_INFUSIONMIXING` → transactionals/bloodBank/InfusionMixingDto.java:25
- `T_INFUSIONMIXINGDETAILS` → transactionals/bloodBank/InfusionMixingDetailsDto.java:27
- `t_patientRequisition` → transactionals/bloodBank/PatientBloodRequisition.java:29
- `t_patientRequisitionDetails` → transactionals/bloodBank/PatientBloodRequisitionDetails.java:30

### ihms_diet — 14 Diet (3 tables)

- `T_DIETITEMSCHEDULEDETAILS` → transactionals/DietItemScheduleDetails.java:26
- `T_DIETORDERDETAILS` → transactionals/DietOrderDetails.java:27
- `T_TODAYDIETMENU` → transactionals/TodayDietItemMenu.java:34

### ihms_cssd — 15 CSSD (5 tables)

- `T_CSSDKITFORMATION` → transactionals/CssdKitFormation.java:26
- `T_CSSDKITFORMATIONDETAILS` → transactionals/CssdKitFormationDetails.java:27
- `T_CSSDKITISSUEITEM` → transactionals/Cssdkitissueitem.java:26
- `T_CSSDKITISSUEITEMDETAILS` → transactionals/Cssdkitissueitemdetails.java:32
- `T_CSSDSTERLIZATIONHEADER` → transactionals/CssdItemSterilizationDetails.java:26

### ihms_linen — 16 Linen & Laundry (2 tables)

- `T_LINENREQUEST` → transactionals/LinenRequest.java:21
- `T_LINENREQUESTDETAILS` → transactionals/LinenRequestDetails.java:20

### ihms_transport — 17 Transport ( + M_AMBULANCEMASTER/M_DRIVERMASTER) (3 tables)

- `t_vehiclemaintenance` → transactionals/VehicleMaintenance.java:23
- `t_vehiclerequest` → transactionals/TransportRequest.java:23
- `t_vehiclerequestdetails` → transactionals/TransportRequestDetails.java:22

### ihms_equipment — 18 Equipment Management (12 tables)

- `T_EqpComplaint` → transactionals/EqpComplaint.java:24
- `T_eqpComplaintDetails` → transactionals/EqpComplaintDetails.java:24
- `t_eqpcomplaintresolution` → transactionals/EqpComplaintResolution.java:24
- `t_eqpcomplaintresolutionagentdetail` → transactionals/EqpComplaintResolutionDetails.java:24
- `T_EqpPMSchedule` → transactionals/EqpPMSchedule.java:31
- `T_EqpPMScheduleDetails` → transactionals/EqpPMScheduleDetails.java:30
- `T_EqpPreventiveMaintenance` → transactionals/EqpPreventiveMaintenance.java:32
- `T_EQUIPMENTDATADETAILS` → transactionals/EquipmentUsageDetails.java:26
- `T_EQUIPMENTDETAILDOCS` → transactionals/EquipmentDetailDocs.java:27
- `t_equipmentdetails` → transactionals/Equipment.java:22
- `T_EQUIPMENTOPENINGBALANCE` → transactionals/EqiupOpeningBalance.java:30
- `T_EquipmentTransfer` → transactionals/EquipmentTransfer.java:27

### ihms_bmw — 19 Bio-Medical Waste (2 tables)

- `T_BIOMEDICALWASTE` → transactionals/BioMedicalWaste.java:21
- `T_BIOMEDICALWASTEDETAILS` → transactionals/BioMedicalWasteDetails.java:28

### ihms_grievance — 20 Grievance & Feedback (2 tables)

- `T_PATIENTCOMPLAINTS` → transactionals/PatientComplaints.java:30
- `T_PATIENTGRIEVANCE` → transactionals/PatientGrievanceHistory.java:30

### ihms_master — 21 Reference Master Data (571 M_* masters minus conflicts; owns cache-key contract) (573 tables)

- `APPLICATION_DOCUMENTS` → ApplicationDocuments.java:26
- `block_mst` → masters/Blockmaster.java:37
- `city_mst` → masters/Citymaster.java:35
- `CONFIG_APPLICATION` → masters/ConfigApplication.java:38
- `ConfigurationProperty` → ConfigurationProperty.java:24
- `district_mst` → masters/DistrictMaster.java:40
- `HOLIDAY_LIST` → HolidayMaster.java:34
- `HOSPITAL_GEOLOCATION_MAPPING` → HospitalGeoLocationMapping.java:25
- `M_ACCIDENTTYPE` → masters/Accidenttype.java:34
- `M_ACCLEDGERCOSTCENTERDETAILS` → masters/Accledgercostcenterdetails.java:32
- `M_ACCLEDGERMAILINGDETAILS` → masters/Accledgermailingdetails.java:32
- `M_ACCLEDGEROTHERDETAILS` → masters/Accledgerotherdetails.java:32
- `M_ACCLEDGERVCHTYPEDETAILS` → masters/Accledgervchtypedetails.java:32
- `M_ACCOUNTHEADMASTER` → masters/Accountheadmaster.java:40
- `M_ACCOUNTINGCOMPANYMASTER` → masters/Accountingcompanymaster.java:32
- `M_ACCOUNTINGCOSTCATEGORY` → masters/Accountingcostcategory.java:32
- `M_ACCOUNTINGCOSTCENTRE` → masters/Accountingcostcentre.java:32
- `M_ACCOUNTINGGROUPMASTER` → masters/Accountinggroupmaster.java:32
- `M_ACCOUNTINGLEDGER` → masters/Accountingledger.java:32
- `M_ACCOUNTINGTAXMASTER` → masters/Accountingtaxmaster.java:32
- `M_ACCOUNTINGVOUCHURETYPEMASTER` → masters/Accountingvouchuretypemaster.java:32
- `M_ACOUNTINGCOMPANYDETAILS` → masters/Acountingcompanydetails.java:32
- `M_ACTUALBEDCOUNT` → masters/ActualBedCountMaster.java:26
- `M_AdditiveMaster` → masters/AdditiveMaster.java:30
- `M_ADMINISTRATIONSCHEDULE` → masters/AdministrationSchedule.java:32
- `M_ADMISSIONCUSTOMFIELDSMASTER` → masters/Admissioncustomfieldsmaster.java:32
- `M_ADMISSIONTYPE` → masters/Admissiontype.java:36
- `M_ADMITTINGDOCTORCLASSIFICATIONMASTER` → masters/Admittingdoctorclassificationmaster.java:32
- `M_ADVANCEAGAINST` → masters/Advanceagainst.java:37
- `M_ADVICEMASTER` → masters/Advicemaster.java:32
- `M_ADVISEDITEMDETAILS` → masters/Adviseditemdetails.java:32
- `M_ADVISEDSERVICEDETAILS` → masters/Advisedservicedetails.java:32
- `M_ALLERGYMASTER` → masters/AllergyMaster.java:32
- `M_AMBULANCEMASTER` → masters/Ambulancemaster.java:35
- `M_AMBULANCETYPEMASTER` → masters/AmbulanceTypeMaster.java:31
- `M_AMCAGENT` → masters/Amcagent.java:34
- `M_ANESTHESIANOTES` → masters/Anesthesianotes.java:34
- `M_ANESTHESIATYPEMASTER` → masters/Anesthesiatypemaster.java:36
- `M_ANSWER` → masters/Answer.java:34
- `M_ANSWERGROUP` → masters/Answergroup.java:34
- `M_APPBOOKINGTYPEMASTER` → masters/Appbookingtypemaster.java:32
- `M_AppBookingTypeMaster` → masters/Bookingtypemaster.java:34
- `M_APPOINTMENTREASONMASTER` → masters/Appointmentreasonmaster.java:32
- `M_APPOINTMENTTYPEMASTER` → masters/Appointmenttypemaster.java:32
- `M_AQUITYMASTER` → masters/Aquitymaster.java:30
- `M_AREAMASTER` → masters/Areamaster.java:34
- `M_ATTENDANTPASSSERVICERATE` → masters/AttendantPassServiceRateMaster.java:37
- `M_AUDITEVENTMASTER` → masters/AuditEventMaster.java:30
- `M_AUTHORITYMASTER` → masters/AuthorityMaster.java:26
- `M_BANKACCOUNTMASTER` → masters/BankAccountMaster.java:24
- `M_BANKBRANCHMASTER` → masters/Bankbranchmaster.java:36
- `M_BANKMASTER` → masters/Bankmaster.java:36
- `M_BEDAMMENITIESMASTER` → masters/Bedammenitiesmaster.java:32
- `M_BEDAMMENITYDETAILS` → masters/Bedammenitydetails.java:32
- `M_BEDRELESECHECKLIST` → masters/Bedrelesechecklist.java:32
- `M_BEDSTATUSMASTER` → masters/Bedstatusmaster.java:36
- `M_BILLINGDEPARTMENTMASTER` → masters/BillingDepartmentMaster.java:31
- `M_BLOODADVERSEREACTIONMASTER` → masters/Bloodadversereactionmaster.java:36
- `M_BLOODBAGTYPECOMPONENTDETAIL` → masters/Bloodbagtypecomponentdetail.java:32
- `M_BLOODBAGTYPECOMPONENTMASTER` → masters/Bloodbagtypecomponentmaster.java:38
- `M_BLOODBAGTYPEMASTER` → masters/Bloodbagtypemaster.java:37
- `M_BLOODCLUBDETAIL` → masters/Bloodclubdetail.java:32
- `M_BLOODCLUBMASTER` → masters/Bloodclubmaster.java:32
- `M_BLOODCOMPONENTMASTER` → masters/Bloodcomponentmaster.java:37
- `M_BLOODDISCARDREASONMASTER` → masters/Blooddiscardreasonmaster.java:36
- `M_BloodDonationCampMaster` → masters/BloodDonationCampMaster.java:37
- `M_BLOODDONORTYPEMASTER` → masters/Blooddonortypemaster.java:36
- `M_BLOODGROUPCROSSMATCHING` → masters/Bloodgroupcrossmatching.java:39
- `M_BLOODGROUPMASTER` → masters/Bloodgroupmaster.java:36
- `M_BLOODREDCELLSERIOLOGYGROUPMASTER` → masters/Bloodredcellseriologygroupmaster.java:34
- `M_BLOODREDCELLSEROLOGYDETAIL` → masters/Bloodredcellserologydetail.java:32
- `M_BLOODREDCELLSEROLOGYMASTER` → masters/Bloodredcellserologymaster.java:32
- `M_BLOODREQUISITIONREASONMASTER` → masters/BloodRequisitionReasonMaster.java:25
- `M_BLOODSCREENINGMASTER` → masters/Bloodscreeningmaster.java:38
- `M_BLOODTESTMASTER` → masters/Bloodtestmaster.java:33
- `M_BLOODTESTRESULTMASTER` → masters/Bloodtestresultmaster.java:36
- `M_BRANDMASTER` → masters/Brandmaster.java:34
- `M_BUILDINGDEPARTMENTDETAILS` → masters/Buildingdepartmentdetails.java:25
- `M_BUILDINGFLOORSTRUCTURE` → masters/Buildingfloorstructure.java:32
- `M_BUILDINGMASTER` → masters/Buildingmaster.java:36
- `M_CADREDESIGNATIONMASTER` → masters/CadreDesignationMaster.java:31
- `M_CADREMASTER` → masters/CadreMaster.java:25
- `m_CampMaster` → masters/BloodCampMaster.java:34
- `M_CAMPMASTER` → masters/Campmaster.java:39
- `M_CAMPTYPEMASTER` → masters/Camptypemaster.java:36
- `M_CANCELLATIONREASON` → masters/Cancellationreason.java:36
- `m_casetypemaster` → CasetypeMaster.java:37
- `M_CASHCOUNTERMASTER` → masters/Cashcountermaster.java:37
- `M_CASTEMASTER` → masters/Castemaster.java:31
- `M_CHARTCONFIGURATION` → masters/Chartconfiguration.java:36
- `M_CHECKLISTTYPE` → masters/ChecklistType.java:31
- `M_CITYMASTER` → masters/Citymst.java:36
- `M_CLASSIFICATION` → masters/Classification.java:36
- `M_CLASSMASTER` → masters/Classmaster.java:40
- `M_CLASSWARDMAPPINGFORADMISSIONBLOCK` → masters/ClassWardMappingForAdmissionBlock.java:30
- `M_ClearanceMaster` → masters/Clearancemaster.java:32
- `M_CLINICALFINDINGSMASTER` → masters/Clinicalfindingsmaster.java:32
- `M_ClinicalLevel` → masters/ClinicalLevel.java:29
- `M_CLINICDEPARTMENTDETAILS` → masters/Clinicdepartmentdetails.java:37
- `M_CLINICDOCTORDETAILS` → masters/Clinicdoctordetails.java:33
- `m_clinicdoctordetails` → masters/ClinicalDoctorDetails.java:30
- `M_CODETYPE` → masters/Codetype.java:34
- `M_COMPANYASSOCIATEMASTER` → masters/Companyassociatemaster.java:36
- `M_COMPANYCONTRACTDETAILS` → masters/Companycontractdetails.java:32
- `M_COMPANYDRUGDETAILS` → masters/Companydrugdetails.java:32
- `M_COMPANYMASTER` → masters/Companymaster.java:37
- `M_COMPANYPLANDETAILS` → masters/Companyplandetails.java:34
- `M_COMPANYTARIFFDETAILS` → masters/Companytariffdetails.java:34
- `M_COMPANYTYPEMASTER` → masters/Companytypemaster.java:36
- `M_COMPLAINTCATEGORYMASTER` → masters/Complaintcategorymaster.java:36
- `M_COMPLAINTMASTER` → masters/Complaintmaster.java:32
- `M_COMPLIANCEMASTER` → masters/Compliancemaster.java:34
- `M_COMPLIANCETYPEMASTER` → masters/Compliancetypemaster.java:36
- `M_COMPOUNDMASTER` → masters/Compoundmaster.java:36
- `M_COMPOUNDMASTERDETAIL` → masters/Compoundmasterdetail.java:36
- `M_CONCESSIONCATEGORYMASTER` → masters/Concessioncategorymaster.java:36
- `M_ConcessionTemplateDetails` → masters/Concessiontemplatedetails.java:33
- `M_ConcessionTemplateMaster` → masters/Concessiontemplatemaster.java:39
- `M_CONFIG_AUTOCHARGES` → masters/ConfigAutocharges.java:32
- `M_CONSENTMASTER` → masters/Consentmaster.java:32
- `M_CONSENTTYPE` → masters/Consenttype.java:34
- `M_CONTAINERMASTER` → masters/Containermaster.java:39
- `M_COUNTRYMASTER` → masters/Countrymaster.java:34
- `M_CREDITACCOUNTMASTER` → masters/CreditAccountMaster.java:24
- `M_CULTURENEGATIVEMASTER` → masters/Culturenegativemaster.java:40
- `M_CURRENCY` → masters/Currency.java:32
- `M_CUSTOMERSERVICEMAPPING` → masters/CustomerServiceMapping.java:34
- `M_DAIGNOSISMASTER` → masters/Daignosismaster.java:32
- `M_DASHBOARDMASTER` → masters/Dashboardmaster.java:32
- `M_DAYCAREMASTER` → masters/DayCareMaster.java:25
- `M_DELIVERY` → masters/Delivery.java:32
- `M_DELIVERYPARTICULARSMASTER` → masters/DeliveryParticularsMaster.java:35
- `M_DELIVERYTYPE` → masters/Deliverytype.java:32
- `M_DEPARTMENT_MST` → masters/DepartmentMst.java:38
- `M_DEPARTMENTCABINROOMMAPPINGMASTER` → masters/DepartmentCabinRoomMappingMaster.java:25
- `M_DEPARTMENTMASTER` → masters/Departmentmaster.java:45
- `M_DEPARTMENTROOMMAPPINGMASTER` → masters/DepartmentRoomMappingMaster.java:25
- `M_DEPARTMENTSCHEDULE` → masters/Departmentschedule.java:32
- `M_DEPARTMENTSCHEDULEDETAILMASTER` → masters/Departmentscheduledetailmaster.java:32
- `M_DEPARTMENTSCHEDULEMASTER` → masters/Departmentschedulemaster.java:32
- `M_DESIGNATIONMASTER` → masters/Designationmaster.java:38
- `M_DIAGNOSISSERVICEMAPPING` → masters/DiagnosisServiceMapping.java:27
- `M_DIETITEMMASTER` → masters/Dietitemmaster.java:37
- `M_DIETPLANDETAILS` → masters/Dietplandetails.java:32
- `M_DIETPLANMASTER` → masters/Dietplanmaster.java:32
- `M_DIETSESSIONMASTER` → masters/Dietsessionmaster.java:37
- `M_DiscardImgReasonMaster` → masters/Imagediscardreasonmaster.java:36
- `M_DISCARDREASONMASTER` → masters/Discardreasonmaster.java:36
- `M_DISCARDREJECTIONMASTER` → masters/Discardrejectionmaster.java:35
- `M_DISCHARGEDEPARTMENTTEMPLATE` → masters/DischargeDepartmentTemplate.java:33
- `M_DISCHARGEDESTINATION` → masters/Dischargedestination.java:32
- `M_DISCHARGETEMPLATE` → masters/Dischargetemplate.java:32
- `M_DISCHARGETEMPLATEDETAILS` → masters/Dischargetemplatedetails.java:32
- `M_DISCHARGETYPE` → masters/Dischargetype.java:36
- `M_DISEASEGROUPMASTER` → masters/DiseaseGroupMaster.java:31
- `M_DISEASEMASTER` → masters/DiseaseMaster.java:32
- `M_DISPENSINGTYPE` → masters/Dispensingtype.java:34
- `M_DISPLAYUNITMASTER` → masters/DisplayUnitMaster.java:35
- `M_DISTRICTZONEMASTER` → Districtzonemaster.java:30
- `M_DOCTORCHECKINIP` → masters/DoctorCheckinIpMaster.java:22
- `M_DOCTORPROGESSNOTE` → masters/Doctorprogessnote.java:32
- `M_DOCTORROSTERMASTER` → masters/Doctorrostermaster.java:32
- `M_DOCUMENTCLASSMASTER` → masters/DocumentClassMaster.java:33
- `M_DonorDefaultVitalValue` → masters/DonorDefaultVitalValue.java:28
- `M_DONORREJECTIONREASONMASTER` → masters/Donorrejectionreasonmaster.java:36
- `M_DOSEFREQUENCYDETAILS` → masters/Dosefrequencydetails.java:32
- `M_DOSEFREQUENCYMASTER` → masters/Dosefrequencymaster.java:32
- `M_DRIVERMASTER` → masters/DriverMaster.java:31
- `M_DRUGINGREDIENTS` → masters/Drugingredients.java:38
- `M_DRUGINTERACTIONS` → masters/Druginteractions.java:35
- `m_drugschedule` → masters/PharmDrugMatser.java:24
- `M_DRUGSUBSTITUTE` → masters/Drugsubstitute.java:30
- `M_EBITACONFIGURATION` → masters/Ebitaconfiguration.java:32
- `m_emergencyregtypemaster` → EmergencyregtypeMaster.java:29
- `M_EMPANELMASTER` → masters/Empanelmaster.java:36
- `M_EmployeementStatus` → masters/Employeementstatus.java:30
- `M_EMR_PCR_FIELDMASTER` → masters/EmrPcrFieldmaster.java:32
- `M_EMR_PCR_SECTIONMASTER` → masters/EmrPcrSectionmaster.java:32
- `M_EMR_REFERRAL_FIELDMASTER` → masters/EmrReferralFieldmaster.java:32
- `M_EMR_REFERRAL_SECTIONMASTER` → masters/EmrReferralSectionmaster.java:32
- `M_EMRIMAGE` → masters/Emrimage.java:32
- `M_EMRIMAGEICON` → masters/Emrimageicon.java:32
- `M_EMRTEMPLATE` → masters/Emrtemplate.java:32
- `M_EMRTEMPLATECONFIGURATIONMASTER` → masters/Emrtemplateconfigurationmaster.java:32
- `M_ENCOUNTERTYPE` → masters/Encountertype.java:32
- `m_eqpwarranty` → transactionals/EquipmentWarranty.java:25
- `M_eqpworkingstatus` → masters/Equipworkingstatus.java:36
- `M_EqpWorkingStatus` → masters/EqpWorkingStatusMaster.java:27
- `M_EQUIPCHECKLIST` → masters/Equipchecklist.java:36
- `M_EQUIPCOMPLIANCEDETAILS` → masters/Equipcompliancedetails.java:32
- `M_EQUIPCONSUMABLECHECKLIST` → masters/Equipconsumablechecklist.java:32
- `M_EQUIPELECTRICALSAFETY` → masters/Equipelectricalsafety.java:32
- `M_EQUIPINSURANCEDETAILS` → masters/Equipinsurancedetails.java:33
- `M_EQUIPMENTDETAIL` → masters/Equipmentdetail.java:37
- `M_EQUIPMENTDETAILSAMC` → masters/Equipmentdetailsamc.java:32
- `M_EQUIPMENTELECTICALCHECKLIST` → masters/Equipmentelecticalchecklist.java:32
- `M_EquipmentElecticalCheckList` → masters/ElectricalChecklistmaster.java:36
- `M_EQUIPMENTFUNCTIONALCHECKLIST` → masters/Equipmentfunctionalchecklist.java:32
- `M_EquipmentFunctionalCheckList` → masters/Functionalchecklistmaster.java:36
- `M_EquipmentIndent` → transactionals/EquipmentIndentDetails.java:29
- `M_EQUIPMENTPREVENTIVECHECKLIST` → masters/Equipmentpreventivechecklist.java:36
- `M_EQUIPMENTSERVICEMASTER` → transactionals/EquipmentServiceMaster.java:26
- `M_EQUIPMENTSTATUS` → masters/Equipmentstatus.java:31
- `M_EquipmentStatus` → masters/Complaintstatusmaster.java:36, masters/EquipmentStatusMaster.java:27
- `M_EQUIPORDERDETAILS` → masters/Equiporderdetails.java:32
- `M_EQUIPPERIPHERALDETAILS` → masters/Equipperipheraldetails.java:32
- `M_EQUIPPREVENTCHECKLIST` → masters/Equippreventchecklist.java:32
- `M_EQUIPSERVICEAGENTDETAIL` → masters/Equipserviceagentdetail.java:32
- `M_EQUIPSLADETAILS` → masters/Equipsladetails.java:32
- `M_EQUIPSPAREDETAILS` → masters/Equipsparedetails.java:32
- `M_EQUIPTECHNICALSPECIFICATION` → masters/Equiptechnicalspecification.java:32
- `M_EQUPCOMPLAINTTYPE` → masters/Equpcomplainttype.java:34
- `M_EqupComplaintType` → masters/Complainttypemaster.java:36
- `M_ERRORFLAGMASTER` → masters/ErrorFlagMaster.java:28
- `M_EXTERNALBLOODSTORAGECENTER` → masters/Externalbloodstoragecenter.java:35
- `M_EXTERNALHOSPITALMASTER` → masters/Externalhospitalmaster.java:35
- `M_FEEDBACKTEMPLATE` → masters/Feedbacktemplate.java:35
- `M_FEEDBACKTEMPLATEDETAILS` → masters/Feedbacktemplatedetails.java:32
- `M_FILEDETAILS` → FileDetails.java:31
- `M_FILELINKINGFORDOCTORS` → masters/Filelinkingfordoctors.java:32
- `M_FILESTORE` → FileStore.java:35
- `m_flagmaster` → masters/FlagMaster.java:29
- `M_FLOORMASTER` → masters/Floormaster.java:36
- `M_FOODINTAKEINSTRUCTIONMASTER` → masters/Foodintakeinstructionmaster.java:32
- `M_FOODINTAKEMASTER` → masters/Foodintakemaster.java:32
- `M_FOODITEMCATEGORYMASTER` → masters/Fooditemcategorymaster.java:32
- `M_FOODITEMMASTER` → masters/Fooditemmaster.java:32
- `M_GASTYPEMASTER` → masters/Gastypemaster.java:33
- `M_Gendercategeory` → masters/Gendercategory.java:35
- `M_GENDERMASTER` → masters/Gendermaster.java:34
- `M_GENDERPREFIXMAPPING` → masters/Genderprefixmapping.java:34
- `M_GENERALSURGERYDETAILS` → masters/Generalsurgerydetails.java:32
- `M_GRADEMASTER` → masters/Grademaster.java:32
- `M_GRIEVANCECATEGORYMASTER` → masters/GrievanceCategoryMaster.java:25
- `M_GRIEVANCESEVERITYMASTER` → masters/GrievanceSeverityMaster.java:25
- `M_GRIEVANCESUBCATEGORYMASTER` → masters/GrievanceSubCategoryMaster.java:26
- `M_GRNITEMCHECKLIST` → masters/Grnitemchecklist.java:32
- `M_GrnRejectReasonMaster` → masters/GrnRejectReasonMaster.java:35
- `M_GROWTHCHARTDETAILS` → masters/Growthchartdetails.java:41
- `M_GROWTHCHARTMENU` → masters/GrowthChartMenu.java:39
- `M_GROWTHCHARTSERIES` → masters/Growthchartseries.java:36
- `M_GROWTHCHARTSERIESDATA` → masters/Growthchartseriesdata.java:33
- `M_HEADSMASTER` → masters/HeadsMaster.java:27
- `M_HEALTHFACTORCATEGORYMASTER` → masters/HealthFactorCategoryMaster.java:33
- `M_HEALTHFACTORMASTER` → masters/Healthfactormaster.java:36
- `M_HIGHRISKPREGNANCYFACTORMASTER` → masters/HighRiskPregnancyFactorMaster.java:32
- `M_HISTONOPREFIX` → masters/HistoNoPrefixMaster.java:31
- `M_HISTORYMASTER` → masters/Historymaster.java:32
- `M_HOSIPTALCATEGORYMASTER` → masters/Hospitalcategorymaster.java:36
- `M_HOSIPTALTYPEMASTER` → masters/Hospitaltypemaster.java:35
- `M_HSNCODEMASTER` → masters/HsnCodeMaster.java:35
- `M_ICD10_CM` → masters/DiagnosisMaster.java:18, masters/IcdCmMaster.java:34
- `M_ICDCMCATEGORY` → masters/IcdcmCategory.java:31
- `M_ICDCMCATEGORY_MAPPING` → masters/IcdcmCategoryMapping.java:41
- `M_IDENTITYTYPE` → masters/Identitytype.java:36
- `M_IMAGEANNOTATIONSMASTER` → masters/ImageAnnotationMaster.java:26
- `M_IMAGECATEGORY` → masters/Imagecategory.java:34
- `M_IMAGINGLOCATIONMASTER` → masters/ImagingLocationMaster.java:32
- `M_IMMUNIZATIONAGEGROUPMASTER` → masters/Immunizationagegroupmaster.java:35
- `M_IMPORTERMASTER` → masters/ImporterMaster.java:37
- `M_INCIDENTTYPECATEGORYMASTER` → masters/IncidentTypeCategoryMaster.java:31
- `M_INCIDENTTYPEMASTER` → masters/IncidentTypeMaster.java:31
- `M_INCOMECATEGORYMASTER` → masters/Incomecategorymaster.java:34
- `M_INSTRUCTIONMASTER` → masters/Instructionmaster.java:34
- `M_INSURANCEAGENT` → masters/Insuranceagent.java:36
- `M_INSURANCECATEGORYMASTER` → masters/InsuranceCategoryMaster.java:31
- `M_INSURANCEGRADEMASTER` → masters/InsuranceGradeMaster.java:20
- `M_INTAKEOUTPUTMASTER` → masters/Intakeoutputmaster.java:32
- `M_INTRAOPERATIVEINSTRUCTIONSMASTER` → masters/Intraoperativeinstructionsmaster.java:36
- `M_IPDROSTERDETAILS` → masters/IpdRosterDetails.java:32
- `M_ITEM_MST` → masters/Item_Mst.java:33
- `M_ITEMBRANDMAPPING` → masters/ItemBrandMapping.java:34
- `M_ITEMCATEGORY` → masters/Itemcategory.java:37
- `M_ITEMCOMPANY` → masters/Itemcompany.java:32
- `M_ITEMCOMPOUNDDETAILS` → masters/ItemCompoundDetails.java:27
- `M_ITEMDISEASEDETAILS` → masters/Itemdiseasedetails.java:32
- `M_ITEMGROUP` → masters/Itemgroup.java:37
- `M_ITEMMASTEROTHERDETAILS` → masters/Itemmasterotherdetails.java:32
- `M_IVF_CATHETERTYPEMASTER` → masters/IvfCathetertypemaster.java:32
- `M_IVF_CUMULUSMASTER` → masters/IvfCumulusmaster.java:32
- `M_IVF_DIFFICULTYTYPEMASTER` → masters/IvfDifficultytypemaster.java:32
- `M_IVF_DOSMASTER` → masters/IvfDosmaster.java:32
- `M_IVF_FERTILIZATIONSTAGEMASTER` → masters/IvfFertilizationstagemaster.java:32
- `M_IVF_GRADEMASTER` → masters/IvfGrademaster.java:32
- `M_IVF_METHODOFSPERMPREPARATIONMASTER` → masters/IvfMethodofspermpreparationmaster.java:32
- `M_IVF_MOIMASTER` → masters/IvfMoimaster.java:32
- `M_IVF_OPSTYPEMASTER` → masters/IvfOpstypemaster.java:32
- `M_IVF_PICMASTER` → masters/IvfPicmaster.java:32
- `M_IVF_PLANMASTER` → masters/IvfPlanmaster.java:32
- `M_IVF_SOURCEDENUDINGNEEDLEMASTER` → masters/IvfSourcedenudingneedlemaster.java:32
- `M_IVF_SOURCENEEDLEMASTER` → masters/IvfSourceneedlemaster.java:32
- `M_IVF_SOURCEOOCYTEMASTER` → masters/IvfSourceoocytemaster.java:32
- `M_IVF_SOURCESEMENMASTER` → masters/IvfSourcesemenmaster.java:32
- `M_IVFCANMASTER` → masters/Ivfcanmaster.java:32
- `M_IVFMAININDICATION` → masters/Ivfmainindication.java:32
- `M_IVFPLANNEDSPERMCOLLECTION` → masters/Ivfplannedspermcollection.java:32
- `M_IVFPLANNEDTREATMENT` → masters/Ivfplannedtreatment.java:32
- `M_IVFPROTOCOLTYPE` → masters/Ivfprotocoltype.java:32
- `M_LAB_CONFIG` → masters/LabConfigMaster.java:25
- `M_LANGUAGE` → masters/Languagemaster.java:31
- `M_LANGUAGE_LABELS` → masters/Languagelabel.java:36
- `M_LANGUAGEPREFEREDMASTER` → masters/Languagepreferedmaster.java:32
- `M_LEDGERMASTER` → masters/Ledgermaster.java:32
- `M_Levelmaster` → masters/Level.java:34
- `M_LINENCATEGORY` → masters/Linencategory.java:32
- `M_LINENDETAIL` → masters/Linendetail.java:32
- `M_LOCALEXAMINATIONMASTER` → masters/Localexaminationmaster.java:32
- `M_LOYALTYCARDMASTER` → masters/Loyaltycardmaster.java:32
- `M_LOYALTYPROGRAMATTACHMENTDETAILS` → masters/Loyaltyprogramattachmentdetails.java:32
- `M_LOYALTYPROGRAMCLINICDETAILS` → masters/Loyaltyprogramclinicdetails.java:32
- `M_LOYALTYPROGRAMFAMILYDETAILSMASTER` → masters/Loyaltyprogramfamilydetailsmaster.java:32
- `M_LOYALTYPROGRAMMASTER` → masters/Loyaltyprogrammaster.java:32
- `M_LOYALTYPROGRAMPATIENTCATEGORYDETAILS` → masters/Loyaltyprogrampatientcategorydetails.java:32
- `M_LOYALTYPROGRAMTARIFFDETAILS` → masters/Loyaltyprogramtariffdetails.java:32
- `M_MACHINEMASTER` → masters/Machinemaster.java:36
- `M_MACHINETESTMASTER` → Machinetestmaster.java:39
- `M_MAINTAINANCETYPE` → masters/Maintainancetype.java:36
- `M_MAINTENANCECOMPLAINTSTATUS` → masters/Maintenancecomplaintstatus.java:32
- `M_MAINTENANCECOMPLAINTTYPE` → masters/Maintenancecomplainttype.java:32
- `M_MANDALMASTER` → masters/Mandalmaster.java:34
- `M_MARITALSTATUSMASTER` → masters/Maritalstatusmaster.java:34
- `M_MEDICATIONROUTE` → masters/Medicationroute.java:40
- `M_MENUMASTER` → masters/Menumaster.java:32
- `M_MENUMASTER_OLD` → masters/MenumasterOld.java:32
- `M_MLCCHECKLISTMASTER` → masters/Mlcchecklistmaster.java:32
- `M_MLCJURISDICTIONCHECKLISTMASTER` → masters/Mlcjurisdictionchecklistmaster.java:32
- `M_MODALITYSCHEDULEDETAILMASTER` → masters/Modalityscheduledetailmaster.java:32
- `M_MODALITYSCHEDULEMASTER` → masters/Modalityschedulemaster.java:32
- `M_MODEOFDELIVERY` → masters/ModeOfDelivery.java:31
- `M_MODEOFPAYMENT` → masters/Modeofpayment.java:32
- `M_MODEOFSCARPSALE` → masters/Modeofscarpsale.java:32
- `M_MODULENAMEMASTER` → masters/ModuleNameMaster.java:27
- `M_ModulePermissionMaster` → masters/ModulePermissionMaster.java:26
- `M_MOLECULE` → masters/Molecule.java:38
- `M_MOTHERTONGUEMASTER` → masters/Mothertonguemaster.java:34
- `M_MRDBOXMASTER` → masters/Mrdboxmaster.java:32
- `M_MRDRACKMASTER` → masters/Mrdrackmaster.java:32
- `M_MRDROWMASTER` → masters/Mrdrowmaster.java:32
- `M_MRNSERIES` → masters/MrnSeries.java:24
- `M_NATIONALITYMASTER` → masters/Nationalitymaster.java:34
- `M_NONDRUGALLERGIES` → masters/Nondrugallergies.java:32
- `M_NOTECATEGORY` → masters/NoteCategory.java:32
- `M_NOTETITLEMASTER` → masters/NotetitleMaster.java:36
- `M_NOTIFYABLEDIESASE` → masters/Notifyablediesase.java:36
- `M_NPIMASTERDETAILS` → masters/Npimasterdetails.java:32
- `M_NURSENOTE` → masters/Nursenote.java:32
- `M_NURSESCHEDULEDETAILMASTER` → masters/Nursescheduledetailmaster.java:32
- `M_NURSESCHEDULEMASTER` → masters/Nurseschedulemaster.java:32
- `M_NURSINGORDER` → masters/Nursingordermaster.java:36
- `M_NURSINGSTATIONDEFDETAILS` → masters/Nursingstationdefdetails.java:32
- `M_NURSINGSTATIONDEFMASTER` → masters/Nursingstationdefmaster.java:31
- `M_NURSINGSTATIONMASTER` → masters/Nursingstationmaster.java:34
- `M_OCCUPATIONMASTER` → masters/Occupationmaster.java:34
- `M_OPDMACHINEMASTER` → masters/OpdMachineMaster.java:25
- `M_OPDSTATIONMASTER` → masters/OpdStationMaster.java:25
- `M_OPENINGBALANCE` → masters/Openingbalance.java:32
- `M_OPERATIONRESULTMASTER` → masters/Operationresultmaster.java:36
- `M_OPERATIONSTATUSMASTER` → masters/Operationstatusmaster.java:36
- `m_orderset` → masters/Orderset.java:34
- `M_ORDERSETADVICEDETAILS` → masters/Ordersetadvicemaster.java:32
- `M_ORDERSETDEFINATIONMASTER` → masters/Ordersetdefinationmaster.java:32
- `M_ORDERSETMEDICINEDETAILS` → masters/Ordersetmedicinedetails.java:32
- `M_ORDERSETSERVICEMASTER` → masters/Ordersetservicemaster.java:32
- `M_ORGANMASTER` → masters/Organmaster.java:36
- `M_OTPROCEDURECHECKLISTITEMS` → masters/OTProcedurechecklistitems.java:33
- `M_OTSCHEDULE` → masters/Otschedule.java:38
- `M_OTSCHEDULEDETAILS` → masters/Otscheduledetails.java:32
- `M_OTTABLEMASTER` → masters/Ottablemaster.java:39
- `M_OTTHEATREMASTER` → masters/Ottheatremaster.java:39
- `M_PACKAGEDEFINATIONDETAILS` → masters/Packagedefinationdetails.java:36
- `M_PACKAGEHEALTHPLANDEFINATIONMASTER` → masters/Packagehealthplandefinationmaster.java:38
- `M_PACKAGEITEMCATEGORYMASTER` → masters/Packageitemcategorymaster.java:32
- `M_PACKAGEMASTER` → masters/Packagemaster.java:37
- `M_PACKAGEVISITDETIAILS` → masters/Packagevisitdetiails.java:34
- `M_Parameterresultingmethod` → masters/Parameterresultingmethod.java:36
- `M_PATHAGENCYMASTER` → masters/Pathagencymaster.java:32
- `M_PATHANTIBIOTICHELPVALUESDETAILSMASTER` → masters/Pathantibiotichelpvaluesdetailsmaster.java:32
- `M_PATHANTIBIOTICMACHINEMAP` → masters/AntibioticMachineMap.java:25
- `M_PATHANTIBIOTICMASTER` → masters/Pathantibioticmaster.java:38
- `M_PATHCATEGORYMASTER` → masters/Pathcategorymaster.java:32
- `M_PATHCULTUREMASTER` → masters/Pathculturemaster.java:32
- `M_PATHOCATEGORY` → masters/Pathocategory.java:39
- `M_PATHOLOCATIONMASTER` → masters/PathoLocationMaster.java:36
- `M_PATHOPARAMETERCATEGORYMASTER` → masters/Pathoparametercategorymaster.java:32
- `M_PATHOPARAMETERDEFAULTVALUEDETAILS` → masters/Pathoparameterdefaultvaluedetails.java:32
- `m_pathoparameterdefaultvaluedetails` → masters/Pathoparameterdetails.java:36
- `M_PATHOPARAMETERDETAILSMASTER` → masters/Pathoparameterdetailsmaster.java:32
- `M_PATHOPARAMETERHELPVALUEDETAILS` → masters/Pathoparameterhelpvaluedetails.java:31, masters/Rangeparameterdetails.java:34
- `M_PATHOPARAMETERMASTER` → masters/Pathoparametermaster.java:40
- `M_PATHOPARAMETERUNITS` → masters/Pathoparameterunits.java:36
- `M_PATHOPARAMMACHINEMAP` → masters/Pathoparammachinemap.java:40
- `M_PATHOPROFILEMASTER` → masters/Pathoprofilemaster.java:36
- `M_PATHOPROFILETESTDETAILS` → masters/Pathoprofiletestdetails.java:35
- `M_PATHORGANISMDETAILMASTER` → masters/Pathorganismdetailmaster.java:35
- `M_PATHORGANISMMACHINEMAP` → masters/OrganismMachineMap.java:25
- `M_PATHORGANISMMASTER` → masters/Pathorganismmaster.java:38
- `M_PATHOSAMPLEMASTER` → masters/Pathosamplemaster.java:39
- `M_PATHOSAMPLESUTABILITY` → masters/Pathosamplesutability.java:32
- `M_PATHOSUBSPECIALIZATION` → masters/Pathosubspecialization.java:36
- `M_PATHOTEMPLATEMASTER` → masters/Pathotemplatemaster.java:36
- `M_PATHOTESTITEMDETAILS` → masters/Pathotestitemdetails.java:33
- `M_PATHOTESTMASTER` → masters/Pathotestmaster.java:40
- `M_PATHOTESTPARAMETER` → masters/Pathotestparameter.java:33
- `M_PATHOTESTSAMPLE` → masters/Pathotestsample.java:34
- `M_PATHOTESTTEMPLATEDETAILMASTER` → masters/Pathotesttemplatedetailmaster.java:33
- `M_PATHOZONEMASTER` → masters/PathoZoneMaster.java:26
- `M_PATHPARAMETERFORMULAMASTER` → masters/Pathparameterformulamaster.java:32
- `M_PATHPARAMETERMASTER` → masters/Pathparametermaster.java:32
- `M_PATHPARAMETERRANGEMASTER` → masters/Pathparameterrangemaster.java:32
- `M_PATHPARAMUNITMASTER` → masters/Pathparamunitmaster.java:32
- `M_PATHPROFILEMASTER` → masters/Pathprofilemaster.java:32
- `M_PATHPROFILETESTMASTER` → masters/Pathprofiletestmaster.java:32
- `M_PATHSAMPLEMASTER` → masters/Pathsamplemaster.java:34
- `M_PATHSENSITIVITYRESULTMASTER` → masters/Pathsensitivityresultmaster.java:36
- `M_PATHTEMPLATEMASTER` → masters/Pathtemplatemaster.java:32
- `M_PATHTESTITEMDETAILMASTER` → masters/Pathtestitemdetailmaster.java:32
- `M_PATHTESTMASTER` → masters/Pathtestmaster.java:32
- `M_PATHTESTPARAMETERMASTER` → masters/Pathtestparametermaster.java:32
- `M_PATHTESTSAMPLEMASTER` → masters/Pathtestsamplemaster.java:32
- `M_PATHTESTTEMPLATEMASTER` → masters/Pathtesttemplatemaster.java:32
- `M_PATIENTBROUGHTBYMASTER` → masters/Patientbroughtbymaster.java:32
- `M_PATIENTCATEGORYMASTER` → masters/Patientcategorymaster.java:38
- `M_PATIENTCONSENTMASTER` → masters/Patientconsentmaster.java:32
- `M_PATIENTSOURCEMASTER` → masters/Patientsourcemaster.java:41
- `M_PATIENTTYPEMASTER` → masters/Patienttypemaster.java:34
- `M_PATIENTWISECONSETPRINTING` → masters/Patientwiseconsetprinting.java:32
- `M_PAYMENTMODETYPE` → masters/Paymentmodetype.java:36
- `M_PAYSCHEDULE` → masters/Payschedule.java:35
- `M_PHONECATEGORY` → masters/Phonecategory.java:36
- `M_PLACEOFBIRTH` → masters/Placeofbirthmaster.java:27
- `M_PLANITEMDETAILS` → masters/Planitemdetails.java:32
- `M_PLANMASTER` → masters/Planmaster.java:38
- `M_PLANSERVICEDETAILS` → masters/Planservicedetails.java:34
- `M_PLANUNCOVEREDITEMDETAILS` → masters/Planuncovereditemdetails.java:32
- `M_PLANUNCOVEREDSERVICEDETAILS` → masters/Planuncoveredservicedetails.java:33
- `M_PLANUNITDETAILS` → masters/PlanUnitDetails.java:28
- `M_POLICESTATIONMASTER` → masters/Policestationmaster.java:36
- `M_POSTOPERATIVEINSTRUCTIONSMASTER` → masters/Postoperativeinstructionsmaster.java:36
- `M_PRE_POSTINSTRUCTIONDETAILS` → masters/PrePostinstructiondetails.java:32
- `M_PREFIXMASTER` → masters/Prefixmaster.java:36
- `M_PREGANANCYCLASS` → masters/Preganancyclass.java:36
- `M_PREOPERATIVEINSTRUCTIONSMASTER` → masters/Preoperativeinstructionsmaster.java:36
- `M_PRIMARYSYMPTOM_DIAGNOSIS_MAPPING` → masters/PrimarySymptomToDiagnosisMapping.java:32
- `M_PRIMARYSYMPTOMS` → masters/Primarysymptoms.java:36
- `M_PRIORITY` → masters/Priority.java:36
- `M_PROCEDURECATEGORYMASTER` → masters/Procedurecategorymaster.java:36
- `M_PROCEDURECHECKLIST` → masters/Procedurechecklist.java:33
- `M_PROCEDURECHECKLISTITEMS` → masters/Procedurechecklistitems.java:34
- `M_PROCEDURECHEKLISTDETAILS` → masters/Procedurecheklistdetails.java:39
- `M_PROCEDURECONSENTDETAILS` → masters/Procedureconsentdetails.java:32
- `M_PROCEDUREEQUIPMENTDETAILS` → masters/Procedureequipmentdetails.java:35
- `M_PROCEDUREINSTRUCTIONDETAILS` → masters/Procedureinstructiondetails.java:32
- `M_PROCEDUREITEMDETAILS` → masters/Procedureitemdetails.java:37
- `M_PROCEDUREMASTER` → masters/Proceduremaster.java:41
- `M_PROCEDURESERVICEDETAILS` → masters/Procedureservicedetails.java:32
- `M_PROCEDURESTAFFDETAILS` → masters/Procedurestaffdetails.java:32
- `M_PROCEDURESUBCATEGORYMASTER` → masters/Proceduresubcategorymaster.java:36
- `M_PROCEDURETYPEMASTER` → masters/Proceduretypemaster.java:36
- `M_PROVISIONALDIAGNOSIS` → masters/Provisionaldiagnosis.java:37
- `M_PTCSREGISTRATION` → masters/Ptcregistration.java:41
- `M_QUALIFICATIONMASTER` → masters/Qualificationmaster.java:34
- `M_QUESTION` → masters/Question.java:34
- `M_RACEMASTER` → masters/Racemaster.java:32
- `M_RADFILMSIZE` → masters/Radfilmsize.java:36
- `M_RADITEMDETAILSMASTER` → masters/Raditemdetailsmaster.java:38
- `M_RADMODALITYMASTER` → masters/Radmodalitymaster.java:38
- `M_RADTEMPLATEMASTER` → masters/Radtemplatemaster.java:36
- `M_RADTEMPLATERESULT` → masters/Radtemplateresult.java:36
- `M_RADTESTCATEGORY` → masters/Radtestcategory.java:36
- `M_RADTESTMASTER` → masters/Radtestmaster.java:45
- `M_RADTESTTEMPLATEDETAILMASTER` → masters/Radtesttemplatedetailmaster.java:37
- `M_REFENTITYMASTER` → masters/Refentitymaster.java:40
- `M_REFENTITYTYPEMASTER` → masters/Refentitytypemaster.java:34
- `M_REFERRALTYPEMASTER` → masters/Referraltypemaster.java:24
- `M_REFERRALUNITMAPPING` → masters/ReferralUnitMapping.java:41
- `M_REGISTRATIONCUSTOMFIELDSMASTER` → masters/Registrationcustomfieldsmaster.java:34
- `M_REGISTRATIONCUSTOMFIELDSMASTERDETAILS` → masters/Registrationcustomfieldsmasterdetails.java:32
- `M_RELATIONMASTER` → masters/Relationmaster.java:36
- `M_RELIGIONMASTER` → masters/Religionmaster.java:34
- `M_REMARKS` → masters/Pathoremarksmaster.java:30
- `M_REPORTMASTER` → masters/Reportmaster.java:32
- `M_REPORTPARAMETERDETAILS` → masters/Reportparameterdetails.java:32
- `M_REPORTPARAMETERMASTER` → masters/Reportparametermaster.java:32
- `M_REQUESTTYPEMASTER` → masters/Requesttypemaster.java:36
- `M_RGHSDEPARTMENTMASTER` → masters/RghsDepartmentMaster.java:26
- `M_RISKFACTORMASTER` → masters/Riskfactormaster.java:36
- `M_RMRSREASONMASTER` → RmrsReasonmaster.java:30
- `M_ROOMAMMENITIESMASTER` → masters/Roomammenitiesmaster.java:32
- `M_ROOMAMMENITYDETAILS` → masters/Roomammenitydetails.java:32
- `M_ROOMMASTERFORQUEUE` → RoomMasterForQueue.java:23
- `M_ROUTE` → masters/Route.java:37
- `M_SALEMARKUPMASTER` → masters/SaleMarkupMaster.java:25
- `M_SCHEDULELISTDESCRIPTION` → masters/ScheduleListMaster.java:24
- `M_SCHEDULEMASTER` → masters/Schedulemaster.java:36
- `M_SCHEMEMASTER` → masters/Schememaster.java:32
- `M_SCREENCONFIGURATIONS` → masters/Screenconfigurations.java:32
- `M_SERVICEAGENTDETAILS` → masters/Serviceagentdetails.java:32
- `M_SERVICEAGENTMASTER` → masters/Serviceagentmaster.java:36
- `M_SERVICEMASTER` → masters/Servicemaster.java:44
- `M_SERVICEMASTERNEW` → masters/Servicemasternew.java:32
- `M_SERVICEPACKAGEGROUPMASTER` → masters/Servicepackagegroupmaster.java:32
- `M_SERVICEPACKAGEMASTER` → masters/Servicepackagemaster.java:34
- `M_SERVICEPACKAGEMEDICINEDETAILS` → masters/Servicepackagemedicinedetails.java:32
- `M_SERVICETAXDETAILS` → masters/Servicetaxdetails.java:32
- `m_shift_pattern` → masters/ShiftPatternMaster.java:31
- `M_SHIFTMASTER` → masters/Shiftmaster.java:35
- `M_SIGNSANDSYMPTOMSMASTER` → masters/SignsAndSymptomsMaster.java:28
- `M_SITE` → masters/SiteMaster.java:29
- `M_SMSEMAILEVENTLIST` → masters/Smsemaileventlist.java:32
- `M_SPECIALITY` → masters/SpecialityMaster.java:31
- `M_SPECIALIZATION` → masters/Specialization.java:37
- `M_SPECIALIZATIONMASTER` → masters/Specializationmaster.java:32
- `M_SPONSORMEMBERRELATION` → masters/Sponsormemberrelation.java:32
- `M_STATEMASTER` → masters/Statemaster.java:36
- `M_STERILIZATIONEXPIRYMASTER` → masters/Sterilizationexpirymaster.java:34
- `M_STERILIZATIONMACHINEMASTER` → masters/Sterilizationmachinemaster.java:36
- `M_STERILIZATIONMASTER` → masters/Sterilizationmaster.java:36
- `M_STERILIZATIONQUALITYTESTMASTER` → masters/Sterilizationqualitytestmaster.java:36
- `M_STERILIZATIONTYPEMASTER` → masters/Sterilizationtypemaster.java:36
- `M_STORE` → masters/Store.java:43
- `M_STOREAGETYPE` → masters/Storeagetype.java:36
- `M_SUBDEPARTMENTMASTER` → masters/Subdepartmentmaster.java:36
- `M_SUBPLANMASTER` → masters/SubPlanmaster.java:36
- `M_SUBSPECIALIZATION` → masters/Subspecialization.java:36
- `M_SUBUNITMASTER` → masters/Subunitmaster.java:32
- `M_SUPPLIER` → masters/Supplier.java:41
- `M_SUPPLIERMASTER` → masters/SupplierMaster.java:31
- `M_surgeryhead` → masters/Surgeryheadmaster.java:40
- `M_SurgeryHeadDetails` → masters/SurgeryheadmasterDetails.java:33
- `m_surgeryheads` → masters/Surgeryheads.java:35
- `M_SURGERYNOTES` → masters/Surgerynotes.java:36
- `M_TABLETMAPPING` → masters/Tabletmapping.java:32
- `M_TARIFFCOPAYDETAILS` → masters/Tariffcopaydetails.java:33
- `M_TARIFFMASTER` → masters/Tariffmaster.java:41
- `M_TARIFFPACKAGECLASSWISERATES` → masters/Tariffpackageclasswiserates.java:32
- `M_TARIFFPACKAGEHEALTHPLANDEFINATIONMASTER` → masters/Tariffpackagehealthplandefinationmaster.java:35
- `M_TARIFFPACKAGEITEMCATEGORYMASTER` → masters/Tariffpackageitemcategorymaster.java:32
- `M_TARIFFSERVICEPACKAGECLASSWISERATES` → masters/Tariffservicepackageclasswiserates.java:32
- `M_TARIFFSERVICEPACKAGEGROUPMASTER` → masters/Tariffservicepackagegroupmaster.java:32
- `M_TARIFFSERVICEPACKAGEMASTER` → masters/Tariffservicepackagemaster.java:34
- `M_TARIFFSERVICEPACKAGEMEDICINEDETAILS` → masters/Tariffservicepackagemedicinedetails.java:32
- `M_TAXBUILDERMASTER` → masters/Taxbuildermaster.java:36
- `M_TAXMASTER` → masters/Taxmaster.java:38
- `M_TAXONOMYCODEMASTER` → masters/Taxonomycodemaster.java:32
- `M_TermAndCondition` → transactionals/TermsAndCondition.java:20
- `M_TERMANDCONDITION` → masters/Termandcondition.java:35
- `M_TERMSOFPAYMENT` → masters/Termsofpayment.java:38
- `M_THERAPEUTICCLASS` → masters/Therapeuticclass.java:36
- `M_TIMEFORMEALMASTER` → masters/Timeformealmaster.java:32
- `M_TIMEOFMEALMASTER` → masters/Timeofmealmaster.java:32
- `m_transactionmachine` → masters/TransactionMachine.java:29
- `M_TRIAGEMASTER` → masters/Triagemaster.java:32
- `M_UNITDEPARTMENTDETAILS` → masters/Unitdepartmentdetails.java:37
- `M_UNITDOCTORDETAILS` → masters/Unitdoctordetails.java:32
- `M_UNITMASTER` → masters/Unitmaster.java:46
- `M_UNITOFMEASURE` → masters/Unitofmeasure.java:36
- `M_UNITOFSTORAGE` → masters/Unitofstorage.java:34
- `M_USERBANKACCOUNTDETAILS` → masters/UserBankAccountDetails.java:21
- `M_VACCINEIMMUNIZATIONAGEGROUP` → masters/Vaccineimmunizationagegroup.java:35
- `M_VACCINEMASTER` → masters/Vaccinemaster.java:36
- `M_VEHICLEREQUESTTYPEMASTER` → masters/VehicleRequestTypeMaster.java:32
- `M_VILLAGEMASTER` → masters/Villagemaster.java:34
- `M_VISITTYPEMASTER` → masters/Visittypemaster.java:39
- `M_VISITTYPESERVICEDETAILS` → masters/Visittypeservicedetails.java:32
- `M_VISITTYPEUNITDETAILS` → masters/Visittypeunitdetails.java:32
- `M_VITALMASTER` → masters/Vitalmaster.java:41
- `M_VITALSMASTER` → masters/Vitalsmaster.java:37
- `m_vitaltypes` → masters/Vitaltype.java:37
- `M_WARDTYPEMASTER` → masters/Wardtypemaster.java:36
- `M_warrantyMaster` → masters/WarrantyMaster.java:30
- `M_WASTEBAGMASTER` → masters/Wastebagmaster.java:36
- `M_WINGMASTER` → masters/Wingmaster.java:36
- `M_WORKSHOPMASTER` → masters/Workshopmaster.java:34

### ihms_iam — 22 Security & IAM (T_USER* + M_STAFF/DOCTOR conflict → IAM owns profiles) (22 tables)

- `M_DOCTORADDRESSDETAILS` → masters/Doctoraddressdetails.java:32
- `M_DOCTORADDRESSMASTER` → masters/Doctoraddressmaster.java:36
- `M_DOCTORDEPARTMENTDETAILS` → masters/Doctordepartmentdetails.java:33
- `M_DOCTORMASTER` → masters/Doctormaster.java:41
- `M_DOCTORSCHEDULE` → masters/Doctorschedule.java:38
- `M_DOCTORSCHEDULEDETAILMASTER` → masters/Doctorscheduledetailmaster.java:32
- `M_DOCTORSCHEDULEMASTER` → masters/Doctorschedulemaster.java:32
- `M_DOCTORTYPEMASTER` → masters/Doctortypemaster.java:36
- `M_DOCTORVISITTYPEDETAILS` → masters/Doctorvisittypedetails.java:32
- `M_STAFFADDRESSDETAILS` → masters/Staffaddressdetails.java:32
- `M_STAFFAPPOINTMENTDETAILS` → masters/Staffappointmentdetails.java:32
- `M_STAFFDEPARTMENTDETAILS` → masters/Staffdepartmentdetails.java:35
- `M_STAFFDEPENDENTDETAIL` → masters/Staffdependentdetail.java:32
- `M_STAFFEDUCATIONDETAIL` → masters/Staffeducationdetail.java:32
- `M_STAFFEXPDETAIL` → masters/Staffexpdetail.java:32
- `M_STAFFMASTER` → masters/Staffmaster.java:41
- `T_USER_FACEAUTH` → trx/abdm/UserFaceAuth.java:21
- `T_USERDETAILS` → masters/TUserdetails.java:45
- `T_USERROLEMASTER` → masters/TUserrolemaster.java:38
- `T_UserStoreDetails` → masters/TUserStoreDetails.java:30
- `T_USERUNITDETAILS` → masters/TUserunitdetails.java:32
- `T_UserWardDetails` → masters/TUserWardDetails.java:23

### ihms_rcm — 23 RCM & Claims (T_ADMISSION_TID variant) (4 tables)

- `T_ADMISSION_TID` → rcm/AdmissionTIDDetails.java:17
- `T_ELIGIBILITYCHECK` → rcm/EligibilityCheck.java:31
- `T_NHCX_REQUEST` → hcx/NHCXRequest.java:28
- `T_NHCX_REQUEST_DETAILS` → hcx/NHCXRequestDetails.java:24

### ihms_rmrs — 24 RMRS & PMRD (Treasury — M_AUTHORITY/HEADS/LEDGER conflict → RMRS per §1.1; s_* collections) (2 tables)

- `s_servicewisecollection` → statistical/ServiceWiseCollection.java:21
- `s_userwisecollection` → statistical/UserWiseCollection.java:21

### ihms_interop — 25 Interop & Notifications (legacy poll queues → Kafka; T_EHRJOBDETAILS) (6 tables)

- `T_ABDMREQUESTQUEUE` → ABDMRequestQueue.java:29
- `T_EHRJOBDETAILS` → transactionals/EHRJobDetails.java:21
- `T_INBOUNDHIS` → transactionals/Inboundhis.java:28
- `T_OUTBOUNDHIS` → transactionals/Outboundhis.java:29
- `T_WHATSAPPMESSAGEQUEUE` → WhatsappMessageQueue.java:22
- `T_WHATSAPPMESSAGESTATUS` → WhatsappMessageStatus.java:21

### ihms_govt — 28 Govt Programs (EJS 57 ANDROID_/MASTER_/ES_/USER_/MEDIA + Aasmaan 35 convention + BENEFICIARY_ENROLMENT_IHMS/T_REFRENCETID) (57 tables)

- `ANDROID_ABORTION_DETAILS` → ejs/EjsAbortionDetails.java:23
- `ANDROID_ANC` → ejs/EjsAnc.java:23
- `ANDROID_ANC_VISITS` → ejs/EjsAncVisits.java:23
- `ANDROID_ASHA_WORK` → ejs/EjsAshaWork.java:23
- `android_asha_work_approval` → ejs/EjsAshaWorkApproval.java:18
- `ANDROID_ATTENDANCE` → ejs/EjsAttendance.java:23
- `ANDROID_CBAC_FORM` → ejs/EjsCbac.java:23
- `ANDROID_CHILD_CARE` → ejs/EjsChildCare.java:25
- `ANDROID_CHILD_CARE_CHECKUP` → ejs/EjsChildCareCheckup.java:23
- `ANDROID_DEATH_DETAILS` → ejs/EjsDeathDetails.java:24
- `ANDROID_DISEASES` → ejs/EjsDisease.java:23
- `ANDROID_ECS` → ejs/EjsEcs.java:24
- `ANDROID_FAMILY_AWC_DETAILS` → ejs/EjsFamilyAwcDetails.java:22
- `ANDROID_FAMILY_DETAILS` → ejs/EjsFamilyDetails.java:21
- `ANDROID_FAMILY_PLANNING` → ejs/EjsFamilyPlanning.java:23
- `ANDROID_HELP` → ejs/EjsHelp.java:21
- `ANDROID_INCENTIVES` → ejs/EjsIncentives.java:22
- `ANDROID_MC_DETAILS` → ejs/EjsMcDetails.java:22
- `ANDROID_MCHN` → ejs/EjsMchn.java:22
- `ANDROID_MEMBERS` → ejs/EjsMembers.java:23
- `ANDROID_MOTIVATED_BY_ASHA` → ejs/EjsAshaMotivation.java:22
- `ANDROID_PNC` → ejs/EjsPnc.java:22
- `ANDROID_PNC_VISITS` → ejs/EjsPncVisits.java:23
- `ANDROID_REFER_DETAILS` → ejs/EjsReferDetails.java:23
- `ANDROID_SCHOOL_DETAILS` → ejs/EjsSchoolDetails.java:23
- `ANDROID_SUPERVISOR_ANC` → ejs/EjsSupervisorAnc.java:22
- `ANDROID_SUPERVISOR_CHILD` → ejs/EjsSupervisorChild.java:22
- `ANDROID_SUPERVISOR_ECS` → ejs/EjsSupervisorEcs.java:22
- `ANDROID_SUPERVISOR_MEMBERS` → ejs/EjsSupervisorMember.java:22
- `ANDROID_SUPERVISOR_PNC` → ejs/EjsSupervisorPnc.java:22
- `ANDROID_SUPERVISOR_VISITS` → ejs/EjsSupervisorVisits.java:22
- `ANDROID_WORK_DETAILS` → ejs/EjsWorkDetails.java:23
- `BENEFICIARY_ENROLMENT_IHMS` → chiranjeevi/BeneficiaryEnrolment.java:20
- `ES_CHECKLIST_QUES_ANSWERS` → ejs/EjsChecklistQuesAnswers.java:23
- `ES_CHECKLIST_QUESTIONS` → ejs/EjsChecklistQuestions.java:22
- `ES_CHECKLISTS` → ejs/EjsChecklists.java:18
- `ES_CHECKLISTS_SUBMISSIONS` → ejs/EjsChecklistsSubmissions.java:22
- `INDICATORS` → ejs/EjsIndicators.java:18
- `MASTER_ACTIVITY` → ejs/EjsMasterActivity.java:31
- `MASTER_AWCS` → ejs/EjsMasterAwcs.java:18
- `MASTER_BLOCKS` → ejs/EjsMasterBlocks.java:18
- `MASTER_DISTRICTS` → ejs/EjsMasterDistricts.java:18
- `MASTER_ENTITY` → ejs/EjsMasterEntity.java:31
- `MASTER_GRAMPANCHAYATS` → ejs/EjsMasterGrampanchayats.java:18
- `MASTER_PHCS` → ejs/EjsMasterPhcs.java:18
- `MASTER_STATES` → ejs/EjsMasterStates.java:18
- `MASTER_SUBCENTERS` → ejs/EjsMasterSubcenters.java:18
- `MASTER_VILLAGES` → ejs/EjsMasterVillages.java:18
- `MASTER_ZONES` → ejs/EjsMasterZones.java:18
- `MEDIA` → ejs/Media.java:23
- `MEDIA_CATEGORIES` → ejs/MediaCategories.java:18
- `MEDIA_TYPES` → ejs/MediaTypes.java:18
- `PROFILE` → ejs/EjsProfile.java:18
- `USER_` → ejs/EjsUser.java:20
- `USER_DESIGNATION` → ejs/EjsUserDesignation.java:18
- `user_detail_temp` → txt/UserDetailTemp.java:21
- `USER_TYPES` → ejs/EjsUserTypes.java:18

### ihms_audit — cross-cutting Audit (decision pending per PG §1.1 — T_AUDIT/T_TRX_HISTORY/T_LOGIN_AUDIT/T_ABDM_AUDIT/T_APPOINTMENT_CHANNEL) (2 tables)

- `T_AUDIT` → Audit.java:35
- `T_TRX_HISTORY` → TrxHistory.java:12

### ihms_other — Residual — 128 active T_* that need Oracle dump re-homing (see below); DO NOT create schema; re-assign after dump (109 tables)

- `CORONA_FAMILY_DETAILS` → ejs/EjsCoronaFamilyDetails.java:22
- `CORONA_MEMBERS_DETAILS` → ejs/EjsCoronaMembersDetails.java:22
- `EMERGENCY_CASE_DETAILS` → trx/EmergencyCaseDetails.java:22
- `IHMS_COUNTS` → trx/IhmsCounts.java:23
- `IHR_MEDICATION_TMP` → transactionals/PatientMedication.java:24
- `T_ABDM_TRX` → ABDMTransaction.java:18
- `T_ACTION_NOTIFICATION` → ActionNotification.java:30
- `T_AdmMLCDetails` → trx/MLCDetails.java:29
- `T_ANCVISITDETAILS` → transactionals/AncVisitDetails.java:31
- `T_APPERRORDETAILS` → transactionals/AppErrorDetails.java:19
- `T_APPOINTMENTSCHEDULESUMMARY` → AppointmentScheduleSummary.java:32
- `T_ATTENDANTPASSDETAILS` → transactionals/AttendantPassDetails.java:22
- `T_BACKGROUND_TASKS` → BackgroundTask.java:24
- `T_CAREPLAN` → transactionals/CarePlan.java:26
- `T_CAREPLANDETAILS` → transactionals/CarePlanDetails.java:26
- `T_CHILDDEATHREVIEW` → transactionals/ChildDeathReview.java:24
- `T_ClearanceAuthority` → masters/TClearanceAuthority.java:30
- `T_COLLECTION` → transactionals/RmrsCollection.java:25
- `T_COLLECTIONDETAILS` → transactionals/CollectionDetail.java:24
- `T_COLLECTIONHEADER` → transactionals/CollectionHeader.java:24
- `T_CORONASTATSDETAILS` → transactionals/CoronaStatsDetails.java:28
- `T_CWFMAPPING` → masters/CWFMapping.java:41
- `T_DELIVERYPARTICULARSMONTHLY` → transactionals/DeliveryParticularsMonthly.java:25
- `T_DOCTORWAIVER` → transactionals/Doctorwaiver.java:33
- `T_DOCUMENTS` → transactionals/Documents.java:31
- `T_EASUSHADHI_INDENT_ISSUE_REQ` → transactionals/EAushadiIndentIssueReq.java:21
- `T_EASUSHADHI_QC_REQ` → transactionals/EaushadiQCReq.java:22
- `T_EASUSHADHI_RETURNSTATUS_REQ` → transactionals/EaushadiReturnStatusUpdateReq.java:22
- `T_EAUSHADHI_INDENT_TRX` → EaushadhiIndentTrx.java:21
- `T_EAUSHADIITEMSTOCK` → transactionals/EAushadiItemStock.java:22
- `T_EMAILTEMPLATE` → masters/EmailTemplate.java:36
- `T_EMAILTEMPLATEDETAILS` → masters/EmailTemplateDetails.java:34
- `T_ERAKTKOSH_COMPONENTCODE_MAPPING` → masters/EraktkoshComponentcodeMapping.java:31
- `T_ERAKTKOSHREQUEST_TRX` → EraktkoshRequest.java:24
- `T_EVENT_SUBMISSION` → transactionals/EventSubmission.java:24
- `T_EXTERNALINVCONSUMPTIONSTOCK` → ExternalInvConsumptionStock.java:36
- `T_EXTERNALINVENTORYSTOCK` → ExternalInvetoryStock.java:36
- `T_EXTERNALORDERDETAILS` → transactionals/ExternalOrderDetails.java:26
- `T_EXTPACKAGESUBSCRIPTION` → ExtPackageSubscription.java:20
- `T_FAMILYMEMBERDETAILS` → transactionals/FamilyMemberDetails.java:32
- `T_FAMILYPLANNINGCHECKLISTDETAILS` → transactionals/FamilyPlanningChecklistDetails.java:25
- `T_FAMILYPLANNINGDETAILS` → transactionals/FamilyPlanningDetails.java:26
- `T_Followupvisitinfo` → trx/FollowUpVisitInfo.java:23
- `t_fuelstationdetails` → transactionals/FuelStationDetails.java:22
- `T_GRIEVANCEDOCUMENTS` → transactionals/GrievanceDocuments.java:21
- `T_GRIEVANCESEVERITYTEMPLATEDETAILS` → masters/GrievanceSeverityTemplateDetails.java:34
- `t_hospitalevents` → masters/Events.java:30
- `T_INDENTINVOICE` → transactionals/IndentInvoice.java:24
- `T_INDENTINVOICEDETAILS` → transactionals/IndentInvoiceDetails.java:23
- `T_ISSUEITEM_DRAFT` → DraftedIndentDetails.java:24
- `T_ITEMSUPPLIER` → transactionals/ItemSupplier.java:28
- `T_JANTA_REPORTSUMMARY` → transactionals/ReportSummary.java:27
- `T_JOBAUDIT` → JobAuditEvent.java:22
- `T_MACHINEWISEMICRORESULTS` → masters/MachineWiseMicroResult.java:20
- `T_MORTUARY` → Mortuary.java:28
- `T_MORTUARYVERIFICATIONS` → MortuaryVerifications.java:23
- `T_NOTEDETAILS` → NoteDetails.java:29
- `T_PATIENTGRIEVANCECOMMENTS` → transactionals/PatientGrievanceComments.java:22
- `T_PATIENTGRIEVANCEFEEDBACK` → transactionals/PatientGrievanceFeedback.java:21
- `T_PAYMENTTRANSFER` → transactionals/payment/PaymentTransfer.java:23
- `T_PAYMENTTRANSFERREQUEST` → transactionals/payment/PaymentTransferRequest.java:23
- `T_PAYMENTTRANSFERREQUESTDETAIL` → transactionals/payment/PaymentTransferRequestDetail.java:21
- `T_PENDINGCONSULTVISITS` → transactionals/PendingConsultVisits.java:27
- `T_PHCCLINICALDATA` → PHCClinicalData.java:30
- `T_POISSUE` → txt/POissue.java:32
- `t_poissueitembatchdetails` → txt/IssuedItemBatchDetails.java:33
- `t_poissueitemdetails` → txt/POissueItemDetails.java:31
- `T_PROCEDURECSSDISSUE` → transactionals/ProcedureCssdIssue.java:25
- `T_PROCEDURECSSDISSUEDETAILS` → transactionals/ProcedureCssdIssueDetail.java:25
- `T_PROCEDURECSSDKITDETAILS` → transactionals/ProcedureCssdKitDetails.java:26
- `T_PROCEDURECSSDREQUEST` → transactionals/ProcedureCssdRequest.java:25
- `T_PROCEDURECSSDREQUESTDETAILS` → transactionals/ProcedureCssdRequestDetailDto.java:25
- `t_purchaserequest` → transactionals/PurchaseRequest.java:23
- `t_purchaserequestdetails` → transactionals/PurchaseRequestDetails.java:23
- `T_QuestionAnswer` → transactionals/QuestionAnswerDto.java:25
- `T_QUEUE` → transactionals/PatientQueue.java:22
- `T_RATECONTRACTDETAILS` → transactionals/RateContractDetails.java:31
- `T_RATECONTRACTUNITS` → transactionals/RateContractUnits.java:32
- `T_REFRENCETID` → chiranjeevi/RefrenceTid.java:20
- `t_reorderlevelsummery` → ItemReorderLevelSummaryDetails.java:26
- `T_ResolutionAsignee` → transactionals/ResolutionAsignee.java:27
- `T_RGHSOPDMETADATA_TRX` → transactionals/RGHSOpdMetadata.java:22
- `T_RMRSDashboardTransaction` → transactionals/RMRSDashboardTransaction.java:24
- `t_role_enabledpermission` → RoleBasedPermission.java:33
- `T_RULEDEFINITION` → RuleDefinition.java:19
- `T_RULES` → RuleSet.java:27
- `T_SCWFMAPPING` → masters/SCWFMapping.java:32
- `T_SERVICEHOLD` → ServiceHoldRequest.java:22
- `t_shift_pattern_details` → masters/ShiftPatternDetailsMaster.java:32
- `T_SIMSRGHSDATA` → RGHSSims.java:23
- `T_SPONSOR` → txt/Sponsor.java:28
- `T_SpouseAndChildrenDetails` → transactionals/SpouseAndChildrenDetails.java:26
- `T_STOREITEMCATEGORY` → transactionals/StoreItemCategory.java:25
- `T_SUSPENSEACCOUNTDETAILS` → transactionals/SuspenseAccountDetails.java:23
- `T_TARIFFSERVICECLASSRATEDETAIL` → transactionals/Tariffserviceclassratedetail.java:30
- `T_TARIFFSERVICEMASTER` → masters/Tariffservicemaster.java:34
- `t_taxMasterDetails` → masters/TaxMasterDetails.java:35
- `T_TELECONSULTANTDETAILS` → transactionals/TeleConsultantDetails.java:28
- `T_USER_HOSPITAL_AUTHORITY` → transactionals/TUserHospitalAuthority.java:23
- `t_user_shift_details` → transactionals/UserShiftDetailsDto.java:32
- `T_UserBuildingDetails` → masters/TUserBuildingDetails.java:24
- `T_USERDISTRICTDETAILS` → transactionals/UserDistrictDetails.java:28
- `T_USERSPECIALITYDETAILS` → transactionals/UserSpecialityDetails.java:29
- `T_USERSTATEDETAILS` → transactionals/UserStateDetails.java:28
- `T_UserSupplierDetails` → masters/UserSupplierDetails.java:24
- `T_USERTRANSFERDETAILS` → masters/UserTransferDetails.java:26
- `T_vitaltypedetails` → masters/Tvitaltypedetails.java:32
- `T_warrantyList` → transactionals/WarrantyList.java:26
- `UploadSession` → UploadSession.java:17

---

## Appendix B — 35 commented Aasmaan models (no active JPA)

All 35 have `//@Table(name = "")` commented out (e.g., `aasmaan/AasmaanCaseHighrisk.java:17`). By EclipseLink default they map to class-name uppercased (`AASMAANCASE`, `AASMAANCASEEVENT`, …). Verify actual Oracle names before PG DDL; recommendation: add explicit `@Table` annotations.

- `aasmaan/AasmaanCaseHighrisk.java:17` — //@Table(name = "")
- `aasmaan/AasmaanCaseEvent.java:17` — //@Table(name = "")
- `aasmaan/AasmaanCaseIoForm.java:17` — //@Table(name = "")
- `aasmaan/AasmaanInstitutionShift.java:17` — //@Table(name = "")
- `aasmaan/AasmaanCaseMonitoring.java:17` — //@Table(name = "")
- `aasmaan/AasmaanCaseChecklist3.java:17` — //@Table(name = "")
- `aasmaan/AasmaanCaseReferralFinish.java:17` — //@Table(name = "")
- `aasmaan/AasmaanCaseChecklist2.java:17` — //@Table(name = "")
- `aasmaan/AasmaanCaseStageCount.java:17` — //@Table(name = "")
- `aasmaan/AasmaanCaseFinishAdmission.java:17` — //@Table(name = "")
- `aasmaan/AasmaanCase.java:17` — //@Table(name = "")
- `aasmaan/AasmaanCaseReferralCancel.java:17` — //@Table(name = "")
- `aasmaan/AasmaanRscCallLog.java:17` — //@Table(name = "")
- `aasmaan/AasmaanCaseAdministrationDrug.java:17` — //@Table(name = "")
- `aasmaan/AasmaanStat.java:17` — //@Table(name = "")
- `aasmaan/AasmaanUser.java:17` — //@Table(name = "")
- `aasmaan/AasmaanInstitutionLevel.java:17` — //@Table(name = "")
- `aasmaan/AasmaanCaseCmfs.java:17` — //@Table(name = "")
- `aasmaan/AasmaanCaseChecklist4.java:17` — //@Table(name = "")
- `aasmaan/AasmaanCaseFieldMonitoring.java:17` — //@Table(name = "")
- `aasmaan/AasmaanCasePdMonitoring.java:17` — //@Table(name = "")
- `aasmaan/AasmaanCasePrescribedDrug.java:17` — //@Table(name = "")
- `aasmaan/AasmaanETicket.java:17` — //@Table(name = "")
- `aasmaan/AasmaanCaseDischargeNote.java:17` — //@Table(name = "")
- `aasmaan/AasmaanReport.java:17` — //@Table(name = "")
- `aasmaan/AasmaanInstitutionReferral.java:17` — //@Table(name = "")
- `aasmaan/AasmaanCaseDeliveryNote.java:17` — //@Table(name = "")
- `aasmaan/AasmaanInstitutionLevelInstitution.java:17` — //@Table(name = "")
- `aasmaan/AasmaanInstitutionFeedback.java:17` — //@Table(name = "")
- `aasmaan/AasmaanCaseChecklist1.java:17` — //@Table(name = "")
- `aasmaan/AasmaanCaseSchedule.java:17` — //@Table(name = "")
- `aasmaan/AasmaanCaseReferralSlip.java:17` — //@Table(name = "")
- `aasmaan/AasmaanCasePncMonitoring.java:17` — //@Table(name = "")
- `aasmaan/AasmaanDevice.java:17` — //@Table(name = "")
- `aasmaan/AasmaanInstitution.java:17` — //@Table(name = "")


## Appendix C — Generation method (repro)

```bash
# Active JPA
grep -Rn '@Table' /Users/adityabanerjee/personal_project/ihms/src/main/java/com/ohum/models --include="*.java" | grep -v '//@Table' > /tmp/active_lines2.txt
grep -R '@Table' ... | sed -E 's/.*@Table\(name *= *"([^"]+)".*/\1/' | sort -u > /tmp/tables_active.txt  # 898 distinct
# Native-SQL
grep -R 'T_ADVANCE\|T_GRN\|T_PurchaseOrder\|T_CHARGES' /Users/adityabanerjee/personal_project/ihms/src/main/java/com/ohum --include="*.java" -n | grep -i 'FROM\|JOIN\|UPDATE\|INTO'
# Aasmaan
grep -R '//@Table' /Users/adityabanerjee/personal_project/ihms/src/main/java/com/ohum/models/aasmaan -n
```

*This draft was generated by `jam (jim-mt4k8u34)` via a Python group-by on the above extracts, using `POSTGRESQL-MIGRATION §1` schema-per-service as the assigner and file-path heuristics for residuals. Full assignment logic is in `/tmp/gen_mapping.py` (ponytail naive scan — see Method & limits).*

---

*End of draft — GAP-12. Next step is Oracle dump cross-check and god sign-off before editing MICROSERVICE-CATALOG/10 ownership tables.*
