-- ============================================================================
-- V1 — ihms_iam schema (Security & IAM service, Catalog #22) — Wave 0
-- Clean target of the legacy auth/user cluster:
--   T_USERDETAILS -> t_userdetails, T_USERROLEMASTER -> t_userrolemaster,
--   T_USERROLES  -> t_userroles.
-- ONE-TABLE-ONE-OWNER: ALL tables here belong to the IAM service.
-- Session tokens live in Redis / in-memory (Catalog #22) — NOT in PG.
-- Audit columns mirror sibling-service migration conventions
-- (added_by/added_datetime/updated_by/updated_datetime, unit_id, status).
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS ihms_iam;

SET search_path TO ihms_iam, public;

-- ---------------------------------------------------------------------------
-- t_userdetails — clean target of T_USERDETAILS (login subject store).
-- password_hash is a BCrypt hash; legacy plaintext/Encrypter values are NOT
-- migrated (secret remediation per Catalog #22 security target).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_iam.t_userdetails (
    id BIGSERIAL,
    unit_id BIGINT,
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    full_name VARCHAR(255),
    employee_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    CONSTRAINT pk_userdetails PRIMARY KEY (id),
    CONSTRAINT uq_userdetails_username UNIQUE (username)
);
CREATE INDEX IF NOT EXISTS ix_userdetails_status ON ihms_iam.t_userdetails (status);
CREATE INDEX IF NOT EXISTS ix_userdetails_unit ON ihms_iam.t_userdetails (unit_id);

-- ---------------------------------------------------------------------------
-- t_userrolemaster — clean target of T_USERROLEMASTER (role catalogue).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_iam.t_userrolemaster (
    id BIGSERIAL,
    unit_id BIGINT,
    code VARCHAR(64) NOT NULL,
    description VARCHAR(255),
    status BOOLEAN NOT NULL DEFAULT TRUE,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    CONSTRAINT pk_userrolemaster PRIMARY KEY (id),
    CONSTRAINT uq_userrolemaster_code UNIQUE (code)
);
CREATE INDEX IF NOT EXISTS ix_userrolemaster_status ON ihms_iam.t_userrolemaster (status);

-- ---------------------------------------------------------------------------
-- t_userroles — clean target of T_USERROLES (user<->role grants, per-unit aware).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_iam.t_userroles (
    id BIGSERIAL,
    unit_id BIGINT,
    user_id BIGINT NOT NULL REFERENCES ihms_iam.t_userdetails (id),
    role_id BIGINT NOT NULL REFERENCES ihms_iam.t_userrolemaster (id),
    status BOOLEAN NOT NULL DEFAULT TRUE,
    added_by BIGINT,
    added_datetime TIMESTAMP(3),
    updated_by BIGINT,
    updated_datetime TIMESTAMP(3),
    CONSTRAINT pk_userroles PRIMARY KEY (id),
    CONSTRAINT uq_userroles_user_role UNIQUE (user_id, role_id)
);
CREATE INDEX IF NOT EXISTS ix_userroles_user ON ihms_iam.t_userroles (user_id);
CREATE INDEX IF NOT EXISTS ix_userroles_role ON ihms_iam.t_userroles (role_id);

-- ---------------------------------------------------------------------------
-- Seed baseline roles (admin/staff/patient — same names as legacy defaults).
-- Bootstrap users are created via POST /api/v1/users (BCrypt-hashed passwords).
-- ---------------------------------------------------------------------------
INSERT INTO ihms_iam.t_userrolemaster (code, description, status, added_by, added_datetime)
VALUES ('admin', 'Administrative operations', TRUE, 1, NOW()),
       ('staff', 'Staff/provider access', TRUE, 1, NOW()),
       ('patient', 'Patient identity', TRUE, 1, NOW())
ON CONFLICT (code) DO NOTHING;
