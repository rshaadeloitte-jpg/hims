SET search_path TO ihms_billing, public;

ALTER TABLE ihms_billing.t_bill_payment_ledger
    ADD COLUMN IF NOT EXISTS created_unit_id BIGINT,
    ADD COLUMN IF NOT EXISTS updated_unit_id BIGINT;

ALTER TABLE ihms_billing.t_credit_note
    ADD COLUMN IF NOT EXISTS created_unit_id BIGINT,
    ADD COLUMN IF NOT EXISTS updated_unit_id BIGINT;

ALTER TABLE ihms_billing.t_debit_note
    ADD COLUMN IF NOT EXISTS created_unit_id BIGINT,
    ADD COLUMN IF NOT EXISTS updated_unit_id BIGINT;
