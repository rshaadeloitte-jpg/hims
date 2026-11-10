-- ============================================================================
-- V1 — ihms_nursing schema (Nursing & Inpatient Care service, S12) — WP-6
-- Phase-1 slice (GAP-REGISTER-2 WP-6, MICROSERVICE-CATALOG #12): vitals
-- charting (T_PATIENTVITALS), nursing notes (T_NURSENOTE), nursing tasks
-- (T_NURSETASKDETAILS), plus the transactional outbox for the PROPOSED events
-- VitalsRecorded + NurseTaskCompleted (KAFKA-EVENT-CATALOG §3 — proposed,
-- no consumers yet; contracts may change before ratification).
--
-- ONE-TABLE-ONE-OWNER (§56): ALL tables here belong to the nursing service.
-- Conventions mirror sibling services (IMP-DEC-003 explicit sequences;
-- audit block added_by/added_datetime/updated_by/updated_datetime).
--
-- BOUNDARY NOTE (phase 1): patient_id / admission_id are stored WITHOUT foreign-key
-- constraints and are accepted as OPAQUE IDS by the API. Cross-service validation
-- against adt-service (admission exists / belongs to patient / not discharged)
-- is deferred to the inter-service phase.
--
-- Task lifecycle (t_nursetaskdetails.task_status SMALLINT — register's "status";
--   renamed to avoid the audit-block "status" BOOLEAN collision):
--   1=PENDING 2=COMPLETED 3=CANCELLED; complete allowed ONLY 1->2 (service guard).
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS ihms_nursing;

SET search_path TO ihms_nursing, public;

-- ---------------------------------------------------------------------------
-- t_patientvitals — clean target of T_PATIENTVITALS
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_nursing.t_patientvitals (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    patient_id BIGINT NOT NULL,
    admission_id BIGINT,               -- opaque id in phase 1 (no FK by design)
    vitals_type VARCHAR(100) NOT NULL,
    value NUMERIC(10,2),
    unit VARCHAR(32),
    recorded_at TIMESTAMP(3) NOT NULL,
    recorded_by BIGINT,
    CONSTRAINT pk_t_patientvitals PRIMARY KEY (id),
    CONSTRAINT ck_t_patientvitals_value CHECK (value IS NULL OR value >= 0)
);
CREATE INDEX IF NOT EXISTS ix_t_patientvitals_patient_time
    ON ihms_nursing.t_patientvitals (patient_id, recorded_at);
CREATE INDEX IF NOT EXISTS ix_t_patientvitals_admission ON ihms_nursing.t_patientvitals (admission_id);
COMMENT ON TABLE ihms_nursing.t_patientvitals IS 'Clean target of T_PATIENTVITALS. One recorded vital observation per row. patient_id/admission_id are opaque ids in phase 1 (adt-service owns admissions). Recording emits PROPOSED event VitalsRecorded via the outbox.';

-- ---------------------------------------------------------------------------
-- t_nursenote — clean target of T_NURSENOTE
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_nursing.t_nursenote (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    admission_id BIGINT NOT NULL,      -- opaque id in phase 1 (no FK by design)
    note TEXT NOT NULL,
    nurse_id BIGINT,
    noted_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT pk_t_nursenote PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_t_nursenote_admission ON ihms_nursing.t_nursenote (admission_id, noted_at);
COMMENT ON TABLE ihms_nursing.t_nursenote IS 'Clean target of T_NURSENOTE. Free-text nursing note per admission; admission_id is an opaque id in phase 1. No event in phase 1.';

-- ---------------------------------------------------------------------------
-- t_nursetaskdetails — clean target of T_NURSETASKDETAILS
-- task_status SMALLINT: 1=PENDING 2=COMPLETED 3=CANCELLED (complete only 1->2).
-- NOTE: GAP-REGISTER-2 WP-6 calls this column "status"; it is materialized as
-- task_status because the shared audit block (sibling convention) already owns
-- the row-active BOOLEAN column "status" — same resolution as appointment-service
-- appt_status.
-- frequency semantics per 06-business-rules-catalog §A (OD/BD/TID/Q4H...)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_nursing.t_nursetaskdetails (
    id BIGINT NOT NULL, unit_id BIGINT, status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT, added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3), synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    admission_id BIGINT NOT NULL,      -- opaque id in phase 1 (no FK by design)
    task_desc VARCHAR(500) NOT NULL,
    frequency VARCHAR(32),
    due_at TIMESTAMP(3),
    task_status SMALLINT NOT NULL DEFAULT 1 CHECK (task_status BETWEEN 1 AND 3),
    completed_at TIMESTAMP(3),
    completed_by BIGINT,
    CONSTRAINT pk_t_nursetaskdetails PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS ix_t_nursetaskdetails_status ON ihms_nursing.t_nursetaskdetails (task_status, due_at);
CREATE INDEX IF NOT EXISTS ix_t_nursetaskdetails_admission ON ihms_nursing.t_nursetaskdetails (admission_id, task_status);
COMMENT ON TABLE ihms_nursing.t_nursetaskdetails IS 'Clean target of T_NURSETASKDETAILS. Lifecycle 1=PENDING 2=COMPLETED 3=CANCELLED; completion only from PENDING (service guard) and emits PROPOSED event NurseTaskCompleted via the outbox. admission_id is an opaque id in phase 1.';

-- ---------------------------------------------------------------------------
-- Transactional outbox (IMP-DEC-004; DDL mirrors sibling services incl.
-- event_id UNIQUE). Events (both PROPOSED per KAFKA-EVENT-CATALOG §3):
--   VitalsRecorded       (on vital create)     -> topic ihms.nursing.events, partition key patientId
--   NurseTaskCompleted   (on task completion)  -> topic ihms.nursing.events, partition key admissionId
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_nursing.outbox (
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
COMMENT ON TABLE ihms_nursing.outbox IS 'Transactional outbox: vitals-recording tx writes PROPOSED VitalsRecorded here atomically; task-completion tx writes PROPOSED NurseTaskCompleted. Relay publishes to ihms.nursing.events.';
CREATE INDEX idx_outbox_dispatch ON ihms_nursing.outbox (status, created_at) WHERE status = 'PENDING';

-- ---------------------------------------------------------------------------
-- Sequences for migrated tables (IMP-DEC-003)
-- ---------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS ihms_nursing.t_patientvitals_seq    START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_nursing.t_nursenote_seq        START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_nursing.t_nursetaskdetails_seq START WITH 1;
