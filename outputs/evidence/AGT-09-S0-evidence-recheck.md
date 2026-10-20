# AGT-09-S0 — Legacy Evidence Gate Recheck (Wave 0, Slice S0)

**Agent**: AGT-09 (Legacy Evidence & Data Migration Engineer) · **Date**: 2026-08-14 (recheck round)
**Scope**: Re-test BLK-001/BLK-002 blockers; refresh DDL column-map, CRUD semantics and sequence evidence from the read-only legacy tree `/mnt/d/ihms` (D:\ihms). **No legacy data was modified** — connectivity probes were TCP-only (timeout ≤ 2 s), all source/SQL inspection read-only.
**Report path**: `outputs/evidence/AGT-09-S0-evidence-recheck.md`

---

## 1. Blocker re-test results

TCP probes (connect only; no Oracle session opened, no app requests issued). All timeouts short (2.0 s).

| # | Target | Endpoint | Result | Detail |
|---|---|---|---|---|
| 1 | Oracle (configured legacy JDBC) | `10.10.7.63:1521` | **UNREACHABLE** | TimeoutError (2.0 s) — host/port not accepting TCP |
| 2 | Legacy app HTTP | `10.10.7.63:8080` | **UNREACHABLE** | TimeoutError (2.0 s) |
| 3 | Legacy app HTTP | `10.10.7.63:80` | **UNREACHABLE** | TimeoutError (2.0 s) |
| 4 | Oracle localhost | `localhost:1521` | **UNREACHABLE** | Connection refused (Errno 111) — nothing listening |
| 5 | Oracle loopback | `127.0.0.1:1521` | **UNREACHABLE** | Connection refused (Errno 111) |
| 6 | Legacy app localhost | `localhost:8080` | **UNREACHABLE** | Connection refused (Errno 111) |
| 7 | Legacy app loopback | `127.0.0.1:8080` | **UNREACHABLE** | Connection refused (Errno 111) |
| 8 | HTTP localhost | `localhost:80` | **UNREACHABLE** | Connection refused (Errno 111) |

**Process/port corroboration** (`ss -tlnp`): no listener on 1521/8080/80; no legacy `com.ohum` JVM process running (only containerized Zookeeper/Kafka/Solr + dev PG-5432/Kafka-9092/Solr-9983).

### Verdict
- **BLK-001 (no Oracle access) — STILL BLOCKED / CONFIRMED.** `10.10.7.63:1521/orcl` (the active `spring.datasource.url` in `D:\ihms\application.properties:77`, user `IHMSDOCITDEVMODS_PROD_V1`, committed password placeholder `<redacted>`) is not reachable from this machine. MIG-001 live-DDL / `ALL_SEQUENCES.LAST_NUMBER` capture items remain deferred.
- **BLK-002 (no legacy runtime) — STILL BLOCKED / CONFIRMED, with a nuance (see §6 CONTR-C).** No live instance to capture contract fixtures. Deepening evidence: `/mnt/d/ihms/logs/*` prove the legacy monolith **ran locally on this host until 2026-04-30 18:15** (Tomcat `:8080`, context `/IHMS`, `Started App in 1717.925s`; last scheduled jobs/HTTP hits 2026-04-30 18:15:52) — i.e., the previously-running runtime is gone now, not never-present. Contract shapes remain source-derived (per BLK-002 workaround).

---

## 2. DDL column-map verification — `V1__init_ihms_master.sql` vs legacy JPA entities

Sources: legacy entities `D:\ihms\src\main\java\com\ohum\models\masters\{Unitmaster,Departmentmaster,Companymaster}.java` (note: the master models live under `src/main/java/...` — `/mnt/d/ihms/apps/*` contains only build/JS app shells, no Java) + base class `MBase.java`; target DDL `services/reference-service/src/main/resources/db/migration/V1__init_ihms_master.sql`. Column sets compared **1:1, case-exact** (PostgreSQL quoted-identifier preservation makes case significant).

Base-class columns injected per table by `MBase.java:42-66` (`@Column`): `UnitID, STATUS, CREATEDUNITID, UPDATEDUNITID, ADDEDBY, ADDEDDATETIME, UPDATEDBY, UPDATEDDATETIME, SYNCHRONIZED` (9) + `ID` (PK, `@Id`).

| Class (legacy) | @Table | Java @Column (model) | +MBase+ID | DDL count | Match / Mismatch |
|---|---|---|---|---|---|
| `Unitmaster.java:46` | `M_UNITMASTER` | 97 | 107 | 104 | **MISMATCH — 3 columns missing in V1 DDL**: `ISADMINISTRATIVE` (Unitmaster.java:203), `ISCONFIRMATORY` (:200), `ISOFFLINE` (:206). No DDL-only columns. Case-exact names `VAT_TIN_No` and `apiToken` match DDL exactly (quoted mixed-case preserved). |
| `Departmentmaster.java:45` | `M_DEPARTMENTMASTER` | 29 | 39 | 39 | **MATCH** — all 39 columns 1:1, case-exact (incl. `qmsNursingStationAvailable`), defaults/constraints consistent. |
| `Companymaster.java:37` | `M_COMPANYMASTER` | 27 | 37 | 37 | **MATCH** — all 37 columns 1:1, case-exact. |

**M_UNITMASTER missing-column impact — HIGH**: the 3 absent columns are not dead metadata:
- Referenced by live stored procedures `CIMS_GETCHARGESNEW.sql`, `CIMS_GETCHARGESTEMPNEW.sql`, `CIMS_GETSERVICELISTFORBILL_WITHOUT_COPAY_CONCESSION.sql` (grep hit).
- User-facing in the legacy masters form schema `public/modules/masters/hospital/schemas/unitmaster.json:1475,1519,3819` (`isAdministrative`, `isOffline`, `isConfirmatory`).
- Handled in `Unitmaster.update()` / getters-setters (Java).

Type-level spot-check (no mismatches found in compared rows): DDL `BYTEA` ↔ Java `byte[]` LOGO/REPORTFORMAT; `TIMESTAMP(3)` ↔ `java.sql.Timestamp`; `NUMERIC(19,2)/(19,4)` ↔ `BigDecimal`; `SMALLINT` ↔ `Short` (ISCWFCOVERSHEET); `DATE` ↔ `java.sql.Date` (CONTRACTFROMDATE/TODATE). Headers in `V1` claim sizes are “provisional pending live Oracle DDL dump (MIG-001, BLK-001)” — consistent.

---

## 3. GenericServiceImpl semantics summary (crudget / crudgetAll / crudgetAllAll)

Source: `D:\ihms\src\main\java\com\ohum\services\GenericServiceImpl.java`; routing in `CrudOperationsFilter.java`; pagination primitive `GenericQuery.paginate(start,count)`.

### 3.1 Routing (CrudOperationsFilter)
- Pattern: `<context>/api/rest/<resource>/...` (`CrudOperationsFilter.java:123`); per-resource class resolved from `@CrudApi`; 659 registered CRUD entities.
- `GET /<resource>` (no id) → `crudgetAll(page,count,search,searchFld,claz,fm)` (filter line 558).
- `GET /<resource>/all` → `crudgetAllAll(page,count,search,searchFld,claz,fm)` (line 515).
- `GET /<resource>/<id>` → `crudget(id,claz,fm,null)` (line ~526).
- `allowedFilters`: only request params whose name is in `@CrudApi(allowedFilters)` **and** present as a Java field are collected, then passed through the model’s own `getFilterMap()` (typed conversion; filter lines 497-503, 545-551).
  - `Unitmaster`: `allowedFilters={"id","status"}` → filterMap produces `status:Boolean`.
  - `Departmentmaster`: `allowedFilters={"status","isclinical","isEmergency","isteleconsult","defaultdepartment"}`.
  - `Companymaster`: `allowedFilters={"description","status"}` → `description:String` (exact-equality when used as filter; LIKE only via `search/searchFld`).

### 3.2 `crudget(idStr, claz, filterMap, unitId)` (line 107)
- `id == 0` → error `BEXA003` (“Invalid input data !”, `icu_en_in.properties:9`).
- Base query: `SELECT t FROM <Clazz> t WHERE t.id = :id AND t.unitId = :unitId` (unit defaulted to session user’s unit).
- **Unitmaster special case**: `WHERE t.id = :id` only — **no unitId scoping** (masters are global).
- filterMap keys other than `id`/`unitId` appended as additional `AND t.<key> = :<key>` equalities.

### 3.3 `crudgetAll(pageStr, countStr, searchStr, searchFld, claz, filterMap)` (line 153 → `internalCrudgetAll` :158)
- Parsing: non-numeric `page`/`count` → `BEXA003` (lines 167, 174).
- **Offset/page math**: `page = (count > 0) ? page / count : 0` (line 177); `paginate(page, count)` sets `firstResult = page*count`, `maxResults = count` (`GenericQuery.paginate`). Net effect: the request **`page` param is an OFFSET**, normalized down to the nearest `count`-page boundary (`floor(offset/count)*count`); page size = `count`; `count=0` ⇒ no `maxResults` and offset forced to 0.
- Base query always scoped: `WHERE t.unitId = :unitId` (line 180).
- **Search OR-alignment** (lines 182-223): `searchStr` is a **comma-separated** value list, `searchFld` a **space-separated** field list; they are zipped index-wise (`searchStrs[i] ↔ searchList[i]`, falling back to `searchStrs[0]`). Each field contributes `lower(t.<fld>) LIKE lower('%<val>%')` joined by ` OR ` and wrapped `AND ( ... )` (blank field ⇒ fallback `t.description`).
- **addeddatetime range** (lines 190-210): a `addeddatetime` search value is split on `_` → `LHS_RHS`; both parts ⇒ `AND t.addeddatetime >= 'LHS' AND t.addeddatetime <= 'RHS'`; single part ⇒ `>= 'val'` only. (Code quirk: `lhsrhs[1]` is dereferenced before length is checked — a single date without `_` would raise `ArrayIndexOutOfBoundsException`; the trailing `<=` branch at lines 205-208 is unreachable dead code. Flag for parity test authors.)
- **filterMap** keys (except `unitId`) become `AND t.<key> = :<keyWithDotsReplaced>`; `unitId` defaults to the session user’s unit when absent.
- Ordering/pagination quirks:
  - filterMap present + `searchFld` non-blank → paginate + `ORDER BY t.<searchFld> ASC`.
  - **filterMap present + `searchFld` blank → NO paginate and NO order — full filtered list returned regardless of page/count** (GenericServiceImpl.java:248; paginate+order path at :245-246). Legacy UI relies on client-side paging in that case (e.g., filtered master lists).
  - no filterMap → paginate applied; `Audit` gets `ORDER BY t.id DESC`; other classes get `ORDER BY t.<searchFld> ASC` only when `searchFld` non-blank, else natural order.

### 3.4 `crudgetAllAll(pageStr, countStr, searchStr, searchFld, claz, filterMap)` (line 259 → `internalCrudgetAllAll` :264)
- Same page/count/BEXA003 handling; **no unitId clause** — `SELECT t FROM <Clazz> t` (all units; cross-tenant dump for `Unitmaster`).
- filterMap present → `WHERE t.<k1> = :k1 AND t.<k2> = :k2 …` (named params with `.`→`_`); search = **single value** OR-ed across all space-separated `searchFld` fields (description fallback); paginate always applied; `ORDER BY t.<searchFld> ASC` when non-blank.
- No filterMap + search: `searchFld == "id"` ⇒ literal `WHERE t.id = <value>` (string-concatenated, exact match); otherwise single `lower(t.<fld>) LIKE ...`; no search ⇒ full unqualified table, paginated.

### 3.5 BEXA003 usage (GenericServiceImpl)
`BEXA003 = "Invalid input data !"` thrown for: `crudupdate` null model or id==0 (line 78), `crudget` id==0 (line 114), non-numeric page/count in both getAll variants (167/174, 274/281), `crudremove` non-numeric or zero id (347/350). It is a generic input-validation error reused across 20+ services (AddUser, Mdm, Patient*, Pharmacy, etc.) and **is not a business rule code** — parity implementations should map it to HTTP 400.

---

## 4. Sequences evidence (SEQ_CREATE_SCRIPT.sql + usage sites)

**File**: `/mnt/d/ihms/generated-procs/SEQ_CREATE_SCRIPT.sql` (24 lines, 755 B) — it is a **PL/SQL generator procedure**, not a static sequence list:

- `SELECT 'CREATE SEQUENCE '||SEQUENCE_NAME||' START WITH 1 NOCACHE;' FROM all_sequences WHERE SEQUENCE_OWNER = 'IHMSDOCITDEVMODS_PROD_V1' AND SEQUENCE_NAME LIKE '%SEQ_%'` (lines 4-5), writing one `CREATE SEQUENCE <name> START WITH 1 NOCACHE;` per hit via `UTL_FILE` to `DB_BACKUP:SEQ_CREATE_SCRIPT.SQL`.
- Companion scripts: `SEQ_DROP_SCRIPT.sql` (DROP counterparts), `EMP_CSV.sql` (same generator, different output name), `FNCREATENUMBERIDENTITYTABLE.sql` (generates `<column>_seq` with `CACHE 20`).

**Naming conventions evidenced**:
1. Table-ID sequences: `<TABLE>_SEQ` — `M_UNITMASTER_SEQ` (`MasterCachedServiceImpl.java:5848`), `M_DEPARTMENTMASTER_SEQ` (`:5950`), `M_CompanyMaster_seq` (`CIMS_ADDCOMPANYMASTER.sql:10`) — Oracle folds unquoted names to upper case ⇒ `M_COMPANYMASTER_SEQ`.
2. Per-unit activity sequences: `<ACTIVITY>_AC_SEQ_<unitId>` — generated in `MdmServiceImpl.java:588-636` (e.g. `T_PathOrderBookingDetails_AC_SEQ_<id>`, `T_OPDBill_AC_SEQ_<id>`, …), consumed via `EXECUTE IMMEDIATE 'SELECT <activity>_AC_SEQ_<unit> ...'` in `CIMS_GETLATESTAUTOCODEANDINCREMETITBYONE.sql:27,51,78`.
3. Unit-based OPD/IPD visit-number sequences: `T_UNITBASED_OPDNO_SEQ_<unitId>` / `T_UNITBASED_IPDNO_SEQ_<unitId>` (`Unitmaster.java:1503-1509` + `MdmServiceImpl.java:586,597`).

**LAST_NUMBER capture mechanism — FINDING (contradicts assumptions):**
- **There is NO LAST_NUMBER capture mechanism anywhere in the read-only tree.** The generator explicitly queries `SEQUENCE_NAME` only and regenerates every matching sequence `START WITH 1 NOCACHE` — i.e., the shipped capture script **destroys** high-water marks (any rerun resets to 1) rather than recording them.
- Correct capture requires a live-DB query: `SELECT SEQUENCE_NAME, LAST_NUMBER, INCREMENT_BY, CACHE_SIZE FROM ALL_SEQUENCES WHERE SEQUENCE_OWNER='IHMSDOCITDEVMODS_PROD_V1'` (`ALL_SEQUENCES.LAST_NUMBER`) — impossible while BLK-001 holds. **Migration risk:** legacy master PKs are assigned manually from these sequences (`id = seq.nextval` in `MasterCachedServiceImpl`/procs, e.g. `CIMS_ADDCOMPANYMASTER.sql:10`), so the target `ihms_master` sequences must be `RESTART`-ed to `max(id)+1` per migrated table at cutover; `START WITH 1` (current V1 default) will collide with migrated rows.

**V1 target sequences** (`V1__init_ihms_master.sql`, tail): `M_UNITMASTER_SEQ`, `M_DEPARTMENTMASTER_SEQ`, `M_COMPANYMASTER_SEQ` all `START WITH 1 ... NO CYCLE` — names match legacy convention ✓; start values must be fixed at cutover (see above).

---

## 5. Row-count / stats evidence

- **No row-count or volume statistics exist in the read-only tree** for `M_UNITMASTER`, `M_DEPARTMENTMASTER`, `M_COMPANYMASTER` (no dumps, no count queries in logs — `grep M_UNITMASTER` in `IHMS.log` = 0 hits; docs contain no table row estimates).
- The only runtime-ish indicators are legacy log artifacts: memory banners (`M_B_API 4432MB|4889MB|7196MB`), scheduled-job cadence (DB Lock list, Item syncing, ABDM pushes, Auto Indent jobs) and HTTP hits (`GET /IHMS/api/rest/master/showSSOLoginPage`) up to 2026-04-30 18:15 — confirming a populated, actively-used deployment existed, but **not** row counts.
- **Row counts remain BLK-001-gated** (W0 data-extraction job against live Oracle; read-only `SELECT COUNT(*)` per table at MIG-001). No writes were performed.

---

## 6. NEW evidence vs existing docs — explicit contradictions

| ID | Existing doc claim | New evidence | Verdict |
|---|---|---|---|
| CONTR-A | `V1__init_ihms_master.sql` header: “Tables are 1:1 … M_UNITMASTER ← Unitmaster.java (MBase + ~90 columns, verified 2026-08-14)” | Exact 1:1 audit: M_UNITMASTER has **97 model columns (107 with MBase+ID)** in Java vs **104 in V1 DDL**; `ISADMINISTRATIVE`, `ISCONFIRMATORY`, `ISOFFLINE` absent from DDL while present in Java (Unitmaster.java:200-206), used by 3 live procs and the unitmaster frontend schema | **CONTRADICTION — DDL is incomplete for M_UNITMASTER** (3 columns to add before cutover; department & company tables verified 1:1). |
| CONTR-B | `V1` sequence comment “convention `<TABLE>_SEQ` per legacy SEQ_CREATE_SCRIPT / evidence `T_EQUIPMENTCHECKLIST_SEQ`, `m_vitaltypes_seq`; exact names verified at MIG-001” | Names independently confirmed (`M_UNITMASTER_SEQ`, `M_DEPARTMENTMASTER_SEQ`, `M_COMPANYMASTER_SEQ` **do** exist and are actively used for manual PK assignment). Conventional names hold ✓ — but **no verification source exists in-tree** for LAST_NUMBER, and the only in-tree “capture script” discards high-water marks (`START WITH 1 NOCACHE`) | **PARTIAL CONTRADICTION** — naming OK; “LAST_NUMBER capture from generated-procs” is impossible; capture must come from live DB (defer to MIG-001). |
| CONTR-C | BLK-002 “No running legacy instance → no contract fixture capture” (implies absent) | Logs `/mnt/d/ihms/logs/IHMS.log` + dated rotation show the legacy app **ran on this host through 2026-04-30 18:15** (Tomcat :8080, context `/IHMS`, `Started App in 1717.925 s`); files were copied from the origin dev box (`D:\RISL\ihms`, `C:\Users\Amitk\.m2`) | **NUANCE, not a blocker change** — currently no instance (re-test confirms), but a local runtime did exist recently; if a snapshot/VDI of that host is recoverable, contract fixtures may be obtainable sooner than network access to 10.10.7.63. |
| CONTR-D | (implicit) generic CRUD list endpoint paginates uniformly | `crudgetAll` **drops pagination** whenever `filterMap` is non-empty and `searchFld` blank (GenericServiceImpl.java:245-248) | **NEW behavior fact** for MIG-002 parity tests — filtered master lists return the complete set; client does local paging. |
| CONTR-E | (implicit) `page` param = page index | `page` is an **offset** normalized to page boundaries (`page/count`, then `firstResult=page*count`) — mirrors FAUG DataTables server-side `iDisplayStart/iDisplayLength` convention | **NEW behavior fact** — port must keep offset semantics, not naive `page*size`. |
| CONTR-F | (none previously) | `crudget` **bypasses unitId scope for Unitmaster** (global single-row fetch), while `crudgetAll` is unit-scoped and `crudgetAllAll` is fully unscoped across units | **NEW behavior fact** — replication contract for masters API (unit-agnostic reads). |

**Blocker verdicts (for parent):** BLK-001 — UNREACHABLE (still blocked); BLK-002 — UNREACHABLE (still blocked, with CONTR-C nuance); Evidence verdict — **CONTRADICTIONS** (3 DDL gaps on M_UNITMASTER + sequence LAST_NUMBER capture limitation + behavior facts CONTR-D/F).

---

## 7. Method & provenance (read-only; no writes to legacy data)

- Connectivity: raw TCP sockets, 2.0 s timeouts, 8 targets; corroborated with `ss -tlnp` and `ps`.
- DDL audit: regex-extracted `@Column(name=...)` from the 3 master models + `MBase.java` (9 base cols + `ID`), parsed all `CREATE TABLE` statements from `V1__init_ihms_master.sql` (104/39/37 columns), diffed **case-exact** (quoted identifiers in PG make case meaningful); confirmed `VAT_TIN_No`/`apiToken`/`qmsNursingStationAvailable` mixed-case names match exactly.
- Semantics: full read of `GenericServiceImpl.java` (18,295 B) and `CrudOperationsFilter.java` (39,508 B) with line numbers; `GenericQuery.paginate` primitive inspected.
- Sequences: full read of `SEQ_CREATE_SCRIPT.sql`, `SEQ_DROP_SCRIPT.sql`, `EMP_CSV.sql`, plus all `<table>_seq` usage sites in Java/procs.
- Files inspected are under `/mnt/d/ihms` (D:\ihms, read-only legacy reference) and `services/reference-service/.../V1__init_ihms_master.sql` (read-only). **No legacy data modified; no writes executed.**
