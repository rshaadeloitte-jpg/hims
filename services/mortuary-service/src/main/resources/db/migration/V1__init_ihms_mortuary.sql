-- ============================================================================
-- V1 — ihms_mortuary schema (Mortuary & Death service, M28) — Wave-3 tranche 3
-- Clean target of the legacy mortuary cluster (MICROSERVICE-CATALOG #28):
-- dead body registry, autopsy requests, release certificates.
--
-- ONE-TABLE-ONE-OWNER (§56): ALL tables here belong to the mortuary service.
-- Conventions mirror sibling services (IMP-DEC-003 explicit sequences; audit
-- block added_by/added_datetime/updated_by/updated_datetime).
--
-- BR-M28-001 dead body status (VARCHAR): ADMITTED, RELEASED, AUTOPSY_PENDING,
--   AUTOPSY_COMPLETE, POLICE_CASE, HANDED_OVER
-- BR-M28-002 autopsy request status (SMALLINT): 1=PENDING, 2=APPROVED,
--   3=COMPLETED, 4=REJECTED
-- BR-M28-003 release certificate status (SMALLINT): 1=REQUESTED, 2=APPROVED,
--   3=RELEASED, 4=REJECTED
--
-- NOTE: NO outbox table yet — events DEFERRED per KAFKA-EVENT-CATALOG §3.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS ihms_mortuary;

SET search_path TO ihms_mortuary, public;

-- ---------------------------------------------------------------------------
-- t_dead_body — dead body registry
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_mortuary.t_dead_body (
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
    admission_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    patient_name VARCHAR(255) NOT NULL,
    death_date TIMESTAMP(3) NOT NULL,
    cause_of_death VARCHAR(500),
    death_location VARCHAR(255),
    next_of_kin VARCHAR(255),
    next_of_kin_relation VARCHAR(100),
    next_of_kin_phone VARCHAR(20),
    body_status VARCHAR(50) NOT NULL DEFAULT 'ADMITTED',
    chamber_no VARCHAR(50),
    death_cert_no VARCHAR(100),
    police_case BOOLEAN NOT NULL DEFAULT FALSE,
    mlc_no VARCHAR(100),
    remark VARCHAR(500),
    CONSTRAINT pk_t_dead_body PRIMARY KEY (id),
    CONSTRAINT ck_t_dead_body_status CHECK (body_status IN ('ADMITTED', 'RELEASED', 'AUTOPSY_PENDING', 'AUTOPSY_COMPLETE', 'POLICE_CASE', 'HANDED_OVER'))
);
CREATE INDEX IF NOT EXISTS ix_t_dead_body_admission ON ihms_mortuary.t_dead_body (admission_id);
CREATE INDEX IF NOT EXISTS ix_t_dead_body_patient ON ihms_mortuary.t_dead_body (patient_id);
CREATE INDEX IF NOT EXISTS ix_t_dead_body_status ON ihms_mortuary.t_dead_body (body_status);
CREATE INDEX IF NOT EXISTS ix_t_dead_body_death_date ON ihms_mortuary.t_dead_body (death_date);
COMMENT ON TABLE ihms_mortuary.t_dead_body IS 'Dead body registry (M28). body_status: ADMITTED, RELEASED, AUTOPSY_PENDING, AUTOPSY_COMPLETE, POLICE_CASE, HANDED_OVER. admission_id links to adt-service t_admission.';
COMMENT ON COLUMN ihms_mortuary.t_dead_body.admission_id IS 'FK to adt-service t_admission.admission_id (death linkage).';

-- ---------------------------------------------------------------------------
-- t_autopsy_request — autopsy request tracking
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_mortuary.t_autopsy_request (
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
    dead_body_id BIGINT NOT NULL REFERENCES ihms_mortuary.t_dead_body (id),
    request_date TIMESTAMP(3) NOT NULL,
    requested_by BIGINT NOT NULL,
    request_status SMALLINT NOT NULL DEFAULT 1,
    reason VARCHAR(500),
    doctor_id BIGINT,
    autopsy_date TIMESTAMP(3),
    findings TEXT,
    rejection_reason VARCHAR(500),
    CONSTRAINT pk_t_autopsy_request PRIMARY KEY (id),
    CONSTRAINT ck_t_autopsy_request_status CHECK (request_status IN (1, 2, 3, 4))
);
CREATE INDEX IF NOT EXISTS ix_t_autopsy_request_dead_body ON ihms_mortuary.t_autopsy_request (dead_body_id);
CREATE INDEX IF NOT EXISTS ix_t_autopsy_request_status ON ihms_mortuary.t_autopsy_request (request_status);
COMMENT ON TABLE ihms_mortuary.t_autopsy_request IS 'Autopsy request tracking (M28). request_status: 1=PENDING, 2=APPROVED, 3=COMPLETED, 4=REJECTED.';

-- ---------------------------------------------------------------------------
-- t_release_certificate — body release certificate tracking
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_mortuary.t_release_certificate (
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
    dead_body_id BIGINT NOT NULL REFERENCES ihms_mortuary.t_dead_body (id),
    release_no VARCHAR(64) NOT NULL,
    request_date TIMESTAMP(3) NOT NULL,
    requested_by BIGINT NOT NULL,
    release_status SMALLINT NOT NULL DEFAULT 1,
    handover_to VARCHAR(255),
    handover_relation VARCHAR(100),
    handover_phone VARCHAR(20),
    handover_date TIMESTAMP(3),
    remark VARCHAR(500),
    rejection_reason VARCHAR(500),
    CONSTRAINT pk_t_release_certificate PRIMARY KEY (id),
    CONSTRAINT uq_t_release_certificate_no UNIQUE (release_no),
    CONSTRAINT ck_t_release_certificate_status CHECK (release_status IN (1, 2, 3, 4))
);
CREATE INDEX IF NOT EXISTS ix_t_release_certificate_dead_body ON ihms_mortuary.t_release_certificate (dead_body_id);
CREATE INDEX IF NOT EXISTS ix_t_release_certificate_status ON ihms_mortuary.t_release_certificate (release_status);
COMMENT ON TABLE ihms_mortuary.t_release_certificate IS 'Body release certificate tracking (M28). release_status: 1=REQUESTED, 2=APPROVED, 3=RELEASED, 4=REJECTED.';

-- ---------------------------------------------------------------------------
-- Sequences (IMP-DEC-003 explicit server-owned sequences)
-- ---------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS ihms_mortuary.t_dead_body_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_mortuary.t_autopsy_request_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_mortuary.t_release_certificate_seq START WITH 1;
