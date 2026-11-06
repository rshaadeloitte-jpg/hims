-- ============================================================================
-- V2 — M23 RMRS gap (legacy com.ohum.services.RMRSImpl):
--   * journal voucher (multi-leg direct journal entries, no collection)
--   * suspense account (flag ledger as suspense; clear/transfer operations)
--   * bank statement (ingest + reconcile lines against journal/collection)
--   * PO request (RMRS-side procurement request)
-- Status dictionaries (locked at this version):
--   t_journal_voucher.doc_status:   1=DRAFT 2=POSTED 3=REVERSED
--   t_bank_statement.doc_status:   1=IMPORTED 2=RECONCILED 3=DISPUTED
--   t_po_request.doc_status:        1=NEW 2=APPROVED 3=ISSUED 4=REJECTED
--   t_ledger_master.suspense:       boolean flag (one or more ledgers marked
--                                    as suspense for clearing pending postings)
-- ============================================================================
SET search_path TO ihms_rmrs, public;

-- Journal voucher (header) — independent of t_rmrs_collection
CREATE TABLE IF NOT EXISTS ihms_rmrs.t_journal_voucher (
    voucher_id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    voucher_no VARCHAR(64) NOT NULL,
    narration VARCHAR(500),
    voucher_date DATE NOT NULL,
    doc_status SMALLINT NOT NULL DEFAULT 1 CHECK (doc_status BETWEEN 1 AND 3),
    posted_by BIGINT,
    posted_datetime TIMESTAMP(3),
    reversed_by BIGINT,
    reversed_datetime TIMESTAMP(3),
    CONSTRAINT pk_t_journal_voucher PRIMARY KEY (voucher_id),
    CONSTRAINT uq_t_journal_voucher_no UNIQUE (voucher_no)
);
CREATE INDEX IF NOT EXISTS ix_t_journal_voucher_date ON ihms_rmrs.t_journal_voucher (voucher_date);
COMMENT ON TABLE ihms_rmrs.t_journal_voucher IS 'M23 journal voucher header (multi-leg direct journal).';

-- Journal voucher legs (1 row per debit/credit leg of a voucher)
CREATE TABLE IF NOT EXISTS ihms_rmrs.t_journal_voucher_leg (
    leg_id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    voucher_id BIGINT NOT NULL REFERENCES ihms_rmrs.t_journal_voucher (voucher_id),
    ledger_id BIGINT NOT NULL REFERENCES ihms_rmrs.t_ledger_master (ledger_id),
    debit NUMERIC(19,2) NOT NULL DEFAULT 0,
    credit NUMERIC(19,2) NOT NULL DEFAULT 0,
    CONSTRAINT pk_t_journal_voucher_leg PRIMARY KEY (leg_id),
    CONSTRAINT ck_t_journal_voucher_leg_one_sided CHECK (
        (debit > 0 AND credit = 0) OR (debit = 0 AND credit > 0))
);
CREATE INDEX IF NOT EXISTS ix_t_journal_voucher_leg_voucher ON ihms_rmrs.t_journal_voucher_leg (voucher_id);
CREATE INDEX IF NOT EXISTS ix_t_journal_voucher_leg_ledger ON ihms_rmrs.t_journal_voucher_leg (ledger_id);
COMMENT ON TABLE ihms_rmrs.t_journal_voucher_leg IS 'One debit OR credit leg of a journal voucher. Exactly one side > 0 (DB CHECK).';

-- Bank statement lines (one row per statement line per ledger)
CREATE TABLE IF NOT EXISTS ihms_rmrs.t_bank_statement (
    statement_line_id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    bank_ledger_id BIGINT NOT NULL REFERENCES ihms_rmrs.t_ledger_master (ledger_id),
    txn_date DATE NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    direction VARCHAR(6) NOT NULL CHECK (direction IN ('DEBIT','CREDIT')),
    description VARCHAR(500),
    external_ref VARCHAR(64),
    doc_status SMALLINT NOT NULL DEFAULT 1 CHECK (doc_status BETWEEN 1 AND 3),
    matched_entry_id BIGINT,
    matched_collection_id BIGINT,
    CONSTRAINT pk_t_bank_statement PRIMARY KEY (statement_line_id)
);
CREATE INDEX IF NOT EXISTS ix_t_bank_statement_status ON ihms_rmrs.t_bank_statement (doc_status);
CREATE INDEX IF NOT EXISTS ix_t_bank_statement_date ON ihms_rmrs.t_bank_statement (txn_date);
COMMENT ON TABLE ihms_rmrs.t_bank_statement IS 'M23 bank statement (M23 reconciliation). direction = DEBIT|CREDIT. matched_*_id only set when doc_status = RECONCILED.';

-- PO request (RMRS-side procurement request to item-service)
CREATE TABLE IF NOT EXISTS ihms_rmrs.t_po_request (
    request_id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    request_no VARCHAR(64) NOT NULL,
    requesting_dept_id BIGINT,
    head_id BIGINT NOT NULL REFERENCES ihms_rmrs.t_head_master (head_id),
    ledger_id BIGINT REFERENCES ihms_rmrs.t_ledger_master (ledger_id),
    amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    remarks VARCHAR(500),
    doc_status SMALLINT NOT NULL DEFAULT 1 CHECK (doc_status BETWEEN 1 AND 4),
    approved_by BIGINT,
    approved_datetime TIMESTAMP(3),
    issued_po_id BIGINT,
    CONSTRAINT pk_t_po_request PRIMARY KEY (request_id),
    CONSTRAINT uq_t_po_request_no UNIQUE (request_no)
);
CREATE INDEX IF NOT EXISTS ix_t_po_request_status ON ihms_rmrs.t_po_request (doc_status);
COMMENT ON TABLE ihms_rmrs.t_po_request IS 'M23 RMRS PO request to procurement (item-service).';

-- Suspense flag on ledger (NULL = normal; non-NULL = held in suspense since)
ALTER TABLE ihms_rmrs.t_ledger_master
    ADD COLUMN IF NOT EXISTS suspense BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE ihms_rmrs.t_ledger_master
    ADD COLUMN IF NOT EXISTS suspense_reason VARCHAR(255);

-- Sequences
CREATE SEQUENCE IF NOT EXISTS ihms_rmrs.t_journal_voucher_seq      START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_rmrs.t_journal_voucher_leg_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_rmrs.t_bank_statement_seq      START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_rmrs.t_po_request_seq          START WITH 1;
