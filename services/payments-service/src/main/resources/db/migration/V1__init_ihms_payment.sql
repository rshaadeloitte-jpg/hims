-- ============================================================================
-- V1 — ihms_payment schema (Payments & Refunds service, S06) — WP-13
-- (Wave-4 tranche 4, Catalog #6). Clean target of the legacy payment cluster:
-- T_PAYMENT(+T_PAYMENTMODEDETAILS), T_ADVANCE, T_REFUND plus the transactional
-- outbox for E-08 PaymentReceived / E-12 RefundIssued.
--
-- ONE-TABLE-ONE-OWNER (§56): ALL tables here belong to the payments service.
-- TransactionType SMALLINT CHECK IN (2,3,4) per 06-business-rules-catalog §A6:
--   2=payment  3=advance refund  4=bill refund
-- t_advance.balance is GENERATED ALWAYS AS (amount - used) STORED: the DB owns
-- remaining-advance arithmetic; services only mutate amount/used.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS ihms_payment;

SET search_path TO ihms_payment, public;

-- ---------------------------------------------------------------------------
-- t_advance — clean target of T_ADVANCE (CIMS_ADDADVANCE).
-- balance is a generated column: NEVER written directly. A refund reduces
-- `amount`; BR-BILL-007 consumption increments `used`.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_payment.t_advance (
    advance_id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    patient_id BIGINT NOT NULL,
    amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    used NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (used >= 0),
    balance NUMERIC(19,2) GENERATED ALWAYS AS (amount - used) STORED,
    CONSTRAINT pk_t_advance PRIMARY KEY (advance_id)
);
CREATE INDEX IF NOT EXISTS ix_t_advance_patient ON ihms_payment.t_advance (patient_id);
COMMENT ON TABLE ihms_payment.t_advance IS 'Clean target of T_ADVANCE. balance = amount - used (GENERATED ALWAYS STORED) — the DB owns the arithmetic; refund decrements amount (GAP-REGISTER wording "decrements used" interpreted as reducing the REMAINING balance; used tracks BR-BILL-007 bill consumption and decrementing it would INCREASE balance — PRELIMINARY interpretation flagged in WP-13 report).';

-- ---------------------------------------------------------------------------
-- t_payment — clean target of T_PAYMENT. transaction_type per §A6 CHECK IN
-- (2,3,4); this service inserts type 2 here (refunds are recorded on t_refund).
-- change_amount mirrors legacy T_Bill.ChangeAmount capture (BR-BILL-006 note).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_payment.t_payment (
    payment_id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    bill_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    transaction_type SMALLINT NOT NULL DEFAULT 2 CHECK (transaction_type IN (2, 3, 4)),
    change_amount NUMERIC(19,2) NOT NULL DEFAULT 0,
    CONSTRAINT pk_t_payment PRIMARY KEY (payment_id)
);
CREATE INDEX IF NOT EXISTS ix_t_payment_bill ON ihms_payment.t_payment (bill_id);
CREATE INDEX IF NOT EXISTS ix_t_payment_patient ON ihms_payment.t_payment (patient_id);
COMMENT ON TABLE ihms_payment.t_payment IS 'Clean target of T_PAYMENT. transaction_type SMALLINT CHECK IN (2,3,4) per 06 §A6 (2=payment, 3=advance refund, 4=bill refund); cash/advance rows of one capture live in t_payment_mode_detail (mode 7 = ADVANCE reserved).';

-- ---------------------------------------------------------------------------
-- t_payment_mode_detail — clean target of T_PAYMENTMODEDETAILS.
-- mode 7 = ADVANCE reserved: BR-BILL-007 partial splits are recorded as extra
-- mode-7 rows alongside the tendered modes.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_payment.t_payment_mode_detail (
    mode_detail_id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    payment_id BIGINT NOT NULL REFERENCES ihms_payment.t_payment (payment_id),
    mode SMALLINT NOT NULL,
    amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    CONSTRAINT pk_t_payment_mode_detail PRIMARY KEY (mode_detail_id)
);
CREATE INDEX IF NOT EXISTS ix_t_payment_mode_detail_payment ON ihms_payment.t_payment_mode_detail (payment_id);
COMMENT ON TABLE ihms_payment.t_payment_mode_detail IS 'Clean target of T_PAYMENTMODEDETAILS. mode 7 = ADVANCE reserved (BR-BILL-007): every FIFO consumption chunk gets its own row + t_advance_consumption line.';

-- ---------------------------------------------------------------------------
-- t_advance_consumption — BR-BILL-007 ledger (legacy CIMS_ADDPAYMENTADVANCEDETAILS).
-- One row per (advance, payment) chunk consumed against the self balance.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_payment.t_advance_consumption (
    consumption_id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    advance_id BIGINT NOT NULL REFERENCES ihms_payment.t_advance (advance_id),
    payment_id BIGINT NOT NULL REFERENCES ihms_payment.t_payment (payment_id),
    consumed_amount NUMERIC(19,2) NOT NULL CHECK (consumed_amount > 0),
    CONSTRAINT pk_t_advance_consumption PRIMARY KEY (consumption_id),
    CONSTRAINT uq_consumption_per_payment UNIQUE (advance_id, payment_id)
);
CREATE INDEX IF NOT EXISTS ix_t_advance_consumption_payment ON ihms_payment.t_advance_consumption (payment_id);
CREATE INDEX IF NOT EXISTS ix_t_advance_consumption_advance ON ihms_payment.t_advance_consumption (advance_id);
COMMENT ON TABLE ihms_payment.t_advance_consumption IS 'BR-BILL-007 advance-adjustment ledger: advances with balance>0 consumed in advance_id order against the self balance first; partial splits recorded chunk-by-chunk.';

-- ---------------------------------------------------------------------------
-- t_refund — clean target of T_REFUND (AddRefundServiceImpl :82-155).
-- transactionType 3 (advance refund; guard BEXAR002 when balance <= 0) and
-- 4 (bill refund). Restore-of-charges compensation for settled-bill
-- cancellation stays billing-side (BR-BILL-008) — deferred, documented.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_payment.t_refund (
    refund_id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    transaction_type SMALLINT NOT NULL CHECK (transaction_type IN (3, 4)),
    bill_id BIGINT,
    advance_id BIGINT,
    patient_id BIGINT NOT NULL,
    amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    reason VARCHAR(500) NOT NULL,
    CONSTRAINT pk_t_refund PRIMARY KEY (refund_id)
);
CREATE INDEX IF NOT EXISTS ix_t_refund_bill ON ihms_payment.t_refund (bill_id);
CREATE INDEX IF NOT EXISTS ix_t_refund_patient ON ihms_payment.t_refund (patient_id);
COMMENT ON TABLE ihms_payment.t_refund IS 'Clean target of T_REFUND. transactionType 3 = advance refund (requires t_advance.balance > 0 else error BEXAR002; reduces t_advance.amount), 4 = bill refund. restoredCharges[] on E-12 stays empty until billing-side compensation lands (PRELIMINARY per KAFKA-EVENT-CATALOG E-12).';

-- ---------------------------------------------------------------------------
-- Transactional outbox (IMP-DEC-004; envelope per KAFKA-EVENT-CATALOG §4;
-- DDL mirrors sibling services incl. event_id UNIQUE). Events:
--   PaymentReceived  payment-received.v1  (E-08, partition key billId)
--   RefundIssued     refund-issued.v1     (E-12 PRELIMINARY, partition key billId)
--   -> topic ihms.payment.events via relay.
--   NOTE: the settlement saga step is SYNCHRONOUS request-scoped REST to
--   billing-service; the outbox append happens AFTER a successful settlement
--   call inside the same DB transaction (documented saga deviation — see
--   GAP-REGISTER-4 WP-13 / WP-13-payments.md report).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_payment.outbox (
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
COMMENT ON TABLE ihms_payment.outbox IS 'Transactional outbox: E-08 PaymentReceived is appended atomically with the t_payment/t_payment_mode_detail/t_advance_consumption writes AFTER the synchronous billing settlement REST call succeeded; E-12 RefundIssued likewise with the t_refund write. Relay publishes to ihms.payment.events.';
CREATE INDEX idx_outbox_dispatch ON ihms_payment.outbox (status, created_at) WHERE status = 'PENDING';

-- ---------------------------------------------------------------------------
-- Sequences for migrated tables (IMP-DEC-003)
-- ---------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS ihms_payment.t_advance_seq             START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_payment.t_payment_seq             START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_payment.t_payment_mode_detail_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_payment.t_advance_consumption_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_payment.t_refund_seq              START WITH 1;
