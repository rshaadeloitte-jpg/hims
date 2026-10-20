-- ============================================================================
-- V3 — M13 remaining gaps: expiry/damage returns, inter-store transfer,
--      item enquiry support (read-side), reorder alerts.
-- Ported from legacy ExpiryItemServiceImpl (CIMS_ADDITEMEXPIRYRETURN +
-- CIMS_ADDITEMEXPIRYRETURNDETAILS, GRNRETURN transaction type) and
-- InventoryServiceImpl stock transfer semantics.
-- ============================================================================

SET search_path TO ihms_item, public;

-- ---------------------------------------------------------------------------
-- t_expiry_return — header (legacy CIMS_ADDITEMEXPIRYRETURN)
-- return_type: 1=EXPIRED 2=DAMAGED 3=BOTH
-- doc_status: 1=PENDING 2=APPROVED 3=REJECTED 4=CANCELLED 5=RETURNED_TO_SUPPLIER
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_item.t_expiry_return (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    return_no VARCHAR(64) NOT NULL,
    store_id BIGINT NOT NULL REFERENCES ihms_item.store (id),
    supplier_id BIGINT REFERENCES ihms_item.supplier (id),
    return_type SMALLINT NOT NULL DEFAULT 1 CHECK (return_type IN (1,2,3)),
    doc_status SMALLINT NOT NULL DEFAULT 1 CHECK (doc_status BETWEEN 1 AND 5),
    remarks VARCHAR(500),
    approved_by BIGINT,
    approved_datetime TIMESTAMP(3),
    returned_at TIMESTAMP(3),
    credit_note_no VARCHAR(64),
    credit_note_amount NUMERIC(19,2),
    CONSTRAINT pk_t_expiry_return PRIMARY KEY (id),
    CONSTRAINT uq_t_expiry_return_no UNIQUE (return_no)
);
COMMENT ON COLUMN ihms_item.t_expiry_return.return_type IS '1=EXPIRED 2=DAMAGED 3=BOTH (legacy ExpiryItemReturnDto expiryDamageFlag).';
COMMENT ON COLUMN ihms_item.t_expiry_return.doc_status IS '1=PENDING 2=APPROVED 3=REJECTED 4=CANCELLED 5=RETURNED_TO_SUPPLIER.';
CREATE INDEX IF NOT EXISTS ix_t_expiry_return_store ON ihms_item.t_expiry_return (store_id, doc_status);
CREATE INDEX IF NOT EXISTS ix_t_expiry_return_supplier ON ihms_item.t_expiry_return (supplier_id);

-- ---------------------------------------------------------------------------
-- t_expiry_return_detail — one line (legacy CIMS_ADDITEMEXPIRYRETURNDETAILS)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_item.t_expiry_return_detail (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    expiry_return_id BIGINT NOT NULL REFERENCES ihms_item.t_expiry_return (id),
    item_id BIGINT NOT NULL REFERENCES ihms_item.item (id),
    batch_id BIGINT REFERENCES ihms_item.item_batch (id),
    grn_id BIGINT,
    quantity NUMERIC(19,4) NOT NULL CHECK (quantity > 0),
    purchase_rate NUMERIC(19,2),
    return_reason VARCHAR(255),
    CONSTRAINT pk_t_expiry_return_detail PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_t_expiry_return_detail_hdr ON ihms_item.t_expiry_return_detail (expiry_return_id);
CREATE INDEX IF NOT EXISTS ix_t_expiry_return_detail_item ON ihms_item.t_expiry_return_detail (item_id);

-- ---------------------------------------------------------------------------
-- t_stock_transfer — inter-store transfer (legacy CIMS stock transfer via
-- movementType 6=TRANSFER_IN / 7=TRANSFER_OUT pair)
-- doc_status: 1=REQUESTED 2=DISPATCHED 3=RECEIVED 4=CANCELLED
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_item.t_stock_transfer (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    transfer_no VARCHAR(64) NOT NULL,
    from_store_id BIGINT NOT NULL REFERENCES ihms_item.store (id),
    to_store_id BIGINT NOT NULL REFERENCES ihms_item.store (id),
    doc_status SMALLINT NOT NULL DEFAULT 1 CHECK (doc_status BETWEEN 1 AND 4),
    remarks VARCHAR(500),
    dispatched_by BIGINT,
    dispatched_at TIMESTAMP(3),
    received_by BIGINT,
    received_at TIMESTAMP(3),
    CONSTRAINT pk_t_stock_transfer PRIMARY KEY (id),
    CONSTRAINT uq_t_stock_transfer_no UNIQUE (transfer_no),
    CONSTRAINT ck_t_stock_transfer_stores CHECK (from_store_id <> to_store_id)
);
CREATE INDEX IF NOT EXISTS ix_t_stock_transfer_from ON ihms_item.t_stock_transfer (from_store_id, doc_status);
CREATE INDEX IF NOT EXISTS ix_t_stock_transfer_to ON ihms_item.t_stock_transfer (to_store_id, doc_status);

CREATE TABLE IF NOT EXISTS ihms_item.t_stock_transfer_detail (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    transfer_id BIGINT NOT NULL REFERENCES ihms_item.t_stock_transfer (id),
    item_id BIGINT NOT NULL REFERENCES ihms_item.item (id),
    batch_id BIGINT REFERENCES ihms_item.item_batch (id),
    quantity NUMERIC(19,4) NOT NULL CHECK (quantity > 0),
    received_qty NUMERIC(19,4) NOT NULL DEFAULT 0,
    remarks VARCHAR(500),
    CONSTRAINT pk_t_stock_transfer_detail PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_t_stock_transfer_detail_hdr ON ihms_item.t_stock_transfer_detail (transfer_id);

-- Sequences (IMP-DEC-003)
CREATE SEQUENCE IF NOT EXISTS ihms_item.t_expiry_return_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_item.t_expiry_return_detail_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_item.t_stock_transfer_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_item.t_stock_transfer_detail_seq START WITH 1;
