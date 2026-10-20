-- ============================================================================
-- V2 — M11 CSSD: quality test fields on sterilization cycle + dispatch/receive
-- on kit issue.
-- ============================================================================
SET search_path TO ihms_cssd, public;

ALTER TABLE ihms_cssd.t_sterilization_cycle
    ADD COLUMN IF NOT EXISTS biological_indicator VARCHAR(20);
ALTER TABLE ihms_cssd.t_sterilization_cycle
    ADD COLUMN IF NOT EXISTS chemical_indicator VARCHAR(20);
COMMENT ON COLUMN ihms_cssd.t_sterilization_cycle.biological_indicator IS 'M11 CSSD quality test: BI result (PASS/FAIL) recorded at complete().';
COMMENT ON COLUMN ihms_cssd.t_sterilization_cycle.chemical_indicator IS 'M11 CSSD quality test: CI result (PASS/FAIL) recorded at complete().';

ALTER TABLE ihms_cssd.t_kit_issue
    ADD COLUMN IF NOT EXISTS dispatched_to_store_id BIGINT;
ALTER TABLE ihms_cssd.t_kit_issue
    ADD COLUMN IF NOT EXISTS dispatched_at TIMESTAMP(3);
ALTER TABLE ihms_cssd.t_kit_issue
    ADD COLUMN IF NOT EXISTS received_at TIMESTAMP(3);
ALTER TABLE ihms_cssd.t_kit_issue
    ADD COLUMN IF NOT EXISTS received_by VARCHAR(64);
COMMENT ON COLUMN ihms_cssd.t_kit_issue.dispatched_to_store_id IS 'M11 CSSD store-dispatch destination (separate from to_store_id which is the issue target).';
COMMENT ON COLUMN ihms_cssd.t_kit_issue.dispatched_at IS 'M11 CSSD dispatch timestamp.';
COMMENT ON COLUMN ihms_cssd.t_kit_issue.received_at IS 'M11 CSSD store-receive timestamp.';
COMMENT ON COLUMN ihms_cssd.t_kit_issue.received_by IS 'M11 CSSD store-receive receiver.';
