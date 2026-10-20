# iHMS / OHUM PAS NG — PostgreSQL Migration Design (Oracle → PG)

**Status: PRELIMINARY — synthesized from completed discovery (no re-exploration).**
**Sources:** `05-database-catalog.md`, `_discovery/agent-05/findings.md`, `10-target-architecture.md`, `_discovery/agent-10/findings.md`, `07-integrations-and-search.md`, `06-business-rules-catalog.md`.
**Method:** evidence-first; every mapping cites legacy evidence (file:line). **No naive type conversion** — every type decision is driven by usage semantics, proc cross-talk, and business rules. Unknowns are marked `[UNKNOWN]`. Contradictions between source docs are flagged inline and in §8.

---

## 0. Non-negotiable constraints (evidence)

1. **Solr is the patient-identity query path and stays outside PG** — no PG full-text/search design is proposed (§5). `07 §1` ("SOLR MUST REMAIN"), ABDM consent discovery queries Solr (`SolrUtil.searchABHAPatientHIDInSolr` :2150).
2. **Legacy IDs must continue** — MRNO, IPDNO/OPDNo (formatted), bill no, HL7 control IDs are referenced externally (`10 §3.2`; `BR-PAT-001`, `BR-OPD-003`, `BR-IPD-004`, `BR-BILL-001` in 06). Sequence continuity is mandatory.
3. **The DB layer is proc-centric** — 719 Oracle PL/SQL artifacts (`generated-procs/`), 30 live-called from Java (`agent-05 §6.1`). PG must host ported PL/pgSQL or replacement logic for the live 30.
4. **~2,000 raw SQL string literals are Oracle-only** — only 9 dual-dialect branch sites exist (`agent-05 §5.3`). PG schema must match what rewritten SQL expects, not the reverse.

---

## 1. Domain Ownership Model (legacy table groups → target service PG schema)

Schema-per-service (`10 §3.1`). Schema naming: `ihms_<service>` (preliminary). Rules: **no shared tables across PG schemas**; borrowed data = read-model fed by events or REST API (`10 §2.2`).

| Target service (# in 10-doc) | PG schema | Owned legacy table groups (evidence: 05 §3, agent-05 §8) | Notes / conflicts |
|---|---|---|---|
| 1 Patient Identity & Registration | `ihms_patient` | T_REGISTRATION family (native-only!), T_REGISTRATION_DETAILS, T_PATIENTSPONSOR, T_PATIENT_ABDM(+_CC,_SHARETOKEN), T_PATIENT_CONSENT_REQUESTS_ABDM(+_DATA), T_REGISTRATIONQUEUE, T_PATIENT* (~30 transactional patient tables: allergies, advice, diagnosis, notes, vitals, feedback, e-order, documents), T_NA_ITEMDETAILS (dto entity) | **T_REGISTRATION schema is not recoverable from Java** — native-SQL-only (`agent-05 §5.4`); DDL must be dumped from Oracle |
| 2 Appointment & Encounter (OPD) | `ihms_encounter` | T_ENCOUNTER, T_APPOINTMENT, T_PATIENTTOKEN (`t_patienttokens`), T_DOCTORCHECKIN_QUEUEROOM, doctor queue/cabin tables, T_APPOINTMENT_CHANNEL (audit), T_coronaStats? | T_ENCOUNTER is the highest-shared table (52 refs) — cutover-critical (`05 §7`) |
| 3 ADT & Bed | `ihms_adt` | T_ADMISSION, `t_admission` (native), T_BEDRESERVATION, T_TRANSFERREQUEST, `t_transfer` (native), T_PATIENTDISCHARGEDATA, T_MLCDETAILS, T_ATTENDANTPASS, T_ADMISSIONDETAILS (proc-side) | M_BEDMASTER/M_WARDMASTER conflict with Reference — see §1.1 |
| 4 Ordering & Prescription (Clinical) | `ihms_clinical` | T_PATIENTADVICE, T_PATIENTEORDER, T_PATIENTMEDICATION, T_PATIENTNOTE, T_EMRDETAILS, T_EMRSERVICES, T_CONSULTORDER, T_PRESCRIPTIONNOTES | |
| 5 Billing & Settlement | `ihms_billing` | T_BILL, T_BILLDETAILS, T_CHARGE, T_CHARGES_TEMP, T_DOCTORBILL, T_DOCTORSHARE, T_CONCESSION, T_CREDITNOTE + `t_creditNote`/`t_creditNoteDetails` (case-mixed!), T_FREQ_ITEMS | Bill no. numbering via auto-code sequences (`BR-BILL-001`) |
| 6 Payments & Refunds | `ihms_payment` | T_PAYMENT, T_PAYMENTMODEDETAILS, T_ADVANCE, T_REFUND, T_RECEIPT, T_ITEMSALE? | T_ITEMSALE written by Pharmacy services — keep in Pharmacy (9); billing/payment read via API |
| 7 Lab (Pathology) | `ihms_lab` | T_PATHORDERBOOKING(+DETAIL), T_PATHOPATIENTPARAMETERDETAILS, T_MACHINECONFIGURATION, T_PATIENTREQUISITION, `t_patientrequisition` (native) | patho masters (M_PATHOTESTMASTER…) stay in Reference; borrow via API |
| 8 Radiology | `ihms_rad` | T_RADORDERBOOKING(+DETAILS), rad masters | |
| 9 Pharmacy & Dispensing | `ihms_pharmacy` | T_PRESCRIPTIONMASTER, T_ITEMSALE, T_PRESCRIPTIONDISPENSEDETAILS, T_ITEMCOMPOUNDDETAILS, ward picklist tables | **Stock dual-write hotspot**: Pharmacy calls CIMS_ADDITEMSTOCK (`PharmacyServiceImpl.java:2140`) — stock tables stay in Inventory; Pharmacy uses stock API (§1.1) |
| 10 Inventory & Procurement | `ihms_inventory` | M_ITEMMASTER, T_ITEMSTOCK, T_ITEMBATCHMASTER, T_PURCHASEORDER, T_GRN*, T_INDENT, T_SUPPLIER, T_RATECONTRACT, T_QUOTATION, T_CONSIGNMENTGRN, T_ITEMCLINIC, T_OPENINGBALANCE, T_REORDERANDSAFETYMASTER, T_POISSUE* (txt temp) | M_ITEMMASTER ownership conflict — see §1.1 |
| 11 OT & Surgery | `ihms_ot` | T_OTSCHEDULE(+DETAILS), T_SURGERYDETAILS, procedure masters, T_ANESTHESIANOTES, T_PROCEDURECSSD* (CSSD cross-refs) | |
| 12 Nursing & Inpatient Care | `ihms_nursing` | T_NURSENOTE, T_PATIENTVITALS, T_NURSETASKDETAILS, T_INTAKEOUTPUT, T_NURSEPATIENTDETAILS, nursing station masters, IpdRoster | |
| 13 Blood Bank | `ihms_bloodbank` | T_BLOODBAG, T_BLOODCOMPONENT, T_BLOODREQUISITION, T_BLOODDONOR, T_BLOODDONATIONCAMP, donor masters (21 tables) | |
| 14 Diet | `ihms_diet` | T_DIETORDER, T_DIETITEMSCHEDULEDETAILS, T_TODAYDIETITEMMENU, diet/food masters | |
| 15 CSSD | `ihms_cssd` | T_CSSDKITFORMATION, T_CSSDKITISSUEITEM, T_STERILIZATION*, T_PROCEDURECSSD* | |
| 16 Linen & Laundry | `ihms_linen` | T_LINENREQUEST(+DETAILS), T_LINENCATEGORY, T_LINENDETAIL | |
| 17 Transport | `ihms_transport` | T_TRANSPORTREQUEST(+DETAILS), T_VEHICLEMAINTENANCE, M_AMBULANCEMASTER, M_DRIVERMASTER | |
| 18 Equipment Management | `ihms_equipment` | T_EQUIPMENT, T_EQPCOMPLAINT(+RESOLUTION), T_EQPPM*, warranty/insurance/AMC tables, M_EQUIPMENTDETAIL? | |
| 19 Bio-Medical Waste | `ihms_bmw` | T_BIOMEDICALWASTE(+DETAILS), T_WASTEBAGMASTER | |
| 20 Grievance & Feedback | `ihms_grievance` | T_PATIENTCOMPLAINTS, T_PATIENTGRIEVANCE*, T_COMPLAINTMASTER, T_FEEDBACKTEMPLATE | |
| 21 Reference Master Data | `ihms_master` | M_* masters (~582 tables: service, tariff, unit, department, company, tax, store, bank, ICD10, health factor, patho/rad/OT/drug masters, M_MRNSERIES), CONFIG_APPLICATION, block_mst/city_mst/district_mst, auto_code_seq (numbering) | **God-node** (514 graph edges, `MasterCachedServiceImpl`); Redis cache keys owned here |
| 22 Security & IAM | `ihms_iam` | T_USERDETAILS, T_USERROLES, T_USERROLEMASTER, T_USERUNITDETAILS, T_USERSTOREDETAILS, T_USERWARDDETAILS, M_MENUMASTER, M_MODULEPERMISSIONMASTER, t_role_enabledpermission, T_USER_FACEAUTH?, T_LOGIN_AUDIT?, user_detail_temp | M_STAFFMASTER/M_DOCTORMASTER conflict — see §1.1 |
| 23 RCM & Claims | `ihms_rcm` | T_ELIGIBILITYCHECK, T_ADMISSION_TID (a.k.a. T_ADMISSIONTIDDETAILS — naming variant, §8), T_NHCX_REQUEST(+_DETAILS) | |
| 24 RMRS & PMRD (Treasury) | `ihms_rmrs` | T_RMRSCOLLECTION, T_AUTHORITYMASTER, T_HEADSMASTER, T_LEDGERMASTER, T_ACCOUNTING* (14), s_servicewisecollection, s_userwisecollection | |
| 25 Interop & Notifications | `ihms_interop` | T_OUTBOUNDHIS, T_INBOUNDHIS, T_ABDMREQUESTQUEUE, T_WHATSAPPMESSAGEQUEUE/STATUS, T_EHRJOBDETAILS, T_ABDM_AUDIT, T_BENEFICIARY_ENROLMENT_IHMS? | Queue tables are the **legacy outbox** — replaced by Kafka topics (see KAFKA-EVENT-CATALOG.md); T_ABDM_AUDIT audit home TBD |
| 26 Reporting & Analytics | `ihms_reporting` | T_REPORTMASTER, stats/archive tables (T_STATS_ARCHIVED? `[UNKNOWN]`), PDF blob tables | read-model only |
| 27 Search & Index | *(no schema — owns Solr collections)* | — | Solr `patient`, `encounter`, `item`, `icd10cm`, `healthfactor` (6th `service` collection is orphaned — `07 §1.2`) |
| 28 Govt Programs (EJS/Aasmaan/Chiranjeevi) | `ihms_govt` | ANDROID_*, ES_*, MASTER_* (states/districts/blocks/villages/PHCs/AWCs/GPs), MEDIA*, USER_*/PROFILE/INDICATORS (ejs), AASMAAN* (35, **verify real names**), BENEFICIARY_ENROLMENT_IHMS, T_REFRENCETID | Aasmaan `@Table` commented out → default uppercase names (`agent-05 §2.2.6`) |
| 29 Queue & Kiosk | `ihms_queue` | T_PATIENTQUEUE, T_OPDSTATIONMASTER, T_TABLETMAPPING | |
| (cross-cutting) Audit | `ihms_audit` *(decision)* | T_TRX_HISTORY, T_LOGIN_AUDIT, T_ABDM_AUDIT, T_APPOINTMENT_CHANNEL, T_AUDIT (`08 §1`: 59 service files, 568 audit call sites) | Open Q7 in `10 §7`; alternative: audit tables stay with owning services |

### 1.1 Ownership conflicts between 05/10 docs (must be resolved before DDL)

| Conflict | Doc A | Doc B | Impact | Recommendation |
|---|---|---|---|---|
| M_BEDMASTER / M_WARDMASTER | 10 row 3: ADT owns "bed/ward/room/cabin masters" | 10 row 21: Reference owns "M_* (609 entities)" | Occupancy flag `OCCUPIED` + `BedStatusId` written by ADT procs (`CIMS_ADDADMISSIONDETAILS.sql:108`, `PatientTransferDischargeServiceImpl.java:789-790`) | ADT owns **operational bed state**; Reference owns definitional M_* via API. Cleanest: move M_BEDMASTER wholesale to `ihms_adt` (hot-write table) and keep the remaining ~580 in `ihms_master` |
| M_ITEMMASTER | 10 row 10: Inventory owns "M_ITEMMASTER, T_ITEMSTOCK…" | 10 row 21: Reference owns "M_* (609 entities)" | Item search = Solr `item` collection; itemMaster Redis cache key "owned by Reference actually" (10 row 10 note) | Inventory owns M_ITEMMASTER + T_ITEMSTOCK (+batch); Reference owns the `itemMaster` **cache key contract** and invalidates via MasterDataChanged event |
| M_STAFFMASTER / M_DOCTORMASTER | 10 row 22: IAM owns them ("profiles") | 10 row 21: Reference owns all M_* | Staff/doctor are identity + clinical reference data | IAM owns profile tables; Reference serves read-master API; duplicate-write risk — decide single owner (recommend IAM for M_STAFFMASTER/M_DOCTORMASTER, Reference keeps rest) |
| T_ITEMSALE | 05 §3.2 lists under transactionals (generic) | 10 row 9: Pharmacy owns T_ITEMSALE | Billing reads item sales for bill settlement | Pharmacy owns; Billing consumes via API/event |
| T_APPOINTMENT_CHANNEL / audit tables | 05 §3.2: "audit tables" | 10 rows: not assigned | — | Decision pending (§8) |

---

## 2. Type Mapping Table (conversion notes, NOT naive)

| # | Oracle (evidence) | PostgreSQL | Notes / semantics evidence |
|---|---|---|---|
| 1 | `NUMBER(19)` PK/FK (Long entity IDs; `number(19)` proc params — `CIMS_ADDADMISSIONINFORMATION.sql`) | `BIGINT` | All manual-PK Longs. EJS module uses `BigInteger` ids (`EjsBase.java:22`) → `numeric(38)` or `BIGINT` (57 tables; audit overflow risk at 9.2e18 — unlikely, but EJS tables predate: **use `numeric(38)` for EJS** or confirm max id `[UNKNOWN]`) |
| 2 | `NUMBER(p,s)` money / rates / qty (BigDecimal; proc `BigDecimal scale 4` rounding — `agent-05 §6.1`) | `NUMERIC(p,s)` — money `NUMERIC(19,2)` (`10 §3.3`); intermediate computation `NUMERIC(19,4)` | **Never FLOAT/DOUBLE** — doctor-share rounding is `CEILING` in DB / `HALF_UP` on DTO (`BR-BILL-002`), tax = rate×pct/100 (`BR-BILL-004`) |
| 3 | `NUMBER(10)` counters / flags (`v_IsUsedForANC number(10)` — CIMS_ADDADMISSIONINFORMATION.sql) | `INTEGER` / `SMALLINT` | Numeric **state codes are load-bearing**: VisitStatus 3/4/6/9/10/11/12, bill Status=1, blood requisition 1/2/3/10, donor DNR 1–11, NHCX 0–8 (`06 §A`). Recommend `SMALLINT` + `CHECK` + documented enum map (`10 §3.5`). **Do not convert to boolean.** |
| 4 | `VARCHAR2(n)` (proc params `varchar2(200/100/20)` — `agent-10 §5 row 3`) | `VARCHAR(n)`; oversized → `TEXT` | Size audit needed: names/addresses ≤500 in practice (`agent-10 §5 row 3`). `[UNKNOWN]` full size histogram — extract `ALL_TAB_COLUMNS` before DDL. Watch: Oracle `VARCHAR2` empty-string = NULL semantics vs PG empty string — audit `''` comparisons in SQL literals |
| 5 | `CLOB` (12 `@Lob` entities: FileDetails, EquipmentDetailDocs, WarrantyList, Equipmentdetail, Equipinsurancedetails, ImageAnnotationMaster… — `agent-10 §5 row 4`) | `TEXT` | **⚠ Verify base64 PII columns**: `@EncodeAsBase64`/`Base64Data` fields decode in GenericDAO:566-578; procs `ENCODEFUNCTION/DECODEFUNCTION/DECODEUTF8ENCODEDSTRING/DECODEBASE64` on T_Registration_Int (`MigratePatientRegistrationUtil.java:36`). base64 is ASCII-safe → `TEXT` is fine; **never `bytea` unless a raw CLOB stores binary** (`[UNKNOWN]` — confirm). PG `TEXT` has no size limit; Oracle CLOB max 4 GB → chunked inserts in migration only |
| 6 | `DATE` | **Semantic split — the app mixes date-only and datetime** | Evidence of time semantics: `TRUNC(ADDEDDATETIME)` (ApplicationJobs:594), TRUNC(SYSDATE) comparisons (102 hits), proc `p_date timestamp` params (`agent-10 §5 row 5`). Rule: **date-only columns** (DOB, RegistrationDate, blood-bag expiry, diet menu date) → `DATE`; **all audit/event timestamps** (ADDEDDATETIME, UPDATEDDATETIME, ADDEDBY, bill dates, encounter dates, charge dates) → `TIMESTAMP(3)` (procs already use TIMESTAMP(3)) |
| 7 | `TIMESTAMP(3)` | `TIMESTAMP(3)` **without time zone** | App writes GMT via Java (`05 §8`); `BR-BILL-004` GST boundary `transDate ≥ 2017-07-01 00:00:00` (BaseServiceImpl:521-533) — document the GMT convention; mixing server-local + GMT breaks this. `[UNKNOWN]` whether all writes are truly GMT |
| 8 | `CHAR(1)` / `NUMBER` booleans — **inconsistent** | `BOOLEAN` **only where Java `Boolean` and no proc cross-talk**; keep `CHAR(1)` where procs write `'1'/'0'` | Evidence of the mess: MBase `STATUS` is Java `Boolean` mapped to Oracle NUMBER (`MBase.java:42-67`); procs write `char` `'1'/'0'` (`CIMS_ADDADMISSIONINFORMATION.sql`: `p_isbaby char`, `OCCUPIED='1'` `CIMS_ADDADMISSIONDETAILS.sql:108`, `IsClosed='1'`, `p_isdelete char`, `p_iscancelled char`); PG branch already uses `'t'/'f'`/`'true'/'false'` (BaseServiceImpl:577-598). Per-table decision matrix required; **PG `boolean` rejects `'1'`/'0'` implicit casts** — SQL literals must be rewritten consistently |
| 9 | `NUMBER(1)`-style flags in status columns (SOLR_STATUS, ISACTIVE, IS_DELETED, ISSUESTATUS…) | `SMALLINT` + CHECK (0/1) | 39 direct `STATUS` + ~50 domain status columns (`agent-05 §4`); Solr drainers read `SOLR_STATUS=0` — keep integer semantics |
| 10 | `SEQUENCE.<t>_seq.NEXTVAL FROM DUAL` | **Preserve per-table explicit sequences** — `CREATE SEQUENCE "<t>_seq" START WITH <last_number> CACHE 20`; **new tables** may use `BIGSERIAL`/`IDENTITY` (`10 §3.2`) | See §2.1 (the dual-dialect code already calls `NEXTVAL('<t>_seq')` — ServiceLayerAspect:727, GenericDAO:857, BaseServiceImpl:566-574) |
| 11 | `BigInteger` EJS ids | `NUMERIC(38)` | see row 1 |
| 12 | Column names (ADDEDBY/ADDEDDATETIME/…) | Keep legacy names in quoted form (§4) | Audit columns injected into ≥750 tables via MBase (`05 §2`) |

### 2.1 Sequence strategy: IDENTITY vs per-service sequences

**Decision: keep explicit per-table sequences for all legacy tables; `IDENTITY` only for brand-new tables.**

Evidence:
- ID assignment is **dynamic per table name** in AOP: `ServiceLayerAspect.java:723` `String seq = tab.name() + "_seq";` → Oracle `SELECT <TABLENAME>_seq.NEXTVAL FROM DUAL` (:725), PG `SELECT NEXTVAL('<TABLENAME>_seq')` (:727). **Any table without a matching `<table>_seq` breaks persistence.**
- 14 named sequences in code: `t_trx_history_seq`, `T_LOGIN_AUDIT_SEQ`, `T_ABDM_AUDIT_SEQ`, `T_APPOINTMENT_CHANNEL_SEQ`, `t_outboundhis_seq`, `T_nursePatientDetails_seq`, `t_reorderlevelsummery_seq`, `T_AdmissionDetails_seq`, `T_PrescriptionDetails_seq`, `T_PaymentModeDetails_seq`, `T_Refunddetail_seq`, `t_emrdetails_seq`, `t_emr_services_seq`, `auto_code_seq` (`agent-05 §3`).
- Procs consume `.NEXTVAL` **190 times inside PL/SQL** (e.g. `CIMS_ADDBILL.sql:51` `SELECT T_Bill_seq.nextval, p_billno INTO p_ID, p_billno from dual`; `CIMS_ADDADVANCE.sql:22`) — ported procs expect the sequences to exist in PG with the same names.
- Numbering sequences (MRNO, OPDNo, IPDNo, bill no) go through `cims_getlatestautocodeandincremetitbyone` / `getLatestAutocodeSeq` (`BR-PAT-001`, `BR-BILL-001`) — these are **format+increment** functions, not plain identity; port to a PG function, not IDENTITY.

**Seeding rule (avoid ID collisions):**
1. Extract `SELECT sequence_name, last_number, cache_size FROM user_sequences` for owner `IHMSDOCITDEVMODS_PROD_V1` (`SEQ_CREATE_SCRIPT.sql:5`).
2. For each table: `CREATE SEQUENCE IF NOT EXISTS "<t>_seq" START WITH <GREATEST(last_number, (SELECT COALESCE(MAX(ID),1) FROM "<T>")) + 1> CACHE 20;` — MAX(id)+1 is the authoritative floor; `last_number` may already include the CACHE-20 lookahead, so take the max of both.
3. Identity strategy for **new** tables only: `BIGINT GENERATED BY DEFAULT AS IDENTITY` (`BY DEFAULT` so restored legacy data can still insert explicit ids).
4. `autoSequence=false` entities (e.g. `FrequentItem.java:99` — hashed ID via `FrequentItem.getId(...)`) must be excluded from sequence creation; document the per-entity opt-out list `[UNKNOWN — extract via grep autoSequence]`.

---

## 3. Oracle → PostgreSQL Construct Migration Catalog (with evidence counts)

Java-side totals from `agent-05 §5.2` (file:line evidence); proc-side from §6.1.

| Oracle construct | PG replacement | Java hits | Proc hits | Hotspots (file:line) | Notes |
|---|---|---|---|---|---|
| `TRUNC(d)` | `DATE_TRUNC('day', d)` / `d::date` | **102** (85 in ApplicationJobs, 10 QueueManagmentServiceImpl, 5 CommonUtilServiceImpl) | 17 | ApplicationJobs:1163 `ADDEDDATETIME > TRUNC(SYSDATE)`, :594; EHRSyncJobs:233-303 | Date-math rewrite is the single largest mechanical task; `TRUNC(ADDEDDATETIME)` proves the column carries time |
| `TRUNC(d,'MM')`, `ADD_MONTHS` | `DATE_TRUNC('month', d)`, `now() - INTERVAL '1 month'` | see above | — | IntegrationServiceImpl:2225, ApplicationJobs:2446 (`agent-10 §5 row 10`) | |
| `SYSDATE` | `NOW()` / `CURRENT_TIMESTAMP` | **70** (ApplicationJobs 53, EHRSyncJobs 8, CommonUtil 7) | 287 | ApplicationJobs:2263 `"date" >= SYSDATE - 15`, EHRSyncJobs:102, BloodBankServiceImpl:6214 | `SYSDATE-15` → `now() - interval '15 days'` |
| `TRUNC(SYSDATE)` | `CURRENT_DATE` (day semantics) | **53** (subset of TRUNC) | — | ApplicationJobs:1158-1166,1665; DoctorServiceImpl:1045; QueueManagmentServiceImpl:1477-1759 | `BR-IPD-005` 3-day abscond job and `BR-OPD-002` same-day slots depend on these |
| `ROWNUM <= n` / `ROWNUM = 1` | `LIMIT n` / `LIMIT 1` (or `FETCH FIRST n ROWS ONLY`) | **19** | **486** | BaseServiceImpl:945 `ROWNUM <= 50`, HL7Helper:1824-1826, RulesEngineUtil (8), ApplicationJobs (8), RMRSServiceImpl:1361-1366, NursingServiceImpl:7124 | **⚠ Oracle ROWNUM semantics ≠ PG LIMIT without ORDER BY** — audit each site for implicit ordering; PG returns arbitrary rows |
| `paginate(0,500)` (EclipseLink) | `LIMIT ? OFFSET ?` | — | — | ApplicationJobs:533,554,631 | agent-10 §5 row 12 |
| `SELECT … FROM DUAL` | remove (no DUAL in PG) | **27** | (implicit) | HL7Helper (19 NEXTVAL/DUAL), RulesEngineUtil (8), ServiceLayerAspect:725, BaseServiceImpl, AuditHelper (4), validationQuery `SELECT 1 FROM DUAL` → `SELECT 1` | validationQuery in application.properties:101 |
| `NEXTVAL` via dual | `SELECT nextval('<seq>')` (already coded) or proc-internal `nextval` | **30** | **190** | HL7Helper:246 `t_outboundhis_seq`, AuditHelper:156,917,938,961; procs CIMS_ADDBILL.sql:51, CIMS_ADDADVANCE.sql:22 | §2.1 |
| `DECODE(a,b,c,d)` | `CASE` | **25** | 6 | CommonUtilServiceImpl (10), SolrUtil (5), QueueManagmentServiceImpl (2), CAeSignService (2), IdEncryptionUtil (2), GenericDAO (1) | |
| `NVL(a,b)` | `COALESCE(a,b)` | **6** | **84** | EHRSyncJobs:54-55, ApplicationJobs:2249-2254, SolrUtil, CWFServiceImpl, FileManagementServiceImpl | |
| `TO_DATE(s, fmt)` | `TO_DATE(s, fmt)` — compatible, but **fmt strings differ**: Oracle `'DD-MM-YYYY HH24:MI:SS'` works; `'yyyy-mm-dd hh24:mi:ss.xff'` (BaseServiceImpl:929) → PG `'YYYY-MM-DD HH24:MI:SS.US'` | **24** | 11 | ApplicationJobs (14), QueueManagmentServiceImpl (10) | |
| `TO_CHAR(d, fmt)` | `TO_CHAR(d, fmt)` — **semantics differ**: Oracle `'HH24'` vs PG `'HH24'` ok; Oracle `'fm'`, `'RR'`, `'J'`, `'Q'` differ | **6** | **474** | ApplicationJobs, DietServiceImpl:154 `to_char(cast(SYSDATE as date),'hh24:mi:ss')` → `to_char(now(),'HH24:MI:SS')` | 474 proc TO_CHARs — ported procs must be spot-checked per fmt |
| `MERGE INTO t USING dual ON (…)` | `INSERT … ON CONFLICT (…) DO UPDATE` | **2** | — | BaseServiceImpl:928,935 (T_FREQ_ITEMS upsert) | `CommonABDMUtil.java:1089` has a **commented legacy MERGE** — resurrection risk |
| `LISTAGG(c, ',') WITHIN GROUP (ORDER BY …)` | `STRING_AGG(c, ',' ORDER BY …)` | **2** | — | BaseServiceImpl:945 (paired with ROWNUM<=50) | |
| `TO_TIMESTAMP(s, fmt)` | `TO_TIMESTAMP(s, fmt)` | **2** | — | BaseServiceImpl:929,936 | fmt `xff` → `.US` |
| `FETCH FIRST 1 ROW ONLY` | identical (SQL:2008) | 1 | — | BioMedicalWasteManagementServiceImpl:347 | PG-compatible as-is |
| `CONNECT BY` / `CONNECT BY PRIOR` | `WITH RECURSIVE` | 0 | **1** | proc `CIMS_GETITEMCURRENTREORDERLISTFORPO_1.sql:62` | only in a **non-live** proc — port if/when that proc migrates |
| `EXTRACT(YEAR FROM SYSDATE)` | `EXTRACT(YEAR FROM NOW())` | — | — | BloodBankServiceImpl:7367-7379 | agent-10 §5 row 17 |
| `SELECT 1 FROM DUAL` (pool validation) | `SELECT 1` | props | — | application.properties:101 | |
| `DATE 'yyyy-mm-dd'` literal | identical | — | — | VisitServiceImpl:406-411 | |
| Quoted reserved words `"date"`, `"Comment"` | identical — but **avoid `"date"` as column name in new schema** | — | — | CIMS_ADDADMISSIONINFORMATION.sql; ApplicationJobs:2263 | agent-10 §5 row 19 |
| Oracle `''` = NULL | PG `''` ≠ NULL | — | — | audit empty-string predicates | `[UNKNOWN]` count |
| `(+)` outer-join syntax | `LEFT/RIGHT JOIN` | — | **`[UNKNOWN]`** | grep procs | Oracle-proprietary; not counted in agent-05 (flag: verify in 719 files) |

### 3.1 Stored-procedure migration (719 artifacts → PL/pgSQL)

| Stat (evidence `agent-05 §6.1`) | Value | Migration decision |
|---|---|---|
| Total SQL files in `generated-procs/` | 719 | — |
| `PROCEDURE` | 692 | — |
| `FUNCTION` | 17 (incl. base64 ×4, CONVERT_CHARTONUM, GETFULLNAMEFROMDOCTORID1, FNCREATENUMBERIDENTITYTABLE) | base64 helpers port first (data migration needs them) |
| `PACKAGE` | 11 | packages unwrap to PG schemas/functions `[UNKNOWN]` contents |
| with `SYS_REFCURSOR` OUT | 473 | → PL/pgSQL functions `RETURNS refcursor` or `RETURNS TABLE(...)`; EclipseLink `StoredProcedureQuery` works with PG functions (`05 §8`) |
| with `IN OUT`/`IN OUT NOCOPY` | 389 | → INOUT params |
| with `EXCEPTION WHEN` | 504 | → `EXCEPTION WHEN others` blocks; verify NO_DATA_FOUND parity |
| **Live-called from Java** | **30** | **Port only these** (list below); the remaining ~689 stay as Oracle reference until their module migrates (`10 §5` hybrid strategy) |

**Live 30** (evidence: BillingCommonUtilServiceImpl:110,121,133,147,155,179,240,264; CommonUtilServiceImpl:649,662,674,1117,1120,2972-2978; ApplicationJobs; InventoryJobSchedule; SolrUtil; LinenLaundryServiceImpl — `agent-05 §6.1`):
CIMS_ADDCHARGE · CIMS_ADDITEMSTOCK_UPDATE · CIMS_GETCHARGES · CIMS_GETCHARGESTEMP · CIMS_GETPATIENTADVANCE · CIMS_ITEMSALE_VISIT · CIMS_GETITEMSALESDETAILS · CIMS_GETITEMSALESRETURN_VISIT · CIMS_GETITEMSALESRETURNDETAILS · CIMS_GETSPONSORBYADMISSIONID_NEW_DOC · CIMS_GETITEMCATEGORY · CIMS_GETALLSERVICEMASTERDETAILSBYDESCRIPTION · CIMS_GETITEMTAXLISTFORUNIT · CIMS_GETPATIENTGENERALDETAILSLISTFORSEARCHNEW / _STRICT · CIMS_GETPATHOTESTPARAMETERANDSUBTESTDETAILS_UPDATED_NEW2 / _UNITWISE / NEW3 · CIMS_GETINVSTOCKJOBSCHEDULE · CIMS_GETITEMSBYSTOCKFORLINENREQUEST · CIMS_JOBSCHEDULING · CIMS_UPDATECHARGES / CIMS_UPDATECHARGESTEMP · CIMS_CONFIGUREUNITPARAMETER · CIMS_CheckPackageSubscriptionByAdmId · cims_getappointmentfornotification · CIMS_rpt_PathoResultEntry_new1 · CIMS_rpt_PathoTemplateReport_Final1 · CIMS_rptPathResultEntryFroCultureTest_Final1.

**Verification list for the 30 (per proc, before port):** (a) exact table names used (procs reference `T_UserDetails`, `M_StaffMaster`, `M_WardMaster`, `T_AdmissionDetails` — **case-sensitive names that differ from entity-derived names**, `agent-05 §9`); (b) `TO_CHAR` formats; (c) `ROWNUM=1` semantics; (d) base64/UTL_ENCODE/UTL_FILE usage (UTL_FILE → app-side or `pg_read_file`-equivalent `[UNKNOWN]`); (e) `NEXTVAL` sequence names. **Triggers: 0** in the whole system — no trigger conversion work (`05 §6`).

---

## 4. Case-Sensitivity & Naming

PG lowercases unquoted identifiers; EclipseLink quotes `@Table` names. Mixed-case and case-mixed names are real:

| Evidence group | Examples | PG approach |
|---|---|---|
| Uppercase convention tables (vast majority) | `M_ADMISSIONTYPE`, `T_PATIENTADVICE` | **Unquoted, uppercase → PG folds to lowercase.** Either (a) accept lowercase table names and rewrite all ~2,000 SQL literals + the aspect's `tab.name() + "_seq"` logic, or (b) **quote identifiers** (`"M_ADMISSIONTYPE"`) to preserve case exactly. **Recommend (b) for the strangler window** — the dynamic sequence name `tab.name() + "_seq"` and 190 proc NEXTVALs depend on exact-case names |
| Case-mixed `@Table` | `t_creditNote`, `t_creditNoteDetails`, `M_AdditiveMaster`, `M_AppBookingTypeMaster`, `T_PatientDocuments`, `T_EquipmentTransfer`, `T_eqpComplaintDetails`, `T_nursePatientDetails`, `T_Registration_int`, `T_AdmissionDetails` (procs), `T_UserDetails` (procs), `M_BedMaster` (procs) | Must be **quoted** in PG DDL; Java already resolves them via `@Table` so JPA is safe — only native SQL strings and procs need audit |
| Lowercase full table names | `block_mst`, `city_mst`, `district_mst`, `user_detail_temp`, `t_poissueitemdetails`, `t_poissueitembatchdetails` | Quoted or unquoted both fine (all-lowercase); keep consistent |
| Non-prefixed uppercase | `CONFIG_APPLICATION`, `USER_`, `PROFILE`, `MEDIA`, `INDICATORS`, `BENEFICIARY_ENROLMENT_IHMS`, `T_ADMISSION_TID` | quoted-uppercase policy (same as convention tables) |
| Aasmaan default-named | `AASMAANCASE`, `AASMAANCASEEVENT`… (`@Table` commented out — `agent-05 §2.2.6`) | **verify actual DB names before mapping**; recommend adding explicit `@Table` to entities |
| Sequences | `t_trx_history_seq` (lowercase) vs `T_Bill_seq` (mixed) | Create **quoted sequences with exact legacy names**; `SELECT nextval('"t_trx_history_seq"')` — note the current PG branch passes unquoted `'<seq>'` (`ServiceLayerAspect:727`) which **fails for mixed-case sequences** → must switch to quoted form |

Risk: `agent-05 §9` — proc bodies show exact case (`T_AdmissionDetails`) that may not match entity tables (`T_ADMISSIONDETAILS`?); Oracle is case-sensitive for quoted objects; **reconcile each table name against the Oracle catalog dump before generating DDL**.

---

## 5. Indexing Strategy

Solr handles all search (`07 §1`) — **no PG full-text/ILIKE search indexes are proposed**. PG indexes serve OLTP access paths only:

| Index target (evidence) | Columns (proposed) | Why |
|---|---|---|
| T_REGISTRATION family | `(UNITID, ADDEDDATETIME)`, `(MRNO)`, `(PATIENTID)`, `(registrationId)` | Highest traffic; `05 §8`; native-SQL lookups in CommonUtilServiceImpl |
| T_ENCOUNTER | `(UNITID, ADDEDDATETIME)`, `(PATIENTID)`, `(MRNO)`, `(status)` | 52 refs; visit-state queries in EmergencyAndWalkinVisitServiceImpl; partition-by-year open question (`10 §7 Q2`) |
| T_ADMISSION | `(PATIENTID)`, `(ENCOUNTERID)`, `(BEDID)`, `(ISDISCHARGED)` | ADT flows; `CIMS_ADDADMISSIONINFORMATION` |
| T_BILL / T_BILLDETAILS | `(ENCOUNTERID)`, `(PATIENTID)`, `(BILLNO)`, `(ISSETTLED)` | settlement spine; bill number lookups |
| T_ITEMSTOCK | `(ITEMID, STOREID)` + **row-level locking** | Concurrency: decrement via `SELECT … FOR UPDATE` / `UPDATE … RETURNING` same as legacy procs (`10 §3.4`; CIMS_ADDITEMSTOCK.sql) |
| Every table | FK columns (`UNITID`, `CREATEDUNITID`, `UPDATEDUNITID`), audit columns `(ADDEDDATETIME)` | MBase audit columns on ≥750 tables — btree on ADDEDDATETIME where jobs filter by date (ApplicationJobs TRUNC patterns) |
| Queue tables (legacy → replaced by Kafka, but live during strangler) | `(SYNCHRONIZED/ISSEND/STATUS, NODE, ADDEDDATETIME)` | poller queries: `ApplicationJobs:1611` (WhatsApp), :1661-1688 (ABDM), :2122 (Solr backfill by SOLR_STATUS=0) |
| Partitioning | T_ENCOUNTER, T_BILL by year | open question `10 §7 Q2` — volume unknown |
| Function-based indexes (Oracle) | — | `[UNKNOWN]` — no evidence gathered; check `ALL_INDEXES` during dump |

FK indexes: Oracle has **no automatic FK indexes**; the legacy relies on manual joins. In PG, add btree indexes on every FK column of every table where the FK participates in a WHERE (UnitID filtering is on nearly every query — `05 §7`).

---

## 6. Data Migration Plan Sketch

### 6.1 Strategy (strangler, dual-run)
- **Wave ordering** per `10 §6`: Wave 0 (master/IAM/interop/search) → Wave 1 (patient/OPD/ADT/queue) → Wave 2 (revenue) → …
- **Dual-read**: new services on PG; monolith keeps Oracle; **Interop runs as event/outbox broker during the window** (`10 §5`).
- **Per-table cutover protocol** (`10 §5`): (a) freeze writes from old side → (b) event-forward new writes → (c) backfill → (d) cut reads. Critical cutover list: T_ENCOUNTER, T_BILL, T_ITEMSTOCK (+ T_REGISTRATION family).

### 6.2 Data movement
- **ETL initial load**: dump Oracle (Data Pump `expdp`) → transform (types per §2) → load PG (COPY). Base64 columns load **verbatim** (TEXT-safe); **do not decode during migration** — app decode path stays (`GenericDAO:566-578`).
- **CDC for continuous sync** (strangler window): Oracle → Kafka (e.g., GoldenGate/Debezium-class tooling — `[UNKNOWN]` tooling availability at site) and/or PG logical replication for the outbox (`pg_logical` → Kafka, `10 §5`). Mark: **the legacy has no CDC today** — events must be sourced from app-level changes, not redo logs.
- **Dual-read + reconciliation**: nightly row-count + checksum jobs per migrated table; reconciliation centers on T_ENCOUNTER/T_REGISTRATION families (`05 §7`).

### 6.3 Sequence re-seeding
Per §2.1: `START WITH GREATEST(last_number, MAX(id)+1)`; run **after** initial load, **before** dual-write starts. Verify per-table; keep a manifest of `sequence_name, table, start_value, source_last_number`.

### 6.4 PII / encryption handling
| Item | Legacy state (evidence) | Migration action |
|---|---|---|
| `phi.encryption.enabled` | `false` today (`05 §8`) | Re-verify at migration time; if enabled before cutover, decryption/encryption must happen **in the app**, not ETL |
| `@EncodeAsBase64` / `Base64Data` | decode on fetch (GenericDAO:566-578, 779-783) | Preserve byte-for-byte; add a test harness comparing encode/decode round-trip on PG `TEXT` |
| Base64 helper functions | `ENCODEFUNCTION`, `DECODEFUNCTION`, `DECODEUTF8ENCODEDSTRING`, `ENCODEUTF8DECODEDSTRING`, `DECODEBASE64` (procs + MigratePatientRegistrationUtil:36) | Port 5 helpers to PL/pgSQL **first** (used by migration utilities) |
| CLOB PII | e.g., documents/templates (`agent-10 §5 row 4`) | confirm which CLOBs hold base64 PII vs plain text `[UNKNOWN]` |
| Secrets in properties | eAushadhi basic-auth, WhatsApp keys, SIMS AuthKey, NMC tokens, SSO mobile key hard-coded (`07 §3.3`) | **do not migrate secrets into PG**; move to vault — out of scope for DB migration but gate for PG connectivity config |

---

## 7. Per-Module Migration Mapping Tables (Legacy table | Target PG schema | Conversion notes)

### 7.1 Masters (582 M_* tables + CONFIG_APPLICATION + geography)
| Legacy | Target schema | Conversion notes |
|---|---|---|
| M_UNITMASTER (45 refs) | `ihms_master` | UnitID tenancy column on every table — tenant key `BIGINT`; reference integrity to M_UNITMASTER |
| M_DEPARTMENTMASTER (32) | `ihms_master` | |
| M_BEDMASTER (26; `OCCUPIED` char flag + BedStatusId) | `ihms_adt` (recommended, §1.1) | `OCCUPIED` → `boolean` (CAS `UPDATE … WHERE OCCUPIED=false`, `BR-IPD-002`); BedStatusId → SMALLINT + CHECK |
| M_ITEMMASTER (18) | `ihms_inventory` (recommended) | itemMaster Redis key stays Reference-owned contract |
| M_SERVICEMASTER (15) / M_TARIFFMASTER | `ihms_master` | rate data; money columns NUMERIC(19,2) |
| M_DOCTORMASTER (16) / M_STAFFMASTER (10) | `ihms_iam` (profiles) or `ihms_master` — decision pending | procs join via T_UserDetails.EmployeeID |
| M_PATHOTESTMASTER, M_RAD*MASTER, M_PROCEDUREMASTER, drug/compound masters | `ihms_master` | borrowed by Lab/Rad/Pharmacy/OT via API |
| CONFIG_APPLICATION | `ihms_master` | key-value TEXT columns; referenced by native utils |
| block_mst / city_mst / district_mst | `ihms_master` | Rajasthan geography; 22 refs in CommonUtilServiceImpl |
| M_MRNSERIES | `ihms_patient` or `ihms_master` (numbering ownership) — decision | `BR-PAT-001` MRN series logic |

### 7.2 Transactionals (172 T_* tables)
| Legacy | Target schema | Conversion notes |
|---|---|---|
| T_ENCOUNTER | `ihms_encounter` | TIMESTAMP(3); VisitStatus SMALLINT+CHECK (06 §A1); partitioning Q |
| T_APPOINTMENT / T_PATIENTTOKEN / T_DOCTORCHECKIN_QUEUEROOM | `ihms_encounter` | VisitStatus 11/12 split-brain with encounter — unify lifecycle in PG (06 §C.1) |
| T_OUTBOUNDHIS (47 refs) | `ihms_interop` | **legacy HL7 outbox** → Kafka topic (KAFKA-EVENT-CATALOG.md); RetryCount → retry policy; keep table during strangler |
| T_INBOUNDHIS | `ihms_interop` | inbound pickup job currently disabled (`07 §3.2`) |
| T_USERDETAILS | `ihms_iam` | STATUS char '1' vs Boolean — audit |
| T_LINENREQUEST(+DETAILS) | `ihms_linen` | |
| T_ITEMCLINIC / T_ITEMSTOCK | `ihms_inventory` | stock = row-lock hotspot |
| T_SPONSOR | `ihms_patient` | sponsor-default rules (BR-OPD-005) |
| T_BIOMEDICALWASTE(+DETAILS) | `ihms_bmw` | |
| T_PATIENT* (~30) | `ihms_patient` | |
| T_FREQ_ITEMS | `ihms_billing` (upsert) | MERGE → ON CONFLICT; `autoSequence=false` hashed ID |
| T_TRX_HISTORY / T_LOGIN_AUDIT / T_ABDM_AUDIT / T_APPOINTMENT_CHANNEL / T_AUDIT | `ihms_audit` (decision) | AuditHelper raw INSERTs (40+ variants); sequences `t_trx_history_seq` etc. — **audit writes are hot; index (addeddatetime, userid)** |
| T_ADVANCE / T_PAYMENT / T_PAYMENTMODEDETAILS / T_REFUND / T_RECEIPT | `ihms_payment` | TransactionType SMALLINT+CHECK (06 §A6) |
| T_BILL / T_BILLDETAILS / T_CHARGE / T_CHARGES_TEMP / T_DOCTORBILL / T_DOCTORSHARE / T_CONCESSION / T_CREDITNOTE(+Details) | `ihms_billing` | BillNo varchar formatted; money NUMERIC(19,2); charge snapshots (*BeforeFinalization) for cancellation restore (06 §A3) |
| T_PATHORDERBOOKING(+DETAIL) / T_PATHOPATIENTPARAMETERDETAILS / T_MACHINECONFIGURATION | `ihms_lab` | SampleNo/SampleCollectionNO numerics; IsOutSourced boolean |
| T_RADORDERBOOKING(+DETAILS) | `ihms_rad` | |
| T_PRESCRIPTIONMASTER / T_ITEMSALE / T_PRESCRIPTIONDISPENSEDETAILS / T_ITEMCOMPOUNDDETAILS | `ihms_pharmacy` | DISPENSED booleans; VisitStatus 12 side-effect |
| T_OTSCHEDULE(+DETAILS) / T_SURGERYDETAILS / T_ANESTHESIANOTES | `ihms_ot` | |
| T_NURSENOTE / T_PATIENTVITALS / T_NURSETASKDETAILS / T_INTAKEOUTPUT / T_NURSEPATIENTDETAILS | `ihms_nursing` | |
| T_BLOODBAG / T_BLOODCOMPONENT / T_BLOODREQUISITION / T_BLOODDONOR / T_BLOODDONATIONCAMP | `ihms_bloodbank` | requisition status 1/2/3/10 SMALLINT; expiry/decross jobs on timestamps (06 §A7) |
| T_DIETORDER / T_DIETITEMSCHEDULEDETAILS / T_TODAYDIETITEMMENU | `ihms_diet` | |
| T_CSSDKITFORMATION / T_CSSDKITISSUEITEM / T_STERILIZATION* / T_PROCEDURECSSD* | `ihms_cssd` | |
| T_EQUIPMENT / T_EQPCOMPLAINT(+RESOLUTION) / T_EQPPM* / warranty/AMC | `ihms_equipment` | CLOB docs → TEXT |
| T_TRANSPORTREQUEST(+DETAILS) / T_VEHICLEMAINTENANCE | `ihms_transport` | |
| T_PATIENTCOMPLAINTS / T_PATIENTGRIEVANCE* / T_FEEDBACKTEMPLATE | `ihms_grievance` | |
| T_PATIENTQUEUE / T_OPDSTATIONMASTER / T_TABLETMAPPING | `ihms_queue` | live queue state also in Redis (QMSUtil) |
| T_WHATSAPPMESSAGEQUEUE/STATUS / T_ABDMREQUESTQUEUE / T_EHRJOBDETAILS | `ihms_interop` | legacy poll queues → Kafka (see KAFKA-EVENT-CATALOG.md) |

### 7.3 EJS / Aasmaan / integration modules
| Legacy | Target schema | Conversion notes |
|---|---|---|
| ANDROID_*, ES_*, MASTER_* (ejs geography), MEDIA*(65 refs), USER_*/PROFILE/INDICATORS | `ihms_govt` | EjsBase BigInteger id → NUMERIC(38); MEDIA high-ref table |
| AASMAAN* (35) | `ihms_govt` | verify DB names; add explicit @Table |
| T_PATIENT_ABDM(+_CC,_SHARETOKEN), T_PATIENT_CONSENT_REQUESTS_ABDM(+_DATA) | `ihms_patient` | ABDM token check `EmergencyAndWalkinVisitServiceImpl.java:652` |
| T_USER_FACEAUTH | `ihms_iam` or `ihms_patient` — decision | face-auth tokens |
| T_ADMISSION_TID / T_ELIGIBILITYCHECK | `ihms_rcm` | |
| T_NHCX_REQUEST(+_DETAILS) | `ihms_rcm` | |
| T_REFRENCETID / BENEFICIARY_ENROLMENT_IHMS | `ihms_govt` (Chiranjeevi) | |
| s_servicewisecollection / s_userwisecollection | `ihms_rmrs` | statistical POJOs — plain tables |
| T_REGISTRATION / T_REGISTRATION_DETAILS / T_Registration_Int / T_Registration_Details_Int / T_coronaStats / t_transfer / t_admission / t_patientrequisition / Config_Application(native) | per §1 (patient/adt/lab/master) | **DDL must be dumped from Oracle** — not recoverable from Java (`agent-05 §5.4`); T_Registration_Int holds base64 PII |

---

## 8. Open Questions & Flagged Contradictions

**Contradictions between source docs (resolve before DDL):**
1. **Master entity count**: 05 says 608 masters entities (582 M_* tables); agent-10 says 609 master entities. Delta = 1 — verify.
2. **Transactional entity count**: 05 §3.2 says 165; agent-10 §1.1 says 137. Delta 28 — likely package-boundary differences (bloodBank/Nursing/payment sub-packages); verify against @Table extract.
3. **Proc artifact count**: 692 PROCEDURE + 17 FUNCTION + 11 PACKAGE = **720**, but both docs state **719** files. Verify file count vs artifact count.
4. **M_* ownership conflicts** (§1.1): M_BEDMASTER/M_WARDMASTER (ADT vs Reference), M_ITEMMASTER (Inventory vs Reference), M_STAFFMASTER/M_DOCTORMASTER (IAM vs Reference), T_ITEMSALE (Pharmacy vs billing reads).
5. **T_ADMISSION_TID (05 §3.5, rcm package) vs T_ADMISSIONTIDDETAILS (10 row 23)** — naming variant of the same table; confirm actual Oracle name.
6. **Solr `service` collection**: 10 row 21 lists service/icd10cm/healthfactor as Reference's; 10 row 27 says Search owns all 6; 07 §1.2 shows `service` is **orphaned/dead config**. Reconciliation: Search owns collection infrastructure; Reference owns master-data *content*; `service` collection is dropped.
7. **BIGSERIAL vs sequences**: 05 §8 says "BIGSERIAL NOT allowed"; 10 §3.2 says new tables use BIGSERIAL/IDENTITY. Both apply — legacy tables keep sequences, new tables may use IDENTITY (not a real contradiction, clarified in §2.1).

**Open questions (unknowns):**
- Exact DDL/row counts of the 10 unmapped native tables (must dump from Oracle: `ALL_TABLES`/`DBMS_METADATA`).
- Do the 719 procs' referenced column names match current entities (case-sensitivity drift)?
- `eclipselink.jpa.naming_into_indexed=true` effect on generated SQL `[UNKNOWN]`.
- kundera props hint at historical MongoDB usage — not active; no PG impact.
- Function-based/bitmap indexes in Oracle `[UNKNOWN]` — check `ALL_INDEXES`.
- T_ENCOUNTER/T_BILL yearly volume → partitioning decision (`10 §7 Q2`).
- GMT vs local-time writes (audit before picking `TIMESTAMPTZ` anywhere — recommendation is `TIMESTAMP(3)` without tz).
- Audit table ownership (single `ihms_audit` vs per-service) — `10 §7 Q7`.

---

## 9. Numbers at a Glance (migration workload)

- ~843 tables → 29 schemas; ≥750 with MBase audit columns
- 719 Oracle PL/SQL artifacts; **30 live** to port; 473 SYS_REFCURSOR; 0 triggers
- 14 named sequences + dynamic `<table>_seq` per entity → all recreated, seeded `MAX(id)+1`
- 102 TRUNC / 70 SYSDATE / 53 TRUNC(SYSDATE) / 27 DUAL / 25 DECODE / 24 TO_DATE / 19 ROWNUM / 6 NVL / 6 TO_CHAR / 2 MERGE / 2 LISTAGG / 2 TO_TIMESTAMP / 1 FETCH FIRST (Java); 486 ROWNUM / 474 TO_CHAR / 287 SYSDATE / 190 NEXTVAL / 84 NVL / 17 TRUNC / 11 TO_DATE / 6 DECODE / 1 CONNECT BY (procs)
- ~436 `getQ(` native call sites + ~2,000 raw SQL literals to audit for PG syntax
- 9 existing dual-dialect branch sites to extend, not replace
