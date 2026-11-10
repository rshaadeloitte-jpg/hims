-- ============================================================================
-- V7 — M39 Commons/Platform: app-level key/value config + generic audit log.
-- ============================================================================
SET search_path TO ihms_master, public;

CREATE TABLE IF NOT EXISTS ihms_master.app_config (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    config_key VARCHAR(120) NOT NULL,
    config_value TEXT,
    description VARCHAR(500),
    CONSTRAINT pk_app_config PRIMARY KEY (id),
    CONSTRAINT uq_app_config_unit_key UNIQUE (unit_id, config_key)
);
CREATE INDEX IF NOT EXISTS ix_app_config_key ON ihms_master.app_config (config_key);
COMMENT ON TABLE ihms_master.app_config IS 'M39 app config: per-unit key/value configuration values.';

CREATE TABLE IF NOT EXISTS ihms_master.audit_log (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    entity_type VARCHAR(64) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(16) NOT NULL CHECK (action IN ('CREATE','UPDATE','DELETE','LOGIN','LOGOUT','READ')),
    actor_id BIGINT,
    actor_username VARCHAR(120),
    diff JSONB,
    occurred_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_audit_log PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_audit_log_entity ON ihms_master.audit_log (entity_type, entity_id, occurred_at);
CREATE INDEX IF NOT EXISTS ix_audit_log_actor ON ihms_master.audit_log (actor_id, occurred_at);
COMMENT ON TABLE ihms_master.audit_log IS 'M39 audit trail: append-only generic event log (entity_type, entity_id, action, actor, diff JSONB).';

CREATE SEQUENCE IF NOT EXISTS ihms_master.app_config_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_master.audit_log_seq START WITH 1;
