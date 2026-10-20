-- ============================================================================
-- V1 — ihms_item schema (Item/Inventory service) — OWNERSHIP TRANSFER 2026-08-17
-- This DDL was shipped initially as reference-service V6 (DDL contract,
-- IMP-DEC-025). MIG-012 kickoff transfers ownership to the item service
-- (ONE-TABLE-ONE-OWNER §56): reference-service no longer carries it.
-- Dev-DB migration note: the objects may already exist from the transferred V6
-- run, hence IF NOT EXISTS everywhere (idempotent); fresh environments create
-- them normally. All migrated tables keep explicit sequences (IMP-DEC-003);
-- the two NEW stock tables (balance/ledger) use IDENTITY.
-- ============================================================================

-- ============================================================================
-- V6 — MIG-012 clean target schema `ihms_item` (Item/Inventory service)
-- per AGT-02 design (§5; IMP-DEC-021/D-1/D-4 ratified architecturally).
-- ONE-TABLE-ONE-OWNER (§56): ALL tables here belong to the Item/Inventory
-- service (future repository). reference-service (ihms_master) never writes
-- these tables — it may only expose logical references. The item service's own
-- Flyway (future) applies this schema; it is shipped here under the masters
-- repo as the ratified DDL contract (team-authored schema, IMP-DEC-018).
-- Single-writer stock foundation (D-1): item_stock_balance + item_stock_ledger
-- are mutated ONLY by the item service stock API (balance row locked, ledger
-- appended, event emitted) — legacy dual writers retired by W5.
-- IDENTITY used ONLY on the two NEW tables (balance/ledger); all migrated
-- tables get explicit sequences (IMP-DEC-003).
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS ihms_item;

-- pg_trgm enables the item_name typeahead GIN index (search parity with the
-- legacy item-master list typeahead). WITH SCHEMA public: Flyway's connection
-- sets search_path=ihms_master, which would otherwise install the extension
-- where the (ihms_item, public) search_path cannot see gin_trgm_ops.
CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;

SET search_path TO ihms_item, public;

-- ---------------------------------------------------------------------------
-- 5.1 Item classification lookups
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_item.item_group (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(64) NOT NULL, description VARCHAR(255) NOT NULL,
    ledger_name VARCHAR(64),
    prefix VARCHAR(16),
    account_selection VARCHAR(64),
    item_type VARCHAR(32),
    CONSTRAINT pk_item_group PRIMARY KEY (id),
    CONSTRAINT uq_item_group_unit_code UNIQUE (unit_id, code)
);
CREATE INDEX IF NOT EXISTS ix_item_group_unit_status ON ihms_item.item_group (unit_id, status);

CREATE TABLE IF NOT EXISTS ihms_item.item_category (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    item_group_id BIGINT NOT NULL REFERENCES ihms_item.item_group (id),
    code VARCHAR(64) NOT NULL, description VARCHAR(255) NOT NULL,
    ledger_name VARCHAR(64),
    service_id BIGINT,
    maintenance_default_id BIGINT,
    expiry_days BIGINT,
    CONSTRAINT pk_item_category PRIMARY KEY (id),
    CONSTRAINT uq_item_category_group_code UNIQUE (item_group_id, code)
);
CREATE INDEX IF NOT EXISTS ix_item_category_group ON ihms_item.item_category (item_group_id);

CREATE TABLE IF NOT EXISTS ihms_item.molecule (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(64) NOT NULL, description VARCHAR(255) NOT NULL,
    ingredient_list VARCHAR(1000),
    CONSTRAINT pk_molecule PRIMARY KEY (id),
    CONSTRAINT uq_molecule_unit_code UNIQUE (unit_id, code)
);

CREATE TABLE IF NOT EXISTS ihms_item.brand (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    item_category_id BIGINT REFERENCES ihms_item.item_category (id),
    description VARCHAR(255) NOT NULL,
    CONSTRAINT pk_brand PRIMARY KEY (id),
    CONSTRAINT uq_brand_unit_description UNIQUE (unit_id, description)
);

CREATE TABLE IF NOT EXISTS ihms_item.unit_of_measure (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(32) NOT NULL, description VARCHAR(120) NOT NULL,
    quantity BIGINT,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_scientific BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_unit_of_measure PRIMARY KEY (id),
    CONSTRAINT uq_unit_of_measure_unit_code UNIQUE (unit_id, code)
);

CREATE TABLE IF NOT EXISTS ihms_item.storage_type (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    kind VARCHAR(16) NOT NULL DEFAULT 'STORE_AGE' CHECK (kind IN ('STORE_AGE','UNIT_OF_STORAGE')),
    code VARCHAR(64) NOT NULL, description VARCHAR(255) NOT NULL,
    CONSTRAINT pk_storage_type PRIMARY KEY (id),
    CONSTRAINT uq_storage_type_unit_kind_code UNIQUE (unit_id, kind, code)
);
COMMENT ON TABLE ihms_item.storage_type IS 'Merges M_STOREAGETYPE + M_UNITOFSTORAGE; kind preserves provenance (D-5).';

-- ---------------------------------------------------------------------------
-- 5.2 supplier
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_item.supplier (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(64) NOT NULL, description VARCHAR(255) NOT NULL,
    address_line1 VARCHAR(255), address_line2 VARCHAR(255), address_line3 VARCHAR(255),
    city VARCHAR(120), state VARCHAR(120), country VARCHAR(120), pincode VARCHAR(16),
    contact_person1_name VARCHAR(120), contact_person1_mobile VARCHAR(32), contact_person1_email VARCHAR(255),
    contact_person2_name VARCHAR(120), contact_person2_mobile VARCHAR(32), contact_person2_email VARCHAR(255),
    phone_no VARCHAR(32), fax VARCHAR(32),
    mode_of_payment BIGINT, tax_nature BIGINT, term_of_payment BIGINT, currency BIGINT,
    mst_number VARCHAR(64), vat_number VARCHAR(64), cst_number VARCHAR(64),
    drug_licence_number VARCHAR(64), service_tax_number VARCHAR(64), pan_number VARCHAR(32),
    is_manufacturer BOOLEAN NOT NULL DEFAULT FALSE,
    is_implants_supplier BOOLEAN NOT NULL DEFAULT FALSE,
    is_inhouse_supplier BOOLEAN NOT NULL DEFAULT FALSE,
    is_service_agent BOOLEAN NOT NULL DEFAULT FALSE,
    is_empanelled BOOLEAN NOT NULL DEFAULT FALSE,
    empanelled_by BIGINT, supplier_unit_id BIGINT,
    start_date DATE, end_date DATE,
    supplier_master_id BIGINT,
    terms_and_condition_id VARCHAR(200),
    CONSTRAINT pk_supplier PRIMARY KEY (id),
    CONSTRAINT uq_supplier_unit_code UNIQUE (unit_id, code)
);
CREATE INDEX IF NOT EXISTS ix_supplier_unit_status ON ihms_item.supplier (unit_id, status);
CREATE INDEX IF NOT EXISTS ix_supplier_mfg_flag ON ihms_item.supplier (unit_id, is_manufacturer) WHERE is_manufacturer;

-- ---------------------------------------------------------------------------
-- 5.4 store — created BEFORE item_reorder_policy/item_supplier (FK execution order)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_item.store (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(64) NOT NULL, description VARCHAR(255) NOT NULL,
    is_main_store BOOLEAN NOT NULL DEFAULT FALSE,
    is_central_store BOOLEAN NOT NULL DEFAULT FALSE,
    is_pharmacy_store BOOLEAN NOT NULL DEFAULT FALSE,
    is_cssd_store BOOLEAN NOT NULL DEFAULT FALSE,
    is_implant_store BOOLEAN NOT NULL DEFAULT FALSE,
    is_asset_store BOOLEAN NOT NULL DEFAULT FALSE,
    is_sub_store BOOLEAN NOT NULL DEFAULT FALSE,
    is_patient_requisition BOOLEAN NOT NULL DEFAULT FALSE,
    item_group_id BIGINT REFERENCES ihms_item.item_group (id),
    opening_balance BOOLEAN NOT NULL DEFAULT FALSE,
    indent BOOLEAN NOT NULL DEFAULT FALSE,
    item_return BOOLEAN NOT NULL DEFAULT FALSE,
    goods_received_note BOOLEAN NOT NULL DEFAULT FALSE,
    grn_return BOOLEAN NOT NULL DEFAULT FALSE,
    items_sale BOOLEAN NOT NULL DEFAULT FALSE,
    items_sale_return BOOLEAN NOT NULL DEFAULT FALSE,
    expiry_item_return BOOLEAN NOT NULL DEFAULT FALSE,
    receive_issue_return BOOLEAN NOT NULL DEFAULT FALSE,
    is_op_ip_store SMALLINT,
    wing_id BIGINT, floor_id BIGINT,
    department VARCHAR(255),
    days VARCHAR(255),
    prefix VARCHAR(16),
    sequence_name VARCHAR(64),
    eaushadhi_store_id VARCHAR(64), eaushadhi_parent_store_id VARCHAR(64),
    gst_no VARCHAR(32),
    pharmacy_license_no VARCHAR(64),
    blood_store_license_no VARCHAR(64),
    nhm_blood_bank_code BIGINT,
    address_line1 VARCHAR(255), phone_no VARCHAR(32), email VARCHAR(255),
    CONSTRAINT pk_store PRIMARY KEY (id),
    CONSTRAINT uq_store_unit_code UNIQUE (unit_id, code)
);
CREATE INDEX IF NOT EXISTS ix_store_unit_status ON ihms_item.store (unit_id, status);

-- ---------------------------------------------------------------------------
-- 5.3 item — clean split of M_ITEMMASTER (hot query columns typed; long tail in attributes_json)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_item.item (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    item_code VARCHAR(128),
    item_name VARCHAR(255) NOT NULL,
    item_name_hindi VARCHAR(255),
    item_name_ref_id BIGINT,
    brand_name VARCHAR(255),
    brand_id BIGINT REFERENCES ihms_item.brand (id),
    strength VARCHAR(64),
    strength_uom_id BIGINT REFERENCES ihms_item.unit_of_measure (id),
    molecule_id BIGINT REFERENCES ihms_item.molecule (id),
    item_group_id BIGINT REFERENCES ihms_item.item_group (id),
    item_category_id BIGINT REFERENCES ihms_item.item_category (id),
    dispensing_type_id BIGINT,
    storage_type_id BIGINT REFERENCES ihms_item.storage_type (id),
    pregnancy_class_id BIGINT,
    therapeutic_class_id BIGINT,
    route_id BIGINT,
    code_type_id BIGINT,
    manufacturer_id BIGINT REFERENCES ihms_item.supplier (id),
    marketer_id BIGINT REFERENCES ihms_item.supplier (id),
    purchase_rate NUMERIC(19,2),
    mrp NUMERIC(19,2),
    sale_price NUMERIC(19,2),
    vat_percent NUMERIC(19,2),
    vat_applicable_on SMALLINT,
    inclusive_of_tax BOOLEAN NOT NULL DEFAULT FALSE,
    discount_on_sale NUMERIC(19,2),
    guide_cost NUMERIC(19,2),
    packing_uom_id BIGINT,
    stocking_uom_id BIGINT,
    conversion_factor NUMERIC(19,4),
    batches_required BOOLEAN NOT NULL DEFAULT FALSE,
    reorder_qty INTEGER,
    avg_daily_demand BIGINT,
    safety_stock BIGINT,
    is_reorder BOOLEAN NOT NULL DEFAULT FALSE,
    otc BOOLEAN NOT NULL DEFAULT FALSE,
    nursing_orderable BOOLEAN NOT NULL DEFAULT TRUE,
    doctor_orderable BOOLEAN NOT NULL DEFAULT TRUE,
    used_for_kit BOOLEAN NOT NULL DEFAULT FALSE,
    used_for_cssd BOOLEAN NOT NULL DEFAULT FALSE,
    decimal_consumption BOOLEAN NOT NULL DEFAULT FALSE,
    is_reusable BOOLEAN NOT NULL DEFAULT FALSE,
    is_kit BOOLEAN NOT NULL DEFAULT FALSE,
    partial_dispensing BOOLEAN NOT NULL DEFAULT FALSE,
    is_compound BOOLEAN NOT NULL DEFAULT FALSE,
    is_custom_order BOOLEAN NOT NULL DEFAULT FALSE,
    is_non_stockable BOOLEAN NOT NULL DEFAULT FALSE,
    controlled_substance BOOLEAN NOT NULL DEFAULT FALSE,
    is_life_saving BOOLEAN NOT NULL DEFAULT FALSE,
    is_high_risk BOOLEAN NOT NULL DEFAULT FALSE,
    is_high_cost BOOLEAN NOT NULL DEFAULT FALSE,
    is_asset BOOLEAN NOT NULL DEFAULT FALSE,
    ved_class SMALLINT,
    inventory_code VARCHAR(128),
    unique_country_drug_code VARCHAR(64),
    hsn_code_id VARCHAR(32),
    snomed_code VARCHAR(64), snomed_display VARCHAR(255),
    edl_number VARCHAR(64),
    order_ien VARCHAR(64),
    eaushadhi_item_id BIGINT,
    document_name VARCHAR(255), source_url VARCHAR(1000), document BYTEA,
    suggestions VARCHAR(500), remarks VARCHAR(500),
    attributes_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT pk_item PRIMARY KEY (id),
    CONSTRAINT uq_item_unit_code_name UNIQUE (unit_id, item_code, item_name)
);
CREATE INDEX IF NOT EXISTS ix_item_unit_status ON ihms_item.item (unit_id, status);
CREATE INDEX IF NOT EXISTS ix_item_group ON ihms_item.item (item_group_id);
CREATE INDEX IF NOT EXISTS ix_item_category ON ihms_item.item (item_category_id);
CREATE INDEX IF NOT EXISTS ix_item_molecule ON ihms_item.item (molecule_id);
CREATE INDEX IF NOT EXISTS ix_item_name_trgm ON ihms_item.item USING gin (item_name gin_trgm_ops);
COMMENT ON TABLE ihms_item.item IS 'Clean target of M_ITEMMASTER. Long-tail flags (gas/AMC/blood-bag/approvals) stay in attributes_json until MIG-001 value census (D-4).';

-- item reorder policy per store (M_REORDERANDSAFETYMASTER)
CREATE TABLE IF NOT EXISTS ihms_item.item_reorder_policy (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    store_id BIGINT NOT NULL REFERENCES ihms_item.store (id),
    item_id BIGINT NOT NULL REFERENCES ihms_item.item (id),
    reorder_level NUMERIC(19,4),
    safety_stock NUMERIC(19,4),
    reorder_value NUMERIC(19,2),
    is_automatic BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_item_reorder_policy PRIMARY KEY (id),
    CONSTRAINT uq_item_reorder_policy UNIQUE (store_id, item_id)
);

-- item <-> supplier availability (T_ITEMSUPPLIER)
CREATE TABLE IF NOT EXISTS ihms_item.item_supplier (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    item_id BIGINT NOT NULL REFERENCES ihms_item.item (id),
    supplier_id BIGINT NOT NULL REFERENCES ihms_item.supplier (id),
    item_category_id BIGINT,
    CONSTRAINT pk_item_supplier PRIMARY KEY (id),
    CONSTRAINT uq_item_supplier UNIQUE (item_id, supplier_id)
);

-- ---------------------------------------------------------------------------
-- 5.5 Stock foundation — single-writer (D-1): balance (current state) + ledger (append-only)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_item.item_batch (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    item_id BIGINT NOT NULL REFERENCES ihms_item.item (id),
    batch_code VARCHAR(64) NOT NULL,
    expiry_date DATE,
    manufacturing_date DATE,
    quantity NUMERIC(19,4),
    conversion_factor NUMERIC(19,4),
    purchase_rate NUMERIC(19,2), mrp NUMERIC(19,2),
    vat_percentage NUMERIC(19,2), vat_amount NUMERIC(19,2),
    discount_on_sale NUMERIC(19,2),
    landed_rate NUMERIC(19,2),
    is_freeze_recall BOOLEAN NOT NULL DEFAULT FALSE,
    freeze_recall_remark VARCHAR(500),
    is_ext_store BOOLEAN NOT NULL DEFAULT FALSE,
    is_expiry_date_updated BOOLEAN NOT NULL DEFAULT FALSE,
    previous_expiry_date DATE,
    expiry_update_document VARCHAR(500),
    remarks VARCHAR(500), updated_windows_login VARCHAR(255),
    CONSTRAINT pk_item_batch PRIMARY KEY (id),
    CONSTRAINT uq_item_batch UNIQUE (item_id, batch_code)
);
CREATE INDEX IF NOT EXISTS ix_item_batch_expiry ON ihms_item.item_batch (expiry_date) WHERE expiry_date IS NOT NULL;

-- NEW table (IDENTITY allowed — IMP-DEC-003). Current stock state per (store,item,batch).
CREATE TABLE IF NOT EXISTS ihms_item.item_stock_balance (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES ihms_item.store (id),
    item_id BIGINT NOT NULL REFERENCES ihms_item.item (id),
    batch_id BIGINT REFERENCES ihms_item.item_batch (id),
    department_id BIGINT,
    qty_in_hand NUMERIC(19,4) NOT NULL DEFAULT 0,
    blocked_qty NUMERIC(19,4) NOT NULL DEFAULT 0,
    available_qty NUMERIC(19,4) NOT NULL DEFAULT 0,
    damaged_qty NUMERIC(19,4) NOT NULL DEFAULT 0,
    expired_qty NUMERIC(19,4) NOT NULL DEFAULT 0,
    avg_daily_demand NUMERIC(19,4), safety_stock NUMERIC(19,4),
    is_reorder BOOLEAN NOT NULL DEFAULT FALSE,
    is_reorder_indent BOOLEAN NOT NULL DEFAULT FALSE,
    last_ledger_id BIGINT,
    version BIGINT NOT NULL DEFAULT 0,
    unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_item_stock_balance UNIQUE (store_id, item_id, batch_id)
);
COMMENT ON TABLE ihms_item.item_stock_balance IS 'NEW: current stock per (store,item,batch). Single writer D-1 — only the item service stock API mutates (SELECT FOR UPDATE + ledger + event). MIG-001 backfill derives from last T_ITEMSTOCK movement per key.';
CREATE INDEX IF NOT EXISTS ix_item_stock_balance_item ON ihms_item.item_stock_balance (item_id, store_id);
CREATE INDEX IF NOT EXISTS ix_item_stock_balance_batch ON ihms_item.item_stock_balance (batch_id) WHERE batch_id IS NOT NULL;

-- NEW table (IDENTITY). Append-only movement ledger, same tx as balance update.
CREATE TABLE IF NOT EXISTS ihms_item.item_stock_ledger (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES ihms_item.store (id),
    item_id BIGINT NOT NULL REFERENCES ihms_item.item (id),
    batch_id BIGINT REFERENCES ihms_item.item_batch (id),
    department_id BIGINT,
    movement_type SMALLINT NOT NULL CHECK (movement_type BETWEEN 1 AND 30),
    operation SMALLINT NOT NULL CHECK (operation IN (1,2)),
    quantity NUMERIC(19,4) NOT NULL,
    prev_balance NUMERIC(19,4) NOT NULL,
    new_balance NUMERIC(19,4) NOT NULL,
    ref_doc_type VARCHAR(32),
    ref_doc_id BIGINT,
    grn_id BIGINT,
    item_supplier_id BIGINT REFERENCES ihms_item.item_supplier (id),
    remarks VARCHAR(500),
    is_cancel BOOLEAN NOT NULL DEFAULT FALSE,
    unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS ix_item_stock_ledger_item ON ihms_item.item_stock_ledger (item_id, store_id, added_datetime DESC);
CREATE INDEX IF NOT EXISTS ix_item_stock_ledger_ref ON ihms_item.item_stock_ledger (ref_doc_type, ref_doc_id);
COMMENT ON TABLE ihms_item.item_stock_ledger IS 'NEW: append-only movements derived from legacy T_ITEMSTOCK. movement_type map (1..30) finalized at MIG-001.';

-- Sequences for migrated tables (IMP-DEC-003); balance/ledger use IDENTITY.
CREATE SEQUENCE IF NOT EXISTS ihms_item.item_group_seq       START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_item.item_category_seq    START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_item.molecule_seq         START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_item.brand_seq            START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_item.unit_of_measure_seq  START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_item.storage_type_seq     START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_item.supplier_seq         START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_item.item_seq             START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_item.store_seq            START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_item.item_batch_seq       START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_item.item_reorder_policy_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_item.item_supplier_seq    START WITH 1;

-- D-1 single-writer: allow at most ONE non-batch balance row per (store,item)
-- (PG UNIQUE treats NULLs as distinct, so the composite unique above would
-- permit multiple batch_id-NULL rows).
CREATE UNIQUE INDEX IF NOT EXISTS uq_item_stock_balance_no_batch
    ON ihms_item.item_stock_balance (store_id, item_id) WHERE batch_id IS NULL;

-- ---------------------------------------------------------------------------
-- Transactional outbox (IMP-DEC-004: outbox lives in each domain schema).
-- Same envelope contract as reference-service; consumers dedupe by event_id.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_item.outbox (
    id               BIGSERIAL    PRIMARY KEY,
    event_id         UUID         NOT NULL,
    event_type       VARCHAR(100) NOT NULL,
    version          VARCHAR(16)  NOT NULL DEFAULT 'v1',
    aggregate_type   VARCHAR(100) NOT NULL,
    aggregate_id     BIGINT       NOT NULL,
    partition_key    VARCHAR(100) NOT NULL,
    payload          JSONB        NOT NULL,
    idempotency_key  VARCHAR(255),
    producer_service VARCHAR(100) NOT NULL,
    occurred_at      TIMESTAMP(3) NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempts         INTEGER      NOT NULL DEFAULT 0,
    published_at     TIMESTAMP(3),
    created_at       TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_outbox_event_id UNIQUE (event_id)
);
COMMENT ON TABLE ihms_item.outbox IS 'Transactional outbox: business tx writes event here atomically; KafkaOutboxRelay publishes and marks PUBLISHED. Status: PENDING/PUBLISHED/FAILED/DLQ.';
CREATE INDEX IF NOT EXISTS idx_outbox_dispatch ON ihms_item.outbox (status, created_at) WHERE status = 'PENDING';
