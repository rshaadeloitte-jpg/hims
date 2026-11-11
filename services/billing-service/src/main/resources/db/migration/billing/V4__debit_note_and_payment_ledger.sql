-- ============================================================================
-- V4 — Debit Notes + Partial Payment tracking (M04 gap)
-- Ported from old IHMS: T_DEBITNOTE (opposite of credit note; positive adjustment
-- on a finalized bill — used when additional services/charges need to be added
-- post-finalize, or when errors in concession are corrected upward).
-- Also adds partial-payment ledger table that records running payment state
-- without depending on the payments-service ledger (read-side mirror for
-- billing UI; single-writer is still billing-service).
-- ============================================================================

SET search_path TO ihms_billing, public;

-- ---------------------------------------------------------------------------
-- t_debit_note — positive adjustment against a finalized bill
-- amount is INCREASED into the bill total (opposite of credit note).
-- A debit note cannot exceed the original bill total * 1.5 (safety guard
-- against runaway adjustments; same legacy CIMS_CHECKDEBITNOTE pattern).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_billing.t_debit_note (
    debit_note_id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,

    bill_id BIGINT NOT NULL REFERENCES ihms_billing.t_bill (bill_id),
    patient_id BIGINT NOT NULL,
    debit_note_no VARCHAR(32) NOT NULL,
    reason VARCHAR(500),
    amount NUMERIC(19,2) NOT NULL,
    is_applied BOOLEAN NOT NULL DEFAULT FALSE,
    approved_by BIGINT,
    approved_at TIMESTAMP(3),

    CONSTRAINT pk_t_debit_note PRIMARY KEY (debit_note_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_debit_note_no ON ihms_billing.t_debit_note (debit_note_no);
CREATE INDEX IF NOT EXISTS ix_debit_note_bill ON ihms_billing.t_debit_note (bill_id);
CREATE INDEX IF NOT EXISTS ix_debit_note_patient ON ihms_billing.t_debit_note (patient_id);
COMMENT ON TABLE ihms_billing.t_debit_note IS 'Debit notes (positive adjustment) on finalized bills (M04 gap). Opposite of credit note: amount ADDS to bill total. Approval required (approved_by/at) before apply.';

CREATE SEQUENCE IF NOT EXISTS ihms_billing."t_debit_note_seq"     START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_billing."t_debit_note_no_seq"  START WITH 1 INCREMENT BY 1;

-- ---------------------------------------------------------------------------
-- t_bill_payment_ledger — partial-payment ledger (billing-side mirror)
-- Single-writer is billing-service; payments-service is the source-of-truth
-- for the full payment state, but this ledger allows the billing UI to
-- answer "what's the running balance" without a sync-roundtrip.
-- Each row = a (re)computation of paidSelf/paidNonSelf/balance for a bill.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_billing.t_bill_payment_ledger (
    ledger_id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,

    bill_id BIGINT NOT NULL REFERENCES ihms_billing.t_bill (bill_id),
    event_type VARCHAR(32) NOT NULL,   -- PAYMENT | REFUND | ADVANCE_USE | CANCEL
    paid_self NUMERIC(19,2) NOT NULL DEFAULT 0,
    paid_non_self NUMERIC(19,2) NOT NULL DEFAULT 0,
    balance_self NUMERIC(19,2) NOT NULL DEFAULT 0,
    balance_non_self NUMERIC(19,2) NOT NULL DEFAULT 0,
    is_settled BOOLEAN NOT NULL DEFAULT FALSE,
    source_ref VARCHAR(64),           -- paymentId / refundId / advanceId
    source_service VARCHAR(64),       -- 'payments-service' etc
    notes VARCHAR(500),

    CONSTRAINT pk_t_bill_payment_ledger PRIMARY KEY (ledger_id)
);
CREATE INDEX IF NOT EXISTS ix_bill_payment_ledger_bill ON ihms_billing.t_bill_payment_ledger (bill_id);
CREATE INDEX IF NOT EXISTS ix_bill_payment_ledger_event_type ON ihms_billing.t_bill_payment_ledger (event_type);
COMMENT ON TABLE ihms_billing.t_bill_payment_ledger IS 'Running payment-state ledger for bills (M04 partial-payment). One row per (re)computation. Authoritative payment state lives in payments-service; this is the billing-UI mirror.';

CREATE SEQUENCE IF NOT EXISTS ihms_billing."t_bill_payment_ledger_seq" START WITH 1 INCREMENT BY 1;
