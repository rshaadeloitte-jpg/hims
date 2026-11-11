-- ============================================================================
-- V4 — One sale per prescription (pharmacy dispensing)
-- Adds a partial unique index so a second concurrent create for the same
-- prescription is rejected at the DB level. Live table has 0 rows.
-- ============================================================================

SET search_path TO ihms_pharmacy, public;

CREATE UNIQUE INDEX IF NOT EXISTS uq_t_itemsale_prescription ON ihms_pharmacy.t_itemsale (prescription_id) WHERE prescription_id IS NOT NULL;
