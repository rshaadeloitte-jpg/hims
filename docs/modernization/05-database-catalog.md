# 05 — Database Catalog (AS-IS Oracle → Target PostgreSQL)

> Source: AGENT-05 database forensics on `D:\ihms` (iHMS / OHUM PAS NG). EclipseLink JPA 2.6.4 + Oracle 12c+ (thin driver). Research-only.

---

## 1. Architecture AS-IS (one paragraph)

iHMS is a single-database monolith on Oracle (`jdbc:oracle:thin:@//10.10.7.63:1521/orcl`, schema owner `IHMSDOCITDEVMODS_PROD_V1`, HikariCP pool 5–10, `SELECT 1 FROM DUAL` validation). JPA (EclipseLink, PU `ORCL`, shared cache OFF) scans `com.ohum.models` + `com.ohum.dto`; Spring Data JPA repositories are disabled. All persistence flows through one generic DAO/fluent-query layer (`GenericDAO` + `GenericQuery`) with ~436 ad-hoc native SQL call sites in services, ~2000 SQL string literals, 30 live-called stored procedures (`CIMS_*`), and 719 Oracle PL/SQL artifacts in `generated-procs/`. Identity is 100 % manually assigned `Long` PKs from per-table sequences (`<TABLENAME>_SEQ`); the ID-assignment aspect and 8 other sites already contain dual Oracle/PostgreSQL branches (`indexOf("oracle")`), so a partial PG path exists, but the bulk of SQL is Oracle-only.

Key config evidence:
- `src/main/resources/META-INF/persistence.xml:5-9` — PU `ORCL`, JTA `jdbc/orcl`, `exclude-unlisted-classes=false`
- `src/main/java/com/ohum/App.java:173-193` — EMF: EclipseLink adapter, `Database.ORACLE` (or `POSTGRESQL` when driver isn't oracle), shared cache NONE
- `application.properties:53-105` — Hikari, URL, driver, validationQuery, `spring.data.jpa.repositories.enabled=false`

---

## 2. Mapping Strategy (EclipseLink)

| Rule | Detail |
|---|---|
| Masters | `@Table(name="M_<UPPERCASE_CLASSNAME>")` — 609 files (570 `M_*` pattern, 608 `@Table`) — `src/main/java/com/ohum/models/masters/` =609 (05 §3.1: 608 entities) |
| Transactionals | `@Table(name="T_<UPPERCASE_CLASSNAME>")` — 165 files (130 `T_*` pattern, 162 `@Table`) — `src/main/java/com/ohum/models/transactionals/` =165 recursive |
| Aasmaan module | No `@Table` → EclipseLink default = class name uppercased (`AASMAANCASE`, …), 35 tables |
| EJS module | `ANDROID_*` (ASHA app), `MASTER_*` (locations), `ES_*`, `MEDIA*`, `USER_`, `PROFILE`, `INDICATORS` |
| Integration/temp | Lowercase mixed: `t_creditNote`, `t_patienttokens`, `user_detail_temp`, `district_mst`, `block_mst`, `city_mst` |
| Stat/RCM/HCX/Chiranjeevi | `s_*`, `T_ADMISSION_TID`, `T_ELIGIBILITYCHECK`, `T_NHCX_REQUEST(_DETAILS)`, `T_REFRENCETID`, `BENEFICIARY_ENROLMENT_IHMS` |
| DTO-entities | 21 entities in `dto/` (e.g. `T_NA_ITEMDETAILS`) |
| Relationships | Effectively none (0 `@JoinColumn`/`@ManyToMany`; 14 `@ManyToOne`); all joins hand-written SQL |

Base-class columns injected into every table:
- `MBase` (≥750 tables): `UNITID, STATUS, CREATEDUNITID, UPDATEDUNITID, ADDEDBY, ADDEDDATETIME, UPDATEDBY, UPDATEDDATETIME, SYNCHRONIZED`
- `LBase` (aasmaan): `ID` only
- `EjsBase` (ejs, 57 tables): `BigInteger ID`
- `Base` (createdAt/updatedAt/createdBy/updatedBy/status enum) — **unused by any entity**

---

## 3. Table Catalog (by module)

Total: **~843 tables** (808 explicit `@Table` + 35 convention-mapped) / ~898 entities.

### 3.1 Masters — 608 entities (`com.ohum.models.masters`)
| Table (pattern `M_*`) | Purpose | PK | Status cols | Audit cols | Write locations |
|---|---|---|---|---|---|
| M_UNITMASTER (45 refs) | Hospital units | ID | STATUS | ADDEDBY/DATETIME, UPDATEDBY/DATETIME | Generic CRUD (CrudApi), CommonUtilServiceImpl, ApplicationJobs |
| M_DEPARTMENTMASTER (32) | Departments | ID | STATUS | MBase audit | Generic CRUD, QueueManagmentServiceImpl |
| M_BEDMASTER (26) | Beds (also `OCCUPIED` char flag updated by procs) | ID | STATUS, OCCUPIED | MBase audit | Generic CRUD, `CIMS_ADDADMISSIONDETAILS` proc |
| M_ITEMMASTER (18) | Pharmacy/store items | ID | STATUS | MBase audit | ItemSalesServiceImpl, PharmacyServiceImpl |
| M_SERVICEMASTER (15) | Service catalogue | ID | STATUS | MBase audit | BillingCommonUtilServiceImpl |
| M_DOCTORMASTER (16) | Doctors | ID | STATUS | MBase audit | Generic CRUD, CommonUtilServiceImpl |
| M_STAFFMASTER (10) | Staff (joined from procs via T_UserDetails.EmployeeID) | ID | STATUS | MBase audit | Generic CRUD, procs |
| M_PATHOTESTMASTER (12) | Pathology tests | ID | STATUS | MBase audit | Patho services |
| M_ACCOUNTING* / M_ACCLEDGER* (14) | Ledger/cost centres | ID | STATUS | MBase audit | AccountingServiceImpl |
| M_* (560 more) | Diagnosis masters, vitals, allergies, blood bank, CSSD, equipment, LIS, configurations… | ID | STATUS | MBase audit | Generic CRUD via @CrudApi |
| CONFIG_APPLICATION | App config key-values | ID | — | — | CommonUtilServiceImpl, native in migration utils |
| block_mst / city_mst / district_mst | Rajasthan geography | ID | — | — | CommonUtilServiceImpl (22 refs district_mst) |

### 3.2 Transactionals — 165 entities (`com.ohum.models.transactionals`, +bloodBank/Nursing/payment)
| Table | Purpose | PK | Status | Audit | Write locations |
|---|---|---|---|---|---|
| T_Encounter (52 refs) | Visits (OPD/IPD/ER) — highest-shared table | ID | STATUS/visitMark | MBase audit | CommonUtilServiceImpl, RcmServiceImpl, IntegrationServiceImpl, BillingCommonUtilServiceImpl |
| T_OUTBOUNDHIS (47) | HL7/EMR outbound message queue | ID | STATUS, RetryCount | MBase audit | HL7Helper (118 SELECTs), IntegrationServiceImpl |
| T_USERDETAILS (25) | Login users | ID | STATUS | MBase audit | AuthenticationServiceImpl, ApplicationJobs, SolrUtil |
| T_LINENREQUEST(+DETAILS) (20) | Linen requests | ID | STATUS | MBase audit | LinenLaundryServiceImpl |
| T_ITEMCLINIC (17) / T_ITEMSTOCK (15) | Item clinic mapping / stock | ID | STATUS | MBase audit | InventoryServiceImpl, ItemSalesServiceImpl |
| T_SPONSOR (18) | Sponsoring entities | ID | STATUS | MBase audit | BillingCommonUtilServiceImpl |
| T_BIOMEDICALWASTE(+DETAILS) (18/10) | BMW segregation | ID | STATUS | MBase audit | BioMedicalWasteManagementServiceImpl |
| T_PATIENT* (~30 tables) | Allergies, advice, diagnosis, notes, vitals, feedback, e-order, documents… | ID | STATUS | MBase audit | Patient services, EHRSyncJobs |
| T_FREQ_ITEMS | Frequent items (MERGE INTO upsert) | ID | count | UpdatedDateTime | BaseServiceImpl:928-945 |
| T_TRX_HISTORY | Audit trail (seq `t_trx_history_seq`) | ID | — | addeddatetime | AuditHelper (raw INSERT, 40+ variants) |
| T_LOGIN_AUDIT / T_ABDM_AUDIT / T_APPOINTMENT_CHANNEL | Audit tables | ID | — | — | AuditHelper:917/938/961 |
| T_* (blood bank 21, nursing 4, payment 3) | Blood bags, donor, requisition; nurse orders; payment transfers | ID | STATUS | MBase audit | bloodBank/ Nursing/ payment services |

### 3.3 EJS (ASHA/e-Jan) — 57 entities (`com.ohum.models.ejs`) — `BigInteger` IDs
`ANDROID_*` (ANC, PNC, ASHA_WORK, CHILD_CARE, INCENTIVES, FAMILY_DETAILS, MEMBERS…), `ES_CHECKLIST*`, `MASTER_STATES/DISTRICTS/BLOCKS/VILLAGES/PHCS/AWCS/GRAMPANCHAYATS/ZONES`, `MEDIA/MEDIA_CATEGORIES/MEDIA_TYPES`, `USER_/USER_TYPES/USER_DESIGNATION`, `PROFILE`, `INDICATORS`. Written by ejs services + `ejs.asha.incentive.schedule` job.

### 3.4 Aasmaan — 35 entities (`com.ohum.models.aasmaan`) — `LBase`
Convention-mapped names (`AASMAANCASE*`, case checklist 1–4, delivery/discharge notes, CMFS…). No `@Table`; default naming = class name uppercased. **Migration must verify actual DB names.**

### 3.5 Integration modules (trx, hcx, rcm, chiranjeevi, statistical)
`T_PATIENT_ABDM(+_CC, _SHARETOKEN)`, `T_PATIENT_CONSENT_REQUESTS_ABDM(+_DATA)`, `T_USER_FACEAUTH`, `T_ADMISSION_TID`, `T_ELIGIBILITYCHECK`, `T_NHCX_REQUEST(+_DETAILS)`, `T_REFRENCETID`, `BENEFICIARY_ENROLMENT_IHMS`, `s_servicewisecollection`, `s_userwisecollection` (statistical, plain POJOs).

### 3.6 Unmapped core (native SQL only — schema must be dumped from Oracle)
`T_REGISTRATION`, `T_REGISTRATION_DETAILS`, `T_Registration_Int`, `T_Registration_Details_Int`, `T_coronaStats`, `t_transfer`, `t_admission`, `t_patientrequisition`, `Config_Application`.

---

## 4. PK & Sequence Strategy

- Manual PK: aspect `ServiceLayerAspect:697-750` assigns `Long id` from `<TABLE_NAME>_seq` before `GenericDAO.save`.
- Oracle: `SELECT <seq>.NEXTVAL FROM DUAL`; PostgreSQL (already coded): `SELECT NEXTVAL('<seq>')`.
- Sequences confirmed: `t_trx_history_seq`, `T_LOGIN_AUDIT_SEQ`, `T_ABDM_AUDIT_SEQ`, `T_APPOINTMENT_CHANNEL_SEQ`, `t_outboundhis_seq`, `T_nursePatientDetails_seq`, `t_reorderlevelsummery_seq`, + procs use `T_AdmissionDetails_seq`, `T_PrescriptionDetails_seq`, `T_PaymentModeDetails_seq`, `T_Refunddetail_seq`, `t_emrdetails_seq`, `t_emr_services_seq`, `auto_code_seq`.
- Creation scripts: `generated-procs/SEQ_CREATE_SCRIPT.sql` (`CREATE SEQUENCE <name> START WITH 1 NOCACHE`), `FNCREATENUMBERIDENTITYTABLE.sql` (per-table `<t>_seq`, CACHE 20).
- No `@GeneratedValue`, no IDENTITY columns, no triggers.

**PG recommendation:** recreate one sequence per table (`CREATE SEQUENCE IF NOT EXISTS "<table>_seq"`), keep the existing `NEXTVAL('seq')` calls working (they already exist in code); or migrate to `BIGSERIAL`/identity and update the 10 sequence call sites + aspect.

---

## 5. Oracle-Specific SQL Hotspots (migration risk map)

| Hotspot file | Constructs | Risk | Lines |
|---|---|---|---|
| jobs/ApplicationJobs.java | 121 raw SELECTs, 85 TRUNC(, 53 SYSDATE, 14 TO_DATE | High — date math + job SQL | :739, :1136-1164, spread |
| util/HL7Helper.java | 118 SELECTs, 19 NEXTVAL/DUAL, ROWNUM | High — HL7 queue | :246-248, :1824-1826 |
| services/CommonUtilServiceImpl.java | 119 getQ calls, 10 DECODE, TRUNC, TO_DATE, native T_REGISTRATION SQL | High | :720-821, :1001, :1282, :2863-2868 |
| services/BillingCommonUtilServiceImpl.java | 51 getQ, dual-dialect branch, 20 CIMS_ proc calls | High | :110-264, :380 |
| services/BaseServiceImpl.java | MERGE INTO (2), LISTAGG (2), ROWNUM, TO_TIMESTAMP, 4 dual branches | High | :928-945, :566-598 |
| util/SolrUtil.java | DECODE (5), NVL | Medium — Solr sync | — |
| jobs/EHRSyncJobs.java | SYSDATE (8), TRUNC | Medium | :233-303 |
| services/QueueManagmentServiceImpl.java | TRUNC (10), TO_DATE (10) | Medium | — |
| util/RulesEngineUtil.java | ROWNUM (8), DUAL | Medium | — |
| services/RcmServiceImpl / IntegrationServiceImpl / CWFServiceImpl | TRUNC/SYSDATE/NVL/LISTAGG | Medium | :478 (CWF) |
| audit/AuditHelper.java | 40+ raw INSERT INTO T_TRX_HISTORY, 4 sequences | Medium — audit writes | :156-434, :917-961 |

Construct totals (Java): TRUNC 102 · SYSDATE 70 · NEXTVAL 30 · DUAL 27 · DECODE 25 · TO_DATE 24 · ROWNUM 19 · TO_CHAR 6 · NVL 6 · LISTAGG 2 · MERGE INTO 2 · TO_TIMESTAMP 2 · FETCH FIRST 1. All → PG equivalents: `date_trunc`, `now()/current_date`, `CASE`, `to_date/to_char` (mostly compatible), `LIMIT`, `string_agg`, `INSERT…ON CONFLICT`.

---

## 6. Stored Procedures / Triggers / Sequences Inventory

| Artifact | Count | Notes |
|---|---|---|
| Procedures | 703 (646 CIMS_* +57 RPT*/GET*/DECODE* etc) | Oracle PL/SQL, `SYS_REFCURSOR` OUT in 473, `IN OUT` in 389, EXCEPTION in 504 | 719 files on disk (`generated-procs/`): 703 procedures +16 functions =719; 692/720 was arithmetic drift (692+17+11=720≠719) |
| Functions | 16 | base64 encode/decode ×4, CONVERT_CHARTONUM, GETFULLNAMEFROMDOCTORID1, FNCREATENUMBERIDENTITYTABLE… (16 distinct `CREATE FUNCTION` on disk) |
| Packages | 0 separate files | 11 was miscount — no `CREATE PACKAGE` files on disk; package bodies are inside procedure files or not shipped |
| Triggers | **0** | none anywhere |
| Sequence DDL scripts | 4 files | generator scripts (SEQ_CREATE_SCRIPT / SEQ_DROP_SCRIPT / FNCREATENUMBERIDENTITYTABLE / EMP_CSV) |
| Live-called from Java | 30 | via `namedStoredProcQ()` (BillingCommonUtilServiceImpl, CommonUtilServiceImpl, ApplicationJobs, InventoryJobSchedule, SolrUtil, LinenLaundryServiceImpl) |

Proc-side Oracle usage: ROWNUM 486 · TO_CHAR 474 · SYSDATE 287 · NEXTVAL 190 · NVL 84 · TRUNC 17 · TO_DATE 11 · DECODE 6 · CONNECT BY 1.

**PG mapping:** port the 30 live procs to PL/pgSQL functions; generate the other ~689 later or leave as reference (risk: they also reveal legacy column names that differ from current entities — case sensitivity!).

---

## 7. Cross-Table Dependency Graph (top shared)

```
T_Encounter (52) ──┬─ M_UNITMASTER (45)
                   ├─ M_DEPARTMENTMASTER (32)      M_BEDMASTER (26) ── T_ADMISSION (native)
                   ├─ T_USERDETAILS (25)           M_ITEMMASTER (18) ── T_ITEMSTOCK (15) ── T_ITEMCLINIC (17)
                   ├─ M_DOCTORMASTER (16)          M_SERVICEMASTER (15) ── T_TARIFFSERVICEMASTER ── T_SURGCHARGES
                   ├─ T_PATIENT* (~30 tables)      T_SPONSOR (18) ── billing
                   └─ T_REGISTRATION(-DETAILS) [native only] ── T_OutboundHIS / T_Registration_Int
T_OUTBOUNDHIS (47) ── HL7/EHR/EMR integrations
M_UNITMASTER (45) ── tenants (UnitID filters nearly every query)
district_mst (22) ── CommonUtilServiceImpl registration lookups
T_LINENREQUEST (20) / T_BIOMEDICALWASTE (18) / MEDIA (65 ejs)
```

Shared-root risk: M_UNITMASTER (multi-unit tenancy via `UnitID`), T_Encounter + T_REGISTRATION family are the dependency hubs — migration order and data-consistency checks should center on them.

---

## 8. Initial PostgreSQL Mapping Recommendations

| Table group | PG strategy | Notes |
|---|---|---|
| M_* masters (~609 files, 570 `M_*` pattern) | 1:1 tables, quoted case preserved (`"M_ADMISSIONTYPE"`); `BIGSERIAL` NOT allowed — keep `<table>_seq` sequences to satisfy existing `NEXTVAL('<t>_seq')` code | Auto-create sequences `CREATE SEQUENCE "<t>_seq"` |
| T_* transactionals (~165 files, 130 `T_*` pattern) | same | `TIMESTAMP` (without tz) — app writes GMT via Java; keep as `TIMESTAMP` |
| T_Encounter / T_REGISTRATION family | same + add indexes on (UnitID, AddedDateTime), (MRNO), (PatientID) | Highest traffic; verify row counts first |
| STATUS/Boolean | `Boolean` → `boolean`; char `'1'/'0'` → `char(1)` or `boolean` — audit each entity (`STATUS` Boolean in MBase, `OCCUPIED` char in M_BEDMASTER, `ISDEATH`…) | Use PG `boolean` where Java `Boolean`, keep `char(1)` where procs write `'1'/'0'` |
| Audit columns | keep `ADDEDBY/ADDEDDATETIME/UPDATEDBY/UPDATEDDATETIME/SYNCHRONIZED` names | Some Oracle `NUMBER` → PG `bigint` |
| Base64 PII columns | `CLOB` → `text`; `NUMBER` → `numeric(38)`/`bigint` | Preserve encode/decode; `phi.encryption.enabled=false` today |
| EJS (BigInteger IDs) | `numeric(38)` or `bigint` | 57 tables |
| Aasmaan (default naming) | verify real names in DB (`AASMAANCASE`?) before mapping | `@Table` commented out — recommend adding explicit `@Table` |
| Sequences | port all: `CREATE SEQUENCE "<t>_seq" START WITH <last_value+1> CACHE 20` | Set start values from Oracle `LAST_NUMBER` to avoid collisions |
| 30 live CIMS_* procs | port to `plpgsql` functions with `REFCURSOR` OUT | EclipseLink `StoredProcedureQuery` works with PG functions |
| Text search | Solr external (not in DB) — no action | |
| DUAL / ROWNUM / TRUNC / MERGE / LISTAGG / DECODE | rewrite per §5 table | ~400 sites |

---

## 9. Numbers at a Glance

- Entities: ~898 (`@Entity` in models + 21 DTO-entities)
- Tables: 808 explicit `@Table` + ~35 convention = **~843**
- Sequences referenced: 7+ in Java, 15+ in procs; generator scripts for the rest
- Stored procs: 692 (+17 functions, +11 packages); **0 triggers**
- Native SQL call sites: ~436 `getQ(` in services; ~2000 raw `"SELECT`/SQL string literals
- Dual-dialect branches: 9 (already PG-aware)
- Services: 155 files; Jobs: 5
