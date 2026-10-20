-- ============================================================================
-- V2 — procurement slice (WP-2): GRN + Indent in schema ihms_item.
-- Mirrors the V1 table conventions exactly:
--   * shared MBase-style audit block columns on every table;
--   * explicit per-table sequences for migrated legacy tables (IMP-DEC-003) —
--     t_grn/t_grn_detail derive from legacy T_GRN/T_GRNDETAIL and
--     t_indent/t_indent_detail from legacy T_INDENT/T_INDENTDETAIL, hence they
--     keep the explicit-sequence convention rather than IDENTITY;
--   * clean snake_case names (legacy T_ prefix dropped, same as T_ITEMSTOCK ->
--     item_stock_balance in V1);
--   * supplier master reuses ihms_item.supplier from V1 (ONE-TABLE-ONE-OWNER
--     SS56: there is exactly ONE supplier master table — no duplicate
--     "t_supplier_master" is created).
-- Status codes follow SMALLINT dictionary conventions:
--   grn.status / indent.status: 1=PENDING 2=APPROVED 3=REJECTED 4=CANCELLED.
-- ============================================================================

SET search_path TO ihms_item, public;

CREATE TABLE IF NOT EXISTS ihms_item.t_grn (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    grn_no VARCHAR(64) NOT NULL,
    store_id BIGINT NOT NULL REFERENCES ihms_item.store (id),
    supplier_id BIGINT REFERENCES ihms_item.supplier (id),
    invoice_no VARCHAR(64),
    invoice_date DATE,
    received_date DATE,
    remarks VARCHAR(500),
    doc_status SMALLINT NOT NULL DEFAULT 1 CHECK (doc_status BETWEEN 1 AND 4),
    approved_by BIGINT,
    approved_datetime TIMESTAMP(3),
    CONSTRAINT pk_t_grn PRIMARY KEY (id),
    CONSTRAINT uq_t_grn_no UNIQUE (grn_no)
);
COMMENT ON COLUMN ihms_item.t_grn.doc_status IS '1=PENDING 2=APPROVED 3=REJECTED 4=CANCELLED (status column keeps the generic audit meaning).';
CREATE INDEX IF NOT EXISTS ix_t_grn_store ON ihms_item.t_grn (store_id, added_datetime DESC);
CREATE INDEX IF NOT EXISTS ix_t_grn_supplier ON ihms_item.t_grn (supplier_id);

CREATE TABLE IF NOT EXISTS ihms_item.t_grn_detail (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    grn_id BIGINT NOT NULL REFERENCES ihms_item.t_grn (id),
    item_id BIGINT NOT NULL REFERENCES ihms_item.item (id),
    batch_id BIGINT REFERENCES ihms_item.item_batch (id),
    quantity NUMERIC(19,4) NOT NULL,
    purchase_rate NUMERIC(19,2),
    mrp NUMERIC(19,2),
    remarks VARCHAR(500),
    CONSTRAINT pk_t_grn_detail PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_t_grn_detail_grn ON ihms_item.t_grn_detail (grn_id);
CREATE INDEX IF NOT EXISTS ix_t_grn_detail_item ON ihms_item.t_grn_detail (item_id);

CREATE TABLE IF NOT EXISTS ihms_item.t_indent (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    indent_no VARCHAR(64) NOT NULL,
    store_id BIGINT NOT NULL REFERENCES ihms_item.store (id),
    department_id BIGINT,
    remarks VARCHAR(500),
    doc_status SMALLINT NOT NULL DEFAULT 1 CHECK (doc_status BETWEEN 1 AND 4),
    approved_by BIGINT,
    approved_datetime TIMESTAMP(3),
    CONSTRAINT pk_t_indent PRIMARY KEY (id),
    CONSTRAINT uq_t_indent_no UNIQUE (indent_no)
);
COMMENT ON COLUMN ihms_item.t_indent.doc_status IS '1=PENDING 2=APPROVED 3=REJECTED 4=CANCELLED (status column keeps the generic audit meaning).';
CREATE INDEX IF NOT EXISTS ix_t_indent_store_status ON ihms_item.t_indent (store_id, doc_status);

CREATE TABLE IF NOT EXISTS ihms_item.t_indent_detail (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    indent_id BIGINT NOT NULL REFERENCES ihms_item.t_indent (id),
    item_id BIGINT NOT NULL REFERENCES ihms_item.item (id),
    quantity NUMERIC(19,4) NOT NULL,
    issued_qty NUMERIC(19,4) NOT NULL DEFAULT 0,
    remarks VARCHAR(500),
    CONSTRAINT pk_t_indent_detail PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_t_indent_detail_indent ON ihms_item.t_indent_detail (indent_id);
CREATE INDEX IF NOT EXISTS ix_t_indent_detail_item ON ihms_item.t_indent_detail (item_id);

-- Explicit sequences (IMP-DEC-003 — migrated legacy tables).
CREATE SEQUENCE IF NOT EXISTS ihms_item.t_grn_seq            START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_item.t_grn_detail_seq     START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_item.t_indent_seq         START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_item.t_indent_detail_seq  START WITH 1;
