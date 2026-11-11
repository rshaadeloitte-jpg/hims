-- ============================================================================
-- V2 — M15 Lab/Pathology workflow (GAP-REGISTER WP-7 HIGH priority)
-- Ported from legacy PathoOrderServiceImpl:
--  - Sample barcode generation (generateBarCode / BarCodeGenerator)
--  - Accept/reject workflow (CIMS_SampleAccept / sampleRejected / RejectResultDto)
--  - Double verification release (KDL verify + verify step)
--  - Critical alerts (notifyPatientDetails + isCriticalValue on result value vs range)
--
-- ponytail: single migration adds all M15 columns; per-column splitting is YAGNI
-- until a downstream service joins on a specific column.
-- ============================================================================

SET search_path TO ihms_lab, public;

-- ---------------------------------------------------------------------------
-- t_pathorderdetail — sample workflow
-- legacy columns: SampleNoStr, Barcode?, IsAccepted, IsRejected, AcceptanceDate,
-- RejectionDate, RejectionReason, IsSampleCollected, IsFinalized, IsResultEntry,
-- isVerified (KDL), priority, isCritical (header), acceptanceReason
-- ---------------------------------------------------------------------------
ALTER TABLE ihms_lab.t_pathorderdetail
    ADD COLUMN IF NOT EXISTS sample_no      VARCHAR(50),
    ADD COLUMN IF NOT EXISTS barcode        VARCHAR(100),
    ADD COLUMN IF NOT EXISTS is_accepted   BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_rejected   BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS acceptance_date TIMESTAMP(3),
    ADD COLUMN IF NOT EXISTS rejection_date  TIMESTAMP(3),
    ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(500),
    ADD COLUMN IF NOT EXISTS acceptance_reason VARCHAR(500),
    ADD COLUMN IF NOT EXISTS is_verified    BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS verified_at    TIMESTAMP(3),
    ADD COLUMN IF NOT EXISTS verified_by    BIGINT;

-- barcode should be unique when present (one barcode per sample), but nullable
CREATE UNIQUE INDEX IF NOT EXISTS uq_t_pathorderdetail_barcode
    ON ihms_lab.t_pathorderdetail (barcode) WHERE barcode IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_t_pathorderdetail_sample
    ON ihms_lab.t_pathorderdetail (sample_no) WHERE sample_no IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_t_pathorderdetail_accepted
    ON ihms_lab.t_pathorderdetail (is_accepted) WHERE is_accepted = TRUE;
CREATE INDEX IF NOT EXISTS ix_t_pathorderdetail_verified
    ON ihms_lab.t_pathorderdetail (is_verified) WHERE is_verified = FALSE;

COMMENT ON COLUMN ihms_lab.t_pathorderdetail.sample_no IS 'Ported from T_PathOrderBookingDetails.SampleNoStr: sample identifier for accept/reject grouping. Generated at barcode time.';
COMMENT ON COLUMN ihms_lab.t_pathorderdetail.barcode IS 'Sample barcode string (LAB-{yy}/{seq}-style or generated via sequence). Unique when set.';
COMMENT ON COLUMN ihms_lab.t_pathorderdetail.is_accepted IS 'Ported from IsAccepted (CIMS_SampleAccept): true once sampleAccepted() flips IsAccepted + AcceptanceDate.';
COMMENT ON COLUMN ihms_lab.t_pathorderdetail.is_rejected IS 'Ported from IsRejected (sampleRejected): true once rejected with reason.';
COMMENT ON COLUMN ihms_lab.t_pathorderdetail.is_verified IS 'Double-verification flag (KDL verify / verify step): true only after finalize + verify by second checker.';

-- ---------------------------------------------------------------------------
-- t_pathopatientparameterdetails — critical flag
-- legacy: p.setCriticalValue(true) when result < min or > max of reference_range
-- ---------------------------------------------------------------------------
ALTER TABLE ihms_lab.t_pathopatientparameterdetails
    ADD COLUMN IF NOT EXISTS is_critical          BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS critical_notified_at TIMESTAMP(3);

CREATE INDEX IF NOT EXISTS ix_param_is_critical
    ON ihms_lab.t_pathopatientparameterdetails (is_critical) WHERE is_critical = TRUE;

COMMENT ON COLUMN ihms_lab.t_pathopatientparameterdetails.is_critical IS 'True when numeric value outside reference_range min-max (legacy lines 6506-6511: double resultNum < rangeMin || > rangeMax). Set on upsertResults.';

-- ---------------------------------------------------------------------------
-- t_pathorderbooking — header critical marker (any detail critical)
-- ---------------------------------------------------------------------------
ALTER TABLE ihms_lab.t_pathorderbooking
    ADD COLUMN IF NOT EXISTS is_critical BOOLEAN NOT NULL DEFAULT FALSE;

-- ---------------------------------------------------------------------------
-- barcode sequence (IMP-DEC-003: explicit sequences, not identity)
-- ponytail: single global barcode seq; per-unit seq is later if needed
-- ---------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS ihms_lab.lab_barcode_seq START WITH 1;
