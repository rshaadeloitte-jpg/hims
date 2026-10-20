-- ============================================================================
-- V1 — ihms_rmrs schema (RMRS & PMRD Treasury service, S24)
--       WP-R5 (Wave-5 tranche 5, GAP-REGISTER-5, Catalog #24)
-- Clean target of the legacy treasury slice: receipt/collection masters and
-- day-book postings (t_head_master / t_authority_master / t_ledger_master /
-- t_rmrs_collection / t_accounting_entry).
--
-- ONE-TABLE-ONE-OWNER (§56): ALL tables here belong to the rmrs service.
-- Money columns are NUMERIC(19,2) per register WP-R5.
--
-- Balanced-accounting invariant (register WP-R5): every collection writes a
-- debit posting against the treasury-head ledger and a credit posting against
-- the collection ledger — exactly one side of each entry > 0, the other = 0,
-- and SUM(debit) = SUM(credit) per collection. Enforced in CollectionService
-- (build-time + post-save verification inside the SAME tx as the collection)
-- and pinned by unit + PG16 integration tests.
--
-- Events: CollectionPosted is PROPOSED with NO consumers (KAFKA-EVENT-CATALOG)
-- — nothing is ever written to the outbox below; the table exists solely so
-- tests can pin that absence explicitly. E-08 PaymentReceived consumption
-- (auto-receipt generation) is a Wave-5b read-model concern — payment_ref is
-- ready for it.
--
-- Legacy ROWNUM pagination (POSTGRESQL-MIGRATION :1361-1366) is REPLACED by
-- limit-offset pagination on the collection search query (see
-- RmrsCollectionRepository.search + GET /api/v1/collections?page=&size=).
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS ihms_rmrs;

SET search_path TO ihms_rmrs, public;

-- ---------------------------------------------------------------------------
-- t_head_master — treasury head (service-wise summary analog of s_servicewisecollection)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_rmrs.t_head_master (
    head_id BIGINT NOT NULL,
    head_code VARCHAR(20) NOT NULL,
    head_name VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_t_head_master PRIMARY KEY (head_id),
    CONSTRAINT uq_t_head_master_code UNIQUE (head_code)
);
COMMENT ON TABLE ihms_rmrs.t_head_master IS 'Treasury head master (WP-R5). head_code UNIQUE; is_active default true.';

-- ---------------------------------------------------------------------------
-- t_authority_master — collecting authority; head link optional per register
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_rmrs.t_authority_master (
    authority_id BIGINT NOT NULL,
    authority_code VARCHAR(20) NOT NULL,
    authority_name VARCHAR(120),
    head_id BIGINT REFERENCES ihms_rmrs.t_head_master (head_id),
    unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_t_authority_master PRIMARY KEY (authority_id),
    CONSTRAINT uq_t_authority_master_code UNIQUE (authority_code)
);
CREATE INDEX IF NOT EXISTS ix_t_authority_master_head ON ihms_rmrs.t_authority_master (head_id);
COMMENT ON TABLE ihms_rmrs.t_authority_master IS 'Collecting authority master (WP-R5); head_id FK NULL by register.';

-- ---------------------------------------------------------------------------
-- t_ledger_master — ledger; MUST belong to a head (head-ledger consistency guard)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_rmrs.t_ledger_master (
    ledger_id BIGINT NOT NULL,
    ledger_code VARCHAR(20) NOT NULL,
    ledger_name VARCHAR(120),
    head_id BIGINT NOT NULL REFERENCES ihms_rmrs.t_head_master (head_id),
    unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_t_ledger_master PRIMARY KEY (ledger_id),
    CONSTRAINT uq_t_ledger_master_code UNIQUE (ledger_code)
);
CREATE INDEX IF NOT EXISTS ix_t_ledger_master_head ON ihms_rmrs.t_ledger_master (head_id);
COMMENT ON TABLE ihms_rmrs.t_ledger_master IS 'Ledger master (WP-R5); head_id FK NOT NULL — a collection''s ledgerCode must resolve to a ledger belonging to the given headCode (400 BEXA003 otherwise).';

-- ---------------------------------------------------------------------------
-- t_rmrs_collection — one cash receipt; receipt_no UNIQUE (legacy ROWNUM list
-- replaced by limit-offset search — see repository comment).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_rmrs.t_rmrs_collection (
    collection_id BIGINT NOT NULL,
    receipt_no VARCHAR(24) NOT NULL,
    collection_date DATE NOT NULL,
    patient_id BIGINT,
    amount NUMERIC(19,2) NOT NULL,
    head_id BIGINT NOT NULL REFERENCES ihms_rmrs.t_head_master (head_id),
    ledger_id BIGINT NOT NULL REFERENCES ihms_rmrs.t_ledger_master (ledger_id),
    authority_id BIGINT REFERENCES ihms_rmrs.t_authority_master (authority_id),
    payment_ref VARCHAR(40),          -- E-08 paymentId linkage (string, cross-service id; Wave-5b read-model)
    counter VARCHAR(20),
    collected_by VARCHAR(80),
    unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_t_rmrs_collection PRIMARY KEY (collection_id),
    CONSTRAINT uq_t_rmrs_collection_receipt UNIQUE (receipt_no),
    CONSTRAINT ck_t_rmrs_collection_amount CHECK (amount > 0)
);
CREATE INDEX IF NOT EXISTS ix_t_rmrs_collection_date ON ihms_rmrs.t_rmrs_collection (collection_date);
CREATE INDEX IF NOT EXISTS ix_t_rmrs_collection_head ON ihms_rmrs.t_rmrs_collection (head_id);
CREATE INDEX IF NOT EXISTS ix_t_rmrs_collection_user ON ihms_rmrs.t_rmrs_collection (collected_by);
COMMENT ON TABLE ihms_rmrs.t_rmrs_collection IS 'Cash collection / receipt (WP-R5). receipt_no UNIQUE (API pre-checks -> 409 RMRS_DUPLICATE_RECEIPT; DB constraint backstops). ck pins amount > 0 at DB level too. payment_ref holds the E-08 PaymentReceived paymentId (string, cross-service) for the Wave-5b auto-receipt read model — consuming E-08 is comment-only here.';

-- ---------------------------------------------------------------------------
-- t_accounting_entry — double-entry postings, exactly one side > 0;
-- balanced per collection (SUM(debit)=SUM(credit)) enforced in the service
-- within the same transaction as the collection insert.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_rmrs.t_accounting_entry (
    entry_id BIGINT NOT NULL,
    collection_id BIGINT NOT NULL REFERENCES ihms_rmrs.t_rmrs_collection (collection_id),
    ledger_id BIGINT NOT NULL REFERENCES ihms_rmrs.t_ledger_master (ledger_id),
    debit NUMERIC(19,2) NOT NULL DEFAULT 0,
    credit NUMERIC(19,2) NOT NULL DEFAULT 0,
    entry_date DATE NOT NULL,
    unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_t_accounting_entry PRIMARY KEY (entry_id),
    CONSTRAINT ck_t_accounting_entry_one_sided
        CHECK ((debit > 0 AND credit = 0) OR (credit > 0 AND debit = 0))
);
CREATE INDEX IF NOT EXISTS ix_t_accounting_entry_collection ON ihms_rmrs.t_accounting_entry (collection_id);
CREATE INDEX IF NOT EXISTS ix_t_accounting_entry_ledger ON ihms_rmrs.t_accounting_entry (ledger_id);
COMMENT ON TABLE ihms_rmrs.t_accounting_entry IS 'Double-entry postings (WP-R5): DR the treasury-head ledger / CR the collection ledger per collection. ck_t_accounting_entry_one_sided pins "exactly one of debit/credit > 0 and other = 0"; balance SUM(debit)=SUM(credit) per collection is enforced in CollectionService inside the same tx and pinned by tests.';

-- ---------------------------------------------------------------------------
-- Transactional outbox (IMP-DEC-004 DDL parity with sibling services incl.
-- event_id UNIQUE). NOTE (WP-R5 event policy): CollectionPosted is PROPOSED
-- with no consumers — THIS SERVICE NEVER WRITES HERE. The table exists only so
-- integration tests can assert its emptiness explicitly (negative assertion).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_rmrs.outbox (
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
COMMENT ON TABLE ihms_rmrs.outbox IS 'NEGATIVE-ASSERTION table only: WP-R5 defers/rejects CollectionPosted (PROPOSED, no consumers) — no code path in this service inserts rows here; integration tests pin count()=0 after successful AND rejected collections.';
CREATE INDEX idx_outbox_dispatch ON ihms_rmrs.outbox (status, created_at) WHERE status = 'PENDING';

-- ---------------------------------------------------------------------------
-- Sequences for migrated tables (IMP-DEC-003)
-- ---------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS ihms_rmrs.t_head_master_seq      START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_rmrs.t_authority_master_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_rmrs.t_ledger_master_seq    START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_rmrs.t_rmrs_collection_seq  START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_rmrs.t_accounting_entry_seq START WITH 1;
