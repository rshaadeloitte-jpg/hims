-- ============================================================================
-- V1 — ihms_grievance schema (Grievance & Feedback service, S20) — MIG-020
-- Clean target of the legacy grievance cluster (FEATURE-MATRIX M22):
--   M_GRIEVANCECATEGORYMASTER, M_GRIEVANCESUBCATEGORYMASTER,
--   M_GRIEVANCESEVERITYMASTER, T_PATIENTGRIEVANCE,
--   T_PATIENTGRIEVANCECOMMENTS, T_PATIENTGRIEVANCEFEEDBACK, T_GRIEVANCEDOCUMENTS.
-- ONE-TABLE-ONE-OWNER (§56): ALL tables here belong to the grievance service.
-- All migrated tables keep explicit sequences (IMP-DEC-003).
-- REST-only slice: GrievanceRaised is REJECTED phase 1 (KAFKA-EVENT-CATALOG
-- line 206 — domain-internal, keep REST until a second consumer appears), so
-- NO outbox table here; it lands only when a cross-service consumer emerges.
-- GrievanceSeverityTemplateDetails (role->severity routing) depends on the
-- auth/user domain (T_USERDETAILS) and is deferred to a follow-up slice.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS ihms_grievance;

SET search_path TO ihms_grievance, public;

-- ---------------------------------------------------------------------------
-- Classification masters
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_grievance.grievance_category (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(64), description VARCHAR(255) NOT NULL,
    is_citizen BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_grievance_category PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_grievance_category_status ON ihms_grievance.grievance_category (status);

CREATE TABLE IF NOT EXISTS ihms_grievance.grievance_sub_category (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    category_id BIGINT NOT NULL REFERENCES ihms_grievance.grievance_category (id),
    code VARCHAR(64), description VARCHAR(255) NOT NULL,
    CONSTRAINT pk_grievance_sub_category PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_grievance_sub_category_cat ON ihms_grievance.grievance_sub_category (category_id);

CREATE TABLE IF NOT EXISTS ihms_grievance.grievance_severity (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    code VARCHAR(64), description VARCHAR(255) NOT NULL,
    CONSTRAINT pk_grievance_severity PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_grievance_severity_status ON ihms_grievance.grievance_severity (status);

-- ---------------------------------------------------------------------------
-- grievance_ticket — clean target of T_PATIENTGRIEVANCE (the ticket aggregate).
-- Core lifecycle columns typed; IT-support long-tail flags in attributes_json (D-4).
-- issue_status: 1=NEW 2=ASSIGNED 3=RESOLVED 4=ASSIGN_TO_CREATOR
-- ticket_close_status: 1=PENDING 2=COMPLETED 3=CLOSED
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_grievance.grievance_ticket (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    ticket_no VARCHAR(64),
    issue_date TIMESTAMP(3),
    mrno VARCHAR(64),
    name VARCHAR(255),
    address VARCHAR(1000),
    mobile_no BIGINT,
    category_id BIGINT REFERENCES ihms_grievance.grievance_category (id),
    sub_category_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    issue TEXT,
    issue_status SMALLINT NOT NULL DEFAULT 1 CHECK (issue_status BETWEEN 1 AND 4),
    resolved_date TIMESTAMP(3),
    ticket_close_status SMALLINT CHECK (ticket_close_status BETWEEN 1 AND 3),
    feedback_id BIGINT,
    severity_id BIGINT NOT NULL DEFAULT 0,
    assigned_user_id BIGINT NOT NULL DEFAULT 0,
    attached_file_name VARCHAR(255),
    expected_resolution_date TIMESTAMP(3),
    user_sso_id VARCHAR(128),
    issue_type VARCHAR(64),
    reopened_datetime TIMESTAMP(3),
    reopened_by BIGINT,
    is_citizen BOOLEAN NOT NULL DEFAULT FALSE,
    hospital_ticket VARCHAR(64),
    service_request_type VARCHAR(64),
    attributes_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT pk_grievance_ticket PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_grievance_ticket_status ON ihms_grievance.grievance_ticket (status, issue_status);
CREATE INDEX IF NOT EXISTS ix_grievance_ticket_mobile ON ihms_grievance.grievance_ticket (mobile_no);
CREATE INDEX IF NOT EXISTS ix_grievance_ticket_unit ON ihms_grievance.grievance_ticket (unit_id, status);
CREATE INDEX IF NOT EXISTS ix_grievance_ticket_assigned ON ihms_grievance.grievance_ticket (assigned_user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_grievance_ticket_no ON ihms_grievance.grievance_ticket (ticket_no) WHERE ticket_no IS NOT NULL;
COMMENT ON TABLE ihms_grievance.grievance_ticket IS 'Clean target of T_PATIENTGRIEVANCE. IT-support long-tail (machine_name/model, connectivity, cable/hardware/integration/vendor/tech-manual/exe flags, other_reason) lives in attributes_json until MIG-001 value census (D-4).';

-- ---------------------------------------------------------------------------
-- grievance_comment — T_PATIENTGRIEVANCECOMMENTS (child of ticket)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_grievance.grievance_comment (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    grievance_id BIGINT NOT NULL REFERENCES ihms_grievance.grievance_ticket (id),
    comment TEXT,
    user_name VARCHAR(255),
    CONSTRAINT pk_grievance_comment PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_grievance_comment_ticket ON ihms_grievance.grievance_comment (grievance_id, id DESC);

-- ---------------------------------------------------------------------------
-- grievance_feedback — T_PATIENTGRIEVANCEFEEDBACK (5-question satisfaction + free text)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_grievance.grievance_feedback (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    grievance_id BIGINT NOT NULL REFERENCES ihms_grievance.grievance_ticket (id),
    question1 SMALLINT, question2 SMALLINT, question3 SMALLINT, question4 SMALLINT, question5 SMALLINT,
    free_text_feedback TEXT,
    email VARCHAR(255),
    CONSTRAINT pk_grievance_feedback PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_grievance_feedback_ticket ON ihms_grievance.grievance_feedback (grievance_id);

-- ---------------------------------------------------------------------------
-- grievance_document — T_GRIEVANCEDOCUMENTS (file metadata; blob storage is MIG-021)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_grievance.grievance_document (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    grievance_id BIGINT NOT NULL REFERENCES ihms_grievance.grievance_ticket (id),
    user_id BIGINT,
    file_name VARCHAR(255),
    CONSTRAINT pk_grievance_document PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_grievance_document_ticket ON ihms_grievance.grievance_document (grievance_id);

-- ---------------------------------------------------------------------------
-- Sequences for migrated tables (IMP-DEC-003). grievance_ticket_seq replaces the
-- legacy T_GRIEVENCE_TICKET_SEQ used for the IHMS/G/yyyy/%05d ticket number.
-- ---------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS ihms_grievance.grievance_category_seq     START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_grievance.grievance_sub_category_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_grievance.grievance_severity_seq     START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_grievance.grievance_ticket_seq       START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_grievance.grievance_ticket_no_seq    START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_grievance.grievance_comment_seq      START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_grievance.grievance_feedback_seq     START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_grievance.grievance_document_seq     START WITH 1;
