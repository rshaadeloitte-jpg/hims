-- ============================================================================
-- V1 — ihms_ot schema (OT & Surgery service, S11) — WP-8 (Wave-3 tranche 3)
-- Clean target of the legacy OT cluster (MICROSERVICE-CATALOG #11):
-- t_otschedule + t_otscheduledetail, plus the transactional outbox for
-- ProcedureScheduled / SurgeryCompleted.
--
-- ONE-TABLE-ONE-OWNER (§56): ALL tables here belong to the ot service.
-- Migrated-table conventions mirror sibling services (IMP-DEC-003 explicit
-- sequences; audit block added_by/added_datetime/updated_by/updated_datetime).
--
-- Status dictionary (t_otschedule.status SMALLINT):
--   1 = PLANNED       (initial state on create)
--   2 = IN_PROGRESS   (after POST /{id}/start; only from PLANNED)
--   3 = DONE          (after POST /{id}/complete; only from IN_PROGRESS)
--   4 = CANCELLED     (after POST /{id}/cancel; from PLANNED or IN_PROGRESS)
--
-- BR-OT-001: a schedule requires admission_id (active admission) OR
--   encounter_id (latest visit); neither -> HTTP 400 code BEOT004. Cross-service
--   validation of the referenced ids is DEFERRED (opaque ids boundary, same as
--   nursing service). Dependent-surgery detail rows carry dependent_of ->
--   discount_pct applied from the request; procedure rate defaults 0 when not
--   supplied (tariff lookup deferred to Reference integration).
--
-- BR-OT-002: pre_billing flag persisted here and echoed in responses. The
--   actual direct-charge creation itself is DEFERRED to Billing (Wave 4).
--
-- Events via outbox -> topic ihms.ot.events (PROPOSED per KAFKA-EVENT-CATALOG
-- §3 — weak consumer evidence, CSSD/Inventory consumption unconfirmed):
--   ProcedureScheduled  procedure-scheduled.v1  partition key patientId  (on create)
--   SurgeryCompleted    surgery-completed.v1    partition key patientId  (on complete)
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS ihms_ot;

SET search_path TO ihms_ot, public;

-- ---------------------------------------------------------------------------
-- t_otschedule — OT table booking for one patient episode
-- ---------------------------------------------------------------------------
-- NOTE: on this table the register-pinned lifecycle column is literally
-- ''status'' SMALLINT (1..4); the shared boolean audit ''status'' of sibling
-- convention is intentionally OMITTED here to avoid the name clash.
CREATE TABLE IF NOT EXISTS ihms_ot.t_otschedule (
    schedule_id BIGINT NOT NULL,
    unit_id BIGINT,
    created_unit_id BIGINT, updated_unit_id BIGINT,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    patient_id BIGINT NOT NULL,
    encounter_id BIGINT,
    admission_id BIGINT,
    ot_table VARCHAR(100) NOT NULL,
    scheduled_start TIMESTAMP(3) NOT NULL,
    scheduled_end TIMESTAMP(3),
    anesthetist_id BIGINT,
    surgeon_id BIGINT,
    status SMALLINT NOT NULL DEFAULT 1 CHECK (status IN (1, 2, 3, 4)),
    pre_billing BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_t_otschedule PRIMARY KEY (schedule_id),
    CONSTRAINT ck_t_otschedule_episode CHECK (
        encounter_id IS NOT NULL OR admission_id IS NOT NULL
    ),
    CONSTRAINT ck_t_otschedule_window CHECK (
        scheduled_end IS NULL OR scheduled_end > scheduled_start
    )
);
CREATE INDEX IF NOT EXISTS ix_t_otschedule_day ON ihms_ot.t_otschedule (scheduled_start);
CREATE INDEX IF NOT EXISTS ix_t_otschedule_status ON ihms_ot.t_otschedule (status);
CREATE INDEX IF NOT EXISTS ix_t_otschedule_patient ON ihms_ot.t_otschedule (patient_id);
COMMENT ON TABLE ihms_ot.t_otschedule IS 'Clean target of T_OTSCHEDULE. Column named ot_status because ''status'' is the shared audit flag in sibling convention. ck_t_otschedule_episode is the DB backstop for BR-OT-001 (admission OR encounter required; service raises BEOT004 first).';
COMMENT ON COLUMN ihms_ot.t_otschedule.pre_billing IS 'BR-OT-002: pre-billing requested. Charge CREATION is deferred to Billing (Wave 4); this service persists and echoes the flag only.';

-- ---------------------------------------------------------------------------
-- t_otscheduledetail — procedures booked on a schedule; dependent-surgery
-- discount per BR-OT-001 (dependent_of references a sibling detail row)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_ot.t_otscheduledetail (
    detail_id BIGINT NOT NULL,
    unit_id BIGINT,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_unit_id BIGINT, updated_unit_id BIGINT,
    added_by BIGINT, added_datetime TIMESTAMP(3),
    updated_by BIGINT, updated_datetime TIMESTAMP(3),
    synchronized BOOLEAN NOT NULL DEFAULT FALSE,
    schedule_id BIGINT NOT NULL REFERENCES ihms_ot.t_otschedule (schedule_id),
    procedure_code VARCHAR(50) NOT NULL,
    procedure_name VARCHAR(200) NOT NULL,
    rate NUMERIC(12,2) NOT NULL DEFAULT 0,
    dependent_of BIGINT REFERENCES ihms_ot.t_otscheduledetail (detail_id),
    discount_pct NUMERIC(5,2) NOT NULL DEFAULT 0,
    CONSTRAINT pk_t_otscheduledetail PRIMARY KEY (detail_id),
    CONSTRAINT ck_t_otscheduledetail_discount CHECK (discount_pct >= 0 AND discount_pct <= 100)
);
CREATE INDEX IF NOT EXISTS ix_t_otscheduledetail_schedule ON ihms_ot.t_otscheduledetail (schedule_id);
ALTER TABLE ihms_ot.t_otscheduledetail
    ADD CONSTRAINT fk_t_otscheduledetail_dependent FOREIGN KEY (dependent_of)
    REFERENCES ihms_ot.t_otscheduledetail (detail_id) DEFERRABLE INITIALLY DEFERRED;
COMMENT ON TABLE ihms_ot.t_otscheduledetail IS 'Procedure rows on an OT schedule. rate defaults 0 when no tariff match (tariff lookup deferred to Reference integration). dependent_of set -> dependent surgery; discount_pct applied from request per BR-OT-001. FK is deferrable so a request batch can insert parent+dependent details in one transaction.';
COMMENT ON INDEX ix_t_otscheduledetail_schedule IS 'Detail fetch for GET /schedules/{id} and completion scans.';

-- ---------------------------------------------------------------------------
-- Transactional outbox (IMP-DEC-004; envelope per KAFKA-EVENT-CATALOG §4;
-- DDL mirrors sibling services incl. event_id UNIQUE). Events:
--   ProcedureScheduled procedure-scheduled.v1 (PROPOSED §3, partition key patientId)
--   SurgeryCompleted   surgery-completed.v1   (PROPOSED §3, partition key patientId)
--   -> topic ihms.ot.events via relay.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ihms_ot.outbox (
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
COMMENT ON TABLE ihms_ot.outbox IS 'Transactional outbox: schedule create/complete txs write ProcedureScheduled / SurgeryCompleted (both PROPOSED per KAFKA-EVENT-CATALOG §3) here atomically; relay publishes to ihms.ot.events.';
CREATE INDEX idx_outbox_dispatch ON ihms_ot.outbox (status, created_at) WHERE status = 'PENDING';

-- ---------------------------------------------------------------------------
-- Sequences for migrated tables (IMP-DEC-003)
-- ---------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS ihms_ot.t_otschedule_seq       START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ihms_ot.t_otscheduledetail_seq START WITH 1;
