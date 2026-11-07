-- ============================================================================
-- V3 — Blood transfusion safety: cross-match result record (legacy BloodCrossmatchingResult)
-- ============================================================================
SET search_path TO ihms_bloodbank, public;

CREATE TABLE IF NOT EXISTS ihms_bloodbank.t_bloodcrossmatchingresult (
    id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT,
    updated_unit_id BIGINT,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    requisition_id BIGINT NOT NULL REFERENCES ihms_bloodbank.t_bloodrequisition (requisition_id),
    requisition_detail_id BIGINT REFERENCES ihms_bloodbank.t_bloodrequisitiondetail (detail_id),
    bag_id BIGINT NOT NULL REFERENCES ihms_bloodbank.t_bloodbag (bag_id),
    patient_id BIGINT NOT NULL,
    method VARCHAR(100),
    result VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    test_date TIMESTAMP(3),
    tested_by VARCHAR(100),
    remarks VARCHAR(500),
    isdecrossed BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_t_bloodcrossmatchingresult PRIMARY KEY (id),
    CONSTRAINT ck_t_bloodcrossmatchingresult_result CHECK (result IN ('PENDING', 'COMPATIBLE', 'INCOMPATIBLE'))
);

CREATE INDEX IF NOT EXISTS ix_t_bloodcrossmatchingresult_req
    ON ihms_bloodbank.t_bloodcrossmatchingresult (requisition_id);
CREATE INDEX IF NOT EXISTS ix_t_bloodcrossmatchingresult_bag
    ON ihms_bloodbank.t_bloodcrossmatchingresult (bag_id);
CREATE INDEX IF NOT EXISTS ix_t_bloodcrossmatchingresult_patient
    ON ihms_bloodbank.t_bloodcrossmatchingresult (patient_id);
CREATE INDEX IF NOT EXISTS ix_t_bloodcrossmatchingresult_lookup
    ON ihms_bloodbank.t_bloodcrossmatchingresult (bag_id, requisition_id, patient_id, result, isdecrossed);

COMMENT ON TABLE ihms_bloodbank.t_bloodcrossmatchingresult IS 'Cross-match result record ported from legacy BloodCrossmatchingResult. Tracks compatibility testing lifecycle (PENDING -> COMPATIBLE | INCOMPATIBLE).';

CREATE SEQUENCE IF NOT EXISTS ihms_bloodbank.t_bloodcrossmatchingresult_seq START WITH 1;
