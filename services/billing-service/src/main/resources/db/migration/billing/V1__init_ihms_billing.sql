-- ============================================================================
-- V1 — ihms_billing schema (MIG-040: Billing & Settlement + MIG-041: Payments,
--      Refunds, Advances — Wave 4 Financial/Revenue spine)
--
-- Catalog #5 + slice of catalog #6. Owns:
--   t_bill             — clean target of T_OPDBILL / T_IPDBILL header (BR-BILL-001)
--   t_charge           — clean target of T_BILLDETAILS service-line (BR-BILL-005)
--   t_doctor_share     — clean target of T_DOCTORSHARE (BR-BILL-002)
--   t_advance_ledger   — patient advance balance + FIFO consumption (BR-BILL-007)
--   t_receipt          — payment receipt (BR-BILL-006 settlement)
--   t_refund           — refund / cancellation (BR-BILL-008)
--   t_consumed_event   — WP-S1 saga consumer dedupe ledger
--   outbox             — transactional outbox (IMP-DEC-004)
--
-- Bill-number format: BR-BILL-001 placeholder 000000000000000 at creation,
-- finalized as 'OPDBILL-<seq>' (no admissionId) or 'IPDBILL-<seq>' (admissionId).
-- Sequential per-unit sequences t_opdbill_seq / t_ipdbill_seq keep the legacy
-- CIMS_GETLATESTAUTOCODEANDINCREMETITBYONE semantics.
--
-- Money columns: NUMERIC(19,2) — financial precision (cannot use FLOAT/DOUBLE).
-- Idempotency backstops:
--   uq_t_consumed_event_event_id       UNIQUE(event_id) on t_consumed_event
--   uq_t_bill_saga_event                partial UNIQUE on t_bill.saga_event_id
--   uq_outbox_event_id                  UNIQUE(event_id) on outbox
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS ihms_billing;

SET search_path TO ihms_billing, public;

-- ---------------------------------------------------------------------------
-- Sequences (server-owned explicit sequences, IMP-DEC-003)
-- ---------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS ihms_billing.t_bill_seq            START WITH 1 INCREMENT BY 1 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS ihms_billing.t_charge_seq          START WITH 1 INCREMENT BY 1 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS ihms_billing.t_doctor_share_seq    START WITH 1 INCREMENT BY 1 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS ihms_billing.t_opdbill_seq         START WITH 1 INCREMENT BY 1 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS ihms_billing.t_ipdbill_seq         START WITH 1 INCREMENT BY 1 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS ihms_billing.t_consumed_event_seq  START WITH 1 INCREMENT BY 1 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS ihms_billing.t_advance_seq         START WITH 1 INCREMENT BY 1 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS ihms_billing.t_receipt_seq         START WITH 1 INCREMENT BY 1 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS ihms_billing.t_refund_seq          START WITH 1 INCREMENT BY 1 NO CYCLE;

-- ---------------------------------------------------------------------------
-- t_bill — clean target of T_OPDBILL / T_IPDBILL header (BR-BILL-001)
-- BR-BILL-001: created with status=true, is_settled=false, is_cancelled=false,
-- bill_no='000000000000000' placeholder, cash_counter_id=1; final billNo
-- OPDBILL-<seq> (no admissionId) or IPDBILL-<seq> (admissionId) at finalize.
-- BR-BILL-006: is_settled <=> balance_self <= 0 AND balance_non_self <= 0.
-- *BeforeFinalization columns (BR-BILL-008 restore contract) are charged rows;
-- the bill header recomputes from charges at finalize.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_billing.t_bill (
    bill_id                   BIGINT       NOT NULL,
    unit_id                   BIGINT,
    status                    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_unit_id           BIGINT,
    updated_unit_id           BIGINT,
    added_by                  BIGINT,
    added_datetime            TIMESTAMP(3),
    updated_by                BIGINT,
    updated_datetime          TIMESTAMP(3),
    synchronized              BOOLEAN      NOT NULL DEFAULT FALSE,

    patient_id                BIGINT       NOT NULL,
    encounter_id              BIGINT,
    admission_id              BIGINT,
    cash_counter_id           BIGINT       NOT NULL DEFAULT 1,
    prescriber_doctor_id      BIGINT,
    is_self_company           BOOLEAN      NOT NULL DEFAULT TRUE,
    bill_no                   VARCHAR(20)  NOT NULL DEFAULT '000000000000000',
    is_interim                BOOLEAN      NOT NULL DEFAULT TRUE,
    is_settled                BOOLEAN      NOT NULL DEFAULT FALSE,
    is_cancelled              BOOLEAN      NOT NULL DEFAULT FALSE,

    total_amount              NUMERIC(19,2) NOT NULL DEFAULT 0,
    tax_amount                NUMERIC(19,2) NOT NULL DEFAULT 0,
    concession_amount         NUMERIC(19,2) NOT NULL DEFAULT 0,
    self_amount               NUMERIC(19,2) NOT NULL DEFAULT 0,
    non_self_amount           NUMERIC(19,2) NOT NULL DEFAULT 0,
    balance_self              NUMERIC(19,2) NOT NULL DEFAULT 0,
    balance_non_self          NUMERIC(19,2) NOT NULL DEFAULT 0,
    change_amount             NUMERIC(19,2) NOT NULL DEFAULT 0,
    finalized_at              TIMESTAMP(3),

    -- WP-S1 saga
    saga_source_order_id      BIGINT,
    saga_event_id             VARCHAR(64),

    CONSTRAINT pk_t_bill PRIMARY KEY (bill_id),
    CONSTRAINT ck_t_bill_bill_no_nonblank CHECK (length(btrim(bill_no)) > 0),
    CONSTRAINT ck_t_bill_money_nonneg CHECK (
        total_amount      >= 0 AND tax_amount   >= 0 AND concession_amount >= 0 AND
        self_amount       >= 0 AND non_self_amount >= 0 AND change_amount >= 0),
    CONSTRAINT ck_t_bill_ctx CHECK (NOT (encounter_id IS NOT NULL AND admission_id IS NOT NULL))
);
CREATE INDEX IF NOT EXISTS ix_t_bill_patient      ON ihms_billing.t_bill (patient_id);
CREATE INDEX IF NOT EXISTS ix_t_bill_encounter    ON ihms_billing.t_bill (encounter_id);
CREATE INDEX IF NOT EXISTS ix_t_bill_admission    ON ihms_billing.t_bill (admission_id);
CREATE INDEX IF NOT EXISTS ix_t_bill_bill_no      ON ihms_billing.t_bill (bill_no);
CREATE INDEX IF NOT EXISTS ix_t_bill_settled      ON ihms_billing.t_bill (is_settled);
CREATE INDEX IF NOT EXISTS ix_t_bill_cancelled    ON ihms_billing.t_bill (is_cancelled);
CREATE UNIQUE INDEX IF NOT EXISTS uq_t_bill_saga_event
    ON ihms_billing.t_bill (saga_event_id) WHERE saga_event_id IS NOT NULL;
COMMENT ON TABLE ihms_billing.t_bill IS 'Clean target of T_OPDBILL/T_IPDBILL header (MIG-040 BR-BILL-001/006). Created with bill_no=''000000000000000'' placeholder; finalize assigns OPDBILL-<seq> (no admissionId) or IPDBILL-<seq> (admissionId). is_settled <=> balance_self <= 0 AND balance_non_self <= 0. *BeforeFinalization snapshots live on t_charge; the bill header recomputes from charges at finalize. uq_t_bill_saga_event partial UNIQUE is the saga consumer race backstop (WP-S1).';
COMMENT ON COLUMN ihms_billing.t_bill.bill_no IS 'Placeholder 000000000000000 at create (BR-BILL-001); OPDBILL-<seq> (no admissionId) or IPDBILL-<seq> (admissionId) at finalize.';

-- ---------------------------------------------------------------------------
-- t_charge — clean target of T_BILLDETAILS service-line (BR-BILL-005)
-- Created TEMP (bill_id NULL) via POST /api/v1/charges; attached to a bill by
-- POST /api/v1/bills. Snapshot *BeforeFinalization columns filled at finalize
-- (BR-BILL-008 restore contract; cancellation itself stays monolith-side this tranche).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_billing.t_charge (
    charge_id                 BIGINT       NOT NULL,
    unit_id                   BIGINT,
    status                    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_unit_id           BIGINT,
    updated_unit_id           BIGINT,
    added_by                  BIGINT,
    added_datetime            TIMESTAMP(3),
    updated_by                BIGINT,
    updated_datetime          TIMESTAMP(3),
    synchronized              BOOLEAN      NOT NULL DEFAULT FALSE,

    bill_id                   BIGINT,
    service_id                BIGINT       NOT NULL,
    service_name              VARCHAR(200) NOT NULL,
    rate                      NUMERIC(19,2) NOT NULL,
    qty                       INTEGER      NOT NULL DEFAULT 1,

    tax_pct_a                 NUMERIC(5,2)  NOT NULL DEFAULT 0,
    tax_pct_b                 NUMERIC(5,2)  NOT NULL DEFAULT 0,
    tax_pct_c                 NUMERIC(5,2)  NOT NULL DEFAULT 0,
    apply_after_concession    BOOLEAN      NOT NULL DEFAULT FALSE,

    -- 0 => treated as 100% self (deductable rule, BR-BILL-005)
    co_pay_pct                NUMERIC(5,2)  NOT NULL DEFAULT 0,
    concession_plan_pct       NUMERIC(5,2)  NOT NULL DEFAULT 0,
    concession_bill_pct       NUMERIC(5,2)  NOT NULL DEFAULT 0,
    doctor_share_pct          NUMERIC(5,2)  NOT NULL DEFAULT 0,
    specialization            SMALLINT,
    is_ot_procedure           BOOLEAN      NOT NULL DEFAULT FALSE,

    -- BR-BILL-008 restore snapshots
    total_before_finalization NUMERIC(19,2),
    self_before_finalization  NUMERIC(19,2),
    nonself_before_finalization NUMERIC(19,2),
    is_cancelled              BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_t_charge PRIMARY KEY (charge_id),
    CONSTRAINT ck_t_charge_rate_nonneg CHECK (rate >= 0),
    CONSTRAINT ck_t_charge_qty_pos CHECK (qty > 0),
    CONSTRAINT ck_t_charge_pct_bounds CHECK (
        tax_pct_a  BETWEEN 0 AND 100 AND tax_pct_b  BETWEEN 0 AND 100 AND
        tax_pct_c  BETWEEN 0 AND 100 AND co_pay_pct BETWEEN 0 AND 100 AND
        concession_plan_pct BETWEEN 0 AND 100 AND
        concession_bill_pct BETWEEN 0 AND 100 AND
        doctor_share_pct BETWEEN 0 AND 100)
);
CREATE INDEX IF NOT EXISTS ix_t_charge_bill         ON ihms_billing.t_charge (bill_id);
CREATE INDEX IF NOT EXISTS ix_t_charge_service      ON ihms_billing.t_charge (service_id);
CREATE INDEX IF NOT EXISTS ix_t_charge_temp         ON ihms_billing.t_charge (bill_id) WHERE bill_id IS NULL;
COMMENT ON TABLE ihms_billing.t_charge IS 'Clean target of T_BILLDETAILS service-line (MIG-040 BR-BILL-005). Created TEMP (bill_id NULL) via POST /api/v1/charges; attached to a bill by POST /api/v1/bills. Snapshot *BeforeFinalization columns are filled at finalize for the BR-BILL-008 restore contract.';

-- ---------------------------------------------------------------------------
-- t_doctor_share — clean target of T_DOCTORSHARE (BR-BILL-002)
-- share = totalAmount * pct/100 CEILING scale 2; hospital = total - share HALF_UP
-- scale 2; is_billed=false at insert (DoctorShare accrual consumes E-07 later).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_billing.t_doctor_share (
    share_id                  BIGINT       NOT NULL,
    unit_id                   BIGINT,
    status                    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_unit_id           BIGINT,
    updated_unit_id           BIGINT,
    added_by                  BIGINT,
    added_datetime            TIMESTAMP(3),
    updated_by                BIGINT,
    updated_datetime          TIMESTAMP(3),
    synchronized              BOOLEAN      NOT NULL DEFAULT FALSE,

    bill_id                   BIGINT       NOT NULL,
    charge_id                 BIGINT       NOT NULL,
    doctor_id                 BIGINT       NOT NULL,
    share_amount              NUMERIC(19,2) NOT NULL,
    hospital_amount           NUMERIC(19,2) NOT NULL,
    is_billed                 BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_t_doctor_share PRIMARY KEY (share_id),
    CONSTRAINT ck_t_doctor_share_amounts_nonneg CHECK (share_amount >= 0 AND hospital_amount >= 0)
);
CREATE INDEX IF NOT EXISTS ix_t_doctor_share_bill    ON ihms_billing.t_doctor_share (bill_id);
CREATE INDEX IF NOT EXISTS ix_t_doctor_share_doctor  ON ihms_billing.t_doctor_share (doctor_id);
CREATE INDEX IF NOT EXISTS ix_t_doctor_share_charge  ON ihms_billing.t_doctor_share (charge_id);
COMMENT ON TABLE ihms_billing.t_doctor_share IS 'Clean target of T_DOCTORSHARE (BR-BILL-002). share = totalAmount * pct/100 CEILING scale 2; hospital = total - share HALF_UP scale 2. is_billed=false until downstream accrual consumes the row.';

-- ---------------------------------------------------------------------------
-- t_advance_ledger — patient advance balance + FIFO consumption (BR-BILL-007)
-- One row per advance receipt or per advance consumption against a bill.
-- amount > 0 for RECEIPT (credit), amount < 0 for CONSUMPTION (debit) against
-- a bill. balance_after is the patient running balance after the entry. FIFO
-- consumption reads all unconsumed RECEIPT rows ordered by advance_id ASC.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_billing.t_advance_ledger (
    advance_id                BIGINT       NOT NULL,
    unit_id                   BIGINT,
    status                    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_unit_id           BIGINT,
    updated_unit_id           BIGINT,
    added_by                  BIGINT,
    added_datetime            TIMESTAMP(3),
    updated_by                BIGINT,
    updated_datetime          TIMESTAMP(3),
    synchronized              BOOLEAN      NOT NULL DEFAULT FALSE,

    patient_id                BIGINT       NOT NULL,
    bill_id                   BIGINT,                      -- populated on CONSUMPTION
    entry_type                VARCHAR(20)  NOT NULL,        -- 'RECEIPT' or 'CONSUMPTION'
    amount                    NUMERIC(19,2) NOT NULL,        -- signed: + credit, - debit
    balance_after             NUMERIC(19,2) NOT NULL,
    receipt_no                VARCHAR(64),
    consumed_advance_id       BIGINT,                       -- on consumption: source advance
    mode_code                 VARCHAR(20),
    remarks                   VARCHAR(500),

    CONSTRAINT pk_t_advance_ledger PRIMARY KEY (advance_id),
    CONSTRAINT ck_t_advance_type CHECK (entry_type IN ('RECEIPT', 'CONSUMPTION'))
);
CREATE INDEX IF NOT EXISTS ix_t_advance_patient      ON ihms_billing.t_advance_ledger (patient_id);
CREATE INDEX IF NOT EXISTS ix_t_advance_bill         ON ihms_billing.t_advance_ledger (bill_id);
CREATE INDEX IF NOT EXISTS ix_t_advance_fifo         ON ihms_billing.t_advance_ledger (patient_id, advance_id) WHERE entry_type = 'RECEIPT';
COMMENT ON TABLE ihms_billing.t_advance_ledger IS 'Patient advance balance + FIFO consumption (BR-BILL-007). RECEIPT (amount>0) credits the patient running balance; CONSUMPTION (amount<0, bill_id NOT NULL) debits it. FIFO order is advance_id ASC. balance_after is the running total AFTER the entry — for the patient row''s last RECEIPT/CONSUMPTION this is the current advance balance.';

-- ---------------------------------------------------------------------------
-- t_receipt — payment receipt (BR-BILL-006 settlement)
-- One row per payment attempt. transactionType=2 in legacy (BR-BILL-006).
-- receipt_no from sequence; balance_self/balance_non_self after this receipt
-- is RECOMPUTED at apply time (idempotent replay per BR-BILL-006).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_billing.t_receipt (
    receipt_id                BIGINT       NOT NULL,
    unit_id                   BIGINT,
    status                    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_unit_id           BIGINT,
    updated_unit_id           BIGINT,
    added_by                  BIGINT,
    added_datetime            TIMESTAMP(3),
    updated_by                BIGINT,
    updated_datetime          TIMESTAMP(3),
    synchronized              BOOLEAN      NOT NULL DEFAULT FALSE,

    bill_id                   BIGINT       NOT NULL,
    patient_id                BIGINT       NOT NULL,
    receipt_no                VARCHAR(64)  NOT NULL,
    amount                    NUMERIC(19,2) NOT NULL,        -- self portion
    non_self_amount           NUMERIC(19,2) NOT NULL DEFAULT 0, -- non-self portion
    mode_code                 VARCHAR(20)  NOT NULL,        -- 'CASH','CARD','CHEQUE','ONLINE','COMPANY','ADVANCE'
    bank_id                   BIGINT,
    bank_ref_no               VARCHAR(64),
    transaction_type          SMALLINT     NOT NULL DEFAULT 2, -- legacy: 2=receipt
    cash_counter_id           BIGINT       NOT NULL DEFAULT 1,
    change_amount             NUMERIC(19,2) NOT NULL DEFAULT 0,
    payment_datetime          TIMESTAMP(3) NOT NULL,

    CONSTRAINT pk_t_receipt PRIMARY KEY (receipt_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_t_receipt_no ON ihms_billing.t_receipt (receipt_no);
CREATE INDEX IF NOT EXISTS ix_t_receipt_bill         ON ihms_billing.t_receipt (bill_id);
CREATE INDEX IF NOT EXISTS ix_t_receipt_patient      ON ihms_billing.t_receipt (patient_id);
CREATE INDEX IF NOT EXISTS ix_t_receipt_mode         ON ihms_billing.t_receipt (mode_code);
COMMENT ON TABLE ihms_billing.t_receipt IS 'Payment receipt (MIG-041 BR-BILL-006). transactionType=2 (legacy convention). One row per payment attempt. Self vs non-self portion split kept; mode_code drives cash-mode rule (mode>1 needs bank ref). balance_self/balance_non_self are NOT stored here — they are RECOMPUTED on t_bill at apply time, which makes the call idempotent (BR-BILL-006 settled <=> both balances <= 0).';

-- ---------------------------------------------------------------------------
-- t_refund — refund / cancellation (BR-BILL-008)
-- One row per refund event. transactionType=3 advance refund, transactionType=4
-- bill cancel-refund. References the source receipt (if applicable) and the
-- bill. restores the patient's advance balance (if mode=ADVANCE).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_billing.t_refund (
    refund_id                 BIGINT       NOT NULL,
    unit_id                   BIGINT,
    status                    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_unit_id           BIGINT,
    updated_unit_id           BIGINT,
    added_by                  BIGINT,
    added_datetime            TIMESTAMP(3),
    updated_by                BIGINT,
    updated_datetime          TIMESTAMP(3),
    synchronized              BOOLEAN      NOT NULL DEFAULT FALSE,

    bill_id                   BIGINT       NOT NULL,
    patient_id                BIGINT       NOT NULL,
    receipt_id                BIGINT,                       -- source receipt (if any)
    transaction_type          SMALLINT     NOT NULL,         -- 3=advance, 4=bill cancel
    amount                    NUMERIC(19,2) NOT NULL,
    mode_code                 VARCHAR(20)  NOT NULL,        -- 'CASH','CARD','ADVANCE'
    reason                    VARCHAR(500),
    refund_datetime           TIMESTAMP(3) NOT NULL,

    CONSTRAINT pk_t_refund PRIMARY KEY (refund_id),
    CONSTRAINT ck_t_refund_tx_type CHECK (transaction_type IN (3, 4))
);
CREATE INDEX IF NOT EXISTS ix_t_refund_bill         ON ihms_billing.t_refund (bill_id);
CREATE INDEX IF NOT EXISTS ix_t_refund_patient      ON ihms_billing.t_refund (patient_id);
CREATE INDEX IF NOT EXISTS ix_t_refund_receipt      ON ihms_billing.t_refund (receipt_id);
COMMENT ON TABLE ihms_billing.t_refund IS 'Refund / cancellation ledger (MIG-041 BR-BILL-008). transactionType=3 advance refund; transactionType=4 bill-cancellation refund. References source bill + optional source receipt. The settlement-leg restore is the inverse of the original consumption (CONSUMPTION -> RECEIPT pair on t_advance_ledger).';

-- ---------------------------------------------------------------------------
-- t_consumed_event — WP-S1 saga consumer dedupe ledger
-- event_id (VARCHAR(64) UNIQUE) is the durable dedupe key. status APPLIED ->
-- a real bill DRAFT exists; REJECTED -> payload validation failed; the
-- relay maps that rejection to ordering-service BILLING_FAILED.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_billing.t_consumed_event (
    consumed_id               BIGINT       NOT NULL,
    event_id                  VARCHAR(64)  NOT NULL,
    event_type                VARCHAR(100) NOT NULL,
    source_order_id           BIGINT,
    status                    VARCHAR(20)  NOT NULL DEFAULT 'APPLIED',
    rejection_reason          VARCHAR(500),
    ingested_at               TIMESTAMP(3) NOT NULL,
    added_by                  BIGINT,
    added_datetime            TIMESTAMP(3),

    CONSTRAINT pk_t_consumed_event PRIMARY KEY (consumed_id),
    CONSTRAINT uq_t_consumed_event_event_id UNIQUE (event_id)
);
CREATE INDEX IF NOT EXISTS ix_t_consumed_event_type    ON ihms_billing.t_consumed_event (event_type);
CREATE INDEX IF NOT EXISTS ix_t_consumed_event_order   ON ihms_billing.t_consumed_event (source_order_id);
COMMENT ON TABLE ihms_billing.t_consumed_event IS 'WP-S1 saga consumer dedupe ledger (idempotency). event_id UNIQUE backstops the Kafka listener race; status APPLIED -> a real bill DRAFT exists, REJECTED -> payload validation failed and the relay maps it to ordering-service BILLING_FAILED compensation.';

-- ---------------------------------------------------------------------------
-- outbox — transactional outbox (IMP-DEC-004, KAFKA-EVENT-CATALOG envelope)
-- event_id UNIQUE — consumer dedupe boundary (AGT-10 F-9 / IMP-DEC-024).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_billing.outbox (
    id                       BIGSERIAL    PRIMARY KEY,
    event_id                 UUID         NOT NULL,
    event_type               VARCHAR(100) NOT NULL,
    version                  VARCHAR(16)  NOT NULL DEFAULT 'v1',
    aggregate_type           VARCHAR(100) NOT NULL,
    aggregate_id             BIGINT       NOT NULL,
    partition_key            VARCHAR(100) NOT NULL,
    payload                  JSONB        NOT NULL,
    idempotency_key          VARCHAR(255),
    producer_service         VARCHAR(100) NOT NULL,
    occurred_at              TIMESTAMP(3) NOT NULL,
    status                   VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempts                 INTEGER      NOT NULL DEFAULT 0,
    published_at             TIMESTAMP(3),
    created_at               TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_outbox_event_id UNIQUE (event_id)
);
CREATE INDEX IF NOT EXISTS idx_outbox_dispatch
    ON ihms_billing.outbox (status, created_at) WHERE status = 'PENDING';
COMMENT ON TABLE ihms_billing.outbox IS 'Transactional outbox: business tx writes InvoiceGenerated/BillSettled/ConcessionApproved/PaymentReceived here atomically; KafkaOutboxRelay publishes and marks PUBLISHED. event_id UNIQUE is the consumer dedupe boundary (AGT-10 F-9).';
