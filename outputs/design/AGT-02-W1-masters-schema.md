# AGT-02 — Wave 1 Target Schema Design: MIG-011 (Unit/Facility/Location/Doctor Masters) + MIG-012 (Item Master & Item-Stock Foundation)

**Author:** AGT-02 (PostgreSQL / Persistence engineer)
**Date:** 2026-08-17
**Status:** Design proposal — read-only. No migration files, no repo modifications (one report under `outputs/design/`).
**Directives honoured:** IMP-DEC-018 (target schema authored by modernization team — no Oracle dump required; clean-first from legacy JPA evidence), IMP-DEC-003 (explicit per-table sequences on migrated tables; IDENTITY allowed for new non-migrated tables), IMP-DEC-007 (parity tables keep 1:1 quoted legacy names — this design adds **clean** target tables alongside them), IMP-DEC-019 (W1 packages), IMP-DEC-011/014/015 (read contract), §55 (single-writer ownership questions are **flagged, not resolved**), §56 (ONE-TABLE-ONE-OWNER; no shared tables / no cross-schema FKs; borrowed data via API/events).

---

## 1. Scope & evidence base

Wave 1 packages covered (from `EXECUTION-BACKLOG.md`):

| Package | Objective (backlog) | Legacy evidence used (read-only) |
|---|---|---|
| **MIG-011** | Unit/facility/location & doctor masters — Unitmaster (1,691-LOC entity, split into unit + config), Doctor master, department/ward/room; facility-scoped authz prerequisites | `D:\ihms\src\main\java\com\ohum\models\masters\` — `Unitmaster.java` (M_UNITMASTER), `Subunitmaster.java`, `Doctormaster.java` (M_DOCTORMASTER), `Doctortypemaster.java`, `Specialization.java`, `Countrymaster.java`, `Statemaster.java`, `DistrictMaster.java` (`district_mst`), `Citymaster.java` (`city_mst`), `Areamaster.java`, `Buildingmaster.java`, `Floormaster.java`, `Wingmaster.java`, `Roommaster.java`, `Cabinmaster.java`, `Wardmaster.java`; `MBase.java` audit block |
| **MIG-012** | Item master + item stock foundation (shared-plane ownership; single writer per stock table) | `Itemmaster.java` (M_ITEMMASTER, `@CrudApi(value="item")`), `Item_Mst.java` (M_ITEM_MST), `Itemcategory.java`, `Itemgroup.java`, `Brandmaster.java`, `Molecule.java`, `Unitofmeasure.java`, `Storeagetype.java`, `Unitofstorage.java`, `Store.java` (M_STORE), `Itembatchmaster.java` (M_ITEMBATCHMASTER), `Supplier.java`, `transactionals/ItemStock.java` (T_ITEMSTOCK), `transactionals/ItemSupplier.java` (T_ITEMSUPPLIER), `masters/ReorderAndSafetyMaster.java`, plus inventory/pharmacy frontend schemas |

Frontend (FAUG) evidence — `mastersConfigration.json` + `modules/*/schemas/*.json` (cross-check in §7):
- Hospital group [0]: `#masters:unitmaster/view` (schema `modules/masters/hospital/schemas/unitmaster.json`, CRUD `api/rest/mdm/addUnitMaster`, list `api/rest/unitmaster/<unit_id>?search=&page=&count=&status=`), `#masters:departmentmst/view`, `#masters:cabinmasterwrapper`, `#masters:doctoraddressmaster/view`.
- Admission group [1]: `#masters:wingmaster/view`, `#masters:floormaster/view`, `#masters:buildingmaster/view`, `#masters:wardmaster/view`, `#masters:bedmaster/view`, `#masters:roommaster/view`, `#masters:doctortypemaster/view`.
- Patient group [2]: `#masters:countrymaster/view` … `#masters:statemaster/view`, `#masters:citymaster/view`, `#masters:areamaster/view`, `#masters:districtmaster/view`.
- Inventory group [8]: `#masters:itemgroup/view`, `#masters:itemcategory/view`, `#masters:itemmaster/view` (schema `modules/masters/inventory/schemas/itemmaster.json` — CRUD `api/rest/item`, list `api/rest/masterConfigService/getItemmasterListAsset`), `#masters:molecule/view`, `#masters:therapeuticclass/view`, `#masters:preganancyclass/view`, `#masters:supplier/view`, `#masters:store/view` (schema `modules/masters/inventory/schemas/store.json` — CRUD `api/rest/store`, list `api/rest/store?searchFld=description&status=`), `#masters:taxmaster/view`, `#masters:unitofmeasure/view`, `#masters:storeagetype/view`, `#masters:item_Mst/view`, `#masters:brandmaster/view`, `#masters:suppliermaster/view`, `#masters:reorderAndSafetyMaster/view`.
- Doctor master itself: generic commons schema `modules/commons/schemas/doctormaster.json` (route pattern `#masters:<schema>/view|add|edit/:id`, CRUD `api/rest/doctormaster`, DELETE `__cufl=status` soft-delete) — registered in `fjs-config_commons.json`.

Target-service context: reference-service (`services/reference-service`) already exposes `GET /api/v1/masters` read contract (S0, IMP-DEC-002): `/{entity}/all`, `/{entity}/{id}`, `/{entity}` (unit-scoped), plus entity registry with **allowedFilters from legacy `@CrudApi` annotations** (`MasterEntityRegistry.java`). `ihms_master` schema + V1/V2/V3 migrations exist (MIG-006 S0: M_UNITMASTER/M_DEPARTMENTMASTER/M_COMPANYMASTER parity + outbox). MIG-010 (W1) will add the write path (crudadd/crudupdate/crudremove + MBase.validate + REQUIRES_NEW + outbox `MasterDataChanged`, IMP-DEC-004/019).

---

## 2. Design principles applied (clean target, not 1:1 copy)

1. **Clean-first naming:** snake_case, lowercase, no `M_`/`T_` prefixes; explicit PK names, comments on every table/column with legacy provenance (`<- M_UNITMASTER`).
2. **Parity coexistence:** the legacy-shaped quoted tables (V1–V3, IMP-DEC-007) remain as the MIG-005 gateway shadow. New clean tables are the target of new writes and eventual cutover; the MIG-006-style mapping ledger records each clean column ← legacy column (backlog item, not created here).
3. **Audit block (MBase convention) on every table** — snake_cased:
   `id BIGINT PK`, `unit_id BIGINT` (unit scope; NULL only where explicitly global — see §4 geo), `status BOOLEAN NOT NULL DEFAULT TRUE`, `created_unit_id BIGINT`, `updated_unit_id BIGINT`, `added_by BIGINT`, `added_datetime TIMESTAMP(3)`, `updated_by BIGINT`, `updated_datetime TIMESTAMP(3)`, `synchronized BOOLEAN NOT NULL DEFAULT FALSE`.
   (Legacy MBase: `UnitID, STATUS, CREATEDUNITID, UPDATEDUNITID, ADDEDBY, ADDEDDATETIME, UPDATEDBY, UPDATEDDATETIME, SYNCHRONIZED`; timestamps written in GMT — `beforeCreateUpdateCallback`.)
4. **Types:** BIGINT ids; `NUMERIC(19,4)` quantities, `NUMERIC(19,2)` money/rates (never float — BR-BILL-002 rounding); `TIMESTAMP(3)` for event/audit; `DATE` for date-only (DOB, tenure); `BOOLEAN` where legacy Java `Boolean` and no proc cross-talk; `SMALLINT + CHECK` for legacy numeric state codes that are **load-bearing** (e.g. movement type); `JSONB` for long-tail feature/config flags (per IMP-DEC-010 hardening; avoids 100-column god tables while keeping queryable hot flags typed); `BYTEA` for blobs in migration interim (logo/photo/signature/document) — flag: object storage (MIG-021) as target (see §9 decision D-8).
5. **PK strategy (IMP-DEC-003):** every table that will receive MIG-001 backfilled legacy rows gets an **explicit per-table sequence** (`unit_seq`, `doctor_seq`, `item_seq`, …), seeded to `MAX(id)+1` by the V2-style seed script after backfill (legacy ID continuity: unit ids, doctor ids, item ids are referenced by transactions and by external refs). **IDENTITY (`BIGINT GENERATED … AS IDENTITY`) is used only for genuinely new tables that never carry a legacy id** — in this wave: `item_stock_balance`, `item_stock_ledger` (derived/deduped fresh ids; no legacy id continuity requirement for movement rows).
6. **FKs only intra-schema** (§56): logical references across schemas (e.g. `store.wing_id → ihms_master.facility_location`) are plain BIGINT columns **without FK**, validated/denormalized by API; documented per column.
7. **Status/soft-delete:** `status BOOLEAN` (active/inactive) mirrors legacy `STATUS` on MBase; legacy DELETE uses soft-delete `__cufl=status`; new DELETE endpoint flips `status=false`.

---

## 3. Schema placement recommendation (per ONE-TABLE-ONE-OWNER / §56)

### MIG-011 → `ihms_master` (reference-service = Master Data service)

**Recommendation: keep in `ihms_master`.** Do **not** create a new `ihms_provider` schema.

Justification:
1. ONE-TABLE-ONE-OWNER: every table below is *written only* by master-data administration (generic masters CRUD; `@CrudApi` entities) and *read* everywhere. The reference-service is the single writer; all other services consume via `GET /api/v1/masters/{entity}` + `MasterDataChanged` events (IMP-DEC-004) — this is exactly the S21 bounded context (`10-target-architecture §2.1`).
2. Service-boundary rule: a separate `ihms_provider` schema would imply a separate provider service — a bounded context that does **not** exist in the 29-service catalog and would duplicate the master-service role for doctors/locations. Creating an orphan schema with no owning service violates §56.
3. Cache ownership: legacy cache keys (`doctorMaster`, `unitMaster`, `statesMaster`, `districtMasterMap`, `citiesMaster`, `areaMaster`, … `@CrudCached` annotations) are already owned by Reference (`10 §2.2` final rule: cache keys have a single owner, Reference). Moving the tables without the cache contract would break the cache-invalidation contract.
4. **Doctor master — contested ownership flagged (decision D-2, §9):** `POSTGRESQL-MIGRATION §1.1` records the M_STAFFMASTER/M_DOCTORMASTER conflict — IAM (S22) “owns profile tables” vs Reference “owns all M_*”. This wave designs `doctor` in `ihms_master` with a **logical `iam_user_id`** column (filled when MIG-004 IAM lands; no cross-schema FK so the table can stay put either way). If Agent 00 later decides IAM owns the *identity/profile* slice, only identity-shaped columns migrate; the clinical master stays in `ihms_master`.

### MIG-012 → new `ihms_item` schema (owned by the Item/Inventory service)

**Recommendation: the whole item vertical (definitional + stock) goes to `ihms_item`.** Do not place item/stock in `ihms_master`.

Justification:
1. ONE-TABLE-ONE-OWNER + hot-write: `T_ITEMSTOCK` is a concurrency hotspot (`10 §3.4` row-level locking; ~90 legacy write call sites across 6 services + proc `CIMS_UPDATE_CONSIGNEDGRN_ITEMSTOCK` — MIG-012 backlog). Stock tables must have exactly one writer (the Item service stock API) and that writer must own the schema it writes.
2. FK integrity: `item_stock_balance.item_id → item.id` and `item_batch.item_id → item.id` are intra-domain FKs. If item definitional tables sat in `ihms_master` and stock in `ihms_item`, §56 forbids the cross-schema FK — the item service would have to fetch item rows via API for every stock row constraint, reintroducing the classic item/item-batch integrity hazard the target schema exists to remove.
3. `10-target-architecture §2.1` assigns Inventory (10) ownership of M_ITEMMASTER, T_ITEMSTOCK, T_ITEMBATCHMASTER, T_PURCHASEORDER…; `POSTGRESQL-MIGRATION §1.1` confirms “Inventory owns M_ITEMMASTER + T_ITEMSTOCK (+batch)”.
4. The item classification lookups (group/category/brand/molecule/UoM/storage type/supplier/store) are item-domain-only — meaningless outside inventory/pharmacy — so they move with the domain (one-table-one-owner: each has a single writer, the Item service).
5. Reference-service keeps the **`itemMaster` cache-key contract** and emits `MasterDataChanged` (IMP-DEC-004 outbox) so consumers of the legacy `itemMaster` cache key keep parity — cache contract owner ≠ table owner (this split already exists in `10 §2.1` row 10 note).
6. **Alternative (flagged, decision D-4, §9):** keep item *definitional* tables in `ihms_master` and only stock in `ihms_item`. Consequence: cross-schema FK loss + item service needs item read-model synchronization (violates ONE-TABLE-ONE-OWNER for item group/category which would then be written by Reference but semantically owned by Item). Not recommended.

**Net effect:** new schema `ihms_item` (created by the item service's own Flyway; reference-service keeps `ihms_master`). The MIG-005 gateway routes legacy `api/rest/item/*`, `api/rest/store/*`, `api/rest/itemstock*` calls toward the item service / stock API during strangler.

---
---

## 4. MIG-011 target tables — `ihms_master` (reference-service)

Shared audit block (repeated in every table; snake_case of the MBase MIG-006 convention):

```sql
-- AUDIT_BLOCK (drop-in columns; TIMESTAMP(3) = GMT, matching MBase.beforeCreateUpdateCallback)
unit_id          BIGINT,               -- owning facility (legacy UnitID); NULL only for global reference rows (geo)
status           BOOLEAN      NOT NULL DEFAULT TRUE,   -- active/inactive; legacy MBase STATUS
created_unit_id  BIGINT,               -- unit that created the row (legacy CREATEDUNITID)
updated_unit_id  BIGINT,               -- unit that last updated the row (legacy UPDATEDUNITID)
added_by         BIGINT,               -- user id (legacy ADDEDBY)
added_datetime   TIMESTAMP(3),         -- legacy ADDEDDATETIME (GMT)
updated_by       BIGINT,               -- legacy UPDATEDBY
updated_datetime TIMESTAMP(3),         -- legacy UPDATEDDATETIME (GMT)
synchronized     BOOLEAN      NOT NULL DEFAULT FALSE -- legacy SYNCHRONIZED
```

Sequence + seed pattern (IMP-DEC-003; one per migrated table, `<table>_seq`, seeded to MAX(id)+1 by MIG-001 backfill; identical to V2 mechanics):

```sql
CREATE SEQUENCE IF NOT EXISTS ihms_master.unit_seq    START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_master.doctor_seq  START WITH 1;
-- ... per table
-- V2-style idempotent reseed after backfill:
-- SELECT setval('ihms_master.unit_seq', GREATEST((SELECT COALESCE(MAX(id),1) FROM ihms_master.unit), 1));
```

### 4.1 `unit` — facility (hospital/tenant) master

**Legacy source:** `M_UNITMASTER` (`Unitmaster.java`, ~95 persisted columns, 1,691 LOC — split into `unit` + `unit_config` per MIG-011 objective). **API entity name:** `unit` (registry name; gateway maps legacy `unitmaster` → `unit`). Filters: `id, status` (legacy `@CrudApi(allowedFilters={"id","status"})`). Search fields: `name` (frontend list `api/rest/unitmaster/<unit_id>?search=&searchFld=name`).

```sql
CREATE TABLE ihms_master.unit (
    id                BIGINT      NOT NULL,
    -- identity / naming
    unit_code         VARCHAR(64) NOT NULL,               -- legacy CODE (unique per network -> see index)
    name              VARCHAR(255) NOT NULL,              -- legacy NAME
    description       VARCHAR(500),                       -- legacy DESCRIPTION
    unit_type_id      BIGINT,                             -- legacy TYPE -> hospitaltypemaster (logical ref; lookup table in a later masters package)
    unit_category_id  BIGINT,                             -- legacy CATEGORY -> hospitalcategorymaster (logical ref)
    parent_unit_id    BIGINT,                             -- legacy PARENTID (self FK: parent/administrative unit network)
    is_parent         BOOLEAN     NOT NULL DEFAULT FALSE, -- legacy ISPARENT
    is_administrative BOOLEAN     NOT NULL DEFAULT FALSE, -- legacy ISADMINISTRATIVE (V3 parity col)
    is_operational    BOOLEAN     NOT NULL DEFAULT FALSE, -- legacy ISOPERATIONAL
    is_virtual        BOOLEAN     NOT NULL DEFAULT FALSE, -- legacy ISVIRTUAL
    virtual_unit_id   BIGINT,                             -- legacy VIRTUALID
    is_confirmatory   BOOLEAN,                            -- legacy ISCONFIRMATORY (V3 parity col)
    is_offline        BOOLEAN,                            -- legacy ISOFFLINE (V3 parity col)
    is_central_store  BOOLEAN     NOT NULL DEFAULT FALSE, -- legacy ISCENTRALSTORE (flag: single central store for network)
    bed_capacity      BIGINT,                             -- legacy BEDCAPACITY
    -- address / contact (denormalized from legacy ADDRESSLINE1..3, COUNTRY..PINCODE, CITY)
    address_line1     VARCHAR(500),
    address_line2     VARCHAR(500),
    address_line3     VARCHAR(500),
    city_id           BIGINT,                             -- legacy CITY/DISTRICT text -> normalized to geo_city (decision: normalize)
    district_id       BIGINT,                             -- legacy DISTRICTID
    taluka            VARCHAR(255),
    area              VARCHAR(255),
    pincode           VARCHAR(16),
    contact_no        VARCHAR(32),                        -- legacy CONTACTNO
    std_code          INTEGER,                            -- legacy RESISTDCODE
    country_code_intl INTEGER,                            -- legacy RESINOCOUNTRYCODE
    fax_no            VARCHAR(32),                        -- legacy FAXNO
    email             VARCHAR(255),                       -- legacy EMAIL
    longitude         VARCHAR(32),                        -- legacy LONGITUDE (String in legacy)
    latitude          VARCHAR(32),                        -- legacy LATIUTDE (sic; value is String)
    -- registrations / licences
    pharmacy_license_no VARCHAR(64),                      -- legacy PHARMACYLICENSENO
    clinic_reg_no       VARCHAR(64),                      -- legacy CLINICREGNO
    shop_no             VARCHAR(64),                      -- legacy SHOPNO
    trade_no            VARCHAR(64),                      -- legacy TRADENO
    vat_tin_no          VARCHAR(64),                      -- legacy VAT_TIN_No
    unit_prefix_for_mrn VARCHAR(16),                      -- legacy UNITPREFIXFORMRNO (MRN numbering, BR-PAT-001)
    -- tenure
    contract_from_date DATE,                              -- legacy CONTRACTFROMDATE
    contract_to_date   DATE,                              -- legacy CONTRACTTODATE
    -- national / external identifiers (legacy scattered columns)
    abdm_hfr_id       VARCHAR(64),                        -- legacy ABDMHFRID
    abdm_id           VARCHAR(64),                        -- legacy ABDMID
    hmis_id           VARCHAR(64),                        -- legacy HMISID
    hmis_code         VARCHAR(64),                        -- legacy HMISCODE
    ehr_code          VARCHAR(64),                        -- legacy EHRCODE
    nin_code          VARCHAR(64),                        -- legacy NINCODE
    eaushadhi_store_id   VARCHAR(64),                     -- legacy EAUSHADHISTOREID (external system key)
    eaushadhi_warehouse_id VARCHAR(64),                   -- legacy EAUSHADHIWAREHOUSEID
    eraktkosh_unit_id  VARCHAR(64),                       -- legacy ERAKTKOSHUNITID
    logo               BYTEA,                             -- legacy LOGO (blob; MIG-021 object storage decision D-8)
    config_json        JSONB        NOT NULL DEFAULT '{}'::jsonb, -- long-tail feature flags (see unit_config)
    -- AUDIT_BLOCK explicitly:
    unit_id          BIGINT,                              -- owning unit (self; typically = id for top facility)
    status           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_unit_id  BIGINT,
    updated_unit_id  BIGINT,
    added_by         BIGINT,
    added_datetime   TIMESTAMP(3),
    updated_by       BIGINT,
    updated_datetime TIMESTAMP(3),
    synchronized     BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_unit PRIMARY KEY (id),
    CONSTRAINT fk_unit_parent FOREIGN KEY (parent_unit_id) REFERENCES ihms_master.unit (id),
    CONSTRAINT uq_unit_code UNIQUE (unit_code)             -- legacy CODE; single namespace per installation
);
COMMENT ON TABLE ihms_master.unit IS 'Clean target of M_UNITMASTER (Unitmaster.java). Facility/hospital master; unit_config holds feature flags. Legacy ~95 cols split across unit, unit_config, config_json.';
COMMENT ON COLUMN ihms_master.unit.parent_unit_id IS 'Self FK: parent/administrative unit (network of units). Legacy PARENTID (no FK).';
```

**Justification for split:** the legacy entity is 1,691 LOC mixing identity, licences and ~60 enablement flags. Clean design: `unit` (above) + `unit_config` (hot queryable flags) + `config_json` (long tail). `unit_config`:

```sql
CREATE TABLE ihms_master.unit_config (
    id                BIGINT  NOT NULL,
    unit_id           BIGINT  NOT NULL,                    -- 1:1 with unit (also serves unit scope)
    is_med_verify             BOOLEAN NOT NULL DEFAULT FALSE, -- legacy ISMEDVERIFY
    is_centrally_outsourced_lab BOOLEAN NOT NULL DEFAULT FALSE, -- legacy ISCENTRALLYOUTSOURCEDLAB
    is_cwf_coversheet         SMALLINT,                     -- legacy ISCWFCOVERSHEET (Short; semantics uncertain)
    is_auto_indent            BOOLEAN NOT NULL DEFAULT FALSE, -- legacy ISAUTOINDENT
    auto_indent_execution_time VARCHAR(16),                 -- legacy AUTOINDENTEXECUTIONTIME
    auto_indent_for_unit      BIGINT,                       -- legacy ISAUTOINDENTFOR
    is_multi_main_store       BOOLEAN NOT NULL DEFAULT FALSE, -- legacy ISMULTIMAINSTORE
    show_patho_location       BOOLEAN NOT NULL DEFAULT FALSE, -- legacy SHOWPATHOLOCATION
    show_patho_subspecialization BOOLEAN NOT NULL DEFAULT FALSE, -- legacy SHOWPATHOSUBSPECIALIZATION
    is_pharmacy_upload_presc  BOOLEAN NOT NULL DEFAULT FALSE, -- legacy ISPHARMACYUPLOADPRESC
    is_pharmacy_visit_required BOOLEAN NOT NULL DEFAULT FALSE, -- legacy ISPHARMACYVISITREQUIRED
    is_op_label_required      BOOLEAN NOT NULL DEFAULT FALSE, -- legacy ISOPLABELREQUIRED
    allow_direct_online_appointment BOOLEAN NOT NULL DEFAULT FALSE, -- legacy ALLOWDIRECTONLINEAPPOINTMENT
    allow_lab_normal_result_review BOOLEAN NOT NULL DEFAULT TRUE,  -- legacy ALLOWLABNORMALRESULTREVIEW
    bill_number_list_integration BOOLEAN NOT NULL DEFAULT FALSE,   -- legacy BILLNUMBERLISINTEGRATION
    consult_type              SMALLINT NOT NULL DEFAULT 0,  -- legacy CONSULTTYPE
    enable_ehr                BOOLEAN NOT NULL DEFAULT FALSE, -- legacy ENABLEEHR
    enable_otp_based_login    BOOLEAN NOT NULL DEFAULT FALSE, -- legacy ENABLEOTPBASEDLOGIN
    enable_whatsapp           BOOLEAN NOT NULL DEFAULT FALSE, -- legacy ENABLEWHATSAPP
    is_eaushadhi_data_sync    BOOLEAN NOT NULL DEFAULT FALSE, -- legacy ISEAUSHADHIDATASYNC
    locale_description        VARCHAR(500),                 -- legacy LOCALEDESCRIPTION
    -- AUDIT_BLOCK (same shape as unit)
    status           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_unit_id  BIGINT, updated_unit_id BIGINT,
    added_by BIGINT, added_datetime TIMESTAMP(3), updated_by BIGINT, updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_unit_config PRIMARY KEY (id),
    CONSTRAINT fk_unit_config_unit FOREIGN KEY (unit_id) REFERENCES ihms_master.unit (id),
    CONSTRAINT uq_unit_config_unit UNIQUE (unit_id)
);
-- Remaining legacy flag/feature columns (merchantCode, centralStoreIds, apiKey, apiToken, allowedOperatorMobileNumbersJson,
-- histoNoCategory, isMedVerify handled above, pathiesType, hospitalType, isCwfCoversheet, eaushadhi store desc,
-- bedCapacities, contract dates on unit, eRaktkoshKey, etc.) land in unit.config_json pending MIG-001 value census.
```

### 4.2 `subunit`

**Legacy:** `M_SUBUNITMASTER` (`Subunitmaster.java` — code, description only). Simple lookup; API name `subunit`.

```sql
CREATE TABLE ihms_master.subunit (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(64) NOT NULL, description VARCHAR(255),
    CONSTRAINT pk_subunit PRIMARY KEY (id),
    CONSTRAINT uq_subunit_unit_code UNIQUE (unit_id, code)
);
```

### 4.3 `facility_location` — physical location hierarchy (building / floor / wing / room / cabin)

**Legacy sources:** `M_BUILDINGMASTER`, `M_FLOORMASTER`, `M_WINGMASTER`, `M_ROOMMASTER`, `M_CABINMASTER` (plus `M_NURSINGSTATIONMASTER`, `M_WARDMASTER` — see decision D-3). Legacy tables are flat code/description tables with **no parent FKs** (hierarchy is implicit: legacy `wingmaster` screens and `roommaster`/`wardmaster` reference `floorId`/`wingId` in other entities; `store.wingid/floorid`, `wardmaster.floorid/wingid`). Clean design normalizes the physical tree into one table.

```sql
CREATE TABLE ihms_master.facility_location (
    id            BIGINT       NOT NULL,
    unit_id       BIGINT,                                    -- owning facility (unit scope)
    location_type VARCHAR(16)  NOT NULL
        CHECK (location_type IN ('BUILDING','FLOOR','WING','ROOM','CABIN','WARD','NURSING_STATION','OT_THEATRE')),
    code          VARCHAR(64),                               -- legacy CODE (unique per (unit, type) -> uq)
    name          VARCHAR(255) NOT NULL,                     -- legacy DESCRIPTION (display name)
    name_hindi    VARCHAR(255),                              -- legacy ROOMHINDI / WINGNAMEHINDI / CABINHINDI
    parent_id     BIGINT,                                    -- tree parent: FLOOR->BUILDING, WING->FLOOR, ROOM->FLOOR/WING, CABIN/OT->ROOM
    sequence_no   INTEGER,                                   -- display/print order (legacy implicit)
    is_unique_sequence BOOLEAN NOT NULL DEFAULT FALSE,       -- legacy WINGMASTER.iSUNIQUESEQUENCE (token numbering)
    token_prefix  VARCHAR(16),                               -- legacy ROOMMASTER.TOKENPREFIX / CABINMASTER.TOKEN
    is_room_amenities BOOLEAN   NOT NULL DEFAULT FALSE,      -- legacy ROOMMASTER.ISROOMAMMENITIES
    -- AUDIT_BLOCK
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT,
    added_by BIGINT, added_datetime TIMESTAMP(3), updated_by BIGINT, updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_facility_location PRIMARY KEY (id),
    CONSTRAINT fk_facility_location_parent FOREIGN KEY (parent_id) REFERENCES ihms_master.facility_location (id),
    CONSTRAINT uq_facility_location_unit_type_code UNIQUE (unit_id, location_type, code)
);
CREATE INDEX ix_facility_location_parent ON ihms_master.facility_location (parent_id);
CREATE INDEX ix_facility_location_unit_status ON ihms_master.facility_location (unit_id, status);
COMMENT ON TABLE ihms_master.facility_location IS 'Clean target of M_BUILDINGMASTER/M_FLOORMASTER/M_WINGMASTER/M_ROOMMASTER/M_CABINMASTER (+NURSING_STATION). Legacy had no PK FKs; hierarchy normalized with parent_id. WARD/BED operational state -> ihms_adt (decision D-3).';
```

**Notes / decision (D-3):** (a) single hierarchy table vs legacy per-type tables — recommended to keep ONE table with `location_type` because legacy types are 2-column code/description lookups whose only relationships are *references from other tables*; the gateway can still expose per-type endpoints. (b) `WARD` and `BED` are **operational state** written by ADT (`POSTGRESQL-MIGRATION §1.1` recommends moving M_BEDMASTER wholesale to `ihms_adt`); this wave therefore defines the `WARD` enum value but ships no ward/bed tables — cutover decision needed. (c) `OT_THEATRE` (`M_OTTHEATREMASTER`, has WARDID) is domain-owned by OT (S11) — enum reserved, table deferred.

### 4.4 Geographic reference — `geo_country`, `geo_state`, `geo_district`, `geo_city`, `geo_area`

**Legacy sources:** `M_COUNTRYMASTER`, `M_STATEMASTER`, `district_mst` (`DistrictMaster.java`), `city_mst` (`Citymaster.java`), `M_AREAMASTER`. Legacy is mostly **global reference** (seeded country/state/district/city census data) even though it extends MBase (has UnitID). Clean design: normalized FK chain country → state → district → city → area; `unit_id` **NULL** (global rows) — decision D-6.

```sql
CREATE TABLE ihms_master.geo_country (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(8)  NOT NULL,           -- legacy CODE (ISO-style)
    name VARCHAR(120) NOT NULL,          -- legacy DESCRIPTION
    is_default BOOLEAN NOT NULL DEFAULT FALSE,  -- legacy ISDEFAULTCOUNTRY
    nationality_name VARCHAR(120),       -- legacy NATIONALITYID (logical ref to nationality master; see D-6)
    CONSTRAINT pk_geo_country PRIMARY KEY (id),
    CONSTRAINT uq_geo_country_code UNIQUE (code)
);

CREATE TABLE ihms_master.geo_state (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    country_id BIGINT NOT NULL REFERENCES ihms_master.geo_country (id),
    code VARCHAR(16) NOT NULL, name VARCHAR(120) NOT NULL, name_hindi VARCHAR(120),  -- legacy STATEHINDI
    abdm_code VARCHAR(32),               -- legacy ABDM_CODE
    CONSTRAINT pk_geo_state PRIMARY KEY (id),
    CONSTRAINT uq_geo_state_country_code UNIQUE (country_id, code)
);

CREATE TABLE ihms_master.geo_district (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    state_id BIGINT NOT NULL REFERENCES ihms_master.geo_state (id),
    code VARCHAR(16) NOT NULL, name VARCHAR(120) NOT NULL,
    census_2011_code VARCHAR(16),        -- legacy district_code_2011
    zone_id BIGINT,                      -- legacy ZONEID -> district zone master (logical ref; D-6)
    abdm_district_code VARCHAR(32),      -- legacy ABDM_DISTRICT_CODE
    CONSTRAINT pk_geo_district PRIMARY KEY (id),
    CONSTRAINT uq_geo_district_state_code UNIQUE (state_id, code)
);

CREATE TABLE ihms_master.geo_city (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    district_id BIGINT REFERENCES ihms_master.geo_district (id),
    state_id BIGINT NOT NULL REFERENCES ihms_master.geo_state (id),  -- denormalized for state-level listings
    code VARCHAR(16) NOT NULL, name VARCHAR(120) NOT NULL,
    census_code VARCHAR(16),             -- legacy city_code
    CONSTRAINT pk_geo_city PRIMARY KEY (id),
    CONSTRAINT uq_geo_city_district_code UNIQUE (district_id, code)
);

CREATE TABLE ihms_master.geo_area (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    city_id BIGINT NOT NULL REFERENCES ihms_master.geo_city (id),
    code VARCHAR(32) NOT NULL, name VARCHAR(120) NOT NULL,
    CONSTRAINT pk_geo_area PRIMARY KEY (id),
    CONSTRAINT uq_geo_area_city_code UNIQUE (city_id, code)
);
COMMENT ON TABLE ihms_master.geo_country IS 'Clean target of M_COUNTRYMASTER. Global reference: unit_id NULL (decision D-6). Nationality is a separate legacy master (M_NATIONALITYMASTER); kept as logical name here.';
COMMENT ON TABLE ihms_master.geo_district IS 'Clean target of district_mst (DistrictMaster.java). Census + ABDM codes preserved for govt integrations.';
COMMENT ON TABLE ihms_master.geo_city IS 'Clean target of city_mst (Citymaster.java). Legacy district_id column name DISTRICT_ID kept semantically as FK to geo_district.';
```

### 4.5 Doctor master — `doctor`, `doctor_type`, `specialization`, `doctor_department`, `doctor_unit`

**Legacy sources:** `M_DOCTORMASTER` (`Doctormaster.java`, `@CrudApi` — no allowedFilters, i.e. legacy ignores filters, GET with no filter map), `M_DOCTORTYPEMASTER`, `M_SPECIALIZATION` (+`M_SPECIALIZATIONMASTER` — see D-2/D-6), `M_DOCTORDEPARTMENTDETAILS`, `M_UNITDOCTORDETAILS`. API names: `doctor`, `doctor-type`, `specialization`, `doctor-department`, `doctor-unit`. Doctor list screen searches name (first/last) — frontend typeahead on `doctormaster` list; filters will be whitelisted per registry (recommend: `status, firstName, lastName, specializationId, doctorTypeId, unitId`).

```sql
CREATE TABLE ihms_master.doctor_type (               -- M_DOCTORTYPEMASTER (screen: Admission -> Doctor Type)
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(64) NOT NULL, description VARCHAR(255) NOT NULL,
    CONSTRAINT pk_doctor_type PRIMARY KEY (id),
    CONSTRAINT uq_doctor_type_unit_code UNIQUE (unit_id, code)
);

CREATE TABLE ihms_master.specialization (            -- M_SPECIALIZATION (screen: Billing -> Group Master)
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(64) NOT NULL, description VARCHAR(255) NOT NULL,
    print_order INTEGER,                             -- legacy PRINTORDER
    ledger_name VARCHAR(64),                         -- legacy LEDGERNAME (accounting)
    is_doctor_show BOOLEAN NOT NULL DEFAULT TRUE,    -- legacy isdoctorshow
    is_department_show BOOLEAN NOT NULL DEFAULT TRUE,-- legacy isdepartmentshow
    is_recalculate_service BOOLEAN NOT NULL DEFAULT FALSE, -- legacy isrecalulateservice
    CONSTRAINT pk_specialization PRIMARY KEY (id),
    CONSTRAINT uq_specialization_unit_code UNIQUE (unit_id, code)
);
COMMENT ON TABLE ihms_master.specialization IS 'Clean target of M_SPECIALIZATION. UNCERTAIN: doctor screen labels specialization as "Service Group" and loads service-master data (sp_service_master_full); M_SPECIALIZATIONMASTER (code/description/refentityper) is a parallel lookup. Which master SPECIALIZATIONID actually references must be confirmed at MIG-001 (D-2/D-6).';

CREATE TABLE ihms_master.doctor (
    id BIGINT NOT NULL,
    -- identity (profile)
    first_name VARCHAR(120) NOT NULL,                  -- legacy FIRSTNAME
    middle_name VARCHAR(120),                          -- legacy MIDDLENAME
    last_name VARCHAR(120),                            -- legacy LASTNAME
    gender_id BIGINT,                                  -- legacy GENDERID -> M_GENDERMASTER (logical ref; patient masters)
    dob DATE,                                          -- legacy DOB (Timestamp in legacy; date-only semantics)
    photo BYTEA,                                       -- legacy PHOTO (blob; D-8)
    digital_signature BYTEA,                           -- legacy DIGITALSIGNATURE (blob; D-8)
    marital_status SMALLINT,                           -- legacy MARITALSTATUS (Integer; enum map needed)
    mobile_code BIGINT,                                -- legacy MOBILECODE
    mobile_no VARCHAR(16),                             -- legacy MOBILENO (Long in legacy -> String; preserves leading zeros!)
    email_id VARCHAR(255),                             -- legacy EMAILID
    iam_user_id BIGINT,                                -- NEW: logical ref to ihms_iam user (MIG-004); NULL until IAM lands; NO cross-schema FK (D-2)
    -- employment / registration
    employee_number VARCHAR(64),                       -- legacy EMPLOYEENUMBER
    pf_number VARCHAR(64),                             -- legacy PFNUMBER
    pan_number VARCHAR(32),                            -- legacy PANNUMBER
    date_of_joining DATE,                              -- legacy DATEOFJOINING
    access_card_number VARCHAR(64),                    -- legacy ACCESSCARDNUMBER
    registration_number VARCHAR(64),                   -- legacy REGESTRATIONNUMBER (medical council reg no.)
    doctor_no VARCHAR(64),                             -- legacy DOCTORNO
    prefix_id BIGINT,                                  -- legacy PREFIXID -> M_PREFIXMASTER (logical ref)
    is_nurse BOOLEAN NOT NULL DEFAULT FALSE,           -- legacy ISNURSE
    -- clinical
    education VARCHAR(255),                            -- legacy EDUCATION (string; could become link table later)
    experience VARCHAR(120),                           -- legacy EXPERIENCE (string)
    specialization_id BIGINT REFERENCES ihms_master.specialization (id),  -- legacy SPECIALIZATIONID (UNCERTAIN target, see comment)
    subspecialization_id BIGINT,                       -- legacy SUBSPECIALIZATIONID (logical; M_SUBSPECIALIZATION)
    doctor_type_id BIGINT REFERENCES ihms_master.doctor_type (id),        -- legacy DOCTORTYPEID
    is_concession_authority BOOLEAN NOT NULL DEFAULT FALSE, -- legacy ISCONCESSIONAUTHORITY
    is_resident_doctor BOOLEAN NOT NULL DEFAULT FALSE,  -- legacy ISRESIDENTDOCTOR
    is_hod BOOLEAN NOT NULL DEFAULT FALSE,              -- legacy ISHOD
    phone_no VARCHAR(32),                               -- legacy PHONENO
    contact_number_one VARCHAR(16),                     -- legacy CONTACTNO1 (Long in legacy)
    contact_number_two VARCHAR(16),                     -- legacy CONTACTNO2
    country_id BIGINT, state_id BIGINT, city_id BIGINT, pincode VARCHAR(16), area VARCHAR(120), -- legacy address block (logical geo refs)
    -- AUDIT_BLOCK
    unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_doctor PRIMARY KEY (id),
    CONSTRAINT uq_doctor_iam_user UNIQUE (iam_user_id)   -- 1:1 doctor<->login user when IAM lands (D-2)
);
CREATE INDEX ix_doctor_unit_status ON ihms_master.doctor (unit_id, status);
CREATE INDEX ix_doctor_name ON ihms_master.doctor (last_name, first_name);
CREATE INDEX ix_doctor_specialization ON ihms_master.doctor (specialization_id);
COMMENT ON TABLE ihms_master.doctor IS 'Clean target of M_DOCTORMASTER. Identity vs clinical split keeps doctor as clinical master data (reference-service) with logical iam_user_id to IAM (D-2). APPLICABLEUNITS string -> doctor_unit link table.';
COMMENT ON COLUMN ihms_master.doctor.mobile_no IS 'Legacy MOBILENO is Long; mobile numbers with leading zeros/phone format prefer VARCHAR(16). Value conversion at MIG-001 must preserve digits (leading-zero audit needed).';

-- doctor <-> unit membership (legacy M_UNITDOCTORDETAILS: DOCTORID column + UnitID scope; legacy APPLICABLEUNITS comma string)
CREATE TABLE ihms_master.doctor_unit (
    id BIGINT NOT NULL, unit_id BIGINT NOT NULL, doctor_id BIGINT NOT NULL REFERENCES ihms_master.doctor (id),
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_doctor_unit PRIMARY KEY (id),
    CONSTRAINT uq_doctor_unit UNIQUE (doctor_id, unit_id)
);

-- doctor <-> department mapping (legacy M_DOCTORDEPARTMENTDETAILS)
CREATE TABLE ihms_master.doctor_department (
    id BIGINT NOT NULL, unit_id BIGINT,
    doctor_id BIGINT NOT NULL REFERENCES ihms_master.doctor (id),
    department_id BIGINT NOT NULL,                       -- logical ref -> ihms_master.department (M_DEPARTMENTMASTER parity; clean dept table follows MIG-010)
    is_default_doctor BOOLEAN NOT NULL DEFAULT FALSE,     -- legacy ISDEFAULTDOCTOR
    max_patients_per_day INTEGER,                        -- legacy MAXPATIENTNUMFORADAY
    building_ids JSONB,                                  -- legacy BUILDINGIDS (string of ids; normalized later)
    is_unit_head BOOLEAN NOT NULL DEFAULT FALSE,         -- legacy ISUNITHEAD
    clinical_level_id BIGINT,                            -- legacy CLINICALLEVELID -> M_CLINICALLEVEL (logical)
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_doctor_department PRIMARY KEY (id),
    CONSTRAINT uq_doctor_department UNIQUE (doctor_id, department_id)
);
```

**MIG-011 extension tables (deferred, listed for traceability):** `doctorschedule`/`doctorscheduledetailmaster`/`doctorrostermaster` (schedules/rosters — W2 appointment), `doctorwaiver` (transactional), `filelinkingfordoctors`, `doctoraddressmaster`/`doctoraddressdetails` (address types), `doctorvisittypedetails`.

---
---

## 5. MIG-012 target tables — new schema `ihms_item` (Item/Inventory service)

Schema bootstrap + sequences + audit block same conventions as §4. New schema is created by the **item service's** Flyway migrations (planned V1/V2 there; reference-service's `ihms_master` is untouched by MIG-012 except shared logical references).

```sql
CREATE SCHEMA IF NOT EXISTS ihms_item;
-- Explicit sequences for migrated tables (IMP-DEC-003): item_group_seq, item_category_seq, molecule_seq,
-- brand_seq, unit_of_measure_seq, storage_type_seq, supplier_seq, item_seq, store_seq, item_batch_seq,
-- item_reorder_policy_seq, item_supplier_seq  (seeded to MAX(id)+1 by the MIG-001 backfill script)
-- IDENTITY (no sequence) for NEW tables: item_stock_balance, item_stock_ledger
```

### 5.1 Item classification lookups (all item-domain-owned; ONE-TABLE-ONE-OWNER = Item service)

```sql
CREATE TABLE ihms_item.item_group (                    -- M_ITEMGROUP (screen: Inventory -> Item Group)
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(64) NOT NULL, description VARCHAR(255) NOT NULL,
    ledger_name VARCHAR(64),                           -- legacy LEDGERNAME (accounting head)
    prefix VARCHAR(16),                                -- legacy PREFIX (item code prefix)
    account_selection VARCHAR(64),                     -- legacy ACCOUNTSELECTION
    item_type VARCHAR(32),                             -- legacy ITEMTYPE (drug/consumable/asset...; value map needed)
    CONSTRAINT pk_item_group PRIMARY KEY (id),
    CONSTRAINT uq_item_group_unit_code UNIQUE (unit_id, code)
);
CREATE INDEX ix_item_group_unit_status ON ihms_item.item_group (unit_id, status);

CREATE TABLE ihms_item.item_category (                 -- M_ITEMCATEGORY (screen: Inventory -> Item Category)
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    item_group_id BIGINT NOT NULL REFERENCES ihms_item.item_group (id),   -- legacy ITEMGROUPID
    code VARCHAR(64) NOT NULL, description VARCHAR(255) NOT NULL,
    ledger_name VARCHAR(64),                           -- legacy LEDGERNAME
    service_id BIGINT,                                 -- legacy SERVICEID (logical ref -> service master)
    maintenance_default_id BIGINT,                     -- legacy MAINTENANCEDEFAULTID (equipment default)
    expiry_days BIGINT,                                -- legacy EXPIRYDAYS (default shelf life)
    CONSTRAINT pk_item_category PRIMARY KEY (id),
    CONSTRAINT uq_item_category_group_code UNIQUE (item_group_id, code)
);
CREATE INDEX ix_item_category_group ON ihms_item.item_category (item_group_id);

CREATE TABLE ihms_item.molecule (                      -- M_MOLECULE (screen: Inventory -> Generic)
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(64) NOT NULL, description VARCHAR(255) NOT NULL,
    ingredient_list VARCHAR(1000),                     -- legacy INGREDIENTLIST (comma list)
    CONSTRAINT pk_molecule PRIMARY KEY (id),
    CONSTRAINT uq_molecule_unit_code UNIQUE (unit_id, code)
);

CREATE TABLE ihms_item.brand (                         -- M_BRANDMASTER (screen: Inventory -> Brand)
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    item_category_id BIGINT REFERENCES ihms_item.item_category (id),  -- legacy ITEMCATEGORYID
    description VARCHAR(255) NOT NULL,                 -- legacy DESCRIPTION (no code column in legacy)
    CONSTRAINT pk_brand PRIMARY KEY (id),
    CONSTRAINT uq_brand_unit_description UNIQUE (unit_id, description)
);

CREATE TABLE ihms_item.unit_of_measure (               -- M_UNITOFMEASURE (screen: Inventory -> Unit Of Measure)
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(32) NOT NULL, description VARCHAR(120) NOT NULL,
    quantity BIGINT,                                   -- legacy Quantity (container count)
    is_default BOOLEAN NOT NULL DEFAULT FALSE,         -- legacy defaultUOM
    is_scientific BOOLEAN NOT NULL DEFAULT FALSE,      -- legacy sciUnits (SI units flag)
    CONSTRAINT pk_unit_of_measure PRIMARY KEY (id),
    CONSTRAINT uq_unit_of_measure_unit_code UNIQUE (unit_id, code)
);

CREATE TABLE ihms_item.storage_type (                  -- MERGES M_STOREAGETYPE (screen: Storage Type) + M_UNITOFSTORAGE (no schema; see D-5)
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    kind VARCHAR(16) NOT NULL DEFAULT 'STORE_AGE' CHECK (kind IN ('STORE_AGE','UNIT_OF_STORAGE')), -- provenance discriminator
    code VARCHAR(64) NOT NULL, description VARCHAR(255) NOT NULL,
    CONSTRAINT pk_storage_type PRIMARY KEY (id),
    CONSTRAINT uq_storage_type_unit_kind_code UNIQUE (unit_id, kind, code)
);
COMMENT ON TABLE ihms_item.storage_type IS 'One table for the legacy pair M_STOREAGETYPE (actively used, screen exists) and M_UNITOFSTORAGE (no frontend, uncertain active use). kind preserves provenance for MIG-001 backfill; decision D-5 whether to keep both legacy lookups distinct.';
```

### 5.2 `supplier` — vendor/party master (item-domain)

**Legacy sources:** `M_SUPPLIER` (active screen `#masters:supplier/view`) + `M_SUPPLIERMASTER` (parent/panel supplier, screen `#masters:suppliermaster/view`). `M_ITEMCOMPANY` exists but item master's manufacturer/marketer pickers load `api/rest/supplier/all?status=true&mflag=true` — i.e. suppliers flagged `mflag` are manufacturers (see uncertain list, §8).

```sql
CREATE TABLE ihms_item.supplier (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(64) NOT NULL, description VARCHAR(255) NOT NULL,
    address_line1 VARCHAR(255), address_line2 VARCHAR(255), address_line3 VARCHAR(255),
    city VARCHAR(120), state VARCHAR(120), country VARCHAR(120), pincode VARCHAR(16),   -- legacy free text address (geo normalization optional)
    contact_person1_name VARCHAR(120), contact_person1_mobile VARCHAR(32), contact_person1_email VARCHAR(255),
    contact_person2_name VARCHAR(120), contact_person2_mobile VARCHAR(32), contact_person2_email VARCHAR(255),
    phone_no VARCHAR(32), fax VARCHAR(32),
    mode_of_payment BIGINT, tax_nature BIGINT, term_of_payment BIGINT, currency BIGINT,  -- logical refs (lookups in later packages)
    mst_number VARCHAR(64), vat_number VARCHAR(64), cst_number VARCHAR(64),
    drug_licence_number VARCHAR(64), service_tax_number VARCHAR(64), pan_number VARCHAR(32),
    is_manufacturer BOOLEAN NOT NULL DEFAULT FALSE,    -- legacy MFLAG (drives item master manufacturer picker)
    is_implants_supplier BOOLEAN NOT NULL DEFAULT FALSE,  -- legacy ISIMPLANTSUPPLIER
    is_inhouse_supplier BOOLEAN NOT NULL DEFAULT FALSE,   -- legacy ISINHOUSESUPPLIER
    is_service_agent BOOLEAN NOT NULL DEFAULT FALSE,      -- legacy ISSERVICEAGENT
    is_empanelled BOOLEAN NOT NULL DEFAULT FALSE,         -- legacy EMPANEL flag (allowed filter)
    empanelled_by BIGINT, supplier_unit_id BIGINT,        -- legacy EMPANELLEDBY / SUPPLIERUNITID
    start_date DATE, end_date DATE,                       -- legacy STARTDATE/ENDDATE (empanelment window)
    supplier_master_id BIGINT,                            -- legacy SUPPLIERMASTERID -> M_SUPPLIERMASTER (logical)
    terms_and_condition_id VARCHAR(200),                  -- legacy TERMSANDCONDITIONID (string list?)
    CONSTRAINT pk_supplier PRIMARY KEY (id),
    CONSTRAINT uq_supplier_unit_code UNIQUE (unit_id, code)
);
CREATE INDEX ix_supplier_unit_status ON ihms_item.supplier (unit_id, status);
CREATE INDEX ix_supplier_mfg_flag ON ihms_item.supplier (unit_id, is_manufacturer) WHERE is_manufacturer;
```

### 5.3 `item` — item master (clean split of M_ITEMMASTER)

**Legacy source:** `M_ITEMMASTER` (`Itemmaster.java`, ~110 persisted columns, `@CrudApi(value="item", allowedFilters={"status","itemGroup","itemCategory","dispencingtype","pregclass","therclass","moleculename"})`). List screen

> DDL execution order note: `item_reorder_policy` and `item_supplier` below reference `store` (defined in §5.4) — Flyway scripts must create `store` before those two tables.

> List screen: `api/rest/masterConfigService/getItemmasterListAsset?search=&status=&itemGroup=&itemCategory=`; search string matches name/code. Clean design: hot query columns typed; long-tail asset/gas/blood-bag flags → `policy_json`/`attributes_json`.

```sql
CREATE TABLE ihms_item.item (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    -- identity & naming
    item_code VARCHAR(128),                              -- legacy ITEMCODE (NOT unique in legacy; part of display key with unit/name)
    item_name VARCHAR(255) NOT NULL,                     -- legacy ITEMNAME (display/print name)
    item_name_hindi VARCHAR(255),                        -- legacy ITEMNAMEHINDI
    item_name_ref_id BIGINT,                             -- legacy ITEM_MSTID -> ihms_item.item_name (M_ITEM_MST dictionary; see below)
    brand_name VARCHAR(255),                             -- legacy BRANDNAME (string; also brand_id below — reconcile at backfill, §8)
    brand_id BIGINT REFERENCES ihms_item.brand (id),     -- legacy BRANDID
    strength VARCHAR(64),                                -- legacy STRENGTH (e.g. "500 mg")
    strength_uom_id BIGINT REFERENCES ihms_item.unit_of_measure (id), -- legacy STRENGTHUOMID
    -- classification
    molecule_id BIGINT REFERENCES ihms_item.molecule (id),            -- legacy MOLECULENAME
    item_group_id BIGINT REFERENCES ihms_item.item_group (id),        -- legacy ITEMGROUP (allowed filter)
    item_category_id BIGINT REFERENCES ihms_item.item_category (id),  -- legacy ITEMCATEGORY (allowed filter)
    dispensing_type_id BIGINT,                                -- legacy DISPENCINGTYPE -> M_DISPENSINGTYPE (dispensing_type lookup is a follow-on table; logical ref for now)
    storage_type_id BIGINT REFERENCES ihms_item.storage_type (id),    -- legacy STOREAGETYPE
    pregnancy_class_id BIGINT,                           -- legacy PREGCLASS -> M_PREGANANCYCLASS (logical)
    therapeutic_class_id BIGINT,                         -- legacy THERCLASS -> M_THERAPEUTICCLASS (logical)
    route_id BIGINT,                                     -- legacy ROUTE -> M_ROUTE (logical)
    code_type_id BIGINT,                                 -- legacy CODETYPE -> M_CODETYPE (logical)
    -- supplier / pricing
    manufacturer_id BIGINT REFERENCES ihms_item.supplier (id),        -- legacy MFGBY (supplier with mflag=true picker)
    marketer_id BIGINT REFERENCES ihms_item.supplier (id),            -- legacy MRKBY
    purchase_rate NUMERIC(19,2),                         -- legacy PURCHASERATE
    mrp NUMERIC(19,2),                                   -- legacy MRP
    sale_price NUMERIC(19,2),                            -- legacy SALEPRICE
    vat_percent NUMERIC(19,2),                           -- legacy VATPER
    vat_applicable_on SMALLINT,                          -- legacy VATAPPLICABLEON (enum map needed)
    inclusive_of_tax BOOLEAN NOT NULL DEFAULT FALSE,     -- legacy INCLUSIVEOFTAX
    discount_on_sale NUMERIC(19,2),                      -- legacy DISCOUNTONSALE
    guide_cost NUMERIC(19,2),                            -- legacy GUIDECOST
    packing_uom_id BIGINT,                               -- legacy PUM (packing unit of measure, logical ref)
    stocking_uom_id BIGINT,                              -- legacy SUM (stocking unit of measure, logical ref)
    conversion_factor NUMERIC(19,4),                     -- legacy CONVERSIONFACTOR (String in legacy -> numeric; audit values)
    -- reorder / demand (item-level defaults; per-store policy in item_reorder_policy)
    batches_required BOOLEAN NOT NULL DEFAULT FALSE,     -- legacy BATCHESREQUIRED (batch-managed item)
    reorder_qty INTEGER,                                 -- legacy REORDERQNT
    avg_daily_demand BIGINT,                             -- legacy AVGDAILYDEMAND
    safety_stock BIGINT,                                 -- legacy SAFTYSTOCK
    is_reorder BOOLEAN NOT NULL DEFAULT FALSE,           -- legacy ISREORDER
    -- orderability / consumption flags
    otc BOOLEAN NOT NULL DEFAULT FALSE,                  -- legacy OTC (over the counter)
    nursing_orderable BOOLEAN NOT NULL DEFAULT TRUE,     -- legacy NURSINGORDERABLE
    doctor_orderable BOOLEAN NOT NULL DEFAULT TRUE,      -- legacy DOCTORORDERABLE
    used_for_kit BOOLEAN NOT NULL DEFAULT FALSE,         -- legacy USEDFORKIT
    used_for_cssd BOOLEAN NOT NULL DEFAULT FALSE,        -- legacy USEDFORCSSD
    decimal_consumption BOOLEAN NOT NULL DEFAULT FALSE,  -- legacy DECIMALCONSUMPTION
    is_reusable BOOLEAN NOT NULL DEFAULT FALSE,          -- legacy ISREUSABLE
    is_kit BOOLEAN NOT NULL DEFAULT FALSE,               -- legacy ISKIT
    partial_dispensing BOOLEAN NOT NULL DEFAULT FALSE,   -- legacy PARTIALDISPENSING
    is_compound BOOLEAN NOT NULL DEFAULT FALSE,          -- legacy ISCOMPOUND
    is_custom_order BOOLEAN NOT NULL DEFAULT FALSE,      -- legacy ISCUSTOMORDER
    is_non_stockable BOOLEAN NOT NULL DEFAULT FALSE,     -- legacy ISNONSTOCKABLE (primitive boolean)
    controlled_substance BOOLEAN NOT NULL DEFAULT FALSE, -- legacy CONTROLLEDSUBSTANCE
    is_life_saving BOOLEAN NOT NULL DEFAULT FALSE,       -- legacy ISLIFESAVING
    is_high_risk BOOLEAN NOT NULL DEFAULT FALSE,         -- legacy ISHIGHRISK
    is_high_cost BOOLEAN NOT NULL DEFAULT FALSE,         -- legacy ISHIGHCOST
    is_asset BOOLEAN NOT NULL DEFAULT FALSE,             -- legacy ISASSET (equipment/asset item)
    ved_class SMALLINT,                                  -- legacy VED (Vital/Essential/Desirable; enum 1-3)
    -- identifiers (national / external)
    inventory_code VARCHAR(128),                         -- legacy INVENTORYCODE
    unique_country_drug_code VARCHAR(64),                -- legacy UNIQUECOUNTRYDRUGCODE
    hsn_code_id VARCHAR(32),                             -- legacy HSNCODEID (GST HSN; logical ref to M_HSNCODEMASTER)
    snomed_code VARCHAR(64), snomed_display VARCHAR(255), -- legacy SNOMEDCODE/SNOMEDDISPLAY
    edl_number VARCHAR(64),                              -- legacy EDLNUMBER
    order_ien VARCHAR(64),                               -- legacy ORDERIEN (VistA/VA MDM integration)
    eaushadhi_item_id BIGINT,                            -- legacy EAUSHADIITEMID (govt procurement sync)
    -- document
    document_name VARCHAR(255), source_url VARCHAR(1000), document BYTEA,   -- legacy DOCUMENTNAME/SOURCEURL/DOCUMENT (blob D-8)
    suggestions VARCHAR(500), remarks VARCHAR(500),       -- legacy SUGGESTIONS/REMARKS
    attributes_json JSONB NOT NULL DEFAULT '{}'::jsonb,   -- long-tail: gas (gastype/gascapacity/pressure/umopressure...), depreciation/AMC
                                                           -- (lifeasset/life/depreciation...), blood-bag (BLOOD_BAG_*), batchcode/expirydate,
                                                           -- vatapplicableon, codeType, maintenance flags, approval counts (see §8)
    CONSTRAINT pk_item PRIMARY KEY (id),
    CONSTRAINT uq_item_unit_code_name UNIQUE (unit_id, item_code, item_name)  -- display uniqueness; audit code collisions at backfill
);
CREATE INDEX ix_item_unit_status ON ihms_item.item (unit_id, status);
CREATE INDEX ix_item_group ON ihms_item.item (item_group_id);
CREATE INDEX ix_item_category ON ihms_item.item (item_category_id);
CREATE INDEX ix_item_molecule ON ihms_item.item (molecule_id);
CREATE INDEX ix_item_name_trgm ON ihms_item.item USING gin (item_name gin_trgm_ops);   -- search parity for the typeahead list (requires pg_trgm)
COMMENT ON TABLE ihms_item.item IS 'Clean target of M_ITEMMASTER. Long-tail legacy flags (gas cylinders, depreciation/AMC, blood-bag, approvals) stay in attributes_json until MIG-001 value census decides which need real columns.';
COMMENT ON COLUMN ihms_item.item.item_name_ref_id IS 'Legacy ITEM_MSTID -> M_ITEM_MST (item name dictionary, screen "Item Name"). Clean: separate small table item_name(id, unit_id, code, description) [M_ITEM_MST] — not expanded here to keep the wave bounded; mapping note for backfill.';

-- item reorder policy per store (M_REORDERANDSAFETYMASTER; screen Inventory -> Reorder Level and Safety Stock)
CREATE TABLE ihms_item.item_reorder_policy (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    store_id BIGINT NOT NULL REFERENCES ihms_item.store (id),   -- legacy STOREID (store defined below)
    item_id BIGINT NOT NULL REFERENCES ihms_item.item (id),     -- legacy ITEMID
    reorder_level NUMERIC(19,4),                              -- legacy REORDERLEVEL
    safety_stock NUMERIC(19,4),                               -- legacy SAFETYSTOCK
    reorder_value NUMERIC(19,2),                              -- legacy REORDERVALUE
    is_automatic BOOLEAN NOT NULL DEFAULT FALSE,              -- legacy ISAUTOMATIC
    CONSTRAINT pk_item_reorder_policy PRIMARY KEY (id),
    CONSTRAINT uq_item_reorder_policy UNIQUE (store_id, item_id)
);
COMMENT ON TABLE ihms_item.item_reorder_policy IS 'Legacy M_REORDERANDSAFETYMASTER also carried denormalized ITEMNAME/ITEMGROUPID/ITEMCATEGORYID columns — dropped in clean design (derivable from item).';

-- item <-> supplier availability mapping (T_ITEMSUPPLIER)
CREATE TABLE ihms_item.item_supplier (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    item_id BIGINT NOT NULL REFERENCES ihms_item.item (id),
    supplier_id BIGINT NOT NULL REFERENCES ihms_item.supplier (id),
    item_category_id BIGINT,                                  -- legacy ITEMCATEGORY (semantics on this mapping uncertain — see §8)
    CONSTRAINT pk_item_supplier PRIMARY KEY (id),
    CONSTRAINT uq_item_supplier UNIQUE (item_id, supplier_id)
);
```

### 5.4 `store` — store master (stock ownership root)

**Legacy source:** `M_STORE` (`Store.java`, `@CrudApi(allowedFilters={"status"})`; screen `#masters:store/view`; list `api/rest/store?searchFld=description&status=`; CRUD `api/rest/store`). One-table-one-owner: store rows are **definitional but stock-typed** — store is the root of every stock row, so it lives in `ihms_item` (alternative in D-4).

```sql
CREATE TABLE ihms_item.store (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(64) NOT NULL, description VARCHAR(255) NOT NULL,
    -- store type flags (legacy booleans; more than one can be true)
    is_main_store BOOLEAN NOT NULL DEFAULT FALSE,      -- legacy ISMAINSTORE
    is_central_store BOOLEAN NOT NULL DEFAULT FALSE,   -- legacy ISCENTRALSTORE
    is_pharmacy_store BOOLEAN NOT NULL DEFAULT FALSE,  -- legacy ISPHARMACYSTORE
    is_cssd_store BOOLEAN NOT NULL DEFAULT FALSE,      -- legacy ISCSSDSTORE
    is_implant_store BOOLEAN NOT NULL DEFAULT FALSE,   -- legacy ISIMPLANTSTORE
    is_asset_store BOOLEAN NOT NULL DEFAULT FALSE,     -- legacy ISASSET (asset/equipment store)
    is_sub_store BOOLEAN NOT NULL DEFAULT FALSE,       -- legacy ISSUBSTORE
    is_patient_requisition BOOLEAN NOT NULL DEFAULT FALSE, -- legacy ISPATIENTREQUISITION
    item_group_id BIGINT REFERENCES ihms_item.item_group (id),   -- legacy ITEMGROUP (store default group)
    -- capabilities (legacy booleans)
    opening_balance BOOLEAN NOT NULL DEFAULT FALSE,    -- legacy OPENINGBALANCE
    indent BOOLEAN NOT NULL DEFAULT FALSE,             -- legacy INDENT
    item_return BOOLEAN NOT NULL DEFAULT FALSE,        -- legacy ITEMRETURN
    goods_received_note BOOLEAN NOT NULL DEFAULT FALSE,-- legacy GOODSRECEIVEDNOTE
    grn_return BOOLEAN NOT NULL DEFAULT FALSE,         -- legacy GRNRETURN
    items_sale BOOLEAN NOT NULL DEFAULT FALSE,         -- legacy ITEMSSALE
    items_sale_return BOOLEAN NOT NULL DEFAULT FALSE,  -- legacy ITEMSSALERETURN
    expiry_item_return BOOLEAN NOT NULL DEFAULT FALSE, -- legacy EXPIRYITEMRETURN
    receive_issue_return BOOLEAN NOT NULL DEFAULT FALSE, -- legacy RECEIVEISSUERETURN
    is_op_ip_store SMALLINT,                           -- legacy ISOPIPSTORE (Long; 0/1/2? — see §8)
    -- location (logical refs to ihms_master, NO cross-schema FK; D-3)
    wing_id BIGINT, floor_id BIGINT,                   -- legacy WINGID/FLOORID -> ihms_master.facility_location
    department VARCHAR(255),                           -- legacy DEPARTMENT (string; see §8)
    days VARCHAR(255),                                 -- legacy DAYS (string; see §8)
    -- numbering/identifiers
    prefix VARCHAR(16),                                -- legacy prefix (receipt/indent prefix)
    sequence_name VARCHAR(64),                         -- legacy SEQUENCE_NAME (per-store doc numbering sequence; BR numbering)
    eaushadhi_store_id VARCHAR(64), eaushadhi_parent_store_id VARCHAR(64),  -- legacy EASTOREID/EAPARENTSTOREID (govt sync)
    gst_no VARCHAR(32),                                -- legacy GSTNO
    pharmacy_license_no VARCHAR(64),                   -- legacy PHARMACYLICENSENO
    blood_store_license_no VARCHAR(64),                -- legacy BLOODSTORELICENCENO
    nhm_blood_bank_code BIGINT,                        -- legacy NHMBLOODBANKCODE
    address_line1 VARCHAR(255), phone_no VARCHAR(32), email VARCHAR(255),  -- legacy ADDRESSLINE1/PHONENO/EMAIL
    CONSTRAINT pk_store PRIMARY KEY (id),
    CONSTRAINT uq_store_unit_code UNIQUE (unit_id, code)
);
CREATE INDEX ix_store_unit_status ON ihms_item.store (unit_id, status);
COMMENT ON COLUMN ihms_item.store.wing_id IS 'Logical reference to ihms_master.facility_location(WING) — no cross-schema FK per §56; validated by the item service against the masters API.';
COMMENT ON COLUMN ihms_item.store.sequence_name IS 'Legacy SEQUENCE_NAME: per-store document number sequence (e.g. indent/PO/GRN number streams). Value contains a DB sequence name in legacy — normalize to a numbering-policy column at backfill (see §8 uncertain).';
```

### 5.5 Stock foundation — `item_batch`, `item_stock_balance`, `item_stock_ledger`

**Legacy source:** `M_ITEMBATCHMASTER` (batch header), `T_ITEMSTOCK` (movement log with running balances — one row per stock movement carrying `stockInHand/blockedStock/availableStock`). **Clean split:** current state = `item_stock_balance` (one row per store/item/batch, hot-write single-writer target); history = `item_stock_ledger` (append-only). This split is the **single-writer stock foundation** flagged per §55 (decision D-1).

```sql
CREATE TABLE ihms_item.item_batch (                    -- M_ITEMBATCHMASTER
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    item_id BIGINT NOT NULL REFERENCES ihms_item.item (id),        -- legacy ITEMID
    batch_code VARCHAR(64) NOT NULL,                              -- legacy BATCHCODE
    expiry_date DATE,                                             -- legacy EXPIRYDATE (Timestamp; date-only semantics)
    manufacturing_date DATE,                                      -- legacy DATE (Timestamp; see §8 DATE/TIME pair)
    quantity NUMERIC(19,4),                                       -- legacy QUANTITY (initial qty)
    conversion_factor NUMERIC(19,4),                              -- legacy CONVERSIONFACTOR
    purchase_rate NUMERIC(19,2), mrp NUMERIC(19,2),               -- legacy PURCHASERATE / MRP
    vat_percentage NUMERIC(19,2), vat_amount NUMERIC(19,2),       -- legacy VATPERCENTAGE / VATAMOUNT
    discount_on_sale NUMERIC(19,2),                               -- legacy DISCOUNTONSALE
    landed_rate NUMERIC(19,2),                                    -- legacy LANDEDRATE
    is_freeze_recall BOOLEAN NOT NULL DEFAULT FALSE,              -- legacy ISFREEZE_RECALL (batch recall)
    freeze_recall_remark VARCHAR(500),                            -- legacy FREEZE_RECALL_REMARK
    is_ext_store BOOLEAN NOT NULL DEFAULT FALSE,                  -- legacy ISEXTSTORE
    is_expiry_date_updated BOOLEAN NOT NULL DEFAULT FALSE,        -- legacy ISEXPIRYDATEUPDATED (expiry-edit audit)
    previous_expiry_date DATE,                                    -- legacy PREVIOUSEXPIRYDATE
    expiry_update_document VARCHAR(500),                          -- legacy EXPIRYUPDATEDOCUMENT
    remarks VARCHAR(500), updated_windows_login VARCHAR(255),     -- legacy REMARKS / UPDATEDWINDOWSLOGINNAME
    CONSTRAINT pk_item_batch PRIMARY KEY (id),
    CONSTRAINT uq_item_batch UNIQUE (item_id, batch_code)
);
CREATE INDEX ix_item_batch_expiry ON ihms_item.item_batch (expiry_date) WHERE expiry_date IS NOT NULL;

-- NEW table (fresh ids -> IDENTITY allowed, IMP-DEC-003). Single-writer hotspot: ONLY the item service stock API mutates this table (D-1).
CREATE TABLE ihms_item.item_stock_balance (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES ihms_item.store (id),
    item_id BIGINT NOT NULL REFERENCES ihms_item.item (id),
    batch_id BIGINT REFERENCES ihms_item.item_batch (id),          -- NULL for non-batch-managed items (batches_required=false)
    department_id BIGINT,                                          -- legacy T_ITEMSTOCK.DEPARTMENTID (department-scoped stock; see D-1/§8)
    qty_in_hand NUMERIC(19,4) NOT NULL DEFAULT 0,                  -- legacy STOCKINHAND
    blocked_qty NUMERIC(19,4) NOT NULL DEFAULT 0,                  -- legacy BLOCKEDSTOCK (reserved/parked)
    available_qty NUMERIC(19,4) NOT NULL DEFAULT 0,                -- legacy AVAILABLESTOCK (= in-hand - blocked; maintained by writer)
    damaged_qty NUMERIC(19,4) NOT NULL DEFAULT 0,                  -- legacy DAMAGEDRETURNSTOCK (damaged-return balance)
    expired_qty NUMERIC(19,4) NOT NULL DEFAULT 0,                  -- legacy EXPIREDRETURNSTOCK (expired-return balance)
    avg_daily_demand NUMERIC(19,4), safety_stock NUMERIC(19,4),    -- legacy AVGDAILYDEMAND/SAFTYSTOCK (mirrored at store-item level)
    is_reorder BOOLEAN NOT NULL DEFAULT FALSE,                     -- legacy ISREORDER
    is_reorder_indent BOOLEAN NOT NULL DEFAULT FALSE,              -- legacy ISREORDERINDENT (auto-indent already raised)
    last_ledger_id BIGINT,                                         -- last item_stock_ledger row that touched this balance
    version BIGINT NOT NULL DEFAULT 0,                             -- optimistic lock for balance updates
    -- AUDIT_BLOCK
    unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_item_stock_balance UNIQUE (store_id, item_id, batch_id)
);
COMMENT ON TABLE ihms_item.item_stock_balance IS 'NEW clean table (no 1:1 legacy table): current stock state per (store,item,batch). Legacy T_ITEMSTOCK rows carry running balances; MIG-001 backfill derives current balance as the last movement per (store,item,batch) (see §8). Row locking: SELECT ... FOR UPDATE on this row in the stock API (10 §3.4). Single writer decision D-1.';
CREATE INDEX ix_item_stock_balance_item ON ihms_item.item_stock_balance (item_id, store_id);
CREATE INDEX ix_item_stock_balance_batch ON ihms_item.item_stock_balance (batch_id) WHERE batch_id IS NOT NULL;

-- NEW table (fresh ids -> IDENTITY). Append-only movement ledger; written in the same transaction as the balance update.
CREATE TABLE ihms_item.item_stock_ledger (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES ihms_item.store (id),
    item_id BIGINT NOT NULL REFERENCES ihms_item.item (id),
    batch_id BIGINT REFERENCES ihms_item.item_batch (id),
    department_id BIGINT,                                          -- legacy T_ITEMSTOCK.DEPARTMENTID
    movement_type SMALLINT NOT NULL CHECK (movement_type BETWEEN 1 AND 30),  -- enum: 1=OPENING_BALANCE 2=GRN/RECEIPT 3=ISSUE 4=SALE 5=RETURN
                                             -- 6=TRANSFER_IN 7=TRANSFER_OUT 8=ADJUSTMENT 9=EXPIRY 10=DAMAGE 11=RECALL ... (map in D-1)
    operation SMALLINT NOT NULL CHECK (operation IN (1,2)),        -- 1=ADD, 2=SUBTRACT (legacy OPERATIONTYPE)
    quantity NUMERIC(19,4) NOT NULL,                               -- legacy TRANSACTIONQUANTITY (signed by operation)
    prev_balance NUMERIC(19,4) NOT NULL,                           -- legacy PREVIOUSBALANCE
    new_balance NUMERIC(19,4) NOT NULL,                            -- legacy STOCKINHAND (post-movement)
    ref_doc_type VARCHAR(32),                                      -- GRN / INDENT / SALE / TRANSFER / POISSUE / ADJUSTMENT / OPENING
    ref_doc_id BIGINT,                                             -- legacy TRANSACTIONID (owner-service doc id; no FK — cross-service)
    grn_id BIGINT,                                                 -- legacy GRNID (logical ref to GRN table in item service, later wave)
    item_supplier_id BIGINT REFERENCES ihms_item.item_supplier (id), -- legacy ITEMSUPPLIERID
    remarks VARCHAR(500),                                          -- legacy REMARKS
    is_cancel BOOLEAN NOT NULL DEFAULT FALSE,                      -- legacy ISCANCEL (reversing entry flag)
    -- AUDIT_BLOCK
    unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX ix_item_stock_ledger_item ON ihms_item.item_stock_ledger (item_id, store_id, added_datetime DESC);
CREATE INDEX ix_item_stock_ledger_ref ON ihms_item.item_stock_ledger (ref_doc_type, ref_doc_id);
COMMENT ON TABLE ihms_item.item_stock_ledger IS 'NEW clean table: append-only movements derived from legacy T_ITEMSTOCK (which stores one row per movement with running balances + TRANSACTIONTYPEID/TRANSACTIONID/OPERATIONTYPE). TRANSACTIONTYPEID value map must be extracted at MIG-001 (see §8); numbers 1..30 reserved, CHECK expands once mapped.';
```

**Stock write contract (single-writer, D-1):** only the item service stock API mutates `item_stock_balance` + `item_stock_ledger` in one transaction (balance row `SELECT … FOR UPDATE`, ledger insert, outbox `StockAdjusted`/`InventoryUpdated` event). Pharmacy/CSSD/Medication/Expiry/OT consumers call the API or subscribe to events — no direct stock writes (legacy dual-write path `PharmacyServiceImpl.java:2140` / `CIMS_ADDITEMSTOCK` retired by W5).

---

## 6. Master-service API contract sketch (consistent with `/api/v1/masters/{entity}`)

Read endpoints already live (S0, `MasterReadController.java`); write endpoints are MIG-010 scope (IMP-DEC-019) and reuse the same surface:

```text
GET    /api/v1/masters                      -> entity registry (names + allowed filters)
GET    /api/v1/masters/{entity}             -> unit-scoped list  (params: unitId, filters*, search, searchFld, page, count)
GET    /api/v1/masters/{entity}/all         -> unscoped list     (params: filters*, search, searchFld, page, count)
GET    /api/v1/masters/{entity}/{id}        -> single by id      (params: unitId, filters*)
POST   /api/v1/masters/{entity}             -> create            (body: entity JSON; server assigns id from <entity>_seq; outbox MasterDataChanged)
PUT    /api/v1/masters/{entity}/{id}        -> update            (partial update; __cufl-style field list optional)
DELETE /api/v1/masters/{entity}/{id}        -> soft delete       (status=false, matching legacy DELETE ?__cufl=status)
POST   /api/v1/masters/{entity}/upload      -> bulk upload       (legacy doctormaster/item/unitmaster/... upload endpoints exist)
```

Registry entries proposed for the W1 packages (name = path segment; filters = legacy `@CrudApi.allowedFilters` equivalents; search fields from FAUG list screens):

| Entity | Package | Allowed filters | Search fields (frontend list) | Notes |
|---|---|---|---|---|
| `unit` | MIG-011 | `id, status` (legacy) | `name` (`searchFld=name` in unitmaster screen) | write via `mdm/addUnitMaster` parity at gateway |
| `subunit` | MIG-011 | — (legacy none) | code/description | |
| `facility-location` | MIG-011 | `status, locationType, parentId` (new whitelist) | name/code | gateway maps wingmaster/floormaster/buildingmaster/roommaster/cabinmaster to this entity?type= |
| `geo-country`, `geo-state`, `geo-district`, `geo-city`, `geo-area` | MIG-011 | legacy: district `status, zoneid`; others none | name/code (census code) | district also filtered by zone |
| `doctor` | MIG-011 | legacy none; recommend `status, doctorTypeId, specializationId, isHod, isResidentDoctor` | first/last name, employeeNumber, registrationNumber (typeahead) | |
| `doctor-type` | MIG-011 | `status` (legacy) | description | |
| `specialization` | MIG-011 | `status` (legacy) | description | |
| `doctor-department`, `doctor-unit` | MIG-011 | `status` (new) | doctor/department id | |
| `item` | MIG-012 | legacy: `status, itemGroup, itemCategory, dispencingtype, pregclass, therclass, moleculename` | name/code (getItemmasterListAsset search) | list parity endpoint `itemmaster/list-asset` via gateway |
| `item-group` / `item-category` | MIG-012 | legacy: group `itemType, status`; category `status, itemGroupId` | description | category filtered by group |
| `molecule`, `brand`, `unit-of-measure`, `storage-type` | MIG-012 | `status` (legacy; uom also `defaultUOM`) | description/code | |
| `supplier` | MIG-012 | legacy: `status, isEmpanelled, mflag, unitId` | code/description | mflag=true drives item manufacturer picker |
| `store` | MIG-012 | `status` (legacy) | description (`searchFld=description`) | owned by item service, not masters — see §5.4 |
| `item-batch` | MIG-012 | — (legacy none) | batchCode, itemId | item service resource |
| `item-reorder-policy`, `item-supplier` | MIG-012 | — | store/item ids | item service resources |
| `item-stock-balance`, `item-stock-ledger` | MIG-012 | — | — | **NOT masters** — item service stock API: `GET /api/v1/items/{id}/stock`, `POST /api/v1/stock/movements` (single writer, D-1) |

---

## 7. Frontend cross-check (FAUG evidence: screens each clean table backs)

| Clean table | FAUG screen(s) | Schema file (public/) | Legacy CRUD endpoints |
|---|---|---|---|
| `unit` + `unit_config` | Masters > Hospital > Hospital (unitmaster), Hospital > …(unitConfiguration) | `modules/masters/hospital/schemas/unitmaster.json` | `api/rest/mdm/addUnitMaster`, `master/getUnitMaster/{id}`, `master/updateUnitMaster/{id}`, `unitmaster/{unitId}` (list w/ searchFld=name), upload |
| `facility_location` | Masters > Admission > Building/Floor/Unit(Wing)/Ward/Bed/Class/Block/Room; Hospital > Cabin | `modules/masters/admission/schemas/{buildingmaster,floormaster,wingmaster,roommaster,wardmaster}.json`, `modules/masters/hospital/schemas/cabinmaster.json` | `api/rest/{wingmaster,floormaster,buildingmaster,roommaster,cabinmaster}/*` |
| `geo_*` | Masters > Patient > Country/State/City/Area/District | `modules/masters/patient/schemas/{countrymaster,statemaster,citymaster,districtMaster,areamaster}.json` | `api/rest/{countrymaster,statemaster,citymaster,districtmaster,areamaster}/*` (cascading options city→district→state→country) |
| `doctor` | generic Masters CRUD (commons schema) + Add User flow | `modules/commons/schemas/doctormaster.json` | `api/rest/doctormaster` (POST/GET/PUT; DELETE ?__cufl=status), upload |
| `doctor_type` | Masters > Admission > Doctor Type | `modules/masters/admission/schemas/doctortypemaster.json` | `api/rest/doctortypemaster/*` |
| `specialization` | Masters > Billing > Group Master (+ doctor screen service-group picker) | `modules/masters/billing/schemas/specialization.json` | `api/rest/specialization/*` |
| `doctor_department`, `doctor_unit` | doctor master flow, allocation screens (`allocation:*`) | doctormaster.json (department picker), allocation templates | `doctordepartmentdetails`, `unitdoctordetails` (via doctor save) |
| `item_group` / `item_category` | Masters > Inventory > Item Group / Item Category | `modules/masters/inventory/schemas/{itemgroup,itemcategory}.json` | `api/rest/{itemgroup,itemcategorymaster}/*` |
| `molecule` | Masters > Inventory > Generic | `modules/masters/inventory/schemas/molecule.json` | `api/rest/molecule/*` |
| `brand` | Masters > Inventory > Brand | `modules/masters/inventory/schemas/brandmaster.json` | `api/rest/brandmaster/*` |
| `unit_of_measure` / `storage_type` | Masters > Inventory > Unit Of Measure / Storage Type | `modules/masters/inventory/schemas/{unitofmeasure,storeagetype}.json` | `api/rest/{unitofmeasure,storeagetype}/*` |
| `item` | Masters > Inventory > Item Master; Inventory module item list | `modules/masters/inventory/schemas/itemmaster.json` | `api/rest/item` (CRUD; legacy value="item"), `masterConfigService/getItemmasterListAsset`, `itemmaster/deleteItemmasterListById/{id}` |
| `store` | Masters > Inventory > Store Location; Store pickers | `modules/masters/inventory/schemas/store.json` | `api/rest/store` (CRUD), `inventoryStockList/getStoreMasterById/{id}`, list `searchFld=description` |
| `supplier` | Masters > Inventory > Supplier / Supplier Master | `modules/masters/inventory/schemas/{supplier,suppliermaster}.json` | `api/rest/{supplier,suppliermaster}/*` |
| `item_batch` | GRN/stock screens (batch entries), item expiry lists | (in inventory/pharmacy schemas; no standalone batch screen) | `api/rest/itembatchmaster/*` (called by GRN/PO flows), `itemmaster` batch block |
| `item_stock_balance` / `item_stock_ledger` | Inventory > stock location/ledger screens, pharmacy dispensing, CSSD kit issue | `modules/inventory/templates/stockLocationWrap.html`, `itemMasterListing.html` | `inventoryStockList/*` (226 endpoints), `expiryitem/*`, `CIMS_ADDITEMSTOCK`, `T_ITEMSTOCK` writers (see §9 D-1) |
| `item_reorder_policy` | Masters > Inventory > Reorder Level and Safety Stock | `modules/masters/inventory/schemas/reorderAndSafetyMaster.json` | `api/rest/reorderAndSafetyMaster/*` |
| `item_supplier` | Masters > Inventory > Supplier Item Mapping (`itemSupplierRate`) | inventory schemas | `api/rest/itemmaster/getItemSupplierList?id=&unitId=`, `api/rest/itemSupplier*` |
| `item_name` (M_ITEM_MST) | Masters > Inventory > Item Name | `modules/masters/inventory/schemas/item_mst.json` | `api/rest/item_mst/*` (entity name `item_mst`) |

---

## 8. Data-migration notes for MIG-001 backfill (live-data) + uncertain legacy semantics

**Mechanics (per IMP-DEC-003 / V2 seed):** for each migrated clean table, backfill with legacy ids preserved, then `setval('<table>_seq', GREATEST(MAX(id),1))`. Every clean column carries a `<- legacy column` provenance in its COMMENT (done throughout §4–§5); the MIG-006 mapping ledger records the full pair list.

1. **unit split:** M_UNITMASTER (1:1 parity table stays for gateway) → `unit` + `unit_config` + `unit.config_json`. ~95 source columns: ~30 map to unit, ~15 to unit_config, rest to config_json pending a value census (non-null counts per column) — the census decides which long-tail flags earn real columns. Blobs (LOGO) → BYTEA interim (D-8).
2. **geo:** `district_mst`/`city_mst` are census-seeded; preserve `district_code_2011`, `city_code`, ABDM codes, `zoneid` (district zone master link). unit_id is NULL for global rows (D-6). Legacy CITY on unitmaster is free text — normalize to city_id or keep text pending census (mapping decision D-6).
3. **doctor:** APPLICABLEUNITS comma-string → `doctor_unit` rows (explode at backfill; watch for unit ids not in M_UNITMASTER). `mobileno`/contact numbers (Long) → VARCHAR conversion must preserve digits (leading zeros). SPECIALIZATIONID/SUBSPECIALIZATIONID target is **uncertain** (frontend “Service Group” loads service-master; entity field named specialization) — resolve from legacy data at MIG-001 (D-2). PHOTO/DIGITALSIGNATURE blobs → BYTEA interim. `iam_user_id` stays NULL until MIG-004 maps legacy user ids.
4. **item:** M_ITEMMASTER ~110 columns → `item` (hot columns) + `item.attributes_json`. **BRANDNAME (string) vs BRANDID (Long)** must reconcile (same name may exist under multiple brand rows; prefer brand_id, keep brand_name as display fallback). ITEMCODE uniqueness: legacy not unique — the proposed unique (unit_id, item_code, item_name) will collide; dedupe/soft-merge policy required (decision D-4 note / census). CONVERSIONFACTOR String → NUMERIC(19,4) with invalid-string audit. HSNCODEID is String but references M_HSNCODEMASTER ids — verify values are ids not codes.
5. **store:** per-store SEQUENCE_NAME/prefix drive document numbering (indent/PO/GRN/Sale) — backfill must map to numbering-policy config, and sequence continuity (BR numbering) checked. `department`/`days` string columns — JSON-ish lists? census needed. `isOpIpStore` Long → SMALLINT enum map.
6. **stock split (T_ITEMSTOCK):** backfill derives `item_stock_balance` = last movement row per (store_id, item_id, batch_id) — but legacy also has `blockedStock/availableStock` variants and `departmentId` scoping; confirm whether balance is per (store,item,batch) or (store,item,batch,department) (D-1). All T_ITEMSTOCK rows → `item_stock_ledger` with TRANSACTIONTYPEID/OPERATIONTYPE mapped to movement_type/operation enums (extract DISTINCT values at MIG-001; CHECK constraints then finalized). M_OPENINGBALANCE rows become movement_type=OPENING_BALANCE ledger rows + balance tuples.
7. **M_ITEM_MST (item_name dictionary):** itemmaster.ITEM_MSTID references it — backfill the dictionary first, then set item.item_name_ref_id; legacy entity is 2-column code/description.
8. **Uncertain columns register (no clean decision yet — flagged for Agent 00 + MIG-001 census):**
   - `Unitmaster.LATIUTDE` (longitude/latitude strings; misspelled latitude), `ISCWFCOVERSHEET` (Short), `ISPARENT/VIRTUALID` semantics, `APIKEY/apiToken` secrets-in-DB, `MERCHANTCODE`, `HISTONOCATEGORY`, `CENTRALSTOREIDS` (comma list), eAushadhi/eRaktkosh external keys, `PATHIESTYPE_NEW`.
   - `Doctormaster.MARITALSTATUS` (Integer enum), `SPECIALIZATIONID/SUBSPECIALIZATIONID` target, `APPLICABLEUNITS`, `DOCTORNO` vs `EMPLOYEENUMBER` overlap.
   - `Itemmaster`: PUM/SUM semantics (packing/stocking unit?), `DISPENCINGTYPE`, `VATAPPLICABLEON`, gas-cylinder block, `LIFEASSET/LIFE/DEPRECIATION*` (asset leakage), `ISKIT/USEDFORKIT/USEDFORCSSD` interactions, `BLOOD_BAG_*` (blood-bank items inside item master), `ISNONSTOCKABLE` primitive default, `BATCHCODE/EXPIRYDATE` on the master row (default batch?), `ISSUETYPE`.
   - `Store.M_STORE` boolean column named `m_store` (self-flag semantics unknown), `department`/`days` strings, `isOpIpStore`.
   - `Itemcompany` (M_ITEMCOMPANY) — no active frontend; item master manufacturer picker reads suppliers with mflag; **likely dead/legacy** — confirm via reference census before designing a target (kept out of scope this wave).
   - `M_UNITOFSTORAGE` vs `M_STOREAGETYPE` overlap (D-5).
   - `ItemStock` legacy `TRANSACTIONTYPEID` (Short) value map, `DEPARTMENTID` scoping, `ISREORDERINDENT` semantics.

---

## 9. Decision-needed list (for Agent 00 / ratification; per §55 — not resolved here)

| # | Decision | Context / evidence | Recommendation (for validation, not final) |
|---|---|---|---|
| **D-1** | **Stock single-writer (§55)** | Legacy writers of T_ITEMSTOCK: InventoryServiceImpl (44 call sites), ExpiryItemServiceImpl (19), ItemStockServiceImpl (13), CSSD (7), Medication (4), Pharmacy (3) + proc `CIMS_UPDATE_CONSIGNEDGRN_ITEMSTOCK` (MIG-012 backlog). Pharmacy dual-write path `PharmacyServiceImpl.java:2140` must be retired. | Item service owns `item_stock_balance` + `item_stock_ledger`; all mutations via one stock API (transaction: lock balance → write ledger → emit `StockAdjusted`/`InventoryUpdated`); consumers (pharmacy/CSSD/medication/expiry/OT) read via API/events. Confirm balance granularity incl. `department_id`. |
| **D-2** | **Doctor identity vs user identity (MST vs IAM)** | `POSTGRESQL-MIGRATION §1.1` conflict: IAM “owns M_STAFFMASTER/M_DOCTORMASTER profiles” vs Reference owns all M_*. MIG-004 IAM is blocked/later; doctors may/may not be login users. | W1: keep `doctor` (clinical) in `ihms_master` with logical `iam_user_id` (unique); IAM owns users/roles/grants in `ihms_iam` (MIG-004). If IAM later owns identity slice, move identity columns only. Also confirm SPECIALIZATIONID target (service master vs specialization master). |
| **D-3** | **Location hierarchy & ward/bed/OT ownership** | Legacy flat code/description location masters; ward/bed operational state written by ADT (`POSTGRESQL-MIGRATION §1.1` recommends M_BEDMASTER → ihms_adt); OT theatre domain-owned by OT (S11). | Wave 1: single `facility_location` hierarchy for building/floor/wing/room/cabin (WARD/OT_THEATRE enum reserved only); ward/bed/OT tables deferred to their owning services; store.wing_id/floor_id stay logical refs (no cross-schema FK). |
| **D-4** | **Item master placement** | itemMaster cache key owned by Reference; tables recommended Inventory-owned (`10 §2.1` row 10); §56 forbids cross-schema FKs between item and stock. | Whole item vertical (item + classif. lookups + supplier + store + batch + stock) in `ihms_item`; reference-service keeps cache-key contract + `MasterDataChanged`. Confirm ITEMCODE dedupe policy. |
| **D-5** | **UoM / storage-type legacy pair** | M_UNITOFSTORAGE has no frontend screen; M_STOREAGETYPE is active (“Storage Type”). They look semantically duplicate. | Single `ihms_item.storage_type` with `kind` provenance discriminator; collapse at backfill once usage census done (or keep one lookup if Itemmaster.STOREAGETYPE is the only consumer). |
| **D-6** | **Geo masters scope & normalization** | Legacy geo masters extend MBase (UnitID exists) but are census-seeded global lists; unitmaster stores CITY/DISTRICT as free text while also having DISTRICTID. | `geo_*` are global reference: `unit_id NULL`, logical nationality/zone refs; normalize unit.address → city_id/district_id (keep legacy text backup in migration staging; DDL keeps address text columns too). |
| **D-7** | **Blob strategy (logo/photo/signature/document)** | Legacy stores blobs in DB (unit LOGO, doctor PHOTO/DIGITALSIGNATURE, item DOCUMENT); file management migrates to MinIO/object storage (MIG-021). | Interim: BYTEA columns (migration path); new writes/wave-2+ switch to object storage with URL reference; confirm 12 `@Lob` entities list (POSTGRESQL-MIGRATION §2 row 5). |
| **D-8** | **Per-store numbering (SEQUENCE_NAME/prefix)** | Store rows carry DB sequence names for document numbering; BR numbering requires continuity (BR-BILL-001 etc.). | Normalize to `numbering_policy` config (prefix + seq + format) in item service; sequence reseed participates in MIG-001 so no duplicate numbers post-backfill. |

No repository files were modified; this report is the only artifact produced (`outputs/design/AGT-02-W1-masters-schema.md`).
