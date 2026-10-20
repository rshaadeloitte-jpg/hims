-- ============================================================================
-- V2 — M16 Radiology: retest flag on RadOrderDetail (legacy RadRejectAndRetestDto).
-- doc_status values:
--   1=SCHEDULED 2=STARTED 3=IMAGE_CAPTURED 4=REPORTED 5=COMPLETED
--   6=RETEST_REQUESTED 7=RESCHEDULED 8=CANCELLED 9=REJECTED
-- ============================================================================
SET search_path TO ihms_radiology, public;

ALTER TABLE ihms_rad.t_radorder_detail
    ADD COLUMN IF NOT EXISTS retest_requested BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE ihms_rad.t_radorder_detail
    ADD COLUMN IF NOT EXISTS retest_reason VARCHAR(500);
ALTER TABLE ihms_rad.t_radorder_detail
    ADD COLUMN IF NOT EXISTS rescheduled_to TIMESTAMP(3);
COMMENT ON COLUMN ihms_rad.t_radorder_detail.retest_requested IS 'M16 retest flag (legacy RadRejectAndRetestDto.isRetest).';
COMMENT ON COLUMN ihms_rad.t_radorder_detail.retest_reason IS 'M16 retest reason.';
COMMENT ON COLUMN ihms_rad.t_radorder_detail.rescheduled_to IS 'M16 reschedule timestamp.';

-- Radiologist report template
CREATE TABLE IF NOT EXISTS ihms_rad.t_rad_template (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    template_code VARCHAR(64) NOT NULL,
    modality VARCHAR(40) NOT NULL,
    body_template TEXT NOT NULL,
    description VARCHAR(500),
    CONSTRAINT pk_t_rad_template PRIMARY KEY (id),
    CONSTRAINT uq_t_rad_template_code UNIQUE (template_code)
);
CREATE INDEX IF NOT EXISTS ix_t_rad_template_modality ON ihms_rad.t_rad_template (modality);
COMMENT ON TABLE ihms_rad.t_rad_template IS 'M16 radiologist report template (modality-specific body).';

CREATE SEQUENCE IF NOT EXISTS ihms_rad.t_rad_template_seq START WITH 1;
