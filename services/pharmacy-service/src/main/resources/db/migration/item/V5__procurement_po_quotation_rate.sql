-- ============================================================================
-- V5 — M14 procurement: purchase orders, quotations, rate contracts,
-- provisional/consignment GRN flags.
-- Ported from legacy PurchaseOrderServiceImpl, QuotationServiceImpl, and the
-- consignment / rate-contract DTOs in com.ohum.dto.pas.
-- Status dictionaries (locked at this version):
--   t_purchase_order.doc_status:  1=DRAFT 2=PENDING_APPROVAL 3=APPROVED
--                                 4=ISSUED 5=CLOSED 6=CANCELLED
--   t_quotation.doc_status:       1=DRAFT 2=SUBMITTED 3=AWARDED
--                                 4=REJECTED 5=EXPIRED
--   t_rate_contract.status:       1=ACTIVE 2=EXPIRED 3=CANCELLED
-- ============================================================================
SET search_path TO ihms_item, public;

-- ---------------------------------------------------------------------------
-- t_purchase_order — header (legacy T_PURCHASEORDER)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_item.t_purchase_order (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    po_no VARCHAR(64) NOT NULL,
    quotation_id BIGINT,
    store_id BIGINT NOT NULL REFERENCES ihms_item.store (id),
    supplier_id BIGINT NOT NULL REFERENCES ihms_item.supplier (id),
    order_date DATE,
    expected_date DATE,
    remarks VARCHAR(500),
    doc_status SMALLINT NOT NULL DEFAULT 1 CHECK (doc_status BETWEEN 1 AND 6),
    approved_by BIGINT,
    approved_datetime TIMESTAMP(3),
    issued_at TIMESTAMP(3),
    closed_at TIMESTAMP(3),
    CONSTRAINT pk_t_purchase_order PRIMARY KEY (id),
    CONSTRAINT uq_t_purchase_order_no UNIQUE (po_no)
);
CREATE INDEX IF NOT EXISTS ix_t_purchase_order_store_status ON ihms_item.t_purchase_order (store_id, doc_status);
CREATE INDEX IF NOT EXISTS ix_t_purchase_order_supplier_status ON ihms_item.t_purchase_order (supplier_id, doc_status);
CREATE INDEX IF NOT EXISTS ix_t_purchase_order_quotation ON ihms_item.t_purchase_order (quotation_id);

CREATE TABLE IF NOT EXISTS ihms_item.t_purchase_order_detail (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    po_id BIGINT NOT NULL REFERENCES ihms_item.t_purchase_order (id),
    item_id BIGINT NOT NULL REFERENCES ihms_item.item (id),
    ordered_qty NUMERIC(19,4) NOT NULL CHECK (ordered_qty > 0),
    received_qty NUMERIC(19,4) NOT NULL DEFAULT 0,
    unit_price NUMERIC(19,2) NOT NULL,
    tax_percent NUMERIC(6,2) NOT NULL DEFAULT 0,
    lead_time_days INTEGER,
    remarks VARCHAR(500),
    CONSTRAINT pk_t_purchase_order_detail PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_t_purchase_order_detail_po ON ihms_item.t_purchase_order_detail (po_id);
CREATE INDEX IF NOT EXISTS ix_t_purchase_order_detail_item ON ihms_item.t_purchase_order_detail (item_id);

-- ---------------------------------------------------------------------------
-- t_quotation — header (legacy T_QUOTATION)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_item.t_quotation (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    quotation_no VARCHAR(64) NOT NULL,
    supplier_id BIGINT NOT NULL REFERENCES ihms_item.supplier (id),
    enquiry_ref VARCHAR(64),
    valid_until DATE,
    submitted_at TIMESTAMP(3),
    awarded_at TIMESTAMP(3),
    awarded_po_id BIGINT,
    remarks VARCHAR(500),
    doc_status SMALLINT NOT NULL DEFAULT 1 CHECK (doc_status BETWEEN 1 AND 5),
    CONSTRAINT pk_t_quotation PRIMARY KEY (id),
    CONSTRAINT uq_t_quotation_no UNIQUE (quotation_no)
);
CREATE INDEX IF NOT EXISTS ix_t_quotation_supplier_status ON ihms_item.t_quotation (supplier_id, doc_status);

CREATE TABLE IF NOT EXISTS ihms_item.t_quotation_detail (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    quotation_id BIGINT NOT NULL REFERENCES ihms_item.t_quotation (id),
    item_id BIGINT NOT NULL REFERENCES ihms_item.item (id),
    quantity NUMERIC(19,4) NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(19,2) NOT NULL,
    tax_percent NUMERIC(6,2) NOT NULL DEFAULT 0,
    lead_time_days INTEGER,
    remarks VARCHAR(500),
    CONSTRAINT pk_t_quotation_detail PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_t_quotation_detail_quotation ON ihms_item.t_quotation_detail (quotation_id);
CREATE INDEX IF NOT EXISTS ix_t_quotation_detail_item ON ihms_item.t_quotation_detail (item_id);

-- ---------------------------------------------------------------------------
-- t_rate_contract — legacy T_RATECONTRACT (supplier × item × validity window)
-- status: 1=ACTIVE 2=EXPIRED 3=CANCELLED
-- Only one ACTIVE contract per (supplier_id, item_id) is allowed at a time.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_item.t_rate_contract (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    contract_no VARCHAR(64) NOT NULL,
    supplier_id BIGINT NOT NULL REFERENCES ihms_item.supplier (id),
    item_id BIGINT NOT NULL REFERENCES ihms_item.item (id),
    contract_price NUMERIC(19,2) NOT NULL,
    tax_percent NUMERIC(6,2) NOT NULL DEFAULT 0,
    min_order_qty NUMERIC(19,4),
    valid_from DATE NOT NULL,
    valid_to DATE NOT NULL,
    status_flag SMALLINT NOT NULL DEFAULT 1 CHECK (status_flag BETWEEN 1 AND 3),
    remarks VARCHAR(500),
    CONSTRAINT pk_t_rate_contract PRIMARY KEY (id),
    CONSTRAINT uq_t_rate_contract_no UNIQUE (contract_no),
    CONSTRAINT ck_t_rate_contract_window CHECK (valid_to >= valid_from)
);
CREATE INDEX IF NOT EXISTS ix_t_rate_contract_supplier_item ON ihms_item.t_rate_contract (supplier_id, item_id);
CREATE INDEX IF NOT EXISTS ix_t_rate_contract_status ON ihms_item.t_rate_contract (status_flag);

-- ---------------------------------------------------------------------------
-- Extend t_grn with provisional + consignment flags.
--   provisional: 1 = the GRN is a "provisional" receipt against a PO that has
--                not yet been fully matched (legacy ConsignmentGRNDto.provisionalFlag).
--   consignment:  1 = the GRN is for a consignment stock (legacy
--                ConsignmentGRNDto.consignmentFlag). Stock still posts via
--                the D-1 single-writer; the flag scopes downstream sale logic.
-- ---------------------------------------------------------------------------
ALTER TABLE ihms_item.t_grn
    ADD COLUMN IF NOT EXISTS provisional BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE ihms_item.t_grn
    ADD COLUMN IF NOT EXISTS consignment BOOLEAN NOT NULL DEFAULT FALSE;
COMMENT ON COLUMN ihms_item.t_grn.provisional IS 'Provisional GRN flag (legacy ConsignmentGRNDto.provisionalFlag).';
COMMENT ON COLUMN ihms_item.t_grn.consignment IS 'Consignment GRN flag (legacy ConsignmentGRNDto.consignmentFlag).';

-- ---------------------------------------------------------------------------
-- Sequences (IMP-DEC-003)
-- ---------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS ihms_item.t_purchase_order_seq          START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_item.t_purchase_order_detail_seq   START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_item.t_quotation_seq               START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_item.t_quotation_detail_seq        START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_item.t_rate_contract_seq           START WITH 1;
