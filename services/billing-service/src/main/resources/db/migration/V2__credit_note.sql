-- ============================================================================
-- V2 — Credit Note entity (M04 gap: credit notes)
-- Ported from old IHMS: GetCreditNoteDetailsByBillId, CreditNoteBillListForSearch.
-- A credit note is a negative adjustment against a finalized bill.
-- ============================================================================

SET search_path TO ihms_billing, public;

CREATE TABLE IF NOT EXISTS ihms_billing.t_credit_note (
    credit_note_id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,

    bill_id BIGINT NOT NULL REFERENCES ihms_billing.t_bill (bill_id),
    patient_id BIGINT NOT NULL,
    credit_note_no VARCHAR(32) NOT NULL,
    reason VARCHAR(500),
    amount NUMERIC(19,2) NOT NULL,
    is_applied BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_t_credit_note PRIMARY KEY (credit_note_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_credit_note_no ON ihms_billing.t_credit_note (credit_note_no);
CREATE INDEX IF NOT EXISTS ix_credit_note_bill ON ihms_billing.t_credit_note (bill_id);
CREATE INDEX IF NOT EXISTS ix_credit_note_patient ON ihms_billing.t_credit_note (patient_id);

CREATE SEQUENCE IF NOT EXISTS ihms_billing."t_credit_note_seq" START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_billing."t_credit_note_no_seq" START WITH 1 INCREMENT BY 1;

COMMENT ON TABLE ihms_billing.t_credit_note IS 'Credit notes issued against finalized bills (M04 gap). Ported from old IHMS credit note logic.';
