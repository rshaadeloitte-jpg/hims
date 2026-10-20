# AGENT 05 — Database Forensics: Evidence Dump

**Scope:** D:\ihms (iHMS/OHUM PAS NG — EclipseLink JPA 2.6.4, Oracle DB, PostgreSQL migration target)
**Date:** 2026-08-14 | Research-only — no code modified.

---

## 1. Persistence Configuration (evidence)

| Item | Evidence | Value |
|---|---|---|
| Persistence unit | `src/main/resources/META-INF/persistence.xml:5` | `ORCL`, JTA, `jdbc/orcl` |
| Provider | `persistence.xml:6` | `org.eclipse.persistence.jpa.PersistenceProvider` |
| Class scanning | `persistence.xml:8` | `<exclude-unlisted-classes>false</exclude-unlisted-classes>` |
| Cache | `persistence.xml:9` | `shared-cache-mode NONE` |
| EMF (Spring Boot) | `src/main/java/com/ohum/App.java:173-193` | `LocalContainerEntityManagerFactoryBean`, `packagesToScan = {"com.ohum.models", "com.ohum.dto"}`, `EclipseLinkJpaVendorAdapter` |
| Dialect switch | `App.java:180-184` | `dbDriver.contains("oracle") ? Database.ORACLE : Database.POSTGRESQL` — **EMF already dual-dialect ready** |
| EclipseLink props | `App.java:188-191` | `eclipselink.logging.level.sql`, `eclipselink.cache.shared.default=false`, `eclipselink.jpa.naming_into_indexed=true` |
| Legacy XML EMF | `src/main/resources/persistenceContext.xml:26-33` | references dead PU `ZDocit-HIS-SQL-Server-PU` (inactive); `GlobalDB-MySQL-PU` commented out (line 35-43) |
| JDBC URL | `application.properties:77` | `jdbc:oracle:thin:@//10.10.7.63:1521/orcl` (QA active; staging/prod URLs commented, lines 73, 87) |
| Credentials | `application.properties:95-97` | `IHMSDOCITDEVMODS_PROD_V1` / `lifeohum` |
| Driver | `application.properties:69` | `oracle.jdbc.OracleDriver` (read as `dbDriver` bean property) |
| Pool | `application.properties:53-59` | HikariCP: timeout 10 s, max-pool 10, min-idle 5, pool `HikariConnectionPool` |
| Validation | `application.properties:99-103` | `validationQuery=SELECT 1 FROM DUAL` (Oracle-specific) |
| JPA dialect | `application.properties:63` | `spring.jpa.database=ORACLE`, `spring.datasource.platform=oracle` |
| Spring Data | `application.properties:1286` | `spring.data.jpa.repositories.enabled=false` — JPA repositories disabled |
| Trans. timeout | `application.properties:105` | `spring.transaction.default-timeout=60` |
| Cache (non-DB) | `applicationContext.xml:43-45` | `MemoryCacheImpl` active (Redis/Hazelcast/Ignite commented) |

---

## 2. Entity → Table Mapping Rules (evidence)

### 2.1 Hierarchy (base classes)

| Class | File | Key fields |
|---|---|---|
| `LBase` | `src/main/java/com/ohum/models/LBase.java:21-31` | `@Id protected Long id`; `@Transient autoSequence=true` |
| `MBase` | `src/main/java/com/ohum/models/MBase.java:38-68` | `UnitID`, `STATUS` (Boolean), `CREATEDUNITID`, `UPDATEDUNITID`, `ADDEDBY`, `ADDEDDATETIME` (Timestamp), `UPDATEDBY`, `UPDATEDDATETIME`, `SYNCHRONIZED`; `beforeCreateUpdateCallback(nId,userId,unitId)` assigns id + audit |
| `Base` | `src/main/java/com/ohum/models/Base.java:21-43` | `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `status` enum `EntityStatus{NONE,ACTIVE,INACTIVE,LOCKED,DELETED}` |
| `EjsBase` | `src/main/java/com/ohum/models/ejs/EjsBase.java:22-32` | `@Id protected BigInteger id` (EJS module only) |

**Distribution:** masters → MBase (606/608); transactionals → MBase (126); txt → MBase (10); trx → MBase 8 + LBase 1; aasmaan → LBase (35); ejs → EjsBase (57); statistical → plain POJOs. **Nobody extends `Base`** (dead hierarchy).

### 2.2 Naming conventions (verified by sampling + full extraction)

1. **Masters: `M_<UPPERCASE class name>`** — `@Table(name="M_ADMISSIONTYPE")` (`masters/Admissiontype.java:36`), `M_BEDMASTER` (`Bedmaster.java:43`), `M_ACCOUNTINGLEDGER` (`Accountingledger.java:32`). 582 `M_*` tables.
2. **Transactionals: `T_<UPPERCASE class name>`** — 172 `T_*` tables, e.g. `T_ANCVISITDETAILS`, `T_PATIENTADVICE` (`transactionals/Patientadvice.java`).
3. **Case-mixed exceptions** (camel/pascal in DB): `M_AdditiveMaster`, `M_AppBookingTypeMaster`, `t_creditNote`, `t_creditNoteDetails`, `T_PatientDocuments`, `T_EquipmentTransfer`, `T_eqpComplaintDetails`, `T_nursePatientDetails`, `t_patienttokens`, `T_Registration_int` (native SQL only), `T_AdmissionDetails` (procs), `T_UserDetails` (procs), `M_BedMaster` (procs).
4. **Lowercase full table names:** `block_mst`, `city_mst`, `district_mst`, `user_detail_temp`, `t_poissueitemdetails`, `t_poissueitembatchdetails` (txt/temp package).
5. **Non-prefixed:** `CONFIG_APPLICATION` (masters), `USER_`, `USER_TYPES`, `USER_DESIGNATION`, `PROFILE`, `MEDIA`, `MEDIA_CATEGORIES`, `MEDIA_TYPES`, `BENEFICIARY_ENROLMENT_IHMS`, `T_ADMISSION_TID`, `T_ELIGIBILITYCHECK`, `T_NHCX_REQUEST(+_DETAILS)`, `T_REFRENCETID`, `s_servicewisecollection`, `s_userwisecollection`.
6. **`@Table` absent → EclipseLink default = entity simple name uppercased:** `GenericDAO.getTableName()` (`dao/GenericDAO.java:756-769`): `(t == null) ? entityType.getName().toUpperCase() : t.name()`. **Aasmaan module (35 entities) has `@Table` commented out** (`aasmaan/AasmaanCase.java:13 "//@Table(name = "")"`) → tables `AASMAANCASE`, `AASMAANCASEEVENT`, etc.
7. **@Entity without @Table:** `transactionals/ItemExpiryList`, `ItemUtilizationView`, `RateContractView` → default `ITEMEXPIRYLIST`, `ITEMUTILIZATIONVIEW`, `RATECONTRACTVIEW`.
8. **DTOs as entities:** 21 `@Entity` classes outside `models/` (in `dto/`, e.g. `dto/PatientPrescriptionNAItemDetailsDto.java:23 → T_NA_ITEMDETAILS`) — scanned by `packagesToScan com.ohum.dto` (`App.java:178`).

### 2.3 Relationship mapping — nearly absent

`@ManyToOne`: 14, `@OneToMany`: 1, `@ManyToMany/@OneToOne/@JoinTable/@EmbeddedId/@IdClass/@Inheritance/@Version/@SecondaryTable`: **0** across all models. All relationships are resolved in hand-written SQL.

---

## 3. Primary Key Strategy (evidence)

- **No `@GeneratedValue` / `@SequenceGenerator` anywhere in entities.**
- Sequence-of-last-resort = manual: `LBase.id` (Long) assigned pre-persist in AOP aspect `handlePrePersistCallbackHooks` (`aspects/ServiceLayerAspect.java:697-750`):
  - `ServiceLayerAspect.java:723`: `String seq = tab.name() + "_seq";`
  - Oracle branch: `SELECT <TABLENAME>_seq.NEXTVAL FROM DUAL` (`:725`)
  - PostgreSQL branch: `SELECT NEXTVAL('<TABLENAME>_seq')` (`:727`) — **PG path already coded**
- Same helper `BaseServiceImpl.getNextSequenceValue(seqName)` (`services/BaseServiceImpl.java:566-574`) — both dialects.
- `GenericDAO.java:857`: `"SELECT NEXTVAL('" + de.getTableName() + "_seq')"` (PG branch).
- Per-entity opt-out: `LBase.autoSequence=false` (e.g. `models/FrequentItem.java:99` — uses ID-hashing `FrequentItem.getId(...)`).
- **Known sequence names (from code):** `t_trx_history_seq` (`audit/AuditHelper.java:156`), `T_LOGIN_AUDIT_SEQ` (`:917`), `T_ABDM_AUDIT_SEQ` (`:938`), `T_APPOINTMENT_CHANNEL_SEQ` (`:961`), `t_outboundhis_seq` (`util/HL7Helper.java:246`), `T_nursePatientDetails_seq` (`jobs/AutoNextDayAllocation.java:30`), `t_reorderlevelsummery_seq` (`jobs/InventoryJobSchedule.java:199`), `T_AdmissionDetails_seq` (commented, `generated-procs/CIMS_ADDADMISSIONDETAILS.sql:25`), `T_PrescriptionDetails_seq`, `T_PaymentModeDetails_seq`, `T_Refunddetail_seq`, `t_emrdetails_seq`, `t_emr_services_seq`, `auto_code_seq` (procs).
- **Sequence creation scripts (in generated-procs):**
  - `SEQ_CREATE_SCRIPT.sql:4`: dumps `CREATE SEQUENCE <name> START WITH 1 NOCACHE` for all `*SEQ*` sequences of owner `IHMSDOCITDEVMODS_PROD_V1`
  - `FNCREATENUMBERIDENTITYTABLE.sql:5`: `CREATE SEQUENCE <table>_seq INCREMENT BY 1 MAXVALUE ... CACHE 20`
  - `SEQ_DROP_SCRIPT.sql` exists (drop counterpart)

---

## 4. Audit & Soft-Delete Columns (evidence)

- **MBase (majority of tables):** `UNITID, STATUS (Boolean), CREATEDUNITID, UPDATEDUNITID, ADDEDBY, ADDEDDATETIME, UPDATEDBY, UPDATEDDATETIME, SYNCHRONIZED` (`MBase.java:42-67`). Written by `beforeCreateUpdateCallback` (GMT timestamps) — called from `ServiceLayerAspect.java:743`.
- **STATUS semantics:** Boolean in MBase; `status` enum in dead `Base` class. Domain-specific status columns additionally found: `ISSUESTATUS` (4), `ISACTIVE` (3), `ORDERSTATUS` (3), `REQUESTSTATUS` (2), `KITSTATUS`, `RESOLVEDSTATUS`, `SOLR_STATUS`, `COVID_STATUS`, `IS_DELETED` (4), `DIETACTIONSTATUS`, `COMPLAINTRESOLUTIONSTATUS`, etc. (39 direct `@Column(name="STATUS")` + ~50 domain statuses).
- **Soft-delete = `STATUS`/`IS_DELETED` flag; no hard deletes in procs either** (`p_isdelete char`, `p_iscancelled char` in `CIMS_ADDPATIENTBILLESTIMATIONDETAILS.sql`).
- **Base64-encoded PII in DB:** `@EncodeAsBase64` fields — `GenericDAO.java:566-578` decodes after fetch; procs `ENCODEFUNCTION`/`DECODEFUNCTION`/`DECODEUTF8ENCODEDSTRING`/`ENCODEUTF8DECODEDSTRING`/`DECODEBASE64` used on `T_Registration_Int` (`artifacts/migration-scripts/MigratePatientRegistrationUtil.java:36`).

---

## 5. Native SQL & Oracle-Specific Constructs (Java) — inventory

### 5.1 Query API
- Fluent builder `GenericQuery` (`dao/GenericQuery.java`): `getQ(sql).values(...).nativeQ().get()/list()/update()`, `.namedStoredProcQ()`, `.criteria()`, `QueryType{NAMED,NATIVE,TYPED,CRITERIA,STORED_PROC}`.
- `createNativeQuery` in `GenericDAO.java:550,610,638,697`; `GenericQuery.java:543,564`.
- `getGenericDAO().getQ(...)` calls in services: **436** (top: CommonUtilServiceImpl 119, BillingCommonUtilServiceImpl 51, IntegrationServiceImpl 37, SendNotificationServiceImpl 36, LinenLaundryServiceImpl 32, QueueManagmentServiceImpl 28, FileManagementServiceImpl 27, CWFServiceImpl 27, BioMedicalWasteManagementServiceImpl 26).
- Raw `"SELECT ..."` string literals: 121 in `jobs/ApplicationJobs.java`, 118 in `util/HL7Helper.java`, 78 in `CommonUtilServiceImpl.java`, 37 `SendNotificationServiceImpl`, 28 `QueueManagmentServiceImpl`, 27 `FileManagementServiceImpl`, 23 each `BillingCommonUtilServiceImpl`/`CWFServiceImpl`/`FileStoreUtil`, 22 `IntegrationServiceImpl`, 13 `InventoryJobSchedule`, 10 `RcmServiceImpl`/`LinenLaundryServiceImpl`.

### 5.2 Oracle-specific function/construct usage (file:line evidence)

| Construct | Java hits | Key files (line) |
|---|---|---|
| `TRUNC(` | 102 | `ApplicationJobs.java` (85), `QueueManagmentServiceImpl` (10), `CommonUtilServiceImpl` (5) |
| `SYSDATE` | 70 | `ApplicationJobs` (53), `EHRSyncJobs` (8), `CommonUtilServiceImpl` (7) |
| `TRUNC(SYSDATE)` | 53 | `ApplicationJobs` (e.g. :1163 `ADDEDDATETIME > TRUNC(SYSDATE)`), `EHRSyncJobs`, `CommonUtilServiceImpl` |
| `NEXTVAL` | 30 | `HL7Helper` (19), `AuditHelper` (4), `BaseServiceImpl:569,571`, `ServiceLayerAspect:725,727`, `GenericDAO:857` |
| `DUAL` | 27 | `HL7Helper`, `ApplicationJobs`, `RulesEngineUtil` (8), `BaseServiceImpl` (4), `AuditHelper` (4), `ServiceLayerAspect:725` |
| `DECODE(` | 25 | `CommonUtilServiceImpl` (10), `SolrUtil` (5), `QueueManagmentServiceImpl` (2), `CAeSignService` (2), `IdEncryptionUtil` (2), `GenericDAO` (1) |
| `TO_DATE(` | 24 | `ApplicationJobs` (14), `QueueManagmentServiceImpl` (10) |
| `ROWNUM` | 19 | `HL7Helper` (5), `ApplicationJobs` (8), `RulesEngineUtil` (8), `BaseServiceImpl` (4), `AuditHelper` (4) — e.g. `BaseServiceImpl:945` `ROWNUM <= 50` |
| `NVL(` | 6 | `CommonUtilServiceImpl`, `SolrUtil`, `FileManagementServiceImpl`, `CWFServiceImpl` |
| `TO_CHAR(` | 6 | `ApplicationJobs`, `CommonUtilServiceImpl` |
| `MERGE INTO` | 2 | `BaseServiceImpl:928,935` (`MERGE INTO T_FREQ_ITEMS USING dual ON (...)`) |
| `LISTAGG` | 2 | `BaseServiceImpl:945` (`LISTAGG(ID, ',') WITHIN GROUP(ORDER BY ID) ... ROWNUM <= 50`) |
| `TO_TIMESTAMP` | 2 | `BaseServiceImpl:929,936` (`to_timestamp(...,'yyyy-mm-dd hh24:mi:ss.xff')`) |
| `FETCH FIRST 1 ROW ONLY` | 1 | `BioMedicalWasteManagementServiceImpl:347` (SQL:2008 — PG-compatible) |
| `CONNECT BY` | 0 (Java) | 1 in proc: `CIMS_GETITEMCURRENTREORDERLISTFORPO_1.sql:62` |
| `SELECT 1 FROM DUAL` | props | `application.properties:101` validationQuery |

### 5.3 Dual-dialect (Oracle/PostgreSQL) branches already present — 9 sites
`indexOf("oracle") != -1` switches: `ServiceLayerAspect:724`, `InventoryJobSchedule:94,480`, `BaseServiceImpl:568,577,585,593`, `BillingCommonUtilServiceImpl:380`, `CommonUtilServiceImpl:2863`. Postgres branch uses `SELECT NEXTVAL('seq')`, `'t'/'f'` booleans, `'true'/'false'`. Rest of the SQL base is Oracle-only.

### 5.4 Unmapped core tables (native-SQL only, NO JPA entity)
- `T_REGISTRATION` / `T_REGISTRATION_DETAILS` (`CommonUtilServiceImpl.java:808,817-821,1001,1282`)
- `T_Registration_Int` / `T_Registration_Details_Int` (`CommonUtilServiceImpl:720,730,2156,2166`; `artifacts/migration-scripts/MigratePatientRegistrationUtil.java:36`)
- `T_coronaStats` (`CommonUtilServiceImpl:820`), `t_transfer`, `t_admission` (`InventoryJobSchedule:487`), `t_patientrequisition` (`ApplicationJobs:739`), `T_OutboundHIS` (also entity `T_OUTBOUNDHIS`), `Config_Application` (native: `MigratePatientRegistrationUtil.java:91`)
- **No main Patient table entity exists in models** — patient master is `T_REGISTRATION`-family, accessed 100% via native SQL.

---

## 6. Stored Procedures / Functions / Triggers / Sequences (evidence)

### 6.1 generated-procs/ — 719 SQL files
- **692 PROCEDURE + 17 FUNCTION + 11 PACKAGE** (`CREATE OR REPLACE EDITIONABLE ...` matches) — all `CIMS_`-prefixed (972 CIMS_ refs), legacy "CIMS" product lineage.
- **All are Oracle PL/SQL**, e.g. `CIMS_ADDADMISSIONDETAILS.sql` — `NUMBER/VARCHAR2/TIMESTAMP` params, `SELECT ... INTO ... FROM dual`, `ROWNUM=1`, `EXCEPTION WHEN NO_DATA_FOUND THEN`, `SYS_REFCURSOR` OUT params.
- `SYS_REFCURSOR` OUT: 473 files; `IN OUT`/`IN OUT NOCOPY`: 389 files; `EXCEPTION`/`WHEN`: 504 files.
- Base64 helpers: `ENCODEFUNCTION`, `DECODEFUNCTION`, `DECODEUTF8ENCODEDSTRING`, `ENCODEUTF8DECODEDSTRING`, `DECODEBASE64`, `CONVERT_CHARTONUM`, `GETFULLNAMEFROMDOCTORID1`, `FNCREATENUMBERIDENTITYTABLE`, `SEQ_CREATE_SCRIPT`, `SEQ_DROP_SCRIPT`.
- **Triggers: 0** (`CREATE TRIGGER`/`TRIGGER`: 0 matches).
- **`CREATE SEQUENCE`: 4** (only in the 3 script-generator files above, not literal DDL for each table).
- Proc-side Oracle usage: `ROWNUM` 486, `TO_CHAR` 474, `SYSDATE` 287, `.NEXTVAL` 190, `NVL(` 84, `TRUNC(` 17, `TO_DATE` 11, `DECODE` 6, `CONNECT BY` 1.
- **30 stored procs actively called from Java** via `namedStoredProcQ()` (EclipseLink `StoredProcedureQuery`): `CIMS_ADDCHARGE`, `CIMS_ADDITEMSTOCK_UPDATE`, `CIMS_GETCHARGES`, `CIMS_GETCHARGESTEMP`, `CIMS_GETPATIENTADVANCE`, `CIMS_ITEMSALE_VISIT`, `CIMS_GETITEMSALESDETAILS`, `CIMS_GETITEMSALESRETURN_VISIT`, `CIMS_GETITEMSALESRETURNDETAILS`, `CIMS_GETSPONSORBYADMISSIONID_NEW_DOC`, `CIMS_GETITEMCATEGORY`, `CIMS_GETALLSERVICEMASTERDETAILSBYDESCRIPTION`, `CIMS_GETITEMTAXLISTFORUNIT`, `CIMS_GETPATIENTGENERALDETAILSLISTFORSEARCHNEW/STRICT`, `CIMS_GETPATHOTESTPARAMETERANDSUBTESTDETAILS_UPDATED_NEW2(_UNITWISE/NEW3)`, `CIMS_GETINVSTOCKJOBSCHEDULE`, `CIMS_GETITEMSBYSTOCKFORLINENREQUEST`, `CIMS_JOBSCHEDULING`, `CIMS_UPDATECHARGES(TEMP)`, `CIMS_CONFIGUREUNITPARAMETER`, `CIMS_CheckPackageSubscriptionByAdmId`, `cims_getappointmentfornotification`, `CIMS_rpt_PathoResultEntry_new1`, `CIMS_rpt_PathoTemplateReport_Final1`, `CIMS_rptPathResultEntryFroCultureTest_Final1`, `CIMS_ADDITEMSTOCK_UPDATE`.
  - Evidence: `services/BillingCommonUtilServiceImpl.java:110,121,133,147,155,179,240,264`; `services/CommonUtilServiceImpl.java:649,662,674,1117,1120,2972-2978`.
- Stored-proc call convention: DTO fields annotated `@StoredProcNamedParam` + `spFieldLst` cache (`GenericDAO.java:771-801`); BigDecimal scale 4 rounding.

### 6.2 artifacts/
- `artifacts/migration-scripts/` — Java migration utils only (`MigratePatientRegistrationUtil.java`, `MigratePatientAdmissionUtil.java`, `UpdateLdapID.java`), which READ legacy schema: `T_Registration_Int`, `T_Registration_Details_Int`, `T_OutboundHIS`, `Config_Application`, `M_PrefixMaster`, `M_MaritalStatusMaster`, `M_CityMaster`, `M_StateMaster`, `M_CountryMaster`, `M_ReligionMaster`, `M_NationalityMaster`, `M_GenderMaster` (lines 36,91,114,126,149,165,191,222,235,249,308).
- `artifacts/risl/` — Android APK assets (`ihms_online/`), `persistence.xml` + `application.properties` (Rajasthan RISL deployment variant), no SQL.
- `artifacts/Equipment Interface/` — OLIS lab instrument jar only.
- No `CREATE OR REPLACE`/`CREATE TRIGGER`/`CREATE SEQUENCE` outside generated-procs found in src/main/resources (testdata = txml/json clinical forms only).

---

## 7. Data-Access Pattern (Spring Data disabled)

- `spring.data.jpa.repositories.enabled=false` (`application.properties:1286`) → **no repository interfaces**.
- All persistence through `GenericDAO` (`dao/GenericDAO.java`, `@PersistenceContext EntityManager` at :241-246; `persist` :276, `merge` :330) + `GenericQuery` fluent SQL builder (`dao/GenericQuery.java`).
- Services extend `BaseServiceImpl` which exposes `getGenericDAO()`; AOP aspects (`ServiceLayerAspect`) interpose on `GenericDAO.save` for ID/audit/cache (`:697-750`).
- `@CrudApi`/`@CrudCached` annotations on entities drive generic CRUD APIs + local cache (`M_ADMISSIONTYPE` example `masters/Admissiontype.java:31-32`).

---

## 8. Table Inventory (full, verified from @Table annotations)

Extraction method: regex `@Table\(name = "..."\)` across `src/main/java/com/ohum/models/**` + all packages. **808 unique @Table names**; +35 aasmaan convention-mapped = **~843 tables** across ~898 entities.

### Counts by module
| Package | Files | @Entity | @Table | Prefix |
|---|---|---|---|---|
| masters | 609 | 608 | 608 | M_ (582), CONFIG_APPLICATION, block_mst/city_mst/district_mst |
| transactionals (+bloodBank 21, Nursing 4, payment 3) | 165 | 165 | 162 | T_ (172 total across pkgs), t_* lowercase variants |
| ejs (E-Jan/ASHA Android) | 61 | 57 | 57 | ANDROID_*, ES_*, MASTER_*, MEDIA*, USER_*, PROFILE, INDICATORS |
| aasmaan (Aasmaan app) | 35 | 35 | 0 (convention) | <CLASSNAME> uppercase |
| trx (+abdm 6) | 15 | 15 | 15 | T_* (incl. T_PATIENT_ABDM*, T_USER_FACEAUTH, T_REFRENCETID) |
| txt (temp/queue) | 10 | 10 | 10 | t_* / T_* (t_patienttokens, user_detail_temp, T_APPOINTMENT, T_NURSEPATIENTDETAILS...) |
| chiranjeevi | 2 | 2 | 2 | T_REFRENCETID, BENEFICIARY_ENROLMENT_IHMS |
| hcx | 2 | 2 | 2 | T_NHCX_REQUEST(+_DETAILS) |
| rcm | 2 | 2 | 2 | T_ADMISSION_TID, T_ELIGIBILITYCHECK |
| statistical | 2 | 2 | 2 | s_servicewisecollection, s_userwisecollection |
| dto/ (entities) | 21 | 21 | 1 (@Table) | T_NA_ITEMDETAILS + defaults |

### Top shared tables (refs in services/jobs/util/aspects/dao; word-boundary matched)
| Table | Refs | Consumers (sample) |
|---|---|---|
| T_Encounter | 52 | CommonUtilServiceImpl (12+), IntegrationServiceImpl, RcmServiceImpl, BillingCommonUtilServiceImpl, CWFServiceImpl, EHRSyncJobs |
| T_OUTBOUNDHIS | 47 | HL7Helper (19 NEXTVAL+), IntegrationServiceImpl, ApplicationJobs |
| M_UNITMASTER | 45 | CommonUtilServiceImpl, BaseServiceImpl, ApplicationJobs, RulesEngineUtil |
| M_DEPARTMENTMASTER | 32 | CommonUtilServiceImpl, QueueManagmentServiceImpl |
| M_BEDMASTER | 26 | CommonUtilServiceImpl, ApplicationJobs, QueueManagmentServiceImpl |
| T_USERDETAILS | 25 | CommonUtilServiceImpl, ApplicationJobs, SolrUtil |
| district_mst | 22 | CommonUtilServiceImpl, IntegrationServiceImpl |
| T_LINENREQUEST | 20 | LinenLaundryServiceImpl (32 getQ) |
| M_ITEMMASTER / T_ITEMCLINIC / T_SPONSOR / T_BIOMEDICALWASTE | 17-18 | ItemSales, Inventory, CommonUtil, BioMedicalWasteManagementServiceImpl |
| MEDIA (ejs) | 65 | IntegrationServiceImpl (5) + mapping util |
| USER_ (ejs) | 11 | AuthenticationServiceImpl, IntegrationServiceImpl |
| CONFIG_APPLICATION | 9 | CommonUtilServiceImpl, BaseServiceImpl |

### Status columns (convention sample)
`STATUS` (36 direct + MBase-inherited everywhere), `ISSUESTATUS`, `ISACTIVE`, `ORDERSTATUS`, `REQUESTSTATUS`, `KITSTATUS`, `RESOLVEDSTATUS`, `SOLR_STATUS`, `COVID_STATUS`, `IS_DELETED`, `ISDEATH`, `OCCUPIED` (`M_BEDMASTER` char '0'/'1' in procs `CIMS_ADDADMISSIONDETAILS.sql:108`), `ISCANCELLED`, `ISDELETE`.

---

## 9. Open Questions / Unknowns
- Exact DDL for unmapped tables (T_REGISTRATION, T_REGISTRATION_DETAILS, T_REGISTRATION_INT, T_REGISTRATION_DETAILS_INT, T_coronaStats, t_transfer, t_admission, Config_Application) — must be dumped from Oracle (`ALL_TABLES`/`DBMS_METADATA`).
- Whether the 719 procs match current schema columns (some reference legacy names: `T_UserDetails`, `M_StaffMaster`, `M_WardMaster`, `T_AdmissionDetails` vs entity `T_ADMISSIONDETAILS`? — case-sensitive Oracle objects; proc bodies show exact case `T_AdmissionDetails`).
- Sequence ownership: `IHMSDOCITDEVMODS_PROD_V1` owner confirmed in `SEQ_CREATE_SCRIPT.sql:5`.
- `eclipselink.jpa.naming_into_indexed=true` (custom property) — effect on generated SQL unknown.
- kundera props (`application.properties:23-27`: kundera.nodes/keyspace=zdocit) hint at MongoDB usage — not confirmed in code.

---

## 10. Top Migration Risks (evidence-backed)
1. **~843 tables, ~898 entities, all `Long` PKs via per-table sequences** — every sequence must be recreated in PG (dual-dialect code exists but only 9 branch sites; the other ~2000 SQL strings are Oracle-only).
2. **TRUNC(SYSDATE) (53) / SYSDATE (70) / TO_DATE (24) / TO_CHAR (6) / TRUNC (102)** — date-arithmetic rewrite needed in `ApplicationJobs.java` (85 TRUNC), `QueueManagmentServiceImpl`, `CommonUtilServiceImpl`, `EHRSyncJobs`.
3. **ROWNUM (19) pagination** (`BaseServiceImpl:945`, HL7Helper, RulesEngineUtil, ApplicationJobs) → `LIMIT`/`OFFSET` or row_number().
4. **MERGE INTO (2) + LISTAGG (2)** in `BaseServiceImpl` → PG `INSERT ... ON CONFLICT` / `string_agg`.
5. **DECODE (25) / NVL (6)** → CASE / COALESCE (mostly `SolrUtil`, `CommonUtilServiceImpl`).
6. **719 Oracle PL/SQL procs (692 procedures/17 functions/11 packages, CIMS_*) — 30 live-called** via EclipseLink `namedStoredProcQ` with `SYS_REFCURSOR` OUT, `IN OUT` params, base64 helpers, `UTL_ENCODE`, `UTL_FILE`, DB-side `connect by` — must be ported to PL/pgSQL or replaced by app-side logic.
7. **Unmapped core patient tables** (T_REGISTRATION family) — schema not recoverable from Java; native-SQL-only access.
8. **Case-sensitive/odd table names** (`t_creditNote`, `T_AdmissionDetails`, `T_Registration_int`, `M_AdditiveMaster`, `user_detail_temp`, `T_AppBookingTypeMaster`) — PG lowercases unquoted identifiers; `@Table` names are quoted by EclipseLink → must preserve exact case or normalize everywhere.
9. **Boolean-as-char/number semantics**: MBase `STATUS` Boolean mapped to Oracle NUMBER; procs use `char` `'1'/'0'`, `p_isdelete char`, `Occupied='1'`; PG branch `'t'/'f'` (BaseServiceImpl:577-598) — inconsistent types across tables (`Boolean` vs `char` vs `number`).
10. **Base64-encoded PII columns + `EncodeAsBase64`/`Base64Data`** annotations (GenericDAO:566-578, 779-783) and `phi.encryption` props — data migration must not break encoding; PG text/bytea semantics differ.
11. **Base hierarchy drift** — `Base.java` (createdAt/status enum) unused; `EjsBase` uses `BigInteger` id vs `Long` elsewhere; `autoSequence=false` per-entity overrides.
12. **Dual-PU legacy XML** (`ZDocit-HIS-SQL-Server-PU`) and commented MySQL/Mongo (kundera) configs indicate historical multi-DB ambitions — clean target is single PG.
