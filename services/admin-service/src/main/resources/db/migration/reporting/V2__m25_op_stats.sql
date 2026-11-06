-- ============================================================================
-- V2 — M25 reports: add rm_op_stats to support OT/Procedure summary reports.
-- Mirror of rm_encounter_stats keyed on (stat_date, op_type).
-- ============================================================================
SET search_path TO ihms_reporting, public;

CREATE TABLE IF NOT EXISTS ihms_reporting.rm_op_stats (
    stat_date DATE NOT NULL,
    op_type VARCHAR(40) NOT NULL,
    cnt INT NOT NULL DEFAULT 0,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    CONSTRAINT pk_rm_op_stats PRIMARY KEY (stat_date, op_type),
    CONSTRAINT ck_rm_op_stats_cnt CHECK (cnt >= 0)
);
COMMENT ON TABLE ihms_reporting.rm_op_stats IS 'Read model fed by OtCompleted + BloodBagIssued + LabVerified via internal event ingress (M25 reporting slice).';
