-- ============================================================================
-- V8 — WHO ICD-11 MMS master catalog + master import log
-- ============================================================================
SET search_path TO ihms_master, public;

CREATE TABLE IF NOT EXISTS ihms_master.icd11_mms (
    id BIGSERIAL PRIMARY KEY,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT,
    updated_unit_id BIGINT,
    added_by BIGINT,
    added_datetime TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    foundation_uri VARCHAR(500),
    linearization_uri VARCHAR(500) NOT NULL,
    code VARCHAR(32),
    block_id VARCHAR(64),
    title TEXT NOT NULL,
    class_kind VARCHAR(32) NOT NULL,
    depth_in_kind INTEGER,
    is_residual BOOLEAN NOT NULL DEFAULT FALSE,
    chapter_no VARCHAR(16),
    is_leaf BOOLEAN NOT NULL DEFAULT FALSE,
    parent_linearization_uri VARCHAR(500),
    release_version VARCHAR(64) NOT NULL,
    CONSTRAINT uq_icd11_mms_linearization_uri UNIQUE (linearization_uri)
);

CREATE INDEX IF NOT EXISTS ix_icd11_mms_code ON ihms_master.icd11_mms (code);
CREATE INDEX IF NOT EXISTS ix_icd11_mms_lower_title ON ihms_master.icd11_mms (lower(title));

COMMENT ON TABLE ihms_master.icd11_mms IS 'WHO ICD-11 MMS catalog: chapters, blocks, categories; licensed CC BY-ND 3.0 IGO.';

CREATE TABLE IF NOT EXISTS ihms_master.master_import_log (
    id BIGSERIAL PRIMARY KEY,
    dataset VARCHAR(64) NOT NULL,
    source_url VARCHAR(500),
    release_version VARCHAR(64),
    file_sha256 VARCHAR(64),
    rows_read INTEGER NOT NULL DEFAULT 0,
    rows_inserted INTEGER NOT NULL DEFAULT 0,
    rows_updated INTEGER NOT NULL DEFAULT 0,
    started_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP(3),
    status VARCHAR(32) NOT NULL,
    message TEXT
);

CREATE INDEX IF NOT EXISTS ix_master_import_log_dataset ON ihms_master.master_import_log (dataset, started_at);
COMMENT ON TABLE ihms_master.master_import_log IS 'Master data import tracking log (icd11, countries, testdata).';
