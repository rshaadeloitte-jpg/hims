-- ============================================================================
-- V2 — M32 RCM: DM (District Magistrate) approval, discharge verification, Chiranjeevi.
-- Status dictionaries (locked at this version):
--   t_dm_approval.doc_status:        1=PENDING 2=APPROVED 3=REJECTED
--   t_discharge_verification.status: 1=PENDING 2=VERIFIED 3=REJECTED
--   t_chiranjeevi_case.status:       1=NEW 2=APPROVED 3=REJECTED 4=CLOSED
-- ============================================================================
SET search_path TO ihms_rcm, public;

CREATE TABLE IF NOT EXISTS ihms_rcm.t_dm_approval (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    approval_no VARCHAR(64) NOT NULL,
    patient_id BIGINT NOT NULL,
    admission_id BIGINT,
    scheme VARCHAR(64) NOT NULL,
    requested_amount NUMERIC(19,2) NOT NULL CHECK (requested_amount > 0),
    approved_amount NUMERIC(19,2),
    dm_user_id BIGINT,
    dm_remarks VARCHAR(500),
    requested_at TIMESTAMP(3) NOT NULL,
    decided_at TIMESTAMP(3),
    doc_status SMALLINT NOT NULL DEFAULT 1 CHECK (doc_status BETWEEN 1 AND 3),
    CONSTRAINT pk_t_dm_approval PRIMARY KEY (id),
    CONSTRAINT uq_t_dm_approval_no UNIQUE (approval_no)
);
CREATE INDEX IF NOT EXISTS ix_t_dm_approval_patient ON ihms_rcm.t_dm_approval (patient_id);
CREATE INDEX IF NOT EXISTS ix_t_dm_approval_status ON ihms_rcm.t_dm_approval (doc_status);
COMMENT ON TABLE ihms_rcm.t_dm_approval IS 'M32 DM (District Magistrate) approval (MOIC upload + RGHS TID flow).';

CREATE TABLE IF NOT EXISTS ihms_rcm.t_discharge_verification (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    admission_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    verifier_user_id BIGINT,
    verified_at TIMESTAMP(3),
    bill_settled BOOLEAN NOT NULL DEFAULT FALSE,
    clearance_done BOOLEAN NOT NULL DEFAULT FALSE,
    remarks VARCHAR(500),
    doc_status SMALLINT NOT NULL DEFAULT 1 CHECK (doc_status BETWEEN 1 AND 3),
    CONSTRAINT pk_t_discharge_verification PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_t_discharge_verification_admission ON ihms_rcm.t_discharge_verification (admission_id);
CREATE INDEX IF NOT EXISTS ix_t_discharge_verification_status ON ihms_rcm.t_discharge_verification (doc_status);
COMMENT ON TABLE ihms_rcm.t_discharge_verification IS 'M32 discharge verification: financial clearance + clinical clearance before final discharge.';

CREATE TABLE IF NOT EXISTS ihms_rcm.t_chiranjeevi_case (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    case_no VARCHAR(64) NOT NULL,
    patient_id BIGINT NOT NULL,
    chiranjeevi_id VARCHAR(64) NOT NULL,
    package_code VARCHAR(64) NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    doc_status SMALLINT NOT NULL DEFAULT 1 CHECK (doc_status BETWEEN 1 AND 4),
    requested_at TIMESTAMP(3) NOT NULL,
    decided_at TIMESTAMP(3),
    closed_at TIMESTAMP(3),
    remarks VARCHAR(500),
    CONSTRAINT pk_t_chiranjeevi_case PRIMARY KEY (id),
    CONSTRAINT uq_t_chiranjeevi_case_no UNIQUE (case_no)
);
CREATE INDEX IF NOT EXISTS ix_t_chiranjeevi_case_patient ON ihms_rcm.t_chiranjeevi_case (patient_id);
CREATE INDEX IF NOT EXISTS ix_t_chiranjeevi_case_status ON ihms_rcm.t_chiranjeevi_case (doc_status);
COMMENT ON TABLE ihms_rcm.t_chiranjeevi_case IS 'M32 Chiranjeevi (Rajasthan state scheme) case lifecycle.';

CREATE SEQUENCE IF NOT EXISTS ihms_rcm.t_dm_approval_seq           START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_rcm.t_discharge_verification_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_rcm.t_chiranjeevi_case_seq       START WITH 1;
