-- ============================================================================
-- V3 — MLC capture, photo reference, mother-child linking (M01 gaps)
-- Ported from old IHMS: mlcCase/mlcType fields (PatDetails.java:32-33),
-- photo capture, and parent-child relationship tracking.
-- ============================================================================

SET search_path TO ihms_patient, public;

-- ---------------------------------------------------------------------------
-- Photo reference on patient_registration (M01: photo capture gap)
-- ---------------------------------------------------------------------------
ALTER TABLE ihms_patient.patient_registration
    ADD COLUMN IF NOT EXISTS photo_ref VARCHAR(500);

COMMENT ON COLUMN ihms_patient.patient_registration.photo_ref IS 'Object store key or URL for patient photograph (M01 gap: photo capture)';

-- ---------------------------------------------------------------------------
-- Medico-Legal Case (MLC) records — ported from old IHMS PatDetails.mlcCase/
-- mlcType. In legacy, MLC was stored at encounter level; here we model it as
-- a separate entity linked to patient for pre-registration MLC flagging.
-- Full encounter-level MLC will be handled by appointment/adt services.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_patient.patient_mlc (
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

    patient_id BIGINT NOT NULL REFERENCES ihms_patient.patient_registration (id),
    mlc_case VARCHAR(64) NOT NULL,
    mlc_type VARCHAR(64),
    mlc_number VARCHAR(64),
    police_station VARCHAR(200),
    fir_number VARCHAR(64),
    reported_date_time TIMESTAMP(3),
    remarks TEXT,

    CONSTRAINT pk_patient_mlc PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS ix_patient_mlc_patient ON ihms_patient.patient_mlc (patient_id);
CREATE INDEX IF NOT EXISTS ix_patient_mlc_case ON ihms_patient.patient_mlc (mlc_case);

CREATE SEQUENCE IF NOT EXISTS ihms_patient."patient_mlc_seq" START WITH 1 INCREMENT BY 1;

COMMENT ON TABLE ihms_patient.patient_mlc IS 'Medico-Legal Case records linked to patient (M01 gap: MLC capture). Ported from old IHMS PatDetails.mlcCase/mlcType.';

-- ---------------------------------------------------------------------------
-- Patient relations (M01: mother-child linking gap)
-- Supports parent-child, spouse, and other family linkages.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_patient.patient_relation (
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

    patient_id BIGINT NOT NULL REFERENCES ihms_patient.patient_registration (id),
    related_patient_id BIGINT NOT NULL REFERENCES ihms_patient.patient_registration (id),
    relation_type VARCHAR(32) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_patient_relation PRIMARY KEY (id),
    CONSTRAINT uq_patient_relation UNIQUE (patient_id, related_patient_id, relation_type)
);

CREATE INDEX IF NOT EXISTS ix_patient_relation_patient ON ihms_patient.patient_relation (patient_id);
CREATE INDEX IF NOT EXISTS ix_patient_relation_related ON ihms_patient.patient_relation (related_patient_id);

CREATE SEQUENCE IF NOT EXISTS ihms_patient."patient_relation_seq" START WITH 1 INCREMENT BY 1;

COMMENT ON TABLE ihms_patient.patient_relation IS 'Patient-to-patient relationships: MOTHER, CHILD, SPOUSE, FATHER, GUARDIAN (M01 gap: mother-child linking).';