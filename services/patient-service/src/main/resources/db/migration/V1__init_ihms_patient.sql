-- ============================================================================
-- V1 — ihms_patient schema (Patient Identity & Registration, S01) — MIG-030
-- Wave 3 Clinical Spine KICKOFF slice: T_REGISTRATION clean target (demographic
-- subset), patient document linkage, transactional outbox for E-01
-- PatientRegistered / PatientUpdated (KAFKA-EVENT-CATALOG §2 E-01).
--
-- ONE-TABLE-ONE-OWNER (§56): ALL tables here belong to the patient service.
-- Migrated table keeps an explicit sequence (IMP-DEC-003); outbox uses
-- BIGSERIAL identity like reference-service (new internal ledger table,
-- IMP-DEC-026 convention) with event_id UNIQUE folded in from day one
-- (reference needed a V4 retrofit — AGT-10 F-9).
--
-- Deferred to follow-up slices (tracked in DECISIONS IMP-DEC-039):
--   family/spouse/child tables, sponsor/scheme linkage (RGHS/Chiranjeevi),
--   temp-MRN series + promotion flow (BR-PAT-001 variants), Solr sync consumer.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS ihms_patient;

SET search_path TO ihms_patient, public;

-- ---------------------------------------------------------------------------
-- Patient registration — clean target of T_REGISTRATION (kickoff demographic
-- subset; legacy ~120 native-SQL sites per UNKNOWN-016 stay on the monolith
-- until encounter/admission slices take ownership)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_patient.patient_registration (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,

    mrno VARCHAR(32) NOT NULL,
    is_temp_mrno BOOLEAN NOT NULL DEFAULT FALSE,

    first_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    last_name VARCHAR(100),

    gender VARCHAR(16) NOT NULL,
    dob DATE,
    blood_group VARCHAR(8),

    mobile_no VARCHAR(20),
    email VARCHAR(255),

    aadhaar_no VARCHAR(12),
    vid VARCHAR(32),
    janaadhaar_id VARCHAR(32),
    health_id VARCHAR(64),
    health_id_number VARCHAR(64),

    address_line1 VARCHAR(255),
    city VARCHAR(100),
    district VARCHAR(100),
    state VARCHAR(100),
    pincode VARCHAR(10),

    emergency_contact_name VARCHAR(100),
    emergency_contact_phone VARCHAR(20),

    place_of_birth VARCHAR(100),

    CONSTRAINT pk_patient_registration PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_patient_mrno ON ihms_patient.patient_registration (mrno);
CREATE INDEX IF NOT EXISTS ix_patient_mobile ON ihms_patient.patient_registration (mobile_no);
CREATE INDEX IF NOT EXISTS ix_patient_aadhaar ON ihms_patient.patient_registration (aadhaar_no);
CREATE INDEX IF NOT EXISTS ix_patient_janaadhaar ON ihms_patient.patient_registration (janaadhaar_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_patient_health_id_number ON ihms_patient.patient_registration (health_id_number)
    WHERE health_id_number IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_patient_name_dob ON ihms_patient.patient_registration (first_name, last_name, dob);

-- ---------------------------------------------------------------------------
-- Patient documents — kickoff "document linkage" (metadata only; blob storage
-- is MIG-021 MinIO). doc_ref points into the future object store or legacy NAS.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_patient.patient_document (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,

    patient_id BIGINT NOT NULL REFERENCES ihms_patient.patient_registration (id),
    doc_type VARCHAR(64) NOT NULL,
    doc_ref VARCHAR(255) NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_patient_document PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_patient_document_patient ON ihms_patient.patient_document (patient_id);

-- ---------------------------------------------------------------------------
-- Transactional outbox (IMP-DEC-004; envelope per KAFKA-EVENT-CATALOG; DDL
-- mirrors reference-service V1+V4 incl. event_id UNIQUE)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_patient.outbox (
    id               BIGSERIAL    PRIMARY KEY,
    event_id         UUID         NOT NULL,
    event_type       VARCHAR(100) NOT NULL,
    version          VARCHAR(16)  NOT NULL DEFAULT 'v1',
    aggregate_type   VARCHAR(100) NOT NULL,
    aggregate_id     BIGINT       NOT NULL,
    partition_key    VARCHAR(100) NOT NULL,
    payload          JSONB        NOT NULL,
    idempotency_key  VARCHAR(255),
    producer_service VARCHAR(100) NOT NULL,
    occurred_at      TIMESTAMP(3) NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempts         INTEGER      NOT NULL DEFAULT 0,
    published_at     TIMESTAMP(3),
    created_at       TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_outbox_event_id UNIQUE (event_id)
);
COMMENT ON TABLE ihms_patient.outbox IS 'Transactional outbox: registration tx writes PatientRegistered/PatientUpdated here atomically; relay publishes to ihms.patient.registration.';
CREATE INDEX idx_outbox_dispatch ON ihms_patient.outbox (status, created_at) WHERE status = 'PENDING';

-- ---------------------------------------------------------------------------
-- Sequence (BR-PAT-001: permanent MRN = ddMMYY + patientId padded to 9;
-- patientId comes from this sequence — atomic nextval inside the reg tx)
-- ---------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS ihms_patient."patient_reg_seq" START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS ihms_patient."patient_document_seq" START WITH 1 INCREMENT BY 1;
