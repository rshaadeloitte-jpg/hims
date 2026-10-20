-- ============================================================================
-- V2 — ihms_patient.patient_sponsor (WP-3, Catalog #1 sponsor endpoints)
-- Clean target of T_PATIENTSPONSOR: sponsor/scheme linkage rows owned by the
-- patient service (ONE-TABLE-ONE-OWNER §56). Follows V1 column conventions
-- (audit block added_by/added_datetime/updated_by/updated_datetime, explicit
-- sequence per IMP-DEC-003).
-- ============================================================================

CREATE TABLE IF NOT EXISTS ihms_patient.patient_sponsor (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,

    patient_id BIGINT NOT NULL REFERENCES ihms_patient.patient_registration (id),
    sponsor_type VARCHAR(64) NOT NULL,
    scheme_code VARCHAR(64),
    policy_no VARCHAR(64),
    valid_from DATE,
    valid_to DATE,

    CONSTRAINT pk_patient_sponsor PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS ix_patient_sponsor_patient ON ihms_patient.patient_sponsor (patient_id);

CREATE SEQUENCE IF NOT EXISTS ihms_patient."patient_sponsor_seq" START WITH 1 INCREMENT BY 1;

COMMENT ON TABLE ihms_patient.patient_sponsor IS 'Sponsor/scheme linkage (T_PATIENTSPONSOR clean target). Re-pointed to the surviving patient row by POST /api/v1/patients/merge.';
