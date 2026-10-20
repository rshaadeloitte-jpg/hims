-- ============================================================================
-- V2 — ihms_nursing: Care Plans, Shift Handover, Nurse Allocation, Incident (M09 gaps)
-- Ported from legacy NursingServiceImpl care plan + SBAR handover + allocation
-- matrix + incident reporting patterns.
-- ============================================================================

SET search_path TO ihms_nursing, public;

-- ---------------------------------------------------------------------------
-- t_care_plan — inpatient care plan (NANDA-I style)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_nursing.t_care_plan (
    care_plan_id BIGINT NOT NULL,
    unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    admission_id BIGINT NOT NULL,
    nursing_diagnosis VARCHAR(500) NOT NULL,
    goal VARCHAR(500),
    interventions JSONB NOT NULL DEFAULT '[]'::jsonb,
    outcomes JSONB NOT NULL DEFAULT '[]'::jsonb,
    status_code SMALLINT NOT NULL DEFAULT 1,
    CONSTRAINT pk_t_care_plan PRIMARY KEY (care_plan_id),
    CONSTRAINT ck_t_care_plan_status CHECK (status_code IN (1,2,3))
);
CREATE INDEX IF NOT EXISTS ix_t_care_plan_admission ON ihms_nursing.t_care_plan (admission_id);
COMMENT ON TABLE ihms_nursing.t_care_plan IS 'Inpatient care plan (M09 gap). status_code 1=ACTIVE 2=RESOLVED 3=CANCELLED.';

-- ---------------------------------------------------------------------------
-- t_shift_handover — SBAR handover between shifts
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_nursing.t_shift_handover (
    handover_id BIGINT NOT NULL,
    unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    admission_id BIGINT NOT NULL,
    ward_id BIGINT,
    shift VARCHAR(20) NOT NULL,
    situation TEXT,
    background TEXT,
    assessment TEXT,
    recommendation TEXT,
    pending_tasks TEXT,
    handed_over_by BIGINT NOT NULL,
    received_by BIGINT,
    signed_at TIMESTAMP(3),
    CONSTRAINT pk_t_shift_handover PRIMARY KEY (handover_id)
);
CREATE INDEX IF NOT EXISTS ix_t_shift_handover_admission ON ihms_nursing.t_shift_handover (admission_id);
CREATE INDEX IF NOT EXISTS ix_t_shift_handover_ward ON ihms_nursing.t_shift_handover (ward_id);
COMMENT ON TABLE ihms_nursing.t_shift_handover IS 'SBAR shift handover (M09 gap). shift=MORNING/EVENING/NIGHT. signed_at set when receiving nurse signs.';

-- ---------------------------------------------------------------------------
-- t_nurse_allocation — ward nurse-to-patient allocation
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_nursing.t_nurse_allocation (
    allocation_id BIGINT NOT NULL,
    unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    ward_id BIGINT NOT NULL,
    nurse_id BIGINT NOT NULL,
    admission_id BIGINT NOT NULL,
    shift VARCHAR(20),
    allocated_at TIMESTAMP(3) NOT NULL DEFAULT NOW(),
    released_at TIMESTAMP(3),
    CONSTRAINT pk_t_nurse_allocation PRIMARY KEY (allocation_id)
);
CREATE INDEX IF NOT EXISTS ix_t_nurse_allocation_ward ON ihms_nursing.t_nurse_allocation (ward_id);
CREATE INDEX IF NOT EXISTS ix_t_nurse_allocation_nurse ON ihms_nursing.t_nurse_allocation (nurse_id);
CREATE INDEX IF NOT EXISTS ix_t_nurse_allocation_admission ON ihms_nursing.t_nurse_allocation (admission_id);
COMMENT ON TABLE ihms_nursing.t_nurse_allocation IS 'Nurse-to-patient allocation matrix (M09 gap). One active row per nurse-patient pair; released_at set on de-allocation.';

-- ---------------------------------------------------------------------------
-- t_incident_report — clinical incident reporting
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_nursing.t_incident_report (
    incident_id BIGINT NOT NULL,
    unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    admission_id BIGINT,
    patient_id BIGINT NOT NULL,
    incident_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'LOW',
    description TEXT NOT NULL,
    location VARCHAR(200),
    occurred_at TIMESTAMP(3) NOT NULL,
    reported_by BIGINT NOT NULL,
    review_status SMALLINT NOT NULL DEFAULT 1,
    reviewer_id BIGINT,
    reviewed_at TIMESTAMP(3),
    corrective_action TEXT,
    CONSTRAINT pk_t_incident_report PRIMARY KEY (incident_id),
    CONSTRAINT ck_t_incident_review CHECK (review_status IN (1,2,3))
);
CREATE INDEX IF NOT EXISTS ix_t_incident_patient ON ihms_nursing.t_incident_report (patient_id);
CREATE INDEX IF NOT EXISTS ix_t_incident_admission ON ihms_nursing.t_incident_report (admission_id);
COMMENT ON TABLE ihms_nursing.t_incident_report IS 'Clinical incident reporting (M09 gap). review_status 1=PENDING_REVIEW 2=REVIEWED 3=CLOSED.';

-- Sequences
CREATE SEQUENCE IF NOT EXISTS ihms_nursing.t_care_plan_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_nursing.t_shift_handover_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_nursing.t_nurse_allocation_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_nursing.t_incident_report_seq START WITH 1;
