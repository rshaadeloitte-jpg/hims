-- ============================================================================
-- V3 — M_UNITMASTER parity columns (MIG-001 evidence / AGT-10 red team 2026-08-17)
-- Legacy Unitmaster.java:200-207 maps three persisted @Column fields that the
-- original V1 baseline (2026-08-14) omitted:
--   ISCONFIRMATORY   -> isConfirmatory   (Boolean, no default in legacy)
--   ISADMINISTRATIVE -> isAdministrative (Boolean, no default in legacy)
--   ISOFFLINE        -> isOffline        (Boolean, no default in legacy)
-- V1 has already been applied to dev/test databases, so this amendment is a new
-- migration instead of rewriting V1 (Flyway checksum safety; IMP-DEC-013 note).
-- Nullable BOOLEAN matches legacy (values absent until backfill; MIG-001 gate).
-- ============================================================================

SET search_path TO ihms_master, public;

ALTER TABLE "M_UNITMASTER" ADD COLUMN "ISCONFIRMATORY"   BOOLEAN;
ALTER TABLE "M_UNITMASTER" ADD COLUMN "ISADMINISTRATIVE" BOOLEAN;
ALTER TABLE "M_UNITMASTER" ADD COLUMN "ISOFFLINE"        BOOLEAN;

COMMENT ON COLUMN "M_UNITMASTER"."ISCONFIRMATORY"   IS 'Legacy Unitmaster.isConfirmatory @Column(ISCONFIRMATORY) — parity column added V3 (was missing from V1 baseline).';
COMMENT ON COLUMN "M_UNITMASTER"."ISADMINISTRATIVE" IS 'Legacy Unitmaster.isAdministrative @Column(ISADMINISTRATIVE) — parity column added V3.';
COMMENT ON COLUMN "M_UNITMASTER"."ISOFFLINE"        IS 'Legacy Unitmaster.isOffline @Column(ISOFFLINE) — parity column added V3.';
