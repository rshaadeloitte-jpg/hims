-- ============================================================================
-- V6 — Concession columns missing from live schema (entity/schema parity)
-- Adds t_bill.concession_authorised_by and t_charge.flat_concession_amount,
-- which the JPA entities map but the deployed schema lacks.
-- ============================================================================

SET search_path TO ihms_billing, public;

-- t_bill.concession_authorised_by — user id that authorised the concession
ALTER TABLE ihms_billing.t_bill
    ADD COLUMN IF NOT EXISTS concession_authorised_by BIGINT;

-- t_charge.flat_concession_amount — fixed concession amount per charge line
ALTER TABLE ihms_billing.t_charge
    ADD COLUMN IF NOT EXISTS flat_concession_amount NUMERIC(12,2);

COMMENT ON COLUMN ihms_billing.t_bill.concession_authorised_by IS 'User id that authorised the concession on this bill. NULL when no concession authorised.';
COMMENT ON COLUMN ihms_billing.t_charge.flat_concession_amount IS 'Fixed concession amount applied to this charge line (entity precision 12 scale 2). NULL when none.';