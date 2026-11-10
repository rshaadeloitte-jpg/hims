-- ============================================================================
-- V3 — M38 ABHA: link ABHA health-id to a patient, manage consent, push PHR.
-- Status dictionaries (locked at this version):
--   t_abha_link.link_status:  1=PENDING_VERIFY 2=VERIFIED 3=DELINKED 4=REVOKED
--   t_abha_consent.consent_status: 1=GRANTED 2=REVOKED 3=EXPIRED
--   t_phr_push.status: 1=PENDING 2=PUSHED 3=FAILED
-- ============================================================================
SET search_path TO ihms_interop, public;

CREATE TABLE IF NOT EXISTS ihms_interop.t_abha_link (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    abha_address VARCHAR(64) NOT NULL,
    health_id_number VARCHAR(64),
    patient_id BIGINT,
    txn_id VARCHAR(64) NOT NULL,
    link_status SMALLINT NOT NULL DEFAULT 1 CHECK (link_status BETWEEN 1 AND 4),
    verified_at TIMESTAMP(3),
    delinked_at TIMESTAMP(3),
    CONSTRAINT pk_t_abha_link PRIMARY KEY (id),
    CONSTRAINT uq_t_abha_link_txn UNIQUE (txn_id)
);
CREATE INDEX IF NOT EXISTS ix_t_abha_link_patient ON ihms_interop.t_abha_link (patient_id);
CREATE INDEX IF NOT EXISTS ix_t_abha_link_status ON ihms_interop.t_abha_link (link_status);
CREATE INDEX IF NOT EXISTS ix_t_abha_link_health_id ON ihms_interop.t_abha_link (health_id_number);
COMMENT ON TABLE ihms_interop.t_abha_link IS 'M38 ABHA link: a verified PHR-Address / Health-ID bound to a patientId (or unverified on create).';

CREATE TABLE IF NOT EXISTS ihms_interop.t_abha_consent (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    abha_link_id BIGINT NOT NULL REFERENCES ihms_interop.t_abha_link (id),
    consent_request_id VARCHAR(64) NOT NULL,
    purpose VARCHAR(64) NOT NULL,
    hip_id VARCHAR(64) NOT NULL,
    hiu_id VARCHAR(64) NOT NULL,
    valid_from TIMESTAMP(3) NOT NULL,
    valid_to TIMESTAMP(3) NOT NULL,
    consent_status SMALLINT NOT NULL DEFAULT 1 CHECK (consent_status BETWEEN 1 AND 3),
    granted_at TIMESTAMP(3),
    revoked_at TIMESTAMP(3),
    CONSTRAINT pk_t_abha_consent PRIMARY KEY (id),
    CONSTRAINT uq_t_abha_consent_request UNIQUE (consent_request_id)
);
CREATE INDEX IF NOT EXISTS ix_t_abha_consent_link ON ihms_interop.t_abha_consent (abha_link_id);
COMMENT ON TABLE ihms_interop.t_abha_consent IS 'M38 ABHA consent artefact: grants HIU access to HIP data for a purpose+window.';

CREATE TABLE IF NOT EXISTS ihms_interop.t_phr_push (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    abha_link_id BIGINT NOT NULL REFERENCES ihms_interop.t_abha_link (id),
    payload_type VARCHAR(32) NOT NULL,
    payload JSONB NOT NULL,
    doc_status SMALLINT NOT NULL DEFAULT 1 CHECK (doc_status BETWEEN 1 AND 3),
    pushed_at TIMESTAMP(3),
    failure_reason VARCHAR(500),
    CONSTRAINT pk_t_phr_push PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_t_phr_push_link ON ihms_interop.t_phr_push (abha_link_id);
CREATE INDEX IF NOT EXISTS ix_t_phr_push_status ON ihms_interop.t_phr_push (doc_status);
COMMENT ON TABLE ihms_interop.t_phr_push IS 'M38 PHR push queue (patient health records). doc_status 1=PENDING 2=PUSHED 3=FAILED.';

CREATE SEQUENCE IF NOT EXISTS ihms_interop.t_abha_link_seq    START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_interop.t_abha_consent_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_interop.t_phr_push_seq     START WITH 1;
