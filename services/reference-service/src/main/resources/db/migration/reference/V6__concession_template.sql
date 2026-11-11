-- ============================================================================
-- V6 — M05 Tariff: concession template master. Used by billing concession
-- approval flow (header defines eligibility; lines define rules).
-- doc_status: 1=ACTIVE 2=INACTIVE
-- ============================================================================
SET search_path TO ihms_master, public;

CREATE TABLE IF NOT EXISTS ihms_master.concession_template (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(200) NOT NULL,
    scheme VARCHAR(64) NOT NULL,
    description VARCHAR(500),
    max_percent_off NUMERIC(5,2) NOT NULL CHECK (max_percent_off BETWEEN 0 AND 100),
    requires_approval BOOLEAN NOT NULL DEFAULT FALSE,
    doc_status SMALLINT NOT NULL DEFAULT 1 CHECK (doc_status BETWEEN 1 AND 2),
    CONSTRAINT pk_concession_template PRIMARY KEY (id),
    CONSTRAINT uq_concession_template_unit_code UNIQUE (unit_id, code)
);
CREATE INDEX IF NOT EXISTS ix_concession_template_scheme ON ihms_master.concession_template (scheme, doc_status);
COMMENT ON TABLE ihms_master.concession_template IS 'M05 concession template master: scheme + max discount + approval flag.';

CREATE SEQUENCE IF NOT EXISTS ihms_master.concession_template_seq START WITH 1;
