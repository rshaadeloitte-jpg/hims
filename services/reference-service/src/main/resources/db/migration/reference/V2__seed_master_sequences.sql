-- ============================================================================
-- V2 — Sequence seeding strategy (IMP-DEC-003)
-- Legacy ID continuity: explicit sequences per table, seeded to max(id)+1.
-- On a fresh PG schema the tables are empty -> sequences stay at 1.
-- For migrated data (post backfill) setval bumps to max(id)+1; re-running is
-- idempotent (MAX() of an unchanged table). Initial LAST_NUMBER capture from
-- live Oracle is a MIG-001 gate (BLK-001) — this script is the mechanism.
-- ============================================================================

SET search_path TO ihms_master, public;

DO $$
DECLARE m BIGINT;
BEGIN
    SELECT COALESCE(MAX("ID"), 0) INTO m FROM "M_UNITMASTER";
    IF m > 0 THEN PERFORM setval('"M_UNITMASTER_SEQ"', m); END IF;

    SELECT COALESCE(MAX("ID"), 0) INTO m FROM "M_DEPARTMENTMASTER";
    IF m > 0 THEN PERFORM setval('"M_DEPARTMENTMASTER_SEQ"', m); END IF;

    SELECT COALESCE(MAX("ID"), 0) INTO m FROM "M_COMPANYMASTER";
    IF m > 0 THEN PERFORM setval('"M_COMPANYMASTER_SEQ"', m); END IF;
END $$;
