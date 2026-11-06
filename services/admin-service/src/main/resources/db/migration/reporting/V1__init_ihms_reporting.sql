-- ============================================================================
-- V1 — ihms_reporting schema (Reporting & Analytics service, Catalog #26)
--       WP-T2 (Wave-5 tranche 6, GAP-REGISTER-6)
-- Read models (rm_*) fed by an INTERNAL event-relay ingress — production would
-- be a Kafka consumer of PatientRegistered / EncounterCreated / InvoiceGenerated /
-- PaymentReceived (KAFKA-EVENT-CATALOG); the ingress API is the contract-stubbed
-- stand-in, disclosed at every site. Plus report master/execute engine tables
-- and the transactional outbox TABLE per schema convention.
--
-- ONE-TABLE-ONE-OWNER (§56): ALL tables here belong to the reporting service.
--
-- EVENT POLICY (register WP-T2): this service is a pure CONSUMER/aggregator —
-- it EMITS NOTHING. Nothing is ever written to the outbox; the integration test
-- asserts it stays empty through every flow.
--
-- IDEMPOTENCY: re-ingesting the SAME event must NOT double count (pinned by
-- tests). rm_patient_registry upserts by patient_id; accumulating read models
-- (rm_encounter_stats / rm_revenue_daily) dedupe via t_ingested_event keyed by
-- the source document id (encounterId / invoiceId / paymentId). The register's
-- five-table list has no ledger table, but strict double-ingest protection for
-- accumulating events requires one — ADDED TABLE, DISCLOSED in WP-T2-reporting.md.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS ihms_reporting;

SET search_path TO ihms_reporting, public;

-- ---------------------------------------------------------------------------
-- rm_patient_registry — one row per registered patient (upsert by patient_id).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_reporting.rm_patient_registry (
    patient_id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    mrn VARCHAR(30),
    name VARCHAR(120),
    registered_at TIMESTAMP(3),
    CONSTRAINT pk_rm_patient_registry PRIMARY KEY (patient_id)
);
COMMENT ON TABLE ihms_reporting.rm_patient_registry IS 'Read model fed by PatientRegistered via internal event ingress. Upsert by patient_id => re-ingest of the same event is naturally idempotent (row updated in place, never duplicated).';

-- ---------------------------------------------------------------------------
-- rm_encounter_stats — encounter counts per (stat_date, encounter_type).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_reporting.rm_encounter_stats (
    stat_date DATE NOT NULL,
    encounter_type VARCHAR(20) NOT NULL,
    cnt INT NOT NULL DEFAULT 0,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    CONSTRAINT pk_rm_encounter_stats PRIMARY KEY (stat_date, encounter_type),
    CONSTRAINT ck_rm_encounter_cnt CHECK (cnt >= 0)
);
COMMENT ON TABLE ihms_reporting.rm_encounter_stats IS 'Read model fed by EncounterCreated via internal event ingress. Incremented by 1 per NEW encounter only — duplicate ingestion is blocked by t_ingested_event (strict idempotency, pinned by tests).';

-- ---------------------------------------------------------------------------
-- rm_revenue_daily — revenue accumulation per (stat_date, head_code).
-- head_code defaults to 'OPD' when absent from the event payload.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_reporting.rm_revenue_daily (
    stat_date DATE NOT NULL,
    head_code VARCHAR(20) NOT NULL,
    total NUMERIC(19,2) NOT NULL DEFAULT 0,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    CONSTRAINT pk_rm_revenue_daily PRIMARY KEY (stat_date, head_code),
    CONSTRAINT ck_rm_revenue_total CHECK (total >= 0)
);
COMMENT ON TABLE ihms_reporting.rm_revenue_daily IS 'Read model fed by InvoiceGenerated + PaymentReceived via internal event ingress (amounts ACCUMULATE += per stat_date+head_code; headCode default OPD). Duplicate ingestion adds nothing — guarded by t_ingested_event (pinned by tests).';

-- ---------------------------------------------------------------------------
-- t_report_master — report definitions (TABULAR=1 / SUMMARY=2) over the rm_* read
-- models. definition JSONB: {table, columns[], dateField?} for TABULAR;
-- {table, aggregate: cnt|total} for SUMMARY. Jasper/Velocity rendering and
-- regulatory exports (GSTR/NHM/RGHS) are OUT OF SCOPE (comment-only per register).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_reporting.t_report_master (
    report_id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    report_code VARCHAR(40) NOT NULL,
    title VARCHAR(120) NOT NULL,
    kind SMALLINT NOT NULL,
    definition JSONB NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_t_report_master PRIMARY KEY (report_id),
    CONSTRAINT uq_report_code UNIQUE (report_code),
    CONSTRAINT ck_report_kind CHECK (kind IN (1, 2))
);
COMMENT ON TABLE ihms_reporting.t_report_master IS 'Report master definitions over the rm_* read models. kind SMALLINT: 1=TABULAR 2=SUMMARY. Execution result is stored JSON — a real report dispatcher/rendering engine (Jasper/Velocity) is OUT OF SCOPE per register WP-T2 (disclosed).';

-- ---------------------------------------------------------------------------
-- t_report_execution — one synchronous execution of a report definition.
-- status SMALLINT: 0=QUEUED 1=DONE 2=FAILED. This service executes inline
-- (dispatcher emulation — disclosed); rows are created DONE directly.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_reporting.t_report_execution (
    execution_id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    report_id BIGINT NOT NULL REFERENCES ihms_reporting.t_report_master (report_id),
    params JSONB,
    status SMALLINT NOT NULL DEFAULT 0,
    row_count INT,
    result JSONB,
    executed_at TIMESTAMP(3),
    CONSTRAINT pk_t_report_execution PRIMARY KEY (execution_id),
    CONSTRAINT ck_execution_status CHECK (status IN (0, 1, 2))
);
CREATE INDEX IF NOT EXISTS ix_execution_report ON ihms_reporting.t_report_execution (report_id);
COMMENT ON TABLE ihms_reporting.t_report_execution IS 'One execution of a report against the read models. Inline dispatcher emulation stores the JSONB result + row_count with status DONE (register: "report dispatcher emulated as JSON result" — OUT OF SCOPE disclosure).';

-- ---------------------------------------------------------------------------
-- t_ingested_event — ingest-idempotency ledger (ADDED TABLE vs register list —
-- DISCLOSED in WP-T2-reporting.md). UNIQUE(event_type, event_key) where
-- event_key is the source document id ('patient:<id>', 'encounter:<id>',
-- 'invoice:<id>', 'payment:<id>'). A re-ingested event short-circuits here so
-- accumulating read models can NEVER double count.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_reporting.t_ingested_event (
    ingested_id BIGINT NOT NULL,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    event_type VARCHAR(60) NOT NULL,
    event_key VARCHAR(100) NOT NULL,
    ingested_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_t_ingested_event PRIMARY KEY (ingested_id),
    CONSTRAINT uq_ingested_event UNIQUE (event_type, event_key)
);
COMMENT ON TABLE ihms_reporting.t_ingested_event IS 'Ingest idempotency ledger for the internal event ingress. Not in the register table list — added because strict double-ingest protection for ACCUMULATING read models requires durable dedupe; disclosed in WP-T2-reporting.md.';

-- ---------------------------------------------------------------------------
-- Transactional outbox (IMP-DEC-004; envelope DDL mirrors sibling services incl.
-- event_id UNIQUE). PHASE EVENT POLICY (register WP-T2): this service emits
-- NOTHING — no emitter exists; this table stays EMPTY (asserted by tests).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_reporting.outbox (
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
COMMENT ON TABLE ihms_reporting.outbox IS 'Transactional outbox TABLE per schema convention — ZERO emitters ever (register WP-T2 EVENT POLICY: reporting emits NOTHING); integration test asserts it stays empty through every flow.';
CREATE INDEX idx_outbox_dispatch ON ihms_reporting.outbox (status, created_at) WHERE status = 'PENDING';

-- ---------------------------------------------------------------------------
-- Sequences for migrated tables (IMP-DEC-003). rm_patient_registry PK is the
-- source patient id; rm_* stats use composite natural keys — no sequences there.
-- ---------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS ihms_reporting.t_report_master_seq  START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_reporting.t_report_execution_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_reporting.t_ingested_event_seq START WITH 1;
